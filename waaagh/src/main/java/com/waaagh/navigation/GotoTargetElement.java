package com.waaagh.navigation;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Objects;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A chooser entry of the Ctrl+B popup ("Go to Declaration or Usages").
 * <p>
 * The chooser builds every row from the element itself: the text comes from
 * {@link NavigationItem#getPresentation()}, the icon from {@link PsiElement#getIcon(int)} and the
 * module line from the containing file. Raw handler results do not always fit that contract — a bare
 * usage element (a reference expression) has no presentation at all and a mapped method always shows
 * its Java icon. This wrapper supplies both:
 * <ul>
 *   <li>mapping targets ({@link #forMapping}) — the Feign/Controller method with its native
 *       signature, but presented with the WAAAGH jump icon of its side (orange Feign / blue
 *       Controller);</li>
 *   <li>call sites ({@link #forUsage}) — the caller's code line, icon and
 *       {@code in Class.member, File.java:42} location (the native "Show Usages" look).</li>
 * </ul>
 * In both cases navigation still lands exactly on the wrapped element.
 */
final class GotoTargetElement extends FakePsiElement implements NavigationItem, ColoredItemPresentation {

    /** The element the chooser item navigates to (a mapped method or a call site). */
    private final PsiElement target;

    /** Short name for legacy renderers and speed search. */
    private final String name;

    /** Row text: the method signature for mapping targets, the source line for call sites. */
    private final String text;

    /** {@code (in Class)} / {@code in Class.member, File.java:42}; {@code null} when unknown. */
    @Nullable
    private final String location;

    @Nullable
    private final Icon icon;

    /** Preserves the deprecation colouring of the wrapped element, when it has one. */
    @Nullable
    private final TextAttributesKey attributesKey;

    private GotoTargetElement(@NotNull PsiElement target,
                              @NotNull String name,
                              @NotNull String text,
                              @Nullable String location,
                              @Nullable Icon icon,
                              @Nullable TextAttributesKey attributesKey) {
        this.target = target;
        this.name = name;
        this.text = text;
        this.location = location;
        this.icon = icon;
        this.attributesKey = attributesKey;
    }

    /**
     * A matched Feign / Controller method, presented with the WAAAGH jump icon of the other side
     * (the signature and the location stay the native Java ones).
     */
    static @NotNull GotoTargetElement forMapping(@NotNull PsiMethod method, @NotNull Icon icon) {
        String name = method.getName();
        ItemPresentation presentation = method.getPresentation();
        if (presentation == null) {
            return new GotoTargetElement(method, name, name, null, icon, null);
        }
        String text = presentation.getPresentableText();
        TextAttributesKey attributesKey = presentation instanceof ColoredItemPresentation
                ? ((ColoredItemPresentation) presentation).getTextAttributesKey()
                : null;
        return new GotoTargetElement(method, name, text == null ? name : text,
                presentation.getLocationString(), icon, attributesKey);
    }

    /** A call site, presented as the caller's code line with the caller's icon and location. */
    static @NotNull GotoTargetElement forUsage(@NotNull PsiElement usage) {
        PsiMember caller = PsiTreeUtil.getParentOfType(usage, PsiMember.class, false);
        String line = lineText(usage);
        return new GotoTargetElement(usage, line, line, locationText(usage, caller),
                icon(usage, caller), null);
    }

    @NotNull
    PsiElement getTarget() {
        return target;
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
    private static PsiFile containingFile(@NotNull PsiElement element) {
        return element.isValid() ? element.getContainingFile() : null;
    }

    // ------------------------------------------------------------------
    // ItemPresentation / NavigationItem
    // ------------------------------------------------------------------

    @Override
    public String getName() {
        return name;
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
    public TextAttributesKey getTextAttributesKey() {
        return attributesKey;
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
        return target.getParent();
    }

    @Override
    public PsiFile getContainingFile() {
        return containingFile(target);
    }

    @Override
    public Project getProject() {
        return target.getProject();
    }

    @Override
    public PsiManager getManager() {
        return PsiManager.getInstance(target.getProject());
    }

    @Override
    public PsiElement getNavigationElement() {
        return target;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public TextRange getTextRange() {
        return target.isValid() ? target.getTextRange() : null;
    }

    @Override
    public int getTextOffset() {
        TextRange range = getTextRange();
        return range == null ? 0 : range.getStartOffset();
    }

    @Override
    public boolean isValid() {
        return target.isValid();
    }

    // ------------------------------------------------------------------
    // Navigatable
    // ------------------------------------------------------------------

    @Override
    public boolean canNavigate() {
        Navigatable descriptor = descriptor();
        return descriptor != null && descriptor.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        Navigatable descriptor = descriptor();
        return descriptor != null && descriptor.canNavigateToSource();
    }

    @Override
    public void navigate(boolean requestFocus) {
        Navigatable descriptor = descriptor();
        if (descriptor != null) {
            descriptor.navigate(requestFocus);
        }
    }

    @Nullable
    private Navigatable descriptor() {
        if (!target.isValid()) {
            return null;
        }
        return target instanceof Navigatable ? (Navigatable) target : EditSourceUtil.getDescriptor(target);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GotoTargetElement && target.equals(((GotoTargetElement) other).target);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(target);
    }
}
