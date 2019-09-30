currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml='<tr class="noLeftRightPadding">\
				<td><span>{seqNo}</span></td>\
                <td><span>{handicapName}</span></td>\
				<td><span>{currSysLevelName}</span></td>\
				<td><span>{typeStr}</span></td>\
				<td><span>{alias}</span></td>\
                <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
				<td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
				<td><span>{alarmType}</span></td>\
                <td>\
                    <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span></a>\
                </td>\
                <td>'
					+'<button class="{classAlarm4Bal}"  onclick="clearAlarm4Bal({id})"><i class="ace-icon fa fa-bolt bigger-100 orange"></i><span>清除告警</span></button>'
	                +'<a href="#/BizAccountAlarmDetail:*?aliasLike={alias}&transIn0Out1=1&accountId={id}" type="button" class="{classAlarm4Flow}"><i class="ace-icon fa fa-bolt  bigger-100 orange"></i>告警处理</a>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>\
                </td>\
           </tr>';

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var idList=new Array,idArray= new Array();
	if(!data|| data.length ==0){
		var html = '<div class="noDataTipsPage table table-striped table-bordered table-hover no-margin-bottom no-border-top">'+
		'<div style="margin-bottom:0px;font-size: 20px;" class="alert alert-success center">无数据</div>'+
		'</div>';
		$('#accountPage').html(html);
		return;
	}else{
		$('#accountPage').html('<span class="label label-info" name="footTitle" style="width:100%;height:30px;"> 以上账号含有未自动匹配的流水，可能是重复出款或恶意转出，请速处理</span>')
	}
	var $filter = $('#accountAlarmFilter');
	var handicapId = $filter.find("select[name='search_EQ_handicapId']").val();
	var accType = $filter.find("select[name='accType']").val();
	var aliasLike = $filter.find("input[name='aliasLike']").val();
	var tmp = [],seqNo=1;
    $.each(data,function(idx, obj) {
		var valid = true;
		if(handicapId && obj.handicapId && obj.handicapId !=handicapId)
			valid = false;
		if(accType && obj.type && obj.type != accType)
			valid = false;
		if(aliasLike && obj.alias && obj.alias.indexOf(aliasLike)<0)
			valid = false;
		if(valid){
			obj.seqNo = seqNo;
			seqNo = seqNo+1;
			obj.alarmType = (!obj.mappingAmount||!obj.mappedAmount)?'余额告警':'流水告警';
			obj.classAlarm4Bal =  (!obj.mappingAmount||!obj.mappedAmount)?'btn btn-xs btn-white btn-primary btn-bold orange':'dsn';
			obj.classAlarm4Flow =  (!obj.mappingAmount||!obj.mappedAmount)?'dsn':'btn btn-xs btn-white btn-primary btn-bold';
			obj.matchingAmtOut = obj.mappingAmount + '&nbsp;元&nbsp;&nbsp;/&nbsp;&nbsp;'+obj.mappedAmount+'&nbsp;条&nbsp;';
			obj.recycleBntClass =   obj.transBlackTo?'btn-primary':'btn-warning';
			obj.recycleIconClass =  obj.transBlackTo?'blue':'red';
			obj.transBlackTo = obj.transBlackTo?obj.transBlackTo:'0';
			obj.DailyAmount =htmlDailyAmount(1,obj.limitOut,obj.outwardAmountDaily);
			obj.outwardAmountDaily = obj.outwardAmountDaily+'';
			obj.balance = obj.balance+'';
			obj.typeStr = obj.typeStr+'';
			obj.limitBalanceIcon=getlimitBalanceIconStr(obj);
			obj.alias =$.trim(obj.alias)?obj.alias:'';
			obj.currSysLevelName =obj.currSysLevelName?obj.currSysLevelName:'';
			obj.handicapName =obj.handicapName?obj.handicapName:'';
			obj.hideAccount=hideAccountAll(obj.account);
			obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.owner?hideName(obj.owner):'无')+'</br>'+(obj.hideAccount?obj.hideAccount:"无" );
			obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
			tmp.push(obj);
			idList.push({'id':obj.id});
			idArray.push(obj.id);
		}
    });
	if(!tmp|| tmp.length ==0){
		var html = '<div class="noDataTipsPage table table-striped table-bordered table-hover no-margin-bottom no-border-top">'+
			'<div style="margin-bottom:0px;font-size: 20px;" class="alert alert-success center">无数据</div>'+
			'</div>';
		$('#accountPage').html(html);
		return;
	}else{
		$('#accountPage').html('<span class="label label-info" name="footTitle" style="width:100%;height:30px;"> 以上账号含有未自动匹配的流水，可能是重复出款或恶意转出，请速处理</span>')
	}
    $tbody.html(fillDataToModel4Array(tmp,trHtml));
    loadHover_accountInfoHover(idList);
    SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
}

