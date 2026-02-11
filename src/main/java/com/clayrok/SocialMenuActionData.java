package com.clayrok;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class SocialMenuActionData
{
    public String actionId;
    public String actionData;

    public static final BuilderCodec<SocialMenuActionData> CODEC = 
        BuilderCodec.builder(SocialMenuActionData.class, SocialMenuActionData::new)
        .append(
            new KeyedCodec<>("ActionId", Codec.STRING),
            (obj, val) -> obj.actionId = val,
            obj -> obj.actionId
        )
        .add()
        .append(
            new KeyedCodec<>("ActionData", Codec.STRING),
            (obj, val) -> obj.actionData = val,
            obj -> obj.actionData
        )
        .add()
        .build();
}