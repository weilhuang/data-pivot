package com.data.pivot.plugin.tool;

import cn.hutool.core.util.StrUtil;
import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.model.DataPivotObject;
import com.data.pivot.plugin.model.DataPivotRelation;
import com.intellij.database.model.DasColumn;
import com.intellij.database.psi.*;
import com.intellij.database.util.DasUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.*;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PsiElementUtil {
    public static @Nullable DataPivotRelation getDataPivotRelation(PsiElement psiElement){
        if (!(psiElement instanceof DbColumn)) return null;
        DataPivotRelation dataPivotRelation = new DataPivotRelation();
        DbColumnImpl dbColumn = (DbColumnImpl) psiElement;
        DbTable dbTable = dbColumn.getParent();
        List<String> columnNameList = new ArrayList<>();
        JBIterable<? extends DasColumn> columns = DasUtil.getColumns(dbTable);
        for (DasColumn column : columns) {
            columnNameList.add(column.getName());
        }
        DbElement dbElement = dbTable.getParent();
        DbDataSource dataSource = dbElement.getDataSource();
        String uniqueId = dataSource.getUniqueId();
        String columnName = dbColumn.getName();
        String tableName = dbTable.getName();
        String dbName = dbElement.getName();
        String databaseReference = DataPivotUtil.createDatabaseReference(uniqueId, dbName);
        dataPivotRelation.setDatabaseReference(databaseReference);
        dataPivotRelation.setUniqueId(uniqueId);
        dataPivotRelation.setDatabaseName(dbName);
        dataPivotRelation.setTableName(tableName);
        dataPivotRelation.setColumnName(columnName);
        dataPivotRelation.setColumnList(columnNameList);
        dataPivotRelation.setDbColumn(dbColumn);
        DataPivotMappingSettingInfo dataPivotMappingSettingInfo = DataPivotUtil.getRelationDataPivotSettingInfo(databaseReference);
        dataPivotRelation.setDataPivotMappingSettingInfo(dataPivotMappingSettingInfo);
        if (dataPivotMappingSettingInfo != null) {
            dataPivotRelation.setDataPivotStrategyInfo(DataPivotApplication.getDataPivotStrategyInfo(dataPivotMappingSettingInfo.getStrategyCode()));
        }
        return dataPivotRelation;
    }
    public static @Nullable DataPivotObject getDataPivotObject(PsiElement psiElement){
        if (!(psiElement instanceof PsiField)) return null;
        PsiField currentField= (PsiField) psiElement;
        PsiClass currentClass = currentField.getContainingClass();
        DataPivotObject dataPivotObject = new DataPivotObject();
        PsiField[] fields = currentClass.getFields();
        String qualifiedName = currentClass.getQualifiedName();
        String packageName = StrUtil.subBefore(qualifiedName, ".", true);
        String className = currentClass.getName();
        PsiFile containingFile = currentClass.getContainingFile();
        Module moduleForFile = ModuleUtil.findModuleForFile(containingFile);
        String modelName = moduleForFile.getName();
        dataPivotObject.setPsiElement(psiElement);
        dataPivotObject.setModule(moduleForFile);
        dataPivotObject.setModuleName(modelName);
        dataPivotObject.setPackageName(packageName);
        dataPivotObject.setCurrentClass(currentClass);
        dataPivotObject.setCurrentPsiField(currentField);
        dataPivotObject.setPsiFieldList(Arrays.asList(fields));
        dataPivotObject.setCurrentClassName(className);
        dataPivotObject.setCurrentFieldName(currentField.getName());
        dataPivotObject.setPackageReference(DataPivotUtil.createPackageReference(modelName,packageName));
        dataPivotObject.setFieldReference(DataPivotUtil.createFieldReference(modelName,packageName,className,currentField.getName()));
        DataPivotMappingSettingInfo dataPivotMappingSettingInfo = DataPivotUtil.getObjectDataPivotSettingInfo(dataPivotObject.getPackageReference());
        dataPivotObject.setDataPivotMappingSettingInfo(dataPivotMappingSettingInfo);
        if (dataPivotMappingSettingInfo != null) {
            dataPivotObject.setDataPivotStrategyInfo(DataPivotApplication.getDataPivotStrategyInfo(dataPivotMappingSettingInfo.getStrategyCode()));
        }
        return dataPivotObject;
    }

    public static @Nullable DataPivotMappingSettingInfo getMappingSetting(PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }
        Module moduleForFile = ModuleUtil.findModuleForPsiElement(psiClass);
        if (moduleForFile == null) {
            return null;
        }
        String qualifiedName = psiClass.getQualifiedName();
        String packageName = qualifiedName == null ? null : StrUtil.subBefore(qualifiedName, ".", true);
        if (StrUtil.isEmpty(packageName)) {
            return null;
        }
        String packageReference = DataPivotUtil.createPackageReference(moduleForFile.getName(), packageName);
        return DataPivotUtil.getObjectDataPivotSettingInfo(packageReference);
    }


    public static List<PsiAnnotation> getAnnotationList(PsiModifierListOwner psiModifierListOwner){
        PsiModifierList modifierList = psiModifierListOwner.getModifierList();
        if (modifierList == null) {
            return new ArrayList<>();
        }
        PsiAnnotation[] annotations = modifierList.getAnnotations();
        return Arrays.asList(annotations);
    }

    public static String identityToString(Object obj){
        return obj.getClass().getName() + DataPivotConstants.AT + Integer.toHexString(System.identityHashCode(obj));
    }

}
