package me.hex539.console;

import com.google.protobuf.TextFormat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.domjudge.scoreboard.JudgingDispatcher;
import org.domjudge.scoreboard.ResolverController;
import org.domjudge.scoreboard.ScoreboardModelImpl;
import org.domjudge.proto.DomjudgeProto.*;

public class Executive {
  private final ContestFetcher contestFetcher;
  private static final int MAX_TEAM_NAME_LENGTH = 24;

  public Executive(ContestFetcher contestFetcher) {
    this.contestFetcher = contestFetcher;
  }

  public static void main(String[] args) throws Exception {
    Invocation invocation = Invocation.parseFrom(args);
    final ContestFetcher contestFetcher = new ContestFetcher(invocation);

    List<String> actions = invocation.getActions();
    if (actions == null) {
      actions = Arrays.asList(new String[]{"scoreboard"});
    }

    Map<String, Method> actionMap = Command.Annotations.all(Executive.class);
    for (String action : actions) {
      Method method = actionMap.get(action);
      if (method == null) {
        System.err.println("Unknown action: " + action);
        System.exit(1);
        return;
      }
      try {
        method.invoke(new Executive(contestFetcher), invocation);
      } catch (Exception e) {
        System.err.println("Failed to run command '" + action + "': " + e.getCause().getMessage());
        e.getCause().printStackTrace();
        System.exit(1);
        return;
      }
    }
  }

  @Command(name = "scoreboard")
  private void showScoreboard(Invocation invocation) throws Exception {
    EntireContest entireContest = contestFetcher.get();
    List<ScoreboardRow> scoreboard = entireContest.getScoreboardList();
    Team[] teams = entireContest.getTeamsList().toArray(new Team[0]);
    Map<Long, Team> teamMap = Arrays.stream(teams)
        .collect(Collectors.toMap(Team::getId, Function.identity()));

    System.out.println(PrettyPrinter.formatScoreboardHeader(entireContest.getProblemsList()));
    for (ScoreboardRow row : scoreboard) {
      System.out.println(PrettyPrinter.formatScoreboardRow(teamMap.get(row.getTeam()), row));
    }
  }

  @Command(name = "verdicts")
  private void showJudgements(Invocation invocation) throws Exception {
    EntireContest entireContest = contestFetcher.get();

    ScoreboardModelImpl model = ScoreboardModelImpl.create(entireContest).withoutSubmissions();
    JudgingDispatcher dispatcher = new JudgingDispatcher(model);
    dispatcher.observers.add(model);

    for (Submission submission : entireContest.getSubmissionsList()) {
      dispatcher.notifySubmission(submission);
    }

    for (Judging judging : entireContest.getJudgingsList()) {
      dispatcher.notifyJudging(judging);

      final Submission submission = model.getSubmission(judging.getSubmission());
      final Team team = model.getTeam(submission.getTeam());
      final ScoreboardRow row = model.getRow(team);

      System.out.format(
          "%-" + MAX_TEAM_NAME_LENGTH + "s \t| %-20s | %-16s | %4d | %6d | rank=%3d | %s%n",
          team.getName(),
          model.getProblem(submission.getProblem()).getName(),
          judging.getOutcome(),
          row.getScore().getNumSolved(),
          row.getScore().getTotalTime(),
          row.getRank(),
          PrettyPrinter.formatMiniScoreboardRow(model.getRow(team).getProblemsList()));
    }
  }

  @Command(name = "resolver")
  private void showResolver(Invocation invocation) throws Exception {
    EntireContest entireContest = contestFetcher.get();
    ScoreboardModelImpl model = ScoreboardModelImpl.create(entireContest).withoutSubmissions();

    Map<Long, Team> teamMap = model.getTeams().stream()
        .collect(Collectors.toMap(Team::getId, Function.identity()));

    ResolverController controller = new ResolverController(entireContest, model);

    final AtomicReference<Team> focusedTeam = new AtomicReference<>();
    final AtomicReference<Problem> focusedProblem = new AtomicReference<>();
    controller.observers.add(new ResolverController.Observer() {
      @Override
      public void onProblemFocused(Team team, Problem problem) {
        focusedTeam.set(team);
        focusedProblem.set(problem);
      }
    });

    while (!controller.finished()) {
      controller.advance();

      System.out.println(PrettyPrinter.formatScoreboardHeader(entireContest.getProblemsList()));
      model.getRows().stream()
          .map(row -> PrettyPrinter.formatScoreboardRow(
              teamMap.get(row.getTeam()),
              row,
              row.getTeam() == focusedTeam.get().getId() ? focusedProblem.get() : null))
          .forEach(System.out::println);

      Thread.sleep(200 /* milliseconds */);
    }
  }

  @Command(name = "download")
  private void downloadContest(Invocation invocation) throws Exception {
    TextFormat.print(contestFetcher.get(), System.out);
  }

  private static class PrettyPrinter {
    static String formatScoreboardHeader(List<Problem> problems) {
      StringBuilder sb = new StringBuilder(
          String.format("%-" + MAX_TEAM_NAME_LENGTH + "s\t│ ## │  time ", "team"));
      problems.forEach(p -> sb.append(" │ " + p.getLabel().charAt(0)));
      final String header = sb.toString();
      final String borderT = header.replaceAll("\\│", "┬").replaceAll("[^\\t┬]", "─") + "─┐";
      final String borderB = header.replaceAll("\\│", "┼").replaceAll("[^\\t┼]", "─") + "─┤";
      return borderT + "\n" + header + " │\n" + borderB;
    }

    static String formatScoreboardRow(Team team, ScoreboardRow row) {
      return formatScoreboardRow(team, row, null);
    }

    static String formatScoreboardRow(Team team, ScoreboardRow row, Problem highlight) {
      StringBuilder sb = new StringBuilder(
          String.format("%-" + MAX_TEAM_NAME_LENGTH + "s\t│ %2d │ %6d ",
              team.getName(),
              row.getScore().getNumSolved(),
              row.getScore().getTotalTime()));
      row.getProblemsList().forEach(p -> {
        boolean hl = (highlight != null && highlight.getLabel().equals(p.getLabel()));
        sb.append("│" + (hl ? '[' : ' ') + formatAttempt(p) + (hl ? ']' : ' '));
      });
      return sb.toString() + "│";
    }
    static String formatMiniScoreboardRow(List<ScoreboardProblem> row) {
      return row.stream().map(PrettyPrinter::formatAttempt).collect(Collectors.joining(""));
    }

    static String formatAttempt(ScoreboardProblem p) {
      return p.getSolved() ? "+" : p.getNumPending() > 0 ? "?" : p.getNumJudged() > 0 ? "-" : " ";
    }
  }
}
