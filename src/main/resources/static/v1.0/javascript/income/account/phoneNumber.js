currentPageLocation = window.location.href;
//抓取状态  InAli InWechat BindCustomer
var MobileBindpayStatus={
	1:{status:1,msg:'正常',statusClass:'label-success'},
	0:{status:0,msg:'异常',statusClass:'label-danger'}
};
var MobileMobileStatus={
		1:{status:1,msg:'在线空闲'},
		0:{status:0,msg:'离线 '}
	};
var HandicapBatch=getHandicapBatchInfoByCode();
var offlineStrBank='<span class="label label-grey" style="display:block;width:100%;">离线 - 设备2</span>';
var offlineStrOthers='<span class="label label-grey" style="display:block;width:100%;">离线 - 设备1</span>';
/** 手机号列表 */
var showPage = function (CurPage) {
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/r/mobile/cloud/page",
        data: buildParams(CurPage),
        success: function (jsonObject) {
            if (jsonObject.status != 1) {
                showMessageForFail("查询失败：" + jsonObject.message);
                return;
            }
            var $tbody = $("table#accountListTable").find("tbody");
            $tbody.html("");
            var accArray = [], mobileArray = [];
            $.each(jsonObject.data, function (index, record) {
                mobileArray.push(record.mobile);
                record.owner = record.owner ? record.owner : '';
                if(HandicapBatch&&HandicapBatch[record.handicap]){
					record.handicapCode =  HandicapBatch[record.handicap].code;
					record.handicap = HandicapBatch[record.handicap].name;
                }else{
					record.handicapCode ='';
					record.handicap='';
                }
               
                record.inOutstatus = record.status == mobileStatusFreeze ? '冻结' : '';
                record.inOutstatus = record.status == mobileStatusStopTemp ? '停用' : record.inOutstatus;
                record.inOutstatus = record.status == mobileStatusNormal ? '在用' : record.inOutstatus;
                record.inOutstatusClass = record.status == mobileStatusFreeze || record.status == mobileStatusStopTemp ? 'label-danger' : 'label-success';
                record.typeName = record.type == MobileTypeCustomer ? '客户' : '自用';
                record.typeClass = record.type == MobileTypeCustomer ? ' label-info' : ' label-purple ';
                record.wechatBalance = record.wechatBalance ? record.wechatBalance : 0;
                record.alipayBalance = record.alipayBalance ? record.alipayBalance : 0;
                record.bankBalance = record.bankBalance ? record.bankBalance : 0;
                record.totalBalance = record.wechatBalance + record.alipayBalance + record.bankBalance;
                record.bank = record.bank ? record.bank : '';
                record.wechat = record.wechat ? record.wechat : '';
                record.alipay = record.alipay ? record.alipay : '';
                !record.wechat || accArray.push({acc: record.wechat, mobile: record.mobile, type: 'wechatInfoHover'});
                !record.alipay || accArray.push({acc: record.alipay, mobile: record.mobile, type: 'alipayInfoHover'});
                !record.bank || accArray.push({acc: record.bank, mobile: record.mobile, type: 'bankInfoHover'});
                accArray.push({acc: record.mobile, mobile: record.mobile, type: 'mobileInfoHover'});
                accArray.push({acc: record.mobile, mobile: record.mobile, type: 'mobileBalHover'});
                //流水抓取状态
                var statusStr4bank='',statusStr4wechat='', statusStr4alipay='';
                //银行卡
                if(record.bankStatus){
                	if(record.device2Status==MobileMobileStatus[1].status){//在线空闲
                    	var statusInfo=MobileBindpayStatus[record.bankStatus];
                    	if(statusInfo){
                    		statusStr4bank='<span class="label '+statusInfo.statusClass+'" style="display:block;width:100%;">'+statusInfo.msg+' - 设备2</span>';
                    	}
                	}else{//离线
//                		statusStr4bank=offlineStrBank;
                	}
                }
                if(record.device1Status){
                	if(record.device1Status==MobileMobileStatus[1].status){//在线空闲
                    	//微信
                    	var statusInfoWechat=MobileBindpayStatus[record.wechatStatus];
                        if(statusInfoWechat){
                        	if(statusInfoWechat){
                        		statusStr4wechat='<span class="label '+statusInfoWechat.statusClass+'" style="display:block;width:100%;">'+statusInfoWechat.msg+' - 设备1</span>';
                        	}
                        }
                        //支付宝
                    	var statusInfoAlipay=MobileBindpayStatus[record.alipayStatus];
                        if(statusInfoAlipay){
                        	if(statusInfoAlipay){
                        		statusStr4alipay='<span class="label '+statusInfoAlipay.statusClass+'" style="display:block;width:100%;">'+statusInfoAlipay.msg+' - 设备1</span>';
                        	}
                        }
                	}else{//离线
//                		statusStr4alipay=offlineStrOthers;
//                		statusStr4wechat=offlineStrOthers;
                	}
                }
                //数据填充
                var tr = "<td>" + record.handicap + "</td>\
					 <td>\
					   <a class='bind_hover_card' data-placement='auto right' data-trigger='hover' data-toggle='mobileInfoHover" + record.mobile + "' ><span>" + record.mobile + "</span></a>\
					 </td>\
				     <td><span>" + record.owner + "</span></td>\
				     <td><span class='label label-sm " + record.inOutstatusClass + "'>" + record.inOutstatus + "</span></td>\
				     <td><span class='label label-white middle " + record.typeClass + "'>" + record.typeName + "</span></td>";
                if (record.bank) {
                    tr += "<td>";
                    tr += "<a class='bind_hover_card' data-trigger='hover'  data-placement='auto right' data-toggle='bankInfoHover" + record.mobile + "'>" + hideAccountAll(record.bank) + "</a>";
                    tr += statusStr4bank;
                    tr += "</td>";
                } else {
                    tr += "<td>--/--</td>";
                }
                if (record.wechat) {
                    tr += "<td>";
                    tr += "<a class='bind_hover_card' data-placement='auto right' data-trigger='hover' data-toggle='wechatInfoHover" + record.mobile + "'>" + hideAccountAll(record.wechat) + "</a>";
                    tr += statusStr4wechat;
                    tr += "</td>";
                } else {
                    tr += "<td>--/--</td>";
                }
                if (record.alipay) {
                    tr += "<td>";
                    tr += "<a class='bind_hover_card' data-placement='auto right' data-trigger='hover' data-toggle='alipayInfoHover" + record.mobile + "'>" + hideAccountAll(record.alipay) + "</a>";
                    tr += statusStr4alipay;
                    tr += "</td>";
                } else {
                    tr += "<td>--/--</td>";
                }
                tr += "<td><span>" + (record.creditLimit ? record.creditLimit : "--/--") + "</span></td>";
                tr += "<td><span><a class='bind_hover_card' data-placement='auto right' data-trigger='hover' data-toggle='mobileBalHover" + record.mobile + "' data-mobile='" + record.mobile + "'>" + setAmountAccuracy(record.totalBalance) + "</a></span></td>";
                tr += "<td><span class='bonus' bonus='" + record.mobile + "'></span></td>";
                //已绑账号操作
                tr += "<td>";
			  	tr += "	<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:updateBindInfo:*' onclick='showModalUpdateBindAccount(\"" + record.mobile + "\")'><i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></a>"
			  	tr += "	<a class='btn btn-xs btn-white btn-danger btn-bold red contentRight' contentright='IncomePhoneNumber:updateBindPWD:*' onclick='showModalUpdatePWD(\"" + record.mobile + "\")'><i class='ace-icon fa fa-asterisk bigger-100 red'></i><span>密码</span></a>"
                tr += "	<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:updateBindStatus:*' onclick='showModalUpdateBindStatus(\"" + record.mobile + "\")'><i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>状态</span></a>"
                tr += "	<a class='btn btn-xs btn-white btn-primary btn-bold blue contentRight' contentright='IncomePhoneNumber:searchQR:*' onclick='showModaPageForQr(\"" + record.mobile + "\")'><i class='ace-icon fa fa-barcode bigger-100 blue'></i><span>收款码</span></a>"
                tr += "</td>";
			  	//手机号操作
                tr += "<td>";
                tr += "	<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:updateMobile:*' onclick='showModalUpdateMobile(\"" + record.mobile + "\")'><i class='ace-icon fa fa-pencil-square-o bigger-100 red'></i><span>修改</span></a>\
                <a class='btn btn-xs btn-white btn-primary btn-bold blue contentRight' contentright='IncomePhoneNumber:encash:*' onclick='showWithdrawCashModal(" + record.id + ",\"" + record.mobile + "\",\"" + record.alipay + "\",\"" + record.wechat + "\",\"" + record.bank + "\",\"" + record.handicapCode + "\",showPage)'><i class='ace-icon fa fa-credit-card bigger-100 blue'></i><span>提现</span></a>";
                tr+="<a class='btn btn-xs btn-white btn-danger btn-bold contentRight' contentRight='IncomePhoneNumber:deleteMobile:*' " +
						"onclick='do_deletePhoneNo("+record.mobile+")'>" +
						"<i class='ace-icon fa fa-close bigger-100 red'></i>" +
						"<span>删除</span>" +
					"</a>";
                tr += "</td>";
                $tbody.append($("<tr id='mainTr" + record.id + "'>" + tr + "</tr>"));
            });
            showPading(jsonObject.page, "phoneNumber_page", showPage, null, true, true);
            $("#phoneNumber_page").find(".showImgTips").children().each(function (index, record) {
                if (index <= 4) {
                    $(record).remove();
                }
            });
            loadHover_accountInfoHover(accArray);
            contentRight();
            bonusTotalForeach($tbody, mobileArray);
        }
    });
};

