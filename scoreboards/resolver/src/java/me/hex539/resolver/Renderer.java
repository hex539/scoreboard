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
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.auto.value.AutoValue;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ScoreboardModel;
import me.hex539.contest.ResolverController;
import me.hex539.resolver.draw.AttemptColour;
import me.hex539.resolver.draw.DebugDraw;
import me.hex539.resolver.draw.FontRenderer;
import me.hex539.resolver.layout.Layout;

import org.lwjgl.glfw.GLFWVidMode;

public class Renderer implements ResolverController.Observer {
  private static boolean ENABLE_PARTICLES = true;
  private static boolean ENABLE_WIREFRAME = false;

  private final ScoreboardModel model;
  private final Queue<RankAnimation> moveAnimation = new ArrayDeque<>();
  private final Queue<RankAnimation> scrollAnimation = new ArrayDeque<>();
  private final Queue<RankAnimation> focusAnimation = new ArrayDeque<>();
  private final Particles particles;
  private final FontRenderer font;

  float cellWidth;
  float cellHeight;
  float cellMargin;

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

  private final Layout.Group screen = new Layout.Group();
  private final Layout.Group rootLayout = new Layout.Group();
  private final Map<String, Layout.Group> rowLayouts = new HashMap<>();
  private long lastTime = -1L;

  public Renderer(ScoreboardModel model, CompletableFuture<? extends ByteBuffer> ttfData) {
    this.model = model;

    this.particles = ENABLE_PARTICLES ? new Particles() : null;
    this.font = new FontRenderer(model, ttfData);

    for (ScoreboardRow row : model.getRows()) {
      final String teamId = row.getTeamId();
      final Team team = model.getTeam(teamId);
      final Layout.Group rowLayout = new Layout.Group(ENABLE_WIREFRAME
          ? DebugDraw::wireframe
          : l -> drawRow(l, model.getRow(team)));
      final Layout.Group scoreLayout = new Layout.Group(ENABLE_WIREFRAME
          ? DebugDraw::wireframe
          : null);
      for (ScoreboardProblem attempts : row.getProblemsList()) {
        final Problem problem = model.getProblem(attempts.getProblemId());
        final Layout cellLayout = new Layout(ENABLE_WIREFRAME
            ? DebugDraw::wireframe
            : l -> drawAttempts(l, team, model.getAttempts(team, problem)));
        scoreLayout.children.add(cellLayout);
      }
      rowLayout.children.add(scoreLayout);
      rootLayout.children.add(rowLayout);
      rowLayouts.put(teamId, rowLayout);
    }

    focusedTeam = null;
    focusedProblem = null;
    focusedRank = model.getRows().size();
    finalisedRank = focusedRank + 1;
  }

