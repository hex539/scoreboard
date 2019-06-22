package me.hex539.resolver.input;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import org.lwjgl.glfw.GLFWGamepadState;

public class Gamepad {

  private static final double AXIS_THRESHOLD = 0.2;
  private static final double SCROLL_THRESHOLD = 0.2;

  private final int joystickId;

  private final GLFWGamepadState[] buffers = {
    GLFWGamepadState.create(),
    GLFWGamepadState.create()
  };

  public Gamepad(int joystickId) {
    this.joystickId = joystickId;
  }

  public static Set<Gamepad> findAll() {
    Set<Gamepad> gamepads = Collections.emptySet();
    for (int joystickId = GLFW_JOYSTICK_1; joystickId <= GLFW_JOYSTICK_LAST; joystickId++) {
      if (glfwJoystickPresent(joystickId) && glfwJoystickIsGamepad(joystickId)) {
        if (gamepads.isEmpty()) {
          gamepads = new HashSet<>();
        }
        gamepads.add(new Gamepad(joystickId));
      }
    }
    return gamepads;
  }

  public boolean update() {
    if (glfwJoystickIsGamepad(joystickId)) {
      GLFWGamepadState tmp = buffers[1];
      buffers[1] = buffers[0];
      buffers[0] = tmp;
      return glfwGetGamepadState(joystickId, buffers[1]);
    } else {
      return false;
    }
  }

  public double getScroll() {
    final double dist1 = buffers[1].axes(GLFW_GAMEPAD_AXIS_LEFT_Y);
    final double dist2 = buffers[1].axes(GLFW_GAMEPAD_AXIS_RIGHT_Y);
    final double dist = 0.0
        + (Math.abs(dist1) >= AXIS_THRESHOLD ? dist1 : 0.0)
        + (Math.abs(dist2) >= AXIS_THRESHOLD ? dist2 : 0.0);
    return Math.abs(dist) >= SCROLL_THRESHOLD ? dist : 0.0;
  }

  // controller.onAdvance();
  public int getPresses() {
    int presses = 0;
    for (int i = 0; i < 15; i++) {
      if (buffers[1].buttons(i) == GLFW_RELEASE && buffers[0].buttons(i) == GLFW_PRESS) {
        presses++;
      }
    }
    return presses;
  }
}
