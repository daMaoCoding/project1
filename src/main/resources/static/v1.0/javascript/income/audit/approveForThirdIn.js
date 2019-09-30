//定时循环执行
var third_timer = null;
var third_searchType = null;
var lookUpCheckedThirdAmount = false;
$.each(ContentRight['Income:*'], function (name, value) {
    if (name == 'Income:currentpageSum:*') {
        incomeCurrentPageSum=true;
    }
    if (name =='Income:allRecordSum:*'){
        incomeAllRecordSum=true;
    }
});
function _changSearch() {
    if (third_timer != null) {
        clearInterval(third_timer);
    }
    if ($("#changeFlushTime").val() != '不刷新') {
        third_timer = setInterval(function () {
            _searchMatchedByFilter();
        }, $("#changeFlushTime").val());
    }
}
//绑定按键事件，回车查询数据
$('#tab_matched').bind('keypress', _keyDownEvent);
function _keyDownEvent(e) {
    var evt = e || window.event;
    var keyCode = evt.keyCode || evt.which || evt.charCode;
    if (keyCode == 13) {
        _searchMatchedByFilter();
    }
}
function setSearchType(type) {
    third_searchType = type;
    _datePickerForAll($("input.date-range-picker"));
    _initialSelectChosen(third_searchType);
    _initialHandicap(type);
    $('#toAccount_'+third_searchType).change(function () {
        if($(this).val()!='全部' && $(this).val()!='请选择'){
            _searchMatchedByFilter();
        }
    });
    if (type=='thirdFlows'){
        _searchThirdFlow();
    } else
        _searchMatchedByFilter();
}
/**
 * 查询 已对账 未对账 第三方入款
 */
