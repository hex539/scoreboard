package me.hex539.contest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.clics.proto.ClicsProto.*;

public class JudgementDispatcher {
  private static final boolean DEBUG = false;

  @FunctionalInterface
  private interface Logger {
    void log(String s);
  }

  public final Set<ScoreboardModel.Observer> observers = new HashSet<>();
  private final ScoreboardModel model;
  private final Comparators.RowComparator rowComparator;
  private final Logger logger = (DEBUG ? System.err::println : s -> {});

  private final Map<String, Integer> submissionOrder = new HashMap<>();
  private final Map<String, Map<Integer, List<Submission>>> teamSubmissions = new HashMap<>();
  private final Map<String, Map<Integer, List<Judgement>>> teamJudgements = new HashMap<>();

  public JudgementDispatcher(final ScoreboardModel model) {
    this(model, new Comparators.RowComparator(model));
  }

  public JudgementDispatcher(
      final ScoreboardModel model,
      final Comparators.RowComparator rowComparator) {
    this.model = model;
    this.rowComparator = rowComparator;

    for (Submission s : model.getSubmissions()) {
      submissionOrder.put(s.getId(), submissionOrder.size());
      Team team = model.getTeam(s.getTeamId());
      Problem problem = model.getProblem(s.getProblemId());
      getSubmissions(team, problem).add(s);
    }
    for (Judgement j : model.getJudgements()) {
      Submission s = model.getSubmission(j.getSubmissionId());
      Team team = model.getTeam(s.getTeamId());
      Problem problem = model.getProblem(s.getProblemId());
      getJudgements(team, problem).add(j);
    }
  }

  public boolean notifySubmission(final Submission submission) {
    if (submissionOrder.putIfAbsent(submission.getId(), submissionOrder.size()) != null) {
      logger.log("Submission " + submission.getId() + " already exists");
      return false;
    }

    final Team team;
    try {
      team = model.getTeam(submission.getTeamId());
      logger.log("Adding submission for team " + team.getName());
    } catch (NoSuchElementException e) {
      logger.log("No such team: " + submission.getTeamId());
      return false;
    }
    final Problem problem = model.getProblem(submission.getProblemId());
    getSubmissions(team, problem).add(submission);

    final ScoreboardProblem attempts = scoreProblem(
        problem,
        getJudgements(team, problem),
        getSubmissions(team, problem));
    final ScoreboardScore score = model.getScore(team);
    observers.forEach(x -> {
      x.onProblemSubmitted(team, submission);
      x.onScoreChanged(team, attempts, score);
    });
    return true;
  }

  public boolean notifyJudgement(final Judgement j) {
    final Submission submission;
    try {
      submission = model.getSubmission(j.getSubmissionId());
    } catch (NoSuchElementException e) {
      logger.log("No such submission: " + j.getSubmissionId());
      return false;
    }

    if (j.getJudgementTypeId() == null || "".equals(j.getJudgementTypeId())) {
      logger.log("Ignoring judgement " + j.getId() + " with no judgement type");
      return false;
    }

    final JudgementType type;
    try {
      type = model.getJudgementType(j.getJudgementTypeId());
    } catch (NoSuchElementException e) {
      logger.log("No such judgement type: '" + j.getJudgementTypeId() + "'");
      throw new Error("Unknown judgement type: '" + j.getJudgementTypeId() + "'");
    }

    final Team team;
    try {
      team = model.getTeam(submission.getTeamId());
      logger.log("Updating judging for team " + team.getName());
    } catch (NoSuchElementException e) {
      logger.log("No such team: " + submission.getTeamId());
      return false;
    }

    final Problem problem = model.getProblem(submission.getProblemId());

    int numPending = model.getAttempts(team, problem).getNumPending() - 1;
    int numJudged = model.getAttempts(team, problem).getNumJudged();

    final List<Submission> submissions = getSubmissions(team, problem);
    final List<Judgement> verdicts = getJudgements(team, problem);

    long[] oldPenaltyCount = {0};
    final ScoreboardProblem oldAttempts = scoreProblem(problem, verdicts, submissions, oldPenaltyCount);

    // Insert the new submission.
    // TODO: detect whether the rejudge changed the result.
    final boolean rejudge =
        verdicts.removeIf(x -> x.getSubmissionId().equals(j.getSubmissionId()));
    verdicts.add(j);
    Collections.sort(verdicts, (a, b) -> Integer.compare(
        submissionOrder.get(a.getSubmissionId()),
        submissionOrder.get(b.getSubmissionId())));

    long[] newPenaltyCount = {0};
    final ScoreboardProblem attempts = scoreProblem(problem, verdicts, submissions, newPenaltyCount);

    final ScoreboardScore oldScore = model.getScore(team);
    final ScoreboardScore newScore = attempts.equals(model.getAttempts(team, problem))
        ? oldScore
        : ScoreboardScore.newBuilder()
            .setNumSolved(oldScore.getNumSolved()
                + (attempts.getSolved() ? 1 : 0)
                - (oldAttempts.getSolved() ? 1 : 0))
            .setTotalTime(oldScore.getTotalTime()
                + (attempts.getSolved() ? attempts.getTime() : 0)
                - (oldAttempts.getSolved() ? oldAttempts.getTime() : 0)
                + (newPenaltyCount[0] - oldPenaltyCount[0]) * model.getContest().getPenaltyTime())
            .build();

    final int oldRank = (oldScore != newScore) ? (int) computeRank(team) : 0;
    observers.forEach(x -> {
      x.onSubmissionJudged(team, j);
      if (newScore != oldScore) {
        x.onScoreChanged(team, attempts, newScore);
      }
    });
    if (oldScore != newScore) {
      notifyRankChanged(team, oldRank, (int) computeRank(team));
    }
    return true;
  }

