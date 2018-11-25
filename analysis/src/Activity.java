package me.hex539.analysis;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import edu.clics.proto.ClicsProto.*;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.hex539.contest.ApiDetective;
import me.hex539.contest.ContestConfig;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.MissingJudgements;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

public class Activity {
  private static final long SECONDS_PER_BAR = 60 * 5;
  private static final long MAX_SUBMISSIONS = 20;

  public static void main(String[] args) throws Exception {
    Invocation invocation = Invocation.parseFrom(args);

    final ContestConfig.Source source;
    if (invocation.getUrl() != null) {
      ContestConfig.Source.Builder sourceBuilder =
          ApiDetective.detectApi(invocation.getUrl()).get()
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
      System.err.println("Need one of --url or --file to load a contest");
      System.exit(1);
      return;
    }

    final Set<String> problemLabels;
    if (invocation.getProblems() != null) {
      problemLabels = Arrays.stream(invocation.getProblems().split(","))
          .collect(Collectors.toSet());
    } else {
      problemLabels = Collections.emptySet();
    }

    draw(
        /* workingDirectory= */ new File(invocation.getActions().size() > 0
            ? invocation.getActions().get(0)
            : "."),
        /* entireContest= */ new ContestDownloader(source).fetch(),
        /* problemLabels= */ problemLabels,
        /* applyFreeze= */ invocation.getApplyFreeze(),
        /* printSolvestats= */ invocation.getPrintSolvestats());

  }

  private static void draw(
      File workingDirectory,
      ClicsContest entireContest,
      Set<String> problemLabels,
      boolean applyFreeze,
      boolean printSolveStats) throws Exception {
    entireContest = MissingJudgements.ensureJudgements(entireContest);

    final boolean drawActivity = true;
    final boolean drawLanguageStats = true;

    String mostPopularGroup =
        entireContest.getTeamsMap().values().stream().flatMap(t -> t.getGroupIdsList().stream())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet().stream().max(Map.Entry.comparingByValue())
        .get().getKey();

    ScoreboardModelImpl fullModel = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> !g.getHidden())
        .filterGroups(g -> mostPopularGroup.equals(g.getId()))
        .filterTooLateSubmissions()
        .build();

    Stream<Judgement> judgementsStream = fullModel.getJudgements().stream();
    if (applyFreeze && entireContest.getContest().hasScoreboardFreezeDuration()) {
      final Timestamp freezeTime = Timestamps.subtract(
          Timestamps.add(
              Timestamp.getDefaultInstance(),
              entireContest.getContest().getContestDuration()),
          entireContest.getContest().getScoreboardFreezeDuration());
      judgementsStream = judgementsStream
          .filter(j ->
              Durations.toNanos(
                  Timestamps.between(
                      freezeTime,
                      Timestamps.add(
                          Timestamp.getDefaultInstance(),
                          fullModel.getSubmission(j.getSubmissionId()).getContestTime()))) < 0);
    }
    final Map<String, Judgement> judgementsMap = judgementsStream
        .collect(Collectors.toMap(
            Judgement::getSubmissionId,
            Function.identity(),
            (a, b) -> b));

    final Map<String, SubmitStats> statsByProblem = new HashMap<>();
    for (String problemId : entireContest.getProblems().keySet()) {
      statsByProblem.put(problemId, new SubmitStats(entireContest.getContest()));
    }

    final Map<String, SubmitStats> statsByLanguage = new HashMap<>();
    for (String languageId : entireContest.getLanguages().keySet()) {
      statsByLanguage.put(languageId, new SubmitStats(entireContest.getContest()));
    }

    for (Submission s : fullModel.getSubmissions()) {
      final Judgement judgement = judgementsMap.get(s.getId());
      statsByProblem.get(s.getProblemId()).add(s, judgement, fullModel);
      statsByLanguage.get(s.getLanguageId()).add(s, judgement, fullModel);
    }

    EnvironmentConfiguration configuration =
        EnvironmentConfigurationBuilder.configuration()
            .build();

    if (drawActivity) {
      JtwigTemplate template = JtwigTemplate.classpathTemplate(
          "/me/hex539/analysis/activity.tex.twig",
          configuration);

      final File activityDir = new File(workingDirectory, "activity");
      activityDir.mkdir();

      fullModel.getProblems().stream().parallel().forEach(problem -> {
        final File file = new File(activityDir, problem.getLabel() + ".tex");
        final SubmitStats stats = statsByProblem.get(problem.getId()).crop();

        try (final OutputStream outputStream = new FileOutputStream(file)) {
          saveActivityChart(stats, template, outputStream);
        } catch (IOException e) {
          System.err.println("Error writing to: " + file.getPath());
        }

        if (printSolveStats) {
          System.out.printf(
              "\\newcommand{\\solvestats%s{%d}{%d} %% +%d?\n",
              problem.getLabel(),
              stats.totalAttempts, // stats.teamsAttempted.size(),
              stats.totalAccepted, // stats.teamsAccepted.size(),
              stats.totalPending);// stats.teamsPending.size());
        } else {
          System.err.println("Saved file: " + file.getPath());
        }
      });
    }

