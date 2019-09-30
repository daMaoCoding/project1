currentPageLocation = window.location.href;
var typeALL=[accountTypeInBank,accountTypeOutBank,accountTypeReserveBank,accountTypeBindWechat,accountTypeBindAli,accountTypeThirdCommon,accountTypeBindCommon,accountTypeBindCustomer];
var developType = 1 ;//1：额度提升 2：卡数提升
var tabType = 1;
function changeTab(tab){
	tabType = tab;
	showRebateUser(0);
}
function SpanGrid(tabObj,colIndex){
	if(tabObj != null){
		var i,j;
		var intSpan;
		var strTemp;
		for(i = 0; i < tabObj.rows.length; i++){
			intSpan = 1;
			strTemp = tabObj.rows[i].cells[colIndex].innerText;
			if(strTemp==""||strTemp==null){
				continue;
			}
			for(j = i + 1; j < tabObj.rows.length; j++){
				if(strTemp == tabObj.rows[j].cells[colIndex].innerText){
					intSpan++;
					tabObj.rows[i].cells[0].rowSpan  = intSpan;
					tabObj.rows[i].cells[7].rowSpan  = intSpan;
					tabObj.rows[i].cells[6].rowSpan  = intSpan;
					tabObj.rows[i].cells[8].rowSpan  = intSpan;
					tabObj.rows[i].cells[9].rowSpan  = intSpan;
					var height=(intSpan+2);
					var ht=intSpan;
					//设置文本居中
					tabObj.rows[i].cells[0].style="line-height:"+height;
					tabObj.rows[i].cells[7].style="line-height:"+height;
					tabObj.rows[i].cells[6].style="line-height:"+height;
					tabObj.rows[i].cells[8].style="line-height:"+height;
					tabObj.rows[i].cells[9].style="line-height:"+height;
					tabObj.rows[j].cells[0].style.display = "none";
					tabObj.rows[j].cells[7].style.display = "none";
					tabObj.rows[j].cells[6].style.display = "none";
					tabObj.rows[j].cells[8].style.display = "none";
					tabObj.rows[j].cells[9].style.display = "none";
				}
				else
				{
					break;
				}
			}
			i = j - 1;
		}
	}
}

function SpanGrid1(tabObj,colIndex){
	if(tabObj != null){
		var i,j;
		var intSpan;
		var strTemp;
		for(i = 0; i < tabObj.rows.length; i++){
			intSpan = 1;
			strTemp = tabObj.rows[i].cells[colIndex].innerText;
			if(strTemp==""||strTemp==null){
				continue;
			}
			for(j = i + 1; j < tabObj.rows.length; j++){
				if(strTemp == tabObj.rows[j].cells[colIndex].innerText){
					intSpan++;
					tabObj.rows[i].cells[0].rowSpan  = intSpan;
					tabObj.rows[i].cells[1].rowSpan  = intSpan;
					tabObj.rows[i].cells[2].rowSpan  = intSpan;
					var height=(intSpan+2);
					var ht=intSpan;
					//设置文本居中
					tabObj.rows[i].cells[0].style="line-height:"+height;
					tabObj.rows[i].cells[1].style="line-height:"+height;
					tabObj.rows[i].cells[2].style="line-height:"+height;
					tabObj.rows[j].cells[0].style.display = "none";
					tabObj.rows[j].cells[1].style.display = "none";
					tabObj.rows[j].cells[2].style.display = "none";
				}
				else
				{
					break;
				}
			}
			i = j - 1;
		}
	}
}


function derating(id,margin){
	$('#typeName').empty().html("降额");
	$('#contn').empty().html("降低金额");
	$("#Remark").val("");
	$("#derating_amount").val("");
	$('#showTips').hide();
	$('#Remark_modal').modal('show');
	$('#totalTaskFinishBTN').attr('onclick', 'save_derating('+id+','+margin+');');
}

function deductAmount(uid){
	$('#typeName').empty().html("扣除佣金");
	$('#contn').empty().html("扣除佣金");
	$("#Remark").val("");
	$("#derating_amount").val("");
	$('#Remark_modal').modal('show');
	$('#showTips').show();
	$('#totalTaskFinishBTN').attr('onclick', 'save_deductAmount('+uid+');');
}

function save_derating(id,margin){
	$('#totalTaskFinishBTN').attr("disabled",true);
	var remark=$.trim($("#Remark").val());
	if(remark==""){
		$('#prompt_remark').show(10).delay(1500).hide(10);
		$('#totalTaskFinishBTN').attr("disabled",false);
		return;
	}
	var amount=$.trim($("#derating_amount").val());
	if(amount>margin||amount==""){
		$('#prompt_derating_amount').show(10).delay(1500).hide(10);
		$('#totalTaskFinishBTN').attr("disabled",false);
		return;
	}

	$.ajax({
		async:true,
		type:'post',
		url:'/r/account/derating',
		data:{'id':id,'amount':amount,'remark':remark},
		dataType:'json',
		success:function (res) {
			if(res.status == 1){
				$('#totalTaskFinishBTN').attr("disabled",false);
				$('#Remark_modal').modal('hide');
				showRebateUser(0);
				$("#Remark").val("");
			}else{
				$('#totalTaskFinishBTN').attr("disabled",false);
				showMessageForFail(res.message);
			}
		}
	});
}

function save_deductAmount(uid){
	$('#totalTaskFinishBTN').attr("disabled",true);
	var remark=$.trim($("#Remark").val());
	if(remark==""){
		$('#prompt_remark').show(10).delay(1500).hide(10);
		$('#totalTaskFinishBTN').attr("disabled",false);
		return;
	}
	var amount=$.trim($("#derating_amount").val());
	if(amount==""){
		$('#prompt_derating_amount').show(10).delay(1500).hide(10);
		$('#totalTaskFinishBTN').attr("disabled",false);
		return;
	}

	$.ajax({
		async:true,
		type:'post',
		url:'/r/account/deductamount',
		data:{'uid':uid,'amount':amount,'remark':remark},
		dataType:'json',
		success:function (res) {
			if(res.status == 1){
				$('#totalTaskFinishBTN').attr("disabled",false);
				$('#Remark_modal').modal('hide');
				showRebateUser(0);
				$("#Remark").val("");
			}else{
				$('#totalTaskFinishBTN').attr("disabled",false);
				showMessageForFail(res.message);
			}
		}
	});
}


//兼职信息页签
var information=false;
//降额按钮
var deratingButton=false;

$.each(ContentRight['AccountAuditNew:*'], function (name, value) {
	if (name == 'AccountAuditNew:information:*') {
		information = true;
	}
	if (name == 'AccountAuditNew:derating:*') {
		deratingButton = true;
	}
});


