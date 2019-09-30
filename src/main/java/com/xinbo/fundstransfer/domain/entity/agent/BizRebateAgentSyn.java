package com.xinbo.fundstransfer.domain.entity.agent;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 来自返利网的代理&子账号关系
 */
@Data
@Slf4j
@Entity
@Table(name = "biz_rebate_agent_syn")
public class BizRebateAgentSyn implements Serializable {
  private static final long serialVersionUID = 1L;

  @Transient
  private String token;


  @Id
  @Column(name = "id", insertable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 兼职id-母账号userid
   */
  @Column(name = "uid")
  private String uid;

  /**
   * 是否是代理(满足条件：1，非代理不满足条件0)
   */
  @Column(name = "is_agent")
  private Boolean isAgent;

  /**
   * 代理类型(1,合并账号计算信用额度，2子账号独立计算信用额度)
   */
  @Column(name = "agent_type")
  private Integer agentType;

  /**
   * 子账号信息(List [{uid:子账号id,mobile:子账号手机，userName:子账号登陆名}])
   */
  @Column(name = "agent_sub_users")
  private String agentSubUsers;

  /**
   * 创建时间
   */
  @Column(name = "creat_time")
  private Date creatTime;

  /**
   * 更新时间
   */
  @Column(name = "update_time")
  private Date updateTime = new Date();


  @Data
  public static class AgentSubUsers {
    private String uid;
  }

    /**
     * 获取第一级 子代理uid
     */
  public List<AgentSubUsers> getAgentSubUserIds(){
    try {
      if(StringUtils.isNotBlank(this.agentSubUsers))
        return JSON.parseObject(this.agentSubUsers, new TypeReference<List<AgentSubUsers>>() {});
      return Lists.newArrayList();
    }catch (Exception e){
      return Lists.newArrayList();
    }
  }





}