package me.hex539.app.activity;

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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import me.hex539.app.intent.IntentUtils;
import me.hex539.app.R;

public class LiveScoreboardActivity extends Activity {
  private static final String TAG = LiveScoreboardActivity.class.getSimpleName();

  public static class Extras {
    private Extras() {}

    public static final String URI = "uri";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
  }

  private Handler mApiHandler;
  private HandlerThread mApiHandlerThread;

  private ContestDownloader mDownloader;
  private ResolverController mResolverController;
  private ClicsProto.ClicsContest mEntireContest;
  private ClicsProto.Contest mContest;
  private ScoreboardModelImpl mModel;
  private Adapter mAdapter;

  @Override
  public void onCreate(Bundle savedState) {
    super.onCreate(savedState);
/*
    if (!IntentUtils.validateIntent(getIntent(), Extras.class, TAG)) {
      finish();
      return;
    }
*/
    setContentView(R.layout.scoreboard);

    mApiHandlerThread = new HandlerThread("api");
    mApiHandlerThread.start();
    mApiHandler = new Handler(mApiHandlerThread.getLooper());

//    final String uri = getIntent().getStringExtra(Extras.URI);
    mApiHandler.post(() -> {
      try (final InputStream nwerc2017 = getAssets().open("contests/nwerc2017.pb")) {
        mDownloader = new ContestDownloader()
            .setStream(nwerc2017)
            .setApi("clics");
        mEntireContest = mDownloader.fetch();
        mContest = mEntireContest.getContest();

        final ScoreboardModelImpl fullModel = ScoreboardModelImpl.newBuilder(mEntireContest)
            .filterGroups(g -> "University of Bath".equals(g.getName()))
            .filterTooLateSubmissions()
            .build();
        mResolverController = new ResolverController(mEntireContest, fullModel);
        mModel = fullModel.toBuilder().withEmptyScoreboard().build();
        mAdapter = new Adapter(
          mModel,
          mResolverController,
          this::runOnUiThread);

        mResolverController.start();
      } catch (Exception e) {
        Log.e(TAG, "Failed to fetch active contest", e);
        finish();
        return;
      }
      runOnUiThread(this::initUi);
    });
  }

  private void initUi() {
    final TextView contestName = ((TextView) findViewById(R.id.contest_name));
    contestName.setText(mModel.getContest().getName());

    final RecyclerView scoreboardRows = (RecyclerView) findViewById(R.id.scoreboard_rows);
    scoreboardRows.setAdapter(mAdapter);

    mResolverController.observers.add(new ResolverController.Observer() {
      @Override
      public void onProblemFocused(ClicsProto.Team team, ClicsProto.Problem problem) {
        runOnUiThread(() -> {
          if (team != null) {
            scoreboardRows.smoothScrollToPosition((int) mModel.getRow(team).getRank() - 1);
          }
        });
      }
    });


    System.err.println("advanceResolver!");
    mApiHandler.post(this::advanceResolver);
  }

  private void advanceResolver() {
    System.err.println("advanceResolver!!!! " + mResolverController.finished());
    if (mResolverController.finished()) {
      return;
    }
    mResolverController.advance();
    mApiHandler.postDelayed(this::advanceResolver, 200);
  }


  @Override
  public void onDestroy() {
    if (mApiHandlerThread != null) {
      mApiHandlerThread.interrupt();
    }
    super.onDestroy();
  }

  private static class ViewHolder extends RecyclerView.ViewHolder {
    final ScoreboardRowView view;

    public ViewHolder(View v) {
      super(v);
      view = (ScoreboardRowView) v;
    }
  }