function _searchMatchedByFilter() {
    var type = third_searchType;
    var handicap = null;
    if ($("#handicap_" + type).val() && $("#handicap_" + type).val() != '请选择'&& $("#handicap_" + type).val() != '全部') {
        handicap = $("#handicap_" + type).val();
    }else{
        var handicapArray = [];
        $('#handicap_' + type).find('option').each(function () {
            if(this.value!='请选择'){
                handicapArray.push(this.value);
            }
        });
        handicap = handicapArray.length>0?handicapArray.toString():'';
    }
    var level = null;
    if ($("#level_" + type).val() && $("#level_" + type).val() != '请选择' && $("#handicap_" + type).val() != '全部') {
        level = $("#level_" + type).val();
    }
    var toAccount = '';//第三方商号
    if ($("#toAccount_" + type).val() && $("#toAccount_" + type).val()!='请选择' && $("#handicap_" + type).val() != '全部') {
        if ($("#toAccount_" + type).val().indexOf('%') >= 0)
            toAccount = $.trim($("#toAccount_" + type).val().replace(new RegExp(/%/g), '?'));
        else
            toAccount = $.trim($("#toAccount_" + type).val());
    }
    var member = '';//会员账号
    if ($("#member_" + type).val()) {
        if ($("#member_" + type).val().indexOf('%') >= 0)
            member = $.trim($("#member_" + type).val().replace(new RegExp(/%/g), '?'));
        else
            member = $.trim($("#member_" + type).val());
    }
    var thirdOrderNo = '';
    if ($('#thirdOrderNo_' + type).val()) {
        if ($('#thirdOrderNo_' + type).val().indexOf('%') >= 0)
            thirdOrderNo = $.trim($('#thirdOrderNo_' + type).val().replace(new RegExp(/%/g), '?'));
        else
            thirdOrderNo = $.trim($('#thirdOrderNo_' + type).val());
    }
    var fromMoney = null;
    if ($('#fromMoney_' + type).val()) {
        fromMoney = $('#fromMoney_' + type).val();
    }
    var toMoney = null;
    if ($('#toMoney_' + type).val()) {
        toMoney = $('#toMoney_' + type).val();
    }
    var startAndEnd = $("#startAndEndTime_" + type).val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    }else {
        startTime = _getDefaultTime()[0];
        endTime = _getDefaultTime()[1];
    }
    var CurPage = $("#" + type + "_pageFoot").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var status = '';
    if (type == 'matched') {
        status = 1;
    }
    if (type == 'unmatch') {
        status = 0;
    }
    var data = {
        "status": status, "handicap": handicap, "level": level, "member": member,  "thirdOrderNo": thirdOrderNo,
        "toAccount": toAccount, "fromMoney": fromMoney, "toMoney": toMoney,
        "startTime": startTime, "endTime": endTime, "pageNo": CurPage, "pageSize": $.session.get('initPageSize')
    };
    _executeSearchThird(data, type);

}
function _executeSearchThird(data, type) {
    if (data) {
        $.ajax({
            dataType: 'json',
            type: "get",
            url: "/r/income/third",
            async:false,
            data: data,
            success: function (res) {
                if (res) {
                    if (res.status == 1) {
                        var tr = '', trs = '';
                        if (res.data && res.data.length > 0) {
                            var amount = 0,fee=0,idList = [];
                            $(res.data).each(function (i, val) {
                                tr +=
                                    '<tr>' +
                                    '<td>' + _showHandicapNameByIdOrCode(val.handicapId) + '</td>' +
                                    '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>' +
                                    '<td>' + _checkObj(val.member) + '</td>' +
                                    '<td>' + _checkObj(val.orderNo) + '</td>' +
                                    '<td>' + _checkObj(val.bankName) + '</td>' +
                                    "<td>"
                                    + "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.toId + "' data-placement='auto right' data-trigger='hover'  >"
                                    + _ellipsisAccount(_checkObj(val.toAccount))
                                    + '</a></td><td>' + _checkObj(val.amount) + '</td>';
                                if (type == 'matched') {
                                    tr += '<td>' + (_checkObj(val.fee)?val.fee:0) + '</td>' ;
                                }
                                tr +=     '<td>' + timeStamp2yyyyMMddHHmmss(_checkObj(val.createTime)) + '</td>';
                                if (type == 'matched') {
                                    tr += '<td>' + timeStamp2yyyyMMddHHmmss(_checkObj(val.updateTime)) + '</td>';
                                }
                                if (type == 'unmatch') {
                                    tr +=
                                        '<td>' +
                                        '<button style="padding-top: 0px;padding-bottom: 0px" class="btn btn-xs btn-info btn-white  btn-bold " onclick="_beforeOperate(' + val.id + ',\'' + _checkObj(val.orderNo) + '\',\'' + _checkObj(val.member) + '\',' + _checkObj(val.handicapId) + ',' + _checkObj(val.toId) + ',\'compelIn\');"><i class="ace-icon fa  fa-gavel bigger-120 blue">强制入款</i></button>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
                                        '<button style="padding-top: 0px;padding-bottom: 0px" class="btn btn-xs btn-info btn-white  btn-bold " onclick="_beforeOperate(' + val.id + ',\'' + _checkObj(val.orderNo) + '\',\'' + _checkObj(val.member) + '\',' + _checkObj(val.handicapId) + ',' + _checkObj(val.toId) + ',\'cancel\');"><i class="ace-icon fa fa-trash bigger-120 red">取消</i></button>' +
                                        '</td>';
                                }
                                tr += '</tr>';
                                amount += _checkObj(val.amount);fee+=(_checkObj(val.fee)?val.fee:0) ;
                                idList.push({'id': val.toId, 'type': 'third'});
                            });
                            if(incomeCurrentPageSum){
                                trs ='<tr><td colspan="6" id="currentPageCount">小计：统计中...</td>' ;
                                trs+='<td bgcolor="#579EC8" style="color:white;">' + parseFloat(amount).toFixed(3) + '</td>' ;
                                if(type=='matched'){
                                    trs+='<td bgcolor="#579EC8" style="color:white;">' + parseFloat(fee).toFixed(3) + '</td>' ;
                                }
                                trs+='<td colspan="10"></td></tr>' ;
                            }else{
                                trs ='<tr><td colspan="15" id="currentPageCount">小计：统计中...</td></tr>' ;
                            }
                            if(incomeAllRecordSum){
                                trs+='<tr><td colspan="6" id="countThird">合计：统计中...</td>' ;
                                trs+='<td id="sumAmountThird" bgcolor="#D6487E" style="color:white;width: 150px;display: none;">统计中...</td>' ;
                                if(type=='matched'){
                                    trs+='<td id="sumFeeThird" bgcolor="#D6487E" style="color:white;width: 100px;display: none;">统计中...</td>' ;
                                }
                                trs+='<td colspan="10"></td></tr>' ;
                            }else{
                                trs+='<tr><td colspan="15" id="countThird">合计：统计中...</td></tr>' ;
                            }
                        }
                        $('#third_' + type + '_tbody').empty().html(tr).append(trs);
                        loadHover_accountInfoHover(idList);
                        showPading(res.page, type + '_pageFoot', _searchMatchedByFilter);
                        if(incomeAllRecordSum && lookUpCheckedThirdAmount){
                            _getThirdSum(data, type);
                        }
                        _getThirdCount(data, type);
                    }
                }
            }
        });
    }
}

