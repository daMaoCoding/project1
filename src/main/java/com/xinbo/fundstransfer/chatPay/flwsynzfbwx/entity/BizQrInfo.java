package com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "biz_qr_info")
@Data
public class BizQrInfo  implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * 主键
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", insertable = false, nullable = false)
  private Long id;

  /**
   * 二维码ID唯一，系统生成，名称(PT-UID-0- ###/FLW-UID-1-####) 格式：(平台/返利网-用户id-二维码类型01-#####)
   */
  @Column(name = "qr_id", nullable = false)
  private String qrId;

  /**
   * 二维码来源(0返利网，1平台)
   */
  @Column(name = "qr_from")
  private Integer qrFrom;

  /**
   * 二维码所属人id(返利网兼职/平台会员)
   */
  @Column(name = "uid")
  private String uid;

  /**
   * 二维码所属人账号(返利网兼职/平台会员 )
   */
  @Column(name = "uname")
  private String uname;

  /**
   * 盘口id
   */
  @Column(name = "handicap_code")
  private Integer handicapCode;

  /**
   * 二维码类型(0微信收款码，1支付宝收款码)
   */
  @Column(name = "qr_type")
  private Integer qrType;

  /**
   * 二维码状态(1正常，0停用 。 默认1)
   */
  @Column(name = "qr_status")
  private Integer qrStatus;

  /**
   * 二维码内容(解析后的内容)
   */
  @Column(name = "qr_content")
  private String qrContent;

  /**
   * 支付宝或微信账号
   */
  @Column(name = "account")
  private String account;

  /**
   * 支付宝或微信-真实姓名
   */
  @Column(name = "name")
  private String name;

  /**
   * 二维码所属人 省份id
   */
  @Column(name = "province_id")
  private Long provinceId;

  /**
   * 二维码所属人 市id
   */
  @Column(name = "city_id")
  private Long cityId;

  /**
   * 返利网或平台请求创建(验证)时间
   */
  @Column(name = "create_time")
  private Date createTime;

  /**
   * 二维码验证设备取走任务时间
   */
  @Column(name = "val_qr_job_time")
  private Date valQrJobTime;

  /**
   * 验证设备返回验证结果时间
   */
  @Column(name = "val_qr_back_time")
  private Date valQrBackTime;

  /**
   * 二维码验证设备id
   */
  @Column(name = "val_devices_id")
  private String valDevicesId;

  /**
   * 0新生成，1任务取走验证中，2验证成功 ，3验证失败
   */
  @Column(name = "val_qr_status")
  private Integer valQrStatus;

  /**
   * 1.通知验证结果成功，0.通知验证结果失败
   */
  @Column(name = "val_qr_notif_status")
  private Integer valQrNotifStatus;

  
}