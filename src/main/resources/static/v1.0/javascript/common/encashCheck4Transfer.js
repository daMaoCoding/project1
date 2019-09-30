currentPageLocation = window.location.href;
var request=getRequest(); //fromAccountId  toAccountId incomeReqStatus
request.incomeReqStatus=request.incomeReqStatus?request.incomeReqStatus:incomeRequestStatusMatched;
$("#pageSizeFive").removeClass("checked").on("click",function(){
	searchByFilter(0);
});

var mousedown4EncashCheckRadio = function(obj){
	var radioChecked = $(obj).prop("checked");
	$(obj).prop('checked', !radioChecked);
	return false;
}

var searchByFilter=function(incomeReqStatus){
	if(incomeReqStatus||incomeReqStatus==incomeRequestStatusMatching){
		$("#incomeReqStatus").val(incomeReqStatus);
	}else{
		incomeReqStatus = $("div[name='pageContent'] ul.nav-tabs li.active").attr("incomeReqStatus");
	}
	if(incomeReqStatus==incomeRequestStatusMatching){
		var noneChecked=true;
		$("input[name='searchType']:checked").each(function(){
			if($(this).val()=='sys'){
				noneChecked=false;
				packageIncomeReq4MatchingTable();
			}else if($(this).val()=='bank'){
				noneChecked=false;
				packBankFlow4MatchingTable();
			}
		});
		if(noneChecked){
			packageIncomeReq4MatchingTable();
			packBankFlow4MatchingTable();
		}
	}else if(incomeReqStatus==incomeRequestStatusMatched){
		packBankFlow4MatchedTable();
	}else if(incomeReqStatus==incomeRequestStatusCanceled){
		packageIncomeReq4CanceledTable();
	}
}

var match = function(){
	var sysRecordId = $("table[name='tableIncome']").find("tr td input[name='sysRecordId']:checked").val();
	var bankLogId   = $("table[name='tableBank']").find("tr td input[name='bankLogId']:checked").val();
	showConfirmMatchModal(sysRecordId,bankLogId,function(){
		packageIncomeReq4MatchingTable();
		packBankFlow4MatchingTable();
	});
};

var captureBankStatement = function(){
	var toAccountId = request.toAccountId;
	if(!toAccountId){
		showMessageForFail('请清空浏览器缓存,重新登陆.');
	}
	bootbox.confirm("确定需要立即抓取该账号的流水?", function(result) {
		if(result){
			$.ajax({dataType:'json',type:"POST",async:false,url:'/r/income/activateTools',data: {accountId:toAccountId},success:function(jsonObject){
				if(jsonObject.status !=1){
					showMessageForFail("操作失败："+jsonObject.message);
				}else{
					showMessageForSuccess('指令发送,请稍后查看');
				}
			}});
		}
	});
};

var reject2Platform =  function(incomeReqId,type){
	if(type==401){
		showMessageForFail("兼职提额单，不能打回！");return;
	}else{
		showReject2TransferModal(incomeReqId,packageIncomeReq4MatchingTable);
	}
}

var packageSearchData4IncomeReq_0 = function(CurPage){
	var $tab0 = $("div[name='pageContent'] div[id='tab0']");
	var minAmount = $tab0.find("input[name='minAmount']").val();
	var maxAmount = $tab0.find("input[name='maxAmount']").val();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = $tab0.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	var pageSize=$.session.get('initPageSize');
	if($("#incomeReqStatus").val()==incomeRequestStatusMatching&&$("#pageSizeFive").prop("checked")){
		pageSize=5;
	}
	return {"pageNo":(CurPage<=0?0:CurPage-1),"pageSize":pageSize,"fromId":request.fromAccountId,"toId":request.toAccountId,
		     "startAndEndTimeToArray":startAndEndTimeToArray.toString(), "status":incomeRequestStatusMatching,"minAmount":minAmount,"maxAmount":maxAmount,};
}

