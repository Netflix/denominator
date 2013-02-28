package denominator.mock;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

public final class MockZoneApi implements denominator.ZoneApi {
    @Inject
    MockZoneApi() {
    }

    @Override
    public Iterator<String> list() {
        return ImmutableList.of("denominator.io.").iterator();
    }
}