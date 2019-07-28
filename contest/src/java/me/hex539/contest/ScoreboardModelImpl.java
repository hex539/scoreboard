package me.hex539.contest;

import com.google.auto.value.AutoValue;
import com.google.protobuf.util.Durations;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import me.hex539.contest.model.Teams;
import me.hex539.contest.mutable.JudgeMutable;
import me.hex539.contest.mutable.ProblemsMutable;
import me.hex539.contest.mutable.RanklistMutable;
import me.hex539.contest.mutable.TeamsMutable;

import edu.clics.proto.ClicsProto.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@AutoValue
public abstract class ScoreboardModelImpl implements ScoreboardModel, ScoreboardModel.Observer {
  abstract ClicsContest getClics();

  public abstract TeamsMutable getTeamsModel();
  public abstract ProblemsMutable getProblemsModel();
  public abstract RanklistMutable getRanklistModel();
  public abstract JudgeMutable getJudgeModel();

  public abstract Builder toBuilder();

  public static Builder newBuilder(ClicsContest clics) {
    return new AutoValue_ScoreboardModelImpl.Builder().setClics(clics);
  }

  public static Builder newBuilder(ClicsContest clics, ScoreboardModel src) {
    Builder b = newBuilder(clics);
    b.reference = src;
    return b;
  }

  private static <K, V> Map<K, V> mapBy(Collection<V> v, Function<V, K> m) {
    return v.stream().collect(Collectors.toMap(m, Function.identity()));
  }

  @AutoValue.Builder
  public abstract static class Builder {
    // Internal builder methods.
    abstract Builder setClics(ClicsContest clics);
    abstract Builder setTeamsModel(TeamsMutable teams);
    abstract Builder setProblemsModel(ProblemsMutable problems);
    abstract Builder setRanklistModel(RanklistMutable ranklist);
    abstract Builder setJudgeModel(JudgeMutable judge);

    abstract ClicsContest getClics();
    abstract Optional<TeamsMutable> getTeamsModel();
    abstract Optional<ProblemsMutable> getProblemsModel();
    abstract Optional<RanklistMutable> getRanklistModel();
    abstract Optional<JudgeMutable> getJudgeModel();

    abstract ScoreboardModelImpl autoBuild();

    // ScoreboardModel copy-builder.
    private ScoreboardModel reference = null;

    // Extra setup.
    private Predicate<ScoreboardRow> scoreboardFilter =
        x -> getTeamsModel().get().containsTeam(x.getTeamId());
    private Predicate<Judgement> judgementFilter =
        x -> true;
    private Predicate<Submission> submissionFilter =
        x -> getTeamsModel().get().containsTeam(x.getTeamId());
    private Predicate<Team> teamFilter =
        x -> x.getGroupIdsList().stream()
            .filter(getTeamsModel().get()::containsGroup)
            .findAny()
            .isPresent();
    private Predicate<Group> groupFilter = x -> true;

    public Builder withEmptyScoreboard() {
      scoreboardFilter = x -> false;
      judgementFilter = x -> false;
      return this;
    }

