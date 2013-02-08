package denominator;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.io.Closer;

import dagger.Provides;

/**
 * provides components that implement the {@link DNSApi}.
 * 
 * subclass this, and annotate with the following:
 * 
 * {@code  @Module(entryPoints = DNSApiManager.class) }
 * 
 * make sure your subclass has {@link Provides} methods for {@code String} (used
 * for toString), {@link DNSApi} and {@link Closer}
 */
@Beta
public abstract class Provider {

    /**
     * configuration key associated with this {@link DNSApi}. For example,
     * {@code hopper}
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
        return equal(this.getName(), Provider.class.cast(obj).getName());
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", getName()).toString();
    }
}