/**
 * 获取盘口信息
 */
$('.chosen-select').chosen({
    //allow_single_deselect:true,
    enable_split_word_search: true,
    no_results_text: '没有匹配结果',
    search_contains: true
});
function inithancipad(name) {
    //可查询的单选框 调用chosen 初始化
    var opt = '<option value="请选择">请选择</option>';
    $.ajax({
        type: 'get',
        url: '/r/out/handicap',
        data: {},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + $.trim(val.code) + '">' + val.name + '</option>';
                    });
                    $('#handicap_' + name).empty().html(opt);
                    $('#handicap_' + name).trigger('chosen:updated');
                    $('#handicap_' + name + '_chosen').prop('style', 'width: 78%;');
                }
            }
        }
    });
}
var handicaps = getHandicapBatchInfoByCode();
//初始化时间
function initTime() {
    //正在匹配
    initTimePicker(true, $("[name=time_wechat_total]"), typeCustomLatestToday);
    //未匹配
    initTimePicker(true, $("[name=timeScope_wechatNotMatched]"), typeCustomLatestToday);
    //已匹配
    initTimePicker(true, $("[name=timeScope_wechatMatched]"), typeCustomLatestToday);
    //未认领
    startTime = $.trim(moment().subtract(2, 'days').format("YYYY-MM-DD HH:mm:ss"));
    endTime = $.trim(moment().subtract(1, 'days').format("YYYY-MM-DD HH:mm:ss"));
    initTimePicker(true, $("[name=timeScope_wetchatUnClaim]"), null, startTime + " - " + endTime);
    //已取消
    initTimePicker(true, $("[name=timeScope_wetchatCanceled]"), typeCustomLatestToday);
}

//取消
var TurningPlatformflag = false;
//备注
var Noteflag = false;
//补提单
var AuditorAddOrderflag = false;
//匹配
var Matchflag = false;
$.each(ContentRight['IncomeAuditWechatIn:*'], function (name, value) {
    if (name == 'IncomeAuditWechatIn:cancelOrder:*') {
        TurningPlatformflag = true;
    } else if (name == 'IncomeAuditWechatIn:CustomerAddRemark:*') {
        Noteflag = true;
    } else if (name == 'IncomeAuditWechatIn:AuditorAddOrder:*') {
        AuditorAddOrderflag = true;
    } else if (name == 'IncomeAuditWechatIn:MatchOrder:*') {
        $("#match").css('display', 'block');
    }
});

//重置
function _search() {
    inithancipad("wechat_total");
    $("#fromMoney_wechatToMatch").val("");
    $("#toMoney_wechatToMatch").val("");
    $("#member_wechatToMatch").val("");
    $("#payer_wechatToMatch").val("");
    $("#orderNo_wechatToMatch").val("");
    $("#wechatNumber").val("");
    //queryMatchingWechat();
}
$('#resetBtnNotMatched').on('click', function () {
    inithancipad("wechatNotMatched");
    $("#fromMoney_wechatNotMatched").val("");
    $("#toMoney_wechatNotMatched").val("");
    $("#member_wechatNotMatched").val("");
    $("#payer_wechatNotMatched").val("");
    $("#orderNo_wechatNotMatched").val("");
    $("#payer_wechatNo_wechatNotMatched").val("");
    //queryMatched();
})
$('#resetBtnMatched').on('click', function () {
    inithancipad("wechatMatched");
    $("#fromMoney_wechatMatched").val("");
    $("#toMoney_wechatMatched").val("");
    $("#member_wechatMatched").val("");
    $("#payer_wechatMatched").val("");
    $("#orderNo_wechatMatched").val("");
    $("#payer_wechatNo_wechatMatched").val("");
    //queryMatched();
})
$('#resetBtnWetchatUnClaim').on('click', function () {
    inithancipad("wetchatUnClaim");
    $("#fromMoney_wetchatUnClaim").val("");
    $("#toMoney_wetchatUnClaim").val("");
    $("#member_wetchatUnClaim").val("");
    $("#payer_wechatNo_wetchatUnClaim").val("");
    //queryUnClaim();
})
$('#resetBtnWetchatCanceled').on('click', function () {
    var handId = $('#handicap_wetchatCanceled').val();
    if (handId && handId.indexOf('请选择') < 0) {
        inithancipad("wetchatCanceled");
    }
    $("#fromMoney_wetchatCanceled").val("");
    $("#toMoney_wetchatCanceled").val("");
    $("#member_wetchatCanceled").val("");
    $("#orderNo_wetchatCanceled").val("");
    //queryCanceled();
})
//根据微信号统计正在匹配的流水
var queryMatchingWechat = function () {
    if (sertype != "Matching" || currentPageLocation.indexOf('IncomeAuditWechatIn:*') <= -1) {
        return;
    }
    //当前页码
    var CurPage = $("#wechat_total_page").find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;

    //盘口
    var handicap = $("#handicap_wechat_total").val();
    if (!handicap || handicap == '请选择') {
        var handicap = [];
        $('#handicap_wechat_total').find('option:not(:first-child)').each(function () {
            handicap.push($(this).val());
        });
        handicap = handicap.toString();
    }
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("#time_wechat_total").val();
    var timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        timeStart =startAndEnd[0];
        timeEnd = startAndEnd[1] ;
    } else {
        var todayStart = '';
        if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayStart = moment().hours(07).minutes(0).seconds(0);
        } else {
            todayStart = moment().subtract(1, 'days').hours(07).minutes(0).seconds(0);
        }
        var todayEnd = '';
        if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayEnd = moment().add(1, 'days').hours(06).minutes(59).seconds(59);
        } else {
            todayEnd = moment().hours(06).minutes(59).seconds(59);
        }
        timeStart = todayStart.format("YYYY-MM-DD HH:mm:ss");
        timeEnd = todayEnd.format("YYYY-MM-DD HH:mm:ss");
    }
    //查询外层的账号信息
    var data = {
        "oids": handicap.toString(), "type": 0, "inAccount": $('#wechatNumber').val(),
        "timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),
        "pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10",
    };
    $.ajax({
        type: "post",
        url: "/newpay/find4ByCondition",
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res.status == 1) {
                var tr = '', trs = '';
                if (res.data && res.data.length > 0) {
                    $.each(res.data, function (i, val) {
                        tr +='<tr><td>'+_showHandicapNameByIdOrCode(_checkObj(val.oid)) +'</td>'+
                                '<td>'+val.inAccount+'</td>'+
                                '<td>'+_showNewPayAccountStatus(val.status)+'</td>'+
                                '<td>'+val.reporttime+'</td>'+
                                '<td><span>'+val.mnt+'</span></td></tr>';
                    });
                    trs += '<tr>' +
                        '<td id="currentCount_wechat_total" colspan="15">小计：统计中..</td></tr>';
                    trs += '<tr>' +
                        '<td id="currentCountTotal_wechat_total" colspan="15">总共：统计中..</td></tr>';
                    $('#wechat_total_tbody').empty().html(tr);
                    $('#wechat_total_tbody').append(trs);
                }
                if (res.status==1 && res.page){
                    $('#currentCount_wechat_total').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#currentCountTotal_wechat_total').empty().text('合计：' + res.page.totalElements + '条记录');
                }
            } else {
                $.gritter.add({
                    time: 10000,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            showPading(res.page, "wechat_total_page", queryMatchingWechat);
        }
    });
}

