currentPageLocation = window.location.href;
var typeALL=[accountTypeInBank,accountTypeOutBank,accountTypeReserveBank,accountTypeBindWechat,accountTypeBindAli,accountTypeThirdCommon,accountTypeBindCommon,accountTypeBindCustomer];
/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountPage .Current_Page").text();
	//封装data
	var $div = $("#accountFilter");
    var statusToArray = new Array();
    $div.find("input[name='search_IN_status']:checked").each(function(){
        statusToArray.push(this.value);
    });
    if(statusToArray.length==0){
        statusToArray=[accountStatusNormal,accountStatusFreeze,accountStatusExcep,accountStatusStopTemp,accountStatusEnabled,accountInactivated,accountActivated];
    }
    var type=$div.find("[name=search_EQ_accountType]").val();
    if(!type){
    	type=typeALL;
    }
    var data = {
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize'),
        search_LIKE_account:$.trim($div.find("input[name='search_LIKE_account']").val()),
        search_LIKE_bankType:$.trim($div.find("[name='search_LIKE_bankType']").val()),
        search_LIKE_owner:$.trim($div.find("input[name='search_LIKE_owner']").val()),
        search_EQ_alias:$.trim($div.find("input[name='search_EQ_alias']").val()),
        statusToArray:statusToArray.toString(),
        handicapId:$div.find("select[name='search_EQ_handicapId']").val(),
        typeToArray:type.toString(),
        search_EQ_flag:$div.find("input[name='search_EQ_flag']:checked").val()
    };
    if($div.find("[name=search_EQ_accountType]").val()==accountTypeInBank){
    	data.search_EQ_subType=$div.find("[name='search_EQ_accountType'] option:selected").attr("subType");
    }
    //发送请求
	$.ajax({
        dataType:'JSON',
        type:"POST", 
		async:false,
        url:"/r/account/list4edit",
        data:data,
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			var $tbody=$("#accountListTable tbody");
			$tbody.html("");
			var idList=new Array();
			$.each(jsonObject.data,function(index,record){
				var tr="";
				var typeStr="";
				if(record.type==accountTypeInBank){
					typeStr="入款银行卡";
				}else if(record.type==accountTypeOutBank){
					typeStr="出款银行卡";
				}else if(record.type==accountTypeCashBank){
					typeStr="备用卡";
				}else if(record.type==accountTypeBindCommon){
					typeStr="下发银行卡";
				}
				tr+="<td><span>"+record.handicapName+"</span></td>";
				tr+="<td><span name='alias"+record.id+"'>"+((record.alias&&record.alias!='null')?record.alias:'')+"</span></td>";
				tr+="<td style='padding-left:0px;padding-right:0px;'>" +
						"<a class='bind_hover_card' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"
				record.bankType=record.bankType?record.bankType:'无';
                record.owner=record.owner?record.owner:'无';
				tr+="<span name='bankType_owner"+record.id+"' >"+record.bankType+"&nbsp;|&nbsp;"+record.owner+"&nbsp;|&nbsp;</span>";
				idList.push({'id':record.id});
				tr+=hideAccountAll(record.account);
				tr+=	"</a>" +
					"</td>";
				//不同状态使用不同颜色
				if(record.status==accountStatusFreeze){
					tr+="<td><span class='label label-sm label-danger'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountStatusStopTemp){
					tr+="<td><span class='label label-sm label-primary'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountInactivated){
					tr+="<td><span class='label label-sm label-warning'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountStatusExcep){
					tr+="<td><span class='label label-sm label-inverse'>"+record.statusStr+"</span></td>";
				}else{
					tr+="<td><span class='label label-sm label-success'>"+record.statusStr+"</span></td>";
				}
				tr+="<td><span class=''>"+record.typeStr+"</span></td>";
				tr+="<td><span class=''>"+getFlagStr(record.flag)+"</span></td>";
				//操作
				tr+="<td>";
				//修改账号信息
				tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
					onclick='showModal_updateSpecialAccount("+record.id+")' contentRight='UpdateSpecialAccount:UpdateSpecialAccount:*'>\
					<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-info btn-bold "+OperatorLogBtn+" ' \
						onclick='showModal_accountExtra("+record.id+")' >\
						<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
						"onclick='showInOutListModal("+record.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+="</td>";
				$tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
			});
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.page,"accountPage",showAccountList,null,true);
			contentRight();
        }
	});
}

