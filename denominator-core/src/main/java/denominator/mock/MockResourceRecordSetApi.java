package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterators.contains;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.ns;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;

import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.SOAData;

public final class MockResourceRecordSetApi implements denominator.ResourceRecordSetApi {
    public static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final ZoneApi zoneApi;

        @Inject
        Factory(ZoneApi zoneApi) {
            this.zoneApi = zoneApi;
        }

        @Override
        public ResourceRecordSetApi create(String zoneName) {
            checkArgument(contains(zoneApi.list(), zoneName), "zone %s not found", zoneName);
            return new MockResourceRecordSetApi(zoneName);
        }
    }

    final String zoneName;

    @Inject
    MockResourceRecordSetApi(String zoneName) {
        this.zoneName = zoneName;
    }

    /**
     * adds a bunch of fake records
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        Builder<ResourceRecordSet<?>> builder = ImmutableList.builder();
        builder.add(ResourceRecordSet.builder()
                                     .type("SOA")
                                     .name(zoneName)
                                     .ttl(3600)
                                     .add(SOAData.builder()
                                                 .mname("ns1." + zoneName)
                                                 .rname("admin." + zoneName)
                                                 .serial(1)
                                                 .refresh(3600)
                                                 .retry(600)
                                                 .expire(604800)
                                                 .minimum(60).build()).build());
        builder.add(ns(zoneName, 86400, "ns1." + zoneName));
        builder.add(a("www1." + zoneName, 3600, ImmutableSet.of("1.1.1.1", "1.1.1.2")));
        builder.add(a("www2." + zoneName, 3600, "2.2.2.2"));
        builder.add(cname("www." + zoneName, 3600, ImmutableSet.of("www1." + zoneName, "www2." + zoneName)));
        return builder.build().iterator();
    }
}