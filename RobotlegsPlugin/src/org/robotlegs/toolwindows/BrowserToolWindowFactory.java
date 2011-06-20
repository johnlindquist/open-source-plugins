package org.robotlegs.toolwindows;

import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;

import javax.swing.table.AbstractTableModel;
import java.util.*;

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

        final HashMap<JSFile, List<HashMap<JSClass, JSClass>>> mediatorMappings = mappings.getMappingByClassAndFunctionProject(project, MEDIATOR_MAP, MAP_VIEW);
        HashMap<JSFile, List<HashMap<JSVariable, JSClass>>> commandMappings = mappings.getMappingByClassAndFunctionProject(project, COMMAND_MAP, MAP_EVENT);

        Set<JSFile> keys = mediatorMappings.keySet();

        for (JSFile key : keys)
        {
            Vector<String> column = new Vector<String>();
            System.out.print(key.getName() + " has mappings: \n");
            List<HashMap<JSClass, JSClass>> mapList = mediatorMappings.get(key);
            for (HashMap<JSClass, JSClass> mapping : mapList)
            {
                Set<JSClass> mappingKeys = mapping.keySet();
                for (JSClass mappingKey : mappingKeys)
                {
                    System.out.print("\t" + mappingKey.getName() + " to " + mapping.get(mappingKey).getName() + "\n");
                    column.add(key.getName());
                    column.add(mappingKey.getName());
                    column.add(mapping.get(mappingKey).getName());
                    rows.add(column);

                }
            }

        }

        Set<JSFile> commandMapKeys = commandMappings.keySet();

        for (JSFile key : commandMapKeys)
        {
            Vector<String> column = new Vector<String>();
            System.out.print(key.getName() + " has mappings: \n");

            List<HashMap<JSVariable, JSClass>> mapList = commandMappings.get(key);
            for (HashMap<JSVariable, JSClass> mapping : mapList)
            {
                Set<JSVariable> mappingKeys = mapping.keySet();
                for (JSVariable mappingKey : mappingKeys)
                {
                    System.out.print("\t" + mappingKey.getName() + " to " + mapping.get(mappingKey).getName() + "\n");
                    column.add(key.getName());
                    column.add(mappingKey.getName());
                    column.add(mapping.get(mappingKey).getName());
                    rows.add(column);

                }
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
        JBTable jbTable = new JBTable(model);

        Content content = ContentFactory.SERVICE.getInstance().createContent(jbTable, "", false);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }
}
