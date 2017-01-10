package me.hex539.resolver;

import java.util.Map;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import me.hex539.resolver.cells.ProblemCell;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.api.ScoreboardModel;
import org.domjudge.proto.DomjudgeProto;

// TODO: remove the dependency on a testing library; grow up and use a file URI instead.
import me.hex539.testing.utils.MockScoreboardModel;

public class Executive extends Application {
  private ScoreboardModel model;
  private ScoreboardView view;

  @Override
  public void start(Stage stage) throws Exception {
    Map<String, String> args = getParameters().getNamed();
    String url = args.get("url");

    model = (url != null ? getModel(url) : MockScoreboardModel.example());

    final Parent root = FXMLLoader.load(
        getClass().getResource("/resources/javafx/scoreboard.fxml"));
    final Scene scene = new Scene(root);
    scene.getStylesheets().add("/resources/javafx/style.css");

    view = (ScoreboardView) root.lookup("#scoreboard");
    view.setModel(model);

    final VBox page = (VBox) root.lookup("#page");
    page.setVgrow(view, Priority.ALWAYS);

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

  public static void main(String[] args) {
    launch(args);
  }
}
