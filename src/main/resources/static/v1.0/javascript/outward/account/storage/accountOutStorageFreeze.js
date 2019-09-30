currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml ='<tr>\
                <td><span>{handicapName}</span></td>\
				<td><span>{currSysLevelName}</span></td>\
				<td><span>{alias}</span></td>\
                <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
                <td style="width:70px;"><span>{typeName}</span></td>\
                <td style="width:70px;"><span class="label label-sm {classOfStatus}">{statusName}</span></td>\
                <td style="width:100px;"><span>{bankBalance}{limitBalanceIcon}</span></td>\
    			<td style="width:100px;"><span>{outwardAmountDaily}</span>{htmlExceedOutLimit}</td>\
                <td style="width:100px;"><span>{holderStr}</span></td>\
				<td><span>{flagStr}</span></td>\
				<td style="width:100px;"><span>{updateTimeStr}</span></td>\
                <td style="width:180px;">'
					+'<button class="btn btn-xs btn-white btn-primary btn-bold orange {OperatorLogBtn}" onclick="showModal_accountExtra({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
					+'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showInOutListModal({id});"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>'
				+'</td>\
         </tr>';

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var totalBalanceByBank =0 ,totalDailyOutward=0,idList=new Array;
    $.each(data,function(idx, obj) {
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
        totalDailyOutward = totalDailyOutward + obj.outwardAmountDaily;
        obj.flagStr=getFlagStr(obj.flag);
        obj.OperatorLogBtn=OperatorLogBtn;
        obj.handicapName=obj.handicapName?obj.handicapName:'';
        obj.classOfStatus=(obj.status ==accountStatusFreeze)?'label-danger':'label-warning';
        obj.limitOut=(!!!obj.limitOut)?eval(sysSetting.OUTDRAW_LIMIT_CHECKOUT_TODAY):obj.limitOut;
        obj.htmlExceedOutLimit =(obj.outwardAmountDaily- obj.limitOut )> 0 ? "<i class=\"fa fa-flag red bigger-130\"></i>":"";
        obj.outwardAmountDaily=obj.outwardAmountDaily+'';
        obj.limitBalanceIcon=getlimitBalanceIconStr(obj);
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.alias= $.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName= obj.currSysLevelName?obj.currSysLevelName:'';
        obj.hideAccount=hideAccountAll(obj.account);
        obj.holderStr =obj.holderStr?obj.holderStr:'';
        obj.updateTimeStr =obj.updateTimeStr?obj.updateTimeStr:'';
        obj.accountInfo =obj.bankType+'|'+obj.owner+'|'+obj.hideAccount ;
        idList.push({'id':obj.id});
        obj.accountInfo =obj.bankType+'|'+obj.owner+'|'+obj.hideAccount ;
    });
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:data.length,count:page.totalElements,7:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance}, 7:{subTotal:totalDailyOutward,total:page.header.totalAmountOutwardDaily}});
}

function refreshContent(pageNo){
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0);
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    if(!authRequest.typeToArray){
        authRequest.typeToArray= [accountTypeReserveBank, accountTypeCashBank ].toString();
    }
    if(!authRequest.statusToArray){
        authRequest.statusToArray= [accountStatusFreeze].toString();
    }
    authRequest.search_EQ_flag=$("input[name='flag']:checked").val();
    authRequest.pageSize=$.session.get('initPageSize');
    $.ajax({dataType:'json',type:"get",url:API.r_account_list, data:authRequest,success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data,jsonObject.page);
            showPading(jsonObject.page,"accountPage",refreshContent,null,false,false);
            contentRight();
        }else{
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){ bootbox.alert(result); }});
}

function searchByFilter(){
    authRequest.operator = $("input[name='search_LIKE_operator']").val();
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

bootbox.setLocale("zh_CN");
$("input[name='search_IN_status_type']").attr("checked",true);

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});
$("#accountFilter [name=search_LIKE_bankType]").change(function(){
    searchByFilter();
});
$("#accountFilter [name=search_IN_status_type],[name=flag]").click(function(){
	refreshContent();
});

refreshContent(0);

getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");


