package me.hex539.analysis;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.CommandLineInterface;
import com.lexicalscope.jewel.cli.InvalidOptionSpecificationException;
import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

public @CommandLineInterface interface Invocation {
  @Option(
      shortName = "h",
      longName = "help",
      helpRequest = true,
      description = "Display this help and exit")
    boolean isHelp();

  @Option(
      shortName = "c",
      longName = "contest",
      defaultToNull = true,
      description = "Contest ID to resolve")
    String getContest();

  @Option(
      shortName = "u",
      longName = "url",
      defaultToNull = true,
      description = "Scoreboard URL")
    String getUrl();

  @Option(
      shortName = "f",
      longName = "file",
      defaultToNull = true,
      description = "Path to saved scoreboard")
    String getFile();

  @Option(
      shortName = "a",
      longName = "api",
      defaultToNull = true,
      description = "Filter API version to use (domjudge3 or clics, default is both)")
    String getApi();

  @Option(
      shortName = "g",
      longName = "groups",
      defaultToNull = true,
      description = "Restrict to given comma-separated groups if showing a scoreboard")
    String getGroups();

  @Option(
      shortName = "b",
      longName = "problems",
      defaultToNull = true,
      description = "Restrict to given comma-separated problems")
    String getProblems();

  @Option(
      shortName = "z",
      longName = "prefreeze",
      description = "Show only data from before the freeze, even when the full data is available")
    boolean getApplyFreeze();

  @Option(
      shortName = "t",
      longName = "solvestats",
      description = "Print solvestats instead of file information")
    boolean getPrintSolvestats();

  @Option(
      shortName = "l",
      longName = "username",
      defaultToNull = true,
      description = "API access username")
    String getUsername();

  @Option(
      shortName = "p",
      longName = "password",
      defaultToNull = true,
      description = "API access password")
    String getPassword();

  @Option(
      shortName = "t",
      longName = "textformat",
      description = "Read and write saved contests in text format instead of binary")
    boolean isTextFormat();

  @Unparsed(
      name = "ACTION")
    List<String> getActions();

  public static Invocation parseFrom(String[] args) {
    try {
      final Cli<Invocation> cli = CliFactory.createCli(Invocation.class);
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
