package me.hex539.resolver.cells;

import javafx.scene.control.TableCell;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.domjudge.proto.DomjudgeProto.ScoreboardRow;
import static org.domjudge.proto.DomjudgeProto.ScoreboardProblem;

public class ProblemCell extends TableCell<ScoreboardRow, ScoreboardProblem> {
  private static enum State {
    EMPTY,
    PENDING,
    FAILED,
    SOLVED
  };

  private static final Map<State, String[]> styleClasses = new HashMap<>();
  static {
    styleClasses.put(State.EMPTY, new String[0]);
    styleClasses.put(State.PENDING, new String[] {"problem", "problem-pending"});
    styleClasses.put(State.FAILED, new String[] {"problem", "problem-failed"});
    styleClasses.put(State.SOLVED, new String[] {"problem", "problem-solved"});
  }

  private static final Map<State, String> symbols = new HashMap<>();
  static {
    symbols.put(State.EMPTY, null);
    symbols.put(State.PENDING, "?");
    symbols.put(State.FAILED, "-");
    symbols.put(State.SOLVED, "+");
  }

  @Override
  public void updateItem(ScoreboardProblem item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      return;
    }

    State state = State.EMPTY;
    boolean appendFailures = true;

    long failures = item.getNumJudged();
    if (item.getSolved()) {
      failures--;
      state = State.SOLVED;
    } else if (item.getNumPending() > 0) {
      state = State.PENDING;
      appendFailures = false;
      failures--;
    } else if (failures > 0) {
      state = State.FAILED;
    }

    String text = symbols.get(state);
    if (appendFailures && failures > 0) {
      text += Long.toString(failures);
    }
    setText(text);

    Set<String> add = new HashSet<>();
    Set<String> del = new HashSet<>();
    for (String[] styles : styleClasses.values()) {
      del.addAll(Arrays.asList(styles));
    }
    add.addAll(Arrays.asList(styleClasses.get(state)));
    del.removeAll(add);

    getStyleClass().removeAll(del);
    getStyleClass().addAll(add);
  }
};
