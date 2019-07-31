package me.hex539.contest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.auto.value.AutoValue;

import edu.clics.proto.ClicsProto.*;
import me.hex539.contest.immutable.ImmutableProblems;
import me.hex539.contest.immutable.ImmutableRanklist;
import me.hex539.contest.immutable.ImmutableTeams;
import me.hex539.contest.immutable.SortedLists;
import me.hex539.contest.model.Judge;
import me.hex539.contest.model.Problems;
import me.hex539.contest.model.Ranklist;
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

  public static ImmutableScoreboardModel of(ScoreboardModel model) {
    return new AutoValue_ImmutableScoreboardModel.Builder()
        .setContest(model.getContest())
        .setTeamsModel(ImmutableTeams.of(model.getTeamsModel()))
        .setProblemsModel(ImmutableProblems.of(model.getProblemsModel()))
        .setRanklistModel(ImmutableRanklist.of(model.getRanklistModel()))
        .setJudgementTypes(list(model.getJudgeModel().getJudgementTypes()))
        .setJudgements(list(model.getJudgeModel().getJudgements()))
        .setSubmissions(list(model.getJudgeModel().getSubmissions()))
        .build();
  }

  @AutoValue.Builder
  abstract static class Builder {
    // ScoreboardModel
    public abstract Builder setContest(Contest contest);

    public abstract Builder setTeamsModel(ImmutableTeams teams);
    public abstract Builder setProblemsModel(ImmutableProblems problems);
    public abstract Builder setRanklistModel(ImmutableRanklist ranklist);

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
  @Override public abstract ImmutableProblems getProblemsModel();
  @Override public abstract ImmutableTeams getTeamsModel();
  @Override public abstract ImmutableRanklist getRanklistModel();

  @Override public abstract List<JudgementType> getJudgementTypes();
  @Override public abstract List<Judgement> getJudgements();
  @Override public abstract List<Submission> getSubmissions();

  @Override
  public Optional<Submission> getSubmissionOpt(String id) {
    return SortedLists.binarySearch(getSubmissions(), sub -> id.compareTo(sub.getId()));
  }

  private static <T> List<T> list(Collection<T> list) {
    return Collections.unmodifiableList(new ArrayList<>(list));
  }
}
