package denominator.stub;

import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

final class StubZoneApi implements denominator.ZoneApi {
    @Inject
    StubZoneApi() {
    }

    @Override
    public List<String> list() {
        return ImmutableList.of("denominator.io");
    }
}