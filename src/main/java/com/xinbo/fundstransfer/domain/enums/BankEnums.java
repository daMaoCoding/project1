package com.xinbo.fundstransfer.domain.enums;

import org.apache.commons.lang3.StringUtils;

public enum BankEnums {

    CMB(1, "CMB", "招商银行", new String[]{"95555"}),
    ICBC(2, "ICBC", "工商银行", new String[]{"95588"}),
    CCB(3, "CCB", "建设银行", new String[]{"95533"}),
    ABC(4, "ABC", "农业银行", new String[]{"955599"}),
    PA(5, "PA", "平安银行", new String[]{"95511"}),
    BOC(6, "BOC", "中国银行", new String[]{"95566"}),
    CITIC(7, "CITIC", "中信银行", new String[]{"95558"}),
    CIB(8, "CIB", "兴业银行", new String[]{"95561"}),
    SPDB(9, "SPDB", "浦发银行", new String[]{"95528"}),
    CEB(10, "CEB", "光大银行", new String[]{"95595"}),
    BOCM(11, "BOCM", "交通银行", new String[]{"95559"}),
    CGB(12, "CGB", "广发银行", new String[]{"95508"}),
    CMBC(13, "CMBC", "民生银行", new String[]{"95568"}),
    PBC(14, "PBC", "中国人民银行", new String[]{"95568"}),
    CEXIM(15, "CEXIM", "中国进出口银行", new String[]{"95568"}),
    ADBC(16, "ADBC", "中国农业发展银行", new String[]{"95568"}),
    CDB(17, "CDB", "国家开发银行", new String[]{"95568"}),
    PSBC(18, "PSBC", "中国邮政储蓄银行", new String[]{"95568"}),
    CBHB(19, "CBHB", "渤海银行", new String[]{"95568"}),
    HF(20, "HF", "恒丰银行", new String[]{"95568"}),
    BOB(21, "BOB", "北京银行", new String[]{"95568"}),
    SCSB(22, "SCSB", "上海银行", new String[]{"95568"}),
    JSB(23, "JSB", "江苏银行", new String[]{"95568"}),
    CZ(24, "CZ", "浙商银行", new String[]{"95568"}),
    HX(25, "HX", "华夏银行", new String[]{"95568"}),
    HRB(26, "HRB", "哈尔滨银行", new String[]{"95537"}),
    NJCB(27, "NJCB", "南京银行", new String[]{"95302"}),
    YNRCC(28, "YNRCC", "云南农信", new String[]{"96500"}),
    LZCCB(29, "LZCCB", "柳州银行", new String[]{"0772-96289"}),
    JINZHOUBANK(30, "JINZHOUBANK", "锦州银行", new String[]{"96178"}),
    BSB(31, "BSB", "包商银行", new String[]{"95352"}),
    BOCD(32, "BOCD", "成都银行", new String[]{"028-96511"}),
    ZYBANK(33, "ZYBANK", "中原银行", new String[]{"95186"}),
    TFBANK(34, "TFBANK", "天府银行", new String[]{"028-96869"}),
    BJRCB(35, "BJRCB", "北京农商", new String[]{"96198"}),
    FJHXBANK(36, "FJHXBANK", "福建海峡银行", new String[]{"4008939999"}),
    LZBANK(37, "LZBANK", "兰州银行", new String[]{"4008896799"}),
    LSB(38, "LSB", "临商银行", new String[]{"96588"}),
    HKB(39, "HKB", "汉口银行", new String[]{"027-96558"}),
    WHCCB(40, "WHCCB", "威海市商业银行", new String[]{"40000-96636"}),
    GRC(41, "GRC", "广州农村商业银行", new String[]{"95313"}),
    SHENGJING(42, "SHENGJING", "盛京银行", new String[]{"95337"}),
    WHRCB(43, "WHRCB", "武汉农村商业银行", new String[]{"95367"}),
    GXBBG(44, "GXBBG", "广西北部湾银行", new String[]{"0771-96288"}),
    JXBANK(45, "JXBANK", "江西银行", new String[]{"956055"}),
    BANKCZ(46, "BANKCZ", "沧州银行", new String[]{"96328"}),
    HSBANK(47, "HSBANK", "徽商银行", new String[]{"96588"}),
    BOIMC(48, "BOIMC", "内蒙古银行", new String[]{"40005-96019"}),
    GX966888(49, "GX966888", "广西农村信用社", new String[]{"966888"}),
    ICCB(50, "ICCB", "廊坊银行", new String[]{"4006200099"}),
    CQCBANK(51, "CQCBANK", "重庆银行", new String[]{"023-96899"}),
    QSBANK(52, "QSBANK", "齐商银行", new String[]{"400-86-96588"}),
    DYCCB(53, "DYCCB", "东营银行", new String[]{"0546-96588"});

    private int code;
    private String log;
    private String desc;
    private String[] senders;

    BankEnums(int code, String log, String desc, String[] senders) {
        this.code = code;
        this.log = log;
        this.desc = desc;
        this.senders = senders;
    }

    public static BankEnums findByLog(String log) {
        if (StringUtils.isBlank(log))
            return null;
        log = log.trim();
        for (BankEnums bank : BankEnums.values())
            if (StringUtils.equals(bank.getLog(), log))
                return bank;
        return null;
    }

    public static BankEnums findDesc(String desc) {
        if (StringUtils.isBlank(desc))
            return null;
        desc = desc.trim();
        for (BankEnums bank : BankEnums.values())
            if (StringUtils.equals(bank.getDesc(), desc))
                return bank;
        return null;
    }

    public static BankEnums findCode(Integer code) {
        if (code == null)
            return null;
        for (BankEnums bank : BankEnums.values())
            if (bank.getCode() == code)
                return bank;
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getLog() {
        return log;
    }

    public String getDesc() {
        return desc;
    }

    public String[] getSenders() {
        return senders;
    }
}
