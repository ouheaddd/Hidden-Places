package com.overyourhead.hidden_places.common.npc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum WayfinderProfile {
    TEST_WAYFINDER(
            "test_wayfinder",
            "intro",
            "trade_completed",
            "trade_intro",
            "hostile_dead_end",
            WayfinderTradeOffer.MOSSGATE_KEY_FOR_EMERALD,
            true,
            testDialogue()
    ),
    MOSSGATE_WAYFINDER(
            "mossgate_wayfinder",
            "intro",
            "trade_completed",
            "trade_intro",
            null,
            WayfinderTradeOffer.MOSSGATE_KEY_FOR_TORCHFLOWER,
            false,
            mossgateDialogue()
    ),
    SUNVEIL_WAYFINDER(
            "sunveil_wayfinder",
            "intro",
            "trial_completed",
            null,
            null,
            null,
            false,
            sunveilDialogue()
    );

    private final String translationKey;
    private final String defaultNodeId;
    private final String completedNodeId;
    private final String tradeNodeId;
    private final String hostileNodeId;
    private final WayfinderTradeOffer tradeOffer;
    private final boolean canBecomeHostileFromDialogue;
    private final Map<String, WayfinderDialogueNode> dialogueNodes;

    WayfinderProfile(
            String translationKey,
            String defaultNodeId,
            String completedNodeId,
            String tradeNodeId,
            String hostileNodeId,
            WayfinderTradeOffer tradeOffer,
            boolean canBecomeHostileFromDialogue,
            Map<String, WayfinderDialogueNode> dialogueNodes
    ) {
        this.translationKey = translationKey;
        this.defaultNodeId = defaultNodeId;
        this.completedNodeId = completedNodeId;
        this.tradeNodeId = tradeNodeId;
        this.hostileNodeId = hostileNodeId;
        this.tradeOffer = tradeOffer;
        this.canBecomeHostileFromDialogue = canBecomeHostileFromDialogue;
        this.dialogueNodes = Map.copyOf(dialogueNodes);
    }

    public String translationKey() {
        return this.translationKey;
    }

    public String defaultNodeId() {
        return this.defaultNodeId;
    }

    public String completedNodeId() {
        return this.completedNodeId;
    }

    public String tradeNodeId() {
        return this.tradeNodeId;
    }

    public String hostileNodeId() {
        return this.hostileNodeId;
    }

    public WayfinderDialogueNode defaultNode() {
        return this.dialogueNode(this.defaultNodeId);
    }

    public WayfinderDialogueNode completedNode() {
        return this.dialogueNode(this.completedNodeId);
    }

    public WayfinderDialogueNode dialogueNode(String nodeId) {
        WayfinderDialogueNode node = this.dialogueNodes.get(nodeId);
        if (node != null) {
            return node;
        }

        WayfinderDialogueNode defaultNode = this.dialogueNodes.get(this.defaultNodeId);
        if (defaultNode != null) {
            return defaultNode;
        }

        throw new IllegalStateException("Missing default dialogue node for " + this.name());
    }

    public boolean hasDialogueNode(String nodeId) {
        return this.dialogueNodes.containsKey(nodeId);
    }

    public boolean isCompletedNode(String nodeId) {
        return this.completedNodeId.equals(nodeId);
    }

    public boolean isHostileNode(String nodeId) {
        return this.hostileNodeId != null && this.hostileNodeId.equals(nodeId);
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

    public String nodeIdFromLegacyStage(int stageId) {
        return switch (stageId) {
            case 1 -> "who";
            case 2 -> this.hasDialogueNode("sanctum") ? "sanctum" : this.defaultNodeId;
            case 3 -> this.tradeNodeId != null ? this.tradeNodeId : this.defaultNodeId;
            case 4 -> this.hostileNodeId != null ? this.hostileNodeId : this.defaultNodeId;
            case 5 -> this.completedNodeId;
            default -> this.defaultNodeId;
        };
    }

    private static Map<String, WayfinderDialogueNode> testDialogue() {
        Map<String, WayfinderDialogueNode> nodes = new LinkedHashMap<>();
        nodes.put("intro", new WayfinderDialogueNode("intro", "intro", List.of(
                WayfinderDialogueChoice.gotoNode("option.who", "who"),
                WayfinderDialogueChoice.gotoNode("option.sanctum", "sanctum"),
                WayfinderDialogueChoice.gotoNode("option.trade", "trade_intro"),
                WayfinderDialogueChoice.gotoNode("option.insult", "hostile_dead_end")
        )));
        nodes.put("who", new WayfinderDialogueNode("who", "reply.who", List.of(
                WayfinderDialogueChoice.gotoNode("option.trade", "trade_intro"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("sanctum", new WayfinderDialogueNode("sanctum", "reply.sanctum", List.of(
                WayfinderDialogueChoice.gotoNode("option.trade", "trade_intro"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("trade_intro", new WayfinderDialogueNode("trade_intro", "reply.trade_ready", List.of(
                WayfinderDialogueChoice.openTrade("option.trade_open"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("hostile_dead_end", new WayfinderDialogueNode("hostile_dead_end", "reply.insult", List.of(
                WayfinderDialogueChoice.startFight("option.trouble"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("trade_completed", new WayfinderDialogueNode("trade_completed", "reply.trade_completed", List.of(
                WayfinderDialogueChoice.close("option.leave")
        )));
        return nodes;
    }

    private static Map<String, WayfinderDialogueNode> mossgateDialogue() {
        Map<String, WayfinderDialogueNode> nodes = new LinkedHashMap<>();
        nodes.put("intro", new WayfinderDialogueNode("intro", "intro", List.of(
                WayfinderDialogueChoice.gotoNode("option.who", "who"),
                WayfinderDialogueChoice.gotoNode("option.trade", "trade_intro"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("who", new WayfinderDialogueNode("who", "reply.who", List.of(
                WayfinderDialogueChoice.gotoNode("option.trade", "trade_intro"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("trade_intro", new WayfinderDialogueNode("trade_intro", "reply.trade_ready", List.of(
                WayfinderDialogueChoice.openTrade("option.trade_open"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("trade_completed", new WayfinderDialogueNode("trade_completed", "reply.trade_completed", List.of(
                WayfinderDialogueChoice.close("option.leave")
        )));
        return nodes;
    }

    private static Map<String, WayfinderDialogueNode> sunveilDialogue() {
        Map<String, WayfinderDialogueNode> nodes = new LinkedHashMap<>();
        nodes.put("intro", new WayfinderDialogueNode("intro", "intro", List.of(
                WayfinderDialogueChoice.gotoNode("option.who", "who"),
                WayfinderDialogueChoice.gotoNode("option.ask_trial", "ask_trial"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("who", new WayfinderDialogueNode("who", "reply.who", List.of(
                WayfinderDialogueChoice.gotoNode("option.ask_trial", "ask_trial"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("ask_trial", new WayfinderDialogueNode("ask_trial", "reply.ask_trial", List.of(
                WayfinderDialogueChoice.startSunveilTrial("option.ready"),
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("trial_started", new WayfinderDialogueNode("trial_started", "reply.trial_started", List.of(
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("trial_running", new WayfinderDialogueNode("trial_running", "reply.trial_running", List.of(
                WayfinderDialogueChoice.close("option.leave")
        )));
        nodes.put("trial_completed", new WayfinderDialogueNode("trial_completed", "reply.trial_completed", List.of(
                WayfinderDialogueChoice.close("option.leave")
        )));
        return nodes;
    }
}
