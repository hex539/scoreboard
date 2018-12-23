package me.hex539.resolver;


import java.util.concurrent.Semaphore;


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

    while (!glfwWindowShouldClose(window) && !exit.tryAcquire()) {
      glClear(GL_COLOR_BUFFER_BIT);

      final long timeNow = System.nanoTime();

      boolean active = false;
      active |= renderer.mainLoop(timeNow);
      active |= controller.mainLoop(timeNow);
      glfwSwapBuffers(window);

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
