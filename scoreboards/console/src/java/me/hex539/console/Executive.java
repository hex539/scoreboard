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

import me.hex539.contest.ApiDetective;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.JudgementDispatcher;
import me.hex539.contest.ResolverController;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;

public class Executive {
  private final ContestDownloader contestFetcher;
  private static final int MAX_TEAM_NAME_LENGTH = 24;

  public Executive(ContestDownloader contestFetcher) {
    this.contestFetcher = contestFetcher;
  }

  public static void main(String[] args) throws Exception {
    Invocation invocation = Invocation.parseFrom(args);
    final ContestDownloader contestFetcher = new ContestDownloader()
        .setFile(invocation.getFile())
        .setUrl(invocation.getUrl())
        .setTextFormat(invocation.isTextFormat())
        .setApi(invocation.getApi());
    if (invocation.getUsername() != null || invocation.getPassword() != null) {
      contestFetcher.setCredentials(invocation.getUsername(), invocation.getPassword());
    }

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
        .filterGroups(g -> invocation.getGroups() == null || invocation.getGroups().equals(g.getName()))
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
      if (dispatcher.notifyJudgement(judgement) == null) {
        continue;
      }

      final JudgementType judgementType = model.getJudgementType(judgement.getJudgementTypeId());
      final Submission submission = model.getSubmission(judgement.getSubmissionId());
      final Problem problem = model.getProblem(submission.getProblemId());
      final Team team = model.getTeam(submission.getTeamId());
      final ScoreboardRow row = model.getRow(team);

      System.out.println(
          PrettyPrinter.formatVerdictRow(team, problem, submission, judgementType, row));
    }
  }

  @Command(name = "resolver")
  private void showResolver(Invocation invocation) throws Exception {
    ClicsContest entireContest = contestFetcher.fetch();
    ScoreboardModelImpl reference = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> invocation.getGroups() == null || invocation.getGroups().equals(g.getName()))
        .filterTooLateSubmissions()
        .build();
    ResolverController controller = new ResolverController(entireContest, reference);

    ScoreboardModelImpl model = ScoreboardModelImpl.newBuilder(entireContest, controller.getModel()).build();
    controller.observers.add(model);

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
      TextFormat.print(contestFetcher.fetch(), System.out);
    } else {
      contestFetcher.fetch().writeTo(System.out);
    }
  }
}
