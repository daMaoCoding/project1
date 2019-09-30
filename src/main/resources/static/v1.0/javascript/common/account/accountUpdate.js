//账号修改公用类（新）
var showUpdateAccountNewModal=function(accountId,fnName){
	var accountInfo=getAccountInfoById(accountId);
	$.ajax({
		type:"GET",
		async:false,
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/account.html",
		success : function(html){
			var $div=$(html).find("#UpdateAccountNewModal").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			//表单填充值
			$div.find("#accountId").val(accountId);
			if(accountInfo){
				var isIncome=accountInfo.type==accountTypeInBank;//是否入款卡
				var isOut=accountInfo.type==accountTypeOutBank;//是否出款卡
				var isReserve=accountInfo.type==accountTypeReserveBank;//是否备用卡
				var isBind=($.inArray(accountInfo.type, [accountTypeBindWechat,accountTypeBindAli,accountTypeThirdCommon,accountTypeBindCommon])!=-1);//是否下发卡
				var isPC=(!accountInfo.flag||accountInfo.flag==accountFlagPC);//是否PC
				var isFLW=(accountInfo.flag&&accountInfo.flag==accountFlagRefund);//是否返利网
				var isFLWZeroCredit=isFLW&&(!accountInfo.peakBalance||accountInfo.peakBalance<=0);//是返利网且没有保证金
				//标题 别名 账号
				$div.find("[name=alias]").html(accountInfo.alias);
				$div.find("[name=account]").html(hideAccountAll(accountInfo.account));
				/** 账号基本信息 */
				var titleName="----请选择----";//根据控件宽度给的默认选项
				if(isFLW){
					//开户人 开户行隐藏
				    $div.find(".flwHide").hide();
			    }else{
					//开户人
				    $div.find("input[name='owner']").val(accountInfo.owner);
					//开户行
				    getBankTyp_select($div.find("[name=bankType]"),accountInfo.bankType,titleName);
				    $div.find("input[name='bankName']").val(accountInfo.bankName);
			    }
                //层级
			    $div.find("[name='currSysLevel']").val(accountInfo.currSysLevel);
			    if($.inArray(accountInfo.currSysLevel, [currentSystemLevelOutter,currentSystemLevelDesignated])!=-1){
			    	$div.find("[name='currSysLevel']").attr("disabled","disabled");
			    }
				//省市
			    if(isIncome){
			    	$div.find(".label_status5").html("新卡");
			    	loadProvinceCity_select($div.find("select[name='province']"),$div.find("select[name='city']"),accountInfo.province,accountInfo.city,titleName);
			    }else{
			    	$div.find(".provinceCity").hide();
			    }
                /** 状态 */
                if($.inArray(accountInfo.status, [accountStatusNormal,accountStatusStopTemp,accountStatusEnabled])==-1){
                	//不是（在用 可用 停用）卡 不可以修改状态
                	 $div.find("#widget_status").remove();
                }else{
                    $div.find("[name=status][value='"+accountInfo.status+"']").prop("checked",true);
                	//非出款停用卡 不可以转可用/新卡（出款卡在用应使用【回收】按钮）
                    if(accountInfo.type!=accountTypeOutBank||accountInfo.status!=accountStatusStopTemp){
                    	if(accountInfo.status!=accountStatusEnabled) $div.find("[name=status][value=5]").attr("disabled","disabled");
                    }
                	if(isOut || isFLWZeroCredit ){
                     	//出款卡（应使用【分配】按钮）/ 返利网0保证金 不允许转在用
                     	if(accountInfo.status!=accountStatusNormal) $div.find("[name=status][value=1]").attr("disabled","disabled");
                     }
                }
                /** 用途 */
                if(isIncome&&isFLW){
                	//仅入款卡&&返利网来源 可修改用途 边入边出 1 自购卡（大额专用）2 大额专用（返利网）3 先入后出（正在出）4 先入后出（正在入）5
                    if(accountInfo.flagMoreStr&&(accountInfo.flagMoreStr==4||accountInfo.flagMoreStr==5)){
                    	//先入后出
                        $("[name=outEnable][value='1']").prop("checked",true);//out_enable：先入后出1 /边入边出2 
        			    $div.find("[name='limitPercentage']").val(_checkObj(accountInfo.limitPercentage));
        			    $div.find("[name='minBalance']").val(_checkObj(accountInfo.minBalance));
                    }else if(accountInfo.flagMoreStr&&accountInfo.flagMoreStr==1){
                    	//边入边出
                        $("[name=outEnable][value='2']").prop("checked",true);//out_enable：先入后出1 /边入边出2 
                    }
                    //当前用途
                }else{
                	$("#widget_outEnable").remove();
                }
                /** 其它 */
			    //限额
			    $div.find("[name='minInAmount']").val(_checkObj(accountInfo.minInAmount));//最小入款金额
			    $div.find("[name='peakBalance']").val(_checkObj(accountInfo.peakBalance));//余额峰值 保证金
			    $div.find("[name='limitBalance']").val(_checkObj(accountInfo.limitBalance));//余额告警
			    $div.find("[name='limitIn']").val(_checkObj(accountInfo.limitIn));//当日入款限额
			    $div.find("[name='limitOut']").val(_checkObj(accountInfo.limitOut));//当日出款限额	
			    $div.find("[name='limitOutOne']").val(_checkObj(accountInfo.limitOutOne));//单笔最高出款
			    $div.find("[name='limitOutOneLow']").val(_checkObj(accountInfo.limitOutOneLow));//单笔最低出款
			    $div.find("[name='lowestOut']").val(_checkObj(accountInfo.lowestOut));//最低余额限制
			    $div.find("[name='limitOutCount']").val(_checkObj(accountInfo.limitOutCount));//当日出款笔数
			    if(isFLW){
			    	//返利网 不允许修改余额峰值/保证金
			    	$div.find("[name=peakBalanceLabel]").html("保证金");
			    	$div.find("[name=peakBalance]").attr("placeholder","").attr("disabled","disabled");
			    	if(isFLWZeroCredit){
			    		//0信用额度 限额只读
			    		$div.find("[name='minInAmount']").attr("disabled","disabled");//最小入款金额
					    $div.find("[name='peakBalance']").attr("disabled","disabled");//余额峰值 保证金
					    $div.find("[name='limitBalance']").attr("disabled","disabled");//余额告警
					    $div.find("[name='limitIn']").attr("disabled","disabled");//当日入款限额
					    $div.find("[name='limitOut']").attr("disabled","disabled");//当日出款限额	
					    $div.find("[name='limitOutOne']").attr("disabled","disabled");//单笔最高出款
					    $div.find("[name='limitOutOneLow']").attr("disabled","disabled");//单笔最低出款
					    $div.find("[name='lowestOut']").attr("disabled","disabled");//最低余额限制
					    $div.find("[name='limitOutCount']").attr("disabled","disabled");//当日出款笔数
			    	}
			    	if(!accountInfo.peakBalance||accountInfo.peakBalance<=1000){//保证金低于1000可以设置0元信用额度
			    		$div.find(".creditStr").show();
			    	}
			    }
			    if(isOut){
			    	//出款卡 隐藏入款相关参数：修改最小入款金额
			    	$div.find("[name=minInAmount]").attr("placeholder","").attr("disabled","disabled").val("");
			    }else if(isReserve||isBind){
			    	//下发卡或者备用卡  隐藏入款/出款相关参数：最小入款金额/单笔最高出款/单笔最低出款/最低余额限制/当日出款笔数	
			    	$div.find("[name=minInAmount],[name=limitOutOne],[name=limitOutOneLow],[name=lowestOut],[name=limitOutCount]").attr("placeholder","").attr("disabled","disabled").val("");
			    }
			}
			//确定按钮绑定事件
			$div.find("#doUpdate").bind("click",function(){
				var param={
						accountInfo:accountInfo,
						isIncome:isIncome,//是否入款卡
						isOut:isOut,//是否出款卡
						isReserve:isReserve,//是否备用卡
						isBind:isBind,//是否下发卡
						isPC:isPC,//是否PC
						isFLW:isFLW,//是否返利网
						isFLWZeroCredit:isFLWZeroCredit   //是返利网且没有保证金
				};
				doUpdateAccountAll(param,fnName);
			});
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
				if(fnName){
					fnName();
				}
			});
		}
	});
}
var doUpdateAccountAll=function(param,fnName){
	var $div=$("#UpdateAccountNewModal");
	var accountInfo=param.accountInfo;
    //校验
    var data={ id:$div.find("#accountId").val() };
    var validateEmpty=[],validatePrint=[];//非空和输入校验数组
    //非返利网  开户人/开户行/银行类别 必填
	if(!param.isFLW){
	    var $owner = $div.find("[name='owner']");
	    var $bankType = $div.find("[name='bankType']");
	    var $bankName = $div.find("[name='bankName']");
		data.owner=$owner.val();
		data.bankType=$bankType.val();
		data.bankName=$bankName.val();
		validateEmpty.push({ele:$owner,name:'开户人'});
		validateEmpty.push({ele:$bankType,name:'银行类别'});
		validateEmpty.push({ele:$bankName,name:'开户支行'});
		validatePrint.push({ele:$owner,name:'开户人',minLength:2,maxLength:10});
		validatePrint.push({ele:$bankName,name:'开户支行',maxLength:50});
    }
	//层级
	var $currSysLevel = $div.find("[name='currSysLevel']");
	data.currSysLevel=$currSysLevel.val();
	validateEmpty.push({ele:$currSysLevel,name:'层级'});
	//入款 省市 必填
	 if(param.isIncome){
	    var $province=$div.find("[name='province']");
	    var $city=$div.find("[name='city']");
		data.province=$province.val();
		data.city=$city.val();
		validateEmpty.push({ele:$province,name:'省'});
		validateEmpty.push({ele:$city,name:'市'});
    }
    //状态
	var $status = $div.find("[name=status]:checked");
	if($status&&$status.val()&&$status.val()!=accountInfo.status){//有状态模块  且新旧值不一致 保存新状态
		//所有状态下填写了备注，会储存到操作记录
		var $remark=$div.find("[name='remark']");
		data.status=$status.val();
		data.remark=$remark.val();
		validatePrint.push({ele:$remark,name:'备注',minLength:5,maxLength:100});
		if($status.val()==accountStatusFreeze){//冻结备注必填 且保存到账号的remark字段
			validateEmpty.push({ele:$remark,name:'备注'});
		}
	}
	//用途 入款返利网必填：先入后出1 /边入边出2
	if(param.isIncome&&param.isFLW){
	    var $outEnable = $div.find("input[name='outEnable']:checked");
		data.outEnable=$outEnable.val();
		validateEmpty.push({ele:$outEnable,name:'用途'});
		if($outEnable.val()==1){
			//先入后出
			data.subType=0;
		    var $limitPercentage=$div.find("[name='limitPercentage']");
		    var $minBalance=$div.find("[name='minBalance']");
			data.limitPercentage=$limitPercentage.val().trim();
			data.minBalance=$minBalance.val().trim();
			var tips_limitPercentage="已设置了“先入后出(返利网)”用途的银行卡，百分比";
			var tips_minBalance="已设置了“先入后出(返利网)”用途的银行卡，保留余额";
			validateEmpty.push({ele:$limitPercentage,name:tips_limitPercentage});
			validateEmpty.push({ele:$minBalance,name:tips_minBalance});
			validatePrint.push({ele:$limitPercentage,name:tips_limitPercentage,minEQ:1,maxEQ:100});
			validatePrint.push({ele:$limitPercentage,name:tips_minBalance,minEQ:1,maxEQ:10000});
		}else{
			//边入边出
			data.subType=3;
		}
	}
	//限额  修改不做必填校验，不填就使用之前的值，如果之前没值，会自动取系统配置
    var $minInAmount = $div.find("input[name='minInAmount']");//最小入款金额
    var $peakBalance = $div.find("input[name='peakBalance']");//余额峰值 保证金
    var $limitBalance = $div.find("input[name='limitBalance']");//余额告警
    var $limitIn = $div.find("input[name='limitIn']");//当日入款限额
    var $limitOut = $div.find("input[name='limitOut']");//当日出款限额	
    var $limitOutOne = $div.find("input[name='limitOutOne']");//单笔最高出款
    var $limitOutOneLow = $div.find("input[name='limitOutOneLow']");//单笔最低出款
    var $lowestOut = $div.find("input[name='lowestOut']");//最低余额限制
    var $limitOutCount = $div.find("input[name='limitOutCount']");//当日出款笔数
	data.minInAmount=$minInAmount.val();
	data.peakBalance=$peakBalance.val();
	data.limitBalance=$limitBalance.val();
	data.limitIn=$limitIn.val();
	data.limitOut=$limitOut.val();
	data.limitOutOne=$limitOutOne.val();
	data.limitOutOneLow=$limitOutOneLow.val();
	data.lowestOut=$lowestOut.val();
	data.limitOutCount=$limitOutCount.val();
	validatePrint.push({ele:$minInAmount,name:'最小入款金额',type:'amountPlus',minEQ:0});
	validatePrint.push({ele:$peakBalance,name:'余额峰',type:'amountCanZero',maxEQ:1000000});
	validatePrint.push({ele:$limitBalance,name:'余额告警',type:'amountPlus',min:0,maxEQ:50000});
	validatePrint.push({ele:$limitIn,name:'当日入款限额',type:'amountPlus'});
	validatePrint.push({ele:$limitOut,name:'当日出款限额',type:'amountPlus'});
	validatePrint.push({ele:$limitOutOne,name:'最高单笔出款限额',type:'amountPlus',min:0,maxEQ:50000});
	validatePrint.push({ele:$limitOutOneLow,name:'最低单笔出款限额',minEQ:0,maxEQ:50000});
	validatePrint.push({ele:$lowestOut,name:'最低余额限制',type:'amountPlus',min:0,maxEQ:50000});
	validatePrint.push({ele:$limitOutCount,name:'当日出款笔数',minEQ:0,maxEQ:500});
	//0信用额度
    var $zerocredits = $div.find("[name=zerocredits]:checked");
    if((!accountInfo.peakBalance||accountInfo.peakBalance<=1000)&&$zerocredits&&$zerocredits.val()){
    	 data.zerocredits=$zerocredits.val();
    }
	if(!validateEmptyBatch(validateEmpty)||!validateInput(validatePrint)){
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
		return;
	}
	bootbox.confirm("确定修改账号?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/accountBase/update',
				async:false,
				data:data,
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	//操作成功提示
			            $div.modal("toggle");
			            if(jsonObject.data&&jsonObject.data.updateFailed_outEnable){
			            	showMessageForSuccess('账号修改成功，用途修改失败。<span class="red bolder"><i class="ace-icon fa fa-info-circle bigger-110 red"></i>&nbsp;仅绑定了云闪付的卡，才会成功转为边入边出卡</span>');
			            }else{
				        	showMessageForSuccess("修改成功");
			            }
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

/** 弹窗查看账号基本信息，可复制 */
var showModal_accountBaseInfo=function(accountId){
	var accountInfo=getAccountInfoById(accountId);
	if(!accountId||!accountInfo){
		return;
	}
	var html='<div class="modal fade" aria-hidden="false" >\
		<div class="modal-dialog modal-lg"  style="width:700px;">\
			<div class="modal-content">\
				<div class="modal-header no-padding text-center">\
					<div class="table-header">\
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button>状态\
					</div>\
				</div>\
				<div class="modal-body no-padding-bottom">\
					<h6 class="header smaller center lighter blue no-padding no-margin-top">基本信息</span></h6>\
					<table class="table table-bordered min-width100">\
						<thead></thead>\
						<tbody>\
							<tr>\
								<td class="input-group-addon">盘口</td><td><span name="handicapName"></span></td>\
								<td class="input-group-addon">层级</td><td><span name="currSysLevelName"></span></td>\
							</tr>\
							<tr>\
								<td class="input-group-addon">层级</td><td colspan="3"><span name="levelNameToGroup"></span></td>\
							</tr>\
							<tr>\
								<td class="input-group-addon">账号</td><td><span name="account"></span></td>\
								<td class="input-group-addon">编号</td><td><span name="alias"></span></td>\
							</tr>\
							<tr>\
								<td class="input-group-addon">开户人</td><td><span name="owner"></span></td>\
								<td class="input-group-addon">类型</td><td><span name="type"></span></td>\
							</tr>\
							<tr>\
								<td class="input-group-addon">来源</td><td><span name="flag"></span></td>\
								<td class="input-group-addon">状态</td><td><span name="statusStr"></span></td>\
							</tr>\
							<tr>\
								<td class="input-group-addon">银行类别</td><td><span name="bankType"></span></td>\
								<td class="input-group-addon">省份</td><td><span name="province"></span></td>\
							</tr>\
							<tr>\
								<td class="input-group-addon">开户支行</td><td><span name="bankName"></span></td>\
								<td class="input-group-addon">城市</td><td><span name="city"></span></td>\
							</tr>\
							<tr>\
								<td class="input-group-addon">银行余额</td><td><span name="bankBalance"></span></td>\
								<td class="input-group-addon">GPS-物理机</td><td><span name="GPS1"></span></td>\
							</tr>\
							<tr>\
								<td class="input-group-addon">系统余额</td><td><span name="balance"></span></td>\
								<td class="input-group-addon">GPS-虚拟机</td><td><span name="GPS2"></span></td>\
							</tr>\
						</tbody>\
						<tfoot></tfoot>\
					</table>\
					<div style="display:none;" class="outEnableDiv" ><h6 class="header smaller center lighter blue no-padding no-margin-top">用途&nbsp;&nbsp;&nbsp;&nbsp;<span name="outEnable" class="red"></span></h6></div>\
					<div style="display:none;" class="outEnableDiv input-group-addon" ><span name="outEnableDetail"></span></div>\
					<div style="display:none;height:20px;" class="outEnableDiv" >&nbsp;</div>\
					<h6 class="header smaller center lighter blue no-padding no-margin-top">使用限制</span></h6>\
			    	<table class="table table-bordered min-width100">\
						<thead></thead>\
						<tbody>\
							<tr>\
			        			<td class="input-group-addon"><span>最小入款金额</span><td><span name="minInAmount"></span></td>\
								<td class="input-group-addon"><span></span><td><span></span></td>\
							<tr>\
			        			<td class="input-group-addon"><span name="peakBalanceLabel">余额峰值</span><td><span name="peakBalance"></span></td>\
								<td class="input-group-addon"><span>余额告警</span><td><span name="limitBalance"></span></td>\
							</tr>\
							<tr>\
			        			<td class="input-group-addon"><span>当日入款限额</span></td><td><span name="limitIn"></span>\
			        			<td class="input-group-addon"><span>当日出款限额</span></td><td><span name="limitOut"></span>\
							</tr>\
							<tr>\
			        			<td class="input-group-addon"><span>单笔最高出款</span></td><td><span name="limitOutOne"></span>\
			        			<td class="input-group-addon"><span>单笔最低出款</span></td><td><span name="limitOutOneLow"></span>\
							</tr>\
							<tr>\
			        			<td class="input-group-addon"><span>最低余额限制</span></td><td><span name="lowestOut"></span>\
			        			<td class="input-group-addon"><span>当日出款笔数</span></td><td><span name="limitOutCount"></span>\
							</tr>\
						</tbody>\
					</table>\
					<h6 class="header smaller center lighter blue no-padding no-margin-top">操作信息</span></h6>\
					<table class="table table-bordered min-width100">\
						<thead></thead>\
						<tbody>\
							<tr>\
								<td class="input-group-addon"><span>创建人</span></td><td><span name="creatorStr"></span>\
								<td class="input-group-addon"><span>创建时间</span></td><td><span name="createTimeStr"></span>\
							<tr>\
							<tr>\
								<td class="input-group-addon"><span>更新人</span></td><td><span name="modifierStr"></span>\
								<td class="input-group-addon"><span>更新时间</span></td><td><span name="updateTimeStr"></span>\
							<tr>\
							<tr>\
								<td class="input-group-addon"><span>持卡人</span></td><td><span name="holderStr"></span>\
								<td class="input-group-addon"><span>锁定人</span></td><td><span name="lockerStr"></span>\
							<tr>\
							<tr>\
								<td class="input-group-addon"><span>审核人</span></td><td><span name="incomeAuditor"></span>\
								<td class="input-group-addon"><span></span></td><td><span name=""></span>\
							<tr>\
						</tbody>\
					</table>\
					<h6 class="header smaller center lighter blue no-padding no-margin-top">备注</span></h6>\
					<div><span name="remark"></span></div>\
					<br/>\
				</div>\
			</div>\
		</div>\
	</div>';
	var $div=$(html).appendTo($("body"));
	//固定列宽
	$div.find("table td").attr("style","width:190px;");
	$div.find("table td.input-group-addon").attr("style","width:110px;");
	var isIncome=(accountInfo.type==accountTypeInBank);
	var isFLW=(accountInfo.flag&&accountInfo.flag*1==2);
	if(isFLW){
		$div.find("[name=peakBalanceLabel]").html("保证金");
		accountInfo.holderStr="返利网";
	}
	//基本信息
	$div.find("[name=handicapName]").html(_checkObj(accountInfo.handicapName));
	$div.find("[name=currSysLevelName]").html(_checkObj(accountInfo.currSysLevelName));
	$div.find("[name=levelNameToGroup]").html(_checkObj(accountInfo.levelNameToGroup));
	$div.find("[name=account]").html(hideAccountAll(accountInfo.account));
	$div.find("[name=alias]").html(_checkObj(accountInfo.alias)+getCopyHtml(accountInfo.alias));
	$div.find("[name=owner]").html(hideName(accountInfo.owner));
	if(isIncome){
		accountInfo.typeStr="银行入款卡";
		if(accountInfo.subType){
			if(accountInfo.subType==1){
				accountInfo.typeStr="支付宝入款卡";
			}else if(accountInfo.subType==2){
				accountInfo.typeStr="微信入款卡";
			}else if(accountInfo.subType==3){
				accountInfo.typeStr="云闪付入款卡";
			}
		}
	}
	$div.find("[name=type]").html(_checkObj(accountInfo.typeStr));
	$div.find("[name=flag]").html(isFLW?"返利网":"PC");
	$div.find("[name=statusStr]").html(_checkObj(accountInfo.statusStr));
	$div.find("[name=bankType]").html(_checkObj(accountInfo.bankType)+getCopyHtml(accountInfo.bankType));
	$div.find("[name=province]").html(_checkObj(accountInfo.province)+getCopyHtml(accountInfo.province));
	$div.find("[name=bankName]").html(_checkObj(accountInfo.bankName)+getCopyHtml(accountInfo.bankName));
	$div.find("[name=city]").html(_checkObj(accountInfo.city)+getCopyHtml(accountInfo.city));
	$div.find("[name=balance]").html(_checkObj(accountInfo.balance));
	$div.find("[name=bankBalance]").html(_checkObj(accountInfo.bankBalance));
	if(accountInfo.gps){
		$div.find("[name=GPS2]").html(_checkObj(accountInfo.gps)+getCopyHtml(accountInfo.gps));//虚拟机
		var Host=getHostByIp(accountInfo.gps);//所在物理机
		$div.find("[name=GPS1]").html(_checkObj(Host.ip)+getCopyHtml(Host.ip));//物理机
	}
	//用途 入款卡
	if(isIncome){
		var outEnableDetail="未设置详细参数";
		if(accountInfo.flagMoreStr==1){
			outEnableDetail="可一边入款一边出款";
		}else if(accountInfo.flagMoreStr==2){
			outEnableDetail="入款PC卡，公司自有大额专用(自购卡)";
		}else if(accountInfo.flagMoreStr==4||accountInfo.flagMoreStr==5){
			if(accountInfo.limitPercentage&&accountInfo.minBalance){
				outEnableDetail="当余额大于或等于信用额度的“"+accountInfo.limitPercentage+"%”时，转为出款卡，当余额小于“"+accountInfo.minBalance+"元”，再转为入款卡";
			}
		}
		$div.find(".outEnableDiv").show();
		$div.find("[name=outEnable]").html(getFlagMoreStrHTML(accountInfo.flagMoreStr));
		$div.find("[name=outEnableDetail]").html(outEnableDetail+getCopyHtml(outEnableDetail));
	}
	//使用限制
	$div.find("[name=minInAmount]").html(_checkObj(accountInfo.minInAmount));
	$div.find("[name=peakBalance]").html(_checkObj(accountInfo.peakBalance));
	$div.find("[name=limitBalance]").html(_checkObj(accountInfo.limitBalance));
	$div.find("[name=limitIn]").html(_checkObj(accountInfo.limitIn));
	$div.find("[name=limitOut]").html(_checkObj(accountInfo.limitOut));
	$div.find("[name=limitOutOne]").html(_checkObj(accountInfo.limitOutOne));
	$div.find("[name=limitOutOneLow]").html(_checkObj(accountInfo.limitOutOneLow));
	$div.find("[name=lowestOut]").html(_checkObj(accountInfo.lowestOut));
	$div.find("[name=limitOutCount]").html(_checkObj(accountInfo.limitOutCount));
	//操作信息
	$div.find("[name=creatorStr]").html(_checkObj(accountInfo.creatorStr));
	$div.find("[name=createTimeStr]").html(_checkObj(accountInfo.createTimeStr));
	$div.find("[name=modifierStr]").html(_checkObj(accountInfo.modifierStr));
	$div.find("[name=updateTimeStr]").html(_checkObj(accountInfo.updateTimeStr));
	$div.find("[name=holderStr]").html(_checkObj(accountInfo.holderStr));
	$div.find("[name=lockerStr]").html(_checkObj(accountInfo.lockerStr));
	$div.find("[name=incomeAuditor]").html(_checkObj(accountInfo.incomeAuditor));
	//备注
	$div.find("[name=remark]").html((accountInfo.remark?accountInfo.remark.replace(/\n/g,'<br/>'):"无"));
	
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		// 关闭窗口清除model
		$div.remove();
	});
}

var getCopyHtml=function(text){
	if(!text){
		return '';
	}
	return '&nbsp;&nbsp;<i class="fa fa-copy orange  clipboardbtn" style="cursor:pointer" data-clipboard-text="'+text+'"></i>';
}