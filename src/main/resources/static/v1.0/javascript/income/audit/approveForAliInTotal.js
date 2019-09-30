//初始化盘口
var inithancipad=function(name){
	var $selectHandicap = $("select[name='"+name+"']").html("");
	$.ajax(
			{dataType:'json',
			 async:false,
			 type:"get",
			 url:"/r/handicap/list",
			 data:{enabled:1},success:function(jsonObject){
		if(jsonObject.status == 1){
			$selectHandicap.html("");
			$('<option></option>').html('全部').attr('value','0').attr('selected','selected').attr("handicapCode","").appendTo($selectHandicap);
			for(var index in jsonObject.data){
				var item = jsonObject.data[index];
				$('<option></option>').html(item.name).attr('value',item.id).attr("handicapCode",item.code).appendTo($selectHandicap);
			}
		}
	}});
}
 
//初始化时间
function initTime(){
	//正在匹配
	initTimePicker(true,$("[name=time_Ali_total]"),typeCustomLatestToday);
	//已匹配
	initTimePicker(true,$("[name=timeScope_AliMatched]"),typeCustomLatestToday);
	//未认领
	//startTime = $.trim(moment().subtract(2, 'days').format("YYYY-MM-DD HH:mm:ss"));
   // endTime = $.trim(moment().subtract(1, 'days').format("YYYY-MM-DD HH:mm:ss"));
	//initTimePicker(true,$("[name=timeScope_wetchatUnClaim]"),null,startTime+" - "+endTime);
	//已取消
	initTimePicker(true,$("[name=timeScope_wetchatCanceled]"),typeCustomLatestToday);
}

//根据支付宝号统计正在匹配的流水
var queryMatchingAliTotal=function(){
	if (sertype!="Matching" || currentPageLocation.indexOf('IncomeAuditAliInTotal:*') <= -1) {
        return;
    }
	//当前页码
	var CurPage=$("#Ali_total_page").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#handicap_Ali_total").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='time_Ali_total']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//支付宝号
	var AliNumber=$("#AliNumber").val();
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditAliInTotal/findAliLogByWechar",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"AliNumber":AliNumber,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				 var tr = '';
				 var idList=new Array();
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
					 var statusStr="";
					 if(val.accountStatus==1)
						 statusStr="在用";
					 if(val.accountStatus==3)
						 statusStr="冻结";
					 if(val.accountStatus==4)
						 statusStr="停用";
					 if(val.accountStatus==5)
						 statusStr="可用";
					 idList.push({'id':val.fromAccount});
                     tr += '<tr style=cursor:pointer onclick="AliMatching_show('+val.fromAccount+');rememberIds('+val.fromAccount+');" id=AliTr'+val.fromAccount+'>'
                	      +'<td>'+val.handicapName+'</td>'
                	      +'<td>' 
                	      	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.fromAccount+"' data-placement='rigth' data-trigger='hover'  >"+hideAccountAll(val.account) 
                	      +'</td>'
                	      +'<td>'+val.owner+'</td>';
                	      //不同状态使用不同颜色
	          			  if(val.accountStatus==accountStatusFreeze || val.accountStatus==accountStatusStopTemp){
	          				  tr+="<td><span class='label label-sm label-danger'>"+statusStr+"</span></td>";
	          			  }else{
	          				  tr+="<td><span class='label label-sm label-success'>"+statusStr+"</span></td>";
	          			  }
                          tr+='<td>' + "" + '</td>'
                	      +'<td>' 
	                	      +'<div class="action-buttons">'
		                          +'<a class="red bigger-140 show-details-btn" title="待匹配明细">'
		                              +'<i id=i'+val.fromAccount+' class="ace-icon fa fa-angle-double-down">'+val.counts+'</i>'
		                              +'<span class="sr-only">待匹配明细</span>'
		                          +'</a>'
	                          +'</div>'
                	      +'</td>'
                       +'</tr>';
                 }
				 $('#Ali_total_tbody').empty().html(tr);
				 var trn = '<tr>'
					 		+'<td colspan="6">总计：'+jsonObject.data.page.totalElements+'</td>'
     	                  +'</tr>';
				 $('#Ali_total_tbody').append(trn);
			}else{
				$('#Ali_total_tbody').empty().html('<tr></tr>');
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.page,"Ali_total_page",queryMatchingAliTotal);
		}
	});
	setTimeout("showHistory()",100);
}


