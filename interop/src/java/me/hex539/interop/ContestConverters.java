package me.hex539.interop;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import edu.clics.proto.ClicsProto;
import edu.clics.proto.ClicsProto_2020_03;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.domjudge.proto.DomjudgeProto;

import static edu.clics.proto.ClicsProto.*;
import static edu.clics.proto.ClicsProto_2020_03.*;

public class ContestConverters {
  private ContestConverters() {}
  private static class U extends ContestConverters {}

  static <K, A, B> Map<K, B> map(List<A> a, Function<A, B> ab, Function<B, K> bk) {
    return a.stream().map(ab).collect(Collectors.toMap(bk, Function.identity()));
  }

  static <K, B> Map<K, B> map(List<B> b, Function<B, K> bk) {
    return b.stream().collect(Collectors.toMap(bk, Function.identity()));
  }

  public static ClicsProto.ClicsContest toClics(ClicsContest_2020_03 old) {
    return ClicsProto.ClicsContest.newBuilder()
        .setContest(U.toClics(old.getContest()))
        .setScoreboard(U.toClics(old.getScoreboardList()))
        .addAllEventFeed(old.getEventFeedList())
        .putAllJudgementTypes(old.getJudgementTypesMap())
        .putAllLanguages(old.getLanguagesMap())
        .putAllProblems(old.getProblemsMap())
        .putAllGroups(old.getGroupsMap())
        .putAllOrganizations(old.getOrganizationsMap())
        .putAllTeams(old.getTeamsMap())
        .putAllTeamMembers(old.getTeamMembersMap())
        .putAllSubmissions(old.getSubmissionsMap())
        .putAllJudgements(old.getJudgementsMap())
        .putAllRuns(old.getRunsMap())
        .putAllClarifications(old.getClarificationsMap())
        .putAllAwards(old.getAwardsMap())
        .build();
  }

  public static ClicsProto.Contest toClics(Contest_2020_03 old) {
    final var bob = ClicsProto.Contest.newBuilder();
    bob.setId(old.getId());
    bob.setName(old.getName());
    bob.setFormalName(old.getFormalName());
    bob.setStartTime(old.getStartTime());
    bob.setContestDuration(old.getContestDuration());
    bob.setScoreboardFreezeDuration(old.getScoreboardFreezeDuration());
    if (old.getPenaltyTime() != 0) {
      bob.setPenaltyTime(
              Duration.newBuilder().setSeconds(60 * old.getPenaltyTime()).build());
    }
    bob.addAllBanners(old.getBannersList());
    bob.addAllLogos(old.getLogosList());
    return bob.build();
  }

  public static ClicsProto.Scoreboard toClics(List<ScoreboardRow_2020_03> old) {
    return ClicsProto.Scoreboard.newBuilder()
        .addAllRows(old.stream().map(U::toClics).collect(Collectors.toList()))
        .build();
  }

  public static ClicsProto.ScoreboardRow toClics(ScoreboardRow_2020_03 old) {
    return ClicsProto.ScoreboardRow.newBuilder()
        .setRank(old.getRank())
        .setTeamId(old.getTeamId())
        .setScore(U.toClics(old.getScore()))
        .addAllProblems(
            old.getProblemsList().stream().map(U::toClics).collect(Collectors.toList()))
        .build();
  }

  public static ClicsProto.ScoreboardScore toClics(ScoreboardScore_2020_03 old) {
    return ClicsProto.ScoreboardScore.newBuilder()
        .setNumSolved(old.getNumSolved())
        .setTotalTime(
            Duration.newBuilder().setSeconds(60 * old.getTotalTime()).build())
        .build();
  }

  public static ClicsProto.ScoreboardProblem toClics(ScoreboardProblem_2020_03 old) {
    return ClicsProto.ScoreboardProblem.newBuilder()
        .setProblemId(old.getProblemId())
        .setNumJudged(old.getNumJudged())
        .setNumPending(old.getNumPending())
        .setSolved(old.getSolved())
        .setTime(
            Duration.newBuilder().setSeconds(60 * old.getTime()).build())
        .build();
  }

