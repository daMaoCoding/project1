currentPageLocation = window.location.href;
var CommonModel={
	li4InBank:'<li onclick="changeTransferTabInit({accountTypeReserveBank})" class="active"><a data-toggle="tab" href="#tab5">备用卡</a></li>',
	li4Reserved:'<li onclick="changeTransferTabInit({accountTypeOutBank})" class="active"><a data-toggle="tab" href="#tab5">出款银行卡</a></li>',
	li4Outward:	'<li onclick="changeTransferTabInit({accountTypeReserveBank})" class="active"><a data-toggle="tab" href="#tab5">备用金</a></li>\
				<li onclick="changeTransferTabInit({accountTypeCashBank})"><a data-toggle="tab" href="#tab5">现金卡</a></li>',
	li4Issue:	'<li onclick="changeTransferTabInit({accountTypeOutBank})" class="active"><a data-toggle="tab" href="#tab5">出款银行卡</a></li>\
				<li onclick="changeTransferTabInit({accountTypeOutThird})"><a data-toggle="tab" href="#tab5">出款第三方</a></li>'
}
var CommonData={
	accountTypeReserveBank:accountTypeReserveBank,
	accountTypeOutBank:accountTypeOutBank,
	accountTypeOutThird:accountTypeOutThird,
	accountTypeInBank:accountTypeInBank,
	accountTypeInWechat:accountTypeInWechat,
	accountTypeInAli:accountTypeInAli,
	accountTypeInThird:accountTypeInThird,
	accountTypeReserveBank:accountTypeReserveBank,
	accountTypeCashBank:accountTypeCashBank,
	defaultToAccountType4Reserved:accountTypeOutBank,
	defaultToAccountType4Outward:accountTypeReserveBank,
	defaultToAccountType4Issue:accountTypeOutBank,
	defaultToAccountType4InBank:accountTypeReserveBank
};
var fromAccount = null;

var showTransferBalanceModal=function(accountId,callBack4Close){
	fromAccount =  getAccountInfoById(accountId);
	fromAccount.nbsp="&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
	if(fromAccount.type==accountTypeBindWechat||fromAccount.type==accountTypeBindAli||fromAccount.type==accountTypeThirdCommon||fromAccount.type==accountTypeBindCommon){
		fromAccount.liHtml4ToAccountType=fillDataToModel4Item(CommonData,CommonModel.li4Issue);
		fromAccount.defaultToAccountType=CommonData.defaultToAccountType4Issue;
	}else if(fromAccount.type==accountTypeOutBank||fromAccount.type==accountTypeOutThird){
		fromAccount.liHtml4ToAccountType=fillDataToModel4Item(CommonData,CommonModel.li4Outward);
		fromAccount.defaultToAccountType=CommonData.defaultToAccountType4Outward;
	}else if(fromAccount.type==accountTypeReserveBank){
		fromAccount.liHtml4ToAccountType=fillDataToModel4Item(CommonData,CommonModel.li4Reserved);
		fromAccount.defaultToAccountType=CommonData.defaultToAccountType4Reserved;
	}else if(fromAccount.type==accountTypeInBank||fromAccount.type==accountTypeBindCustomer){
		fromAccount.liHtml4ToAccountType=fillDataToModel4Item(CommonData,CommonModel.li4InBank);
		fromAccount.defaultToAccountType=CommonData.defaultToAccountType4InBank;
	}
	$("body").find("#exchangeBalanceModal").remove();
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({type:"GET", dataType:'html',url:"/"+sysVersoin+"/html/common/showInfoModal.html",success:function(html){
		var  modalHtml =fillDataToModel4Item(fromAccount,$(html).find("#exchangeBalanceModal").html());
		var $div=$("<div id=\"exchangeBalanceModal\" class=\"modal fade\">"+modalHtml+"</div>").appendTo($("body"));
		getBankTyp_select($div.find("select[name='search_LIKE_bankType']"),null,"全部")
		fromAccount.bankBalance=fromAccount.bankBalance?fromAccount.bankBalance:'0';
		fromAccount.balance=fromAccount.balance?fromAccount.balance:'0';
		$div.find("div.fromAccountInfoInFooter").html(fillDataToModel4Item(fromAccount,"账号: {account}{nbsp}开户行: {bankName}{nbsp}开户人: {owner}{nbsp}系统余额: {balance}{nbsp}银行余额: {bankBalance}"));
		showExchangeBalance(0);
		$div.modal("toggle");
		if(callBack4Close){
			$div.on('hide.bs.modal', function (){callBack4Close();})
		}
	}});
}

