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
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
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

    public <K, V> HashMap<JSFile, Vector<Vector<PsiNamedElement>>> getMappingByClassAndFunctionProject(Project project, String className, String functionName)
    {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        //What's a better way to find a JSClass from a String? I want to find: org.robotlegs.core.IMediatorMap
        JSClass jsClass = (JSClass) JSResolveUtil.findElementsByName(className, project, GlobalSearchScope.allScope(project)).iterator().next();

        //Find the "mapView" function on IMediatorMap so we can find where it's used throughout the app
        JSFunction mapView = jsClass.findFunctionByName(functionName);

        //Find all the usages of "mapView" and return then as UsageInfo
        List<UsageInfo2UsageAdapter> mapViewUsages = findUsagesOfPsiElement(mapView, project);

        //Create a map of the first param (the "view") to the second param (the "mediator")
        HashMap<JSFile, Vector<Vector<PsiNamedElement>>> fileToListOfMappings = getMappedElementsFromUsage(mapViewUsages);

        return fileToListOfMappings;
    }


    private HashMap<JSFile, Vector<Vector<PsiNamedElement>>> getMappedElementsFromUsage(List<UsageInfo2UsageAdapter> functionUsages)
    {
        HashMap fileToListOfMappings = new HashMap<JSFile, Vector<Vector<PsiElement>>>();

        for (UsageInfo2UsageAdapter functionUsage : functionUsages)
        {
            PsiFile containingFile = functionUsage.getElement().getContainingFile();

            Vector<Vector<PsiNamedElement>> vectorOfMappings = (Vector<Vector<PsiNamedElement>>) fileToListOfMappings.get(containingFile);


            if (vectorOfMappings == null)
            {
                vectorOfMappings = new Vector<Vector<PsiNamedElement>>();
                fileToListOfMappings.put(containingFile, vectorOfMappings);
            }

            //move "up" once: mediatorMap.mapView -> mediatorMap.mapView(View, Mediator)
            PsiElement context = functionUsage.getElement().getContext();

            //move to args: mediatorMap.mapView(View, Mediator) -> (View, Mediator)
            PsiElement[] children = context.getChildren()[1].getChildren();

            Vector<PsiNamedElement> psiElements = new Vector<PsiNamedElement>();

            PsiNamedElement key = (PsiNamedElement) ((PsiReference) children[0]).resolve();
            psiElements.add(key);

            PsiNamedElement value = null;
            if(children.length > 1)
            {
                value = (PsiNamedElement) ((PsiReference) children[1]).resolve();
                psiElements.add(value);
            }

            vectorOfMappings.add(psiElements);
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
