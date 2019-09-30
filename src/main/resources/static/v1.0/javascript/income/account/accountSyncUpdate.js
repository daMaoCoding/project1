currentPageLocation = window.location.href;
var request=getRequest();//accountId 
var $divLevel=$("#level_widget"),$divFront=$("#front_widget"),$divNickName=$("#nickName_widget"),$divTips=$("#tips_widget"),$divStatus=$("#status_widget"),$divInfo=$("#info_widget");
var accountInfo;

/**
 * 刷新系统设置
 * reloadSize
 */
var reloadSetting=function(reloadSize){
	if(!request.accountId){
		showMessageForFail("账号不存在");
		return;
	}
	accountInfo=getAccountInfoById(request.accountId);
	if(!accountInfo){
		showMessageForFail("账号不存在");
		return;
	}
	if(!accountInfo.handicapId){
		showMessageForFail("请先为账号设定盘口");
		return;
	}
	//页头信息
	var $title=$("#accountInfo");
	$title.find("[name=account]").attr("data-toggle","accountInfoHover"+accountInfo.id).text(hideAccountAll(accountInfo.account));
	//悬浮提示
	if(accountInfo.type==accountTypeInBank){
		loadHover_accountInfoHover([{"id":accountInfo.id}]);
	}else{
		loadHover_accountInfoHover([{"id":accountInfo.id,'type':'ali_wechat'}]);
	}
	$title.find("[name=bankType]").text(accountInfo.bankType?accountInfo.bankType:'');
	$title.find("[name=bankName]").text(accountInfo.bankName?accountInfo.bankName:'');
	$title.find("[name=owner]").text(accountInfo.owner?accountInfo.owner:'');
	var accountSync=getAccountSync(request.accountId);
	var json=accountSync&&accountSync.json?JSON.parse(accountSync.json):{};
	//层级设置
	if(!reloadSize||reloadSize==1){
		var $levelListTable=$divLevel.find("#levelListTable");
		var trHtml="",allLevelList=getLevelByHandicapId(accountInfo.handicapId);
		if(allLevelList&&allLevelList.length>0){
			allLevelList.map(function(record){
				trHtml+="<tr>";
				trHtml+='<td>\
					<input type="hidden" name="levelCode'+record.code+'" value="'+record.code+'" >\
					<input type="checkbox" name="levelCheckbox" value="'+record.code+'" style="width:20px;height:20px;" >\
					</td>';
				trHtml+='<td name="levelName'+record.code+'" >'+record.name+'</td>';
				//单笔最大入款金额
				trHtml+='<td><input type="number" name="limitInOne'+record.code+'" class="input-sm"></td>';
				//当日累计最大金额
				trHtml+='<td><input type="number" name="limitIn'+record.code+'" class="input-sm" ></td>';
				trHtml+="</tr>";
			});
			$levelListTable.find("tbody").html(trHtml);
		}
		if(json&&json.levelList){
			json.levelList.map(function(record){
				$levelListTable.find("[name=levelCheckbox][value="+record.levelCode+"]").prop("checked","checked");
				$levelListTable.find("[name=limitInOne"+record.levelCode+"]").val(record.limitInOne?record.limitInOne:'');
				$levelListTable.find("[name=limitIn"+record.levelCode+"]").val(record.limitIn?record.limitIn:'');
			});
		}
	}
	//前端设置
	if(!reloadSize||reloadSize==2){
		if(json&&json.font){
			$divFront.find("[name=app]").prop("checked",json.font.app);
			$divFront.find("[name=website]").prop("checked",json.font.website);
			$divFront.find("[name=wap]").prop("checked",json.font.wap);
		}
	}
	//昵称和备注
	if(!reloadSize||reloadSize==3){
		if(json&&json.nickName){
			$divNickName.find("[name=nickNameIsCheck]").prop("checked",json.nickName.nickNameIsCheck);
			$divNickName.find("[name=remarkIsCheck]").prop("checked",json.nickName.remarkIsCheck);
			$divNickName.find("[name=nickName]").val(json.nickName.nickName);
			$divNickName.find("[name=remark]").val(json.nickName.remark);
		}
	}
	//温馨提示
	if(!reloadSize||reloadSize==4){
		if(json&&json.tips){
			$divTips.find("[name=useSameTipsIsChoice]").prop("checked",json.tips.useSameTipsIsChoice);
			$divTips.find("[name=transfer]").val(json.tips.transfer);
			$divTips.find("[name=transferQr]").val(json.tips.transferQr);
		}
	}
	//状态设置
	if(!reloadSize||reloadSize==5){
		//此处以账号状态判断状态
		if(accountInfo&&accountInfo.status){
			$divStatus.find("[name=status][value="+accountInfo.status+"]").prop('checked','checked');
			$divStatus.find("[name=remark]").val("");
		}
	}
	//资料设置
	if(!reloadSize||reloadSize==6){
		loadProvinceCity_select($divInfo.find("[name=province_select]"),$divInfo.find("[name=city_select]"));
		//此处以账号状态判断状态
		if(json&&json.info){
			$divInfo.find("[name=province_select]").val(json.info.province);
			setTimeout(function(){
				$divInfo.find("[name=province_select]").change();
				$divInfo.find("[name=city_select]").val(json.info.city);
			},10);
			$divInfo.find("[name=website]").val(json.info.website);
			$divInfo.find("[name=app]").val(json.info.app);
			$divInfo.find("[name=wap]").val(json.info.wap);
			$divInfo.find("[name=warnMoney]").val(json.info.warnMoney);
			$divInfo.find("[name=stopAmountProportion]").val(json.info.stopAmountProportion);
		}
	}
	//在用禁止修改资料与前端设置
	if(accountInfo.status==accountStatusNormal){
		$divInfo.find("input,select,textarea").attr("disabled",true);
		$divFront.find("input").attr("disabled",true);
		$divInfo.find("button").hide();
		$divFront.find("button").hide();
	}else{
		$divInfo.find("input,select,textarea").removeAttr("disabled");
		$divFront.find("input").removeAttr("disabled");
		$divInfo.find("button").show();
		$divFront.find("button").show();
	}
}


