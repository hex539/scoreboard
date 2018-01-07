package org.domjudge.scoreboard;

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
import com.google.auto.value.AutoValue;
import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.domjudge.proto.DomjudgeProto.*;

@AutoValue
public abstract class ScoreboardModelImpl implements ScoreboardModel, ScoreboardModel.Observer {
  @Override public abstract Contest getContest();
  @Override public abstract List<Problem> getProblems();
  abstract Map<Long, Team> getTeamsMap();

  private List<ScoreboardRow> mRows;
  private final List<Submission> mSubmissions = new ArrayList<>();

  public static ScoreboardModelImpl create(
      Contest contest,
      Problem[] problems,
      Team[] teams) {
    teams = teams.clone();
    Arrays.sort(teams, Comparators::compareTeams);
    ScoreboardRow[] emptyRows = new ScoreboardRow[teams.length];
    for (int i = 0; i < teams.length; i++) {
      emptyRows[i] = ScoreboardRow.newBuilder()
          .setRank(i+1)
          .setTeam(teams[i].getId())
          .setScore(ScoreboardScore.newBuilder()
              .setNumSolved(0)
              .setTotalTime(0)
              .build())
          .addAllProblems(Arrays.stream(problems)
              .map(p -> ScoreboardProblem.newBuilder()
                  .setLabel(p.getLabel())
                  .setSolved(false)
                  .setTime(0)
                  .setNumJudged(0)
                  .setNumPending(0)
                  .build())
              .collect(toList()))
          .build();
    }
    return create(contest, problems, teams, emptyRows);
  }

  public static ScoreboardModelImpl create(
      Contest contest,
      Problem[] problems,
      Team[] teams,
      ScoreboardRow[] rows) {
    return new AutoValue_ScoreboardModelImpl(
            contest,
            Arrays.asList(problems),
            Arrays.stream(teams).collect(toMap(Team::getId, x -> x)))
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

  public static ScoreboardModelImpl create(EntireContest entireContest) {
    return create(
        entireContest.getContest(),
        entireContest.getProblemsList().toArray(new Problem[0]),
        entireContest.getTeamsList().toArray(new Team[0]));
  }

  public static ScoreboardModelImpl create(ScoreboardModel copy) {
    Map<Long, Team> teamsMap =
        copy.getTeams().stream().collect(toMap(Team::getId, x -> x));
    ScoreboardModelImpl result = new AutoValue_ScoreboardModelImpl(
            copy.getContest(), copy.getProblems(), teamsMap)
        .setRows(copy.getRows());
    result.mSubmissions.addAll(copy.getSubmissions());
    return result;
  }

  public ScoreboardModelImpl withoutSubmissions() {
    return ScoreboardModelImpl.create(
        getContest(),
        getProblems().toArray(new Problem[0]),
        getTeams().toArray(new Team[0]));
  }

  ScoreboardModelImpl setRows(List<ScoreboardRow> rows) {
    mRows = rows;
    return this;
  }

  @Override
  public List<ScoreboardRow> getRows() {
    return mRows;
  }

  @Override
  public List<Submission> getSubmissions() {
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
  public void onProblemAttempted(Team team, ScoreboardProblem attempt, ScoreboardScore score) {
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
                .setScore(score)
                .build()
            : x)
        .collect(toList()));
  }

  @Override
  public void onTeamRankChanged(Team team, int oldRank, int newRank) {
    setRows(getRows().stream().map(
        x -> (x.getRank() < Math.min(oldRank, newRank) || Math.max(oldRank, newRank) < x.getRank())
            ? x
            : x.toBuilder().setRank(x.getRank() == oldRank
                ? newRank
                : x.getRank() + (newRank < oldRank ? +1 : -1)).build())
        .sorted((a, b) -> (Long.compare(a.getRank(), b.getRank())))
        .collect(toList()));
  }
}
