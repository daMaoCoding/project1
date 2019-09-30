currentPageLocation = window.location.href;

/** 已绑账号密码弹出框 */
var showUpdPWD = function (id) {
	var $div = $("#updatePWD4clone").clone().attr("id", "updatePWD");
	$div.find("#accId").val(id);
	$div.find("td").css("padding-top", "10px");
	$div.modal("toggle");
	var accInfo = getAccountInfoById(id);
	if(accInfo){
		$div.find("[name=bankHide]").val((null==accInfo.alias?"无":accInfo.alias)+'-'+accInfo.bankType);
		$div.find("[name=signBank]").attr("placeholder",accInfo.sign?"********":"");//登陆账号
		$div.find("[name=hookBank]").attr("placeholder",accInfo.hook?"********":"");//登陆密码
		$div.find("[name=hubBank]").attr("placeholder",accInfo.hub?"********":"");//交易密码
	}else{
		$div.find("[name=signBank],[name=hookBank],[name=hubBank]").attr('disabled','disabled');
	}
	$div.on('hidden.bs.modal', function () { $div.remove();});
};

/** 修改已绑账号密码 */
var doUpdPWD=function(){
	var $div=$("#updatePWD");
	bootbox.confirm("确定修改密码信息?", function (result) {
		if (result) {
			var params = {
				accountId:$div.find("#accId").val(),
				sign:$.trim($div.find("[name=signBank]").val(),true),
				hook:$.trim($div.find("[name=hookBank]").val(),true),
				hub:$.trim($div.find("[name=hubBank]").val(),true)
			};
			$.ajax({type:'get',url:'/r/host/alterPWD4Supervisor',data: params,dataType:'json',success:function (res) {
				if (res.status==1) {
					showMessageForSuccess('操作成功');
					$div.modal("toggle");
				} else {
					showMessageForFail("修改失败：" + res.message);
				}
			}});
		}
	});
};

	/**
	*	初始化时间控件(按盘口统计)
	*/
	var initTimePickerHandicap=function(){
		var start =!$('#startAndEndTimefrostless').val()?moment().add(-1,'days').hours(07).minutes(0).seconds(0):$('#startAndEndTimefrostless').val().split(" - ")[0];
	    var end = !$('#startAndEndTimefrostless').val()?moment().hours(06).minutes(59).seconds(59):$('#startAndEndTimefrostless').val().split(" - ")[1];
		
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
	    
	    //冻结卡亏损
		var startAndEndTimefrostless = $('input.date-range-picker-frostless').daterangepicker({
			autoUpdateInput:false,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
	        opens:"left",
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
		}).val(!$('#startAndEndTimefrostless').val()?(start.format('YYYY-MM-DD HH:mm:ss')+' - '+end.format('YYYY-MM-DD HH:mm:ss')):$('#startAndEndTimefrostless').val());
		startAndEndTimefrostless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定的时候把单选按钮的checked清空
			var rObj = document.getElementsByName("form-field-checkbox-frostless");
			for(var i = 0;i < rObj.length;i++){
				if(rObj[i].checked == true){
					rObj[i].checked=false;
					break;
				}
			}
			//清空的同时查询数据
			$('#searhFrostless').click();
		});
		startAndEndTimefrostless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
		
		
		//待金流处理
		var cashflowStartAndEndTimefrostless = $('input.date-range-picker-cashflow').daterangepicker({
			autoUpdateInput:false,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
	        opens:"left",
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
		}).val(!$('#cashflowStartAndEndTimefrostless').val()?(start.format('YYYY-MM-DD HH:mm:ss')+' - '+end.format('YYYY-MM-DD HH:mm:ss')):$('#cashflowStartAndEndTimefrostless').val());
		cashflowStartAndEndTimefrostless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定的时候把单选按钮的checked清空
			var rObj = document.getElementsByName("cashflowDate");
			for(var i = 0;i < rObj.length;i++){
				if(rObj[i].checked == true){
					rObj[i].checked=false;
					break;
				}
			}
			//清空的同时查询数据
			$('#cashflowSearhFrostless').click();
		});
		cashflowStartAndEndTimefrostless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
		
		//待盘口处理
		var pendingStartAndEndTimefrostless = $('input.date-range-picker-pending').daterangepicker({
			autoUpdateInput:false,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
	        opens:"left",
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
		}).val(!$('#pendingStartAndEndTimefrostless').val()?(start.format('YYYY-MM-DD HH:mm:ss')+' - '+end.format('YYYY-MM-DD HH:mm:ss')):$('#pendingStartAndEndTimefrostless').val());
		pendingStartAndEndTimefrostless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定的时候把单选按钮的checked清空
			var rObj = document.getElementsByName("pendingDate");
			for(var i = 0;i < rObj.length;i++){
				if(rObj[i].checked == true){
					rObj[i].checked=false;
					break;
				}
			}
			//清空的同时查询数据
			$('#pendingSearhFrostless').click();
		});
		pendingStartAndEndTimefrostless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
		
		
		
		//待财务处理
		var financeStartAndEndTimefrostless = $('input.date-range-picker-finance').daterangepicker({
			autoUpdateInput:false,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
	        opens:"left",
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
		}).val(!$('#financeStartAndEndTimefrostless').val()?(start.format('YYYY-MM-DD HH:mm:ss')+' - '+end.format('YYYY-MM-DD HH:mm:ss')):$('#financeStartAndEndTimefrostless').val());
		financeStartAndEndTimefrostless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定的时候把单选按钮的checked清空
			var rObj = document.getElementsByName("financeDate");
			for(var i = 0;i < rObj.length;i++){
				if(rObj[i].checked == true){
					rObj[i].checked=false;
					break;
				}
			}
			//清空的同时查询数据
			$('#financeSearhFrostless').click();
		});
		financeStartAndEndTimefrostless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
		
		
		
		//已完成
		var processedStartAndEndTimefrostless = $('input.date-range-picker-processed').daterangepicker({
			autoUpdateInput:false,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
		    startDate: start, //设置开始日期
	        endDate: end, //设置结束日期
	        opens:"left",
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
		}).val(!$('#processedStartAndEndTimefrostless').val()?(start.format('YYYY-MM-DD HH:mm:ss')+' - '+end.format('YYYY-MM-DD HH:mm:ss')):$('#processedStartAndEndTimefrostless').val());
		processedStartAndEndTimefrostless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定的时候把单选按钮的checked清空
			var rObj = document.getElementsByName("processedDate");
			for(var i = 0;i < rObj.length;i++){
				if(rObj[i].checked == true){
					rObj[i].checked=false;
					break;
				}
			}
			//清空的同时查询数据
			$('#processedSearhFrostless').click();
		});
		processedStartAndEndTimefrostless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
		
		
		//入款亏损
		var startAndEndTimeInless = $('input.date-range-picker-inless').daterangepicker({
			autoUpdateInput:true,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
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
		}).val('');
		startAndEndTimeInless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
		});
		startAndEndTimeInless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
		
		//中转亏损
		var startAndEndTimeTransitaless = $('input.date-range-picker-transit').daterangepicker({
			autoUpdateInput:true,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
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
		}).val('');
		startAndEndTimeTransitaless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
		});
		startAndEndTimeTransitaless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
		
		//出款亏损
		var startAndEndTimeoutless = $('input.date-range-picker-out').daterangepicker({
			autoUpdateInput:true,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
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
		}).val('');
		startAndEndTimeoutless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
		});
		startAndEndTimeoutless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
		
		//盘口亏损
		var startAndEndTimeHandicapless = $('input.date-range-picker-handicap').daterangepicker({
			autoUpdateInput:true,
			timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		    timePickerSeconds:true,//显示秒
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
		}).val('');
		startAndEndTimeHandicapless.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
		});
		startAndEndTimeHandicapless.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}	
	//清空账号统计StartAndEndTime的值    冻结卡亏损
	function clearStartAndEndTimefrostless(){
		$("input[name='startAndEndTimefrostless']").val('');
		//点击直接查询
		queryFinfrostLessStat();
	}
	
	//清空账号统计StartAndEndTime的值    冻结卡亏损
	function clearCashflow(){
		$("input[name='cashflowStartAndEndTimefrostless']").val('');
		//点击直接查询
		queryCashflow();
	}
	
	//清空账号统计StartAndEndTime的值    冻结卡亏损
	function clearPending(){
		$("input[name='pendingStartAndEndTimefrostless']").val('');
		//点击直接查询
		queryFinpending();
	}
	
	//清空账号统计StartAndEndTime的值    冻结卡亏损
	function clearFinance(){
		$("input[name='financeStartAndEndTimefrostless']").val('');
		//点击直接查询
		queryFinance();
	}
	
	//清空账号统计StartAndEndTime的值    冻结卡亏损
	function clearProcessed(){
		$("input[name='processedStartAndEndTimefrostless']").val('');
		//点击直接查询
		queryFinprocessed();
	}
	//冻结卡亏损初始化盘口
