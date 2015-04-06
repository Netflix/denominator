package denominator.denominatord;

import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import denominator.DNSApiManager;

import static denominator.denominatord.RecordSetDispatcher.RECORDSET_PATTERN;

public class DenominatorDispatcher extends Dispatcher {

  private final DNSApiManager mgr;
  private final Dispatcher zones;
  private final Dispatcher recordSets;

  DenominatorDispatcher(DNSApiManager mgr, JsonCodec codec) {
    this.mgr = mgr;
    this.zones = new ZoneDispatcher(mgr.api().zones(), codec);
    this.recordSets = new RecordSetDispatcher(mgr, codec);
  }

  @Override
  public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
    try {
      if ("/healthcheck".equals(request.getPath())) {
        if (!request.getMethod().equals("GET")) {
          return new MockResponse().setResponseCode(405);
        }
        return new MockResponse().setResponseCode(mgr.checkConnection() ? 200 : 503);
      } else if (RECORDSET_PATTERN.matcher(request.getPath()).matches()) {
        return recordSets.dispatch(request);
      } else if (request.getPath() != null && request.getPath().startsWith("/zones")) {
        return zones.dispatch(request);
      } else {
        return new MockResponse().setResponseCode(404);
      }
    } catch (InterruptedException e) {
      throw e;
    } catch (RuntimeException e) {
      return new MockResponse().setResponseCode(e instanceof IllegalArgumentException ? 400 : 500)
          .addHeader("Content-Type", "text/plain")
          .setBody(e.getMessage() + "\n"); // curl nice
    }
  }
}
