package org.domjudge.scoreboard;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.domjudge.proto.DomjudgeProto.*;

class Comparators {
  public static class TeamComparator implements Comparator<Team> {
    private final Map<Long, Category> categoryMap;

    public TeamComparator(Collection<Category> categories) {
      categoryMap = categories.stream()
          .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    public int compare(Team a, Team b) {
      if (a.getId() == b.getId()) {
        return 0;
      }
      if (a.getCategory() != b.getCategory()) {
        return Long.compare(
            categoryMap.get(a.getCategory()).getSortOrder(),
            categoryMap.get(b.getCategory()).getSortOrder());
      }
      return a.getName().compareTo(b.getName());
    }
  }

  public static class RowComparator implements Comparator<ScoreboardRow> {
    private final Map<Long, Team> teamMap;
    private final TeamComparator teamComparator;

    public RowComparator(Collection<Team> teams, Collection<Category> categories) {
      this.teamMap = teams.stream().collect(Collectors.toMap(Team::getId, Function.identity()));
      this.teamComparator = new TeamComparator(categories);
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
    public int compare(ScoreboardRow row1, ScoreboardRow row2) {
      final Team team1 = teamMap.get(row1.getTeam());
      final Team team2 = teamMap.get(row2.getTeam());

      if (team1.getId() == team2.getId() || team1.getCategory() != team2.getCategory()) {
        return teamComparator.compare(team1, team2);
      }

      int res = 0;
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
      for (int i = ourSolve.length; i --> 0;) {
        if (ourSolve[i] != theirSolve[i]) {
          return Long.compare(ourSolve[i], theirSolve[i]);
        }
      }
      return teamComparator.compare(team1, team2);
    }

    private static long[] getSolvedTimes(ScoreboardRow row) {
      return row.getProblemsList().stream()
          .filter(ScoreboardProblem::getSolved)
          .mapToLong(ScoreboardProblem::getTime)
          .sorted()
          .toArray();
    }
  }
}
