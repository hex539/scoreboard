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
    if (drawFunction != null) {
      drawFunction.accept(this);
    }
    for (Layout child : children()) {
      if (intersects(child)) {
        child.draw();
      }
    }
  }

  private boolean intersects(Layout other) {
    return Math.max(x, other.x) < Math.min(x + width, other.x + other.width)
        && Math.max(y, other.y) < Math.min(y + height, other.y + other.height);
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
