currentPageLocation = window.location.href;
/**
 * 拿到页面URL请求的参数信息
 */
var getRequest=function() { 
	var url = window.location.href; // 获取url中"?"符后的字串
	var theRequest = new Object(); 
	if (url.indexOf("?") != -1) { 
		var str = url.substr(url.indexOf("?")+1); 
		strs = str.split("&");
		for(var i = 0; i < strs.length; i ++) { 
			theRequest[strs[i].split("=")[0]]=unescape(strs[i].split("=")[1]); 
		} 
	} 
	return theRequest; 
}

/**
 * 匹配中的数据，如果超过指定时间，则状态字符串改为未认领
 * 
 * @param bankLogList
 *            流水list hoursSetting 设置时间，小时数 timeName 日期在list中的名字 例：var reList =
 *            bankLogListReplace(bankLogList,24,'payTime');
 * @returns
 */
function bankLogListReplace(bankLogList,hoursSetting,timeName){
	// 获得当前日期时间（'yyyy-MM-dd HH:mm:ss'）
	  var date = new Date();
	    var seperator1 = "-";
	    var seperator2 = ":";
	    var month = date.getMonth() + 1;
	    var strDate = date.getDate();
	    if (month >= 1 && month <= 9) {
	        month = "0" + month;
	    }
	    if (strDate >= 0 && strDate <= 9) {
	        strDate = "0" + strDate;
	    }
	    var endDate = date.getFullYear() + seperator1 + month + seperator1 + strDate
	            + " " + date.getHours() + seperator2 + date.getMinutes()
	            + seperator2 + date.getSeconds();
	    
		  var endTime = new Date(Date.parse(endDate.replace(/-/g,   "/"))).getTime();
		  var startTime;
		  var dates;
	$.each(bankLogList,function(index,record){
		// 当匹配中状态的数据超过24小时则改为未认领状态
		if(record.status==bankLogStatusMatching){
			  startTime = new Date(Date.parse(record[timeName].replace(/-/g,   "/"))).getTime(); 
			   dates = Math.abs((startTime - endTime))/(1000*60*60); 
			  if(dates>hoursSetting){
				  record.status=bankLogStatusNoOwner;
				  record.statusStr='未认领';
			  }
		}
	});
	
	return bankLogList;
}


/**
 * 根据传入的值返回时间数组
 */
var getTimeArray=function(startAndEndTime,splitBy){
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(splitBy?splitBy:" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	return startAndEndTimeToArray;
}

/**
 * page:查询回来的对象，如：jsonObject.page id:装载分页按钮的div，放在table下如：bankPage（<div
 * id="bankPage"></div>） funName：点击后查询的事件 footTitle:table名 如：平台提单 可选
 * showTotal:左下角的总条数，默认不显示 showImgTips：未匹配/已匹配/已驳回 图列提示
 * TurnPageParameter:点击后查询事件参数
 */
var showPading=function(page,id,funName,footTitle,showTotal,showImgTips,TurnPageParameter){
	// 清空之前的modal
	var $divBox=$("#"+id);
	$divBox.html("");
	// 无数据时做提示
	if(!page||!page.totalElements){
		$divBox.append($("#page_common_html").find(".noDataTipsPage").clone());
        initPaging($("#"+id),page);
	}
	$divBox.attr("class","table table-striped table-bordered table-hover no-margin-bottom no-border-top");
	$divBox.append($("#page_common_html").find(".common_Page").clone());
	// 表单名
	if(footTitle){
		$divBox.find(".pull-left [name=footTitle]").text(footTitle);
	}
	// 总条数
	if(showTotal){
		$divBox.find(".pull-left .showTotal").show();
	}else{
		$divBox.find(".pull-left .showTotal").hide();
	}
	// 图列提示
	if(showImgTips){
		$divBox.find(".pull-left .showImgTips").show();
	}else{
		$divBox.find(".pull-left .showImgTips").hide();
	}
	// 初始化各page值
	initPaging($divBox,page);
	// 绑定事件 >> 翻页
	$divBox.find("li.paging_first,li.paging_previous,li.paging_next,li.paging_last").bind("click", function(){
		$divBox.find(".Current_Page").text($(this).attr("goTo"));
		$divBox.find(".CurrentPageInput").val($(this).attr("goTo"));
		// 执行方法
		if(TurnPageParameter){
			if(funName) {funName(TurnPageParameter);}
		}else{
			if(funName) {funName();}
		}
	});
	// 绑定事件 >> 修改页数
	$divBox.find(".pageRecordCount").bind("change", function(){
		// pageNo改为0
		$divBox.find(".Current_Page").text(0);
		$divBox.find(".CurrentPageInput").val(0);
		$.session.set('initPageSize',$(this).val()?$(this).val()*1:10);
		// 执行方法
		if(TurnPageParameter){
			if(funName) {funName(TurnPageParameter);}
		}else{
			if(funName) {funName();}
		}
	});
}


/**
 * 分页初始化（需要更新的div模块，page对象）
 */
var initPaging=function($divPage,page){
	// 部分场景下，无数据时page会undefined时的处理
	if(!!!page){
		page=pageInitial;
	}
	// 每页条数
	if($.session.get('initPageSize')){
		$divPage.find(".pageRecordCount").val($.session.get('initPageSize'));
	}
    // 总条数
    $divPage.find(".TotalCount").text(page.totalElements);
    // 当前页面
    $divPage.find(".Current_Page").text(page.pageNo==0?1:page.pageNo);
    $divPage.find(".CurrentPageInput").val(page.pageNo==0?1:page.pageNo);
    // 总页数
    $divPage.find(".TotalPage").text(page.totalPages==0?1:page.totalPages);
    // 上一个
    if(page.hasPrevious){
        $divPage.find("li.paging_previous").removeClass("disabled");
        $divPage.find("li.paging_first").removeClass("disabled");
        $divPage.find("li.paging_previous").attr("goTo",page.pageNo-1);
        $divPage.find("li.paging_first").attr("goTo",0);
    }else{
        $divPage.find("li.paging_previous").addClass("disabled");
        $divPage.find("li.paging_first").addClass("disabled");
    }
    // 下一个
    if(page.hasNext){
        $divPage.find("li.paging_next").removeClass("disabled");
        $divPage.find("li.paging_last").removeClass("disabled");
        $divPage.find("li.paging_next").attr("goTo",page.pageNo+1);
        $divPage.find("li.paging_last").attr("goTo",page.totalPages);
    }else{
        $divPage.find("li.paging_next").addClass("disabled");
        $divPage.find("li.paging_last").addClass("disabled");
    }
}

/**
 * 根据id获取对应的入款数据
 */
var getIncomeInfoById=function(id){
	var result;
	$.ajax({async:false,type:"POST",url:"/r/income/findbyid",data:{"id":id},dataType:'json',success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("获取入款信息异常，"+jsonObject.message);
		}else{
			result=jsonObject.data;
		}
			
	}});
	return result;
}

/**
 * 根据id获取对应的银行流水数据
 */
var getBankLogById=function(id){
	var result;
	$.ajax({
		type:"POST",
		async:false,
		url:"/r/banklog/findbyid",
		data:{"id":id},
		dataType:'json',
		success:function(jsonObject){
			// 查询失败；返回值为undefined或空字符串时返回
			if(jsonObject.status ==1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取银行流水信息异常，"+jsonObject.message);
			}
			
		}
	});
	return result;
}
/**
 * 读取用户配置
 */
var findByUserIdAndPropertyKey=function(userId,propertyKey){
	var result;
	$.ajax({
		type:"POST",
		async:false,
        url: '/r/user/findByUserIdAndPropertyKey',
		data:{
        	userId:userId,
        	propertyKey:propertyKey
        },
		dataType:'json',
		success:function(jsonObject){
			// 查询失败；返回值为undefined或空字符串时返回
			if(jsonObject.status ==1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取用户配置信息异常，"+jsonObject.message);
			}
			
		}
	});
	return result;
}

var getOutwardTaskInfoById = function(id){
	var result= null;
	$.ajax({type:"POST",async:false,url:API.r_outtask_findInfoById,data:{"id":id},dataType:'json',success:function(jsonObject){
		if(jsonObject.status ==1){
			result=  jsonObject.data;
		}else{
			showMessage("获取出款任务信息失败，"+jsonObject.message);
		}
	}});
	return result;
}

var getRebateById = function(id){
	var result= null;
	$.ajax({type:"POST",async:false,url:'/r/rebate/findById',data:{"id":id},dataType:'json',success:function(jsonObject){
		if(jsonObject.status ==1){
			result=  jsonObject.data;
		}else{
			showMessage("获取提现信息失败，"+jsonObject.message);
		}
	}});
	return result;
}

// 弹出窗口，message:显示信息
var showMessage=function(message){
	bootbox.dialog({
		message: "<span class='bigger-110'>"+message+"</span>",
		buttons:{
			"click" :
			{
				"label" : "确认",
				"className" : "btn-sm btn-primary"
			}
		}
	});
}



// 根据id查询账号信息
var getAccountInfoById=function(accountId,searchType){
	var result='';
	$.ajax({
		type:"POST",
		url:"/r/account/findById",
		data:{"id":accountId},
		dataType:'json',
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				var data=jsonObject.data;
				if(searchType&&data){
					if(searchType=="account"){
						// 卡号
						result=data.account;
					}else if(searchType=="owner"){
						// 持卡人
						result=data.owner;
					}
				}else{
					// 不指定查询类型，则默认查全部
					result=data;
				}
			}else{
				showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
			}
		}
	});
	return result;
}

//根据别名查询账号信息
var getAccountInfoByAlias=function(alias,searchType){
	var result='';
	$.ajax({
		type:"POST",
		url:"/r/account/findByAlias",
		data:{"alias":alias},
		dataType:'json',
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				if(jsonObject.data&&jsonObject.data.length>0){
					var data=jsonObject.data[0];
					if(searchType&&data){
						if(searchType=="account"){
							// 卡号
							result=data.account;
						}
					}else{
						// 不指定查询类型，则默认查全部
						result=data;
					}
				}
			}else{
				showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
			}
		}
	});
	return result;
}

var getAccountInfoByAcc=function(acc,searchType){
	var result='',url;
	if(searchType == 'wechatInfoHover'){
		url = '/r/account/find4Wechat'
	}else if(searchType == 'alipayInfoHover'){
		url = '/r/account/find4Alipay'
	}else{
		url='/r/account/find4Bank';
	}
	$.ajax({type:"POST", url:url,dataType:'json',async:false, data:{"account":acc},
		success:function(jsonObject){
			if(jsonObject.status == 1){
				 result=jsonObject.data;
			}else{
				// showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
			}
		}
	});
	return result;
}

var findMobileBalFromCloud = function (mobile) {
	var result='';
	$.ajax({type:"POST",url:"/r/mobile/cloud/getBal",data:{"mobile":mobile}, dataType:'json',async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取云端信息异常");
			}
		}
	});
	return result;
}

var findTransInfoByToId = function (toId) {
	var result='';
	$.ajax({type:"POST",url:"/r/accountMonitor/buildTransTo",data:{"toId":toId}, dataType:'json',async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=[jsonObject.data,jsonObject.message];
			}else{
				showMessageForFail("获取下发信息异常");
			}
		}
	});
	return result;
};

var findTransAckMonitorRiskInfoByToId = function(id){
	var result='';
	$.ajax({type:"POST",url:"/r/accountMonitor/buildTransAckRiskList",data:{"id":id}, dataType:'json',async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取信息异常");
			}
		}
	});
	return result;
};

var findMobileFromCloud = function(mobile){
	var result='';
	$.ajax({type:"POST", url:"/r/mobile/cloud/get", data:{"mobile":mobile}, dataType:'json', async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取云端手机设备信息异常");
			}
		}
	});
	return result;
}


// 根据id查询手机信息
var getMobileInfoById=function(mobileId){
	var result;
	$.ajax({
		type:"POST",
		url:"/r/mobile/list",
		data:{
			"pageNo":0,
			"search_EQ_id":mobileId
		},
		dataType:'json',
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data;;
			}else{
				showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
			}
		}
	});
	
	return result?result[0]:null;
}

var getMobileInfoByMobile = function(mobile){
	var result;
	$.ajax({type:"POST",url:"/r/mobile/get",data:{mobile:mobile},dataType:'json',async:false,success:function(jsonObject){
		if(jsonObject.status == 1){
			result=jsonObject.data;;
		}else{
			showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
		}
	}});
	return result;
}

// 根据银行卡ID查询手机信息
var getMobileInfoByBankCardId=function(bankCardId){
	var result;
	$.ajax({
		type:"POST",
		url:"/r/mobile/list",
		data:{
			"pageNo":0,
			"search_EQ_bankCardId":bankCardId
		},
		dataType:'json',
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data;;
			}else{
				showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
			}
		}
	});
	
	return result?result[0]:null;
}
// 根据银行/第三方流水id查询响应匹配的入款/出款信息
var findInOutByLogId=function(logId){
	var result;
	$.ajax({
		type:"POST",
		url:"/r/match/findInOutByLogId",
		data:{"logId":logId},
		dataType:'JSON',
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data?jsonObject.data:undefined;
			}else{
				showMessageForFail("获取匹配信息异常，"+jsonObject.message);
			}
		}
	});
	return result;
}

// 根据id查询完整账号信息
var getAccountMoreInfoById=function(accountId,searchType){
	var result;
	$.ajax({
		type:"POST",
		url:"/r/account/findmorebyid",
		data:{"id":accountId},
		dataType:'json',
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				var data=jsonObject.data;
				result=data;
			}else{
				showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
			}
		}
	});
	return result;
}

/**
 * 获取账号表当前最大的编号
 */
var getMaxAlias=function(){
	var result;
	$.ajax({
		type:"POST",
		url:"/r/account/getMaxAlias",
		data:{},
		dataType:'json',
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取账号表当前最大的编号，"+jsonObject.message);
			}
		}
	});
	return result;
}

	
// 防止按钮重复点击 3秒
var clickRepeat=function($button){
	$button.attr("disabled","disabled");
	setTimeout(function(){
		$button.removeAttr("disabled");
	},1000*3);
}

