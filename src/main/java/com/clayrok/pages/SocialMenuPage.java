package com.clayrok.pages;

import com.clayrok.SocialMenu;
import com.clayrok.SocialMenuActionData;
import com.clayrok.SocialMenuConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocialMenuPage extends InteractiveCustomUIPage<SocialMenuActionData>
{
    private static final int MAX_ITEMS_PER_PAGE = 10;
    private final PlayerRef targetPlayerRef;
    private final List<SocialMenuConfig.ActionGroup> actionGroups;
    private final List<SocialMenuConfig.Action> filteredActions;
    private int actionPageIndex = 0;

    private static ScheduledFuture<?> pendingUpdate;

    public SocialMenuPage(@NonNullDecl PlayerRef playerRef, @NonNullDecl PlayerRef targetPlayerRef)
    {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SocialMenuActionData.CODEC);
        this.targetPlayerRef = targetPlayerRef;
        
        this.actionGroups = SocialMenuConfig.get().getActionGroups().stream()
                .filter(group -> hasPermission(group.permission()))
                .collect(Collectors.toList());

        this.filteredActions = new ArrayList<>();
        for (SocialMenuConfig.ActionGroup group : actionGroups)
        {
            for (SocialMenuConfig.Action action : group.actions())
            {
                if (hasPermission(action.permission()))
                {
                    filteredActions.add(action);
                }
            }
        }
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiBuilder, @NonNullDecl UIEventBuilder eventBuilder, @NonNullDecl Store<EntityStore> store)
    {
        uiBuilder.append("Pages/SocialMenu.ui");
        uiBuilder.set("#PlayerName.Text", targetPlayerRef.getUsername());

        int globalActionIndex = 0;
        int displayedCount = 0;
        int startIndex = actionPageIndex * MAX_ITEMS_PER_PAGE;

        Map<String, String> buttonsToSet = new HashMap<>();

        for (int g = 0; g < actionGroups.size(); g++)
        {
            SocialMenuConfig.ActionGroup group = actionGroups.get(g);
            boolean groupHeaderAdded = false;

            for (int i = 0; i < group.actions().size(); i++)
            {
                SocialMenuConfig.Action action = group.actions().get(i);
                if (action == null || !hasPermission(action.permission())) continue;

                if (globalActionIndex >= startIndex && displayedCount < MAX_ITEMS_PER_PAGE)
                {
                    if (!groupHeaderAdded)
                    {
                        if (displayedCount > 0) uiBuilder.appendInline("#ActionsContainer", "Group { Anchor: (Height: 20); }");

                        String groupTitle = group.title();
                        if (groupTitle != null)
                        {
                            uiBuilder.appendInline("#ActionsContainer", "Group #Title%s {}".formatted(g));
                            uiBuilder.append("#ActionsContainer #Title%s".formatted(g), "Pages/Elements/GroupTitle.ui");
                            uiBuilder.set("#ActionsContainer #Title%s #Label.Text".formatted(g), groupTitle);

                            if (SocialMenuConfig.get().getCenterTitles())
                                uiBuilder.set("#ActionsContainer #Title%s #Label.Style.HorizontalAlignment".formatted(g), "Center");
                        }
                        groupHeaderAdded = true;
                    }

                    uiBuilder.appendInline("#ActionsContainer", "Group #Action%s {}".formatted(globalActionIndex));
                    uiBuilder.append("#ActionsContainer #Action%s".formatted(globalActionIndex), "Pages/Elements/ActionButton.ui");
                    buttonsToSet.put("#Action%s #ActionButton #ButtonText.Text".formatted(globalActionIndex), action.displayName());

                    eventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "#Action%s #ActionButton".formatted(globalActionIndex),
                            EventData.of("ActionId", "ACTION").append("ActionData", String.valueOf(globalActionIndex))
                    );
                    displayedCount++;
                }
                globalActionIndex++;
            }
        }

        buildPageButtons(uiBuilder, eventBuilder);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#Overlay",
                EventData.of("ActionId", "CANCEL")
        );

        updateActionButtonsText(buttonsToSet);
    }

    private void updateActionButtonsText(Map<String, String> buttonsToSet)
    {
        if (pendingUpdate != null && !pendingUpdate.isDone()) {
            pendingUpdate.cancel(true);
        }

        pendingUpdate = SocialMenu.SHEDULER.schedule(() -> {
            UICommandBuilder uiBuilderAsync = new UICommandBuilder();
            buttonsToSet.forEach(uiBuilderAsync::set);
            buttonsToSet.clear();
            sendUpdate(uiBuilderAsync);
        }, 1, TimeUnit.MILLISECONDS);
    }

    public void buildPageButtons(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        if (filteredActions.size() > MAX_ITEMS_PER_PAGE)
        {
            uiBuilder.append("#ActionsContainer", "Pages/Elements/PageButtons.ui");
            uiBuilder.set("#ActionsContainer #PreviousPageBtn.Disabled", actionPageIndex == 0);
            uiBuilder.set("#ActionsContainer #NextPageBtn.Disabled", (actionPageIndex + 1) * MAX_ITEMS_PER_PAGE >= filteredActions.size());

            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ActionsContainer #PreviousPageBtn",
                    EventData.of("ActionId", "CHANGE_PAGE").append("direction", "-1")
            );

            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ActionsContainer #NextPageBtn",
                    EventData.of("ActionId", "CHANGE_PAGE").append("direction", "1")
            );
        }
    }

    private boolean hasPermission(String permission)
    {
        if (permission == null || permission.isEmpty()) return true;
        return PermissionsModule.get().hasPermission(playerRef.getUuid(), permission);
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, String rawData)
    {
        JsonObject jsonObj = JsonParser.parseString(rawData).getAsJsonObject();

        String actionId = jsonObj.get("ActionId").getAsString();
        switch (actionId)
        {
            case "ACTION" -> onAction(jsonObj.get("ActionData").getAsInt());
            case "VALIDATE" -> onValidate(jsonObj);
            case "CHANGE_PAGE" -> onPageChangeClicked(jsonObj.get("direction").getAsInt());
            case "CANCEL" -> onCancelClicked();
        }

        sendUpdate();
    }

    private void onAction(int actionIndex)
    {
        SocialMenuConfig.Action action = filteredActions.get(actionIndex);

        String command = action.command()
                                .replace("{senderName}", playerRef.getUsername())
                                .replace("{targetName}", targetPlayerRef.getUsername())
                                .replace("{senderUuid}", playerRef.getUuid().toString())
                                .replace("{targetUuid}", targetPlayerRef.getUuid().toString());

        if (command.contains("{"))
        {
            askForCommandVars(action.displayName(), command);
        }
        else
        {
            CommandManager.get().handleCommand(playerRef, command);
            if (SocialMenuConfig.get().getCloseOnAction()) close();

            sendUpdate();
        }
    }

    private void askForCommandVars(String actionName, String command)
    {
        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        uiBuilder.setNull("#Overlay #Content.Anchor");

        uiBuilder.clear("#ActionsContainer");
        uiBuilder.append("#ActionsContainer", "Pages/SocialMenuVars.ui");
        uiBuilder.set("#ActionsContainer #Title.Text", actionName);

        EventData eventData = EventData.of("ActionId", "VALIDATE");
        eventData.append("command", command);

        List<AbstractMap.SimpleEntry<String, String>> extractedVars = extractVariables(command);
        for (int i = 0; i < extractedVars.size(); i++)
        {
            if (i > 0) uiBuilder.appendInline("#VarsContainer", "Group #Spacer { Anchor: (Width: 15); }");

            uiBuilder.appendInline("#VarsContainer", "Group #VarInput%s {}".formatted(i));
            String selector = "#VarsContainer #VarInput%s".formatted(i);

            String inputTitle = extractedVars.get(i).getKey().replace("_", " ");
            switch (extractedVars.get(i).getValue())
            {
                case "string" -> {
                    uiBuilder.append(selector, "Pages/Elements/StringInput.ui");
                    uiBuilder.set(selector + " #Input.PlaceholderText", "...");
                }
                case "integer" -> uiBuilder.append(selector, "Pages/Elements/IntegerInput.ui");
                case "float" -> uiBuilder.append(selector, "Pages/Elements/FloatInput.ui");
            }

            uiBuilder.set(selector + " #Label.Text", inputTitle);

            eventData.append("@Value%s".formatted(i), selector + " #Input.Value");
        }

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ValidateBtn",
                eventData
        );

        sendUpdate(uiBuilder, eventBuilder, false);
    }

    private void onValidate(JsonObject jsonObj)
    {
        String command = jsonObj.get("command").getAsString();
        List<String> filteredKeys = jsonObj.keySet().stream()
                                        .filter(key -> key.startsWith("@"))
                                        .sorted()
                                        .toList();

        Pattern pattern = Pattern.compile("\\{[^}]+\\}");
        Matcher matcher = pattern.matcher(command);

        StringBuilder sb = new StringBuilder();
        int i = 0;

        while (matcher.find()) {
            if (i < filteredKeys.size()) {
                matcher.appendReplacement(sb, jsonObj.get(filteredKeys.get(i)).getAsString());
                i++;
            }
        }
        matcher.appendTail(sb);

        CommandManager.get().handleCommand(playerRef, sb.toString());

        if (SocialMenuConfig.get().getCloseOnAction())
        {
            close();
        }
    }

    private void onPageChangeClicked(int direction)
    {
        actionPageIndex += direction;
        rebuild();
    }

    private void onCancelClicked()
    {
        close();
    }

    private static List<AbstractMap.SimpleEntry<String, String>> extractVariables(String input)
    {
        List<AbstractMap.SimpleEntry<String, String>> list = new ArrayList<>();

        Pattern bracketPattern = Pattern.compile("\\{[^{}]+\\}");
        Matcher matcher = bracketPattern.matcher(input);

        while (matcher.find())
        {
            String match = matcher.group(0);
            String content = match.substring(1, match.length() - 1);

            if (content.contains(":"))
            {
                String[] parts = content.split(":", 2);
                String name = parts[0];
                String type = parts[1];
                list.add(new AbstractMap.SimpleEntry<>(name, type));
            }
        }

        return list;
    }
}