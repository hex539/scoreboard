package me.hex539.scoreboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.protobuf.ProtoTypeAdapter;
import com.google.protobuf.AbstractMessage;

import me.hex539.proto.domjudge.Annotations;
import me.hex539.proto.domjudge.DomjudgeProto;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;

public class DomjudgeRest {
  private final String url;
  private final OkHttpClient client;

  public DomjudgeRest(final String url) {
    this.url = url;
    this.client = new OkHttpClient();
  }

  public DomjudgeProto.Team[] getTeams() throws Exception {
    Request request = new Request.Builder()
        .url(url + "/teams")
        .build();
    try (ResponseBody body = client.newCall(request).execute().body()) {
      return getGson().fromJson(body.string(), DomjudgeProto.Team[].class);
    }
  }

  public DomjudgeProto.ScoreboardRow[] getScoreboard(int contestId) throws Exception {
    Request request = new Request.Builder()
        .url(url + "/scoreboard?cid=" + contestId)
        .build();
    try (ResponseBody body = client.newCall(request).execute().body()) {
      return getGson().fromJson(body.string(), DomjudgeProto.ScoreboardRow[].class);
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
