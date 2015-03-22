package denominator.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Singleton;

import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.NothingToClose;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.profile.WeightedResourceRecordSetApi;

import static denominator.common.Preconditions.checkArgument;
import static denominator.model.ResourceRecordSets.notNull;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedMap;

/**
 * in-memory {@code Provider}, used for testing.
 */
public class MockProvider extends BasicProvider {

  private final String url;

  public MockProvider() {
    this(null);
  }

  /**
   * @param url if empty or null use default
   */
  public MockProvider(String url) {
    this.url = url == null || url.isEmpty() ? "mem:mock" : url;
  }

  @Override
  public String url() {
    return url;
  }

  @Override
  public Map<String, Collection<String>> profileToRecordTypes() {
    Map<String, Collection<String>> result = super.profileToRecordTypes();
    List<String> special = new ArrayList<String>(basicRecordTypes());
    special.remove("SOA");
    result.put("geo", Collections.unmodifiableList(special));
    result.put("weighted", result.get("geo"));
    return result;
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, // denominator.Provider
      includes = NothingToClose.class)
  public static final class Module {

    /**
     * Backing data for all views.
     */
    private final Map<String, Collection<ResourceRecordSet<?>>> data;
    private final Map<String, Collection<String>> supportedRegions;
    private final SortedSet<Integer> supportedWeights;

    public Module() {
      data = synchronizedMap(new LinkedHashMap<String, Collection<ResourceRecordSet<?>>>(2));
      SortedSet<Integer> weights = new TreeSet<Integer>();
      for (int i = 0;i <= 100; i++) {
        weights.add(i);
      }
      this.supportedWeights = Collections.unmodifiableSortedSet(weights);
      Map<String, Collection<String>> region = new LinkedHashMap<String, Collection<String>>();
      region.put("United States", asList("AL", "AK", "AS", "AZ", "AR", "AA", "AE", "AP", "CA", "CO",
                                         "CT", "DE", "DC", "FM", "FL", "GA", "GU", "HI", "ID", "IL",
                                         "IN", "IA", "KS", "KY", "LA", "ME", "MH", "MD", "MA", "MI",
                                         "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY",
                                         "NC", "ND", "MP", "OH", "OK", "OR", "PW", "PA", "PR", "RI",
                                         "SC", "SD", "TN", "TX", "UT", "VT", "VI", "VA", "WA", "WV",
                                         "WI", "WY"));
      region.put("Mexico", asList("AG", "CM", "CP", "CH", "CA", "CL", "DU", "GJ", "GR", "HI", "JA",
                                  "MX", "MC", "MR", "NA", "OA", "PU", "QE", "SI", "SO", "TB", "TM",
                                  "TL", "VE", "YU", "ZA"));
      this.supportedRegions = Collections.unmodifiableMap(region);
    }

    @Provides
    CheckConnection alwaysOK() {
      return new CheckConnection() {
        public boolean ok() {
          return true;
        }
      };
    }

    @Provides
    @Singleton
    ZoneApi provideZoneApi() {
      return new MockZoneApi(data);
    }

    @Provides
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory() {
      return new denominator.ResourceRecordSetApi.Factory() {
        @Override
        public ResourceRecordSetApi create(String name) {
          return new MockResourceRecordSetApi(data, name);
        }
      };
    }


    @Provides
    AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApiFactory() {
      return new denominator.AllProfileResourceRecordSetApi.Factory() {
        @Override
        public AllProfileResourceRecordSetApi create(String name) {
          return new MockAllProfileResourceRecordSetApi(data, name, notNull());
        }
      };
    }

    @Provides
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory() {
      return new GeoResourceRecordSetApi.Factory() {
        @Override
        public GeoResourceRecordSetApi create(String name) {
          return new MockGeoResourceRecordSetApi(data, name, supportedRegions);
        }
      };
    }

    @Provides
    WeightedResourceRecordSetApi.Factory provideWeightedResourceRecordSetApiFactory() {
      return new WeightedResourceRecordSetApi.Factory() {
        @Override
        public WeightedResourceRecordSetApi create(String name) {
          return new MockWeightedResourceRecordSetApi(data, name, supportedWeights);
        }
      };
    }
  }
}
