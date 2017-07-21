package org.domjudge.api;

import org.junit.Test;

import me.hex539.testing.utils.MockScoreboardModel;

import static org.domjudge.proto.DomjudgeProto.*;
import static org.mockito.Mockito.*;
import static com.google.common.truth.Truth.*;

public class ModelObserverTest {
  @Test
  public void observeRankChange() {
    ScoreboardModel.Observer observer = mock(ScoreboardModel.Observer.class);

    ScoreboardModelImpl model = ScoreboardModelImpl.create(
        new MockScoreboardModel.Builder()
            .setProblems(     "A", "B", "C")
            .addRow("Team 1", "+", " ", "+")
            .addRow("Team 2", " ", "+", " ")
            .addRow("Team 3", " ", " ", " ")
            .build());
    JudgingDispatcher dispatcher = new JudgingDispatcher(model);

    dispatcher.observers.add(model);
    dispatcher.observers.add(observer);

    final Team team2 = model.getTeams()
        .stream()
        .filter(x -> "Team 2".equals(x.getName()))
        .findAny()
        .get();
    final Problem problemC = model.getProblems()
        .stream()
        .filter(x -> "C".equals(x.getName()))
        .findAny()
        .get();

    // Submit problem C for team 2
    dispatcher.notifySubmission(
        Submission.newBuilder()
            .setId(4)
            .setTeam(team2.getId())
            .setProblem(problemC.getId())
            .setLanguage("Parseltongue")
            .setTime(2.0)
            .build());
    assertThat(model.getSubmissions()).hasSize(1);
    verify(observer).onProblemAttempted(
        eq(team2),
        eq(ScoreboardProblem.newBuilder()
            .setLabel(problemC.getLabel())
            .setNumJudged(0)
            .setNumPending(1)
            .setSolved(false)
            .build()));

    // Judge the problem
    dispatcher.notifyJudging(
        Judging.newBuilder()
            .setSubmissionId(4)
            .setOutcome("correct")
            .setTime(3.21)
            .build());
    verify(observer).onProblemAttempted(
        eq(team2),
        eq(ScoreboardProblem.newBuilder()
            .setLabel(problemC.getLabel())
            .setNumJudged(1)
            .setNumPending(0)
            .setSolved(true)
            .setTime(2)
            .build()));
    assertThat(model.getSubmissions()).hasSize(1);
  }
}
