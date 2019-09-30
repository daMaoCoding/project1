currentPageLocation = window.location.href;
/**
	*	初始化时间控件(按盘口统计)
	*/
	var initTimePickerHandicap=function(){
		var start =(!$('#startAndEndTime').val()&&!$('input:radio[name="form-field-checkbox"]:checked').val())?moment().add(-1,'days').hours(07).minutes(0).seconds(0):$('#startAndEndTime').val().split(" - ")[0];
	    var end = (!$('#startAndEndTime').val()&&!$('input:radio[name="form-field-checkbox"]:checked').val())?moment().hours(06).minutes(59).seconds(59):$('#startAndEndTime').val().split(" - ")[1];	
	  
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
	    	start=moment().add(-1,'days').hours(7).minutes(0).seconds(0);
	    if(!end)
	    	end=moment().hours(06).minutes(59).seconds(59);
	    var startAndEndTime = $('input.date-range-picker').daterangepicker({
			autoUpdateInput:false,
			timePicker: false, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
	        opens : 'right', //日期选择框的弹出位置
			locale: {
				"format": 'YYYY-MM-DD',
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
		}).val((!$('#startAndEndTime').val()&&!$('input:radio[name="form-field-checkbox"]:checked').val())?(start.format('YYYY-MM-DD')+' - '+end.format('YYYY-MM-DD')):$('#startAndEndTime').val());
		startAndEndTime.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD') + ' - ' + picker.endDate.format('YYYY-MM-DD'));
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
	
	//初始化盘口
//	var inithancipad=function(){
//		var $selectHandicap = $("select[name='handicap']").html("");
//		$.ajax({dataType:'json',
//			type:"get",
//			async:false,
//			url:"/r/handicap/list",
//			data:{enabled:1},
//			success:function(jsonObject){
//			if(jsonObject.status == 1){
//				$selectHandicap.html("");
//				$('<option></option>').html('全部').attr('value','').attr('selected','selected').attr("handicapCode","").appendTo($selectHandicap);
//				for(var index in jsonObject.data){
//					var item = jsonObject.data[index];
//					$('<option></option>').html(item.name).attr('value',item.id).attr("handicapCode",item.code).appendTo($selectHandicap);
//				}
//			}
//		}});
//	}
	
	var inithancipad=function(){
	    $.ajax({
	        type: 'get',
	        url: '/r/out/handicap',
	        data: {},
	        async: false,
	        dataType: 'json',
	        success: function (res) {
	            if (res) {
	                if (res.status == 1 && res.data) {
	                    var opt = '<option value="0">全部</option>';
	                    $(res.data).each(function (i, val) {
	                        opt += '<option value="' + $.trim(val.id) + '">' + val.name + '</option>';
	                    });
	                    $('#handicap').empty().html(opt);
	                    $('#handicap').trigger('chosen:updated');
	                }
	            }
	        }
	    });
	}
	
	
	var inithancipadReal=function(){
	    $.ajax({
	        type: 'get',
	        url: '/r/out/handicap',
	        data: {},
	        async: false,
	        dataType: 'json',
	        success: function (res) {
	            if (res) {
	                if (res.status == 1 && res.data) {
	                    var opt = '<option value="0">全部</option>';
	                    $(res.data).each(function (i, val) {
	                        opt += '<option value="' + $.trim(val.id) + '">' + val.name + '</option>';
	                    });
	                    $('#realTime_handicap').empty().html(opt);
	                    $('#realTime_handicap').trigger('chosen:updated');
	                }
	            }
	        }
	    });
	}
	//清空账号统计StartAndEndTime的值
	function clearStartAndEndTime(){
		$("input[name='startAndEndTime']").val('');
		//点击直接查询
		queryFinMoreStat();
	}
	
	//查询按钮  根据值判断查询不同的数据源
	$('#searhByCondition').click(function () {
		queryFinMoreStat();
    });
	
	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryFinMoreStat();
	    }
	}
	
	
	var request=getRequest();
	//出入财务汇总
	var queryFinMoreStat=function(){
		//显示
		$("#loadingModal").modal('show');
		//当前页码
		var CurPage=$("#historyTimePage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		
		//如果子页面传过来的 分页页数不为空
		if(request&&request.parentCurPage){
			CurPage=request.parentCurPage;
		}
		
		//获取盘口
		var handicap=$("#handicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTime']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0])+" 07:00:00");
			startAndEndTimeToArray.push($.trim(startAndEnd[1])+" 06:59:59");
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		var url="";
		if("today"==fieldval){
			url="/r/finmorestat/finmorestatFromClearDateRealTime";
		}else{
			url="/r/finmorestat/finmorestatFromClearDate";
		}
		$.ajax({
			type:"post",
			//url:"/r/finmorestat/finmorestat",
			url:url,
			data:{
				"pageNo":CurPage,
				"handicap":handicap,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
					     //查询成功后，把子页面带过来的当前页码置空
				         request.parentCurPage=null
						 //小计
						 var counts = 0;
						 var amountinactualamount=0;
						 var countoutfee=0;
						 var amountoutactualamount=0;
						 var totalprofit=0;
						 var totalMaoProfit=0;
						 var theLoss=0;
						 var profit=0;
						 var maoProfit=0;
						 var incomePerpeo=0;
						 var incomeCounts=0;
						 var thirdCounts=0;
						 var thirdFee=0;
						 var outPerpeo=0;
						 var outCounts=0;
						 var freezeCounts=0;
						 var freezeAmount=0;
						 var lossCounts=0;
						 var $tbody=$('#historyTime_tbody').empty();
						 for(var index in jsonObject.data.arrlist){
							 var val = jsonObject.data.arrlist[index];
							 profit=val.income-val.incomeFee-Math.abs(val.fee)-val.outwardSys-val.los-(val.freezeAmounts==null?0:val.freezeAmounts);
							 maoProfit=val.income-val.outwardSys;
							 var tr = '';
							 tr += '<tr>'
			                        	+'<td>' + val.handicapName + '</td>'
			                        	+'<td>'+ val.incomePerson +"/"+val.incomeCount+'</td>'
			                        	+"<td> <a class='bind_hover_card breakByWord' data-toggle='money" + val.handicapName + "' data-placement='auto right' data-trigger='hover'>" + val.income.toFixed(2) + "</a></td>"
			                        	+'<td>'+val.outwardPerson+"/"+val.outwardSysCount+'</td>'
			                        	+'<td>' + val.outwardSys + '</td>'
			                        	+'<td><a onclick="showIncomFee('+val.handicapId+',\''+val.handicapName+'\')">' + val.thirdIncomeCount+"/"+val.incomeFee.toFixed(2) + '</a></td>'
			                        	+'<td>' + Math.abs(val.fee) +'</td>'
			                        	+'<td>' + val.losCount+"/"+val.los.toFixed(2) + '</td>'
			                        	+'<td><a onclick="showFreezeCard('+val.handicapId+',\''+val.handicapName+'\')">'+ val.freezeCardCount +"/"+(val.freezeAmounts==null?0:val.freezeAmounts)+ '</a></td>'
			                        	+'<td>'+maoProfit.toFixed(2)+'</td>'
			                        	+'<td>'+profit.toFixed(2)+'</td>'
			                        	//+'<td>'
			                        	    // +'<a href="#/finMoreStatMatch:*?handicap='+val.handicapId+'&handicapvalue='+handicap+'&startAndEndTime='+startAndEndTime+'&fieldval='+fieldval+'&CurPage='+CurPage+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                                        // +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
			                        	//+'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        incomePerpeo+=val.incomePerson;
	                        incomeCounts+=val.incomeCount;
	                        thirdCounts+=val.thirdIncomeCount;
	                        thirdFee+=val.incomeFee;
	                        outPerpeo+=val.outwardPerson;
	                        outCounts+=val.outwardSysCount;
	                        amountinactualamount+=val.income;
	                        countoutfee+=Math.abs(val.fee);
	                        freezeCounts+=val.freezeCardCount;
	                        freezeAmount+=(val.freezeAmounts==null?0:val.freezeAmounts);
	                        amountoutactualamount+=val.outwardSys;
	                        theLoss+=val.los;
	                        lossCounts+=val.losCount;
	                        totalprofit+=profit;
	                        totalMaoProfit+=maoProfit;
	                        $tbody.append($(tr));
	                        loadHover_accountTodayInto(val.handicapName,val.companyIncome,val.thirdIncome);
	                    };
//						 var trs = '<tr>'
//										 +'<td colspan="3">小计：'+counts+'</td>'
//										 +'<td bgcolor="#579EC8" style="color:white;">'+amountinbalance.toFixed(2)+'</td>'
//										 +'<td bgcolor="#579EC8" style="color:white;">'+amountinactualamount.toFixed(2)+'</td>'
//									     +'<td></td><td></td><td bgcolor="#579EC8" style="color:white;">'+countoutfee+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+amountoutbalance+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+amountoutactualamount+'</td>'
//									     +'<td></td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+0+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+totalprofit.toFixed(2)+'</td>'
//									     +'<td></td>'
//						          +'</tr>';
//	                    $('#total_tbody').append(trs);
	                    var trn = '<tr>'
				                    	+'<td></td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+incomePerpeo+'/'+incomeCounts+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+amountinactualamount.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+outPerpeo+'/'+outCounts+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+amountoutactualamount+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+thirdCounts+'/'+thirdFee.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+countoutfee+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+lossCounts+"/"+theLoss.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+freezeCounts+'/'+freezeAmount.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+totalMaoProfit.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+totalprofit.toFixed(2)+'</td>'
								        +'<td></td>'
					             +'</tr>';
	                    $('#historyTime_tbody').append(trn);
				}else{
					//$('#total_tbody').empty().html('<tr></tr>');
					$('#historyTime_tbody').empty().html('<tr><td style="margin-bottom:0px;font-size: 30px;" class="alert alert-success center" colspan="14">无数据</td></tr>');
				}
				//$("[data-toggle='popover']").popover();
				//隐藏
				$("#loadingModal").modal('hide');
				//分页初始化
				//showPading(jsonObject.data.page,"finMorePage",queryFinMoreStat);
			}
		});
		
	}
	
	
	//出入财务汇总
	var queryRealTime=function(){
		//显示
		$("#loadingModal").modal('show');
		//当前页码
		var CurPage=$("#realTimePage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		
		//如果子页面传过来的 分页页数不为空
		if(request&&request.parentCurPage){
			CurPage=request.parentCurPage;
		}
		
		//获取盘口
		var handicap=$("#realTime_handicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		$.ajax({
			type:"post",
			//url:"/r/finmorestat/finmorestat",
			url:"/r/finmorestat/finmorestatFromClearDateRealTime",
			data:{
				"pageNo":CurPage,
				"handicap":handicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
					     //查询成功后，把子页面带过来的当前页码置空
				         request.parentCurPage=null
						 //小计
						 var counts = 0;
						 var amountinactualamount=0;
						 var countoutfee=0;
						 var amountoutactualamount=0;
						 var totalprofit=0;
						 var totalMaoProfit=0;
						 var theLoss=0;
						 var profit=0;
						 var maoProfit=0;
						 var incomePerpeo=0;
						 var incomeCounts=0;
						 var thirdCounts=0;
						 var thirdFee=0;
						 var outPerpeo=0;
						 var outCounts=0;
						 var freezeCounts=0;
						 var freezeAmount=0;
						 var lossCounts=0;
						 var $tbody=$('#realTime_tbody').empty();
						 for(var index in jsonObject.data.arrlist){
							 var val = jsonObject.data.arrlist[index];
							 profit=val.income-val.incomeFee-Math.abs(val.fee)-val.outwardSys-val.los-(val.freezeAmounts==null?0:val.freezeAmounts);
							 maoProfit=val.income-val.outwardSys;
							 var tr = '';
							 tr += '<tr>'
			                        	+'<td>' + val.handicapName + '</td>'
			                        	+'<td>'+ val.incomePerson +"/"+val.incomeCount+'</td>'
			                        	+"<td> <a class='bind_hover_card breakByWord' data-toggle='money" + val.handicapName + "' data-placement='auto right' data-trigger='hover'>" + val.income.toFixed(2) + "</a></td>"
			                        	+'<td>'+val.outwardPerson+"/"+val.outwardSysCount+'</td>'
			                        	+'<td>' + val.outwardSys + '</td>'
			                        	+'<td><a onclick="showIncomFee('+val.handicapId+',\''+val.handicapName+'\')">' + val.thirdIncomeCount+"/"+val.incomeFee.toFixed(2) + '</a></td>'
			                        	+'<td>' + Math.abs(val.fee) +'</td>'
			                        	+'<td>' + val.losCount+"/"+val.los.toFixed(2) + '</td>'
			                        	+'<td><a onclick="showFreezeCard('+val.handicapId+',\''+val.handicapName+'\')">'+ val.freezeCardCount +"/"+(val.freezeAmounts==null?0:val.freezeAmounts)+ '</a></td>'
			                        	+'<td>'+maoProfit.toFixed(2)+'</td>'
			                        	+'<td>'+profit.toFixed(2)+'</td>'
			                        	//+'<td>'
			                        	    // +'<a href="#/finMoreStatMatch:*?handicap='+val.handicapId+'&handicapvalue='+handicap+'&startAndEndTime='+startAndEndTime+'&fieldval='+fieldval+'&CurPage='+CurPage+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                                        // +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
			                        	//+'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        incomePerpeo+=val.incomePerson;
	                        incomeCounts+=val.incomeCount;
	                        thirdCounts+=val.thirdIncomeCount;
	                        thirdFee+=val.incomeFee;
	                        outPerpeo+=val.outwardPerson;
	                        outCounts+=val.outwardSysCount;
	                        amountinactualamount+=val.income;
	                        countoutfee+=Math.abs(val.fee);
	                        freezeCounts+=val.freezeCardCount;
	                        freezeAmount+=(val.freezeAmounts==null?0:val.freezeAmounts);
	                        amountoutactualamount+=val.outwardSys;
	                        theLoss+=val.los;
	                        lossCounts+=val.losCount;
	                        totalprofit+=profit;
	                        totalMaoProfit+=maoProfit;
	                        $tbody.append($(tr));
	                        loadHover_accountTodayInto(val.handicapName,val.companyIncome,val.thirdIncome);
	                    };
//						 var trs = '<tr>'
//										 +'<td colspan="3">小计：'+counts+'</td>'
//										 +'<td bgcolor="#579EC8" style="color:white;">'+amountinbalance.toFixed(2)+'</td>'
//										 +'<td bgcolor="#579EC8" style="color:white;">'+amountinactualamount.toFixed(2)+'</td>'
//									     +'<td></td><td></td><td bgcolor="#579EC8" style="color:white;">'+countoutfee+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+amountoutbalance+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+amountoutactualamount+'</td>'
//									     +'<td></td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+0+'</td>'
//									     +'<td bgcolor="#579EC8" style="color:white;">'+totalprofit.toFixed(2)+'</td>'
//									     +'<td></td>'
//						          +'</tr>';
//	                    $('#total_tbody').append(trs);
	                    var trn = '<tr>'
				                    	+'<td></td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+incomePerpeo+'/'+incomeCounts+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+amountinactualamount.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+outPerpeo+'/'+outCounts+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+amountoutactualamount+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+thirdCounts+'/'+thirdFee.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+countoutfee+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+lossCounts+"/"+theLoss.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+freezeCounts+'/'+freezeAmount.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+totalMaoProfit.toFixed(2)+'</td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+totalprofit.toFixed(2)+'</td>'
								        +'<td></td>'
					             +'</tr>';
	                    $('#realTime_tbody').append(trn);
				}else{
					//$('#total_tbody').empty().html('<tr></tr>');
					$('#realTime_tbody').empty().html('<tr><td style="margin-bottom:0px;font-size: 30px;" class="alert alert-success center" colspan="14">无数据</td></tr>');
				}
				//$("[data-toggle='popover']").popover();
				//隐藏
				$("#loadingModal").modal('hide');
				//分页初始化
				//showPading(jsonObject.data.page,"finMorePage",queryFinMoreStat);
			}
		});
		
	}
	
	
	
	function showFreezeCard(handicapId,handicapName){
		$("#processedHandicap").val(handicapId);
		$("#handicapName").val(handicapName);
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		$("[name=processedDate][value="+fieldval+"]").prop("checked",true);
		queryFinprocessed();
		$('#tabProcessed').modal('show');
	}
	
	function showIncomFee(handicapId,handicapName){
		$("#thirdHandicap").val(handicapId);
		$("#thirdHandicapName").val(handicapName);
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		$("[name=thirdTime][value="+fieldval+"]").prop("checked",true);
		queryFinInStatThethirdparty(0);
		$('#thirdIncom').modal('show');
	}
	
	function clearProcessed(){
		$("input[name='processedStartAndEndTimefrostless']").val('');
		//点击直接查询
		queryFinprocessed();
	}
	
	function serThird(){
		$("input[name='thirdStartAndEndTime']").val('');
		//点击直接查询
		queryFinInStatThethirdparty(0);
	}