$("[name=table3_search_IN_status]").click(function(){
	showRebateUser(0);
});
$("[name=table3_search_LIKE_bankType]").change(function(){
	showRebateUser(0);
});
var showRebateUser=function(CurPage){
	if(tabType == 1){
		if(!!!CurPage&&CurPage!=0) CurPage=$("#accountList_page .Current_Page").text();
	}else{
		if(!!!CurPage&&CurPage!=0) CurPage=$("#tab3_table_people_page .Current_Page").text();
	}
	var $div = $("#tab3_table_people");
	var statusToArray = new Array();
	var handicapId="",alias="",currSysLevel="",bankType="",account="",owner="",rebateUser="",type="",subType="",endAmount="";
	var tabStatus = $("#tabStatus").val();
	if(tabType == 1){
		$div = $("#tab1");
		if(tabStatus == 1){
			statusToArray.push(accountInactivated);
		}else if(tabStatus == 2){
			statusToArray.push(accountActivated);
		}else{
			$div.find("[name='search_IN_status']:checked").each(function(){
				statusToArray.push(this.value);
			});
			if(statusToArray.length==0){
				statusToArray=[accountStatusNormal,accountStatusStopTemp,accountStatusEnabled,accountStatusFreeze];
			}
		}
		if(tabStatus == 4){
			endAmount = "1000";
		}
		handicapId=$div.find("select[name='search_EQ_handicapId']").val();
		alias=$.trim($div.find("input[name='search_EQ_alias']").val());
		currSysLevel=$div.find("input[name='currSysLevel']:checked").val();
		bankType=$div.find("[name='search_LIKE_bankType']").val()||"";
		account=$.trim($div.find("[name='search_LIKE_account']").val())||"";
		owner=$.trim($div.find("[name='search_LIKE_owner']").val())||"";
		rebateUser=$.trim($div.find("[name='rebateUsername']").val())||"";
		type=$div.find("[name=search_EQ_accountType]").val();
		if(type==accountTypeInBank){
			subType=$div.find("[name='search_EQ_accountType'] option:selected").attr("subType");
		}
	}else{
		$div.find("[name='table3_search_IN_status']:checked").each(function(){
			statusToArray.push(this.value);
		});
		if(statusToArray.length==0){
			statusToArray=[accountStatusNormal,accountStatusStopTemp,accountStatusEnabled,accountStatusFreeze,accountStatusDelete,accountInactivated,accountActivated];
		}
		bankType=$div.find("[name='table3_search_LIKE_bankType']").val()||"";
		account=$.trim($div.find("[name='search_LIKE_account']").val())||"";
		owner=$.trim($div.find("[name='search_LIKE_owner']").val())||"";
		rebateUser=$.trim($div.find("[name='rebateUsername']").val())||"";
	}
	if(type==""){
		type=typeALL;
	}
	var data = {
		handicapId:handicapId,
		alias:alias,
		currSysLevel:currSysLevel,
		bankType:bankType,
		typeToArray:type.toString(),
		statusToArray:statusToArray.toString(),
		account:account,
		owner:owner,
		subType:subType,
		rebateUser:rebateUser,
		endAmount:endAmount,
		pageNo:CurPage<=0?0:CurPage-1,
		pageSize:$.session.get('initPageSize')
	};
	var requesturl = "/r/account/showRebateUser";
	//发送请求
	$.ajax({
		dataType:'JSON',
		type:"POST",
		async:false,
		url:requesturl,
		data:data,
		success:function(jsonObject){
			//小计
			var counts=0;
			var tr = '';
			var idList=new Array();
			var bankBalanceSubTotal=0,marginSubTotal=0;
			var tmpUserName="";
			for(var index in jsonObject.data.arrlist){
				var val = jsonObject.data.arrlist[index];
				if(tmpUserName != val.userName){
					marginSubTotal = marginSubTotal + _checkObj(val.margin,null,0);
				}
				tmpUserName=val.userName;
				bankBalanceSubTotal = bankBalanceSubTotal + _checkObj(val.bankBalance,null,0);
				idList.push({'id':val.id});
				tr += '<tr>' +'<td>' + val.userName + '</td>';
				if(tabType == 1){
					tr += '<td>'+ val.mobile + '</td>'
						+'<td>' + val.margin + '</td>';
				}else{
					tr += '<td>'+ val.handicapName +'</td>';
					
				}
				tr +='<td>' + val.currSysLevelName + '</td>'
				+'<td>' + (null==val.alias?"":val.alias) + '</td>';
				tr+="<td>" +getAccountInfoHoverHTML(val) +"</td>";
				if(val.status==accountStatusFreeze){
					tr+="<td><span class='label label-sm label-danger'>"+val.createTimeStr+"</span></td>";
				}else if(val.status==accountStatusStopTemp){
					tr+="<td><span class='label label-sm label-primary'>"+val.createTimeStr+"</span></td>";
				}else if(val.status==accountInactivated){
					tr+="<td><span class='label label-sm label-warning'>"+val.createTimeStr+"</span></td>";
				}else if(val.status==accountStatusExcep){
					tr+="<td><span class='label label-sm label-danger'>"+val.createTimeStr+"</span></td>";
				}else if(val.status==accountStatusDelete){
					tr+="<td><span class='label label-sm label-inverse'>"+val.createTimeStr+"</span></td>";
				}else{
					tr+="<td><span class='label label-sm label-success'>"+val.createTimeStr+"</span></td>";
				}
				if(tabType == 1){
					tr += '<td>'+val.bankBalance+'</td>';
					tr += '<td>';
					if(tabStatus == 1){
						//未激活
						tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAuditNew:Activation:*'  \
							onclick='activeAccount("+val.id+")'>\
							<i class='ace-icon fa fa-check-square-o bigger-100 orange'></i><span>激活</span></button>";
					}else if(tabStatus == 2){
						//已激活
						tr += "<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAuditNew:Enable:*'  \
							onclick='showUpdateflag2Account("+val.id+",showRebateUser,true)'>\
							<i class='ace-icon fa fa-exchange bigger-100 red'></i><span>启用</span></button>";
					}else{
						//全部
						tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAuditNew:Update:*' \
							onclick='showUpdateAccountNewModal("+val.id+",showRebateUser)'>\
							<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
					}
					tr+="<button class='btn btn-xs btn-white btn-success btn-bold "+isHideAccountBtn+"'  \
	                onclick='showModal_accountBaseInfo("+val.id+")'>\
	                <i class='ace-icon fa fa-eye bigger-100 green'></i><span>状态</span></button>";
				}else if(tabType == 2){
					if (_checkObj(val.remark)) {
						if (_checkObj(val.remark).length > 23) {
							tr += '<td>'
								+ '<a  class="bind_hover_card breakByWord"  title="备注信息"'
								+ 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
								+ ' data-content="' + val.remark + '">'
								+ val.remark.replace(/<br>/g, "").substring(0, 4)
								+ '</a>'
								+ '</td>';
							
						} else {
							tr += '<td>'
								+ '<a class="bind_hover_card breakByWord"  title="备注信息"'
								+ 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
								+ ' data-content="' + val.remark + '">'
								+ val.remark
								+ '</a>'
								+ '</td>';
						}
					} else {
						tr += '<td></td>';
					}
					tr+='<td>' + val.margin + '</td>'
					+'<td>' + val.limitPercentage + '</td>'
					+ '<td>'
					+ '<a  class="bind_hover_card breakByWord"  title="扣佣金信息"'
					+ 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
					+ ' data-content="' + val.city + '">'
					+ val.minInAmount
					+ '</a>'
					+ '</td>';
					tr+='<td>';
					if(val.userName!=""&&val.userName!=null&&val.margin>1000 && deratingButton){
						tr+='<button type="button" onclick="derating('+val.id+','+val.margin+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-reply  bigger-100 red"></i>降额</button>';
					}
					if(val.userName!=""&&val.userName!=null && deratingButton){
						tr+='<button type="button" onclick="deductAmount('+val.uid+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon glyphicon glyphicon-minus  bigger-100 orange"></i>扣除佣金</button>';
					}
					if(deratingButton){
						tr+='<button type="button" onclick="resetbankAmount('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon glyphicon glyphicon-time  bigger-100 green"></i>余额清零</button>';
					}
					if(deratingButton){
						tr+='<button type="button" onclick="recalculate('+val.uid+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-exchange bigger-100 orange"></i>计算额度</button>';
					}
				}
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold "+OperatorLogBtn+"' " +
				"onclick='showModal_accountExtra("+val.id+")'>"+
				"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
				"onclick='showInOutListModal("+val.id+")'>"+
				"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+='</td>'+'</tr>';
				counts +=1;
			};
			var $tbody=$div.find("tbody").html(tr);
			//有数据时，显示总计 小计
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				//用户名分组  total_tbody_match  tbody的节点ID
				if(tabType == 1){
					SpanGrid1(account_list_table,0);
					var trs = '<tr>'
						+'<td>小计：'+counts+'</td>'
						+'<td></td>'
						+'<td bgcolor="#579EC8" style="color:white;">'+marginSubTotal.toFixed(2)+'</td>'
						+'<td colspan="4"></td>'
						+'<td bgcolor="#579EC8" style="color:white;">'+bankBalanceSubTotal.toFixed(2)+'</td>'
						+'<td></td>'
						+'</tr>';
					$tbody.append(trs);
					var trn = '<tr>'
						+'<td>总计：'+jsonObject.data.page.totalElements+'</td>'
						+'<td></td>'
						+'<td bgcolor="#D6487E" style="color:white;">'+(jsonObject.data.marginTotal==null?0:jsonObject.data.marginTotal.toFixed(2))+'</td>'
						+'<td colspan="4"></td>'
						+'<td bgcolor="#D6487E" style="color:white;">'+(jsonObject.data.bankBalanceTotal==null?0:jsonObject.data.bankBalanceTotal.toFixed(2))+'</td>'
						+'<td></td>'
						+'</tr>';
				}else{
					SpanGrid(total_tbody_match,0);
					var trs = '<tr>'
						+'<td colspan="11">小计：'+counts+'</td>'
						+'</tr>';
					$tbody.append(trs);
					var trn = '<tr>'
						+'<td colspan="11">总计：'+jsonObject.data.page.totalElements+'</td>'
						+'</tr>';
				}
			}
			$tbody.append(trn);
			if(tabType == 1){
				showPading(jsonObject.data.page,"accountList_page",showRebateUser,null,true);
			}else{
				showPading(jsonObject.data.page,"tab3_table_people_page",showRebateUser,null,true);
			}
			$("[data-toggle='popover']").popover();
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
		}
	});
}



