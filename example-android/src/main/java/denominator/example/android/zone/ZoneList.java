package denominator.example.android.zone;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.tape.Task;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.DNSApiManager;
import denominator.model.Zone;

public class ZoneList implements Task<ZoneList.Callback> {

  private static final String TAG = "Denominator:ZoneList";
  private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());
  private final DNSApiManager mgr;

  @Inject
  ZoneList(DNSApiManager mgr) {
    this.mgr = mgr;
  }

  @Override
  public void execute(final ZoneList.Callback callback) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Log.i(TAG, "Listing Zones in " + mgr.provider().name());
        try {
          long start = System.currentTimeMillis();
          final Iterator<Zone> zones = mgr.api().zones().iterator();
          final long duration = System.currentTimeMillis() - start;
          Log.i(TAG, "success! " + mgr.provider().name());
          MAIN_THREAD.post(new Runnable() {
            @Override
            public void run() {
              callback.onSuccess(zones, duration);
            }
          });
        } catch (final RuntimeException e) {
          e.printStackTrace();
          MAIN_THREAD.post(new Runnable() {
            @Override
            public void run() {
              callback.onFailure(e);
            }
          });
        }
      }
    }).start();
  }

  public interface Callback {

    void onSuccess(Iterator<Zone> zones, long duration);

    void onFailure(Throwable t);
  }

  public static class SuccessEvent {

    public final Iterator<Zone> zones;
    public final long duration;

    SuccessEvent(Iterator<Zone> zones, long duration) {
      this.zones = zones;
      this.duration = duration;
    }
  }
}
