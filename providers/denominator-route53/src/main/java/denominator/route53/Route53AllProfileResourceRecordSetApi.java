package denominator.route53;

import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.peekingIterator;
import static com.google.common.collect.Iterators.tryFind;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.qualifierEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static denominator.model.ResourceRecordSets.withoutProfile;
import static denominator.route53.Route53.ActionOnResourceRecordSet.create;
import static denominator.route53.Route53.ActionOnResourceRecordSet.delete;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.PeekingIterator;

import denominator.AllProfileResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.route53.Route53.ActionOnResourceRecordSet;
import denominator.route53.Route53.ResourceRecordSetList;
import denominator.route53.Route53.ResourceRecordSetList.NextRecord;

public final class Route53AllProfileResourceRecordSetApi implements AllProfileResourceRecordSetApi {

    private final Route53 api;
    private final String zoneId;

    Route53AllProfileResourceRecordSetApi(Route53 api, String zoneId) {
        this.api = api;
        this.zoneId = zoneId;
    }

    /**
     * lists and lazily transforms all record sets who are not aliases into
     * denominator format.
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return lazyIterateRRSets(api.rrsets(zoneId), NotAlias.INSTANCE);
    }

    /**
     * lists and lazily transforms all record sets for a name which are not
     * aliases into denominator format.
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        Predicate<ResourceRecordSet<?>> filter = and(nameEqualTo(name), NotAlias.INSTANCE);
        return lazyIterateRRSets(api.rrsetsStartingAtName(zoneId, name), filter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        Predicate<ResourceRecordSet<?>> filter = and(nameEqualTo(name), typeEqualTo(type), NotAlias.INSTANCE);
        return lazyIterateRRSets(api.rrsetsStartingAtNameAndType(zoneId, name, type), filter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        Predicate<ResourceRecordSet<?>> filter = and(nameEqualTo(name), typeEqualTo(type), qualifierEqualTo(qualifier),
                NotAlias.INSTANCE);
        ResourceRecordSetList first = api.rrsetsStartingAtNameTypeAndIdentifier(zoneId, name, type, qualifier);
        Iterator<ResourceRecordSet<?>> iterator = lazyIterateRRSets(first, filter);
        return Optional.<ResourceRecordSet<?>> fromNullable(iterator.hasNext() ? iterator.next() : null);
    }

    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        return tryFind(iterateByNameAndType(name, type), withoutProfile());
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        Builder<ActionOnResourceRecordSet> changes = ImmutableList.<ActionOnResourceRecordSet> builder();
        Optional<ResourceRecordSet<?>> oldRRS;
        if (rrset.qualifier().isPresent()) {
            oldRRS = getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset.qualifier().get());
        } else {
            oldRRS = getByNameAndType(rrset.name(), rrset.type());
        }
        if (oldRRS.isPresent()) {
            if (oldRRS.get().ttl().equals(rrset.ttl()) && oldRRS.get().equals(rrset))
                return;
            changes.add(delete(oldRRS.get()));
        }
        changes.add(create(rrset));
        api.changeBatch(zoneId, changes.build());
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
        Optional<ResourceRecordSet<?>> oldRRS = getByNameTypeAndQualifier(name, type, qualifier);
        if (!oldRRS.isPresent())
            return;
        api.changeBatch(zoneId, ImmutableList.of(delete(oldRRS.get())));
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        Builder<ActionOnResourceRecordSet> changes = ImmutableList.<ActionOnResourceRecordSet> builder();
        for (Iterator<ResourceRecordSet<?>> it = iterateByNameAndType(name, type); it.hasNext();) {
            changes.add(delete(it.next()));
        }
        api.changeBatch(zoneId, changes.build());
    }

    static final class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

        private final Route53 api;

        @Inject
        Factory(Route53 api) {
            this.api = api;
        }

        @Override
        public Route53AllProfileResourceRecordSetApi create(String id) {
            return new Route53AllProfileResourceRecordSetApi(api, id);
        }
    }

    private static enum NotAlias implements Predicate<ResourceRecordSet<?>> {
        INSTANCE;

        @Override
        public boolean apply(ResourceRecordSet<?> input) {
            if (input == null)
                return false;
            if (input.profiles().isEmpty())
                return true;
            for (Map<String, Object> profile : input.profiles()) {
                if ("alias".equals(profile.get("type")))
                    return false;
            }
            return true;
        }
    }

    Iterator<ResourceRecordSet<?>> lazyIterateRRSets(final ResourceRecordSetList first,
            final Predicate<ResourceRecordSet<?>> filter) {
        if (first.next == null)
            return filter(first.iterator(), filter);
        return new Iterator<ResourceRecordSet<?>>() {
            PeekingIterator<ResourceRecordSet<?>> current = peekingIterator(first.iterator());
            NextRecord next = first.next;

            @Override
            public boolean hasNext() {
                while (!current.hasNext() && next != null) {
                    ResourceRecordSetList nextPage;
                    if (next.identifier != null) {
                        nextPage = api.rrsetsStartingAtNameTypeAndIdentifier(zoneId, next.name, next.type,
                                next.identifier);
                    } else {
                        nextPage = api.rrsetsStartingAtNameAndType(zoneId, next.name, next.type);
                    }
                    current = peekingIterator(nextPage.iterator());
                    next = nextPage.next;
                }
                return current.hasNext() && filter.apply(current.peek());
            }

            @Override
            public ResourceRecordSet<?> next() {
                return current.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}