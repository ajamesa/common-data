package com.github.shootercheng.export.core;

import com.github.shootercheng.common.constant.CommonConstants;
import com.github.shootercheng.common.util.DataUtil;
import com.github.shootercheng.export.common.ExportCommon;
import com.github.shootercheng.export.common.RowQuotationFormat;
import com.github.shootercheng.export.define.RowFormat;
import com.github.shootercheng.export.exception.ExportException;
import com.github.shootercheng.export.param.ExportParam;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.github.shootercheng.common.constant.CommonConstants.EXCEL_XLS;
import static com.github.shootercheng.common.constant.CommonConstants.EXCEL_XLSX;

/**
 * @author James
 */
public abstract class AbstractExcelExport implements BaseExport, QueryExport, DataListExport {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExcelExport.class);

    private String filePath;

    private String targetPath;

    private CellStyle cellStyle;

    private ExportParam exportParam;

    private boolean isTemplate;

    private Workbook workbook;

    private Sheet sheet;

    private int sheetStartLine;

    private int sheetIndex;

    private String excelType;

    private RowFormat rowFormat = new RowQuotationFormat();

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public CellStyle getCellStyle() {
        return cellStyle;
    }

    public void setCellStyle(CellStyle cellStyle) {
        this.cellStyle = cellStyle;
    }

    public ExportParam getExportParam() {
        return exportParam;
    }

    public void setExportParam(ExportParam exportParam) {
        this.exportParam = exportParam;
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean template) {
        isTemplate = template;
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public void setWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public void setSheet(Sheet sheet) {
        this.sheet = sheet;
    }

    public int getSheetStartLine() {
        return sheetStartLine;
    }

    public void setSheetStartLine(int sheetStartLine) {
        this.sheetStartLine = sheetStartLine;
    }

    public int getSheetIndex() {
        return sheetIndex;
    }

    public void setSheetIndex(int sheetIndex) {
        this.sheetIndex = sheetIndex;
    }

    public String getExcelType() {
        return excelType;
    }

    public void setExcelType(String excelType) {
        this.excelType = excelType;
    }

    public RowFormat getRowFormat() {
        return rowFormat;
    }

    public void setRowFormat(RowFormat rowFormat) {
        this.rowFormat = rowFormat;
    }

    public void initExcel() throws IOException {
        if(filePath == null || filePath.trim().length() == 0){
            throw new ExportException("file path is null");
        }
        if (!isTemplate) {
            if (filePath.endsWith(EXCEL_XLS)) {
                workbook = new HSSFWorkbook();
                excelType = EXCEL_XLS;
            } else if (filePath.endsWith(EXCEL_XLSX)) {
                workbook = new SXSSFWorkbook();
                excelType = EXCEL_XLSX;
            } else {
                throw new ExportException("excel type error");
            }
        } else {
            InputStream inputStream = new FileInputStream(filePath);
            if (filePath.endsWith(EXCEL_XLS)) {
                workbook = new HSSFWorkbook(inputStream);
                excelType = EXCEL_XLS;
            } else if (filePath.endsWith(EXCEL_XLSX)) {
                XSSFWorkbook xssfWorkbook = new XSSFWorkbook(filePath);
                workbook = new SXSSFWorkbook(xssfWorkbook);
                excelType = EXCEL_XLSX;
            } else {
                throw new ExportException("excel type error");
            }
        }
    }

    /**
     * 默认使用 ',' 号拼接
     * @param rowData row data
     */
    @Override
    public void processRowData(String rowData) {
        // 去除引号
        rowData = rowFormat.formatRow(rowData);
        if (exportParam.getRowFormat() != null) {
            rowData = exportParam.getRowFormat().formatRow(rowData);
        }
        String[] cellValues = rowData.split(",");
        // 超过最大行数，就再创建下一页
        boolean maxXlsRow = EXCEL_XLS.equals(excelType) && sheetStartLine > CommonConstants.EXCEL_MAX_ROW_XLS;
        boolean maxXlsxRow = EXCEL_XLSX.equals(excelType) && sheetStartLine > CommonConstants.EXCEL_MAX_ROW_XLSX;
        if ( maxXlsRow||maxXlsxRow ) {
            sheetStartLine = exportParam.getStartLine();
            // 初始化已判断null，这里不判断
            String sheetName = exportParam.getSheetName();
            int curIndex = ++sheetIndex;
            initSheet(sheetName + "_" + curIndex);
        }
        fillRowData(sheet, cellValues, sheetStartLine);
        sheetStartLine++;
    }

    public void initSheet(String sheetName) {
        if (!isTemplate) {
            sheet = workbook.createSheet(sheetName);
            String[] headers = exportParam.getHeader().split(",");
            writeExcelTitle(sheet, cellStyle, headers, sheetStartLine);
            sheetStartLine++;
        } else {
            if (sheetIndex == exportParam.getSheetIndex()) {
                sheet = workbook.getSheetAt(exportParam.getSheetIndex());
            } else {
                sheet = workbook.createSheet(sheetName);
                sheetStartLine = 0;
            }
        }
        LOGGER.info("now sheet is {}", sheetName);
    }

    private void writeExcelTitle(Sheet sheet, CellStyle cellStyle, String[] headers, int rowNum) {
        // 创建标题行, 添加样式
        Row row = sheet.createRow(rowNum);
        for(int i = 0; i < headers.length; i++){
            Cell cell = row.createCell(i);
            if (cellStyle != null) {
                cell.setCellStyle(cellStyle);
            }
            cell.setCellValue(headers[i]);
        }
    }

    private void fillRowData(Sheet sheet, String[] cellValues, int curLine){
        Row valueRow = sheet.createRow(curLine);
        for(int j = 0; j < cellValues.length; j++) {
            Cell cell = valueRow.createCell(j);
            if (exportParam.getCellFormat() != null) {
                String columnChar = DataUtil.COLUMN_NUM.get(j);
                String cellValue = exportParam.getCellFormat().format(columnChar, cellValues[j]);
                cell.setCellValue(cellValue);
            } else {
                cell.setCellValue(cellValues[j]);
            }
        }
    }

    public void saveExcel() {
        OutputStream fileOutputStream = null;
        try {
            // 写入文件
            if (!isTemplate) {
                fileOutputStream = new FileOutputStream(filePath);
            } else {
                fileOutputStream = new FileOutputStream(targetPath);
            }
            workbook.write(fileOutputStream);
        } catch (Exception e) {
            LOGGER.error("core excel error");
            throw new ExportException("core excel error", e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    LOGGER.info("close workbook error");
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    LOGGER.info("close file outputstream error");
                }
            }
        }
    }

    @Override
    public void exportQueryPage(Function<Map<String, Object>, List<String>> dataGetFun) {
        String sheetName = getDefaultSheetName();
        initSheet(sheetName);
        exportQuery(dataGetFun, exportParam);
        saveExcel();
    }

    private String getDefaultSheetName() {
        String sheetName = exportParam.getSheetName();
        if (sheetName == null || sheetName.trim().length() == 0) {
            sheetName = "sheet";
            exportParam.setSheetName(sheetName);
        }
        return sheetName;
    }

    @Override
    public <T> void exportList(List<T> dataList) {
        String sheetName = getDefaultSheetName();
        initSheet(sheetName);
        exportList(dataList, exportParam);
        saveExcel();
    }

    @Override
    public void close() {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
