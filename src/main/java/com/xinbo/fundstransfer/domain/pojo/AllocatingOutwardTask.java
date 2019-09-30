package com.xinbo.fundstransfer.domain.pojo;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.enums.TaskType;
import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Created by 000 on 2017/11/23.
 */
public class AllocatingOutwardTask {

    /**
     * 出款请求ID
     */
    private Long reqId;
    /**
     * 盘口ID
     */
    private Integer handicap;
    /**
     * 层级ID
     */
    private Integer level;
    /**
     * 区域
     */
    private Integer zone;
    /**
     * 会员提现银行卡号
     */
    private String memBank;
    /**
     * 请求出款金额
     */
    private Float reqAmount;
    /**
     * 子任务ID
     */
    private Long taskId;
    /**
     * 子任务出款金额
     */
    private Float taskAmount;

    private Integer firstOut;

    private String msg;

    private Integer fromId;

    private Integer operator;

    private String target;

    private Integer manualOut = 0;

    private String toAccountBank = StringUtils.EMPTY;

    private Long createTime;

    /**
     * 任务类型 0：会员出款 1：代理返利
     */
    private Integer taskType;

    public AllocatingOutwardTask() {
    }

    /**
     * 描述:该方法用于将出款任务组装成一定的格式
     * 出款请求id:盘口id:层级id:区域编码{@link BizHandicap#getZone()}:会员收款账号:订单金额:任务id:任务金额:首次出款标志:是否人工出款:会员收款银行:订单创建时间:任务类别{@link TaskType}
     *
     * @param zone      区域值 区分马尼拉 台湾 盘口表biz_handicap里zone字段值为空则表示该记录的id是其他记录的zone值
     * @param r         出款请求订单信息
     * @param t         出款任务信息
     * @param firstout  是否首次出款
     * @param manualout 是否人工出款
     * @param thirdout  是否第三方出款
     * @return
     */
    public static String genMsg(int zone, BizOutwardRequest r, BizOutwardTask t, boolean firstout, boolean manualout,
                                boolean thirdout) {
        String id = r.getId() == null ? StringUtils.EMPTY : r.getId().toString();
        String handicap = r.getHandicap() == null ? StringUtils.EMPTY : r.getHandicap().toString();
        String level = r.getLevel() == null ? StringUtils.EMPTY : r.getLevel().toString();
        String zoneStr = String.valueOf(zone);
        String toAcc = StringUtils.trimToEmpty(r.getToAccount());
        String rAmount = r.getAmount() == null ? StringUtils.EMPTY : r.getAmount().toString();
        String tId = t.getId() == null ? StringUtils.EMPTY : t.getId().toString();
        String tAmount = t.getAmount() == null ? "0" : t.getAmount().toString();
        String firstOut = firstout ? "1" : "0";
		boolean isManual = CommonUtils.checkOutWardBankTypeKeywordsFilter(r.getToAccountBank());
        String manualOut = StringUtils.EMPTY + AllocateOutwardTaskService.ROBOT_OUT_YES;
        if (thirdout) {
            manualOut = StringUtils.EMPTY + AllocateOutwardTaskService.THIRD_OUT_YES;
        } else if (manualout || firstout || isManual) {
            manualOut = StringUtils.EMPTY + AllocateOutwardTaskService.MANUAL_OUT_YES;
        }
        String toAccountBank = StringUtils.trimToEmpty(r.getToAccountBank());
        toAccountBank = toAccountBank.replaceAll(":", StringUtils.EMPTY);
        String createTime = String
                .valueOf(Objects.isNull(r.getCreateTime()) ? System.currentTimeMillis() : r.getCreateTime().getTime());
        return String.format("%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s", id, handicap, level, zoneStr, toAcc, rAmount,
                tId, tAmount, firstOut, manualOut, toAccountBank, createTime, TaskType.MemberOutTask.getTypeId());

    }

