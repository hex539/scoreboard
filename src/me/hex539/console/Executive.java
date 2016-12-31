package me.hex539.console;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CommandLineInterface;
import com.lexicalscope.jewel.cli.InvalidOptionSpecificationException;
import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.hex539.proto.domjudge.DomjudgeProto;
import me.hex539.scoreboard.DomjudgeRest;

import static com.lexicalscope.jewel.cli.CliFactory.createCli;

@CommandLineInterface interface Invocation {
  @Option(
    shortName = "h",
    longName = "help",
    description = "Display this help and exit")
      boolean isHelp();
  @Option(
    shortName = "u",
    longName = "url",
    description = "Scoreboard URL")
      String getUrl();
  @Unparsed(
    name = "ACTION")
      List<String> getActions();
}

public class Executive {
  public static void main(String[] args) throws Exception {
    final Invocation invocation = parseCommandLine(args);
    System.err.println("Fetching from: " + invocation.getUrl());

    DomjudgeRest api = new DomjudgeRest(invocation.getUrl());
    DomjudgeProto.Team[] teams = api.getTeams();
    DomjudgeProto.ScoreboardRow[] scoreboard = api.getScoreboard(/* contestId */ 5);

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

  private static Invocation parseCommandLine(String[] args) {
    try {
      final Cli<Invocation> cli = createCli(Invocation.class);
      try {
        return cli.parseArguments(args);
      } catch (ArgumentValidationException e) {
        System.err.println(e.getMessage());
        System.err.println(cli.getHelpMessage());
        System.exit(1);
        return null;
      }
    } catch (InvalidOptionSpecificationException e) {
      throw new Error(e);
    }
  }
}
