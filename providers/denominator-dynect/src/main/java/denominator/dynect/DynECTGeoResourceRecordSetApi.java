package denominator.dynect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.tryFind;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.qualifierEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;

public final class DynECTGeoResourceRecordSetApi implements GeoResourceRecordSetApi {
    private static final Predicate<ResourceRecordSet<?>> IS_GEO = profileContainsType("geo");

    private final Multimap<String, String> regions;
    private final DynECT api;
    private final String zoneFQDN;

    DynECTGeoResourceRecordSetApi(Multimap<String, String> regions, DynECT api, String zoneFQDN) {
        this.regions = regions;
        this.api = api;
        this.zoneFQDN = zoneFQDN;
    }

    @Override
    public Multimap<String, String> supportedRegions() {
        return regions;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return api.geoRRSetsByZone().get(zoneFQDN).iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return filter(iterator(), and(nameEqualTo(name), IS_GEO));
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        return filter(iterator(), and(nameEqualTo(name), typeEqualTo(type), IS_GEO));
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        return tryFind(iterator(), and(nameEqualTo(name), typeEqualTo(type), qualifierEqualTo(qualifier), IS_GEO));
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
        private final Multimap<String, String> regions;
        private final DynECT api;

        @Inject
        Factory(@Named("geo") Multimap<String, String> regions, DynECT api) {
            this.regions = regions;
            this.api = api;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return Optional.<GeoResourceRecordSetApi> of(new DynECTGeoResourceRecordSetApi(regions, api, idOrName));
        }
    }
}
