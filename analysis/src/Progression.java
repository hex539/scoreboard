package me.hex539.analysis;

import com.google.protobuf.Duration;
import edu.clics.proto.ClicsProto.*;
import java.util.function.Function;
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
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import me.hex539.contest.model.Problems;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

public class Progression {
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
//      fullModel.toBuilder()
//        .withEmptyScoreboard()
//        .filterSubmissions(x -> false)
//        .build();
    JudgementDispatcher dispatcher = new JudgementDispatcher(model);
    dispatcher.observers.add(model);

    final Map<String, TeamProgression> teams = new HashMap<>();
    model.getTeamsModel().getTeams().forEach(
        team -> teams.put(team.getId(), new TeamProgression(team, model.getProblemsModel())));

    final Map<String, Judgement> judgementsMap = fullModel.getJudgeModel().getJudgements()
        .stream()
        .collect(Collectors.toMap(
            Judgement::getSubmissionId,
            Function.identity(),
            (a, b) -> b));

    for (Submission submission : fullModel.getJudgeModel().getSubmissions()) {
      try {
        model.getSubmission(submission.getId());
      } catch (Exception e) {
        if (!dispatcher.notifySubmission(submission)) {
          continue;
        }
      }

      ScoreboardProblem verdict = dispatcher.notifyJudgement(judgementsMap.get(submission.getId()));
      if (verdict == null || !verdict.getSolved()) {
        continue;
      }
      teams.get(submission.getTeamId()).problemAttempted(verdict);

      for (ScoreboardRow row : model.getRanklistModel().getRows()) {
        if (row.getScore().getNumSolved() > 0) {
          teams.get(row.getTeamId()).rankChanged(submission.getContestTime(), row.getRank());
        }
      }
    }

    EnvironmentConfiguration configuration =
        EnvironmentConfigurationBuilder.configuration()
            .escape()
                .withInitialEngine("xml")
                .withDefaultEngine("xml")
                .engines().add("xml", StringEscapeUtils::escapeXml).and()
                .and()
            .functions()
                .add(new SimpleJtwigFunction() {
                  @Override
                  public String name() {
                    return "lightordark";
                  }

                  @Override
                  public Object execute(FunctionRequest request) {
                    return isDark(request.getArguments().get(0).toString()) ? "dark" : "light";
                  }
                })
                .and()
            .build();
    JtwigTemplate template = JtwigTemplate.classpathTemplate(
        "/me/hex539/analysis/progression.svg.twig",
        configuration);
    template.render(
        JtwigModel.newModel()
            .with(
                "teams",
                model.getRanklistModel().getRows().stream()
                    .map(row -> teams.get(row.getTeamId()).finalise(row, model))
                    .collect(Collectors.toList()))
            .with(
                "problems",
                model.getProblemsModel().getProblems())
            .with(
                "contest",
                model.getContest())
            .with(
                "contest_minutes",
                model.getContest().getContestDuration().getSeconds() / 60),
        System.out);
  }

  private static class TeamProgression {
    public final Team team;
    public final TreeMap<Long, Long> rankProgression = new TreeMap<>();
    public final Map<String, TreeMap<Long, ScoreboardProblem>> problemScoreProgression = new HashMap<>();
    public final Map<String, Long> solvedAt = new HashMap<>();

    public ScoreboardRow row;
    public ScoreboardModel model;

    public TeamProgression(Team team, Problems problems) {
      this.team = team;
      problems.getProblems().forEach(
          p -> problemScoreProgression.put(p.getId(), new TreeMap<Long, ScoreboardProblem>()));
    }

    public TeamProgression finalise(ScoreboardRow row, ScoreboardModel model) {
      this.row = row;
      this.model = model;
      return this;
    }

    public void rankChanged(Duration time, long rank) {
      final long minute = time.getSeconds() / 60;
      if (rankProgression.isEmpty()
          || rankProgression.get(rankProgression.lastKey()) != rank) {
        rankProgression.put(minute, rank);
      }
    }

    public void problemAttempted(ScoreboardProblem verdict) {
      if (solvedAt.containsKey(verdict.getProblemId())) {
        return;
      }
      if (!verdict.getSolved()) {
        return;
      }
      problemScoreProgression.get(verdict.getProblemId()).put(
          verdict.getTime(),
          verdict);
      solvedAt.put(verdict.getProblemId(), verdict.getTime());
    }

    private long rankAt(final long minute) {
      if (rankProgression.isEmpty() || rankProgression.firstKey() > minute) {
        return -1;
      }
      return rankProgression.floorEntry(minute).getValue();
    }

    private long scoreAt(final long minute) {
      long res = 0;
      for (long when : solvedAt.values()) {
        if (when <= minute) {
          res++;
        }
      }
      return res;
    }

    public TreeMap<Long, Double> xToY() {
      return LongStream.rangeClosed(0, model.getContest().getContestDuration().getSeconds() / 60)
          .filter(minute -> rankAt(minute) > 0)
          .boxed()
          .collect(Collectors.toMap(
              Function.identity(),
              minute -> {
                double avg = 0, total = 0;
                for (long i = minute - 4; i <= minute + 4; i++) {
                  if (rankAt(i) > 0 && scoreAt(i) == scoreAt(minute)) {
                    final double weight= Math.pow(Math.sqrt(2), -Math.abs(i - minute));
                    avg += rankAt(i) * weight;
                    total += weight;
                  }
                }
                avg /= total;
                return avg;
              },
              (k1, k2) -> {throw new AssertionError("IntStream should not have identical values");},
              TreeMap<Long, Double>::new));
    }

    public String citation() {
      if (row.getRank() % 100 >= 10 && row.getRank() % 100 < 20) {
        return row.getRank() + "th place";
      } else {
        final String[] suffix = {"th", "st", "nd", "rd", "th"};
        return ""
            + row.getRank()
            + suffix[Math.min(suffix.length - 1, (int) (row.getRank() % 10))]
            + " place, "
            + row.getScore().getNumSolved()
            + " solved, "+
            + row.getScore().getTotalTime()
            + " penalty";
      }
    }
  }

  private static boolean isDark(String colourCode) {
    if (colourCode.startsWith("#")) {
      colourCode = colourCode.substring(1);
    }

    final int[] rgb;
    if (colourCode.length() == 3) {
      rgb = new int[] {
        Integer.valueOf(colourCode.substring(0, 1), 16) * 0x11,
        Integer.valueOf(colourCode.substring(1, 2), 16) * 0x11,
        Integer.valueOf(colourCode.substring(2, 3), 16) * 0x11
      };
    } else if (colourCode.length() == 6) {
      rgb = new int[] {
        Integer.valueOf(colourCode.substring(0, 2), 16),
        Integer.valueOf(colourCode.substring(2, 4), 16),
        Integer.valueOf(colourCode.substring(4, 6), 16)
      };
    } else {
      return false;
    }

    return (rgb[0] + rgb[1] * 3 + rgb[2] * 2) < 0x200;
  }
}
