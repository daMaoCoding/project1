currentPageLocation = window.location.href;
    //默认查询时间
    var initialStartTime =!$('#startAndEndTime').val()?moment().add(-1,'days').hours(07).minutes(0).seconds(0):$('#startAndEndTime').val().split(" - ")[0];
	var initialEndTime = !$('#startAndEndTime').val()?moment().hours(06).minutes(59).seconds(59):$('#startAndEndTime').val().split(" - ")[1];
	var initialstartAndEndTimeToArray = new Array();
	initialstartAndEndTimeToArray.push($.trim(initialStartTime.format('YYYY-MM-DD HH:mm:ss')));
	initialstartAndEndTimeToArray.push($.trim(initialEndTime.format('YYYY-MM-DD HH:mm:ss')));
	
//	var handicapinit=function(){
//		//初始化盘口
//		var $selectHandicap = $("select[name='handicap']").html("");
//		$.ajax(
//				{dataType:'json',
//				 async:false,
//				 type:"get",
//				 url:"/r/handicap/list",
//				 data:{enabled:1},success:function(jsonObject){
//			if(jsonObject.status == 1){
//				$selectHandicap.html("");
//				$('<option></option>').html('全部').attr('value','0').attr('selected','selected').attr("handicapCode","").appendTo($selectHandicap);
//				for(var index in jsonObject.data){
//					var item = jsonObject.data[index];
//					$('<option></option>').html(item.name).attr('value',item.id).attr("handicapCode",item.code).appendTo($selectHandicap);
//				}
//			}
//		}});
//	}
	
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
	
	
	var genBankType = function(bankTypeId){
	    var ret ='<option value="">请选择</option>';
	    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
	    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true});
	    $('#'+bankTypeId+'_chosen').prop('style', 'width: 120px;')
	};
	
	
	var initTimePickerHandicap=function(){
    	start=moment().add(-1,'days').hours(07).minutes(0).seconds(0);
    	end=moment().hours(06).minutes(59).seconds(59);
	    var exportStartAndEndTime = $('input.date-range-picker-export').daterangepicker({
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
			exportOut(ExcAccountid,0);
	    });
	    exportStartAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}
	
	var typeValue=3;
	//动态改动值，用于查询时 调用不同的方法
	function changevalue(type){
		typeValue=type;
	}
	var ExcAccountid="99999999";
	function changeExcAccountid(accountid){
		ExcAccountid=accountid;
	}
	//查询按钮  根据值判断查询不同的数据源
	$('#searhByCondition').click(function () {
		if(typeValue==0){
			queryFinInStatBankcard(1);
		}else if(typeValue==1){
			queryFinInStatPaytreasure(1);
		}else if(typeValue==2){
			queryFinInStatWeChat(1);
		}else if(typeValue==3){
			queryFinInStatThethirdparty(1);
		}else if(typeValue==4){
			queryFinInStandbyCard(1);
		}else if(typeValue==6){
			queryFinInthird(1);
		}else if(typeValue==7){
			queryScreening(1);
		}else if(typeValue==8){
			queryFinInStatSengCard(1);
		}else if(typeValue==9){
			queryFinInStandbyCardd(1);
		}
		
    });
	
	function clearStartAndEndTime(){
		$("input[name='startAndEndTime']").val('');
		//点击直接查询
		$('#searhByCondition').click();
	}
	
	
	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	$('#searhByCondition').click();
	    }
	}
	
	function searQuery(){
		$('#searhByCondition').click();
	}
	
	var request=getRequest();
	var yh="";
	var zfb="";
	var wx="";
	var dsf=""
	//入款银行卡中转
	var queryFinInStatBankcard=function(nb){
		$("#loadingModal").modal('show');
		//当前页码
		var CurPage=$("#fininStatBankcardPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		
		//如果子页面传过来的 分页页数不为空
		if(request&&request.parentbankCurPage){
			CurPage=request.parentbankCurPage;
		}
		yh=CurPage;
		
		//汇出账号
		var account=$("#fromaccount").val();
		//开户人
		var accountOwner=$("#accountOwner").val();
		//银行类别
		var bankType=$("#bankType").val();
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
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		if(nb==0){
			account="";
			//默认查询 当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			//url:"/r/fintransstat/fintransstatdeal",
			url:"/r/fintransstat/fintransstatdealFromClearDate",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"Bankcard",
				"accountOwner":accountOwner,
				"bankType":bankType,
				"handicap":handicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.Bankcardarrlist.length > 0){
					    //查询成功，把子页面传过来的翻页 页数清空
				         request.parentbankCurPage=null;
						 var tr = '';
						 //小计
						 var counts = 0;
						 var bankamounts=0;
						 var bankfees=0;
						 var tradingamounts=0;
						 var tradingfees=0;
						 var idList=new Array();
						 for(var index in jsonObject.data.Bankcardarrlist){
							 var val = jsonObject.data.Bankcardarrlist[index];
							 idList.push({'id':val.accountId});
	                        tr += '<tr>'
	                        	 		+'<td>'+(counts+1) +'</td>'
	                        	        +'<td>'+ val.handicapName +'</td>'
			                        	+"<td title='编号|开户人|银行类别'>" 
			                        	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
			                        	+"</a></br>"
			                        	+(val.alias==null?"":val.alias)+"|"+(val.owner==null?"":val.owner)+"|"+(val.bankType==null?"":val.bankType)
			                        	+"</td>"
			                        	+'<td>' + Math.abs(val.outward) + '</td>'
			                        	+'<td>' + Math.abs(val.fee) + '</td>'
			                        	+'<td>'+val.outwardCount+'</td>'
			                        	+'<td>' + val.outwardSys +'</td>'
			                        	+'<td>'+val.outwardSysCount+'</td>'
			                        	+'<td>'
			                        		//+'<a href="#/finTransStatMatch:*?accountid='+val.id+'&type=106&serytype=bank&account='+account+'&startAndEndTime='+startAndEndTime+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'&accountOwner='+accountOwner+'&bankType='+bankType+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                                      // +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>流水明细</a>' 
		                                       +'<a onclick="showInOutListModal('+val.accountId+',\'false\',\''+dd+'\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                                       +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细</a>' 
		                                       +'<a href="#/finTransStatMatch:*?accountid='+val.accountId+'&type=106&serytype=sys&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'&accountOwner='+encodeURI(encodeURI(accountOwner))+'&bankType='+encodeURI(encodeURI(bankType))+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                                       +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</a>';
	                        if(typeValue==0){
	                        	tr +='<a onclick="showExportModal_InOut('+val.accountId+',\'trans\')"type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
	                            	 +'<i class="ace-icon fa fa-list  bigger-100 blue"></i>导出</a>';
                            }      
		                   tr +='</td>'
                                 +'</tr>';
	                        counts +=1;
	                        bankamounts+=Math.abs(val.outward);
	                        bankfees+=Math.abs(val.fee);
	                        tradingamounts+=val.outwardSys;
	                    };
						 $('#total_tbody_Bankcard').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="3">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+bankamounts.toFixed(2)+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+bankfees+'</td>'
									     +'<td></td>'
									     +'<td bgcolor="#579EC8" style="color:white;">'+tradingamounts.toFixed(2)+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
	                    $('#total_tbody_Bankcard').append(trs);
	                    var trn = '<tr>'
				                    	+'<td colspan="3">总计：'+jsonObject.data.Bankcardpage.totalElements+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[0])+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[1])+'</td>'
								        +'<td></td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[2]+'</td>'
								        +'<td colspan="2"></td>'
					             +'</tr>';
	                    $('#total_tbody_Bankcard').append(trn);
				}else{
					$('#total_tbody_Bankcard').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.Bankcardpage,"fininStatBankcardPage",queryFinInStatBankcard);
				$("#loadingModal").modal('hide');
			}
		});
		
	}
	
	
	//入款支付宝中转
	var queryFinInStatPaytreasure=function(nb){
		//当前页码
		var CurPage=$("#fininStatPaytreasurePage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//如果子页面传过来的 分页页数不为空
		if(request&&request.parentzfbCurPage){
			CurPage=request.parentzfbCurPage;
		}
		zfb=CurPage;
		//汇出账号
		var account=$("#fromaccount").val();
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
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		if(nb==0){
			account="";
			//默认查询当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			url:"/r/fintransstat/fintransstatdeal",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"Paytreasure",
				"handicap":handicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.Paytreasurearrlist.length > 0){
					//查询成功，把子页面传过来的翻页 页数清空
				     request.parentzfbCurPage=null;
					 var tr = '';
					 //小计
					 var counts = 0;
					 var bankamounts=0;
					 var bankfees=0;
					 var tradingamounts=0;
					 var tradingfees=0;
					 var idList=new Array();
					 for(var index in jsonObject.data.Paytreasurearrlist){
						 var val = jsonObject.data.Paytreasurearrlist[index];
						 idList.push({'id':val.id});
						 var vtype=returnType(val.type);
                       tr += '<tr>'
		                    	   +"<td>" 
		                    	   +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+val.account
		                    	   +"</a>"
		                    	   +"</td>"
		                    	   +'<td>'+ vtype +'</td>'
		                    	   +'<td>' + val.bankamount + '</td>'
		                    	   +'<td>' + val.bankfee + '</td>'
		                    	   +'<td>'+val.bankcount+'</td>'
		                    	   +'<td>' + val.tradingamount +'</td>'
		                    	   +'<td>' + val.tradingfee + '</td>'
		                    	   +'<td>'+val.tradingcount+'</td>'
		                    	   +'<td>'
		                    	   		+'<a onclick="showInOutListModal('+val.id+',\'false\',\''+dd+'\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                    	   			+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细</a>' 
		                    	   		 +'<a href="#/finTransStatMatch:*?accountid='+val.id+'&type=104&serytype=sys&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                    	   		 	+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</button>'
		                    	   +'</td>'
                    	    +'</tr>';
                       counts +=1;
                       bankamounts+=val.bankamount;
                       bankfees+=val.bankfee;
                       tradingamounts+=val.tradingamount;
                       tradingfees+=val.tradingfee;
                   };
					 $('#total_tbody_Paytreasure').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="2">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankamounts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankfees+'</td>'
								     +'<td></td><td bgcolor="#579EC8" style="color:white;">'+tradingamounts+'</td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+tradingfees+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
	                     $('#total_tbody_Paytreasure').append(trs);
	                     var trn = '<tr>'
				                    	 +'<td colspan="2">总计：'+jsonObject.data.PaytreasurePage.totalElements+'</td>'
				                    	 +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Paytreasuretotal[0]).toString().split(",")[0]+'</td>'
				                    	 +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Paytreasuretotal[0]).toString().split(",")[1]+'</td>'
								         +'<td></td>'
								         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Paytreasuretotal[0]).toString().split(",")[2]+'</td>'
								         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Paytreasuretotal[0]).toString().split(",")[3]+'</td>'
								         +'<td colspan="2"></td>'
					              +'</tr>';
	                     $('#total_tbody_Paytreasure').append(trn);
					}else{
						$('#total_tbody_Paytreasure').empty().html('<tr></tr>');
					}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.PaytreasurePage,"fininStatPaytreasurePage",queryFinInStatPaytreasure);
			}
		});
	}
	
	
	//入款微信中转
	var queryFinInStatWeChat=function(nb){
		//当前页码
		var CurPage=$("#fininStatWeChatPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//如果子页面传过来的 分页页数不为空
		if(request&&request.parentwxCurPage){
			CurPage=request.parentwxCurPage;
		}
		wx=CurPage;
		//汇出账号
		var account=$("#fromaccount").val();
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
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		if(nb==0){
			account="";
			//默认查询当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			url:"/r/fintransstat/fintransstatdeal",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"WeChat",
				"handicap":handicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.WeChatarrlist.length > 0){
					//查询成功，把子页面传过来的翻页 页数清空
				     request.parentwxCurPage=null;
					 var tr = '';
					 //小计
					 var counts = 0;
					 var bankamounts=0;
					 var bankfees=0;
					 var tradingamounts=0;
					 var tradingfees=0;
					 for(var index in jsonObject.data.WeChatarrlist){
						 var val = jsonObject.data.WeChatarrlist[index];
						 var vtype=returnType(val.type);
                       tr += '<tr>'
		                    	   +'<td>' + val.account + '</td>'
		                    	   +'<td>'+ vtype +'</td>'
		                    	   +'<td>' + val.bankamount + '</td>'
		                    	   +'<td>' + val.bankfee + '</td>'
		                    	   +'<td>'+val.bankcount+'</td>'
		                    	   +'<td>' + val.tradingamount +'</td>'
		                    	   +'<td>' + val.tradingfee + '</td>'
		                    	   +'<td>'+val.tradingcount+'</td>'
		                    	   +'<td>'
		                    	    +'<a onclick="showInOutListModal('+val.id+',\'false\',\''+dd+'\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                   	   					+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细</a>' 
                   	   				+'<a href="#/finTransStatMatch:*?accountid='+val.id+'&type=105&serytype=sys&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                   	   					+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</button>'
		                          +'</td>'
                            +'</tr>';
                       counts +=1;
                       bankamounts+=val.bankamount;
                       bankfees+=val.bankfee;
                       tradingamounts+=val.tradingamount;
                       tradingfees+=val.tradingfee;
                   };
					 $('#total_tbody_WeChat').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="2">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankamounts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankfees+'</td>'
								     +'<td></td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+tradingamounts+'</td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+tradingfees+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
	                     $('#total_tbody_WeChat').append(trs);
	                     var trn = '<tr>'
				                    	 +'<td colspan="2">总计：'+jsonObject.data.WeChatpage.totalElements+'</td>'
				                    	 +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.WeChattotal[0]).toString().split(",")[0]+'</td>'
				                    	 +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.WeChattotal[0]).toString().split(",")[1]+'</td>'
								         +'<td></td>'
								         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.WeChattotal[0]).toString().split(",")[2]+'</td>'
								         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.WeChattotal[0]).toString().split(",")[3]+'</td>'
								         +'<td colspan="2"></td>'
					              +'</tr>';
	                     $('#total_tbody_WeChat').append(trn);
					}else{
						$('#total_tbody_WeChat').empty().html('<tr></tr>');
					}
				//分页初始化
				showPading(jsonObject.data.WeChatpage,"fininStatWeChatPage",queryFinInStatWeChat);
			}
		});
	}
	
	
	
	
	//下发银行卡中转
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
		//汇出账号
		var account=$.trim($("#fromaccount").val());
		//开户人
		var accountOwner=$.trim($("#accountOwner").val());
		//银行类别
		var bankType=$.trim($("#bankType").val());
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
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		if(nb==0){
			account="";
			//默认查询当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			//url:"/r/fintransstat/fintransstatdeal",
			url:"/r/fintransstat/fintransstatdealFromClearDate",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"Thethirdparty",
				"accountOwner":accountOwner,
				"bankType":bankType,
				"handicap":handicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.thirdpartyarrlist.length > 0){
					//查询成功，把子页面传过来的翻页 页数清空
				     request.parentthirdCurPage=null;
					 var tr = '';
					 //小计
					 var counts = 0;
					 var bankamounts=0;
					 var bankfees=0;
					 var tradingamounts=0;
					 var tradingfees=0;
					 var idList=new Array();
					 for(var index in jsonObject.data.thirdpartyarrlist){
						 var val = jsonObject.data.thirdpartyarrlist[index];
						 idList.push({'id':val.accountId});
                       tr += '<tr>'
                    	   		   +'<td>' + (counts+1) +'</td>'
                    	   		   +'<td>' + val.handicapName +'</td>'
		                    	   +"<td title='编号|开户人|银行类别'><a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
			                        	+"</a></br>"+val.alias+"|"+val.owner+"|"+val.bankType + '</td>'
		                    	   +'<td>' + Math.abs(val.outward) + '</td>'
		                    	   +'<td>' + Math.abs(val.fee) + '</td>'
		                    	   +'<td>'+val.outwardCount+'</td>'
		                    	   +'<td>' + val.outwardSys +'</td>'
		                    	   +'<td>'+val.outwardSysCount+'</td>'
		                    	   +'<td>'
		                    	   		+'<a onclick="showInOutListModal('+val.accountId+',\'false\',\''+dd+'\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                    	   			+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细</a>'
		                            	+'<a href="#/finTransStatMatch:*?accountid='+val.accountId+'&type=107&serytype=sys&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'&accountOwner='+encodeURI(encodeURI(accountOwner))+'&bankType='+encodeURI(encodeURI(bankType))+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                            		+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</button>'
		                           +'</td>'
                             +'</tr>';
                       counts +=1;
                       bankamounts+=Math.abs(val.outward);
                       bankfees+=Math.abs(val.fee);
                       tradingamounts+=Math.abs(val.outwardSys);
                   };
					 $('#total_tbody_Thethirdparty').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="3">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankamounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankfees+'</td>'
								     +'<td></td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+tradingamounts.toFixed(2)+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
	                     $('#total_tbody_Thethirdparty').append(trs);
	                     var trn = '<tr>'
				                    	 +'<td colspan="3">总计：'+jsonObject.data.thirdpartyPage.totalElements+'</td>'
				                    	 +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[0])+'</td>'
				                    	 +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[1])+'</td>'
								         +'<td></td><td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[2]+'</td>'
								         +'<td colspan="2"></td>'
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
	
	//备用银行卡中转
	var queryFinInStandbyCard=function(nb){
		//当前页码
		var CurPage=$("#fininStatstandbyPage").find(".Current_Page").text();
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
		//汇出账号
		var account=$.trim($("#fromaccount").val());
		//开户人
		var accountOwner=$.trim($("#accountOwner").val());
		//银行类别
		var bankType=$.trim($("#bankType").val());
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
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		if(nb==0){
			account="";
			//默认查询当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			//url:"/r/fintransstat/fintransstatdeal",
			url:"/r/fintransstat/fintransstatdealFromClearDate",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"standbyCard",
				"accountOwner":accountOwner,
				"bankType":bankType,
				"handicap":handicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.standbyList.length > 0){
					//查询成功，把子页面传过来的翻页 页数清空
				     request.parentthirdCurPage=null;
					 var tr = '';
					 //小计
					 var counts = 0;
					 var bankamounts=0;
					 var bankfees=0;
					 var tradingamounts=0;
					 var tradingfees=0;
					 var idList=new Array();
					 for(var index in jsonObject.data.standbyList){
						 var val = jsonObject.data.standbyList[index];
						 idList.push({'id':val.accountId});
                       tr += '<tr>'
                    	   		   +'<td>' + (counts+1) +'</td>'
                    	   		   +'<td>' + val.handicapName +'</td>'
		                    	   +"<td title='编号|开户人|银行类别'><a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
			                        	+"</a></br>"+val.alias+"|"+val.owner+"|"+val.bankType + '</td>'
		                    	   +'<td>' + Math.abs(val.outward) + '</td>'
		                    	   +'<td>' + Math.abs(val.fee) + '</td>'
		                    	   +'<td>'+val.outwardCount+'</td>'
		                    	   +'<td>' + val.outwardSys +'</td>'
		                    	   +'<td>'+val.outwardSysCount+'</td>'
		                    	   +'<td>'
		                    	   		+'<a onclick="showInOutListModal('+val.accountId+',\'false\',\''+dd+'\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                    	   			+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细</a>'
		                            	+'<a href="#/finTransStatMatch:*?accountid='+val.accountId+'&type=110&serytype=sys&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'&accountOwner='+encodeURI(encodeURI(accountOwner))+'&bankType='+encodeURI(encodeURI(bankType))+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                            		+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</button>'
		                           +'</td>'
                             +'</tr>';
                       counts +=1;
                       bankamounts+=Math.abs(val.outward);
                       bankfees+=Math.abs(val.fee);
                       tradingamounts+=Math.abs(val.outwardSys);
                   };
					 $('#total_tbody_standby').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="3">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankamounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankfees+'</td>'
								     +'<td></td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+tradingamounts.toFixed(2)+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
	                     $('#total_tbody_standby').append(trs);
	                     var trn = '<tr>'
			                    	 +'<td colspan="3">总计：'+jsonObject.data.standbyPage.totalElements+'</td>'
			                    	 +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.standbytotal[0]).toString().split(",")[0])+'</td>'
			                    	 +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.standbytotal[0]).toString().split(",")[1])+'</td>'
							         +'<td></td><td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.standbytotal[0]).toString().split(",")[2]+'</td>'
							         +'<td colspan="2"></td>'
					              +'</tr>';
	                     $('#total_tbody_standby').append(trn);
					}else{
						$('#total_tbody_standby').empty().html('<tr></tr>');
					}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.standbyPage,"fininStatstandbyPage",queryFinInStandbyCard);
			}
		});
	}
	
	
	
	//下发卡中转
	var queryFinInStatThesender=function(nb){
		//当前页码
		var CurPage=$("#ThesenderPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//汇出账号
		var account=$("#fromaccount").val();
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
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		if(nb==0){
			account="";
			//默认查询当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			url:"/r/fintransstat/fintransstatdeal",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"handicap":handicap,
				"type":"Thesender"},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.Thesenderarrlist.length > 0){
					var tr = '';
					 //小计
					 var counts = 0;
					 var bankamounts=0;
					 var bankfees=0;
					 var tradingamounts=0;
					 var tradingfees=0;
					 for(var index in jsonObject.data.Thesenderarrlist){
						 var val = jsonObject.data.Thesenderarrlist[index];
						 var vtype=returnType(val.type);
                       tr += '<tr>'
                    	   		   +'<td>' + (counts+1) + '</td>'
		                    	   +'<td>' + val.account + '</td>'
		                    	   +'<td>'+ vtype +'</td>'
		                    	   +'<td>' + val.bankamount + '</td>'
		                    	   +'<td>' + val.bankfee + '</td>'
		                    	   +'<td>'+val.bankcount+'</td>'
		                    	   +'<td>' + val.tradingamount +'</td>'
		                    	   +'<td>' + val.tradingfee + '</td>'
		                    	   +'<td>'+val.tradingcount+'</td>'
		                    	   +'<td>'
		                    	   +'<a onclick="showInOutListModal('+val.id+',\'false\',\''+dd+'\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                            	+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细</a>' 
		                           +'<a href="#/finTransStatMatch:*?accountid='+val.id+'&type=104&serytype=sys&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                            	+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</button>'
		                           +'</td>'
                            +'</tr>';
                       counts +=1;
                       bankamounts+=val.bankamount;
                       bankfees+=val.bankfee;
                       tradingamounts+=val.tradingamount;
                       tradingfees+=val.tradingfee;
                   };
					 $('#total_tbody_Thesender').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="3">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankamounts.toFixed(2)+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+bankfees+'</td>'
								     +'<td></td><td bgcolor="#579EC8" style="color:white;">'+tradingamounts+'</td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+tradingfees+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
	                     $('#total_tbody_Thesender').append(trs);
	                     var trn = '<tr>'
				                    	 +'<td colspan="3">总计：'+jsonObject.data.ThesenderPage.totalElements+'</td>'
				                    	 +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Thesendertotal[0]).toString().split(",")[0]+'</td>'
				                    	 +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Thesendertotal[0]).toString().split(",")[1]+'</td>'
								         +'<td></td>'
								         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Thesendertotal[0]).toString().split(",")[2]+'</td>'
								         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Thesendertotal[0]).toString().split(",")[3]+'</td>'
								         +'<td colspan="2"></td>'
					              +'</tr>';
	                     $('#total_tbody_Thesender').append(trn);
					}else{
						$('#total_tbody_Thesender').empty().html('<tr></tr>');
					}
				//分页初始化
				showPading(jsonObject.data.ThesenderPage,"ThesenderPage",queryFinInStatThesender);
			}
		});
	}
	
	
	//入款第三方中转
	var queryFinInthird=function(nb){
		//当前页码
		var CurPage=$("#fininthirdPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//汇出账号
		var account=$("#fromaccount").val();
		//开户人
		var accountOwner=$.trim($("#accountOwner").val());
		//银行类别
		var bankType=$.trim($("#bankType").val());
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
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		if(nb==0){
			account="";
			//默认查询当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		$.ajax({
			type:"post",
			url:"/r/fintransstat/fintransstatdeal",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"third",
				"accountOwner":accountOwner,
				"bankType":bankType,
				"handicap":handicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.thirdlist.length > 0){
					var tr = '';
					 //小计
					 var counts = 0;
					 var bankamounts=0;
					 var bankfees=0;
					 var tradingamounts=0;
					 var tradingfees=0;
					 var idList=new Array();
					 for(var index in jsonObject.data.thirdlist){
						 var val = jsonObject.data.thirdlist[index];
						 idList.push({'id':val.id});
						 var vtype=returnType(val.type);
                       tr += '<tr>'
                    	           +'<td>'+ (counts+1) +'</td>'
                    	           +'<td>' + val.handicapname + '</td>'
                    	           +"<td title='开户人|开户行'><a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+val.account
		                        	+"</a></br>"+val.bankname+'</td>'
		                    	   +'<td>'+ vtype +'</td>'
		                    	   +'<td>' + val.tradingamount +'</td>'
		                    	   +'<td>' + val.tradingfee + '</td>'
		                    	   +'<td>'+val.tradingcount+'</td>'
		                    	   +'<td>'
		                           +'<a href="#/finTransStatMatch:*?accountid='+val.id+'&type=103&serytype=sys&account='+account+'&startAndEndTime='+startAndEndTime+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'&accountOwner='+encodeURI(encodeURI(accountOwner))+'&bankType='+encodeURI(encodeURI(bankType))+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                            	+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</button></a>'
	                            	+'<a class="btn btn-xs btn-white btn-primary btn-bold" onclick="changeExcAccountid('+val.id+');showMoal();">'
										+'<i class="fa fa-cloud-download" aria-hidden="true"></i>&nbsp;&nbsp;导出'
								    +'</a>'
		                           +'</td>'
                            +'</tr>';
                       counts +=1;
                       tradingamounts+=val.tradingamount;
                       tradingfees+=val.tradingfee;
                   };
					 $('#total_tbody_third').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="4">小计：'+counts+'</td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+tradingamounts+'</td>'
								     +'<td bgcolor="#579EC8" style="color:white;">'+tradingfees+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
	                     $('#total_tbody_third').append(trs);
	                     var trn = '<tr>'
				                    	 +'<td colspan="4">总计：'+jsonObject.data.thirdPage.totalElements+'</td>'
								         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.thirdtotal[0]).toString().split(",")[0]+'</td>'
								         +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.thirdtotal[0]).toString().split(",")[1]+'</td>'
								         +'<td colspan="2"></td>'
					              +'</tr>';
	                     $('#total_tbody_third').append(trn);
					}else{
						$('#total_tbody_third').empty().html('<tr></tr>');
					}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.thirdPage,"fininthirdPage",queryFinInthird);
			}
		});
	}
	
	
	//中转隔天排查
	var queryScreening=function(nb){
		$("#loadingModal").modal('show');
		//当前页码
		var CurPage=$("#screeningPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		
		
		//汇出账号
		var account=$("#fromaccount").val();
		//开户人
		var accountOwner=$("#accountOwner").val();
		//银行类别
		var bankType=$("#bankType").val();
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
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldval=$('input:radio[name="form-field-checkbox"]:checked').val();
		if(nb==0){
			account="";
			//默认查询 当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		$.ajax({
			type:"post",
			url:"/r/fintransstat/fintransstatdeal",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"Screening",
				"accountOwner":accountOwner,
				"bankType":bankType,
				"handicap":handicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.screeninglist.length > 0){
						 var tr = '';
						 //小计
						 var counts = 0;
						 var amounts=0;
						 var idList=new Array();
						 var fromidList=new Array();
						 for(var index in jsonObject.data.screeninglist){
							 var val = jsonObject.data.screeninglist[index];
							 idList.push({'id':val.toId});
							 fromidList.push({'id':val.fromId});
							 var status=val.status==0?"匹配中":"已匹配";
	                        tr += '<tr>'
			                        	+'<td>' 
				                        	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.fromId+"' data-placement='auto right' data-trigger='hover'  >"+val.fromAlias
				                        	+"</a>"
			                        	+'</td>'
			                        	+'<td>' 
				                        	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.toId+"' data-placement='auto right' data-trigger='hover'  >"+val.toAlias
				                        	+"</a>"
			                        	+'</td>'
			                        	+'<td>'+ val.amount +'</td>'
			                        	+'<td>' + val.orderNo + '</td>'
			                        	+'<td>' + status + '</td>'
			                        	+'<td>'+val.fromBankType+'</td>'
			                        	+'<td>' + val.fromOwner +'</td>';
		                   tr +='</td>'
                                 +'</tr>';
	                        counts +=1;
	                        amounts+=val.amount;
	                    };
						 $('#screeningTbody').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="2">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
										 +'<td colspan="4"></td>'
						          +'</tr>';
	                    $('#screeningTbody').append(trs);
	                    var trn = '<tr>'
				                    	+'<td colspan="2">总计：'+jsonObject.data.screeningPage.totalElements+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.screeningtotal[0].toString()+'</td>'
								        +'<td colspan="4"></td>'
					             +'</tr>';
	                    $('#screeningTbody').append(trn);
				}else{
					$('#screeningTbody').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				loadHover_accountInfoHover(fromidList);
				//分页初始化
				showPading(jsonObject.data.screeningPage,"screeningPage",queryScreening);
				$("#loadingModal").modal('hide');
			}
		});
		
	}
	
	
	
	//入款明细下发卡银行
	var queryFinInStatSengCard=function(nb){
		//当前页码
		var CurPage=$("#fininStatSendCardCentPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//如果子页面传过来的 分页页数不为空
//		if(request&&request.parentsendcardCurPage){
//			CurPage=request.parentsendcardCurPage;
//		}
		yh=CurPage;
		//获取盘口
		var handicap=$("#handicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		//获取层级
		var handicapname=$("#level").find("option:selected").text();
		var level=$("#level").val();
		if(level=="" || level==null){
			level=0;
			handicapname="";
		}
		//收款账号
		var account=$("#fromaccount").val();
		//开户人
		var accountOwner=$.trim($("#accountOwner").val());
		//银行类别
		var bankType=$.trim($("#bankType").val());
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
		if(nb==0){
			handicap=0;
			level=0;
			handicapname="";
			account="";
			//初始查询 当天7点到第二天6:59:59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			//url:"/r/fininstat/fininstatistical",
			url:"/r/fininstat/fininstatisticalFromClearDate",
			data:{
				"pageNo":CurPage,
				"handicap":handicap,
				"level":level,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"SendCard",
				"handicapname":handicapname,
				"accountOwner":accountOwner,
				"bankType":bankType,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.sendCardlist.length > 0){
					     var idList=new Array();
						 //查询成功，把子页面传过来的翻页 页数清空
					     request.parentbankCurPage=null;
						 var tr = '';
						 //小计
						 var counts = 0;
						 var amounts=0;
						 var fees=0;
						 for(var index in jsonObject.data.sendCardlist){
							 var val = jsonObject.data.sendCardlist[index];
							 idList.push({'id':val.accountId});
	                        tr += '<tr>'
	                        			+'<td>' + (counts+1) + '</td>'
	                        			+'<td>' + val.handicapName + '</td>'
	                        	        +"<td title='编号|开户人|银行类别'>" 
		          						+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
		          						+"</a></br>"
		          						+val.alias+"|"+val.owner+"|"+val.bankType
	          					      +"</td>"
	                        	      +'<td>' + val.balance + '</td>'
	                        	      +'<td>' + val.income + '</td>'
	                        	      +'<td style="display: none;">'+val.fee+'</td>'
	                        	      +'<td>' + val.incomeCount +'</td>'
	                        	      +'<td>'
	                        	          +'<a href="#/finInSendCardStatMatch:*?id='+val.accountId+'&type=sendcard&handicap='+handicap+'&level='+level+'&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'&accountOwner='+encodeURI(encodeURI(accountOwner))+'&bankType='+encodeURI(encodeURI(bankType))+'" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细'
	                        	          +'</a>'
	                        	          +'<a onclick="showInOutListModal('+val.accountId+',\'false\',\''+dd+'\')" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细'
	                        	          +'</a>'
	                        	          +'&nbsp;<a onclick="showExportModal_InOut('+val.accountId+',\'income\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                          +'<i class="ace-icon fa fa-list  bigger-100 blue"></i>导出</a>'
	                        	      +'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        amounts+=val.income;
	                        //fees+=val.fee;
	                    };
						 $('#total_tbody_sendCardCent').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="4">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
									     //+'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
		                $('#total_tbody_sendCardCent').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="4">总计：'+jsonObject.data.sendCardpage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.sendCardtotal[0]).toString().split(",")[0]+'</td>'
					                	//+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[1]+'</td>'
									    +'<td colspan="2"></td>'
						         +'</tr>';
		                $('#total_tbody_sendCardCent').append(trn);
				}else{
					$('#total_tbody_sendCardCent').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.sendCardpage,"fininStatSendCardCentPage",queryFinInStatSengCard);
			}
		});
		
	}
	
	
	//入款明细备用银行卡
	var queryFinInStandbyCardd=function(nb){
		//当前页码
		var CurPage=$("#fininStatStandbyCardPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//如果子页面传过来的 分页页数不为空
//		if(request&&request.parentsendcardCurPage){
//			CurPage=request.parentsendcardCurPage;
//		}
		yh=CurPage;
		//获取盘口
		var handicap=$("#handicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		//获取层级
		var handicapname=$("#level").find("option:selected").text();
		var level=$("#level").val();
		if(level=="" || level==null){
			level=0;
			handicapname="";
		}
		//收款账号
		var account=$("#fromaccount").val();
		//开户人
		var accountOwner=$.trim($("#accountOwner").val());
		//银行类别
		var bankType=$.trim($("#bankType").val());
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
		if(nb==0){
			handicap=0;
			level=0;
			handicapname="";
			account="";
			//初始查询 当天7点到第二天6:59:59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			//url:"/r/fininstat/fininstatistical",
			url:"/r/fininstat/fininstatisticalFromClearDate",
			data:{
				"pageNo":CurPage,
				"handicap":handicap,
				"level":level,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"StandbyCard",
				"handicapname":handicapname,
				"accountOwner":accountOwner,
				"bankType":bankType,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.standbyCardlist.length > 0){
					     var idList=new Array();
						 //查询成功，把子页面传过来的翻页 页数清空
					     request.parentbankCurPage=null;
						 var tr = '';
						 //小计
						 var counts = 0;
						 var amounts=0;
						 var fees=0;
						 for(var index in jsonObject.data.standbyCardlist){
							 var val = jsonObject.data.standbyCardlist[index];
							 idList.push({'id':val.accountId});
	                        tr += '<tr>'
	                        	      +'<td>' + (counts+1) + '</td>'
	                        	      +'<td>' + val.handicapName + '</td>'
	                        	      +"<td title='编号|开户人|银行类别'>" 
		          						+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
		          						+"</a></br>"
		          						+val.alias+"|"+val.owner+"|"+val.bankType
	          					      +"</td>"
	                        	      +'<td>' + val.balance + '</td>'
	                        	      +'<td>' + val.income + '</td>'
	                        	      +'<td style="display: none;">'+val.fee+'</td>'
	                        	      +'<td>' + val.incomeCount +'</td>'
	                        	      +'<td>'
	                        	          +'<a href="#/finInSendCardStatMatch:*?id='+val.accountId+'&type=standbyCard&handicap='+handicap+'&level='+level+'&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'&accountOwner='+encodeURI(encodeURI(accountOwner))+'&bankType='+encodeURI(encodeURI(bankType))+'" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细'
	                        	          +'</a>'
	                        	          +'<a onclick="showInOutListModal('+val.accountId+',\'false\',\''+dd+'\')" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细'
	                        	          +'</a>'
	                        	          +'&nbsp;<a onclick="showExportModal_InOut('+val.accountId+',\'income\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                          +'<i class="ace-icon fa fa-list  bigger-100 blue"></i>导出</a>'
	                        	      +'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        amounts+=val.income;
	                        //fees+=val.fee;
	                    };
						 $('#total_tbody_standbyCard').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="4">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
									     //+'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
		                $('#total_tbody_standbyCard').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="4">总计：'+jsonObject.data.standbyCardpage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.standbyCardtotal[0]).toString().split(",")[0]).toFixed(2)+'</td>'
					                	//+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[1]+'</td>'
									    +'<td colspan="2"></td>'
						         +'</tr>';
		                $('#total_tbody_standbyCard').append(trn);
				}else{
					$('#total_tbody_standbyCard').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.standbyCardpage,"fininStatStandbyCardPage",queryFinInStandbyCardd);
			}
		});
		
	}
	
	
	
	function returnType(type){
		var vtype="";
		if(type=="1"){
			 vtype="入款卡";
		 }else if(type=="2"){
			 vtype="入款第三方";
		 }else if(type=="3"){
			 vtype="入款支付宝";
		 }else if(type=="4"){
			 vtype="入款微信";
		 }else if(type=="5"){
			 vtype="出款卡";
		 }else if(type=="6"){
			 vtype="出款第三方";
		 }else if(type=="7"){
			 vtype="下发卡";
		 }else if(type=="8"){
			 vtype="备用卡";
		 }else if(type=="9"){
			 vtype="现金卡";
		 }else if(type=="10"){
			 vtype="微信专用";
		 }else if(type=="11"){
			 vtype="支付宝专用";
		 }else if(type=="12"){
			 vtype="第三方专用";
		 }else if(type=="13"){
			 vtype="下发卡";
		 }
		return vtype;
	}
	
	
	/**
	 * 导出弹出框
	 */
	var showModal_exportChoice=function(accountId,exportType){
		var $div=$("#choiceExportModal");
		$div.find("#accountId").val(accountId);
		$div.find("#exportType").val(exportType);
		$div.modal("toggle");
		$div.on('hidden.bs.modal', function () {
			//关闭窗口重新初始化
			$div.find("#accountId").val("");
			$div.find("#exportType").val("");
			initTimePicker(true,$("[name=startAndEndTime_sys]"));
			initTimePicker(true,$("[name=startAndEndTime_bank]"));
			$("[name=parentfieldval_sys]:checked").prop("checked",false);
			$("[name=parentfieldval_bank]:checked").prop("checked",false);
		});
	}
	var beforeExport=function(e,serytype,url){
		var $div=$("#widget_"+serytype);
		var accountId=$("#choiceExportModal #accountId").val();
		var exportType=$("#choiceExportModal #exportType").val();
		var type=$("#choiceExportModal  #exportType").val();
		var startAndEndTime=$("[name=startAndEndTime_"+serytype+"]").val();
		var parentfieldval=$("[name=parentfieldval_"+serytype+"]:checked").val();
		url+="/"+accountId;
		if(startAndEndTime&&startAndEndTime.length>0){
			url+="/"+getTimeArray(startAndEndTime).toString();
		}else{
			url+="/0";
		}
		if(parentfieldval){
			url+="/"+parentfieldval;
		}else{
			url+="/0";
		}
		url+="/"+exportType;
		url+="/"+serytype;
		$(e).attr("href",url);
		debugger
	}
	
	$('#exportParagraph').on('hidden.bs.modal', function () {
		ExcAccountid=99999999; 
		$("#contText").html('导出出款单信息');
	})
	
	
	function showMoal(){
		$('#exportParagraph').modal('toggle');
		$("#contText").html('导出第三方中转单信息');
		exportOut(ExcAccountid,0);
	}
	
	function showMoalThirdParty(){
		$('#exportParagraph').modal('toggle');
		ExcAccountid=88888888;
		$("#contText").html('导出第三方中转单信息');
		exportOut(ExcAccountid,0);
	}
	
	
	function showMoalSender(){
		$('#exportParagraph').modal('toggle');
		$("#contText").html('导出下发银行卡中转单信息');
		exportOut(99999999,0);
	}
	
	function showMoalStandby(){
		$('#exportParagraph').modal('toggle');
		$("#contText").html('导出备用银行卡中转单信息');
		changeExcAccountid(77777777);
		exportOut(77777777,0);
	}
	
	var exportHandicap=function(){
		//初始化盘口
		var $selectHandicap = $("select[name='exportHandicap']").html("");
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
	
	jQuery(function($) {
		//返回按钮处理，设置查询时的值 ,共用的一个查询条件页头   设置当前页面选的值
		inithancipad("handicap");
		genBankType('bankType');
		exportHandicap();
		initTimePickerHandicap();
		if(request&&request.parentaccount){
			$("#fromaccount").val(request.parentaccount);
		}if(request&&request.accountOwner&&request.accountOwner!='undefined'){
			$("#accountOwner").val(decodeURI(decodeURI(request.accountOwner)));
		}if(request&&request.bankType&&request.accountOwner!='bankType'){
			$("#bankType").val(decodeURI(decodeURI(request.bankType)));
		}
		choiceTimeClearAndSearch($('#startAndEndTime'),"form-field-checkbox",$('#searhByCondition'));
		choiceTimeClearAndSearch($('[name=startAndEndTime_sys]'),"parentfieldval_sys");
		choiceTimeClearAndSearch($('[name=startAndEndTime_bank]'),"parentfieldval_bank");
		//返回时默认填充值
		if(request&&request.parentfieldval&&request.parentfieldval!="undefined"){
			$("[name=form-field-checkbox][value="+request.parentfieldval+"]").prop("checked",true);
			$("input[name='startAndEndTime']").val("");
		}else if(request&&request.parentstartAndEndTime){
			$("input[name='startAndEndTime']").val(request.parentstartAndEndTime);
		}
		if(!$('#startAndEndTime').val() && $('input:radio[name="form-field-checkbox"]:checked').val()){
			initTimePicker(false,null,typeCustomLatestOneDay);
		}else{
			clearRadioValue("form-field-checkbox");
			initTimePicker(true,null,typeCustomLatestOneDay,request.parentstartAndEndTime);
		}
		//明细返回的时候 判断 默认在哪个页签
		if(request&&request.type){
			if(request.type==106){
				queryFinInStatBankcard(1);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInStatThethirdparty(0);
				queryFinInStandbyCard(0);
				queryFinInthird(0);
				queryScreening(0);
				queryFinInStatSengCard(0);
				queryFinInStandbyCardd(0);
				//暂时没有下发卡中转queryFinInStatThesender(0);
				$("#bank").click();
			}else if(request.type==104){
				queryFinInStatPaytreasure(1);
				queryFinInStatBankcard(0);
				queryFinInStatWeChat(0);
				queryFinInStatThethirdparty(0);
				queryFinInStandbyCard(0);
				queryFinInthird(0);
				queryScreening(0);
				queryFinInStatSengCard(0);
				queryFinInStandbyCardd(0);
				//暂时没有下发卡中转queryFinInStatThesender(0);
				$("#zhifubao").click();
			}else if(request.type==105){
				queryFinInStatWeChat(1);
				queryFinInStatBankcard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatThethirdparty(0);
				queryFinInStandbyCard(0);
				queryFinInthird(0);
				queryScreening(0);
				queryFinInStatSengCard(0);
				queryFinInStandbyCardd(0);
				//暂时没有下发卡中转queryFinInStatThesender(0);
				$("#weixin").click();
			}else if(request.type==107){
				queryFinInStatThethirdparty(1);
				queryFinInStandbyCard(0);
				queryFinInStatBankcard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInthird(0);
				queryScreening(0);
				queryFinInStatSengCard(0);
				queryFinInStandbyCardd(0);
				//暂时没有下发卡中转queryFinInStatThesender(0);
				$("#thirdpartypay").click();
			}else if(request.type==103){
				queryFinInthird(1);
				queryFinInStatBankcard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInStatThethirdparty(0);
				queryFinInStandbyCard(0);
				queryScreening(0);
				queryFinInStatSengCard(0);
				queryFinInStandbyCardd(0);
				//暂时没有下发卡中转queryFinInStatThesender(1);
				$("#third").click();
			}else if(request.type==110){
				queryFinInStatThethirdparty(0);
				queryFinInStandbyCard(1);
				queryFinInStatBankcard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInthird(0);
				queryScreening(0);
				queryFinInStatSengCard(0);
				queryFinInStandbyCardd(0);
				//暂时没有下发卡中转queryFinInStatThesender(0);
				$("#standbyCard").click();
			}else if(request.type=="standbyCard"){
				queryFinInStandbyCardd(1);
				queryFinInStatThethirdparty(0);
				queryFinInStandbyCard(0);
				queryFinInStatBankcard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInthird(0);
				queryScreening(0);
				queryFinInStatSengCard(0);
				//暂时没有下发卡中转queryFinInStatThesender(0);
				$("#standbyCardd").click();
			}else if(request.type=="sendcard"){
				queryFinInStatSengCard(1);
				queryFinInStandbyCardd(0);
				queryFinInStatThethirdparty(0);
				queryFinInStandbyCard(0);
				queryFinInStatBankcard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInthird(0);
				queryScreening(0);
				//暂时没有下发卡中转queryFinInStatThesender(0);
				$("#sendCard").click();
			}
		}
		//进来第一次的时候查询全部页签，控制返回的时候，只查询当前页签条件的数据(共用的 一个查询条件页签，不控制 四个都会随着变动)
		if(!request.type){
			queryFinInStatBankcard(0);
			queryFinInStatPaytreasure(0);
			queryFinInStatWeChat(0);
			queryFinInStatThethirdparty(0);
			queryFinInthird(0);
			queryScreening(0);
			queryFinInStandbyCard(0);
			queryFinInStatSengCard(0);
			queryFinInStandbyCardd(0);
			inithancipad("handicap");
			//handicapinit();
			//暂时没有下发卡中转queryFinInStatThesender(0);
		}
	})