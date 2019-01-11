package me.hex539.resolver;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import edu.clics.proto.ClicsProto.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Particles {
  private final List<Particle> particles = new ArrayList<>();

  private double screenWidth = 1;
  private double screenHeight = 1;

  private double offsetX = 0;
  private double offsetY = 0;

  /** Acceleration due to gravity, in pixels per second². */
  private double g = -1;

  /** Draw radius of one particle. */
  private double r = 1;

  /** Time of last update (for mechanics calculations). */
  private long t0 = 0;

  public void setVideoSize(double width, double height) {
    final double factor = (width / screenWidth);
    for (Particle p : particles) {
      p.x *= factor;
      p.y *= factor;
      p.vx *= factor;
      p.vy *= factor;
    }

    screenWidth = width;
    screenHeight = height;

    g = -screenWidth / 30.0;
    r = screenWidth * (3.0 / 2.0) / 1920.0;
  }

  public void setOffset(double x, double y) {
    offsetX = x;
    offsetY = y;
  }

  public void add(double x, double y, double a, double vx, double vy, double va, float r, float g, float b) {
    Particle p = new Particle();
    p.expires = t0 + (long) (TimeUnit.SECONDS.toNanos(1) * (Math.random() + 0.5) * 0.4);
    p.x = x - offsetX;
    p.y = y - offsetY;
    p.a = a;
    p.vx = vx;
    p.vy = vy;
    p.va = va;
    p.r = r;
    p.g = g;
    p.b = b;
    particles.add(p);
  }

  public boolean update(long t1) {
    double step = (t1 - t0) / 1e9;
    t0 = t1;

    int n = particles.size();
    for (int i = 0; i < particles.size(); i++) {
      Particle p = particles.get(i);
      p.a += p.va;
      p.x += p.vx * step;
      p.y += (p.vy + g / 2.0) * step;
      p.vy += g * step;
      if (p.expires <= t1
          || p.x + offsetX < 0
          || p.x + offsetX > screenWidth
          || p.y + offsetY < 0
          || p.y + offsetY > screenHeight) {
        particles.set(i, particles.get(n-1));
        particles.remove(n-1);
        n--;
        i--;
      }
    }
    return exist();
  }

  public void draw() {
    if (!exist()) {
      return;
    }

    if (r > 0.75) {
      glBegin(GL_QUADS);
      for (Particle i : particles) {
        glColor3f(i.r, i.g, i.b);
        final double ca = Math.cos(i.a);
        final double sa = Math.sin(i.a);
        glVertex2d(i.x + offsetX + r*ca + r*sa, i.y + offsetY - r*sa + r*ca);
        glVertex2d(i.x + offsetX + r*ca - r*sa, i.y + offsetY - r*sa - r*ca);
        glVertex2d(i.x + offsetX - r*ca - r*sa, i.y + offsetY + r*sa - r*ca);
        glVertex2d(i.x + offsetX - r*ca + r*sa, i.y + offsetY + r*sa + r*ca);
      }
      glEnd();
    } else  {
      glBegin(GL_POINTS);
      for (Particle i : particles) {
        glColor3f(i.r, i.g, i.b);
        glVertex2d(i.x + offsetX, i.y + offsetY);
      }
      glEnd();
    }
  }

  public boolean exist() {
    return !particles.isEmpty();
  }

  private static class Particle {
    public long expires;
    public double x;
    public double y;
    public double a;
    public double vx;
    public double vy;
    public double va;
    public float r;
    public float g;
    public float b;
  }
}
