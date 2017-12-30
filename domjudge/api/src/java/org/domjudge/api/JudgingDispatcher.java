package org.domjudge.api;

import java.util.HashSet;
import java.util.Set;

import org.domjudge.api.JudgingDispatcher;

import static org.domjudge.proto.DomjudgeProto.*;

public class JudgingDispatcher {
  private static final boolean DEBUG = false;

  @FunctionalInterface
  private interface Logger {
    void log(String s);
  }

  public final Set<ScoreboardModel.Observer> observers = new HashSet<>();
  private final ScoreboardModel model;
  private final Logger logger = (DEBUG ? System.err::println : s -> {});

  public JudgingDispatcher(final ScoreboardModel model) {
    this.model = model;
  }

  public void notifySubmission(final Submission submission) {
    final Team team = model.getTeam(submission.getTeam());
    if (team == null) {
      logger.log("No such team: " + submission.getTeam());
      return;
    } else {
      logger.log("Adding submission for team " + team.getName());
    }
    final Problem problem = model.getProblem(submission.getProblem());
    final ScoreboardProblem attempts =
        model.getAttempts(team, problem)
            .toBuilder()
            .setNumPending(model.getAttempts(team, problem).getNumPending() + 1)
            .build();
    final ScoreboardScore score = model.getRow(team).getScore();
    observers.forEach(x -> {
      x.onProblemSubmitted(team, submission);
      x.onProblemAttempted(team, attempts, score);
    });
  }

  public void notifyJudging(final Judging j) {
    final Submission submission = model.getSubmission(j.getSubmission());
    if (submission == null) {
      logger.log("No such submission: " + j.getSubmission());
      return;
    }

    final Team team = model.getTeam(submission.getTeam());

    if (team == null) {
      logger.log("No such team: " + submission.getTeam());
      return;
    } else {
      logger.log("Updating judging for team " + team.getName());
    }

    final Problem problem = model.getProblem(submission.getProblem());

    ScoreboardScore oldScore = model.getRow(team).getScore();
    ScoreboardScore newScore = null;

    final ScoreboardProblem.Builder attemptsBuilder =
        model.getAttempts(team, problem)
            .toBuilder()
            .setNumPending(model.getAttempts(team, problem).getNumPending() - 1)
            .setNumJudged(model.getAttempts(team, problem).getNumJudged() + 1);
    switch (j.getOutcome()) {
      case "correct": {
        if (!model.getAttempts(team, problem).getSolved()) {
          attemptsBuilder.setSolved(true);
          attemptsBuilder.setTime((long) submission.getTime());

          // TODO: broken. needs to implement penalty time properly using the values from
          // the config instead of blindly assuming 20 for every failed attempt.
          final long PENALTY_TIME = 20;
          final long floorTime = Math.round(submission.getTime() - (0.5 - 1e-20));
          final long penaltyTime = PENALTY_TIME * model.getAttempts(team, problem).getNumJudged()
              + (floorTime - model.getContest().getStart()) / 60;

          newScore = ScoreboardScore.newBuilder()
              .setNumSolved(oldScore.getNumSolved() + 1)
              .setTotalTime(oldScore.getTotalTime() + penaltyTime)
              .build();
        }
        break;
      }
      case "compiler-error": {
        // TODO: record previous attempts in an auxiliary data structure or add a routine to filter
        // past submissions on a given problem for a particular team, so we can skip the line below
        // and still get the penalty time right.
        attemptsBuilder.setNumJudged(model.getAttempts(team, problem).getNumJudged());
        break;
      }
      case "wrong-answer":
      case "no-output":
      case "run-error":
      case "timelimit": {
        break;
      }
      default: {
        throw new Error("Unknown outcome: " + j.getOutcome());
      }
    }

    final ScoreboardScore score = (newScore != null ? newScore : oldScore);
    final ScoreboardProblem attempts = attemptsBuilder.build();

    final int oldRank = (int) computeRank(team);
    notifyProblemAttempted(team, attempts, score);
    final int newRank = (int) computeRank(team);

    notifyRankChanged(team, oldRank, newRank);
  }

  public void notifyProblemAttempted(Team team, ScoreboardProblem attempts, ScoreboardScore score) {
    observers.forEach(x -> x.onProblemAttempted(team, attempts, score));
  }

  public void notifyRankChanged(Team team, int oldRank, int newRank) {
    if (newRank != oldRank) {
      logger.log("Team rank " + oldRank + " -> " + newRank + " for " + team.getName());
      observers.forEach(x -> x.onTeamRankChanged(team, oldRank, newRank));
    }
  }

  private long computeRank(final Team team) {
    return model.getRows().stream()
        .filter(r ->
            Comparators.compareRows(team, model.getRow(team), model.getTeam(r.getTeam()), r) > 0)
        .count() + 1;
  }
}
