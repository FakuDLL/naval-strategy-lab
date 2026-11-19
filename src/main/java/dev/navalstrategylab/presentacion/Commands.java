package dev.navalstrategylab.presentacion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands {
    private List<Command> commands;

    private Commands(List<Command> commands) {
        this.commands = new ArrayList<>(commands);
    }

    public static Commands create(Command... commands) {
        return new Commands(Arrays.asList(commands));
    }

    public Commands add(Command command) {
        commands.add(command);
        return this;
    }

    public Commands add(Command... commands) {
        this.commands.addAll(Arrays.asList(commands));
        return this;
    }

    public static Commands uno(String id, Object parametro) {
        return create(new Command(id, parametro));
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }
}
