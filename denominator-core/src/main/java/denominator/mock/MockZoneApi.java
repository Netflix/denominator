package denominator.mock;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;

public final class MockZoneApi implements denominator.ZoneApi {
    private final Multimap<String, ResourceRecordSet<?>> data;

    // wildcard types are not currently injectable in dagger
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Inject
    MockZoneApi(Multimap<String, ResourceRecordSet> data) {
        this.data = Multimap.class.cast(data);
    }

    @Override
    public Iterator<String> list() {
        return data.keySet().iterator();
    }
}