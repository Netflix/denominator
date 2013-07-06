package denominator.cli;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import dagger.ObjectGraph;

public class GroupHandler extends OptionHandler<Group> {

    private final ObjectGraph graph = ObjectGraph.create(new SupportedGroups());

    @Inject
    Set<Group> groups;

    public GroupHandler(CmdLineParser parser, OptionDef option, Setter<Object> setter) {
        super(parser, option, setter);
        graph.inject(this);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        String groupName = params.getParameter(0);

        List<String> groupNames = new ArrayList<String>(groups.size());
        for (Group group : groups) {
            groupNames.add(group.name());
            if (groupName.equals(group.name())) {
                setter.addValue(group);
                return params.size();
            }
        }
        throw new CmdLineException(owner, format("invalid group %s valid choices: %s", groupName, groupNames));
    }

    @Override
    public String getDefaultMetaVariable() {
        return "CMD ARGS...";
    }
}
