//绑定银行卡的新增、修改js，引入此js请同时引入common_getInfo.js
currentPageLocation = window.location.href;
/**
 * 新增银行卡窗口展示
 */
var showAddBindAccount=function(){
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#addBindAccount").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			//加载银行品牌
			var options="";
			options+="<option value='' >--请选择--</option>";
			$.each(bank_name_list,function(index,record){
				options+="<option value="+record+" >"+record+"</option>";
			});
			$div.find("select[name=choiceBankBrand]").append($(options));
			//初始化盘口
			getHandicap_select($div.find("select[name='handicap_select']"));
			$div.modal("toggle");
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
var addBindAccount=function(){
	var $div=$("#addBindAccount");
    var $account = $div.find("input[name='account']");
    var $bankType = $div.find("select[name='choiceBankBrand']");
    var $bankName = $div.find("input[name='bankName']");
    var $balance = $div.find("input[name='balance']");
    var $owner = $div.find("input[name='owner']");
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $bindType=$div.find('input:radio[name="bindType"]:checked');
	var $currSysLevel = $div.find("select[name='currSysLevel']");
	var $limitBalance = $div.find("[name='limitBalance']");
	var $remark = $div.find("[name='remark']");
	var $handicapId = $("select[name='handicap_select']");
    //校验非空和输入校验
    var validateEmpty=[
    	{ele:$handicapId,name:'盘口'},
    	{ele:$account,name:'账号'},
    	{ele:$owner,name:'开户人'},
    	{ele:$bankType,name:'开户行 > 银行类别'},
    	{ele:$bankName,name:'开户行 > 支行'},
    	{ele:$balance,name:'余额'},
    	{ele:$remark,name:'备注'},
    	{ele:$bindType,name:'绑定类型'}
    ];
    var validatePrint=[
    	{ele:$account,name:'账号',maxLength:25},
    	{ele:$owner,name:'开户人',minLength:2,maxLength:10},
    	{ele:$bankName,name:'开户行 > 支行',maxLength:50},
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$balance,name:'余额',type:'amountCanZero'},
    	{ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$remark,name:'备注',minLength:5,maxLength:100}
    ];
    if(!validateEmptyBatch(validateEmpty)||
			!validateInput(validatePrint)){
    	return;
    }
	bootbox.confirm("确定新增银行卡 ?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/create',
				async:false,
				data:{
		    		"handicapId":$handicapId.val(),
					"type":$bindType.val(),
					"account":$.trim($account.val(),true),
					"bankType":$.trim($bankType.val(),true),
					"bankName":$.trim($bankName.val(),true),
					"owner":$.trim($owner.val(),true),
					"bankBalance":$.trim($balance.val()),
					"limitIn":$.trim($limitIn.val()),
					"limitOut":$.trim($limitOut.val()),
					"limitBalance":$.trim($limitBalance.val()),
					"currSysLevel":$.trim($currSysLevel.val(),true),
					"remark":$.trim($remark.val())
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	//操作成功提示
			        	showMessageForever(accountInactivatedAdd,"新增成功！");
			            $div.modal("toggle");
			        	//刷新数据列表
			            showAccountList();
			        }else{
			        	showMessageForFail("新增失败："+jsonObject.message);
			        }
			    }
			});
		}
	});
}

/**
 * 加载修改银行卡模态窗口
 */
