package me.hex539.console;

import com.google.protobuf.TextFormat;

import com.google.protobuf.util.Timestamps;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ApiDetective;
import me.hex539.contest.ContestConfig;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.JudgementDispatcher;
import me.hex539.contest.ResolverController;
import me.hex539.contest.ScoreboardModelImpl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Executive {
  private final ContestDownloader contestFetcher;
  private static final int MAX_TEAM_NAME_LENGTH = 24;

  public Executive(ContestDownloader contestFetcher) {
    this.contestFetcher = contestFetcher;
  }

  public static void main(String[] args) throws Exception {
    Invocation invocation = Invocation.parseFrom(args);

    final ContestConfig.Source source;
    if (invocation.getUrl() != null) {
      ContestConfig.Source.Builder sourceBuilder =
          ApiDetective.detectApi(invocation.getUrl())
              .orElseThrow(() -> new Error("No contests found"))
              .toBuilder();
      if (invocation.getUsername() != null) {
          sourceBuilder.setAuthentication(
              ContestConfig.Authentication.newBuilder()
                  .setHttpUsername(invocation.getUsername())
                  .setHttpPassword(invocation.getPassword())
                  .build());
      }
      source = sourceBuilder.build();
    } else if (invocation.getFile() != null) {
      source = ContestConfig.Source.newBuilder()
          .setFilePath(invocation.getFile())
          .build();
    } else {
      System.err.println("Need one of --url or --path to load a contest");
      System.exit(1);
      return;
    }

    final ContestDownloader contestFetcher = new ContestDownloader(source);

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
    ClicsContest entireContest = contestFetcher.fetch();
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
    ClicsContest entireContest = contestFetcher.fetch();

    ScoreboardModelImpl fullModel = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> invocation.getGroups() != null
            ? invocation.getGroups().equals(g.getName())
            : !g.getHidden())
        .filterTooLateSubmissions()
        .build();
    ScoreboardModelImpl model = fullModel.toBuilder()
        .withEmptyScoreboard()
        .filterSubmissions(x -> false)
        .build();
    JudgementDispatcher dispatcher = new JudgementDispatcher(model);
    dispatcher.observers.add(model);

    for (Judgement judgement : fullModel.getJudgeModel().getJudgements()) {
      try {
        model.getSubmission(judgement.getSubmissionId());
      } catch (Exception e) {
        if (!dispatcher.notifySubmission(fullModel.getSubmission(judgement.getSubmissionId()))) {
          continue;
        }
      }
      if (dispatcher.notifyJudgement(judgement) == null) {
        continue;
      }

      final JudgementType judgementType =
          model.getJudgeModel().getJudgementType(judgement.getJudgementTypeId());
      final Submission submission = model.getSubmission(judgement.getSubmissionId());
      final Problem problem = model.getProblem(submission.getProblemId());
      final Team team = model.getTeamsModel().getTeam(submission.getTeamId());
      final ScoreboardRow row = model.getRow(team);

      System.out.println(
          PrettyPrinter.formatVerdictRow(team, problem, submission, judgementType, row));
    }
  }

  @Command(name = "events")
  private void showEvents(Invocation invocation) throws Exception {
    Optional<BlockingQueue<Optional<EventFeedItem>>> feed =
        contestFetcher.eventFeed(chooseContest(contestFetcher.contests()));
    if (!feed.isPresent()) {
      System.err.println("Event feed is not available.");
      return;
    }
    for (Optional<EventFeedItem> item; (item = feed.get().take()).isPresent();) {
      System.out.format("%-30s %s %s\n",
          Timestamps.toString(item.get().getTime()),
          item.get().getOperation().toString(),
          item.get().getType().toString());
    }
  }

  @Command(name = "live")
  private void showLiveScoreboard(Invocation invocation) throws Exception {
    Optional<BlockingQueue<Optional<EventFeedItem>>> feed =
        contestFetcher.eventFeed(chooseContest(contestFetcher.contests()));
    if (!feed.isPresent()) {
      System.err.println("Event feed is not available.");
      return;
    }

    ClicsContest entireContest = contestFetcher.fetch();
    ScoreboardModelImpl fullModel = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> invocation.getGroups() != null
            ? invocation.getGroups().equals(g.getName())
            : !g.getHidden())
        .filterTooLateSubmissions()
        .build();
    ScoreboardModelImpl model = fullModel.toBuilder()
        .withEmptyScoreboard()
        .filterSubmissions(x -> false)
        .build();
    JudgementDispatcher dispatcher = new JudgementDispatcher(model);
    dispatcher.observers.add(model);

    for (Optional<EventFeedItem> otem; (otem = feed.get().take()).isPresent();) {
      final EventFeedItem item = otem.get();

      if (item.hasContestData()) {
      } else if (item.hasJudgementTypeData()) {
      } else if (item.hasLanguageData()) {
      } else if (item.hasProblemData()) {
      } else if (item.hasGroupData()) {
        switch (item.getOperation()) {
          case create:
          case update:
            model.getTeamsModel().onGroupAdded(item.getGroupData());
            break;
          case delete:
            model.getTeamsModel().onGroupRemoved(item.getGroupData());
            break;
        }
      } else if (item.hasOrganizationData()) {
      } else if (item.hasTeamData()) {
        switch (item.getOperation()) {
          case create:
          case update:
            if (model.getTeamsModel().containsTeam(item.getTeamData())) {
              model.getTeamsModel().onTeamAdded(item.getTeamData());
              model.getRanklistModel().onTeamAdded(item.getTeamData());
            }
            break;
          case delete:
            model.getRanklistModel().onTeamRemoved(item.getTeamData());
            model.getTeamsModel().onTeamRemoved(item.getTeamData());
            break;
        }
      } else if (item.hasStateData()) {
      } else if (item.hasSubmissionData()) {
        final Submission submission = item.getSubmissionData();
        if (model.getTeamsModel().containsTeam(submission.getTeamId())) {
          dispatcher.notifySubmission(item.getSubmissionData());
        }
      } else if (item.hasJudgementData()) {
        dispatcher.notifyJudgement(item.getJudgementData());
      } else if (item.hasRunData()) {
      } else if (item.hasClarificationData()) {
      } else if (item.hasAwardData()) {
      }

      if (feed.get().isEmpty()) {
        System.out.println(PrettyPrinter.formatScoreboardHeader(model.getProblems()));
        model.getRows().stream()
            .limit(20)
            .map(row -> PrettyPrinter.formatScoreboardRow(
                model.getTeamsModel().getTeam(row.getTeamId()),
                row,
                false,
                null))
            .forEach(System.out::println);

        Thread.sleep(200); // milliseconds
      }
    }
  }

  @Command(name = "resolver")
  private void showResolver(Invocation invocation) throws Exception {
    ClicsContest entireContest = contestFetcher.fetch();
    ScoreboardModelImpl reference = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> invocation.getGroups() != null
            ? invocation.getGroups().equals(g.getName())
            : !g.getHidden())
        .filterTooLateSubmissions()
        .build();
    ScoreboardModelImpl model = ScoreboardModelImpl.newBuilder(entireContest, reference)
        .withEmptyScoreboard()
        .filterSubmissions(s -> false)
        .build();

    ResolverController controller = new ResolverController(entireContest, reference);
    controller.addObserver(model);

    final AtomicReference<Team> focusedTeam = new AtomicReference<>();
    final AtomicReference<Problem> focusedProblem = new AtomicReference<>();
    controller.addObserver(new ResolverController.Observer() {
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
              model.getTeamsModel().getTeam(row.getTeamId()),
              row,
              focusedTeam.get() != null && row.getTeamId() == focusedTeam.get().getId(),
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
      TextFormat.print(contestFetcher.fetch(), System.out);
    } else {
      contestFetcher.fetch().writeTo(System.out);
    }
  }

  private Contest chooseContest(List<Contest> contests) {
    if (contests.size() == 0) {
      System.err.println("No contests available");
      System.exit(1);
    }
    if (contests.size() > 1) {
      System.err.println("Choose contest:");
      System.err.println(" ---");
      for (int i = 0; i < contests.size(); i++) {
        System.err.printf(" %2d) %s\n", i + 1, contests.get(i).getName());
      }
      System.err.println(" ---");
      while (true) {
        System.err.print("  > ");
        try {
          final int id = new java.util.Scanner(System.in).nextInt();
          if (0 < id && id <= contests.size()) {
            return contests.get(id - 1);
          }
        } catch (java.util.InputMismatchException e) {
          continue;
        }
      }
    }
    return contests.get(0);
  }
}
