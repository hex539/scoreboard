package me.hex539.app.view;

import static android.support.v4.content.ContextCompat.getDrawable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.auto.value.AutoValue;
import edu.clics.proto.ClicsProto;
import java.util.ArrayList;
import java.util.List;
import me.hex539.app.R;
import me.hex539.app.intent.IntentUtils;
import me.hex539.contest.ScoreboardModel;

public class ScoreboardRowView extends LinearLayout {

  @AutoValue
  public static abstract class RowInfo {
    public abstract ClicsProto.ScoreboardRow getRow();
    public abstract ClicsProto.Team getTeam();
    public abstract ClicsProto.Organization getOrganization();

    public static RowInfo create(
        ClicsProto.ScoreboardRow row,
        ClicsProto.Team team,
        ClicsProto.Organization organization) {
      return new AutoValue_ScoreboardRowView_RowInfo(row, team, organization);
    }
  }

  private TextView mTeamNameView;
  private TextView mTeamAffiliationView;
  private List<ScoreboardProblemView> mProblemViews = new ArrayList<>();
  private RowInfo rowInfo;
  private ClicsProto.Problem focusedProblem;

  public ScoreboardRowView(Context context) {
    super(context);
    onCreate();
  }

  public ScoreboardRowView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    onCreate();
  }

  private void onCreate() {
    inflate(getContext(), R.layout.scoreboard_row, this);
    mTeamNameView = (TextView) findViewById(R.id.team_name);
    mTeamAffiliationView = (TextView) findViewById(R.id.team_affiliation);
  }

  public ScoreboardRowView setRowInfo(RowInfo rowInfo) {
    this.rowInfo = rowInfo;
    mTeamNameView.setText(rowInfo.getTeam().getName());
    mTeamAffiliationView.setText(rowInfo.getOrganization().getName());

    final ViewGroup problemRoot = findViewById(R.id.problems);
//    if (mProblemViews.size() != rowInfo.getProblemsList().size()) {
//    }
    problemRoot.removeAllViews();
    mProblemViews.clear();

    final ViewGroup root = findViewById(R.id.row_root);
    if (focusedProblem != null) {
      root.setBackground(getDrawable(getContext(), R.color.row_background_focused));
    } else {
      root.setBackground(getDrawable(getContext(), R.color.row_background));
    }

    for (ClicsProto.ScoreboardProblem p : rowInfo.getRow().getProblemsList()) {
      ScoreboardProblemView v = new ScoreboardProblemView(getContext());
      v.setProblem(p);
      if (focusedProblem != null && p.getProblemId().equals(focusedProblem.getId())) {
        v.setFocused(true);
      }
      problemRoot.addView(v);
      mProblemViews.add(v);
    }
    return this;
  }

  public ScoreboardRowView setFocusedProblem(ClicsProto.Problem problem) {
    if (focusedProblem != problem) {
      focusedProblem = problem;
      setRowInfo(rowInfo);
    }
    return this;
  }
}
