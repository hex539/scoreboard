package me.hex539.contest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import com.google.auto.value.AutoValue;

import edu.clics.proto.ClicsProto.*;
import me.hex539.contest.immutable.SortedLists;
import me.hex539.contest.model.Judge;
import me.hex539.contest.model.Teams;

/**
 * Thread-safe implementation of {@link ScoreboardModel}.
 *
 * <p>Read-only write-once implementation of a scoreboard which supports
 * fast paths for extracting data -- all methods are either O(1) or
 * O(logN) in the number of teams/groups.
 *
 * <p>This is the only scoreboard model that is safe to use from
 * multiple threads. Others do not support simultaneous reads and writes
 * and will fail nastily if this is attempted (but they can still be
 * used across threads with appropriate locking.)
 */
@AutoValue
public abstract class ImmutableScoreboardModel
    implements ScoreboardModel, Judge, Teams {

  // Internal fields.
  abstract List<Problem> getProblemsById();
  abstract List<ScoreboardRow> getRowsByTeamId();

  public static ImmutableScoreboardModel of(ScoreboardModel model) {
    final List<ScoreboardRow> rows = list(model.getRows());

    final Teams teamsModel = model.getTeamsModel();
    final List<Organization> organizations = SortedLists.sortBy(teamsModel.getOrganizations(), Organization::getId);
    final List<Team> teams = SortedLists.sortBy(teamsModel.getTeams(), Team::getId);
    final List<Group> groups = SortedLists.sortBy(teamsModel.getGroups(), Group::getId);

    return newBuilder()
        .setRows(rows)
        .setContest(model.getContest())
        .setOrganizations(organizations)
        .setTeams(teams)
        .setGroups(groups)
        .setJudgementTypes(list(model.getJudgeModel().getJudgementTypes()))
        .setJudgements(list(model.getJudgeModel().getJudgements()))
        .setSubmissions(list(model.getJudgeModel().getSubmissions()))
        .setProblems(model.getProblems())
        .setProblemsById(SortedLists.sortBy(model.getProblems(), Problem::getId))
        .setRowsByTeamId(SortedLists.sortBy(model.getRows(), ScoreboardRow::getTeamId))
        .build();
  }

  static Builder newBuilder() {
    return new AutoValue_ImmutableScoreboardModel.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    // ScoreboardModel
    public abstract Builder setRows(List<ScoreboardRow> scoreboardRows);
    public abstract Builder setContest(Contest contest);
    abstract Builder setRowsByTeamId(List<ScoreboardRow> rows);

    // Teams
    public abstract Builder setOrganizations(List<Organization> organizations);
    public abstract Builder setTeams(List<Team> teams);
    public abstract Builder setGroups(List<Group> groups);

    // Judge
    public abstract Builder setJudgementTypes(List<JudgementType> types);
    public abstract Builder setJudgements(List<Judgement> judgements);
    public abstract Builder setSubmissions(List<Submission> submissions);

    // Problems
    public abstract Builder setProblems(List<Problem> problems);
    abstract Builder setProblemsById(List<Problem> problems);

    // Builder
    abstract ImmutableScoreboardModel build();
  }

  @Override
  public Teams getTeamsModel() {
    return this;
  }

  @Override
  public Judge getJudgeModel() {
    return this;
  }

  @Override
  public Optional<JudgementType> getJudgementTypeOpt(String id) {
    return SortedLists.binarySearch(getJudgementTypes(), type -> id.compareTo(type.getId()));
  }

  // Widening return types of inherited functions.
  @Override public abstract List<JudgementType> getJudgementTypes();
  @Override public abstract List<Organization> getOrganizations();
  @Override public abstract List<Group> getGroups();
  @Override public abstract List<Team> getTeams();
  @Override public abstract List<Judgement> getJudgements();
  @Override public abstract List<Submission> getSubmissions();

  @Override
  public Optional<Organization> getOrganizationOpt(String id) {
    return SortedLists.binarySearch(getOrganizations(), org -> id.compareTo(org.getId()));
  }

  @Override
  public Optional<Group> getGroupOpt(String id) {
    return SortedLists.binarySearch(getGroups(), group -> id.compareTo(group.getId()));
  }

  @Override
  public Optional<Team> getTeamOpt(String id) {
    return SortedLists.binarySearch(getTeams(), team -> id.compareTo(team.getId()));
  }

  @Override
  public Problem getProblem(String id) throws NoSuchElementException {
    return SortedLists.binarySearch(getProblemsById(), prob -> id.compareTo(prob.getId())).get();
  }

  @Override
  public ScoreboardRow getRow(long index) throws NoSuchElementException {
    return getRows().get((int) index);
  }

  @Override
  public ScoreboardRow getRow(Team team) throws NoSuchElementException {
    final String id = team.getId();
    return SortedLists.binarySearch(getRowsByTeamId(), row -> id.compareTo(row.getTeamId())).get();
  }

  @Override
  public ScoreboardProblem getAttempts(Team team, Problem problem) throws NoSuchElementException {
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

  @Override
  public Optional<Submission> getSubmissionOpt(String id) {
    return SortedLists.binarySearch(getSubmissions(), sub -> id.compareTo(sub.getId()));
  }

  private static <T> List<T> list(Collection<T> list) {
    return Collections.unmodifiableList(new ArrayList<>(list));
  }
}
