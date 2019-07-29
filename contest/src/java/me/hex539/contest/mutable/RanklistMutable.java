package me.hex539.contest.mutable;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import me.hex539.contest.Comparators;
import me.hex539.contest.SplayList;
import me.hex539.contest.model.Problems;
import me.hex539.contest.model.Ranklist;
import me.hex539.contest.model.Teams;

import edu.clics.proto.ClicsProto.*;

@AutoValue
public abstract class RanklistMutable implements Ranklist, Ranklist.Observer, Teams.Observer {

  public abstract Problems getProblems();
  public abstract Teams getTeams();

  protected abstract SplayList<ScoreboardRow.Builder> getRealRows();

  private final Map<String, ScoreboardRow.Builder> rowsByTeam = new HashMap<>();
  private List<ScoreboardRow> rowsByIndex = null;

  public static Builder newBuilder() {
    return new AutoValue_RanklistMutable.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProblems(Problems problems);
    public abstract Builder setTeams(Teams teams);

    public abstract Problems getProblems();
    public abstract Teams getTeams();

    abstract Builder setRealRows(SplayList<ScoreboardRow.Builder> realRows);
    abstract Optional<SplayList<ScoreboardRow.Builder>> getRealRows();

    abstract RanklistMutable autoBuild();

    private RanklistMutable buildInner() {
      RanklistMutable res = autoBuild();
      res.getRealRows().forEach(row -> res.rowsByTeam.put(row.getTeamId(), row));
      getTeams().getTeams().forEach(res::onTeamAdded);
      return res;
    }

    public RanklistMutable build() {
      if (!getRealRows().isPresent()) {
        setRealRows(new SplayList<>(new Comparators.RowComparator(getTeams())));
      }
      return buildInner();
    }

    public Builder setRows(List<ScoreboardRow> rows, Predicate<ScoreboardRow> filter) {
      return setRealRows(
          new SplayList<>(
              rows.stream()
                  .filter(filter)
                  .map(ScoreboardRow::toBuilder)
                  .collect(Collectors.toList()),
              new Comparators.RowComparator(getTeams())));
    }

    public RanklistMutable copyFrom(Ranklist src, Predicate<ScoreboardRow> filter) {
      return setRows(src.getRows(), filter).buildInner();
    }
  }

  @Override
  public List<ScoreboardRow> getRows() {
    if (rowsByIndex == null) {
      rowsByIndex = new ArrayList<>();
      int i = 0;
      for (ScoreboardRow.Builder row : getRealRows()) {
        rowsByIndex.add(fixRank(row, ++i));
      }
    }
    // TODO: make an immutable copy
    return rowsByIndex;
  }

  @Override
  public ScoreboardRow getRow(long index) throws NoSuchElementException {
    return rowsByIndex != null
        ? rowsByIndex.get((int) index)
        : fixRank(getRealRows().get((int) index), (int) index + 1);
  }

  @Override
  public ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return fixRank(getRowInternal(team));
  }

  private ScoreboardRow.Builder getRowInternal(Team team) throws NoSuchElementException {
    try {
      return Optional.ofNullable(rowsByTeam.get(team.getId())).get();
    } catch (NoSuchElementException e) {
      throw new NoSuchElementException("Team \"" + team.getId() + "\" does not exist.");
    }
  }

  @Override
  public long getRank(Team team) throws NoSuchElementException {
    return getRank(getRowInternal(team));
  }

  @Override
  public ScoreboardScore getScore(Team team) throws NoSuchElementException {
    return getRowInternal(team).getScore();
  }

  @Override
  public ScoreboardProblem getAttempts(Team team, Problem problem) throws NoSuchElementException {
    return getRowInternal(team).getProblems(getProblems().getProblemIndex(problem.getId()));
  }

  @Override
  public void onTeamAdded(Team team) {
    if (rowsByTeam.containsKey(team.getId())) {
      return;
    }
    final ScoreboardRow.Builder row = createEmptyScoreboardRow(0, team, getProblems());
    addRow(row);
  }

  @Override
  public void onTeamRemoved(Team team) {
    final ScoreboardRow.Builder row = getRowInternal(team);
    removeRow(row);
  }

  @Override
  public void onProblemScoreChanged(Team team, ScoreboardProblem upd) {
    final ScoreboardRow.Builder row = getRowInternal(team);

    final int idx = getProblems().getProblemIndex(upd.getProblemId());
    final ScoreboardProblem orig = row.getProblems(idx);

    // If solve status or time change, sort order can also change.
    final boolean resort = upd.getSolved() != orig.getSolved() || upd.getTime() != orig.getTime();

    if (resort) removeRow(row);
    row.setProblems(idx, upd);
    if (resort) addRow(row);
  }

  @Override
  public void onScoreChanged(Team team, ScoreboardScore score) {
    final ScoreboardRow.Builder row = getRowInternal(team);
    if (row != null && score.equals(row.getScore())) {
      return;
    }
    removeRow(row);
    row.setScore(score);
    addRow(row);
  }

  @Override
  public void onTeamRankChanged(Team team, int oldRank, int newRank) {
    // Already handled by onScoreChanged.
  }

  /**
   * Find the real rank of a team before returning it.
   *
   * Internal representation doesn't care about rank, just relative order. This is because
   * keeping track of rank after scoreboard changes is an O(N) operation that can sometimes
   * involve rebuilding the whole scoreboard.
   *
   * To keep clients happy we find the real rank of the team and set it on a copy before
   * handing it out. The updated row isn't saved anywhere because we don't need it.
   */
  private ScoreboardRow fixRank(ScoreboardRow.Builder row) {
    return fixRank(row, getRank(row));
  }

  private ScoreboardRow fixRank(ScoreboardRow.Builder row, int realRank) {
    final ScoreboardRow.Builder existingRow = rowsByTeam.get(row.getTeamId());
    if (existingRow == null) {
      throw new NoSuchElementException("Team has no scoreboard row: " + row.getTeamId());
    } else if (existingRow != row) {
      throw new AssertionError("Invalid scoreboard row reference for team: " + row.getTeamId());
    }
    return (realRank == row.getRank() ? row : row.setRank(realRank)).build();
  }

  private int getRank(ScoreboardRow.Builder row) {
    int realRank = getRealRows().indexOf(row) + 1;
    if (realRank == 0) {
      if (rowsByTeam.get(row.getTeamId()) == null) {
        throw new NoSuchElementException("Team has no scoreboard row: " + row.getTeamId());
      } else {
        throw new AssertionError("Invalid scoreboard row reference for team: " + row.getTeamId()
            + "\n" + row.toString().replaceAll("\n", " ")
            + "\n" + rowsByTeam.get(row.getTeamId()).toString().replaceAll("\n", " "));
      }
    }
    return realRank;
  }

  private void removeRow(ScoreboardRow.Builder row) {
    rowsByIndex = null;
    rowsByTeam.remove(row.getTeamId());
    getRealRows().remove(row);
  }

  private void addRow(ScoreboardRow.Builder row) {
    rowsByIndex = null;
    rowsByTeam.put(row.getTeamId(), row);
    getRealRows().add(row);
  }

  private static ScoreboardRow.Builder createEmptyScoreboardRow(long rank, Team team, Problems pm) {
    return ScoreboardRow.newBuilder()
        .setRank(rank)
        .setTeamId(team.getId())
        .setScore(ScoreboardScore.newBuilder()
            .setNumSolved(0)
            .setTotalTime(0)
            .build())
        .addAllProblems(pm.getProblems().stream()
            .map(p -> ScoreboardProblem.newBuilder().setProblemId(p.getId()).build())
            .collect(Collectors.toList()));
  }
}
