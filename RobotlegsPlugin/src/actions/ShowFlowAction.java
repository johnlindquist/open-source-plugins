package actions;

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * User: John Lindquist
 * Date: 6/16/11
 * Time: 10:26 AM
 */
public class ShowFlowAction extends AnAction
{

    public void actionPerformed(AnActionEvent e)
    {
        Project project = e.getData(LangDataKeys.PROJECT);
        buildMappingData(project, "IMediatorMap", "mapView");

//        Collection<JSClass> contextSubclasses = JSInheritanceUtil.findDirectSubClasses(jsClass, false);
//        JSClass next = contextSubclasses.iterator().next();
//        Collection<JSClass> declaringClasses = JSInheritanceUtil.findDeclaringClasses(mediatorMap);//finds a class that declares mediatorMape
//        FindUsagesHandler usagesOfPsiElement = findUsagesOfPsiElement(mapView, project);
//        PsiElement[] primaryElements = usagesOfPsiElement.getPrimaryElements();
//        JSQualifiedNamedElement next = JSResolveUtil.findElementsByName("Context", project, GlobalSearchScope.allScope(project)).iterator().next();
//        JSChangeUtil.createJSTreeFromText(project, "")
//        JSResolveUtil.getClassOfContext() //this should be useful to find the commands that reference mediatorMap

//        FindUsagesHandler findUsagesHandler = findUsagesOfPsiElement(psiElement, project);
//        ((UsageInfo2UsageAdapter)usage).getElement().getContext().getChildren()[1] //getting args list of a mapView call
//        ((UsageInfo2UsageAdapter)usage).getElement().getContext().getChildren()[1].getChildren()[0] //first arg
//        ((UsageInfo2UsageAdapter)usage).getElement().getContext().getChildren()[1].getChildren()[1] //second arg
//        ((UsageInfo2UsageAdapter)usage).getElement().getContext().getChildren()[1].getChildren()[0].resolve() //the class of the first arg

//        PsiElement[] psiElements = findUsagesHandler.getPrimaryElements();
    }

    private void buildMappingData(Project project, String className, String functionName)
    {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        //What's a better way to find a JSClass from a String? I want to find: org.robotlegs.core.IMediatorMap
        JSClass jsClass = (JSClass) JSResolveUtil.findElementsByName(className, project, GlobalSearchScope.allScope(project)).iterator().next();

        //Find the "mapView" function on IMediatorMap so we can find where it's used throughout the app
        JSFunction mapView = jsClass.findFunctionByName(functionName);

        //Find all the usages of "mapView" and return then as UsageInfo
        List<UsageInfo2UsageAdapter> mapViewUsages = findUsagesOfPsiElement(mapView, project);

        //Create a map of the first param (the "view") to the second param (the "mediator")
        HashMap<JSFile, HashMap<JSClass, JSClass>> fileWithViewToMediatorMap = getMappedElementsFromUsage(mapViewUsages);

        Set<JSFile> keys = fileWithViewToMediatorMap.keySet();

        for (JSFile key : keys)
        {
            System.out.print(key.getName() +  " has mappings: \n");
            List<HashMap<JSClass, JSClass>> listOfMappings = (List<HashMap<JSClass, JSClass>>) fileWithViewToMediatorMap.get(key);
            for (HashMap<JSClass, JSClass> mapping : listOfMappings)
            {
                Set<JSClass> viewClasses = mapping.keySet();
                for (JSClass viewClass : viewClasses)
                {
                    System.out.print("\t" + viewClass.getName() + " to " + mapping.get(viewClass).getName() +  "\n");
                }
            }
        }
    }

    private HashMap<JSFile, HashMap<JSClass, JSClass>> getMappedElementsFromUsage(List<UsageInfo2UsageAdapter> mapViewUsages)
    {
        HashMap fileToViewMediatorMapList = new HashMap<JSFile, List<HashMap < JSClass, JSClass >>>();

        for (UsageInfo2UsageAdapter mapViewUsage:mapViewUsages)
        {
            PsiFile containingFile = mapViewUsage.getElement().getContainingFile();

            List<HashMap<JSClass, JSClass>> viewMediatorMapList = (List<HashMap<JSClass, JSClass>>) fileToViewMediatorMapList.get(containingFile);

            if (viewMediatorMapList == null)
            {
                viewMediatorMapList = new ArrayList<HashMap<JSClass, JSClass>>();
                fileToViewMediatorMapList.put(containingFile, viewMediatorMapList);
            }

            //move "up" once: mediatorMap.mapView -> mediatorMap.mapView(View, Mediator)
            PsiElement context = mapViewUsage.getElement().getContext();

            //move to args: mediatorMap.mapView(View, Mediator) -> (View, Mediator)
            PsiElement[] children = context.getChildren()[1].getChildren();

            JSClass view = (JSClass) ((PsiReference) children[0]).resolve();
            JSClass mediator = (JSClass) ((PsiReference) children[1]).resolve();

            //map the view to the mediator
            HashMap<JSClass, JSClass> viewToMediatorMap = new HashMap<JSClass, JSClass>();
            viewToMediatorMap.put(view, mediator);
            viewMediatorMapList.add(viewToMediatorMap);
        }
        return fileToViewMediatorMapList;
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
        FindUsagesOptions findUsagesOptions = findUsagesHandler.getFindUsagesOptions();
        findUsagesManager.processUsages(findUsagesHandler, collect, findUsagesOptions);
        return usages;
    }

    private static void checkCallIdentifier(PsiElement element, String identifierString)
    {
        PsiElement identifier = (PsiElement) element.getContext().getContext().getChildren()[0].getNode().findChildByType(JSTokenTypes.IDENTIFIER);
        if (identifier != null)
        {
            String listenerText = element.getContext().getContext().getText();
            PsiFile containingFile = element.getContainingFile();
            if (identifier.getText().equals("mapListener"))
            {
                System.out.print("You found a robotlegs mapListener reference at " + containingFile.getText() + listenerText + "\n");
            }
            if (identifier.getText().equals(identifierString))
            {
                System.out.print("You found an addEventListener reference" + containingFile.getText() + listenerText + "\n");
            }
        }
    }
}


