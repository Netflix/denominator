package denominator.cli;

import java.io.IOException;
import java.io.Writer;

public interface Command {

    String description();

    Action action();

    void execute(Writer writer) throws IOException;
}
