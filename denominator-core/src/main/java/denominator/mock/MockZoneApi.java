package denominator.mock;

import javax.inject.Inject;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

public final class MockZoneApi implements denominator.ZoneApi {
    @Inject
    MockZoneApi() {
    }

    @Override
    public FluentIterable<String> list() {
        return FluentIterable.from(ImmutableList.of("denominator.io."));
    }
}