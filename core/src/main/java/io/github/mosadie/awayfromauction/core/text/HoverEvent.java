package io.github.mosadie.awayfromauction.core.text;

public class HoverEvent {
    public enum HoverAction {
        SHOW_TEXT,
        SHOW_ACHIEVEMENT,
        SHOW_ITEM,
        SHOW_ENTITY
    }

    private final HoverAction action;
    private final ITextComponent details;

    public HoverEvent(HoverAction action, ITextComponent details) {
        this.action = action;
        this.details = details;
    }

    public HoverAction getAction() {
        return action;
    }

    public ITextComponent getDetails() {
        return details;
    }
}