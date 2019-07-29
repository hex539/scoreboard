package me.hex539.contest;

import static com.google.common.truth.Truth.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.util.Durations;
import edu.clics.proto.ClicsProto.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import me.hex539.contest.ResolverController.Observer;
import me.hex539.contest.ResolverController.Resolution;
import me.hex539.contest.model.Ranklist;
import org.junit.Test;

public class ResolverTest {
  @Test
  public void testResolveNwerc2007() throws Exception {
    ClicsContest entireContest =
        new ContestDownloader(getClass().getResourceAsStream("/resources/contests/nwerc2007.pb")).fetch();

    // NWERC 2007 had a penalty for compile-error, but the demoweb api fails to say so.
    // Fix it so that our resolution matches the real one.
    entireContest = entireContest.toBuilder()
        .putJudgementTypes("CE", entireContest.getJudgementTypesOrThrow("CE").toBuilder()
            .setPenalty(true)
            .build())
        .build();

    final ScoreboardModel reference =
        ScoreboardModelImpl.newBuilder(entireContest)
            .filterGroups(g -> "1".equals(g.getId()))
            .filterTooLateSubmissions()
            .build()
            .immutable();

    final ScoreboardModelImpl model =
        ScoreboardModelImpl.newBuilder(entireContest, reference)
            .withEmptyScoreboard()
            .filterSubmissions(s -> false)
            .build();

    ResolverController resolver = new ResolverController(entireContest, reference, false)
        .addObserver(model);

    assertThat(resolver.advance()).isEqualTo(Resolution.STARTED);
    Observer observer = mock(Observer.class);
    resolver.addObserver(observer);
    resolver.drain();

    verify(observer, never()).onProblemSubmitted(any(), any());
    verify(observer, times(215)).onProblemScoreChanged(any(), any());
    verify(observer, times(29)).onScoreChanged(any(), any());
    verify(observer, times(28)).onTeamRankChanged(any(), anyInt(), anyInt());
    verify(observer, times(51)).onTeamRankFinalised(any(), anyInt());

    final Ranklist rl = model.getRanklistModel();
    assertThat(rl.getRows().size()).isEqualTo(51);
    for (int i = 0; i < 51; i++) {
      assertThat(rl.getRow(i)).isEqualTo(reference.getRanklistModel().getRow(i));
      assertThat(rl.getRow(i)).isEqualTo(
          entireContest.getScoreboard(i).toBuilder().setRank(i + 1).build());
    }
  }

  @Test
  public void testResolveNwerc2017() throws Exception {
    final ClicsContest entireContest =
        new ContestDownloader(getClass().getResourceAsStream("/resources/contests/nwerc2017.pb")).fetch();

    final ScoreboardModel reference =
        ScoreboardModelImpl.newBuilder(entireContest)
            .filterGroups(g -> "12890".equals(g.getId()))
            .filterTooLateSubmissions()
            .build()
            .immutable();

    final ScoreboardModelImpl model =
        ScoreboardModelImpl.newBuilder(entireContest, reference)
            .withEmptyScoreboard()
            .filterSubmissions(s -> false)
            .build();

    ResolverController resolver = new ResolverController(entireContest, reference, false)
        .addObserver(model);

    assertThat(resolver.advance()).isEqualTo(Resolution.STARTED);
    Observer observer = mock(Observer.class);
    resolver.addObserver(observer);
    resolver.drain();

    verify(observer, never()).onProblemSubmitted(any(), any());
    verify(observer, times(399)).onProblemScoreChanged(any(), any());
    verify(observer, times(53)).onScoreChanged(any(), any());
    verify(observer, times(53)).onTeamRankChanged(any(), anyInt(), anyInt());
    verify(observer, times(120)).onTeamRankFinalised(any(), anyInt());

    final Ranklist rl = model.getRanklistModel();
    assertThat(rl.getRows().size()).isEqualTo(120);
    for (int i = 0; i < 120; i++) {
      assertThat(rl.getRow(i)).isEqualTo(reference.getRanklistModel().getRow(i));
      assertThat(rl.getRow(i)).isEqualTo(
          entireContest.getScoreboard(i).toBuilder().setRank(i + 1).build());
    }
  }

