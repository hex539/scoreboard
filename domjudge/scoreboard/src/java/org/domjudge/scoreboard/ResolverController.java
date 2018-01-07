package org.domjudge.scoreboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.domjudge.proto.DomjudgeProto.*;

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
   *     - onProblemAttempted(team, A, fail)
   *     - onProblemAttempted(team, A, succeed)
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

  /**
   * Alias of {@link JudgingDispatcher#observers}. {@link Observer} members will get extra
   * events not sent to generic observers.
   */
  public final Set<ScoreboardModel.Observer> observers;

  private final EntireContest contest;
  private final ScoreboardModel model;
  private final JudgingDispatcher dispatcher;

  private final Map<Long, Judging> judgings;

  private final Map<Long, SortedMap<Long, List<Submission>>> teamSubmissions = new HashMap<>();
  private final Map<Long, TeamKey> teamKeys = new HashMap<>();
  private final PriorityQueue<TeamKey> teamOrdering = new PriorityQueue<>();

  // Judging a problem can be broken down into multiple actions, for example highliting the
  // problem in the scoreboard and *then* revealing the result after a pause. This queue works
  // as a buffer to allow calculating the sequence of events once eagerly and letting clients
  // query for events one-by-one.
  private final Queue<Runnable> pendingActions = new ArrayDeque<>();

  public ResolverController(final EntireContest contest) {
    this(contest, ScoreboardModelImpl.create(contest).withoutSubmissions());
  }

  public ResolverController(final EntireContest contest, final ScoreboardModelImpl model) {
    this.contest = contest;
    this.model = model;

    this.judgings = getOrInventJudgings();

    this.dispatcher = new JudgingDispatcher(model);
    this.observers = dispatcher.observers;
    observers.add(model);

    createSubmissions();
  }

  private Map<Long, Judging> getOrInventJudgings() {
    List<Judging> judgings = contest.getJudgingsCount() != 0
        ? contest.getJudgingsList()
        : inventJudgings(contest);
    return judgings.stream()
        .collect(Collectors.toMap(Judging::getSubmission, Function.identity(), (a, b) -> b));
  }

  /**
   * Public versions of contests often don't include any detailed information about judging
   * results. Let's invent our own to make resolving the scoreboard possible.
   */
  private static List<Judging> inventJudgings(EntireContest contest) {
    List<Judging> judgings = new ArrayList<>(contest.getSubmissionsCount());

    Map<Long, Map<Long, List<Submission>>> attemptsByTeamAndProblem =
        contest.getSubmissionsList().stream()
            .collect(Collectors.groupingBy(
                Submission::getTeam,
                Collectors.groupingBy(
                    Submission::getProblem)));

    Map<String, Problem> problemsByLabel = contest.getProblemsList().stream()
        .collect(Collectors.toMap(Problem::getLabel, Function.identity()));

    for (ScoreboardRow row : contest.getScoreboardList()) {
      for (ScoreboardProblem prob : row.getProblemsList()) {
        final long probId = problemsByLabel.get(prob.getLabel()).getId();
        final boolean solved = prob.getSolved();
        long failed = prob.getNumJudged() - (solved ? 1 : 0);
        for (Submission sub : attemptsByTeamAndProblem
            .getOrDefault(row.getTeam(), Collections.emptyMap())
            .getOrDefault(probId, Collections.emptyList())) {
          final Judging.Builder j = Judging.newBuilder()
              .setId(judgings.size() + 1)
              .setSubmission(sub.getId())
              .setTime(sub.getTime() + 20);
          if (failed > 0) {
            j.setOutcome("wrong-answer");
            failed--;
          } else if (solved) {
            j.setOutcome("correct");
          } else {
            j.setOutcome("compiler-error");
          }
          judgings.add(j.build());
        }
      }
    }
    return judgings;
  }

  public boolean finished() {
    return pendingActions.isEmpty() && teamOrdering.isEmpty();
  }

  public void advance() {
    if (pendingActions.isEmpty()) {
      removeInvalidatedKeys();
      judgeNextProblem(teamOrdering.poll().team);
    }
    pendingActions.poll().run();
  }

  private void createSubmissions() {
    for (Submission submission : contest.getSubmissionsList()) {
      dispatcher.notifySubmission(submission);

      if (submission.getTime() < contest.getContest().getFreeze()) {
        dispatcher.notifyJudging(judgings.get(submission.getId()));
      } else {
        addPendingSubmission(submission);
      }
    }

    contest.getTeamsList().forEach(this::invalidateTeamOrdering);
  }

  private Stream<Observer> getResolverObservers() {
    return observers.stream().filter(Observer.class::isInstance).map(Observer.class::cast);
  }

  private void addPendingSubmission(final Submission submission) {
    teamSubmissions
        .computeIfAbsent(submission.getTeam(), k -> new TreeMap<>())
        .computeIfAbsent(submission.getProblem(), k -> new ArrayList<>())
        .add(submission);
  }

  private void judgeNextProblem(final Team team) {
    logger.log("judgeNextProblem " + team.getId());

    final long problemId = teamSubmissions.get(team.getId()).firstKey();
    final List<Submission> attempts = teamSubmissions.get(team.getId()).remove(problemId);
    if (teamSubmissions.get(team.getId()).size() == 0) {
      teamSubmissions.remove(team.getId());
    }

    pendingActions.offer(() -> moveToProblem(team, model.getProblem(problemId)));
    pendingActions.offer(() -> judgeSubmissions(team, attempts));
  }

  private void moveToProblem(final Team team, final Problem problem) {
    getResolverObservers().forEach(o -> o.onProblemFocused(team, problem));
  }

  private void judgeSubmissions(final Team team, final List<Submission> attempts) {
    attempts.stream().map(Submission::getId).map(judgings::get).forEach(dispatcher::notifyJudging);
    invalidateTeamOrdering(team);
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
    public final Team team;
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
      return -Comparators.compareRows(team, row, other.team, other.row);
    }
  }
}
