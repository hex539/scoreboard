package me.hex539.contest;

import java.util.AbstractList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Function;

public final class SplayList<T> extends AbstractList<T> implements List<T>, SortedSet<T> {
  private final Comparator<? super T> comp;
  private final Function<? super T, ?> tagger;
  private SplayTree<T> root;

  private final Map<Object, SplayTree<T>> tagMap;

  public SplayList() {
    this((a, b) -> ((Comparable) a).compareTo(b));
  }

  public SplayList(Comparator<? super T> comparator) {
    this.comp = comparator;
    this.tagger = null;
    this.tagMap = null;
  }

  public <K> SplayList(Function<? super T, ?> tagger) {
    this.comp = null;
    this.tagger = tagger;
    this.tagMap = new HashMap<>();
  }

  public SplayList(Collection<? extends T>  other) {
    this();
    addAll(other);
  }

  public SplayList(Collection<? extends T> other, Comparator<? super T> comparator) {
    this(comparator);
    addAll(other);
  }

  public <K> SplayList(Collection<? extends T> other, Function<? super T, ?> tagger) {
    this(tagger);
    addAll(other);
  }

  public static <T> Comparator<T> unordered() {
    return null;
  }

  private final SplayTree<T> create(T value) {
    SplayTree<T> res = new SplayTree<T>(value);
    if (tagger != null) {
      if (tagMap.put(tagger.apply(value), res) != null) {
        throw new AssertionError("Creating duplicate tag: " + tagger.apply(value));
      }
    }
    return res;
  }

  private final SplayTree<T> delete(SplayTree<T> tree) {
    if (tagger != null) {
      if (tagMap.remove(tagger.apply(tree.key)) == null) {
        throw new AssertionError("Removing nonexistent tag: " + tagger.apply(tree.key));
      }
    }
    return tree.delete();
  }

  @Override
  public Comparator<? super T> comparator() {
    return comp;
  }

  @Override
  public Spliterator<T> spliterator() {
    return super.spliterator();
  }

  @Override
  public T first() {
    return get(0);
  }

  @Override
  public T last() {
    return get(size() - 1);
  }

  @Override
  public SplayList<T> headSet(T toElement) {
    SplayList<T> res = new SplayList<>(this, comparator());
    if (res.root != null) {
      res.root = res.root.splay().find(toElement, comparator()).cutBefore();
    }
    return res;
  }

  @Override
  public SplayList<T> tailSet(T fromElement) {
    SplayList<T> res = new SplayList<>(this, comparator());
    if (res.root != null) {
      res.root = res.root.splay().find(fromElement, comparator());
      res.root.cutBefore();
    }
    return res;
  }

  @Override
  public SplayList<T> subSet(T fromElement, T toElement) {
    return tailSet(fromElement).headSet(toElement);
  }

  @Override
  public SplayList<T> subList(int fromIndex, int toIndex) {
    SplayList<T> res = new SplayList<>(this, comparator());
    if (res.root != null && toIndex < size()) {
      res.root = res.root.get(toIndex).splay().cutBefore();
    }
    if (res.root != null && fromIndex > 0) {
      res.root = res.root.get(fromIndex).splay();
      res.root.cutBefore();
    }
    return res;
  }

  @Override
  public T get(int index) {
    if (root == null || index < 0 || index >= root.splay().size) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    return root.splay().get(index).key;
  }

  @Override
  public T remove(int index) {
    if (root == null) {
      throw new NoSuchElementException();
    }
    SplayTree<T> v = root.splay().get(index);
    root = delete(v);
    return v.key;
  }

  @Override
  public boolean remove(Object o) {
    if (root == null) {
      return false;
    }
    SplayTree<T> t = root.splay().find((T) o, comparator());
    if (t == null) {
      return false;
    }
    root = delete(t);
    return true;
  }

  @Override
  public void clear() {
    root = null;
  }

  @Override
  public T set(int index, T value) {
    if (index < 0 || index >= size()) {
      throw new NoSuchElementException();
    }
    final T res;
    if (comparator() != null) {
      if (index > 0 && comparator().compare(get(index - 1), value) >= 0) {
        throw new IllegalArgumentException();
      }
      if (index + 1 < size() && comparator().compare(get(index + 1), value) <= 0) {
        throw new IllegalArgumentException();
      }
      res = remove(index);
      add(value);
    } else {
      res = remove(index);
      add(index, value);
    }
    return res;
  }

  @Override
  public void add(int index, T value) {
    if (index < 0 || index > size()) {
      throw new NoSuchElementException();
    }
    if (comparator() != null) {
      if (index > 0 && comparator().compare(get(index - 1), value) >= 0) {
        throw new IllegalArgumentException();
      }
      if (index < size() && comparator().compare(get(index), value) <= 0) {
        throw new IllegalArgumentException();
      }
      add(value);
    } else {
      SplayTree<T> t = create(value);
      if (root == null) {
        root = t;
      } else if (index == 0) {
        root = t.insertBefore(root.splay().get(index));
      } else {
        root = t.insertAfter(root.splay().get(index - 1));
      }
    }
  }

  @Override
  public boolean add(T value) {
    final int oldSize = size();
    if (comparator() == null) {
      add(size(), value);
    } else {
      SplayTree<T> t = create(value);
      root = t.insertInto(root != null ? root.splay() : null, comparator());
    }
    return size() == oldSize + 1;
  }

  @Override
  public int size() {
    return root == null ? 0 : root.splay().size;
  }

  public int indexOfTag(Object val) {
    if (tagger == null) {
      throw new AssertionError("Object is not set up to support tagging");
    }
    SplayTree<T> res = tagMap.get(val);
    return res != null ? res.indexOf() : -1;
  }

  @Override
  public int indexOf(Object val) {
    if (comparator() == null) {
      throw new UnsupportedOperationException("Consider using indexOfTag()");
    }
    SplayTree<T> v = (root == null ? null : root.splay().find((T) val, comparator()));
    return v != null ? v.indexOf() : -1;
  }

  @Override
  public int lastIndexOf(Object val) {
    return indexOf(val);
  }
}
