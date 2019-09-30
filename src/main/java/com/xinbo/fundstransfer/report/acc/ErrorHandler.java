package com.xinbo.fundstransfer.report.acc;

import com.xinbo.fundstransfer.report.ActionDeStruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
public class ErrorHandler extends ApplicationObjectSupport {
	private static final Map<String, Error> dealMap = new LinkedHashMap<>();

	@PostConstruct
	public void init() {
		super.getApplicationContext().getBeansWithAnnotation(ErrorUp.class)
				.forEach((k, v) -> dealMap.put(k, (Error) v));
	}

	public String handle(SysAccPush entity, List<ActionDeStruct> actionDeStructList) {
		if (Objects.isNull(entity) || entity.getTarget() == 0 || Objects.isNull(entity.getData()))
			return StringUtils.EMPTY;
		return dealMap.get(Error.ERROR_ACC + entity.getClassify()).deal(entity.getErrorId(), entity.getTarget(),
				entity.getData(), entity.getRemark(), entity.getOperator(),
				new String[] { entity.getOrderNo(), entity.getOrderType(), entity.getErrSt() }, actionDeStructList);
	}
}
