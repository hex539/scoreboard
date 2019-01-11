package me.hex539.resolver.draw;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.*;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import edu.clics.proto.ClicsProto.*;

import me.hex539.contest.ScoreboardModel;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;

public class FontRenderer {
  public static final String FONT_SYMBOLA = "/resources/fonts/Symbola.ttf";
  public static final String FONT_UNIFONT = "/resources/fonts/unifont-11.0.03.ttf";

  private static final int WIDTH = 2048;
  private static final int HEIGHT = 512;

  private final ByteBuffer[] ttfData = new ByteBuffer[1];
  private final STBTTFontinfo[] fontInfo = new STBTTFontinfo[1];
  private int ascent;
  private int descent;
  private int lineGap;

  private Map<Integer, Font> fonts = new HashMap<>();
  private final List<Integer> allCodePoints = new ArrayList<>();

  public enum Alignment {
    LEFT,
    CENTRE,
    RIGHT
  }

  public static final class Symbols {
    private Symbols() {}

    public static final String CORRECT = "✓";
    public static final String PENDING = "?";
    public static final String WRONG = "✖";
    public static final String TRIES_ONE = "ᴛʀʏ";
    public static final String TRIES_MANY = "ᴛʀɪᴇs";

    static final String[] ALL = {CORRECT, PENDING, WRONG, TRIES_ONE, TRIES_MANY};
  }

  private static class Font {
    public final int height;
    public final int texId;
    public final STBTTPackedchar.Buffer cdata;

    public Font(final int height, final int texId, final STBTTPackedchar.Buffer cdata) {
      this.height = height;
      this.texId = texId;
      this.cdata = cdata;
    }
  }

  private double screenWidth = 1;
  private double screenHeight = 1;

  public FontRenderer(ScoreboardModel model, CompletableFuture<? extends ByteBuffer> ttfFont) {
    try {
      ttfData[0] = ttfFont.get();
    } catch (Exception e) {
      throw new Error("Failed to load font", e);
    }

    supplyCodePoints(model, allCodePoints);

    fontInfo[0] = STBTTFontinfo.create();
    if (!stbtt_InitFont(fontInfo[0], ttfData[0])) {
      throw new IllegalStateException("Failed to initialize font information.");
    }

    try (MemoryStack stack = stackPush()) {
      final IntBuffer pAscent  = stack.mallocInt(1);
      final IntBuffer pDescent = stack.mallocInt(1);
      final IntBuffer pLineGap = stack.mallocInt(1);
      stbtt_GetFontVMetrics(fontInfo[0], pAscent, pDescent, pLineGap);
      this.ascent = pAscent.get(0);
      this.descent = pDescent.get(0);
      this.lineGap = pLineGap.get(0);
    }
  }

  private static void supplyCodePoints(ScoreboardModel model, List<Integer> into){
    List<Integer> codePoints = new ArrayList<>();
    for (int i = 32; i < 128; i++) {
      codePoints.add(i);
    }
    for (String special : Symbols.ALL) {
      addString(codePoints, special);
    }
    for (Team team : model.getTeams()) {
      addString(codePoints, team.getName());
    }
    for (Organization organisation : model.getOrganizations()) {
      addString(codePoints, organisation.getName());
    }
    Collections.sort(codePoints);
    int last = -1;
    for (int i : codePoints) {
      if (i != last) {
        into.add(i);
        last = i;
      }
    }
  }

  private static void addString(List<Integer> list, String str) {
    for (int i = 0, to = str.length(); i < to; ) {
      char c1 = str.charAt(i);
      if (Character.isHighSurrogate(c1) && i + 1 < to) {
        char c2 = str.charAt(i + 1);
        if (Character.isLowSurrogate(c2)) {
          list.add(Character.toCodePoint(c1, c2));
          i += 2;
        }
      }
      list.add((int) c1);
      i += 1;
    }
  }

  private Font initGraphics(int fontHeight) {
    Font font = new Font(
        /* height= */ fontHeight,
        /* texId= */ glGenTextures(),
        /* cdata[]= */ STBTTPackedchar.malloc(allCodePoints.size()));

    ByteBuffer bitmap = BufferUtils.createByteBuffer(WIDTH * HEIGHT);

    final int stride = 0;
    final int padding = 1;
    STBTTPackContext context = STBTTPackContext.create();
    stbtt_PackBegin(context, bitmap, WIDTH, HEIGHT, stride, padding);

    IntBuffer buffer = BufferUtils.createIntBuffer(allCodePoints.size());
    for (int i : allCodePoints) {
      buffer.put(i);
    }
    buffer.flip();

    double fontSize = fontHeight * (double) (ascent - descent) / (double) ascent;

    STBTTPackRange.Buffer packRanges = STBTTPackRange.malloc(1);
    packRanges.put(STBTTPackRange.malloc().set(
        /* size= */ (float) fontSize,
        /* start= */ 0,
        /* buffer= */ buffer,
        /* length= */ allCodePoints.size(),
        /* font = */ font.cdata,
        /* unused= */ (byte) 0,
        /* unused= */ (byte) 0));
    packRanges.flip();
    final int fontIndex = 0;
    stbtt_PackFontRanges(context, ttfData[0], fontIndex, packRanges);
    stbtt_PackEnd(context);

    glBindTexture(GL_TEXTURE_2D, font.texId);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, WIDTH, HEIGHT, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

    return font;
  }

