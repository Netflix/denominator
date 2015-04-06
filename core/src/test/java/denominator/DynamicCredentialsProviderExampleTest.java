package denominator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import denominator.Credentials.ListCredentials;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.model.Zone;

import static denominator.CredentialsConfiguration.anonymous;
import static denominator.CredentialsConfiguration.checkValidForProvider;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class DynamicCredentialsProviderExampleTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. dynamic requires customer,username,password");

    // shouldn't exception here, as credentials aren't yet used
    DNSApiManager mgr = create(new DynamicCredentialsProvider(), anonymous());
    // shouldn't exception here, as credentials aren't yet used
    ZoneApi zones = mgr.api().zones();
    zones.iterator();
  }

  @Test
  public void testThreePartCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("incorrect credentials supplied. dynamic requires customer,username,password");

    // shouldn't exception here, as credentials aren't yet used
    DNSApiManager
        mgr =
        create(new DynamicCredentialsProvider(), credentials("username", "password"));
    // shouldn't exception here, as credentials aren't yet used
    ZoneApi zones = mgr.api().zones();
    zones.iterator();
  }

  @Test
  public void testImplicitDynamicCredentialsUpdate() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. dynamic requires customer,username,password");

    DNSApiManager mgr = create(new DynamicCredentialsProvider());
    ZoneApi zones = mgr.api().zones();
    assertThat(zones.iterator())
        .containsExactly(Zone.create("acme", "wily", 86400, "coyote"));
    assertThat(zones.iterator())
        .containsExactly(Zone.create("acme", "road", 86400, "runner"));

    // now, if the supplier doesn't supply a set of credentials, we should
    // get a correct message
    zones.iterator();
  }

  /**
   * example of domain-specific credentials.
   */
  static class CustomerUsernamePassword {

    private final String customer;
    private final String username;
    private final String password;

    private CustomerUsernamePassword(String customer, String username, String password) {
      this.customer = customer;
      this.username = username;
      this.password = password;
    }
  }

  /**
   * in this example, the zone api lazy uses domain-specific credentials on each invocation.
   */
  static class DynamicCredentialsZoneApi implements ZoneApi {

    private final javax.inject.Provider<CustomerUsernamePassword> creds;

    /**
     * Supplier facilitates dynamic credentials.
     */
    @Inject
    DynamicCredentialsZoneApi(javax.inject.Provider<CustomerUsernamePassword> creds) {
      this.creds = creds;
    }

    /**
     * make sure you note in your javadoc that IllegalArgumentException can arise when the user
     * supplies the incorrect form of credentials.
     */
    @Override
    public Iterator<Zone> iterator() {
      // IllegalArgumentException is possible on lazy get
      CustomerUsernamePassword cup = creds.get();
      // normally, the credentials object would be used to invoke a remote
      // command. in this case, we don't and say we did :)
      return asList(Zone.create(cup.customer, cup.username, 86400, cup.password)).iterator();
    }

    @Override
    public Iterator<Zone> iterateByName(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String put(Zone zone) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String idOrName) {
      throw new UnsupportedOperationException();
    }
  }

  final static class DynamicCredentialsProvider extends BasicProvider {

    /**
     * override name as default would be {@code dynamiccredentials} based on the class name.
     */
    @Override
    public String name() {
      return "dynamic";
    }

    /**
     * inform the user that we need credentials with 3 parameters.
     */
    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
      options.put("username", asList("customer", "username", "password"));
      return options;
    }

    @dagger.Module(injects = DNSApiManager.class,
        complete = false, // denominator.Provider is externally provided
        includes = {NothingToClose.class,
                    GeoUnsupported.class,
                    WeightedUnsupported.class,
                    OnlyBasicResourceRecordSets.class})
    static class Module {

      final AtomicInteger credentialIndex = new AtomicInteger();
      final List<List<String>> credentials = asList(
          asList("acme", "wily", "coyote"),
          asList("acme", "road", "runner")
      );

      /**
       * simulates remote credential management where credentials can change across requests, or
       * accidentally return null.
       */
      @Provides
      Credentials dynamicCredentials() {
        int index = credentialIndex.getAndIncrement();
        if (credentials.size() < (index + 1)) {
          return null; // simulate no credentials
        }
        List<String> current = credentials.get(index);
        return ListCredentials.from(current);
      }

      /**
       * wiring of the zone api requires a Supplier as otherwise credentials would not be able to
       * dynamically update.
       */
      @Provides
      @Singleton
      ZoneApi provideZoneApi(javax.inject.Provider<CustomerUsernamePassword> creds) {
        return new DynamicCredentialsZoneApi(creds);
      }

      /**
       * using mock as example case is made already with the zone api
       */
      @Provides
      ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory() {
        return new ResourceRecordSetApi.Factory() {
          final DNSApi mock = Denominator.create("mock").api();

          @Override
          public ResourceRecordSetApi create(String id) {
            return mock.basicRecordSetsInZone(id);
          }
        };
      }

      /**
       * @param credsProvider expected to return customer, username, and password in correct order
       */
      @Provides
      CustomerUsernamePassword fromListCredentials(Provider provider,
                                                   javax.inject.Provider<denominator.Credentials> credsProvider) {
        ListCredentials
            creds =
            (ListCredentials) checkValidForProvider(credsProvider.get(), provider);
        return new CustomerUsernamePassword(creds.get(0).toString(), creds.get(1).toString(),
                                            creds.get(2).toString());
      }

      @Provides
      CheckConnection alwaysOK() {
        return new CheckConnection() {
          public boolean ok() {
            return true;
          }
        };
      }
    }
  }
}
