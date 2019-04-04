package me.hex539.contest;

import com.google.auto.value.AutoValue;
import com.google.protobuf.util.Durations;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import edu.clics.proto.ClicsProto.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@AutoValue
public abstract class ScoreboardModelImpl implements ScoreboardModel, ScoreboardModel.Observer {
  abstract ClicsContest getClics();
  abstract List<ScoreboardRow.Builder> getScoreboardRows();
  abstract Map<String, Submission> getSubmissionsMap();
  abstract Map<String, Judgement> getJudgementsMap();
  abstract Map<String, Problem> getProblemsMap();
  abstract Map<String, Team> getTeamsMap();
  abstract Map<String, Group> getGroupsMap();

  abstract SplayList<ScoreboardRow.Builder> getFancyScoreboardRows();
  abstract Map<String, ScoreboardRow.Builder> getTeamRowMap();

  public abstract Builder toBuilder();

  public static Builder newBuilder(ClicsContest clics) {
    return new AutoValue_ScoreboardModelImpl.Builder()
        .setClics(clics)
        .setProblemsMap(clics.getProblemsMap())
        .setGroupsMap(clics.getGroupsMap())
        .setTeamsMap(clics.getTeamsMap())
        .setSubmissionsMap(clics.getSubmissionsMap())
        .setJudgementsMap(clics.getJudgementsMap())
        .setScoreboardRows(clics.getScoreboardList()
            .stream().map(ScoreboardRow::toBuilder).collect(Collectors.toList()));
  }

  public static Builder newBuilder(ClicsContest clics, ScoreboardModel src) {
    return new AutoValue_ScoreboardModelImpl.Builder()
        .setClics(clics)
        .setProblemsMap(mapBy(src.getProblems(), Problem::getId))
        .setGroupsMap(mapBy(src.getGroups(), Group::getId))
        .setTeamsMap(mapBy(src.getTeams(), Team::getId))
        .setSubmissionsMap(mapBy(src.getSubmissions(), Submission::getId))
        .setJudgementsMap(mapBy(src.getJudgements(), Judgement::getId))
        .setScoreboardRows(src.getRows()
            .stream().map(ScoreboardRow::toBuilder).collect(Collectors.toList()));
  }

  private static <K, V> Map<K, V> mapBy(Collection<V> v, Function<V, K> m) {
    return v.stream().collect(Collectors.toMap(m, Function.identity()));
  }

  @AutoValue.Builder
  public abstract static class Builder {
    // Internal builder methods.
    abstract Builder setClics(ClicsContest clics);
    abstract Builder setScoreboardRows(List<ScoreboardRow.Builder> scoreboardRows);
    abstract Builder setJudgementsMap(Map<String, Judgement> judgementsMap);
    abstract Builder setSubmissionsMap(Map<String, Submission> submissionsMap);
    abstract Builder setTeamsMap(Map<String, Team> teamsMap);
    abstract Builder setGroupsMap(Map<String, Group> groupsMap);
    abstract Builder setProblemsMap(Map<String, Problem> problems);
    abstract Builder setFancyScoreboardRows(SplayList<ScoreboardRow.Builder> rows);
    abstract Builder setTeamRowMap(Map<String, ScoreboardRow.Builder> teamRows);

    abstract ClicsContest getClics();
    abstract List<ScoreboardRow.Builder> getScoreboardRows();
    abstract Map<String, Judgement> getJudgementsMap();
    abstract Map<String, Submission> getSubmissionsMap();
    abstract Map<String, Team> getTeamsMap();
    abstract Map<String, Group> getGroupsMap();
    abstract Map<String, Problem> getProblemsMap();
    abstract SplayList<ScoreboardRow.Builder> getFancyScoreboardRows();

    abstract ScoreboardModelImpl autoBuild();

