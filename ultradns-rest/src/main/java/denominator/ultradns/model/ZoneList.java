package denominator.ultradns.model;

import java.util.ArrayList;
import java.util.List;

public class ZoneList {

    private List<Zone> zones;

    public List<Zone> getZones() {
        return zones;
    }

    public List<String> getZoneNames() {
        List<String> zoneNames = new ArrayList<String>();
        for(Zone zone : getZones()){
            zoneNames.add(zone.getProperties().getName());
        }
        return zoneNames;
    }

    private class Zone {

        private ZoneProperties properties;

        public ZoneProperties getProperties() {
            return properties;
        }
    }

    private class ZoneProperties {

        private String name;

        public String getName() {
            return name;
        }
    }

}
