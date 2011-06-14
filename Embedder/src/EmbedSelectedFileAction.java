import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.lang.javascript.psi.impl.JSVarStatementImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiBinaryFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.file.PsiFileImplUtil;

import java.util.ArrayList;

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
     *
     * @param e
     */
    public void actionPerformed(AnActionEvent e)
    {
        PsiFile image = e.getData(LangDataKeys.PSI_FILE);

        if (image instanceof PsiBinaryFile)
        {
            //The editor isn't in focus, so you have to find it to get the currently selected file ("targetFile")
            Editor editor = FileEditorManager.getInstance(e.getData(LangDataKeys.PROJECT)).getSelectedTextEditor();
            if (editor instanceof EditorImpl)
            {
                //todo: I bet there's a Util to do this logic below for me... Otherwise, build my own
                VirtualFile virtualFile = ((EditorImpl) editor).getVirtualFile();
                ArrayList<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();
                virtualFiles.add(virtualFile);
                PsiManager psiManager = image.getManager();
                PsiFile[] psiFilesByVirtualFiles = PsiFileImplUtil.getPsiFilesByVirtualFiles(virtualFiles, psiManager);
                PsiFile targetFile = psiFilesByVirtualFiles[0]; //todo: determine if this would ever be incorrect... (getting the first psiFile result from its virtualFile)


                if (targetFile instanceof JSFile)
                {
                    //We need the psiFile to be able to access psi elements (constructor, methods, etc) and add/edit/update them
                    JSFile jsFile = (JSFile) targetFile;
                    final JSClass jsClass = JSPsiImplUtils.findClass(jsFile);


                    //This is the "src" dir of the image
                    VirtualFile sourceRoot = ModuleRootManager.getInstance(e.getData(LangDataKeys.MODULE)).getSourceRoots()[0];
                    char separator = '/';
                    //Get the relative path from the image to the src dir
                    String relativePathFromRoot = VfsUtil.getRelativePath(image.getVirtualFile(), sourceRoot, separator);
                    Project project = e.getData(LangDataKeys.PROJECT);
                    //Because an image can start with a number (and a var can't), prepend the image name with "image_"
                    String statement = "[Embed(source=\"/" + relativePathFromRoot + "\")] public var image_" + image.getName().replace(".jpg", "") + ":Class;";
                    ASTNode jsTreeFromText = JSChangeUtil.createJSTreeFromText(project, statement, JavaScriptSupportLoader.ECMA_SCRIPT_L4);
                    final JSVarStatementImpl jsVarStatement = new JSVarStatementImpl(jsTreeFromText);

                    ApplicationManager.getApplication().runWriteAction(new Runnable()
                    {
                        @Override public void run()
                        {

                            jsClass.addBefore(jsVarStatement, jsClass.getConstructor());
                            CodeStyleManager.getInstance(jsClass.getManager()).reformat(jsClass);
                        }
                    });
                }

            }
        }
    }
}
