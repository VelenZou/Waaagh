package com.waaagh.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

public class ProjectUtils {

  /**
   * 获取所有打开的项目列表
   *
   * @return {@link Project[]}
   */
  public static Project[] getOpenProjects() {
    return ProjectManager.getInstance().getOpenProjects();
  }

  /**
   * 检查元素是否是纯粹的业务文件，而非三方源码
   **/
  public static boolean isBizElement(PsiElement element) {
    if (element == null) {
      return false;
    }

    // 检查文件类型
    if (element.getContainingFile() == null) {
      return false;
    }

    // 所属文件
    VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
    if (virtualFile == null) {
      return false;
    }

    // 首先检查是否是 Java 文件。
    String fileName = virtualFile.getName();
    if (!fileName.endsWith(".java")) {
      return false;
    }

    // 然后排除三方包中的文件
    Project project = element.getProject();

    ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);

    if (projectFileIndex.isInLibrary(virtualFile)) {
      return false;
    }

    return projectFileIndex.isInSourceContent(virtualFile);
  }

}
