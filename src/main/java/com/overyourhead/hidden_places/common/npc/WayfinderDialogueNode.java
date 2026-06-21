package com.overyourhead.hidden_places.common.npc;

import java.util.List;

public record WayfinderDialogueNode(
        String id,
        String bodyTranslationSuffix,
        List<WayfinderDialogueChoice> choices
) {
    public String bodyTranslationKey(WayfinderProfile profile) {
        return "screen.hidden_places." + profile.translationKey() + "." + this.bodyTranslationSuffix;
    }
}
