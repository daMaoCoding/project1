currentPageLocation = window.location.href;

var fromAccount = null;


var noAliHtml =     '<div class="noDataTipsPage table table-striped table-bordered table-hover no-margin-bottom no-border-top">\
		                <div style="margin-bottom:0;font-size:20px;" class="alert alert-success center">未绑定</div>\
	                  </div>';

var noWecHtml =     '<div class="noDataTipsPage table table-striped table-bordered table-hover no-margin-bottom no-border-top">\
		                <div style="margin-bottom:0;font-size:20px;" class="alert alert-success center">未绑定</div>\
	                  </div>';

var noBankHtml =     '<div class="noDataTipsPage table table-striped table-bordered table-hover no-margin-bottom no-border-top">\
		                <div style="margin-bottom:0;font-size:20px;" class="alert alert-success center">未绑定</div>\
	                  </div>';

var fmtDataHtml = '<table class="table table-striped table-bordered table-hover no-margin-bottom">\
	          		 <thead>\
						<tr>\
							<th style="width:120px;" class="third_hide">编号</th>\
							<th style="width:220px;">提现账号</th>\
							<th style="width:190px;">开户行</th>\
							<th style="width:85px;">开户人</th>\
							<th style="width:80px;">当日收款</th>\
							<th style="width:130px;">银行余额</th>\
							<th style="width:120px;">金额</th>\
							<th style="width:95px;">确认</th>\
						</tr>\
					 </thead>\
					 <tbody></tbody>\
				  </table>';

var showWithdrawCashModal = function(mobileId,mobile,alipay,weichat,account,handicap,callBack4Close){
	var data = {mobileId:mobileId,
		        mobile:mobile,
		        alipay:alipay?alipay:'',
				weichat:weichat?weichat:'',
				account:account?account:'',
		        accountTypeBindCustomer:accountTypeBindCustomer,
		        accountTypeInWechat:accountTypeInWechat,
		        accountTypeInAli:accountTypeInAli,
		        alipayFmt:((alipay&&account)?fmtDataHtml:(!alipay?noAliHtml:noBankHtml)),
		        wechatFmt:((weichat&&account)?fmtDataHtml:(!weichat?noWecHtml:noBankHtml)),
		        bankFmt:((account)?fmtDataHtml:noBankHtml)
	};
	$("body").find("#withdrawCashModal").remove();
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({type:"GET", dataType:'html',url:"/"+sysVersoin+"/html/common/showInfoModal.html",success:function(html){
		var  modalHtml =fillDataToModel4Item(data,$(html).find("#withdrawCashModal").html());
		var $div=$("<div id=\"withdrawCashModal\" class=\"modal fade\">"+modalHtml+"</div>").appendTo($("body"));
        account||$div.find("#tab"+accountTypeBindCustomer).html(noBankHtml);
		showWithdrawCash(0);
		$div.modal("toggle");
		if(callBack4Close){
			$div.on('hide.bs.modal', function (){callBack4Close();})
		}
	}});
};

