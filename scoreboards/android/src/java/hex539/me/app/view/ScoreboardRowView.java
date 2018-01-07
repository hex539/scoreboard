package me.hex539.app.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import me.hex539.app.R;
import me.hex539.app.intent.IntentUtils;
import org.domjudge.proto.DomjudgeProto;
import org.domjudge.scoreboard.ScoreboardModel;

public class ScoreboardRowView extends LinearLayout implements ScoreboardModel.Observer{

  private TextView mTeamNameView;
  private TextView mTeamAffiliationView;
  private LongSparseArray<View> mProblems = new LongSparseArray<>();

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

  public void setTeam(DomjudgeProto.Team team) {
    mTeamNameView.setText(team.getName());
    mTeamAffiliationView.setText(team.getAffiliation());
  }
/*
  // TODO
  @Override
  public void onProblemSubmitted(Team team, Submission submission) {

  }

  @Override
  public void onProblemAttempted(Team team, ScoreboardProblem problem) {
  }
*/
}
