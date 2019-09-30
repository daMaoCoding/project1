currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml='<tr class="noLeftRightPadding">\
                   <td><span>{handicapName}</span></td>\
				   <td><span>{currSysLevelName}</span></td>\
				   <td><span>{alias}</span></td>\
                   <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
                   <td><span class="label label-sm {classOfStatus}">{statusName}</span></td>\
                   <td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
                   <td><span>{holderStr}</span></td>\
				   <td><span>{flagStr}</span></td>\
                   <td>'
    +'<button class="btn btn-xs btn-white btn-primary btn-bold {classHideOperate} {updateClass}" onclick="showUpdateOutAccount({id},refreshContent,false,true)"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange {updateFlag2Class}" onclick="showUpdateflag2Account({id},refreshContent)" ><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
    +'<button class="btn btn-xs btn-white btn-primary btn-bold {classHideActive}" onclick="showFreezeModal({id},refreshContent);"><i class="ace-icon fa fa-remove bigger-100 orange"></i><span>冻结</span></button>'
    +'<button class="btn btn-xs btn-white btn-primary btn-bold {classHideActive}" onclick="activeAccount({id});"><i class="ace-icon fa fa-check-square-o bigger-100 orange"></i><span>激活</span></button>'
    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange {OperatorLogBtn}" onclick="showModal_accountExtra({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showInOutListModal({id});"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>'
    +'</td>\
               </tr>';

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var  totalBalanceByBank =0 ,idList=new Array,idArray = new Array();
    $.each(data,function(idx, obj) {
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
        if(obj.status==accountStatusFreeze){
            obj.classHideOperate=" hide ";
        }
        if(obj.flag&&obj.flag*1==2){
            //返利网账号（flag=2)
            if(obj.peakBalance&&obj.peakBalance>0){
                //余额峰值(保证金)大于0 可以修改状态与限额
                obj.updateFlag2Class='';
            }else{
                obj.updateFlag2Class=' hidden ';
            }
            //修改其它信息的按钮置空
            obj.updateClass=' hidden ';
        }else{
            //其它账号
            obj.updateClass='';
            obj.updateFlag2Class=' hidden ';
        }
        if(obj.status == accountInactivated){
            obj.classHideActive = ' hidden ';
        }else{
            obj.classHideActive = '';
        }
        obj.flagStr=getFlagStr(obj.flag);
        obj.OperatorLogBtn=OperatorLogBtn;
        obj.classOfStatus=(obj.status == accountStatusFreeze ||obj.status == accountStatusStopTemp)?'label-danger':(obj.status == accountStatusEnabled?'label-warning':'label-success');
        obj.DailyAmount =htmlDailyAmount(1,obj.limitOut,obj.outwardAmountDaily);
        obj.balance = obj.balance?obj.balance:'0';
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.bankName = obj.bankName?obj.bankName:'';
        obj.bankType = obj.bankType?obj.bankType:'';
        obj.bankName = obj.type==accountTypeOutThird?obj.bankName:(obj.bankType?obj.bankType:obj.bankName);
        obj.owner=obj.owner?obj.owner:'';
        obj.alias =$.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName =obj.currSysLevelName?obj.currSysLevelName:'';
        obj.handicapName =obj.handicapName?obj.handicapName:'';
        obj.isThird = obj.type==accountTypeOutThird;
        obj.hideAccount=hideAccountAll(obj.account);
        obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.owner?obj.owner:'无')+'|'+(obj.hideAccount?obj.hideAccount:'无') ;
        idList.push({'id':obj.id,'type':(obj.type==accountTypeOutThird||obj.type==accountTypeInThird?'third':'Bank')});
        idArray.push(obj.id);
    });
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:data.length,count:page.totalElements,6:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance}});
    SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
}

function refreshContent(pageNo){
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:$("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0;
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    if(!authRequest.typeToArray){
        authRequest.typeToArray= [accountTypeOutBank,accountTypeOutThird,accountTypeReserveBank,accountTypeCashBank].toString();
    }
    if(!authRequest.statusToArray){
        authRequest.statusToArray= [accountStatusNormal,accountStatusStopTemp,accountStatusEnabled ].toString();
    }
    authRequest.sortProperty='status';
    authRequest.sortDirection=0;
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.search_IN_handicapId = $("select[name='search_EQ_handicapId']").val().toString();
    authRequest.currSysLevel=$("input[name='currSysLevel']:checked").val();
    authRequest.search_IN_flag = '2';
    authRequest.search_EQ_flag=$("input[name='flag']:checked").val();
    $.ajax({ dataType:'json',type:"get", url:API.r_account_list,data:authRequest,success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data,jsonObject.page);
            showPading(jsonObject.page,"accountPage",refreshContent,null,false,false);
            contentRight();
        }else{
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){  bootbox.alert(result); }});
}

function searchByFilter(){
    authRequest.operator = $("input[name='search_LIKE_operator']").val();
    authRequest.search_LIKE_alias = $("input[name='search_LIKE_alias']").val();
    authRequest.search_LIKE_account = $("input[name='search_LIKE_account']").val();
    authRequest.bankType=$.trim($("select[name='search_LIKE_bankType']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    var tempStatusToArray = new Array();
    var tempTypeToArray = new Array();
    $("input[name='search_IN_status_type']:checked").each(function(){
        if(this.value){
            tempStatusToArray.push(this.value);
        }
        if($(this).attr("accountType")){
            tempTypeToArray.push($(this).attr("accountType"));
        }
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
function changeStatus(status) {
    if(status==6){
        status
    }
    refreshContent(0);
}

function activeAccount(accountId){
    var accountInfo = getAccountInfoById(accountId);
    authRequest.accountId = accountId;
    //校验通过后请求服务端生成一条测试任务
    if(_checkAccountToActive(accountInfo)){
        bootbox.confirm("请先确保账户（"+accountInfo.bankType+"|"+ accountInfo.owner+"|"+ accountInfo.account.substr(accountInfo.account.length-4)+"）处于正常连接状态，确定向此账户推送一笔测试任务？", function(result) {
            if (result) {
                $.ajax({ dataType:'json',type:"get", url:'/r/account/getActiveTrans',data:authRequest,success:function(jsonObject){
                    if(jsonObject.status == 1){
                        if(jsonObject.data){
                            var message = "账户（"+accountInfo.bankType+"|"+ accountInfo.owner+"|"+ accountInfo.account.substr(accountInfo.account.length-4)+"）将向"+"<br>"+"账户（"+
                                jsonObject.data.bankType+"|"+ jsonObject.data.owner+"|"+ jsonObject.data.account.substr(jsonObject.data.account.length-4)+"）转一笔账"
                            showMessageForSuccess(message,2000);
                        }else{
                            showMessageForSuccess("未获取到测试转账任务！",2000);
                        }
                    }
                },error:function(result){  bootbox.alert(result); }});
            }
        });
    }
}
function _checkAccountToActive(accountInfo){
    var type = accountInfo.type;
    var message = "";
    if(accountInfo.flag && accountInfo.flag ==1 && !accountInfo.currSysLevel){
        message = message +"请先完善账号层级信息！";
        showMessageForFail(message,3000);
        return false;
    }
    return true;
}

bootbox.setLocale("zh_CN");

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
$("#accountFilter [name=currSysLevel],[name=search_IN_status_type],[name=flag]").click(function(){
    searchByFilter();
});
getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
searchByFilter();