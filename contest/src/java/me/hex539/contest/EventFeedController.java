package me.hex539.contest;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ContestDownloader;
import me.hex539.contest.JudgementDispatcher;
import me.hex539.contest.ScoreboardModelImpl;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
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
        updateItem(
            item.getContestData(),
            item.getOperation(),
            x -> {}, // model::setContest,
            x -> {});
      } else if (item.hasJudgementTypeData()) {
          updateItem(
              item.getJudgementTypeData(),
              item.getOperation(),
              model.getJudgeModel()::onJudgementTypeAdded,
              model.getJudgeModel()::onJudgementTypeRemoved);
      } else if (item.hasLanguageData()) {
      } else if (item.hasProblemData()) {
      } else if (item.hasGroupData()) {
        updateItem(
            item.getGroupData(),
            item.getOperation(),
            model.getTeamsModel()::onGroupAdded,
            model.getTeamsModel()::onGroupRemoved);
      } else if (item.hasOrganizationData()) {
        updateItem(
            item.getOrganizationData(),
            item.getOperation(),
            model.getTeamsModel()::onOrganizationAdded,
            model.getTeamsModel()::onOrganizationRemoved);
      } else if (item.hasTeamData()) {
        updateItem(
            item.getTeamData(),
            item.getOperation(),
            model.getTeamsModel()::onTeamAdded,
            model.getTeamsModel()::onTeamRemoved);
      } else if (item.hasStateData()) {
      } else if (item.hasSubmissionData()) {
        updateItem(
            item.getSubmissionData(),
            item.getOperation(),
            dispatcher::notifySubmission,
            x -> {});
      } else if (item.hasJudgementData()) {
        dispatcher.notifyJudgement(item.getJudgementData());
      } else if (item.hasRunData()) {
      } else if (item.hasClarificationData()) {
      } else if (item.hasAwardData()) {
      }
    }

    return activity;
  }

  static <T> void updateItem(
      T data,
      EventFeedItem.Operation operation,
      Consumer<? super T> onAdd,
      Consumer<? super T> onRemove) {
    switch (operation) {
      case create:
      case update:
        onAdd.accept(data);
        break;
      case delete:
        onRemove.accept(data);
        break;
    }
  }
}

