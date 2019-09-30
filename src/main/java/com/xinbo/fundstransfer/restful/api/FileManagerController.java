package com.xinbo.fundstransfer.restful.api;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.GeneralResponseData;

/**
 * 文件上传
 */
@RestController("fileUploadController")
@RequestMapping("/api/file")
public class FileManagerController {
	private static final Logger log = LoggerFactory.getLogger(FileManagerController.class);
	/**
	 * 系统设置，全局文件上传目录
	 */
	// @Value("${spring.http.multipart.location}")
	// private String uploadPath;
	@Value("${funds.transfer.multipart.location}")
	private String uploadPathNew;

	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/download")
	@ResponseBody
	public void incomeSys(HttpServletResponse resp, @RequestParam(value = "path") String path)
			throws JsonProcessingException {
		path = uploadPathNew + File.separator + path.trim();
		download(resp, path);
	}

	@RequestMapping("/getFile")
	@ResponseBody
	public void getFile(HttpServletResponse response, @RequestParam(value = "path") String path)
			throws JsonProcessingException {
		try {
			log.info("getFile>>  path:{}", path);
			File file = new File(uploadPathNew + File.separator + path.trim());
			String filename = file.getName();
			String ext = filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
			InputStream fis = new BufferedInputStream(new FileInputStream(file));
			byte[] buffer = new byte[fis.available()];
			fis.read(buffer);
			fis.close();
			response.reset();
			response.addHeader("Content-Disposition", "attachment;filename=" + new String(filename.getBytes()));
			response.addHeader("Content-Length", "" + file.length());
			OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
			response.setContentType("application/octet-stream");
			toClient.write(buffer);
			toClient.flush();
			toClient.close();
		} catch (IOException ex) {
			log.error("getFile>> ", ex);
		}
	}

	@RequestMapping("/fileList")
	public String fileList(@RequestParam(value = "path", required = false) String path) throws JsonProcessingException {
		boolean IS_ROOT = StringUtils.isBlank(path);
		List<FileNode> dataList = new ArrayList<>();
		String PATHROOT = uploadPathNew;
		path = StringUtils.isBlank(path) ? (PATHROOT) : (PATHROOT + File.separator + path.trim());
		File rootFile = Paths.get(path).toFile();
		Predicate<File> fil = (p) -> !p.getName().startsWith(".") && (p.isFile() || p.isDirectory());
		List<File> subList = Arrays.stream(rootFile.listFiles()).filter(fil)
				.sorted((o1, o2) -> (o2.isDirectory() ? 1 : 0) - (o1.isFile() ? 0 : 1)).collect(Collectors.toList());
		for (File f1 : subList) {
			String name = f1.getName();
			if (StringUtils.isBlank(name)
					|| (IS_ROOT && !Objects.equals("app", name.trim()) && !Objects.equals("pc", name.trim())))
				continue;
			String pathRoute = f1.getPath();
			boolean root = pathRoute.endsWith(PATHROOT) || f1.getParentFile().getPath().endsWith(PATHROOT);
			boolean file = f1.isFile();
			int subCount = file ? 0 : f1.listFiles().length;
			String next = f1.getPath().replace(PATHROOT, StringUtils.EMPTY);
			next = next.startsWith(File.separator) ? next.substring(1) : next;
			pathRoute = next;
			if (subCount == 0) {
				next = StringUtils.EMPTY;
			}
			dataList.add(new FileNode(root, file, name, next, subCount, pathRoute));
		}
		String parent = StringUtils.EMPTY;
		if (!rootFile.getPath().endsWith(PATHROOT)) {
			if (!rootFile.getParentFile().getPath().endsWith(PATHROOT)) {
				parent = rootFile.getParentFile().getPath().split(PATHROOT)[1];
				parent = parent.startsWith(File.separator) ? parent.substring(1) : parent;
			}
		}
		Map<String, Object> data = new HashMap<>();
		data.put("parent", parent);
		data.put("dataList", dataList);
		GeneralResponseData<Map<String, Object>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		res.setData(data);
		return mapper.writeValueAsString(res);
	}