function refreshContent(){
    $.ajax({ dataType:'json', type:"get",url:API.r_accountMonitor_listAccForAlarm, success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data,jsonObject.page);
        }else {
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){ bootbox.alert(result);},initPage:function(his){}});
}

function loadContentRight(){
    contentRight({'OutwardAccountBankUsed:CheckTransferIn:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }, 'OutwardAccountBankUsed:CheckTransferOut:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }});
}

function searchByFilter(){
    authRequest.operator = $("input[name='search_LIKE_operator']").val();
    authRequest.search_LIKE_alias = $("input[name='search_LIKE_alias']").val();
    authRequest.search_LIKE_account = $("input[name='search_LIKE_account']").val();
    authRequest.bankType=$.trim($("input[name='search_LIKE_bankType']").val());
    refreshContent(0);
}
bootbox.setLocale("zh_CN");

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});
var showModal_outwardInfo=function(){
	var $div=$("#outwardInfoModal").modal("toggle");
	//重置参数
	resetOutwardInfo();
}
var resetOutwardInfo=function(){
	var $divOutdraw=$("#outwardInfoModal");
	getHandicap_select($divOutdraw.find("select[name='handicap_sync']"),null,"--------请选择--------");
	//如何充值bootstrap的radio待研究
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
    $divOutdraw.find("[name=OUTDRAW_LIMIT_CHECKOUT_TODAY]").val(sysSetting.OUTDRAW_LIMIT_CHECKOUT_TODAY);
    $divOutdraw.find("[name=OUTDRAW_SYSMONEYLIMIT]").val(sysSetting.OUTDRAW_SYSMONEYLIMIT);
    $divOutdraw.find("[name=OUTDRAW_SYSMONEY_LOWEST]").val(sysSetting.OUTDRAW_SYSMONEY_LOWEST);
    $divOutdraw.find("[name=OUTDRAW_LIMIT_APPROVE]").val(sysSetting.OUTDRAW_LIMIT_APPROVE);
    $divOutdraw.find("[name=OUTDRAW_LIMIT_APPROVE_MANAGE]").val(sysSetting.OUTDRAW_LIMIT_APPROVE_MANAGE);
    $divOutdraw.find("[name=OUTDRAW_CHECK_CODE]").val(sysSetting.OUTDRAW_CHECK_CODE);
    $divOutdraw.find("[name=OUTDRAW_SPLIT_AMOUNT_OUTSIDE]").val(sysSetting.OUTDRAW_SPLIT_AMOUNT_OUTSIDE);
    $divOutdraw.find("[name=OUTDRAW_SPLIT_AMOUNT_INSIDE]").val(sysSetting.OUTDRAW_SPLIT_AMOUNT_INSIDE);
    $divOutdraw.find("[name=OUTDRAW_THIRD_LOWEST_BAL]").val(sysSetting.OUTDRAW_THIRD_LOWEST_BAL);
}
var saveOutwardInfo=function(){
	bootbox.confirm("确定修改系统设置?", function(result) {
		if (result) {
			var $divOutdraw=$("#outwardInfoModal");
		    var keysArray = new Array(), valsArray = new Array();
		    var validate = new Array();
		    var $OUTDRAW_LIMIT_CHECKOUT_TODAY = $divOutdraw.find("[name=OUTDRAW_LIMIT_CHECKOUT_TODAY]");
		    var $OUTDRAW_SYSMONEYLIMIT = $divOutdraw.find("[name=OUTDRAW_SYSMONEYLIMIT]");
		    var $OUTDRAW_SYSMONEY_LOWEST = $divOutdraw.find("[name=OUTDRAW_SYSMONEY_LOWEST]");
		    var $OUTDRAW_LIMIT_APPROVE = $divOutdraw.find("[name=OUTDRAW_LIMIT_APPROVE]");
		    var $OUTDRAW_LIMIT_APPROVE_MANAGE = $divOutdraw.find("[name=OUTDRAW_LIMIT_APPROVE_MANAGE]");
		    var $OUTDRAW_CHECK_CODE = $divOutdraw.find("[name=OUTDRAW_CHECK_CODE]");
		    var $OUTDRAW_SPLIT_AMOUNT_OUTSIDE = $divOutdraw.find("[name=OUTDRAW_SPLIT_AMOUNT_OUTSIDE]");
		    var $OUTDRAW_SPLIT_AMOUNT_INSIDE = $divOutdraw.find("[name=OUTDRAW_SPLIT_AMOUNT_INSIDE]");
		    var $OUTDRAW_THIRD_LOWEST_BAL = $divOutdraw.find("[name=OUTDRAW_THIRD_LOWEST_BAL]");
		    //转主管审核金额不能小于人工审核配置金额
		    if ($.trim($OUTDRAW_LIMIT_APPROVE_MANAGE.val()) * 1 < $.trim($OUTDRAW_LIMIT_APPROVE.val()) * 1) {
		        showMessageForCheck("保存失败,“转主管审核金额” 不可小于 “人工审核配置金额”");
		        return;
		    }
		    validate.push({
		        ele: $OUTDRAW_LIMIT_CHECKOUT_TODAY,
		        name: '当日出款最大限额',
		        type: 'amountPlus',
		        minEQ: 10000,
		        maxEQ: 1000000
		    });
		    validate.push({ele: $OUTDRAW_SYSMONEYLIMIT, name: '机器自动出款最大限额', type: 'amountPlus', minEQ: 1000, maxEQ: 50000});
		    validate.push({ele: $OUTDRAW_SYSMONEY_LOWEST, name: '出款卡最低余额', type: 'amountPlus', minEQ: 1000, maxEQ: 10000});
		    validate.push({
		        ele: $OUTDRAW_LIMIT_APPROVE,
		        name: '大额出款，超过此设置将人工审核',
		        type: 'amountPlus',
		        minEQ: 10000,
		        maxEQ: 100000
		    });
		    validate.push({
		        ele: $OUTDRAW_LIMIT_APPROVE_MANAGE,
		        name: '大额出款，超过此设置将转主管审核',
		        type: 'amountPlus',
		        minEQ: 50000,
		        maxEQ: 5000000
		    });
		    validate.push({ele: $OUTDRAW_CHECK_CODE, name: '出款审核几倍打码量限制', type: 'positiveInt', min: 0, max: 10});
		    validate.push({
		        ele: $OUTDRAW_SPLIT_AMOUNT_OUTSIDE,
		        name: '外层拆单金额',
		        type: 'amountPlus',
		        minEQ: 20000,
		        maxEQ: 50000
		    });
		    validate.push({
		        ele: $OUTDRAW_SPLIT_AMOUNT_INSIDE,
		        name: '内层拆单金额',
		        type: 'amountPlus',
		        minEQ: 20000,
		        maxEQ: 50000
		    });
		    validate.push({
		        ele: $OUTDRAW_THIRD_LOWEST_BAL,
		        name: '第三方出款最低金额',
		        type: 'amountPlus',
		        minEQ: 10000,
		        maxEQ: 9999999
		    });
		    $.each($divOutdraw.find("input"), function (index, result) {
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
		                $divOutdraw.modal("toggle");
		            } else {
		                showMessageForFail("保存失败" + jsonObject.message);
		            }
		        }
		    });
		}
	});
};

