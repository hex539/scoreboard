package me.hex539.resolver;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.auto.value.AutoValue;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;

import org.lwjgl.glfw.GLFWVidMode;

public class Renderer implements ResolverController.Observer {
  private static boolean ENABLE_PARTICLES = true;

  private final ScoreboardModel model;
  private final Queue<RankAnimation> moveAnimation = new ArrayDeque<>();
  private final Queue<RankAnimation> scrollAnimation = new ArrayDeque<>();
  private final Queue<RankAnimation> focusAnimation = new ArrayDeque<>();
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
  double visibleRowsBelow = 7.4;

  double minScrolledRank;
  double maxScrolledRank;

  private Team focusedTeam;
  private Problem focusedProblem;
  private int focusedRank;
  private int finalisedRank;
  private boolean dirtyParticles;

  public Renderer(ScoreboardModel model, CompletableFuture<? extends ByteBuffer> ttfData) {
    this.model = model;

    this.particles = ENABLE_PARTICLES ? new Particles() : null;
    this.font = new FontRenderer(model, ttfData);

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

    cellWidth = (int) ((screenWidth * 0.7) / Math.max(12, model.getProblems().size())) * 0.9;
    cellHeight = cellWidth / 2.4;
    cellMargin = cellWidth / 10;

    teamLabelWidth = screenWidth - (cellWidth + cellMargin) * model.getProblems().size();

    rowWidth = (cellWidth + cellMargin) * model.getProblems().size();
    rowHeight = cellHeight + cellMargin;

    minScrolledRank = + (1*screenHeight + cellMargin / 2.0) / (rowHeight + cellMargin)
         - visibleRowsBelow;
    maxScrolledRank = Math.max(minScrolledRank,
        + (0*screenHeight + cellMargin / 2.0) / (rowHeight + cellMargin)
             - visibleRowsBelow + model.getRows().size());

    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glViewport(0, 0, videoMode.width(), videoMode.height());
    glOrtho(
        0, videoMode.width(),
        0, videoMode.height(),
        0.0, -1.0);
  }

  @Override
  public void onProblemFocused(Team team, Problem problem) {
    if (focusedTeam != team) {
      if (team != null && problem == null) {
        final int oldRank = (focusedTeam != null ? (int) model.getRow(focusedTeam).getRank() : -1);
        final int newRank = (int) model.getRow(team).getRank();
        if (!focusAnimation.isEmpty()) {
          focusAnimation.clear();
        }
        final long delay = TimeUnit.MILLISECONDS.toNanos(0);
        final long duration = TimeUnit.MILLISECONDS.toNanos(180);
        focusAnimation.offer(RankAnimation.create(
            oldRank, newRank, System.nanoTime() + delay, duration));
        scrollTo(newRank);
      }
    }

    focusedTeam = team;
    focusedProblem = problem;
  }

  @Override
  public void onProblemScoreChanged(Team team, ScoreboardProblem attempts) {
    dirtyParticles = true;
  }

  @Override
  public void onTeamRankChanged(Team team, int rankFrom, int rankTo) {
    final int difference = Math.abs(rankTo - rankFrom);
    final long delay, duration;
    if (moveAnimation.size() < 4) {
      delay = TimeUnit.MILLISECONDS.toNanos(300);
      duration = TimeUnit.MILLISECONDS.toNanos((long) (300 * Math.sqrt(Math.min(10, difference))));
    } else {
      delay = TimeUnit.MILLISECONDS.toNanos(0);
      duration = TimeUnit.MILLISECONDS.toNanos((long) (150 * Math.sqrt(Math.min(10, difference))));
    }
    moveAnimation.offer(RankAnimation.create(
        rankFrom, rankTo, System.nanoTime() + delay, duration));
  }

  @Override
  public void onTeamRankFinalised(Team team, int rank) {
    finalisedRank = rank;
    focusedProblem = null;
  }

  private void scrollTo(int rank) {
    if (rank != focusedRank) {
      final long duration = TimeUnit.MILLISECONDS.toNanos(200);
      scrollAnimation.offer(RankAnimation.create(focusedRank, rank, System.nanoTime(), duration));
      focusedRank = rank;
    }
  }

