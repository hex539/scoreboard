package edu.clics.api;

import static com.google.common.truth.Truth.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;

import edu.clics.proto.ClicsProto;
import edu.clics.proto.ClicsProto.*;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

public class RestTest {

  final Contest contest = Contest.newBuilder().build();

  ClicsRest client;
  MockWebServer server;

  @Before
  public void createServerAndClient() {
    server = new MockWebServer();
    client = new ClicsRest(server.url("/api").toString());
  }

  @After
  public void destroyServer() throws IOException {
    server.shutdown();
  }

  @Test
  public void parseEventFeed_deleteContest_correctIdAndTimestamp() throws Exception {
    JsonObject data = new JsonObject();
    data.addProperty("id", "15100");

    JsonObject json = new JsonObject();
    json.addProperty("id", "34111");
    json.addProperty("type", "contests");
    json.addProperty("op", "delete");
    json.addProperty("time", "2018-12-10T21:28:47.447+00:00");
    json.add("data", data);

    server.enqueue(new MockResponse().setBody(json.toString()));

    EventFeedItem event = client.eventFeed(contest).get().poll(400, MILLISECONDS).get();
    assertThat(event.getId()).isEqualTo("34111");
    assertThat(event.getType()).isEqualTo(EventFeedItem.Type.contests);
    assertThat(event.getOperation()).isEqualTo(EventFeedItem.Operation.delete);
    assertThat(event.getTime()).isEqualTo(
        Timestamp.newBuilder()
            .setSeconds(1544477327L)
            .setNanos(447000000)
            .build());
  }

  @Test
  public void parseEventFeed_createGroups() throws Exception {
    JsonObject data = new JsonObject();
    data.addProperty("id", "15099");
    data.addProperty("icpc_id", "15099");
    data.addProperty("name", "Bath Local");
    data.addProperty("color", "#ffffff");
    data.addProperty("sortorder", 0);

    JsonObject json = new JsonObject();
    json.addProperty("id", "32051");
    json.addProperty("type", "groups");
    json.addProperty("op", "create");
    json.addProperty("time", "2018-12-10T21:28:47.447+00:00");
    json.add("data", data);

    server.enqueue(new MockResponse().setBody(json.toString()));

    EventFeedItem event = client.eventFeed(contest).get().poll(400, MILLISECONDS).get();

    assertThat(event.getType()).isEqualTo(EventFeedItem.Type.groups);
    assertThat(event.getOperation()).isEqualTo(EventFeedItem.Operation.create);
    assertThat(event.getTime()).isEqualTo(
        Timestamp.newBuilder()
            .setSeconds(1544477327L)
            .setNanos(447000000)
            .build());

    assertThat(event.hasGroupData()).isTrue();
    assertThat(event.getGroupData().getId()).isEqualTo("15099");
    assertThat(event.getGroupData().getName()).isEqualTo("Bath Local");
  }

  @Test
  public void parseEventFeed_createTeams() throws Exception {
    JsonArray groupIds = new JsonArray();
    groupIds.add("15099");

    JsonObject data = new JsonObject();
    data.addProperty("id", "344");
    data.addProperty("name", "Null");
    data.add("group_ids", groupIds);

    JsonObject json = new JsonObject();
    json.addProperty("id", "32062");
    json.addProperty("type", "teams");
    json.addProperty("op", "create");
    json.add("data", data);

    server.enqueue(new MockResponse().setBody(json.toString()));

    EventFeedItem event = client.eventFeed(contest).get().poll(400, MILLISECONDS).get();
    assertThat(event.getType()).isEqualTo(EventFeedItem.Type.teams);
    assertThat(event.getOperation()).isEqualTo(EventFeedItem.Operation.create);

    assertThat(event.hasGroupData()).isFalse();
    assertThat(event.hasTeamData()).isTrue();
    assertThat(event.getTeamData().getId()).isEqualTo("344");
    assertThat(event.getTeamData().getName()).isEqualTo("Null");
  }

