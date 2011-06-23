package utils;

import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.usages.UsageInfo2UsageAdapter;
import org.robotlegs.toolwindows.UsageMapping;

import java.util.List;
import java.util.Vector;

/**
 * User: John Lindquist
 * Date: 6/19/11
 * Time: 7:41 PM
 */
public class RobotlegsMappingUtils
{

    public static <K, V> Vector<UsageMapping> getMappingByClassAndFunctionProject(Project project, String classQName, String[] functionNames)
    {
        Vector<UsageMapping> fileToListOfMappings = new Vector<UsageMapping>();
        JSClass jsClass = (JSClass) JSResolveUtil.findClassByQName(classQName, GlobalSearchScope.allScope(project));

        if (jsClass != null)
        {
            for (String functionName : functionNames)
            {
                //Find the "mapView" function on IMediatorMap so we can find where it's used throughout the app
                JSFunction foundFunction = jsClass.findFunctionByName(functionName);

                if (foundFunction != null)
                {
                    //Find all the usages of "mapView" and return then as UsageInfo
                    List<UsageInfo2UsageAdapter> mapViewUsages = FindUsagesUtils.findUsagesOfPsiElement(foundFunction, project);

                    //Create a map of the first param (the "view") to the second param (the "mediator")
                    fileToListOfMappings.addAll(getMappedElementsFromUsage(mapViewUsages));
                }
            }
        }

        return fileToListOfMappings;
    }


    private static Vector<UsageMapping> getMappedElementsFromUsage(List<UsageInfo2UsageAdapter> functionUsages)
    {
        Vector<UsageMapping> usageMappings = new Vector<UsageMapping>();

        for (UsageInfo2UsageAdapter functionUsage : functionUsages)
        {
            PsiFile containingFile = functionUsage.getElement().getContainingFile();
            UsageMapping usageMapping = new UsageMapping(functionUsage);

            //move "up" from function: mediatorMap.mapView -> mediatorMap.mapView(View, Mediator)
            PsiElement context = functionUsage.getElement().getContext();

            if (context != null)
            {
                //move "right" to args: mediatorMap.mapView(View, Mediator) -> (View, Mediator)
                PsiElement[] children = context.getChildren()[1].getChildren();
                for (PsiElement child : children)
                {
                    usageMapping.add(ResolveUtils.resolveElement(child));
                }

                usageMappings.add(usageMapping);
            }

        }
        return usageMappings;
    }

}
