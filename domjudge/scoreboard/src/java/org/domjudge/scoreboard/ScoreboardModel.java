package org.domjudge.scoreboard;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.domjudge.proto.DomjudgeProto.*;

public interface ScoreboardModel {

  public interface Observer {
    default void setModel(ScoreboardModel model) {}
    default void onProblemSubmitted(Team team, Submission submission) {}
    default void onProblemAttempted(Team team, ScoreboardProblem problem, ScoreboardScore score) {}
    default void onTeamRankChanged(Team team, int oldRank, int newRank) {}
  }

  Contest getContest();
  List<Problem> getProblems();
  Collection<Team> getTeams();
  Collection<Category> getCategories();
  List<ScoreboardRow> getRows();

  default Map<String, JudgementType> getJudgementTypes() {
    return DefaultJudgementTypes.get();
  }

  default List<Submission> getSubmissions() {
    return Collections.emptyList();
  }

  default Team getTeam(long id) throws NoSuchElementException {
    return getTeams().stream().filter(x -> x.getId() == id).findFirst().get();
  }

  default Problem getProblem(long id) throws NoSuchElementException {
    return getProblems().stream().filter(x -> x.getId() == id).findFirst().get();
  }

  default Category getCategory(Team team) throws NoSuchElementException {
    return getCategories().stream().filter(x -> x.getId() == team.getCategory()).findFirst().get();
  }

  default ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return getRows().stream().filter(x -> x.getTeam() == team.getId()).findFirst().get();
  }

  default Submission getSubmission(long id) throws NoSuchElementException {
    return getSubmissions().stream().filter(x -> x.getId() == id).findFirst().get();
  }

  default ScoreboardProblem getAttempts(Team team, Problem problem) throws NoSuchElementException {
    return getRow(team).getProblemsList().stream()
            .filter(x -> x.getLabel().equals(problem.getLabel()))
            .findFirst()
            .orElseThrow(
                () -> new NoSuchElementException("Cannot find problem " + problem.getLabel()));
  }
}
