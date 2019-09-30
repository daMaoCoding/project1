currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var HandicapBatch = getHandicapBatchInfoByCode();
var trHtml='<tr>\
            <td><span>{handicapName}</span></td>\
            <td><span>{currSysLevelName}</span></td>\
            <td><span>{alias}</span></td>\
            <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
            <td style="width:75px;"><span class="label label-sm {classOfStatus}">{statusName}</span></td>\
			<td><span>{flagStr}</span></td>\
            <td><span class="bankAcked" accountStatInOutId="{id}">{bankAcked}</span></td>\
            <td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}</span><span class="time"></span></div></td>\
            <td><div class="SysLogEvent" target="{id}"><span class="amount">{balance}</span></div></td>\
			<td style="width:90px;">\
                 <a {outCountMappingEle} target="_self" class="contentRight" contentRight="OutwardAccountStorageReserved:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
                 <a {outCountMappedEle} target="_self"  class="contentRight" contentRight="OutwardAccountStorageReserved:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
                 <a {outCountCancelEle} target="_self"  class="contentRight" contentRight="OutwardAccountStorageReserved:TransferHisOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{outCountMapped}</span></a>\
                <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
            </td>\
		    <td style="width:90px;">\
			    <a {inCountMappingEle} target="_self" class="contentRight" contentRight="OutwardAccountStorageReserved:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
			    <a {inCountMappedEle} target="_self"  class="contentRight" contentRight="OutwardAccountStorageReserved:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
			    <a {inCountCancelEle} target="_self"  class="contentRight" contentRight="OutwardAccountStorageReserved:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
			    <span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId={id}>{matchingAmtIn}</span>\
			 </td>\
            <td style="width:250px;">'
                +'<button class="btn btn-xs btn-white btn-warning btn-bold orange {otherStatus}" style="{cssStop}" onclick="showStopTempModal({id},refreshContent)"><i class="ace-icon fa fa-stop bigger-100 red"></i><span>停用</span></button>'
                +'<button class="btn btn-xs btn-white btn-warning btn-bold orange {otherStatus}" style="{cssUsed}" onclick="showNormalModal({id},refreshContent)"><i class="ace-icon fa fa-exchange bigger-100 red"></i><span>在用</span></button>'
                +'<button class="btn btn-xs btn-white btn-primary btn-bold {otherStatus}" style="{cssFreeze}" onclick="showFreezeModal({id},refreshContent);" ><i class="ace-icon fa fa-remove bigger-100 orange"></i><span>冻结</span></button>'
                +'<button class="btn btn-xs btn-white btn-warning btn-bold orange {otherStatus} {freezeStatus}" onclick="showNormalModal({id},refreshContent)" ><i class="ace-icon fa fa-reply bigger-100 red"></i><span>恢复</span></button>'
                +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight {otherStatus}" contentRight="OutwardAccountStorageReserved:Update:*"  onclick="updAccount({id},refreshContent)"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
                +'<button class="btn btn-xs btn-white btn-primary btn-bold orange {OperatorLogBtn}" onclick="showModal_accountExtra({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
                +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showInOutListModal({id});"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>'
                +'</td>\
        </tr>';

function updAccount(id){
    showUpdateOutAccount(id ,function(){
        refreshContent();
    });
}

