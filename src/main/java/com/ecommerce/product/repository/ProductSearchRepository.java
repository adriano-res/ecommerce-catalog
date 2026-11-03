package com.ecommerce.product.repository;

import com.ecommerce.product.dto.FacetOption;
import com.ecommerce.product.dto.PriceRangeFacet;
import com.ecommerce.product.dto.ProductSearchItemResponse;
import com.ecommerce.product.dto.ProductSearchResult;
import com.ecommerce.product.dto.SearchFacetResponse;
import com.ecommerce.product.dto.SearchFilterRequest;
import com.ecommerce.product.dto.SearchSort;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.shared.pagination.CursorEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Repository;

/**
 * Executes single-round-trip faceted product search queries using MongoDB
 * aggregation pipeline.
 */
@Repository
public class ProductSearchRepository {

  private static final String COLLECTION_NAME = "products";

  private final MongoTemplate mongoTemplate;

  public ProductSearchRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Executes a faceted product search aggregation pipeline in a single database round-trip.
   *
   * <p>Full-text search behavior: When {@code request.q()} is provided, filtering is performed
   * using MongoDB's {@code $text} query operator against the {@code idx_products_text} index,
   * which covers {@code name} (weight 10), {@code brand} (weight 5), and {@code description}
   * (weight 1). Note that {@code $text} operates on complete words/tokens with Portuguese language
   * stemming; it does not perform partial prefix or substring matching (e.g., "miss" matches
   * "Miss the Misery", but partial prefixes such as "mis" or "m" do not match).
   *
   * @param request search parameters including text query, price range, attributes, sort, and cursor
   * @param categoryId resolved category ID for hierarchical ancestor path matching (or null)
   * @return consolidated search result containing items, total count, facets, and next cursor
   */
  public ProductSearchResult search(SearchFilterRequest request, String categoryId) {
    List<Document> matchClauses = new ArrayList<>();
    matchClauses.add(new Document("status", "ACTIVE"));

    // Text search delegates to MongoDB's $text operator, which evaluates against the single text
    // index 'idx_products_text' created in V002_CreateProductIndexes. This index filters across
    // 'name' (weight 10), 'brand' (weight 5), and 'description' (weight 1) using tokenized word
    // matching and Portuguese language stemming. Complete words are required for token match.
    if (request.q() != null && !request.q().isBlank()) {
      matchClauses.add(new Document("$text", new Document("$search", request.q().trim())));
    }

    if (categoryId != null && !categoryId.isBlank()) {
      matchClauses.add(new Document("categoryPath", categoryId.trim()));
    }

    if (request.priceMin() != null || request.priceMax() != null) {
      Document priceDoc = new Document();
      if (request.priceMin() != null) {
        long minMinor = request.priceMin().movePointRight(2).longValue();
        priceDoc.append("$gte", minMinor);
      }
      if (request.priceMax() != null) {
        long maxMinor = request.priceMax().movePointRight(2).longValue();
        priceDoc.append("$lte", maxMinor);
      }
      matchClauses.add(new Document("price.amountMinor", priceDoc));
    }

    if (request.attributes() != null && !request.attributes().isEmpty()) {
      for (Map.Entry<String, List<String>> entry : request.attributes().entrySet()) {
        String attrKey = entry.getKey();
        List<String> rawValues = entry.getValue();
        if (attrKey == null || attrKey.isBlank() || rawValues == null || rawValues.isEmpty()) {
          continue;
        }
        List<Object> targetValues = resolveAttributeTargetValues(rawValues);
        if (!targetValues.isEmpty()) {
          Document elemMatch = new Document("k", attrKey).append("v", new Document("$in", targetValues));
          matchClauses.add(new Document("attributes", new Document("$elemMatch", elemMatch)));
        }
      }
    }

    Document matchStage = new Document("$match", new Document("$and", matchClauses));

    SearchSort sort = request.sort();
    if (sort == null) {
      sort = (request.q() != null && !request.q().isBlank())
          ? SearchSort.RELEVANCE
          : SearchSort.NEWEST;
    }

    boolean isRelevanceWithText = (sort == SearchSort.RELEVANCE && request.q() != null && !request.q().isBlank());

    List<Document> resultsPipeline = new ArrayList<>();
    if (isRelevanceWithText) {
      resultsPipeline.add(
          new Document("$addFields", new Document("score", new Document("$meta", "textScore"))));
    }

    if (request.cursor() != null && !request.cursor().isBlank()) {
      Document cursorCondition = buildCursorMatch(request.cursor(), sort, isRelevanceWithText);
      if (cursorCondition != null) {
        resultsPipeline.add(new Document("$match", cursorCondition));
      }
    }

    resultsPipeline.add(new Document("$sort", buildSortDoc(sort, isRelevanceWithText)));

    int limit = (request.limit() != null && request.limit() > 0) ? request.limit() : 20;
    resultsPipeline.add(new Document("$limit", limit + 1));

    Document facetStage = new Document(
        "$facet",
        new Document()
            .append("results", resultsPipeline)
            .append("totalCount", List.of(new Document("$count", "total")))
            .append(
                "attrFacets",
                List.of(
                    new Document("$unwind", "$attributes"),
                    new Document(
                        "$group",
                        new Document(
                            "_id",
                            new Document("k", "$attributes.k")
                                .append("v", "$attributes.v"))
                            .append("count", new Document("$sum", 1L)))))
            .append(
                "priceRanges",
                List.of(
                    new Document(
                        "$bucket",
                        new Document("groupBy", "$price.amountMinor")
                            .append("boundaries", List.of(0L, 200000L, 400000L))
                            .append("default", 400000L)
                            .append(
                                "output",
                                new Document("count", new Document("$sum", 1L)))))));

    AggregationOperation matchOp = ctx -> matchStage;
    AggregationOperation facetOp = ctx -> facetStage;

    Aggregation aggregation = Aggregation.newAggregation(matchOp, facetOp);
    AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, COLLECTION_NAME,
        Document.class);
    Document facetDoc = aggregationResults.getUniqueMappedResult();

