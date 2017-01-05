package me.hex539.resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.domjudge.api.ScoreboardModel;
import org.domjudge.proto.DomjudgeProto;

/**
 * TODO: Move this into tests for the scoreboard lib. Possibly expose a convenience
 *       library too, with a syntax along the lines of:
 *       <code>
 *       ScoreboardMocks.FakeModel.newBuilder()
 *            .setProblems(     "A",    "B",     "C",     "D")
 *            .addRow("Team 1", SOLVED, SOLVED,  PENDING, FAILED)
 *            .addRow("Team 2", SOLVED, PENDING, PENDING, FAILED)
 *            .addRow("Team 3", FAILED, null,    null,    PENDING)
 *            .build()
 *       </code>
 */
public final class MockModel implements ScoreboardModel{
    @Override
    public DomjudgeProto.Contest getContest() {
      return DomjudgeProto.Contest.newBuilder()
          .setId(44)
          .setShortName("Trial")
          .setName("Challenge")
          .build();
    }

    @Override
    public Collection<DomjudgeProto.Team> getTeams() {
      /**
       * Some tricky cases: Unicode 9.0, 6.0, and 1.1 respectively.
       */
      return Arrays.asList(new DomjudgeProto.Team[] {
          DomjudgeProto.Team.newBuilder().setId(1).setName("Bath Ducks 🦆").build(),
          DomjudgeProto.Team.newBuilder().setId(2).setName("Bath Crocs 🐊").build(),
          DomjudgeProto.Team.newBuilder().setId(3).setName("Bath Shower ☂").build()
      });
    }

    @Override
    public Collection<DomjudgeProto.Problem> getProblems() {
      return Arrays.asList(new DomjudgeProto.Problem[] {
        DomjudgeProto.Problem.newBuilder()
            .setLabel("X")
            .setName("Example problem")
            .setShortName("Example")
            .build()
      });
    }

    @Override
    public Collection<DomjudgeProto.ScoreboardRow> getRows() {
      return Arrays.asList(new DomjudgeProto.ScoreboardRow[] {
        DomjudgeProto.ScoreboardRow.newBuilder()
            .setTeam(1)
            .setRank(1)
            .setScore(DomjudgeProto.ScoreboardScore.newBuilder()
                .setNumSolved(1)
                .setTotalTime(23)
                .build())
            .addProblems(DomjudgeProto.ScoreboardProblem.newBuilder()
                  .setLabel("X")
                  .setSolved(true)
                  .setNumJudged(1)
                  .build())
            .build(),
        DomjudgeProto.ScoreboardRow.newBuilder()
            .setTeam(2)
            .setRank(2)
            .setScore(DomjudgeProto.ScoreboardScore.newBuilder()
                .setNumSolved(1)
                .setTotalTime(500)
                .build())
            .addProblems(DomjudgeProto.ScoreboardProblem.newBuilder()
                  .setLabel("X")
                  .setSolved(true)
                  .setNumJudged(1)
                  .build())
            .build(),
        DomjudgeProto.ScoreboardRow.newBuilder()
            .setTeam(3)
            .setRank(3)
            .setScore(DomjudgeProto.ScoreboardScore.newBuilder()
                .setNumSolved(0)
                .setTotalTime(0)
                .build())
            .addProblems(DomjudgeProto.ScoreboardProblem.newBuilder()
                  .setLabel("X")
                  .setSolved(true)
                  .setNumJudged(1)
                  .build())
            .build()});
  }
}
