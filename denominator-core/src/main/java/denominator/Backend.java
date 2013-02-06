package denominator;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;

import com.google.common.base.Objects;

/**
 * an implementation that allows you to control edge network services including
 * DNS
 */
public abstract class Backend {
    /**
     * configuration key associated with this edge. For example, {@code hopper}
     */
    public abstract String getName();

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        return equal(this.getName(), Backend.class.cast(obj).getName());
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", getName()).toString();
    }

}