package org.domjudge.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.protobuf.ProtoTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.AbstractMessage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

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
  private final OkHttpClient client = new OkHttpClient();
  private final GsonSingleton gson = new GsonSingleton();

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
  }

  public DomjudgeRest setCredentials(String username, String password) {
    auth = new Auth(username, password);
    return this;
  }

  public Affiliation[] getAffiliations() throws Exception {
    return getFrom("/affiliations", Affiliation[].class);
  }

  public Category[] getCategories() throws Exception {
    return getFrom("/categories", Category[].class);
  }

  public Clarification[] getClarifications() throws Exception {
    return getFrom("/clarifications", Clarification[].class);
  }

  public Contest getContest() throws Exception {
    return getFrom("/contest", Contest.class);
  }

  public Contest[] getContests() throws Exception {
    Type type = new TypeToken<Map<String, Contest>>(){}.getType();
    return ((Map<String, Contest>) getFrom("/contests", type))
        .values()
        .stream()
        .map(Contest.class::cast)
        .toArray(Contest[]::new);
  }

  public JudgementType[] getJudgementTypes(Contest contest) throws Exception {
    return getFrom("/judgement_types", JudgementType[].class);
  }

  public Judging[] getJudgings(Contest contest) throws Exception {
    return getFrom("/judgings?cid=" + contest.getId(), Judging[].class);
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

  public ScoreboardRow[] getScoreboard(Contest contest) throws Exception {
    return getFrom("/scoreboard?cid=" + contest.getId(), ScoreboardRow[].class);
  }

  public Submission[] getSubmissions(Contest contest) throws Exception {
    return getFrom("/submissions?cid=" + contest.getId(), Submission[].class);
  }

  public Team[] getTeams() throws Exception {
    return getFrom("/teams", Team[].class);
  }

  public EntireContest getEntireContest() throws Exception {
    return getEntireContest(getContest());
  }

  public EntireContest getEntireContest(Contest contest) throws Exception {
    return EntireContest.newBuilder()
        .addAllAffiliations(Arrays.asList(getAffiliations()))
        .addAllCategories(Arrays.asList(getCategories()))
        .addAllClarifications(Arrays.asList(getClarifications()))
        .setContest(contest)
        .addAllContests(Arrays.asList(getContests()))
        .addAllJudgementTypes(Arrays.asList(getJudgementTypes(contest)))
        .addAllProblems(Arrays.asList(getProblems(contest)))
        .addAllScoreboard(Arrays.asList(getScoreboard(contest)))
        .addAllSubmissions(Arrays.asList(getSubmissions(contest)))
        .addAllTeams(Arrays.asList(getTeams()))
        .build();
  }

  protected <T> T getFrom(String endpoint, Class<T> c) throws IOException {
    return requestFrom(endpoint, response -> gson.get().fromJson(response.string(), c));
  }

  protected <T> T getFrom(String endpoint, java.lang.reflect.Type c) throws IOException {
    return requestFrom(endpoint, response -> gson.get().fromJson(response.string(), c));
  }

  @FunctionalInterface
  private interface ResponseHandler<T, R> {
    R apply(T t) throws IOException;
  }

  protected <T> T requestFrom(String endpoint, ResponseHandler<? super ResponseBody, T> handler)
        throws IOException {
    Request.Builder request = new Request.Builder()
        .url(url + endpoint);

    if (auth != null) {
      request.header("Authorization", Credentials.basic(auth.username, auth.password));
    }

    try (ResponseBody body = client.newCall(request.build()).execute().body()) {
      return handler.apply(body);
    }
  }

  private static class GsonSingleton {
    private Gson gson = null;

    public Gson get() {
      if (gson != null) {
        return gson;
      }
      return (gson = supply());
    }

    protected Gson supply() {
      ProtoTypeAdapter adapter = ProtoTypeAdapter.newBuilder()
          .setEnumSerialization(ProtoTypeAdapter.EnumSerialization.NAME)
          .setFieldNameSerializationFormat(LOWER_UNDERSCORE, LOWER_UNDERSCORE)
          .addSerializedNameExtension(Annotations.serializedName)
          .addSerializedEnumValueExtension(Annotations.serializedValue)
          .build();

      GsonBuilder gsonBuilder = new GsonBuilder();
      gsonBuilder.registerTypeAdapter(Boolean.class, new SloppyBooleanDeserializer());
      addMessagesFromClass(gsonBuilder, adapter, DomjudgeProto.class);
      return gsonBuilder.create();
    }

    protected static void addMessagesFromClass(
        GsonBuilder builder,
        ProtoTypeAdapter adapter,
        Class c) {
      for (Class subClass : c.getDeclaredClasses()) {
        if (AbstractMessage.class.isAssignableFrom(subClass)) {
          builder.registerTypeHierarchyAdapter(subClass, adapter);
        }
      }
    }
  }
}
