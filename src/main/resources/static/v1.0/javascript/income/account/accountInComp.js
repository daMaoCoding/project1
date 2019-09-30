currentPageLocation = window.location.href;
var EncashLog=false;
$.each(ContentRight['IncomeAccountComp:*'], function (name, value) {
    if (name == 'IncomeAuditComp:EncashLog:*') {
    	EncashLog = true;
    }
});


$("#accountFilter").find("[name=search_EQ_handicapId],[name=search_EQ_LevelId],[name=search_LIKE_account],[name=search_LIKE_bankType]").change(function(){
	showAccountList();
});
$("#accountFilter").find("[name=currSysLevel],[name=search_IN_flag],#searchBtn").click(function(){
	showAccountList();
});


//Tab切换 账号类型
var changeTabInit=function(accountType){
	//必须先更新选中Tab状态，后续数据会根据此值切换
	$("#accountTabType").val(accountType);
	//根据账号类型切换表头
	if(accountType==accountTypeInBank){//银行入款卡
		$("[name=status5Lbl]").html("新卡");
		$(".incomeShow").show();
		$(".incomeHide").hide();
	}else if(accountType==accountTypeReserveBank ){//备用卡
		$("[name=status5Lbl]").html("可用");
		//备用卡 不允许设置出入款额度
		$(".incomeShow").hide();
		$(".incomeHide").show();
	}
	//切换Tab后， 分页重新加载
	showAccountList(0);
}
/** 切换设备状态TAB */
var changeDevice=function(deviceStaus){
	$("#deviceStatus").val(deviceStaus);
	showAccountList(0);
}

//初始化新增银行卡信息
var loadAddBindCard=function(){
	var nbsp='&nbsp;&nbsp;&nbsp;&nbsp;';
	var $div=$("#bindIssuedModal #tabAddBindBank");
	$div.find("#tableAdd td").css("padding-top","10px");
	//加载银行品牌
	var options="";
	options+="<option value='' >"+nbsp+"请选择"+nbsp+"</option>";
	$.each(bank_name_list,function(index,record){
		options+="<option value="+record+" >"+nbsp+record+nbsp+"</option>";
	});
	$div.find("select[name=choiceBankBrand]").html(options);
}