var buildParams = function (CurPage) {
    var $div = $("#accountFilter");
    if (!CurPage && CurPage != 0) CurPage = $("#phoneNumber_page").find(".Current_Page").text();
    var data = {
        pageNo: CurPage <= 0 ? 0 : CurPage - 1,
        pageSize: $.session.get('initPageSize'),
        search_LIKE_mobile: $.trim($div.find("[name=search_LIKE_mobileNo]").val()),
        search_LIKE_owner: $.trim($div.find("[name=search_LIKE_owner]").val()),
        search_LIKE_wechat: $.trim($div.find("[name=search_LIKE_wechat]").val()),
        search_LIKE_alipay: $.trim($div.find("[name=search_LIKE_alipay]").val()),
        search_LIKE_bank: $.trim($div.find("[name=search_LIKE_bankCard]").val()),
        search_EQ_handicapId: $.trim($div.find("select[name=search_EQ_handicapId]").val()),
        search_EQ_level: $.trim($div.find("select[name=search_EQ_level]").val())
    };
    if ($div.find("[name=type]:checked").length == 1) {
        data.search_EQ_type = $div.find("[name=type]:checked").val();
    }
    var statusArray = [];
    $div.find("[name=status]:checked").map(function () {
        statusArray.push($(this).val());
    });
    data.statusArray = statusArray.toString();
    return data;
};

/** 新增手机号基本信息 */
var showModalAddPhone = function () {
    var $div = $("#addPhoneNo4clone").clone().attr("id", "modalAddPhone");
    $div.find("td").css("padding-top", "10px");
    getHandicap_select($div.find("select[name='handicap_select']"));
    $div.modal("toggle");
	//加载银行品牌
	var nbsp='&nbsp;&nbsp;&nbsp;&nbsp;';
	var options="";
	options+="<option value='' >"+nbsp+"请选择"+nbsp+"</option>";
	$.each(bank_name_list,function(index,record){
		options+="<option value="+record+" >"+nbsp+record+nbsp+"</option>";
	});
	$div.find("select[name=choiceBankBrand]").html(options);
    $div.find("[name=type]").change(function () {
        if ($div.find("[name=type]:checked").val() == MobileTypeCustomer) {//客户
            $div.find(".customerShow").show();
        } else {
            $div.find(".customerShow").hide();
        }
    });
    $div.on('hidden.bs.modal', function () {
        //关闭窗口清除内容;
        $div.find("[name=type]").unbind();
        $div.remove();
    });
};

/** 修改手机号基本信息*/
var showModalUpdateMobile = function (mobile) {
    var $div = $("#updateMobile4clone").clone().attr("id", "updateMobile");
    $div.find("td").css("padding-top", "10px");
    $div.modal("toggle");
    var data = findMobileFromCloud(mobile);
    //基本信息
    var $base = $div.find("div[id=tabBase]");
    if(HandicapBatch&&HandicapBatch[data.handicap]){
        $div.find("[name=handicap]").val(HandicapBatch[data.handicap].name);
    }
    $div.find("#mobileId").val(data.id);
    $div.find("[name=mobile]").val(data.mobile);
    $div.find("[name=owner]").val(data.owner);
    $div.find("[name=wechat]").val(data.wechat ? data.wechat : '');
    $div.find("[name=alipay]").val(data.alipay ? data.alipay : '');
    $div.find("[name=bank]").val(data.bank ? data.bank : '');
    $div.find("[name=type][value=" + data.type + "]").prop("checked", true);
    $div.find("[name=level][value=" + data.level + "]").prop("checked", true);
    $div.find("[name=status][value=" + data.status + "]").prop("checked", true);
    $div.find("[name=creditLimit]").val(data.creditLimit ? data.creditLimit : 0);
    if(data.bonusEntity){
        $div.find("[name=bonusCard]").val(data.bonusEntity.account ? data.bonusEntity.account : '');
        $div.find("[name=bonusCardOwner]").val(data.bonusEntity.owner ? data.bonusEntity.owner : '');
        $div.find("[name=bonusCardName]").val(data.bonusEntity.bankName ? data.bonusEntity.bankName : '')
    };
    //账号信息
    $div.find("[name=type]").change(function () {
        if ($div.find("[name=type]:checked").val() == MobileTypeCustomer) {//客户
            $div.find(".customerShow").show();
        } else {
            $div.find(".customerShow").hide();
        }
    });
    $div.find("[name=type]").change();
	$div.on('hidden.bs.modal', function () {
	    //关闭窗口清除内容;
		$div.find("[name=type]").unbind();
	    $div.remove();
	});
};

