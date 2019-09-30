var accoutStatusHTMLMap={};
var accountIdArrayForTotal = [];
var currentPageLiAccountId = null;
var lockUpTotalMatchedAmount = false;
var generateIncomeOrder = null;//补提单标识，防止连续确定发起重复请求。
$.each(ContentRight['Income:*'], function (name, value) {
    if (name == 'Income:currentpageSum:*') {
        incomeCurrentPageSum=true;
    }
    if (name =='Income:allRecordSum:*'){
        incomeAllRecordSum=true;
    }
});
/**
 * 正在匹配/已匹配/未认领/已取消 Tab切换
 */
var changeTabInit=function(tabType){
	$("#incomeTabType").val(tabType);
	if(tabType=='Matching'){
		loadIncomeAccountTab(0);
	}else if(tabType=='Matched'){
		loadMatchedList(0);
	}else if(tabType=='BankLogUnMatch'){
		loadBankLogUnMatchList(0);
	}else if(tabType=='Canceled'){
		loadCancelList(0);
	}
}
/**
 * 正在匹配 入款卡 Tab切换
 */
var changeTabAccountMatching=function(incomeId){
	$("#choseAccount").val(incomeId);
	loadAccoutStatusHTML(incomeId);
	loadincomeMatchingList(0);
	loadBankLogMatchingList(0);
    currentPageLiAccountId = $("#choseAccount").val();
}
/**
 * 根据指定incomeId加载div内容
 */
var loadAccoutStatusHTML=function(incomeId){
	$("#accoutStatusInfo").html("");
	$("#accoutStatusInfo").html(accoutStatusHTMLMap[incomeId]);
	
}
/**
 * 提单||流水 查询范围
 */
var choiceLoadIncomeBanklog=function(){
	$.each($("#Matching_tab [name=search_range]"),function(index,result){
		if($(result).is(":checked")&&$(result).val()=='income'){
			loadincomeMatchingList();
		}else if($(result).val()=='banklog'){
			loadBankLogMatchingList();
		}
	});
}

/**
 * 发送消息给主管，与新增备注的公用方法
 */
var sendMessageOrRemark=function (id,type,operate,fnName) {
	if(operate=='sendMsg'){
		$("#customerTitle").text("发送消息给审核人");
	}else{
		$("#customerTitle").text("新增备注信息");
	}
    $('#incomeType_customer').val(type);
    $('#operateType_customer').val(operate);
    $('#customer_remark').val('');
    if (type=='1') {
        $('#sysRequestId_customer').val(id);
    }
    if (type=='2') {
        $('#bankFlowId_customer').val(id);
    }
    var $div=$('#customer_modal');
    $div.modal('show');
    $div.on('hidden.bs.modal', function () {
		if(fnName){
			fnName();
		}
	});
}


