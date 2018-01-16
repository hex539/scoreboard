package me.hex539.contest;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.clics.proto.ClicsProto.*;

public class ResolverController {
  private static final boolean DEBUG = false;

  /**
   * Something that shows the scoreboard resolution.
   *
   * Workflow for an observer:
   *   - onProblemSubmitted(team1, A)
   *   - etc.
   *
   *   - onProblemFocusChanged(team, A)
   *     - onScoreChanged(team, A, fail)
   *     - onScoreChanged(team, A, succeed)
   *     - onTeamRankChanged(team, old, new)
   *   - etc.
   */
  public interface Observer extends ScoreboardModel.Observer {
    default void onProblemFocused(Team team, Problem problem) {}
    default void onTeamRankFinalised(Team team, int rank) {}
  }

  private static final int INVALID_RANK_STARTED = -1;
  private static final int INVALID_RANK_FINISHED = 0;

  public enum Resolution {
    FAILED_PROBLEM,
    SOLVED_PROBLEM,
    FOCUSED_TEAM,
    FOCUSED_PROBLEM,
    FINALISED_RANK,
    AWARD,
    FINISHED
  }

  @FunctionalInterface
  private interface Logger {
    void log(String s);
  }

  private final Logger logger = (DEBUG ? System.err::println : s -> {});

  private int currentRank = -1;

  private final ClicsContest contest;
  private final ScoreboardModelImpl model;

  private final JudgementDispatcher dispatcher;
  /**
   * Alias of {@link JudgementDispatcher#observers}. {@link Observer} members will get extra
   * events not sent to generic observers.
   */
  public final Set<ScoreboardModel.Observer> observers;

  private final Map<String, SortedMap<Integer, List<Submission>>> teamSubmissions = new HashMap<>();
  private final Map<String, Judgement> judgementsForSubmissions = new HashMap<>();

  // Judging a problem can be broken down into multiple actions, for example highlighting the
  // problem in the scoreboard and *then* revealing the result after a pause. This queue works
  // as a buffer to allow calculating the sequence of events once eagerly and letting clients
  // query for events one-by-one.
  private final Queue<Supplier<Resolution>> pendingActions = new ArrayDeque<>();

  public ResolverController(final ClicsContest contest) {
    this(contest, ScoreboardModelImpl.newBuilder(contest).build());
  }

  public ResolverController(final ClicsContest contest, final ScoreboardModel sourceModel) {
    this.contest = ensureJudgings(contest, sourceModel);
    this.model =
      ScoreboardModelImpl.newBuilder(contest, sourceModel)
        .withEmptyScoreboard()
        .filterSubmissions(s -> false)
        .build();

    this.dispatcher = new JudgementDispatcher(model, new Comparators.RowComparator(model));
    this.observers = dispatcher.observers;
    observers.add(this.model);
    createSubmissions(sourceModel);
  }

  private static ClicsContest ensureJudgings(ClicsContest contest, ScoreboardModel model) {
    if (contest.getJudgementsCount() != 0 || contest.getSubmissionsCount() == 0) {
      return contest;
    }
    return contest.toBuilder().putAllJudgements(inventJudgements(contest, model)).build();
  }

  /**
   * Public versions of contests often don't include any detailed information about judging
   * results. Let's invent our own to make resolving the scoreboard possible.
   */
  private static Map<String, Judgement> inventJudgements(
      ClicsContest contest,
      ScoreboardModel sourceModel) {
    Map<String, Judgement> judgements = new HashMap<>();

    Map<String, Map<String, List<Submission>>> attemptsByTeamAndProblem =
        sourceModel.getSubmissions().stream()
            .collect(Collectors.groupingBy(
                Submission::getTeamId,
                Collectors.groupingBy(
                    Submission::getProblemId)));

    Map<String, Problem> problemsByLabel = contest.getProblemsMap();

    for (ScoreboardRow row : sourceModel.getRows()) {
      for (ScoreboardProblem prob : row.getProblemsList()) {
        final boolean solved = prob.getSolved();
        long failed = prob.getNumJudged() - (solved ? 1 : 0);
        for (Submission sub : attemptsByTeamAndProblem
            .getOrDefault(row.getTeamId(), Collections.emptyMap())
            .getOrDefault(prob.getProblemId(), Collections.emptyList())) {
          final Judgement.Builder j = Judgement.newBuilder()
              .setId("j" + Integer.toHexString(judgements.size() + 1))
              .setSubmissionId(sub.getId())
              .setStartTime(sub.getTime())
              .setStartContestTime(sub.getContestTime())
              .setEndTime(sub.getTime())
              .setEndContestTime(sub.getContestTime());
          if (failed > 0) {
            j.setJudgementTypeId(contest.getJudgementTypesOrThrow("WA").getId());
            failed--;
          } else if (solved && prob.getTime() == sub.getContestTime().getSeconds() / 60) {
            j.setJudgementTypeId(contest.getJudgementTypesOrThrow("AC").getId());
          } else {
            j.setJudgementTypeId(contest.getJudgementTypesOrThrow("CE").getId());
          }
          Judgement jj = j.build();
          judgements.put(jj.getId(), jj);
        }
      }
    }
    return judgements;
  }

  public boolean finished() {
    return pendingActions.isEmpty() && currentRank == INVALID_RANK_FINISHED;
  }

  public Resolution advance() {
    if (finished()) {
      return Resolution.FINISHED;
    }
    populatePendingActions();
    return pendingActions.poll().get();
  }

