currentPageLocation = window.location.href;

errrBankIdList = {};

var banklog_amountTotal=false;
$.each(ContentRight['OtherPermission:*'], function (name) {
    if (name == 'OtherPermission:ExportAndTotal:*') {
    	banklog_amountTotal = true;
    }
});
var changeLogTab=function(logType){
	var $div=$("#bankLogList_modal");
	$div.find("#logTab").val(logType);
	if(logType=='all'){//银行流水
		$div.find(".statusSearch").show();
		bankLogList_In_OutList(0);
	}else if(logType=='sysLog'){//系统流水
		sysLogList(0);
	}else if(logType=='problem'){//排查记录
		problemList(0);
	}
};

var changeOrderBy=function(orderBy){
	var $div=$("#bankLogList_modal");
	$div.find("table#list tbody").html("");
	$div.find("#orderByTh").val(orderBy);
	bankLogList_In_OutList(0);
};

var clickOnErrorCheckbox = function(accountId,obj){
	var val = obj.value;
	var tmp = [];
	var list =  errrBankIdList[''+accountId+''];
	list = !list||list.length==0?[]:list;
	$.each(list,function(index,record){
		if(record != val){
			tmp.push(record);
		}
	});
	if(obj.checked){
		tmp.push(val);
	}
	errrBankIdList[''+accountId+''] = tmp;
};

