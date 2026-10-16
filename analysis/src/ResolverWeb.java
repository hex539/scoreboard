package me.hex539.analysis;

import com.google.protobuf.util.Durations;
import edu.clics.proto.ClicsProto.*;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.TreeMap;
import me.hex539.contest.ContestConfig;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.JudgementDispatcher;
import me.hex539.contest.MissingJudgements;
import me.hex539.contest.ScoreboardModelImpl;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

public class ResolverWeb {
  private static Invocation invocation;

  public static void main(String[] args) throws Exception {
    invocation = Invocation.parseFrom(args);
    final ContestConfig.Source source = Analyser.getSource(invocation);
    draw(new ContestDownloader(source).fetch());
  }

  private static void draw(ClicsContest entireContest) throws Exception {
    entireContest = MissingJudgements.ensureJudgements(entireContest);

    ScoreboardModelImpl fullModel = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(Analyser.getGroupPredicate(invocation, entireContest))
        .filterTooLateSubmissions()
        .build();
    ScoreboardModelImpl model = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(Analyser.getGroupPredicate(invocation, entireContest))
        .withEmptyScoreboard()
        .filterSubmissions(x -> false)
        .build();
    JudgementDispatcher dispatcher = new JudgementDispatcher(model);
    dispatcher.observers.add(model);

    boolean hasFreeze = entireContest.getContest().hasScoreboardFreezeDuration();
    long contestDurationSeconds = entireContest.getContest().getContestDuration().getSeconds();
    long freezeDurationSeconds = hasFreeze ? entireContest.getContest().getScoreboardFreezeDuration().getSeconds() : 0;
    long freezeTimeSeconds = contestDurationSeconds - freezeDurationSeconds;

    List<Submission> sortedSubmissions = fullModel.getJudgeModel().getSubmissions().stream()
        .sorted(Comparator.comparingLong(s -> s.getContestTime().getSeconds()))
        .collect(Collectors.toList());

    final Map<String, Judgement> judgementsMap = fullModel.getJudgeModel().getJudgements()
        .stream()
        .collect(Collectors.toMap(
            Judgement::getSubmissionId,
            Function.identity(),
            (a, b) -> b));

    for (Submission submission : sortedSubmissions) {
      try {
        model.getSubmission(submission.getId());
      } catch (Exception e) {
        if (!dispatcher.notifySubmission(submission)) {
          continue;
        }
      }

      if (submission.getContestTime().getSeconds() < freezeTimeSeconds) {
        dispatcher.notifyJudgement(judgementsMap.get(submission.getId()));
      }
    }

    List<Map<String, Object>> serializedTeams = new ArrayList<>();
    for (Team team : model.getTeamsModel().getTeams()) {
      ScoreboardRow row = model.getRanklistModel().getRow(team);
      if (row == null) {
        continue;
      }
      Map<String, Object> teamMap = new HashMap<>();
      teamMap.put("id", team.getId());
      teamMap.put("name", team.getName());
      
      String orgName = "";
      if (!team.getOrganizationId().isEmpty()) {
        try {
          orgName = model.getTeamsModel().getOrganization(team.getOrganizationId()).getName();
        } catch (Exception ignored) {}
      }
      teamMap.put("organization", orgName);
      teamMap.put("rank", row.getRank());
      teamMap.put("solved", row.getScore().getNumSolved());
      teamMap.put("penalty", Durations.toMinutes(row.getScore().getTotalTime()));

      List<Map<String, Object>> probsList = new ArrayList<>();
      for (Problem prob : model.getProblemsModel().getProblems()) {
        ScoreboardProblem attempts = model.getRanklistModel().getAttempts(team, prob);
        ScoreboardProblem fullAttempts = fullModel.getRanklistModel().getAttempts(team, prob);

        Map<String, Object> probMap = new HashMap<>();
        probMap.put("problemId", prob.getId());
        probMap.put("solved", attempts != null && attempts.getSolved());
        probMap.put("attempts", attempts != null ? attempts.getNumJudged() : 0);
        probMap.put("pending", attempts != null && attempts.getNumPending() > 0);
        probMap.put("willSolve", fullAttempts != null && fullAttempts.getSolved());
        probMap.put("solveTime", fullAttempts != null ? Durations.toMinutes(fullAttempts.getTime()) : 0);
        probMap.put("finalAttempts", fullAttempts != null ? fullAttempts.getNumJudged() : 0);
        probsList.add(probMap);
      }
      teamMap.put("problems", probsList);
      serializedTeams.add(teamMap);
    }

    List<Map<String, Object>> serializedProblems = new ArrayList<>();
    for (Problem prob : model.getProblemsModel().getProblems()) {
      Map<String, Object> probMap = new HashMap<>();
      probMap.put("id", prob.getId());
      probMap.put("label", prob.getLabel());
      probMap.put("name", prob.getName());
      serializedProblems.add(probMap);
    }

    EnvironmentConfiguration configuration = EnvironmentConfigurationBuilder.configuration().build();
    JtwigTemplate template = JtwigTemplate.classpathTemplate(
        "/me/hex539/analysis/resolver.html.twig",
        configuration);

    template.render(
        JtwigModel.newModel()
            .with("teams", serializedTeams)
            .with("problems", serializedProblems)
            .with("contestName", entireContest.getContest().getName()),
        System.out);
  }
}