/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	if(information) $('#info').show();
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountList_page .Current_Page").text();
	//封装data
	var $div = $("#accountFilter");
	var statusToArray = new Array();
	if($("#tabStatus").val()*1==1){
		//未激活
		statusToArray=[accountInactivated];
	}else if($("#tabStatus").val()*1==2){
		//已激活
		statusToArray=[accountActivated];
	}else{
		//待提额 汇总
		$div.find("input[name='search_IN_status']:checked").each(function(){
			statusToArray.push(this.value);
		});
		if(statusToArray.length==0){
			statusToArray=[accountStatusNormal,accountStatusFreeze,accountStatusExcep,accountStatusStopTemp,accountStatusEnabled];
		}
	}
	var type=$div.find("[name=search_EQ_accountType]").val();
	if(!type){
		type=typeALL;
	}
	var data = {
		search_LIKE_account:$.trim($div.find("input[name='search_LIKE_account']").val()),
		search_LIKE_bankType:$.trim($div.find("[name='search_LIKE_bankType']").val()),
		search_LIKE_owner:$.trim($div.find("input[name='search_LIKE_owner']").val()),
		search_EQ_alias:$.trim($div.find("input[name='search_EQ_alias']").val()),
		currSysLevel:$div.find("input[name='currSysLevel']:checked").val(),
		statusToArray:statusToArray.toString(),
		typeToArray:type.toString(),
		search_EQ_flag:$div.find("input[name='search_EQ_flag']:checked").val(),
		search_NOTEQ_flag:0,//不查PC
		pageNo:CurPage<=0?0:CurPage-1,
		pageSize:$.session.get('initPageSize')
	};
	if($div.find("[name=search_EQ_accountType]").val()==accountTypeInBank){
		data.search_EQ_subType=$div.find("[name='search_EQ_accountType'] option:selected").attr("subType");
	}
	var requesturl = "";
	if($("#tabStatus").val()*1==4){
		//待提额
		requesturl = "/r/account/toberaised";
		data.handicapId=$div.find("select[name='search_EQ_handicapId']").val();
	}else{
		//未激活 已激活 汇总
		requesturl = "/r/account/list";
		data.search_IN_handicapId=$div.find("select[name='search_EQ_handicapId']").val();
		if(!data.search_IN_handicapId){
			data.search_IN_handicapId=handicapId_list.toString();
		}
	}
	//发送请求
	$.ajax({
		dataType:'JSON',
		type:"POST",
		async:false,
		url:requesturl,
		data:data,
		success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			//已匹配和汇总 增加信用额度
			var showActiving=($("#tabStatus").val()*1==1);
			var showActived=($("#tabStatus").val()*1==2);
			var showOther=($("#tabStatus").val()*1==3||$("#tabStatus").val()*1==4);
			$("#activing_show,#actived_show,#others_show").hide();
			if(showActiving){
				$("#activing_show").show();
			}else if(showActived){
				$("#actived_show").show();
			}else{
				$("#others_show").show();
			}
			var $tbody=$("#accountListTable tbody");
			$tbody.html("");
			var totalBalanceByBank =0 ,idList=new Array(),idArray = new Array();
			$.each(jsonObject.data,function(index,record){
				!record.bankBalance?record.bankBalance=0:null;
				totalBalanceByBank+=record.bankBalance*1;
				idArray.push(record.id);
				var tr="";
				tr+="<td><span>"+record.handicapName+"</span></td>";
				if(showOther) {
					tr += "<td><span>" + _checkObj(record.currSysLevelName) + "</span></td>";
				}
				tr+="<td><span>"+_checkObj(record.alias)+"</span></td>";
				//手机余额状态
				tr+="<td><span>"+hideAccountAll(record.mobile,true)+"</span><span target='"+record.id+"' class='time4St bal label label-grey' style='display:block;width:99%;float:left;' >离线</span></td>";
				idList.push({'id':record.id});
				tr+="<td>" +getAccountInfoHoverHTML(record) +"</td>";
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
				if(showOther) {
					tr += "<td><span class=''>" + record.typeStr + "</span></td>";
				}
				//余额
				tr+='<td>';
				tr+="<span>"+record.bankBalance+htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily)+"</span>";
				//手机流水状态
				tr+='<span class="errorAlarm error'+record.id+'"></span><span target="'+record.id+'" class="time4St flow label label-grey" style="display:block;width:99%;float:left;" >&nbsp;&nbsp;</span>';
				tr+='</td>';
				if(showOther || showActived){
					tr+="<td><span class=''>"+_checkPeakBalance(record.peakBalance)+"</span></td>";
				}
				//操作  修改/修改 冻结/激活 操作记录  明细
				tr+="<td>";
				var enableBtn="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAuditNew:Enable:*' \
                        onclick='showUpdateOutAccount("+record.id+",showAccountList,false,true)'>\
                        <i class='ace-icon fa fa-exchange bigger-100 red'></i><span>启用</span></button>";
				var enableLimitBtn="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAuditNew:Enable:*'  \
                        onclick='showUpdateflag2Account("+record.id+",showAccountList,true)'>\
                        <i class='ace-icon fa fa-exchange bigger-100 red'></i><span>启用</span></button>";
				if($("#tabStatus").val()*1==1){
					//未激活
					tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAuditNew:Activation:*'  \
                        onclick='activeAccount("+record.id+")'>\
                        <i class='ace-icon fa fa-check-square-o bigger-100 orange'></i><span>激活</span></button>";
				}else if($("#tabStatus").val()*1==2){
					//已激活
					if(record.flag&&record.flag*1==2){
						tr+=enableLimitBtn;
					}else{
						//其它账号
						tr+=enableBtn;
					}
				}else{
					//汇总
					tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAuditNew:Update:*' \
	                        onclick='showUpdateAccountNewModal("+record.id+",showAccountList)'>\
	                        <i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
				}
				tr+="<button class='btn btn-xs btn-white btn-success btn-bold "+isHideAccountBtn+"'  \
                onclick='showModal_accountBaseInfo("+record.id+")'>\
                <i class='ace-icon fa fa-eye bigger-100 green'></i><span>状态</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-info btn-bold "+OperatorLogBtn+" ' \
                        onclick='showModal_accountExtra("+record.id+")' >\
                        <i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
					"onclick='showInOutListModal("+record.id+")'>"+
					"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+="</td>";
				$tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
			});
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var status = $("#tabStatus").val();
				var totalRows;
				if(status == 1 || status == 2){
					totalRows={
						column:12,
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements
					};
					totalRows[6]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
				}else{
					totalRows={
						column:13,
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements
					};
					totalRows[8]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
				}
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.page,"accountList_page",showAccountList,null,true);
			contentRight();
			rereshAccSt(idArray);
		}
	});
}

