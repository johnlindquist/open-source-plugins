package org.robotlegs.toolwindows;

import com.intellij.psi.PsiNamedElement;
import com.intellij.usages.UsageInfo2UsageAdapter;

import java.util.Vector;

/**
 * User: John Lindquist
 * Date: 6/20/11
 * Time: 4:39 PM
 */
public class UsageMapping
{
    private UsageInfo2UsageAdapter usage;

    public UsageInfo2UsageAdapter getUsage()
    {
        return usage;
    }

    public Vector<PsiNamedElement> getMappedElements()
    {
        return mappedElements;
    }

    private Vector<PsiNamedElement> mappedElements;

    public UsageMapping(UsageInfo2UsageAdapter usage)
    {
        this.usage = usage;
        mappedElements = new Vector<PsiNamedElement>();
    }

    public void add(PsiNamedElement mappedElement)
    {
        mappedElements.add(mappedElement);
    }
}
