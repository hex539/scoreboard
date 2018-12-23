package me.hex539.resolver;

import com.google.auto.value.AutoValue;

import java.util.concurrent.TimeUnit;
import java.util.Queue;
import java.util.ArrayDeque;


import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;

import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Renderer implements ResolverController.Observer {
  private static boolean ENABLE_PARTICLES = true;

  private final ScoreboardModel model;
  private final Queue<RankAnimation> moveAnimation = new ArrayDeque<>();
  private final Queue<RankAnimation> scrollAnimation = new ArrayDeque<>();
  private final Particles particles;
  private final FontRenderer font;

  double screenWidth;
  double screenHeight;

  double cellWidth;
  double cellHeight;
  double cellMargin;

  double teamLabelWidth;

  double rowWidth;
  double rowHeight;
  long visibleRowsBelow = 5;

  double minScrolledRank;
  double maxScrolledRank;

  private Team focusedTeam;
  private Problem focusedProblem;
  private int focusedRank;
  private int finalisedRank;
  private boolean dirtyParticles;

  public Renderer(ScoreboardModel model) {
    this.model = model;

    this.particles = ENABLE_PARTICLES ? new Particles() : null;
    this.font = new FontRenderer(model);

    focusedTeam = null;
    focusedProblem = null;
    focusedRank = model.getRows().size();
    finalisedRank = focusedRank + 1;
  }

  public void setVideoMode(GLFWVidMode videoMode) {
    screenWidth = videoMode.width();
    screenHeight = videoMode.height();
    if (particles != null) {
      particles.setVideoSize(screenWidth, screenHeight);
    }
    if (font != null) {
      font.setVideoSize(screenWidth, screenHeight);
    }

    teamLabelWidth = screenWidth / 3.0;

    cellWidth = (int) ((screenWidth - teamLabelWidth - cellMargin * 4) / Math.max(4, model.getProblems().size())) * 0.9;
    cellHeight = cellWidth / (1.0 + Math.sqrt(5));
    cellMargin = cellWidth / 10;

    rowWidth = (cellWidth + cellMargin) * model.getProblems().size();
    rowHeight = cellHeight + cellMargin;

    minScrolledRank = + (1*screenHeight + 1*cellMargin) / (rowHeight + cellMargin)
         - visibleRowsBelow;
    maxScrolledRank = Math.max(minScrolledRank,
        + (0*screenHeight + 2*cellMargin) / (rowHeight + cellMargin)
             - visibleRowsBelow + model.getRows().size());

    glClearColor(0.1f, 0.1f, 0.1f, 0.1f);
    glViewport(0, 0, videoMode.width(), videoMode.height());
    glOrtho(
        0, videoMode.width(),
        0, videoMode.height(),
        0.0, -1.0);
  }

  @Override
  public void onProblemFocused(Team team, Problem problem) {
    focusedTeam = team;
    focusedProblem = problem;
    if (team != null && problem == null) {
      scrollTo((int) model.getRow(team).getRank());
    }
  }

  @Override
  public void onProblemScoreChanged(Team team, ScoreboardProblem attempts) {
    dirtyParticles = true;
  }

  @Override
  public void onTeamRankChanged(Team team, int rankFrom, int rankTo) {
    final int difference = Math.abs(rankTo - rankFrom);
    final long delay = TimeUnit.MILLISECONDS.toNanos(500);
    final long duration = TimeUnit.MILLISECONDS.toNanos(500);
    moveAnimation.offer(RankAnimation.create(
        rankFrom, rankTo, System.nanoTime() + duration / 2, duration));
  }

  @Override
  public void onTeamRankFinalised(Team team, int rank) {
    finalisedRank = rank;
  }

  private double getScrolledRank(RankAnimation anim, long timeNow) {
    final double scrolledRank = anim != null
        ? slerp((double) anim.fromRank(), (double) anim.toRank(), anim.progress(timeNow))
        : (double) focusedRank;
    return clamp(scrolledRank, minScrolledRank, maxScrolledRank);
  }

  private void scrollTo(int rank) {
    if (rank != focusedRank) {
      final long duration = TimeUnit.MILLISECONDS.toNanos(200);
      scrollAnimation.offer(RankAnimation.create(focusedRank, rank, System.nanoTime(), duration));
      focusedRank = rank;
    }
  }

  public boolean mainLoop(final long timeNow) {
    final RankAnimation scrollAnim = peekAnimation(scrollAnimation, timeNow);
    final RankAnimation moveAnim = peekAnimation(moveAnimation, timeNow);

    final double scrolledRank = getScrolledRank(scrollAnim, timeNow);
    final double baseY = (scrolledRank + visibleRowsBelow) * (rowHeight + cellMargin);
    if (particles != null) {
      particles.setOffset(0.0, baseY);
    }

    for (int rowIndex = model.getRows().size(); rowIndex --> 0;) {
      final ScoreboardRow row = model.getRows().get(rowIndex);
      final boolean teamFocused = focusedTeam != null && row.getTeamId() == focusedTeam.getId();

      final double effectiveRank;
      if (moveAnim != null) {
        final long fromRank =
            row.getRank() == moveAnim.toRank()
                ? moveAnim.fromRank()
                : row.getRank()
                    + (moveAnim.fromRank() < row.getRank() ? 1 : 0)
                    - (moveAnim.toRank() < row.getRank() ? 1 : 0);
        effectiveRank = slerp((double) fromRank, (double) row.getRank(), moveAnim.progress(timeNow));
      } else {
        effectiveRank = (double) row.getRank();
      }

      final double rowX = (screenWidth - teamLabelWidth - rowWidth) / 2.0 + teamLabelWidth;
      final double rowY = baseY - effectiveRank * (rowHeight + cellMargin) - cellMargin;

      if (0 <= rowY + rowHeight && rowY <= screenHeight) {
        drawRow(rowX, rowY, row, teamFocused, teamFocused ? focusedProblem: null);
      }
    }

    if (particles != null && particles.update(timeNow)) {
      particles.draw();
      return true;
    }

    return scrollAnim != null || moveAnim != null;
  }

  private void drawRow(
      double rowX,
      double rowY,
      ScoreboardRow row,
      boolean teamFocused,
      Problem problemFocused) {
    if (teamFocused) {
      drawFocus(rowX, rowY);
    }
    if (row.getRank() >= finalisedRank) {
      drawRank(rowX, rowY, row.getRank());
    }
    drawLabel(rowX, rowY, model.getTeam(row.getTeamId()), teamFocused);
    drawScore(rowX, rowY, row.getScore(), teamFocused);

    for (int i = 0; i < row.getProblemsList().size(); i++) {
      final ScoreboardProblem attempts = row.getProblemsList().get(i);
      final double cellX = rowX + (cellWidth + cellMargin) * i;
      final double cellY = rowY + cellMargin / 2.0;
      final boolean focused =
          problemFocused != null && problemFocused.getId().equals(attempts.getProblemId());
      drawAttempts(cellX, cellY, attempts, focused);
    }
  }

  private static RankAnimation peekAnimation(Queue<RankAnimation> queue, long timeNow) {
    while (!queue.isEmpty()) {
      RankAnimation anim = queue.peek();
      if (anim == null || timeNow < anim.toTime()) {
        return anim;
      }
      queue.poll();
    }
    return null;
  }

  private void drawFocus(double rowX, double rowY) {
    glColor3d(0.1, 0.1, 0.5);
    glBegin(GL_QUADS);
    glVertex2d(0,rowY - cellMargin / 2);
    glVertex2d(screenWidth, rowY - cellMargin / 2);
    glVertex2d(screenWidth, rowY+rowHeight + cellMargin / 2);
    glVertex2d(0,rowY+rowHeight + cellMargin / 2);
    glEnd();
  }

  private void drawRank(double rowX, double rowY, long rank) {
    glColor3d(0.4, 0.4, 0.4);
    font.drawText(
        rowX - teamLabelWidth,
        rowY + cellMargin / 2.0,
        (int) (cellHeight - cellMargin),
        String.format("%-2d", rank));
  }

  private void drawLabel(double rowX, double rowY, Team team, boolean focused) {
    glColor3d(0.6, 0.6, 0.6);
    font.drawText(rowX - teamLabelWidth * 0.9, rowY + cellMargin / 2.0, (int) (cellHeight * 0.25), model.getOrganization(team.getOrganizationId()).getName());
    if (focused) {
      glColor3d(1.0, 1.0, 1.0);
    }
    font.drawText(rowX - teamLabelWidth * 0.9, rowY + rowHeight / 2.0, (int) (cellHeight * 0.4), team.getName());
  }

  private void drawScore(double rowX, double rowY, ScoreboardScore score, boolean focused) {
    if (focused) {
      glColor3d(1.0, 1.0, 1.0);
    } else {
      glColor3d(0.4, 0.4, 0.4);
    }
    font.drawText(
        rowX - teamLabelWidth * 0.05,
        rowY + cellMargin,
        (int) (cellHeight - cellMargin),
        String.format("%-3d", score.getNumSolved()));
  }

  private void drawAttempts(double cellX, double cellY, ScoreboardProblem attempts, boolean focused) {
    final boolean pending = (attempts.getNumPending() > 0);
    final boolean attempted = (attempts.getNumJudged() > 0 || pending);
    final String text;

    float r = 0.0f, g = 0.0f, b = 0.0f;
    if (attempts.getSolved()) {
      g = 0.8f;
      text = (attempts.getNumJudged() == 1 ? "+" : String.format("%d", attempts.getNumJudged() - 1));
    } else if (attempts.getNumPending() > 0) {
      b = 1.0f;
      text = "?";
    } else if (attempts.getNumJudged() > 0) {
      r = 1.0f;
      text = String.format("-%d", attempts.getNumJudged());
    } else {
      r = 0.025f; g = 0.025f; b = 0.025f;
      text = null;
    }

    // TODO: minor race condition because of firing from inside UI code which
    // does not make any guarantees about running at the right time. Trigger
    // this directly inside the state change.
    if (focused && !pending && particles != null && dirtyParticles) {
      for (int i = 0; i < 4000; i++) {
        double vx = Math.random() - 0.5;
        double vy = Math.random() - 0.5;
        double x = cellX + (vx + 0.5) * cellWidth;
        double y = cellY + (vy + 0.5) * cellHeight;
        vx += Math.random() - 0.5;
        vy += Math.random() - 0.5;
        double h = Math.sqrt(vx*vx + vy*vy + 1e-9);
        vx *= 200 / h;
        vy *= 200 / h;
        particles.add(x, y, vx, vy, r, g, b);
      }
      dirtyParticles = false;
    }

    if (focused) {
      if (pending) {
        glColor3f(0.0f, 0.0f, 1.0f);
      } else {
        glColor3f(1.0f, 1.0f, 1.0f);
      }
      glBegin(GL_QUADS);
      glVertex2d(cellX-cellMargin/2,cellY-cellMargin/2);
      glVertex2d(cellX+cellWidth+cellMargin/2,cellY-cellMargin/2);
      glVertex2d(cellX+cellWidth+cellMargin/2,cellY+cellHeight+cellMargin/2);
      glVertex2d(cellX-cellMargin/2,cellY+cellHeight+cellMargin/2);
      glEnd();
    }

    if (true) {
      if (focused && pending) {
        glColor3f(1.0f, 1.0f, 1.0f);
      } else {
        glColor3f(r, g, b);
      }
      glBegin(GL_QUADS);
      glVertex2d(cellX,cellY);
      glVertex2d(cellX+cellWidth,cellY);
      glVertex2d(cellX+cellWidth,cellY+cellHeight);
      glVertex2d(cellX,cellY+cellHeight);
      glEnd();
    }

    if (!attempted) {
      glColor3f(0.05f, 0.05f, 0.05f);
      glBegin(GL_QUADS);
      glVertex2d(cellX+cellHeight/8.0, cellY);
      glVertex2d(cellX+cellWidth, cellY);
      glVertex2d(cellX+cellWidth, cellY+cellHeight-cellHeight/8.0);
      glVertex2d(cellX+cellHeight/8.0, cellY+cellHeight-cellHeight/8.0);
      glEnd();
    }

    if (text != null) {
      if (focused && pending) {
        glColor3f(r, g, b);
      } else {
        glColor3f(1.0f, 1.0f, 1.0f);
      }
      font.drawText(cellX+cellWidth/2 - cellHeight * 0.1, cellY + cellHeight * 0.3, (int) (cellHeight * 0.4), text);
    }
  }

  private static double clamp(double x, double a, double b) {
    if (x < a) return a;
    if (x > b) return b;
    return x;
  }

  private static double slerp(double a, double b, double x) {
    final double y = 0.5 + 0.5 * Math.cos(x * Math.PI);
    return (a - b) * y + b;
  }

  @AutoValue
  public static abstract class RankAnimation {
    abstract long fromRank();
    abstract long fromTime();

    abstract long toRank();
    abstract long toTime();

    public static RankAnimation create(long from, long to, long now, long duration) {
      return new AutoValue_Renderer_RankAnimation(from, now, to, now + duration);
    }

    public final double progress(long timeNow) {
      return clamp((timeNow - fromTime()) / (double) (toTime() - fromTime()), 0.0, 1.0);
    }
  }
}
