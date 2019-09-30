/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountPage .Current_Page").text();
	//封装data
	var $div = $("#accountFilter");
    var flagToArray = new Array();
    $div.find("input[name='search_IN_flag']:checked").each(function(){
    	flagToArray.push(this.value);
    });
    if(flagToArray.length==0){
    	flagToArray=[accountFlagPC,accountFlagRefund];
    }
    var data = {
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize'),
        search_IN_handicapId:handicapId_list.toString(),
        search_LIKE_account:$.trim($div.find("input[name='search_LIKE_account']").val()),
        search_LIKE_bankType:$.trim($div.find("[name='search_LIKE_bankType']").val()),
        search_EQ_alias:$.trim($div.find("input[name='search_EQ_alias']").val()),
        search_LIKE_owner:$.trim($div.find("input[name='search_LIKE_owner']").val()),
        search_IN_flag:flagToArray.toString(),
        typeToArray:[accountTypeOutBank].toString(),//出款卡
        currSysLevel:$div.find("input[name='currSysLevel']:checked").val(),
        deviceStatus:$("#deviceStatus").val(),
        sortProperty:'bankBalance',
        needTotal:1,//需要查询账号状态总计
        transBlackTo:1,
        sortDirection:1
    };
    //状态
    if(data.deviceStatus){
    	data.statusToArray=[1,5].toString();
    	if(data.deviceStatus=="online"||data.deviceStatus=="offline"){
    		//在线离线  有区分在用可用
    	    data.statusToArray=$div.find("input[name='search_IN_status']:checked").val();
    	}else if(data.deviceStatus=="stop"){
    		data.deviceStatus=null;
    		data.statusToArray=[4].toString();
    	}
    }else{
    	data.statusToArray=[1,4,5].toString();
    }
	//持卡人 相关查询条件任何TAB都需要传递到后台（后台根据查询条件分别算出在线离线和停用冻结的总数）
	data.operator = $("input[name='search_LIKE_operator']").val();
	data.holderType=$("[name=holderType]:checked").val();
    //发送请求
	$.ajax({
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:API.r_account_list2,
        data:data,
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			var $tbody=$("#accountListTable").find("tbody");
			var trs="";
			var totalBalanceByBank =0 ,totalBalance=0,idList=new Array(),idArray = new Array();
			$.each(jsonObject.data,function(index,record){
				idArray.push(record.id);
				idList.push({'id':record.id});
				var tr="";
				if(record.status==accountStatusNormal||(record.status==accountStatusEnabled&&((!record.flag||record.flag==0)||(record.flag&&record.flag*1==2&&record.peakBalance&&record.peakBalance>0)))){
		        	//在用卡  或者 （可用卡 返利网账号（flag=2) 余额峰值(保证金)大于0 ）可以分配
					tr+="<td><input type='checkbox' name='checkboxAccId'  class='ace' value='"+record.id+"'  /><span class='lbl'></span></td>";
		        }else{
		        	tr+="<td></td>";
		        }
				tr+="<td><span>"+_checkObj(record.currSysLevelName)+"</span></td>";
				tr+="<td><span>"+_checkObj(record.alias)+"</span></td>";	
				tr+="<td>" +getAccountInfoHoverHTML(record) +"</td>";
                tr+="<td>"+getStatusInfoHoverHTML(record)+"<br/>"+getDeviceStatusInfoHoverHTML(record)+"</td>";
				!record.bankBalance?record.bankBalance=0:null;
				!record.balance?record.balance=0:null;
				tr+="<td><div class='BankLogEvent' target='"+record.id+"'><span name='bankBalance' class='amount'>"+record.bankBalance+htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily)+"</span><span class='time'></span></div></td>";
				tr+="<td>"+record.balance+"</td>";
				//余额更新时间
				tr+="<td style='text-align:left !important'>";
				tr+="&nbsp;&nbsp;<a title='"+_checkObj(record.bankBalTime)+"'>银行："+hhmmss(record.bankBalTime);
				tr+="</a><br/>&nbsp;&nbsp;<a title='"+_checkObj(record.sysBalTime)+"'>系统："+hhmmss(record.sysBalTime);
				tr+="</a></td>";
				//转入/转出记录
				record.incomeRequestStatusMatching=incomeRequestStatusMatching+'';
				record.incomeRequestStatusMatched=incomeRequestStatusMatched+'';
		        record.incomeRequestStatusCanceled=incomeRequestStatusCanceled+'';
		        record.outwardTaskStatusDeposited =outwardTaskStatusDeposited+'';
		        record.outwardTaskStatusMatched=outwardTaskStatusMatched+'';
		        record.outwardTaskStatusFailure=outwardTaskStatusFailure+'';
		        record.inCountMapped=!!!record.inCount.mapped?"0":record.inCount.mapped;
		        record.inCountMapping=!!!record.inCount.mapping?"0":record.inCount.mapping;
		        record.inCountCancel=!!!record.inCount.cancel?"0":record.inCount.cancel;
		        record.outCountMapped=!!!record.outCount.mapped?"0":record.outCount.mapped;
		        record.outCountMapping=!!!record.outCount.mapping?"0":record.outCount.mapping;
		        record.outCountCancel=!!!record.outCount.cancel?"0":record.outCount.cancel;
		        record.accountStatInOutCategoryIn = accountStatInOutCategoryIn+'';
		        record.accountStatInOutCategoryOutMember=accountStatInOutCategoryOutMember+'';
				record.inCountMappedEle=(record.inCount.mapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
				record.inCountMappingEle=(record.inCount.mapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
		        record.inCountCancelEle=(record.inCount.cancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
		        record.outCountMappedEle=(record.outCount.mapped!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+record.id+'&outwardTaskStatus='+outwardTaskStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
		        record.outCountMappingEle=(record.outCount.mapping!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+record.id+'&outwardTaskStatus='+outwardTaskStatusDeposited+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
		        record.outCountCancelEle=(record.outCount.cancel!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+record.id+'&outwardTaskStatus='+outwardTaskStatusFailure+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
				tr+="<td style='text-align:left !important'>";
				tr+='&nbsp;&nbsp;转入：\
					<a '+record.inCountMappingEle+' target="_self" class="contentRight" contentRight="AccountOutComp:InOutDetail:*" accountStatInOutCategory="'+record.accountStatInOutCategoryIn+'" accountStatInOutId="'+record.id+'" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">'+record.inCountMapping+'</span></a>\
					<a '+record.inCountMappedEle+' target="_self" class="contentRight" contentRight="AccountOutComp:InOutDetail:*" accountStatInOutCategory="'+record.accountStatInOutCategoryIn+'" accountStatInOutId="'+record.id+'" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">'+record.inCountMapped+'</span></a>\
					<a '+record.inCountCancelEle+' target="_self" class="contentRight" contentRight="AccountOutComp:InOutDetail:*" accountStatInOutCategory="'+record.accountStatInOutCategoryIn+'" accountStatInOutId="'+record.id+'" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">'+record.inCountCancel+'</span></a>\
					<span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId="'+record.id+'">'+record.matchingAmtIn+'</span>\
					<br/>&nbsp;&nbsp;转出：\
					 <a '+record.outCountMappingEle+' target="_self" class="contentRight" contentRight="AccountOutComp:InOutDetail:*" accountStatInOutCategory="'+record.accountStatInOutCategoryOutMember+'" accountStatInOutId="'+record.id+'" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">'+record.outCountMapping+'</span></a>\
					 <a '+record.outCountMappedEle+' target="_self"  class="contentRight" contentRight="AccountOutComp:InOutDetail:*" accountStatInOutCategory="'+record.accountStatInOutCategoryOutMember+'" accountStatInOutId="'+record.id+'" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">'+record.outCountMapped+'</span></a>\
					 <a '+record.outCountCancelEle+' target="_self"  class="contentRight" contentRight="AccountOutComp:InOutDetail:*" accountStatInOutCategory="'+record.accountStatInOutCategoryOutMember+'" accountStatInOutId="'+record.id+'" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">'+record.outCountCancel+'</span></a>\
					 <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId="'+record.id+'">'+record.matchingAmtOut+'</span>';
				tr+="</td>";
				//操作
				tr+="<td>";
				//是否可修改状态
				var canChangeStatus=(record.flag&&record.flag*1==2)?false:true;	//PC默认都展示状态按钮
				if(record.flag&&record.flag*1==2&&record.peakBalance&&record.peakBalance>0){
		        	//返利网账号（flag=2) 余额峰值(保证金)大于0 可以修改状态
					canChangeStatus=true;
		        }
				//回收
				var btn_recycle='<button class="btn btn-xs btn-white '+(record.transBlackTo?' btn-primary ':' btn-warning ')+' btn-bold contentRight"  contentright="AccountOutComp:Update:*" onclick="showRecycleModal('+record.id+','+(record.transBlackTo?record.transBlackTo:'0')+',showAccountList)">\
											<i class="ace-icon fa fa-reply bigger-100 '+(record.transBlackTo?' blue ':' red ')+'"></i>\
											<span>回收</span>\
										</button>';
				//分配
				var btn_allocate='<button class="btn btn-xs btn-white btn-warning btn-bold orange contentRight"  contentright="AccountOutComp:Update:*" onclick="showAllocateModal('+record.id+',showAccountList)">\
											<i class="ace-icon fa fa-list bigger-100 red"></i>\
											<span>分配</span>\
										</button>';
				var deviceStatus=$("#deviceStatus").val();
				if(deviceStatus=="online"||deviceStatus=="offline"){
					//在线离线   在用，可用卡
					if(record.status==1){//在用卡
						tr+=btn_recycle;	//回收
					}else if(record.status==5){//可用卡
						if(canChangeStatus) tr+=btn_allocate;	//分配
					}
				}
				//修改
				tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentright='AccountOutComp:Update:*'  " +
				"onclick='showUpdateAccountNewModal("+record.id+",showAccountList)'>"+
				"<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
				//状态
				tr+="<button class='btn btn-xs btn-white btn-success btn-bold "+isHideAccountBtn+"'  " +
				"onclick='showModal_accountBaseInfo("+record.id+")'>"+
				"<i class='ace-icon fa fa-eye bigger-100 green'></i><span>状态</span></button>";
				//操作记录
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold "+OperatorLogBtn+"' " +
						"onclick='showModal_accountExtra("+record.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
				//明细
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
						"onclick='showInOutListModal("+record.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+="</td>";
				trs+="<tr id='mainTr"+record.id+"'>"+tr+"</tr>";
				totalBalanceByBank+=record.bankBalance*1;
				totalBalance+=record.balance*1;
			});
			$tbody.html(trs);
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			loadHover_accountStopReasonHover(idArray);
			//入款卡统计总卡数量
			if(jsonObject&&jsonObject.page&&jsonObject.page.header&&jsonObject.page.header.IdSize){
				var IdSize=jsonObject.page.header.IdSize;
				$("#totalIdSize").text(IdSize[0]);
				$("#onlineSize").text(IdSize[1]);
				$("#offlineSize").text(IdSize[2]);
				$("#stopSize").text(IdSize[3]);
			}
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var totalRows={
						column:15, 
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements
					};
				totalRows[6]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
				totalRows[7]={subTotal:totalBalance,total:jsonObject.page.header.totalAmountBalance};
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			//分页初始化
			showPading(jsonObject.page,"accountPage",showAccountList,null,false,true);
			loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutMember],idArray,null,function(){ loadContentRight() });
			contentRight();
			SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
        }
	});
}

function loadContentRight(){
    contentRight({'AccountOutComp:InOutDetail:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }, 'AccountOutComp:InOutDetail:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }});
}

