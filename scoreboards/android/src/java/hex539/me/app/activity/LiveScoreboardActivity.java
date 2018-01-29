package me.hex539.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;
import edu.clics.proto.ClicsProto;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import me.hex539.app.R;
import me.hex539.app.controller.ResolverHandler;
import me.hex539.app.controller.ScoreboardViewController;
import me.hex539.app.data.ScoreboardAdapter;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.ResolverController;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;

public class LiveScoreboardActivity extends Activity {
  private static final String TAG = LiveScoreboardActivity.class.getSimpleName();

  private Handler mUiHandler;
  private HandlerThread mResolverHandlerThread;
  private ResolverHandler mResolverHandler;
  private ScoreboardViewController mScoreboardViewController;

  @Override
  public void onCreate(Bundle savedState) {
    super.onCreate(savedState);
    setContentView(R.layout.scoreboard);

    mUiHandler = new Handler(Looper.getMainLooper());
    mResolverHandlerThread = new HandlerThread("api");
    mResolverHandlerThread.start();

    loadContest();
  }

  @Override
  public void onDestroy() {
    mResolverHandlerThread.interrupt();
    super.onDestroy();
  }

  @UiThread
  private void loadContest() {
    final Handler uiHandler = mUiHandler;

    final CompletableFuture<ClicsProto.ClicsContest> entireContest =
        CompletableFuture.supplyAsync(() -> {
          try (final InputStream nwerc2017 = getAssets().open("contests/nwerc2017.pb")) {
            return new ContestDownloader(nwerc2017).fetch();
          } catch (Exception e) {
            throw new Error();
          }
        });

    final CompletableFuture<ScoreboardModel> scoreboardModel =
        entireContest.thenApplyAsync(
            clicsContest -> ScoreboardModelImpl.newBuilder(clicsContest)
                .filterGroups(g -> "University of Bath".equals(g.getName()))
                .filterTooLateSubmissions()
                .build());

    final CompletableFuture<ResolverController> resolver =
        entireContest.thenCombineAsync(scoreboardModel, ResolverController::new);

    final CompletableFuture<ScoreboardAdapter> adapter = resolver
        .thenApplyAsync(r -> new ScoreboardAdapter(r.getModel(), uiHandler));

    final Runnable initUi = () -> initUi(scoreboardModel.getNow(null), adapter.getNow(null));

    mResolverHandler = new ResolverHandler(mResolverHandlerThread.getLooper(), resolver, adapter);
    mResolverHandler.post(() -> runOnUiThread(initUi));
  }

  @UiThread
  private void initUi(ScoreboardModel model, ScoreboardAdapter adapter) {
    mScoreboardViewController = new ScoreboardViewController(
        (RecyclerView) findViewById(R.id.scoreboard_rows), adapter, model);

    final TextView contestName = ((TextView) findViewById(R.id.contest_name));
    contestName.setText(model.getContest().getName());

    mResolverHandler.postAdvance();
  }
}
