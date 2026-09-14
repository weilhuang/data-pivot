package com.data.pivot.plugin.config.trigger;

import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.model.DataPivotTrigger;
import com.data.pivot.plugin.tool.DatabaseUtil;

import java.util.List;

/**
 * Previously opened {@code DriverManager} connections for every data source at startup.
 * Query/Analysis now use {@link com.data.pivot.plugin.tool.QueryTool} pools only; this
 * trigger only clears leftover entries if an older wiring still invokes it.
 */
public class DatabaseConnectionMapperTrigger implements DataPivotTrigger<DataPivotDatabaseInfo> {
    @Override
    public void load(List<DataPivotDatabaseInfo> dataPivotDatabaseInfoList) {
        DatabaseUtil.closeConnections();
        DataPivotApplication.getInstance().MAPPER.DR_DATABASE_CONNECTION_MAPPER.clear();
    }
}
