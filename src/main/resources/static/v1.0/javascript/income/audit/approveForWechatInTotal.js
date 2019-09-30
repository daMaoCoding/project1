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
	initTimePicker(true,$("[name=time_WechatTotal_total]"),typeCustomLatestToday);
	//已匹配
	initTimePicker(true,$("[name=timeScope_WechatTotalMatched]"),typeCustomLatestToday);
	//未认领
	//startTime = $.trim(moment().subtract(2, 'days').format("YYYY-MM-DD HH:mm:ss"));
    //endTime = $.trim(moment().subtract(1, 'days').format("YYYY-MM-DD HH:mm:ss"));
	//initTimePicker(true,$("[name=timeScope_wetchatUnClaim]"),null,startTime+" - "+endTime);
	//已取消
	initTimePicker(true,$("[name=timeScope_wetchatCanceled]"),typeCustomLatestToday);
}

//根据微信号统计正在匹配的流水
var queryMatchingWechatTotal=function(){
	if (sertype!="Matching" || currentPageLocation.indexOf('IncomeAuditWechatInTotal:*') <= -1) {
        return;
    }
	//当前页码
	var CurPage=$("#WechatTotal_total_page").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#handicap_WechatTotal_total").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='time_WechatTotal_total']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//微信号
	var WechatTotalNumber=$("#WechatTotalNumber").val();
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditWechatTotalIn/findWechatTotalLogByWechar",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"WechatTotalNumber":WechatTotalNumber,
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
					 idList.push({'id':val.fromAccount,'type':'ali_WechatTotal'});
                     tr += '<tr style=cursor:pointer onclick="WechatTotalMatching_show('+val.fromAccount+');rememberIds('+val.fromAccount+');" id=WechatTotalTr'+val.fromAccount+'>'
                	      +'<td>'+val.handicapName+'</td>'
                	      +'<td>' 
                	      	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.fromAccount+"' data-placement='right' data-trigger='hover'  >"+hideAccountAll(val.account) 
                	      +'</td>'
                	      +'<td>'+ val.owner+'</td>';
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
				 $('#WechatTotal_total_tbody').empty().html(tr);
				 var trn = '<tr>'
					 		+'<td colspan="6">总计：'+jsonObject.data.page.totalElements+'</td>'
     	                  +'</tr>';
				 $('#WechatTotal_total_tbody').append(trn);
			}else{
				$('#WechatTotal_total_tbody').empty().html('<tr></tr>');
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.page,"WechatTotal_total_page",queryMatchingWechatTotal);
		}
	});
	setTimeout("showHistory()",200);
}


function showHistory(){
	for(var i=0;i<WechatTotalIds.length;i++){
		WechatTotalMatching_show(WechatTotalIds[i]);
	}
}