var customerExecute=function () {
    if (!$('#customer_remark').val()) {
        $('#remarkPrompt_customer').show(10).delay(500).hide(10);
        return false;
    }
    var type = $('#incomeType_customer').val();//区分 是订单操作1 还是流水操作2
    var operate = $('#operateType_customer').val();//区分 是发消息sendMsg 还是添加备注remark
    var data = '';
    var url = '';
    if (type==1) {
        if (operate=='sendMsg') {
            //公司入款 发消息
            data = {'id':$('#sysRequestId_customer').val(),'accountId':$("#choseAccount").val(),'message':$('#customer_remark').val()};
            url = '/r/income/customerSendMsg';
        }
        if (operate=='remark') {
            //公司入款 添加备注
            data = {'id':$('#sysRequestId_customer').val(),'remark':$('#customer_remark').val()};
            url = '/r/income/customerAddRemark';
        }
    }
    if (type==2) {
        if (operate=='sendMsg') {
            //银行流水 发消息
            data = {'id':$('#bankFlowId_customer').val(),'accountId':$("#choseAccount").val(),'message':$('#customer_remark').val()};
            url = '/r/banklog/customerSendMsg';
        }
        if (operate=='remark') {
            //银行流水 添加备注
            data = {'id':$('#bankFlowId_customer').val(),'remark':$('#customer_remark').val()};
            url = '/r/banklog/customerAddRemark';
        }
    }
    $.ajax({
        type:'post',
        url:url,
        data:data,
        dataType:'json',
        success:function (res) {
            if (res) {
                $('#customer_modal').modal('hide');
                if (res.status==1) {
                    $.gritter.add({
                        time: 500,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                    if (type==1 && operate=='remark') {
                        //刷新公司入款 0
                    	loadincomeMatchingList();
                    }
                    if (type==2 && operate=='remark') {
                        //刷新银行流水 1
                    	loadBankLogMatchingList();
                    }
                }
            }
        }
    });
}



/**
 * 加载匹配中账号列表
 */
var loadIncomeAccountTab=function(CurPage){
	if($("#incomeTabType").val()!='Matching'){
		return;
	}
	var $filter = $("#Matching_accountFilter");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountList_page .Current_Page").text();
    var handicap = $filter.find("select[name='search_EQ_handicapId']").val();
    if (!handicap) {
        handicap = [];
        $filter.find("select[name='search_EQ_handicapId']").find('option:not(:first-child)').each(function () {
            handicap.push($(this).val());
        });
        handicap = handicap.toString();
    }
    var statusToArray = new Array();
    $filter.find("input[name='search_IN_status']:checked").each(function(){
        statusToArray.push(this.value);
    });
    if(statusToArray.length==0){
        statusToArray=[accountStatusNormal,accountStatusFreeze,accountStatusStopTemp];
    }
    var flagToArray = new Array();
    $filter.find("input[name='search_IN_flag']:checked").each(function(){
    	flagToArray.push(this.value);
    });
    if(flagToArray.length==0){
    	flagToArray=[accountFlagPC,accountFlagRefund];
    }
	$.ajax({
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:"/r/account/findIncomeAccountOrderByBankLog",
        data:{	
        	pageNo:CurPage<=0?0:CurPage-1,
        	account:$.trim($filter.find("[name='search_LIKE_account']").val()),
        	alias:$.trim($filter.find("[name='search_EQ_alias']").val()),
        	bankType:$.trim($filter.find("[name='search_LIKE_bankType']").val()),
        	handicapId:handicap,
        	owner:$.trim($filter.find("input[name='search_LIKE_owner']").val()),
            search_IN_flag:flagToArray.toString(),
        	statusToArray:statusToArray.toString()
        },
        success:function(jsonObject){
			if(jsonObject.status !=1){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
        	$ul=$("#accountList_ul"),liStr="",idList=new Array(),accountId=new Array();
        	$.each(jsonObject.data,function(index,result){
        		var record={
        				id:result[0],
        				account:result[1],
        				alias:result[2],
        				bankType:result[3],
        				owner:result[4],
        				status:result[5],
        				bankLogCounts:result[6],
        				handicapStr:result[7],
        		};
        		
        		//默认选中第一个
        		var active='';
        		if(index==0){
        			active=' class="active" ';
            		$("#choseAccount").val(record.id);
                    currentPageLiAccountId = $("#choseAccount").val();
        		}
        		//根据状态区分样式颜色等，默认正常
        		var styleStr='color: #00af0e;';
        		if(record.status==3){
        			styleStr='font-weight:bolder;color: red;';
        		}else if(record.status==4){
        			styleStr='font-weight:bolder;color: orange;';
        		}else{
                    accountIdArrayForTotal.push(record.id);//只定时查正常状态的账号流水，以显示未匹配的数量
                }
        		var accountName=record.alias?record.alias:hideAccountAll(record.account);
        		var bankLogCountsStr=record.bankLogCounts&&record.bankLogCounts*1!=0?'<span class="badge badge-important">'+record.bankLogCounts+'</span>':'';
        		liStr+='<li '+active+' id="accountListli'+record.id+'" onclick="changeTabAccountMatching('+record.id+');"  style="width:20%;z-index:0">\
        					<a data-toggle="tab" class="no-padding-left no-padding-right center" >'+bankLogCountsStr+'\
				        	<span class="bind_hover_card breakByWord" data-toggle="accountInfoHover'+record.id+'" data-placement="auto top" data-trigger="hover"  href="#selectedAccountsDiv">\
				        		<span style="display: none;" class="badge badge-important" id="spanToShowRecord'+record.id+'"></span>\
				        		<label id="accountColor123" style="'+styleStr+'">'+(record.handicapStr?record.handicapStr:'无')+'|'+accountName+'|'+(record.owner?hideName(record.owner):'无')+'|'+replaceBankStr(record.bankType)+'</label>\
				        	</span>\
				        	</a>\
				        </li>';
        		idList.push({'id':record.id});
        		accountId.push(record.id);
        	});
        	$ul.html(liStr);
			//加载流水抓取事件
		    monitorBankFlowAccount(accountId);
    		//加载第一个Tab账号的抓取信息
    		loadAccoutStatusHTML(accountId[0]);
			//分页初始化
			showPading(jsonObject.page,"accountList_page",loadIncomeAccountTab,'入款账号',true);
			$("#accountList_page").find(".pageRecordCount").hide();
			//加载账号悬浮提示 暂时注释 需要再打开
//			loadHover_accountInfoHover(idList);
			if(!jsonObject.data||jsonObject.data.length==0){
				//清空数据
				$("#choseAccount").val("");
			}
			//加载全部入款信息 和 银行流水信息
			loadincomeMatchingList(0);
			loadBankLogMatchingList(0);
        }
	});
}

/**
 * 匹配中入款数据
 */
var loadincomeMatchingList=function(CurPage){
	if($("#incomeTabType").val()!='Matching'){
		return;
	}
	var $div=$("#Matching_tab #incomeMatchingDiv");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#incomeMatchingDiv_tablePage .Current_Page").text();
	//封装data
	var $filter = $("#incomeMatchingDiv");
    var startAndEnd = $('#incomeMatchingDiv input[name="startAndEndTime"]').val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf(' - ') > 0) {
            startAndEnd = startAndEnd.split(' - ');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    } else {
        startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
        endTime = moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    }
	var data =  {
        'toId':$("#choseAccount").val(),
        'fromAccount': $.trim($filter.find("input[name='fromAccount']").val()),
        'memberUserName': $.trim($filter.find("input[name='memberUserName']").val()),
        'orderNo':$.trim($filter.find("input[name='orderNo']").val()),
        "minAmount": $.trim($filter.find("input[name='minAmount']").val()),
        "maxAmount": $.trim($filter.find("input[name='maxAmount']").val()),
        "startAndEndTimeToArray": [startTime,endTime].toString(),
        "statusArray":[incomeRequestStatusMatching].toString(),
        "type":incomeRequestTypePlatFromBank,
        "pageNo": CurPage<=0?0:CurPage-1,
        "pageSize": $.session.get('initPageSize')
    };
    $.ajax({
        type: 'POST',
        url: '/r/income/findbyvo',
        data:data,
        dataType: 'json',
        success: function (jsonObject) {
        	if(-1==jsonObject.status){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
    		var $tbody=$div.find("table tbody").html(""),trStr="";
        	if($("#choseAccount").val()){
    			var amountTotal=0;
    			$.each(jsonObject.data,function(index,record){
    				record.amount=record.amount?setAmountAccuracy(record.amount):0;
    				if(record.remark&&record.remark.indexOf("补提单")>-1){
    					trStr+="<tr style='color:white;background-color:sandybrown;' >";
    				}else{
    					trStr+="<tr>";
    				}
    				trStr+="<td><span>"+record.handicapName+"</span></td>";
    				record.levelName=record.levelName?record.levelName:"";
    				trStr+="<td><span>"+record.levelName+"</span></td>";
    				trStr+="<td><span title='会员账号 - 付款姓名'>"+(record.memberUserName?record.memberUserName:"无")+"&nbsp;-&nbsp;"+(record.memberRealName?record.memberRealName:"无")+"</span></td>";
    				trStr+="<td><span>"+record.amount+"</span></td>";
    				trStr+="<td><span>"+record.orderNo+"</span></td>";
    				trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
    				trStr+="<td><a style='width:90px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+record.remark+"'>"+
    							(record.remark?record.remark:'')+
    							"</a>" +
    						"</td>";
    				trStr+="<td>";
    				trStr+=	"<button id='SendMessageRightBTN2'  onclick='sendMessageOrRemark("+record.id+",1,\"sendMsg\",loadincomeMatchingList);'" +
    						" class='btn btn-xs btn-white btn-success contentRight' type='button'>" +
    						"<i class='ace-icon fa fa-envelope-o '>发消息</i></button>";
    				trStr+=	"<button id='SendMessageRightBTN2'  onclick='sendMessageOrRemark("+record.id+",1,\"remark\",loadincomeMatchingList);'" +
    						" class='btn btn-xs btn-white btn-info contentRight' type='button'>" +
    						"<i class='ace-icon fa fa-comments'>加备注</i></button>";
    				trStr+="</td>";
    				trStr+="</tr>";
    				amountTotal+=record.amount*1;
    			});
    			$tbody.html(trStr);
    			showPading(jsonObject.page,"incomeMatchingDiv_tablePage",loadincomeMatchingList,"入款记录");
    			if(jsonObject.data){
    				var totalRows={
    						column:12, 
    						subCount:jsonObject.data.length,
    						count:jsonObject.page.totalElements,
    						4:{subTotal:incomeCurrentPageSum?amountTotal:'noPermission',total:incomeAllRecordSum?jsonObject.page.header.totalAmount:'noPermission'}
    					};
    				showSubAndTotalStatistics4Table($tbody,totalRows);
    			}
        	}else{
        		$tbody.html(trStr);
        		showPading(null,"incomeMatchingDiv_tablePage",loadincomeMatchingList,"入款记录");
        	}
			
        }
    });
}


/**
 * 已取消数据
 */
var loadCancelList=function(CurPage){
	if($("#incomeTabType").val()!='Canceled'){
		return;
	}
	var $div=$("#Canceled_tab");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#Canceled_tablePage .Current_Page").text();
	//封装data
	var $filter = $("#Cancel_accountFilter ");
	var accountId = $filter.find("select[name='search_EQ_id']").val();
		var data =  {
			'handicap': $filter.find("select[name='search_EQ_handicapId']").val(),
	        'fromAccount': $.trim($filter.find("[name='fromAccount']").val()),
	        'memberUserName': $.trim($filter.find("input[name='memberUserName']").val()),
	        'orderNo':$.trim($filter.find("input[name='orderNo']").val()),
	        "minAmount": $.trim($filter.find("input[name='minAmount']").val()),
	        "maxAmount": $.trim($filter.find("input[name='maxAmount']").val()),
	        "startAndEndTimeToArray": getTimeArray($filter.find("[name=startAndEndTime]").val()).toString(),
	        "statusArray":[incomeRequestStatusCanceled].toString(),
	        "type":incomeRequestTypePlatFromBank,
	        "isCancel":1,
	        "pageNo": CurPage<=0?0:CurPage-1,
	        "pageSize": $.session.get('initPageSize')
	    };    
		var search_EQ_alias = $.trim($filter.find("[name='search_EQ_alias']").val());
	    if(search_EQ_alias){
	    	var account=getAccountInfoByAlias(search_EQ_alias,"account");
	    	if(!account){
	    		showMessageForFail("编号“"+search_EQ_alias+"”不存在");
	    		return;
	    	}
	    	data.toAccount=account;
	    }
	    $.ajax({
	        type: 'POST',
	        url: '/r/income/findbyvo',
	        data:data,
	        dataType: 'json',
	        success: function (jsonObject) {
	        	if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
					return;
				}
				var $tbody=$div.find("table tbody").html(""),trStr="",toAccountList=new Array();;
				var amountTotal=0;
				$.each(jsonObject.data,function(index,record){
					toAccountList.push({"id":record.toId});
					record.amount=record.amount?setAmountAccuracy(record.amount):0;
					trStr+="<tr>";
					trStr+="<td><span>"+record.handicapName+"</span></td>";
					record.levelName=record.levelName?record.levelName:"";
					trStr+="<td><span>"+record.levelName+"</span></td>";
					trStr+="<td><span title='会员账号 - 付款姓名'>"+(record.memberUserName?record.memberUserName:"无")+"&nbsp;-&nbsp;"+(record.memberRealName?record.memberRealName:"无")+"</span></td>";
					trStr+="<td>" +
								"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.toId+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(record.toAccount)+
								"</a>" +
							"</td>";
					trStr+="<td><span>"+record.amount+"</span></td>";
					trStr+="<td><span>"+record.orderNo+"</span></td>";
					trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
					trStr+="<td><a style='width:130px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+record.remark+"'>"+
									(record.remark?record.remark:'')+
								"</a>" +
							"</td>";
					trStr+="</tr>";
					amountTotal+=record.amount*1;
				});
				$tbody.html(trStr);
				loadHover_accountInfoHover(toAccountList);
				showPading(jsonObject.page,"Canceled_tablePage",loadCancelList);
				if(jsonObject.data){
					var totalRows={
							column:12, 
							subCount:jsonObject.data.length,
							count:jsonObject.page.totalElements,
							5:{subTotal:incomeCurrentPageSum?amountTotal:'noPermission',total:incomeAllRecordSum?jsonObject.page.header.totalAmount:'noPermission'}
						};
					showSubAndTotalStatistics4Table($tbody,totalRows);
				}
	        }
	    });
}

/**
 * 已匹配数据
 */
var loadMatchedList=function(CurPage){
	if($("#incomeTabType").val()!='Matched'){
		return;
	}
	var $div=$("#Matched_tab");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#Matched_tablePage .Current_Page").text();
	//封装data
	var $filter = $("#Matched_accountFilter");
	var handicapList=new Array();
	if($filter.find("select[name='search_EQ_handicapId']").val()){
		handicapList.push($filter.find("select[name='search_EQ_handicapId']").val());
	}else{
		$(handicap_list).each(function(index,record){
			handicapList.push(record.id);
		});
	}
	var data =  {
			'handicapList': handicapList.toString(),
	        'memberUserName': $.trim($filter.find("input[name='memberUserName']").val()),
	        'operatorUid': $.trim($filter.find("input[name='operatorUid']").val()),
	        'orderNo':$.trim($filter.find("input[name='orderNo']").val()),
	        "minAmount": $.trim($filter.find("input[name='minAmount']").val(),true),
	        "maxAmount": $.trim($filter.find("input[name='maxAmount']").val(),true),
	        "startAndEndTimeToArray": getTimeArray($filter.find("[name=startAndEndTime]").val(),'~').toString(),
	        "pageNo": CurPage<=0?0:CurPage-1,
	        "pageSize": $.session.get('initPageSize')
	    };
	if(!data.startAndEndTimeToArray){
		showMessageForFail("请先选择时间范围");
		return;
	}
    var search_EQ_alias = $.trim($filter.find("[name='search_EQ_alias']").val());
    if(search_EQ_alias){
    	var account=getAccountInfoByAlias(search_EQ_alias,"account");
    	if(!account){
    		showMessageForFail("编号“"+search_EQ_alias+"”不存在");
    		return;
    	}
    	data.toAccount=account;
    }
	if($filter.find("[name=operatorType]:checked").length==1){
		data.operatorType=$filter.find("[name=operatorType]:checked").val();
	}
    $.ajax({
        type: 'POST',
        url: '/r/income/findmatchedbyvo',
        data:data,
        dataType: 'json',
        success: function (jsonObject) {
        	if(-1==jsonObject.status){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var $tbody=$div.find("table tbody").html(""),trStr="",toAccountList=new Array();
			var amountTotal=0,differenceTotal=0;
			var page=jsonObject.data.page;
			var data=jsonObject.data.dataToPage;
			var header=jsonObject.data.header;
			if(data&&data.content){
				$.each(data.content,function(index,result){
					var record={
							"fromAccount":result[1],
							"fromId":result[2],
							"memberUserName":result[3],
							"memberRealName":result[4],
							"toAccount":result[5],
							"toId":result[6],
							"amount":result[7],
							"orderNo":result[8],
							"operatorUid":result[9],
							"updateTime":result[10],
							"createTime":result[11],
	                        "remark":result[12]
						};
					if(result[0]){
						var handicapInfo=getHandicapInfoById(result[0]);
						if(handicapInfo){
							record.handicapName=handicapInfo.name;
						}
					}
					record.amount=record.amount?setAmountAccuracy(record.amount):0;
					record.difference=record.difference?setAmountAccuracy(record.difference):0;
					toAccountList.push({"id":record.toId});
					trStr+="<tr>";
					trStr+="<td><span>"+_checkObj(record.handicapName)+"</span></td>";
					trStr+="<td><span title='会员账号 - 付款姓名'>"+(record.memberUserName?record.memberUserName:"无")+"&nbsp;-&nbsp;"+(record.memberRealName?record.memberRealName:"无")+"</span></td>";
					trStr+="<td>" +
								"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.toId+"' data-placement='auto left' data-trigger='hover'  >"+hideAccountAll(record.toAccount)+
								"</a>" +
							"</td>";
					trStr+="<td><span>"+record.amount+"</span></td>";
					trStr+="<td><span>"+record.orderNo+"</span></td>";
					trStr+="<td><span>"+(record.operatorUid?record.operatorUid:"机器")+"</span></td>";
					trStr+="<td>"+getHTMLremark(record.remark,90)+"</td>";
					trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
					trStr+="<td><a title='"+timeStamp2yyyyMMddHHmmss(record.updateTime)+" - "+timeStamp2yyyyMMddHHmmss(record.createTime)+"'>"+(formatDuring(new Date(record.updateTime).getTime()-new Date(record.createTime).getTime()))+"</a></td>";
					trStr+="</tr>";
					amountTotal+=record.amount*1;
					differenceTotal+=record.difference*1;
				});
			}
			$tbody.html(trStr);
			//加载账号悬浮提
			loadHover_accountInfoHover(toAccountList);
			showPading(page,"Matched_tablePage",loadMatchedList);
			var totalRows={
					column:12, 
					subCount:data.content?data.content.length:0,
					count:page.totalElements,
					5:{subTotal:incomeCurrentPageSum?amountTotal:'noPermission',total:incomeAllRecordSum?((header&&header[0])?header[0]:0):'noPermission'}
				};
			showSubAndTotalStatistics4Table($tbody,totalRows);
            $.each(ContentRight['IncomeAuditCompTotal:*'], function (name, value) {
                if (name == 'IncomeAuditCompTotal:lookUpMatchedTotalAmount:*') {
                    lockUpTotalMatchedAmount = true;
                }
            });
            if (!lockUpTotalMatchedAmount){
                $('#Matched_tab').find('tr[name="total"]').html("<td colspan='20' style='text-align: center'><h5>无权限查看总额</h5></td>");
            }
            $("[data-toggle='popover']").popover();
        }
    });
}

/**
 * 未认领银行流水
 */
var loadBankLogUnMatchList=function(CurPage){
	if($("#incomeTabType").val()!='BankLogUnMatch'){
		return;
	}
	var $div=$("#BankLogUnMatch_tab");
	var $filter = $("#BankLogUnMatch_accountFilter ");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#BankLogUnMatch_tablePage .Current_Page").text();
		var data =  {
			"toAccountOwner": $.trim($filter.find("input[name='toAccountOwner']").val(),true),
			"remark": $.trim($filter.find("input[name='remark']").val(),true),
	        "minAmount": $.trim($filter.find("input[name='minAmount']").val(),true),
	        "maxAmount": $.trim($filter.find("input[name='maxAmount']").val(),true),
			"startAndEndTimeToArray":getTimeArray($filter.find("[name=startAndEndTime]").val()).toString(),
			"pageNo" : CurPage<=0?0:CurPage-1,
			"pageSize":$.session.get('initPageSize'),
	        "searchTypeIn0Out1":0,//只查询入款流水记录
			"statusToArray":[bankLogStatusNoOwner].toString()//未认领
	    };
	    var search_EQ_alias = $.trim($filter.find("[name='search_EQ_alias']").val());
	    if(search_EQ_alias){
	    	var account=getAccountInfoByAlias(search_EQ_alias,"account");
	    	if(!account){
	    		showMessageForFail("编号“"+search_EQ_alias+"”不存在");
	    		return;
	    	}
	    	data.fromAccountNO=account;
	    }
	    $.ajax({
	        type: 'POST',
	        url: "/r/banklog/noOwner4Income",
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
					record.balance=record.balance?setAmountAccuracy(record.balance):0;
					toAccountList.push({'id':record.fromAccount});
					trStr+="<tr>";
					trStr+="<td>" +
								"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.fromAccount+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(record.fromAccountNO)+
								"</a>" +
							"</td>";
					trStr+="<td><span>"+hideAccountAll(record.toAccount)+"</span></td>";
					trStr+="<td><span>"+(record.toAccountOwner?record.toAccountOwner:'')+"</span></td>";
					trStr+="<td><span>"+record.amount+"</span></td>";
					trStr+="<td><span>"+(record.balance?record.balance:0)+"</span></td>";
					trStr+="<td><span>"+record.tradingTimeStr+"</span></td>";
					trStr+="<td><span>"+record.createTimeStr+"</span></td>";
					trStr+="<td>"+getHTMLremark(record.summary,90)+"</td>";
					trStr+="<td>"+getHTMLremark(record.remark,90)+"</td>";
					trStr+="<td>";
					trStr+=	"<button contentRight='IncomeAuditCompTotal:bankFlowFinalDeal:*' id='SendMessageRightBTN2'  onclick='sendMessageOrRemark("+record.id+",2,\"remark\",loadBankLogUnMatchList);'" +
								" class='btn btn-xs btn-white btn-info contentRight' type='button'>" +
								"<i class='ace-icon fa fa-comments'>加备注</i>" +
							"</button>";
                    trStr+=	"<button contentRight='IncomeAuditCompTotal:bankFlowFinalDeal:*' onclick='showModal_DisposedFee("+record.id+",loadBankLogUnMatchList);' " +
                        "class='btn btn-xs btn-warning btn-white contentRight' type='button'>" +
                        "<i class='ace-icon fa fa-check '>处理</i>" +
                        "</button>" ;
                    //需求 7495
                    trStr+="<button contentRight='IncomeAuditCompTotal:incomeRequestFinalDeal:*' id='addOrderRightBTN' onclick='addIncomeRequest("+record.id+");' " +
                        "class='btn btn-xs btn-purple btn-white contentRight' type='button'>" +
                        "<i class='ace-icon fa fa-pencil '>补提单</i></button>" ;
                    trStr+=	"</td>";
					trStr+="</tr>";
					amountTotal+=record.amount*1;
				});
				$tbody.html(trStr);
				//加载账号悬浮提示
				loadHover_accountInfoHover(toAccountList);
				showPading(jsonObject.page,"BankLogUnMatch_tablePage",loadBankLogUnMatchList);
				contentRight();
				if(jsonObject.data){
					var totalRows={
							column:12, 
							subCount:jsonObject.data.length,
							count:jsonObject.page.totalElements,
							4:{subTotal:incomeCurrentPageSum?amountTotal:'noPermission',total:incomeAllRecordSum?jsonObject.page.header.totalAmount:'noPermission'}
						};
					showSubAndTotalStatistics4Table($tbody,totalRows);
				}
	        }
	    });
}