/** 限额按钮 */
var showModal_outLimitInfo=function(){
	var $div=$("#outLimitModal").modal("toggle");
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
		            	showAccountList();
		                $divOutdraw.modal("toggle");
		            } else {
		                showMessageForFail("保存失败" + jsonObject.message);
		            }
		        }
		    });
		}
	});
};

/** 设置按钮 */
var showModal_outwardInfo=function(){
	var $div=$("#outwardInfoModal").modal("toggle");
	//重置参数
	resetOutwardInfo();
}
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
		    validate.push({ele: $OUTDRAW_LIMIT_CHECKOUT_TODAY,name: '当日出款最大限额',type: 'amountPlus',minEQ: 10000,maxEQ: 1000000});
		    validate.push({ele: $OUTDRAW_LIMIT_OUT_ONE,name: '单笔出款最大限额',type: 'amountPlus',minEQ: 10000,maxEQ: 50000});
		    validate.push({ele: $OUTDRAW_SYSMONEYLIMIT, name: '机器自动出款最大限额', type: 'amountPlus', minEQ: 1000, maxEQ: 50000});
		    validate.push({ele: $OUTDRAW_SYSMONEY_LOWEST, name: '出款卡最低余额', type: 'amountPlus', minEQ: 1000, maxEQ: 10000});
		    validate.push({ele: $MOBILE_BEGIN_ISSUED_LESS_PERCENT, name: '手机信用额度下发百分比', type: 'amountPlus', minEQ: 1, maxEQ: 50});
		    validate.push({ele: $OUTDRAW_LIMIT_APPROVE,name: '大额出款，超过此设置将人工审核',type: 'amountPlus',minEQ: 10000,maxEQ: 100000});
		    validate.push({ele: $OUTDRAW_LIMIT_APPROVE_MANAGE,name: '大额出款，超过此设置将转主管审核',type: 'amountPlus',minEQ: 50000,maxEQ: 5000000});
		    validate.push({ele: $OUTDRAW_CHECK_CODE, name: '出款审核几倍打码量限制', type: 'positiveInt', min: 0, max: 10});
		    validate.push({ele: $OUTDRAW_SPLIT_AMOUNT_OUTSIDE,name: '外层拆单金额',type: 'amountPlus',minEQ: 20000,maxEQ: 50000});
		    validate.push({ele: $OUTDRAW_SPLIT_AMOUNT_INSIDE,name: '内层拆单金额',type: 'amountPlus',minEQ: 20000,maxEQ: 50000});
		    validate.push({ele: $OUTDRAW_THIRD_LOWEST_BAL,name: '第三方出款最低金额',type: 'amountPlus',minEQ: 10000,maxEQ: 9999999});
		    validate.push({ele: $DAIFU_AMOUNT_UPLIMIT,name: '第三方代付最高限额',type: 'amountPlus',minEQ: 100,maxEQ: 100000});
			if($OUTDRAW_RATE_LIMIT.val()){
				validate.push({ele: $OUTDRAW_RATE_LIMIT,name: '出款频率',type: 'amountCanZero',minEQ: 0,maxEQ: 3600});
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
/** 同步出款订单 */
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
/** 一键回收 */
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
		                showMessageForSuccess("回收成功");
						showAccountList();
					}else{
						showMessageForFail(jsonObject.message);
					}
				}});
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
};

