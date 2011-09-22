package com.johnlindquist.quickjump;

import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.keymap.impl.keyGestures.GestureActionEvent;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.TestActionEvent;
import org.jdesktop.swingx.action.ActionFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * User: John Lindquist
 * Date: 9/8/11
 * Time: 12:10 AM
 */
public class QuickJumpSmartCompletionAction extends QuickJumpAction {

    @Override
    protected void moveCaret(Integer offset) {
        super.moveCaret(offset);

        selectWordAtCaret();

        ActionManager actionManager = ActionManagerImpl.getInstance();
        final AnAction action = actionManager.getAction(IdeActions.ACTION_CODE_COMPLETION);
        AnActionEvent event = new AnActionEvent(null, editor.getDataContext(),IdeActions.ACTION_CODE_COMPLETION, inputEvent.getPresentation(), ActionManager.getInstance(), 0);
        action.actionPerformed(event);
    }

    protected void selectWordAtCaret() {
        CharSequence text = document.getCharsSequence();
        List<TextRange> ranges = new ArrayList<TextRange>();
        SelectWordUtil.addWordSelection(false, text, editor.getCaretModel().getOffset(), ranges);
        if (ranges.isEmpty()) return;

        int startWordOffset = Math.max(0, ranges.get(0).getStartOffset());
        int endWordOffset = Math.min(ranges.get(0).getEndOffset(), document.getTextLength());

        if (ranges.size() == 2 && editor.getSelectionModel().getSelectionStart() == startWordOffset &&
                editor.getSelectionModel().getSelectionEnd() == endWordOffset) {
            startWordOffset = Math.max(0, ranges.get(1).getStartOffset());
            endWordOffset = Math.min(ranges.get(1).getEndOffset(), document.getTextLength());
        }

        editor.getSelectionModel().setSelection(startWordOffset, endWordOffset);
    }
}
