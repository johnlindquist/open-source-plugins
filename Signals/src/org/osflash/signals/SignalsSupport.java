package org.osflash.signals;

import com.intellij.codeInsight.hint.EditorHintListener;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.ui.LightweightHint;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * User: John Lindquist
 * Date: 7/4/11
 * Time: 9:30 PM
 */
public class SignalsSupport implements ProjectComponent{

    @Override public void initComponent(){
        final ParameterInfoComponentAdapter fakeParameterInfoComponent = new ParameterInfoComponentAdapter();

        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        bus.connect().subscribe(EditorHintListener.TOPIC, new EditorHintListener(){
            public void hintShown(final Project project, final LightweightHint hint, final int flags){

                JComponent component = hint.getComponent();
                Class<? extends JComponent> aClass = component.getClass();

                if (aClass.getName().equals("com.intellij.codeInsight.hint.ParameterInfoComponent")){
                    fakeParameterInfoComponent.setComponent((JPanel) component);
                    Object[] objects = fakeParameterInfoComponent.getObjects();
                    if (objects[0] instanceof JSFunction){
                        JSFunction jsFunctionObject = (JSFunction) objects[0];

                        if (jsFunctionObject.getParameterList().getText().equals("(...valueObjects)")){
                            PsiElement psiElement = fakeParameterInfoComponent.getMyParameterOwner();

                            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                            JSCallExpression jsCallExpression = SignalsUtils.getCallExpressionFromCaret(editor, file);

                            JSArgumentList argumentList = jsCallExpression.getArgumentList();
                            PsiReference reference = argumentList.getPrevSibling().getFirstChild().getReference();
                            JSArgumentList signalParams = SignalsUtils.getStringParametersFromSignalReference(reference);

                            JSFunction jsFunction = (JSFunction) JSChangeUtil.createJSTreeFromText(psiElement.getProject(), "function foo" + signalParams.getText(), JavaScriptSupportLoader.ECMA_SCRIPT_L4).getPsi();
                            fakeParameterInfoComponent.setMyObjects(new Object[]{jsFunction});
                            fakeParameterInfoComponent.update();
                        }
                    }
                }
            }
        });

    }

    @Override public void disposeComponent(){
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull @Override public String getComponentName(){
        return "SignalsSupport";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void projectOpened(){
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void projectClosed(){
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private class ParameterInfoComponentAdapter{
        private JPanel component;
        private Method getObjectsMethod;
        private Method updateMethod;
        private Field myParameterOwnerField;
        private Field myObjectsField;

        private ParameterInfoComponentAdapter(){
            Class<?> aClass = null;
            try{
                aClass = Class.forName("com.intellij.codeInsight.hint.ParameterInfoComponent");

                getObjectsMethod = aClass.getDeclaredMethod("getObjects");
                updateMethod = aClass.getDeclaredMethod("update");
                myObjectsField = aClass.getDeclaredField("a");
                myParameterOwnerField = aClass.getDeclaredField("c");

                getObjectsMethod.setAccessible(true);
                updateMethod.setAccessible(true);
                myParameterOwnerField.setAccessible(true);
                myObjectsField.setAccessible(true);
            }
            catch (ClassNotFoundException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            catch (NoSuchMethodException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            catch (NoSuchFieldException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


        }


        public void setComponent(JPanel component){
            this.component = component;
        }

        public Object[] getObjects(){
            try{
                return (Object[]) getObjectsMethod.invoke(component);
            }
            catch (IllegalAccessException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            catch (InvocationTargetException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            return null;
        }

        public PsiElement getMyParameterOwner(){
            try{
                return (PsiElement) myParameterOwnerField.get(component);
            }
            catch (IllegalAccessException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            return null;
        }

        public void setMyObjects(Object[] objects){
            try{
                myObjectsField.set(component, objects);
            }
            catch (IllegalAccessException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        public void update(){
            try{
                updateMethod.invoke(component);
            }
            catch (IllegalAccessException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            catch (InvocationTargetException e){
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}
