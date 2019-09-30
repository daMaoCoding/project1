currentPageLocation = window.location.href;
var $divOutdraw = $("#outdraw_widget"), $divFinance = $("#finance_widget"),$transferTestAccount_widget = $("#transferTestAccount_widget"),$masLog_widget = $("#masLog_widget");
var start = "";
/**
 * 刷新系统设置
 * reloadSize
 */
var reloadSetting = function (reloadSize) {
    if (!sysSetting) {
        //重新查询
        $.ajax({
            type: "POST",
            url: '/r/set/findAllToMap',
            dataType: 'JSON',
            async: false,
            success: function (res) {
                if (res.status != 1) {
                    showMessageForFail('系统设置初始化失败，请稍后再试。');
                    return;
                }
                sysSetting = res.data;
            }
        });
    }
    //初始化数据
    if (!reloadSize || reloadSize == 2) {
        start = sysSetting.OUTDRA_HALT_ALLOC_START_TIME;
    }
    if (!reloadSize || reloadSize == 3) {
        $divFinance.find("[name=FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY]").val(sysSetting.FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY);
        $divFinance.find("[name=INCOME_LIMIT_MONITOR_TIMEOUT]").val(sysSetting.INCOME_LIMIT_MONITOR_TIMEOUT);
        $divFinance.find("[name=FINANCE_ACCOUNT_BALANCE_ALARM]").val(sysSetting.FINANCE_ACCOUNT_BALANCE_ALARM);
    }
    if (!reloadSize || reloadSize == 5) {
        if (sysSetting.TRANSFER_TEST_ACCOUNT) {
            $transferTestAccount_widget.find("[name=TRANSFER_TEST_ACCOUNT]").val(sysSetting.TRANSFER_TEST_ACCOUNT);
            $transferTestAccount_widget.find("[name=TRANSFER_TEST_BANKNAME]").val(sysSetting.TRANSFER_TEST_BANKNAME);
            $transferTestAccount_widget.find("[name=TRANSFER_TEST_OWNER]").val(sysSetting.TRANSFER_TEST_OWNER);
            $transferTestAccount_widget.find("[name=TRANSFER_TEST_AMOUNT]").val(sysSetting.TRANSFER_TEST_AMOUNT);
        }
    }
    if (!reloadSize || reloadSize == 6) {
//		 $.ajax({
//	        type:'GET',
//	        url:'xxx',
//	        success:function (res) {
//	            if (res){
//	                var checkedLoglevel = res.effectiveLevel;
//	                $.each($masLog_widget.find('input[name=masLogLevel]'),function (index,result) {
//	                    if ($(result).val()==checkedLoglevel) {
//	                        $(result).prop('checked','checked');
//	                    }
//	                });
//	            }
//	        }
//		});
    }
    if (!reloadSize || reloadSize == 7) {
        loadSynchSwitch();
   }
}

/**
 * 更新系统设置
 */