  @Test
  public void parseEventFeed_updateJudgement() throws Exception {
    JsonObject data = new JsonObject();
    data.addProperty("id", "3368");
    data.addProperty("submission_id", "2475");
    data.addProperty("judgement_type_id", "AC");
    data.addProperty("start_time", "2018-11-25T15:01:18.709+01:00");
    data.addProperty("start_contest_time", "5:01:18.709");
    data.addProperty("end_time", "2018-11-25T15:01:28.593+01:00");
    data.addProperty("end_contest_time", "5:01:28.593");
    data.addProperty("max_run_time", 0.039);

		JsonObject json = new JsonObject();
    json.addProperty("id", "32154");
    json.addProperty("type", "judgements");
    json.addProperty("op", "update");
    json.add("data", data);

    server.enqueue(new MockResponse().setBody(json.toString()));

    EventFeedItem event = client.eventFeed(contest).get().poll(400, MILLISECONDS).get();
    assertThat(event.getType()).isEqualTo(EventFeedItem.Type.judgements);
    assertThat(event.getOperation()).isEqualTo(EventFeedItem.Operation.update);

    assertThat(event.hasJudgementData()).isTrue();
    assertThat(event.getJudgementData().getStartTime()).isEqualTo(
        Timestamp.newBuilder()
            .setSeconds(1543154478L)
            .setNanos(709000000)
            .build());
    assertThat(event.getJudgementData().getEndContestTime()).isEqualTo(
        Duration.newBuilder()
            .setSeconds(18088L)
            .setNanos(593000000)
            .build());
    assertThat(event.getJudgementData().getMaxRunTime()).isWithin(1e-9).of(0.039);
  }

  @Test
  public void parseEventFeed_multiple() throws Exception {
    JsonObject json1 = new JsonObject();
    json1.addProperty("id", "32060");
    json1.addProperty("type", "contests");
    json1.addProperty("op", "create");

    JsonObject json2 = new JsonObject();
    json2.addProperty("id", "32061");
    json2.addProperty("type", "contests");
    json2.addProperty("op", "update");

    JsonObject json3 = new JsonObject();
    json3.addProperty("id", "32062");
    json3.addProperty("type", "groups");
    json3.addProperty("op", "delete");

    server.enqueue(new MockResponse().setBody(
        json1.toString() + "\r\n\r\n" + json2.toString() + "\n" + json3.toString()));

    BlockingQueue<Optional<EventFeedItem>> events = client.eventFeed(contest).get();
    assertThat(events.poll(400, MILLISECONDS).get().getOperation()).isEqualTo(EventFeedItem.Operation.create);
    assertThat(events.poll(400, MILLISECONDS).get().getOperation()).isEqualTo(EventFeedItem.Operation.update);
    assertThat(events.poll(400, MILLISECONDS).get().getOperation()).isEqualTo(EventFeedItem.Operation.delete);
    assertThat(events.poll(400, MILLISECONDS).isPresent()).isFalse();
  }

  @Test
  public void writeEventFeed_createTeams_roundTrip() throws Exception {
    JsonArray groupIds = new JsonArray();
    groupIds.add("15099");

    JsonObject data = new JsonObject();

    data.addProperty("id", "344");
    data.addProperty("name", "Null");
    data.add("group_ids", groupIds);

    JsonObject json = new JsonObject();
    json.addProperty("id", "32062");
    json.addProperty("type", "teams");
    json.addProperty("op", "create");
    json.add("data", data);

    server.enqueue(new MockResponse().setBody(json.toString()));

    EventFeedItem event = client.eventFeed(contest).get().poll(400, MILLISECONDS).get();
    assertThat(event.getOperation()).isEqualTo(EventFeedItem.Operation.create);

    assertThat(new ClicsRest.GsonSingleton().get().toJson(event)).isEqualTo(json.toString());
  }
}
