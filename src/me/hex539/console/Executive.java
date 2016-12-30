package me.hex539.console;

import com.google.gson.Gson;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CommandLineInterface;
import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import me.hex539.scoreboard.ScoreboardModel;
import me.hex539.scoreboard.TeamModel;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lexicalscope.jewel.cli.CliFactory.parseArguments;

@CommandLineInterface interface Invocation {
  @Option(
    shortName = "h",
    longName = "--help",
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
    Invocation invocation = parseArguments(Invocation.class, args);
    System.err.println("Fetching from: " + invocation.getUrl());

    OkHttpClient client = new OkHttpClient();
    Request request;

    final TeamModel[] teams;
    final ScoreboardModel.Row[] scoreboard;
    final Gson gson = new Gson();

    request = new Request.Builder()
        .url(invocation.getUrl() + "/teams")
        .build();
    try (ResponseBody body = client.newCall(request).execute().body()) {
      teams = gson.fromJson(body.string(), TeamModel[].class);
    }

    request = new Request.Builder()
        .url(invocation.getUrl() + "/scoreboard?cid=5")
        .build();
    try (ResponseBody body = client.newCall(request).execute().body()) {
      scoreboard = gson.fromJson(body.string(), ScoreboardModel.Row[].class);
    }

    Map<Long, TeamModel> teamMap = new HashMap<>();
    for (TeamModel team : teams) {
      teamMap.put(team.id, team);
    }

    for (ScoreboardModel.Row row : scoreboard) {
      TeamModel team = teamMap.get(row.team);
      System.out.format("%-30s\t| %2d | %5d%n",
          team.name,
          row.score.num_solved,
          row.score.total_time);
    }
  }
}
