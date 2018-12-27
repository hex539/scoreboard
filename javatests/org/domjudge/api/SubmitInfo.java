package org.domjudge.api;

import static com.google.common.truth.Truth.assertThat;

import com.google.protobuf.Duration;
import edu.clics.proto.ClicsProto.*;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.JudgementDispatcher;

class SubmitInfo {
  final JudgementDispatcher d;
  final ScoreboardModel m;
  final Submission s;

  private static int nextSubmission = 1112;

  public SubmitInfo(JudgementDispatcher d, ScoreboardModel m, Submission s) {
    this.d = d;
    this.m = m;
    this.s = s;
  }

  public SubmitInfo submit() {
    d.notifySubmission(s);
    return this;
  }

  public SubmitInfo judge(String verdict) {
    assertThat(d.notifyJudgement(
        Judgement.newBuilder()
            .setId("j" + s.getId().substring(1))
            .setSubmissionId(s.getId())
            .setJudgementTypeId(verdict)
            .setStartContestTime(Duration.newBuilder().setSeconds(123456).build())
            .setEndContestTime(Duration.newBuilder().setSeconds(123456).build())
            .build()))
        .isNotNull();
    return this;
  }

  public static SubmitInfo submission(
      JudgementDispatcher disp,
      ScoreboardModel model,
      String id) {
    return new SubmitInfo(disp, model, model.getSubmission(id));
  }

  public static SubmitInfo submission(
      JudgementDispatcher disp,
      ScoreboardModel model,
      String team,
      String problem,
      int minutes) {
    return new SubmitInfo(disp, model, Submission.newBuilder()
        .setId("s" + Integer.toString(nextSubmission++))
        .setTeamId(team)
        .setProblemId(problem)
        .setLanguageId("Parseltongue")
        .setContestTime(Duration.newBuilder().setSeconds(minutes * 60 + 31).build())
        .build());
  }
}
