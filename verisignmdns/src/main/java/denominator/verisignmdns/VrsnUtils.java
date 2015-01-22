package denominator.verisignmdns;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import denominator.Credentials;

class VrsnUtils {

    /**
     * Converts Provider<Credentials> object into Map<String,String> ie
     * credential name, credential value.
     */
    static Map<String, String> getMapOfCredentials(Provider<Credentials> credentials) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (credentials != null) {
            Credentials currentCreds = credentials.get();
            if (currentCreds instanceof List && ((List) currentCreds).size() >= 2) {
                List tempCredList = List.class.cast(currentCreds);
                result.put("username", (String) tempCredList.get(0));
                result.put("password", (String) tempCredList.get(1));
            } else if (currentCreds instanceof Map) {
                // it must be map
                result.putAll(Map.class.cast(currentCreds));
            }
        }
        return result;
    }

    static String getUsername(Provider<Credentials> credentials) {
        return getMapOfCredentials(credentials).get("username");
    }

    static String getPassword(Provider<Credentials> credentials) {
        return getMapOfCredentials(credentials).get("password");
    }
}
