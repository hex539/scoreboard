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
  }

  @FunctionalInterface
  private interface Logger {
    void log(String s);
  }

  private final Logger logger = (DEBUG ? System.err::println : s -> {});

  private boolean started = false;

  private final ClicsContest contest;
  private final ScoreboardModel sourceModel;
  private final ScoreboardModelImpl model;

  private final JudgementDispatcher dispatcher;
  /**
   * Alias of {@link JudgementDispatcher#observers}. {@link Observer} members will get extra
   * events not sent to generic observers.
   */
  public final Set<ScoreboardModel.Observer> observers;

  private final Comparators.RowComparator rowComparator;

  private final Map<String, SortedMap<Integer, List<Submission>>> teamSubmissions = new HashMap<>();
  private final Map<String, TeamKey> teamKeys = new HashMap<>();
  private final PriorityQueue<TeamKey> teamOrdering = new PriorityQueue<>();

  private final Map<String, Judgement> judgementsForSubmissions = new HashMap<>();

  // Judging a problem can be broken down into multiple actions, for example highliting the
  // problem in the scoreboard and *then* revealing the result after a pause. This queue works
  // as a buffer to allow calculating the sequence of events once eagerly and letting clients
  // query for events one-by-one.
  private final Queue<Runnable> pendingActions = new ArrayDeque<>();

  public ResolverController(final ClicsContest contest) {
    this(contest, ScoreboardModelImpl.newBuilder(contest).build());
  }

  public ResolverController(final ClicsContest contest, final ScoreboardModel sourceModel) {
    this.contest = ensureJudgings(contest, sourceModel);
    this.sourceModel = sourceModel;
    this.model = // ((ScoreboardModelImpl) sourceModel).toBuilder()
      ScoreboardModelImpl.newBuilder(contest, sourceModel)
        .withEmptyScoreboard()
        .filterSubmissions(s -> false)
        .build();

    this.rowComparator = new Comparators.RowComparator(model);

    this.dispatcher = new JudgementDispatcher(model, rowComparator);
    this.observers = dispatcher.observers;
    observers.add(this.model);

  }

  public void start() {
    if (!started) {
      createSubmissions();
      started = true;
    }
  }

  private static ClicsContest ensureJudgings(ClicsContest contest, ScoreboardModel model) {
//    if (contest.getJudgementsCount() != 0 || contest.getSubmissionsCount() == 0) {
//      return contest;
//    }
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
    return pendingActions.isEmpty() && teamOrdering.isEmpty();
  }

  public void advance() {
    if (!started) {
      start();
      return;
    }
    if (pendingActions.isEmpty()) {
      removeInvalidatedKeys();
      judgeNextProblem(teamOrdering.poll().team);
    }
    pendingActions.poll().run();
  }

  private void createSubmissions() {
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

    sourceModel.getTeams().forEach(this::invalidateTeamOrdering);
  }

  private void judgeSubmission(Submission submission) {
    if (!judgementsForSubmissions.containsKey(submission.getId())) {
      throw new Error("Submission " + submission + " has no judgement");
    }
    dispatcher.notifyJudgement(judgementsForSubmissions.get(submission.getId()));
  }

  private Stream<Observer> getResolverObservers() {
    return observers.stream().filter(Observer.class::isInstance).map(Observer.class::cast);
  }

  private void addPendingSubmission(final Submission submission) {
    final Problem problem = model.getProblem(submission.getProblemId());
    final Team team;
    try {
      team = model.getTeam(submission.getTeamId());
    } catch (NoSuchElementException e) {
      return;
    }
    teamSubmissions
        .computeIfAbsent(team.getId(), k -> new TreeMap<>())
        .computeIfAbsent(problem.getOrdinal(), k -> new ArrayList<>())
        .add(submission);
  }

  private void judgeNextProblem(final Team team) {
    logger.log("judgeNextProblem " + team.getId());

    final int problemOrdinal = teamSubmissions.get(team.getId()).firstKey();
    final List<Submission> attempts = teamSubmissions.get(team.getId()).remove(problemOrdinal);
    if (teamSubmissions.get(team.getId()).size() == 0) {
      teamSubmissions.remove(team.getId());
    }

    final String problemId = attempts.get(0).getProblemId();
    pendingActions.offer(() -> moveToProblem(team, contest.getProblemsOrThrow(problemId)));
    pendingActions.offer(() -> judgeSubmissions(team, attempts));
  }

  private void moveToProblem(final Team team, final Problem problem) {
    getResolverObservers().forEach(o -> o.onProblemFocused(team, problem));
  }

  private void judgeSubmissions(final Team team, final List<Submission> attempts) {
    attempts.forEach(this::judgeSubmission);
    invalidateTeamOrdering(team);
    if (finished()) {
      pendingActions.offer(() -> moveToProblem(null, null));
    }
  }

  private void invalidateTeamOrdering(final Team team) {
    logger.log("invalidateTeamOrdering " + team.getId());
    TeamKey key =
        teamSubmissions.containsKey(team.getId())
            ? new TeamKey(team, model.getRow(team))
            : null;
    Optional.ofNullable(teamKeys.put(team.getId(), key)).ifPresent(TeamKey::invalidate);
    Optional.ofNullable(key).ifPresent(teamOrdering::add);
    removeInvalidatedKeys();
  }

  private void removeInvalidatedKeys() {
    while (teamOrdering.peek() != null && teamOrdering.peek().invalidated) {
      teamOrdering.poll();
    }
  }

  /**
   * Priority queue comparator holder for finding the least-ranked team with at least one
   * submission still to be judged.
   *
   * Once an item becomes out-of-date it will be invalidated but left in the priority queue
   * since this is more efficient than trying to remove it, and also more efficient than
   * using something like TreeSet which has good asymptotics but worse real-world performance
   * since it's based on a pointer tree instead of an array-backed heap.
   */
  private class TeamKey implements Comparable<TeamKey> {
    private final Team team;
    private final ScoreboardRow row;
    public boolean invalidated = false;

    public TeamKey(final Team team, final ScoreboardRow row) {
      this.team = team;
      this.row = row;
    }

    public void invalidate() {
      invalidated = true;
    }

    @Override
    public int compareTo(TeamKey other) {
      return -rowComparator.compare(row, other.row);
    }
  }
}
