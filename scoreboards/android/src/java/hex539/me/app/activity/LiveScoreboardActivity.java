package me.hex539.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import me.hex539.app.intent.IntentUtils;
import me.hex539.app.R;
import me.hex539.app.view.ScoreboardRowView;
import org.domjudge.api.DomjudgeRest;
import org.domjudge.api.JudgingDispatcher;
import org.domjudge.api.ScoreboardModel;
import org.domjudge.api.ScoreboardModelImpl;
import org.domjudge.proto.DomjudgeProto;

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

  private DomjudgeRest mApi;
  private ScoreboardModelImpl mScoreboard;
  private JudgingDispatcher mDispatcher;
  private Queue<DomjudgeProto.Submission> mPendingSubmissions = new ArrayDeque<>();
  private Queue<DomjudgeProto.Judging> mPendingJudgings = new ArrayDeque<>();

  @Override
  public void onCreate(Bundle savedState) {
    super.onCreate(savedState);
    if (!IntentUtils.validateIntent(getIntent(), Extras.class, TAG)) {
      finish();
      return;
    }

    setContentView(R.layout.scoreboard);

    mApiHandlerThread = new HandlerThread("api");
    mApiHandlerThread.start();
    mApiHandler = new Handler(mApiHandlerThread.getLooper());

    final String uri = getIntent().getStringExtra(Extras.URI);
    final String username = getIntent().getStringExtra(Extras.USERNAME);
    final String password = getIntent().getStringExtra(Extras.PASSWORD);

    mApiHandler.post(() -> {
      try {
        mApi = new DomjudgeRest(uri);
        if (username != null) {
          mApi.setCredentials(username, password);
        }
        mScoreboard = ScoreboardModelImpl.create(mApi).withoutSubmissions();
        mDispatcher = new JudgingDispatcher(mScoreboard);
        mDispatcher.observers.add(mScoreboard);

        for (DomjudgeProto.Submission submission : mApi.getSubmissions(mScoreboard.getContest())) {
          mPendingSubmissions.add(submission);
        }
        for (DomjudgeProto.Judging judging : mApi.getJudgings(mScoreboard.getContest())) {
          mPendingJudgings.add(judging);
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to fetch active contest", e);
        finish();
        return;
      }
      runOnUiThread(this::initUi);
    });
  }

  private void handleNextSubmission() {
    DomjudgeProto.Submission submission = mPendingSubmissions.poll();
    while (submission != null && submission.getTeam() > 120) {
      submission = mPendingSubmissions.poll();
    }
    if (submission != null) {
      mApiHandler.postDelayed(this::handleNextSubmission, 0);
      mDispatcher.notifySubmission(submission);
    }
    else handleNextJudging();
  }

  private void handleNextJudging() {
    DomjudgeProto.Judging judging = mPendingJudgings.poll();
    while (judging != null) {
      try {
        mDispatcher.notifyJudging(judging);
        mApiHandler.postDelayed(this::handleNextJudging, 100);
        break;
      } catch (Exception e) {
        judging = mPendingJudgings.poll();
      }
    }
  }

  private void initUi() {
    final TextView contestName = ((TextView) findViewById(R.id.contest_name));
    contestName.setText(mScoreboard.getContest().getName());

    final RecyclerView scoreboardRows = (RecyclerView) findViewById(R.id.scoreboard_rows);
    scoreboardRows.setAdapter(new Adapter(mScoreboard, mDispatcher, this::runOnUiThread));

    mApiHandler.post(this::handleNextSubmission);
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
      implements ScoreboardModel.Observer {

    private final ScoreboardModel mModel;
    private final JudgingDispatcher mDispatcher;
    private final Consumer<Runnable> mRunOnUiThread;

    private List<DomjudgeProto.ScoreboardRow> mCurrentRows;

    public Adapter(
        ScoreboardModel model,
        JudgingDispatcher dispatcher,
        Consumer<Runnable> runOnUiThread) {
      mModel = model;
      mDispatcher = dispatcher;
      mRunOnUiThread = runOnUiThread;

      mDispatcher.observers.add(this);

      mCurrentRows = mModel.getRows();
      setHasStableIds(true);
    }

    private void runOnUiThread(Runnable r) {
      mRunOnUiThread.accept(r);
    }

    // RecyclerView.Adapter

    @Override
    public long getItemId(int position) {
      return getTeamAt(position).getId();
    }

    @Override
    public int getItemCount() {
      return mModel.getRows().size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
      return new ViewHolder(new ScoreboardRowView(viewGroup.getContext()));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
      viewHolder.view.setTeam(getTeamAt(position));
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

    // ScoreboardModel.Observer

    @Override
    public void onProblemSubmitted(DomjudgeProto.Team team, DomjudgeProto.Submission submission) {
      final List<DomjudgeProto.ScoreboardRow> nextRows = mModel.getRows();
      final long position = mModel.getRow(team).getRank() - 1;
      runOnUiThread(() -> {
        mCurrentRows = nextRows;
        System.err.println("onProblemSubmitted " + team.getName());
        notifyItemChanged((int) position);
      });
    }

    @Override
    public void onProblemAttempted(
        DomjudgeProto.Team team,
        DomjudgeProto.ScoreboardProblem problem,
        DomjudgeProto.ScoreboardScore score) {
      final List<DomjudgeProto.ScoreboardRow> nextRows = mModel.getRows();
      final long position = mModel.getRow(team).getRank() - 1;
      runOnUiThread(() -> {
        mCurrentRows = nextRows;
        System.err.println("onProblemAttempted " + team.getName());
        notifyItemChanged((int) position);
      });
    }

    @Override
    public void onTeamRankChanged(DomjudgeProto.Team team, int oldRank, int newRank) {
      final List<DomjudgeProto.ScoreboardRow> nextRows = mModel.getRows();
      runOnUiThread(() -> {
        mCurrentRows = nextRows;
        notifyItemMoved(oldRank - 1, newRank - 1);
        System.err.println("notifyItemMoved " + (oldRank - 1) + " " + (newRank - 1));
      });
    }

    // Misc

    private DomjudgeProto.Team getTeamAt(int position) {
      return mModel.getTeam(mModel.getRows().get(position).getTeam());
    }
  }
}
