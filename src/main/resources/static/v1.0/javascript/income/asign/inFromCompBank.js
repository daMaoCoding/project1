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
					<a {outCountMappingEle} target="_self" class="contentRight" contentRight="IncomeAsignCompBank:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
				    <a {outCountMappedEle}  target="_self" class="contentRight" contentRight="IncomeAsignCompBank:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
				    <a {outCountCancelEle}  target="_self" class="contentRight" contentRight="IncomeAsignCompBank:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{outCountCancel}</span></a>\
                    <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
                </td>\
                <td>'
					+'<button class="btn btn-xs btn-white btn-warning btn-bold orange contentRight" onclick="showTransferBalanceModal({id},refreshContent)" contentRight="IncomeAsignCompBank:TransferOut:*"><i class="ace-icon fa fa-exchange bigger-100"></i><span>转账</span></button>'
					+'<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>'
				+'</td>\
             </tr>';
function showContent(data,page){
    if(!!!data)return;
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var totalBalanceBySys = 0, totalBalanceByBank =0 ,totalDailyIncome=0,idList=new Array,idArray= new Array();
    for (var index in data) {
        var item = data[index];
        totalBalanceBySys = totalBalanceBySys + item.balance ;
        totalBalanceByBank = totalBalanceByBank + item.bankBalance ;
        totalDailyIncome = totalDailyIncome + item.incomeAmountDaily;
        item.flagStr=getFlagStr(item.flag);
        item.statusHTML=getStatusInfoHoverHTML(item)+"<br/>"+getDeviceStatusInfoHoverHTML(item)
        item.DailyAmount=htmlDailyAmount(0,item.bindLimitIn,item.incomeAmountDaily);
        item.outCountMapped=!!!item.outCount.mapped?"0":item.outCount.mapped;
        item.outCountMapping=!!!item.outCount.mapping?"0":item.outCount.mapping;
        item.outCountCancel=!!!item.outCount.cancel?"0":item.outCount.cancel;
        item.htmlExceedInLimit =(item.bindInAmountDaily- item.limitIn )> 0 ? "<i class=\"fa fa-flag red bigger-130\"></i>":"";
        item.balance=(!!!item.balance)?"0":item.balance;
        item.bankBalance=(!!!item.bankBalance)?'0':item.bankBalance;
        item.incomeAmountDaily=(item.incomeAmountDaily?setAmountAccuracy(item.incomeAmountDaily):item.incomeAmountDaily) +'';
        item.limitBalanceIcon = getlimitBalanceIconStr(item);
        item.outCountMappingEle=(item.outCountMapping!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+item.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        item.outCountMappedEle=(item.outCountMapped!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+item.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        item.outCountCancelEle=(item.outCountCancel!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+item.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        item.accountStatInOutCategoryOutTranfer=accountStatInOutCategoryOutTranfer+'';
        item.alias=(item.alias&&item.alias!='null')?item.alias:'';
        item.currSysLevelName=item.currSysLevelName?item.currSysLevelName:'';
        item.hideAccount=hideAccountAll(item.account);
        item.accountInfo =(item.bankType?item.bankType:'无')+'|'+item.owner+'<br/>'+item.hideAccount ;
        item.matchingAmtOut ='';
        item.matchingAmtIn ='';
        item.platAcked ='---';
        item.bankAcked ='---';
        idList.push({'id':item.id});
        idArray.push(item.id);
    }
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:data.length,count:page.totalElements,7:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance},8:{subTotal:totalBalanceBySys,total:page.header.totalAmountBalance}});
    loadEncashCheckAndStatus([accountStatInOutCategoryOutTranfer],idArray,null,function(){ loadContentRight() });
    SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
}

function refreshContent(pageNo){
    authRequest.typeToArray= [accountTypeInBank].toString();
    authRequest.pageNo=(pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0);
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    authRequest.pageNo=authRequest.pageNo?authRequest.pageNo:0;
    if(!authRequest.statusToArray){
    	var statusArray=new Array();
    	$.each($("input[name='search_IN_status_type']:checked"),function(index,result){
    		statusArray.push($(result).val());
    	});
        authRequest.statusToArray= statusArray.toString();
    }
    authRequest.sortProperty='status';
    authRequest.sortDirection=0;
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.currSysLevel=$("input[name='currSysLevel']:checked").val();
    authRequest.search_EQ_flag=$("input[name='flag']:checked").val();
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
        $form.find('select[name=search_EQ_handicapId]').val(his.search_EQ_handicapId);
        $form.find('select[name=search_EQ_LevelId]').val(his.search_EQ_LevelId);
        $form.find('input[name=search_LIKE_alias]').val(his.search_LIKE_alias);
        $form.find('input[name=search_LIKE_account]').val(his.search_LIKE_account);
        $form.find('select[name=search_LIKE_bankType]').val(his.bankType);
        $form.find('input[name=search_IN_status_type]').prop('checked',false);
        $.each(his.statusToArray.split(','),function(index,obj){
            $form.find('input[name=search_IN_status_type][value='+obj+']').prop('checked',true);
        })
    }});
}

function loadContentRight(){
    contentRight({'IncomeAsignCompBank:TransferHisOut:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }});
}

function searchByFilter(){
    authRequest.search_LIKE_account = $("[name='search_LIKE_account']").val();
    authRequest.search_IN_handicapId = $("select[name='search_EQ_handicapId']").val().toString();
    authRequest.search_LIKE_alias = $("input[name='search_LIKE_alias']").val();
    authRequest.levelId = $("select[name='search_EQ_LevelId']").val();
    authRequest.bankType=$.trim($("select[name='search_LIKE_bankType']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    var tempStatusToArray = new Array();
    $("input[name='search_IN_status_type']:checked").each(function(){
        tempStatusToArray.push(this.value);
    });
    var statusToArray = new Array();
    for(var index0 in tempStatusToArray){
        var item0 = tempStatusToArray[index0].split(",");
        for(var index1 in item0){
            statusToArray.push(item0[index1]);
        }
    }
	if(statusToArray.length==0){
		statusToArray=[accountStatusNormal,accountStatusEnabled,accountStatusStopTemp];
    }
    statusToArray = removeDuplicatedItem(statusToArray);
    authRequest.statusToArray=statusToArray.toString();
    authRequest.typeToArray=[accountTypeInBank].toString();
    authRequest.deviceStatus=$("#deviceStatus").val();
    refreshContent(0);
}

$("#accountFilter [name=search_EQ_handicapId],[name=search_EQ_LevelId],[name=search_LIKE_account],[name=search_LIKE_bankType]").change(function(){
	searchByFilter();
});
$("#accountFilter [name=currSysLevel],[name=search_IN_status_type],[name=flag],#search-button").click(function(){
	searchByFilter();
});
/** 切换设备状态TAB */
var changeDevice=function(deviceStaus){
	$("#deviceStatus").val(deviceStaus);
	searchByFilter();
}
$("#search-button").on(ace.click_event, function() {searchByFilter();});

$('#dynamic-table').find('tbody').html('');
bootbox.setLocale("zh_CN");

getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});



initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #search-button"),150,"refresh_inFromCompBank");
searchByFilter();

