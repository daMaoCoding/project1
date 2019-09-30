currentPageLocation = window.location.href;

var descOrAsc="desc";
var orderBy="tradingtime";
	/**
	 * 点击详情按钮
	 */
	function _details(id) {
	    //通过账号交易流水表id查询相关的信息
	    $.ajax({
	        type:'post',
	        url:'/r/finoutstat/finoutstatflowdetails',
	        data:{"id":id},
	        dataType:'json',
	        success:function (jsonObject) {
	        	if(jsonObject.status == 1 && jsonObject.data){
	        		var val = jsonObject.data.arrlist[0];
	        		if(val){
	        			var opname="";
	        			if(val.operatorname==null)
	        				opname="机器";
	        			else
	        				opname=val.operatorname;
	        			var comfirmor=val.comfirmor==null?"机器":val.comfirmor;
	        			var modal='<table class="table table-bordered" border="1">'
	        						 +'<tr><td colspan="8" style="font-size: 14pt; color: #ff3333; font-family:宋体" align="center" valign="middle">会员信息</td></tr>'
	        				         +'<tr>'
	        				         	+'<td class="text-right bk-color-f9">盘口：</td>'
	        				         	+'<td>'+val.handicapname+'</td>'
	        				         	+'<td class="text-right bk-color-f9">层级：</td>'
	        				         	+'<td>'+val.levelname+'</td>'
	        				         	+'<td class="text-right bk-color-f9">账号：</td>'
	        				         	+'<td>'+val.member+'</td>'
	        				         +'</tr>'
	        				         +'<tr>'
	        				         	+'<td class="text-right bk-color-f9"> 转入账号：</td>'
	        				         	+'<td>'+val.toaccount+'</td>'
	        				         	+'<td class="text-right bk-color-f9">开户行：</td>'
	        				         	+'<td>'+val.toaccountname+'</td>'
	        				         	+'<td class="text-right bk-color-f9">开户人：</td>'
	        				         	+'<td>'+val.toaccountowner+'</td>'
	        				         +'</tr>'
	        				         +'<tr><td colspan="8" align="center" style="font-size: 14pt; color: #ff3333; font-family:宋体" valign="middle">流水明细</td></tr>'
	        				         +'<tr>'
	        				         	+'<td class="text-right bk-color-f9">转入账号：</td>'
	        				         	+'<td>'+val.atoaccount+'</td>'
	        				         	+'<td class="text-right bk-color-f9">金额：</td>'
	        				         	+'<td>'+val.amount+'</td>'
	        				         	+'<td class="text-right bk-color-f9">手续费：</td>'
	        				         	+'<td>'+val.fee+'</td>'
	        				         	+'<td class="text-right bk-color-f9">时间：</td>'
	        				         	+'<td>'+val.tradingtime+'</td>'
	        				         +'</tr>'
	        				         +'<tr><td colspan="8" align="center" style="font-size: 14pt; color: #ff3333; font-family:宋体" valign="middle">出款操作明细</td></tr>'
	        				         +'<tr>'
	        				         	+'<td class="text-right bk-color-f9">转入账号：</td>'
	        				         	+'<td>'+val.toaccount+'</td>'
	        				         	+'<td class="text-right bk-color-f9">金额：</td>'
	        				         	+'<td>'+val.damount+'</td>'
	        				         	+'<td class="text-right bk-color-f9">手续费：</td>'
	        				         	+'<td>'+val.bfee+'</td>'
	        				         	+'<td class="text-right bk-color-f9">时间：</td>'
	        				         	+'<td>'+val.asigntime+'</td>'
	        				         +'</tr>'
	        				         +'<tr>'
	        				         	+'<td class="text-right bk-color-f9">转出操作人：</td>'
	        				         	+'<td>'+opname+'</td>'
	        				         	+'<td class="text-right bk-color-f9">操作确认人：</td>'
	        				         	+'<td>'+comfirmor+'</td>'
	        				         +'</tr>'
	        				      +'</table>';
	        			             
	        			
	        			
	        			
	        			var modal_body='出款账号：  '+hideAccountAll(val.accountname)+' &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'+
						'开户行：  '+val.bankname+' &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'+
						'开户人：  '+val.owner+' &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
	        		
//	        		var inner_body='&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span style="color:#F00">会员信息：</span></br>'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;盘口：  '+val.handicapname+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;层级：  '+val.levelname+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;账号：  '+val.member+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</br>'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;转入账号：  '+val.toaccount+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;开户行：  '+val.toaccountname+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;开户人：  '+val.toaccountowner+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</br>'
//	    				+'&nbsp;&nbsp;流水明细：</br>'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;转入账号：  '+val.atoaccount+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;金额：  '+val.amount+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;手续费：  '+val.fee+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;时间：  '+val.tradingtime+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</br>'
//	    				+'&nbsp;&nbsp;操作明细：</br>'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;交易类型：   出款&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</br>'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;转入账号：  '+val.toaccount+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;金额：  '+val.damount+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;手续费：  '+val.bfee+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;时间：  '+val.asigntime+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</br>'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;转出操作人：  '+val.operatorname+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</br>'
//	    				+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;操作确认人：  '+val.comfirmor+'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</br>';
	                $('#modal_body').empty().html(modal_body);
	                $('#inner_body').empty().html(modal);
	                $('#finOutStatFlow_details').modal('toggle');
	        		}else{
						bootbox.alert("没有数据或数据有问题！");
			        }
				}
	        }
	    });
	
	}


	var initTimePickerHandicap=function(){
    	start=moment().add(-1,'days').hours(07).minutes(0).seconds(0);
    	end=moment().hours(06).minutes(59).seconds(59);
	    var exportStartAndEndTime = $('input.date-range-picker').daterangepicker({
			autoUpdateInput:false,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
			locale: {
				"format": 'YYYY-MM-DD HH:mm:ss',
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
		}).val((start.format('YYYY-MM-DD HH:mm:ss')+' - '+end.format('YYYY-MM-DD HH:mm:ss')));
	    exportStartAndEndTime.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			queryFinOutStatFlow();
	    });
	    exportStartAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}

	//查询按钮
	$('#searhBytToAccountOwner').click(function () {
		queryFinOutStatFlow();
    });
	
	//绑定按键事件，回车查询数据
	$('#auditTab').bind('keypress',getKeyCode);   
	function getKeyCode(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryFinOutStatFlow();
	    }
	}
	
	function clearNoNum(obj)
    {
        //先把非数字的都替换掉，除了数字和.
        obj.value = obj.value.replace(/[^\d.]/g,"");
        //必须保证第一个为数字而不是.
        obj.value = obj.value.replace(/^\./g,"");
        //保证只有出现一个.而没有多个.
        obj.value = obj.value.replace(/\.{2,}/g,".");
        //保证.只出现一次，而不能出现两次以上
        obj.value = obj.value.replace(".","$#$").replace(/\./g,"").replace("$#$",".");
    }
	
	
	//定义公共的变量，返回的时候进行传参。判断需要查询的数据，已经参数的值显示
	var parentaccount="";
	var parentstartAndEndTime="";
	var parentfieldval="";
	var parentzhCurPage="";
	var parentpkCurPage="";
	var accountOwner="";
	var bankType="";
	var handicapAccount="";
	//出款明细>银行明细
	var queryFinOutStatFlow=function(){
		//在分页的地方显示出款账号
		var accountCK="";
		//获取开户人
		var toaccountowner=$("#toaccountowner").val();
		//获取收款账号
		var toaccount=$("#toaccount").val();
		//当前页码
		var CurPage=$("#finOutStatFlowPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		var request=getRequest();
		//账号id
		var accountidvalue;
		//获取汇入账号的参数
		if(request&&request.accountid){
			accountidvalue=request.accountid;
			parentaccount=request.account;
			parentstartAndEndTime=request.startAndEndTime;
			parentfieldval=request.fieldval;
			accountOwner=decodeURI(decodeURI(request.accountOwner));
			bankType=decodeURI(decodeURI(request.bankType));
			handicapAccount=request.handicap;
			//记录父页面当前页码信息
			parentzhCurPage=request.zhCurPage;
			parentpkCurPage=request.pkCurPage;
		}
		//拼接父页面的时间
		var parentstartAndEndTimeToArray = new Array();
		if(parentstartAndEndTime){
			var ptstartAndEnd = parentstartAndEndTime.split(" - ");
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[0]));
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[1]));
		}
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTime']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//金额
		var startamount=$("#startamount").val();
		if(startamount=="" || startamount==null){
			startamount=0;
		}
		var endamount=$("#endamount").val();
		if(endamount=="" || endamount==null){
			endamount=0;
		}
		if((startamount!="" && startamount!=null) || (endamount!="" && endamount!=null)){
			if(endamount=="" || endamount==null || startamount=="" || startamount==null){
				bootbox.alert("请输入金额范围！");
				return;
			}else if(Math.abs(startamount)>Math.abs(endamount)){
				bootbox.alert("结束金额不能小于开始金额！");
				return;
			}
		}
		parentstartAndEndTimeToArray = parentstartAndEndTimeToArray.toString();
		//获取审核状态
		var bkstatus=$("#bkstatus").val();
		//类型
		var typestatus=$("#typeStatus").val();
		
		$.ajax({
			type:"post",
			url:"/r/finoutstat/finoutstatflow",
			data:{
				"pageNo":CurPage,
				"accountid":accountidvalue,
				"toaccountowner":toaccountowner,
				"toaccount":toaccount,
				"parentstartAndEndTimeToArray":startAndEndTimeToArray,
				"parentfieldval":parentfieldval,
				"startamount":startamount,
				"endamount":endamount,
				"bkstatus":bkstatus,
				"typestatus":typestatus,
				"descOrAsc":descOrAsc,
				"orderBy":orderBy,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
					var tr = '';
					 //小计
					 var counts = 0;
					 var amounts=0;
					 var fees=0;
					 var date=jsonObject.data.arrlist[0];
					 accountCK=date.account;
					 
					 for(var index in jsonObject.data.arrlist){
						 var val = jsonObject.data.arrlist[index];
//						 var vtype="";
//						 if(val.type=="1"){
//							 vtype="支付宝";
//						 }else if(val.type=="2"){
//							 vtype="微信";
//						 }else if(val.type=="3"){
//							 vtype="银行卡";
//						 }else if(val.type=="4"){
//							 vtype="第三方";
//						 }else if(val.type=="5"){
//							 vtype="平账";
//						 }
                        tr += '<tr>'
                        	     //+'<td>' + val.account + '</td>'
                        	     +'<td>'+val.tradingtime+'</td>'
                        	     +'<td>'+val.createtime+'</td>'
                        	     +'<td>' + val.amount + '</td>' 
                        	     +'<td>' + val.balance + '</td>' 
                        	     +'<td>'+ val.toaccount +'</td>'
                        	     +'<td>'+val.toaccountowner+'</td>'
                        	     +'<td>'+val.summary+'</td>'
                        	     +'<td>'+val.remark+'</td>'
                        	     +'<td>' + _showBkStatus(val.status) +'</td>'
                        	     //'<td>' + vtype +'</td>'
                        	     //+'<td>' + val.fee + '</td>'
                        	    //抓不到交易流水号 +'<td>' +val.transactionno +'</td>'
                        	     +'<td>'
                        	        +'<button type="button" onclick="_details('+val.id+')" class="btn btn-xs btn-white btn-primary btn-bold">'
                                        +'详情'
                        	        +'</button>'
                        	     +'</td>'
                        	 +'</tr>';
                        counts +=1;
                        amounts+=val.amount;
                        fees+=val.fee;
                    };
					 $('#total_tbody').empty().html(tr);
					 var trs = '<tr>'
							         +'<td colspan="2">小计：'+counts+'</td>'
							         +'<td  bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
						             //+'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
						             +'<td colspan="7"></td>'
					          +'</tr>';
	                $('#total_tbody').append(trs);
	                var trn = '<tr>'
	                	         +'<td colspan="2">总计：'+jsonObject.data.page.totalElements+'</td>'
	                	         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[0]+'</td>'
	                	         //+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[1]+'</td>'
					             +'<td colspan="7"></td>'
					         +'</tr>';
	                $('#total_tbody').append(trn);
				}else{
					$('#total_tbody').empty().html('<tr></tr>');
				}
				//分页初始化
				showPading(jsonObject.data.page,"finOutStatFlowPage",queryFinOutStatFlow,'出款账号：'+hideAccountAll(accountCK));
			}
		});
		
	}
	
	function _showBkStatus(obj) {
	    var status = '';
	    switch (obj){
	        case 0:
	            status= "匹配中";
	            break;
	        case 1:
	            status= "已匹配";
	            break;
	        case 3:
	            status= "未认领";
	            break;
	        case 4:
	            status= "已处理";
	            break;
	        case 5:
	            status= "手续费";
	            break;
	    }
	    return status;
	}
	
	function orderByType(upOrDown,type){
		if(type=='trading'){
			if(upOrDown=='down'){
				$("#tradingDown").hide();
				$("#tradingUp").show();
				descOrAsc="asc";
				orderBy="tradingtime";
			}else{
				$("#tradingDown").show();
				$("#tradingUp").hide();
				descOrAsc="desc";
				orderBy="tradingtime";
			}
		}else{
			if(upOrDown=='down'){
				$("#grabDown").hide();
				$("#grabUp").show();
				descOrAsc="asc";
				orderBy="createtime";
			}else{
				$("#grabDown").show();
				$("#grabUp").hide();
				descOrAsc="desc";
				orderBy="createtime";
			}
		}
		queryFinOutStatFlow();
	}
	
	jQuery(function($) {
		initTimePickerHandicap();
		queryFinOutStatFlow();
		$("#back").attr("href","#/FinanceOutward:*?account="+parentaccount+"&startAndEndTime="+parentstartAndEndTime+"&fieldval="+parentfieldval
				       +"&parentzhCurPage="+parentzhCurPage+"&parentpkCurPage="+parentpkCurPage+"&accountOwner="+encodeURI(encodeURI(accountOwner))
				       +"&bankType="+encodeURI(encodeURI(bankType))+"&handicapAccount="+handicapAccount);
	})