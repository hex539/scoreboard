package me.hex539.contest.model;

import edu.clics.proto.ClicsProto.*;
import java.util.List;
import java.util.NoSuchElementException;

public interface Problems {
  List<Problem> getProblems();

  default Problem getProblem(String id) throws NoSuchElementException {
    return getProblems().stream().filter(x -> id.equals(x.getId())).findFirst().get();
  }

  default int getProblemIndex(String id) throws NoSuchElementException {
    return getProblems().indexOf(getProblem(id));
  }

  default boolean containsProblem(String id) {
    return getProblems().stream().filter(x -> id.equals(x.getId())).findFirst().isPresent();
  }

  default int getProblemsCount() {
    return getProblems().size();
  }
}