var initAccountInfo_bank_wechat_alipay = function ($div,data1,data2,data3) {
    if(data1){
        var result = data1;
        // type 银行卡 bank_balance_ //微信 wechat_balance_ //支付宝 alipay_balance_
        $div.find("span[name=bank_balance_incomeAuditor]").text(result.incomeAuditor?result.incomeAuditor:'无');
        $div.find("span[name=bank_balance_handicapName]").text(result.handicapName?result.handicapName:'无');
        $div.find("span[name=bank_balance_levelNameToGroup]").text(result.levelNameToGroup?result.levelNameToGroup:'无');
        $div.find("span[name=bank_balance_alias]").text(result.alias?result.alias:'无');
        $div.find("span[name=bank_balance_currSysLevelName]").text(result.currSysLevelName?result.currSysLevelName:'无');
        $div.find("span[name=bank_balance_account]").text(hideAccountAll(result.account));
        result.bankName=result.bankName?result.bankName:'无';
        result.bankType=result.bankType?result.bankType:'无';
        $div.find("span[name=bank_balance_bankName]").text(result.bankType+"-"+result.bankName);
        $div.find("span[name=bank_balance_owner]").text(result.owner?result.owner:'无');
        $div.find("span[name=bank_balance_status]").text(result.statusStr);
        $div.find("span[name=bank_balance_type]").text(result.typeStr);
        $div.find("span[name=bank_balance_bankBalance]").text(result.bankBalance?result.bankBalance:0);
        $div.find("span[name=bank_balance_limitIn]").text(result.limitIn?result.limitIn:'无');
        $div.find("span[name=bank_balance_limitOut]").text(result.limitOut?result.limitOut:'无');
        $div.find("span[name=bank_balance_limitOutOne]").text(result.limitOutOne?result.limitOutOne:'无');
        $div.find("span[name=bank_balance_creator]").text(result.creatorStr?result.creatorStr:'无');
        $div.find("span[name=bank_balance_modifier]").text(result.modifierStr?result.modifierStr:'无');
        $div.find("span[name=bank_balance_holderStr]").text(result.holderStr?result.holderStr:'无');
        $div.find("span[name=bank_balance_lockerStr]").text(result.lockerStr?result.lockerStr:'无');
        $div.find("span[name=bank_balance_createTime]").text(result.createTimeStr?result.createTimeStr:'无');
        $div.find("span[name=bank_balance_updateTime]").text(result.updateTimeStr?result.updateTimeStr:'无');
        $div.find("span[name=bank_balance_limitBalance]").text(result.limitBalance?result.limitBalance:0);
        $div.find("span[name=bank_balance_peakBalance]").text(result.peakBalance?result.peakBalance:0);
        $div.find("span[name=bank_balance_lowestOut]").text(result.lowestOut?result.lowestOut:0);
        $div.find("span[name=bank_balance_remark]").html(result.remark?result.remark.replace(new RegExp("\r\n","g"),"<br/>"):'无');
    }
    if(data2){
        var result = data2;
        // type 银行卡 bank_balance_ //微信 wechat_balance_ //支付宝 alipay_balance_
        $div.find("span[name=wechat_balance_incomeAuditor]").text(result.incomeAuditor?result.incomeAuditor:'无');
        $div.find("span[name=wechat_balance_handicapName]").text(result.handicapName?result.handicapName:'无');
        $div.find("span[name=wechat_balance_levelNameToGroup]").text(result.levelNameToGroup?result.levelNameToGroup:'无');
        $div.find("span[name=wechat_balance_alias]").text(result.alias?result.alias:'无');
        $div.find("span[name=wechat_balance_currSysLevelName]").text(result.currSysLevelName?result.currSysLevelName:'无');
        $div.find("span[name=wechat_balance_account]").text(hideAccountAll(result.account));
        result.bankName=result.bankName?result.bankName:'无';
        result.bankType=result.bankType?result.bankType:'无';
        $div.find("span[name=wechat_balance_bankName]").text(result.bankType+"-"+result.bankName);
        $div.find("span[name=wechat_balance_owner]").text(result.owner?result.owner:'无');
        $div.find("span[name=wechat_balance_status]").text(result.statusStr);
        $div.find("span[name=wechat_balance_type]").text(result.typeStr);
        $div.find("span[name=wechat_balance_bankBalance]").text(result.bankBalance?result.bankBalance:0);
        $div.find("span[name=wechat_balance_limitIn]").text(result.limitIn?result.limitIn:'无');
        $div.find("span[name=wechat_balance_limitOut]").text(result.limitOut?result.limitOut:'无');
        $div.find("span[name=wechat_balance_limitOutOne]").text(result.limitOutOne?result.limitOutOne:'无');
        $div.find("span[name=wechat_balance_creator]").text(result.creatorStr?result.creatorStr:'无');
        $div.find("span[name=wechat_balance_modifier]").text(result.modifierStr?result.modifierStr:'无');
        $div.find("span[name=wechat_balance_holderStr]").text(result.holderStr?result.holderStr:'无');
        $div.find("span[name=wechat_balance_lockerStr]").text(result.lockerStr?result.lockerStr:'无');
        $div.find("span[name=wechat_balance_createTime]").text(result.createTimeStr?result.createTimeStr:'无');
        $div.find("span[name=wechat_balance_updateTime]").text(result.updateTimeStr?result.updateTimeStr:'无');
        $div.find("span[name=wechat_balance_limitBalance]").text(result.limitBalance?result.limitBalance:0);
        $div.find("span[name=wechat_balance_peakBalance]").text(result.peakBalance?result.peakBalance:0);
        $div.find("span[name=wechat_balance_lowestOut]").text(result.lowestOut?result.lowestOut:0);
        $div.find("span[name=wechat_balance_remark]").html(result.remark?result.remark.replace(new RegExp("\r\n","g"),"<br/>"):'无');
    }
    if(data3){
        var result = data3;
        // type 银行卡 bank_balance_ //微信 wechat_balance_ //支付宝 alipay_balance_
        $div.find("span[name=alipay_balance_incomeAuditor]").text(result.incomeAuditor?result.incomeAuditor:'无');
        $div.find("span[name=alipay_balance_handicapName]").text(result.handicapName?result.handicapName:'无');
        $div.find("span[name=alipay_balance_levelNameToGroup]").text(result.levelNameToGroup?result.levelNameToGroup:'无');
        $div.find("span[name=alipay_balance_alias]").text(result.alias?result.alias:'无');
        $div.find("span[name=alipay_balance_currSysLevelName]").text(result.currSysLevelName?result.currSysLevelName:'无');
        $div.find("span[name=alipay_balance_account]").text(hideAccountAll(result.account));
        result.bankName=result.bankName?result.bankName:'无';
        result.bankType=result.bankType?result.bankType:'无';
        $div.find("span[name=alipay_balance_bankName]").text(result.bankType+"-"+result.bankName);
        $div.find("span[name=alipay_balance_owner]").text(result.owner?result.owner:'无');
        $div.find("span[name=alipay_balance_status]").text(result.statusStr);
        $div.find("span[name=alipay_balance_type]").text(result.typeStr);
        $div.find("span[name=alipay_balance_bankBalance]").text(result.bankBalance?result.bankBalance:0);
        $div.find("span[name=alipay_balance_limitIn]").text(result.limitIn?result.limitIn:'无');
        $div.find("span[name=alipay_balance_limitOut]").text(result.limitOut?result.limitOut:'无');
        $div.find("span[name=alipay_balance_limitOutOne]").text(result.limitOutOne?result.limitOutOne:'无');
        $div.find("span[name=alipay_balance_creator]").text(result.creatorStr?result.creatorStr:'无');
        $div.find("span[name=alipay_balance_modifier]").text(result.modifierStr?result.modifierStr:'无');
        $div.find("span[name=alipay_balance_holderStr]").text(result.holderStr?result.holderStr:'无');
        $div.find("span[name=alipay_balance_lockerStr]").text(result.lockerStr?result.lockerStr:'无');
        $div.find("span[name=alipay_balance_createTime]").text(result.createTimeStr?result.createTimeStr:'无');
        $div.find("span[name=alipay_balance_updateTime]").text(result.updateTimeStr?result.updateTimeStr:'无');
        $div.find("span[name=alipay_balance_limitBalance]").text(result.limitBalance?result.limitBalance:0);
        $div.find("span[name=alipay_balance_peakBalance]").text(result.peakBalance?result.peakBalance:0);
        $div.find("span[name=alipay_balance_lowestOut]").text(result.lowestOut?result.lowestOut:0);
        $div.find("span[name=alipay_balance_remark]").html(result.remark?result.remark.replace(new RegExp("\r\n","g"),"<br/>"):'无');
    }
}
var initAccountInfo_Others=function($div,result,type,mobile){
	// 设备信息
	var deviceInfo;
	if(type =='alipayInfoHover'){
		$div.find(".wechatAliShow").show();
		if(result.alipayEntity){
			if(result.device1Status==1){
				deviceInfo=result.dev1Sstatus;
			}
			result.acc = result.alipayEntity;
			result.acc.bankBalance = result.alipayBalance;
			result.acc.owner = result.alipayEntity.nickname;
		}
	}else if(type =='wechatInfoHover'){
		$div.find(".wechatAliShow").show();
		if(result.wechatEntity){
			if(result.device1Status==1){
				deviceInfo=result.dev1Sstatus;
			}
			result.acc = result.wechatEntity;
			result.acc.bankBalance = result.wechatBalance;
			result.acc.owner = result.wechatEntity.nickname;
			result.acc.owner = result.wechatEntity.nickname;
		}
	}else{
		$div.find(".bankShow").show();
		$div.find(".ownerTitle strong").text("开户行");
		if(result.bankEntity){
			if(result.device2Status==1){
				deviceInfo=result.dev2Sstatus;
			}
			result.acc = result.bankEntity;
			result.acc.bankBalance = result.bankBalance;
			$div.find("span[name=bankType]").text(result.acc.bankType?result.acc.bankType:'无');
			$div.find("span[name=bankName]").text(result.acc.bankName?result.acc.bankName:'无');
		}
	}
	// 在线，且有设备内容
	if(deviceInfo){
		$div.find(".deviceInfo").show();
		$div.find("span[name=device]").text(deviceInfo.device?deviceInfo.device:'');
		$div.find("span[name=battery]").text(deviceInfo.battery?deviceInfo.battery:'');
		$div.find("span[name=signal]").text(deviceInfo.signal?deviceInfo.signal:'');
		$div.find("span[name=deviceId]").text(deviceInfo.deviceId?deviceInfo.deviceId:'');
		$div.find("span[name=gps]").text(deviceInfo.gps?deviceInfo.gps:'');
		$div.find("span[name=device_updateTime]").text(timeStamp2yyyyMMddHHmmss(deviceInfo.updateTime));
	}
	var HandicapBatch=getHandicapBatchInfoByCode();
	$div.find("span[name=handicapName]").text(HandicapBatch&&HandicapBatch[result.handicap]?HandicapBatch[result.handicap].name:'');
	$div.find("span[name=levelNameToGroup]").text(result.level?(result.level==currentSystemLevelOutter?"外层":(result.level==currentSystemLevelInner?"内层":"指定层")):"");
	$div.find("span[name=owner]").text(result.acc.owner?hideName(record.owner):'无');
	$div.find("span[name=account]").text(hideAccountAll(result.acc.account));
	$div.find("span[name=bankBalance]").text(result.acc.bankBalance?result.acc.bankBalance:'');
	result.acc.statusStr = result.acc.status==mobileStatusFreeze?'冻结':result.acc.statusStr;
	result.acc.statusStr = result.acc.status==mobileStatusStopTemp?'暂停':result.acc.statusStr;
	result.acc.statusStr = result.acc.status==mobileStatusNormal?'在用':result.acc.statusStr;
	$div.find("span[name=status]").text(result.acc.statusStr);
	$div.find("span[name=type]").text(result.type == MobileTypeCustomer?'客户':'自用');
	$div.find("span[name=limitInDaily]").text(result.acc.limitInDaily?result.acc.limitInDaily:'无');
	$div.find("span[name=limitBalance]").text(result.acc.limitBalance?result.acc.limitBalance:'无');
	var outTypeStr="全部";
	if(result.acc.outType){
		if(result.acc.outType==1){
			outTypeStr="二维码收款";
		}else if(result.acc.outType==2){
			outTypeStr="银行卡";
		}
	}
	$div.find("span[name=outType]").text(outTypeStr);
	$div.find("span[name=createTime]").text(timeStamp2yyyyMMddHHmmss(result.acc.createTime));
	$div.find("span[name=updateTime]").text(timeStamp2yyyyMMddHHmmss(result.acc.updateTime));
	$div.find("span[name=remark]").html(result.acc.remark?result.acc.remark.replace(new RegExp("\r\n","g"),"<br/>"):'无');
	$div.find("span[name=mobile]").html(result.mobile);
	$div.find("span[name=mobile_owner]").html(result.owner);
	
}

var initAccountInfo_bank=function($div,result,type){
	if(type=="third"){
		$div.find(".ownerTitle").text("经办人");
		$div.find(".bankNameTitle").text("第三方");
		//第三方账号查询费率规则并拼接
		$div.find("#third_div").show();
		var feeSpan="";
		if(result.accountFeeConfig.feeType==1){
			feeSpan+="从到账金额扣取&nbsp;&nbsp;&nbsp;&nbsp;";
		}else{
			feeSpan+="从商户余额扣取&nbsp;&nbsp;&nbsp;&nbsp;";
		}
		if(result.accountFeeConfig.calFeeType==1){//按阶梯式计算
			$.each(result.accountFeeConfig.calFeeLevelMoneyList, function (key,val) { 
				feeSpan+="<br/>"+val.moneyBegin.toFixed(2);
				if(val.moneyEnd=="Infinity"){
					feeSpan+="元以上";
				}else{
					feeSpan+=" 元至  "+val.moneyEnd.toFixed(2)+"元";
				}
				feeSpan+="&nbsp;&nbsp;收费"+val.feeMoney.toFixed(2)+"元";
			 });
		}else{//按百分比计算
			feeSpan+=("统一收取费率："+(result.accountFeeConfig.calFeePercent*100).toFixed(2)+"&nbsp;%")
		}
		$div.find("[name=third_fee_rule]").html(feeSpan);
	}
    if(result.flag==2||(result.type==accountTypeOutBank&&result.flag==accountFlagMobile)){
		//返利网账号 或 出款卡手机类型
    	$div.find("[name='label_peakBalance']").text("保证金");
	}
	$div.find("span[name=incomeAuditor]").text(result.incomeAuditor?result.incomeAuditor:'无');
	$div.find("span[name=handicapName]").text(result.handicapName?result.handicapName:'无');
	$div.find("span[name=levelNameToGroup]").text(result.levelNameToGroup?result.levelNameToGroup:'无');
	$div.find("span[name=alias]").text(result.alias?result.alias:'无');
	$div.find("span[name=currSysLevelName]").text(result.currSysLevelName?result.currSysLevelName:'无');
	$div.find("span[name=account]").text(hideAccountAll(result.account));
	result.bankName=result.bankName?result.bankName:'无';
	result.bankType=result.bankType?result.bankType:'无';
	$div.find("span[name=bankName]").text(result.bankType+"-"+result.bankName);
	$div.find("span[name=owner]").text(result.owner?hideName(result.owner):'无');
	$div.find("span[name=status]").text(result.statusStr);
	if(result.type==accountTypeInBank){
		$div.find("span[name=minInAmount]").text(result.minInAmount?result.minInAmount:'无');
		$div.find("span[name=outEnable]").text(getFlagMoreStrHTML(result.flagMoreStr));
		result.typeStr="银行入款卡";
		if(result.subType){
			if(result.subType==1){
				result.typeStr="支付宝入款卡";
			}else if(result.subType==2){
				result.typeStr="微信入款卡";
			}else if(result.subType==3){
				result.typeStr="云闪付入款卡";
			}
		}
	}
	$div.find("span[name=type]").text(result.typeStr);
	$div.find("span[name=bankBalance]").text(result.bankBalance?result.bankBalance:0);
	$div.find("span[name=limitIn]").text(result.limitIn?result.limitIn:'无');
	$div.find("span[name=limitOut]").text(result.limitOut?result.limitOut:'无');
	$div.find("span[name=limitOutOne]").text(result.limitOutOne?result.limitOutOne:'无');
    $div.find("span[name=limitOutCount]").text(result.limitOutCount?result.limitOutCount:"无");
    $div.find("span[name=limitOutOneLow]").text(result.limitOutOneLow?result.limitOutOneLow:"无");
	$div.find("span[name=creator]").text(result.creatorStr?result.creatorStr:'无');
	$div.find("span[name=modifier]").text(result.modifierStr?result.modifierStr:'无');
	 if(result.flag&&result.flag*1==2){
		 result.holderStr="返利网";
	 }
	$div.find("span[name=holderStr]").text(result.holderStr?result.holderStr:'无');
	$div.find("span[name=lockerStr]").text(result.lockerStr?result.lockerStr:'无');
	$div.find("span[name=createTime]").text(result.createTimeStr?result.createTimeStr:'无');
	$div.find("span[name=updateTime]").text(result.updateTimeStr?result.updateTimeStr:'无');
	$div.find("span[name=limitBalance]").text(result.limitBalance?result.limitBalance:0);
	if(result.flag&&result.flag*1==2&&null!=result.deposit){
		$div.find("span[name=peakBalance]").html((result.peakBalance?result.peakBalance:0)+"<span style='color:red'>("+result.deposit+"押金)</span>");
	}else{
		$div.find("span[name=peakBalance]").text(result.peakBalance?result.peakBalance:0);
	}
	$div.find("span[name=lowestOut]").text(result.lowestOut?result.lowestOut:0);
	$div.find("span[name=remark]").html(result.remark?result.remark.replace(new RegExp("\r\n","g"),"<br/>"):'无');
	$div.find("span[name=flag]").html((result.flag&&result.flag*1==1)?("手机&nbsp;"+hideAccountAll(result.mobile,true)):((result.flag&&result.flag*1==2)?"返利网":"PC"));
	if(result.gps){
		var Host=getHostByIp(result.gps);
		$div.find("span[name=gps]").html("虚拟机"+result.gps+"&nbsp;"+(Host?'<br/>物理机'+Host.ip:""));
	}else{
		$div.find("span[name=gps]").html('无');
	}
}

//str flagMoreStr值，addStyle 是否加上样式, limitPercentage minBalance加上样式时传入
var getFlagMoreStrHTML=function(str,addStyle,limitPercentage,minBalance){
	if(!str) return "";
	var result="";
	if(addStyle){
		if(str==1){
			result="<a title='可一边入款一边出款' >边入边出<br/><span class='red bolder' style='font-size:10px;'>(返利网)</a>";
		}else if(str==2){
			result="大额专用<br/><span class='red bolder' style='font-size:10px;'>(自购卡)</span>";
		}else if(str==3){
			var title="大额专用(返利网)，当余额大于或等于信用额度的“"+_checkObj(limitPercentage)+"%”时，留“"+_checkObj(minBalance)+"元”，其余一笔转出到出款卡";
			if(!limitPercentage){
				title="未设置详细参数";
			}
			result="<a title='"+title+"'>大额专用<br/><span class='red bolder' style='font-size:10px;'>(返利网)</span></a>";
		}else if(str==4){
			var title="先入后出卡(返利网)，当余额大于或等于信用额度的“"+_checkObj(limitPercentage)+"%”时，转为出款卡，当余额小于“"+_checkObj(minBalance)+"元”，再转为入款卡";
			result="<a title='"+title+"'>先入后出<br/><span class='red bolder' style='font-size:10px;'>(返利网/正在出)</span></a>";
			if(!limitPercentage){
				title="未设置详细参数";
			}
		}else if(str==5){
			var title="先入后出卡(返利网)，当余额大于或等于信用额度的“"+_checkObj(limitPercentage)+"%”时，转为出款卡，当余额小于“"+_checkObj(minBalance)+"元”，再转为入款卡";
			if(!limitPercentage){
				title="未设置详细参数";
			}
			result="<a title='"+title+"'>先入后出<br/><span class='red bolder' style='font-size:10px;'>(返利网/正在入)</span></a>";
		}
	}else{
		if(str==1){
			result="边入边出(返利网)";
		}else if(str==2){
			result="大额专用(自购卡)";
		}else if(str==3){
			result="大额专用(返利网)";
		}else if(str==4){
			result="先入后出(返利网/正在出)";
		}else if(str==5){
			result="先入后出(返利网/正在入)";
		}
	}
	return result;
}


/*
 * data: { column:12, subCount:1,count:3, 8:{subTotal:123.00,total:123.00},
 * 9:{subTotal:500.00,total:600.00} }
 */
var showSubAndTotalStatistics4Table=function($bodyOfTable,data){
	if(data.subCount==0||data.count==0)return;
	var obj=new Object(),sub=new Array(),tot=new Array(),column = data.column,spanindex=0,colspan=0,init0=0,init1=0;
	var noPermission_subTotal=false,noPermission_total=false;
	for(var index=1;index<=column;index++){
		var item = data[index];
		if(item){
			init0++;
			if(item.subTotal!='noPermission'){
				obj['sub'+index]='<td bgcolor="#579EC8" style="color:white;font-size:10px;"><span>'+setAmountAccuracy(item.subTotal)+'</span></td>';
			}else{
				noPermission_subTotal=true;//出现一次则说明无权限
			}
			if(item.total!='noPermission'){
				obj['tot'+index]='<td class="allRecordSum" bgcolor="#D6487E" style="color:white;font-size:10px;"><span>'+setAmountAccuracy(item.total)+'</span></td>';
			}else{
				noPermission_total=true;//出现一次则说明无权限
			}
		}else{
			colspan++;
			spanindex=spanindex==0?index:spanindex;
		}
		if(item||(!!!item&&index==column)){
			init1++;
			if(noPermission_subTotal){
				//无权限时
				if(init1==1){
					obj['sub'+spanindex]='<td colspan="12"><span>'+('小计：'+data.subCount+' 条')+'</span></td>';
				}
			}else{
				obj['sub'+spanindex]='<td colspan="'+colspan+'"><span>'+(init1==1?('小计：'+data.subCount+' 条'):('&nbsp;&nbsp;'))+'</span></td>';
			}
			if(noPermission_total){
				//无权限时
				if(init1==1){
					obj['tot'+spanindex]='<td colspan="12"><span>'+('总计：'+data.count+' 条')+'</span></td>';
				}
			}else{
				obj['tot'+spanindex]='<td colspan="'+colspan+'"><span>'+(init1==1?('总计：'+data.count+' 条'):('&nbsp;&nbsp;'))+'</span></td>';
			}
			spanindex=colspan=0;
		}
	}
	for(var index=1;index<=column;index++){
		sub.push(obj['sub'+index]);
		tot.push(obj['tot'+index]);
	}
	$('<tr name="subtotal" >'+sub.join('')+'</tr><tr name="total" >'+tot.join('')+'</tr>').appendTo($bodyOfTable);
}

// 重置
var reset=function(div){
	if(!div) return;
	$("#"+div).find("input[type=text],input[type=number],[type=file],[type=string]").val("");
	$("#"+div).find("input:checkbox").prop("checked","checked");
	$("#"+div).find("input.defaultNoCheck:checkbox").prop("checked",false);
	$("#"+div).find("input.defaultCheck:radio").prop("checked","checked").click();
//	$("#"+div).find("select").val("");
	$("#"+div).find("select").find("option:first").prop("selected", 'selected');
	$("#"+div).find("[multiple=multiple]").multiselect('refresh');
	$("#"+div).find('select').trigger('chosen:updated');
}

/**
 * 展示修改模态窗 修改入款卡账号信息
 */
