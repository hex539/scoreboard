package org.domjudge.api;

import static com.google.common.truth.Truth.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.domjudge.proto.DomjudgeProto;
import org.domjudge.proto.DomjudgeProto.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RestTest {
  final Contest contest = Contest.newBuilder().build();

  DomjudgeRest client;
  MockWebServer server;

  @Before
  public void createServerAndClient() {
    server = new MockWebServer();
    client = new DomjudgeRest(server.url("/api").toString());
  }

  @After
  public void destroyServer() throws IOException {
    server.shutdown();
  }

  @Test
  public void parseContest() throws Exception {
    String json = "{"
        + "\"id\":2,"
        + "\"shortname\":\"TC1\","
        + "\"name\":\"Testing old API 🙃\","
        + "\"start\":1479375900,"
        + "\"freeze\":null,"
        + "\"end\":2520864000,"
        + "\"length\":1041488100,"
        + "\"unfreeze\":null,"
        + "\"penalty\":0"
        + "}";

    server.enqueue(new MockResponse().setBody(json));
    Contest contest = client.getContest();

    assertThat(contest.getId()).isEqualTo(2);
    assertThat(contest.getShortName()).isEqualTo("TC1");
    assertThat(contest.getName()).isEqualTo("Testing old API 🙃");
    assertThat(contest.getStart().getValue()).isEqualTo(1479375900L);
    assertThat(contest.hasFreeze()).isFalse();
    assertThat(contest.getEnd().getValue()).isEqualTo(2520864000L);
    assertThat(contest.getLength().getValue()).isEqualTo(1041488100L);
    assertThat(contest.hasUnfreeze()).isFalse();
    assertThat(contest.hasPenalty()).isTrue();
    assertThat(contest.getPenalty().getValue()).isEqualTo(0);
  }

  @Test
  public void parseUser_withRoles() throws Exception {
    String json = "{"
        + String.join(",",
            "\"id\":1",
            "\"teamid\":2",
            "\"email\":\"webmaster@example.com\"",
            "\"ip\":null",
            "\"lastip\":\"192.168.0.2\"",
            "\"name\":\"Webby McMaster\"",
            "\"username\":\"team-\\\"4546\"",
            "\"roles\":[\"admin\",\"user\",\"jury\"]")
        + "}";

    server.enqueue(new MockResponse().setBody(json));
    User user = client.getUser().get();

    assertThat(user.getId().getValue()).isEqualTo(1);
    assertThat(user.getTeamId().getValue()).isEqualTo(2);
    assertThat(user.getEmail()).isEqualTo("webmaster@example.com");
    assertThat(user.getIp()).isEmpty();
    assertThat(user.getLastIp()).isEqualTo("192.168.0.2");
    assertThat(user.getName()).isEqualTo("Webby McMaster");
    assertThat(user.getUsername()).isEqualTo("team-\"4546");
    assertThat(user.getRolesList())
        .containsExactly(User.Role.admin, User.Role.user, User.Role.jury).inOrder();

    assertThat(new DomjudgeRest.GsonSingleton().get().toJson(user))
        .isEqualTo(json.replaceAll("\"ip\":null,", ""));
  }

  @Test
  public void parseTeams() throws Exception {
    String json = "[{"
        + String.join(",",
            "\"id\":174",
            "\"name\":\"test user\"",
            "\"members\":null",
            "\"nationality\":null",
            "\"category\":3",
            "\"group\":\"Students\"",
            "\"affilid\":null",
            "\"affiliation\":null")
      + "}]";

    server.enqueue(new MockResponse().setBody(json));
    Team[] teams = client.getTeams();

    assertThat(teams.length).isEqualTo(1);
    assertThat(teams[0].hasAffilId()).isFalse();
    assertThat(teams[0].getId()).isEqualTo(174);
    assertThat(teams[0].getName()).isEqualTo("test user");
  }

  @Test
  public void parseCategories() throws Exception {
    String json = "[{"
        + String.join(",",
            "\"categoryid\":3",
            "\"name\":\"Students\"",
            "\"color\":null",
            "\"sortorder\":0")
        + "}, {}]";

    server.enqueue(new MockResponse().setBody(json));
    Category[] categories = client.getCategories();

    assertThat(categories.length).isEqualTo(2);
    assertThat(categories[0].getId()).isEqualTo(3);
    assertThat(categories[0].hasSortOrder()).isTrue();
    assertThat(categories[0].getSortOrder().getValue()).isEqualTo(0);
    assertThat(categories[0].getName()).isEqualTo("Students");
  }
}
