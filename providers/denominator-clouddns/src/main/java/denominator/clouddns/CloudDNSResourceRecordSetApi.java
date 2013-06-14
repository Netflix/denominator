package denominator.clouddns;

import static com.google.common.base.Preconditions.checkNotNull;
import static denominator.model.ResourceRecordSets.nameEqualTo;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.features.RecordApi;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

public final class CloudDNSResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final RecordApi api;

    CloudDNSResourceRecordSetApi(RecordApi recordApi) {
        this.api = recordApi;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        // assumes these are sorted, which might be bad
        return new GroupByRecordNameAndTypeIterator(api.list().concat().iterator());
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        checkNotNull(name, "name was null");
        return Iterators.filter(iterator(), nameEqualTo(name));
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        checkNotNull(name, "name was null");
        checkNotNull(type, "type was null");
        GroupByRecordNameAndTypeIterator it = new GroupByRecordNameAndTypeIterator(api.listByNameAndType(name, type)
                .concat().iterator());
        return it.hasNext() ? Optional.<ResourceRecordSet<?>> of(it.next()) : Optional.<ResourceRecordSet<?>> absent();
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        throw new UnsupportedOperationException();
    }

    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final CloudDNSApi api;

        @Inject
        Factory(CloudDNSApi api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(final String id) {
            return new CloudDNSResourceRecordSetApi(api.getRecordApiForDomain(Integer.parseInt(id)));
        }
    }
}