var showModal_updateIncomeAccount=function(accountId,fnName){
	var accountInfo=getAccountInfoById(accountId);
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#updateIncomeAccount").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			$div.find("#tableAdd td.noPaddingTop").css("padding-top","0px");
			// 表单填充值
			$div.find("#accountId").val(accountId);
			if(accountInfo){
			    $div.find("input[name='id']").val(accountInfo.id);
			    $div.find("input[name='account']").val(hideAccountAll(accountInfo.account));
			    $div.find("input[name='limitIn']").val(accountInfo.limitIn);
			    $div.find("input[name='limitOut']").val(accountInfo.limitOut);
			    $div.find("input[name='limitOutOne']").val(accountInfo.limitOutOne);
			    $div.find("input[name='limitBalance']").val(accountInfo.limitBalance);
			    // 银行卡才有编号
				if(accountInfo.type==accountTypeInBank){
                    loadProvinceCity_select($div.find("select[name='province']"),$div.find("select[name='city']"),accountInfo.province,accountInfo.city);
				    $div.find("input[name='bankName']").val(accountInfo.bankName);
				    $div.find("input[name='alias']").val(accountInfo.alias);
				    $div.find("input[name='owner']").val(accountInfo.owner);
				    $div.find("input[name='peakBalance']").val(accountInfo.peakBalance?accountInfo.peakBalance:"0");
				    $div.find("input[name='minInAmount']").val(accountInfo.minInAmount?accountInfo.minInAmount:"");
				    $div.find("input[name='limitOutCount']").val(accountInfo.limitOutCount?accountInfo.limitOutCount:"0");
				    $div.find("input[name='limitOutOneLow']").val(accountInfo.limitOutOneLow?accountInfo.limitOutOneLow:"");
				    $div.find("select[name='currSysLevel'] option").each(function () {
                        if(accountInfo.currSysLevel && this.value===accountInfo.currSysLevel.toString()){
                            $(this).attr('selected','selected');
                        }
                    });
				    // 加载银行品牌
				    getBankTyp_select($div.find("select[name=choiceBankBrand]"),accountInfo.bankType);
				}else{
					$div.find(".needHide").hide();
					if(accountInfo.type==accountTypeInThird||accountInfo.type==accountTypeOutThird){
						if(accountInfo.status==4){
							$("#showTbody").show();
							$("#showList").show();
							$("#freezeTr").show();
						}else{
							$("#showTbody").hide();
							$("#showList").hide();
							$("#freezeTr").hide();
						}
						
						$div.find(".thirdShow").show();
						$div.find(".header").show();
						getThirdType_select($div.find("select[name=choiceThirdBrand]"),accountInfo.bankType,"请选择类别");
					    $div.find("input[name='thirdBankName']").val(accountInfo.bankName);
						$div.find("input[name='thirdBalance']").val(accountInfo.balance);
						$(":radio[name='rateType'][value='" + accountInfo.rateType + "']").prop("checked", "checked");
						if(accountInfo.rateType==1){
							$("#updateIncomeAccount").find(".ladder").show();
							var result = JSON.parse(accountInfo.rateValue);
							$div.find("input[name='startAmount']").val(result[0].startAmount);
							$div.find("input[name='endAmount']").val(result[0].endAmount);
							$div.find("input[name='rates']").val(result[0].rates);
							for (var i = 1; i < result.length; i++) {
								addResultRow(result[i].startAmount,result[i].endAmount,result[i].rates);
						    }
						}else{
							$div.find(".fixed").show();
							if(accountInfo.rate!=null && accountInfo.rate!=0)
								$div.find("input[name='rate']").val(accountInfo.rate);
							else
								$div.find("input[name='rate']").val(3);
						}
						
						if(accountInfo.accountFeeConfig.feeType==0){
							$(":radio[name='mtc'][value=0]").prop("checked", "checked");
						}else if(accountInfo.accountFeeConfig.feeType==1){
							$(":radio[name='mtc'][value=1]").prop("checked", "checked");
						}
						if(accountInfo.accountFeeConfig.calFeeType==0&&accountInfo.status==4){
							$(":radio[name='computingMethod'][value=0]").prop("checked", "checked");
							changeComputingMethod(0);
							$div.find("input[name='downRate']").val((accountInfo.accountFeeConfig.calFeePercent*100).toFixed(2));
						}else if(accountInfo.accountFeeConfig.calFeeType==1&&accountInfo.status==4){
							$(":radio[name='computingMethod'][value=1]").prop("checked", "checked");
							changeComputingMethod(1);
							if(accountInfo.accountFeeConfig.calFeeLevelType==1){
								var trr="";
								$.each(accountInfo.accountFeeConfig.calFeeLevelMoneyList, function (key,val) { 
									 trr+="<tr>"
										+"<td  colspan='2' style='height: 10px;width: 470px;'>"
										+val.moneyBegin.toFixed(2);
										if(val.moneyEnd=="Infinity"){
											trr+="元以上<td/>";
										}else{
											trr+=" 元至  "+val.moneyEnd.toFixed(2)+"元<td/>";
										}
										trr+="<td style='width: 250px;'>&nbsp;&nbsp;收费"+val.feeMoney.toFixed(2)+"元<td/>"
										+"<td>&nbsp;&nbsp;<a onclick='deleteRate("+accountInfo.handicapId+","+accountId+",1,"+val.index+")' type='button'  class='btn btn-xs btn-white btn-primary btn-bold'>删除</td>"
									+"</tr>";
								 })
								  $("#showList").append($(trr));
							}else{
								var trr="";
								$.each(accountInfo.accountFeeConfig.calFeeLevelPercentList, function (key,val) { 
									 trr+="<tr id='showList'>"
										+"<td colspan='2' style='height: 10px;width: 470px;'>"
										+val.moneyBegin;
										if(val.moneyEnd=="Infinity"){
											trr+="元以上<td/>";
										}else{
											trr+=" 元至  "+val.moneyEnd+"元<td/>";
										}
										trr+="<td style='width: 250px;'>&nbsp;&nbsp;收费"+(val.feePercent*1*100)+"%<td/>"
										+"<td>&nbsp;&nbsp;<a onclick='deleteRate("+accountInfo.handicapId+","+accountId+",0,"+val.index+")' type='button'  class='btn btn-xs btn-white btn-primary btn-bold'>删除</td>"
									+"</tr>";
								 })
								 $("#showList").append($(trr));
							}
						}
						if(accountInfo.accountFeeConfig.calFeeLevelType==1){
							$(":radio[name='charging'][value=1]").prop("checked", "checked");
							changeCharging(0)
						}else if(accountInfo.accountFeeConfig.calFeeLevelType==0){
							$(":radio[name='charging'][value=0]").prop("checked", "checked");
							changeCharging(1);
						}
						var charging=$div.find("input[name='charging']:checked").val();
						if(charging==1){
							$("#showMessage").html("元");
						}else{
							$("#showMessage").html("%");
						}
						
					}
				}
				// 初始化盘口
				getHandicap_select($div.find("select[name='handicap_select']"),accountInfo.handicapId);
			}else{
				// 提示数据不存在
				showMessageForFail("操作失败：账号不存在");
			}
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				// 关闭窗口清除model
				$div.remove();
			});
			$div.find("#do_update").bind("click",function(){
				do_updateIncomeAccount(accountInfo.type,accountInfo.flag,fnName,accountInfo.status);
			});
		}
	});
}

var do_updateIncomeAccount=function(type,oldFlag,fnName,status){
	var $div=$("#updateIncomeAccount");
    var accountId = $div.find("#accountId").val();
    if(!accountId) return;
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $limitOutOne = $div.find("input[name='limitOutOne']");
    var $bankName = $div.find("input[name='bankName']");
    var $owner = $div.find("input[name='owner']");
	var $currSysLevel = $div.find("select[name='currSysLevel']");
    var $limitBalance = $div.find("input[name='limitBalance']");
    var $peakBalance = $div.find("input[name='peakBalance']");
    var $minInAmount;
    var $bankType;
    var $thirdBalance;
    var $limitOutCount;
    var $limitOutOneLow;
    var rateType;
    var rate;
    var mtc;
    var computingMethod;
    var downRate;
    var charging;
    let $province=$div.find("select[name='province']");
    let $city=$div.find("select[name='city']");
    if(type==accountTypeInThird||type==accountTypeOutThird){
    	$bankType= $div.find("select[name=choiceThirdBrand]");
    	$thirdBankName= $div.find("[name=thirdBankName]");
    	$thirdBalance = $div.find("input[name='thirdBalance']");
    	rateType=$("input[name='rateType']:checked").val();
    	rate= $div.find("input[name='rate']").val();
    	//收费方式
    	mtc= $div.find("input[name='mtc']:checked").val();
    	//计算方式
    	computingMethod= $div.find("input[name='computingMethod']:checked").val();
    	//统一收费费率
    	downRate= $div.find("input[name='downRate']").val();
    }else{
    	$limitOutCount = $div.find("[name=limitOutCount]");
    	$limitOutOneLow = $div.find("[name=limitOutOneLow ]");
    	$minInAmount = $div.find("input[name='minInAmount']");
        $bankType = $div.find("select[name=choiceBankBrand]");
    }
    var freezeType= $('#freezeAccount').is(':checked');
    var freezeTrBZ=$('#freezeAccountBZ').val();
    var freezeTe=0;
    if(freezeType){
    	freezeTe=1;
    	if(""==freezeTrBZ || null==freezeTrBZ){
    		showMessageForFail("请填写冻结备注！");
    		return;
    	}
    }
    var data={
			"id":accountId,
			"limitIn":$.trim($limitIn.val()),
			"limitOut":$.trim($limitOut.val()),
			"limitOutOne":$.trim($limitOutOne.val()),
			"limitBalance":$.trim($limitBalance.val()),
			"freezeTe":freezeTe,
			"remark":freezeTrBZ
		};
    var validatePrint=[
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$limitOutOne,name:'最高单笔出款限额',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitBalance,name:'余额告警',type:'amountPlus'}
    ];
    if(type==accountTypeInBank){
    	// 银行卡类型参数更多
    	data["owner"]=$.trim($owner.val(),true);
    	data["bankType"]=$.trim($bankType.val(),true);
    	data["bankName"]=$.trim($bankName.val(),true);
		data["currSysLevel"]=$.trim($currSysLevel.val());
		data["peakBalance"]=$.trim($peakBalance.val());
		data["limitOutCount"]=$.trim($limitOutCount.val());
		data["limitOutOneLow"]=$.trim($limitOutOneLow.val());
		data["minInAmount"]=$.trim($minInAmount.val());
		data["province"]=$.trim($province.val(),true);
		data["city"]=$.trim($city.val(),true);
    	validatePrint.push(
        	{ele:$owner,name:'开户人',minLength:2,maxLength:10},
        	{ele:$bankName,name:'开户支行',maxLength:50},
        	{ele:$limitOutOneLow,name:'最低单笔出款限额',min:0,maxEQ:50000},
        	{ele:$limitOutCount,name:'当日出款笔数',minEQ:0,maxEQ:500},
        	{ele:$minInAmount,name:'最小入款金额',minEQ:0}
        );
    }else if(type==accountTypeInThird||type==accountTypeOutThird){
    	if(status==4){
    		if(computingMethod==0){
        		if(downRate*1>=11 || $.trim(downRate,true)==""){
        			showError("不能为空并且为大于等于0小于10的小数！");
    				return;
        		}
        		data['accountFeeConfig.feeType']=mtc;
        		data['accountFeeConfig.calFeeType']=computingMethod;
        		data['accountFeeConfig.calFeePercent']=(downRate*1/100);
        	}else{
        		charging=$div.find("input[name='charging']:checked").val();
        		data['accountFeeConfig.feeType']=mtc;
        		data['accountFeeConfig.calFeeType']=computingMethod;
        		data['accountFeeConfig.calFeeLevelType']=charging;
        	}
    	}
    	
    	data["bankType"]=$.trim($bankType.val(),true);
    	data["bankName"]=$.trim($thirdBankName.val(),true);
    	data["balance"]=$.trim($thirdBalance.val());
    	data["rateType"]=$.trim(rateType,true);
    	data["rate"]=$.trim(rate,true);
    	validatePrint.push({ele:$thirdBankName,name:'第三方',maxLength:50});
    	var json = [];
    	//记录结束金额为空的个数
    	var endAmountEmptys=0;
    	//获取阶梯式费率
    	if(rateType==1){
    		if($("#updateIncomeAccount").find("input[name='rates']").length>0){
        		for(var i=0;i<$("#updateIncomeAccount").find("input[name='rates']").length;i++){
        			var row = {};
        			row.startAmount=$("#updateIncomeAccount").find("input[name='startAmount']")[i].value;
        			if(row.startAmount==""){
        				showError("开始金额不能为空！");
        				return;
        			}
        			row.endAmount=$("#updateIncomeAccount").find("input[name='endAmount']")[i].value;
        			if(row.startAmount!="" && row.endAmount!="" && row.startAmount*1>row.endAmount*1){
        				showError("开始金额不能大于结束金额！");
        				return;
        			}
        			if(row.endAmount==""){
        				endAmountEmptys++;
        			}
        			row.rates=$("#updateIncomeAccount").find("input[name='rates']")[i].value;
        			if(row.rates*1>=100){
        				showError("费率不能超过或等于100%！");
        				return;
        			}
        			if(intersection($("#updateIncomeAccount").find("input[name='startAmount']")[i],i)==false)
        				return;
        			json.push(row);
        		}
        	}
    		if(endAmountEmptys>=2 || endAmountEmptys!=1){
        	    showError("结束金额只允许且必须有一个为空！");
        	    return;
        	}
    	}
    }
    if(rate*1>=100){
    	showError("费率不能超过或等于100%！");
		return;
    }
    if($.isEmptyObject(json))
    	data["rateValue"]="";
    else{
    	data["rateValue"]=JSON.stringify(json);
    	data["rate"]="";
    }
    if(!validateInput(validatePrint)){
    	return;
    }
    if(type==1){
	    if (!$.trim($owner.val())) {
	        $('#prompt_owner').text('请填写开户人').show(10).delay(1500).hide(10);
	        return;
	    }
	    if (!$.trim($bankType.val())) {
	        $('#prompt_choiceBankBrand').text('请选择开户行').show(10).delay(1500).hide(10);
	        return;
	    }
	    if (!$.trim($bankName.val())) {
	        $('#prompt_bankName').text('请填写开户支行').show(10).delay(1500).hide(10);
	        return;
	    }
    }
	bootbox.confirm("确定更新此账号信息?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/update',
				async:false,
				data:data,
				success:function(jsonObject){
			        if(jsonObject.status == 1&&jsonObject.data){
			        	//操作成功提示
			            $div.modal("toggle");
			        	showMessageForSuccess("修改成功");
			            if(fnName){
			            	fnName();
			            }
			            return true;
			        }else{
			        	showMessageForFail("账号修改失败："+jsonObject.message);
			        	return false;
			        }
			    },
			    error:function(result){
		        	showMessageForFail("修改失败："+jsonObject.message);
		        	return false;
			    }
			});
		}
	});
}

var do_updateIncomeAccountNoShowMessage=function(type,oldFlag,fnName){
	var $div=$("#updateIncomeAccount");
    var accountId = $div.find("#accountId").val();
    if(!accountId) return;
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $limitOutOne = $div.find("input[name='limitOutOne']");
    var $bankName = $div.find("input[name='bankName']");
    var $owner = $div.find("input[name='owner']");
	var $currSysLevel = $div.find("select[name='currSysLevel']");
    var $limitBalance = $div.find("input[name='limitBalance']");
    var $peakBalance = $div.find("input[name='peakBalance']");
    var $minInAmount;
    var $bankType;
    var $thirdBalance;
    var $limitOutCount;
    var $limitOutOneLow;
    var rateType;
    var rate;
    var mtc;
    var computingMethod;
    var downRate;
    var charging;
    let $province=$div.find("select[name='province']");
    let $city=$div.find("select[name='city']");
    if(type==accountTypeInThird||type==accountTypeOutThird){
    	$bankType= $div.find("select[name=choiceThirdBrand]");
    	$thirdBankName= $div.find("[name=thirdBankName]");
    	$thirdBalance = $div.find("input[name='thirdBalance']");
    	rateType=$("input[name='rateType']:checked").val();
    	rate= $div.find("input[name='rate']").val();
    	//收费方式
    	mtc= $div.find("input[name='mtc']:checked").val();
    	//计算方式
    	computingMethod= $div.find("input[name='computingMethod']:checked").val();
    	//统一收费费率
    	downRate= $div.find("input[name='downRate']").val();
    	charging=$div.find("input[name='charging']:checked").val();
    }else{
    	$limitOutCount = $div.find("[name=limitOutCount]");
    	$limitOutOneLow = $div.find("[name=limitOutOneLow ]");
    	$minInAmount = $div.find("input[name='minInAmount']");
        $bankType = $div.find("select[name=choiceBankBrand]");
    }
    var data={
			"id":accountId,
			"limitIn":$.trim($limitIn.val()),
			"limitOut":$.trim($limitOut.val()),
			"limitBalance":$.trim($limitBalance.val())
		};
    var validatePrint=[
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$limitBalance,name:'余额告警',type:'amountPlus'}
    ];
    if(type==accountTypeInBank){
    	// 银行卡类型参数更多
    	data["owner"]=$.trim($owner.val(),true);
    	data["bankType"]=$.trim($bankType.val(),true);
    	data["bankName"]=$.trim($bankName.val(),true);
		data["currSysLevel"]=$.trim($currSysLevel.val());
		data["peakBalance"]=$.trim($peakBalance.val());
		data["limitOutOne"]=$.trim($limitOutOne.val());
		data["limitOutCount"]=$.trim($limitOutCount.val());
		data["limitOutOneLow"]=$.trim($limitOutOneLow.val());
		data["minInAmount"]=$.trim($minInAmount.val());
		data["province"]=$.trim($province.val(),true);
		data["city"]=$.trim($city.val(),true);
    	validatePrint.push(
        	{ele:$owner,name:'开户人',minLength:2,maxLength:10},
        	{ele:$bankName,name:'开户支行',maxLength:50},
        	{ele:$limitOutOne,name:'最高单笔出款限额',type:'amountPlus',min:0,maxEQ:50000},
        	{ele:$limitOutOneLow,name:'最低单笔出款限额',min:0,maxEQ:50000},
        	{ele:$limitOutCount,name:'当日出款笔数',minEQ:0,maxEQ:500},
        	{ele:$minInAmount,name:'最小入款金额',minEQ:0}
        );
    }else if(type==accountTypeInThird||type==accountTypeOutThird){
    	if(computingMethod==0){
    		if(downRate*1>=11 || $.trim(downRate,true)==""){
    			showError("不能为空并且为大于等于0小于10的小数！");
				return;
    		}
    		data['accountFeeConfig.feeType']=mtc;
    		data['accountFeeConfig.calFeeType']=computingMethod;
    		data['accountFeeConfig.calFeePercent']=downRate;
    	}else{
    		data['accountFeeConfig.feeType']=mtc;
    		data['accountFeeConfig.calFeeType']=computingMethod;
    		data['accountFeeConfig.calFeeLevelType']=charging;
    	}
    	
    	data["bankType"]=$.trim($bankType.val(),true);
    	data["bankName"]=$.trim($thirdBankName.val(),true);
    	data["balance"]=$.trim($thirdBalance.val());
    	data["rateType"]=$.trim(rateType,true);
    	data["rate"]=$.trim(rate,true);
    	validatePrint.push({ele:$thirdBankName,name:'第三方',maxLength:50});
    	var json = [];
    	//记录结束金额为空的个数
    	var endAmountEmptys=0;
    	//获取阶梯式费率
    	if(rateType==1){
    		if($("#updateIncomeAccount").find("input[name='rates']").length>0){
        		for(var i=0;i<$("#updateIncomeAccount").find("input[name='rates']").length;i++){
        			var row = {};
        			row.startAmount=$("#updateIncomeAccount").find("input[name='startAmount']")[i].value;
        			if(row.startAmount==""){
        				showError("开始金额不能为空！");
        				return;
        			}
        			row.endAmount=$("#updateIncomeAccount").find("input[name='endAmount']")[i].value;
        			if(row.startAmount!="" && row.endAmount!="" && row.startAmount*1>row.endAmount*1){
        				showError("开始金额不能大于结束金额！");
        				return;
        			}
        			if(row.endAmount==""){
        				endAmountEmptys++;
        			}
        			row.rates=$("#updateIncomeAccount").find("input[name='rates']")[i].value;
        			if(row.rates*1>=100){
        				showError("费率不能超过或等于100%！");
        				return;
        			}
        			if(intersection($("#updateIncomeAccount").find("input[name='startAmount']")[i],i)==false)
        				return;
        			json.push(row);
        		}
        	}
    		if(endAmountEmptys>=2 || endAmountEmptys!=1){
        	    showError("结束金额只允许且必须有一个为空！");
        	    return;
        	}
    	}
    }
    if(rate*1>=100){
    	showError("费率不能超过或等于100%！");
		return;
    }
    if($.isEmptyObject(json))
    	data["rateValue"]="";
    else{
    	data["rateValue"]=JSON.stringify(json);
    	data["rate"]="";
    }
    if(!validateInput(validatePrint)){
    	return;
    }
    if(type==1){
	    if (!$.trim($owner.val())) {
	        $('#prompt_owner').text('请填写开户人').show(10).delay(1500).hide(10);
	        return;
	    }
	    if (!$.trim($bankType.val())) {
	        $('#prompt_choiceBankBrand').text('请选择开户行').show(10).delay(1500).hide(10);
	        return;
	    }
	    if (!$.trim($bankName.val())) {
	        $('#prompt_bankName').text('请填写开户支行').show(10).delay(1500).hide(10);
	        return;
	    }
    }
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/update',
				async:false,
				data:data,
				success:function(jsonObject){
			        if(jsonObject.status == 1&&jsonObject.data){
			            if(fnName){
			            	fnName();
			            }
			            return true;
			        }else{
			        	showMessageForFail("账号修改失败："+jsonObject.message);
			        	return false;
			        }
			    },
			    error:function(result){
		        	showMessageForFail("修改失败："+jsonObject.message);
		        	return false;
			    }
			});
}