  private void populatePendingActions() {
    if (currentRank == INVALID_RANK_FINISHED || !pendingActions.isEmpty()) {
      return;
    }
    if (currentRank == INVALID_RANK_STARTED) {
      currentRank = model.getTeams().size();
      final int rank = currentRank;
      pendingActions.offer(() -> moveToProblem(getTeamAt(rank), null));
      return;
    }

    final Team currentTeam = getTeamAt(currentRank);
    if (judgeNextProblem(currentTeam)) {
      return;
    }

    pendingActions.offer(() -> finaliseRank(currentTeam, currentRank));

    if (currentRank == 1) {
      pendingActions.offer(() -> moveToProblem(null, null));
      currentRank = INVALID_RANK_FINISHED;
      return;
    } else {
      currentRank--;
      final int rank = currentRank;
      pendingActions.offer(() -> moveToProblem(getTeamAt(rank), null));
    }
  }

  public ScoreboardModel getModel() {
    return model;
  }

  private Team getTeamAt(int rank) {
    return model.getTeam(model.getRow(rank - 1).getTeamId());
  }

  private void createSubmissions(ScoreboardModel sourceModel) {
    final boolean hasEnd = contest.getContest().hasContestDuration();
    final Timestamp endTime = !hasEnd ? null : Timestamps.add(
        Timestamp.getDefaultInstance(),
        contest.getContest().getContestDuration());

    final boolean hasFreeze = contest.getContest().hasScoreboardFreezeDuration();
    final Timestamp freezeTime = !hasFreeze ? null : Timestamps.subtract(
        endTime,
        contest.getContest().getScoreboardFreezeDuration());

    for (Judgement j : contest.getJudgementsMap().values()) {
      judgementsForSubmissions.put(j.getSubmissionId(), j);
    }

    sourceModel.getSubmissions().forEach(submission -> {
      final Duration afterEnd = !hasEnd ? null : Timestamps.between(
          endTime,
          Timestamps.add(
              Timestamp.getDefaultInstance(),
              submission.getContestTime()));
      if (hasEnd && Durations.toNanos(afterEnd) >= 0) {
        return;
      }

      final Duration intoFreeze = !hasFreeze ? null : Timestamps.between(
          freezeTime,
          Timestamps.add(
              Timestamp.getDefaultInstance(),
              submission.getContestTime()));
      dispatcher.notifySubmission(submission);
      if (hasFreeze && (intoFreeze.getSeconds() >= 0 || intoFreeze.getNanos() >= 0)) {
        addPendingSubmission(submission);
      } else {
        judgeSubmission(submission);
      }
    });
  }

  private ScoreboardProblem judgeSubmission(Submission submission) {
    Judgement judgement = judgementsForSubmissions.get(submission.getId());
    if (judgement == null) {
      throw new Error("Submission " + submission + " has no judgement");
    }
    return dispatcher.notifyJudgement(judgement);
  }

  private Stream<Observer> getResolverObservers() {
    return observers.stream().filter(Observer.class::isInstance).map(Observer.class::cast);
  }

  private void addPendingSubmission(final Submission submission) {
    final Problem problem = model.getProblem(submission.getProblemId());

    // The submission needs to exist.
    final Team team;
    try {
      team = model.getTeam(submission.getTeamId());
    } catch (NoSuchElementException e) {
      return;
    }

    // The problem needs to be pending (unsolved) as of the freeze.
    if (model.getAttempts(team, problem).getSolved()) {
      return;
    }

    teamSubmissions
        .computeIfAbsent(team.getId(), k -> new TreeMap<>())
        .computeIfAbsent(problem.getOrdinal(), k -> new ArrayList<>())
        .add(submission);
  }

  private boolean judgeNextProblem(final Team team) {
    logger.log("judgeNextProblem " + team.getId());

    if (!teamSubmissions.containsKey(team.getId())) {
      return false;
    }

    final int problemOrdinal = teamSubmissions.get(team.getId()).firstKey();
    final List<Submission> attempts = teamSubmissions.get(team.getId()).remove(problemOrdinal);
    if (teamSubmissions.get(team.getId()).size() == 0) {
      teamSubmissions.remove(team.getId());
    }

    final String problemId = attempts.get(0).getProblemId();
    pendingActions.offer(() -> moveToProblem(team, contest.getProblemsOrThrow(problemId)));
    pendingActions.offer(() -> judgeSubmissions(team, attempts));
    return true;
  }

  private Resolution judgeSubmissions(final Team team, final List<Submission> attempts) {
    boolean solved = false;
    for (Submission attempt : attempts) {
      if (judgeSubmission(attempt).getSolved()) {
        solved = true;
      }
    }
    if (solved) {
      final Team teamAtRankNow = getTeamAt(currentRank);
      if (teamAtRankNow != team) {
        pendingActions.offer(() -> moveToProblem(teamAtRankNow, null));
      }
    }
    return solved ? Resolution.SOLVED_PROBLEM : Resolution.FAILED_PROBLEM;
  }

  private Resolution finaliseRank(Team team, int rank) {
    getResolverObservers().forEach(x -> x.onTeamRankFinalised(team, rank));
    return Resolution.FINALISED_RANK;
  }

  private Resolution moveToProblem(final Team team, final Problem problem) {
    getResolverObservers().forEach(o -> o.onProblemFocused(team, problem));
    return problem != null
        ? Resolution.FOCUSED_PROBLEM : team != null ? Resolution.FOCUSED_TEAM : Resolution.FINISHED;
  }
}
