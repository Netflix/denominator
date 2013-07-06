package denominator.cli.groups;

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import org.kohsuke.args4j.CmdLineException;
import org.testng.annotations.Test;

import com.google.common.base.Splitter;

import denominator.cli.Denominator;

@Test
public class VersionGroupTest {

    @Test(description = "denominator version")
    public void version() throws IOException, CmdLineException {
        StringWriter writer = new StringWriter();

        Denominator.execute(writer, "version", "get");

        Iterator<String> lines = Splitter.onPattern("\\r?\\n").split(writer.toString()).iterator();
        assertTrue(lines.next().startsWith("Denominator "));
        assertTrue(lines.next().startsWith("Java version: "));
    }
}