//明细弹出框
var showInOutListModal=function(errorId,accountId,noDefaultBankLogTime){
	errrBankIdList = {};
	errorId4Invst = errorId;
	targetErrorId4Invst = accountId;
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/bankLogInvstList.html",
		success : function(html){
			var $div=$(html).find("#bankLogList_modal").clone().appendTo($("body"));
				$div.find("[name=search_IN_status]").html(getBanklogStatusOptionList());
				$div.find("input#accountInfo_id").val(accountId);
				$div.modal("toggle");
				//加载时间控件
				if(noDefaultBankLogTime){
					initTimePicker(false,$("[name=startAndEndTime]"));
				}else{
					initTimePicker(true,$("[name=startAndEndTime]"),7);
				}
				initTimePicker(true,$("[name=startAndEndTime_sys]"),7);
				initTimePicker(true,$("[name=startAndEndTime_problem]"),7);
				//数据列表
				sysLogList(0);
				//查询账号信息
				var result=getAccountInfoById(accountId);
				if(result){
					$div.find("input#accountInfo_alias").val(result.alias);
					var accountInfo=result.account;
					if(accountInfo){
						var accountTitle=(result.alias?result.alias:'无')+'|'+(result.bankType?result.bankType:'无')+'|'+(result.owner?hideName(result.owner):'无')+'|'+hideAccountAll(result.account)+" 明细 - "+result.id;
						$div.find("#accountInfo_account").text(accountTitle);
					}
				}else{
					showMessageForFail("账号信息查询异常，请刷新页面");
				}
				$div.find("[name=search_IN_status]").change(function(){
					bankLogList_In_OutList();
				});
				$div.find("[name=searchTypeIn0Out1]").click(function(){
					bankLogList_In_OutList();
				});
				$(document).keypress(function(){
					if(event.keyCode == 13) {
						$div.find("#searchBtn button").click();
					}
				});
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
};
var bankLogList_In_OutListById=function(){
	$("#bankLogList_modal #orderByTh").val("id");
	bankLogList_In_OutList();
};

/**
 * 银行流水明细列表
 */
var bankLogList_In_OutList=function(CurPage){
	var $div=$("#bankLogList_modal");
	//封装查询条件
	if(!!!CurPage&&CurPage!=0) CurPage=$("#bankLogList_In_OutPage .Current_Page").text();
	var startAndEndTimeToArray=getTimeArray($div.find("#filter [name=startAndEndTime]").val());
	var formData={
			"fromAccount":$div.find("#accountInfo_id").val(),
			"toAccount":$.trim($div.find("#filter [name=toAccount]").val()),
			"toAccountOwner":$.trim($div.find("#filter [name=toAccountOwner]").val()),
			"startAndEndTimeToArray":startAndEndTimeToArray.toString(),
			"minAmount":$.trim($div.find("#filter #qstartAmount").val()),
			"maxAmount":$.trim($div.find("#filter #qendAmount").val()),
			"pageNo" : CurPage<=0?0:CurPage-1,
			"pageSize":$.session.get('initPageSize'),
			"orderBy":$div.find("#orderByTh").val()
		};
	if($div.find("#logTab").val()=='type0'){
		formData.status=bankLogStatusMatching;
	}else{
		formData.status=$div.find("#filter [name='search_IN_status']").val();
	}
	var bankIdList = errrBankIdList[''+targetErrorId4Invst+''];
	//转入转出
	if($div.find("[name=searchTypeIn0Out1]:checked").length==1){
		if($div.find("[name=searchTypeIn0Out1]:checked").val()==1){
			//转出时  金额是负数
			formData.maxAmount=formData.maxAmount<0?formData.maxAmount:0;
		}else{
			//转入时  金额是正数
			formData.minAmount=formData.minAmount>0?formData.minAmount:0;
		}
	}
	$.ajax({
		dataType : 'JSON',
		type : "POST",
		async:false,
		url : "/r/banklog/bankLogList",
		data : formData,
		success : function(jsonObject) {
			if(jsonObject.status !=1){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var sysDate=new Date();
			var tbody="",totalBankLogAmount_Plus=0,totalBankLogAmount_Nagetive=0,idList=[];
			var bankIdList = errrBankIdList[''+targetErrorId4Invst+''];
			$.each(jsonObject.data,function(index,record){
				var tr="";
				if(bankIdList&&bankIdList.length>0&&bankIdList.indexOf(''+record.id+'')>=0){
					tr+="<td><input type='checkbox' value='"+record.id+"' onchange='clickOnErrorCheckbox("+record.fromAccount+",this);' checked/>异常</td>"
				}else{
					tr+="<td><input type='checkbox' value='"+record.id+"' onchange='clickOnErrorCheckbox("+record.fromAccount+",this);'/>异常</td>"
				}
				tr+="<td style='display:none;'><span>"+record.id+"</span></td>";
				tr+="<td><span>"+record.tradingTimeStr+"</span></td>";
				tr+="<td><span>"+record.createTimeStr+"</span></td>";
				if(record.amount>0){
					tr+="<td><span>"+record.amount+"</span></td>";
					tr+="<td><span>--</span></td>";
					totalBankLogAmount_Plus+=record.amount*1;
				}else{
					tr+="<td><span>--</span></td>";
					tr+="<td><span>"+record.amount+"</span></td>";
					totalBankLogAmount_Nagetive+=record.amount*1;
				}
				tr+="<td><span>"+(record.balance?record.balance:0)+"</span></td>";
				tr+="<td><span>"+(record.toAccount?hideAccountAll(record.toAccount):'')+"</span></td>";
				tr+="<td><span>"+(record.toAccountOwner?record.toAccountOwner:'')+"</span></td>";
				tr+="<td>"+getHTMLremark(record.summary,90)+"</td>";
				tr+="<td>"+getHTMLremark(record.remark,90)+"</td>";
				if(record.status==bankLogStatusMatched){
					idList.push(record.id);
				}
				tr+="<td>"+getHTMLBankLogStatus(record,sysDate)+"</td>";
				tbody+="<tr id='tr"+record.id+"'>"+tr+"</tr>";
			});
			$div.find("table#list tbody").html(tbody);
			loadHover_InOutInfoHover(idList);
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var amountPlus='0',amountNegative='0',amount='0';
				if(banklog_amountTotal&&jsonObject.page.header&&jsonObject.page.header.totalAmount&&jsonObject.page.header.totalAmount[0]){
					var total=jsonObject.page.header.totalAmount[0];
					amountPlus=total[0];//大于0的金额
					amountNegative=total[1];//小于0的金额
					amount=total[2];//总计
				}
				var totalRows={
						column:12, 
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements,
						4:{subTotal:banklog_amountTotal?totalBankLogAmount_Plus:'0',total:amountPlus},
						5:{subTotal:banklog_amountTotal?totalBankLogAmount_Nagetive:'0',total:amountNegative}
					};
				showSubAndTotalStatistics4Table($div.find("table#list tbody"),totalRows);
			}
			if(!banklog_amountTotal){
				$div.find("[name=subtotal]").hide();
				$div.find("[name=total]").hide();
			}
			showPading(jsonObject.page,"bankLogList_In_OutPage",bankLogList_In_OutList);
		}
	});
};
var sysLogList=function(CurPage){
	var $div=$("#bankLogList_modal");
	//封装查询条件
	if(!!!CurPage&&CurPage!=0) CurPage=$("#sysLogPage .Current_Page").text();
	var startAndEndTimeToArray=getTimeArray($div.find("[name=startAndEndTime_sys]").val());
	//查询条件顺序勿更换，提升SQL效率
	var formData={
			"search_GT_type":0,
			"search_GT_status":0,
			"search_GTE_amount":$.trim($div.find("[name=qstartAmount_sys]").val()),
			"search_LTE_amount":$.trim($div.find("[name=qendAmount_sys]").val()),
			"search_GTE_createTime":$.trim(startAndEndTimeToArray[0]),
			"search_LTE_createTime":$.trim(startAndEndTimeToArray[1]),
			"search_LIKE_oppAccount":$.trim($div.find("[name=toAccount_sys]").val()),
			"search_LIKE_orderNo":$.trim($div.find("[name=orderNo_sys]").val()),
			"search_EQ_accountId":$div.find("#accountInfo_id").val(),
			"pageNo" : CurPage<=0?0:CurPage-1,
			"pageSize":$.session.get('initPageSize')
		};
	$.ajax({
		dataType : 'JSON',
		type : "POST",
		async:false,
		url : "/r/syslog/list",
		data : formData,
		success : function(jsonObject) {
			if(jsonObject.status !=1){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var bankIdList = errrBankIdList[''+targetErrorId4Invst+''];
			var tbody="",totalBankLogAmount_Plus=0,totalBankLogAmount_Nagetive=0,totalFee=0,idList=[],subCount=0;
			$.each(jsonObject.data,function(index,record){
				if(record.id){
					//java层插入的未匹配的不计入小计
					subCount++;
				}
				totalFee+=(record.fee?record.fee:0)*1;
				var oppHandicap=getHandicapInfoById(record.oppHandicap);
				var type = getSysLogTypeById(record.type,record.amount);
				type  = record.type ==999?{id:999,msg:''}:type;
				var status=getSysLogStatusById(record.status);
				status = record.status==999?{id:999,msg:''}:status;
				status = record.status==888?{id:888,msg:'确认中'}:status;
				status = record.status==777?{id:777,msg:'处理中'}:status;
				status = record.status==666?{id:666,msg:'处理中'}:status;
				var tr="";
				if(record.type==1&&(!record.orderId)||record.type==7){
					if(bankIdList&&bankIdList.length>0&&bankIdList.indexOf(''+record.bankLogId+'')>=0){
						tr+="<td><input title='标记异常' type='checkbox' value='"+record.bankLogId+"' onchange='clickOnErrorCheckbox("+record.accountId+",this);' checked/></td>"
					}else{
						tr+="<td><input title='标记异常' type='checkbox' value='"+record.bankLogId+"' onchange='clickOnErrorCheckbox("+record.accountId+",this);'/></td>"
					}
				}else{
					tr+="<td></td>"
				}
				tr+="<td><span>"+(oppHandicap?_checkObj(oppHandicap.name):'')+"</span></td>";
				tr+="<td><span>"+_checkObj(type.msg)+"</span></td>";
				tr+="<td><span>"+_checkObj(record.orderNo)+"</span></td>";
				var oppStr=record.oppAccount?(hideAccountAll(record.oppAccount)+' | '+hideName(record.oppOwner)):'无';
				var amountTr='<td><a  class="bind_hover_card breakByWord"  '+
	                   ' data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'+
	                       ' data-content="'+oppStr+'" >'+record.amount+'</a>'+
	               ' </td>';
				if(record.amount==0){
					tr+="<td><span>--</span></td><td><span>--</span></td>";
				}else if(record.amount>0){
					tr+=amountTr;
					tr+="<td><span>--</span></td>";
					if(record.id){
						//java层插入的未匹配的不计入小计
						totalBankLogAmount_Plus+=record.amount*1;
					}
				}else{
					tr+="<td><span>--</span></td>";
					tr+=amountTr;
					if(record.id){
						//java层插入的未匹配的不计入小计
						totalBankLogAmount_Nagetive+=record.amount*1;
					}
				}
				tr+="<td><span>"+_checkObj(record.fee)+"</span></td>";
				tr+="<td><span>"+_checkObj(record.balance)+"</span></td>";
				tr+="<td><span>"+_checkObj(record.bankBalance)+"</span></td>";
				tr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
				if(record.type ==7||record.type == 999||record.status == 999||record.status == 888||record.status == 777||record.status==666){
					if(record.type == 999||record.status == 999){
						tr+="<td><span>&nbsp;&nbsp;&nbsp;</span></td>";
					}else if(record.status == 888){
						tr+="<td><span  class='badge badge-warning'>待确认</span></td>";
					}else if(record.status == 666){
						tr+="<td><span  class='badge badge-warning'>处理中</span></td>";
					}else if(record.type == 7){
						tr+="<td><span  class='badge badge-warning'>确认中</span></td>";
					}else{
						tr+="<td><span class='badge badge-warning'>处理中</span></td>";
					}
				}else if(record.type==1){//入款：
					if(!record.orderNo){
						tr+="<td><span class='badge badge-warning'>未认领</span></td>";
					}else{
						var statusClass=record.status==-1?'badge-inverse':(record.status==1?'badge-success':'badge-danger');
						if(record.bankLogId && (record.balance == record.bankBalance)){
							tr+="<td><span class='badge badge-success'>已对账</span></td>";
						}else{
							tr+="<td><span class='badge "+((record.balance == record.bankBalance)?statusClass:'badge-danger')+"'>"+((record.balance == record.bankBalance)?_checkObj(status.msg):'异常')+"</span></td>";
						}
					}
				}else{//其他卡状态
					var statusClass=record.status==-1?'badge-inverse':(record.status==1?'badge-success':'badge-danger');
					if(record.bankLogId && (record.balance == record.bankBalance)){
						tr+="<td><span class='badge badge-success'>已对账</span></td>";
					}else{
						tr+="<td><span class='badge "+((record.balance == record.bankBalance)?statusClass:'badge-danger')+"'>"+((record.balance == record.bankBalance)?_checkObj(status.msg):'异常')+"</span></td>";
					}
				}
				tr+="<td>"+getHTMLremark(record.summary,110)+"</td>";
				var red =  (record.bankBalance - record.balance)<0;
				tr+="<td>"+getHTMLremark(record.remark,100,red)+"</td>";
				tbody+="<tr id='tr"+record.id+"'>"+tr+"</tr>";
			});
			$div.find("table#sysLog_inOut tbody").html(tbody);
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var totalRows={
						column:13,
						subCount:subCount,
						count:jsonObject.page.totalElements,
						5:{subTotal:totalBankLogAmount_Plus,total:jsonObject.page.header.amountPlus},
						6:{subTotal:totalBankLogAmount_Nagetive,total:jsonObject.page.header.amountNagetive},
						7:{subTotal:totalFee,total:jsonObject.page.header.feeTotal},
						8:{subTotal:(totalBankLogAmount_Plus*1+totalBankLogAmount_Nagetive*1),total:(jsonObject.page.header.amountPlus*1+jsonObject.page.header.amountNagetive*1)}
					};
				showSubAndTotalStatistics4Table($div.find("table#sysLog_inOut tbody"),totalRows);
			}
			if(!banklog_amountTotal){
				$div.find("[name=subtotal]").hide();
				$div.find("[name=total]").hide();
			}
			if(jsonObject.page&&!jsonObject.page.totalElements&&jsonObject.data.length>0){
				jsonObject.page.totalElements = jsonObject.data.length;
			}
			showPading(jsonObject.page,"sysLogPage",sysLogList);
			loadHover_InOutInfoHover(idList);
		    $("[data-toggle='popover']").popover();
		}
	});
};
var problemList=function(CurPage){
	var $div=$("#bankLogList_modal");
	//封装查询条件
	if(!!!CurPage&&CurPage!=0) CurPage=$("#problemPage .Current_Page").text();
	var startAndEndTimeToArray=getTimeArray($div.find("[name=startAndEndTime_problem]").val());
	//查询条件顺序勿更换，提升SQL效率
	var formData={
			"search_GTE_amount":$.trim($div.find("[name=qstartAmount_problem]").val()),
			"search_LTE_amount":$.trim($div.find("[name=qendAmount_problem]").val()),
			"search_GTE_createTime":$.trim(startAndEndTimeToArray[0]),
			"search_LTE_createTime":$.trim(startAndEndTimeToArray[1]),
			"search_LIKE_oppAccount":$.trim($div.find("[name=toAccount_problem]").val()),
			"search_LIKE_orderNo":$.trim($div.find("[name=orderNo_problem]").val()),
			"search_EQ_accountId":$div.find("#accountInfo_id").val(),
			"pageNo" : CurPage<=0?0:CurPage-1,
			"pageSize":$.session.get('initPageSize')
		};
	$.ajax({
		dataType : 'JSON',
		type : "POST",
		async:false,
		url : "/r/syslog/list4Invst",
		data : formData,
		success : function(jsonObject) {
			if(jsonObject.status !=1){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var tbody="",totalBankLogAmount_Plus=0,totalBankLogAmount_Nagetive=0,totalFee=0,totalBalance=0,idList=new Array();
			$.each(jsonObject.data,function(index,record){
				totalFee+=(record.fee?record.fee:0)*1;
				totalBalance+=(record.balance?record.balance:0)*1;
				var oppHandicap=getHandicapInfoById(record.oppHandicap);
				var type=getObjectById(record.type,SYS_INVST_TYPE_ARRAY_ALL);
				var tr="";
				tr+="<td><span>"+(oppHandicap?_checkObj(oppHandicap.code):'---')+"</span></td>";
				tr+="<td><span>"+_checkObj(record.orderNo)+"</span></td>";
				var oppStr=record.oppAccount?(hideAccountAll(record.oppAccount)+' | '+hideName(record.oppOwner)):'无';
				var amountTr='<td><a  class="bind_hover_card breakByWord"  '+
	                   ' data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'+
	                       ' data-content="'+oppStr+'" >'+record.amount+'</a>'+
	               ' </td>';
				if(record.amount>0){
					tr+=amountTr;
					tr+="<td><span>--</span></td>";
					totalBankLogAmount_Plus+=record.amount*1;
				}else{
					tr+="<td><span>--</span></td>";
					tr+=amountTr;
					totalBankLogAmount_Nagetive+=record.amount*1;
				}
				tr+="<td><span>"+_checkObj(record.balance)+"</span></td>";
				tr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.occurTime)+"</span></td>";
				tr+="<td><span>"+(_checkObj(type?type.msg:''))+"</span></td>";
				tr+="<td><span>"+_checkObj(record.confirmerName)+"</span></td>";
				tr+="<td><span>"+record.consumeStr+"</span></td>";
				tr+="<td>"+getHTMLremark(record.summary,110)+"</td>";
				tr+="<td>"+getHTMLremark(record.remark,110)+"</td>";
				tbody+="<tr id='tr"+record.id+"'>"+tr+"</tr>";
			});
			$div.find("table#problem_inOut tbody").html(tbody);
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var totalRows={
						column:12, 
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements,
						3:{subTotal:totalBankLogAmount_Plus,total:jsonObject.page.header.amountPlus},
						4:{subTotal:totalBankLogAmount_Nagetive,total:jsonObject.page.header.amountNagetive}
					};
				showSubAndTotalStatistics4Table($div.find("table#problem_inOut tbody"),totalRows);
			}
			if(!banklog_amountTotal){
				$div.find("[name=subtotal]").hide();
				$div.find("[name=total]").hide();
			}
			showPading(jsonObject.page,"problemPage",problemList);
			loadHover_InOutInfoHover(idList);
		    $("[data-toggle='popover']").popover();
		}
	});
};
var errorId4Invst = null;
var targetErrorId4Invst = null;
var targetSpareTime = 0;
var targetPeakBalance = 0 ;
var $div$ = null;
var postFun = null;

