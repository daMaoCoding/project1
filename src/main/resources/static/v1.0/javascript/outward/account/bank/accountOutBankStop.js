currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml='<tr class="noLeftRightPadding">\
                <td><span>{handicapName}</span></td>\
				<td><span>{currSysLevelName}</span></td>\
				<td><span>{alias}</span><br/>{statusHTMl}</td>\
                <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
    			<td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
                <td><div class="SysLogEvent" target="{id}"><span class="amount">{balance}</span></div></td>\
                <td>\
                    <a {outCountMappingEle} target="_self"  class="contentRight" contentRight="OutwardAccountBankStop:CheckTransferOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
                    <a {outCountMappedEle} target="_self"   class="contentRight" contentRight="OutwardAccountBankStop:CheckTransferOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
                    <a {outCountCancelEle} target="_self"   class="contentRight" contentRight="OutwardAccountBankStop:CheckTransferOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-danger" title="待排查">{outCountCancel}</span></a>\
                    <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
                </td>\
			    <td>\
				    <a {inCountMappingEle} target="_self" class="contentRight" contentRight="OutwardAccountBankStop:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
				    <a {inCountMappedEle} target="_self"  class="contentRight" contentRight="OutwardAccountBankStop:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
				    <a {inCountCancelEle} target="_self"  class="contentRight" contentRight="OutwardAccountBankStop:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
				    <span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId={id}>{matchingAmtIn}</span>\
				</td>\
                <td><span>{holderStr}</span></td>\
				<td><span>{flagStr}</span></td>\
                <td>'
					+'<button class="btn btn-xs btn-white btn-warning btn-bold orange {updateStatusClass}" onclick="showEnabledModal({id},refreshContent)"><i class="ace-icon fa fa-exchange bigger-100 red"></i><span>转可用</span></button>'
					+'<button class="btn btn-xs btn-white btn-warning btn-bold orange " onclick="showFreezeModal({id},refreshContent)" ><i class="ace-icon fa fa-remove bigger-100 red"></i><span>冻结</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange contentRight {updateClass}" onclick="showUpdateOutAccount({id},refreshContent)" contentRight="OutwardAccountBankStop:Update:*"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange contentRight {updateFlag2Class}" onclick="showUpdateflag2Account({id},refreshContent)" contentRight="OutwardAccountBankStop:Update:*"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改限额</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange {OperatorLogBtn}" onclick="showModal_accountExtra({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>\
                </td>\
           </tr>';

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var  totalBalanceBySys=0,totalBalanceByBank =0 ,idList=new Array,idArray= new Array();
    $.each(data,function(idx, obj) {
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
    	totalBalanceBySys = totalBalanceBySys + obj.balance ;
        obj.statusHTMl=getDeviceStatusInfoHoverHTML(obj);
        if(obj.flag&&obj.flag*1==2){
        	//返利网账号（flag=2)
        	if(obj.peakBalance&&obj.peakBalance>0){
        		//余额峰值(保证金)大于0 可以修改状态与限额
        		obj.updateStatusClass='';
            	obj.updateFlag2Class='';
        	}else{
        		obj.updateStatusClass=' hidden ';
            	obj.updateFlag2Class=' hidden ';
        	}
        	//修改其它信息的按钮置空
        	obj.updateClass=' hidden ';
        }else{
        	//其它账号
        	obj.updateClass='';
        	obj.updateStatusClass='';
        	obj.updateFlag2Class=' hidden ';
        }
        obj.flagStr=getFlagStr(obj.flag);
        obj.OperatorLogBtn=OperatorLogBtn;
        obj.DailyAmount =htmlDailyAmount(1,obj.limitOut,obj.outwardAmountDaily);
        obj.incomeRequestStatusMatching=incomeRequestStatusMatching+'';
        obj.incomeRequestStatusMatched=incomeRequestStatusMatched+'';
        obj.incomeRequestStatusCanceled=incomeRequestStatusCanceled+'';
        obj.outwardTaskStatusDeposited =outwardTaskStatusDeposited+'';
        obj.outwardTaskStatusMatched=outwardTaskStatusMatched+'';
        obj.outwardTaskStatusFailure=outwardTaskStatusFailure+'';
        obj.inCountMapped=!!!obj.inCount.mapped?"0":obj.inCount.mapped;
        obj.inCountMapping=!!!obj.inCount.mapping?"0":obj.inCount.mapping;
        obj.inCountCancel=!!!obj.inCount.cancel?"0":obj.inCount.cancel;
        obj.outCountMapped=!!!obj.outCount.mapped?"0":obj.outCount.mapped;
        obj.outCountMapping=!!!obj.outCount.mapping?"0":obj.outCount.mapping;
        obj.outCountCancel=!!!obj.outCount.cancel?"0":obj.outCount.cancel;
        obj.inCountMappedEle=(obj.inCount.mapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.inCountMappingEle=(obj.inCount.mapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.inCountCancelEle=(obj.inCount.cancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountMappedEle=(obj.outCount.mapped!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+obj.id+'&outwardTaskStatus='+outwardTaskStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountMappingEle=(obj.outCount.mapping!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+obj.id+'&outwardTaskStatus='+outwardTaskStatusDeposited+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountCancelEle=(obj.outCount.cancel!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+obj.id+'&outwardTaskStatus='+outwardTaskStatusFailure+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outwardAmountDaily = obj.outwardAmountDaily +'';
        obj.balance = obj.balance?obj.balance:'0';
        obj.limitBalanceIcon=getlimitBalanceIconStr(obj);
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.accountStatInOutCategoryIn = accountStatInOutCategoryIn+'';
        obj.accountStatInOutCategoryOutMember=accountStatInOutCategoryOutMember+'';
        obj.alias =$.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName =obj.currSysLevelName?obj.currSysLevelName:'';
        obj.handicapName =obj.handicapName?obj.handicapName:'';
        obj.hideAccount=hideAccountAll(obj.account);
        obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.owner?obj.owner:'无')+'</br>'+(obj.hideAccount?obj.hideAccount:"无" );
        obj.matchingAmtOut ='';
        obj.matchingAmtIn ='';
        obj.sysBalance = '---';
        idList.push({'id':obj.id});
        idList.push({'id':obj.id,type:'transAskMonitorRiskHover'});
        idArray.push(obj.id);
    });
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
	loadHover_accountStopReasonHover(idArray);
    //统计总卡数量
	if(page&&page.header&&page.header.IdSize){
		var IdSize=page.header.IdSize;
		$("#totalIdSize").text(IdSize[0]);
		$("#onlineSize").text(IdSize[1]);
		$("#stopSize").text(IdSize[2]);
		$("#offlineSize").text(IdSize[3]);
	}
    showSubAndTotalStatistics4Table($tbody,{column:13, subCount:data.length,count:page.totalElements,5:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance},6:{subTotal:totalBalanceBySys,total:page.header.totalAmountBalance}});
    loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutMember],idArray,null,function(){ loadContentRight() });
    SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
}

function loadContentRight(){
    contentRight({'OutwardAccountBankStop:CheckTransferIn:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }, 'OutwardAccountBankStop:CheckTransferOut:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }});
}

