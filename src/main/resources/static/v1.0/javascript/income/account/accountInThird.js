currentPageLocation = window.location.href;
var lockedAccountIdsMap = null;//临时保存锁定账号的输入金额 手续费
$("#accountFilter [name=search_EQ_handicapId],[name=search_EQ_LevelId],[name=search_LIKE_account]").change(function () {
    showAccountList();
});
$("#accountFilter #searchBtn").click(function () {
    showAccountList();
});
var subTabInDrawPage=0;
var changEnCashTabInit_bind = function (bindType) {
    if (bindType == 1 || bindType == 0) {
        $('#chechbox_th').show();
        $('#doEnchashments').hide();$('#bindIssued1').show();$('#bindIssued0').show();
        bindType == 1 ? $('#lock_status').show() : $('#lock_status').hide();
        $('#amount_draw').hide();
        $('#fee_draw').hide();
        $('#action_draw').hide();
    } else {
        $('#doEnchashments').show();$('#bindIssued1').hide();$('#bindIssued0').hide();
        $('#lock_status').hide();
        $('#amount_draw').show();
        $('#fee_draw').show();
        $('#action_draw').show();$('#chechbox_th').hide();
        if (tempValueOnChange && tempValueOnChange.size>0){
            for (let k in tempValueOnChange){
                let val = tempValueOnChange.get(k);
                let vals = val.split(',');
                $('#amount'+k).val(vals[0]);
                $('#fee'+k).val(vals[1]);
            }
        }
    }
    $("#EnCashBindType").val(bindType);//已绑定  未绑定 已锁定 页签标识
    subTabInDrawPage=bindType;
    showEncashAccountList(0);
}
var changEnCashTabInit = function (encashType) {
    $("#ThirdEncashTabType").val(encashType);
    if (encashType == 1 || encashType == 2) {
        //提现到下发卡 现金卡
        showEncashAccountList(0);
    } else if (encashType == 4) {
        var nbsp = '&nbsp;&nbsp;&nbsp;&nbsp;';
        var $div = $("#Third_EnCash_Modal #tabAddBindBank");
        //加载银行品牌
        var options = "";
        options += "<option value='' >" + nbsp + "请选择" + nbsp + "</option>";
        $.each(bank_name_list, function (index, record) {
            options += "<option value=" + record + " >" + nbsp + record + nbsp + "</option>";
        });
        $div.find("select[name=choiceBankBrand]").html(options);
        //重置查询条件
        reset('tabAddBindBank');
    }
}
/**
 * 根据ID刷新当前账号的余额与差额
 */
var refreshBalance = function (incomeId) {
    $balance = $("#mainTr" + incomeId).find("[name=balance]");
    $bankBalance = $("#mainTr" + incomeId).find("[name=bankBalance]");
    $difference = $("#mainTr" + incomeId).find("[name=difference]");
    var record = getAccountInfoById(incomeId);
    if (record) {
        record.balance = record.balance ? record.balance : 0;
        record.bankBalance = record.bankBalance ? record.bankBalance : 0;
        $balance.text(record.balance ? record.balance : 0);
        $bankBalance.text(record.bankBalance ? record.bankBalance : 0);
        $difference.text((record.bankBalance ? record.bankBalance1 : 0) * 1 - (record.balance ? record.balance : 0) * 1);
    }
}

/**
 * 绑定/解绑
 * @param incomeAccountId
 * @param issueAccountId
 * @param binding0binded1
 * @returns
 */
function bindIssued(incomeAccountId, issueAccountId, binding0binded1, confirm) {
    if (!confirm) {
        bootbox.confirm(binding0binded1 == 1 ? "确定要绑定到当前账号 ?" : "确定要解绑当前账号 ?", function (result) {
            if (result) {
                doBindIssued(incomeAccountId, issueAccountId, binding0binded1);
            }
            setTimeout(function () {
                $('body').addClass('modal-open');
            }, 500);
        });
    } else {
        doBindIssued(incomeAccountId, issueAccountId, binding0binded1);
    }
}
/**
 * 绑定 解绑 issueAccountId是绑定或者解绑的卡  incomeAccountId是第三方入款卡
 */
var doBindIssued = function (incomeAccountId, issueAccountId, binding0binded1) {
    var issueAccountIdArr =  [];
    issueAccountIdArr.push(issueAccountId);
    doBindIssuedArr(incomeAccountId,issueAccountIdArr,binding0binded1);
}
var doBindIssuedArr = function (incomeAccountId, issueAccountIdArr, binding0binded1) {
    var data = {
        "incomeAccountId":incomeAccountId,
        "issueAccountId": issueAccountIdArr,
        "binding0binded1": binding0binded1
    };
    $.ajax({
        dataType: 'JSON',contentType: 'application/json;charset=UTF-8',
        type: "POST",
        async: false,
        url: API.r_account_bindOrUnbindForIssue,
        data: JSON.stringify(data),
        success: function (jsonObject) {
            if (jsonObject.status == 1) {
                //绑定，解绑成功 刷新数据
                showEncashAccountList();
                if (binding0binded1 == 1) {
                    //设置当前账号有绑定记录
                    $("#hasBindCard").val(1);
                }
            } else {
                if (jsonObject.status == -1) {
                    showMessageForFail("操作失败：" + jsonObject.message);
                }
            }
            setTimeout(function () {
                $('body').addClass('modal-open');
            }, 500);
        }
    });
}
/**
 * 锁定或解锁
 */
var doLockOrUnlockBind = function (fromId, toId, isLockOrUnlock, isRefresh) {
    $.ajax({
        type: "PUT",
        async: false,
        url: API.r_account_lockOrUnlock,
        data: {
            lock1OrUnlock0: isLockOrUnlock,
            fromId: fromId,
            toId: toId
        },
        dataType: 'json',
        success: function (jsonObject) {
            if (-1 == jsonObject.status) {
                showMessageForFail("操作失败：" + jsonObject.message);
                showEncashAccountList();
                return;
            }
            //操作成功刷新数据
            if (isRefresh == 1) {
                showEncashAccountList();
            }
            isLockOrUnlock == 0 ? deleteMapElement(toId.toString()) : null;
            if (isLockOrUnlock == 0 && subTabType == 2) {
                showEncashAccountList();
                if (tempValueOnChange && tempValueOnChange.size>0){
                    tempValueOnChange.delete(toId+'');
                    dealBalanceAmountOnchange(toId);
                }
            }
        }
    });
}
/**
 * 新增银行卡
 */
var addBindAccount = function () {
    bootbox.confirm("新增下发卡（第三方专用）将会默认绑定到当前账号，且不可解除，继续操作 ?", function (result) {
        if (result) {
            var $div = $("#Third_EnCash_Modal #tabAddBindBank");
            var $account = $div.find("input[name='account']");
            var $bankType = $div.find("select[name='choiceBankBrand']");
            var $bankName = $div.find("input[name='bankName']");
            var $balance = $div.find("input[name='balance']");
            var $owner = $div.find("input[name='owner']");
            var $limitIn = $div.find("input[name='limitIn']");
            var $limitOut = $div.find("input[name='limitOut']");
            var $limitBalance = $div.find("input[name='limitBalance']");
            var $alias = $div.find("input[name='alias']");
            var $currSysLevel = $div.find("select[name='currSysLevel']");
            //校验
            var validate = [
                {ele: $account, name: '账号'},
                {ele: $balance, name: '余额'},
                {ele: $owner, name: '开户人'},
                {ele: $bankType, name: '开户行 > 银行类别'},
                {ele: $bankName, name: '开户行 '}
            ];
            var validatePrint = [
                {ele: $account, name: '账号', maxLength: 25},
                {ele: $owner, name: '开户人', minLength: 2, maxLength: 10},
                {ele: $bankName, name: '开户行 > 支行', maxLength: 50},
                {ele: $balance, name: '余额', type: 'amountCanZero'}
            ];
            if (!validateEmptyBatch(validate)
                || !validateInput(validatePrint)) {
                return;
            }
            $.ajax({
                type: "PUT",
                dataType: 'JSON',
                url: '/r/account/create',
                async: false,
                data: {
                    "type": [accountTypeThirdCommon].toString(),
                    "status": accountStatusNormal,
                    "account": $.trim($account.val(), true),
                    "bankType": $.trim($bankType.val(),true),
                    "bankName": $.trim($bankName.val(),true),
                    "owner": $.trim($owner.val(),true),
                    "bankBalance": $.trim($balance.val()),
                    "limitIn": $.trim($limitOut.val()),
                    "limitOut": $.trim($limitOut.val()),
                    "limitBalance": $.trim($limitBalance.val()),
                    "alias": $.trim($alias.val(),true),
                    "currSysLevel": $.trim($currSysLevel.val())
                },
                success: function (jsonObject) {
                    if (jsonObject.status == 1) {
                        //绑定当前卡
                        if (jsonObject.data && jsonObject.data.id) {
                            //入款账号的ID，成功添加返回的银行卡ID  绑定
                            bindIssued($("#Third_EnCash_Modal #accountInfo_id").val(), jsonObject.data.id, 1, true);
                            //操作成功提示
                            showMessageForSuccess("新增成功,已绑定到当前账号");
                            //跳转到下发卡  > 已绑定界面
                            $("#Third_EnCash_Modal #ThirdEncashTab_one,#ThirdEncashTab_one a").click();
                            $("#Third_EnCash_Modal #EnCashBindType_one,#EnCashBindType_one a").click();
                            //重置
                            reset('tabAddBindBank');
                        }
                    } else {
                        showMessageForFail("新增失败," + jsonObject.message);
                    }
                }
            });
        }
    });
}