//根据微信号 查找待匹配的流水和入款单。
function queryMBAndInvoice(WechatTotalId){
	//提单当前页码
	var invoiceCurPage=$("#COPY"+WechatTotalId).find("#invoice_footPage").find(".Current_Page").text();
	if(!!!invoiceCurPage){
		invoiceCurPage=0;
	}else{
		invoiceCurPage=invoiceCurPage-1;
	}if(invoiceCurPage<0){
		invoiceCurPage=0;
	}
	//流水当前页码
	var banklogCurPage=$("#COPY"+WechatTotalId).find("#banklog_footPage").find(".Current_Page").text();
	if(!!!banklogCurPage){
		banklogCurPage=0;
	}else{
		banklogCurPage=banklogCurPage-1;
	}if(banklogCurPage<0){
		banklogCurPage=0;
	}
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='time_WechatTotal_total']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//会员号
	var member=$("#member_WechatTotalToMatch").val(); 
	//订单号
	var orderNo=$("#orderNo_WechatTotalToMatch").val();
	//开始金额
	var fromAmount=$("#fromMoney_WechatTotalToMatch").val();
	//结束金额
	var toAmount=$("#toMoney_WechatTotalToMatch").val();payer_WechatTotalMatched
	//存款人
	var payer=$("#payer_WechatTotalToMatch").val();
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditWechatTotalIn/findMBAndInvoice",
		data:{
			"invoicePageNo":invoiceCurPage,
			"banklogPageNo":banklogCurPage,
			"WechatTotalId":WechatTotalId,
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
	                	      +'<td id=sysAccountId  style="display: none;">'+val.WechatTotalid+'</td>'
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
				 $("#COPY"+WechatTotalId).find('#tbody_invoice').empty().html(tr);
				 var trs = '<tr>'
			            +'<td colspan="1">小计：'+counts+'</td>'
			            +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
		                +'<td colspan="3"></td>'
		         +'</tr>';
				 $("#COPY"+WechatTotalId).find('#tbody_invoice').append(trs);
				 var trn = '<tr>'
     	            +'<td colspan="1">总计：'+jsonObject.data.invoiceDataToPage.totalElements+'</td>'
     	            +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.invoiceTotal[0]+'</td>'
     	            +'<td colspan="3"></td>'
		          +'</tr>';
				 $("#COPY"+WechatTotalId).find('#tbody_invoice').append(trn);
			}else{
				$("#COPY"+WechatTotalId).find('#tbody_invoice').empty().html('<tr></tr>');
			}
			//分页初始化
			showPading(jsonObject.data.invoiceDataToPage,"COPY"+WechatTotalId+" #invoice_footPage",queryMBAndInvoice);
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
				 $("#COPY"+WechatTotalId).find('#tbody_banklog').empty().html(tr);
				 var trs = '<tr>'
			            +'<td colspan="1">小计：'+counts+'</td>'
			            +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
		                +'<td colspan="4"></td>'
		         +'</tr>';
				 $("#COPY"+WechatTotalId).find('#tbody_banklog').append(trs);
				 var trn = '<tr>'
    	            +'<td colspan="1">总计：'+jsonObject.data.bankLogDataToPage.totalElements+'</td>'
    	            +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.bankLogTotal[0]+'</td>'
    	            +'<td colspan="4"></td>'
		          +'</tr>';
				 $("#COPY"+WechatTotalId).find('#tbody_banklog').append(trn);
			}else{
				$("#COPY"+WechatTotalId).find('#tbody_banklog').empty().html('<tr></tr>');
			}
			 $("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.bankLogDataToPage,"COPY"+WechatTotalId+" #banklog_footPage",queryMBAndInvoice);
		}
	});
}


var WechatTotalIds=new Array()
function rememberIds(id){
	if(WechatTotalIds.indexOf(id)<0){
		WechatTotalIds.push(id);
	}
}


var WechatTotalMatching_show=function(WechatTotalId){
	$("#i"+WechatTotalId).removeClass("fa-angle-double-down").addClass("fa-angle-double-up");
	var $tr=$("#WechatTotalTr"+WechatTotalId);
	//事件切换
	$tr.attr('onclick', '').unbind("click").click(function(){
		WechatTotalMatching_hide(WechatTotalId);
	});
	//拼接DIV
	var $td=$("<td colspan='8' style='padding:10px !important;'></td>").append($(".table-detail").clone().attr("id", "COPY" + WechatTotalId).removeClass("table-detail").removeClass("hide"));
	$tr.after($("<tr class='WechatTotal_matchingTr' id='WechatTotal_matching"+WechatTotalId+"' ></tr>").append($td));
	queryMBAndInvoice(WechatTotalId);
}

var WechatTotalMatching_hide=function(WechatTotalId){
	if(WechatTotalIds.indexOf(WechatTotalId)>=0){
		WechatTotalIds.splice(WechatTotalIds.indexOf(WechatTotalId),1);
	}
	$("#i"+WechatTotalId).removeClass("fa-angle-double-up").addClass("fa-angle-double-down");
	var $tr=$("#WechatTotalTr"+WechatTotalId);
	//事件切换
	$tr.attr('onclick', '').unbind("click").click(function(){
		WechatTotalMatching_show(WechatTotalId);
	});
	$("#WechatTotal_matching"+WechatTotalId).remove();
}

