package org.robotlegs.toolwindows;

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * User: John Lindquist
 * Date: 6/19/11
 * Time: 7:41 PM
 */
public class Mappings
{
    public Mappings()
    {
    }

    public <K, V> Vector<UsageMapping> getMappingByClassAndFunctionProject(Project project, String classQName, String functionName)
    {
        //What's a better way to find a JSClass from a String? I want to find: org.robotlegs.core.IMediatorMap
        JSClass jsClass = (JSClass) JSResolveUtil.findClassByQName(classQName, GlobalSearchScope.allScope(project));


        //Find the "mapView" function on IMediatorMap so we can find where it's used throughout the app
        JSFunction mapView = jsClass.findFunctionByName(functionName);

        //Find all the usages of "mapView" and return then as UsageInfo
        List<UsageInfo2UsageAdapter> mapViewUsages = findUsagesOfPsiElement(mapView, project);

        //Create a map of the first param (the "view") to the second param (the "mediator")
        Vector<UsageMapping> fileToListOfMappings = getMappedElementsFromUsage(mapViewUsages);

        return fileToListOfMappings;
    }


    private Vector<UsageMapping> getMappedElementsFromUsage(List<UsageInfo2UsageAdapter> functionUsages)
    {
        Vector<UsageMapping> usageMappings = new Vector<UsageMapping>();

        for (UsageInfo2UsageAdapter functionUsage : functionUsages)
        {
            PsiFile containingFile = functionUsage.getElement().getContainingFile();
            UsageMapping usageMapping = new UsageMapping(functionUsage);

            //move "up" once: mediatorMap.mapView -> mediatorMap.mapView(View, Mediator)
            PsiElement context = functionUsage.getElement().getContext();

            //move to args: mediatorMap.mapView(View, Mediator) -> (View, Mediator)
            PsiElement[] children = context.getChildren()[1].getChildren();

            PsiNamedElement key = (PsiNamedElement) ((PsiReference) children[0]).resolve();

            PsiNamedElement value;
            if (children.length > 1) //todo: loop through children?
            {
                value = (PsiNamedElement) ((PsiReference) children[1]).resolve();
                usageMapping.add(value);
            }
            usageMapping.add(key);

            usageMappings.add(usageMapping);
        }
        return usageMappings;
    }

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
                    String name = ((UsageInfo2UsageAdapter) usage).getElement().getText();
                    System.out.print("found: " + name + "\n");
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
