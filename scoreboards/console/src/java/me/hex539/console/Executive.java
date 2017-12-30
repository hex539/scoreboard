package me.hex539.console;

import com.google.protobuf.TextFormat;

import java.util.function.Function;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.api.JudgingDispatcher;
import org.domjudge.api.ScoreboardModel;
import org.domjudge.api.ScoreboardModelImpl;
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
    ScoreboardModelImpl model = ScoreboardModelImpl.create(api).withoutSubmissions();
    JudgingDispatcher dispatcher = new JudgingDispatcher(model);
    dispatcher.observers.add(model);

    for (DomjudgeProto.Submission submission : api.getSubmissions(model.getContest())) {
      dispatcher.notifySubmission(submission);
    }

    for (DomjudgeProto.Judging judging : api.getJudgings(model.getContest())) {
      dispatcher.notifyJudging(judging);

      final DomjudgeProto.Submission submission = model.getSubmission(judging.getSubmission());

      StringBuilder sb = new StringBuilder();
      for (DomjudgeProto.ScoreboardProblem sp :
          model.getRow(model.getTeam(submission.getTeam())).getProblemsList()) {
        if (sp.getSolved()) {
          sb.append("+");
        } else {
          sb.append("-");
        }
      }

      System.out.format("%-30s \t| %-20s | %-16s | %4d | %5d | rank=%3d | %s%n",
          model.getTeam(submission.getTeam()).getName(),
          model.getProblem(submission.getProblem()).getName(),
          judging.getOutcome(),
          model.getRow(model.getTeam(submission.getTeam())).getScore().getNumSolved(),
          model.getRow(model.getTeam(submission.getTeam())).getScore().getTotalTime(),
          model.getRow(model.getTeam(submission.getTeam())).getRank(),
          sb.toString());
    }
  }

  @Command(name = "download")
  private static void downloadContest(Invocation invocation) throws Exception {
    DomjudgeRest api = getRestApi(invocation);
    DomjudgeProto.EntireContest entireContest = api.getEntireContest(api.getContest());

    TextFormat.print(entireContest, System.out);
  }

  private static <K, V> Map<K, V> groupBy(V[] items, Function<V, K> mapper) {
    return Arrays.stream(items).collect(Collectors.toMap(mapper, Function.identity()));
  }
}