/** 一键分配 */
var saveAllocByBatch = function(){
    var array = [];
    $('input[name=checkboxAccId]:checked').each(function(index,obj){
        array.push(obj.value);
    });
    if(array && array.length ==0){
        showMessageForFail('请勾选要批量分配的账号&nbsp;(&nbsp;只针对机器&nbsp;)&nbsp;.');
        return;
    }
    bootbox.dialog({
        message: "<span class='bigger-110'>批量分配给出款账号,&nbsp;(&nbsp;只针对机器&nbsp;)&nbsp;</span>",
        buttons:{
            "click" :{"label" : "批量分配","className" : "btn-sm btn-primary","callback": function() {
                $.ajax({ type:"post", url:API.r_account_asin4OutwardAccountByBatch,data:{"accArray":array.toString()},dataType:'json',success:function(jsonObject){
                    if(jsonObject.status == 1){
		                showMessageForSuccess("分配成功");
						showAccountList();
                    }else{
                        showMessageForFail(jsonObject.message);
                    }
                }});
            }},
            "click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
        }
    });
};

/** 切换设备状态TAB */
var changeDevice=function(deviceStaus){
	$("#deviceStatus").val(deviceStaus);
	hideShowButton();
	showAccountList(0);
}
/** 查询条件 在用可用切换 */
$("#accountFilter").find("[name=search_IN_status]").click(function(){
	hideShowButton();
	showAccountList();
});
var hideShowButton=function(){
	var deviceStaus=$("#deviceStatus").val();
	if(deviceStaus&&(deviceStaus=='online'||deviceStaus=='offline')){//在线离线
		$(".statusDiv").show();
		var currStatus=$("#accountFilter").find("[name='search_IN_status']:checked").val();
		if(currStatus==1){
			//在用
			$(".status1_show").show();
			$(".status5_show").hide();
		}else if(currStatus==5){
			//可用
			$(".status5_show").show();
			$(".status1_show").hide();
		}else{
			$(".status1_show").hide();
			$(".status5_show").hide();
		}
	}else{
		$(".statusDiv").hide();
		$(".status1_show").hide();
		$(".status5_show").hide();
	}
}
/** 全选/全不选 */
var selectAllOrNotAll = function(obj){
	var checked = obj.checked;
	$('[name=checkboxAccId]').prop('checked',checked);
};
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #searchBtn"),80,"refresh_accountOutcomp");
showAccountList();



$("#accountFilter").find("[name=search_EQ_LevelId],[name=search_LIKE_account],[name=search_LIKE_bankType]").change(function(){
	showAccountList();
});
$("#accountFilter").find("[name=currSysLevel],[name=search_IN_flag],[name=holderType],#searchBtn").click(function(){
	showAccountList();
});
$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		$("#accountFilter #searchBtn button").click();
	}
});