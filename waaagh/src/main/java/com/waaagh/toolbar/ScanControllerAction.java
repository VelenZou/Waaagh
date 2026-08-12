//package com.waaagh.toolbar;
//
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.project.Project;
//import com.waaagh.entity.HttpMappingInfo;
//import com.waaagh.entity.CustomDialog;
//import com.waaagh.utils.ToolBarUtils;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.List;
//
///**
// * @Description:
// * @Author: lyflexi
// * @project: waaagh
// * @Date: 2024/10/18 16:03
// */
//public class ScanControllerAction extends AnAction {
//
//    @Override
//    public void actionPerformed(@NotNull AnActionEvent event) {
//        Project project = event.getProject();
//        List<HttpMappingInfo> httpMappingInfos = ToolBarUtils.scanAllProjectControllerInfo(project);
//        showControllerInfo(httpMappingInfos);
//
//    }
//    private void showControllerInfo(List<HttpMappingInfo> httpMappingInfos) {
//        CustomDialog dialog = new CustomDialog(httpMappingInfos);
//        dialog.show();
//    }
//}