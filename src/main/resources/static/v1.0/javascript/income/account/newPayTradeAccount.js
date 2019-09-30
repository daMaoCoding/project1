/**
 * Created by Owner on 2018/5/28.
 */
currentPageLocation = window.location.href;
var searchTypeForACC = 'bank';
var handicap4LogSearch = null;
var account4LogSearch = null;
function _initialHandicap() {
    //可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        //allow_single_deselect:true,
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    var opt = '';
    $.ajax({
        type: 'get',
        url: '/r/out/handicap',
        data: {},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var handicap =window.localStorage.getItem("handicap4LogSearch");
                    $(res.data).each(function (i, val) {
                        if(handicap&&handicap==val.code){
                            opt += '<option selected="selected" value="' + $.trim(val.code) + '" handicapcode="' + $.trim(val.code) + '">' + val.name + '</option>';
                        }else{
                            opt += '<option value="' + $.trim(val.code) + '" handicapcode="' + $.trim(val.code) + '">' + val.name + '</option>';
                        }
                    });
                }
                if(!opt){
                    opt='<option value="-9">无盘口权限</option>';
                }
                $('#handicap_newPayTrade').empty().html(opt);
                $('#handicap_newPayTrade').trigger("chosen:updated");
                $('#handicap_newPayTrade_chosen').prop('style', 'width: 78%;');
            }
        }
    });
}
function _resetForData() {
    _initialHandicap();
    $('#account_newPayTrade').val('');
    $('#status_1').prop('checked', '');
    $('#status_2').prop('checked', '');
    $('#status_3').prop('checked', '');
    $('#syslevel_in').prop('checked', '');
    $('#syslevel_middle').prop('checked', '');
    $('#syslevel_out').prop('checked', '');
    $('#account_type1').prop('checked', '');
    $('#account_type2').prop('checked', '');
    _searchForData();
}
/**
 * 根据盘口初始化层级  现在没有层级 如果以后有只需打开即可
 */
$('#handicap_newPayTrade').change(function () {
    if ($('#handicap_newPayTrade').val() != '全部') {
        //_getLevelPayTrade();

    }
    _searchForData();

});

