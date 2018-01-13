package org.domjudge.api;

import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.protobuf.ProtoTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.AbstractMessage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.domjudge.api.RequiresRole;
import org.domjudge.proto.Annotations;
import org.domjudge.proto.DomjudgeProto;
import org.domjudge.proto.DomjudgeProto.*;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

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

  @RequiresRole(any = true)
  public Affiliation[] getAffiliations() throws CompletionException {
    return getFrom("/affiliations", Affiliation[].class).get();
  }

  @RequiresRole(any = true)
  public Category[] getCategories() throws CompletionException {
    return getFrom("/categories", Category[].class).get();
  }

  @RequiresRole(any = true)
  public Clarification[] getClarifications() throws CompletionException {
    return getFrom("/clarifications", Clarification[].class).get();
  }

  @RequiresRole(any = true)
  public Contest getContest() throws CompletionException {
    return getFrom("/contest", Contest.class).get();
  }

  @RequiresRole(any = true)
  public Contest[] getContests() throws CompletionException {
    Type type = new TypeToken<Map<String, Contest>>(){}.getType();
    return ((Map<String, Contest>) getFrom("/contests", type).get())
        .values()
        .stream()
        .map(Contest.class::cast)
        .toArray(Contest[]::new);
  }

  @RequiresRole(any = true)
  public JudgementType[] getJudgementTypes(Contest contest) throws CompletionException {
    return getFrom("/judgement_types", JudgementType[].class).get();
  }

  @RequiresRole(anyOf = {
      User.Role.team,
      User.Role.judgehost,
      User.Role.jury,
      User.Role.admin,
      User.Role.event_reader,
      User.Role.full_event_reader})
  public Judging[] getJudgings(Contest contest) throws CompletionException {
    return getJudgingsInternal(contest).get();
  }

  @RequiresRole(anyOf = {
      User.Role.team,
      User.Role.judgehost,
      User.Role.jury,
      User.Role.admin,
      User.Role.event_reader,
      User.Role.full_event_reader})
  public Optional<Judging[]> getJudgingsInternal(Contest contest) throws CompletionException {
    return getFrom("/judgings?cid=" + contest.getId(), Judging[].class);
  }

  @RequiresRole(any = true)
  public Problem[] getProblems(Contest contest) throws CompletionException {
    // We need to sort the problems by label because DOMjudge gives them out in a not-very-useful
    // order, despite showing them in sorted order on the scoreboard.
    Optional<Problem[]> problems = getFrom("/problems?cid=" + contest.getId(), Problem[].class);
    Arrays.sort(problems.get(), (Problem a, Problem b) -> {
        int res = 0;
        return (res = a.getLabel().compareTo(b.getLabel())) != 0
            || (res = a.getShortName().compareTo(b.getShortName())) != 0
            || (res = a.getName().compareTo(b.getName())) != 0
            || (res = Long.compare(a.getId(), b.getId())) != 0
            ? res : 0;});
    return problems.get();
  }

  @RequiresRole(any = true)
  public ScoreboardRow[] getScoreboard(Contest contest) throws CompletionException {
    return getFrom("/scoreboard?cid=" + contest.getId(), ScoreboardRow[].class).get();
  }

  @RequiresRole(any = true)
  public Submission[] getSubmissions(Contest contest) throws CompletionException {
    return getFrom("/submissions?cid=" + contest.getId(), Submission[].class).get();
  }

  @RequiresRole(any = true)
  public Team[] getTeams() throws CompletionException {
    return getFrom("/teams", Team[].class).get();
  }

  @RequiresRole(any = true)
  public Optional<User> getUser() throws CompletionException {
    return getFrom("/user", User.class).filter(u -> u.getId() != 0);
  }

  @RequiresRole(any = true)
  public EntireContest getEntireContest() throws Exception {
    return getEntireContest(getContest());
  }

  public EntireContest getEntireContest(Contest contest) throws Exception {
    return getEntireContest(contest, ForkJoinPool.commonPool());
  }

  public EntireContest getEntireContest(Contest contest, Executor executor) throws Exception {
    // final Set<User.Role> roles =
    //    getUser().map(User::getRolesList).map(HashSet::new).orElseGet(HashSet::new);

    final EntireContest.Builder b = EntireContest.newBuilder().setContest(contest);
    return CompletableFuture.allOf(
        CompletableFuture.runAsync(
            () -> b.addAllAffiliations(Arrays.asList(getAffiliations())), executor),
        CompletableFuture.runAsync(
            () -> b.addAllCategories(Arrays.asList(getCategories())), executor),
        CompletableFuture.runAsync(
            () -> b.addAllClarifications(Arrays.asList(getClarifications())), executor),
        CompletableFuture.runAsync(
            () -> b.addAllContests(Arrays.asList(getContests())), executor),
        CompletableFuture.runAsync(
            () -> b.addAllJudgementTypes(Arrays.asList(getJudgementTypes(contest))), executor),
        CompletableFuture.runAsync(
            () -> b.addAllProblems(Arrays.asList(getProblems(contest))), executor),
        CompletableFuture.runAsync(
            () -> b.addAllScoreboard(Arrays.asList(getScoreboard(contest))), executor),
        CompletableFuture.runAsync(
            () -> b.addAllSubmissions(Arrays.asList(getSubmissions(contest))), executor),
        CompletableFuture.runAsync(
            () -> b.addAllTeams(Arrays.asList(getTeams()))),
        // Optional items that need credentials and may be denied.
        CompletableFuture.runAsync(
            () -> getJudgingsInternal(contest).map(Arrays::asList).ifPresent(b::addAllJudgings)))
      .thenApply(ignore -> b.build())
      .join();
  }

  protected static boolean userHasAnyRole(Set<User.Role> user, User.Role... roles) {
    return new HashSet<>(Arrays.asList(roles)).removeAll(user);
  }

  protected <T> Optional<T> getFrom(String endpoint, Class<T> c) throws CompletionException {
    return requestFrom(endpoint, b -> b.map(body -> gson.get().fromJson(body, c)));
  }

  protected <T> Optional<T> getFrom(String endpoint, Type c) throws CompletionException {
    return requestFrom(endpoint, b -> b.map(body -> gson.get().fromJson(body, c)));
  }

  @FunctionalInterface
  private interface ResponseHandler<T, R> {
    R apply(Optional<T> t) throws IOException;
  }

  protected <T> T requestFrom(String endpoint, ResponseHandler<? super String, T> handler)
        throws CompletionException {
    Request.Builder request = new Request.Builder()
        .url(url + endpoint);

    if (auth != null) {
      request.header("Authorization", Credentials.basic(auth.username, auth.password));
    }

    try (Response response = client.newCall(request.build()).execute()) {
      switch  (response.code()) {
        case 200:
          // OK
          return handler.apply(Optional.ofNullable(response.body().string()));
        case 403: // Forbidden (need to authenticate, older api versions)
        case 405: // Method not allowed (need to authenticate, newer api versions)
          return handler.apply(Optional.empty());
        default:
          // Not handled. Probably an invalid request.
          throw new IOException(response.code() + ": " + response.message());
      }
    } catch (IOException e) {
      throw new CompletionException(e);
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
