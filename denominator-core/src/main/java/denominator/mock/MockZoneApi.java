package denominator.mock;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.Zones;

public final class MockZoneApi implements denominator.ZoneApi {
    private final Multimap<Zone, ResourceRecordSet<?>> data;

    // wildcard types are not currently injectable in dagger
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Inject
    MockZoneApi(Multimap<Zone, ResourceRecordSet> data) {
        this.data = Multimap.class.cast(data);
    }

    @Override
    public Iterator<String> list() {
        return FluentIterable.from(data.keySet())
                             .transform(Zones.toName())
                             .toSet().iterator();
    }

    @Override
    public Iterator<Zone> iterator() {
        return data.keySet().iterator();
    }
}