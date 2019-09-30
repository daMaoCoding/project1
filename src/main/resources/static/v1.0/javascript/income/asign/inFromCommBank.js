var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml='\
            <tr>\
                <td><span>{handicapName}</span></td>\
				<td><span>{currSysLevelName}</span></td>\
				<td><span>{alias}</span></td>\
                <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
				<td>{statusHTML}</td>\
				<td><span>{flagStr}</span></td>\
                <td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
                <td><div class="SysLogEvent" target="{id}"><span class="amount">{balance}</span></div></td>\
				<td>\
				    <a {inCountMappingEle} target="_self" class="contentRight" contentRight="IncomeAsignComnBank:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
				    <a {inCountMappedEle} target="_self"  class="contentRight" contentRight="IncomeAsignComnBank:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
				    <a {inCountCancelEle} target="_self"  class="contentRight" contentRight="IncomeAsignComnBank:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
				    <span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId={id}>{matchingAmtIn}</span>\
				</td>\
				<td>\
				    <a {outCountMappingEle} target="_self" class="contentRight" contentRight="IncomeAsignComnBank:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
				    <a {outCountMappedEle} target="_self"  class="contentRight" contentRight="IncomeAsignComnBank:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
				    <a {outCountCancelEle} target="_self"  class="contentRight" contentRight="IncomeAsignComnBank:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{outCountCancel}</span></a>\
				    <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
				</td>\
                <td>'
					+'<button class="btn btn-xs btn-white btn-warning btn-bold orange contentRight" onclick="showTransferBalanceModal({id},refreshContent)" contentRight="IncomeAsignComnBank:TransferOut:*"><i class="ace-icon fa fa-exchange bigger-100"></i><span>转账</span></button>'
					+'<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>'
				+'</td>\
             </tr>';
function showContent(data,page){
    if(!!!data)return;
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var   totalBalanceBySys = 0,totalBalanceByBank =0 ,idList=new Array,idArray= new Array();
    for (var index in data) {
        var item = data[index];
    	totalBalanceBySys = totalBalanceBySys + item.balance ;
        totalBalanceByBank = totalBalanceByBank + item.bankBalance ;
        item.flagStr=getFlagStr(item.flag);
        item.handicapName=item.handicapName?item.handicapName:'';
        item.outCountMapped=!!!item.outCount.mapped?"0":item.outCount.mapped;
        item.outCountMapping=!!!item.outCount.mapping?"0":item.outCount.mapping;
        item.outCountCancel=!!!item.outCount.cancel?"0":item.outCount.cancel;
        item.inCountMapped=!!!item.inCount.mapped?"0":item.inCount.mapped;
        item.inCountMapping=!!!item.inCount.mapping?"0":item.inCount.mapping;
        item.inCountCancel=!!!item.inCount.cancel?"0":item.inCount.cancel;
        item.statusHTML=getStatusInfoHoverHTML(item)+"<br/>"+getDeviceStatusInfoHoverHTML(item);
        item.DailyAmount =htmlDailyAmount(0,item.limitIn,item.incomeAmountDaily);
        item.balance = item.balance?item.balance:'0';
        item.limitBalanceIcon=getlimitBalanceIconStr(item);
        item.bankBalance=item.bankBalance?item.bankBalance:'0';
        item.inCountMappingEle=' href="#/EncashCheck4Transfer:*?toAccountId='+item.id+'&toAccount='+item.account+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" ';
        item.inCountMappedEle=(item.inCountMapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+item.id+'&toAccount='+item.account+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        item.inCountCancelEle=(item.inCountCancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+item.id+'&toAccount='+item.account+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        item.outCountMappingEle=(item.outCountMapping!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+item.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        item.outCountMappedEle=(item.outCountMapped!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+item.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        item.outCountCancelEle=(item.outCountCancel!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+item.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        item.accountStatInOutCategoryIn = accountStatInOutCategoryIn+'';
        item.accountStatInOutCategoryOutTranfer=accountStatInOutCategoryOutTranfer+'';
        item.alias=item.alias?item.alias:'';
        item.currSysLevelName= item.currSysLevelName?item.currSysLevelName:'';
        item.hideAccount=hideAccountAll(item.account);
        item.accountInfo =item.bankType+'|'+item.owner+'<br/>'+item.hideAccount ;
        idList.push({'id':item.id});
        idList.push({'id':item.id,type:'transAskMonitorRiskHover'});
        idArray.push(item.id);
    }
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:data.length,count:page.totalElements,7:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance},8:{subTotal:totalBalanceBySys,total:page.header.totalAmountBalance}});
    loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutTranfer],idArray,null,function(){ loadContentRight() });
    SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
}

