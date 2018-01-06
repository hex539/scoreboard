package org.domjudge.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.domjudge.proto.DomjudgeProto.JudgementType;

/**
 * Mapping of {@code Judging.getOutcome()} to {@code JudgementType.getLabel()} on DOMjudge
 * according to out-of-the-box default config.
 *
 * TODO: this is a hack because api/v3 judgement_types doesn't correspond to the outcome field
 * in judgements. find out why not and how this is supposed to work (if it's supposed to work
 * at all and clients don't just have to try and guess what the mapping is).
 */
abstract class DefaultJudgementTypes {
  public static final Map<String, JudgementType> get() {
    if (defaultJudgementTypes == null) {
      defaultJudgementTypes = createJudgementTypes();
    }
    return defaultJudgementTypes;
  }

  private static Map<String, JudgementType> defaultJudgementTypes = null;

  private static final Map<String, JudgementType> createJudgementTypes() {
    final Map<String, JudgementType> types = new HashMap<>();
    types.put(
        "correct",
        JudgementType.newBuilder()
            .setId(1).setLabel("AC").setName("correct").setSolved(true).build());
    types.put(
        "wrong-answer",
        JudgementType.newBuilder()
            .setId(2).setLabel("WA").setName("wrong answer").setPenalty(true).build());
    types.put(
        "timelimit",
        JudgementType.newBuilder()
            .setId(3).setLabel("TLE").setName("time limit exceeded").setPenalty(true).build());
    types.put(
        "run-error",
        JudgementType.newBuilder()
            .setId(4).setLabel("RTE").setName("run time error").setPenalty(true).build());
    types.put(
        "output-limit",
        JudgementType.newBuilder()
            .setId(5).setLabel("OLE").setName("output limit exceeded").setPenalty(true).build());
    types.put(
        "memory-limit",
        JudgementType.newBuilder()
            .setId(6).setLabel("MLE").setName("memory limit exceeded").setPenalty(true).build());
    types.put(
        "compiler-error",
        JudgementType.newBuilder()
            .setId(7).setLabel("CE").setName("compile error").build());
    return Collections.unmodifiableMap(types);
  }
}
