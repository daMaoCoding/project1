currentPageLocation = window.location.href;
//匹配确认弹出框 需与commin_getInfo.js同时调用
/**
 * 公用匹配方法 默认无盘口、层级
 * fnName:操作完毕后需要刷新的方法名
 */
var showConfirmMatchModal=function(incomeId,bankId,fnName){
	if(!incomeId||!bankId){
		showMessageForFail((incomeId?"请选择流水记录！":"请选择提单记录"));
		return;
	}
	var incomeInfo=getIncomeInfoById(incomeId);
	var bankInfo=getBankLogById(bankId);
	if(Math.abs((Math.abs(incomeInfo.amount)-Math.abs(bankInfo.amount))/Math.abs(bankInfo.amount))>0.01*sysSetting.INCOME_PERCENT){
    	showMessageForFail("金额浮动率过高，不允许匹配！银行流水匹配浮动比例为："+sysSetting.INCOME_PERCENT+"%");
		return;
	}
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#confirmMatchModal").clone().appendTo($("body"));
			var tbody="";
			if(incomeInfo){
				var tr="";
				tr+="<td><span>系统记录</span></td>";
				tr+="<td><span>"+incomeInfo.id+"</span></td>";
				tr+="<td><span>"+hideAccountAll(incomeInfo.fromAccount)+"</span></td>";
				tr+="<td><span>"+hideAccountAll(incomeInfo.toAccount)+"</span></td>";
				tr+="<td><span>"+timeStamp2yyyyMMddHHmmss(incomeInfo.createTime)+"</span></td>";
				tr+="<td><span>"+incomeInfo.amount+"</span></td>";
				if(incomeInfo.fee){
					tr+="<td><span>"+incomeInfo.fee+"</span></td>";
				}else{
					tr+="<td></td>";
				}
				tbody+="<tr>"+tr+"</tr>";
			}
			if(bankInfo){
				var tr="";
				tr+="<td><span>银行流水</span></td>";
				tr+="<td><span>"+bankInfo.id+"</span></td>";
				tr+="<td><span>"+hideAccountAll(bankInfo.toAccount==incomeInfo.toAccount?bankInfo.fromAccountNO:bankInfo.toAccount)+"</span></td>";
				tr+="<td><span>"+hideAccountAll(bankInfo.toAccount==incomeInfo.toAccount?bankInfo.toAccount:bankInfo.fromAccountNO)+"</span></td>";//银行流水的to存的是字符串
				tr+="<td><span>"+bankInfo.tradingTimeStr+"</span></td>";
				tr+="<td><span>"+bankInfo.amount+"</span></td>";
				tr+="<td></td>";
				tbody+="<tr>"+tr+"</tr>";
			}
			$div.find("tbody").html(tbody);
			$div.find("#doMatch").bind("click",function(){
				var $remark=$div.find("#remark");
			    if(!validateEmptyBatch([{ele:$remark,name:'备注'}])){
			    	return;
			    }
				$.ajax({
					type:"PUT",
					url:API.r_income_match,
					async:false,
					dataType:'json',
					data:{
						"flowId":bankId,
						"incomeReqId":incomeId,
						"remark":$.trim($remark.val()),
						"orderNo":0,"handicapId":0,"memberCode":0,"accountId":0
					},
					success:function(jsonObject){
						if(jsonObject.status == 1){
							showMessageForSuccess("匹配成功");
							$div.modal("toggle");
						}else{
							showMessageForFail("操作失败:"+jsonObject.message);
						}
						//刷新数据
						if(fnName){
							fnName();
						}
					}
				});
			});
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}