/**
 * 快速搜索银行流水
 */
var loadFastSearchBankLogList=function(CurPage){
	var $div=$("#FastSearchBankLog_tab");
	var $filter = $("#FastSearchBankLog_accountFilter ");
	if($("#incomeTabType").val()!='FastSearchBankLog'){
		return;
	}
	if(!$.trim($filter.find("input[name='search_EQ_alias']").val(),true)&&!$.trim($filter.find("input[name='toAccount']").val(),true)
			&&!$.trim($filter.find("input[name='toAccountOwner']").val(),true)){
		showMessageForFail("请先输入至少一个查询条件（收款编号/存款账号/存款人）");
		return;
	}
	if(!!!CurPage&&CurPage!=0) CurPage=$("#FastSearchBankLog_tablePage .Current_Page").text();
	var data =  {
			"toAccount": $.trim($filter.find("input[name='toAccount']").val(),true),
			"toAccountOwner": $.trim($filter.find("input[name='toAccountOwner']").val(),true),
			"minAmount": $.trim($filter.find("input[name='minAmount']").val(),true),
			"maxAmount": $.trim($filter.find("input[name='maxAmount']").val(),true),
	        "startAndEndTimeToArray": getTimeArray($filter.find("[name=startAndEndTime]").val(),'~').toString(),
			"pageNo" : CurPage<=0?0:CurPage-1,
			"pageSize":$.session.get('initPageSize'),
			"searchTypeIn0Out1":0,//只查询入款流水记录
			"statusToArray":[$filter.find("[name='search_IN_status']:checked").val()].toString()
	};
	if(!data.startAndEndTimeToArray){
		showMessageForFail("请先选择时间范围");
		return;
	}
    var search_EQ_alias = $.trim($filter.find("[name='search_EQ_alias']").val());
    if(search_EQ_alias){
    	var accountInfo=getAccountInfoByAlias(search_EQ_alias);
    	if(!accountInfo){
    		showMessageForFail("编号“"+search_EQ_alias+"”不存在");
    		return;
    	}
    	data.fromAccount=accountInfo.id;
    }
	$.ajax({
		type: 'POST',
		url: API.r_banklog_findbyfrom,
		data:data,
		dataType: 'json',
		success: function (jsonObject) {
			if(-1==jsonObject.status){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var $tbody=$div.find("table tbody").html(""),trStr="",toAccountList=new Array(),idList=new Array();
			var amountTotal=0;
			var sysDate=new Date();
			$.each(jsonObject.data,function(index,record){
				record.amount=record.amount?setAmountAccuracy(record.amount):0;
				record.balance=record.balance?setAmountAccuracy(record.balance):0;
				toAccountList.push({'id':record.fromAccount});
				trStr+="<tr>";
				trStr+="<td>" +
				"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.fromAccount+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(record.fromAccountNO)+
				"</a>" +
				"</td>";
				trStr+="<td><span>"+hideAccountAll(record.toAccount)+"</span></td>";
				trStr+="<td><span>"+(record.toAccountOwner?record.toAccountOwner:'')+"</span></td>";
				trStr+="<td><span>"+record.amount+"</span></td>";
				trStr+="<td><span>"+(record.balance?record.balance:0)+"</span></td>";
				trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.tradingTime)+"</span></td>";
				trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
				trStr+="<td>"+getHTMLremark(record.summary,110)+"</td>";
				trStr+="<td>"+getHTMLremark(record.remark,110)+"</td>";
				if(record.status==bankLogStatusMatched){
					idList.push(record.id);
				}
				trStr+="<td>"+getHTMLBankLogStatus(record,sysDate)+"</td>";
				trStr+="</tr>";
				amountTotal+=record.amount*1;
			});
			$tbody.html(trStr);
			//加载已匹配对应的系统记录
			loadHover_InOutInfoHover(idList);
			//加载账号悬浮提示
			loadHover_accountInfoHover(toAccountList);
			showPading(jsonObject.page,"FastSearchBankLog_tablePage",loadFastSearchBankLogList);
			if(jsonObject.data){
				var totalRows={
						column:12, 
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements,
						4:{subTotal:incomeCurrentPageSum?amountTotal:'noPermission',total:incomeAllRecordSum?jsonObject.page.header.totalAmount:'noPermission'}
				};
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
		}
	});
}

/**
 * 匹配中银行流水
 */
var loadBankLogMatchingList=function(CurPage){
	if($("#incomeTabType").val()!='Matching'){
		return;
	}
	var $div=$("#incomeMatching_BankLog");
	var $filter = $("#incomeMatchingDiv");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#BankLogMatching_tablePage .Current_Page").text();
    var startAndEnd = $('#incomeMatchingDiv input[name="startAndEndTime"]').val();
	var data =  {
		"fromAccount":$("#choseAccount").val(),
		"toAccount":$.trim($filter.find("input[name='toAccount']").val(),true),
        "toAccountOwner": $.trim($filter.find("input[name='memberUserName']").val(),true),
        "minAmount": $.trim($filter.find("input[name='minAmount']").val(),true),
        "maxAmount": $.trim($filter.find("input[name='maxAmount']").val(),true),
		"pageNo" : CurPage<=0?0:CurPage-1,
		"pageSize":$.session.get('initPageSize'),
        "searchTypeIn0Out1":0,//只查询入款流水记录
		"statusToArray":[bankLogStatusMatching].toString()//未认领
    };
    $.ajax({
        type: 'POST',
        url: API.r_banklog_findbyfrom,
        data:data,
        dataType: 'json',
        success: function (jsonObject) {
        	if(-1==jsonObject.status){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var $tbody=$div.find("table tbody").html(""),trStr="";
			if($("#choseAccount").val()){

				var amountTotal=0;
				$.each(jsonObject.data,function(index,record){
					record.amount=record.amount?setAmountAccuracy(record.amount):0;
					record.balance=record.balance?setAmountAccuracy(record.balance):0;
					trStr+="<tr>";
					trStr+="<td><span>"+hideAccountAll(record.toAccount)+"</span></td>";
					trStr+="<td><span>"+(record.toAccountOwner?record.toAccountOwner:'')+"</span></td>";
					trStr+="<td><span>"+record.amount+"</span></td>";
					trStr+="<td><span>"+(record.balance?record.balance:0)+"</span></td>";
					trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.tradingTime)+"</span></td>";
					trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
					trStr+="<td>"+getHTMLremark(record.summary,90)+"</td>";
					trStr+="<td>"+getHTMLremark(record.remark,90)+"</td>";
					trStr+="<td>";
					trStr+=	"<button id='SendMessageRightBTN2'  onclick='sendMessageOrRemark("+record.id+",2,\"sendMsg\",loadBankLogMatchingList);'" +
							" class='btn btn-xs btn-white btn-success' type='button'>" +
							"<i class='ace-icon fa fa-envelope-o '>发消息</i></button>";
					trStr+=	"<button id='SendMessageRightBTN2'  onclick='sendMessageOrRemark("+record.id+",2,\"remark\",loadBankLogMatchingList);'" +
							" class='btn btn-xs btn-white btn-info' type='button'>" +
							"<i class='ace-icon fa fa-comments'>加备注</i></button>";
					trStr+="<button contentRight='IncomeAuditCompTotal:incomeRequestFinalDeal:*' id='addOrderRightBTN' onclick='addIncomeRequest("+record.id+");' " +
							"class='btn btn-xs btn-purple btn-white contentRight' type='button'>" +
	                		"<i class='ace-icon fa fa-pencil '>补提单</i></button>" ;
					trStr+="<button contentRight='IncomeAuditCompTotal:bankFlowFinalDeal:*' onclick='showModal_DisposedFee("+record.id+",loadBankLogMatchingList);' " +
							"class='btn btn-xs btn-warning btn-white contentRight' type='button'>" +
							"<i class='ace-icon fa fa-check '>处理</i></button>" ;
					trStr+="</td>";
					trStr+="</tr>";
					amountTotal+=record.amount*1;
				});
				$tbody.html(trStr);
				showPading(jsonObject.page,"BankLogMatching_tablePage",loadBankLogMatchingList,"银行流水");
				contentRight();
				if(jsonObject.data){
					var totalRows={
							column:12, 
							subCount:jsonObject.data.length,
							count:jsonObject.page.totalElements,
							3:{subTotal:incomeCurrentPageSum?amountTotal:'noPermission',total:incomeAllRecordSum?jsonObject.page.header.totalAmount:'noPermission'}
						};
					showSubAndTotalStatistics4Table($tbody,totalRows);
				}
			}else{
				//请空数据
				$tbody.html(trStr);
				showPading(null,"BankLogMatching_tablePage",loadBankLogMatchingList,"银行流水");
			}
        }
    });
}
/**
 * 快速搜索
 */
var loadFastSearchList=function(CurPage){
	if($("#incomeTabType").val()!='FastSearch'){
		return;
	}
	var $div=$("#FastSearch_tab");
	var $filter = $("#FastSearch_accountFilter");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#FastSearch_tablePage .Current_Page").text();
	var memberRealName=$.trim($filter.find("input[name='memberRealName']").val(),true);
	var memberUserName=$.trim($filter.find("input[name='memberUserName']").val(),true);
	var orderNo=$.trim($filter.find("input[name='orderNo']").val(),true);
	//查询条件都为空则不执行查询
	if(!memberUserName&&!memberRealName&&!orderNo){
		showMessageForFail("请输入查询条件");
		return;
	}
	//封装data
		var data =  {
	        'memberUserName': memberUserName,
	        'memberRealName': memberRealName,
	        "startAndEndTimeToArray": getTimeArray($filter.find("[name=startAndEndTime]").val()).toString(),
	        'orderNo':orderNo,
	        'search_IN_status':[incomeRequestStatusMatching,incomeRequestStatusMatched,incomeRequestStatusUnmatching,incomeRequestStatusCanceled].toString(),//不查拆单后的订单
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
					trStr+="<td><span title='会员账号 - 付款姓名'>"+(record.memberUserName?record.memberUserName:"无")+"&nbsp;-&nbsp;"+(record.memberRealName?record.memberRealName:"无")+"</span></td>";
					trStr+="<td>" +
							"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.toId+"' data-placement='auto left' data-trigger='hover'  >"+hideAccountAll(record.toAccount)+
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
						trStr+="<td>"+getHTMLremark(record.remark,50)+"</td>";
						trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
					}else{
						//已匹配 或者已取消 等 其它状态
						trStr+="<td><span>"+(record.operatorUid?record.operatorUid:"机器")+"</span></td>"
						trStr+="<td><span>"+(record.confirmUid?record.confirmUid:"-")+"</span></td>";
						if(incomeRequestStatusMatched==record.status||MATCHED4SUBINBANKALI==record.status){
							trStr+="<td><span class='label label-success'>"+record.statusName+"</span></td>";
						}else{
							trStr+="<td><span class='label label-inverse'>"+record.statusName+"</span></td>";
						}
						trStr+="<td>"+getHTMLremark(record.remark,50)+"</td>";
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
							5:{subTotal:incomeCurrentPageSum?amountTotal:'noPermission',total:incomeAllRecordSum?jsonObject.page.header.totalAmount:'noPermission'}
						};
					showSubAndTotalStatistics4Table($tbody,totalRows);
				}
                $("[data-toggle='popover']").popover();
	        }
	    });
}


