package denominator.cli.codec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.yaml.snakeyaml.Dumper;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import dagger.Module;
import dagger.Provides;
import denominator.Provider;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

@Module(library = true)
public class YamlCodec {

    @Provides
    @Named("COLUMNS")
    int columns(){
        String columns = System.getenv("COLUMNS");
        return columns == null ? 80 : Integer.parseInt(columns);
    }

    @Provides
    @Singleton
    Yaml yaml(DenominatorRepresenter representer, @Named("COLUMNS") int columns) {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setWidth(columns);
        return new Yaml(new Dumper(representer, dumperOptions));
    }

    static class DenominatorRepresenter extends Representer {

        @Inject
        DenominatorRepresenter() {
            multiRepresenters.put(Provider.class, new Represent() {

                @Override
                public Node representData(Object data) {
                    Provider provider = Provider.class.cast(data);
                    Map<String, Object> providerAsMap = new LinkedHashMap<String, Object>();
                    providerAsMap.put("name", provider.name());
                    providerAsMap.put("url", provider.url());
                    providerAsMap.put("duplicateZones", provider.supportsDuplicateZoneNames());
                    if (!provider.credentialTypeToParameterNames().isEmpty())
                        providerAsMap.put("credentialTypes", provider.credentialTypeToParameterNames());
                    return DenominatorRepresenter.this.representData(providerAsMap);
                }

            });

            multiRepresenters.put(Zone.class, new Represent() {

                @Override
                public Node representData(Object data) {
                    Zone zone = Zone.class.cast(data);
                    Map<String, String> zoneAsMap = new LinkedHashMap<String, String>();
                    zoneAsMap.put("name", zone.name());
                    if (zone.id() != null)
                        zoneAsMap.put("id", zone.id());
                    return DenominatorRepresenter.this.representData(zoneAsMap);
                }

            });

            multiRepresenters.put(ResourceRecordSet.class, new Represent() {

                @Override
                public Node representData(Object data) {
                    ResourceRecordSet<?> rrset = ResourceRecordSet.class.cast(data);
                    Map<String, Object> rrsetAsMap = new LinkedHashMap<String, Object>();
                    rrsetAsMap.put("name", rrset.name());
                    rrsetAsMap.put("type", rrset.type());
                    if (rrset.qualifier() != null)
                        rrsetAsMap.put("qualifier", rrset.qualifier());
                    if (rrset.ttl() != null)
                        rrsetAsMap.put("ttl", rrset.ttl());
                    if (!rrset.profiles().isEmpty())
                        rrsetAsMap.put("profiles", rrset.profiles());
                    if (!rrset.records().isEmpty()) {
                        // compact
                        List<String> rdataList = new ArrayList<String>(rrset.records().size());
                        for (Map<String, Object> rdata : rrset.records()) {
                            rdataList.add(Util.join(' ', rdata.values().toArray()));
                        }
                        rrsetAsMap.put("records", rdataList);
                    }
                    return DenominatorRepresenter.this.representData(rrsetAsMap);
                }

            });
        }
    }
}