//根据微信号 查找待匹配的流水和入款单。
function queryMBAndInvoice(account) {
    //提单当前页码
    var invoiceCurPage = $("#COPY" + account).find("#invoice_footPage").find(".Current_Page").text();
    if (!!!invoiceCurPage) {
        invoiceCurPage = 0;
    } else {
        invoiceCurPage = invoiceCurPage - 1;
    }
    if (invoiceCurPage < 0) {
        invoiceCurPage = 0;
    }
    //流水当前页码
    var banklogCurPage = $("#COPY" + account).find("#banklog_footPage").find(".Current_Page").text();
    if (!!!banklogCurPage) {
        banklogCurPage = 0;
    } else {
        banklogCurPage = banklogCurPage - 1;
    }
    if (banklogCurPage < 0) {
        banklogCurPage = 0;
    }
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("input[name='time_wechat_total']").val();
    var startAndEndTimeToArray = new Array();
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    startAndEndTimeToArray = startAndEndTimeToArray.toString();
    //会员号
    var member = $("#member_wechatToMatch").val();
    //订单号
    var orderNo = $("#orderNo_wechatToMatch").val();
    //开始金额
    var fromAmount = $("#fromMoney_wechatToMatch").val();
    //结束金额
    var toAmount = $("#toMoney_wechatToMatch").val();
    //存款人
    var payer = $("#payer_wechatToMatch").val();
    $.ajax({
        type: "post",
        url: "/r/IncomeAuditWechatIn/findMBAndInvoice",
        data: {
            "invoicePageNo": invoiceCurPage,
            "banklogPageNo": banklogCurPage,
            "account": account,
            "startAndEndTimeToArray": startAndEndTimeToArray,
            "member": member,
            "orderNo": orderNo,
            "fromAmount": fromAmount,
            "toAmount": toAmount,
            "payer": payer,
            "pageSize": $.session.get('initPageSize')
        },
        dataType: 'json',
        success: function (jsonObject) {
            //订单号
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.invoiceArrlist.length > 0) {
                var tr = '';
                var counts = 0;
                var amounts = 0;
                for (var index in jsonObject.data.invoiceArrlist) {
                    var val = jsonObject.data.invoiceArrlist[index];
                    tr += '<tr style="cursor: pointer;" onclick="_checkSysOrderTr(this)">'
                        + '<td><input onclick="_selectInputRadioForIncome(event,this);"  click_value=""  style="width:18px;height:18px;cursor: pointer;" type="radio"  name="sysRequestId" value="' + val.id + '"/></td>'
                        + '<td id=sysMemberName>' + val.member + '</td>'
                        + '<td id=sysAccount  style="display: none;">' + val.account + '</td>'
                        + '<td id=sysAccountId  style="display: none;">' + val.wechatid + '</td>'
                        + '<td id=sysAmount>' + val.amount + '</td>'
                        + '<td id=sysOrderNo>' + val.orderNo + '</td>'
                        + '<td id=sysCreateTime>' + val.crTime + '</td>';
                    if (_checkObj(val.remark)) {
                        if (_checkObj(val.remark).length > 10) {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark).substring(0, 10) + "..."
                                + '</a></td>';
                        } else {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark)
                                + '</a></td>';
                        }

                    } else {
                        tr += '<td></td>';

                    }
                    tr += '<td>';
                    // +'<button class=" btn btn-xs btn-white btn-warning" type="button" onclick="_beforeConceal(' + val.id + ')" >' +
                    //'<i class="ace-icon fa fa-flask bigger-120">隐藏</i>' +
                    if (TurningPlatformflag) {
                        tr += '<button id="concealOrderRightBTN" class=" btn btn-xs btn-white btn-danger" type="button" onclick="_beforeCancel(' + val.id + ',\'' + val.handicap + '\',\'' + val.orderNo + '\');" >' +
                            '<i class="ace-icon fa fa-remove bigger-120 red">取消</i></button>';
                    }
                    if (Noteflag) {
                        tr += '<button onclick="_beforeAddRemarkOrSendMessage(' + val.id + ',\'1\'' + ',\'remark\'' + ');" class="btn btn-xs btn-white btn-info" type="button" >' +
                            '<i class="ace-icon fa fa-comments">备注</i></button>';
                    }
                    tr += '</td>'
                        + '</tr>';
                    counts += 1;
                    amounts += val.amount;
                }
                $("#COPY" + account).find('#tbody_invoice').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="2">小计：' + counts + '</td>'
                    + '<td bgcolor="#579EC8" style="color:white;">' + amounts + '</td>'
                    + '<td colspan="4"></td>'
                    + '</tr>';
                $("#COPY" + account).find('#tbody_invoice').append(trs);
                var trn = '<tr>'
                    + '<td colspan="2">总计：' + jsonObject.data.invoiceDataToPage.totalElements + '</td>'
                    + '<td bgcolor="#D6487E" style="color:white;">' + jsonObject.data.invoiceTotal[0] + '</td>'
                    + '<td colspan="4"></td>'
                    + '</tr>';
                $("#COPY" + account).find('#tbody_invoice').append(trn);
            } else {
                $("#COPY" + account).find('#tbody_invoice').empty().html('<tr></tr>');
            }
            //分页初始化
            showPading(jsonObject.data.invoiceDataToPage, "COPY" + account + " #invoice_footPage", queryMBAndInvoice, null, null, null, account);
            //流水
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.bankLogArrlist.length > 0) {
                var tr = '';
                var counts = 0;
                var amounts = 0;
                for (var index in jsonObject.data.bankLogArrlist) {
                    var val = jsonObject.data.bankLogArrlist[index];
                    tr += '<tr style="cursor:pointer;" onclick="_checkBankFlowTr(this)">'
                        + '<td><input onclick="_selectInputRadioForIncome(event,this);" click_value="" style="width:18px;height:18px;cursor: pointer;" type="radio"  name="bankFlowId" value="' + val.id + '"/></td>'
                        + '<td id=bankAmount>' + val.amount + '</td>'
                        + '<td id=bankTradingTime>' + val.trStr + '</td>'
                        + '<td id=bankCreateTime>' + val.crStr + '</td>'
                        + '<td id=bankSummary>' + val.summary + '</td>';
                    if (_checkObj(val.remark)) {
                        if (_checkObj(val.remark).length > 10) {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark).substring(0, 10) + "..."
                                + '</a></td>';
                        } else {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark)
                                + '</a></td>';
                        }

                    } else {
                        tr += '<td></td>';

                    }
                    tr += '<td>';
                    if (AuditorAddOrderflag) {
                        tr += '<button onclick="_addIncomeRequest(\'' + val.id + '\',\'' + val.account + '\',\'' + val.amount + '\',\'' + val.crStr + '\',\'' + val.handicapName + '\');" class="btn btn-xs btn-white btn-info" type="button" >' +
                            '<i class="ace-icon fa fa-comments">补提单</i></button>';
                    }
                    if (Noteflag) {
                        tr += '<button onclick="_beforeAddRemarkOrSendMessage(' + val.id + ',\'2\'' + ',\'remark\'' + ');" class="btn btn-xs btn-white btn-info" type="button" >' +
                            '<i class="ace-icon fa fa-comments">备注</i></button>';
                    }
                    tr += '</td>'
                        + '</tr>';
                    counts += 1;
                    amounts += val.amount;
                }
                $("#COPY" + account).find('#tbody_banklog').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="1">小计：' + counts + '</td>'
                    + '<td bgcolor="#579EC8" style="color:white;">' + amounts + '</td>'
                    + '<td colspan="5"></td>'
                    + '</tr>';
                $("#COPY" + account).find('#tbody_banklog').append(trs);
                var trn = '<tr>'
                    + '<td colspan="1">总计：' + jsonObject.data.bankLogDataToPage.totalElements + '</td>'
                    + '<td bgcolor="#D6487E" style="color:white;">' + jsonObject.data.bankLogTotal[0] + '</td>'
                    + '<td colspan="5"></td>'
                    + '</tr>';
                $("#COPY" + account).find('#tbody_banklog').append(trn);
            } else {
                $("#COPY" + account).find('#tbody_banklog').empty().html('<tr></tr>');
            }
            $("[data-toggle='popover']").popover();
            //分页初始化
            showPading(jsonObject.data.bankLogDataToPage, "COPY" + account + " #banklog_footPage", queryMBAndInvoice, null, null, null, account);
        }
    });
}


