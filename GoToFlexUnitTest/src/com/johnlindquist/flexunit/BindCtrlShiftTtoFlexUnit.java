package com.johnlindquist.flexunit;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.impl.KeymapManagerImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.InputEvent;

/**
 * User: John Lindquist
 * Date: 9/12/11
 * Time: 11:11 PM
 */
public class BindCtrlShiftTtoFlexUnit implements ApplicationComponent{
    public BindCtrlShiftTtoFlexUnit(){
    }

    public void initComponent(){
        //hack: hooking into ctrl+shift+T ;)
        final Keymap keymap = KeymapManagerImpl.getInstance().getActiveKeymap();
        keymap.addShortcut("GoToFlexUnitTestOrCode", new KeyboardShortcut(KeyStroke.getKeyStroke('T', InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK), null));
    }

    public void disposeComponent(){
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName(){
        return "BindCtrlShiftTtoFlexUnit";
    }
}
