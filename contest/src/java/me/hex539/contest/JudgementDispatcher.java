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
import me.hex539.contest.model.Judge;
import me.hex539.contest.model.Problems;
import me.hex539.contest.model.Teams;

public class JudgementDispatcher {
  private static final boolean DEBUG = false;
  private static final boolean VERBOSE = false;

  @FunctionalInterface
  private interface Logger {
    void log(String s);
  }

  public final Set<ScoreboardModel.Observer> observers = new HashSet<>();
  private final boolean showCompileErrors;
  private final ScoreboardModel model;
  private final Judge judge;
  private final Problems problems;
  private final Teams teams;
  private final Comparators.RowComparator rowComparator;

  private final Logger warn = (DEBUG ? System.err::println : s -> {});
  private final Logger info = (VERBOSE ? System.err::println : s -> {});

  private final Map<String, Integer> submissionOrder = new HashMap<>();
  private final Map<String, Map<String, List<Submission>>> teamSubmissions = new HashMap<>();
  private final Map<String, Map<String, List<Judgement>>> teamJudgements = new HashMap<>();

  public JudgementDispatcher(final ScoreboardModel model) {
    this(model, true);
  }

  public JudgementDispatcher(
      final ScoreboardModel model,
      final boolean showCompileErrors) {
    this(model, showCompileErrors, new Comparators.RowComparator(model.getTeamsModel()));
  }

  public JudgementDispatcher(
      final ScoreboardModel model,
      final boolean showCompileErrors,
      final Comparators.RowComparator rowComparator) {
    this.model = model;
    this.judge = model.getJudgeModel();
    this.problems = model.getProblemsModel();
    this.teams = model.getTeamsModel();
    this.showCompileErrors = showCompileErrors;
    this.rowComparator = rowComparator;

    for (Submission s : judge.getSubmissions()) {
      submissionOrder.put(s.getId(), submissionOrder.size());
      Team team = teams.getTeam(s.getTeamId());
      Problem problem = problems.getProblem(s.getProblemId());
      getSubmissions(team, problem).add(s);
    }
    for (Judgement j : judge.getJudgements()) {
      Submission s = judge.getSubmission(j.getSubmissionId());
      Team team = teams.getTeam(s.getTeamId());
      Problem problem = problems.getProblem(s.getProblemId());
      getJudgements(team, problem).removeIf(x -> x.getSubmissionId().equals(j.getSubmissionId()));
      getJudgements(team, problem).add(j);
    }
  }

  public boolean notifySubmission(final Submission submission) {
    if (submissionOrder.putIfAbsent(submission.getId(), submissionOrder.size()) != null) {
      warn.log("Submission " + submission.getId() + " already exists");
      return false;
    }

    final Team team;
    final Problem problem;
    try {
      team = teams.getTeam(submission.getTeamId());
      problem = problems.getProblem(submission.getProblemId());
    } catch (NoSuchElementException e) {
      return false;
    }
    getSubmissions(team, problem).add(submission);

    final ScoreboardProblem attempts = scoreProblem(
        problem,
        getJudgements(team, problem),
        getSubmissions(team, problem));
    for (ScoreboardModel.Observer x : observers) {
      x.onProblemSubmitted(team, submission);
      x.onProblemScoreChanged(team, attempts);
    }
    return true;
  }

  public ScoreboardProblem notifyJudgement(final Judgement j) {
    if (j.getJudgementTypeId() == null || "".equals(j.getJudgementTypeId())) {
      warn.log("Ignoring judgement " + j.getId() + " with no judgement type");
      return null;
    }
    final JudgementType type = judge.getJudgementType(j.getJudgementTypeId());

    final Submission submission;
    final Team team;
    final Problem problem;
    try {
      submission = judge.getSubmission(j.getSubmissionId());
      team = teams.getTeam(submission.getTeamId());
      problem = problems.getProblem(submission.getProblemId());
    } catch (NoSuchElementException e) {
      return null;
    }

    final List<Submission> submissions = getSubmissions(team, problem);
    final List<Judgement> verdicts = getJudgements(team, problem);

    long[] oldPenaltyCount = {0};
    final ScoreboardProblem oldAttempts =
        scoreProblem(problem, verdicts, submissions, oldPenaltyCount);

    // Insert the new submission.
    boolean rejudge = false;
    for (int i = 0; i < verdicts.size(); i++) {
      if (verdicts.get(i).getSubmissionId().equals(j.getSubmissionId())) {
        verdicts.set(i, j);
        rejudge = true;
      }
    }
    if (!rejudge) {
      verdicts.add(j);
    }
    Collections.sort(verdicts, (a, b) -> Integer.compare(
        submissionOrder.get(a.getSubmissionId()),
        submissionOrder.get(b.getSubmissionId())));

    long[] newPenaltyCount = {0};
    final ScoreboardProblem attempts =
        scoreProblem(problem, verdicts, submissions, newPenaltyCount);

    final ScoreboardScore oldScore = model.getRanklistModel().getScore(team);
    final boolean attemptsChanged = !attempts.equals(oldAttempts);

    final ScoreboardScore newScore = !attemptsChanged
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

    final boolean scoreChanged = attemptsChanged && !newScore.equals(oldScore);

    final int oldRank;
    if (scoreChanged) {
      oldRank = (int) model.getRanklistModel().getRank(team);
    } else {
      oldRank = 0;
    }

    observers.forEach(x -> {
      x.onSubmissionJudged(team, j);
      if (attemptsChanged) {
        x.onProblemScoreChanged(team, attempts);
      }
    });

    if (scoreChanged) {
      observers.forEach(x -> x.onScoreChanged(team, newScore));

      final int newRank = (int) model.getRanklistModel().getRank(team);
      if (oldRank != newRank) {
        info.log("Team rank " + oldRank + " -> " + newRank + " for " + team.getName());
        observers.forEach(x -> x.onTeamRankChanged(team, oldRank, newRank));
      }
    }
    return attempts;
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
    int penaltyCount = 0;
    int attemptsToSolve = 0;
    Judgement solvedAt = null;

    for (Judgement j : verdicts) {
      JudgementType t = judge.getJudgementType(j.getJudgementTypeId());
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
          .setNumJudged(penaltyCount + 1)
          .setNumPending(0)
          .setSolved(true)
          .setTime(
              judge.getSubmission(solvedAt.getSubmissionId()).getContestTime().getSeconds() / 60)
          .build();
    } else {
      // Show submissions with only compiler error as -1, to be consistent with real scoreboards
      // and to show a more reasonable fiction when resolving where pending submissions don't
      // disappear into nothingness if they're all CE.
      penalty[0] = 0;
      return ScoreboardProblem.newBuilder()
          .setProblemId(problem.getId())
          .setNumJudged(attemptsToSolve == 0
              ? 0
              : Math.max(showCompileErrors ? 1 : 0, penaltyCount))
          .setNumPending(submissions.size() - verdicts.size())
          .setSolved(false)
          .build();
    }
  }

  private List<Submission> getSubmissions(Team team, Problem problem) {
    return teamSubmissions
        .computeIfAbsent(team.getId(), k -> new HashMap<>())
        .computeIfAbsent(problem.getId(), k -> new ArrayList<>());
  }

  private List<Judgement> getJudgements(Team team, Problem problem) {
    return teamJudgements
        .computeIfAbsent(team.getId(), k -> new HashMap<>())
        .computeIfAbsent(problem.getId(), k -> new ArrayList<>());
  }
}
