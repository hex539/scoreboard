package me.hex539.resolver;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;

public class ResolverWindow extends Thread {
  private static final boolean PRINT_FPS = false;
  private static final boolean LIMIT_FPS = true;
  private static final long MAX_FPS = 60;

  private final ResolverController resolver;
  private final ScoreboardModel model;
  private final Renderer renderer;
  private final Controller controller;

  private final Semaphore exit = new Semaphore(0);
  private final Semaphore advance = new Semaphore(0);
  private final Semaphore toggleFullscreen = new Semaphore(0);
  private final Semaphore resizeWindow = new Semaphore(0);

  int windowHeight = -1;
  int windowWidth = -1;
  boolean isFullscreen = false;

  public ResolverWindow(
      CompletableFuture<? extends ResolverController> resolver,
      CompletableFuture<? extends ScoreboardModel> model,
      CompletableFuture<? extends ByteBuffer> ttfData) throws Exception {
    this.model = model.get();
    this.resolver = resolver.get();
    this.controller = new Controller(this.resolver);
    this.renderer = new Renderer(this.model, ttfData);

    this.resolver.addObserver(this.renderer);
  }

  public void run() {
    if (!glfwInit()) {
      System.exit(1);
    }
    GLFWErrorCallback.createPrint().set();

    long primaryMonitor = glfwGetPrimaryMonitor();
    GLFWVidMode videoMode = glfwGetVideoMode(primaryMonitor);

    windowHeight = Math.max(videoMode.height() * 3 / 4, 1);
    windowWidth = Math.min(videoMode.width(), windowHeight * 16 / 9);

    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
    final long window = glfwCreateWindow(
        windowWidth,
        windowHeight,
        "Resolver",
        /* display= */ NULL,
        NULL);
    if (window == 0L) {
      System.err.println("Failed to create a GLFW window for OpenGL.");
      System.exit(1);
    }
    glfwMakeContextCurrent(window);
    glfwSetWindowSizeCallback(window, this::onWindowSizeCallback);
    createCapabilities();

    glfwSetKeyCallback(window, this::onKeyCallback);
    renderer.setVideoMode(windowWidth, windowHeight);

    ArrayDeque<Long> frames = PRINT_FPS ? new ArrayDeque<>() : null;
    final long minFrameTime = LIMIT_FPS ? TimeUnit.SECONDS.toNanos(1) / MAX_FPS : 0L;

    for (long lastFrameTime = 0; !glfwWindowShouldClose(window) && !exit.tryAcquire();) {
      if (toggleFullscreen.tryAcquire()) {
        isFullscreen = !isFullscreen;
        if (isFullscreen) {
          primaryMonitor = glfwGetPrimaryMonitor();
          videoMode = glfwGetVideoMode(primaryMonitor);

          glfwSetWindowMonitor(
              window,
              primaryMonitor,
              0, 0, videoMode.width(), videoMode.height(),
              videoMode.refreshRate());
          renderer.setVideoMode(videoMode.width(), videoMode.height());
        } else {
          resizeWindow.release();
        }
      }
      if (resizeWindow.tryAcquire()) {
        glfwSetWindowMonitor(
            window,
            NULL,
            0, 0, windowWidth, windowHeight,
            videoMode.refreshRate());
        renderer.setVideoMode(windowWidth, windowHeight);
      }

      long timeNow = System.nanoTime();
      if (LIMIT_FPS) {
        timeNow += (long) (1e9 / MAX_FPS);
      }

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

  private void onWindowSizeCallback(long win, int width, int height) {
    if (isFullscreen) {
      return;
    }
    windowWidth = width;
    windowHeight = height;
    resizeWindow.release();
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
      case GLFW_KEY_F11:
      case 'F': {
        toggleFullscreen.release();
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
