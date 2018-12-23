package me.hex539.resolver;

import java.util.ArrayDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;

import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ResolverWindow extends Thread {
  private static final boolean PRINT_FPS = false;
  private static final boolean LIMIT_FPS = true;
  private static final long MAX_FPS = 30;

  private final ResolverController resolver;
  private final ScoreboardModel model;
  private final Renderer renderer;
  private final Controller controller;

  private final Semaphore exit = new Semaphore(0);
  private final Semaphore advance = new Semaphore(0);

  public ResolverWindow(ResolverController resolver, ScoreboardModel model) {
    this.resolver = resolver;
    this.model = model;
    this.controller = new Controller(resolver);
    this.renderer = new Renderer(model);

    this.resolver.addObserver(this.renderer);
  }

  public void run() {
    if (!glfwInit()) {
      System.exit(1);
    }

    long primaryMonitor = glfwGetPrimaryMonitor();
    GLFWVidMode videoMode = glfwGetVideoMode(primaryMonitor);

    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    final long window = glfwCreateWindow(
        videoMode.width(),
        videoMode.height(),
        "Resolver",
        primaryMonitor,
        NULL);
    glfwMakeContextCurrent(window);
    createCapabilities();

    glfwSetKeyCallback(window, this::onKeyCallback);
    renderer.setVideoMode(videoMode);

    ArrayDeque<Long> frames = PRINT_FPS ? new ArrayDeque<>() : null;
    final long minFrameTime = LIMIT_FPS ? TimeUnit.SECONDS.toNanos(1) / MAX_FPS : 0L;

    for (long lastFrameTime = 0; !glfwWindowShouldClose(window) && !exit.tryAcquire();) {
      long timeNow = System.nanoTime();

      if (PRINT_FPS) {
        frames.add(timeNow);
        while (frames.peekFirst() <= timeNow - TimeUnit.SECONDS.toNanos(1)) {
          frames.pollFirst();
        }
        System.err.println("FPS: " + frames.size());
      }

      boolean active = false;
      glClear(GL_COLOR_BUFFER_BIT);
      active |= renderer.mainLoop(timeNow);
      active |= controller.mainLoop(timeNow);
      glfwSwapBuffers(window);

      if (LIMIT_FPS) {
        long sleepDuration = (lastFrameTime + minFrameTime) - timeNow;
        if (sleepDuration > 0) {
          try {
            Thread.sleep(sleepDuration / 1000000, (int) (sleepDuration % 1000000));
          } catch (InterruptedException e) {
          }
          timeNow = lastFrameTime + minFrameTime;
        }
      }
      lastFrameTime = timeNow;

      active = true;
      if (active) {
        glfwPollEvents();
      } else {
        glfwWaitEvents();
      }
    }
    glfwTerminate();
  }

  private void onKeyCallback(long win, int key, int scancode, int action, int mods) {
    if (action != GLFW_RELEASE && action != GLFW_REPEAT) {
      return;
    }
    switch (key) {
      case GLFW_KEY_ESCAPE: {
        exit.release();
        return;
      }
      case GLFW_KEY_ENTER:
      case GLFW_KEY_SPACE:
      case GLFW_KEY_UP:
      case GLFW_KEY_RIGHT:
      case GLFW_KEY_LEFT:
      case GLFW_KEY_DOWN: {
        controller.onAdvance();
        return;
      }
      default: {
        return;
      }
    }
  }
}
