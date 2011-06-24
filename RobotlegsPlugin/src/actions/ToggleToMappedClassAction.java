package actions;

import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.lang.javascript.psi.resolve.JSInheritanceUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.usages.UsageInfo2UsageAdapter;
import enums.RobotlegsEnum;
import utils.FindUsagesUtils;

import java.util.Collection;
import java.util.List;

/**
 * User: John Lindquist
 * Date: 6/23/11
 * Time: 5:17 PM
 */
public class ToggleToMappedClassAction extends AnAction
{
    public void actionPerformed(AnActionEvent e)
    {
        Project project = e.getData(LangDataKeys.PROJECT);
        PsiFile file = e.getData(LangDataKeys.PSI_FILE);

        if (file instanceof JSFile)
        {
            JSClass jsClass = JSPsiImplUtils.findClass((JSFile) file);

            for (RobotlegsEnum classType : RobotlegsEnum.values())
            {
                //todo: determine if checking type is the best approach
                if (isType(jsClass, classType.getClassQName()))
                {
                    if (classType == RobotlegsEnum.EVENT)
                    {
                        return;
                    }

                    PsiElement functionContext = findMappingFunctionContext(classType.getMappingFunction(), jsClass, project);
                    if (functionContext != null)
                    {
                        PsiElement mappedElement = getMappedElement(functionContext, classType.getMappingParamIndex());
                        openClassInEditor(project, mappedElement);
                        return;
                    }
                }
            }

        }
    }

    private PsiElement getMappedElement(PsiElement functionElement, int paramIndex)
    {
        PsiElement paramsElement = functionElement.getChildren()[1];

        return paramsElement.getChildren()[paramIndex];
    }

    private void openClassInEditor(Project project, PsiElement classReference)
    {
        PsiReference psiReference = (PsiReference) classReference;
        PsiFile containingFile = psiReference.resolve().getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        FileEditorManager.getInstance(project).openFile(virtualFile, true);
        Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        selectedTextEditor.getCaretModel().moveToOffset(JSPsiImplUtils.findClass((JSFile) containingFile).getTextOffset());
        selectedTextEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }

/*
        DefaultListModel model = new DefaultListModel();
        model.addElement("Hello");
        model.addElement("There");
        JBList list = new JBList(model);
        PopupChooserBuilder popupChooserBuilder = new PopupChooserBuilder(list);
        JBPopup popup = popupChooserBuilder.createPopup();
        popup.showCenteredInCurrentWindow(project);

*/


    private PsiElement findMappingFunctionContext(String functionName, JSClass jsClass, Project project)
    {
        List<UsageInfo2UsageAdapter> usages = FindUsagesUtils.findUsagesOfPsiElement(jsClass, project);
        for (UsageInfo2UsageAdapter usage : usages)
        {
            PsiElement element = usage.getElement();
            PsiElement contextOneLevelUp = element.getContext();
            if (contextOneLevelUp != null)
            {
                PsiElement contextTwoLevelsUp = contextOneLevelUp.getContext();
                if (contextTwoLevelsUp != null)
                {
                    PsiElement[] children = contextTwoLevelsUp.getChildren();
                    PsiElement psiElement = children[0].getLastChild();
                    String text = null;
                    if (psiElement != null)
                    {
                        text = psiElement.getText();
                        if (text.equals(functionName))
                        {
                            return contextTwoLevelsUp;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isType(JSClass jsClass, String classQName)
    {
        Collection<JSClass> parents = JSInheritanceUtil.findAllParentsForClass(jsClass, true);
        for (JSClass parent : parents)
        {
            if (parent.getQualifiedName().equals(classQName))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isElementFirstParam(PsiElement element)
    {
        return element == element.getContext().getChildren()[0];
    }

    private PsiElement getParamToTheRightOfElement(PsiElement element)
    {
        PsiElement psiElement = element.getContext().getChildren()[1];

        return psiElement;
    }

    private PsiElement getParamToTheLeftOfElement(PsiElement element)
    {
        PsiElement psiElement = element.getContext().getChildren()[0];

        return psiElement;
    }
}
