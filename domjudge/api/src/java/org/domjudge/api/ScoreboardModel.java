package org.domjudge.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.domjudge.proto.DomjudgeProto.*;

public interface ScoreboardModel {

  public interface Observer {
    default void setModel(ScoreboardModel model) {}
    default void onProblemSubmitted(Team team, Submission submission) {}
    default void onProblemAttempted(Team team, ScoreboardProblem problem) {}
    default void onTeamRankChanged(Team team, int oldRank, int newRank) {}
  }

  Contest getContest();
  Collection<Problem> getProblems();
  Collection<Team> getTeams();
  Collection<ScoreboardRow> getRows();

  default Collection<Submission> getSubmissions() {
    return Collections.emptySet();
  }

  default Team getTeam(long id) throws NoSuchElementException {
    return getTeams().stream().filter(x -> x.getId() == id).findFirst().get();
  }

  default Problem getProblem(long id) throws NoSuchElementException {
    return getProblems().stream().filter(x -> x.getId() == id).findFirst().get();
  }

  default ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return getRows().stream().filter(x -> x.getTeam() == team.getId()).findFirst().get();
  }

  default Submission getSubmission(long id) throws NoSuchElementException {
    return getSubmissions().stream().filter(x -> x.getId() == id).findFirst().get();
  }

  default ScoreboardProblem getAttempts(Team team, Problem problem) throws NoSuchElementException {
    final Optional<ScoreboardProblem> match =
        getRow(team).getProblemsList().stream()
            .filter(x -> x.getLabel().equals(problem.getLabel()))
            .findFirst();
    if (!match.isPresent()) {
      System.out.println("Cannot find " + problem.getLabel() + " for " + team);
    }
    return match.get();
  }
}
