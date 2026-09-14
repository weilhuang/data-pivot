package com.data.pivot.plugin.constants;

import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class DataPivotConstants {
    public static final Icon LOGO = IconLoader.getIcon("/icons/logo.svg", DataPivotConstants.class);
    public static String NOTIFICATION_GROUP_ID = "Data Pivot Messages";
    public static String DATA_PIVOT_MAIN_SETTING = "Data-Pivot Configuration";
    public static final String DATA_GRIP = "DataGrip";
    public static final String SLASH = StrUtil.SLASH;
    public static final String DOT = StrUtil.DOT;
    public static final String AT = StrUtil.AT;
    public static final String NUMBER_SIGN = "#";
    public static final String MYSQL = "MYSQL";
    public static final String DATA_PIVOT = "data-pivot";
    public static final String MP_VALUE = "value";
    public static final String JPA_VALUE = "name";
    public static final String DEFAULT_SQL_CODE = "DEFAULT_SQL";
    public static final String SQL_TABLE_CODE = "{tableName}";
    public static final String SQL_COLUMN_CODE = "{columnName}";
    public static final Integer DEFAULT_PAGE_NUM = 1; //页数
    public static final Integer DEFAULT_PAGE_SIZE = 20; //每页查询数量
    /** Legacy MySQL-only analysis template kept for persisted Settings JSON. */
    public static final String DEFAULT_SQL_CONTENT = "SELECT     \n" +
            SQL_COLUMN_CODE +","+
            "    COUNT(*) AS rs_count,    \n" +
            "    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM \n" +
            SQL_TABLE_CODE +
            "    ), 2) AS percentage    \n" +
            "    FROM     \n" +
            SQL_TABLE_CODE +
            "    GROUP BY     \n" +
            SQL_COLUMN_CODE +
            "    ORDER BY     \n" +
            "    rs_count DESC, percentage DESC    \n" +
            "    LIMIT "+DEFAULT_PAGE_SIZE+";";

    public static final String SCRIPT_FUNCTION_NAME = "convert";
    public static final String JS_SCRIPT_CODE = "function convert(inputString) {  \n" +
            "    // 对输入的字符串进行处理  \n" +
            "    var outputString = inputString.toUpperCase();  \n" +
            "      \n" +
            "    // 返回处理后的字符串  \n" +
            "    return outputString;  \n" +
            "}"; //每页查询数量


    //转下划线
    public static final String TO_UNDER_SCORE_SCRIPT = "function convert(str) {  \n" +
            "    return str.replace(/([a-z])([A-Z])/g, '$1_$2').toLowerCase();  \n" +
            "}  ";

    //转大驼峰
    public static final String TO_BIG_CAMEL_CASE_SCRIPT  = "function convert(str) {  \n" +
            "    var camel = str.replace(/_([a-z])/g, function (all, letter) {  \n" +
            "        return letter.toUpperCase();  \n" +
            "    });  \n" +
            "    return camel.charAt(0).toUpperCase() + camel.slice(1);  \n" +
            "}  ";
    //转小驼峰
    public static final String TO_CAMEL_CASE_SCRIPT  = "function convert(str) {  \n" +
            "    return str.replace(/_([a-z])/g, function (all, letter) {  \n" +
            "        return letter.toUpperCase();  \n" +
            "    });  \n" +
            "} ";

}