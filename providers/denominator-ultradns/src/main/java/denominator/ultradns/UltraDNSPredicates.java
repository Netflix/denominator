package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import org.jclouds.ultradns.ws.domain.ResourceRecord;
import org.jclouds.ultradns.ws.domain.ResourceRecordMetadata;
import org.jclouds.ultradns.ws.domain.RoundRobinPool;

import com.google.common.base.Predicate;

final class UltraDNSPredicates {
    private UltraDNSPredicates() { /* */
    }

    public static Predicate<ResourceRecord> resourceTypeEqualTo(int typeValue) {
        return new ResourceTypeEqualToPredicate(typeValue);
    }

    /** @see UltraDNSPredicates#resourceTypeEqualTo(int) */
    private static class ResourceTypeEqualToPredicate implements Predicate<ResourceRecord>, Serializable {
        private final int typeValue;

        private ResourceTypeEqualToPredicate(int typeValue) {
            this.typeValue = checkNotNull(typeValue, "typeValue");
        }

        @Override
        public boolean apply(ResourceRecord in) {
            return typeValue == in.getType();
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

    /** @see UltraDNSPredicates#resourceTypeEqualTo(int) */
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

    public static Predicate<RoundRobinPool> poolDNameEqualTo(String dname) {
        return new PoolDNameEqualToPredicate(dname);
    }

    /** @see UltraDNSPredicates#poolDNameEqualTo(String) */
    private static class PoolDNameEqualToPredicate implements Predicate<RoundRobinPool>, Serializable {
        private final String dname;

        private PoolDNameEqualToPredicate(String dname) {
            this.dname = checkNotNull(dname, "dname");
        }

        @Override
        public boolean apply(RoundRobinPool in) {
            return dname.equals(in.getDName());
        }

        @Override
        public String toString() {
            return "PoolDNameEqualTo(" + dname + ")";
        }

        private static final long serialVersionUID = 0;
    }

    public static Predicate<RoundRobinPool> poolNameEqualTo(String name) {
        return new PoolNameEqualToPredicate(name);
    }

    /** @see UltraDNSPredicates#poolNameEqualTo(String) */
    private static class PoolNameEqualToPredicate implements Predicate<RoundRobinPool>, Serializable {
        private final String name;

        private PoolNameEqualToPredicate(String name) {
            this.name = checkNotNull(name, "name");
        }

        @Override
        public boolean apply(RoundRobinPool in) {
            return name.equals(in.getName());
        }

        @Override
        public String toString() {
            return "PoolNameEqualTo(" + name + ")";
        }

        private static final long serialVersionUID = 0;
    }
}
