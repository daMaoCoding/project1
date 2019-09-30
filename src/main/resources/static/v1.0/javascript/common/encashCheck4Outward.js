currentPageLocation = window.location.href;
var request=getRequest(); //fromAccountId outwardTaskStatus
request.outwardTaskStatus=request.outwardTaskStatus?request.outwardTaskStatus:outwardTaskStatusDeposited;
$("#pageSizeFive").removeClass("checked").on("click",function(){
	searchByFilter(1);
});

var mousedown4EncashCheckRadio = function(obj){
	var radioChecked = $(obj).prop("checked");
	$(obj).prop('checked', !radioChecked);
	return false;
}

var clickRebateBtn = function(){
	var outwardstatus =  $('li.tabli4outward.active').attr('outwardtaskstatus');
	searchByFilter(outwardstatus);
};

var searchByFilter=function(outwardTaskStatus){
	if(outwardTaskStatus||outwardTaskStatus==outwardTaskStatusDeposited){
		$("#outwardTaskStatus").val(outwardTaskStatusDeposited);
	}else{
		outwardTaskStatus = $("div[name='pageContent'] ul.nav-tabs li.active").attr("outwardTaskStatus");
	}
	if(outwardTaskStatus==outwardTaskStatusDeposited){
		var noneChecked=true;
		$("input[name='searchType']:checked").each(function(){
			if($(this).val()=='sys'){
				noneChecked=false;
				packageOutward4DepositedTable();
			}else if($(this).val()=='bank'){
				noneChecked=false;
				packBankFlow4MatchingTable();
			}
		});
		if(noneChecked){
			packageOutward4DepositedTable();
			packBankFlow4MatchingTable();
		}
	}else if(outwardTaskStatus==outwardTaskStatusMatched){
		packBankFlow4MatchedTable();
	}else if(outwardTaskStatus==outwardTaskStatusFailure){
		packageOutward4UnmatchedTable();
	}
}

var match = function(){
	var sysRecordId = $("table[name='tableIncome']").find("tr td input[name='sysRecordId']:checked").val();
	var bankLogId   = $("table[name='tableBank']").find("tr td input[name='bankLogId']:checked").val();
	var rebateDataOnly = $("#rebateDataOnly").prop("checked");
	if(rebateDataOnly){
		showConfirmMatchModal4Rebate(sysRecordId,bankLogId,function(){
			packageOutward4DepositedTable();
			packBankFlow4MatchingTable();
		});
	}else{
		showConfirmMatchModal4Outward(sysRecordId,bankLogId,function(){
			packageOutward4DepositedTable();
			packBankFlow4MatchingTable();
		});
	}
}

var matchWithoutBankLog = function(outwardTaskId){
	showConfirmMatchModal4Outward(sysRecordId,bankLogId,function(){
		packageOutward4DepositedTable();
		packBankFlow4MatchingTable();
	});
}

var matchWithoutBankLog = function (outwardTaskId){
	showMatchWithoutBankLogModal4Outward(outwardTaskId,function(){
		packageOutward4DepositedTable();
		packBankFlow4MatchingTable();
	});
}

var reject = function(outwardTaskId){
	var rebateDataOnly = $("#rebateDataOnly").prop("checked");
	if(rebateDataOnly){
		showReject2RebateTaskModal(outwardTaskId,function(){
			packageOutward4DepositedTable();
		});
	}else{
		showReject2OutwardTaskModal(outwardTaskId,function(){
			packageOutward4DepositedTable();
		});
	}
}

var packageSearchData4Outward_Deposited = function(CurPage){
	var $tab = $("div[name='pageContent'] div[id='tab"+outwardTaskStatusDeposited+"']");
	var id = $tab.find("input[name='id']").val();
	var toAccount = $tab.find("input[name='toAccount']").val();
	var minAmount = $tab.find("input[name='minAmount']").val();
	var maxAmount = $tab.find("input[name='maxAmount']").val();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = $tab.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	var pageSize=$.session.get('initPageSize');
	if($("#outwardTaskStatus").val()==outwardTaskStatusDeposited&&$("#pageSizeFive").prop("checked")){
		pageSize=5;
	}
	return {"pageNo":(CurPage<=0?0:CurPage-1),"pageSize":pageSize,"fromAccountId":request.fromAccountId,"toAccount":toAccount,
		     "startAndEndTimeToArray":startAndEndTimeToArray.toString(), "status":outwardTaskStatusDeposited,"id":id,"minAmount":minAmount,"maxAmount":maxAmount};
}

