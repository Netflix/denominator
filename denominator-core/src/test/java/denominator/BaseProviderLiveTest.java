package denominator;

import static com.google.common.collect.Iterators.size;
import static com.google.common.io.Closeables.close;
import static java.lang.String.format;
import static java.util.logging.Logger.getAnonymousLogger;

import java.io.IOException;

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
        int zoneCount = size(manager.getApi().getZoneApi().list());
        getAnonymousLogger().info(format("%s has %s zones", manager, zoneCount));
    }

    protected void skipIfNoCredentials() {
        if (manager == null)
            throw new SkipException("manager not configured");
    }

    @AfterClass
    private void tearDown() throws IOException {
       close(manager, true);
    }
}
