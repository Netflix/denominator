package denominator.ultradns;

import java.util.Collection;
import java.util.Map;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module(injects = UltraDNSRestGeoResourceRecordSetApi.Factory.class, complete = false)
public class UltraDNSRestGeoSupport {

  @Provides
  @Named("geo")
  Map<String, Collection<String>> regions(UltraDNSRest api) {
    return api.getAvailableRegions();
  }
}
