package denominator.cli;

import static dagger.Provides.Type.SET;
import dagger.Module;
import dagger.Provides;
import denominator.cli.groups.ProviderGroup;
import denominator.cli.groups.ResourceRecordSetGroup;
import denominator.cli.groups.VersionGroup;
import denominator.cli.groups.ZoneGroup;

@Module(complete = false, injects = GroupHandler.class)
public class SupportedGroups {
    @Provides(type = SET)
    Group version() {
        return new VersionGroup();
    }

    @Provides(type = SET)
    Group provider() {
        return new ProviderGroup();
    }

    @Provides(type = SET)
    Group zone() {
        return new ZoneGroup();
    }

    @Provides(type = SET)
    Group rrset() {
        return new ResourceRecordSetGroup();
    }
}
