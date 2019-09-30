currentPageLocation = window.location.href;
var searchTypeTaskTotal = null;//outDrawing toOutDraw  masterOut  failedOut successOut  refused  canceled
var searchtype = null, searchid = null, searchoutwardRequest = null;
//分配
var Distributionflag = false, TurningPlatformflag = false, Freezeflag = false,Noteflag = false, SendMessageflag = false,ToFailureflag = false;
//从新生成任务
var NewGenerationTaskflag = false,FinishTask = false, NotifyP = false, UploadReceipt = false;
//完成出款 查询排序初始化
var sortFlagForSuccessOut = null, finishedOutWardSubPage = false;
var outDrawingTab =false,toOutDrawTab=false,masterOutTab=false,failedOutTab=false,failedOutDealedTab=false,
    refusedTab =false,canceledTab=false,backwashTab=false,QuickQueryTab=false,lookUpFinishedTotalAmount = false;
var downloadUrl = window.location.origin + '/';
var liArray = [];
$.each(ContentRight['OutwardTaskTotal:*'], function (name, value) {
    if (name == 'OutwardTaskTotal:finishedOutWardSubPage:*') {
        finishedOutWardSubPage = true;
    }
    if (name == 'OutwardTaskTotal:ReversedOutTab:*') {
        backwashTab = true;
    }
    if (name == 'OutwardTaskTotal:QuickQueryTab:*') {
        QuickQueryTab = true;
    }
    if (name == 'OutwardTaskTotal:CanceledOutTab:*') {
        canceledTab = true;
    }
    if (name == 'OutwardTaskTotal:RefusedOutTab:*') {
        refusedTab = true;
    }
    if (name == 'OutwardTaskTotal:CheckedSummaryTab:*') {
        failedOutDealedTab = true;
    }
    if (name == 'OutwardTaskTotal:ToCheckOutTab:*') {
        failedOutTab = true;
    }
    if (name == 'OutwardTaskTotal:MasterDealOutTab:*') {
        masterOutTab = true;
    }
    if (name == 'OutwardTaskTotal:ToDrawOutTab:*') {
        toOutDrawTab = true;
    }
    if (name == 'OutwardTaskTotal:DrawingOutTab:*') {
        outDrawingTab = true;
    }

    if (backwashTab) {
        $('#backwashTab').prop('style','');
    } else {
        $('#backwashTab').prop('style','display:none;');
    }
    if (backwashTab) {
        $('#backwashTab').prop('style','');
    } else {
        $('#backwashTab').prop('style','display:none;');
    }
    if (QuickQueryTab) {
        $('#QuickQueryTab').prop('style','');
    } else {
        $('#QuickQueryTab').prop('style','display:none;');
    }
    if (canceledTab) {
        $('#canceledTab').prop('style','');
    } else {
        $('#canceledTab').prop('style','display:none;');
    }
    if (refusedTab) {
        $('#refusedTab').prop('style','');
    } else {
        $('#refusedTab').prop('style','display:none;');
    }
    if (failedOutDealedTab) {
        $('#failedOutDealedTab').show();
    } else {
        $('#failedOutDealedTab').prop('style','display:none;');
    }
    if (failedOutTab) {
        $('#failedOutTab').prop('style','');
    } else {
        $('#failedOutTab').prop('style','display:none;');
    }
    if (masterOutTab) {
        $('#masterOutTab').prop('style','');
    } else {
        $('#masterOutTab').prop('style','display:none;');
    }
    if (outDrawingTab) {
        $('#outDrawingTab').prop('style','');
    } else {
        $('#outDrawingTab').prop('style','display:none;');
    }
    if (toOutDrawTab) {
        $('#toOutDrawTab').prop('style','');
    } else {
        $('#toOutDrawTab').prop('style','display:none;');
    }
    if (finishedOutWardSubPage) {
        $('#successOutTab').prop('style','');
    } else {
        $('#successOutTab').prop('style','display:none;');
    }
});

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
jQuery(function($) {
	getHandicap_select($("#RebateDrawingHandicap"),null,"全部");
	getHandicap_select($("#rebateHandicap"),null,"全部");
	getHandicap_select($("#checkHandicap"),null,"全部");
	getHandicap_select($("#canceledHandicap"),null,"全部");
	getHandicap_select($("#successHandicap"),null,"全部");
	initTimePicker(true,$("[name=successTime]"),typeCustomLatestToday);
	initTimePicker(true,$("[name=canceledTime]"),typeCustomLatestToday);
	genBankType("bankType");
	//_searchRebateTimeTask('RebateDrawing');
})


var downloadUrl = window.location.origin + '/';
function clearNoNum(obj) {
    //先把非数字的都替换掉，除了数字和.
    obj.value = obj.value.replace(/[^\d.]/g, "");
    //必须保证第一个为数字而不是.
    obj.value = obj.value.replace(/^\./g, "");
    //保证只有出现一个.而没有多个.
    obj.value = obj.value.replace(/\.{2,}/g, ".");
    //保证.只出现一次，而不能出现两次以上
    obj.value = obj.value.replace(".", "$#$").replace(/\./g, "").replace("$#$", ".");
    obj.value = obj.value.replace(/^(\-)*(\d+)\.(\d\d).*$/, '$1$2.$3');//只能输入两个小数
    if (obj.value.indexOf(".") < 0 && obj.value != "") {//以上已经过滤，此处控制的是如果没有小数点，首位不能为类似于 01、02的金额
        obj.value = parseFloat(obj.value);
    }
}

function secondToDate(result) {
    var h = Math.floor(result / 3600) < 10 ? '0'+Math.floor(result / 3600) : Math.floor(result / 3600);
    var m = Math.floor((result / 60 % 60)) < 10 ? '0' + Math.floor((result / 60 % 60)) : Math.floor((result / 60 % 60));
    var s = Math.floor((result % 60)) < 10 ? '0' + Math.floor((result % 60)) : Math.floor((result % 60));
    return result = h + ":" + m + ":" + s;
}

function resetValues(type){
	if(type=='RebateDrawing'){
		$("#RebateDrawingHandicap ").val('');
		$('#RebateDrawingOrderNo').val('');
		$('#RebateDrawingFromMoney').val('');
		$('#RebateDrawingToMoney').val('');
	}else if(type=='Rebate'){
		$("#rebateHandicap ").val('');
		$('#rebateOrderNo').val('');
		$('#rebateFromMoney').val('');
		$('#rebateToMoney').val('');
	}else if(type=='Check'){
		$("#checkHandicap ").val('');
		$('#checkOrderNo').val('');
		$('#checkFromMoney').val('');
		$('#checkToMoney').val('');
	}else if(type=='Canceled'){
		$("#canceledHandicap ").val('');
		$('#canceledOrderNo').val('');
		$('#canceledFromMoney').val('');
		$('#canceledToMoney').val('');
	}else if(type=='Success'){
		$("#successHandicap ").val('');
		$('#successOrderNo').val('');
		$('#successFromMoney').val('');
		$('#successToMoney').val('');
	}
}

//取消
var cancelButton=false;
//重新生成任务
var newTaskButton=false;
//完成
var finishedButton=false;
//转待排查
var checkButton=false;
$.each(ContentRight['OutwardTaskTotal:*'], function (name, value) {
    if (name == 'OutwardTaskTotal:TurningPlatform:*') {
    	cancelButton = true;
    }
    if (name == 'OutwardTaskTotal:NewGenerationTask:*') {
    	newTaskButton = true;
    }
    if (name == 'OutwardTaskTotal:FinishTask:*') {
    	finishedButton = true;
    }
    if (name == 'OutwardTaskTotal:ToFailure:*') {
    	checkButton = true;
    }
});

function addRebateRemark(id,type){
	if(type==3){
		$('#typeName').empty().html("取消");
	}else if(type==6){
		$('#typeName').empty().html("转待排查");
	}else if(type==1){
		$('#typeName').empty().html("完成");
	}else if(type==0){
		$('#typeName').empty().html("重新生成任务");
	}else if(type==8){
		$('#typeName').empty().html("分配");
	}else{
		$('#typeName').empty().html("备注");
	}
	$("#Remark").val("");
	$('#Remark_modal').modal('show');
	$('#rebateTotalTaskFinishBTN').attr('onclick', 'save_Remark('+id+','+type+');');
}

function save_Remark(id,status){
	var remark=$.trim($("#Remark").val());
	if(remark==""){
		  $('#prompt_remarkk').show(10).delay(1500).hide(10);
		  return;
	  }
	$.ajax({
		async:true,
		type:'post',
        url:'/r/rebate/saveRemarkAndUpdataStatus',
        data:{'id':id,'remark':remark,'status':status},
        dataType:'json',
        success:function (res) {
        	if(res.status == 1){
        		$('#Remark_modal').modal('hide');
        		queryRebateDrawing();
        		queryRebate();
        		queryCheck();
        		querySuccess();
        		queryCanceled();
            	$("#Remark").val("");
        	}
        }
    });
}

var showContract = function(mobile){
    $.ajax({type: 'get',url: '/r/account/getUserByMobile',data: {"mobile":mobile},dataType: 'json',success: function (jsonObject) {
        if (jsonObject.status==1) {
            var data = jsonObject.data;
            showMessageForSuccess("联系人：" + data.contactor + " 联系方式：" + data.contactText, 3000);
        }else{
            showMessageForFail(jsonObject.message);
        }
    }});
};


//-----下载图片-----
//正在出款
var queryRebateDrawing=function(){
	if (currentPageLocation.indexOf('OutwardTaskTotal:*') == -1){
		timeOutSearchRebate=null;
     	clearInterval(timeOutSearchRebate);
     	return;
	 }
	//当前页码
	var CurPage=$("#rebateDrawingPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//订单号
	var orderNo=$("#RebateDrawingOrderNo").val();
	//金额
	var fromAmount=$("#RebateDrawingFromMoney").val();
	var toMoney=$("#RebateDrawingToMoney").val();
	if(fromAmount*1>toMoney*1 && toMoney*1>0){
		showMessageForFail("金额有误！");
	    return
	}
	
	//获取盘口
	var handicap=$("#RebateDrawingHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	var rebateType=$("#rebateDrawingType").val();
	$.ajax({
		type:"post",
		url:"/r/rebate/findRebate",
		data:{
			"pageNo":CurPage,
			"status":0,
			"type":"rebate",
			"orderNo":orderNo,
			"rebateType":rebateType,
			"fromAmount":fromAmount,
			"toMoney":toMoney,
			"startAndEndTime":"",
			"handicap":handicap,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
				var tr = '';
				 //小计
				 var counts = 0;
				 var amounts=0;
				 var idList=new Array();
				 for(var index in jsonObject.data.rebatelist){
					 var val = jsonObject.data.rebatelist[index];
					 idList.push({'id':val.accountId});
                    tr += '<tr>'
                    			+'<td>' + val.handicapName + '</td>'
	                        	+'<td>' + val.toHolder + '</td>'
	                        	+'<td>' + val.toAccountType +"</br>"+val.toAccountInfo+ '</td>'
	                        	//+'<td>'+ val.toAccountInfo +'</td>'
	                        	+'<td>' + val.toAccount + '</td>'
	                        	+'<td>' + val.tid + '</td>'
	                        	+'<td>' + (val.type==1?"返利":"降额") + '</td>'
	                        	+'<td>' + val.outPerson + '</td>'
	                        	+"<td>"  
	                      	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.outAccount)
	                      	      +"</a>"
	                      	    +"</td>"
	                        	+'<td>' + val.amount + '</td>'
	                        	+'<td>' + val.createTimeStr + '</td>'
	                        	+'<td>' + val.asignTimeStr + '</td>';
	                        	if((val.differenceMinutes/60)>10)
	                        		tr+='<td style="color: white;background-color: indianred">' +  secondToDate(val.differenceMinutes) + '</td>';
	                        	else if((val.differenceMinutes/60)>3)
	                        		tr+='<td style="color: white;background-color: limegreen">' +  secondToDate(val.differenceMinutes) + '</td>';
	                        	else
	                        		tr+='<td>' +  secondToDate(val.differenceMinutes) + '</td>';
	                        	tr+='<td>'
		                   	      + '<a class="bind_hover_card breakByWord"  title="备注"'
		                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
		                            + ' data-content="' + val.remark + '">'
		                             + val.remark.substring(0,10)
		                         + '</a>'
	                            +'</td>'
	                        	+'<td>'
	                        		+'<button onclick="addRebateRemark('+val.id+',8);" type="button" class=" btn btn-xs btn-white btn-warning  btn-bold"><i class="ace-icon fa fa-reply  bigger-100 red"></i>分配</button>'
	                        		+'<button type="button" onclick="addRebateRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
	                        	+'</td>'
                    	 +'</tr>';
                    counts +=1;
                    amounts+=val.amount;
                };
				 $('#rebateDrawing_tbody').empty().html(tr);
				 var trs = '<tr>'
								 +'<td colspan="8">小计：'+counts+'</td>'
								 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts.toFixed(2)+'</td>'
							     +'<td colspan="5"></td>'
						  +'</tr>';
                $('#rebateDrawing_tbody').append(trs);
                var trn = '<tr>'
			                	+'<td colspan="8">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
			                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+jsonObject.data.rebateTotal+'</td>'
							    +'<td colspan="5"></td>'
				         +'</tr>';
                $('#rebateDrawing_tbody').append(trn);
				}else {
	                $('#rebateDrawing_tbody').empty();
	            }
			//加载账号悬浮提示
			$("[data-toggle='popover']").popover();
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.rebatePage,"rebateDrawingPage",queryRebateDrawing);
		}
	});	
}
//未出款 
var queryRebate=function(){
	if (currentPageLocation.indexOf('OutwardTaskTotal:*') == -1){
		timeOutSearchRebate=null;
		 clearInterval(timeOutSearchRebate);
     	return;
	 }
//当前页码
var CurPage=$("#rebateTabPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
//订单号
var orderNo=$("#rebateOrderNo").val();
//金额
var fromAmount=$("#rebateFromMoney").val();
var toMoney=$("#rebateToMoney").val();
if(fromAmount*1>toMoney*1 && toMoney*1>0){
	showMessageForFail("金额有误！");
    return
}
//获取盘口
var handicap=$("#rebateHandicap").val();
if(handicap=="" || handicap==null){
	handicap=0;
}
var rebateType=$("#rebateType").val();
$.ajax({
	type:"post",
	url:"/r/rebate/findRebate",
	data:{
		"pageNo":CurPage,
		"status":0,
		"type":"rebated",
		"orderNo":orderNo,
		"rebateType":rebateType,
		"fromAmount":fromAmount,
		"toMoney":toMoney,
		"startAndEndTime":"",
		"handicap":handicap,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var amounts=0;
			 var idList=new Array();
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 idList.push({'id':val.accountId});
                tr += '<tr>'
                			+'<td>' + val.handicapName + '</td>'
                        	+'<td>' + val.toHolder + '</td>'
                        	+'<td>' + val.toAccountType +"</br>"+val.toAccountInfo+ '</td>'
                        	//+'<td>'+ val.toAccountInfo +'</td>'
                        	+'<td>' + val.toAccount + '</td>'
                        	+'<td>' + val.tid + '</td>'
                        	+'<td>' + (val.type==1?"返利":"降额") + '</td>'
                        	+'<td>' + val.amount + '</td>'
                        	+'<td>' + val.createTimeStr + '</td>'
                        	+'<td>'
	                   	      + '<a class="bind_hover_card breakByWord"  title="备注"'
	                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
	                            + ' data-content="' + val.remark + '">'
	                             + val.remark.substring(0,10)
	                         + '</a>'
                          +'</td>'
                        	+'<td>'
                        	    +'<button onclick="addRebateRemark('+val.id+',8);" type="button" class=" btn btn-xs btn-white btn-warning  btn-bold"><i class="ace-icon fa fa-reply  bigger-100 red"></i>分配</button>'
                        		+'<button type="button" onclick="addRebateRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
                        	+'</td>'
                	 +'</tr>';
                counts +=1;
                amounts+=val.amount;
            };
			 $('#rebateTab_tbody').empty().html(tr);
			 var trs = '<tr>'
							 +'<td colspan="6">小计：'+counts+'</td>'
							 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts.toFixed(2)+'</td>'
						     +'<td colspan="3"></td>'
					  +'</tr>';
            $('#rebateTab_tbody').append(trs);
            var trn = '<tr>'
		                	+'<td colspan="6">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+jsonObject.data.rebateTotal+'</td>'
						    +'<td colspan="3"></td>'
			         +'</tr>';
            $('#rebateTab_tbody').append(trn);
			}else {
                $('#rebateTab_tbody').empty();
            }
		//加载账号悬浮提示
		loadHover_accountInfoHover(idList);
		$("[data-toggle='popover']").popover();
		//分页初始化
		showPading(jsonObject.data.rebatePage,"rebateTabPage",queryRebate);
	}
});
}

