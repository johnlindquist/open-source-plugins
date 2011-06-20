package org.robotlegs.toolwindows;

import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Set;
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

        HashMap<JSFile, Vector<Vector<PsiNamedElement>>> mediatorMappings = mappings.getMappingByClassAndFunctionProject(project, MEDIATOR_MAP, MAP_VIEW);
        HashMap<JSFile, Vector<Vector<PsiNamedElement>>> commandMappings = mappings.getMappingByClassAndFunctionProject(project, COMMAND_MAP, MAP_EVENT);
        HashMap<JSFile, Vector<Vector<PsiNamedElement>>> singletonMappings = mappings.getMappingByClassAndFunctionProject(project, INJECTOR, MAP_SINGLETON);

        Content mediatorContent = createTable(project, contentManager, mediatorMappings, MEDIATOR_MAP_NAME);
        Content commandContent = createTable(project, contentManager, commandMappings, COMMAND_MAP_NAME);
        Content singletonContent = createTable(project, contentManager, singletonMappings, SINGLETON_MAP_NAME);


        contentManager.setSelectedContent(commandContent);

        ((JBTable) commandContent.getComponent()).setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private Content createTable(Project project, ContentManager contentManager, HashMap<JSFile, Vector<Vector<PsiNamedElement>>> mappings, String tableName)
    {
        Set<JSFile> files = mappings.keySet();
        final Vector<Vector> names = new Vector<Vector>();
        final Vector<Vector> dataRows = new Vector<Vector>();
        prepareTableData(mappings, dataRows, names, files);
        AbstractTableModel tableModel = new MyAbstractTableModel(names);
        final JBTable table = new JBTable(tableModel);
        table.setCellSelectionEnabled(true);
        Content content = ContentFactory.SERVICE.getInstance().createContent(table, tableName, false);
        contentManager.addContent(content);
        table.addMouseListener(new MyMouseAdapter(table, dataRows, project));

        return content;
    }

    private void prepareTableData(HashMap<JSFile, Vector<Vector<PsiNamedElement>>> mappings, Vector<Vector> dataRows, Vector<Vector> names, Set<JSFile> files)
    {
        for (JSFile file : files)
        {
            System.out.print(file.getName() + " has mappings: \n");

            Vector<Vector<PsiNamedElement>> mapList = mappings.get(file);
            for (Vector<PsiNamedElement> mapping : mapList)
            {
                String name;
                PsiNamedElement value = null;
                if (mapping.size() > 1)
                {
                    value = mapping.get(1);
                }
                if (value != null) //todo: consider null object pattern
                {
                    name = value.getName();
                }
                else
                {
                    name = "Nothing";
                }

                System.out.print("\t" + mapping.get(0).getName() + " to " + name + "\n");
                Vector<String> column = new Vector<String>();
                Vector<PsiElement> dataColumn = new Vector<PsiElement>();

                column.add(file.getName());
                dataColumn.add(JSPsiImplUtils.findClass(file));

                column.add(mapping.get(0).getName());
                dataColumn.add(mapping.get(0));

                if (value != null)
                {
                    column.add(value.getName());
                    dataColumn.add(value);
                }
                names.add(column);
                dataRows.add(dataColumn);
            }
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

            PsiElement psiElement = (PsiElement) dataRows.get(row).get(column);
            VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
            Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            selectedTextEditor.getCaretModel().moveToOffset(psiElement.getTextOffset());
            selectedTextEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
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
