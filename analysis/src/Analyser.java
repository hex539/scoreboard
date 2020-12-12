package me.hex539.analysis;

import edu.clics.proto.ClicsProto.*;
import me.hex539.contest.ApiDetective;
import me.hex539.contest.ContestConfig;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Analyser {

  public static ContestConfig.Source getSource(Invocation invocation) {
    if (invocation.getUrl() != null) {
      ContestConfig.Source.Builder sourceBuilder =
          ApiDetective.detectApi(invocation.getUrl()).get()
              .toBuilder();
      if (invocation.getContest() != null) {
        sourceBuilder.setContestId(invocation.getContest());
      }
      if (invocation.getUsername() != null) {
          sourceBuilder.setAuthentication(
              ContestConfig.Authentication.newBuilder()
                  .setHttpUsername(invocation.getUsername())
                  .setHttpPassword(invocation.getPassword())
                  .build());
      }
      return sourceBuilder.build();
    }

    if (invocation.getFile() != null) {
      return ContestConfig.Source.newBuilder()
          .setFilePath(invocation.getFile())
          .build();
    }

    System.err.println("Need one of --url or --file to load a contest");
    System.exit(1);
    return null;
  }

  public static Predicate<Group> getGroupPredicate(Invocation invocation, ClicsContest contest) {
    if (invocation.getGroups() != null) {
      final Set<String> groupIds =
          Arrays.stream(invocation.getGroups().split(",")).collect(Collectors.toSet());
      return (Group group) -> groupIds.contains(group.getId());
    }

   return (Group group) -> !group.getHidden();
  }

  public static Predicate<Group> getMostPopularGroup(ClicsContest contest) {
    String mostPopularGroup =
        contest.getTeamsMap().values().stream().flatMap(t -> t.getGroupIdsList().stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
                .filter(entry -> !contest.getGroups().get(entry.getKey()).getHidden())
                .max(Map.Entry.comparingByValue())
                .get().getKey();
    return mostPopularGroup != null
        ? (Group group) -> mostPopularGroup.equals(group.getId())
        : (Group group) -> true;
  }
}