  public boolean mainLoop(final long timeNow) {
    boolean animating = false;
    animating |= (peekAnimation(scrollAnimation, timeNow) != null);
    animating |= (peekAnimation(moveAnimation, timeNow) != null);

    double scrolledRank = (double) focusedRank;
    for (RankAnimation anim : scrollAnimation) {
      scrolledRank += slerp((double) (anim.fromRank() - anim.toRank()), 0.0, anim.progress(timeNow));
    }
    scrolledRank = clamp(scrolledRank, minScrolledRank, maxScrolledRank);
    final double baseY = (scrolledRank + visibleRowsBelow) * (rowHeight + cellMargin) + cellMargin;
    if (particles != null) {
      particles.setOffset(0.0, baseY);
    }

    List<RankAnimation> moveAnims = new ArrayList<>(moveAnimation);
    Collections.reverse(moveAnims);

    for (int rowIndex = model.getRows().size(); rowIndex --> 0;) {
      final ScoreboardRow row = model.getRow(rowIndex);
      final boolean teamFocused = focusedTeam != null && row.getTeamId() == focusedTeam.getId();

      // If we have multiple animations at once, we need to unwind the history of
      // this team's rank as we go back through the stack of changes.
      long rankForAnim = row.getRank();

      double eccentricity = 0.0;
      for (RankAnimation anim : moveAnims) {
        double factor = 0.0;
        if (rankForAnim == anim.toRank()) {
          eccentricity += slerp(anim.fromRank() - anim.toRank(), 0.0, anim.progress(timeNow));
          rankForAnim += anim.fromRank() - anim.toRank();
        } else if (anim.fromRank() >= rankForAnim && rankForAnim >= anim.toRank()) {
          eccentricity += 1.0 * slerp(-1.0, 0.0, anim.progress(timeNow));
          rankForAnim -= 1;
        }
      }

      final double effectiveRank = row.getRank() + eccentricity;

      final double rowX = (screenWidth - teamLabelWidth - rowWidth) / 2.0 + teamLabelWidth;
      final double rowY = baseY - effectiveRank * (rowHeight + cellMargin) - cellMargin;

      if (0 <= rowY + rowHeight && rowY <= screenHeight) {
        drawRow(rowX, rowY, row, teamFocused, teamFocused ? focusedProblem: null, timeNow);
      }
    }

    if (particles != null && particles.update(timeNow)) {
      particles.draw();
      animating = true;
    }

    return animating;
  }