var showUpdateBindAccount=function(accountId,fnName){
	var accountInfo=getAccountInfoById(accountId);
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#updateBindAccount").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			$div.find("#tableAdd td.noPaddingTop").css("padding-top","0px");
			//表单填充值
			$div.find("#accountId").val(accountId);
			if(accountInfo){
			    $div.find("input[name='id']").val(accountInfo.id);
			    $div.find("input[name='account']").val(hideAccountAll(accountInfo.account));
			    $div.find("input[name='bankName']").val(accountInfo.bankName);
			    $div.find("input[name='owner']").val(accountInfo.owner);
			    $div.find("input[name='peakBalance']").val(accountInfo.peakBalance);
			    $div.find("input[name='limitIn']").val(accountInfo.limitIn);
			    $div.find("input[name='limitOut']").val(accountInfo.limitOut);
			    $div.find("select[name='currSysLevel']").val(accountInfo.currSysLevel);
			    $div.find("[name='limitBalance']").val(accountInfo.limitBalance);
			    $div.find("input:radio[name='bindType'][value="+accountInfo.type+"]").attr("checked","checked");
			    if(accountInfo.flag!=accountFlagMobile){
			    	$div.find("input[name='peakBalance']").attr("disabled","disabled");
			    }
			}else{
				//提示数据不存在，并刷新页面表单
				showMessageForFail("操作失败：账号不存在");
	            showAccountList();
			}
			//加载银行品牌
			getBankTyp_select($div.find("select[name='choiceBankBrand']"),accountInfo.bankType,"全部")
			if(fnName){
				fnName();
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
 * 修改银行卡信息
 */
var updateBindAccount=function(){
	var $div=$("#updateBindAccount");
    var accountId = $div.find("#accountId").val();
    var $bankType = $div.find("select[name='choiceBankBrand']");
    var $bankName = $div.find("input[name='bankName']");
    var $owner = $div.find("input[name='owner']");
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $bindType=$div.find('input:radio[name="bindType"]:checked');
	var $currSysLevel = $div.find("select[name='currSysLevel']");
	var $limitBalance = $div.find("[name='limitBalance']");
	var $peakBalance = $div.find("[name='peakBalance']");
//	var $flag = $div.find("[name='flag']:checked");
//	var $mobile = $div.find("[name='mobile']");
    //校验
    var validateEmpty=[
    	{ele:$owner,name:'开户人'},
    	{ele:$bankType,name:'开户行 > 银行类别'},
    	{ele:$bankName,name:'开户行 > 支行'}
//    	{ele:$flag,name:'来源'}
    ];
    var validatePrint=[
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$peakBalance,name:'余额峰值',type:'amountCanZero',maxEQ:50000},
    	{ele:$owner,name:'开户人',minLength:2,maxLength:10},
    	{ele:$bankName,name:'开户行 > 支行',maxLength:50}
    ];
//    if($flag.val()&&$flag.val()==accountFlagMobile){
//    	validateEmpty.push({ele:$mobile,name:'手机号'});
//    	validatePrint.push({ele:$mobile,name:'手机号',minLength:11,maxLength:11});
//    }
    if(!validateEmptyBatch(validateEmpty)||
			!validateInput(validatePrint)){
    	return;
    }
	bootbox.confirm("确定更新此银行卡信息?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/update',
				async:false,
				data:{
					"id":accountId,
					"type":$bindType.val(),
					"bankType":$.trim($bankType.val(),true),
					"bankName":$.trim($bankName.val(),true),
					"owner":$.trim($owner.val(),true),
					"peakBalance":$.trim($peakBalance.val()),
					"limitIn":$.trim($limitIn.val()),
					"limitOut":$.trim($limitOut.val()),
					"limitBalance":$.trim($limitBalance.val()),
					"currSysLevel":$.trim($currSysLevel.val())
//					"flag":$.trim($flag.val()),
//					"mobile":$.trim($mobile.val(),true)
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	//操作成功提示
			            $div.modal("toggle");
			            showAccountList();
			        	showMessageForSuccess("修改成功");
			        }else{
			        	showMessageForFail("修改失败："+jsonObject.message);
			        }
			    },
			    error:function(result){
		        	showMessageForFail("修改失败："+jsonObject.message);
			    }
			});
		}
	});
}

/**
 * 根据ID展示绑定记录
 */
var showBindAccountListModal=function(accountId){
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#bindAccountListModal").clone().appendTo($("body"));
			//表单填充值
			$div.find("#accountId").val(accountId);
			//刷新数据
			showBindAccountList(accountId,0);
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
			$("#bindAccountListModal").keypress(function(e){
				if(event.keyCode == 13) {
					$("#bindAccountListModal #searchBtn button").click();
				}
			});
		}
	});
}

/**
 * 根据银行卡ID查找对应绑定的列表
 */
var showBindAccountList=function(bankId,CurPage){
	var $div = $("#bindAccountListModal");
	if(!!!bankId) bankId=$div.find("#accountId").val();
	if(!!!CurPage&&CurPage!=0) CurPage=$("#bindAccountListPage .Current_Page").text();
	//封装查询条件
    var statusOfIncomeToArray = new Array();
    $div.find("input[name='search_IN_status']:checked").each(function(){
        statusOfIncomeToArray.push(this.value);
    });
    if(statusOfIncomeToArray.length==0){
        statusOfIncomeToArray=[accountStatusNormal,accountStatusStopTemp,accountStatusFreeze];
    }
    var $body = $div.find("table tbody").html("");
    $.ajax({
    	type:"POST",
    	url:API.r_account_findIncomeByIssueAccountId, 
    	dataType:'json',
        data:{
        	"issueAccountId":bankId,
        	"statusOfIncomeToArray":statusOfIncomeToArray.toString(),
        	"search_LIKE_account":$.trim($div.find("input[name='search_LIKE_account']").val()),
        	"search_LIKE_owner":$.trim($div.find("input[name='search_LIKE_owner']").val()),
        	"pageSize":$.session.get('initPageSize'),
        	"pageNo":CurPage<=0?0:CurPage-1
        },
        success:function(jsonObject){
            if(jsonObject.status == 1){
            	$.each(jsonObject.data,function(index,record){
            		var tr="";
            		tr+="<td><span>"+record.account+"</span></td>";
            		tr+="<td><span>"+hideName(record.owner)+"</span></td>";
            		//不同状态使用不同颜色
    				if(record.status==accountStatusFreeze || record.status==accountStatusStopTemp){
    					tr+="<td><span class='label label-sm label-danger'>"+record.statusStr+"</span></td>";
    				}else{
    					tr+="<td><span class='label label-sm label-success'>"+record.statusStr+"</span></td>";
    				}
    				tr+="<td><span>"+record.typeStr+"</span></td>";
            		$body.append($("<tr>"+tr+"</tr>"));
            	});
            	//分页初始化
    			showPading(jsonObject.page,"bindAccountListPage",showBindAccountList,null,true);
            }else{
				showMessageForFail("查询失败："+jsonObject.message);
            }
        }
    });
}


