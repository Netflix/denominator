package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import org.jclouds.ultradns.ws.domain.ResourceRecord;
import org.jclouds.ultradns.ws.domain.ResourceRecordMetadata;

import com.google.common.base.Predicate;
import com.google.common.primitives.UnsignedInteger;

final class UltraDNSPredicates {
    private UltraDNSPredicates() { /* */
    }

    public static Predicate<ResourceRecord> resourceTypeEqualTo(UnsignedInteger typeValue) {
        return new ResourceTypeEqualToPredicate(typeValue);
    }

    /** @see UltraDNSPredicates#resourceTypeEqualTo(UnsignedInteger) */
    private static class ResourceTypeEqualToPredicate implements Predicate<ResourceRecord>, Serializable {
        private final UnsignedInteger typeValue;

        private ResourceTypeEqualToPredicate(UnsignedInteger typeValue) {
            this.typeValue = checkNotNull(typeValue, "typeValue");
        }

        @Override
        public boolean apply(ResourceRecord in) {
            return typeValue.equals(in.getType());
        }

        @Override
        public String toString() {
            return "ResourceTypeEqualTo(" + typeValue + ")";
        }

        private static final long serialVersionUID = 0;
    }

    public static Predicate<ResourceRecordMetadata> recordGuidEqualTo(String guid) {
        return new RecordGuidEqualToPredicate(guid);
    }

    /** @see UltraDNSPredicates#resourceTypeEqualTo(UnsignedInteger) */
    private static class RecordGuidEqualToPredicate implements Predicate<ResourceRecordMetadata>, Serializable {
        private final String guid;

        private RecordGuidEqualToPredicate(String guid) {
            this.guid = checkNotNull(guid, "guid");
        }

        @Override
        public boolean apply(ResourceRecordMetadata in) {
            return guid.equals(in.getGuid());
        }

        @Override
        public String toString() {
            return "RecordGuidEqualTo(" + guid + ")";
        }

        private static final long serialVersionUID = 0;
    }
}
