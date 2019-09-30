currentPageLocation = window.location.href;
var request=getRequest();//fromAccountId toAccountId  incomeReqStatus
var modelHtml ='<tr id={id}>\
					<td><span>{handicapName}</span></td><td><span>{levelName}</span></td><td><span>{memberUserName}</span></td><td><span>{memberRealName}</span></td><td><a class="bind_hover_card" data-toggle="accountInfoHover{toId}" data-placement="auto right" data-trigger="hover">{toAccountInfo}</a></td><td><span>{orderNo}</span></td>\
					<td><span>{amount}</span></td><td><span>{fee}</span></td><td><span class="label label-sm {classOfStatus}">{statusName}</span></td><td><span>{createTime}</span></td>\
					<td><a style="width:130px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;" title="{remark}">{remark}</a></td>\
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
	    "handicap":$filter.find("select[name='search_EQ_handicapId']").val(),
		"toAccount":$filter.find("input[name='toAccount']").val(),
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
		var totalAccount =0,totalFee=0,idList=new Array,incomeReqIdArray=new Array();
		$.each(jsonObject.data,function(index,record){
			totalAccount = totalAccount + record.amount;
			record.fee=record.fee?record.fee:"0";
			totalFee = totalFee + eval(record.fee);
			record.handicapName=record.handicapName?record.handicapName:"";
			record.levelName=record.levelName?record.levelName:"";
			record.remark=record.remark?record.remark:"";
			record.memberUserName=record.memberUserName?record.memberUserName:"";
			record.classOfStatus=(record.status==incomeRequestStatusMatched)?'label-success':((record.status==incomeRequestStatusCanceled)?'label-inverse':'label-warning');
			record.statusName =record.status==incomeRequestStatusMatched?'<a class="bind_hover_card white" data-toggle="bankLogInfoHover'+record.id+'" data-placement="auto right" data-trigger="hover">'+record.statusName+'</a>':record.statusName;
			record.createTime = timeStamp2yyyyMMddHHmmss(record.createTime);
			record.fromAccountInfo = (!record.fromAlias&&!record.fromBankType&&!record.fromOwner)?record.fromAccount:(record.fromAlias+'|'+record.fromBankType+'|'+record.fromOwner);
			record.toAccountInfo = (!record.toAlias&&!record.toBankType&&!record.toOwner)?record.toAccount:(record.toAlias+'|'+record.toBankType+'|'+record.toOwner);
			idList.push({'id':record.fromId});
			idList.push({'id':record.toId});
			incomeReqIdArray.push({'id':record.id});
		});
		$(fillDataToModel4Array(jsonObject.data,modelHtml)).appendTo($tbody);
		loadHover_accountInfoHover(idList);
		loadHover_BankLogInfoByIncomeReqIdArray(incomeReqIdArray);
		showSubAndTotalStatistics4Table($tbody,{column:12, subCount:jsonObject.data.length,count:jsonObject.page.totalElements,7:{subTotal:totalAccount,total:jsonObject.page.header.totalAmount},8:{subTotal:totalFee,total:jsonObject.page.header.totalFee}});
		showPading(jsonObject.page,"encashStatusPage",showEncashmentList);
	},initPage:function (his) {
	}});
}
initTimePicker(true,$("[name=startAndEndTime]"),7);
getHandicap_select($("select[name='search_EQ_handicapId']"),null,"全部");
$("#accountFilter").find("[name=search_EQ_handicapId]").change(function(){
	showEncashmentList();
});
$("#searchBtnModal").click(function(){
	showEncashmentList();
});
showEncashmentList();