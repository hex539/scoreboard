package me.hex539.resolver;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ApiDetective;
import me.hex539.contest.ContestConfig;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import me.hex539.contest.ResolverController;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Executive {
  public static void main(String[] args) throws Exception {
    Invocation invocation = Invocation.parseFrom(args);

    final ContestConfig.Source source = getSource(invocation).orElseGet(() -> {
      System.exit(1);
      return null;
    });

    final Set<String> groups = invocation.getGroups() != null
        ? new HashSet<>(Arrays.asList(invocation.getGroups().split(",")))
        : null;
    final ClicsContest entireContest = new ContestDownloader(source).fetch();
    final ScoreboardModel reference =
        ScoreboardModelImpl.newBuilder(entireContest)
            .filterGroups(g -> groups != null
                ? groups.contains(g.getName())
                : !g.getHidden())
            .filterTooLateSubmissions()
            .build();
    final ScoreboardModelImpl model =
        ScoreboardModelImpl.newBuilder(entireContest, reference)
            .withEmptyScoreboard()
            .filterSubmissions(s -> false)
            .build();

    ResolverController resolver = new ResolverController(entireContest, reference);
    resolver.addObserver(model);

    new ResolverWindow(resolver, model).run();
    resolver.drain();
  }

  private static Optional<ContestConfig.Source> getSource(Invocation invocation) {
    final ContestConfig.Source source;
    if (invocation.getUrl() != null) {
      ContestConfig.Source.Builder sourceBuilder =
          ApiDetective.detectApi(invocation.getUrl()).get()
              .toBuilder();
      if (invocation.getUsername() != null) {
          sourceBuilder.setAuthentication(
              ContestConfig.Authentication.newBuilder()
                  .setHttpUsername(invocation.getUsername())
                  .setHttpPassword(invocation.getPassword())
                  .build());
      }
      return Optional.ofNullable(sourceBuilder.build());
    } else if (invocation.getFile() != null) {
      return Optional.ofNullable(
          ContestConfig.Source.newBuilder()
              .setFilePath(invocation.getFile())
              .build());
    } else {
      System.err.println("Need one of --url or --path to load a contest.");
      return Optional.empty();
    }
  }
}