/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	//不指定账号类型时，获取隐藏input的类型值
	var accountType=$("#accountTabType").val();
	var isReserve=(accountType==accountTypeReserveBank);//是否备用卡
	var pageId=isReserve?"account8Page":"account1Page";
	if(!!!CurPage&&CurPage!=0) CurPage=$("#"+pageId+" .Current_Page").text();
	//封装data
	var $div = $("#accountFilter");
    var search_LIKE_account = $div.find("[name='search_LIKE_account']").val();
    var search_LIKE_bankType = $div.find("[name='search_LIKE_bankType']").val();
    var search_LIKE_auditor = $div.find("input[name='search_LIKE_auditor']").val();
    var search_LIKE_owner = $div.find("input[name='search_LIKE_owner']").val();
    var search_EQ_alias = $div.find("input[name='search_EQ_alias']").val();
    var flagToArray = new Array();
    $div.find("input[name='search_IN_flag']:checked").each(function(){
    	flagToArray.push(this.value);
    });
    if(flagToArray.length==0){
    	flagToArray=[accountFlagPC,accountFlagRefund];
    }
    var fromIdArray=new Array(),tdName='Outdetail',tdIncomeName='incomeDetail',tdEncashName="encash";
    var data = {
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize'),
        search_LIKE_account:$.trim(search_LIKE_account),
        search_LIKE_bankType:$.trim(search_LIKE_bankType),
        search_EQ_alias:$.trim(search_EQ_alias),
        auditor:$.trim(search_LIKE_auditor),
        search_LIKE_owner:$.trim(search_LIKE_owner),
        search_IN_flag:flagToArray.toString(),
        typeToArray:[accountType].toString(),
        currSysLevel:$div.find("input[name='currSysLevel']:checked").val(),
        deviceStatus:$("#deviceStatus").val(),
        sortProperty:'bankBalance',
        needTotal:1,//需要查询账号状态总计
        sortDirection:1
    };
    if(isReserve){
    	data.search_IN_handicapId=handicapId_list.toString();
    }else{
    	data.search_IN_handicapId=$div.find("select[name='search_EQ_handicapId']").val();
    }
    if(data.deviceStatus){
    	data.statusToArray=[1,5].toString();
    	if(data.deviceStatus=="stop"){
    		data.deviceStatus=null;
    		data.statusToArray=[4].toString();
    	}
    }else{
    	data.statusToArray=[1,4,5].toString();
    }
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
			var $tbody=$("#tab1 table").find("tbody");
			if(isReserve){
				$tbody=$("#tab8 table").find("tbody");
			}
			$tbody.html("");
			var totalBalanceByBank =0 ,totalBalance=0,idList=new Array(),idArray = new Array();
			$.each(jsonObject.data,function(index,record){
				fromIdArray.push(record.id);
				idArray.push(record.id);
				idList.push({'id':record.id});
				idList.push({'id':record.id,type:'transAskMonitorRiskHover'});
				var tr="";
				if(!isReserve){
					//云闪付（边入边出） 或者PC类型（大额专用（自购卡）,数据库可能是空值） 不允许勾选
					var isHideCheckBox=(!record.flag||record.flag=="0")?" disabled  title='大额专用（自购卡）不能勾选' ":"";
					tr+="<td><input type='checkbox' name='checkbox_limit' class='ace' value='"+record.id+"' "+isHideCheckBox+"><span class='lbl'></span></td>";
					tr+="<td><span>"+record.handicapName+"</span></td>";
				}
				tr+="<td><span name='currSysLevelName"+record.id+"'>"+(record.currSysLevelName?record.currSysLevelName:'')+"</span></td>";
				tr+="<td><span name='alias"+record.id+"'>"+((record.alias&&record.alias!='null')?record.alias:'')+"</span></td>";	
				tr+="<td>" +getAccountInfoHoverHTML(record) +"</td>";
                tr+="<td>"+getStatusInfoHoverHTML(record)+"<br/>"+getDeviceStatusInfoHoverHTML(record)+"</td>";
				if(!isReserve){
					//入款卡TAB页签
					tr+="<td>"+getFlagMoreStrHTML(record.flagMoreStr,true,record.limitPercentage,record.minBalance)+"</td>";
				}else{
					tr+="<td>"+getFlagStr(record.flag)+"</td>";
				}
				!record.bankBalance?record.bankBalance=0:null;
				!record.balance?record.balance=0:null;
				tr+="<td><div class='BankLogEvent' target='"+record.id+"'><span name='bankBalance' class='amount'>"+record.bankBalance+htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily)+"</span><span class='time'></span></div></td>";
				tr+="<td>"+record.balance+"</td>";
				if(!isReserve){
					tr+="<td style='text-align:left !important'>";
					tr+="&nbsp;&nbsp;<a title='"+_checkObj(record.bankBalTime)+"'>银行："+hhmmss(record.bankBalTime);
					tr+="</a><br/>&nbsp;&nbsp;<a title='"+_checkObj(record.sysBalTime)+"'>系统："+hhmmss(record.sysBalTime);
					tr+="</a></td>";
				}
				//银行卡 转入记录  
				if(EncashLog){
					if(isReserve){
						record.inCountMapped=!!!record.inCount.mapped?"0":record.inCount.mapped;
						record.inCountMapping=!!!record.inCount.mapping?"0":record.inCount.mapping;
						record.inCountCancel=!!!record.inCount.cancel?"0":record.inCount.cancel;
						record.outCountMapped=!!!record.outCount.mapped?"0":record.outCount.mapped;
						record.outCountMapping=!!!record.outCount.mapping?"0":record.outCount.mapping;
						record.outCountCancel=!!!record.outCount.cancel?"0":record.outCount.cancel;
						record.incomeRequestStatusMatching=incomeRequestStatusMatching+'';
						record.incomeRequestStatusMatched=incomeRequestStatusMatched+'';
						record.incomeRequestStatusCanceled=incomeRequestStatusCanceled+'';
						record.accountStatInOutCategoryIn = accountStatInOutCategoryIn+'';
						record.accountStatInOutCategoryOutMember = accountStatInOutCategoryOutMember+'';
						record.accountStatInOutCategoryOutTranfer = accountStatInOutCategoryOutTranfer+'';
						record.inCountMappedEle=(record.inCount.mapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
						record.inCountMappingEle=(record.inCount.mapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
						record.inCountCancelEle=(record.inCount.cancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
						record.outCountMappingEle=(record.outCount.mapping!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
						record.outCountMappedEle=(record.outCount.mapped!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
						record.outCountCancelEle=(record.outCount.cancel!=0)?(' href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
						tr+='<td style="width:90px;">\
				                <a '+record.outCountMappingEle+' target="_self" accountStatInOutCategory="'+record.accountStatInOutCategoryOutMember+'" accountStatInOutId="'+record.id+'" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">'+record.outCountMapping+'</span></a>\
				                <a '+record.outCountMappedEle+' target="_self" accountStatInOutCategory="'+record.accountStatInOutCategoryOutMember+'" accountStatInOutId="'+record.id+'" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">'+record.outCountMapped+'</span></a>\
				                <a '+record.outCountCancelEle+' target="_self" accountStatInOutCategory="'+record.accountStatInOutCategoryOutMember+'" accountStatInOutId="'+record.id+'" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">'+record.outCountMapped+'</span></a>\
				                <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId='+record.id+'>'+record.matchingAmtOut+'</span>\
				            </td>';
						tr+='<td style="width:90px;">\
							 	<a '+record.inCountMappingEle+' target="_self" accountStatInOutCategory="'+record.accountStatInOutCategoryIn+'" accountStatInOutId="'+record.id+'" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">'+record.inCountMapping+'</span></a>\
							    <a '+record.inCountMappedEle+' target="_self"  accountStatInOutCategory="'+record.accountStatInOutCategoryIn+'" accountStatInOutId="'+record.id+'" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">'+record.inCountMapped+'</span></a>\
							    <a '+record.inCountCancelEle+' target="_self"  accountStatInOutCategory="'+record.accountStatInOutCategoryIn+'" accountStatInOutId="'+record.id+'" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">'+record.inCountCancel+'</span></a>\
							    <span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId='+record.id+'>'+record.matchingAmtIn+'</span>\
							 </td>';
					}else{
						tr+="<td style='text-align:left !important'>";
						tr+="&nbsp;&nbsp;转入："+getRecording_Td(record.id,tdIncomeName,null,true);
						tr+="<br/>&nbsp;&nbsp;下发："+getRecording_Td(record.id,tdName,null,true);
						tr+="</td>";
					}
				}else{
					if(isReserve){
						tr+="<td></td>";
						tr+="<td></td>";
					}else{
						tr+="<td></td>";
					}
				}
				
				//操作
				tr+="<td>";
				if(record.status!=accountStatusDelete){
					tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' " +
						"onclick='showUpdateAccountNewModal("+record.id+",showAccountList)' contentRight='IncomeAuditComp:UpdateCompBank:*'>" +
						"<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i>" +
						"<span>修改</span>" +
					"</button>";
				}
				tr+="<button class='btn btn-xs btn-white btn-success btn-bold  "+isHideAccountBtn+"' \
					onclick='showModal_accountBaseInfo("+record.id+")'>\
					<i class='ace-icon fa fa-eye bigger-100 green'></i><span>状态</span>" +
				"</button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold "+OperatorLogBtn+"' " +
						"onclick='showModal_accountExtra("+record.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
				//明细
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
						"onclick='showInOutListModal("+record.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+="</td>";
				$tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
				totalBalanceByBank+=record.bankBalance*1;
				totalBalance+=record.balance*1;
			});
			//异步刷新数据
			if(EncashLog){
				//转入 转出记录
				if(isReserve){
//					loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutMember],fromIdArray);
//					loadInOutTransfer(fromIdArray,tdIncomeName,[accountStatInOutCategoryIn],'IncomeAuditComp:EncashLog:*','toAccountId',true);
//					loadInOutTransfer(fromIdArray,tdName,[accountStatInOutCategoryOutMember],'IncomeAuditComp:EncashLog:*','fromAccountId');
					loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutMember],idArray);
				}else{
					loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutTranfer],fromIdArray);
					loadInOutTransfer(fromIdArray,tdIncomeName,[accountStatInOutCategoryIn],'IncomeAuditComp:EncashLog:*','toAccountId',true);
					loadInOutTransfer(fromIdArray,tdName,[accountStatInOutCategoryOutTranfer],'IncomeAuditComp:EncashLog:*','fromAccountId');
				}
			}
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
				totalRows[isReserve?6:8]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
				totalRows[isReserve?7:9]={subTotal:totalBalance,total:jsonObject.page.header.totalAmountBalance};
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			//分页初始化
			showPading(jsonObject.page,pageId,showAccountList,null,false,true);
			contentRight();
			SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
        }
	});
}

