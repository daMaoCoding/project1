package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.service.SysImportBankLogService;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/r/importbanklog")
public class SysImportBankLogController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(SysRoleController.class);

    @Autowired
    private SysImportBankLogService sysImportBankLogService;

    @RequiresPermissions({ "SystemImportBankLog:*" })
    @RequestMapping("/export")
    public void exportExcel() throws JsonProcessingException {
        try {
            response.addHeader("Content-Disposition", "attachment;filename=template.xls");
            sysImportBankLogService.fileExport(response.getOutputStream());
            response.setContentType("application/vnd.ms-excel;charset=UTF-8");
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (Exception e) {
            logger.error("导出Excel模板失败：{}", e);
        }
    }

    @RequiresPermissions({ "SystemImportBankLog:*" })
    @RequestMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file) throws JsonProcessingException {
        String params = buildParams().toString();
        logger.debug(String.format("%s，参数：%s", "导入银行流水信息", params));
        if (!file.isEmpty()) {
            try {
                String originalFilename = file.getOriginalFilename();//原文件名字
                InputStream is = file.getInputStream();//获取输入流
                HashMap result = sysImportBankLogService.fileImp(is, originalFilename);
                String success = (String) result.get("success");
                if ("0".equals(success)) {
                    List<String> res = (List<String>) result.get("info");
                    GeneralResponseData<List<String>> responseData = new GeneralResponseData<>(
                            GeneralResponseData.ResponseStatus.SUCCESS.getValue());
                    responseData.setData(res);
                    return mapper.writeValueAsString(responseData);
                } else {
                    List<BizBankLog> res = (List<BizBankLog>) result.get("info");
                    GeneralResponseData<List<BizBankLog>> responseData = new GeneralResponseData<>(
                            GeneralResponseData.ResponseStatus.SUCCESS.getValue());
                    responseData.setData(res);
                    return mapper.writeValueAsString(responseData);
                }
            } catch (Exception e) {
                logger.error("导入Excel数据失败：{}", e);
            }
        }
        return "导入Excel数据失败：{}";
    }
}
