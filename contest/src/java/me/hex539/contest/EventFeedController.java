package me.hex539.contest;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ContestDownloader;
import me.hex539.contest.JudgementDispatcher;
import me.hex539.contest.ScoreboardModelImpl;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.NoSuchElementException;

public class EventFeedController {
  public final ScoreboardModelImpl model;
  private final JudgementDispatcher dispatcher;
  private final BlockingQueue<Optional<EventFeedItem>> feed;

  private boolean finished = false;

  public EventFeedController(ContestDownloader contestDownloader, Contest contest)
      throws Exception {
    Optional<BlockingQueue<Optional<EventFeedItem>>> f = contestDownloader.eventFeed(contest);
    if (!f.isPresent()) {
      throw new NoSuchElementException("Event feed is not available.");
    }

    feed = f.get();

    ClicsContest entireContest = contestDownloader.fetch();
    model = ScoreboardModelImpl.newBuilder(entireContest)
        .filterGroups(g -> false)
        .filterSubmissions(s -> false)
        .build();
    dispatcher = new JudgementDispatcher(model);
    dispatcher.observers.add(model);
  }

  public boolean finished() {
    return finished;
  }

  public boolean advance() {
    if (finished) {
      return false;
    }

    boolean activity = false;
    while (true) {
      if (feed.isEmpty()) {
        break;
      }

      final Optional<EventFeedItem> otem;
      try {
        otem = feed.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      if (otem.isPresent()) {
        activity = true;
      } else {
        finished = true;
        break;
      }

      final EventFeedItem item = otem.get();

      if (item.hasContestData()) {
        // model.setContest(contest);
      } else if (item.hasJudgementTypeData()) {
        switch (item.getOperation()) {
          case create:
          case update:
            model.getJudgeModel().onJudgementTypeAdded(item.getJudgementTypeData());
            break;
          case delete:
            // model.getJudgeModel().onJudgementTypeRemoved(item.getJudgementTypeData());
            break;
        }
      } else if (item.hasLanguageData()) {
      } else if (item.hasProblemData()) {
      } else if (item.hasGroupData()) {
        switch (item.getOperation()) {
          case create:
          case update:
            model.getTeamsModel().onGroupAdded(item.getGroupData());
            break;
          case delete:
            model.getTeamsModel().onGroupRemoved(item.getGroupData());
            break;
        }
      } else if (item.hasOrganizationData()) {
        switch (item.getOperation()) {
          case create:
          case update:
            model.getTeamsModel().onOrganizationAdded(item.getOrganizationData());
            break;
          case delete:
            model.getTeamsModel().onOrganizationRemoved(item.getOrganizationData());
            break;
        }
      } else if (item.hasTeamData()) {
        switch (item.getOperation()) {
          case create:
          case update:
//            if (model.getTeamsModel().containsTeam(item.getTeamData())) {
              model.getTeamsModel().onTeamAdded(item.getTeamData());
              model.getRanklistModel().onTeamAdded(item.getTeamData());
//            }
            break;
          case delete:
            model.getRanklistModel().onTeamRemoved(item.getTeamData());
            model.getTeamsModel().onTeamRemoved(item.getTeamData());
            break;
        }
      } else if (item.hasStateData()) {
      } else if (item.hasSubmissionData()) {
        final Submission submission = item.getSubmissionData();
        if (model.getTeamsModel().containsTeam(submission.getTeamId())) {
          dispatcher.notifySubmission(item.getSubmissionData());
        }
      } else if (item.hasJudgementData()) {
        dispatcher.notifyJudgement(item.getJudgementData());
      } else if (item.hasRunData()) {
      } else if (item.hasClarificationData()) {
      } else if (item.hasAwardData()) {
      }
    }

    return activity;
  }
}