    if (drawLanguageStats) {
      final File file = new File(workingDirectory, "language_stats.tex");
      try (final OutputStream outputStream = new FileOutputStream(file)) {
        saveLanguagesChart(
            new ArrayList<>(entireContest.getLanguages().values()),
            entireContest.getLanguages().keySet().stream().map(statsByLanguage::get).collect(Collectors.toList()),
            JtwigTemplate.classpathTemplate(
                "/me/hex539/analysis/language_stats.tex.twig",
                configuration),
            outputStream);
      }
      if (!printSolveStats) {
        System.err.println("Saved file: " + file.getPath());
      }
    }
  }

  private static void saveActivityChart(
      SubmitStats stats,
      JtwigTemplate template,
      OutputStream outputStream) {
    template.render(
        JtwigModel.newModel()
            .with("pending", LongStream.of(stats.pending).boxed().collect(Collectors.toList()))
            .with("accepted", LongStream.of(stats.accepted).boxed().collect(Collectors.toList()))
            .with("wronganswer", LongStream.of(stats.wrongAnswer).boxed().collect(Collectors.toList()))
            .with("timelimit", LongStream.of(stats.timeLimit).boxed().collect(Collectors.toList()))
            .with("failed", LongStream.of(stats.otherFailed).boxed().collect(Collectors.toList())),
        outputStream);
  }

  private static void saveLanguagesChart(
      List<Language> languages,
      List<SubmitStats> languageStats,
      JtwigTemplate template,
      OutputStream outputStream) {
    template.render(
        JtwigModel.newModel()
            .with("languages", languages)
            .with("stats", languageStats),
        outputStream);
  }

  private static class SubmitStats {
    int totalAttempts = 0;
    int totalAccepted = 0;
    int totalPending = 0;

    int totalTimeLimit = 0;
    int totalWrongAnswer = 0;
    int totalOtherFailed = 0;

    final Set<String> teamsAttempted = new HashSet<>();
    final Set<String> teamsAccepted = new HashSet<>();
    final Set<String> teamsPending = new HashSet<>();

    final long[] pending;
    final long[] accepted;
    final long[] wrongAnswer;
    final long[] timeLimit;
    final long[] otherFailed;

    public SubmitStats(Contest contest) {
      final long totalSeconds = contest.getContestDuration().getSeconds();
      final int n = (int) ((totalSeconds + SECONDS_PER_BAR - 1) / SECONDS_PER_BAR);
      pending = new long[n];
      accepted = new long[n];
      wrongAnswer = new long[n];
      timeLimit = new long[n];
      otherFailed = new long[n];
    }

    public SubmitStats add(Submission submission, Judgement judgement, ScoreboardModel model) {
      final Team team = model.getTeam(submission.getTeamId());
      final JudgementType verdict =
          judgement != null
              && judgement.getJudgementTypeId() != null
              && !"".equals(judgement.getJudgementTypeId())
                  ? model.getJudgementType(judgement.getJudgementTypeId())
                  : null;

      if (verdict != null && verdict.getId().equals("CE")) {
        return this;
      }
      if (teamsAccepted.contains(team.getId())) {
        return this;
      }

      final int segment = (int) (submission.getContestTime().getSeconds() / SECONDS_PER_BAR);

      final long[] grouping =
          verdict == null ? pending
          : verdict.getSolved() ? accepted
          : verdict.getId().equals("TLE") ? timeLimit
          : verdict.getId().equals("WA") ? wrongAnswer
          : otherFailed;

      teamsAttempted.add(team.getId());
      totalAttempts += 1;
      if (grouping == accepted) {
        teamsAccepted.add(team.getId());
        totalAccepted += 1;
      }
      if (grouping == pending) {
        teamsPending.add(team.getId());
        totalPending += 1;
      }
      if (grouping == wrongAnswer) totalWrongAnswer += 1;
      if (grouping == timeLimit) totalTimeLimit += 1;
      if (grouping == otherFailed) totalOtherFailed += 1;

      return this;
    }

    public SubmitStats crop() {
      for (int i = 0; i < pending.length; i++) {
        // Positive Y
        accepted[i] = Math.min(accepted[i], MAX_SUBMISSIONS);
        pending[i] = Math.min(pending[i], MAX_SUBMISSIONS - accepted[i]);

        // Negative Y
        wrongAnswer[i] = Math.min(wrongAnswer[i], MAX_SUBMISSIONS);
        timeLimit[i] = Math.min(timeLimit[i], MAX_SUBMISSIONS - wrongAnswer[i]);
        otherFailed[i] = Math.min(otherFailed[i], MAX_SUBMISSIONS - timeLimit[i]);
      }

      return this;
    }
  }
}
