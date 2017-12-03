package org.domjudge.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.protobuf.ProtoTypeAdapter;
import com.google.protobuf.AbstractMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.domjudge.proto.Annotations;
import org.domjudge.proto.DomjudgeProto;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static org.domjudge.proto.DomjudgeProto.*;

public class DomjudgeRest {
  private final String url;
  private final OkHttpClient client;

  private static class Auth {
    final String username;
    final String password;

    public Auth(String username, String password) {
      this.username = username;
      this.password = password;
    }
  }
  private Auth auth;

  public DomjudgeRest(final String url) {
    this.url = url;
    this.client = new OkHttpClient();
  }

  public DomjudgeRest setCredentials(String username, String password) {
    auth = new Auth(username, password);
    return this;
  }

  public Contest getContest() throws Exception {
    return getFrom("/contest", Contest.class);
  }

  public Problem[] getProblems(Contest contest) throws Exception {
    // We need to sort the problems by label because DOMjudge gives them out in a not-very-useful
    // order, despite showing them in sorted order on the scoreboard.
    Problem[] problems = getFrom("/problems?cid=" + contest.getId(), Problem[].class);
    Arrays.sort(problems, (Problem a, Problem b) -> {
        int res = 0;
        return (res = a.getLabel().compareTo(b.getLabel())) != 0
            || (res = a.getShortName().compareTo(b.getShortName())) != 0
            || (res = a.getName().compareTo(b.getName())) != 0
            || (res = Long.compare(a.getId(), b.getId())) != 0
            ? res : 0;});
    return problems;
  }

  public Team[] getTeams() throws Exception {
    return getFrom("/teams", Team[].class);
  }

  public ScoreboardRow[] getScoreboard(Contest contest) throws Exception {
    return getFrom("/scoreboard?cid=" + contest.getId(), ScoreboardRow[].class);
  }

  protected <T> T getFrom(String endpoint, Class<T> c) throws IOException {
    Request.Builder request = new Request.Builder()
        .url(url + endpoint);

    if (auth != null) {
      request.header("Authorization", Credentials.basic(auth.username, auth.password));
    }

    try (ResponseBody body = client.newCall(request.build()).execute().body()) {
      return getGson().fromJson(body.string(), c);
    }
  }

  protected static Gson getGson() {
    ProtoTypeAdapter adapter = ProtoTypeAdapter.newBuilder()
        .setEnumSerialization(ProtoTypeAdapter.EnumSerialization.NAME)
        .setFieldNameSerializationFormat(LOWER_UNDERSCORE, LOWER_UNDERSCORE)
        .addSerializedNameExtension(Annotations.serializedName)
        .addSerializedEnumValueExtension(Annotations.serializedValue)
        .build();

    GsonBuilder gsonBuilder = new GsonBuilder();
    addMessagesFromClass(gsonBuilder, adapter, DomjudgeProto.class);
    return gsonBuilder.create();
  }

  protected static void addMessagesFromClass(GsonBuilder builder, ProtoTypeAdapter adapter, Class c) {
    for (Class subClass : c.getDeclaredClasses()) {
      if (AbstractMessage.class.isAssignableFrom(subClass)) {
        builder.registerTypeHierarchyAdapter(subClass, adapter);
      }
    }
  }
}