var showExchangeBalance=function(CurPage){
	var $div = $('#exchangeBalanceModal');
	if(!!!CurPage) CurPage=$("#exchangeBalancePage .Current_Page").text();
	var toAccountType=$div.find("input#toAccountType").val();
	var search_LIKE_account = $div.find("input[name='search_LIKE_account']").val();
	search_LIKE_account = search_LIKE_account?search_LIKE_account.replace(/(^\s*)|(\s*$)/g, ""):"";
	var search_LIKE_owner = $div.find("input[name='search_LIKE_owner']").val();
	search_LIKE_owner = search_LIKE_owner?search_LIKE_owner.replace(/(^\s*)|(\s*$)/g, ""):"";
	var lockToArray = new Array();
	$div.find("input[name='search_IN_lockStatus']:checked").each(function(){
		lockToArray.push(this.value);
	});
	var statusToArray=[accountStatusNormal].toString();
	var $tbody =$div.find("table[id='exchangeBalanceTable'] tbody").html("");
	if(accountTypeOutThird==toAccountType){
		$div.find("table[id='exchangeBalanceTable'] .third_hide").hide();
	}else{
		$div.find("table[id='exchangeBalanceTable'] .third_hide").show();
	}
	var fromAccount =  getAccountInfoById($div.find("#fromAccountId").val());
	$.ajax({
		dataType:'json',
		type:"get",
		async:false,
		url:API.r_account_list4Trans,
		data:{
			"pageNo":CurPage<=0?0:CurPage-1,
			"pageSize":$.session.get('initPageSize'), 
			"search_LIKE_account":search_LIKE_account, 
			"search_LIKE_owner":search_LIKE_owner, 
			"type":toAccountType, 
			"statusToArray":statusToArray, 
			"lockToArray":lockToArray,
			"bankType" : $.trim($div.find("[name='search_LIKE_bankType']").val()),
			"fromId":fromAccount.id
		},
		success:function(jsonObject){
			if(jsonObject.status==-1){
				showMessageForFail("查询失败："+jsonObject.message);return;
			}
			var idList=new Array();
			$.each(jsonObject.data,function(index,record){
				var array = new Array();
				record.limitIn=(!!!record.limitIn)?eval(sysSetting.INCOME_LIMIT_CHECKIN_TODAY):record.limitIn;
				var htmlExceedInLimit =(record.incomeAmountDaily- record.limitIn )> 0 ? "<i class=\"fa fa-flag red bigger-130\"></i>":"";
				//当前账号余额告警值（如果未配置，则取系统设置）-系统余额=可转入金额
				var lastAmount=record.limitBalance?record.limitBalance:sysSetting.FINANCE_ACCOUNT_BALANCE_ALARM-record.balance;
				//可转入金额对比当前账号余额
				var value= record.transInt+record.transRadix;
				var classHidden =record.lockByOperator!=1?"hidden":"";
				if(accountTypeOutThird!=toAccountType){
					array.push("<td><span>"+(record.alias?record.alias:'')+(record.currSysLevelName?('-'+record.currSysLevelName):'')+"</span></td>");
					idList.push({'id':record.id});
				}else{
					idList.push({'id':record.id,'type':'third'});
				}
				var bankType=record.bankType?record.bankType+"|":"";
				array.push("<td><span><a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(record.account)+"</a></span><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-text='"+record.account+"'></i></td>");
				array.push("<td><span>"+bankType+record.bankName+"</span><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-text='"+bankType+record.bankName+"'></i></td>");
				array.push("<td><span>"+record.owner+"</span><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-text='"+record.owner+"'></i></td>");
				array.push("<td><span>"+(record.incomeAmountDaily?record.incomeAmountDaily:0)+"</span>"+htmlExceedInLimit+"</td>");
				array.push("<td><span>"+(record.bankBalance?record.bankBalance:0)+getlimitBalanceIconStr(record)+"</span></td>");
				array.push("<td><input style='width:70px;height:23px;'class='"+classHidden+" amount"+record.id+"' type='text' name='amount' value='"+value+"' transRadix='"+record.transRadix +"' transInt='"+record.transInt+"' id='amount"+record.id+"'><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-action=\"copy\" data-clipboard-target=\"#amount"+record.id+"\"></i></td>");
				array.push("<td><input style='width:40px;height:23px;'class='"+classHidden+"' type='text' name='fee"+record.id+"' id='fee"+record.id+"'><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-action=\"copy\" data-clipboard-target=\"#fee"+record.id+"\"></i></td>");
				if(record.lockByOperator==1){
					array.push("<td><button class='btn btn-xs btn-white btn-warning btn-bold green' onclick='doLockOrUnlock("+fromAccount.id+","+record.id+",0)'><i class='ace-icon fa fa-unlock bigger-100 orange'></i><span>解锁</span></button></td>");
					array.push("<td><button class='btn btn-xs btn-white btn-warning btn-bold green' onclick='doExchangeBalance("+record.id+",\""+record.account+"\",this)'><i class='ace-icon fa fa-exchange bigger-100 orange'></i><span>完成转账</span></button></td>");
				}else{
					array.push("<td><button type='button' class='btn btn-xs btn-white btn-warning btn-bold green' onclick='doLockOrUnlock("+fromAccount.id+","+record.id+",1)'><i class='ace-icon fa fa-lock bigger-100 orange'></i><span>锁定</span></button></td>");
					array.push("<td></td>");
				}
				$("<tr>"+array.join('')+"</tr>").appendTo($tbody);
			});
			var footTitle ="";
			if(fromAccount){
				fromAccount.nbsp="   ";
				fromAccount.bankBalance=fromAccount.bankBalance?fromAccount.bankBalance:'0';
				fromAccount.balance=fromAccount.balance?fromAccount.balance:'0';
				fromAccount.hideAccount=hideAccountAll(fromAccount.account);
				footTitle=fillDataToModel4Item(fromAccount,"汇款账号：{hideAccount}；开户行：{bankName}；	开户人：{owner}；银行余额：{bankBalance}");
			}
			showPading(jsonObject.page,"exchangeBalancePage",showExchangeBalance,footTitle,true);//刷新
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			$('#exchangeBalanceModal').bootstrapValidator({
				fields: { amount: {validators: { callback: { callback: function(){return true;}}}} }
			}).on('success.field.bv', function(e) {
				var t = $(e.target);
				var transRadix = parseFloat(t.attr('transRadix'));
				var transInt = parseInt(t.attr('transInt'));
				var val = isNaN(t.val());
				var ret = val?(transInt+transRadix):(val+transRadix);
				t.val(ret);
			});
		}
	});
}

var doExchangeBalance=function(toAccountId,toAccount){
	var $div=$("#exchangeBalanceModal");
	var fromAccountId=$div.find("#fromAccountId").val();
	var amount=$div.find("td input.amount"+toAccountId).val();
	var fee=$div.find("td input[name='fee"+toAccountId+"']").val();
	var fromAccountType=$div.find("#fromAccountType").val(),incomeType;
	//根据类型指定下发类型
	if(fromAccountType==accountTypeInBank){
		incomeType=incomeRequestTypeIssueCompBank;
	}else if(fromAccountType==accountTypeInThird){
		incomeType=incomeRequestTypeWithdrawThird;
	}else if(fromAccountType==accountTypeInAli){
		incomeType=incomeRequestTypeWithdrawAli;
	}else if(fromAccountType==accountTypeInWechat){
		incomeType=incomeRequestTypeWithdrawWechat;
	}else if(fromAccountType==accountTypeBindWechat){
		incomeType=incomeRequestTypeIssueWechat;
	}else if(fromAccountType==accountTypeBindAli){
		incomeType=incomeRequestTypeIssueAli;
	}else if(fromAccountType==accountTypeThirdCommon){
		incomeType=incomeRequestTypeIssueComnBank;
	}else if(fromAccountType==accountTypeBindCommon){
		incomeType=incomeRequestTypeIssueComnBank;
	}else if(fromAccountType==accountTypeReserveBank){
		incomeType=incomeRequestTypeReserveToOutBank;
	}else if(fromAccountType == accountTypeOutBank){
		incomeType = incomeRequestTypeTransferOutBank;
	}else if(fromAccountType == accountTypeOutThird){
		incomeType = incomeRequestTypeTransferOutThird;
	}else if(fromAccountType == accountTypeBindCustomer){
		incomeType = incomeRequestTypeCustomerToReserved;
	}
	if(!!!fromAccountId){
		showMessageForFail("请先选择一个出款账号！");return;
	}
	if(!!!amount||isNaN(amount)||amount<=0){
		showMessageForFail("转账金额为空或输入有误！");return;
	}
	if(fee&&(isNaN(fee)||fee<=0)){
		showMessageForFail("手续费为空或输入有误！");return;
	}
//	bootbox.dialog({
//		message: "<span class='bigger-80'>汇入账号："+toAccount+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;金额："+amount+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;手续费："+fee+"</span>",
//		buttons:{"click" :{"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
//					var tmp = new Date().getTime();
//			        var data={handicap:'0',type:incomeType,amount:amount,remark:'',orderNo:tmp,usercode:0,createTime:tmp,toAccount:toAccount,username:'',token:'',realname:'',level:'0',ackTime:'',fromAccount:fromAccount.account,operator:getCookie('JUSERID'),fee:fee, fromId:fromAccountId,toId:toAccountId};
//					$.ajax({dataType:'JSON',contentType: "application/json" ,type:"PUT",async:false,url:API.api_income_put,data:JSON.stringify(data), success:function(jsonObject){
//						if(jsonObject.status==-1){
//							showMessageForFail("操作失败："+jsonObject.message);return
//						};
//						showExchangeBalance();
//						showMessageForSuccess("转账成功");
//						//刷新余额
//						var fromAccount=getAccountInfoById(fromAccountId);
//						if(fromAccount){
//							fromAccount.nbsp="&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
//							$div.find(".fromAccountInfoInFooter").html(fillDataToModel4Item(fromAccount,"账号: {account}{nbsp}开户行: {bankName}{nbsp}开户人: {owner}{nbsp}系统余额: {balance}{nbsp}银行余额: {bankBalance}"));
//						}
//					}});
//				}},
//	     		"click1":{"label":"取消","className":"btn-sm btn-default"}
//		}
//	});
	bootbox.confirm("<span class='bigger-80'>汇入账号："+toAccount+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;金额："+amount+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;手续费："+fee+"</span>",function(result){
		if(result){
			var tmp = new Date().getTime();
	        var data={handicap:'0',type:incomeType,amount:amount,remark:'',orderNo:tmp,usercode:0,createTime:tmp,toAccount:toAccount,username:'',token:'',realname:'',level:'0',ackTime:'',fromAccount:fromAccount.account,operator:getCookie('JUSERID'),fee:fee, fromId:fromAccountId,toId:toAccountId};
			$.ajax({dataType:'JSON',contentType: "application/json" ,type:"PUT",async:false,url:API.api_income_put,data:JSON.stringify(data), success:function(jsonObject){
				if(jsonObject.status==-1){
					showMessageForFail("操作失败："+jsonObject.message);return
				};
				showExchangeBalance();
				showMessageForSuccess("转账成功");
				//完成转账后解锁
				doLockOrUnlock(fromAccountId,toAccountId,0);
				//刷新余额
				var fromAccount=getAccountInfoById(fromAccountId);
				if(fromAccount){
					fromAccount.nbsp="&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
					$div.find(".fromAccountInfoInFooter").html(fillDataToModel4Item(fromAccount,"账号: {account}{nbsp}开户行: {bankName}{nbsp}开户人: {owner}{nbsp}系统余额: {balance}{nbsp}银行余额: {bankBalance}"));
				}
			}});
		}

		setTimeout(function(){       
	        $('body').addClass('modal-open');
	    },500);
	})
}

var doLockOrUnlock=function(fromId,toId,isLockOrUnlock){
	$.ajax({dataType:'json',type:"PUT", async:false, url:API.r_account_lockOrUnlock, data:{lock1OrUnlock0:isLockOrUnlock,fromId:fromId, toId:toId}, success:function(jsonObject){
		if(jsonObject.status==-1){
			showMessageForFail("操作失败："+jsonObject.message);return;
		}
		showExchangeBalance();
	}});
}

var changeTransferTabInit = function(toAccountType){
	$("body").find("#exchangeBalanceModal").find("input[id='toAccountType']").val(toAccountType);
	showExchangeBalance(0);
}