function _searchForData() {
    var handicap = $('#handicap_newPayTrade').val();
    if(!handicap ||handicap=='-9'){
        return ;
    }
    window.localStorage.setItem("handicap4LogSearch",handicap);
    // if (!handicap || handicap == '全部') {
    //     var handicap = [];
    //     $('#handicap_newPayTrade').find('option:not(:first-child)').each(function () {
    //         handicap.push($(this).val());
    //     });
    //     handicap = handicap.toString();
    // }
    var account = $('#account_newPayTrade').val();
    var syslevel = [];
    $("input[name='syslevel_newPayTrade']:checked").each(function () {
        syslevel.push(this.value);
    });

    var status = [], account_type = [];
    $('input[name="status_newPayTrade"]').each(function () {
        if ($(this).prop('checked')) {
            status.push(this.value);
        }
    });
    $('input[name="account_type"]').each(function () {
        if ($(this).prop('checked')) {
            account_type.push(this.value);
        }
    });
    account_type = account_type.toString();
    status = status.toString();
    var CurPage = $("#newPayTrade_" + searchTypeForACC + "_accountPage").find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    var startAndEnd = $('#timeScope_newPayAccount').val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
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
        startTime = todayStart.format("YYYY-MM-DD HH:mm:ss");
        endTime = todayEnd.format("YYYY-MM-DD HH:mm:ss");
    }
    var data = {
        "oid": handicap.toString(),
        "statuses": status,
        "levels": syslevel.toString(),
        "configTypes": account_type,
        "account": account,
        "timeStart": new Date(startTime).getTime(),
        "timeEnd": new Date(endTime).getTime(),
        "pageNo": CurPage,
        "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10",
    };
    if (searchTypeForACC == 'wechat') {
        data = $.extend(data, {"type": 0});
    }
    if (searchTypeForACC == 'alipay') {
        data = $.extend(data, {"type": 1});
    }
    var url = searchTypeForACC == 'bank' ? "/newpay/findBankCardByCondition" : ( searchTypeForACC == 'wechat' || searchTypeForACC == 'alipay' ? "/newpay/findAWByCondition" : null);
    if (!url) {
        return;
    }
    var checkType = searchTypeForACC == 'bank' ? 2 : searchTypeForACC == 'wechat' ? 0 : 1;
    $.ajax({
        type: 'post',
        url: url,
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                var tr = '';
                var amount = 0;
                var list4accountInfoHover = [], list4MobileInfoHover = [];
                if (res.status == 1) {
                    tr = '';
                    var accType = searchTypeForACC == 'bank' ? 2 : (searchTypeForACC == 'wechat' ? 0 : 1);
                    if (res.data && res.data.length > 0) {
                        $.each(res.data, function (i, val) {
                            amount += parseFloat(val.balance);
                            list4MobileInfoHover.push({oid: val.oid, id: val.mobileId});
                            list4accountInfoHover.push({oid: val.oid, id: val.mobileId, type: accType});
                            tr += '<tr><td>' + _showHandicapNameByIdOrCode(_checkObj(val.oid)) + '</td>';
                            tr += '<td>' + _showLevelName(val.level) + '</td>';
                            if (searchTypeForACC == 'bank') {
                                tr += "<td>";
                                tr += "<a class='bind_hover_card' data-trigger='hover'  data-placement='auto right' data-toggle='bankInfoHover" + val.mobileId + "'>" + val.account + "</a>";
                                tr += "</td>";
                            } else if (searchTypeForACC == 'alipay') {
                                tr += "<td>";
                                tr += "<a class='bind_hover_card' data-placement='auto right' data-trigger='hover' data-toggle='alipayInfoHover" + val.mobileId + "'>" + val.account + "</a>";
                                tr += "</td>";
                            } else if (searchTypeForACC == 'wechat') {
                                tr += "<td>";
                                tr += "<a class='bind_hover_card' data-placement='auto right' data-trigger='hover' data-toggle='wechatInfoHover" + val.mobileId + "'>" + val.account + "</a>";
                                tr += "</td>";
                            }
                            tr += '<td><a class="bind_hover_card" data-placement="auto right" data-trigger="hover" data-toggle="mobileInfoHover' + val.mobileId + '" data-original-title="" title="'+("设备号:"+(val.device?val.device:"无")+"(状态:"+_showDeviceStatus(val.deviceStatus)+")")+'">' +
                                '<span>' + hidePhoneNumber(val.tel) + '</span></a></td>';
                            tr += '<td>' + _checkObj(val.name) + '</td>';
                            tr += '<td>' + _showNewPayAccountType(val.type) + '</td>';//自用 客户 类型
                            tr += '<td>' + _showNewPayAccountStatus(val.status) + '</td>';
                            if (searchTypeForACC == 'bank') {
                                tr += '<td>' + val.balance + '</td>';
                            }
                            tr += '<td>';
                            //查询类型
                            var type1 = searchTypeForACC == 'bank' ? 2 : searchTypeForACC == 'wechat' ? 0 : searchTypeForACC == 'alipay' ? 1 : '';
                            //绑定的手机号id
                            var mobileId = searchTypeForACC == 'bank' ? '' : val.mobileId;
                            if (val.inMnt4) {
                                tr += '<a style="text-decoration:none;" href="#/NewPayTradeDetail:*?oid=' + val.oid + '&amp;level=' + val.level + '&amp;accoutId=' + val.id + '&amp;mobileId=' + mobileId + '&amp;type=' + type1 + '&amp;inoutType=0&amp;status=0&amp;timeStart=' + new Date($.trim(startTime)).getTime() + '&amp;timeEnd=' + new Date($.trim(endTime)).getTime() + '">' +
                                    '<span class="badge badge-warning" title="匹配中" name="mapping">' + val.inMnt4 + '</span>' +
                                    '</a>';
                            } else {
                                tr += '<a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-warning" title="匹配中" name="mapping">0</span></a>';
                            }
                            if (val.inMnt1) {
                                tr += '<a style=" text-decoration:none;" href="#/NewPayTradeDetail:*?oid=' + val.oid + '&amp;level=' + val.level + '&amp;accoutId=' + val.id + '&amp;mobileId=' + mobileId + '&amp;type=' + type1 + '&amp;inoutType=0&amp;status=1&amp;timeStart=' + new Date($.trim(startTime)).getTime() + '&amp;timeEnd=' + new Date($.trim(endTime)).getTime() + '">' +
                                    '<span class="badge badge-success" title="已匹配" name="mapped">' + val.inMnt1 + '</span></a>';
                            } else {
                                tr += '<a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-success" title="已匹配" name="mapping">0</span></a>';
                            }
                            if (searchTypeForACC != 'bank') {
                                if (val.inMnt3) {
                                    tr += '<a style=" text-decoration:none;" href="#/NewPayTradeDetail:*?oid=' + val.oid + '&amp;level=' + val.level + '&amp;accoutId=' + val.id + '&amp;mobileId=' + mobileId + '&amp;type=' + type1 + '&amp;inoutType=0&amp;status=2&amp;timeStart=' + new Date($.trim(startTime)).getTime() + '&amp;timeEnd=' + new Date($.trim(endTime)).getTime() + '">' +
                                        '<span class="badge badge-inverse" title="已驳回" name="cancel">' + val.inMnt3 + '</span>';
                                } else {
                                    tr += '<a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-inverse" title="已驳回" name="mapping">0</span></a>';
                                }
                            }
                            tr += '</td><td>';
                            if (val.outMnt4) {
                                tr += '<a style=" text-decoration:none;" href="#/NewPayTradeDetail:*?oid=' + val.oid + '&amp;level=' + val.level + '&amp;accoutId=' + val.id + '&amp;mobileId=' + mobileId + '&amp;type=' + type1 + '&amp;inoutType=1&amp;status=0&amp;timeStart=' + new Date($.trim(startTime)).getTime() + '&amp;timeEnd=' + new Date($.trim(endTime)).getTime() + '">' +
                                    '<span class="badge badge-warning" title="匹配中" name="mapping">' + val.outMnt4 + '</span>' +
                                    '</a>';
                            } else {
                                tr += '<a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-warning" title="匹配中" name="mapping">0</span></a>';
                            }
                            if (val.outMnt1) {
                                tr += '<a style=" text-decoration:none;" href="#/NewPayTradeDetail:*?oid=' + val.oid + '&amp;level=' + val.level + '&amp;accoutId=' + val.id + '&amp;mobileId=' + mobileId + '&amp;type=' + type1 + '&amp;inoutType=1&amp;status=1&amp;timeStart=' + new Date($.trim(startTime)).getTime() + '&amp;timeEnd=' + new Date($.trim(endTime)).getTime() + '">' +
                                    '<span class="badge badge-success" title="已匹配" name="mapped">' + val.outMnt1 + '</span></a>';
                            } else {
                                tr += '<a style="cursor:not-allowed;text-decoration:none;" target="_self"><span class="badge badge-success" title="已匹配" name="mapping">0</span></a>';
                            }
                            tr += '</td><td>' +
                                '<button type="button" class=" btn btn-xs btn-white btn-info btn-bold  " onclick="_searchLogDetailBefore(\'' + val.oid + '\',\'' + checkType + '\',\'' + val.id + '\',\'' + val.mobileId + '\',\'' + new Date(startTime).getTime() + '\',\'' + new Date(endTime).getTime() + '\');"><i class="icon-only ace-icon fa fa-align-justify"></i>流水</button>' +
                                '<button type="button" class=" btn btn-xs btn-white btn-info btn-bold  " onclick="_launchCheck(\'' + val.oid + '\',\'' + val.id + '\',\'' + val.account + '\',\'' + checkType + '\');"><i class="ace-icon fa fa-credit-card bigger-100 orange"></i>对账</button>' +
                                '</td></tr>';
                        });
                        var trs = '';
                        if (searchTypeForACC == 'bank') {
                            trs += '<tr>' +
                                '<td id="currentCount_' + searchTypeForACC + '" colspan="7">小计：统计中..</td>' +
                                '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(amount).toFixed(3) + '</td>' +
                                '<td colspan="9"></td>' +
                                '</tr>';
                            trs += '<tr><td id="currentCountTotal_' + searchTypeForACC + '" colspan="7">总共：统计中..</td>' +
                                '<td id="currentSumTotal_' + searchTypeForACC + '" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">'+parseFloat(res.page.header.other).toFixed(3)+'</td>' +
                                '<td colspan="9"></td>' +
                                '</tr>';
                        } else {
                            trs += '<tr>' +
                                '<td id="currentCount_' + searchTypeForACC + '" colspan="15">小计：统计中..</td></tr>';
                            trs += '<tr>' +
                                '<td id="currentCountTotal_' + searchTypeForACC + '" colspan="15">总共：统计中..</td></tr>';
                        }
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
                $('#newPayTrade_' + searchTypeForACC + '_tbody').empty().html(tr);
                $('#newPayTrade_' + searchTypeForACC + '_tbody').append(trs);
                if (res.status == 1 && res.page) {
                    $('#currentCount_' + searchTypeForACC).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#currentCountTotal_' + searchTypeForACC).empty().text('合计：' + res.page.totalElements + '条记录');
                }
                loadHover_wechat_bankInfoHover(list4accountInfoHover);
                loadHover_MobileInfoHover(list4MobileInfoHover);
                //bonusTotalForeach($('#newPayTrade_' + searchTypeForACC + '_tbody'), mobileArray);
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips  span.badge-danger').prop('title', '无法匹配').text('');
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips ').show();
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips label:eq(1)').hide();
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips span:eq(3)').hide();
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips span:eq(4)').hide();
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips span:eq(5)').hide();
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips span:eq(6)').hide();
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips span:eq(7)').hide();
                $('#newPayTrade_' + searchTypeForACC + '_accountPage').find('.showImgTips span:eq(8)').hide();
            }
            if (localStorage.getItem("backLiFlag")) {
                localStorage.removeItem("backLiFlag");
            }
            showPading(res.page, "newPayTrade_" + searchTypeForACC + "_accountPage", _searchForData);
        }
    })
}