//锁定时记录id
var accountIds = new Array()
function rememberOrRemoveIds(id, isLockOrUnlock) {
    if (isLockOrUnlock == 1) {
        if (accountIds.indexOf(id) < 0) {
            accountIds.push(id);
        }
    } else {
        if (accountIds.indexOf(id) >= 0) {
            accountIds.splice(accountIds.indexOf(id), 1);
        }
    }
}


/**
 * 批量提现 实现
 */
var doEnchashments = function () {
    var $div = $("#Third_EnCash_Modal");
    var fromAccountId = $div.find("#accountInfo_id").val();
    var askingMessage = "确定要批量提现吗 ?";
    if (accountIds.length <= 0) {
        showMessageForFail("请填写提现数据！");
        return;
    }
    bootbox.confirm(askingMessage, function (result) {
        if (result) {
            var tmp = new Date().getTime();
            var amountToArray = [];
            var toAccountToArray = [];
            var feeToArray = [];
            var toIdArray = [];
            var orderNoArray = [];
            for (var i = 0; i < accountIds.length; i++) {
                var $amount = $div.find("#amount" + accountIds[i]);
                var $fee = $div.find("#fee" + accountIds[i]);
                var $toAccount = $div.find("#toAccount" + accountIds[i]);
                if (!$.trim($amount.val())) {
                    continue;
                }

				// //非空校验&输入正确性校验
				// if(!validateEmptyBatch([{ele:$amount,name:'金额'} ])||
				// 		!validateInput([{ele:$amount,name:'金额',type:'amountPlus'},{ele:$fee,name:'手续费',type:'amountCanZero'} ])){
			    // 	return;
			    // }
			    if (parseFloat($amount.val())%1!=0){
                    showMessageForFail("金额不能有小数!");
                    return;
                }
                orderNoArray.push($.trim(tmp + i + ""));
                amountToArray.push($.trim($amount.val()) + "");
                feeToArray.push($.trim($fee.val()) + "");
                toAccountToArray.push($.trim($toAccount.text()) + "");
                toIdArray.push(accountIds[i] + "");
            }
            if(amountToArray.indexOf("0")!=-1){
            	showMessageForFail("金额有误！");
            	return;
            }
            var data = {
                    "type": incomeRequestTypeWithdrawThird,
                    "amountToArray": amountToArray,
                    "toAccountToArray": toAccountToArray,
                    "fromAccount": $div.find("#accountInfo_account").text(),
                    "operator": getCookie('JUSERID'),
                    "feeToArray": feeToArray,
                    "fromId": fromAccountId,
                    "toIdArray": toIdArray,
                    "orderNoArray": orderNoArray
                };
            $.ajax({
                type: "POST",
                async: true,
                url: '/r/income/saveThirdTrans',
                contentType : "application/json ; charset=utf-8",
                dataType: 'json',
                data: JSON.stringify(data),
                success: function (jsonObject) {
                    if (-1 == jsonObject.status) {
                        showMessageForFail("操作失败：" + jsonObject.message);
                        return;
                    }
                    showMessageForSuccess("提现成功");
                    if(lockedAccountIdsMap){
                        //直接批量提现或者提现 是不会有值的
                        lockedAccountIdsMap.clear();
                        lockedAccountIdsMap = null;
                        if (tempValueOnChange && tempValueOnChange.size>0){
                            for (let i in toIdArray){
                                tempValueOnChange.delete(toIdArray[i]+'');
                            }
                        }
                    }
                    showEncashAccountList();
                    //操作成功刷新数据 并自动解锁
                    // setTimeout(function () {
                    //     for (var i = 0; i < accountIds.length; i++) {
                    //         var $amount = $div.find("#amount" + accountIds[i]);
                    //         if (!$.trim($amount.val())) {
                    //             continue;
                    //         }
                    //         doLockOrUnlockBind(fromAccountId, accountIds[i], 0, 0);
                    //         rememberOrRemoveIds(accountIds[i], 0);
                    //     }
                    //     showEncashAccountList();
                    // }, 150);
                }
            });
        }
        setTimeout(function () {
            $('body').addClass('modal-open');
        }, 500);

    });
}
function _saveAllInputValues() {
    var amountArray = [];
    var feeArray = [];
    var recordIdArray = [];
    $('input[name="drawAmount"]').each(function () {
        amountArray.push(this.value);
        recordIdArray.push(this.id.split('amount')[1]);
    });
    $('input[name="drawFee"]').each(function () {
        feeArray.push($(this).value);

    });
    if (amountArray && amountArray.length > 0) {
        $.each(amountArray, function (i, val) {
            saveInputTemplate(val, recordIdArray[i], 1);
        });
        $.each(feeArray, function (i, val) {
            saveInputTemplate(val, recordIdArray[i], 2);
        });
    }
}
/**
 * 提现操作 下发卡和现金卡
 */
var doEnchashment = function (fromAccountId, toAccountId) {
    var $div = $("#Third_EnCash_Modal");
    var isOk = false, askingMessage = "确定要提现吗 ?";
    //为未绑定的下发卡 且有绑定记录时，确认提示是否继续操作
    if ($div.find("#ThirdEncashTabType").val() == 1 && $("#EnCashBindType").val() == 0 && $div.find("#hasBindCard").val() == 1) {
        askingMessage = "【第三方账号已有绑定下发卡记录，请确认是否为必须绑卡才可以使用的第三方账号，如果是，请绑定后再操作】，继续提现？";
    }
    var $amount = $div.find("#amount" + toAccountId);
    if (!$amount) {
        $.gritter.add({
            title: '消息提示',
            text: "请填写提现金额",
            // image: 'admin/clear/notif_icon.png',
            sticky: false,
            time: 5000,
            speed: 100,
            position: 'bottom-right',
            class_name: 'gritter-success'// gritter-center
        });
        return;
    }
    bootbox.confirm(askingMessage, function (result) {
        if (result) {
            var $fee = $div.find("#fee" + toAccountId);
            var $toAccount = $div.find("#toAccount" + toAccountId);
            //非空校验&输入正确性校验
            if (!validateEmptyBatch([{ele: $amount, name: '金额'}]) ||
                !validateInput([{ele: $amount, name: '金额', type: 'amountPlus'}, {
                    ele: $fee,
                    name: '手续费',
                    type: 'amountCanZero'
                }])) {
                return;
            }
            var tmp = new Date().getTime();
            var amountToArray = [];
            amountToArray.push($.trim($amount.val()));
            var toAccountToArray =[];
            toAccountToArray.push($.trim($toAccount.text()));
            var feeToArray = [];
            feeToArray.push($.trim($fee.val()));
            var toIdArray = [];
            toIdArray.push(toAccountId);
            var orderNoArray = [];
            orderNoArray.push(tmp.toString());
            _saveAllInputValues();
            var data =  {
                "type": incomeRequestTypeWithdrawThird,
                "amountToArray": amountToArray,
                "toAccountToArray": toAccountToArray,
                "fromAccount": $div.find("#accountInfo_account").text(),
                "operator": getCookie('JUSERID'),
                "feeToArray": feeToArray,
                "fromId": fromAccountId,
                "toIdArray": toIdArray,
                "orderNoArray": orderNoArray
            };
            $.ajax({
                type: "post",
                async: true,
                url: '/r/income/saveThirdTrans',
                contentType : "application/json ; charset=utf-8",
                dataType: 'json',
                data:JSON.stringify(data),
                success: function (jsonObject) {
                    if (-1 == jsonObject.status) {
                        showMessageForFail("操作失败：" + jsonObject.message);
                        return;
                    }
                    showMessageForSuccess("提现成功");
                    if (subTabType == 2) {
                        deleteMapElement(toAccountId);
                        showEncashAccountList();
                    }
                    //操作成功刷新数据 并自动解锁
                    // setTimeout(function () {
                    //     doLockOrUnlockBind(fromAccountId, toAccountId, 0, 1);
                    //     rememberOrRemoveIds(toAccountId, 0);
                    // }, 150);
                }
            });
        }
        setTimeout(function () {
            $('body').addClass('modal-open');
        }, 1500);
        if (tempValueOnChange && tempValueOnChange.size>0){
            for (let i in toIdArray){
                tempValueOnChange.delete(toIdArray[i]+'');
            }
        }
    });

}


/**
 * 系统余额明细弹出框
 */
var showThirdDetailModal = function (formAccountId) {
    closeThirdModal();
    $.ajax({
        type: "GET",
        async: false,
        dataType: 'html',
        url: "/" + sysVersoin + "/html/income/account/accountInThirdDetail.html",
        success: function (html) {
            $div = $(html).find("#Third_Detail_Modal").clone().appendTo($("body"));
            $div.find("#accountInfo_id").val(formAccountId);
            showThirdIssuedDetailList(0);
            $div.modal("toggle");
        }
    });
}
//socket 用于接收锁定消息,有锁定和解锁的要刷新页面,防止同时锁定
var ws_thirdInAccountDraw = null;
// function create_ws_thirdInAccountDraw() {
//     if (window.location.protocol == 'http:') {
//         ws_thirdInAccountDraw = new WebSocket('ws://' + window.location.host + '/ws/thirdInAccountDraw');
//     } else {
//         ws_thirdInAccountDraw = new WebSocket('wss://' + window.location.host + '/ws/thirdInAccountDraw');
//     }
//     ws_thirdInAccountDraw.onmessage = function (msg) {
//         console.log("msg--:" + msg.data);
//         if (msg && msg.data && msg.data == 'FRESH_PAGE_THIRD_ACCOUNT') {
//             if (subTabType == 1) {
//                 showEncashAccountList();//刷新
//             }
//         }
//     }
//     ws_thirdInAccountDraw.onclose = function (e) {
//         ws_thirdInAccountDraw.close();
//         ws_thirdInAccountDraw = null;
//     }
// }
window.onbeforeunload(function () {
    if (ws_thirdInAccountDraw) {
        ws_thirdInAccountDraw.close();
        ws_thirdInAccountDraw = null;
    }
});
/**
 * 提现窗口弹出
 */
