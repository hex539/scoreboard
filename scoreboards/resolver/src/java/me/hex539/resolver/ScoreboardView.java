package me.hex539.resolver;

import javafx.scene.control.TableCell;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import me.hex539.resolver.cells.ProblemCell;

import me.hex539.contest.ScoreboardModel;
import edu.clics.proto.ClicsProto;

public class ScoreboardView extends TableView<ClicsProto.ScoreboardRow>
    implements ScoreboardModel.Observer {

  @Override
  public void setModel(ScoreboardModel model) {
    getColumns().setAll(
        getColumn(String.class, "Team", (r -> model.getTeam(r.getTeamId()).getName())),
        getColumn(Object.class, "Solved", (r -> r.getScore().getNumSolved())),
        getColumn(Object.class, "Time", (r -> r.getScore().getTotalTime()))
    );
    getColumns().addAll(model.getProblems().stream()
        .map(p -> getProblemColumn(p.getLabel(), model, p))
        .collect(Collectors.toList()));
    setItems(FXCollections.observableList(new ArrayList<>(model.getRows())));
  }

  @Override
  public void onProblemScoreChanged(ClicsProto.Team team, ClicsProto.ScoreboardProblem attempt) {
    refresh();
  }

  @Override
  public void onScoreChanged(ClicsProto.Team team, ClicsProto.ScoreboardScore score) {
    refresh();
  }

  private static <T> TableColumn<ClicsProto.ScoreboardRow, T> getColumn(
      final Class<T> t,
      final String title,
      final Function<ClicsProto.ScoreboardRow, T> f) {
    final TableColumn<ClicsProto.ScoreboardRow, T> res =
        new TableColumn<ClicsProto.ScoreboardRow, T>() {{
            setCellValueFactory(features ->
              new ReadOnlyObjectWrapper(f.apply(features.getValue())));
        }};
    res.setText(title);
    res.setSortable(false);
    return res;
  }

  private static TableColumn<ClicsProto.ScoreboardRow, ClicsProto.ScoreboardProblem>
      getProblemColumn(
          final String title,
          final ScoreboardModel model,
          final ClicsProto.Problem problem) {
    TableColumn<ClicsProto.ScoreboardRow, ClicsProto.ScoreboardProblem> res = getColumn(
        ClicsProto.ScoreboardProblem.class,
        title,
        row -> model.getAttempts(model.getTeam(row.getTeamId()), problem));
    res.setCellFactory(p -> new ProblemCell());
    return res;
  }
}
