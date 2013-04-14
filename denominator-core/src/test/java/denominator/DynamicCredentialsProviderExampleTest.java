package denominator;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Lists.transform;
import static denominator.CredentialsConfiguration.anonymous;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials.ListCredentials;
import denominator.CredentialsConfiguration.CredentialsAsList;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyNormalResourceRecordSets;
import denominator.mock.MockResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

@Test
public class DynamicCredentialsProviderExampleTest {
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
     * in this example, the zone api lazy uses domain-specific credentials on
     * each invocation.
     */
    static class DynamicCredentialsZoneApi implements ZoneApi {
        private final Supplier<CustomerUsernamePassword> creds;

        /**
         * Supplier facilitates dynamic credentials.
         */
        @Inject
        DynamicCredentialsZoneApi(Supplier<CustomerUsernamePassword> creds) {
            this.creds = creds;
        }

        /**
         * make sure you note in your javadoc that IllegalArgumentException can
         * arise when the user supplies the incorrect form of credentials.
         */
        @Override
        public Iterator<String> list() {
            // IllegalArgumentException is possible on lazy get
            CustomerUsernamePassword cup = creds.get();
            // normally, the credentials object would be used to invoke a remote
            // command. in this case, we don't and say we did :)
            return ImmutableList.of(cup.customer, cup.username, cup.password).iterator();
        }
    }

    // incomplete as it requires credentials to be bound externally
    @Module(entryPoints = DNSApiManager.class,
               complete = false,
               includes = { NothingToClose.class,
                            GeoUnsupported.class,
                            OnlyNormalResourceRecordSets.class } )
    static class DynamicCredentialsProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }

        /**
         * override name as default would be {@code dynamiccredentials} based on the class name.
         */
        @Provides
        public String getName() {
            return "dynamic";
        }

        /**
         * simulates remote credential management where credentials can change
         * across requests, or accidentally return null.
         */
        @Override
        public Optional<Supplier<Credentials>> defaultCredentialSupplier() {
            return Optional.<Supplier<Credentials>> of(new Supplier<Credentials>() {

                AtomicInteger credentialIndex = new AtomicInteger();
                List<List<String>> credentials = ImmutableList.<List<String>> builder()
                        .add(ImmutableList.of("acme", "wily", "coyote"))
                        .add(ImmutableList.of("acme", "road", "runner"))
                        .build();

                public Credentials get() {
                    int index = credentialIndex.getAndIncrement();
                    if (credentials.size() < (index + 1))
                        return null; // simulate no credentials
                    List<String> current = credentials.get(index);
                    return ListCredentials.from(current);
                }
            });
        }
        /**
         * wiring of the zone api requires a Supplier as otherwise credentials
         * would not be able to dynamically update.
         */
        @Provides
        @Singleton
        ZoneApi provideZoneApi(Supplier<CustomerUsernamePassword> creds) {
            return new DynamicCredentialsZoneApi(creds);
        }

        /**
         * using mock as example case is made already with the zone api
         */
        @Provides
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(MockResourceRecordSetApi.Factory in) {
            return in;
        }

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings("rawtypes")
        @Provides
        @Singleton
        Multimap<String, ResourceRecordSet> provideData() {
            return ImmutableMultimap.of();
        }

        /**
         * avoid too much logic in the Module. For example, we've extracted the
         * logic that implements a supplier of {@code CustomerUsernamePassword}
         * to its own class.
         */
        @Provides
        @Singleton
        Supplier<CustomerUsernamePassword> useCorrectCredentials(CredentialsAsList supplier) {
            return Suppliers.compose(new CustomerUsernamePasswordFromList(), supplier);
        }

        /**
         * inform the user that we need credentials with 3 parameters.
         */
        @Override
        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("username", "customer", "username", "password").build();
        }

        /**
         * assured by {@link CredentialsAsList}, we have customer, username, and
         * password in correct order.
         */
        static class CustomerUsernamePasswordFromList implements Function<List<Object>, CustomerUsernamePassword> {
            public CustomerUsernamePassword apply(List<Object> creds) {
                List<String> strings = transform(creds, toStringFunction());
                return new CustomerUsernamePassword(strings.get(0), strings.get(1), strings.get(2));
            }
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. dynamic requires customer, username, password")
    public void testCredentialsRequired() {
        // shouldn't exception here, as credentials aren't yet used
        DNSApiManager mgr = create(new DynamicCredentialsProvider(), anonymous());
        // shouldn't exception here, as credentials aren't yet used
        ZoneApi zoneApi = mgr.getApi().getZoneApi();
        zoneApi.list();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. dynamic requires customer, username, password")
    public void testThreePartCredentialsRequired() {
        // shouldn't exception here, as credentials aren't yet used
        DNSApiManager mgr = create(new DynamicCredentialsProvider(), credentials("username", "password"));
        // shouldn't exception here, as credentials aren't yet used
        ZoneApi zoneApi = mgr.getApi().getZoneApi();
        zoneApi.list();
    }

    @Test
    public void testImplicitDynamicCredentialsUpdate() {
        DNSApiManager mgr = create(new DynamicCredentialsProvider());
        ZoneApi zoneApi = mgr.getApi().getZoneApi();
        assertEquals(ImmutableList.copyOf(zoneApi.list()), ImmutableList.of("acme", "wily", "coyote"));
        assertEquals(ImmutableList.copyOf(zoneApi.list()), ImmutableList.of("acme", "road", "runner"));
        // now, if the supplier doesn't supply a set of credentials, we should
        // get a correct message
        try {
            ImmutableList.copyOf(zoneApi.list());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "no credentials supplied. dynamic requires customer, username, password");
        }
    }

}