/**
 * 补提单 弹出框
 */
var addIncomeRequest=function(bankLogId) {
	var bankLog=getBankLogById(bankLogId);
	if(!!!bankLog) return;
    $('#bankLogIdToReBuild').val(bankLogId);
	var $div=$("#makeUpFlow");
	$div.find("#makeUpFlow_bankLogId").val(bankLogId);
	$div.find("#makeUpMemberAccount,#makeUpRemark,#makeUpDepositType").val("");
	$div.find("[name=makeUpDepositType]").prop('checked',false);
	$div.find('#makeUpName').val(bankLog.toAccountOwner).prop('readonly','readonly');
	$div.find('#makeUpAmount').val(bankLog.amount).prop('readonly','readonly');
	var accountInfo=getAccountInfoById(bankLog.fromAccount);
	$div.find('#makeUpAccount').val(accountInfo.account);
	$div.find('#hideAccount').val(hideAccountAll(accountInfo.account,"account")).prop('readonly','readonly');
    $('#makeUpPrompt').text('');
	$div.modal('show');
}
/**
 * 补提单
 * @returns
 */
var doAddIncomeRequest=function() {
    var bankLogId = $('#bankLogIdToReBuild').val();
    if(!bankLogId){
    	return;
	}
    if (generateIncomeOrder && generateIncomeOrder == bankLogId) {
    	$('#makeUpPrompt').text("20秒内不能重复补单！").show(10).delay(800).hide(10);
        return false;
    } else {
        var $div=$("#makeUpFlow");
        var memberAccount = $div.find('#makeUpMemberAccount').val();
        var makeUpName = $div.find('#makeUpName').val();
        var makeUpAmount = $div.find('#makeUpAmount').val();
        var makeUpAccount = $div.find('#makeUpAccount').val();
        var makeUpRemark = $div.find('#makeUpRemark').val();
        var makeUpDepositType = $div.find('#makeUpDepositType').val();
        $('input[name="makeUpDepositType"]').each(function () {
            if($(this).prop('checked')) {
                makeUpDepositType = $(this).val();
            }
        });
        var pfTypeSub =  '';
        $('input[name="pfTypeSub"]').each(function () {
            if($(this).prop('checked')) {
                pfTypeSub = $(this).val();
            }
        });
        if(!memberAccount) {
            $('#makeUpPrompt').text("请填写会员账号！").show(10).delay(300).hide(10);
            return false;
        }
        if(!makeUpAmount) {
            $('#makeUpPrompt').text("请填写存款金额！").show(10).delay(300).hide(10);
            return false;
        }
        if(!makeUpAccount) {
            $('#makeUpPrompt').text("请填写公司收款账号！").show(10).delay(300).hide(10);
            return false;
        }
        if(!makeUpDepositType) {
            $('#makeUpPrompt').text("请选择存款类型！").show(10).delay(300).hide(10);
            return false;
        }
        if (!pfTypeSub) {
            $('#makeUpPrompt').text("请选择提单类型！").show(10).delay(1000).hide(10);
            return false;
        }
        var bankLogId = $('#bankLogIdToReBuild').val();
        var data = {
            "memberAccount":memberAccount,"name":makeUpName,"amount":makeUpAmount,"accountNo":makeUpAccount,"accountId":currentPageLiAccountId,
            "remark":makeUpRemark,"type":makeUpDepositType,"localHostIp":localHostIp,"bankLogId":bankLogId,"pfTypeSub":pfTypeSub
        };
        generateIncomeOrder = bankLogId;

        $.ajax({
            type: 'post',
            url: '/r/income/generateIncomeRequestOrder',
            data: data,async:false,
            dataType: 'json',
            success: function (jsonObject) {
            	console.log("补提单结果:"+jsonObject.message);
                if (jsonObject.status != 1) {
                    showMessageForFail(jsonObject.message);
                }else{
                    $div.modal('hide');
                    //不论搜索范围，必须强制全部刷新（银行流水可能已被匹配走）
                    //客服补提单  socket取消了 所以这里提示
                    $.gritter.add({
                        time: '',
                        class_name: '',
                        title: '公司入款汇总客服补提单消息',
                        text:jsonObject.message,
                        sticky: true,
                        image: '../images/message.png'
                    });
                    loadincomeMatchingList();
                    loadBankLogMatchingList();
                    $('#bankLogIdToReBuild').val('');
                    setTimeout(function () {
                        generateIncomeOrder = null;
                    }, 20 * 1000);
                }
            }
        });
    }
}
// var total_connect_incomeCompany=function() {
// 	return;
//     if (window.location.protocol == 'http:') {
//         ws_incomeCompany = new WebSocket('ws://' + window.location.host + '/ws/income?Audit=Comp&timeStamp=' + new Date());
//     } else {
//         ws_incomeCompany = new WebSocket('wss://' + window.location.host + '/ws/income?Audit=Comp');
//     }
//     ws_incomeCompany.onmessage = function (event) {
//         var jsonObj = JSON.parse(event.data);
//         if (jsonObj) {
//             if (new Number(jsonObj.incomeAuditWsFrom)==7 &&jsonObj.owner==getCookie('JUID')) {
//                 //客服补提单
//             	  $.gritter.add({
//                       time: '',
//                       class_name: '',
//                       title: '客服补提单消息',
//                       text: JSON.parse(jsonObj.message).Desc=='OK'?'补提单成功！':JSON.parse(jsonObj.message).Desc,
//                       sticky: true,
//                       image: '../images/message.png'
//                   });
//                   return ;
//               }
//           }
//       }
//   }
//var total_disconnectForIncompany=function() {
//      if (ws_incomeCompany != null) {
//          ws_incomeCompany.close();
//          console.log("关闭socket");
//      }
//  }
//  window.onbeforeunload = function () {
//	  total_disconnectForIncompany();
//  }
/**
 * 页面账号流水抓取状态加载
 */
