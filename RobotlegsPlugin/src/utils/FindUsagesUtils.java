package utils;

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * User: John Lindquist
 * Date: 6/23/11
 * Time: 5:44 PM
 */
public class FindUsagesUtils
{
    public static List<UsageInfo2UsageAdapter> findUsagesOfPsiElement(PsiElement psiElement, Project project)
    {
        final List<UsageInfo2UsageAdapter> usages = new ArrayList<UsageInfo2UsageAdapter>();
        Processor<Usage> collect = new Processor<Usage>()
        {
            public boolean process(@NotNull Usage usage)
            {
                synchronized (usages)
                {
                    usages.add(((UsageInfo2UsageAdapter) usage));
                }
                return true;
            }
        };

        FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(project)).getFindUsagesManager();
        FindUsagesHandler findUsagesHandler = findUsagesManager.getFindUsagesHandler(psiElement, false);
        findUsagesManager.processUsages(findUsagesHandler, collect);
        return usages;
    }
}
