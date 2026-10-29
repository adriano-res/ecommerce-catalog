package com.ecommerce.shared.pagination;

import java.util.Collections;
import java.util.List;

public record CursorPage<T>(
    List<T> items,
    int limit,
    boolean hasNext,
    String nextCursor,
    long totalCount) {

  public CursorPage {
    items = (items != null) ? List.copyOf(items) : Collections.emptyList();
  }

  public static <T> CursorPage<T> empty(int limit) {
    return new CursorPage<>(Collections.emptyList(), limit, false, null, 0L);
  }

  public static <T> CursorPage<T> of(List<T> items, int limit, boolean hasNext, String nextCursor, long totalCount) {
    return new CursorPage<>(items, limit, hasNext, nextCursor, totalCount);
  }
}