/** 账号信息特殊修改 */
var showModal_updateSpecialAccount=function(accountId){
	var $div=$("#updateSpecialAccount4clone").clone().attr("id","updateSpecialAccount");
	$div.find("#tableAdd td").css("padding-top","10px");
	$div.find("#tableAdd td.noPaddingTop").css("padding-top","0px");
	//表单填充值
	$div.find("#accountId").val(accountId);
	var accountInfo=getAccountInfoById(accountId);
	if(accountInfo){
	    $div.find("#accountId").val(accountInfo.id);
	    $div.find("input[name='account']").val(accountInfo.account).attr("placeholder",accountInfo.account);
	    $div.find("select[name='currSysLevel']").val(accountInfo.currSysLevel);
		//初始化盘口 账号类型
		getHandicap_select($div.find("select[name='handicap_select']"),accountInfo.handicapId);
		getAccountType_select_search($div.find("select[name='accountType_select']"),null,accountInfo.type,accountInfo.subType);
		if(accountInfo.type==accountTypeInBank&&(accountInfo.flag!=2||accountInfo.subType==3)){
			//入款卡 PC  或者云闪付入款卡，不允许修改类型
			$div.find("[name=accountType_select]").attr("disabled","disabled");
		}
		if(accountInfo.type!=accountTypeInBank||accountInfo.subType!=3){
			//非云闪付卡不允许修改为云闪付
			$div.find("[name=accountType_select]").find("option[value=1][subtype=3]").remove();
		}
	}
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model
		$div.remove();
	});
}
var do_updateSpecialAccount=function(){
	var $div=$("#updateSpecialAccount");
	var accountInfo=getAccountInfoById($div.find("#accountId").val());
	if(!accountInfo) return;
	var $handicap=$div.find("[name=handicap_select]");
	var $account=$div.find("[name=account]");
	var $type=$div.find("[name=accountType_select]");
	var $currSysLevel = $div.find("select[name='currSysLevel']");
	var validate=[
    	{ele:$handicap,name:'盘口'},
    	{ele:$account,name:'账号'},
    	{ele:$type,name:'类型'}
    ];
    var validatePrint=[
    	{ele:$account,name:'账号',maxLength:25}
    ];
    var data={
			"id":$div.find("#accountId").val(),
			"account":$.trim($account.val(),true),
			"handicapId":$handicap.val(),
			"type":$type.val(),
			"currSysLevel":$currSysLevel.val()
		};
    if($type.val()==accountTypeInBank){
    	//入款卡新增子类型
    	data.subType=$type.find("option:selected").attr("subType");
    }
    if(!validateEmptyBatch(validate)
    		||!validateInput(validatePrint)){
    	return;
    }
    bootbox.confirm({title:"确认",message:"请再次确认", callback: function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/updateSpecialAccount',
				async:false,
				data:data,
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	showAccountList();
			        	$div.modal("toggle");
			        	if(jsonObject.message){
			        		//修改过账号有返回message
				        	showMessageForever(accountInactivatedAdd,"修改成功！",jsonObject.data?jsonObject.data.flag:null);
			        	}else{
			        		showMessageForSuccess("修改成功");
			        	}
			        }else{
			        	showMessageForFail("修改失败："+jsonObject.message);
			        }
				}
			});
		}
    }});
}
/** 账号删除功能 */
var doDeleteAccount=function(accountId){
	bootbox.confirm("确定要删除吗 ?", function(result) {
		if (result) {
			$.ajax({
				dataType:'JSON',
				type:"PUT",
				async:false,
				url:API.r_account_del,
				data:{
					id:accountId
				},
				success:function(jsonObject){
					if(-1==jsonObject.status){
						showMessageForFail("操作失败："+jsonObject.message);
						return;
					}else{
						showMessageForSuccess("删除成功");
					}
					//操作成功刷新数据
					showAccountList();
				}
			});
		}
	});
}

