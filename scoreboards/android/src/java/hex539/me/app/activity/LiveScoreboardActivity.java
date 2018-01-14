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

  private Handler mApiHandler;
  private HandlerThread mApiHandlerThread;

  private ContestDownloader mDownloader;
  private ResolverController mResolverController;
  private ClicsProto.ClicsContest mEntireContest;
  private ClicsProto.Contest mContest;
  private ScoreboardModelImpl mModel;
  private ScoreboardAdapter mAdapter;

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

        mModel = ScoreboardModelImpl.newBuilder(mEntireContest)
            .filterGroups(g -> "University of Bath".equals(g.getName()))
            .filterTooLateSubmissions()
            .build();

        ScoreboardModel emptyModel = mModel.toBuilder()
            .filterSubmissions(s -> false)
            .withEmptyScoreboard()
            .build();
        mAdapter = new ScoreboardAdapter(emptyModel, this::runOnUiThread);

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

    final RecyclerView scoreboardRows = (RecyclerView) findViewById(R.id.scoreboard_rows);
    scoreboardRows.setAdapter(mAdapter);
    scoreboardRows.smoothScrollToPosition(mModel.getTeams().size());
    mApiHandler.post(this::advanceResolver);
  }

  private void advanceResolver() {
    if (mResolverController.finished()) {
      return;
    }
    mResolverController.advance();
    mApiHandler.postDelayed(this::advanceResolver, 500 /* milliseconds */);
  }


  @Override
  public void onDestroy() {
    if (mApiHandlerThread != null) {
      mApiHandlerThread.interrupt();
    }
    super.onDestroy();
  }
}
