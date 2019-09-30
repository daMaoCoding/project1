currentPageLocation = window.location.href;
	/**
	*	初始化时间控件(按盘口统计)
	*/
	var initTimePickerHandicap=function(){
		var start =!$('#startAndEndTime').val()?moment().add(-1,'days').hours(07).minutes(0).seconds(0):$('#startAndEndTime').val().split(" - ")[0];
	    var end = !$('#startAndEndTime').val()?moment().hours(06).minutes(59).seconds(59):$('#startAndEndTime').val().split(" - ")[1];
		
	    var todayStart = '';
	    if ((moment()>=moment().hours(07).minutes(0).seconds(0) && moment()< moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
	        todayStart = moment().hours(07).minutes(0).seconds(0);
	    }
	    var yestStart = '', yestEnd = '';
	    if ((moment()>=moment().hours(07).minutes(0).seconds(0) && moment()< moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
	        yestStart = moment().hours(07).minutes(0).seconds(0).subtract('days', 1);
	        yestEnd = moment().add(1, 'days').hours(06).minutes(59).seconds(59).subtract('days', 1);
	    }
	    if ((moment()>=moment().hours(0).minutes(0).seconds(0) && moment()<moment().hours(07).minutes(0).seconds(0) )){
	        yestStart = moment().subtract(1,'days').hours(07).minutes(0).seconds(0).subtract('days', 1);
	        yestEnd = moment().hours(06).minutes(59).seconds(59).subtract('days', 1);
	    }
	    var near7Start = '', near7End = '';
	    if ((moment()>=moment().hours(07).minutes(0).seconds(0) && moment()< moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
	        near7Start = moment().hours(07).minutes(0).seconds(0).subtract('days', 6);
	        near7End = moment().add(1, 'days').hours(06).minutes(59).seconds(59);
	    }
	    if ((moment()>=moment().hours(0).minutes(0).seconds(0) && moment()<moment().hours(07).minutes(0).seconds(0) )){
	        near7Start = moment().subtract(1,'days').hours(07).minutes(0).seconds(0).subtract('days', 6);
	        near7End = moment().hours(06).minutes(59).seconds(59);
	    }
	    
	    var startAndEndTime = $('input.date-range-picker').daterangepicker({
			autoUpdateInput:false,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
	        ranges : {
	            '最近1小时': [moment().subtract('hours',1), moment()],
	            '今日': [todayStart, moment()],
	            '昨日': [yestStart, yestEnd],
	            '最近7日': [near7Start, near7End]
	        },
			locale: {
				"format": 'YYYY-MM-DD HH:mm:ss',
				"separator": " - ",
				"applyLabel": "确定",
				"cancelLabel": "取消",
				"fromLabel": "从",
				"toLabel": "到",
				"customRangeLabel": "自定义",
				"dayNames": ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
				"daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
				"monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
				"firstDay": 1
			}
		}).val(!$('#startAndEndTime').val()?(start.format('YYYY-MM-DD HH:mm:ss')+' - '+end.format('YYYY-MM-DD HH:mm:ss')):$('#startAndEndTime').val());
		startAndEndTime.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确实时查询数据
			$('#searhByCondition').click();
		});
		startAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}	
	

	//查询按钮  
	$('#searhByCondition').click(function () {
		queryfinTransBalanceSys();
    });
	
	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryfinTransBalanceSys();
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

	var id=0;
	var parentaccount="";
	var parentCurPage="";
	//余额明细>明细>系统明细
	var queryfinTransBalanceSys=function(){
		//当前页码
		var CurPage=$("#Page").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		var request=getRequest();
		var accountid=0;
		if(request&&request.accountid){
			accountid=request.accountid;
			//返回时，查询不同数据源的参考值
			id=request.id;
			//返回时  初始化查询条件
			parentaccount=request.account;
			parentCurPage=request.CurPage;
		}
		//汇入账号
		var to_account=$("#to_account").val();
		//汇出账号
		var from_account=$("#from_account").val();
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
		if((startamount!="" && startamount!=null)||(endamount!="" && endamount!=null)){
			if(startamount==0){
				bootbox.alert("不能以0！");
				return;
			}
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
			url:"/r/finbalancestat/finTransBalanceSys",
			data:{
				"pageNo":CurPage,
				"to_account":to_account,
				"from_account":from_account,
				"startAndEndTime":startAndEndTimeToArray,
				"startamount":startamount,
				"endamount":endamount,
				"accountid":accountid,
				"id":id,
				"type":"bank",
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
						var tr = '';
						 //小计
						 var counts = 0;
						 var amounts=0;
						 var fees=0;
						 for(var index in jsonObject.data.arrlist){
							 var val = jsonObject.data.arrlist[index];
	                        tr += '<tr>'
			                        	+'<td>' + val.fromaccountname + '</td>'
			                        	+'<td>'+ val.toaccountname+'</td>'
			                        	+'<td>' + Math.abs(val.amount) + '</td>'
			                        	+'<td>'+val.fee+'</td>'
			                        	+'<td>' + val.createtime +'</td>'
			                        	//+'<td>' + val.remark +'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        amounts+=val.amount;
	                        fees+=val.fee;
	                    };
						 $('#total_tbody').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="2">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+Math.abs(amounts)+'</td>'
									     +'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
		                $('#total_tbody').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="2">总计：'+jsonObject.data.page.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.total[0]).toString().split(",")[0])+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[1]+'</td>'
									    +'<td colspan="2"></td>'
						         +'</tr>';
		                $('#total_tbody').append(trn);
				}else{
					$('#total_tbody').empty().html('<tr></tr>');
				}
				//分页初始化
				showPading(jsonObject.data.page,"Page",queryfinTransBalanceSys);
			}
		});
		
	}
	
	
	jQuery(function($) {
		initTimePickerHandicap();
		queryfinTransBalanceSys();
		$("#back").attr("href","#/finBalanceStatMatch:*?type="+id+"&parentaccount="+parentaccount+"&parentCurPage="+parentCurPage);
	})