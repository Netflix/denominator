package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.qualifierEqualTo;
import static denominator.model.ResourceRecordSets.toProfileTypes;
import static denominator.model.ResourceRecordSets.typeEqualTo;

import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Multimap;

import denominator.AllProfileResourceRecordSetApi;
import denominator.Provider;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public class MockAllProfileResourceRecordSetApi implements denominator.AllProfileResourceRecordSetApi {

    protected final Provider provider;
    protected final Multimap<Zone, ResourceRecordSet<?>> records;
    protected final Zone zone;

    MockAllProfileResourceRecordSetApi(Provider provider, Multimap<Zone, ResourceRecordSet<?>> records, Zone zone) {
        this.provider = provider;
        this.records = records;
        this.zone = zone;
    }

    @Deprecated
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return iterator();
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return usingToString().immutableSortedCopy(records.get(zone)).iterator();
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        return iterateByName(name);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        checkNotNull(name, "name");
        return from(records.get(zone))
                .filter(nameEqualTo(name))
                .toSortedList(usingToString())
                .iterator();
    }

    protected void put(Predicate<ResourceRecordSet<?>> valid, ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier().isPresent(), "no qualifier on: %s", rrset);
        checkArgument(valid.apply(rrset), "%s failed on: %s", valid, rrset);
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset
                .qualifier().get());
        if (rrsMatch.isPresent()) {
            records.remove(zone, rrsMatch.get());
        }
        records.put(zone, rrset);
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        Set<String> profiles = toProfileTypes(rrset);
        checkArgument(provider.profileToRecordTypes().keySet().containsAll(profiles),
                "cannot put rrset %s:%s%s as it contains profiles %s which aren't supported %s", rrset.name(),
                rrset.type(), rrset.qualifier().isPresent() ? ":" + rrset.qualifier().get() : "", profiles,
                provider.profileToRecordTypes());
        put(Predicates.<ResourceRecordSet<?>> alwaysTrue(), rrset);
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
        return iterateByNameAndType(name, type);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(records.get(zone))
                .filter(nameAndTypeEqualTo(name, type))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(type, "qualifier");
        return from(records.get(zone))
                .firstMatch(and(nameAndTypeEqualTo(name, type), qualifierEqualTo(qualifier)));
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameTypeAndQualifier(name, type, qualifier);
        if (rrsMatch.isPresent()) {
            records.remove(zone, rrsMatch.get());
        }
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        for (Iterator<ResourceRecordSet<?>> it = iterateByNameAndType(name, type); it.hasNext();) {
            records.remove(zone, it.next());
        }
    }

    static Predicate<ResourceRecordSet<?>> nameAndTypeEqualTo(String name, String type) {
        return and(nameEqualTo(name), typeEqualTo(type));
    }

    static class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

        private final Provider provider;
        private final Multimap<Zone, ResourceRecordSet<?>> records;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Provider provider, Multimap<Zone, ResourceRecordSet> records) {
            this.provider = provider;
            this.records = Multimap.class.cast(records);
        }

        @Override
        public AllProfileResourceRecordSetApi create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return new MockAllProfileResourceRecordSetApi(provider, records, zone);
        }
    }
}
