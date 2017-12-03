package me.hex539.console;

import java.util.function.Function;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

public class Executive {
  public static void main(String[] args) throws Exception {
    Invocation invocation = Invocation.parseFrom(args);
    Map<String, Method> actionMap = Command.Annotations.all(Executive.class);

    List<String> actions = invocation.getActions();
    if (actions == null) {
      actions = Arrays.asList(new String[]{"scoreboard"});
    }

    for (String action : actions) {
      Method method = actionMap.get(action);
      if (method == null) {
        System.err.println("Unknown action: " + action);
        System.exit(1);
        return;
      }
      method.invoke(null, invocation);
    }
  }

  private static DomjudgeRest getRestApi(Invocation invocation) {
    System.err.println("Fetching from: " + invocation.getUrl());
    DomjudgeRest api = new DomjudgeRest(invocation.getUrl());

    if (invocation.getUsername() != null || invocation.getPassword() != null) {
      final String username = invocation.getUsername();
      final String password = invocation.getPassword();
      if (username == null || password == null) {
        System.err.println("Need to provide both or neither of username:password");
        System.exit(1);
        return null;
      }
      api.setCredentials(username, password);
    }

    return api;
  }

  @Command(name = "scoreboard")
  private static void showScoreboard(Invocation invocation) throws Exception {
    DomjudgeRest api = getRestApi(invocation);
    DomjudgeProto.Contest contest = api.getContest();
    DomjudgeProto.ScoreboardRow[] scoreboard = api.getScoreboard(contest);
    Map<Long, DomjudgeProto.Team> teamMap =
        groupBy(api.getTeams(), DomjudgeProto.Team::getId);

    for (DomjudgeProto.ScoreboardRow row : scoreboard) {
      DomjudgeProto.Team team = teamMap.get(row.getTeam());
      System.out.format("%-30s\t| %2d | %5d",
          team.getName(),
          row.getScore().getNumSolved(),
          row.getScore().getTotalTime());
      for (DomjudgeProto.ScoreboardProblem problem : row.getProblemsList()) {
        System.out.format(" | %c", (problem.getSolved() ? '+' : ' '));
      }
      System.out.println();
    }
  }

  @Command(name = "verdicts")
  private static void showJudgements(Invocation invocation) throws Exception {
    DomjudgeRest api = getRestApi(invocation);
    DomjudgeProto.Contest contest = api.getContest();
    DomjudgeProto.Judging[] judgings = api.getJudgings(contest);

    Map<Long, DomjudgeProto.Team> teamMap =
        groupBy(api.getTeams(), DomjudgeProto.Team::getId);
    Map<Long, DomjudgeProto.Submission> submissionMap =
        groupBy(api.getSubmissions(contest), DomjudgeProto.Submission::getId);
    Map<Long, DomjudgeProto.Problem> problemMap =
        groupBy(api.getProblems(contest), DomjudgeProto.Problem::getId);

    for (DomjudgeProto.Judging judging : judgings) {
      final DomjudgeProto.Submission submission = submissionMap.get(judging.getSubmission());
      final DomjudgeProto.Team team = teamMap.get(submission.getTeam());
      final DomjudgeProto.Problem problem = problemMap.get(submission.getProblem());

      System.out.format("%-30s | %-20s | %s%n",
          team.getName(),
          problem.getName(),
          judging.getOutcome());
    }
  }

  private static <K, V> Map<K, V> groupBy(V[] items, Function<V, K> mapper) {
    return Arrays.stream(items).collect(Collectors.toMap(mapper, Function.identity()));
  }
}
