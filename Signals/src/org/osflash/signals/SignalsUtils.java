package org.osflash.signals;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecmal4.JSSuperExpression;
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: John Lindquist
 * Date: 7/14/11
 * Time: 1:25 PM
 */
public class SignalsUtils{

    /**
     * Takes dispatch(|) and return dispatch() from original class
     *
     * @param editor
     * @param file
     * @return
     */
    public static JSCallExpression getCallExpressionFromCaret(Editor editor, PsiFile file){
        final CaretModel caretModel = editor.getCaretModel();
        PsiElement element = file.findElementAt(caretModel.getOffset());
        return PsiTreeUtil.getParentOfType(element, JSCallExpression.class);
    }

    /**
     * Takes (Array, String) and turns it into (array:Array, string:String)
     *
     * @return
     * @param paramsText
     */
    public static String buildParametersFromClassParameters(String paramsText){
        String result = "";
        String substring = paramsText.substring(1, paramsText.length() - 1);
        String[] split = substring.split(",");

        result = "(";
        for (int i = 0, splitLength = split.length; i < splitLength; i++){
            String s = split[i];
            Matcher m = Pattern.compile("\\w").matcher(s);

            StringBuilder stringBuilder = new StringBuilder();

            m.find();
            int start = m.start();
            stringBuilder.append(s.substring(start, start + 1).toLowerCase());
            stringBuilder.append(s.substring(start + 1));

            result = result + stringBuilder.toString() + ":" + s;
            if (i < split.length - 1){
                result = result + ", ";
            }
        }

        result = result + ")";
        return result;
    }

    public static JSArgumentList getStringParametersFromSignalReference(final PsiReference signal){
        JSType jsType = findJSTypeFromReference(signal);

        JSSuperExpression jsSuperExpression = PsiTreeUtil.findChildOfType(jsType.resolveClass(), JSSuperExpression.class);

        //check for super()
        JSArgumentList argumentsList;
        if (jsSuperExpression != null){
            argumentsList = ((JSCallExpression) jsSuperExpression.getContext()).getArgumentList();
            if (argumentsList != null){
                return argumentsList;
            }
        }

        //if super check fails, continue to check for new Signal()

        FindArgumentsListFromSignal findArgumentsListFromSignal = new FindArgumentsListFromSignal(signal);
        ApplicationManager.getApplication().runReadAction(findArgumentsListFromSignal);

        return findArgumentsListFromSignal.getArgumentsList();
    }

    public static JSType findJSTypeFromReference(PsiReference signal){
        return ((JSVariable) TargetElementUtil.getInstance().getTargetCandidates(signal).iterator().next()).getType();
    }

    public static JSNewExpression findInitializerFromReference(PsiReference reference){
        PsiElement context = reference.getElement().getContext();
        PsiElement superContext = context.getContext();
        if (superContext instanceof JSAssignmentExpression){
            JSExpression rOperand = ((JSAssignmentExpression) superContext).getROperand();
            if (rOperand instanceof JSNewExpression){
                return ((JSNewExpression) rOperand);
            }
        }
        else if (context instanceof JSVariable && ((JSVariable) context).getInitializer() != null){
            return (JSNewExpression) ((JSVariable) context).getInitializer();
        }
        return null;
    }

    static PsiElement getSignalFromCallExpression(JSCallExpression jsCallExpression){
        return jsCallExpression.getFirstChild().getFirstChild();
    }

    private static class FindArgumentsListFromSignal implements Runnable{
        private final PsiReference signal;
        private JSArgumentList argumentsList;

        public FindArgumentsListFromSignal(PsiReference signal){
            this.signal = signal;
        }

        @Override public void run(){
            PsiElement signalElement = signal.getElement();
            ProjectAndLibrariesScope searchScope = new ProjectAndLibrariesScope(signalElement.getProject());
            Collection<PsiReference> references = ReferencesSearch.search(((JSReferenceExpressionImpl) signalElement).resolve(), searchScope, false).findAll();


            for (PsiReference reference : references){
                JSNewExpression jsNewExpression = findInitializerFromReference(reference);
                if (jsNewExpression != null){
                    argumentsList = jsNewExpression.getArgumentList();
                }
            }

        }

        public JSArgumentList getArgumentsList(){
            return argumentsList;
        }
    }
}
