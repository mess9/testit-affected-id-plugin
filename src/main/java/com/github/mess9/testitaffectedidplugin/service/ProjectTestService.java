package com.github.mess9.testitaffectedidplugin.service;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ProjectTestService {

	private final Project project;

	public ProjectTestService(Project project) {
		this.project = project;
	}

	public Map<String, PsiMethod> collectProjectTests() {
		Map<String, PsiMethod> result = new HashMap<>();
		var annotations = Arrays.stream(TestAnnotation.values()).map(TestAnnotation::getAnnotation).toList();

		PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(project);
		GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

		for (String className : shortNamesCache.getAllClassNames()) {
			PsiClass[] classes = shortNamesCache.getClassesByName(className, scope);

			for (PsiClass psiClass : classes) {
				for (PsiMethod method : psiClass.getMethods()) {
					PsiModifierList modifiers = method.getModifierList();

					for (PsiAnnotation annotation : modifiers.getAnnotations()) {
						String fqn = annotation.getQualifiedName();
						if (fqn != null && annotations.contains(fqn)) {
							String classFqcn = psiClass.getQualifiedName();
							if (classFqcn == null) continue;

							String externalId = classFqcn + "." + method.getName();
							result.put(externalId, method);
							break;
						}
					}
				}
			}
		}
		return result;
	}
}
