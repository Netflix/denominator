package denominator.config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.ReadOnlyResourceRecordSetApi;
import denominator.ResourceRecordSetApi;

/**
 * @deprecated Will be removed in denominator 2.0. Please use
 *             {@link OnlyBasicResourceRecordSets}
 */
@Deprecated
@Module(injects = DNSApiManager.class, complete = false)
public class OnlyNormalResourceRecordSets {

    @Provides
    @Singleton
    ReadOnlyResourceRecordSetApi.Factory provideReadOnlyResourceRecordSetApiFactory(
            AllProfileResourceRecordSetApi.Factory factory) {
        return factory;
    }

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(ResourceRecordSetApi.Factory factory) {
        return new OnlyBasicResourceRecordSets().provideAllProfileResourceRecordSetApi(factory);
    }
}
