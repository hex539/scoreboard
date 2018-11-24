package edu.clics.api;

import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.protobuf.ProtoTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Annotations;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.clics.proto.ClicsProto;
import edu.clics.proto.ClicsProto.*;

import me.hex539.api.RestClient;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

public class ClicsRest extends RestClient<ClicsRest> {
  private final GsonSingleton gson = new GsonSingleton();
  private boolean apiInRoot = false;

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

  private String getContestPath(Contest contest) {
    return (apiInRoot ? "" : "/contests/" + contest.getId());
  }

  private ClicsContest.Builder buildPublicContest(Contest contest) throws IOException {
    return buildPublicContest(contest, ForkJoinPool.commonPool());
  }

  private ClicsContest.Builder buildPublicContest(Contest contest, Executor executor)
      throws IOException {
    final String path = getContestPath(contest);
    final ClicsContest.Builder b = ClicsContest.newBuilder()
        .setContest(contest);

    return CompletableFuture.allOf(
        CompletableFuture.runAsync(() -> b.setState(
            getFrom(path + "/state", ContestState.class).get()),
            executor),
//        TODO: Not implemented by DOMjudge (2018-11).
//        CompletableFuture.runAsync(() -> b.putAllAwards(
//            getMapFrom(path + "/awards", Award[].class, Award::getId)),
//            executor),
        CompletableFuture.runAsync(() -> b.addAllScoreboard(
            requestFrom(path + "/scoreboard", this::parseScoreboard).getRowsList()),
            executor),
        CompletableFuture.runAsync(() -> b.putAllJudgementTypes(
            getMapFrom(path + "/judgement-types", JudgementType[].class, JudgementType::getId)),
            executor),
        CompletableFuture.runAsync(() -> b.putAllLanguages(
            getMapFrom(path + "/languages", Language[].class, Language::getId)),
            executor),
        CompletableFuture.runAsync(() -> b.putAllProblems(
            getMapFrom(path + "/problems", Problem[].class, Problem::getId)),
            executor),
        CompletableFuture.runAsync(() -> b.putAllGroups(
            getMapFrom(path + "/groups", Group[].class, Group::getId)),
            executor),
        CompletableFuture.runAsync(() -> b.putAllOrganizations(
            getMapFrom(path + "/organizations", Organization[].class, Organization::getId)),
            executor),
        CompletableFuture.runAsync(() -> b.putAllTeams(
            getMapFrom(path + "/teams", Team[].class, Team::getId)),
            executor),
//        TODO: Not implemented by DOMjudge (2018-11).
//        CompletableFuture.runAsync(() -> b.putAllTeamMembers(
//            getMapFrom(path + "/team-members", TeamMember[].class, TeamMember::getId)),
//            executor),
        CompletableFuture.runAsync(() -> b.putAllSubmissions(
            getMapFrom(path + "/submissions", Submission[].class, Submission::getId)),
            executor),
        CompletableFuture.runAsync(() -> b.putAllJudgements(
            getMapFrom(path + "/judgements", Judgement[].class, Judgement::getId)),
            executor))
        .thenApplyAsync(ignore -> b)
        .join();
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
      throws CompletionException {
    return getListFrom(endpoint, c).stream()
//        .filter(x -> m.apply(x) != null)
        .collect(Collectors.toMap(m, Function.identity(), (a, b) -> a));
  }

  protected <T> List<T> getListFrom(String endpoint, Class<T[]> c) throws CompletionException {
    return requestFrom(endpoint, b -> b.map(body -> gson.get().fromJson(body, c)))
        .map(Arrays::asList)
        .orElseGet(Collections::emptyList);
  }

  protected <T> Optional<T> getFrom(String endpoint, Class<T> c) throws CompletionException {
    return requestFrom(endpoint, b -> b.map(body -> gson.get().fromJson(body, c)));
  }

  protected <T> Optional<T> getFrom(String endpoint, Type c) throws CompletionException {
    return requestFrom(endpoint, b -> b.map(body -> gson.get().fromJson(body, c)));
  }

  /**
   * The value returned from /scoreboard may be either a redundant Scoreboard object,
   * or a raw list of ScoreboardRow[]. We need to be able to handle either.
   */
  protected Scoreboard parseScoreboard(Optional<String> body) {
    if (body.isPresent()) {
      final String text = body.get();
      try {
        return gson.get().fromJson(text, Scoreboard.class);
      } catch (JsonParseException e1) {
        try {
          ScoreboardRow[] scoreboardRows = gson.get().fromJson(text, ScoreboardRow[].class);
          return Scoreboard.newBuilder()
              .addAllRows(Arrays.asList(scoreboardRows))
              .build();
        } catch (JsonParseException e2) {
          throw new RuntimeException("Scoreboard not available", e1);
        }
      }
    }
    throw new RuntimeException("Scoreboard endpoint did not respond");
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
