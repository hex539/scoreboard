package me.hex539.resolver;


import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ResolverController;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Controller {
  private final ResolverController resolver;

  private final Semaphore canAdvance = new Semaphore(0);
  private final PriorityBlockingQueue<Event> events = new PriorityBlockingQueue<>();

  public Controller(final ResolverController resolver) {
    this.resolver = resolver;
    if (resolver.advance() != ResolverController.Resolution.STARTED) {
      throw new IllegalArgumentException("Resolver did not return STARTED on first call");
    }
  }

  public void onAdvance() {
    events.clear();
    if (canAdvance.availablePermits() == 0) {
      canAdvance.release();
    }
  }

  public boolean mainLoop(long timeNow) {
    boolean active = false;
    while (true) {
      Event event = events.peek();
      if (event != null && event.runAt <= timeNow) {
        events.poll().run.run();
        active = true;
      } else {
        break;
      }
    }
    while (canAdvance.tryAcquire()) {
      advance();
    }
    return active || !events.isEmpty();
  }

  public void post(long runIn, Runnable run) {
    events.offer(new Event(System.nanoTime() + runIn, run));
  }

  private void advance() {
    switch (resolver.advance()) {
      case FAILED_PROBLEM:
        post(TimeUnit.MILLISECONDS.toNanos(400), this::advance);
        break;
      case SOLVED_PROBLEM:
        post(TimeUnit.MILLISECONDS.toNanos(1400), this::advance);
        break;
      case FOCUSED_TEAM:
        post(TimeUnit.MILLISECONDS.toNanos(0), this::advance);
        break;
      case FOCUSED_PROBLEM:
        post(TimeUnit.MILLISECONDS.toNanos(600), this::advance);
        break;
      case FINALISED_RANK:
      case FINISHED:
        break;
      default:
        post(TimeUnit.MILLISECONDS.toNanos(40), this::advance);
        break;
    }
  }

  private class Event implements Comparable<Event> {
    public final long runAt;
    public final Runnable run;

    public Event(long runAt, Runnable run) {
      this.runAt = runAt;
      this.run = run;
    }

    @Override
    public int compareTo(Event other) {
      return Long.compare(runAt, other.runAt);
    }
  }
}
