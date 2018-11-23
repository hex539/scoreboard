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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
  private static final long SECONDS_PER_BAR = 60 * 5 / 2;
  private static final long MAX_SUBMISSIONS = 10;

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
        /* entireContest= */ new ContestDownloader(source).fetch(),
        /* problemLabels= */ problemLabels,
        /* applyFreeze= */ invocation.getApplyFreeze(),
        /* workingDirectory= */ new File(invocation.getActions().size() > 0
            ? invocation.getActions().get(0)
            : "."));
  }

  private static void draw(
      ClicsContest entireContest,
      Set<String> problemLabels,
      boolean applyFreeze,
      File workingDirectory) throws Exception {
    entireContest = MissingJudgements.ensureJudgements(entireContest);

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

    for (Submission s : fullModel.getSubmissions()) {
      statsByProblem.get(s.getProblemId()).add(s, judgementsMap.get(s.getId()), fullModel);
    }

    EnvironmentConfiguration configuration =
        EnvironmentConfigurationBuilder.configuration()
            .build();
    JtwigTemplate template = JtwigTemplate.classpathTemplate(
        "/me/hex539/analysis/activity.tex.twig",
        configuration);

    final File activityDir = new File(workingDirectory, "activity");
    activityDir.mkdir();

    fullModel.getProblems().stream().parallel().forEach(problem -> {
      final File file = new File(activityDir, problem.getLabel() + ".tex");
      try (final OutputStream outputStream = new FileOutputStream(file)) {
        saveActivityChart(
            statsByProblem.get(problem.getId()).crop(),
            template,
            outputStream);
      } catch (IOException e) {
        System.err.println("Error writing to: " + file.getPath());
      }
      System.err.println("Saved file: " + file.getPath());
    });
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

  private static class SubmitStats {
    final Set<String> teamsAttempted = new HashSet<>();
    final Set<String> teamsSolved = new HashSet<>();

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
              ? model.getJudgementType(judgement.getJudgementTypeId())
              : null;

      final long[] grouping =
          verdict == null ? pending
          : verdict.getSolved() ? accepted
          : verdict.getId().equals("TLE") ? timeLimit
          : verdict.getId().equals("WA") ? wrongAnswer
          : otherFailed;

      final int segment = (int) (submission.getContestTime().getSeconds() / SECONDS_PER_BAR);

      if (!teamsSolved.contains(team.getId()) && judgement != null) {
        teamsAttempted.add(team.getId());
        if (grouping == accepted) {
          teamsSolved.add(team.getId());
        }
      }
      grouping[segment] += 1;
      return this;
    }

    public SubmitStats crop() {
      for (int i = 0; i < pending.length; i++) {
        // Both directions
        pending[i] = Math.min(pending[i], MAX_SUBMISSIONS * 2);

        // Positive Y
        accepted[i] = Math.min(accepted[i], MAX_SUBMISSIONS);

        // Negative Y
        wrongAnswer[i] = Math.min(wrongAnswer[i], MAX_SUBMISSIONS);
        timeLimit[i] = Math.min(timeLimit[i], MAX_SUBMISSIONS - wrongAnswer[i]);
        otherFailed[i] = Math.min(otherFailed[i], MAX_SUBMISSIONS - timeLimit[i]);
      }
      return this;
    }
  }
}
