package com.github.shootercheng.parse.parse;

import com.github.shootercheng.common.util.DataUtil;
import com.github.shootercheng.parse.constant.CommonConstant;
import com.github.shootercheng.parse.constant.MapperType;
import com.github.shootercheng.parse.exception.FileParseException;
import com.github.shootercheng.parse.param.ParseParam;
import com.github.shootercheng.parse.utils.FileParseCommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chengdu
 *
 */
public class CsvFileParse implements FileParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvFileParse.class);

    private static final String SPLIT_REGEX = "[,\t]";

    private CsvFileParse() {
    }

    private static class CsvFileParseHolder {
        private static final CsvFileParse parser = new CsvFileParse();
    }

    public static CsvFileParse instance() {
        return CsvFileParseHolder.parser;
    }

    @Override
    public <T> List<T> parseFile(String filePath, Class<T> clazz, ParseParam parseParam) {
        // 校验入参
        checkParam(parseParam);
        BufferedReader reader = null;
        List<T> resultList = new ArrayList<>();
        try {
            String charsetName = parseParam.getEncode() != null ?
                    parseParam.getEncode() : CommonConstant.GBK;
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(filePath), charsetName));
            int readLine = 0;
            String lineStr;
            while ( (lineStr = reader.readLine()) != null) {
                int headLine = parseParam.getHeadLine();
                // 匹配 head
                if (parseParam.getMapperType() == MapperType.HEAD && readLine == headLine) {
                    Map<Integer, String> headMap = getHeadMap(lineStr);
                    FileParseCommonUtil.buildParseParam(clazz, parseParam, headMap);
                } else if (readLine >= parseParam.getStartLine()) {
                    String[] lineArr = splitCsvLine(lineStr);
                    T t = convertArrToVo(clazz, lineArr, parseParam);
                    if (t != null) {
                        resultList.add(t);
                    } else {
                        parseParam.getErrorRecord()
                        .writeErrorMsg("line " + readLine + ":" + lineStr +
                        "covert to null");
                    }
                    if (parseParam.getDataConsumer() != null) {
                        if (resultList.size() >= parseParam.getBatchNum()) {
                            parseParam.getDataConsumer().accept(resultList, 0);
                        }
                    }
                }
                readLine++;
            }
            if (parseParam.getDataConsumer() != null) {
                if (resultList.size() > 0) {
                    parseParam.getDataConsumer().accept(resultList,0);
                }
            }
        } catch (Exception e) {
            LOGGER.error("parse csv file error {}", e.getMessage());
            throw new FileParseException("parse csv file error", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return resultList;
    }

    private Map<Integer, String> getHeadMap(String lineStr) {
        String[] lineArr = splitCsvLine(lineStr);
        Map<Integer, String> headMap = new HashMap<>();
        for (int i = 0; i < lineArr.length; i++) {
            headMap.put(i, lineArr[i]);
        }
        return headMap;
    }

    private <T> T convertArrToVo(Class<T> clazz, String[] inputArr, ParseParam parseParam) {
        T t = null;
        try {
            t = clazz.newInstance();
            Map<String, Method> fieldSetterMap = parseParam.getFieldSetterMap();
            for (Map.Entry<String, Method> entry : fieldSetterMap.entrySet()) {
                Integer column = DataUtil.EXCEL_COLUMN.get(entry.getKey());
                String cellValue = inputArr[column];
                if (parseParam.getCellFormat() != null) {
                    cellValue = parseParam.getCellFormat().format(entry.getKey(), cellValue);
                }
                FileParseCommonUtil.invokeValue(t, entry.getValue(), cellValue);
            }
            if (parseParam.getBusinessDefineParse() != null) {
                parseParam.getBusinessDefineParse().defineParse(t, inputArr, parseParam);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return t;
    }

    private String[] splitCsvLine(String inputLine) {
        if (!inputLine.contains(",") && !inputLine.contains("\t")) {
            throw new IllegalArgumentException("input csv line unknown delimiter");
        }
        String[] inputArr = inputLine.split(SPLIT_REGEX);
        List<String> resultList = new ArrayList<>(inputArr.length);
        for (int i = 0; i < inputArr.length; i++) {
            String istr = inputArr[i];
            // "123,456,789" 在一个单元格 需要合并
            if (istr.length() > 0 && istr.charAt(0) == '"' && istr.charAt(istr.length() - 1) != '"') {
                // 逗号连接替换成分号
                StringBuilder mergeStr = new StringBuilder(istr.substring(1) + ";");
                for (int j = i + 1; j < inputArr.length; j++) {
                    String jstr = inputArr[j];
                    i++;
                    if (jstr.length() > 0 && jstr.charAt(0) != '"' && jstr.charAt(jstr.length() - 1) == '"') {
                        mergeStr.append(jstr, 0, jstr.length() - 1);
                        break;
                    } else {
                        mergeStr.append(jstr).append(";");
                    }
                }
                resultList.add(mergeStr.toString());
            } else {
                resultList.add(istr);
            }
        }
        String[] rightArr = new String[resultList.size()];
        resultList.toArray(rightArr);
        return rightArr;
    }

    @Override
    public <T> Map<Integer, List<T>> parseFileSheets(String filePath, Class<T> clazz, Map<Integer, ParseParam> parseParamMap) {
        return null;
    }
}
