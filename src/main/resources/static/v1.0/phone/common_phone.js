/** 微信、支付宝、银行卡 */
var loadHover_wechat_bankInfoHover=function(idList){
	// 发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({type:"GET", async:false, dataType:'html', url : "/"+sysVersoin+"/phone/phoneInfoHover.html",
		success : function(html){
			$.map(idList,function(record){
				var infoName="",typeStr="";
				if(record.type==0){//微信
					infoName="wechatInfoHover";
					typeStr="微信";
				}else if(record.type==1){//支付宝
					infoName="alipayInfoHover";
					typeStr="支付宝";
				}else if(record.type==2){//银行卡
					infoName="bankInfoHover";
					typeStr="银行卡";
				}else{
					return;
				}
				$("[data-toggle='"+infoName+record.id+"']").popover({
					html : true,
					title: function(){
						return '<center class="blue">'+typeStr+'信息&nbsp;-&nbsp;'+record.id+'</center>';
					},
					delay:{show:0, hide:100},
					content: function(){
						return loadInfo_wechatAli_bank(record,html);
					}
				});
			});
		}
	});
	
}
var loadInfo_wechatAli_bank=function(data,html){
	var record=getAccountInfo(data.oid,data.id,data.type);
	var $div;
	if(data.type==0||data.type==1){
		$div=$(html).find("#wechatAliInfoHover");
		$div.find("[name=limitInDaily]").text(record.inLimit?record.inLimit:"");
		var aliasStr="";
		if(data.type==1){
			$div.find(".alipayShow").removeClass("hide");
			$div.find("[name=ylbThreshold]").text(record.ylbThreshold?record.ylbThreshold:"无");
			$div.find("[name=ylbInterval]").text(record.ylbInterval?record.ylbInterval:"无");
			$div.find("[name=uid]").text(record.uid?record.uid:"无");
			$div.find("[name=isEpAlipay]").text(record.isEpAlipay&&record.isEpAlipay==1?"是":"否"+"（是/否 企业支付宝）");
			if(record.zfbAlias1){
				aliasStr+=record.zfbAlias1+"&nbsp;&nbsp;";
			}
			if(record.zfbAlias2){
				aliasStr+=record.zfbAlias2;
			}
		}else{
			if(record.wxAlias1){
				aliasStr+=record.wxAlias1+"&nbsp;&nbsp;";
			}
			if(record.wxAlias2){
				aliasStr+=record.wxAlias2+"&nbsp;&nbsp;";
			}
			if(record.wxAlias3){
				aliasStr+=record.wxAlias3;
			}
		}
		$div.find("[name=aliasStr]").html(aliasStr?aliasStr:"无");
	}else if(data.type==2){
		$div=$(html).find("#bankInfoHover");
		$div.find("[name=bankBalance]").text(record.balance?record.balance:"");
		$div.find("[name=bankType]").text(record.bankName?record.bankName:"");
		$div.find("[name=bankName]").text(record.bankOpen?record.bankOpen:"");
	}
	var HandicapBatch=getHandicapBatchInfoByCode();
	$div.find("[name=handicapName]").text(HandicapBatch&&HandicapBatch[data.oid]?HandicapBatch[data.oid].name:'');
	$div.find("[name=account]").text(record.account?record.account:"");
	$div.find("[name=owner]").text(record.name?record.name:"");
	$div.find("[name=levelNameToGroup]").text(record.level==0?"外层":(record.level==2?"内层":"指定层"));
	$div.find("[name=status]").text(record.status==1?"启用":"停用");
	$div.find("[name=createTime]").text(record.createtime?record.createtime:"");
	$div.find("[name=updateTime]").html(record.uptime?record.uptime:"");
	$div.find("[name=bankBalance]").text(record.balance?record.balance:"0");
	$div.find("[name=limitBalance]").text(record.balanceAlarm?record.balanceAlarm:"");
	$div.find("[name=tel]").text(record.tel?hidePhoneNumber(record.tel):"");
	$div.find("[name=contactName]").text(record.contactName?record.contactName:"");
	var outType="";
	outType+=record.qrDrawalMethod&&record.qrDrawalMethod==1?"二维码提现&nbsp;&nbsp;":"";
	outType+=record.bankDrawalMethod&&record.bankDrawalMethod==1?"银行卡提现":"";
	$div.find("[name=outType]").html(outType?outType:"未设置");
	$div.find("[name=remark]").text(record.remark?record.remark:"");
	return $div;
	
}



