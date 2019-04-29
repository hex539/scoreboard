package me.hex539.resolver;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.CommandLineInterface;
import com.lexicalscope.jewel.cli.InvalidOptionSpecificationException;
import com.lexicalscope.jewel.cli.Option;

@CommandLineInterface
public interface Invocation {
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
      shortName = "g",
      longName = "groups",
      defaultToNull = true,
      description = "Restrict to given comma-separated groups if showing a scoreboard")
    String getGroups();

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
