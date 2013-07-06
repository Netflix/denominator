package denominator.cli;

import static dagger.Provides.Type.SET;
import static denominator.common.Preconditions.checkArgument;
import static java.lang.System.getProperty;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.testng.annotations.Test;

import dagger.Module;
import dagger.Provides;
import denominator.BasicProvider;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.cli.Denominator.ReaderForPath;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.model.Zone;

@Test
public class DenominatorTest {

    @Test
    public void defaultCommandIsGet() throws IOException, CmdLineException {
        StringWriter one = new StringWriter();
        Denominator.execute(one, "-p", "mock", "zone", "get");
        StringWriter two = new StringWriter();
        Denominator.execute(two, "-p", "mock", "zone");
        assertEquals(one.toString(), two.toString());
    }

    @Test
    public void oldListWorks() throws IOException, CmdLineException {
        StringWriter one = new StringWriter();
        Denominator.execute(one, "-p", "mock", "zone", "get");
        StringWriter two = new StringWriter();
        Denominator.execute(two, "-p", "mock", "zone", "list");
        assertEquals(one.toString(), two.toString());
    }

    @Test
    public void parseWhenNoCredentialsRequired() throws IOException, CmdLineException {
        Denominator.execute(new StringWriter(), "-p", "mock", "zone", "get");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "invalid provider name superdns! valid choices: \\[mock.*\\]")
    public void providerNotFound() throws IOException, CmdLineException {
        Denominator.execute(new StringWriter(), "-p", "superdns", "zone", "get");
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*mem:wowow.*")
    public void overridesUrl() throws IOException, CmdLineException {
        Denominator.execute(new StringWriter(), "-p", "route53", "-u", "mem:wowow", "-c", "joe", "-c", "letmein",
                "zone", "get");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. needscredentials requires one of the following forms: when type is accessKey: accessKey,secretKey; session: accessKey,secretKey,sessionToken")
    public void needsCredentials() throws IOException, CmdLineException {
        Denominator.execute(new StringWriter(), new String[] { "-p", "needscredentials", "zone", "get" },
                new NeedsCredentials());
    }

    @Test
    public void credentialsFromArgs() throws IOException, CmdLineException {
        Writer writer = new StringWriter();

        Denominator.execute(writer, new String[] { "-p", "needscredentials", "-c", "joe", "-c", "letmein", "zone",
                "get" }, new NeedsCredentials());

        assertEquals(writer.toString(), ""//
                + "{name: joe}\n"//
                + "--- {name: letmein}\n"//
                + "");
    }

    @Module(library = true)
    class NeedsCredentials {
        @Provides(type = SET)
        Provider providerToModule() {
            return new NeedsCredentialsProvider();
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "file not found .*/foobar.yaml")
    public void yamlNotFound() throws IOException, CmdLineException {
        Denominator.execute(new StringWriter(), new String[] { "-n", "twoPart", "-C", "~/foobar.yaml", "zone", "get" },
                new TestYaml());
    }

    public void providerFromYamlReadsTwoPartCredentials() throws IOException, CmdLineException {
        Writer writer = new StringWriter();

        Denominator.execute(writer, new String[] { "-n", "twoPart", "-C", "~/.denominator/config.yml", "zone", "get" },
                new NeedsCredentials(), new TestYaml());

        assertEquals(writer.toString(), ""//
                + "{name: foo1}\n"//
                + "--- {name: foo2}\n"//
                + "");
    }

    public void providerFromYamlReadsThreePartCredentials() throws IOException, CmdLineException {
        Writer writer = new StringWriter();

        Denominator.execute(writer,
                new String[] { "-n", "threePart", "-C", "~/.denominator/config.yml", "zone", "get" },
                new NeedsCredentials(), new TestYaml());

        assertEquals(writer.toString(), ""//
                + "{name: foo3}\n"//
                + "--- {name: foo4}\n"//
                + "--- {name: foo5}\n"//
                + "");
    }

    @Module(overrides = true, library = true)
    class TestYaml {
        @Provides
        ReaderForPath readerForPath() {
            return new ReaderForPath() {
                @Override
                public Reader apply(String path) {
                    checkArgument(path.equals(getProperty("user.home") + "/.denominator/config.yml"),
                            "file not found %s", path);
                    return new StringReader(""//
                            + "name: twoPart\n" //
                            + "provider: needscredentials\n" //
                            + "credentials:\n" //
                            + "  accessKey: foo1\n"//
                            + "  secretKey: foo2\n" //
                            + "---\n"//
                            + "name: threePart\n" //
                            + "provider: needscredentials\n"//
                            + "credentials:\n"//
                            + "  accessKey: foo3\n" //
                            + "  secretKey: foo4\n" //
                            + "  sessionToken: foo5\n"//
                            + "\n");
                }
            };
        }
    }

    static class NeedsCredentialsProvider extends BasicProvider {

        @Override
        public Map<String, Collection<String>> credentialTypeToParameterNames() {
            Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
            options.put("accessKey", Arrays.asList("accessKey", "secretKey"));
            options.put("session", Arrays.asList("accessKey", "secretKey", "sessionToken"));
            return options;
        }

        @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, includes = {
                NothingToClose.class, GeoUnsupported.class, WeightedUnsupported.class,
                OnlyBasicResourceRecordSets.class })
        static class Module {
            @Provides
            Provider provider() {
                return new NeedsCredentialsProvider();
            }

            // prints the credentials out instead of zones
            @Provides
            ZoneApi provideZoneApi(Credentials creds) {
                final List<Zone> zones = new ArrayList<Zone>();
                for (Object credentialArg : ListCredentials.asList(creds)) {
                    zones.add(Zone.create(credentialArg.toString()));
                }
                return new ZoneApi() {
                    public Iterator<Zone> iterator() {
                        return zones.iterator();
                    }
                };
            }

            @Provides
            ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory() {
                return null;
            }
        }
    }
}
