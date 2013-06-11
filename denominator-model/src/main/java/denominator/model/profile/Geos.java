package denominator.model.profile;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;

/**
 * Static utility methods that build {@code Geo} instances.
 * 
 * @deprecated Will be removed in denominator 2.0. Please use
 *             {@link denominator.model.ResourceRecordSets}
 */
@Deprecated
public class Geos {

    private Geos() {
    }

    /**
     * returns true if {@link Geo#group() group}, if equals the
     * input {@code group};
     * 
     * @param group
     *            expected name of the group. ex. {@code US-East}
     * 
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link denominator.model.ResourceRecordSets#qualifierEqualTo(String)}
     */
    @Deprecated
    public static Predicate<Geo> groupEqualTo(String group) {
        return new GroupEqualToPredicate(group);
    }

    @Deprecated
    private static final class GroupEqualToPredicate implements Predicate<Geo> {
        private final String group;

        private GroupEqualToPredicate(String group) {
            this.group = checkNotNull(group, "group");
        }

        @Override
        public boolean apply(Geo input) {
            if (input == null)
                return false;
            return group.equals(input.group());
        }

        @Override
        public String toString() {
            return "GroupEqualTo(" + group + ")";
        }
    }
}
