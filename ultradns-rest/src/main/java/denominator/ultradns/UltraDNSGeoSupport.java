package denominator.ultradns;

import java.util.Collection;
import java.util.Map;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module(injects = UltraDNSGeoResourceRecordSetApi.Factory.class, complete = false)
public class UltraDNSGeoSupport {

  @Provides
  @Named("geo")
  Map<String, Collection<String>> regions(UltraDNS api) {
    return api.getAvailableRegions();
  }
}
