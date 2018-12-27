package me.hex539.contest;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.List;
import java.util.SortedSet;
import org.junit.Test;
import java.util.TreeSet;

import me.hex539.contest.SplayList;

import static com.google.common.truth.Truth.*;

public class DataStructuresTest {
  @Test
  public void testNaturalOrder() {
    SplayList<String> l = new SplayList<>();
    l.add("zanzibar");
    l.add("gondola");
    l.add("alphabet");
    l.add("int32");

    assertThat(l).containsExactly("alphabet", "gondola", "int32", "zanzibar").inOrder();
  }

  @Test
  public void testRemoval() {
    SplayList<Long> l = new SplayList<>();
    for (Long i : longList(1, 2, 4, 6, 5, 9, 7, 3, 8)) {
      l.add(i);
    }

    // Remove valid elements.
    assertThat(l.remove(Long.valueOf(4))).isTrue();
    assertThat(l.remove(Long.valueOf(5))).isTrue();

    // Can't remove invalid elements.
    assertThat(l.remove(Long.valueOf(10))).isFalse();
    assertThat(l.remove(Long.valueOf(0))).isFalse();

    // Size should be updated.
    assertThat(l.size()).isEqualTo(7);

    // Indices should be updated.
    assertThat(l.indexOf(Long.valueOf(4))).isEqualTo(-1);
    assertThat(l.indexOf(Long.valueOf(1))).isEqualTo(0);
    assertThat(l.indexOf(Long.valueOf(7))).isEqualTo(4);

    // Indices should be correct.
    assertThat(l.get(0)).isEqualTo(1);
    assertThat(l.get(1)).isEqualTo(2);
    assertThat(l.get(5)).isEqualTo(8);

    // Contents should still be in order.
    assertThat(l).containsExactlyElementsIn(longList(1, 2, 3, 6, 7, 8, 9)).inOrder();
  }

  @Test
  public void testStream() {
    SplayList<Long> l = new SplayList<>();
    l.addAll(longList(6, 5, 4, 3, 7, 8, 9, 1, 2));

    assertThat(l.size()).isEqualTo(9);
    assertThat(l.remove(Long.valueOf(1))).isTrue();
    assertThat(l.remove(Long.valueOf(9))).isTrue();
    assertThat(l.size()).isEqualTo(7);

    assertThat(l.stream().collect(Collectors.toList()))
        .containsExactlyElementsIn(longList(2, 3, 4, 5, 6, 7, 8)).inOrder();

    assertThat(l.stream().filter(x -> x > 7).findFirst().get()).isEqualTo(8);
  }

  @Test
  public void testCustomComparator() {
    SplayList<String> l = new SplayList<>((a, b) -> {
      if (a.charAt(1) != b.charAt(1)) {
        return Character.compare(b.charAt(1), a.charAt(1));
      }
      return a.compareTo(b);
    });
    l.addAll(Arrays.asList(new String[] {"9299", "430", "31", "921111", "1299"}));
    assertThat(l).containsExactly("430", "1299", "921111", "9299", "31").inOrder();
  }

  @Test
  public void testHundredThousandAscendingElements() {
    SplayList<String> l = new SplayList<>();
    SortedSet<String> e = new TreeSet<>();
    for (int i = 0; i < 100000; i++) {
      l.add(Long.toString(i));
      e.add(Long.toString(i));
    }
    for (int i = 50000; i < 100000; i++) {
      l.remove(Long.toString(i));
      e.remove(Long.toString(i));
    }
    assertThat(l).containsExactlyElementsIn(e).inOrder();

    assertThat(l.indexOf("0")).isEqualTo(0);
    assertThat(l.indexOf("1")).isEqualTo(1);
    assertThat(l.indexOf("2")).isEqualTo(11112);
    assertThat(l.indexOf("9999")).isEqualTo(49999);
    assertThat(l.indexOf("49999")).isEqualTo(44444);
    assertThat(l.lastIndexOf("49999")).isEqualTo(44444);
    assertThat(l.indexOf("50000")).isEqualTo(-1);

    // Check retainAll performance.
    assertThat(l.retainAll(e)).isFalse();
    assertThat(l.size()).isEqualTo(50000);

    // Check removeAll performance.
    assertThat(l.removeAll(e)).isTrue();
    assertThat(l.size()).isEqualTo(0);
  }

  @Test
  public void testBooleanList() {
    SplayList<Boolean> l = new SplayList<>();
    l.add(Boolean.TRUE);
    assertThat(l.size()).isEqualTo(1);
    assertThat(l.stream().filter(x -> x).count()).isEqualTo(1);
    assertThat(l.stream().filter(x -> !x).count()).isEqualTo(0);
    assertThat(l.isEmpty()).isFalse();
  }

  @Test
  public void testSlicing() {
    SplayList<Long> l = new SplayList<>();
    for (Long i : longList(1, 2, 4, 6, 5, 9, 7, 3, 8)) {
      l.add(i);
    }

    assertThat(l.headSet(Long.valueOf(4))).containsExactly(1L, 2L, 3L).inOrder();
    assertThat(l.tailSet(Long.valueOf(4))).containsExactly(4L, 5L, 6L, 7L, 8L, 9L).inOrder();
    assertThat(l.subSet(3L, 8L)).containsExactly(3L, 4L, 5L, 6L, 7L).inOrder();
  }

  @Test
  public void testFlatReversibleList() {
    SplayList<Long> l = new SplayList<>(x -> x);

    l.addAll(longList(1, 2, 4, 6, 5, 9, 7, 3, 8));
    assertThat(l.get(3)).isEqualTo(6);

    l.remove(3);
    assertThat(l.get(3)).isEqualTo(5);

    l.set(3, Long.valueOf(55));
    assertThat(l.get(3)).isEqualTo(55);
    assertThat(l.indexOfTag(55L)).isEqualTo(3);
    assertThat(l.subList(1, l.size() - 1)).containsExactly(2L, 4L, 55L, 9L, 7L, 3L).inOrder();

    // Copy onto the end of another unsorted list.
    SplayList<Long> b = new SplayList<>(SplayList.unordered());
    b.addAll(longList(1, 2, 3));
    b.addAll(l);
    assertThat(b).containsExactly(1L, 2L, 3L, 1L, 2L, 4L, 55L, 9L, 7L, 3L, 8L).inOrder();

    // Merge into another sorted list.
    SplayList<Long> c = new SplayList<>();
    c.addAll(longList(1, 2, 3));
    c.addAll(l.subList(1, l.size() - 1));
    assertThat(c).containsExactly(1L, 2L, 3L, 4L, 7L, 9L, 55L).inOrder();
  }

  private static List<Long> longList(long... v) {
    return Arrays.stream(v).boxed().collect(Collectors.toList());
  }
}
