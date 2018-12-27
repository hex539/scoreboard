package me.hex539.contest;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import edu.clics.proto.ClicsProto.*;

public class ResolverController {

  /** Number of events we'll try to stay ahead by. Zero or negative means no limit. */
  private static final int DEFAULT_BUFFER_AHEAD = 256;

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

  private final ScoreboardModel.Observer proxyObserver = ObserverCapturer.captivate(
      new ScoreboardModel.Observer() {},
      ScoreboardModel.Observer.class,
      this::addScoreboardEvent);

  public enum Resolution {
    FAILED_PROBLEM,
    SOLVED_PROBLEM,
    FOCUSED_TEAM,
    FOCUSED_PROBLEM,
    FINALISED_RANK,
    AWARD,
    STARTED,
    FINISHED
  }

  /**
   * Another cheap reimplementation of Either to handle messages. Good candidate for replacement
   * by protobuf+grpc later on to support working as a client/server for other scoreboard replay
   * tools in future.
   */
  @AutoValue
  abstract static class Advancer {
    @Nullable public abstract Consumer<Observer> focus();
    @Nullable public abstract Consumer<ScoreboardModel.Observer> scoreboard();
    @Nullable public abstract Resolution resolution();

    public static Advancer focus(Consumer<Observer> focus) {
      return new AutoValue_ResolverController_Advancer(
          /* focus= */ Preconditions.checkNotNull(focus),
          /* scoreboard= */ null,
          /* resolution= */ null);
    }

    public static Advancer scoreboard(Consumer<ScoreboardModel.Observer> scoreboard) {
      return new AutoValue_ResolverController_Advancer(
          /* focus= */ null,
          /* scoreboard= */ Preconditions.checkNotNull(scoreboard),
          /* resolution= */ null);
    }

    public static Advancer resolution(Resolution resolution) {
      return new AutoValue_ResolverController_Advancer(
          /* focus= */ null,
          /* scoreboard= */ null,
          /* resolution= */ Preconditions.checkNotNull(resolution));
    }
  }

  private void addFocusEvent(Consumer<Observer> focus) {
    try {pendingActions.put(Advancer.focus(focus));} catch (InterruptedException e) {}
  }

  private void addScoreboardEvent(Consumer<ScoreboardModel.Observer> scoreboard) {
    try {pendingActions.put(Advancer.scoreboard(scoreboard));} catch (InterruptedException e) {}
  }

  private void addResolution(Resolution resolution) {
    try {pendingActions.put(Advancer.resolution(resolution));} catch (InterruptedException e) {}
  }

  private final ClicsContest contest;
  private final ScoreboardModelImpl model;
  private final JudgementDispatcher dispatcher;
  private final Thread resolutionThread;

  private final Map<String, SortedMap<Integer, List<Submission>>> teamSubmissions = new HashMap<>();
  private final Map<String, Judgement> judgementsForSubmissions = new HashMap<>();

  private final LinkedBlockingQueue<Advancer> pendingActions;
  private final Client client;

  public ResolverController(ClicsContest contest) {
    this(contest, ScoreboardModelImpl.newBuilder(contest).build());
  }

  public ResolverController(ClicsContest contest, ScoreboardModel sourceModel) {
    this(contest, sourceModel, false);
  }

  public ResolverController(ClicsContest contest, ScoreboardModel sourceModel, boolean showCompileErrors) {
    this(contest, sourceModel, showCompileErrors, DEFAULT_BUFFER_AHEAD);
  }

  public ResolverController(ClicsContest contest, ScoreboardModel sourceModel, boolean showCompileErrors, int bufferAhead) {
    this.contest = MissingJudgements.ensureJudgements(contest);
    this.model =
      ScoreboardModelImpl.newBuilder(contest, sourceModel)
        .withEmptyScoreboard()
        .filterSubmissions(s -> false)
        .build();

    this.dispatcher = new JudgementDispatcher(model, showCompileErrors);
    this.dispatcher.observers.add(this.model);
    this.dispatcher.observers.add(this.proxyObserver);

    this.pendingActions = bufferAhead > 0
        ? new LinkedBlockingQueue<>(bufferAhead)
        : new LinkedBlockingQueue<>();

    this.client = new Client(pendingActions);
    this.resolutionThread = new Thread(() -> {
      createSubmissions(sourceModel);
      resolveContest();
    });
    this.resolutionThread.start();
  }

  public ResolverController addObserver(ScoreboardModel.Observer observer) {
    client.observers.add(observer);
    return this;
  }

  public ResolverController removeObserver(ScoreboardModel.Observer observer) {
    client.observers.remove(observer);
    return this;
  }

  public boolean finished() {
    return client.finished();
  }

  public Resolution advance() {
    return client.advance();
  }

  public void drain() {
    while (!finished()) {
      advance();
    }
  }