function addAccount(){
    showAddOutAccount(accountTypeReserveBank,accountInactivated ,function(){
        refreshContent();
    });
}

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var  totalBalanceByBank =0 ,totalDailyOutward=0,totalDailyIncome=0,idList=new Array,idArray= new Array();
    $.each(data,function(idx, obj) {
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
        totalDailyOutward = totalDailyOutward +obj.outwardAmountDaily;
        totalDailyIncome = totalDailyIncome + obj.incomeAmountDaily;
        obj.flagStr=getFlagStr(obj.flag);
        obj.OperatorLogBtn=OperatorLogBtn;
        obj.classOfStatus=(obj.status ==accountStatusStopTemp)?'label-warning':((obj.status ==accountStatusEnabled)?'label-purple':(obj.status ==accountStatusFreeze?'label-danger':'label-success'));
        obj.freezeStatus=(obj.status ==accountStatusFreeze?'':' hide '); 
        obj.otherStatus=(obj.status !=accountStatusFreeze?'':' hide '); 
        obj.limitOut=(!!!obj.limitOut)?eval(sysSetting.OUTDRAW_LIMIT_CHECKOUT_TODAY):obj.limitOut;
        obj.limitIn=(!!!obj.limitIn)?eval(sysSetting.INCOME_LIMIT_CHECKIN_TODAY):obj.limitIn;
        obj.htmlExceedOutLimit =(obj.outwardAmountDaily- obj.limitOut )> 0 ? "<i class=\"fa fa-flag red bigger-130\"></i>":"";
        obj.htmlExceedInLimit =(obj.incomeAmountDaily- obj.limitIn )> 0 ? "<i class=\"fa fa-flag red bigger-130\"></i>":"";
        obj.incomeRequestStatusMatching=incomeRequestStatusMatching+'';
        obj.incomeRequestStatusMatched=incomeRequestStatusMatched+'';
        obj.incomeRequestStatusCanceled=incomeRequestStatusCanceled+'';
        obj.inCountMapped=!!!obj.inCount.mapped?"0":obj.inCount.mapped;
        obj.inCountMapping=!!!obj.inCount.mapping?"0":obj.inCount.mapping;
        obj.inCountCancel=!!!obj.inCount.cancel?"0":obj.inCount.cancel;
        obj.outCountMapped=!!!obj.outCount.mapped?"0":obj.outCount.mapped;
        obj.outCountMapping=!!!obj.outCount.mapping?"0":obj.outCount.mapping;
        obj.outCountCancel=!!!obj.outCount.cancel?"0":obj.outCount.cancel;
        obj.inCountMappedEle=(obj.inCount.mapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.inCountMappingEle=(obj.inCount.mapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.inCountCancelEle=(obj.inCount.cancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountMappingEle=(obj.outCount.mapping!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountMappedEle=(obj.outCount.mapped!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountCancelEle=(obj.outCount.cancel!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outwardAmountDaily=obj.outwardAmountDaily+'';
        obj.incomeAmountDaily= obj.incomeAmountDaily+'';
        obj.balance = obj.balance?obj.balance:'0';
        obj.limitBalanceIcon=getlimitBalanceIconStr(obj);
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.accountStatInOutCategoryIn = accountStatInOutCategoryIn+'';
        obj.accountStatInOutCategoryOutMember = accountStatInOutCategoryOutMember+'';
        obj.accountStatInOutCategoryOutTranfer = accountStatInOutCategoryOutTranfer+'';
        obj.alias= $.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName= obj.currSysLevelName?obj.currSysLevelName:'';
        obj.hideAccount=hideAccountAll(obj.account);
        obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.owner?obj.owner:'无')+'|'+(obj.hideAccount?obj.hideAccount:'无') ;
        obj.matchingAmtOut ='';
        obj.matchingAmtIn ='';
        obj.platAcked ='---';
        obj.bankAcked ='---';
        obj.sysBalance = '---';
        obj.cssStop = obj.status == accountStatusNormal ? '':'display:none;';
        obj.cssUsed = (obj.status == accountStatusStopTemp||obj.status == accountStatusEnabled) ? '':'display:none;';
        obj.cssFreeze = obj.status = accountStatusStopTemp ?'':'display:none;';
        idList.push({'id':obj.id});
        idList.push({'id':obj.id,type:'transAskMonitorRiskHover'});
        idArray.push(obj.id);
    });
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:data.length,count:page.totalElements,8:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance}});
    loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutMember],idArray,null,function(){ loadContentRight() });
    SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
}

function refreshContent(pageNo){
	if($("[name=flag]:checked").length==1){
		authRequest.search_EQ_flag=$("[name=flag]:checked").val();
	}else{
		authRequest.search_EQ_flag=null;
	}
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0);
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    authRequest.typeToArray= [ accountTypeReserveBank].toString();
    if(!authRequest.statusToArray){
        authRequest.statusToArray= [accountStatusNormal,accountStatusEnabled,accountStatusStopTemp,accountStatusFreeze].toString();
    }
    authRequest.currSysLevel=$("input[name='currSysLevel']:checked").val();
    authRequest.sortProperty='bankBalance';
    authRequest.sortDirection=1;
    authRequest.pageSize=$.session.get('initPageSize');
    $.ajax({ dataType:'json', type:"get",url:API.r_account_list,data:authRequest,success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data,jsonObject.page);
            showPading(jsonObject.page,"accountPage",refreshContent,null,false,true);
            loadContentRight();
            contentRight();
        }else{
            bootbox.alert(jsonObject.message);
        }
    }, error:function(result){bootbox.alert(result);},initPage:function(his){
        var $form = $('#accountFilter');
        $form.find('input[name=search_LIKE_account]').val(his.search_LIKE_account);
        $form.find('input[name=search_LIKE_alias]').val(his.search_LIKE_alias);
        $form.find('select[name=search_LIKE_bankType]').val(his.bankType);
        $form.find('input[name=search_IN_status_type]').prop('checked',false);
        $.each(his.statusToArray.split(','),function(index,obj){
            $form.find('input[name=search_IN_status_type][value='+obj+']').prop('checked',true);
        })
    } });
}

function loadContentRight(){
    contentRight({'OutwardAccountStorageReserved:CheckTransferIn:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }, 'OutwardAccountStorageReserved:TransferHisOut:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }});
}

