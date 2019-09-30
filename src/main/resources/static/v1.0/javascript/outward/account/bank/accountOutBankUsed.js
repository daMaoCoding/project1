currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml='<tr class="noLeftRightPadding">\
				<td><input type="checkbox" name="checkboxAccId" value="{id}"/></td>\
                <td><span>{handicapName}</span></td>\
				<td><span>{currSysLevelName}</span></td>\
				<td><span>{alias}</span><br/>{statusHTMl}</td>\
                <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
				<td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
				<td><div class="SysLogEvent" target="{id}"><span class="amount">{balance}</span></div></td>\
                <td>\
                    <a {outCountMappingEle} target="_self" class="contentRight" contentRight="OutwardAccountBankUsed:CheckTransferOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
                    <a {outCountMappedEle}  target="_self" class="contentRight" contentRight="OutwardAccountBankUsed:CheckTransferOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
                    <a {outCountCancelEle}  target="_self" class="contentRight" contentRight="OutwardAccountBankUsed:CheckTransferOut:*" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-danger" title="待排查">{outCountCancel}</span></a>\
                    <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
                </td>\
			    <td>\
				    <a {inCountMappingEle} target="_self" class="contentRight" contentRight="OutwardAccountBankUsed:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
				    <a {inCountMappedEle}  target="_self" class="contentRight" contentRight="OutwardAccountBankUsed:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
				    <a {inCountCancelEle}  target="_self" class="contentRight" contentRight="OutwardAccountBankUsed:CheckTransferIn:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
				    <span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId={id}>{matchingAmtIn}</span>\
				</td>\
                <td><span>{holderStr}</span></td>\
				<td><span>{flagStr}</span></td>\
                <td>'
                    +'<button class="btn btn-xs btn-white {recycleBntClass} btn-bold  " onclick="showRecycleModal({id},{transBlackTo},refreshContent)"><i class="ace-icon fa fa-reply bigger-100 {recycleIconClass}"></i><span>回收</span></button>'
                    +'<button class="btn btn-xs btn-white btn-warning btn-bold orange " onclick="showStopTempModal({id},refreshContent)"><i class="ace-icon fa fa-stop bigger-100 red"></i><span>停用</span></button>'
                    +'<button class="btn btn-xs btn-white btn-warning btn-bold orange " onclick="showFreezeModal({id},refreshContent)"><i class="ace-icon fa fa-remove bigger-100 red"></i><span>冻结</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange contentRight {updateClass}" contentRight="OutwardAccountBankUsed:Update:*" onclick="showUpdateOutAccount({id},refreshContent)"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange contentRight {updateFlag2Class}" contentRight="OutwardAccountBankUsed:Update:*" onclick="showUpdateflag2Account({id},refreshContent)"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改限额</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange {OperatorLogBtn}" onclick="showModal_accountExtra({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>\
                </td>\
           </tr>';

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var totalBalanceBySys = 0,totalBalanceByBank =0 ,totalDailyOutward=0,idList=new Array,idArray= new Array();
    $.each(data,function(idx, obj) {
    	totalBalanceBySys = totalBalanceBySys + obj.balance ;
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
        totalDailyOutward = totalDailyOutward + obj.outwardAmountDaily;
        obj.statusHTMl=getDeviceStatusInfoHoverHTML(obj);
        if(obj.flag&&obj.flag*1==2){
        	//返利网账号（flag=2)
        	obj.holderStr="返利网";
        	if(obj.peakBalance&&obj.peakBalance>0){
        		//余额峰值(保证金)大于0 可以修改限额
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
        obj.flagStr=getFlagStr(obj.flag);
        obj.OperatorLogBtn=OperatorLogBtn;
        obj.recycleBntClass =   obj.transBlackTo?'btn-primary':'btn-warning';
        obj.recycleIconClass =  obj.transBlackTo?'blue':'red';
        obj.transBlackTo = obj.transBlackTo?obj.transBlackTo:'0';
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
        obj.outwardAmountDaily = obj.outwardAmountDaily+'';
        obj.balance = obj.balance?obj.balance:'0';
        obj.limitBalanceIcon=getlimitBalanceIconStr(obj);
        obj.accountStatInOutCategoryIn = accountStatInOutCategoryIn+'';
        obj.accountStatInOutCategoryOutMember=accountStatInOutCategoryOutMember+'';
        obj.alias =$.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName =obj.currSysLevelName?obj.currSysLevelName:'';
        obj.handicapName =obj.handicapName?obj.handicapName:'';
        obj.hideAccount=hideAccountAll(obj.account);
        obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.owner?obj.owner:'无')+'</br>'+(obj.hideAccount?obj.hideAccount:"无" );
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.matchingAmtOut ='';
        obj.matchingAmtIn ='';
		obj.sysBalance = '---';
        idList.push({'id':obj.id});
		idList.push({'id':obj.id,type:'transAskMonitorRiskHover'});
        idArray.push(obj.id);
    });
    $tbody.html(fillDataToModel4Array(data,trHtml));
    if(data){
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
    	showSubAndTotalStatistics4Table($tbody,{column:14, subCount:data.length,count:page.totalElements,6:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance},7:{subTotal:totalBalanceBySys,total:page.header.totalAmountBalance}});
    	loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutMember],idArray,null,function(){ loadContentRight() });
        SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
    }
   
}

