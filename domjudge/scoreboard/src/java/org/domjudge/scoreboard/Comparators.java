package org.domjudge.scoreboard;

import static org.domjudge.proto.DomjudgeProto.*;

class Comparators {
  public static int compareTeams(Team a, Team b) {
    if (a.getCategory() != b.getCategory()) {
      // TODO: use DomjudgeProto.Category.sortOrder
      return -Long.compare(a.getCategory(), b.getCategory());
    }
    return a.getName().compareTo(b.getName());
  }

  public static int compareRows(Team team1, ScoreboardRow row1, Team team2, ScoreboardRow row2) {
    if (team1.getId() == team2.getId()) {
      return 0;
    }
    if (team1.getCategory() != team2.getCategory()) {
      return compareTeams(team1, team2);
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

    long[] ourSolve = row1.getProblemsList().stream()
        .filter(ScoreboardProblem::getSolved)
        .mapToLong(ScoreboardProblem::getTime)
        .sorted()
        .toArray();
    long[] theirSolve = row2.getProblemsList().stream()
        .filter(ScoreboardProblem::getSolved)
        .mapToLong(ScoreboardProblem::getTime)
        .sorted()
        .toArray();
    for (int i = ourSolve.length; i --> 0;) {
      if (ourSolve[i] != theirSolve[i]) {
        return ourSolve[i] < theirSolve[i] ? -1 : +1;
      }
    }
    return compareTeams(team1, team2);
  }
}
