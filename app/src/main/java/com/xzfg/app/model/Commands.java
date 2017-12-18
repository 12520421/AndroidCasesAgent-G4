package com.xzfg.app.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Commands {

    private static Field[] settingsFields = AgentSettings.class.getDeclaredFields();
    private static Field[] roleFields = AgentRoles.class.getDeclaredFields();

    public static List<String> parse(String commandString) {
        LinkedList<Field> fields = new LinkedList<>();
        fields.addAll(Arrays.asList(settingsFields));
        fields.addAll(Arrays.asList(roleFields));

        LinkedList<String> commands = new LinkedList<>();

        if (commandString == null || commandString.trim().length() <= 0) {
            return commands;
        }

        String[] entries = commandString.split("\\|");

        for (String entry : entries) {
            if (entry == null || !entry.contains("~")) {
                continue;
            }

            // convert SetSetupField values to the accepted format.
            if (entry.contains("CASESAgent@")) {
                entry = entry.replace("~NONE","").replace("CASESAgent@","~");
            }

            String[] pair = entry.split("~");
            String name = uncapitalize(pair[0].replace("CASESAgent", ""));
            if (name != null && name.trim().length() > 0) {
                boolean hasField = false;
                for (Field field : fields) {
                    if (field.getName().equals(name)) {
                        hasField = true;
                    }
                }
                if (!hasField) {
                    commands.add(name);
                }
            }
        }

        return commands;
    }


    private static String uncapitalize(final String str) {
        if (str != null && str.length() > 0) {
            final char[] buffer = str.toCharArray();
            buffer[0] = Character.toLowerCase(buffer[0]);
            return new String(buffer);
        } else {
            return str;
        }
    }

}
