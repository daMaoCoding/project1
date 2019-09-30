currentPageLocation = window.location.href;
	//查询按钮
	$('#searhByAccount').click(function () {
		queryFinOutStatSys();
    });
	
	//绑定按键事件，回车查询数据
	$('#auditTab').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryFinOutStatSys();
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
			queryFinOutStatSys();
	    });
	    exportStartAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
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
	//出款明细>系统明细
	var queryFinOutStatSys=function(){
		//在分页的地方显示出款账号
		var accountCK="";
		//获取账号
		var account=$("#account").val();
		//当前页码
		var CurPage=$("#finOutStatSysPage").find(".Current_Page").text();
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
			//记录父页面页码信息
			parentzhCurPage=request.zhCurPage;
			parentpkCurPage=request.pkCurPage;
		}
		//拼接父页面的时间
		var parentstartAndEndTimeToArray = new Array();
		if(parentstartAndEndTime){
			var ptstartAndEnd = parentstartAndEndTime.split(" - ");
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[0]));
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[1]));
			$("input[name='startAndEndTime']").val(parentstartAndEndTime);
		}
		parentstartAndEndTimeToArray = parentstartAndEndTimeToArray.toString();
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
		//获取审核状态
		var restatus=$("#restatus").val();
		//获取任务状态
		var tastatus=$("#tastatus").val();
		if((startamount!="" && startamount!=null) || (endamount!="" && endamount!=null)){
			if(endamount=="" || endamount==null || startamount=="" || startamount==null){
				bootbox.alert("请输入金额范围！");
				return;
			}else if(Math.abs(startamount)>Math.abs(endamount)){
				bootbox.alert("结束金额不能小于开始金额！");
				return;
			}
		}
		$.ajax({
			type:"post",
			url:"/r/finoutstat/finoutstatsys",
			data:{
				"pageNo":CurPage,
				"accountid":accountidvalue,
				"account":account,
				"parentstartAndEndTimeToArray":startAndEndTimeToArray,
				"parentfieldval":parentfieldval,
				"startamount":startamount,
				"endamount":endamount,
				"restatus":restatus,
				"tastatus":tastatus,
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
					 accountCK=date.accountname;
					 
					 for(var index in jsonObject.data.arrlist){
						 var val = jsonObject.data.arrlist[index];
						 var otname="";
						 if(val.operatorname==null)
							 otname="机器";
						 else
							 otname=val.operatorname;
						 if(val.restatus==1)
							 restatus="审核通过";
                        tr += '<tr>'
                        	     +'<td>' + val.handicapname + '</td>'
                        	     +'<td>'+ val.levelname +'</td>'
                        	     +'<td>' + val.member + '</td>'
                        	     +'<td>'+val.accountowner+'</td>'
                        	     +'<td>' + val.orderno + '</td>'
                        	     +'<td>' + _showReqStatus(val.restatus) + '</td>'
                        	     +'<td>' + _showTaskStatus(val.tastatus) + '</td>'
                        	     +'<td>' +Math.abs(val.amount) +'</td>'
                        	     +'<td>'+otname+'</td>'
                        	     +'<td>'+val.asigntime+'</td>'
                        	 +'</tr>';
                        counts +=1;
                        amounts+=val.amount;
                        fees+=val.fee;
                    };
					 $('#total_tbody').empty().html(tr);
					 var trs = '<tr>'
								 +'<td colspan="7">小计：'+counts+'</td>'
								 +'<td bgcolor="#579EC8" style="color:white;">'+Math.abs(amounts)+'</td>'
							     +'<td colspan="2"></td>'
							  +'</tr>';
	                $('#total_tbody').append(trs);
	                var trn = '<tr>'
			                	+'<td colspan="7">总计：'+jsonObject.data.page.totalElements+'</td>'
			                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.total[0]).toString().split(",")[0])+'</td>'
							    +'<td colspan="2"></td>'
							 +'</tr>';
	                $('#total_tbody').append(trn);
				}else{
					$('#total_tbody').empty().html('<tr></tr>');
				}
				//分页初始化
				showPading(jsonObject.data.page,"finOutStatSysPage",queryFinOutStatSys,'出款账号：'+hideAccountAll(accountCK));
			}
		});
		
	}
	
	jQuery(function($) {
		initTimePickerHandicap();
		queryFinOutStatSys();
		$("#back").attr("href","#/FinanceOutward:*?account="+parentaccount+"&startAndEndTime="+parentstartAndEndTime+"&fieldval="+parentfieldval
	      +"&parentzhCurPage="+parentzhCurPage+"&parentpkCurPage="+parentpkCurPage+"&accountOwner="+encodeURI(encodeURI(accountOwner))
	      +"&bankType="+encodeURI(encodeURI(bankType))+"&handicapAccount="+handicapAccount);
	})