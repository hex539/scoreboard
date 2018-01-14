package me.hex539.app.view;

import static android.support.v4.content.ContextCompat.getColor;

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

public class ScoreboardProblemView extends LinearLayout {

  private TextView mScoreView;
  private ClicsProto.ScoreboardProblem mProblem;
  private boolean mFocused = false;

  public ScoreboardProblemView(Context context) {
    super(context);
    onCreate();
  }

  public ScoreboardProblemView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    onCreate();
  }

  private void onCreate() {
    inflate(getContext(), R.layout.scoreboard_problem, this);
    mScoreView = (TextView) findViewById(R.id.score);
  }

  public void setProblem(ClicsProto.ScoreboardProblem problem) {
    mProblem = problem;
    updateAppearance();
  }

  public void setFocused(boolean focused) {
    mFocused = focused;
    updateAppearance();
  }

  private void updateAppearance() {
    if (mProblem.getSolved()) {
      if (mProblem.getNumJudged() > 1) {
        mScoreView.setText("+" + (mProblem.getNumJudged() - 1));
      } else {
        mScoreView.setText("+");
      }
      mScoreView.setBackgroundResource(R.color.problem_correct);
    } else if (mProblem.getNumPending() > 0) {
      mScoreView.setText("?");
      mScoreView.setBackgroundResource(R.color.problem_pending);
    } else if (mProblem.getNumJudged() > 0) {
      mScoreView.setText("-" + (mProblem.getNumJudged()));
      mScoreView.setBackgroundResource(R.color.problem_incorrect);
    } else {
      mScoreView.setText(" ");
      mScoreView.setBackgroundResource(R.color.problem_unattempted);
    }

    if (mFocused) {
      mScoreView.setBackgroundResource(R.color.problem_label_focused);
    }
  }
}