var packageSearchData4Outward_Unmatched = function(CurPage){
	var $tab = $("div[name='pageContent'] div[id='tab"+outwardTaskStatusFailure+"']");
	var toAccount = $tab.find("input[name='toAccount']").val();
	var minAmount = $tab.find("input[name='minAmount']").val();
	var maxAmount = $tab.find("input[name='maxAmount']").val();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = $tab.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	return {"pageNo":(CurPage<=0?0:CurPage-1),"fromAccountId":request.fromAccountId,"toAccount":toAccount,
		"startAndEndTimeToArray":startAndEndTimeToArray.toString(), "status":outwardTaskStatusFailure,"minAmount":minAmount,"maxAmount":maxAmount};

};

var packageSearchData4BankLog=function(CurPage){
	var tab = $("div[name='pageContent'] div[id='tab"+outwardTaskStatusDeposited+"']");
	var id = tab.find("input[name='id']").val();
	var toAccount = tab.find("input[name='toAccount']").val();
	var minAmount = tab.find("input[name='minAmount']").val();
	var maxAmount = tab.find("input[name='maxAmount']").val();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = tab.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	var pageSize=$.session.get('initPageSize');
	if($("#outwardTaskStatus").val()==outwardTaskStatusDeposited&&$("#pageSizeFive").prop("checked")){
		pageSize=5;
	}
	return {"pageNo":(CurPage<=0?0:CurPage-1),"pageSize":pageSize,"fromAccount":request.fromAccountId,"toAccount":toAccount,
		"startAndEndTimeToArray":startAndEndTimeToArray.toString(), "search_EQ_status":bankLogStatusMatching,
		"id":id,"minAmount":minAmount,"maxAmount":maxAmount,"searchTypeIn0Out1":1};
}

var packageSearchData4Matched = function(CurPage){
	var $tab = $("div[name='pageContent'] div[id='tab"+outwardTaskStatusMatched+"']");
	var toAccount = $tab.find("input[name='toAccount']").val();
	var minAmount = $tab.find("input[name='minAmount']").val();
	var maxAmount = $tab.find("input[name='maxAmount']").val();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = $tab.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	return {"pageNo":(CurPage<=0?0:CurPage-1),"pageSize":$.session.get('initPageSize')?$.session.get('initPageSize'):10,"fromAccountId":request.fromAccountId,"toAccount":toAccount,
		"startAndEndTimeToArray":startAndEndTimeToArray.toString(), "status":outwardTaskStatusMatched,"minAmount":minAmount,"maxAmount":maxAmount};

};

var modelHtml4OutwardMatching = '\
		<tr>\
			<td><input type="radio" name="sysRecordId" value="{outwardTaskId}" style="width:18px;height:18px;cursor: pointer;" onmousedown="mousedown4EncashCheckRadio(this)" onclick="return false;"></td>\
			<td><span>{memberUserName}</span></td><td><span>{orderNo}</span></td>\
			<td><a class="bind_hover_card" data-toggle="accountInfoHover{fromAccountId}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td>\
			<td><span>{toAccountStar}</span></td><td><span>{toAccountOwner}</span></td><td><span>{asignTime}</span></td>\
			<td><span>{amount}</span></td><td><span>{taskOperatorUid}</span></td>\
			<td><a width:40px; overflow:hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis; title="{taskRemark}">{taskRemarkShort}</a></td>\
			<td><a href="javascript:void(0);"  class="{hideImg}" onclick="showCaptureImg(\'{screenshot}\')">{captureFlag}</a></td>\
			<td>\
				<label class="inline"><button type="button" onclick="reject({outwardTaskId})" class="btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa fa-exclamation-circle bigger-100 red"></i>转待排查</button></label>\
				<label style="display: none;"><button type="button" onclick="matchWithoutBankLog({outwardTaskId})" class="btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button></label>\
			</td>\
		</tr>';