	@RequestMapping("/screenshot")
	public String screenshot(@RequestBody String bodyJson) throws JsonProcessingException {
		FileOutputStream fos;
		try {
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String path = params.get("path");
			String screenshot = params.get("screenshot");
			log.info("file( screenshot ) >> RequestBody: path: {}", path);
			int poz = path.lastIndexOf("/");
			String fileName = path.substring(poz + 1);
			path = path.substring(0, poz);

			String absolutePath = uploadPathNew + File.separator + path;
			File dir = new File(absolutePath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			byte[] bytes = Base64.decodeBase64(screenshot.getBytes());
			fos = new FileOutputStream(new File(absolutePath + File.separator + fileName));
			fos.write(bytes);
			fos.flush();
			fos.close();
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	/**
	 * 上传图片或者上传PC端的日志文件
	 *
	 * @param file
	 *            压缩文件.zip或者图片
	 * @param path
	 *            文件相对路径，注意：如果是图片
	 *            不要包含文件名，path格式：大类文件夹名+当天日期+帐号作为子目录，示例：screenshot/20171129/6238783747474
	 *            如果是日志文件.zip:path格式是:pc/2019-02-15/虚拟机ip/loghhmm.zip(log小时分.zip,hhmm表示请求下载日志时间的时分)
	 * @return
	 */
	@RequestMapping(value = "/upload", consumes = "multipart/form-data", method = RequestMethod.POST)
	public String upload(@RequestParam(name = "file") MultipartFile file, @RequestParam(name = "path") String path) {
		// "app" + new SimpleDateFormat("/yyyy-MM-dd/").format(d) + alias + new
		// SimpleDateFormat("/HHmm").format(d)
		// + ".zip";
		// pc/2019-02-15/虚拟机ip/loghhmm.zip
		String absolutePath = uploadPathNew + File.separator + path;
		try {
			// 判断文件是否存在
			File dir = new File(absolutePath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			absolutePath += File.separator + file.getOriginalFilename();
			log.debug("File saved, absolutePath:{}", absolutePath);
			file.transferTo(new File(absolutePath));
		} catch (Exception e) {
			log.error("", e);
			return "{\"status\":-1,\"message\":\"" + e.getLocalizedMessage() + "\"}";
		}
		return "{\"status\":1,\"message\":\"success\"}";
	}

	/**
	 * 删除
	 */
	@RequestMapping(value = "/delete", method = { RequestMethod.GET, RequestMethod.POST })
	public String delete(@RequestParam(value = "path", required = true) String path) {
		try {
			String absolutePath = uploadPathNew + File.separator + path;
			boolean isDelete = deleteDir(new File(absolutePath));
			log.info("absolutePath:{}, IsDelete :{}", isDelete, absolutePath);
			return "{\"status\":1,\"message\":\"success\"}";
		} catch (Exception e) {
			log.error("", e);
			return "{\"status\":-1,\"message\":\"" + e.getLocalizedMessage() + "\"}";
		}
	}

	private void download(HttpServletResponse resp, String path) {
		File file = new File(path);
		resp.reset();
		resp.setContentType("application/octet-stream");
		resp.setCharacterEncoding("utf-8");
		resp.setContentLength((int) file.length());
		int poz = path.lastIndexOf(File.separator);
		String fileName = path.substring(poz + 1);
		resp.setHeader("Content-Disposition", "attachment;filename=" + fileName);
		byte[] buff = new byte[1024];
		BufferedInputStream bis = null;
		OutputStream os = null;
		try {
			os = resp.getOutputStream();
			bis = new BufferedInputStream(new FileInputStream(file));
			int i = 0;
			while ((i = bis.read(buff)) != -1) {
				os.write(buff, 0, i);
				os.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 递归删除目录下的所有文件及子目录下所有文件
	 *
	 * @param dir
	 *            将要删除的文件目录
	 * @return boolean Returns "true" if all deletions were successful. If a
	 *         deletion fails, the method stops attempting to delete and returns
	 *         "false".
	 */
	private static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			// 递归删除目录中的子目录下
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// 目录此时为空，可以删除
		return dir.delete();
	}

	class FileNode {
		private boolean root;
		private boolean file;
		private String name;
		private String next;
		private String path;
		private int subCount;

		public FileNode() {
		}

		public FileNode(boolean root, boolean file, String name, String next, int subCount, String path) {
			this.root = root;
			this.file = file;
			this.next = next;
			this.name = name;
			this.subCount = subCount;
			this.path = path;
		}

		public String getNext() {
			return next;
		}

		public void setNext(String next) {
			this.next = next;
		}

		public boolean isRoot() {
			return root;
		}

		public void setRoot(boolean root) {
			this.root = root;
		}

		public boolean isFile() {
			return file;
		}

		public void setFile(boolean file) {
			this.file = file;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getSubCount() {
			return subCount;
		}

		public void setSubCount(int subCount) {
			this.subCount = subCount;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}
}
