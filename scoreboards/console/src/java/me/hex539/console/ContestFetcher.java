package me.hex539.console;

import com.google.protobuf.TextFormat;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicReference;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

public class ContestFetcher {
  private final FetchTask fetcher;
  private final AtomicReference<DomjudgeProto.EntireContest> cache = new AtomicReference<>();

  @FunctionalInterface
  interface FetchTask {
    DomjudgeProto.EntireContest fetch() throws Exception;
  }

  public ContestFetcher(Invocation invocation) {
    final String url = invocation.getUrl();
    final String file = invocation.getFile();

    if (url != null) {
      final DomjudgeRest rest = getRestApi(invocation);
      fetcher = rest::getEntireContest;
      return;
    } else if (file != null) {
      fetcher = () -> fetchContestFile(file);
    } else {
      throw new IllegalArgumentException("Need to specify either --url or --file");
    }
  }

  public ContestFetcher clearCache() {
    cache.set(null);
    return this;
  }

  public DomjudgeProto.EntireContest get() throws Exception {
    // TODO: CompletableFuture does this with less code.
    final AtomicReference<Exception> exception = new AtomicReference<>();
    final DomjudgeProto.EntireContest res = cache.updateAndGet(c -> {
      if (c == null) {
        try {
          return fetcher.fetch();
        } catch (Exception e) {
          exception.set(e);
        }
      }
      return c;
    });
    if (res == null) {
      throw exception.get();
    }
    return res;
  }

  private DomjudgeProto.EntireContest fetchContestFile(String path) throws Exception {
    try (Reader is = new InputStreamReader(new FileInputStream(path))) {
      DomjudgeProto.EntireContest.Builder ecb = DomjudgeProto.EntireContest.newBuilder();
      TextFormat.merge(is, ecb);
      return ecb.build();
    }
  }

  private static DomjudgeRest getRestApi(Invocation invocation) {
    System.err.println("Fetching from site: " + invocation.getUrl());
    DomjudgeRest api = new DomjudgeRest(invocation.getUrl());

    if (invocation.getUsername() != null || invocation.getPassword() != null) {
      final String username = invocation.getUsername();
      final String password = invocation.getPassword();
      if (username == null || password == null) {
        throw new IllegalArgumentException("Need to provide both or neither of username:password");
      }
      api.setCredentials(username, password);
    }

    return api;
  }
}
