package denominator;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * base implementation of {@link Provider}, which sets defaults and properly
 * implements {@code equals} and {@code hashCode}.
 */
@Beta
public abstract class BasicProvider implements Provider {

    // protected to ensure subclassed
    protected BasicProvider() {
        checkLowerCamel(getCredentialTypeToParameterNames());
    }

    /**
     * if we choose to support numbers, this will need to be updated
     */
    private static Pattern lowerCamel = Pattern.compile("^[a-z]+([A-Z][a-z]+)*$");

    private void checkLowerCamel(Multimap<String, String> credentialTypeToParameterNames) {
        for (Entry<String, String> entry : credentialTypeToParameterNames.entries()) {
            checkArgument(lowerCamel.matcher(entry.getKey()).matches(),
                    "please correct credential type %s to lowerCamel case", entry.getKey());
            checkArgument(lowerCamel.matcher(entry.getValue()).matches(),
                    "please correct %s credential parameter %s to lowerCamel case", entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName().toLowerCase().replace("provider", "");
    }

    @Override
    public String getUrl() {
        return "mem:" + getName();
    }

    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.of();
    }

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
        return equal(this.getName(), BasicProvider.class.cast(obj).getName())
                && equal(this.getUrl(), BasicProvider.class.cast(obj).getUrl());
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", getName()).add("url", getUrl()).toString();
    }
}