function searchByFilter(){
	authRequest.search_EQ_handicapId =  $("[name='search_EQ_handicapId']").val();
    authRequest.search_LIKE_account = $("input[name='search_LIKE_account']").val();
    authRequest.search_LIKE_alias = $("input[name='search_LIKE_alias']").val();
    authRequest.bankType=$.trim($("select[name='search_LIKE_bankType']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    var tempStatusToArray = new Array();
    var tempTypeToArray = new Array();
    $("input[name='search_IN_status_type']:checked").each(function(){
        tempStatusToArray.push(this.value);
        tempTypeToArray.push($(this).attr("accountType"));
    });
    var statusToArray = new Array();
    var typeToArray = new Array();
    for(var index0 in tempStatusToArray){
        var item0 = tempStatusToArray[index0].split(",");
        for(var index1 in item0){
            statusToArray.push(item0[index1]);
        }
    }
    for(var index0 in tempTypeToArray){
        var item0 = tempTypeToArray[index0].split(",");
        for(var index1 in item0){
            typeToArray.push(item0[index1]);
        }
    }
    statusToArray = removeDuplicatedItem(statusToArray);
    typeToArray =  removeDuplicatedItem(typeToArray);
    authRequest.statusToArray=statusToArray.toString();
    authRequest.typeToArray=typeToArray.toString();
    refreshContent(0);
}

$("#search-button").on(ace.click_event, function() { searchByFilter();});
$('#dynamic-table').find('tbody').html('');
bootbox.setLocale("zh_CN");

getHandicap_select($("select[name='search_EQ_handicapId']"),null,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
$("#accountFilter [name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
	searchByFilter();
});
$("#accountFilter [name=search_IN_status_type],[name=currSysLevel],[name=flag]").click(function(){
	searchByFilter();
});
searchByFilter();
var showModal_reserveInfo=function(){
    var $div=$("#reserveInfoModal").modal("toggle");
    //重置参数
    resetReserveInfo();
}
var saveIncomeInfo=function(){
    bootbox.confirm("确定修改系统设置?", function(result) {
        if (result) {
            var $divReserve=$("#reserveInfoModal");
            var keysArray = new Array(), valsArray = new Array();
            var validate = new Array();
            var $RESERVE_TO_RESERVE_BALANCE = $divReserve.find("[name=RESERVE_TO_RESERVE_BALANCE]");
            var $RESERVE_TO_RESERVE_MINAMOUNT = $divReserve.find("[name=RESERVE_TO_RESERVE_MINAMOUNT]");
            validate.push({
                ele: $RESERVE_TO_RESERVE_BALANCE,
                name: '备用卡互转阀值',
                type: 'positiveInt',
                maxLength: 6,
                minEQ: 10000,
                maxEQ: 100000
            });
            validate.push({
                ele: $RESERVE_TO_RESERVE_MINAMOUNT,
                name: '备用卡互转单笔最小金额',
                type: 'positiveInt',
                maxLength: 6,
                minEQ: 3000,
                maxEQ: 50000
            });
            $.each($divReserve.find("input"), function (index, result) {
                keysArray.push($(result).attr("name"));
                valsArray.push($(result).val());
            });
            //校验
            if (!validateEmptyBatch(validate) || !validateInput(validate)) {
                return;
            }
            $.ajax({
                type: "PUT",
                dataType: 'JSON',
                url: '/r/set/update',
                async: false,
                data: {
                    "keysArray": keysArray.toString(),
                    "valsArray": valsArray.toString()
                },
                success: function (jsonObject) {
                    if (jsonObject && jsonObject.status == 1) {
                        //异步刷新系统配置全局变量
                        loadSysSetting();
                        showMessageForSuccess("保存成功");
                        $divReserve.modal("toggle");
                    } else {
                        showMessageForFail("保存失败" + jsonObject.message);
                    }
                }
            });
        }
        setTimeout(function(){
            $('body').addClass('modal-open');
        },500);
    });
}
var resetReserveInfo=function(){
    var $divReserve=$("#reserveInfoModal");
    //查询最新系统设置
    $.ajax({
        type: "POST",
        url: '/r/set/findAllToMap',
        dataType: 'JSON',
        async: false,
        success: function (res) {
            if (res.status != 1) {
                showMessageForFail('系统设置初始化失败，请稍后再试。');
                return;
            }
            //全局系统设置变量
            sysSetting = res.data;
        }
    });
    $divReserve.find("[name=RESERVE_TO_RESERVE_BALANCE]").val(sysSetting.RESERVE_TO_RESERVE_BALANCE);
    $divReserve.find("[name=RESERVE_TO_RESERVE_MINAMOUNT]").val(sysSetting.RESERVE_TO_RESERVE_MINAMOUNT);
}



