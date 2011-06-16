package actions;

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiElement;
import com.intellij.usages.Usage;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * User: John Lindquist
 * Date: 6/16/11
 * Time: 10:26 AM
 */
public class ShowFlowAction extends AnAction
{

    public void actionPerformed(AnActionEvent e)
    {
        final List<Usage> usages = new ArrayList<Usage>();
        final PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);
        Processor<Usage> collect = new Processor<Usage>()
        {
            public boolean process(@NotNull Usage usage)
            {
                synchronized (usages)
                {
                    usages.add(usage);
                }
                return true;
            }
        };

        FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(e.getData(LangDataKeys.PROJECT))).getFindUsagesManager();
        FindUsagesHandler findUsagesHandler = findUsagesManager.getFindUsagesHandler(psiElement, false);
        findUsagesManager.processUsages(findUsagesHandler, collect);

        PsiElement[] psiElements = findUsagesHandler.getPrimaryElements();
    }
}
