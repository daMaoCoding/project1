package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizThirdAccountInputDTO implements Serializable {
	private Integer id;
	// @NotNull(message = "状态不能为空")
	private Byte status;
	@NotBlank(message = "登陆账号不能为空")
	private String loginAccount;
	@NotBlank(message = "登陆密码不能为空")
	private String loginPass;
	@NotBlank(message = "支付密码不能为空")
	private String payPass;
	@NotNull(message = "关联的账号id不能为空")
	private Integer accountId;
	@NotBlank(message = "链接地址不能为空")
	private String thirdNameUrl;

	private String createTime;
	private SysUser sysUser;

}
