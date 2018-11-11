package me.hex539.contest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import me.hex539.contest.ContestConfig;
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
  private static final String[] CLICS_BASES = {"", "/api"};

  private static final OkHttpClient client = new OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .followSslRedirects(true)
      .followRedirects(false)
      .build();

  public static Optional<ContestConfig.Source> detectApi(String baseUrl) {
    return detectAllApis(baseUrl.replaceAll("/+$", ""))
        .reduce((a,b) -> compare(a,b) > 0 ? a : b);
  }

  private static Stream<ContestConfig.Source> detectAllApis(String baseUrl) {
    List<CompletableFuture<ContestConfig.Source>> attempts = new ArrayList<>();

    for (String domjudge: DOMJUDGE_BASES) {
      for (String api : APIV3_BASES) {
        final String url = baseUrl + domjudge + api;
        attempts.add(CompletableFuture.supplyAsync(() -> canaryApiV3(url)));
      }
      for (String api : CLICS_BASES) {
        final String url = baseUrl + domjudge + api;
        attempts.add(CompletableFuture.supplyAsync(() -> canaryClics(url, api.isEmpty())));
      }
    }

    return attempts.stream()
        .map(ApiDetective::getOptional)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private static ContestConfig.Source canaryApiV3(String baseUrl) throws CompletionException{
    final String url = baseUrl + "/categories";
    try (Response response = client.newCall(new Request.Builder().url(url).build()).execute()) {
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

  private static ContestConfig.Source canaryClics(String baseUrl, boolean inRoot)
      throws CompletionException{
    final String url = baseUrl + "/contests";
    try (Response response = client.newCall(new Request.Builder().url(url).build()).execute()) {
      if (response.code() == 200 && !response.isRedirect()) {
        System.err.println("Hit " + url);
        return ContestConfig.Source.newBuilder()
            .setBaseUrl(baseUrl)
            .setClicsApi(ContestConfig.ClicsApi.newBuilder()
                .setApiInRoot(inRoot)
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
