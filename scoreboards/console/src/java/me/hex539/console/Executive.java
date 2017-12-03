package me.hex539.console;

import java.lang.reflect.Method;
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
    System.err.println("Fetching from: " + invocation.getUrl());

    DomjudgeRest api = getRestApi(invocation);
    DomjudgeProto.Contest contest = api.getContest();
    DomjudgeProto.Team[] teams = api.getTeams();
    DomjudgeProto.ScoreboardRow[] scoreboard = api.getScoreboard(contest);

    Map<Long, DomjudgeProto.Team> teamMap = new HashMap<>();
    for (DomjudgeProto.Team team : teams) {
      teamMap.put(team.getId(), team);
    }

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
}
