package me.hex539.testing.utils;

import com.google.protobuf.Duration;
import edu.clics.proto.ClicsProto.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.hex539.contest.ScoreboardModel;

public final class MockScoreboardModel {
  public static ScoreboardModel example() {
    return new Builder()
        .setProblems("Apricot", "Bamboo", "Coconut", "Durian")
        .addRow("Bath Ducks 🦆", "+", "+",  "+",  "+")
        .addRow("Bath Crocs 🐊", "+", " ",  "+4", "?")
        .addRow("Bath Shower ☂", " ", "-1", "+2", "?1")
        .build();
  }

  public static class Builder {
    private final List<Problem> problems = new ArrayList<>();
    private final List<ScoreboardRow> rows = new ArrayList<>();
    private final List<Team> teams = new ArrayList<>();
    private final List<Organization> organizations = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private final List<Submission> submissions = new ArrayList<>();
    private final List<Judgement> judgements = new ArrayList<>();

    private static final int SUBMISSION_TIME = 100;

    public ScoreboardModel build() {
      return new ScoreboardModel() {
        @Override
        public Contest getContest() {
          return Contest.newBuilder().setFormalName("FakeContest").build();
        }

        @Override
        public Collection<Team> getTeams() {
          return teams;
        }

        @Override
        public List<Problem> getProblems() {
          return problems;
        }

        @Override
        public List<ScoreboardRow> getRows() {
          return rows;
        }

        @Override
        public Collection<Organization> getOrganizations() {
          return organizations;
        }

        @Override
        public Collection<Group> getGroups() {
          return groups;
        }

        @Override
        public List<Submission> getSubmissions() {
          return submissions;
        }

        @Override
        public List<Judgement> getJudgements() {
          return judgements;
        }
      };
    }

    public Builder setProblems(final String... names) {
      problems.addAll(IntStream.range(0, names.length)
          .mapToObj(i -> Problem.newBuilder()
              .setId(names[i])
              .setOrdinal(i)
              .setName(names[i])
              .setLabel(names[i].substring(0, 1))
              .build())
          .collect(Collectors.toList()));
      return this;
    }

    public Builder addRow(final String teamName) {
      final Random rand = new Random();
      final String[] attempts = IntStream.range(0, problems.size())
          .mapToObj(i ->
              String.valueOf(" +-?".charAt(rand.nextInt(4)))
              + String.valueOf("0123".charAt(rand.nextInt(4))))
          .map(s -> (s.endsWith("0") ? s.startsWith("+")
              ? s.substring(0, 1)
              : s.substring(0, 1) + "1" : s))
          .map(s -> (s.startsWith(" ") ? " " : s))
          .map(s -> (s.equals("-") ? "-1" : s))
          .toArray(length -> new String[length]);
      return addRow(teamName, attempts);
    }

    public Builder addRow(final String teamName, final String... attempts) {
      if (teams.size() == 0) {
        organizations.add(Organization.newBuilder()
            .setId("org")
            .setName("Fake Organization")
            .build());
        groups.add(Group.newBuilder()
            .setId("teams")
            .setName("Fake Group")
            .build());
      }
      final Team team = Team.newBuilder()
          .setId(teamName)
          .setName(teamName)
          .setOrganizationId(organizations.get(0).getId())
          .addGroupIds(groups.get(0).getId())
          .build();
      teams.add(team);

      final List<ScoreboardProblem> cols = IntStream.range(0, attempts.length)
          .mapToObj(i -> ScoreboardProblem.newBuilder()
              .setProblemId(problems.get(i).getId())
              .setSolved(attempts[i].startsWith("+"))
              .setTime(attempts[i].startsWith("+") ? SUBMISSION_TIME : 0)
              .setNumJudged(
                  (attempts[i].startsWith("+") || attempts[i].startsWith("?") ? 1 : 0)
                  + (attempts[i].length() > 1 ? Integer.parseInt(attempts[i].substring(1)) : 0))
              .setNumPending(attempts[i].startsWith("?") ? 1 : 0)
              .build())
          .collect(Collectors.toList());

      cols.stream()
          .forEach(col -> {
              IntStream.range(0, (col.getSolved() ? 1 : 0) * -1 + col.getNumJudged())
                  .forEach(i -> addSubmission(teamName, col.getProblemId(), "wrong-answer"));
              IntStream.range(0, (col.getSolved() ? 1 : 0))
                  .forEach(i -> addSubmission(teamName, col.getProblemId(), "correct"));
          });

      rows.add(ScoreboardRow.newBuilder()
          .setTeamId(team.getId())
          .setRank(rows.size() + 1)
          .setScore(ScoreboardScore.newBuilder()
              .setNumSolved((int) cols.stream().filter(ScoreboardProblem::getSolved).count())
              .setTotalTime((int) cols.stream().mapToLong(c -> c.getSolved() ? c.getTime() : 0).sum())
              .build())
          .addAllProblems(cols)
          .build());

      return this;
    }

    private Submission addSubmission(final String teamName, final String problemId) {
      Submission res = Submission.newBuilder()
          .setId("ms" + Integer.toString(submissions.size() + 1))
          .setTeamId(teamName)
          .setProblemId(problemId)
          .setContestTime(Duration.newBuilder().setSeconds(SUBMISSION_TIME * 60))
          .setLanguageId("Parseltongue")
          .build();
      submissions.add(res);
      return res;
    }

    private Judgement addSubmission(final String teamName, final String problemId, String verdict) {
      Submission s = addSubmission(teamName, problemId);
      Judgement j = Judgement.newBuilder()
          .setId("mj" + Integer.toString(judgements.size() + 1))
          .setSubmissionId(s.getId())
          .setJudgementTypeId(verdict)
          .build();
      judgements.add(j);
      return j;
    }
  }
}