var packageSearchData4IncomeReq_1 = function(CurPage){
	var tab3 = $("div[name='pageContent'] div[id='tab1']");
	var fromAccount = tab3.find("input[name='fromAccount']").val();
	var toAccount = tab3.find("input[name='toAccount']").val();
	var minAmount = tab3.find("input[name='minAmount']").val();
	var maxAmount = tab3.find("input[name='maxAmount']").val();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = tab3.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	return {"pageNo":(CurPage<=0?0:CurPage-1),"pageSize":$.session.get('initPageSize'),"fromId":request.fromAccountId,"toId":request.toAccountId,
		"startAndEndTimeToArray":startAndEndTimeToArray.toString(), "status":incomeRequestStatusMatched,
		"fromAccount":fromAccount,"toAccount":toAccount,"minAmount":minAmount,"maxAmount":maxAmount};
}

var packageSearchData4IncomeReq_3 = function(CurPage){
	var tab3 = $("div[name='pageContent'] div[id='tab3']");
	var fromAccount = tab3.find("input[name='fromAccount']").val();
	var toAccount = tab3.find("input[name='toAccount']").val();
	var minAmount = tab3.find("input[name='minAmount']").val();
	var maxAmount = tab3.find("input[name='maxAmount']").val();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = tab3.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	return {"pageNo":(CurPage<=0?0:CurPage-1),"pageSize":$.session.get('initPageSize'),"fromId":request.fromAccountId,"toId":request.toAccountId,
		"startAndEndTimeToArray":startAndEndTimeToArray.toString(), "status":incomeRequestStatusCanceled,
		"fromAccount":fromAccount,"toAccount":toAccount,"minAmount":minAmount,"maxAmount":maxAmount};
}

var packageSearchData4BankLog=function(CurPage){
	var tab0 = $("div[name='pageContent'] div[id='tab0']");
	var id = tab0.find("input[name='id']").val();
	var minAmount = tab0.find("input[name='minAmount']").val();
	var maxAmount = tab0.find("input[name='maxAmount']").val();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = tab0.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	var pageSize=$.session.get('initPageSize');
	if($("#incomeReqStatus").val()==incomeRequestStatusMatching&&$("#pageSizeFive").prop("checked")){
		pageSize=5;
	}
	return {"pageNo":(CurPage<=0?0:CurPage-1),"pageSize":pageSize,"fromAccount":request.toAccountId,"toAccount":request.fromAccount,
		"startAndEndTimeToArray":startAndEndTimeToArray.toString(), "search_EQ_status":bankLogStatusMatching,searchTypeIn0Out1:0,
		"id":id,"minAmount":minAmount,"maxAmount":maxAmount};
}

var packageSearchData4Matched=function(CurPage){
	var tab1 = $("div[name='pageContent'] div[id='tab1']");
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray = new Array();
	var startAndEndTime = tab1.find("input[name='startAndEndTime']").val();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	var minAmount = tab1.find("input[name='minAmount']").val();
	var maxAmount = tab1.find("input[name='maxAmount']").val();
	return {"pageNo":(CurPage<=0?0:CurPage-1),"pageSize":$.session.get('initPageSize'),"toAccount":request.toAccountId,"startAndEndTimeToArray": startAndEndTimeToArray.toString(),"minAmount":minAmount,"maxAmount":maxAmount};
}

var modelHtml4eIncomeReqMatching = '\
		<tr>\
			<td><input type="radio" name="sysRecordId" value="{id}" style="width:18px;height:18px;cursor: pointer;" onmousedown="mousedown4EncashCheckRadio(this)" onclick="return false;"></td>\
			<td><a class="bind_hover_card" data-toggle="accountInfoHover{toId}" data-placement="auto right" data-trigger="hover">{toAccountInfo}</a></td>\
			<td><a class="bind_hover_card" data-toggle="accountInfoHover{fromId}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td>\
			<td><span>{fromOwner}</span></td><td><span>{amount}</span></td><td><span>{fee}</span></td><td><span>{orderNo}</span></td>\
			<td><span>{createTime}</span></td><td><span>{operatorUid}</span></td>\
			<td><a class="bind_hover_card breakByWord" title="备注信息" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="{remark}">{remarkShort}</a></td>\
			<td>\
				<label class="inline"><button id="{id}" type="button" onclick="reject2Platform({id},{type})" class="btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa fa-reply bigger-100 red"></i>打回</button></label>\
			</td>\
		</tr>';

