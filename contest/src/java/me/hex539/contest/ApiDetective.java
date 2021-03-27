package me.hex539.contest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import me.hex539.contest.ContestConfig;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Request;

/**
 * Detector for different paths and API versions of a contest on a given server.
 * <p>
 * TODO: fill in the best-matching contest ID and Name fields at the same time
 *       as spidering for the contest.
 */
public final class ApiDetective {
  private ApiDetective() {}

  private static final String[] DOMJUDGE_BASES = {"", "/domjudge"};
  private static final String[] APIV3_BASES = {"", "/api", "/api/v3"};
  private static final String[] CLICS_BASES = {"", "/api", "/clics-api"};

  public static Optional<ContestConfig.Source> detectApi(String baseUrl) {
    return detectApi(baseUrl, null, null);
  }

  public static Optional<ContestConfig.Source> detectApi(
      String baseUrl,
      @Nullable String username,
      @Nullable String password) {
    return detectAllApis(baseUrl.replaceAll("/+$", ""), username, password)
        .reduce((a,b) -> compare(a,b) > 0 ? a : b);
  }

  private static Stream<ContestConfig.Source> detectAllApis(
      String baseUrl,
      @Nullable String username,
      @Nullable String password) {
    List<CompletableFuture<ContestConfig.Source>> attempts = new ArrayList<>();

    final OkHttpClient client = new OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .followSslRedirects(true)
      .followRedirects(false)
      .build();

    final Supplier<Request.Builder> requestBuilder = () -> (username != null || password != null)
        ? new Request.Builder().header("Authorization", Credentials.basic(username, password))
        : new Request.Builder();

    for (String domjudge: DOMJUDGE_BASES) {
      for (String api : APIV3_BASES) {
        final String url = baseUrl + domjudge + api;
        attempts.add(CompletableFuture.supplyAsync(() -> canaryApiV3(client, requestBuilder, url)));
      }
      for (String api : CLICS_BASES) {
        final String url = baseUrl + domjudge + api;
        attempts.add(CompletableFuture.supplyAsync(() -> canaryClics(client, requestBuilder, url)));
      }
    }

    return attempts.stream()
        .map(ApiDetective::getOptional)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private static ContestConfig.Source canaryApiV3(
      final OkHttpClient client,
      final Supplier<Request.Builder> requestBuilder,
      final String baseUrl) throws CompletionException {
    final String url = baseUrl + "/categories";
    try (Response response = client.newCall(requestBuilder.get().url(url).build()).execute()) {
      if (response.code() == 200 && !response.isRedirect()) {
        System.err.println("Hit " + url);
        return ContestConfig.Source.newBuilder()
            .setBaseUrl(baseUrl)
            .setDomjudge3Api(ContestConfig.Domjudge3Api.newBuilder())
            .build();
      }
      throw new IOException(url);
    } catch (IOException e) {
      throw new CompletionException(e);
    }
  }

  private static ContestConfig.Source canaryClics(
      final OkHttpClient client,
      final Supplier<Request.Builder> requestBuilder,
      final String baseUrl) throws CompletionException {
      return canaryClics(client, requestBuilder, baseUrl, false);
  }

  private static ContestConfig.Source canaryClics(
      final OkHttpClient client,
      final Supplier<Request.Builder> requestBuilder,
      final String baseUrl,
      final boolean trailingSlash) throws CompletionException {
    final String url = baseUrl + "/contests" + (trailingSlash ? "/" : "");
    final String gps = baseUrl + "/groups" + (trailingSlash ? "/" : "");
    try (Response response = client.newCall(requestBuilder.get().url(url).build()).execute()) {
      if (response.code() == 308 && response.isRedirect()) {
        if ((url + "/").equals(response.header("Location"))) {
          return canaryClics(client, requestBuilder, baseUrl, true);
        }
      }
      if (response.code() == 200 && !response.isRedirect()) {
        System.err.println("Hit " + url);

        // Check that contests is an array, not a map.
        if (!response.body().string().trim().startsWith("[")) {
          return null;
        }

        // If there is an /api/groups endpoint, we're probably using an old
        // version of DOMjudge with endpoints in the wrong place.
        boolean apiInRoot = false;
        try (Response rCanary = client.newCall(requestBuilder.get().url(gps).build()).execute()) {
          apiInRoot = (rCanary.code() == 200 && !rCanary.isRedirect());
        }

        return ContestConfig.Source.newBuilder()
            .setBaseUrl(baseUrl)
            .setClicsApi(ContestConfig.ClicsApi.newBuilder()
                .setApiInRoot(apiInRoot)
                .build())
            .build();
      }
      throw new IOException(url);
    } catch (IOException e) {
      throw new CompletionException(e);
    }
  }

  private static int compare(ContestConfig.Source a, ContestConfig.Source b) {
    if (a.hasClicsApi() != b.hasClicsApi()) {
      return Boolean.compare(a.hasClicsApi(), b.hasClicsApi());
    }
    if (a.hasClicsApi()) {
      return Boolean.compare(!a.getClicsApi().getApiInRoot(), !b.getClicsApi().getApiInRoot());
    }
    return 0;
  }

  private static <T> Optional<T> getOptional(CompletableFuture<T> t) {
    try {
      return Optional.ofNullable(t.get());
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
