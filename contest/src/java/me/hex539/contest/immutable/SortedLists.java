package me.hex539.contest.immutable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import edu.clics.proto.ClicsProto.*;

/**
 * Helpers for dealing with sorted lists.
 */
public abstract class SortedLists {

  private SortedLists() {}

  public static <T> Optional<T> binarySearch(List<T> items, Function<T, Integer> compareTo) {
    final T res;

    if (!items.isEmpty()) {
      int l = 0;
      for (int rad = (1 << 30); rad != 0; rad >>>= 1) {
        if (l + rad < items.size() && compareTo.apply(items.get(l + rad)) >= 0) {
          l += rad;
        }
      }
      res = items.get(l);
    } else {
      res = null;
    }

    if (res != null && compareTo.apply(res) == 0) {
      return Optional.ofNullable(res);
    } else {
      return Optional.empty();
    }
  }

  public static <T, K extends Comparable<K>> List<T> sortBy(Collection<T> l, Function<T, K> key) {
    List<T> list = new ArrayList<>(l);
    Collections.sort(list, (a, b) -> key.apply(a).compareTo(key.apply(b)));
    return Collections.unmodifiableList(list);
  }
}
