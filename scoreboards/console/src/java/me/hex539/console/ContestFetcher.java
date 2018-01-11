package me.hex539.console;

import com.google.protobuf.TextFormat;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicReference;

import edu.clics.api.ClicsRest;
import edu.clics.proto.ClicsProto;

import me.hex539.interop.ContestConverters;

import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

public class ContestFetcher {
  private final FetchTask fetcher;
  private final AtomicReference<ClicsProto.ClicsContest> cache = new AtomicReference<>();

  @FunctionalInterface
  interface FetchTask {
    ClicsProto.ClicsContest fetch() throws Exception;
  }

  public ContestFetcher(Invocation invocation) {
    final String url = invocation.getUrl();
    final String file = invocation.getFile();

    if (url != null) {
      switch (invocation.getApiTarget()) {
        case "domjudge3": {
          final DomjudgeRest rest = getRestApi(invocation);
          fetcher = () -> ContestConverters.toClics(rest.getEntireContest());
          return;
        }
        case "clics": {
          final ClicsRest rest = getClicsRestApi(invocation);
          fetcher = () -> rest.downloadAllContests().values().iterator().next();
          return;
        }
        default: {
          throw new IllegalArgumentException("Unknown API version: " + invocation.getApiTarget());
        }
      }
    } else if (file != null) {
      switch (invocation.getApiTarget()) {
        case "domjudge3": {
          fetcher = () -> fetchDomjudgeContestFile(file, invocation.isTextFormat());
          return;
        }
        case "clics": {
          fetcher = () -> fetchClicsContestFile(file, invocation.isTextFormat());
          return;
        }
        default: {
          throw new IllegalArgumentException("Unknown API version: " + invocation.getApiTarget());
        }
      }
    } else {
      throw new IllegalArgumentException("Need to specify either --url or --file");
    }
  }

  public ContestFetcher clearCache() {
    cache.set(null);
    return this;
  }

  public ClicsProto.ClicsContest get() throws Exception {
    // TODO: CompletableFuture does this with less code.
    final AtomicReference<Exception> exception = new AtomicReference<>();
    final ClicsProto.ClicsContest res = cache.updateAndGet(c -> {
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

  private ClicsProto.ClicsContest fetchClicsContestFile(String path, boolean isTextFormat)
        throws Exception {
    try (FileInputStream f = new FileInputStream(path)) {
      if (isTextFormat) {
        try (Reader is = new InputStreamReader(f)) {
          ClicsProto.ClicsContest.Builder ccb = ClicsProto.ClicsContest.newBuilder();
          TextFormat.merge(is, ccb);
          return ccb.build();
        }
      } else {
        return ClicsProto.ClicsContest.parseFrom(f);
      }
    }
  }

  private ClicsProto.ClicsContest fetchDomjudgeContestFile(String path, boolean isTextFormat)
      throws Exception {
    try (FileInputStream f = new FileInputStream(path)) {
      if (isTextFormat) {
        try (Reader is = new InputStreamReader(f)) {
          DomjudgeProto.EntireContest.Builder ecb = DomjudgeProto.EntireContest.newBuilder();
          TextFormat.merge(is, ecb);
          return ContestConverters.toClics(ecb.build());
        }
      } else {
        return ContestConverters.toClics(DomjudgeProto.EntireContest.parseFrom(f));
      }
    }
  }

  private static ClicsRest getClicsRestApi(Invocation invocation) {
    System.err.println("Fetching from site: " + invocation.getUrl());
    ClicsRest api = new ClicsRest(invocation.getUrl());

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
