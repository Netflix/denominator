package denominator;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static denominator.common.Preconditions.checkArgument;
import static java.util.Arrays.asList;

/**
 * base implementation of {@link Provider}, which sets defaults and properly implements {@code
 * equals} and {@code hashCode}.
 */
public abstract class BasicProvider implements Provider {

  /**
   * if we choose to support numbers, this will need to be updated
   */
  private static Pattern lowerCamel = Pattern.compile("^[a-z0-9]+([A-Z][a-z]+)*$");

  // protected to ensure subclassed
  protected BasicProvider() {
    checkLowerCamel(credentialTypeToParameterNames());
  }

  private void checkLowerCamel(Map<String, Collection<String>> credentialTypeToParameterNames) {
    for (String credentialType : credentialTypeToParameterNames.keySet()) {
      for (String credentialParam : credentialTypeToParameterNames.get(credentialType)) {
        checkArgument(lowerCamel.matcher(credentialType).matches(),
                      "please correct credential type %s to lowerCamel case", credentialType);
        checkArgument(lowerCamel.matcher(credentialParam).matches(),
                      "please correct %s credential parameter %s to lowerCamel case",
                      credentialType, credentialParam);
      }
    }
  }

  @Override
  public String name() {
    return getClass().getSimpleName().toLowerCase().replace("provider", "");
  }

  @Override
  public String url() {
    return "mem:" + name();
  }

  @Override
  public Set<String> basicRecordTypes() {
    Set<String> types = new LinkedHashSet<String>();
    types.addAll(
        asList("A", "AAAA", "CERT", "CNAME", "MX", "NAPTR", "NS", "PTR", "SOA", "SPF", "SRV",
               "SSHFP", "TXT"));
    return types;
  }

  @Override
  public Map<String, Collection<String>> profileToRecordTypes() {
    Map<String, Collection<String>>
        profileToRecordTypes =
        new LinkedHashMap<String, Collection<String>>();
    List<String>
        roundRobinType =
        asList("A", "AAAA", "MX", "NS", "PTR", "SPF", "SRV", "SSHFP", "TXT");
    profileToRecordTypes.put("roundRobin", roundRobinType);
    return profileToRecordTypes;
  }

  @Override
  public boolean supportsDuplicateZoneNames() {
    return false;
  }

  @Override
  public Map<String, Collection<String>> credentialTypeToParameterNames() {
    return new LinkedHashMap<String, Collection<String>>();
  }

  @Override
  public int hashCode() {
    return 37 * name().hashCode() + url().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.name().equals(Provider.class.cast(obj).name()) && this.url()
        .equals(Provider.class.cast(obj).url());
  }

  @Override
  public String toString() {
    return new StringBuilder().append(getClass().getSimpleName()).append('{').append("name=")
        .append(name())
        .append(',').append("url=").append(url()).append('}').toString();
  }
}
