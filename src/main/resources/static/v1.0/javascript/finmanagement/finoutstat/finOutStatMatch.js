currentPageLocation = window.location.href;
	//查询按钮
	$('#searhMatch').click(function () {
		queryFinOutMatch();
    });
	
	//层级change事件
	$('#level').change(function () {
		queryFinOutMatch();
    });
	
	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCode);   
	function getKeyCode(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryFinOutMatch();
	    }
	}
	
	/**
	*	初始化时间控件(按账号统计)
	*/
	var initTimePickerr=function(){
		var start =$('#startAndEndTimeMctch').val().split(" - ")[0];
	    var end = $('#startAndEndTimeMctch').val().split(" - ")[1];
	  //如果为空则赋一个值  要不时间显示不了
	    if(!start)
	    	start=moment().hours(07).minutes(0).seconds(0);
	    if(!end)
	    	end=moment().hours(07).minutes(0).seconds(0);
		var startAndEndTimeMctch = $('input.date-range-picker-Mctch').daterangepicker({
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
		}).val($('#startAndEndTimeMctch').val());
		startAndEndTimeMctch.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定时候调用查询数据
			$('#searhMatch').click();
		});
		startAndEndTimeMctch.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}
	
	
	function SpanGrid(tabObj,colIndex){
		 if(tabObj != null){
			  var i,j;
			  var intSpan;
			  var strTemp;
			  for(i = 0; i < tabObj.rows.length; i++){
				   intSpan = 1;
				   strTemp = tabObj.rows[i].cells[colIndex].innerText;
				   for(j = i + 1; j < tabObj.rows.length; j++){
					    if(strTemp == tabObj.rows[j].cells[colIndex].innerText){
						     intSpan++;
						     tabObj.rows[i].cells[0].rowSpan  = intSpan;
						     tabObj.rows[i].cells[1].rowSpan  = intSpan;
						     tabObj.rows[i].cells[2].rowSpan  = intSpan;
						     var height=(intSpan+2);
						     var ht=intSpan;
						     //设置文本居中
						     tabObj.rows[i].cells[0].style="line-height:"+height;
						     tabObj.rows[i].cells[1].style="line-height:"+height;
							 tabObj.rows[i].cells[2].style="line-height:"+height;
							 tabObj.rows[i].cells[3].style="line-height:"+height;
						     tabObj.rows[i].cells[4].style="line-height:"+height;
							 
						     tabObj.rows[i].cells[3].rowSpan  = intSpan;
						     tabObj.rows[i].cells[4].rowSpan  = intSpan;
						     tabObj.rows[j].cells[0].style.display = "none";
						     tabObj.rows[j].cells[1].style.display = "none";
						     tabObj.rows[j].cells[2].style.display = "none";
						     tabObj.rows[j].cells[3].style.display = "none";
						     tabObj.rows[j].cells[4].style.display = "none";
					    }
					    else
					    {
					    	break;
					    }
				   }
				   i = j - 1;
			  }
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
	var requestid=0;	
	var requestidhandicap=0;
	
	//定义公共的变量，返回的时候进行传参。判断需要查询的数据，已经参数的值显示
	var parenthandicap="";
	var parentlevel="";
	var parentstartAndEndTime="";
	var parentfieldval="";
	var parentzhCurPage="";
	var parentpkCurPage="";
	//出款明细>按盘口统计>明细
	var queryFinOutMatch=function(){
		$("#loadingModal").modal('show');
		//获取传过来的参数
		var rqhandicap=0;
		var id=0;
		var type="byhandicap";
		var request=getRequest();
		if(request&&request.typee){
			type=request.typee;
			if(type=="byhandicap"){
				rqhandicap=request.handicap;
				requestidhandicap=request.handicap;
				//赋值
				parenthandicap=request.handicapp;
				parentlevel=request.level;
				parentstartAndEndTime=request.startAndEndTime;
				parentfieldval=request.fieldvalHandicap;
				//记录父页面页码
				parentzhCurPage=request.zhCurPage;
				parentpkCurPage=request.pkCurPage;
			}else if(type=="byid"){
				type="byid";
				requestid=request.id;
				id=request.id;
				//赋值
				parenthandicap=request.handicap;
				parentlevel=request.level;
				parentstartAndEndTime=request.startAndEndTime;
				parentfieldval=request.fieldvalHandicap;
				//记录父页面页码
				parentzhCurPage=request.zhCurPage;
				parentpkCurPage=request.pkCurPage;
			}
		}
		//盘口
		var handicap=$("#handicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		//层级
		var level=$("#level").val();
		if(level=="" || level==null){
			level=0;
		}
		//会员
		var member=$("#member").val();
		//时间
		var startAndEndTime = $("input[name='startAndEndTimeMctch']").val();
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
			if(startamount==0){
				bootbox.alert("不能从0开始！");
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
		//当前页码
		var CurPage=$("#finOutMatchPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//拼接父页面的时间
		var parentstartAndEndTimeToArray = new Array();
		if(parentstartAndEndTime){
			var ptstartAndEnd = parentstartAndEndTime.split(" - ");
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[0]));
			parentstartAndEndTimeToArray.push($.trim(ptstartAndEnd[1]));
		}
		parentstartAndEndTimeToArray = parentstartAndEndTimeToArray.toString();
		//审核状态
		var restatus=$("#restatus").val();
		//出款状态
		var tastatus=$("#tastatus").val();
		$.ajax({
			type:"post",
			url:"/r/finoutstat/finoutmacth",
			data:{
				"pageNo":CurPage,
				"handicap":handicap,
				"level":level,
				"member":member,
				"startAndEndTime":startAndEndTimeToArray,
				"startamount":startamount,
				"endamount":endamount,
				"type":type,
				"rqhandicap":requestidhandicap,
				"id":requestid,
				"parentstartAndEndTimeToArray":parentstartAndEndTimeToArray,
				"parentfieldval":parentfieldval,
				"restatus":restatus,
				"tastatus":tastatus,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
					var handicapp=jsonObject.data.arrlist[0].handicappno;
					//如果有数据就初始化盘口，盘口就是当前数据的盘口
					//初始化盘口
					var $selectHandicap = $("select[name='handicap']").html("");
					$.ajax({dataType:'json',type:"get",url:"/r/handicap/list",data:{enabled:1},success:function(jsonObjectHandicap){
						if(jsonObjectHandicap.status == 1){
							$selectHandicap.html("");
							var handicap=jsonObject.data.arrlist[0].handicappno;
							$('<option></option>').html('全部').attr('value','').attr("handicapCode","").appendTo($selectHandicap);
							for(var index in jsonObjectHandicap.data){
								var item = jsonObjectHandicap.data[index];
								if(item.id==handicapp){
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
							data:{"handicap":handicapp},
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
					
					
					 var tr = '';
					 //小计
					 var counts=0;
					 var amountcounts = 0;
					 var amount=0;
					 var bankamount=0;
					 var fees=0;
					 var rowspancount=1;
					 var ordernonumber=jsonObject.data.arrlist[0].orderno;
					 //var idList=new Array();
					 for(var index in jsonObject.data.arrlist){
						 var val = jsonObject.data.arrlist[index];
						 //idList.push({'id':val.fromaccountid});
						 var val1="";
						 if((parseInt(index)+1)<jsonObject.data.arrlist.length){
							 val1= jsonObject.data.arrlist[parseInt(index)+1];
						 }
						 var operator=val.operator==null?"机器":val.operator;
                         tr += '<tr>'
	                        	 	  +'<td>' + val.handicapname + '</td>'
	                        	 	  +'<td>'+ val.member +'</td>'
	                        	 	  +'<td>' + val.orderno + '</td>'
	                        	 	  +'<td>' + _showReqStatus(val.restatus) + '</td>'
	                        	 	  +'<td>' + _showTaskStatus(val.tastatus) + '</td>'
	                        	 	  +'<td>' + val.createtime + '</td>'
	                        	 	  +'<td>' + val.updatetime + '</td>'
	                        	 	  +'<td>' + val.owner+'|'+val.banktype+'|'+val.alias+'</td>'
	                        	 	  +'<td>'+val.amounts+'</td>'
	                        	 	  +'<td>' +val.amount +'</td>'
	                        	 	  +'<td>'+operator+'</td>'
	                        	 	  +'<td>' +val.toaccountowner +'</td>'
	                        	 	  +'<td>' +(null==val.bankamount?"":val.bankamount) +'</td>'
	                        	 	 +'<td>' +(null==val.bankcreatime?"":val.bankcreatime) +'</td>'
                        	 +'</tr>';
                         counts +=1;
                         //处理拆单的现象，拆单父表存在多条一样的数据  只能记录一条这样的记录
                         if(val.orderno!=val1.orderno){
                        	 amountcounts+=val.amounts;
                         }
                         amount+=val.amount;
                         bankamount+=(null==val.bankamount?0:val.bankamount);
                         fees+=val.fee;
                     };
					 $('#total_tbody_match').empty().html(tr);
					 SpanGrid(total_tbody_match,2);
					 var trs = '<tr>'
							         +'<td colspan="8">小计：'+counts+'</td>'
							         +'<td bgcolor="#579EC8" style="color:white;">'+amountcounts+'</td>'
							         +'<td bgcolor="#579EC8" style="color:white;">'+amount+'</td>'
						             +'<td colspan="2"></td><td bgcolor="#579EC8" style="color:white;">'+bankamount+'</td>'
					           +'</tr>';
	                 $('#total_tbody_match').append(trs);
	                 var trn = '<tr>'
				                	 +'<td colspan="8">总计：'+jsonObject.data.page.totalElements+'</td>'
				                	 +'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.total[0]+'</td>'
				                	 +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total1[0]).toString().split(",")[0]+'</td>'
								     +'<td colspan="2"></td>'
								     +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total1[0]).toString().split(",")[2]+'</td>'
					          +'</tr>';
	                 $('#total_tbody_match').append(trn);
				}else{
					$('#total_tbody_match').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				//loadHover_accountInfoHover(idList);
				//分页初始化
				$("#loadingModal").modal('hide');
				showPading(jsonObject.data.page,"finOutMatchPage",queryFinOutMatch);
			}
		});
		
	}
	jQuery(function($) {
		var request=getRequest();
		$("input[name='startAndEndTimeMctch']").val(request.startAndEndTime);
		initTimePickerr();
		queryFinOutMatch();
		$("#back").attr("href","#/FinanceOutward:*?type=handicap&parenthandicap="+parenthandicap+"&parentlevel="+parentlevel+"&parentstartAndEndTime="+parentstartAndEndTime+"&parentfieldval="+parentfieldval
				                                  +"&parentzhCurPage="+parentzhCurPage+"&parentpkCurPage="+parentpkCurPage);
	})