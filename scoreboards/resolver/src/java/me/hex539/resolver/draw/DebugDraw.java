package me.hex539.resolver.draw;

import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import me.hex539.resolver.layout.Layout;

public final class DebugDraw {
  private DebugDraw() {}

  public static void wireframe(Layout layout) {
    float m = 2.0f;

    glColor3f(1.0f, 1.0f, 1.0f);
    glBegin(GL_LINES);

    glVertex2f(layout.x - m, layout.y);
    glVertex2f(layout.x + layout.width + m, layout.y);

    glVertex2f(layout.x - m, layout.y + layout.height);
    glVertex2f(layout.x + layout.width + m, layout.y + layout.height);

    glVertex2f(layout.x, layout.y - m);
    glVertex2f(layout.x, layout.y + m + layout.height);

    glVertex2f(layout.x + layout.width, layout.y - m);
    glVertex2f(layout.x + layout.width, layout.y + m + layout.height);

    glEnd();
  }
}