//	var $selectHandicap = $("select[name='frosthandicap']").html("");
//	$.ajax({dataType:'json',type:"get",url:"/r/handicap/list",data:{enabled:1},success:function(jsonObject){
//		if(jsonObject.status == 1){
//			$selectHandicap.html("");
//			$('<option></option>').html('全部').attr('value','').attr('selected','selected').attr("handicapCode","").appendTo($selectHandicap);
//			for(var index in jsonObject.data){
//				var item = jsonObject.data[index];
//				$('<option></option>').html(item.name).attr('value',item.id).attr("handicapCode",item.code).appendTo($selectHandicap);
//			}
//		}
//	}});
	
	var inithancipad=function(name){
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
	                    $('#' + name).empty().html(opt);
	                    $('#' + name).trigger('chosen:updated');
	                }
	            }
	        }
	    });
	}
	
	//冻结卡亏损查询
	$('#searhFrostless').click(function () {
		queryFinfrostLessStat();
    });
	
	//待金流处理
	$('#cashflowSearhFrostless').click(function () {
		queryCashflow();
    });
	
	//待盘口处理
	$('#pendingSearhFrostless').click(function () {
		queryFinpending();
    });
	
	//待财务处理
	$('#financeSearhFrostless').click(function () {
		queryFinance();
    });
	
	//已完成
	$('#processedSearhFrostless').click(function () {
		queryFinprocessed();
    });
	
	
	//绑定按键事件，回车查询数据
	$('#tab0').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryFinfrostLessStat();
	    }
	}
	
	//冻结卡亏损
	var queryFinfrostLessStat=function(){
		$("#loadingModal").modal('show');
		//当前页码
		var CurPage=$("#frostlessPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//获取盘口
		var handicap=$("#frosthandicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		
		//获取账号类型
		var cartype=$('input:radio[name="carType"]:checked').val();
//		//获取层级
//		var level=$("#frostlevel").val();
//		if(level=="" || level==null){
//			level=0;
//		}
		//账号
		var account=$("#frostaccount").val();
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTimefrostless']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox-frostless"]:checked').val();
		
		$.ajax({
			type:"post",
			url:"/r/finLessStat/fininless",
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
							 if(val.type=="1" && ""!=val.sbuType&&val.sbuType!="2"){
								 vtype=val.sbuType=="1"?"入款卡-支付宝":(val.sbuType=="2"?"入款卡-微信":"入款卡");
							 }
							var levelnames="";
							if(val.level!="" && val.level!=null){
								levelnames=val.level.replace(/,/g,'');
								levelnames=levelnames.substring(0,levelnames.length-2);
							}else{
								levelnames=val.level;
							}
	                        tr += '<tr>'
	                        	      +'<td>' + (null==val.handicap?"":val.handicap) + '</td>'
	                        	      +'<td>' + (val.currSysLeval?(val.currSysLeval==currentSystemLevelOutter?"外层":(val.currSysLeval==currentSystemLevelInner?"内层":"指定层")):"") + '</td>'
	                        	      +"<td title='编号|开户人|银行类别'>"  
	                        	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
	                        	      +"</a></br>"
	                        	      +val.alias+"|"+val.owner+"|"+val.banktype
	                        	      +"</td>"
	                        	      //+'<td>' + val.handicap + '</td>'
	                        	     // +'<td>'+ levelnames +'</td>'
	                        	      +'<td>' + vtype + '</td>'
	                        	      +'<td>' + (val.bankbalance==null?"0":val.bankbalance) +'</td>'
	                        	      +'<td>' + (val.balance==null?"0":val.balance) +'</td>'
	                        	      //+'<td>' + (val.bankbalance-val.balance).toFixed(2) +'</td>'
	                                  +'<td style="width:20%">' + val.cause +'</td>';
	                        	      if (_checkObj(val.remark)) {
	                                      if (_checkObj(val.remark).length > 23) {
	                                          tr += '<td>'
	                                              + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
	                                              + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
	                                              + ' data-content="' + val.remark + '">'
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
	                        	      tr +='<td>'
	                        	         +'<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal('+val.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>'
	                        	         +'<button class="btn btn-xs btn-white btn-primary btn-bold orange '+OperatorLogBtn+' "  onclick="showModal_accountExtra('+val.id+')"><i class="ace-icon fa fa-book"></i><span>操作记录</span></button>'
	                        	      +'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        bankAmounts+=val.bankbalance;
	                        lossAmounts+=val.balance;
	                        existingAmount+=(val.bankbalance-val.balance);
	                    };
						 $('#frostless_tbody').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="4">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+lossAmounts.toFixed(2)+'</td>'
										 //+'<td bgcolor="#579EC8" style="color:white;">'+existingAmount.toFixed(2)+'</td>'
									     +'<td colspan="3"></td>'
						          +'</tr>';
		                $('#frostless_tbody').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="4">总计：'+jsonObject.data.frostPage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[0]).toFixed(2)+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[1]).toFixed(2)+'</td>'
					                	//+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[2]).toFixed(2)+'</td>'
									    +'<td colspan="3"></td>'
						         +'</tr>';
		                $('#frostless_tbody').append(trn);
				}else {
                    $('#frostless_tbody').empty().html('<tr></tr>');
                }
				 $("[data-toggle='popover']").popover();
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.frostPage,"frostlessPage",queryFinfrostLessStat);
				$("#loadingModal").modal('hide');
			}
		});
		
	}
	
//处理
var disposeButton=false;
//完成
var finishButton=false;
//回收
var recycleButton=false;
//金流处理
var cashFlowProcessingButton=false;
//盘口处理
var dishProcessingButton=false;
//盘口驳回
var dishToDismissButton=false;
//财务驳回
var financialRejectedButton=false;
//金流处理页签
var cashFlowProcessing=false;
//盘口处理页签
var dishProcessing=false;
//财务处理页签
var financial=false;
//汇总页签
var financialTotal=false;
function show(){
	$.each(ContentRight['FinLessStat:*'], function (name, value) {
	    if (name == 'FinLessStat:dispose:*') {
	    	disposeButton = true;
	    }
	    if (name == 'FinLessStat:accomplish:*') {
	    	finishButton = true;
	    }
	    if (name == 'FinLessStat:recycle:*') {
	    	recycleButton = true;
	    }
	    if (name == 'FinLessStat:CashFlowProcessing:*') {
	    	cashFlowProcessingButton = true;
	    }
	    if (name == 'FinLessStat:dishProcessing:*') {
	    	dishProcessingButton = true;
	    }
	    if (name == 'FinLessStat:dishToDismiss:*') {
	    	dishToDismissButton = true;
	    }
	    if (name == 'FinLessStat:financialRejected:*') {
	    	financialRejectedButton = true;
	    }
	    
	    if (name == 'FinLessStat:financialTotalner:*') {
	    	financialTotal=true;
	    }
	    
	    if (name == 'FinLessStat:financialner:*') {
	    	financial=true;
	    }
	    
	    if (name == 'FinLessStat:financialHandicapner:*') {
	    	dishProcessing=true;
	    }
	    
	    if (name == 'FinLessStat:financialGoldener:*') {
	    	cashFlowProcessing=true;
	    }
	});
} 

function clickTab(){
	if(financialTotal){
		$("#financialTotal").show();
    	$("#processed").click();
	}
	if(financial){
		$("#financial").show();
    	$("#finance").click();
	}
	if(dishProcessing){
		$("#dishProcessing").show();
    	$("#pending").click();
	}
	if(cashFlowProcessing){
		$("#financialGoldener").show();
		$("#cashfloww").click();
	}
}

