/**
 * Created by Owner on 2018/5/30.
 */
currentPageLocation = window.location.href;
var previousPageLi = null;
var previousInOut = null;
var colspanValue = 4;
function _searchForDetailData() {
    //oid='+val.oid+'&amp;level=' + val.level +'&amp;accoutId=' + val.id +'&amp;mobileId='+mobileId+'&amp;type=' + type1 + '&amp;inoutType=0&amp;status=4&amp;timeStart=' + new Date($.trim(startTime)).getTime() + '&amp;timeEnd=' + new Date($.trim(endTime)).getTime()
    var param = currentPageLocation.split('?')[1];
    var params = param.split('&');
    var param1 = params[0].split('=')[1];//oid
    var param2 = params[1].split('=')[1];//level
    var param3 = params[2].split('=')[1];//accountId
    var param4 = params[3].split('=')[1];//mobileId
    var param5 = params[4].split('=')[1];//type 0 1 2 微信 支付宝 银行
    var param6 = params[5].split('=')[1];//inoutType 入0 出1
    var param7 = params[6].split('=')[1];//status 0 1 2 匹配中 已匹配  已取消
    var param8 = params[7].split('=')[1];//timeStart
    var param9 = params[8].split('=')[1];//timeEnd

    var type = param5;
    previousPageLi=type==2?'bank':type==0?'wechat':type==1?'alipay':"";

    previousInOut = param6==0?'in':'out';

    if (param6 ==1) {
        $('#member_newPayTradeDetail').attr('disabled', 'disabled');
    }

    if (previousInOut=='in'){
        if(previousPageLi=='bank'){
            $('#remark_detail').hide();
        }else{
            $('#remark_detail').show();
        }
        $('#account_trade_type').text('类型');
    }else{
        $('#remark_detail').show();$('#account_trade_type').text('交易类型');
    }
    var startAndEnd = $('#timeScope_newPayTradeDetail').val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = new Date(startAndEnd[0]).getTime();
            endTime = new Date(startAndEnd[1]).getTime();
        }
    } else {
        startTime = param8;endTime=param9;
    }

    var CurPage = $("#newPayTrade_detail_accountPage").find(".Current_Page").text();
    CurPage = CurPage?CurPage>0?CurPage-1:0:0;
    var url='',data = {};
    if (previousInOut=='in'){
        if(previousPageLi=='bank'){
            url='/newpay/find2ByCondition';
            data = {
                "oid": param1, "level": param2,"accountId": param3, "inoutType": param6, "status": param7, "timeStart": startTime, "timeEnd": endTime,
                "code": $('#orderNo_newPayTradeDetail').val(),  "userName": $('#member_newPayTradeDetail').val(),
                "moneyStart": $('#fromMoney_newPayTradeDetail').val(), "moneyEnd": $('#toMoney_newPayTradeDetail').val(), "pageNo": CurPage,
                "pageSize": $.session.get('initPageSize')?$.session.get('initPageSize'):10,
            };
        }else{
            url='/newpay/findAW2ByCondition';
            data = {
                "oid": param1, "level": param2,"type":param5,"accountId": param3,"mobileId": param4, "inoutType": param6, "status": param7, "timeStart": startTime, "timeEnd": endTime,
                "code": $('#orderNo_newPayTradeDetail').val(),  "userName": $('#member_newPayTradeDetail').val(),
                "moneyStart": $('#fromMoney_newPayTradeDetail').val(), "moneyEnd": $('#toMoney_newPayTradeDetail').val(), "pageNo": CurPage,
                "pageSize": $.session.get('initPageSize')?$.session.get('initPageSize'):10,
            };
        }
    }else{
        url = '/newpay/find3ByCondition';
        data = {
            "oid": param1, "level": param2,"type":param5,"accountId": param3, "inoutType": param6, "status": param7, "timeStart": startTime, "timeEnd": endTime,
            "code": $('#orderNo_newPayTradeDetail').val(),  "userName": $('#member_newPayTradeDetail').val(),
            "moneyStart": $('#fromMoney_newPayTradeDetail').val(), "moneyEnd": $('#toMoney_newPayTradeDetail').val(), "pageNo": CurPage,
            "pageSize": $.session.get('initPageSize')?$.session.get('initPageSize'):10,
        };
    }

    $.ajax({
        type: 'post', url: url,
        dataType: 'json', async:false, data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                var page = res.page, tr = '',trs = '', amount = 0;
                if (res.status==1 && res.data && res.data.length >0) {
                    $.each(res.data, function (i, val) {
                        amount += parseFloat(val.money);
                        tr += '<tr><td>' + _showHandicapNameByIdOrCode(_checkObj(val.oid)) + '</td>';
                        tr += '<td>'+(previousInOut == 'in'?val.levelCode:_showLevelName(val.level))+'</td>';
                        if (previousInOut == 'in') {
                            if (previousPageLi=='bank'){
                                $('#member_td').hide();$('#levelName_td').hide();
                                colspanValue = 3;
                            }else{
                                $('#member_td').show();$('#levelName_td').show();
                                tr +='<td>'+(val.levelName?val.levelName:'')+'</td>';
                                tr +='<td>' + _checkObj(val.userName) + '</td>';
                                colspanValue = 5;
                            }
                            $('#from_account').hide();$('#account_trade_type').hide();
                            var toAccount = previousPageLi=='bank'?val.account:val.inAccount;
                            tr += '<td>' + _checkObj(toAccount) + '</td>' ;
                        } else {
                            $('#from_account').show();
                            $('#remark_detail').hide();
                            $('#member_td').hide();$('#levelName_td').hide();
                            var toAccount = val.payAccount;
                            tr += '<td>' + _checkObj(toAccount) + '</td>' ;
                            tr +='<td>' + _checkObj(val.fromAccount) + '</td>';
                            //转账类型
                            var typeF = _showNewPayTradeType(val.type);
                        }
                        tr +='<td>' + _checkObj(val.money) + '</td>' ;
                        if (previousPageLi=='out'){
                            tr+= '<td>' + typeF + '</td>' ;
                        }
                        tr+= '<td>' + _checkObj(val.code) + '</td>' +
                            '<td>' + _showNewPayTradeStatus(val.status) + '</td>' +
                            '<td>' + _checkObj(val.createtime) + '</td>' +
                            '<td>' + _checkObj(val.admintime) + '</td>';
                        if (previousInOut == 'in' && previousPageLi!='bank') {
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td>'
                                        + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' +_showRemarkNewPay(val.remark)  + '">'
                                        + _checkObj(val.remark).substring(0, 5)
                                        + '</a>'
                                        + '</td></tr>';
                                } else {
                                    tr += '<td>'
                                        + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark + '">'
                                        + _checkObj(val.remark)
                                        +'</a>'
                                        + '</td></tr>';
                                }
                            } else {
                                tr += "<td></td></tr>";
                            }
                        }
                    });
                    trs += '<tr>' +
                        '<td id="currentCount_newPayDetail" colspan="' + colspanValue + '">小计:' + ((page.totalElements - page.pageSize * (page.pageNo)) >= 0 ? page.pageSize : (page.totalElements - page.pageSize * (page.pageNo - 1) )) + '条记录</td>' +
                        '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(amount).toFixed(3) + '</td>' +
                        '<td colspan="9"></td>' +
                        '</tr>';
                    trs += '<tr><td id="currentCountTotal_newPayDetail" colspan="' + colspanValue + '">总共：' + page.totalElements + '条记录</td>' +
                        '<td id="currentSumTotal_newPayDetail" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">'+parseFloat(res.page.header.other).toFixed(3)+'</td>' +
                        '<td colspan="9"></td>' +
                        '</tr>';
                }
                $('#newPayTrade_detail_tbody').empty().html(tr);
                $('#newPayTrade_detail_tbody').append(trs);  $("[data-toggle='popover']").popover();
            }
            showPading(page, "newPayTrade_detail_accountPage", _searchForDetailData);
        }
    })
}
function _back() {
    window.history.back();
    localStorage.setItem("backLiFlag", previousPageLi);
}
function _resetForNewpayDetail() {
    //_initialHandicapForNewpayDetail();
    $('#level_newPayTradeDetail').empty().html('<option>全部</option><option>外层</option><option>指定层</option><option>内层</option>');
    $('#level_newPayTradeDetail').trigger("chosen:updated");
    $('#level_newPayTradeDetail_chosen').prop('style', 'width: 78%;');
    $('#member_newPayTradeDetail').val('');
    $('#account_newPayTradeDetail').val('');
    $('#fromMoney_newPayTradeDetail').val('');
    $('#toMoney_newPayTradeDetail').val('');
    $('#orderNo_newPayTradeDetail').val('');
    $('#timeScope_newPayTradeDetail').val('');
    //_datePickerForAll($('#timeScope_newPayTradeDetail'));
    setTimeout(function () {
        _searchForDetailData();
    }, 200);
}
/**
 * 根据盘口初始化层级
 */
