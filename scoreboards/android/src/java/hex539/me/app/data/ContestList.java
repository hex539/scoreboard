package me.hex539.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.util.AtomicFile;
import android.util.Log;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.clics.proto.ClicsProto;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import me.hex539.app.SavedConfigs;
import me.hex539.contest.ContestConfig;
import me.hex539.contest.ContestDownloader;

public class ContestList {

  private static final String TAG = ContestList.class.getSimpleName();
  private static final AtomicReference<ContestList> singleton = new AtomicReference<>();

  private final HandlerThread storageThread;
  private final StorageHandler storageHandler;

  private final AtomicFile registerFile;
  private final File contestDirectory;

  private final CompletableFuture<Boolean> firstRegister = new CompletableFuture<>();
  private final AtomicReference<SavedConfigs.Root> latestRegister = new AtomicReference<>();
  private final Set<Consumer<SavedConfigs.Root>> registerListeners = new HashSet<>();

  private final LoadingCache<String, CompletableFuture<ClicsProto.ClicsContest>> contestCache;

  public static ContestList getOrCreate(Context context) {
    if (singleton.get() == null) {
      synchronized (singleton) {
        if (singleton.get() == null) {
          singleton.set(new ContestList(
              /* registerFile= */ new File(context.getFilesDir(), "contest-list.pb"),
              /* contestDirectory= */ new File(context.getFilesDir(), "contests")));
        }
      }
    }
    return singleton.get();
  }

  public static ContestList get() {
    if (singleton.get() == null) {
      throw new AssertionError("ContestList.get() called before create()");
    }
    return singleton.get();
  }

  private ContestList(File registerFile, File contestDirectory) {
    this.storageThread = new HandlerThread("ContestList.storage");
    this.storageThread.start();

    this.registerFile = new AtomicFile(registerFile);
    this.contestDirectory = contestDirectory;

    this.contestCache = CacheBuilder.newBuilder()
        .maximumSize(5)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(new CacheLoader<String, CompletableFuture<ClicsProto.ClicsContest>>() {
          @Override
          public CompletableFuture<ClicsProto.ClicsContest> load(String key) {
            return storageHandler.loadContest(key);
          }
        });

    this.storageHandler = new StorageHandler(this.storageThread.getLooper());
    this.storageHandler.createContestDirectory();
    this.storageHandler.loadRegisterFile();
  }

  public CompletableFuture<SavedConfigs.Root> getRegister() {
    return firstRegister.thenApply(ignore -> latestRegister.get());
  }

  private void updateRegister(UnaryOperator<SavedConfigs.Root> updater) {
    SavedConfigs.Root newValue = latestRegister.updateAndGet(updater);
    firstRegister.complete(true);
    synchronized (registerListeners) {
      registerListeners.forEach(l -> l.accept(newValue));
    }
  }

  public void addListener(Consumer<SavedConfigs.Root> l) {
    synchronized (registerListeners) {
      registerListeners.add(l);
    }
  }

  public void removeListener(Consumer<SavedConfigs.Root> l) {
    synchronized (registerListeners) {
      registerListeners.remove(l);
    }
  }

  public CompletableFuture<ClicsProto.ClicsContest> getContest(String sourceId) {
    return getRegister()
        .thenApplyAsync(register -> register.getContestsOrThrow(sourceId))
        .thenApplyAsync(this::getContest)
        .thenApplyAsync(CompletableFuture::join);
  }

  public CompletableFuture<ClicsProto.ClicsContest> getContest(ContestConfig.Source source) {
    try {
      return contestCache.get(source.getId());
    } catch (ExecutionException e) {
      throw new Error(e);
    }
  }

  public CompletableFuture<ClicsProto.ClicsContest> addContest(ContestConfig.Source source) {
    return storageHandler.addContest(source);
  }

  public CompletableFuture<ClicsProto.ClicsContest> addContest(
      ContestConfig.Source source,
      ClicsProto.ClicsContest contest) {
    return storageHandler.addContest(source, contest);
  }

  private class StorageHandler extends Handler {

    public StorageHandler(Looper looper) {
      super(looper);
    }

    public void createContestDirectory() {
      post(() -> {
        if (!contestDirectory.isDirectory()) {
          contestDirectory.mkdirs();
        }
      });
    }

