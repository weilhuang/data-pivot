package com.data.pivot.plugin.context;

import com.data.pivot.plugin.config.trigger.ORMSettingMapperTrigger;
import com.data.pivot.plugin.config.trigger.ROMSettingMapperTrigger;
import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.model.DataPivotCacheContainer;
import com.data.pivot.plugin.tool.MessageUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataPivotMappingSettingInfoCacheContainer extends DataPivotCacheContainer<DataPivotMappingSettingInfo> {
    public DataPivotMappingSettingInfoCacheContainer() {
        super(new ORMSettingMapperTrigger(),new ROMSettingMapperTrigger());
    }
    @Override
    protected List<DataPivotMappingSettingInfo> init(Project project) {
        List<DataPivotMappingSettingInfo> query = new DataPivotMappingSettingInfo().query();
        List<DataPivotMappingSettingInfo> result = new ArrayList<>();
        //切换项目时 uuid 更新
        Map<String, DataPivotDatabaseInfo> dpDrDatabaseMapper =
                DataPivotApplication.getInstance(project).MAPPER.DP_DR_DATABASE_MAPPER;
        Map<String, DataPivotDatabaseInfo> dpDpDatabaseMapper =
                DataPivotApplication.getInstance(project).MAPPER.DP_DP_DATABASE_MAPPER;
        for (DataPivotMappingSettingInfo dataPivotMappingSettingInfo : query) {
            if (dpDrDatabaseMapper.get(dataPivotMappingSettingInfo.getDatabaseReference()) == null) {
                DataPivotDatabaseInfo dataPivotDatabaseInfo = dpDpDatabaseMapper.get(dataPivotMappingSettingInfo.getDatabasePath());
                if (dataPivotDatabaseInfo!=null) {
                    dataPivotMappingSettingInfo.setDatabaseReference(dataPivotDatabaseInfo.getDatabaseReference());
                    dataPivotMappingSettingInfo.setDatabasePath(dataPivotDatabaseInfo.getDatabasePath());
                    dataPivotMappingSettingInfo.setDataSourceName(dataPivotDatabaseInfo.getDataSourceName());
                    dataPivotMappingSettingInfo.setDatabaseName(dataPivotDatabaseInfo.getDatabaseName());
                    result.add(dataPivotMappingSettingInfo);
                }
            }else {
                result.add(dataPivotMappingSettingInfo);
            }
        }
        //更新
        //new DataPivotMappingSettingInfo().save(new ArrayList<>());
        new DataPivotMappingSettingInfo().save(result);
//        if (result == null || result.isEmpty()) {
//            MessageUtil.Notice.infoAction(DataPivotBundle.message("data.pivot.notice.title"),
//                    DataPivotBundle.message("data.pivot.notice.setting.null.content"),
//                    new NotificationAction(DataPivotBundle.message("data.pivot.notice.setting.null.action")) {
//                        @Override
//                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
//                            ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), DataPivotConstants.DATA_PIVOT_MAIN_SETTING);
//                            notification.expire();
//                        }
//                    });
//            return new ArrayList<>();
//        } else {
//            return result;
//        }
        return result;
    }

}
