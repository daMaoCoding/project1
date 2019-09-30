package com.xinbo.fundstransfer.restful.v3.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RequestTestIncomeInputDTO implements Serializable {
    @NotBlank
    String handicap;
    @NotBlank
    String orderNo;
    @NotNull
    BigDecimal amount;
    @NotNull
    @Range(min = 1, max = 4)
    Integer type;
    @NotBlank
    String createTime;
    @NotBlank
    String toAccount;
    @NotBlank
    String username;
    @NotBlank
    String level;
    // @NotBlank
    String realname;
    String ackTime;
    // 以下是内部使用属性
    String remark;
    String fromAccount;
    Integer operator;
    String fee;
    Integer fromId;
    Integer toId;
    String toAccountBank;
}
