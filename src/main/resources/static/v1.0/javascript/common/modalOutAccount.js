//出款银行卡各种公用方法，引入此js请同时引入common_getInfo.js
currentPageLocation = window.location.href;
var allocateModal='	<div id="allocateModal" class="modal fade">\
						<div class="modal-dialog modal-lg" style="width:540px;">\
							<div class="modal-content">\
								<div class="modal-header no-padding text-center">\
									<div class="table-header"><button class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>分配&nbsp;&nbsp;&nbsp;&nbsp;账号:{hideAccount}&nbsp;&nbsp;&nbsp;&nbsp;{bankNameTitle}:{bankName}</span></div>\
								</div>\
								<div class="modal-body"><select onchange="disable();" class="form-control" name="uid"></select></div>\
								<div class="col-sm-12 modal-footer">\
								    <button class="btn btn-sm btn-primary {hideFlag}" onclick="allocateOutAccount(false,{id});">人工出款</button>\
									<button class="btn btn-sm btn-primary" id="boot" onclick="allocateOutAccount(true,{id});">机器出款</button>\
									<button class="btn btn-sm btn-default" data-dismiss="modal">取消</button>\
								</div>\
							</div>\
						</div>\
					</div>';
var allocateModalCalBack=null;

function showAllocateModal(accountId,calBack){
	allocateModalCalBack = calBack;
	$("body").find("#allocateModal").remove();
	var account = getAccountInfoById(accountId);
	account.bankNameTitle='开户行';
	account.hideAccount=hideAccountAll(account.account);
	if(account.flag&&account.flag==2){
		//屏蔽掉人工出款按钮
		account.hideFlag=' hide ';
	}else{
		account.hideFlag='';
	}
	var $div = $(fillDataToModel4Item(account,allocateModal)).appendTo($("body")).modal("toggle");
	$.ajax({ type:"post", url:API.r_user_findUserList4OutwardAsign,dataType:'json',success:function(jsonObject){
		if(jsonObject.status == 1){
			var array = new Array();
			array.push("<option value=\"\">请选择</option>");
			$.each(jsonObject.data,function(index,record){
				array.push("<option value=\""+record.id+"\">"+record.uid+"</option>");
			});
			$div.find("div.modal-body select.form-control").html("<select class=\"form-control\">"+array.join('')+"</select>");
		}
	}});
}

function disable(){
	 var uid = $("#allocateModal select[name=uid]").val();
	 if(uid){
		 $("#boot").attr("disabled", true);
	 }else{
		 $("#boot").attr("disabled", false);
	 }
}

function allocateOutAccount(robot,accountId){
	var uid = null;
	if(!robot){
		 uid = $("#allocateModal select[name=uid]").val();
		if(!uid){
			return ;
		}
	}
	$.ajax({ type:"post", url:API.r_account_asin4OutwardAccount, data:{"accountId":accountId,"operatorId":uid},dataType:'json',success:function(jsonObject){
		if(jsonObject.status == 1){
			$("#allocateModal").modal("toggle");
			if(allocateModalCalBack){
				allocateModalCalBack();
			}
		}
	}});
}

