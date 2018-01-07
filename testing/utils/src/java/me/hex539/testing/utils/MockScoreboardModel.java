package me.hex539.testing.utils;

import static org.domjudge.proto.DomjudgeProto.Contest;
import static org.domjudge.proto.DomjudgeProto.Problem;
import static org.domjudge.proto.DomjudgeProto.ScoreboardRow;
import static org.domjudge.proto.DomjudgeProto.ScoreboardScore;
import static org.domjudge.proto.DomjudgeProto.ScoreboardProblem;
import static org.domjudge.proto.DomjudgeProto.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.domjudge.scoreboard.ScoreboardModel;

public final class MockScoreboardModel {
  public static ScoreboardModel example() {
    return new Builder()
        .setProblems("Apricot", "Bamboo", "Coconut", "Durian")
        .addRow("Bath Ducks 🦆",  "+", "+",  "+",  "+")
        .addRow("Bath Crocs 🐊", "+", " ",  "+4", "?")
        .addRow("Bath Shower ☂", " ", "-1", "+2", "?1")
        .build();
  }

  public static class Builder {
    private final List<Problem> problems = new ArrayList<>();
    private final List<ScoreboardRow> rows = new ArrayList<>();
    private final List<Team> teams = new ArrayList<>();

    public ScoreboardModel build() {
      return new ScoreboardModel() {
        @Override
        public Contest getContest() {
          return Contest.newBuilder().setId(77).build();
        }

        @Override
        public Collection<Team> getTeams() {
          return teams;
        }

        @Override
        public List<Problem> getProblems() {
          return problems;
        }

        @Override
        public List<ScoreboardRow> getRows() {
          return rows;
        }
      };
    }

    public Builder setProblems(final String... names) {
      problems.addAll(IntStream.range(0, names.length)
          .mapToObj(i -> Problem.newBuilder()
              .setId(1 + (0x1234 ^ i))
              .setName(names[i])
              .setLabel(names[i])
              .setShortName(names[i].substring(0, 1))
              .build())
          .collect(Collectors.toList()));
      return this;
    }

    public Builder addRow() {
      return addRow("Test");
    }

    public Builder addRow(final String teamName) {
      final Random rand = new Random();
      final String[] attempts = IntStream.range(0, problems.size())
          .mapToObj(i ->
              String.valueOf(" +-?".charAt(rand.nextInt(4)))
              + String.valueOf("0123".charAt(rand.nextInt(4))))
          .map(s -> (s.endsWith("0") ? s.startsWith("+")
              ? s.substring(0, 1)
              : s.substring(0, 1) + "1" : s))
          .map(s -> (s.startsWith(" ") ? " " : s))
          .map(s -> (s.equals("-") ? "-1" : s))
          .toArray(length -> new String[length]);
      return addRow(teamName, attempts);
    }

    public Builder addRow(final String teamName, final String... attempts) {
      final Team team = Team.newBuilder()
          .setId(1 + (0xAAAA ^ teams.size()))
          .setName(teamName)
          .build();
      teams.add(team);

      final List<ScoreboardProblem> cols = IntStream.range(0, attempts.length)
          .mapToObj(i -> ScoreboardProblem.newBuilder()
              .setLabel(problems.get(i).getLabel())
              .setSolved(attempts[i].startsWith("+"))
              .setTime(attempts[i].startsWith("+") ? 321 : 0)
              .setNumJudged(
                  (attempts[i].startsWith("+") || attempts[i].startsWith("?") ? 1 : 0)
                  + (attempts[i].length() > 1 ? Integer.parseInt(attempts[i].substring(1)) : 0))
              .setNumPending(attempts[i].startsWith("?") ? 1 : 0)
              .build())
          .collect(Collectors.toList());

      rows.add(ScoreboardRow.newBuilder()
          .setTeam(team.getId())
          .setRank(rows.size() + 1)
          .setScore(ScoreboardScore.newBuilder()
              .setNumSolved(cols.stream().filter(ScoreboardProblem::getSolved).count())
              .setTotalTime(cols.stream().mapToLong(c -> c.getSolved() ? c.getTime() : 0).sum())
              .build())
          .addAllProblems(cols)
          .build());

      return this;
    }
  }
}