//待金流处理
var queryCashflow=function(){
	$("#loadingModal").modal('show');
	//当前页码
	var CurPage=$("#cashflowPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#cashflowHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	
	//获取账号类型
	var cartype=$('input:radio[name="cashflowType"]:checked').val();
//		//获取层级
//		var level=$("#frostlevel").val();
//		if(level=="" || level==null){
//			level=0;
//		}
	//账号
	var account=$("#cashflowAccount").val();
	//编号
	var alias=$("#cashflowAlias").val();
	//分类
	var classification=$('input:radio[name="cashflowClassification"]:checked').val();
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='cashflowStartAndEndTimefrostless']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//获取查询范围
	var fieldval=$('input:radio[name="cashflowDate"]:checked').val();
	
	$.ajax({
		type:"post",
		url:"/r/finLessStat/finpending",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"level":0,
			"accounttype":0,
			"account":account,
			"classification":classification,
			"alias":alias,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"fieldval":fieldval,
			"type":"frostless",
			"cartype":cartype,
			"status":"0",
			"disposeType":"pending",
			"statusType":"0",
			"jdType":"",
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
							 vtype="出款第三方";
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
						 if(val.type=="1" && ""!=val.sbuType&&val.sbuType!="2"){
							 vtype=val.sbuType=="1"?"入款卡-支付宝":(val.sbuType=="2"?"入款卡-微信":"入款卡");
						 }
						var levelnames="";
						if(val.level!="" && val.level!=null){
							levelnames=val.level.replace(/,/g,'');
							levelnames=levelnames.substring(0,levelnames.length-2);
						}else{
							levelnames=val.level;
						}
						var n=val.pendingRemark.lastIndexOf("(盘口驳回)");
						var k=val.pendingRemark.lastIndexOf("<br>");
						var statusCont="";
						if(n!=-1)
							statusCont=val.pendingRemark.substring(k+4,n);
                        tr += '<tr>'
                        	      +'<td>' + (null==val.handicap?"":val.handicap) + '</td>'
                        	      +'<td>' + (val.currSysLeval?(val.currSysLeval==currentSystemLevelOutter?"外层":(val.currSysLeval==currentSystemLevelInner?"内层":"指定层")):"") + '</td>'
                        	      +"<td title='编号|银行类别'>"  
                        	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
                        	      +"</a></br>"
                        	      +val.alias+"|"+val.banktype
                        	      +"</td>"
                        	      +'<td>' + vtype + '</br><span style="color: red;">('+(val.flag=="2"?"返利网":(val.type=="2"?"第三方":"自购卡"))+')</span></td>'
                        	      +'<td>' + (val.bankbalance==null?"0":val.bankbalance) +'</td>'
                        	      +'<td>' + val.amount +'</td>'
                        	      +'<td>' + (val.bankbalance-val.amount).toFixed(2) +'</td>'
                        	      +'<td style="width:20%">'
                                  + '<a  class="bind_hover_card breakByWord"  title="冻结原因"'
                                  + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                  + ' data-content="' + val.cause + '">'
                                  +  val.cause
                                  + '</a>'
                                  + '</td>'
                        		  +'<td>' + val.createTime +'</td>';
                        	      if (_checkObj(statusCont)) {
                                      if (_checkObj(statusCont).length > 23) {
                                          tr += '<td>'
                                              + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                              + 'data-html="true" data-toggle="popover" data-trigger="click" data-placement="left"'
                                              + ' data-content="' + val.pendingRemark + '">'
                                              + _checkObj(statusCont).replace(/<br>/g, "").substring(0, 13)
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
                        	      if(disposeButton)
									  tr +='<button class="btn btn-xs btn-white btn-warning btn-bold "  onclick="showUpdPWD('+val.id+')"><i class="ace-icon fa fa-asterisk bigger-100 orange"><span>密码</span></i></button>';
                        	      if(cashFlowProcessingButton)
                        	    	  tr +='<button class="btn btn-xs btn-white btn-warning btn-bold "  onclick="showPasswordModal('+val.pendingId+','+(val.alias=='无'?0:val.alias)+')"><i class="ace-icon fa fa-pencil bigger-100 yellow"><span>处理</span></i></button>';
//                        	      if(finishButton)
//                        	    	  tr +='<button class="btn btn-xs btn-white btn-success btn-bold  "  onclick="dispose('+val.id+','+val.pendingId+','+(val.totalAmount*1)+',\''+val.pendingRemark+'\')"><i class="ace-icon fa fa-check bigger-100 blue"><span>完成</span></i></button>';
                        	      if(val.type!="2")
                        	    	  tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal('+val.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>';
                        	      tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange '+OperatorLogBtn+' "  onclick="showModal_accountExtra('+val.id+')"><i class="ace-icon fa fa-book"></i><span>操作记录</span></button>'
                        	      +'</td>'
                        	 +'</tr>';
                        counts +=1;
                        bankAmounts+=val.bankbalance;
                        lossAmounts+=val.amount;
                        existingAmount+=(val.bankbalance-val.amount);
                    }
					 $('#cashflow_tbody').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="4">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+lossAmounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+existingAmount.toFixed(2)+'</td>'
								     +'<td colspan="6"></td>'
					          +'</tr>';
	                $('#cashflow_tbody').append(trs);
	                var trn = '<tr>'
				                	+'<td colspan="4">总计：'+jsonObject.data.frostPage.totalElements+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[0]).toFixed(2)+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[3]).toFixed(2)+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+(Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[0]).toFixed(2)-Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[3]).toFixed(2))+'</td>'
								    +'<td colspan="6"></td>'
					         +'</tr>';
	                $('#cashflow_tbody').append(trn);
			}else {
                $('#cashflow_tbody').empty().html('<tr></tr>');
            }
			 $("[data-toggle='popover']").popover();
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.frostPage,"cashflowPage",queryCashflow);
			$("#loadingModal").modal('hide');
		}
	});
	
}




//待盘口处理
var queryFinpending=function(){
	$("#loadingModal").modal('show');
	//当前页码
	var CurPage=$("#pendingPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#pendingHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	
	//获取账号类型
	var cartype=$('input:radio[name="pendingCarType"]:checked').val();
//		//获取层级
//		var level=$("#frostlevel").val();
//		if(level=="" || level==null){
//			level=0;
//		}
	//账号
	var account=$("#pendingAccount").val();
	//编号
	var alias=$("#pendingAlias").val();
	//分类
	var classification=$('input:radio[name="pendingClassification"]:checked').val();
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='pendingStartAndEndTimefrostless']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//获取查询范围
	var fieldval=$('input:radio[name="pendingDate"]:checked').val();
	
	$.ajax({
		type:"post",
		url:"/r/finLessStat/finpending",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"level":0,
			"accounttype":0,
			"account":account,
			"classification":classification,
			"alias":alias,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"fieldval":fieldval,
			"type":"frostless",
			"cartype":cartype,
			"status":"1",
			"disposeType":"pending",
			"statusType":"1",
			"jdType":"",
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
							 vtype="出款第三方";
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
						 if(val.type=="1" && ""!=val.sbuType&&val.sbuType!="2"){
							 vtype=val.sbuType=="1"?"入款卡-支付宝":(val.sbuType=="2"?"入款卡-微信":"入款卡");
						 }
						var levelnames="";
						if(val.level!="" && val.level!=null){
							levelnames=val.level.replace(/,/g,'');
							levelnames=levelnames.substring(0,levelnames.length-2);
						}else{
							levelnames=val.level;
						}
						var n=val.pendingRemark.lastIndexOf("(金流处理)");
						var l=val.pendingRemark.lastIndexOf("(财务驳回)");
						var k=val.pendingRemark.lastIndexOf("<br>");
						var statusCont=val.pendingRemark.substring(k+4,n);
						if(l>0){
							statusCont=val.pendingRemark.substring(k+4,l);
						}
                        tr += '<tr>'
                        	      +'<td>' + (null==val.handicap?"":val.handicap) + '</td>'
                        	      +'<td>' + (val.currSysLeval?(val.currSysLeval==currentSystemLevelOutter?"外层":(val.currSysLeval==currentSystemLevelInner?"内层":"指定层")):"") + '</td>'
                        	      +"<td title='编号|银行类别'>"  
                        	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
                        	      +"</a></br>"
                        	      +val.alias+"|"+val.banktype
                        	      +"</td>"
                        	      +'<td>' + vtype + '</br><span style="color: red;">('+(val.flag=="2"?"返利网":(val.type=="2"?"第三方":"自购卡"))+')</span></td>'
                        	      +'<td>' + (val.bankbalance==null?"0":val.bankbalance) +'</td>'
                        	      +'<td>' + val.amount +'</td>'
                        	      +'<td>' + (val.bankbalance-val.amount).toFixed(2) +'</td>'
                        	      //+'<td>' + val.operator +'</td>'
                        	      +'<td style="width:20%">'
                                  + '<a  class="bind_hover_card breakByWord"  title="冻结原因"'
                                  + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                  + ' data-content="' + val.cause + '">'
                                  +  val.cause
                                  + '</a>'
                                  + '</td>'
                        		  +'<td>' + val.createTime +'</td>';
                        	      if (_checkObj(statusCont)) {
                                      if (_checkObj(statusCont).length > 23) {
                                          tr += '<td>'
                                              + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                              + 'data-html="true" data-toggle="popover" data-trigger="click" data-placement="left"'
                                              + ' data-content="' + val.pendingRemark + '">'
                                              + _checkObj(statusCont).replace(/<br>/g, "").substring(0, 13)
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
                        	      if(dishToDismissButton)
                        	    	  tr +='<button class="btn btn-xs btn-white btn-danger btn-bold "  onclick="showRemark('+val.pendingId+',0)"><i class="ace-icon fa fa-reply bigger-100 red"><span>驳回</span></i></button>';
                        	      if(dishProcessingButton)
                        	    	  tr +='<button class="btn btn-xs btn-white btn-warning btn-bold "  onclick="jiedongAccountModal('+val.pendingId+','+(val.bankbalance-val.amount).toFixed(2)+',\''+val.showPassword+'\','+(val.alias=='无'?0:val.alias)+')"><i class="ace-icon fa fa-pencil bigger-100 yellow"><span>处理</span></i></button>';
//                        	      if(finishButton)
//                        	    	  tr +='<button class="btn btn-xs btn-white btn-success btn-bold  "  onclick="dispose('+val.id+','+val.pendingId+','+(val.totalAmount*1)+',\''+val.pendingRemark+'\')"><i class="ace-icon fa fa-check bigger-100 blue"><span>完成</span></i></button>';
                        	      if(val.type!="2")
                        	    	  tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal('+val.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>';
                        	      tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange '+OperatorLogBtn+' "  onclick="showModal_accountExtra('+val.id+')"><i class="ace-icon fa fa-book"></i><span>操作记录</span></button>'
                        	      +'</td>'
                        	 +'</tr>';
                        counts +=1;
                        bankAmounts+=val.bankbalance;
                        lossAmounts+=val.amount;
                        existingAmount+=(val.bankbalance-val.amount);
                    };
					 $('#pending_tbody').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="4">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+lossAmounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+existingAmount.toFixed(2)+'</td>'
								     +'<td colspan="6"></td>'
					          +'</tr>';
	                $('#pending_tbody').append(trs);
	                var trn = '<tr>'
				                	+'<td colspan="4">总计：'+jsonObject.data.frostPage.totalElements+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[0]).toFixed(2)+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[3]).toFixed(2)+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+(Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[0]).toFixed(2)-Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[3]).toFixed(2))+'</td>'
								    +'<td colspan="6"></td>'
					         +'</tr>';
	                $('#pending_tbody').append(trn);
			}else {
                $('#pending_tbody').empty().html('<tr></tr>');
            }
			 $("[data-toggle='popover']").popover();
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.frostPage,"pendingPage",queryFinpending);
			$("#loadingModal").modal('hide');
		}
	});
	
}