/** 已绑账号密码 */
var showModalUpdatePWD = function (mobile) {
	var $div = $("#updatePWD4clone").clone().attr("id", "updatePWD");
	$div.find("td").css("padding-top", "10px");
	$div.modal("toggle");
	var data = findMobileFromCloud(mobile);
	//密码信息
	if(data.bankEntity){
		var bankType=data.bankEntity.bankType;
	    if(data.bankEntity.sing){
		    $div.find("[name=singBank]").attr("placeholder","********");
	    }
	    if(data.bankEntity.ping){
	    	$div.find("[name=pingBank]").attr("placeholder","********");
	    }
		if(bankType&&bankType=='建设银行'){
			$div.find("[name=bingBank],[name=uingBank]").removeAttr('disabled');
		    if(data.bankEntity.bing){
			    $div.find("[name=bingBank]").attr("placeholder","********");
		    }
		    if(data.bankEntity.uing){
		    	$div.find("[name=uingBank]").attr("placeholder","********");
		    }
			
		}else{
			$div.find("[name=bingBank],[name=uingBank]").attr('disabled','disabled');
		}
		$div.find("[name=bank]").val(data.bank);
		$div.find("[name=bankHide]").val(hideAccountAll(data.bank)+" - "+bankType);
		
	}else{
		$div.find("[name=singBank],[name=pingBank],[name=bingBank],[name=uingBank]").attr('disabled','disabled');
	}
	if(data.wechatEntity){
	    $div.find("[name=wechat]").val(data.wechat);
	    $div.find("[name=wechatHide]").val(hideAccountAll(data.wechat));
	    if(data.wechatEntity.sing){
		    $div.find("[name=singWechat]").attr("placeholder","********");
	    }
	    if(data.wechatEntity.ping){
	    	$div.find("[name=pingWechat]").attr("placeholder","********");
	    }
	}else{
		$div.find("[name=singWechat],[name=pingWechat]").attr('disabled','disabled');
	}
    if(data.alipayEntity){
        $div.find("[name=alipay]").val(data.alipay);
        $div.find("[name=alipayHide]").val(hideAccountAll(data.alipay));
	    if(data.alipayEntity.sing){
		    $div.find("[name=singAlipay]").attr("placeholder","********");
	    }
	    if(data.alipayEntity.ping){
	    	$div.find("[name=pingAlipay]").attr("placeholder","********");
	    }
	}else{
		$div.find("[name=singAlipay],[name=pingAlipay]").attr('disabled','disabled');
	}
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
};

/** 已绑账号状态 */
var showModalUpdateBindStatus = function (mobile) {
	var $div = $("#updateBindStatus4clone").clone().attr("id", "updateBindStatus");
	$div.find("td").css("padding-top", "10px");
	$div.modal("toggle");
    $div.find("#mobile").val(mobile);
	//绑定信息初始化
	updateBindStatus_infoLoad($div);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
};
var updateBindStatus_infoLoad=function($div){
	var data = findMobileFromCloud($div.find("#mobile").val());
	if(data){
		var type=$div.find("#updateBindAccount_tabType").val();
		if(type=='bank'){
			if(data.bankEntity){
			    $div.find("[name=account]").val(hideAccountAll(data.bank));
			    $div.find("#account").val(data.bank);
			    $div.find("[name=status][value=" + data.bankEntity.status + "]").prop("checked", true);
				$div.find("#updateBindStatus_ok").show();
				$div.find("table").show();
				$div.find("#unBind").hide();
			}else{
				$div.find("#updateBindStatus_ok").hide();
				$div.find("table").hide();
				$div.find("#unBind").show();
			}
		}else if(type=='wechat'){
			if(data.wechatEntity){
			    $div.find("[name=account]").val(hideAccountAll(data.wechat));
			    $div.find("#account").val(data.wechat);
			    $div.find("[name=status][value=" + data.wechatEntity.status + "]").prop("checked", true);
				$div.find("#updateBindStatus_ok").show();
				$div.find("table").show();
				$div.find("#unBind").hide();
			}else{
				$div.find("#updateBindStatus_ok").hide();
				$div.find("table").hide();
				$div.find("#unBind").show();
			}
		}else if(type=='alipay'){
			if(data.alipayEntity){
			    $div.find("[name=account]").val(hideAccountAll(data.alipay));
			    $div.find("#account").val(data.alipay);
			    $div.find("[name=status][value=" + data.alipayEntity.status + "]").prop("checked", true);
				$div.find("#updateBindStatus_ok").show();
				$div.find("table").show();
				$div.find("#unBind").hide();
			}else{
				$div.find("#updateBindStatus_ok").hide();
				$div.find("table").hide();
				$div.find("#unBind").show();
			}
		}
	}
}
var updateBindStatus_changeTab=function(type){
	var $div=$("#updateBindStatus");
	$div.find("#updateBindAccount_tabType").val(type);
	//重置table内数据
	reset("updateBindStatus table");
	updateBindStatus_infoLoad($div);
}