  public void setVideoMode(final int screenWidth, final int screenHeight) {
    screen.width = screenWidth;
    screen.height = screenHeight;

    rootLayout.width = screen.width;
    rootLayout.height = screen.height;

    if (particles != null) {
      particles.setVideoSize(screen.width, screen.height);
    }
    if (font != null) {
      font.setVideoSize(screen.width, screen.height);
    }

    cellWidth = (int) ((screen.width * 0.7) / Math.max(12, model.getProblems().size())) * 0.9f;
    cellHeight = cellWidth / 2.4f;
    cellMargin = cellWidth / 10;

    teamLabelWidth = screen.width - (cellWidth + cellMargin) * model.getProblems().size();

    rowWidth = (cellWidth + cellMargin) * model.getProblems().size();
    rowHeight = cellHeight + cellMargin;

    minScrolledRank = + (1*screen.height + cellMargin / 2.0) / (rowHeight + cellMargin)
         - visibleRowsBelow;
    maxScrolledRank = Math.max(minScrolledRank,
        + (0*screen.height + cellMargin / 2.0) / (rowHeight + cellMargin)
             - visibleRowsBelow + model.getRows().size());

    for (Layout.Group rowLayout : rowLayouts.values()) {
      int position = 0;
      for (Layout cellLayout : rowLayout.children().get(0).children()) {
        cellLayout.relX = (float) ((cellWidth + cellMargin) * position);
        cellLayout.relY = 0.0f;
        cellLayout.width = (float) cellWidth;
        cellLayout.height = (float) cellHeight;
        ++position;
      }
    }

    glLoadIdentity();
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glViewport(0, 0, screenWidth, screenHeight);
    glOrtho(
        0, screen.width,
        0, screen.height,
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
    if (attempts.getNumJudged() != 0 && attempts.getNumPending() == 0) {
      Optional.ofNullable(rowLayouts.get(team.getId()))
          .map(x -> x.children().get(0))
          .map(x -> x.children().get(model.getProblemIndex(attempts.getProblemId())))
          .ifPresent(cellLayout -> particles.launchFrom(
              cellLayout,
              2000,
              AttemptColour.of(attempts),
              attempts.getSolved()));
    }
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
    lastTime = timeNow;

    boolean animating = false;
    animating |= (peekAnimation(scrollAnimation, timeNow) != null);
    animating |= (peekAnimation(moveAnimation, timeNow) != null);
    animating |= particles.exist();

    layout(timeNow);
    draw();
    return animating;
  }

  private void layout(final long timeNow) {
    // Animate the viewport toward the next focused team.
    double scrolledRank = (double) focusedRank;
    for (RankAnimation anim : scrollAnimation) {
      scrolledRank += slerp((anim.toRank() - anim.fromRank()) * -1.0, 0.0, anim.progress(timeNow));
    }
    scrolledRank = clamp(scrolledRank, minScrolledRank, maxScrolledRank);
    screen.y = (float) ((scrolledRank + visibleRowsBelow) * (rowHeight + cellMargin) + cellMargin);

    List<RankAnimation> moveAnims = new ArrayList<>(moveAnimation);
    Collections.reverse(moveAnims);

    for (int rowIndex = model.getRows().size(); rowIndex --> 0;) {
      final ScoreboardRow row = model.getRow(rowIndex);

      double effectiveRank = row.getRank();

      // If we have multiple animations at once, we need to unwind the history of
      // this team's rank as we go back through the stack of changes.
      long rankForAnim = row.getRank();
      for (RankAnimation anim : moveAnims) {
        double factor = 0.0;
        if (rankForAnim == anim.toRank()) {
          effectiveRank += slerp(anim.fromRank() - anim.toRank(), 0.0, anim.progress(timeNow));
          rankForAnim += anim.fromRank() - anim.toRank();
        } else if (anim.fromRank() >= rankForAnim && rankForAnim >= anim.toRank()) {
          effectiveRank += 1.0 * slerp(-1.0, 0.0, anim.progress(timeNow));
          rankForAnim -= 1;
        }
      }

      final Layout rowLayout = rowLayouts.get(row.getTeamId());
      rowLayout.relX = 0.0f;
      rowLayout.relY = (float) (screen.y - effectiveRank * (rowHeight + cellMargin) - cellMargin) - cellMargin / 2.0f;
      rowLayout.width = (float) screen.width;
      rowLayout.height = (float) rowHeight + cellMargin;

      final Layout scoreLayout = rowLayout.children().get(0);
      scoreLayout.relX = (float) (teamLabelWidth + (screen.width - teamLabelWidth - rowWidth) / 1.0f);
      scoreLayout.relY = (float) cellMargin;
      scoreLayout.width = (float) rowWidth;
      scoreLayout.height = (float) rowHeight;
    }
    rootLayout.layout();

    if (particles != null) {
      particles.setOffset(0.0, screen.y);
      particles.update(timeNow);
    }
  }

  private void draw() {
    rootLayout.draw();

    if (particles != null) {
      particles.draw();
    }
  }

  private void drawRow(Layout layout, ScoreboardRow row) {
    long timeNow = lastTime;
    double rowX = layout.x;
    double rowY = layout.y;

    final boolean teamFocused = focusedTeam != null && row.getTeamId() == focusedTeam.getId();

    RankAnimation focus = peekAnimation(focusAnimation, timeNow);

    if (teamFocused) {
      drawFocus(layout, focus != null ? focus.progress(timeNow) : 1.0);
    } else {
      if (row.getRank() >= finalisedRank) {
        drawZebra(layout, row.getRank());
      }
      if (focus != null && focus.fromRank() == row.getRank()) {
        layout.relY += rowHeight + cellMargin;
        drawFocus(layout, focus.progress(timeNow) - 1.0);
        layout.relY -= rowHeight + cellMargin;
      }
    }

    if (row.getRank() >= finalisedRank) {
      drawRank(rowX, rowY, row.getRank(), teamFocused);
    }
    drawLabel(rowX, rowY, model.getTeam(row.getTeamId()), teamFocused);
    drawScore(rowX, rowY, row.getScore(), teamFocused);
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

  private void drawFocus(Layout layout, double progress) {
    final float h = (float) ((rowHeight + cellMargin) * progress);
    glColor3d(0.0, 0.33, 0.48);

    if (progress > 0.0) {
      new Layout(this::glQuad).covering(layout).adjust(0.0f, 0.0f, 0.0f, h - layout.height).draw();
    } else {
      new Layout(this::glQuad).covering(layout).adjust(0.0f, layout.height + h, 0.0f, -h - layout.height).draw();
    }
  }

  private void drawZebra(Layout layout, long rank) {
    final float intensity = (rank % 2 == 1 ? 0.05f: 0.1f);
    final float r = 0.2f, g = 0.2f, b = 0.2f;
    glColor3f(-intensity + r, -intensity + g, -intensity + b);
    new Layout(this::glQuad).covering(layout).draw();
  }

  private void drawRank(double rowX, double rowY, long rank, boolean focused) {
    glColor3d(0.4, 0.4, 0.4);
    font.drawText(
        rowX + teamLabelWidth * 0.1 - cellMargin,
        rowY + cellMargin * 2,
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
              rowX + teamLabelWidth * 0.1,
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
        rowX + teamLabelWidth * 0.1,
        rowY + rowHeight / 2.0,
        (int) ((rowHeight + cellMargin) / 2.0),
        team.getName());
  }

  private void drawScore(double rowX, double rowY, ScoreboardScore score, boolean focused) {
    if (score.getNumSolved() != 0) {
      glColor3d(0.6, 0.6, 0.6);
      font.drawText(
          rowX + teamLabelWidth - rowHeight * 0.1,
          rowY + cellMargin,
          (int) (rowHeight / 2.0 - cellMargin),
          String.format("%4d", score.getTotalTime()),
          FontRenderer.Alignment.RIGHT);
    }
    glColor3d(1.0, 1.0, 1.0);
    font.drawText(
        rowX + teamLabelWidth - rowHeight * 0.1,
        rowY + rowHeight / 2.0,
        (int) ((rowHeight + cellMargin) / 2.0),
        String.format("%2d", score.getNumSolved()),
        FontRenderer.Alignment.RIGHT);
  }

  private void drawAttempts(Layout cellLayout, Team team, ScoreboardProblem attempts) {
    final boolean focused = focusedTeam != null
        && focusedTeam.getId().equals(team.getId())
        && focusedProblem != null
        && focusedProblem.getId().equals(attempts.getProblemId());

    final boolean pending = (attempts.getNumPending() > 0);
    final boolean attempted = (attempts.getNumJudged() > 0 || pending);
    final String text;

    final double cellX = cellLayout.x;
    final double cellY = cellLayout.y;

    int totalAttempts = attempts.getNumJudged() + attempts.getNumPending();
    final String subText;
    switch (totalAttempts) {
      case 0: subText = null; break;
      case 1: subText = Integer.toString(totalAttempts) + " " + FontRenderer.Symbols.TRIES_ONE; break;
      default: subText = Integer.toString(totalAttempts) + " " + FontRenderer.Symbols.TRIES_MANY; break;
    }

    final short[] rgb = AttemptColour.of(attempts);
    float r = rgb[0] / 255.0f;
    float g = rgb[1] / 255.0f;
    float b = rgb[2] / 255.0f;
    if (attempts.getSolved()) {
      text = FontRenderer.Symbols.CORRECT;
    } else if (attempts.getNumPending() > 0) {
      text = FontRenderer.Symbols.PENDING;
    } else if (attempts.getNumJudged() > 0) {
      text = FontRenderer.Symbols.WRONG;
    } else {
      text = null;
    }

    if (focused) {
      if (pending) {
        glColor3ub((byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);
      } else {
        glColor3ub((byte) AttemptColour.JUDGING[0], (byte) AttemptColour.JUDGING[1], (byte) AttemptColour.JUDGING[2]);
      }
      new Layout(this::glQuad).covering(cellLayout).adjust(-cellMargin/4.0f, -cellMargin/4.0f, +cellMargin/2.0f, +cellMargin/2.0f).draw();
    }

    if (!attempted) {
      glEnable(GL_BLEND);

      final float shWidth = (float) (cellHeight / 8.0);
      final float shHeight = (float) (cellHeight / 4.0);

      glBegin(GL_TRIANGLE_FAN);
      glColor4f(0f, 0f, 0f, 0.7f);
      glVertex2d(cellX, cellY + cellHeight);
      glVertex2d(cellX+cellWidth, cellY + cellHeight);
      glColor4f(r, g, b, 0.6f);
      glVertex2d(cellX+cellWidth, cellY+cellHeight-shHeight);
      glVertex2d(cellX+shWidth, cellY+cellHeight-shHeight);
      glVertex2d(cellX+shWidth, cellY);
      glColor4f(0f, 0f, 0f, 0.7f);
      glVertex2d(cellX, cellY);
      glEnd();

      glColor4f(r, g, b, 0.6f);
      new Layout(this::glQuad).covering(cellLayout).adjust(shWidth, 0, -shWidth, -shHeight).draw();

      glDisable(GL_BLEND);
    } else {
      if (focused && pending) {
        glColor3f(1.0f, 1.0f, 1.0f);
      } else {
        glColor3f(r, g, b);
      }
      new Layout(this::glQuad).covering(cellLayout).draw();
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

  private void glQuad(Layout layout) {
    glBegin(GL_QUADS);
    glVertex2d(layout.x, layout.y);
    glVertex2d(layout.x + layout.width, layout.y);
    glVertex2d(layout.x + layout.width, layout.y + layout.height);
    glVertex2d(layout.x, layout.y + layout.height);
    glEnd();
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
