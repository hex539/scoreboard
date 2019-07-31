package me.hex539.contest.immutable;

import java.util.List;
import java.util.NoSuchElementException;
import me.hex539.contest.model.Ranklist;

import edu.clics.proto.ClicsProto.*;

public class ImmutableRanklist implements Ranklist {

  private final List<ScoreboardRow> rows;
  private final List<ScoreboardRow> rowsByTeamId;

  public static ImmutableRanklist of(Ranklist copy) {
    if (copy instanceof ImmutableRanklist) {
      return (ImmutableRanklist) copy;
    }
    return new ImmutableRanklist(copy);
  }

  ImmutableRanklist(Ranklist copy) {
    rows = copy.getRows();
    rowsByTeamId = SortedLists.sortBy(rows, ScoreboardRow::getTeamId);
  }

  @Override
  public List<ScoreboardRow> getRows() {
    return rows;
  }

  @Override
  public ScoreboardRow getRow(long index) throws NoSuchElementException {
    return getRows().get((int) index);
  }

  @Override
  public ScoreboardRow getRow(Team team) throws NoSuchElementException {
    final String id = team.getId();
    return SortedLists.binarySearch(rowsByTeamId, row -> id.compareTo(row.getTeamId())).get();
  }
}