    // Extra setup.
    private Predicate<ScoreboardRowOrBuilder> scoreboardFilter =
        x -> getTeamsMap().containsKey(x.getTeamId());
    private Predicate<Judgement> judgementFilter =
        x -> getSubmissionsMap().containsKey(x.getSubmissionId());
    private Predicate<Submission> submissionFilter =
        x -> getTeamsMap().containsKey(x.getTeamId());
    private Predicate<Team> teamFilter =
        x -> x.getGroupIdsList().stream().filter(getGroupsMap()::containsKey).findAny().isPresent();
    private Predicate<Group> groupFilter = x -> true;

    public Builder withEmptyScoreboard() {
      return setScoreboardRows(createEmptyScoreboard(makeTeamsModel(), makeProblemsModel()));
    }

    public Builder filterScoreboardRows(final Predicate<ScoreboardRowOrBuilder> pred) {
      scoreboardFilter = scoreboardFilter.and(pred);
      return this;
    }

    public Builder filterJudgements(final Predicate<Judgement> pred) {
      judgementFilter = judgementFilter.and(pred);
      return this;
    }

    public Builder filterSubmissions(final Predicate<Submission> pred) {
      submissionFilter = submissionFilter.and(pred);
      return this;
    }

    public Builder filterTooLateSubmissions() {
      return filterSubmissions(s ->
          Durations.toNanos(s.getContestTime()) <
          Durations.toNanos(getClics().getContest().getContestDuration()));
    }

    public Builder filterTeams(final Predicate<Team> pred) {
      teamFilter = teamFilter.and(pred);
      return this;
    }

    public Builder filterGroups(final Predicate<Group> pred) {
      groupFilter = groupFilter.and(pred);
      return this;
    }

    public ScoreboardModelImpl build() {
      return this
          .setGroupsMap(filterMap(getGroupsMap(), groupFilter))
          .setTeamsMap(filterMap(getTeamsMap(), teamFilter))
          .setSubmissionsMap(filterMap(getSubmissionsMap(), submissionFilter))
          .setJudgementsMap(filterMap(getJudgementsMap(), judgementFilter))
          .setFancyScoreboardRows(applyFilterToScoreboardRows(
                getScoreboardRows(),
                scoreboardFilter,
                new Comparators.RowComparator(makeTeamsModel())))
          .setScoreboardRows(getFancyScoreboardRows())
          .setTeamRowMap(new HashMap<>(
              getFancyScoreboardRows().stream()
                  .collect(Collectors.toMap(ScoreboardRowOrBuilder::getTeamId, Function.identity()))))
          .autoBuild();
    }

    private Teams makeTeamsModel() {
     return new Teams() {
        @Override public Collection<Organization> getOrganizations() {return null;}
        @Override public Collection<Group> getGroups() {return getGroupsMap().values();}
        @Override public Collection<Team> getTeams() {return getTeamsMap().values();}
        @Override public Group getGroup(String id) {return getGroupsMap().get(id);}
        @Override public Team getTeam(String id) {return getTeamsMap().get(id);}
      };
    }

    private Problems makeProblemsModel() {
      return new Problems() {
        @Override public List<Problem> getProblems() {
          return getProblemsMap().values().stream()
              .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
              .collect(Collectors.toList());
        }
        @Override public Problem getProblem(String id) {return getProblemsMap().get(id);}
      };
    }

    private static SplayList<ScoreboardRow.Builder> applyFilterToScoreboardRows(
        List<? extends ScoreboardRow.Builder> rows,
        Predicate<? super ScoreboardRowOrBuilder> pred,
        Comparator<? super ScoreboardRowOrBuilder> order) {
      final SplayList<ScoreboardRow.Builder> res = new SplayList<>(order);
      int firstRank = -1;
      int rank = 1;
      for (ScoreboardRow.Builder i : rows) if (pred.test(i)) {
        if (firstRank == -1) {
          firstRank = rank;
        } else if ((i.getRank() - firstRank) != (rank - 1) && i.getScore().getNumSolved() != 0) {
          System.err.println("Scoreboard is not sorted increasing by rank."
              + "\nTeam " + i.getTeamId() + " has index " + (rank - 1)
              + " but rank " + i.getRank());
          continue;
        }
        ScoreboardRow.Builder b = i.build().toBuilder().setRank(rank);
        res.add(b);
        if (res.indexOf(b) != rank - 1) {
          throw new AssertionError("Scoreboard is not sorted descending by score."
              + "\nTeam " + b.getTeamId() + " has index " + (res.indexOf(b) + 1)
              + " but rank " + b.getRank());
        }
        ++rank;
      }
      return res;
    }