//待排查
var queryCheck=function(){
	if (currentPageLocation.indexOf('OutwardTaskTotal:*') == -1){
		timeOutSearchCheck=null;
		clearInterval(timeOutSearchCheck);
     	return;
	 }
//当前页码
var CurPage=$("#checkTabPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
//订单号
var orderNo=$("#checkOrderNo").val();
//金额
var fromAmount=$("#checkFromMoney").val();
var toMoney=$("#checkToMoney").val();
if(fromAmount*1>toMoney*1 && toMoney*1>0){
	showMessageForFail("金额有误！");
    return
}
//获取盘口
var handicap=$("#checkHandicap").val();
if(handicap=="" || handicap==null){
	handicap=0;
}
var rebateType=$("#checkType").val();
$.ajax({
	type:"post",
	url:"/r/rebate/findRebate",
	data:{
		"pageNo":CurPage,
		"status":6,
		"type":"check",
		"orderNo":orderNo,
		"fromAmount":fromAmount,
		"rebateType":rebateType,
		"toMoney":toMoney,
		"startAndEndTime":"",
		"handicap":handicap,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var amounts=0;
			 var idList=new Array();
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 idList.push({'id':val.accountId});
				 tr += '<tr>'
					 +'<td>' + val.handicapName + '</td>'
                 	+'<td>' + val.toHolder + '</td>'
                 	+'<td>' + val.toAccountType +"</br>"+val.toAccountInfo+ '</td>'
                 	//+'<td>'+ val.toAccountInfo +'</td>'
                 	+'<td>' + val.toAccount + '</td>'
                 	+'<td>' + val.tid + '</td>'
                 	+'<td>' + (val.type==1?"返利":"降额") + '</td>'
                 	+'<td>' + val.outPerson + '</td>'
                 	+"<td>"  
               	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.outAccount)
               	      +"</a>"
               	    +"</td>"
               	    +'<td>' + val.accountAlias + '</td>'
                 	+'<td>' + val.amount + '</td>'
                 	+'<td>' + val.createTimeStr + '</td>'
                 	+'<td>'
             	      + '<a class="bind_hover_card breakByWord"  title="备注"'
                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                      + ' data-content="' + val.remark + '">'
                       + val.remark.substring(0,10)
                   + '</a>'
                  +'</td>';
                  if (!isHideImg&&val.screenshot.length>0){
                	  tr += '<td>' +
                      '<a href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\''  + val.screenshot + '\')">查看</a>' +
                      '</td>';
                  }else{
                	  tr += '<td></td>';
                  }
                 	tr+='<td>';
				      if(cancelButton)
				    	  tr +='<button type="button" onclick="addRebateRemark('+val.id+',3)" class="btn btn-xs btn-white btn-danger btn-bold"><i class="ace-icon fa fa-remove  bigger-100 red"></i>取消</button>';
				      if(newTaskButton)
				    	  tr +='<button onclick="addRebateRemark('+val.id+',0);" type="button" class=" btn btn-xs btn-white btn-warning  btn-bold"><i class="ace-icon fa fa-reply  bigger-100 red"></i>重新生成任务</button>';
				    tr +='<button type="button" onclick="addRebateRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                 	  if(finishedButton)	
                 		  tr+='<button type="button" onclick="addRebateRemark('+val.id+',1)" class="btn btn-xs btn-white btn-success btn-bold "><i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                 	tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showContract('+val.uid+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>联系方式</span></button>'
                 	  +'<a onclick="showInOutListModal('+val.accountId+')" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
                 	  +'</td>'
         	 +'</tr>';
                counts +=1;
                amounts+=val.amount;
            };
			 $('#checkTab_tbody').empty().html(tr);
			 var trs = '<tr>'
							 +'<td colspan="9">小计：'+counts+'</td>'
							 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts.toFixed(2)+'</td>'
						     +'<td colspan="4"></td>'
					  +'</tr>';
            $('#checkTab_tbody').append(trs);
            var trn = '<tr>'
		                	+'<td colspan="9">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+jsonObject.data.rebateTotal+'</td>'
						    +'<td colspan="4"></td>'
			         +'</tr>';
            $('#checkTab_tbody').append(trn);
			}else {
                $('#checkTab_tbody').empty();
            }
		//加载账号悬浮提示
		loadHover_accountInfoHover(idList);
		$("[data-toggle='popover']").popover();
		//分页初始化
		showPading(jsonObject.data.rebatePage,"checkTabPage",queryCheck);
	}
});
}


//完成出款
var querySuccess=function(){
	//当前页码
var CurPage=$("#successTabPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
//订单号
var orderNo=$("#successOrderNo").val();
//金额
var fromAmount=$("#successFromMoney").val();
var toMoney=$("#successToMoney").val();
if(fromAmount*1>toMoney*1 && toMoney*1>0){
	showMessageForFail("金额有误！");
    return
}
var startAndEndTime = $("input[name='successTime']").val();
var startAndEndTimeToArray = new Array();
if(startAndEndTime){
	var startAndEnd = startAndEndTime.split(" - ");
	startAndEndTimeToArray.push($.trim(startAndEnd[0]));
	startAndEndTimeToArray.push($.trim(startAndEnd[1]));
}
startAndEndTimeToArray = startAndEndTimeToArray.toString();

var handicap=$("#successHandicap").val();
if(handicap=="" || handicap==null){
	handicap=0;
}

var uName=$("#uName").val();
var rebateType=$("#successType").val();
$.ajax({
	type:"post",
	url:"/r/rebate/findRebate",
	data:{
		"pageNo":CurPage,
		"status":1,
		"type":"check",
		"orderNo":orderNo,
		"rebateType":rebateType,
		"fromAmount":fromAmount,
		"toMoney":toMoney,
		"startAndEndTime":startAndEndTimeToArray,
		"handicap":handicap,
		"uName":uName,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var amounts=0;
			 var idList=new Array();
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 idList.push({'id':val.accountId});
				 tr += '<tr>'
					 +'<td>' + val.uName + '</td>'
					 +'<td>' + val.handicapName + '</td>'
                 	+'<td>' + val.toHolder + '</td>'
                 	+'<td>' + val.toAccountType +"</br>"+val.toAccountInfo+ '</td>'
                 	//+'<td>'+ val.toAccountInfo +'</td>'
                 	+'<td>' + val.toAccount + '</td>'
                 	+'<td>' + val.tid + '</td>'
                 	+'<td>' + (val.type==1?"返利":"降额") + '</td>'
                 	+'<td>' + val.outPerson + '</td>'
                 	+"<td>"  
               	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.outAccount)
               	      +"</a>"
               	    +"</td>"
                 	+'<td>' + val.amount + '</td>'
                 	+'<td>' + (val.status==1?"已完成":"已匹配") + '</td>'
                 	+'<td>' + val.createTimeStr + '</td>'
                 	+'<td>' + val.updateTimeStr + '</td>'
                 	+'<td>'
             	      + '<a class="bind_hover_card breakByWord"  title="备注"'
                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                      + ' data-content="' + val.remark + '">'
                       + val.remark.substring(0,10)
                   + '</a>'
                  +'</td>';
                  if (!isHideImg&&val.screenshot.length>0){
                	  tr += '<td>' +
                      '<a href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\''  + val.screenshot + '\')">查看</a>' +
                      '</td>';
                  }else{
                	  tr += '<td></td>';
                  }
				 tr +='<td>';
                 	if(checkButton)	
                 		tr+='<button onclick="addRebateRemark('+val.id+',6)" type="button" class=" btn btn-xs btn-white btn-danger btn-bold"><i class="ace-icon fa fa-share  bigger-100 red"></i>转待排查</button>';
                 	tr+='<button type="button" onclick="addRebateRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
                 	+'</td>'
         	 +'</tr>';
                counts +=1;
                amounts+=val.amount;
            };
			 $('#successTab_tbody').empty().html(tr);
			 var trs = '<tr>'
							 +'<td colspan="9">小计：'+counts+'</td>'
							 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts.toFixed(2)+'</td>'
						     +'<td colspan="6"></td>'
					  +'</tr>';
            $('#successTab_tbody').append(trs);
            var trn = '<tr>'
		                	+'<td colspan="9">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+jsonObject.data.rebateTotal+'</td>'
						    +'<td colspan="6"></td>'
			         +'</tr>';
            $('#successTab_tbody').append(trn);
			}else {
                $('#successTab_tbody').empty();
            }
		//加载账号悬浮提示
		loadHover_accountInfoHover(idList);
		$("[data-toggle='popover']").popover();
		//分页初始化
		showPading(jsonObject.data.rebatePage,"successTabPage",querySuccess);
	}
});
}

//已取消
var queryCanceled=function(){
	//当前页码
var CurPage=$("#canceledTabPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
//订单号
var orderNo=$("#canceledOrderNo").val();
//金额
var fromAmount=$("#canceledFromMoney").val();
var toMoney=$("#canceledToMoney").val();
if(fromAmount*1>toMoney*1 && toMoney*1>0){
	showMessageForFail("金额有误！");
    return
}
var startAndEndTime = $("input[name='canceledTime']").val();
var startAndEndTimeToArray = new Array();
if(startAndEndTime){
	var startAndEnd = startAndEndTime.split(" - ");
	startAndEndTimeToArray.push($.trim(startAndEnd[0]));
	startAndEndTimeToArray.push($.trim(startAndEnd[1]));
}
startAndEndTimeToArray = startAndEndTimeToArray.toString();
var handicap=$("#canceledHandicap").val();
if(handicap=="" || handicap==null){
	handicap=0;
}
var rebateType=$("#canceledTypee").val();
$.ajax({
	type:"post",
	url:"/r/rebate/findRebate",
	data:{
		"pageNo":CurPage,
		"status":3,
		"type":"canceled",
		"orderNo":orderNo,
		"rebateType":rebateType,
		"fromAmount":fromAmount,
		"toMoney":toMoney,
		"startAndEndTime":startAndEndTimeToArray,
		"handicap":handicap,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var amounts=0;
			 var idList=new Array();
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 idList.push({'id':val.accountId});
				 tr += '<tr>'
					 +'<td>' + val.handicapName + '</td>'
                 	+'<td>' + val.toHolder + '</td>'
                 	+'<td>' + val.toAccountType +"</br>"+val.toAccountInfo+ '</td>'
                 	//+'<td>'+ val.toAccountInfo +'</td>'
                 	+'<td>' + val.toAccount + '</td>'
                 	+'<td>' + val.tid + '</td>'
                 	+'<td>' + (val.type==1?"返利":"降额") + '</td>'
                 	+'<td>' + val.outPerson + '</td>'
                 	+"<td>"  
               	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.outAccount)
               	      +"</a>"
               	    +"</td>"
                 	+'<td>' + val.amount + '</td>'
                 	+'<td>' + val.createTimeStr + '</td>'
                 	+'<td>' + val.updateTimeStr + '</td>'
                 	+'<td>'
             	      + '<a class="bind_hover_card breakByWord"  title="备注"'
                      + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                      + ' data-content="' + val.remark + '">'
                       + val.remark.substring(0,10)
                   + '</a>'
                  +'</td>';
                  if (!isHideImg&&val.screenshot.length>0){
                	  tr += '<td>' +
                      '<a href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\''  + val.screenshot + '\')">查看</a>' +
                      '</td>';
                  }else{
                	  tr += '<td></td>';
                  }
                  tr +='<td>'
                 	+'<button type="button" onclick="addRebateRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
                 	+'</td>'
         	 +'</tr>';
                counts +=1;
                amounts+=val.amount;
            };
			 $('#canceledTab_tbody').empty().html(tr);
			 var trs = '<tr>'
							 +'<td colspan="8">小计：'+counts+'</td>'
							 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts.toFixed(2)+'</td>'
						     +'<td colspan="6"></td>'
					  +'</tr>';
            $('#canceledTab_tbody').append(trs);
            var trn = '<tr>'
		                	+'<td colspan="8">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+jsonObject.data.rebateTotal+'</td>'
						    +'<td colspan="6"></td>'
			         +'</tr>';
            $('#canceledTab_tbody').append(trn);
			}else {
                $('#canceledTab_tbody').empty();
            }
		//加载账号悬浮提示
		loadHover_accountInfoHover(idList);
		$("[data-toggle='popover']").popover();
		//分页初始化
		showPading(jsonObject.data.rebatePage,"canceledTabPage",queryCanceled);
	}
});}


var genBankType = function(bankTypeId){
    var ret ='<option value="">请选择</option>';
    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true});
    $('#'+bankTypeId+'_chosen').prop('style', 'width: 120px;')
};


function _permitCheck(){
    $('#ulTable li').each(function(i){
        if($(this).attr('style')!='display: none;'){
            liArray.push(this.id);
        }
    });
    var divId =null;
    if(liArray.length>0){
        $('#'+liArray[0]).prop('class','active');
        divId = liArray[0].split('Tab')[0];
        $('#'+divId).prop('class','tab-pane in active');
        if(divId== 'outDrawing'|| divId=='toOutDraw'||divId=='masterOut'||divId=='failedOut'){
            $('#freshOutwardTaskLi').show();
        }else{
            $('#freshOutwardTaskLi').hide();
        }

    }else{
        $('#tabOutTaskTotal').html('<div class="row center " style=" font-size: larger;color: red ">请分配页签权限</div>');
    }
    if(!divId){
        return;
    }
    _getPathAndHtml('historyDetail');
    _dateP();_checkAccountToDrawInSession();_initialMultiSelect();
    _initiaiHandicaps(divId); _addTimeSelect();
    _initialSort();
    modifyNouns();
    modifyStyle();
    initialSearchTypeToal(divId);
}
$.each(ContentRight['OutwardTaskTotal:*'], function (name, value) {
    if (name == 'OutwardTaskTotal:Distribution:*') {
        Distributionflag = true;
    } else if (name == 'OutwardTaskTotal:TurningPlatform:*') {
        TurningPlatformflag = true;
    } else if (name == 'OutwardTaskTotal:Freeze:*') {
        Freezeflag = true;
    } else if (name == 'OutwardTaskTotal:Note:*') {
        Noteflag = true;
    } else if (name == 'OutwardTaskTotal:SendMessage:*') {
        SendMessageflag = true;
    } else if (name == 'OutwardTaskTotal:ToFailure:*') {
        ToFailureflag = true;
    } else if (name == 'OutwardTaskTotal:NewGenerationTask:*') {
        NewGenerationTaskflag = true;
    } else if (name == 'OutwardTaskTotal:FinishTask:*') {
        FinishTask = true;
    } else if (name == 'OutwardTaskTotal:NotifyPlatForm:*') {
        NotifyP = true;
    } else if (name == 'OutwardTaskTotal:uploadReceipt:*') {
        UploadReceipt = true;
    }
});
$.each(ContentRight['Outward:*'], function (name, value) {
    if (name == 'Outward:currentpageSum:*') {
        outwardCurrentPageSum=true;
    }
    if (name =='Outward:allRecordSum:*'){
        outwardAllRecordSum=true;
    }
});
function _addTimeSelect() {
    var opt = '<option  value="0" >不刷新</option><option  selected="selected" value="15">15秒</option><option  value="30">30秒</option>' +
        '<option  value="60">60秒</option><option  value="120">120秒</option><option  value="180">180秒</option>';
    $('#autoUpdateTimeTask').empty().append(opt);
}
$('#autoUpdateTimeTask').unbind().bind('change', function () {
    _searchAfterAutoUpdateTimeTask();
});
/**定义查询页签*/
function initialSearchTypeToal(type) {
    searchTypeTaskTotal = type;
    _initiaiHandicaps(searchTypeTaskTotal);
    if (searchTypeTaskTotal != 'QuickQuery' && searchTypeTaskTotal != "backwash") {
        initPaging($('#' + searchTypeTaskTotal + '_footPage'), pageInitial);
        //successOut  refused  canceled
        if (searchTypeTaskTotal == 'failedOutDealed' || searchTypeTaskTotal == 'successOut' || searchTypeTaskTotal == 'refused' || searchTypeTaskTotal == 'canceled') {
            if (timeOutSearchTasks) {
                clearInterval(timeOutSearchTasks);
                timeOutSearchTasks = null;
            }
            _dateP();
            _search();
        } else {
            _addTimeSelect();
            _searchAfterAutoUpdateTimeTask();
        }

        if (searchTypeTaskTotal == 'failedOut') {
            _dateP();
            _initiaiHandicaps(searchTypeTaskTotal);$('#timeScope_failedOut').val('');
            _search();
        } else if (searchTypeTaskTotal == 'masterOut') {
            _dateP();
            _initiaiHandicaps(searchTypeTaskTotal); $('#timeScope_masterOut').val('');
            _search();
        } else if (searchTypeTaskTotal == 'toOutDraw') {
            _initiaiHandicaps(searchTypeTaskTotal);_search();
        } else {
            _dateP();
            //_search();
            //setTimeout('_search', 100);//延迟加载
        }
        if(type=='outDrawing'){
           _search();
        }
    } else {
        if (timeOutSearchTasks) {
            clearInterval(timeOutSearchTasks);
            timeOutSearchTasks = null;
        }
        if (searchTypeTaskTotal == 'QuickQuery') {
            _resetValuesForTaskTotal('QuickQuery');
            if ($.trim($('#orderNo_QuickQuery').val()) || $.trim($('#member_QuickQuery').val())) {
                _quickQueryClick();//弹出子窗口
            }
        } else {
            _dateP();
            _searchBackWash();
        }
    }

}
var newWindCount = null;
var oldOrderNoOrMember = null;
window.onbeforeunload = function () {
    newWindCount = null;
    oldOrderNoOrMember = null;
    window.sessionStorage.clear();
};

/**快捷查询*/
function _quickQueryClick() {
    if (timeOutSearchTasks) {
        clearInterval(timeOutSearchTasks);
    }
    if (!$.trim($('#orderNo_QuickQuery').val()) && !$.trim($('#member_QuickQuery').val())){
        $.gritter.add({
            time: 1500,
            class_name: '',
            title: '系统消息',
            text: '请输入订单号或者会员名',
            sticky: false,
            image: '../images/message.png'
        });
        return;
    }
    var openUrl = '/html/outward/quickQueryForOut.html?';//两个参数肯定其中有一个所以url默认带'?'
    if ($.trim($('#orderNo_QuickQuery').val())) {
        openUrl += 'orderNo=' + $.trim($('#orderNo_QuickQuery').val());
    } else {
        openUrl += 'orderNo=null';
    }
    if ($.trim($('#member_QuickQuery').val())) {
        openUrl += '&memberName=' + $.trim($('#member_QuickQuery').val());
    } else {
        openUrl += '&memberName=null';
    }
    var startTime = '', endTime = '';
    var startAndEnd = $('#timeScope_QuickQuery').val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    } else {
        startTime = _getDefaultTime()[0];
        endTime = _getDefaultTime()[1];
    }
    if (startTime && endTime){
        openUrl += '&startTime=' + $.trim(startTime)+'&endTime='+$.trim(endTime);
    }else{
        openUrl += '&startTime=null&endTime=null';
    }
    //一个页面打开不同的子窗口 sessionStorage 对子父窗口都有效
    _openNewWindow(openUrl, "");
    //_searchQuickQuery(openUrl);
}
function _openNewWindow(url, winName) {
    var urlPrefix = window.location.origin + '/' + getCookie('JVERSION');
    url = urlPrefix + url;
    var winSet = "scrollbars=yes,menubar=no,resizable=yes,status=no,location=no,toolbar=no,alwaysRaised=yes,depended=yes";
    var _w = getSubWindow();
    var left = (_w.document.body.clientWidth ? _w.document.body.clientWidth : _w.document.documentElement.clientWidth) - 1100;
    if (left < 0) {
        left = 0;
    }
    var positions = ",width=1000,height=300,left=" + left + ",top=230";
    var winSizeStr = winSet + positions;
    _w.open(url, winName + '_blank', winSizeStr);
}
function getSubWindow() {
    var w = window;
    if (window != window.parent) {
        if (window.parent != window.parent.parent) {
            w = window.parent.parent;
        }
        else {
            w = window.parent;
        }
    }
    return w;
}