function deleteRate(handicapId,accountId,calFeeLevelType,index){
	var $div=$("#updateIncomeAccount");
	var data={
			"handicapId":handicapId,
			"accountId":accountId,
			"calFeeLevelType":calFeeLevelType,
			"index":index
		};
	bootbox.confirm("确定删除?", function(result) {
		if (result) {
			$.ajax({
				type:"POST",
				dataType:'JSON',
				url:'/r/account/fee/del',
				contentType: 'application/json',
				async:false,
				data:JSON.stringify(data),
				success:function(jsonObject){
					if(jsonObject.status == 1){
			        	showMessageForSuccess("删除成功");
			        	$div.modal("toggle");
			        	setTimeout(function(){
			        		showModal_updateIncomeAccount(accountId,"showAccountList");
			        	}, 450);
			        }else{
			        	showMessageForFail("删除失败："+jsonObject.message);
			        }
			    },
			    error:function(result){
		        	showMessageForFail("删除失败："+jsonObject.message);
			    }
			});
		}
	});
}

function addRate(){
	var $div=$("#updateIncomeAccount");
	//计费方式
	var charging=$div.find("input[name='charging']:checked").val();
	//盘口id
	var handicapId=$div.find("[name=handicap_select]").val();
	//账号id
	var accountId = $div.find("#accountId").val();
	var accountInfo=getAccountInfoById(accountId);
	var data={
		"handicapId":handicapId,
		"accountId":$.trim(accountId)
	};
	//开始金额
	var moneyBegin=$div.find("[name=charStartAmount]").val();
	//结束金额
	var moneyEnd=$div.find("[name=charEndAmount]").val();
	var bischecked= $('#Infinity').is(':checked');
	if(bischecked){
		moneyEnd="Infinity";
	}
	if(moneyBegin==""){
		showError("开始金额不能为空，请检查！");
 	    return;
	}
	//收费
	var rateAmount=$div.find("[name=rateAmount]").val();
	if(rateAmount==""){
		showError("收费未设置，请检查！");
 	    return;
	}
	if(charging==0){
		if(rateAmount*1>=11){
			showError("费率百分比只能在0-10之间，请检查！");
	 	    return;
		}
	}
	if(!bischecked&&moneyBegin*1>moneyEnd*1){
		 showError("金额设置有误，请检查！");
 	    return;
	}
	if(moneyBegin==""&&moneyEnd==""&&!bischecked){
		showError("金额设置有误，请检查！");
 	    return;
	}
	if(charging==1){
		data["moneyBegin"]=$.trim(moneyBegin),
		data["moneyEnd"]=$.trim(moneyEnd),
		data["calFeeLevelType"]=1,
		data["feeMoney"]=$.trim(rateAmount)
	}else{
		data["moneyBegin"]=$.trim(moneyBegin),
		data["moneyEnd"]=$.trim(moneyEnd),
		data["calFeeLevelType"]=0,
		data["feePercent"]=$.trim(rateAmount*1/100)
	}
	bootbox.confirm("确定添加?", function(result) {
		if (result) {
			$.ajax({
				type:"POST",
				dataType:'JSON',
				url:'/r/account/fee/add',
				contentType: 'application/json',
				async:false,
				data:JSON.stringify(data),
				success:function(jsonObject){
					if(jsonObject.status == 1){
						do_updateIncomeAccountNoShowMessage(accountInfo.type,accountInfo.flag,"showAccountList");
						$div.modal("toggle");
			        	showMessageForSuccess("修改成功");
			        	setTimeout(function(){
			        		showModal_updateIncomeAccount(accountId,"showAccountList");
			        	}, 450);
			        }else{
			        	showMessageForFail("添加失败："+jsonObject.message);
			        	do_updateIncomeAccountNoShowMessage(accountInfo.type,accountInfo.flag,"showAccountList");
			        	$div.modal("toggle");
			        	setTimeout(function(){
			        		showModal_updateIncomeAccount(accountId,"showAccountList");
			        	}, 450);
			        }
			    },
			    error:function(result){
		        	showMessageForFail("添加失败："+jsonObject.message);
			    }
			});
		}
	});
}

// 自动加载银行类别
var getBankTyp_select=function($div,bankType,titleName){
	var options="";
	options+="<option value='' >"+(titleName?titleName:"--------------请选择--------------")+"</option>";
	$.each(bank_name_list,function(index,record){
		if(bankType&&bankType==record){
			options+="<option selected value="+record+" >"+record+"</option>";
		}else{
			options+="<option value="+record+" >"+(isHideOutAccountAndModifyNouns?replaceBankStr(record):record)+"</option>";
		}
	});
	$div.html(options);
}

// 自动加载盘口选择列表
var getHandicap_select=function($div,handicapId,titleName){
	var options="";
	//如果handicapId 传入0 则使用当前用户全部权限的盘口ID集合，输入null或者其它值 默认选项值为空
	var defaultValue="";
	if(handicapId==0){
		defaultValue=handicapId_list.toString();
	}
	options+="<option value='"+defaultValue+"' >"+(titleName?titleName:"--------------请选择--------------")+"</option>";
	$.each(handicap_list,function(index,record){
		if(handicapId&&record.id==handicapId){
			options+="<option selected value="+record.id+" code="+record.code+" >"+record.name+"</option>";
		}else{
			options+="<option value="+record.id+" code="+record.code+" >"+record.name+"</option>";
		}
	});
	$div.html(options);
}

// 自动加载账号类型选择列表
var getAccountType_select=function($div,accountType,titleName){
	var accountType4edit = [
		{id:1,name:"入款银行卡"},
		{id:5,name:"出款银行卡"},
		{id:8,name:"备用卡"},
		{id:10,name:"微信专用下发卡"},
		{id:11,name:"支付宝专用下发卡"},
		{id:12,name:"第三方专用下发卡"},
		{id:13,name:"下发卡"},
		{id:14,name:"客户绑定银行卡"}
	]
	var options="";
	options+="<option value='' >"+(titleName?titleName:"--------------请选择--------------")+"</option>";
	$.each(accountType4edit,function(index,record){
		if(accountType&&record.id==accountType){
			options+="<option selected value="+record.id+" >"+record.name+"</option>";
		}else{
			options+="<option value="+record.id+" >"+record.name+"</option>";
		}
	});
	$div.html(options);
}
var getAccountType_select_search=function($div,titleName,type,subType){
	var accountType4edit = [
		{id:1,subType:0,name:"银行入款卡"},
		{id:1,subType:1,name:"支付宝入款卡"},
		{id:1,subType:2,name:"微信入款卡"},
		{id:1,subType:3,name:"云闪付入款卡"},
		{id:5,name:"出款银行卡"},
		{id:8,name:"备用卡"},
		{id:10,name:"微信专用下发卡"},
		{id:11,name:"支付宝专用下发卡"},
		{id:12,name:"第三方专用下发卡"},
		{id:13,name:"下发卡"}
		]
	var options="";
	options+="<option value='' >"+(titleName?titleName:"--------------请选择--------------")+"</option>";
	$.each(accountType4edit,function(index,record){
		options+="<option value="+record.id+"  subType="+record.subType+">"+record.name+"</option>";
	});
	$div.html(options);
	if(type){
		if(type==accountTypeInBank){
			//入款卡加载指定子类型
			if(!subType)subType=0;
			$div.find("option[value=1][subType="+subType+"]").attr("selected","selected");
		}else{
			$div.find("option[value="+type+"]").attr("selected","selected");
		}
		
	}
}

// 自动加载银行类别
var getThirdType_select=function($div,thirdType,title){
	var options="";
	options+="<option value='' >"+(title?title:"--------------请选择--------------")+"</option>";
	$.each(third_name_list,function(index,record){
		if(thirdType&&thirdType==record){
			options+="<option selected value="+record+" >"+record+"</option>";
		}else{
			options+="<option value="+record+" >"+record+"</option>";
		}
	});
	$div.html(options);
}
/** 循环生成停用原因悬浮提示  */
var loadHover_accountStopReasonHover=function(idList){
	$.each(idList,function(index,id){
		$("#stopAccountTitle"+id).hover(function(){
			$("#stopAccountTitle"+id).attr("title",stopReasonTitle(id));
		})
	});
}
var stopReasonTitle=function(accountId){
	var result="未知暂停原因";
	$.ajax({
		type: "GET",
		dataType: 'JSON',
		url: '/r/account/statusDesc?id='+accountId,
		async: false,
		success: function (jsonObject) {
			if(jsonObject&&jsonObject.data){
				result=jsonObject.data;
			}
		}});
	return result;
}
/**
 * 循环生成银行卡id
 */
var loadHover_accountInfoHover=function(idList){
	//拥有隐藏账号信息的权限时 不显示任何东西
	if(isHideAccount){
		return;
	}
	// 发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({type:"GET", async:false, dataType:'html', url : "/"+sysVersoin+"/html/common/showInfoHover.html",
		success : function(html){
			$.each(idList,function(index,result){
				if(result.type == 'mobileInfoHover'){// 手机基本信息
					$("[data-toggle='mobileInfoHover"+result.acc+"']").popover({
						html : true,
						title: function(){
							return '<center class="blue">手机信息</center>';
						},
						delay:{show:0, hide:100},
						content: function(){
							var $div = $(html).find("#clientMobile");
							var data = findMobileFromCloud(result.acc);
							if(data){
								if(data.type==MobileTypeCustomer){
									// 客户类型有信用额度和返佣信息
									$div.find(".typeCustomerShow").show();
									$div.find("span[name=creditLimit]").text(data.creditLimit?data.creditLimit:'无');
									if(data.bonusEntity){
										$div.find("span[name=bonusCard]").text(data.bonusEntity.account?data.bonusEntity.account:'');
										$div.find("span[name=bonusCardOwner]").text(data.bonusEntity.owner?data.bonusEntity.owner:'');
										$div.find("span[name=bonusCardName]").text(data.bonusEntity.bankName?data.bonusEntity.bankName:'');
									}
								}
								var HandicapBatch=getHandicapBatchInfoByCode();
								$div.find("span[name=handicapName]").text(HandicapBatch&&HandicapBatch[data.handicap]?HandicapBatch[data.handicap].name:'');
								$div.find("span[name=levelNameToGroup]").text(data.level?(data.level==currentSystemLevelOutter?"外层":(data.level==currentSystemLevelInner?"内层":"指定层")):"");
								$div.find("span[name=mobile]").text(data.mobile);
								$div.find("span[name=owner]").text(data.owner?data.owner:'');
								$div.find("span[name=type]").text(data.type==MobileTypeCustomer?'客户':'自用');
								$div.find("span[name=status]").text(data.status==mobileStatusNormal?'在用':(data.status==mobileStatusFreeze?'冻结':'停用'));
								$div.find("span[name=device1]").text(data.device1?data.device1:"");
								$div.find("span[name=device2]").text(data.device2?data.device2:"");
								$div.find("span[name=wechat]").text(data.wechat?hideAccountAll(data.wechat):'未注册');
								$div.find("span[name=alipay]").text(data.alipay?hideAccountAll(data.alipay):'未注册');
								$div.find("span[name=bankCard]").text(data.bank?hideAccountAll(data.bank):'未绑定');
								$div.find("span[name=updateTime]").text(timeStamp2yyyyMMddHHmmss(data.updateTime));
								$div.find("span[name=remark]").text(data.remark?data.remark:'');
							}
							return $div;
						}
					});
				}else if(result.type == 'mobileBalHover'){// 手机账号：总余额信息
					$("[data-toggle='mobileBalHover"+result.acc+"']").popover({
						html : true,
						title: function(){
							return title_accountInfoHover(null,result.type);
						},
						delay:{show:0, hide:100},
						content: function(){
							return content_accountInfoHover(null,html,'mobileBalHover',result.acc,result.mobile);
						}
					});
				}else if(result.type == 'alipayInfoHover'||result.type == 'wechatInfoHover' || result.type== 'bankInfoHover'){// 微信/支付宝/银行卡
					$("[data-toggle='"+result.type+result.mobile+"']").popover({
						html : true,delay:{show:0, hide:100},
						title: '<center>账号信息</center>',
						content: function(){
							return content_accountInfoHover(null,html,result.type,result.acc,result.mobile);
						}
					});
				}else if(result.type == 'transInfoHover'){
					$("[data-toggle='"+result.type+result.id+"']").popover({
						html : true,delay:{show:0, hide:100},title:'<center>下发信息</center>',
						content: function(){
							var retData =  findTransInfoByToId(result.id);
							var ret = retData[0],resist = retData[1];
							if(!ret||ret.length ==0){
								resist = resist ? resist : '无数据';
								return	'<div class="alert center no-margin no-border" style="font-size:18px;width:550px">'+resist+'</div>'
							}
							var html = '<div style="width:600px">';
							html = html +'<div class="col-sm-12">';
							html = html +	'<div class="col-xs-5 text-right"><center>下发账号</center></div>';
							html = html +	'<div class="col-xs-2 no-padding-lr"><center>分配时间</center></div>';
							html = html +	'<div class="col-xs-1 no-padding-lr"><center>金额</center></div>';
							html = html +	'<div class="col-xs-2 no-padding-lr"><center>状态</center></div>';
							html = html +	'<div class="col-xs-2 no-padding-lr"><center>更新时间</center></div>';
							html = html +'</div>';
							ret.forEach(function(val) {
								var alias = _checkObj(val.frAlias), acc = _checkObj(val.frAcc), owner = _checkObj(val.frOwner);
								var ret = (alias ? alias: '无') + (acc ? '|'+(acc.substring(0, 3) + "**" + acc.substring(acc.length - 4)): '无') + (owner ? '|'+owner: '无');
								var style = val.status && val.status == 20 ?'color: white;background-color: limegreen':'';
								html = html +'<div class="col-sm-12" style="'+style+'">';
								html = html +	'<div class="col-xs-5 text-left">'+(ret == '无无无'? '': ret)+'</div>';
								html = html +	'<div class="col-xs-2 no-padding-lr"><center>'+timeStamp2yyyyMMddHHmmss(val.createTime).substring(11, 19)+'</center></div>';
								html = html +	'<div class="col-xs-1 text-left no-padding-lr">'+val.transAmt+'</div>';
								html = html +	'<div class="col-xs-2 no-padding-lr"><center>'+val.statusMsg+'</center></div>';
								html = html +	'<div class="col-xs-2 no-padding-lr"><center>'+timeStamp2yyyyMMddHHmmss(val.ltTime).substring(11, 19)+'</center></div>';
								html = html +'</div>';
								html = html +'<div class="col-sm-12" style="height:1px;"></div>';
							});
							if(resist){
								html = html +'<div class="col-sm-12"><center>'+resist+'</center></div>';
							}
							html + '</div>';
							return html;
						}
					});
				}else if(result.type == 'transAskMonitorRiskHover'){
					$("[data-toggle='"+result.type+result.id+"']").popover({
						html : true,delay:{show:0, hide:100},title:'<center>对账信息</center>',
						content: function(){
							var ret =  findTransAckMonitorRiskInfoByToId(result.id);
							if(!ret){
								return	'<div class="alert center no-margin no-border" style="font-size:18px;width:550px">无数据</div>'
							}
							var html = '<div style="width:600px">';
							var retList = ret.retList;
							if(retList && retList.length > 0){
								html = html +'<div class="col-sm-12">';
								html = html +	'<div class="col-xs-2 no-padding-lr"><center>交易金额</center></div>';
								html = html +	'<div class="col-xs-3 no-padding-lr"><center>交易时间</center></div>';
								html = html +	'<div class="col-xs-5 text-right"><center>对方账号</center></div>';
								html = html +	'<div class="col-xs-2 text-right"><center>确认状态</center></div>';
								html = html +'</div>';
								retList.forEach(function(val) {
									var alias = _checkObj(val.oppAlias), owner = _checkObj(val.oppOwner);
									var ret = (alias ? alias: '无')  + (owner ? '|'+owner: '无');
									ret = (ret == '无无'?'无':ret)+(val.sort ==1?'(出款)':'(下发)');
									//ret = '无|出款';
									var style = val.status && val.status == 20 ?'color: white;background-color: limegreen':'';
									var st = val.confirm ==0 ?'无效':(val.confirm ==1?'有效':'待确认');
									html = html +'<div class="col-sm-12" style="'+style+'">';
									html = html +	'<div class="col-xs-2 no-padding-lr"><center>'+val.tdAmt+'</center></div>';
									html = html +	'<div class="col-xs-3 no-padding-lr"><center>'+timeStamp2yyyyMMddHHmmss(val.tdTm).substring(11, 19)+'</center></div>';
									html = html +	'<div class="col-xs-5 text-left"><center>'+(ret == '无无'? '': ret)+'</center></div>';
									html = html +	'<div class="col-xs-2 text-left no-padding-lr"><center>'+st+'</center></div>';
									html = html +'</div>';
									html = html +'<div class="col-sm-12" style="height:1px;"></div>';
								});

							}
							var idSt = ret.ret?'&nbsp;&nbsp;对账结果：<span>成功</span>&nbsp;&nbsp;':'对账结果：<span class="red">失败</span>&nbsp;&nbsp;';
							var idAmt = '&nbsp;&nbsp;对账前后金额：'+ret.lastRealBal+'&nbsp;元&nbsp;-&nbsp;'+ret.thisRealBal+'&nbsp;元&nbsp;=&nbsp;'+setAmountAccuracy(ret.lastRealBal-ret.thisRealBal)+'&nbsp;元&nbsp;';
							var idTm =  '&nbsp;&nbsp;对账时间：'+timeStamp2yyyyMMddHHmmss(ret.riskMillis);
                            html = html +'<div class="col-sm-12">';
                            html = html +   idSt;
                            html = html +'</div>';
                            html = html +'<div class="col-sm-12">';
                            html = html +	idTm;
                            html = html +'</div>';
                            html = html +'<div class="col-sm-12">';
                            html = html +	idAmt;
                            html = html +'</div>';
							return html;
						}
					});
				}else{
					$("[data-toggle='accountInfoHover"+result.id+"']").popover({
						html : true,
						title: function(){
							return title_accountInfoHover(result.id,result.type);
						},
						delay:{show:0, hide:100},
						content: function(){
							return content_accountInfoHover(result.id,html,result.type,result.acc,null);
						}
					});
				}
			});
		}
	});
}

