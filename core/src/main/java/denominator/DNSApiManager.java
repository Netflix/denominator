package denominator;

import java.io.Closeable;
import java.io.IOException;

import javax.inject.Inject;

/**
 * represents the connection between a {@link DNSApi} interface and the {@link Provider} that
 * implements it.
 */
public class DNSApiManager implements Closeable {

  private final Provider provider;
  private final DNSApi api;
  private final CheckConnection checkConnection;
  private final Closeable closer;

  @Inject
  DNSApiManager(Provider provider, DNSApi api, CheckConnection checkConnection, Closeable closer) {
    this.provider = provider;
    this.api = api;
    this.checkConnection = checkConnection;
    this.closer = closer;
  }

  /**
   * the currently configured {@link DNSApi}
   */
  public DNSApi api() {
    return api;
  }

  /**
   * Get the provider associated with this instance
   */
  public Provider provider() {
    return provider;
  }

  /**
   * Returns true, if api commands are likely to succeed.
   *
   * @see CheckConnection
   */
  public boolean checkConnection() {
    return checkConnection.ok();
  }

  /**
   * closes resources associated with the connections, such as thread pools or open files.
   */
  @Override
  public void close() throws IOException {
    closer.close();
  }

  @Override
  public String toString() {
    return provider.toString();
  }
}
