package me.hex539.resolver;

import com.google.protobuf.TextFormat;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
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
import org.domjudge.scoreboard.ScoreboardModel;
import org.domjudge.scoreboard.ScoreboardModelImpl;
import org.domjudge.proto.DomjudgeProto;

public class Executive extends Application {
  private ScoreboardModel model;
  private ScoreboardView view;

  @Override
  public void start(Stage stage) throws Exception {
    Map<String, String> args = getParameters().getNamed();
    String url = args.get("url");
    String file = args.get("file");

    if (url != null) {
      model = getModel(url);
    } else if (file != null) {
      model = getLocalModel(file);
    } else {
      System.err.println("Need to supply a contest with either of --url or --file");
      System.exit(1);
    }

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
    return ScoreboardModelImpl.create(api);
  }

  private static ScoreboardModel getLocalModel(String path) throws IOException {
    System.err.println("Fetching from file: " + path);

    try (Reader is = new InputStreamReader(new FileInputStream(path))) {
      DomjudgeProto.EntireContest.Builder ecb = DomjudgeProto.EntireContest.newBuilder();
      TextFormat.merge(is, ecb);
      return ScoreboardModelImpl.create(ecb.build());
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
