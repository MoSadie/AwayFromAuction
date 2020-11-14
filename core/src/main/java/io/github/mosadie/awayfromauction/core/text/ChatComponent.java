package io.github.mosadie.awayfromauction.core.text;

import io.github.mosadie.awayfromauction.core.AfAUtils;

public class ChatComponent extends StringComponent {
    public ChatComponent(String string) {
        super(string);
        setColor(AfAUtils.ColorEnum.WHITE);
    }
}
