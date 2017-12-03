package me.hex539.console;

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
      shortName = "u",
      longName = "url",
      description = "Scoreboard URL")
    String getUrl();

  @Option(
      defaultToNull = true,
      shortName = "l",
      longName = "username",
      description = "API access username")
  String getUsername();

  @Option(
      defaultToNull = true,
      shortName = "p",
      longName = "password",
      description = "API access password")
  String getPassword();

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
