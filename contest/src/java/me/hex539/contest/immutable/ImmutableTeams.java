package me.hex539.contest.immutable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

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
    organizations = sortBy(source.getOrganizations(), Organization::getId);
    groups = sortBy(source.getGroups(), Group::getId);
    teams = sortBy(source.getTeams(), Team::getId);
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
    return binarySearch(organizations, org -> id.compareTo(org.getId()));
  }

  @Override
  public Optional<Group> getGroupOpt(String id) {
    return binarySearch(groups, group -> id.compareTo(group.getId()));
  }

  @Override
  public Optional<Team> getTeamOpt(String id) {
    return binarySearch(teams, team -> id.compareTo(team.getId()));
  }

  private static <T> Optional<T> binarySearch(List<T> items, Function<T, Integer> compareTo) {
    final T res;

    if (!items.isEmpty()) {
      int l = 0;
      for (int rad = (1 << 30); rad != 0; rad >>>= 1) {
        if (l + rad < items.size() && compareTo.apply(items.get(l + rad)) >= 0) {
          l += rad;
        }
      }
      res = items.get(l);
    } else {
      res = null;
    }

    if (res != null && compareTo.apply(res) == 0) {
      return Optional.ofNullable(res);
    } else {
      return Optional.empty();
    }
  }

  private static <T, K extends Comparable<K>> List<T> sortBy(Collection<T> l, Function<T, K> key) {
    List<T> list = new ArrayList<>(l);
    Collections.sort(list, (a, b) -> key.apply(a).compareTo(key.apply(b)));
    return Collections.unmodifiableList(list);
  }
}