var showEncashThirdModal = function (formAccountId) {
    closeModal(1);
    //create_ws_thirdInAccountDraw();
    var accountInfo = getAccountInfoById(formAccountId);
    $.ajax({
        type: "GET",
        async: false,
        dataType: 'html',
        url: "/" + sysVersoin + "/html/income/account/accountInThird_Encash.html",
        success: function (html) {
            $div = $(html).find("#Third_EnCash_Modal").clone().appendTo($("body"));
            $div.find("#accountInfo_id").val(formAccountId);
            getBankTyp_select($div.find("[name='search_LIKE_bankType']"), null, "全部")
            if (accountInfo) {
                $div.find("#accountInfo_bankType").text(accountInfo.bankType);
                $div.find("#accountInfo_bankName").text(accountInfo.bankName);
                $div.find("#accountInfo_account").text(accountInfo.account);
            }
            $('#lock_status').show();
            showEncashAccountList(0);
            $div.modal("toggle");
            $div.on('hidden.bs.modal', function () {
                //刷新提现记录
                loadInOutTransfer([formAccountId], "detail", null, 'IncomeAccountThird:EncashLog:*');
                //刷新余额  差额
                refreshBalance(formAccountId);
                //关闭窗口清除model
//				$div.remove();
            });
        }
    });
}
/**
 * 提现操作 客户卡
 */
var doEnchashmentToCustomer = function (lineId) {
    var $div = $("#Third_EnCash_Modal #tabEncash_CustomThird table");
    var $toAccount = $div.find("td [name='toAccount" + lineId + "']");
    var $remark = $div.find("td [name='remark" + lineId + "']");
    var $toBankName = $div.find("td [name='toAccountBankName" + lineId + "']");
    var $toOwner = $div.find("td [name='toAccountOwner" + lineId + "']");
    var $amount = $div.find("td input[name='amount" + lineId + "']");
    var $fee = $div.find("td input[name='fee" + lineId + "']");
    var tmp = new Date().getTime();
    $amount.val($amount.val().replace(/\s/g,""));
    $toBankName.val($toBankName.val().replace(/\s/g,""));
    $toOwner.val($toOwner.val().replace(/\s/g,""));
    if($fee.val()>=100){
    	showMessageForFail("手续费不能超过100");
    	return;
    }
    //校验
    var validate = [
        {ele: $toAccount, name: '汇入账号'},
        {ele: $toBankName, name: '开户行 '},
        {ele: $toOwner, name: '开户人'}
    ];
    var validatePrint = [
        {ele: $toAccount, name: '汇入账号', maxLength: 30},
        {ele: $toBankName, name: '开户行', maxLength: 50},
        {ele: $toOwner, name: '开户人', minLength: 2, maxLength: 10}
    ];
    if (!validateEmptyBatch(validate) || !validateInput(validatePrint)) {
        return;
    }
    var data = {
        handicap: '0',
        type: incomeRequestTypeWithdrawThirdToCustomer,
        amount: $amount.val().replace(/\s/g,""),
        remark: $.trim($remark.val()),
        orderNo: tmp,
        usercode: 0,
        createTime: tmp,
        toAccount: $toAccount.val().replace(/\s/g,""),
        username: $toOwner.val().replace(/\s/g,""),
        token: '',
        realname: $toOwner.val().replace(/\s/g,""),
        level: '0',
        ackTime: '',
        fromAccount: $("#Third_EnCash_Modal #accountInfo_account").text(),
        operator: getCookie('JUSERID'),
        fee: $fee.val().replace(/\s/g,""),
        fromId: $("#Third_EnCash_Modal #accountInfo_id").val(),
        toAccountBank: $toBankName.val().replace(/\s/g,"")
    };
    bootbox.confirm("是否提现？", function (res) {
        if (res) {
            $.ajax({
                dataType: 'JSON', contentType: "application/json",
                type: "PUT",
                async: false,
                url: '/r/outtask/saveThirdCashOut',
                data: JSON.stringify(data),
                success: function (jsonObject) {
                    if (-1 == jsonObject.status) {
                        showMessageForFail("操作失败：" + jsonObject.message);
                        return;
                    } else {
                        showMessageForSuccess("提现成功：" + jsonObject.message);
                        $div.find("#tr" + lineId).find("input,select,button").attr("disabled", "disabled");
                    }
                }
            });
        }
    });
}
/**
 * 客户卡校验
 */
var validateSearch = function () {
    var $div = $("#tabEncash_CustomThird");
    var $amountTotal = $div.find("[name=amountTotal]");
    var $amount = $div.find("[name=amount]");
    //校验开始
    var validateList = [
        {ele: $amountTotal, name: '提现金额', type: 'amountPlus'},
        {ele: $amount, name: '单笔金额', type: 'amountPlus'}
    ];
    //非空校验&输入校验
    if (!validateEmptyBatch(validateList) || !validateInput(validateList)) {
        return false;
    }
    var amountTotal = $.trim($amountTotal.val());
    var amount = $.trim($amount.val());
    if (amountTotal * 1 < amount * 1) {
        showMessageForCheck("【提现金额】不可以小于【单笔金额】！", $amount);
        return false;
    }
    //自动拼接金额行数等
    var lastLineAmount = amountTotal % amount;
    var count = Math.floor(amountTotal / amount);
    if (lastLineAmount != 0) {
        count++;
    }
    if (count > 20) {
        showMessageForCheck("出款笔数不可超过20！", $amount);
        return false;
    }
    return {
        "lastLineAmount": lastLineAmount,
        "count": count,
        "amount": amount
    };
}
/**
 * 客户卡
 */
var showTempCardList = function () {
    var $div = $("#tabEncash_CustomThird");
//	//校验
//	var amountJson=validateSearch();
//	var lastLineAmount,count,amount;
//	if(amountJson){
//		lastLineAmount=amountJson.lastLineAmount;
//		count=amountJson.count;
//		amount=amountJson.amount;
//	}else{
//		return;
//	}
    var tbody = "";
    for (var i = 1; i <= 100; i++) {
        if (checkedTr(i)) {
            var tr = "";
            tr += "<td><input type='text' onkeyup='clearNoNum(this)' class='input-sm' id='toAccount" + i + "'  name='toAccount" + i + "'/></td>";
            tr += "<td>";
            tr += "<input type='text' class='input-sm' name='toAccountBankName" + i + "'/>";
            tr += "</td>";
            tr += "<td><input type='text' class='input-sm' name='toAccountOwner" + i + "'/></td>";
            tr += "<td><input type='text' onkeyup='clearNoNum(this)' class='input-sm' name='amount" + i + "'/></td>";
            tr += "<td><input type='text' onkeyup='clearNoNum(this)' class='input-sm' name='fee" + i + "'/></td>";
            tr += "<td><input type='text' class='input-sm' name='remark" + i + "'/></td>";
            //操作
            tr += "<td>";
            //提现
            tr += "<button onclick=doEnchashmentToCustomer(" + i + ") type='button' class='btn btn-xs btn-white btn-success btn-bold' >"
                + "<i class='ace-icon fa fa-credit-card bigger-100 green'></i><span>提现</span></button>";
            tr += "</td>";
            tbody += "<tr id='tr" + i + "'>" + tr + "</tr>";
            break;
        }
    }
    $div.find("tbody").append(tbody);
}
var checkedTr = function (row) {
    if ($("#toAccount" + row).length == 1) {
        return false;
    } else {
        return true;
    }
}


/**
 * 下发记录
 */
var showThirdIssuedDetailList = function (CurPage) {
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("input[name='startAndEndTimeThird']").val();
    if (!startAndEndTime) {
        initTimePicker(true, $("[name=startAndEndTimeThird]"), typeCustomLatestToday);
        startAndEndTime = $("input[name='startAndEndTimeThird']").val();
    }
    var startAndEndTimeToArray = new Array();
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    startAndEndTimeToArray = startAndEndTimeToArray.toString();
    var $div = $("#Third_Detail_Modal"), data = {};
    if (!!!CurPage && CurPage != 0) CurPage = $("#Issued_Third_Table_Page .Current_Page").text();
    var $tbody = $div.find("#Issued_Third_Table tbody");
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/r/account/findIssuedThird",
        data: {
            "fromId": $("#Third_Detail_Modal #accountInfo_id").val(),
            "startAndEndTimeToArray": startAndEndTimeToArray,
            "pageSize": $.session.get('initPageSize'),
            "pageNo": CurPage <= 0 ? 0 : CurPage - 1
        },
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0) {
                var tr = '';
                var fromIdList = new Array();
                var toIdList = new Array();
                //小计
                var counts = 0;
                for (var index in jsonObject.data.arrlist) {
                    var val = jsonObject.data.arrlist[index];
                    fromIdList.push({'id': val.fromId});
                    toIdList.push({'id': val.toId});
                    tr += '<tr>'
                        + '<td><a class="bind_hover_card breakByWord" data-toggle="accountInfoHover' + val.toId + '" data-placement="auto right" data-trigger="hover"  >' + hideAccountAll(val.toAccount)
                        + '</a></td>'
                        + '<td>' + val.orderNo + '</td>'
                        + '<td>' + val.amount + '</td>'
                        + '<td>' + val.fee + '</td>'
                        + '<td>' + (val.status == 0 ? "匹配中" : (val.status == 1 ? "已匹配" : "已取消")) + '</td>'
                        + '<td>' + val.createStr + '</td>'
                        + '</tr>';
                    counts += 1;
                }
                ;
                $('#Issued_Third_Table_Page_tbody').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="4">小计：' + counts + '</td>'
                    + '<td colspan="3"></td>'
                    + '</tr>';
                $('#Issued_Third_Table_Page_tbody').append(trs);
                var trn = '<tr>'
                    + '<td colspan="4">总计：' + jsonObject.data.page.totalElements + '</td>'
                    + '<td colspan="3"></td>'
                    + '</tr>';
                $('#Issued_Third_Table_Page_tbody').append(trn);
            } else {
                $('#Issued_Third_Table_Page_tbody').empty().html('<tr></tr>');
            }
            //加载账号悬浮提示
            loadHover_accountInfoHover(fromIdList);
            //加载账号悬浮提示
            loadHover_accountInfoHover(toIdList);
            //分页初始化
            showPading(jsonObject.data.page, "Issued_Third_Table_Page", showThirdIssuedDetailList, null, null, null, 0);
        }
    });
}