//入款明细第三方
var queryFinInStatThethirdparty=function(nb){
	//当前页码
	var CurPage=$("#fininStatThethirdpartyPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//如果子页面传过来的 分页页数不为空
	if(request&&request.parentthirdCurPage){
		CurPage=request.parentthirdCurPage;
	}
	dsf=CurPage;
	//获取盘口
	var handicap=$("#thirdHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	//获取层级
	var level=$("#level").val();
	var handicapname=$("#level").find("option:selected").text();
	if(level=="" || level==null){
		level=0;
		handicapname="";
	}
	//收款账号
	var account=$("#account").val();
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='thirdStartAndEndTime']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//获取查询范围
	var fieldval=$('input:radio[name="thirdTime"]:checked').val();
	$.ajax({
		type:"post",
		url:"/r/fininstat/fininstatistical",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"level":level,
			"account":account,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"fieldval":fieldval,
			"type":"Thethirdparty",
			"handicapname":handicapname,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.thirdpartyarrlist.length > 0){
				var idList=new Array();
				//查询成功，把子页面传过来的翻页 页数清空
			     request.parentthirdCurPage=null;
				 var tr = '';
				 //小计
				 var counts = 0;
				 var amounts=0;
				 var fees=0;
				 for(var index in jsonObject.data.thirdpartyarrlist){
					 var val = jsonObject.data.thirdpartyarrlist[index];
					 idList.push({'id':val.id});
					 var levelnames=val.levelname;
                     tr += '<tr>'
                    	 		+'<td>' + (counts+1) + '</td>'
                        	    +'<td>' + val.handicapname + '</td>'
                        	    +'<td>'+ levelnames +'</td>'
                        	    +"<td>" 
	                        	    +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+val.account
	                        	    +"</a>"
                        	    +"</td>"
	                        	+'<td>' + val.bankBalance + '</td>'
	                        	+'<td>' + val.counts+"/"+val.amount + '</td>'
	                        	+'<td>'+val.fee.toFixed(2)+'</td>'
                    	 +'</tr>';
                    counts +=1;
                    amounts+=val.amount;
                    fees+=val.fee;
                };
				 $('#total_tbody_Thethirdparty').empty().html(tr);
				 var trs = '<tr>'
								 +'<td colspan="5">小计：'+counts+'</td>'
								 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
							     +'<td bgcolor="#579EC8" style="color:white;">'+fees.toFixed(2)+'</td>'
						  +'</tr>';
	                $('#total_tbody_Thethirdparty').append(trs);
	                var trn = '<tr>'
				                	+'<td colspan="5">总计：'+jsonObject.data.thirdpartyPage.totalElements+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[0]+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[1]+'</td>'
					         +'</tr>';
	                $('#total_tbody_Thethirdparty').append(trn);
				}else{
					$('#total_tbody_Thethirdparty').empty().html('<tr></tr>');
				}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.thirdpartyPage,"fininStatThethirdpartyPage",queryFinInStatThethirdparty);
		}
	});
	
}
	
	
	
