package denominator.clouddns;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static denominator.model.ResourceRecordSets.nameEqualTo;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.features.RecordApi;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

public final class CloudDNSResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final RecordApi api;

    CloudDNSResourceRecordSetApi(RecordApi recordApi) {
        this.api = recordApi;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        // assumes these are sorted, which might be bad
        return new GroupByRecordNameAndTypeIterator(api.list().concat().iterator());
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        checkNotNull(name, "name was null");
        return Iterators.filter(list(), nameEqualTo(name));
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        checkNotNull(name, "name was null");
        checkNotNull(type, "type was null");
        GroupByRecordNameAndTypeIterator it = new GroupByRecordNameAndTypeIterator(api.listByNameAndType(name, type)
                .concat().iterator());
        return it.hasNext() ? Optional.<ResourceRecordSet<?>> of(it.next()) : Optional.<ResourceRecordSet<?>> absent();
    }

    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final CloudDNSApi api;

        @Inject
        Factory(CloudDNSApi api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(final String domainName) {
            Optional<Domain> domain = api.getDomainApi().list().concat().firstMatch(domainNameEquals(domainName));
            checkArgument(domain.isPresent(), "domain %s not found", domainName);
            return new CloudDNSResourceRecordSetApi(api.getRecordApiForDomain(domain.get().getId()));
        }
    }

    /**
     * Rackspace domains are addressed by id, not by name.
     */
    private static final Predicate<Domain> domainNameEquals(final String domainName) {
        checkNotNull(domainName, "domainName");
        return new Predicate<Domain>() {
            @Override
            public boolean apply(Domain input) {
                return input.getName().equals(domainName);
            }
        };
    }

    @Override
    public void add(ResourceRecordSet<?> rrset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void applyTTLToNameAndType(int ttl, String name, String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replace(ResourceRecordSet<?> rrset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        throw new UnsupportedOperationException();
    }
}