/**
 * 出现金记录
 */
var showThirdEncashDetailList = function (CurPage) {
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("input[name='startAndEndTimeEncash']").val();
    if (!startAndEndTime) {
        initTimePicker(true, $("[name=startAndEndTimeEncash]"), typeCustomLatestToday);
        startAndEndTime = $("input[name='startAndEndTimeEncash']").val();
    }
    var startAndEndTimeToArray = new Array();
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    startAndEndTimeToArray = startAndEndTimeToArray.toString();
    var $div = $("#Third_Detail_Modal"), data = {};
    if (!!!CurPage && CurPage != 0) CurPage = $("#Encash_Third_Table_Page .Current_Page").text();
    var $tbody = $div.find("#Encash_Third_Table tbody");
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/r/account/findEncashThird",
        data: {
            "fromId": $("#Third_Detail_Modal #accountInfo_id").val(),
            "startAndEndTimeToArray": startAndEndTimeToArray,
            "pageSize": $.session.get('initPageSize'),
            "pageNo": CurPage <= 0 ? 0 : CurPage - 1
        },
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0) {
                var tr = '';
                var fromIdList = new Array();
                //小计
                var counts = 0;
                for (var index in jsonObject.data.arrlist) {
                    var val = jsonObject.data.arrlist[index];
                    fromIdList.push({'id': val.fromid});
                    tr += '<tr>'
                        + '<td>' + val.toAccount + '</td>'
                        + '<td>' + val.toAccountOwner + '</td>'
                        + '<td>' + val.toAccountBank + '</td>'
                        + '<td>' + val.amount + '</td>'
                        + '<td>' + val.fee + '</td>'
                        + '<td>' + val.remark + '</td>'
                        + '<td>' + val.createTime + '</td>'
                        + '<td>' + val.operator + '</td>'
                        + '</tr>';
                    counts += 1;
                }
                ;
                $('#Encash_Third_Table_Page_tbody').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="4">小计：' + counts + '</td>'
                    + '<td colspan="5"></td>'
                    + '</tr>';
                $('#Encash_Third_Table_Page_tbody').append(trs);
                var trn = '<tr>'
                    + '<td colspan="4">总计：' + jsonObject.data.page.totalElements + '</td>'
                    + '<td colspan="5"></td>'
                    + '</tr>';
                $('#Encash_Third_Table_Page_tbody').append(trn);
            } else {
                $('#Encash_Third_Table_Page_tbody').empty().html('<tr></tr>');
            }
            //加载账号悬浮提示
            loadHover_accountInfoHover(fromIdList);
            //分页初始化
            showPading(jsonObject.data.page, "Encash_Third_Table_Page", showThirdEncashDetailList, null, null, null, 0);
        }
    });
}

/**
 * 给会员出款记录
 */
var showThirdMembersDetailList = function (CurPage) {
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("input[name='startAndEndTimeMembers']").val();
    var handicap = $("#search_account_handicapId").val();
    if (!startAndEndTime) {
        initTimePicker(true, $("[name=startAndEndTimeMembers]"), typeCustomLatestToday);
        startAndEndTime = $("input[name='startAndEndTimeMembers']").val();
    }
    if (!handicap) {
        inithancipad("search_account_handicapId");
        handicap = $("#search_account_handicapId").val();
    }
    var startAndEndTimeToArray = new Array();
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    startAndEndTimeToArray = startAndEndTimeToArray.toString();
    var $div = $("#Third_Detail_Modal"), data = {};
    if (!!!CurPage && CurPage != 0) CurPage = $("#Members_Third_Table_Page .Current_Page").text();
    var $tbody = $div.find("#Members_Third_Table tbody");
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/r/account/findMembersThird",
        data: {
            "fromId": $("#Third_Detail_Modal #accountInfo_id").val(),
            "startAndEndTimeToArray": startAndEndTimeToArray,
            "handicapCode": handicap,
            "pageSize": $.session.get('initPageSize'),
            "pageNo": CurPage <= 0 ? 0 : CurPage - 1
        },
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0) {
                var tr = '';
                var fromIdList = new Array();
                //小计
                var counts = 0;
                for (var index in jsonObject.data.arrlist) {
                    var val = jsonObject.data.arrlist[index];
                    fromIdList.push({'id': val.fromid});
                    tr += '<tr>'
                        + '<td>' + val.handicap + '</td>'
                        + '<td>' + val.level + '</td>'
                        + '<td>' + val.toAccount + '</td>'
                        + '<td>' + val.toAccountOwner + '</td>'
                        + '<td>' + val.toAccountBank + '</td>'
                        + '<td>' + val.orderNo + '</td>'
                        + '<td>' + val.member + '</td>'
                        + '<td>' + val.amount + '</td>'
                        + '<td>' + val.fee + '</td>'
                        + '<td>' + val.remark + '</td>'
                        + '<td>' + val.createTime + '</td>'
                        + '<td>' + val.operator + '</td>'
                        + '</tr>';
                    counts += 1;
                }
                ;
                $('#Members_Third_Table_Page_tbody').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="4">小计：' + counts + '</td>'
                    + '<td colspan="9"></td>'
                    + '</tr>';
                $('#Members_Third_Table_Page_tbody').append(trs);
                var trn = '<tr>'
                    + '<td colspan="4">总计：' + jsonObject.data.page.totalElements + '</td>'
                    + '<td colspan="9"></td>'
                    + '</tr>';
                $('#Members_Third_Table_Page_tbody').append(trn);
            } else {
                $('#Members_Third_Table_Page_tbody').empty().html('<tr></tr>');
            }
            //加载账号悬浮提示
            loadHover_accountInfoHover(fromIdList);
            //分页初始化
            showPading(jsonObject.data.page, "Members_Third_Table_Page", showThirdMembersDetailList, null, null, null, 0);
        }
    });
}

var inithancipad = function (name) {
    $.ajax({
        type: 'get',
        url: '/r/out/handicap',
        data: {},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var opt = '<option value="null">全部</option>';
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + $.trim(val.code) + '">' + val.name + '</option>';
                    });
                    $('#' + name).empty().html(opt);
                    $('#' + name).trigger('chosen:updated');
                }
            }
        }
    });
}
var chechbox_th_click = function (obj) {
    $(obj).find('input').click();
}
var chechbox_th_input_click = function (obj) {
    var th_input_selected=false;
    if (obj.checked){
        th_input_selected=true;
    } else{
       th_input_selected=false;
    }
    var $td_input=$('#Third_EnCash_Modal  #Encash_Third_Table').find('input[name="bindOrUnbindInput"]');
    $td_input.each(function (i) {
        if (th_input_selected){
            if ($($td_input[i]).attr("click_flag")=='1'){
                $td_input[i].checked=true;
                $($td_input[i]).attr("click_flag",'2');
            }
        } else{
            if ($($td_input[i]).attr("click_flag")=='2'){
                $($td_input[i]).attr("click_flag",'1');
                $td_input[i].checked=false;
            }
        }
    }) ;
};
var bindOrUnbindInputClick = function (obj) {
    var click_flag=$(obj).attr('click_flag');
    if (click_flag==1){
        $(obj).attr('click_flag','2');
        $(obj).checked=true;
        //如果 th下的所有td的input都选中 那么全部 就选中
        var allChecked = true;
        var $td_input=$('#Encash_Third_Table input[name="bindOrUnbindInput"]');
        $td_input.each(function (i) {
            if (!$td_input[i].checked){
                allChecked=false;
            }
        });
        if (allChecked){
            var $th_input = $('#Encash_Third_Table  #chechbox_th').find('input');
            if (!$th_input[0].checked){
                $th_input[0].checked=true;
            }
        }
    } else{
        $(obj).attr("click_flag",'1');
        $(obj).checked=false;
        $(obj).defaultChecked=false;
        //如果 tr中某一个取消选中则全选的复选框要取消
        var $th_input = $('#Encash_Third_Table  #chechbox_th').find('input');
        if ($th_input[0].checked){
            $th_input[0].checked=false;
        }
    }
}
var bindIssuedSelect = function (type) {
    var toBindIds = [];
    var thirdId =$("#Third_EnCash_Modal #accountInfo_id").val();
    var actionFlag = true;
    $('#Third_EnCash_Modal  #Encash_Third_Table').find('input[name="bindOrUnbindInput"]:checked').each(function () {
        if(type==0){
            //解绑
            if ('1'!=$(this).attr('bind_flag')||$(this).attr('bind_flag')=='null'){
                showMessageForFail("必须全部选择已绑定的才能一键解绑");actionFlag = false;return;
            }else{
                toBindIds.push($(this).val());
            }
        }else if(type==1){
            //绑定
            if ($(this).attr('bind_flag')!='null' || '1'==$(this).attr('bind_flag')){
                showMessageForFail("必须全部选择未绑定的才能一键绑定");actionFlag = false;return;
            }else{
                toBindIds.push($(this).val());
            }
        }
    });
    if (type==1){
        if (!thirdId || toBindIds.length==0){
            showMessageForFail("没有能绑定的记录或者请选择需要绑定的记录");
            actionFlag=false;
        }
    } else{
        if (!thirdId || toBindIds.length==0){
            showMessageForFail("没有能解绑的记录或者请选择需要解绑的记录"); actionFlag=false;
        }
    }

    if (!actionFlag){
        return;
    }
    var tip = type==1?"是否一键绑定所有勾选项?":"是否一键解绑所有勾选项?";

    bootbox.confirm(tip,function (res) {
        if (res){
            doBindIssuedArr(thirdId,toBindIds,type);
            $('#chechbox_th').find('input').prop('checked','');
        }
    });
}