var rereshAccSt = function(idList){
	$.ajax({type:'get',url:'/r/cabana/status',data: {accIdArray:idList.toString()},dataType:'json',success:function (res) {
			if (res.status==1&&res.data&&res.data.length>0) {
				var nowTm = new Date().getTime();
				$('span.errorAlarm').html('');
				$('span.time4St.bal').text('离线').attr('class','time4St bal label label-grey').attr('style','display:block;width:99%;float:left;');
				$('span.time4St.flow').html('&nbsp;&nbsp;').attr('class','time4St flow label label-grey').attr('style','display:block;width:99%;float:left;');
				res.data.forEach(function(val,index) {
					if(val.error){
						var error = '';
						error = error + '<span class="badge badge-transparent tooltip-error" title="'+val.error+'" onclick="doError('+val.id+',\''+val.error+'\');">';
						error = error + '  <i class="ace-icon fa fa-exclamation-triangle red bigger-130"></i>';
						error = error + '</span>';
						$('span.errorAlarm.error'+val.id).html(error);
					}
					{
						var classInf = val.time ==0||(val.time > 0 && (nowTm-val.time)< 180000)?'label-success':'label-warning';
						if(val.time && val.time > 0 &&((nowTm-val.time)>= 600000)){
							classInf = 'label-danger';
						}
						$('span.time4St.bal[target='+val.id+']').each(function(){
							$(this).text(val.time >0?(geeTime4Crawl(val.time)):'已连接').attr('class','time4St bal label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
						});
					}
					{
						var classInf = val.logtime ==0||(val.logtime > 0 && (nowTm-val.logtime)< 180000)?'label-success':'label-warning';
						if(val.logtime && val.logtime > 0 &&((nowTm-val.logtime)>= 600000)){
							classInf = 'label-danger';
						}
						$('span.time4St.flow[target='+val.id+']').each(function(){
							$(this).html(val.logtime >0?(geeTime4Crawl(val.logtime)):'&nbsp;&nbsp;').attr('class','time4St flow label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
						});
					}
					$('span.mode4Acc[target='+val.id+']').text((val.time?(val.mode ==1 ?'抓流水':(val.mode ==2?'转账':'默认')):'---'));
					{
						var flag =  val.flag;//1-simulator, 0-phone
						if(flag && flag ==1){
							$('button[smstarget='+val.id+']').attr('class','btn btn-xs btn-white btn-primary btn-bold orange contentRight');
						}else{
							$('button[smstarget='+val.id+']').attr('class','dsn contentRight');
						}
					}
				});
			}
		}});
};



function activeAccount(accountId){
	var accountInfo = getAccountInfoById(accountId);
	bootbox.confirm("请先确保账户（"+accountInfo.bankType+"|"+ accountInfo.owner+"|"+ accountInfo.account.substr(accountInfo.account.length-4)+"）处于正常连接状态，确定向此账户推送一笔测试任务？", function(result) {
		if (result) {
			$.ajax({ dataType:'json',type:"get", url:'/r/account/getActiveTrans',data:{accountId:accountId},success:function(jsonObject){
					if(jsonObject.status == 1){
						if(jsonObject.data){
							var message = "账户（"+accountInfo.bankType+"|"+ accountInfo.owner+"|"+ accountInfo.account.substr(accountInfo.account.length-4)+"）将向"+"<br>"+"账户（"+
								jsonObject.data.bankType+"|"+ jsonObject.data.owner+"|"+ jsonObject.data.account.substr(jsonObject.data.account.length-4)+"）转一笔账"
							showMessageForSuccess(message,2000);
						}else{
							showMessageForSuccess("正在获取到测试转账任务！",2000);
						}
					}
				},error:function(result){  bootbox.alert(result); }});
		}
	});
}

function initAccount(accountId){
	var troubleshoot = "1";
	$.ajax({ dataType:'json',type:"get",async:false, url:'/r/problem/getSysErr',data:{accId:accountId},success:function(jsonObject){
			if(jsonObject.status == -1 && jsonObject.message == "存在待排查数据"){
				troubleshoot = "2";
			}
		},error:function(result){  bootbox.alert(result); }});
	if(troubleshoot == "2"){
		showMessageForSuccess("请移步 问题排查汇总——》账目排查 进行初始化！",2000);
	}else{
		bootbox.confirm("确定对账号ID（"+accountId+"）进行初始化？", function(result) {
			if (result) {
				$.ajax({ dataType:'json',type:"get", url:'/auto/test/initByAccountId',async:false,data:{id:accountId},success:function(jsonObject){
						if(jsonObject.status == 1){
							showMessageForSuccess("初始化账号成功！",2000);
						}
					},error:function(result){  bootbox.alert(result); }});
			}
		});
	}
}

function resetbankAmount(accountId){
	bootbox.confirm("确定对账号ID（"+accountId+"）进行余额清零？", function(result) {
		if (result) {
			$.ajax({ dataType:'json',type:"get", url:'/r/account/resetbankAmount',async:false,data:{id:accountId},success:function(jsonObject){
					if(jsonObject.status == 1){
						showMessageForSuccess(jsonObject.message,2000);
						showRebateUser(0);
					}
				},error:function(result){  bootbox.alert(result); }});
		}
	});
}

function recalculate(uid){
	$.ajax({ dataType:'json',type:"get", url:'/r/account/recalculate',async:false,data:{uid:uid},success:function(jsonObject){
			if(jsonObject.status == 1){
				showMessageForSuccess(jsonObject.message,2000);
				showRebateUser(0);
			}else{
				showMessageForFail(jsonObject.message,2000);
			}
		},error:function(result){  bootbox.alert(result); }});
}


function changeStatus(status) {
	$("#tabStatus").val(status);
	$("#rebateUser_show").hide();
	if(status==1){
		//未激活
		$("#hideShowHtml").hide();
	}else if(status==2){
		//已激活
		$("#hideShowHtml").hide();
	}else{
		//汇总 待提额
		$("#hideShowHtml").show();
	}
	showRebateUser(0);
}

function _checkPeakBalance(obj) {
	var ob = 0;
	if (obj) {
		ob = obj;
	}
	return ob;
}
function replacePos(strObj, pos, replacetext){
	var str = strObj.substr(0, pos-1) + replacetext + strObj.substring(pos, strObj.length);
	return str;
}
getHandicap_select($("#accountFilter select[name='search_EQ_handicapId']"),null,"全部");
getBankTyp_select($("#accountFilter select[name='search_LIKE_bankType']"),null,"全部");
getBankTyp_select($("[name='table3_search_LIKE_bankType']"),null,"全部");
getAccountType_select_search($("#accountFilter select[name='search_EQ_accountType']"),"全部");
$("#accountFilter").find("[name=search_EQ_handicapId],[name='search_EQ_accountType'],[name='search_LIKE_bankType']").change(function(){
	showRebateUser(0);
});
$("#accountFilter").find("[name=search_EQ_flag],[name='search_IN_status'],[name=currSysLevel]").click(function(){
	showRebateUser(0);
});
showRebateUser(0);

/**
 * 显示发展管理的数据
 */
var showDevelopList=function(CurPage,SortProperty,SortDirect){
    if(!!!CurPage&&CurPage!=0) CurPage=$("#developmentTable_page .Current_Page").text();
    //封装data
    var $div = $("#developFilter");
    var sortP = SortProperty?SortProperty:"margin";
    var sortD = SortDirect?SortDirect:2;
    var data = {
        'rebateUsername':$.trim($div.find("input[name='rebateUsername_develop']").val()),
        'marginMin':$div.find("input[name='marginMin_develop']").val(),
        'marginMax':$div.find("input[name='marginMax_develop']").val(),
        'currMarginMin':$div.find("input[name='currMarginMin_develop']").val(),
        'currMarginMax':$div.find("input[name='currMarginMax_develop']").val(),
        'totalRebateMin':$div.find("input[name='totalRebateMin_develop']").val(),
        'totalRebateMax':$div.find("input[name='totalRebateMax_develop']").val(),
		'developType':developType,
        sortProperty:sortP,
        sortDirection:sortD,
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize')
    };
    //发送请求
    $.ajax({
        dataType:'JSON',
        type:"POST",
        async:false,
        url:"/r/rebate/listDevelopManage",
        data:data,
        success:function(jsonObject){
            if(jsonObject.status !=1){
                if(-1==jsonObject.status){
                    showMessageForFail("查询失败："+jsonObject.message);
                }
                return;
            }
            $("#dev_activing_show").show();
            var $tbody=$("#developmentTable tbody");
            $tbody.html("");
            var idList=new Array(),idArray = new Array();
            var totalMargin=0,totalCurrMargin=0,totalLine=0,totalOutFlow=0,totalRebate=0,subCount=0;
            $.each(jsonObject.data,function(index,record){
                totalMargin = totalMargin + _checkObj(record.margin,null,0);
                totalCurrMargin = totalCurrMargin + _checkObj(record.margin,null,0) + _checkObj(record.tmpMargin,null,0);
                totalLine = totalLine + _checkObj(record.linelimit,null,0)+_checkObj(record.tmpMargin,null,0);
                totalOutFlow = totalOutFlow+_checkObj(record.totalOutFlow,null,0);
                totalRebate = totalRebate+_checkObj(record.totalRebate,null,0);
                subCount = subCount + 1;
                idArray.push(record.id);
                var tr="";
                tr+="<td><span>"+record.username+"</span></td>";
                tr+="<td><span>"+_checkObj(record.margin,null,0)+"</span></td>";
                tr+="<td><span>"+record.joinDays+"</span></td>";
                tr+="<td><span>"+_checkObj(record.totalOutFlow,null,0)+"</span></td>";
                tr+="<td><span>"+_checkObj(record.totalRebate,null,0)+"</span></td>";
                if(developType==1){
                	var isJoin = record.inFlwActivity&&record.inFlwActivity==1?"是":"否";
                    tr+="<td><span>"+isJoin+"</span></td>";
				}else{
                    tr+="<td><span>"+record.cardUsedOrStop+"</span></td>";
				}
                tr+="<td><a id='showModal_remark"+record.id+"'  title='"+_checkObj(record.remark)+"'>"+(record.remark?_checkObj(record.remark.substring(0,20)):"无备注，点击添加")+"</a></td>";//备注

                idList.push({'id':record.id});
                //操作  修改/修改 冻结/激活 操作记录  明细
                tr+="<td>";
                var updateBtn="<button class='btn btn-xs btn-white btn-warning btn-bold' \
                        onclick='showModalInfo_Contact("+record.uid+",\""+record.username+"\")'>\
                        <i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>联系方式</span></button>";
                var updateLimitBtn="<button class='btn btn-xs btn-white btn-warning btn-bold'\
                        onclick='showModal_dealRebate(\""+record.uid+"\","+record.margin+",\""+record.cardUsedOrStop+"\",\""+record.username+"\","+record.id+")'>\
                        <i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>处理</span></button>";
                tr+=updateBtn;
                tr+=updateLimitBtn;
                tr+="</td>";
                $tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
                $tbody.find("#showModal_remark"+record.id).bind("click",function(){
                    //绑定修改事件
                    var param={
                        businessId:record.uid,
                        type:"rebateUser",
                        remarkTitle:"返利网用户："+_checkObj(record.username),
                        delete_contentRight:"rebateUser:RemarkDelete:*",
                        fnName:showDevelopList
                    };
                    showModalInfo_remark(param);
                });
            });
            //有数据时，显示总计 小计
            if(jsonObject.page&&jsonObject.page.header){
            	var currHeader=jsonObject.page.header;
                var totalRows={
                    column:12,
                    subCount:jsonObject.data.length,
                    count:jsonObject.page.totalElements
                };
                totalRows[2]={subTotal:totalCurrMargin,total:currHeader.totalCurrMargin};
                totalRows[3]={subTotal:totalLine,total:currHeader.totalLineLimit};
                totalRows[4]={subTotal:totalMargin,total:currHeader.margin};
                totalRows[6]={subTotal:totalOutFlow,total:currHeader.totalOutFlow};
                totalRows[7]={subTotal:totalRebate,total:currHeader.totalRebate};
                showSubAndTotalStatistics4Table($tbody,totalRows);
            }
            //加载账号悬浮提示
            //loadHover_accountInfoHover(idList);
            //分页初始化
            showPading(jsonObject.page,"developmentTable_page",showDevelopList,null,true);
            contentRight();
            rereshAccSt(idArray);
        }
    });
}
var changeDevelopType=function(type){
    developType = type;
    if(type == 2){
        $("#developShowInfo").text("（注：请客服人员尽快与兼职人员联系，提升卡数）");
        $("#dev_counts").text("卡数（启用 / 停用)");
	}else{
        $("#developShowInfo").text("（注：请客服人员尽快与兼职人员联系，提升信用额度）");
        $("#dev_counts").text("参加优惠活动");
	}
    showDevelopList();
}
/**
var changeOrderBy=function (orderColumn,obj) {
	var $i=$(obj).find("i");
	var classStr=$i.attr("class");
	var sortD;
	if(classStr=="fa fa-angle-double-down"){
        $i.attr("class","fa fa-angle-double-up");
        sortD = 0;
	}else{
        $i.attr("class","fa fa-angle-double-down");
        sortD = 1;
	}
    showDevelopList(null,orderColumn,sortD);
}
 **/
var changeOrderBy=function (orderColumn,obj) {
    var $i=$(obj);
    var classStr=$i.attr("class");
    var sortD;
    if(classStr=="fa fa-angle-double-down"){
        sortD = 2;   //降序
    }else{
        sortD = 1;   //升序
    }
    showDevelopList(null,orderColumn,sortD);
}

// 显示设置页面
var showModal_setting=function(){
	var setting = "accountAuditMarginSetting";
    var keysArray;
	if(developType==2){
        setting = "accountAuditCardsSetting";
        keysArray = new Array("ACTIVE_CARDS_JOIN_DAYS_MARGIN");
	}else{
        keysArray = new Array("MARGIN_ZERO_DAYS_MORE","MARGIN_LESS_3000_DAYS_MORE","MARGIN_3000_10000_DAYS_MORE","MARGIN_ACTIVE_CARDS");
	}
	var result;
    $.ajax({async:false,type:"POST",url:"/r/set/findSettingByKeys",data:{"keysArray": keysArray.toString()},dataType:'json',success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail("获取入款信息异常，"+jsonObject.message);
        }else{
            result=jsonObject.data;
        }
    }});
    //发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
    $.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
    $.ajax({
        type:"GET",
        async:false,
        dataType:'html',
        url : "/"+sysVersoin+"/html/common/showInfoModal.html",
        success : function(html){
            var $div=$(html).find("#"+setting).clone().appendTo($("body"));
            $div.find("#tableAdd td").css("padding-top","10px");
            $div.find("#tableAdd td.noPaddingTop").css("padding-top","0px");
            //表单填充值
            $.each(result, function (index, record) {
            	if(record.propertyKey == "MARGIN_ZERO_DAYS_MORE"){
                    $div.find("input[name='margin_zero_days_more']").val(record.propertyValue);
                    if(record.isEnable != '0'){
                        $div.find("input:checkbox[name='margin_zero_days_more_enable'][value='1']").attr("checked", "checked");
					}
				}
                if(record.propertyKey == "MARGIN_LESS_3000_DAYS_MORE"){
                    $div.find("input[name='margin_less_3000_days_more']").val(record.propertyValue);
                    if(record.isEnable != '0'){
                        $div.find("input:checkbox[name='margin_less_3000_days_more_enable'][value='1']").attr("checked", "checked");
                    }
                }
                if(record.propertyKey == "MARGIN_3000_10000_DAYS_MORE"){
					$div.find("input[name='margin_3000_10000_days_more']").val(record.propertyValue);
                    if(record.isEnable != '0'){
                        $div.find("input:checkbox[name='margin_3000_10000_days_more_enable'][value='1']").attr("checked", "checked");
                    }
                }
                if(record.propertyKey == "MARGIN_ACTIVE_CARDS"){
                	var value = record.propertyValue;
                	if(value){
                		var values = value.split('-');
                        $div.find("input[name='margin_active_cards_margin']").val(values[0]);
                        $div.find("input[name='margin_active_cards_cards']").val(values[1]);
					}
                    if(record.isEnable != '0'){
                        $div.find("input:checkbox[name='margin_active_cards_enable'][value='1']").attr("checked", "checked");
                    }
                }
                if(record.propertyKey == "ACTIVE_CARDS_JOIN_DAYS_MARGIN"){
                    var value = record.propertyValue;
                    if(value){
                        var values = value.split('-');
                        $div.find("input[name='active_cards_join_days_margin_cards']").val(values[0]);
                        $div.find("input[name='active_cards_join_days_margin_days']").val(values[1]);
                        $div.find("input[name='active_cards_join_days_margin_margin']").val(values[2]);
                    }
                    if(record.isEnable != '0'){
                        $div.find("input:checkbox[name='active_cards_join_days_margin_enable'][value='1']").attr("checked", "checked");
                    }
                }
            });
            $div.modal("toggle");
            $div.on('hidden.bs.modal', function () {
                //关闭窗口清除model
                $div.remove();
            });
        }
    });
}

