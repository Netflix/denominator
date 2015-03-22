package denominator.route53;

import java.util.Arrays;
import java.util.Iterator;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

import static denominator.common.Util.filter;
import static denominator.model.ResourceRecordSets.alwaysVisible;
import static denominator.route53.Route53.ActionOnResourceRecordSet.delete;

public final class Route53ResourceRecordSetApi implements ResourceRecordSetApi {

  private final Route53AllProfileResourceRecordSetApi allApi;
  private final Route53 api;
  private final String zoneId;

  Route53ResourceRecordSetApi(Route53AllProfileResourceRecordSetApi allProfileResourceRecordSetApi,
                              Route53 api,
                              String zoneId) {
    this.allApi = allProfileResourceRecordSetApi;
    this.api = api;
    this.zoneId = zoneId;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return filter(allApi.iterator(), alwaysVisible());
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return filter(allApi.iterateByName(name), alwaysVisible());
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    ResourceRecordSet<?> rrset = allApi.getByNameAndType(name, type);
    return alwaysVisible().apply(rrset) ? rrset : null;
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    allApi.put(rrset);
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    ResourceRecordSet<?> oldRRS = getByNameAndType(name, type);
    if (oldRRS == null) {
      return;
    }
    api.changeResourceRecordSets(zoneId, Arrays.asList(delete(oldRRS)));
  }

  static final class Factory implements denominator.ResourceRecordSetApi.Factory {

    private final Route53AllProfileResourceRecordSetApi.Factory allApi;
    private final Route53 api;

    @Inject
    Factory(Route53AllProfileResourceRecordSetApi.Factory allApi, Route53 api) {
      this.allApi = allApi;
      this.api = api;
    }

    @Override
    public ResourceRecordSetApi create(String id) {
      return new Route53ResourceRecordSetApi(allApi.create(id), api, id);
    }
  }
}
