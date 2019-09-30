package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

@RestController
@RequestMapping("/r/captch")
public class CaptchController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(CaptchController.class);

    @RequestMapping("/getCaptcha")
    public void getCaptcha() {
        // 验证码图片的宽度。
        int width = 70;
        // 验证码图片的高度。
        int height = 30;
        BufferedImage buffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffImg.createGraphics();

        // 创建一个随机数生成器类
        Random random = new Random();         // 设定图像背景色(因为是做背景，所以偏淡)
        g.setColor(getRandColor(200, 250));
        g.fillRect(0, 0, width, height);

        // 创建字体，字体的大小应该根据图片的高度来定。
        Font font = new Font("Times New Roman", Font.HANGING_BASELINE, 28);        // 设置字体。
        g.setFont(font);          // 画边框。
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, width - 1, height - 1);

        // 随机产生155条干扰线，使图象中的认证码不易被其它程序探测到。
        //g.setColor(Color.GRAY);
        g.setColor(getRandColor(160, 200));
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int xl = random.nextInt(12);
            int yl = random.nextInt(12);
            g.drawLine(x, y, x + xl, y + yl);
        }

        // randomCode用于保存随机产生的验证码，以便用户登录后进行验证。
        StringBuffer randomCode = new StringBuffer();

        // 设置默认生成4个验证码
        int length = 4;
        // 设置备选验证码:包括"a-z"和数字"0-9"
        String base = "abcdefghijkmnpqrstuvwxyzABCDEFGHIJKLMNPQRSTUVWXYZ123456789";
        int size = base.length();

        for (int i = 0; i < length; i++) {
            int start = random.nextInt(size);
            String strRand = base.substring(start, start + 1);
            g.setColor(new Color(20 + random.nextInt(110), 20 + random.nextInt(110), 20 + random.nextInt(110)));
            g.drawString(strRand, 15 * i + 6, 24);
            randomCode.append(strRand);
        }

        // 将四位数字的验证码保存到Session中。
        HttpSession session = request.getSession();
        session.setAttribute("captch", randomCode.toString().toLowerCase());
        //图象生效
        g.dispose();
        // 禁止图像缓存。
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        // 将图像输出到Servlet输出流中。105
        ServletOutputStream sos = null;
        try {
            sos = response.getOutputStream();
            ImageIO.write(buffImg, "jpeg", sos);
            sos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sos != null) {
                try {
                    sos.close();
                    sos = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequestMapping("/checkCaptcha")
    public String checkCaptcha(@RequestParam(value = "captch", required = true) String captch)
            throws JsonProcessingException {
        GeneralResponseData<Object> responseData = new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue());
        if (StringUtils.isBlank(captch) || captch.length() != 4) {
            return mapper.writeValueAsString(responseData);
        }
        HttpSession session = request.getSession();
        String cap = (String) session.getAttribute("captch");
        if (captch.equals(cap)) {
            responseData = new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            return mapper.writeValueAsString(responseData);
        }
        return mapper.writeValueAsString(responseData);
    }

    private Color getRandColor(int fc, int bc) {// 给定范围获得随机颜色
        Random random = new Random();
        if (fc > 255)
            fc = 255;
        if (bc > 255)
            bc = 255;
        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);
        return new Color(r, g, b);
    }
}
