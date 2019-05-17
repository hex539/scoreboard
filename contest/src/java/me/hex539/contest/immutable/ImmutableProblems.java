package me.hex539.contest.immutable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.hex539.contest.model.Problems;

import edu.clics.proto.ClicsProto.*;

public class ImmutableProblems implements Problems {

  private final Map<String, Integer> indicesById;
  private final List<Problem> problemsById;
  private List<Problem> problemsByIndex;

  public static ImmutableProblems of(Problems copy) {
    if (copy instanceof ImmutableProblems) {
      return (ImmutableProblems) copy;
    }
    return new ImmutableProblems(copy);
  }

  public ImmutableProblems(Problems copy) {
    indicesById = IntStream.range(0, copy.getProblems().size())
        .collect(HashMap::new, (m, i) -> m.put(copy.getProblems().get(i).getId(), i), Map::putAll);
    problemsByIndex = SortedLists.sortBy(copy.getProblems(), Problem::getOrdinal);
    problemsById = SortedLists.sortBy(copy.getProblems(), Problem::getId);
  }

  @Override
  public List<Problem> getProblems() {
    return problemsByIndex;
  }

  @Override
  public Problem getProblem(String id) throws NoSuchElementException {
    return SortedLists.binarySearch(problemsById, p -> id.compareTo(p.getId())).get();
  }

  @Override
  public int getProblemIndex(String id) throws NoSuchElementException {
    return Optional.ofNullable(indicesById.get(id)).get();
  }
}
