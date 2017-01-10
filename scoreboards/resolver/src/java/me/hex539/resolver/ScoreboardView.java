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

import org.domjudge.api.DomjudgeRest;
import org.domjudge.api.ScoreboardModel;
import org.domjudge.proto.DomjudgeProto;

public class ScoreboardView extends TableView<DomjudgeProto.ScoreboardRow> {
  public void setModel(ScoreboardModel model) {
    getColumns().setAll(
        getColumn(String.class, "Team", (r -> model.getTeam(r.getTeam()).getName())),
        getColumn(Object.class, "Solved", (r -> r.getScore().getNumSolved())),
        getColumn(Object.class, "Time", (r -> r.getScore().getTotalTime()))
    );
    getColumns().addAll(model.getProblems().stream()
        .map(p -> getProblemColumn(p.getShortName(), model, p))
        .collect(Collectors.toList()));
    setItems(FXCollections.observableList(new ArrayList<>(model.getRows())));
  }

  private static <T> TableColumn<DomjudgeProto.ScoreboardRow, T> getColumn(
      final Class<T> t,
      final String title,
      final Function<DomjudgeProto.ScoreboardRow, T> f) {
    final TableColumn<DomjudgeProto.ScoreboardRow, T> res =
        new TableColumn<DomjudgeProto.ScoreboardRow, T>() {{
            setCellValueFactory(features ->
              new ReadOnlyObjectWrapper(f.apply(features.getValue())));
        }};
    res.setText(title);
    res.setSortable(false);
    return res;
  }

  private static TableColumn<DomjudgeProto.ScoreboardRow, DomjudgeProto.ScoreboardProblem>
      getProblemColumn(
          final String title,
          final ScoreboardModel model,
          final DomjudgeProto.Problem problem) {
    TableColumn<DomjudgeProto.ScoreboardRow, DomjudgeProto.ScoreboardProblem> res = getColumn(
        DomjudgeProto.ScoreboardProblem.class,
        title,
        row -> model.getAttempts(model.getTeam(row.getTeam()), problem));
    res.setCellFactory(p -> new ProblemCell());
    return res;
  }
}
