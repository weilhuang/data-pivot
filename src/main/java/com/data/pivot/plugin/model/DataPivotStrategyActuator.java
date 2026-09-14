package com.data.pivot.plugin.model;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.entity.custom.DataPivotCustomAnnotationInfo;
import com.data.pivot.plugin.entity.custom.DataPivotCustomScriptInfo;
import com.data.pivot.plugin.entity.custom.DataPivotMapperInfo;
import com.data.pivot.plugin.entity.custom.DataPivotMapperMethodInfo;
import com.data.pivot.plugin.entity.custom.DataPivotStrategyInfo;
import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.tool.DataPivotUtil;
import com.data.pivot.plugin.tool.MessageUtil;
import com.data.pivot.plugin.tool.ModuleUtils;
import com.data.pivot.plugin.tool.ProjectUtils;
import com.data.pivot.plugin.tool.PsiAnnotationUtil;
import com.data.pivot.plugin.enums.MapperMethodType;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.tool.StringConverter;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.impl.java.stubs.index.JavaShortClassNameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.*;

public class DataPivotStrategyActuator{
    public static DataPivotRelation mapping(DataPivotObject dataPivotObject,Editor editor){
        DataPivotMappingSettingInfo dataPivotMappingSettingInfo = dataPivotObject.getDataPivotMappingSettingInfo();
        DataPivotStrategyInfo dataPivotStrategyInfo = dataPivotObject.getDataPivotStrategyInfo();
        PsiField currentPsiField = dataPivotObject.getCurrentPsiField();
        PsiClass currentClass = dataPivotObject.getCurrentClass();
        DataPivotMapperInfo ormMapper = dataPivotStrategyInfo.getOrmMapper();
        DataPivotMapperMethodInfo structMethod = ormMapper.getStructMethod();
        DataPivotMapperMethodInfo elementMethod = ormMapper.getElementMethod();
        String tableName = getStructName(currentClass,structMethod);
        if (StrUtil.isEmpty(tableName)) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.hint.orm.struct.name.null",
                    currentClass.getName()
                    ));
            return null;
        }
        String columnName = getElementName(currentPsiField,elementMethod);
        if (StrUtil.isEmpty(columnName)) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.hint.orm.element.name.null",
                    currentClass.getName()+DataPivotConstants.NUMBER_SIGN+currentPsiField.getName()
            ));
            return null;
        }
        Map<String, Map<String, DataPivotRelation>> stringMapMap = DataPivotApplication.getInstance().MAPPER.DP_RELATION_INFO_MAPPER.get(dataPivotMappingSettingInfo.getDatabaseReference());
        if (stringMapMap == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.hint.orm.database.mapping.null",
                    dataPivotMappingSettingInfo.getDatabasePath()
            ));
            return null;
        }
        Map<String, DataPivotRelation> stringDataPivotRelationMap = stringMapMap.get(tableName);
        if (stringDataPivotRelationMap == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.hint.orm.table.mapping.null",
                    dataPivotMappingSettingInfo.getDatabasePath()+DataPivotConstants.DOT+tableName
            ));
            return null;
        }
        DataPivotRelation dataPivotRelation = stringDataPivotRelationMap.get(columnName);
        if (dataPivotRelation == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.hint.orm.column.mapping.null",
                    dataPivotMappingSettingInfo.getDatabasePath()+DataPivotConstants.DOT+tableName+DataPivotConstants.DOT+columnName
            ));
            return null;
        }
        return dataPivotRelation;
    }

    public static String resolveTableName(PsiClass currentClass, DataPivotStrategyInfo strategy) {
        if (currentClass == null || strategy == null || strategy.getOrmMapper() == null
                || strategy.getOrmMapper().getStructMethod() == null) {
            return null;
        }
        return getStructName(currentClass, strategy.getOrmMapper().getStructMethod());
    }

    public static String resolveColumnName(PsiField currentPsiField, DataPivotStrategyInfo strategy) {
        if (currentPsiField == null || strategy == null || strategy.getOrmMapper() == null
                || strategy.getOrmMapper().getElementMethod() == null) {
            return null;
        }
        return getElementName(currentPsiField, strategy.getOrmMapper().getElementMethod());
    }

    private static String getStructName (PsiClass currentClass, DataPivotMapperMethodInfo dataPivotMapperMethodInfo) {
        String type = dataPivotMapperMethodInfo.getType();
        if (MapperMethodType.ANNOTATION.getName().equals(type)) {
            //1.注解处理
            List<DataPivotCustomAnnotationInfo> customAnnotation = dataPivotMapperMethodInfo.getCustomAnnotation();
            String fieldAnnotationValue = getClassAnnotationValue(currentClass,dataPivotMapperMethodInfo.getCustomAnnotation());
            String tableName = null;
            if (StrUtil.isNotEmpty(fieldAnnotationValue)) {
                tableName = PsiAnnotationUtil.removeDoubleQuotes(fieldAnnotationValue);
                return tableName;
            }else if (dataPivotMapperMethodInfo.getDefaultMethod()!=null){
                //3.默认方法处理
                tableName = executeScript(dataPivotMapperMethodInfo.getDefaultMethod().getContent(),currentClass.getName());
                return tableName;
            }
        }else if (MapperMethodType.SCRIPT.getName().equals(type)){
            //脚本处理
            DataPivotCustomScriptInfo customScript = dataPivotMapperMethodInfo.getCustomScript();
            return executeScript(customScript.getContent(),currentClass.getName());
        }
        return null;
    }

    /**
     * @param currentPsiField
     * @param dataPivotMapperMethodInfo
     * @return
     */
    private static String getElementName(PsiField currentPsiField, DataPivotMapperMethodInfo dataPivotMapperMethodInfo) {
        String type = dataPivotMapperMethodInfo.getType();
        if (MapperMethodType.ANNOTATION.getName().equals(type)) {
            //1.注解处理
            List<DataPivotCustomAnnotationInfo> customAnnotation = dataPivotMapperMethodInfo.getCustomAnnotation();
            String fieldAnnotationValue = getFieldAnnotationValue(currentPsiField,dataPivotMapperMethodInfo.getCustomAnnotation());
            String columnName = null;
            if (StrUtil.isNotEmpty(fieldAnnotationValue)) {
                columnName = PsiAnnotationUtil.removeDoubleQuotes(fieldAnnotationValue);
                return columnName;
            }else{
                //3.默认方法处理
                columnName = executeScript(dataPivotMapperMethodInfo.getDefaultMethod().getContent(),currentPsiField.getName());
                return columnName;
            }
        }else if (MapperMethodType.SCRIPT.getName().equals(type)){
            //脚本处理
            DataPivotCustomScriptInfo customScript = dataPivotMapperMethodInfo.getCustomScript();
            return executeScript(customScript.getContent(),currentPsiField.getName());
        }
        return null;
    }
    public static String getFieldAnnotationValue(PsiField psiField,List<DataPivotCustomAnnotationInfo> customAnnotation){
        PsiModifierList modifierList = psiField.getModifierList();
        PsiAnnotation[] annotations = modifierList.getAnnotations();
        for (PsiAnnotation psiAnnotation : annotations) {
            for (DataPivotCustomAnnotationInfo dataPivotCustomAnnotationInfo : customAnnotation) {
                String annotationQualifiedName = dataPivotCustomAnnotationInfo.getAnnotationQualifiedName();
                if (psiAnnotation.getQualifiedName().equals(annotationQualifiedName)) {
                    return PsiAnnotationUtil.getAnnotationValue(psiAnnotation,dataPivotCustomAnnotationInfo.getAnnotationParameters());
                }
            }
        }
        return null;
    }
    public static String getClassAnnotationValue(PsiClass psiClass,List<DataPivotCustomAnnotationInfo> customAnnotation){
        PsiModifierList modifierList = psiClass.getModifierList();
        PsiAnnotation[] annotations = modifierList.getAnnotations();
        for (PsiAnnotation psiAnnotation : annotations) {
            for (DataPivotCustomAnnotationInfo dataPivotCustomAnnotationInfo : customAnnotation) {
                String annotationQualifiedName = dataPivotCustomAnnotationInfo.getAnnotationQualifiedName();
                if (psiAnnotation.getQualifiedName().equals(annotationQualifiedName)) {
                    return PsiAnnotationUtil.getAnnotationValue(psiAnnotation,dataPivotCustomAnnotationInfo.getAnnotationParameters());
                }
            }
        }
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            return getClassAnnotationValue(superClass,customAnnotation);
        }
        return null;
    }

    public static DataPivotObject mapping(DataPivotRelation dataPivotRelation,Editor editor){
        DataPivotStrategyInfo dataPivotStrategyInfo = dataPivotRelation.getDataPivotStrategyInfo();
        DataPivotMapperInfo romMapper = dataPivotStrategyInfo.getRomMapper();
        DataPivotMappingSettingInfo dataPivotMappingSettingInfo = dataPivotRelation.getDataPivotMappingSettingInfo();
        String modelName = dataPivotMappingSettingInfo.getModelName();
        String packageName = dataPivotMappingSettingInfo.getPackageName();
        List<Module> moduleList = ModuleUtils.getModuleList();
        Module currentModule = null;
        for (Module module : moduleList) {
            if (module.getName().equals(modelName)) {
                currentModule = module;
            }
        }
        if (currentModule == null) {
            MessageUtil.Dialog.info(DataPivotBundle.message(
                    "data.pivot.hint.rom.module.mapping.null",
                    modelName
            ));
            return null;
        }
        List<PsiClass> psiClassList = getPsiClassList(dataPivotRelation, currentModule, romMapper);
        if (psiClassList == null||psiClassList.isEmpty()) {
            MessageUtil.Dialog.info( DataPivotBundle.message(
                    "data.pivot.hint.orm.class.mapping.null",
                    dataPivotRelation.getTableName()
            ));
            return null;
        }
        PsiField psiField = getPsiField(psiClassList,romMapper,dataPivotRelation);
        if (psiField == null) {
            MessageUtil.Dialog.info( DataPivotBundle.message(
                    "data.pivot.hint.orm.field.mapping.null",
                    dataPivotRelation.getColumnName()
            ));
            return null;
        }
        PsiClass containingClass = psiField.getContainingClass();
        DataPivotObject dataPivotObject = new DataPivotObject();
        dataPivotObject.setPsiElement(psiField);
        dataPivotObject.setModule(currentModule);
        dataPivotObject.setModuleName(modelName);
        dataPivotObject.setPackageName(packageName);
        dataPivotObject.setCurrentClass(containingClass);
        dataPivotObject.setCurrentPsiField(psiField);
        dataPivotObject.setPsiFieldList(Arrays.asList(containingClass.getFields()));
        dataPivotObject.setCurrentClassName(containingClass.getName());
        dataPivotObject.setCurrentFieldName(psiField.getName());
        dataPivotObject.setPackageReference(DataPivotUtil.createPackageReference(modelName,packageName));
        dataPivotObject.setFieldReference(DataPivotUtil.createFieldReference(modelName,packageName,containingClass.getName(),psiField.getName()));
        dataPivotObject.setDataPivotMappingSettingInfo(dataPivotMappingSettingInfo);
        dataPivotObject.setDataPivotStrategyInfo(DataPivotApplication.getDataPivotStrategyInfo(dataPivotMappingSettingInfo.getStrategyCode()));
        return dataPivotObject;
    }

    private static List<PsiClass> getPsiClassList(DataPivotRelation dataPivotRelation, Module currentModule, DataPivotMapperInfo dataPivotMapperInfo) {
        Project currProject = ProjectUtils.getCurrProject();
        //数据库一对多,目前先用全局搜,注解查询用模块下,脚本查询用全作用域?
        //需要规范ROM一对一?表名前缀法?类似全类名?
        //GlobalSearchScope globalSearchScope = GlobalSearchScope.moduleScope(currentModule);
        GlobalSearchScope globalSearchScope = GlobalSearchScope.allScope(currProject);
        DataPivotMapperMethodInfo structMethod = dataPivotMapperInfo.getStructMethod();
        DataPivotMapperMethodInfo elementMethod = dataPivotMapperInfo.getElementMethod();
        String type = structMethod.getType();
        List<PsiClass> result = new ArrayList<>();
        if (MapperMethodType.ANNOTATION.getName().equals(type)) {
            //注解处理
            List<DataPivotCustomAnnotationInfo> customAnnotation = structMethod.getCustomAnnotation();
            for (DataPivotCustomAnnotationInfo dataPivotCustomAnnotationInfo : customAnnotation) {
                String shortName = StrUtil.subAfter(dataPivotCustomAnnotationInfo.getAnnotationQualifiedName(),DataPivotConstants.DOT,true);
                Collection<PsiAnnotation> psiAnnotations = JavaAnnotationIndex.getInstance().get(shortName, currProject, globalSearchScope);
                for (PsiAnnotation psiAnnotation : psiAnnotations) {
                    PsiModifierList psiModifierList = (PsiModifierList) psiAnnotation.getParent();
                    PsiElement psiElement = psiModifierList.getParent();
                    if (!(psiElement instanceof PsiClass)) {
                        continue;
                    }
                    PsiClass psiClass = (PsiClass) psiElement;
                    //判断类注解值是否和映射表名相等
                    String annotationValue = PsiAnnotationUtil.getAnnotationValue(psiAnnotation,dataPivotCustomAnnotationInfo.getAnnotationParameters());
                    if (!dataPivotRelation.getTableName().equals(PsiAnnotationUtil.removeDoubleQuotes(annotationValue))) {
                        continue;
                    }
                    //判断该全类名是否为package前缀
                    String qualifiedName = psiClass.getQualifiedName();
                    String packageName = dataPivotRelation.getDataPivotMappingSettingInfo().getPackageName();
                    if (!DataPivotUtil.comparePackageName(packageName,qualifiedName)) {
                        continue;
                    }
                    PsiField[] psiFields = psiClass.getFields();
                    if (psiFields == null) {
                        continue;
                    }
                    result.add(psiClass);
                }
            }
            if (result.isEmpty()) {
                String defaultMethodValue = executeScript(structMethod.getDefaultMethod().getContent(), dataPivotRelation.getTableName());
                //注解时没有找到,默认方法兜底 structMethod.getDefaultMethod()
                Collection<PsiClass> psiClasses = JavaShortClassNameIndex.getInstance().get(defaultMethodValue, currProject, globalSearchScope);
                return ListUtil.toList(psiClasses);
            }
            return result;
        }else if (MapperMethodType.SCRIPT.getName().equals(type)){
            DataPivotCustomScriptInfo customScript = structMethod.getCustomScript();
            String tableName = executeScript(customScript.getContent(), dataPivotRelation.getTableName());
            //JavaShortClassNameIndex.getInstance().getAllKeys(currProject).stream().filter(a->StrUtil.contains(a,"Sys")).collect(Collectors.toList());
            Collection<PsiClass> psiClasses = JavaShortClassNameIndex.getInstance().get(tableName, currProject, globalSearchScope);
            return ListUtil.toList(psiClasses);
        }
        return null;
    }

    private static PsiField getPsiField(List<PsiClass> psiClassList, DataPivotMapperInfo dataPivotMapperInfo, DataPivotRelation dataPivotRelation) {
        for (PsiClass psiClass : psiClassList) {
            return getPsiField(psiClass,dataPivotMapperInfo,dataPivotRelation);
        }
        return null;
    }
    private static PsiField getPsiField(PsiClass psiClass, DataPivotMapperInfo dataPivotMapperInfo, DataPivotRelation dataPivotRelation) {
        DataPivotMapperMethodInfo elementMethod = dataPivotMapperInfo.getElementMethod();
        String type = elementMethod.getType();
        if (MapperMethodType.ANNOTATION.getName().equals(type)) {
            String defaultMethodValue = executeScript(elementMethod.getDefaultMethod().getContent(), dataPivotRelation.getColumnName());
            for (PsiField psiField : psiClass.getFields()) {
                List<DataPivotCustomAnnotationInfo> customAnnotation = elementMethod.getCustomAnnotation();
                for (DataPivotCustomAnnotationInfo dataPivotCustomAnnotationInfo : customAnnotation) {
                    String annotationQualifiedName = dataPivotCustomAnnotationInfo.getAnnotationQualifiedName();
                    PsiAnnotation psiFieldAnnotation = psiField.getModifierList().findAnnotation(annotationQualifiedName);
                    if (psiFieldAnnotation != null) {
                        String psiFieldAnnotationValue = PsiAnnotationUtil.getAnnotationValue(psiFieldAnnotation,dataPivotCustomAnnotationInfo.getAnnotationParameters());
                        if (dataPivotRelation.getColumnName().equals(PsiAnnotationUtil.removeDoubleQuotes(psiFieldAnnotationValue))) {
                            return psiField;
                        }
                    }else {

                        if (StrUtil.isNotEmpty(defaultMethodValue)&&defaultMethodValue.equals(psiField.getName())) {
                            return psiField;
                        }
                    }
                }
            }
        }else if (MapperMethodType.SCRIPT.getName().equals(type)){
            DataPivotCustomScriptInfo customScript = elementMethod.getCustomScript();
            String columnName = executeScript(customScript.getContent(), dataPivotRelation.getColumnName());
            for (PsiField field : psiClass.getFields()) {
                if (field.getName().equals(columnName)) {
                    return field;
                }
            }
        }
        //查询父类
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            return getPsiField(superClass,dataPivotMapperInfo,dataPivotRelation);
        }
        return null;
    }

    private static String executeScript(String scriptString, String inputString) {
        // 移除js执行引擎,插件瘦身
        switch (scriptString){
            case DataPivotConstants.TO_BIG_CAMEL_CASE_SCRIPT:
                return StringConverter.toBigCamelCase(inputString);
            case DataPivotConstants.TO_CAMEL_CASE_SCRIPT:
                return StringConverter.toCamelCase(inputString);
            case DataPivotConstants.TO_UNDER_SCORE_SCRIPT:
                return StringConverter.toUnderScore(inputString);
            default:return null;
        }
//        org.graalvm.polyglot.Context context = Context.create();
//        context.eval("js",scriptString);
//        org.graalvm.polyglot.Value result = context.eval("js", DataPivotConstants.SCRIPT_FUNCTION_NAME+"('" + inputString + "')");
//        return result.asString();
//
//        ScriptEngineManager manager = new ScriptEngineManager();
//        ScriptEngine engine = manager.getEngineByName("JavaScript");
//        if (engine == null) {
//            //new ScriptEngineManager().getEngineFactories();
//            //engine = new ScriptEngineManager().getEngineByName("graal.js");
//            org.graalvm.polyglot.Context context = Context.create();
//            context.eval("js",scriptString);
//            org.graalvm.polyglot.Value result = context.eval("js", DataPivotConstants.SCRIPT_FUNCTION_NAME+"('" + inputString + "')");
//            return result.asString();
//        }else {
//            try {
//                engine.eval(scriptString);
//                String result = (String) engine.eval(DataPivotConstants.SCRIPT_FUNCTION_NAME+"('" + inputString + "')");
//                System.out.println("JavaScript函数返回的值: " + result);
//                return result;
//            } catch (ScriptException e) {
//                e.printStackTrace();
//            }
//        }
//        return null;
    }
}
