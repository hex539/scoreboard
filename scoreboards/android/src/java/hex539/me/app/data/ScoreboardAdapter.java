package me.hex539.app.data;

import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import edu.clics.proto.ClicsProto;
import me.hex539.app.R;
import me.hex539.app.view.ScoreboardRowView;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;
import me.hex539.contest.SplayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ScoreboardAdapter extends RecyclerView.Adapter<ScoreboardAdapter.ViewHolder> 
    implements ResolverController.Observer {

  public interface FocusObserver {
    void onTeamFocused(ClicsProto.Team team);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    final ScoreboardRowView view;

    public ViewHolder(View v) {
      super(v);
      view = (ScoreboardRowView) v;
    }
  }

  private final ScoreboardModel mModel;
  private final SplayList<ClicsProto.ScoreboardRow> mRows;
  private final Handler mHandler;

  private final Map<String, Long> stableIds = new HashMap<>();
  private final Set<String> finalisedTeams = new HashSet<>();

  private final Set<FocusObserver> focusObservers = new HashSet<>();
  private ClicsProto.Team mFocusedTeam;
  private ClicsProto.Problem mFocusedProblem;

  private boolean everHadObservers;
  private long lastEventTime;

  private static final ViewGroup.LayoutParams layoutParams =
      new ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT);

  public ScoreboardAdapter(ScoreboardModel model, Handler handler) {
    mModel = model;
    mHandler = handler;
    mRows = new SplayList<>(model.getRows(), ClicsProto.ScoreboardRow::getTeamId);
    setHasStableIds(true);
  }

  public void addFocusObserver(FocusObserver f) {
    mHandler.post(() -> focusObservers.add(f));
  }

  public void removeFocusObserver(FocusObserver f) {
    mHandler.post(() -> focusObservers.remove(f));
  }

  public int indexOfTeam(ClicsProto.Team team) {
    return mRows.indexOfTag(team.getId());
  }

  protected void runInOrder(Runnable r) {
    runInOrder(r, 1L);
  }

  protected void runInOrder(Runnable r, long delayBefore) {
    if (!everHadObservers) {
      r.run();
    } else {
      lastEventTime = Math.max(lastEventTime + delayBefore, SystemClock.uptimeMillis());
      mHandler.postAtTime(r, lastEventTime);
    }
  }

  private void notifyTeamChanged(ClicsProto.Team team) {
    notifyItemChanged(mRows.indexOfTag(team.getId()), team);
  }

  private void notifyTeamFocused(ClicsProto.Team team) {
    notifyTeamChanged(team);
    focusObservers.forEach(x -> x.onTeamFocused(team));
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
    ScoreboardRowView view = new ScoreboardRowView(viewGroup.getContext());
    view.setLayoutParams(layoutParams);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int position) {
    onBindViewHolder(viewHolder, position, Collections.emptyList());
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int position, List<Object> payloads) {
    ClicsProto.ScoreboardRow row = mRows.get(position);
    ClicsProto.Team team = mModel.getTeam(row.getTeamId());
    ClicsProto.Organization organization = mModel.getOrganization(team.getOrganizationId());
    viewHolder.view
      .setRowInfo(
          ScoreboardRowView.RowInfo.create(row, team, organization))
      .setFocusedProblem(
          mFocusedTeam == team ? team : null,
          mFocusedTeam == team ? mFocusedProblem : null)
      .setRank(
          finalisedTeams.contains(team.getId()) ? Long.valueOf(position + 1) : null)
      .rebuild(payloads.isEmpty());
  }

  @Override
  public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
    super.registerAdapterDataObserver(observer);
    everHadObservers = true;
  }

  @Override
  public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
    super.unregisterAdapterDataObserver(observer);
  }

  // ResolverController.Observer

  @Override
  public void onProblemFocused(ClicsProto.Team team, ClicsProto.Problem problem) {
    runInOrder(() -> {
      Optional.ofNullable(mFocusedTeam).ifPresent(this::notifyTeamChanged);
      mFocusedTeam = team;
      mFocusedProblem = problem;
      Optional.ofNullable(mFocusedTeam).ifPresent(this::notifyTeamFocused);
    });
  }

  @Override
  public void onTeamRankChanged(ClicsProto.Team team, int oldRank, int newRank) {
    runInOrder(() -> {
      mRows.add(newRank - 1, mRows.remove(oldRank - 1));
      notifyItemMoved(oldRank - 1, newRank - 1);
    }, 50 /* milliseconds */);
  }

  @Override
  public void onTeamRankFinalised(ClicsProto.Team team, int rank) {
    runInOrder(() -> {
      finalisedTeams.add(team.getId());
      notifyTeamChanged(team);
    });
  }

  @Override
  public void onProblemScoreChanged(ClicsProto.Team team, ClicsProto.ScoreboardProblem attempt) {
    runInOrder(() -> {
      final int index = mRows.indexOfTag(team.getId());
      if (index == -1) {
        throw new AssertionError("Team " + team + " does not exist");
      }
      mRows.set(index, mRows.get(index).toBuilder()
          .setProblems(mModel.getProblem(attempt.getProblemId()).getOrdinal(), attempt)
          .build());
      notifyItemChanged(index, team);
    });
  }

  @Override
  public void onScoreChanged(ClicsProto.Team team, ClicsProto.ScoreboardScore score) {
    runInOrder(() -> {
      final int index = mRows.indexOfTag(team.getId());
      mRows.set(index, mRows.get(index).toBuilder()
          .setScore(score)
          .build());
      notifyItemChanged(index, team);
    });
  }    
}
