package me.hex539.contest.mutable;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import me.hex539.contest.model.Teams;

import edu.clics.proto.ClicsProto.*;

public class TeamsMutable implements Teams {

  private final Map<String, Organization> organizations = new HashMap<>();
  private final Map<String, Group> groups = new HashMap<>();
  private final Map<String, Team> teams = new HashMap<>();

  private final Map<String, Integer> organizationRefs = new HashMap<>();
  private final Map<String, Integer> groupRefs = new HashMap<>();

  public TeamsMutable() {
    return;
  }

  public TeamsMutable(Teams copy) {
    this();
    copy.getOrganizations().forEach(this::onOrganizationAdded);
    copy.getGroups().forEach(this::onGroupAdded);
    copy.getTeams().forEach(this::onTeamAdded);
  }

  @Override
  public Collection<Organization> getOrganizations() {
    return organizations.values();
  }

  @Override
  public Collection<Group> getGroups() {
    return groups.values();
  }

  @Override
  public Collection<Team> getTeams() {
    return teams.values();
  }

  @Override
  public Optional<Organization> getOrganizationOpt(String id) {
    return Optional.ofNullable(organizations.get(id));
  }

  @Override
  public boolean containsOrganization(String id) {
    return organizations.containsKey(id);
  }

  @Override
  public Optional<Group> getGroupOpt(String id) {
    return Optional.ofNullable(groups.get(id));
  }

  @Override
  public boolean containsGroup(String id) {
    return groups.containsKey(id);
  }

  @Override
  public Optional<Team> getTeamOpt(String id) {
    return Optional.ofNullable(teams.get(id));
  }

  @Override
  public boolean containsTeam(String id) {
    return teams.containsKey(id);
  }

  public void onOrganizationAdded(Organization organization) {
    organizations.put(organization.getId(), organization);
  }

  public void onOrganizationRemoved(Organization organization) {
    Preconditions.checkState(
        organizationRefs.getOrDefault(organization.getId(), 0) == 0,
        "Organization still has references");
    organizations.remove(organization.getId());
  }

  public void onGroupAdded(Group group) {
    groups.put(group.getId(), group);
  }

  public void onGroupRemoved(Group group) {
    Preconditions.checkState(
        groupRefs.getOrDefault(group.getId(), 0) == 0,
        "Group still has references");
    groups.remove(group.getId());
  }

  public void onTeamAdded(Team team) {
    onTeamRemoved(team);
    if (teams.put(team.getId(), team) == null) {
      return;
    }
    increase(organizationRefs, team.getOrganizationId());
    team.getGroupIdsList().forEach(id -> increase(groupRefs, id));
  }

  public void onTeamRemoved(Team team) {
    if (!teams.remove(team.getId(), team)) {
      return;
    }
    team.getGroupIdsList().forEach(id -> decrease(groupRefs, id));
    decrease(organizationRefs, team.getOrganizationId());
  }

  private static void increase(Map<String, Integer> refs, String key) {
    refs.compute(key, (k, v) -> (v == null ? 0 : v) + 1);
  }

  private static void decrease(Map<String, Integer> refs, String key) {
    int value = refs.getOrDefault(key, 0) - 1;
    if (value != 0) {
      refs.put(key, value);
    } else {
      refs.remove(key);
    }
  }
}
