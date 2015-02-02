package denominator.example.android.zone;

import com.squareup.otto.Bus;
import com.squareup.tape.InMemoryObjectQueue;
import com.squareup.tape.ObjectQueue;
import com.squareup.tape.TaskQueue;

import javax.inject.Singleton;

import dagger.Provides;

@dagger.Module(
    injects = ZoneListTaskService.class,
    complete = false // application
)
public class ZoneListModule {

  @Provides
  @Singleton
  ObjectQueue<ZoneList> objectQueue() {
    return new InMemoryObjectQueue<ZoneList>();
  }

  @Provides
  @Singleton
  TaskQueue<ZoneList> taskQueue(ZoneListQueue zlq) {
    return zlq;
  }

  @Provides
  @Singleton
  Bus bus() {
    return new Bus();
  }
}
