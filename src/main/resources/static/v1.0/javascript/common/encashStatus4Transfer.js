currentPageLocation = window.location.href;
var request=getRequest();//fromAccountId toAccountId  incomeReqStatus
var modelHtml ='<tr id="{id}" >\
					<td><a title="{remark}">{typeName}</a></td>\
					<td><a class="bind_hover_card" data-toggle="accountInfoHover{fromId}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a><div class="BankLogEvent" target="{fromId}"><span class="time"></span></div></td>\
					<td><a class="bind_hover_card" data-toggle="accountInfoHover{toId}" data-placement="auto right" data-trigger="hover">{toAccountInfo}</a><div class="BankLogEvent" target="{toId}"><span class="time"></span></div></td>\
					<td><span>{orderNo}</span></td>\
					<td><span>{amount}</span></td>\
					<td><span>{fee}</span></td>\
					<td><span class="label label-sm {classOfStatus}">{statusName}</span>{statusBtn}</td>\
					<td><span>{createTime}</span></td>\
				</tr>';
var showEncashmentList=function(){
	var $div=$("#encashStatusPage");
	var CurPage=$div.find(".Current_Page").text();
	if(!!!CurPage)  CurPage=0;
	var $filter = $("#accountFilter");
	var startAndEndTime = $filter.find("input[name='startAndEndTime']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	var dataRequest={
		"pageNo":CurPage<=0?0:CurPage-1,
		"pageSize":$.session.get('initPageSize'),
		"fromId":request.fromAccountId,
		"toId":request.toAccountId,
		"fromAccount":$filter.find("input[name='fromAccount']").val(),
		"toAccount":$filter.find("input[name='toAccount']").val(),
		"remark":$filter.find("input[name='remark']").val(),
		"orderNo":$filter.find("input[name='orderNo']").val(),
		"memberUserName":$filter.find("input[name='memberUserName']").val(),
		"minAmount":$filter.find("input[name='minAmount']").val(),
		"maxAmount":$filter.find("input[name='maxAmount']").val(),
		"startAndEndTimeToArray":startAndEndTimeToArray,
		"statusArray":[request.incomeReqStatus].toString()
	};
	$.ajax({type:"POST", url:API.r_income_findbyvo,data:dataRequest,dataType:'json',success:function(jsonObject){
		if(-1==jsonObject.status){
			showMessageForFail("查询失败："+jsonObject.message);return;
		}
		var $tbody = $("div[name='accountFlow'] table.table tbody").html("");
		if(request.isIncomeBankCard){
			$(".userNameNeedHide").removeClass("hidden");
		}
		var totalAccount =0,totalFee=0,idList=new Array,incomeReqIdArray=new Array(),accountIdMap = new Object(),accountIdArray = new Array();
		$.each(jsonObject.data,function(index,record){
			totalAccount = totalAccount + record.amount;
			record.fee=record.fee?record.fee:"0";
			totalFee = totalFee + eval(record.fee);
			record.remark=record.remark?record.remark:"无备注信息";
			record.createTime = timeStamp2yyyyMMddHHmmss(record.createTime);
			record.fromAccountInfo = (!record.fromAlias&&!record.fromBankType&&!record.fromOwner)?record.fromAccount:((record.fromAlias?record.fromAlias:'无')+'|'+(record.fromBankType?record.fromBankType:'无')+'|'+(record.fromOwner?record.fromOwner:'无'));
			record.toAccountInfo = (!record.toAlias&&!record.toBankType&&!record.toOwner)?record.toAccount:((record.toAlias?record.toAlias:'无')+'|'+(record.toBankType?record.toBankType:'无')+'|'+(record.toOwner?record.toOwner:'无'));
			idList.push({'id':record.fromId});
			idList.push({'id':record.toId});
			record.classOfStatus=(record.status==incomeRequestStatusMatched)?'label-success':((record.status==incomeRequestStatusCanceled)?'label-inverse':'label-warning');
			record.statusBtn='';
			if(record.type==incomeRequestTypeWithdrawThirdToCustomer){
				//提现到客户卡额外处理
				if(record.status==incomeRequestStatusMatched){
					record.statusName ='<a class="bind_hover_card white" >已确认</a>';
				}else if(record.status==incomeRequestStatusCanceled){
					record.statusName ='<a class="bind_hover_card white" >已打回</a>';
				}else if(record.status==incomeRequestStatusMatching){
					//匹配中新增按钮
					record.statusName ='';
					record.statusBtn='<button type="button" class="btn btn-xs btn-white btn-success btn-bold"\
										onclick="third2CustomConfirmOrReject(1,'+record.id+')">\
										<i class="ace-icon fa fa-check bigger-100 danger"></i>\
										<span>确认</span>\
									</button>\
									<button type="button" class="btn btn-xs btn-white btn-danger btn-bold" \
										onclick="third2CustomConfirmOrReject(3,'+record.id+')">\
										<i class="ace-icon fa fa-undo bigger-100 danger"></i>\
										<span>打回</span>\
									</button>';
				}
			}else{
				record.statusName =record.status==incomeRequestStatusMatched?'<a class="bind_hover_card white" data-toggle="bankLogInfoHover'+record.id+'" data-placement="auto right" data-trigger="hover">'+record.statusName+'</a>':record.statusName;
			}
			incomeReqIdArray.push({'id':record.id});
			accountIdMap[record.fromId]=record.fromId;
			accountIdMap[record.toId]=record.toId;
		});
		$(fillDataToModel4Array(jsonObject.data,modelHtml)).appendTo($tbody);
		loadHover_accountInfoHover(idList);
		loadHover_BankLogInfoByIncomeReqIdArray(incomeReqIdArray);
		showSubAndTotalStatistics4Table($tbody,{column:10, subCount:jsonObject.data.length,count:jsonObject.page.totalElements,5:{subTotal:totalAccount,total:jsonObject.page.header.totalAmount},6:{subTotal:totalFee,total:jsonObject.page.header.totalFee}});
		showPading(jsonObject.page,"encashStatusPage",showEncashmentList);
		for(var toId in accountIdMap){
			accountIdArray.push(toId);
		}
		SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,accountIdArray);
	},initPage:function (his) {
	}});
}
var third2CustomConfirmOrReject=function(operaType,incomeId){
	bootbox.confirm("确定修改入款状态?一经修改不可退回！", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/income/third2CustomConfirmOrReject',
				async:false,
				data:{
					"incomeRequestId":incomeId,
					"confirm1Reject3":operaType
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	showMessageForSuccess("修改成功");
			        	//刷新
			        	showEncashmentList();
			        }else{
			        	showMessageForFail("修改失败："+jsonObject.message);
			        }
			    }
			});
		}
	});
}
initTimePicker(true,$("[name=startAndEndTime]"),7);
$("#searchBtnModal").click(function(){
	showEncashmentList();
});
showEncashmentList();