import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;

/**
 * User: John Lindquist
 * Date: 6/8/11
 * Time: 10:21 PM
 */
public class CreateMediatorAction extends AnAction
{


    public void actionPerformed(AnActionEvent e)
    {
        PsiFile psiFile = e.getData(DataKeys.PSI_FILE);
        if (psiFile instanceof JSFile)
        {
            JSFile jsFile = (JSFile) psiFile;
            JSClass jsClass = JSPsiImplUtils.findClass(jsFile);
            //Are there null object patterns in place, or just a lot of null checks when searching the psi?
            if (jsClass != null)
            {
                //What's the easiest way to check if the class "is a flash.display.DisplayObjectContainer"?
                if (isDisplayObjectContainer(jsClass))
                {
                    String className = jsClass.getName() + "Mediator.as";
                    PsiDirectory directory = jsFile.getContainingDirectory();

                    //This works, but throws "Assertion failed: Write access is allowed inside write-action only (see com.intellij.openapi.application.Application.runWriteAction()) "?
                    PsiFile file = directory.createFile(className);
                    //TODO: How do a I create a psi from scratch? Or should I just use a template?
                }
                else
                {
                    //Can I hide the action from the menu instead of showing the message?
                    Messages.showMessageDialog("You can only create a Mediator from a DisplayObjectContainer", "Just fyi", Messages.getInformationIcon());
                }
            }
        }

    }

    private Boolean isDisplayObjectContainer(JSClass jsClass)
    {
        JSClass[] superClasses = jsClass.getSuperClasses();
        //Should this be a visitor or some other approach to check all the super classes?
        if (superClasses != null)
        {
            for (JSClass superClass : superClasses)
            {
                String superClassQualifiedName = superClass.getQualifiedName();
                if (superClassQualifiedName.equals("flash.display.DisplayObjectContainer"))
                {
                    return true;
                }
                else
                {
                    return isDisplayObjectContainer(superClass);
                }
            }
        }

        return false;
    }
}
