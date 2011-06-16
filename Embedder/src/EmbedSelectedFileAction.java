import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;

import java.util.ArrayList;
import java.util.List;

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
        Project project = e.getData(LangDataKeys.PROJECT);

        PsiFile[] psiFilesToEmbed = getPsiFilesToEmbed(e);
        for (PsiFile psiFile : psiFilesToEmbed)
        {
            embedPsiFile(psiFile, ModuleUtil.findModuleForPsiElement(psiFile), project);
        }
    }


    private PsiFile[] getPsiFilesToEmbed(AnActionEvent e)
    {
        List<PsiFile> psiFiles = new ArrayList<PsiFile>();

        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        PsiElement[] psiElements = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
        PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);

        if (psiFile != null)
        {
            psiFiles.add(psiFile);
        }
        else if (psiElement != null)
        {
            if (psiElement instanceof PsiDirectory)
            {
                PsiDirectory psiDirectory = (PsiDirectory) psiElement;
                addAllPsiFilesFromSubdirectories(psiDirectory, psiFiles);
            }
        }
        else if (psiElements != null)
        {
            for (PsiElement element : psiElements)
            {
                if (element instanceof PsiFile)
                {
                    psiFiles.add((PsiFile) element);
                }
            }
        }
        return PsiUtilBase.toPsiFileArray(psiFiles);
    }

    private void addAllPsiFilesFromSubdirectories(PsiDirectory psiDirectory, List<PsiFile> psiFiles)
    {
        PsiFile[] psiDirectoryFiles = psiDirectory.getFiles();
        for (PsiFile psiDirectoryFile : psiDirectoryFiles)
        {
            psiFiles.add(psiDirectoryFile);
        }

        for (PsiDirectory psiSubDirectory : psiDirectory.getSubdirectories())
        {
            addAllPsiFilesFromSubdirectories(psiSubDirectory, psiFiles);
        }
    }

    private void embedPsiFile(final PsiFile psiFile, final Module module, final Project project)
    {
        ApplicationManager.getApplication().runWriteAction(new Runnable()
        {
            @Override public void run()
            {
                if (psiFile instanceof PsiBinaryFile)
                {
                    //The editor isn't in focus, so you have to find it to get the currently selected file ("targetFile")
                    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                    if (editor != null)
                    {
                        PsiFile targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

                        //replace with different strategies depending on file type
                        if (targetFile instanceof JSFile)
                        {
                            //We need the psiFile to be able to access psi elements (constructor, methods, etc) and add/edit/update them
                            JSFile jsFile = (JSFile) targetFile;
                            final JSClass jsClass = JSPsiImplUtils.findClass(jsFile);


                            char separator = '/';
                            VirtualFile commonAncestor = VfsUtil.getCommonAncestor(targetFile.getVirtualFile(), psiFile.getVirtualFile());
                            //This is the "src" dir of the class
                            VirtualFile sourceRoot = ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(targetFile.getVirtualFile());
                            String imageFullPath = VfsUtil.getRelativePath(psiFile.getVirtualFile(), commonAncestor, separator);
                            String pathToRepresentWithEllipses;
                            pathToRepresentWithEllipses = VfsUtil.getRelativePath(sourceRoot, commonAncestor, separator);
                            if (pathToRepresentWithEllipses == null) //this case occurs when the image is deeper in the structure than the class
                            {
                                pathToRepresentWithEllipses = "";
                                imageFullPath = VfsUtil.getRelativePath(psiFile.getVirtualFile(), sourceRoot, separator);
                            }


                            String ellipses = pathToRepresentWithEllipses.replaceAll("[^/^\\/]+", "..");
                            String relativePathFromRoot = null;
                            if (!ellipses.equals(""))
                            {
                                relativePathFromRoot = "/" + ellipses + "/" + imageFullPath;
                            }
                            else
                            {
                                relativePathFromRoot = "/" + imageFullPath;
                            }

                            //Because an image can start with a number (and a var can't), prepend the image name with "image_"
                            String statement = "[Embed(source=\"" + relativePathFromRoot + "\")]\npublic var image_" + psiFile.getName().replace(".jpg", "") + ":Class;";
                            ASTNode jsTreeFromText = JSChangeUtil.createJSTreeFromText(project, statement, JavaScriptSupportLoader.ECMA_SCRIPT_L4);

                            if (jsClass.getConstructor() != null)
                            {
                                jsClass.addBefore(jsTreeFromText.getPsi(), jsClass.getConstructor());
                            }
                            else
                            {
                                jsClass.add(jsTreeFromText.getPsi());
                            }
                        }
                    }

                }
            }
        });
    }
}
