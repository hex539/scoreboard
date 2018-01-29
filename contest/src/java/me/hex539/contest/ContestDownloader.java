package me.hex539.contest;

import com.google.protobuf.TextFormat;

import edu.clics.api.ClicsRest;
import edu.clics.proto.ClicsProto;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import me.hex539.api.RestClient;
import me.hex539.contest.ContestConfig;
import me.hex539.interop.ContestConverters;
import org.domjudge.api.DomjudgeRest;
import org.domjudge.proto.DomjudgeProto;

public class ContestDownloader {
  private final ContestConfig.Source source;
  private final InputStream stream;

  public ContestDownloader(String url) {
    this(ApiDetective.detectApi(url).get());
  }

  public ContestDownloader(ContestConfig.Source source) {
    this.source = source;
    this.stream = null;
  }

  public ContestDownloader(InputStream stream) {
    this.source = null;
    this.stream = stream;
  }

  public ClicsProto.ClicsContest fetch() throws Exception {
    if (stream != null || source != null && source.getFilePath().length() > 0) {
      if (stream != null) {
        return fetchClicsContestFile(stream, false);
      }
      try (InputStream fileStream = new FileInputStream(source.getFilePath())) {
        return fetchClicsContestFile(fileStream, false);
      }
    }

    if (source.hasClicsApi()) {
      return getClicsRestApi().downloadAllContests().values().iterator().next();
    }

    if (source.getDomjudge3Api() != null) {
      return ContestConverters.toClics(getRestApi().getEntireContest());
    }

    throw new IllegalArgumentException("No known API type for contest: " + source.getBaseUrl());
  }

  private ClicsProto.ClicsContest fetchContest(InputStream f) throws Exception {
    return fetchClicsContestFile(f, false);
  }

  private ClicsProto.ClicsContest fetchClicsContestFile(InputStream f, boolean isTextFormat)
      throws Exception {
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

  private DomjudgeProto.EntireContest fetchDomjudgeContestFile(InputStream f, boolean isTextFormat)
      throws Exception {
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

  private <T extends RestClient> T populateRestClient(T api) {
    if (source.hasAuthentication()) {
      api.setCredentials(
          source.getAuthentication().getHttpUsername(),
          source.getAuthentication().getHttpPassword());
    }
    return api;
  }

  private ClicsRest getClicsRestApi() {
    return populateRestClient(new ClicsRest(source.getBaseUrl()));
  }

  private DomjudgeRest getRestApi() {
    return populateRestClient(new DomjudgeRest(source.getBaseUrl()));
  }
}
