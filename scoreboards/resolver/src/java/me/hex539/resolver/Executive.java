package me.hex539.resolver;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.hex539.resolver.cells.ProblemCell;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.api.ScoreboardModel;
import org.domjudge.proto.DomjudgeProto;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

// TODO: remove the dependency on a testing library; grow up and use a file URI instead.
import me.hex539.testing.utils.MockScoreboardModel;

public class Executive extends Application {
  public static void main(String[] args) {
    launch(args);
  }

  private ScoreboardModel model;

  @Override
  public void start(Stage stage) throws Exception {
    Map<String, String> args = getParameters().getNamed();
    String url = args.get("url");
    model = (url != null ? getModel(url) : MockScoreboardModel.example());

    StackPane root = new StackPane();

    VBox page = new VBox();
    root.getChildren().add(page);

    TableView table = new TableView<DomjudgeProto.ScoreboardRow>();
    table.getColumns().setAll(
        getColumn(String.class, "Team", (r -> model.getTeam(r.getTeam()).getName())),
        getColumn(Object.class, "Solved", (r -> r.getScore().getNumSolved())),
        getColumn(Object.class, "Time", (r -> r.getScore().getTotalTime()))
    );
    table.getColumns().addAll(model.getProblems().stream()
        .map(p -> getProblemColumn(p.getShortName(), model, p))
        .collect(Collectors.toList()));
    table.setItems(FXCollections.observableList(new ArrayList<>(model.getRows())));

    page.setVgrow(table, Priority.ALWAYS);
    page.getChildren().add(table);

    final Scene scene = new Scene(root);
    scene.getStylesheets().add("/resources/javafx/style.css");

    stage.setTitle(model.getContest().getName());
    stage.setMaximized(true);
    stage.setScene(scene);
    stage.show();
  }

  private static ScoreboardModel getModel(String url) throws Exception {
    System.err.println("Fetching from: " + url);

    DomjudgeRest api = new DomjudgeRest(url);
    final DomjudgeProto.Contest contest = api.getContest();
    final DomjudgeProto.Problem[] problems = api.getProblems(contest);
    final DomjudgeProto.Team[] teams = api.getTeams();
    final DomjudgeProto.ScoreboardRow[] rows = api.getScoreboard(contest);
    return new ScoreboardModel.Impl(contest, problems, teams, rows);
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
