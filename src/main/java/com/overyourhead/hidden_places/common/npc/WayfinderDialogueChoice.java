package com.overyourhead.hidden_places.common.npc;

public record WayfinderDialogueChoice(
        String textTranslationSuffix,
        WayfinderDialogueAction action,
        String targetNodeId
) {
    public static WayfinderDialogueChoice gotoNode(String textTranslationSuffix, String targetNodeId) {
        return new WayfinderDialogueChoice(textTranslationSuffix, WayfinderDialogueAction.GOTO_NODE, targetNodeId);
    }

    public static WayfinderDialogueChoice openTrade(String textTranslationSuffix) {
        return new WayfinderDialogueChoice(textTranslationSuffix, WayfinderDialogueAction.OPEN_TRADE, null);
    }

    public static WayfinderDialogueChoice startFight(String textTranslationSuffix) {
        return new WayfinderDialogueChoice(textTranslationSuffix, WayfinderDialogueAction.START_FIGHT, null);
    }

    public static WayfinderDialogueChoice close(String textTranslationSuffix) {
        return new WayfinderDialogueChoice(textTranslationSuffix, WayfinderDialogueAction.CLOSE, null);
    }

    public static WayfinderDialogueChoice complete(String textTranslationSuffix) {
        return new WayfinderDialogueChoice(textTranslationSuffix, WayfinderDialogueAction.COMPLETE, null);
    }

    public String textTranslationKey(WayfinderProfile profile) {
        return "screen.hidden_places." + profile.translationKey() + "." + this.textTranslationSuffix;
    }
}