function refreshContent(pageNo){
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0);
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    authRequest.typeToArray= [accountTypeOutBank].toString();
    authRequest.statusToArray= [accountStatusStopTemp].toString();
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.search_IN_handicapId = $("select[name='search_EQ_handicapId']").val().toString();
    authRequest.deviceStatus=$("#deviceStatus").val();
    authRequest.currSysLevel=$("input[name='currSysLevel']:checked").val();
    authRequest.search_EQ_flag=$("input[name='flag']:checked").val();
    $.ajax({ dataType:'json', type:"get",url:API.r_account_list,data:authRequest, success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data,jsonObject.page);
            showPading(jsonObject.page,"accountPage",refreshContent,null,false,true);
            loadContentRight();
        }else {
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){ bootbox.alert(result);},initPage:function(his){
        var $form = $('#accountFilter');
        $form.find('input[name=search_LIKE_account]').val(his.search_LIKE_account);
        $form.find('input[name=search_LIKE_alias]').val(his.search_LIKE_alias);
        $form.find('input[name=search_LIKE_operator]').val(his.operator);
        $form.find('select[name=search_LIKE_bankType]').val(his.bankType);
    }});
}

function searchByFilter(){
    authRequest.operator = $("input[name='search_LIKE_operator']").val();
    authRequest.search_LIKE_alias = $("input[name='search_LIKE_alias']").val();
    authRequest.search_LIKE_account = $("input[name='search_LIKE_account']").val();
    authRequest.bankType=$.trim($("select[name='search_LIKE_bankType']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    refreshContent(0);
}

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});

$("#bankType_multiselect").html(options.join('')).multiselect({
    enableFiltering: true,enableHTML: true,nonSelectedText :'----全部----',nSelectedText :'已选中',buttonClass: 'btn btn-white btn-primary',buttonWidth: '160px',
    templates: {
        button: '<button type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>',
        ul: '<ul class="multiselect-container dropdown-menu"></ul>',
        filter: '<li class="multiselect-item filter"><div class="input-group"><span class="input-group-addon"><i class="fa fa-search"></i></span><input class="form-control multiselect-search" type="text"></div></li>',
        filterClearBtn: '<span class="input-group-btn"><button class="btn btn-default btn-white btn-grey multiselect-clear-filter" type="button"><i class="fa fa-times-circle red2"></i></button></span>',
        li: '<li><a tabindex="0"><label></label></a></li>',
        divider: '<li class="multiselect-item divider"></li>',
        liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
    }
});
$("#accountFilter [name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
    searchByFilter();
});
$("#accountFilter [name=currSysLevel],[name=flag]").click(function(){
    searchByFilter();
});
var changeDevice=function(deviceStaus){
	$("#deviceStatus").val(deviceStaus);
	searchByFilter(0);
}
getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
refreshContent(0);