/** 手机号 */
var loadHover_MobileInfoHover=function(idList){
	// 发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({type:"GET", async:false, dataType:'html', url : "/"+sysVersoin+"/phone/phoneInfoHover.html",
		success : function(html){
			$.map(idList,function(record){
				$("[data-toggle='mobileInfoHover"+record.id+"']").popover({
					html : true,
					title: function(){
						return '<center class="blue">手机信息&nbsp;-&nbsp;'+record.id+'</center>';
					},
					delay:{show:0, hide:100},
					content: function(){
						return loadInfo_Mobile(record,html);
					}
				});
			});
		}
	});
	
}
var loadInfo_Mobile=function(data,html){
	var record=getTelInfo(data.oid,data.id);
	var $div = $(html).find("#clientMobile");
	if(record){
		if(record.type==0){
			// 客户类型有信用额度和返佣信息
			$div.find(".typeCustomerShow").show();
			$div.find("span[name=creditLimit]").html(record.credits?("&nbsp;&nbsp;<strong>信用额度</strong>&nbsp;&nbsp;"+record.credits):'&nbsp;&nbsp;无');
			$div.find("span[name=bonusCard]").text(record.commissionBankNum?record.commissionBankNum:'');
			$div.find("span[name=bonusCardOwner]").text(record.commissionOpenMan?record.commissionOpenMan:'');
			$div.find("span[name=bonusCardName]").text(record.commissionBankName?record.commissionBankName:'');
		}
		var HandicapBatch=getHandicapBatchInfoByCode();
		$div.find("span[name=handicapName]").text(HandicapBatch&&HandicapBatch[data.oid]?HandicapBatch[data.oid].name:'');
		$div.find("span[name=levelNameToGroup]").text(record.level==0?"外层":(record.level==2?"内层":"指定层"));
		$div.find("span[name=mobile]").text(hidePhoneNumber(record.tel));
		$div.find("span[name=owner]").text(record.contactName?record.contactName:'');
		$div.find("span[name=type]").text(record.type==0?'客户':'自用');
		$div.find("span[name=status]").text(record.status==1?"启用":"停用");
		$div.find("span[name=device1]").text(record.wechatDeviceCol?record.wechatDeviceCol:"");
		$div.find("span[name=device2]").text(record.alipayDeviceCol?record.alipayDeviceCol:"");
		$div.find("span[name=device3]").text(record.bankDeviceCol?record.bankDeviceCol:"");
		$div.find("span[name=wechat]").text(record.wechatAccount?record.wechatAccount:'未注册');
		$div.find("span[name=alipay]").text(record.alipayAccount?record.alipayAccount:'未注册');
		$div.find("span[name=bankCard]").text(record.bankAccount?record.bankAccount:'未绑定');
		$div.find("span[name=remark]").text(record.remark?record.remark:'');
		$div.find("span[name=prefix]").text(record.prefix?record.prefix:'');
		$div.find("span[name=suffix]").text(record.suffix?record.suffix:'');
	}
	return $div;
	
}


