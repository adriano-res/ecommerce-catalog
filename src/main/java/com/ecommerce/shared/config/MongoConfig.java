package com.ecommerce.shared.config;

import com.ecommerce.shared.money.Money;
import java.util.Currency;
import java.util.List;
import org.bson.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
@EnableMongoAuditing(auditorAwareRef = "auditorAwareImpl")
public class MongoConfig {

  @Bean
  public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }

  @Bean
  public MongoCustomConversions customConversions() {
    return new MongoCustomConversions(List.of(
        new MoneyWritingConverter(),
        new MoneyReadingConverter()
    ));
  }

  @WritingConverter
  public static class MoneyWritingConverter implements Converter<Money, Document> {
    @Override
    public Document convert(Money source) {
      Document doc = new Document();
      doc.put("amountMinor", source.amountMinor());
      doc.put("currency", source.currency().getCurrencyCode());
      return doc;
    }
  }

  @ReadingConverter
  public static class MoneyReadingConverter implements Converter<Document, Money> {
    @Override
    public Money convert(Document source) {
      Long amountMinor = source.getLong("amountMinor");
      String currencyCode = source.getString("currency");
      if (amountMinor == null || currencyCode == null) {
        return null;
      }
      return Money.ofMinor(amountMinor, Currency.getInstance(currencyCode));
    }
  }
}