//待财务处理
var queryFinance=function(){
	$("#loadingModal").modal('show');
	//当前页码
	var CurPage=$("#financePage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取盘口
	var handicap=$("#financeHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	
	//获取账号类型
	var cartype=$('input:radio[name="financeCarType"]:checked').val();
//		//获取层级
//		var level=$("#frostlevel").val();
//		if(level=="" || level==null){
//			level=0;
//		}
	//账号
	var account=$("#financeAccount").val();
	//编号
	var alias=$("#financeAlias").val();
	//分类
	var classification=$('input:radio[name="financeClassification"]:checked').val();
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='financeStartAndEndTimefrostless']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	//获取查询范围
	var fieldval=$('input:radio[name="financeDate"]:checked').val();
	//解冻方式
	var jdType=$('input:radio[name="frostType"]:checked').val();
	
	$.ajax({
		type:"post",
		url:"/r/finLessStat/finpending",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"level":0,
			"accounttype":0,
			"account":account,
			"classification":classification,
			"alias":alias,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"fieldval":fieldval,
			"type":"frostless",
			"cartype":cartype,
			"status":"2",
			"disposeType":"processed",
			"statusType":"2",
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
							 vtype="出款第三方";
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
						 if(val.type=="1" && ""!=val.sbuType&&val.sbuType!="2"){
							 vtype=val.sbuType=="1"?"入款卡-支付宝":(val.sbuType=="2"?"入款卡-微信":"入款卡");
						 }
						var levelnames="";
						if(val.level!="" && val.level!=null){
							levelnames=val.level.replace(/,/g,'');
							levelnames=levelnames.substring(0,levelnames.length-2);
						}else{
							levelnames=val.level;
						}
						var cont=val.pendingRemark.substring((val.pendingRemark.lastIndexOf("(解冻方式)")+6),
								(val.pendingRemark.lastIndexOf("(处理)")));
                        tr += '<tr>'
                        	      +'<td>' + (null==val.handicap?"":val.handicap) + '</td>'
                        	      +'<td>' + (val.currSysLeval?(val.currSysLeval==currentSystemLevelOutter?"外层":(val.currSysLeval==currentSystemLevelInner?"内层":"指定层")):"") + '</td>'
                        	      +"<td title='编号|银行类别'>"  
                        	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
                        	      +"</a></br>"
                        	      +val.alias+"|"+val.banktype
                        	      +"</td>"
                        	      +'<td>' + vtype + '</br><span style="color: red;">('+(val.flag=="2"?"返利网":(val.type=="2"?"第三方":"自购卡"))+')</span></td>'
                        	      +'<td>' + (val.bankbalance==null?"0":val.bankbalance) +'</td>'
                        	      +'<td>' + val.amount +'</td>'
                        	      +'<td>' + (val.bankbalance-val.amount).toFixed(2) +'</td>'
                        	      +'<td>' + val.operator +'</td>'
                        	      +'<td>' + "<span class='badge badge-success'>"+val.defrostType+"</span>" +'</td>'
                        	      +'<td style="width:10%">'
                                  + '<a  class="bind_hover_card breakByWord"  title="冻结原因"'
                                  + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                  + ' data-content="' + val.cause + '">'
                                  +  val.cause
                                  + '</a>'
                                  + '</td>'
                        		  +'<td>' + val.createTime +'</td>';
                        	      if (_checkObj(cont)) {
                                      if (_checkObj(cont).length > 23) {
                                          tr += '<td>'
                                              + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                              + 'data-html="true" data-toggle="popover" data-trigger="click" data-placement="left"'
                                              + ' data-content="' + val.pendingRemark + '">'
                                              + _checkObj(cont).replace(/<br>/g, "").substring(0, 7)+"..."
                                              + '</a>'
                                              + '</td>';

                                      } else {
                                          tr += '<td>'
                                              + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                              + 'data-html="true" data-toggle="popover" data-trigger="click" data-placement="left"'
                                              + ' data-content="' + val.pendingRemark + '">'
                                              + _checkObj(cont)
                                              + '</a>'
                                              + '</td>';
                                      }
                                  } else {
                                      tr += '<td></td>';
                                  }
                        	      tr +='<td>';
//                        	      if(disposeButton)
//                        	    	  tr +='<button class="btn btn-xs btn-white btn-warning btn-bold "  onclick="jiedongAccountModal('+val.pendingId+','+(val.bankbalance-val.totalAmount).toFixed(2)+')"><i class="ace-icon fa fa-pencil bigger-100 yellow"><span>处理</span></i></button>';
                        	      if(financialRejectedButton)
                        	    	  tr +='<button class="btn btn-xs btn-white btn-danger btn-bold "  onclick="showRemark('+val.pendingId+',1)"><i class="ace-icon fa fa-reply bigger-100 red"><span>驳回</span></i></button>';
                        	      if(finishButton)
                        	    	  tr +='<button class="btn btn-xs btn-white btn-success btn-bold  "  onclick="dispose('+val.id+','+val.pendingId+','+(val.amount*1)+',\''+val.pendingRemark+'\','+val.bankbalance+','+val.amount+',\''+val.defrostType+'\','+val.type+')"><i class="ace-icon fa fa-check bigger-100 blue"><span>完成</span></i></button>';
                        	      if(val.type!="2")
                        	    	  tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal('+val.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>';
                        	      tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange '+OperatorLogBtn+' "  onclick="showModal_accountExtra('+val.id+')"><i class="ace-icon fa fa-book"></i><span>操作记录</span></button>'
                        	      +'</td>'
                        	 +'</tr>';
                        counts +=1;
                        bankAmounts+=val.bankbalance;
                        lossAmounts+=val.amount;
                        existingAmount+=(val.bankbalance-val.amount);
                    };
					 $('#finance_tbody').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="4">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+lossAmounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+existingAmount.toFixed(2)+'</td>'
								     +'<td colspan="6"></td>'
					          +'</tr>';
	                $('#finance_tbody').append(trs);
	                var trn = '<tr>'
				                	+'<td colspan="4">总计：'+jsonObject.data.frostPage.totalElements+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[0]).toFixed(2)+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[3]).toFixed(2)+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.frosttotal[0]).toString().split(",")[4]).toFixed(2)+'</td>'
								    +'<td colspan="6"></td>'
					         +'</tr>';
	                $('#finance_tbody').append(trn);
			}else {
                $('#finance_tbody').empty().html('<tr></tr>');
            }
			 $("[data-toggle='popover']").popover();
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.frostPage,"financePage",queryFinance);
			$("#loadingModal").modal('hide');
		}
	});
	
}




