package me.hex539.contest.mutable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.hex539.contest.model.Problems;

import edu.clics.proto.ClicsProto.*;

public class ProblemsMutable implements Problems {

  private final Map<String, Problem> problemsById = new HashMap<>();
  private Map<String, Integer> indicesById = null;
  private List<Problem> problemsByIndex = null;

  public ProblemsMutable() {
    return;
  }

  public ProblemsMutable(Problems copy) {
    this();
    copy.getProblems().forEach(this::onProblemAdded);
  }

  @Override
  public List<Problem> getProblems() {
    if (problemsByIndex == null) {
      problemsByIndex = problemsById.values().stream()
          .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
          .collect(Collectors.toList());
    }
    return problemsByIndex;
  }

  @Override
  public int getProblemsCount() {
    return problemsById.size();
  }

  @Override
  public Problem getProblem(String id) throws NoSuchElementException {
    return Optional.ofNullable(problemsById.get(id)).get();
  }

  @Override
  public int getProblemIndex(String id) throws NoSuchElementException {
    if (indicesById == null) {
      indicesById = IntStream.range(0, getProblems().size())
          .collect(HashMap::new, (m, i) -> m.put(getProblems().get(i).getId(), i), Map::putAll);
    }
    return Optional.ofNullable(indicesById.get(id)).get();
  }

  public void onProblemAdded(Problem problem) {
    onProblemRemoved(problem);
    if (problemsById.put(problem.getId(), problem) != null) {
      return;
    }
    indicesById = null;
    problemsByIndex = null;
  }

  public void onProblemRemoved(Problem problem) {
    if (problemsById.remove(problem.getId()) == null) {
      return;
    }
    indicesById = null;
    problemsByIndex = null;
  }
}