var subTabType = null;
/**
 * 下发卡 现金卡
 */
var showEncashAccountList = function (CurPage) {
    var $div = $("#Third_EnCash_Modal"), data = {};
    if (!!!CurPage && CurPage != 0) CurPage = $("#Encash_Third_Table_Page .Current_Page").text();
    var statusArray = new Array(), typeArray = new Array();
    $div.find("input[name='search_IN_status']:checked").each(function () {
        statusArray.push(this.value);
    });
    var currSysLevelToArray = new Array();
    $div.find("input[name='currSysLevel']:checked").each(function () {
        currSysLevelToArray.push(this.value);
    });
    var isIssued = $div.find("#ThirdEncashTabType").val() == 1;//下发卡  现金卡 外层页签标识
    var thirdAccountIdToDraw = $("#Third_EnCash_Modal #accountInfo_id").val();
    var flag=$('input:radio[name="flag"]:checked').val();
    if (isIssued) {
        //提现到下发卡时
        $("#EnCashBindType_UL").show();
        $("#toRecord").show();
        data = {
            "fromId": thirdAccountIdToDraw,
            "queryType":$.trim($div.find("input[name='search_EQ_queryType']:checked").val()),
            "account": $.trim($div.find("input[name='search_LIKE_account']").val()),
            "alias":$.trim($div.find("input[name='search_LIKE_alias']").val()),
            "bankType": $.trim($div.find("[name='search_LIKE_bankType']").val()),
            "owner": $.trim($div.find("input[name='search_LIKE_owner']").val()),
            "incomeAccountId": $div.find("#accountInfo_id").val(),
            "statusOfIssueToArray": statusArray.toString(),
            "typeOfIssueToArray": [accountTypeThirdCommon, accountTypeBindCommon].toString(),
            "binding0binded1": $("#EnCashBindType").val(),
            "currSysLevelToArray": currSysLevelToArray.toString(),
            "flag":flag,
            "pageSize": $.session.get('initPageSize'),
            "pageNo": CurPage <= 0 ? 0 : CurPage - 1
        };
    } else {
        $("#EnCashBindType_UL").hide();
        $("#toRecord").hide();
        data = {
            "fromId": $("#Third_EnCash_Modal #accountInfo_id").val(),
            "account": $.trim($div.find("input[name='search_LIKE_account']").val()),
            "bankType": $.trim($div.find("[name='search_LIKE_bankType']").val()),
            "owner": $.trim($div.find("input[name='search_LIKE_owner']").val()),
            "incomeAccountId": $div.find("#accountInfo_id").val(),
            "statusOfIssueToArray": statusArray.toString(),
            "typeOfIssueToArray": [accountTypeCashBank].toString(),
            "binding0binded1": 2,//不做是否绑定的查询
            "currSysLevelToArray": currSysLevelToArray.toString(),
            "flag":flag,
            "pageSize": $.session.get('initPageSize'),
            "pageNo": CurPage <= 0 ? 0 : CurPage - 1
        };
    }
    var $tbody = $div.find("#Encash_Third_Table tbody");
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/r/account/findbindissue",
        data: data,
        success: function (jsonObject) {
            if (-1 == jsonObject.status) {
                showMessageForFail("查询失败：" + jsonObject.message);
                return;
            }
            var tbodyStr = "", idList = new Array(),idListStatus = new Array();
            //第三方账号是否有绑定过卡，如果有，有可能是必须绑卡才可以使用的第三方，需要做提示
            if ($div.find("#EnCashBindType").val() == 1 && jsonObject.data && jsonObject.data.length > 0) {
                //每次弹窗查看未绑定卡时，一定会先查绑定卡
                $div.find("#hasBindCard").val(1);
            }
            //绑定 未绑定  已锁定 分开
            //绑定才可以提现  只看到自己锁定的账号  和 未被锁定的账号  别人锁定的账号看不见
            subTabType = $div.find("#EnCashBindType").val();
            $.each(jsonObject.data, function (index, record) {
                var currSysLevel = "";
                if (record.currSysLevel == currentSystemLevelInner)
                    currSysLevel = "内层";
                else if (record.currSysLevel ==currentSystemLevelDesignated)
                    currSysLevel = "指定层";
                else
                    currSysLevel = "外层";
                var tr = "";
                idList.push({'id': record.id});
                idListStatus.push(record.id);
                if ($("#EnCashBindType").val()==1){
                    if(record.bindId!=5){
                        tr += "<td><input type='checkbox' onclick='window.event.stopPropagation();bindOrUnbindInputClick(this);' name='bindOrUnbindInput' click_flag='1' value='"+record.id+"' bind_flag='"+record.bindId+"' id='bindOrUnbind_"+record.id+"'/></td>";
                    }else{
                        tr +="<td></td>";
                    }
                }
                tr +="<td>" +(record.alias?record.alias:'')+"</td>";
                tr += "<td>" +
                    "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + record.id +
                    	"' data-placement='auto right' data-trigger='hover' id='accountNo" + record.id + "' >" +
                    	hideAccountAll(record.account) +
                    "</a>" +
                    "<i class='fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-text='" + record.account + "'></i>" +
                    "</td>";
                tr += "<td><span>" + currSysLevel + "</span></td>";
                tr += "<td><span style='display:none' id='toAccount" + record.id + "'>" + record.account + "</span><span id='bankType" + record.id + "' >" + (record.bankType ? record.bankType : "") + "</span><i class='fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target='#bankType" + record.id + "'></i></td>";
                tr += "<td><span style='display:none' value=" + record.id + " name='ids'>" + record.id + "</span><span id='bankName" + record.id + "' >" + (record.bankName?record.bankName:'') + "</span><i class='fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target='#bankName" + record.id + "'></i></td>";
                tr += "<td><span id='owner" + record.id + "' >" + (record.owner?record.owner:'') + "</span><i class='fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target='#owner" + record.id + "'></i></td>";
                //不同状态使用不同颜色
                if (record.status == accountStatusFreeze || record.status == accountStatusStopTemp) {
                    tr += "<td><span class='label label-sm label-danger'>" + record.statusStr + "</span></td>";
                } else {
                    tr += "<td><span class='label label-sm label-success'>" + record.statusStr + "</span></td>";
                }
                tr += "<td><span>" + (record.typeStr?record.typeStr:'' )+ "</span></td>";
                tr += "<td><div class='BankLogEvent' target="+record.id+"><span class='amount'>"+ (record.bankBalance ? record.bankBalance : '0') + getlimitBalanceIconStr(record) +"</span><span class='time'></span></div></td>";
                //读取当前账号配置的当日最大收款额度，如果没有，则读取系统配置
                var alarmMaxIncomeDaily;
                if (record.limitIn) {
                    alarmMaxIncomeDaily = record.limitIn;
                } else if (sysSetting && sysSetting.INCOME_LIMIT_CHECKIN_TODAY) {
                    alarmMaxIncomeDaily = eval(sysSetting.INCOME_LIMIT_CHECKIN_TODAY);
                }
                //剩余可下发额度
                var lastAmount = alarmMaxIncomeDaily - record.incomeAmountDaily;
                var valueStr = " value='" + (record.transInt + record.transRadix) + "' ";
                //当日收款超过当日最大收款额时告警
                tr += "<td>"+ (record.limitOut?record.limitOut:"0") + "</td>";
                if (isIssued) {
                    tr += "<td>" +
                        "<a onclick='toMinimize();' href='#/EncashCheck4Transfer:*?toAccountId=" + record.id + "&toAccount=" + record.account + "&incomeReqStatus=0" + "'><span class='badge badge-warning' title='匹配中'>" + record.inCount.mapping + "</span></a>"
                        + "<a onclick='toMinimize();' href='#/EncashCheck4Transfer:*?toAccountId=" + record.id + "&toAccount=" + record.account + "&incomeReqStatus=1" + "'><span class='badge badge-success' title='已匹配'>" + record.inCount.mapped + "</span></a>"
                        + "<a onclick='toMinimize();' href='#/EncashCheck4Transfer:*?toAccountId=" + record.id + "&toAccount=" + record.account + "&incomeReqStatus=3" + "'><span class='badge badge-inverse' title='已驳回'>" + record.inCount.cancel + "</span></a></td>";
                }
                var lockedFlag1 = 999999 + parseInt(getCookie('JUSERID'))+parseInt(thirdAccountIdToDraw);
                var lockedFlag2 = 999999 + parseInt(getCookie('JUSERID'));
                if (subTabType == 1) {//已绑定
                    if (!record.bindId || record.bindId != 1 || record.bindId==5) {
                        if (record.bindId==5){
                            tr += "<td><button type='button' class='btn btn-xs btn-white btn-warning btn-bold green' " +
                                "onclick='rememberOrRemoveIds(" + record.id + ",1);doLockOrUnlockBind(" + $div.find("#accountInfo_id").val() + "," + record.id + ",1,1)'>" +
                                "<i class='ace-icon fa fa-lock bigger-100 orange'></i><span>锁定</span></button>";
                        } else{
                            //未绑定的
                            tr += "<td><button class='btn btn-xs btn-white btn-success btn-bold' "
                                + "onclick='bindIssued(" + $div.find("#accountInfo_id").val() + "," + record.id + ",1)'>"
                                + "<i class='ace-icon fa fa-lock bigger-100 green'></i><span>绑定</span></button></td>";
                        }

                    }else {
                        tr += "<td><button type='button' class='btn btn-xs btn-white btn-warning btn-bold green' " +
                            "onclick='rememberOrRemoveIds(" + record.id + ",1);doLockOrUnlockBind(" + $div.find("#accountInfo_id").val() + "," + record.id + ",1,1)'>" +
                            "<i class='ace-icon fa fa-lock bigger-100 orange'></i><span>锁定</span></button>";

                        tr += "<button class='btn btn-xs btn-white btn-danger btn-bold' red "
                            + "onclick='rememberOrRemoveIds(" + record.id + ",0);bindIssued(" + $div.find("#accountInfo_id").val() + "," + record.id + ",0);'>"
                            + "<i class='ace-icon fa fa-unlock bigger-90 red'></i><span>解绑</span></button></td>";

                    }

                }

                if (subTabType == 2) {
                    //当前人锁定 可以看到锁定的所有账号
                    if ((record.lockByOperator && (record.lockByOperator == lockedFlag1||record.lockByOperator == lockedFlag2))||(record.bindId && record.bindId==5)) {
                        //提现按钮
                        var amountToDraw = (parseInt((record.limitBalance * 1 - record.bankBalance * 1)) * -1 >= 0 ? parseInt((record.limitBalance * 1 - record.bankBalance * 1)) * -1 : setAmountForLockedAccount(record.id));
                        tr += "<td>" +
                            "<span class='input-icon'>" +
                            "<input  min='0' name='drawAmount' onchange='saveInputTemplate(this.value,\"" + record.id + "\",1);' style='width:70px;' value='" +amountToDraw  + "' class='input-sm' type='text' " + valueStr + " id='amount" + record.id + "' >" +
                            "<i class='ace-icon fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target=\"#amount" + record.id + "\" ></i>" +
                            "</span>" +
                            "</td>"; //金额 自动填充 银行余额-最低限额 (parseInt((record.limitBalance * 1 - record.bankBalance * 1)) * -1 >= 0 ? parseInt((record.limitBalance * 1 - record.bankBalance * 1)) * -1 : setAmountForLockedAccount(record.id))
                        tr += "<td>" +
                            "<span class='input-icon'>" +
                            "<input  min='0' name='drawFee' onchange='saveInputTemplate(this.value,\"" + record.id + "\",2);' style='width:70px;' value='" +setFeeForLockedAccount(record.id) + "' class='input-sm' type='text' id='fee" + record.id + "' >" +
                            "<i class='ace-icon fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target=\"#fee" + record.id + "\" ></i>" +
                            "</span>" +
                            "</td>";//手续费 setFeeForLockedAccount(record.id)
                        tr += "<td>" +
                            "<button class='btn btn-xs btn-white btn-danger btn-bold' " +
                            "onclick='doEnchashment(" + $div.find("#accountInfo_id").val() + "," + record.id + ")'>" +
                            "<i class='ace-icon fa fa-credit-card bigger-100 red'></i><span>提现</span></button>" +
                            "</td>";
                    } else {
                        tr += "<td colspan='3'><button disabled class='btn btn-xs btn-white btn-danger btn-bold' red >"
                            + "<i class='ace-icon fa fa-unlock bigger-90 red'></i><span>未绑定</span></button></td>";
                        "</td>";
                    }
                    //已锁定数据，显示解锁、并且把锁定的账号id记录下来
                    rememberOrRemoveIds(record.id, 1);
                    tr += "<td><button type='button' class='btn btn-xs btn-white btn-success btn-bold' " +
                        "onclick='rememberOrRemoveIds(" + record.id + ",0);doLockOrUnlockBind(" + $div.find("#accountInfo_id").val() + "," + record.id + ",0,1);'>" +
                        "<i class='ace-icon fa fa-unlock bigger-100 green'></i><span>解锁</span></button></td>";
                }
                tbodyStr += "<tr>" + tr + "</tr>";
                // if(subTabType==0 || subTabType==1){
                //     tbodyStr += "<tr>" + tr + "</tr>";
                // }else{
                //    if(tr){
                //        tbodyStr += "<tr>" + tr + "</tr>";
                //    }
                // }
            });
            $tbody.html(tbodyStr);
            showPading(jsonObject.page, "Encash_Third_Table_Page", showEncashAccountList, null, true);
            SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idListStatus);
            // if(subTabType==0 || subTabType==1){
            //     $tbody.html(tbodyStr);
            //     $('#Encash_Third_Table_Page').empty(); $('#Encash_Third_Table_Page').show();
            //     //分页初始化
            //     showPading(jsonObject.page, "Encash_Third_Table_Page", showEncashAccountList, null, true);
            // }else{
            //     if(tbodyStr){
            //         $tbody.attr('class',"");
            //         $tbody.html(tbodyStr);
            //     }else{
            //         tbodyStr = '<tr><td colspan="15"><h3>无锁定记录</h3></td></tr>';
            //         $tbody.html(tbodyStr);
            //         $tbody.attr('class',"alert alert-success center");
            //     }
            //     $('#Encash_Third_Table_Page').empty(); $('#Encash_Third_Table_Page').hide();
            // }
            //加载账号悬浮提示
            loadHover_accountInfoHover(idList);
            //小数点生成事件
            $('#Third_EnCash_Modal').bootstrapValidator({
                fields: {
                    amount: {
                        validators: {
                            callback: {
                                callback: function () {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }).on('success.field.bv', function (e) {
                var t = $(e.target);
                var radix = t.attr('radix');
                radix = isNaN(radix) ? 0.00 : parseFloat(radix);
                var val = t.val();
                val = isNaN(val) ? 0 : parseInt(val);
                t.val(val + radix);
            });
        }
    });
}
var tempValueOnChange = new Map();
function dealBalanceAmountOnchange(accountId) {
    if (!accountId){
        if (tempValueOnChange && tempValueOnChange.size>0){
            tempValueOnChange.delete(accountId+'');
        }
    }
    let amount = accountId?$('#Encash_Third_Table  #amount'+accountId).val():null;
    let fee =  accountId?$('#Encash_Third_Table  #fee'+accountId).val():null;
    let amountAll = amount && fee ?parseInt(amount)+parseInt(fee):null;
    let fromAccountId = $div.find("#accountInfo_id").val();
    if (accountId&& amountAll){
        tempValueOnChange.set(accountId,amount+','+fee);
    }
    let data ={
           "id":fromAccountId,"toId":accountId,"amount":amountAll
    }
    $.ajax({
        type:'post',contentType: 'application/json;charset=UTF-8',
        dataType: 'JSON',url:'/r/account/dealBalance',data:JSON.stringify(data),async:false,
        success:function (res) {
           console.log("加减系统余额结果:"+res);
       }
    });
}
function saveInputTemplate(val, accountId, type) {
    var amount = val, amountStr = '';
    if (!amount) {
        return;
    }
    if (!lockedAccountIdsMap) {
        lockedAccountIdsMap = new Map();
    }
    if (lockedAccountIdsMap.size > 0) {
        amountStr = lockedAccountIdsMap.get(accountId);
        if (amountStr) {
            var amountArray = amountStr.split(',');
            var amount1 = amountArray[0], fee = amountArray[1];
            if (type == 1) {
                amount1 = amount;//输入提现金额
            } else {
                fee = amount;//手续费
            }
            amountStr = amount1 + ',' + fee;
            lockedAccountIdsMap.set(accountId, amountStr);
        } else {
            saveInputValueFirstTime(amount, accountId, amountStr, type);
        }
    } else {
        saveInputValueFirstTime(amount, accountId, amountStr, type);
    }
    dealBalanceAmountOnchange(accountId);
}
function saveInputValueFirstTime(amount, accountId, amountStr, type) {
    if (amount) {
        if (type == 1) {
            //输入提现金额
            amountStr = amount + ',0';
        } else {
            //手续费
            amountStr = '0,' + amount;
        }
    }
    lockedAccountIdsMap.set(accountId, amountStr);
}
function setAmountForLockedAccount(accountId) {
    var amount = 0;
    if (lockedAccountIdsMap && lockedAccountIdsMap.size > 0) {
        lockedAccountIdsMap.forEach(function (val, key, mp) {
            if (key == accountId) {
                var amountArray = val.split(',');
                amount = amountArray[0];
            }
        });
    }
    return amount;
}
function setFeeForLockedAccount(accountId) {
    var fee = 0;
    if (lockedAccountIdsMap && lockedAccountIdsMap.size > 0) {
        lockedAccountIdsMap.forEach(function (val, key, mp) {
            if (key == accountId) {
                var amountArray = val.split(',');
                fee = amountArray[1];
            }
        });
    }
    return fee;
}
function deleteMapElement(toId) {
    if (lockedAccountIdsMap && lockedAccountIdsMap.size > 0) {
        lockedAccountIdsMap.delete(toId.toString());
        if (lockedAccountIdsMap.size == 0) {
            lockedAccountIdsMap = null;
        }
    }
}
/**
 * 根据账号Type拼接对应数据
 */
var showAccountList = function (CurPage) {
    //封装data
    var $div = $("#accountFilter");
    if (!!!CurPage && CurPage != 0) CurPage = $("#accountPage .Current_Page").text();
    var search_LIKE_account = $div.find("[name='search_LIKE_account']").val();
    var levelId = $div.find("select[name='search_EQ_LevelId']").val();
    var search_LIKE_auditor = $div.find("input[name='search_LIKE_auditor']").val();
    var search_LIKE_bankName = $div.find("input[name='search_LIKE_bankName']").val();
    var search_LIKE_owner = $div.find("input[name='search_LIKE_owner']").val();
    var statusToArray = [$("#tabStatus").val()];
    var fromIdArray = new Array(), tdName = 'detail';
    var data = {
        pageNo: CurPage <= 0 ? 0 : CurPage - 1,
        pageSize: $.session.get('initPageSize'),
        search_IN_handicapId: $div.find("select[name='search_EQ_handicapId']").val().toString(),
        search_LIKE_account: search_LIKE_account,
        levelId: levelId,
        auditor: search_LIKE_auditor,
        search_LIKE_bankName: $.trim(search_LIKE_bankName),
        search_LIKE_owner: $.trim(search_LIKE_owner),
        statusToArray: statusToArray.toString(),
        typeToArray: [accountTypeInThird].toString(),
        sortProperty: 'status',
        sortDirection: 0
    }
    //发送请求
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: API.r_account_list,
        data: data,
        success: function (jsonObject) {
            if (jsonObject.status != 1) {
                if (-1 == jsonObject.status) {
                    showMessageForFail("查询失败：" + jsonObject.message);
                }
                return;
            }
            $tbody = $("table#accountListTable").find("tbody");
            $tbody.html("");
            var totalBalanceByBank = 0, totalBalance = 0, idList = new Array;
            $.each(jsonObject.data, function (index, record) {
                fromIdArray.push(record.id);
                var tr = "";
                tr += "<td style='display:none;'><span>" + record.id + "</span></td>";
                tr += "<td><span>" + record.handicapName + "</span></td>";
                tr += "<td><span>" + record.levelNameToGroup + "</span></td>";
                tr += "<td>" +
                    "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + record.id + "' data-placement='auto right' data-trigger='hover'  >" + hideAccountAll(record.account) +
                    "</a>" +
                    "</td>";
                idList.push({'id': record.id, 'type': 'third'});
                //去除 类别显示
                tr += "<td><span title='第三方商户'>" + (record.bankName ? record.bankName : "无") + "</span></td>";//第三方
                //去除 经办人
                //tr+="<td><span>"+(record.owner?record.owner:"")+"</span></td>";//经办人
                //不同状态使用不同颜色
                if (record.status == accountStatusFreeze || record.status == accountStatusStopTemp) {
                    tr += "<td><span class='label label-sm label-danger'>" + record.statusStr + "</span></td>";
                } else {
                    tr += "<td><span class='label label-sm label-success'>" + record.statusStr + "</span></td>";
                }
                tr += "<td><a onclick='showThirdDetailModal(" + record.id + ")' class='bind_hover_card breakByWord' data-toggle='summary" + record.id + "' data-placement='auto left' data-trigger='hover'>"
                    + (record.balance == null ? 0 : record.balance)
                    + "</a></td>";
                record.bankBalance = record.bankBalance ? record.bankBalance : 0;
                tr += "<td id='bankBalance" + record.id + "'><span name='bankBalance' >" + record.bankBalance + "</span><i class='red ace-icon fa fa-pencil-square-o' onclick='changeInput(" + record.id + "," + record.bankBalance + ")' title='校正余额' style='cursor:pointer;'  ></i></td>";
                //提现明细
                tr += getRecording_Td(record.id, tdName);
                //操作
                tr += "<td>";
                //修改账号信息
                tr += "<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
					onclick='showModal_updateIncomeAccount(" + record.id + ",showAccountList)' contentRight='IncomeAccountThird:Update:*'>\
					<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
                //提现
                tr += "<button class='btn btn-xs btn-white btn-warning ss btn-bold contentRight' type='button' "
                    + "onclick='showEncashThirdModal(" + record.id + ")' contentRight='IncomeAccountThird:Withdraw:*'>"
                    + "<i class='ace-icon fa fa-credit-card bigger-100 orange'></i><span>提现</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold "+OperatorLogBtn+"' " +
						"onclick='showModal_accountExtra("+record.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
                tr += "</td>";
                $tbody.append($("<tr id='mainTr" + record.id + "'>" + tr + "</tr>"));
                totalBalanceByBank += record.bankBalance * 1;
                totalBalance += record.balance * 1;
                loadHover_accountTodayInto(record.id, record.totalAmount, record.amount, record.feeAmount, record.balance, record.mappedAmount, record.mappingAmount);

            });
            //异步刷新数据
            loadInOutTransfer(fromIdArray, tdName, null, 'IncomeAccountThird:EncashLog:*');
            //有数据时，显示总计 小计
            if (jsonObject.page && (jsonObject.page.totalElements * 1)) {
                var totalRows = {
                    column: 12,
                    subCount: jsonObject.data.length,
                    count: jsonObject.page.totalElements,
                    6: {subTotal: totalBalance, total: jsonObject.page.header.totalAmountBalance},
                    7: {subTotal: totalBalanceByBank, total: jsonObject.page.header.totalAmountBankBalance}//,
                };
                showSubAndTotalStatistics4Table($tbody, totalRows);
            }
            $("[data-toggle='popover']").popover();
            //分页初始化
            showPading(jsonObject.page, "accountPage", showAccountList, null, false, true);
            contentRight();
            //加载账号悬浮提示
            loadHover_accountInfoHover(idList);
        }
    });
}

