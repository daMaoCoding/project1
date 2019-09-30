currentPageLocation = window.location.href;
var _outerUlLiClickAccountType13 = function (type) {
    if (type==='accountsManage') {
        $('#accountFilter').show();$('#FilterEncash_Third').hide();
        showAccountList(0);
    }else{
        $('#accountFilter').hide();$('#FilterEncash_Third').show();
        showEncashAccountList(0,'1');
    }
}
var resetAccountNeedToDrawSearch=function (type) {
    reset(type); //showEncashAccountList(0,thirdAccountSubPage);
};
var accountNeedToDrawSearch = function () {
    showEncashAccountList(0);
}
var lockedUnlockClick=function (type) {
    $("#EnCashBindType").val(type);
    if (type===1){
        $('#doEnchashments').hide();
    } else{
        $('#doEnchashments').show();
    }
    showEncashAccountList(0);
}
var showEncashAccountList = function (CurPage) {
    var $div = $("#FilterEncash_Third"), data = {};
    if (!!!CurPage && CurPage != 0) CurPage = $("#Encash_Third_Table_Page .Current_Page").text();
    var statusArray = new Array(), typeArray = new Array();
    $div.find("input[name='search_IN_status']:checked").each(function () {
        statusArray.push(this.value);
    });
    if (statusArray.length==0){
        statusArray=[1,4];
    }
    var currSysLevelToArray = new Array();
    $div.find("input[name='currSysLevel']:checked").each(function () {
        currSysLevelToArray.push(this.value);
    });
    var thirdAccountIdToDraw = $("#Third_EnCash_Modal #accountInfo_id").val();
    var flag=$('input:radio[name="flag"]:checked').val();
    //提现到下发卡时
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
        "typeOfIssueToArray": [ accountTypeBindWechat , accountTypeBindAli , accountTypeThirdCommon, accountTypeBindCommon].toString(),
        "binding0binded1": $("#EnCashBindType").val(),
        "currSysLevelToArray": currSysLevelToArray.toString(),
        "flag":flag,
        "pageSize": $.session.get('initPageSize'),
        "pageNo": CurPage <= 0 ? 0 : CurPage - 1
    };
    var $tbody = $('#accountsNeedDrawDiv').find("#Encash_Third_Table tbody");
    $.ajax({
        dataType: 'JSON', type: "POST", async: false, url: "/r/account/findbindissue", data: data,
        success: function (jsonObject) {
            if (-1 == jsonObject.status) {
                showMessageForFail("查询失败：" + jsonObject.message);
                return;
            }
            var tbodyStr = "", idList = new Array(),idListStatus = new Array();
            //绑定 未绑定  已锁定 分开
            //绑定才可以提现  只看到自己锁定的账号  和 未被锁定的账号  别人锁定的账号看不见
            var subTabType = $div.find("#EnCashBindType").val();
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
                if (subTabType==='1'){
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
                tr += "<td>" +
                    "<a onclick='toMinimize();' href='#/EncashCheck4Transfer:*?toAccountId=" + record.id + "&toAccount=" + record.account + "&incomeReqStatus=0" + "'><span class='badge badge-warning' title='匹配中'>" + record.inCount.mapping + "</span></a>"
                    + "<a onclick='toMinimize();' href='#/EncashCheck4Transfer:*?toAccountId=" + record.id + "&toAccount=" + record.account + "&incomeReqStatus=1" + "'><span class='badge badge-success' title='已匹配'>" + record.inCount.mapped + "</span></a>"
                    + "<a onclick='toMinimize();' href='#/EncashCheck4Transfer:*?toAccountId=" + record.id + "&toAccount=" + record.account + "&incomeReqStatus=3" + "'><span class='badge badge-inverse' title='已驳回'>" + record.inCount.cancel + "</span></a></td>";
                var lockedFlag1 = 999999 + parseInt(getCookie('JUSERID'))+parseInt(thirdAccountIdToDraw);
                var lockedFlag2 = 999999 + parseInt(getCookie('JUSERID'));
                if (subTabType === '1') {
                    //未锁定 包括绑定和未绑定的  这个页签 只需有锁定按钮(弹出第三方账号列表 选择锁定的第三方账号 且只能选择一个第三方账号锁定)
                    tr += "<td><button type='button' class='btn btn-xs btn-white btn-warning btn-bold green' " +
                        "onclick='beforeLocked("+ record.id + ")'>" +
                        "<i class='ace-icon fa fa-unlock bigger-100 green'></i><span>锁定</span></button>";
                }
                if (subTabType === '2') {
                    //当前人锁定 可以看到锁定的所有账号
                    if ((record.lockByOperator && (record.lockByOperator == lockedFlag1||record.lockByOperator == lockedFlag2))||(record.bindId && record.bindId==5)) {
                        //提现按钮
                        tr += "<td>" +
                            "<span class='input-icon'>" +
                            "<input onkeyup=\"this.value=this.value.replace(/\\D/g,'').replace(/^(00+)|[^\\d]+/g,'')\" onafterpaste=\"this.value=this.value.replace(/\\D/g,'')\" min='0' name='drawAmount' onchange='saveInputTemplate(this.value,\"" + record.id + "\",1);' style='width:70px;' value='" +  record.drawAbleAmount+ "' class='input-sm' type='text' " + valueStr + " id='amount" + record.id + "' >" +
                            "<i class='ace-icon fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target=\"#amount" + record.id + "\" ></i>" +
                            "</span>" +
                            "</td>"; //金额 自动填充 银行余额-最低限额  (parseInt((record.limitBalance * 1 - record.bankBalance * 1)) * -1 >= 0 ? parseInt((record.limitBalance * 1 - record.bankBalance * 1)) * -1 : setAmountForLockedAccount(record.id))
                        tr += "<td>" +
                            "<span class='input-icon'>" +
                            "<input onkeyup=\"this.value=this.value.replace(/\\D/g,'').replace(/^(00+)|[^\\d]+/g,'')\" onafterpaste=\"this.value=this.value.replace(/\\D/g,'')\"  min='0' name='drawFee' onchange='saveInputTemplate(this.value,\"" + record.id + "\",2);' style='width:70px;' value='" +record.drawAbleFee  + "' class='input-sm' type='text' id='fee" + record.id + "' >" +
                            "<i class='ace-icon fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target=\"#fee" + record.id + "\" ></i>" +
                            "</span>" +
                            "</td>";//手续费 setFeeForLockedAccount(record.id)
                        tr += "<td>" +
                            "<button class='btn btn-xs btn-white btn-danger btn-bold' " +
                            "onclick='doEnchashment(" + $div.find("#accountInfo_id").val() + "," + record.id + ")'>" +
                            "<i class='ace-icon fa fa-credit-card bigger-100 red'></i><span>提现</span></button>" +
                            "</td>";
                    } else{
                        tr +="<td></td><td></td><td></td>";
                    }
                    //已锁定数据，显示解锁、并且把锁定的账号id记录下来
                    rememberOrRemoveIds(record.id, 1);
                    tr += "<td><button type='button' class='btn btn-xs btn-white btn-success btn-bold' " +
                        "onclick='rememberOrRemoveIds(" + record.id + ",0);doLockOrUnlockBind(" + $div.find("#accountInfo_id").val() + "," + record.id + ",0,1);'>" +
                        "<i class='ace-icon fa fa-lock  bigger-100 orange '></i><span>解锁</span></button></td>";
                }
                tbodyStr += "<tr>" + tr + "</tr>";
            });
            $tbody.html(tbodyStr);
            showPading(jsonObject.page, "Encash_Third_Table_Page", showEncashAccountList, null, true);
            //SysEvent .on(SysEvent.EVENT_OFFLINE,bankLogOffline,idListStatus);

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
var resetQueryThirdAccount = function () {
    reset('accountInThirdModal_filter');queryThirdAccount(0);
}
//模态框查询在用的第三方账号
var beforeLocked=function (id) {
    $('#accountId').val(id);
    queryThirdAccount(0);
    $('#accountInThirdModal').modal('show');
}
var queryThirdAccount = function (CurPage) {
    //封装data
    var $div = $('#accountInThirdModal  #accountInThirdModal_filter');
    if (!!!CurPage && CurPage != 0) CurPage = $("#accountInThirdModalPage .Current_Page").text();
    var search_LIKE_account = $div.find("[name='search_LIKE_account']").val();
    var search_EQ_handicapId = $div.find("select[name='search_EQ_handicapId']").val();
    if (!search_EQ_handicapId) {
        //只查询当前人拥有的盘口账号信息
        search_EQ_handicapId = [];
        $('select[name="search_EQ_handicapId"]').find('option:not(:first-child)').each(function () {
            search_EQ_handicapId.push($(this).val());
        });
    }
    var levelId = $div.find("select[name='search_EQ_LevelId']").val();
    var search_LIKE_auditor = $div.find("input[name='search_LIKE_auditor']").val();
    var search_LIKE_bankName = $div.find("input[name='search_LIKE_bankName']").val();
    var search_LIKE_owner = $div.find("input[name='search_LIKE_owner']").val();
    var statusToArray = [1];
    var fromIdArray = new Array(), tdName = 'detail';
    var data = {
        pageNo: CurPage <= 0 ? 0 : CurPage - 1,
        pageSize: $.session.get('initPageSize'),
        search_LIKE_account: search_LIKE_account,
        levelId: levelId,
        auditor: search_LIKE_auditor,
        search_LIKE_bankName: $.trim(search_LIKE_bankName),
        search_LIKE_owner: $.trim(search_LIKE_owner),
        statusToArray: statusToArray.toString(),
        typeToArray: [accountTypeInThird].toString(),
        sortProperty: 'status',
        sortDirection: 0,'queryFlag':1
    }
    if (search_EQ_handicapId instanceof Array) {
        //search_EQ_handicapId:search_EQ_handicapId.toString(),
        data.search_IN_handicapId = search_EQ_handicapId.toString();
    } else {
        data.search_EQ_handicapId = search_EQ_handicapId;
    }
    $.ajax({
        dataType: 'JSON', type: "POST", async: false, url: API.r_account_list, data: data,
        success:function (jsonObject) {
            if (jsonObject){
                if (jsonObject.status != 1) {
                    if (-1 == jsonObject.status) {
                        showMessageForFail("查询失败：" + jsonObject.message);
                    }
                    return;
                }
                var $tbody = $("table#accountThirdListTable").find("tbody");
                $tbody.html("");
                var totalBalanceByBank = 0, totalBalance = 0, idList = new Array;
                $.each(jsonObject.data, function (index, record) {
                    fromIdArray.push(record.id);
                    var tr = "<td><input type='checkbox' value='"+record.id+"'></td>";
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
                    tr += "<button class='btn btn-xs btn-white btn-warning btn-bold  ' \
					onclick='showModal_updateIncomeAccount(" + record.id + ",showAccountList)' contentRight='IncomeAccountThird:Update:*'>\
					<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
                    //锁定
                    if ($("#EnCashBindType").val()==='1'){
                        tr += "<button class='btn btn-xs btn-white btn-warning ss btn-bold  ' type='button' "
                            + "onclick='lockedUnlockClick(" + record.id + ")' contentRight='IncomeAccountThird:Withdraw:*'>"
                            + "<i class='ace-icon fa fa-unlock bigger-100 green'></i><span>锁定</span></button>";
                    }else{
                        //提现
                        tr += "<button class='btn btn-xs btn-white btn-warning ss btn-bold  ' type='button' "
                            + "onclick='drawOut(" + record.id + ")' contentRight='IncomeAccountThird:Withdraw:*'>"
                            + "<i class='ace-icon fa fa-credit-card bigger-100 orange'></i><span>提现</span></button>";
                    }
                    tr+="<button class='btn btn-xs btn-white btn-primary btn-bold "+OperatorLogBtn+"' " +
                        "onclick='showModal_accountExtra("+record.id+")'>"+
                        "<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
                    //明细
                    tr += "<button class='btn btn-xs btn-white btn-info btn-bold' type='button' " +
                        "onclick='show_third_InOutListModal(" + record.id + ")'>" +
                        "<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
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
                        7: {subTotal: totalBalance, total: jsonObject.page.header.totalAmountBalance},
                        8: {subTotal: totalBalanceByBank, total: jsonObject.page.header.totalAmountBankBalance}//,
                    };
                    showSubAndTotalStatistics4Table($tbody, totalRows);
                }
                $("[data-toggle='popover']").popover();
                //分页初始化
                showPading(jsonObject.page, "accountInThirdModalPage", queryThirdAccount, null,false, true);
                //contentRight();
                //加载账号悬浮提示
                loadHover_accountInfoHover(idList);
            }
        }
    });
}
//锁定
var  lockedUnlock = function (thirdId) {

}
//提现
var drawOut = function (thirdId) {

}
function clearNoNum(obj) {
    //先把非数字的都替换掉，除了数字和.
    obj.value = obj.value.replace(/[^\d.-]/g, "");
    //保证只有出现一个.而没有多个.
    obj.value = obj.value.replace(/\.{2,}/g, ".");
    //保证.只出现一次，而不能出现两次以上
    obj.value = obj.value.replace(".", "$#$").replace(/\./g, "").replace("$#$", ".");
}
var oldBalanceValue = null;
function changeInput(id, value) {
    oldBalanceValue=value;
    $("#bankBalance" + id).find("span").html("<input onkeyup='clearNoNum(this)' id='bankBalanceInput" + id + "' class='input-sm' style='width:80px;' value='" + value + "'>");
    $("#bankBalance" + id).find("i").attr("class", "green ace-icon fa  fa-check-square");
    $("#bankBalance" + id).find("i").attr("onclick", "savaBankBalance(" + id + ")");
}
//保存修改第三方余额
function savaBankBalance(id) {
    var newBalanceVal = $("#bankBalanceInput" + id).val();
    if (oldBalanceValue && newBalanceVal && oldBalanceValue==newBalanceVal) {
        console.debug("旧余额:"+oldBalanceValue+",新余额:"+newBalanceVal+" 一致 ");
        var str = '<span name="bankBalance">'+ oldBalanceValue + '</span><i class="red ace-icon fa fa-pencil-square-o" onclick="changeInput('+id+','+oldBalanceValue+')" title="校正余额"  style="cursor:pointer;"></i>';
        $("#bankBalance" + id).html(str);
        return;
    }
    var data = {
        "id": id,
        "bankBalance":newBalanceVal ,
    };
    $.ajax({
        type: "PUT",
        dataType: 'JSON',
        url: '/r/account/update',
        async: false,
        data: data,
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data) {
                queryThirdAccount(0);
            } else {
                showMessageForFail("账号修改失败：" + jsonObject.message);
            }
        },
        error: function (result) {
            showMessageForFail("修改失败：" + jsonObject.message);
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

var data = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml = '<tr>\
		<td><span>{currSysLevelName}</span></td>\
		<td><span>{alias}</span></td>\
		<td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"  >{accountInfo}</a></td>\
		<td>{typeStr}</td>\
		<td>{statusHTML}</td>\
		<td>{flagStr}</td>\
		<td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
		<td><div class="SysLogEvent" target="{id}"><span class="amount">{balance}</span></div></td>\
		<td>\
			<a {inCountMappingEle} target="_self" class="contentRight" contentRight="IncomeAccountIssue:EncashLog:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
			<a {inCountMappedEle} target="_self" class="contentRight" contentRight="IncomeAccountIssue:EncashLog:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
			<a {inCountCancelEle} target="_self" class="contentRight" contentRight="IncomeAccountIssue:EncashLog:*" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
			<span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId={id}>{matchingAmtIn}</span>\
		</td>\
		<td>\
		 <a {outCountMappingEle} target="_self" class="contentRight" contentRight="IncomeAccountIssue:EncashLog:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
		 <a {outCountMappedEle} target="_self"  class="contentRight" contentRight="IncomeAccountIssue:EncashLog:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
		 <a {outCountCancelEle} target="_self"  class="contentRight" contentRight="IncomeAccountIssue:EncashLog:*" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{outCountCancel}</span></a>\
		 <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
		</td>\
		<td>'
			+'<button class="btn btn-xs btn-white btn-bold btn-warning contentRight {isRetrieve} " onclick="recycle4BindComm({id})" contentRight="IncomeAccountIssue:UpdateStatus:*"><i class="ace-icon fa fa-undo bigger-100 orange"></i><span>回收卡</span></button>'
			+'<button class="btn btn-xs btn-white btn-bold btn-info contentRight {isNotRetrieve} " onclick="cancelRecycle4BindComm({id})" contentRight="IncomeAccountIssue:UpdateStatus:*"><i class="ace-icon fa fa-undo bigger-100 blue"></i><span>取消回收</span></button>'
			+'<button class="btn btn-xs btn-white btn-bold btn-warning contentRight" onclick="showUpdateAccountNewModal({id},showAccountList)" contentRight="IncomeAccountIssue:Update:*"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
			+'<button class="btn btn-xs btn-white btn-bold btn-success {isHideAccountBtn}" onclick="showModal_accountBaseInfo({id})" ><i class="ace-icon fa fa-eye bigger-100 green"></i><span>状态</span></button>'
			+'<button class="btn btn-xs btn-white btn-bold btn-info {OperatorLogBtn}" onclick="showModal_accountExtra({id})" ><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
			+'<button class="btn btn-xs btn-white btn-bold btn-info" onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>明细</span></button>'
			+'<button class="btn btn-xs btn-white btn-bold btn-info {classHiddenOfBtnBindList}" onclick="showBindAccountListModal({id})" contentRight="IncomeAccountIssue:BindHis:*"><i class="ace-icon fa fa-sitemap bigger-100 blue"></i><span>绑定记录</span></button>'
		+'</td>\
	</tr>';


/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	var $div = $("#accountFilter");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountPage .Current_Page").text();
	var flagToArray = new Array();
    $div.find("input[name='search_IN_flag']:checked").each(function(){
    	flagToArray.push(this.value);
    });
    if(flagToArray.length==0){
    	flagToArray=[accountFlagPC,accountFlagRefund];
    }
    data.search_IN_flag=flagToArray.toString();
	data.pageNo = CurPage<=0?0:CurPage-1;
	data.search_LIKE_account = $.trim($div.find("input[name='search_LIKE_account']").val());
	data.search_LIKE_owner = $.trim($div.find("input[name='search_LIKE_owner']").val());
	data.bankType=$.trim($div.find("select[name='search_LIKE_bankType']").val());
    if(!data.bankType||data.bankType=='请选择'){
        data.bankType = null;
    }
	data.search_LIKE_alias = $.trim($div.find("input[name='search_LIKE_alias']").val());
	data.search_IN_handicapId=handicapId_list.toString();
	data.typeToArray = [accountTypeBindWechat,accountTypeBindAli,accountTypeThirdCommon,accountTypeBindCommon].toString();
	data.sortProperty='status';
	data.sortDirection=0;
    data.pageSize=$.session.get('initPageSize');
    data.currSysLevel=$div.find("input[name='currSysLevel']:checked").val();
    data.isRetrieve=$div.find("input[name='isRetrieve']:checked").val();
    data.deviceStatus=$("#deviceStatus").val();
    data.needTotal=1;//需要查询账号状态总计
    if(data.deviceStatus){
    	data.statusToArray=[1,5].toString();
    	if(data.deviceStatus=="stop"){
    		data.deviceStatus=null;
    		data.statusToArray=[4].toString();
    	}
    }else{
    	data.statusToArray=[1,4,5].toString();
    }
	$.ajax({ dataType:'JSON',type:"POST", async:false, url:API.r_account_list2, data:data,success:function(jsonObject){
			if(jsonObject.status !=1){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
			var $tbody=$("table#accountListTable").find("tbody");
            var totalBalanceBySys = 0,totalBalanceByBank =0 ,totalIncomeAmountDaily=0;
			$.each(jsonObject.data,function(index,record){
				record.flagStr=getFlagStr(record.flag);
		        record.OperatorLogBtn=OperatorLogBtn;
		        record.isHideAccountBtn=isHideAccountBtn;
				record.handicapName=record.handicapName?record.handicapName:'';
				record.alias=(record.alias&&record.alias!='null')?record.alias:'';
				record.currSysLevelName=record.currSysLevelName?record.currSysLevelName:'';
				record.classOfStatus=(record.status==accountStatusFreeze || record.status==accountStatusStopTemp)?'label-danger':((record.status ==accountStatusEnabled)?'label-purple':'label-success');
				record.limitIn=(!!!record.limitIn)?eval(sysSetting.INCOME_LIMIT_CHECKIN_TODAY):record.limitIn;
				record.DailyAmount=htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily);
				record.classHiddenOfBtnBindList =  record.type==accountTypeThirdCommon ?'hidden':'contentRight';
				totalIncomeAmountDaily+=record.incomeAmountDaily*1;
				totalBalanceBySys+=record.balance*1;
				totalBalanceByBank+=record.bankBalance*1;
				record.balance = record.balance?record.balance:'0';
				record.limitBalanceIcon=getlimitBalanceIconStr(record);
				record.bankBalance=record.bankBalance?record.bankBalance:'0';
				record.incomeDetail=getRecording_Td(record.id,"incomeDetail");
				record.statusHTML=getStatusInfoHoverHTML(record)+"<br/>"+getDeviceStatusInfoHoverHTML(record);
				record.outDetail=getRecording_Td(record.id,"outDetail");
				record.inCountMapping ='0';
				record.inCountMapped ='0';
				record.inCountCancel ='0';
				record.matchingAmtIn ='';
				record.outCountMapping ='0';
				record.outCountMapped ='0';
				record.outCountCancel ='0';
				record.matchingAmtOut ='';
				record.accountStatInOutCategoryOutTranfer=accountStatInOutCategoryOutTranfer+'';
				record.accountStatInOutCategoryIn=accountStatInOutCategoryIn+'';
				record.inCountMappingEle=(record.inCountMapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
				record.inCountMappedEle=(record.inCountMapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
				record.inCountCancelEle=(record.inCountCancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
				record.outCountMappingEle=(record.outCountMapping!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
				record.outCountMappedEle=(record.outCountMapped!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
				record.outCountCancelEle=(record.outCountCancel!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
				record.hideAccount=hideAccountAll(record.account);
				record.accountInfo =record.bankType+'|'+hideName(record.owner)+'<br/>'+record.hideAccount ;
				if(record.status==accountStatusFreeze){
					record.classHideOperate=" hide ";
				}
				if(record.status==accountStatusNormal){
					//在用卡
					if(record.isRetrieve&&record.isRetrieve==1){	//回收状态
						record.isRetrieve=" hidden ";
					}else{	//非回收状态
						record.isNotRetrieve=" hidden ";
					}
				}else{
					//非在用卡没有回收状态
					record.isNotRetrieve=" hidden ";
					record.isRetrieve=" hidden ";
				}
				
			});
			$tbody.html(fillDataToModel4Array(jsonObject.data,trHtml));
			showSubAndTotalStatistics4Table($tbody,{column:13,subCount:jsonObject.data.length,count:jsonObject.page.totalElements,7:{subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance},8:{subTotal:totalBalanceBySys,total:jsonObject.page.header.totalAmountBalance}});
			showPading(jsonObject.page,"accountPage",showAccountList);
			//统计总卡数量
			if(jsonObject&&jsonObject.page&&jsonObject.page.header&&jsonObject.page.header.IdSize){
				var IdSize=jsonObject.page.header.IdSize;
				$("#totalIdSize").text(IdSize[0]);
				$("#onlineSize").text(IdSize[1]);
				$("#offlineSize").text(IdSize[2]);
				$("#stopSize").text(IdSize[3]);
			}
			//账号悬浮提示
			var idList=new Array(),idArray = new Array();
			$.each(jsonObject.data,function(index,result){
				idList.push({'id':result.id});
				idList.push({'id':result.id,type:'transAskMonitorRiskHover'});
				idArray.push(result.id);
			});
			loadHover_accountInfoHover(idList);
			loadHover_accountStopReasonHover(idArray);
		    loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutTranfer],idArray,null,function(){ contentRight() });
			contentRight();
		SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
        }
	});
}



var doDeleteAccount=function(accountId){
	bootbox.confirm("确定要删除吗 ?", function(result) {
		if (result) {
			$.ajax({
				dataType:'JSON',
				type:"PUT",
				async:false,
				url:API.r_account_del,
				data:{
					id:accountId
				},
				success:function(jsonObject){
					if(-1==jsonObject.status){
						showMessageForFail("操作失败："+jsonObject.message);
						return;
					}else{
						showMessageForSuccess("删除成功");
					}
					//操作成功刷新数据
					showAccountList();
				}
			});
		}
	});
}
/** 回收卡 */
var recycle4BindComm=function(accountId){
	$.ajax({
		type: "POST",
		dataType: 'JSON',
		url: '/r/account/recycle4BindComm',
		async: false,
		data:{
			"accountId":accountId
		},
		success: function (jsonObject) {
			if(-1==jsonObject.status){
				showMessageForFail("操作失败："+jsonObject.message);
				return;
			}else{
				showMessageForSuccess("回收卡成功");
			}
			//操作成功刷新数据
			showAccountList();
		}
	});
}
/** 取消回收 */
var cancelRecycle4BindComm=function(accountId){
	$.ajax({
		type: "POST",
		dataType: 'JSON',
		url: '/r/account/cancelRecycle4BindComm',
		data:{
			"accountId":accountId
		},
		async: false,
		success: function (jsonObject) {
			if(-1==jsonObject.status){
				showMessageForFail("操作失败："+jsonObject.message);
				return;
			}else{
				showMessageForSuccess("取消回收成功");
			}
			//操作成功刷新数据
			showAccountList();
		}
	});
}

getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
$("#accountFilter").find("[name=currSysLevel],[name=search_IN_flag],[name=isRetrieve]").click(function(){
	showAccountList();
});
$("#accountFilter").find("[name=search_LIKE_bankType]").change(function(){
	showAccountList();
});
contentRight();
/** 切换设备状态TAB */
var changeDevice=function(deviceStaus){
	$("#deviceStatus").val(deviceStaus);
	//showAccountList();
    _outerUlLiClickAccountType13('accountsManage');
}
//showAccountList(0);
$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		$("#accountFilter #searchBtn button").click();
	}
});
getBankTyp_select($("select[name='search_LIKE_bankType']"),null,"全部")
_outerUlLiClickAccountType13('accountsManage');

