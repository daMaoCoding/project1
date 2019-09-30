currentPageLocation = window.location.href;

var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml = '<tr>\
		<td><span>{handicapName}</span></td>\
		<td><span>{currSysLevelName}</span></td>\
		<td><span>{alias}</span></td>\
		<td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"  >{accountInfo}</a></td>\
		<td><span>{flagStr}</span></td>\
		<td>{typeStr}</td>\
		<td><span class="label label-sm {classOfStatus}">{statusName}</span></td>\
		<td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
		<td><div class="SysLogEvent" target="{id}"><span class="amount">{balance}</span></div></td>\
		<td>\
			<a {inCountMappingEle} target="_self" class="contentRight" contentRight="IncomeAsignAlipay:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
			<a {inCountMappedEle} target="_self" class="contentRight" contentRight="IncomeAsignAlipay:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
			<a {inCountCancelEle} target="_self" class="contentRight" contentRight="IncomeAsignAlipay:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
			<span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId={id}>{matchingAmtIn}</span>\
		</td>\
		<td>\
		 <a {outCountMappingEle} target="_self" class="contentRight" contentRight="IncomeAsignAlipay:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
		 <a {outCountMappedEle} target="_self"  class="contentRight" contentRight="IncomeAsignAlipay:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
		 <a {outCountCancelEle} target="_self"  class="contentRight" contentRight="IncomeAsignAlipay:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{outCountCancel}</span></a>\
		 <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
		</td>\
		<td>'
			+'<button class="btn btn-xs btn-white btn-bold btn-info" onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>明细</span></button>'
		+'</td>\
	</tr>';