var packageIncomeReq4MatchingTable=function(){
	$("div[name='sysRecord'] table.table tbody").html();
	var CurPage=$("#tablePage0_sys").find(".Current_Page").text();
	var orderIdArray = new Array();
	$.ajax({dataType:'json',type:"POST",async:false,url:API.r_income_findbyvo,data: packageSearchData4IncomeReq_0(CurPage),success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var idList=new Array;
		$.each(jsonObject.data, function(idx, obj) {
			obj.amount = obj.amount?obj.amount:'0';
			obj.fee = obj.fee?obj.fee:'0';
			obj.createTime=timeStamp2yyyyMMddHHmmss(obj.createTime);
			obj.memberRealName = obj.memberRealName?obj.memberRealName:'';
			obj.remark=obj.remark?obj.remark:'';
			obj.remarkShort=obj.remark?obj.remark.substring(0,5)+'...':'';
			obj.operatorUid = obj.operatorUid?obj.operatorUid:'系统';
			obj.orderNo = obj.orderNo?obj.orderNo:'';
			obj.fromOwner = hideName(obj.fromOwner);
			obj.toAccountInfo = (obj.toAlias?obj.toAlias:'')+'|'+hideName(obj.toOwner)+'|'+(obj.toBankType?obj.toBankType:'')+(obj.toAccount?'|...'+obj.toAccount.slice(-4):'');
			obj.fromAccountInfo = (obj.fromAlias?obj.fromAlias:'')+'|'+hideName(obj.fromOwner)+'|'+(obj.fromBankType?obj.fromBankType:'')+'|...'+ (!obj.fromAccount?'':obj.fromAccount.slice(-4));
			idList.push({'id':obj.fromId});
			idList.push({'id':obj.toId});
			orderIdArray.push(obj.id);
		});
		$("div[name='sysRecord'] table.table tbody").html(fillDataToModel4Array(jsonObject.data,modelHtml4eIncomeReqMatching));
		loadHover_accountInfoHover(idList);
		showPading(jsonObject.page,"tablePage0_sys",packageIncomeReq4MatchingTable,"系统记录",true);
	},initPage:function (his) {}});
	if(orderIdArray.length==0){
		return;
	}
	$.ajax({dataType:'json',type:'POST',url:API.r_match_findByTypeNotAndOrderIdIn,data:{typeNot:0,orderIdIn:orderIdArray.toString()},success:function(jsonObject){
		if(jsonObject.status !=1){
			return;
		}
		var $td = $("table[name='tableIncome'] tr td");
		$.each(jsonObject.data,function(idx,obj){
			$('#'+obj.orderId).hide();
			$td.find("input[name='sysRecordId'][value='"+obj.orderId+"']").closest("tr").addClass("btn-yellow no-hover");
		});
	}});
}

var modelHtml4eIncomeReqCanceled =  '\
		<tr>\
		    <td><a class="bind_hover_card" data-toggle="accountInfoHover{fromId}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td>\
			<td><a class="bind_hover_card" data-toggle="accountInfoHover{toId}" data-placement="auto right" data-trigger="hover">{toAccountInfo}</a></td>\
			<td><span>{amount}</span></td><td><span>{orderNo}</span></td><td><span>{operatorUid}</span></td><td><span>{createTime}</span></td>\
			<td><a class="bind_hover_card breakByWord" title="备注信息" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="{remark}">{remarkShort}</a></td>\
		</tr>';

