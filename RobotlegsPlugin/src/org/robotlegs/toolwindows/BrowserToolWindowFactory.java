package org.robotlegs.toolwindows;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
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

    private static final String MEDIATOR_MAP = "IMediatorMap";
    private static final String MAP_VIEW = "mapView";

    private static final String COMMAND_MAP = "ICommandMap";
    private static final String MAP_EVENT = "mapEvent";

    private static final String INJECTOR = "IInjector";
    private static final String MAP_SINGLETON = "mapSingleton";

    //tab names
    private static final String MEDIATOR_MAP_NAME = "MediatorMap";
    private static final String COMMAND_MAP_NAME = "CommandMap";
    private static final String SINGLETON_MAP_NAME = "SingletonMap";

    @Override public void createToolWindowContent(final Project project, ToolWindow toolWindow)
    {
        ContentManager contentManager = toolWindow.getContentManager();

        Mappings mappings = new Mappings();

        Vector<UsageMapping> mediatorMappings = mappings.getMappingByClassAndFunctionProject(project, MEDIATOR_MAP, MAP_VIEW);
        Vector<UsageMapping> commandMappings = mappings.getMappingByClassAndFunctionProject(project, COMMAND_MAP, MAP_EVENT);
        Vector<UsageMapping> singletonMappings = mappings.getMappingByClassAndFunctionProject(project, INJECTOR, MAP_SINGLETON);

        Content mediatorContent = createTable(project, contentManager, mediatorMappings, MEDIATOR_MAP_NAME);
        Content commandContent = createTable(project, contentManager, commandMappings, COMMAND_MAP_NAME);
        Content singletonContent = createTable(project, contentManager, singletonMappings, SINGLETON_MAP_NAME);


        contentManager.setSelectedContent(commandContent);

        ((JBTable) commandContent.getComponent()).setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private Content createTable(Project project, ContentManager contentManager, Vector<UsageMapping> usageMappings, String tableName)
    {
        final Vector<Vector> names = new Vector<Vector>();
        final Vector<Vector> dataRows = new Vector<Vector>();

        prepareTableData(usageMappings, dataRows, names, usageMappings);

        AbstractTableModel tableModel = new MyAbstractTableModel(names);
        final JBTable table = new JBTable(tableModel);

        table.setCellSelectionEnabled(true);
        Content content = ContentFactory.SERVICE.getInstance().createContent(table, tableName, false);
        contentManager.addContent(content);

        table.addMouseListener(new MyMouseAdapter(table, dataRows, project));

        return content;
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

    private static class MyMouseAdapter extends MouseAdapter
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

        @Override public void mouseClicked(MouseEvent e)
        {
            int row = table.rowAtPoint(e.getPoint());
            int column = table.columnAtPoint(e.getPoint());
            Object valueAt = table.getValueAt(row, column);
            System.out.print(valueAt + "\n");

            if (dataRows.get(row).get(column) instanceof PsiElement)
            {
                PsiElement psiElement = (PsiElement) dataRows.get(row).get(column);
                VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
                FileEditorManager.getInstance(project).openFile(virtualFile, true);
                Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();


                selectedTextEditor.getCaretModel().moveToOffset(psiElement.getTextOffset());
                selectedTextEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            }

            if (dataRows.get(row).get(column) instanceof UsageInfo2UsageAdapter)
            {
                UsageInfo2UsageAdapter usageAdapter = (UsageInfo2UsageAdapter) dataRows.get(row).get(column);
                VirtualFile virtualFile = usageAdapter.getFile();
                FileEditorManager.getInstance(project).openFile(virtualFile, true);
                Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();


                selectedTextEditor.getCaretModel().moveToOffset(usageAdapter.getUsageInfo().getNavigationOffset());
                selectedTextEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            }


        }
    }

    private static class MyAbstractTableModel extends AbstractTableModel
    {

        private final Vector<Vector> rowNames;

        public MyAbstractTableModel(Vector<Vector> rowNames)
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
}