    if (facetDoc == null) {
      return ProductSearchResult.empty();
    }

    return extractSearchResult(facetDoc, limit, sort, isRelevanceWithText);
  }

  private ProductSearchResult extractSearchResult(
      Document facetDoc, int limit, SearchSort sort, boolean isRelevanceWithText) {
    List<Document> rawResults = facetDoc.getList("results", Document.class);
    if (rawResults == null) {
      rawResults = Collections.emptyList();
    }

    boolean hasNext = rawResults.size() > limit;
    List<Document> pageDocs = hasNext ? rawResults.subList(0, limit) : rawResults;

    List<ProductSearchItemResponse> items = new ArrayList<>(pageDocs.size());
    for (Document rawDoc : pageDocs) {
      ProductDocument doc = mongoTemplate.getConverter().read(ProductDocument.class, rawDoc);
      String thumbnailUrl = (doc.getImages() != null && !doc.getImages().isEmpty())
          ? doc.getImages().get(0).url()
          : null;
      items.add(
          new ProductSearchItemResponse(
              doc.getId(),
              doc.getSku(),
              doc.getSlug(),
              doc.getName(),
              doc.getBrand(),
              doc.getPrice(),
              thumbnailUrl));
    }

    String nextCursor = null;
    if (hasNext && !pageDocs.isEmpty()) {
      Document lastRaw = pageDocs.get(pageDocs.size() - 1);
      ProductDocument lastDoc = mongoTemplate.getConverter().read(ProductDocument.class, lastRaw);
      String tieBreakerId = lastDoc.getId();
      String sortValue = switch (sort) {
        case PRICE_ASC, PRICE_DESC -> String.valueOf(lastDoc.getPrice().amountMinor());
        case RELEVANCE -> {
          if (isRelevanceWithText) {
            Number scoreNum = lastRaw.get("score", Number.class);
            yield (scoreNum != null) ? String.valueOf(scoreNum.doubleValue()) : "0.0";
          } else {
            Instant createdAt = (lastDoc.getAudit() != null && lastDoc.getAudit().createdAt() != null)
                ? lastDoc.getAudit().createdAt()
                : Instant.EPOCH;
            yield String.valueOf(createdAt.toEpochMilli());
          }
        }
        case NEWEST -> {
          Instant createdAt = (lastDoc.getAudit() != null && lastDoc.getAudit().createdAt() != null)
              ? lastDoc.getAudit().createdAt()
              : Instant.EPOCH;
          yield String.valueOf(createdAt.toEpochMilli());
        }
      };
      nextCursor = CursorEncoder.encode(sortValue, tieBreakerId);
    }

    long totalCount = 0L;
    List<Document> totalList = facetDoc.getList("totalCount", Document.class);
    if (totalList != null && !totalList.isEmpty()) {
      Number totalNum = totalList.get(0).get("total", Number.class);
      if (totalNum != null) {
        totalCount = totalNum.longValue();
      }
    }

    long count0 = 0L;
    long count200k = 0L;
    long count400k = 0L;
    List<Document> priceBuckets = facetDoc.getList("priceRanges", Document.class);
    if (priceBuckets != null) {
      for (Document bucket : priceBuckets) {
        Object id = bucket.get("_id");
        Number countNum = bucket.get("count", Number.class);
        long c = (countNum != null) ? countNum.longValue() : 0L;
        if (Long.valueOf(0L).equals(id) || Integer.valueOf(0).equals(id)) {
          count0 = c;
        } else if (Long.valueOf(200000L).equals(id) || Integer.valueOf(200000).equals(id)) {
          count200k = c;
        } else {
          count400k = c;
        }
      }
    }
    List<PriceRangeFacet> priceRangeFacets = List.of(
        new PriceRangeFacet("0.00", "1999.99", count0),
        new PriceRangeFacet("2000.00", "3999.99", count200k),
        new PriceRangeFacet("4000.00", null, count400k));

    Map<String, List<FacetOption>> attrMap = new LinkedHashMap<>();
    List<Document> attrDocs = facetDoc.getList("attrFacets", Document.class);
    if (attrDocs != null) {
      for (Document doc : attrDocs) {
        Document idDoc = doc.get("_id", Document.class);
        if (idDoc == null) {
          continue;
        }
        String key = idDoc.getString("k");
        Object valObj = idDoc.get("v");
        if (key == null || valObj == null) {
          continue;
        }
        String valueStr = String.valueOf(valObj);
        Number countNum = doc.get("count", Number.class);
        long count = (countNum != null) ? countNum.longValue() : 0L;

        attrMap.computeIfAbsent(key, k -> new ArrayList<>()).add(new FacetOption(valueStr, count));
      }
    }
    for (List<FacetOption> options : attrMap.values()) {
      options.sort(
          Comparator.comparingLong(FacetOption::count)
              .reversed()
              .thenComparing(FacetOption::value));
    }

    SearchFacetResponse facetResponse = new SearchFacetResponse(attrMap, priceRangeFacets);
    return new ProductSearchResult(items, totalCount, facetResponse, nextCursor);
  }

  private Document buildSortDoc(SearchSort sort, boolean isRelevanceWithText) {
    Document sortDoc = new Document();
    switch (sort) {
      case PRICE_ASC -> {
        sortDoc.append("price.amountMinor", 1);
        sortDoc.append("_id", 1);
      }
      case PRICE_DESC -> {
        sortDoc.append("price.amountMinor", -1);
        sortDoc.append("_id", 1);
      }
      case RELEVANCE -> {
        if (isRelevanceWithText) {
          sortDoc.append("score", -1);
          sortDoc.append("_id", 1);
        } else {
          sortDoc.append("audit.createdAt", -1);
          sortDoc.append("_id", 1);
        }
      }
      case NEWEST -> {
        sortDoc.append("audit.createdAt", -1);
        sortDoc.append("_id", 1);
      }
    }
    return sortDoc;
  }

  private Document buildCursorMatch(
      String cursorString, SearchSort sort, boolean isRelevanceWithText) {
    CursorEncoder.Cursor cursor = CursorEncoder.decode(cursorString);
    String sortValStr = cursor.sortValue();
    String tieBreakerId = cursor.tieBreakerId();

    Document idCondition = ObjectId.isValid(tieBreakerId)
        ? new Document("_id", new Document("$gt", new ObjectId(tieBreakerId)))
        : new Document("_id", new Document("$gt", tieBreakerId));

    switch (sort) {
      case PRICE_ASC -> {
        long cursorPrice = Long.parseLong(sortValStr);
        Document tieClause = new Document("price.amountMinor", cursorPrice);
        tieClause.putAll(idCondition);
        return new Document(
            "$or",
            List.of(
                new Document("price.amountMinor", new Document("$gt", cursorPrice)), tieClause));
      }
      case PRICE_DESC -> {
        long cursorPrice = Long.parseLong(sortValStr);
        Document tieClause = new Document("price.amountMinor", cursorPrice);
        tieClause.putAll(idCondition);
        return new Document(
            "$or",
            List.of(
                new Document("price.amountMinor", new Document("$lt", cursorPrice)), tieClause));
      }
      case RELEVANCE -> {
        if (isRelevanceWithText) {
          double cursorScore = Double.parseDouble(sortValStr);
          Document tieClause = new Document("score", cursorScore);
          tieClause.putAll(idCondition);
          return new Document(
              "$or",
              List.of(new Document("score", new Document("$lt", cursorScore)), tieClause));
        } else {
          Date cursorDate = parseDate(sortValStr);
          Document tieClause = new Document("audit.createdAt", cursorDate);
          tieClause.putAll(idCondition);
          return new Document(
              "$or",
              List.of(
                  new Document("audit.createdAt", new Document("$lt", cursorDate)), tieClause));
        }
      }
      case NEWEST -> {
        Date cursorDate = parseDate(sortValStr);
        Document tieClause = new Document("audit.createdAt", cursorDate);
        tieClause.putAll(idCondition);
        return new Document(
            "$or",
            List.of(
                new Document("audit.createdAt", new Document("$lt", cursorDate)), tieClause));
      }
    }
    return null;
  }

  private Date parseDate(String val) {
    try {
      return new Date(Long.parseLong(val));
    } catch (NumberFormatException e) {
      return Date.from(Instant.parse(val));
    }
  }

  private List<Object> resolveAttributeTargetValues(List<String> rawValues) {
    List<Object> targets = new ArrayList<>();
    for (String raw : rawValues) {
      if (raw == null) {
        continue;
      }
      String trimmed = raw.trim();
      targets.add(trimmed);

      if ("true".equalsIgnoreCase(trimmed)) {
        targets.add(Boolean.TRUE);
      } else if ("false".equalsIgnoreCase(trimmed)) {
        targets.add(Boolean.FALSE);
      }

      try {
        long l = Long.parseLong(trimmed);
        targets.add(l);
        targets.add((int) l);
      } catch (NumberFormatException ignored) {
        try {
          double d = Double.parseDouble(trimmed);
          targets.add(d);
        } catch (NumberFormatException ignoredDouble) {
        }
      }
    }
    return targets;
  }
}
