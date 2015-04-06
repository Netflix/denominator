package denominator.cli;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import java.util.Iterator;

import denominator.DNSApiManager;
import denominator.cli.Denominator.DenominatorCommand;
import denominator.model.Zone;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.forArray;
import static java.lang.String.format;

class ZoneCommands {

  @Command(name = "list", description = "Lists the zones present in this provider.  The zone id is the first column.")
  public static class ZoneList extends DenominatorCommand {

    @Option(type = OptionType.COMMAND, name = {"-n",
                                               "--name"}, description = "name of the zone. ex. denominator.io.")
    public String name;

    public Iterator<String> doRun(final DNSApiManager mgr) {
      Iterator<Zone> zones =
          name == null ? mgr.api().zones().iterator() : mgr.api().zones().iterateByName(name);
      return Iterators.transform(zones, new Function<Zone, String>() {
        @Override
        public String apply(Zone input) {
          return format("%-24s %-36s %-36s %d", input.id(), input.name(), input.email(),
                        input.ttl());
        }
      });
    }
  }

  @Command(name = "add", description =
      "Adds a zone or updates the first existing zone with the same name, outputs the result's id.\n"
      + "Note: This may create a duplicate zone if the provider supports it.")
  public static class ZoneAdd extends DenominatorCommand {

    @Option(type = OptionType.COMMAND, required = true, name = {"-n",
                                                                "--name"}, description = "name of the zone. ex. denominator.io.")
    public String name;

    @Option(type = OptionType.COMMAND, name = {"-t",
                                               "--ttl"}, description = "time to live of the zone (SOA). defaults to 86400")
    public int ttl = 86400;

    @Option(type = OptionType.COMMAND, required = true, name = {"-e",
                                                                "--email"}, description = "Email contact for the zone. ex. admin@denominator.io")
    public String email;

    public Iterator<String> doRun(final DNSApiManager mgr) {
      final Zone zone = Zone.create(null, name, ttl, email);
      return new Iterator<String>() {
        boolean printed = false;
        boolean replaced = false;
        boolean done = false;

        @Override
        public boolean hasNext() {
          return !done;
        }

        @Override
        public String next() {
          if (!printed) {
            printed = true;
            String context = zone.name() + (zone.id() == null ? "" : " [" + zone.id() + "]");
            return format(";; adding zone %s with ttl %d and email %s", context, ttl, email);
          } else if (!replaced) {
            replaced = true;
            return mgr.api().zones().put(zone);
          } else {
            done = true;
            return ";; ok";
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  @Command(name = "update", description = "Updates an existing zone with the specified ttl and/or email.")
  public static class ZoneUpdate extends DenominatorCommand {

    @Option(type = OptionType.COMMAND, required = true, name = {"-i",
                                                                "--id"}, description = "id of the zone.")
    public String id;

    @Option(type = OptionType.COMMAND, name = {"-t",
                                               "--ttl"}, description = "time to live of the zone (SOA)")
    public Integer ttl;

    @Option(type = OptionType.COMMAND, name = {"-e",
                                               "--email"}, description = "Email contact for the zone. ex. nil@denominator.io")
    public String email;

    public Iterator<String> doRun(final DNSApiManager mgr) {
      final Zone existing = getZone(mgr, id);
      final Zone update = Zone.create(id, existing.name(), ttl != null ? ttl : existing.ttl(),
                                      email != null ? email : existing.email());
      if (existing.equals(update)) {
        return forArray(";; ok");
      }
      return new Iterator<String>() {
        boolean printed = false;
        boolean done = false;

        @Override
        public boolean hasNext() {
          return !done;
        }

        @Override
        public String next() {
          if (!printed) {
            printed = true;
            StringBuilder result = new StringBuilder();
            result.append(";; updating zone ").append(existing.id()).append(" with ");
            if (ttl != null && email != null) {
              result.append("ttl ").append(ttl).append(" and ").append("email ").append(email);
            } else if (ttl != null) {
              result.append("ttl ").append(ttl);
            } else {
              result.append("email ").append(email);
            }
            return result.toString();
          } else {
            done = true;
            mgr.api().zones().put(update);
            return ";; ok";
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  static Zone getZone(DNSApiManager mgr, String id) {
    for (Zone zone : mgr.api().zones()) {
      if (zone.id().equals(id)) {
        return zone; // TODO: consider getById
      }
    }
    throw new IllegalArgumentException("zone " + id + " not found");
  }

  @Command(name = "delete", description = "deletes a zone by id")
  public static class ZoneDelete extends DenominatorCommand {

    @Option(type = OptionType.COMMAND, required = true, name = {"-i",
                                                                "--id"}, description = "id of the zone.")
    public String id;

    public Iterator<String> doRun(final DNSApiManager mgr) {
      String cmd = format(";; deleting zone %s", id);
      return concat(forArray(cmd), new Iterator<String>() {
        boolean done = false;

        @Override
        public boolean hasNext() {
          return !done;
        }

        @Override
        public String next() {
          mgr.api().zones().delete(id);
          done = true;
          return ";; ok";
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      });
    }
  }
}
