package org.domjudge.api;

import org.domjudge.proto.DomjudgeProto.*;

final class Fallbacks {
  private Fallbacks() {}

  public static JudgementType[] judgementTypes() {
    return new JudgementType[] {
      jtb().setId(1).setLabel("CE").setName("Compile Error").setPenalty(false).build(),
      jtb().setId(2).setLabel("MLE").setName("Memory Limit").setPenalty(true).build(),
      jtb().setId(3).setLabel("OLE").setName("Output Limit").setPenalty(true).build(),
      jtb().setId(4).setLabel("RTE").setName("Runtime Error").setPenalty(true).build(),
      jtb().setId(5).setLabel("TLE").setName("Time Limit").setPenalty(true).build(),
      jtb().setId(6).setLabel("WA").setName("Wrong Answer").setPenalty(true).build(),
      jtb().setId(7).setLabel("PE").setName("Presentation Error").setPenalty(true).build(),
      jtb().setId(8).setLabel("NO").setName("No Output").setPenalty(true).build(),
      jtb().setId(9).setLabel("AC").setName("Correct").setPenalty(false).setSolved(true).build()
    };
  }

  private static JudgementType.Builder jtb() {
    return JudgementType.newBuilder();
  }
}
