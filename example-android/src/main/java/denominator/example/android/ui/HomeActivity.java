package denominator.example.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.tape.TaskQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.ObjectGraph;
import dagger.Provides;
import denominator.Provider;
import denominator.example.android.DenominatorApplication;
import denominator.example.android.R;
import denominator.example.android.zone.ZoneList;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

public class HomeActivity extends FragmentActivity {

  @Inject
  Provider provider;
  @Inject
  ZoneList zoneList;
  @Inject
  TaskQueue<ZoneList> taskQueue;
  @Inject
  Bus bus;

  private ObjectGraph activityGraph;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    DenominatorApplication application = DenominatorApplication.class.cast(getApplication());

    // make a child graph that injects us and our fragments
    @dagger.Module(injects = {
        HomeActivity.class,
        ZoneListFragment.class
    }, complete = false)
    class ActivityModule {

      @Provides
      @Singleton
      Activity provideActivityContext() {
        return HomeActivity.this;
      }
    }

    activityGraph = application.getApplicationGraph().plus(new ActivityModule());
    activityGraph.inject(this);

    setTitle(getString(R.string.home_title, provider.name()));
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
          .add(android.R.id.content, inject(new ZoneListFragment()))
          .commit();
    }
  }

  // wire up preferences screen when menu button is pressed.
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU) {
      startActivity(new Intent(this, PreferencesActivity.class));
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  /**
   * flash the response time of doing the list.
   */
  @Subscribe
  public void onZones(ZoneList.SuccessEvent event) {
    String durationEvent = getString(R.string.list_duration, event.duration);
    Toast.makeText(this, durationEvent, LENGTH_SHORT).show();
  }

  /**
   * show any error messages posted to the bus.
   */
  @Subscribe
  public void onFailure(Throwable t) {
    Toast.makeText(this, t.getMessage(), LENGTH_LONG).show();
  }

  @Override
  public void onResume() {
    super.onResume();
    bus.register(this);
    taskQueue.add(zoneList);
  }

  @Override
  public void onPause() {
    super.onPause();
    bus.unregister(this);
  }

  @Override
  protected void onDestroy() {
    activityGraph = null;
    super.onDestroy();
  }

  /**
   * use this on {@link android.support.v4.app.Fragment#onActivityCreated} to restore injected
   * fields.
   */
  public <T> T inject(T toInject) {
    return activityGraph.inject(toInject);
  }
}