var do_set_margin_cards=function(){
    var $div;
    var param=new Array();

    if(developType==2){
        // 卡数提升设置项设置值拼接
		var record1={};
        $div = $("#accountAuditCardsSetting");
        record1.isEnable = '0';
        $div.find("#active_cards_join_days_margin_enable:checked").each(function(){
            record1.isEnable = '1';
        });
        var cards = $div.find("[name=active_cards_join_days_margin_cards]").val();
        var days = $div.find("[name=active_cards_join_days_margin_days]").val();
		var margin = $div.find("[name=active_cards_join_days_margin_margin]").val();
        if(!cards||!days||!margin){
            showMessageForFail("值不允许为空");
            return;
		}
		var propertyValue = $div.find("[name=active_cards_join_days_margin_cards]").val() + '-' + $div.find("[name=active_cards_join_days_margin_days]").val() + '-' + $div.find("[name=active_cards_join_days_margin_margin]").val();
        record1.propertyKey = 'ACTIVE_CARDS_JOIN_DAYS_MARGIN';
        record1.propertyValue = propertyValue;
		param.push(record1);
    }else{
        // 额度提升设置项设置值拼接
        $div = $("#accountAuditMarginSetting");
        var zeroDay = $div.find("[name=margin_zero_days_more]").val();
        var less3000 = $div.find("[name=margin_less_3000_days_more]").val();
        var m3000T10000 = $div.find("[name=margin_3000_10000_days_more]").val();
        var activeMargin = $div.find("[name=margin_active_cards_margin]").val();
        var activeCards = $div.find("[name=margin_active_cards_cards]").val();

        if(!zeroDay||!less3000||!m3000T10000||!activeMargin||!activeCards){
            showMessageForFail("值不允许为空");
            return;
        }

        var record1={},record2={},record3={},record4={};
		// 零信用额度参数设置
        record1.isEnable = '0';
        $div.find("#margin_zero_days_more_enable:checked").each(function(){
            record1.isEnable = '1';
        });
        record1.propertyKey = 'MARGIN_ZERO_DAYS_MORE';
        record1.propertyValue = $div.find("[name=margin_zero_days_more]").val();
        param.push(record1);

        // 信用额度小于3000参数设置
        record2.isEnable = '0';
        $div.find("#margin_less_3000_days_more_enable:checked").each(function(){
            record2.isEnable = '1';
        });
        record2.propertyKey = 'MARGIN_LESS_3000_DAYS_MORE';
        record2.propertyValue = $div.find("[name=margin_less_3000_days_more]").val();
        param.push(record2);

        // 信用额度3000到10000参数设置
        record3.isEnable = '0';
        $div.find("#margin_3000_10000_days_more_enable:checked").each(function(){
            record3.isEnable = '1';
        });
        record3.propertyKey = 'MARGIN_3000_10000_DAYS_MORE';
        record3.propertyValue = $div.find("[name=margin_3000_10000_days_more]").val();
        param.push(record3);

        // 信用额度及卡张数参数设置
        record4.isEnable = '0';
        $div.find("#margin_active_cards_enable:checked").each(function(){
            record4.isEnable = '1';
        });
        record4.propertyKey = 'MARGIN_ACTIVE_CARDS';
        record4.propertyValue = $div.find("[name=margin_active_cards_margin]").val() + '-' + $div.find("[name=margin_active_cards_cards]").val();
        param.push(record4);
    }
	$.ajax({
		type:"PUT",
		dataType:'JSON',
		url:'/r/set/updateByParams',
		async:false,
		data:{"param":JSON.stringify(param) },
		success:function(jsonObject){
			if(jsonObject.status == 1){
				//操作成功提示
				showMessageForSuccess("修改成功！");
				$div.modal("toggle");
				//刷新数据列表
				showDevelopList();
			}else{
				showMessageForFail("修改失败："+jsonObject.message);
			}
		}
	});
}

