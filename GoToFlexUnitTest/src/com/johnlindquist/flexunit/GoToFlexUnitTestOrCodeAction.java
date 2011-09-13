package com.johnlindquist.flexunit;

import com.intellij.javascript.flex.MxmlParserDefinition;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.lang.javascript.validation.fixes.CreateClassOrInterfaceAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.testIntegration.GotoTestOrCodeAction;

import java.io.IOException;

/**
 * User: John Lindquist
 * Date: 9/12/11
 * Time: 11:21 PM
 */
public class GoToFlexUnitTestOrCodeAction extends AnAction{

    protected PsiFile psiFile;

    public void actionPerformed(AnActionEvent e){
        psiFile = e.getData(DataKeys.PSI_FILE);
        if (psiFile instanceof JSFile || psiFile instanceof MxmlParserDefinition.MxmlFile){
            goToFlexUnitTestOrCodeAction(e);
        }
        else{
            //hack: do the old gotoTest if it's not a JS file.
            final GotoTestOrCodeAction gotoTestOrCodeAction = new GotoTestOrCodeAction();
            gotoTestOrCodeAction.actionPerformed(e);
        }
    }

    private void goToFlexUnitTestOrCodeAction(AnActionEvent e){
        final Module module = e.getData(DataKeys.MODULE);
        final Project project = e.getData(PlatformDataKeys.PROJECT);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        JSClass jsClass = JSPsiImplUtils.findClass((JSFile) psiFile);
        final String packageName = JSResolveUtil.getPackageName(jsClass);

        final VirtualFile testFolder = getTestFolderFromCurrentModule(module);
        Runnable runnable = new Runnable(){
            public void run(){
                if (testFolder != null){
                    try{

                        VirtualFile childDirectory = testFolder.findChild(packageName);
                        if (childDirectory == null){
                            childDirectory = testFolder.createChildDirectory(project, packageName);
                        }

                        String testFileName = psiFile.getVirtualFile().getNameWithoutExtension() + "Test";
                        PsiDirectory psiDirectory = PsiManagerImpl.getInstance(project).findDirectory(childDirectory);
                        PsiFile testFile = psiDirectory.findFile(testFileName + ".as");
                        if(testFile == null){
                            testFile = (PsiFile) CreateClassOrInterfaceAction.createClass(testFileName, packageName, psiDirectory, "Test ActionScript Class");
                        }

                        FileEditorManager.getInstance(project).openFile(testFile.getVirtualFile(), true);
                    }
                    catch (IOException e1){
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    catch (Exception e1){
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        };

        ApplicationManager.getApplication().runWriteAction(runnable);

    }

    protected VirtualFile getTestFolderFromCurrentModule(Module module){
        VirtualFile testFolder = null;
        final ContentEntry[] contentEntries = ModuleRootManager.getInstance(module).getContentEntries();
        for (ContentEntry contentEntry : contentEntries){
            final SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
            for (SourceFolder sourceFolder : sourceFolders){
                if (sourceFolder.isTestSource()){
                    testFolder = sourceFolder.getFile();
                }
            }
        }
        return testFolder;
    }
}