/**
 * 更新系统设置
 */
var updateSetting=function(updateSize){
	//读取设置信息
	var accountSync=getAccountSync(request.accountId);
	var json=accountSync&&accountSync.json?JSON.parse(accountSync.json):{};
	var validate=new Array();
	var validatePrint=new Array();
	//层级设置
	if(!updateSize||updateSize==1){
		var levelList=new Array();
		$divLevel.find("[name=levelCheckbox]:checked").each(function(index,checkbox){
			var code=$(checkbox).val();
			var levelName=$divLevel.find("[name=levelName"+code+"]").text();
			var $limitInOne=$divLevel.find("[name=limitInOne"+code+"]");
			var $limitIn=$divLevel.find("[name=limitIn"+code+"]");
			levelList.push({
				"levelCode":code,
				"limitInOne":$limitInOne.val(),
				"limitIn":$limitIn.val()
			});
			validatePrint.push(
					{ele:$limitInOne,name:'【层级设置】'+levelName+',单笔最大入款金额',type:'amountPlus'},
					{ele:$limitIn,name:'【层级设置】'+levelName+',单日累计最大金额',type:'amountPlus'}
			);
		});
		if(levelList.size<1){
			showMessageForFail("请选中至少1个层级");
			return;
		}
		json.levelList=levelList;
	}
	//前端选择 在用不可修改
	if(accountInfo.status!=accountStatusNormal&&(!updateSize||updateSize==2)){
		json.font={
			"app":$divFront.find("[name=app]").prop("checked")==true,
			"website":$divFront.find("[name=website]").prop("checked")==true,
			"wap":$divFront.find("[name=wap]").prop("checked")==true
		};
	}
	//昵称和备注
	if(!updateSize||updateSize==3){
		var $nickName=$divNickName.find("[name=nickName]");
		var $remark=$divNickName.find("[name=remark]");
		json.nickName={
			"nickNameIsCheck":$divNickName.find("[name=nickNameIsCheck]").prop("checked")==true,
			"remarkIsCheck":$divNickName.find("[name=remarkIsCheck]").prop("checked")==true,
			"nickName":$nickName.val(),
			"remark":$remark.val()
		};
		validatePrint.push(
				{ele:$nickName,name:'【昵称和备注】通道昵称',maxLength:25},
				{ele:$remark,name:'【昵称和备注】备注',maxLength:100}
		);
	}
	//温馨提示
	if(!updateSize||updateSize==4){
		var $transfer=$divTips.find("[name=transfer]");
		var $transferQr=$divTips.find("[name=transferQr]");
		json.tips={
			"useSameTipsIsChoice":$divTips.find("[name=useSameTipsIsChoice]").prop("checked")==true,
			"transfer":$transfer.val(),
			"transferQr":$transferQr.val()
		};
		validate.push(
				{ele:$transfer,name:'【APP、WAP银行转账温馨提示】转账温馨'},
				{ele:$transferQr,name:'【APP、WAP银行转账温馨提示】转账支付确认码温馨'}
		);
		validatePrint.push(
				{ele:$transfer,name:'【APP、WAP银行转账温馨提示】转账温馨',maxLength:25},
				{ele:$transferQr,name:'【APP、WAP银行转账温馨提示】转账支付确认码温馨',maxLength:100}
		);
	}
	//状态
	if(!updateSize||updateSize==5){
		//账号表状态修改
		var status=$divStatus.find("[name=status]:checked").val();
		var remark=$divStatus.find("[name=remark]").val();
		var url="";
		//状态无变更不需要保存
		if(accountInfo.status!=status){
			if(status==accountStatusNormal){
				//1 在用
				url=API.r_account_asin4OutwardAccount;
			}else if(status==accountStatusFreeze){
				//3 冻结
				url=API.r_account_toFreezeForver;
				if(!remark||remark.length<5||remark.length>100){
					showMessageForFail("【状态设置】请输入备注，5-100字之间");
					return;
				}
			}else if(status==accountStatusStopTemp){
				//4 停用
				url=API.r_account_toStopTemp;
			}else{
				return;
			}
			//恢复不强制写备注 但是如果写了 就要按字数来
			if(remark&&(remark.length<5||remark.length>100)){
				showMessageForFail("【状态设置】请输入备注，5-100字之间");
				return;
			}
			var saveStatusIsOk=false;
			$.ajax({
				dataType:'json',
				type:"get",
				url:url,
				async:false,
				data:{
					"accountId":request.accountId,
					"remark":remark
				},
				success:function(jsonObject){
					if(jsonObject&&jsonObject.status&&jsonObject.status == -1){
						showMessageForFail("保存失败,状态设置异常："+jsonObject.message);
					}else{
						saveStatusIsOk=true;
						//保存状态
						if(!json.info){
							json.info={};
						}
						json.info["status"]=$divStatus.find("[name=status]:checked").val();
					}
				}
			});
			if(!saveStatusIsOk){
				//保存失败不往下执行
				return;
			}
		}
	}
	//资料设置 在用不可修改
	if(accountInfo.status!=accountStatusNormal&&(!updateSize||updateSize==6)){
		var $province=$divInfo.find("[name=province_select]");
		var $city=$divInfo.find("[name=city_select]");
		var $website=$divInfo.find("[name=website]");
		var $app=$divInfo.find("[name=app]");
		var $wap=$divInfo.find("[name=wap]");
		var $warnMoney=$divInfo.find("[name=warnMoney]");
		var $stopAmountProportion=$divInfo.find("[name=stopAmountProportion]");
		if(!json.info){
			json.info={};
		}
		json.info["province"]=$province.val();
		json.info["city"]=$city.val();
		json.info["website"]=$.trim($website.val(),true);
		json.info["app"]=$.trim($app.val(),true);
		json.info["wap"]=$.trim($wap.val(),true);
		json.info["warnMoney"]=$.trim($warnMoney.val(),true);
		json.info["stopAmountProportion"]=$.trim($stopAmountProportion.val(),true);
		validate.push(
				{ele:$province,name:'【资料设置】省'},
				{ele:$city,name:'【资料设置】市'}
		);
		validatePrint.push(
				{ele:$website,name:'【资料设置】网站提示文字',maxLength:64},
				{ele:$app,name:'【资料设置】APP提示文字',maxLength:64},
				{ele:$wap,name:'【资料设置】WAP提示文字 ',maxLength:64},
				{ele:$warnMoney,name:'【资料设置】停用金额',type:'amountPlus'},
				{ele:$stopAmountProportion,name:'【资料设置】停用百分比(%)',minEQ:0,maxEQ:100}
		);
	}
	//针对已存在的数据，修改平台资料时保存账号修改按钮的信息
	if(accountInfo){
		if(!json.info){
			json.info={};
		}
		if(!json.info["account"]){
			//刷新其它基础信息
			json.info["account"]=accountInfo.account;
			json.info["bankType"]=accountInfo.bankType?accountInfo.bankType:'';
			json.info["bankName"]=accountInfo.bankName?accountInfo.bankName:'';
			json.info["owner"]=accountInfo.owner?accountInfo.owner:'';
			if(!json.info["status"]){
        		json.info["status"]=accountInfo.status;
			}
			if(!json["handicap"]){
				handicap_list_all.map(function(handicap){
					if(handicap&&handicap.id==accountInfo.handicapId){
						json["handicap"]=handicap.code;
						return;
					}
				});
			}
		}
	}
	//校验
	if(!validateEmptyBatch(validate)||!validateInput(validatePrint)){
    	return;
    }
	$.ajax({
		type:"PUT",
		dataType:'JSON',
		url:'/r/accountSync/save',
		async:false,	
		data:{
			"accountId":request.accountId,
			"json":JSON.stringify(json)
		},
		success:function(jsonObject){
			if(jsonObject&&jsonObject.status==1){
				showMessageForSuccess("保存成功");
				accountInfo=getAccountInfoById(request.accountId);
			}else{
				showMessageForFail("保存失败"+jsonObject.message);
			}
		}
	});
}


contentRight();
reloadSetting();