/** 已绑账号信息 */
var showModalUpdateBindAccount = function (mobile) {
	var $div = $("#updateBindAccount4clone").clone().attr("id", "updateBindAccount");
	$div.find("td").css("padding-top", "10px");
	$div.modal("toggle");
    $div.find("#mobile").val(mobile);
	//加载银行品牌
	var nbsp='&nbsp;&nbsp;&nbsp;&nbsp;';
	var options="";
	options+="<option value='' >"+nbsp+"请选择"+nbsp+"</option>";
	$.each(bank_name_list,function(index,record){
		options+="<option value="+record+" >"+nbsp+record+nbsp+"</option>";
	});
	$div.find("select[name=choiceBankBrand]").html(options);
	updateBindAccount_infoLoad($div);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
};
var updateBindAccount_infoLoad=function($div){
	var data = findMobileFromCloud($div.find("#mobile").val());
	if(data){
		var type=$div.find("#updateBindAccount_tabType").val();
		if(type=='bank'){
			if(data.bankEntity){
			    $div.find("#account").val(data.bank);
				$div.find("#bankInfo").find("[name=account]").val(data.bankEntity.account);
				$div.find("#bankInfo").find("[name=owner]").val(data.bankEntity.owner);
				$div.find("#bankInfo").find("[name=choiceBankBrand]").val(data.bankEntity.bankType);
				$div.find("#bankInfo").find("[name=bankName]").val(data.bankEntity.bankName);
				$div.find("#bankInfo").find("[name=limitBalance]").val(data.bankEntity.limitBalance);
			}else{
			    $div.find("#account").val("");
				$div.find("#bankInfo").find("[name=account]").attr("type","number");
				$div.find("#wechatAlipayInfo").hide();
			}
		}else if(type=='wechat'||type=='alipay'){
			var accountEntity;
			if(type=='wechat'){
				accountEntity=data.wechatEntity;
			}else if(type=='alipay'){
				accountEntity=data.alipayEntity;
			}
			if(accountEntity){
				if(accountEntity.outType==0){
					$div.find("#wechatAlipayInfo").find("[name=transOutType]").prop("checked","checked");
				}else if(accountEntity.outType){
					$div.find("#wechatAlipayInfo").find("[name=transOutType]").prop("checked",false);
					$div.find("#wechatAlipayInfo").find("[name=transOutType][value="+accountEntity.outType+"]").prop("checked","checked");
				}
			    $div.find("#account").val(accountEntity.account);
				$div.find("#wechatAlipayInfo").find("[name=account]").val(accountEntity.account);
				$div.find("#wechatAlipayInfo").find("[name=owner]").val(accountEntity.nickname);
				$div.find("#wechatAlipayInfo").find("[name=limitInDaily]").val(accountEntity.limitInDaily);
				$div.find("#wechatAlipayInfo").find("[name=limitBalance]").val(accountEntity.limitBalance);
			}else{
			    $div.find("#account").val("");
				$div.find("#bankInfo").hide();
			}
		}
	}
}
var updateBindAccount_changeTab=function(type){
	var $div=$("#updateBindAccount");
	//重置table内数据
	reset("updateBindAccount table");
	$div.find("#updateBindAccount_tabType").val(type);
	if(type=='bank'){
		$div.find("#wechatAlipayInfo,#unBind").hide();
		$div.find("#bankInfo").show();
	}else if(type=='wechat'||type=='alipay'){
		$div.find("#bankInfo,#unBind").hide();
		$div.find("#wechatAlipayInfo").show();
	}
	updateBindAccount_infoLoad($div);
}

/** 已绑账号二维码获取 */
var showModaPageForQr = function (mobile) {
	var $div = $("#searchBindQR");
	$div.modal("toggle");
	$div.find("td").css("padding-top", "10px");
    $div.find("#mobile").val(mobile);
	$(".do_genQr").hide();
    //根据上次记录的TAB类型查询 第一页开始
    searchBindQR_changeTab( $div.find("#searchBindQR_tabType").val(),0);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容
		reset('searchBindQR_filter');
		reset('tableQRList');
		reset('wechatAlipayQRInfo .do_genQr');
	});
};
/** 已绑账号二维码获取 */
var searchBindQR_changeTab=function(type,CurPage){
	var $div=$("#searchBindQR");
	$div.find("#searchBindQR_tabType").val(type);
	reset('tableQRList');
	var data = findMobileFromCloud($div.find("#mobile").val());
	if(data){
		if(type==accountType.InWechat.typeId){
			if(data.wechatEntity){
				$div.find("#dataDiv").show();
				$div.find("#unBind").hide();
				$div.find("#account").text(hideAccountAll(data.wechat));
				//重置table内数据
				searchBindQR_infoLoad(data,CurPage==0?CurPage:null);
			}else{
				$div.find("#dataDiv").hide();
				$div.find("#unBind").show();
			}
		}else if(type==accountType.InAli.typeId){
			if(data.alipayEntity){
				$div.find("#dataDiv").show();
				$div.find("#unBind").hide();
				$div.find("#account").text(hideAccountAll(data.alipay));
				//重置table内数据
				searchBindQR_infoLoad(data,CurPage==0?CurPage:null);
			}else{
				$div.find("#dataDiv").hide();
				$div.find("#unBind").show();
			}
		}
	}
}
/** 已绑账号二维码获取 */
var searchBindQR_infoLoad=function(data,CurPage){
	var $div=$("#searchBindQR");
	if(!data){
		data = findMobileFromCloud($div.find("#mobile").val());
	}
	var type=$div.find("#searchBindQR_tabType").val();
	var account;
	if(type==accountType.InWechat.typeId){
		account=data.wechat;
	}else if(type==accountType.InAli.typeId){
		account=data.alipay;
	}
	if(!!!CurPage&&CurPage!=0) CurPage=$("#searchBindQRPage .Current_Page").text();
	$.ajax({
        type: "POST",
        dataType: 'JSON',
        url: '/r/mobile/cloud/pageForQr',
        async: false,
        data: {
        	pageNo:(!CurPage||CurPage<=0)?0:CurPage-1,
            pageSize:$.session.get('initPageSize'),
	        mobile:$div.find("#mobile").val(),
	        account:account,
	        accountType:type,
	        amtBegin:$div.find("[name=minAmount]").val(),
	        amtEnd:$div.find("[name=maxAmount]").val()
        },
        success: function (jsonObject) {
            if (jsonObject.status == 1) {
            	if(jsonObject.data){
            		var trs="";
            		$.each(jsonObject.data,function(index,record){
            			trs+="<tr>";
            			trs+='<td>\
            					<label class="pos-rel">\
									<input type="checkbox" class="ace checkboxDelQR" value="'+record.amount+'">\
									<span class="lbl"></span>\
								</label>\
            				</td>';
            			trs+="<td>"+(index+1)+"</td>";
            			trs+="<td><a>"+record.qr+"</a></td>";
            			trs+="<td>"+record.amount+"</td>";
            			trs+="<td>";
            			trs+="<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:delQR:*' onclick='doDelQR(\"" + record.amount + "\")'><i class='ace-icon fa fa-trash-o bigger-100 red'></i><span>删除</span></a>"
            			trs+="</td>";
            			trs+="</tr>";
            		});
            		$div.find("#dataDiv tbody").html(trs);
            	}
                //分页初始化
    			showPading(jsonObject.page,"searchBindQRPage",searchBindQR_infoLoad,null,true);
                contentRight();
            } else {
                showMessageForFail("查询失败：" + jsonObject.message);
            }
        }
    });
}
/** 删除收款码 */
var doDelQR=function(amount){
	var $div=$("#searchBindQR");
	var data = findMobileFromCloud($div.find("#mobile").val());
	var type=$div.find("#searchBindQR_tabType").val();
	var account;
	if(type==accountType.InWechat.typeId){
		account=data.wechat;
	}else if(type==accountType.InAli.typeId){
		account=data.alipay;
	}
	bootbox.confirm("确定删除?", function (result) {
	    if (result) {
			$.ajax({
			    type: "POST",
			    dataType: 'JSON',
			    url: '/r/mobile/cloud/delQR',
			    async: false,
			    data: {
			        mobile:$div.find("#mobile").val(),
			        account:account,
			        accountType:type,
			        amt:amount
			    },
			    success: function (jsonObject) {
			        if (jsonObject.status == 1) {
			        	$div.find("#searchBindQR_filter #searchBtn").click();
			        	showMessageForSuccess("删除成功");
			        } else {
			            showMessageForFail("删除失败：" + jsonObject.message);
			        }
			    }
			});
	    }
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
	});
}

