package denominator.route53;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterators.emptyIterator;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.route53.Route53ResourceRecordSetApi.nameEqualTo;
import static denominator.route53.Route53ResourceRecordSetApi.typeEqualTo;
import static denominator.route53.ToDenominatorResourceRecordSet.isWeighted;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.jclouds.route53.Route53Api;
import org.jclouds.route53.domain.ChangeBatch;
import org.jclouds.route53.domain.ResourceRecordSet.RecordSubset;
import org.jclouds.route53.domain.ResourceRecordSet.RecordSubset.Weighted;
import org.jclouds.route53.domain.ResourceRecordSetIterable.NextRecord;
import org.jclouds.route53.features.ResourceRecordSetApi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import denominator.Provider;
import denominator.model.ResourceRecordSet;
import denominator.profile.WeightedResourceRecordSetApi;

final class Route53WeightedResourceRecordSetApi implements WeightedResourceRecordSetApi {
    private static final Predicate<ResourceRecordSet<?>> IS_WEIGHTED =
            profileContainsType(denominator.model.profile.Weighted.class);

    private final ResourceRecordSetApi route53RRsetApi;
    private final Set<String> supportedTypes;
    private final SortedSet<Integer> supportedWeights;

    Route53WeightedResourceRecordSetApi(ResourceRecordSetApi route53RRsetApi, Set<String> supportedTypes, SortedSet<Integer> supportedWeights) {
        this.route53RRsetApi = route53RRsetApi;
        this.supportedTypes = supportedTypes;
        this.supportedWeights = supportedWeights;
    }

    @Override
    public SortedSet<Integer> supportedWeights() {
        return supportedWeights;
    }

    @Deprecated
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return route53RRsetApi.list().concat().filter(isWeighted()).transform(ToDenominatorResourceRecordSet.INSTANCE)
                .iterator();
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        return iterateByName(name);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return route53RRsetApi.listAt(NextRecord.name(name)).filter(and(isWeighted(), nameEqualTo(name)))
                .transform(ToDenominatorResourceRecordSet.INSTANCE).iterator();
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
        return iterateByNameAndType(name, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        if (!supportedTypes.contains(type)){
            return emptyIterator();
        }
        return route53RRsetApi.listAt(NextRecord.nameAndType(name, type))
                .filter(and(isWeighted(), nameEqualTo(name), typeEqualTo(type)))
                .transform(ToDenominatorResourceRecordSet.INSTANCE).iterator();
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(qualifier, "qualifier");
        if (!supportedTypes.contains(type)){
            return Optional.absent();
        }
        return filterRoute53RRSByNameTypeAndId(name, type, qualifier)
                .transform(ToDenominatorResourceRecordSet.INSTANCE).first();
    }

    @SuppressWarnings("unchecked")
    FluentIterable<RecordSubset.Weighted> filterRoute53RRSByNameTypeAndId(String name, String type, String id) {
        return route53RRsetApi.listAt(NextRecord.nameTypeAndIdentifier(name, type, id))
                .filter(and(isWeighted(), nameEqualTo(name), typeEqualTo(type), idEqualTo(id)))
                .transform(new Function<org.jclouds.route53.domain.ResourceRecordSet, RecordSubset.Weighted>() {

                    @Override
                    public RecordSubset.Weighted apply(org.jclouds.route53.domain.ResourceRecordSet input) {
                        return RecordSubset.Weighted.class.cast(input);
                    }

                });
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier().isPresent(), "no qualifier on: %s", rrset);
        checkArgument(IS_WEIGHTED.apply(rrset), "%s failed on: %s", IS_WEIGHTED, rrset);
        checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for geo: %s", rrset.type(), supportedTypes);

        ChangeBatch.Builder changes = ChangeBatch.builder();

        Weighted replacement = Weighted.class.cast(ToRoute53ResourceRecordSet.INSTANCE.apply(rrset));

        Optional<Weighted> oldRRS = filterRoute53RRSByNameTypeAndId(rrset.name(), rrset.type(), rrset.qualifier().get())
                .first();
        if (oldRRS.isPresent()) {
            if (oldRRS.get().getWeight() == replacement.getWeight()
                    && oldRRS.get().getTTL().equals(replacement.getTTL())
                    && oldRRS.get().getValues().equals(replacement.getValues()))
                return;
            changes.delete(oldRRS.get());
        }

        changes.create(replacement);
        route53RRsetApi.apply(changes.build());
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
        Optional<Weighted> oldRRS = filterRoute53RRSByNameTypeAndId(name, type, qualifier).first();
        if (!oldRRS.isPresent())
            return;
        route53RRsetApi.delete(oldRRS.get());
    }

    static final class Factory implements WeightedResourceRecordSetApi.Factory {

        private final Route53Api api;
        private final Set<String> supportedTypes;
        private final SortedSet<Integer> supportedWeights;

        @Inject
        Factory(Route53Api api, Provider provider, @Named("weighted") SortedSet<Integer> supportedWeights) {
            this.api = api;
            this.supportedTypes = provider.profileToRecordTypes().get("weighted");
            this.supportedWeights = supportedWeights;
        }

        @Override
        public Optional<WeightedResourceRecordSetApi> create(String id) {
            return Optional.<WeightedResourceRecordSetApi> of(new Route53WeightedResourceRecordSetApi(api
                    .getResourceRecordSetApiForHostedZone(id), supportedTypes, supportedWeights));
        }
    }

    static Predicate<org.jclouds.route53.domain.ResourceRecordSet> idEqualTo(String id) {
        return new Route53IdEqualToPredicate(id);
    }

    private static class Route53IdEqualToPredicate implements Predicate<org.jclouds.route53.domain.ResourceRecordSet> {
        private final String id;

        private Route53IdEqualToPredicate(String id) {
            this.id = checkNotNull(id, "id");
        }

        @Override
        public boolean apply(org.jclouds.route53.domain.ResourceRecordSet input) {
            if (!(input instanceof RecordSubset))
                return false;
            return id.equals(RecordSubset.class.cast(input).getId());
        }

        @Override
        public String toString() {
            return "IdEqualTo(" + id + ")";
        }
    }
}