/** 已删除账号查询 */
var showAccountDelList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountDelPage .Current_Page").text();
	//封装data
	var $div = $("#accountDelFilter");
	var type=$div.find("[name=search_EQ_accountType]").val();
    if(!type){
    	type=typeALL;
    }
    var data = {
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize'),
        search_LIKE_account:$.trim($div.find("input[name='search_LIKE_account']").val()),
        search_LIKE_bankType:$.trim($div.find("[name='search_LIKE_bankType']").val()),
        search_LIKE_owner:$.trim($div.find("input[name='search_LIKE_owner']").val()),
        search_EQ_alias:$.trim($div.find("input[name='search_EQ_alias']").val()),
        statusToArray:[accountStatusDelete].toString(),
        handicapId:$div.find("select[name='search_EQ_handicapId']").val(),
        typeToArray:type.toString()
    };
    if($div.find("[name=search_EQ_accountType]").val()==accountTypeInBank){
    	data.search_EQ_subType=$div.find("[name='search_EQ_accountType'] option:selected").attr("subType");
    }
    //发送请求
	$.ajax({
        dataType:'JSON',
        type:"POST", 
		async:false,
        url:"/r/account/list4edit",
        data:data,
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			var $tbody=$("#accountDelListTable tbody");
			$tbody.html("");
			var idList=new Array();
			$.each(jsonObject.data,function(index,record){
				var tr="";
				tr+="<td><span>"+record.handicapName+"</span></td>";
				tr+="<td><span name='alias"+record.id+"'>"+((record.alias&&record.alias!='null')?record.alias:'')+"</span></td>";
				tr+="<td style='padding-left:0px;padding-right:0px;'>" +
						"<a class='bind_hover_card' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"
				record.bankType=record.bankType?record.bankType:'无';
                record.owner=record.owner?record.owner:'无';
				tr+="<span name='bankType_owner"+record.id+"' >"+record.bankType+"&nbsp;|&nbsp;"+record.owner+"&nbsp;|&nbsp;</span>";
				idList.push({'id':record.id});
				tr+=hideAccountAll(record.account);
				tr+=	"</a>" +
					"</td>";
				tr+="<td><span class=''>"+record.typeStr+"</span></td>";
				//操作
				tr+="<td>";
				//修改账号信息
				tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
						onclick='do_restoreDelAccount("+record.id+")' contentRight='UpdateSpecialAccount:RestoreDelAccount:*'>\
						<i class='ace-icon fa fa-undo bigger-100 orange'></i><span>恢复</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-inverse btn-bold contentRight' \
						onclick='do_delClearAccount("+record.id+")' contentRight='UpdateSpecialAccount:DeleteAndClear:*'>\
						<i class='ace-icon fa fa-trash-o bigger-100 dark'></i><span>彻底删除</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-info btn-bold "+OperatorLogBtn+" ' \
						onclick='showModal_accountExtra("+record.id+")' >\
						<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
						"onclick='showInOutListModal("+record.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+="</td>";
				$tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
			});
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.page,"accountDelPage",showAccountDelList,null,true);
			contentRight();
        }
	});
}
/** 已删除账号恢复 */
var do_restoreDelAccount=function(accountId){
	bootbox.confirm("确定恢复账号到停用状态?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/restoreDelAccount',
				async:false,
				data:{
					"accountId":accountId
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	showAccountDelList();
			        	showMessageForSuccess("恢复成功");
			        }else{
			        	showMessageForFail("恢复成功："+jsonObject.message);
			        }
				}
			});
		}
    });
}
/** 彻底删除账号 */
var do_delClearAccount=function(accountId){
	bootbox.confirm("确定彻底清除账号?<br/> <span class='red bolder'>【一经删除  不可恢复！！！】<br/>【请先确认账号是否已从工具中移除】</span>", function(result) {
		if (result) {
			$.ajax({
				type:"POST",
				dataType:'JSON',
				url:'/r/account/deleteAndClear',
				async:false,
				data:{
					"id":accountId
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	showMessageForSuccess("已成功清除账号信息！");
		        		showAccountDelList();
			        }else{
			        	showMessageForFail(jsonObject.message);
			        }
				}
			});
		}
	});
}
contentRight();
getHandicap_select($("#accountFilter select[name='search_EQ_handicapId']"),null,"全部");
getAccountType_select_search($("#accountFilter select[name='search_EQ_accountType']"),"全部");
getBankTyp_select($("#accountFilter select[name='search_LIKE_bankType']"),null,"全部");
$("#accountFilter").find("[name=search_EQ_handicapId],[name='search_EQ_accountType'],[name='search_LIKE_bankType']").change(function(){
	showAccountList();
});
$("#accountFilter").find("[name=search_IN_status],[name=search_EQ_flag]").click(function(){
	showAccountList();
});
$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		showAccountList();
	}
});


getHandicap_select($("#accountDelFilter select[name='search_EQ_handicapId']"),null,"全部");
getBankTyp_select($("#accountDelFilter select[name='search_LIKE_bankType']"),null,"全部");
getAccountType_select_search($("#accountDelFilter select[name='search_EQ_accountType']"),"全部");
$("#accountDelFilter").find("[name=search_EQ_handicapId],[name='search_EQ_accountType'],[name='search_LIKE_bankType']").change(function(){
	showAccountDelList();
});
$("#accountDelFilter").keypress(function(e){
	if(event.keyCode == 13) {
		showAccountDelList();
	}
});
