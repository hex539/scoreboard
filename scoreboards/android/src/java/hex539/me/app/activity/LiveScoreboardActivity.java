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
import java.io.InputStream;
import me.hex539.app.intent.IntentUtils;
import me.hex539.app.R;
import me.hex539.app.view.ScoreboardRowView;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import edu.clics.proto.ClicsProto;

public class LiveScoreboardActivity extends Activity {
  private static final String TAG = LiveScoreboardActivity.class.getSimpleName();

  public static class Extras {
    private Extras() {}

    public static String URI = "uri";
  }

  private Handler mApiHandler;
  private HandlerThread mApiHandlerThread;

  private ContestDownloader mDownloader;
  private ClicsProto.ClicsContest mEntireContest;
  private ClicsProto.Contest mContest;
  private ScoreboardModel mModel;

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
            .build();
      } catch (Exception e) {
        Log.e(TAG, "Failed to fetch active contest", e);
        finish();
        return;
      }
      runOnUiThread(() -> {
        ((TextView) findViewById(R.id.contest_name)).setText(mContest.getName());
        final RecyclerView scoreboardRows = (RecyclerView) findViewById(R.id.scoreboard_rows);
        scoreboardRows.setAdapter(new Adapter(mModel));
      });
    });
  }

  private static class ViewHolder extends RecyclerView.ViewHolder {
    final ScoreboardRowView view;

    public ViewHolder(View v) {
      super(v);
      view = (ScoreboardRowView) v;
    }
  }

  private static class Adapter extends RecyclerView.Adapter<ViewHolder> {
    private final ScoreboardModel mModel;

    public Adapter(ScoreboardModel model) {
      mModel = model;
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
      viewHolder.view.setRowInfo(ScoreboardRowView.RowInfo.create(row, team, organization));
    }
  }

  @Override
  public void onDestroy() {
    if (mApiHandlerThread != null) {
      mApiHandlerThread.interrupt();
    }
    super.onDestroy();
  }
}
