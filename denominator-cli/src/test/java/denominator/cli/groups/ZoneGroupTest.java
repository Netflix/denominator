package denominator.cli.groups;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.kohsuke.args4j.CmdLineException;
import org.testng.annotations.Test;

import denominator.cli.Denominator;

@Test
public class ZoneGroupTest {

    @Test(description = "denominator zone list")
    public void zoneList() throws IOException, CmdLineException {
        StringWriter writer = new StringWriter();

        Denominator.execute(writer, "-p", "mock", "zone", "get");

        assertEquals(writer.toString(), ""//
                + "{name: denominator.io.}\n");
    }
}
