package com.xinbo.fundstransfer.ali4enterprise.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;

@Controller
public class ApplicationVersion {

    @ResponseBody
    @RequestMapping(value = "/application/info", method = RequestMethod.GET)
    public String versionInformation() {
        return readGitProperties();
    }
    private String readGitProperties() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("git.properties");
        try {
            return readFromInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return "Version information could not be retrieved";
        }
    }
    private String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            InetAddress addr = InetAddress.getLocalHost();
            String hostAddress = addr.getHostAddress();
            resultStringBuilder.append("\nhostname: "+ hostAddress);
            while ((line = br.readLine()) != null) {
                    resultStringBuilder.append(line).append("\n");
            }
        }


        return resultStringBuilder.toString();
    }
}