/** 单个手机信息读取 */
var getTelInfo=function(handicapCode,telId){
	var result=null;
	var params={
		oid:handicapCode,
		id:telId
	}
	 $.ajax({
	        type: "POST",
	        contentType: 'application/json;charset=UTF-8',
	        dataType: 'JSON',
	        url: '/newpay/findTelInfo',
	        async: false,
	        data: JSON.stringify(params),
	        success: function (jsonObject) {
	            if (jsonObject.status == 1) {
	            	result=jsonObject.data;
	            } else {
	                showMessageForFail("查询失败：" + jsonObject.message);
	            }
	        }
	    });
	return result;
}
/** 单个账号信息读取 不带※ */
var getAccountInfo2=function(handicapCode,telId,type){
	var result=null;
	var params={
		oid:handicapCode,
		id:telId,
		type:type
	}
	 $.ajax({
	        type: "POST",
	        contentType: 'application/json;charset=UTF-8',
	        dataType: 'JSON',
	        url: '/newpay/findAccountInfo2',
	        async: false,
	        data: JSON.stringify(params),
	        success: function (jsonObject) {
	            if (jsonObject.status == 1) {
	            	result=jsonObject.data;
	            } else {
	                showMessageForFail("查询失败：" + jsonObject.message);
	            }
	        }
	    });
	return result;
}
/** 单个账号信息读取 带※ */
var getAccountInfo=function(handicapCode,telId,type){
	var result=null;
	var params={
			oid:handicapCode,
			id:telId,
			type:type
	}
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/newpay/findAccountInfo',
		async: false,
		data: JSON.stringify(params),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				result=jsonObject.data;
			} else {
				showMessageForFail("查询失败：" + jsonObject.message);
			}
		}
	});
	return result;
}

/** 查询业主未删除的新支付通道 */
var getPOCForCrk=function(handicapCode,type){
	var result=null;
	var params={
			oid:handicapCode,
			type:type
	}
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/newpay/findPOCForCrk',
		async: false,
		data: JSON.stringify(params),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				result=jsonObject.data;
			} else {
				showMessageForFail("查询失败：" + jsonObject.message);
			}
		}
	});
	return result;
}

/** 查询客户资料已绑定的支付通道 */
var getBindPOByWechatAli=function(handicapCode,mobileId,type){
	var result=null;
	var params={
			oid:handicapCode,
			mobileId:mobileId,
			type:type
	}
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/newpay/newpayAisleConfigFindBind',
		async: false,
		data: JSON.stringify(params),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				result=jsonObject.data;
			} else {
				showMessageForFail("查询失败：" + jsonObject.message);
			}
		}
	});
	return result;
}

/** 自动加载盘口选择列表 默认选中第一个盘口 */
var getHandicapCode_select_one=function($div,handicapCode){
	var options="";
	$.each(handicap_list,function(index,record){
		if(handicapCode&&record.code==handicapCode){
			options+="<option selected value="+record.code+" >"+record.name+"</option>";
		}else{
			options+="<option value="+record.code+" >"+record.name+"</option>";
		}
	});
	$div.html(options);
}

var resetCustomer=function(div){
	if(!div) return;
	// 重置重写
	var $handicap=$("#"+div).find("select[name=search_EQ_handicapCode]");
	$handicap.val($handicap.find("option").first().val());
	reset(div);
	//非重复代码 勿删
	$handicap.val($handicap.find("option").first().val());
}

/** 根据平台的银行类别自动识别出入款系统的银行类别 */
var getBankType4Customer=function(bankType){
	var result;
	$.map(bank_name_list,function(bankName){
		if($.trim(bankName)==bankType||bankType.match(bankName)){
			result=bankName;
		}
	});
	return result;
}
/**
 * 手机号加星号 前2后4 应该是十一位
 */
var hidePhoneNumber=function(account){
    if(account){
        if (account.length>=8) {
            account = account.substring(0,3)+'**'+account.substring(account.length-3);
        }else{
        	account = account.substring(0,1)+'**'+account.substring(account.length-1);
        }
    }else{
        account = '';
    }
	return account;
}

/** 统计常用金额、非常用金额已生成二维码个数和总个数 */
var getStatisticsMWR=function(handicapCode,telId,type){
	var result=null;
	var params={
		oid:handicapCode,
		mobileId:telId,
		type:type
	}
	 $.ajax({
	        type: "POST",
	        contentType: 'application/json;charset=UTF-8',
	        dataType: 'JSON',
	        url: '/newpay/statisticsMWR',
	        async: false,
	        data: JSON.stringify(params),
	        success: function (jsonObject) {
	            if (jsonObject.status == 1) {
	            	result=jsonObject.data;
	            } else {
	                showMessageForFail("查询失败：" + jsonObject.message);
	            }
	        }
	    });
	return result;
}

/** 输入校验 只能 中文/字母/数字 */
var checkInputContent=function(str){
	var char=/^[\u4e00-\u9fa5_a-zA-Z0-9]+$/;  
	return char.test(str);
}