function showRecycleModal(accountId,transBlackTo,callBack){
	var accountInfo = getAccountInfoById(accountId);
	accountInfo.bankNameTitle='开户行';
	accountInfo.tips ="<span class='red'>将停止下发给该账号,当金额过低时,将自动回收到可用状态,(自动回收,只对机器出款有效)</span>";
	if(transBlackTo){
		accountInfo.tips ="<span class='red'>已停止下发给该账号,当金额过低时,将自动回收到可用状态,是否继续回收到可用状态,(自动回收,只对机器出款有效)</span>";
	}
	bootbox.dialog({
		message: "<span class='bigger-110'>确定回收该账号（转可用）<br/>账号："+hideAccountAll(accountInfo.account)+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+accountInfo.bankNameTitle+"："+accountInfo.bankName+"</br>"+accountInfo.tips+"</span>",
		buttons:{
			"click" :{"label" : "回收","className" : "btn-sm btn-primary","callback": function() {
				$.ajax({ type:"post", url:API.r_account_recycle4OutwardAccount,data:{"accountId":accountId},dataType:'json',success:function(jsonObject){
					if(jsonObject.status == 1){
						if(callBack)
							callBack();
					}else{
						showMessageForFail(jsonObject.message);
					}
				}});
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
}

function showEnabledModal(accountId,callBack){
	var accountInfo = getAccountInfoById(accountId);
	bootbox.dialog({
		message: "<span class='bigger-110'>转可用<br/>账号："+hideAccountAll(accountInfo.account)+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;开户行："+accountInfo.bankName+"</span>",
		buttons:{
			"click" :{"label" : "转可用","className" : "btn-sm btn-primary","callback": function() {
				$.ajax({ type:"post", url:API.r_account_toEnabled,data:{"accountId":accountId},dataType:'json',success:function(jsonObject){
					if(jsonObject.status == 1 && callBack){
						callBack();
					}
				}});
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
}

function showNormalModal(accountId,callBack){
	var accountInfo = getAccountInfoById(accountId);
	//冻结恢复时
	if(accountInfo.status==accountStatusFreeze){
		bootbox.dialog({
			title:"恢复确认",
			message:statusChangeMessage(accountInfo),
			buttons:{
				"click1" :{"label" : "恢复","className" : "btn btn-sm btn-primary", "callback": function() {
					var remark=$("[name=freezeModal_remark]").val();
					if(!remark||remark.length<5||remark.length>100){
						showMessageForFail("请输入备注，5-100字之间");
						return false;
					}
					$.ajax({dataType:'json',type:"get", url:API.r_account_asin4OutwardAccount,data:{"accountId":accountId,"remark":remark },success:function(jsonObject){
						if(jsonObject.status == 1 && callBack){
							callBack();
						}
					} });
				}},
				"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
			}
		});
	}else{
		accountInfo.bankNameTitle='开户行';
		bootbox.dialog({
			message: "<span class='bigger-110'>确定恢复使用<br/>账号："+hideAccountAll(accountInfo.account)+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+accountInfo.bankNameTitle+"："+accountInfo.bankName+"</span>",
			buttons:{
				"click1" :{"label" : "恢复","className" : "btn btn-sm btn-primary","callback": function() {
					$.ajax({ type:"post", url:API.r_account_asin4OutwardAccount, data:{"accountId":accountId},dataType:'json',success:function(jsonObject){
						if(jsonObject.status == 1){
							if(callBack){
								callBack();
							}
						}
					}});
				}},
				"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
			}
		});
	}
}

function showStopTempModal(accountId,callBack){
	var accountInfo = getAccountInfoById(accountId);
	//冻结转停用时
	if(accountInfo.status==accountStatusFreeze){
		bootbox.dialog({
			title:"停用确认",
			message:statusChangeMessage(accountInfo),
			buttons:{
				"click1" :{"label" : "停用","className" : "btn btn-sm btn-primary", "callback": function() {
					var remark=$("[name=freezeModal_remark]").val();
					if(!remark||remark.length<5||remark.length>100){
						showMessageForFail("请输入备注，5-100字之间");
						return false;
					}
					$.ajax({dataType:'json',type:"get", url:API.r_account_toStopTemp,data:{"accountId":accountId,"remark":remark },success:function(jsonObject){
						if(jsonObject.status == 1 && callBack){
							callBack();
						}
					} });
				}},
				"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
			}
		});
	}else{
		accountInfo.bankNameTitle='开户行';
		bootbox.dialog({
			message: "<span class='bigger-110'>确定停用该账号<br/>账号："+hideAccountAll(accountInfo.account)+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+accountInfo.bankNameTitle+"："+accountInfo.bankName+"</span>",
			buttons:{
				"click1" :{"label" : "停用","className" : "btn btn-sm btn-primary","callback": function() {
					$.ajax({ dataType:'json',type:"get", url:API.r_account_toStopTemp, data:{ "accountId":accountId}, success:function(jsonObject){
						if(jsonObject.status == 1 && callBack){
							callBack();
						}
					}});
				}},
				"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
			}
		});
	}
}
var statusChangeMessage=function(accountInfo){
	accountInfo.bankNameTitle='开户行';
	return "<span class='bigger-110'>" +
				"<br/>" +
				"<label class='control-label bolder blue'>账号：&nbsp;&nbsp;&nbsp;&nbsp;</label>"
				+hideAccountAll(accountInfo.account)+"&nbsp;&nbsp;&nbsp;&nbsp;" +
				"<label class='control-label bolder blue'>"+accountInfo.bankNameTitle+"：&nbsp;&nbsp;&nbsp;&nbsp;</label>"
				+accountInfo.bankName+
			"</span>" +
			"<br/>" +
			"<label class='control-label bolder blue'>备注：&nbsp;&nbsp;&nbsp;&nbsp;</label>" +
			"<span class='input-icon'>" +
				"<input name='freezeModal_remark' style='height:32px;width:400px;'class='input-medium' placeholder='请输入备注（5-100字）' >" +
				"<i class='ace-icon fa fa-asterisk red'></i>" +
			"</span>" +
			"<br/><br/>";
}
function showFreezeModal(accountId,callBack){
	var accountInfo = getAccountInfoById(accountId);
	bootbox.dialog({
		title:"冻结确认",
		message:statusChangeMessage(accountInfo),
		buttons:{
			"click1" :{"label" : "冻结","className" : "btn btn-sm btn-danger", "callback": function() {
				var remark=$("[name=freezeModal_remark]").val();
				if(!remark||remark.length<5||remark.length>100){
					showMessageForFail("请输入备注，5-100字之间");
					return false;
				}
				$.ajax({dataType:'json',type:"get", url:API.r_account_toFreezeForver,data:{"accountId":accountId,"remark":remark },success:function(jsonObject){
					if(jsonObject.status == 1 && callBack){
						callBack();
					}
				} });
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
}

/**
 * 新增出款银行卡
 */
var showAddOutAccount=function(type,status,fnName){
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({
		type:"GET",
		async:false,
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/outAccount.html",
		success : function(html){
			var $div=$(html).find("#addOutAccount").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
	    	//加载银行品牌
			var options="";
			options+="<option value=''>------------请选择------------</option>";
			$.each(bank_name_list,function(index,record){
				options+="<option value="+record+" >"+record+"</option>";
			});
			$div.find("select[name=bankType]").html(options);
			if(type==accountTypeReserveBank){
				//备用卡填充默认建议值并加提示
				$div.find("[name=peakBalance]").parent().parent().after($("<tr><td colspan='4'><span class='red bolder'><i class='ace-icon fa fa-exclamation-triangle bigger-130 red' style='cursor:pointer;'></i>不建议修改告警值和峰值，否则备用卡做出款卡用时，出小款会出问题</span></td></tr>"))
				$div.find("[name=limitBalance]").val(5000);
				$div.find("[name=peakBalance]").val(50000);
			}
			if (type==accountTypeInBank){
	            loadProvinceCity_select($div.find('select[name="province_select"]'),$div.find('select[name="city_select"]'));
				$div.find('tr[id="inBankAccount_min_amount"]').show();
			    $div.find('tr[id="level_prompt"]').hide();
			}else{
				$(".inComeShow").hide();
                $div.find('tr[id="inBankAccount_min_amount"]').hide();
                $div.find('tr[id="level_prompt"]').show();
            }
			//确定按钮绑定事件
			$div.find("#doAdd").bind("click",function(){
				addOutAccount(type,fnName);
			});
			$div.modal("toggle");
			//初始化盘口
			getHandicap_select($div.find("select[name='handicap_select']"));
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}
/**
 * 新增银行卡
 */
var addOutAccount=function(type,fnName){
	var $div=$("#addOutAccount");
    var $account = $div.find("input[name='account']");
    var $bankName =$div.find("input[name='bankName']");
    var $balance = $div.find("input[name='balance']");
    var $owner = $div.find("input[name='owner']");
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $limitOutOne = $div.find("input[name='limitOutOne']");
    var $lowestOut = $div.find("input[name='lowestOut']");
	var $limitOutOne = $div.find("input[name='limitOutOne']");
	var $limitOutOneLow = $div.find("input[name='limitOutOneLow']");
	var $limitOutCount = $div.find("input[name='limitOutCount']");
    var $limitBalance = $div.find("input[name='limitBalance']");
	var $currSysLevel = $div.find("select[name='currSysLevel']");
	let $currSysLevelInBank = $div.find("select[name='currSysLevelInBank'] option:selected");
	var $peakBalance = $div.find("input[name='peakBalance']");
    var $remark = $div.find("input[name='remark']");
	var $handicapId = $("select[name='handicap_select']");
    var $bankType = $div.find("select[name='bankType']");
    var $province = $div.find("select[name='province_select']");
    var $city = $div.find("select[name='city_select']");
    let $minAmount= $div.find('tr[id="inBankAccount_min_amount"]');
    var data={
    		"handicapId":$handicapId.val(),
			"type":type,
			"account":$.trim($account.val(),true),
			"bankName":$.trim($bankName.val(),true),
			"owner":$.trim($owner.val(),true),
			"balance":$balance.val(),
			"limitIn":$limitIn.val(),
			"limitOut":$limitOut.val(),
			"limitBalance":$limitBalance.val(),
			"lowestOut":$lowestOut.val(),
			"limitOutOne":$limitOutOne.val(),
			"limitOutOneLow":$limitOutOneLow.val(),
			"limitOutCount":$limitOutCount.val(),
			"peakBalance":$peakBalance.val(),
			"currSysLevel":$currSysLevel.val(),
			"bankType":$.trim($bankType.val(),true),
            "province":$.trim($province.val(),true),
            "city":$.trim($city.val(),true),
			"minAmount":$.trim($minAmount.val(),true),
            "subType":0,
			"remark":$remark.val()
		};
    var url = '/r/account/create';
    //校验非空和输入校验
    var validateEmpty=[
    	{ele:$handicapId,name:'盘口'},
    	{ele:$account,name:'账号'},
    	{ele:$owner,name:'开户人'},
    	{ele:$bankType,name:'银行类别'},
    	{ele:$bankName,name:'支行'},
    	{ele:$balance,name:'余额'},
    	{ele:$remark,name:'备注'}
    ];
    var validatePrint=[
    	{ele:$account,name:'账号',maxLength:25},
    	{ele:$balance,name:'余额',type:'amountCanZero'},
    	{ele:$peakBalance,name:'余额峰值',type:'amountPlus',maxEQ:50000},
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$lowestOut,name:'最低余额限制',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitOutOne,name:'最高单笔出款限额',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitOutOneLow,name:'最低单笔出款限额',minEQ:0,maxEQ:50000},
    	{ele:$limitOutCount,name:'当日出款笔数',minEQ:0,maxEQ:500},
    	{ele:$owner,name:'开户人',minLength:2,maxLength:10},
    	{ele:$bankName,name:'支行',maxLength:50}
    ];
    if (type==accountTypeInBank) {
        validateEmpty=[
            {ele:$handicapId,name:'盘口'},
            {ele:$account,name:'账号'},
            {ele:$currSysLevel,name:'层级'},
            {ele:$owner,name:'开户人'},
            {ele:$bankType,name:'银行类别'},
            {ele:$bankName,name:'支行'},
            {ele:$province,name:'省份'},
            {ele:$city,name:'城市'},
            {ele:$balance,name:'余额'},
            {ele:$limitBalance,name:'余额告警'},
            {ele:$peakBalance,name:'余额峰值'},
            {ele:$limitIn,name:'当日入款限额'},
            {ele:$lowestOut,name:'最低余额限制'}
        ];
        validatePrint=[
            {ele:$account,name:'账号',maxLength:25},
            {ele:$balance,name:'余额',type:'amountCanZero'},
            {ele:$peakBalance,name:'余额峰值',type:'amountPlus',maxEQ:5000000},
            {ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
            {ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:5000000},
            {ele:$owner,name:'开户人',minLength:2,maxLength:10},
            {ele:$bankName,name:'支行',maxLength:50}
        ];
        url = '/r/account/addInBankAccount';
    }
    if(!validateEmptyBatch(validateEmpty)||
			!validateInput(validatePrint)){
    	return;
    }
    var ajaxData ={
        type:"POST",
        dataType:'JSON',
        url:url,
        async:false,
        data:data
    };
    if (type==accountTypeInBank) {
        $.extend(ajaxData,{contentType:"application/json;charset=UTF-8"});
        ajaxData.data= JSON.stringify(data);
    }
    $.extend(ajaxData,{
        success:function(jsonObject){
            if(jsonObject.status == 1){
                showMessageForever(accountInactivatedAdd,"新增成功！");
                $div.modal("toggle");
                //刷新数据列表
                if(fnName){
                    fnName();
                }
            }else{
                //失败提示
                showMessageForFail("新增失败："+jsonObject.message);
            }
        }
    });
	bootbox.confirm("确定新增账号?", function(result) {
		if (result) {
			$.ajax(ajaxData);
		}
	});
}

/**
 * 加载修改银行卡模态窗口
 * accountId:需要修改的ID
 * fnName：修改成功后的回调函数，（刷新表单）
 * isActivated：是否已激活
 */
var showUpdateOutAccount=function(accountId,fnName,isThird,isActivated){
	var accountInfo=getAccountInfoById(accountId);
	$.ajax({
		type:"GET",
		async:false,
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/outAccount.html",
		success : function(html){
			var $div=$(html).find("#updateOutAccount").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			//表单填充值
			$div.find("#accountId").val(accountId);
			if(accountInfo){
			    $div.find("input[name='id']").val(accountInfo.id);
			    $div.find("input[name='account']").val(hideAccountAll(accountInfo.account));
			    $div.find("input[name='bankName']").val(accountInfo.bankName);
			    $div.find("input[name='owner']").val(accountInfo.owner);
			    $div.find("input[name='limitIn']").val(accountInfo.limitIn);
			    $div.find("input[name='limitOut']").val(accountInfo.limitOut);
			    $div.find("input[name='limitOutOne']").val(accountInfo.limitOutOne);
			    $div.find("input[name='limitOutOneLow']").val(accountInfo.limitOutOneLow?accountInfo.limitOutOneLow:"");
			    $div.find("input[name='limitOutCount']").val(accountInfo.limitOutCount?accountInfo.limitOutCount:"0");
			    $div.find("input[name='lowestOut']").val(accountInfo.lowestOut);
			    $div.find("input[name='limitBalance']").val(accountInfo.limitBalance);
			    $div.find("input[name='peakBalance']").val(accountInfo.peakBalance);
			    if(accountInfo.type==accountTypeOutBank){
//					$div.find("[name=flag]").click(function(){
//						if($div.find("[name=flag]:checked").val()==accountFlagMobile){
//							//出款卡 手机类型
//					    	$div.find("[name='label_peakBalance']").text("保证金");
//						}else{
//					    	$div.find("[name='label_peakBalance']").text("余额峰值");
//						}
//					});
					if(accountInfo.flag==accountFlagMobile){
						//出款卡 手机类型
				    	$div.find("[name='label_peakBalance']").text("保证金");
					}
				}
				//初始化盘口
//				getHandicap_select($div.find("select[name='handicap_select']"),accountInfo.handicapId);
			    getAccountType_select_search($div.find("select[name='accountType_select']"),null,accountInfo.type,accountInfo.subType);
			    if(!isActivated){
			    	$div.find("select[name='accountType_select']").attr("disabled","disabled");
			    }else{
			    	$div.find("[name=owner],[name=bankType]").attr("disabled","disabled");
			    	$div.find(".classOfDialogTitle").html("完善银行卡信息");
			    	$div.find(".statusTr").show();
					if(accountInfo.type==accountTypeInBank){
						//银行入款卡 和支付宝入款卡都不可以改为新卡 银行入款卡不可以在用
						$div.find(".incomeHide5").remove();
						if(accountInfo.subType==0){
							$div.find(".incomeHide1").remove();
						}
					}
			    }
				//内外层
			    if(accountInfo.currSysLevel){
				    $div.find("select[name='currSysLevel']").val(accountInfo.currSysLevel);
			    }
				//加载银行品牌
				var options="";
				$.each(isThird?third_name_list:bank_name_list,function(index,record){
					if(accountInfo.bankType==record){
						options+="<option selected value="+record+" >"+record+"</option>";
					}else{
						options+="<option value="+record+" >"+record+"</option>";
					}
				});
				$("select[name=bankType]").append($(options));
//				$div.find("[name=flag][value="+accountInfo.flag+"]").prop("checked",true);
//				if(accountInfo.flag&&accountInfo.flag==accountFlagMobile){
//					$div.find("[name='mobile']").val(accountInfo.mobile);
//				}
			    if(isThird){
			    	//第三方无编号  银行类别
			    	$div.find(".isThirdRemove").remove();
			    	$div.find("[name=bankName]").css("width","200px");
					$div.find(".ownerTitle").text("经办人");
					$div.find(".bankNameTitle").text("第三方");
					$div.find(".chocieBankType").text("第三方类别");
					$div.find("[name=owner]").prop("placeholder","经办人");
					$div.find("[name=bankName]").prop("placeholder","第三方");
					$div.find("span.classOfDialogTitle").text("修改第三方账号");
			    }
			}else{
				//提示数据不存在，并刷新页面表单
				if(fnName){
					fnName();
				}
			}
			//确定按钮绑定事件
			$div.find("#doUpdate").bind("click",function(){
				updateOutAccount(fnName,isThird,accountInfo.type,isActivated);
			});
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}

/**
 * 修改银行卡信息
 */
var updateOutAccount=function(fnName,isThird,type,isActivated){
	var $div=$("#updateOutAccount");
    var $accountId = $div.find("#accountId");
	var accountInfo=getAccountInfoById($accountId.val());
    var $bankName = $div.find("input[name='bankName']");
    var $owner = $div.find("input[name='owner']");
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $lowestOut = $div.find("input[name='lowestOut']");
    var $limitOutOne = $div.find("input[name='limitOutOne']");
    var $limitOutOneLow = $div.find("input[name='limitOutOneLow']");
    var $limitOutCount = $div.find("input[name='limitOutCount']");
    var $limitBalance = $div.find("input[name='limitBalance']");
	var $currSysLevel = $div.find("select[name='currSysLevel']");
    var $peakBalance = $div.find("input[name='peakBalance']");
	var $bankType = $div.find("select[name='bankType']");
//	var $flag = $div.find("[name='flag']:checked");
//	var $mobile = $div.find("[name='mobile']");
	var $type = $div.find("[name='accountType_select']");
	var $status = $div.find("[name=status]:checked");
    var data={
			"id":$accountId.val(),
			"bankName":$.trim($bankName.val(),true),
			"owner":$.trim($owner.val(),true),
			"limitIn":$limitIn.val(),
			"limitOut":$limitOut.val(),
			"limitOutOne":$limitOutOne.val(),
			"limitOutOneLow":$limitOutOneLow.val(),
			"limitOutCount":$limitOutCount.val(),
			"limitBalance":$limitBalance.val(),
			"lowestOut":$lowestOut.val(),
			"bankType":$.trim($bankType.val()),
			"currSysLevel":$currSysLevel.val(),
			"peakBalance":$peakBalance.val()
//			"flag":$flag.val(),
//			"mobile":$.trim($mobile.val(),true)
		};
    //校验非空和输入校验
    var validateEmpty=[
    	{ele:$owner,name:'开户人'},
    	{ele:$bankType,name:isThird?'第三方类别':'银行类别'},
    	{ele:$bankName,name:isThird?'第三方':'支行'}
//    	{ele:$flag,name:'来源'}
    ];
//    if($flag.val()&&$flag.val()==accountFlagMobile){
//    	validateEmpty.push({ele:$mobile,name:'手机号'});
//    }
    if(isActivated){
    	data.type=$type.val();
    	data.status=$status.val();
    	validateEmpty.push({ele:$type,name:'类型'});
    	validateEmpty.push({ele:$status,name:'状态'});
    }
    if(data.type==accountTypeInBank){
    	data.subType=$type.find("option:selected").attr("subType");
    }
    var validatePrint=[
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$lowestOut,name:'最低余额限制',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitOutOne,name:'最高单笔出款限额',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitOutOneLow,name:'最低单笔出款限额',minEQ:0,maxEQ:50000},
    	{ele:$limitOutCount,name:'当日出款笔数',minEQ:0,maxEQ:500},
    	{ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$owner,name:isThird?'开户人':'经办人',minLength:2,maxLength:10},
    	{ele:$bankName,name:isThird?'第三方':'支行',maxLength:50}
    ];
    if(type==accountTypeOutBank&&accountInfo.flag==accountFlagMobile){
		//出款卡
    	validateEmpty.push({ele:$peakBalance,name:'保证金'});
    	validatePrint.push({ele:$peakBalance,name:'保证金',type:'amountCanZero',maxEQ:50000});
//    	data.lowestOut=setLowestOut($peakBalance.val(),$lowestOut.val());
//    	data.limitOutOne=setLimitOutOne($peakBalance.val(),$limitOutOne.val());
	}else{
    	validatePrint.push({ele:$peakBalance,name:'余额峰值',type:'amountCanZero',maxEQ:50000});
	}
    if(!validateEmptyBatch(validateEmpty)||
			!validateInput(validatePrint)){
    	return;
    }
	bootbox.confirm("确定修改账号?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/update',
				async:false,
				data:data,
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	//操作成功提示
			            $div.modal("toggle");
			        	showMessageForSuccess("修改成功");
			            if(fnName){
			            	fnName();
			            }
			        }else{
			        	//失败提示
			        	showMessageForFail("修改失败："+jsonObject.message);
			        }
			    }
			});
		}
	});
}