var loadHover_accountTodayInto = function (id, totalAmount, amount, feeAmount, balance, mappedAmount, mappingAmount) {
    $("[data-toggle='summary" + id + "']").popover({
        html: true,
        title: function () {
            return '<center class="blue">今日入款汇总</center>';
        },
        delay: {show: 0, hide: 100},
        content: function () {
            return "<div id='accountInfoHover' style='width:400px' >"
                + "<div class='col-sm-12'>"
                + "	<div class='col-xs-4 text-right'><strong>入款金额：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + totalAmount + "</span></div>"
                + "	<div class='col-xs-4 text-right'><strong>实际入款：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + amount + "</span></div>"
                + "</div>"
                + "<div class='col-sm-12'>"
                + "	<div class='col-xs-4 text-right'><strong>手续费：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + feeAmount + "</span></div>"
                + "	<div class='col-xs-4 text-right'><strong>系统余额：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + (balance == null ? 0 : balance) + "</span></div>"
                + "</div>"
                + "<div class='col-sm-12'>"
                + "	<div class='col-xs-4 text-right'><strong>已匹配：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + (mappedAmount == null ? 0 : mappedAmount) + "</span></div>"
                + "	<div class='col-xs-4 text-right'><strong>未匹配：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + (mappingAmount == null ? 0 : mappingAmount) + "</span></div>"
                + "</div>";
            // return "<table border='1'><tr><td align='left' width='500px;'>入款金额："+totalAmount+"</td><td width='200px;'>实际入款："+amount+"</td></tr><tr><td width='200px;'>手续费："+feeAmount+"</td><td width='200px;'>系统余额："+balance+"</td></tr><tr><td width='200px;'>已匹配："+mappedAmount+"</td><td width='200px;'>未匹配："+mappingAmount+"</td></tr></table>";
        }
    });
}