//    Processing(0, "正在审核"), Approved(1, "审核通过"), Reject(2, "拒绝"), ManagerProcessing(3, "主管处理"), Canceled(4, "已取消"), Acknowledged(5, "出款成功，平台已确认"), Failure(6, "出款成功，与平台确认失败");
function _showReqStatus(obj) {
    var status = '';
    if (obj == 0) {
        obj = 10110000;
    }
    switch (obj) {
        case 10110000:
            status = "正在审核";
            break;
        case 1:
            status = "审核通过";
            break;
        case 2:
            status = "拒绝";
            break;
        case 3:
            status = "主管处理";
            break;
        case 4:
            status = "已取消";
            break;
        case 5:
            status = "出款成功，平台已确认";
            if (isHideOutAccountAndModifyNouns){
                status = "出货成功，平台已确认";
            }
            break;
        case 6:
            status = "出款成功，与平台确认失败";
            if (isHideOutAccountAndModifyNouns){
                status = "出货成功，与平台确认失败";
            }
            break;
        default:
            status = '';
            break;
    }
    return status;
}
//Undeposit(0, "未出款"), Deposited(1, "已出款"), ManagerDeal(2, "主管处理"), ManageCancel(3, "主管取消"), ManageRefuse(4,
//"主管拒绝"), Matched(5, "流水匹配"), Failure(6, "转排查"), Invalid(7, "无效记录，已重新出款"), ManageDiscard(8, "主管丢弃");
function _showTaskStatus(obj) {
    var status = '';
    if (obj == 0) {
        obj = 10110000;
    }
    switch (obj) {
        case 10110000:
            status = "未出款";
            break;
        case 1:
            status = "已出款";
            break;
        case 2:
            status = "主管处理";
            break;
        case 3:
            status = "主管取消";
            break;
        case 4:
            status = "主管拒绝";
            break;
        case 5:
            status = "流水匹配";
            break;
        case 6:
            status = "转排查";
            break;
        case 7:
            status = "无效记录，已重新出款";
            break;
        case 8:
            status = "银行维护";
            break;
        default:
            status = '';
            break;
    }
    return status;
}

/**
 * 查询 根据type查询不同页面
 */
function _search() {
    if (timeOutSearchTasks && currentPageLocation.indexOf('OutwardTaskTotal:*') <= -1) {
        clearInterval(timeOutSearchTasks);
        timeOutSearchTasks = null;
        return;
    }
    var type = searchTypeTaskTotal, maintain_toOutDraw = null;
    if (type == 'toOutDraw') {
    	queryRebate();
    	if ($('#maintain_toOutDraw').prop('checked')) {
            maintain_toOutDraw = $('#maintain_toOutDraw').val();
        }
    }
    var handicap = '', handId = $('#handicap_' + type).val();
    if (handId && handId.indexOf('请选择') < 0) {
        //handicap = handId;
        handicap = $('#handicap_' + type+'  option[value="'+handId+'"]').attr('handicap_code');
    }
    if(type=="failedOut"){
    	queryCheck();
    }
    //主管处理 待排查 层级改为内外中 查询出款卡的层级的
    var level = '';
    //正在出款 未出款 层级为内外中 查询出款任务的层级
    var sysLevel = '';
    var orderNo = null,orderNoR = $('#orderNo_' + type).val();
    if (orderNoR) {
        if (orderNoR.indexOf('%') >= 0)
            orderNo = orderNoR.replace(new RegExp(/%/g), "?");
        else
            orderNo = orderNoR;
    }
    var member = null, memberR = $('#member_' + type).val();
    if (memberR) {
        if (memberR.indexOf('%') >= 0)
            member = memberR.replace(new RegExp(/%/g), "?");
        else
            member = memberR;
    }
    var fromAccount = null,operator = null;
    if (type != 'toOutDraw') {
        if ($('#account_' + type).val() && $('#account_' + type).val().indexOf('请选择') < 0) {
            fromAccount = $('#account_' + type).val();
        }
        if ($('#operator_' + type).val()) {
            operator = $('#operator_' + type).val();
        }
    }
    // 人工 机器 代付(指针对待排查) 手机(只针对完成出款)
    var robot = '', manual = '',phone='' ,type1 = '',all ='',thirdInsteadPay='';
    if (type == 'successOut' || type == 'failedOut' || type == 'masterOut') {
        if ($('#robot_' + type).prop('checked')) {
            robot = $('#robot_' + type).val(),type1=robot;
        }
        if ($('#manual_' + type).prop('checked')) {
            manual = $('#manual_' + type).val(),type1=manual;
        }
        if(type == 'successOut' && $('#phone_' + type).prop('checked')){
            phone =$('#phone_' + type).val(),type1=phone;
        }

        if(type == 'successOut'){
            if (robot && manual && phone || $('#allType_' + type).prop('checked')) {
                robot = manual = phone =type1='';
            }
        }else{
            if (type == 'failedOut' && $('#daifu_' + type).prop('checked')){
                thirdInsteadPay = $('#daifu_' + type).val();
            }
            if ($('#allType_' + type).prop('checked')){
                robot =  manual = thirdInsteadPay = type1='';
            }
        }
    }
    var startTime = '', endTime = '';
    var startAndEnd = $('#timeScope_' + type).val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    } else {
        //startTime = _getDefaultTime()[0];
       // endTime = _getDefaultTime()[1];
        if (type == 'failedOut' || type == 'masterOut') {

        } else {
            startTime = _getDefaultTime()[0];
            endTime = _getDefaultTime()[1];
        }
    }
    var fromMoney = '', toMoney = '';
    if ($('#fromMoney_' + type).val()) {
        fromMoney = $('#fromMoney_' + type).val();
    }
    if ($('#toMoney_' + type).val()) {
        toMoney = $('#toMoney_' + type).val();
    }
    var CurPage = $("#" + type + "_footPage").find(".Current_Page").text();
    CurPage = CurPage?CurPage>0?CurPage-1:0:0;
    var data = {}, url = '';
    if (type=='failedOutDealed') {
        url = '/r/taskReview/troubleShoot';
        var shooter = $.trim($('#shooter_failedOutDealed').val());
        level = $('#level_failedOutDealed').text()=='请选择'?'':$('#level_failedOutDealed').val();
        if (level){
            level = $('#level_failedOutDealed option[value="'+level+'"]').text();
        }
        data =  {
            "handicap": handicap,"level":level,"orderNo": orderNo,
            "startTime": $.trim(startTime),"endTime": $.trim(endTime),
            "member": member,"amountStart": fromMoney,"amountEnd": toMoney,
            "outAccount": fromAccount,"operator": operator,"shooter": shooter,
            "type": type1,"queryType":  2,"pageType":2,
            "pageNo": CurPage,"pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : 10
        }
    } else {
        if(type=='masterOut' || type=='failedOut'){
            level = $('input[name="level_'+type+'"]:checked').val();
            level = level==3?"":level;
        }
        else if ( type=='successOut') {
            level = $('#level_successOut').val();
            level = level=='请选择'?"":level;
            sysLevel = $('input[name="sysLevel_'+type+'"]:checked').val();
            sysLevel = sysLevel==3?"":sysLevel;
		}
        else if(type=='outDrawing'|| type=='toOutDraw' ){
            sysLevel = $('input[name="sysLevel_'+type+'"]:checked').val();
            sysLevel = sysLevel==3?"":sysLevel;
        }else{
            var levelId = $('#level_' + type).val();
            if (levelId && levelId.indexOf('请选择') < 0) {
                //level = levelId;
                level = $('#level_failedOutDealed option[value="'+levelId+'"]').text() || $('#level_' + type).val();
            }
        }
        url = '/r/outtask/total';
        data = {"thirdInsteadPay":thirdInsteadPay,
            "handicap": handicap,"level": level,"orderNo": orderNo,"startTime": $.trim(startTime),
            "endTime": $.trim(endTime),"member": member,"fromMoney": fromMoney,"toMoney": toMoney,"sysLevel":sysLevel,
            "accountAlias": fromAccount,"operatorName": operator,"manual": manual,"robot": robot,"flag": type,"maintain": maintain_toOutDraw,
            "pageNo": CurPage,"pageSize": $.session.get('initPageSize')?$.session.get('initPageSize'):10
        }
        if (type == 'successOut') {
            // 出款类型 银行卡 第三方 代付
            var drawType1 = $('#bankDraw_successOut').prop('checked')?$('#bankDraw_successOut').val():'';
            var drawType2 = $('#thirdDraw_successOut').prop('checked')?$('#thirdDraw_successOut').val():'';
            thirdInsteadPay = $('#daifuDraw_successOut').prop('checked')?$('#daifuDraw_successOut').val():'';
            if ($('#allDraw_successOut').prop('checked')){
                drawType1 = drawType2 = thirdInsteadPay ='';
            }
            var drawType = drawType1 || drawType2 || thirdInsteadPay || all;
            data = $.extend(data, {"drawType":drawType,"sortFlag": sortFlagForSuccessOut,"phone":phone});
        }
        //|| type=='toOutDraw' || type=='masterOut' || type=='failedOut'
        if(type=='outDrawing'){
        	queryRebateDrawing();
        	var bankType = $('#bank_'+type).val();//银行类型
            if(bankType&&bankType!='请选择'){
                bankType = $('#bank_'+type).val();
            }else{
                bankType = '';
            }
           data = $.extend(data, {"bankType": bankType});
        }
    }
    $.ajax({
        type: 'get', url: url, data: data, async: false, dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    if (type == 'failedOutDealed'){
                        _fillReviewedTasksData(res, type);showPading(res.page, type + '_footPage', _search);
                        if(outwardAllRecordSum){
                            _getFailedOutDealedSum(data, type);
                        }
                        _getFailedOutDealedCount(data, type);
                    }else{
                        if (type == 'outDrawing') {
                            _fillDataForOutDrawing(res.data, type);
                            if ($('#outDrawing  #promptMessageTotalOutDrawing')) {
                                $('#outDrawing  #promptMessageTotalOutDrawing').remove();
                            }
                            $('#outDrawing').append('<div id="promptMessageTotalOutDrawing"><span style="color: mediumvioletred;font-size: 15px">温馨提示：红色<span style="background-color: indianred">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过10分钟。' +
                                '绿色<span style="background-color: lightgreen">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过3分钟。</span></div>');
                        }
                        if (type == 'toOutDraw') {
                            _fillDataForToOut(res.data, type);
                        }
                        if (type == 'masterOut') {
                            _fillDataForMasterDeal(res.data, type);
                            if ($('#masterOut  #promptMessageTotalMasterOut')) {
                                $('#masterOut  #promptMessageTotalMasterOut').remove();
                            }
                            _checkTroubleShootingOnlineUsersInfo(2);
                            $('#masterOut').append('<div id="promptMessageTotalMasterOut"><span style="color: mediumvioletred;font-size: 15px">温馨提示：红色<span style="background-color: indianred">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过10分钟。</span></div>');
                        }
                        if (type == 'failedOut') {
                            _fillDataForFailOut(res.data, type);
                            if ($('#failedOut  #promptMessageTotalFailedOut')) {
                                $('#failedOut  #promptMessageTotalFailedOut').remove();
                            }
                            _checkTroubleShootingOnlineUsersInfo(6);
                            //浅绿色<span style="background-color: lightgreen">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示该条记录状态为"已匹配流水"。</span>
                            $('#failedOut').append('<div id="promptMessageTotalFailedOut" ><span style="color: mediumvioletred;font-size: 15px" >温馨提示：红色<span style="background-color: indianred">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过5分钟。</div> ');
                            //_checkTroubleShootingOnlineUsersInfo();
                        }
                        if (type == 'successOut') {
                            _fillDataForSuccessOut(res.data, type);
                        }
                        if (type == 'refused' || type == 'canceled') {
                            _fillDataForRefusedOrCanceledOut(res.data, type);
                        }
                        if (type == 'masterOut' || type == 'failedOut' || type == 'successOut') {
                            $('#' + type).find('th').addClass('no-padding-right-td');
                            $('#' + type).find('td').addClass('no-padding-right-td');
                        }
                        showPading(res.page, type + '_footPage', _search);
                        if(outwardAllRecordSum){
                            _getOutwardTaskTotalSum(data, type);
                        }
                        _getOutwardTaskTotalCount(data, type);
                    }
                }
            }

        }
    });
}
function _getFailedOutDealedSum(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/taskReview/troubleShootSum', async: false,
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $('#TotalSum' + type).text(parseFloat(res.data).toFixed(3));
                }
            }
        }
    });
}
function _getFailedOutDealedCount(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/taskReview/troubleShootCount', async: false,
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#CurrentCount' + type).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#AllCount' + type).empty().text('合计：' + res.page.totalElements + '条记录');
                }
                showPading(res.page, type + '_footPage', _search);
            }
        }
    });
}
//已排查数据渲染
function _fillReviewedTasksData(res, subPageType) {
    var data = res.data, idListThird=[], idList = [], tr = '', trs = '';
    $('#tbody_' + subPageType).empty();
    if (data&&data.length>0) {
        var amount = 0;
        $(data).each(function (i, val) {
            var screenshotArr = null;
            if (_checkObj(val.photo)) {
                screenshotArr = val.photo.split('/');
            }
            var hasScreenshot = false;
            if (screenshotArr) {
                if (screenshotArr.length > 3) {
                    hasScreenshot = (screenshotArr[3]).indexOf('screenshot') > -1;
                }  else if (screenshotArr.length>1){
                    hasScreenshot = (screenshotArr[2]).indexOf('screenshot') > -1;
                } else{
                    hasScreenshot = (screenshotArr[0]).indexOf('screenshot') > -1;
                }
            }
            // idList.push({'id': val.accountId});
            var third=val.thirdInsteadPay&& val.accountId;
            if (third){
                idListThird.push(  val.accountId );
            } else{
                idList.push({'id': val.accountId});
            }
            if (val) {
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                    //是否是公司用款
                    if(!isHideAccount){
                        tr += '<td><a  href="javascript:_showOrderDetail(' + val.reqId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                    }else{
                        tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    }
                } else {
                    tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                }
                tr += '<td>' + _checkObj(val.amount) + '</td>';
                if (val.taskStatus == '流水匹配') {
                    //已匹配流水的记录 浅绿色背景标识
                    tr += '<td style="background-color: lightgreen">' + _checkObj(val.taskStatus) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.taskStatus) + '</td>';
                }
                tr += '<td>' + _checkObj(val.drawer) + '</td><td>'+val.shooter+'</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccount)) {
                    outAccount += "|" + hideAccountAll(val.outAccount);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountOwner)) {
                    outAccount += "|" + hideName(val.outAccountOwner);
                } else {
                    outAccount += '无';
                }
                if (!outAccount || outAccount == '无无无') {
                    outAccount = '';
                }
                if (third){
                    if (outAccount) {
                        tr += '<td>' +
                            "<a  class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                } else{
                    if (outAccount) {
                        tr += '<td>' +
                            "<a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                }

                tr += '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + timeStamp2yyyyMMddHHmmss(_checkObj(val.asignTime)) + '" data-original-title="排查时间">' + timeStamp2yyyyMMddHHmmss(_checkObj(val.asignTime)) + '</a></td>';

                if (val.failedOutTime5 == 'true') {
                    tr += '<td style="background-color: indianred;color: white">' + _checkObj(val.timeConsume) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.timeConsume) + '</td>';
                }
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td>'
                            + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td>'
                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark)
                            + '</a>'
                            + '</td>';

                    }
                } else {
                    tr += '<td></td>';
                }
                if (!isHideImg&&_checkObj(val.photo)) {
                    if (hasScreenshot) {
                        tr += '<td>' +
                            '<a  href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + _checkObj(val.photo) + '\')">查看</a>' +
                            '</td>';
                    } else {
                        tr += '<td>' +
                            '<a   href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + _checkObj(val.photo) + '\')">回执</a>' +
                            '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                tr += '</tr>';
                amount += val.amount;
            }
        });
        if(outwardCurrentPageSum){
            trs +='<tr><td id="CurrentCount' + subPageType + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width:  auto;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs +='<tr><td id="CurrentCount' + subPageType + '" colspan="15">小计：统计中..</td></tr>' ;
        }
        if(outwardAllRecordSum){
            trs += '<tr><td id="AllCount' + subPageType + '" colspan="4">总共：统计中..</td>' +
                '<td id="TotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width:  auto;">统计中..</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs += '<tr><td id="AllCount' + subPageType + '" colspan="15">总共：统计中..</td></tr>';
        }
        $('#tbody_' + subPageType).empty().html(tr).append(trs);
    }
    $("[data-toggle='popover']").popover();
    //加载账号悬浮提示
    if(!isHideAccount){
        accountPopOver(idList);  loadHover_thirdDaiFuHover(idListThird);
    }
}
//账号悬浮与否
function accountPopOver(idList) {
    if(!isHideAccount){
        loadHover_accountInfoHover(idList);
    }
}
//正在出款数据渲染
function _fillDataForOutDrawing(data, subPageType) {
    var idList = [], idListThird=[],  tr = '' ,  trs = '';
    $('#tbody_' + subPageType).empty();
    if (data) {
        var amount = 0;
        $(data).each(function (i, val) {
            if (val) {
                // idList.push({'id': val.accountId});
                var third=val.thirdInsteadPay&&val.accountId;
                if (third){
                    idListThird.push( val.accountId );
                } else{
                    idList.push({'id': val.accountId});
                    idList.push({'id': val.accountId,type:'transInfoHover'});
                }

                amount += val.amount;
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (val.timeGap) {
                    //是否超时
                    if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                        //是否是公司用款
                        if (!isHideAccount)
                        tr += '<td><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                        else tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    } else {
                        tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    }
                } else {
                    if (_checkObj(val.member)) {
                        //是否是公司用款
                        if (!isHideAccount)
                        tr += '<td><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                        else tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    } else {
                        tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    }
                }
                tr += '<td>'+_checkObj(val.operatorType)+'</td>';
                tr += '<td>' + _checkObj(val.operator) + '</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccount)) {
                    outAccount += "|" +   hideAccountAll(val.outAccount);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountOwner)) {
                    outAccount += "|" + hideName(val.outAccountOwner);
                } else {
                    outAccount += '无';
                }
                if (!outAccount || outAccount == '无无无') {
                    outAccount = '';
                }
                if (third){
                    if (outAccount) {
                        tr += '<td>' +
                            "<a  class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                } else{
                    if (outAccount) {
                        tr += '<td>' +
                            "<a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                }

                if(outAccount){
                    tr += '<td><a  class="bind_hover_card breakByWord" data-toggle="transInfoHover' + val.accountId + '" data-placement="auto left" data-trigger="hover">' + _checkObj(val.amount) + '</a></td>';
                }else{
                    tr += '<td>' + _checkObj(val.amount) + '</td>';
                }
                tr += '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + _checkObj(val.asignTime) + '" data-original-title="认领时间">' + _checkObj(val.asignTime).substring(11, 19) + '</a></td>';
                if (val.timeGap3to10) {
                    tr += '<td style="color: white;background-color: limegreen">' + _checkObj(val.timeUsed) + '</td>';
                } else if (val.timeGapMore10) {
                    tr += '<td style="color: white;background-color: indianred">' + _checkObj(val.timeUsed) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.timeUsed) + '</td>';
                }
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td>'
                            + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td>'
                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark)
                            + '</a>'
                            + '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                // if (Noteflag) {
                //     tr += '<td style="width: 115px;text-align: center;"><button type="button"  onclick="SCustomerserviceRemark(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                //         '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                // }
                // if (val.timeGap){
                //     //超时的单子
                //     tr += '<button type="button"  onclick="_dealAfter3minutes(' + val.id + ');"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                //     '<i class="ace-icon fa  fa-check-square-o bigger-100 green"></i>处理</button>';
                // }
                // tr += '</td>';

                if (Noteflag) {
                    tr += '<td style="width: 55px;text-align: center;"><button type="button"  onclick="SCustomerserviceRemark(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button></td>';
                } else {
                    tr += '<td></td>';
                }

            }
        });
        if(outwardCurrentPageSum){
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="7">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 100px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="6"></td></tr>' ;
        }else{
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="15">小计：统计中..</td></tr>' ;
        }
        if(outwardAllRecordSum){
            trs +='<tr>' +
                '<td id="outwardTaskTotalAllCount' + subPageType + '" colspan="7">总共：统计中..</td>' +
                '<td id="outwardTaskTotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td><td colspan="6"></td></tr>';
        }else{
            trs+='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="15">总共：统计中..</td></tr>';
        }
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);
    $("[data-toggle='popover']").popover();
    if(!isHideAccount){
        accountPopOver(idList); loadHover_thirdDaiFuHover(idListThird);
    }
}

