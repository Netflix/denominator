package denominator.dynect;

import static denominator.common.Util.and;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.nameTypeAndQualifierEqualTo;
import static denominator.model.ResourceRecordSets.profileContainsType;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;

public final class DynECTGeoResourceRecordSetApi implements GeoResourceRecordSetApi {
    private static final Filter<ResourceRecordSet<?>> IS_GEO = profileContainsType("geo");

    private final Map<String, Collection<String>> regions;
    private final DynECT api;
    private final String zoneFQDN;

    DynECTGeoResourceRecordSetApi(Map<String, Collection<String>> regions, DynECT api, String zoneFQDN) {
        this.regions = regions;
        this.api = api;
        this.zoneFQDN = zoneFQDN;
    }

    @Override
    public Map<String, Collection<String>> supportedRegions() {
        return regions;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        Collection<ResourceRecordSet<?>> val = api.geoRRSetsByZone().get(zoneFQDN);
        return val != null ? val.iterator() : Collections.<ResourceRecordSet<?>> emptyList().iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return filter(iterator(), and(nameEqualTo(name), IS_GEO));
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        return filter(iterator(), and(nameAndTypeEqualTo(name, type), IS_GEO));
    }

    @Override
    public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        return nextOrNull(filter(iterator(), and(nameTypeAndQualifierEqualTo(name, type, qualifier), IS_GEO)));
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
        throw new UnsupportedOperationException();
    }

    static final class Factory implements GeoResourceRecordSetApi.Factory {
        private final Map<String, Collection<String>> regions;
        private final DynECT api;

        @Inject
        Factory(@Named("geo") Map<String, Collection<String>> regions, DynECT api) {
            this.regions = regions;
            this.api = api;
        }

        @Override
        public GeoResourceRecordSetApi create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return new DynECTGeoResourceRecordSetApi(regions, api, idOrName);
        }
    }
}