function changeInput(id, value) {
    $("#bankBalance" + id).find("span").html("<input onkeyup='clearNoNum(this)' id='bankBalanceInput" + id + "' class='input-sm' style='width:80px;' value='" + value + "'>");
    $("#bankBalance" + id).find("i").attr("class", "green ace-icon fa fa-check-square-o");
    $("#bankBalance" + id).find("i").attr("onclick", "savaBankBalance(" + id + ")");
}
function savaBankBalance(id) {
    var data = {
        "id": id,
        "bankBalance": $("#bankBalanceInput" + id).val(),
    };
    $.ajax({
        type: "PUT",
        dataType: 'JSON',
        url: '/r/account/update',
        async: false,
        data: data,
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data) {
                showAccountList();
            } else {
                showMessageForFail("账号修改失败：" + jsonObject.message);
            }
        },
        error: function (result) {
            showMessageForFail("修改失败：" + jsonObject.message);
        }
    });
}
var toMinimize = function () {
    $("#Third_EnCash_Modal").modal("toggle");
    if (ws_thirdInAccountDraw) {
        ws_thirdInAccountDraw.close();
        ws_thirdInAccountDraw = null;
    }
    if (subTabInDrawPage===2){
        dealBalanceAmountOnchange(null);
        subTabInDrawPage=null;
    }
    // setTimeout(function () {
    //     $("#issuedBy").show();
    // }, 1500);
}

