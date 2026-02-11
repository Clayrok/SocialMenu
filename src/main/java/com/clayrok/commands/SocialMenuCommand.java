package com.clayrok.commands;

import com.clayrok.commands.subcommands.ReloadCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class SocialMenuCommand extends AbstractCommandCollection
{
    public SocialMenuCommand()
    {
        super("SocialMenu", "SocialMenu base command.");
        this.addSubCommand(new ReloadCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}