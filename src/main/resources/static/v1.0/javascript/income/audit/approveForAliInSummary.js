/**
 * Created by Administrator on 2018/10/5.
 */
var aliInSummary_timer = null;
var aliInSummary_SearchType = 'alimatched';
function _changSearch() {
    if (aliInSummary_timer != null) {
        clearInterval(aliInSummary_timer);
    }
    if ($("#changeFlushTime").val() != '不刷新') {
        aliInSummary_timer = setInterval(function () {
            _searchAliSummaryMatchedByFilter();
        }, $("#changeFlushTime").val());
    }
}
function setSearchType(type) {
    aliInSummary_SearchType = type;
    _searchAliSummaryMatchedByFilter();
}
_initialHandicap4Ali('alimatched');
_datePickerForAll($("#tab_alimatched input.date-range-picker"));
setSearchType('alimatched');

function _initialHandicap4Ali(type) {
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
}
$('#level_' + aliInSummary_SearchType).change(function () {
    _initialInAccount(aliInSummary_SearchType);
});
_initialInAccount(aliInSummary_SearchType);
function _initialInAccount(type) {
    var levelId = $('#level_' + type).val();
    var url = '';
    var data = {};
    if (levelId && levelId != '全部' && levelId != '请选择') {
        url = '/r/account/getByLevelId';
        data = {'levelId': levelId};
    }
    else {
        url = '/r/account/getAccountsByCurrentUser';
    }
    $.ajax({
        type: 'get',
        url: url,
        data: data,
        dataType: 'json',async:false,
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var opt = '<option>请选择</option>';
                    $(res.data).each(function (i, val) {
                        if (val.type == 3) {
                            //入款支付宝
                            opt += '<option value="' + val.account + '">' + _checkObj(val.account) + '</option>';
                        }
                    });
                    $('#toAccount_' + type).empty().html(opt);
                    $('#toAccount_' + type).trigger("chosen:updated");
                }
            }
        }
    });
}
$('#handicap_' + aliInSummary_SearchType).change(function () {
    _initalLevel(this.value, aliInSummary_SearchType);
   setSearchType(aliInSummary_SearchType);
});
$('#level_' + aliInSummary_SearchType).change(function () {
    setSearchType(aliInSummary_SearchType);
});
$('#toAccount_' + aliInSummary_SearchType).change(function () {
    setSearchType(aliInSummary_SearchType);
});
function _searchAliSummaryMatchedByFilter() {
    var handicap = $('#handicap_' + aliInSummary_SearchType).val();
    var level = $('#level_' + aliInSummary_SearchType).val();
    var member = $('#member_' + aliInSummary_SearchType).val();
    var aliAccount = $('#toAccount_' + aliInSummary_SearchType).val();
    var startAndEnd = $("#startAndEndTime_" + aliInSummary_SearchType).val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    } else {
        startTime = _getDefaultTime()[0];
        endTime = _getDefaultTime()[1];
    }
    var fromMoney = null;
    if ($('#fromMoney_' + aliInSummary_SearchType).val()) {
        fromMoney = $('#fromMoney_' + aliInSummary_SearchType).val();
    }
    var toMoney = null;
    if ($('#toMoney_' + aliInSummary_SearchType).val()) {
        toMoney = $('#toMoney_' + aliInSummary_SearchType).val();
    }
    var orderNo = '';
    if ($('#orderNo_' + aliInSummary_SearchType).val()) {
        if ($('#orderNo_' + aliInSummary_SearchType).val().indexOf('%') >= 0)
            orderNo = $.trim($('#orderNo_' + aliInSummary_SearchType).val().replace(new RegExp(/%/g), '?'));
        else
            orderNo = $.trim($('#orderNo_' + aliInSummary_SearchType).val());
    }
    var CurPage = $("#" + aliInSummary_SearchType + "_pageFoot").find(".Current_Page").text();
    if (!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    //(aliAccount=='全部'||aliAccount=='请选择')?"":$.trim(aliAccount)
    var data = {
        "handicapId": (handicap=='全部'||handicap=='请选择')?null:handicap,
        "level": (level=='全部'||level=='请选择')?null:level,
        "member": member?member:null,
        "startTime": startTime?$.trim(startTime):null,
        "endTime": endTime?$.trim(endTime):null,
        "fromMoney": fromMoney?fromMoney:null,
        "toMoney": toMoney?toMoney:null,
        "orderNo": orderNo?$.trim(orderNo):null,
        "aliAccount":aliAccount,
        "pageNo": CurPage,
        "pageSize": $.session.get('initPageSize')?$.session.get('initPageSize'):10
    };
    $.ajax({
        type: 'POST',contentType: 'application/json;charset=UTF-8',
        url: '/r/aliIn/findPage',
        data:  JSON.stringify(data),dataType: 'json',
        success: function (res) {
            if (res) {
                $('#tbody_' + aliInSummary_SearchType).empty();
                var tr = '', trs = '', amount = 0, idList = [];
                if (res.status == 1 && res.data) {
                    $(res.data).each(function (i, val) {
                        idList.push({'id': val.accountId});
                        amount += parseFloat(val.amount);
                        // "<td><a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.toId + "' data-placement='auto right' data-trigger='hover'  >"
                        //+ _ellipsisForBankName(_checkObj(val.account))
                        //+ "</a></td>" +
                        tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicapId) + '</td>' +
                            '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>' +
                            '<td>' + _checkObj(val.member) + '</td>' +
                            '<td>' + _checkObj(val.orderNo) + '</td>' +
                            '<td>' + _ellipsisForBankName(_checkObj(val.account))+ '</td>' +
                            '<td>' + _checkObj(val.amount) + '</td>' +
                            '<td>' + timeStamp2yyyyMMddHHmmss(_checkObj(val.createTime)) + '</td>' +
                            '<td>' + timeStamp2yyyyMMddHHmmss(_checkObj(val.updateTime)) + '</td>';
                        if (_checkObj(val.remark)) {
                            if (_checkObj(val.remark).length > 23) {
                                tr += '<td><a  class="bind_hover_card breakByWord"  title="备注信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + _divideRemarks(val.remark) + '">'
                                    + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                                    + '</a></td>';

                            } else {
                                tr += '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + val.remark + '">'
                                    + _checkObj(val.remark)
                                    + '</a></td>';
                            }
                        } else {
                            tr += '<td></td>';
                        }
                        tr+='</tr>';
                    });
                    trs += '<tr><td id="CurrentCount' + aliInSummary_SearchType + '" colspan="5">小计：统计中..</td>' +
                        '<td bgcolor="#579EC8" style="color:white;width:  auto;">' + parseFloat(amount).toFixed(3) + '</td>' +
                        '<td colspan="15"></td></tr>';
                    trs += '<tr><td id="AllCount' + aliInSummary_SearchType + '" colspan="5">总共：统计中..</td>' +
                        '<td id="TotalSum' + aliInSummary_SearchType + '" bgcolor="#D6487E" style="color:white;width:  auto;">统计中..</td>' +
                        '<td colspan="15"></td></tr>';
                    $('#tbody_' + aliInSummary_SearchType).empty().html(tr).append(trs);
                    showPading(res.page, aliInSummary_SearchType + '_pageFoot', _searchAliSummaryMatchedByFilter);
                    _findCount(data, aliInSummary_SearchType);
                    _findSum(data, aliInSummary_SearchType);
                }else{
                    showPading(res.page, aliInSummary_SearchType + '_pageFoot', _searchAliSummaryMatchedByFilter);
                }
                $("[data-toggle='popover']").popover();
                //加载账号悬浮提示
                loadHover_accountInfoHover(idList);
            }else{
                showPading(null, aliInSummary_SearchType + '_pageFoot', _searchAliSummaryMatchedByFilter);
            }
        }
    });
}
function _findCount(data, type) {
    $.ajax({
        type: 'POST',contentType: 'application/json;charset=UTF-8',
        url: '/r/aliIn/count',
        data: JSON.stringify(data),
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#CurrentCount' + type).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#AllCount' + type).empty().text('合计：' + res.page.totalElements + '条记录');
                }
                showPading(res.page, type + '_pageFoot', _searchAliSummaryMatchedByFilter);
            }
        }
    });
}
function _findSum(data, type) {
    $.ajax({
        type: 'POST',contentType: 'application/json;charset=UTF-8',
        url: '/r/aliIn/sum',
        data: JSON.stringify(data),
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $('#TotalSum' + type).text(parseFloat(res.data).toFixed(3));
                }
            }
        }
    });
}
/**太长的商号省略显示*/
function _ellipsisForBankName(toAccount) {
    var ellipsis = '';
    if (toAccount) {
        if (toAccount.toString().length > 12) {
            ellipsis = toAccount.toString().substring(0, 4);
            ellipsis += '***';
            ellipsis += toAccount.toString().substring(toAccount.toString().length - 4, toAccount.toString().length);
        } else {
            ellipsis = toAccount;
        }
    }
    return ellipsis;
}
function _resetValueSThird(type) {
    $('#fromMoney_' + type).val('');$('#toMoney_' + type).val('');
    $('#member_' + type).val('');$('#orderNo_'+type).val('');$('#toAccount_' + type).val('');
   // _initialInAccount(type);
    _initialHandicap4Ali(type);
    _datePickerForAll($("#tab_alimatched input.date-range-picker"));
    setSearchType(type);
}

/**
 * 初始化层级
 * @private
 */
function _initalLevel(handicapId, type) {
    if (handicapId=='全部' || handicapId=='请选择'){
        return ;
    }
    $.ajax({
        dataType: 'json',
        type: "get",
        url: "/r/level/getByHandicapId",
        data: {"handicapId": handicapId},
        success: function (res) {
            if (res.status == 1 && res.data) {
                if (res.data.length > 0) {
                    var opt = '<option>全部</option>';
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + val.id + '">' + val.name + '</option>';
                    });
                    $('#level_' + type).empty().html(opt);
                    $('#level_' + type).trigger('chosen:updated');
                }
            }
        }
    });
}