//未出款数据渲染
function _fillDataForToOut(data, subPageType) {
    var tr = '',trs = '';$('#tbody_' + subPageType).empty();
    if (data) {
        var amount = 0;
        $(data).each(function (i, val) {
            if (val) {
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (_checkObj(val.member) && _checkObj(val.orderNo) && _checkObj(val.member) != '公司用款') {
                    //是否是公司用款
                    if (!isHideAccount)
                    tr += '<td><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                    else  tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                }
                tr += '<td>' + _checkObj(val.amount) + '</td>';
                // '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + _checkObj(val.asignTime) + '" data-original-title="生成任务时间">' + _checkObj(val.asignTime).substring(11, 19) + '</a></td>';
                // '<td>' + _checkObj(val.timeConsume) + '</td>';
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td><a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td>'
                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark)
                            + '</a>'
                            + '</td>';
                    }
                }else{
                    tr += '<td></td>';
                }
                tr += '<td id="masterToOutOperate" style=" width: 210px;text-align: center">';
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款' && TurningPlatformflag) {
                    tr += ' <button type="button"  onclick="_beforeFeedBack(3,' + val.id + ',' + val.outwardRequestId + ');"  class="btn btn-xs btn-white btn-danger btn-bold">' +
                        '<i class="ace-icon fa fa-remove  bigger-100 red"></i>取消</button>';
                }
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款' && Freezeflag) {
                    tr += '<button type="button" onclick="_beforeFeedBack(4,' + val.id + ',' + val.outwardRequestId + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-bolt bigger-100 blue"></i>拒绝</button>';
                }
                if (Distributionflag) {
                    tr += '<button type="button" onclick="_distribution(' + val.id + ',' + '\'1\');"  class="btn btn-xs btn-white btn-info btn-bold">' +
                        '<i class="ace-icon fa fa-user-circle-o  bigger-100 red"></i>分配</button>';
                }
                if (Noteflag) {
                    tr += '<button type="button" onclick="SCustomerserviceRemark(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                }
                tr += '</td>';
                amount += val.amount;
            }
        });
        if(outwardCurrentPageSum){
            trs+='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 100px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="6"></td></tr>' ;
        }else{
            trs+='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="15">小计：统计中..</td></tr>' ;
        }
        if(outwardAllRecordSum){
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="4">总共：统计中..</td>' +
                '<td id="outwardTaskTotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td><td colspan="6"></td></tr>';
        }else{
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="15">总共：统计中..</td></tr>';
        }
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);
    $("[data-toggle='popover']").popover();
}
//主管处理数据渲染
function _fillDataForMasterDeal(data, subPageType) {
    var idList = [], idListThird =[], tr = '', trs = '';$('#tbody_' + subPageType).empty();
    if (data) {
        var amount = 0;
        $(data).each(function (i, val) {
            var screenshotArr = null;
            if (_checkObj(val.successPhotoUrl)) {
                screenshotArr = val.successPhotoUrl.split('/');
            }
            var hasScreenshot = false;
            if (screenshotArr) {
                if (screenshotArr.length > 3) {
                    hasScreenshot = (screenshotArr[3]).indexOf('screenshot') > -1;
                } else if (screenshotArr.length>1){
                    hasScreenshot = (screenshotArr[2]).indexOf('screenshot') > -1;
                } else{
                    hasScreenshot = (screenshotArr[0]).indexOf('screenshot') > -1;
                }
            }
            var third=val.thirdInsteadPay&&val.accountId;
            if (third){
                idListThird.push(  val.accountId );
            } else{
                idList.push({'id': val.accountId});
            }
            if (val) {
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                    //是否是公司用款
                    if (!isHideAccount)
                    tr += '<td><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                    else tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                }
                tr += '<td>' + _checkObj(val.amount) + '</td>' +
                    '<td>' + _checkObj(val.operator) + '</td><td>'+_checkObj(val.taskHolder)+'</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (!isHideOutAccountAndModifyNouns){
                    if (_checkObj(val.outAccount)) {
                        outAccount += "|" + hideAccountAll(val.outAccount);
                    } else {
                        outAccount += '无';
                    }
                    if (_checkObj(val.outAccountOwner)) {
                        outAccount += "|" + hideName(val.outAccountOwner);
                    } else {
                        outAccount += '无';
                    }
                }

                if (!outAccount || outAccount == '无无无') {
                    outAccount = '';
                }
                if (third){
                    if (outAccount) {
                        tr += '<td>' +
                            "<a  class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                } else{
                    if (outAccount) {
                        tr += '<td>' +
                            "<a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                }

                tr += '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + _checkObj(val.asignTime) + '" data-original-title="认领时间">' + _checkObj(val.asignTime).substring(11, 19) + '</a></td>';
                if (val.masterOutTime10) {
                    tr += '<td style="color: white;background-color: indianred;">' + _checkObj(val.timeConsume) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.timeConsume) + '</td>';
                }
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td><a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td>'
                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark)
                            + '</a>'
                            + '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                if (_checkObj(val.successPhotoUrl)) {
                    if (hasScreenshot) {
                        tr += '<td>' +
                            '<a  href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + _checkObj(val.successPhotoUrl) + '\')">查看</a>' +
                            '</td>';
                    } else {
                        tr += '<td>' +
                            '<a href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + _checkObj(val.successPhotoUrl) + '\')">回执</a>' +
                            '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                tr += '<td style="width: 270px;text-align: center" id="masterOutOperate">';
                if (Distributionflag) {
                    tr += '<button type="button" onclick="_distribution(' + val.id + ',' + '\'2\');"  class="btn btn-xs btn-white btn-info btn-bold">' +
                        '<i class="ace-icon fa fa-user-circle-o  bigger-100 red"></i>分配</button>';
                }
                if (_checkObj(val.member) && TurningPlatformflag) {
                    tr += '<button type="button"  onclick="_beforeFeedBack(3,' + val.id + ',' + val.outwardRequestId + ');"  class="btn btn-xs btn-white btn-danger btn-bold">' +
                        '<i class="ace-icon fa fa-remove  bigger-100 red"></i>取消</button>';
                }
                if (_checkObj(val.member) && Freezeflag) {
                    tr += '<button type="button" onclick="_beforeFeedBack(4,' + val.id + ',' + val.outwardRequestId + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-hand-paper-o bigger-100 blue"></i>拒绝</button>';
                }
                if (Noteflag) {
                    tr += '<button type="button"  onclick="SCustomerserviceRemark(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                }
                // if (_checkObj(val.member) && SendMessageflag && !thirdRemarkFlag) {
                //     tr += '<button type="button"    onclick="sendmessage();"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                //         '<i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i>发消息</button>';
                // }
                if (FinishTask && val.accountId && val.thirdRemarkFlag == 'no') {
                    tr += '<button type="button"  onclick="_beforeTotalTurntoFinished(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                }
                tr += '</td>';
                amount += val.amount;
            }
        });
        if(outwardCurrentPageSum){
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 100px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="15"></td></tr>' ;
        }else{
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="20">小计：统计中..</td></tr>' ;
        }
        if(outwardAllRecordSum){
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="4">总共：统计中..</td>' +
                '<td id="outwardTaskTotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="20">总共：统计中..</td></tr>';
        }
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);$("[data-toggle='popover']").popover();
    if(!isHideAccount){
        accountPopOver(idList);
        loadHover_thirdDaiFuHover(idListThird);
    }
}
//待排查数据渲染
function _fillDataForFailOut(data, subPageType) {
    var idList = [],idListThird=[], tr = '', trs = '';
    $('#tbody_' + subPageType).empty();
    if (data) {
        var amount = 0;
        $(data).each(function (i, val) {
            var screenshotArr = null;
            if (_checkObj(val.successPhotoUrl)) {
                screenshotArr = val.successPhotoUrl.split('/');
            }
            var hasScreenshot = false;
            if (screenshotArr && screenshotArr instanceof Array) {
                if (screenshotArr.length > 3) {
                    hasScreenshot = (screenshotArr[3]).indexOf('screenshot') > -1;
                } else if (screenshotArr.length>1){
                    hasScreenshot = (screenshotArr[2]).indexOf('screenshot') > -1;
                } else{
                    hasScreenshot = (screenshotArr[0]).indexOf('screenshot') > -1;
                }
            }
            var third=val.thirdInsteadPay&&val.accountId;
            if (third){
                idListThird.push( val.accountId );
            } else{
                idList.push({'id': val.accountId});
            }
            if (val) {
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                    //是否是公司用款
                    if (!isHideAccount)
                    tr += '<td><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                    else  tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                }
                tr += '<td>' + _checkObj(val.amount) + '</td>';
                if (val.taskStatus == 5) {
                    //已匹配流水的记录 浅绿色背景标识
                    tr += '<td style="background-color: lightgreen">' + _showTaskStatus(val.taskStatus) + '</td>';
                } else {
                    tr += '<td>' + _showTaskStatus(val.taskStatus) + '</td>';
                }
                tr += '<td>' + _checkObj(val.operator) + '</td><td>'+_checkObj(val.taskHolder)+'</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccount)) {
                    outAccount += "|" +  hideAccountAll(val.outAccount);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountOwner)) {
                    outAccount += "|" + hideName(val.outAccountOwner);
                } else {
                    outAccount += '无';
                }
                if (!outAccount || outAccount == '无无无') {
                    outAccount = '';
                }
                if (third){
                    if (outAccount) {
                        tr += '<td>' +
                            "<a  class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                } else{
                    if (outAccount) {
                        tr += '<td>' +
                            "<a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                }

                tr += '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + _checkObj(val.asignTime) + '" data-original-title="认领时间">' + _checkObj(val.asignTime).substring(11, 19) + '</a></td>';

                if (val.failedOutTime5) {
                    tr += '<td style="background-color: indianred;color: white">' + _checkObj(val.timeConsume) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.timeConsume) + '</td>';
                }
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td>'
                            + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td>'
                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark)
                            + '</a>'
                            + '</td>';

                    }
                } else {
                    tr += '<td></td>';
                }
                if (_checkObj(val.successPhotoUrl)) {
                    if (hasScreenshot) {
                        tr += '<td>' +
                            '<a  href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\''  + _checkObj(val.successPhotoUrl) + '\')">查看</a>' +
                            '</td>';
                    } else {
                        tr += '<td>' +
                            '<a   href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + _checkObj(val.successPhotoUrl) + '\')">回执</a>' +
                            '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                if (hasScreenshot && UploadReceipt && _checkObj(val.member) && NewGenerationTaskflag) {
                    tr += '<td style="width: 254px;text-align: center" class="no-padding-right">';
                } else {
                    tr += '<td style="width: 210px;text-align: center" >';
                }
                if (_checkObj(val.member) && NewGenerationTaskflag) {
                    //_distribution(' + val.id + ',' + '\'2\');
                    //_recreateTaskForTotal(' + val.id + ',' + val.accountId + ');
                    tr += '<button onclick="_distribution(' + val.id + ',' + '\'3\');" type="button"  class=" btn btn-xs btn-white btn-warning  btn-bold">' +
                        '<i class="ace-icon fa fa-reply  bigger-100 red"></i>重新生成任务</button>';
                }
                if (Noteflag) {
                    tr += '<button type="button" onclick="SCustomerserviceRemark(' + val.id + ');"  class="btn btn-xs btn-white btn-info btn-bold ">' +
                        '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                }
                if (FinishTask) {
                    tr += '<button type="button"  onclick="_beforeTotalTurntoFinished(' + val.id + ');"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                        '<i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                }
                if (hasScreenshot && UploadReceipt) {
                    tr += '<button type="button"  onclick="_uploadReceiptPhoto(' + val.id + ');"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                        '<i class="fa fa-arrow-circle-up bigger-100 green"></i>回执</button>';
                }
                tr += '</td>';
                amount += val.amount;
            }
        });
        if(outwardCurrentPageSum){
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: auto;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="15"></td></tr>' ;
        }else{
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="15">小计：统计中..</td></tr>' ;
        }
        if(outwardAllRecordSum){
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="4">总共：统计中..</td>' +
                '<td id="outwardTaskTotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width: auto;">统计中..</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="15">总共：统计中..</td></tr>';
        }
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);$("[data-toggle='popover']").popover();
    if(!isHideAccount){
        accountPopOver(idList);
        loadHover_thirdDaiFuHover(idListThird);
    }
    //_checkTroubleShootingOnlineUsersInfo();
}
//成功出款数据渲染
function _fillDataForSuccessOut(data, subPageType) {
    var idList = [],idListThird =[], tr = '', trs = '';
    $('#tbody_' + subPageType).empty();
    if (data) {
        var amount = 0;
        $(data).each(function (i, val) {
            var screenshotArr = null;
            if (_checkObj(val.successPhotoUrl)) {
                screenshotArr = val.successPhotoUrl.split('/');
            }
            var hasScreenshot = false;
            if (screenshotArr) {
                if (screenshotArr.length > 3) {
                    hasScreenshot = (screenshotArr[3]).indexOf('screenshot') > -1 ? true : false;
                } else {
                    hasScreenshot = false;
                }
            }
            var third=val.thirdInsteadPay&&val.accountId;
            if (third){
                idListThird.push( val.accountId );
            } else{
                idList.push({'id': val.accountId});
            }
            if (val) {
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                    //是否是公司用款
                    if (!isHideAccount)
                    tr += '<td><a href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                    else tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                }
                tr += '<td>' + _checkObj(val.amount) + '</td>' +
                    '<td>' + _showTaskStatus(val.taskStatus) + '</td>' +
                    '<td>' + _checkObj(val.operator) + '</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountType)) {
                    outAccount += "|" + _checkObj(val.outAccountType);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccount)) {
                    outAccount += "|" +   hideAccountAll(val.outAccount);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountOwner)) {
                    outAccount += "|" + hideName(val.outAccountOwner);
                } else {
                    outAccount += '无';
                }
                if (!outAccount || outAccount == '无无无无') {
                    outAccount = '';
                }
                if (third){
                    if (outAccount) {
                        tr += "<td><a  class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                } else{
                    if (outAccount) {
                        tr += "<td><a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += '<td></td>';
                    }
                }
                tr += '<td><a class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + _checkObj(val.asignTime) + '" data-original-title="认领时间">' + _checkObj(val.asignTime).substring(11, 19) + '</a></td>';
                // tr += '<td>' + _checkObj(val.timeConsume) + '</td>';
                var timeShow = '提单时间:' + timeStamp2yyyyMMddHHmmss(val.orderTime) + '<br>' +
                    '审核耗时:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' + val.auditTime + '<br>' +
                    '等待时间:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' + val.waitTime + '<br>' +
                    '出款耗时:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' + val.successOutTime + '<br>' +
                    '总计耗时:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' + val.timeConsumingAll;

                if (val.successOutTime15) {
                    //超过15分钟的 红色背景标识
                    tr += '<td style="color: white; background-color: indianred"><a style="color: white;background-color: indianred" ';
                } else if (val.successOutTime3To15) {
                    //超过3分钟的小于15分钟的 红色背景标识
                    tr += '<td style="color: white;background-color: limegreen"><a style="color: white;background-color: limegreen" ';
                } else if (val.successOutTimeOther) {
                    tr += '<td><a ';
                }
                tr += ' class="bind_hover_card breakByWord"  title="耗时统计"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="auto left"'
                    + ' data-content="' + timeShow + '">'
                    + val.timeConsumingAll
                    + '</a>'
                    + '</td>';
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td><a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="auto left"'
                            + ' data-content="' + _divideRemarks(val.remark)  + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(21, 25)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td><a tabindex="0" data-container="body" class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';

                    }
                } else {
                    tr += '<td></td>';
                }
                if (!isHideImg&&_checkObj(val.successPhotoUrl)) {
                    // hasScreenshot旧的截图标识
                    if (hasScreenshot) {
                        tr += '<td><a  href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + _checkObj(val.successPhotoUrl) + '\')">查看</a>' +
                            '</td>';
                    } else {
                        tr += '<td><a   href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + _checkObj(val.successPhotoUrl) + '\')">回执</a>' +
                            '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                if (val.requestStatus != 5) {
                    if (ToFailureflag && (_checkObj(val.member) && NotifyP && (val.requestStatus != 5 || (val.requestStatus == 6 && val.taskStatus != 5) )) && ( UploadReceipt && val.taskStatus == 5)) {
                        tr += '<td style="width:330px;text-align: center" >';
                    } else if (ToFailureflag && Noteflag && !(_checkObj(val.member) && NotifyP && (val.requestStatus != 5 || (val.requestStatus == 6 && val.taskStatus != 5) ) ) && !(UploadReceipt && val.taskStatus == 5)) {
                        tr += '<td style="width:205px;text-align: center">';
                    } else if (ToFailureflag && Noteflag && !(_checkObj(val.member) && NotifyP && (val.requestStatus != 5 || (val.requestStatus == 6 && val.taskStatus != 5) ) ) && (UploadReceipt && val.taskStatus == 5)) {
                        tr += '<td style="width:255px;text-align: center">';
                    } else {
                        tr += '<td style="width:205px;text-align: center">';
                    }

                } else {
                    tr += '<td style="width:252px;text-align: center">';
                }
                if (ToFailureflag && val.thirdRemarkFlag == 'no') {
                    //公司用款也可以转排查  _checkObj(val.member)
                    tr += '<button onclick="_turnToFailForTotal(' + val.id + ');" type="button" class=" btn btn-xs btn-white btn-danger btn-bold">' +
                        '<i class="ace-icon fa fa-share  bigger-100 red"></i>转待排查</button>';

                }
                if ((_checkObj(val.member) && NotifyP && (val.requestStatus != 5 || (val.requestStatus == 6 && val.taskStatus != 5) )) && val.thirdRemarkFlag == 'no') {
                    tr +=
                        '<button onclick="_noticePlatformTotal(' + val.id + ');"  type="button"  class=" btn btn-xs btn-white btn-info btn-bold">' +
                        '<i class="ace-icon fa  fa-envelope-open-o  bigger-100 green"></i>通知平台</button>';
                }
                if (Noteflag) {
                    if (val.thirdRemarkFlag == 'no') {
                        tr += '<button type="button" onclick="SCustomerserviceRemark(' + val.id + ');"  class=" btn btn-xs btn-white btn-info btn-bold ">' +
                            '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                    } else {
                        tr += '<button type="button" onclick="SCustomerserviceRemark(' + val.id + ',\'yes' + '\');"  class=" btn btn-xs btn-white btn-info btn-bold ">' +
                            '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                    }
                }
                if (val.isArtificialOutMoney == 'false') {
                    tr += '<button type="button" onclick="artificial(\'' + val.toAccountNo + '\');"  class=" btn btn-xs btn-white btn-purple btn-bold ">' +
                        '<i class="ace-icon fa fa-eye"></i>设置人工</button>';
                } else {
                    tr += '<button type="button" onclick="cancelArtificial(\'' + val.toAccountNo + '\');"  class=" btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-eye-slash"></i>取消人工</button>';
                }
                if (UploadReceipt && val.taskStatus == 5 && val.thirdRemarkFlag == 'no') {
                    //流水匹配的才有上传回执按钮  不限制是否已经上传 hasScreenshot
                    tr += '<button type="button" onclick="_uploadReceiptPhoto(' + val.id + ');"  class=" btn btn-xs btn-white btn-success btn-bold ">' +
                        '<i class="fa fa-arrow-circle-up bigger-100 green"></i>回执</button>';
                }
                tr += '</td>';
                amount += val.amount;
            }
        });
        if(outwardCurrentPageSum){
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 110px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="15">小计：统计中..</td></tr>' ;
        }
        if(outwardAllRecordSum){
            trs += '<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="4">总共：统计中..</td>' +
                '<td id="outwardTaskTotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width: 110px;">统计中..</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs += '<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="19">总共：统计中..</td></tr>';
        }
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);
    if ($('#successOut  #promptMessageTotal')) {
        $('#successOut  #promptMessageTotal').remove();
    }
    $('#successOut').append('<div id="promptMessageTotal" style="color: mediumvioletred;font-size: 15px">' +
        '<span>温馨提示：如需通知平台，"通知平台"按钮在"出款审核汇总"->"已审核"页签。</span>' +
        '绿色<span style="background-color: limegreen">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: mediumvioletred;font-size: 15px">(表示总耗时在3-15分钟之内)</span>' + '&nbsp;&nbsp;&nbsp;&nbsp;' +
        '红色<span style="background-color: indianred">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span style="color: mediumvioletred;font-size: 15px">(表示总耗时超过15分钟)</span>' +
        '</div>');$("[data-toggle='popover']").popover();
    if(!isHideAccount){
        accountPopOver(idList); loadHover_thirdDaiFuHover(idListThird);
    }
}
//拒绝 取消出款数据渲染
function _fillDataForRefusedOrCanceledOut(data, subPageType) {
    var idList = [], idListThird=[],tr = '', trs = '';
    $('#tbody_' + subPageType).empty();
    if (data) {
        var amount = 0;
        $(data).each(function (i, val) {
            var screenshotArr = null;
            if (_checkObj(val.successPhotoUrl)) {
                screenshotArr = val.successPhotoUrl.split('/');
            }
            var hasScreenshot = false;
            if (screenshotArr) {
                if (screenshotArr.length > 3) {
                    hasScreenshot = (screenshotArr[3]).indexOf('screenshot') > -1;
                } else {
                    hasScreenshot = (screenshotArr[2]).indexOf('screenshot') > -1;
                }
            }
            var third=val.thirdInsteadPay&&val.accountId;
            if (third){
                idListThird.push(  val.accountId );
            } else{
                idList.push({'id': val.accountId});
            }
            if (val) {
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                    //是否是公司用款
                    if (!isHideAccount)
                    tr += '<td ><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                    else tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                }
                tr +=
                    '<td>' + _checkObj(val.amount) + '</td>' +
                    '<td>' + _checkObj(val.operator) + '</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccount)) {
                    outAccount += "|" + hideAccountAll(val.outAccount);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountOwner)) {
                    outAccount += "|" + hideName(val.outAccountOwner);
                } else {
                    outAccount += '无';
                }
                if (!outAccount || outAccount == '无无无') {
                    outAccount = '';
                }
                if (third){
                    if (outAccount) {
                        tr += "<td><a  class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += "<td></td>";
                    }
                } else{
                    if (outAccount) {
                        tr += "<td><a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                            + outAccount +
                            "</a>" +
                            '</td>';
                    } else {
                        tr += "<td></td>";
                    }
                }

                tr += '<td><a class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + _checkObj(val.asignTime) + '" data-original-title="认领时间">' + _checkObj(val.asignTime).substring(11, 19) + '</a></td>';
                tr += '<td>' + _checkObj(val.timeConsume) + '</td>';
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td><a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(21, 25)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td>'
                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                if (!isHideImg&&_checkObj(val.successPhotoUrl)) {
                    if (hasScreenshot) {
                        tr += '<td>' +
                            '<a href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\''  + _checkObj(val.successPhotoUrl) + '\')">查看</a>' +
                            '</td>';
                    } else {
                        tr += '<td>' +
                            '<a href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\''   + _checkObj(val.successPhotoUrl) + '\')">回执</a>' +
                            '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                if (Noteflag) {
                    tr += '<td style="width: 55px;text-align: center"><button type="button" onclick="SCustomerserviceRemark(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button></td>';
                } else {
                    tr += "<td></td>";
                }

                tr += '</td>';
                amount += val.amount;
            }
        });
        if(outwardCurrentPageSum){
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 100px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="15">小计：统计中..</td></tr>';
        }
        if(outwardAllRecordSum){
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="4">总共：统计中..</td>' +
                '<td id="outwardTaskTotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="19">总共：统计中..</td></tr>';
        }
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);$("[data-toggle='popover']").popover();
    if(!isHideAccount){
        accountPopOver(idList);  loadHover_thirdDaiFuHover(idListThird);
    }
}

