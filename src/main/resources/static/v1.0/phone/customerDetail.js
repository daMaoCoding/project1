currentPageLocation = window.location.href;
var request=getRequest();

/** 查询入款历史记录 */
var searchInCountHistory=function(CurPage){
	var $div=$("#tabInHistory");
    if (!CurPage && CurPage != 0) CurPage = $("#InCountHistory_page").find(".Current_Page").text();
    var startAndEndTime = $div.find('input[name=startAndEndTime]').val(),timeStart,timeEnd;
    if (startAndEndTime) {
        var stEdTm = startAndEndTime.split(' - ');
        timeStart = new Date(stEdTm[0]).getTime() + '';
        timeEnd = new Date(stEdTm[1]).getTime() + '';
    }
    if(!timeStart||!timeEnd){
    	showMessageForCheck("请先选择日期！");
    	return;
    }
	var params={
			oid:request.oid,
			inoutType:$div.find("[name=inoutType]:checked").val(),
			type:$div.find("[name=accountType]:checked").val(),
			mobileId:request.id,
			moneyStart:$div.find("[name=startAmt]").val(),
			moneyEnd:$div.find("[name=endAmt]").val(),
			timeStart:timeStart,
			timeEnd:timeEnd,
			pageNo:CurPage <= 0 ? 0 : CurPage - 1,
			pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
	}
	 $.ajax({
        type: "POST",
        contentType: 'application/json;charset=UTF-8',
        dataType: 'JSON',
        url: '/newpay/find8ByCondition',
        async: false,
        data: JSON.stringify(params),
        success: function (jsonObject) {
            if (jsonObject.status == 1) {
            	var $tbody=$div.find("tbody").html(""),trs="";
            	if(jsonObject.data){
            		$.map(jsonObject.data,function(record){
            			trs+="<tr>";
            			trs+="<td><span>"+record.inAccount+"</span></td>";
            			trs+="<td><span>"+record.money+"</span></td>";
            			trs+="<td><span>"+(record.tradeCode?record.tradeCode:"")+"</span></td>";
            			trs+="<td><span>"+record.createtime+"</span></td>";//交易时间
            			trs+="<td><span>"+(record.reporttime?record.reporttime:"")+"</span></td>";//上报时间
            			trs+="<td><span>"+(record.chkRemark?record.chkRemark:"")+"</span></td>";
            			trs+="<td><span>"+(record.remark?_showRemarkNewPay(record.remark):"")+"</span></td>";
            			trs+="</tr>";
            		});
            		$tbody.html(trs);
            	}
        		showPading(jsonObject.page,"InCountHistory_page",searchInCountHistory,null,true);
            } else {
                showMessageForFail("查询失败：" + jsonObject.message);
            }
        }
    });
}

/** 查询入佣金历史记录 */
var searchCommissionHistory=function(CurPage){
	var $div=$("#tabCommission");
    if (!CurPage && CurPage != 0) CurPage = $("#CommissionHistory_page").find(".Current_Page").text();
    var startAndEndTime = $div.find('input[name=startAndEndTime]').val(),timeStart,timeEnd;
    if (startAndEndTime) {
        var stEdTm = startAndEndTime.split(' - ');
        timeStart = new Date(stEdTm[0]).getTime() + '';
        timeEnd = new Date(stEdTm[1]).getTime() + '';
    }
    if(!timeStart||!timeEnd){
    	showMessageForCheck("请先选择日期！");
    	return;
    }
	var params={
			oid:request.oid,
			mobileId:request.id,
			moneyStart:$div.find("[name=startAmt]").val(),
			moneyEnd:$div.find("[name=endAmt]").val(),
			timeStart:timeStart,
			timeEnd:timeEnd,
			pageNo:CurPage <= 0 ? 0 : CurPage - 1,
			pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
	}
	 $.ajax({
        type: "POST",
        contentType: 'application/json;charset=UTF-8',
        dataType: 'JSON',
        url: '/newpay/findCommissionDetailByCondition',
        async: false,
        data: JSON.stringify(params),
        success: function (jsonObject) {
            if (jsonObject.status == 1) {
            	var $tbody=$div.find("tbody").html(""),trs="";
            	if(jsonObject.data){
            		$.map(jsonObject.data,function(record){
            			trs+="<tr>";
            			trs+="<td><span>"+record.commissionBankNum+"</span></td>";
            			trs+="<td><span>"+record.commissionBankName+"</span></td>";
            			trs+="<td><span>"+record.commissionOpenMan+"</span></td>";
            			trs+="<td><span>"+record.money+"</span></td>";
            			trs+="<td><span>"+record.status+"</span></td>";
            			trs+="<td><span>"+(record.createtime?record.createtime:"")+"</span></td>";
            			trs+="<td><span>"+(record.remark?record.remark:"")+"</span></td>";
            			trs+="</tr>";
            		});
            		$tbody.html(trs);
            	}
        		showPading(jsonObject.page,"CommissionHistory_page",searchCommissionHistory,null,true);
            } else {
                showMessageForFail("查询失败：" + jsonObject.message);
            }
        }
    });
}


initTimePicker(true,$("[name=startAndEndTime]"),23);
$(document).ready(function(){
	if(request.type=='commission'){
		//切换到返佣TAB
		$("#commissionTab").click();
	}else{
		if(request.type=='out'){
			//选中出款
			$("[name=inoutType][value=0]").removeAttr("checked");
			$("[name=inoutType][value=1]").prop("checked","checked");
		}
		searchInCountHistory(0);
	}

});
$("[name=inoutType],[name=accountType]").click(function(){
	searchInCountHistory();
});
