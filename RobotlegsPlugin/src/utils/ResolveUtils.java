package utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

/**
 * User: John Lindquist
 * Date: 6/23/11
 * Time: 5:46 PM
 */
public class ResolveUtils
{
    public static PsiElement resolveElement(PsiElement child)
    {
        PsiElement value;
        if (child instanceof PsiReference)
        {
            value = ((PsiReference) child).resolve();
        }
        else
        {
            value = child;
        }
        return value;
    }
}