var packageOutward4DepositedTable=function(){
	var rebateDataOnly = $("#rebateDataOnly").prop("checked");
	var url = rebateDataOnly?'/r/rebate/list':API.r_outtask_list;
	$("div[name='sysRecord'] table.table tbody").html();
	var CurPage=$("#tablePage1_sys").find(".Current_Page").text();
	$.ajax({dataType:'json',type:"POST",async:false,url:url,data: packageSearchData4Outward_Deposited(CurPage),success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var idList=new Array;
		$.each(jsonObject.data, function(idx, obj) {
			obj.toAccount = obj.toAccount?obj.toAccount:'';
			obj.taskOperatorUid = obj.taskOperatorUid?obj.taskOperatorUid:'系统';
			obj.fromAccount = obj.fromAccount?obj.fromAccount:'';
			obj.fromAccountInfo = (obj.fromAlias?obj.fromAlias:'')+'|'+hideName(obj.fromOwner)+'|'+(obj.fromBankType?obj.fromBankType:'')+'|...'+obj.fromAccount.slice(-4);
			obj.asignTime=timeStamp2yyyyMMddHHmmss(obj.asignTime);
			obj.taskRemark = obj.taskRemark?obj.taskRemark:'';
			obj.taskRemarkShort = obj.taskRemark?obj.taskRemark.substring(0,5)+'...':'';
			obj.screenshot = obj.screenshot?'/'+obj.screenshot:'';
			if(isHideImg){
				obj.hideImg=' hide ';
			}else{
				obj.hideImg='';
				obj.captureFlag =obj.screenshot?'查看':'';
			}
			obj.toAccount = obj.toAccount?obj.toAccount:'';
			obj.orderNo = obj.orderNo?obj.orderNo:'';
			obj.memberUserName = obj.memberUserName?obj.memberUserName:'';
			obj.toAccountOwner = hideName(obj.toAccountOwner);
			obj.toAccountStar = obj.toAccount?(hideAccountAll(obj.toAccount)):'';
			idList.push({'id':obj.fromAccountId});
		});
		$("div[name='sysRecord'] table.table tbody").html(fillDataToModel4Array(jsonObject.data,modelHtml4OutwardMatching));
		loadHover_accountInfoHover(idList);
		showPading(jsonObject.page,"tablePage1_sys",packageOutward4DepositedTable,"系统记录",true);
		$("[data-toggle='popover']").popover();
	},initPage:function (his) {
	}});
}

var modelHtml4eOutwardCanceled =  '\
		<tr>\
			<td><a class="bind_hover_card" data-toggle="accountInfoHover{fromAccountId}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td><td><span>{toAccountStar}</span></td>\
			<td><span>{amount}</span></td><td><span>{taskOperatorUid}</span></td><td><span>{asignTime}</span></td>\
	        <td><a width:40px; overflow:hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis; title="{taskRemark}">{taskRemarkShort}</a></td>\
	        <td><a class="{hideImg}" href="javascript:void(0);" onclick="showCaptureImg(\'{screenshot}\')">{captureFlag}</a></td>\
		</tr>';

