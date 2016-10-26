package org.hobbit.core;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

public class UniqueCommandsTest {

    @Test
    public void testCommands() throws IllegalArgumentException, IllegalAccessException {
        String[] definedCommands = new String[256];
        Class<Commands> clazz = Commands.class;
        Field[] fields = clazz.getFields();
        int commandId;
        for (int i = 0; i < fields.length; ++i) {
            if (!fields[i].getName().equals("ID_TO_COMMAND_NAME_MAP")) {
                commandId = fields[i].getByte(null);
                Assert.assertNull("The command " + fields[i].getName() + " has the same ID as the command "
                        + definedCommands[commandId], definedCommands[commandId]);
                definedCommands[commandId] = fields[i].getName();
            }
        }
    }

    @Test
    public void testControllerApiCommands() throws IllegalArgumentException, IllegalAccessException {
        String[] definedCommands = new String[256];
        Class<FrontEndApiCommands> clazz = FrontEndApiCommands.class;
        Field[] fields = clazz.getFields();
        int commandId;
        for (int i = 0; i < fields.length; ++i) {
            commandId = fields[i].getByte(null);
            Assert.assertNull("The command " + fields[i].getName() + " has the same ID as the command "
                    + definedCommands[commandId], definedCommands[commandId]);
            definedCommands[commandId] = fields[i].getName();
        }
    }
}
