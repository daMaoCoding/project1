currentPageLocation = window.location.href;
	/**
	*	初始化时间控件(按盘口统计)
	*/
	var initTimePickerHandicap=function(){
		var start =$('#startAndEndTime').val().split(" - ")[0];
	    var end = $('#startAndEndTime').val().split(" - ")[1];
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
		}).val($('#startAndEndTime').val());
		startAndEndTime.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定时直接调用查询
			$('#searhByCondition').click();
		});
		startAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}	
	

	//查询按钮  
	$('#searhByCondition').click(function () {
		queryfinInStatMatch();
    });
	
	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryfinInStatMatch();
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

	var typetab="";
	//定义公共的变量，返回的时候进行传参。判断需要查询的数据，已经参数的值显示
	var parenthandicap="";
	var parentlevel="";
	var parentaccount="";
	var parentstartAndEndTime="";
	var parentfieldval="";
	var parentbankCurPage="";
	var parentwxCurPage="";
	var parentzfbCurPage="";
	var parentthirdCurPage="";
	var accountOwner="";
	var bankType="";
	//入款明细》明细
	var queryfinInStatMatch=function(){
		//当前页码
		var CurPage=$("#fininStatMatchPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		var request=getRequest();
		var id=0;
		if(request&&request.id){
			id=request.id;
			typetab=request.type;
			//赋值公共的变量，返回的时候进行传参。判断需要查询的数据，已经参数的值显示
			parenthandicap=request.handicap;
			parentlevel=request.level;
			parentaccount=request.account;
			parentstartAndEndTime=request.startAndEndTime;
			parentfieldval=request.fieldval;
			accountOwner=decodeURI(decodeURI(request.accountOwner));
			bankType=decodeURI(decodeURI(request.bankType));
			//设置父页面的分页  返回的时候带回去
			parentbankCurPage=request.yh;
			parentwxCurPage=request.wx;
			parentzfbCurPage=request.zfb;
			parentthirdCurPage=request.dsf;
		}
		//汇入人
		var memberrealname=$("#memberrealname").val();
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
		//拼接父页面的时间
		var parentstartAndEndTimeToArray = new Array();
		if(parentstartAndEndTime){
			var ptstartAndEnd = parentstartAndEndTime.split(" - ");
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[0]));
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[1]));
			$("input[name='startAndEndTime']").val(parentstartAndEndTime);
		}
		parentstartAndEndTimeToArray = parentstartAndEndTimeToArray.toString();
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
		
		$.ajax({
			type:"post",
			url:"/r/fininstat/fininstatmatch",
			data:{
				"pageNo":CurPage,
				"memberrealname":memberrealname,
				"startAndEndTime":startAndEndTimeToArray,
				"startamount":startamount,
				"endamount":endamount,
				"id":id,
				"type":typetab,
				"parentstartAndEndTime":parentstartAndEndTimeToArray,
				"parentfieldval":parentfieldval,
				"handicap":0,
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
							 var type="";
								if(val.type==1){
									type="平台支付宝入款";
								}else if(val.type==2){
									type="平台微信入款";
								}else if(val.type==3){
									type="平台银行卡入款";
								}else if(val.type==4){
									type="平台第三方入款";
								}
	                        tr += '<tr>'
			                        	+'<td>' + val.handicapname + '</td>'
			                        	+'<td>'+ type +'</td>'
			                        	+'<td>' + val.memberrealname + '</td>'
			                        	+'<td>' + val.orderno + '</td>'
			                        	+'<td>'+val.amount+'</td>'
			                        	+'<td>' + val.createtime +'</td>'
			                        	+'<td>' + (val.remark==null?"":val.remark) +'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        amounts+=val.amount;
	                    };
						 $('#total_tbody_Match').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="4">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
									     +'<td colspan="3"></td>'
								  +'</tr>';
		                $('#total_tbody_Match').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="4">总计：'+jsonObject.data.page.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[0]+'</td>'
									    +'<td colspan="3"></td>'
						         +'</tr>';
		                $('#total_tbody_Match').append(trn);
				}else{
					$('#total_tbody_Match').empty().html('<tr></tr>');
				}
				//分页初始化
				showPading(jsonObject.data.page,"fininStatMatchPage",queryfinInStatMatch);
			}
		});
		
	}
	
	
	jQuery(function($) {
		var request=getRequest();
		$("input[name='startAndEndTime']").val(request.startAndEndTime);
		initTimePickerHandicap();
		queryfinInStatMatch();
		$("#back").attr("href","#/FinInStat:*?type="+typetab+"&handicap="+parenthandicap
				+"&level="+parentlevel+"&account="+parentaccount+"&startAndEndTime="+parentstartAndEndTime
				+"&fieldval="+parentfieldval+"&parentbankCurPage="+parentbankCurPage
				+"&parentwxCurPage="+parentwxCurPage+"&parentzfbCurPage="+parentzfbCurPage+"&parentthirdCurPage="+parentthirdCurPage
				+"&accountOwner="+encodeURI(encodeURI(accountOwner))+"&bankType="+encodeURI(encodeURI(bankType)));
	})