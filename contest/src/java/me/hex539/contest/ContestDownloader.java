package me.hex539.contest;

import com.google.protobuf.TextFormat;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicReference;

import edu.clics.api.ClicsRest;
import edu.clics.proto.ClicsProto;

import me.hex539.interop.ContestConverters;
import java.util.concurrent.CompletableFuture;
import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

public class ContestDownloader {
  enum ApiTarget {
    clics,
    domjudge3;
  }

  private String url;
  private String file;
  private ApiTarget apiTarget = ApiTarget.clics;
  private boolean textFormat = false;
  private String username;
  private String password;

  public ContestDownloader setUrl(String url) {
    this.url = url;
    return this;
  }

  public ContestDownloader setFile(String file) {
    this.file = file;
    return this;
  }

  public ContestDownloader setApi(String api) {
    return setApi(ApiTarget.valueOf(api));
  }

  public ContestDownloader setApi(ApiTarget api) {
    this.apiTarget = api;
    return this;
  }

  public ContestDownloader setCredentials(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
  }

  public ContestDownloader setTextFormat(boolean textFormat) {
    this.textFormat = textFormat;
    return this;
  }

  public ClicsProto.ClicsContest fetch() throws Exception {
    switch (apiTarget) {
      case clics:
        return fetchClics();
      case domjudge3:
        return ContestConverters.toClics(fetchDomjudge3());
      default:
        throw new IllegalArgumentException("No such API type: " + apiTarget);
    }
  }

  private ClicsProto.ClicsContest fetchClics() throws Exception {
    if (url != null) {
      final ClicsRest rest = getClicsRestApi();
      return rest.downloadAllContests().values().iterator().next();
    }
    if (file != null) {
      return fetchClicsContestFile(file, textFormat);
    }
    throw new Error();
  }

  private DomjudgeProto.EntireContest fetchDomjudge3() throws Exception {
    if (url != null) {
      return getRestApi().getEntireContest();
    }
    if (file != null) {
      return fetchDomjudgeContestFile(file, textFormat);
    }
    throw new Error();
  }

  private ClicsProto.ClicsContest fetchClicsContestFile(
      String path, boolean isTextFormat) throws Exception {
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

  private DomjudgeProto.EntireContest fetchDomjudgeContestFile(String path, boolean isTextFormat)
      throws Exception {
    try (FileInputStream f = new FileInputStream(path)) {
      if (isTextFormat) {
        try (Reader is = new InputStreamReader(f)) {
          DomjudgeProto.EntireContest.Builder ecb = DomjudgeProto.EntireContest.newBuilder();
          TextFormat.merge(is, ecb);
          return ecb.build();
        }
      } else {
        return DomjudgeProto.EntireContest.parseFrom(f);
      }
    }
  }

  private ClicsRest getClicsRestApi() {
    System.err.println("Fetching from site: " + url);
    ClicsRest api = new ClicsRest(url);

    if (username != null || password != null) {
      if (username == null || password == null) {
        throw new IllegalArgumentException("Need to provide both or neither of username:password");
      }
      api.setCredentials(username, password);
    }

    return api;
  }

  private DomjudgeRest getRestApi() {
    System.err.println("Fetching from site: " + url);
    DomjudgeRest api = new DomjudgeRest(url);

    if (username != null || password != null) {
      if (username == null || password == null) {
        throw new IllegalArgumentException("Need to provide both or neither of username:password");
      }
      api.setCredentials(username, password);
    }

    return api;
  }
}