/**
 * 展示入款模态窗 修改入款卡账号信息
 * type: bank  wechat ali third
 */
var showModal_addIncomeAccount=function(addType){
	var addType="bank",accountTabType=$("#accountTabType").val();
	if(accountTabType==3){
		//支付宝
		addType="ali";
	}else if(accountTabType==4){
		//微信
		addType="wechat"
	}
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#addIncomeAccount_"+addType).clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			$div.find("#tableAdd td.noPaddingTop").css("padding-top","0px");
			//初始化盘口
			getHandicap_select($div.find("select[name='handicap_select']"));
			if(addType=="bank"){
				//银行类别
				getBankTyp_select($div.find("[name=choiceBankBrand]"));
			}
			$("[name=flag][value="+accountFlagPC+"]").prop("checked",true);
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
			$div.find("#do_update").unbind("click").bind("click",function(){
				do_addIncomeAccount(addType);
			});
		}
	});
}

var do_addIncomeAccount=function(addType){
	var $div=$("#addIncomeAccount_"+addType);
    var $handicap = $div.find("[name='handicap_select']");
    var $account = $div.find("input[name='account']");
    var $bankType = $div.find("select[name='choiceBankBrand']");
    var $bankName = $div.find("input[name='bankName']");
    var $balance = $div.find("input[name='balance']");
    var $owner = $div.find("input[name='owner']");
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $limitOutOne = $div.find("input[name='limitOutOne']");
	var $currSysLevel = $div.find("select[name='currSysLevel']");
	var $limitBalance = $div.find("[name='limitBalance']");
	var $remark = $div.find("[name='remark']");
	var $flag = $div.find("[name='flag']:checked");
	var $mobile = $div.find("[name='mobile']");
	 //校验非空和输入校验
    var validateEmpty=[
    	{ele:$handicap,name:'盘口'},
    	{ele:$account,name:'账号'},
    	{ele:$owner,name:'开户人'},
    	{ele:$bankType,name:'开户行 > 银行类别'},
    	{ele:$bankName,name:'开户行 > 支行'},
    	{ele:$balance,name:'余额'},
    	{ele:$flag,name:'来源'},
    	{ele:$remark,name:'备注'}
    ];
    if($flag.val()&&$flag.val()==accountFlagMobile){
    	validateEmpty.push({ele:$mobile,name:'手机号'});
    }
    var validatePrint=[
    	{ele:$account,name:'账号',maxLength:25},
    	{ele:$owner,name:'开户人',minLength:2,maxLength:10},
    	{ele:$bankName,name:'开户行 > 支行',maxLength:50},
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$limitOutOne,name:'单笔出款限额',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$balance,name:'余额',type:'amountCanZero'},
    	{ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$remark,name:'备注',minLength:5,maxLength:100}
    ];
	if(addType=="ali"||addType=="wechat"){
		validateEmpty=[
	    	{ele:$handicap,name:'盘口'},
	    	{ele:$account,name:'账号'},
	    	{ele:$owner,name:'姓名'},
	    	{ele:$balance,name:'余额'},
	    	{ele:$remark,name:'备注'}
	    ];
	    validatePrint=[
	    	{ele:$account,name:'账号',maxLength:25},
	    	{ele:$owner,name:'姓名',minLength:2,maxLength:10},
	    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
	    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
	    	{ele:$balance,name:'余额',type:'amountCanZero'},
	    	{ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:50000},
	    	{ele:$remark,name:'备注',minLength:5,maxLength:100}
	    ];
	}
   
    if(!validateEmptyBatch(validateEmpty)||
			!validateInput(validatePrint)){
    	return;
    }
    //入款银行卡默认未激活，否则新卡
    var status=($("#accountTabType").val()==accountTypeInBank?accountInactivated:accountStatusEnabled);
	bootbox.confirm("确定新增账号?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/create',
				async:false,
				data:{
					"type":$("#accountTabType").val(),
					"status":status,
					"handicapId":$.trim($handicap.val(),true),
					"account":$.trim($account.val(),true),
					"bankType":$.trim($bankType.val(),true),
					"bankName":$.trim($bankName.val(),true),
					"owner":$.trim($owner.val(),true),
					"bankBalance":$.trim($balance.val()),
					"limitIn":$.trim($limitIn.val()),
					"limitOut":$.trim($limitOut.val()),
					"limitOutOne":$.trim($limitOutOne.val()),
					"limitBalance":$.trim($limitBalance.val()),
					"currSysLevel":$.trim($currSysLevel.val()),
					"remark":$.trim($remark.val()),
					"flag":$.trim($flag.val()),
					"mobile":$.trim($mobile.val(),true)
				},
				success:function(jsonObject){
			        if(jsonObject&&jsonObject.status == 1){
    					showMessageForever(accountInactivatedAdd,"新增成功！");
			            showAccountList();
			            $div.modal("toggle");
			        }else{
			        	showMessageForFail("账号修改失败："+jsonObject.message);
			        }
			    }
			});
		}
	});
}

