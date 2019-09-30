currentPageLocation = window.location.href;

var banklog_amountTotal=false;
$.each(ContentRight['OtherPermission:*'], function (name, value) {
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
	}else if(logType=='type0'){//匹配中
		$div.find(".statusSearch").hide();
		bankLogList_In_OutList(0);
	}else if(logType=='problem'){//排查记录
		problemList(0);
	}
}

var changeOrderBy=function(orderBy){
	var $div=$("#bankLogList_modal");
	$div.find("table#list tbody").html("");
	$div.find("#orderByTh").val(orderBy);
	bankLogList_In_OutList(0);
}

var changeInOutTypeInit=function(type){
	var $div=$("#bankLogList_modal");
	reset('filter');
	$div.find("input[name=tabType]").val(type);
	//刷新数据
	bankLogList_In_OutList(0);
}

var changeInOutTypeInit_Third=function(type){
	var $div=$("#thirdLogList_inOut_modal");
	$div.find("#filter input").val("");
	$div.find("input[name=tabType]").val(type);
	//刷新数据
	thirdLogList_In_OutList(0);
}
var do_exportBnakLog=function(){
	var $div=$("#bankLogList_modal");
	var startAndEndTime=$div.find("[name=startAndEndTime]").val();
	//拼接URL
	var url="/r/exportaccount/bankLog/"+$div.find("#accountInfo_id").val();
	if(startAndEndTime&&startAndEndTime.length>0){
		url+="/"+getTimeArray(startAndEndTime).toString();
	}else{
		url+="/0";
	}
	$div.find("#exportBankLogBtn").attr("href",url+"/0");
}
/**
 * defaultBSTime 银行流水TAB控件默认时间
 */