function refreshContent(pageNo){
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0);
    authRequest.pageNo = authRequest.pageNo<0?0:authRequest.pageNo;
    authRequest.search_IN_handicapId = $("select[name='search_EQ_handicapId']").val().toString();
    authRequest.deviceStatus=$("#deviceStatus").val();
    authRequest.typeToArray= [accountTypeOutBank].toString();
    authRequest.statusToArray= [accountStatusNormal].toString();
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.currSysLevel=$("input[name='currSysLevel']:checked").val();
    authRequest.search_EQ_flag=$("input[name='flag']:checked").val();
	if($("[name=holderType]:checked").length==1){
		authRequest.holderType=$("[name=holderType]:checked").val();
	}else{
		$("[name=holderType]").attr("checked","checked");
		authRequest.holderType=null;
	}
	authRequest.transBlackTo=1;
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
    authRequest.bankType=$.trim($("select[name='search_LIKE_bankType']").val());
	if(!authRequest.bankType||authRequest.bankType=='请选择'){
		authRequest.bankType = null;
	}
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

var showModal_outLimitInfo=function(){
	var $div=$("#outLimitModal").modal("toggle");
};

var selectAllOrNotAll = function(obj){
	var checked = obj.checked;
	$('input[name=checkboxAccId]').attr('checked',checked);
};