function showHistory(){
	for(var i=0;i<AliIds.length;i++){
		AliMatching_show(AliIds[i]);
	}
}


//根据支付宝号 查找待匹配的流水和入款单。
function queryMBAndInvoice(AliId){
	//提单当前页码
	var invoiceCurPage=$("#COPY"+AliId).find("#invoice_footPage").find(".Current_Page").text();
	if(!!!invoiceCurPage){
		invoiceCurPage=0;
	}else{
		invoiceCurPage=invoiceCurPage-1;
	}if(invoiceCurPage<0){
		invoiceCurPage=0;
	}
	//流水当前页码
	var banklogCurPage=$("#COPY"+AliId).find("#banklog_footPage").find(".Current_Page").text();
	if(!!!banklogCurPage){
		banklogCurPage=0;
	}else{
		banklogCurPage=banklogCurPage-1;
	}if(banklogCurPage<0){
		banklogCurPage=0;
	}
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='time_Ali_total']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//会员号
	var member=$("#member_AliToMatch").val(); 
	//订单号
	var orderNo=$("#orderNo_AliToMatch").val();
	//开始金额
	var fromAmount=$("#fromMoney_AliToMatch").val();
	//结束金额
	var toAmount=$("#toMoney_AliToMatch").val();
	//存款人
	var payer=$("#payer_AliToMatch").val();
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditAliInTotal/findMBAndInvoice",
		data:{
			"invoicePageNo":invoiceCurPage,
			"banklogPageNo":banklogCurPage,
			"AliId":AliId,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"member":member,
			"orderNo":orderNo,
			"fromAmount":fromAmount,
			"toAmount":toAmount,
			"payer":payer,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			//订单号
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.invoiceArrlist.length > 0){
				 var tr = '';
				 var counts = 0;
				 var amounts=0;
				 for(var index in jsonObject.data.invoiceArrlist){
					 var val = jsonObject.data.invoiceArrlist[index];
                     tr += '<tr style="cursor: pointer;" onclick="_checkSysOrderTr(this)">'
	                	      +'<td id=sysMemberName>'+val.memberName+'</td>'
	                	      +'<td id=sysAccount  style="display: none;">'+val.account+'</td>'
	                	      +'<td id=sysAccountId  style="display: none;">'+val.aliPayid+'</td>'
	                	      +'<td id=sysAmount>'+val.amount+'</td>'
	                	      +'<td id=sysOrderNo>'+val.orderNo+'</td>'
	                	      +'<td id=sysCreateTime>'+val.createTime+'</td>';
	                	      if (_checkObj(val.remark)) {
                                  if (_checkObj(val.remark).length > 10) {
                                      tr +=
                                          '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                          + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                          + ' data-content="' + _checkObj(val.remark) + '">'
                                          + _checkObj(val.remark).substring(0, 10) + "..."
                                          + '</a></td>';
                                  } else {
                                      tr +=
                                          '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                          + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                          + ' data-content="' + _checkObj(val.remark) + '">'
                                          + _checkObj(val.remark)
                                          + '</a></td>';
                                  }

                              } else {
                                  tr += '<td></td>';

                              }
                        +'</tr>';
                     counts +=1;
                     amounts+=val.amount;
                 }
				 $("#COPY"+AliId).find('#tbody_invoice').empty().html(tr);
				 var trs = '<tr>'
			            +'<td colspan="1">小计：'+counts+'</td>'
			            +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
		                +'<td colspan="3"></td>'
		         +'</tr>';
				 $("#COPY"+AliId).find('#tbody_invoice').append(trs);
				 var trn = '<tr>'
     	            +'<td colspan="1">总计：'+jsonObject.data.invoiceDataToPage.totalElements+'</td>'
     	            +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.invoiceTotal[0]+'</td>'
     	            +'<td colspan="3"></td>'
		          +'</tr>';
				 $("#COPY"+AliId).find('#tbody_invoice').append(trn);
			}else{
				$("#COPY"+AliId).find('#tbody_invoice').empty().html('<tr></tr>');
			}
			//分页初始化
			showPading(jsonObject.data.invoiceDataToPage,("COPY"+AliId+" #invoice_footPage"),queryMBAndInvoice);
			//流水
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.bankLogArrlist.length > 0){
				 var tr = '';
				 var counts = 0;
				 var amounts=0;
				 for(var index in jsonObject.data.bankLogArrlist){
					 var val = jsonObject.data.bankLogArrlist[index];
                    tr += '<tr style="cursor:pointer;" onclick="_checkBankFlowTr(this)">'
	                	      +'<td id=bankDepositor>'+val.depositor+'</td>'
	                	      +'<td id=bankAmount>'+val.amount+'</td>'
	                	      +'<td id=bankTradingTime>'+val.trTime+'</td>'
	                	      +'<td id=bankCreateTime>'+val.crTime+'</td>'
	                	      +'<td id=bankSummary>'+val.summary+'</td>';
	                	      if (_checkObj(val.remark)) {
                                  if (_checkObj(val.remark).length > 10) {
                                      tr +=
                                          '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                          + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                          + ' data-content="' + _checkObj(val.remark) + '">'
                                          + _checkObj(val.remark).substring(0, 10) + "..."
                                          + '</a></td>';
                                  } else {
                                      tr +=
                                          '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                          + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                          + ' data-content="' + _checkObj(val.remark) + '">'
                                          + _checkObj(val.remark)
                                          + '</a></td>';
                                  }

                              } else {
                                  tr += '<td></td>';

                              }
                       +'</tr>';
                    counts +=1;
                    amounts+=val.amount;
                }
				 $("#COPY"+AliId).find('#tbody_banklog').empty().html(tr);
				 var trs = '<tr>'
			            +'<td colspan="1">小计：'+counts+'</td>'
			            +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
		                +'<td colspan="4"></td>'
		         +'</tr>';
				 $("#COPY"+AliId).find('#tbody_banklog').append(trs);
				 var trn = '<tr>'
    	            +'<td colspan="1">总计：'+jsonObject.data.bankLogDataToPage.totalElements+'</td>'
    	            +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.bankLogTotal[0]+'</td>'
    	            +'<td colspan="4"></td>'
		          +'</tr>';
				 $("#COPY"+AliId).find('#tbody_banklog').append(trn);
			}else{
				$("#COPY"+AliId).find('#tbody_banklog').empty().html('<tr></tr>');
			}
			 $("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.bankLogDataToPage,("COPY"+AliId+" #banklog_footPage"),queryMBAndInvoice);
		}
	});
}
var AliIds=new Array()
function rememberIds(id){
	if(AliIds.indexOf(id)<0){
		AliIds.push(id);
	}
}


