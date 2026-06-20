package com.overyourhead.hidden_places.common.npc;

public enum WayfinderProfile {
    TEST_WAYFINDER(
            "test_wayfinder",
            WayfinderDialogueStage.INTRO,
            WayfinderTradeOffer.MOSSGATE_KEY_FOR_EMERALD,
            true
    );

    private final String translationKey;
    private final WayfinderDialogueStage defaultDialogueStage;
    private final WayfinderTradeOffer tradeOffer;
    private final boolean canBecomeHostileFromDialogue;

    WayfinderProfile(
            String translationKey,
            WayfinderDialogueStage defaultDialogueStage,
            WayfinderTradeOffer tradeOffer,
            boolean canBecomeHostileFromDialogue
    ) {
        this.translationKey = translationKey;
        this.defaultDialogueStage = defaultDialogueStage;
        this.tradeOffer = tradeOffer;
        this.canBecomeHostileFromDialogue = canBecomeHostileFromDialogue;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public WayfinderDialogueStage defaultDialogueStage() {
        return this.defaultDialogueStage;
    }

    public boolean hasTrade() {
        return this.tradeOffer != null;
    }

    public WayfinderTradeOffer tradeOffer() {
        return this.tradeOffer;
    }

    public boolean allowsTradeOffer(WayfinderTradeOffer offer) {
        return this.hasTrade() && this.tradeOffer == offer;
    }

    public boolean canBecomeHostileFromDialogue() {
        return this.canBecomeHostileFromDialogue;
    }
}
