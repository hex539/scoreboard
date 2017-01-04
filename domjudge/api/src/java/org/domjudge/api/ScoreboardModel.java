package org.domjudge.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

import static java.util.stream.Collectors.toMap;
import static org.domjudge.proto.DomjudgeProto.*;

public interface ScoreboardModel {

  Contest getContest();
  Collection<Problem> getProblems();
  Collection<Team> getTeams();
  Collection<ScoreboardRow> getRows();

  default Team getTeam(long id) throws NoSuchElementException {
    return getTeams().stream().filter(x -> x.getId() == id).findFirst().get();
  }

  default ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return getRows().stream().filter(x -> x.getTeam() == team.getId()).findFirst().get();
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

  public final class Impl implements ScoreboardModel {
    final Contest contest;
    final List<Problem> problems;
    final Map<Long, Team> teams;
    final List<ScoreboardRow> rows;

    public Impl(Contest contest, Problem[] problems, Team[] teams, ScoreboardRow[] rows) {
      this.contest = contest;
      this.problems = Arrays.asList(problems);
      this.teams = Arrays.stream(teams).collect(toMap(Team::getId, Function.identity()));
      this.rows = Arrays.asList(rows);
    }

    @Override
    public Contest getContest() {
      return contest;
    }

    @Override
    public Collection<Problem> getProblems() {
      return problems;
    }

    @Override
    public Collection<Team> getTeams() {
      return teams.values();
    }

    @Override
    public Collection<ScoreboardRow> getRows() {
      return rows;
    }

    @Override
    public Team getTeam(long id) {
      return teams.get(id);
    }
  }
}
