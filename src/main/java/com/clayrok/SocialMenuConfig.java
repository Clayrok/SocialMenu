package com.clayrok;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SocialMenuConfig
{
    private static SocialMenuConfig instance;
    private final Path configPath;

    public record Action(String displayName, String command, String permission) {}
    public record ActionGroup(String title, String permission, List<Action> actions) {}
    private final List<ActionGroup> actionGroups = new ArrayList<>();

    private String
            openPerm,
            reloadPerm;

    private boolean
            centerTitles,
            openOnUse,
            openOnPick,
            closeOnAction;

    public static SocialMenuConfig get()
    {
        if (instance == null)
        {
            instance = new SocialMenuConfig();
        }
        return instance;
    }

    private SocialMenuConfig()
    {
        this.configPath = Path.of(SocialMenuConfig.getConfigFolderPath(), "config.json");

        if (!Files.exists(configPath))
        {
            saveDefaultConfig();
        }

        reload();
    }

    private void saveDefaultConfig()
    {
        try
        {
            Files.createDirectories(configPath.getParent());
            try (var is = SocialMenuConfig.class.getResourceAsStream("/config/config.json"))
            {
                if (is != null)
                {
                    Files.copy(is, configPath);
                }
                else
                {
                    HytaleLogger.forEnclosingClass().atSevere().log("Internal config.json not found in jar resources!");
                }
            }
        }
        catch (Exception e)
        {
            HytaleLogger.forEnclosingClass().atSevere().log("Could not copy default config: " + e.getMessage());
        }
    }

    public String reload() {
        try {
            if (!Files.exists(configPath)) return "Config file not found.";

            String content = Files.readString(configPath, java.nio.charset.StandardCharsets.UTF_8);
            content = content.replace('\u00A0', ' ')  // Non-breaking space
                             .replace('\u2007', ' ')  // Figure space
                             .replace('\u202F', ' ')  // Narrow non-breaking space
                             .replace('\ufeff', ' ')  // Byte Order Mark (BOM)
                             .trim();

            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            String newOpenPerm = getStringOrNull(json, "OPEN_PERM");
            String newReloadPerm = getString(json, "RELOAD_PERM", "clayrok.socialmenu.reload");
            boolean newCenterTitles = getBool(json, "CENTER_TITLES", true);
            boolean newOpenOnUse = getBool(json, "OPEN_ON_USE", false);
            boolean newOpenOnPick = getBool(json, "OPEN_ON_PICK", true);
            boolean newCloseOnAction = getBool(json, "CLOSE_ON_ACTION", true);

            List<ActionGroup> newGroups = parseActionGroups(json, "ActionGroups");

            this.openPerm = newOpenPerm;
            this.reloadPerm = newReloadPerm;
            this.centerTitles = newCenterTitles;
            this.openOnUse = newOpenOnUse;
            this.openOnPick = newOpenOnPick;
            this.closeOnAction = newCloseOnAction;

            this.actionGroups.clear();
            this.actionGroups.addAll(newGroups);

            return "Config reloaded.";
        }
        catch (Exception e) {
            HytaleLogger.forEnclosingClass().atSevere().withCause(e).log("Error reloading config");
            return "An error occurred: " + e.getClass().getSimpleName();
        }
    }

    private String getStringOrNull(JsonObject json, String key) { return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : null; }
    private String getString(JsonObject json, String key, String def) { return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : def; }
    private boolean getBool(JsonObject json, String key, boolean def) { return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsBoolean() : def; }

    private List<ActionGroup> parseActionGroups(JsonObject json, String key)
    {
        List<ActionGroup> groups = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray())
        {
            JsonArray array = json.getAsJsonArray(key);
            for (int i = 0; i < array.size(); i++)
            {
                JsonObject groupObj = array.get(i).getAsJsonObject();
                String title = getString(groupObj, "title", null);
                String groupPermission = getStringOrNull(groupObj, "permission");
                List<Action> actions = new ArrayList<>();

                if (groupObj.has("actions") && groupObj.get("actions").isJsonArray())
                {
                    JsonArray actionsArray = groupObj.getAsJsonArray("actions");
                    for (int j = 0; j < actionsArray.size(); j++)
                    {
                        JsonObject item = actionsArray.get(j).getAsJsonObject();
                        String displayName = getString(item, "displayName", "");
                        String command = getString(item, "command", "");
                        String actionPermission = getStringOrNull(item, "permission");
                        actions.add(new Action(displayName, command, actionPermission));
                    }
                }

                groups.add(new ActionGroup(title, groupPermission, actions));
            }
        }

        return groups;
    }

    public static String getConfigFolderPath()
    {
        try
        {
            Path jarLocation = Path.of(SocialMenuConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            return jarLocation.resolve("SocialMenu").toString();
        }
        catch (Exception e) { return "SocialMenu"; }
    }

    public String getOpenPerm() { return openPerm; }
    public String getReloadPerm() { return reloadPerm; }
    public boolean getCenterTitles() { return centerTitles; }
    public boolean getOpenOnUse() { return openOnUse; }
    public boolean getOpenOnPick() { return openOnPick; }
    public boolean getCloseOnAction() { return closeOnAction; }
    public List<ActionGroup> getActionGroups() { return actionGroups; }
}