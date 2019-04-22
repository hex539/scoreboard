package me.hex539.contest.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import edu.clics.proto.ClicsProto.*;

public interface Judge {
  public interface Observer {
    default void onProblemSubmitted(Team team, Submission submission) {}
    default void onSubmissionJudged(Team team, Judgement judgement) {}
  }

  default Collection<JudgementType> getJudgementTypes() {
    return Collections.emptySet();
  }

  default Optional<JudgementType> getJudgementTypeOpt(String id) {
    return getJudgementTypes().stream().filter(x -> id.equals(x.getId())).findFirst();
  }

  default JudgementType getJudgementType(String id) throws NoSuchElementException {
    return getJudgementTypeOpt(id).get();
  }

  default boolean containsJudgementType(String id) {
    return getJudgementTypeOpt(id).isPresent();
  }

  default boolean containsJudgementType(JudgementType type) {
    return containsJudgementType(type.getId());
  }

  default List<Judgement> getJudgements() {
    return Collections.emptyList();
  }

  default Optional<Judgement> getJudgementOpt(String id) {
    return getJudgements().stream().filter(x -> id.equals(x.getId())).findFirst();
  }

  default boolean containsJudgement(String id) {
    return getJudgementOpt(id).isPresent();
  }

  default boolean containsJudgement(Judgement judgement) {
    return containsJudgement(judgement.getId());
  }

  default List<Submission> getSubmissions() {
    return Collections.emptyList();
  }

  default Optional<Submission> getSubmissionOpt(String id) {
    return getSubmissions().stream().filter(x -> id.equals(x.getId())).findFirst();
  }

  default Submission getSubmission(String id) throws NoSuchElementException {
    return getSubmissionOpt(id).get();
  }

  default boolean containsSubmission(String id) {
    return getSubmissionOpt(id).isPresent();
  }

  default boolean containsSubmission(Submission submission) {
    return containsSubmission(submission.getId());
  }
}