/** 批量删除二维码 */
var searchBindQR_doBatchDelQR=function(){
	var $div=$("#searchBindQR");
	var data = findMobileFromCloud($div.find("#mobile").val());
	var type=$div.find("#searchBindQR_tabType").val();
	var account;
	if(type==accountType.InWechat.typeId){
		account=data.wechat;
	}else if(type==accountType.InAli.typeId){
		account=data.alipay;
	}
	var amtList=new Array();
	$(".checkboxDelQR:checked").each(function() {  
		amtList.push(this.value);
    });
	if(amtList.length<1){
		showMessageForCheck("请选中至少一行收款码");
		return;
	}
	bootbox.confirm("确定删除选中二维码?", function (result) {
	    if (result) {
			$.ajax({
			    type: "POST",
			    dataType: 'JSON',
			    url: '/r/mobile/cloud/delQRList',
			    async: false,
			    data: {
			        mobile:$div.find("#mobile").val(),
			        account:account,
			        accountType:type,
			        amtList:amtList.toString()
			    },
			    success: function (jsonObject) {
			        if (jsonObject.status == 1) {
			        	$div.find("#searchBindQR_filter #searchBtn").click();
			        	showMessageForSuccess("删除成功");
			        } else {
			            showMessageForFail("删除失败：" + jsonObject.message);
			        }
			    }
			});
	    }
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
	});
}

/** 批量删除二维码 全选反选 */
var checkedAll_batchDelQR=function(){
	var isCheck=$("#checkedBbatchDelQR").is(':checked');
	//全选或者全不选
	$(".checkboxDelQR").each(function() {  
        this.checked = isCheck;       //循环赋值给每个复选框是否选中
    });
}
var show_hide_do_genQr=function(){
	if($(".do_genQr").is(":hidden")){
		$(".do_genQr").show();
	}else{
		$(".do_genQr").hide();
	}
}

/** 生成0元收款码 */
var searchBindQR_genZeroQR=function(){
	var $div=$("#searchBindQR");
	var data = findMobileFromCloud($div.find("#mobile").val());
	var mobile=$div.find("#mobile").val();
	var type=$div.find("#searchBindQR_tabType").val();
	var files=$("#file").prop("files");
	var account;
	if(type==accountType.InWechat.typeId){
		account=data.wechat;
	}else if(type==accountType.InAli.typeId){
		account=data.alipay;
	}
    var formdata = new FormData();
    formdata.append("file",files[0]);
    bootbox.confirm("确定生成?", function (result) {
        if (result) {
			$.ajax({
			    type: "POST",
			    dataType: 'JSON',
			    url: '/r/mobile/cloud/genZeroQR',
			    async: false,
			    data:  {
					mobile:$div.find("#mobile").val(),
					account:account,
					accountType:type
				},
			    success: function (jsonObject) {
			        if (jsonObject.status == 1) {
			        	 showMessageForSuccess("生成成功：");
			        } else {
			            showMessageForFail("生成失败：" + jsonObject.message);
			        }
			    }
			});
        }
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
    });
}
/** 按导入生成收款码 */
var searchBindQR_genImportQR=function(){
	var $div=$("#searchBindQR");
	var data = findMobileFromCloud($div.find("#mobile").val());
	var mobile=$div.find("#mobile").val();
	var type=$div.find("#searchBindQR_tabType").val();
	var files=$("#file").prop("files");
	var account;
	if(type==accountType.InWechat.typeId){
		account=data.wechat;
	}else if(type==accountType.InAli.typeId){
		account=data.alipay;
	}
    //校验
	if(!mobile||!account||!type){
        showMessageForFail("生成失败,参数不可为空" );
        return;
	}
	if(files.length==0){
		showMessageForFail("请先上传TXT文件");
		return;
	}
	if(files.length>1){
		showMessageForFail("只允许上传一个文件");
		return;
	}
	
    var formdata = new FormData();
    formdata.append("file",files[0]);
    bootbox.confirm("确定生成?", function (result) {
        if (result) {
			$.ajax({
			    type: "POST",
			    dataType: 'JSON',
			    url: '/r/mobile/cloud/genImportQR/'+$div.find("#mobile").val()+'/'+account+'/'+type,
			    async: false,
			    data: formdata,
                cache: false,
                processData: false,
                contentType: false, 
			    success: function (jsonObject) {
			        if (jsonObject.status == 1) {
			        	 showMessageForSuccess("生成成功：" + jsonObject.message);
			        } else {
			            showMessageForFail("生成失败：" + jsonObject.message);
			        }
			    }
			});
        }
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
    });
 
}

/** 按输入生成收款码 */
var searchBindQR_genPrintQR=function(){
	var $div=$("#searchBindQR");
	var data = findMobileFromCloud($div.find("#mobile").val());
	var type=$div.find("#searchBindQR_tabType").val();
	var $amount=$div.find("[name=amount]");
	var account;
	if(type==accountType.InWechat.typeId){
		account=data.wechat;
	}else if(type==accountType.InAli.typeId){
		account=data.alipay;
	}
	//校验
	var validate=[
		{ele:$amount,name:'输入金额'}
		];
	if(!validateEmptyBatch(validate)){
		return;
	}
	bootbox.confirm("确定生成?", function (result) {
		if (result) {
			$.ajax({
				type: "POST",
				dataType: 'JSON',
				url: '/r/mobile/cloud/genExactQR',
				async: false,
				data: {
					mobile:$div.find("#mobile").val(),
					account:account,
					accountType:type,
					amounts:$amount.val()
				},
				success: function (jsonObject) {
					if (jsonObject.status == 1) {
						showMessageForSuccess("生成成功：");
					} else {
						showMessageForFail("生成失败：" + jsonObject.message);
					}
				}
			});
		}
	});
}

