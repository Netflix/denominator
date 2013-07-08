package denominator.designate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.tryFind;
import static com.google.common.collect.Lists.newArrayList;
import static denominator.designate.DesignateFunctions.toRDataMap;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

import denominator.ResourceRecordSetApi;
import denominator.designate.OpenStackApis.Designate;
import denominator.designate.OpenStackApis.Record;
import denominator.model.ResourceRecordSet;

class DesignateResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final Designate api;
    private final String domainId;

    DesignateResourceRecordSetApi(Designate api, String domainId) {
        this.api = api;
        this.domainId = domainId;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return new GroupByRecordNameAndTypeIterator(api.records(domainId).iterator());
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        // TODO: investigate query by name call in designate
        return filter(iterator(), nameEqualTo(name));
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        // TODO: investigate query by name and type call in designate
        return tryFind(iterator(), and(nameEqualTo(name), typeEqualTo(type)));
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);

        List<Map<String, Object>> recordsLeftToCreate = newArrayList(rrset.records());

        for (Record record : api.records(domainId)) {
            // TODO: name and type filter
            if (rrset.name().equals(record.name) && rrset.type().equals(record.type)) {
                Map<String, Object> rdata = toRDataMap(record);
                if (recordsLeftToCreate.contains(rdata)) {
                    recordsLeftToCreate.remove(rdata);
                    if (rrset.ttl().isPresent()) {
                        if (rrset.ttl().get().equals(record.ttl)) {
                            continue;
                        }
                        record.ttl = rrset.ttl().get();
                        api.updateRecord(domainId, record);
                    }
                } else {
                    api.deleteRecord(domainId, record.id);
                }
            }
        }

        Record record = new Record();
        record.name = rrset.name();
        record.type = rrset.type();
        record.ttl = rrset.ttl().orNull();

        for (Map<String, Object> rdata : recordsLeftToCreate) {
            LinkedHashMap<String, Object> mutable = new LinkedHashMap<String, Object>(rdata);
            if (mutable.containsKey("priority")) { // SRVData
                record.priority = Integer.class.cast(mutable.remove("priority"));
            } else if (mutable.containsKey("preference")) { // MXData
                record.priority = Integer.class.cast(mutable.remove("preference"));
            } else {
                record.priority = null;
            }
            record.data = Joiner.on(' ').join(mutable.values());
            api.createRecord(domainId, record);
        }
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        for (Record record : api.records(domainId)) {
            // TODO: name and type filter
            if (name.equals(record.name) && type.equals(record.type)) {
                api.deleteRecord(domainId, record.id);
            }
        }
    }

    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final Designate api;

        @Inject
        Factory(Designate api) {
            this.api = checkNotNull(api, "api");
        }

        @Override
        public ResourceRecordSetApi create(String id) {
            return new DesignateResourceRecordSetApi(api, checkNotNull(id, "id"));
        }
    }
}
