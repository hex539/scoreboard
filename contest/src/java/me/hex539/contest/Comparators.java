package me.hex539.contest;

import edu.clics.proto.ClicsProto.*;
import java.util.Comparator;

class Comparators {
  public static class TeamComparator implements Comparator<Team> {
    private final Teams contest;

    public TeamComparator(Teams contest) {
      this.contest = contest;
    }

    @Override
    public int compare(Team a, Team b) {
      if (a.getId().equals(b.getId())) {
        return 0;
      }

      int res = 0;
      if ((res = Boolean.compare(isVisible(a), isVisible(b))) != 0
          || (res = String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName())) != 0
          || (res = a.getName().compareTo(b.getName())) != 0) {
        return res;
      }
      return 0;
    }

    protected boolean isVisible(Team t) {
      return t.getGroupIdsList().stream()
          .map(contest::getGroup).filter(Group::getHidden).count() == 0;
    }
  }

  public static class RowComparator implements Comparator<ScoreboardRowOrBuilder> {
    private final Teams contest;
    private final TeamComparator teamComparator;

    public RowComparator(Teams contest) {
      this.contest = contest;
      this.teamComparator = new TeamComparator(contest);
    }

    /**
     * Scoring algorithm (mostly) as described by official ICPC site.
     *
     * <a href="https://icpc.baylor.edu/worldfinals/rules#HScoringoftheFinals">Official rules</a>:
     * <ul>
     *   <li>Teams are ranked according to the most problems solved.
     *   <li>Teams placing in the first twelve places who solve the same number of problems are
     *       ranked:
     *     <ul>
     *       <li>first by least total time and, if need be,
     *       <li>by the earliest time of submittal of the last accepted run.
     *     </ul>
     *   <li>The total time is the sum of the time consumed for each problem solved.
     *   <li>The time consumed for a solved problem is the time elapsed from the beginning of the
     *       contest to the submittal of the first accepted run, plus 20 penalty minutes for every
     *       previously rejected run for that problem.
     *   <li>There is no time consumed for a problem that is not solved.
     * </ul>
     *
     * The part about top-12 is applied to all teams in the contest, not just the top-12. This
     * is because it is a ridiculous requirement for subregionals where overall rank matters even
     * for teams who don't get medals, on account of each university having its own quota and
     * possibly even its own filtered scoreboard.
     **/
    @Override
    public int compare(ScoreboardRowOrBuilder row1, ScoreboardRowOrBuilder row2) {
      final Team team1 = contest.getTeam(row1.getTeamId());
      final Team team2 = contest.getTeam(row2.getTeamId());

      if (team1.getId().equals(team2.getId())) {
       return 0;
      }

      int res = 0;
      if ((res = Boolean.compare(
          teamComparator.isVisible(team1),
          teamComparator.isVisible(team2))) != 0) {
        return res;
      }
      if ((res = Long.compare(
          -row1.getScore().getNumSolved(),
          -row2.getScore().getNumSolved())) != 0) {
        return res;
      }
      if ((res = Long.compare(
          row1.getScore().getTotalTime(),
          row2.getScore().getTotalTime())) != 0) {
        return res;
      }

      long[] ourSolve = getSolvedTimes(row1);
      long[] theirSolve = getSolvedTimes(row2);
      for (int i = Math.min(ourSolve.length, theirSolve.length); i --> 0;) {
        if (ourSolve[i] != theirSolve[i]) {
          return Long.compare(ourSolve[i], theirSolve[i]);
        }
      }
      return teamComparator.compare(team1, team2);
    }

    private static long[] getSolvedTimes(ScoreboardRowOrBuilder row) {
      return row.getProblemsList().stream()
          .filter(ScoreboardProblem::getSolved)
          .mapToLong(ScoreboardProblem::getTime)
          .sorted()
          .toArray();
    }
  }
}
