package org.robotlegs.toolwindows;

import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
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

    @Override public void createToolWindowContent(final Project project, ToolWindow toolWindow)
    {

        ContentManager contentManager = toolWindow.getContentManager();

        String[] columnNames = {"File", "View", "Mediator"};
        final Vector<Vector> rows = new Vector<Vector>();


        final Mappings mappings = new Mappings();

        final HashMap<JSFile, Vector<Vector<PsiNamedElement>>> mediatorMappings = mappings.getMappingByClassAndFunctionProject(project, MEDIATOR_MAP, MAP_VIEW);
        HashMap<JSFile, Vector<Vector<PsiNamedElement>>> commandMappings = mappings.getMappingByClassAndFunctionProject(project, COMMAND_MAP, MAP_EVENT);

        Set<JSFile> mediatorFiles = mediatorMappings.keySet();

        for (JSFile file : mediatorFiles)
        {
            System.out.print(file.getName() + " has mappings: \n");
            Vector<Vector<PsiNamedElement>> mapList = mediatorMappings.get(file);
            for (Vector<PsiNamedElement> mapping : mapList)
            {
                System.out.print("\t" + mapping.get(0).getName() + " to " + mapping.get(1).getName() + "\n");
                Vector<String> column = new Vector<String>();

                column.add(file.getName());
                column.add(mapping.get(0).getName());
                column.add(mapping.get(1).getName());
                rows.add(column);
            }

        }

        Set<JSFile> commandFiles = commandMappings.keySet();

        for (JSFile file : commandFiles)
        {
            System.out.print(file.getName() + " has mappings: \n");

            Vector<Vector<PsiNamedElement>> mapList = commandMappings.get(file);
            for (Vector<PsiNamedElement> mapping : mapList)
            {
                System.out.print("\t" + mapping.get(0).getName() + " to " + mapping.get(1).getName() + "\n");
                Vector<String> column = new Vector<String>();

                column.add(file.getName());
                column.add(mapping.get(0).getName());
                column.add(mapping.get(1).getName());
                rows.add(column);
            }
        }


        AbstractTableModel model = new AbstractTableModel()
        {

            @Override public int getRowCount()
            {
                return rows.size();
            }

            @Override public int getColumnCount()
            {
                return 3;
            }

            @Override public Object getValueAt(int rowIndex, int columnIndex)
            {
                return rows.get(rowIndex).get(columnIndex);
            }
        };
        final JBTable jbTable = new JBTable(model);

//        Content content = ContentFactory.SERVICE.getInstance().createContent(jbTable, "", false);
        Content content = ContentFactory.SERVICE.getInstance().createContent(jbTable, "", false);


        contentManager.addContent(content);
        contentManager.setSelectedContent(content);

        jbTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        jbTable.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e)
            {
                Object valueAt = jbTable.getValueAt(jbTable.rowAtPoint(e.getPoint()), jbTable.columnAtPoint(e.getPoint()));
                System.out.print(valueAt + "\n");
            }
        });
    }
}
