package me.hex539.resolver.draw;

import edu.clics.proto.ClicsProto.*;

public interface AttemptColour {
  static final short[] SOLVED = {0, 175, 0};
  static final short[] WRONG = {207, 0, 0};
  static final short[] PENDING = {63, 63, 255};
  static final short[] JUDGING = {255, 255, 255};
  static final short[] BLANK = {12, 12, 12};

  static short[] of(ScoreboardProblem attempts) {
    if (attempts.getSolved()) return SOLVED;
    if (attempts.getNumPending() > 0) return PENDING;
    if (attempts.getNumJudged() > 0) return WRONG;
    return BLANK;
  }
}
