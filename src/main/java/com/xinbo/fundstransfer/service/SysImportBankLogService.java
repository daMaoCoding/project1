package com.xinbo.fundstransfer.service;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.HashMap;

public interface SysImportBankLogService {
	/**
	 * 导出模板文件
	 * @param out
	 * @throws Exception
	 */
	void fileExport(OutputStream out) throws Exception;

	/**
	 * 导入文件
	 * @param is
	 * @throws Exception
	 */
	HashMap<String,Object> fileImp(InputStream is, String filename) throws Exception;
	
}
