package me.hex539.console;

import java.util.List;
import java.util.stream.Collectors;

import edu.clics.proto.ClicsProto.*;

/**
 * Output in official ICPC results.csv format
 */
class CsvPrinter {
  static String formatScoreboardHeader() {
    return String.join(",",
        "teamId",
        "rank",
        "medalCitation",
        "problemsSolved",
        "totalTime",
        "lastProblemTime",
        "siteCitation",
        "citation",
        "name");
  }

  static String formatScoreboardRow(Team team, ScoreboardRow row) {
    return String.join(",",
        "" + team.getIcpcId(),
        "" + row.getRank(),
        /* medal */ "",
        "" + row.getScore().getNumSolved(),
        "" + (row.getScore().getTotalTime().getSeconds() / 60L),
        "" + row.getProblemsList().stream()
            .filter(p -> p.getSolved())
            .mapToLong(p -> p.getTime().getSeconds() / 60L)
            .max()
            .orElse(0L),
        "" + /* siteCitation */ "",
        "" + /* citation */ "",
        "\"" + team.getName().replace(',', '-') + "\"");
  }
}