var updateSetting = function (updateSize) {
    var keysArray = new Array(), valsArray = new Array();
    var validate = new Array();
    if (!updateSize || updateSize == 2) {
        var $OUTDRA_HALT_ALLOC_START_TIME = $divOutdraw.find("[name=OUTDRA_HALT_ALLOC_START_TIME]");
        validate.push({ele: $OUTDRA_HALT_ALLOC_START_TIME, name: '停止派单时间'});
        $.each($divOutdraw.find("input"), function (index, result) {
            keysArray.push($(result).attr("name"));
            valsArray.push($(result).val());
        });
    }
    if (!updateSize || updateSize == 3) {
        var $FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY = $divFinance.find("[name=FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY]");
        var $INCOME_LIMIT_MONITOR_TIMEOUT = $divFinance.find("[name=INCOME_LIMIT_MONITOR_TIMEOUT]");
        var $FINANCE_ACCOUNT_BALANCE_ALARM = $divFinance.find("[name=FINANCE_ACCOUNT_BALANCE_ALARM]");
        validate.push({
            ele: $FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY,
            name: '冻结卡继续抓取流水期限',
            type: 'positiveInt',
            minEQ: 1,
            maxEQ: 7
        });
        validate.push({ele: $INCOME_LIMIT_MONITOR_TIMEOUT, name: '流水抓取超时时间', type: 'positiveInt', minEQ: 5, maxEQ: 20});
        validate.push({
            ele: $FINANCE_ACCOUNT_BALANCE_ALARM,
            name: '出入款账号余额告警值',
            type: 'amountCanZero',
            minEQ: 500,
            maxEQ: 10000
        });
        $.each($divFinance.find("input"), function (index, result) {
            keysArray.push($(result).attr("name"));
            valsArray.push($(result).val());
        });
    }
    if (updateSize == 5) {
        var $TRANSFER_TEST_ACCOUNT = $transferTestAccount_widget.find("[name=TRANSFER_TEST_ACCOUNT]");
        var $TRANSFER_TEST_BANKNAME = $transferTestAccount_widget.find("[name=TRANSFER_TEST_BANKNAME]");
        var $TRANSFER_TEST_OWNER = $transferTestAccount_widget.find("[name=TRANSFER_TEST_OWNER]");
        var $TRANSFER_TEST_AMOUNT = $transferTestAccount_widget.find("[name=TRANSFER_TEST_AMOUNT]");
        validate.push({ele: $TRANSFER_TEST_ACCOUNT, name: '收款账号', maxLength: 25});
        validate.push({ele: $TRANSFER_TEST_BANKNAME, name: '开户行', maxLength: 50});
        validate.push({ele: $TRANSFER_TEST_OWNER, name: '收款人', minLength: 2, maxLength: 10});
        validate.push({ele: $TRANSFER_TEST_AMOUNT, name: '转账金额', min: 0, maxEQ: 10});
        $.each($transferTestAccount_widget.find("input"), function (index, result) {
            keysArray.push($(result).attr("name"));
            valsArray.push($(result).val());
        });
    }
    if (updateSize == 6) {
        //MAS日志设置
//		var checkedVal = $masLog_widget.find('input[name=masLogLevel]:checked').val();
//		if (checkedVal) {
//			$.ajax({
//				type:'POST',
//	        	url:'xxxxx',
//	        	data:{"level":checkedVal},
//	        	dataType:'json',
//	        	success:function (res) {
//	            	if(res&&res.status==1){
//	                	console.log("修改日志等级成功...")
//	            	}
//	         	}
//	    	});
//		}
    }
    //校验
    if (!validateEmptyBatch(validate) || !validateInput(validate)) {
        return;
    }
    $.ajax({
        type: "PUT",
        dataType: 'JSON',
        url: '/r/set/update',
        async: false,
        data: {
            "keysArray": keysArray.toString(),
            "valsArray": valsArray.toString()
        },
        success: function (jsonObject) {
            if (jsonObject && jsonObject.status == 1) {
                //异步刷新系统配置全局变量
                loadSysSetting();
                showMessageForSuccess("保存成功");
            } else {
                showMessageForFail("保存失败" + jsonObject.message);
            }
        }
    });
}
/**
 * 工具升级
 */
var doCommonUpLevel = function (e) {
    bootbox.confirm("确定有发布新版本 ?", function (result) {
        if (result) {
            clickRepeat($(e));
            $.ajax({
                dataType: 'json', async: false, type: "POST", url: '/r/set/tools/upgrade', success: function () {
                }
            });
        }
    });
};
/**
 * APP升级
 */
