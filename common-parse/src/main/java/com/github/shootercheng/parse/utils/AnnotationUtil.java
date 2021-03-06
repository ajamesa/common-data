package com.github.shootercheng.parse.utils;

import com.github.shootercheng.common.util.ReflectUtil;
import com.github.shootercheng.common.util.StringUtils;
import com.github.shootercheng.parse.anno.Location;
import com.github.shootercheng.parse.exception.ParamBuildException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author James
 */
public class AnnotationUtil {
    private static final Map<Class<?>, Map<Integer, Map<String, Method>>> DECLARED_CLASS_SHEET_SETTER = new ConcurrentHashMap<>(1024);

    public static Map<Integer, Map<String, Method>> findManySheetSetter(Class<?> clazz) {
        Map<Integer, Map<String, Method>> manySheetMethodMap = DECLARED_CLASS_SHEET_SETTER.get(clazz);
        if (manySheetMethodMap == null) {
            manySheetMethodMap = new HashMap<>(16);
            Map<String, Method> allBeanSetter = ReflectUtil.getBeanSetterMap(clazz);
            List<Field[]> fieldList = ReflectUtil.getAllFields(clazz);
            for (Field[] fields : fieldList) {
                for (Field field : fields) {
                    Location location = field.getAnnotation(Location.class);
                    String fieldName = field.getName();
                    if (location == null || StringUtils.isEmpty(fieldName)) {
                        continue;
                    }
                    int sheet = location.sheet();
                    String column = location.column().toUpperCase();
                    Map<String, Method> setterMethodMap = manySheetMethodMap.get(sheet);
                    if (setterMethodMap == null) {
                        setterMethodMap = new HashMap<>(16);
                        manySheetMethodMap.put(sheet, setterMethodMap);
                    }
                    // 如果同一个 sheet 页 已有某一列对应的方法，
                    // 取第一次遇到的
                    if (setterMethodMap.containsKey(column)) {
                        continue;
                    }
                    Method setterMethod = allBeanSetter.get(fieldName.toLowerCase());
                    if (setterMethod == null) {
                        throw new ParamBuildException("Bean " + clazz + " not contain field " + fieldName +
                                " set method ,please check config column map");
                    }
                    setterMethodMap.put(column, setterMethod);
                }
            }
        }
        DECLARED_CLASS_SHEET_SETTER.put(clazz, manySheetMethodMap);
        return manySheetMethodMap;
    }

    public static Map<String, Method> findOneSheetSetter(Class<?> clazz) {
        Map<Integer, Map<String, Method>> sheetParamMap = findManySheetSetter(clazz);
        if (sheetParamMap == null || sheetParamMap.size() != 1) {
            throw new ParamBuildException("only support one sheet config");
        }
        Integer sheetNum = sheetParamMap.keySet().iterator().next();
        return findManySheetSetter(clazz).get(sheetNum);
    }

    public static Map<String, Method> findOneSheetSetterBySheet(Class<?> clazz, int sheet) {
        Map<Integer, Map<String, Method>> sheetParamMap = findManySheetSetter(clazz);
        Map<String, Method> sheetSetter = sheetParamMap.get(sheet);
        if (sheetSetter == null) {
            throw new ParamBuildException("please check sheet num and annotation ");
        }
        return sheetSetter;
    }
}
