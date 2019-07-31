package me.hex539.contest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.google.auto.value.AutoValue;

import edu.clics.proto.ClicsProto.*;
import me.hex539.contest.immutable.ImmutableTeams;
import me.hex539.contest.immutable.SortedLists;
import me.hex539.contest.model.Judge;
import me.hex539.contest.model.Problems;
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
public abstract class ImmutableScoreboardModel implements ScoreboardModel, Judge {

  // Internal fields.
  abstract List<ScoreboardRow> getRowsByTeamId();

  public static ImmutableScoreboardModel of(ScoreboardModel model) {
    final List<ScoreboardRow> rows = list(model.getRows());

    return newBuilder()
        .setRows(rows)
        .setContest(model.getContest())
        .setTeamsModel(ImmutableTeams.of(model.getTeamsModel()))
        .setProblemsModel(model.getProblemsModel())
        .setJudgementTypes(list(model.getJudgeModel().getJudgementTypes()))
        .setJudgements(list(model.getJudgeModel().getJudgements()))
        .setSubmissions(list(model.getJudgeModel().getSubmissions()))
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

    public abstract Builder setTeamsModel(Teams teams);
    public abstract Builder setProblemsModel(Problems problems);

    // Judge
    public abstract Builder setJudgementTypes(List<JudgementType> types);
    public abstract Builder setJudgements(List<Judgement> judgements);
    public abstract Builder setSubmissions(List<Submission> submissions);

    // Builder
    abstract ImmutableScoreboardModel build();
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
  @Override public abstract List<Judgement> getJudgements();
  @Override public abstract List<Submission> getSubmissions();

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