var accInvstTypeHtml = function(accId,logId,amt){
	var ret = '';
	ret = ret + '<select errorTargetId4select="'+accId+'" errorLogId4select="'+logId+'" errorLogAmt="'+amt+'" onchange="onChangeHandleType(this);">';
	$.each(SYS_INVST_TYPE_ARRAY, function(i,val){
		ret = ret+'<option value="'+val.type+'">'+val.msg+'</option>';
	});
	ret = ret + '</select>';
	return ret;
};

var accInvstTypeName = function(type){
	var ret = '---';
	$.each(SYS_INVST_TYPE_ARRAY, function(i,val){
		if(val.type == type)
			ret = val.msg;
	});
	return ret;
};

var showAccInvstModal = function(errId,accId,LogIdList,fun){
	errorId4Invst = errId;
	targetErrorId4Invst = accId;
	$div$ = null;
	postFun = fun;
	$.ajax({type:"GET",async:false,dataType:'html',url : "/"+sysVersoin+"/html/common/bankLogInvstList.html",
		success : function(html){
			var $div=$(html).find("#accInvst_modal").clone().appendTo($("body"));
			$div$ = $div;
			$div.modal("toggle");
			var result=getAccountInfoById(accId);
			if(result){
				targetSpareTime = result.flag ? result.flag : 0;
				$div.find("#targetBankType").text(result.bankType);
				$div.find("#targetAccount").text(hideAccountAll(result.account));
				$div.find("#targetOwner").text((result.owner?hideName(result.owner):'无'));
				$div.find("#targetBalance").text(result.balance+'元');
				$div.find("#targetBankBalance").text(result.bankBalance+'元');
				$div.find("#targetMargin").text((setAmountAccuracy(result.bankBalance-result.balance))+'元');
				if(!targetSpareTime){
					$div.find("#targetSpareTime").remove();
				}else{
					targetPeakBalance = result.peakBalance;
					$div.find("#targetPeakBalance").text(result.peakBalance);
				}
			}else{
				showMessageForFail("账号信息查询异常，请刷新页面");
			}
			var logs =  findlist4BankLogs(accId,LogIdList);
			var logsHtml = ''
			if(logs&&logs.length >0 ){
				$.each(logs,function(index,record){
					logsHtml = logsHtml+ '<tr logId="'+record.id+'">';
					logsHtml = logsHtml+ '<td>'+moment(new Date(record.tradingTime)).format('YYYY-MM-DD HH:mm:ss')+'</td>';
					logsHtml = logsHtml+ '<td>'+(record.toAccount?hideAccountAll(record.toAccount):'无')+'</td>';
					logsHtml = logsHtml+ '<td>'+(record.toAccountOwner?hideName(record.toAccountOwner):'无')+'</td>';
					logsHtml = logsHtml+ '<td>'+accInvstTypeHtml(record.fromAccount,record.id,record.amount)+'</td>';
					logsHtml = logsHtml+ '<td>'+record.amount+'</td>';
					logsHtml = logsHtml+ '<td><input class="input-small" errorLogId4input'+record.id+'="'+record.id+'" placeholder="不需填写" disabled/></td>';
					logsHtml = logsHtml+ '</tr>';
				});
			}
			$div.find("#errorBankLogList").html(logsHtml);
			$div.on('hidden.bs.modal', function () {
				$div.remove();
			});
		}
	});
};