/**补提单信息*/
function _addIncomeRequest(id, account, amount, createTime, handicap) {
    $('#bankLogIdToReBuild').val(id);
    $('#createTime').val(createTime);
    $('#handicap').val(handicap);
    $('#makeUpMemberAccount').val('');
    $('#makeUpAmount').val(amount).prop('readonly', 'readonly');
    $('#makeUpAccount').val(account).prop('readonly', 'readonly');
    $('#makeUpRemark').val('');
    $('#makeUpFlow').modal('show');
}

/**确定补提单信息*/
function _confirmAddIncomeRequest() {
    var memberAccount = $('#makeUpMemberAccount').val();
    var makeUpAmount = $('#makeUpAmount').val();
    var makeUpAccount = $('#makeUpAccount').val();
    var makeUpRemark = $('#makeUpRemark').val();
    var handicap = $('#handicap').val();
    var createTime = $('#createTime').val();
    if (!memberAccount) {
        $('#makeUpPrompt').text("请填写会员账号！").show(10).delay(1000).hide(10);
        return false;
    }
    if (!makeUpAmount) {
        $('#makeUpPrompt').text("请填写存款金额！").show(10).delay(1000).hide(10);
        return false;
    }
    if (!makeUpAccount) {
        $('#makeUpPrompt').text("请填写公司收款账号！").show(10).delay(1000).hide(10);
        return false;
    }
    if (!makeUpRemark) {
        $('#makeUpPrompt').text("请填写备注！").show(10).delay(1000).hide(10);
        return false;
    }
    var bankLogId = $('#bankLogIdToReBuild').val();
    var data = {
        "memberAccount": memberAccount, "amount": makeUpAmount, "account": makeUpAccount,
        "remark": makeUpRemark, "createTime": createTime, "handicap": handicap, "bankLogId": bankLogId
    };
    $.ajax({
        type: 'post',
        url: '/r/IncomeAuditWechatIn/generateWecahtRequestOrder',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res && res.status == 1) {
                $('#makeUpFlow').modal('hide');
                $.gritter.add({
                    time: 500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                queryMatchingWechat();
            } else {
                $.gritter.add({
                    time: '',
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            $('#bankLogIdToReBuild').val('');
        }
    });
}


/**
 * 点击取消
 */
function _beforeCancel(incomeRequestId, handicap, orderNo, memberCode) {
    $('#cancel_remark').val('');
    $('#cancel_sysRequestId').val(incomeRequestId);
    $('#cancel_handicapId').val(handicap);
    $('#cancel_orderNo').val(orderNo);
    $('#cancel_modal').modal('show');
}

/**
 * 确定取消操作
 * @param accountId
 * @param incomeRequestId
 * @private
 */
function _confirmConcel() {
    var remark = $('#cancel_remark').val();
    var incomeRequestId = $('#cancel_sysRequestId').val();
    var handicap = $('#cancel_handicapId').val();
    var orderNo = $('#cancel_orderNo').val();
    if (!remark) {
        $('#cancel_remarkPrompt').show(10).delay(1000).hide(10);
        return;
    }
    bootbox.confirm('是否确定取消?', function (res) {
        if (res) {
            _executeCancelOrConceal(incomeRequestId, handicap, orderNo, 'cancel', remark);
        }
    });
}

/**
 * 隐藏操作
 * @param accountId
 * @param incomeRequestId
 * @private
 */
function _beforeConceal(incomeRequestId) {
    $('#operate_type').val('');
    bootbox.confirm('是否确定隐藏？', function (res) {
        if (res) {
            _executeCancelOrConceal(incomeRequestId, null, null, 'conceal', null);
        }
    });
}

/**
 * 取消操作 隐藏操作 确定
 * @param id
 * @param type 只是用来区分是取消 还是隐藏 并非作为参数传到后台
 * @returns {boolean}
 * @private
 */
function _executeCancelOrConceal(id, handicap, orderNo, type, remark, memberCode) {
    $.ajax({
        type: 'post',
        url: '/r/IncomeAuditWechatIn/reject2Platform',
        async: false,
        data: {
            "incomeRequestId": id,
            "remark": remark,
            "type": type,
            "orderNo": orderNo,
            "handicap": handicap,
        },
        dataType: 'json',
        success: function (res) {
            if (res.status == 1 && type == 'cancel') {
                $('#cancel_modal').modal('hide');
            }
            $.gritter.add({
                time: '',
                class_name: '',
                title: '系统消息',
                text: res.message,
                sticky: false,
                image: '../images/message.png'
            });
        },
        complete: function (res) {
            if (res.status == 200) {
                queryMatchingWechat();//刷新 提单记录
            }
        },
        error: function (res) {
            $.gritter.add({
                time: '',
                class_name: '',
                title: '系统消息',
                text: res.message,
                sticky: false,
                image: '../images/message.png'
            });
        }
    });
}

function _beforeAddRemarkOrSendMessage(id, type, operate) {
    $('#incomeType_customer').val(type);
    $('#operateType_customer').val(operate);
    $('#customer_remark').val('');
    if (type == '1') {
        $('#sysRequestId_customer').val(id);
    }
    if (type == '2' || type == '3') {
        $('#bankFlowId_customer').val(id);
    }
    $('input[name="selectEvent1"]').prop('checked', false);

    if (operate == 'deal') {
        $('.selectEvent').show();
    } else {
        $('.selectEvent').hide();
    }
    executeEventType = operate;
    $('#customer_modal').modal('show');
}

function _customerExecute() {
    var eventValue = '';
    if (executeEventType == 'deal') {
        eventValue = $('input[name="selectEvent1"]:checked').val();
        if (!eventValue) {
            $('#remarkPrompt_customer').empty().text('请选择"已处理"或者"手续费"').show(10).delay(1000).hide(10);
            return false;
        }
    } else {
        $('#remarkPrompt_customer').empty().text('请填写备注再提交');
    }
    if (!$.trim($('#customer_remark').val())) {
        $('#remarkPrompt_customer').empty().text('请填写备注再提交').show(10).delay(1000).hide(10);
        return false;
    }
    var type = $('#incomeType_customer').val();//区分 是订单操作1 还是流水操作2
    var operate = $('#operateType_customer').val();//区分 是发消息sendMsg 还是添加备注remark
    var data = '';
    var url = '';
    if (type == 1) {
        if (operate == 'sendMsg') {
            //公司入款 发消息
            data = {
                'id': $('#sysRequestId_customer').val(),
                'accountId': accountIdWhole,
                'message': $('#customer_remark').val()
            };
            url = '/r/IncomeAuditWechatIn/customerSendMsg';
        }
        if (operate == 'remark') {
            //公司入款 添加备注
            data = {'id': $('#sysRequestId_customer').val(), 'remark': $('#customer_remark').val(), 'type': 'invoice'};
            url = '/r/IncomeAuditWechatIn/customerAddRemark';
        }
    }
    if (type == 2 || type == 3) {
        if (operate == 'sendMsg') {
            //银行流水 发消息
            data = {
                'id': $('#bankFlowId_customer').val(),
                'accountId': accountIdWhole,
                'message': $('#customer_remark').val()
            };
            url = '/r/IncomeAuditWechatIn/customerSendMsg';
        }
        if (operate == 'remark') {
            //银行流水 添加备注
            data = {'id': $('#bankFlowId_customer').val(), 'remark': $('#customer_remark').val(), 'type': 'wechatlog'};
            url = '/r/IncomeAuditWechatIn/customerAddRemark';
        }
        if (operate == 'deal') {
            //银行流水 已处理 手续费
            data = {
                'bankLogId': $('#bankFlowId_customer').val(),
                'remark': $.trim($('#customer_remark').val()),
                'status': eventValue
            };
            url = '/r/IncomeAuditWechatIn/doDisposedFee';
        }
    }
    $.ajax({
        type: 'post',
        url: url,
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                $('#customer_modal').modal('hide');
                if (res.status == 1) {
                    $.gritter.add({
                        time: 500,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                    if (type == 1 && operate == 'remark') {
                        //刷新公司入款 0
                        queryMatchingWechat();
                    }
                    if (type == 2 && (operate == 'deal' || operate == 'remark')) {
                        //刷新银行流水 1
                        queryMatchingWechat();
                    }
                    if (type == 3 && (operate == 'deal' || operate == 'remark')) {
                        //刷新未认领流水
                        queryUnClaim();
                    }
                }
            }
        }
    });
}


var wechatIds = new Array()
function rememberIds(id) {
    if (wechatIds.indexOf(id) < 0) {
        wechatIds.push(id);
    }
}

//某一行的复选框点击事件
function _selectInputRadioForIncome(event, obj) {
    disabledEventPropagation(event);
    if ($(obj).attr('click_value') == '1') {
        $(obj).attr('click_value', '2');
        $(obj).prop('checked', '');
    } else {
        //选中
        $(obj).attr('click_value', '1');
        $(obj).prop('checked', 'checked');

    }
}

function _checkBankFlowTr(obj) {
    var checkedFlag = $(obj).find('input').prop('checked');
    if (checkedFlag) {
        $(obj).find('input').attr('click_value', '1');
        $(obj).find('input').prop('checked', false);
    } else {
        $(obj).find('input').attr('click_value', '2');
        $(obj).find('input').prop('checked', true);
    }
}

function _checkSysOrderTr(obj) {
    var checkedFlag = $(obj).find('input').prop('checked');
    if (checkedFlag) {
        $(obj).find('input').attr('click_value', '1');
        $(obj).find('input').prop('checked', false);
    } else {
        $(obj).find('input').attr('click_value', '2');
        $(obj).find('input').prop('checked', true);
    }
}

/**
 * 匹配按钮
 * @private
 */
function _beforeMatch(obj) {
    $('input[name="matchRemark"]').val('');
    if (!$('input:radio[name="sysRequestId"]:checked').val()) {
        //$('#matchPrompt').text('请选择要匹配的提单记录').show().delay('slow').fadeOut();
        $.gritter.add({
            time: 1000,
            class_name: '',
            title: '系统消息',
            text: "请选择要匹配的提单记录",
            sticky: false,
            image: '../images/message.png'
        });
        return false;
    }
    if (!$('input:radio[name="bankFlowId"]:checked').val()) {
        //$('#matchPrompt').text('请选择要匹配的银行流水').show().delay('slow').fadeOut();
        $.gritter.add({
            time: 1000,
            class_name: '',
            title: '系统消息',
            text: "请选择要匹配的银行流水",
            sticky: false,
            image: '../images/message.png'
        });
        return false;
    }

    //提单金额
    var incomeAmount = $('input:radio[name="sysRequestId"]:checked').parent().parent().find('td[id="sysAmount"]').text();
    var sys_orderNo = $('input:radio[name="sysRequestId"]:checked').parent().parent().find('td[id="sysOrderNo"]').text();
    var incomeRequestTime = $('input:radio[name="sysRequestId"]:checked').parent().parent().find('td[id="sysCreateTime"]').text();
    var accountNoWhole = $('input:radio[name="sysRequestId"]:checked').parent().parent().find('td[id="sysAccount"]').text();
    //流水金额
    var bankAmount = $('input:radio[name="bankFlowId"]:checked').parent().parent().find('td[id="bankAmount"]').text();
    var balanceGap = parseFloat((parseFloat(incomeAmount) - parseFloat(bankAmount))).toFixed(3);
    var bankLogSummary = $('input:radio[name="bankFlowId"]:checked').parent().parent().find('td[id="bankSummary"]').find('a').text();
    var bankLogCatchTime = $('input:radio[name="bankFlowId"]:checked').parent().parent().find('td[id="bankTradingTime"]').text();
    //匹配订单 和流水 id
    $('#sysRequestId_match').val($('input:radio[name="sysRequestId"]:checked').val());
    $('#bankFlowId_match').val($('input:radio[name="bankFlowId"]:checked').val());
    $('#accountId_match').val($('input:radio[name="sysRequestId"]:checked').parent().parent().find('td[id="sysAccountId"]').text());
    //流水模态框
    $('#bankFlowBody_amount').html(bankAmount);
    $('#bankFlowBody_flowNo').html(bankLogSummary);//没有交易流水号,显示摘要
    $('#bankFlowBody_time').html(bankLogCatchTime);
    //提单模态框
    $('#sysRequestBody_amount').html(incomeAmount);
    $('#sysRequestBody_orderNo').html(sys_orderNo);
    $('#sysRequestBody_time').html(incomeRequestTime);
    if (balanceGap != 0) {
        balanceGap = balanceGap > 0 ? balanceGap : balanceGap * -1;
        $('#balanceGapPrompt').show();
        //$('#inconsistentAmountMatchInfo1').show();
        $('#inconsistentAmountMatchInfo2').show();
        $('#inconsistentAmountMatchInfo3').show();
        // $('#inconsistentAmountMatchInfo4').show();
        $('#commonMatchInfo').hide();
        //(bankAmount<=100 && (balanceGap/bankAmount > sysSetting.INCOME_PERCENT))|| (bankAmount>100 && balanceGap >100)
        if (parseFloat(balanceGap) >= parseFloat(sysSetting.INCOME_BALANCE)) {
            $('#matchPrompt').text('差额太大请重新选择').show(10).delay(5000).fadeOut();
            return false;
        } else {
            var depositor = $('input:radio[name="sysRequestId"]:checked').parent().parent().find('td[id="sysMemberName"]').text();
            $('#inconsistentAmountMatchAmount').val(bankAmount);//流水金额
            $('#inconsistentAmountMatchName').val(depositor);//存款人姓名
            $('#inconsistentAmountMatchBankAccount').val(accountNoWhole);//公司收款账号
            $('#inconsistentAmountMatchBalanceGap').val(balanceGap);//差额
            //$('#inconsistentAmountMatchMemberAccount').val(memberAccount);//会员账号
        }
    } else {
        //$('#balanceGap').val(balanceGap);
        $('#balanceGapPrompt').hide();
        $('#inconsistentAmountMatchAmount').val('');//流水金额
        $('#inconsistentAmountMatchName').val('');//存款人姓名
        $('#inconsistentAmountMatchBankAccount').val('');//公司收款账号
        $('#inconsistentAmountMatchRemark').val('');//金额不一致匹配备注
        $('#inconsistentAmountMatchBalanceGap').val('');//差额
        $('#inconsistentAmountMatchMemberAccount').val('');//会员账号
        $('#inconsistentAmountMatchInfo1').hide();
        $('#inconsistentAmountMatchInfo2').hide();
        $('#inconsistentAmountMatchInfo3').hide();
        $('#inconsistentAmountMatchInfo4').hide();
        $('#commonMatchInfo').show();
    }
    $('#toMatchInfo').modal('show');

}


/**
 * 执行匹配操作
 * @param id
 */
function _executeMatch() {
    var matchRemark = '';
    var bankFlowId = $('#bankFlowId_match').val();
    var sysRequestId = $('#sysRequestId_match').val();
    var companyAccount = $('#inconsistentAmountMatchBankAccount').val();//公司收款账号
    var wechatId = $('#accountId_match').val();
    if (!companyAccount) {
        //金额一致匹配 companyAccount值不存在
        matchRemark = $('input[name="matchRemark"]').val();
        if (!matchRemark) {
            $('#remarkPrompt').empty().text('请填写备注再提交').show(100).delay(2000).hide(100);
            return false;
        }
    } else {
        matchRemark = $('#inconsistentAmountMatchRemark').val();
        if (!matchRemark) {
            $('#remarkPrompt').empty().text('请填写备注').show(100).delay(2000).hide(100);
            return false;
        }
    }
    $.ajax({
        type: "post",
        url: "/r/IncomeAuditWechatIn/wechatInMatch",
        data: {
            "sysRequestId": sysRequestId,
            "bankFlowId": bankFlowId,
            "matchRemark": matchRemark
        },
        dataType: 'json',
        success: function (res) {
            if (res && res.status == 1) {
                $.gritter.add({
                    time: 500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                $('#toMatchInfo').modal('hide');
                queryMatchingWechat();//刷新
                $('input[name="matchRemark"]').val('');
                $('#inconsistentAmountMatchAmount').val('');//流水金额
                $('#inconsistentAmountMatchName').val('');//存款人姓名
                $('#inconsistentAmountMatchBankAccount').val('');//公司收款账号
                $('#inconsistentAmountMatchRemark').val('');//金额不一致匹配备注
                $('#inconsistentAmountMatchBalanceGap').val('');//差额
                $('#inconsistentAmountMatchMemberAccount').val('');//会员账号
                setTimeout("wechatMatching_hide(" + wechatId + ")", 200);
                setTimeout("wechatMatching_show(" + wechatId + ")", 200);
            } else {
                $.gritter.add({
                    time: 500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
        }
    });
}

var wechatMatching_show = function (account) {
    $("#i" + account).removeClass("fa-angle-double-down").addClass("fa-angle-double-up");
    var $tr = $("#wechatTr" + account);
    //事件切换
    $tr.attr('onclick', '').unbind("click").click(function () {
        wechatMatching_hide(account);
    });
    //拼接DIV
    if (wechatIds.indexOf(account) < 0) {
        var $td = $("<td colspan='8' style='padding:10px !important;'></td>").append($(".table-detail").clone().attr("id", "COPY" + account).removeClass("table-detail").removeClass("hide"));
        $tr.after($("<tr class='wechat_matchingTr' id='wechat_matching" + account + "' ></tr>").append($td));
        queryMBAndInvoice(account);
    }
}

var wechatMatching_hide = function (account) {
    if (wechatIds.indexOf(account) >= 0) {
        wechatIds.splice(wechatIds.indexOf(account), 1);
    }
    $("#i" + account).removeClass("fa-angle-double-up").addClass("fa-angle-double-down");
    var $tr = $("#wechatTr" + account);
    //事件切换
    $tr.attr('onclick', '').unbind("click").click(function () {
        wechatMatching_show(account);
    });
    $("#wechat_matching" + account).remove();
}

//查询未匹配的单
var queryNotMatched = function () {
    //当前页码
    var CurPage = $("#wechatNotMatched_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    //获取盘口
    var handicap = $("#handicap_wechatNotMatched").val();
    if (handicap == "" || handicap == null || handicap == 0) {
        handicap = 0;
    }
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("input[name='timeScope_wechatNotMatched']").val();
    var startAndEndTimeToArray = new Array();
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    startAndEndTimeToArray = startAndEndTimeToArray.toString();
    //会员号
    var member = $("#member_wechatNotMatched").val();
    //订单号
    var orderNo = $("#orderNo_wechatNotMatched").val();
    //开始金额
    var fromAmount = $("#fromMoney_wechatNotMatched").val();
    //结束金额
    var toAmount = $("#toMoney_wechatNotMatched").val();
    //存款人
    var payer = $("#payer_wechatNotMatched").val();
    //微信号
    var wechatNumber = $("#payer_wechatNo_wechatNotMatched").val();
    $.ajax({
        type: "post",
        url: "/r/IncomeAuditWechatIn/findWechatMatched",
        data: {
            "pageNo": CurPage,
            "handicap": handicap,
            "startAndEndTimeToArray": startAndEndTimeToArray,
            "member": member,
            "orderNo": orderNo,
            "fromAmount": fromAmount,
            "toAmount": toAmount,
            "payer": payer,
            "wechatNumber": wechatNumber,
            "status": 0,
            "pageSize": $.session.get('initPageSize')
        },
        dataType: 'json',
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0) {
                var tr = '';
                var counts = 0;
                var amounts = 0;
                for (var index in jsonObject.data.arrlist) {
                    var val = jsonObject.data.arrlist[index];
                    tr += '<tr>'
                        + '<td>' + handicaps[val.handicap].name + '</td>'
                        + '<td>' + val.level + '</td>'
                        + '<td>' + val.member + '</td>'
                        + '<td>' + val.orderNo + '</td>'
                        + '<td>' + val.amount + '</td>'
                        + '<td>'
                        + hideAccountAll(val.account)
                        + '</td>'
                        + '<td>' + val.crTime + '</td>'
                    if (_checkObj(val.remark)) {
                        if (_checkObj(val.remark).length > 10) {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark).substring(0, 10) + "..."
                                + '</a></td>';
                        } else {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark)
                                + '</a></td>';
                        }

                    } else {
                        tr += '<td></td>';
                    }
                    +'</tr>';
                    counts += 1;
                    amounts += val.amount;
                }
                $('#tbody_wechatNotMatched').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="4">小计：' + counts + '</td>'
                    + '<td bgcolor="#579EC8" style="color:white;">' + amounts + '</td>'
                    + '<td colspan="5"></td>'
                    + '</tr>';
                $('#tbody_wechatNotMatched').append(trs);
                var trn = '<tr>'
                    + '<td colspan="4">总计：' + jsonObject.data.page.totalElements + '</td>'
                    + '<td bgcolor="#D6487E" style="color:white;">' + jsonObject.data.total[0] + '</td>'
                    + '<td colspan="5"></td>'
                    + '</tr>';
                $('#tbody_wechatNotMatched').append(trn);
            } else {
                $('#tbody_wechatNotMatched').empty().html('<tr></tr>');
            }
            $("[data-toggle='popover']").popover();
            //分页初始化
            showPading(jsonObject.data.page, "wechatNotMatched_footPage", queryNotMatched);
        }
    });
}


//查询已经匹配的单
var queryMatched = function () {
    if ($('#handicap_wechatMatched option').length <= 0)
        inithancipad("wechatMatched");
    //当前页码
    var CurPage = $("#wechatMatched_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    //获取盘口
    var handicap = $("#handicap_wechatMatched").val();
    if (handicap == "" || handicap == null || handicap == 0) {
        handicap = 0;
    }
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("input[name='timeScope_wechatMatched']").val();
    var startAndEndTimeToArray = new Array();
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    startAndEndTimeToArray = startAndEndTimeToArray.toString();
    //会员号
    var member = $("#member_wechatMatched").val();
    //订单号
    var orderNo = $("#orderNo_wechatMatched").val();
    //开始金额
    var fromAmount = $("#fromMoney_wechatMatched").val();
    //结束金额
    var toAmount = $("#toMoney_wechatMatched").val();
    //存款人
    var payer = $("#payer_wechatMatched").val();
    //微信号
    var wechatNumber = $("#payer_wechatNo_wechatMatched").val();
    $.ajax({
        type: "post",
        url: "/r/IncomeAuditWechatIn/findWechatMatched",
        data: {
            "pageNo": CurPage,
            "handicap": handicap,
            "startAndEndTimeToArray": startAndEndTimeToArray,
            "member": member,
            "orderNo": orderNo,
            "fromAmount": fromAmount,
            "toAmount": toAmount,
            "payer": payer,
            "wechatNumber": wechatNumber,
            "status": 1,
            "pageSize": $.session.get('initPageSize')
        },
        dataType: 'json',
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0) {
                var tr = '';
                var counts = 0;
                var amounts = 0;
                for (var index in jsonObject.data.arrlist) {
                    var val = jsonObject.data.arrlist[index];
                    tr += '<tr>'
                        + '<td>' + handicaps[val.handicap].name + '</td>'
                        + '<td>' + val.level + '</td>'
                        + '<td>' + val.member + '</td>'
                        + '<td>' + val.orderNo + '</td>'
                        + '<td>' + val.amount + '</td>'
                        + '<td>'
                        + hideAccountAll(val.account)
                        + '</td>'
                        + '<td>' + val.crTime + '</td>'
                        + '<td>' + val.akTime + '</td>'
                        + '<td>' + (formatDuring(new Date(val.akTime).getTime() - new Date(val.crTime).getTime())) + '</td>'
                    if (_checkObj(val.remark)) {
                        if (_checkObj(val.remark).length > 10) {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark).substring(0, 10) + "..."
                                + '</a></td>';
                        } else {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark)
                                + '</a></td>';
                        }

                    } else {
                        tr += '<td></td>';
                    }
                    +'</tr>';
                    counts += 1;
                    amounts += val.amount;
                }
                $('#tbody_wechatMatched').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="4">小计：' + counts + '</td>'
                    + '<td bgcolor="#579EC8" style="color:white;">' + amounts + '</td>'
                    + '<td colspan="5"></td>'
                    + '</tr>';
                $('#tbody_wechatMatched').append(trs);
                var trn = '<tr>'
                    + '<td colspan="4">总计：' + jsonObject.data.page.totalElements + '</td>'
                    + '<td bgcolor="#D6487E" style="color:white;">' + jsonObject.data.total[0] + '</td>'
                    + '<td colspan="5"></td>'
                    + '</tr>';
                $('#tbody_wechatMatched').append(trn);
            } else {
                $('#tbody_wechatMatched').empty().html('<tr></tr>');
            }
            $("[data-toggle='popover']").popover();
            //分页初始化
            showPading(jsonObject.data.page, "wechatMatched_footPage", queryMatched);
        }
    });
}

//查询已经取消的单
var queryCanceled = function () {
    if ($('#handicap_wetchatCanceled option').length <= 0)
        inithancipad("wetchatCanceled");
    //当前页码
    var CurPage = $("#wetchatCanceled_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    //获取盘口
    var handicap = $("#handicap_wetchatCanceled").val();
    if (handicap == "" || handicap == null || handicap == 0) {
        handicap = 0;
    }
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("input[name='timeScope_wetchatCanceled']").val();
    var startAndEndTimeToArray = new Array();
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    startAndEndTimeToArray = startAndEndTimeToArray.toString();
    //会员号
    var member = $("#member_wetchatCanceled").val();
    //订单号
    var orderNo = $("#orderNo_wetchatCanceled").val();
    //开始金额
    var fromAmount = $("#fromMoney_wetchatCanceled").val();
    //结束金额
    var toAmount = $("#toMoney_wetchatCanceled").val();
    $.ajax({
        type: "post",
        url: "/r/IncomeAuditWechatIn/findWechatCanceled",
        data: {
            "pageNo": CurPage,
            "handicap": handicap,
            "startAndEndTimeToArray": startAndEndTimeToArray,
            "member": member,
            "orderNo": orderNo,
            "fromAmount": fromAmount,
            "toAmount": toAmount,
            "pageSize": $.session.get('initPageSize')
        },
        dataType: 'json',
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0) {
                var tr = '';
                var counts = 0;
                var amounts = 0;
                for (var index in jsonObject.data.arrlist) {
                    var val = jsonObject.data.arrlist[index];
                    tr += '<tr>'
                        + '<td>' + handicaps[val.handicap].name + '</td>'
                        + '<td>' + val.level + '</td>'
                        + '<td>'
                        + hideAccountAll(val.account)
                        + '</td>'
                        + '<td>' + val.member + '</td>'
                        + '<td>' + val.orderNo + '</td>'
                        + '<td>' + val.amount + '</td>'
                        + '<td>' + val.crTime + '</td>'
                        + '<td>' + val.akTime + '</td>'
                    if (_checkObj(val.remark)) {
                        if (_checkObj(val.remark).length > 10) {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark).substring(0, 10) + "..."
                                + '</a></td>';
                        } else {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark)
                                + '</a></td>';
                        }

                    } else {
                        tr += '<td></td>';
                    }
                    +'</tr>';
                    counts += 1;
                    amounts += val.amount;
                }
                $('#tbody_WetchatCanceled').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="5">小计：' + counts + '</td>'
                    + '<td bgcolor="#579EC8" style="color:white;">' + amounts + '</td>'
                    + '<td colspan="3"></td>'
                    + '</tr>';
                $('#tbody_WetchatCanceled').append(trs);
                var trn = '<tr>'
                    + '<td colspan="5">总计：' + jsonObject.data.page.totalElements + '</td>'
                    + '<td bgcolor="#D6487E" style="color:white;">' + jsonObject.data.total[0] + '</td>'
                    + '<td colspan="3"></td>'
                    + '</tr>';
                $('#tbody_WetchatCanceled').append(trn);
            } else {
                $('#tbody_WetchatCanceled').empty().html('<tr></tr>');
            }
            $("[data-toggle='popover']").popover();
            //分页初始化
            showPading(jsonObject.data.page, "wetchatCanceled_footPage", queryCanceled);
        }
    });
}