  private static class Adapter
      extends RecyclerView.Adapter<ViewHolder>
      implements ResolverController.Observer {

    private final ScoreboardModelImpl mModel;
    private final ResolverController mDispatcher;
    private final Consumer<Runnable> mRunOnUiThread;
    private final Map<String, Long> stableIds = new HashMap<>();

    public Adapter(
        ScoreboardModelImpl model,
        ResolverController dispatcher,
        Consumer<Runnable> runOnUiThread) {
      mModel = model;
      mDispatcher = dispatcher;
      mRunOnUiThread = runOnUiThread;

      mDispatcher.observers.add(this);

      setHasStableIds(true);
    }

    private void runOnUiThread(Runnable r) {
      mRunOnUiThread.accept(r);
    }

    // RecyclerView.Adapter

    @Override
    public long getItemId(int position) {
      final String teamId = mModel.getRow(position).getTeamId();
      Long itemId = stableIds.get(teamId);
      if (itemId == null) {
        stableIds.put(teamId, (itemId = Long.valueOf(stableIds.size())));
      }
      return itemId;
    }

    @Override
    public int getItemCount() {
      return mModel.getTeams().size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
      return new ViewHolder(new ScoreboardRowView(viewGroup.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
      ClicsProto.ScoreboardRow row = mModel.getRow(position);
      ClicsProto.Team team = mModel.getTeam(row.getTeamId());
      ClicsProto.Organization organization = mModel.getOrganization(team.getOrganizationId());
      viewHolder.view
        .setRowInfo(ScoreboardRowView.RowInfo.create(row, team, organization))
        .setFocusedProblem(mFocusedTeam == team ? mFocusedProblem : null);
    }

    @Override
    public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
//      if (mDispatcher != null && !hasObservers()) {
//        mDispatcher.observers.add(this);
//      }
      super.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
      super.unregisterAdapterDataObserver(observer);
//      if (mDispatcher != null && !hasObservers()) {
//        mDispatcher.observers.remove(this);
//      }
    }

    // ResolverController.Observer

    private ClicsProto.Team mFocusedTeam;
    private ClicsProto.Problem mFocusedProblem;

    @Override
    public void onProblemFocused(ClicsProto.Team team, ClicsProto.Problem problem) {
      runOnUiThread(() -> {
        if (mFocusedTeam != null) {
          notifyItemChanged((int) mModel.getRow(mFocusedTeam).getRank() - 1);
        }
        if (team != null && problem != null) {
          notifyItemChanged((int) mModel.getRow(team).getRank() - 1);
        }
        mFocusedTeam = team;
        mFocusedProblem = problem;
      });
    }

    @Override
    public synchronized void onProblemSubmitted(ClicsProto.Team team, ClicsProto.Submission submission) {
      runOnUiThread(() -> {
        System.err.println("onProblemSubmitted " + team.getName());
        notifyItemChanged((int) mModel.getRow(team).getRank() - 1);
        mModel.onProblemSubmitted(team, submission);
      });
    }

    @Override
    public synchronized void onSubmissionJudged(ClicsProto.Team team, ClicsProto.Judgement judgement) {
      runOnUiThread(() -> {
        mModel.onSubmissionJudged(team, judgement);
      });
    }

    @Override
    public synchronized void onTeamRankChanged(ClicsProto.Team team, int oldRank, int newRank) {
      runOnUiThread(() -> {
        mModel.onTeamRankChanged(team, oldRank, newRank);
      });
    }

    @Override
    public void onProblemScoreChanged(ClicsProto.Team team, ClicsProto.ScoreboardProblem attempt) {
      runOnUiThread(() -> {
        mModel.onProblemScoreChanged(team, attempt);
        notifyItemChanged((int) mModel.getRow(team).getRank() - 1);
      });
    }

    @Override
    public void onScoreChanged(ClicsProto.Team team, ClicsProto.ScoreboardScore score) {
      runOnUiThread(() -> {
        final int oldRank = (int) mModel.getRow(team).getRank();
        mModel.onScoreChanged(team, score);
        final int newRank = (int) mModel.getRow(team).getRank();

        notifyItemChanged(oldRank - 1);
        if (oldRank != newRank) {
          notifyItemMoved(oldRank - 1, newRank - 1);
        }
      });
    }

    // Misc

    private synchronized ClicsProto.Team getTeamAt(int position) {
      return mModel.getTeam(mModel.getRow(position).getTeamId());
    }
  }
}