var monitorBankFlowAccount=function(oldAccountIdArray) {
  if (oldAccountIdArray) {
  		accoutStatusHTMLMap={};
        SysEvent.on(SysEvent.EVENT_OFFLINE,function(data){
            if (data){
                var detail = JSON.parse(data);
                var accountId = detail.id;
                var status = '';
                var classStr = "btn btn-success btn-sm pull-right";
                switch (detail.runningStatus) {
                    case monitorAccountStatusUnstart:
                        status='未开始';
                        classStr = "btn btn-grey btn-sm pull-right";
                        break;
                    case monitorAccountStatusAcquisition:
                        status='抓取中';
                        classStr = "btn btn-success btn-sm pull-right";
                        break;
                    case monitorAccountStatusPause:
                        status='暂停';
                        classStr = "btn btn-info btn-sm pull-right";
                        break;
                    case monitorAccountStatusWarn :
                        status='失败';
                        classStr = "btn btn-warning btn-sm pull-right";
                        break;
                    default:
                        status = '';
                        break;
                }
                var msg = '';
                if (status) {
                    msg += "状态:"+status ;
                }
                if (detail.lastTime) {
                    if (status)
                        msg += "|最后抓取时间："+timeStamp2yyyyMMddHHmmss(detail.lastTime);
                    else
                        msg += "最后抓取时间："+timeStamp2yyyyMMddHHmmss(detail.lastTime);
                }
                if (detail.ip){
                    if (status || detail.lastTime)
                        msg += "|主机:"+detail.ip;
                    else {
                        msg += "主机:"+detail.ip;
                    }
                }
                if (msg){
                    for (var i in oldAccountIdArray){
                        if (oldAccountIdArray[i]==accountId) {
                        	accoutStatusHTMLMap[accountId]='<span class="'+classStr+'" >工具'+msg+'</span>';
                        }
                    }
                }
            }

        },oldAccountIdArray);
    }
}      	