var title_accountInfoHover=function(accountId,type){
    var title='';
    if(type=='mobileBalHover'){
        title='余额信息&nbsp;(银行卡|微信|支付宝)&nbsp;&nbsp;&nbsp;';
    }else{
        title='账号信息&nbsp;(id:'+accountId+')&nbsp;&nbsp;&nbsp;';
    }
/*	if(SysEvent.retrieve(accountId)){
		var hostInfo=JSON.parse(SysEvent.retrieve(accountId));
		if(hostInfo&&hostInfo.ip){
			title+=hostInfo.ip;
		}
	}*/
	return '<center>'+title+'</center>';
}
/**
 * 循环生成主机信息
 */
var loadHover_hostInfoHover=function(allHost){
	var html='<div style="width:600px;" class="col-sm-12"></div>';
	allHost.map(function(record){
		var host=record.host
		if(!host){
			host=record.ip;
		}
		$("[data-toggle='hostInfoHover"+host+"']").popover({  
			html : true,    
			title: '<center>账号列表</center>', 
			delay:{show:0, hide:100},  
			content: function() {  
				return content_hostInfoHover(host,html);
			}
		});
	});
}
var content_hostInfoHover=function(host,html){
	var accountHtml='<div class="col-xs-3"><strong>编号</strong></div>\
					<div class="col-xs-4"><strong>账号</strong></div>\
					<div class="col-xs-3"><strong>开户行</strong></div>\
					<div class="col-xs-2"><strong>开户人</strong></div>';
	var hasRecord=false;
	$.ajax({
		dataType:'json',
		type:"get",
		async:false, 
		url:API.r_host_findAccountListOfHost, 
		data:{host:host},
        success: function (jsonObject) {
        	 if(jsonObject.status==-1){
                 showMessageForFail("查询警告："+jsonObject.message);return;
             }
        	 if(jsonObject.data){
        		 jsonObject.data.map(function(record){
             		// 判断是否是第三方
             	    var recordIsThird=accountTypeInThird==record.type||accountTypeOutThird==record.type;
             	    if(!recordIsThird){
             	    	hasRecord=true;
             	    	// 银行卡列表
             	    	accountHtml+='<div class="col-xs-3"><strong>'+record.alias+'</strong></div>\
     	    						<div class="col-xs-4"><strong>'+record.account+'</strong></div>\
     	    						<div class="col-xs-3"><strong>'+record.bank+'</strong></div>\
     	    						<div class="col-xs-2"><strong>'+record.owner+'</strong></div>';
                 	}
             	 });
        	 }
        }
	});
	if(!hasRecord){
		accountHtml='<div style="margin-bottom:0px;"class="center"><h4 class="smaller lighter blue">未添加账号</h4></div>';
	}
	return $(html).append(accountHtml);
}
/**
 * 循环生成入款信息ID
 */
var loadHover_InOutInfoHover=function(idList){
	// 发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoHover.html", 
		success : function(html){
			$.each(idList,function(index,id){
				$("[data-toggle='InOutInfoHover"+id+"']").popover({  
			        html : true,    
			        title: '<center>平台提单信息</center>', 
			        delay:{show:0, hide:100},  
			        content: function() {  
			        	return content_InOutInfoHover(id,html);    
			        }
			    });
			});
		}
	});
	
}
// 公司入款 订单号显示
var loadHover_IncomeRequestByHoverOrderNo=function(infoArray){
    // 发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
    $.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
    $.ajax({
        type:"GET",
        async:false,
        dataType:'html',
        url : "/"+sysVersoin+"/html/common/showIncomeRequestByHoverOrderNo.html",
        success : function(res){
        	$.each(infoArray,function (i,val) {
                $("[data-toggle='incomeRequestOrderNoHover_"+val.reqId+"']").popover({
                    html : true,
                    title: '<center>匹配详情</center>',
                    delay:{show:0, hide:100},
                    content: function() {
                        return content_IncomeRequestMatchedInfoByHoverOrderNO(val.reqId,res);
                    }
                });
            });
        }
    });
}
var content_IncomeRequestMatchedInfoByHoverOrderNO = function (reqId,html) {
	var singleRecord ;
	$.ajax({
		type:'get',
		url:'/r/income/matchedInfo',
		data:{"reqId":reqId},
		dataType:'json',
		async:false,
		success:function (res) {
			if (res){
				if (res.status==1){
                    singleRecord = res.data;
				}
			}
        }
	});
    var $div = $(html).find("#IncomeRequestMatchedInfoHover");
    if (singleRecord) {
		$div.find('#handicapNameHover').text(singleRecord.handicapNameHover?singleRecord.handicapNameHover:"无");
        $div.find('#levelNameHover').text(singleRecord.levelNameHover?singleRecord.levelNameHover:"无");
        $div.find('#memberAccountHover').text(singleRecord.memberAccountHover ?singleRecord.memberAccountHover :"无");
        $div.find('#toAccountHover').text(singleRecord.toAccountHover?(singleRecord.toAccountHover.substring(0,4)+'**'+singleRecord.toAccountHover.substring(singleRecord.toAccountHover.length-4,singleRecord.toAccountHover.length)):"无");
        $div.find('#amountHover').text(singleRecord.amountHover?singleRecord.amountHover:"无");
        $div.find('#bankAmountHover').text(singleRecord.bankAmountHover?singleRecord.bankAmountHover:"无");
        $div.find('#amountMinusHover').text(singleRecord.amountMinusHover?singleRecord.amountMinusHover:"无");
        $div.find('#orderNoHover').text(singleRecord.orderNoHover?singleRecord.orderNoHover:"无");
        $div.find('#payerHover').text(singleRecord.payerHover?singleRecord.payerHover:"无");
        $div.find('#receiverHover').text(singleRecord.receiverHover?singleRecord.receiverHover:"无");
        $div.find('#receivedBankHover').text(singleRecord.receivedBankHover?singleRecord.receivedBankHover:"无");
        $div.find('#bankFlowRemarkHover').text(singleRecord.bankFlowRemarkHover?singleRecord.bankFlowRemarkHover:"无");
        $div.find('#creatTimeHover').text(singleRecord.creatTimeHover?timeStamp2yyyyMMddHHmmss(singleRecord.creatTimeHover):"无");
        $div.find('#tradeTimeHover').text(singleRecord.tradingTimeHover?timeStamp2yyyyMMddHHmmss(singleRecord.tradingTimeHover):"无");
        $div.find('#matchedTimeHover').text(singleRecord.matchedTimeHover?timeStamp2yyyyMMddHHmmss(singleRecord.matchedTimeHover):"无");
        $div.find('#consumeTimeHover').text(singleRecord.consumeTimeHover?singleRecord.consumeTimeHover:"无");
	}else{
        $div = $(html).find("#hover_nodata");
    }
    return $div;
}
// 模拟动态加载内容(真实情况可能会跟后台进行ajax交互)
var content_InOutInfoHover=function (id,html) {
	// 默认无数据
	var $div = $(html).find("#hover_nodata");
	var result=findInOutByLogId(id);
	if(result){
		if(result.incomeInfo){
			$div= $(html).find("#incomeInfoHover");
			content_IncomeInfoHover(result.incomeInfo,$div);
		}else if(result.outwardTask){
			$div= $(html).find("#outwardInfoHover");
			content_OutwardInfoHover(result.outwardTask,result.outwardRequest,$div);
		}
	}
	return $div;
}
/**
 * 加载出款信息悬浮提示框
 */
var content_OutwardInfoHover=function(outwardTask,outwardRequest,$div){
	if(outwardTask){
		$div.find("span[name=toAccount]").text(outwardTask.toAccount?hideAccountAll(result.toAccount):'无');
		$div.find("span[name=statusStr]").text(outwardTask.statusStr?outwardTask.statusStr:'无');
		$div.find("span[name=asignTime]").text(outwardTask.asignTime?timeStamp2yyyyMMddHHmmss(outwardTask.asignTime):'无');
		$div.find("span[name=timeConsuming]").text(outwardTask.timeConsuming?timeDifferenceForMatch(outwardTask.timeConsuming,0):'无');
		$div.find("span[name=operatorUid]").text(outwardTask.operatorUid?outwardTask.operatorUid:'机器');
		$div.find("span[name=remark]").html((outwardTask.remark?outwardTask.remark+"<br/>":'')+(outwardRequest.remark?outwardRequest.remark:''));
	}
	if(outwardRequest){
		$div.find("span[name=toAccountName]").text(outwardRequest.toAccountName?outwardRequest.toAccountName:'无');
		$div.find("span[name=toAccountBank]").text(outwardRequest.toAccountBank?outwardRequest.toAccountBank:'无');
		$div.find("span[name=toAccountOwner]").text(outwardRequest.toAccountOwner?outwardRequest.toAccountOwner:'无');
		$div.find("span[name=orderNo]").text(outwardRequest.orderNo?outwardRequest.orderNo:'无');
		$div.find("span[name=createTime]").text(outwardRequest.createTime?timeStamp2yyyyMMddHHmmss(outwardRequest.createTime):'无');
		$div.find("span[name=updateTime]").text(outwardRequest.updateTime?timeStamp2yyyyMMddHHmmss(outwardRequest.updateTime):'无');
		$div.find("span[name=member]").text(outwardRequest.member?outwardRequest.member:'无');
		$div.find("span[name=review]").text(outwardRequest.review?outwardRequest.review:'无');
		$div.find("span[name=reviewerUid]").text(outwardRequest.reviewerUid?outwardRequest.reviewerUid:'无');
		$div.find("span[name=amount]").text(outwardRequest.amount?outwardRequest.amount:'0');
	}
}

/**
 * 加载入款卡悬浮内容
 */
var content_IncomeInfoHover=function(result,$div){
	// 平台入款type集合
	var PlatFrom_Type = [incomeRequestTypePlatFromAli,incomeRequestTypePlatFromWechat,incomeRequestTypePlatFromBank,incomeRequestTypePlatFromThird]; 
	// 平台入款 显示玩家盘口层级
	if(result.type&&$.inArray(result.type, PlatFrom_Type)!=-1){
		$div.find(".PlatFrom_Type_Show").show();
	}else{
		$div.find(".PlatFrom_Type_Show").hide();
	}
	$div.find("span[name=fromAccount]").text(result.fromAccount?hideAccountAll(result.fromAccount):'无');
	$div.find("span[name=fromAlias]").text(result.fromAlias?result.fromAlias:'无');
	$div.find("span[name=fromBankType]").text(result.fromBankType?result.fromBankType:'无');
	$div.find("span[name=fromOwner]").text(result.fromOwner?result.fromOwner:'无');
	$div.find("span[name=toAccount]").text(result.toAccount?hideAccountAll(result.toAccount):'无');
	$div.find("span[name=toAlias]").text(result.toAlias?result.toAlias:'无');
	$div.find("span[name=toBankType]").text(result.toBankType?result.toBankType:'无');
	$div.find("span[name=toOwner]").text(result.toOwner?result.toOwner:'无');
	$div.find("span[name=orderNo]").text(result.orderNo?result.orderNo:'无');
	$div.find("span[name=handicapName]").text(result.handicapName?result.handicapName:'无');
	$div.find("span[name=levelName]").text(result.levelName?result.levelName:'无');
	$div.find("span[name=amount]").text(result.amount?result.amount:'0');
	$div.find("span[name=fee]").text(result.fee?result.fee:'0');
	$div.find("span[name=typeName]").text(result.typeName?result.typeName:'无');
	$div.find("span[name=statusName]").text(result.statusName?result.statusName:'无');
	$div.find("span[name=createTime]").text(result.createTime?timeStamp2yyyyMMddHHmmss(result.createTime):'无');
	$div.find("span[name=updateTime]").text(result.updateTime?timeStamp2yyyyMMddHHmmss(result.updateTime):'无');
	$div.find("span[name=memberUserName]").text(result.memberUserName?result.memberUserName:'无');
	$div.find("span[name=memberRealName]").text(result.memberRealName?result.memberRealName:'无');
	$div.find("span[name=operatorUid]").text(result.operatorUid?result.operatorUid:'机器');
	$div.find("span[name=remark]").text(result.remark?result.remark:'无');
	$div.find("span[name=limitBalance]").text(result.limitBalance?result.limitBalance:'0');
}
var loadHover_BankLogInfoByIncomeReqIdArray = function(incomeReqId){
		var model ='<div id="BankLogInfoHover_others"  style="width:450px;" >\
						<div class="col-sm-12">\
							<div class="col-xs-3 text-right"><strong>转出账号</strong></div><div class="col-xs-6 no-padding-lr"><span name="modifier">{fromAccountNO}</span></div>\
						</div>\
						<div class="col-sm-12  bk-color-f9">\
							<div class="col-xs-3 text-right"><strong>转入账号</strong></div><div class="col-xs-6 no-padding-lr"><span name="modifier">{toAccount}</span></div>\
						</div>\
						<div class="col-sm-12">\
							<div class="col-xs-3 text-right"><strong>收款人</strong></div><div class="col-xs-6 no-padding-lr"><span name="modifier">{toAccountOwner}</span></div>\
						</div>\
						<div class="col-sm-12 bk-color-f9">\
							<div class="col-xs-3 text-right  "><strong>交易金额</strong></div><div class="col-xs-6 no-padding-lr"><span name="limitIn">{amount}</span></div>\
						</div>\
						<div class="col-sm-12">\
							<div class="col-xs-3 text-right "><strong>交易时间</strong></div><div class="col-xs-6 no-padding-lr"><span name="limitOut">{tradingTimeStr}</span></div>\
						</div>\
						<div class="col-sm-12">\
							<div class="col-xs-3 text-right"><strong>备注</strong></div><div class="col-xs-6 no-padding-lr"><span name="limitOut">{remark}</span></div>\
						</div>\
					</div>';
		$.each(incomeReqId,function(index,result){
			$("[data-toggle='bankLogInfoHover"+result.id+"']").popover({
				html : true,title: '<center>银行流水信息</center>',delay:{show:0, hide:100},
				content: function() {
					var data = null;
					$.ajax({type:"POST",url:"/r/banklog/findByIncomeReqId",data:{"id":result.id},dataType:'json',async:false,success:function(jsonObject){
						if(jsonObject.status == 1){
							data = jsonObject.data;
							data.fromAccountNO=data.fromAccountNO?data.fromAccountNO:'';
							data.toAccount=data.toAccount?data.toAccount:'';
							data.toAccountOwner=data.toAccountOwner?data.toAccountOwner:'';
							data.amount=data.amount?data.amount:'';
							data.tradingTimeStr=data.tradingTimeStr?data.tradingTimeStr:'';
							data.remark=data.remark?data.remark:'';
						}
					}});
					if(data==null){
						data = {fromAccountNO:'',toAccount:'',toAccountOwner:'',amount:'',tradingTimeStr:'',remark:''};
					}
					return fillDataToModel4Item(data,model);
				}
			});
		});
}

// 模拟动态加载内容(真实情况可能会跟后台进行ajax交互)
function content_accountInfoHover(id,html,type,acc,mobile) {
    if('mobileBalHover' ==  type){
        var $div = $(html).find("#clientInfo_bankCard_wechat_alipay");
		var data =  findMobileBalFromCloud(acc);
		if(data){
			$div.find("span[name=mobile]").text(data.mobile);
			$div.find("span[name=balAli]").text(data.balAli?data.balAli:0);
			$div.find("span[name=balWec]").text(data.balWec?data.balWec:0);
			$div.find("span[name=balBank]").text(data.balBank?data.balBank:0);
			$div.find("span[name=balInAliDaily]").text(data.balInAliDaily?data.balInAliDaily:0);
			$div.find("span[name=balInWecDaily]").text(data.balInWecDaily?data.balInWecDaily:0);
			$div.find("span[name=balInBankDaily]").text(data.balInBankDaily?data.balInBankDaily:0);
			$div.find("span[name=balOutAliDaily]").text(data.balOutAliDaily?data.balOutAliDaily:0);
			$div.find("span[name=balOutWecDaily]").text(data.balOutWecDaily?data.balOutWecDaily:0);
			$div.find("span[name=balOutBankDaily]").text(data.balOutBankDaily?data.balOutBankDaily:0);
			$div.find("span[name=alipay]").text(hideAccountAll(data.alipay));
			$div.find("span[name=wechat]").text(hideAccountAll(data.wechat));
			$div.find("span[name=bank]").text(hideAccountAll(data.bank));
		}else{
			 $div = $(html).find("#hover_nodata");
		}
		return $div;
    }else{
		var $div;
		var result = id ? getAccountInfoById(id) :findMobileFromCloud(mobile);
        if(result){
            if(type&&type!="Bank"&&type!="incomeBank"&&type!="third"){
                $div = $(html).find("#accountInfoHover_others");
                initAccountInfo_Others($div,result,type,mobile);
            }else{
                $div = $(html).find("#accountInfoHover");
                initAccountInfo_bank($div,result,type);
            }
        }else{
            $div = $(html).find("#hover_nodata");
        }
        return $div;
    }

}

/**
 * 返回字符串
 *isSpan: td换为span
 */
var getRecording_Td=function(accountId,tdName,type,isSpan){
	var tag="td";
	if(isSpan){
		tag="span";
	}
	if(type&&type=="fromTo"){
		var td='<'+tag+' id="from'+accountId[0]+'to'+accountId[1]+'" name="'+tdName+'">';
	}else{
		var td='<'+tag+' id="'+accountId+'" name="'+tdName+'">';
	}
	td+='<a style="cursor:not-allowed;text-decoration:none;" target="_self">\
			<span class="badge badge-warning" title="匹配中" name="mapping">0</span>\
		</a>\
		<a style="cursor:not-allowed;text-decoration:none;" target="_self">\
			<span class="badge badge-success" title="已匹配" name="mapped">0</span>\
		</a>\
		<a style="cursor:not-allowed;text-decoration:none;" target="_self">\
			<span class="badge badge-inverse" title="已驳回" name="cancel">0</span>\
		</a>';
	if(tdName=='incomeDetail'){
		td+='<span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId="'+accountId+'"></span>';
	}
	if(tdName=='outDetail'){
		td+='<span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId="'+accountId+'"></span>';
	}
	td+='</'+tag+'>';
	return td;
}