var showCabanaUpgrade=function(){
	var APPupgradeModal='	<div id="APPupgradeModal" class="modal fade">\
		<div class="modal-dialog modal-lg" style="width:450px;">\
			<div class="modal-content">\
				<div class="modal-header no-padding text-center">\
					<div class="table-header"><button class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>APP版本更新</span></div>\
				</div>\
				<div class="modal-body">\
				<span class="blue">版本号：</span>\
				<input type="number" name="version" class="input-sm" /><br/><br/>\
				<span class="blue">测试手机号：</span><span class="red bolder">（请输入11位手机号，多个手机号使用分号隔开）</span><br/>\
				<textarea style="margin: 0px; width: 420px; height: 60px;" name="APP_PRE_UPDATE_MOBILES" ></textarea>\
				</div>\
				<div class="col-sm-12 modal-footer">\
					<button class="btn btn-primary" type="button" onclick="doCabanaUpgrade();">确认</button>\
					<button class="btn btn-danger" type="button" data-dismiss="modal">取消</button>\
				</div>\
			</div>\
		</div>\
	</div>';
	$.ajax({
		type: "POST",
		dataType: 'JSON',
		url: '/r/account/getLastVersion',
		async: false,
		success: function (jsonObject) {
			if(jsonObject&&jsonObject.status==1){
				var $div = $(APPupgradeModal).appendTo($("body"));
				$div.modal("toggle");
				$div.find("[name=version]").val(jsonObject.data);
				$div.find("[name=APP_PRE_UPDATE_MOBILES]").val(sysSetting.APP_PRE_UPDATE_MOBILES);
				$div.on('hidden.bs.modal', function () {
					//关闭窗口清除model
					$div.remove();
				});
			}else{
	        	showMessageForFail("版本号获取失败："+jsonObject.message);
			}
		}
	});
}
var doCabanaUpgrade=function(){
	//有手机号是测试，无手机号是升级
	var $div=$("#APPupgradeModal");
	var $version=$div.find("[name=version]");
	var APP_PRE_UPDATE_MOBILES=$.trim($div.find("[name=APP_PRE_UPDATE_MOBILES]").val());
	if(!validateEmpty($version,"版本号")){
		return;
	}
	var tipStr=(APP_PRE_UPDATE_MOBILES?"测试":"升级");
	bootbox.confirm("确定对APP进行版本<span class='red bolder'>"+tipStr+"</span> ?", function (result) {
        if (result) {
    		var param={
    				version:$.trim($version.val())
    		}
			var doUp=true;
    		if(APP_PRE_UPDATE_MOBILES){
    			param.APP_PRE_UPDATE_MOBILES=APP_PRE_UPDATE_MOBILES;//参数增加手机号
    			//存储系统设置
    			$.ajax({
    		        type: "PUT",
    		        dataType: 'JSON',
    		        url: '/r/set/update',
    		        async: false,
    		        data: {
    		            "keysArray": ["APP_PRE_UPDATE_MOBILES"].toString(),
    		            "valsArray": [APP_PRE_UPDATE_MOBILES].toString()
    		        },
    		        success: function (jsonObject) {
    		            if (jsonObject && jsonObject.status == 1) {
    		                //异步刷新系统配置全局变量
    		                loadSysSetting();
    		                console.log("保存成功");
    		            } else {
    		                showMessageForFail("保存手机号到系统设置失败" + jsonObject.message);
    		                //不进行升级
    		                doUp=false;
    		            }
    		        }
    		    });
    		}
    		if(doUp){
    			//执行 升级/测试
                $.ajax({
                    dataType: 'JSON',
                    async: false, 
                    type: "POST", 
                    url: '/r/set/cabanaVersion', 
                    data:param,
                    success: function (jsonObject) {
    		            if (jsonObject && jsonObject.status == 1) {
    		            	showMessageForSuccess(tipStr+"推送成功");
    		    			$div.modal("toggle");
    		            } else {
    		                showMessageForFail(tipStr+"推送失败：" + jsonObject.message);
    		            }
                    }
                });
    		}
        }
    });
}

/**
 * APP打补丁
 */
var showModalPatch=function(){
	var APPpatchModal='	<div id="APPpatchModal" class="modal fade">\
		<div class="modal-dialog modal-lg" style="width:450px;">\
			<div class="modal-content">\
				<div class="modal-header no-padding text-center">\
					<div class="table-header"><button class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>APP版本更新</span></div>\
				</div>\
				<div class="modal-body">\
				<span class="blue">版本号：&nbsp;&nbsp;</span>\
				<input type="number" name="APP_PATCH_VERSION" class="input-sm" /><br/><br/>\
				<span class="blue">文件地址：</span><br/>\
				<textarea style="margin: 0px; width: 420px; height: 60px;" name="APP_PATCH_URL" ></textarea>\
				</div>\
				<div class="col-sm-12 modal-footer">\
					<button class="btn btn-primary" type="button" onclick="doCabanaPatch();">确认</button>\
					<button class="btn btn-danger" type="button" data-dismiss="modal">取消</button>\
				</div>\
			</div>\
		</div>\
	</div>';
	var $div = $(APPpatchModal).appendTo($("body"));
	$div.modal("toggle");
	$div.find("[name=APP_PATCH_VERSION]").val(sysSetting.APP_PATCH_VERSION);
	$div.find("[name=APP_PATCH_URL]").val(sysSetting.APP_PATCH_URL);
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model
		$div.remove();
	});
}
var doCabanaPatch=function(){
	//有手机号是测试，无手机号是升级
	var $div=$("#APPpatchModal");
	var $APP_PATCH_VERSION=$div.find("[name=APP_PATCH_VERSION]");
	var $APP_PATCH_URL=$div.find("[name=APP_PATCH_URL]");
	if(!validateEmpty($APP_PATCH_VERSION,"版本号")){
		return;
	}
	if(!validateEmpty($APP_PATCH_URL,"文件地址")){
		return;
	}
	bootbox.confirm("确定APP打补丁 ?", function (result) {
        if (result) {
			var doUp=true;
			//存储系统设置
			$.ajax({
		        type: "PUT",
		        dataType: 'JSON',    		        
		        url: '/r/set/update',
		        async: false,
		        data: {
		            "keysArray": ["APP_PATCH_VERSION","APP_PATCH_URL"].toString(),
		            "valsArray": [$APP_PATCH_VERSION.val().trim(),$APP_PATCH_URL.val().trim()].toString()
		        },
		        success: function (jsonObject) {
		            if (jsonObject && jsonObject.status == 1) {
		                //异步刷新系统配置全局变量
		                loadSysSetting();
		                console.log("保存成功");
		            } else {
		                showMessageForFail("保存版本号和文件地址到系统设置失败" + jsonObject.message);
		                //不进行升级
		                doUp=false;
		            }
		        }
		    });
    		if(doUp){
    			//执行 升级/测试
                $.ajax({
                    dataType: 'JSON',
                    async: false, 
                    type: "POST", 
                    url: '/r/set/app/patch', 
                    success: function (jsonObject) {
    		            if (jsonObject && jsonObject.status == 1) {
    		            	showMessageForSuccess("打补丁推送成功");
    		    			$div.modal("toggle");
    		            } else {
    		                showMessageForFail("打补丁推送失败：" + jsonObject.message);
    		            }
                    }
                });
    		}
        }
    });
}