// 显示设置页面
var showModal_dealRebate=function(uid,margin,cardUsedOrStop,userName,id){
    var setting = "accountAuditDealRebate";
    //发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
    $.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
    $.ajax({
        type:"GET",
        async:false,
        dataType:'html',
        url : "/"+sysVersoin+"/html/common/showInfoModal.html",
        success : function(html){
            var $div=$(html).find("#"+setting).clone().appendTo($("body"));
            $div.find("#tableAdd td").css("padding-top","10px");
            $div.find("#tableAdd td.noPaddingTop").css("padding-top","0px");
            //表单填充值
            $div.find("#rebateUserName").text(userName);
            if(developType == 2){
                $div.find("#cardOrMargin").html("卡数");
                $div.find("#showAddJoinActivity").hide();
			}else{
                $div.find("#cardOrMargin").html("信用额度");
                $div.find("#showAddJoinActivity").show();
			}
            $div.find("#rebate_uid").val(uid);
            var canjoin = false;
            if(margin && cardUsedOrStop){
            	var used = cardUsedOrStop.split("/");
            	if(margin>=3000 && isNumber(used[0]) && isNumber(used[1])){
                    if(parseInt(used[0]) + parseInt(used[1]) >=2){
                        canjoin = true;
					}
				}
			}
			if(!canjoin){
                $div.find("#joinActivity").prop("disabled","disabled");
			}
            $div.modal("toggle");
            $div.on('hidden.bs.modal', function () {
                //关闭窗口清除model
                $div.remove();
            });
        }
    });
}

var changeDisplay=function () {
    var $div = $("#accountAuditDealRebate");
    var isDisplay = true;
    $div.find("#isDisplay:checked").each(function(){
        isDisplay = false;
    });
    if(!isDisplay){
        $div.find("[name=displayDate]").val('');
        $div.find("[name=displayDate]").prop("disabled","disabled");
    }else{
        $div.find("[name=displayDate]").removeAttr("disabled");
    }
}

//保存处理信息
var dealWithRebate=function () {
    var $div = $("#accountAuditDealRebate");
    var joinActivity = '';
    $div.find("#joinActivity:checked").each(function(){
        joinActivity = '1';
    });
    var isDisplay = '';
    $div.find("#isDisplay:checked").each(function(){
        isDisplay = '0';
    });
    if(!joinActivity && !isDisplay && !$.trim($div.find("#displayDate").val())){
        showMessageForFail("请至少选择一种处理方式！");
        return;
    }
    var data = {
        'uid':$.trim($div.find("#rebate_uid").val()),
        'joinActivity':joinActivity,
        'isDisplay':isDisplay,
        'displayDate':$.trim($div.find("#displayDate").val())
    };
    $.ajax({
        type:"PUT",
        dataType:'JSON',
        url:'/api/v3/dealWithMore',
        async:false,
        data:data,
        success:function(jsonObject){
            if(jsonObject.status == 1){
                //操作成功提示
                showMessageForSuccess("处理成功！");
                $div.modal("toggle");
                //刷新数据列表
                showDevelopList();
            }else{
                showMessageForFail("处理失败："+jsonObject.message);
            }
        }
    });


}

var isNumber=function (data) {
    var str = data.replace(/^\s+|\s+$/g,"");
    var n = Number(str);
    if (!isNaN(n))
    {
        return true;
    }
    return false;
}

var showModalInfo_Contact=function (uid,username) {
    var param={
        uid:uid,
        phoneTitle:"返利网用户："+_checkObj(username),
        fnName:showDevelopList
    };
    showModalInfo_phone(param);
}

/**
 * 手机，例：param={businessId:151,type:'phone',remarkTitle:'返利网账号：ylw',search_contentRight:'IncomeAccountComp:search:*'}，param里面的参数：
 * uid：必填
 * phoneTitle：标题，选填
 * search_contentRight：查询权限，选填
 * add_contentRight：新增权限，选填
 * delete_contentRight：删除权限，选填
 * fnName：完成操作后是否需要执行刷新页面事件 选填（如  fnName:showAccountList   值不可以加引号）
 */
var showModalInfo_phone=function(param){
    if(!param||!param.uid){
        return;
    }
    var html='<div class="modal fade" aria-hidden="false"  id="div_ModalInfo_phone">\
		<div class="modal-dialog modal-lg"  style="width:780px;">\
			<div class="modal-content">\
				<div class="modal-header no-padding text-center">\
					<div class="table-header">\
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button>联系方式\
					</div>\
				</div>\
				<div class="modal-body no-padding-bottom">\
					<input id="param_uid" type="hidden"/>\
					<input id="param_delete_contentRight" type="hidden"/>\
					<h5 class="header smaller center lighter blue no-padding no-margin-top" id="phoneTitle" style="display:none;"></h5>\
					<div id="div_add_phoneList">\
						<span class="control-label blue ">添加新联系方式：<i class="ace-icon fa fa-asterisk red"></i></span>\
						<select class="chosen-select" name="contactType" style="width:100px;" >\
							<option value="" selected >请选择</option>\
							<option value="1" >手机</option>\
							<option value="2" >微信</option>\
							<option value="3" >QQ</option>\
							<option value="4" >Email</option>\
							<option value="5" >其他</option>\
						</select>&nbsp;&nbsp;\
						<input class="input-sm" name="contactNo" style="width:400px;" type="text" placeholder="请输入对应联系方式" >&nbsp;&nbsp;\
						<label class="inline">\
							<input type="checkbox" name="isCommon" class="ace defaultNoCheck" value="0"/><span class="lbl">常用</span>\
						</label>\
						<label class="pull-right inline">\
							<button class="btn btn-xs btn-white btn-info btn-bold" onclick="doAddPhoneHistory();">\
								<i class="ace-icon fa fa-plus bigger-100 green"></i>新增\
							</button>\
						</label>\
					</div>\
					<div style="height:10px;">&nbsp;</div>\
					<div id="div_search_phoneList">\
						<h6 class="header smaller center lighter blue no-padding no-margin-top">已有联系方式：</span></h6>\
						<table class="table table-bordered" style="width:750px;">\
							<thead>\
								<tr>\
									<th style="width:160px;">类型</th>\
									<th style="width:400px;">详细</th>\
									<th style="width:110px;">常用</th>\
									<th style="width:80px;">操作</th>\
								</tr>\
							</thead>\
							<tbody></tbody>\
						</table>\
						<div id="showModalInfo_phone_page"></div>\
					</div>\
					<br/>\
				</div>\
			</div>\
		</div>\
	</div>';
    var $div=$(html).appendTo($("body"));
    $div.find("#param_uid").val(param.uid);
    if(param.phoneTitle){//标题
        $div.find("#phoneTitle").show().html(param.phoneTitle);
    }
    if(param.add_contentRight){//新增权限
        $div.find("#div_add_phoneList").addClass("contentRight").attr("contentRight",param.add_contentRight);
    }
    if(param.delete_contentRight){//删除权限
        $div.find("#param_delete_contentRight").val(param.delete_contentRight);
    }
    if(param.search_contentRight){//查询权限
        $div.find("#div_search_phoneList").addClass("contentRight").attr("contentRight",param.search_contentRight);
        //加载数据列表
    }
    searchModalInfo_phone(0);

    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () {
        // 关闭窗口清除model
        if(param.fnName){
            param.fnName();
        }
        $div.remove();
    });
    contentRight();
}
/**
 * 新增手机信息
 */
var doAddPhoneHistory=function(){
    var $div=$("#div_ModalInfo_phone");
    var validatePrint=[
        {ele:$div.find("[name=contactType]"),name:'联系类型'},
        {ele:$div.find("[name=contactNo]"),name:'联系方式',minLength:5,maxLength:120}
    ];
    if(!validateEmptyBatch(validatePrint)){
        setTimeout(function(){
            $('body').addClass('modal-open');
        },500);
        return;
    }
	$.ajax({
		type:"POST",
		dataType:'JSON',
		url:'/r/rebateContact/add',
		async:false,
		data:{
			contactType:$div.find("[name=contactType]").val(),//Mobile(1,"手机"),WeChat(2,"微信"),QQ(3,"QQ"),Email(4,"Email"),Others(5,"其他");
			isCommon:($div.find("[name=isCommon]:checked").length==1?"1":"0"),//常用 true false
			contactNo:$div.find("[name=contactNo]").val(),//联系方式
			uid:$div.find("#param_uid").val() //uid
		},
		success:function(jsonObject){
			if(jsonObject.status == 1){
				//操作成功提示
				showMessageForSuccess("新增成功！");
				reset('div_add_phoneList');//清空备注
				searchModalInfo_phone();//刷新数据列表
			}else{
				showMessageForFail("新增失败："+jsonObject.message);
			}
		}
	});
}
/**
 * 删除手机信息
 */
