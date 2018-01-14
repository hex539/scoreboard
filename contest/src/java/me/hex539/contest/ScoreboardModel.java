package me.hex539.contest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import edu.clics.proto.ClicsProto.*;

public interface ScoreboardModel extends Judge, Teams, Problems {

  public interface Observer {
    default void setModel(ScoreboardModel model) {}
    default void onProblemSubmitted(Team team, Submission submission) {}
    default void onSubmissionJudged(Team team, Judgement judgement) {}
    default void onProblemScoreChanged(Team team, ScoreboardProblem problem) {}
    default void onScoreChanged(Team team, ScoreboardScore score) {}
    default void onTeamRankChanged(Team team, int oldRank, int newRank) {}
  }

  List<ScoreboardRow> getRows();
  Contest getContest();

  default ScoreboardRow getRow(long index) throws NoSuchElementException {
    return getRows().get((int) index);
  }

  default ScoreboardRow getRow(Team team) throws NoSuchElementException {
    return getRows().stream().filter(x -> team.getId().equals(x.getTeamId())).findFirst().get();
  }

  default ScoreboardScore getScore(Team team) throws NoSuchElementException {
    return getRow(team).getScore();
  }

  default ScoreboardProblem getAttempts(Team team, Problem problem) throws NoSuchElementException {
    try {
      return getRow(team).getProblemsList().stream()
              .filter(x -> x.getProblemId().equals(problem.getId()))
              .findFirst()
              .orElseThrow(
                  () -> new NoSuchElementException("Cannot find problem " + problem.getLabel()));
    } catch (Throwable fromOrElseThrow) {
      throw (NoSuchElementException) fromOrElseThrow;
    }
  }
}

interface Teams {
  Collection<Organization> getOrganizations();
  Collection<Team> getTeams();

  default Organization getOrganization(String id) throws NoSuchElementException {
    return getOrganizations().stream().filter(x -> id.equals(x.getId())).findFirst().get();
  }

  default Collection<Group> getGroups() {
    return Collections.emptyList();
  }

  default Group getGroup(String id) throws NoSuchElementException {
    return getGroups().stream().filter(x -> id.equals(x.getId())).findFirst().get();
  }

  default Team getTeam(String id) throws NoSuchElementException {
    return getTeams().stream().filter(x -> id.equals(x.getId())).findFirst().get();
  }
}

interface Judge {
  default  JudgementType getJudgementType(String id) {
    throw new UnsupportedOperationException();
  }
  
  default List<Judgement> getJudgements() {
    return Collections.emptyList();
  }

  default List<Submission> getSubmissions() {
    return Collections.emptyList();
  }

  default Submission getSubmission(String id) throws NoSuchElementException {
    return getSubmissions().stream().filter(x -> id.equals(x.getId())).findFirst().get();
  }
}

interface Problems {
  List<Problem> getProblems();

  default Problem getProblem(String id) throws NoSuchElementException {
    return getProblems().stream().filter(x -> id.equals(x.getId())).findFirst().get();
  }
}