var AliMatching_show=function(AliId){
	$("#i"+AliId).removeClass("fa-angle-double-down").addClass("fa-angle-double-up");
	var $tr=$("#AliTr"+AliId);
	//事件切换
	$tr.attr('onclick', '').unbind("click").click(function(){
		AliMatching_hide(AliId);
	});
	//拼接DIV
	var $td=$("<td colspan='8' style='padding:10px !important;'></td>").append($(".table-detail").clone().attr("id", "COPY" + AliId).removeClass("table-detail").removeClass("hide"));
	$tr.after($("<tr class='Ali_matchingTr' id='Ali_matching"+AliId+"' ></tr>").append($td));
	queryMBAndInvoice(AliId);
}

var AliMatching_hide=function(AliId){
	if(AliIds.indexOf(AliId)>=0){
		AliIds.splice(AliIds.indexOf(AliId),1);
	}
	$("#i"+AliId).removeClass("fa-angle-double-up").addClass("fa-angle-double-down");
	var $tr=$("#AliTr"+AliId);
	//事件切换
	$tr.attr('onclick', '').unbind("click").click(function(){
		AliMatching_show(AliId);
	});
	$("#Ali_matching"+AliId).remove();
}

//查询已经匹配的单
var queryMatched=function(){
	//当前页码
	var CurPage=$("#AliMatched_footPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#handicap_AliMatched").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='timeScope_AliMatched']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//会员号
	var member=$("#member_AliMatched").val(); 
	//订单号
	var orderNo=$("#orderNo_AliMatched").val();
	//开始金额
	var fromAmount=$("#fromMoney_AliMatched").val();
	//结束金额
	var toAmount=$("#toMoney_AliMatched").val();
	//存款人
	var payer=$("#payer_AliMatched").val();
	//支付宝号
	var AliNumber=$("#payer_AliNo_AliMatched").val();
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditAliInTotal/findAliMatched",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"member":member,
			"orderNo":orderNo,
			"fromAmount":fromAmount,
			"toAmount":toAmount,
			"payer":payer,
			"AliNumber":AliNumber,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				 var tr = '';
				 var idList=new Array();
				 var counts = 0;
				 var amounts=0;
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
					 idList.push({'id':val.aliPayid});
                     tr += '<tr>'
                	      +'<td>'+val.handicapName+'</td>'
                	      +'<td>'+val.memberName+'</td>'
                	      +'<td>'+val.orderNo+'</td>'
                	      +'<td>'+val.amount+'</td>'
                	      +'<td>'+val.depositor+'</td>'
                	      +'<td>' 
                	      	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.aliPayid+"' data-placement='left' data-trigger='hover'  >"+hideAccountAll(val.account) 
                	      +'</td>'
                	      +'<td>'+val.createTime+'</td>'
                	      +'<td>'+val.updateTime+'</td>'
                	      +'<td>'+(formatDuring(new Date(val.updateTime).getTime()-new Date(val.createTime).getTime()))+'</td>'
                	      if (_checkObj(val.remark)) {
                              if (_checkObj(val.remark).length > 10) {
                                  tr +=
                                      '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                      + ' data-content="' + _checkObj(val.remark) + '">'
                                      + _checkObj(val.remark).substring(0, 10) + "..."
                                      + '</a></td>';
                              } else {
                                  tr +=
                                      '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                      + ' data-content="' + _checkObj(val.remark) + '">'
                                      + _checkObj(val.remark)
                                      + '</a></td>';
                              }

                          } else {
                              tr += '<td></td>';
                          }
                       +'</tr>';
                     counts +=1;
                     amounts+=val.amount;
                 }
				 $('#tbody_AliMatched').empty().html(tr);
				 var trs = '<tr>'
			            +'<td colspan="3">小计：'+counts+'</td>'
			            +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
		                +'<td colspan="6"></td>'
		         +'</tr>';
				 $('#tbody_AliMatched').append(trs);
				 var trn = '<tr>'
     	            +'<td colspan="3">总计：'+jsonObject.data.page.totalElements+'</td>'
     	            +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.total[0]+'</td>'
     	            +'<td colspan="6"></td>'
		          +'</tr>';
				 $('#tbody_AliMatched').append(trn);
			}else{
				$('#tbody_AliMatched').empty().html('<tr></tr>');
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			 $("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.page,"AliMatched_footPage",queryMatched);
		}
	});
}