/** 装载所有盘口以code为key */
var getHandicapBatchInfoByCode=function(){
    var result={};
    $.map(handicap_list_all,function(record){
        result[record.code]=record;
    });
    return result;
};

function refreshContent(pageNo){
    authRequest.typeToArray= [accountTypeThirdCommon,accountTypeBindCommon].toString();
    authRequest.pageNo=(pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0);
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    authRequest.pageNo=authRequest.pageNo?authRequest.pageNo:0;
    authRequest.search_IN_handicapId=$("[name=search_EQ_handicapId]").val().toString();;
    var statusArray=new Array();
    $.each($("input[name='search_IN_status_type']:checked"),function(index,result){
    		statusArray.push($(result).val());
    });
	if(statusArray.length==0){
		statusArray=[accountStatusNormal,accountStatusEnabled,accountStatusStopTemp];
    }
    var handicapCode = $("select[name='search_EQ_handicapCode']").val();
    authRequest.statusToArray= statusArray.toString();
    authRequest.sortProperty='status';
    authRequest.sortDirection=0;
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.search_LIKE_account = $.trim($("input[name='search_LIKE_account']").val());
    authRequest.search_LIKE_alias = $.trim($("input[name='search_LIKE_alias']").val());
    authRequest.bankType=$.trim($("select[name='search_LIKE_bankType']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    authRequest.currSysLevel=$("input[name='currSysLevel']:checked").val();
    authRequest.search_EQ_flag=$("input[name='flag']:checked").val();
    authRequest.deviceStatus=$("#deviceStatus").val();
    $.ajax({type:"get", url:API.r_account_list, data:authRequest, dataType:'json', success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data,jsonObject.page);
            showPading(jsonObject.page,"accountPage",refreshContent,null,false,true);
			//统计总卡数量
			if(jsonObject&&jsonObject.page&&jsonObject.page.header&&jsonObject.page.header.IdSize){
				var IdSize=jsonObject.page.header.IdSize;
				$("#totalIdSize").text(IdSize[0]);
				$("#onlineSize").text(IdSize[1]);
				$("#stopSize").text(IdSize[2]);
				$("#offlineSize").text(IdSize[3]);
			}
            loadContentRight();
        }else{
            bootbox.alert(jsonObject.message);
        }
    }, error:function(result){bootbox.alert(result);},initPage:function(his){
        var $form = $('#accountFilter');
        $form.find('input[name=search_LIKE_alias]').val(his.search_LIKE_alias);
        $form.find('input[name=search_LIKE_account]').val(his.search_LIKE_account);
        $form.find('input[name=bankType]').val(his.bankType);
        $form.find('input[name=search_IN_status_type]').prop('checked',false);
        $.each(his.statusToArray.split(','),function(index,obj){
            $form.find('input[name=search_IN_status_type][value='+obj+']').prop('checked',true);
        })
    }});
}

function loadContentRight(){
    contentRight({'IncomeAsignComnBank:CheckTransferIn:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }, 'IncomeAsignComnBank:TransferHisOut:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }});
}

$('#dynamic-table').find('tbody').html('');
bootbox.setLocale("zh_CN");

getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
$("#accountFilter [name=currSysLevel],[name=search_IN_status_type],[name=flag]").click(function(){
	refreshContent();
});
$("#accountFilter [name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
	refreshContent();
});
/** 切换设备状态TAB */
var changeDevice=function(deviceStaus){
	$("#deviceStatus").val(deviceStaus);
	refreshContent();
}
initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #search-button"),149,"refresh_inFromCommBank");
refreshContent(0);
