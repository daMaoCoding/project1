currentPageLocation = window.location.href;
	var initialStartTime =!$('#startAndEndTime').val()?moment().add(-1,'days').hours(07).minutes(0).seconds(0):$('#startAndEndTime').val().split(" - ")[0];
	var initialEndTime = !$('#startAndEndTime').val()?moment().hours(06).minutes(59).seconds(59):$('#startAndEndTime').val().split(" - ")[1];
	var initialstartAndEndTimeToArray = new Array();
	initialstartAndEndTimeToArray.push($.trim(initialStartTime.format('YYYY-MM-DD HH:mm:ss')));
	initialstartAndEndTimeToArray.push($.trim(initialEndTime.format('YYYY-MM-DD HH:mm:ss')));

//	//初始化盘口
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
//				$('<option></option>').html('全部').attr('value','0').attr('selected','selected').attr("handicapCode","").appendTo($selectHandicap);
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
	
	
	var genBankType = function(bankTypeId){
	    var ret ='<option value="">请选择</option>';
	    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
	    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true});
	    $('#'+bankTypeId+'_chosen').prop('style', 'width: 120px;')
	};
	
	var handicap4LogSearch = null;
	var account4LogSearch = null;
	var typeValue=0;
	//动态改动值，用于查询时 调用不同的方法
	function changevalue(type){
		typeValue=type;
	}
	function search(){
		$('#searhByCondition').click();
	}
	//查询按钮  根据值判断查询不同的数据源
	$('#searhByCondition').click(function () {
		if(typeValue==0){
			queryFinInStatBankcard(1);
		}else if(typeValue==1){
			queryFinInStatWeChat(1);
		}else if(typeValue==2){
			queryFinInStatPaytreasure(1);
		}else if(typeValue==3){
			queryFinInStatThethirdparty(1);
		}else if(typeValue==4){
			queryFinInStatSengCard(1);
		}else if(typeValue==5){
			queryFinInStandbyCard(1);
		}else if(typeValue==8){
			queryFinInClientCard(1);
		}
		
    });
	
	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	$('#searhByCondition').click();
	    }
	}
	
	//如果盘口为全部，层级不能选
	function changeLevel(){
		var handicap=$("#handicap").val();
			//初始化层级
			var $selectLevel =  $("select[name='level']").html('');
			$.ajax({dataType:'json',
				async:false,
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
	
	var request=getRequest();
	//记录四个页签的当前页数
	var yh="";
	var wx="";
	var zfb="";
	var dsf=""
	//入款明细银行
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
		var account=$("#account").val();
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
				"type":"Bankcard",
				"handicapname":handicapname,
				"accountOwner":accountOwner,
				"bankType":bankType,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.Bankcardarrlist.length > 0){
					     var idList=new Array();
						 //查询成功，把子页面传过来的翻页 页数清空
					     request.parentbankCurPage=null;
						 var tr = '';
						 //小计
						 var counts = 0;
						 var amounts=0;
						 var fees=0;
						 for(var index in jsonObject.data.Bankcardarrlist){
							 var val = jsonObject.data.Bankcardarrlist[index];
							 var levelnames=val.levelname;
							 idList.push({'id':val.accountId});
	                        tr += '<tr>'
	                        		  +'<td>' + (counts+1) + '</td>'
	                        	      +'<td>' + val.handicapName + '</td>'
	                        	      +'<td>'+ (val.levels==null?"": val.levels) +'</td>'
	                        	      +"<td title='编号|开户人|银行类别'>" 
		          						+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
		          						+"</a></br>"
		          						+val.alias+"|"+val.owner+"|"+val.bankType
	          					      +"</td>"
	                        	      +'<td>' + val.balance + '</td>'
	                        	      +'<td>' + val.incomeSys + '</td>'
	                        	      +'<td style="display: none;">'+val.fee+'</td>'
	                        	      +'<td>' + val.incomeCount +'</td>'
	                        	      +'<td>'
	                        	          +'<a href="#/finInStatMatch:*?id='+val.accountId+'&type=bank&handicap='+handicap+'&level='+level+'&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'&accountOwner='+encodeURI(encodeURI(accountOwner))+'&bankType='+encodeURI(encodeURI(bankType))+'" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细'
	                        	          +'</a>'
	                        	          +'<a onclick="showInOutListModal('+val.accountId+',\'false\',\''+dd+'\')" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细'
	                        	          +'</a>'
	                        	          +'&nbsp;<a onclick="showExportModal_InOut('+val.accountId+',\'income\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                          +'<i class="ace-icon fa fa-list  bigger-100 blue"></i>导出</a>'
	                        	      +'</td>'
	                        	 +'</tr>';
	                        counts +=1;
	                        amounts+=val.incomeSys;
	                        //fees+=val.fee;
	                    };
						 $('#total_tbody_Bankcard').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="5">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
									     //+'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
		                $('#total_tbody_Bankcard').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="5">总计：'+jsonObject.data.Bankcardpage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[0]).toFixed(2)+'</td>'
					                	//+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[1]+'</td>'
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
		var account=$("#account").val();
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
	var queryFinInStandbyCard=function(nb){
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
		var account=$("#account").val();
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
				showPading(jsonObject.data.standbyCardpage,"fininStatStandbyCardPage",queryFinInStandbyCard);
			}
		});
		
	}
	
	
	//入款明细客户绑定卡
	var queryFinInClientCard=function(nb){
		//当前页码
		var CurPage=$("#fininclientCardPage").find(".Current_Page").text();
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
		var account=$("#account").val();
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
				"type":"ClientCard",
				"handicapname":handicapname,
				"accountOwner":accountOwner,
				"bankType":bankType,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.clientCardlist.length > 0){
					     var idList=new Array();
						 //查询成功，把子页面传过来的翻页 页数清空
					     request.parentbankCurPage=null;
						 var tr = '';
						 //小计
						 var counts = 0;
						 var amounts=0;
						 var fees=0;
						 for(var index in jsonObject.data.clientCardlist){
							 var val = jsonObject.data.clientCardlist[index];
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
						 $('#total_tbody_clientCard').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="4">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
									     //+'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
		                $('#total_tbody_clientCard').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="4">总计：'+jsonObject.data.clientCardpage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.clientCardtotal[0]).toString().split(",")[0]+'</td>'
					                	//+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Bankcardtotal[0]).toString().split(",")[1]+'</td>'
									    +'<td colspan="2"></td>'
						         +'</tr>';
		                $('#total_tbody_clientCard').append(trn);
				}else{
					$('#total_tbody_clientCard').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.clientCardpage,"fininclientCardPage",queryFinInClientCard);
			}
		});
		
	}
	
	
	
	var handicaps=getHandicapBatchInfoByCode(); 
	//入款明细微信
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
		//获取盘口
		var handicap=$("#handicap").val();
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
			//默认查询 当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
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
				"type":"WeChat",
				"handicapname":handicapname,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.WeChatarrlist.length > 0){
					var idList=new Array();
					//查询成功，把子页面传过来的翻页 页数清空
				     request.parentwxCurPage=null;
					 var tr = '';
					 //小计
					 var counts = 0;
					 var amounts=0;
					 var fees=0;
					 for(var index in jsonObject.data.WeChatarrlist){
						 var val = jsonObject.data.WeChatarrlist[index];
						 idList.push({'id':val.id});
						 var levelnames=val.levelname;
	                        tr += '<tr>'
	                        	    +'<td>' + val.handicapname + '</td>'
	                        	    +"<td>" 
		                        	    +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+val.account
		                        	    +"</a>"
	                        	    +"</td>"
		                        	+'<td>' + val.amount + '</td>'
		                        	+'<td style="display: none;">'+val.fee+'</td>'
		                        	+'<td>' + val.counts +'</td>'
		                        	+'<td>'
		                        		+'<a href="#/finInStatMatch:*?id='+val.id+'&type=weixin&handicap='+handicap+'&level='+level+'&account='+account+'&startAndEndTime='+startAndEndTime+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'" type="button" class="btn btn-xs btn-white btn-primary btn-bold">'
		                        		+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
	                        	        +'&nbsp;<a onclick="showExportModal_InOut('+val.id+',\'wechatIncome\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                        	+'<i class="ace-icon fa fa-list  bigger-100 blue"></i>导出</a>'
		                        	+'</td>'
                        	 +'</tr>';
                        counts +=1;
                        amounts+=val.amount;
                        fees+=val.fee;
                    };
					 $('#total_tbody_WeChat').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="2">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
								     //+'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
		                $('#total_tbody_WeChat').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="2">总计：'+jsonObject.data.WeChatpage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+((new Array(jsonObject.data.WeChattotal[0]).toString().split(",")[0])*1).toFixed(2)+'</td>'
					                	//+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.WeChattotal[0]).toString().split(",")[1]+'</td>'
									    +'<td colspan="2"></td>'
						         +'</tr>';
		                $('#total_tbody_WeChat').append(trn);
					}else{
						$('#total_tbody_WeChat').empty().html('<tr></tr>');
					}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.WeChatpage,"fininStatWeChatPage",queryFinInStatWeChat);
			}
		});
		
	}
	
	
	//入款明细支付宝
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
		//获取盘口
		var handicap=$("#handicap").val();
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
			//默认查询当天7点 到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
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
					var idList=new Array();
					//查询成功，把子页面传过来的翻页 页数清空
				     request.parentzfbCurPage=null;
					 var tr = '';
					 //小计
					 var counts = 0;
					 var amounts=0;
					 var fees=0;
					 for(var index in jsonObject.data.Paytreasurearrlist){
						 var val = jsonObject.data.Paytreasurearrlist[index];
						 idList.push({'id':val.id});
						 var levelnames=val.levelname;
	                     tr += '<tr>'
	                        	    +'<td>' + val.handicapname + '</td>'
	                        	    +'<td style="display: none;">'+ levelnames +'</td>'
	                        	    +"<td>" 
		                        	    +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+val.account
		                        	    +"</a>"
	                        	    +"</td>"
		                        	+'<td>' + val.amount + '</td>'
		                        	+'<td style="display: none;">'+val.fee+'</td>'
		                        	+'<td>' + val.counts +'</td>'
		                        	+'<td>'
		                        		+'<a href="#/finInStatMatch:*?id='+val.id+'&type=zhifubao&handicap='+handicap+'&level='+level+'&account='+account+'&startAndEndTime='+startAndEndTime+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                        			+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
	                        	        +'&nbsp;<a onclick="showExportModal_InOut('+val.id+',\'aliIncome\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                        	+'<i class="ace-icon fa fa-list  bigger-100 blue"></i>导出</a>'
		                        	+'</td>'
                        	 +'</tr>';
                        counts +=1;
                        amounts+=val.amount;
                        fees+=val.fee;
                    };
					 $('#total_tbody_Paytreasure').empty().html(tr);
					 var trs = '<tr>'
									 +'<td colspan="2">小计：'+counts+'</td>'
									 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
								     //+'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
								     +'<td colspan="2"></td>'
					          +'</tr>';
		                $('#total_tbody_Paytreasure').append(trs);
		                var totalAmount=new Array(jsonObject.data.Paytreasuretotal[0]).toString().split(",")[0]*1;
		                var trn = '<tr>'
					                	+'<td colspan="2">总计：'+jsonObject.data.PaytreasurePage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+totalAmount.toFixed(2)+'</td>'
					                	//+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.Paytreasuretotal[0]).toString().split(",")[1]+'</td>'
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

