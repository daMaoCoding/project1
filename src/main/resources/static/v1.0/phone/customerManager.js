var bankTypeList4Customer=[];
var MobileTypeCustomer4Customer=0,MobileTypeSelf4Customer=1;
var HandicapBatch=getHandicapBatchInfoByCode();


/** 客户资料列表 */
var showPage = function (CurPage) {
    var $div = $("#accountFilter");
    if (!CurPage && CurPage != 0) CurPage = $("#phoneNumber_page").find(".Current_Page").text();
    var oid=$.trim($div.find("select[name=search_EQ_handicapCode]").val());
    if(!oid){
    	return
    }
    var params = {
        pageNo: CurPage <= 0 ? 0 : CurPage - 1,
        pageSize: $.session.get('initPageSize')?$.session.get('initPageSize'):10,
        oid:oid,//盘口编码
        level:$.trim($div.find("[name=currSysLevel]:checked").val()),// 0：外层，8：指定层，2：内层，不填表示全部
        bankAccount:$.trim($div.find("[name=search_LIKE_bankCard]").val()),// 银行卡账号
        wechatAccount:$.trim($div.find("[name=search_LIKE_wechat]").val()),// 微信账号
        alipayAccount:$.trim($div.find("[name=search_LIKE_alipay]").val()),// 支付宝账号
        tel:$.trim($div.find("[name=search_LIKE_mobileNo]").val())// 联系电话
    };
    var typeArray = [],statusArray = [];
    $div.find("[name=type]:checked").map(function () {
    	typeArray.push($(this).val());
    });
    $div.find("[name=status]:checked").map(function () {
        statusArray.push($(this).val());
    });
    params.types = typeArray.toString();
    params.statuses = statusArray.toString();
    $.ajax({
        contentType: 'application/json;charset=UTF-8',
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/newpay/findByCondition",
        data: JSON.stringify(params),
        success: function (jsonObject) {
            if (jsonObject.status != 1) {
                showMessageForFail("查询失败：" + jsonObject.message);
                return;
            }
            var $tbody = $("table#accountListTable").find("tbody").html("");
            var trs="",list4accountInfoHover = [], list4MobileInfoHover=[],mobileArray = [];
            $.map(jsonObject.data, function (record) {
            	mobileArray.push(record.tel);
            	list4MobileInfoHover.push({oid:record.oid,id:record.id});
            	//数据填充
                var tr = "<td>" + (HandicapBatch&&HandicapBatch[record.oid]?HandicapBatch[record.oid].name:"") + "</td>";
                tr+="<td>";
                tr+="<a class='bind_hover_card' data-placement='auto right' data-trigger='hover' data-toggle='mobileInfoHover" + record.id + "' >";
            	tr+="<span>" + record.contactName + "</span><br/>" ;
                tr+="<span>" + hidePhoneNumber(record.tel) + "</span>";
                tr+="</a>";
                tr+="</td>";
                tr+="<td>";
            	if(record.status==1){//1启用 0停用
            		tr+="<span class='label label-sm label-success'>在用</span><br/><br/>";
            	}else{
            		tr+="<span class='label label-sm label-danger'>停用</span><br/><br/>";
            	}
            	if(record.type==MobileTypeCustomer4Customer){//0客户 1 自用
            		tr+="<span class='label label-white middle  label-info'>客户</span>";
            	}else{
            		tr+="<span class='label label-purple middle  label-info'>自用</span>";
            	}
                tr+="</td>";
            	if(record.bankAccountId){
                	list4accountInfoHover.push({oid:record.oid,id:record.id,type:2});
                    tr += "<td>";
                	if(record.bkStatus==1){//1启用 0停用
                		tr+="<span class='label label-sm label-success'>在用</span></br>";
                	}else{
                		tr+="<span class='label label-sm label-danger'>停用</span></br>";
                	}
                    tr += "<a class='bind_hover_card' data-trigger='hover'  data-placement='auto right' data-toggle='bankInfoHover" + record.id + "'>" + record.bankAccount + "</a>";
                    tr += "</td>";
            	} else {
                    tr += "<td>--/--</td>";
                }
            	if(record.wechatAccountId){
                	list4accountInfoHover.push({oid:record.oid,id:record.id,type:0});
            		tr += "<td>";
                	if(record.wxStatus==1){//1启用 0停用
                		tr+="<span class='label label-sm label-success'>在用</span></br>";
                	}else{
                		tr+="<span class='label label-sm label-danger'>停用</span></br>";
                	}
            		tr += "<a class='bind_hover_card' data-trigger='hover'  data-placement='auto right' data-toggle='wechatInfoHover" + record.id + "'>" + record.wechatAccount + "</a>";
            		tr += getDeviceStatus(record.wechatStatus);
            		tr += "</td>";
            	} else {
                    tr += "<td>--/--</td>";
                }
            	if(record.alipayAccountId){
                	list4accountInfoHover.push({oid:record.oid,id:record.id,type:1});
            		tr += "<td>";
                	if(record.zfbStatus==1){//1启用 0停用
                		tr+="<span class='label label-sm label-success'>在用</span></br>";
                	}else{
                		tr+="<span class='label label-sm label-danger'>停用</span></br>";
                	}
            		tr += "<a class='bind_hover_card' data-trigger='hover'  data-placement='auto right' data-toggle='alipayInfoHover" + record.id + "'>" + record.alipayAccount + "</a>";
            		tr += getAlipayDeviceStatus(record.alipayStatus,record.wapStatus);
            		tr += "</td>";
                	tr += "<td>"+(record.isEpAlipay&&record.isEpAlipay==1?"是":"否")+"</td>";
            	} else {
                    tr += "<td>--/--</td>";
                    tr += "<td>--</td>";
                }
                //可用额度
            	tr+="<td>";
            	tr+="<span title='信用额度'>"+(record.credits?setAmountAccuracy(record.credits):"--")+"<span><br/>";
            	tr+="<span title='待确认额度'>"+(record.unconfirmOutmoney?setAmountAccuracy(record.unconfirmOutmoney):"--")+"<span><br/>";
            	tr+="<span title='可用额度'>"+(record.availableCredits?setAmountAccuracy(record.availableCredits):"--")+"<span>";
            	tr+="</td>";
            	//银行余额/系统余额
            	tr+="<td>";
            	tr+="<span style='float:left'>&nbsp;&nbsp;总额</span>";
            	tr+="<span style='float:right'>"+(record.balance?setAmountAccuracy(record.balance*1+record.ylbBalance*1):"--")+"/"+(record.sysBalance?setAmountAccuracy(record.sysBalance*1+record.sysYlbBalance*1):"--")+"&nbsp;&nbsp;</span><br/>";
            	tr+="<span style='float:left'>&nbsp;&nbsp;微信</span>";
            	tr+="<span style='float:right'>"+(record.wxBalance?setAmountAccuracy(record.wxBalance):"--")+"/"+(record.sysWxBalance?setAmountAccuracy(record.sysWxBalance):"--")+"&nbsp;&nbsp;</span><br/>";
            	tr+="<span style='float:left'>&nbsp;&nbsp;银行卡</span>";
            	tr+="<span style='float:right'>"+(record.bankBalance?setAmountAccuracy(record.bankBalance):"--")+"/"+(record.sysBankBalance?setAmountAccuracy(record.sysBankBalance):"--")+"&nbsp;&nbsp;</span><br/>";
            	tr+="<span style='float:left'>&nbsp;&nbsp;支付宝</span>";
            	tr+="<span style='float:right'>"+(record.zfbBalance?setAmountAccuracy(record.zfbBalance):"--")+"/"+(record.sysZfbBalance?setAmountAccuracy(record.sysZfbBalance):"--")+"&nbsp;&nbsp;</span><br/>";
            	tr+="<span style='float:left'>&nbsp;&nbsp;余利宝</span>";
            	tr+="<span style='float:right'>"+(record.ylbBalance?setAmountAccuracy(record.ylbBalance):"--")+"/"+(record.sysYlbBalance?setAmountAccuracy(record.sysYlbBalance):"--")+"&nbsp;&nbsp;</span><br/>";
            	tr+="</td>";
            	//收款
            	tr+="<td>";
            	tr+="<a style='text-decoration:none;' target='_self' href='#/CustomerDetail:*?oid="+record.oid+"&id="+record.id+"&type=in'>"+
        				"<span title='今日收款'>"+(record.todayInCount?record.todayInCount:"--")+
        			"</a><br/>";
            	tr+="<a style='text-decoration:none;' target='_self' href='#/CustomerDetail:*?oid="+record.oid+"&id="+record.id+"&type=out'>"+
	            		"</span><span title='今日出款'>"+(record.todayOutCount?record.todayOutCount:"--")+
	            	"</a><br/>";
            	tr+="<a style='text-decoration:none;' target='_self' href='#/CustomerDetail:*?oid="+record.oid+"&id="+record.id+"&type=commission'>"+
	            		"</span><span title='累计已获得的确认佣金'>"+(record.commission?record.commission:"--")+"</span>"+
	            	"</a>";
            	tr+="</td>";
            	//账号操作
            	tr+= "<td>";
  			  	tr+= "<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:updateBindInfo:*'\
		  				onclick='showModalUpdateBindAccount(" + record.oid + "," + record.id + ",\"" + record.tel + "\")'>\
		  		 		<span>修改</span>\
  			  		</a>";
  			  	tr+= "<a class='btn btn-xs btn-white btn-danger btn-bold red contentRight' contentright='IncomePhoneNumber:updateBindPWD:*' \
  			  			onclick='showModalUpdatePWD(" + record.oid + "," + record.id + ")'>\
	  					<span>密码</span>\
  			  		</a>";
                tr+= "<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:updateBindStatus:*'" +
                		" onclick='showModalUpdateBindStatus(" + record.oid + "," + record.id + ")'>" +
                		"<span>状态</span>" +
                	"</a>";
                tr+= "<a class='btn btn-xs btn-white btn-success btn-bold green contentRight' contentright='IncomePhoneNumber:settingPO:*' " +
                		"onclick='showModalSettingPO(" + record.oid + "," + record.id + ")'>" +
                		"<span>通道</span>" +
                	"</a>";
                tr+= "<a class='btn btn-xs btn-white btn-success btn-bold green'  " +
		                "onclick='showModalQrBindAccount(" + record.oid + "," + record.id + "," + record.tel + ")'>" +
		                "<span>设备号</span>" +
	                "</a>";
                tr+= "<a class='btn btn-xs btn-white btn-success btn-bold green contentRight' contentright='IncomePhoneNumber:searchQR:*' " +
		                "onclick='showModalPageForQr(" + record.oid + "," + record.id + ")'>" +
		                "<span>收款码</span>" +
	                "</a>";
            	//客户类型可重置信用额度
            	if(record.type==MobileTypeCustomer4Customer){
                    tr+= "<a class='btn btn-xs btn-white btn-success btn-bold green contentRight' contentright='IncomePhoneNumber:resetCredits:*' \
	                	onclick='showModalResetCredits(" + record.oid + "," + record.id + ");' >\
	                	<span>重置额度</span>\
	                </a>";
            	}
                tr+= "</td>";
                //手机号操作
                tr+= "<td>";
                tr+= "<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:updateMobile:*' \
                		onclick='showModalUpdateMobile(" + record.oid + "," + record.id + ");' >\
        				<span>修改</span>\
            		</a>";
//                tr+="<a class='btn btn-xs btn-white btn-danger btn-bold red contentRight' contentRight='IncomePhoneNumber:deleteMobile:*' " +
//						"onclick='do_deletePhoneNo(" + record.oid + "," + record.id + ")'>" +
//						"<span>删除</span>" +
//					"</a>";
                tr+= "<a title='修改未确认出款金额开关' class='btn btn-xs btn-white btn-success btn-bold blue' " +
			                "onclick='showModalModifyUoFlag(" + record.oid + "," + record.id + ");' >" +
			                "<span>未确认开关</span>" +
		                "</a>";
                tr += "</td>";
				trs+=("<tr id='mainTr" + record.id + "'>" + tr + "</tr>");
            });
            $tbody.append($(trs));
            showPading(jsonObject.page, "phoneNumber_page", showPage, null, true);
            $("#phoneNumber_page").find(".showImgTips").children().each(function (index, record) {
                if (index <= 4) {
                    $(record).remove();
                }
            });
            loadHover_wechat_bankInfoHover(list4accountInfoHover);
            loadHover_MobileInfoHover(list4MobileInfoHover);
            contentRight();
//            bonusTotalForeach($tbody, mobileArray);
        }
    });
};

