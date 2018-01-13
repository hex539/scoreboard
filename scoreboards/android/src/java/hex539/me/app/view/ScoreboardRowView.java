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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.auto.value.AutoValue;
import edu.clics.proto.ClicsProto;
import me.hex539.app.R;
import me.hex539.app.intent.IntentUtils;
import me.hex539.contest.ScoreboardModel;

public class ScoreboardRowView extends LinearLayout implements ScoreboardModel.Observer {

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
  private RowInfo rowInfo;

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

  public void setRowInfo(RowInfo rowInfo) {
    this.rowInfo = rowInfo;
    mTeamNameView.setText(rowInfo.getTeam().getName());
    mTeamAffiliationView.setText(rowInfo.getOrganization().getName());
  }

  public void setTeam(ClicsProto.Team team, ClicsProto.Organization organization) {
    mTeamNameView.setText(team.getName());
    mTeamAffiliationView.setText(organization.getName());
  }
/*
  // TODO
  @Override
  public void onProblemSubmitted(Team team, Submission submission) {

  }

  @Override
  public void onScoreChanged(Team team, ScoreboardProblem problem) {
  }
*/
}
