package me.hex539.analysis;

import edu.clics.proto.ClicsProto.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import me.hex539.contest.ScoreboardModel;

public class SubmitStats {

  private static final long SECONDS_PER_BAR = (60 * 5) / 2;
  private static final long MAX_SUBMISSIONS = 20;

  int totalAttempts = 0;
  int totalAccepted = 0;
  int totalPending = 0;

  int totalTimeLimit = 0;
  int totalWrongAnswer = 0;
  int totalOtherFailed = 0;

  final Map<String, Set<String>> teamsAttempted = new HashMap<>();
  final Map<String, Set<String>> teamsAccepted = new HashMap<>();
  final Map<String, Set<String>> teamsPending = new HashMap<>();

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
    if (get(teamsAccepted, team).contains(submission.getProblemId())) {
      return this;
    }

    final int segment = (int) (submission.getContestTime().getSeconds() / SECONDS_PER_BAR);
    if (segment < 0 || accepted.length <= segment) {
      return this;
    }

    final long[] grouping =
        verdict == null ? pending
        : verdict.getSolved() ? accepted
        : verdict.getId().equals("TLE") ? timeLimit
        : verdict.getId().equals("WA") ? wrongAnswer
        : otherFailed;

    add(teamsAttempted, team, submission);
    totalAttempts += 1;
    if (grouping == accepted) {
      add(teamsAccepted, team, submission);
      totalAccepted += 1;
    }
    if (grouping == pending) {
      add(teamsPending, team, submission);
      totalPending += 1;
    }
    if (grouping == wrongAnswer) totalWrongAnswer += 1;
    if (grouping == timeLimit) totalTimeLimit += 1;
    if (grouping == otherFailed) totalOtherFailed += 1;

    grouping[segment] += 1;
    return this;
  }

  private Set<String> get(Map<String, Set<String>> map, Team team) {
    final Set<String> res;
    if (!map.containsKey(team.getId())) {
      map.put(team.getId(), (res = new HashSet<String>()));
    } else {
      res = map.get(team.getId());
    }
    return res;
  }

  private void add(Map<String, Set<String>> map, Team team, Submission submission) {
    get(map, team).add(submission.getProblemId());
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