/** 按间隔生成收款码 */
var searchBindQR_genQR=function(){
	var $div=$("#searchBindQR");
	var data = findMobileFromCloud($div.find("#mobile").val());
	var type=$div.find("#searchBindQR_tabType").val();
	var $amtBegin=$div.find("[name=smallAmount]");
	var $amtEnd=$div.find("[name=limitAmount]");
	var $step=$div.find("[name=stepAmount]");
	var account;
	if(type==accountType.InWechat.typeId){
		account=data.wechat;
	}else if(type==accountType.InAli.typeId){
		account=data.alipay;
	}
    //校验
    var validate=[
    	{ele:$amtBegin,name:'小额部分',type:'positiveInt',minEQ:1,maxEQ:50000},
    	{ele:$amtEnd,name:'最高限额',min:1,maxEQ:50000},
    	{ele:$step,name:'间隔（超过小额部分）',type:'positiveInt',minEQ:1,maxEQ:10000}
    ];
    if(!validateEmptyBatch(validate)
    		||!validateInput(validate)){
    	return;
    }
    if($amtBegin.val()*1>$amtEnd.val()*1){
        showMessageForFail("小额部分不可以高于最高限额！");
        return;
    }
    bootbox.confirm("确定生成?", function (result) {
        if (result) {
			$.ajax({
			    type: "POST",
			    dataType: 'JSON',
			    url: '/r/mobile/cloud/genQR',
			    async: false,
			    data: {
			        mobile:$div.find("#mobile").val(),
			        account:account,
			        accountType:type,
			        amtBegin:$amtBegin.val(),
			        amtEnd:$amtEnd.val(),
			        step:$step.val()
			    },
			    success: function (jsonObject) {
			        if (jsonObject.status == 1) {
			        	 showMessageForSuccess("生成成功：");
			        } else {
			            showMessageForFail("生成失败：" + jsonObject.message);
			        }
			    }
			});
        }
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
    });
}

/** 删除手机号 */
var do_deletePhoneNo=function(mobile){
	bootbox.confirm("确定删除手机号 ?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/mobile/cloud/delete',
				async:false,
				data:{
					"mobile":mobile
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	//操作成功提示
			        	showMessageForSuccess("删除成功");
			        	//刷新数据列表
			        	showPage();
			        }else{
			        	showMessageForFail("删除失败："+jsonObject.message);
			        }
			    }
			});
		}
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
	});
	
}

/** 新增手机号 */
var doAddPhone = function () {
      var $div = $("#modalAddPhone");
      var $handicapId = $div.find("[name=handicap_select]");
      var $mobile = $div.find("[name=mobile]");
      var $owner = $div.find("[name=owner]");
      var $bankCard = $div.find("[name=bankCard]");
      var $bankCardOwner = $div.find("[name=bankCardOwner]");
      var $bankType = $div.find("select[name='choiceBankBrand']");
      var $bankCardName = $div.find("[name=bankCardName]");
      var $bankLimitBalance = $div.find("[name=bankLimitBalance]");
      var $wechat = $div.find("[name=wechat]");
      var $wechatOwner = $div.find("[name=wechatOwner]");
      var $wechatLimitIn = $div.find("[name=wechatLimitIn]");
      var $wechatLimitBalance = $div.find("[name=wechatLimitBalance]");
      var $alipay = $div.find("[name=alipay]");
      var $alipayOwner = $div.find("[name=alipayOwner]");
      var $alipayLimitIn = $div.find("[name=alipayLimitIn]");
      var $alipayLimitBalance = $div.find("[name=alipayLimitBalance]");
      var $type = $div.find("[name=type]:checked");
      var $level = $div.find("[name=level]:checked");
      var $creditLimit = $div.find("[name=creditLimit]");
      var $bonusCard = $div.find("[name=bonusCard]");
      var $bonusCardOwner = $div.find("[name=bonusCardOwner]");
      var $bonusCardName = $div.find("[name=bonusCardName]");
      var validate = [
          {ele: $handicapId, name: '盘口'},
          {ele: $level, name: '内外层'},
          {ele: $mobile, name: '手机号'},
          {ele: $owner, name: '联系人', minLength: 2, maxLength: 10},
          {ele: $type, name: '手机号类型'}
      ];
      if($wechat.val()){
    	  validate.push({ele: $wechatOwner,name:'微信姓名'});
    	  validate.push({ele: $wechatLimitIn,name:'微信当日入款限额'});
    	  validate.push({ele: $wechatLimitBalance,name:'微信余额告警'});
      }
      if($alipay.val()){
    	  validate.push({ele: $alipayOwner,name:'支付宝姓名'});
    	  validate.push({ele: $alipayLimitIn,name:'支付宝当日入款限额'});
    	  validate.push({ele: $alipayLimitBalance,name:'支付宝余额告警'});
      }
      if($bankCard.val()){
    	  validate.push({ele: $bankCardOwner,name:'银行卡开户人'});
    	  validate.push({ele: $bankType,name:'银行卡开户行 > 银行类别'});
    	  validate.push({ele: $bankCardName,name:'银行卡开户行 > 开户支行'});
    	  validate.push({ele: $bankLimitBalance,name:'银行卡余额告警'});
      }
      if ($type.val() == MobileTypeCustomer) {//客户类型 必填信用额度,返佣账户
          validate.push({ele: $creditLimit, name: '信用额度'});
          validate.push({ele: $bonusCard, name: '返佣账号'});
          validate.push({ele: $bonusCardOwner, name: '返佣开户人'});
          validate.push({ele: $bonusCardName, name: '返佣开户行'});
      }
      if (!validateEmptyBatch(validate)) {//非空校验
          return;
      }
//      if($mobile.val().length!=11){
//  		showMessageForCheck("手机号长度应为11位！");
//  		return;
//      }
//      if($mobile.val().substr(0, 1)*1!=1){
//		showMessageForCheck("手机号必须以1开头！");
//		return;
//      }
      validate.push({ele: $bankCard, name: '银行卡账号', maxLength: 25});
      validate.push({ele: $bankCardOwner, name: '银行卡姓名', minLength: 2, maxLength: 10});
      validate.push({ele: $bankLimitBalance,name:'银行卡余额告警',type:'amountPlus',min:0,maxEQ:50000}),
      validate.push({ele: $wechat, name: '微信账号', maxLength: 25});
      validate.push({ele: $wechatOwner, name: '微信姓名', minLength: 2, maxLength: 10});
      validate.push({ele: $wechatLimitIn, name: '微信入款限额（当日）', type: 'amountPlus'});
      validate.push({ele: $wechatLimitBalance,name:'微信余额告警',type:'amountPlus',min:0,maxEQ:50000}),
      validate.push({ele: $alipay, name: '支付宝账号', maxLength: 25});
      validate.push({ele: $alipayOwner, name: '支付宝姓名', minLength: 2, maxLength: 10});
      validate.push({ele: $alipayLimitIn, name: '支付宝入款限额（当日）', type: 'amountPlus'});
      validate.push({ele: $alipayLimitBalance,name:'支付宝余额告警',type:'amountPlus',min:0,maxEQ:50000});
      if ($type.val() == MobileTypeCustomer) {//客户类型 校验信用额度,返佣账户
          	validate.push({ele: $creditLimit, name: '信用额度', type: 'amountPlus'});
          	validate.push({ele: $bonusCard, name: '返佣账号',maxLength:25});
          	validate.push({ele: $bonusCardOwner, name: '返佣开户人',minLength:2,maxLength:10});
          	validate.push({ele: $bonusCardName, name: '返佣开户行',maxLength:50});
      }
      if (!validateInput(validate)) {//输入校验
          return;
      }
      var wechatTransOutType=0,alipayTransOutType=0;
      if($div.find("input[name='wechatTransOutType']:checked").length==1){
    	  wechatTransOutType=$div.find("input[name='wechatTransOutType']:checked").val();
      }
      if($div.find("input[name='alipayTransOutType']:checked").length==1){
    	  alipayTransOutType=$div.find("input[name='alipayTransOutType']:checked").val();
      }
	bootbox.confirm("确定新增手机号 ?", function (result) {
		if (result) {
			 var params = {
		        handicapId: $handicapId.val(),
		        mobile: $mobile.val(),
		        owner: $owner.val(),
		        type: $type.val(),
		        level: $level.val(),
		        creditLimit: $creditLimit.val(),
		        bankAcc: $bankCard.val(),
		        bankOwner: $bankCardOwner.val(),
		        bankType: $bankType.val(),
		        bankName: $bankCardName.val(),
		        bankLimitBalance:$bankLimitBalance.val(),
		        wecAcc: $wechat.val(),
		        wecOwner: $wechatOwner.val(),
		        wecInLimitDaily: $wechatLimitIn.val(),
		        wechatLimitBalance:$wechatLimitBalance.val(),
		        wechatTransOutType:wechatTransOutType,
		        aliAcc: $alipay.val(),
		        aliOwner: $alipayOwner.val(),
		        aliInLimitDaily: $alipayLimitIn.val(),
		        alipayLimitBalance:$alipayLimitBalance.val(),
		        alipayTransOutType:alipayTransOutType,
		        bonusCard: $div.find("[name=bonusCard]").val(),
		        bonusCardOwner:$div.find("[name=bonusCardOwner]").val(),
		        bonusCardName:$div.find("[name=bonusCardName]").val()
		    };
		    $.ajax({
		        type: "PUT",
		        dataType: 'JSON',
		        url: '/r/mobile/cloud/put',
		        async: false,
		        data: params,
		        success: function (jsonObject) {
		            if (jsonObject.status == 1) {
		                showMessageForSuccess("新增成功");
		                $div.modal("toggle");
		                showPage();
		            } else {
		                showMessageForFail("新增失败：" + jsonObject.message);
		            }
		        }
		    });
	    }
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
	   
    });
}

