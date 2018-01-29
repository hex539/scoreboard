package me.hex539.app.controller;

import android.support.annotation.UiThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import edu.clics.proto.ClicsProto;
import me.hex539.app.data.ScoreboardAdapter;
import me.hex539.contest.ScoreboardModel;

public class ScoreboardViewController {
  private final RecyclerView view;
  private final ScoreboardAdapter adapter;
  private final ScoreboardModel model;
  private final LinearLayoutManager layout;

  @UiThread
  public ScoreboardViewController(
      RecyclerView view,
      ScoreboardAdapter adapter,
      ScoreboardModel model) {
    this.view = view;
    this.adapter = adapter;
    this.model = model;
    this.layout = (LinearLayoutManager) (view.getLayoutManager());
    initUi();
  }

  @UiThread
  private void initUi() {
    adapter.addFocusObserver(this::onTeamFocused);

    layout.setStackFromEnd(true);
    view.setHasFixedSize(true);
    view.setAdapter(adapter);
    view.getItemAnimator().setMoveDuration(900L);
    view.setChildDrawingOrderCallback((a, b) -> a-1-b);
    view.scrollToPosition(model.getTeams().size());
  }

  @UiThread
  private void onTeamFocused(ClicsProto.Team team) {
    if (team != null) {
      layout.scrollToPositionWithOffset(adapter.indexOfTeam(team), view.getHeight() / 2);
    } else {
      layout.scrollToPositionWithOffset(0, 0);
    }
  }
}
