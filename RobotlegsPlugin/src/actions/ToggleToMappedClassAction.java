package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBList;

import javax.swing.*;

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

        /*if (file instanceof JSFile)
        {
            JSClass jsClass = JSPsiImplUtils.findClass((JSFile) file);

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
                            if (text.equals("mapView") || text.equals("mapEvent") || text.equals("mapSignalClass"))
                            {
                                PsiElement otherElement = null;
                                if (isElementFirstParam(element))
                                {
                                    otherElement = getParamToTheRightOfElement(element);
                                }
                                else
                                {
                                    otherElement = getParamToTheLeftOfElement(element);
                                }
                                PsiReference psiReference = (PsiReference) otherElement;
                                PsiFile containingFile = psiReference.resolve().getContainingFile();
                                VirtualFile virtualFile = containingFile.getVirtualFile();
                                FileEditorManager.getInstance(project).openFile(virtualFile, true);
                                Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();

                                selectedTextEditor.getCaretModel().moveToOffset(JSPsiImplUtils.findClass((JSFile) containingFile).getTextOffset());
                                selectedTextEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                            }
                        }
                    }
                }
            }
        }*/

        DefaultListModel model = new DefaultListModel();
        model.addElement("Hello");
        model.addElement("There");
        JBList list = new JBList(model);
        PopupChooserBuilder popupChooserBuilder = new PopupChooserBuilder(list);
        JBPopup popup = popupChooserBuilder.createPopup();
        popup.showCenteredInCurrentWindow(project);


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