var closeThirdModal = function () {
    $("#Third_Detail_Modal").modal("toggle");
    $("#Third_Detail_Modal,.modal-backdrop").remove();
    $("body").removeClass('modal-open');
}

var closeModal = function (type) {
    $("#Third_EnCash_Modal").modal("toggle");
    $("#Third_EnCash_Modal,.modal-backdrop").remove();
    $("body").removeClass('modal-open');
    $("#issuedBy").hide();
    if (type != 1) {
        if (subTabInDrawPage===2){
            dealBalanceAmountOnchange(null);
            subTabInDrawPage=null;
        }
    	setTimeout(function () {
    	　　　 showAccountList(0);
    	　　},50);
        if (ws_thirdInAccountDraw) {
            ws_thirdInAccountDraw.close();
            ws_thirdInAccountDraw = null;
        }
    }
}
function intersection(obj, index) {
    if ($("#updateIncomeAccount").find("input[name='rates']").length > 0) {
        for (var i = 0; i < $("#updateIncomeAccount").find("input[name='rates']").length; i++) {
            if (i == index)
                continue;
            var startAmount = $("#updateIncomeAccount").find("input[name='startAmount']")[i].value;
            var endAmount = $("#updateIncomeAccount").find("input[name='endAmount']")[i].value;
            if (startAmount != "" && endAmount != "") {
                if (obj.value != "" && obj.value * 1 >= startAmount * 1 && obj.value * 1 <= endAmount * 1) {
                    showError("存在交集请检查！");
                    //obj.focus();
                    return false;
                }
            } else if (startAmount != "" && endAmount == "") {
                if (obj.value * 1 > startAmount * 1) {
                    showError("存在交集请检查！");
                    //obj.focus();
                    return false;
                }
            }
        }
    }
    return true;
}
function showError(message) {
    $.gritter.add({
        time: '500',
        class_name: '',
        title: '系统消息',
        text: message,
        sticky: false,
        image: '../images/message.png'
    });
}
function clearNoNum(obj) {
    //先把非数字的都替换掉，除了数字和.
    obj.value = obj.value.replace(/[^\d.-]/g, "");
    //保证只有出现一个.而没有多个.
    obj.value = obj.value.replace(/\.{2,}/g, ".");
    //保证.只出现一次，而不能出现两次以上
    obj.value = obj.value.replace(".", "$#$").replace(/\./g, "").replace("$#$", ".");
}



/** 设置按钮 */
var showModal_thirdInfo=function(){
	var $div=$("#thirdInfoModal").modal("toggle");
	//重置参数
	resetThirdInfo();
}
var resetThirdInfo=function(){
	var $divThird=$("#thirdInfoModal");
    //查询最新系统设置
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
            //全局系统设置变量
            sysSetting = res.data;
        }
    });
    //是否开启第三方到出款卡的下发
	 $.ajax({
	        type: "GET",
	        url: '/r/set/third2OutSetting',
	        dataType: 'JSON',
	        async: false,
	        success: function (jsonObject) {
	        	if (jsonObject && jsonObject.status == 1) {
	        		$divThird.find("[name=third2Out][value="+jsonObject.data+"]").prop("checked",true);
	            }else{
	                showMessageForFail("是否开启第三方下发查询失败");
	            }
	        }
	 });
    $divThird.find("[name=THIRD_TO_OUT_MORE_BALANCE]").val(sysSetting.THIRD_TO_OUT_MORE_BALANCE);
    $divThird.find("[name=THIRD_TO_OUT_LESS_BALANCE]").val(sysSetting.THIRD_TO_OUT_LESS_BALANCE);
    $divThird.find("[name=THIRD_TO_OUT_BELOW_BALANCE]").val(sysSetting.THIRD_TO_OUT_BELOW_BALANCE);
    $divThird.find("[name=THIRD_TO_OUT_INTER_AMOUNT]").val(sysSetting.THIRD_TO_OUT_INTER_AMOUNT);
    $divThird.find("[name=THIRD_TO_OUT_OUTTER_AMOUNT]").val(sysSetting.THIRD_TO_OUT_OUTTER_AMOUNT);
    $divThird.find("[name=ISSUED_TO_OUT_EXCEED_CREDITS_PERCENTAGE]").val(sysSetting.ISSUED_TO_OUT_EXCEED_CREDITS_PERCENTAGE);
    $divThird.find("[name=OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME]").val(sysSetting.OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME);
    $divThird.find("[name=DRAW_TASK_SINGLE_AMOUNT]").val(sysSetting.DRAW_TASK_SINGLE_AMOUNT);
};
var saveThirdInfo=function(){
	var $divThird=$("#thirdInfoModal");
    var keysArray = new Array(), valsArray = new Array();
    var validate = new Array();
    var $THIRD_TO_OUT_MORE_BALANCE = $divThird.find("[name=THIRD_TO_OUT_MORE_BALANCE]");
    var $THIRD_TO_OUT_LESS_BALANCE = $divThird.find("[name=THIRD_TO_OUT_LESS_BALANCE]");
    var $THIRD_TO_OUT_BELOW_BALANCE = $divThird.find("[name=THIRD_TO_OUT_BELOW_BALANCE]");
    var $THIRD_TO_OUT_INTER_AMOUNT = $divThird.find("[name=THIRD_TO_OUT_INTER_AMOUNT]");
    var $THIRD_TO_OUT_OUTTER_AMOUNT = $divThird.find("[name=THIRD_TO_OUT_OUTTER_AMOUNT]");
    var $ISSUED_TO_OUT_EXCEED_CREDITS_PERCENTAGE = $divThird.find("[name=ISSUED_TO_OUT_EXCEED_CREDITS_PERCENTAGE]");
    var $OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME = $divThird.find("[name=OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME]");
    var $DRAW_TASK_SINGLE_AMOUNT = $divThird.find("[name=DRAW_TASK_SINGLE_AMOUNT]");
    validate.push({ele: $THIRD_TO_OUT_MORE_BALANCE,name: '出款卡余额设置（用于判断同一区域同一层级中有多少张出款卡达到要求）',type: 'amountPlus',minEQ: 10000,maxEQ: 30000});
    validate.push({ele: $THIRD_TO_OUT_LESS_BALANCE,name: '出款卡可下发金额大于该值，给第三方下发',type: 'amountPlus',minEQ: 10000,maxEQ: 30000});
    validate.push({ele: $THIRD_TO_OUT_BELOW_BALANCE,name: '出款卡余额大于该值，不给第三方下发',type: 'amountPlus',minEQ: 500,maxEQ: 3000});
    validate.push({ele: $THIRD_TO_OUT_INTER_AMOUNT,name: '内层的满足条件的出款卡数量超过设定值给第三方下发',type: 'amountPlus',minEQ: 1,maxEQ: 50});
    validate.push({ele: $THIRD_TO_OUT_OUTTER_AMOUNT,name: '外层的满足条件的出款卡数量超过设定值给第三方下发',type: 'amountPlus',minEQ: 1,maxEQ: 50});
    validate.push({ele: $ISSUED_TO_OUT_EXCEED_CREDITS_PERCENTAGE,name: '下发给出款卡的钱允许超过该卡的信用额度百分比',type: 'amountPlus',minEQ: 1,maxEQ: 10});
    validate.push({ele: $OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME, name: '第三方下发到出款卡锁定的过期时间', minEQ: 5000, maxEQ: 500000});
    validate.push({ele: $DRAW_TASK_SINGLE_AMOUNT, name: '发任务本次下发金额最小值', minEQ: 1, maxEQ: 1440});
    $.each($divThird.find("input"), function (index, result) {
        keysArray.push($(result).attr("name"));
        valsArray.push($(result).val());
    });
    //只做必填校验，不做输入校验
    if (!validateEmptyBatch(validate)) {
		 setTimeout(function(){       
	            $('body').addClass('modal-open');
	        },500);
        return;
    }
	bootbox.confirm("确定修改系统设置?", function(result) {
		if (result) {
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
		                $divThird.modal("toggle");
		            } else {
		                showMessageForFail("保存失败" + jsonObject.message);
		            }
		        }
		    });
		}
		 setTimeout(function(){       
	            $('body').addClass('modal-open');
	        },500);
	});
};
/** 是否开启第三方到出款卡的下发 */
var third2OutSetting=function(){
	 $.ajax({
	        type: "GET",
	        url: '/r/set/third2OutSetting',
	        dataType: 'JSON',
	        async: false,
	        data:{"action":$("#thirdInfoModal").find("[name=third2Out]:checked").val()},
	        success: function (jsonObject) {
	        	if (jsonObject && jsonObject.status == 1) {
		        	showMessageForSuccess("保存成功");
	            } else {
	                showMessageForFail("保存失败" + jsonObject.message);
	            }
	        }
	 });
}



var changeTabStatus=function(tabStatus){
	$("#tabStatus").val(tabStatus);
	showAccountList(0);
}
//加载盘口信息
getHandicap_select($("select[name='search_EQ_handicapId']"), 0, "全部");
initRefreshSelect($("#accountFilter #refreshAccountListSelect"), $("#accountFilter #searchBtn"), 150, "refresh_accountInThird");
//进入页面默认加载第三方账号信息
showAccountList(0);
$("#accountFilter").keypress(function (e) {
    if (event.keyCode == 13) {
        $("#accountFilter #searchBtn button").click();
    }
});

