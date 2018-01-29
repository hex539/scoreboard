package me.hex539.resolver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import me.hex539.contest.ApiDetective;
import me.hex539.contest.ContestConfig;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import me.hex539.resolver.cells.ProblemCell;

public class Executive extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    Map<String, String> args = getParameters().getNamed();
    Set<String> flags = new HashSet<>(getParameters().getUnnamed());

    if (args.get("url") == null && args.get("file") == null) {
      System.err.println("Need to supply a contest with either of --url= or --file=");
      System.exit(1);
    }

    final ContestConfig.Source source;
    if (args.containsKey("url")) {
      source = ApiDetective.detectApi(args.get("url")).get().toBuilder()
          .setAuthentication(args.containsKey("username")
                ? ContestConfig.Authentication.newBuilder()
                    .setHttpUsername(args.get("username"))
                    .setHttpPassword(args.get("password"))
                    .build()
                : null)
          .build();
    } else if (args.containsKey("file")) {
      source = ContestConfig.Source.newBuilder().setFilePath(args.get("file")).build();
    } else {
      System.err.println("Need one of --url or --file to load a contest");
      System.exit(1);
      return;
    }

    final Set<String> groups = args.containsKey("groups")
        ? new HashSet<>(Arrays.asList(args.get("groups").split(",")))
        : null;
    final ScoreboardModel model =
        ScoreboardModelImpl.newBuilder(new ContestDownloader(source).fetch())
            .filterGroups(g -> groups == null || groups.contains(g.getName()))
            .build();

    final Parent root = FXMLLoader.load(
        getClass().getResource("/resources/javafx/scoreboard.fxml"));
    final Scene scene = new Scene(root);
    scene.getStylesheets().add("/resources/javafx/style.css");

    final ScoreboardView view = (ScoreboardView) root.lookup("#scoreboard");
    view.setModel(model);

    final VBox page = (VBox) root.lookup("#page");
    page.setVgrow(view, Priority.ALWAYS);

    stage.setTitle(model.getContest().getName());
    stage.setMaximized(true);
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