    public Builder withJudgements(Predicate<Judgement> judgementFilter) {
      JudgeMutable judge = JudgeMutable.newBuilder()
        .setProblems(getProblemsModel().get())
        .setTeams(getTeamsModel().get())
        .build();
      setJudgeModel(judge);
      getClics().getJudgementTypesMap().values().stream()
          .forEach(judge::onJudgementTypeAdded);
      getClics().getSubmissionsMap().values().stream()
          .filter(submissionFilter)
          .sorted((a, b) -> Long.compare(
              Durations.toNanos(a.getContestTime()),
              Durations.toNanos(b.getContestTime())))
          .forEach(s -> judge.onProblemSubmitted(
              getTeamsModel().get().getTeam(s.getTeamId()),
              s));
      getClics().getJudgementsMap().values().stream()
          .filter(judgementFilter)
          .sorted((a, b) -> Long.compare(
              Durations.toNanos(a.getEndContestTime()),
              Durations.toNanos(b.getEndContestTime())))
          .forEach(j -> judge.onSubmissionJudged(j));
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
      if (!getTeamsModel().isPresent()) {
        TeamsMutable teams = new TeamsMutable();
        setTeamsModel(teams);

        if (reference != null) {
          Teams refTeams = reference.getTeamsModel();
          refTeams.getOrganizations().forEach(teams::onOrganizationAdded);
          refTeams.getGroups().stream().filter(groupFilter).forEach(teams::onGroupAdded);
          refTeams.getTeams().stream().filter(teamFilter).forEach(teams::onTeamAdded);
        } else {
          getClics().getOrganizationsMap().values()
              .forEach(teams::onOrganizationAdded);
          getClics().getGroupsMap().values().stream()
              .filter(groupFilter)
              .forEach(teams::onGroupAdded);
          getClics().getTeamsMap().values().stream()
              .filter(teamFilter)
              .forEach(teams::onTeamAdded);
        }
      }

      if (!getProblemsModel().isPresent()) {
        if (reference != null) {
          setProblemsModel(new ProblemsMutable(reference.getProblemsModel()));
        } else {
          ProblemsMutable problems = new ProblemsMutable();
          setProblemsModel(problems);
          getClics().getProblemsMap().values().stream()
              .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
              .forEach(problems::onProblemAdded);
        }
      }

      if (!getRanklistModel().isPresent()) {
        if (reference != null) {
          setRanklistModel(RanklistMutable.newBuilder()
              .setTeams(getTeamsModel().get())
              .setProblems(getProblemsModel().get())
              .copyFrom(
                  reference.getRanklistModel(),
                  scoreboardFilter));
        } else {
          setRanklistModel(RanklistMutable.newBuilder()
              .setTeams(getTeamsModel().get())
              .setProblems(getProblemsModel().get())
              .setRows(
                  getClics().getScoreboardList(),
                  scoreboardFilter)
              .build());
        }
      }

      if (!getJudgeModel().isPresent()) {
        if (reference != null) {
          setJudgeModel(JudgeMutable.newBuilder()
              .setTeams(getTeamsModel().get())
              .setProblems(getProblemsModel().get())
              .copyFrom(
                  reference.getJudgeModel(),
                  submissionFilter,
                  judgementFilter));
        } else {
          withJudgements(judgementFilter);
        }
      }
      return autoBuild();
    }
  }

  @Override
  public Contest getContest() {
    return getClics().getContest();
  }

  @Override
  public List<Problem> getProblems() {
    return getProblemsModel().getProblems();
  }

  @Override
  public List<ScoreboardRow> getRows() {
    return getRanklistModel().getRows();
  }

  @Override
  public Problem getProblem(String id) throws NoSuchElementException {
    return getProblemsModel().getProblem(id);
  }

  @Override
  public ScoreboardRow getRow(long index) throws NoSuchElementException {
    return getRanklistModel().getRow(index);
  }

  @Override
  public ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return getRanklistModel().getRow(team);
  }

  @Override
  public long getRank(Team team) throws NoSuchElementException {
    return getRanklistModel().getRank(team);
  }

  @Override
  public ScoreboardScore getScore(Team team) throws NoSuchElementException {
    return getRanklistModel().getScore(team);
  }

  @Override
  public ScoreboardProblem getAttempts(Team team, Problem problem) throws NoSuchElementException {
    return getRanklistModel().getAttempts(team, problem);
  }

  public Submission getSubmission(String id) throws NoSuchElementException {
    return getJudgeModel().getSubmission(id);
  }

  @Override
  public void onProblemSubmitted(Team team, Submission submission) {
    getJudgeModel().onProblemSubmitted(team, submission);
  }

  @Override
  public void onSubmissionJudged(Team team, Judgement judgement) {
    getJudgeModel().onSubmissionJudged(team, judgement);
  }

  @Override
  public void onProblemScoreChanged(Team team, ScoreboardProblem attempt) {
    getRanklistModel().onProblemScoreChanged(team, attempt);
  }

  @Override
  public void onScoreChanged(Team team, ScoreboardScore score) {
    getRanklistModel().onScoreChanged(team, score);
  }

  @Override
  public void onTeamRankChanged(Team team, int oldRank, int newRank) {
    // Already handled by onScoreChanged.
  }
}