function _uploadReceiptPhoto(taskId) {
    if (taskId) {
        bootbox.confirm("是否向工具发起上传回执消息？", function (res) {
            if (res) {
                $.ajax({
                    type: 'put',
                    url: '/r/outtask/uploadReceiptForTask',
                    dataType: 'json',
                    data: {"taskId": taskId},
                    success: function (res) {
                        if (res) {
                            if (res.status == 1) {
                                _search();
                            }
                            $.gritter.add({
                                time: 1500,
                                class_name: '',
                                title: '系统消息',
                                text: res.message,
                                sticky: false,
                                image: '../images/message.png'
                            });
                        }
                    }
                });
            }
        });
    }
}
function _taskTotalPhoto(name) {
    var url = downloadUrl + name;
    var subName = name.split('/');
    subName = subName[subName.length-1];
    $('#taskTotalImg').attr('src', url);
    $('#taskTotalImg').attr('download', subName);
    $('#taskTotalImg').attr('href', url);
    $('#taskTotalImgModal').modal('show');
    if (browserIsIe()) {
        //是ie等,绑定事件
        $('#downLoadImgBtn').on("click", function () {
            //var imgSrc = $(this).siblings("img").attr("src");
            //调用创建iframe的函数
            _downLoadReportIMG(url);
        });
    } else {
        $('#downLoadImgBtn').attr("download", "");
        $('#downLoadImgBtn').attr("href", url);
    }
}
//-----下载图片-----
function _downLoadReportIMG(imgPathURL) {
    if (!document.getElementById("frameForImg"))
        $('<iframe style="display:none;" id="frameForImg" name="frameForImg" onload="_doSaveAsIMG();" width="0" height="0" src="about:blank"></iframe>').appendTo("body");
    if (document.all.frameForImg.src != imgPathURL) {
        document.all.frameForImg.src = imgPathURL;
    }
    else {
        _doSaveAsIMG();
    }
}
function _doSaveAsIMG() {
    if (document.all.frameForImg.src != "about:blank") {
        window.frames["frameForImg"].document.execCommand("saveAs");
    }
}
function _getOutwardTaskTotalSum(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/outtask/getOutwardTaskTotalSum',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $('#outwardTaskTotalSum' + type).text(parseFloat(res.data.sumAmount).toFixed(3));
                    if (type == 'successOut') {
                        $.each(ContentRight['OutwardTaskTotal:*'], function (name, value) {
                            if (name == 'OutwardTaskTotal:lookUpFinishedTotalAmount:*') {
                                lookUpFinishedTotalAmount = true;
                            }
                        });
                        if (!lookUpFinishedTotalAmount) {
                            $('#outwardTaskTotalSum' + type).text('无权限查看');
                        }
                    }
                }
            }
        }
    });
}
function _getOutwardTaskTotalCount(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/outtask/getOutwardTaskTotalCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#outwardTaskTotalCurrentCount' + type).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#outwardTaskTotalAllCount' + type).empty().text('合计：' + res.page.totalElements + '条记录');
                }
                showPading(res.page, type + '_footPage', _search);
            }
        }
    });
}
function sendmessage() {
    $('#CustomersendMessage_modal').modal('show');
}

function save_message() {
    var message = $.trim($('#messageCont').val());
    if (!message) {
        $('#messageCont_remark').show(10).delay(1000).hide(10);
        return;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/customersendmessage',
        data: {"message": message},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            //隐藏
            $('#CustomersendMessage_modal').modal('hide');
            //成功后把备注清空
            $('#messageCont').val("");
        }
    });
}

/**
 * 显示 订单详情
 * @param id
 * @private
 */
function _showOrderDetail(id, orderNo) {
    $.ajax({
        type: "get",
        url: '/r/out/getById',
        data: {"id": id},
        dataType: "json",
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    _initialOrderText(id + 'detail', res.data);
                }
            }
        }
    });
}


/**转排查任务*/
function _turnToFailForTotal(id) {
    $('#turnFailIdTotal').val(id);
    $('#turnToFailRemarkTotal').val('');
    $('#modal-turnToFailTotal').modal('show');
    $('#confirmTurnToFailTotal').attr('onclick', '_confirmTurnToFailTotal();');
}
function _confirmTurnToFailTotal() {
    if (!$.trim($('#turnToFailRemarkTotal').val())) {
        $('#remark-turnToFailTotal').show(10).delay(1000).hide(10);
        return false;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/turnToFail',
        data: {"taskId": $('#turnFailIdTotal').val(), "remark": $('#turnToFailRemarkTotal').val()},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                _search();
                $('#modal-turnToFailTotal').modal('hide');
            }
        }
    });
}
/**通知平台*/
function _noticePlatformTotal(id) {
    if (id) {
        bootbox.confirm('确定通知平台吗？', function (res) {
            if (res) {
                $.ajax({
                    type: 'post',
                    url: '/r/outtask/noticePlatForm',
                    data: {"taskId": id},
                    dataType: 'json',
                    success: function (res) {
                        if (res) {
                            $.gritter.add({
                                time: 300,
                                class_name: '',
                                title: '系统消息',
                                text: res.message,
                                sticky: false,
                                image: '../images/message.png'
                            });
                            _search();
                        }
                    }
                });
            }
        })
    }
}
/**重新生成任务*/
function _recreateTaskForTotal(taskId, accountId) {
    //重新生成任务时 检查是否存在流水，如果存在流水则给提示信息，避免重复出款
    $.ajax({
        type: 'post',
        url: '/r/outtask/checkBankLog',
        data: {'taskId': taskId},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    bootbox.confirm("存在流水，前去排查？", function (res) {
                        if (res) {
                            showInOutListModal(accountId);
                        } else {
                            $('#turnFailIdTotal').val(taskId);
                            $('#turnToFailRemarkTotal').val('');
                            $('#modal-turnToFailTotal').modal('show');
                            $('#confirmTurnToFailTotal').attr('onclick', '_confirmReGenernate();');
                        }
                    });
                } else {
                    $('#turnFailIdTotal').val(taskId);
                    $('#turnToFailRemarkTotal').val('');
                    $('#modal-turnToFailTotal').modal('show');
                    $('#confirmTurnToFailTotal').attr('onclick', '_confirmReGenernate();');
                }
            }
        }
    });
}

function _confirmReGenernate() {
    if (!$.trim($('#turnToFailRemarkTotal').val())) {
        $('#remark-turnToFailTotal').show(10).delay(1000).hide(10);
        return false;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/recreate',
        data: {'taskId': $('#turnFailIdTotal').val(), "remark": $.trim($('#turnToFailRemarkTotal').val())},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                _search();
                $('#modal-turnToFailTotal').modal('hide');
            }
        }
    });
}
var resetRadio = function (type) {
  if (type=='successOut' || type=='failedOut' || type=='masterOut') {
      $('#allType_'+type).attr('checked_val','2');
      $('#allType_'+type).prop('checked','checked');

      $('#manual_'+type).prop('checked','');
      $('#manual_'+type).attr('checked_val','1');

      $('#daifu_'+type).prop('checked','');
      $('#daifu_'+type).attr('checked_val','1');

      $('#robot_ '+type).prop('checked','');
      $('#robot_ '+type).attr('checked_val','1');

      if (type=='successOut'){
          $('#phone_'+type).prop('checked','');
          $('#phone_'+type).attr('checked_val','1');

          $('#allDraw_'+type).attr('checked_val','2');
          $('#allDraw_'+type).prop('checked','checked');

          $('#bankDraw_'+type).attr('checked_val','1');
          $('#bankDraw_'+type).prop('checked','');

          $('#thirdDraw_'+type).attr('checked_val','1');
          $('#thirdDraw_'+type).prop('checked','');

          $('#daifuDraw_'+type).attr('checked_val','1');
          $('#daifuDraw_'+type).prop('checked','');
      }
  }
};
/**
 * 重置按钮
 * @param type
 * @private
 */
function _resetValuesForTaskTotal(type) {
    resetRadio(type);
    sortFlagForSuccessOut = null;
    _initialSort();
    initPaging($('#' + type + '_footPage'), pageInitial);//重置后页面页脚也恢复初始值
    //_gethandicap(type);
    _initiaiHandicaps(searchTypeTaskTotal);
    if(type=='masterOut' || type=='failedOut'){
        //_initiaiHandicaps(searchTypeTaskTotal);
        $('#level_all_'+type).prop('checked','checked');
        $('#level_all_'+type).attr('checked_val',2);
        $('#level_in_'+type).prop('checked','');
        $('#level_in_'+type).attr('checked_val',1);
        $('#level_out_'+type).prop('checked','');
        $('#level_out_'+type).attr('checked_val',1);
        $('#level_middle_'+type).prop('checked','');
        $('#level_middle_'+type).attr('checked_val',1);
        if(type=='toOutDraw'){
            $('#maintain_toOutDraw').prop('checked','');$('#maintain_toOutDraw').attr('checked_val',1);
        }
    }else if(type=='outDrawing'|| type=='toOutDraw'){
        $('input[name="sysLevel_'+type +'"]').each(function (i) {
            if(this.value!='3'){
                $(this).attr('checked_val',1);$(this).prop('checked','');
            }else{
                $(this).attr('checked_val',1);$(this).prop('checked','checked');
            }
        });
        $('#sysLevel_'+type).prop('checked','checked');
        $('#sysLevel_'+type).attr('checked_val',2);
    }else{
        //_getLevel(type);
    }

    if ($('#maintain_toOutDraw').prop('checked')) {
        $('#maintain_toOutDraw').prop('checked', false);
    }
    if (type != 'QuickQuery') {
        $('#account_' + type).val('');
    }
    var operator = $('#operator_' + type).val();
    if (operator && operator != 'undefined') {
        $('#operator_' + type).val('');
    }
    var orderNoR = $('#orderNo_' + type).val();
    if (orderNoR) {
        $('#orderNo_' + type).val('');
    }
    var memberR = $('#member_' + type).val();
    if (memberR) {
        $('#member_' + type).val('');
    }
    var amount = $('#amount_' + type).val();
    if (amount) {
        $('#amount_' + type).val('');
    }
    if (type == 'failedOut' || type == 'masterOut') {
        $('#timeScope_failedOut').val('');
        $('#timeScope_masterOut').val('');
    } else {
        _dateP();
    }
    if ($('#fromMoney_' + type).val()) {
        $('#fromMoney_' + type).val('');
    }
    if ($('#toMoney_' + type).val()) {
        $('#toMoney_' + type).val('');
    }
    if ($('#robot_' + type).prop('checked')) {
        $('#robot_' + type).prop('checked', false);
    }
    if ($('#manual_' + type).prop('checked')) {
        $('#manual_' + type).prop('checked', false);
    }
    if ($('#phone_' + type).prop('checked')) {
        $('#phone_' + type).prop('checked', false);
    }
    if (type=='failedOutDealed'){
        if ($('#shooter_' + type).val()) {
            $('#shooter_' + type).val('');
        }
    }
    if (type=='successOut') {
        $('#bankDraw_successOut').prop('checked','');$('#thirdDraw_successOut').prop('checked','');$('#daifuDraw_successOut').prop('checked','');
    }
    if (type == 'QuickQuery') {
        //_searchQuickQuery();
    } else {
        _search();
        //initialSearchTypeToal(type);
    }
}

//分配之前--获取开户行信息 type 1 表示未出款页签 分配 2表示主管处理页签分配
// type=3 表示 待排查的时候重新生成任务，指定该任务出款方式
function _distribution(id, type) {
    $('#distributionTaskId').val(id);
    $('#operatePage').val(type);
    var opt = '';
    $(bank_name_list).each(function (i, bankType) {
        opt += '<option>' + bankType + '</option>';
    });
    $('#form-field-select-allocateTask1').empty().html(opt);
    _initialMultiSelect();
    $('#allocateTaskRemark').val('');
    $('input[name="distributeObject"]:radio').prop('checked', false);
    if(type==3){
        $('#modal-distribution-header').text('重新生成并分配任务');
    }else{
        $('#modal-distribution-header').text('任务分配');
    }
    $('#modal-distribution').modal('show');
}