/**
 * 强制入款 取消操作之前
 * @param type
 * @private
 */
function _beforeOperate(id, orderNo, memberCode, handicapId, toAccountId, type) {
    var data = {
        "incomeRequestId": id, "orderNo": orderNo, "memberCode": memberCode,
        "handicapId": handicapId, "accountId": toAccountId, "remark": null,  "type": type
    };
    if (type == "cancel") {
        $('input[name="third_cancel_remark"]').val(''); $('#sysRequestId_third').val(id);
        $('#handicap_third').val(handicapId); $('#orderNo_third').val(orderNo);
        $('#member_third').val($.trim(memberCode));  $('#accoutId_third').val(toAccountId);
        $('#third_cancel_modal').modal('show');
    }
    if (type == "compelIn") {
        bootbox.confirm("是否强制入款？", function (res) {
            if (res) {
                _changeThirdStatus(data);
            }
        })
    }
}
/**取消模态框 提交*/
function _beforeSummit(toAccountId, id, handicapId, orderNo, memberCode) {
    if (!$('input[name="third_cancel_remark"]').val()) {
        $('#remarkPrompt_cancel').show().delay('slow').fadeOut();
        return false;
    }
    var remark = $('input[name="third_cancel_remark"]').val();
    var data = {
        "incomeRequestId": id, "orderNo": orderNo, "memberCode": memberCode,
        "handicapId": handicapId, "accountId": toAccountId, "remark": remark, "type": "cancel"
    };
    _changeThirdStatus(data);
}
/**
 * 强制入款  取消 操作
 * @param id
 * @param type
 * @private
 */
function _changeThirdStatus(data) {
    $.ajax({
        type: 'post',
        url: '/r/income/reject2Platform',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $.gritter.add({
                        time: '',
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                    $('#third_cancel_modal').modal('hide');
                    _searchMatchedByFilter();
                }
            }
        }
    });
}
$.each(ContentRight['IncomeAuditThird:*'], function (name, value) {
    if (name == 'IncomeAuditThird:lookUpCheckedThirdAmount:*') {
        lookUpCheckedThirdAmount = true;
    }
});
/**
 * 获取总金额
 * @param data
 * @private
 */
function _getThirdSum(data, type) {
    if (data) {
        $.ajax({
            dataType: 'json',
            type: "get",
            url: "/r/income/thirdSumAmount",
            data: data,
            success: function (res) {
                if (res) {
                    if (res.status == 1) {
                        if (res.data) {
                            if(type=='matched'){
                                if(!lookUpCheckedThirdAmount){
                                    $('#third_' + type + '_tbody   #sumAmountThird').text('无权限查看');
                                }else{
                                    $('#third_' + type + '_tbody   #sumAmountThird').text(parseFloat(res.data.amountAll).toFixed(3));
                                    $('#third_' + type + '_tbody   #sumFeeThird').text(parseFloat(res.data.feeAll).toFixed(3));
                                }
                            }else{
                                $('#third_' + type + '_tbody   #sumAmountThird').text(parseFloat(res.data.amountAll).toFixed(3));
                            }
                            $('#third_' + type + '_tbody   #sumAmountThird').show();
                            if(type=='matched'){
                                $('#third_' + type + '_tbody   #sumFeeThird').show();
                            }
                        }
                    }
                }
            }
        });
    }
}
/**
 * 获取总记录数和分页
 * @param data
 * @private
 */
function _getThirdCount(data, type) {
    if (data) {
        $.ajax({
            dataType: 'json',
            type: "get",
            url: "/r/income/thirdCount",
            data: data,
            success: function (res) {
                if (res) {
                    if (res.status == 1) {
                        if (res.page) {
                            $('#third_' + type + '_tbody   #currentPageCount').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * res.page.pageNo) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1))) + '条记录');
                            $('#third_' + type + '_tbody   #countThird').empty().text('合计：' + res.page.totalElements + '条记录');
                            showPading(res.page, type + '_pageFoot', _searchMatchedByFilter);
                        }
                    }
                }
            }
        });
    }
}
/**
 * 查询第三方流水
 * @param pageNo
 * @param type
 * @private
 */
