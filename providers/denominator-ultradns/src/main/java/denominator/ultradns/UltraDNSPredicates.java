package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import com.google.common.base.Predicate;

import denominator.ultradns.UltraDNS.Record;

final class UltraDNSPredicates {
    private UltraDNSPredicates() {
    }

    public static Predicate<Record> resourceTypeEqualTo(int typeValue) {
        return new ResourceTypeEqualToPredicate(typeValue);
    }

    /** @see UltraDNSPredicates#resourceTypeEqualTo(int) */
    private static class ResourceTypeEqualToPredicate implements Predicate<Record>, Serializable {
        private final int typeValue;

        private ResourceTypeEqualToPredicate(int typeValue) {
            this.typeValue = checkNotNull(typeValue, "typeValue");
        }

        @Override
        public boolean apply(Record in) {
            return typeValue == in.typeCode;
        }

        @Override
        public String toString() {
            return "ResourceTypeEqualTo(" + typeValue + ")";
        }

        private static final long serialVersionUID = 0;
    }

    public static Predicate<Record> recordIdEqualTo(String id) {
        return new RecordIdEqualToPredicate(id);
    }

    private static class RecordIdEqualToPredicate implements Predicate<Record>, Serializable {
        private final String id;

        private RecordIdEqualToPredicate(String id) {
            this.id = checkNotNull(id, "id");
        }

        @Override
        public boolean apply(Record in) {
            return id.equals(in.id);
        }

        @Override
        public String toString() {
            return "RecordIdEqualTo(" + id + ")";
        }

        private static final long serialVersionUID = 0;
    }
}