function _checkClickLabel(obj) {
    var checkedFlag = $(obj).find('input').prop('checked');
    if (checkedFlag) {
        $(obj).find('input').prop('checked', false);
    } else {
        $(obj).find('input').prop('checked', true);
    }
}
//分配操作 1 未出款页签的分配 2 主管处理页签的分配 3 表示待排查的重新生成任务
function _distributionTask() {
    var type = $('#operatePage').val();
    var taskId = $('#distributionTaskId').val();
    var bankTypes = $('#form-field-select-allocateTask1').val();
    var remark = $('#allocateTaskRemark').val();
    var distributeObject = '';
    $('input[name="distributeObject"]').each(function () {
        if ($(this).is(":checked")) {
            distributeObject = $(this).val();
        }
    });
    if(type!='3'){
        if (!distributeObject) {
            $('#noDistributionObj').show(10).delay(1000).hide(10);
            return;
        } else {
            $('#noDistributionObj').hide();
        }
    }
    var url = '/r/outtask/reallocateTask';
    if(type=='3'){
        url= '/r/outtask/recreate';
    }
    $.ajax({
        type: 'post',
        url:url ,
        data: {'taskId': taskId, 'type': distributeObject, "bankType": bankTypes.toString(), "remark": remark},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                if (res.status == 1) {
                    $('#modal-distribution').modal('hide');
                    $('#distributionTaskId').val('');
                    if (type == '2') {
                        initialSearchTypeToal('masterOut');
                    }
                    if (type == '1') {
                        initialSearchTypeToal('toOutDraw');
                    }
                    if (type=='3'){
                        initialSearchTypeToal('failedOut');
                    }
                }
            }
        }
    });
}

function _checkObj(obj) {
    var ob = '';
    if (obj) {
        ob = obj;
    }
    return ob;
}
$('input[name="input-radio"]').click(function () {
    _checkedChange(this);
});
function _checkedChange(obj) {
    var radio = obj;
    if (radio.tag == 1) {
        radio.checked = false;
        radio.tag = 0;
    }
    else {
        radio.checked = true;
        radio.tag = 1
    }
}
//拆单明细
function _showDetail(id) {
    $.ajax({
        type: 'get',
        url: '/r/outtask/getById',
        data: {"id": id, "type": "detail"},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $('#detail_orderNo').text(res.data.orderNo);
                    $('#detail_handicap').text(res.data.handicap);
                    $('#detail_member').text(res.data.member);
                    $('#detail_type').text(res.data.type);
                    $('#detail_asignTime').text(timeStamp2yyyyMMddHHmmss(res.data.asignTime));
                    if (res.data.detailList && res.data.detailList.length > 1) {
                        var tr = '';
                        var amount = 0;
                        var count = 0;
                        $(res.data.detailList).each(function (i, val) {
                            count++;
                            amount += val.amount;
                            tr += '<tr><td>' + (i + 1) + '</td>' +
                                '<td>' + val.id + '</td>' +
                                '<td>' + _checkObj(val.amount) + '</td>' +
                                '<td>' + _checkObj(val.timeCosuming) + '</td>' +
                                '<td>' + _checkObj(val.fromAccount) + '</td>';
                            if (val.status == 1) {
                                tr += '<td><span class="label label-sm label-success">' + _status(val.status) + '</span></td>';
                            } else if (val.status == 0) {
                                tr += '<td><span class="label label-sm label-warning">' + _status(val.status) + '</span></td>';
                            } else if (val.status == 2) {
                                tr += '<td><span class="label label-sm label-inverse">' + _status(val.status) + '</span></td>';
                            } else {
                                tr += '<td><span class="label label-sm label-purple">' + _status(val.status) + '</span></td>';
                            }
                            tr += '<td>' + _checkObj(val.operator) + '</td><td>' + _checkObj(val.remark) + '</td>';
                            if (val.status != 5) {
                                if (val.status == 1) {
                                    tr += '<td><button type="button" class="btn btn-xs btn-white btn-info btn-bold">' +
                                        '<i class="ace-icon fa fa-check bigger-100 green"></i>重新出款 </button></td>'
                                } else {
                                    tr += '<td><button type="button" class="btn btn-xs btn-white btn-info btn-bold">' +
                                        '<i class="ace-icon fa fa-share  bigger-100 orange"></i>转出 </button></td>'
                                }
                            }

                            tr += '</tr>';
                        });
                        $('#detail_body').empty().html(tr);
                        var trs = '<tr><td colspan="2">合计：' + count + ' 条记录</td>' +
                            '<td bgcolor="#579EC8" style="color:white;">合计：' + amount + '</td><td colspan="6"></td></tr>';
                        $('#detail_body').append(trs);
                    } else {
                        $('#detail_body').empty().html('<tr><td style="text-align: center" colspan="9"><h3>无拆单</h3></td></tr>');
                    }
                }
                else {
                    $('#detail_table').empty().html('<div style="text-align: center"><h3>查询不到数据,请稍后...</h3></div>')
                }
                $('#modal-table-detail').modal('show');
            }
        }
    });
}
function _cancelOut() {
    $('#remark').val('');
    $('#remark').prop('readonly', '').prop('style', '');
    if ($('#remark').prop('readonly')) {
        $('#remark').removeAttr('readonly');
    }
    if ($('#remark').prop('style')) {
        $('#remark').removeAttr('style');
    }
    $('#modal-table-toOut #successFlagBankName').hide();
    $('#modal-table-toOut #successFlagToAccountName').hide();
    $('#modal-table-toOut #successFlagOwner').hide();
    $('#modal-table-toOut #successFlagAccountNo').hide();
    $('#modal-table-toOut #successFlagAmount').hide();
}
//取消  拒绝
function _beforeFeedBack(type, id, outwardRequest) {
    searchtype = type;
    searchid = id;
    searchoutwardRequest = outwardRequest;
    $('#feedback_taskId').val(id);
    $('#button_type').val(type);
    $('#feedback_remark').val('');
    if (type == 1) {
        if ($.session.get('drawOutType') != 'masterOut') {
            $.session.set('drawOutType', 'masterOut');
        }
        _checkAccountToDrawInSession();
    }
    if (type == 4) {
        $('#titleDiv').empty().append(
            '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">' +
            '<span class="white">&times;</span></button>任务拒绝');
    }
    if (type == 3) {
        $('#titleDiv').empty().append(
            '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">' +
            '<span class="white">&times;</span></button>任务取消');
    }
    if (type == 8) {
        $('#titleDiv').empty().append(
            '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">' +
            '<span class="white">&times;</span></button>银行维护');
    }
    $('#modal-table-toOut #successFlagBankName').hide();
    $('#modal-table-toOut #successFlagToAccountName').hide();
    $('#modal-table-toOut #successFlagOwner').hide();
    $('#modal-table-toOut #successFlagAccountNo').hide();
    $('#modal-table-toOut #successFlagAmount').hide();
    $('#remark').val('').prop('readonly', '').prop('style', '');
    //初始化模态框
    var flagM = false;
    $.ajax({
        type: 'get',
        url: '/r/outtask/getById',
        data: {"id": id, "outRequestId": outwardRequest, "type": null},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    flagM = true;
                    if (type != 1) {
                        $('#feedback_orderNo').text(res.data.orderNo);
                        $('#feedback_handicap').text(res.data.handicap);
                        $('#feedback_level').text(res.data.level);
                        $('#feedback_member').text(res.data.member);
                        $('#feedback_type').text(res.data.type);
                        $('#feedback_asignTime').text(timeStamp2yyyyMMddHHmmss(res.data.asignTime));
                        $('#feedback_amount').val(res.data.amount);

                        $('#feedback_userId').val(_checkObj(res.data.userId));
                        $('#feedback_taskId').val(_checkObj(res.data.taskId));
                        $('#feedback_orderNoInput').val(_checkObj(res.data.orderNo));
                        $('#feedback_memberCode').val(_checkObj(res.data.memberCode));
                    }
                    if (type == 1) {
                        $('#toOut_orderNo').text(res.data.orderNo);
                        $('#toOut_handicap').text(res.data.handicap);
                        $('#toOut_level').text(res.data.level);
                        $('#toOut_member').text(res.data.member);
                        $('#toOut_type').text(res.data.type);
                        $('#toOut_asignTime').text(timeStamp2yyyyMMddHHmmss(res.data.asignTime));

                        $('#toOut_userId').val(_checkObj(res.data.userId));
                        $('#toOut_taskId').val(_checkObj(res.data.taskId));
                        $('#toOut_orderNoInput').val(_checkObj(res.data.orderNo));
                        $('#toOut_memberCode').val(_checkObj(res.data.memberCode));

                        $('#toOut_amount').text(res.data.amount);
                        $('#toOut_owner').text(res.data.toAccountOwner);
                        $('#toOut_bankName').text(res.data.toAccountBank);
                        $('#toOut_toAccountName').text(res.data.toAccountName);
                        $('#toOut_accountNo').text(res.data.toAccountNo);
                    }
                }
            }
        }
    });
    //主管 出款
    if (type == 1) {
        _checkAccountToDrawInSession();//检查session中的出款账号

        $('#modal-table-toOut').modal('show');
    }
    else {
        $('#toAccount_error').prop('checked', '');
        $('#modal-table-feedback').modal('show');
    }
}
// 取消  拒绝
function _sureButton() {
    var taskId = $('#feedback_taskId').val();
    var remark = $('#feedback_remark').val();
    var type = $('#button_type').val();
    if (!taskId) {
        bootbox.alert("taskId参数丢失,请联系技术");
        return;
    }
    if (!remark) {
        $('#prompt').show(10).delay(1000).hide(10);
        return;
    }
    var flag = '';
    if (type == 3) {
        flag = "确定要取消吗？";
    }
    if (type == 4) {
        flag = "确定要拒绝吗？";
    }
    var accountError = '';
    if ($('#toAccount_error').prop('checked')) {
        accountError = $('#toAccount_error').val();
    }
    bootbox.confirm(flag, function (res) {
        if (res && type) {
            $.ajax({
                type: 'post',
                url: '/r/outtask/status',
                data: {
                    "taskId": taskId, "remark": remark, "status": type, "accountError": accountError
                },
                dataType: 'json',
                async: false,
                success: function (res) {
                    if (res) {
                        if (res.status == 1) {
                            $.gritter.add({
                                time: '',
                                class_name: '',
                                title: '系统消息',
                                text: res.message,
                                sticky: false,
                                image: '../images/message.png'
                            });
                            $('#modal-table-feedback').modal('hide');
                            $('#feedback_remark').val('');
                            initialSearchTypeToal(searchTypeTaskTotal);
                        }
                    }
                }
            });
        }
    });
}

$('#cancelModal').unbind().bind('click', function () {
    //$('#out-account-prompt').text('').hide();
    $('#modal-fromAccountInfo').modal('hide');
});

function _selectAccountByClickRadio(obj) {
    //选中按钮切换
    $('input[name=outAccount]').get(obj).checked = true;
}

//确定账号
$('#confirmAccoutToDraw').unbind().bind('click', function () {
    _confirmAccountToOut();
});
/**
 * 确定出款账号 点击
 * @private
 */
function _confirmAccountToOut() {

    $.session.set('#drawOutType', 'masterOut');
    var outType = $.session.get('#drawOutType');
    if (!$('#modal-fromAccountInfo-master-tbody')) {
        $('#out-account-prompt1').text('目前没有可用的账号，请联系主管！').show(10).delay(4000).hide(10);
        return false;
    }
    if ($('#modal-fromAccountInfo-master-tbody input[name="outAccount"]:checked').val()) {

        var trs = $('input[name="outAccount"]:checked').parent().parent();

        $.session.set('accountToDraw_' + outType, $(trs).find('td[id="accountOutTotal"]').text());
        $.session.set('accountToDrawAlias_' + outType, $(trs).find('td[id="accountOutTotalAlias"]').text());
        //$.session.set('accountToDrawLevelName_'+outType,$(trs).find('td[id="currSysLevelNameTotal"]').text());
        $.session.set('accountToDrawId_' + outType, $('input[name="outAccount"]:checked').val());
        //$.session.set('accountToDrawBankName_'+outType,$(trs).find('td[id="bankNameOutTotal"]').text());
        $.session.set('accountToDrawBalance_' + outType, $(trs).find('td[id="balanceOutTotal"]').text());
        $.session.set('accountToLimit_' + outType, _checkObj($(trs).find('td[id="limitOutTotal"]').text()));
        $.session.set('outwardAmountDailyTotal', _checkObj($(trs).find('td[id="outwardAmountDailyTotal"]').text()) == '' ? 0 : _checkObj($(trs).find('td[id="outwardAmountDailyTotal"]').text()));
        //复制选择的账号信息
        //var fromAccountInfo = '账号：'+$.session.get('accountToDraw_'+outType)+'   层次：'+$.session.get('accountToDrawLevelName_'+outType)+ '   开户行：'+$.session.get('accountToDrawBankName_'+outType)+'  系统余额：'+$.session.get('accountToDrawBalance_'+outType)+
        //'    当日出款额度： '+_checkObj($.session.get('accountToLimit_'+outType))+'   当日累计出款：'+$.session.get('outwardAmountDailyTotal');
        var fromAccountInfo = '账号：' + $.session.get('accountToDraw_' + outType) + '&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;  编号：<label id="totalAliasLabel" style="font-size:30 px;color: mediumvioletred">' + $.session.get('accountToDrawAlias_' + outType) +
            '</label>&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;    系统余额：' + parseFloat($.session.get('accountToDrawBalance_' + outType)).toFixed(3);
        if (outType == 'toOut_operate') {
            $('#Account_toOut_Info').val(fromAccountInfo);
        }
        if (outType == 'masterOut') {
            $('#fromAccountInfoMaster').empty().html(fromAccountInfo);
        }
        $('#modal-fromAccountInfo').modal('hide');
    }
    else {
        $('#out-account-prompt').text('请选择出款账号！').show(10).delay(1000).hide(10);
        return false;
    }

}
function _checkAccountToDraw(accountToDraw) {
    //根据输入的账号查询数据库是否存在该银行账号并且状态是在用而且不是锁定的
    $.ajax({
        type: 'get',
        url: '/r/account/getByAccount',
        data: {"account": $.trim(accountToDraw)},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data && res.data.length > 0) {
                    var tr = '';
                    $.each(res.data, function (i, val) {
                        var outType = $.session.get('#drawOutType');
                        if ($.session.get('accountToDrawId_' + outType)) {
                            if ($.session.get('accountToDrawId' + outType) == val.id) {
                                tr += '<tr style="background-color: lawngreen"><td ><input type="radio" checked="checked"  name="outAccount" value="' + val.id + '"></td>';
                            }
                            else {
                                tr += '<tr><td ><input type="radio"  name="outAccount" value="' + val.id + '"></td>';
                            }
                        } else {
                            tr += '<tr><td ><input type="radio"  name="outAccount" value="' + val.id + '"></td>';
                        }
                        tr +=
                            '<td id="accountOutTotal">' + val.account + '</td>' +
                            '<td id="currSysLevelNameTotal">' + val.currSysLevelName + '</td>' +
                            '<td >' + _showAccountStatus(val.status) + '</td>' +
                            '<td id="bankNameOutTotal">' + val.bankName + '</td>' +
                            '<td >' + val.owner + '</td>' +
                            '<td >' + _checkObj(val.bankBalance).toFixed(3) + '</td>' +
                            '<td id="balanceOutTotal">' + val.balance + '</td>' +
                            '<td id="limitOutTotal">' + _checkObj(val.limitOut) + '</td>' +
                            '<td id="outwardAmountDailyTotal">' + _checkObj(val.outwardAmountDaily) + '</td>' +
                            '</tr>';

                    });
                    $('#modal-fromAccountInfo-master-tbody').empty().html(tr);
                    $('#out-account-prompt').hide();
                }
                else {
                    $('#modal-fromAccountInfo-master-tbody').empty().html('<tr><td colspan="14" style="text-align: center;"><h3>查不到账号</h3></td></tr>');
                    //$('#out-account-prompt').text('账号不存在，重新输入').show(10).delay(500).hide(10);
                    // if (accountToDraw) {
                    //     $('#out-account-prompt').text('账号不存在，重新输入').show(10).delay(500).hide(10);
                    // } else {
                    //     $('#out-account-prompt').text('').hide();
                    // }
                }
            }
        }
    });
}
function _findAccountToDraw(accountToDraw) {
    var flag = false;
    $.ajax({
        type: 'get',
        url: '/r/account/getByAccount',
        data: {"account": accountToDraw},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var outType = $.session.get('#drawOutType');
                    if (res.data.length > 0 && $.session.get('accountToDrawId_' + outType) && $.session.get('accountToDrawId_' + outType) == res.data[0].id && res.res.data[0].status == accountStatusNormal) {
                        $.session.set('accountToDrawId_' + outType, res.data[0].id);
                        flag = false;
                        $.session.set('accountToDraw_' + outType, res.data[0].account);
                        $.session.set('accountToDrawAlias_' + outType, res.data[0].alias);
                        // if ($.session.get('accountToDrawLevelName_'+outType)) {
                        //     $.session.remove('accountToDrawLevelName_'+outType);
                        //     $.session.set('accountToDrawLevelName_'+outType, res.data[0].currSysLevelName);
                        // }
                        //$.session.set('accountToDrawBankName_'+outType, res.data[0].bankName);
                        $.session.set('accountToDrawBalance_' + outType, res.data[0].balance);

                        //val.limitOutval.outwardAmountDaily
                        $.session.set('accountToLimit_' + outType, res.data[0].limitOut);
                        $.session.set('outwardAmountDailyTotal', res.data[0].outwardAmountDaily);
                        //复制选择的账号信息
                        //var fromAccountInfo = '账号：' + $.session.get('accountToDraw_'+outType) +'    层次：'+$.session.get('accountToDrawLevelName_'+outType)+ '      开户行：' + $.session.get('accountToDrawBankName_'+outType) + '      系统余额：' + $.session.get('accountToDrawBalance_'+outType)+
                        //   '    当日出款额度： '+_checkObj($.session.get('accountToLimit_'+outType))+'   当日累计出款：'+$.session.get('outwardAmountDailyTotal');
                        var fromAccountInfo = '账号：' + $.session.get('accountToDraw_' + outType) + '&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;  编号：<label id="totalAliasLabel" style="font-size:30 px;color: mediumvioletred">' + $.session.get('accountToDrawAlias_' + outType) +
                            '</label>&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp;    系统余额：' + parseFloat($.session.get('accountToDrawBalance_' + outType)).toFixed(3);
                        $('#fromAccountInfoMaster').empty().html(fromAccountInfo);
                        $('#modal-fromAccountInfo').modal('hide');
                    }
                    else {
                        $('#selectAccoutPrompt_masterOut').text('账户异常重新选择').show(10).delay(1000).hide(10);
                        flag = true;
                        $.session.remove('accountToDrawId_' + outType);
                        $.session.remove('accountToDraw_' + outType);
                        $.session.remove('accountToDrawAlias_' + outType);
                        $.session.remove('accountToDrawBalance_' + outType);
                        $.session.remove('accountToLimit_' + outType);
                        $.session.remove('outwardAmountDailyTotal' + outType);
                        $('#fromAccountInfoMaster').empty().html('');
                    }

                } else {
                    $('#selectAccoutPrompt_masterOut').text('账户异常重新选择').show(10).delay(1000).hide(10);
                    flag = true;
                }
            }
        }
    });
    return flag;
}
//防止修改数据库账户余额后刷新页等，更新
function _checkAccountToDrawInSession() {
    var outType = $.session.get('drawOutType');
    if ($.session.get('accountToDraw_' + outType)) {
        var accountToDraw = $.session.get('accountToDraw_' + outType);
        if (accountToDraw) {
            _findAccountToDraw(accountToDraw);
        } else {
            $('#fromAccountInfoMaster').html('');
        }
    }
}

