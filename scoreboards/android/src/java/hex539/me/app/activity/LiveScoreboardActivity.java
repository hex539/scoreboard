package me.hex539.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.clics.proto.ClicsProto;
import java.io.InputStream;
import me.hex539.app.intent.IntentUtils;
import me.hex539.app.R;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import me.hex539.contest.ResolverController;
import me.hex539.app.intent.IntentUtils;
import me.hex539.app.data.ScoreboardAdapter;
import me.hex539.app.R;

public class LiveScoreboardActivity extends Activity {
  private static final String TAG = LiveScoreboardActivity.class.getSimpleName();

  public static class Extras {
    private Extras() {}

    public static final String URI = "uri";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
  }

  private Handler mUiHandler;
  private Handler mApiHandler;
  private HandlerThread mApiHandlerThread;

  private ContestDownloader mDownloader;
  private ResolverController mResolverController;
  private ClicsProto.ClicsContest mEntireContest;
  private ClicsProto.Contest mContest;
  private ScoreboardModelImpl mModel;
  private ScoreboardAdapter mAdapter;

  private RecyclerView mScoreboardRows;
  private LinearLayoutManager mScoreboardLayout;
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

    mUiHandler = new Handler(Looper.getMainLooper());

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

        mModel = ScoreboardModelImpl.newBuilder(mEntireContest)
            .filterGroups(g -> "University of Bath".equals(g.getName()))
            .filterTooLateSubmissions()
            .build();

        ScoreboardModel emptyModel = mModel.toBuilder()
            .filterSubmissions(s -> false)
            .withEmptyScoreboard()
            .build();
        mAdapter = new ScoreboardAdapter(emptyModel, mUiHandler);
        mAdapter.addFocusObserver(this::onTeamFocused);

        mResolverController = new ResolverController(mEntireContest, mModel);
        mResolverController.observers.add(mAdapter);
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

    mScoreboardRows = (RecyclerView) findViewById(R.id.scoreboard_rows);
    mScoreboardRows.setHasFixedSize(true);
    mScoreboardRows.setAdapter(mAdapter);
    mScoreboardRows.getItemAnimator().setMoveDuration(900L);
    mScoreboardRows.setChildDrawingOrderCallback((a, b) -> a-1-b);
    mScoreboardRows.scrollToPosition(mModel.getTeams().size());

    mScoreboardLayout = (LinearLayoutManager) (mScoreboardRows.getLayoutManager());
    mScoreboardLayout.setStackFromEnd(true);

    mApiHandler.post(this::advanceResolver);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mApiHandlerThread == null) {
      mApiHandlerThread = new HandlerThread("api");
      mApiHandlerThread.start();
      mApiHandler = new Handler(mApiHandlerThread.getLooper());
      mApiHandler.post(this::advanceResolver);
    }
  }

  @Override
  public void onPause() {
    if (mApiHandlerThread != null) {
      mApiHandler.removeCallbacksAndMessages(null);
      mApiHandlerThread.interrupt();
      mApiHandlerThread = null;
    }
    super.onPause();
  }

  private void advanceResolver() {
    if (mResolverController.finished()) {
      return;
    }

    long advanceDelay;
    switch (mResolverController.advance()) {
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
 
    mApiHandler.postDelayed(this::advanceResolver, advanceDelay /* milliseconds */);
  }

  private void onTeamFocused(ClicsProto.Team team) {
    if (team != null) {
      mScoreboardLayout.scrollToPositionWithOffset(
          mAdapter.indexOfTeam(team),
          mScoreboardRows.getHeight() / 2);
    }
  }
}
