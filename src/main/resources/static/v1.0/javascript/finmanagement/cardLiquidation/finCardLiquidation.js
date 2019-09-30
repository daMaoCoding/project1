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
	
	//查询按钮
	$('#searhByCondition').click(function () {
		queryFinCardLiquidationcard(0);
    });
	
	//清空账号统计StartAndEndTime的值
	function clearStartAndEndTime(){
		$("input[name='startAndEndTime']").val('');
		//点击直接查询
		queryFinCardLiquidationcard(0);
	}
	
	var typeValue=0;
	//动态改动值，用于查询时 调用不同的方法
	function changevalue(type){
		typeValue=type;
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
	
	var genBankType = function(bankTypeId){
	    var ret ='<option value="">请选择</option>';
	    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
	    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true});
	    $('#'+bankTypeId+'_chosen').prop('style', 'width: 120px;')
	};
	
	function searQuery(){
		$('#searhByCondition').click();
	}
	
	var request=getRequest();
	var yh="";
	var zfb="";
	var wx="";
	var dsf=""
	//出入卡清算
	var queryFinCardLiquidationcard=function(nb){
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
		//获取账号类型
		var cartype=$('input:radio[name="carType"]:checked').val();
		//获取账号状态
		var status=$('input:radio[name="status"]:checked').val();
		$.ajax({
			type:"post",
			url:"/r/fintransstat/fincardliquidation",
			data:{
				"pageNo":CurPage,
				"account":account,
				"startAndEndTimeToArray":startAndEndTimeToArray,
				"fieldval":fieldval,
				"type":"Bankcard",
				"accountOwner":accountOwner,
				"bankType":bankType,
				"handicap":handicap,
				"cartype":cartype,
				"status":status,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.CardLiquidationarrlist.length > 0){
						 var tr = '';
						 //小计
						 var counts = 0;
						 var bankamounts=0;
						 var bankfees=0;
						 var tradingamounts=0;
						 var tradingfees=0;
						 var bankBlance=0;
						 var income=0;
						 var incomeSys=0;
						 var los=0;
						 var minusBalance=0;
						 var minusIncome=0;
						 var minusIncomeSys=0;
						 var idList=new Array();
						 for(var index in jsonObject.data.CardLiquidationarrlist){
							 var val = jsonObject.data.CardLiquidationarrlist[index];
							 idList.push({'id':val.accountId});
	                        tr += '<tr>'
	                        	+"<td>"+val.handicapName+"</td>"
	                      	      +"<td title='编号|开户人|银行类别'>" 
		                        	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.accountId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
		                        	      +"</a></br>"
		                        	      +val.alias+"|"+val.owner+"|"+val.bankType
	                      	      +"</td>"
	                      	      +'<td>'+(val.status=='在用'?'<span class="label label-sm label-success">在用</span>':(val.status=='可用'?'<span class="label label-sm label-purple">可用</span>':(val.status=='停用')?'<span class="label label-sm label-danger">停用</span>':'<span class="label label-sm label-danger">冻结</span>'))+'</td>'
	                      	      +'<td>'+returnType(val.accountType)+'</td>'
	                      	      +'<td>'+ (val.accountType==8?((val.balance).toFixed(2)):(val.balance.toFixed(2)))+'</td>'
	                      	      +'<td>' + (val.accountType==8?(Math.abs(val.income).toFixed(2)):(Math.abs(val.income).toFixed(2))) + '</td>'
	                      	      +'<td>' + (val.accountType==8?(val.incomeCount):(val.incomeCount)) + '</td>'
	                      	      +'<td>' +(val.accountType==8?(Math.abs(val.incomeSys).toFixed(2)):(Math.abs(val.incomeSys).toFixed(2))) +'</td>'
	                      	      +'<td>'+ (val.accountType==8?(val.incomeSysCount):(val.incomeSysCount))+'</td>'
	                      	      +'<td>' + Math.abs(val.outward).toFixed(2) + '</td>'
	                      	      +'<td>' + val.outwardCount + '</td>'
	                      	      +'<td>' +Math.abs(val.outwardSys).toFixed(2) +'</td>'
	                      	      +'<td>'+val.outwardSysCount+'</td>'
	                      	      +'<td>' + Math.abs(val.fee) + '</td>'
	                      	      +'<td>'+val.feeCount+'</td>'
	                      	      +'<td>'+Math.abs(val.los)+'</td>'
	                      	      +'<td>'+val.losCount+'</td>'
	                      	      +'<td><button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal('+val.accountId+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button></td>'
                              +'</tr>';
	                        counts +=1;
	                        bankamounts+=Math.abs(val.outward);
	                        bankBlance+=val.balance;
	                        bankfees+=Math.abs(val.fee);
	                        income+=Math.abs(val.income);
	                        incomeSys+=Math.abs(val.incomeSys);
	                        tradingamounts+=val.outwardSys;
	                        los+=Math.abs(val.los);
	                        if(val.accountType==8){
	                        	minusBalance+=(val.balance);
	                        	minusIncome+=(val.income);
	                        	minusIncomeSys+=(val.incomeSys);
	                        }
	                    };
						 $('#total_tbody_Bankcard').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="4">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+(bankBlance-minusBalance).toFixed(2)+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+(income-minusIncome).toFixed(2)+'</td>'
										 +'<td></td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+(incomeSys-minusIncomeSys).toFixed(2)+'</td>'
									     +'<td></td>'
									     +'<td bgcolor="#579EC8" style="color:white;">'+bankamounts.toFixed(2)+'</td>'
									     +'<td></td>'
									     +'<td bgcolor="#579EC8" style="color:white;">'+tradingamounts.toFixed(2)+'</td>'
									     +'<td></td>'
									     +'<td bgcolor="#579EC8" style="color:white;">'+bankfees.toFixed(2)+'</td>'
									     +'<td></td>'
									     +'<td bgcolor="#579EC8" style="color:white;">'+los.toFixed(2)+'</td>'
									     +'<td colspan="2"></td>'
						          +'</tr>';
	                    $('#total_tbody_Bankcard').append(trs);
	                    var trn = '<tr>'
				                    	+'<td colspan="4">总计：'+jsonObject.data.CardLiquidationpage.totalElements+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+(Math.abs(new Array(jsonObject.data.CardLiquidationtotal[0]).toString().split(",")[3]).toFixed(2)-Math.abs(new Array(jsonObject.data.minusDate[0]).toString().split(",")[0]).toFixed(2)).toFixed(2)+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+(Math.abs(new Array(jsonObject.data.CardLiquidationtotal[0]).toString().split(",")[0]).toFixed(2)-Math.abs(new Array(jsonObject.data.minusDate[0]).toString().split(",")[1]).toFixed(2)).toFixed(2)+'</td>'
								        +'<td></td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+(Math.abs(new Array(jsonObject.data.CardLiquidationtotal[0]).toString().split(",")[5]).toFixed(2)-Math.abs(new Array(jsonObject.data.minusDate[0]).toString().split(",")[2]).toFixed(2)).toFixed(2)+'</td>'
								        +'<td></td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.CardLiquidationtotal[0]).toString().split(",")[1]).toFixed(2)+'</td>'
								        +'<td></td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.CardLiquidationtotal[0]).toString().split(",")[6]).toFixed(2)+'</td>'
								        +'<td></td>'
								        +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.CardLiquidationtotal[0]).toString().split(",")[2]).toFixed(2)+'</td>'
								        +'<td></td>'
									    +'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.CardLiquidationtotal[0]).toString().split(",")[4]).toFixed(2)+'</td>'
									    +'<td colspan="2"></td>'
					             +'</tr>';
	                    $('#total_tbody_Bankcard').append(trn);
				}else{
					$('#total_tbody_Bankcard').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.CardLiquidationpage,"fininStatBankcardPage",queryFinCardLiquidationcard);
				$("#loadingModal").modal('hide');
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
			 vtype="出款卡第三方";
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
	
	
	jQuery(function($) {
		//返回按钮处理，设置查询时的值 ,共用的一个查询条件页头   设置当前页面选的值
		inithancipad("handicap");
		genBankType('bankType');
		//handicapinit();
		initTimePicker(false,null,typeCustomLatestOneDay);
		queryFinCardLiquidationcard(0);
	})