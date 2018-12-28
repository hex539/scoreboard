package me.hex539.resolver;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ResolverController;

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
    canAdvance.release();
  }

  public boolean mainLoop(long timeNow) {
    boolean active = false;
    while (canAdvance.tryAcquire()) {
      active = true;
      while (!advance()) {
        continue;
      }
    }
    for (Event event; (event = events.peek()) != null && event.runAt <= timeNow;) {
      events.poll().run.run();
      active = true;
    }
    return active || !events.isEmpty();
  }

  public void post(long runIn, Runnable run) {
    events.offer(new Event(System.nanoTime() + runIn, run));
  }

  private boolean advance() {
    switch (resolver.advance()) {
      case FAILED_PROBLEM:
        post(TimeUnit.MILLISECONDS.toNanos(400), this::advance);
        break;
      case SOLVED_PROBLEM:
        post(TimeUnit.MILLISECONDS.toNanos(800), this::advance);
        break;
      case FOCUSED_TEAM:
        post(TimeUnit.MILLISECONDS.toNanos(200), this::advance);
        break;
      case FOCUSED_PROBLEM:
        post(TimeUnit.MILLISECONDS.toNanos(1200), this::advance);
        break;
      case FINALISED_RANK:
        return false;
      case FINISHED:
        break;
      default:
        post(TimeUnit.MILLISECONDS.toNanos(0), this::advance);
        break;
    }
    return true;
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
