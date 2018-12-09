package me.hex539.app.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.support.v17.preference.LeanbackSettingsFragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;
import edu.clics.proto.ClicsProto;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import me.hex539.app.R;
import me.hex539.app.SavedConfigs;
import me.hex539.app.activity.LiveScoreboardActivity;
import me.hex539.app.data.ContestList;
import me.hex539.contest.ApiDetective;
import me.hex539.contest.ContestConfig;

public class ContestListFragment extends LeanbackSettingsFragment {
  private static final String TAG = ContestListFragment.class.getSimpleName();

  @Override
  public void onPreferenceStartInitialScreen() {
    startPreferenceFragment(new PrefsFragment());
  }

  @Override
  public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
    return false;
  }

  @Override
  public boolean onPreferenceStartScreen(PreferenceFragment caller, PreferenceScreen pref) {
    return false;
  }

  public static class PrefsFragment extends LeanbackPreferenceFragment {

    private PreferenceCategory savedCategory;
    private EditTextPreference addContest;

    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      ContestList.getOrCreate(context).addListener(onSavedContestsUpdated);
    }

    @Override
    public void onDetach() {
      ContestList.get().removeListener(onSavedContestsUpdated);
      super.onDetach();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());
      setPreferenceScreen(screen);

      savedCategory = new PreferenceCategory(screen.getContext());
      savedCategory.setTitle(R.string.saved_contests);
      screen.addPreference(savedCategory);

      addContest = new EditTextPreference(screen.getContext());
      addContest.setDialogLayoutResource(R.layout.edit_text_dialog);
      addContest.setPersistent(false);
      addContest.setKey("add-contest");
      addContest.setTitle(R.string.add_contest);
      addContest.setOnPreferenceChangeListener(this::onAddContestClicked);
    }

    private void addFixedPreferences() {
      savedCategory.addPreference(addContest);
    }

    /**
     * Listener to refresh the UI whenever something happens to the list of saved contests.
     * <p>
     * This needs to be a lambda instead of a method reference because method references fail to
     * countenance any meaningful concept of identity.
     */
    private final Consumer<SavedConfigs.Root> onSavedContestsUpdated = savedContests -> {
      savedCategory.removeAll();
      addFixedPreferences();
      savedContests.getContests().values().stream()
          .sorted(Comparator.comparing(ContestConfig.Source::getName))
          .forEach(source -> {
            Preference pref = new Preference(getContext());
            pref.setPersistent(false);
            pref.setTitle(source.getName());
            pref.setOnPreferenceClickListener(self -> {openContest(source); return true;});
            savedCategory.addPreference(pref);
          });
    };

    private void openContest(ContestConfig.Source source) {
      final Intent intent = new Intent(getContext(), LiveScoreboardActivity.class);
      intent.putExtra(LiveScoreboardActivity.Extras.CONTEST_ID, source.getId());
      getContext().startActivity(intent);
    }

    private boolean onAddContestClicked(Preference pref, Object newValue) {
      final String baseUrl = (String) newValue;

      CompletableFuture<ClicsProto.ClicsContest> contest =
          CompletableFuture.supplyAsync(() -> ApiDetective.detectApi(baseUrl).get())
              .thenApplyAsync(ContestList.get()::addContest)
              .thenApplyAsync(CompletableFuture::join);

      new ContestSaveResultTask().execute(contest);
      return false;
    }

    private class ContestSaveResultTask
        extends AsyncTask<CompletableFuture<ClicsProto.ClicsContest>, Void, Boolean> {
      @Override
      public Boolean doInBackground(CompletableFuture<ClicsProto.ClicsContest>... args) {
        try {
          return args[0].get() != null;
        } catch (ExecutionException | InterruptedException e) {
          return false;
        }
      }

      @Override
      public void onPostExecute(Boolean valid) {
        Toast.makeText(
            getContext(), 
            valid
                ? R.string.contest_save_success
                : R.string.contest_save_failure,
            Toast.LENGTH_SHORT).show();
      }
    }
  }
}
