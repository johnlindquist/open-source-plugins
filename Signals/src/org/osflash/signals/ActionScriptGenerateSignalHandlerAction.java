package org.osflash.signals;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.flex.ImportUtils;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * @author John Lindquist
 */
public class ActionScriptGenerateSignalHandlerAction extends PsiElementBaseIntentionAction{

    public ActionScriptGenerateSignalHandlerAction(){
        setText("Create Signal Handler");
    }

    @NotNull @Override public String getFamilyName(){
        return getText();
    }

    @Override public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException{
        JSCallExpression jsCallExpression = SignalsUtils.getCallExpressionFromCaret(editor, file);
        JSArgumentList argumentList = jsCallExpression.getArgumentList();

        PsiElement signal = SignalsUtils.getSignalFromCallExpression(jsCallExpression);

        JSArgumentList signalParams = SignalsUtils.getStringParametersFromSignalReference(signal.getReference());
        String signalHandlerName = replaceParamsWithArgs(project, argumentList, signal);

        PsiElement psiElement = generateHandlerFromParams(project, (JSFile) file, signalHandlerName, signalParams);
        moveCursorInsideMethod(editor, psiElement);
    }

    private PsiElement generateHandlerFromParams(Project project, JSFile file, String signalHandlerName, JSArgumentList signalParams){
        JSClass jsClass = JSPsiImplUtils.findClass(file);

        importClassesFromParameters(signalParams, jsClass);

        String result = SignalsUtils.buildParametersFromClassParameters(signalParams.getText());


        ASTNode jsTreeFromText = JSChangeUtil.createJSTreeFromText(project, "protected function " + signalHandlerName + result + ":void{" + "" + "\n}\n", JavaScriptSupportLoader.ECMA_SCRIPT_L4);

        final PsiElement functionElement = jsTreeFromText.getPsi();

        return jsClass.add(functionElement);
    }

    private String replaceParamsWithArgs(Project project, JSArgumentList argumentList, PsiElement signal){
        String signalText = signal.getLastChild().getText();
        String signalHandlerName = "on" + signalText.substring(0, 1).toUpperCase() + signalText.substring(1, signalText.length());
        PsiElement signalHandler = JSChangeUtil.createJSTreeFromText(project, "(" + signalHandlerName + ")").getPsi();
        argumentList.replace(signalHandler);
        return signalHandlerName;
    }

    public void importClassesFromParameters(JSArgumentList signalParams, JSClass jsClass){
        JSExpression[] arguments = signalParams.getArguments();
        for (JSExpression argument : arguments){
            PsiElement resolve = ((JSReferenceExpression) argument).resolve();
            String qualifiedName = ((JSClass) resolve).getQualifiedName();
            ImportUtils.importAndShortenReference(qualifiedName, jsClass, true, true);
        }
    }

    public void moveCursorInsideMethod(final Editor editor, final PsiElement addedElement){
        final PsiElement lastElement = PsiTreeUtil.getDeepestLast(addedElement);
        final PsiElement prevElement = lastElement.getPrevSibling();

        final int offset = (prevElement != null ? prevElement : lastElement).getTextOffset();
        final int offsetToNavigate = InjectedLanguageManager.getInstance(addedElement.getProject()).injectedToHost(addedElement, offset);

        if (ApplicationManager.getApplication().isHeadlessEnvironment()){
            editor.getCaretModel().moveToOffset(offsetToNavigate);
        }
        else{
            new OpenFileDescriptor(addedElement.getProject(), addedElement.getContainingFile().getVirtualFile(), offsetToNavigate).navigate(true); // properly contributes to editing history
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
    }

    @Override public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element){
        JSCallExpression jsCallExpression = PsiTreeUtil.getParentOfType(element, JSCallExpression.class);

        if (jsCallExpression == null){
            return false;
        }

        JSExpression methodExpression = jsCallExpression.getMethodExpression();
        if (methodExpression instanceof JSReferenceExpression){
            String referencedName = ((JSReferenceExpression) methodExpression).getReferencedName();
            return "add".equals(referencedName);
        }

        return false;
    }

}