var showReject2TransferModal = function(incomeId,fnName){
	if(!incomeId){
		showMessageForFail("请选择提单记录");
		return;
	}
	$("body").find("#confirmMatchModal").remove();
	var incomeInfo=getIncomeInfoById(incomeId);
	$.ajax({type:"GET", async:false, dataType:'html', url : "/"+sysVersoin+"/html/common/showInfoModal.html", success : function(html){
		var $div=$(html).find("#confirmMatchModal").clone().appendTo($("body"));
		$div.find("tbody").html("<tr>\
			<td><span>"+hideAccountAll(incomeInfo.fromAccount)+"</span></td><td><span>"+hideAccountAll(incomeInfo.toAccount)+"</span></td><td><span>"+timeStamp2yyyyMMddHHmmss(incomeInfo.createTime)+"</span></td>\
			<td><span>"+incomeInfo.amount+"</span></td><td><span>"+(incomeInfo.fee?incomeInfo.fee:0)+"</span></td></tr>");
		$div.find("thead th").each(function(){
			var t = $(this);
			if(t.text()=='流水号'||t.text()=='数据类型'||t.text()=='ID'){
				t.remove();
			}
		});
		$div.find("#accountTitle").text("驳回记录");
		$div.find("#doMatch").text("确定");
		$div.find("#doMatch").bind("click",function(){
			$.ajax({type:"PUT",url:API.r_income_reject2CurrSys,async:false,dataType:'json',data:{"incomeRequestId":incomeId,"remark":$.trim($div.find("#remark").val()),"orderNo":0,"handicapId":0,"memberCode":0},success:function(jsonObject){
				if(jsonObject.status == 1){
					$div.modal("toggle");
					showMessageForSuccess("操作成功");
				}else{
					showMessageForFail("操作失败:"+jsonObject.message);
				}
				if(fnName){
					fnName();
				}
			}});
		});
		$div.modal("toggle");
	}});
}

var outwardMatchModal='<div id="confirmMatchModal4Outward" class="modal fade">\
							<div class="modal-dialog modal-lg" style="width:1000px;">\
								<div class="modal-content">\
									<div class="modal-header no-padding text-center">\
										<div class="table-header">\
											<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>匹配确认</span>\
										</div>\
									</div>\
									<div class="modal-body no-margin no-padding">\
										<table class="table table-striped table-bordered table-hover no-margin-bottom" >\
											<thead><tr><th>类型</th><th>汇出账号</th><th>汇入账号</th><th>金额</th><th>手续费</th><th>时间</th><th>流水号</th></tr></thead>\
											<tbody>\
												   <tr><td>出款记录</td><td>{outFromAccount}</td><td>{outToAccount}</td><td>{outAmount}</td><td>{outFee}</td><td>{outTradingTime}</td><td>{outTransactionNo}</td></tr>	\
												   <tr><td>流水记录</td><td>{bankFromAccount}</td><td>{bankToAccount}</td><td>{bankAmount}</td><td>{bankFee}</td><td>{bankTradingTime}</td><td>{bankTransactionNo}</td></tr>\
											 	   <tr>\
											 	   	<td><span class="label label-lg label-purple arrowed-right">备注</span></td>\
											 	   	<td colspan="5"><span class="input-icon"><input type="text" id="remark" style="height:30px;width:600px;"></span></td>\
											 	   	<td><button class="btn btn-primary btn-sm" type="button" id="doMatch">完成匹配</button></td>\
											 	   </tr>\
											</tbody>\
										</table>\
									<div>\
								</div>\
							</div>\
						</div>';

var showConfirmMatchModal4Outward = function(outwardTaskId,bankId,fnName){
	if(!outwardTaskId||!bankId){
		showMessageForFail((outwardTaskId?"请选择流水记录！":"请选择出款记录"));
		return;
	}
	var outInfo =  getOutwardTaskInfoById(outwardTaskId), bankInfo = getBankLogById(bankId);
	if(Math.abs((Math.abs(outInfo.amount)-Math.abs(bankInfo.amount))/Math.abs(bankInfo.amount))>0.01*sysSetting.INCOME_PERCENT){
    	showMessageForFail("金额浮动率过高，不允许匹配！银行流水匹配浮动比例为："+sysSetting.INCOME_PERCENT+"%");
		return;
	}
	var html = fillDataToModel4Item({
		outPayCode :outInfo.taskRemark,outTransactionNo:'',outFromAccount:hideAccountAll(outInfo.fromAccount),outToAccount:hideAccountAll(outInfo.toAccount),outTradingTime:timeStamp2yyyyMMddHHmmss(outInfo.asignTime),outAmount:outInfo.amount,outFee:outInfo.fee,
		bankPayCode:bankInfo.payCode,bankTransactionNo:bankInfo.transactionNo,bankFromAccount:hideAccountAll(bankInfo.fromAccountNO),bankToAccount:hideAccountAll(bankInfo.toAccount),bankTradingTime:timeStamp2yyyyMMddHHmmss(bankInfo.tradingTime),bankAmount:(-1)*bankInfo.amount,bankFee:bankInfo.fee
	},outwardMatchModal);
	var $div = $(html).appendTo($("body"));
	$div.find("#doMatch").bind("click",function(){
		var $remark=$div.find("#remark");
	    if(!validateEmptyBatch([{ele:$remark,name:'备注'}])){
	    	return;
	    }
		$.ajax({type:"PUT",url:API.r_outtask_match,async:false,dataType:'json',data:{"bankFlowId":bankId,"outwardTaskId":outwardTaskId,"remark":$.trim($div.find("#remark").val())}, success:function(jsonObject){
			if(jsonObject.status == 1){
				$div.modal("toggle");
				showMessageForSuccess("匹配成功");
			}else{
				showMessageForFail("操作失败:"+jsonObject.message);
			}
			if(fnName){
				fnName();
			}
		}});
	});
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model
		$div.remove();
	});
}

