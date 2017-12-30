package org.domjudge.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;
import me.hex539.testing.utils.MockScoreboardModel;
import org.junit.Test;

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

    final Team team2 = findTeam(model, "Team 2");
    final Problem problemC = model.getProblems()
        .stream().filter(x -> "C".equals(x.getName())).findAny().get();

    // Submit problem C for team 2.
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
            .build()),
        any(ScoreboardScore.class));

    // Judge the problem.
    dispatcher.notifyJudging(
        Judging.newBuilder()
            .setSubmission(4)
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
            .build()),
        any(ScoreboardScore.class));
    assertThat(model.getSubmissions()).hasSize(1);
  }

  @Test
  public void increaseTeamRank() {
    ScoreboardModel.Observer observer = mock(ScoreboardModel.Observer.class);

    ScoreboardModelImpl model = ScoreboardModelImpl.create(
        new MockScoreboardModel.Builder()
            .setProblems(     "A", "B", "C")
            .addRow("Team 1", " ", "+", " ")
            .addRow("Team 2", " ", " ", " ")
            .addRow("Team 3", " ", " ", " ")
            .addRow("Team 4", " ", " ", " ")
            .addRow("Team 5", " ", " ", " ")
            .addRow("Team 6", "+", "+", "+")
            .build());
    JudgingDispatcher dispatcher = new JudgingDispatcher(model);

    dispatcher.observers.add(model);
    dispatcher.observers.add(observer);

    final Team team5 = findTeam(model, "Team 5");
    dispatcher.notifyRankChanged(team5, 5, 2);
    verify(observer).onTeamRankChanged(eq(team5), eq(5), eq(2));

    assertThat(getScoreboard(model))
        .containsExactly("Team 1", "Team 5", "Team 2", "Team 3", "Team 4", "Team 6")
        .inOrder();
  }

  @Test
  public void decreaseTeamRank() {
    ScoreboardModel.Observer observer = mock(ScoreboardModel.Observer.class);

    ScoreboardModelImpl model = ScoreboardModelImpl.create(
        new MockScoreboardModel.Builder()
            .setProblems(     "A", "B", "C")
            .addRow("Team 1", "+", "+", " ")
            .addRow("Team 2", "+", " ", " ")
            .addRow("Team 3", " ", " ", " ")
            .addRow("Team 4", " ", " ", " ")
            .addRow("Team 5", " ", " ", " ")
            .addRow("Team 6", "+", "+", "+")
            .build());
    JudgingDispatcher dispatcher = new JudgingDispatcher(model);

    dispatcher.observers.add(model);
    dispatcher.observers.add(observer);

    final Team team2 = findTeam(model, "Team 2");
    dispatcher.notifyRankChanged(team2, 2, 6);
    verify(observer).onTeamRankChanged(eq(team2), eq(2), eq(6));

    assertThat(getScoreboard(model))
        .containsExactly("Team 1", "Team 3", "Team 4", "Team 5", "Team 6", "Team 2")
        .inOrder();
  }
  private static Team findTeam(ScoreboardModel model, String teamName) {
    return model.getTeams()
        .stream().filter(x -> teamName.equals(x.getName())).findAny().get();
  }

  private static List<String> getScoreboard(ScoreboardModel model) {
    final List<ScoreboardRow> rows = new ArrayList<>(model.getRows());
    Collections.sort(rows, (a, b) -> Long.compare(a.getRank(), b.getRank()));
    return rows
        .stream()
        .map(ScoreboardRow::getTeam)
        .map(model::getTeam)
        .map(Team::getName)
        .collect(Collectors.toList());
  }
}
