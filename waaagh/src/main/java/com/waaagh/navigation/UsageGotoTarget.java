package com.waaagh.navigation;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Objects;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A call-site entry of the Ctrl+B chooser ("Go to Declaration or Usages").
 * <p>
 * A bare usage element (a reference expression, a method call, ...) has no navigation presentation:
 * the chooser would render it as its raw source text, without an icon and without a container /
 * location string. The chooser builds every row from the element itself
 * ({@link NavigationItem#getPresentation()} text, {@link PsiElement#getIcon(int)} icon, containing
 * file for the module line), so this wrapper carries the presentation of the caller: the code line,
 * the caller's icon and an {@code in Class.member, File.java:42} location — while still navigating
 * exactly to the usage.
 */
final class UsageGotoTarget extends FakePsiElement implements NavigationItem {

    private final PsiElement usage;

    /** Source line of the usage (or the usage text when the line cannot be read). */
    private final String text;

    /** {@code in Class.member, File.java:42}; {@code null} when nothing could be collected. */
    @Nullable
    private final String location;

    @Nullable
    private final Icon icon;

    private UsageGotoTarget(@NotNull PsiElement usage,
                            @NotNull String text,
                            @Nullable String location,
                            @Nullable Icon icon) {
        this.usage = usage;
        this.text = text;
        this.location = location;
        this.icon = icon;
    }

    static @NotNull UsageGotoTarget of(@NotNull PsiElement usage) {
        PsiMember caller = PsiTreeUtil.getParentOfType(usage, PsiMember.class, false);
        return new UsageGotoTarget(usage, lineText(usage), locationText(usage, caller), icon(usage, caller));
    }

    @NotNull
    PsiElement getUsage() {
        return usage;
    }

    private static @NotNull String lineText(@NotNull PsiElement usage) {
        String elementText = usage.getText();
        if (elementText == null) {
            elementText = "";
        }
        PsiFile file = containingFile(usage);
        if (file == null) {
            return elementText;
        }
        Document document = PsiDocumentManager.getInstance(usage.getProject()).getDocument(file);
        TextRange range = usage.getTextRange();
        if (document == null || range == null) {
            return elementText;
        }
        int line = document.getLineNumber(range.getStartOffset());
        String lineText = document.getText(
            TextRange.create(document.getLineStartOffset(line), document.getLineEndOffset(line))).trim();
        return lineText.isEmpty() ? elementText : lineText;
    }

    @NotNull
    private static String locationText(@NotNull PsiElement usage, @Nullable PsiMember caller) {
        String container = callerName(caller);
        String position = position(usage);
        if (container == null) {
            return position == null ? "" : position;
        }
        return position == null ? "in " + container : "in " + container + ", " + position;
    }

    @Nullable
    private static String callerName(@Nullable PsiMember caller) {
        if (caller == null) {
            return null;
        }
        PsiClass containingClass = caller.getContainingClass();
        String memberName = caller.getName();
        if (containingClass == null || memberName == null) {
            return memberName;
        }
        String className = containingClass.getName();
        return className == null ? memberName : className + "." + memberName;
    }

    @Nullable
    private static String position(@NotNull PsiElement usage) {
        PsiFile file = containingFile(usage);
        if (file == null) {
            return null;
        }
        String fileName = file.getName();
        Document document = PsiDocumentManager.getInstance(usage.getProject()).getDocument(file);
        TextRange range = usage.getTextRange();
        if (document == null || range == null) {
            return fileName;
        }
        return fileName + ":" + (document.getLineNumber(range.getStartOffset()) + 1);
    }

    @Nullable
    private static Icon icon(@NotNull PsiElement usage, @Nullable PsiMember caller) {
        Icon callerIcon = caller == null ? null : caller.getIcon(0);
        if (callerIcon != null) {
            return callerIcon;
        }
        PsiFile file = containingFile(usage);
        return file == null ? null : file.getIcon(0);
    }

    @Nullable
    private static PsiFile containingFile(@NotNull PsiElement usage) {
        return usage.isValid() ? usage.getContainingFile() : null;
    }

    // ------------------------------------------------------------------
    // ItemPresentation / NavigationItem
    // ------------------------------------------------------------------

    @Override
    public String getName() {
        return text;
    }

    @Override
    public String getPresentableText() {
        return text;
    }

    @Override
    public String getLocationString() {
        // The chooser wraps a non-null location in " (...)", so keep it empty instead of "".
        return location == null || location.isEmpty() ? null : location;
    }

    @Override
    public Icon getIcon(boolean unused) {
        return icon;
    }

    @Override
    public Icon getIcon(int flags) {
        return icon;
    }

    // ------------------------------------------------------------------
    // PsiElement
    // ------------------------------------------------------------------

    @Override
    public PsiElement getParent() {
        return usage.getParent();
    }

    @Override
    public PsiFile getContainingFile() {
        return containingFile(usage);
    }

    @Override
    public Project getProject() {
        return usage.getProject();
    }

    @Override
    public PsiManager getManager() {
        return PsiManager.getInstance(usage.getProject());
    }

    @Override
    public PsiElement getNavigationElement() {
        return usage;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public TextRange getTextRange() {
        return usage.isValid() ? usage.getTextRange() : null;
    }

    @Override
    public int getTextOffset() {
        TextRange range = getTextRange();
        return range == null ? 0 : range.getStartOffset();
    }

    @Override
    public boolean isValid() {
        return usage.isValid();
    }

    // ------------------------------------------------------------------
    // Navigatable
    // ------------------------------------------------------------------

    @Override
    public boolean canNavigate() {
        Navigatable target = descriptor();
        return target != null && target.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        Navigatable target = descriptor();
        return target != null && target.canNavigateToSource();
    }

    @Override
    public void navigate(boolean requestFocus) {
        Navigatable target = descriptor();
        if (target != null) {
            target.navigate(requestFocus);
        }
    }

    @Nullable
    private Navigatable descriptor() {
        if (!usage.isValid()) {
            return null;
        }
        return usage instanceof Navigatable ? (Navigatable) usage : EditSourceUtil.getDescriptor(usage);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UsageGotoTarget && usage.equals(((UsageGotoTarget) other).usage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(usage);
    }
}
