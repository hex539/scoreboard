package me.hex539.contest.model;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import edu.clics.proto.ClicsProto.*;

public interface Teams {
  Collection<Organization> getOrganizations();
  Collection<Team> getTeams();

  default Optional<Organization> getOrganizationOpt(String id) {
    return getOrganizations().stream().filter(x -> id.equals(x.getId())).findFirst();
  }

  default Organization getOrganization(String id) throws NoSuchElementException {
    return getOrganizationOpt(id).get();
  }

  default boolean containsOrganization(String id) {
    return getOrganizationOpt(id).isPresent();
  }

  default Collection<Group> getGroups() {
    return Collections.emptyList();
  }

  default Optional<Group> getGroupOpt(String id) {
    return getGroups().stream().filter(x -> id.equals(x.getId())).findFirst();
  }

  default Group getGroup(String id) throws NoSuchElementException {
    return getGroupOpt(id).get();
  }

  default boolean containsGroup(String id) {
    return getGroupOpt(id).isPresent();
  }

  default boolean containsGroup(Group group) {
    return containsGroup(group.getId());
  }

  default Optional<Team> getTeamOpt(String id) {
    return getTeams().stream().filter(x -> id.equals(x.getId())).findFirst();
  }

  default Team getTeam(String id) throws NoSuchElementException {
    try {
      return getTeamOpt(id).get();
    } catch (NoSuchElementException e) {
      throw new NoSuchElementException("No such team: '" + id + "'");
    }
  }

  default boolean containsTeam(String id) {
    return getTeamOpt(id).isPresent();
  }

  default boolean containsTeam(Team team) {
    return containsTeam(team.getId());
  }

  interface Observer {
    default void onOrganizationAdded(Organization organization) {}
    default void onOrganizationRemoved(Organization organization) {}

    default void onGroupAdded(Group group) {}
    default void onGroupRemoved(Group group) {}

    default void onTeamAdded(Team team) {}
    default void onTeamRemoved(Team team) {}
  }
}