var showWithdrawCash=function(CurPage){
	var $div = $('#withdrawCashModal');
	if(!CurPage) CurPage = $div.find(".Current_Page").text();
	var frAccType = $div.find('input[name=frAccType]').val();
	var alipay = $div.find('input[name=alipay]').val();
	var weichat = $div.find('input[name=weichat]').val();
	var account = $div.find('input[name=account]').val();
	var mobile = $div.find('input[name=mobile]').val();
	var search_LIKE_account = $div.find("input[name='search_LIKE_account']").val();
	search_LIKE_account = search_LIKE_account?search_LIKE_account.replace(/(^\s*)|(\s*$)/g, ""):"";
	var search_LIKE_owner = $div.find("input[name='search_LIKE_owner']").val();
	search_LIKE_owner = search_LIKE_owner ? search_LIKE_owner.replace(/(^\s*)|(\s*$)/g, ""):"";
	var lockToArray = [];
	$div.find("input[name='search_IN_lockStatus']:checked").each(function(){
		lockToArray.push(this.value);
	});
	var $tbody = $div.find("div#tab"+frAccType+" table tbody").html('');
	var data = {};
	var url = API.r_account_list;
	var ALI_WEC = frAccType == accountTypeInAli || frAccType == accountTypeInWechat;
	if(!account){
		return;
	}
	if(ALI_WEC){
		data.account = account;
		url ='/r/mobile/cloud/get';
		data.mobile = mobile;
	}else{
		data.typeToArray = [accountTypeReserveBank,accountTypeOutBank].toString();
		data.search_IN_handicapId=handicapId_list.toString();
		data.pageNo = CurPage<=0?0:CurPage-1;
		data.bankType = null;
		data.statusToArray = [accountStatusNormal].toString();
		data.pageSize = $.session.get('initPageSize');
		data.search_LIKE_account = search_LIKE_account;
		data.search_LIKE_owner = search_LIKE_owner;
		data.bankType = $.trim($div.find("input[name='search_LIKE_bankType']").val());
		data.search_EQ_type = $div.find("select[name='search_EQ_type']").val();
	}
	$.ajax({dataType:'json',type:"get",async:false,url:url,data:data,
		success:function(jsonObject){
			if(jsonObject.status != 1){
				showMessageForFail("查询失败："+jsonObject.message);return;
			}
			if(ALI_WEC){
				jsonObject.data.bankEntity.bankBalance =jsonObject.data.bankBalance;
				jsonObject.data = [jsonObject.data.bankEntity];
			}
			var idList= [];
			$.each(jsonObject.data,function(index,record){
				record.lockByOperator=1;
				var array = [];
				record.limitIn=(!record.limitIn)?eval(sysSetting.INCOME_LIMIT_CHECKIN_TODAY):record.limitIn;
				var htmlExceedInLimit =(record.incomeAmountDaily- record.limitIn )> 0 ? "<i class=\"fa fa-flag red bigger-130\"></i>":"";
				//可转入金额对比当前账号余额
				var classHidden =record.lockByOperator!=1?"hidden":"";
				idList.push({'id':record.id,'type':'third'});
				var bankType=record.bankType?record.bankType+"|":"";
				array.push("<td><span>"+(record.alias?record.alias:'')+"</span></td>");
				array.push("<td><span><a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"+record.account+"</a></span><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-text='"+record.account+"'></i></td>");
				array.push("<td><span>"+bankType+(record.bankName?record.bankName:'')+"</span><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-text='"+bankType+record.bankName+"'></i></td>");
				array.push("<td><span>"+(record.owner?record.owner:'')+"</span><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-text='"+record.owner+"'></i></td>");
				array.push("<td><span>"+(record.incomeAmountDaily?record.incomeAmountDaily:0)+"</span>"+htmlExceedInLimit+"</td>");
				array.push("<td><span>"+(record.bankBalance?record.bankBalance:0)+getlimitBalanceIconStr(record)+"</span></td>");
				array.push("<td><div class='col-lg-20'><input style='width:70px;height:23px;' class='"+classHidden+"' type='text' name='amount' value='0' id='amount"+record.id+"'><i class='fa fa-copy orange bigger-110 clipboardbtn "+classHidden+"' style='cursor:pointer' data-clipboard-action=\"copy\" data-clipboard-target=\"#amount"+record.id+"\"></i></div></td>");
				array.push("<td><button class='btn btn-xs btn-white btn-warning btn-bold green' onclick='doExchangeBalance("+record.id+",\""+record.account+"\",\""+record.bankName+"\",\""+record.owner+"\",this)'><i class='ace-icon fa fa-exchange bigger-100 orange'></i><span>提现</span></button></td>");
				$("<tr>"+array.join('')+"</tr>").appendTo($tbody);
			});
			var footTitle ="";
			if(fromAccount){
				fromAccount.nbsp="   ";
				fromAccount.bankBalance=fromAccount.bankBalance?fromAccount.bankBalance:'0';
				fromAccount.balance=fromAccount.balance?fromAccount.balance:'0';
				footTitle=fillDataToModel4Item(fromAccount,"汇款账号：{account}；开户行：{bankName}；	开户人：{owner}；银行余额：{bankBalance}");
			}
			showPading(jsonObject.page,"exchangeBalancePage",showWithdrawCash,footTitle,true);//刷新
			//加载账号悬浮提示
			//loadHover_accountInfoHover(idList);
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
};

var doExchangeBalance=function(toAccountId,toAccount,toAccountBank,nickName,obj){
	var $div=$("#withdrawCashModal");
	var amount = $(obj).closest("tr").find("[name=amount]").val();
	var alipay=$div.find("input[name=alipay]").val();
	var weichat=$div.find("input[name=weichat]").val();
	var account_=$div.find("input[name=account]").val();
	var frType=$div.find("input[name=frAccType]").val();
	if(!amount||isNaN(amount)||amount<=0){
		showMessageForFail("转账金额为空或输入有误！");return;
	}
	var account = frType==accountTypeInAli?alipay:(frType==accountTypeInWechat?weichat:account_);
	bootbox.dialog({
		message: "<span class='bigger-80'>汇入账号："+toAccount+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;金额："+amount+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>",
		buttons:{"click" :{"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
					var data = {frType:frType, account:account, amount:amount, nickName:nickName, toAccount:toAccount, toAccountBank:toAccountBank};
					$.ajax({dataType:'JSON',type:"POST",async:false,url:API.api_income_cloud_put,data:data, success:function(jsonObject){
						if(jsonObject.status!=1){
							showMessageForFail("操作失败："+jsonObject.message);return
						}
						showMessageForSuccess("转账成功");
						showWithdrawCash();
					}});
				}},
	     		"click1":{"label":"取消","className":"btn-sm btn-default"}
		}
	});
};

var withdrawCashTabInit = function(frAccType){
	$("body").find("#withdrawCashModal").find("input[name='frAccType']").val(frAccType);
	showWithdrawCash(0);
};