var rebateMatchModal='<div id="confirmMatchModal4Rebate" class="modal fade">\
							<div class="modal-dialog modal-lg" style="width:1000px;">\
								<div class="modal-content">\
									<div class="modal-header no-padding text-center">\
										<div class="table-header">\
											<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>匹配确认</span>\
										</div>\
									</div>\
									<div class="modal-body no-margin no-padding">\
										<table class="table table-striped table-bordered table-hover no-margin-bottom" >\
											<thead><tr><th>类型</th><th>汇入账号</th><th>金额</th><th>时间</th><th>流水号</th></tr></thead>\
											<tbody>\
												   <tr><td>提现记录</td><td>{outToAccount}</td><td>{outAmount}</td><td>{outTradingTime}</td><td>{outTransactionNo}</td></tr>	\
												   <tr><td>流水记录</td><td>{bankToAccount}</td><td>{bankAmount}</td><td>{bankTradingTime}</td><td>{bankTransactionNo}</td></tr>\
											 	   <tr>\
											 	   	<td><span class="label label-lg label-purple arrowed-right">备注</span></td>\
											 	   	<td colspan="3"><span class="input-icon"><input type="text" id="remark" style="height:30px;width:600px;"></span></td>\
											 	   	<td><button class="btn btn-primary btn-sm" type="button" id="doMatch">完成匹配</button></td>\
											 	   </tr>\
											</tbody>\
										</table>\
									<div>\
								</div>\
							</div>\
						</div>';

var showConfirmMatchModal4Rebate = function(outwardTaskId,bankId,fnName){
	if(!outwardTaskId||!bankId){
		showMessageForFail((outwardTaskId?"请选择流水记录！":"请选择出款记录"));
		return;
	}
	var outInfo =  getRebateById(outwardTaskId), bankInfo = getBankLogById(bankId);
	var html = fillDataToModel4Item({
		outPayCode :outInfo.taskRemark,outTransactionNo:outInfo.tid,outFromAccount:outInfo.fromAccount,outToAccount:outInfo.toAccount,outTradingTime:timeStamp2yyyyMMddHHmmss(outInfo.asignTime),outAmount:outInfo.amount,outFee:outInfo.fee,
		bankPayCode:bankInfo.payCode,bankTransactionNo:bankInfo.transactionNo,bankFromAccount:bankInfo.fromAccountNO,bankToAccount:bankInfo.toAccount,bankTradingTime:timeStamp2yyyyMMddHHmmss(bankInfo.tradingTime),bankAmount:(-1)*bankInfo.amount,bankFee:bankInfo.fee
	},rebateMatchModal);
	var $div = $(html).appendTo($("body"));
	$div.find("#doMatch").bind("click",function(){
		var $remark=$div.find("#remark");
		if(!validateEmptyBatch([{ele:$remark,name:'备注'}])){
			return;
		}
		$.ajax({type:"PUT",url:'/r/rebate/match',async:false,dataType:'json',data:{"bankFlowId":bankId,"rebateId":outwardTaskId,"remark":$.trim($div.find("#remark").val())}, success:function(jsonObject){
			if(jsonObject.status == 1){
				$div.modal("toggle");
				showMessageForSuccess("匹配成功");
			}else{
				showMessageForFail("操作失败:"+jsonObject.message);
			}
			if(fnName){
				fnName();
			}
		}});
	});
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model
		$div.remove();
	});
}