var searAccount="";	
function changeAccount(account){
	searAccount=account;
	_searchSysDetail();
}
//系统明细
function _searchSysDetail(){
	var startAndEndTime = $("input[name='timeScope_SysDetail']").val();
	if(!startAndEndTime)
		initTimePicker(true,$("[name=timeScope_SysDetail]"));
	//当前页码
	var CurPage=$("#Sys_detail_foot").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//金额
	var startamount=$("#fromMoney_SysDetail").val();
	var endamount=$("#toMoney_SysDetail").val();
	//订单号
	var orderno=$("#sysAccount").val();
	//获取时间段
	//日期 条件封装
	startAndEndTime = $("input[name='timeScope_SysDetail']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	$.ajax({
		type:"post",
		url:"/r/fininstat/sysDetail",
		data:{
			"pageNo":CurPage,
			"account":searAccount,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"fromAmount":startamount,
			"toAmount":endamount,
			"orderNo":orderno,
			"type":typeValue == 2 ? "alipay" : typeValue == 1 ? "wechat" : null,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				 var tr = '';
				 //小计
				 var counts = 0;
				 var amounts=0;
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
                     tr += '<tr>'
                        	    +'<td>' + val.orderNo + '</td>'
                        	    +'<td>' +val.level    + '</td>'
                        	    +'<td>' +val.amount   + '</td>'
                        	    +'<td>' + val.crTime + '</td>'
	                        	+'<td>' + val.akTime + '</td>'
	                        	+'<td>' + val.member +'</td>'
	                        	+'<td>' + (val.status==0?"匹配中":val.status==1?"已匹配":"") +'</td>';
	                        	 if (_checkObj(val.remark)) {
	                                  if (_checkObj(val.remark).length > 10) {
	                                      tr +=
	                                          '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
	                                          + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
	                                          + ' data-content="' + _checkObj(val.remark) + '">'
	                                          + _checkObj(val.remark).substring(0, 10) + "..."
	                                          + '</a></td>';
	                                  } else {
	                                      tr +=
	                                          '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
	                                          + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
	                                          + ' data-content="' + _checkObj(val.remark) + '">'
	                                          + _checkObj(val.remark)
	                                          + '</a></td>';
	                                  }

	                              } else {
	                                  tr += '<td></td>';

	                              }
	                        	 tr +='</tr>';
                    counts +=1;
                    amounts+=val.amount;
                };
				 $('#sysdetail_body').empty().html(tr);
				 var trs = '<tr>'
								 +'<td colspan="2">小计：'+counts+'</td>'
								 +'<td bgcolor="#579EC8" style="color:white;">'+amounts.toFixed(2)+'</td>'
							     +'<td colspan="5"></td>'
				          +'</tr>';
	                $('#sysdetail_body').append(trs);
	                var trn = '<tr>'
			                	+'<td colspan="2">总计：'+jsonObject.data.page.totalElements+'</td>'
			                	+'<td bgcolor="#D6487E" style="color:white;">'+jsonObject.data.total[0]+'</td>'
							    +'<td colspan="5"></td>'
					         +'</tr>';
	                $('#sysdetail_body').append(trn);
				}else{
					$('#sysdetail_body').empty().html('<tr></tr>');
				}
			$("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.page,"Sys_detail_foot",_searchSysDetail,null,null,null,account);
		}
	});
	$('#modal-SysDetail').modal('show');
}