/**
 * 账号管理 转入转出记录 accountIdArray: [1,2,3] tdName:
 * 调用公用返回字符串方法getRecording_Td时传入的tdName categoryArray:
 * [accountStatInOutCategoryIn] permission_key: 'IncomeAuditComp:EncashLog:*'
 * formOrTo: 'toAccountId' 'fromAccountId'(默认)
 */
var loadInOutTransfer=function(accountIdArray,tdName,categoryArray,permission_key,formOrTo,income){
	var permissionStr="";
	// 默认取值 1 内部转账 In(0), OutTranfer(1), OutMember(2);
	if(!categoryArray){
		categoryArray=[accountStatInOutCategoryOutTranfer];
	}
	if(permission_key){
		permissionStr=" class='contentRight' contentright='"+permission_key+"' ";
	}
	if(!formOrTo){
		formOrTo='fromAccountId';
	}
	$.ajax({
		type:"POST",
		dataType:'JSON',
		url:'/r/account/findStatInOut',
		async:true,
		data:{
			'categoryArray':categoryArray.toString(),
			'accountIdArray':accountIdArray.toString()
		},
		success:function(jsonObject){
	        if(jsonObject.status == 1&&jsonObject.data){
	        	$.each(jsonObject.data[categoryArray[0]],function(index,result){
	        		var tr="";
	        		//if(result.mapping==0){
	        		//	tr+="<a disabled='true' style='cursor:not-allowed;text-decoration:none;'>" +
	        		//			"<span class='badge badge-warning' title='匹配中'>"+result.mapping+"</span>"+
	        		//		"</a>&nbsp;";
	        		//}else if(income){
					//	tr+="<a "+permissionStr+" href='#/EncashStatus4TransferIncome:*?"+formOrTo+"="+result.id+"&incomeReqStatus=0' style='text-decoration:none;'>" +
					//		"<span class='badge badge-warning' title='匹配中'>"+result.mapping+"</span>"+
					//		"</a>&nbsp;";
	        		//}else{
					//	tr+="<a "+permissionStr+" href='#/EncashStatus4Transfer:*?"+formOrTo+"="+result.id+"&incomeReqStatus=0' style='text-decoration:none;'>" +
					//		"<span class='badge badge-warning' title='匹配中'>"+result.mapping+"</span>"+
					//		"</a>&nbsp;";
					//}
					if(income){
						tr+="<a "+permissionStr+" href='#/EncashStatus4TransferIncome:*?"+formOrTo+"="+result.id+"&incomeReqStatus=0' style='text-decoration:none;'>" +
							"<span class='badge badge-warning' title='匹配中'>"+result.mapping+"</span>"+
							"</a>&nbsp;";
					}else{
						tr+="<a "+permissionStr+" href='#/EncashStatus4Transfer:*?"+formOrTo+"="+result.id+"&incomeReqStatus=0' style='text-decoration:none;'>" +
							"<span class='badge badge-warning' title='匹配中'>"+result.mapping+"</span>"+
							"</a>&nbsp;";
					}
					if(income){
						tr+="<a "+permissionStr+" href='#/EncashStatus4TransferIncome:*?"+formOrTo+"="+result.id+"&incomeReqStatus=1' style='text-decoration:none;'>" +
							"<span class='badge badge-success' title='已匹配'>"+result.mapped+"</span>" +
							"</a>&nbsp;";
					}else{
						tr+="<a "+permissionStr+" href='#/EncashStatus4Transfer:*?"+formOrTo+"="+result.id+"&incomeReqStatus=1' style='text-decoration:none;'>" +
							"<span class='badge badge-success' title='已匹配'>"+result.mapped+"</span>" +
							"</a>&nbsp;";
					}
	        		//if(result.mapped==0){
	        		//	tr+="<a disabled='true' style='cursor:not-allowed;text-decoration:none;'>" +
	        		//			"<span class='badge badge-success' title='已匹配'>"+result.mapped+"</span>" +
	        		//		"</a>&nbsp;";
	        		//}else if(income){
	        		//	tr+="<a "+permissionStr+" href='#/EncashStatus4TransferIncome:*?"+formOrTo+"="+result.id+"&incomeReqStatus=1' style='text-decoration:none;'>" +
	        		//			"<span class='badge badge-success' title='已匹配'>"+result.mapped+"</span>" +
	        		//		"</a>&nbsp;";
	        		//}else{
					//	tr+="<a "+permissionStr+" href='#/EncashStatus4Transfer:*?"+formOrTo+"="+result.id+"&incomeReqStatus=1' style='text-decoration:none;'>" +
					//		"<span class='badge badge-success' title='已匹配'>"+result.mapped+"</span>" +
					//		"</a>&nbsp;";
					//}
					if(income){
						tr+="<a disabled='true' style='cursor:not-allowed;text-decoration:none;'>" +
							"<span class='badge badge-inverse' title='已驳回'>"+result.cancel+"</span>"+
							"</a>&nbsp;";
					}else{
						tr+="<a "+permissionStr+" href='#/EncashStatus4Transfer:*?"+formOrTo+"="+result.id+"&incomeReqStatus=3' style='text-decoration:none;'>" +
							"<span class='badge badge-inverse' title='已驳回'>"+result.cancel+"</span>"+
							"</a>&nbsp;";
					}
	        		//if(result.cancel==0){
	        		//	tr+="<a disabled='true' style='cursor:not-allowed;text-decoration:none;'>" +
	        		//			"<span class='badge badge-inverse' title='已驳回'>"+result.cancel+"</span>"+
	        		//		"</a>&nbsp;";
	        		//}else if(income){
	        		//	tr+="<a "+permissionStr+" href='#/EncashStatus4TransferIncome:*?"+formOrTo+"="+result.id+"&incomeReqStatus=3' style='text-decoration:none;'>" +
	        		//			"<span class='badge badge-inverse' title='已驳回'>"+result.cancel+"</span>"+
	        		//		"</a>&nbsp;";
	        		//}else{
					//	tr+="<a "+permissionStr+" href='#/EncashStatus4Transfer:*?"+formOrTo+"="+result.id+"&incomeReqStatus=3' style='text-decoration:none;'>" +
					//		"<span class='badge badge-inverse' title='已驳回'>"+result.cancel+"</span>"+
					//		"</a>&nbsp;";
					//}
					if(tdName=='incomeDetail'){
						tr+='<span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId="'+result.id+'"></span>';
					}
					if(tdName=='Outdetail'){
						tr+='<span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId="'+result.id+'"></span>';
					}
	        		$("[id="+result.id+"][name="+tdName+"]").html(tr);
	        	});
				var matchingAmt = jsonObject.data.matchingAmt?jsonObject.data.matchingAmt:[];
				$.each(matchingAmt?matchingAmt:t,function(index,item){
					var $matchingAmtIn = $('span.matchingAmtIn[accountStatInOutId='+index+']').removeClass('text-success').removeClass('text-danger').text('');
					var $matchingAmtOut = $('span.matchingAmtOut[accountStatInOutId='+index+']').removeClass('text-success').removeClass('text-danger').text('');
					var $platAcked = $('span.platAcked[accountStatInOutId='+index+']').text('---');
					var $bankAcked = $('span.bankAcked[accountStatInOutId='+index+']').text('---');
					if(item[0]!=0){
						$matchingAmtIn.addClass('text-success').css({'font-weight':900}).text((item[0]+'元/')+(item[1]+'笔')).attr('title','转入未对账流水');
					}
					if(item[2]!=0){
						$matchingAmtOut.addClass('text-danger').css({'font-weight':900}).text((item[2]+'元/')+(item[3]+'笔')).attr('title','转出未对账流水');
					}
					if(item[4]!=0){
						$bankAcked.html((item[4]+'元</br>')+(item[5]+'笔'));
					}
					if(item[6]!=0){
						$platAcked.html((item[6]+'元</br>')+(item[7]+'笔'));
					}
				});
	        	if(permission_key){
	        		var objArray=new Array();
	        		objArray[permission_key]=function($currObj,hasRight){
				        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
				    }
					contentRight(objArray);
	        	}
	        }
	    }
	});
}

var loadEncashCheckAndStatus = function (categoryArray,accountIdArray,fromToArray,contentRight){
	categoryArray = categoryArray!=null?categoryArray.toString():null;
	accountIdArray = accountIdArray!=null?accountIdArray.toString():null;
	fromToArray = fromToArray!=null?fromToArray.toString():null;
	$.ajax({type:"POST",dataType:'JSON',url:'/r/account/findStatInOut',async:true,data:{categoryArray:categoryArray,accountIdArray:accountIdArray,fromToArray:fromToArray},success:function(jsonObject){
		if(jsonObject.status ==1 && jsonObject.data){
			var inArray = jsonObject.data[accountStatInOutCategoryIn],outTranferArray = jsonObject.data[accountStatInOutCategoryOutTranfer],outMemberArray = jsonObject.data[accountStatInOutCategoryOutMember],fromToArray=jsonObject.data[accountStatInOutCategoryFromTo],t= new Array();
			var matchingAmt = jsonObject.data.matchingAmt?jsonObject.data.matchingAmt:null;
			//var sysBalAndAlarm = jsonObject.data.sysBalAndAlarm?jsonObject.data.sysBalAndAlarm:null;

			$.each(inArray?inArray:t,function(index,item){
				var $a =  $('a[accountStatInOutCategory='+accountStatInOutCategoryIn+'][accountStatInOutId='+item.id+']');
				$a.each(function(){
					var t = $(this),statInOut= t.attr('accountStatInOut'),inReqStatus=statInOut=='mapping'?incomeRequestStatusMatching:(statInOut=='mapped'?incomeRequestStatusMatched:incomeRequestStatusCanceled);
					//item[statInOut]?t.attr('href','#/EncashCheck4Transfer:*?toAccountId='+item.id+'&incomeReqStatus='+inReqStatus).attr('style','text-decoration:none;').find('span').text(item[statInOut]):null;
					t.attr('href','#/EncashCheck4Transfer:*?toAccountId='+item.id+'&incomeReqStatus='+inReqStatus).attr('style','text-decoration:none;').find('span').text(item[statInOut]);
				});
			});
			$.each(outTranferArray?outTranferArray:t,function(index,item){
				var $a =  $('a[accountStatInOutCategory='+accountStatInOutCategoryOutTranfer+'][accountStatInOutId='+item.id+']');
				$a.each(function(){
					var t = $(this),statInOut= t.attr('accountStatInOut'),inReqStatus=statInOut=='mapping'?incomeRequestStatusMatching:(statInOut=='mapped'?incomeRequestStatusMatched:incomeRequestStatusCanceled);
					//item[statInOut]?t.attr('href','#/EncashStatus4Transfer:*?fromAccountId='+item.id+'&incomeReqStatus='+inReqStatus).attr('style','text-decoration:none;').find('span').text(item[statInOut]):null;
					t.attr('href','#/EncashStatus4Transfer:*?fromAccountId='+item.id+'&incomeReqStatus='+inReqStatus).attr('style','text-decoration:none;').find('span').text(item[statInOut]);
				});
			});
			$.each(outMemberArray?outMemberArray:t,function(index,item){
				var $a =  $('a[accountStatInOutCategory='+accountStatInOutCategoryOutMember+'][accountStatInOutId='+item.id+']');
				$a.each(function(){
					var t = $(this),statInOut= t.attr('accountStatInOut'),outTaskStatus=statInOut=='mapping'?outwardTaskStatusDeposited:(statInOut=='mapped'?outwardTaskStatusMatched:outwardTaskStatusFailure);
					//item[statInOut]?t.attr('href','#/EncashCheck4Outward:*?fromAccountId='+item.id+'&outwardTaskStatus='+outTaskStatus).attr('style','text-decoration:none;').find('span').text(item[statInOut]):null;
					t.attr('href','#/EncashCheck4Outward:*?fromAccountId='+item.id+'&outwardTaskStatus='+outTaskStatus).attr('style','text-decoration:none;').find('span').text(item[statInOut]);
				});
			});
			$.each(fromToArray?fromToArray:t,function(index,item){
				var $a =  $('a[accountStatInOutCategory='+accountStatInOutCategoryFromTo+'][accountStatInOutId="'+item[accountStatInOutCategoryFromTo]+'"]'),fromTo=item[accountStatInOutCategoryFromTo].split(':');
				$a.each(function(){
					var t = $(this),statInOut= t.attr('accountStatInOut'),inReqStatus=statInOut=='mapping'?incomeRequestStatusMatching:(statInOut=='mapped'?incomeRequestStatusMatched:incomeRequestStatusCanceled);
					//item[statInOut]?t.attr('href','#/EncashCheck4Transfer:*?fromAccountId='+fromTo[0]+'&toAccountId='+fromTo[1]+'&incomeReqStatus='+inReqStatus).attr('style','text-decoration:none;').find('span').text(item[statInOut]):null;
					t.attr('href','#/EncashCheck4Transfer:*?fromAccountId='+fromTo[0]+'&toAccountId='+fromTo[1]+'&incomeReqStatus='+inReqStatus).attr('style','text-decoration:none;').find('span').text(item[statInOut]);
				});
			});
			$.each(matchingAmt?matchingAmt:t,function(index,item){
				var $matchingAmtIn = $('span.matchingAmtIn[accountStatInOutId='+index+']').removeClass('text-success').removeClass('text-danger').text('');
				var $matchingAmtOut = $('span.matchingAmtOut[accountStatInOutId='+index+']').removeClass('text-success').removeClass('text-danger').text('');
				var $platAcked = $('span.platAcked[accountStatInOutId='+index+']').text('---');
				var $bankAcked = $('span.bankAcked[accountStatInOutId='+index+']').text('---');
				if(item[0]!=0){
					$matchingAmtIn.addClass('text-success').css({'font-weight':900}).text((item[0]+'元/')+(item[1]+'笔')).attr('title','转入未对账流水');
				}
				if(item[2]!=0){
					$matchingAmtOut.addClass('text-danger').css({'font-weight':900}).text((item[2]+'元/')+(item[3]+'笔')).attr('title','转出未对账流水');
				}
				if(item[4]!=0){
					$bankAcked.html((item[4]+'元</br>')+(item[5]+'笔'));
				}
				if(item[6]!=0){
					$platAcked.html((item[6]+'元</br>')+(item[7]+'笔'));
				}
			});
			//$.each(sysBalAndAlarm?sysBalAndAlarm:t,function(index,item){
			//	var $sysBal = $('div.SysLogEvent[target='+index+']').find('span.amount');
			//	if(item[1]){
			//		$sysBal.text(item[0]).addClass('red').prop({'title':'系统余额告警'});
			//	}else{
			//		$sysBal.text(item[0]).removeClass('red').prop({'title':''});
			//	}
			//});
			contentRight?contentRight():null;
		}
	}});
}


/**
 * 初始化是否自动刷新当前页面控件 $div:控件位置 $clickBtn:刷新页面的点击按钮 inputId:隐藏标识符 找不到input 销毁所有定时任务
 */
var initRefreshSelect=function($div,$clickBtnArray,widthPX,inputId){
	var selectStart='<select class="chosen-select" name="refreshSelect" >';
	if(widthPX){
		selectStart='<select class="chosen-select" style="width:'+widthPX+'px" name="refreshSelect" >';
	}
	var refreshSelectInterval,findInput,selectHTML=selectStart+
					'<option value="" selected="selected" >不刷新</option>\
					<option value="15" >15秒</option>\
					<option value="30" >30秒</option>\
					<option value="60" >60秒</option>\
					<option value="120" >120秒</option>\
					<option value="180" >180秒</option>\
				</select>';
	$div.attr("style","display:inline");
	$div.html(selectHTML);
	$select=$div.find("[name=refreshSelect]");
	$select.unbind("change");
	$select.bind("change",function(){
		var second=$select.val();
		if(second&&second>0){
			clearInterval(refreshSelectInterval);
			clearInterval(findInput);
			refreshSelectInterval=setInterval(function(){
				if($clickBtnArray.length==1){
					$clickBtnArray.click();
				}else{
					$.each($clickBtnArray,function(index,result){
						result.click();
					})
				}
			},second*1000);
			findInput=setInterval(function(){
				// 执行查找input隐藏域 找不到说明已离开页面，清理定时器
				if(!$("#"+inputId).length||$("#"+inputId).length<=0){
					clearInterval(refreshSelectInterval);
					clearInterval(findInput);
				}
			},500);
		}else{
			clearInterval(refreshSelectInterval);
			clearInterval(findInput);
		}
	});
	
}


/**
 * 耗时 匹配创建时间-入款申请时间 其它计算也可以通用
 */
var timeDifferenceForMatch=function(transactionTime,incomeTime){
	var timeDifference=transactionTime;
	if(!transactionTime){
		// 应对测试环境数据异常
		return '0秒';
	}
	if(incomeTime){
		timeDifference=Math.abs(transactionTime-incomeTime);
	}
	var days	= timeDifference / 1000 / 60 / 60 / 24;
	var daysRound	= Math.floor(days);
	var hours	= timeDifference/ 1000 / 60 / 60 - (24 * daysRound);
	var hoursRound	= Math.floor(hours);
	var minutes	= timeDifference / 1000 /60 - (24 * 60 * daysRound) - (60 * hoursRound);
	var minutesRound= Math.floor(minutes);
	var seconds	= timeDifference/ 1000 - (24 * 60 * 60 * daysRound) - (60 * 60 * hoursRound) - (60 * minutesRound);
	if(daysRound>0){
		return daysRound + "天" + hoursRound + "时" + minutesRound + "分" + seconds + "秒";
	}else{
		if(hoursRound>0){
			return hoursRound + "时" + minutesRound + "分" + seconds + "秒";
		}else{
			if(minutesRound>0){
				return minutesRound + "分" + seconds + "秒";
			}else{
				if(seconds>0){
					return seconds + "秒";
				}else{
					return "1秒";
				}
			}
		}
	}
}

/**
 * 获取余额告警标志
 */
var getlimitBalanceIconStr=function(account){
	if(!account) return;
	var limitBalance=account.limitBalance?account.limitBalance:sysSetting.FINANCE_ACCOUNT_BALANCE_ALARM;
	if(account.bankBalance*1>=limitBalance*1){
		return "<i title='已达到或超过余额告警值:"+limitBalance+"' class='ace-icon fa fa-exclamation-triangle bigger-130 red' style='cursor:pointer;'></i>";
	}else{
		return "";
	}
}

/**
 * 选中双选时间控件后，清空查询范围,选中查询范围后，清空时间控件 并查询 $picker ：双选时间控件选择器 radioName：单选按钮名
 * $searchBtn：选填，清空后查询按钮
 */
var choiceTimeClearAndSearch=function($picker,radioName,$searchBtn){
	$picker.on('apply.daterangepicker', function(ev, picker) {
		$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
		// 确定的时候把单选按钮的checked清空
		$("[name="+radioName+"]:checked").prop("checked",false);
		// 清空的同时查询数据
		if($searchBtn){
			$searchBtn.click();
		}
	});	
	$("[name="+radioName+"]").click(function(){
		$picker.val("");
		// 清空的同时查询数据
		if($searchBtn){
			$searchBtn.click();
		}
	});
}