    public void loadRegisterFile() {
      post(() -> {
        if (exists(registerFile)) {
          try (InputStream fin = registerFile.openRead()) {
            final SavedConfigs.Root register = SavedConfigs.Root.parseFrom(fin);
            updateRegister(ignore -> register);
            return;
          } catch (Exception e) {
            Log.e(TAG, "Failed to read saved contest register. Starting a new one.", e);
            if (!registerFile.getBaseFile().renameTo(new File(String.format(
                  "%s.invalid.%d",
                  registerFile.getBaseFile().getPath(),
                  System.currentTimeMillis())))) {
              Log.e(TAG, "Failed to backup old contest register. Sorry.");
            }
          }
        } else {
          Log.d(TAG, "Contest register does not exist. Starting a new one.");
        }

        final SavedConfigs.Root register = SavedConfigs.Root.newBuilder().build();
        if (!attemptWrite(registerFile, out -> register.writeTo(out))) {
          Log.wtf(TAG, "Failed to write empty contest register. Something is seriously wrong.");
        }
        updateRegister(ignore -> register);
      });
    }

    public void writeRegisterFile() {
      post(() -> {
        if (!attemptWrite(registerFile, out -> latestRegister.get().writeTo(out))) {
          Log.e(TAG, "Failed to write update contest register.");
        }
      });
    }

    public CompletableFuture<ClicsProto.ClicsContest> addContest(ContestConfig.Source source) {
      CompletableFuture<ClicsProto.ClicsContest> res = new CompletableFuture<>();
      post(() -> {
        try {
          final ClicsProto.ClicsContest contest = new ContestDownloader(source).fetch();
          res.complete(addContestImmediate(source, contest));
        } catch (Exception e) {
          Log.e(TAG, "Failed to save contest from " + source, e);
          res.cancel(/* mayInterruptIfRunning= */ true);
        }
      });
      return res;
    }

    public CompletableFuture<ClicsProto.ClicsContest> addContest(ContestConfig.Source source, ClicsProto.ClicsContest contest) {
      CompletableFuture<ClicsProto.ClicsContest> res = new CompletableFuture<>();
      post(() -> res.complete(addContestImmediate(source, contest)));
      return res;
    }

    private ClicsProto.ClicsContest addContestImmediate(
        final ContestConfig.Source originalSource,
        final ClicsProto.ClicsContest contest) {
      final ContestConfig.Source source = originalSource.toBuilder()
          .setId(contest.getContest().getFormalName())
          .setName(contest.getContest().getName())
          .build();

      try {
        try (OutputStream os = new FileOutputStream(getContestFile(source.getId()))) {
          contest.writeTo(os);
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to save contest from \"" + source + "\" to disk.", e);
        return contest;
      }

      try {
        contestCache.put(source.getId(), CompletableFuture.completedFuture(contest));
        updateRegister(r -> r.toBuilder().putContests(source.getId(), source).build());
        writeRegisterFile();
      } catch (Exception e) {
        Log.e(TAG, "Failed to write new contest source register.", e);
      }
      return contest;
    }

    public void deleteContest(String sourceId) {
      post(() -> {
        try {
          updateRegister(r -> r.toBuilder().removeContests(sourceId).build());
          writeRegisterFile();
          contestCache.invalidate(sourceId);
        } catch (Exception e) {
          Log.e(TAG, "Failed to write new register.", e);
        }
        if (!getContestFile(sourceId).delete()) {
          Log.w(TAG, "Could not delete contest source \"" + sourceId + "\".");
        }
      });
    }

    public CompletableFuture<ClicsProto.ClicsContest> loadContest(String sourceId) {
      Log.d(TAG, "Loading contest: " + sourceId);
      final CompletableFuture<ClicsProto.ClicsContest> result = new CompletableFuture<>();
      post(() -> {
        try {
          final File contestFile = getContestFile(sourceId);
          if (contestFile.canRead()) {
            result.complete(
                new ContestDownloader(
                    ContestConfig.Source.newBuilder()
                        .setFilePath(contestFile.getPath())
                        .build())
                    .fetch());
            return;
          }
        } catch (Exception e) {
          Log.e(TAG, "Failed to load contest from source \"" + sourceId + "\"", e);
        }
        result.cancel(/* mayInterruptIfRunning= */ true);
      });
      return result;
    }
  }

  private static boolean exists(AtomicFile file) {
    try {
      file.openRead().close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private static boolean attemptWrite(AtomicFile file, IOConsumer<FileOutputStream> writer) {
    try {
      final FileOutputStream fout = file.startWrite();
      try {
        writer.accept(fout);
        file.finishWrite(fout);
        return true;
      } catch (Exception e) {
        file.failWrite(fout);
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to start writing file: " + file);
    }
    return false;
  }

  @FunctionalInterface
  private interface IOConsumer<T> {
    void accept(T subject) throws IOException;
  }

  private File getContestFile(String contestId) {
    final String safeName = new String(
        Base64.getUrlEncoder().encode(contestId.getBytes()),
        StandardCharsets.UTF_8);
    return new File(contestDirectory, safeName + ".pb");
  }
}
