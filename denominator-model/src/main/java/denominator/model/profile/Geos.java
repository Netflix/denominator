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
     * returns true if {@link Geo#getName() group name}, if equals the
     * input {@code groupName};
     * 
     * @param groupName
     *            expected name of the group. ex. {@code US-East}
     */
    public static Predicate<Geo> nameEqualTo(String groupName) {
        return new GroupNameEqualToPredicate(groupName);
    }

    private static final class GroupNameEqualToPredicate implements Predicate<Geo> {
        private final String groupName;

        private GroupNameEqualToPredicate(String groupName) {
            this.groupName = checkNotNull(groupName, "groupName");
        }

        @Override
        public boolean apply(Geo input) {
            if (input == null)
                return false;
            return groupName.equals(input.getName());
        }

        @Override
        public String toString() {
            return "GroupNameEqualTo(" + groupName + ")";
        }
    }
}
