package io.github.mosadie.awayfromauction.core.text;

import io.github.mosadie.awayfromauction.core.AfAUtils.ColorEnum;

import java.util.ArrayList;
import java.util.List;

public class StringComponent implements ITextComponent {

    private final List<ITextComponent> siblings;
    private final String string;

    private boolean bold = false;
    private boolean italics = false;
    private boolean underlined = false;
    private ColorEnum color = ColorEnum.BLACK;
    private ClickEvent clickEvent;
    private HoverEvent hoverEvent;

    public StringComponent(String string) {
        this.string = string;
        siblings = new ArrayList<>();
    }

    @Override
    public void appendSibling(ITextComponent sibling) {
        siblings.add(sibling);
    }

    @Override
    public void appendText(String text) {
        appendSibling(new StringComponent(text));
    }

    @Override
    public List<ITextComponent> getSiblings() {
        return siblings;
    }

    @Override
    public String getAsUnformattedString() {
        return string;
    }

    @Override
    public boolean getBold() {
        return bold;
    }

    @Override
    public ITextComponent setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    @Override
    public boolean getItalicized() {
        return italics;
    }

    @Override
    public ITextComponent setItalicized(boolean italicized) {
        this.italics = italicized;
        return this;
    }

    @Override
    public ColorEnum getColor() {
        return color;
    }

    @Override
    public ITextComponent setColor(ColorEnum color) {
        this.color = color;
        return this;
    }

    @Override
    public boolean getUnderlined() {
        return underlined;
    }

    @Override
    public ITextComponent setUnderlined(boolean underlined) {
        this.underlined = underlined;
        return this;
    }
    
    @Override
    public ITextComponent setClickEvent(ClickEvent event) {
        this.clickEvent = event;
        return this;
    }

    @Override
    public ClickEvent getClickEvent() {
        return clickEvent;
    }

    @Override
    public ITextComponent setHoverEvent(HoverEvent event) {
        this.hoverEvent = event;
        return this;
    }

    @Override
    public HoverEvent getHoverEvent() {
        return hoverEvent;
    }
}