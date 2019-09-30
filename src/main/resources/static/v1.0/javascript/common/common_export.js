
/**
 * type: income wechatIncome aliIncome thirdIncome outward trans
 */
var showExportModal_InOut=function(accountId,operaType){
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/export.html", 
		success : function(html){
			var $div=$(html).find("#choiceExportModal_InOut").clone().appendTo($("body"));
			$div.find("#accountId").val(accountId);
			$div.find("#operaType").val(operaType);
			if($.inArray(operaType,["wechatIncome","aliIncome"]) != -1){
				//微信 支付宝无银行流水
				$div.find("#bankLogLink").remove();
			}
			if(accountId==0 || accountId==-1 || accountId==-2|| accountId==-3|| accountId==-8 || accountId==-5){
				//导出全部下发卡银行流水 没有系统流水。
				$div.find("#sysLink").remove();
			}
			if(accountId==-2 || accountId==-8){
				$div.find("#exBankType").val($.trim($("#bankType").val()));
			}
			//初始化时间控件，并初始化互相清空事件
	    	initTimePicker(true,$div.find("[name=startAndEndTime_export]"));
			choiceTimeClearAndSearch($div.find('[name=startAndEndTime_export]'),"fieldval_export");
			if(operaType=='incomeIssued'){
				$div.find("#sysLink,#bankLogLink").hide();
				$div.find("#incomeIssiuedLink").removeClass("hidden");
				//入款明细 下发银行卡 已匹配明细导出，与明细页面内容保持一致
				$div.find("#incomeIssiuedLink").click(function(){
					doExport_InOut_IncomeIssiuedLink();
				});
			}else{
				$div.find("#sysLink").click(function(){
					doExport_InOut_sys();
				});
				$div.find("#bankLogLink").click(function(){
					doExport_InOut_bankLog();
				});
			}
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}
/**
 * 入款明细 下发银行卡 已匹配系统详情
 */
var doExport_InOut_IncomeIssiuedLink=function(){
	var $div=$("#choiceExportModal_InOut");
	var accountId=$div.find("#accountId").val(),operaType=$div.find("#operaType").val();
	var url="/r/exportaccount/incomeIssued";
	var startAndEndTime=$("[name=startAndEndTime_export]").val();
	var fieldval_export=$("[name=fieldval_export]:checked").val();
	url+="/"+accountId;
	if(startAndEndTime&&startAndEndTime.length>0){
		url+="/"+getTimeArray(startAndEndTime).toString();
	}else{
		url+="/0";
	}
	if(fieldval_export){
		url+="/"+fieldval_export;
	}else{
		url+="/0";
	}
	$div.find("#incomeIssiuedLink").attr("href",url);
}

var doExport_InOut_sys=function(){
	var $div=$("#choiceExportModal_InOut");
	var accountId=$div.find("#accountId").val(),operaType=$div.find("#operaType").val(),url="";
	if($.inArray(operaType,["income","wechatIncome","aliIncome","trans","senderCard"]) != -1){
		//入款明细 入款银行卡 入款微信 入款支付宝 
		url+="/r/exportaccount/incomeSys";
	}else if($.inArray(operaType,["trans"]) != -1){
		//系统中转
		url+="/r/exportaccount/incomeSys";
	}else if(operaType=="outward"){
		//出款明细 按账号统计
		url+="/r/exportaccount/outwardSys";
	}
	var startAndEndTime=$("[name=startAndEndTime_export]").val();
	var fieldval_export=$("[name=fieldval_export]:checked").val();
	//获取盘口
	var handicap=$("#handicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	if($.inArray(operaType,["aliIncome"]) != -1&&handicap!=0){
		url+="/"+"8888,"+handicap;
	}else{
		url+="/"+accountId;
	}
	if(startAndEndTime&&startAndEndTime.length>0){
		url+="/"+getTimeArray(startAndEndTime).toString();
		fieldval_export=false;
	}else{
		url+="/0";
	}
	if(fieldval_export){
		url+="/"+fieldval_export;
	}else{
		url+="/0";
	}
	if($.inArray(operaType,["income","trans"]) != -1){
		url+="/"+operaType;
	}else if($.inArray(operaType,["income","trans","senderCard","aliIncome"]) != -1){
		url+="/"+operaType;
	}
	$div.find("#sysLink").attr("href",url);
}

var doExport_InOut_bankLog=function(accountId,operaType){
	var $div=$("#choiceExportModal_InOut");
	var accountId=$div.find("#accountId").val(),operaType=$div.find("#operaType").val(),url="";
	if($.inArray(operaType,["income","outward","trans","senderCard"]) != -1){
		if(accountId==-2||accountId==-8){
			url+="/r/exportaccount/outBankLog";
		}else{
			url+="/r/exportaccount/bankLog";
		}
		var startAndEndTime=$("[name=startAndEndTime_export]").val();
		var fieldval_export=$("[name=fieldval_export]:checked").val();
		url+="/"+accountId;
		if(startAndEndTime&&startAndEndTime.length>0){
			url+="/"+getTimeArray(startAndEndTime).toString();
		}else{
			url+="/0";
		}
		if(fieldval_export){
			url+="/"+fieldval_export;
		}else{
			url+="/0";
		}
		if(accountId==-2||accountId==-8){
			url+="/"+($div.find("#exBankType").val()==""?"0":$div.find("#exBankType").val());
		}
	}
	$div.find("#bankLogLink").attr("href",url);
}


//导出所选时间内的出款单
function exportOut(accountid,handicapid){
	//日期 条件封装
	var startAndEndTime = $("input[name='exportStartAndEndTime']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//获取导出盘口
	//获取盘口
	var handicap=$("#exportHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	var url="/r/out/exportout";
	$("#exportOut").attr("href",url+"/"+startAndEndTimeToArray+"/"+handicap+"/"+accountid+"/"+handicapid);
}
