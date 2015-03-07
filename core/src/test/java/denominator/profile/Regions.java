package denominator.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Regions {

  private final Map<String, Collection<String>> all;
  private final String lastRegion;
  private final String lastTerritory;
  private final List<String> exceptLastTerritory;

  Regions(Map<String, Collection<String>> input) {
    all = input;
    String[] names = input.keySet().toArray(new String[input.size()]);
    lastRegion = names[names.length - 1];
    exceptLastTerritory = new ArrayList<String>(input.get(lastRegion));
    lastTerritory = exceptLastTerritory.remove(exceptLastTerritory.size() - 1);
  }

  Map<String, Collection<String>> allButLastTerritory() {
    Map<String, Collection<String>> result = new LinkedHashMap<String, Collection<String>>(all);
    if (exceptLastTerritory.isEmpty()) {
      result.remove(lastRegion);
    } else {
      result.put(lastRegion, exceptLastTerritory);
    }
    return result;
  }

  Map<String, Collection<String>> onlyLastTerritory() {
    Map<String, Collection<String>> result = new LinkedHashMap<String, Collection<String>>(1);
    result.put(lastRegion, Arrays.asList(lastTerritory));
    return result;
  }

  Map<String, Collection<String>> plusLastTerritory(Map<String, Collection<String>> input) {
    Map<String, Collection<String>> result = new LinkedHashMap<String, Collection<String>>(input);
    if (result.containsKey(lastRegion)) {
      List<String> moreTerritories = new ArrayList<String>(result.get(lastRegion));
      moreTerritories.add(lastTerritory);
      result.put(lastRegion, moreTerritories);
    } else {
      result.put(lastRegion, Arrays.asList(lastTerritory));
    }
    return result;
  }
}
