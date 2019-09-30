jQuery(function($) {
	getHandicap_select($("#RebateDrawingHandicap"),null,"全部");
	getHandicap_select($("#rebateHandicap"),null,"全部");
	getHandicap_select($("#checkHandicap"),null,"全部");
	getHandicap_select($("#canceledHandicap"),null,"全部");
	getHandicap_select($("#successHandicap"),null,"全部");
	//queryRebateDrawing();
	initTimePicker(true,$("[name=successTime]"),typeCustomLatestToday);
	initTimePicker(true,$("[name=canceledTime]"),typeCustomLatestToday);
	initTimePickerHandicap("auditCommissionTime");
	initTimePickerHandicap("completeTime");
	genBankType("bankType");
	//_searchRebateTimeTask('RebateDrawing');
	queryAuditCommission();
})

function initTimePickerHandicap(name){
	     start=moment().add(-4,'days');
    	end=moment();
	    var exportStartAndEndTime = $("[name="+name+"]").daterangepicker({
			autoUpdateInput:false,
			timePicker: false, //显示时间
		    timePicker24Hour: false, //24小时制
		    timePickerSeconds:false,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
			locale: {
				"format": 'YYYY-MM-DD',
				"separator": " - ",
				"applyLabel": "确定",
				"cancelLabel": "取消",
				"fromLabel": "从",
				"toLabel": "到",
				"customRangeLabel": "Custom",
				"dayNames": ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
				"daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
				"monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
				"firstDay": 1
			}
		}).val((start.format('YYYY-MM-DD')+' - '+end.format('YYYY-MM-DD')));
	    exportStartAndEndTime.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD') + ' - ' + picker.endDate.format('YYYY-MM-DD'));
			exportOut(reaccountid,rehandicapid);
	    });
	    exportStartAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}


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
$.each(ContentRight['RebateTaskTotal:*'], function (name, value) {
    if (name == 'RebateTaskTotal:cancel:*') {
    	cancelButton = true;
    }
    if (name == 'RebateTaskTotal:newTask:*') {
    	newTaskButton = true;
    }
    if (name == 'RebateTaskTotal:finished:*') {
    	finishedButton = true;
    }
    if (name == 'RebateTaskTotal:check:*') {
    	checkButton = true;
    }
});

function addRemark(id,type){
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
	$('#totalTaskFinishBTN').attr('onclick', 'save_Remark('+id+','+type+');');
}

