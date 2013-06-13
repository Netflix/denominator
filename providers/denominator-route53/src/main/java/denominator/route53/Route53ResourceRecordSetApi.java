package denominator.route53;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static denominator.route53.ToDenominatorResourceRecordSet.isAlias;
import static denominator.route53.ToDenominatorResourceRecordSet.isSubset;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.route53.Route53Api;
import org.jclouds.route53.domain.ChangeBatch;
import org.jclouds.route53.domain.ResourceRecordSetIterable.NextRecord;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

final class Route53ResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final org.jclouds.route53.features.ResourceRecordSetApi route53RRsetApi;

    Route53ResourceRecordSetApi(org.jclouds.route53.features.ResourceRecordSetApi route53RRsetApi) {
        this.route53RRsetApi = route53RRsetApi;
    }

    /**
     * lists and lazily transforms all record sets who are not aliases into
     * denominator format.
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return route53RRsetApi.list().concat().filter(not(or(isAlias(), isSubset())))
                .transform(ToDenominatorResourceRecordSet.INSTANCE).iterator();
    }

    /**
     * lists and lazily transforms all record sets for a name which are not
     * aliases into denominator format.
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return route53RRsetApi.listAt(NextRecord.name(name))
                .filter(and(not(or(isAlias(), isSubset())), nameEqualTo(name)))
                .transform(ToDenominatorResourceRecordSet.INSTANCE).iterator();
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        return filterRoute53RRSByNameAndType(name, type).transform(ToDenominatorResourceRecordSet.INSTANCE).first();
    }

    /**
     * for efficiency, starts the list at the specified {@code name} and
     * {@code type}.
     */
    @SuppressWarnings("unchecked")
    FluentIterable<org.jclouds.route53.domain.ResourceRecordSet> filterRoute53RRSByNameAndType(String name, String type) {
        return route53RRsetApi.listAt(NextRecord.nameAndType(name, type)).filter(
                and(not(or(isAlias(), isSubset())), nameEqualTo(name), typeEqualTo(type)));
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        ChangeBatch.Builder changes = ChangeBatch.builder();

        org.jclouds.route53.domain.ResourceRecordSet replacement = ToRoute53ResourceRecordSet.INSTANCE.apply(rrset);

        Optional<org.jclouds.route53.domain.ResourceRecordSet> oldRRS = filterRoute53RRSByNameAndType(rrset.name(),
                rrset.type()).first();
        if (oldRRS.isPresent()) {
            if (oldRRS.get().getTTL().equals(replacement.getTTL())
                    && oldRRS.get().getValues().equals(replacement.getValues()))
                return;
            changes.delete(oldRRS.get());
        }

        changes.create(replacement);

        route53RRsetApi.apply(changes.build());
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        Optional<org.jclouds.route53.domain.ResourceRecordSet> oldRRS = filterRoute53RRSByNameAndType(name, type)
                .first();
        if (!oldRRS.isPresent())
            return;
        route53RRsetApi.delete(oldRRS.get());
    }

    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final Route53Api api;

        @Inject
        Factory(Route53Api api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(String id) {
            return new Route53ResourceRecordSetApi(api.getResourceRecordSetApiForHostedZone(id));
        }
    }

    static Predicate<org.jclouds.route53.domain.ResourceRecordSet> nameEqualTo(String name) {
        return new Route53NameEqualToPredicate(name);
    }

    private static class Route53NameEqualToPredicate implements Predicate<org.jclouds.route53.domain.ResourceRecordSet> {
        private final String name;

        private Route53NameEqualToPredicate(String name) {
            this.name = checkNotNull(name, "name");
        }

        @Override
        public boolean apply(org.jclouds.route53.domain.ResourceRecordSet input) {
            return name.equals(input.getName());
        }

        @Override
        public String toString() {
            return "NameEqualTo(" + name + ")";
        }
    }

    static Predicate<org.jclouds.route53.domain.ResourceRecordSet> typeEqualTo(String type) {
        return new Route53TypeEqualToPredicate(type);
    }

    private static class Route53TypeEqualToPredicate implements Predicate<org.jclouds.route53.domain.ResourceRecordSet> {
        private final String type;

        private Route53TypeEqualToPredicate(String type) {
            this.type = checkNotNull(type, "type");
        }

        @Override
        public boolean apply(org.jclouds.route53.domain.ResourceRecordSet input) {
            return type.equals(input.getType());
        }

        @Override
        public String toString() {
            return "TypeEqualTo(" + type + ")";
        }
    }
}