//发起对账 type=2 银行 1支付宝 0微信
function _launchCheck(oid, accountId, account, type) {
    bootbox.confirm('<h4 class="center">确定对' + account + '进行对账操作？</h4>', function (res) {
        if (res) {
            $.ajax({
                type: 'post',
                url: "/newpay/verifyAccount",
                data: JSON.stringify({"oid": oid, "accountId": accountId, "type": type}),
                contentType: 'application/json;charset=UTF-8',
                dataType: 'json',
                success: function (res) {
                    if (res) {
                        $.gritter.add({
                            time: 10000,
                            class_name: '',
                            title: '系统消息',
                            text: res.message,
                            sticky: false,
                            image: '../images/message.png'
                        });
                        _searchForData();
                    }
                }
            });
        }
    });

}
function _searchLogDetailBefore(oid, searchType, accountId, mobileId, timeStart, timeEnd) {
    $('#logDetailOid').val(oid);
    $('#logDetailCheckType').val(searchType);
    $('#logDetailAccountId').val(accountId);
    $('#logDetailMobileId').val(mobileId);
    
	$("[name=status_logDetail]:checkbox").prop("checked","checked");
    $('#timeScope_logDetail').val('');
    $('#fromMoney_logDetail').val('');
    $('#toMoney_logDetail').val('');
    _datePickerForAll($('#timeScope_logDetail'));

    _searchLogDetail(timeStart, timeEnd);
}
//流水明细
function _searchLogDetail(timeStart, timeEnd) {
    var oid = $('#logDetailOid').val();
    var searchType = $('#logDetailCheckType').val();
    var accountId = $('#logDetailAccountId').val();
    var mobileId = $('#logDetailMobileId').val();
    if (searchType == 2) {
        $('#to_account').show();
    } else {
        $('#to_account').hide();
    }
    var status = [];
    $('input[name="status_logDetail"]').each(function () {
        if ($(this).prop('checked')) {
            status.push(this.value);
        }
    });
    var amountFrom = $('#fromMoney_logDetail').val();
    var amountTo = $('#toMoney_logDetail').val();
    var CurPage = $("#log_detail_foot").find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    var startAndEnd = $('#timeScope_logDetail').val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = new Date(startAndEnd[0]).getTime();
            endTime = new Date(startAndEnd[1]).getTime();
        }
    } else {
        startTime = timeStart;
        endTime = timeEnd;
    }
    var url = '', data = {
        "pageNo": CurPage,
        "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : 10,
        "oid": oid,
        "mobileId": mobileId,
        "accountId": accountId,
        "statuses": status.toString(),
        "timeStart":startTime,
        "timeEnd":endTime,
        "moneyStart": amountFrom,
        "moneyEnd": amountTo
    };
    if (searchType == 2) {
        url = '/newpay/find9ByCondition';$('#inoutType4WA').hide();$('#chRemark4WA').hide();
    } else {
        url = '/newpay/findAWLog3ByCondition';
        $('#inoutType4WA').show();$('#chRemark4WA').show();
        data = $.extend(data, {"type": searchType});
    }
    $.ajax({
        type: 'post', url: url,
        dataType: 'json', async: false, data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                var tr = '', trs = '', amount = 0;
                if (res.status == 1) {
                    var colspanNum = 1;
                    if (searchType == 2) {
                        $('#to_account').show();
                        $('#to_remark').hide();
                        colspanNum = 2;
                    } else {
                        $('#to_account').hide();
                        $('#to_remark').show();
                    }
                    if (res.data && res.data.length > 0) {
                        $.each(res.data, function (i, val) {
                            amount += parseFloat(val.money);
                            if (searchType == 2) {
                                tr += '<tr><td>' + _checkObj(val.inAccount) + '</td>' +
                                    '<td>' + _checkObj(val.toAccount) + '</td>';
                            } else {
                                tr += '<tr><td>' + _checkObj(val.inAccount) + '</td>';
                            }
                            tr += '<td>' + _checkObj(val.money) + '</td>' ;
                            if(searchType!=2){
                                tr +='<td>' + _checkObj(val.inoutTypeDesc) + '</td>' ;
                            }
                            tr +='<td>' + _showNewLogTradeStatus(val.status) + '</td>' +
                                '<td>' + _checkObj(val.tradeCode) + '</td>' +
                                '<td>' + _checkObj(val.createtime) + '</td>' +
                                '<td>' + _checkObj(val.reporttime)+ '</td>' +
                                '<td>' + _checkObj(val.summary) + '</td>';
                            if(searchType!=2){
                                tr +='<td>' + _checkObj(val.chkRemark) + '</td>' ;
                            }
                            if (searchType != 2) {
                                // 2为银行
                                if (_checkObj(val.remark)) {
                                    if (_checkObj(val.remark).length > 5) {
                                        tr += '<td>'
                                            + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _showRemarkNewPay(val.remark )+ '">'
                                            + _checkObj(val.remark).substring(0, 5)
                                            + '</a>'
                                            + '</td>';
                                    } else {
                                        tr += '<td>'
                                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + val.remark + '">'
                                            + _checkObj(val.remark)
                                            +'</a>'
                                            + '</td>';
                                    }
                                } else {
                                    tr += "<td></td>";
                                }
                            }
                            tr += "</tr>";
                        });
                        trs += '<tr>' +
                            '<td id="currentCount_logDetail" colspan="' + colspanNum + '">小计：统计中..</td>' +
                            '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(amount).toFixed(3) + '</td>' +
                            '<td colspan="9"></td>' +
                            '</tr><tr><td id="currentCountTotal_logDetail" colspan="' + colspanNum + '">总共：统计中..</td>' +
                            '<td id="currentSumTotal_logDetail" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">'+parseFloat(res.page.header.other).toFixed(3)+'</td>' +
                            '<td colspan="9"></td>' +
                            '</tr>';
                    }
                    $('#detail_body').empty().html(tr);
                    $('#detail_body').append(trs);
                    if (res.status == 1 && res.page) {
                        $('#currentCount_logDetail').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                        $('#currentCountTotal_logDetail').empty().text('合计：' + res.page.totalElements + '条记录');
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
                $("[data-toggle='popover']").popover();
                showPading(res.page, "log_detail_foot", _searchLogDetail);
                $('#modal-logDetail').modal('show');
            }
        }
    });
}
//订单状态
function _showNewLogTradeStatus(status) {
    switch (status) {
        case 0:
            status = '<span class="label label-sm label-warning">未匹配</span>';
            break;
        case 1:
            status = '<span class="label label-sm label-success">已匹配</span>';
            break;
        default:
            break;
    }
    return status;
}