//先隐藏已匹配
contentRight();
var $clickBtnArray=[$("#Matching_tab #incomeMatchingDiv #searchBtn"),$("#Matched_tab #searchBtn"),$("#BankLogUnMatch_tab #searchBtn"),$("#Canceled_tab #searchBtn"),$("#FastSearch_tab #searchBtn")];
initRefreshSelect($("#refreshAccountListSelect"),$clickBtnArray,null,"refresh_approveForCompanyInTotal");
//正在匹配
initTimePicker(true,$("#Matching_tab [name=startAndEndTime]"),7);
//未认领
initTimePicker(false,$("#BankLogUnMatch_tab [name=startAndEndTime]"));
//已取消
initTimePicker(true,$("#Canceled_tab [name=startAndEndTime]"),7);
//快捷查订单
initTimePicker(true,$("#FastSearch_tab [name=startAndEndTime]"),7);
//快捷查流水 时间范围 3天
_datePickerForAll($("#FastSearchBankLog_tab [name=startAndEndTime]"));
//已匹配 时间范围 3天
_datePickerForAll($("#Matched_tab [name=startAndEndTime]"));

//正在匹配：
var $matching = $("#Matching_tab");
getHandicap_select($matching.find("select[name='search_EQ_handicapId']"),null,"全部");
getBankTyp_select($matching.find("select[name='search_LIKE_bankType']"),null,"全部")
//已匹配：
var $matched = $("#Matched_tab");
getHandicap_select($matched.find("select[name='search_EQ_handicapId']"),null,"全部");
//已取消:
var $canceled = $("#Canceled_tab");
getHandicap_select($canceled.find("select[name='search_EQ_handicapId']"),null,"全部");