var clearAlarm4Bal = function(accId){
	bootbox.confirm("确定清除该账号的余额告警信息&nbsp;?&nbsp;", function(result) {
		if (result) {
			$.ajax({type: 'get',url: '/r/accountMonitor/clearAccAlarm4Risk',data: {"id":accId},dataType: 'json',success: function (res) {
				if (res.status==1) {
					showMessageForSuccess('操作成功');
				}
				refreshContent();
			}});
		}
	});
};

var saveSyncInfo=function(){
	var $div=$("#outwardInfoModal");
	var handicap_sync = $div.find('[name=handicap_sync] option:selected').attr("code");
	if(!validateEmptyBatch([{ele:$div.find('[name=handicap_sync]'),name:'盘口'}])){
    	return;
    }
	bootbox.confirm("确定同步出款订单?", function(result) {
		if (result) {
			 $.ajax({
	            type: 'get',
	            url: '/r/synch/synchInfo',
	            data: {"type": 2, "handicap": handicap_sync},
	            dataType: 'json',
	            success: function (res) {
	                if (res) {
	                    $.gritter.add({
	                        time: '',
	                        class_name: '',
	                        title: '系统消息',
	                        text: res.message,
	                        sticky: false,
	                        image: '../images/message.png'
	                    });
	                    $('input[name="sync_type"]:checked').prop('checked', '');
	                    $div.modal("toggle");
	                }
	            }
	        });
		}
	});
}
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
$("#accountFilter [name=search_EQ_handicapId]").change(function(){
	refreshContent();
});
$("#accountFilter [name=holderType],[name=currSysLevel]").click(function(){
	refreshContent();
});
initRefreshSelect($("#refreshAccountAlarmSelect"),$("#searchAccountAlarmBtn"),null,"refresh_account_alarm");

loadHandicap_Level($("select[name='search_EQ_handicapId']"),null,$("select[name='search_EQ_id']"),null);
refreshContent(0);
