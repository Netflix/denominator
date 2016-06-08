package denominator.verisigndns;

import static denominator.common.Util.nextOrNull;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

final class VerisignDnsResourceRecordSetApi implements ResourceRecordSetApi {

  private final VerisignDnsAllProfileResourceRecordSetApi allApi;

  public VerisignDnsResourceRecordSetApi(VerisignDnsAllProfileResourceRecordSetApi allApi) {
    this.allApi = allApi;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return allApi.iterator();
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return allApi.iterateByName(name);
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    return nextOrNull(allApi.iterateByNameAndType(name, type));
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    allApi.put(rrset);
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    allApi.deleteByNameAndType(name, type);
  }

  static final class Factory implements ResourceRecordSetApi.Factory {

    private final VerisignDnsAllProfileResourceRecordSetApi.Factory allApi;

    @Inject
    Factory(VerisignDnsAllProfileResourceRecordSetApi.Factory allApi) {
      this.allApi = allApi;
    }

    @Override
    public ResourceRecordSetApi create(String name) {
      return new VerisignDnsResourceRecordSetApi(allApi.create(name));
    }
  }

}
