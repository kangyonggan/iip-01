import com.github.ofofs.jca.annotation.Getter;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author kangyonggan
 * @since 7/5/18
 */
public class GetterProcessor {
    public Collection<PsiAnnotation> collectProcessedAnnotations(PsiClass psiClass) {
        Collection<PsiAnnotation> result = new ArrayList<>();
        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiClass, Getter.class);
        if (null != psiAnnotation) {
            result.add(psiAnnotation);
        }
        return result;
    }
}