//查询已经匹配的单
var queryMatched=function(){
	//当前页码
	var CurPage=$("#WechatTotalMatched_footPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#handicap_WechatTotalMatched").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='timeScope_WechatTotalMatched']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//会员号
	var member=$("#member_WechatTotalMatched").val(); 
	//订单号
	var orderNo=$("#orderNo_WechatTotalMatched").val();
	//开始金额
	var fromAmount=$("#fromMoney_WechatTotalMatched").val();
	//结束金额
	var toAmount=$("#toMoney_WechatTotalMatched").val();payer_WechatTotalMatched
	//存款人
	var payer=$("#payer_WechatTotalMatched").val();
	//微信号
	var WechatTotalNumber=$("#payer_WechatTotalNo_WechatTotalMatched").val();
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditWechatTotalIn/findWechatTotalMatched",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"member":member,
			"orderNo":orderNo,
			"fromAmount":fromAmount,
			"toAmount":toAmount,
			"payer":payer,
			"WechatTotalNumber":WechatTotalNumber,
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
					 idList.push({'id':val.wechatid});
                     tr += '<tr>'
                	      +'<td>'+val.handicapName+'</td>'
                	      +'<td>'+val.memberName+'</td>'
                	      +'<td>'+val.orderNo+'</td>'
                	      +'<td>'+val.amount+'</td>'
                	      +'<td>'+val.depositor+'</td>'
                	      +'<td>' 
                	      	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.wechatid+"' data-placement='left' data-trigger='hover'  >"+hideAccountAll(val.account) 
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
				 $('#tbody_WechatTotalMatched').empty().html(tr);
				 var trs = '<tr>'
			            +'<td colspan="3">小计：'+counts+'</td>'
			            +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
		                +'<td colspan="6"></td>'
		         +'</tr>';
				 $('#tbody_WechatTotalMatched').append(trs);
				 var trn = '<tr>'
     	            +'<td colspan="3">总计：'+jsonObject.data.page.totalElements+'</td>'
     	            +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.total[0]+'</td>'
     	            +'<td colspan="6"></td>'
		          +'</tr>';
				 $('#tbody_WechatTotalMatched').append(trn);
			}else{
				$('#tbody_WechatTotalMatched').empty().html('<tr></tr>');
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			 $("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.page,"WechatTotalMatched_footPage",queryMatched);
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
	var toAmount=$("#toMoney_wetchatCanceled").val();payer_WechatTotalMatched
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditWechatTotalIn/findWechatTotalCanceled",
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
					 idList.push({'id':val.wechatid});
                     tr += '<tr>'
                	      +'<td>'+val.handicapName+'</td>'
                	      +'<td>' 
	              	      	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.wechatid+"' data-placement='right' data-trigger='hover'  >"+hideAccountAll(val.account) 
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
	//微信号
	var WechatTotalNo=$("#payer_WechatTotalNo_wetchatUnClaim").val();
	//开始金额
	var fromAmount=$("#fromMoney_wetchatUnClaim").val();
	//结束金额
	var toAmount=$("#toMoney_wetchatUnClaim").val();payer_WechatTotalMatched
	$.ajax({
		type:"post",
		url:"/r/IncomeAuditWechatTotalIn/findWechatTotalUnClaim",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"member":member,
			"WechatTotalNo":WechatTotalNo,
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
    $('#autoUpdateTimeWechatTotalIn').empty().append(opt);
}
$('#autoUpdateTimeWechatTotalIn').unbind().bind('change', function () {
	_searchIncomeAuditWechatTotalIn();
});

jQuery(function($) {
	inithancipad("handicap_WechatTotal_total");
	inithancipad("handicap_WechatTotalMatched");
	inithancipad("handicap_wetchatCanceled");
	inithancipad("handicap_wetchatUnClaim");
	$('#freshWechatTotalInLi').show();
	_addTimeSelect();
	_searchIncomeAuditWechatTotalIn();
	initTime();
	queryMatchingWechatTotal();
	queryMatched();
	queryCanceled();
	queryUnClaim();
})