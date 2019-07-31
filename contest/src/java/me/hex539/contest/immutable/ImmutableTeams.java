package me.hex539.contest.immutable;

import java.util.List;
import java.util.Optional;

import edu.clics.proto.ClicsProto.*;
import me.hex539.contest.model.Teams;

/**
 * Read-only and thread-safe implementation of {@link Teams}.
 */
public class ImmutableTeams implements Teams {

  private final List<Organization> organizations;
  private final List<Group> groups;
  private final List<Team> teams;

  public static ImmutableTeams of(Teams source) {
    if (source instanceof ImmutableTeams) {
      return (ImmutableTeams) source;
    }
    return new ImmutableTeams(source);
  }

  public ImmutableTeams(Teams source) {
    organizations = SortedLists.sortBy(source.getOrganizations(), Organization::getId);
    groups = SortedLists.sortBy(source.getGroups(), Group::getId);
    teams = SortedLists.sortBy(source.getTeams(), Team::getId);
  }

  @Override
  public List<Organization> getOrganizations() {
    return organizations;
  }

  @Override
  public List<Group> getGroups() {
    return groups;
  }

  @Override
  public List<Team> getTeams() {
    return teams;
  }

  @Override
  public Optional<Organization> getOrganizationOpt(String id) {
    return SortedLists.binarySearch(organizations, org -> id.compareTo(org.getId()));
  }

  @Override
  public Optional<Group> getGroupOpt(String id) {
    return SortedLists.binarySearch(groups, group -> id.compareTo(group.getId()));
  }

  @Override
  public Optional<Team> getTeamOpt(String id) {
    return SortedLists.binarySearch(teams, team -> id.compareTo(team.getId()));
  }
}