/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
    var $div = $("#accountFilter");
    if(!!!CurPage&&CurPage!=0) CurPage=$("#accountPage .Current_Page").text();
    authRequest.pageNo = CurPage<=0?0:CurPage-1;
    authRequest.search_LIKE_account = $.trim($div.find("input[name='search_LIKE_account']").val());
    authRequest.search_LIKE_owner = $.trim($div.find("input[name='search_LIKE_owner']").val());
    authRequest.bankType=$.trim($div.find("select[name='search_LIKE_bankType']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    authRequest.search_LIKE_alias = $.trim($div.find("input[name='search_LIKE_alias']").val());
    authRequest.search_IN_handicapId =  $div.find("select[name='search_EQ_handicapId']").val().toString();
    var statusToArray = new Array();
    $div.find("input[name='search_IN_status']:checked").each(function(){  statusToArray.push(this.value); });
    if(statusToArray.length==0){
        statusToArray=[accountStatusNormal,accountStatusEnabled,accountStatusStopTemp];
    }
    authRequest.statusToArray = statusToArray.toString(),
        authRequest.typeToArray = [accountTypeBindAli].toString();
    authRequest.sortProperty='status';
    authRequest.sortDirection=0;
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.currSysLevel=$div.find("input[name='currSysLevel']:checked").val();
    authRequest.search_EQ_flag=$("input[name='flag']:checked").val();
    authRequest.deviceStatus=$("#deviceStatus").val();
    $.ajax({ dataType:'JSON',type:"POST", async:false, url:API.r_account_list, data:authRequest,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail("查询失败："+jsonObject.message);
            return;
        }
        var $tbody=$("table#accountListTable").find("tbody");
        var totalBalanceBySys = 0,totalBalanceByBank =0 ,totalIncomeAmountDaily=0;
        $.each(jsonObject.data,function(index,record){
        	record.flagStr=getFlagStr(record.flag);
            record.handicapName=record.handicapName?record.handicapName:'';
            record.alias=(record.alias&&record.alias!='null')?record.alias:'';
            record.currSysLevelName=record.currSysLevelName?record.currSysLevelName:'';
            record.classOfStatus=(record.status==accountStatusFreeze || record.status==accountStatusStopTemp)?'label-danger':'label-success';
            record.limitIn=(!!!record.limitIn)?eval(sysSetting.INCOME_LIMIT_CHECKIN_TODAY):record.limitIn;
            record.DailyAmount=htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily);
            record.classHiddenOfBtnBindList =  record.type==accountTypeThirdCommon ?'hidden':'contentRight';
            totalIncomeAmountDaily+=record.incomeAmountDaily*1;
            totalBalanceBySys+=record.balance*1;
            totalBalanceByBank+=record.bankBalance*1;
            record.limitBalanceIcon=getlimitBalanceIconStr(record);
            record.bankBalance=record.bankBalance?record.bankBalance:'0';
            record.balance=record.balance?record.balance:'0';
            record.incomeDetail=getRecording_Td(record.id,"incomeDetail");
            record.outDetail=getRecording_Td(record.id,"outDetail");
            record.inCountMapping ='0';
            record.inCountMapped ='0';
            record.inCountCancel ='0';
            record.matchingAmtIn ='';
            record.outCountMapping ='0';
            record.outCountMapped ='0';
            record.outCountCancel ='0';
            record.matchingAmtOut ='';
            record.bankAcked='---';
            record.accountStatInOutCategoryOutTranfer=accountStatInOutCategoryOutTranfer+'';
            record.accountStatInOutCategoryIn=accountStatInOutCategoryIn+'';
            record.inCountMappingEle=(record.inCountMapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
            record.inCountMappedEle=(record.inCountMapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
            record.inCountCancelEle=(record.inCountCancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
            record.outCountMappingEle=(record.outCountMapping!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
            record.outCountMappedEle=(record.outCountMapped!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
            record.outCountCancelEle=(record.outCountCancel!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
            record.hideAccount=hideAccountAll(record.account);
            record.accountInfo =record.bankType+'|'+record.owner+'<br/>'+record.hideAccount ;
        });
        $tbody.html(fillDataToModel4Array(jsonObject.data,trHtml));
        showSubAndTotalStatistics4Table($tbody,{column:12,subCount:jsonObject.data.length,count:jsonObject.page.totalElements,8:{subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance},9:{subTotal:totalBalanceBySys,total:jsonObject.page.header.totalAmountBalance}});
        showPading(jsonObject.page,"accountPage",showAccountList);
        //账号悬浮提示
        var idList=new Array(),idArray = new Array();
        $.each(jsonObject.data,function(index,result){
            idList.push({'id':result.id});
            idArray.push(result.id);
        });
        loadHover_accountInfoHover(idList);
		//入款卡统计总卡数量
		if(jsonObject&&jsonObject.page&&jsonObject.page.header&&jsonObject.page.header.IdSize){
			var IdSize=jsonObject.page.header.IdSize;
			$("#totalIdSize").text(IdSize[0]);
			$("#onlineSize").text(IdSize[1]);
			$("#stopSize").text(IdSize[2]);
			$("#offlineSize").text(IdSize[3]);
		}
        loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutTranfer],idArray,null,function(){ contentRight() });
        contentRight();
        SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
    }
    });
}



var doDeleteAccount=function(accountId){
    bootbox.confirm("确定要删除吗 ?", function(result) {
        if (result) {
            $.ajax({
                dataType:'JSON',
                type:"PUT",
                async:false,
                url:API.r_account_del,
                data:{
                    id:accountId
                },
                success:function(jsonObject){
                    if(-1==jsonObject.status){
                        showMessageForFail("操作失败："+jsonObject.message);
                        return;
                    }else{
                        showMessageForSuccess("删除成功");
                    }
                    //操作成功刷新数据
                    showAccountList();
                }
            });
        }
    });
}


getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部")
$("#accountFilter").find("[name=currSysLevel],[name=search_IN_status],[name=flag]").click(function(){
    showAccountList();
});
$("#accountFilter").find("[name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
    showAccountList();
});
/** 切换设备状态TAB */
var changeDevice=function(deviceStaus){
	$("#deviceStatus").val(deviceStaus);
	showAccountList();
}
contentRight();
showAccountList(0);
$("#accountFilter").keypress(function(e){
    if(event.keyCode == 13) {
        $("#accountFilter #searchBtn button").click();
    }
});