//重置
function _resetSearchLog() {
    $('#timeScope_logDetail').val('');
    $('#fromMoney_logDetail').val('');
    $('#toMoney_logDetail').val('');
    _datePickerForAll($('#timeScope_logDetail'));
    _searchLogDetail(null, null);
}


function _showNewPayAccountType(status) {
    switch (status) {
        case 1:
            status = '<span class="label label-sm label-success">自用</span>';
            break;
        case 0:
            status = '<span class="label label-white middle  label-info">客户</span>';
            break;
        default:
            break;
    }
    return status;
}
function _changeTabInit(type) {
    searchTypeForACC = type;
    _searchForData();
}
function _judgeSearchType() {
    var backLiFlag = localStorage.getItem("backLiFlag");
    if (backLiFlag) {//页面返回
        searchTypeForACC = backLiFlag;
        $('#Li_' + searchTypeForACC + '  a').tab('show');
        _changeTabInit(searchTypeForACC);
        localStorage.removeItem("backLiFlag");
    } else {
        _changeTabInit(searchTypeForACC);
    }
}
$('#timeScope_logDetail').on("mousedown", function () {
    _datePickerForAll($('#timeScope_logDetail'));
});
_datePickerForAll($('#timeScope_newPayAccount'));
_flushByTime();
_initialHandicap();
_judgeSearchType();