var reject2OutwardTaskModal='<div id="reject2OutwardTaskModal" class="modal fade">\
							<div class="modal-dialog modal-lg" style="width:1000px;">\
								<div class="modal-content">\
									<div class="modal-header no-padding text-center">\
										<div class="table-header">\
											<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>转待排查</span>\
										</div>\
									</div>\
									<div class="modal-body no-margin no-padding">\
										<table class="table table-striped table-bordered table-hover no-margin-bottom" >\
											<thead><tr><th>汇出账号</th><th>汇入账号</th><th>时间</th><th>金额</th><th>手续费</th></tr></thead>\
											<tbody>\
												   <tr><td>{fromAccountHide}</td><td>{toAccountHide}</td><td>{tradingTime}</td><td>{amount}</td><td>{fee}</td></tr>\
											 	   <tr>\
											 	       <td><span class="label label-lg label-purple arrowed-right">备注</span></td>\
											 	   	   <td colspan="3"><span class="input-icon"><input type="text" id="remark" style="height:30px;width:600px;"></span></td>\
											 	   	   <td colspan="2"><button class="btn btn-primary btn-sm" type="button" id="doReject">确定</button></td>\
											 	   </tr>\
											</tbody>\
										</table>\
									<div>\
								</div>\
							</div>\
						</div>';

var showReject2OutwardTaskModal = function(outwardTaskId,fnName){
	$("body").find("#reject2OutwardTaskModal").remove();
	var outInfo =  getOutwardTaskInfoById(outwardTaskId);
	outInfo.taskRemark = outInfo.taskRemark?outInfo.taskRemark:'';
	outInfo.taskRemarkShort = outInfo.taskRemark?outInfo.taskRemark.substring(0,3)+'...':'';
	outInfo.tradingTime=timeStamp2yyyyMMddHHmmss(outInfo.asignTime);
	outInfo.toAccountHide=hideAccountAll(outInfo.toAccount);
	outInfo.fromAccountHide=hideAccountAll(outInfo.fromAccount);
	var html = fillDataToModel4Item(outInfo,reject2OutwardTaskModal);
	var $div = $(html).appendTo($("body"));
	$("[data-toggle='popover']").popover();
	$div.find("#doReject").bind("click",function(){
		var remark = $.trim($div.find("#remark").val());
		if(!remark){
			showMessageForFail("备注不能为空.");
			return;
		}
		$.ajax({type:"PUT",url:API.r_outtask_reject,async:false,dataType:'json',data:{"outwardTaskId":outwardTaskId,"remark":remark}, success:function(jsonObject){
			if(jsonObject.status == 1){
				$div.modal("toggle");
				showMessageForSuccess("操作成功");
			}else{
				showMessageForFail("操作失败:"+jsonObject.message);
			}
			if(fnName){
				fnName();
			}
		}});
	});
	$div.modal("toggle");
}

var reject2RebateTaskModal='<div id="reject2RebateTaskModal" class="modal fade">\
							<div class="modal-dialog modal-lg" style="width:1000px;">\
								<div class="modal-content">\
									<div class="modal-header no-padding text-center">\
										<div class="table-header">\
											<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>转待排查</span>\
										</div>\
									</div>\
									<div class="modal-body no-margin no-padding">\
										<table class="table table-striped table-bordered table-hover no-margin-bottom" >\
											<thead><tr><th>订单号</th><th>汇入账号</th><th>时间</th><th>金额</th></tr></thead>\
											<tbody>\
												   <tr><td>{tid}</td><td>{toAccountInfo}</td><td>{tradingTime}</td><td>{amount}</td></tr>\
											 	   <tr>\
											 	       <td><span class="label label-lg label-purple arrowed-right">备注</span></td>\
											 	   	   <td colspan="2"><span class="input-icon"><input type="text" id="remark" style="height:30px;width:600px;"></span></td>\
											 	   	   <td><button class="btn btn-primary btn-sm" type="button" id="doReject">确定</button></td>\
											 	   </tr>\
											</tbody>\
										</table>\
									<div>\
								</div>\
							</div>\
						</div>';