$("#Matched_accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		$(e).find("#searchBtn button").click();
	}
});
loadIncomeAccountTab(0);
//total_connect_incomeCompany();
//正在匹配 盘口，层级 收款账号change 绑定查询账号事件
$("#Matching_tab").find("[name=search_EQ_handicapId],[name=search_LIKE_bankType],[name=search_IN_flag]").change(function() {
	loadIncomeAccountTab();
});
//正在匹配 状态 click 绑定查询账号事件
$("#Matching_tab [name=search_IN_status]").click(function() {
	loadIncomeAccountTab();
});
//正在匹配 范围 click 绑定查询订单与流水
$("#Matching_tab [name=search_range]").click(function() {
	choiceLoadIncomeBanklog();
});

//已匹配 盘口，层级 收款账号change
$("#Matched_tab").find("[name=search_EQ_handicapId],[name=search_EQ_LevelId],[name=search_EQ_id]").change(function() {
	loadMatchedList();
});
//已匹配  人工 机器 复选框 click
$("#Matched_tab [name=operatorType]").click(function() {
	loadMatchedList();
});

//已取消 盘口，层级 收款账号change
$("#Canceled_tab [name=search_EQ_handicapId],[name=search_EQ_LevelId],[name=search_EQ_id]").change(function() {
	loadCancelList();
});

//快捷查流水 状态 click
$("#FastSearchBankLog_tab [name=search_IN_status]").click(function() {
	loadFastSearchBankLogList();
});