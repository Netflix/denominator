package denominator.model.profile;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;

/**
 * Static utility methods that build {@code Geo} instances.
 * 
 */
public class Geos {

    private Geos() {
    }

    /**
     * returns true if {@link Geo#group() group}, if equals the
     * input {@code group};
     * 
     * @param group
     *            expected name of the group. ex. {@code US-East}
     */
    public static Predicate<Geo> groupEqualTo(String group) {
        return new GroupEqualToPredicate(group);
    }

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