    private static <K, V> Map<K, V> filterMap(Map<K, V> m, Predicate<V> f) {
      final Map<K, V> res = new HashMap<>();
      m.entrySet().stream()
          .filter(e -> f.test(e.getValue()))
          .forEach(e -> res.put(e.getKey(), e.getValue()));
      return res;
    }
  }

  private static List<ScoreboardRow.Builder> createEmptyScoreboard(Teams tm, Problems pm) {
    List<ScoreboardRow.Builder> emptyRows = new ArrayList<>();
    tm.getTeams().stream().sorted(new Comparators.TeamComparator(tm)).forEach(t -> {
      emptyRows.add(createEmptyScoreboardRow(emptyRows.size() + 1, t, pm));
    });
    return emptyRows;
  }

  private static ScoreboardRow.Builder createEmptyScoreboardRow(long rank, Team team, Problems pm) {
    return ScoreboardRow.newBuilder()
        .setRank(rank)
        .setTeamId(team.getId())
        .setScore(ScoreboardScore.newBuilder()
            .setNumSolved(0)
            .setTotalTime(0)
            .build())
        .addAllProblems(pm.getProblems().stream()
            .map(p -> ScoreboardProblem.newBuilder().setProblemId(p.getId()).build())
            .collect(toList()));
  }

  @Override
  public Contest getContest() {
    return getClics().getContest();
  }

  @Override
  public Collection<JudgementType> getJudgementTypes() {
    return getClics().getJudgementTypes().values();
  }

  @Override
  public JudgementType getJudgementType(String id) {
    try {
      return getClics().getJudgementTypesOrThrow(id);
    } catch (IllegalArgumentException e) {
      throw new NoSuchElementException("Judgement type '" + id + "'");
    }
  }

  @Override
  public Organization getOrganization(String id) {
    try {
      return getClics().getOrganizationsOrThrow(id);
    } catch (IllegalArgumentException e) {
      throw new NoSuchElementException("Organization '" + id + "'");
    }
  }

  @Override
  public Collection<Organization> getOrganizations() {
    return getClics().getOrganizationsMap().values();
  }

  @Override
  public List<Problem> getProblems() {
    return getClics().getProblems().values().stream()
        .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
        .collect(Collectors.toList());
  }

  @Override
  public List<ScoreboardRow> getRows() {
    List<ScoreboardRow> res = new ArrayList<>();
    for (ScoreboardRow.Builder row : getScoreboardRows()) {
      res.add(fixRank(row));
    }
    return res;
  }

  @Override
  public List<Submission> getSubmissions() {
    return getSubmissionsMap().values().stream()
        .sorted((a, b) -> Long.compare(
            Durations.toNanos(a.getContestTime()),
            Durations.toNanos(b.getContestTime())))
        .collect(Collectors.toList());
  }

  @Override
  public List<Judgement> getJudgements() {
    return getJudgementsMap().values().stream()
        .sorted((a, b) -> Long.compare(
            Durations.toNanos(a.getEndContestTime()),
            Durations.toNanos(b.getEndContestTime())))
        .collect(Collectors.toList());
  }

  @Override
  public Collection<Group> getGroups() {
    return getGroupsMap().values();
  }

  @Override
  public Collection<Team> getTeams() {
    return getTeamsMap().values();
  }

  @Override
  public Group getGroup(String id) throws NoSuchElementException {
    return getGroupsMap().get(id);
  }