//已完成
var queryFinprocessed=function(){
	$("#loadingModal").modal('show');
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
//		//获取层级
//		var level=$("#frostlevel").val();
//		if(level=="" || level==null){
//			level=0;
//		}
	//账号
	var account=$("#processedAccount").val();
	//编号
	var alias=$("#processedAlias").val();
	//分类
	var classification=$('input:radio[name="processeClassification"]:checked').val();
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
			"classification":classification,
			"alias":alias,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"fieldval":fieldval,
			"type":"frostless",
			"cartype":cartype,
			"status":"3",
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
						 if(val.type=="1" && ""!=val.sbuType&&val.sbuType!="2"){
							 vtype=val.sbuType=="1"?"入款卡-支付宝":(val.sbuType=="2"?"入款卡-微信":"入款卡");
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
                        	      +"<td title='编号|银行类别'>"  
                        	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
                        	      +"</a></br>"
                        	      +val.alias+"|"+val.banktype
                        	      +"</td>"
                        	      +'<td>' + vtype + '</br><span style="color: red;">('+(val.flag=="2"?"返利网":(val.type=="2"?"第三方":"自购卡"))+')</span></td>'
                        	      +'<td>' + (val.bankbalance==null?"0":val.bankbalance) +'</td>'
                        	      +'<td>' + val.amount +'</td>'
                        	      +'<td>' + (val.bankbalance-val.amount).toFixed(2) +'</td>'
                        	      +'<td>'+(val.pendingStatus==3?"<span class='badge badge-success'>持续冻结</span>":(val.pendingStatus==4?"<span class='badge badge-success'>解冻恢复使用</span>":"<span class='badge badge-success'>永久删除</span>"))+ '</td>'
                        	      +'<td>' + "<span data-html='true' data-toggle='popover' data-trigger='hover' data-placement='left' data-content='"+cont+"' class='badge badge-info' title='解冻方式'>"+val.defrostType+"</span>" +'</td>'
                        	      +'<td>' + val.operator +'</td>'
                        	      +'<td>' + val.confirmor +'</td>'
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
                        	       if(recycleButton&&val.type!=2)
                        	    	   tr +='<button class="btn btn-xs btn-white btn-warning btn-bold" onclick="showRecycleModal('+val.id+',0)"><i class="ace-icon fa fa-reply bigger-100 red"></i><span>转停用</span></button>';
                        	       if(val.type!="2")
                         	    	  tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal('+val.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>';
                         	      tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange '+OperatorLogBtn+' "  onclick="showModal_accountExtra('+val.id+')"><i class="ace-icon fa fa-book"></i><span>操作记录</span></button>'
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
			$("#loadingModal").modal('hide');
		}
	});
	
}

	