  private void notifyRankChanged(Team team, int oldRank, int newRank) {
    if (newRank != oldRank) {
      logger.log("Team rank " + oldRank + " -> " + newRank + " for " + team.getName());
      observers.forEach(x -> x.onTeamRankChanged(team, oldRank, newRank));
    }
  }

  private ScoreboardProblem scoreProblem(
      Problem problem,
      List<Judgement> verdicts,
      List<Submission> submissions) {
    return scoreProblem(problem, verdicts, submissions, new long[1]);
  }

  private ScoreboardProblem scoreProblem(
      Problem problem,
      List<Judgement> verdicts,
      List<Submission> submissions,
      long[] penalty) {
    if (submissions.size() < verdicts.size()) {
      throw new Error("More verdicts than submissions for team");
    }
    long penaltyCount = 0;
    int attemptsToSolve = 0;
    Judgement solvedAt = null;

    for (Judgement j : verdicts) {
      JudgementType t = model.getJudgementType(j.getJudgementTypeId());
      attemptsToSolve++;
      if (t.getPenalty()) {
        penaltyCount++;
      }
      if (t.getSolved()) {
        solvedAt = j;
        break;
      }
    }

    if (solvedAt != null) {
      penalty[0] = penaltyCount;
      return ScoreboardProblem.newBuilder()
          .setProblemId(problem.getId())
          .setNumJudged(attemptsToSolve)
          .setNumPending(submissions.size() - verdicts.size())
          .setSolved(true)
          .setTime(
              model.getSubmission(solvedAt.getSubmissionId()).getContestTime().getSeconds() / 60)
          .build();
    } else {
      penalty[0] = 0;
      return ScoreboardProblem.newBuilder()
          .setProblemId(problem.getId())
          .setNumJudged(attemptsToSolve)
          .setNumPending(submissions.size() - verdicts.size())
          .setSolved(false)
          .build();
    }
  }

  private long computeRank(final Team team) {
    final ScoreboardRow myRow = model.getRow(team);
    if (model instanceof ScoreboardModelImpl) {
      return myRow.getRank();
    } else {
      return model.getRows().stream().filter(r -> rowComparator.compare(myRow, r) > 0).count() + 1;
    }
  }

  private List<Submission> getSubmissions(Team team, Problem problem) {
    return teamSubmissions
        .computeIfAbsent(team.getId(), k -> new HashMap<>())
        .computeIfAbsent(problem.getOrdinal(), k -> new ArrayList<>());
  }

  private List<Judgement> getJudgements(Team team, Problem problem) {
    return teamJudgements
        .computeIfAbsent(team.getId(), k -> new HashMap<>())
        .computeIfAbsent(problem.getOrdinal(), k -> new ArrayList<>());
  }
}