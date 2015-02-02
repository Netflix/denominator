package denominator.example.android.zone;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.tape.TaskQueue;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.example.android.DenominatorApplication;
import denominator.example.android.zone.ZoneList.Callback;
import denominator.model.Zone;

/**
 * This service guarantees that zone lists happen in the background and only once at a time.
 */
public class ZoneListTaskService extends Service implements Callback {

  private static final String TAG = "Denominator:ZoneListTaskService";

  @Inject
  TaskQueue<ZoneList> taskQueue;
  @Inject
  Bus bus;

  private boolean running;

  @Override
  public void onCreate() {
    super.onCreate();
    DenominatorApplication.class.cast(getApplication()).getApplicationGraph().inject(this);
    Log.i(TAG, " starting");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    executeNext();
    return START_STICKY;
  }

  private void executeNext() {
    // no duplicates
    if (running) {
      taskQueue.remove();
    }
    ZoneList task = taskQueue.peek();
    if (task != null) {
      running = true;
      task.execute(this);
    } else {
      Log.i(TAG, " stopping");
      stopSelf();
    }
  }

  @Override
  public void onSuccess(Iterator<Zone> zones, long duration) {
    running = false;
    taskQueue.remove();
    bus.post(new ZoneList.SuccessEvent(zones, duration));
    executeNext();
  }

  @Override
  public void onFailure(Throwable t) {
    bus.post(t);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