var packageOutward4UnmatchedTable = function(){
	var rebateDataOnly = $("#rebateDataOnly").prop("checked");
	var url = rebateDataOnly?'/r/rebate/list':API.r_outtask_list;
	$("div[name='sysRecordUnmatched'] table.table tbody").html('');
	var CurPage=$("#tablePage6").find(".Current_Page").text();
	$.ajax({dataType:'json',type:"POST",async:false,url:url,data: packageSearchData4Outward_Unmatched(CurPage),success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var subtotal={count:0,sum:0.00},idList=new Array;
		$.each(jsonObject.data,function(index,obj){
			subtotal.count++;subtotal.sum+=obj.amount*1;
			obj.taskOperatorUid=obj.taskOperatorUid?obj.taskOperatorUid:'系统';
			obj.fromAccountInfo = (obj.fromAlias?obj.fromAlias:'')+'|'+hideName(obj.fromOwner)+'|'+(obj.fromBankType?obj.fromBankType:'')+'|...'+obj.fromAccount.slice(-4);
			obj.taskRemark=obj.taskRemark?obj.taskRemark:'';
			obj.taskRemarkShort=obj.taskRemark.substring(0, 5);
			obj.asignTime=timeStamp2yyyyMMddHHmmss(obj.asignTime);
			obj.screenshot = obj.screenshot?'/'+obj.screenshot:'';
			if(isHideImg){
				obj.hideImg=' hide ';
			}else{
				obj.hideImg='';
				obj.captureFlag =obj.screenshot?'查看':'';
			};
			obj.toAccountStar = obj.toAccount?(hideAccountAll(obj.toAccount)):'';
			idList.push({'id':obj.fromAccountId});
		});
		var $body = $("div[name='sysRecordUnmatched'] table.table tbody");
		$body.html(fillDataToModel4Array(jsonObject.data,modelHtml4eOutwardCanceled));
		loadHover_accountInfoHover(idList);
		showSubAndTotalStatistics4Table($body,{column:7,subCount:subtotal.count,count:jsonObject.page.totalElements,3:{subTotal:subtotal.sum,total:jsonObject.page.header.totalAmount}});
		showPading(jsonObject.page,"tablePage6",packageOutward4UnmatchedTable,null,true,false);
		$("[data-toggle='popover']").popover();
	},initPage:function (his) {
	}});
};

var modelHtml4BankFlow = '\
	   <tr>\
		   <td><input type="radio" name="bankLogId" value="{id}" style="width:18px;height:18px;cursor: pointer;" onmousedown="mousedown4EncashCheckRadio(this)" onclick="return false;"></td>\
		   <td><a class="bind_hover_card" data-toggle="accountInfoHover{fromAccount}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td>\
		   <td><span>{toAccount}</a></td><td><span>{toAccountOwner}</a></td>\
		   <td name="createTimeStr"><span>{tradingTimeStr}</span></td><td><span>{createTimeStr}</span>\
		   <td name="amount"><span>{amount}</span></td>\
		   <td name="amount"><span>{summary}</span></td>\
		   <td><a width:40px; overflow:hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis; title="{remark}">{remarkShort}</a></td>\
	   </tr>';

var packBankFlow4MatchingTable = function(){
	var CurPage=$("#tablePage1_bank").find(".Current_Page").text();
	$.ajax({dataType:'json',type:"POST",async:false,url:API.r_banklog_findbyfrom,data:packageSearchData4BankLog(CurPage),success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var idList=new Array;
		$.each(jsonObject.data,function(index,obj){
			obj.amount= (-1)*obj.amount;
			obj.toAccount= hideAccountAll(obj.toAccount);
			obj.fromAccountNO= obj.fromAccountNO?obj.fromAccountNO:'';
			obj.fromAccountInfo = (obj.fromAlias?obj.fromAlias:'')+'|'+hideName(obj.fromOwner)+'|'+(obj.fromBankType?obj.fromBankType:'')+'|...'+obj.fromAccountNO.slice(-4);
			obj.fromAccount= obj.fromAccount?obj.fromAccount:'';
			obj.tradingTimeStr=obj.tradingTimeStr? obj.tradingTimeStr:'';
			obj.remark= obj.remark?obj.remark:'';
			obj.remarkShort= obj.remark?obj.remark.substring(0,5)+'...':'';
			obj.toAccountOwner=hideName(obj.toAccountOwner);
			obj.tradingTimeStr=obj.tradingTimeStr? obj.tradingTimeStr:'';
			obj.createTimeStr=obj.createTime? timeStamp2yyyyMMddHHmmss(obj.createTime):'';
			obj.summary= obj.summary?obj.summary:'';
			idList.push({'id':obj.fromAccount});
		});
		$("div[name='bankFlow'] table.table tbody").html(fillDataToModel4Array(jsonObject.data,modelHtml4BankFlow));
		loadHover_accountInfoHover(idList);
		showPading(jsonObject.page,"tablePage1_bank",packBankFlow4MatchingTable,"银行流水",true);
		$("[data-toggle='popover']").popover();
	},initPage:function (his) {
	}});
}

