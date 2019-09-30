//同步账号JS

var showSyncAccountList=function(CurPage){
	var $div=$("#syncAccountFilter");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#syncAccountList_page .Current_Page").text();
    var statusToArray = new Array();
    $div.find("input[name='search_IN_status_sync']:checked").each(function(){
        statusToArray.push(this.value);
    });
    if(statusToArray.length==0){
        statusToArray=[accountStatusNormal,accountStatusStopTemp,accountStatusEnabled];
    }
	var params={
			handicapId:$div.find("select[name='search_EQ_handicapId_sync']").val(),
	        account:$div.find("[name='search_LIKE_account_sync']").val(),
	        bankName:$div.find("[name='search_LIKE_bankName_sync']").val(),
	        status:statusToArray,
			pageNo:(!CurPage||CurPage<=0)?0:CurPage-1,
			pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
	};
	var jsonObject=ali4epAccountList(params);
	if (jsonObject&&jsonObject.status == 1 ) {
    	result = jsonObject.data;
		var trs="",$tbody=$("#syncAccountListTable tbody");
		$.each(result,function(index,record){
			trs+="<tr>";
			trs+="<td>"+_showHandicapNameByIdOrCode(record.handicapId)+"</td>";
			trs+="<td>"+_checkObj(record.account)+"</td>";
			trs+="<td>"+_checkObj(record.bankName)+"</td>";
			var color=(record.status == accountStatusFreeze ||record.status == accountStatusStopTemp)?'label-danger':(record.status == accountStatusEnabled?'label-warning':'label-success');
			trs+="<td><span class='label label-sm "+color+"'>"+_checkObj(record.statusStr)+"</span></td>";
			trs+="<td>"+timeStamp2yyyyMMddHHmmss(_checkObj(record.createTime))+"</td>";
			trs+="<td>"+_checkObj(record.remark)+"</td>";
			trs+="</tr>";
		});
		$tbody.html(trs);
		//分页初始化
		var pageObject=ali4enterpriseCount(params);
		if (pageObject&&pageObject.status == 1 ) {
	    	page = pageObject.page;
	    	showPading(page,"syncAccountList_page",searchContent_infoLoad,null,true);
		} else {
			showMessageForFail("分页查询失败：" + pageObject.message);
		}
//		contentRight();
	} else {
		showMessageForFail("数据列表查询失败：" + jsonObject.message);
	}
}


$("#syncAccountFilter").find("[name=search_IN_status_sync]").click(function(){
	showSyncAccountList(0);
});
$("#syncAccountFilter").find("[name=search_EQ_handicapId_sync]").change(function(){
	showSyncAccountList(0);
});
/** 同步账号 查询 */
var ali4epAccountList=function(param){
	var result;
	$.ajax({
		dataType: 'JSON',
		contentType: 'application/json;charset=UTF-8',
		type: "POST",
		url: "/ali4epAccount/list",
		async:false, 
		data: JSON.stringify(param),
		success: function (jsonObject) {
			result=jsonObject;
		}
	});
	return result;
}

/** 同步账号 查询分页 */
var ali4enterpriseCount=function(param){
	var result;
	$.ajax({
		dataType: 'JSON',
		contentType: 'application/json;charset=UTF-8',
		type: "POST",
		url: "/ali4epAccount/count",
		async:false, 
		data: JSON.stringify(param),
		success: function (jsonObject) {
			result=jsonObject;
		}
	});
	return result;
}