var typeALL=[accountTypeInBank,accountTypeOutBank,accountTypeReserveBank,accountTypeBindWechat,accountTypeBindAli,accountTypeThirdCommon,accountTypeBindCommon,accountTypeBindCustomer];

/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountPage .Current_Page").text();
	//封装data
	var $div = $("#accountFilter");
    var search_LIKE_account = $div.find("[name='search_LIKE_account']").val();
    var search_LIKE_bankType = $div.find("[name='search_LIKE_bankType']").val();
    var search_LIKE_owner = $div.find("input[name='search_LIKE_owner']").val();
    var type=$div.find("[name=search_EQ_accountType]").val();
    if(!type){
    	type=typeALL;
    }
    var data = {
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize'),
        search_IN_handicapId:$div.find("select[name='search_EQ_handicapId']").val().toString(),
        search_LIKE_account:search_LIKE_account,
        search_LIKE_bankType:$.trim(search_LIKE_bankType),
        search_LIKE_owner:$.trim(search_LIKE_owner),
        statusToArray:[accountStatusExcep].toString(),
        typeToArray:type.toString(),
        currSysLevel:$div.find("input[name='currSysLevel']:checked").val()
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
				tr+=record.bankType+"&nbsp;|&nbsp;"+record.owner+"&nbsp;|&nbsp;";
				tr+=hideAccountAll(record.account);
				tr+=	"</a>" +
					"</td>";	
				tr+="<td><span class='label label-sm label-danger'>"+record.statusStr+"</span></td>";
				tr+="<td>"+record.typeStr+"</td>";
				!record.bankBalance?record.bankBalance=0:null;
				tr+="<td><div class='BankLogEvent' target='"+record.id+"'><span name='bankBalance' class='amount'>"+record.bankBalance+htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily)+"</span><span class='time'></span></div></td>";
				//操作/**/
				tr+="<td>";
				tr+="<button class='btn btn-xs btn-white btn-warning btn-bold orange' " +
						"onclick='showStopTempModal("+record.id+",showAccountList)'>"+
					"<i class='ace-icon fa fa-stop bigger-100 orange'></i><span>停用</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-danger btn-bold red' " +
						"onclick='showFreezeModal("+record.id+",showAccountList)'>"+
					"<i class='ace-icon fa fa-remove bigger-100 red'></i><span>冻结</span></button>";
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
			showPading(jsonObject.page,"accountPage",showAccountList);
			contentRight();
			SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
        }
	});
};

getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
getAccountType_select($("select[name='search_EQ_accountType']"),null,"全部");
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部");
initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #searchBtn"),150,"refresh_accountIncomp");


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