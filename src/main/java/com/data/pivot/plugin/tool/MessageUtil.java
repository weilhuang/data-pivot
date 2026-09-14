package com.data.pivot.plugin.tool;

import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessageUtil {
    /**
     * 对话
     */
    public static class Dialog{
        public static void info(String msg) {
            info(DataPivotConstants.DATA_PIVOT, msg);
        }
        public static void info(String title,String msg) {
            Messages.showInfoMessage(msg, title);
        }
    }

    /**
     * 提示
     */
    public static class Hint{
        public static void info(Editor editor,String msg){
            HintManager hintManager = ApplicationManager.getApplication().getService(HintManager.class);
            hintManager.showInformationHint(editor,msg,HintManager.ABOVE);
        }
        public static void error(Editor editor,String msg){
            HintManager hintManager = ApplicationManager.getApplication().getService(HintManager.class);
            hintManager.showErrorHint(editor,msg,HintManager.ABOVE);
        }

    }

    /**
     * 通知
     */
    public static class Notice{
        public static void info(String content){
            infoAction(DataPivotBundle.message("data.pivot.notice.info.title"),content,null);
        }
        public static void error(String content){
            errorAction(DataPivotBundle.message("data.pivot.notice.error.title"),content,null);
        }
        public static void infoAction(String content,@NotNull NotificationAction notificationAction){
            infoAction(DataPivotBundle.message("data.pivot.notice.info.title"),content,notificationAction);
        }
        public static void errorAction(String content,@NotNull NotificationAction notificationAction){
            errorAction(DataPivotBundle.message("data.pivot.notice.error.title"),content,notificationAction);
        }
        public static void infoAction(String title, String content,NotificationAction notificationAction){
            Notification notification = new Notification(
                    DataPivotConstants.NOTIFICATION_GROUP_ID,
                    title,
                    content,
                    NotificationType.INFORMATION);
            if (notificationAction != null) {
                notification.addAction(notificationAction);
            }
            Notifications.Bus.notify(notification, ProjectUtils.getCurrProject());
        }
        public static void errorAction(String title, String content,NotificationAction notificationAction){
            Notification notification = new Notification(
                    DataPivotConstants.NOTIFICATION_GROUP_ID,
                    title,
                    content,
                    NotificationType.ERROR);
            if (notificationAction != null) {
                notification.addAction(notificationAction);
            }
            Notifications.Bus.notify(notification, ProjectUtils.getCurrProject());
        }

    }

    public static void mappingError(@Nullable Editor editor, @NotNull String message) {
        if (editor != null) {
            Hint.error(editor, message);
            return;
        }
        Notice.errorAction(message, openSettingsAction());
    }

    public static @NotNull NotificationAction openSettingsAction() {
        return new NotificationAction(DataPivotBundle.message("data.pivot.notice.setting.null.action")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(
                        e.getProject(), DataPivotConstants.DATA_PIVOT_MAIN_SETTING);
                notification.expire();
            }
        };
    }
}
