package denominator.example.android.zone;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Bus;
import com.squareup.tape.ObjectQueue;

import javax.inject.Inject;

public class ZoneListQueue extends com.squareup.tape.TaskQueue<ZoneList> {

  private final Context context;
  private final Bus bus;

  @Inject
  ZoneListQueue(ObjectQueue<ZoneList> delegate, Application context, Bus bus) {
    super(delegate);
    this.context = context;
    this.bus = bus;
    bus.register(this);

    if (size() > 0) {
      startService();
    }
  }

  private void startService() {
    context.startService(new Intent(context, ZoneListTaskService.class));
  }

  @Override
  public void add(ZoneList entry) {
    super.add(entry);
    startService();
  }
}