function _searchThirdFlow() {
    var level_thirdFlows = null;
    if ($('#level_thirdFlows').val() != '请选择'&&$('#level_thirdFlows').val() != '全部') {
        level_thirdFlows = $('#level_thirdFlows').val();
    }
    var handicap_thirdFlows = null;
    if ($('#handicap_thirdFlows').val() != '请选择'&&$('#handicap_thirdFlows').val() != '全部') {
        handicap_thirdFlows = $('#handicap_thirdFlows').val();
    }
    var orderNo_thirdFlows = '';
    if ($('#orderNo_thirdFlows').val()) {
        orderNo_thirdFlows = $.trim($('#orderNo_thirdFlows').val());
    }
    var channel = '';
    if ($('#channel_thirdFlows').val()) {
        channel = $.trim($('#channel_thirdFlows').val());
    }
    var startAndEnd = $('#startAndEndTime_thirdFlows').val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    }else {
        startTime = _getDefaultTime()[0];
        endTime =  _getDefaultTime()[1];
    }
    var toAccount_thirdFlows = null;
    if ($('#toAccount_thirdFlows').val() != '请选择' && $('#toAccount_thirdFlows').val() != '全部') {
        toAccount_thirdFlows = $('#toAccount_thirdFlows').val();
    }
    var fromMoney_thirdFlows = '';
    if ($('#fromMoney_thirdFlows').val()) {
        fromMoney_thirdFlows = $('#fromMoney_thirdFlows').val();
    }
    var toMoney_thirdFlows = '';
    if ($('#toMoney_thirdFlows').val()) {
        toMoney_thirdFlows = $('#toMoney_thirdFlows').val();
    }
    var CurPage = $("#thirdFlows_pageFoot").find(".Current_Page").text();
    if (!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var data = {
        "handicap": handicap_thirdFlows, "level": level_thirdFlows, "orderNo": orderNo_thirdFlows, "channel": channel,
        "account": toAccount_thirdFlows, "startMoney": fromMoney_thirdFlows, "endMoney": toMoney_thirdFlows,
        "startTime": startTime, "endTime": endTime, "pageNo": CurPage, "pageSize": $.session.get('initPageSize')
    };
    $.ajax({
        type: 'get',
        url: '/r/income/thirdLogNoCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                var tr = '',trs ='', amount = 0, fee = 0,balance = 0, idList = [];
                if (res.status == 1 && res.data && res.data.length > 0) {
                    $(res.data).each(function (i, val) {
                        tr +=
                            '<tr>' +
                            '<td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                            '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>' +
                            '<td>' + _checkObj(val.thirdBankName) + '</td>' +
                            '<td>' +
                            '<a class="bind_hover_card breakByWord" data-toggle="accountInfoHover' + val.fromAccountId + '" data-placement="auto right" data-trigger="hover"  >'
                            +_ellipsisForBankName(_checkObj(val.thirdAccount)) +
                            '</a>' +
                            '</td>' +
                            '<td>' + _checkObj(val.channel) + '</td>' +
                            '<td>' + _checkObj(val.thirdAccontBalance) + '</td>' +
                            '<td>' + _checkObj(val.amount) + '</td>' +
                            '<td>' + _checkObj(val.fee) + '</td>' +
                            '<td>' + _checkObj(val.orderNo) + '</td>' +
                            '<td>' + timeStamp2yyyyMMddHHmmss(_checkObj(val.tradingTime)) + '</td>' +
                            '<td>' + _checkObj(val.remark) + '</td>' +
                            '</tr>';
                        amount += val.amount; fee += val.fee; balance += val.thirdAccontBalance;
                        idList.push({'id': val.fromAccountId , 'type': 'ali_wechat'});
                    });
                    if(incomeCurrentPageSum){
                        trs +='<tr><td id="recordCurrentPage" colspan="5">小计：0条记录</td>' ;
                        trs+='<td bgcolor="#579EC8" id="thirdFlows_tbodyTd5">' + parseFloat(balance).toFixed(3) + '</td>' +
                            '<td bgcolor="#579EC8" id="thirdFlows_tbodyTd1">' + parseFloat(amount).toFixed(3) + '</td>' +
                            '<td bgcolor="#579EC8" id="thirdFlows_tbodyTd3">' + parseFloat(fee).toFixed(3) + '</td>' ;
                        trs+='<td colspan="10"></td></tr>' ;
                    }else{
                        trs +='<tr><td id="recordCurrentPage" colspan="15">小计：0条记录</td></tr>' ;
                    }
                    if(incomeAllRecordSum){
                        trs +='<tr><td id="recordAllPage" colspan="5">合计：0条记录</td>' ;
                        trs +='<td bgcolor="#D6487E" id="thirdFlows_tbodyTd6">统计中：...</td>' +
                            '<td bgcolor="#D6487E" id="thirdFlows_tbodyTd2">统计中：...</td>' +
                            '<td bgcolor="#D6487E" id="thirdFlows_tbodyTd4">统计中：...</td>' ;
                        trs+='<td colspan="10"></td></tr>' ;
                    }else{
                        trs +='<tr><td id="recordAllPage" colspan="15">合计：0条记录</td>' ;
                    }
                }
                $('#thirdFlows_tbody').empty().html(tr).append(trs);
                _getThirdFlowCount(data);
                if(incomeAllRecordSum){
                    _getSumThridFlow(data);
                }
                loadHover_accountInfoHover(idList);
                showPading(res.page, "thirdFlows_pageFoot", _searchThirdFlow);
                $('td').attr('style', 'text-align:center');
                $('#thirdFlows_tbodyTd1').attr('style', 'color:white;text-align:center;width:130px;');
                $('#thirdFlows_tbodyTd2').attr('style', 'color:white;text-align:center;width:130px;');
                $('#thirdFlows_tbodyTd3').attr('style', 'color:white;text-align:center;width:130px;');
                $('#thirdFlows_tbodyTd4').attr('style', 'color:white;text-align:center;width:130px;');
                $('#thirdFlows_tbodyTd5').attr('style', 'color:white;text-align:center;width:130px;');
                $('#thirdFlows_tbodyTd6').attr('style', 'color:white;text-align:center;width:130px;');
            }
        }
    });
}
/**查询三方流水总记录数*/
function _getThirdFlowCount(data) {
    $.ajax({
        type: 'get',
        url: '/r/income/thirdLogCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#thirdFlows_tbody  #recordCurrentPage').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * res.page.pageNo) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1))) + '条记录');
                    $('#thirdFlows_tbody  #recordAllPage').empty().text('合计：' + res.page.totalElements + '条记录');
                    showPading(res.page, 'thirdFlows_pageFoot', _searchThirdFlow);
                }
            }
        }
    });
}
/**获取总金额*/
function _getSumThridFlow(data) {
    ///sumForThirdLog
    $.ajax({
        type: 'get',
        url: '/r/income/thirdLogSum',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    // 金额 手续费 余额
                    $('#thirdFlows_tbodyTd2').text(parseFloat(res.data[0][0] ? res.data[0][0] : 0).toFixed(3));
                    $('#thirdFlows_tbodyTd4').text(parseFloat(res.data[0][1] ? res.data[0][1] : 0).toFixed(3));
                    $('#thirdFlows_tbodyTd6').text(parseFloat(res.data[0][2] ? res.data[0][2] : 0).toFixed(3));
                }
            }
        }
    });
}
/**
 * 重置
 * @private
 */