  public static ClicsProto.ClicsContest toClics(DomjudgeProto.EntireContest dom) {
    return ClicsProto.ClicsContest.newBuilder()
        .setContest(
            U.toClics(dom.getContest()))
        .setScoreboard(
            D.toClics(dom.getScoreboardList(), dom.getProblemsList()))
        .putAllJudgementTypes(
            map(dom.getJudgementTypesList(), D::toClics, ClicsProto.JudgementType::getId))
        .putAllLanguages(
            map(dom.getLanguagesList(), D::toClics, ClicsProto.Language::getId))
        .putAllProblems(
            map(D.toClics(dom.getProblemsList()), ClicsProto.Problem::getId))
        .putAllGroups(
            map(dom.getCategoriesList(), D::toClics, ClicsProto.Group::getId))
        .putAllOrganizations(
            map(dom.getAffiliationsList(), D::toClics, ClicsProto.Organization::getId))
        .putAllTeams(
            map(dom.getTeamsList(), D::toClics, ClicsProto.Team::getId))
        .putAllSubmissions(
            map(dom.getSubmissionsList(), s -> D.toClics(s, dom.getContest()), ClicsProto.Submission::getId))
        .putAllJudgements(
            map(dom.getJudgingsList(), j -> D.toClics(j, dom.getContest()), ClicsProto.Judgement::getId))
        .build();
  }

  public static ClicsProto.Contest toClics(DomjudgeProto.Contest dom) {
    final long PENALTY_TIME = 20;

    ClicsProto.Contest.Builder bob = ClicsProto.Contest.newBuilder()
        .setId("" + dom.getId())
        .setName("" + dom.getName())
        .setFormalName(dom.getName())
        .setPenaltyTime(Duration.newBuilder()
            .setSeconds(PENALTY_TIME * 60)
            .build());

    if (dom.hasStart()) {
      bob.setStartTime(Timestamp.newBuilder().setSeconds(dom.getStart().getValue()));
    }
    if (dom.hasStart() && dom.hasEnd()) {
      bob.setContestDuration(Duration.newBuilder()
          .setSeconds(dom.getEnd().getValue() - dom.getStart().getValue())
          .build());
    }
    if (dom.hasFreeze() && dom.hasEnd()) {
      bob.setScoreboardFreezeDuration(Duration.newBuilder()
          .setSeconds(dom.getEnd().getValue() - dom.getFreeze().getValue())
          .build());
    }
    return bob.build();
  }

  // @return {@param x} in seconds, rounded towards zero.
  private static final long doubleToSeconds(double x) {
    return (long) x;
  }

  private static final int doubleToNanos(double x) {
    return (int) (1000000000L * (x - doubleToSeconds(x)));
  }

  private static final Timestamp doubleToTimestamp(double x) {
    return Timestamp.newBuilder()
        .setSeconds(doubleToSeconds(x))
        .setNanos(doubleToNanos(x))
        .build();
  }

  private static final Duration doubleToDuration(double x) {
    return Duration.newBuilder()
        .setSeconds(doubleToSeconds(x))
        .setNanos(doubleToNanos(x))
        .build();
  }

  private static class D {

    public static ClicsProto.JudgementType toClics(DomjudgeProto.JudgementType dom) {
      return ClicsProto.JudgementType.newBuilder()
          .setId(dom.getLabel())
          .setName(dom.getName())
          .setPenalty(dom.getPenalty())
          .setSolved(dom.getSolved())
          .build();
    }

    public static ClicsProto.Language toClics(DomjudgeProto.Language dom) {
      return ClicsProto.Language.newBuilder()
          .setId(dom.getId())
          .setName(dom.getId())
          .build();
    }

    public static List<ClicsProto.Problem> toClics(List<DomjudgeProto.Problem> dom) {
      List<ClicsProto.Problem> problems = dom.stream()
          .map(D::toClics)
          .sorted((a, b) -> a.getLabel().compareTo(b.getLabel()))
          .collect(Collectors.toCollection(ArrayList::new));
      for (int i = problems.size(); i --> 0;) {
        problems.set(i, problems.get(i).toBuilder().setOrdinal(i).build());
      }
      return problems;
    }

    public static ClicsProto.Problem toClics(DomjudgeProto.Problem dom) {
      return ClicsProto.Problem.newBuilder()
          .setId("" + dom.getId())
          .setLabel(dom.getLabel())
          .setName(dom.getName())
          .setOrdinal(1)
          .setRgb(Optional.ofNullable(HtmlColours.nameToRgb(dom.getColor())).orElse("#ff0000"))
          .setColor(dom.getColor())
          .build();
    }

    public static ClicsProto.Group toClics(DomjudgeProto.Category dom) {
      return ClicsProto.Group.newBuilder()
          .setId("" + dom.getId())
          .setName("" + dom.getName())
          .setType("" + dom.getName())
          // TODO: iffy.
          .setHidden(dom.hasSortOrder() && dom.getSortOrder().getValue() > 0)
          .build();
    }

    public static ClicsProto.Organization toClics(DomjudgeProto.Affiliation dom) {
      return ClicsProto.Organization.newBuilder()
          .setId("" + dom.getId())
          .setName(dom.getName())
          .setFormalName(dom.getName())
          .setCountry(dom.getCountry())
          .build();
    }

