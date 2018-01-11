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

import edu.clics.api.ClicsRest;
import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.JudgementDispatcher;
import me.hex539.contest.ResolverController;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;

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
    ClicsContest entireContest = contestFetcher.get();
    List<ScoreboardRow> scoreboard = entireContest.getScoreboardList();

    System.out.println(PrettyPrinter.formatScoreboardHeader(
        entireContest.getProblemsMap().values().stream()
            .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
            .collect(Collectors.toList())));
    for (ScoreboardRow row : scoreboard) {
      System.out.println(PrettyPrinter.formatScoreboardRow(
            entireContest.getTeamsOrThrow(row.getTeamId()), row));
    }
  }

  @Command(name = "verdicts")
  private void showJudgements(Invocation invocation) throws Exception {
    ClicsContest entireContest = contestFetcher.get();

    ScoreboardModelImpl fullModel = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> invocation.getGroup() == null || invocation.getGroup().equals(g.getName()))
        .filterTooLateSubmissions()
        .build();
    ScoreboardModelImpl model = fullModel.toBuilder()
        .withEmptyScoreboard()
        .filterSubmissions(x -> false)
        .build();
    JudgementDispatcher dispatcher = new JudgementDispatcher(model);
    dispatcher.observers.add(model);

    for (Judgement judgement : fullModel.getJudgements()) {
      try {
        model.getSubmission(judgement.getSubmissionId());
      } catch (Exception e) {
        if (!dispatcher.notifySubmission(fullModel.getSubmission(judgement.getSubmissionId()))) {
          continue;
        }
      }
      if (!dispatcher.notifyJudgement(judgement)) {
        continue;
      }

      final Submission submission = model.getSubmission(judgement.getSubmissionId());
      final Team team = model.getTeam(submission.getTeamId());
      final ScoreboardRow row = model.getRow(team);

      System.out.format(
          "%-" + MAX_TEAM_NAME_LENGTH + "s \t| %-20s | %-16s | %4d | %6d | rank=%3d | %s%n",
          team.getName(),
          model.getProblem(submission.getProblemId()).getName(),
          entireContest.getJudgementTypesOrThrow(judgement.getJudgementTypeId()).getName(),
          row.getScore().getNumSolved(),
          row.getScore().getTotalTime(),
          row.getRank(),
          PrettyPrinter.formatMiniScoreboardRow(model.getRow(team).getProblemsList()));
    }
  }

  @Command(name = "resolver")
  private void showResolver(Invocation invocation) throws Exception {
    ClicsContest entireContest = contestFetcher.get();
    ScoreboardModelImpl reference = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> invocation.getGroup() == null || invocation.getGroup().equals(g.getName()))
        .filterTooLateSubmissions()
        .build();
    ResolverController controller = new ResolverController(entireContest, reference);

    ScoreboardModelImpl model = reference.toBuilder().withEmptyScoreboard().build();
    controller.observers.add(model);
    controller.start();

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

      System.out.println(PrettyPrinter.formatScoreboardHeader(model.getProblems()));
      model.getRows().stream()
          .map(row -> PrettyPrinter.formatScoreboardRow(
              model.getTeam(row.getTeamId()),
              row,
              focusedTeam.get() != null && row.getTeamId() == focusedTeam.get().getId() 
                  ? focusedProblem.get()
                  : null))
          .forEach(System.out::println);

      Thread.sleep(200); // milliseconds
    }
  }

  @Command(name = "download")
  private void downloadContest(Invocation invocation) throws Exception {
    if (invocation.isTextFormat()) {
      TextFormat.print(contestFetcher.get(), System.out);
    } else {
      contestFetcher.get().writeTo(System.out);
    }
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
              Math.max(0, row.getScore().getNumSolved()),
              Math.max(0, row.getScore().getTotalTime())));
      for (ScoreboardProblem p : row.getProblemsList()) {
        boolean hl = (highlight != null && highlight.getId().equals(p.getProblemId()));
        sb.append("│" + (hl ? '[' : ' ') + formatAttempt(p) + (hl ? ']' : ' '));
      }
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
