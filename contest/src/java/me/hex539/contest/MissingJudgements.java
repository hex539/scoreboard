package me.hex539.contest;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.protobuf.util.Durations;

import edu.clics.proto.ClicsProto.*;

public class MissingJudgements {

  private MissingJudgements() {}

  public static ClicsContest ensureJudgements(ClicsContest contest) {
    if (contest.getJudgementsCount() != 0 || contest.getSubmissionsCount() == 0) {
      return contest;
    }
    return contest.toBuilder().putAllJudgements(inventJudgements(contest)).build();
  }

  /**
   * Public versions of contests often don't include any detailed information about judging
   * results. Let's invent our own to make resolving the scoreboard possible.
   */
  private static Map<String, Judgement> inventJudgements(ClicsContest contest) {
    Map<String, Judgement> judgements = new HashMap<>();

    Map<String, Map<String, List<Submission>>> attemptsByTeamAndProblem =
        contest.getSubmissions().values().stream()
            .sorted(Comparator.comparing(s -> Durations.toNanos(s.getContestTime())))
            .collect(Collectors.groupingBy(
                Submission::getTeamId,
                Collectors.groupingBy(
                    Submission::getProblemId)));

    Map<String, Problem> problemsByLabel = contest.getProblemsMap();

    for (ScoreboardRow row : contest.getScoreboardList()) {
      for (ScoreboardProblem prob : row.getProblemsList()) {
        final boolean solved = prob.getSolved();
        long failed = prob.getNumJudged() - (solved ? 1 : 0);
        for (Submission sub : attemptsByTeamAndProblem
            .getOrDefault(row.getTeamId(), Collections.emptyMap())
            .getOrDefault(prob.getProblemId(), Collections.emptyList())) {
          final Judgement.Builder j = Judgement.newBuilder()
              .setId("j" + Integer.toHexString(judgements.size() + 1))
              .setSubmissionId(sub.getId())
              .setStartTime(sub.getTime())
              .setStartContestTime(sub.getContestTime())
              .setEndTime(sub.getTime())
              .setEndContestTime(sub.getContestTime());
          if (failed > 0) {
            j.setJudgementTypeId(contest.getJudgementTypesOrThrow("WA").getId());
            failed--;
          } else if (solved && prob.getTime() == sub.getContestTime().getSeconds() / 60) {
            j.setJudgementTypeId(contest.getJudgementTypesOrThrow("AC").getId());
          } else {
            j.setJudgementTypeId(contest.getJudgementTypesOrThrow("CE").getId());
          }
          Judgement jj = j.build();
          judgements.put(jj.getId(), jj);
        }
      }
    }
    return judgements;
  }
}