function save_Remark(id,status){
	var remark=$.trim($("#Remark").val());
	if(remark==""){
		  $('#prompt_remark').show(10).delay(1500).hide(10);
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




//正在出款
var queryRebateDrawing=function(){
	if (currentPageLocation.indexOf('RebateTaskTotal:*') == -1){
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
	
	$.ajax({
		type:"post",
		url:"/r/rebate/findRebate",
		data:{
			"pageNo":CurPage,
			"status":0,
			"type":"rebate",
			"orderNo":orderNo,
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
	                        		+'<button onclick="addRemark('+val.id+',8);" type="button" class=" btn btn-xs btn-white btn-warning  btn-bold"><i class="ace-icon fa fa-reply  bigger-100 red"></i>分配</button>'
	                        		+'<button type="button" onclick="addRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
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
	if (currentPageLocation.indexOf('RebateTaskTotal:*') == -1){
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
$.ajax({
	type:"post",
	url:"/r/rebate/findRebate",
	data:{
		"pageNo":CurPage,
		"status":0,
		"type":"rebated",
		"orderNo":orderNo,
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
                        	    +'<button onclick="addRemark('+val.id+',8);" type="button" class=" btn btn-xs btn-white btn-warning  btn-bold"><i class="ace-icon fa fa-reply  bigger-100 red"></i>分配</button>'
                        		+'<button type="button" onclick="addRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
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
	if (currentPageLocation.indexOf('RebateTaskTotal:*') == -1){
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
$.ajax({
	type:"post",
	url:"/r/rebate/findRebate",
	data:{
		"pageNo":CurPage,
		"status":6,
		"type":"check",
		"orderNo":orderNo,
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
				    	  tr +='<button type="button" onclick="addRemark('+val.id+',3)" class="btn btn-xs btn-white btn-danger btn-bold"><i class="ace-icon fa fa-remove  bigger-100 red"></i>取消</button>';
				      if(newTaskButton)
				    	  tr +='<button onclick="addRemark('+val.id+',0);" type="button" class=" btn btn-xs btn-white btn-warning  btn-bold"><i class="ace-icon fa fa-reply  bigger-100 red"></i>重新生成任务</button>';
				    tr +='<button type="button" onclick="addRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                 	  if(finishedButton)	
                 		  tr+='<button type="button" onclick="addRemark('+val.id+',1)" class="btn btn-xs btn-white btn-success btn-bold "><i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
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

$.ajax({
	type:"post",
	url:"/r/rebate/findRebate",
	data:{
		"pageNo":CurPage,
		"status":1,
		"type":"check",
		"orderNo":orderNo,
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
                 		tr+='<button onclick="addRemark('+val.id+',6)" type="button" class=" btn btn-xs btn-white btn-danger btn-bold"><i class="ace-icon fa fa-share  bigger-100 red"></i>转待排查</button>';
                 	tr+='<button type="button" onclick="addRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
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
$.ajax({
	type:"post",
	url:"/r/rebate/findRebate",
	data:{
		"pageNo":CurPage,
		"status":3,
		"type":"canceled",
		"orderNo":orderNo,
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
                 	+'<button type="button" onclick="addRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
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

//佣金审核统计
var queryAuditCommission=function(){
	//当前页码
var CurPage=$("#auditCommissionTabPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
var startAndEndTime = $("input[name='auditCommissionTime']").val();
var startAndEndTimeToArray = new Array();
if(startAndEndTime){
	var startAndEnd = startAndEndTime.split(" - ");
	startAndEndTimeToArray.push($.trim(startAndEnd[0]));
	startAndEndTimeToArray.push($.trim(startAndEnd[1]));
}
startAndEndTimeToArray = startAndEndTimeToArray.toString();
var results=$("#results").val();

$.ajax({
	type:"post",
	url:"/r/rebate/findAuditCommission",
	data:{
		"pageNo":CurPage,
		"startAndEndTime":startAndEndTimeToArray,
		"results":results,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var bankAmounts=0;
			 var rebateAmounts=0;
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 tr += '<tr>';
					if(val.status==0){
						tr +='<td>'+ val.calcTime +'</td>'
	                 	+'<td></td>'
	                 	+"<td></td>"
	                 	+'<td></td>'
	                 	+'<td></td>'
	                 	+'<td></td>'
						+'<td>';
					}else{
						tr +='<td>' + val.calcTime + '</td>'
	                 	+'<td>' + val.counts + '</td>'
	                 	+"<td><a onclick='detail(\""+val.calcTime+"\",1)'>" + val.bankAmounts.toFixed(2) + "</a></td>"
	                 	+'<td>'+ (val.rebateAmount/val.bankAmounts).toFixed(4) +'</td>'
	                 	+'<td>' + val.rebateAmount.toFixed(2) + '</td>'
	                 	+'<td>'
		           	      + '<a class="bind_hover_card breakByWord"  title="备注"'
		                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
		                    + ' data-content="' + val.remark + '">'
		                     + val.remark.substring(((val.remark.indexOf("<br>"))*1+4),((val.remark.indexOf("<br>"))*1+9))
		                 + '</a>'
		                +'</td>'
		                +'<td>';
					}
				 		if(val.status==0){
					 		if(val.isLock==0){
					 			tr+="<button type='button' onclick='lock(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-lock bigger-110 red'></i>锁定</button>"
								 +"<button disabled='disabled' type='button' onclick='detail(\""+val.calcTime+"\",2)' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-times  bigger-110 red'></i>审核</button>";
								 //+"<button type='button' onclick='detail(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-list bigger-100 blue'></i>明细</button>";
					 		}else if(val.isLock==1){
					 			if(val.isLockMyself==0){
					 				tr+="<button disabled='disabled' type='button' onclick='lock(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-refresh bigger-110 red'></i>锁定</button>"
									 +"<button disabled='disabled' type='button' onclick='detail(\""+val.calcTime+"\",2)' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-times  bigger-110 red'></i>审核</button>";
									// +"<button type='button' onclick='detail(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-list bigger-100 blue'></i>明细</button>";
					 			}else if(val.isLockMyself==1){
					 				tr+="<button  type='button' onclick='unlock(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-unlock bigger-110 red'></i>解锁</button>"
									 +"<button  type='button' onclick='detail(\""+val.calcTime+"\",2)' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-check-square-o bigger-100 orange'></i>审核</button>";
									// +"<button type='button' onclick='detail(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-list bigger-100 blue'></i>明细</button>";
					 			}
					 		}
				 		}else{
				 			if(val.status==2){
				 				tr+="<button disabled='disabled' type='button' onclick='lock(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-lock bigger-110 bigger-110 red'></i>锁定</button>"
								 +"<button disabled='disabled' type='button' onclick='detail(\""+val.calcTime+"\",2)' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-times  bigger-110 red'></i>审核</button>"
								 +"<button type='button' onclick='recalculate(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-refresh  bigger-110 red'></i>重新计算</button>"
								// +"<button type='button' onclick='detail(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-list bigger-100 blue'></i>明细</button>";
				 			}else{
				 				tr+="<button disabled='disabled' type='button' onclick='lock(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-refresh bigger-110 red'></i>锁定</button>"
								 +"<button disabled='disabled' type='button' onclick='detail(\""+val.calcTime+"\",2)' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-times  bigger-110 red'></i>审核</button>"
								 //+"<button type='button' onclick='detail(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-list bigger-100 blue'></i>明细</button>";
				 			}
				 		}
                 	tr+='</td>'
                 	+'</tr>';
                counts +=1;
                bankAmounts+=val.bankAmounts;
                rebateAmounts+=val.rebateAmount;
            };
			 $('#auditCommissionTab_tbody').empty().html(tr);
			 var trs = '<tr>'
							 +'<td colspan="2">小计：'+counts+'</td>'
							 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
							 +'<td></td>'
						     +'<td bgcolor="#579EC8" style="color:white;">'+rebateAmounts.toFixed(2)+'</td>'
						     +'<td colspan="3"></td>'
					  +'</tr>';
            $('#auditCommissionTab_tbody').append(trs);
            var trn = '<tr>'
		                	+'<td colspan="2">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[0])+'</td>'
		                	+'<td></td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[1])+'</td>'
						    +'<td colspan="3"></td>'
			         +'</tr>';
            $('#auditCommissionTab_tbody').append(trn);
			}else {
                $('#auditCommissionTab_tbody').empty();
            }
		$("[data-toggle='popover']").popover();
		//分页初始化
		showPading(jsonObject.data.rebatePage,"auditCommissionTabPage",queryAuditCommission);
	}
});}


function lock(caclTime){
	bootbox.confirm("确定锁定（"+caclTime+"）数据？", function(result) {
		if (result) {
			$.ajax({ dataType:'json',type:"get", url:'/r/rebate/lock',async:false,data:{"caclTime":caclTime},success:function(jsonObject){
					if(jsonObject.status == 1){
						showMessageForSuccess(jsonObject.message,2000);
						queryAuditCommission();
					}else{
						showMessageForFail(jsonObject.message);
					}
				},error:function(result){  bootbox.alert(result); }});
		}
	});
}

function unlock(caclTime){
	bootbox.confirm("确定解锁（"+caclTime+"）数据？", function(result) {
		if (result) {
			$.ajax({ dataType:'json',type:"get", url:'/r/rebate/unlock',async:false,data:{"caclTime":caclTime},success:function(jsonObject){
					if(jsonObject.status == 1){
						showMessageForSuccess(jsonObject.message,2000);
						queryAuditCommission();
					}
				},error:function(result){  bootbox.alert(result); }});
		}
	});
}

function audit(caclTime,type){
	$("#auditRemark").val("");
	$('#audit_modal').modal('show');
	$('#auditFinishBTN').attr('onclick', "save_audit(\""+caclTime+"\","+type+");");
}

function recalculate(caclTime){
	bootbox.confirm("确定要重新计算（"+caclTime+"）返利数据？", function(result) {
		if (result) {
			$.ajax({
				async:true,
				type:'post',
		        url:'/r/rebate/recalculate',
		        data:{'caclTime':caclTime},
		        dataType:'json',
		        success:function (res) {
		        	if(res.status == 1){
		        		showMessageForSuccess(res.message,2000);
		        		queryAuditCommission();
		        	}else{
		        		showMessageForFail(res.message);
		        	}
		        }
		    });
	}
	});
}


function save_audit(caclTime,type){
	$('#auditFinishBTN').attr("disabled",true); 
	$('#auditFinishBTN').attr('onclick', "");
	var remark=$.trim($("#auditRemark").val());
	if(remark==""||remark==null||remark.length<5){
		showMessageForFail("请填写备注且长度大于5！");
		$('#auditFinishBTN').attr("disabled",false); 
		$('#auditFinishBTN').attr('onclick', "save_audit(\""+caclTime+"\","+type+");");
		return
	}
	var rebateUser="";
	if(type==2){
		//部分通过
		$("input[name='passRebateAmounts']:checked").each(function(i){//把所有被选中的复选框的值存入数组
			rebateUser+=$(this).val()+",";
        });
	}
	if(type==2 && ""==rebateUser){
		showMessageForFail("请选择不通过的返利网账号！");
		$('#auditFinishBTN').attr("disabled",false); 
		$('#auditFinishBTN').attr('onclick', "save_audit(\""+caclTime+"\","+type+");");
		return
	}
	var status=$("#auditResults").val();
	$.ajax({
		async:false,
		type:'post',
        url:'/r/rebate/saveAudit',
        data:{'caclTime':caclTime,'status':status,'remark':remark,'rebateUser':rebateUser},
        dataType:'json',
        success:function (res) {
        	if(res.status == 1){
        		$('#audit_modal').modal('hide');
        		$('#show_deal').modal('hide');
        		$('#auditFinishBTN').attr("disabled",false); 
        		showMessageForSuccess(res.message,2000);
        		queryAuditCommission();
        	}else{
        		$('#auditFinishBTN').attr("disabled",false); 
        		showMessageForFail(res.message);
        	}
        }
    });
}
var chooseTime="";
var chooseType="";
var arr = new Array();
function detail(caclTime,type){
if(""!=caclTime && undefined!=caclTime){
	chooseTime=caclTime;
	chooseType=type;
}
if(""==caclTime || undefined==caclTime){
	caclTime=chooseTime;
	type=chooseType;
}
if(type==2){
	$('#showButton').show();
	$('#allPass').attr('onclick', "audit(\""+caclTime+"\",1);");
	$('#partPass').attr('onclick', "audit(\""+caclTime+"\",2);");
}else{
	$('#showButton').hide();
	$('#allPass').attr('onclick', "");
}
$('#show_deal').modal('show');
//当前页码
var CurPage=$("#show_dealPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
var rebateUser=$("#rebateUser").val();
var bankType=$("#bankType").val();
var startamount=$("#startamount").val();
var endamount=$("#endamount").val();
if(""!=startamount&&""!=endamount&&startamount*1>endamount*1){
	showMessageForFail("金额查询有误！");
	return;
}
var dd=caclTime+" 00:00:00 - "+caclTime+" 23:59:59";
$.ajax({
	type:"post",
	url:"/r/rebate/findDetail",
	data:{
		"pageNo":CurPage,
		"rebateUser":rebateUser,
		"bankType":bankType,
		"caclTime":caclTime,
		"startamount":startamount,
		"endamount":endamount,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var bankAmounts=0;
			 var rebateAmounts=0;
			 var idList=new Array();
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 idList.push({'id':val.accountId});
				 tr += '<tr>';
				 if((arr.indexOf(val.rebateUser))>-1){
						 tr +='<td><input type="checkbox" checked onclick="selected(\''+val.rebateUser+'\')" name="passRebateAmounts" value="'+val.rebateUser+'"></td>';
					 }else{
						 tr +='<td><input type="checkbox" onclick="selected(\''+val.rebateUser+'\')" name="passRebateAmounts" value="'+val.rebateUser+'"></td>';
					 }
					 tr +='<td>' + val.rebateUser + '</br>总数：'+val.totalCards+'</td>'
					 +"<td title='银行类别|开户人|账号'>" 
		           	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+val.bankType+"|" +hideName(val.owner)
		           	      +"</a></br>"
		           	      +hideAccountAll(val.account)
		       	      +"</td>"
                 	+'<td><a onclick="showInOutListModal('+val.accountId+',\'false\',\''+dd+'\')">' + val.bankAmounts.toFixed(2) + '</a></td>'
                 	+'<td>'+ (val.rebateAmount/val.bankAmounts).toFixed(4) +'</td>'
                 	+'<td>' + val.rebateAmount.toFixed(2) + '</td>'
                 	+'<td>' + val.rebateAmount.toFixed(2) + '</td>'
                 	+'</tr>';
                counts +=1;
                bankAmounts+=val.bankAmounts;
                rebateAmounts+=val.rebateAmount;
            };
			 $('#show_deal_tbody').empty().html(tr);
			 SpanGrid(show_deal_tbody,1);
			 var trs = '<tr>'
							 +'<td colspan="3">小计：'+counts+'</td>'
							 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
							 +'<td></td>'
							 +'<td></td>'
						     +'<td bgcolor="#579EC8" style="color:white;">'+rebateAmounts.toFixed(2)+'</td>'
					  +'</tr>';
            $('#show_deal_tbody').append(trs);
            var trn = '<tr>'
		                	+'<td colspan="3">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[0])+'</td>'
		                	+'<td></td>'
		                	+'<td></td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[1])+'</td>'
			         +'</tr>';
            $('#show_deal_tbody').append(trn);
			}else {
                $('#show_deal_tbody').empty();
            }
		//加载账号悬浮提示
		loadHover_accountInfoHover(idList);
		//分页初始化
		showPading(jsonObject.data.rebatePage,"show_dealPage",detail);
	}
});
}

function selected(rebateUser){
	var index=arr.indexOf(rebateUser);
	if(index > -1){
		arr.splice(index, 1); 
	}else{
		arr.push(rebateUser);
	}
}

function SpanGrid(tabObj,colIndex){
	if(tabObj != null){
		var i,j;
		var intSpan;
		var strTemp;
		for(i = 0; i < tabObj.rows.length; i++){
			intSpan = 1;
			strTemp = tabObj.rows[i].cells[colIndex].innerText;
			var totalAmounts=0;
			if(strTemp==""||strTemp==null){
				continue;
			}
			totalAmounts+=tabObj.rows[i].cells[5].innerText*1;
			for(j = i + 1; j < tabObj.rows.length; j++){
				if(strTemp == tabObj.rows[j].cells[colIndex].innerText){
					totalAmounts+=tabObj.rows[j].cells[5].innerText*1;
					intSpan++;
					tabObj.rows[i].cells[0].rowSpan  = intSpan;
					tabObj.rows[i].cells[1].rowSpan  = intSpan;
					tabObj.rows[i].cells[6].rowSpan  = intSpan;
					tabObj.rows[i].cells[6].innerHTML=totalAmounts;
					var height=(intSpan+2);
					var ht=intSpan;
					//设置文本居中
					tabObj.rows[i].cells[0].style="line-height:"+height;
					tabObj.rows[i].cells[1].style="line-height:"+height;
					tabObj.rows[i].cells[6].style="line-height:"+height;
					tabObj.rows[j].cells[0].style.display = "none";
					tabObj.rows[j].cells[1].style.display = "none";
					tabObj.rows[j].cells[6].style.display = "none";
				}
				else{
					totalAmounts+=tabObj.rows[i].cells[5].innerText*1;
					tabObj.rows[i].cells[1].innerHTML=tabObj.rows[i].cells[1].innerHTML+" 被用："+intSpan;
					break;
				}
			}
			i = j - 1;
			if(i == (tabObj.rows.length-1)){
				tabObj.rows[i].cells[1].innerHTML=tabObj.rows[i].cells[1].innerHTML+" 被用："+intSpan;
			}
		}
	}
}

var genBankType = function(bankTypeId){
    var ret ='<option value="">请选择</option>';
    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true});
    $('#'+bankTypeId+'_chosen').prop('style', 'width: 120px;')
};


//佣金出款完成
var queryComplete=function(){
	//当前页码
var CurPage=$("#completeTabPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
var startAndEndTime = $("input[name='completeTime']").val();
var startAndEndTimeToArray = new Array();
if(startAndEndTime){
	var startAndEnd = startAndEndTime.split(" - ");
	startAndEndTimeToArray.push($.trim(startAndEnd[0]));
	startAndEndTimeToArray.push($.trim(startAndEnd[1]));
}
startAndEndTimeToArray = startAndEndTimeToArray.toString();

$.ajax({
	type:"post",
	url:"/r/rebate/findComplete",
	data:{
		"pageNo":CurPage,
		"startAndEndTime":startAndEndTimeToArray,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var bankAmounts=0;
			 var rebateAmounts=0;
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 tr += '<tr>'
					 +'<td>' + val.calcTime + '</td>'
                 	+'<td>' + val.counts + '</td>'
                 	+'<td>' + val.bankAmounts.toFixed(2) + '</td>'
                 	+'<td>0</td>'
                 	+'<td>' + val.rebateAmount.toFixed(2) + '</td>'
                 	+'<td>'
                 	      + '<a class="bind_hover_card breakByWord"  title="备注"'
                          + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                          + ' data-content="' + val.remark + '">'
                           + val.remark.substring(0,10)
                       + '</a>'
                      +'</td>'
                 	+'<td>'
						+"<button type='button' onclick='completeDetail(\""+val.calcTime+"\")' class='btn btn-xs btn-white btn-info btn-bold '><i class='ace-icon fa fa-list bigger-100 blue'></i>明细</button>"
                 	+'</td>'
                 	+'</tr>';
                counts +=1;
                bankAmounts+=val.bankAmounts;
                rebateAmounts+=val.rebateAmount;
            };
			 $('#completeTab_tbody').empty().html(tr);
			 var trs = '<tr>'
							 +'<td colspan="2">小计：'+counts+'</td>'
							 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
							 +'<td></td>'
						     +'<td bgcolor="#579EC8" style="color:white;">'+rebateAmounts.toFixed(2)+'</td>'
						     +'<td colspan="2"></td>'
					  +'</tr>';
            $('#completeTab_tbody').append(trs);
            var trn = '<tr>'
		                	+'<td colspan="2">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[0])+'</td>'
		                	+'<td></td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[1])+'</td>'
						    +'<td colspan="2"></td>'
			         +'</tr>';
            $('#completeTab_tbody').append(trn);
			}else {
                $('#completeTab_tbody').empty();
            }
		$("[data-toggle='popover']").popover();
		//分页初始化
		showPading(jsonObject.data.rebatePage,"completeTabPage",queryComplete);
	}
});}

//佣金出款完成
var completeDetailTime="";
var completeDetail=function(caclTime){
$('#complete_deal').modal('show');
if(""!=caclTime && undefined!=caclTime){
	completeDetailTime=caclTime;
}
if(""==caclTime || undefined==caclTime){
	caclTime=completeDetailTime;
}
//当前页码
var CurPage=$("#complete_dealPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
var rebateUser=$("#completeRebateUser").val();

$.ajax({
	type:"post",
	url:"/r/rebate/findCompleteDetail",
	data:{
		"pageNo":CurPage,
		"rebateUser":rebateUser,
		"caclTime":caclTime,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var bankAmounts=0;
			 var rebateAmounts=0;
			 var rebateBalance=0;
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 tr += '<tr>'
					 	+'<td>' + val.rebateUser + '</td>'
	                 	//+'<td>' + val.bankAmounts.toFixed(2) + '</td>'
	                 	+'<td>' + val.rebateAmount.toFixed(2) + '</td>'
	                 	+'<td>0</td>'
	                 	+'<td>' + val.rebateBalanc.toFixed(2) + '</td>'
                 	+'</tr>';
                counts +=1;
                bankAmounts+=val.bankAmounts;
                rebateAmounts+=val.rebateAmount;
                rebateBalance+=val.rebateBalanc;
            };
			 $('#complete_deal_tbody').empty().html(tr);
			 var trs = '<tr>'
						 +'<td>小计：'+counts+'</td>'
						 //+'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
						 +'<td bgcolor="#579EC8" style="color:white;">'+rebateAmounts.toFixed(2)+'</td>'
						 +'<td></td>'
						 +'<td bgcolor="#579EC8" style="color:white;">'+rebateBalance.toFixed(2)+'</td>'
					  +'</tr>';
            $('#complete_deal_tbody').append(trs);
            var trn = '<tr>'
		            	+'<td>总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		            	//+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[0])+'</td>'
		            	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[1])+'</td>'
		            	+'<td></td>'
		            	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[2])+'</td>'
			         +'</tr>';
            $('#complete_deal_tbody').append(trn);
			}else {
                $('#complete_deal_tbody').empty();
            }
		//分页初始化
		showPading(jsonObject.data.rebatePage,"complete_dealPage",completeDetail);
	}
});}
