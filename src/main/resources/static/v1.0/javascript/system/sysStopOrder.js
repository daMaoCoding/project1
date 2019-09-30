	$('#searhByCondition').click(function () {
		queryStopOrder();
    });

	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryStopOrder();
	    }
	}
	
	
	/**
	*	初始化时间控件(按账号统计)
	*/
	var initTimePickerr=function(){
		
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
		}).val('');
		startAndEndTime.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
          //清空的同时查询数据
            $('#searhByCondition').click();
		});
		startAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}
	
	//余额明细>明细
	var queryStopOrder=function(){
		//当前页码
		var CurPage=$("#stopOrderPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//获取账号
		var username=$("#username").val();
		//获取类型
		var type=$("#stopOrderType").val();
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTime']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		
		$.ajax({
			type:"post",
			url:"/r/income/searstoporder",
			data:{
				"pageNo":CurPage,
				"username":username,
				"type":type,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
						 var tr = '';
						 for(var index in jsonObject.data.arrlist){
							 var val = jsonObject.data.arrlist[index];
	                        tr += '<tr>'
			                        	+'<td>'+ val.username +'</td>'
			                        	+'<td>'+ val.createtime +'</td>'
			                        	+'<td>'+ val.remark +'</td>'
			                        	+'<td>'+ val.type +'</td>'
                                 +'</tr>';
	                    };
						 $('#total_tbody').empty().html(tr);
	                    var trn = '<tr>'
				                    	+'<td colspan="4" >总计：'+jsonObject.data.page.totalElements+'</td>'
	                    	     +'</tr>';
	                    $('#total_tbody').append(trn);
				}else{
					$('#total_tbody').empty().html('<tr></tr>');
				}
				//分页初始化
				showPading(jsonObject.data.page,"stopOrderPage",queryStopOrder);
			}
		});
		
	}
	
	
	
	jQuery(function($) {
		initTimePickerr();
		queryStopOrder();
	})