function showRecycleModal(accountId,transBlackTo){
	var accountInfo = getAccountInfoById(accountId);
	accountInfo.bankNameTitle=accountInfo.type==accountTypeOutBank?'开户行':'第三方名称';
	bootbox.dialog({
		message: "<span class='bigger-110'>确定把该账号（转停用）<br/>账号："+hideAccountAll(accountInfo.account)+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+accountInfo.bankNameTitle+"："+accountInfo.bankName+"</span>",
		buttons:{
			"click" :{"label" : "转停用","className" : "btn-sm btn-primary","callback": function() {
				$.ajax({ type:"post", url:"/r/finLessStat/updateAccountStatus",data:{"accountid":accountId},dataType:'json',success:function(jsonObject){
					if(jsonObject.status == 1){
						showMessageForSuccess("转停用成功！");
					}else{
						showMessageForFail(jsonObject.message);
					}
				}});
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
}
	
	
	var acid="";
	function changaccountid(accountid){
		acid=accountid;
		showHistoryModal();
	}
	
	function showHistoryModal(){
		//当前页码
		var CurPage=$("#historyPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		$.ajax({
			type:"post",
			url:"/r/finLessStat/findHistory",
			data:{
				"pageNo":CurPage,
				"accountid":acid,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.historylist.length > 0){
						var tr = '';
						 //小计
						 var counts = 0;
						 for(var index in jsonObject.data.historylist){
							 var val = jsonObject.data.historylist[index];
	                         tr += '<tr>'
		                        	  if (_checkObj(val.remark)) {
	                                      if (_checkObj(val.remark).length > 23) {
	                                          tr += '<td>'
	                                              + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
	                                              + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="right"'
	                                              + ' data-content="' + val.remark + '">'
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
	                           		  tr +='<td>' + val.time + '</td>'
	                        	      +'<td>' + val.operator + '</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                    };
						 $('#history_tbody').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="5">小计：'+counts+'</td>'
						          +'</tr>';
		                $('#history_tbody').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="5">总计：'+jsonObject.data.historyPage.totalElements+'</td>'
						         +'</tr>';
		                $('#history_tbody').append(trn);
				}else {
                    $('#history_tbody').empty().html('<tr></tr>');
                }
				 $("[data-toggle='popover']").popover();
				//分页初始化
				showPading(jsonObject.data.historyPage,"historyPage",showHistoryModal);
				$('#history_modal').modal('show');
			}
		});
	}
	
	
	function showFrozenAccountModal(id,bankbalance) {
	    $('#FrozenAccountRemark').val('');
	    $('#amount').val('');
	    $('#FrozenAccount_id').val(id);
	    $('#bankbalance').val(bankbalance);
	    $('#FrozenAccount_modal').modal('show');
	}
	
	function showRemark(id,status) {
	    $('#remark_Remark').val('');
	    $('#status').val('');
	    $('#remark_id').val(id);
	    $('#status').val(status);
	    $('#remark_modal').modal('show');
	}
	
	$('body').on('click', function(event) {
		var target = $(event.target);
		if (!target.hasClass('popover')
			&& target.parent('.popover-content').length === 0
			&& target.parent('.myPopover').length === 0
			&& target.parent('.popover-title').length === 0
			&& target.parent('.popover').length === 0 && !target.hasClass('bind_hover_card')) {
			$('.popover').popover('hide');
		}
	});
	
	function jiedongAccountModal(id,bankbalance,showPassword,alias) {
	    $('#jiedong_Remark').val('');
	    $('#jiedong_amount').val('');
	    $('#cardNum').val('');
	   // $('#cardAmount').val('');
	    $('#bkname').val('');
	    $('#account').val('');
	    $('#owner').val('');
	    //$('#amount').val('');
	    $('#cashAmount').val('');
	    $('#rate').val('');
	    $('#alias').val('');
	    $('#showPassword').empty().html("");
	    $('#frozenAmount').empty().html("");
	    $('#residueAmount').empty().html("");
	    $('#jiedong_id').val(id);
	    $('#los_amount').val(bankbalance);
	    $('#alias').val(alias);
	    $('#frozenAmount').empty().html(bankbalance);
	    if(showPassword=='yes'){
	    	 $("input[name='jiedong_password']").get(1).checked=true; 
	    	$("#jiedong_showPassword").show();
	    }else{
	    	$("#jiedong_showPassword").hide();
	    }
	    $('#jiedong_modal').modal('show');
	}
	    
	    
	
	function jiedong_showPasswrod(){
		var type=$('input:radio[name="jiedong_password"]:checked').val();
		var alias=$("#alias").val();
		var id=$("#jiedong_id").val();
		if(type=='yes'){
			$.ajax({
		        type: 'post',
		        url: '/r/finLessStat/findPassword',
		        data: {"alias": alias,"pendingId":id},
		        dataType: 'json',
		        success: function (res) {
		        	if(res.status == 1){
		        		$('#showPassword').empty().html(res.message);
		        	}else{
		        		$('#showPassword').empty().html(res.message);
		        	}
		        }
		    });
		}else{
			$('#showPassword').empty().html("");
		}
	}
	    
	function showPasswordModal(id,alias){
		$('#pending_id').val('');
	    $('#account_alias').val('');
	    $('#passwordRemark').val('');
	    $('#pending_id').val(id);
	    $('#account_alias').val(alias);
	    $('#message').empty().html("");
	    $("input[name='password']").get(1).checked=true; 
	    $('#password_modal').modal('show');
	}
	
	function showPasswrod(){
		var type=$('input:radio[name="password"]:checked').val();
		var alias=$("#account_alias").val();
		var pendingid=$("#pending_id").val();
		if(type=='yes'){
			 $.ajax({
			        type: 'post',
			        url: '/r/finLessStat/findPassword',
			        data: {"alias": alias,"pendingId":pendingid},
			        dataType: 'json',
			        success: function (res) {
			        	if(res.status == 1){
			        		 $('#message').empty().html(res.message);
			        	}else{
			        		$("input[name='password']").get(1).checked=true; 
			        		$.gritter.add({
			                    time: 300,
			                    class_name: '',
			                    title: '系统消息',
			                    text: res.message,
			                    sticky: false,
			                    image: '../images/message.png'
			                });
			        	}
			        }
			    });
		}else{
			 $('#message').empty().html();
		}
	}
	
	function dispose(accountId,traceId,totalAmount,remark,bal,amount,defrostType,type) {
		if(totalAmount<=0&&remark.length<=0){
            $.gritter.add({
                time: 300,
                class_name: '',
                title: '系统消息',
                text: '请先处理！',
                sticky: false,
                image: '../images/message.png'
            });
			return;
		}
		$('#FrozenAccountRemark').val('');
		$('#dAmount').empty().html("");
		$('#jAmount').empty().html("");
		$('#sAmount').empty().html("");
		$('#jType').empty().html("");
		$('#jconts').empty().html("");
	    $('#FrozenAccount_id').val(accountId);
	    $('#traceId').val(traceId);
	    $('#dAmount').empty().html(bal);
		$('#jAmount').empty().html(amount);
		$('#sAmount').empty().html((bal*1-amount*1).toFixed(2));
		$('#jType').empty().html(defrostType);
		var cont=remark.replace("<br>","").substring((remark.lastIndexOf("(解冻方式)")+6),
				(remark.lastIndexOf("解冻金额")-4))
		if(defrostType=='其它'){
			cont=remark.substring((remark.lastIndexOf("备注：")+3),
					(remark.lastIndexOf("(处理)")))
		}
		$('#jconts').empty().html(cont);
		if(type==1||type==2){
			$("#syRadio").css('display','none'); 
			$("#sySpan").css('display','none'); 
			$("#qsRadio").css('display','none'); 
			$("#qsSpan").css('display','none'); 
			$("#deratingSpan").css('display','none');
		}else{
			$("#syRadio").css('display','block'); 
			$("#sySpan").css('display','block'); 
			$("#qsRadio").css('display','block'); 
			$("#qsSpan").css('display','block');
			$("#deratingSpan").css('display','block');
		}
	    $('#FrozenAccount_modal').modal('show');
	}
	
	function cashflow(){
		var remark = $('#passwordRemark').val().replace(/\s/g,"");
		if(remark.indexOf("永久删除")>-1 || remark.indexOf("解冻恢复使用")>-1 || remark.indexOf("持续冻结")>-1){
			showMessageForFail("备注不能包含：永久删除、解冻恢复使用、持续冻结");
			return;
		}
	    if (!remark) {
	        $('#password_remark').text('请填写备注').show(10).delay(1500).hide(10);
	        return;
	    }
	    var type=$('input:radio[name="password"]:checked').val();
	    var traceId = $.trim($('#pending_id').val());
	    $.ajax({
	        type: 'post',
	        url: '/r/finLessStat/cashflow',
	        data: {"traceId":traceId, "remark": remark,"type":type},
	        dataType: 'json',
	        success: function (res) {
	        	if(res.status == 1){
	        		$.gritter.add({
	                    time: 300,
	                    class_name: '',
	                    title: '系统消息',
	                    text: res.message,
	                    sticky: false,
	                    image: '../images/message.png'
	                });
	        		queryCashflow();
				}else{
					showMessageForFail(res.message);
					queryCashflow();
				}
	            $('#password_modal').modal('hide');
	        }
	    });
	}
	
	function save_FrozenAccount() {
	    var remark =$('#FrozenAccountRemark').val().replace(/\s/g,'');
	    if(remark.indexOf("永久删除")>-1 || remark.indexOf("解冻恢复使用")>-1 || remark.indexOf("持续冻结")>-1){
			showMessageForFail("备注不能包含：永久删除、解冻恢复使用、持续冻结");
			return;
		}
	    if (!remark) {
	        $('#prompt_remark').text('请填写备注').show(10).delay(1500).hide(10);
	        return;
	    }
	    //处理类型
	    var type=$('input:radio[name="ProcessingType"]:checked').val();
	    var accountid = $('#FrozenAccount_id').val();
	    var traceId= $('#traceId').val();
	    var jAmount= $('#jAmount').html();
	    var derating= $("input[name='jdxy']:checked").val();
	    $.ajax({
	        type: 'post',
	        url: '/r/finLessStat/freezingProcess',
	        data: {"accountid": accountid, "traceId":traceId, "remark": remark,"type":type,"jAmount":jAmount,"derating":derating},
	        dataType: 'json',
	        success: function (res) {
	        	
	        	if(res.status == 1){
	        		$.gritter.add({
	                    time: 300,
	                    class_name: '',
	                    title: '系统消息',
	                    text: res.message,
	                    sticky: false,
	                    image: '../images/message.png'
	                });
	        		queryFinpending();
	 	            queryFinance();
				}else{
					showMessageForFail(res.message);
					queryFinance();
				}
	            $('#FrozenAccount_modal').modal('hide');
	        }
	    });
	}
	
	function jiedong() {
	    var remark =$('#jiedong_Remark').val().replace(/\s/g,'');
	    if(remark.indexOf("永久删除")>-1 || remark.indexOf("解冻恢复使用")>-1 || remark.indexOf("持续冻结")>-1){
			showMessageForFail("备注不能包含：永久删除、解冻恢复使用、持续冻结");
			return;
		}
	    if (!remark) {
	        $('#prompt_jiedong_remark').text('请填写备注').show(10).delay(1500).hide(10);
	        return;
	    }
	    //金额
	    var amount=$("#jiedong_amount").val();
	    if(!amount){
	    	$('#prompt_jiedong_amount').text('请填写金额').show(10).delay(1500).hide(10);
	        return;
	    }
	    if(amount*1>($('#los_amount').val()*1)){
		    $('#prompt_jiedong_more').text('解冻金额超过余额').show(10).delay(1500).hide(10);
	        return;
	    }
	    var particulars="";
	    //解冻方式
	    var type=$('input:radio[name="defrostType"]:checked').val();
	    if(type=='2' || type=='3' || type=='5'){
	    	var bkname=$.trim($('#bkname').val());
	    	var account=$.trim($('#account').val());
	    	var owner=$.trim($('#owner').val());
	    	//var typeAmount=$.trim($('#amount').val());
	    	if(!bkname || !account || !owner){
	    		$('#prompt_jiedong_remark').text('请填写对应的信息！').show(10).delay(1500).hide(10);
		    	return;
	    	}
	    	particulars+="银行名称:"+bkname+"  账号:"+account+"  开户人:"+owner;
	    }else if(type=='4'){
	    	var bkname=$.trim($('#bkname').val());
	    	var account=$.trim($('#account').val());
	    	var owner=$.trim($('#owner').val());
	    	//var amount=$.trim($('#amount').val());
	    	if(!bkname || !account || !owner){
	    		$('#prompt_jiedong_remark').text('请填写对应的信息！').show(10).delay(1500).hide(10);
		    	return;
	    	}
	    	particulars+="第三方名称:"+bkname+"  账号:"+account+"  开户人:"+owner;
	    }else if(type=='6'){
	    	var cashType=$('input:radio[name="cashType"]:checked').val();
	    	var cashAmount=$.trim($('#cashAmount').val());
	    	if(cashType=='pesos' || cashType=='TWD'){
	    		var rate=$.trim($('#rate').val());
	    		if(!cashAmount || !rate){
	    			$('#prompt_jiedong_remark').text('请填写对应的信息！').show(10).delay(1500).hide(10);
		    		return;
	    		}
	    		particulars+="币种:"+cashType+"  金额:"+cashAmount+"  汇率:"+rate;
	    	}else{
	    		if(!cashAmount){
	    			$('#prompt_jiedong_remark').text('请填写对应的信息！').show(10).delay(1500).hide(10);
		    		return;
	    		}
	    		particulars+="金额:"+cashAmount;
	    	}
	    }else if(type=='1'){
	    	var cardNum=$.trim($('#cardNum').val());
	    	//var cardAmount=$.trim($('#cardAmount').val());
	    	if(!cardNum){
	    		$('#prompt_jiedong_remark').text('请填写对应的信息！').show(10).delay(1500).hide(10);
		    	return;
	    	}
	    	particulars+="卡数:"+cardNum;
	    }
	    var id=$('#jiedong_id').val();
	    $.ajax({
	        type: 'post',
	        url: '/r/finLessStat/jiedongmoney',
	        data: {"id": id,"amount":amount, "remark": remark,"type":type,"particulars":particulars},
	        dataType: 'json',
	        success: function (res) {
	        	if(res.status == 1){
        		    $.gritter.add({
	                    time: 300,
	                    class_name: '',
	                    title: '系统消息',
	                    text: res.message,
	                    sticky: false,
	                    image: '../images/message.png'
	                });
    	            queryFinpending();
				}else{
					showMessageForFail(res.message);
					queryFinpending();
				}
	        	$('#jiedong_modal').modal('hide');
	        }
	    });
	}

function chooseType(){
	var choosetype=$('input:radio[name="defrostType"]:checked').val();
	if(choosetype=='2' || choosetype=='3' || choosetype=='5'){
		$("#payCash").hide();
		$("#payCarType").hide();
		$('#bankName').empty().html("银行名称"); 
		$("#payType").show();
	}else if(choosetype=='4'){
		$("#payCash").hide();
		$("#payCarType").hide();
		$('#bankName').empty().html("第三方名称"); 
		$("#payType").show();
	}else if(choosetype=='6'){
		$("#payType").hide();
		$("#payCarType").hide();
		$("#payCash").show();
	}else if(choosetype=='1'){
		$("#payType").hide();
		$("#payCash").hide();
		$("#payCarType").show();
	}else{
		$("#payType").hide();
		$("#payCarType").hide();
		$("#payCash").hide();
	}
}

function chooseCashType(){
	var choosetype=$('input:radio[name="cashType"]:checked').val();
	if(choosetype=='rmb'){
		$("#rateText").hide();
		$("#rate").hide();
	}else{
		$("#rateText").show();
		$("#rate").show();
	}
}
	
	
function saveRemark() {
    var remark =$('#remark_Remark').val().replace(/\s/g,'');
    if(remark.indexOf("永久删除")>-1 || remark.indexOf("解冻恢复使用")>-1 || remark.indexOf("持续冻结")>-1){
		showMessageForFail("备注不能包含：永久删除、解冻恢复使用、持续冻结");
		return;
	}
    if (!remark) {
        $('#prompt_remark_remark').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    var id=$('#remark_id').val();
    var status=$('#status').val();
    $.ajax({
        type: 'post',
        url: '/r/finLessStat/accomplish',
        data: {"id": id,"remark": remark,"status":status},
        dataType: 'json',
        success: function (res) {
        	if(res.status == 1){
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }else{
            	showMessageForFail(res.message);
            }
            $('#remark_modal').modal('hide');
            queryFinpending();
            queryFinance();
        }
    });
}
	
function clearNoNum(obj){
	var amount=0;
	//修复第一个字符是小数点 的情况.
	if(obj.value !=''&& obj.value.substr(0,1) == '.'){
		obj.value="";
	}
	obj.value = obj.value.replace(/^0*(0\.|[1-9])/, '$1');//解决 粘贴不生效
	obj.value = obj.value.replace(/[^\d.]/g,"");  //清除“数字”和“.”以外的字符
	obj.value = obj.value.replace(/\.{2,}/g,"."); //只保留第一个. 清除多余的     
	obj.value = obj.value.replace(".","$#$").replace(/\./g,"").replace("$#$",".");    
	obj.value = obj.value.replace(/^(\-)*(\d+)\.(\d\d).*$/,'$1$2.$3');//只能输入两个小数
	amount=obj.value;
	if(obj.value.indexOf(".")< 0 && obj.value !=""){//以上已经过滤，此处控制的是如果没有小数点，首位不能为类似于 01、02的金额
		if(obj.value.substr(0,1) == '0' && obj.value.length == 2){
			obj.value= obj.value.substr(1,obj.value.length);
			amount=obj.value;
		}
	}
	if(($('#los_amount').val()-amount)<0){
		showMessageForFail("超出冻结金额！");
		obj.value=0;
		$('#residueAmount').empty().html(($('#los_amount').val()-obj.value).toFixed(2));
	}else{
		$('#residueAmount').empty().html(($('#los_amount').val()-amount).toFixed(2));
	}
} 

function onlyClearNoNum(obj){
	//修复第一个字符是小数点 的情况.
	if(obj.value !=''&& obj.value.substr(0,1) == '.'){
		obj.value="";
	}
	obj.value = obj.value.replace(/^0*(0\.|[1-9])/, '$1');//解决 粘贴不生效
	obj.value = obj.value.replace(/[^\d.]/g,"");  //清除“数字”和“.”以外的字符
	obj.value = obj.value.replace(/\.{2,}/g,"."); //只保留第一个. 清除多余的     
	obj.value = obj.value.replace(".","$#$").replace(/\./g,"").replace("$#$",".");    
	obj.value = obj.value.replace(/^(\-)*(\d+)\.(\d\d).*$/,'$1$2.$3');//只能输入两个小数
	amount=obj.value;
	if(obj.value.indexOf(".")< 0 && obj.value !=""){//以上已经过滤，此处控制的是如果没有小数点，首位不能为类似于 01、02的金额
		if(obj.value.substr(0,1) == '0' && obj.value.length == 2){
			obj.value= obj.value.substr(1,obj.value.length);
			amount=obj.value;
		}
	}
} 

	
	
	//入款亏损
	var queryFinInLessStat=function(){
		//当前页码
		var CurPage=$("#inlessPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//获取盘口
		var handicap=$("#inhandicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		//获取层级
		var level=$("#inlevel").val();
		if(level=="" || level==null){
			level=0;
		}
		//账号
		var account=$("#inaccount").val();
		//账号类型
		var accounttype=$("#inaccounttype").val();
		if(level=="" || level==null){
			accounttype=0;
		}
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTimeInless']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox-inless"]:checked').val();
		
		$.ajax({
			type:"post",
			url:"/r/finLessStat/fininless",
			data:{
				"pageNo":CurPage,
				"handicap":handicap,
				"level":level,
				"accounttype":accounttype,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"inless",
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.Bankcardarrlist.length > 0){
						var tr = '';
						 //小计
						 var counts = 0;
						 var amounts=0;
						 var fees=0;
						 for(var index in jsonObject.data.Bankcardarrlist){
							 var val = jsonObject.data.Bankcardarrlist[index];
	                        tr += '<tr>'
	                        	      +'<td>' + val.handicapname + '</td>'
	                        	      +'<td>'+ val.levelname +'</td>'
	                        	      +'<td>' + val.account + '</td>'
	                        	      +'<td>' + val.amount + '</td>'
	                        	      +'<td style="display: none;">'+val.fee+'</td>'
	                        	      +'<td>' + val.counts +'</td>'
	                        	      +'<td>'
	                        	          +'<a href="#/finInStatMatch:*?id='+val.id+'&type=bank" type="button"><i class="ace-icon fa fa-list  bigger-100 orange"></i>明细'
	                        	          +'</a>'
	                        	      +'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        amounts+=val.amount;
	                        fees+=val.fee;
	                    };
						 $('#total_tbody_Bankcard').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="3">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts+'</td>'
									     +'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
		                $('#total_tbody_Bankcard').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="3">总计：'+jsonObject.data.Bankcardpage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[0]+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[1]+'</td>'
									    +'<td colspan="2"></td>'
						         +'</tr>';
		                $('#total_tbody_Bankcard').append(trn);
				}else {
                    $('#total_tbody_Bankcard').empty().html('<tr><td style="text-align: center" colspan="12"><h3>无数据</h3></td></tr>');
                }
				//分页初始化
				showPading(jsonObject.data.Bankcardpage,"fininStatBankcardPage",queryFinInStatBankcard);
			}
		});
		
	}
	
	//中转亏损
	var queryFinTransitLessStat=function(){
		//当前页码
		var CurPage=$("#transitlessPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//汇出账号类型
		var transitaccounttype=$("#transitaccounttype").val();
		if(transitaccounttype=="" || transitaccounttype==null){
			transitaccounttype=0;
		}
		//账号
		var account=$("#transitaccount").val();
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTimeTransitaless']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox-transitless"]:checked').val();
		
		$.ajax({
			type:"post",
			url:"/r/finLessStat/fintransitless",
			data:{
				"pageNo":CurPage,
				"transitaccounttype":transitaccounttype,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"transitless",
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.WeChatarrlist.length > 0){
					var tr = '';
					 //小计
					 var counts = 0;
					 var amounts=0;
					 var fees=0;
					 for(var index in jsonObject.data.WeChatarrlist){
						 var val = jsonObject.data.WeChatarrlist[index];
                        tr += '<tr>'
		                        	+'<td>' + val.handicapname + '</td>'
		                        	+'<td>'+ val.levelname +'</td>'
		                        	+'<td>' + val.account + '</td>'
		                        	+'<td>' + val.amount + '</td>'
		                        	+'<td style="display: none;">'+val.fee+'</td>'
		                        	+'<td>' + val.counts +'</td>'
		                        	+'<td>'
		                        		+'<a href="#/finInStatMatch:*?id='+val.id+'&type=weixin" type="button"><i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
		                        	+'</td>'
                        	 +'</tr>';
                        counts +=1;
                        amounts+=val.amount;
                        fees+=val.fee;
                    };
					 $('#total_tbody_WeChat').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="3">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts+'</td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
	                $('#total_tbody_WeChat').append(trs);
	                var trn = '<tr>'
				                	+'<td colspan="3">总计：'+jsonObject.data.WeChatpage.totalElements+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+new Array(jsonObject.data.WeChattotal[0]).toString().split(",")[0]+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.WeChattotal[0]).toString().split(",")[1]+'</td>'
								    +'<td colspan="2"></td>'
					         +'</tr>';
	                $('#total_tbody_WeChat').append(trn);
					}else {
		                $('#total_tbody_WeChat').empty().html('<tr><td style="text-align: center" colspan="12"><h3>无数据</h3></td></tr>');
		            }
				//分页初始化
				showPading(jsonObject.data.WeChatpage,"fininStatWeChatPage",queryFinInStatWeChat);
			}
		});
		
	}
	
	
	//出款亏损
	var queryFinOutLessStat=function(){
		//当前页码
		var CurPage=$("#outless_Page").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//账号类型
		var accounttype=$("#outaccounttype").val();
		if(accounttype=="" || accounttype==null){
			accounttype=0;
		}
		//账号
		var account=$("#outaccount").val();
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTimeoutless']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox-outless"]:checked').val();
		
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
				"type":"Paytreasure",
				"handicapname":handicapname,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.Paytreasurearrlist.length > 0){
					var tr = '';
					 //小计
					 var counts = 0;
					 var amounts=0;
					 var fees=0;
					 for(var index in jsonObject.data.Paytreasurearrlist){
						 var val = jsonObject.data.Paytreasurearrlist[index];
                        tr += '<tr>'
		                        	+'<td>' + val.handicapname + '</td>'
		                        	+'<td>'+ val.levelname +'</td>'
		                        	+'<td>' + val.account + '</td>'
		                        	+'<td>' + val.amount + '</td>'
		                        	+'<td style="display: none;">'+val.fee+'</td>'
		                        	+'<td>' + val.counts +'</td>'
		                        	+'<td>'
		                        		+'<a href="#/finInStatMatch:*?id='+val.id+'&type=zhifubao" type="button"><i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
		                        	+'</td>'
                        	 +'</tr>';
                        counts +=1;
                        amounts+=val.amount;
                        fees+=val.fee;
                    };
					 $('#total_tbody_Paytreasure').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="3">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts+'</td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
	                $('#total_tbody_Paytreasure').append(trs);
	                var trn = '<tr>'
				                	+'<td colspan="3">总计：'+jsonObject.data.PaytreasurePage.totalElements+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+new Array(jsonObject.data.Paytreasuretotal[0]).toString().split(",")[0]+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Paytreasuretotal[0]).toString().split(",")[1]+'</td>'
								    +'<td colspan="2"></td>'
					         +'</tr>';
	                $('#total_tbody_Paytreasure').append(trn);
					}else {
		                $('#total_tbody_Paytreasure').empty().html('<tr><td style="text-align: center" colspan="12"><h3>无数据</h3></td></tr>');
		            }
				//分页初始化
				showPading(jsonObject.data.PaytreasurePage,"fininStatPaytreasurePage",queryFinInStatPaytreasure);
			}
		});
	}
	
	//盘口亏损
	var queryFinHandicapLessStat=function(){
		//当前页码
		var CurPage=$("#handicaplessPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//获取盘口
		var handicap=$("#handicapHc").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTimeHandicapless']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox-handicapless"]:checked').val();
		
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
					var tr = '';
					 //小计
					 var counts = 0;
					 var amounts=0;
					 var fees=0;
					 for(var index in jsonObject.data.thirdpartyarrlist){
						 var val = jsonObject.data.thirdpartyarrlist[index];
                        tr += '<tr>'
		                        	+'<td>' + val.handicapname + '</td>'
		                        	+'<td>'+ val.levelname +'</td>'
		                        	+'<td>' + val.account + '</td>'
		                        	+'<td>' + val.amount + '</td>'
		                        	+'<td style="display: none;">'+val.fee+'</td>'
		                        	+'<td>' + val.counts +'</td>'
		                        	+'<td>'
		                        		+'<a href="#/finInStatMatch:*?id='+val.id+'&type=thirdparty" type="button"><i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
		                        	+'</td>'
                        	 +'</tr>';
                        counts +=1;
                        amounts+=val.amount;
                        fees+=val.fee;
                    };
					 $('#total_tbody_Thethirdparty').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="3">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts+'</td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
								     +'<td colspan="2"></td>'
							  +'</tr>';
	                $('#total_tbody_Thethirdparty').append(trs);
	                var trn = '<tr>'
				                	+'<td colspan="3">总计：'+jsonObject.data.thirdpartyPage.totalElements+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[0]+'</td>'
				                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[1]+'</td>'
								    +'<td colspan="2"></td>'
					         +'</tr>';
	                $('#total_tbody_Thethirdparty').append(trn);
					}else {
		                $('#total_tbody_Thethirdparty').empty().html('<tr><td style="text-align: center" colspan="12"><h3>无数据</h3></td></tr>');
		            }
				//分页初始化
				showPading(jsonObject.data.thirdpartyPage,"fininStatThethirdpartyPage",queryFinInStatThethirdparty);
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
	
	
	jQuery(function($) {
		initTimePickerHandicap();
		getHandicap_select($("#frosthandicap"),null,"全部");
		getHandicap_select($("#pendingHandicap"),null,"全部");
		getHandicap_select($("#processedHandicap"),null,"全部");
		getHandicap_select($("#financeHandicap"),null,"全部");
		getHandicap_select($("#cashflowHandicap"),null,"全部");
		if($('input:radio[name="form-field-checkbox-frostless"]:checked').val()){
			$("input[name='startAndEndTimefrostless']").val("");
		}
		//if($('input:radio[name="pendingDate"]:checked').val()){
			$("input[name='pendingStartAndEndTimefrostless']").val("");
		//}
		if($('input:radio[name="processedDate"]:checked').val()){
			$("input[name='processedStartAndEndTimefrostless']").val("");
		}
		//if($('input:radio[name="financeDate"]:checked').val()){
			$("input[name='financeStartAndEndTimefrostless']").val("");
		//}
			$("input[name='cashflowStartAndEndTimefrostless']").val("");
		//queryFinfrostLessStat();
		queryCashflow();
		queryFinpending();
		queryFinprocessed();
		queryFinance();
		show();
		clickTab();
//		queryFinInStatBankcard();
//		queryFinInStatWeChat();
//		queryFinInStatPaytreasure();
//		queryFinInStatThethirdparty();
		//明细返回的时候 判断 默认在哪个页签
		var request=getRequest();
		if(request&&request.type){
			if(request.type=="bank"){
				$("#bank").click();
			}else if(request.type=="weixin"){
				$("#weixin").click();
			}else if(request.type=="zhifubao"){
				$("#zhifubao").click();
			}else if(request.type=="thirdparty"){
				$("#thirdpartypay").click();
			}
		}
	})