/** 修改手机号基本信息 */
var doUpdateMobile = function () {
    var $div = $("#updateMobile");
    var $mobile = $div.find("[name=mobile]");
    var $creditLimit = $div.find("[name=creditLimit]");
    var $owner = $div.find("[name=owner]");
    var $bonusCard = $div.find("[name=bonusCard]");
    var $bonusCardOwner = $div.find("[name=bonusCardOwner]");
    var $bonusCardName = $div.find("[name=bonusCardName]");
    var $type = $div.find("[name=type]:checked");
    var validate = [
        {ele: $mobile, name: '手机号'},
        {ele: $owner, name: '联系人'}
    ];
    if ($type.val() == MobileTypeCustomer) {//客户类型 必填信用额度,返佣账户
        validate.push({ele: $creditLimit, name: '信用额度'});
        validate.push({ele: $bonusCard, name: '返佣账号'});
        validate.push({ele: $bonusCardOwner, name: '返佣开户人'});
        validate.push({ele: $bonusCardName, name: '返佣开户行'});
    }
    if (!validateEmptyBatch(validate)) {//非空校验
        return;
    }
    var validatePrint = [
        {ele: $owner, name: '联系人', minLength: 2, maxLength: 10}
    ];
    if ($type.val() == MobileTypeCustomer) {//客户类型 校验信用额度,返佣账户
    	validatePrint.push({ele: $creditLimit, name: '信用额度', type: 'amountPlus'});
    	validatePrint.push({ele: $bonusCard, name: '返佣账号',maxLength:25});
    	validatePrint.push({ele: $bonusCardOwner, name: '返佣开户人',minLength:2,maxLength:10});
    	validatePrint.push({ele: $bonusCardName, name: '返佣开户行',maxLength:50});
    }
//    if($mobile.val().length!=11){
//  		showMessageForCheck("手机号长度应为11位！");
//  		return;
//    }
//
//    if($mobile.val().substr(0, 1)*1!=1){
//		showMessageForCheck("手机号必须以1开头！");
//		return;
//    }
    if (!validateInput(validatePrint)) {//输入校验
        return;
    }
	bootbox.confirm("确定修改 ?", function (result) {
	  if (result) {
		  var params = {
			    	mobileId:$div.find("#mobileId").val(),
			        mobile: $mobile.val(),
			        status: $div.find("[name=status]:checked").val(),
			        creditLimit: $creditLimit.val(),
			        level: $div.find("[name=level]:checked").val(),
			        owner: $owner.val(),
			        bonusAccount: $bonusCard.val(),
			        bonusOwner:$bonusCardOwner.val(),
			        bonusBankName:$bonusCardName.val()
			    };
			    $.ajax({
			        type: "PUT",
			        dataType: 'JSON',
			        url: '/r/mobile/cloud/updBase',
			        async: false,
			        data: params,
			        success: function (jsonObject) {
			            if (jsonObject.status == 1) {
			                showMessageForSuccess("修改成功");
			                $div.modal("toggle");
			                showPage();
			            } else {
			                showMessageForFail("修改失败：" + jsonObject.message);
			            }
			        }
			    });
	  }
    });
}

