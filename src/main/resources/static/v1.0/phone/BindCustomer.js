
/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountPage .Current_Page").text();
	//封装data
	var $div = $("#accountFilter");
    var search_LIKE_account = $div.find("[name='search_LIKE_account']").val();
    var search_LIKE_bankType = $div.find("[name='search_LIKE_bankType']").val();
    var search_LIKE_auditor = $div.find("input[name='search_LIKE_auditor']").val();
    var search_LIKE_owner = $div.find("input[name='search_LIKE_owner']").val();
    var statusToArray = new Array();
    $div.find("input[name='search_IN_status']:checked").each(function(){
        statusToArray.push(this.value);
    });
    if(statusToArray.length==0){
        statusToArray=[accountStatusNormal,accountStatusStopTemp,accountStatusEnabled];
    }
    var fromIdArray=new Array(),tdName='Outdetail',tdIncomeName='incomeDetail',tdEncashName="encash";
    var data = {
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize'),
        search_IN_handicapId:$div.find("select[name='search_EQ_handicapId']").val().toString(),
        search_LIKE_account:search_LIKE_account,
        search_LIKE_bankType:$.trim(search_LIKE_bankType),
        auditor:$.trim(search_LIKE_auditor),
        search_LIKE_owner:$.trim(search_LIKE_owner),
        statusToArray:statusToArray.toString(),
        typeToArray:[accountTypeBindCustomer].toString(),
        currSysLevel:$div.find("input[name='currSysLevel']:checked").val(),
        sortProperty:'bankBalance',
        sortDirection:1
    };
    //发送请求
	$.ajax({
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:API.r_account_list,
        data:data,
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			var $tbody=$("table#accountListTable").find("tbody");
			$tbody.html("");
			var totalBalanceByBank =0 ,idList=new Array(),idArray = new Array();
			$.each(jsonObject.data,function(index,record){
				fromIdArray.push(record.id);
				idList.push({'id':record.id});
				idArray.push(record.id);
				var tr="";
				tr+="<td><span>"+record.handicapName+"</span></td>";
				tr+="<td><span>"+_checkObj(record.currSysLevelName)+"</span></td>";
				tr+="<td><span>"+_checkObj(record.alias)+"</span></td>";
				record.bankType=record.bankType?record.bankType:'无';
                record.owner=record.owner?record.owner:'无';
				tr+="<td style='padding-left:0px;padding-right:0px;'>" +
						"<a class='bind_hover_card' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"
				tr+=record.bankType+"&nbsp;|&nbsp;"+record.owner+"<br/>";
				tr+=hideAccountAll(record.account);
				tr+=	"</a>" +
					"</td>";	
				tr+="<td><span>"+hideAccountAll(record.mobile,true)+"</span></td>";
				//不同状态使用不同颜色
				if(record.status==accountStatusFreeze || record.status==accountStatusStopTemp){
					tr+="<td><span class='label label-sm label-danger'>"+record.statusStr+"</span></td>";
				}else{
					tr+="<td><span class='label label-sm label-success'>"+record.statusStr+"</span></td>";
				}
				!record.bankBalance?record.bankBalance=0:null;
				tr+="<td><div class='BankLogEvent' target='"+record.id+"'><span name='bankBalance' class='amount'>"+record.bankBalance+htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily)+"</span><span class='time'></span></div></td>";
				tr+='<td target="flowIn'+record.id+'"><a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-success" title="已匹配" name="mapping">0</span></a></td>'
				tr+='<td target="recdOut'+record.id+'"><a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-warning" title="匹配中" name="mapping">0</span></a><a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-success" title="已匹配" name="mapping">0</span></a></td>'
				//操作/**/
				tr+="<td>";
				tr+="<button class='btn btn-xs btn-white btn-warning btn-bold' \
						onclick='showModal_updateAccount("+record.id+")' >\
					<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
						"onclick='showInOutListModal("+record.id+")'>"+
					"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+="</td>";
				$tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
				totalBalanceByBank+=record.bankBalance*1;
			});
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var totalRows={
						column:12, 
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements
					};
				totalRows[7]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			//分页初始化
			showPading(jsonObject.page,"accountPage",showAccountList,null,false,true);
			contentRight();
			showStat4BindCustomer(idArray);
        }
	});
};

