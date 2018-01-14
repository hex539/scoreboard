package me.hex539.app.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.clics.proto.ClicsProto;
import me.hex539.app.intent.IntentUtils;
import me.hex539.app.R;
import me.hex539.app.view.ScoreboardRowView;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.JudgementDispatcher;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import me.hex539.contest.ResolverController;
import me.hex539.contest.SplayList;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;

public class ScoreboardAdapter extends RecyclerView.Adapter<ScoreboardAdapter.ViewHolder>
    implements ResolverController.Observer {

  public static class ViewHolder extends RecyclerView.ViewHolder {
    final ScoreboardRowView view;

    public ViewHolder(View v) {
      super(v);
      view = (ScoreboardRowView) v;
    }
  }

  private final ScoreboardModel mModel;
  private final SplayList<ClicsProto.ScoreboardRow> mRows;
  private final Consumer<Runnable> mRunOnUiThread;

  private final Map<String, Long> stableIds = new HashMap<>();
  private ClicsProto.Team mFocusedTeam;
  private ClicsProto.Problem mFocusedProblem;

  public ScoreboardAdapter(ScoreboardModel model, Consumer<Runnable> runOnUiThread) {
    mModel = model;
    mRows = new SplayList<>(model.getRows(), ClicsProto.ScoreboardRow::getTeamId);
    mRunOnUiThread = runOnUiThread;
    setHasStableIds(true);
  }

  protected void runOnUiThread(Runnable r) {
    mRunOnUiThread.accept(r);
  }

  // RecyclerView.Adapter

  @Override
  public long getItemId(int position) {
    final String teamId = mRows.get(position).getTeamId();
    Long itemId = stableIds.get(teamId);
    if (itemId == null) {
      stableIds.put(teamId, (itemId = Long.valueOf(stableIds.size())));
    }
    return itemId;
  }

  @Override
  public int getItemCount() {
    return mRows.size();
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
    return new ViewHolder(new ScoreboardRowView(viewGroup.getContext()));
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int position) {
    ClicsProto.ScoreboardRow row = mRows.get(position);
    ClicsProto.Team team = mModel.getTeam(row.getTeamId());
    ClicsProto.Organization organization = mModel.getOrganization(team.getOrganizationId());
    viewHolder.view
      .setRowInfo(ScoreboardRowView.RowInfo.create(row, team, organization))
      .setFocusedProblem(mFocusedTeam == team ? mFocusedProblem : null);
  }

  @Override
  public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
    super.registerAdapterDataObserver(observer);
  }

  @Override
  public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
    super.unregisterAdapterDataObserver(observer);
  }

  private void notifyTeamChanged(ClicsProto.Team team) {
    notifyItemChanged(mRows.indexOfTag(team.getId()), team);
  }

  // ResolverController.Observer

  @Override
  public void onProblemFocused(ClicsProto.Team team, ClicsProto.Problem problem) {
    runOnUiThread(() -> {
      Optional.ofNullable(mFocusedTeam).ifPresent(this::notifyTeamChanged);
      mFocusedTeam = team;
      mFocusedProblem = problem;
      Optional.ofNullable(mFocusedTeam).ifPresent(this::notifyTeamChanged);
    });
  }

  @Override
  public synchronized void onProblemSubmitted(ClicsProto.Team team, ClicsProto.Submission submission) {
    runOnUiThread(() -> notifyTeamChanged(team));
  }

  @Override
  public synchronized void onTeamRankChanged(ClicsProto.Team team, int oldRank, int newRank) {
    runOnUiThread(() -> {
      mRows.add(newRank - 1, mRows.remove(oldRank - 1));
      notifyItemChanged(oldRank - 1);
      notifyItemMoved(oldRank - 1, newRank - 1);
    });
  }

  @Override
  public void onProblemScoreChanged(ClicsProto.Team team, ClicsProto.ScoreboardProblem attempt) {
    runOnUiThread(() -> {
      final int index = mRows.indexOfTag(team.getId());
      if (index == -1) {
        throw new AssertionError("Team " + team + " does not exist");
      }
      mRows.set(index, mRows.get(index).toBuilder()
          .setProblems(mModel.getProblem(attempt.getProblemId()).getOrdinal(), attempt)
          .build());
    });
  }

  @Override
  public void onScoreChanged(ClicsProto.Team team, ClicsProto.ScoreboardScore score) {
    runOnUiThread(() -> {
      final int index = mRows.indexOfTag(team.getId());
      mRows.set(index, mRows.get(index).toBuilder()
          .setScore(score)
          .build());
    });
  }
}
