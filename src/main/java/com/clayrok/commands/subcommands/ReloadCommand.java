package com.clayrok.commands.subcommands;

import com.clayrok.SocialMenuConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ReloadCommand extends AbstractPlayerCommand
{
    public ReloadCommand()
    {
        super("Reload", "Reloads the configuration.");
        this.requirePermission(SocialMenuConfig.get().getReloadPerm());
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world)
    {
        SocialMenuConfig.get().reload();

        Message success = Message.raw("Configuration reloaded.");
        success.color("#2ae917");

        playerRef.sendMessage(success);
    }
}