//转帐完成按钮
function _transferDoneTotal() {
    var outType = $.session.get('drawOutType');
    var flag = false;
    if ($.session.get('accountToDraw_' + outType)) {
        flag = _findAccountToDraw($.session.get('accountToDraw_' + outType));//防止人工修改数据库余额
        if (flag) {
            return false;
        }
    }
    var fromAccountInfo = '';
    if (outType == 'toOut_operate') {
        fromAccountInfo = $('#Account_toOut_Info').val();
    }
    if (outType == 'masterOut') {
        fromAccountInfo = $('#fromAccountInfoMaster').html();
    }
    var fromAccountId = $.session.get('accountToDrawId_' + outType);
    var userId = '';
    if (outType == 'toOut_operate') {
        userId = $('#toOut_operate_userId').val();
    }
    if (outType == 'masterOut') {
        userId = $('#toOut_userId').val();
    }
    var taskId = '';
    if (outType == 'toOut_operate') {
        taskId = $('#toOut_operate_taskId').val();
    }
    if (outType == 'masterOut') {
        taskId = $('#toOut_taskId').val();
    }
    if (!fromAccountInfo || !fromAccountId) {
        $('#selectAccoutPrompt_masterOut').text('请选择出款账号').show(10).delay(1000).hide(10);
        return false;
    }
    var remark = '';
    // if (outType=='toOut_operate') {
    //     $('#remark_toOut').val();
    //     remark = $('#remark_toOut').val();
    // }
    var amountToDraw = '';//$('#toOut_amount').text();
    if (outType == 'toOut_operate') {
        amountToDraw = $('#amount').text();
    }
    if (outType == 'masterOut') {
        amountToDraw = $('#toOut_amount').text();
    }
    var accountToDrawBalance = $.session.get('accountToDrawBalance_' + outType);
    if (accountToDrawBalance) {
        if (parseFloat(accountToDrawBalance) < Number(amountToDraw)) {
            $('#selectAccoutPrompt_masterOut').text('账户余额不足').show(10).delay(1000).hide(10);
            return false;
        }
    }
    if ($.session.get('accountToLimit_' + outType) && parseFloat(amountToDraw) > parseFloat($.session.get('accountToLimit_' + outType))) {
        $('#selectAccoutPrompt_' + outType).text('出款金额不能大于当日出款额度').show(10).delay(1000).hide(10);
        return false;
    }
    if ($.session.get('outwardAmountDailyTotal') && (parseFloat(amountToDraw) + parseFloat($.session.get('outwardAmountDailyTotal'))) > parseFloat($.session.get('accountToLimit_' + outType))) {
        $('#selectAccoutPrompt_' + outType).text('已超出当日累计出款，请重新选择账号！').show(10).delay(1000).hide(10);
        return false;
    }
    bootbox.confirm('确定出款吗？', function (res) {
        if (res) {
            $.ajax({
                type: 'post',
                url: '/r/outtask/transfer',
                data: {"fromAccountId": fromAccountId, "userId": userId, "taskId": taskId},
                dataType: 'json',
                async: false,
                success: function (res) {
                    if (res) {
                        $('#modal-table-toOut').modal('hide');
                        $('#modal-table-toOut #successFlagBankName').hide();
                        $('#modal-table-toOut #successFlagToAccountName').hide();
                        $('#modal-table-toOut #successFlagOwner').hide();
                        $('#modal-table-toOut #successFlagAccountNo').hide();
                        $('#modal-table-toOut #successFlagAmount').hide();
                        if (outType == 'toOut_operate') {
                            $('#remark_toOut').val('');
                        }
                        if (outType == 'masterOut') {
                            $('#remark').val('');
                            if ($('#remark').prop('readonly')) {
                                $('#remark').removeAttr('readonly');
                            }
                            if ($('#remark').prop('style')) {
                                $('#remark').removeAttr('style');
                            }
                            initialSearchTypeToal('masterOut');
                            _checkAccountToDrawInSession();
                        }
                        $.gritter.add({
                            time: 500,
                            class_name: '',
                            title: '系统消息',
                            text: res.message,
                            sticky: false,
                            image: '../images/message.png'
                        });
                    }
                    else {
                        $.gritter.add({
                            time: 500,
                            class_name: '',
                            title: '系统消息',
                            text: '请稍后....',
                            sticky: false,
                            image: '../images/message.png'
                        });
                        return false;
                    }
                }
            });
        }
    });
}
//主管出款 完成出款
$('#transferDoneTotal').click(function () {
    //_transferDoneTotal();
});
//未出款 完成出款 暂时作废
$('#transfer_done').click(function () {
    //_transferDoneTotal();
});

function _show(obj) {
    var ob = '';
    if (obj)
        ob = obj.toFixed(3) + '分钟';
    return ob;
}
function _status(statu) {
    var ob = '';
    var Num = Number(statu);
    var status = Num.valueOf(statu);
    if (status == 0) {
        ob = '未出款';
    } else {
        switch (status) {
            case 1:
                ob = '出款完成';
                break;
            case 2:
                ob = '与流水匹配成功';
                break;
            case 3:
                ob = '未与流水匹配';
                break;
            case 4:
                ob = '正在出款';
                break;
        }
    }
    return ob;
}
/**
 * 获取盘口信息
 */
function _gethandicap(type) {
    $.ajax({
        type: 'get',
        url: '/r/out/handicap',
        data: {},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var opt = '<option value="请选择">请选择</option>';
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + $.trim(val.code) + '">' + val.name + '</option>';
                    });
                    $('#handicap_' + type).empty().html(opt);
                    $('#handicap_' + type).trigger('chosen:updated');
                    // if(type!='outDrawing'){
                    //     _handicapTypeChange(type);
                    // }
                }
            }
        }
    });
}
/**
 * 初始化层级
 * @param type
 * @private
 */
function _getLevel(type) {
    $('#level_' + type).empty();
    var opt = '<option value="请选择">请选择</option>';
    if(type=='outDrawing'){
        //_initiaiHandicaps(type);
    }else{
        if ($('#handicap_' + type).val() != '请选择') {
            var handicap = $('#handicap_' + type).val();
            $.ajax({
                type: 'get',
                url: '/r/level/getByHandicap',
                data: {"handicap": handicap},
                async:false,
                dataType: 'json',
                success: function (res) {
                    if (res) {
                        if (res.status == 1 && res.data) {
                            $(res.data).each(function (i, val) {
                                opt += '<option value="' + $.trim(val) + '">' + val + '</option>';
                            });
                        }
                        $('#level_' + type).empty().html(opt);
                        $('#level_' + type).trigger('chosen:updated');
                    }
                }
            });
        } else {
            $('#level_' + type).empty().html(opt);
            $('#level_' + type).trigger('chosen:updated');
        }
    }
}
/**盘口事件*/
function _handicapTypeChange() {
    var type = searchTypeTaskTotal;
    if (type=='outDrawing'|| type=='toOutDraw' || type=='masterOut' || type=='failedOut'){

    }else{
        $('#level_' + type).empty();
        if ($('#handicap_' + type).val() && $('#handicap_' + type).val() != '请选择') {
            _getLevel(type);
        } else {
            $('#level_' + type).empty().html('<option>请选择</option>');
            $('#level_' + type).trigger('chosen:updated');
        }
    }
    _search();
}
/**
 * 初始化账号
 * @param url
 * @param type
 * @param levelId
 * @private
 */
function _initalAccountOut(type) {
    $.ajax({
        type: 'get',
        url: '/r/account/getAllOutAccount',
        data: {},
        dataType: 'json',
        success: function (res) {
            if (res) {
                var opt = '<option selected="selected" value="请选择">请选择</option>';
                if (res.status == 1 && res.data) {
                    $(res.data).each(function (i, val) {
                        if (val.type == 5 && val.alias) {
                            opt += '<option value="' + val.alias + '" >' + val.alias + '</option>';
                        }
                    });
                }
                $('#dataList_account_' + type).empty().html(opt);
                //$('#dataList_account_'+type).trigger('chosen:updated');
            }
        }
    });
}
function _initiaiHandicaps(type) {
    _initialSelectChosen(type);
}
function _initialSelectChosen(type) {
    //可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        //allow_single_deselect:true,
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    if (handicapAndLevelInitialOptions) {
        $('#handicap_' + type).empty().html(handicapAndLevelInitialOptions[0]);
        $('#handicap_' + type).trigger('chosen:updated');
        if (type!='successOut') {
            $('#level_' + type).empty().html(handicapAndLevelInitialOptions[1]);
        }else {
            $('#level_' + type).empty().html('<option>请选择</option>');
        }
        $('#level_' + type).trigger('chosen:updated');
    }
    if (type=='outDrawing' || type=='toOutDraw') {
        $('#handicap_' + type + '_chosen').prop('style', 'width: 30%;');
    }else{
        $('#handicap_' + type + '_chosen').prop('style', 'width: 78%;');
    }
    $('#level_' + type + '_chosen').prop('style', 'width: 78%;');
    //_gethandicap(type);
    //|| type=='toOutDraw'
    if(type=='outDrawing'|| type=='masterOut' || type=='failedOut'){
        var opt1 ='<option>请选择</option>';
        $.each(bank_name_list,function (i,val) {
            opt1 +='<option value="'+val+'">'+val+'</option>';
        });
        $('#bank_'+type).empty().html(opt1);
        $('#bank_'+type).trigger('chosen:updated');
        if( type=='masterOut' || type=='failedOut'){
           // $('#handicap_' + type + '_chosen').prop('style', 'width: 78%;');
            $('#bank_'+type+ '_chosen').prop('style', 'width: 68%;');
        }else{
            if(type=='toOutDraw'){
                //$('#bank_'+type+ '_chosen').prop('style', 'width: 78%;');
                //$('#handicap_' + type + '_chosen').prop('style', 'width: 78%;');
            }else{
                $('#bank_'+type+ '_chosen').prop('style', 'width: 30%;');
                //$('#handicap_' + type + '_chosen').prop('style', 'width: 30%;');
            }

        }
    }else{
        //$('#handicap_' + type + '_chosen').prop('style', 'width: 78%;');
        //$('#level_' + type + '_chosen').prop('style', 'width: 78%;');
        $('#account_' + type + '_chosen').prop('style', 'width: 68%;');
    }

}

function _radioClick(obj) {
    var checked_val = $(obj).attr('checked_val');
    var checked_id = $(obj).attr('id');
    var checked_name = $(obj).attr('name');
    if(checked_id&&checked_id=='maintain_toOutDraw'){
        if(checked_val==1){
            $(obj).attr('checked_val',2);$(obj).prop('checked','checked');
        }else{
            $(obj).attr('checked_val',1);$(obj).prop('checked','');
        }
    }else if(checked_name&&(checked_name=='sysLevel_outDrawing'||checked_name=='sysLevel_toOutDraw'|| checked_name=='sysLevel_successOut')){
        if(checked_val==1){
            var currentVal = $(obj).val();
            $('input[name="sysLevel_'+searchTypeTaskTotal +'"]').each(function (i) {
                if(this.value!=currentVal){
                    $(this).attr('checked_val',1);$(this).prop('checked','');
                }
            });
            $(obj).attr('checked_val',2);$(obj).prop('checked','checked');
        }else{
            $(obj).attr('checked_val',1);$(obj).prop('checked','');
        }
    }else{
        if(checked_val==1){
            var currentVal = $(obj).val();
            $('input[name="level_'+searchTypeTaskTotal +'"]').each(function (i) {
                if(this.value!=currentVal){
                    $(this).attr('checked_val',1);$(this).prop('checked','');
                }
            });
            $(obj).attr('checked_val',2);$(obj).prop('checked','checked');
        }else{
            $(obj).attr('checked_val',1);$(obj).prop('checked','');
        }
    }
    _search();
}

//添加备注
function SCustomerserviceRemark(id, third) {
    if (!third) {
        $('#CustomerserviceRemark').val('');
        $('#CustomerserviceRemark_id').val(id);
        $('#CustomerserviceRemark_modal').modal('show');
        $('#totalTaskFinishBTN').attr('onclick', 'save_CustomerserviceRemark();');
        $('#thirdHandiacp').hide();
        $('#thirdAccountName').hide();
        $('#thirdOutAmount').hide();
        $('#reOutForThird').hide();
        $('#normalRemarkForThird').hide();
    } else {
        //第三方出款备注
        $('input[name="distributeObject"]').each(function () {
            $(this).prop('checked', '');
        });
        $('#thirdHandiacp').show();
        $('#thirdAccountName').show();
        $('#thirdOutAmount').show();
        $('#CustomerserviceRemark').val('');
        $('#CustomerserviceRemark_id').val(id);
        $('#reOutForThird').show();
        $('#normalRemarkForThird').show();
        $('#CustomerserviceRemark_modal').modal('show');
        $('#totalTaskFinishBTN').attr('onclick', '_saveRemarkForThird();');
    }
}
//保存第三方备注
function _saveRemarkForThird() {
    var taskId = $('#CustomerserviceRemark_id').val();
    var remarkTpe = $('input[name="distributeObject"]:checked').val();
    var remark = '';
    if (!remarkTpe) {
        $('#prompt_remark').text('请选择第三方出款的备注类型').show(10).delay(1500).hide(10);
        return;
    }
    remark = $('#CustomerserviceRemark').val();
    if (remarkTpe == 'normalRemark') {
        if (!remark) {
            $('#prompt_remark').text('请填写备注!').show(10).delay(1500).hide(10);
            return;
        }
    }
    var handicapName = '', accountName = '', amount = '';
    if (remarkTpe == 'reThirdOut') {
        handicapName = $.trim($('#thirdHandiacpValue').val());
        accountName = $.trim($('#thirdAccountNameValue').val());
        amount = $.trim($('#thirdOutAmountValue').val());
        if (!handicapName || !accountName || !amount) {
            $('#prompt_remark').text('重新出款,请填写相关信息!').show(10).delay(1500).hide(10);
            return;
        }
        if (!remark) {
            $('#prompt_remark').text('请填写备注!').show(10).delay(1500).hide(10);
            return;
        }
        remark = '{盘口:' + handicapName + ',商号:' + accountName + ',金额:' + amount + ',(备注:)' + remark + '(第三方出款)}';
    }

    $.ajax({
        type: 'post',
        url: '/r/outtask/remark',
        data: {"taskId": taskId, "remark": remark},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 1000,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            $('#CustomerserviceRemark_id').val('');
            $('#CustomerserviceRemark_modal').modal('hide');
            initialSearchTypeToal(searchTypeTaskTotal);
        }
    });
}

function artificial(toAccountNo) {
    bootbox.confirm('确定设置账号：<span style="color:red;">' + _ellipsisAccount(toAccountNo) + '</span>为人工出款吗？', function (res) {
        if (res) {
            $.ajax({
                type: 'post',
                url: '/r/outtask/artificialoutmoney',
                data: {"toaccount": toAccountNo},
                dataType: 'json',
                success: function (res) {
                    if (res) {
                        $.gritter.add({
                            time: 300,
                            class_name: '',
                            title: '系统消息',
                            text: res.message,
                            sticky: false,
                            image: '../images/message.png'
                        });
                    }
                    initialSearchTypeToal(searchTypeTaskTotal);
                }
            });
        }
    });
}

function cancelArtificial(toAccountNo) {
    bootbox.confirm('确定取消账号：<span style="color:red;">' +  _ellipsisAccount(toAccountNo) + '</span>人工出款吗？', function (res) {
        if (res) {
            $.ajax({
                type: 'post',
                url: '/r/outtask/cancelartificial',
                data: {"toaccount": toAccountNo},
                dataType: 'json',
                success: function (res) {
                    if (res) {
                        $.gritter.add({
                            time: 300,
                            class_name: '',
                            title: '系统消息',
                            text: res.message,
                            sticky: false,
                            image: '../images/message.png'
                        });
                    }
                    initialSearchTypeToal(searchTypeTaskTotal);
                }
            });
        }
    });
}

