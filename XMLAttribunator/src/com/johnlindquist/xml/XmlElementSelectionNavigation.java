package com.johnlindquist.xml;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.impl.source.xml.XmlTokenImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTokenType;

/**
 * User: John Lindquist
 * Date: 7/22/11
 * Time: 1:56 PM
 */
public class XmlElementSelectionNavigation{
    public static final String RIGHT = "right";
    public static final String LEFT = "left";
    private TextRange nextRange;

    public XmlElementSelectionNavigation(Editor editor, String direction){
        PsiElement element = PsiUtil.getElementAtCaret(editor);
        CaretModel caretModel = editor.getCaretModel();

        XmlTokenImpl token = (XmlTokenImpl) element;
        nextRange = getRangeFromToken(token, caretModel, direction);
        int startOffset = nextRange.getStartOffset();
        caretModel.moveToOffset(startOffset);
        SelectionModel selectionModel = editor.getSelectionModel();
        selectionModel.setSelection(startOffset, nextRange.getEndOffset());

        new CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(editor.getProject(), editor);
    }

    private TextRange getRangeFromToken(XmlTokenImpl token, CaretModel caretModel, String direction){
        XmlAttributeImpl attribute = getAttributeFromToken(token);

        if (RIGHT.equals(direction)){
            //account for namespace
            if (token.getTokenType() == XmlTokenType.XML_NAME){
                //special case for namespace
                if (attribute.isNamespaceDeclaration()){

                    String namespacePrefix = attribute.getNamespacePrefix();
                    String localName = attribute.getLocalName();
                    int textOffset = attribute.getFirstChild().getTextOffset();
                    int localNameOffset = textOffset + namespacePrefix.length() + 1;

                    //the caret is on the prefix, so go to the local name
                    if (caretModel.getOffset() < localNameOffset){
                        return new TextRange(localNameOffset, localNameOffset + localName.length());
                    }

                    PsiElement attributeValue = attribute.getLastChild().getChildren()[1];
                    return attributeValue.getTextRange();
                }
                //just a plain attribute (not a namespace)
                else{
                    //the caret is either on the namespace or it's not a namespace, so go to the value
                    PsiElement attributeValue = attribute.getLastChild();
                    PsiElement valueToken = attributeValue.getChildren()[1];
                    return valueToken.getTextRange();
                }
            }
            //the caret is on the attribute value
            else if (token.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN){
                //Don't forget the whitespace!
                PsiElement whiteSpace = attribute.getNextSibling();
                attribute = (XmlAttributeImpl) whiteSpace.getNextSibling();
                //you've moved to the attribute name of the next attribute
                if (attribute.isNamespaceDeclaration()){
                    String namespacePrefix = attribute.getNamespacePrefix();
                    int textOffset = attribute.getFirstChild().getTextOffset();
                    int localNameOffset = textOffset + namespacePrefix.length() + 1;

                    //time to select the prefix
                    if (caretModel.getOffset() < localNameOffset){
                        return new TextRange(textOffset, textOffset + namespacePrefix.length());
                    }
                }

                //just return the attribute name
                return attribute.getFirstChild().getTextRange();
            }

        }
        else if (LEFT.equals(direction)){
            //special case for namespace
            if (attribute.isNamespaceDeclaration()){
                String namespacePrefix = attribute.getNamespacePrefix();
                String localName = attribute.getLocalName();
                int textOffset = attribute.getFirstChild().getTextOffset();
                int localNameOffset = textOffset + namespacePrefix.length() + 1;

                //the caret is on the prefix, so go to the local name
                if (caretModel.getOffset() < localNameOffset){
                    return new TextRange(localNameOffset, localNameOffset + localName.length());
                }
            }
        }


        return token.getTextRange();
    }

    private XmlAttributeImpl getAttributeFromToken(XmlTokenImpl token){
        PsiElement context = token.getContext();
        IElementType contextElementType = context.getNode().getElementType();

        if (contextElementType == XmlElementType.XML_ATTRIBUTE_VALUE){
            context = context.getContext(); //move up from the value to the attribute
        }

        return (XmlAttributeImpl) context;
    }

    public TextRange getNextRange(){
        return nextRange;
    }

}
