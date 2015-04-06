package denominator.denominatord;


import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import denominator.ZoneApi;
import denominator.model.Zone;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

public class ZoneDispatcher extends Dispatcher {
  private final Logger log = Logger.getLogger(Dispatcher.class.getName());
  private final ZoneApi api;
  private final JsonCodec codec;

  ZoneDispatcher(ZoneApi api, JsonCodec codec) {
    this.api = api;
    this.codec = codec;
  }

  @Override
  public MockResponse dispatch(RecordedRequest request) {
    if (request.getMethod().equals("GET")) {
      Query query = Query.from(request.getPath());
      if (query.name != null) {
        return codec.toJsonArray(api.iterateByName(query.name));
      }
      return codec.toJsonArray(api.iterator());
    } else if (request.getMethod().equals("PUT")) {
      Zone zone = codec.readJson(request, Zone.class);
      long s = currentTimeMillis();
      log.info(format("replacing zone %s", zone));
      String id = api.put(zone);
      log.info(format("replaced zone %s in %sms", zone, currentTimeMillis() - s));
      return new MockResponse().setResponseCode(201).addHeader("Location", "/zones/" + id);
    } else if (request.getMethod().equals("DELETE")) {
      String zoneId = request.getPath().replace("/zones/", "");
      long s = currentTimeMillis();
      log.info(format("deleting zone %s ", zoneId));
      api.delete(zoneId);
      log.info(format("deleted zone %s in %sms", zoneId, currentTimeMillis() - s));
      return new MockResponse().setResponseCode(204);
    } else {
      return new MockResponse().setResponseCode(405);
    }
  }
}