//明细弹出框
var showInOutListModal=function(accountId,noDefaultBankLogTime,defaultBSTime){
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/bankLogList.html", 
		success : function(html){
			var $div=$(html).find("#bankLogList_modal").clone().appendTo($("body"));
				$div.find("[name=search_IN_status]").html(getBanklogStatusOptionList());
				$div.find("input#accountInfo_id").val(accountId);
				$div.modal("toggle");
				//加载时间控件
				if(noDefaultBankLogTime){
					initTimePicker(false,$div.find("[name=startAndEndTime]"));
				}else{
					initTimePicker(true,$div.find("[name=startAndEndTime]"),7);
				}
				initTimePicker(true,$div.find("[name=startAndEndTime_sys]"),7);
				if(defaultBSTime){
					//银行流水设置默认值
					$div.find("[name=startAndEndTime]").val(defaultBSTime);
					$div.find("[name=startAndEndTime_sys]").val(defaultBSTime);
					$(":checkbox[name='searchTypeIn0Out1'][value=1]").prop("checked", true);
					$(":checkbox[name='searchTypeIn0Out1'][value=0]").prop("checked", false);
				}
				initTimePicker(true,$div.find("[name=startAndEndTime_problem]"),7);
				//数据列表
				sysLogList(0);
				//查询账号信息
				var result=getAccountMoreInfoById(accountId);
				if(result&&result.account){
					$div.find("input#accountInfo_alias").val(result.account.alias);
					var accountInfo=result.account;
					if(accountInfo){
						var accountTitle="";
						var handicapInfo=getHandicapInfoById(accountInfo.handicapId);
						if(handicapInfo){
							accountTitle+=handicapInfo.name+'|';
						}
						if(accountInfo.type==accountTypeInBank){
							accountTitle+=getFlagMoreStrHTML(accountInfo.flagMoreStr)+'|';
						}
						accountTitle+=(accountInfo.alias?accountInfo.alias:'无')+'|'+(accountInfo.bankType?accountInfo.bankType:'无')+'|'+(accountInfo.owner?hideName(accountInfo.owner):'无')+'|'+hideAccountAll(accountInfo.account);
						$div.find("#accountInfo_account").text(accountTitle);
						$div.find("#accountInfo_id_span").text(accountInfo.id);
						$div.find("#accountInfo_bankBalance").text(accountInfo.bankBalance?accountInfo.bankBalance:0);
						$div.find("#accountInfo_noOwnerBalance").text(result.sumAmountCondition);
						$div.find("#incomeDailyTotal").text(result.incomeDailyTotal);
						$div.find("#outDailyTotal").text(result.outDailyTotal);
						$div.find("#queryIncomeTotal").text(result.queryIncomeTotal);
						$div.find("#queryOutTotal").text(result.queryOutTotal);
						if(!banklog_amountTotal){
							$div.find("#exportBankLogBtn").hide();
							$div.find("#bankLog_accountInfo_total").hide();
						}
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
				$(document).keypress(function(e){
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
}
var bankLogList_In_OutListById=function(CurPage){
	$("#bankLogList_modal #orderByTh").val("id");
	bankLogList_In_OutList();
}

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
			var tbody="",totalBankLogAmount_Plus=0,totalBankLogAmount_Nagetive=0,idList=new Array();
			$.each(jsonObject.data,function(index,record){
				var tr="";
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
						3:{subTotal:banklog_amountTotal?totalBankLogAmount_Plus:'0',total:amountPlus},
						4:{subTotal:banklog_amountTotal?totalBankLogAmount_Nagetive:'0',total:amountNegative}
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
}
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
			var tbody="",totalBankLogAmount_Plus=0,totalBankLogAmount_Nagetive=0,totalFee=0,idList=new Array(),subCount=0;
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
						column:12, 
						subCount:subCount,
						count:jsonObject.page.totalElements,
						4:{subTotal:totalBankLogAmount_Plus,total:jsonObject.page.header.amountPlus},
						5:{subTotal:totalBankLogAmount_Nagetive,total:jsonObject.page.header.amountNagetive},
						6:{subTotal:totalFee,total:jsonObject.page.header.feeTotal},
						7:{subTotal:(totalBankLogAmount_Plus*1+totalBankLogAmount_Nagetive*1),total:(jsonObject.page.header.amountPlus*1+jsonObject.page.header.amountNagetive*1)}
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
}
var problemList=function(CurPage){
	var $div=$("#bankLogList_modal");
	//封装查询条件
	if(!!!CurPage&&CurPage!=0) CurPage=$("#problemPage .Current_Page").text();
	var startAndEndTimeToArray=getTimeArray($div.find("[name=startAndEndTime_problem]").val());
	//查询条件顺序勿更换，提升SQL效率
	var formData={
		"search_GTE_amount":$.trim($div.find("[name=qstartAmount_problem]").val()),
		"search_LTE_amount":$.trim($div.find("[name=qendAmount_problem]").val()),
		"search_GTE_occurTime":$.trim(startAndEndTimeToArray[0]),
		"search_LTE_occurTime":$.trim(startAndEndTimeToArray[1]),
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
}



var thirdLogList_In_OutList=function(CurPage){
	var $div=$("#thirdLogList_inOut_modal");
	//封装查询条件
	if(!!!CurPage&&CurPage!=0) CurPage=$("#thirdLogList_inOut_Page .Current_Page").text();
	var startAndEndTimeToArray=getTimeArray($div.find("#filter [name=startAndEndTime]").val());
	var formData={
			"fromAccount":$div.find("#accountInfo_id").val(),
			"orderNo":$.trim($div.find("#filter [name=orderNo]").val()),
			"startAndEndTimeToArray":startAndEndTimeToArray.toString(),
			"pageNo" : CurPage<=0?0:CurPage-1,
			"pageSize":$.session.get('initPageSize')
		};
	var tabType=$div.find("input[name=tabType]").val();
	var amountStart=$div.find("#filter #qstartAmount").val();
	var amountEnd=$div.find("#filter #qendAmount").val();
	//根据转入转出来自动区分查询类型
	if(tabType=='in'){//查询转入记录
		formData["searchTypeIn0Out1"]=0;
		//金额
		formData["minAmount"]=amountStart;
		formData["maxAmount"]=amountEnd;
	}else if(tabType=='out'){//查询转出记录
		formData["searchTypeIn0Out1"]=1;
		//金额为负数的区间值，调换顺序加上负号
		formData["minAmount"]=amountEnd?amountEnd*-1:"";
		formData["maxAmount"]=amountStart?amountStart*-1:"";
	}
	$.ajax({
		dataType : 'JSON',
		type : "POST",
		async:false,
		url : "/r/thirdlog/findbyfrom",
		data : formData,
		success : function(jsonObject) {
			if(jsonObject.status !=1){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var sysDate=new Date();
			var $tbody=$div.find("table#list tbody"),htmlStr="";
			var totalThirdLogAmount=0,totalThirdLogFee=0;
			$.each(jsonObject.data,function(index,record){
				var tr="";
				tr+="<td><span>"+(record.tradingTime?timeStamp2yyyyMMddHHmmss(record.tradingTime):'无')+"</span></td>";
				tr+="<td><span>"+(record.amount?record.amount:0)+"</span></td>";
				tr+="<td><span>"+(record.fee?record.fee:0)+"</span></td>";
				tr+="<td><span>"+(record.orderNo?record.orderNo:'无')+"</span></td>";
				tr+="<td><a style='width:330px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+record.remark+"'>"+(record.remark?record.remark:'')+"</a></td>";
				htmlStr+="<tr id='"+record.id+"'>"+tr+"</tr>";
				totalThirdLogAmount+=record.amount*1;
				totalThirdLogFee+=record.fee?record.fee*1:0;
			});
			$tbody.html(htmlStr);
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var totalRows={
						column:12, 
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements,
						2:{subTotal:totalThirdLogAmount,total:jsonObject.page.header?jsonObject.page.header.totalAmount:0},
						3:{subTotal:totalThirdLogFee,total:jsonObject.page.header?jsonObject.page.header.totalFee:0}
					};
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			showPading(jsonObject.page,"thirdLogList_inOut_Page",thirdLogList_In_OutList);
		}
	});
}


//第三方账号流水明细，收入 支出
var show_third_InOutListModal=function(accountId){
	//查询账号信息
	var result=getAccountMoreInfoById(accountId);
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/bankLogListBAK.html", 
		success : function(html){
			var $div=$(html).find("#thirdLogList_inOut_modal").clone().appendTo($("body"));
			if(result&&result.account){
				$div.find("input#accountInfo_id").val(accountId);
				var accountInfo=result.account;
				if(accountInfo){
					var accountTitle=(accountInfo.bankType?accountInfo.bankType:'无')+(accountInfo.owner?accountInfo.owner:'无')+'|'+hideAccountAll(accountInfo.account);
					$div.find("#accountInfo_account").text(accountTitle);
					$div.find("#accountInfo_id_span").text(accountInfo.id);
					$div.find("#accountInfo_bankBalance").text(accountInfo.bankBalance?accountInfo.bankBalance:0);
					$div.find("#accountInfo_noOwnerBalance").text(result.sumAmountCondition);
					$div.find("#incomeDailyTotal").text(result.incomeDailyTotal);
					$div.find("#outDailyTotal").text(result.outDailyTotal);
					$div.find("#queryIncomeTotal").text(result.queryIncomeTotal);
					$div.find("#queryOutTotal").text(result.queryOutTotal);
				}
				//加载时间控件
				initTimePicker(true,$("[name=startAndEndTime]"),7);
				//数据列表
				thirdLogList_In_OutList(0);
				$div.modal("toggle");
				$(document).keypress(function(e){
					if(event.keyCode == 13) {
						$div.find("#searchBtn button").click();
					}
				});
			}else{
				showMessageForFail("账号信息查询异常，请刷新页面");
			}
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}



