package runnables;

import com.intellij.lang.javascript.JavascriptLanguage;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;

/**
 * User: John Lindquist
 * Date: 6/9/11
 * Time: 2:13 PM
 */
public class CreateMediatorRunnable implements Runnable, Computable<Object>
{
    private PsiDirectory directory;
    private String className;

    public CreateMediatorRunnable(PsiDirectory directory, String className)
    {
        this.directory = directory;
        this.className = className;
    }

    public Object compute()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void run()
    {
        PsiFileFactory fileFactory = PsiFileFactory.getInstance(directory.getProject());
        PsiFile psiFile = fileFactory.createFileFromText(className, JavascriptLanguage.INSTANCE, "");
        directory.add(psiFile);
    }
}
