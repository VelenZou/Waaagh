//package com.waaagh.toolbar;
//
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.project.Project;
//import com.waaagh.cache.BilateralCacheManager;
//import com.waaagh.utils.ToolBarUtils;
//import org.jetbrains.annotations.NotNull;
//
///**
// * @Description:
// * @Author: lyflexi
// * @project: waaagh
// * @Date: 2024/10/18 16:03
// */
//public class RefreshCacheAction extends AnAction {
//
//    private AnActionEvent event;
//
//    @Override
//    public void actionPerformed(@NotNull AnActionEvent event) {
//        this.event = event;
//        Project project = event.getProject();
//        BilateralCacheManager.clear();
//        // 扫描项目中的Java源文件
//        ToolBarUtils.scanAllProjectControllerInfo(project);
//        ToolBarUtils.scanAllProjectFeignInfo(project);
//    }
//
//    public void refresh(){
//        Project project = event.getProject();
//        BilateralCacheManager.clear(project);
//        // 扫描项目中的Java源文件
//        ToolBarUtils.scanAllProjectControllerInfo(project);
//    }
//}