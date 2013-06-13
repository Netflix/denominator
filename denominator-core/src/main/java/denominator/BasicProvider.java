package denominator;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;

import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

/**
 * base implementation of {@link Provider}, which sets defaults and properly
 * implements {@code equals} and {@code hashCode}.
 */
@Beta
public abstract class BasicProvider implements Provider {

    // protected to ensure subclassed
    protected BasicProvider() {
        checkLowerCamel(credentialTypeToParameterNames());
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

    @Deprecated
    @Override
    public String getName() {
        return name();
    }

    @Override
    public String name() {
        return getClass().getSimpleName().toLowerCase().replace("provider", "");
    }

    @Deprecated
    @Override
    public String getUrl() {
        return url();
    }

    @Override
    public String url() {
        return "mem:" + name();
    }

    @Override
    public Set<String> basicRecordTypes() {
        return ImmutableSet.of("A", "AAAA", "CNAME", "MX", "NS", "PTR", "SOA", "SPF", "SRV", "SSHFP", "TXT");
    }

    @Override
    public SetMultimap<String, String> profileToRecordTypes() {
        return ImmutableSetMultimap.<String, String> builder()
                .putAll("roundRobin", filter(basicRecordTypes(), not(in(ImmutableSet.of("SOA", "CNAME"))))).build();
    }

    @Override
    public boolean supportsDuplicateZoneNames() {
        return false;
    }

    @Deprecated
    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return credentialTypeToParameterNames();
    }

    @Override
    public Multimap<String, String> credentialTypeToParameterNames() {
        return ImmutableMultimap.of();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name(), url());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        return equal(this.name(), BasicProvider.class.cast(obj).name())
                && equal(this.url(), BasicProvider.class.cast(obj).url());
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", name()).add("url", url()).toString();
    }
}
