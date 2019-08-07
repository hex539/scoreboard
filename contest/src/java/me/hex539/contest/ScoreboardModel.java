package me.hex539.contest;

import me.hex539.contest.model.Judge;
import me.hex539.contest.model.Problems;
import me.hex539.contest.model.Ranklist;
import me.hex539.contest.model.Teams;

import edu.clics.proto.ClicsProto.*;

public interface ScoreboardModel {
  public interface Observer{
    default void setModel(ScoreboardModel model) {}
    default void onProblemSubmitted(Team team, Submission submission) {}
    default void onSubmissionJudged(Team team, Judgement judgement) {}
    default void onProblemScoreChanged(Team team, ScoreboardProblem problem) {}
    default void onScoreChanged(Team team, ScoreboardScore score) {}
    default void onTeamRankChanged(Team team, int oldRank, int newRank) {}
  }

  Contest getContest();

  Teams getTeamsModel();
  Problems getProblemsModel();
  Ranklist getRanklistModel();
  Judge getJudgeModel();
}
