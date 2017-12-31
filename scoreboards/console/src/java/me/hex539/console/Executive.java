package me.hex539.console;

import com.google.protobuf.TextFormat;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  private static DomjudgeProto.EntireContest getEntireContest(Invocation invocation) {
    if (invocation.getUrl() != null) {
      try {
        return getRestApi(invocation).getEntireContest();
      } catch (Exception e) {
        System.err.println("Failed to fetch contest: " + e.getMessage());
        System.exit(1);
      }
    }
    if (invocation.getFile() != null) {
      try (Reader is = new InputStreamReader(new FileInputStream(invocation.getFile()))) {
        DomjudgeProto.EntireContest.Builder ecb = DomjudgeProto.EntireContest.newBuilder();
        TextFormat.merge(is, ecb);
        return ecb.build();
      } catch (IOException e) {
        System.err.println("Failed to read file: " + e.getMessage());
        System.exit(1);
      }
    }
    System.err.println("Need to specify either --url or --file");
    System.exit(1);
    return null;
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
    DomjudgeProto.EntireContest entireContest = getEntireContest(invocation);
    DomjudgeProto.Contest contest = entireContest.getContest();
    DomjudgeProto.ScoreboardRow[] scoreboard = entireContest.getScoreboardList()
        .toArray(new DomjudgeProto.ScoreboardRow[0]);
    DomjudgeProto.Team[] teams = entireContest.getTeamsList().toArray(new DomjudgeProto.Team[0]);

    Map<Long, DomjudgeProto.Team> teamMap = collectBy(teams, DomjudgeProto.Team::getId);

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
    DomjudgeProto.EntireContest entireContest = getEntireContest(invocation);

    ScoreboardModelImpl model = ScoreboardModelImpl.create(entireContest).withoutSubmissions();
    JudgingDispatcher dispatcher = new JudgingDispatcher(model);
    dispatcher.observers.add(model);

    for (DomjudgeProto.Submission submission : entireContest.getSubmissionsList()) {
      dispatcher.notifySubmission(submission);
    }

    for (DomjudgeProto.Judging judging : entireContest.getJudgingsList()) {
      dispatcher.notifyJudging(judging);

      final DomjudgeProto.Submission submission = model.getSubmission(judging.getSubmission());
      final DomjudgeProto.Team team = model.getTeam(submission.getTeam());

      System.out.format("%-30s \t| %-20s | %-16s | %4d | %5d | rank=%3d | %s%n",
          team.getName(),
          model.getProblem(submission.getProblem()).getName(),
          judging.getOutcome(),
          model.getRow(team).getScore().getNumSolved(),
          model.getRow(team).getScore().getTotalTime(),
          model.getRow(team).getRank(),
          formatScoreboardRow(model.getRow(team).getProblemsList()));
    }
  }

  private static String formatScoreboardRow(List<DomjudgeProto.ScoreboardProblem> row) {
    StringBuilder sb = new StringBuilder();
    for (DomjudgeProto.ScoreboardProblem sp : row) {
      sb.append(sp.getSolved() ? "+" : "-");
    }
    return sb.toString();
  }

  @Command(name = "download")
  private static void downloadContest(Invocation invocation) throws Exception {
    DomjudgeRest api = getRestApi(invocation);
    TextFormat.print(api.getEntireContest(api.getContest()), System.out);
  }

  private static <K, V> Map<K, V> collectBy(V[] items, Function<V, K> mapper) {
    return Arrays.stream(items).collect(Collectors.toMap(mapper, Function.identity()));
  }
}