var showStat4BindCustomer = function(idArray){
	$.ajax({dataType:'JSON',url:'/r/account/findStat4BindCustomer',data:{accountIdArray:idArray.toString()},success:function(jsonObject){
			if(!jsonObject.status||jsonObject.status!=1||!jsonObject.data||jsonObject.data.length==0){
				return;
			}
		    for(var accId in  jsonObject.data){
				var item = jsonObject.data[accId];
				var inCount = item[0];
				var oMappingCount = item[1];
				var oMappedCount = item[2];
			    var flowInHtml = '<a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-success" title="请参考转入流水明细">'+inCount+'</span></a>';
				$('td[target=flowIn'+accId+']').html(flowInHtml);
				var recdOutHtml = '<a style="text-decoration:none;" target="_self" href="#/EncashCheck4Outward:*?fromAccountId='+accId+'&amp;outwardTaskStatus=1"><span class="badge badge-warning" title="匹配中">'+oMappingCount+'</span></a>&nbsp;&nbsp;<a style="text-decoration:none;" target="_self" href="#/EncashCheck4Outward:*?fromAccountId='+accId+'&amp;outwardTaskStatus=5"><span class="badge badge-success" title="已匹配">'+oMappedCount+'</span></a>';
				$('td[target=recdOut'+accId+']').html(recdOutHtml);
			}
	}});
};

/**
 * 展示修改模态窗 修改账号信息
 */
var showModal_updateAccount=function(accountId){
	var accountInfo=getAccountInfoById(accountId);
	var $div=$("#updateAccount");
	// 表单填充值
	$div.find("#accountId").val(accountId);
	if(accountInfo){
	    accountInfo.bankType=accountInfo.bankType?accountInfo.bankType:'无';
	    accountInfo.owner=accountInfo.owner?accountInfo.owner:'无';
	    $div.find("[name=accountInfo]").html((accountInfo.bankType+"&nbsp;|&nbsp;"+accountInfo.owner+"&nbsp;|&nbsp;"+hideAccountAll(accountInfo.account)));
	    $div.find("[name='limitIn']").val(accountInfo.limitIn);
	    $div.find("[name='limitOut']").val(accountInfo.limitOut);
	    $div.find("[name='limitOutOne']").val(accountInfo.limitOutOne);
	    $div.find("[name='limitOutOneLow']").val(accountInfo.limitOutOneLow);
	    $div.find("[name='limitOutCount']").val(accountInfo.limitOutCount);
//	    $div.find("[name=flag][value="+accountInfo.flag+"]").prop("checked",true);
//	    if(accountInfo.flag&&accountInfo.flag==accountFlagMobile){
//		    $div.find("[name='mobile']").val(accountInfo.mobile);
//	    }
		// 初始化盘口
		getHandicap_select($div.find("select[name='handicap_select']"),accountInfo.handicapId);
	}else{
		// 提示数据不存在
		showMessageForFail("操作失败：账号不存在");
	}
	$div.modal("toggle");
}

var do_updateAccount=function(){
	var $div=$("#updateAccount");
    var accountId = $div.find("#accountId").val();
    if(!accountId) return;
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $limitOutOne = $div.find("input[name='limitOutOne']");
	var $limitOutOneLow = $div.find("input[name='limitOutOneLow']");
	var $limitOutCount = $div.find("input[name='limitOutCount']");
//    var $flag = $div.find("[name='flag']:checked");
    var data={
			"id":accountId,
			"limitIn":$.trim($limitIn.val()),
			"limitOut":$.trim($limitOut.val()),
			"limitOutOne":$.trim($limitOutOne.val()),
			"limitOutOneLow":$limitOutOneLow.val(),
			"limitOutCount":$limitOutCount.val()
//			"flag":$.trim($flag.val())
		};
    var validatePrint=[
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$limitOutOne,name:'最高单笔出款限额',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitOutOneLow,name:'最低单笔出款限额',minEQ:0,maxEQ:50000},
    	{ele:$limitOutCount,name:'当日出款笔数',minEQ:0,maxEQ:500}
    ];
//	if($flag.val()==accountFlagMobile){
//        var $mobile = $div.find("input[name='mobile']");
//		//来源是手机时必填手机号
//		if(!validateEmptyBatch([{ele:$mobile,name:'手机号'}])){
//	    	return;
//	    }
//		data["mobile"]=$.trim($mobile.val());
//	}
    if(!validateInput(validatePrint)){
    	return;
    }
	bootbox.confirm("确定更新此账号信息?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/update',
				async:false,
				data:data,
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	showAccountList();
			            $div.modal("hide");
			        }else{
			        	showMessageForFail("账号修改失败："+jsonObject.message);
			        }
			    }
			});
		}
	});
}

getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #searchBtn"),150,"refresh_accountIncomp");

$("#updateAccount").find("#tableUpdate td").css("padding-top","10px");
$("#updateAccount").find("#tableUpdate td.noPaddingTop").css("padding-top","0px");
$("#updateAccount").find("#do_update").bind("click",function(){
	do_updateAccount();
});

$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		$("#accountFilter #searchBtn button").click();
	}
});
$("#accountFilter").find("[name=search_EQ_handicapId],[name=search_EQ_LevelId],[name=search_LIKE_account],[name=search_LIKE_bankType]").change(function(){
	showAccountList();
});
$("#accountFilter").find("[name=currSysLevel],[name=search_IN_status],#searchBtn").click(function(){
	showAccountList();
});
showAccountList(0);