  private void drawRow(
      double rowX,
      double rowY,
      ScoreboardRow row,
      boolean teamFocused,
      Problem problemFocused,
      long timeNow) {

    RankAnimation focus = peekAnimation(focusAnimation, timeNow);

    if (teamFocused) {
      drawFocus(rowX, rowY, focus != null ? focus.progress(timeNow) : 1.0);
    } else {
      if (row.getRank() >= finalisedRank) {
        drawZebra(rowX, rowY, row.getRank());
      }
      if (focus != null && focus.fromRank() == row.getRank()) {
        drawFocus(rowX, rowY + rowHeight + cellMargin, focus.progress(timeNow) - 1.0);
      }
    }

    if (row.getRank() >= finalisedRank) {
      drawRank(rowX, rowY, row.getRank(), teamFocused);
    }
    drawLabel(rowX, rowY, model.getTeam(row.getTeamId()), teamFocused);
    drawScore(rowX, rowY, row.getScore(), teamFocused);

    int i = 0;
    for (ScoreboardProblem attempts : row.getProblemsList()) {
      final double cellX = rowX + (cellWidth + cellMargin) * i;
      final double cellY = rowY + cellMargin / 2.0;
      final boolean focused =
          problemFocused != null && problemFocused.getId().equals(attempts.getProblemId());
      drawAttempts(cellX, cellY, attempts, focused);
      i++;
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

  private void drawFocus(double rowX, double rowY, double progress) {
    final double y = rowY - cellMargin / 2;
    final double h = (rowHeight + cellMargin) * progress;

    glColor3d(0.0, 0.33, 0.48);
    glBegin(GL_QUADS);
    glVertex2d(0, y);
    glVertex2d(screenWidth, y);
    glVertex2d(screenWidth, y + h);
    glVertex2d(0, y + h);
    glEnd();
  }

  private void drawZebra(double rowX, double rowY, long rank) {
    final float intensity = (rank % 2 == 1 ? 0.05f: 0.1f);
    final float r = 0.2f, g = 0.2f, b = 0.2f;
    glColor3f(-intensity + r, -intensity + g, -intensity + b);
    glBegin(GL_QUADS);
    glVertex2d(0,rowY - cellMargin / 2);
    glVertex2d(screenWidth, rowY - cellMargin / 2);
    glVertex2d(screenWidth, rowY+rowHeight + cellMargin / 2);
    glVertex2d(0,rowY+rowHeight + cellMargin / 2);
    glEnd();
  }

  private void drawRank(double rowX, double rowY, long rank, boolean focused) {
    glColor3d(0.4, 0.4, 0.4);
    font.drawText(
        rowX - teamLabelWidth * 0.9 - cellMargin,
        rowY + cellMargin * 1.5,
        (int) (cellHeight - cellMargin),
        Long.toString(rank),
        FontRenderer.Alignment.RIGHT);
  }

  private void drawLabel(double rowX, double rowY, Team team, boolean focused) {
    glColor3d(0.6, 0.6, 0.6);
    final String organizationId = team.getOrganizationId();
    if (organizationId != null && !organizationId.isEmpty()) {
        try {
          font.drawText(
              rowX - teamLabelWidth * 0.9,
              rowY + cellMargin,
              (int) (rowHeight / 2.0 - cellMargin),
              model.getOrganization(organizationId).getName());
        } catch (NoSuchElementException noSuchOrganization) {
          // Team has an organisation but it's missing... FIXME, find out why some teams have
          // organization=0 despite no such organization existing.
        }
    }
    glColor3d(1.0, 1.0, 1.0);
    font.drawText(
        rowX - teamLabelWidth * 0.9,
        rowY + rowHeight / 2.0,
        (int) (rowHeight / 2.0),
        team.getName());
  }

  private void drawScore(double rowX, double rowY, ScoreboardScore score, boolean focused) {
    if (score.getNumSolved() != 0) {
      glColor3d(0.6, 0.6, 0.6);
      font.drawText(
          rowX - rowHeight * 0.1,
          rowY + cellMargin / 2.0,
          (int) (rowHeight / 2.0 - cellMargin / 2.0),
          String.format("%4d", score.getTotalTime()),
          FontRenderer.Alignment.RIGHT);
    }
    if (focused) {
      glColor3d(1.0, 1.0, 1.0);
    } else {
      glColor3d(0.4, 0.4, 0.4);
    }
    font.drawText(
        rowX - rowHeight * 0.1,
        rowY + rowHeight / 2.0,
        (int) (rowHeight / 2.0),
        String.format("%2d", score.getNumSolved()),
        FontRenderer.Alignment.RIGHT);
  }

  private void drawAttempts(double cellX, double cellY, ScoreboardProblem attempts, boolean focused) {
    final boolean pending = (attempts.getNumPending() > 0);
    final boolean attempted = (attempts.getNumJudged() > 0 || pending);
    final String text;

    int totalAttempts = attempts.getNumJudged() + attempts.getNumPending();
    final String subText;
    switch (totalAttempts) {
      case 0: subText = null; break;
      case 1: subText = Integer.toString(totalAttempts) + " " + FontRenderer.Symbols.TRIES_ONE; break;
      default: subText = Integer.toString(totalAttempts) + " " + FontRenderer.Symbols.TRIES_MANY; break;
    }

    final float r, g, b;
    if (attempts.getSolved()) {
      r = 0.0f; b = 0.0f; g = (175.0f / 255.0f);
      text = FontRenderer.Symbols.CORRECT;
    } else if (attempts.getNumPending() > 0) {
      r = 0.25f; g = 0.25f; b = 1.0f;
      text = FontRenderer.Symbols.PENDING;
    } else if (attempts.getNumJudged() > 0) {
      r = (207.0f / 255.0f); g = 0.0f; b = 0.0f;
      text = FontRenderer.Symbols.WRONG;
    } else {
      r = 0.025f; g = 0.025f; b = 0.025f;
      text = null;
    }

    // TODO: minor race condition because of firing from inside UI code which
    // does not make any guarantees about running at the right time. Trigger
    // this directly inside the state change.
    if (focused && !pending && particles != null && dirtyParticles) {
      for (int i = 0; i < 2000; i++) {
        double vx = Math.random() - 0.5;
        double vy = Math.random() - 0.5;
        double vang = (Math.random() - 0.5) * Math.PI * 10;
        double x = cellX + (vx * 0.9 + 0.5) * cellWidth;
        double y = cellY + (vy * 0.9 + 0.5) * cellHeight;
        double ang = Math.random() * Math.PI * 2.0;
        vx += (Math.random() - 0.5) * 0.75;
        vy += (Math.random() - 0.5) * 0.75;
        float p = (float) Math.random();
        double h = Math.sqrt(vx*vx + vy*vy + 1e-9);
        double l = (screenWidth / 25) * (0.5 + 1.0/(0.5 + p) + Math.random());
        vx *= (l / h);
        vy *= (l / h);
        vy += cellHeight * (attempts.getSolved() ? 2 : 1);
        particles.add(x, y, ang, vx, vy, vang,
          Math.min(1.0f, r+p*(r+g*(0.7152f/0.2126f))),
          Math.min(1.0f, g+p*(g+r*(0.2126f/0.7152f)+b*(0.0722f/0.7152f))),
          Math.min(1.0f, b+p*(b+g*(0.7152f/0.2126f))));
      }
      dirtyParticles = false;
    }

    if (focused) {
      if (pending) {
        glColor3f(r, g, b);
      } else {
        glColor3f(1.0f, 1.0f, 1.0f);
      }
      glBegin(GL_QUADS);
      glVertex2d(cellX-cellMargin/4,cellY-cellMargin/4);
      glVertex2d(cellX+cellWidth+cellMargin/4,cellY-cellMargin/4);
      glVertex2d(cellX+cellWidth+cellMargin/4,cellY+cellHeight+cellMargin/4);
      glVertex2d(cellX-cellMargin/4,cellY+cellHeight+cellMargin/4);
      glEnd();
    }

    if (!attempted) {
      glEnable(GL_BLEND);

      glBegin(GL_TRIANGLE_FAN);

      final double shWidth = cellHeight / 8.0;
      final double shHeight = cellHeight / 4.0;

      glColor4f(0f, 0f, 0f, 0.7f);
      glVertex2d(cellX, cellY + cellHeight);
      glVertex2d(cellX+cellWidth, cellY + cellHeight);
      glColor4f(0.1f, 0.1f, 0.1f, 0.6f);
      glVertex2d(cellX+cellWidth, cellY+cellHeight-shHeight);
      glVertex2d(cellX+shWidth, cellY+cellHeight-shHeight);
      glVertex2d(cellX+shWidth, cellY);
      glColor4f(0f, 0f, 0f, 0.7f);
      glVertex2d(cellX, cellY);
      glEnd();

      glColor4f(0.1f, 0.1f, 0.1f, 0.6f);
      glBegin(GL_QUADS);
      glVertex2d(cellX+cellWidth, cellY+cellHeight-shHeight);
      glVertex2d(cellX+shWidth, cellY+cellHeight-shHeight);
      glVertex2d(cellX+shWidth, cellY);
      glVertex2d(cellX+cellWidth, cellY);
      glEnd();

      glDisable(GL_BLEND);
    } else {
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

    if (text != null) {
      if (focused && pending) {
        glColor3f(r, g, b);
      } else {
        glColor3f(1.0f, 1.0f, 1.0f);
      }
      font.drawText(
        cellX + cellWidth * 0.5,
        cellY + cellHeight * 0.4,
        (int) (cellHeight * 0.6),
        text,
        FontRenderer.Alignment.CENTRE);
      if (subText != null) {
        font.drawText(
            cellX + cellWidth * 0.5,
            cellY + cellHeight * 0.1,
            (int) (cellHeight * 0.4),
            subText,
            FontRenderer.Alignment.CENTRE);
      }
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
