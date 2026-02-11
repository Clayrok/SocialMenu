package com.clayrok;

import com.clayrok.pages.SocialMenuPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

public class SocialMenuInteractionWatcher implements PlayerPacketWatcher
{
    @Override
    public void accept(PlayerRef playerRef, Packet packet)
    {
        if (SocialMenuConfig.get().getOpenPerm() != null &&
                !PermissionsModule.get().hasPermission(playerRef.getUuid(), SocialMenuConfig.get().getOpenPerm())) return;

        if (packet instanceof SyncInteractionChains)
        {
            for (SyncInteractionChain interaction : ((SyncInteractionChains) packet).updates)
            {
                boolean openOnUse = interaction.interactionType == InteractionType.Use &&
                        SocialMenuConfig.get().getOpenOnUse();
                boolean openOnPick = interaction.interactionType == InteractionType.Pick &&
                        SocialMenuConfig.get().getOpenOnPick();

                if (openOnUse || openOnPick)
                {
                    Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
                        Ref<EntityStore> ref = playerRef.getReference();
                        Store<EntityStore> store = ref.getStore();

                        Ref<EntityStore> targetEntityRef = TargetUtil.getTargetEntity(ref, store);
                        if (targetEntityRef == null) return;

                        Store<EntityStore> targetEntityStore = targetEntityRef.getStore();
                        PlayerRef targetPlayerRef = targetEntityStore.getComponent(targetEntityRef,
                                Universe.get().getPlayerRefComponentType());
                        if (targetPlayerRef == null) return;

                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player == null) return;

                        player.getPageManager().openCustomPage(ref, store, new SocialMenuPage(playerRef, targetPlayerRef));
                    });
                }
            }
        }
    }
}