  public void destroyGraphics() {
    for (Font font : fonts.values()) {
      glDeleteTextures(font.texId);
    }
    fonts.clear();
  }

  public void setVideoSize(double width, double height) {
    if (width != screenWidth || height != screenHeight) {
      screenWidth = width;
      screenHeight = height;
      destroyGraphics();
    }
  }

  public void drawText(double x, double y, int size, String text) {
    drawText(x, y, size, text, Alignment.LEFT);
  }

  public void drawText(double x, double y, int size, String text, Alignment align) {
    Font font = fonts.get(size);
    if (font == null) {
      font = initGraphics(size);
      fonts.put(size, font);
    }

    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    renderText(font, x, y, text, align);

    glDisable(GL_BLEND);
    glDisable(GL_TEXTURE_2D);
  }

  private void renderText(Font font, double startX, double startY, String text, Alignment align) {
    glBindTexture(GL_TEXTURE_2D, font.texId);

    try (MemoryStack stack = stackPush()) {
      IntBuffer pCodePoint = stack.mallocInt(1);

      FloatBuffer x = stack.floats((float) startX);
      FloatBuffer y = stack.floats((float) startY);

      STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);

      int lineStart = 0;

      final float scale = 1.0f;
      final float factorX = (float) +scale;
      final float factorY = (float) -scale;
      float lineY = (float) startY;

      int n = text.length();
      float[] s = new float[n * 2];
      float[] t = new float[n * 2];
      float[] vx = new float[n * 2];
      float[] vy = new float[n * 2];
      n = 0;

      glBegin(GL_QUADS);
      for (int i = 0, to = text.length(); i < to; ) {
        i += nextCodepoint(text, to, i, pCodePoint);

        int cp = pCodePoint.get(0);
        if (cp == '\n') {
          y.put(0, lineY = y.get(0) + (ascent - descent + lineGap) * scale);
          x.put(0, 0);

          lineStart = i;
          continue;
        }

        float cpX = x.get(0);
        stbtt_GetPackedQuad(font.cdata, WIDTH, HEIGHT, indexOfGlyph(cp), x, y, q, true);
        if (i < to) {
          nextCodepoint(text, to, i, pCodePoint);
          x.put(0, x.get(0) + stbtt_GetGlyphKernAdvance(fontInfo[0], indexOfGlyph(cp), indexOfGlyph(pCodePoint.get(0))) * scale);
        }

        s[n*2+0] = q.s0();
        s[n*2+1] = q.s1();
        t[n*2+0] = q.t0();
        t[n*2+1] = q.t1();
        vx[n*2+0] = q.x0();
        vx[n*2+1] = q.x1();
        vy[n*2+0] = 2 * lineY - q.y0();
        vy[n*2+1] = 2 * lineY - q.y1();
        n++;
      }

      if (align != Alignment.LEFT) {
        double xMin = vx[0];
        double xMax = vx[n*2-1];
        for (int i = 0; i < n*2; i++) {
          vx[i] += startX - (align == Alignment.CENTRE ? (xMax + xMin) * 0.5 : xMax);
        }
      }

      for (int i = 0; i < n; i++) {
        glTexCoord2f(s[i*2+0], t[i*2+0]);
        glVertex2f(vx[i*2+0], vy[i*2+0]);

        glTexCoord2f(s[i*2+1], t[i*2+0]);
        glVertex2f(vx[i*2+1], vy[i*2+0]);

        glTexCoord2f(s[i*2+1], t[i*2+1]);
        glVertex2f(vx[i*2+1], vy[i*2+1]);

        glTexCoord2f(s[i*2+0], t[i*2+1]);
        glVertex2f(vx[i*2+0], vy[i*2+1]);
      }

      glEnd();
    }
  }

  private static int nextCodepoint(String text, int to, int i, IntBuffer cpOut) {
    char c1 = text.charAt(i);
    if (Character.isHighSurrogate(c1) && i + 1 < to) {
      char c2 = text.charAt(i + 1);
      if (Character.isLowSurrogate(c2)) {
        cpOut.put(0, Character.toCodePoint(c1, c2));
        return 2;
      }
    }
    cpOut.put(0, c1);
    return 1;
  }

  private int indexOfGlyph(int codePoint) {
    int l = 0, r = allCodePoints.size();
    while (l + 1 < r) {
      int x = (l + r) / 2;
      if (allCodePoints.get(x) <= codePoint) {
        l = x;
      } else {
        r = x;
      }
    }
    return l;
  }

  public static ByteBuffer mapResource(String location) {
    try (final InputStream is = FontRenderer.class.getResourceAsStream(location)) {
      if (is == null) {
        throw new Error("Resource does not exist: " + location);
      }
      final byte[] data = readAllBytes(is);
      final ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
      buffer.put(data, 0, data.length);
      buffer.flip();
      return buffer;
    } catch (IOException e) {
      throw new Error("Failed to map resource: " + location, e);
    }
  }
  /**
   * Poor implementation of Java9's InputStream.readAllBytes().
   *
   * <p>TODO (2019): replace with is.readAllBytes() again once OpenJDK 11 is widely enough
   * used to not cause issues for people trying to quickly compile without upgrading java.
   */
  private static byte[] readAllBytes(InputStream is) throws IOException {
    final byte[] data = new byte[is.available()];
    for (int i = 0; i < data.length;) {
      int l = is.read(data, i, data.length - i);
      if (l != -1) {
        i += l;
      } else {
        break;
      }
    }
    return data;
  }
}
