package fr.horizonsmp.jeirecipefix.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandRouterTest {

    @Test
    void noArgsIsHelp() {
        assertEquals(CommandRouter.Type.HELP, CommandRouter.route(new String[]{}).type());
    }

    @Test
    void resyncDefaultsToSelf() {
        CommandRouter.Action a = CommandRouter.route(new String[]{"resync"});
        assertEquals(CommandRouter.Type.RESYNC, a.type());
        assertEquals("self", a.target());
    }

    @Test
    void resyncWithTargetKeepsTarget() {
        CommandRouter.Action a = CommandRouter.route(new String[]{"reSync", "Notch"});
        assertEquals(CommandRouter.Type.RESYNC, a.type());
        assertEquals("Notch", a.target());
    }

    @Test
    void reloadAndInfoAndUnknown() {
        assertEquals(CommandRouter.Type.RELOAD, CommandRouter.route(new String[]{"reload"}).type());
        assertEquals(CommandRouter.Type.INFO, CommandRouter.route(new String[]{"info"}).type());
        CommandRouter.Action unknown = CommandRouter.route(new String[]{"bogus"});
        assertEquals(CommandRouter.Type.UNKNOWN, unknown.type());
        assertNull(CommandRouter.route(new String[]{"reload"}).target());
    }
}
