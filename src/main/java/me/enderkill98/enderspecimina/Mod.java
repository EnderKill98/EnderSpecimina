package me.enderkill98.enderspecimina;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Mod implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("EnderSpeciminia");
    private static final HashMap<String, Logger> LOGGERS_FOR_CLASSES = new HashMap<>();

    public static ScrollZoom scrollZoom;
    public static HighwayVisualizer highwayVisualizer;
    public static ItemFrameKeeper itemFrameKeeper;

    @Override
    public void onInitializeClient() {
        Config.HANDLER.load();

        scrollZoom = new ScrollZoom();
        ClientTickEvents.START_CLIENT_TICK.register(scrollZoom);

        highwayVisualizer = new HighwayVisualizer();
        ClientTickEvents.END_CLIENT_TICK.register(highwayVisualizer);
        highwayVisualizer.setActive(Config.HANDLER.instance().highwaysEnabled);

        itemFrameKeeper = new ItemFrameKeeper();
        ClientTickEvents.END_CLIENT_TICK.register(itemFrameKeeper);
        itemFrameKeeper.onSetActive(Config.HANDLER.instance().itemFrameKeeper);
    }

    public static Logger getLoggerFor(Class<?> clazz) {
        return getLoggerFor(clazz.getPackageName(), clazz.getSimpleName());
    }

    public static Logger getLoggerFor(String packageName, String simpleClassName) {
        final String fullName = packageName + "." + simpleClassName;
        if(Mod.LOGGERS_FOR_CLASSES.containsKey(fullName))
            return LOGGERS_FOR_CLASSES.get(fullName);
        String[] packageParts = packageName.split("\\.");
        if (packageParts.length < 3 || !packageParts[0].equals("me") || !packageParts[1].equals("enderkill98") || !packageParts[2].equals("enderspeciminia")) {
            LOGGER.warn("A non-EnderStuff-Logger was requested for " + fullName);
            LOGGERS_FOR_CLASSES.put(fullName, LOGGER);
            return LOGGER;
        }
        String category = packageParts.length > 3 ? packageParts[3] : "";
        category = switch (category) {
            case "botcommands" -> "Bot";
            case "dotcommands" -> "Dot";
            case "mixin" -> "Mixin";
            case "module" -> "Module";
            case "utils" -> "Util";
            case "render" -> "Render";
            default -> category;
        };

        String name = simpleClassName;
        if(name.endsWith("Command")) name = name.substring(0, name.length() - "Command".length());
        if(name.endsWith("Mixin")) name = name.substring(0, name.length() - "Mixin".length());
        if(name.endsWith("Util")) name = name.substring(0, name.length() - "Util".length());

        Logger logger = LoggerFactory.getLogger("EnderSpeciminia/" + (category.isEmpty() ? "" : category + "/") + name);
        LOGGERS_FOR_CLASSES.put(fullName, logger);
        //LOGGER.info("Registered logger: " + logger.getName());
        return logger;
    }

}
