package com.xinbo.fundstransfer.report.up;

import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import java.util.List;

@FunctionalInterface
public interface ReportCheckNoneInit {

	List<SysBalTrans> ifAbsent(int target);
}
