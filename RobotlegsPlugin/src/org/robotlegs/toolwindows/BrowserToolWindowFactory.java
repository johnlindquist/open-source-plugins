package org.robotlegs.toolwindows;

import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;
import com.intellij.usages.UsageInfo2UsageAdapter;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

/**
 * User: John Lindquist
 * Date: 6/19/11
 * Time: 6:15 PM
 */
public class BrowserToolWindowFactory implements ToolWindowFactory
{

    private static final String MEDIATOR_MAP = "org.robotlegs.core.IMediatorMap";
    private static final String MAP_VIEW = "mapView";

    private static final String COMMAND_MAP = "org.robotlegs.core.ICommandMap";
    private static final String MAP_EVENT = "mapEvent";

    private static final String INJECTOR = "org.robotlegs.core.IInjector";
    private static final String MAP_SINGLETON = "mapSingleton";

    //tab names
    private static final String MEDIATOR_MAP_NAME = "MediatorMap";
    private static final String COMMAND_MAP_NAME = "CommandMap";
    private static final String SINGLETON_MAP_NAME = "SingletonMap";
    private Project project;
    private ToolWindow toolWindow;

    @Override public void createToolWindowContent(final Project project, ToolWindow toolWindow)
    {
        this.project = project;
        this.toolWindow = toolWindow;
        ContentManager contentManager = toolWindow.getContentManager();

        Mappings mappings = new Mappings();

        Vector<UsageMapping> mediatorMappings = mappings.getMappingByClassAndFunctionProject(project, MEDIATOR_MAP, MAP_VIEW);
        Vector<UsageMapping> commandMappings = mappings.getMappingByClassAndFunctionProject(project, COMMAND_MAP, MAP_EVENT);
        Vector<UsageMapping> singletonMappings = mappings.getMappingByClassAndFunctionProject(project, INJECTOR, MAP_SINGLETON);

        Content mediatorContent = createTable(project, contentManager, mediatorMappings, MEDIATOR_MAP_NAME);
        Content commandContent = createTable(project, contentManager, commandMappings, COMMAND_MAP_NAME);
        Content singletonContent = createTable(project, contentManager, singletonMappings, SINGLETON_MAP_NAME);
    }

    private Content createTable(Project project, ContentManager contentManager, Vector<UsageMapping> usageMappings, String tableName)
    {
        final Vector<Vector> names = new Vector<Vector>();
        final Vector<Vector> dataRows = new Vector<Vector>();

        prepareTableData(usageMappings, dataRows, names, usageMappings);

        if (names.size() > 0)
        {
            AbstractTableModel tableModel = new MappingsTableModel(names);
            final JBTable table = new JBTable(tableModel);

            table.setCellSelectionEnabled(true);
            Content content = ContentFactory.SERVICE.getInstance().createContent(table, tableName, false);
            contentManager.addContent(content);

            table.addMouseListener(new MyMouseAdapter(table, dataRows, project));
            table.setEnableAntialiasing(true);

            return content;
        }

        return null;
    }

    private void prepareTableData(Vector<UsageMapping> mappings, Vector<Vector> dataRows, Vector<Vector> names, Vector<UsageMapping> usageMappings)
    {
        for (UsageMapping usageMapping : usageMappings)
        {
            Vector<String> column = new Vector<String>();
            Vector<Object> dataColumn = new Vector<Object>();

            System.out.print(usageMapping.getUsage().getFile().getName() + " has mappings: \n");

            PsiFile psiFile = usageMapping.getUsage().getElement().getContainingFile();
            column.add(psiFile.getName()); //todo: reconsider how to approach getting names of files
            dataColumn.add(usageMapping.getUsage());

            Vector<PsiNamedElement> mappedElements = usageMapping.getMappedElements();
            for (PsiNamedElement mapping : mappedElements)
            {
                column.add(mapping.getName());
                dataColumn.add(mapping);
            }

            names.add(column);
            dataRows.add(dataColumn);
        }
    }

    private class MyMouseAdapter extends MouseAdapter
    {
        private final JBTable table;
        private final Vector<Vector> dataRows;
        private final Project project;

        public MyMouseAdapter(JBTable table, Vector<Vector> dataRows, Project project)
        {
            this.table = table;
            this.dataRows = dataRows;
            this.project = project;
        }