  @Test
  public void testResolveNwerc2018() throws Exception {
    final ClicsContest entireContest =
        new ContestDownloader(getClass().getResourceAsStream("/resources/contests/nwerc2018.pb")).fetch();

    final ScoreboardModel reference =
        ScoreboardModelImpl.newBuilder(entireContest)
            .filterGroups(g -> !g.getHidden())
            .filterTooLateSubmissions()
            .build()
            .immutable();

    final ScoreboardModelImpl model =
        ScoreboardModelImpl.newBuilder(entireContest, reference)
            .withEmptyScoreboard()
            .filterSubmissions(s -> false)
            .build();

    ResolverController resolver = new ResolverController(entireContest, reference, false)
        .addObserver(model);

    assertThat(resolver.advance()).isEqualTo(Resolution.STARTED);
    Observer observer = mock(Observer.class);
    resolver.addObserver(observer);
    resolver.drain();

    verify(observer, never()).onProblemSubmitted(any(), any());
    verify(observer, times(412)).onProblemScoreChanged(any(), any());
    verify(observer, times(69)).onScoreChanged(any(), any());
    verify(observer, times(68)).onTeamRankChanged(any(), anyInt(), anyInt());
    verify(observer, times(122)).onTeamRankFinalised(any(), anyInt());

    final Ranklist rl = model.getRanklistModel();
    assertThat(rl.getRows().size()).isEqualTo(entireContest.getScoreboardCount());
    for (int i = 0; i < entireContest.getScoreboardCount(); i++) {
      assertThat(rl.getRow(i)).isEqualTo(reference.getRanklistModel().getRow(i));
      assertThat(rl.getRow(i)).isEqualTo(
          entireContest.getScoreboard(i).toBuilder().setRank(i + 1).build());
    }
  }

  @Test
  public void testResolveLargeNumberOfTeams() throws Exception {
    final int n = 10_000;

    Instant startBuild = Instant.now();

    final ClicsContest.Builder ccBuilder = ClicsContest.newBuilder()
        .setContest(Contest.newBuilder()
            .setContestDuration(Durations.fromMillis(TimeUnit.HOURS.toMillis(2)))
            .setScoreboardFreezeDuration(Durations.fromMillis(TimeUnit.HOURS.toMillis(1)))
            .setPenaltyTime(20)
            .build())
        .putJudgementTypes("AC", JudgementType.newBuilder().setId("AC").setSolved(true).build())
        .putProblems("A", Problem.newBuilder().setId("A").setLabel("A").setOrdinal(0).build())
        .putProblems("B", Problem.newBuilder().setId("B").setLabel("B").setOrdinal(1).build())
        .putGroups("P", Group.newBuilder().setId("P").setName("Participants").build());
    for (int i = 0; i < n; i++) {
      final String teamId = String.format("t%08d", i);
      ccBuilder.putTeams(teamId, Team.newBuilder().setId(teamId).setName(teamId).addGroupIds("P").build());
      ccBuilder.putSubmissions(teamId + "s1", Submission.newBuilder()
          .setId(teamId + "s1").setProblemId("A").setTeamId(teamId)
          .setContestTime(Durations.fromMillis(TimeUnit.MINUTES.toMillis(100)))
          .build());
      ccBuilder.putSubmissions(teamId + "s2", Submission.newBuilder()
          .setId(teamId + "s2").setProblemId("B").setTeamId(teamId)
          .setContestTime(Durations.fromMillis(TimeUnit.MINUTES.toMillis(100)))
          .build());
      ccBuilder.addScoreboard(ScoreboardRow.newBuilder()
          .setRank(i + 1).setTeamId(teamId)
          .setScore(ScoreboardScore.newBuilder().setNumSolved(2).setTotalTime(200))
          .addProblems(ScoreboardProblem.newBuilder()
              .setProblemId("A").setNumJudged(1).setSolved(true).setTime(100).build())
          .addProblems(ScoreboardProblem.newBuilder()
              .setProblemId("B").setNumJudged(1).setSolved(true).setTime(100).build())
          .build());
    }
    final ClicsContest entireContest = ccBuilder.build();

    Instant startModel = Instant.now();
    ScoreboardModel reference = ScoreboardModelImpl.newBuilder(entireContest).build();
    Instant startImmutable = Instant.now();
    reference = reference.immutable();
    assertThat(reference.getTeamsModel().getTeams().size()).isEqualTo(n);

    Instant createResolve = Instant.now();

    ResolverController resolver = new ResolverController(entireContest, reference);
    assertThat(resolver.advance()).isEqualTo(Resolution.STARTED);
    Observer observer = mock(Observer.class);
    resolver.addObserver(observer);
    resolver.advance();

    Instant startResolve = Instant.now();

    resolver.drain();

    Instant finishResolve = Instant.now();

    System.err.println("");
    System.err.println("Time to build CLICS:    " + Duration.between(startBuild, startModel));
    System.err.println("Time to build model:    " + Duration.between(startModel, startImmutable));
    System.err.println("Time to finish model:   " + Duration.between(startImmutable, createResolve));
    System.err.println("Time to start resolve:  " + Duration.between(createResolve, startResolve));
    System.err.println("Time to finish resolve: " + Duration.between(startResolve, finishResolve));

    verify(observer, times(2 * n)).onTeamRankChanged(any(), anyInt(), eq(1));
  }
}
