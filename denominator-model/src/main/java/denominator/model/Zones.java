package denominator.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Static utility methods pertaining to {@code Zone} instances.
 * 
 * @since 1.2
 */
public class Zones {

    private Zones() {
    }

    /**
     * Returns a function that calls {@link Zone#create(String)} on its argument. The
     * function does not accept nulls; it will throw a
     * {@link NullPointerException} when applied to {@code null}.
     */
    public static Function<String, Zone> toZone() {
        return ToZoneFunction.INSTANCE;
    }

    // enum singleton pattern
    private static enum ToZoneFunction implements Function<String, Zone> {
        INSTANCE;

        @Override
        public Zone apply(String o) {
            return Zone.create(o);
        }

        @Override
        public String toString() {
            return "toZone";
        }
    }

    /**
     * Returns a function that calls {@code name()} on its argument. The
     * function does not accept nulls; it will throw a
     * {@link NullPointerException} when applied to {@code null}.
     */
    public static Function<Zone, String> toName() {
        return ToNameFunction.INSTANCE;
    }

    // enum singleton pattern
    private static enum ToNameFunction implements Function<Zone, String> {
        INSTANCE;

        @Override
        public String apply(Zone o) {
            checkNotNull(o, "zone");
            return o.name();
        }

        @Override
        public String toString() {
            return "toName";
        }
    }

    /**
     * evaluates to true if the input {@link Zone} exists with
     * {@link Zone#name() name} corresponding to the {@code name} parameter.
     * 
     * @param name
     *            the {@link Zone#name() name} of the desired zone
     */
    public static Predicate<Zone> nameEqualTo(String name) {
        return new NameEqualToPredicate(name);
    }

    private static final class NameEqualToPredicate implements Predicate<Zone> {
        private final String name;

        public NameEqualToPredicate(String name) {
            this.name = checkNotNull(name, "name");
        }

        @Override
        public boolean apply(Zone input) {
            if (input == null)
                return false;
            return name.equals(input.name());
        }

        @Override
        public String toString() {
            return "NameEqualTo(" + name + ")";
        }
    }

    /**
     * evaluates to true if the input {@link Zone} exists with
     * {@link Zone#id() id} present and corresponding to the {@code id}
     * parameter.
     * 
     * @param id
     *            the {@link Zone#id() id} of the desired zone
     */
    public static Predicate<Zone> idEqualTo(String id) {
        return new IdEqualToPredicate(id);
    }

    private static final class IdEqualToPredicate implements Predicate<Zone> {
        private final String id;

        public IdEqualToPredicate(String id) {
            this.id = checkNotNull(id, "id");
        }

        @Override
        public boolean apply(Zone input) {
            if (input == null)
                return false;
            return id.equals(input.id().orNull());
        }

        @Override
        public String toString() {
            return "IdEqualTo(" + id + ")";
        }
    }
}
