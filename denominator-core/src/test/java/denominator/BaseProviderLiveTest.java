package denominator;

import static com.google.common.io.Closeables.closeQuietly;
import static java.lang.String.format;
import static java.util.logging.Logger.getAnonymousLogger;

import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseProviderLiveTest {

    protected DNSApiManager manager;

    @Test
    public void testListZones() {
        skipIfNoCredentials();
        List<String> zoneNames = manager.getApi().getZoneApi().list().toList();
        getAnonymousLogger().info(format("%s has %s zones", manager, zoneNames.size()));
    }

    protected void skipIfNoCredentials() {
        if (manager == null)
            throw new SkipException("manager not configured");
    }

    @AfterClass
    private void tearDown() {
        closeQuietly(manager);
    }
}
