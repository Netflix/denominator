package denominator.route53;

import static com.google.common.collect.Iterators.filter;
import static denominator.model.ResourceRecordSets.withoutProfile;
import static denominator.route53.Route53.ActionOnResourceRecordSet.delete;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

public final class Route53ResourceRecordSetApi implements ResourceRecordSetApi {

    private final Route53AllProfileResourceRecordSetApi allApi;
    private final Route53 api;
    private final String zoneId;

    Route53ResourceRecordSetApi(Route53AllProfileResourceRecordSetApi allProfileResourceRecordSetApi, Route53 api,
            String zoneId) {
        this.allApi = allProfileResourceRecordSetApi;
        this.api = api;
        this.zoneId = zoneId;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return filter(allApi.iterator(), withoutProfile());
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return filter(allApi.iterateByName(name), withoutProfile());
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        return allApi.getByNameAndType(name, type);
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        allApi.put(rrset);
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        Optional<ResourceRecordSet<?>> oldRRS = getByNameAndType(name, type);
        if (!oldRRS.isPresent())
            return;
        api.changeBatch(zoneId, ImmutableList.of(delete(oldRRS.get())));
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