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
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.hex539.contest.ContestConfig;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.MissingJudgements;
import me.hex539.contest.ScoreboardModelImpl;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

public class Activity {
  private static Invocation invocation;

  public static void main(String[] args) throws Exception {
    invocation = Invocation.parseFrom(args);
    final ContestConfig.Source source = Analyser.getSource(invocation);

    final Set<String> problemLabels;
    if (invocation.getProblems() != null) {
      problemLabels = Arrays.stream(invocation.getProblems().split(","))
          .collect(Collectors.toSet());
    } else {
      problemLabels = Collections.emptySet();
    }

    draw(
        /* workingDirectory= */ new File(
            invocation.getActions() != null && invocation.getActions().size() > 0
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

    ScoreboardModelImpl fullModel = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(Analyser.getGroupPredicate(invocation, entireContest))
        .filterTooLateSubmissions()
        .build();

    Stream<Judgement> judgementsStream = fullModel.getJudgeModel().getJudgements().stream();
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

    for (Submission s : fullModel.getJudgeModel().getSubmissions()) {
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

      fullModel.getProblemsModel().getProblems().stream().parallel().forEach(problem -> {
        final File file = new File(activityDir, problem.getLabel() + ".tex");
        final SubmitStats stats = statsByProblem.get(problem.getId()).crop();

        try (final OutputStream outputStream = new FileOutputStream(file)) {
          saveActivityChart(stats, template, outputStream);
        } catch (IOException e) {
          System.err.println("Error writing to: " + file.getPath());
        }

        if (printSolveStats) {
          System.out.printf(
              "\\newcommand{\\solvestats%s}{\\printsolvestats{%d}{%d + ?}} %% +%d?\n",
              problem.getLabel(),
              stats.totalAttempts,
              stats.totalAccepted,
              stats.totalPending);
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
}
