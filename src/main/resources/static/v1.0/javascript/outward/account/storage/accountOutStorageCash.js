currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml='<tr>\
            <td><span>{currSysLevelName}</span></td>\
			<td><span>{alias}</span></td>\
            <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
            <td style="width:75px;"><span class="label label-sm {classOfStatus}">{statusName}</span></td>\
            <td style="width:75px;"><span>{bankBalance}{limitBalanceIcon}</span></td>\
		    <td style="width:75px;"><span>{incomeAmountDaily}</span>{htmlExceedInLimit}</td>\
		    <td style="width:75px;"><span>{outwardAmountDaily}</span>{htmlExceedOutLimit}</td>\
	            <td style="width:90px;">\
                <a {inCountMappingEle} target="_self" class="contentRight" contentRight="OutwardAccountStorageCash:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
                <a {inCountMappedEle} target="_self"  class="contentRight" contentRight="OutwardAccountStorageCash:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
                <a {inCountCancelEle} target="_self"  class="contentRight" contentRight="OutwardAccountStorageCash:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
                </td>\
            <td style="width:180px;">'
				+'<button class="btn btn-xs btn-white btn-primary btn-bold " onclick="showFreezeModal({id},refreshContent);" ><i class="ace-icon fa fa-remove bigger-100 orange"></i><span>冻结</span></button>'
				+'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" onclick="updAccount({id},refreshContent)" contentRight="OutwardAccountStorageCash:Update:*"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
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
    showAddOutAccount(accountTypeCashBank,accountInactivated ,function(){
        refreshContent();
    });
}

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var totalBalanceByBank =0  ,totalDailyOutward=0,totalDailyIncome=0,idList=new Array,idArray= new Array();
    $.each(data,function(idx, obj) {
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
        totalDailyOutward = totalDailyOutward +obj.outwardAmountDaily;
        totalDailyIncome = totalDailyIncome + obj.incomeAmountDaily;
        obj.OperatorLogBtn=OperatorLogBtn;
        obj.classOfStatus=( obj.status ==accountStatusFreeze)?'label-danger':'label-success';
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
        obj.inCountMappedEle=(obj.inCount.mapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.inCountMappingEle=(obj.inCount.mapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.inCountCancelEle=(obj.inCount.cancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outwardAmountDaily=obj.outwardAmountDaily+'';
        obj.incomeAmountDaily= obj.incomeAmountDaily+'';
        obj.balance = obj.balance+'';
        obj.limitBalanceIcon=getlimitBalanceIconStr(obj);
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.accountStatInOutCategoryIn = accountStatInOutCategoryIn+'';
        obj.alias= $.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName=obj.currSysLevelName?obj.currSysLevelName:'';
        obj.hideAccount=hideAccountAll(obj.account);
        obj.accountInfo =obj.bankType+'|'+obj.owner+'|'+obj.hideAccount ;
        idList.push({'id':obj.id});
        idArray.push(obj.id);
    });
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:data.length,count:page.totalElements, 5:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance},6:{subTotal:totalDailyIncome,total:page.header.totalAmountIncomeDaily}, 7:{subTotal:totalDailyOutward,total:page.header.totalAmountOutwardDaily}});
    loadEncashCheckAndStatus([accountStatInOutCategoryIn],idArray,null,function(){ loadContentRight() });
}

function refreshContent(pageNo){
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0);
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    if(!authRequest.typeToArray){
        authRequest.typeToArray= [ accountTypeCashBank ].toString();
    }
    if(!authRequest.statusToArray){
        authRequest.statusToArray= [ accountStatusNormal].toString();
    }
    authRequest.sortProperty='status';
    authRequest.sortDirection=0;
    authRequest.pageSize=$.session.get('initPageSize');
    $.ajax({ dataType:'json', type:"get",url:API.r_account_list,data:authRequest,success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data,jsonObject.page);
            showPading(jsonObject.page,"accountPage",refreshContent,null,false,true);
            loadContentRight();
        }else{
            bootbox.alert(jsonObject.message);
        }
    }, error:function(result){bootbox.alert(result);},initPage:function(his){
        var $form = $('#accountFilter');
        $form.find('input[name=search_LIKE_account]').val(his.search_LIKE_account);
        $form.find('input[name=search_LIKE_alias]').val(his.search_LIKE_alias);
        $form.find('input[name=search_LIKE_bankType]').val(his.bankType);
        $form.find('input[name=search_IN_status_type]').prop('checked',false);
        $.each(his.statusToArray.split(','),function(index,obj){
            $form.find('input[name=search_IN_status_type][value='+obj+']').prop('checked',true);
        })
    } });
}
function loadContentRight(){
    contentRight({'OutwardAccountStorageCash:CheckTransferIn:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }});
}

function searchByFilter(){
    authRequest.search_LIKE_account = $("input[name='search_LIKE_account']").val();
    authRequest.search_LIKE_alias = $("input[name='search_LIKE_alias']").val();
    authRequest.bankType=$.trim($("input[name='search_LIKE_bankType']").val());
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

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});

$("#accountFilter [name=search_IN_status_type]").click(function(){
	searchByFilter();
});

bootbox.setLocale("zh_CN");
searchByFilter();


