package me.hex539.contest.mutable;

import com.google.auto.value.AutoValue;
import com.google.protobuf.util.Durations;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import me.hex539.contest.model.Judge;
import me.hex539.contest.model.Problems;
import me.hex539.contest.model.Teams;

import edu.clics.proto.ClicsProto.*;

@AutoValue
public abstract class JudgeMutable implements Judge, Judge.Observer {

  public abstract Problems getProblems();
  public abstract Teams getTeams();

  private final Map<String, JudgementType> judgementTypes = new HashMap<>();
  private final Map<String, Submission> submissions = new HashMap<>();
  private final Map<String, Judgement> judgements = new HashMap<>();

  public static Builder newBuilder() {
    return new AutoValue_JudgeMutable.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProblems(Problems problems);
    public abstract Builder setTeams(Teams teams);

    public abstract JudgeMutable build();

    public JudgeMutable copyFrom(
        Judge src,
        Predicate<Submission> submissionFilter,
        Predicate<Judgement> judgementFilter) {
      JudgeMutable res = build();
      src.getJudgementTypes().forEach(res::onJudgementTypeAdded);
      src.getSubmissions().stream().filter(submissionFilter).forEach(res::onProblemSubmitted);
      src.getJudgements().stream().filter(judgementFilter).forEach(res::onSubmissionJudged);
      return res;
    }
  }

  @Override
  public Collection<JudgementType> getJudgementTypes() {
    return judgementTypes.values();
  }

  public void onJudgementTypeAdded(JudgementType type) {
    judgementTypes.put(type.getId(), type);
  }

  @Override
  public Optional<JudgementType> getJudgementTypeOpt(String id) {
    return Optional.ofNullable(judgementTypes.get(id));
  }

  @Override
  public List<Judgement> getJudgements() {
    return judgements.values().stream()
        .sorted((a, b) -> Long.compare(
            Durations.toNanos(a.getEndContestTime()),
            Durations.toNanos(b.getEndContestTime())))
        .collect(Collectors.toList());
  }

  @Override
  public List<Submission> getSubmissions() {
    return submissions.values().stream()
        .sorted((a, b) -> Long.compare(
            Durations.toNanos(a.getContestTime()),
            Durations.toNanos(b.getContestTime())))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Submission> getSubmissionOpt(String id) {
    return Optional.ofNullable(submissions.get(id));
  }

  @Override
  public boolean containsSubmission(String id) {
    return submissions.containsKey(id);
  }

  @Override
  public void onProblemSubmitted(Team team, Submission submission) {
    if (!getTeams().containsTeam(team)) {
      return;
    }
    onProblemSubmitted(submission);
  }

  public void onProblemSubmitted(Submission submission) {
    if (submission != null && !getProblems().containsProblem(submission.getProblemId())) {
      return;
    }
    submissions.put(submission.getId(), submission);
  }

  @Override
  public void onSubmissionJudged(Team team, Judgement judgement) {
    if (!getTeams().containsTeam(team)) {
      return;
    }
    onSubmissionJudged(judgement);
  }

  public void onSubmissionJudged(Judgement judgement) {
    if (!submissions.containsKey(judgement.getSubmissionId())) {
      return;
    }
    judgements.put(judgement.getId(), judgement);
  }
}
