package me.hex539.resolver;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ApiDetective;
import me.hex539.contest.ContestConfig;
import me.hex539.contest.ContestDownloader;
import me.hex539.contest.ImmutableScoreboardModel;
import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ScoreboardModelImpl;
import me.hex539.contest.ResolverController;
import me.hex539.resolver.draw.FontRenderer;

import static org.lwjgl.glfw.GLFW.*;

public class Executive {
  public static void main(String[] args) throws Exception {
    Invocation invocation = Invocation.parseFrom(args);

    final ContestConfig.Source source = getSource(invocation).orElseGet(() -> {
      System.exit(1);
      return null;
    });

    final CompletableFuture<ByteBuffer[]> fontData = CompletableFuture.supplyAsync(
        () -> new ByteBuffer[] {
            FontRenderer.mapResource(FontRenderer.FONT_NOTO_SANS),
            FontRenderer.mapResource(FontRenderer.FONT_NOTO_SANS_SYMBOLS),
            FontRenderer.mapResource(FontRenderer.FONT_SYMBOLA),
            FontRenderer.mapResource(FontRenderer.FONT_UNIFONT)
          });

    final CompletableFuture<ClicsContest> entireContest =
        CompletableFuture.supplyAsync(() -> {
          try {
            return new ContestDownloader(source).fetch();
          } catch (Exception e) {
            throw new CompletionException(e);
          }
        });

    final CompletableFuture<ScoreboardModel> reference = entireContest
        .thenApplyAsync(ScoreboardModelImpl::newBuilder)
        .thenApplyAsync(b -> b
            .filterTooLateSubmissions()
            .filterGroups(getGroupFilter(invocation))
            .build())
        .thenApplyAsync(ImmutableScoreboardModel::of);

    final CompletableFuture<ScoreboardModelImpl> model = entireContest
        .thenCombineAsync(reference, ScoreboardModelImpl::newBuilder)
        .thenApplyAsync(b -> b
            .withEmptyScoreboard()
            .filterSubmissions(s -> false)
            .build());

    final CompletableFuture<ResolverController> resolver = entireContest
        .thenCombineAsync(reference, ResolverController::new)
        .thenCombineAsync(model, ResolverController::addObserver);

    new ResolverWindow(resolver, model, fontData).run();
    System.exit(0);
  }

  private static Optional<ContestConfig.Source> getSource(Invocation invocation) {
    final ContestConfig.Source source;
    if (invocation.getUrl() != null) {
      ContestConfig.Source.Builder sourceBuilder =
          ApiDetective.detectApi(
              invocation.getUrl(),
              invocation.getUsername(),
              invocation.getPassword()).get()
              .toBuilder();
      if (invocation.getContest() != null) {
        sourceBuilder.setContestId(invocation.getContest());
      }
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

  private static Predicate<Group> getGroupFilter(Invocation invocation) {
    final Set<String> groups = invocation.getGroups() != null
        ? new HashSet<>(Arrays.asList(invocation.getGroups().split(",")))
        : null;
    return groups != null
        ? g -> groups.contains(g.getName()) || groups.contains(g.getId())
        : g -> !g.getHidden();
  }
}