var getHTMLremark=function(remark,width,colorred){
	if(!width){
		width=100;
	}
	if(colorred)
	 	return "<a style='width:"+width+"px;color:red; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+remark+"'>"+(remark?remark:'')+"</a>";
	else
		return "<a style='width:"+width+"px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+remark+"'>"+(remark?remark:'')+"</a>";
}
/**
 * 根据record获取银行流水状态HTML
 */
var getHTMLBankLogStatus=function(record,sysDate){
	if(!record) return "";
	if(!sysDate){
		sysDate=new Date();
	}
	// 小时数大于系统设置未认领时间时，为未认领
	if(record.status==bankLogStatusMatching&&((sysDate.getTime()-record.tradingTime)/1000/60/60)>noOwnerBankOverTime){
		record.status=bankLogStatusNoOwner;
		record.statusStr="未认领";
	}
	var classStr="";
	if(record.status==bankLogStatusMatching){
		classStr="warning";
	}else if(record.status==bankLogStatusMatched){
		classStr="success";
	}else if(record.status==bankLogStatusNoOwner){
		classStr="inverse";
	}else if(record.status==bankLogStatusDisposed){
		classStr="info";
	}else if(record.status==bankLogStatusFee){
		classStr="purple";
	}else{
		classStr="pink";
	}
	if(record.status==bankLogStatusMatched){
		// 已匹配
		return"<a class='bind_hover_card' data-toggle='InOutInfoHover"+record.id+"' data-placement='auto left' data-trigger='click' >\
					<span class='label label-sm label-"+classStr+"'>已匹配</span>\
				</a>";
	}else{
		return "<span class='label label-sm label-"+classStr+"'>"+record.statusStr+"</span>";
	}
}

/**
 * 银行流水状态修改 弹出框
 */
var showModal_DisposedFee=function(bankLogId,fnName){
	// 转为已处理或手续费弹出框
	var modalHtml='<div id="modal_changeBankLog" class="modal fade">\
						<div class="modal-dialog modal-middle">\
							<div class="modal-content">\
								<div class="modal-header text-center no-padding">\
									<div class="table-header">\
										<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button>\
										<span >银行流水处理</span>\
									</div>\
								</div>\
								<div class="modal-body no-padding control-group">\
									<i class="fa fa-asterisk red" aria-hidden="true"></i><label >类型：</label>\
									<div class="radio" style="display:inline;">\
										<label>\
											<input name="bankLogStatus" type="radio" class="ace" value="4">\
											<span class="lbl">已处理</span>\
										</label>\
										<label>\
											<input name="bankLogStatus" type="radio" class="ace" value="5">\
											<span class="lbl">手续费</span>\
										</label>\
									</div>\
									<div>\
										<i class="fa fa-asterisk red" aria-hidden="true"></i><label >备注：</label>\
										<textArea style="width: 500px" name="bankLogRemark" type="text" ></textArea>\
									</div>\
								</div>\
								<div class="modal-footer">\
									<button class="btn btn-primary" type="button"\
											onclick="do_DisposedFee('+bankLogId+');">确定</button>\
									<button class="btn btn-danger" type="button" data-dismiss="modal">取消</button>\
								</div>\
							</div>\
						</div>\
					</div>';
	
	var $div=$(modalHtml).appendTo($("body"));
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		// 关闭窗口清除model
		$div.remove();
		if(fnName){
			fnName();
		}
	});
}

/**
 * 银行流水状态修改 已处理或手续费
 */
var do_DisposedFee=function(bankLogId,fnName){
	var $div=$("#modal_changeBankLog");
	// 非空校验
	var $bankLogStatus=$div.find("[name=bankLogStatus]:checked");
	var $remark=$div.find("[name=bankLogRemark]");
	if($bankLogStatus.length!=1){
		showMessageForCheck("类型不能为空！");
		return;
	}
	if(!validateEmpty($remark,"备注")){
		return;
	}
	bootbox.confirm("确定处理此条流水?", function(result) {
		if (result) {
			$.ajax({
		        type: 'POST',
		        url: "/r/banklog/doDisposedFee",
		        data:{
		        	"bankLogId":bankLogId,
		        	"status":$bankLogStatus.val(),
		        	"remark":$.trim($remark.val())
		        },
		        dataType: 'json',
		        success: function (jsonObject) {
	        		if(jsonObject.status==1){
	        			showMessageForSuccess("修改成功");
	        			$div.modal("toggle");
	        		}else{
	        			showMessageForFail("修改失败");
	        		}
		        }
			});
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

/**
 * 账号长度大于10则显示前后4位，否则全部显示
 */
var hideAccount=function(account){
	if (account.length>10) {
		account = account.substring(0,4)+'**'+account.substring(account.length-4);
    }
	return account;
}

/**
 * 给所有账号加上星号
 * isPhoneNumber 是手机号 前2后4
 */
var hideAccountAll=function(account,isPhoneNumber){
    if(account){
    	account=account+"";//部分数字会报错，转化为字符串
    	account=account.trim();
    	if(isPhoneNumber){
    		account = account.substring(0,3)+'**'+account.substring(account.length-3);
    	}else{
            if (account.length>=8) {
                account = account.substring(0,4)+'****'+account.substring(account.length-4);
            }else if(account.length>=4){
                account = account.substring(0,2)+'********'+account.substring(account.length-2);
            }else if(account.length>=2){
                account = account.substring(0,1)+'**********'+account.substring(account.length-1);
            }
    	}
    }else{
        account = '';
    }
	return account;
}
/**
 * 给姓名的姓加星号
 */
var hideName=function(name){
	if(name){
		name=name.trim();
		//开户人新规则
		if(name.length>=3){//ABC——>A*C ; ABCD(四个字或以上）——>A*CD
			name=name.substring(0,1)+'*'+name.substring(2,name.length);
		}else{//AB——>*B
			name=name.substring(0,0)+'*'+name.substring(1,name.length);
		}
	}else{
		name='';
	}
	return name
}

/**
 * 自动生成options节点 list 例：var monitor_accountType =
 * [{id:1,msg:'入款银行卡'},{id:4,msg:'入款第三方'}] value 选中的实际值（id）
 */
var getOptionList=function(list,value){
	var options="<option value='' selected='selected'>全部</option>";
	$(list).each(function(index,record){
		if(value&&record.id==value){
			options+="<option value='"+record.id+"' selected='selected'>"+record.msg+"</option>";
		}else{
			options+="<option value='"+record.id+"' >"+record.msg+"</option>";
		}
	});
	return options;
}

/**
 * 获取当前账号拥有权限的盘口
 */
var getHandicapByPerm=function($handicap_selected){
	$handicap_selected.html('');
	$.ajax({
		type:"get",
		async:false,
		url:API.r_handicap_findByPerm,
		data:{enabled:1},
		dataType:'json',
		success:function(jsonObject){
		    if(jsonObject.status == 1){
		        $('<option></option>').html('全部').attr('value','').attr('selected','selected').attr("handicapCode","").appendTo($handicap_selected);
		        for(var index in jsonObject.data){
		            var item = jsonObject.data[index];
		            $('<option></option>').html(item.name).attr('value',item.id).attr("handicapCode",item.code).appendTo($handicap_selected);
		        }
		    }
		}
	});
}

var getBanklogStatusOptionList=function(width){
	var list=[
		{id:1,msg:"已匹配"},
		{id:0,msg:"匹配中"},
		{id:3,msg:"未认领"},
		{id:4,msg:"已处理"},
		{id:5,msg:"手续费"},
		{id:6,msg:"冲正，未处理"},
		{id:7,msg:"冲正，已处理"},
		{id:8,msg:"利息/结息"},
		{id:9,msg:"亏损-人工"},
		{id:10,msg:"亏损-系统"},
		{id:11,msg:"亏损-其他"},
		{id:12,msg:"外部资金"}
		];
	return getOptionList(list);
}

/**
 * sync:同步到平台true
 */
var showChangeStatusModal=function(accountId,callBack,sync){
	//不调平台
	sync=false;
	var accountInfo = getAccountInfoById(accountId);
	if(!accountInfo) return;
	var buttons={};
	var btnNormal={"label" : "在用","className" : "btn btn-sm btn-success","callback": function() {
			var remark=$("[name=freezeModal_remark]").val();
			// 恢复不强制写备注 但是如果写了 就要按字数来
			if(remark&&(remark.length<5||remark.length>100)){
				showMessageForFail("请输入备注，5-100字之间");
				return false;
			}
			$.ajax({ dataType:'json',type:"get", url:API.r_account_asin4OutwardAccount, data:{ "accountId":accountId,"remark":remark}, success:function(jsonObject){
				if(jsonObject.status == 1 ){
					showMessageForSuccess("修改成功");
					if(sync){
						updateStatus_sync(accountId,accountStatusNormal,accountInfo);
					}
					if(callBack) callBack();
				}else{
					showMessageForFail(jsonObject.message);
				}
			}});
		}
	};
	var btnStop={"label" : "停用","className" : "btn btn-sm btn-primary","callback": function() {
			var remark=$("[name=freezeModal_remark]").val();
			// 停用不强制写备注 但是如果写了 就要按字数来
			if(remark&&(remark.length<5||remark.length>100)){
				showMessageForFail("请输入备注，5-100字之间");
				return false;
			}
			$.ajax({ dataType:'json',type:"get", url:API.r_account_toStopTemp, data:{ "accountId":accountId,"remark":remark}, success:function(jsonObject){
				if(jsonObject.status == 1){
					showMessageForSuccess("本系统状态修改成功");
					if(sync){
						updateStatus_sync(accountId,accountStatusStopTemp,accountInfo);
					}
					if(callBack) callBack();
				}else{
					showMessageForFail(jsonObject.message);
				}
			}});
		}
	};
	var freeze={"label" : "冻结","className" : "btn btn-sm btn-danger", "callback": function() {
			var remark=$("[name=freezeModal_remark]").val();
			if(!remark||remark.length<5||remark.length>100){
				showMessageForFail("请输入备注，5-100字之间");
				return false;
			}
			$.ajax({dataType:'json',type:"get", url:API.r_account_toFreezeForver,data:{"accountId":accountId,"remark":remark },success:function(jsonObject){
				if(jsonObject.status == 1){
					showMessageForSuccess("本系统状态修改成功");
					if(sync){
						updateStatus_sync(accountId,accountStatusFreeze,accountInfo);
					}
					if(callBack) callBack();
				}else{
					showMessageForFail(jsonObject.message);
				}
			}});
		}
	};
	var canChangeNormal=true;//可用改为在用
	if(accountInfo.flag&&accountInfo.flag*1==2){
    	//返利网账号（flag=2) 余额峰值(保证金)小于等于0 不可以修改状态到在用
		if(accountInfo.peakBalance<=0){
			canChangeNormal=false;
		}
    }
	if(accountInfo.status==accountStatusNormal){
		//在用 改停用
		buttons.btnStop=btnStop;
	}else if(accountInfo.status==accountStatusStopTemp){
		//停用 改在用 (银行入款卡 subType=0不允许修改为在用)
		if(canChangeNormal){
			buttons.btnNormal=btnNormal;
		}
	}else if(accountInfo.status==accountStatusEnabled){
		if(canChangeNormal){
			buttons.btnNormal=btnNormal;
		}
		buttons.btnStop=btnStop;
	}
	if(accountInfo.status!=accountStatusFreeze){
		//不是冻结状态都可以改冻结
		buttons.freeze=freeze;
	}
	buttons.cancel={"label" : "取消","className" : "btn btn-sm btn-default"};
	bootbox.dialog({
		title:		"状态修改",
		message: 	"<span class='bigger-110'>" +
						"<br/>" +
						"<label class='control-label bolder blue'>账号：&nbsp;&nbsp;&nbsp;&nbsp;</label>"
						+hideAccountAll(accountInfo.account)+"&nbsp;&nbsp;&nbsp;&nbsp;" +
						"<label class='control-label bolder blue'>开户行：&nbsp;&nbsp;&nbsp;&nbsp;</label>"
						+accountInfo.bankName+
					"</span>" +
					"<br/>" +
					"<label class='control-label bolder blue'>备注：&nbsp;&nbsp;&nbsp;&nbsp;</label>" +
					"<input name='freezeModal_remark' style='height:32px;width:400px;'class='input-medium' placeholder='改为冻结请输入备注（5-100字）' >" +
					"<br/><br/><span class='red bolder' >注：返利网账号，保证金大于0时，可转在用或修改限额&nbsp;&nbsp;</span><br/>",
		buttons:buttons
	});
}

/**
 * 新增层级时需要使用的插件
 */
var choiceLevel_match=function($handicapEle,$levelEle,$levelEle_width){
// getHandicap_Level();
	var options_handicap='<option value="" selected="selected">全部</option>';
	handicap_list_all.map(function(handicap){
		options_handicap+='<option value="'+handicap.id+'" >'+handicap.name+'</option>';
	});
	var options_level=['<option value="" selected="selected">请先选择盘口</option>'];
	multiselect_html($levelEle,options_level,$levelEle_width);
	$handicapEle.html(options_handicap).change(function(){
		options_level=[];
		level_list.map(function(levels){
			// 获取对应层级列表
			if(levels&&levels.length>0&&levels[0].handicapId&&levels[0].handicapId==$handicapEle.val()){
				levels.map(function(level){
					options_level.push('<option value="'+level.id+'" >'+level.name+'</option>');
				});
			}
		});
		if(options_level.length==0){
			options_level.push('<option value="" >当前盘口无层级</option>');
		}
		$levelEle.multiselect("destroy");
		multiselect_html($levelEle,options_level,$levelEle_width);
	});
}

var multiselect_html=function($multiselect,optionsHTML,width){
	$multiselect.html(optionsHTML.join('')).multiselect({
		enableFiltering: true,
		enableHTML: true,
		nonSelectedText :'----全部----',
		nSelectedText :'已选中',
		buttonClass: 'btn btn-white btn-primary',
		buttonWidth: width?width:'200px',
		templates: {
			button: '<button type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>',
			ul: '<ul class="multiselect-container dropdown-menu"></ul>',
			filter: '<li class="multiselect-item filter"><div class="input-group"><span class="input-group-addon"><i class="fa fa-search"></i></span><input class="form-control multiselect-search" type="text"></div></li>',
			filterClearBtn: '<span class="input-group-btn"><button class="btn btn-default btn-white btn-grey multiselect-clear-filter" type="button"><i class="fa fa-times-circle red2"></i></button></span>',
			li: '<li><a tabindex="0"><label></label></a></li>',
			divider: '<li class="multiselect-item divider"></li>',
			liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
		}
	});
}
/**
 * 根据盘口获取当前层级列表
 */
var getLevelByHandicapId=function(handicapId){
	if(!handicapId){
		return;
	}
	var curr_level_list;
	$.each(level_list,function(index,record){
		if(record&&record.length>0&&record[0].handicapId==handicapId){
			curr_level_list=record;
		}
	});
	return curr_level_list;
}

/**
 * 获取与平台同步设置信息
 */
var getAccountSync=function(accountId){
	var result;
	$.ajax({
		type:"PUT",
		dataType:'JSON',
		url:'/r/accountSync/getByAccountId',
		async:false,
		data:{
			"accountId":accountId
		},
		success:function(jsonObject){
			if(jsonObject&&jsonObject.status==1){
				result=jsonObject.data
			}
		}
	});
	return result;
}

/**
 * 时间转换为格式 例如5/9 15:57:31
 */
var geeTime4Crawl=function(lastTime){
	 if(!lastTime){
		return '';
	 }
	 //全系统流水时间更换成此格式
	 var yyyyMMddHHmmss=timeStamp2yyyyMMddHHmmss(lastTime);
	 if(yyyyMMddHHmmss&&yyyyMMddHHmmss.length>12){
		var times=yyyyMMddHHmmss.split(" ");
		if(times&&times.length>1){
			return times[1];
		}else{
			return '';
		}
	 }else{
		 return '';
	 }
	 
	 //以下是 如5/9 15:57:31
	 var time = new Date();
     time.setTime(lastTime);
     var mon = time.getMonth(),day = time.getDate(),hor = time.getHours(),min = time.getMinutes(),sec = time.getSeconds();
     hor = (hor&&hor>0&&hor<=9)?('0'+hor):(hor==0?'00':hor);
     min = (min&&min>0&&min<=9)?('0'+min):(min==0?'00':min);
     sec = (sec&&sec>0&&sec<=9)?('0'+sec):(sec==0?'00':sec);
     return  (mon+1) + '/' + day + ' ' + hor + ':' + min + ':' + sec;
   
}

/** 装载所有盘口以code为key */
var getHandicapBatchInfoByCode=function(){
	var result={};
	$.map(handicap_list_all,function(record){
		result[record.code]=record;
	});
	return result;
}

/**
 * 展示修改模态窗 查看账号操作记录
 */
var showModal_accountExtra=function(accountId){
	var accountInfo=getAccountInfoById(accountId);
	if(!accountInfo){
		showMessageForFail("操作失败：账号不存在");
		return;
	}
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#accountExtraListModal").clone().appendTo($("body"));
			initTimePicker();
			// 表单填充值
			$div.find("#accountId").val(accountId);
			$div.find("[name='alias']").text(accountInfo.alias?accountInfo.alias:'');
		    $div.find("[name='account']").text(hideAccountAll(accountInfo.account));
		    $div.find("[name='owner']").text(accountInfo.owner?hideName(accountInfo.owner):'');
		    // 查询数据
		    accountExtraList(0);
			$div.modal("toggle");
			$div.keypress(function(e){
				if(event.keyCode == 13) {
					$div.find("#searchBtn button").click();
				}
			});
			$div.on('hidden.bs.modal', function () {
				// 关闭窗口清除model
				$div.remove();
			});
		}
	});
}
var accountExtraList=function(CurPage){
	var $div = $("#accountExtraListModal");
    if(!!!CurPage&&CurPage!=0) CurPage=$("#accountExtraListPage .Current_Page").text();
	 $.ajax({
		dataType : 'JSON',
		type : "POST",
		async:false,
		url : "/r/accountExtra/findAll",
		data : {
			"startAndEndTimeToArray" :getTimeArray($div.find("[name=startAndEndTime]").val()).toString(),
			"operator" :$.trim($div.find("#operator").val(),true),
			"accountId" : $div.find("#accountId").val(),
			"pageSize":$.session.get('initPageSize'),
			"pageNo" : CurPage<=0?0:CurPage-1
		},
		success : function(jsonObject) {
			if(-1==jsonObject.status){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var $tbody=$div.find("tbody");
			var trStr="";
			$.map(jsonObject.data,function(record){
				trStr+="<tr>";
				trStr+="<td><span>"+record.operator+"</span></td>";
				trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.time)+"</span></td>";
				trStr+="<td>" +
							"<a style='width:380px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+record.remark+"'>"+
								(record.remark?record.remark:'')+
							"</a>" +
						"</td>";
				trStr+="</tr>";
			});
			$tbody.html(trStr);
			showPading(jsonObject.page,"accountExtraListPage",accountExtraList,null,true);
		}
	 });
}



/**
 * 展示修改模态窗 查看下发卡点击记录
 */
var showModal_accountClick=function(accountId){
	var accountInfo=getAccountInfoById(accountId);
	if(!accountInfo){
		showMessageForFail("操作失败：账号不存在");
		return;
	}
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#AccountClickListModal").clone().appendTo($("body"));
			initTimePicker();
			// 表单填充值
			$div.find("#accountId").val(accountId);
			$div.find("[name='alias']").text(accountInfo.alias?accountInfo.alias:'');
		    $div.find("[name='account']").text(hideAccountAll(accountInfo.account));
		    $div.find("[name='owner']").text(accountInfo.owner?hideName(accountInfo.owner):'');
		    // 查询数据
		    accountClickList(0);
			$div.modal("toggle");
			$div.keypress(function(e){
				if(event.keyCode == 13) {
					$div.find("#searchBtn button").click();
				}
			});
			$div.on('hidden.bs.modal', function () {
				// 关闭窗口清除model
				$div.remove();
			});
		}
	});
}
var accountClickList=function(CurPage){
	var $div = $("#AccountClickListModal");
    if(!!!CurPage&&CurPage!=0) CurPage=$("#accountClickListPage .Current_Page").text();
	 $.ajax({
		dataType : 'JSON',
		type : "POST",
		async:false,
		url : "/r/accountClick/list",
		data : {
			"startAndEndTimeToArray" :getTimeArray($div.find("[name=startAndEndTime]").val()).toString(),
			"operator" :$.trim($div.find("#operator").val(),true),
			"accountId" : $div.find("#accountId").val(),
			"pageSize":$.session.get('initPageSize'),
			"pageNo" : CurPage<=0?0:CurPage-1
		},
		success : function(jsonObject) {
			if(-1==jsonObject.status){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var $tbody=$div.find("tbody");
			var trStr="";
			$.map(jsonObject.data,function(record){
				trStr+="<tr>";
				trStr+="<td><span>"+record.operator+"</span></td>";
				trStr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.time)+"</span></td>";
				trStr+="<td>" +
							"<a style='width:380px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+record.remark+"'>"+
								(record.remark?record.remark:'')+
							"</a>" +
						"</td>";
				trStr+="</tr>";
			});
			$tbody.html(trStr);
			showPading(jsonObject.page,"accountClickListPage",accountClickList,null,true);
		}
	 });
}


