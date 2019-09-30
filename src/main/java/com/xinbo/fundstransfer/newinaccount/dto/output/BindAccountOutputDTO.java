package com.xinbo.fundstransfer.newinaccount.dto.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonDTO;
import com.xinbo.fundstransfer.newinaccount.dto.input.InAccountInfoDTO;
import lombok.Data;
import lombok.ToString;

import javax.naming.directory.SearchResult;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class BindAccountOutputDTO extends CommonDTO implements Serializable {
	List<InAccountInfoDTO> accountList;// 在用的银行卡信息
}