var packageIncomeReq4CanceledTable = function(){
	$("div[name='sysRecord'] table.table tbody").html();
	var CurPage=$("#tablePage3").find(".Current_Page").text();
	$.ajax({dataType:'json',type:"POST",async:false,url:API.r_income_findbyvo,data: packageSearchData4IncomeReq_3(CurPage),success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var subtotal={count:0,sum:0.00},idList=new Array;
		$.each(jsonObject.data,function(index,obj){
			subtotal.count++;subtotal.sum+=obj.amount*1;
			obj.operatorUid=obj.operatorUid?obj.operatorUid:'系统';
			obj.createTime=timeStamp2yyyyMMddHHmmss(obj.createTime);
			obj.toAccountInfo = (obj.toAlias?obj.toAlias:'')+'|'+hideName(obj.toOwner)+'|'+(obj.toBankType?obj.toBankType:'')+(obj.toAccount?'|...'+obj.toAccount.slice(-4):'');
			obj.fromAccountInfo = (obj.fromAlias?obj.fromAlias:'')+'|'+hideName(obj.fromOwner)+'|'+(obj.fromBankType?obj.fromBankType:'')+(obj.fromAccount?'|...'+obj.fromAccount.slice(-4):'');
			obj.remark=obj.remark?obj.remark:'';
			obj.remarkShort=obj.remark?obj.remark.substring(0,5)+'...':'';
			obj.orderNo = obj.orderNo?obj.orderNo:'';
			idList.push({id:obj.toId});
			idList.push({id:obj.fromId});
		});
		var $body = $("div[name='sysRecord'] table.table tbody");
		$body.html(fillDataToModel4Array(jsonObject.data,modelHtml4eIncomeReqCanceled));
		loadHover_accountInfoHover(idList);
		showSubAndTotalStatistics4Table($body,{column:7,subCount:subtotal.count,count:jsonObject.page.totalElements,3:{subTotal:subtotal.sum,total:jsonObject.page.header.totalAmount}});
		showPading(jsonObject.page,"tablePage3",packageIncomeReq4CanceledTable,null,true);
		$("[data-toggle='popover']").popover();
	},initPage:function (his) {}});
}

var modelHtml4BankFlow = '\
		   <tr>\
			   <td><input type="radio" name="bankLogId" value="{id}" style="width:18px;height:18px;cursor: pointer;" onmousedown="mousedown4EncashCheckRadio(this)" onclick="return false;"></td>\
			   <td><a class="bind_hover_card" data-toggle="accountInfoHover{fromAccount}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td>\
			   <td><span>{toAccount}</a></td>\
			   <td><span>{toAccountOwner}</a></td>\
		  	   <td name="amount"><span>{amount}</span></td>\
			   <td name="amount"><span>{balance}</span></td>\
			   <td><span>{tradingTimeStr}</span></td>\
			   <td><span>{createTimeStr}</span></td>\
			   <td><a style="width:100px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;" title="{summary}">{summary}</a></td>\
			   <td><a style="width:100px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;" title="{remark}">{remark}</a></td>\
		   </tr>';

var packBankFlow4MatchingTable = function(){
	var CurPage=$("#tablePage0_bank").find(".Current_Page").text();
	$.ajax({dataType:'json',type:"POST",async:false,url:API.r_banklog_findbyfrom,data:packageSearchData4BankLog(CurPage),success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var idList=new Array;
		$.each(jsonObject.data,function(index,record){
			record.transactionNo=record.transactionNo?record.transactionNo:'';
			record.toAccount=hideAccountAll(record.toAccount);
			record.toAccountOwner=hideName(record.toAccountOwner);
			record.createTimeStr=timeStamp2yyyyMMddHHmmss(record.createTime);
			record.summary=record.summary?record.summary:'';
			record.remark=record.remark?record.remark:'';
			record.fromAccountInfo = (record.fromAlias?record.fromAlias:'')+'|'+hideName(record.fromOwner)+'|'+(record.fromBankType?record.fromBankType:'')+'|...'+record.fromAccountNO.slice(-4);
			idList.push({'id':record.fromAccount});
		});
		$("div[name='bankFlow'] table.table tbody").html(fillDataToModel4Array(jsonObject.data,modelHtml4BankFlow));
		loadHover_accountInfoHover(idList);
		showPading(jsonObject.page,"tablePage0_bank",packBankFlow4MatchingTable,"银行流水",true);
		$("[data-toggle='popover']").popover();
	},initPage:function (his) {}});
};

