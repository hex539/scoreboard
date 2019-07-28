package me.hex539.contest.model;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import edu.clics.proto.ClicsProto.*;

public interface Ranklist {
  public interface Observer {
    default void onProblemScoreChanged(Team team, ScoreboardProblem problem) {}
    default void onScoreChanged(Team team, ScoreboardScore score) {}
    default void onTeamRankChanged(Team team, int oldRank, int newRank) {}
  }

  default List<ScoreboardRow> getRows() {
    return Collections.emptyList();
  }

  default ScoreboardRow getRow(long index) throws NoSuchElementException {
    return Optional.ofNullable(getRows().get((int) index)).get();
  }

  default ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return getRows().stream().filter(x -> team.getId().equals(x.getTeamId())).findFirst().get();
  }

  default long getRank(Team team) throws NoSuchElementException {
    return getRow(team).getRank();
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
}