var onChangeHandleType = function(obj){
	var t1 = computerDescMargin();
	$div$.find("#targetPeakBalanceRemain").text((parseInt(targetPeakBalance)+parseInt(t1))+"（立即生效）");
	var t = $(obj);
	var type = t.val();
	var logid = t.attr('errorlogid4select');
	var $input = $("input[errorLogId4input"+logid+"="+logid+"]");
	if(SYS_INVST_TYPE_Refund == type){
		$input.attr("disabled",false).attr("placeholder","出款单号|返利单号");
	}else if(SYS_INVST_TYPE_DuplicateOutward == type){
		$input.attr("disabled",false).attr("placeholder","出款单号|返利单号");
	}else if(SYS_INVST_TYPE_ManualTransIn == type){
		$input.attr("disabled",false).attr("placeholder","汇出账号编码");
	}else if(SYS_INVST_TYPE_ManualTransOut == type ){
		$input.attr("disabled",false).attr("placeholder","汇入账号编码");
	}else{
		$input.attr("disabled",true).attr("placeholder","不需填写");
	}
};

var computerDescMargin = function(){
	var t1 = 0;
	$.each($("select[errorTargetId4select="+targetErrorId4Invst+"]"),function(i,val){
		var t = $(this);
		if(t.val()==4){
			t1 = t1 + parseFloat(t.attr("errorLogAmt"));
		}
	});
	return t1;
};

