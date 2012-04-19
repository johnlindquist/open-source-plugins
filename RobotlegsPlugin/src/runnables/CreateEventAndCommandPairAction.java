package runnables;

import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * User: John Lindquist
 * Date: 6/9/11
 * Time: 4:32 PM
 */
public class CreateEventAndCommandPairAction extends CreateElementActionBase
{
    @NotNull @Override protected PsiElement[] invokeDialog(Project project, PsiDirectory psiDirectory)
    {
        Messages.showChooseDialog(project, "choose something", "choose title", Messages.getInformationIcon(), new String[1], "Hello");
        return PsiElement.EMPTY_ARRAY;
    }

    protected void checkBeforeCreate(String s, PsiDirectory psiDirectory) throws IncorrectOperationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull @Override protected PsiElement[] create(String s, PsiDirectory psiDirectory) throws Exception
    {
        return new PsiElement[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override protected String getErrorTitle()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override protected String getCommandName()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override protected String getActionName(PsiDirectory psiDirectory, String s)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
