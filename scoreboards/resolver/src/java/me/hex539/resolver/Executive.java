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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.api.ScoreboardModel;
import org.domjudge.proto.DomjudgeProto;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.function.Function;

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

  /**
   * TODO: Move this into tests for the scoreboard lib. Possibly expose a convenience
   *       library too, with a syntax along the lines of:
   *       <code>
   *       ScoreboardMocks.FakeModel.newBuilder()
   *            .setProblems(     "A",    "B",     "C",     "D")
   *            .addRow("Team 1", SOLVED, SOLVED,  PENDING, FAILED)
   *            .addRow("Team 2", SOLVED, PENDING, PENDING, FAILED)
   *            .addRow("Team 3", FAILED, null,    null,    PENDING)
   *            .build()
   *       </code>
   */
  private ScoreboardModel getMockModel() {
    return new ScoreboardModel() {
      @Override
      public DomjudgeProto.Contest getContest() {
        return DomjudgeProto.Contest.newBuilder()
            .setId(44)
            .setShortName("Trial")
            .setName("Challenge")
            .build();
      }

      @Override
      public Collection<DomjudgeProto.Team> getTeams() {
        /**
         * Some tricky cases: Unicode 9.0, 6.0, and 1.1 respectively.
         */
        return Arrays.asList(new DomjudgeProto.Team[] {
            DomjudgeProto.Team.newBuilder().setId(1).setName("Bath Ducks 🦆").build(),
            DomjudgeProto.Team.newBuilder().setId(2).setName("Bath Crocs 🐊").build(),
            DomjudgeProto.Team.newBuilder().setId(3).setName("Bath Shower ☂").build()
        });
      }

      @Override
      public Collection<DomjudgeProto.Problem> getProblems() {
        return Arrays.asList(new DomjudgeProto.Problem[] {
          DomjudgeProto.Problem.newBuilder()
              .setLabel("X")
              .setName("Example problem")
              .setShortName("Example")
              .build()
        });
      }

      @Override
      public Collection<DomjudgeProto.ScoreboardRow> getRows() {
        return Arrays.asList(new DomjudgeProto.ScoreboardRow[] {
          DomjudgeProto.ScoreboardRow.newBuilder()
              .setTeam(1)
              .setRank(1)
              .setScore(DomjudgeProto.ScoreboardScore.newBuilder()
                  .setNumSolved(1)
                  .setTotalTime(23)
                  .build())
              .addProblems(DomjudgeProto.ScoreboardProblem.newBuilder()
                    .setLabel("X")
                    .setSolved(true)
                    .setNumJudged(1)
                    .build())
              .build(),
          DomjudgeProto.ScoreboardRow.newBuilder()
              .setTeam(2)
              .setRank(2)
              .setScore(DomjudgeProto.ScoreboardScore.newBuilder()
                  .setNumSolved(1)
                  .setTotalTime(500)
                  .build())
              .addProblems(DomjudgeProto.ScoreboardProblem.newBuilder()
                    .setLabel("X")
                    .setSolved(true)
                    .setNumJudged(1)
                    .build())
              .build(),
          DomjudgeProto.ScoreboardRow.newBuilder()
              .setTeam(3)
              .setRank(3)
              .setScore(DomjudgeProto.ScoreboardScore.newBuilder()
                  .setNumSolved(0)
                  .setTotalTime(0)
                  .build())
              .addProblems(DomjudgeProto.ScoreboardProblem.newBuilder()
                    .setLabel("X")
                    .setSolved(true)
                    .setNumJudged(1)
                    .build())
              .build()
        });
      }
    };
  }

  @Override
  public void start(Stage stage) throws Exception {
    Map<String, String> args = getParameters().getNamed();
    final String url = args.get("url");

    StackPane root = new StackPane();

    VBox page = new VBox(/* spacing */ 8);
    root.getChildren().add(page);

    ScoreboardModel model = (url != null ? getModel(url) : getMockModel());

    TableView table = new TableView<DomjudgeProto.ScoreboardRow>();
    table.setStyle("-fx-font-size: 20");

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
        getColumn(
            Object.class,
            problem.getShortName(),
            (row -> {
              DomjudgeProto.Team team = model.getTeam(row.getTeam());
              DomjudgeProto.ScoreboardProblem attempts = model.getAttempts(team, problem);
              return attempts.getSolved() ? "+"
                  : attempts.getNumJudged() > 0 ? "-"
                  : "";})));
    }

    VBox.setVgrow(table, Priority.ALWAYS);
    page.getChildren().add(table);

    stage.setTitle(model.getContest().getName());
    stage.setScene(new Scene(root));
    stage.show();
  }

  /** JavaFX is an aberration of nature. The depressing thing is there are not many
   *  superior UX toolkits in this language, where 'superior' involves having
   *  proper unicode support and some degree of cross-platform-ness.
   */
  private static <T> TableColumn<DomjudgeProto.ScoreboardRow, T> getColumn(
      final Class<T> t,
      final String text,
      final Function<DomjudgeProto.ScoreboardRow, T> f) {
    final TableColumn<DomjudgeProto.ScoreboardRow, T> res =
        new TableColumn<DomjudgeProto.ScoreboardRow, T>() {{
            setCellValueFactory(features ->
              new ReadOnlyObjectWrapper(f.apply(features.getValue())));
        }};
    res.setText(text);
    res.setSortable(false);
    return res;
  }
}

interface ScoreboardView {
  public interface Model {
    DomjudgeProto.Team getTeam(long id);
    DomjudgeProto.ScoreboardRow[] getRows();
  }
}
