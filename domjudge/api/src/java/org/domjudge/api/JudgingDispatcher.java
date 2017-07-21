package org.domjudge.api;

import java.util.HashSet;
import java.util.Set;

import org.domjudge.api.JudgingDispatcher;
import org.domjudge.proto.DomjudgeProto;

import static org.domjudge.proto.DomjudgeProto.*;

public class JudgingDispatcher {
  public final Set<ScoreboardModel.Observer> observers = new HashSet<>();
  private final ScoreboardModel model;

  public JudgingDispatcher(final ScoreboardModel model) {
    this.model = model;
  }

  public void notifySubmission(final Submission submission) {
    final Team team = model.getTeam(submission.getTeam());
    final Problem problem = model.getProblem(submission.getProblem());
    final ScoreboardProblem attempts =
        model.getAttempts(team, problem)
            .toBuilder()
            .setNumPending(model.getAttempts(team, problem).getNumPending() + 1)
            .build();
    observers.forEach(x -> {
      x.onProblemSubmitted(team, submission);
      x.onProblemAttempted(team, attempts);
    });
  }

  public void notifyJudging(final Judging j) {
    final Submission submission = model.getSubmission(j.getSubmissionId());
    final Team team = model.getTeam(submission.getTeam());
    final Problem problem = model.getProblem(submission.getProblem());
    final ScoreboardProblem.Builder attemptsBuilder =
        model.getAttempts(team, problem)
            .toBuilder()
            .setNumPending(model.getAttempts(team, problem).getNumPending() - 1)
            .setNumJudged(model.getAttempts(team, problem).getNumJudged() + 1);
    switch (j.getOutcome()) {
      case "correct": {
        attemptsBuilder.setSolved(true);
        attemptsBuilder.setTime((long) submission.getTime());
        break;
      }
      case "wrong-answer":
      case "no-output":
      case "time-limit-exceeded": {
        break;
      }
      default: {
        throw new Error("Unknown outcome: " + j.getOutcome());
      }
    }
    final ScoreboardProblem attempts = attemptsBuilder.build();
    observers.forEach(x -> x.onProblemAttempted(team, attempts));
  }

  public void notifyRankChanged(final Team team, int oldRank, int newRank) {
    if (newRank != oldRank) {
      observers.forEach(x -> x.onTeamRankChanged(team, oldRank, newRank));
    }
  }
}