var saveSyncInfo=function(){
	var $div=$("#incomeInfoModal");
	var sync_type = $div.find('input[name="sync_type"]:checked').val();
	var handicap_sync = $div.find('[name=handicap_sync] option:selected').attr("code");
	if(!validateEmptyBatch([{ele:$div.find('[name=handicap_sync]'),name:'盘口'},{ele:$div.find('input[name="sync_type"]:checked'),name:'同步类型'}])){
    	return;
    }
	bootbox.confirm("确定同步?", function(result) {
		if (result) {
		    if (sync_type && sync_type.length > 0) {
		        $.ajax({
		            type: 'get',
		            url: '/r/synch/synchInfo',
		            data: {"type": sync_type, "handicap": handicap_sync},
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
		}
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
	});
}

var showModal_incomeInfo=function(){
	var $div=$("#incomeInfoModal").modal("toggle");
	//重置参数
	resetIncomeInfo();
}
var resetIncomeInfo=function(){
	var $divIncome=$("#incomeInfoModal");
	getHandicap_select($divIncome.find("select[name='handicap_sync']"),null,"---请选择---");
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
    $divIncome.find("[name=INCOME_ACCOUNTS_PERUSER]").val(sysSetting.INCOME_ACCOUNTS_PERUSER);
    $divIncome.find("[name=INCOME_LIMIT_CHECKIN_TODAY]").val(sysSetting.INCOME_LIMIT_CHECKIN_TODAY);
    $divIncome.find("[name=INCOME_PERCENT]").val(sysSetting.INCOME_PERCENT);
    $divIncome.find("[name=INCOME_BALANCE]").val(sysSetting.INCOME_BALANCE);
    $divIncome.find("[name=INCOME_MATCH_HOURS]").val(sysSetting.INCOME_MATCH_HOURS);
    $divIncome.find("[name=INCOME_LIMIT_REQUEST_CANCEL]").val(sysSetting.INCOME_LIMIT_REQUEST_CANCEL);
    $divIncome.find("[name=Income_YSF_OneDay_Limit]").val(sysSetting.Income_YSF_OneDay_Limit);
    $divIncome.find("[name=INBANK_OUT_DEFAULT_CREDIT_PERCENTAGE]").val(sysSetting.INBANK_OUT_DEFAULT_CREDIT_PERCENTAGE);
    $divIncome.find("[name=INBANK_OUT_RESERVED_FEES]").val(sysSetting.INBANK_OUT_RESERVED_FEES);
    $divIncome.find("[name=INBANK_CLOSE_OUT_DEFAULT_CREDIT_PERCENTAGE]").val(sysSetting.INBANK_CLOSE_OUT_DEFAULT_CREDIT_PERCENTAGE);
    $divIncome.find("[name=INBANK_CLOSE_OUT_LOWEST_BALANCE]").val(sysSetting.INBANK_CLOSE_OUT_LOWEST_BALANCE);
    //盘口列表 以及 入款卡&备用卡当出款卡给会员出款按钮
    var open_close_btn=sysSetting.ALLOCATE_OUTWARD_TASK_ENABLE_INBANK;
    open_close_btn=open_close_btn&&open_close_btn==1?1:0;//默认关闭
    $divIncome.find("[name=open_close_btn][value="+open_close_btn+"]").attr("checked","checked");
	var $handicap_div=$divIncome.find("#handNameList");
	var enableHandicap=sysSetting.ENABLE_INBANK_ALLOCATE_OUTWARD_HANDICAP;
	var HTML="";
	var closeBtn='';
	$.each(handicap_list_all,function(i,handicap){
		if(!handicap||handicap.status!=1||!handicap.zone){
			//无区域或未开启的盘口跳过
			return true;
		}
		if(handicap.id){
			var lable_color=' label-grey ';
			var checked='';
			if(enableHandicap&&enableHandicap.indexOf(";"+handicap.id+";")!=-1){
				//已勾选的盘口
				lable_color=' label-primary ';
				checked=' checked="checked" ';
				closeBtn='';
			}
			HTML+='<div class="col-sm-2">'+
						'<span style="margin-bottom:10px;" class="label label-xlg '+lable_color+' ">'+
							'<label style="width:110px;text-align:left;">'+
								'<input type="checkbox" '+checked+' name="checkbox_handicapName" class="ace" value="'+handicap.id+'"/>'+
								'&nbsp;<span class="lbl">'+handicap.name+'</span>'+
							'</label>'+
						'</span>'+closeBtn+
					'</div>';
		}
	});
	$handicap_div.html(HTML);
	//区域盘口列表
    var zone_manila=sysSetting.HANDICAP_MANILA_ZONE,zone_taiwan=sysSetting.HANDICAP_TAIWAN_ZONE,newIncomeFlag=sysSetting.NEW_INCOME_FLAG_HANDICAPID;
	var zone_HTML="",newIncome_HTML="";
	var closeBtn='',newIncome_closeBtn='';
	$.each(handicap_list_all,function(i,handicap){
		if(!handicap||handicap.status!=1||!handicap.zone){
			//无区域或未开启的盘口跳过
			return true;
		}
		if(handicap.id){
			//默认马尼拉地区
			var lable_color=' label-grey ';
			var checked='';
			//默认未选中
			var newIncome_lable_color=' label-grey ';
			var newIncome_checked='';
			if(zone_taiwan&&zone_taiwan.indexOf(";"+handicap.id+";")!=-1){
				//台湾
				lable_color=' label-primary ';
				checked=' checked="checked" ';
				closeBtn='';
			}
			zone_HTML+='<div class="col-sm-2">'+
						'<span style="margin-bottom:10px;" class="label label-xlg '+lable_color+' ">'+
							'<label style="width:110px;text-align:left;">'+
								'<input type="checkbox" '+checked+' name="checkbox_handicapNameZone" class="ace" value="'+handicap.id+'"/>'+
								'&nbsp;<span class="lbl">'+handicap.name+'</span>'+
							'</label>'+
						'</span>'+closeBtn+
					'</div>';
			if(newIncomeFlag&&newIncomeFlag.indexOf(";"+handicap.id+";")!=-1){
				//已开启
				newIncome_lable_color=' label-primary ';
				newIncome_checked=' checked="checked" ';
				newIncome_closeBtn='';
			}
			newIncome_HTML+='<div class="col-sm-2">'+
												'<span style="margin-bottom:10px;" class="label label-xlg '+newIncome_lable_color+' ">'+
													'<label style="width:110px;text-align:left;">'+
														'<input type="checkbox" '+newIncome_checked+' name="checkbox_newIncomeFlag" class="ace" value="'+handicap.id+'"/>'+
														'&nbsp;<span class="lbl">'+handicap.name+'</span>'+
													'</label>'+
												'</span>'+closeBtn+
											'</div>';
		}
	});
	$divIncome.find("#handNameZoneList").html(zone_HTML);
	$divIncome.find("#newIncomeFlagList").html(newIncome_HTML);
	
}
var saveIncomeInfo=function(){
	bootbox.confirm("确定修改系统设置?", function(result) {
		if (result) {
			var $divIncome=$("#incomeInfoModal");
		    var keysArray = new Array(), valsArray = new Array();
		    var validate = new Array();
		    var $INCOME_ACCOUNTS_PERUSER = $divIncome.find("[name=INCOME_ACCOUNTS_PERUSER]");
		    var $INCOME_LIMIT_CHECKIN_TODAY = $divIncome.find("[name=INCOME_LIMIT_CHECKIN_TODAY]");
		    var $INCOME_PERCENT = $divIncome.find("[name=INCOME_PERCENT]");
		    var INCOME_BALANCE = $divIncome.find("[name=INCOME_BALANCE]");
		    var $INCOME_MATCH_HOURS = $divIncome.find("[name=INCOME_MATCH_HOURS]");
		    var $INCOME_LIMIT_REQUEST_CANCEL = $divIncome.find("[name=INCOME_LIMIT_REQUEST_CANCEL]");
		    var $Income_YSF_OneDay_Limit = $divIncome.find("[name=Income_YSF_OneDay_Limit]");
		    var $INBANK_OUT_DEFAULT_CREDIT_PERCENTAGE = $divIncome.find("[name=INBANK_OUT_DEFAULT_CREDIT_PERCENTAGE]");
		    var $INBANK_OUT_RESERVED_FEES = $divIncome.find("[name=INBANK_OUT_RESERVED_FEES]");
		    var $INBANK_CLOSE_OUT_DEFAULT_CREDIT_PERCENTAGE = $divIncome.find("[name=INBANK_CLOSE_OUT_DEFAULT_CREDIT_PERCENTAGE]");
		    var $INBANK_CLOSE_OUT_LOWEST_BALANCE = $divIncome.find("[name=INBANK_CLOSE_OUT_LOWEST_BALANCE]");
		    validate.push({ele: $INCOME_ACCOUNTS_PERUSER,  name: '入款审核，分配账号最大数',type: 'positiveInt',maxLength: 5, minEQ: 1, maxEQ: 300});
		    validate.push({ele: $INCOME_LIMIT_CHECKIN_TODAY, name: '当日入款最大限额', type: 'amountPlus'});
		    validate.push({ele: $INCOME_PERCENT, name: '银行流水匹配浮动比例', type: 'amountCanZero', min: 0, maxEQ: 2});
		    validate.push({ele: INCOME_BALANCE, name: '入款人工匹配差额', type: 'amountCanZero', min: 0, maxEQ: 100});
		    validate.push({ele: $INCOME_MATCH_HOURS, name: '入款与流水记录匹配最大时间间隔', type: 'amountCanZero', minEQ: 1, maxEQ: 24});
		    validate.push({ele: $INBANK_OUT_DEFAULT_CREDIT_PERCENTAGE, name: '专注入款卡余额大于或等于信用额度的百分比', type: 'amountCanZero', minEQ: 1, maxEQ: 100});
		    validate.push({ele: $INBANK_OUT_RESERVED_FEES, name: '专注入款卡保留余额', type: 'amountCanZero', minEQ: 1, maxEQ: 10000});
		    validate.push({ele: $INBANK_CLOSE_OUT_DEFAULT_CREDIT_PERCENTAGE, name: '先入后出卡转出款卡信用额度百分比默认值', type: 'amountCanZero', minEQ: 1, maxEQ: 100});
		    validate.push({ele: $INBANK_CLOSE_OUT_LOWEST_BALANCE, name: '先入后出卡低于保留余额转为入款卡', type: 'amountCanZero', minEQ: 1, maxEQ: 10000});
		    validate.push({ele: $INCOME_LIMIT_REQUEST_CANCEL,  name: '入款与流水记录匹配最大时间间隔',  type: 'amountCanZero', minEQ: 1, maxEQ: 24});
		    validate.push({ele: $Income_YSF_OneDay_Limit,  name: '云闪付（边入边出）卡每日入款限额',  type: 'amountPlus'});
		    $.each($divIncome.find("input"), function (index, result) {
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
		                $divIncome.modal("toggle");
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
var saveAccount4Out=function(){
	bootbox.confirm("<span class='red bolder'>请确认【入款卡PC与手机的工作模式】是否已设置好。</span>确定保存？", function(result) {
		if (result) {
			var $divIncome=$("#incomeInfoModal");
			var enable_handicap=new Array();
			$.each($divIncome.find("[name=checkbox_handicapName]:checked"),function(i,result){
				enable_handicap.push($(result).val());
			});
			var keysArray = new Array(), valsArray = new Array();
			//可用盘口
			keysArray.push("ENABLE_INBANK_ALLOCATE_OUTWARD_HANDICAP");
			if(enable_handicap&&enable_handicap.length>0&&enable_handicap[0]){
				valsArray.push(";"+enable_handicap.join(";")+";");
			}else{
				valsArray.push("");
			}
			//是否开启
			var open_close_btn = $divIncome.find('input[name="open_close_btn"]:checked').val();
			keysArray.push("ALLOCATE_OUTWARD_TASK_ENABLE_INBANK");
			if(open_close_btn=="0"||open_close_btn=="1"){
				valsArray.push(open_close_btn);
			}else{
				//默认关闭
				valsArray.push("0");
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
						$divIncome.modal("toggle");
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
var saveHandicapZone=function(){
	bootbox.confirm("<span class='red bolder'>确定保存盘口区域信息？", function(result) {
		if (result) {
			var $divIncome=$("#incomeInfoModal");
			var manila_handicap=new Array(),taiwan_handicap=new Array();
			$.each($divIncome.find("[name=checkbox_handicapNameZone]"),function(i,result){
				if($(result).is(":checked")){
					taiwan_handicap.push($(result).val());
				}else{
					manila_handicap.push($(result).val());
				}
			});
			var newIncomeFlag_handicap=new Array();
			$.each($divIncome.find("[name=checkbox_newIncomeFlag]"),function(i,result){
				if($(result).is(":checked")){
					newIncomeFlag_handicap.push($(result).val());
				}
			});
			var keysArray = new Array(), valsArray = new Array();
			//可用盘口
			keysArray.push("HANDICAP_MANILA_ZONE");
			keysArray.push("HANDICAP_TAIWAN_ZONE");
			keysArray.push("NEW_INCOME_FLAG_HANDICAPID");
			if(manila_handicap&&manila_handicap.length>0&&manila_handicap[0]){
				valsArray.push(";"+manila_handicap.join(";")+";");
			}else{
				valsArray.push("");
			}
			if(taiwan_handicap&&taiwan_handicap.length>0&&taiwan_handicap[0]){
				valsArray.push(";"+taiwan_handicap.join(";")+";");
			}else{
				valsArray.push("");
			}
			if(newIncomeFlag_handicap&&newIncomeFlag_handicap.length>0&&newIncomeFlag_handicap[0]){
				valsArray.push(";"+newIncomeFlag_handicap.join(";")+";");
			}else{
				valsArray.push("");
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
						$divIncome.modal("toggle");
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
function updAccount(id){
    showUpdateOutAccount(id ,function(){
        showAccountList();
    });
}

var saveReservedInfo=function(){
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

var showModal_reserveInfo=function(){
    var $div=$("#reserveInfoModal").modal("toggle");
    //重置参数
    showAccountList();
}
function addAccount(){
    showAddOutAccount(accountTypeReserveBank,accountInactivated ,function(){
    	showAccountList();
    });
}
//新增入款卡
function addInAccount() {
    showAddOutAccount(accountTypeInBank,accountInactivated ,function(){
        showAccountList();
    });
}
/** 出入款额度设置 */
var showModal_set_inOut_limit=function(){
	var dataLength=$("[name=checkbox_limit]:checked").length;
	if(dataLength<1){
		showMessageForCheck("请至少选择一行数据");
		 return;
	}
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#InOutLimitModal").clone().appendTo($("body"));
			$div.modal("toggle");
			var flagList=new Array();
			var trs="";
			$.each($("[name=checkbox_limit]:checked"),function(i,result){
				var accountInfo=getAccountInfoById($(result).val());
				if(accountInfo){
					flagList.push({id:accountInfo.id,flagMoreStr:accountInfo.flagMoreStr});
				}
				trs += "<tr trId='"+accountInfo.id+"'>";
				trs += "<td><span>"+_checkObj(accountInfo.bankType)+"</span></td>";
				trs += "<td><span>"+hideAccountAll(accountInfo.account)+"</span></td>";
				trs += "<td><span>"+hideName(accountInfo.owner)+"</span></td>";
				trs += "<td><span>"+_checkObj(accountInfo.peakBalance)+"</span></td>";
				trs += "<td><span>"+getFlagMoreStrHTML(accountInfo.flagMoreStr,true,accountInfo.limitPercentage,accountInfo.minBalance)+"</span></td>";
				//选择
				trs += "<td>";
				trs += '<label class="inline">\
								<input type="radio" name="outEnable_checkbox'+accountInfo.id+'" class="ace" disabled value="0" >\
								<span class="lbl">\
									大额专用<br>\
									<span class="red bolder" style="font-size:10px;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(返利网)</span>\
								</span>\
							</label><label class="inline">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>';	
				trs += '<label class="inline">\
								<input type="radio" name="outEnable_checkbox'+accountInfo.id+'" class="ace" value="1" >\
								<span class="lbl">\
									先入后出<br>\
									<span class="red bolder" style="font-size:10px;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(返利网)</span>\
								</span><label class="inline">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>\
							</label>';
				trs += '<label class="inline">\
								<input type="radio" name="outEnable_checkbox'+accountInfo.id+'" class="ace" value="2" >\
								<span class="lbl">\
									边入边出<br>\
									<span class="red bolder" style="font-size:10px;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(返利网)</span>\
								</span>\
							</label>';
				trs += "</td>";
				trs += "</tr>";
			});
			$div.find("table[name=selected_cards] tbody").html(trs);
			//循环加载当前的选项卡
			loadCurrFlag(flagList);
		    $div.on('hidden.bs.modal', function () {
		        //关闭窗口清除内容;
		        $div.remove();
		    });
		}
	});
}
var loadCurrFlag=function(flagList){
	$.each(flagList,function(index,record){
		var checkedValue=""
		if(record.flagMoreStr==1){
			//边入边出
			checkedValue="2";
		}else if(record.flagMoreStr==3){
			//大额专用（返利网）3
			checkedValue="0";
		}else if(record.flagMoreStr==4||record.flagMoreStr==5){
			//先入后出（正在出）4；先入后出（正在入）5
			checkedValue="1";
		}
		$("[name=outEnable_checkbox"+record.id+"][value='"+checkedValue+"']").prop("checked",true);
	});
	
}
var do_set_inOut_limit=function(){
	var $div=$("#InOutLimitModal");
	var param=new Array();
	//大额专用(返利网)的参数 has0:是否有勾选大额专用的银行卡
	var limitPercentage0=$div.find("[name=limitPercentage0]").val(), minBalance0=$div.find("[name=minBalance0]").val(),has0=false;
	//先入后出(返利网)的参数 has1:是否有勾选边入边出的银行卡
	var limitPercentage1=$div.find("[name=limitPercentage1]").val(), minBalance1=$div.find("[name=minBalance1]").val(),has1=false;
	$div.find("[name=selected_cards] tr").map(function(i,tr){
		var accountId=$(tr).attr("trId");
		var type=$(tr).find("[name=outEnable_checkbox"+accountId+"]:checked").val();
		//只对选择了类型的银行卡行进行校验以及设置
		if(type){
			var record={
					"bankId":accountId,
					"outEnable":type
			}
			if(type==0){//大额专用(返利网)
				has0=true;
				record.limitPercentage=limitPercentage0;
				record.minBalance=minBalance0;
			}else if(type==1){//先入后出(返利网)
				has1=true;
				record.limitPercentage=limitPercentage1;
				record.minBalance=minBalance1;
			}else{//边入边出
				//暂无参数
			}
			param.push(record);
		}
	});
	if(!param||param.length<1){
		showMessageForFail("请设置至少一行数据的用途");
		return;
	}
	var validatePrint=[];
	if(has0){
		showMessageForFail("请修改“大额专用(返利网)”用途的账号为其它用途");
		return;
	}
	if(has1){
		validatePrint.push({ele:$div.find("[name=limitPercentage1]"),name:'已设置了“先入后出(返利网)”用途的银行卡，百分比',minEQ:1,maxEQ:100});
		validatePrint.push({ele:$div.find("[name=minBalance1]"),name:'已设置了“先入后出(返利网)”用途的银行卡，保留余额',minEQ:1,maxEQ:10000});
	}
	if(!validateEmptyBatch(validatePrint)||!validateInput(validatePrint)){
	    	return;
	 }
	 bootbox.confirm({title:"确认",message:"请再次确认", callback: function(result) {
		if (result) {
			$.ajax({
		        type: "POST",
		        url: '/r/account/batchUpdateLimit',
		        dataType: 'JSON',
		        async: false,
		        data:{"param":JSON.stringify(param) },
		        success: function (jsonObject) {
		        	if(jsonObject.status ==1){
						showAccountList();
						if(jsonObject.data&&jsonObject.data.failed&&jsonObject.data.failed.length>0){
							//选中的数据有未更新成功的
							var trs="";
							$.map(jsonObject.data.failed,function(result){
								var accountInfo=getAccountInfoById(result);
								trs += "<tr>";
								trs += "<td><span>"+_checkObj(accountInfo.bankType)+"</span></td>";
								trs += "<td><span>"+hideAccountAll(accountInfo.account)+"</span></td>";
								trs += "<td><span>"+hideName(accountInfo.owner)+"</span></td>";
								trs += "<td><span>"+_checkObj(accountInfo.peakBalance)+"</span></td>";
								trs += "</tr>";
							});
							var failedTips='<table name="selected_cards" class="table table-bordered  no-margin-bottom">\
													<thead>\
														<tr>\
															<th style="width:25%;">银行类别</th>\
															<th style="width:25%;">账号</th>\
															<th style="width:25%;">开户人</th>\
															<th style="width:25%;">信用额度</th>\
														</tr>\
													</thead>\
													<tbody>'+trs+'</tbody>\
												</table>';
							showMessageForFail('以下数据更新失败：<span class="red bolder"><i class="ace-icon fa fa-info-circle bigger-110 red"></i>&nbsp;仅绑定了云闪付的卡，才会成功转为边入边出卡</span><br/>'+failedTips);
						}else{
							showMessageForSuccess("更新成功");
							$div.modal("toggle");
						}
					}else{
						showMessageForFail("更新失败，"+jsonObject.message);
					}
		        }
			});
		}
	 }});
}

getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #searchBtn"),80,"refresh_accountIncomp");
changeTabInit(accountTypeInBank);
//如果没有入款银行卡的权限，有备用卡权限，默认查备用卡
var haveIncomePer=false;
$.each(ContentRight['IncomeAccountComp:*'], function (name, value) {
    if (name == 'IncomeAccountComp:IncomeTab:*') {
    	haveIncomePer=true;
    }
});
if(!haveIncomePer){
	$.each(ContentRight['IncomeAccountComp:*'], function (name, value) {
		if (name == 'IncomeAccountComp:ReservedTab:*') {
			$("#reservedTab").addClass("active");
			$("#tab8").addClass("in active");
	    	changeTabInit(8);
		}
	});
}

$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		$("#accountFilter #searchBtn button").click();
	}
});