function _resetValueSThird(type) {
    //initPaging($('#'+type + '_pageFoot'),pageInitial);
    _initialHandicap(type);
    var toAccount = $("#toAccount_" + type).val();//收款帐号
    if (toAccount) {
        if (type == 'thirdFlows') {
            $('#level_thirdFlows').change();
        }else {
            _initialInAccount(type);
        }
    }
    $("#member_" + type).val('');
    $('#thirdOrderNo_' + type).val('');
    $('#fromMoney_' + type).val('');
    $('#toMoney_' + type).val('');
    _datePickerForAll($("input.date-range-picker"));
    if (type == 'thirdFlows') {
        $('#payer_thirdFlows').val('');
    }
    if (type == "thirdFlows") {
        $('#orderNo_thirdFlows').val('');
        $('#channel_thirdFlows').val('');
        //setTimeout('_searchThirdFlow',300);
        _searchThirdFlow();
    } else {
        //setTimeout('setSearchType(\''+type+'\')',300);
        setSearchType(type);
    }
}
/**
 * 初始化盘口
 * @private
 */
function _initialHandicap2(type) {
    $('#handicap_' + type).empty();
    $.ajax({
        dataType: 'json',
        type: "get",
        url: "/r/handicap/findByPerm",
        async:false,
        data: {enabled: 1},
        success: function (res) {
            if (res.status == 1 && res.data) {
                if (res.data.length > 0) {
                    var opt = '<option>全部</option>';
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + val.id + '">' + val.name + '</option>';
                    });
                    $('#handicap_' + type).empty().html(opt);
                    $('#handicap_' + type).trigger('chosen:updated');
                    _handicapChange(type);
                }
            }
        }
    });
}
function _initialHandicap(type) {
    //可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        //allow_single_deselect:true,
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    if (handicapAndLevelInitialOptions) {
        $('#handicap_' + type).empty().html(handicapAndLevelInitialOptions[0]);
        $('#handicap_' + type).trigger('chosen:updated');
        $('#level_' + type).empty().html(handicapAndLevelInitialOptions[1]);
        $('#level_' + type).trigger('chosen:updated');
    }
    $('#handicap_' + type + '_chosen').prop('style', 'width: 78%;');
    $('#level_' + type + '_chosen').prop('style', 'width: 78%;');
    _handicapChange(type);
}
function _handicapChange(type) {
    if (!type){
        return;
    }
    $('#handicap_' + type).change(function () {
        _searchMatchedByFilter();
        if ($('#handicap_' + type).val() && $('#handicap_' + type).val() != '请选择' && $('#handicap_' + type).val() != '全部') {
            _initalLevel($('#handicap_' + type).val(), type);
        } else {
            $('#level_' + type).empty().html('<option>请选择</option>');
            $('#level_' + type).trigger('chosen:updated');
        }
    });
}