var saveHandleResult = function(){
	var htm = "确认该处理操作";
	var transferToOther = $("#transferToOther").val();
	transferToOther = $.trim(transferToOther);
	if(!transferToOther){
		if(targetSpareTime){
			htm = htm +"</br>&nbsp;&nbsp;&nbsp;&nbsp;";
			htm = htm + "该兼职人员信用额度："+targetPeakBalance;
			htm = htm +"</br>&nbsp;&nbsp;&nbsp;&nbsp;";
			htm = htm + "排查后信用额度："+(parseInt(targetPeakBalance)+parseInt(computerDescMargin()));
		}
	}else{
		htm = htm +"</br>&nbsp;&nbsp;&nbsp;&nbsp;";
		htm = htm + "该任务转交给："+transferToOther+"处理";
	}
	var remark = $("#errorInvstRemark").val();
	if(!remark){
		showMessageForFail('排查情况说明不能为空.')
		return;
	}
	var errorLogs = [];
	$.each($("select[errorTargetId4select="+targetErrorId4Invst+"]"),function(i,val){
		var t = $(this);
		var logid = t.attr("errorLogId4select");
		var $input = $("input[errorLogId4input"+logid+"="+logid+"]");
		var orderNo = $input.val();
		orderNo = orderNo?orderNo:''
		errorLogs.push(logid+'#'+t.val()+'#'+orderNo);
	});
	var invstResult = $("input[name=accInvstResult]:checked").val();
	bootbox.confirm(htm, function (result) {
		if(result){
			$.ajax({type:"POST",url:"/r/problem/accInvDoing",data:{"errorId":errorId4Invst,remark:remark,errorLogs:errorLogs.toString(),invstResult:invstResult,"transferToOther":transferToOther},dataType:'json',async:false,success:function(jsonObject){
				if(jsonObject.status == 1){
					result = jsonObject.data;
					showMessageForFail("操作成功.");
					$div$.modal("toggle");
					if(postFun){
						postFun();
					}
				}else{
					showMessageForFail(jsonObject.message);
				}
			}});
		}
	});
};