$('#handicap_newPayTradeDetail').change(function () {
    if ($('#handicap_newPayTradeDetail').val() != '全部') {
        //_getLevel();
        _searchForDetailData();
    }
    else {

    }
});
//订单状态
function _showNewPayTradeStatus(status) {
    switch (status) {
        case 0:
            status = '<span class="label label-sm label-warning">匹配中</span>';
            break;
        case 1:
            status = '<span class="label label-sm label-success">已匹配</span>';
            break;
        case 2:
            status = '<span class="label label-sm label-inverse">已取消</span>';
            break;
        default:
            break;
    }
    return status;
}
//交易类型
function _showNewPayTradeType(type) {
    //转出类型，0：微信提现到银行卡，1：支付宝提现到银行卡，2：兼职人员银行卡转账到业主收款银行卡
    var typeDesc = '';
    switch (type) {
        case 1:
            type = '<span class="label label-sm label-success" title="支付宝提现到银行卡">支付宝提现</span>';
            break;
        case 0:
            type = '<span class="label label-sm label-danger" title="微信提现到银行卡">微信提现</span>';
            break;
        case 2:
            type = '<span class="label label-sm label-default" title="兼职人员银行卡转账到业主收款银行卡">兼转业主</span>';
            break;
        default:
            break;
    }
    return typeDesc;
}

$('#timeScope_newPayTradeDetail').on("mousedown", function () {
    _datePickerForAll($('#timeScope_newPayTradeDetail'));
});
_searchForDetailData();
