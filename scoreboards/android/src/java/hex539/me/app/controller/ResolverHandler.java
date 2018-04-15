package me.hex539.app.controller;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import java.util.concurrent.Future;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;
import me.hex539.app.data.ScoreboardAdapter;

public class ResolverHandler extends Handler {
  private ScoreboardAdapter adapter;
  private ResolverController resolver;

  @UiThread
  public ResolverHandler(
      Looper looper, Future<ResolverController> resolver, Future<ScoreboardAdapter> adapter) {
    super(looper);
    post(() -> onCreate(resolver, adapter));
  }

  @WorkerThread
  private void onCreate(Future<ResolverController> resolver, Future<ScoreboardAdapter> adapter) {
    try {
      this.resolver = resolver.get();
      this.adapter = adapter.get();
    } catch (Exception e) {
      throw new Error(e);
    }
    this.resolver.addObserver(this.adapter);
    this.resolver.advance();
  }

  @UiThread
  public void postAdvance() {
    post(this::advanceResolver);
  }

  @WorkerThread
  private void advanceResolver() {
    if (resolver.finished()) {
      this.resolver.removeObserver(this.adapter);
      return;
    }

    final long advanceDelay;
    switch (resolver.advance()) {
      case SOLVED_PROBLEM:
        advanceDelay = 1100;
        break;
      case FOCUSED_TEAM:
        advanceDelay = 1000;
        break;
      default:
        advanceDelay = 200;
        break;
    }

    postDelayed(this::advanceResolver, advanceDelay /* milliseconds */);
  }
}
