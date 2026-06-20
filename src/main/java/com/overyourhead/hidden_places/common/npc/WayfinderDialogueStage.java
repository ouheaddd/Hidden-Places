package com.overyourhead.hidden_places.common.npc;

import java.util.Arrays;

public enum WayfinderDialogueStage {
    INTRO(0, "intro"),
    WHO(1, "reply.who"),
    SANCTUM(2, "reply.sanctum"),
    TRADE(3, "reply.trade_ready"),
    HOSTILE_DEAD_END(4, "reply.insult"),
    TRADE_COMPLETED(5, "reply.trade_completed");

    private final int id;
    private final String bodyTranslationSuffix;

    WayfinderDialogueStage(int id, String bodyTranslationSuffix) {
        this.id = id;
        this.bodyTranslationSuffix = bodyTranslationSuffix;
    }

    public int id() {
        return this.id;
    }

    public String bodyTranslationKey(WayfinderProfile profile) {
        return "screen.hidden_places." + profile.translationKey() + "." + this.bodyTranslationSuffix;
    }

    public static WayfinderDialogueStage byId(int id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id == id)
                .findFirst()
                .orElse(INTRO);
    }
}