var showReject2RebateTaskModal = function(outwardTaskId,fnName){
	$("body").find("#reject2RebateTaskModal").remove();
	var outInfo =  getRebateById(outwardTaskId);
	outInfo.toAccountInfo = outInfo.toAccount.substring(0,4)+"..."+outInfo.toAccount.substring(outInfo.toAccount.length-4,outInfo.toAccount.length);
	outInfo.taskRemarkShort = outInfo.taskRemark?outInfo.taskRemark.substring(0,3)+'...':'';
	outInfo.tradingTime=timeStamp2yyyyMMddHHmmss(outInfo.asignTime);
	var html = fillDataToModel4Item(outInfo,reject2RebateTaskModal);
	var $div = $(html).appendTo($("body"));
	$("[data-toggle='popover']").popover();
	$div.find("#doReject").bind("click",function(){
		var remark = $.trim($div.find("#remark").val());
		if(!remark){
			showMessageForFail("备注不能为空.");
			return;
		}
		$.ajax({type:"PUT",url:'/r/rebate/reject',async:false,dataType:'json',data:{"id":outwardTaskId,"remark":remark}, success:function(jsonObject){
			if(jsonObject.status == 1){
				$div.modal("toggle");
				showMessageForSuccess("操作成功");
			}else{
				showMessageForFail("操作失败:"+jsonObject.message);
			}
			if(fnName){
				fnName();
			}
		}});
	});
	$div.modal("toggle");
}

var matchWithoutBankLog2OutwardTaskModal='<div id="matchWithoutBankLog2OutwardTaskModal" class="modal fade">\
							<div class="modal-dialog modal-lg" style="width:1000px;">\
								<div class="modal-content">\
									<div class="modal-header no-padding text-center">\
										<div class="table-header">\
											<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>转完成</span>\
										</div>\
									</div>\
									<div class="modal-body no-margin no-padding">\
										<table class="table table-striped table-bordered table-hover no-margin-bottom" >\
											<thead><tr><th>汇出账号</th><th>汇入账号</th><th>时间</th><th>金额</th><th>手续费</th></tr></thead>\
											<tbody>\
												   <tr><td>{fromAccount}</td><td>{toAccount}</td><td>{tradingTime}</td><td>{amount}</td><td>{fee}</td></tr>\
											 	   <tr>\
											 	       <td><span class="label label-lg label-purple arrowed-right">备注</span></td>\
											 	   	   <td colspan="3"><span class="input-icon"><input type="text" id="remark" style="height:30px;width:600px;"></span></td>\
											 	   	   <td colspan="2"><button class="btn btn-primary btn-sm" type="button" id="doMatchWithoutBankLog">确定</button></td>\
											 	   </tr>\
											</tbody>\
										</table>\
									<div>\
								</div>\
							</div>\
						</div>';

var showMatchWithoutBankLogModal4Outward = function(outwardTaskId,fnName){
	$("body").find("#matchWithoutBankLog2OutwardTaskModal").remove();
	var outInfo =  getOutwardTaskInfoById(outwardTaskId);
	outInfo.taskRemark = outInfo.taskRemark?outInfo.taskRemark:'';
	outInfo.taskRemarkShort = outInfo.taskRemark?outInfo.taskRemark.substring(0,3)+'...':'';
	outInfo.tradingTime=timeStamp2yyyyMMddHHmmss(outInfo.asignTime);
	var html = fillDataToModel4Item(outInfo,matchWithoutBankLog2OutwardTaskModal);
	var $div = $(html).appendTo($("body"));
	$("[data-toggle='popover']").popover();
	$div.find("#doMatchWithoutBankLog").bind("click",function(){
		var remark = $.trim($div.find("#remark").val());
		if(!remark){
			showMessageForFail("备注不能为空.");
			return;
		}
		$.ajax({type:"PUT",url:API.r_outtask_matchWithoutBankLog,async:false,dataType:'json',data:{"outwardTaskId":outwardTaskId,"remark":remark}, success:function(jsonObject){
			if(jsonObject.status == 1){
				$div.modal("toggle");
				showMessageForSuccess("操作成功");
			}else{
				showMessageForFail("操作失败:"+jsonObject.message);
			}
			if(fnName){
				fnName();
			}
		}});
	});
	$div.modal("toggle");
}
