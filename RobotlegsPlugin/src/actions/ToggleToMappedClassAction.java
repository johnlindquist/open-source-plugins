package actions;

import com.intellij.lang.javascript.flex.XmlBackedJSClassImpl;
import com.intellij.lang.javascript.psi.JSElement;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList;
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
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.usages.UsageInfo2UsageAdapter;
import enums.RobotlegsEnum;
import utils.FindUsagesUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        final Project project = e.getData(LangDataKeys.PROJECT);
        PsiFile file = e.getData(LangDataKeys.PSI_FILE);

        final JSClass jsClass = getJSClassFromFile(file);

        if (jsClass != null)
        {
            for (final RobotlegsEnum classType : RobotlegsEnum.values())
            {
                //todo: determine if checking type is the best approach since Command isn't always subclassed
                if (isType(jsClass, classType.getClassQName()))
                {
                    if (classType == RobotlegsEnum.EVENT)
                    {
                        showListOfPublicStatics(project, jsClass, classType);
                        return;
                    }

                    if (openMappedElement(jsClass, classType, project))
                    {
                        return;
                    }
                }
            }
        }
    }

    private void showListOfPublicStatics(final Project project, JSClass jsClass, final RobotlegsEnum classType)
    {
        DefaultListModel model = new DefaultListModel();
        for (JSVariable jsVariable : jsClass.getFields())
        {
            if (isPublicStatic(jsVariable))
            {
                model.addElement(jsVariable);
            }
        }
        final JBList list = new JBList(model);
        list.setCellRenderer(new ListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                @SuppressWarnings({"unchecked"})
                final JComponent comp = new JBLabel(((JSVariable) value).getName());
                comp.setOpaque(true);
                if (isSelected)
                {
                    comp.setBackground(list.getSelectionBackground());
                    comp.setForeground(list.getSelectionForeground());
                }
                else
                {
                    comp.setBackground(list.getBackground());
                    comp.setForeground(list.getForeground());
                }
                return comp;
            }
        });


        list.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            @Override public void valueChanged(ListSelectionEvent e)
            {
                Object source = e.getSource();
                System.out.println(source.toString());
            }
        });


        /* list.addKeyListener(new KeyAdapter()
{
@Override public void keyReleased(KeyEvent e)
{
 if (e.getKeyCode() == KeyEvent.VK_ENTER)
 {
     Object selectedValue = list.getSelectedValue();
     openMappedElement((JSElement) selectedValue, classType, project);
 }
}
});*/


        PopupChooserBuilder popupChooserBuilder = new PopupChooserBuilder(list);
        JBPopup popup = popupChooserBuilder.createPopup();
        popup.showCenteredInCurrentWindow(project);


        list.addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e)
            {
                System.out.println(e.toString());
                openMappedElement((JSElement) list.getSelectedValue(), classType, project);
            }
        });

        list.addKeyListener(new KeyAdapter()
        {
            @Override public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    System.out.println(e.toString());
                    openMappedElement((JSElement) list.getSelectedValue(), classType, project);
                }
            }
        });


        return;
    }

    private boolean isPublicStatic(JSVariable jsVariable)
    {
        JSAttributeList attributeList = jsVariable.getAttributeList();
        if (attributeList.hasModifier(JSAttributeList.ModifierType.STATIC) && attributeList.getAccessType() == JSAttributeList.AccessType.PUBLIC)
        {
            return true;
        }
        return false;
    }

    private boolean openMappedElement(JSElement jsElement, RobotlegsEnum classType, Project project)
    {
        PsiElement functionContext = findMappingFunctionContext(classType.getMappingFunction(), jsElement, project);
        if (functionContext != null)
        {
            PsiElement mappedElement = getMappedElement(functionContext, classType.getMappingParamIndex());
            openClassInEditor(project, mappedElement);
            return true;
        }
        return false;
    }

    private JSClass getJSClassFromFile(PsiFile file)
    {
        JSClass jsClass = null;
        if (file instanceof XmlFile)
        {
            jsClass = XmlBackedJSClassImpl.getXmlBackedClass(getRootTag((XmlFile) file));
        }

        if (file instanceof JSFile)
        {
            jsClass = JSPsiImplUtils.findClass((JSFile) file);
        }
        return jsClass;
    }

    private static XmlTag getRootTag(XmlFile xmlFile)
    {
        final XmlDocument document = xmlFile.getDocument();
        return document != null ? document.getRootTag() : null;
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

        if (containingFile instanceof JSFile) //it might be an MXML file
        {
            selectedTextEditor.getCaretModel().moveToOffset(JSPsiImplUtils.findClass((JSFile) containingFile).getTextOffset());
            selectedTextEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }

    private PsiElement findMappingFunctionContext(String functionName, JSElement jsElement, Project project)
    {
        List<UsageInfo2UsageAdapter> usages = FindUsagesUtils.findUsagesOfPsiElement(jsElement, project);
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