var findlist4BankLogs = function (accId,logIds) {
	var result=[];
	$.ajax({type:"POST",url:"/r/syslog/list4BankLogs",data:{"accId":accId,logIds:logIds.toString()},dataType:'json',async:false,success:function(jsonObject){
		if(jsonObject.status == 1){
			result = jsonObject.data;
		}else{
			showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
		}
	}});
	return result;
};

var getInvstByErrorId = function(errorId){
	var result=[];
	$.ajax({type:"POST",url:"/r/problem/findInvstByErrorId",data:{"errorId":errorId},dataType:'json',async:false,success:function(jsonObject){
		if(jsonObject.status == 1){
			result = jsonObject.data;
		}else{
			showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
		}
	}});
	return result;
};

var getErrorByErrorId = function(errorId){
	var result={};
	$.ajax({type:"POST",url:"/r/problem/findErrorByErrorId",data:{"errorId":errorId},dataType:'json',async:false,success:function(jsonObject){
		if(jsonObject.status == 1){
			result = jsonObject.data;
		}else{
			showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
		}
	}});
	return result;
};

var showAccInvstWatchModal = function(errId,accId){
	$.ajax({type:"GET",async:false,dataType:'html',url : "/"+sysVersoin+"/html/common/bankLogInvstList.html",
		success : function(html){
			var $div=$(html).find("#accInvstWatch_modal").clone().appendTo($("body"));
			$div$ = $div;
			$div.modal("toggle");
			var result=getAccountInfoById(accId);
			if(result){
				targetSpareTime = result.flag ? result.flag : 0;
				$div.find("#targetBankType").text(result.bankType);
				$div.find("#targetAccount").text(hideAccountAll(result.account));
				$div.find("#targetOwner").text((result.owner?hideName(result.owner):'无'));
				$div.find("#targetBalance").text(result.balance+'元');
				$div.find("#targetBankBalance").text(result.bankBalance+'元');
				$div.find("#targetMargin").text((setAmountAccuracy(result.bankBalance-result.balance))+'元');
				if(!targetSpareTime){
					$div.find("#targetSpareTime").remove();
				}else{
					targetPeakBalance = result.peakBalance;
					$div.find("#targetPeakBalance").text(result.peakBalance);
				}
			}else{
				showMessageForFail("账号信息查询异常，请刷新页面");
			}
			var logsHtml = '';
			var invst =  getInvstByErrorId(errId);
			if(invst&&invst.length >0 ){
				$.each(invst,function(index,record){
					logsHtml = logsHtml+ '<tr>';
					logsHtml = logsHtml+ '<td>'+moment(new Date(record.createTime)).format('YYYY-MM-DD HH:mm:ss')+'</td>';
					logsHtml = logsHtml+ '<td>'+(record.oppAccount?hideAccountAll(record.oppAccount):'无')+'</td>';
					logsHtml = logsHtml+ '<td>'+(record.oppOwner?hideName(record.oppOwner):'无')+'</td>';
					logsHtml = logsHtml+ '<td>'+accInvstTypeName(record.type)+'</td>';
					logsHtml = logsHtml+ '<td>'+record.amount+'</td>';
					logsHtml = logsHtml+ '<td>'+record.orderNo+'</td>';
					logsHtml = logsHtml+ '</tr>';
				});
			}
			$div.find("#errorBankLogList").html(logsHtml);
			var error = getErrorByErrorId(errId);
			$div.find("#errorInvstRemark").html(error.lastRemark);
			$div.find("#errorInvstFooter").html("排查操作人："+error.collectorName+" 操作时间："+moment(new Date(error.finishTime)).format('YYYY-MM-DD HH:mm:ss'));
			$div.find("#accInvstResult").html(error.status==3?"恢复启用":"从冻结流程");
			$div.on('hidden.bs.modal', function () {
				$div.remove();
			});
		}
	});
};

var accInvDoing = function(errId,target){
	var bankIdList = errrBankIdList[''+target+''];
	if(!bankIdList||bankIdList.length ==0){
		showMessageForFail('请先选择异常流水');
		return;
	}
	showAccInvstModal(errId,target,bankIdList,queryProblem4AccInv);
};

