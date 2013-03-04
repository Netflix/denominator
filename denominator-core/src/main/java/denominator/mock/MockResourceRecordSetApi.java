package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.collect.Multimap;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;


public final class MockResourceRecordSetApi implements denominator.ResourceRecordSetApi {
    public static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final Multimap<String, ResourceRecordSet<?>> data;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<String, ResourceRecordSet> data) {
            this.data = Multimap.class.cast(data);
        }

        @Override
        public ResourceRecordSetApi create(String zoneName) {
            checkArgument(data.keySet().contains(zoneName), "zone %s not found", zoneName);
            return new MockResourceRecordSetApi(data, zoneName);
        }
    }

    private final Multimap<String, ResourceRecordSet<?>> data;
    private final String zoneName;

    MockResourceRecordSetApi(Multimap<String, ResourceRecordSet<?>> data, String zoneName) {
        this.data = data;
        this.zoneName = zoneName;
    }

    /**
     * adds a bunch of fake records
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return data.get(zoneName).iterator();
    }
}