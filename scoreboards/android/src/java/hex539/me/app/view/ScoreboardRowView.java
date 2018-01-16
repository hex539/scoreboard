package me.hex539.app.view;

import static android.support.v4.content.ContextCompat.getColor;
import static android.support.v4.content.ContextCompat.getDrawable;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.auto.value.AutoValue;
import edu.clics.proto.ClicsProto;
import java.util.ArrayList;
import java.util.List;
import me.hex539.app.R;

import android.view.ViewGroup.LayoutParams;

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
  private boolean focusedTeam;
  private Long rank;

  private int currentBackground = -1;

  public ScoreboardRowView(Context context) {
    super(context);
    onCreate();
  }

  public ScoreboardRowView(Context context, AttributeSet attrs, int styleAttr, int styleRes) {
    super(context, attrs, styleAttr, styleRes);
    onCreate();
  }

  private void onCreate() {
    inflate(getContext(), R.layout.scoreboard_row, this);
    mTeamNameView = (TextView) findViewById(R.id.team_name);
    mTeamAffiliationView = (TextView) findViewById(R.id.team_affiliation);
  }

  public ScoreboardRowView setRowInfo(RowInfo rowInfo) {
    this.rowInfo = rowInfo;
    return this;
  }

  public ScoreboardRowView setFocusedProblem(ClicsProto.Team team, ClicsProto.Problem problem) {
    if (focusedTeam != (team != null) || focusedProblem != problem) {
      focusedTeam = (team != null);
      focusedProblem = problem;
      setRowInfo(rowInfo);
    }
    return this;
  }

  public ScoreboardRowView setRank(Long rank) {
    this.rank = rank;
    return this;
  }

  private void setRootBackground(int resId, boolean full) {
    if (resId == currentBackground && !full) {
      return;
    }
    currentBackground = resId;
    final ViewGroup root = findViewById(R.id.row_root);
    AnimatedVectorDrawable background = (AnimatedVectorDrawable) getDrawable(getContext(), resId);
    root.setBackground(background);
    if (!full) {
      background.start();
    }
  }

  private void unsetRootBackground(boolean full) {
    currentBackground = -1;
    final ViewGroup root = findViewById(R.id.row_root);
    root.setBackground(getDrawable(getContext(), R.color.row_background));
  }

  public void rebuild(boolean full) {
    mTeamNameView.setText(rowInfo.getTeam().getName());
    mTeamAffiliationView.setText(rowInfo.getOrganization().getName());

    // Update overall focus.
    if (focusedTeam) {
      mTeamNameView.setTextColor(getColor(getContext(), R.color.team_name_focused));
      setRootBackground(R.drawable.row_background_focus, full);
    } else {
      mTeamNameView.setTextColor(getColor(getContext(), R.color.team_name));
      if (rank != null) {
        setRootBackground(rank % 2 == 0
            ? R.drawable.row_background_final_even
            : R.drawable.row_background_final_odd, full);
      } else {
        unsetRootBackground(full);
      }
    }

    // Update problem list.
    final ViewGroup problemRoot = findViewById(R.id.problems);
    List<ClicsProto.ScoreboardProblem> scores = rowInfo.getRow().getProblemsList();
    if (mProblemViews.size() != scores.size()) {
      problemRoot.removeAllViews();
      for (int i = 0; i < scores.size(); i++) {
        ScoreboardProblemView v = new ScoreboardProblemView(getContext());
        v.setLayoutParams(
            new LinearLayout.LayoutParams(
              0,
              LayoutParams.WRAP_CONTENT,
              1f));
        problemRoot.addView(v);
        mProblemViews.add(v);
      }
    }
    for (int i = 0; i < scores.size(); i++) {
      final ClicsProto.ScoreboardProblem p = scores.get(i);
      final ScoreboardProblemView v = mProblemViews.get(i);
      v.setProblem(p);
      v.setFocused(focusedProblem != null && p.getProblemId().equals(focusedProblem.getId()));
    }

    // Update rank text.
    final TextView rankView = (TextView) findViewById(R.id.team_rank);
    if (rank != null) {
      TransitionManager.beginDelayedTransition(findViewById(R.id.row_root));
      rankView.setText(rank.toString());
      rankView.setVisibility(View.VISIBLE);
    } else {
      rankView.setVisibility(View.INVISIBLE);
    }

    ((TextView) findViewById(R.id.team_score)).setText("" + rowInfo.getRow().getScore().getNumSolved());
    ((TextView) findViewById(R.id.team_time)).setText("" + rowInfo.getRow().getScore().getTotalTime());
  }
}