    /**
     * 存储在redis的待分配任务： 格式:
     * 请求id:盘口id:层级id:区域编码:收款账号:出款请求金额:出款任务id:出款任务金额:首次出款标志:是否人工出款:收款银行:创建时间:任务类别
     * ,如
     * 26662044:6:295:27:6214620621004231:100.00:40495617:100.00:0:0:收款银行:1543477326000:1
     *
     * @param r
     * @return
     */
    public static String genMsg(int zone, BizAccountRebate r, boolean manualout) {
        String tid = r.getTid() == null ? StringUtils.EMPTY : r.getTid().toString();
        String handicap = r.getHandicap() == null ? StringUtils.EMPTY : r.getHandicap().toString();
        String level = "999"; // 层级暂时用999，分配的时候会指定层级这里只是使之不为空
        String zoneStr = String.valueOf(zone);
        String toAcc = StringUtils.trimToEmpty(r.getToAccount());
        String rAmount = r.getAmount() == null ? StringUtils.EMPTY : r.getAmount().toString();
        String id = r.getId() == null ? StringUtils.EMPTY : r.getId().toString();
        String tAmount = r.getAmount() == null ? "0" : r.getAmount().toString();
        String firstOut = "0";
        String manualOut = StringUtils.EMPTY + AllocateOutwardTaskService.ROBOT_OUT_YES;
        if (manualout) {
            manualOut = StringUtils.EMPTY + AllocateOutwardTaskService.MANUAL_OUT_YES;
        }
        String toAccountBank = StringUtils.trimToEmpty(r.getToAccountType());
        toAccountBank = toAccountBank.replaceAll(":", StringUtils.EMPTY);
        String createTime = String
                .valueOf(Objects.isNull(r.getCreateTime()) ? System.currentTimeMillis() : r.getCreateTime().getTime());
        return String.format("%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s", tid, handicap, level, zoneStr, toAcc, rAmount,
                id, tAmount, firstOut, manualOut, toAccountBank, createTime, TaskType.RebateTask.getTypeId());
    }

    public AllocatingOutwardTask(String msg) {
        if (StringUtils.isBlank(msg)) {
            return;
        }
        this.msg = StringUtils.trimToEmpty(msg);
        String[] info = msg.split(":");
        int l = info.length;
        this.taskType = 0;
        for (int index = 0; index < l; index++) {
            if (index == 0) {
                this.reqId = StringUtils.isBlank(info[0]) ? null : Long.valueOf(info[0]);
            } else if (index == 1) {
                this.handicap = StringUtils.isBlank(info[1]) ? null : Integer.valueOf(info[1]);
            } else if (index == 2) {
                this.level = StringUtils.isBlank(info[2]) ? null : Integer.valueOf(info[2]);
            } else if (index == 3) {
                this.zone = StringUtils.isBlank(info[3]) ? null : Integer.valueOf(info[3]);
            } else if (index == 4) {
                this.memBank = info[4];
            } else if (index == 5) {
                this.reqAmount = StringUtils.isBlank(info[5]) ? null : Float.valueOf(info[5]);
            } else if (index == 6) {
                this.taskId = StringUtils.isBlank(info[6]) ? null : Long.valueOf(info[6]);
            } else if (index == 7) {
                this.taskAmount = StringUtils.isBlank(info[7]) ? null : Float.valueOf(info[7]);
            } else if (index == 8) {
                this.firstOut = StringUtils.isBlank(info[8]) ? 1 : Integer.valueOf(info[8]);
            } else if (index == 9) {
                this.manualOut = StringUtils.isBlank(info[9]) ? 1 : Integer.valueOf(info[9]);
            } else if (index == 10) {
                this.toAccountBank = StringUtils.trimToEmpty(info[10]);
            } else if (index == 11) {
                this.createTime = Long.valueOf(info[11]);
            } else if (l == 13 && index == 12) {
                this.taskType = StringUtils.isBlank(info[12]) ? 0 : Integer.valueOf(info[12]);
            }
        }
    }

    public String getMsg() {
        return msg;
    }

    public Long getReqId() {
        return reqId;
    }

    public void setReqId(Long reqId) {
        this.reqId = reqId;
    }

    public Integer getHandicap() {
        return handicap;
    }

    public void setHandicap(Integer handicap) {
        this.handicap = handicap;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getZone() {
        return zone;
    }

    public void setZone(Integer zone) {
        this.zone = zone;
    }

    public String getMemBank() {
        return memBank;
    }

    public void setMemBank(String memBank) {
        this.memBank = memBank;
    }

    public Float getReqAmount() {
        return reqAmount;
    }

    public void setReqAmount(Float reqAmount) {
        this.reqAmount = reqAmount;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Float getTaskAmount() {
        return taskAmount;
    }

    public void setTaskAmount(Float taskAmount) {
        this.taskAmount = taskAmount;
    }

    public Integer getFromId() {
        return fromId;
    }

    public void setFromId(Integer fromId) {
        this.fromId = fromId;
    }

    public Integer getFirstOut() {
        return firstOut;
    }

    public void setFirstOut(Integer firstOut) {
        this.firstOut = firstOut;
    }

    public Integer getOperator() {
        return operator;
    }

    public void setOperator(Integer operator) {
        this.operator = operator;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Integer getManualOut() {
        return manualOut;
    }

    public void setManualOut(Integer manualOut) {
        this.manualOut = manualOut;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getToAccountBank() {
        return toAccountBank;
    }

    public void setToAccountBank(String toAccountBank) {
        this.toAccountBank = toAccountBank;
    }

    public Long getCreateTime() {
        return Objects.isNull(createTime) ? System.currentTimeMillis() : createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Integer getTaskType() {
        return taskType;
    }

    public void setTaskType(Integer taskType) {
        this.taskType = taskType;
    }

    @Override
    public String toString() {
        return this.getMsg();
    }
}