var doDeletePhoneHistory=function(id){
    if(!id){
        return;
    }
	$.ajax({
		type:"POST",
		dataType:'JSON',
		url:'/r/rebateContact/delete',
		data:{id:id},
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				showMessageForSuccess("删除成功！");
				searchModalInfo_phone();//刷新数据列表
			}else{
				showMessageForFail("删除失败："+jsonObject.message);
			}
		}
	});
}
/**
 * 分页查询手机信息
 */
var searchModalInfo_phone=function(CurPage){
    var $div=$("#div_ModalInfo_phone");
    if(!!!CurPage&&CurPage!=0) CurPage=$("#showModalInfo_phone_page .Current_Page").text();
    $.ajax({
        type:"GET",
        dataType:'JSON',
        url:'/r/rebateContact/list?uid='+$div.find("#param_uid").val()+'&pageNo='+(CurPage<=0?0:CurPage-1)+'&pageSize='+($.session.get('initPageSize')?$.session.get('initPageSize'):10),
        async:false,
        success:function(jsonObject){
            if(jsonObject.status == 1){
                var tbody="";
                $.each(jsonObject.data,function(index,record){
                    var tr="";
                    var contactTypeStr="其他";
                    switch(record.contactType) {
                        case "1":
                            contactTypeStr= "手机";
                            break;
                        case "2":
                            contactTypeStr= "微信";
                            break;
                        case "3":
                            contactTypeStr= "QQ";
                            break;
                        case "4":
                            contactTypeStr= "Email";
                            break;
                    }
                    tr+="<td><span>"+(contactTypeStr)+"</span></td>";
                    tr+="<td><span>"+_checkObj(record.contactNo)+"</span></td>";
                    tr+="<td>"+(record.isCommon=='1'?"是":"否")+"</td>";
                    tr+="<td><button class='btn btn-xs btn-danger btn-info  contentRight' " +
                        "onclick='doDeletePhoneHistory("+record.id+")' contentRight='"+($div.find("#param_delete_contentRight").val()||"")+"'>" +
                        "<i class='ace-icon glyphicon glyphicon-trash bigger-100'></i>" +
                        "<span>删除</span>" +
                        "</button></td>";
                    tbody+="<tr id='tr"+record.id+"'>"+tr+"</tr>";
                });
                $div.find("tbody").html(tbody);
                //分页初始化
                showPading(jsonObject.page,"showModalInfo_phone_page",searchModalInfo_phone,null,true);
                contentRight();
            }else{
                showMessageForFail("读取失败："+jsonObject.message);
            }
        }
    });
}



var hideDetailDiv=function(){
	$("#tab3_business_main").show();
	$("#tab3_bussiness_detail").hide();
}
var showDetailDiv=function(rowId,dateStr){
	var $div=$("#tab3_bussiness_detail").show();
	$("#tab3_business_main").hide();
	//加载标题
	$div.find("[id=main_date]").html(timeStamp2yyyyMMddHHmmss(dateStr).substr(0,10));
	$("#main_date_value").val(timeStamp2yyyyMMddHHmmss(dateStr).substr(0,10));
	$("#main_title_value").val(rowId);
	if(rowId==1){
		$div.find("[id=main_title]").html("新增账号");
	}else if(rowId==3){
		$div.find("[id=main_title]").html("总账号数");
	}else if(rowId==4){
		$div.find("[id=main_title]").html("新号增卡");
	}else if(rowId==5){
		$div.find("[id=main_title]").html("现号增卡");
	}else if(rowId==7){
		$div.find("[id=main_title]").html("总卡数");
	}else if(rowId==8){
		$div.find("[id=main_title]").html("新号增信额");
	}else if(rowId==9){
		$div.find("[id=main_title]").html("现号增信额");
	}
	if (rowId == 1 || rowId == 3) {
		$("[name='table3_search_IN_status']").prop("checked", false);
	} else {
		$("[name='table3_search_IN_status']").prop("checked", true);
	}
	showRebateUserList();
//	//读取数据列表
//	$div.find("td").html("现在读取行ID为"+rowId+"的数据table");
}
var changeBussinessType=function(type){
    developType = type;
    if(type == 2){
    	loadTimeLimit30Default5($("[name='startDate']"));
    	$("#bussinessRemark").show();
	}else{
		$("#bussinessRemark").hide();
	}
    showBussinessList();
}

/**
 * 显示数据分析管理的数据
 */
var showBussinessList=function(CurPage){
    if(!!!CurPage&&CurPage!=0) CurPage=$("#tab3_table_page .Current_Page").text();
    //封装data
    var $div = $("#table3_bussiness_Filter");
    var startAndEndDate = $div.find("input[name='startDate']").val().split(" - ");
    var data = {
        'startDate':startAndEndDate[0],
        'endDate':startAndEndDate[1],
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize')
    };
    //发送请求
    $.ajax({
        dataType:'JSON',
        type:"POST",
        async:false,
        url:"/r/account/showRebateStatistics",
        data:data,
        success:function(jsonObject){
            if(jsonObject.status !=1){
                if(-1==jsonObject.status){
                    showMessageForFail("查询失败："+jsonObject.message);
                }
                return;
            }
            var $tbody=$("#bussinessTab tbody");
            $tbody.html("");
            var newAccTotal=0,quitAccTotal=0,newAccAddCardTotal=0,oldAccAddCardTotal=0,quitCardTotal=0,newAccAddLimit=0,oldAccAddLimit=0,lowerLimit=0,subCount=0;
            $.each(jsonObject.data.rebateList,function(index,record){
            	newAccTotal = newAccTotal + _checkObj(record.newAccTotal,null,0);
            	quitAccTotal = quitAccTotal + _checkObj(record.quitAccTotal,null,0);
            	newAccAddCardTotal = newAccAddCardTotal + _checkObj(record.newAccNewCard,null,0);
            	oldAccAddCardTotal = oldAccAddCardTotal + _checkObj(record.nowAccNewCard,null,0);
            	quitCardTotal = quitCardTotal + _checkObj(record.quitCardTotal,null,0);
            	newAccAddLimit = newAccAddLimit + _checkObj(record.newAccUpgradeCredits,null,0);
            	oldAccAddLimit = oldAccAddLimit + _checkObj(record.nowAccUpgradeCredits,null,0);
            	lowerLimit = lowerLimit + _checkObj(record.reduceCredits,null,0);
                subCount = subCount + 1;
                var tr="";
                tr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.statisticsDate).substr(0,10)+"</span></td>";
                tr+="<td><span><a onclick='showDetailDiv(1,"+record.statisticsDate+")'>"+_checkObj(record.newAccTotal,null,0)+"</a></span></td>";
                tr+="<td><span>"+_checkObj(record.quitAccTotal,null,0)+"</span></td>";
                tr+="<td><span><a onclick='showDetailDiv(3,"+record.statisticsDate+")'>"+_checkObj(record.accTotal,null,0)+"</a></span></td>";
                tr+="<td><span><a onclick='showDetailDiv(4,"+record.statisticsDate+")'>"+_checkObj(record.newAccNewCard,null,0)+"</a></span></td>";
                tr+="<td><span><a onclick='showDetailDiv(5,"+record.statisticsDate+")'>"+_checkObj(record.nowAccNewCard,null,0)+"</a></span></td>";
                tr+="<td><span>"+_checkObj(record.quitCardTotal,null,0)+"</span></td>";
                tr+="<td style='background-color:#ddd'><a onclick='showDetailDiv(7,"+record.statisticsDate+")'>"+_checkObj(record.cardTotal,null,0)
                	+"</a><br/><span class='green bolder'>启用 "+_checkObj(record.enableCardTotal,null,0)+"</span> / "
                	+"<span class='red bolder'>停用 "+_checkObj(record.disableCardTotal,null,0)+"</span> / "
                	+"<span class='red bolder'>冻结 "+_checkObj(record.freezeCardTotal,null,0)+"</span></td>"
                tr+="<td><span><a onclick='showDetailDiv(8,"+record.statisticsDate+")'>"+_checkObj(record.newAccUpgradeCredits,null,0)+"</a></span></td>";
                tr+="<td><span><a onclick='showDetailDiv(9,"+record.statisticsDate+")'>"+_checkObj(record.nowAccUpgradeCredits,null,0)+"</a></span></td>";
                tr+="<td><span>"+_checkObj(record.reduceCredits,null,0)+"</span></td>";
                tr+="<td><span>"+_checkObj(record.creditsTotal,null,0)+"</span></td>";
                tr+="</td>";
                $tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
            });
            //有数据时，显示总计 小计
            if(jsonObject.data.rebateList.length > 0){
            	var tr1 = "<tr><td bgcolor='#579EC8'>小计:"+subCount+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'>"+newAccTotal+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'>"+quitAccTotal+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'></td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'>"+newAccAddCardTotal+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'>"+oldAccAddCardTotal+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'>"+quitCardTotal+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'></td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'>"+newAccAddLimit+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'>"+oldAccAddLimit+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'>"+lowerLimit+"</td>" +
						"<td bgcolor='#579EC8' style='color:white;font-size:10px;'></td>" +
						"</tr>";
				$tbody.append(tr1);
//                var totalRows={
//                    column:12,
//                    subCount:jsonObject.data.rebateList.length
//                };
//                totalRows[2]={subTotal:newAccTotal};
//                totalRows[3]={subTotal:quitAccTotal};
//                totalRows[5]={subTotal:newAccAddCardTotal};
//                totalRows[6]={subTotal:oldAccAddCardTotal};
//                totalRows[7]={subTotal:quitCardTotal};
//                totalRows[9]={subTotal:newAccAddLimit};
//                totalRows[10]={subTotal:oldAccAddLimit};
//                totalRows[11]={subTotal:lowerLimit};
//                showSubAndTotalStatistics4Table($tbody,totalRows);
            }
            //分页初始化
            showPading(jsonObject.data.page,"tab3_table_page",showBussinessList,null,true);
            contentRight();
        }
    });
}



