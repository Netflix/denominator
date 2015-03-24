package denominator.config;

import java.io.Closeable;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;

/**
 * In many providers, we would likely inject resources that need cleanup and call them inside the
 * {@link Closeable}. For example, shutting down thread pools, or syncing disk write. In this case,
 * there's nothing to close.
 */
@Module(injects = DNSApiManager.class, complete = false)
public class NothingToClose implements Closeable {

  @Provides
  @Singleton
  Closeable provideCloser() {
    return this;
  }

  @Override
  public void close() {
    // nothing to close
  }

}