//查询未认领的流水
var queryUnClaim = function () {
    if ($('#handicap_wetchatUnClaim option').length <= 0)
        inithancipad("wetchatUnClaim");
    //当前页码
    var CurPage = $("#wetchatUnClaim_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    //获取盘口
    var handicap = $("#handicap_wetchatUnClaim").val();
    if (handicap == "" || handicap == null || handicap == 0) {
        handicap = 0;
    }
    //获取时间段
    //日期 条件封装
    var startAndEndTime = $("input[name='timeScope_wetchatUnClaim']").val();
    var startAndEndTimeToArray = new Array();
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    startAndEndTimeToArray = startAndEndTimeToArray.toString();
    //存款人
    var member = $("#member_wetchatUnClaim").val();
    //微信号
    var wechatNo = $("#payer_wechatNo_wetchatUnClaim").val();
    //开始金额
    var fromAmount = $("#fromMoney_wetchatUnClaim").val();
    //结束金额
    var toAmount = $("#toMoney_wetchatUnClaim").val();
    $.ajax({
        type: "post",
        url: "/r/IncomeAuditWechatIn/findWechatUnClaim",
        data: {
            "pageNo": CurPage,
            "handicap": handicap,
            "startAndEndTimeToArray": startAndEndTimeToArray,
            "member": member,
            "wechatNo": wechatNo,
            "fromAmount": fromAmount,
            "toAmount": toAmount,
            "pageSize": $.session.get('initPageSize')
        },
        dataType: 'json',
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0) {
                var tr = '';
                var counts = 0;
                var amounts = 0;
                for (var index in jsonObject.data.arrlist) {
                    var val = jsonObject.data.arrlist[index];
                    tr += '<tr>'
                        + '<td>' + handicaps[val.handicapName].name + '</td>'
                        + '<td>'
                        + hideAccountAll(val.account)
                        + '</td>'
                        + '<td>' + val.amount + '</td>'
                        + '<td>' + val.trStr + '</td>'
                        + '<td>' + val.crStr + '</td>'
                    if (_checkObj(val.remark)) {
                        if (_checkObj(val.remark).length > 10) {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark).substring(0, 10) + "..."
                                + '</a></td>';
                        } else {
                            tr +=
                                '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + _checkObj(val.remark) + '">'
                                + _checkObj(val.remark)
                                + '</a></td>';
                        }

                    } else {
                        tr += '<td></td>';
                    }
                    tr += '<td></button><button onclick="_beforeAddRemarkOrSendMessage(' + val.id + ',\'3\'' + ',\'remark\'' + ');" class="btn btn-xs btn-white btn-info" type="button" >' +
                        '<i class="ace-icon fa fa-comments">备注</i>' +
                        '</button></td>'
                        + '</tr>';
                    counts += 1;
                    amounts += val.amount;
                }
                $('#tbody_wetchatUnClaim').empty().html(tr);
                var trs = '<tr>'
                    + '<td colspan="2">小计：' + counts + '</td>'
                    + '<td bgcolor="#579EC8" style="color:white;">' + amounts + '</td>'
                    + '<td colspan="4"></td>'
                    + '</tr>';
                $('#tbody_wetchatUnClaim').append(trs);
                var trn = '<tr>'
                    + '<td colspan="2">总计：' + jsonObject.data.page.totalElements + '</td>'
                    + '<td bgcolor="#D6487E" style="color:white;">' + jsonObject.data.total[0] + '</td>'
                    + '<td colspan="4"></td>'
                    + '</tr>';
                $('#tbody_wetchatUnClaim').append(trn);
            } else {
                $('#tbody_wetchatUnClaim').empty().html('<tr></tr>');
            }
            $("[data-toggle='popover']").popover();
            //分页初始化
            showPading(jsonObject.data.page, "wetchatUnClaim_footPage", queryUnClaim);
        }
    });
}

var sertype = "Matching";
function change(type) {
    sertype = type;
}
function _addTimeSelect() {
    var opt = '<option  value="0" selected="selected">不刷新</option><option   value="15">15秒</option><option  value="30">30秒</option>' +
        '<option  value="60">60秒</option><option  value="120">120秒</option><option  value="180">180秒</option>';
    $('#autoUpdateTimeWechatIn').empty().append(opt);
}
$('#autoUpdateTimeWechatIn').unbind().bind('change', function () {
    _searchIncomeAuditWechatIn();
});

inithancipad("wechat_total");$('#freshWechatInLi').show();
_addTimeSelect();_searchIncomeAuditWechatIn();
initTime();queryMatchingWechat();