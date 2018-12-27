package me.hex539.contest;

import java.util.Comparator;

final class SplayTree<T> {
  int size = 1;
  SplayTree<T> l = null;
  SplayTree<T> r = null;
  SplayTree<T> p = null;
  public T key;

  public SplayTree(T key) {
    this.key = key;
  }

  public SplayTree<T> insertInto(SplayTree<T> parent, Comparator<? super T> comparator) {
    while (true) {
      if (parent == null) {
        p = null;
      } else {
        final int side = comparator.compare(key, parent.key);
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

  public SplayTree<T> insertBefore(SplayTree<T> parent) {
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

  public SplayTree<T> insertAfter(SplayTree<T> parent) {
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

  public SplayTree<T> getFirst() {
    SplayTree<T> res = this;
    while (res.l != null) {
      res = res.l;
    }
    return res.splay();
  }

  public SplayTree<T> getLast() {
    SplayTree<T> res = this;
    while (res.r != null) {
      res = res.r;
    }
    return res.splay();
  }
  private void setL(SplayTree<T> l) {
    if ((this.l = l) != null) l.p = this;
    updateSize();
  }

  private void setR(SplayTree<T> r) {
    if ((this.r = r) != null) r.p = this;
    updateSize();
  }

  private void updateSize() {
    size = 1 + (l != null ? l.size : 0) + (r != null ? r.size : 0);
  }

  private void rol() {
    SplayTree<T> a = p, b = this, c = l;
    b.setL(c.r);
    c.setR(b);
    if (a != null) {if (a.l == b) a.setL(c); else a.setR(c);} else c.p = null;
  }

  private void ror() {
    SplayTree<T> a = p, b = this, c = r;
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

  public SplayTree<T> splay() {
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

  public SplayTree<T> delete() {
    splay();
    if (l == null && r == null) {
      return null;
    } else if (l == null) {
      return cutAfter();
    } else if (r == null) {
      return cutBefore();
    } else {
      SplayTree<T> a = cutBefore();
      SplayTree<T> b = cutAfter();
      return a.getLast().insertBefore(b.getFirst());
    }
  }

  public SplayTree<T> cutBefore() {
    splay();
    SplayTree<T> res = l;
    if (res != null) {
      setL(null);
      res.p = null;
      res.splay();
    }
    return res;
  }

  public SplayTree<T> cutAfter() {
    splay();
    SplayTree<T> res = r;
    if (r != null) {
      r.p = null;
      r = null;
      res.splay();
    }
    return res;
  }

  public SplayTree<T> get(int index) {
    SplayTree<T> cur = this;
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

  public SplayTree<T> find(T item, Comparator<? super T> comparator) {
    SplayTree<T> cur = this;
    SplayTree<T> top = this;
    while (cur != null) {
      top = cur;
      int c = comparator.compare(item, cur.key);
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
