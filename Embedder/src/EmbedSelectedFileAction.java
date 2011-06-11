import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.ecmal4.impl.JSAttributeImpl;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.lang.javascript.psi.impl.JSVarStatementImpl;
import com.intellij.lang.javascript.ui.JSFormatUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

/**
 * User: John Lindquist
 * Date: 6/11/11
 * Time: 10:29 AM
 */
public class EmbedSelectedFileAction extends AnAction
{

    /**
     * Let's you select a file from the project view and add it as [Embed(source="selectedFile")]
     * to the currently open class in the editor
     * @param e
     */
    public void actionPerformed(AnActionEvent e)
    {
        PsiFile image = e.getData(LangDataKeys.PSI_FILE);

        //What's the best way to find the relative path to the src for the selected PsiFile?
        String imagePath = "/assets/";
        //How can I find the psiClass that's currently open in the editor?
        PsiFile targetFile = e.getData(LangDataKeys.PSI_FILE).getParent().getParentDirectory().getSubdirectories()[1].getSubdirectories()[0].getFiles()[0];

        if (targetFile instanceof JSFile)
        {
            JSFile jsFile = (JSFile) targetFile;
            final JSClass jsClass = JSPsiImplUtils.findClass(jsFile);

            Project project = e.getData(LangDataKeys.PROJECT);

            //Need a better naming scheme for the asset since images can just be numbers like 1452345423.jpg :(
            ASTNode jsTreeFromText = JSChangeUtil.createJSTreeFromText(project, "var asset" + image.getName().replace(".jpg", "") + ":Class;", JavaScriptSupportLoader.ECMA_SCRIPT_L4);
            final JSVarStatementImpl jsVarStatement = new JSVarStatementImpl(jsTreeFromText);

            ASTNode publicAttribute = JSChangeUtil.createJSTreeFromText(project, "public");
            jsVarStatement.add(new JSAttributeImpl(publicAttribute));

            ASTNode embed = JSChangeUtil.createJSTreeFromText(project, "[Embed(source=\""+ imagePath + image.getName() + "\")] ");
            jsVarStatement.add(new JSAttributeImpl(embed));

            ApplicationManager.getApplication().runWriteAction(new Runnable()
            {
                @Override public void run()
                {

                    jsClass.addBefore(jsVarStatement, jsClass.getConstructor());
                    //How do I reformat after the var has been added?
                    JSFormatUtil.formatClass(jsClass, 0);
                }
            });
        }

    }
}
