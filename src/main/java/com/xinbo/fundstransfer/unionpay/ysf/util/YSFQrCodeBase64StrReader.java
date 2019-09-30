/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

/**
 * @author blake
 *
 */
public class YSFQrCodeBase64StrReader {

	private static final Logger LOG = LoggerFactory.getLogger(YSFQrCodeBase64StrReader.class);
	
	/**
	 * 	<pre>将图片转换为base64的字符串后，字符串开头以    data:image/图片格式;base64,  开头,
	 * 需要将其删除掉之后才可以正常解码
	 * </pre>
	 * @param base64Str
	 * @return
	 */
	private static String removeFileHeader(final String base64Str) {
		if(Pattern.matches("data:image/(bmp|jpg|jpeg|png|tif|gif|pcx|tga|exif|fpx|svg|psd|cdr|pcd|dxf|ufo|eps|ai|raw|WMF|webp);base64,.+",base64Str )) {
			String tmp = base64Str.substring(base64Str.indexOf(",")+1);
			return tmp;
		}
		return base64Str;
	}
	
	/**
	 * 从二维码图片转换成的 base64Str 字符串中获取二维码内容
	 * @param base64Str
	 * @return base64Str 或者 解析出错时返回空。否则返回二维码内容
	 * @throws IOException
	 * @throws NotFoundException 
	 */
	public static String getQrContent(final String base64Str) {
		if(StringUtils.isEmpty(base64Str)) {
			return null;
		}
		byte[] base64StrBuffer = Base64.getDecoder().decode(removeFileHeader(base64Str));
		String content = null;
		EnumMap<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		hints.put(DecodeHintType.CHARACTER_SET, "UTF-8"); // 指定编码方式,防止中文乱码
		ByteArrayInputStream in = new ByteArrayInputStream(base64StrBuffer);
		BufferedImage image;
		try {
			image = ImageIO.read(in);
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			Binarizer binarizer = new HybridBinarizer(source);
			BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
			MultiFormatReader reader = new MultiFormatReader();
			Result result = reader.decode(binaryBitmap, hints);
			content = result.getText();
		} catch (IOException e) {
			LOG.error("字符串字节码转换为BufferImage时异常",e);
		} catch (NotFoundException e) {
			LOG.error("未能从 BufferImage 中获取到二维码内容",e);
		}
		return content;
	}

}
