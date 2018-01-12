package me.hex539.contest;

import java.util.AbstractList;
import java.util.Comparator;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.Spliterator;

public class SplayList<T> extends AbstractList<T> implements List<T>, SortedSet<T> {
  private final Comparator<? super T> comp;
  private SplayTree root;

  public SplayList() {
    this((a, b) -> ((Comparable) a).compareTo(b));
  }

  public SplayList(Comparator<? super T> comparator) {
    this.comp = comparator;
  }

  public SplayList(Collection<? extends T>  other) {
    this();
    addAll(other);
  }

  public SplayList(Collection<? extends T> other, Comparator<? super T> comparator) {
    this(comparator);
    addAll(other);
  }

  public static <T> Comparator<T> unordered() {
    return null;
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
      res.root = res.root.splay().find(toElement).cutBefore();
    }
    return res;
  }

  @Override
  public SplayList<T> tailSet(T fromElement) {
    SplayList<T> res = new SplayList<>(this, comparator());
    if (res.root != null) {
      res.root = res.root.splay().find(fromElement);
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
    if (root == null) {
      throw new NoSuchElementException();
    }
    if (index < 0 || index >= root.splay().size) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    return root.splay().get(index).key;
  }

  @Override
  public T remove(int index) {
    if (root == null) {
      throw new NoSuchElementException();
    }
    SplayTree v = root.splay().get(index);
    root = v.delete();
    return v.key;
  }

  public void debug() {
    if (root != null) root.traverse(0);
  }

  @Override
  public boolean remove(Object o) {
    if (root == null) {
      return false;
    }
    SplayTree t = root.splay().find((T) o);
    if (t == null) {
      return false;
    }
    root = t.delete();
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
      SplayTree t = new SplayTree(value);
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
      SplayTree t = new SplayTree(value);
      root = t.insertInto(root != null ? root.splay() : null);
    }
    return size() == oldSize + 1;
  }

  @Override
  public int size() {
    return root == null ? 0 : root.splay().size;
  }

  @Override
  public int indexOf(Object val) {
    SplayTree v = (root == null ? null : root.splay().find((T) val));
    return v != null ? v.indexOf() : -1;
  }

  @Override
  public int lastIndexOf(Object val) {
    return indexOf(val);
  }

  private class SplayTree {
		int size = 1;
    SplayTree l = null;
    SplayTree r = null;
    SplayTree p = null;
    public final T key;

    public SplayTree(T key) {
      this.key = key;
    }

    private void traverse(int depth) {
      if (l != null) l.traverse(depth + 1);
      for (int i = depth; i --> 0;) {
        System.err.print("--");
      }
      System.err.println(" " + key);
      if (r != null) r.traverse(depth + 2);
    }

    public SplayTree insertInto(SplayTree parent) {
      while (true) {
        if (parent == null) {
          p = null;
        } else {
          final int side = comparator().compare(key, parent.key);
          if (side == 0) {
            // Already exists. Don't insert.
            return parent.splay();
          } else if (side < 0) {
            if (parent.l == null) {
              parent.setL(this);
            } else {
              parent = parent.l;
              continue;
            }
          } else if (side > 0) {
            if (parent.r == null) {
              parent.setR(this);
            } else {
              parent = parent.r;
              continue;
            }
          }
        }
        return splay();
      }
    }

    public SplayTree insertBefore(SplayTree parent) {
      SplayTree orig = parent;
      while (true) {
        if (parent == null) {
          p = null;
        } else {
          final int side = (parent == orig ? -1 : 1);
          if (side == 0) {
            // Technically the outer container should return false, but we aren't inserting
            // anything more than once by design, so this is OK for now.
            throw new Error("Comparator should not return 0");
          } else if (side < 0) {
            if (parent.l == null) {
              parent.setL(this);
            } else {
              parent = parent.l;
              continue;
            }
          } else if (side > 0) {
            if (parent.r == null) {
              parent.setR(this);
            } else {
              parent = parent.r;
              continue;
            }
          }
        }
        return splay();
      }
    }

    public SplayTree insertBefore(SplayTree parent, boolean z) {
      splay();
      if (parent == null) {
        return splay();
      } else if (parent.l == null) {
        parent.setL(this);
      } else {
        parent = parent.l;
        while (parent.r != null) {
          parent = parent.r;
        }
        parent.setR(this);
      }
      return splay();
    }

    public SplayTree insertAfter(SplayTree parent) {
      splay();
      if (parent == null) {
        return this;
      } else if (parent.r == null) {
        parent.setR(this);
      } else {
        parent = parent.r;
        while (parent.l != null) {
          parent = parent.l;
        }
        parent.setL(this);
      }
      return splay();
    }

    public SplayTree getFirst() {
      SplayTree res = this;
      while (res.l != null) {
        res = res.l;
      }
      return res.splay();
    }

    public SplayTree getLast() {
      SplayTree res = this;
      while (res.r != null) {
        res = res.r;
      }
      return res.splay();
    }
    private void setL(SplayTree l) {
      if ((this.l = l) != null) l.p = this;
      updateSize();
    }

    private void setR(SplayTree r) {
      if ((this.r = r) != null) r.p = this;
      updateSize();
    }

    private void updateSize() {
      size = 1 + (l != null ? l.size : 0) + (r != null ? r.size : 0);
    }

    private void rol() {
      SplayTree a = p, b = this, c = l;
      b.setL(c.r);
      c.setR(b);
      if (a != null) {if (a.l == b) a.setL(c); else a.setR(c);} else c.p = null;
    }

    private void ror() {
      SplayTree a = p, b = this, c = r;
      b.setR(c.l);
      c.setL(b);
      if (a != null) {if (a.l == b) a.setL(c); else a.setR(c);} else c.p = null;
    }

    private void rotate() {
      if (p != null) {
        if (p.l == this) {
          p.rol();
        } else if (p.r == this) {
          p.ror();
        } else {
          throw new Error("Vertex " + this + " is neither left nor right child of " + p);
        }
      }
    }

    public SplayTree splay() {
      while (p != null) {
        if (p.p != null) {
          if ((p.l == this) == (p.p.l == p)) {
            p.rotate();
          } else {
            rotate();
          }
        }
        rotate();
      }
      return this;
    }

    public SplayTree delete() {
      splay();
      if (l == null && r == null) {
        return null;
      } else if (l == null) {
        return cutAfter();
      } else if (r == null) {
        return cutBefore();
      } else {
        SplayTree a = cutBefore();
        SplayTree b = cutAfter();
        return a.getLast().insertBefore(b.getFirst());
      }
    }

    public SplayTree cutBefore() {
      splay();
      SplayTree res = l;
      if (res != null) {
        setL(null);
        res.p = null;
        res.splay();
      }
      return res;
    }

    public SplayTree cutAfter() {
      splay();
      SplayTree res = r;
      if (r != null) {
        r.p = null;
        r = null;
        res.splay();
      }
      return res;
    }

    public SplayTree get(int index) {
      SplayTree cur = this;
      while (true) {
        int ls = (cur.l != null ? cur.l.size : 0);
        if (index == ls) return cur.splay();
        if (index < ls) {
          cur = cur.l;
        } else {
          cur = cur.r;
          index -= ls + 1;
        }
      }
    }

    public SplayTree find(T item) {
      SplayTree cur = this;
      SplayTree top = this;
      while (cur != null) {
        top = cur;
        int c = comparator().compare(item, cur.key);
        if (c == 0) return cur.splay();
        cur = c < 0 ? cur.l : cur.r;
      }
      top.splay();
      return null;
    }

    public int indexOf() {
      splay();
      return (l != null ? l.size : 0);
    }
  }
}
