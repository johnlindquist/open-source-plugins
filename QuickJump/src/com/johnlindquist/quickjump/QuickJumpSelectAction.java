package com.johnlindquist.quickjump;

import com.intellij.usageView.UsageInfo;

/**
 * User: John Lindquist
 * Date: 9/8/11
 * Time: 12:10 AM
 */
public class QuickJumpSelectAction extends QuickJumpAction{

    @Override protected void moveCaret(UsageInfo usageInfo){
        int navigationOffset = usageInfo.getNavigationOffset();
        super.moveCaret(usageInfo);
        editor.getSelectionModel().setSelection(navigationOffset, navigationOffset + searchBox.getText().length());
    }
}
