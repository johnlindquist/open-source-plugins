package com.johnlindquist.quickjump;

/**
 * User: John Lindquist
 * Date: 9/8/11
 * Time: 12:10 AM
 */
public class QuickJumpSelectFromCaretAction extends QuickJumpAction{

    @Override protected void moveCaret(Integer offset){
        editor.getSelectionModel().removeSelection();
        int caretOffset = editor.getCaretModel().getOffset();
        if(offset < caretOffset){
            offset = offset + searchBox.getText().length();
        }
        editor.getCaretModel().moveToOffset(offset);

        editor.getSelectionModel().setSelection(caretOffset, offset + searchBox.getText().length());
    }
}
