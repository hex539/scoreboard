package me.hex539.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.app.FragmentActivity;
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

import me.hex539.app.fragment.ContestListFragment;

public class SettingsActivity extends FragmentActivity {
  private static final String TAG = SettingsActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedState) {
    super.onCreate(savedState);

    if (savedState == null) {
      getFragmentManager()
          .beginTransaction()
          .replace(android.R.id.content, new ContestListFragment())
          .commit();
    }
  }
}
