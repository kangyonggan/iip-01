import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author kangyonggan
 * @since 7/5/18
 */
public class Hello extends AnAction {

    private BaseHandler baseHandler;

    @Override
    public void actionPerformed(AnActionEvent event) {
        baseHandler = new BaseHandler();
        final Project project = event.getProject();
        if (project == null) {
            return;
        }

        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        if (psiDocumentManager.hasUncommitedDocuments()) {
            psiDocumentManager.commitAllDocuments();
        }

        final DataContext dataContext = event.getDataContext();
        final Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);

        if (null != editor) {
            final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
            if (null != psiFile) {
                final PsiClass targetClass = getTargetClass(editor, psiFile);
                if (null != targetClass) {
                    process(project, psiFile, targetClass);
                }
            }
        } else {
            final VirtualFile[] files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
            if (null != files) {
                for (VirtualFile file : files) {
                    if (file.isDirectory()) {
                        // TODO
                        System.out.println("======2");
                    } else {
                        // TODO
                        System.out.println("======3");
                    }
                }
            }
        }
    }

    @Nullable
    private PsiClass getTargetClass(Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        }
        final PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        return target instanceof SyntheticElement ? null : target;
    }

    protected void process(@NotNull final Project project, @NotNull final PsiFile psiFile, @NotNull final PsiClass psiClass) {
        executeCommand(project, new Runnable() {
            @Override
            public void run() {
                baseHandler.invoke(project, psiFile, psiClass);
            }
        });
    }

    private void executeCommand(final Project project, final Runnable action) {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(action);
            }
        }, getCommandName(), null);
    }

    private String getCommandName() {
        String text = getTemplatePresentation().getText();
        return text == null ? "" : text;
    }
}
