package actions;

import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiFile;

/**
 * User: John Lindquist
 * Date: 6/23/11
 * Time: 5:17 PM
 */
public class ToggleToMappedClassAction extends AnAction
{
    public void actionPerformed(AnActionEvent e)
    {
        PsiFile file = e.getData(LangDataKeys.PSI_FILE);
        if (file instanceof JSFile)
        {
            JSClass jsClass = JSPsiImplUtils.findClass((JSFile) file);

        }
    }
}