//流水明细
function _searchLogDetail(handicap, account) {
    handicap4LogSearch = handicap;
    account4LogSearch = account;
//    $('#timeScope_logDetail').on("mouseup", function () {
//        _datePickerForAll($('#timeScope_logDetail'));
//    });
    var startAndEndTime = $("input[name='timeScope_logDetail']").val();
	if(!startAndEndTime)
		initTimePicker(true,$("[name=timeScope_logDetail]"));
    _searchForLog();
}
function _searchForLog() {
    if (!handicap4LogSearch || !account4LogSearch) {
        return;
    }
    var type = typeValue == 2 ? 2 : typeValue == 1 ? 3 : null;
    var status = [];
    $('input[name="status_logDetail"]').each(function () {
        if ($(this).prop('checked')) {
            status.push(this.value);
        }
    });
    if (status.length == 0) {
        status.push(0);
        status.push(1);
    }
    var amountFrom = $('#fromMoney_logDetail').val();
    var amountTo = $('#toMoney_logDetail').val();
    var CurPage = $("#log_detail_foot").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var startAndEnd = $('#timeScope_logDetail').val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf(' - ') > 0) {
            startAndEnd = startAndEnd.split(' - ');
            startTime = $.trim(startAndEnd[0]);
            endTime = $.trim(startAndEnd[1]);
        }
    }
    var data = {
        "pageNo": CurPage, "pageSize": $.session.get('initPageSize'),
        "handicap": handicap4LogSearch, "account": account4LogSearch, "type": type, "status": status.toString(),
        "timeStart": startTime, "timeEnd": endTime, "amountFr": amountFrom, "amountTo": amountTo
    }
    $.ajax({
        url: '/r/newPay/account/logDetail',
        dataType: 'json',
        data: data,
        success: function (res) {
            if (res && res.data.status == 1 && res.data.data) {
                //account  amount createTime id  remark status   tradeTime
                //收款账号	付款账号	金额	状态	交易时间	创建时间	备注
                var json = res.data.data;
                var tr = '<tr>', trs = '', amount = 0;
                $('#detail_body').empty();
                var colspanNum = 1;
                if (type==1){
                    $('#to_account').show();
                    $('#to_remark').hide();
                    colspanNum =2;
                }else{
                    $('#to_remark').show();
                }
                if (json&&json.length>0) {
                    $.each(json, function (i, val) {
                        amount += parseFloat(val.amount);
                        if (type == 1) {
                            tr += '<td>' + _checkObj(val.fromAccount) + '</td>'+
                                '<td>' + _checkObj(val.toAccount) + '</td>';
                        } else {
                            tr += '<td>' + _checkObj(val.account) + '</td>';
                        }
                        tr += '<td>' + _checkObj(val.amount) + '</td>' +
                            '<td>' + _checkObj(val.status) + '</td>' +
                            '<td>' + timeStamp2yyyyMMddHHmmss(val.tradeTime) + '</td>' +
                            '<td>' + timeStamp2yyyyMMddHHmmss(val.createTime) + '</td>' +
                            '<td>' + _checkObj(val.summary) + '</td>' ;
                        if(type!=1){
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td>'
                                        + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark + '">'
                                        + _checkObj(val.remark).substring(0, 5)
                                        + '</a>'
                                        + '</td></tr>';
                                } else {
                                    tr += '<td>'
                                        + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark + '">'
                                        + _checkObj(val.remark) +
                                        +'</a>'
                                        + '</td></tr>';
                                }
                            } else {
                                tr += "<td></td></tr>";
                            }
                        }
                    });
                    trs += '<tr>' +
                        '<td id="currentCount_logDetail" colspan="'+colspanNum+'">小计：统计中..</td>' +
                        '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(amount).toFixed(3) + '</td>' +
                        '<td colspan="9"></td>' +
                        '</tr><tr><td id="currentCountTotal_logDetail" colspan="'+colspanNum+'">总共：统计中..</td>' +
                        '<td id="currentSumTotal_logDetail" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">统计中..</td>' +
                        '<td colspan="9"></td>' +
                        '</tr>';
                    $('#detail_body').empty().html(tr);
                    $('#detail_body').append(trs);
                }
                showPading(res.page, "log_detail_foot", _searchForLog);
                _searchLogCount(data);
                $("[data-toggle='popover']").popover();
            }
            $('#modal-logDetail').modal('show');
        }
    });

}
function _searchLogCount(data) {
    $.ajax({
        url: '/r/newPay/account/logDetailCountAndSum',
        dataType: 'json',
        data: data,
        success: function (res) {
            if (res && res.status == 1 && res.page) {
                $('#currentCount_logDetail').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                $('#currentCountTotal_logDetail').empty().text('合计：' + res.page.totalElements + '条记录');
                $('#currentSumTotal_logDetail').empty().text(parseFloat(res.data.sum).toFixed(3));
            }
            showPading(res.page, "log_detail_foot", _searchForLog);
        }
    });
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
		var handicap=$("#handicap").val();
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
			//默认查询当天7点到第二天6：59：59
			startAndEndTimeToArray=initialstartAndEndTimeToArray.toString();
		}
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
		                        	+'<td>'
		                        		+'<a href="#/finInStatMatch:*?id='+val.id+'&type=thirdparty&handicap='+handicap+'&level='+level+'&account='+account+'&startAndEndTime='+startAndEndTime+'&fieldval='+fieldval+'&yh='+yh+'&wx='+wx+'&zfb='+zfb+'&dsf='+dsf+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                        			+'<i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
		                        		+'&nbsp;<a onclick="showExportModal_InOut('+val.id+',\'senderCard\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                    		+'<i class="ace-icon fa fa-list  bigger-100 blue"></i>导出</a>'
		                        	+'</td>'
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
								     +'<td colspan="1"></td>'
							  +'</tr>';
		                $('#total_tbody_Thethirdparty').append(trs);
		                var trn = '<tr>'
					                	+'<td colspan="5">总计：'+jsonObject.data.thirdpartyPage.totalElements+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[0]+'</td>'
					                	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.thirdpartytotal[0]).toString().split(",")[1]+'</td>'
									    +'<td colspan="1"></td>'
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
	
	
	/**
	 * 快速搜索
	 */
	var loadFastSearchList=function (){
		//当前页码
		var CurPage=$("#FastSearch_tablePage").find(".Current_Page").text();
		var $div=$("#FastSearch_tab");
		var $filter = $("#FastSearch_accountFilter");
		if(!!!CurPage&&CurPage!=0) CurPage=$("#FastSearch_tablePage .Current_Page").text();
		var memberRealName=$.trim($filter.find("input[name='memberRealName']").val(),true);
		var memberUserName=$.trim($filter.find("input[name='memberUserName']").val(),true);
		var orderNo=$.trim($filter.find("input[name='orderNo']").val(),true);
		//查询条件都为空则不执行查询
		if(!memberUserName&&!memberRealName&&!orderNo){
			return;
		}
		//封装data
			var data =  {
		        'memberUserName': memberUserName,
		        'memberRealName': memberRealName,
		        'orderNo':orderNo,
		        "pageNo": CurPage<=0?0:CurPage-1,
		        "pageSize": $.session.get('initPageSize')
		    };
		    $.ajax({
		        type: 'POST',
		        url: '/r/income/findfastsearch',
		        data:data,
		        dataType: 'json',
		        success: function (jsonObject) {
		        	if(-1==jsonObject.status){
						showMessageForFail("查询失败："+jsonObject.message);
						return;
					}
					var $tbody=$div.find("table tbody").html(""),trStr="",toAccountList=new Array();
					var amountTotal=0;
					$.each(jsonObject.data,function(index,record){
						record.amount=record.amount?setAmountAccuracy(record.amount):0;
						toAccountList.push({"id":record.toId});
						trStr+="<tr>";
						trStr+="<td><span title='会员姓名 - 付款姓名'>"+(record.memberUserName?record.memberUserName:"无")+"&nbsp;-&nbsp;"+(record.memberRealName?record.memberRealName:"无")+"</span></td>";
						trStr+="<td>" +
								"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.toId+"' data-placement='auto left' data-trigger='hover'  >"+record.toAccount+
								"</a>" +
							"</td>";
						trStr+="<td><span>"+record.toBankType+"</span></td>";
						trStr+="<td><span>"+record.toOwner+"</span></td>";
						trStr+="<td><span>"+record.amount+"</span></td>";
						trStr+="<td><span>"+record.orderNo+"</span></td>";
						if(record.status==incomeRequestStatusMatching){
							//匹配中
							trStr+="<td><span>"+(record.operatorUid?record.operatorUid:"-")+"</span></td>";
							trStr+="<td><span>"+(record.confirmUid?record.confirmUid:"-")+"</span></td>";
							trStr+="<td><span class='label label-warning'>匹配中</span></td>";
							trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
						}else{
							//已匹配 或者已取消
							trStr+="<td><span>"+(record.operatorUid?record.operatorUid:"机器")+"</span></td>"
							trStr+="<td><span>"+(record.confirmUid?record.confirmUid:"-")+"</span></td>";;
							if(incomeRequestStatusMatched==record.status){
								trStr+="<td><span class='label label-success'>已匹配</span></td>";
							}else{
								trStr+="<td><span class='label label-inverse'>已取消</span></td>";
							}
							trStr+="<td><a title='最后更新时间："+timeStamp2yyyyMMddHHmmss(record.updateTime)+"' >"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</a></td>";
						}
						trStr+="</tr>";
						amountTotal+=record.amount*1;
					});
					$tbody.html(trStr);
					//加载账号悬浮提示
					loadHover_accountInfoHover(toAccountList);
					showPading(jsonObject.page,"FastSearch_tablePage",loadFastSearchList);
					if(jsonObject.data){
						var totalRows={
								column:12, 
								subCount:jsonObject.data.length,
								count:jsonObject.page.totalElements,
								[5]:{subTotal:amountTotal,total:jsonObject.page.header.totalAmount}
							};
						showSubAndTotalStatistics4Table($tbody,totalRows);
					}
		        }
		    });
	}
	
	/**
	 * 导出弹出框初始化
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
		$(e).attr("href",url);
	}
	jQuery(function($) {
		//先初始化盘口，返回时才能获取盘口信息 进行默认值
		inithancipad();
		genBankType('bankType');
		//返回按钮处理，设置查询时的值 ,共用的一个查询条件页头   设置当前页面选的值
		if(request&&request.handicap){
			var counts=$("#handicap option").length;
			for(var i=0;i<counts;i++){
			   if($("#handicap").get(0).options[i].value == request.handicap){
				  $("#handicap").get(0).options[i].selected = true; 
				  break; 
			   } 
			}
			//如果有盘口的信息，则根据盘口信息 初始化层级
			changeLevel();
		}if(request&&request.level){
			var counts=$("#level option").length;
			for(var i=0;i<counts;i++){
			   if($("#level").get(0).options[i].value == request.level){
				  $("#level").get(0).options[i].selected = true; 
				  break;  
			   } 
			}
		}if(request&&request.account){
			$("#account").val(request.account);
		}if(request&&request.accountOwner&&request.accountOwner!="undefined"){
			$("#accountOwner").val(decodeURI(decodeURI(request.accountOwner)));
		}if(request&&request.bankType&&request.bankType!="undefined"){
			$("#bankType").val(decodeURI(decodeURI(request.bankType)));
		}
		
		choiceTimeClearAndSearch($('#startAndEndTime'),"form-field-checkbox",$('#searhByCondition'));
		choiceTimeClearAndSearch($('[name=startAndEndTime_sys]'),"parentfieldval_sys");
		choiceTimeClearAndSearch($('[name=startAndEndTime_bank]'),"parentfieldval_bank");
		//返回时默认填充值
		if(request&&request.fieldval&&request.fieldval!="undefined"){
			$("[name=form-field-checkbox][value="+request.fieldval+"]").prop("checked",true);
			$("input[name='startAndEndTime']").val("");
		}else if(request&&request.startAndEndTime){
			$("input[name='startAndEndTime']").val(request.startAndEndTime);
		}
		//时间初始化
		if(!$('#startAndEndTime').val() && $('input:radio[name="form-field-checkbox"]:checked').val()){
			initTimePicker(false,null,typeCustomLatestOneDay);
	    }else{
	    	clearRadioValue("form-field-checkbox");
	    	initTimePicker(true,null,typeCustomLatestOneDay,request.startAndEndTime);
	    }
		//明细返回的时候 判断 默认在哪个页签
		if(request&&request.type){
			if(request.type=="bank"){
				//传入参数来判断 是否要根据条件去查询数据。
				queryFinInStatBankcard(1);
				queryFinInStatSengCard(0);
				queryFinInStandbyCard(0);
				queryFinInClientCard(0);
				queryFinInStatWeChat(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatThethirdparty(0);
				$("#bank").click();
			}else if(request.type=="weixin"){
				queryFinInStatWeChat(1);
				queryFinInStatSengCard(0);
				queryFinInStandbyCard(0);
				queryFinInStatBankcard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatThethirdparty(0);
				queryFinInClientCard(0);
				$("#weixin").click();
			}else if(request.type=="zhifubao"){
				queryFinInStatPaytreasure(1);
				queryFinInStatSengCard(0);
				queryFinInStandbyCard(0);
				queryFinInStatWeChat(0);
				queryFinInStatBankcard(0);
				queryFinInStatThethirdparty(0);
				queryFinInClientCard(0);
				$("#zhifubao").click();
			}else if(request.type=="thirdparty"){
				queryFinInStatThethirdparty(1);
				queryFinInStatSengCard(0);
				queryFinInStandbyCard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInStatBankcard(0);
				queryFinInClientCard(0);
				$("#thirdpartypay").click();
			}else if(request.type=="sendcard"){
				queryFinInStatThethirdparty(0);
				queryFinInStatSengCard(1);
				queryFinInStandbyCard(0);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInStatBankcard(0);
				queryFinInClientCard(0);
				$("#sendCard").click();
			}else if(request.type=="standbyCard"){
				queryFinInStatThethirdparty(0);
				queryFinInStatSengCard(0);
				queryFinInStandbyCard(1);
				queryFinInStatPaytreasure(0);
				queryFinInStatWeChat(0);
				queryFinInStatBankcard(0);
				queryFinInClientCard(0);
				$("#standbyCard").click();
			}
			//如果返回之后不 点击其它页签的查询 直接点击明细，再次返回的时候 带不出来当前页，所以在第一次返回的时候就记录一下，防止返回后直接点击明细
//			yh=request.parentbankCurPage;
//			wx=request.parentwxCurPage;
//			zfb=request.parentzfbCurPage;
//			dsf=request.parentthirdCurPage;
		}
		
		//进来第一次的时候查询全部页签，控制返回的时候，只查询当前页签条件的数据(共用的 一个查询条件页签，不控制 四个都会随着变动)
		if(!request.type){
			queryFinInStatBankcard(0);
			queryFinInStatSengCard(0);
			queryFinInStatThethirdparty(0);
			queryFinInStatWeChat(0);
			queryFinInStatPaytreasure(0);
			queryFinInStandbyCard(0);
			queryFinInClientCard(0);
//			//初始化的时候记录当前页的信息
//			yh=$("#fininStatBankcardPage").find(".Current_Page").text();
//			wx=$("#fininStatWeChatPage").find(".Current_Page").text();
//			zfb=$("#fininStatPaytreasurePage").find(".Current_Page").text();
//			dsf=$("#fininStatThethirdpartyPage").find(".Current_Page").text();
		}
	})