  @Override
  public Team getTeam(String id) throws NoSuchElementException {
    return Optional.ofNullable(getTeamsMap().get(id)).get();
  }

  @Override
  public Problem getProblem(String id) throws NoSuchElementException {
    return Optional.ofNullable(getProblemsMap().get(id)).get();
  }

  @Override
  public ScoreboardRow getRow(long index) throws NoSuchElementException {
    return fixRank(getScoreboardRows().get((int) index));
  }

  @Override
  public ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return fixRank(getRowInternal(team));
  }

  private ScoreboardRow.Builder getRowInternal(Team team) throws NoSuchElementException {
    try {
      return Optional.ofNullable(getTeamRowMap().get(team.getId())).get();
    } catch (NoSuchElementException e) {
      throw new NoSuchElementException("Team \"" + team.getId() + "\" does not exist.");
    }
  }

  @Override
  public ScoreboardScore getScore(Team team) throws NoSuchElementException {
    return getRowInternal(team).getScore();
  }

  @Override
  public ScoreboardProblem getAttempts(Team team, Problem problem) throws NoSuchElementException {
    final List<ScoreboardProblem> attempts = getRowInternal(team).getProblemsList();
    for (ScoreboardProblem attempt : attempts) {
      if (problem.getId().equals(attempt.getProblemId())) {
        return attempt;
      }
    }
    throw new NoSuchElementException("Cannot find problem " + problem.getId());
  }

  @Override
  public Submission getSubmission(String id) throws NoSuchElementException {
    return Optional.ofNullable(getSubmissionsMap().get(id)).get();
  }

  @Override
  public void onProblemSubmitted(Team team, Submission submission) {
    getSubmissionsMap().put(submission.getId(), submission);
  }

  @Override
  public void onSubmissionJudged(Team team, Judgement judgement) {
    getJudgementsMap().put(judgement.getId(), judgement);
  }

  @Override
  public void onProblemScoreChanged(Team team, ScoreboardProblem attempt) {
    // We have to re-insert the original row, even though the score stayed the same.
    // This is because submit times are used as a tie-breaker.
    final ScoreboardRow.Builder row = getRowInternal(team);
    getFancyScoreboardRows().remove(row);

    final List<ScoreboardProblem> attempts = row.getProblemsList();
    for (int i = attempts.size(); i --> 0;) {
      final ScoreboardProblem p = attempts.get(i);
      if (attempt.getProblemId().equals(p.getProblemId())) {
        getFancyScoreboardRows().add(row.setProblems(i, attempt));
        return;
      }
    }
    throw new NoSuchElementException("Cannot find problem " + attempt.getProblemId());
  }

  @Override
  public void onScoreChanged(Team team, ScoreboardScore score) {
    final ScoreboardRow.Builder row = getRowInternal(team);
    getFancyScoreboardRows().remove(row);
    getFancyScoreboardRows().add(row.setScore(score));
  }

  @Override
  public void onTeamRankChanged(Team team, int oldRank, int newRank) {
    // Already handled by onScoreChanged.
  }

  /**
   * Find the real rank of a team before returning it.
   *
   * Internal representation doesn't care about rank, just relative order. This is because
   * keeping track of rank after scoreboard changes is an O(N) operation that can sometimes
   * involve rebuilding the whole scoreboard.
   *
   * To keep clients happy we find the real rank of the team and set it on a copy before
   * handing it out. The updated row isn't saved anywhere because we don't need it.
   */
  private ScoreboardRow fixRank(ScoreboardRow.Builder row) {
    if (getTeamRowMap().get(row.getTeamId()) != row) {
      throw new AssertionError("Invalid scoreboard row for team " + row.getTeamId());
    }
    int realRank = getFancyScoreboardRows().indexOf(row) + 1;
    if (realRank == 0) {
      throw new AssertionError("Scoreboard row for team " + row.getTeamId() + " is missing");
    }
    return (realRank == row.getRank() ? row : row.setRank(realRank)).build();
  }
}
