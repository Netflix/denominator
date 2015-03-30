package denominator.clouddns;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.model.Zone;

class CloudDNSZoneApi implements denominator.ZoneApi {

  private final CloudDNS api;

  @Inject
  CloudDNSZoneApi(CloudDNS api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    return api.domains().iterator();
  }

  @Override
  public Iterator<Zone> iterateByName(String name) {
    return api.domainsByName(name).iterator();
  }
}