var modelHtml4Matched =  '\
		<tr>\
		    <td><a class="bind_hover_card" data-toggle="accountInfoHover{fromId}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td>\
			<td><a class="bind_hover_card" data-toggle="accountInfoHover{toId}" data-placement="auto right" data-trigger="hover">{toAccountInfo}</a></td>\
			<td><span>{amount}</span></td><td><span>{orderNo}</span></td><td><span>{operatorUid}</span></td><td><span>{createTime}</span></td>\
			<td><a class="bind_hover_card breakByWord" title="备注信息" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="{remark}">{remarkShort}</a></td>\
		</tr>';

var packBankFlow4MatchedTable = function(){
	$("div[name='sysRecord'] table.table tbody").html();
	var CurPage=$("#tablePage"+incomeRequestStatusMatched).find(".Current_Page").text();
	$.ajax({dataType:'json',type:"POST",async:false,url:API.r_income_findbyvo,data: packageSearchData4IncomeReq_1(CurPage),success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var subtotal={count:0,sum:0.00},idList=new Array;
		$.each(jsonObject.data,function(index,obj){
			subtotal.count++;subtotal.sum+=obj.amount*1;
			obj.operatorUid=obj.operatorUid?obj.operatorUid:'系统';
			obj.createTime=timeStamp2yyyyMMddHHmmss(obj.createTime);
			obj.toAccountInfo = (obj.toAlias?obj.toAlias:'')+'|'+hideName(obj.toOwner)+'|'+(obj.toBankType?obj.toBankType:'')+(obj.toAccount?'|...'+obj.toAccount.slice(-4):'');
			obj.fromAccountInfo = (obj.fromAlias?obj.fromAlias:'')+'|'+hideName(obj.fromOwner)+'|'+(obj.fromBankType?obj.fromBankType:'')+(obj.fromAccount?('|...'+obj.fromAccount.slice(-4)):'');
			obj.remark=obj.remark?obj.remark:'';
			obj.remarkShort=obj.remark?obj.remark.substring(0,5)+'...':'';
			obj.orderNo = obj.orderNo?obj.orderNo:'';
			idList.push({id:obj.toId});
			idList.push({id:obj.fromId});
		});
		var $body = $("#simple_table_1  table.table tbody");
		//var $body = $("div[name='sysRecord'] table.table tbody");
		$body.html(fillDataToModel4Array(jsonObject.data,modelHtml4Matched));
		loadHover_accountInfoHover(idList);
		showSubAndTotalStatistics4Table($body,{column:9,subCount:subtotal.count,count:jsonObject.page.totalElements,3:{subTotal:subtotal.sum,total:jsonObject.page.header.totalAmount}});
		showPading(jsonObject.page,"tablePage1",packBankFlow4MatchedTable,null,true);
		$("[data-toggle='popover']").popover();
	},initPage:function (his) {}});
}

initTimePicker(true,$("[name=startAndEndTime]"),7);
$("div[name='pageContent'] ul.nav-tabs.nav li.active").removeClass("active");
$("div[name='pageContent'] ul.nav-tabs.nav li[incomeReqStatus="+request.incomeReqStatus+"]").addClass("active");
$("div[name='pageContent'] div[id^='tab']").removeClass("active");
$("div[name='pageContent'] div[id='tab"+request.incomeReqStatus+"']").addClass("active");
searchByFilter(request.incomeReqStatus);
if(request.toAccountId){
	$('div.BankLogEvent.EncashCheck4Transfer').attr('target',request.toAccountId);
	SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,[request.toAccountId]);
}
