package denominator.denominatord;


import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.ReadOnlyResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

import static denominator.common.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class RecordSetDispatcher extends Dispatcher {

  static final Pattern RECORDSET_PATTERN = Pattern.compile("/zones/([\\.\\w]+)/recordsets(\\?.*)?");

  private final Logger log = Logger.getLogger(Dispatcher.class.getName());
  private final DNSApiManager mgr;
  private final JsonCodec codec;

  RecordSetDispatcher(DNSApiManager mgr, JsonCodec codec) {
    this.mgr = mgr;
    this.codec = codec;
  }

  @Override
  public MockResponse dispatch(RecordedRequest request) {
    Matcher matcher = RECORDSET_PATTERN.matcher(request.getPath());
    if (!matcher.matches()) {
      return new MockResponse().setResponseCode(404);
    }
    String zoneIdOrName = matcher.group(1);
    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(zoneIdOrName);
    checkArgument(api != null, "cannot control record sets in zone %s", zoneIdOrName);
    if (request.getMethod().equals("GET")) {
      Query query = Query.from(request.getPath());
      return codec.toJsonArray(recordSetsForQuery(api, query));
    } else if (request.getMethod().equals("PUT")) {
      ResourceRecordSet<?> recordSet = codec.readJson(request, ResourceRecordSet.class);
      Query query = Query.from(recordSet);
      long s = currentTimeMillis();
      log.info(format("replacing recordset %s", query));
      api.put(recordSet);
      log.info(format("replaced recordset %s in %sms", query, currentTimeMillis() - s));
      return new MockResponse().setResponseCode(204);
    } else if (request.getMethod().equals("DELETE")) {
      Query query = Query.from(request.getPath());
      long s = currentTimeMillis();
      log.info(format("deleting recordset %s ", query));
      if (query.qualifier != null) {
        api.deleteByNameTypeAndQualifier(query.name, query.type, query.qualifier);
      } else if (query.type != null) {
        checkArgument(query.name != null, "name query required with type");
        api.deleteByNameAndType(query.name, query.type);
      } else if (query.name != null) {
        throw new IllegalArgumentException("you must specify both name and type when deleting");
      }
      log.info(format("deleted recordset %s in %sms", query, currentTimeMillis() - s));
      return new MockResponse().setResponseCode(204);
    } else {
      return new MockResponse().setResponseCode(405);
    }
  }

  static Iterator<?> recordSetsForQuery(ReadOnlyResourceRecordSetApi api, Query query) {
    if (query.qualifier != null) {
      ResourceRecordSet<?> rrset =
          api.getByNameTypeAndQualifier(query.name, query.type, query.qualifier);
      return rrset != null ? singleton(rrset).iterator() : emptySet().iterator();
    } else if (query.type != null) {
      return api.iterateByNameAndType(query.name, query.type);
    } else if (query.name != null) {
      return api.iterateByName(query.name);
    }
    return api.iterator();
  }
}