function changeRateTyle(type){
	if(type==0){
		$("#updateIncomeAccount").find(".fixed").show();
		$("#updateIncomeAccount").find(".ladder").hide();
	}else{
		$("#updateIncomeAccount").find(".fixed").hide();
		$("#updateIncomeAccount").find(".ladder").show();
	}
}

function changeComputingMethod(type){
	var $div=$("#updateIncomeAccount");
	var charging=$div.find("input[name='charging']:checked").val();
	if(type==0){
		$("#downRateTr").show();
		$("#chargingTr").hide();
		$("#ladderTr").hide();
		$("#showList").hide();
	}else{
		$("#downRateTr").hide();
		$("#chargingTr").show();
		$("#ladderTr").show();
		$("#showList").show();
	}
	if(charging==1){
		$("#showMessage").html("元");
	}else{
		$("#showMessage").html("%");
	}
}

function changeCharging(type){
	if(type==0){
		$("#showMessage").html("元");
	}else{
		$("#showMessage").html("%");
	}
}

function addResultRow(startAmount,endAmount,rate){
	for(var i=1;i<=4;i++){
		if(checkedRow(i)){
			//执行新增
			  var addContent="<tr id='feeTr"+i+"'><td style='padding-top: 10px;' class='ladder'>金额</td>"
				+"<td style='padding-top: 10px;' class='ladder'><input onblur='intersection(this,"+i+")' style='width: 70px;' onkeyup='clearNoNum(this)' value='"+startAmount+"' name='startAmount' type='text'>~<input onblur='intersection(this,"+i+")' onkeyup='clearNoNum(this)' value='"+endAmount+"' name='endAmount' style='width: 70px;' type='text'></td>"
				+"<td style='padding-top: 10px;' class='ladder'>费率(百分比)</td>"
				+"<td style='padding-top: 10px;' class='ladder'><input onkeyup='clearNoNum(this)' type='text' value='"+rate+"' name='rates' value='3'>%<a onclick='removeRow("+i+")'  style='cursor:pointer;' class='ace-icon glyphicon glyphicon-minus'></a></td></tr>";
			    $("#rateTbody").append($(addContent));
			  break;
		}
	}
}

function addRow(){
	if($("#updateIncomeAccount").find("input[name='rates']").length>=5){
		 $.gritter.add({
             time: '500',
             class_name: '',
             title: '系统消息',
             text: "最多添加5行费率！",
             sticky: false,
             image: '../images/message.png'
         });
		return;
	}
	for(var i=1;i<=4;i++){
		if(checkedRow(i)){
			//执行新增
			  var addContent="<tr id='feeTr"+i+"'><td style='padding-top: 10px;' class='ladder'>金额</td>"
				+"<td style='padding-top: 10px;' class='ladder'><input onblur='intersection(this,"+i+")' onkeyup='clearNoNum(this)' style='width: 70px;' name='startAmount' type='text'>~<input onblur='intersection(this,"+i+")' onkeyup='clearNoNum(this)' name='endAmount' style='width: 70px;' type='text'></td>"
				+"<td style='padding-top: 10px;' class='ladder'>费率(百分比)</td>"
				+"<td style='padding-top: 10px;' class='ladder'><input onkeyup='clearNoNum(this)' type='text' name='rates' value='3'>%<a onclick='removeRow("+i+")'  style='cursor:pointer;' class='ace-icon glyphicon glyphicon-minus'></a></td></tr>";
			    $("#rateTbody").append($(addContent));
			  break;
		}
	}
  
}
//是否可以新增此行号
var checkedRow=function(row){
	if($("#feeTr"+row).length==1){
		return false;
	}else{
		return true;
	}
}
//移除行
function removeRow(row){
	$("#feeTr"+row).remove();
}

var accountInactivatedAdd="账号现处于未激活状态，您需要做以下操作才能正常使用该卡：<br/>\
							1、将此卡挂在工具机台上<br/>\
							2、作一笔测试转帐<br/>\
							3、测试转帐完成后，还需要立即抓取流水<br/>\
							经过系统审核后，此卡被激活，变成停用状态，有权限的工作人员可以启用或停用。";
var showMessageForever=function(content,title,flag){
	if(flag&&flag==1){
		//来源为手机时，状态为可用 无需激活提示
        showMessageForSuccess(title);
	}else{
		$.gritter.add({
	         title: title?title:'系统消息',
	         text: content,
	         width:600,
	         sticky: true,
	         image: '../images/message.png'
	     });
	}
}

//只能输入2位小数的金额
var clearNoNum2=function(obj){
	obj.value = obj.value.replace(/[^\d.]/g,""); //清除"数字"和"."以外的字符
	obj.value = obj.value.replace(/^\./g,""); //验证第一个字符是数字
	obj.value = obj.value.replace(/\.{2,}/g,"."); //只保留第一个, 清除多余的
	obj.value = obj.value.replace(".","$#$").replace(/\./g,"").replace("$#$",".");
	obj.value = obj.value.replace(/^(\-)*(\d+)\.(\d\d).*$/,'$1$2.$3'); //只能输入两个小数
}
/** 根据IP获取主机信息 */
var getHostByIp=function(ip){
	var result;
	$.ajax({
		type:"POST",
		async:false,
		url:"/r/newhost/findbyIP",
		data:{"ip":ip},
		dataType:'json',
		success:function(jsonObject){
			// 查询失败；返回值为undefined或空字符串时返回
			if(jsonObject.status ==1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取主机信息异常，"+jsonObject.message);
			}
			
		}
	});
	return result;
}
/** 根据flag返回字符串 */
var getFlagStr=function(flag){
	var result="PC";
	if(flag){
		if(flag==1){
			result="手机";
		}else if(flag==2){
			result="返利网";
		}
	}
	return result;
}
/** 过滤银行字符 */
var replaceBankStr=function(bankType){
	if(bankType){
		return bankType.replace("银行","");
	}else{
		return "无";
	}
}

var getHandicapInfoById=function(handicapId){
	var result;
	$.map(handicap_list_all,function(record){
		if(record&&record.id==handicapId){
			result= record;
		}
	});
	return result;
}

/**
 * 返利网 账号修改 flag=2
 */
var showUpdateflag2Account=function(accountId,fnName,isActivated){
	var accountInfo=getAccountInfoById(accountId);
	var showZeroCredit = (!accountInfo.peakBalance||accountInfo.peakBalance<=1000);
	$.ajax({
		type:"GET",
		async:false,
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/outAccount.html",
		success : function(html){
			var $div=$(html).find("#updateFlag2Account").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			//表单填充值
			$div.find("#accountId").val(accountId);
			if(accountInfo){
			    $div.find("input[name='id']").val(accountInfo.id);
			    $div.find("input[name='account']").val(hideAccountAll(accountInfo.account));
			    $div.find("input[name='limitIn']").val(accountInfo.limitIn);
			    $div.find("input[name='limitOut']").val(accountInfo.limitOut);
			    $div.find("input[name='limitOutOne']").val(accountInfo.limitOutOne);
			    $div.find("input[name='limitOutOneLow']").val(accountInfo.limitOutOneLow?accountInfo.limitOutOneLow:"");
			    $div.find("input[name='limitOutCount']").val(accountInfo.limitOutCount?accountInfo.limitOutCount:"0");
			    $div.find("input[name='lowestOut']").val(accountInfo.lowestOut);
			    $div.find("input[name='limitBalance']").val(accountInfo.limitBalance);
				if(accountInfo.type==accountTypeInBank){
					//入款卡 最小入款金额和是否可做出款用
					$div.find(".needHide").show();
				    $div.find("input[name='minInAmount']").val(accountInfo.minInAmount?accountInfo.minInAmount:"");
				    //省 市
				    loadProvinceCity_select($div.find("select[name='province']"),$div.find("select[name='city']"),accountInfo.province,accountInfo.city);
				}
				//初始化盘口 任何类型不允许修改盘口
				getHandicap_select($div.find("select[name='handicap_select']"),accountInfo.handicapId);
			    if(accountInfo.currSysLevel){
				    $div.find("select[name='currSysLevel']").val(accountInfo.currSysLevel);
			    }
				if(isActivated){
					//可以修改状态 类型
			    	$div.find(".statusTr").show();
					getAccountType_select_search($div.find("select[name='accountType_select']"),null,accountInfo.type,accountInfo.subType);
			    	$div.find(".classOfDialogTitle").html("完善银行卡信息");
					if(accountInfo.type==accountTypeInBank){
						//银行入款卡 和支付宝入款卡都不可以改为新卡 银行入款卡不可以在用
						$div.find(".incomeHide5").remove();
						if(accountInfo.subType==0){
							$div.find(".incomeHide1").remove();
						}
					}
				}else{
					$div.find("[name=handicap_select]").attr("disabled","disabled");
				}
				if(showZeroCredit){
                    $div.find(".creditStr").show();
				}
			}else{
				if(fnName){
					fnName();
				}
			}
			//确定按钮绑定事件
			$div.find("#doUpdate").bind("click",function(){
				updateFlag2Account(fnName,accountInfo.type,isActivated);
			});
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}
/**
 * 修改银行卡信息
 */
var updateFlag2Account=function(fnName,type,isActivated){
	var $div=$("#updateFlag2Account");
    var $accountId = $div.find("#accountId");
	var accountInfo=getAccountInfoById($accountId.val());
    var $accountId = $div.find("#accountId");
    var $limitIn = $div.find("input[name='limitIn']");
    var $limitOut = $div.find("input[name='limitOut']");
    var $lowestOut = $div.find("input[name='lowestOut']");
    var $limitOutOne = $div.find("input[name='limitOutOne']");
    var $limitOutOneLow = $div.find("input[name='limitOutOneLow']");
    var $limitOutCount = $div.find("input[name='limitOutCount']");
    var $limitBalance = $div.find("input[name='limitBalance']");
	var $currSysLevel = $div.find("select[name='currSysLevel']");
	var $status = $div.find("[name=status]:checked");
	var $handicapId=$div.find("[name=handicap_select]");
	var $type=$div.find("[name=accountType_select]");
    var $zerocredits = $div.find("[name=zerocredits]:checked");
    var $province=$div.find("select[name='province']");
    var $city=$div.find("select[name='city']");
    var data={
			"id":$accountId.val(),
			"limitIn":$limitIn.val(),
			"limitOut":$limitOut.val(),
			"lowestOut":$lowestOut.val(),
			"limitOutOne":$limitOutOne.val(),
			"limitOutOneLow":$limitOutOneLow.val(),
			"limitOutCount":$limitOutCount.val(),
			"limitBalance":$limitBalance.val(),
			"currSysLevel":$currSysLevel.val()
		};
    if(accountInfo.type==accountTypeInBank){
		data.province=$.trim($province.val(),true);
		data.city=$.trim($city.val(),true);
    }
    //校验非空和输入校验
    var validatePrint=[
    	{ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitIn,name:'当日入款限额',type:'amountPlus'},
    	{ele:$limitOut,name:'当日出款限额',type:'amountPlus'},
    	{ele:$lowestOut,name:'最低余额限制',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitOutOne,name:'最高单笔出款限额',type:'amountPlus',min:0,maxEQ:50000},
    	{ele:$limitOutOneLow,name:'最低单笔出款限额',minEQ:0,maxEQ:50000},
    	{ele:$limitOutCount,name:'当日出款笔数',minEQ:0,maxEQ:500}
    ];
    if(isActivated){
    	data.status=$status.val();
    	data.handicapId=$handicapId.val();
    	data.type=$type.val();
    	validatePrint.push({ele:$status,name:'状态'});
    	validatePrint.push({ele:$handicapId,name:'盘口'});
    	validatePrint.push({ele:$type,name:'类型'});
    	//如果是入款卡，加上subType,否则清空
        if(data.type==accountTypeInBank){
        	data.subType=$type.find("option:selected").attr("subType");
        }else{
        	data.subType=0;
        }
    }
	var showZeroCredit = (!accountInfo.peakBalance||accountInfo.peakBalance<=1000);
    if(showZeroCredit){
        data.zerocredits=$zerocredits.val();
    }
    if(!validateEmptyBatch(validatePrint)||
			!validateInput(validatePrint)){
    	return;
    }
    if(accountInfo.type==accountTypeInBank){
    	//入款卡 有最小入款金额 和是否可做出款卡用
        var $minInAmount = $div.find("[name=minInAmount]");
		data["minInAmount"]=$.trim($minInAmount.val());
		if(!validateInput([{ele:$minInAmount,name:'最小入款金额',minEQ:0}])){
	    	return;
		}
    }
	bootbox.confirm("确定修改账号?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/account/update',
				async:false,
				data:data,
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	//操作成功提示
			            $div.modal("toggle");
			        	showMessageForSuccess("修改成功");
			            if(fnName){
			            	fnName();
			            }
			        }else{
			        	//失败提示
			        	showMessageForFail("修改失败："+jsonObject.message);
			        }
			    }
			});
		}
	});
}

function getUserByUid(uid){
	$.ajax({dataType:'json',
		async:false,
		type:"post",url:"/r/finoutstat/getUserByUid",
		data:{"uid":uid},
		success:function(jsonObject){
			console.info(jsonObject);
	}});
}

function getLimitAckToRedis(id){
	$.ajax({dataType:'json',
		async:false,
		type:"post",url:"/r/finoutstat/getLimitAckToRedis",
		data:{"id":id},
		success:function(jsonObject){
			console.info(jsonObject);
	}});
}

function setLimitAckToRedis(id){
	$.ajax({dataType:'json',
		async:false,
		type:"post",url:"/r/finoutstat/setLimitAckToRedis",
		data:{"id":id},
		success:function(jsonObject){
			console.info(jsonObject.data);
	}});
}

function LimitAck(account,amount){
	$.ajax({dataType:'json',
		async:false,
		type:"post",url:"/r/finoutstat/limitAck",
		data:{"account":account,"amount":amount},
		success:function(jsonObject){
			console.info(jsonObject.message);
	}});
}

function addBalance(id,amount){
	$.ajax({dataType:'json',
		async:false,
		type:"post",url:"/r/finoutstat/addBalance",
		data:{"id":id,"amount":amount},
		success:function(jsonObject){
			console.info(jsonObject.message);
	}});
}

function changeDate (date){
	var nowDate= new Date(); 
	var nowYear = nowDate.getFullYear();
	var nowMonth = nowDate.getMonth();
	var nowDay = nowDate.getDate();
	var nowDayOfWeek= nowDate.getDay();
	if(date=='thisWeek'){
		var getYesterdayDate = new Date(nowYear, nowMonth, nowDay - 1);
		return formatDate(getYesterdayDate)+" 07:00:00"+" - "+formatDate(nowDate)+" 06:59:59"
	}else if(date=='week'){
		var getWeekStartDate = new Date(nowYear, nowMonth, nowDay - (nowDayOfWeek - 1));
		var getWeekEndDate = new Date(nowYear, nowMonth, nowDay + (8 - nowDayOfWeek));
		return formatDate(getWeekStartDate)+" 07:00:00"+" - "+formatDate(getWeekEndDate)+" 06:59:59"
	}else if(date=='lastweek'){
		 var getUpWeekStartDate = new Date(nowYear, nowMonth, nowDay - nowDayOfWeek -6);
		 var getUpWeekEndDate = new Date(nowYear, nowMonth, nowDay + (8 - nowDayOfWeek - 7));
		 return formatDate(getUpWeekStartDate)+" 07:00:00"+" - "+formatDate(getUpWeekEndDate)+" 06:59:59"
	}else if(date=='thisMonth'){
		var getMonthStartDate = new Date(nowYear, nowMonth, 1);
		var getMonthEndDate = new Date(nowYear, nowMonth, getMonthDays(nowMonth));
		return formatDate(getMonthStartDate)+" 07:00:00"+" - "+formatDate(getMonthEndDate)+" 06:59:59"
	}
}

//获得某月的天数
function getMonthDays(myMonth){
	var nowDate= new Date(); 
	var nowYear = nowDate.getFullYear();
	var nowMonth = nowDate.getMonth();
	var monthStartDate = new Date(nowYear, myMonth, 1);
	 var monthEndDate = new Date(nowYear, myMonth + 1, 2);
	 var days = (monthEndDate - monthStartDate)/(1000 * 60 * 60 * 24);
	 return days;
}

function formatDate(date) {
	 var myyear = date.getFullYear();
	 var mymonth = date.getMonth()+1;
	 var myweekday = date.getDate();
	 if(mymonth < 10){
		 mymonth = "0" + mymonth;
	 }
	 if(myweekday < 10){
		 myweekday = "0" + myweekday;
	 }
	 return (myyear+"-"+mymonth + "-" + myweekday);
}

//切割备注的最后一段字符串 根据<br/>
var cutFlag4remark=function(remark){
	var  result="";
	if(remark){
		var temp=remark.replace(/<br>/g, "cutFlag4remark").split("cutFlag4remark");
		result=temp[temp.length-1];
	}
	return result;
}

function showfreezeTrBZ(){
	 var freezeType= $('#freezeAccount').is(':checked');
	 if(freezeType){
		$("#freezeTrBZ").show();
	 }else{
		$("#freezeTrBZ").hide();
	 }
}
