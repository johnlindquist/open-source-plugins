package actions;

import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.lang.javascript.psi.resolve.JSInheritanceUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import runnables.CreateMediatorRunnable;

/**
 * User: John Lindquist
 * Date: 6/8/11
 * Time: 10:21 PM
 */
public class CreateMediatorAction extends AnAction
{
    private Boolean isClassDisplayObjectContainer;
    private JSFile jsFile;
    private JSClass jsClass;

    @Override public void update(AnActionEvent e)
    {
        super.update(e);

        isClassDisplayObjectContainer = false;

        PsiFile psiFile = e.getData(DataKeys.PSI_FILE);
        if (psiFile instanceof JSFile)
        {
            jsFile = (JSFile) psiFile;

            jsClass = JSPsiImplUtils.findClass(jsFile);
            if (jsClass != null)
            {
                //What's the easiest way to check if the class "is a flash.display.DisplayObjectContainer"?
                isClassDisplayObjectContainer = JSInheritanceUtil.isParentClass(jsClass, "flash.display.DisplayObjectContainer");

            }
        }
    }

    /**
     * Create a "Mediator" class if  the current class is a DisplayObjectContainer
     * e.g., the current class is "FooContainer" then this would create "FooContainerMediator"
     *
     * @param e
     */

    //TODO: Add functionality to create from the instance under the caret
    public void actionPerformed(AnActionEvent e)
    {
/*
        if (isClassDisplayObjectContainer)
        {
            String className = jsClass.getName() + "Mediator.as";
            PsiDirectory directory = jsFile.getContainingDirectory();

            Runnable runnable = (Runnable) new CreateMediatorRunnable(directory, className);
            ApplicationManager.getApplication().runWriteAction(runnable);
            //TODO: How do a I create a psi from scratch? Or should I just use a template?
        }
*/

        PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);
        if (psiElement instanceof JSVariable)
        {
            JSVariable jsVariable = (JSVariable) psiElement;
            JSClass variableClass = jsVariable.getType().resolveClass();
            createMediatorFromClass(variableClass);
        }

        if (psiElement instanceof JSClass)
        {
            createMediatorFromClass((JSClass) psiElement);
        }

        if (psiElement instanceof JSFile)
        {
            JSClass psiClass = JSPsiImplUtils.findClass((JSFile) psiElement);
            createMediatorFromClass(psiClass);
        }

        if (psiElement != null)
        {
            System.out.print(psiElement.getText());
        }
    }

    private void createMediatorFromClass(JSClass variableClass)
    {
        boolean parentClass = JSInheritanceUtil.isParentClass(variableClass, "flash.display.DisplayObjectContainer");

        if (parentClass)
        {
            String className = variableClass.getName() + "Mediator.as";

            PsiDirectory containingDirectory = variableClass.getContainingFile().getContainingDirectory();
            Runnable runnable = (Runnable) new CreateMediatorRunnable(containingDirectory, className);
            ApplicationManager.getApplication().runWriteAction(runnable);
        }
    }

}