/** 新增客户资料基本信息 */
var showModalAddPhone = function () {
    var $div = $("#addPhoneNo4clone").clone().attr("id", "modalAddPhone");
    $div.find("td").css("padding-top", "5px");
    getHandicapCode_select($div.find("select[name='handicap_select']"));
    $div.modal("toggle");
	//加载银行品牌
	var nbsp='&nbsp;&nbsp;&nbsp;&nbsp;';
	var options="";
	options+="<option value='' >"+nbsp+"请选择"+nbsp+"</option>";
	$.each(bankTypeList4Customer,function(index,record){
		options+="<option value="+record.id+" bankName="+record.bankName+" >"+nbsp+record.bankName+nbsp+"</option>";
	});
	$div.find("select[name=choiceBankBrand]").html(options);
    $div.find("[name=type]").change(function () {
        if ($div.find("[name=type]:checked").val() == MobileTypeCustomer4Customer) {//客户
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
}
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
      var $ylbInterval=$div.find("[name=ylbInterval]");
      var $zfbAlias1=$div.find("[name=zfbAlias1]");
      var $zfbAlias2=$div.find("[name=zfbAlias2]");
      var $wxAlias1=$div.find("[name=wxAlias1]");
      var $wxAlias2=$div.find("[name=wxAlias2]");
      var $wxAlias3=$div.find("[name=wxAlias3]");
      var $uid=$div.find("[name=uid]");
      var $isEpAlipay=$div.find("[name=isEpAlipay]:checked");
      var validate = [
          {ele: $handicapId, name: '盘口'},
          {ele: $level, name: '内外层'},
          {ele: $mobile, name: '手机号'},
          {ele: $owner, name: '联系人', minLength: 2, maxLength: 50},
          {ele: $type, name: '手机号类型'}
      ];
      if($wechat.val()){
    	  validate.push({ele: $wechatOwner,name:'微信姓名'});
    	  validate.push({ele: $wechatLimitIn,name:'微信当日入款限额'});
    	  validate.push({ele: $wechatLimitBalance,name:'微信提现额度'});
      }
      if($alipay.val()){
    	  validate.push({ele: $alipayOwner,name:'支付宝姓名'});
    	  validate.push({ele: $alipayLimitIn,name:'支付宝当日入款限额'});
    	  validate.push({ele: $alipayLimitBalance,name:'支付宝提现额度'});
    	  validate.push({ele: $uid,name:'支付宝UID'});
    	  validate.push({ele: $isEpAlipay,name:'是/否 企业支付宝'});
      }
      if($bankCard.val()){
    	  validate.push({ele: $bankCardOwner,name:'银行卡开户人'});
    	  validate.push({ele: $bankType,name:'银行卡开户行 > 银行类别'});
    	  validate.push({ele: $bankCardName,name:'银行卡开户行 > 开户支行'});
    	  validate.push({ele: $bankLimitBalance,name:'银行卡提现额度'});
      }
      if ($type.val() == MobileTypeCustomer4Customer) {//客户类型 必填信用额度,返佣账户
          validate.push({ele: $creditLimit, name: '信用额度'});
          validate.push({ele: $bonusCard, name: '返佣账号'});
          validate.push({ele: $bonusCardOwner, name: '返佣开户人'});
          validate.push({ele: $bonusCardName, name: '返佣开户行'});
      }
      if (!validateEmptyBatch(validate)) {//非空校验
          return;
      }
      validate.push({ele: $bankCard, name: '银行卡账号', maxLength: 25});
      validate.push({ele: $bankCardOwner, name: '银行卡姓名', minLength: 2, maxLength: 10});
      validate.push({ele: $bankLimitBalance,name:'银行卡提现额度',type:'amountPlus',min:0,maxEQ:50000}),
      validate.push({ele: $wechat, name: '微信账号', maxLength: 25});
      validate.push({ele: $wechatOwner, name: '微信姓名', minLength: 2, maxLength: 10});
      validate.push({ele: $wechatLimitIn, name: '微信入款限额（当日）', type: 'amountPlus'});
      validate.push({ele: $wechatLimitBalance,name:'微信提现额度',type:'amountPlus',min:0,maxEQ:50000}),
      validate.push({ele: $alipay, name: '支付宝账号', maxLength: 25});
      validate.push({ele: $alipayOwner, name: '支付宝姓名', minLength: 2, maxLength: 50});
      validate.push({ele: $alipayLimitIn, name: '支付宝入款限额（当日）', type: 'amountPlus'});
      validate.push({ele: $alipayLimitBalance,name:'支付宝提现额度',type:'amountPlus',min:0,maxEQ:50000});
      validate.push({ele: $ylbInterval,name:'支付宝余额查询时间间隔',type:'amountPlus',minEQ:3,maxEQ:60});
      validate.push({ele: $zfbAlias1,name:'支付宝别名1',minLength:1,maxLength:50});
      validate.push({ele: $zfbAlias1,name:'支付宝别名2',minLength:1,maxLength:50});
      validate.push({ele: $wxAlias1,name:'微信别名1',minLength:1,maxLength:50});
      validate.push({ele: $wxAlias2,name:'微信别名2',minLength:1,maxLength:50});
      validate.push({ele: $wxAlias3,name:'微信别名3',minLength:1,maxLength:50});
      if ($type.val() == MobileTypeCustomer4Customer) {//客户类型 校验信用额度,返佣账户
          	validate.push({ele: $creditLimit, name: '信用额度', type: 'amountPlus'});
          	validate.push({ele: $bonusCard, name: '返佣账号',maxLength:25});
          	validate.push({ele: $bonusCardOwner, name: '返佣开户人',minLength:2,maxLength:10});
          	validate.push({ele: $bonusCardName, name: '返佣开户行',maxLength:50});
      }
      if (!validateInput(validate)) {//输入校验
          return;
      }
	bootbox.confirm("确定新增客户资料 ?", function (result) {
		if (result) {
			 var params = {
				oid:$handicapId.val(),//盘口编码
				contactName:$.trim($owner.val()),//联系人名
				tel:$.trim($mobile.val()),//联系电话
				credits:$.trim($creditLimit.val()),//信用额度
				type:$type.val(),//类型，0：客户，1：自用
				level:$level.val(),//内外层，0：外层，8：指定层，2：内层
				commissionBankNum:$.trim($bonusCard.val()),//返佣账号
				commissionOpenMan:$.trim($bonusCardOwner.val()),//返佣开户人
				commissionBankName:$.trim($bonusCardName.val()),//返佣开户行
				wechatAccount:$.trim($wechat.val()),//微信账号
				wechatName:$.trim($wechatOwner.val()),//微信姓名
				wechatInLimit:$.trim($wechatLimitIn.val()),//微信入款限额
				wechatBalanceAlarm:$.trim($wechatLimitBalance.val()),//微信提现额度
				wechatLoginPassword:$.trim($div.find("[name=singWechat]").val()),//微信登陆密码
				wechatPaymentPassword:$.trim($div.find("[name=pingWechat]").val()),//微信支付密码
				wechatQrDrawalMethod:$div.find("[name='wechatQrDrawalMethod']:checked").length==1?1:0,//微信提现方式：二维码提现（0：未使用，1：使用）
				wechatBankDrawalMethod:$div.find("[name='wechatBankDrawalMethod']:checked").length==1?1:0,//微信提现方式：银行卡提现（0：未使用，1：使用）
				alipayAccount:$.trim($alipay.val()),//支付宝账号
				alipayName:$.trim($alipayOwner.val()),//支付宝姓名
				alipayInLimit:$.trim($alipayLimitIn.val()),//支付宝入款限额
				alipayBalanceAlarm:$.trim($alipayLimitBalance.val()),//支付宝提现额度
				alipayLoginPassword:$.trim($div.find("[name=singAlipay]").val()),//支付宝登陆密码
				alipayPaymentPassword:$.trim($div.find("[name=pingAlipay]").val()),//支付宝支付密码
				alipayQrDrawalMethod:$div.find("[name='alipayQrDrawalMethod']:checked").length==1?1:0,//支付宝提现方式：二维码提现（0：未使用，1：使用）
				alipayBankDrawalMethod:$div.find("[name='alipayBankDrawalMethod']:checked").length==1?1:0,//支付宝提现方式：银行卡提现（0：未使用，1：使用）
				bankId:$bankType.val(),//银行id
				bankAccount:$.trim($bankCard.val()),//银行卡账号
				openMan:$.trim($bankCardOwner.val()),//开户人
				bankName:$.trim($div.find("[name=choiceBankBrand] option:selected").attr("bankName")),//银行名称（类型）
				bankOpen:$.trim($bankCardName.val()),//开户支行名称
				bankBalanceAlarm:$.trim($bankLimitBalance.val()),//银行卡提现额度
				bankLoginPassword:$.trim($div.find("[name=singBank]").val()),//银行卡登陆密码
				bankPaymentPassword:$.trim($div.find("[name=pingBank]").val()),//银行卡支付密码
				bankPassword:$.trim($div.find("[name=bingBank]").val()),//银行密码
				ushieldPassword:$.trim($div.find("[name=uingBank]").val()),//U盾密码
				ylbThreshold:$.trim($div.find("[name=ylbThreshold]").val()),//余利宝提现额度
				ylbInterval:$.trim($ylbInterval.val()),//余利宝余额查询时间间隔  分钟转为毫秒
				zfbAlias1:$.trim($zfbAlias1.val(),true),//支付宝别名1
				zfbAlias1:$.trim($zfbAlias2.val(),true),//支付宝别名2
				zfbAlias1:$.trim($wxAlias1.val(),true),//微信别名1
				zfbAlias1:$.trim($wxAlias2.val(),true),//微信别名2
				zfbAlias1:$.trim($wxAlias3.val(),true),//微信别名3
				uid:$.trim($uid.val(),true),//支付宝UID
				isEpAlipay:$.trim($isEpAlipay.val(),true)//是/否 企业支付宝
		    };
		    $.ajax({
		        type: "POST",
                contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/add',
		        async: false,
		        data: JSON.stringify(params),
		        success: function (jsonObject) {
		            if (jsonObject.status == 1) {
		            	if($.trim($bankCard.val())){
		            		//有银行账号 执行同步项目代码
			            	var currSysLevel=$level.val();
			            	if(currSysLevel==0){
			            		currSysLevel=currentSystemLevelOutter;
			            	}else if(currSysLevel==1){
			            		currSysLevel=currentSystemLevelMiddle;
			            	}else if(currSysLevel==2){
			            		currSysLevel=currentSystemLevelInner;
			            	}
							var remark="联系人："+$.trim($owner.val())+"&nbsp;&nbsp;类型："+($type.val()==0?"客户":"自用");
							if($type.val()==0){
								remark+="<br/>信用额度："+$.trim($creditLimit.val())+"&nbsp;&nbsp;返佣信息："
								+$.trim($bonusCardOwner.val())+"&nbsp;|&nbsp;"+$.trim($bonusCardName.val())+"&nbsp;|&nbsp;"+$.trim($bonusCard.val());
							}
			            	var data = {
			       				handicapCode:$handicapId.val(),//盘口编码
			       				currSysLevel:currSysLevel,
			       				account:$.trim($bankCard.val()),//银行卡账号
			       				owner:$.trim($bankCardOwner.val()),//开户人
			       				bankName:$.trim($bankCardName.val()),//开户支行名称
			       				mobile:$.trim($mobile.val()),//手机号
			       				limitBalance:$.trim($bankLimitBalance.val()),//银行卡提现额度
			       				hook:$.trim($div.find("[name=singBank]").val()),//银行卡登陆密码
			       				hub:$.trim($div.find("[name=pingBank]").val()),//银行卡支付密码
			       				bing:$.trim($div.find("[name=uingBank]").val()),//U盾密码
			       			}
			            	var selected_bankType=$.trim($div.find("[name=choiceBankBrand] option:selected").attr("bankName"));
			            	var bankType=getBankType4Customer(selected_bankType);
			            	if(bankType){
			            		data.bankType=bankType;
			            	}else{
			            		data.bankType=selected_bankType;
			            	}
				       		$.ajax({
				  		    	type: "POST",
				  		        dataType: 'JSON',
				  		        url: '/r/account/createAccount14',
				  		        async: false,
				  		        data: data,
				  		        success: function (jsonObject) {
				  		            if (jsonObject.status == 1) {
				  		            	console.log("同步到本系统成功");
				  		            }
				  		        }
				       		});
		            	}
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
};

/** 修改客户资料基本信息*/
var showModalUpdateMobile = function (handicapCode,mobileId) {
    var $div = $("#updateMobile4clone").clone().attr("id", "updateMobile");
    $div.find("td").css("padding-top", "10px");
    $div.modal("toggle");
    var data = getTelInfo(handicapCode,mobileId);
    //基本信息
    if(data){
        getHandicapCode_select($div.find("[name=handicap]"),handicapCode);
        $div.find("#mobileId").val(mobileId);
        $div.find("#old_mobile").val(data.tel);
        $div.find("[name=mobile]").val(data.tel);
        $div.find("[name=owner]").val(data.contactName);
        $div.find("[name=type][value=" + data.type + "]").prop("checked", true);
        $div.find("[name=level][value=" + data.level + "]").prop("checked", true);
        $div.find("[name=status][value=" + data.status + "]").prop("checked", true);
        $div.find("[name=creditLimit]").val(data.credits ? data.credits : 0);
        $div.find("[name=bonusCard]").val(data.commissionBankNum ? data.commissionBankNum : '');
        $div.find("[name=bonusCardOwner]").val(data.commissionOpenMan ? data.commissionOpenMan : '');
        $div.find("[name=bonusCardName]").val(data.commissionBankName ? data.commissionBankName : '');
    }
    //账号信息
    $div.find("[name=type]").change(function () {
        if ($div.find("[name=type]:checked").val() == MobileTypeCustomer4Customer) {//客户
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
}
var doUpdateMobile=function(){
	var $div = $("#updateMobile");
    var $handicap = $div.find("[name=handicap]");
    var $mobile = $div.find("[name=mobile]");
    var $creditLimit = $div.find("[name=creditLimit]");
    var $owner = $div.find("[name=owner]");
    var $bonusCard = $div.find("[name=bonusCard]");
    var $bonusCardOwner = $div.find("[name=bonusCardOwner]");
    var $bonusCardName = $div.find("[name=bonusCardName]");
    var $type = $div.find("[name=type]:checked");
    var validate = [
        {ele: $handicap, name: '盘口'},
        {ele: $mobile, name: '手机号'},
        {ele: $owner, name: '联系人'}
    ];
    if ($type.val() == MobileTypeCustomer4Customer) {//客户类型 必填信用额度,返佣账户
        validate.push({ele: $creditLimit, name: '信用额度'});
        validate.push({ele: $bonusCard, name: '返佣账号'});
        validate.push({ele: $bonusCardOwner, name: '返佣开户人'});
        validate.push({ele: $bonusCardName, name: '返佣开户行'});
    }
    if (!validateEmptyBatch(validate)) {//非空校验
        return;
    }
    var validatePrint = [
        {ele: $owner, name: '联系人', minLength: 2, maxLength: 50}
    ];
    if ($type.val() == MobileTypeCustomer4Customer) {//客户类型 校验信用额度,返佣账户
    	validatePrint.push({ele: $creditLimit, name: '信用额度', type: 'amountPlus'});
    	validatePrint.push({ele: $bonusCard, name: '返佣账号',maxLength:25});
    	validatePrint.push({ele: $bonusCardOwner, name: '返佣开户人',minLength:2,maxLength:10});
    	validatePrint.push({ele: $bonusCardName, name: '返佣开户行',maxLength:50});
    }
    if (!validateInput(validatePrint)) {//输入校验
        return;
    }
	bootbox.confirm("确定修改 ?", function (result) {
		if (result) {
			var params = {
				oid:$handicap.val(),
				id:$div.find("#mobileId").val(),
				contactName:$.trim($owner.val()),
				tel:$.trim($mobile.val()),
				status:$div.find("[name=status]:checked").val(),
				type:$type.val(),
				credits:$.trim($creditLimit.val()),
				level:$div.find("[name=level]:checked").val(),
				commissionOpenMan:$.trim($bonusCardOwner.val()),
				commissionBankName:$.trim($bonusCardName.val()),
				commissionBankNum:$.trim($bonusCard.val())
		    };
		    $.ajax({
		        type: "POST",
		        contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/modifyInfo',
		        async: false,
		        data: JSON.stringify(params),
		        success: function (jsonObject) {
		            if (jsonObject.status == 1) {
		            	//如果有绑定银行卡 更新本系统银行层级，如果手机停用，银行卡也停用
		            	var bankInfo=getAccountInfo2($handicap.val(),$div.find("#mobileId").val(),2);
		            	if(bankInfo&&bankInfo.account){
		            		//如果无变更无需发送更新请求
	            			var account=getAccountInfoByAcc(bankInfo.account);
	            			if(account){
	            				var newCurrSysLevel=$div.find("[name=level]:checked").val(),currSysLevel_change=false;
				            	if(newCurrSysLevel==0){
				            		newCurrSysLevel=currentSystemLevelOutter;
				            	}else if(newCurrSysLevel==1){
				            		newCurrSysLevel=currentSystemLevelMiddle;
				            	}else if(newCurrSysLevel==2){
				            		newCurrSysLevel=currentSystemLevelInner;
				            	}
				            	if(account.currSysLevel!=newCurrSysLevel){
				            		currSysLevel_change=true;
				            	}
		            			var newStatus=$div.find("[name=status]:checked").val(),status_change=false;
		            			if(newStatus==0){//手机停用 
		            				newStatus=accountStatusStopTemp;
		            				//银行卡也停用
		            				if(account.status!=accountStatusStopTemp){
		            					status_change=true;
		            				}
		            			}
		            			//有任何一个变更
			            		if(currSysLevel_change||status_change){
		            				console.log("执行更新");
		    		            	var data = {
		    		            		currSysLevel:newCurrSysLevel,
		    		       				account:bankInfo.account
		    		       			}
		    		            	if(status_change){
		    		            		data.status=newStatus;
		    		            	}
		    			       		$.ajax({
		    			  		    	type: "POST",
		    			  		        dataType: 'JSON',
		    			  		        url: '/r/account/updateAccount14Other',
		    			  		        async: false,
		    			  		        data: data,
		    			  		        success: function (jsonObject) {
		    			  		            if (jsonObject.status == 1) {
		    			  		            	console.log("同步到本系统成功");
		    			  		            }
		    			  		        }
		    			       		});
			            		}
	            			}
		            	}
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


/** 删除客户资料 */
var do_deletePhoneNo=function(handicapCode,mobileId){
	bootbox.confirm("确定删除?<br/> <span class='red bolder'>【一经删除  不可恢复！！！】<br/>【即使该账号被删除，也不允许再添加相同的手机号/支付宝/微信/银行卡！！！】</span>", function (result) {
		if (result) {
			//先查银行卡信息 勿挪动顺序
			var bankInfo=getAccountInfo2(handicapCode,mobileId,2)
			var params={
				oid:handicapCode,
				id:mobileId
			}
			$.ajax({
		        type: "POST",
		        contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/remove',
		        async: false,
		        data: JSON.stringify(params),
		        success: function (jsonObject) {
		            if (jsonObject.status == 1) {
		            	if(bankInfo&&bankInfo.account){
	            			var account=getAccountInfoByAcc(bankInfo.account);
		            		//删除 修改状态
	            			if(account){
	            				$.ajax({ 
									dataType:'json',
									type:"get",
									url:API.r_account_del, 
									data:{ 
										"id":account.id
									},
					  		        success: function (jsonObject) {
					  		            if (jsonObject.status == 1) {
					  		            	console.log("同步到本系统成功");
					  		            }
					  		        }
				            	});
	            			}
		            	}
		            	
		                showMessageForSuccess("删除成功");
		                showPage();
		            } else {
		                showMessageForFail("删除失败：" + jsonObject.message);
		            }
		        }
		   });
		}
	});
}

/** 重置信用额度 */
var showModalResetCredits = function (handicapCode,mobileId) {
	var $div = $("#resetCredits4clone").clone().attr("id", "resetCredits");
	$div.find("td").css("padding-top", "10px");;
    $div.find("#handicapCode").val(handicapCode);
    $div.find("#mobileId").val(mobileId);
	$div.modal("toggle");
	//绑定信息初始化
	resetCredits_infoLoad($div);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
}
var resetCredits_infoLoad=function($div){
	var oid=$div.find("#handicapCode").val();
	var id=$div.find("#mobileId").val();
	var type=$div.find("#resetCredits_tabType").val();
	data=getAccountInfo(oid,id,type);
	if(data){
		if(data&&data.accountId){
		    $div.find("[name=account]").val(data.account);
		    $div.find("[name=money]").val("");
			$div.find("#unBind").hide();
			$div.find("#resetCredits_ok").show();
			$div.find("table").show();
		}else{
			$div.find("#resetCredits_ok").hide();
			$div.find("table").hide();
			$div.find("#unBind").show();
		}
	}
}
var resetCredits_changeTab=function(type){
	var $div=$("#resetCredits");
	$div.find("#resetCredits_tabType").val(type);
	//重置table内数据
	reset("resetCredits table");
	resetCredits_infoLoad($div);
}
var doResetCredits=function(){
	var $div = $("#resetCredits");
    var type=$div.find("#resetCredits_tabType").val();
    if(!type){
    	return;
    }
	var oid=$div.find("#handicapCode").val(),money=$div.find("[name=money]").val();
	var phoneInfo=getTelInfo(oid,$div.find("#mobileId").val());
	if(money>phoneInfo.credits){
		showMessageForFail("信用额度最高不可超过："+phoneInfo.credits);
		return;
	}
	bootbox.confirm("确定重置 ?", function (result) {
		if (result) {
			if(type==2){
				$div.find(".bank").show();
				$div.find(".wechatAli").hide();
			}else if(type==0||type==1){// 微信支付宝
				$div.find(".wechatAli").show();
				$div.find(".bank").hide();
			}
			var device;
			if(type==0){
				device=phoneInfo.wechatDeviceCol;
			}else if(type==1){
				device=phoneInfo.alipayDeviceCol;
			}else if(type==2){
				device=phoneInfo.bankDeviceCol;
			}
			var params = {
				oid:oid,
				device:device[0],
				type:type,
				money:money
		    };
			 $.ajax({
			    type: "POST",
	            contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/reset',
		        async: false,
		        data: JSON.stringify(params),
		        success: function (jsonObject) {
		            if (jsonObject.data) {
		                showMessageForSuccess("重置信用额度成功");
		                $div.modal("toggle");
		                showPage();
		            } else {
		                showMessageForFail("重置信用额度失败：" + jsonObject.message);
		            }
		        }
		    });
	  }
	});
}

/** 已绑账号信息弹出框 */
var showModalUpdateBindAccount = function (handicapCode,mobileId,mobile) {
	var $div = $("#updateBindAccount4clone").clone().attr("id", "updateBindAccount");
	$div.find("td").css("padding-top", "10px");
	$div.modal("toggle");
    $div.find("#handicapCode").val(handicapCode);
    $div.find("#mobileId").val(mobileId);
    $div.find("#mobile").val(mobile);
	//加载银行品牌
	var nbsp='&nbsp;&nbsp;&nbsp;&nbsp;';
	var options="";
	options+="<option value='' >"+nbsp+"请选择"+nbsp+"</option>";
	$.map(bankTypeList4Customer,function(record){
		options+="<option value="+record.id+" bankName="+record.bankName+" >"+nbsp+record.bankName+nbsp+"</option>";
	});
	$div.find("select[name=choiceBankBrand]").html(options);
	updateBindAccount_infoLoad($div);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
}
var updateBindAccount_infoLoad=function($div){
	var oid=$div.find("#handicapCode").val();
	var id=$div.find("#mobileId").val();
	var type=$div.find("#updateBindAccount_tabType").val();
	$div.find(".wechatAli,.bank,.ali,.wechat").hide();
	if(type==2){
		$div.find(".bank").show();
	}else if(type==0){// 微信
		$div.find(".wechatAli,.wechat").show();
	}else if(type==1){// 支付宝
		$div.find(".wechatAli,.ali").show();
	}
	var data = getAccountInfo2(oid,id,type);
	if(data){
		$div.find("[name=account]").attr("placeholder","修改账号");
		if(data.accountId){
			$div.find("[name=isEpAlipay]").prop("disabled","disabled");
			$div.find("#accountId").val(data.accountId);
			$div.find("[name=account]").val(data.account);
			$div.find("[name=owner]").val(data.name);
			$div.find("[name=limitBalance]").val(data.balanceAlarm);
			if(type==2){//银行卡
				$div.find("[name=choiceBankBrand]").val(data.bankId);
				$div.find("[name=bankOpen]").val(data.bankOpen);
			}else if(type==0||type==1){// 微信支付宝
				$div.find("[name=limitInDaily]").val(data.inLimit);
				//提现方式 二维码提现
				if(data.qrDrawalMethod&&data.qrDrawalMethod==1){
					$div.find("[name=qrDrawalMethod]").prop("checked",true);
				}else{
					$div.find("[name=qrDrawalMethod]").prop("checked",false);
				}
				//提现方式 银行卡提现
				if(data.bankDrawalMethod&&data.bankDrawalMethod==1){
					$div.find("[name=bankDrawalMethod]").prop("checked",true);
				}else{
					$div.find("[name=bankDrawalMethod]").prop("checked",false);
				}
				if(type==1){
					$div.find("[name=ylbThreshold]").val(data.ylbThreshold);
					$div.find("[name=ylbInterval]").val(data.ylbInterval);
					$div.find("[name=zfbAlias1]").val(data.zfbAlias1);
					$div.find("[name=zfbAlias2]").val(data.zfbAlias2);
					$div.find("[name=uid]").val(data.uid);
					if(data.isEpAlipay==0||data.isEpAlipay==1){
						$div.find("[name=isEpAlipay][value="+data.isEpAlipay+"]").prop("checked",true);
					}
				}else{
					$div.find("[name=wxAlias1]").val(data.wxAlias1);
					$div.find("[name=wxAlias2]").val(data.wxAlias2);
					$div.find("[name=wxAlias3]").val(data.wxAlias3);
				}
			}
		}else{
			$div.find("[name=account]").attr("placeholder","新增账号");
		    $div.find("#accountId").val("");
		}
	}
}
var updateBindAccount_changeTab=function(type){
	var $div=$("#updateBindAccount");
	//重置table内数据
	reset("updateBindAccount table");
	$div.find("#updateBindAccount_tabType").val(type);
	updateBindAccount_infoLoad($div);
}
/** 修改已绑账号基本信息 */
var doUpdateBindAccount = function () {
	var $div = $("#updateBindAccount");
	var bankInfo;
	var type= $div.find("#updateBindAccount_tabType").val();
	var $account = $div.find("[name=account]");
    var $owner = $div.find("[name=owner]");
    var $limitBalance = $div.find("[name=limitBalance]");
	var validate=[
    	{ele: $account,name:'账号',maxLength:25},
		{ele: $owner, name:(type==2?'开户人':'姓名'),minLength:2,maxLength:50},
		{ele: $limitBalance, name: '提现额度',type:'amountPlus',min:0,maxEQ:50000}
	];
	var params = {
			oid:$div.find("#handicapCode").val(),
			mobileId:$div.find("#mobileId").val(),
			accountId:$div.find("#accountId").val()?$div.find("#accountId").val():null,
			type:type,
			account:$.trim($account.val()),
			name:$.trim($owner.val()),
			balanceAlarm:$.trim($limitBalance.val())
	};
	if(type==2){
		//勿挪动位置  如果有绑定银行卡 更新本系统银行状态，先查出原银行账号，因为更新后账号会进行变更！
		bankInfo=getAccountInfo2($div.find("#handicapCode").val(),$div.find("#mobileId").val(),2);
	    var $bankType = $div.find("[name=choiceBankBrand]");
	    var $bankOpen = $div.find("[name=bankOpen]");
		validate.push({ele: $bankType, name: '开户行 > 银行类别'});
		validate.push({ele: $bankOpen, name: '开户行 >支行',maxLength:50});
		params.bankId=$bankType.val();//银行卡类型对应的ID
		params.bankName=$bankType.find("option:selected").attr("bankName");
		params.openMan=$owner.val();
		params.bankOpen=$bankOpen.val();
	}else if(type==0||type==1){
		var validateIn=new Array();
	    var $limitInDaily = $div.find("[name=limitInDaily]");
	    validate.push({ele: $limitInDaily, name: '当日入款限额',type:'amountPlus'});
		params.name=$owner.val();
		params.inLimit=$limitInDaily.val();
		params.qrDrawalMethod=$div.find("input[name='qrDrawalMethod']:checked").length==1?1:0;
		params.bankDrawalMethod=$div.find("input[name='bankDrawalMethod']:checked").length==1?1:0;
		if(type==1){
			var $ylbThreshold=$div.find("[name=ylbThreshold]");
			var $ylbInterval=$div.find("[name=ylbInterval]");
			var $zfbAlias1=$div.find("[name=zfbAlias1]");
			var $zfbAlias2=$div.find("[name=zfbAlias2]");
			var $uid=$div.find("[name=uid]");
		    var $isEpAlipay = $div.find("[name=isEpAlipay]:checked");
			validate.push({ele: $uid,name:'支付宝UID'});
			validate.push({ele: $isEpAlipay,name:'是否企业支付宝'});
			validateIn.push({ele: $ylbInterval,name:'支付宝余额查询时间间隔',type:'amountPlus',minEQ:3,maxEQ:60});
			validateIn.push({ele: $zfbAlias1,name:'支付宝别名1',minLength:1,maxLength:50});
			validateIn.push({ele: $zfbAlias1,name:'支付宝别名2',minLength:1,maxLength:50});
			params.ylbThreshold=$ylbThreshold.val();
			params.ylbInterval=$ylbInterval.val();
			params.zfbAlias1=$.trim($zfbAlias1.val(),true);
			params.zfbAlias2=$.trim($zfbAlias2.val(),true);
			params.uid=$.trim($uid.val(),true);
			params.isEpAlipay=$.trim($isEpAlipay.val(),true);
		}else{
		    var $wxAlias1=$div.find("[name=wxAlias1]");
		    var $wxAlias2=$div.find("[name=wxAlias2]");
		    var $wxAlias3=$div.find("[name=wxAlias3]");
		    validateIn.push({ele: $wxAlias1,name:'微信别名1',minLength:1,maxLength:50});
		    validateIn.push({ele: $wxAlias2,name:'微信别名2',minLength:1,maxLength:50});
		    validateIn.push({ele: $wxAlias3,name:'微信别名3',minLength:1,maxLength:50});
			params.wxAlias1=$.trim($wxAlias1.val(),true);
			params.wxAlias2=$.trim($wxAlias2.val(),true);
			params.wxAlias3=$.trim($wxAlias3.val(),true);
		}
		//校验
		if(!validateInput(validateIn)){
			return;
		}
	}
	if (!validateEmptyBatch(validate)||!validateInput(validate)) {//非空校验
        return;
    }
	bootbox.confirm("确定修改 ?", function (result) {
		if (!result) {
			return;
		}
		$.ajax({
			type: "PUT",
			dataType: 'JSON',
			contentType: 'application/json;charset=UTF-8',
	        dataType: 'JSON',
	        url: '/newpay/modifyAccount',
			async: false,
			data: JSON.stringify(params),
			success: function (jsonObject) {
				if (jsonObject.status == 1) {
					if(type==2){
            			var data={
		       				account:$.trim($account.val(),true),//银行卡账号
		       				owner:$.trim($owner.val()),//开户人
		       				bankName:$.trim($bankOpen.val()),//开户支行名称
		       				limitBalance:$.trim($limitBalance.val())//银行卡提现额度
		       			}
		            	var selected_bankType=$.trim($div.find("[name=choiceBankBrand] option:selected").attr("bankName"));
		            	var bankType=getBankType4Customer(selected_bankType);
		            	if(bankType){
		            		data.bankType=bankType;
		            	}else{
		            		data.bankType=selected_bankType;
		            	}
            			var account=getAccountInfoByAcc(bankInfo.account);
            			if(account){
            				//修改
            				data.id=account.id;
            			}else{
            				//新增
            				if(!bankInfo.account){
            					//平台无账号 本系统无
            					data.status=accountStatusStopTemp;
            				}else{
            					//平台有账号  本系统无
            					if(bankInfo.status==0){//停用 
                    				data.status=accountStatusStopTemp;
                    			}else if(bankInfo.status==1){//启用
                    				data.status=accountStatusNormal;
                    			}
            				}
            				data.mobile=$div.find("#mobile").val();//手机号
			            	data.handicapCode=$div.find("#handicapCode").val();
			            	if(bankInfo.level==0){
			            		data.currSysLevel=currentSystemLevelOutter;
			            	}else if(bankInfo.level==1){
			            		data.currSysLevel=currentSystemLevelMiddle;
			            	}else if(bankInfo.level==2){
			            		data.currSysLevel=currentSystemLevelInner;
			            	}
                			
            			}
            			//平台无账号新增||修改||平台有账号本系统无账号且状态为在用
            			if((!bankInfo.account)||account||data.status==accountStatusNormal){
            				//银行卡存在 或（不存在 但状态为在用）才新增/更新
            				$.ajax({
    			  		    	type: "POST",
    			  		        dataType: 'JSON',
    			  		        url: '/r/account/'+(account?'updateAccount14Info':'createAccount14'),
    			  		        async: false,
    			  		        data: data,
    			  		        success: function (jsonObject) {
    			  		            if (jsonObject.status == 1) {
    			  		            	console.log("同步到本系统成功");
    			  		            }
    			  		        }
    			       		});
            			}
	            	}
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

/** 已绑账号状态弹出框 */
var showModalUpdateBindStatus = function (handicapCode,mobileId) {
	var $div = $("#updateBindStatus4clone").clone().attr("id", "updateBindStatus");
	$div.find("td").css("padding-top", "10px");;
    $div.find("#handicapCode").val(handicapCode);
    $div.find("#mobileId").val(mobileId);
	$div.modal("toggle");
	//绑定信息初始化
	updateBindStatus_infoLoad($div);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
}
var updateBindStatus_infoLoad=function($div){
	var oid=$div.find("#handicapCode").val();
	var id=$div.find("#mobileId").val();
	var type=$div.find("#updateBindAccount_tabType").val();
	data=getAccountInfo(oid,id,type);
	if(data){
		if(data&&data.accountId){
			$div.find("#accountId").val(data.accountId);
		    $div.find("[name=account]").val(data.account);
		    $div.find("[name=status][value=" + data.status + "]").prop("checked", true);
			$div.find("#unBind").hide();
			$div.find("#updateBindStatus_ok").show();
			$div.find("table").show();
		}else{
			$div.find("#accountId").val("");
			$div.find("#updateBindStatus_ok").hide();
			$div.find("table").hide();
			$div.find("#unBind").show();
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
/** 修改已绑账号状态 */
var doUpdateBindStatus = function () {
    var $div = $("#updateBindStatus");
    var type=$div.find("#updateBindAccount_tabType").val();
    if(!type){
    	return;
    }
	bootbox.confirm("确定修改 ?", function (result) {
	  if (result) {
		var params = {
			oid:$div.find("#handicapCode").val(),
			accountId:$div.find("#accountId").val(),
			type:type,
			status:$div.find("[name=status]:checked").val()
	    };
		 $.ajax({
		    type: "POST",
            contentType: 'application/json;charset=UTF-8',
	        dataType: 'JSON',
	        url: '/newpay/modifyStatus',
	        async: false,
	        data: JSON.stringify(params),
	        success: function (jsonObject) {
	            if (jsonObject.status == 1) {
	            	if(type==2){
	            		//如果有绑定银行卡 更新本系统银行状态
		            	var bankInfo=getAccountInfo2($div.find("#handicapCode").val(),$div.find("#mobileId").val(),2);
		            	if(bankInfo&&bankInfo.account){
		            		//如果无变更无需发送更新请求
	            			var account=getAccountInfoByAcc(bankInfo.account);
	            			if(account){
	            				var newStatus=$div.find("[name=status]:checked").val(),status_change=false;
	                			if(newStatus==0){//停用 
	                				newStatus=accountStatusStopTemp;
	                				if(account.status!=accountStatusStopTemp){
	                					status_change=true;
	                				}
	                			}else if(newStatus==1){//启用
	                				newStatus=accountStatusNormal;
	                				if(account.status!=accountStatusNormal){
	                					status_change=true;
	                				}
	                			}
	                			//有状态变更
	    	            		if(status_change){
	        		            	if(status_change){
	            			       		$.ajax({
	            			  		    	type: "POST",
	            			  		        dataType: 'JSON',
	            			  		        url: '/r/account/updateAccount14Other',
	            			  		        async: false,
	            			  		        data: {
	                		       				account:bankInfo.account,
	                		            		status:newStatus
	                		       			},
	            			  		        success: function (jsonObject) {
	            			  		            if (jsonObject.status == 1) {
	            			  		            	console.log("同步到本系统成功");
	            			  		            }
	            			  		        }
	            			       		});
	        		            	}
	    	            		}
	            			}
		            	}
	            	}
	                showMessageForSuccess("新增成功");
	                $div.modal("toggle");
	                showPage();
	            } else {
	                showMessageForFail("新增失败：" + jsonObject.message);
	            }
	        }
	    });
	  }
	});
};

/** 已绑通道弹出框 */
var showModalSettingPO = function (handicapCode,mobileId) {
	var $div = $("#bindPO4clone").clone().attr("id", "bindPO");
	$div.find("td").css("padding-top", "10px");;
	$div.find("#handicapCode").val(handicapCode);
	$div.find("#mobileId").val(mobileId);
	$div.modal("toggle");
	//绑定信息初始化
	bindPO_infoLoad($div);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
}
var bindPO_infoLoad=function($div){
	var oid=$div.find("#handicapCode").val();
	var id=$div.find("#mobileId").val();
	var type=$div.find("#bindPO_tabType").val();
	data=getAccountInfo(oid,id,type);
	if(data){
		if(data&&data.accountId){
			var $tbody=$div.find("tbody");
			$div.find("#accountId").val(data.accountId);
			$div.find("[name=account]").text(data.account);
			$div.find("#unBind").hide();
			$div.find("#bindPO_ok").show();
			$div.find("table,[name=account]").show();
			//业主未删除的新支付通道
			var poList=getPOCForCrk(oid,type);
			//已绑定的支付通道
			var bindedPO=getBindPOByWechatAli(oid,id,type);
			var trs="";
			$.map(poList,function(record){
				//是否已绑定
				var isBinded=$.inArray(record.id, JSON.parse(bindedPO))!=-1;
				trs+="<tr>";
    			trs+='<td>\
    					<label class="pos-rel">\
							<input type="checkbox" class="ace checkboxBindPO " '+(isBinded?' checked ':'')+' value="'+record.id+'">\
							<span class="lbl"></span>\
						</label>\
    				</td>';
    			trs+="<td>"+(record.payCode&&record.payCode!='null'?record.payCode:"")+"</td>";
    			trs+="<td>"+(record.isEpAlipay&&record.isEpAlipay==1?"是":"否")+"</td>";
    			trs+="</tr>";
			});
			var $tbody=$div.find("tbody").html(trs);
		}else{
			$div.find("#accountId").val("");
			$div.find("[name=account]").text("");
			$div.find("table,[name=account]").hide();
			$div.find("#bindPO_ok").hide();
			$div.find("#unBind").show();
		}
	}
}
var bindPO_changeTab=function(type){
	var $div=$("#bindPO");
	$div.find("#bindPO_tabType").val(type);
	//重置table内数据
	reset("bindPO table");
	bindPO_infoLoad($div);
}

/** 绑定支付宝/微信支付通道 ocIdCol:单个 ocIdColArry:数组 */
var doSaveBindPO = function () {
	var $div = $("#bindPO");
	var ocIdColArry=new Array();
	$div.find(".checkboxBindPO:checked").each(function() {  
		ocIdColArry.push(this.value);
    });
	var oid=$div.find("#handicapCode").val();
	var mobileId=$div.find("#mobileId").val();
	var type=$div.find("#bindPO_tabType").val();
	if(!type){
		return;
	}
	bootbox.confirm("确定绑定支付通道 ?", function (result) {
		if (result) {
			var params = {
					oid:oid,
					mobileId:mobileId,
					type:type,
					ocIdCol:ocIdColArry
			};
			$.ajax({
				type: "POST",
				contentType: 'application/json;charset=UTF-8',
				dataType: 'JSON',
				url: '/newpay/newpayAisleConfigBind',
				async: false,
				data: JSON.stringify(params),
				success: function (jsonObject) {
					if (jsonObject.data == 1) {
						showMessageForSuccess("新增成功");
						$div.modal("toggle");
						showPage();
					} else {
						showMessageForFail("新增失败：" + jsonObject.message);
					}
				}
			});
		}
	});
};
/** 批量绑定通道 全选反选 */
var checkedAll_batchBindPO=function(){
	var isCheck=$("#bindPO #checkedBbatchBindPO").is(':checked');
	//全选或者全不选
	$("#bindPO .checkboxBindPO").each(function() {  
        this.checked = isCheck;       //循环赋值给每个复选框是否选中
    });
}

/** 已绑账号密码弹出框 */
var showModalUpdatePWD = function (handicapCode,mobileId) {
	var $div = $("#updatePWD4clone").clone().attr("id", "updatePWD");
	$div.find("#handicapCode").val(handicapCode);
	$div.find("#mobileId").val(mobileId);
	$div.find("td").css("padding-top", "10px");
	$div.modal("toggle");
	var pwdExistsData={};
	//查询密码是否已被设置
	$.ajax({
        type: "POST",
        contentType: 'application/json;charset=UTF-8',
        dataType: 'JSON',
        url: '/newpay/findPwdExists',
        async: false,
        data: JSON.stringify({
        	oid:handicapCode,
    		id:mobileId
        }),
        success: function (jsonObject) {
            if (jsonObject.status == 1) {
            	pwdExistsData=jsonObject.data;
            } else {
                showMessageForFail("查询失败：" + jsonObject.message);
            }
        }
    });
	//单个手机信息
	var telInfo=getTelInfo(handicapCode,mobileId);
	//密码信息
	if(telInfo.bankAccount){
		//银行卡单个信息
		var bankInfo=getAccountInfo(handicapCode,mobileId,2);
		//动态读取银行类别
		var bankType=bankInfo.bankName;
		$div.find("[name=bankHide]").val(telInfo.bankAccount+" - "+bankType);
	    if(pwdExistsData.bankLoginPwdExists==1){
		    $div.find("[name=singBank]").attr("placeholder","********");
	    }
	    if(pwdExistsData.bankPaymentPwdExists==1){
	    	$div.find("[name=pingBank]").attr("placeholder","********");
	    }
		if(bankType&&bankType=='建设银行'||bankType&&bankType=='中国建设银行'){
			$div.find("[name=bingBank],[name=uingBank]").removeAttr('disabled');
		    if(pwdExistsData.bankPwdExists==1){
			    $div.find("[name=bingBank]").attr("placeholder","********");
		    }
		    if(pwdExistsData.uShieldPwdExists==1){
		    	$div.find("[name=uingBank]").attr("placeholder","********");
		    }
			
		}else{
			$div.find("[name=bingBank],[name=uingBank]").attr('disabled','disabled');
		}
		
	}else{
		$div.find("[name=singBank],[name=pingBank],[name=bingBank],[name=uingBank]").attr('disabled','disabled');
	}
	if(telInfo.wechatAccount){
	    $div.find("[name=wechatHide]").val(telInfo.wechatAccount);
	    if(pwdExistsData.wechatLoginPwdExists){
		    $div.find("[name=singWechat]").attr("placeholder","********");
	    }
	    if(pwdExistsData.wechatPaymentPwdExists){
	    	$div.find("[name=pingWechat]").attr("placeholder","********");
	    }
	}else{
		$div.find("[name=singWechat],[name=pingWechat]").attr('disabled','disabled');
	}
    if(telInfo.alipayAccount){
        $div.find("[name=alipayHide]").val(telInfo.alipayAccount);
	    if(pwdExistsData.alipayLoginPwdExists){
		    $div.find("[name=singAlipay]").attr("placeholder","********");
	    }
	    if(pwdExistsData.alipayPaymentPwdExists){
	    	$div.find("[name=pingAlipay]").attr("placeholder","********");
	    }
	}else{
		$div.find("[name=singAlipay],[name=pingAlipay]").attr('disabled','disabled');
	}
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
}
/** 修改已绑账号密码 */
var doUpdatePWD=function(){
	var $div=$("#updatePWD");
	 bootbox.confirm("确定修改密码信息?", function (result) {
         if (result) {
             var params = {
            		 oid:$div.find("#handicapCode").val(),
            		 id:$div.find("#mobileId").val(),
            		 wechatLoginPassword:$.trim($div.find("[name=singWechat]").val(),true),
            		 wechatPaymentPassword:$.trim( $div.find("[name=pingWechat]").val(),true),
            		 alipayLoginPassword:$.trim($div.find("[name=singAlipay]").val(),true),
            		 alipayPaymentPassword:$.trim($div.find("[name=pingAlipay]").val(),true),
            		 bankLoginPassword:$.trim($div.find("[name=singBank]").val(),true),
            		 bankPaymentPassword:$.trim($div.find("[name=pingBank]").val(),true),
            		 bankPassword:$.trim($div.find("[name=bingBank]").val(),true),
            		 ushieldPassword:$.trim($div.find("[name=uingBank]").val(),true)
             };
        	 $.ajax({
 		        type: "POST",
                contentType: 'application/json;charset=UTF-8',
 		        dataType: 'JSON',
 		        url: '/newpay/modifyPwd',
 		        async: false,
 		        data: JSON.stringify(params),
 		        success: function (jsonObject) {
 		            if (jsonObject.status == 1) {
 		            	//如果有绑定银行卡 更新本系统银行密码
 		            	var bankInfo=getAccountInfo2($div.find("#handicapCode").val(),$div.find("#mobileId").val(),2);
 		            	if(bankInfo&&bankInfo.account){
 		            		//如果无变更无需发送更新请求
 	            			var account=getAccountInfoByAcc(bankInfo.account);
 	            			if(account){
 	            				var has_change=false;
 	            				var data={
         		       				account:bankInfo.account
         		       			}
 	            				if($div.find("[name=singBank]").val()){
 	            					has_change=true;
 	            					data.hook=$.trim($div.find("[name=singBank]").val(),true);
 	            				}
 	            				if($div.find("[name=pingBank]").val()){
 	            					has_change=true;
 	            					data.hub=$.trim($div.find("[name=pingBank]").val(),true);
 	            				}
 	            				if($div.find("[name=uingBank]").val()){
 	            					has_change=true;
 	            					data.bing=$.trim($div.find("[name=uingBank]").val(),true);
 	            				}
 	                			//有密码变更
 	    	            		if(has_change){
            			       		$.ajax({
            			  		    	type: "POST",
            			  		        dataType: 'JSON',
            			  		        url: '/r/account/updateAccount14Other',
            			  		        async: false,
            			  		        data: data,
            			  		        success: function (jsonObject) {
            			  		            if (jsonObject.status == 1) {
            			  		            	console.log("同步到本系统成功");
            			  		            }
            			  		        }
            			       		});
 	    	            		}
 	            			}
 		            	}
 		                showMessageForSuccess("修改成功");
 		                $div.modal("toggle");
 		            } else {
 		                showMessageForFail("修改失败：" + jsonObject.message);
 		            }
 		        }
 		    });
         }
     });
}

/** 已绑账号设备号二维码读取 */
var showModalQrBindAccount = function (handicapCode,mobileId,tel) {
	var $div = $("#qrBindAccount4clone").clone().attr("id", "qrBindAccount");
	$div.modal("toggle");
    $div.find("#handicapCode").val(handicapCode);
    $div.find("#mobileId").val(mobileId);
    $div.find("[name=mobile]").text(hideAccountAll(tel+"",true));
	qrBindAccount_infoLoad(1,$div);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
}
var qrBindAccount_infoLoad=function(type,$div){
	if(!$div){
		$div=$("#qrBindAccount");
	}
	$div.find("#qrCode").html("");
	var handicapCode=$div.find("#handicapCode").val();
	var mobileId=$div.find("#mobileId").val();
    var data = getTelInfo(handicapCode,mobileId);
    $div.find("#bind,#unBind").hide();
	$div.find("[name=account]").text("");
	$div.find("[name=device]").text("");
	if(data){
		$div.find("#accountId").val(data.accountId);
	    var device,account;
		if(type==2&&data.bankAccount){//银行卡
			device=data.bankDeviceCol?data.bankDeviceCol:null;
			account=data.bankAccount?data.bankAccount:null;
		}else if(type==0&&data.wechatAccount){// 微信
			device=data.wechatDeviceCol?data.wechatDeviceCol:null;
			account=data.wechatAccount?data.wechatAccount:null;
		}else if(type==1&&data.alipayAccount){// 支付宝
			device=data.alipayDeviceCol?data.alipayDeviceCol:null;
			account=data.alipayAccount?data.alipayAccount:null;
		}
		if(account){
		    $div.find("[name=account]").text(account);
		    $div.find("[name=device]").text(device);
		    if(device){
			    //生成二维码
			    var QR_JSON={
		    		mobile:$div.find("[name=mobile]").text(),
		    		deviceid:device[0]
			    };
				$div.find("#qrCode").qrcode({
				    render: "canvas",
				    text: JSON.stringify(QR_JSON)
				});
		    }
		    $div.find("#bind").show();
		}else{
		    $div.find("#unBind").show();
		}
	}else{
        showMessageForFail("手机号不存在");
	}
}
/** 按输入生成收款码 */
var searchBindQR_genPrintQR=function(){
	var $div=$("#searchBindQR");
	var type=$div.find("#searchBindQR_tabType").val();
	var $amount=$div.find("[name=amount]");
	var $qrCodeCount=$div.find("[name=qrCodeCount]");
	//校验
	var validate=[
		{ele:$amount,name:'输入金额'}
		];
	 var validatePrint=[
		 	{ele:$qrCodeCount,name:'输入个数',type:'positiveInt',min:1,maxEQ:100}
	    ];
	if(!validateEmptyBatch(validate)||!validateInput(validatePrint)){
		return;
	}
	var params={
			oid:$div.find("#handicapCode").val(),
			type:type,
			accountId:$div.find("#accountId").val(),
			moneySet:$amount.val().split(","),
			qrCodeCount:$qrCodeCount.val()
	}
	bootbox.confirm("确定生成?", function (result) {
		if (result) {
			$.ajax({  
				type: "POST",
                contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/batchAddQR',
		        async: false,
		        data: JSON.stringify(params),
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


/** 已绑账号二维码获取 */
var showModalPageForQr = function (handicapCode,mobileId) {
	var $div = $("#searchBindQR");
	$div.find("#handicapCode").val(handicapCode);
	$div.find("#mobileId").val(mobileId);
	$div.modal("toggle");
	$div.find("td").css("padding-top", "10px");
	$(".do_genQr").hide();
	$div.find("#download").attr("href","/"+sysVersoin+"/phone/common.txt");
    //根据上次记录的TAB类型查询 第一页开始
    searchBindQR_changeTab($div.find("#searchBindQR_tabType").val(),0);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容
		reset('searchBindQR_filter');
		reset('tableQRList');
		reset('wechatAlipayQRInfo .do_genQr');  
	});
};
var searchBindQR_changeTab=function(type,CurPage){
	var $div=$("#searchBindQR");
	var handicapCode=$div.find("#handicapCode").val();
	var mobileId=$div.find("#mobileId").val();
	$div.find("#searchBindQR_tabType").val(type);
	//收起生成二维码面板
	if(!$(".do_genQr").is(":hidden")){
		$(".do_genQr").hide();
	}
	if(type==0){//微信
		$div.find(".aliTips").hide();
		$div.find(".wechatTips").show();
	}else if(type==1){//支付宝
		$div.find(".wechatTips").hide();
		$div.find(".aliTips").show();
	}
	//清除上一次数据
	$("#tableQRList tbody").html("");
	var data = getAccountInfo(handicapCode,mobileId,type);
	if(data&&data.accountId){
		$div.find("#dataDiv").show();
		$div.find("#unBind").hide();
		$div.find("#account").text(data.account);
		$div.find("#accountId").val(data.accountId);
		//执行查询
		searchBindQR_infoLoad(CurPage==0?CurPage:null);
	}else{
		$div.find("#dataDiv").hide();
		$div.find("#unBind").show();
		$div.find("#account").text("");
		$div.find("#accountId").val("");
	}
}
var searchBindQR_infoLoad=function(CurPage){
	var $div=$("#searchBindQR");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#searchBindQRPage .Current_Page").text();
	//重置table内数据
	var params={
	   		oid:$div.find("#handicapCode").val(),
	   		mobileId:$div.find("#mobileId").val(),
			type:$div.find("#searchBindQR_tabType").val(),
			accountId:$div.find("#accountId").val(),
			moneyStart:$div.find("[name=minAmount]").val(),
			moneyEnd:$div.find("[name=maxAmount]").val(),
        	pageNo:(!CurPage||CurPage<=0)?0:CurPage-1,
		    pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10

		};
		$.ajax({
	        type: "POST",
	        contentType: 'application/json;charset=UTF-8',
	        dataType: 'JSON',
	        url: '/newpay/findQRByCondition',
	        async: false,
	        data: JSON.stringify(params),
	        success: function (jsonObject) {
	            if (jsonObject.status == 1) {
            		var trs="";
	            	if(jsonObject.data){
	            		$.each(jsonObject.data,function(index,record){
	            			trs+="<tr>";
	            			trs+='<td>\
	            					<label class="pos-rel">\
										<input type="checkbox" class="ace checkboxDelQR defaultNoCheck" value="'+record.id+'">\
										<span class="lbl"></span>\
									</label>\
	            				</td>';
	            			trs+="<td>"+(index+1)+"</td>";
	            			trs+="<td><a>"+record.url+"</a></td>";
	            			trs+="<td>"+record.money+"</td>";
	            			trs+="<td>"+_checkObj(record.chkRemark)+"</td>";
	            			trs+="<td>";
	            			trs+="<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:delQR:*' onclick='doDelQR(" + record.oid + "," + record.id + ")'><i class='ace-icon fa fa-trash-o bigger-100 red'></i><span>删除</span></a>"
	            			trs+="</td>";
	            			trs+="</tr>";
	            		});
	            	}
            		$div.find("#dataDiv tbody").html(trs);
	                //分页初始化
	    			showPading(jsonObject.page,"searchBindQRPage",searchBindQR_infoLoad,null,true);
	                contentRight();
	            } else {
	                showMessageForFail("查询失败：" + jsonObject.message);
	            }
	        }
	    });
	
}

/** 生成0元收款码 */
var searchBindQR_genZeroQR=function(){
	var $div=$("#searchBindQR");
	var type=$div.find("#searchBindQR_tabType").val();
	var params={
		oid:$div.find("#handicapCode").val(),
		type:type,
		accountId:$div.find("#accountId").val(),
		moneySet:[0]
	}
	bootbox.confirm("确定生成?", function (result) {
		if (result) {
			$.ajax({  
				type: "POST",
                contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/batchAddQR',
		        async: false,
		        data: JSON.stringify(params),
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
/** 生成常用金额与非常用金额  */ 
var searchBindQR_genQR=function(commonFlag){
	var $div=$("#searchBindQR");
	var oid=$div.find("#handicapCode").val();
	var mobileId=$div.find("#mobileId").val();
	var type=$div.find("#searchBindQR_tabType").val();
	var telInfo=getTelInfo(oid,mobileId);
	if(!telInfo){
		return;
	}
	var device;
	if(type==0){//微信
		device=telInfo.wechatDeviceCol[0];
	}else if(type==1){//支付宝
		device=telInfo.alipayDeviceCol[0];
	}else{
		return;
	}
	var params={
		oid:oid,
		type:type,
		device:device,
		commonFlag:commonFlag//常用金额标记：1-常用  0-非常用
	}
	bootbox.confirm("确定生成?", function (result) {
		if (result) {
			$.ajax({  
				type: "POST",
				contentType: 'application/json;charset=UTF-8',
				dataType: 'JSON',
				url: '/newpay/genANMultQr',
				async: false,
				data: JSON.stringify(params),
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
var show_hide_do_genQr=function(){
	if($(".do_genQr").is(":hidden")){
		$(".do_genQr").show();
		//刷新生成状态
		var $div=$("#searchBindQR");
		var oid=$div.find("#handicapCode").val();
		var mobileId=$div.find("#mobileId").val();
		var type=$div.find("#searchBindQR_tabType").val();
		var genStatus=getStatisticsMWR(oid,mobileId,type);
		if(genStatus){
			$div.find("[name=mnt1]").text(genStatus.mnt1);
			$div.find("[name=mnt2]").text(genStatus.mnt2);
			$div.find("[name=mnt3]").text(genStatus.mnt3);
			$div.find("[name=mnt4]").text(genStatus.mnt4);
			if(genStatus.qrAccomplishFlag==0){//未完成
				$div.find("[name=qrAccomplishFlag]").html('<span class="btn btn-minier btn-danger"><i class="ace-icon fa fa-flask"></i>未完成</span>');
			}else if(genStatus.qrAccomplishFlag==1){//已完成
				$div.find("[name=qrAccomplishFlag]").html('<span class="btn btn-minier btn-success"><i class="ace-icon fa fa-check"></i>已完成</span>');
			}else{
				$div.find("[name=qrAccomplishFlag]").html('');
			}
		}else{
			$div.find("[name=mnt1],[name=mnt2],[name=mnt3],[name=mnt4]").text("--");
			$div.find("[nanme=qrAccomplishFlag]").html("");
		}
	}else{
		$(".do_genQr").hide();
	}
}

/** 单个删除二维码 */
var doDelQR=function(handicapCode,qrId){
	var $div=$("#searchBindQR");
	var params={list:[{oid:handicapCode,id:qrId}]};
	bootbox.confirm("确定删除?", function (result) {
	    if (result) {
			$.ajax({
			    type: "POST",
			    contentType: 'application/json;charset=UTF-8',
			    dataType: 'JSON',
			    url: '/newpay/batchRemoveQR',
			    async: false,
			    data: JSON.stringify(params),
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
	var amtList=[];
	$(".checkboxDelQR:checked").each(function() {  
		amtList.push({oid:$div.find("#handicapCode").val(),id:this.value});
    });
	if(amtList.length<1){
		showMessageForCheck("请选中至少一行收款码");
		return;
	}
	bootbox.confirm("确定删除选中二维码?", function (result) {
	    if (result) {
			$.ajax({
			    type: "POST",
			    contentType:'application/json;charset=UTF-8',
			    dataType: 'JSON',
			    url: '/newpay/batchRemoveQR',
			    async: false,
			    data: JSON.stringify({list:amtList}),
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

/** 词汇绑定 按钮点击事件 */
var showModalBindWordTypeModal=function(oid,mobileId){
    var $div = $("#bindWordTypeModal");
    $div.find("[name=search_LIKE_mobileNo]").val("");
    $div.modal("toggle");
	searchBindWordType_infoLoad(0);
}
/** 词汇列表 */
var searchBindWordType_infoLoad=function(CurPage){
	var $div=$("#bindWordTypeModal");
    if (!CurPage && CurPage != 0) CurPage = $("#bindWordType_page").find(".Current_Page").text();
	var type=$.trim($div.find("[name=wordType_type]:checked").val());
	var params={
	   		oid:$.trim($("#accountFilter").find("select[name=search_EQ_handicapCode]").val()),
	   		tel:$.trim($div.find("[name=search_LIKE_mobileNo]").val()),//手机号（客户）
        	pageNo:(!CurPage||CurPage<=0)?0:CurPage-1,
        	pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
		};
		$.ajax({
	        type: "POST",
	        contentType: 'application/json;charset=UTF-8',
	        dataType: 'JSON',
	        url: '/newpay/findForBind',
	        async: false,
	        data: JSON.stringify(params),
	        success: function (jsonObject) {
	            if (jsonObject.status == 1) {
            		var trs="";
	            	if(jsonObject.data){
	            		var list4MobileInfoHover=[];
	            		$.each(jsonObject.data,function(index,record){
	                    	list4MobileInfoHover.push({oid:record.oid,id:record.mobileId});
	            			trs+="<tr>";
	            			trs+="<td>"+(index+1)+"</td>";
	            			trs+="<td>"+_checkObj(record.ownerName)+"</td>";
	            			trs+="<td><a class='bind_hover_card' data-placement='auto right' data-trigger='hover' data-toggle='mobileInfoHover" + record.mobileId + "' >";
	                        trs+="<span>" + hidePhoneNumber(record.tel) + "</span>";
	                        trs+="</a></td>";
	            			trs+="<td><span style='float:left'>&nbsp;&nbsp;形容词</span>";
	            			trs+="<span style='float:right'>"+(record.adj?_checkObj(record.adj):"--")+"&nbsp;&nbsp;</span>";//形容
	            			trs+="<br/><span style='float:left'>&nbsp;&nbsp;名词</span>";
	            			trs+="<span style='float:right'>"+(record.noun?_checkObj(record.noun):"--")+"&nbsp;&nbsp;</span>";//名词
	            			trs+="</td>";
	            			trs+="<td>"+(record.adminTime?timeStamp2yyyyMMddHHmmss(record.adminTime):'--')+"</td>";
	            			trs+="<td>";
	            			//支付宝和微信都没有成功生成过二维码才可以绑定
	            			if(record.mcmStatus==0&&record.wechatMcmStatus==0){
		            			trs+="<a class='btn btn-xs btn-white btn-success btn-bold green contentRight' contentright='IncomePhoneNumber:updateFix:*'  \
										onclick='showModalBindingWordType(" + record.oid  + "," + record.mobileId +",0);' >\
										<span>绑定形容词</span>\
									</a>&nbsp;&nbsp;&nbsp;&nbsp;";
		            			trs+="<a class='btn btn-xs btn-white btn-success btn-bold green contentRight' contentright='IncomePhoneNumber:updateFix:*'  \
			            				onclick='showModalBindingWordType(" + record.oid  + "," + record.mobileId +",1);' >\
			            				<span>绑定名词</span>\
		            				</a>";
	            			}else{
	            				trs+="--";
	            			}
	            			trs+="</td>";
	            			trs+="</tr>";
	            		});
	            	}
            		$div.find("#searchBindWordType_table tbody").html(trs);
                    showPading(jsonObject.page, "bindWordType_page", searchBindWordType_infoLoad, null, true);
                    loadHover_MobileInfoHover(list4MobileInfoHover);
	                contentRight();
	            } else {
	                showMessageForFail("查询失败：" + jsonObject.message);
	            }
	        }
	    });
}

/** 词汇类型 绑定到当前账号 */
var showModalBindingWordType=function(handicapCode,mobileId,type){
	//加载数据列表
	var trs="";
	var params={
			oid:handicapCode,//盘口
			type:type//类型，0：形容词，1：名词
	};
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/newpay/findWordType',
		async: false,
		data: JSON.stringify(params),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				if(jsonObject.data){
					$.each(jsonObject.data,function(index,record){
						trs+="<tr>";
						trs+="<td>"+(index+1)+"</td>";
						trs+="<td>"+_checkObj(record.typeName)+"</td>";
						trs+="<td>";
						trs+="<a class='btn btn-xs btn-white btn-success btn-bold green contentRight' contentright='IncomePhoneNumber:updateFix:*'  \
								onclick='doBindingWordType(" + record.oid + ","+ type + ","+ mobileId + "," + record.id + ");' >\
								<span>绑定</span>\
							</a>";
						trs+="</td>";
						trs+="</tr>";
					});
				}
			} else {
				showMessageForFail("查询失败：" + jsonObject.message);
			}
		}
	});
	var typeStr=(type==0?"形容词":"名词");
	bootbox.dialog({
		title:"<span class='blue bolder' >绑定"+typeStr+"类型到当前账号</span>",
		message: '<table class="table table-striped table-bordered table-hover no-margin-bottom">\
					<thead>\
						<tr>\
							<th style="width:10%;">序号</th>\
							<th style="width:70%;">类型名称</th>\
							<th style="width:20%;">操作</th>\
						</tr>\
					</thead>\
					<tbody>'+trs+'</tbody>\
				</table>',
		buttons:{
			"click" :{
				"label" : "关闭","className" : "btn-sm btn-primary center bindCloseBtn","callback": function() {
					setTimeout(function(){       
						$('body').addClass('modal-open');
					},500);
				}},
		},
		closeButton:false
	});
}
var doBindingWordType=function(handicapCode,type,mobileId,wordTypeId){
	bootbox.confirm("确定绑定到当前账号?", function (result) {
		if (result) {
			var params={
					oid:handicapCode,
					type:type,
					mobileId:mobileId,
					wordTypeId:wordTypeId
			}
			$.ajax({
				type: "POST",
				contentType: 'application/json;charset=UTF-8',
				dataType: 'JSON',
				url: '/newpay/bindingWordType',
				async: false,
				data: JSON.stringify(params),
				success: function (jsonObject) {
					if (jsonObject.status == 1) {
						showMessageForSuccess("绑定成功");
						$(".bindCloseBtn").click();
						searchBindWordType_infoLoad();
					} else {
						showMessageForFail("绑定失败：" + jsonObject.message);
					}
				}
			});
		}
		setTimeout(function(){       
			$('body').addClass('modal-open');
		},500);
	});
}
/** 词汇类型 按钮点击事件 */
var showModalWordType=function(oid,mobileId){
	var $div = $("#wordTypeModal");
	reset('searchWordType_filter');
	$div.modal("toggle");
	$div.find("[name=wordType_type]").click(function(){
		searchWordType_infoLoad();
	});
	initTimePicker();
	searchWordType_infoLoad();
}
var searchWordType_infoLoad=function(){
	var $div=$("#wordTypeModal");
	var type=$.trim($div.find("[name=wordType_type]:checked").val());
	var params={
			oid:$.trim($("#accountFilter").find("select[name=search_EQ_handicapCode]").val()),//盘口id读取当前查询条件
			type:type//类型，0：形容词，1：名词
	};
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/newpay/findWordType',
		async: false,
		data: JSON.stringify(params),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				var trs="";
				if(jsonObject.data){
					$.each(jsonObject.data,function(index,record){
						trs+="<tr>";
						trs+="<td>"+(index+1)+"</td>";
						trs+="<td>"+_checkObj(record.typeName)+"</td>";
						trs+="<td>";
						trs+="<a class='btn btn-xs btn-white btn-inverse btn-bold black contentRight' contentright='IncomePhoneNumber:updateFix:*'  \
								onclick='showModalRemoveWordType(" + record.oid + "," + record.id + ");' >\
								<span>删除</span>\
							</a>";
						trs+="<a class='btn btn-xs btn-white btn-success btn-bold green contentRight' contentright='IncomePhoneNumber:updateFix:*'  \
								onclick='showModalAddContent(" + record.oid + "," + record.id + ");' >\
								<span>新增内容</span>\
							</a>";
						trs+="</td>";
						trs+="</tr>";
					});
				}
				$div.find("#searchWordType_table tbody").html(trs);
				contentRight();
			} else {
				showMessageForFail("查询失败：" + jsonObject.message);
			}
		}
	});
}

/** 词汇类型 新增 */
var showModalAddWordType=function(){
	bootbox.dialog({
		title:"<span class='blue bolder' >新增词汇类型</span>",
		message: "<span class='label label-lg label-purple arrowed-right'>类型名称</span><input type='text' name='addWordType_typeName' class='input-sm'  placeholder='仅支持：中文/字母/数字'  >&nbsp;&nbsp;\
					<span class='label label-lg label-purple arrowed-right'>类型</span>\
					<label class='inline'>\
						<input type='radio' name='addWordType_type' class='ace defaultCheck' value='0' >\
						<span class='lbl'>形容词</span>\
					</label>\
					<label class='inline'>\
						<input type='radio' name='addWordType_type' class='ace' value='1' >\
						<span class='lbl'>名词</span>\
					</label>",
		buttons:{
			"click" :{
				"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
					var $typeNamet=$(this).find("[name=addWordType_typeName]");
					var $type=$(this).find("[name=addWordType_type]:checked");
					var validateEmpty=[
						{ele:$typeNamet,name:'类型名称',minLength:1,maxLength:10}
						];
					if(!validateEmptyBatch(validateEmpty)||
							!validateInput(validateEmpty)){
						return false;
					}
				    if(!checkInputContent($typeNamet.val())){
				    	showMessageForCheck("请勿输入中文/字母/数字外的任何特殊字符");
				    	return false;
				    }
					if($type.length!=1){
						showMessageForCheck("请选择类型");
						return false;
					}
					var params={
							oid:$.trim($("#accountFilter").find("select[name=search_EQ_handicapCode]").val()),
							typeName:$.trim($typeNamet.val()),
							type:$type.val()
					}
					$.ajax({
						type: "POST",
						contentType: 'application/json;charset=UTF-8',
						dataType: 'JSON',
						url: '/newpay/addWordType',
						async: false,
						data: JSON.stringify(params),
						success: function (jsonObject) {
							if (jsonObject.status == 1) {
								showMessageForSuccess("新增成功");
								searchWordType_infoLoad();
							} else {
								showMessageForFail("新增失败：" + jsonObject.message);
							}
						}
					});

					setTimeout(function(){       
						$('body').addClass('modal-open');
					},500);
				}},
				"click2" :{"label" : "取消","className" : "btn btn-sm btn-default",
					"callback": function() {
						setTimeout(function(){       
							$('body').addClass('modal-open');
						},500);
					}}
		}
	});
}

/** 词汇类型 删除 */
var showModalRemoveWordType=function(handicapCode,wordTypeId){
	bootbox.confirm("确定删除?", function (result) {
		if (result) {
			var params={
					oid:handicapCode,
					id:wordTypeId
			}
			$.ajax({
				type: "POST",
				contentType: 'application/json;charset=UTF-8',
				dataType: 'JSON',
				url: '/newpay/removeWordType',
				async: false,
				data: JSON.stringify(params),
				success: function (jsonObject) {
					if (jsonObject.status == 1) {
						showMessageForSuccess("删除成功");
						searchWordType_infoLoad();
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


/** 形容词名词 分页查询 */
var showModalContent=function(){
	var $div = $("#contenModal");
	reset('searchContent_filter');
	$div.modal("toggle");
	$div.find("[name=type],[name=status]").click(function(){
		searchContent_infoLoad(0);
	});
	initTimePicker();
	searchContent_infoLoad(0);
}
var searchContent_infoLoad=function(CurPage){
	var $div=$("#contenModal");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#contentPage .Current_Page").text();
	var params={
			oid:$.trim($("#accountFilter").find("select[name=search_EQ_handicapCode]").val()),//盘口id读取当前查询条件
			type:$.trim($div.find("[name=type]:checked").val()),//类型，0：形容词，1：名词
			content:$.trim($div.find("[name=content]").val()),
			typeName:$.trim($div.find("[name=typeName]").val()),
			adminName:$.trim($div.find("[name=adminName]").val()),
			pageNo:(!CurPage||CurPage<=0)?0:CurPage-1,
					pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
	};
	if($div.find("[name=status]:checked").length==1){
		params.status=$div.find("[name=status]:checked").val();//状态：1-在用 0-停用 不填全部
	}
	var startAndEndTime=$div.find("[name=startAndEndTime]").val();
	if(startAndEndTime&&startAndEndTime.length>0){
		var timeArray=getTimeArray(startAndEndTime);
		if(timeArray&&timeArray.length>0){
			params.uptimeStart=new Date(timeArray[0]).getTime();
		}
		if(timeArray&&timeArray.length>1){
			params.uptimeEnd=  new Date(timeArray[1]).getTime();
		}
	}
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/newpay/findContentByCondition',
		async: false,
		data: JSON.stringify(params),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				var trs="";
				if(jsonObject.data){
					$.each(jsonObject.data,function(index,record){
						trs+="<tr>";
						trs+="<td>"+(index+1)+"</td>";
						if(record.type==1){//类型，0：形容词，1：名词
							trs+="<td><span class='label label-white middle  label-info'>名词</span></td>";
						}else{
							trs+="<td><span class='label label-purple middle  label-info'>形容词</span></td>";
						}
						if(record.status==1){//状态：1-在用 0-停用
							trs+="<td><span class='label label-sm label-success'>在用</span><br/><br/></td>";
						}else{
							trs+="<td><span class='label label-sm label-danger'>停用</span><br/><br/></td>";
						}
						trs+="<td>"+_checkObj(record.typeName)+"</td>";
						trs+="<td>"+_checkObj(record.content)+"</td>";
						trs+="<td>"+_checkObj(record.adminName)+"</td>";
						trs+="<td>"+_checkObj(record.uptime)+"</td>";
						trs+="<td>";
						trs+="<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='IncomePhoneNumber:updateFix:*'  \
							onclick='showModalUpdateContent(" + record.oid + "," + record.id +  ",\"" + _checkObj(record.content) + "\");' >\
							<span>修改</span>\
							</a>";
						if(record.status==1){//状态：1-在用 0-停用
							trs+= "<a class='btn btn-xs btn-white btn-danger btn-bold red contentRight' contentright='IncomePhoneNumber:updateFix:*' \
								onclick='showModalEnableContent(" + record.oid + "," + record.id + ",0);' >\
								<span>停用</span>\
								</a>";
						}else{
							trs+= "<a class='btn btn-xs btn-white btn-success btn-bold green contentRight' contentright='IncomePhoneNumber:updateFix:*'  \
								onclick='showModalEnableContent(" + record.oid + "," + record.id + ",1);' >\
								<span>在用</span>\
								</a>";
						}
						trs+="<a class='btn btn-xs btn-white btn-inverse btn-bold black contentRight' contentright='IncomePhoneNumber:updateFix:*'  \
							onclick='showModalRemoveContent(" + record.oid + "," + record.id + ");' >\
							<span>删除</span>\
							</a>";
						trs+="</td>";
						trs+="</tr>";
					});
				}
				$div.find("tbody").html(trs);
				//分页初始化
				showPading(jsonObject.page,"contentPage",searchContent_infoLoad,null,true);
				contentRight();
			} else {
				showMessageForFail("查询失败：" + jsonObject.message);
			}
		}
	});
}

/** 形容词/名词 内容新增 */
var showModalAddContent=function(oid,wordTypeId){
	bootbox.dialog({
		title:"<span class='blue bolder' >新增内容</span>",
		message: "<span class='label label-lg label-purple arrowed-right'>内容</span><input type='text' name='addContent_content' class='input-sm'  placeholder='仅支持：中文/字母/数字'  >&nbsp;&nbsp;\
					<span class='label label-lg label-purple arrowed-right'>类型</span>\
					<label class='inline'>\
						<input type='radio' name='addContent_type' class='ace defaultCheck' value='0' >\
						<span class='lbl'>形容词</span>\
					</label>\
					<label class='inline'>\
						<input type='radio' name='addContent_type' class='ace' value='1' >\
						<span class='lbl'>名词</span>\
					</label>",
		buttons:{
			"click" :{
				"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
					var $newContent=$(this).find("[name=addContent_content]");
					var $type=$(this).find("[name=addContent_type]:checked");
					var validateEmpty=[
						{ele:$newContent,name:'内容',minLength:1,maxLength:10}
						];
					if(!validateEmptyBatch(validateEmpty)||
							!validateInput(validateEmpty)){
						return false;
					}
				    if(!checkInputContent($newContent.val())){
				    	showMessageForCheck("请勿输入中文/字母/数字外的任何特殊字符");
				    	return false;
				    }
					if($type.length!=1){
						showMessageForCheck("请选择类型");
						return false;
					}
					var params={
							oid:oid,
							type:$type.val(),
							typeId:wordTypeId,//词汇类型ID
							content:$.trim($newContent.val())
					}
					$.ajax({
						type: "POST",
						contentType: 'application/json;charset=UTF-8',
						dataType: 'JSON',
						url: '/newpay/contentAdd',
						async: false,
						data: JSON.stringify(params),
						success: function (jsonObject) {
							if (jsonObject.status == 1) {
								showMessageForSuccess("新增成功");
								searchContent_infoLoad();
							} else {
								showMessageForFail("新增失败：" + jsonObject.message);
							}
						}
					});

					setTimeout(function(){       
						$('body').addClass('modal-open');
					},500);
				}},
				"click2" :{"label" : "取消","className" : "btn btn-sm btn-default",
					"callback": function() {
						setTimeout(function(){       
							$('body').addClass('modal-open');
						},500);
					}}
		}
	});
}

/** 形容词/名词 内容修改 */
var showModalUpdateContent=function(handicapCode,mobileId,oldContent){
	bootbox.dialog({
		title:"<span class='blue bolder' >修改内容<span>",
		message: "<input type='text' name='updateContent' class='input-sm' value="+oldContent+" placeholder='仅支持：中文/字母/数字'  >",
		buttons:{
			"click" :{
				"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
					var $newContent=$(this).find("[name=updateContent]");
					 var validateEmpty=[
					    	{ele:$newContent,name:'内容',minLength:1,maxLength:10}
					    ];
				    if(!validateEmptyBatch(validateEmpty)||
							!validateInput(validateEmpty)){
				    	return false;
				    }
				    if(!checkInputContent($newContent.val())){
				    	showMessageForCheck("请勿输入中文/字母/数字外的任何特殊字符");
				    	return false;
				    }
					var params={
							oid:handicapCode,
							id:mobileId,
							content:$.trim($newContent.val())
					}
					$.ajax({
						type: "POST",
						contentType: 'application/json;charset=UTF-8',
						dataType: 'JSON',
						url: '/newpay/contentModify',
						async: false,
						data: JSON.stringify(params),
						success: function (jsonObject) {
							if (jsonObject.status == 1) {
								showMessageForSuccess("修改成功");
								searchContent_infoLoad();
							} else {
								showMessageForFail("修改失败：" + jsonObject.message);
							}
						}
					});
					setTimeout(function(){       
						$('body').addClass('modal-open');
					},500);
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default",
				"callback": function() {
					setTimeout(function(){       
						$('body').addClass('modal-open');
					},500);
				}
			}
		},
		closeButton:false
	});
}

/** 形容词/名词 在用/可用 */
var showModalEnableContent=function(handicapCode,mobileId,newStatus){
	bootbox.confirm("确定修改状态?", function (result) {
		if (result) {
			var params={
					oid:handicapCode,
					id:mobileId,
					status:newStatus
			}
			$.ajax({
				type: "POST",
				contentType: 'application/json;charset=UTF-8',
				dataType: 'JSON',
				url: '/newpay/contentEnable',
				async: false,
				data: JSON.stringify(params),
				success: function (jsonObject) {
					if (jsonObject.status == 1) {
						showMessageForSuccess("修改成功");
						searchContent_infoLoad();
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

/** 形容词/名词 删除 */
var showModalRemoveContent=function(handicapCode,id){
	bootbox.confirm("确定删除?", function (result) {
		if (result) {
			var params={
					oid:handicapCode,
					id:id
			}
			$.ajax({
				type: "POST",
				contentType: 'application/json;charset=UTF-8',
				dataType: 'JSON',
				url: '/newpay/contentRemove',
				async: false,
				data: JSON.stringify(params),
				success: function (jsonObject) {
					//不管是否成功都刷新
					searchContent_infoLoad();
					if (jsonObject.status == 1) {
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


/** 修改未确认出款金额开关 */
var showModalModifyUoFlag=function(oid,mobileId){
	var telInfo=getTelInfo(oid,mobileId);
	if(!telInfo) return;
	var checkedONCheck="",checkedOFFCheck="";
	if(telInfo.uoFlag==1){//开
		checkedONCheck=" checked ";
	}else{//默认关
		checkedOFFCheck=" checked ";
	}
	bootbox.dialog({
		title:"<span class='blue bolder' >修改未确认出款金额开关</span>",
		message: "<span class='label label-lg label-purple arrowed-right'>未确认出款金额开关</span>\
					<label class='inline'>\
					<input type='radio' name='modify_uoFlag' class='ace' value='0' "+checkedOFFCheck+">\
					<span class='lbl'>关</span>\
					</label>\
					<label class='inline'>\
					<input type='radio' name='modify_uoFlag' class='ace' value='1' "+checkedONCheck+">\
					<span class='lbl'>开</span>\
					</label>",
			buttons:{
				"click" :{
					"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
						var $uoFlag=$(this).find("[name=modify_uoFlag]:checked");
						if($uoFlag.length!=1){
							showMessageForCheck("请选择开关");
							return false;
						}
						var params={
								oid:oid,
								mobileId:mobileId,
								uoFlag:$uoFlag.val(),
						}
						$.ajax({
							type: "POST",
							contentType: 'application/json;charset=UTF-8',
							dataType: 'JSON',
							url: '/newpay/modifyUoFlag',
							async: false,
							data: JSON.stringify(params),
							success: function (jsonObject) {
								if (jsonObject.status == 1) {
									showMessageForSuccess("修改成功");
									searchContent_infoLoad();
								} else {
									showMessageForFail("修改失败：" + jsonObject.message);
								}
							}
						});
					}},
					"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
			}
	});
	setTimeout(function(){       
		$('body').addClass('modal-open');
	},500);
}


/** 银行类别读取 */
var getBankTypeList=function(){
	//先写死数据
	bankTypeList4Customer= [];
	$.ajax({
        type: "GET",
        contentType: 'application/json;charset=UTF-8',
        dataType: 'JSON',
        url: '/newpay/findAll',
        async: false,
        success: function (jsonObject) {
            if (jsonObject.status == 1) {
            	bankTypeList4Customer=jsonObject.data;
            } else {
                console.log("查询银行下拉列表失败：" + jsonObject.message);
            }
        }
    });
}
var getAlipayDeviceStatus=function(status,WPStatus){
	//默认离线
	var statusInfo={
			'title':' title="设备状态" ',
			'text':'离线',
			'classColor':' label-grey '
	},WPStatusInfo={
			'title':' title="wap工具状态" ',
			'text':'离线',
			'classColor':' label-grey '
	};
	if(status||status==0){
		statusInfo={
				'title':' title="设备状态" ',
				'text':status==0?'可用':(status==1?'繁忙':'离线'),
				'classColor':status==0?' label-success ':(status==1?' label-error ':' label-grey ')
		}
	}
	if(WPStatus||WPStatus==0){
		WPStatusInfo={
				'title':' title="wap工具状态" ',
				'text':WPStatus==0?'可用':(WPStatus==1?'繁忙':'离线'),
				'classColor':WPStatus==0?' label-success ':(WPStatus==1?' label-error ':' label-grey ')
		}
	}
	var result='<span style="display:block;width:100%;" class="no-padding">';
	if(statusInfo){
		result+='<span '+statusInfo.title+' class="inline label '+statusInfo.classColor+'" >'+statusInfo.text+'</span>';
	}
	result+='<span>|</span>';
	result+='<span '+WPStatusInfo.title+' class="inline label '+WPStatusInfo.classColor+'" >'+WPStatusInfo.text+'</span>';
	result+='</span>';
	return result;
	//离线不执行
/*	if(((status||status==0)&&status!=2) && ((WPStatus||WPStatus==0)&&WPStatus!=2)){//两个状态都存在时
		var result='<span style="display:block;width:100%;" class="no-padding">';
		result+='<span '+statusInfo.title+' class="inline label '+statusInfo.classColor+'" >'+statusInfo.text+'</span>';
		result+='<span>|</span>';
		result+='<span '+WPStatusInfo.title+' class="inline label '+WPStatusInfo.classColor+'" >'+WPStatusInfo.text+'</span>';
		result+='</span>';
		return result;
	}else if((status||status==0)&&status!=2){
		return '<span '+statusInfo.title+' class="label '+statusInfo.classColor+'" style="display:block;width:100%;">'+statusInfo.text+'</span>';
	}else if((WPStatus||WPStatus==0)&&WPStatus!=2){
		return '<span '+WPStatusInfo.title+' class="label '+WPStatusInfo.classColor+'" style="display:block;width:100%;">'+WPStatusInfo.text+'</span>';
	}else{
		return '';
	}*/
}
/** 根据账号类型返回设备状态 */
var getDeviceStatus=function(status,title){
	var titleStr="";
	if(title){
		titleStr=' title="'+title+'" ';
	}
	if(status==0){
		return '<span '+titleStr+' class="label label-success" style="display:block;width:100%;">可用</span>';
	}else if(status==1){
		return '<span '+titleStr+' class="label label-error" style="display:block;width:100%;">繁忙</span>';
	}else{
		//默认离线
		return '<span class="label label-grey" style="display:block;width:100%;">离线</span>';
	}
}

getHandicapCode_select_one($("select[name='search_EQ_handicapCode']"));
showPage(0);
initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #searchBtn"),84,"refresh_customerManagerNumber");
//自动刷新导致失焦 先注释
//$("#refreshAccountListSelect [name=refreshSelect]").val(10);
//$("#refreshAccountListSelect [name=refreshSelect]").change();
$("#accountFilter").find("[name=type],[name=status],[name=currSysLevel]").click(function(){
	showPage(0);
});
$("[name=search_EQ_handicapCode],[name=search_EQ_level]").change(function(){
	showPage(0);
});
$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		showPage();
	}
});
getBankTypeList();
initTimePicker();