//查询已经取消的单
var queryCanceled=function(){
	//当前页码
	var CurPage=$("#wetchatCanceled_footPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#handicap_wetchatCanceled").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='timeScope_wetchatCanceled']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//会员号
	var member=$("#member_wetchatCanceled").val(); 
	//订单号
	var orderNo=$("#orderNo_wetchatCanceled").val();
	//开始金额
	var fromAmount=$("#fromMoney_wetchatCanceled").val();
	//结束金额
	var toAmount=$("#toMoney_wetchatCanceled").val();
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditAliInTotal/findAliCanceled",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"member":member,
			"orderNo":orderNo,
			"fromAmount":fromAmount,
			"toAmount":toAmount,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				 var tr = '';
				 var idList=new Array();
				 var counts = 0;
				 var amounts=0;
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
					 idList.push({'id':val.aliPayid});
                     tr += '<tr>'
                	      +'<td>'+val.handicapName+'</td>'
                	      +'<td>' 
	              	      	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.aliPayid+"' data-placement='right' data-trigger='hover'  >"+hideAccountAll(val.account) 
	              	      +'</td>'
                	      +'<td>'+val.memberName+'</td>'
                	      +'<td>'+val.orderNo+'</td>'
                	      +'<td>'+val.amount+'</td>'
                	      +'<td>'+val.updateTime+'</td>'
                	      if (_checkObj(val.remark)) {
                              if (_checkObj(val.remark).length > 10) {
                                  tr +=
                                      '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                      + ' data-content="' + _checkObj(val.remark) + '">'
                                      + _checkObj(val.remark).substring(0, 10) + "..."
                                      + '</a></td>';
                              } else {
                                  tr +=
                                      '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                      + ' data-content="' + _checkObj(val.remark) + '">'
                                      + _checkObj(val.remark)
                                      + '</a></td>';
                              }

                          } else {
                              tr += '<td></td>';
                          }
                       +'</tr>';
                     counts +=1;
                     amounts+=val.amount;
                 }
				 $('#tbody_WetchatCanceled').empty().html(tr);
				 var trs = '<tr>'
			            +'<td colspan="4">小计：'+counts+'</td>'
			            +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
		                +'<td colspan="2"></td>'
		         +'</tr>';
				 $('#tbody_WetchatCanceled').append(trs);
				 var trn = '<tr>'
     	            +'<td colspan="4">总计：'+jsonObject.data.page.totalElements+'</td>'
     	            +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.total[0]+'</td>'
     	            +'<td colspan="2"></td>'
		          +'</tr>';
				 $('#tbody_WetchatCanceled').append(trn);
			}else{
				$('#tbody_WetchatCanceled').empty().html('<tr></tr>');
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			 $("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.page,"wetchatCanceled_footPage",queryCanceled);
		}
	});
}

