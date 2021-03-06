package com.github.catdou.export;

import com.github.catdou.export.models.User;
import com.github.shootercheng.export.common.ExportCommon;
import com.github.shootercheng.export.param.ExportParam;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James
 */
public class ExportTest {

    public ExportParam buildUserExportParam() {
        Map<String, String> fieldColumnMap = new HashMap<>();
        fieldColumnMap.put("A", "userName");
        fieldColumnMap.put("C", "seq");
        fieldColumnMap.put("B", "passWord");
        // build setter method
        List<Method> getterMethod = ExportCommon.buildParamGetter(User.class, fieldColumnMap);
        return new ExportParam()
                .setHeader("username,password,seq")
                .setGetterMethod(getterMethod);
    }

    public List<User> createDataList(int num) {
        List<User> userList = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            User user = new User("james" + i, "****", i);
            userList.add(user);
        }
        return userList;
    }
}
