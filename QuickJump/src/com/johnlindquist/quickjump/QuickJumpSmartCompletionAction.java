package com.johnlindquist.quickjump;

import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;

/**
 * User: John Lindquist
 * Date: 9/8/11
 * Time: 12:10 AM
 */
public class QuickJumpSmartCompletionAction extends QuickJumpAction{

    @Override protected void moveCaret(Integer offset){
        super.moveCaret(offset);

        selectWordAtCaret();

        final AnAction action = ActionManagerImpl.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION);

        action.actionPerformed(inputEvent);
    }

    protected void selectWordAtCaret(){
        CharSequence text = document.getCharsSequence();
        List<TextRange> ranges = new ArrayList<TextRange>();
        SelectWordUtil.addWordSelection(false, text, editor.getCaretModel().getOffset(), ranges);
        if (ranges.isEmpty()) return;

        int startWordOffset = Math.max(0, ranges.get(0).getStartOffset());
        int endWordOffset = Math.min(ranges.get(0).getEndOffset(), document.getTextLength());

        if (ranges.size() == 2 && editor.getSelectionModel().getSelectionStart() == startWordOffset &&
                editor.getSelectionModel().getSelectionEnd() == endWordOffset){
            startWordOffset = Math.max(0, ranges.get(1).getStartOffset());
            endWordOffset = Math.min(ranges.get(1).getEndOffset(), document.getTextLength());
        }

        editor.getSelectionModel().setSelection(startWordOffset, endWordOffset);
    }
}