//查询未认领的流水
var queryUnClaim=function(){
	//当前页码
	var CurPage=$("#wetchatUnClaim_footPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#handicap_wetchatUnClaim").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='timeScope_wetchatUnClaim']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//存款人
	var member=$("#member_wetchatUnClaim").val(); 
	//支付宝号
	var AliNo=$("#payer_AliNo_wetchatUnClaim").val();
	//开始金额
	var fromAmount=$("#fromMoney_wetchatUnClaim").val();
	//结束金额
	var toAmount=$("#toMoney_wetchatUnClaim").val();
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditAliInTotal/findAliUnClaim",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"member":member,
			"AliNo":AliNo,
			"fromAmount":fromAmount,
			"toAmount":toAmount,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				 var tr = '';
				 var idList=new Array();
				 var counts = 0;
				 var amounts=0;
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
					 idList.push({'id':val.fromAccount});
                     tr += '<tr>'
                	      +'<td>'+val.handicapName+'</td>'
                	      +'<td>' 
	              	      	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.fromAccount+"' data-placement='right' data-trigger='hover'  >"+hideAccountAll(val.account) 
	              	      +'</td>'
                	      +'<td>'+val.depositor+'</td>'
                	      +'<td>'+val.amount+'</td>'
                	      +'<td>'+val.trTime+'</td>'
                	      +'<td>'+val.crTime+'</td>'
                	      if (_checkObj(val.remark)) {
                              if (_checkObj(val.remark).length > 10) {
                                  tr +=
                                      '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                      + ' data-content="' + _checkObj(val.remark) + '">'
                                      + _checkObj(val.remark).substring(0, 10) + "..."
                                      + '</a></td>';
                              } else {
                                  tr +=
                                      '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                      + ' data-content="' + _checkObj(val.remark) + '">'
                                      + _checkObj(val.remark)
                                      + '</a></td>';
                              }

                          } else {
                              tr += '<td></td>';
                          }
                       +'</tr>';
                     counts +=1;
                     amounts+=val.amount;
                 }
				 $('#tbody_wetchatUnClaim').empty().html(tr);
				 var trs = '<tr>'
			            +'<td colspan="3">小计：'+counts+'</td>'
			            +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
		                +'<td colspan="4"></td>'
		         +'</tr>';
				 $('#tbody_wetchatUnClaim').append(trs);
				 var trn = '<tr>'
     	            +'<td colspan="3">总计：'+jsonObject.data.page.totalElements+'</td>'
     	            +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.total[0]+'</td>'
     	            +'<td colspan="4"></td>'
		          +'</tr>';
				 $('#tbody_wetchatUnClaim').append(trn);
			}else{
				$('#tbody_wetchatUnClaim').empty().html('<tr></tr>');
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			 $("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.page,"wetchatUnClaim_footPage",queryUnClaim);
		}
	});
}

var sertype="Matching";
function change(type){
	sertype=type;
}
function _addTimeSelect() {
    var opt = '<option  value="0" >不刷新</option><option  selected="selected" value="15">15秒</option><option  value="30">30秒</option>' +
        '<option  value="60">60秒</option><option  value="120">120秒</option><option  value="180">180秒</option>';
    $('#autoUpdateTimeAliInTotal').empty().append(opt);
}
$('#autoUpdateTimeAliInTotal').unbind().bind('change', function () {
	_searchIncomeAuditAliInTotal();
});

jQuery(function($) {
	inithancipad("handicap_Ali_total");
	inithancipad("handicap_AliMatched");
	inithancipad("handicap_wetchatCanceled");
	inithancipad("handicap_wetchatUnClaim");
	$('#freshAliInTotalLi').show();
	_addTimeSelect();
	_searchIncomeAuditAliInTotal();
	initTime();
	queryMatchingAliTotal();
	queryMatched();
	queryCanceled();
	queryUnClaim();
})