package denominator.route53;

import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterators.peekingIterator;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;

import java.util.Iterator;

import com.google.common.collect.PeekingIterator;

import denominator.model.ResourceRecordSet;

/**
 * used when there are server-side groups, such as weight or geo, which cause
 * record sets to not be unique solely on name and type.
 */
class GroupByRecordNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {
    private final PeekingIterator<ResourceRecordSet<?>> peekingIterator;

    public GroupByRecordNameAndTypeIterator(Iterator<ResourceRecordSet<?>> sortedIterator) {
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    public boolean hasNext() {
        return peekingIterator.hasNext();
    }

    @Override
    public ResourceRecordSet<?> next() {
        ResourceRecordSet<?> rrset = peekingIterator.next();
        while (hasNext() && and(nameEqualTo(rrset.name()), typeEqualTo(rrset.type())).apply(peekingIterator.peek())) {
            ResourceRecordSet<?> next = peekingIterator.next();
            rrset = ResourceRecordSet.builder()
                                     .name(rrset.name())
                                     .type(rrset.type())
                                     .ttl(rrset.ttl().or(next.ttl()).orNull())
                                     .addAll(rrset)
                                     .addAll(next)
                                     .build();
        }
        return rrset;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}