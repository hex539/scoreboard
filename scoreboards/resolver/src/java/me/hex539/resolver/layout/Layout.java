package me.hex539.resolver.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Layout {

  private final Consumer<Layout> drawFunction;

  public float relX;
  public float relY;

  public float x = 0;
  public float y = 0;

  public float width = 0;
  public float height = 0;

  public Layout(final Consumer<Layout> drawFunction) {
    this.drawFunction = drawFunction;
  }

  public Layout covering(Layout other) {
    return covering(other.x, other.y, other.width, other.height);
  }

  public Layout covering(float x, float y, float width, float height) {
    this.x = (this.relX = x);
    this.y = (this.relY = y);
    this.width = width;
    this.height = height;
    return this;
  }

  public Layout adjust(float x, float y, float width, float height) {
    this.x += x; this.relX += x;
    this.y += y; this.relY += y;
    this.width += width;
    this.height += height;
    return this;
  }

  public void layout() {
    x = relX;
    y = relY;
    layoutChildren();
  }

  private void layoutChildren() {
    for (Layout child : children()) {
      child.x = x + child.relX;
      child.y = y + child.relY;
      child.layoutChildren();
    }
  }

  public void draw() {
    draw(this);
  }

  public void draw(Layout clip) {
    if (drawFunction != null) {
      drawFunction.accept(this);
    }
    for (Layout child : children()) {
      if (clip.intersects(child)) {
        child.draw(clip.intersect(child));
      }
    }
  }

  private boolean intersects(Layout other) {
    return Math.max(x, other.x) < Math.min(x + width, other.x + other.width)
        && Math.max(y, other.y) < Math.min(y + height, other.y + other.height);
  }

  public Layout intersect(Layout other) {
    return new Layout(null).covering(
        Math.max(x, other.x),
        Math.max(y, other.y),
        Math.min(x + width, other.x + other.width) - Math.max(x, other.x),
        Math.min(y + height, other.y + other.height) - Math.max(y, other.y));
  }

  public List<Layout> children() {
    return Collections.emptyList();
  }

  public static class Group extends Layout {

    public Group() {
      super(null);
    }

    public Group(final Consumer<Layout> drawFunction) {
      super(drawFunction);
    }

    public final List<Layout> children = new ArrayList<>();

    public List<Layout> children() {
      return children;
    }
  }
}
