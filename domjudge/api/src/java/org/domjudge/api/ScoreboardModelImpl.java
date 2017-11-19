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
import com.google.auto.value.AutoValue;
import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.domjudge.proto.DomjudgeProto.*;

@AutoValue
public abstract class ScoreboardModelImpl implements ScoreboardModel, ScoreboardModel.Observer {
  @Override public abstract Contest getContest();
  @Override public abstract Collection<Problem> getProblems();
  abstract Map<Long, Team> getTeamsMap();

  private Collection<ScoreboardRow> mRows;
  private final Collection<Submission> mSubmissions = new ArrayList<>();

  public static ScoreboardModelImpl create(
      Contest contest,
      Problem[] problems,
      Team[] teams,
      ScoreboardRow[] rows) {
    return new AutoValue_ScoreboardModelImpl(
            contest,
            Arrays.asList(problems),
            Arrays.stream(teams).collect(toMap(t -> t.getId(), Function.identity())))
        .setRows(Arrays.asList(rows));
  }

  public static ScoreboardModelImpl create(DomjudgeRest api) throws Exception {
    DomjudgeProto.Contest contest = api.getContest();
    return create(
        contest,
        api.getProblems(contest),
        api.getTeams(),
        api.getScoreboard(contest));
  }

  public static ScoreboardModelImpl create(ScoreboardModel copy) {
    Map<Long, Team> teamsMap =
        copy.getTeams().stream().collect(toMap(t -> t.getId(), Function.identity()));
    ScoreboardModelImpl result = new AutoValue_ScoreboardModelImpl(
            copy.getContest(), copy.getProblems(), teamsMap)
        .setRows(copy.getRows());
    result.mSubmissions.addAll(copy.getSubmissions());
    return result;
  }

  ScoreboardModelImpl setRows(Collection<ScoreboardRow> rows) {
    mRows = rows;
    return this;
  }

  @Override
  public Collection<ScoreboardRow> getRows() {
    return mRows;
  }

  @Override
  public Collection<Submission> getSubmissions() {
    return mSubmissions;
  }

  @Override
  public Collection<Team> getTeams() {
    return getTeamsMap().values();
  }

  @Override
  public Team getTeam(long id) {
    return getTeamsMap().get(id);
  }

  @Override
  public void onProblemSubmitted(Team team, Submission submission) {
    mSubmissions.add(submission);
  }

  @Override
  public void onProblemAttempted(Team team, ScoreboardProblem attempt) {
    final ScoreboardRow row = getRow(team);
    final List<ScoreboardProblem> changed = row.getProblemsList()
        .stream()
        .map(x -> x.getLabel().equals(attempt.getLabel()) ? attempt : x)
        .collect(toList());
    setRows(getRows()
        .stream()
        .map(x -> x == row
            ? x.toBuilder()
                .clearProblems()
                .addAllProblems(changed)
                .build()
            : x)
        .collect(toList()));
  }

  @Override
  public void onTeamRankChanged(Team team, int oldRank, int newRank) {
    setRows(getRows().stream().map(
        x -> {
          if (x.getRank() < Math.min(oldRank, newRank)
              || x.getRank() > Math.max(oldRank, newRank)) {
            return x;
          }
          if (x.getRank() == oldRank) {
            return x.toBuilder().setRank(newRank).build();
          }
          return x.toBuilder().setRank(x.getRank() + (x.getRank() < oldRank ? 1: -1)).build();
        })
        .collect(toList()));
  }
}
