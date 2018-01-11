package edu.clics.api;

import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.protobuf.ProtoTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.AbstractMessage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.Annotations;
import edu.clics.proto.ClicsProto;
import edu.clics.proto.ClicsProto.*;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

class RestClient<Self extends RestClient> {
  private final String url;
  private final OkHttpClient client = new OkHttpClient.Builder()
      .connectTimeout(1, TimeUnit.MINUTES)
      .readTimeout(1, TimeUnit.MINUTES)
      .build();

  private static class Auth {
    final String username;
    final String password;

    public Auth(String username, String password) {
      this.username = username;
      this.password = password;
    }
  }
  private Auth auth;

  public RestClient(final String url) {
    this.url = url;
  }

  public Self setCredentials(String username, String password) {
    auth = new Auth(username, password);
    return (Self) this;
  }

  @FunctionalInterface
  public interface ResponseHandler<T, R> {
    R apply(Optional<T> t) throws IOException;
  }

  public <T> T requestFrom(String endpoint, ResponseHandler<? super String, T> handler)
        throws IOException {
    Request.Builder request = new Request.Builder()
        .url(url + endpoint);

    if (auth != null) {
      request.header("Authorization", Credentials.basic(auth.username, auth.password));
    }
    System.err.println("Fetching " + url + endpoint + "...");

    try (Response response = client.newCall(request.build()).execute()) {
      switch  (response.code()) {
        case 200:
          // OK
          final String body = response.body().string();
          try {
            return handler.apply(Optional.ofNullable(body));
          } catch (Exception e) {
            System.err.println("Failed on response body:\n" + body);
            throw e;
          }

        case 403: // Forbidden (need to authenticate, older api versions)
        case 405: // Method not allowed (need to authenticate, newer api versions)
          return handler.apply(Optional.empty());
        default: // Not handled. Probably an invalid request.
          throw new IOException(
              "GET " + endpoint + ": " + response.code() + ", " + response.message());
      }
    }
  }
}

public class ClicsRest extends RestClient<ClicsRest> {
  private final GsonSingleton gson = new GsonSingleton();

  public ClicsRest(final String url) {
    super(url);
  }

  public Map<String, ClicsContest> downloadAllContests() throws Exception {
    Map<String, ClicsContest> contests = new HashMap<>();
    for (Contest contest : getContests()) {
      contests.put(contest.getId(), downloadPublicContest(contest));
    }
    return contests;
  }

  public List<Contest> getContests() throws Exception {
    return getListFrom("/contests", Contest[].class);
  }

  public ClicsContest downloadPublicContest(Contest contest) throws IOException {
    return buildPublicContest(contest).build();
  }

  public ClicsContest downloadContest(Contest contest) throws IOException {
    return buildFullContest(contest).build();
  }

  private static String getContestPath(Contest contest) {
    final boolean DOMJUDGE_CONTEST_API_IN_ROOT = true;
    return (DOMJUDGE_CONTEST_API_IN_ROOT ? "" : "/contests/" + contest.getId());
  }

  private ClicsContest.Builder buildPublicContest(Contest contest) throws IOException {
    final String path = getContestPath(contest);
    return ClicsContest.newBuilder()
        .setContest(contest)

        // TODO: Not implement by DOMjudge (2018-01).
//        .setState(
//            getFrom(path + "/state", ContestState.class).get())
        // TODO: Not implemented by DOMjudge (2018-01).
//        .putAllAwards(
//            getMapFrom(path + "/awards", Award[].class, Award::getId))

        .addAllScoreboard(
            getListFrom(path + "/scoreboard", ScoreboardRow[].class))
        .putAllJudgementTypes(
            getMapFrom(path + "/judgement-types", JudgementType[].class, JudgementType::getId))
        .putAllLanguages(
            getMapFrom(path + "/languages", Language[].class, Language::getId))
        .putAllProblems(
            getMapFrom(path + "/problems", Problem[].class, Problem::getId))
        .putAllGroups(
            getMapFrom(path + "/groups", Group[].class, Group::getId))
        .putAllTeams(
            getMapFrom(path + "/teams", Team[].class, Team::getId))
        // TODO: Not implemented by DOMjudge (2018-01).
//        .putAllTeamMembers(
//            getMapFrom(path + "/team-members", TeamMember[].class, TeamMember::getId))
        .putAllSubmissions(
            getMapFrom(path + "/submissions", Submission[].class, Submission::getId))
        .putAllJudgements(
            getMapFrom(path + "/judgements", Judgement[].class, Judgement::getId));
  }

  // Including usually-unnecessary fields like run information, clarifications, and team members.
  private ClicsContest.Builder buildFullContest(Contest contest) throws IOException {
    final String path = getContestPath(contest);
    return buildPublicContest(contest)
        .putAllRuns(
            getMapFrom(path + "/runs", Run[].class, Run::getId))
        .putAllClarifications(
            getMapFrom(path + "/clarifications", Clarification[].class, Clarification::getId));
  }

  protected <T, K> Map<K, T> getMapFrom(String endpoint, Class<T[]> c, Function<T, K> m)
      throws IOException {
    return getListFrom(endpoint, c).stream().collect(Collectors.toMap(m, Function.identity()));
  }

  protected <T> List<T> getListFrom(String endpoint, Class<T[]> c) throws IOException {
    return requestFrom(endpoint, b -> b.map(body -> gson.get().fromJson(body, c)))
        .map(Arrays::asList)
        .orElseGet(Collections::emptyList);
  }

  protected <T> Optional<T> getFrom(String endpoint, Class<T> c) throws IOException {
    return requestFrom(endpoint, b -> b.map(body -> gson.get().fromJson(body, c)));
  }

  protected <T> Optional<T> getFrom(String endpoint, Type c) throws IOException {
    return requestFrom(endpoint, b -> b.map(body -> gson.get().fromJson(body, c)));
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
      gsonBuilder.registerTypeAdapter(
          com.google.protobuf.Timestamp.class,
          new Deserializers.TimestampDeserializer());
      gsonBuilder.registerTypeAdapter(
          com.google.protobuf.Duration.class,
          new Deserializers.DurationDeserializer());
      addMessagesFromClass(gsonBuilder, adapter, ClicsProto.class);
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
