package denominator;

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
    Set<String> result = new LinkedHashSet<String>();
    result.addAll(asList("A", "AAAA", "CERT", "CNAME", "MX", "NAPTR", "NS", "PTR", "SOA", "SPF",
                         "SRV", "SSHFP", "TXT"));
    return result;
  }

  @Override
  public Map<String, Collection<String>> profileToRecordTypes() {
    Map<String, Collection<String>> result = new LinkedHashMap<String, Collection<String>>();
    List<String> roundRobin = asList("A", "AAAA", "MX", "NS", "PTR", "SPF", "SRV", "SSHFP", "TXT");
    result.put("roundRobin", roundRobin);
    return result;
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
  public boolean equals(Object obj) {
    if (obj instanceof Provider) {
      Provider other = (Provider) obj;
      return name().equals(other.name())
             && url().equals(other.url());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + name().hashCode();
    result = 31 * result + url().hashCode();
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Provider [");
    builder.append("name=").append(name());
    builder.append("url=").append(url());
    builder.append("]");
    return builder.toString();
  }
}
