currentPageLocation = window.location.href;
	/**
	*	初始化时间控件(按盘口统计)
	*/
	var initTimePickerHandicap=function(){
		var start =$('#startAndEndTime').val().split(" - ")[0];
	    var end = $('#startAndEndTime').val().split(" - ")[1];
	    
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
	    
	  //如果为空则赋一个值  要不时间显示不了
	    if(!start)
	    	start=moment().hours(07).minutes(0).seconds(0);
	    if(!end)
	    	end=moment().hours(07).minutes(0).seconds(0);
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
		}).val($('#startAndEndTime').val());
		startAndEndTime.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定的时候把单选按钮的checked清空
			var rObj = document.getElementsByName("form-field-checkbox");
			for(var i = 0;i < rObj.length;i++){
				if(rObj[i].checked == true){
					rObj[i].checked=false;
					break;
				}
			}
			//清空的同时查询数据
			$('#searhByCondition').click();
		});
		startAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}
	
	
	//清空账号统计StartAndEndTime的值
	function clearStartAndEndTime(){
		$("input[name='startAndEndTime']").val('');
		//点击直接查询
		queryFinMoreLevelStat();
	}
	
	//查询按钮  根据值判断查询不同的数据源
	$('#searhByCondition').click(function () {
		queryFinMoreLevelStat();
    });
	
	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryFinMoreLevelStat();
	    }
	}
	
	function initialization(handicap){
		//初始化盘口
		var $selectHandicap = $("select[name='handicap']").html("");
		$.ajax({dataType:'json',type:"get",url:"/r/handicap/list",data:{enabled:1},success:function(jsonObjectHandicap){
			if(jsonObjectHandicap.status == 1){
				$selectHandicap.html("");
				$('<option></option>').html('全部').attr('value','').attr("handicapCode","").appendTo($selectHandicap);
				for(var index in jsonObjectHandicap.data){
					var item = jsonObjectHandicap.data[index];
					if(item.id==handicap){
						$('<option></option>').html(item.name).attr('value',item.id).attr('selected','selected').attr("handicapCode",item.code).appendTo($selectHandicap);
						break;
					}
				}
				$("#handicap").attr("disabled",true);
			}
		}});
		
		//初始化层级
		if($("#level").val()=="" || $("#level").val()==null){
			var $selectLevel =  $("select[name='level']").html('');
			$.ajax({dataType:'json',
				type:"get",url:"/r/finoutstat/findbyhandicapid",
				data:{"handicap":handicap},
				success:function(jsonObject){
					if(jsonObject.status == 1){
						$selectLevel.html('');
						$('<option></option>').html('全部').attr('value','').attr('selected','selected').attr("levelCode","").appendTo($selectLevel);
						for(var index in jsonObject.data){
							var item = jsonObject.data[index];
							$('<option></option>').html(item.name).attr('value',item.id).attr("levelCode",item.code).appendTo($selectLevel);
						}
					}
			}});	
		}
		
	}

	//出入财务汇总>明细
	var handicap=0;
	var parenthandicap="";
	var parentstartAndEndTime="";
	var parentfieldval="";
	//记录父页面页码
	var parentCurPage=""; 
	var queryFinMoreLevelStat=function(){
		$("#loadingModal").modal('show');
		//当前页码
		var CurPage=$("#finMoreStatLevelPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//获取层级
		var level=$("#level").val();
		if(level=="" || level==null){
			level=0;
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
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		//拼接父页面的时间
		var parentstartAndEndTimeToArray = new Array();
		if(parentstartAndEndTime){
			var ptstartAndEnd = parentstartAndEndTime.split(" - ");
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[0]));
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[1]));
		}
		parentstartAndEndTimeToArray = parentstartAndEndTimeToArray.toString();
		$.ajax({
			type:"post",
			url:"/r/finmorestat/finmorelevelstat",
			data:{
				"pageNo":CurPage,
				"handicap":handicap,
				"level":level,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"parentstartAndEndTimeToArray":parentstartAndEndTimeToArray,
				"parentfieldval":parentfieldval,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
						 var tr = '';
						 //小计
						 var counts = 0;
						 var amountinbalance=0;
						 var amountinactualamount=0;
						 var countoutfee=0;
						 var amountoutbalance=0;
						 var amountoutactualamount=0;
						 var totalprofit=0;
						 for(var index in jsonObject.data.arrlist){
							 var val = jsonObject.data.arrlist[index];
	                        tr += '<tr>'
			                        	+'<td>' + val.handicapname + '</td>'
			                        	+'<td>'+val.levelname+'</td>'
			                        	+'<td>'+ val.countinps +'</td>'
			                        	+'<td>' + val.countin + '</td>'
			                        	+'<td style="display: none;">'+0+'</td>'
			                        	+'<td>' + val.amountinbalance + '</td>'
			                        	+'<td>'+val.amountinactualamount+'</td>'
			                        	+'<td>' + val.countoutps +'</td>'
			                        	+'<td>' + val.countout + '</td>'
			                        	+'<td>' + val.amountoutbalance + '</td>'
			                        	+'<td>' + val.amountoutactualamount + '</td>'
			                        	+'<td>' + 0+ '</td>'
			                        	+'<td>' + 0 + '</td>'
			                        	+'<td>'+val.profit+'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        amountinbalance+=val.amountinbalance;
	                        amountinactualamount+=val.amountinactualamount;
	                        countoutfee+=val.countoutfee;
	                        amountoutbalance+=val.amountoutbalance;
	                        amountoutactualamount+=val.amountoutactualamount;
	                        totalprofit+=val.profit;
	                    };
						 $('#total_tbody').empty().html(tr);
//						 var trs = '<tr>'
//										 +'<td colspan="4">小计：'+counts+'</td>'
//										 +'<td bgcolor="#579EC8" style="color:white;">'+amountinbalance.toFixed(2)+'</td>'
//										 +'<td bgcolor="#579EC8" style="color:white;">'+amountinactualamount.toFixed(2)+'</td>'
//									     +'<td></td>'
//									     +'<td></td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+countoutfee.toFixed(2)+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+amountoutbalance.toFixed(2)+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+amountoutactualamount.toFixed(2)+'</td>'
//									     +'<td></td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+0+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+totalprofit.toFixed(2)+'</td>'
//						          +'</tr>';
//	                    $('#total_tbody').append(trs);
	                    var trn = '<tr>'
				                    	+'<td colspan="4">总计：'+jsonObject.data.page.totalElements+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+amountinbalance.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+amountinactualamount.toFixed(2)+'</td>'
								        +'<td></td>'
								        +'<td></td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+amountoutbalance.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+amountoutactualamount.toFixed(2)+'</td>'
								        +'<td></td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+0+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+totalprofit.toFixed(2)+'</td>'
					             +'</tr>';
	                    $('#total_tbody').append(trn);
				}else{
					//$('#total_tbody').empty().html('<tr></tr>');
					$('#total_tbody').empty().html('<tr><td style="margin-bottom:0px;font-size: 30px;" class="alert alert-success center" colspan="14">无数据</td></tr>');
				}
				$("#loadingModal").modal('hide');
				//分页初始化
				//showPading(jsonObject.data.page,"finMoreStatLevelPage",queryFinMoreLevelStat);
			}
		});
		
	}
	
	
	
	jQuery(function($) {
		//获取传过来的盘口信息
		var request=getRequest();
		if(request&&request.handicap){
			handicap=request.handicap;
			//赋值公共的变量，返回的时候进行传参。判断需要查询的数据，已经参数的值显示
			parenthandicap=request.handicapvalue;
			parentstartAndEndTime=request.startAndEndTime;
			parentfieldval=request.fieldval;
			parentCurPage=request.CurPage;
		}
		$("input[name='startAndEndTime']").val(parentstartAndEndTime);
		initTimePickerHandicap();
		//把父页面的时间带进子页面赋值去查询
		var rObj = document.getElementsByName("form-field-checkbox");
		//处理父页面单选按钮被清空后，undefined的情况
		if(request.fieldval=='undefined'){
			for(var i = 0;i < rObj.length;i++){
				if(rObj[i].checked == true){
					rObj[i].checked=false;
					break;
				}
			}
		}else{
			for(var i = 0;i < rObj.length;i++){
				if(rObj[i].value == parentfieldval){
					rObj[i].checked = 'checked';
					break;
				}
			}
		}
		queryFinMoreLevelStat();
		//handicap  父页面传过来的盘口信息
		initialization(handicap);
		$("#back").attr("href","#/FinMoreStat:*?parenthandicap="+parenthandicap+"&parentstartAndEndTime="+parentstartAndEndTime+"&parentfieldval="+parentfieldval+"&parentCurPage="+parentCurPage);
	})