package me.hex539.analysis;

import edu.clics.proto.ClicsProto.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.Arrays;
import java.util.Collections;
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
  private static final long MINUTES_PER_BAR = 5;
  private static final long SECONDS_DIVIDER = 60 * MINUTES_PER_BAR;
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

    draw(new ContestDownloader(source).fetch(), problemLabels);
  }

  private static void draw(ClicsContest entireContest, Set<String> problemLabels) throws Exception {
    entireContest = MissingJudgements.ensureJudgements(entireContest);

    String mostPopularGroup =
        entireContest.getTeamsMap().values().stream().flatMap(t -> t.getGroupIdsList().stream())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet().stream().max(Map.Entry.comparingByValue())
        .get().getKey();

    final Set<String> problemIds = entireContest.getProblems().values().stream()
        .filter(p -> problemLabels.contains(p.getLabel()))
        .map(Problem::getId)
        .collect(Collectors.toSet());

    ScoreboardModelImpl fullModel = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> !g.getHidden())
        .filterGroups(g -> mostPopularGroup.equals(g.getId()))
        .filterSubmissions(s -> problemIds.isEmpty() || problemIds.contains(s.getProblemId()))
        .filterTooLateSubmissions()
        .build();

    final Map<String, Judgement> judgementsMap = fullModel.getJudgements()
        .stream()
        .collect(Collectors.toMap(
            Judgement::getSubmissionId,
            Function.identity(),
            (a, b) -> b));

    final int contestSegments = (int) (fullModel.getContest().getContestDuration().getSeconds() / SECONDS_DIVIDER);
    final long[] pending = new long[contestSegments];
    final long[] accepted = new long[contestSegments];
    final long[] failed = new long[contestSegments];

    for (Submission submission : fullModel.getSubmissions()) {
      final int submissionSegment = (int) (submission.getContestTime().getSeconds() / SECONDS_DIVIDER);
      final Judgement judgement = judgementsMap.get(submission.getId());
      if (judgement == null) {
        System.err.println("No judgement: " + submission.getId());
        pending[submissionSegment] = Math.min(pending[submissionSegment] + 1, MAX_SUBMISSIONS);
      } else if (entireContest.getJudgementTypes()
          .get(judgement.getJudgementTypeId())
          .getSolved()) {
        accepted[submissionSegment] = Math.min(accepted[submissionSegment] + 1, MAX_SUBMISSIONS);
      } else {
        failed[submissionSegment] = Math.min(failed[submissionSegment] + 1, MAX_SUBMISSIONS);
      }
    }

    /* TODO: Escape TeX instead of HTML. */
    EnvironmentConfiguration configuration =
        EnvironmentConfigurationBuilder.configuration()
            .escape()
                .withInitialEngine("xml")
                .withDefaultEngine("xml")
                .engines().add("xml", StringEscapeUtils::escapeXml).and()
                .and()
            .build();
    JtwigTemplate template = JtwigTemplate.classpathTemplate(
        "/me/hex539/analysis/activity.tex.twig",
        configuration);
    template.render(
        JtwigModel.newModel()
            .with("pending", LongStream.of(pending).boxed().collect(Collectors.toList()))
            .with("accepted", LongStream.of(accepted).boxed().collect(Collectors.toList()))
            .with("failed", LongStream.of(failed).boxed().collect(Collectors.toList())),
        System.out);
  }
}