/**已废弃*/
var doPatch = function(e) {
    bootbox.confirm("确定APP打补丁 ?", function (result) {
        if (result) {
            clickRepeat($(e));
            $.ajax({
                dataType: 'json', async: false, type: "POST", url: '/r/set/app/patch', success: function () {
                }
            });
        }
    });
}

/**
 * 初始化字体按钮
 */
$('#editor2').css({'height': '200px'}).ace_wysiwyg({
    toolbar_place: function (toolbar) {
        return $(this).closest('.widget-box')
            .find('.widget-header').prepend(toolbar)
            .find('.wysiwyg-toolbar').addClass('inline');
    },
    toolbar: [
        'bold',
        {name: 'italic', title: 'Change Title!', icon: 'ace-icon fa fa-leaf'},
        'strikethrough',
        null,
        'insertunorderedlist',
        'insertorderedlist',
        null,
        'justifyleft',
        'justifycenter',
        'justifyright'
    ],
    speech_button: false
});

/**
 * 发布消息到全局用户
 * @returns
 */
$("#publishMsgToAllUser").click(function () {
    clickRepeat($("#publishMsgToAllUser"));
    $.ajax({
        type: "POST",
        url: '/r/set/sendmsgtoall',
        data: {"message": $("#editor2").html()},
        dataType: 'JSON',
        success: function (res) {
            if (res && res.status == 1) {
                console.log("发布消息成功");
            }
        }
    })
});

$(document).ready(function () {
    reloadSetting();
    contentRight();
    initTimePickerSendTime();
});

/**
 *    初始化时间控件
 */
var initTimePickerSendTime = function () {
    var startTime = $('input.date-range-picker').daterangepicker({
        autoUpdateInput: false,
        singleDatePicker: true,
        timePicker: true, //显示时间
        timePicker24Hour: true, //24小时制
        timePickerSeconds: true,//显示秒
        startDate: start, //设置开始日期
        locale: {
            "format": 'HH:mm:ss',
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
    }).val(start);
    startTime.on('apply.daterangepicker', function (ev, picker) {
        $(this).val(picker.startDate.format('HH:mm:ss'));
    });
    startTime.on('cancel.daterangepicker', function (ev, picker) {
        $(this).val('');
    });
}

/** 保存系统定时同步平台数据 */
var controlSynchSwitch=function(type){
	var action;
	if(type==1){
		//入款
		action=$("[name=action_income]:checked").val();
	}else if(type==2){
		//出款
		action=$("[name=action_out]:checked").val();
		
	}else{
		return;
	}
	bootbox.confirm("是否保存?",function (res) {
        if (res){
        	$.ajax({
	       		 type:'get',
	       		 dataType:'json',
	       		 data:{'type':type,'action':action},
	       		 async:false,
	       		 url:'/r/synch/controlSynchSwitch',
	       		 success:function (jsonObject) {
	     			if(jsonObject.status ==1){
	     				showMessageForSuccess("保存成功");
	     				loadSynchSwitch();
	     			}else{
	     				showMessageForFail("保存失败："+jsonObject.message);
	     			}
	             }
            });
        }
    });
	 
}
/** 读取系统定时同步平台数据 */
var loadSynchSwitch=function(){
	 $.ajax({
		 type:'get',
		 dataType:'json',
		 data:{'action':3},
		 async:false,
		 url:'/r/synch/controlSynchSwitch',
		 success:function (jsonObject) {
			if(jsonObject.status ==1&&jsonObject.data){
				 $("[name=action_income],[name=action_out]").removeProp('checked');
				 $("[name=action_income][value="+jsonObject.data.income*1+"]").prop('checked','checked');
				 $("[name=action_out][value="+jsonObject.data.outreq*1+"]").prop('checked','checked');
  			}
        }
    });
}
