package me.hex539.scoreboard;

import me.hex539.proto.domjudge.*;
import java.util.List;

public class ScoreboardModel {
  public static class Row {
    public static class Score {
      public long num_solved;
      public long total_time;
    }
    public static class Problem {
      public String label;
      public long num_judged;
      public long num_pending;
      public boolean solved;
      public long time;
    }

    public long rank;
    public long team;
    public Score score;
    public List<Problem> problems;

    public int getScore() {
      return 0;
    }

    public int getPenalty() {
      return 0;
    }

    public boolean hasSolved(Object problem) {
      return false;
    }

    public int getAttempts(Object problem) {
      return 0;
    }
  }
}