var queryFinprocessed=function(){
	//当前页码
	var CurPage=$("#processedPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#processedHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	
	//获取账号类型
	var cartype=$('input:radio[name="processedCarType"]:checked').val();
//			//获取层级
//			var level=$("#frostlevel").val();
//			if(level=="" || level==null){
//				level=0;
//			}
	//账号
	var account=$("#processedAccount").val();
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='processedStartAndEndTimefrostless']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//获取查询范围
	var fieldval=$('input:radio[name="processedDate"]:checked').val();
	var statusType=$('input:radio[name="statusType"]:checked').val();
	var jdType=$('input:radio[name="processedfrostType"]:checked').val();
	$.ajax({
		type:"post",
		url:"/r/finLessStat/finpending",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"level":0,
			"accounttype":0,
			"account":account,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"fieldval":fieldval,
			"type":"frostless",
			"cartype":cartype,
			"status":"6",
			"disposeType":"processed",
			"statusType":statusType,
			"jdType":jdType,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.frostarrlist.length > 0){
					var tr = '';
					 //小计
					 var counts = 0;
					 var bankAmounts=0;
					 var lossAmounts=0;
					 var existingAmount=0;
					 var idList=new Array();
					 for(var index in jsonObject.data.frostarrlist){
						 var val = jsonObject.data.frostarrlist[index];
						 idList.push({'id':val.id});
						 var vtype="";
						 if(val.type=="1"){
							 vtype="入款卡";
						 }else if(val.type=="2"){
							 vtype="入款第三方";
						 }else if(val.type=="3"){
							 vtype="入款支付宝";
						 }else if(val.type=="4"){
							 vtype="入款微信";
						 }else if(val.type=="5"){
							 vtype="出款卡";
						 }else if(val.type=="6"){
							 vtype="出款卡第三方";
						 }else if(val.type=="7"){
							 vtype="下发卡";
						 }else if(val.type=="8"){
							 vtype="备用卡";
						 }else if(val.type=="9"){
							 vtype="现金卡";
						 }else if(val.type=="10"){
							 vtype="微信专用";
						 }else if(val.type=="11"){
							 vtype="支付宝专用";
						 }else if(val.type=="12"){
							 vtype="第三方专用";
						 }else if(val.type=="13"){
							 vtype="下发卡";
						 }
						var levelnames="";
						if(val.level!="" && val.level!=null){
							levelnames=val.level.replace(/,/g,'');
							levelnames=levelnames.substring(0,levelnames.length-2);
						}else{
							levelnames=val.level;
						}
						var cont=val.pendingRemark.substring((val.pendingRemark.lastIndexOf("(解冻方式)")+6),
								(val.pendingRemark.lastIndexOf("解冻金额")));
						if(val.defrostType=='其它')
							cont=val.pendingRemark.substring((val.pendingRemark.lastIndexOf("备注：")+3),
									(val.pendingRemark.lastIndexOf("(处理)")));
						var k=val.pendingRemark.lastIndexOf("持续冻结")+5;
						var l=val.pendingRemark.lastIndexOf("解冻恢复使用")+7;
						var n=val.pendingRemark.lastIndexOf("永久删除")+5;
						var statusCont=val.pendingRemark.substring(k,val.pendingRemark.length);
						if(l>6){
							statusCont=val.pendingRemark.substring(l,val.pendingRemark.length);
						}else if(n>4){
							statusCont=val.pendingRemark.substring(n,val.pendingRemark.length);
						}
                        tr += '<tr>'
                        	      +'<td>' + (null==val.handicap?"":val.handicap) + '</td>'
                        	      +'<td>' + (val.currSysLeval?(val.currSysLeval==currentSystemLevelOutter?"外层":(val.currSysLeval==currentSystemLevelInner?"内层":"指定层")):"") + '</td>'
                        	      +"<td title='编号|开户人|银行类别'>"  
                        	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
                        	      +"</a></br>"
                        	      +val.alias+"|"+val.owner+"|"+val.banktype
                        	      +"</td>"
                        	      +'<td>' + vtype + '</td>'
                        	      +'<td>' + (val.bankbalance==null?"0":val.bankbalance) +'</td>'
                        	      +'<td>' + val.amount +'</td>'
                        	      +'<td>' + (val.bankbalance-val.amount).toFixed(2) +'</td>'
                        	      +'<td>'+(val.pendingStatus==3?"<span class='badge badge-success'>持续冻结</span>":(val.pendingStatus==4?"<span class='badge badge-success'>解冻恢复使用</span>":(val.pendingStatus==5?"<span class='badge badge-success'>永久冻结</span>":(val.pendingStatus==0?"<span class='badge badge-danger'>金流处理</span>":(val.pendingStatus==1?"<span class='badge badge-danger'>盘口处理</span>":(val.pendingStatus==2?"<span class='badge badge-danger'>财务处理</span>":"<span class='badge badge-success'></span>"))))))+ '</td>'
                        	      +'<td>' + "<span data-html='true' data-toggle='popover' data-trigger='hover' data-placement='left' data-content='"+cont+"' class='badge badge-info' title='解冻方式'>"+val.defrostType+"</span>" +'</td>'
                        	      +'<td>' + val.operator +'</td>'
                        	      +'<td>' + (val.confirmor==null?"":val.confirmor) +'</td>'
                        	      +'<td style="width:8%">' + val.cause +'</td>'
                        		  +'<td>' + val.createTime +'</td>';
                        	      if (_checkObj(statusCont)) {
                                      if (_checkObj(statusCont).length > 23) {
                                          tr += '<td>'
                                              + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                              + 'data-html="true" data-toggle="popover" data-trigger="click" data-placement="left"'
                                              + ' data-content="' + val.pendingRemark + '">'
                                              + _checkObj(statusCont).replace(/<br>/g, "").substring(0, 4)
                                              + '</a>'
                                              + '</td>';

                                      } else {
                                          tr += '<td>'
                                              + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                              + 'data-html="true" data-toggle="popover" data-trigger="click" data-placement="left"'
                                              + ' data-content="' + val.pendingRemark + '">'
                                              + _checkObj(statusCont)
                                              + '</a>'
                                              + '</td>';
                                      }
                                  } else {
                                      tr += '<td></td>';
                                  }
                        	      tr +='<td>';
                        	      tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal('+val.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>'
                        	         +'<button class="btn btn-xs btn-white btn-primary btn-bold orange '+OperatorLogBtn+' "  onclick="showModal_accountExtra('+val.id+')"><i class="ace-icon fa fa-book"></i><span>操作记录</span></button>'
                        	      +'</td>'
                        	 +'</tr>';
                        counts +=1;
                        bankAmounts+=val.bankbalance;
                        lossAmounts+=val.amount;
                        existingAmount+=(val.bankbalance-val.amount);
                    };
					 $('#processed_tbody').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="4">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+lossAmounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+existingAmount.toFixed(2)+'</td>'
								     +'<td colspan="8"></td>'
					          +'</tr>';
	                $('#processed_tbody').append(trs);
	                var trn = '<tr>'
				                	+'<td colspan="4">总计：'+jsonObject.data.frostPage.totalElements+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[0]).toFixed(2)+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[3]).toFixed(2)+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[4]).toFixed(2)+'</td>'
								    +'<td colspan="9"></td>'
					         +'</tr>';
	                $('#processed_tbody').append(trn);
			}else {
                $('#processed_tbody').empty().html('<tr></tr>');
            }
			 $("[data-toggle='popover']").popover();
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.frostPage,"processedPage",queryFinprocessed);
		}
	}); 
	
}


