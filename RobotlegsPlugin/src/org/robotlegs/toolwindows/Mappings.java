package org.robotlegs.toolwindows;

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public <K, V> HashMap<JSFile, List<HashMap<K, V>>> getMappingByClassAndFunctionProject(Project project, String className, String functionName)
    {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        //What's a better way to find a JSClass from a String? I want to find: org.robotlegs.core.IMediatorMap
        JSClass jsClass = (JSClass) JSResolveUtil.findElementsByName(className, project, GlobalSearchScope.allScope(project)).iterator().next();

        //Find the "mapView" function on IMediatorMap so we can find where it's used throughout the app
        JSFunction mapView = jsClass.findFunctionByName(functionName);

        //Find all the usages of "mapView" and return then as UsageInfo
        List<UsageInfo2UsageAdapter> mapViewUsages = findUsagesOfPsiElement(mapView, project);

        //Create a map of the first param (the "view") to the second param (the "mediator")
        HashMap<JSFile, List<HashMap<K, V>>> fileToListOfMappings = getMappedElementsFromUsage(mapViewUsages);

        return fileToListOfMappings;
    }


    private <K, V> HashMap<JSFile, List<HashMap<K, V>>> getMappedElementsFromUsage(List<UsageInfo2UsageAdapter> functionUsages)
    {
        HashMap fileToListOfMappings = new HashMap<JSFile, List<HashMap<JSClass, JSClass>>>();

        for (UsageInfo2UsageAdapter functionUsage : functionUsages)
        {
            PsiFile containingFile = functionUsage.getElement().getContainingFile();

            List<HashMap<PsiElement, PsiElement>> listOfMappings = (List<HashMap<PsiElement, PsiElement>>) fileToListOfMappings.get(containingFile);

            if (listOfMappings == null)
            {
                listOfMappings = new ArrayList<HashMap<PsiElement, PsiElement>>();
                fileToListOfMappings.put(containingFile, listOfMappings);
            }

            //move "up" once: mediatorMap.mapView -> mediatorMap.mapView(View, Mediator)
            PsiElement context = functionUsage.getElement().getContext();

            //move to args: mediatorMap.mapView(View, Mediator) -> (View, Mediator)
            PsiElement[] children = context.getChildren()[1].getChildren();

            PsiElement key = ((PsiReference) children[0]).resolve();
            PsiElement value = ((PsiReference) children[1]).resolve();

            //map the view to the mediator
            HashMap<PsiElement, PsiElement> keyToValueMap = new HashMap<PsiElement, PsiElement>();
            keyToValueMap.put(key, value);
            listOfMappings.add(keyToValueMap);
        }
        return fileToListOfMappings;
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