  private static class Client {
    private final LinkedBlockingQueue<Advancer> pendingActions;
    private Resolution lastResolution = null;

    public final Set<ScoreboardModel.Observer> observers = new HashSet<>();

    public Client(LinkedBlockingQueue<Advancer> pendingActions) {
      this.pendingActions = pendingActions;
    }

    public boolean finished() {
      return lastResolution == Resolution.FINISHED;
    }

    public Resolution advance() {
      if (finished()) {
        return Resolution.FINISHED;
      }
      try {
        for (Advancer next; (next = pendingActions.take()) != null;) {
          if (next.focus() != null) {
            for (ScoreboardModel.Observer observer : observers) {
              if (observer instanceof Observer) {
                next.focus().accept((Observer) observer);
              }
            }
          } else if (next.scoreboard() != null) {
            observers.forEach(next.scoreboard());
          } else {
            return (lastResolution = next.resolution());
          }
        }
      } catch (InterruptedException e) {
      }
      return (lastResolution = Resolution.FINISHED);
    }
  }

  public void resolveContest() {
    Team currentTeam = null;
    Team prevTeam = null;

    for (int currentRank = model.getTeams().size(); currentRank > 0; prevTeam = currentTeam) {
      currentTeam = getTeamAt(currentRank);
      if (currentTeam != prevTeam) {
        moveToProblem(currentTeam, null);
      }
      if (!judgeNextProblem(currentTeam, currentRank)) {
        finaliseRank(currentTeam, currentRank);
        currentRank--;
      }
    }

    moveToProblem(null, null);
  }

  private ScoreboardModel getModel() {
    return model;
  }

  private Team getTeamAt(int rank) {
    return model.getTeam(model.getRow(rank - 1).getTeamId());
  }

  private void createSubmissions(ScoreboardModel sourceModel) {
    final Timestamp endTime = contest.getContest().hasContestDuration()
        ? Timestamps.add(Timestamp.getDefaultInstance(), contest.getContest().getContestDuration())
        : null;

    final Timestamp freezeTime = contest.getContest().hasScoreboardFreezeDuration()
        ? Timestamps.subtract(endTime, contest.getContest().getScoreboardFreezeDuration())
        : null;

    for (Judgement j : contest.getJudgementsMap().values()) {
      judgementsForSubmissions.put(j.getSubmissionId(), j);
    }

    sourceModel.getSubmissions().forEach(submission -> {
      if (endTime != null) {
        final Duration afterEnd = Timestamps.between(
            endTime,
            Timestamps.add(
                Timestamp.getDefaultInstance(),
                submission.getContestTime()));
        if (Durations.toNanos(afterEnd) >= 0) {
          return;
        }
      }

      dispatcher.notifySubmission(submission);

      if (freezeTime != null) {
        final Duration intoFreeze = Timestamps.between(
            freezeTime,
            Timestamps.add(
                Timestamp.getDefaultInstance(),
                submission.getContestTime()));

        if (intoFreeze.getSeconds() >= 0) {
          addPendingSubmission(submission);
          return;
        }
      }

      judgeSubmission(submission);
    });
    addResolution(Resolution.STARTED);
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

  private boolean judgeNextProblem(final Team team, final int currentRank) {
    if (!teamSubmissions.containsKey(team.getId())) {
      return false;
    }

    final int problemOrdinal = teamSubmissions.get(team.getId()).firstKey();
    final List<Submission> attempts = teamSubmissions.get(team.getId()).remove(problemOrdinal);
    if (teamSubmissions.get(team.getId()).size() == 0) {
      teamSubmissions.remove(team.getId());
    }

    moveToProblem(team, contest.getProblemsOrThrow(attempts.get(0).getProblemId()));
    judgeSubmissions(team, currentRank, attempts);
    return true;
  }

  private void judgeSubmissions(Team team, int currentRank, List<Submission> attempts) {
    boolean solved = false;
    for (Submission s : attempts) {
      if (judgeSubmission(s).filter(ScoreboardProblem::getSolved).isPresent()) {
        solved = true;
      }
    }
    addResolution(solved ? Resolution.SOLVED_PROBLEM : Resolution.FAILED_PROBLEM);
  }

  private Optional<ScoreboardProblem> judgeSubmission(Submission submission) {
    return Optional.ofNullable(judgementsForSubmissions.get(submission.getId()))
        .map(dispatcher::notifyJudgement);
  }

  private void finaliseRank(Team team, int rank) {
    addFocusEvent(o -> o.onTeamRankFinalised(team, rank));
    addResolution(Resolution.FINALISED_RANK);
  }

  private void moveToProblem(final Team team, final Problem problem) {
    addFocusEvent(o -> o.onProblemFocused(team, problem));
    addResolution(problem != null ? Resolution.FOCUSED_PROBLEM
        : team != null ? Resolution.FOCUSED_TEAM
        : Resolution.FINISHED);
  }
}