var loadHover_accountTodayInto = function (handicapName,companyIncome,thirdIncome) {
	if(null==companyIncome){
		companyIncome=0
	}
	if(null==thirdIncome){
		thirdIncome=0
	}
	$("[data-toggle='money" + handicapName + "']").popover({
        html: true,
        title: function () {
            return '<center class="blue">入款分类</center>';
        },
        delay: {show: 0, hide: 100},
        content: function () {
            return "<div id='accountInfoHover' style='width:500px' >"
                + "<div class='col-sm-14'>"
                + "	<div class='col-xs-4 text-right'><strong>公司入款金额：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + companyIncome.toFixed(2) + "</span></div>"
                + "	<div class='col-xs-6 text-right'><strong>第三方入款金额：</strong>"+thirdIncome.toFixed(2)+"</div>"
                + "</div>"
        }
    });
}
	
	
	jQuery(function($) {
		if(request&&request.parentstartAndEndTime){
			$("input[name='startAndEndTime']").val(request.parentstartAndEndTime);
		}
		initTimePicker(false,$("[name=thirdStartAndEndTime]"),typeCustomLatestToday);
		inithancipad();
		inithancipadReal();
		//返回按钮处理，设置查询时的值  账号统计
		if(request&&request.parenthandicap){
			var counts=$("#handicap option").length;
			for(var i=0;i<counts;i++){
			   if($("#handicap").get(0).options[i].value == request.parenthandicap){
				  $("#handicap").get(0).options[i].selected = true; 
				  break; 
			   } 
			}
		}if(request&&request.parentfieldval){
			var rObj = document.getElementsByName("form-field-checkbox");
			//解决父页面，直接选择时间时  把单选按钮checked清空 返回undefined
			if(request.parentfieldval=='undefined'){
				for(var i = 0;i < rObj.length;i++){
					if(rObj[i].checked == true){
						rObj[i].checked=false;
						break;
					}
				}
			}else{
				for(var i = 0;i < rObj.length;i++){
					if(rObj[i].value == request.parentfieldval){
						rObj[i].checked = 'checked';
						break;
					}
				}
			}
		}
		initTimePickerHandicap();
		queryFinMoreStat();
	})