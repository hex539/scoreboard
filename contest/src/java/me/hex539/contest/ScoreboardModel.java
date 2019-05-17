package me.hex539.contest;

import java.util.List;
import java.util.NoSuchElementException;

import me.hex539.contest.model.Judge;
import me.hex539.contest.model.Problems;
import me.hex539.contest.model.Ranklist;
import me.hex539.contest.model.Teams;

import edu.clics.proto.ClicsProto.*;

public interface ScoreboardModel extends Problems, Ranklist {
  public interface Observer{
    default void setModel(ScoreboardModel model) {}
    default void onProblemSubmitted(Team team, Submission submission) {}
    default void onSubmissionJudged(Team team, Judgement judgement) {}
    default void onProblemScoreChanged(Team team, ScoreboardProblem problem) {}
    default void onScoreChanged(Team team, ScoreboardScore score) {}
    default void onTeamRankChanged(Team team, int oldRank, int newRank) {}
  }

  List<ScoreboardRow> getRows();
  Contest getContest();

  Teams getTeamsModel();
  default Problems getProblemsModel() {return this;}
  default Ranklist getRanklistModel() {return this;}
  Judge getJudgeModel();

  default ScoreboardRow getRow(long index) throws NoSuchElementException {
    return getRows().get((int) index);
  }

  default ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return getRows().stream().filter(x -> team.getId().equals(x.getTeamId())).findFirst().get();
  }

  default ScoreboardScore getScore(Team team) throws NoSuchElementException {
    return getRow(team).getScore();
  }

  default ScoreboardProblem getAttempts(Team team, Problem problem) throws NoSuchElementException {
    try {
      return getRow(team).getProblemsList().stream()
              .filter(x -> x.getProblemId().equals(problem.getId()))
              .findFirst()
              .orElseThrow(
                  () -> new NoSuchElementException("Cannot find problem " + problem.getLabel()));
    } catch (Throwable fromOrElseThrow) {
      throw (NoSuchElementException) fromOrElseThrow;
    }
  }

  default ScoreboardModel immutable() {
    return ImmutableScoreboardModel.of(this);
  }
}
