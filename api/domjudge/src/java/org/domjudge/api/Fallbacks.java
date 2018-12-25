package org.domjudge.api;

import com.google.protobuf.Int64Value;
import org.domjudge.proto.DomjudgeProto.*;

final class Fallbacks {
  private Fallbacks() {}

  public static JudgementType[] judgementTypes() {
    return new JudgementType[] {
      judgementType(1, "CE", "Compile Error", false, false),
      judgementType(2, "MLE", "Memory Limit", true, false),
      judgementType(3, "OLE", "Output Limit", true, false),
      judgementType(4, "RTE", "Runtime Error", true, false),
      judgementType(5, "TLE", "Time Limit", true, false),
      judgementType(6, "WA", "Wrong Answer", true, false),
      judgementType(7, "PE", "Presentation Error", true, false),
      judgementType(8, "NO", "No Output", true, false),
      judgementType(9, "AC", "Correct", false, true)
    };
  }

  private static JudgementType judgementType(
      long id,
      String label,
      String name,
      boolean penalty,
      boolean solved) {
    return JudgementType.newBuilder()
        .setId(Int64Value.newBuilder().setValue(id))
        .setLabel(label)
        .setName(name)
        .setPenalty(penalty)
        .setSolved(solved)
        .build();
  }
}