function save_CustomerserviceRemark() {
    var remark = $.trim($('#CustomerserviceRemark').val());
    if (!remark) {
        $('#prompt_remark').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    var taskId = $('#CustomerserviceRemark_id').val();
    $.ajax({
        type: 'post',
        url: '/r/outtask/remark',
        data: {"taskId": taskId, "remark": remark},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            $('#CustomerserviceRemark_modal').modal('hide');
            initialSearchTypeToal(searchTypeTaskTotal);
        }
    });
}
var _beforeTotalTurntoFinished = function (id) {
    $('#CustomerserviceRemark').val('');
    $('#CustomerserviceRemark_id').val(id);
    $('#CustomerserviceRemark_modal').modal('show');
    $('#totalTaskFinishBTN').attr('onclick', '_exectuTransferFromFailTotal();');
};
//执行转账
function _exectuTransferFromFailTotal() {
    if (!$('#CustomerserviceRemark').val()) {
        $('#prompt_remark').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    var type = searchTypeTaskTotal;
    if (type == 'failedOut') {
        type = 'failedOutToMatched';
    }
    if (type == 'masterOut') {
        type = 'masterOutToMatched';
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/finsh',
        data: {
            "type": type,
            "taskId": $('#CustomerserviceRemark_id').val(),
            "remark": $('#CustomerserviceRemark').val()
        },
        dataType: 'json',
        async: false,
        success: function (res) {
            $.gritter.add({
                time: 2000,
                class_name: '',
                title: '系统消息',
                text: res.message,
                sticky: false,
                image: '../images/message.png'
            });
            initialSearchTypeToal(searchTypeTaskTotal);
            _initalAccountOut(searchTypeTaskTotal);
            $('#totalTaskFinishBTN').attr('onclick', '');
            $('#CustomerserviceRemark_modal').modal('hide');
        }
    });
}
//初始化时间控件
function _dateP() {
    if (searchTypeTaskTotal && searchTypeTaskTotal=='masterOut' ||searchTypeTaskTotal=='failedOut'  ){
        $('#timeScope_'+searchTypeTaskTotal).on("mouseup", function () {
            _datePickerForAll($('#' + searchTypeTaskTotal).find('input.date-range-picker'));
        });
    } else if (searchTypeTaskTotal && searchTypeTaskTotal != 'toOutDraw' && $('#' + searchTypeTaskTotal).find('input.date-range-picker')) {
        _datePickerForAll($('#' + searchTypeTaskTotal).find('input.date-range-picker'));
    }
}
$('#handicap_backwash').change(function () {
    _searchBackWash();
});
//查询冲正
function _searchBackWash() {
    var handicapCode = '';
    if ($('#handicap_backwash').val() && $('#handicap_backwash').val() != '请选择') {
        handicapCode = $.trim($('#handicap_backwash option[value='+$('#handicap_backwash').val()+']').attr('handicap_code'));
    }
    //默认只查询当前人盘口权限下的数据
    if (!handicapCode) {
        handicapCode = [];
        $('#handicap_backwash').find('option:not(:first-child)').each(function () {
            handicapCode.push($(this).attr('handicap_code'));
        })
    }
    var fromAccount = '';
    if ($('#account_backwash').val() && $('#account_backwash').val() != '请选择') {
        fromAccount = $.trim($('#account_backwash').val());
    }
    var orderNo = $.trim($('#orderNo_backwash').val());
    var status = [];
    if ($('#backwash_done').prop('checked')) {
        status.push($('#backwash_done').val());
    }
    if ($('#backwash_todo').prop('checked')) {
        status.push($('#backwash_todo').val());
    }
    if (!status || status.length == 0) {
        status = [6];
    }
    if (!$('#backwash_done').prop('checked') && !$('#backwash_todo').prop('checked')) {
        status = [6, 7];
    }
    var fromMoney = $.trim($('#fromMoney_backwash').val());
    var toMoney = $.trim($('#toMoney_backwash').val());
    var startTime = '';
    var endTime = '';
    var startAndEnd = $('#timeScope_backwash').val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    } else {
        startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
        endTime = moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    }
    var operator = $.trim($('#operator_backwash').val());
    var CurPage = $("#backwash_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var data = {
        "handicap": handicapCode.toString(), "fromAccount": fromAccount, "orderNo": orderNo, "operator": operator,
        "status": status.toString(), "amountStart": fromMoney, "amountEnd": toMoney, "startTime": startTime,
        "endTime": endTime, "pageNo": CurPage, "pageSize": $.session.get('initPageSize')
    };
    var dealFlag = false,addRemark=false,queryDetail=false;
    $.each(ContentRight['OutwardTaskTotal:*'], function (name, value) {
        if (name == 'OutwardTaskTotal:dealBackWash:*') {
            dealFlag = true;
        }
        if (name == 'OutwardTaskTotal:BackWashRemark:*') {
            addRemark = true;
        }
        if (name == 'OutwardTaskTotal:BackWashDetail:*') {
            queryDetail = true;
        }
    });
    $.ajax({
        type: 'get',
        url: '/r/banklog/queryBackWashBankLog',
        dataType: 'json',
        data: data,
        success: function (res) {
            if (res) {
                var tr = '',trs = '', amount = 0,  idListThird=[],idList = [];
                if (res.status == 1) {
                    if (res.data && res.data.length > 0) {
                        $.each(res.data, function (i, val) {
                            amount += val.amount;
                            // idList.push({'id': val.fromAccountId});
                            var third=val.thirdInsteadPay&&val.fromAccountId;
                            if (third){
                                idListThird.push( val.fromAccountId );
                            } else{
                                idList.push({'id': val.fromAccountId});
                            }
                            var tradingTime = '';
                            if (timeStamp2yyyyMMddHHmmss(val.tradingTime)) {
                                tradingTime = timeStamp2yyyyMMddHHmmss(val.tradingTime);
                            }
                            var creatTime = '';
                            if (timeStamp2yyyyMMddHHmmss(val.createTime)) {
                                creatTime = timeStamp2yyyyMMddHHmmss(val.createTime);
                            }
                            var accountInfo = '';
                            if (val.alias) {
                                accountInfo += val.alias;
                            } else {
                                accountInfo += '无';
                            }
                            if (val.bankType) {
                                accountInfo += "|" + val.bankType;
                            } else {
                                accountInfo += '|无';
                            }
                            if (val.accountNo) {
                                accountInfo += "|" + _ellipsisAccount(val.accountNo);
                            } else {
                                accountInfo += '|无';
                            }
                            tr += '<tr><td>' + _checkObj(val.handicap) + '</td>' ;
                            if (third){
                                tr +=     '<td><a class="bind_hover_card" data-toggle="thirdDaiFuHover' + val.fromAccountId + '" data-placement="auto right" data-trigger="hover">' + _checkObj(accountInfo) + '</a></td>' ;
                            } else{
                                tr +=     '<td><a class="bind_hover_card" data-toggle="accountInfoHover' + val.fromAccountId + '" data-placement="auto right" data-trigger="hover">' + _checkObj(accountInfo) + '</a></td>' ;
                            }
                            tr +='<td >'+ _checkObj(tradingTime) + '</td>' +
                                '<td >' +_checkObj(creatTime)+ '</td>' +
                                '<td>' + _checkObj(val.amount) + '</td>' +
                                '<td>' + _checkObj(val.bankBalance) + '</td>';
                            '<td>' + _ellipsisAccount(_checkObj(val.toAccount)) + '</td>';
                            if (_checkObj(val.toAccount)) {
                                tr += '<td ><a class="bind_hover_card breakByWord"  title="对方账号"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + _ellipsisAccount(_checkObj(val.toAccount)) + '">'
                                    + _ellipsisAccount(_checkObj(val.toAccount))
                                    + '</a>'
                                    + '</td>';
                            } else {
                                tr += '<td ></td>';
                            }
                            if (_checkObj(val.toAccountOwner)) {
                                if (_checkObj(val.toAccountOwner).length > 5) {
                                    tr += '<td ><a class="bind_hover_card breakByWord"  title="对方姓名"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.toAccountOwner) + '">'
                                        + _checkObj(val.toAccountOwner).substring(0, 5)
                                        + '</a>'
                                        + '</td>';
                                } else {
                                    tr += '<td ><a class="bind_hover_card breakByWord"  title="对方姓名"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.toAccountOwner) + '">'
                                        + _checkObj(val.toAccountOwner)
                                        + '</a>'
                                        + '</td>';
                                }
                            } else {
                                tr += '<td ></td>';
                            }
                            tr += '<td>' + _checkObj(val.summary) + '</td>';
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td ><a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.remark) + '">'
                                        + _checkObj(val.remark).substring(0, 5)
                                        + '</a>'
                                        + '</td>';
                                } else {
                                    tr += '<td ><a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.remark) + '">'
                                        + _checkObj(val.remark)
                                        + '</a>'
                                        + '</td>';
                                }
                            } else {
                                tr += '<td ></td>';
                            }
                            tr += '<td style="width: 160px">';
                            if (dealFlag) {
                                tr += '<button type="button" class="btn btn-xs btn-white btn-bold btn-success"  onclick="_dealBackwash(' + val.bankLogId + ');"><i class="ace-icon fa fa-check bigger-100 primary"></i>处理</button>';
                            }
                            if(addRemark){
                                tr += '<button type="button" class="btn btn-xs btn-white btn-bold btn-primary"  onclick="_remarkBackWash(' + val.bankLogId + ')"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>备注</span></button>' ;
                            }
                            if(queryDetail){
                                tr += '<button type="button" class="btn btn-xs btn-white btn-bold btn-warning"  onclick="_showBackWashDetail(' + val.fromAccountId + ')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>';
                            }
                            '</td></tr>';
                        });
                        if(outwardCurrentPageSum){
                            trs +='<tr><td id="BackWashCurrentCount" colspan="4">小计：统计中..</td>' +
                                '<td bgcolor="#579EC8" style="color:white;width: 100px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                                '<td colspan="15"></td></tr>' ;
                        }else{
                            trs +='<tr><td id="BackWashCurrentCount" colspan="19">小计：统计中..</td></tr>' ;
                        }
                        if(outwardAllRecordSum){
                            trs +='<tr><td id="BackWashCount" colspan="4">总共：统计中..</td>' +
                                '<td id="BackWashSum" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td><td colspan="15"></td></tr>';
                        }else{
                            trs +='<tr><td id="BackWashCount" colspan="19">总共：统计中..</td></tr>';
                        }
                    }
                }
                $('#tbody_backwash').html(tr).append(trs);
                _getBackWashSum(data);
                _getBackWashCount(data);
                $("[data-toggle='popover']").popover();
                loadHover_accountInfoHover(idList);
                loadHover_thirdDaiFuHover(idListThird);
            }
            showPading(res.page, 'backwash_footPage', _searchBackWash);
        }
    });
}
function _resetValueBackwash() {
    initPaging($('#backwash_footPage'), pageInitial);//重置后页面页脚也恢复初始值
    _initialSelectChosen('backwash');
    $('#account_backwash').val('');
    $('#orderNo_backwash').val('');
    $('#backwash_todo').prop('checked', 'checked');
    $('#backwash_done').prop('checked', '');
    $('#fromMoney_backwash').val('');
    $('#toMoney_backwash').val('');
    $('#operator_backwash').val('');
    _dateP();
    _searchBackWash();

}
function _getBackWashSum(data) {
    $.ajax({
        type: 'get',
        url: '/r/banklog/sumBackWashBankLog',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#BackWashSum').text(parseFloat(res.data).toFixed(3));
                }
            }
        }
    });
}
function _getBackWashCount(data) {
    $.ajax({
        type: 'get',
        url: '/r/banklog/countBackWashBankLog',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#BackWashCurrentCount').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#BackWashCount').empty().text('合计：' + res.page.totalElements + '条记录');
                    showPading(res.page, 'backwash_footPage', _searchBackWash);
                }
            }
        }
    });
}
/**处理冲正*/
function _dealBackwash(id) {
    $('#prompt_remark').hide();
    $('#backwashOrderNoDiv').show();
    $('#backwashOrderNo').val('');
    $('#CustomerserviceRemark').val('');
    $('#CustomerserviceRemark_id').val(id);
    $('#CustomerserviceRemark_modal #titleDivCustomer').html('<button type="button" class="close" data-dismiss="modal" aria-hidden="true">\n' +
        '                        <span class="white">&times;</span>\n' +
        '                    </button>\n' +
        '                    处理');
    $('#CustomerserviceRemark_modal').modal('show');
    $('#totalTaskFinishBTN').attr('onclick', '_executeDeal();');
}
function _executeDeal() {
    var remark = $('#CustomerserviceRemark').val();
    var bankLogId = $('#CustomerserviceRemark_id').val();
    var orderNo = $('#backwashOrderNo').val();
    if (!orderNo) {
        $('#prompt_remark').text('请填写对应的订单号').show(10).delay(1500).hide(10);
        return;
    }
    if (!remark) {
        $('#prompt_remark').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    bootbox.confirm("确定处理这条冲正吗？", function (res) {
        if (res) {
            $.ajax({
                type: 'put',
                url: '/r/banklog/dealBackwashBankLog',
                data: {"remark": remark + "(订单号:" + orderNo + ")", "bankLogId": bankLogId, "localHostIp": localHostIp},
                dataType: 'json',
                success: function (res) {
                    if (res) {
                        if (res.status == 1) {
                            $('#CustomerserviceRemark_modal').modal('hide');
                            $('#CustomerserviceRemark_id').val('');
                            $('#backwashOrderNo').val('');
                            $('#backwashOrderNoDiv').hide();
                            $('#CustomerserviceRemark_modal #titleDivCustomer').html('<button type="button" class="close" data-dismiss="modal" aria-hidden="true">\n' +
                                '                        <span class="white">&times;</span>\n' +
                                '                    </button>\n' +
                                '                    备注');
                            _searchBackWash();
                        }
                    }
                    $.gritter.add({
                        time: 2000,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                    $('#totalTaskFinishBTN').attr('onclick', '');
                }
            });
        }
    });

}
/**添加备注*/
function _remarkBackWash(id) {
    $('#prompt_remark').hide();
    $('#backwashOrderNoDiv').hide();
    $('#CustomerserviceRemark').val('');
    $('#CustomerserviceRemark_id').val(id);
    $('#CustomerserviceRemark_modal #titleDivCustomer').html('<button type="button" class="close" data-dismiss="modal" aria-hidden="true">\n' +
        '                        <span class="white">&times;</span>\n' +
        '                    </button>\n' +
        '                    备注');
    $('#CustomerserviceRemark_modal').modal('show');
    $('#totalTaskFinishBTN').attr('onclick', '_saveRemarkBackWash();');

}
function _saveRemarkBackWash() {
    var remark = $('#CustomerserviceRemark').val();
    var bankLogId = $('#CustomerserviceRemark_id').val();
    if (!remark) {
        $('#prompt_remark').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    $.ajax({
        type: 'put',
        url: '/r/banklog/customerAddRemark',
        data: {"remark": remark, "id": bankLogId, "localHostIp": localHostIp},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#CustomerserviceRemark_modal').modal('hide');
                    $('#CustomerserviceRemark_id').val('');
                    _searchBackWash();
                }
            }
            $.gritter.add({
                time: 2000,
                class_name: '',
                title: '系统消息',
                text: res.message,
                sticky: false,
                image: '../images/message.png'
            });
            $('#totalTaskFinishBTN').attr('onclick', '');
        }
    });
}
/**显示明细*/
var _showBackWashDetail = function (accountId) {
    //查询账号信息
    var result = getAccountMoreInfoById(accountId);
    $.ajax({
        type: "GET",
        async: false,
        dataType: 'html',
        url: "/" + sysVersoin + "/html/common/bankLogList.html",
        success: function (html) {
            var $div = $(html).find("#bankLogList_modal").clone().appendTo($("body"));
            if (result && result.account) {
                $div.find("input#accountInfo_id").val(accountId);
                var accountInfo = result.account;
                if (accountInfo) {
                    var accountTitle = (accountInfo.alias ? accountInfo.alias : '无') + '|' + (accountInfo.bankType ? accountInfo.bankType : '无') + '|' + (accountInfo.owner ? accountInfo.owner : '无') + '|' + hideAccountAll(accountInfo.account);
                    $div.find("#accountInfo_account").text(accountTitle);
                    $div.find("#accountInfo_balance").text(accountInfo.balance ? accountInfo.balance : 0);
                    $div.find("#accountInfo_bankBalance").text(accountInfo.bankBalance ? accountInfo.bankBalance : 0);
                    $div.find("#accountInfo_noOwnerBalance").text(result.sumAmountCondition);
                    $div.find("#incomeDailyTotal").text(result.incomeDailyTotal);
                    $div.find("#outDailyTotal").text(result.outDailyTotal);
                    $div.find("#queryIncomeTotal").text(result.queryIncomeTotal);
                    $div.find("#queryOutTotal").text(result.queryOutTotal);
                }
                //加载时间控件
                initTimePicker();
                //数据列表
                bankLogList_In_OutList();
                $div.modal("toggle");
                $div.find("[name=searchTypeIn0Out1],[name=search_IN_status]").click(function () {
                    bankLogList_In_OutList();
                });
                $(document).keypress(function (e) {
                    if (event.keyCode == 13) {
                        $div.find("#searchBtn button").click();
                    }
                });
            } else {
                showMessageForFail("账号信息查询异常，请刷新页面");
            }
            $div.on('hidden.bs.modal', function () {
                //关闭窗口清除model
                $div.remove();
            });
        }
    });
};
function _initialSort() {
    $('#amountSortDiv').find('a').html('<i class="ace-icon fa fa-angle-down"></i>');
    $('#amountSortDiv').find('a').attr('title', '降序(默认金额出款时间耗时降序)');
    $('#asignTimeSortDiv').find('a').html('<i class="ace-icon fa fa-angle-down"></i>');
    $('#asignTimeSortDiv').find('a').attr('title', '降序(默认金额出款时间耗时降序)');
    $('#timeConsumingSortDiv').find('a').html('<i class="ace-icon fa fa-angle-down"></i>');
    $('#timeConsumingSortDiv').find('a').attr('title', '降序(默认金额出款时间耗时降序)');
}
//目前只允许对一个列排序
//可以使用Juery dataTable 对查询出来的数据排序
function _sortSearch(obj) {
    var iClassName = $(obj).find('a').find('i').attr('class');
    var id = $(obj).attr('id');
    if (iClassName.indexOf('fa-angle-up') > -1) {
        $(obj).find('a').html('<i class="ace-icon fa fa-angle-down"></i>');
        $(obj).find('a').attr('title', '降序');
        if (id == 'amountSortDiv') {
            sortFlagForSuccessOut = 1;
        }
        if (id == 'asignTimeSortDiv') {
            sortFlagForSuccessOut = 2;
        }
        if (id == 'timeConsumingSortDiv') {
            sortFlagForSuccessOut = 3;
        }
    } else {
        $(obj).find('a').html('<i class="ace-icon fa fa-angle-up"></i>');
        $(obj).find('a').attr('title', '升序');
        if (id == 'amountSortDiv') {
            sortFlagForSuccessOut = 4;
        }
        if (id == 'asignTimeSortDiv') {
            sortFlagForSuccessOut = 5;
        }
        if (id == 'timeConsumingSortDiv') {
            sortFlagForSuccessOut = 6;
        }
    }
    _search();
}

function modifyStyle(){
    if (isHideOutAccountAndModifyNouns) {
       // $('#orderNo_masterOut').attr('style','height: 32px;width:67.6%;');
        $('#fromMoney_masterOut').attr('style','height:32px;width:29%;');
        $('#toMoney_masterOut').attr('style','height:32px;width:30%;');
        $('#fromMoney_backwash').attr('style','height:32px;width:29%;');
        $('#toMoney_backwash').attr('style','height:32px;width:30%;');
        $('.modifyAmountException').html('金额<div class="action-buttons inline" id="amountSortDiv" onclick="_sortSearch(this);">\n' +
            '                        <a href="javascript:void(0);" class="green bigger-140 show-details-btn"\n' +
            '                           title="降序(默认出货点数出货时间耗时降序)">\n' +
            '                            <i class="ace-icon fa fa-angle-down"></i>\n' +
            '                        </a>\n' +
            '                    </div>');
        $('.DrawingAmountInput').attr('style','height:32px;width:8%;');
        $('.ToDrawAmountInput').attr('style','height:32px;width:30%;');
        $('.FailOutAmountInput').attr('style','height:32px;width:29.5%;');
        $('.ManagerDealDiv').attr('style','display:none;');
        $('.modifyOutAccountFail').text('出货账号');
    }
}
function _checkSysOrderTr(obj) {
    var checked_val = $(obj).attr('checked_val');
    var checked_class = $(obj).attr('class');
    if(checked_val==1){
        var currentVal = $(obj).val();
        if (checked_class.indexOf('type_'+searchTypeTaskTotal)>-1) {
            $('input.type_'+searchTypeTaskTotal+'').each(function (i) {
                if(this.value!=currentVal){
                    $(this).attr('checked_val',1);$(this).prop('checked','');
                }
            });
        }
        if (checked_class.indexOf('drawType_'+searchTypeTaskTotal)>-1){
            $('input.drawType_'+searchTypeTaskTotal+'').each(function (i) {
                if(this.value!=currentVal){
                    $(this).attr('checked_val',1);$(this).prop('checked','');
                }
            });
        }
        $(obj).attr('checked_val',2);$(obj).prop('checked','checked');
    }else{
        $(obj).attr('checked_val',1);$(obj).prop('checked','');
    }
    _search();
}
_permitCheck();




