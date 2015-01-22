package denominator.verisignmdns;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.model.ResourceRecordSets.notNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Provider;

import denominator.AllProfileResourceRecordSetApi;
import denominator.Credentials;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.verisignmdns.VrsnMdns.Record;

public class VerisignMDNSAllProfileResourceRecordSetApi implements denominator.AllProfileResourceRecordSetApi {
    private final String domainName;
    private final VrsnMdns api;
    private final Provider<Credentials> credentialsProvider;

    VerisignMDNSAllProfileResourceRecordSetApi(Provider<Credentials> credentialsProvider, String domainName,
            VrsnMdns api) {
        this.domainName = domainName;
        this.api = api;
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Returns Sorted Set of Resource Record Set to to keep behavior similar to
     * MDNS web UI.
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        List<Record> recordList = api.getResourceRecordsList(domainName);
        return VrsnContentConversionHelper.getSortedSetForDenominator(recordList).iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        checkNotNull(name, "name was null");
        throw new VrsnMdnsException("Method Not Implemented", -1);
    }

    protected void put(Filter<ResourceRecordSet<?>> valid, ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier() != null, "no qualifier on: %s", rrset);
        checkArgument(valid.apply(rrset), "%s failed on: %s", valid, rrset);

        // @TODO IMPLEMENT -- in future development phase /////////
        throw new VrsnMdnsException("Method Not Implemented", -1);

    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        // @ Keeping MOCK code for now...
        // @TODO IMPLEMENT -- in future development phase /////////
        put(notNull(), rrset);
        throw new VrsnMdnsException("Method Not Implemented", -1);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        checkNotNull(type, "type was null");
        checkNotNull(name, "name was null");

        List<Record> recordList = api.getResourceRecordsListForTypeAndName(domainName, type, name);
        Iterator<ResourceRecordSet<?>> result = VrsnContentConversionHelper.getSortedSetForDenominator(recordList)
                .iterator();
        return result;
    }

    /**
     * NOTE- for MDNS get only required ResourceRecordId ie. qualifier.
     * Parameters name and type are ignored.
     */
    @Override
    public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        ResourceRecordSet<?> result = null;
        List<Record> recordList = api.getResourceRecordByQualifier(qualifier);
        SortedSet<ResourceRecordSet<?>> rrSet = VrsnContentConversionHelper.getSortedSetForDenominator(recordList);
        if (!rrSet.isEmpty()) {
            result = rrSet.first();
        }
        return result;
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
        // @ Keeping MOCK code for now...
        // @TODO IMPLEMENT -- in future development phase /////////
        throw new VrsnMdnsException("Method Not Implemented", -1);

    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        throw new VrsnMdnsException("Method Not Implemented", -1);
    }

    static class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {
        private Map<Zone, SortedSet<ResourceRecordSet<?>>> records;
        private String domainName;
        private VrsnMdns api;
        private final Provider<Credentials> credentialsProvider;

        // unbound wildcards are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Provider<Credentials> credentialsProvider, denominator.Provider provider, VrsnMdns api) {
            this.records = Map.class.cast(records);
            this.credentialsProvider = credentialsProvider;
            this.api = api;
        }

        @Override
        public AllProfileResourceRecordSetApi create(String idOrName) {
            return new VerisignMDNSAllProfileResourceRecordSetApi(credentialsProvider, idOrName, api);
        }
    }
}
