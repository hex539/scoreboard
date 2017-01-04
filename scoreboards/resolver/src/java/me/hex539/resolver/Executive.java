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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.hex539.proto.domjudge.DomjudgeProto;
import me.hex539.scoreboard.DomjudgeRest;

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

  private ScoreboardView.Model getModel(String url) throws Exception {
    System.err.println("Fetching from: " + url);

    DomjudgeRest api = new DomjudgeRest(url);
    final DomjudgeProto.Team[] teams = api.getTeams();
    final DomjudgeProto.ScoreboardRow[] scoreboard = api.getScoreboard(5);

    final Map<Long, DomjudgeProto.Team> teamMap = new HashMap<>();
    for (DomjudgeProto.Team team : teams) {
      teamMap.put(team.getId(), team);
    }

    return new ScoreboardView.Model() {
      @Override
      public DomjudgeProto.Team getTeam(long id) {
        return teamMap.get(id);
      }

      @Override
      public DomjudgeProto.ScoreboardRow[] getRows() {
        return scoreboard;
      }
    };
  }

  /**
   * TODO: Move this into the scoreboard lib and start creating a
   *       single ScoreboardView.Model for every different renderer
   *       to use.
   */
  private ScoreboardView.Model getMockModel() {
    return new ScoreboardView.Model() {
      @Override
      public DomjudgeProto.Team getTeam(long id) {
        return DomjudgeProto.Team.newBuilder()
            .setName(
                id == 1 ? "Bath Ducks 🦆" : /* Unicode 9.0 */
                id == 2 ? "Bath Crocs 🐊":/* Unicode 6.0 */
                id == 3 ? "Bath Shower ☂" : /* Unicode 1.1 */
                          "team_" + id)
            .build();
      }

      @Override
      public DomjudgeProto.ScoreboardRow[] getRows() {
        return new DomjudgeProto.ScoreboardRow[] {
          DomjudgeProto.ScoreboardRow.newBuilder()
              .setTeam(1)
              .setRank(1)
              .setScore((DomjudgeProto.ScoreboardScore.newBuilder()
                  .setNumSolved(1)
                  .setTotalTime(23)
                  .build()))
              .build(),
          DomjudgeProto.ScoreboardRow.newBuilder()
              .setTeam(2)
              .setRank(2)
              .setScore((DomjudgeProto.ScoreboardScore.newBuilder()
                  .setNumSolved(1)
                  .setTotalTime(500)
                  .build()))
              .build(),
          DomjudgeProto.ScoreboardRow.newBuilder()
              .setTeam(3)
              .setRank(3)
              .setScore((DomjudgeProto.ScoreboardScore.newBuilder()
                  .setNumSolved(0)
                  .setTotalTime(0)
                  .build()))
              .build()
        };
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

    ScoreboardView.Model model = (url != null ? getModel(url) : getMockModel());

    TableView table = new TableView<DomjudgeProto.ScoreboardRow>();
    table.setStyle("-fx-font-size: 20");

    ObservableList<DomjudgeProto.ScoreboardRow> rows = FXCollections.observableList(
        Arrays.asList(model.getRows())
    );
    table.setItems(rows);

    table.getColumns().setAll(
        getColumn(String.class, "Team", (r -> model.getTeam(r.getTeam()).getName())),
        getColumn(Object.class, "Solved", (r -> r.getScore().getNumSolved())),
        getColumn(Object.class, "Time", (r -> r.getScore().getTotalTime()))
    );
    for (int i = 0; i < model.getRows()[0].getProblemsCount(); i++) {
      final int x = i;
      table.getColumns().add(
        getColumn(
            Object.class,
            model.getRows()[0].getProblemsList().get(x).getLabel(),
            (r ->
                r.getProblemsList().get(x).getSolved() ? "+" :
                r.getProblemsList().get(x).getNumJudged() > 0 ? "-" : "")));
    }

    VBox.setVgrow(table, Priority.ALWAYS);
    page.getChildren().add(table);

    stage.setTitle("Resolver View");
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
