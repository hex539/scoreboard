package me.hex539.interop;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import edu.clics.proto.ClicsProto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.domjudge.proto.DomjudgeProto;

public class ContestConverters {
  private ContestConverters() {}
  private static class U extends ContestConverters {}

  static <K, A, B> Map<K, B> map(List<A> a, Function<A, B> ab, Function<B, K> bk) {
    return a.stream().map(ab).collect(Collectors.toMap(bk, Function.identity()));
  }

  static <K, B> Map<K, B> map(List<B> b, Function<B, K> bk) {
    return b.stream().collect(Collectors.toMap(bk, Function.identity()));
  }

  public static ClicsProto.ClicsContest toClics(DomjudgeProto.EntireContest dom) {
    return ClicsProto.ClicsContest.newBuilder()
        .setContest(
            U.toClics(dom.getContest()))
        .addAllScoreboard(
            U.toClics(dom.getScoreboardList(), dom.getProblemsList()))
        .putAllJudgementTypes(
            map(dom.getJudgementTypesList(), U::toClics, ClicsProto.JudgementType::getId))
        .putAllLanguages(
            map(dom.getLanguagesList(), U::toClics, ClicsProto.Language::getId))
        .putAllProblems(
            map(U.toClics(dom.getProblemsList()), ClicsProto.Problem::getId))
        .putAllGroups(
            map(dom.getCategoriesList(), U::toClics, ClicsProto.Group::getId))
        .putAllOrganizations(
            map(dom.getAffiliationsList(), U::toClics, ClicsProto.Organization::getId))
        .putAllTeams(
            map(dom.getTeamsList(), U::toClics, ClicsProto.Team::getId))
        .putAllSubmissions(
            map(dom.getSubmissionsList(), s -> U.toClics(s, dom.getContest()), ClicsProto.Submission::getId))
        .putAllJudgements(
            map(dom.getJudgingsList(), j -> U.toClics(j, dom.getContest()), ClicsProto.Judgement::getId))
        .build();
  }

  private static final long doubleToSeconds(double x) {
    return Math.round(x - (0.5 - 1e-20));
  }

  private static final int doubleToNanos(double x) {
    return (int) Math.round(1000000000L * (x - doubleToSeconds(x)) - (0.5 - 1e-20));
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

  public static ClicsProto.Contest toClics(DomjudgeProto.Contest dom) {
    final long PENALTY_TIME = 20;

    ClicsProto.Contest.Builder bob = ClicsProto.Contest.newBuilder()
        .setId("" + dom.getId())
        .setName("" + dom.getName())
        .setFormalName(dom.getName())
        .setPenaltyTime(PENALTY_TIME);

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
        .map(U::toClics)
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
        case 'n':
        case 'c': type = "AC"; break;
        case 'p':
        case 'w': type = "WA"; break;
        case 't': type = "TLE"; break;
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

  public static List<ClicsProto.ScoreboardRow> toClics(
      List<DomjudgeProto.ScoreboardRow> rows,
      List<DomjudgeProto.Problem> problems) {
    Map<String, ClicsProto.Problem> p =
        ContestConverters.map(problems, U::toClics, ClicsProto.Problem::getLabel);
    return rows.stream().map(x -> toClics(x, p)).collect(Collectors.toList());
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
        .setTotalTime((long) dom.getTotalTime())
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
        bob.setTime(sp.getTime().getValue());
    }
    return bob.build();
  }
}
