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
			//确实的时候查询数据
			$('#searhByCondition').click();
		});
		startAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}	
	

	//查询按钮  
	$('#searhByCondition').click(function () {
		queryfinTransStatMatch();
    });
	
	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryfinTransStatMatch();
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
	
	var inithancipad=function(){
		//初始化盘口
		var $selectHandicap = $("select[name='search_account_handicapId']").html("");
		$.ajax(
				{dataType:'json',
				 async:false,
				 type:"get",
				 url:"/r/handicap/list",
				 data:{enabled:1},success:function(jsonObject){
			if(jsonObject.status == 1){
				$selectHandicap.html("");
				$('<option></option>').html('全部').attr('value','0').attr('selected','selected').attr("handicapCode","").appendTo($selectHandicap);
				for(var index in jsonObject.data){
					var item = jsonObject.data[index];
					$('<option></option>').html(item.name).attr('value',item.id).attr("handicapCode",item.code).appendTo($selectHandicap);
				}
			}
		}});
	}

	var typetab="";
	//定义公共的变量，返回的时候进行传参。判断需要查询的数据，已经参数的值显示
	var parentaccount="";
	var parentstartAndEndTime="";
	var parentfieldval="";	
	//父页面的页码
	var parentbankCurPage="";
	var parentzfbCurPage="";
	var parentwxCurPage="";
	var parentthirdCurPage="";
	var accountOwner="";
	var bankType="";
	//中转明细》明细
	var queryfinTransStatMatch=function(){
		//汇入账号显示在分页条上
		var accountHR="";
		//当前页码
		var CurPage=$("#finTransStatMatchPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		var request=getRequest();
		var accountid=0;
		var type=0;
		var serytype="";
		if(request&&request.accountid){
			accountid=request.accountid;
			//用于查询入款类型
			type=request.type;
			//用于查询银行流水还是系统流水
			serytype=request.serytype;
			typetab=request.type;
			//用于返回时，把参数带回去
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
		//订单号
		var orderno=$("#orderno").val();
		var status=$("#status").val();
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTime']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
			$("input[name='startAndEndTime']").val(parentstartAndEndTime);
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
		//拼接父页面的时间
		var parentstartAndEndTimeToArray = new Array();
		if(parentstartAndEndTime){
			var ptstartAndEnd = parentstartAndEndTime.split(" - ");
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[0]));
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[1]));
		}
		parentstartAndEndTimeToArray = parentstartAndEndTimeToArray.toString();
		var handicap=$("#search_account_handicapId").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		$.ajax({
			type:"post",
			url:"/r/fintransstat/finTransStatMatch",
			data:{
				"pageNo":CurPage,
				"orderno":orderno,
				"startAndEndTime":startAndEndTimeToArray,
				"startamount":startamount,
				"endamount":endamount,
				"accountid":accountid,
				"type":type,
				"serytype":serytype,
				"parentstartAndEndTimeToArray":parentstartAndEndTimeToArray,
				"parentfieldval":parentfieldval,
				"status":status,
				"handicap":handicap,
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
						 accountHR=date.fromaccountname;
						 var idList=new Array();
						 for(var index in jsonObject.data.arrlist){
							 var val = jsonObject.data.arrlist[index];
							 idList.push({'id':val.toid});
							 var status="";
							 if(val.status==0){
								 status="匹配中";
							 }else if(val.status==1){
								 status="已匹配";
							 }else if(val.status==2){
								 status="无法匹配";
							 }else if(val.status==3){
								 status="已取消";
							 }
	                        tr += '<tr>'
	                        			+'<td>' + val.handicapname + '</td>'
			                        	+'<td>' + val.orderno + '</td>'
			                        	+'<td>'+ status +'</td>'
			                        	+'<td>'
			                        		+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.toid+"' data-placement='auto right' data-trigger='hover'  >"+val.alias
			                        	    +"</a>" 
			                        	+'</td>'
			                        	+'<td>' + val.amount + '</td>'
			                        	+'<td>' + val.createtime +'</td>'
			                        	+'<td>' + val.remark +'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        amounts+=val.amount;
	                        fees+=val.fee;
	                    };
						 $('#total_tbody').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="4">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
		                $('#total_tbody').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="4">总计：'+jsonObject.data.page.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[0]+'</td>'
									    +'<td colspan="2"></td>'
						         +'</tr>';
		                $('#total_tbody').append(trn);
				}else{
					$('#total_tbody').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.page,"finTransStatMatchPage",queryfinTransStatMatch,'汇出账号：'+accountHR);
			}
		});
		
	}
	
	
	jQuery(function($) {
		var request=getRequest();
		$("input[name='startAndEndTime']").val(request.startAndEndTime);
		initTimePickerHandicap();
		inithancipad();
		queryfinTransStatMatch();
		$("#back").attr("href","#/FinTransStat:*?type="+typetab+"&parentaccount="+parentaccount+"&parentstartAndEndTime="+parentstartAndEndTime
				+"&parentfieldval="+parentfieldval+"&parentbankCurPage="+parentbankCurPage
				+"&parentwxCurPage="+parentwxCurPage+"&parentzfbCurPage="+parentzfbCurPage+"&parentthirdCurPage="+parentthirdCurPage
				+"&accountOwner="+encodeURI(encodeURI(accountOwner))+"&bankType="+encodeURI(encodeURI(bankType)));
	})