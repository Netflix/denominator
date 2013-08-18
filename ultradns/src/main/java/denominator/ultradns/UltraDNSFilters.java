package denominator.ultradns;

import static denominator.common.Preconditions.checkNotNull;

import java.io.Serializable;

import denominator.common.Filter;
import denominator.ultradns.UltraDNS.Record;

final class UltraDNSFilters {
    private UltraDNSFilters() {
    }

    public static Filter<Record> resourceTypeEqualTo(int typeValue) {
        return new ResourceTypeEqualToPredicate(typeValue);
    }

    /** @see UltraDNSFilters#resourceTypeEqualTo(int) */
    private static class ResourceTypeEqualToPredicate implements Filter<Record>, Serializable {
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

    public static Filter<Record> recordIdEqualTo(String id) {
        return new RecordIdEqualToPredicate(id);
    }

    private static class RecordIdEqualToPredicate implements Filter<Record>, Serializable {
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