    public static ClicsProto.Team toClics(DomjudgeProto.Team dom) {
      ClicsProto.Team.Builder b = ClicsProto.Team.newBuilder()
          .setId("" + dom.getId())
          .setName(dom.getName())
          .setOrganizationId("" + dom.getAffilId().getValue());
      if (dom.hasCategory()) {
        b.addGroupIds("" + dom.getCategory().getValue());
      }
      return b.build();
    }

    public static ClicsProto.Submission toClics(
        DomjudgeProto.Submission dom,
        DomjudgeProto.Contest contest) {
      ClicsProto.Submission.Builder b = ClicsProto.Submission.newBuilder()
          .setId("" + dom.getId())
          .setLanguageId(dom.getLanguage())
          .setTime(doubleToTimestamp(dom.getTime()))
          .setContestTime(doubleToDuration(contest.hasStart()
              ? dom.getTime() - contest.getStart().getValue()
              : dom.getTime()));
      if (dom.hasProblem()) {
        b.setProblemId("" + dom.getProblem().getValue());
      }
      if (dom.hasTeam()) {
        b.setTeamId("" + dom.getTeam().getValue());
      }
      return b.build();
    }

    public static ClicsProto.Judgement toClics(
        DomjudgeProto.Judging dom,
        DomjudgeProto.Contest contest) {
      final String type;
      if (dom.getOutcome().toLowerCase().startsWith("compile")) {
        type = "CE";
      } else {
        switch ((dom.getOutcome().toLowerCase() + " ").charAt(0)) {
          case 'c': type = "AC"; break;
          case 'm': type = "MLE"; break;
          case 'n': type = "NO"; break;
          case 'o': type = "OLE"; break;
          case 'p': type = "PE"; break;
          case 't': type = "TLE"; break;
          case 'w': type = "WA"; break;
          default: type = "RTE"; break;
        }
      }
      return ClicsProto.Judgement.newBuilder()
          .setId("" + dom.getId())
          .setSubmissionId("" + dom.getSubmission())
          .setJudgementTypeId(type)
          .setStartTime(doubleToTimestamp(dom.getTime()))
          .setStartContestTime(doubleToDuration(contest.hasStart()
              ? dom.getTime() - contest.getStart().getValue()
              : dom.getTime()))
          .setEndTime(doubleToTimestamp(dom.getTime()))
          .setEndContestTime(doubleToDuration(contest.hasStart()
              ? dom.getTime() - contest.getStart().getValue()
              : dom.getTime()))
          .setMaxRunTime(0)
          .build();
    }

    public static ClicsProto.Scoreboard toClics(
        List<DomjudgeProto.ScoreboardRow> rows,
        List<DomjudgeProto.Problem> problems) {
      Map<String, ClicsProto.Problem> p =
          ContestConverters.map(problems, D::toClics, ClicsProto.Problem::getLabel);
      return ClicsProto.Scoreboard.newBuilder()
          .addAllRows(
              rows.stream().map(x -> toClics(x, p)).collect(Collectors.toList()))
          .build();
    }

    public static ClicsProto.ScoreboardRow toClics(
        DomjudgeProto.ScoreboardRow dom,
        Map<String, ClicsProto.Problem> problemsByLabel) {
      return ClicsProto.ScoreboardRow.newBuilder()
          .setRank(dom.getRank())
          .setTeamId("" + dom.getTeam())
          .setScore(toClics(dom.getScore()))
          .addAllProblems(
              dom.getProblemsList().stream()
                  .sorted((a, b) -> a.getLabel().compareTo(b.getLabel()))
                  .map(sp -> toClics(sp, problemsByLabel))
                  .collect(Collectors.toList()))
          .build();
    }

    public static ClicsProto.ScoreboardScore toClics(DomjudgeProto.ScoreboardScore dom) {
      return ClicsProto.ScoreboardScore.newBuilder()
          .setNumSolved((int) dom.getNumSolved())
          .setTotalTime(Duration.newBuilder().setSeconds(60 * (long) dom.getTotalTime()).build())
          .build();
    }

    public static ClicsProto.ScoreboardProblem toClics(
        DomjudgeProto.ScoreboardProblem sp,
        Map<String, ClicsProto.Problem> problemsByLabel) {
      ClicsProto.ScoreboardProblem.Builder bob =  ClicsProto.ScoreboardProblem.newBuilder()
          .setProblemId(problemsByLabel.get(sp.getLabel()).getId())
          .setNumJudged((int) sp.getNumJudged())
          .setNumPending((int) sp.getNumPending())
          .setSolved(sp.getSolved());
      if (sp.hasTime()) {
          // bob.setTime(Duration.newBuilder().setSeconds(60L * sp.getTime().getValue()).build());
          bob.setTime(Durations.fromMinutes(sp.getTime().getValue()));
      }
      return bob.build();
    }
  }
}
