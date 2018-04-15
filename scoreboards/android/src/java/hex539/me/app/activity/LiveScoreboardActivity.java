package me.hex539.app.activity;

import android.app.Activity;
import android.content.Context;
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
import com.google.common.base.Preconditions;
import edu.clics.proto.ClicsProto;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.hex539.app.controller.ResolverHandler;
import me.hex539.app.controller.ScoreboardViewController;
import me.hex539.app.data.ContestList;
import me.hex539.app.data.ScoreboardAdapter;
import me.hex539.app.R;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.ResolverController;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;

public class LiveScoreboardActivity extends Activity {
  private static final String TAG = LiveScoreboardActivity.class.getSimpleName();

  public static class Extras {
    private Extras() {}

    public static final String CONTEST_ID = "contest-id";
  }

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
    final Context context = getBaseContext();

    final String contestId =
        Preconditions.checkNotNull(getIntent().getStringExtra(Extras.CONTEST_ID));

    final CompletableFuture<ClicsProto.ClicsContest> entireContest =
        ContestList.getOrCreate(context).getContest(contestId);

    final CompletableFuture<String> largestGroup =
        entireContest.thenApplyAsync(c -> mostPopularGroup(c).orElse(""));

    final CompletableFuture<ScoreboardModelImpl> referenceModel =
        entireContest.thenCombineAsync(
            largestGroup,
            (clicsContest, groupId) -> ScoreboardModelImpl.newBuilder(clicsContest)
                .filterGroups(g -> groupId.equals(g.getId()))
                .filterTooLateSubmissions()
                .build());

    final CompletableFuture<ScoreboardModelImpl> scoreboardModel =
        entireContest.thenCombineAsync(
            referenceModel,
            (clicsContest, reference) -> ScoreboardModelImpl.newBuilder(clicsContest, reference)
                .withEmptyScoreboard()
                .filterSubmissions(s -> false)
                .build());

    final CompletableFuture<ResolverController> resolver = entireContest
        .thenCombineAsync(referenceModel, ResolverController::new)
        .thenCombineAsync(scoreboardModel, ResolverController::addObserver);

    final CompletableFuture<ScoreboardAdapter> adapter = scoreboardModel
        .thenApplyAsync(model -> new ScoreboardAdapter(model, uiHandler));

    mResolverHandler = new ResolverHandler(mResolverHandlerThread.getLooper(), resolver, adapter);
    mResolverHandler.post(() -> onAdapterLoaded(scoreboardModel.join(), adapter.join()));
  }

  @WorkerThread
  private void onAdapterLoaded(ScoreboardModel model, ScoreboardAdapter adapter) {
    runOnUiThread(() -> initUi(model, adapter));
  }

  @UiThread
  private void initUi(ScoreboardModel model, ScoreboardAdapter adapter) {
    mScoreboardViewController = new ScoreboardViewController(
        (RecyclerView) findViewById(R.id.scoreboard_rows), adapter, model);

    final TextView contestName = ((TextView) findViewById(R.id.contest_name));
    contestName.setText(model.getContest().getName());

    mResolverHandler.postAdvance();
  }

  private static Optional<String> mostPopularGroup(ClicsProto.ClicsContest clicsContest) {
    return clicsContest.getTeamsMap().values().stream().flatMap(t -> t.getGroupIdsList().stream())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet().stream().max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
  }
}