/** 修改已绑账号状态 */
var doUpdateBindStatus = function () {
    var $div = $("#updateBindStatus");
    var tabType = $div.find("#updateBindAccount_tabType").val();
    var type;
    if(tabType=='bank'){
    	type=accountType.BindCustomer.typeId;
    }else if(tabType=='wechat'){
    	type=accountType.InWechat.typeId;
    }else if(tabType=='alipay'){
    	type=accountType.InAli.typeId;
    }else{
    	return;
    }
	bootbox.confirm("确定修改 ?", function (result) {
	  if (!result) {
        return;
	  }
	    var params = {
	        mobile:$div.find("#mobile").val(),
	        account:$div.find("#account").val(),
	        type:type,
	        status: $div.find("[name=status]:checked").val()
	    };
	    $.ajax({
	        type: "PUT",
	        dataType: 'JSON',
	        url: '/r/mobile/cloud/updAccStatus',
	        async: false,
	        data: params,
	        success: function (jsonObject) {
	            if (jsonObject.status == 1) {
	                showMessageForSuccess("修改成功");
	                $div.modal("toggle");
	                showPage();
	            } else {
	                showMessageForFail("修改失败：" + jsonObject.message);
	            }
	        }
	    });
  });
};

/** 修改已绑账号基本信息 */
var doUpdateBindAccount = function () {
	var $div = $("#updateBindAccount");
	var tabType = $div.find("#updateBindAccount_tabType").val();
	var type;
	var params;
	var transOutType;
	if(tabType=='bank'){
		var $table=$div.find("#bankInfo");
		var $account = $table.find("[name=account]");
	    var $owner = $table.find("[name=owner]");
	    var $bankType = $table.find("[name=choiceBankBrand]");
	    var $accountName = $table.find("[name=bankName]");
	    var $limitBalance = $table.find("[name=limitBalance]");
		type=accountType.BindCustomer.typeId;
		var validate=[
	    	{ele: $account,name:'账号'},
			{ele: $owner, name: '开户人'},
			{ele: $bankType, name: '开户行 > 银行类别'},
			{ele: $accountName, name: '开户行 >支行'},
			{ele: $limitBalance, name: '余额告警'}
		]
		if (!validateEmptyBatch(validate)) {//非空校验
	        return;
	    }
		var validatePrint=[
	    	{ele:$account,name:'账号',maxLength:25},
			{ele: $owner, name: '开户人',minLength:2,maxLength:10},
			{ele: $accountName, name: '开户行',maxLength:50},
			{ele: $limitBalance, name: '余额告警',type:'amountPlus',min:0,maxEQ:50000}
		]
		if (!validateInput(validatePrint)) {//输入校验
	        return;
	    }
		params = {
				mobile:$div.find("#mobile").val(),
				account:$account.val(),
				type:type,
				owner:$owner.val(),
				bankType:$bankType.val(),
				accountName:$accountName.val(),
				limitBalance:$limitBalance.val()
		};
	}else{
		if(tabType=='wechat'){
			type=accountType.InWechat.typeId;
		}else if(tabType=='alipay'){
			type=accountType.InAli.typeId;
		}else{
			return;
		}
		var $table=$div.find("#wechatAlipayInfo");
		var $account = $table.find("[name=account]");
	    var $owner = $table.find("[name=owner]");
	    var $limitInDaily = $div.find("[name=limitInDaily]");
	    var $limitBalance = $table.find("[name=limitBalance]");
		var validate=[
	    	{ele: $account,name:'账号'},
			{ele: $owner, name: '姓名'},
	    	{ele: $limitInDaily,name:'当日入款限额'},
	    	{ele: $limitBalance,name:'余额告警'}
		]
		if (!validateEmptyBatch(validate)) {//非空校验
	        return;
	    }
		var validatePrint=[
	    	{ele:$account,name:'账号',maxLength:25},
			{ele: $owner, name: '姓名',minLength:2,maxLength:10},
			{ele: $limitInDaily, name: '当日入款限额',type:'amountPlus'},
			{ele: $limitBalance, name: '余额告警',type:'amountPlus',min:0,maxEQ:50000}
		]
		 if (!validateInput(validatePrint)) {//输入校验
	        return;
	    }
	    if($div.find("input[name='transOutType']:checked").length==1){
	    	transOutType=$div.find("input[name='transOutType']:checked").val();
	    }else{
	    	transOutType=0;
	    }
		params = {
				mobile:$div.find("#mobile").val(),
				account:$account.val(),
				transOutType:transOutType,
				type:type,
				owner:$owner.val(),
				limitInDaily:$limitInDaily.val(),
				limitBalance:$limitBalance.val()
		};
	}
	bootbox.confirm("确定修改 ?", function (result) {
		if (!result) {
			return;
		}
		$.ajax({
			type: "PUT",
			dataType: 'JSON',
			url: '/r/mobile/cloud/updAcc',
			async: false,
			data: params,
			success: function (jsonObject) {
				if (jsonObject.status == 1) {
					showMessageForSuccess("修改成功");
					$div.modal("toggle");
					showPage();
				} else {
					showMessageForFail("修改失败：" + jsonObject.message);
				}
			}
		});
	});
};

/** 修改已绑账号密码 */
var doUpdatePWD=function(){
	var $div=$("#updatePWD");
	 bootbox.confirm("确定修改密码信息?", function (result) {
         if (result) {
             var data = {
                 bank: $div.find("[name=bank]").val(),
                 singBank: $div.find("[name=singBank]").val(),
                 pingBank: $div.find("[name=pingBank]").val(),
                 bingBank: $div.find("[name=bingBank]").val(),
                 uingBank: $div.find("[name=uingBank]").val(),
                 wechat: $div.find("[name=wechat]").val(),
                 singWechat: $div.find("[name=singWechat]").val(),
                 pingWechat: $div.find("[name=pingWechat]").val(),
                 alipay: $div.find("[name=alipay]").val(),
                 singAlipay: $div.find("[name=singAlipay]").val(),
                 pingAlipay: $div.find("[name=pingAlipay]").val()
             };
             $.ajax({
                 type: "PUT",
                 dataType: 'JSON',
                 url: '/r/mobile/cloud/updPwd',
                 async: false,
                 data: data,
                 success: function (jsonObject) {
                     if (jsonObject.status == 1) {
                         showMessageForSuccess("修改成功");
                         $div.modal("toggle");
                         showPage();
                     } else {
                         showMessageForFail("修改失败：" + jsonObject.message);
                     }
                 }
             });
         }
         setTimeout(function(){       
             $('body').addClass('modal-open');
         },500);
     });
}

getHandicapByPerm($("select[name='search_EQ_handicapId']"));
initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #searchBtn"),70,"refresh_phoneNumber");
$("#refreshAccountListSelect [name=refreshSelect]").val(10);
$("#refreshAccountListSelect [name=refreshSelect]").change();
$("#accountFilter [name=type],[name=status]").click(function(){
	showPage(0);
});
$("[name=search_EQ_handicapId],[name=search_EQ_level]").change(function(){
	showPage(0);
});
$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		showPage();
	}
});
$("#searchBindQR_filter").keypress(function(e){
	if(event.keyCode == 13) {
		$("#searchBindQR_filter #searchBtn").click();
	}
});
showPage(0);