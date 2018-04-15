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

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ContestDownloader;
import me.hex539.contest.JudgementDispatcher;
import me.hex539.contest.ResolverController;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;

class PrettyPrinter {
  private static final int MAX_TEAM_NAME_LENGTH = 24;

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
    return formatScoreboardRow(team, row, false, null);
  }

  static String formatScoreboardRow(
      Team team,
      ScoreboardRow row,
      boolean highlightRow,
      Problem highlightProblem) {
    StringBuilder sb = new StringBuilder(
        String.format("%-" + MAX_TEAM_NAME_LENGTH + "s\t│ %2d │ %6d ",
            team.getName(),
            Math.max(0, row.getScore().getNumSolved()),
            Math.max(0, row.getScore().getTotalTime())));
    for (ScoreboardProblem p : row.getProblemsList()) {
      boolean hl = (highlightProblem != null && highlightProblem.getId().equals(p.getProblemId()));
      sb.append("│" + (hl ? '[' : ' ') + formatAttempt(p) + (hl ? ']' : ' '));
    }
    String res = sb.toString() + "│";
    if (highlightRow) {
      String[] parts = res.split("\t", 2);
      parts[parts.length-1] = parts[parts.length-1].replaceAll(" ", "_");
      res = String.join("\t", parts);
    }
    return res;
  }

  static String formatVerdictRow(
      Team team, Problem problem, Submission submission, JudgementType jt, ScoreboardRow row) {
    return String.format(
        "%-" + MAX_TEAM_NAME_LENGTH + "s \t| %-20s | %-16s | %4d | %6d | rank=%3d | %s",
        team.getName(),
        problem.getName(),
        jt.getName(),
        row.getScore().getNumSolved(),
        row.getScore().getTotalTime(),
        row.getRank(),
        formatMiniScoreboardRow(row.getProblemsList()));
  }

  static String formatMiniScoreboardRow(List<ScoreboardProblem> row) {
    return row.stream().map(PrettyPrinter::formatAttempt).collect(Collectors.joining(""));
  }

  static String formatAttempt(ScoreboardProblem p) {
    return p.getSolved() ? "+" : p.getNumPending() > 0 ? "?" : p.getNumJudged() > 0 ? "-" : " ";
  }
}