var saveRecycleByBatch = function(){
	var array = [];
	$('input[name=checkboxAccId]:checked').each(function(index,obj){
		array.push(obj.value);
	});
	if(array && array.length ==0){
		showMessageForFail('请勾选要批量回收的账号.');
	    return;
	}
	var tips ="<span class='red'>将停止下发给该账号,当金额过低时,将自动回收到可用状态,(自动回收,只对机器出款有效)</span>";
	bootbox.dialog({
		message: "<span class='bigger-110'>确定批量回收账号（转可用）<br/>"+tips+"</span>",
		buttons:{
			"click" :{"label" : "回收","className" : "btn-sm btn-primary","callback": function() {
				$.ajax({ type:"post", url:API.r_account_recycle4OutwardAccountByBatch,data:{"accArray":array.toString()},dataType:'json',success:function(jsonObject){
					if(jsonObject.status == 1){
						refreshContent();
					}else{
						showMessageForFail(jsonObject.message);
					}
				}});
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
};

var resetOutwardInfo=function(){
	var $divOutdraw=$("#outwardInfoModal");

	var zone_options="";
	zone_options+="<option value='' >"+("-----请选择-----")+"</option>";
	$.each(zone_list_all,function(index,record){
		zone_options+="<option value="+record.id+" code="+record.code+" >"+record.name+"</option>";
	});
	$divOutdraw.find("select[name='zone_sync']").html(zone_options);

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
    $divOutdraw.find("[name=OUTDRAW_LIMIT_OUT_ONE]").val(sysSetting.OUTDRAW_LIMIT_OUT_ONE);
    $divOutdraw.find("[name=OUTDRAW_SYSMONEYLIMIT]").val(sysSetting.OUTDRAW_SYSMONEYLIMIT);
    $divOutdraw.find("[name=OUTDRAW_SYSMONEY_LOWEST]").val(sysSetting.OUTDRAW_SYSMONEY_LOWEST);
    $divOutdraw.find("[name=MOBILE_BEGIN_ISSUED_LESS_PERCENT]").val(sysSetting.MOBILE_BEGIN_ISSUED_LESS_PERCENT);
    $divOutdraw.find("[name=OUTDRAW_LIMIT_APPROVE]").val(sysSetting.OUTDRAW_LIMIT_APPROVE);
    $divOutdraw.find("[name=OUTDRAW_LIMIT_APPROVE_MANAGE]").val(sysSetting.OUTDRAW_LIMIT_APPROVE_MANAGE);
    $divOutdraw.find("[name=OUTDRAW_CHECK_CODE]").val(sysSetting.OUTDRAW_CHECK_CODE);
    $divOutdraw.find("[name=OUTDRAW_SPLIT_AMOUNT_OUTSIDE]").val(sysSetting.OUTDRAW_SPLIT_AMOUNT_OUTSIDE);
    $divOutdraw.find("[name=OUTDRAW_SPLIT_AMOUNT_INSIDE]").val(sysSetting.OUTDRAW_SPLIT_AMOUNT_INSIDE);
    $divOutdraw.find("[name=OUTDRAW_THIRD_LOWEST_BAL]").val(sysSetting.OUTDRAW_THIRD_LOWEST_BAL);
	$divOutdraw.find("[name=OUTDRAW_RATE_LIMIT]").val(sysSetting.OUTDRAW_RATE_LIMIT);
	$divOutdraw.find("[name=DAIFU_AMOUNT_UPLIMIT]").val(sysSetting.DAIFU_AMOUNT_UPLIMIT);
};


var saveOutwardInfo=function(){
	bootbox.confirm("确定修改系统设置?", function(result) {
		if (result) {
			var $divOutdraw=$("#outwardInfoModal");
		    var keysArray = new Array(), valsArray = new Array();
		    var validate = new Array();
		    var $OUTDRAW_LIMIT_CHECKOUT_TODAY = $divOutdraw.find("[name=OUTDRAW_LIMIT_CHECKOUT_TODAY]");
		    var $OUTDRAW_LIMIT_OUT_ONE = $divOutdraw.find("[name=OUTDRAW_LIMIT_OUT_ONE]");
		    var $OUTDRAW_SYSMONEYLIMIT = $divOutdraw.find("[name=OUTDRAW_SYSMONEYLIMIT]");
		    var $OUTDRAW_SYSMONEY_LOWEST = $divOutdraw.find("[name=OUTDRAW_SYSMONEY_LOWEST]");
		    var $MOBILE_BEGIN_ISSUED_LESS_PERCENT = $divOutdraw.find("[name=MOBILE_BEGIN_ISSUED_LESS_PERCENT]");
		    var $OUTDRAW_LIMIT_APPROVE = $divOutdraw.find("[name=OUTDRAW_LIMIT_APPROVE]");
		    var $OUTDRAW_LIMIT_APPROVE_MANAGE = $divOutdraw.find("[name=OUTDRAW_LIMIT_APPROVE_MANAGE]");
		    var $OUTDRAW_CHECK_CODE = $divOutdraw.find("[name=OUTDRAW_CHECK_CODE]");
		    var $OUTDRAW_SPLIT_AMOUNT_OUTSIDE = $divOutdraw.find("[name=OUTDRAW_SPLIT_AMOUNT_OUTSIDE]");
		    var $OUTDRAW_SPLIT_AMOUNT_INSIDE = $divOutdraw.find("[name=OUTDRAW_SPLIT_AMOUNT_INSIDE]");
		    var $OUTDRAW_THIRD_LOWEST_BAL = $divOutdraw.find("[name=OUTDRAW_THIRD_LOWEST_BAL]");
			var $OUTDRAW_RATE_LIMIT = $divOutdraw.find("[name=OUTDRAW_RATE_LIMIT]");
			var $DAIFU_AMOUNT_UPLIMIT = $divOutdraw.find("[name=DAIFU_AMOUNT_UPLIMIT]");
		    //转主管审核金额不能小于人工审核配置金额
		    if ($.trim($OUTDRAW_LIMIT_APPROVE_MANAGE.val()) * 1 < $.trim($OUTDRAW_LIMIT_APPROVE.val()) * 1) {
		        showMessageForCheck("保存失败,“转主管审核金额” 不可小于 “人工审核配置金额”");
		        setTimeout(function(){       
		            $('body').addClass('modal-open');
		        },500);
		        return;
		    }
		    validate.push({
		        ele: $OUTDRAW_LIMIT_CHECKOUT_TODAY,
		        name: '当日出款最大限额',
		        type: 'amountPlus',
		        minEQ: 10000,
		        maxEQ: 1000000
		    });
		    validate.push({
		    	ele: $OUTDRAW_LIMIT_OUT_ONE,
		    	name: '单笔出款最大限额',
		    	type: 'amountPlus',
		    	minEQ: 10000,
		    	maxEQ: 50000
		    });
		    validate.push({ele: $OUTDRAW_SYSMONEYLIMIT, name: '机器自动出款最大限额', type: 'amountPlus', minEQ: 1000, maxEQ: 50000});
		    validate.push({ele: $OUTDRAW_SYSMONEY_LOWEST, name: '出款卡最低余额', type: 'amountPlus', minEQ: 1000, maxEQ: 10000});
		    validate.push({ele: $MOBILE_BEGIN_ISSUED_LESS_PERCENT, name: '手机信用额度下发百分比', type: 'amountPlus', minEQ: 1, maxEQ: 50});
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
		    validate.push({
		    	ele: $DAIFU_AMOUNT_UPLIMIT,
		    	name: '第三方代付最高限额',
		    	type: 'amountPlus',
		    	minEQ: 100,
		    	maxEQ: 100000
		    });
			if($OUTDRAW_RATE_LIMIT.val()){
				validate.push({
					ele: $OUTDRAW_RATE_LIMIT,
					name: '出款频率',
					type: 'amountCanZero',
					minEQ: 0,
					maxEQ: 3600
				});
			}
		    $.each($divOutdraw.find("input"), function (index, result) {
		        keysArray.push($(result).attr("name"));
		        valsArray.push($(result).val());
		    });
		    //校验
		    if (!validateEmptyBatch(validate) || !validateInput(validate)) {
				 setTimeout(function(){       
			            $('body').addClass('modal-open');
			        },500);
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
		 setTimeout(function(){       
	            $('body').addClass('modal-open');
	        },500);
	});
};

var saveOutwardLimit=function(){
	var keysArray = new Array(), valsArray = new Array();
	var $divOutdraw=$("#outLimitModal");
	var $OUTDRAW_OUTER_LIMIT_TODAY = $divOutdraw.find("[name=OUTDRAW_OUTER_LIMIT_TODAY]");
    var $OUTDRAW_INNER_LIMIT_TODAY = $divOutdraw.find("[name=OUTDRAW_INNER_LIMIT_TODAY]");
    var $OUTDRAW_SPECIFY_LIMIT_TODAY = $divOutdraw.find("[name=OUTDRAW_SPECIFY_LIMIT_TODAY]");
	var $search_EQ_handicapId = $divOutdraw.find("[name=search_EQ_handicapId]");
	var $search_EQ_bankType = $divOutdraw.find("[name=search_EQ_bankType]");
	var $search_EQ_flag = $divOutdraw.find("[name=search_EQ_flag]");
	var handicapText ='全部';
	if($search_EQ_handicapId.val()){
		handicapText = $search_EQ_handicapId.find("option[value="+$search_EQ_handicapId.val()+"]").text();
	}
    if($OUTDRAW_OUTER_LIMIT_TODAY.val() && $OUTDRAW_OUTER_LIMIT_TODAY.val()*1>1000000){
    	showMessageForCheck("最大值只能设置为1000000");
    	 return;
    }
    if($OUTDRAW_INNER_LIMIT_TODAY.val() && $OUTDRAW_INNER_LIMIT_TODAY.val()*1>1000000){
    	 showMessageForCheck("最大值只能设置为1000000");
    	 return;
    }
    if( $OUTDRAW_SPECIFY_LIMIT_TODAY.val() &&  $OUTDRAW_SPECIFY_LIMIT_TODAY.val()*1>1000000){
    	showMessageForCheck("最大值只能设置为1000000");
    	 return;
    }
	bootbox.confirm("确定修改&nbsp;&nbsp;"+handicapText+"&nbsp;&nbsp;在用出款卡当日出款限额?", function(result) {
		if (result) {
		    $.ajax({
		        type: "PUT",
		        dataType: 'JSON',
		        url: '/r/account/updateOuterLimit',
		        async: false,
		        data: {
		            "outerLimit": $OUTDRAW_OUTER_LIMIT_TODAY.val(),
		            "innerLimit": $OUTDRAW_INNER_LIMIT_TODAY.val(),
		            "specifyLimit":$OUTDRAW_SPECIFY_LIMIT_TODAY.val(),
					"handicapId":$search_EQ_handicapId.val(),
					"bankType":$search_EQ_bankType.val(),
					"flag":$search_EQ_flag.val()
		        },
		        success: function (jsonObject) {
		            if (jsonObject && jsonObject.status == 1) {
		                showMessageForSuccess("保存成功");
		                //置空填写的值
		                $OUTDRAW_OUTER_LIMIT_TODAY.val("");
		                $OUTDRAW_INNER_LIMIT_TODAY.val("");
		                $OUTDRAW_SPECIFY_LIMIT_TODAY.val("");
		                searchByFilter();
		                $divOutdraw.modal("toggle");
		            } else {
		                showMessageForFail("保存失败" + jsonObject.message);
		            }
		        }
		    });
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
		 setTimeout(function(){       
	            $('body').addClass('modal-open');
	        },500);
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
$("#accountFilter [name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
	searchByFilter();
});
$("#accountFilter [name=holderType],[name=currSysLevel],[name=flag]").click(function(){
	searchByFilter();
});
var changeDevice=function(deviceStaus){
	$("#deviceStatus").val(deviceStaus);
	searchByFilter(0);
}

getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("select[name='search_EQ_bankType']"),null,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
refreshContent(0);
contentRight();
