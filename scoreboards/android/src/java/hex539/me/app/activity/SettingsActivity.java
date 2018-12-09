package me.hex539.app.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import me.hex539.app.R;

import me.hex539.app.fragment.ContestListFragment;

public class SettingsActivity extends FragmentActivity {
  private static final String TAG = SettingsActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedState) {
    super.onCreate(savedState);

    if (savedState == null) {
      getFragmentManager()
          .beginTransaction()
          .replace(android.R.id.content, new ContestListFragment())
          .commit();
    }
  }
}
