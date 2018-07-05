import com.intellij.conversion.ProjectSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author kangyonggan
 * @since 7/5/18
 */
public class BaseHandler {

    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiClass psiClass) {
        if (psiFile.isWritable()) {
            invoke(project, psiClass, false);
        }
    }

    private void invoke(Project project, PsiClass psiClass, boolean processInnerClasses) {
        Collection<PsiAnnotation> processedAnnotations = new HashSet<>();

        processedAnnotations.addAll(processClass(project, psiClass));

        PsiElement psiElement = rebuildPsiElement(project, psiClass);

        // 加上这个psi元素，就可以提示getter了
        psiClass.add(psiElement);

//        deleteAnnotations(processedAnnotations);
    }


    private PsiElement rebuildPsiElement(@NotNull Project project, PsiElement psiElement) {
        if (psiElement instanceof PsiMethod) {
            return rebuildMethod(project, (PsiMethod) psiElement);
        } else if (psiElement instanceof PsiField) {
            return rebuildField(project, (PsiField) psiElement);
        } else if (psiElement instanceof PsiClass) {
            return rebuildClass(project, (PsiClass) psiElement);
        }
        return null;
    }

    private PsiField rebuildField(@NotNull Project project, @NotNull PsiField fromField) {
        final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);

        final PsiField resultField = elementFactory.createField(fromField.getName(), fromField.getType());
        copyModifiers(fromField.getModifierList(), resultField.getModifierList());
        resultField.setInitializer(fromField.getInitializer());

        return (PsiField) CodeStyleManager.getInstance(project).reformat(resultField);
    }

    private PsiClass rebuildClass(@NotNull Project project, @NotNull PsiClass fromClass) {
        final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);

        PsiClass resultClass = elementFactory.createClass(StringUtil.defaultIfEmpty(fromClass.getName(), "UnknownClassName"));
        copyModifiers(fromClass.getModifierList(), resultClass.getModifierList());
        rebuildTypeParameter(fromClass, resultClass);

        for (PsiField psiField : fromClass.getFields()) {
            resultClass.add(rebuildField(project, psiField));
        }
        for (PsiMethod psiMethod : fromClass.getMethods()) {
            resultClass.add(rebuildMethod(project, psiMethod));
        }

        return (PsiClass) CodeStyleManager.getInstance(project).reformat(resultClass);
    }


    private PsiMethod rebuildMethod(@NotNull Project project, @NotNull PsiMethod fromMethod) {
        final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);

        final PsiMethod resultMethod;
        final PsiType returnType = fromMethod.getReturnType();
        if (null == returnType) {
            resultMethod = elementFactory.createConstructor(fromMethod.getName());
        } else {
            resultMethod = elementFactory.createMethod(fromMethod.getName(), returnType);
        }

        rebuildTypeParameter(fromMethod, resultMethod);

        final PsiClassType[] referencedTypes = fromMethod.getThrowsList().getReferencedTypes();
        if (referencedTypes.length > 0) {
            PsiJavaCodeReferenceElement[] refs = new PsiJavaCodeReferenceElement[referencedTypes.length];
            for (int i = 0; i < refs.length; i++) {
                refs[i] = elementFactory.createReferenceElementByType(referencedTypes[i]);
            }
            resultMethod.getThrowsList().replace(elementFactory.createReferenceList(refs));
        }

        for (PsiParameter parameter : fromMethod.getParameterList().getParameters()) {
            PsiParameter param = elementFactory.createParameter(parameter.getName(), parameter.getType());
            if (parameter.getModifierList() != null) {
                PsiModifierList modifierList = param.getModifierList();
                for (PsiAnnotation originalAnnotation : parameter.getModifierList().getAnnotations()) {
                    final PsiAnnotation annotation = modifierList.addAnnotation(originalAnnotation.getQualifiedName());
                    for (PsiNameValuePair nameValuePair : originalAnnotation.getParameterList().getAttributes()) {
                        annotation.setDeclaredAttributeValue(nameValuePair.getName(), nameValuePair.getValue());
                    }
                }
            }
            resultMethod.getParameterList().add(param);
        }

        final PsiModifierList fromMethodModifierList = fromMethod.getModifierList();
        final PsiModifierList resultMethodModifierList = resultMethod.getModifierList();
        copyModifiers(fromMethodModifierList, resultMethodModifierList);
        for (PsiAnnotation psiAnnotation : fromMethodModifierList.getAnnotations()) {
            final PsiAnnotation annotation = resultMethodModifierList.addAnnotation(psiAnnotation.getQualifiedName());
            for (PsiNameValuePair nameValuePair : psiAnnotation.getParameterList().getAttributes()) {
                annotation.setDeclaredAttributeValue(nameValuePair.getName(), nameValuePair.getValue());
            }
        }

        PsiCodeBlock body = fromMethod.getBody();
        if (null != body) {
            resultMethod.getBody().replace(body);
        }

        return (PsiMethod) CodeStyleManager.getInstance(project).reformat(resultMethod);
    }

    private void copyModifiers(PsiModifierList fromModifierList, PsiModifierList resultModifierList) {
        for (String modifier : PsiModifier.MODIFIERS) {
            resultModifierList.setModifierProperty(modifier, fromModifierList.hasModifierProperty(modifier));
        }
    }

    private void rebuildTypeParameter(@NotNull PsiTypeParameterListOwner listOwner, @NotNull PsiTypeParameterListOwner resultOwner) {
        final PsiTypeParameterList fromMethodTypeParameterList = listOwner.getTypeParameterList();
        if (listOwner.hasTypeParameters() && null != fromMethodTypeParameterList) {
            PsiTypeParameterList typeParameterList = PsiMethodUtil.createTypeParameterList(fromMethodTypeParameterList);
            if (null != typeParameterList) {
                final PsiTypeParameterList resultOwnerTypeParameterList = resultOwner.getTypeParameterList();
                if (null != resultOwnerTypeParameterList) {
                    resultOwnerTypeParameterList.replace(typeParameterList);
                }
            }
        }
    }


    private Collection<PsiAnnotation> processClass(@NotNull Project project, @NotNull PsiClass psiClass) {
        GetterProcessor getterProcessor = new GetterProcessor();
        Collection<PsiAnnotation> psiAnnotations = getterProcessor.collectProcessedAnnotations(psiClass);

        return psiAnnotations;
    }

    private void deleteAnnotations(Collection<PsiAnnotation> psiAnnotations) {
        for (PsiAnnotation psiAnnotation : psiAnnotations) {
            psiAnnotation.delete();
        }
    }
}
