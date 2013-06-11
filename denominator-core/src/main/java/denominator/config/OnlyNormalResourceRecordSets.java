package denominator.config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
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
    AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(ResourceRecordSetApi.Factory factory) {
        return new OnlyBasicResourceRecordSets().provideResourceRecordSetApiFactory(factory);
    }
}
