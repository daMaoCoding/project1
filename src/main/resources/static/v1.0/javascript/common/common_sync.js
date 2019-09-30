/** 账号同步到平台交互文件 */
var modifyBindCardStatus2=function(param){
	var result=false;
	$.ajax({
		dataType: 'JSON',
		contentType: 'application/json;charset=UTF-8',
		type: "POST",
		async:false,
		url: "/newpay/modifyBindCardStatus2",
		data: JSON.stringify(param),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				result=true;
			}
		}
	});
	return result;
}
var updateStatus_sync=function(accountId,newStatus,accountInfo){
	if(!accountInfo) accountInfo=getAccountInfoById(accountId);
	//非入款卡不进行同步
	if(accountInfo.type!=accountTypeInBank) return;
	//状态
	if(newStatus==accountStatusNormal) newStatus=1
	else if(newStatus==accountStatusStopTemp) newStatus=0
	else if(newStatus==accountStatusFreeze) newStatus=4
	else return;
	//盘口
	var handicapInfo=getHandicapInfoById(accountInfo.handicapId);
	if(!handicapInfo) return;
	var param=[{
			"oid":handicapInfo.code,
			"cardPayeeCol":[{
				"cardNo":accountInfo.account,
				"payeeName":accountInfo.owner
			}],
			"status":newStatus,// 是 0：停用 ，1：启用，2：被平台禁用，3：新卡，4：冻结，5：删除
			"operationAdminName":getCookie('JUID')
	}]
	if(modifyBindCardStatus2(param)){
		showMessageForSuccess("平台状态修改成功");
	}else{
		showMessageForFail("平台状态修改失败");
	}
}