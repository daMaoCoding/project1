currentPageLocation = window.location.href;
var reaccountid="";
var rehandicapid="";
	//清空盘口统计StartAndEndTime的值
	function clearStartAndEndTimeHandicap(){
		$("input[name='startAndEndTimeHandicap']").val('');
		//点击直接查询
		queryHandicap();
	}
	
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
			exportOut(reaccountid,rehandicapid);
	    });
	    exportStartAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}
	
	
	var genBankType = function(bankTypeId){
	    var ret ='<option value="">请选择</option>';
	    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
	    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true});
	    $('#'+bankTypeId+'_chosen').prop('style', 'width: 120px;')
	};
	
	//如果盘口为全部，层级不能选
	function changeLevel(){
		var handicap=$("#search_EQ_handicapId").val();
		if(handicap==""){
			$("#search_EQ_LevelId").attr("disabled",true);
		}else{
			//初始化层级
			var $selectLevel =  $("select[name='search_EQ_LevelId']").html('');
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
			$("#search_EQ_LevelId").attr("disabled",false);
		}
	}
	bootbox.setLocale("zh_CN");
//	var inithancipad=function(name){
//		//初始化盘口
//		var $selectHandicap = $("select[name='"+name+"']").html("");
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

	//查询按钮
	$('#searhByCondition').click(function () {
		queryAccountStatistics();
    });
	
	//绑定按键事件，回车查询数据
	$('#finOutStatTab').bind('keypress',getKeyCode);   
	function getKeyCode(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryAccountStatistics();
	    }
	}
	
	//绑定按键事件，回车查询数据
	$('#handicapTab').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryHandicap();
	    }
	}
	
	
	var request=getRequest();
	var zh="";
	var pk="";
	//查询账号统计	
	var queryAccountStatistics=function(){
		$("#loadingModal").modal('show');
		//获取账号
		//获取盘口
		var handicap=$("#search_account_handicapId").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		var account=$("#account").val();
		//获取开户人
		var accountOwner=$.trim($("#accountOwner").val());
		//获取银行类别
		var bankType=$.trim($("#bankType").val());
		//当前页码
		var CurPage=$("#finOutStatPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//如果是返回操作 则默认是操作之前的页码
		if(request&&request.parentzhCurPage){
			CurPage=request.parentzhCurPage;
		}
		zh=CurPage;
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
		//获取账号类型
		var cartype=$('input:radio[name="carType"]:checked').val();
		var dd="";
		if(fieldval!=""&&fieldval!=null){
			dd=changeDate(fieldval);
		}else{
			dd=startAndEndTime;
		}
		$.ajax({
			type:"post",
			//url:"/r/finoutstat/accountstatistics",
			url:"/r/finoutstat/accountStatisticsFromClearDate",
			data:{
				"pageNo":CurPage,
				"account":account,
				"accountOwner":accountOwner,
				"bankType":bankType,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"handicap":handicap,
				"cartype":cartype,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				     //查询成功后清空子页面带过来的页码，使用自己页码的页码查询
					 request.parentzhCurPage=null;
					 var tr = '';
					 //小计
					 var counts = 0;
					 var bankamounts=0;
					 var bankfees=0;
					 var tradingamounts=0;
					 var tradingfees=0;
					 var idList=new Array();
					 for(var index in jsonObject.data.arrlist){
						 var val = jsonObject.data.arrlist[index];
						 idList.push({'id':val.accountId});
                         tr += '<tr>'
                        	      +'<td>'+(counts+1)+'</td>'
                        	 	  +"<td>"+val.handicapName+"</td>"
                        	      +"<td title='编号|开户人|银行类别'>" 
	                        	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
	                        	      +"</a></br>"
	                        	      +val.alias+"|"+val.owner+"|"+val.bankType
                        	      +"</td>"
                        	      +'<td>'+val.balance+'</td>'
                        	      +'<td>' + Math.abs(val.outward) + '</td>'
                        	      +'<td>' + val.outwardCount + '</td>'
                        	      +'<td>' +Math.abs(val.outwardSys) +'</td>'
                        	      +'<td>'+val.outwardSysCount+'</td>'
                        	      +'<td>' + Math.abs(val.fee) + '</td>'
                        	      +'<td>'+val.feeCount+'</td>'
                        	      +'<td>'+val.los+'</td>'
                        	      +'<td>'+val.losCount+'</td>'
                        	      +'<td>'
                        	              +'<a href="#/finOutStatSys:*?accountid='+val.accountId+'&account='+account+'&startAndEndTime='+dd+'&fieldval='+fieldval+'&zhCurPage='+zh+'&pkCurPage='+pk+'&accountOwner='+encodeURI(encodeURI(accountOwner))+'&bankType='+encodeURI(encodeURI(bankType))+'&handicap='+handicap+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                          +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</a>' 
                                          +'<a onclick="showInOutListModal('+val.accountId+',\'false\',\''+dd+'\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                          +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>银行明细</a>'
                                          +'<a onclick="showExportModal_InOut('+val.accountId+',\'outward\')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
                                          +'<i class="ace-icon fa fa-list  bigger-100 blue"></i>导出</a>'
                                  +'</td>'
                               +'</tr>';
                         counts +=1;
                         bankamounts+=Math.abs(val.outward);
                         bankfees+=Math.abs(val.fee);
                         tradingamounts+=val.outwardSys;
                         tradingfees+=val.tradingfee;
                     };
					 $('#total_tbody').empty().html(tr);
					 var trs = '<tr>'
						            +'<td colspan="4">小计：'+counts+'</td>'
						            +'<td bgcolor="#579EC8" style="color:white;">'+bankamounts+'</td>'
						            +'<td colspan="1"></td>'
					                +'<td bgcolor="#579EC8" style="color:white;">'+Math.abs(tradingamounts)+'</td>'
					                +'<td colspan="1"></td>'
						            +'<td bgcolor="#579EC8" style="color:white;">'+bankfees+'</td>'
					                +'<td colspan="5"></td>'
					         +'</tr>';
                 $('#total_tbody').append(trs);
                 var trn = '<tr>'
                	            +'<td colspan="4">总计：'+jsonObject.data.page.totalElements+'</td>'
                	            +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.total[0]).toString().split(",")[0])+'</td>'
                	            +'<td colspan="1"></td>'
                	            +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[2]+'</td>'
				                +'<td ></td>'
				                +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.total[0]).toString().split(",")[1])+'</td>'
				                +'<td colspan="5"></td>'
				          +'</tr>';
                 $('#total_tbody').append(trn);
				}else{
					$('#total_tbody').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.page,"finOutStatPage",queryAccountStatistics);
				$("#loadingModal").modal('hide');
			}
		});
	}
	
	//盘口统计查询
	$('#searhByConditionHandicap').click(function () {
		queryHandicap();
    });
	//查询盘口统计
	var queryHandicap=function(){
		//获取盘口
		var handicap=$("#search_EQ_handicapId").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		//获取层级
		var levelp=$("#search_EQ_LevelId").val();
		if(levelp=="" || levelp==null){
			levelp=0;
		}
		//当前页码
		var CurPage=$("#finHandicapPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//如果是返回操作 则默认是操作之前的页码
		if(request&&request.parentpkCurPage){
			CurPage=request.parentpkCurPage;
		}
		pk=CurPage;
		//获取时间段
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTimeHandicap']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		//获取查询范围
		var fieldvalHandicap=$('input:radio[name="form-field-checkboxHandicap"]:checked').val();
		$.ajax({
			type:"post",
			//url:"/r/finoutstat/accountstatisticshandicap",
			url:"/r/finoutstat/accountstatisticshandicapFromClearDate",
			data:{
				"pageNo":CurPage,
				"handicap":handicap,
				"level":levelp,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldvalHandicap":fieldvalHandicap,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
					 //成功后清空子页面带过来的页码信息，使用自己页码的页码
					 request.parentpkCurPage=null;
					 var tr = '';
					 //小计
					 var counts = 0;
					 var amounts=0;
					 var fees=0;
					 for(var index in jsonObject.data.arrlist){
						 var val = jsonObject.data.arrlist[index];
						 var level =val.level;
						 if(level=='0'){
							 level='全部';
						 }else{
							 level=val.levelname;
						 }
                         tr += '<tr>'
                        	      +'<td>' + val.handicapName + '</td>'
                        	      //+'<td>'+ level +'</td>'
                        	      +'<td>' + val.outwardSys + '</td>'
                        	      +'<td>' + Math.abs(val.fee) + '</td>'
                        	      +'<td>'+val.outwardSysCount+'</td>';
                         //如果是统计查询就传盘口过去，在根据盘口查询到对应的id  去查相关的信息,如果不是统计查询就直接传id过去查询相关的信息?a=a&b=b&c=c&d=d 
                         //if(val.level=='0'){
                        	 tr +='<td>'
                        		 	+'<a href="#/finOutHandicap:*?handicap='+val.handicapId+'&typee='+"byhandicap"+'&handicapp='+handicap+'&level='+levelp+'&startAndEndTime='+startAndEndTime+'&fieldvalHandicap='+fieldvalHandicap+'&zhCurPage='+zh+'&pkCurPage='+pk+'" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
                        		 	+'&nbsp;&nbsp;<a class="btn btn-xs btn-white btn-primary btn-bold" onclick="showMoal(0,'+val.handicapId+');">'
										+'<i class="fa fa-cloud-download" aria-hidden="true"></i>&nbsp;&nbsp;导出'
								    +'</a>'
                        		 	+'</td>'
                        	  +'</tr>';
                        // }else{
                        	 //tr +='<td>'
                        		 	//+'<a href="#/finOutHandicap:*?id='+val.id+'&typee='+"byid"+'&handicap='+handicap+'&level='+levelp+'&startAndEndTime='+startAndEndTime+'&fieldvalHandicap='+fieldvalHandicap+'&zhCurPage='+zh+'&pkCurPage='+pk+'" type="button" class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</a>'
                        		// +'</td>'
                        	 // +'</tr>';
                        // }
                         counts +=1;
                         amounts+=val.outwardSys;
                         fees+=Math.abs(val.fee);
                     };
					 $('#total_tbodyHandicap').empty().html(tr);
					 var trs = '<tr>'
						           +'<td colspan="1">小计：'+counts+'</td>'
						           +'<td bgcolor="#579EC8" style="color:white;">'+amounts+'</td>'
						           +'<td bgcolor="#579EC8" style="color:white;">'+fees+'</td>'
					               +'<td colspan="2"></td>'
					          +'</tr>';
	                 $('#total_tbodyHandicap').append(trs);
//	                 var trn = '<tr>'
//	                	           +'<td colspan="1">总计：'+jsonObject.data.page.totalElements+'</td>'
//	                	           +'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[0]+'</td>'
//	                	           +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.total[0]).toString().split(",")[1])+'</td>'
//					               +'<td colspan="2"></td>'
//					           +'</tr>';
//	                 $('#total_tbodyHandicap').append(trn);
				}else{
					$('#total_tbodyHandicap').empty().html('<tr></tr>');
				}
				//分页初始化
				showPading(jsonObject.data.page,"finHandicapPage",queryHandicap);
			}
		});
		
	}
	
	
	function _searchQuickQuery() {
	    var member_QuickQuery = $('#member_QuickQuery').val();
	    var orderNo_QuickQuery = $('#orderNo_QuickQuery').val();
	    var CurPage = $("#QuickQuery_footPage").find(".Current_Page").text();

	    if (!CurPage) {
	        CurPage = 0;
	    } else {
	        CurPage = CurPage - 1;
	    }
	    if (CurPage < 0) {
	        CurPage = 0;
	    }
	    $.ajax({
	        url:'/r/out/quickQuery',
	        type:'get',
	        data:{
	        	"member":$.trim(member_QuickQuery),
	        	"orderNo":$.trim(orderNo_QuickQuery), 
	        	"pageNo": CurPage, 
	        	"pageSize": $.session.get('initPageSize')
	        },
	        dataType:'json',
	        success:function (res) {
	            if (res){
	                var tr = '';
	                var trs = '';
	                if (res.status==1) {
	                    var amount1 = 0;
	                    var amount2 = 0;
	                    if (res.data && res.data.retList && res.data.retList.length>0) {
	                        $(res.data.retList).each(function (i,val) {
	                            tr +='<tr><td>'+val.handicap+'</td>'+
	                                '<td>'+val.level+'</td>'+
	                                '<td>'+val.reqMember+'</td>'+
	                                '<td>'+val.orderNo+'</td>'+
	                                '<td>'+val.reqAmount+'</td>'+
	                                '<td>'+val.taskAmount+'</td>'+
	                                '<td>'+_showReqStatus(val.reqStatus)+'</td>'+
	                                '<td>'+_showTaskStatus(val.taskStatus)+'</td>'+
	                                '<td>'+timeStamp2yyyyMMddHHmmss(val.reqUpdateTime)+'</td>'+
	                                '<td>'+timeStamp2yyyyMMddHHmmss(val.taskAsignTime)+'</td>'+
	                                '<td >'
                                        + '<a class="bind_hover_card breakByWord"  title="备注信息" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="'+val.taskRemark+'">'
                                        	+"...."
                                        + '</a>'
                                  + '</td></tr>';
	                            amount1 += parseFloat(val.reqAmount);
	                            amount2 += parseFloat(val.taskAmount);
	                        });
	                        trs =
	                            '<tr><td  colspan="4">小计:'+((parseInt(res.page.totalElements/res.page.pageSize)>= res.page.pageNo)?res.page.pageSize: (res.page.totalElements%res.page.pageSize))+'条记录</td>' +
	                            '<td bgcolor="#579EC8" style="color:white;width: 150px;">' + parseFloat(amount1).toFixed(3) + '</td>'+
	                            '<td bgcolor="#579EC8" style="color:white;width: 150px;">' + parseFloat(amount2).toFixed(3) + '</td></tr>' +
	                            '<td colspan="11"></td></tr>';
	                    }
	                }
	                $('#tbody_QuickQuery').empty().html(tr).append(trs);
	                $("[data-toggle='popover']").popover();
	                showPading(res.page, 'QuickQuery_footPage', _searchQuickQuery);
	            }

	        }
	    });

	}
	
	function _showReqStatus(obj) {
	    var status = '';
	    if(obj==0) {
	        obj = 10110000;
	    }
	    switch (obj){
	        case 10110000:
	            status= "正在审核";
	            break;
	        case 1:
	            status= "审核通过";
	            break;
	        case 2:
	            status= "拒绝";
	            break;
	        case 3:
	            status= "主管处理";
	            break;
	        case 4:
	            status= "已取消";
	            break;
	        case 5:
	            status= "出款成功，平台已确认";
	            break;
	        case 6:
	            status= "出款成功，与平台确认失败";
	            break;
	        default:
	            status = '';
	            break;
	    }
	    return status;
	}
	
	function _showTaskStatus(obj) {
	    var status = '';
	    if(obj==0) {
	        obj = 10110000;
	    }
	    switch (obj){
	        case 10110000:
	            status= "未出款";
	            break;
	        case 1:
	            status= "已出款";
	            break;
	        case 2:
	            status= "主管处理";
	            break;
	        case 3:
	            status= "主管取消";
	            break;
	        case 4:
	            status= "主管拒绝";
	            break;
	        case 5:
	            status= "流水匹配";
	            break;
	        case 6:
	            status= "出款失败";
	            break;
	        case 7:
	            status= "无效记录，已重新出款";
	            break;
	        case 8:
	            status= "银行维护";
	            break;
	        default:
	            status = '';
	            break;
	    }
	    return status;
	}
	
	
	var exportAll=function(){
		var $div=$("#choiceExportModal");
		//直接选中a标签无法点击，需要选择器选中内容
//		$div.find("a#sysLink").children().click();

//		setInterval(function(){
//			$div.find("a#bankLogLink").children().click();
//		}, 1);
		window.location.href="/r/account/exportoutwardSys";
		window.location.href="/r/account/exportBankLog";
//		exportBankLogInterval.clearInterval();
		
		
	}
	
	function changeHandicap(){
		exportOut(reaccountid,rehandicapid);
	}
	
	function showMoal(accountid,handicapid){
		reaccountid=accountid;
		rehandicapid=handicapid;
		$('#exportParagraph').modal('toggle');
		exportOut(accountid,handicapid);
	}
	
	/**
	 * 导出弹出框初始化
	 */
	var showModal_exportChoice=function(accountId){
		var $div=$("#choiceExportModal");
		$div.find("#accountId").val(accountId);
		$div.modal("toggle");
		$div.on('hidden.bs.modal', function () {
			//关闭窗口重新初始化
			$div.find("#accountId").val("");
			initTimePicker(true,$("[name=startAndEndTime_sys]"));
			initTimePicker(true,$("[name=startAndEndTime_bank]"));
			$("[name=parentfieldval_sys]:checked").prop("checked",false);
			$("[name=parentfieldval_bank]:checked").prop("checked",false);
		});
	}
	var beforeExport=function(e,serytype,url){
		var $div=$("#widget_"+serytype);
		var accountId=$("#choiceExportModal #accountId").val();
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
		$(e).attr("href",url);
	}
	
	jQuery(function($) {
		//要先初始化盘口，才能循环到盘口的信息，
		inithancipad("search_EQ_handicapId");
		inithancipad("search_account_handicapId");
		//返回按钮处理，设置查询时的值  账号统计
		if(request&&request.account){
			$("#account").val(request.account);
		}
		if(request&&request.accountOwner){
			$("#accountOwner").val(decodeURI(decodeURI(request.accountOwner)));
		}
		if(request&&request.bankType){
			$("#bankType").val(decodeURI(decodeURI(request.bankType)));
		}
		//返回按钮处理，设置查询时的值  盘口统计
		//盘口详情返回的时候默认在盘口页签
		if(request&&request.type){
			$("#handicaptab").click();
		}if(request&&request.parenthandicap){
			var counts=$("#search_EQ_handicapId option").length;
			for(var i=0;i<counts;i++){
			   if($("#search_EQ_handicapId").get(0).options[i].value == request.parenthandicap){
			      $("#search_EQ_handicapId").get(0).options[i].selected = true; 
			      break; 
		       } 
			}
			//如果有盘口的信息，则根据盘口信息 初始化层级
			changeLevel();
		}if(request&&request.handicapAccount){
			var counts=$("#search_account_handicapId option").length;
			for(var i=0;i<counts;i++){
			   if($("#search_account_handicapId").get(0).options[i].value == request.handicapAccount){
			      $("#search_account_handicapId").get(0).options[i].selected = true; 
			      break; 
		       } 
			}
		}if(request&&request.parentlevel){
			var counts=$("#search_EQ_LevelId option").length;
			for(var i=0;i<counts;i++){
			   if($("#search_EQ_LevelId").get(0).options[i].value == request.parentlevel){
			      $("#search_EQ_LevelId").get(0).options[i].selected = true; 
			      break;  
		       } 
			}
		}
		choiceTimeClearAndSearch($('#startAndEndTime'),"form-field-checkbox",$('#searhByCondition'));
		choiceTimeClearAndSearch($('#startAndEndTimeHandicap'),"form-field-checkboxHandicap",$('#searhByConditionHandicap'));
		choiceTimeClearAndSearch($('[name=startAndEndTime_sys]'),"parentfieldval_sys");
		choiceTimeClearAndSearch($('[name=startAndEndTime_bank]'),"parentfieldval_bank");
		//返回时默认填充值
		if(request&&request.fieldval&&request.fieldval!="undefined"){
			$("[name=form-field-checkbox][value="+request.fieldval+"]").prop("checked",true);
			$("input[name='startAndEndTime']").val("");
		}else if(request&&request.startAndEndTime){
			$("input[name='startAndEndTime']").val(request.startAndEndTime);
		}
		if(request&&request.parentfieldval&&request.parentfieldval!="undefined"){
			$("[name=form-field-checkboxHandicap][value="+request.parentfieldval+"]").prop("checked",true);
			$("input[name='startAndEndTimeHandicap']").val("");
		}else if(request&&request.parentstartAndEndTime){
			$("input[name='startAndEndTimeHandicap']").val(request.parentstartAndEndTime);
		}
		//时间初始化
		if($('input:radio[name="form-field-checkbox"]:checked').val() && !$('#startAndEndTime').val()){
	    	initTimePicker(false,null,typeCustomLatestOneDay);
	    }else{
	    	clearRadioValue("form-field-checkbox");
	    	initTimePicker(true,null,null,request.startAndEndTime);
	    }
		//时间初始化
		if(!$('#startAndEndTimeHandicap').val() && $('input:radio[name="form-field-checkboxHandicap"]:checked').val()){
			initTimePicker(false,$("[name=startAndEndTimeHandicap]"),typeCustomLatestToday);
	    }else{
	    	clearRadioValue("form-field-checkboxHandicap");
	    	initTimePicker(true,$("[name=startAndEndTimeHandicap]"),null,request.parentstartAndEndTime);
	    }
		
		queryAccountStatistics();
		queryHandicap();
		exportHandicap();
		_searchQuickQuery();
		initTimePickerHandicap();
		genBankType('bankType');
		//exportOut();
	})