/**
 * 初始化层级
 * @private
 */
function _initalLevel(handicapId, type) {
    var opt = '<option>请选择</option>';
    $.ajax({
        dataType: 'json',
        type: "get",
        url: "/r/level/getByHandicapId",
        data: {"handicapId": handicapId},
        success: function (res) {
            if (res.status == 1 && res.data) {
                if (res.data.length > 0) {
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + val.id + '">' + val.name + '</option>';
                    });
                }
                $('#level_' + type).empty().html(opt);
                $('#level_' + type).trigger('chosen:updated');
            }
        }
    });
}
$('#level_thirdFlows').change(function () {
    _searchMatchedByFilter();
    _initialInAccount('thirdFlows');

});
$('#level_unmatch').change(function () {
    _searchMatchedByFilter();
    _initialInAccount('unmatch');
});
$('#level_matched').change(function () {
    _searchMatchedByFilter();
    _initialInAccount('matched');
});
/**
 * 初始化收款账号--三方商号
 * @private
 */
function _initialInAccount(type) {
    var levelId = $('#level_' + type).val();
    var url = '';
    var data = {};
    if (levelId && levelId != '请选择') {
        url = '/r/account/getByLevelId';
        data = {'levelId': levelId};
    }
    else {
        url = '/r/account/getAccountsByCurrentUser';
    }
    var opt = '<option>请选择</option>';
    $.ajax({
        type: 'get',
        url: url,
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $(res.data).each(function (i, val) {
                        if (val.type!=1 && val.type!=5) {
                            //不是公司入款卡 和出款卡
                            opt += '<option value="' + val.account + '">' + _checkObj(val.bankName) + '</option>';
                        }
                    });
                    $('#toAccount_'+third_searchType).empty().html(opt);
                    $('#toAccount_'+third_searchType).trigger("chosen:updated");
                }
            }
        }
    });
}
$('#toAccount_matched').change(function () {
    if($(this).val()!='全部' && $(this).val()!='请选择')
        _searchMatchedByFilter();
});
$('#toAccount_unmatch').change(function () {
    if($(this).val()!='全部' && $(this).val()!='请选择')
        _searchMatchedByFilter();
});

function _initialSelectChosen(type) {
    //可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    $("#handicap_"+type+"_chosen").prop('style', 'width: 78%;');
    $("#level_"+type+"_chosen").prop('style', 'width: 78%;');
    if('thirdFlows'==type){
        $('#toAccount_'+type+'_chosen').prop('style', 'width: 69%;');
    }else{
        $('#toAccount_'+type+'_chosen').prop('style', 'width: 60%;');
    }
}
setSearchType('matched');