var modelHtml4Matched_sys =  '\
		<tr>\
			<td><a class="bind_hover_card" data-toggle="accountInfoHover{fromAccountId}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td><td><span>{toAccountStar}</span></td>\
			<td><span>{amount}</span></td><td><span>{asignTime}</span></td><td><span>{taskOperatorUid}</span></td>\
	        <td><a width:40px; overflow:hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis; title="{taskRemark}">{taskRemarkShort}</a></td>\
	        <td><a class="{hideImg}" href="javascript:void(0);" onclick="showCaptureImg(\'{screenshot}\')">{captureFlag}</a></td>\
		</tr>';

var packBankFlow4MatchedTable = function(){
	var rebateDataOnly = $("#rebateDataOnly").prop("checked");
	var url = rebateDataOnly?'/r/rebate/list':API.r_outtask_list;
	$("div[name='sysRecordUnmatched'] table.table tbody").html('');
	var CurPage=$("#tablePage"+incomeRequestStatusMatched).find(".Current_Page").text();
	$.ajax({dataType:'json',type:"POST",async:false,url:url,data: packageSearchData4Matched(CurPage),success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var $body = $("div[name='pageContent'] div[id='tab"+outwardTaskStatusMatched+"'] table tbody").html('');
		var subtotal={count:0,sum:0.00},idList=new Array;
		$.each(jsonObject.data,function(index,obj){
			subtotal.count++;subtotal.sum+=obj.amount*1;
			obj.taskOperatorUid=obj.taskOperatorUid?obj.taskOperatorUid:'系统';
			obj.fromAccountInfo = (obj.fromAlias?obj.fromAlias:'')+'|'+hideName(obj.fromOwner)+'|'+(obj.fromBankType?obj.fromBankType:'')+'|...'+obj.fromAccount.slice(-4);
			obj.taskRemark=obj.taskRemark?obj.taskRemark:'';
			obj.taskRemarkShort=obj.taskRemark.substring(0, 5);
			obj.asignTime=timeStamp2yyyyMMddHHmmss(obj.asignTime);
			obj.screenshot = obj.screenshot?'/'+obj.screenshot:'';
			if(isHideImg){
				obj.hideImg=' hide ';
			}else{
				obj.hideImg='';
				obj.captureFlag =obj.screenshot?'查看':'';
			}
			obj.toAccountStar = obj.toAccount?(hideAccountAll(obj.toAccount)):'';
			idList.push({'id':obj.fromAccountId});
		});
		$body.html(fillDataToModel4Array(jsonObject.data,modelHtml4Matched_sys));
		loadHover_accountInfoHover(idList);
		showSubAndTotalStatistics4Table($body,{column:7,subCount:subtotal.count,count:jsonObject.page.totalElements,3:{subTotal:subtotal.sum,total:jsonObject.page.header.totalAmount}});


		showPading(jsonObject.page,"tablePage"+incomeRequestStatusMatched,packBankFlow4MatchedTable);
		$("[data-toggle='popover']").popover();
	},initPage:function (his) {
	}});
};
initTimePicker(true,$("[name=startAndEndTime]"),7);
$("div[name='pageContent'] ul.nav-tabs.nav li.active").removeClass("active");
$("div[name='pageContent'] ul.nav-tabs.nav li[outwardtaskstatus="+request.outwardTaskStatus+"]").addClass("active");
$("div[name='pageContent'] div[id^='tab']").removeClass("active");
$("div[name='pageContent'] div[id='tab"+request.outwardTaskStatus+"']").addClass("active");
searchByFilter(request.outwardTaskStatus);
