package io.github.mosadie.awayfromauction.core.text;

import io.github.mosadie.awayfromauction.core.AfAUtils.ColorEnum;

import java.util.List;

public interface ITextComponent {
    void appendSibling(ITextComponent sibling);
    void appendText(String text);
    List<ITextComponent> getSiblings();

    String getAsUnformattedString();

    boolean getBold();
    ITextComponent setBold(boolean bold);

    boolean getItalicized();
    ITextComponent setItalicized(boolean italicized);

    boolean getUnderlined();
    ITextComponent setUnderlined(boolean underlined);

    ColorEnum getColor();
    ITextComponent setColor(ColorEnum color);

    ITextComponent setClickEvent(ClickEvent event);
    ClickEvent getClickEvent();

    ITextComponent setHoverEvent(HoverEvent event);
    HoverEvent getHoverEvent();
}