        @Override public void mouseClicked(final MouseEvent e)
        {
            int row = table.rowAtPoint(e.getPoint());
            int column = table.columnAtPoint(e.getPoint());
            Object selectionName = table.getValueAt(row, column);
            System.out.print(selectionName + "\n");

            Object itemUnderMouse = dataRows.get(row).get(column);

            if (SwingUtilities.isLeftMouseButton(e))
            {
                if (itemUnderMouse instanceof PsiElement)
                {
                    PsiElement psiElement = (PsiElement) itemUnderMouse;
                    VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
                    FileEditorManager.getInstance(project).openFile(virtualFile, true);
                    Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();

                    selectedTextEditor.getCaretModel().moveToOffset(psiElement.getTextOffset());
                    selectedTextEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                }

                if (itemUnderMouse instanceof UsageInfo2UsageAdapter)
                {
                    UsageInfo2UsageAdapter usageAdapter = (UsageInfo2UsageAdapter) itemUnderMouse;
                    VirtualFile virtualFile = usageAdapter.getFile();
                    FileEditorManager.getInstance(project).openFile(virtualFile, true);
                    Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();

                    selectedTextEditor.getCaretModel().moveToOffset(usageAdapter.getUsageInfo().getNavigationOffset());
                    selectedTextEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                }
            }


            if (SwingUtilities.isRightMouseButton(e))
            {
                ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TODO_VIEW_TOOLBAR, createActionGroup(selectionName, itemUnderMouse));
                popupMenu.getComponent().show(table, table.getMousePosition().x, table.getMousePosition().y);

            }

        }
    }

    private ActionGroup createActionGroup(Object selectionName, final Object itemUnderMouse)
    {
        DefaultActionGroup group = new DefaultActionGroup();

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        final PsiFile targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

        group.add(new InjectClassIntoEditorAction(selectionName, targetFile, itemUnderMouse));

        return group;
    }

    private static class MappingsTableModel extends AbstractTableModel
    {

        private final Vector<Vector> rowNames;

        public MappingsTableModel(Vector<Vector> rowNames)
        {
            this.rowNames = rowNames;
        }

        @Override public int getRowCount()
        {
            return rowNames.size();
        }

        @Override public int getColumnCount()
        {
            return rowNames.get(0).size();
        }

        @Override public Object getValueAt(int rowIndex, int columnIndex)
        {

            Vector rows = rowNames.get(rowIndex);
            return rows.get(columnIndex);
        }
    }

    private class InjectSelectedClassIntoEditorClass implements Runnable
    {
        private final PsiFile targetFile;
        private final Object itemUnderMouse;

        public InjectSelectedClassIntoEditorClass(PsiFile targetFile, Object itemUnderMouse)
        {
            this.targetFile = targetFile;
            this.itemUnderMouse = itemUnderMouse;
        }

        @Override public void run()
        {
            JSClass editorClass = JSPsiImplUtils.findClass((JSFile) targetFile);
            JSClass jsClass = (JSClass) itemUnderMouse;
            String nameOfInjectedClass = jsClass.getName();
            JSFile containingFile = (JSFile) jsClass.getContainingFile();
            String importStatement = "import " + JSPsiImplUtils.findPackageStatement(containingFile).getQualifiedName() + "." + jsClass.getName();
            PsiElement importLine = JSChangeUtil.createJSTreeFromText(project, importStatement, JavaScriptSupportLoader.ECMA_SCRIPT_L4).getPsi();
            editorClass.addBefore(importLine, editorClass.getFirstChild());

            String lowercaseNameOfClass = nameOfInjectedClass.substring(0, 1).toLowerCase() + nameOfInjectedClass.substring(1, nameOfInjectedClass.length());

            String statement = "[Inject]\npublic var " + lowercaseNameOfClass + ":" + nameOfInjectedClass + ";";
            PsiElement injectedField = JSChangeUtil.createJSTreeFromText(project, statement, JavaScriptSupportLoader.ECMA_SCRIPT_L4).getPsi();
            editorClass.addBefore(injectedField, editorClass.getFunctions()[0]);

            System.out.println(statement);

            PsiDocumentManager.getInstance(project).commitAllDocuments();
        }
    }

    private class InjectClassIntoEditorAction extends AnAction
    {

        private final PsiFile targetFile;
        private final Object itemUnderMouse;

        public InjectClassIntoEditorAction(Object selectionName, PsiFile targetFile, Object itemUnderMouse)
        {
            super("Inject " + selectionName + " into " + targetFile.getName());
            this.targetFile = targetFile;
            this.itemUnderMouse = itemUnderMouse;
        }

        @Override public void actionPerformed(AnActionEvent e)
        {
            if (itemUnderMouse instanceof PsiElement)
            {
                CommandProcessor.getInstance().executeCommand(project, new Runnable()
                {
                    @Override public void run()
                    {
                        ApplicationManager.getApplication().runWriteAction(new InjectSelectedClassIntoEditorClass(targetFile, itemUnderMouse));
                    }
                }, "Inject Class as Field into File", null);

            }
        }
    }
}
