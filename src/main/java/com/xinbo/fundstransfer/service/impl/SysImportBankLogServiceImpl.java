package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.runtime.task.ToolResponseData;
import com.xinbo.fundstransfer.service.SysImportBankLogService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class SysImportBankLogServiceImpl implements SysImportBankLogService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ObjectMapper mapper;

    @Override
    public void fileExport(OutputStream out) throws Exception {
        //exportExcel("导入模板", out);
        InputStream in = this.getClass().getResourceAsStream("/导入银行流水模板.xls");
        int len = -1;
        byte[] buffer = new byte[35000];
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    /**
     * 导入银行卡流水
     *
     * @param is
     * @param filename
     * @return 导入结果
     * @throws Exception
     */
    @Override
    public HashMap<String, Object> fileImp(InputStream is, String filename) throws Exception {
        return importExcel(is, filename);
    }

    /**
     * @param title
     * @param out
     */
    private void exportExcel(String title, OutputStream out) {
        // 声明一个工作薄
        HSSFWorkbook workbook = new HSSFWorkbook();
        // 生成一个表格
        HSSFSheet sheet = workbook.createSheet(title);
        // 生成一个样式
        HSSFCellStyle textstyle = workbook.createCellStyle();
        HSSFDataFormat format = workbook.createDataFormat();
        textstyle.setDataFormat(format.getFormat("@"));
        // 设置表格默认列宽度为15个字节
        sheet.setDefaultColumnWidth((short) 20);
        // 第一行写入卡号数据
        HSSFRow cardnumrow = sheet.createRow(0);
        HSSFCell cardcell = cardnumrow.createCell(0);
        HSSFRichTextString text = new HSSFRichTextString("银行卡号：");
        cardcell.setCellValue(text);
        cardcell.setCellStyle(textstyle);

        HSSFCell cardment = cardnumrow.createCell(1, HSSFCell.CELL_TYPE_BLANK);
        addComment(sheet, "此单元格填写银行卡号信息", cardment, 0);

        // 第二行写入卡号数据
        HSSFRow currentasset = sheet.createRow(1);
        HSSFCell assetcell = currentasset.createCell(0);
        HSSFRichTextString assettext = new HSSFRichTextString("当前余额：");
        assetcell.setCellValue(assettext);
        assetcell.setCellStyle(textstyle);
        HSSFCell balancement = currentasset.createCell(1, HSSFCell.CELL_TYPE_BLANK);
        addComment(sheet, "此单元格填写当前余额信息", balancement, 0);

        // 第三行留空白
        HSSFRow blockrow = sheet.createRow(2);
        HSSFCell blockcell = blockrow.createCell(0);
        blockcell.setCellValue("");

        // 第四行写银行流水的标题
        HSSFRow titlerow = sheet.createRow(3);
        //交易时间
        HSSFCell tradetimecell = titlerow.createCell(0);
        HSSFRichTextString tradetimetext = new HSSFRichTextString("交易时间");
        tradetimecell.setCellValue(tradetimetext);
        //备注信息
        HSSFCell remarkcell = titlerow.createCell(1);
        HSSFRichTextString remarktext = new HSSFRichTextString("摘要（备注信息）");
        remarkcell.setCellValue(remarktext);

        //记账金额（收入）
        HSSFCell incomecell = titlerow.createCell(2);
        HSSFRichTextString incometext = new HSSFRichTextString("记账金额（收入）");
        incomecell.setCellValue(incometext);
        addComment(sheet, "当没有收入支出栏时，统一使用这一栏，如果为支出用 -金额 来区分", incomecell, 0);

        //记账金额（收入）
        HSSFCell outcomecell = titlerow.createCell(3);
        HSSFRichTextString outcometext = new HSSFRichTextString("记账金额（支出）");
        outcomecell.setCellValue(outcometext);

        //记账金额（收入）
        HSSFCell balancecell = titlerow.createCell(4);
        HSSFRichTextString balancetext = new HSSFRichTextString("余额");
        balancecell.setCellValue(balancetext);

        //对方户名
        HSSFCell tonamecell = titlerow.createCell(5);
        HSSFRichTextString tonametext = new HSSFRichTextString("对方户名");
        tonamecell.setCellValue(tonametext);

        //对方账号
        HSSFCell toaccocell = titlerow.createCell(6);
        HSSFRichTextString toaccotext = new HSSFRichTextString("对方账号");
        toaccocell.setCellValue(toaccotext);

        //备注信息
        HSSFCell paycodecell = titlerow.createCell(7);
        HSSFRichTextString paycodetext = new HSSFRichTextString("确认码");
        paycodecell.setCellValue(paycodetext);

        //formatBlockCell(sheet, 5, 20, 5, textstyle);
        try {
            workbook.write(out);
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 单元格增加注释
     *
     * @param sheet
     * @param commentstr
     * @param cell
     * @param cellnum
     */
    private void addComment(HSSFSheet sheet, String commentstr, HSSFCell cell, int cellnum) {
        HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
        // 定义注释的大小和位置,详见文档
        HSSFComment comment = patriarch.createComment(new HSSFClientAnchor(0, 0, 0, 0, (short) 4, 2, (short) 6, 5));
        // 设置注释内容
        comment.setString(new HSSFRichTextString(commentstr));
        cell.setCellComment(comment);
    }

    /**
     * 格式化空白cell
     *
     * @param startrow
     * @param totalrow
     * @param col
     * @param style
     */
    private void formatBlockCell(HSSFSheet sheet, int startrow, int totalrow, int col, HSSFCellStyle style) {
        for (int i = 0; i < totalrow; i++) {
            HSSFRow row = sheet.createRow(startrow + i);
            for (int j = 0; j < col; j++) {
                HSSFCell cell = row.createCell(i);
                cell.setCellStyle(style);
            }
        }
    }

    /**
     * 输入流转换成HSSFWorkbook进行处理
     *
     * @param in
     * @throws IOException
     */
    private HashMap<String, Object> importExcel(InputStream in, String filename) throws Exception {
        String extension = filename.lastIndexOf(".") == -1 ? "" : filename
                .substring(filename.lastIndexOf(".") + 1);
        if ("xls".equalsIgnoreCase(extension)) {
            return deal2003Excel(in);
        } else if ("xlsx".equalsIgnoreCase(extension)) {
            return deal2007Excel(in);
        } else {
            HashMap<String, Object> result = new HashMap<>();
            ArrayList<String> reslist = new ArrayList<>();
            reslist.add("不支持的文件类型");
            result.put("sucess", "0");
            result.put("info", reslist);
            return result;
        }
    }

    /**
     * 处理Excel2003数据
     *
     * @param in
     * @throws Exception
     */
    private HashMap<String, Object> deal2003Excel(InputStream in) throws Exception {
        HashMap<String, Object> resMap = new HashMap<>();
        ToolResponseData data = new ToolResponseData();
        ArrayList<BizBankLog> logs = new ArrayList<BizBankLog>();
        ArrayList<String> result = new ArrayList<String>();
        HSSFWorkbook workbook = new HSSFWorkbook(in);
        HSSFSheet sheet = workbook.getSheetAt(0);
        String accountstr = getCellValue(sheet, 0, 1);
        int fromAccount = getAccountIDByAccount(accountstr, result);
        String balancestr = getCellValue(sheet, 1, 1);
        setBalanceValue(balancestr, result, data);
        String[] parsePatterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss","YYYYMMDD"};
        for (int i = 4; i < sheet.getPhysicalNumberOfRows(); i++) {
            int j = i + 1;
            StringBuffer sbuffer = new StringBuffer("");
            BizBankLog banklog = new BizBankLog();
            banklog.setFromAccount(fromAccount);
            try {
                String tradetime = getCellValue(sheet, i, 0);
                //当交易日期为空时，不再读后续的数据
                if("".equals(tradetime)){
                    break;
                }
                Date tradedate = DateUtils.parseDate(tradetime, parsePatterns);
                banklog.setTradingTime(tradedate);
            } catch (Exception e) {
                sbuffer.append("第" + j + "行第1列日期格式不正确！");
            }
            banklog.setRemark(getCellValue(sheet, i, 1));
            banklog.setToAccountOwner(getCellValue(sheet, i, 5));
            banklog.setToAccount(getCellValue(sheet, i, 6));
            try {
                BigDecimal thisbalance = getBigDecimal(getCellValue(sheet, i, 4));
                banklog.setBalance(thisbalance);
            } catch (Exception e) {
                sbuffer.append("第" + j + "行第5列余额数据格式不正确！");
            }

            try {
                String incomestr = getCellValue(sheet, i, 2);
                incomestr = incomestr.replaceAll("_", "");
                String outcomestr = getCellValue(sheet, i, 3);
                outcomestr = outcomestr.replaceAll("_", "");
                if ("".equals(incomestr) && !"".equals(outcomestr)) {
                    banklog.setAmount(getBigDecimal(outcomestr).negate());
                } else if (!"".equals(incomestr)) {
                    banklog.setAmount(getBigDecimal(incomestr));
                } else {
                    sbuffer.append("第" + j + "行收入支出数据不正确！");
                }
            } catch (Exception e) {
                sbuffer.append("第" + j + "行收入支出数据格式不正确！");
            }
            String resultstr = sbuffer.toString();
            if (!"".equals(resultstr)) {
                result.add(resultstr);
            }
            logs.add(banklog);
        }
        if (result.size() > 0) {
            resMap.put("success", "0");
            resMap.put("info", result);
            return resMap;
        } else {
            data.setBanklogs(logs);
            MemCacheUtils.getInstance().getBanklogs().offer(mapper.writeValueAsString(data)); //导入流水写入缓存中
            resMap.put("success", "1");
            resMap.put("info", data.getBanklogs());
            return resMap;
        }
    }

    /**
     * 处理Excel2007数据
     *
     * @param in
     * @throws IOException
     */
    private HashMap<String, Object> deal2007Excel(InputStream in) throws Exception {
        HashMap<String, Object> resMap = new HashMap<>();
        ToolResponseData data = new ToolResponseData();
        ArrayList<BizBankLog> logs = new ArrayList<BizBankLog>();
        ArrayList<String> result = new ArrayList<String>();
        XSSFWorkbook xwb = new XSSFWorkbook(in);
        XSSFSheet sheet = xwb.getSheetAt(0);
        String accountstr = getCellValue(sheet, 0, 1);
        int fromAccount = getAccountIDByAccount(accountstr, result);
        String balancestr = getCellValue(sheet, 1, 1);
        setBalanceValue(balancestr, result, data);
        String[] parsePatterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss","YYYYMMDD"};
        for (int i = 4; i < sheet.getPhysicalNumberOfRows(); i++) {
            int j = i + 1;
            StringBuffer sbuffer = new StringBuffer("");
            BizBankLog banklog = new BizBankLog();
            banklog.setFromAccount(fromAccount);
            try {
                String tradetime = getCellValue(sheet, i, 0);
                Date tradedate = DateUtils.parseDate(tradetime, parsePatterns);
                banklog.setTradingTime(tradedate);
            } catch (Exception e) {
                sbuffer.append("第" + j + "行第1列日期格式不正确！");
            }
            banklog.setRemark(getCellValue(sheet, i, 1));
            banklog.setToAccountOwner(getCellValue(sheet, i, 5));
            banklog.setToAccount(getCellValue(sheet, i, 6));
            try {
                BigDecimal thisbalance = getBigDecimal(getCellValue(sheet, i, 4));
                banklog.setBalance(thisbalance);
            } catch (Exception e) {
                sbuffer.append("第" + j + "行第5列余额数据格式不正确！");
            }

            try {
                String incomestr = getCellValue(sheet, i, 2);
                incomestr = incomestr.replaceAll("_", "");
                String outcomestr = getCellValue(sheet, i, 3);
                outcomestr = outcomestr.replaceAll("_", "");
                if ("".equals(incomestr) && !"".equals(outcomestr)) {
                    banklog.setAmount(getBigDecimal(outcomestr).negate());
                } else if (!"".equals(incomestr)) {
                    banklog.setAmount(getBigDecimal(incomestr));
                } else {
                    sbuffer.append("第" + j + "行收入支出数据不正确！");
                }
            } catch (Exception e) {
                sbuffer.append("第" + j + "行收入支出数据格式不正确！");
            }
            String resultstr = sbuffer.toString();
            if (!"".equals(resultstr)) {
                result.add(resultstr);
            }
            logs.add(banklog);
        }
        if (result.size() > 0) {
            resMap.put("success", "0");
            resMap.put("info", result);
            return resMap;
        } else {
            data.setBanklogs(logs);
            MemCacheUtils.getInstance().getBanklogs().offer(mapper.writeValueAsString(data));   //导入流水写入缓存中
            resMap.put("success", "1");
            resMap.put("info", data.getBanklogs());
            return resMap;
        }
    }

    /**
     * 获取2003单元格数据
     *
     * @param sheet
     * @param rownum
     * @param colnum
     * @return
     * @throws Exception
     */
    private String getCellValue(HSSFSheet sheet, int rownum, int colnum) throws Exception {
        HSSFRow hssrow = sheet.getRow(rownum);
        HSSFCell hsscell = hssrow.getCell(colnum);
        if (hsscell == null) {
            return "";
        }
        String value;
        switch (hsscell.getCellType()) {
            case HSSFCell.CELL_TYPE_STRING:
                value = hsscell.getStringCellValue();
                break;
            case HSSFCell.CELL_TYPE_NUMERIC:
                if (colnum == 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    value = sdf.format(HSSFDateUtil.getJavaDate(hsscell.getNumericCellValue()));
                }else if(colnum == 7){
                    DecimalFormat df=new DecimalFormat("#");
                    value = df.format(hsscell.getNumericCellValue());
                }else {
                    value = String.valueOf(hsscell.getNumericCellValue());
                }
                break;
            default:
                value = hsscell.toString();
        }
        return StringUtils.trimToEmpty(value);
    }

    /**
     * 获取2007单元格数据
     *
     * @param sheet
     * @param rownum
     * @param colnum
     * @return
     * @throws Exception
     */
    private String getCellValue(XSSFSheet sheet, int rownum, int colnum) throws Exception {
        XSSFRow xssrow = sheet.getRow(rownum);
        XSSFCell xsscell = xssrow.getCell(colnum);
        if (xsscell == null) {
            return "";
        }
        String value;
        switch (xsscell.getCellType()) {
            case XSSFCell.CELL_TYPE_STRING:
                value = xsscell.getStringCellValue();
                break;
            case XSSFCell.CELL_TYPE_NUMERIC:
                if(colnum==0){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    value = sdf.format(HSSFDateUtil.getJavaDate(xsscell.getNumericCellValue()));
                }else if(colnum == 7){
                    DecimalFormat df=new DecimalFormat("#");
                    value = df.format(xsscell.getNumericCellValue());
                }else{
                    value = String.valueOf(xsscell.getNumericCellValue());
                }
                break;
            default:
                value = xsscell.toString();
        }
        return StringUtils.trim(value);
    }

    private int getAccountIDByAccount(String accountstr, ArrayList<String> result) throws Exception {
        if (StringUtils.isEmpty(accountstr)) {
            result.add("第1行第2列，银行卡号不允许为空！");
            return 0;
        }
        List<BizAccount> account = accountRepository.findByAccount(accountstr);
        if (account.size() != 1) {
            result.add("第1行第2列，提供的银行卡号不正确！");
            return 0;
        }
        return account.get(0).getId();
    }

    private void setBalanceValue(String str, ArrayList<String> result, ToolResponseData data) {
        try {
            data.setBalance(Float.valueOf(str.replaceAll(",", "")));
        } catch (Exception e) {
            result.add("第2行第2列，当前余额数据格式有误！");
        }
    }

    private BigDecimal getBigDecimal(String str) throws Exception {
        return new BigDecimal(str.replaceAll(",", ""));
    }
}