/**
 * 显示数据分析管理-统计明细的数据
 */
var showRebateUserList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#tab3_table_detail_page .Current_Page").text();
	var $div = $("#tab3_bussiness_detail");
	var statusToArray = new Array();
	var queryType = $div.find("[id='main_title_value']").val();
	$div.find("input[name='table3_search_IN_status']:checked").each(function(){
		statusToArray.push(this.value);
	});
	var data = {
		alias:$div.find("[name='search_EQ_alias']").val()||"",
		bankType:$div.find("[name='table3_search_LIKE_bankType']").val()||"",
		statusToArray:statusToArray.toString(),
		account:$.trim($div.find("[name='search_LIKE_account']").val())||"",
		owner:$.trim($div.find("[name='search_LIKE_owner']").val())||"",
		rebateUser:$.trim($div.find("[name='rebateUserName']").val())||"",
		queryType:queryType,
		startDate:$div.find("[id='main_date_value']").val(),
		pageNo:CurPage<=0?0:CurPage-1,
		pageSize:$.session.get('initPageSize')
	};
	var requesturl = "/r/account/showRebateUserNew";
	//发送请求
	$.ajax({
		dataType:'JSON',
		type:"POST",
		async:false,
		url:requesturl,
		data:data,
		success:function(jsonObject){
			//小计
			var counts=0;
			var tr = '';
			var idList=new Array();
			for(var index in jsonObject.data){
				var val = jsonObject.data[index];
				idList.push({'id':val.id});
				tr += '<tr>'
					+'<td>' + val.userName + '</td>'
					+'<td>' + (null==val.margin?0:val.margin) + '</td>'
					+'<td>' + (null==val.bankBalance?0:val.bankBalance) + '</td>'
					+'<td>' + (null==val.currSysLevelName?"":val.currSysLevelName) + '</td>'
					+'<td>' + (null==val.alias?"":val.alias) + '</td>';

				tr+="<td>" +(null==val.id?"":getAccountInfoHoverHTML(val)) +"</td>";
				if(null != val.status){
					if(val.status==accountStatusFreeze || val.status==accountStatusExcep){
						tr+="<td><span class='label label-sm label-danger'>"+val.createTimeStr+"</span></td>";
					}else if(val.status==accountStatusStopTemp){
						tr+="<td><span class='label label-sm label-primary'>"+val.createTimeStr+"</span></td>";
					}else if(val.status==accountInactivated){
						tr+="<td><span class='label label-sm label-warning'>"+val.createTimeStr+"</span></td>";
					}else if(val.status==accountStatusDelete){
						tr+="<td><span class='label label-sm label-inverse'>"+val.createTimeStr+"</span></td>";
					}else{
						tr+="<td><span class='label label-sm label-success'>"+val.createTimeStr+"</span></td>";
					}
				}else{
					tr+="<td></td>";
				}
				tr+='<td>' + (null==val.bankBalance?0:val.bankBalance) + '</td>';
				if (_checkObj(val.remark)) {
					if (_checkObj(val.remark).length > 23) {
						tr += '<td>'
							+ '<a  class="bind_hover_card breakByWord"  title="备注信息"'
							+ 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
							+ ' data-content="' + val.remark + '">'
							+ val.remark.replace(/<br>/g, "").substring(0, 4)
							+ '</a>'
							+ '</td>';

					} else {
						tr += '<td>'
							+ '<a class="bind_hover_card breakByWord"  title="备注信息"'
							+ 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
							+ ' data-content="' + val.remark + '">'
							+ val.remark
							+ '</a>'
							+ '</td>';
					}
				} else {
					tr += '<td></td>';
				}
//					tr+="<button class='btn btn-xs btn-white btn-primary btn-bold "+OperatorLogBtn+"' " +
//						"onclick='showModal_accountExtra("+val.id+")'>"+
//						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
//					tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
//						"onclick='showInOutListModal("+val.id+")'>"+
//						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
//					tr+='</td>';
				+'</tr>';
				counts +=1;
			};
			var $tbody=$div.find("tbody").html(tr);
			//用户名分组  tab3_table_detail_tbody  tbody的节点ID
			SpanGridNew(tab3_table_detail_tbody,0);
			if(jsonObject.data.length > 0){
				var trs = '<tr>' + '<td colspan="9">小计：' + counts
						+ '</td>' + '</tr>';
				$tbody.append(trs);
				var trn = '<tr>' + '<td colspan="9">总计：'
						+ jsonObject.page.totalElements + '</td>'
						+ '</tr>';
				$tbody.append(trn);
			}
			showPading(jsonObject.page,"tab3_table_detail_page",showRebateUserList);
			$("[data-toggle='popover']").popover();
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
		}
	});
}


function SpanGridNew(tabObj,colIndex){
	if(tabObj != null){
		var i,j;
		var intSpan;
		var strTemp;
		for(i = 0; i < tabObj.rows.length; i++){
			intSpan = 1;
			strTemp = tabObj.rows[i].cells[colIndex].innerText;
			var balanceCount = tabObj.rows[i].cells[2].innerText;
			if(strTemp==""||strTemp==null){
				continue;
			}
			for(j = i + 1; j < tabObj.rows.length; j++){
				if(strTemp == tabObj.rows[j].cells[colIndex].innerText){
					var tmpBalance = tabObj.rows[j].cells[2].innerText;
					balanceCount = (+balanceCount)+(+tmpBalance);
					tabObj.rows[i].cells[2].innerText = balanceCount.toFixed(2);
					intSpan++;
					tabObj.rows[i].cells[0].rowSpan  = intSpan;
					tabObj.rows[i].cells[1].rowSpan  = intSpan;
					tabObj.rows[i].cells[2].rowSpan  = intSpan;
					var height=(intSpan+2);
					var ht=intSpan;
					//设置文本居中
					tabObj.rows[i].cells[0].style="line-height:"+height;
					tabObj.rows[i].cells[1].style="line-height:"+height;
					tabObj.rows[i].cells[2].style="line-height:"+height;
					tabObj.rows[j].cells[0].style.display = "none";
					tabObj.rows[j].cells[1].style.display = "none";
					tabObj.rows[j].cells[2].style.display = "none";
				}
				else
				{
					break;
				}
			}
			i = j - 1;
		}
	}
}
var loadTimeLimit30Default5=function(timeInputObj) {
	var todayStart=todayEnd= moment();
    var yestStart = moment().subtract('days', 1);
    var yestEnd = moment().subtract('days', 1);
    var last5Start = moment().subtract('days', 4);
    var last5End = moment();
    var opensVal = 'right';
    if (timeInputObj.length == 0) {
        return;
    }
    timeInputObj.daterangepicker({
    	timePicker: false,
        timePickerIncrement: 1,
        autoUpdateInput: true,
        timePickerSeconds: false,
        startDate: last5Start, //设置开始日期
        endDate: last5End, //设置开始日期
        dateLimit : {
            days : 30
        },
        ranges: {
            '今日': [todayStart, todayEnd],
            '昨日': [yestStart, yestEnd],
            '最近5日': [last5Start, last5End]
        },
        opens: opensVal, //日期选择框的弹出位置
        locale: {
            "format": "YYYY-MM-DD",
            "separator": " - ",
            "applyLabel": "确定",
            "cancelLabel": "取消",
            "fromLabel": "从",
            "toLabel": "到",
            "customRangeLabel": "自定义(最多查30天)",
            "dayNames": [
                "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
            ],
            "daysOfWeek": [
                "日", "一", "二", "三", "四", "五", "六"
            ],
            "monthNames": [
                "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"
            ],
            "firstDay": 1
        }
    }, _showDefaultForALL(timeInputObj, last5Start, last5End)).on('apply.daterangepicker', function (ev, picker) {
        timeInputObj.val(picker.startDate.format('YYYY-MM-DD') + ' - ' + picker.endDate.format('YYYY-MM-DD'));
    }).on('cancel.daterangepicker', function (ev, picker) {
        timeInputObj.val('');
    });
}