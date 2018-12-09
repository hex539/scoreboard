package org.domjudge.api;

import edu.clics.proto.ClicsProto.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import me.hex539.contest.JudgementDispatcher;
import me.hex539.testing.utils.MockScoreboardModel;
import org.junit.Test;

import static com.google.common.truth.Truth.*;
import static org.domjudge.api.SubmitInfo.submission;
import static org.mockito.Mockito.*;

public class ModelObserverTest {
  @Test
  public void observeRankChange() {
    ScoreboardModel.Observer observer = mock(ScoreboardModel.Observer.class);

    ScoreboardModelImpl model = ScoreboardModelImpl.newBuilder(
        ClicsContest.newBuilder()
          .putJudgementTypes("correct", JudgementType.newBuilder()
              .setName("Correct")
              .setSolved(true)
              .build())
          .build(),
        new MockScoreboardModel.Builder()
            .setProblems(     "A", "B", "C")
            .addRow("Team 1", "+", " ", "+")
            .addRow("Team 2", " ", "+", " ")
            .addRow("Team 3", " ", " ", " ")
            .build()).build();
    JudgementDispatcher dispatcher = new JudgementDispatcher(model);

    dispatcher.observers.add(model);
    dispatcher.observers.add(observer);

    final Team team2 = model.getTeam("Team 2");

    // Initially we'll have the submissions+judgements from the mock.
    assertThat(model.getSubmissions()).hasSize(3);
    assertThat(model.getJudgements()).hasSize(3);

    // Submit problem C for team 2.
    SubmitInfo s = submission(dispatcher, model, "Team 2", "C", 2).submit();
    verify(observer).onProblemScoreChanged(
        eq(team2),
        eq(ScoreboardProblem.newBuilder()
            .setProblemId("C")
            .setNumJudged(0)
            .setNumPending(1)
            .setSolved(false)
            .build()));
    assertThat(model.getSubmissions()).hasSize(4);
    assertThat(model.getJudgements()).hasSize(3);

    // Judge the problem.
    s.judge("correct");
    verify(observer).onProblemScoreChanged(
        eq(team2),
        eq(ScoreboardProblem.newBuilder()
            .setProblemId("C")
            .setNumJudged(1)
            .setNumPending(0)
            .setSolved(true)
            .setTime(2)
            .build()));
    assertThat(model.getJudgements()).hasSize(4);
  }

  @Test
  public void increaseTeamRank() {
    ScoreboardModel.Observer observer = mock(ScoreboardModel.Observer.class);

    ScoreboardModelImpl model = ScoreboardModelImpl.newBuilder(
        ClicsContest.newBuilder()
          .putJudgementTypes("correct", JudgementType.newBuilder()
              .setName("Correct")
              .setSolved(true)
              .build())
          .build(),
        new MockScoreboardModel.Builder()
            .setProblems(     "A", "B", "C")
            .addRow("Team 1", " ", "+", " ")
            .addRow("Team 2", " ", " ", " ")
            .addRow("Team 3", " ", " ", " ")
            .addRow("Team 4", " ", " ", " ")
            .addRow("Team 5", " ", " ", " ")
            .addRow("Team 6", " ", " ", " ")
            .build()).build();
    JudgementDispatcher dispatcher = new JudgementDispatcher(model);

    dispatcher.observers.add(model);
    dispatcher.observers.add(observer);

    final Team team5 = model.getTeam("Team 5");
    submission(dispatcher, model, "Team 5", "A", 101).submit().judge("correct");
    verify(observer).onTeamRankChanged(eq(team5), eq(5), eq(2));
    assertThat(getRankList(model))
        .containsExactly("Team 1", "Team 5", "Team 2", "Team 3", "Team 4", "Team 6")
        .inOrder();

    submission(dispatcher, model, "Team 5", "C", 100).submit().judge("correct");
    verify(observer).onTeamRankChanged(eq(team5), eq(2), eq(1));
    assertThat(getRankList(model))
        .containsExactly("Team 5", "Team 1", "Team 2", "Team 3", "Team 4", "Team 6")
        .inOrder();
  }

  @Test
  public void decreaseTeamRank() {
    ScoreboardModel.Observer observer = mock(ScoreboardModel.Observer.class);

    ScoreboardModelImpl model = ScoreboardModelImpl.newBuilder(
        ClicsContest.newBuilder()
          .putJudgementTypes("correct", JudgementType.newBuilder()
              .setName("Correct")
              .setSolved(true)
              .build())
          .putJudgementTypes("incorrect", JudgementType.newBuilder()
              .setName("Incorrect")
              .setSolved(false)
              .build())
          .build(),
        new MockScoreboardModel.Builder()
            .setProblems(     "A", "B", "C")
            .addRow("Team 1", "+", "+", "+")
            .addRow("Team 2", "+", "+", " ")
            .addRow("Team 3", " ", "+", "+")
            .addRow("Team 4", "+", " ", "+")
            .addRow("Team 5", " ", " ", "+")
            .addRow("Team 6", " ", " ", " ")
            .build()).build();
    JudgementDispatcher dispatcher = new JudgementDispatcher(model);

    dispatcher.observers.add(model);
    dispatcher.observers.add(observer);
    final Team team2 = model.getTeam("Team 2");

    // Rejudge from 2 to 1 correct answers.
    submission(dispatcher, model, "ms4").judge("incorrect");
    verify(observer).onTeamRankChanged(eq(team2), eq(2), eq(4));

    // Rejudge down to 0 correct answers.
    submission(dispatcher, model, "ms5").judge("incorrect");
    verify(observer).onTeamRankChanged(eq(team2), eq(4), eq(5));

    assertThat(getRankList(model))
        .containsExactly("Team 1", "Team 3", "Team 4", "Team 5", "Team 2", "Team 6")
        .inOrder();
  }

  private static List<String> getRankList(ScoreboardModel model) {
    final List<ScoreboardRow> rows = new ArrayList<>(model.getRows());
    Collections.sort(rows, (a, b) -> Long.compare(a.getRank(), b.getRank()));
    return rows
        .stream()
        .map(ScoreboardRow::getTeamId)
        .map(model::getTeam)
        .map(Team::getName)
        .collect(Collectors.toList());
  }
}
