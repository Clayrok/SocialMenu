package com.clayrok;

import com.clayrok.commands.SocialMenuCommand;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SocialMenu extends JavaPlugin
{
    private PacketFilter socialMenuInteractionWatcher = null;

    public SocialMenu(@NonNullDecl JavaPluginInit init)
    {
        super(init);
    }

    @Override
    protected void setup()
    {
        socialMenuInteractionWatcher = PacketAdapters.registerInbound(new SocialMenuInteractionWatcher());

        getCommandRegistry().registerCommand(new SocialMenuCommand());
    }

    @Override
    protected void shutdown()
    {
        PacketAdapters.deregisterInbound(socialMenuInteractionWatcher);
    }
}