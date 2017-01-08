package me.hex539.resolver;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXToolbar;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.api.ScoreboardModel;
import org.domjudge.proto.DomjudgeProto;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class Executive extends Application {
  public static void main(String[] args) {
    launch(args);
  }

  private ScoreboardModel getModel(String url) throws Exception {
    System.err.println("Fetching from: " + url);

    DomjudgeRest api = new DomjudgeRest(url);
    final DomjudgeProto.Contest contest = api.getContest();
    final DomjudgeProto.Problem[] problems = api.getProblems(contest);
    final DomjudgeProto.Team[] teams = api.getTeams();
    final DomjudgeProto.ScoreboardRow[] rows = api.getScoreboard(contest);
    return new ScoreboardModel.Impl(contest, problems, teams, rows);
  }

  @Override
  public void start(Stage stage) throws Exception {
    Map<String, String> args = getParameters().getNamed();
    final String url = args.get("url");

    StackPane root = new StackPane();

    VBox page = new VBox(/* spacing */ 8);
    root.getChildren().add(page);

    ScoreboardModel model = (url != null ? getModel(url) : new MockModel());

    TableView table = new TableView<DomjudgeProto.ScoreboardRow>();

    ObservableList<DomjudgeProto.ScoreboardRow> rows = FXCollections.observableList(
        new ArrayList<>(model.getRows())
    );
    table.setItems(rows);

    table.getColumns().setAll(
        getColumn(String.class, "Team", (r -> model.getTeam(r.getTeam()).getName())),
        getColumn(Object.class, "Solved", (r -> r.getScore().getNumSolved())),
        getColumn(Object.class, "Time", (r -> r.getScore().getTotalTime()))
    );
    for (final DomjudgeProto.Problem problem : model.getProblems()) {
      table.getColumns().add(
        getProblemColumn(
            problem.getShortName(),
            row -> model.getAttempts(model.getTeam(row.getTeam()), problem)));
    }

    VBox.setVgrow(table, Priority.ALWAYS);
    page.getChildren().add(table);

    final Scene scene = new Scene(root);
    scene.getStylesheets().add("/resources/javafx/style.css");

    stage.setTitle(model.getContest().getName());
    stage.setMaximized(true);
    stage.setScene(scene);
    stage.show();
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

  private static TableColumn<DomjudgeProto.ScoreboardRow, DomjudgeProto.ScoreboardProblem> getProblemColumn(
      final String title,
      final Function<DomjudgeProto.ScoreboardRow, DomjudgeProto.ScoreboardProblem> f) {
    TableColumn<DomjudgeProto.ScoreboardRow, DomjudgeProto.ScoreboardProblem> res =
        getColumn(DomjudgeProto.ScoreboardProblem.class, title, f);
    res.setCellFactory(p -> {
        return new TableCell<DomjudgeProto.ScoreboardRow, DomjudgeProto.ScoreboardProblem>() {
          @Override
          public void updateItem(DomjudgeProto.ScoreboardProblem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
              setText(null);
              return;
            }

            String text = "";
            long failures = item.getNumJudged();
            if (item.getSolved()) {
              failures--;
              text = "+";
            } else if (failures > 0) {
              text = "-";
            }
            setText(text + (failures > 0 ? Long.toString(failures) : ""));

            Set<String> add = new HashSet<>();
            Set<String> del = new HashSet<>();
            (item.getSolved() == true ? add : del).add("problem-solved");
            (item.getSolved() == false && failures > 0 ? add : del).add("problem-failed");
            (!add.isEmpty() ? add : del).add("problem");
            getStyleClass().removeAll(del);
            getStyleClass().addAll(add);
          }
        };
      });
    return res;
  }
}
