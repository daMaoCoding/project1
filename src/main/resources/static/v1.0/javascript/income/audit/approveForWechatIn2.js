/**
 * Created by Owner on 2018/3/14.
 */
currentPageLocation = window.location.href;
var selectedHandicap  = null;
var addRemarkRightFlag=false,addOrderRightFlag=false,
    cancelIncomeReqFlag=false,matchOrderRight=false,wechatMatchedOrders_li=false;
var searchWechatType=null;
$.each(ContentRight['IncomeAuditWechatIn:*'], function (name, value) {
    if (name == 'IncomeAuditWechatIn:CustomerAddRemark:*') {
        addRemarkRightFlag = true;
    }
    if (name=='IncomeAuditWechatIn:AuditorAddOrder:*'){
        addOrderRightFlag = true;
    }
    if (name == 'IncomeAuditWechatIn:cancelOrder:*') {
        cancelIncomeReqFlag = true;
    }
    if (name == 'IncomeAuditWechatIn:MatchOrder:*') {
        matchOrderRight = true;
    }
    if (name == 'IncomeAuditWechatIn:WechatMatchedOrdersLi:*') {
        wechatMatchedOrders_li = true;
        $('#wechatMatchedOrders_li').show();
    }
});
(function () {
    if (wechatMatchedOrders_li) {
        $('#wechatMatchedOrders_li').show();
    } else {
        $('#wechatMatchedOrders_li').hide();
    }
})();
$.each(ContentRight['Income:*'], function (name, value) {
    if (name == 'Income:currentpageSum:*') {
        incomeCurrentPageSum=true;
    }
    if (name =='Income:allRecordSum:*'){
        incomeAllRecordSum=true;
    }
});
function _handicap_change() {
    if (searchWechatType){
        $('#handicap_'+searchWechatType).change(function () {
            if (this.value!='-9'){
                selectedHandicap = this.value;
            }
        });
    }
}
//初始化盘口
function _initialApliPayHandicaps(type) {
    //时间控件
    _datePickerForAll($("#timeScope_"+type));
    $('#handicap_'+type).chosen(
        {
            allow_single_deselect: true,
            search_contains: true,
            no_results_text: "没有找到"
        });
    _getHandicapsByUserId(type);

}
function _getHandicapsByUserId(type) {
    $.ajax({
        url: '/r/out/handicap',async:false,
        type:'get',
        dataType:'json',
        success:function (res) {
            var opt ='';
            if (res && res.status==1 && res.data && res.data.length>0){
                $(res.data).each(function (i,val) {
                    if (selectedHandicap && val.code==selectedHandicap){
                        opt += '<option selected="selected" value="' + $.trim(val.code) + '" handicapcode="' + $.trim(val.code) + '">' + val.name + '</option>';
                    }else{
                        opt += '<option value="' + $.trim(val.code) + '" handicapcode="' + $.trim(val.code) + '">' + val.name + '</option>';
                    }
                });
            }
            if(!opt){
                opt ='<option value="-9">无盘口权限</option>';
            }
            $('#handicap_'+type).empty().html(opt);$('#handicap_'+type).trigger('chosen:updated');
            $('#handicap_'+type+'_chosen').prop('style','width:78%');
        }
    });
}
//正在匹配的 查询账号信息
function _searchwechatAccount() {
    //盘口
    var handicap = $("#handicap_wechatToMatch").val();
    if (!handicap || handicap=='-9') {
        return;
    }
    //获取时间段
    var startAndEndTime = $("#time_wechatToMatch").val(), timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" ~ ");timeStart =startAndEnd[0];timeEnd = startAndEnd[1] ;
    } else {
        var todayStart = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayStart = moment().hours(7).minutes(0).seconds(0);
        } else {
            todayStart = moment().subtract(1, 'days').hours(7).minutes(0).seconds(0);
        }
        var todayEnd = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayEnd = moment().add(1, 'days').hours(6).minutes(59).seconds(59);
        } else {
            todayEnd = moment().hours(6).minutes(59).seconds(59);
        }
        timeStart = todayStart.format("YYYY-MM-DD HH:mm:ss");timeEnd = todayEnd.format("YYYY-MM-DD HH:mm:ss");
    }
    //当前页码
    var CurPage = $("#footPage_wechatToMatch").find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    //查询外层的账号信息
    var data = {
        "oid": handicap.toString(), "type": 0, "inAccount": $('#wechatNumber_wechatToMatch').val(),
        "timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),
        "pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10"
    };
    $.ajax({
        type: "post",
        url: "/newpay/find4ByCondition",
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            $('#alipayToMatch_tbody').empty();
            if (res.status == 1) {
                var tr = '', trs = '';
                if (res.data && res.data.length > 0) {
                    $.each(res.data, function (i, val) {
                        tr +='<tr id="tr_'+val.id+'" onclick="window.event.preventDefault();_trClickEvent('+val.id+','+val.oid+',\''+val.inAccount+'\',\''+val.device+'\')" class="action-buttons show-details-btn" style="cursor: pointer;">' +
                            '<td>'+_showHandicapNameByIdOrCode(val.oid) +'</td>'+
                            '<td title="所在设备:'+val.device+'">'+_ellipsisAccount(val.inAccount)+'</td>'+
                            '<td class="center">'+_showNewPayAccountStatus(val.status)+'</td>'+
                            '<td class="center">'+val.reporttime+'</td>'+
                            '<td class="center">' +
                            '<div><a onclick="window.event.stopPropagation();_trClickEvent('+val.id+','+val.oid+',\''+val.inAccount+'\',\''+val.device+'\')" href="javascript:void(0);" class="red bigger-140 " title="待匹配明细">'+
                            '<i id="toMatchNumber" class="ace-icon fa fa-angle-double-down">'+val.mnt+'</i>'+
                            '<span class="sr-only">待匹配明细</span></a></div>'+
                            '</td></tr>';
                    });
                    trs += '<tr>' +
                        '<td id="currentCount_wechatToMatch" colspan="15">小计：统计中..</td></tr>';
                    trs += '<tr>' +
                        '<td id="currentCountTotal_wechatToMatch" colspan="15">总共：统计中..</td></tr>';
                }
                $('#wechatToMatch_tbody').empty().html(tr);
                $('#wechatToMatch_tbody').append(trs);
                if (res.status==1 && res.page){
                    $('#currentCount_wechatToMatch').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#currentCountTotal_wechatToMatch').empty().text('合计：' + res.page.totalElements + '条记录');
                }
                _showOutOrInner();
            } else {
                $.gritter.add({
                    time: 10000,class_name: '',title: '系统消息',text: res.message,sticky: false,
                    image: '../images/message.png'
                });
            }
            showPading(res.page, "footPage_wechatToMatch", _searchwechatAccount);
        }
    });
}
//查询未匹配 未认领 已取消 已匹配
function _searchWechatInByStatus() {
    var type =searchWechatType;
    var handicap = $("#handicap_"+type).val();
    if (!handicap || handicap=='-9') {
        return;
    }
    //获取时间段
    var startAndEndTime = $("#timeScope_"+type).val(), timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" ~ ");timeStart =startAndEnd[0];timeEnd = startAndEnd[1] ;
    } else {
        var todayStart = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayStart = moment().hours(7).minutes(0).seconds(0);
        } else {
            todayStart = moment().subtract(1, 'days').hours(7).minutes(0).seconds(0);
        }
        var todayEnd = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayEnd = moment().add(1, 'days').hours(6).minutes(59).seconds(59);
        } else {
            todayEnd = moment().hours(6).minutes(59).seconds(59);
        }
        timeStart = todayStart.format("YYYY-MM-DD HH:mm:ss");timeEnd = todayEnd.format("YYYY-MM-DD HH:mm:ss");
    }
    var CurPage = $("#"+type+"_footPage").find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage-1 : 0 : 0;
    //查询外层的账号信息
    var inAccount = type=='wechatCanceled'?"":$('#wechatNumber_'+type).val();
    var moneyFrom = $('#fromMoney_'+type).val();var moneyEnd = $('#toMoney_'+type).val();
    var orderNo = type=='wechatUnClaim'?"":$('#orderNo_'+type).val();var member = $('#member_'+type).val();
    var url='', data = {};
    if (type=='wechatUnMatch' || type=='wechatMatched'){
        url='/newpay/find5ByCondition';// 未匹配 已匹配
        var status = type=='wechatUnMatch'?0:1;
        data = {
            "oid": handicap.toString(), "type": 0, "status":status,"userName":member,
            "code":orderNo,"inAccount":inAccount,"timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),
            "moneyStart":moneyFrom,"moneyEnd":moneyEnd,"pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10"
        };
        if (status==1){
            data = $.extend({},data,{"chkRemark":$('#chkRemark_wechatMatched').val()})
        }
    }else{
        if (type=='wechatUnClaim'){
            url='/newpay/find6ByCondition';//未认领
            data = {
                "oid": handicap.toString(),"type": 0,"inAccount":inAccount,"timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),
                "moneyStart":moneyFrom,"moneyEnd":moneyEnd,"pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10"
            };
        }else{
            url='/newpay/find7ByCondition';//已取消
            data = {
                "oid": handicap.toString(), "userName":member,"code":orderNo,"type": 0,"inAccount":inAccount,"timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),
                "moneyStart":moneyFrom,"moneyEnd":moneyEnd,"pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10","chkRemark":$('#chkRemark_'+type).val()
            };
        }
    }
    $.ajax({
        type: "post", url: url,  dataType: 'json',
        async: false, data: JSON.stringify(data), contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res){
                var param={};
                param.tr='',param.amount=0,param.trs='',param.type=type;
                if(res.status==1){
                    if(res.data && res.data.length>0){
                        if(type=='wechatMatched' ||type=='wechatUnMatch'){
                            //已匹配渲染数据  未匹配渲染数据
                            param = _renderWechatMatchedOrUnMatch(res,param);
                        }
                        if (type=='wechatUnClaim'){
                            //未认领渲染数据
                            param =  _renderWechatUnClaim(res,param);
                        }
                        if (type=='wechatCanceled'){
                            //已取消渲染数据
                            param = _renderWechatCanceled(res,param);
                        }
                    }
                }else {
                    $.gritter.add({
                        time: 10000, class_name: '', title: '系统消息', text: res.message,
                        sticky: false, image: '../images/message.png'
                    });
                }
                $('#tbody_'+type ).empty().html(param.tr).append(param.trs);
                if(res.page){
                    $('#currentCount_'+type ).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#currentCountTotal_'+type ).empty().text('合计：' + res.page.totalElements + '条记录');
                }
                $("[data-toggle='popover']").popover();
                showPading(res.page, type+"_footPage", _searchWechatInByStatus );
            }
        }
    });
}
function _renderWechatMatchedOrUnMatch(res,param) {
    if(res.status==1 && res.data && res.data.length>0){
        var amount =0;
        $.each(res.data,function (i,val) {
            amount += parseFloat(val.money);
            param.tr +='<tr><td>'+_showHandicapNameByIdOrCode(val.oid)+'</td><td>'+val.level+'</td>'+
                    '<td>'+val.userName+'</td><td>'+val.code+'</td><td>'+val.money+'</td>'+
                    '<td>'+_ellipsisAccount(val.inAccount)+'</td><td>'+val.createtime+'</td>';
            if(param.type=='wechatMatched'){
                param.tr +='<td>'+val.admintime+'</td>';
                param.tr +='<td>'+(new Date(val.admintime).getTime()-new Date(val.createtime))/1000+'秒</td>';
            }
            if (_checkObj(val.remark).length > 6) {
                param.tr +=
                    '<td><a   class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + _showRemarkNewPay(val.remark) + '">'
                    + _checkObj(val.remark).substring(0, 6) + "..."
                    + '</a></td>';
            }else{
                param.tr +=
                    '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' +val.remark+ '">'
                    + _checkObj(val.remark)
                    + '</a></td>';
            }
            if (val.chkRemark){
                param.tr +=  '<td>'+val.chkRemark+'</td>';
            }else{
                param.tr +=  '<td></td>';
            }
            param.tr +='</tr>';
        });
        param.amount = amount;
        if(incomeCurrentPageSum){
            param.trs += '<tr><td id="currentCount_' + param.type  + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(param.amount).toFixed(3) + '</td>' +
                '<td colspan="10"></td>' +
                '</tr>';
        }else{
            param.trs += '<tr><td id="currentCount_' + param.type  + '" colspan="15">小计：统计中..</td></tr>';
        }
        if(incomeAllRecordSum){
            param.trs += '<tr><td id="currentCountTotal_' + param.type  + '" colspan="4">总共：统计中..</td>' +
                '<td id="currentSumTotal_' + param.type  + '" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">'+parseFloat(res.page.header.other).toFixed(3)+'</td>' +
                '<td colspan="10"></td>' +
                '</tr>';
        }else{
            param.trs += '<tr><td id="currentCountTotal_' + param.type  + '" colspan="15">总共：统计中..</td></tr>';
        }
    }
    return param;
}
function _renderWechatUnClaim(res,param) {
    if(res.status==1 && res.data && res.data.length>0){
        var amount =0;
        $.each(res.data,function (i,val) {
            amount += parseFloat(val.money);
            param.tr +='<tr><td>'+_showHandicapNameByIdOrCode(val.oid)+'</td><td>'+_ellipsisAccount(val.inAccount) +'</td>'+
                '<td>'+val.money+'</td><td>'+val.tradeCode+'</td><td>'+val.createtime+'</td><td>'+val.reporttime+'</td>';

            if (_checkObj(val.remark).length > 6) {
                param.tr +=
                    '<td><a   class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + _showRemarkNewPay(val.remark)  + '">'
                    + _checkObj(val.remark).substring(0, 6) + "..."
                    + '</a></td>';
            }else{
                param.tr +=
                    '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' +val.remark+ '">'
                    + _checkObj(val.remark)
                    + '</a></td>';
            }
            if (val.chkRemark){
                param.tr +=  '<td>'+val.chkRemark+'</td>';
            }else{
                param.tr +=  '<td></td>';
            }
            param.tr+='</tr>';
        });
        param.amount = amount;
        if(incomeCurrentPageSum){
            param.trs += '<tr><td id="currentCount_wechatUnClaim" colspan="2">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(param.amount).toFixed(3) + '</td>' +
                '<td colspan="10"></td>' +
                '</tr>';
        }else{
            param.trs += '<tr><td id="currentCount_wechatUnClaim" colspan="15">小计：统计中..</td></tr>';
        }
        if(incomeAllRecordSum){
            param.trs += '<tr><td id="currentCountTotal_wechatUnClaim" colspan="2">总共：统计中..</td>' +
                '<td id="currentSumTotal_wechatUnClaim" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">'+parseFloat(res.page.header.other).toFixed(3)+'</td>' +
                '<td colspan="9"></td>' +
                '</tr>';
        }else{
            param.trs += '<tr><td id="currentCountTotal_wechatUnClaim" colspan="15">总共：统计中..</td></tr>';
        }
    }
    return param;
}
function _renderWechatCanceled(res,param) {
    if(res.status==1 && res.data && res.data.length>0){
        var amount =0;
        $.each(res.data,function (i,val) {
            amount += parseFloat(val.money);
            param.tr +='<tr><td>'+_showHandicapNameByIdOrCode(val.oid)+'</td><td>'+val.level+'</td>'+
                '<td>'+_ellipsisAccount(val.inAccount)+'</td><td>'+val.userName+'</td><td>'+val.code+'</td>'+
                '<td>'+val.money+'</td><td>'+val.createtime+'</td><td>'+val.admintime+'</td>';
            if (_checkObj(val.remark).length > 6) {
                param.tr +=
                    '<td><a   class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + _showRemarkNewPay(val.remark)  + '">'
                    + _checkObj(val.remark).substring(0, 6) + "..."
                    + '</a></td>';
            }else{
                param.tr +=
                    '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + val.remark  + '">'
                    + _checkObj(val.remark)
                    + '</a></td>';
            }
            if (val.chkRemark){
                param.tr +=  '<td>'+val.chkRemark+'</td>';
            }else{
                param.tr +=  '<td></td>';
            }
            param.tr +='</tr>';
        });
        param.amount = amount;
        if(incomeCurrentPageSum){
            param.trs += '<tr><td id="currentCount_wechatCanceled" colspan="5">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(param.amount).toFixed(3) + '</td>' +
                '<td colspan="10"></td></tr>';
        }else{
            param.trs += '<tr><td id="currentCount_wechatCanceled" colspan="15">小计：统计中..</td></tr>';
        }
        if(incomeAllRecordSum){
            param.trs += '<tr><td id="currentCountTotal_wechatCanceled" colspan="5">总共：统计中..</td>' +
                '<td id="currentSumTotal_wechatCanceled" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">'+parseFloat(res.page.header.other).toFixed(3)+'</td>' +
                '<td colspan="10"></td>' +
                '</tr>';
        }else{
            param.trs += '<tr><td id="currentCountTotal_wechatCanceled" colspan="15">总共：统计中..</td></tr>';
        }
    }
    return param;
}
/**正在匹配重置条件*/
function _resetConditions(type) {
    if(type=='wechatToMatch_order_flow'){
        _initialApliPayHandicaps('wechatToMatch');
        $('#member_wechatToMatch').val('');
        $('#orderNo_wechatToMatch').val('');
        $('#fromMoney_wechatToMatch').val('');
        $('#toMoney_wechatToMatch').val('');
        $('#wechatNumber_wechatToMatch').val('');
        _datePickerForAll($('#time_wechatToMatch'));
        $('#flowNo_wechatToMatch').val('');
        _searchFlowDetailDisplay();
        _searchOrderDetailDisplay();
    }else{
        if(type!='wechatToMatch'){
            _datePickerForAll($('#timeScope_' + type));
        }else{
            _datePickerForAll($('#time_' + type));
        }
        if (type == 'wechatMatchedOrders') {
            _initialHandicap4Ali(type);
            _searchWecahtSummaryMatchedByFilter();
        } else {
            _initialApliPayHandicaps(type);
            if (type == 'wechatToMatch') {
                _searchwechatAccount();
            } else {
                _searchWechatInByStatus();
            }
        }
        $('#member_' + type).val('');
        $('#orderNo_' + type).val('');
        $('#fromMoney_' + type).val('');
        $('#toMoney_' + type).val('');
        $('#wechatNumber_' + type).val('');
        $('#chkRemark_wechatMatched').val('');
        $('#chkRemark_wechatMatched').val('');
    }

}
function _addTimeSelect() {
    var opt = '<option  value="0" selected="selected">不刷新</option><option   value="15">15秒</option><option  value="30">30秒</option>' +
        '<option  value="60">60秒</option><option  value="120">120秒</option><option  value="180">180秒</option>';
    $('#autoUpdateTimeWechatIn').empty().append(opt);
}
$('#autoUpdateTimeWechatIn').unbind().bind('change', function () {
    _searchIncomeAuditWechatInTimeTask();
});
//点击ul-li事件
function _initialSearTypeWechat(type) {
    searchWechatType =  type;
    if (type=='wechatToMatch'){
        _handicap_change();
        _datePickerForAll($('#time_'+type));
        _addTimeSelect();
        $('#freshWechatInLi').show();
        _initialApliPayHandicaps(type);
        _searchwechatAccount();
        _searchIncomeAuditWechatInTimeTask();
    }else{
        _datePickerForAll($('#timeScope_' + type));
        if (type != 'wechatMatchedOrders') {
            _handicap_change();
            if (timeOutSearchWechatIn) {
                clearInterval(timeOutSearchWechatIn);
            }
            $('#autoUpdateTimeWechatIn').empty();
            $('#freshWechatInLi').hide();
            _initialApliPayHandicaps(type);
            _searchWechatInByStatus();
        } else{
            _initialHandicap4Ali(type);
            _addTimeSelect();
            _searchWecahtSummaryMatchedByFilter();
        }
    }
}
function _searchWecahtSummaryMatchedByFilter() {
    var handicap = $('#handicap_' + searchWechatType).val();
    var level = $('#level_' + searchWechatType).val();
    var member = $('#member_' + searchWechatType).val();
    var aliAccount = $('#alipayNumber_' + searchWechatType).val();
    var startAndEnd = $("#timeScope_" + searchWechatType).val();
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
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayStart = moment().hours(7).minutes(0).seconds(0);
        } else {
            todayStart = moment().subtract(1, 'days').hours(7).minutes(0).seconds(0);
        }
        var todayEnd = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayEnd = moment().add(1, 'days').hours(6).minutes(59).seconds(59);
        } else {
            todayEnd = moment().hours(6).minutes(59).seconds(59);
        }
        startTime = todayStart.format("YYYY-MM-DD HH:mm:ss");
        endTime = todayEnd.format("YYYY-MM-DD HH:mm:ss");
    }
    var fromMoney = null;
    if ($('#fromMoney_' + searchWechatType).val()) {
        fromMoney = $('#fromMoney_' + searchWechatType).val();
    }
    var toMoney = null;
    if ($('#toMoney_' + searchWechatType).val()) {
        toMoney = $('#toMoney_' + searchWechatType).val();
    }
    var orderNo = '';
    if ($('#orderNo_' + searchWechatType).val()) {
        if ($('#orderNo_' + searchWechatType).val().indexOf('%') >= 0)
            orderNo = $.trim($('#orderNo_' + searchWechatType).val().replace(new RegExp(/%/g), '?'));
        else
            orderNo = $.trim($('#orderNo_' + searchWechatType).val());
    }
    var CurPage = $("#" + searchWechatType + "_pageFoot").find(".Current_Page").text();
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
        "handicapId": (handicap == '全部' || handicap == '请选择') ? null : handicap,
        "level": (level == '全部' || level == '请选择') ? null : level,
        "member": member ? member : null,
        "startTime": startTime ? $.trim(startTime) : null,
        "endTime": endTime ? $.trim(endTime) : null,
        "fromMoney": fromMoney ? fromMoney : null,
        "toMoney": toMoney ? toMoney : null,
        "orderNo": orderNo ? $.trim(orderNo) : null,
        "aliAccount": aliAccount,
        "pageNo": CurPage,"type":2,
        "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : 10
    };
    $.ajax({
        type: 'POST', contentType: 'application/json;charset=UTF-8',
        url: '/r/aliIn/findPage',
        data: JSON.stringify(data), dataType: 'json',
        success: function (res) {
            if (res) {
                $('#tbody_' + searchWechatType).empty();
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
                            '<td>' + _ellipsisAccount(_checkObj(val.account)) + '</td>' +
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
                        tr += '</tr>';
                    });
                    trs += '<tr><td id="CurrentCount' + searchWechatType + '" colspan="5">小计：统计中..</td>' +
                        '<td bgcolor="#579EC8" style="color:white;width:  auto;">' + parseFloat(amount).toFixed(3) + '</td>' +
                        '<td colspan="15"></td></tr>';
                    trs += '<tr><td id="AllCount' + searchWechatType + '" colspan="5">总共：统计中..</td>' +
                        '<td id="TotalSum' + searchWechatType + '" bgcolor="#D6487E" style="color:white;width:  auto;">统计中..</td>' +
                        '<td colspan="15"></td></tr>';
                    $('#tbody_' + searchWechatType).empty().html(tr).append(trs);
                    showPading(res.page, searchWechatType + '_pageFoot', _searchWecahtSummaryMatchedByFilter);
                    _findCount(data, searchWechatType);
                    _findSum(data, searchWechatType);
                } else {
                    showPading(res.page, searchWechatType + '_pageFoot', _searchWecahtSummaryMatchedByFilter);
                }
                $("[data-toggle='popover']").popover();
                //加载账号悬浮提示
                loadHover_accountInfoHover(idList);
            } else {
                showPading(null, searchWechatType + '_pageFoot', _searchWecahtSummaryMatchedByFilter);
            }
        }
    });
}
function _findCount(data, type) {
    $.ajax({
        type: 'POST', contentType: 'application/json;charset=UTF-8',
        url: '/r/aliIn/count',
        data: JSON.stringify(data),
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#CurrentCount' + type).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#AllCount' + type).empty().text('合计：' + res.page.totalElements + '条记录');
                }
                showPading(res.page, type + '_pageFoot', _searchWecahtSummaryMatchedByFilter);
            }
        }
    });
}
function _findSum(data, type) {
    $.ajax({
        type: 'POST', contentType: 'application/json;charset=UTF-8',
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
$('#handicap_wechatMatchedOrders').change(function () {
    _initalLevel(this.value, 'wechatMatchedOrders');
    _searchWecahtSummaryMatchedByFilter();
});
$('#level_wechatMatchedOrders').change(function () {
    _searchWecahtSummaryMatchedByFilter();
});
/**
 * 初始化层级
 * @private
 */
function _initalLevel(handicapId) {
    var opt = '<option>全部</option>';
    if (handicapId && handicapId != '全部' && handicapId != '请选择') {
        $.ajax({
            dataType: 'json',
            type: "get",
            url: "/r/level/getByHandicapId", async: false,
            data: {"handicapId": handicapId},
            success: function (res) {
                if (res.status == 1 && res.data) {
                    if (res.data.length > 0) {
                        $(res.data).each(function (i, val) {
                            opt += '<option value="' + val.id + '">' + val.name + '</option>';
                        });
                    }
                }
            }
        });
    }
    $('#level_wechatMatchedOrders').empty().html(opt);
    $('#level_wechatMatchedOrders').trigger('chosen:updated');
    $('#level_wechatMatchedOrders_chosen').prop('style', 'width:78%');

}
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
//tr 点击事件
function _trClickEvent(id,oid,inAccount,device) {
    _disPlayTrInnerTab(id,oid,inAccount,device);
}
function _disPlayTrInnerTab(id,oid,inAccount,device) {
    if($('.afterTr')&&$('.afterTr').length>0){
        $('.afterTr').each(function () { $(this).remove();});
    }
    if (!inAccount||!id){
        $('#wechat_outer_btn').show();
        $('#wechat_inner_btn').hide();
        return;
    }
    var tr_id = '#tr_'+id;
    var className = $(tr_id).find('td:nth-child(2)').attr('class');
    if (className && className.indexOf('tdBgcolor')>-1){
        //点击 关闭
        var saveAccountIdBeforeClose = window.localStorage.getItem('saveAccountIdBeforeClose');
        if(saveAccountIdBeforeClose){
            window.localStorage.removeItem('searchWechatFlowOrderId');
            window.localStorage.removeItem('searchWechatFlowOrderOid');
            window.localStorage.removeItem('searchWechatFlowOrderInAccount');
            window.localStorage.removeItem('searchWechatFlowOrderInDevice');
        }
        _searchwechatAccount();
    }else{
        //点击 展开
        window.localStorage.setItem('searchWechatFlowOrderId',id);
        window.localStorage.setItem('searchWechatFlowOrderOid',oid);
        window.localStorage.setItem('searchWechatFlowOrderInAccount',inAccount);
        window.localStorage.setItem('searchWechatFlowOrderInDevice',device);
    }
    window.localStorage.setItem('saveAccountIdBeforeClose',id);
    _showOutOrInner();
}
function _showOutOrInner() {
    var id = window.localStorage.getItem('searchWechatFlowOrderId');
    var saveAccountIdBeforeClose = window.localStorage.getItem('saveAccountIdBeforeClose');
    var tr_id = '#tr_'+saveAccountIdBeforeClose;
    if (!id){
        //关闭
        if(saveAccountIdBeforeClose){
            $(tr_id).find('td:nth-child(2)').prop('class','clickedTd');
        }
        $('#wechat_outer_btn').show();
        $('#wechat_inner_btn').hide();
    }else{
        if($('#wechatToMatch_tbody')&& $('#wechatToMatch_tbody').html().length>0){
            //打开
            $('#wechat_outer_btn').hide();
            $('#wechat_inner_btn').show();
            var className2Td = $(tr_id).find('td:nth-child(2)').prop('class');
            if (className2Td && className2Td.indexOf('clickedTd')>-1){
                $(tr_id).find('td:nth-child(2)').prop('class','');
            }
            _generateInnerTrForOrder();
            $(tr_id).find('td:nth-child(2)').prop('class','tdBgcolor');
            $(tr_id).siblings().not('.detail-row').find('td:nth-child(2)').prop('class','');
            _searchOrderDetailDisplay();_searchFlowDetailDisplay();

            $(tr_id).closest('tr').next().toggleClass('open');//内层的tab打开
            $(tr_id).siblings().not('.detail-row').toggleClass('siblingsDisplay');
            // $(tr_id).closest('tr').next().siblings().each(function () {
            //     if($(this).prop('id')=='tr_'+id || $(this).prop('id')=='footer_tr1' || $(this).prop('id')=='footer_tr2'){
            //     }else{
            //         $(this).toggleClass('siblingsDisplay');
            //     }
            // });//该tr之下的其他tr隐藏
            $(tr_id).find(ace.vars['.icon']).toggleClass('fa-angle-double-down').toggleClass('fa-angle-double-up');
        }else{
            $('#wechat_outer_btn').show();
            $('#wechat_inner_btn').hide();
        }

    }
}
//order 生成内层公共的tr table
function _generateInnerTrForOrder() {
    var inAccount = window.localStorage.getItem('searchWechatFlowOrderInAccount');
    var id = window.localStorage.getItem('searchWechatFlowOrderId');
    var device = window.localStorage.getItem('searchWechatFlowOrderInDevice');
    if (!inAccount||!id || !device){
        return;
    }
    var tr_id = '#tr_'+id;
    if(!$('#afterTr_'+id)||$('#afterTr_'+id).length==0){
        var trs =
            '<tr id="afterTr_'+id+'" class="detail-row"><td colspan="8">' +
            '<div style="padding-top: 0px" class="table-detail">' +
            '<div><table class="table  table-bordered table-hover no-margin-bottom"><thead><tr>'+
            '<th>选择</th><th>盘口</th><th>会员账号</th><th>金额</th><th>订单号</th><th>提单时间</th><th>备注</th><th>收款理由</th> <th>操作</th>'+
            '</tr></thead><tbody id="order_tbody_'+id+'"></tbody></table>' +
            '<div id="footPage_innerOrder_'+id+'"></div>'+
            '</div>' +
            '<div id="match_wetchin_div'+id+'" style="display: none;" class="row hr-12 match_wetchin_div">' +
            '<button onclick="_beforeInnerMatch('+id+',\''+inAccount+'\');" type="button" class="btn btn-sm btn-purple">' +
            '<i class="ace-icon fa fa-check-circle bigger-80"></i>' +
            '<span>匹配</span>' +
            '</button>' +
            '</div>'+
            '<div><table class="table  table-bordered table-hover no-margin-bottom"><thead>'+
            '<tr><th>选择</th><th>盘口</th><th>流水号</th><th>金额</th><th>支付时间</th><th>最新抓取时间</th>'+
            '<th>备注</th><th>收款理由</th><th>操作</th></tr></thead>' +
            '<tbody id="flow_tbody_'+id+'"></tbody>'+
            '</table><div id="footPage_innerFlow_'+id+'"></div>' +
            '</div>'+
            '</td></tr>';
        $(tr_id).after(trs);
    }
    if (matchOrderRight){
        $('div.match_wetchin_div').show();
    }
}
//查订单
function _searchOrderDetailDisplay() {
    var oid = window.localStorage.getItem("searchWechatFlowOrderOid");
    var inAccount = window.localStorage.getItem('searchWechatFlowOrderInAccount');
    var id = window.localStorage.getItem('searchWechatFlowOrderId');
    var device = window.localStorage.getItem('searchWechatFlowOrderInDevice');
    if (!inAccount || !oid ||!id || !device){
        return;
    }
    //获取时间段
    var startAndEndTime = $("#time_wechatToMatch").val(),timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" ~ ");timeStart =startAndEnd[0];timeEnd = startAndEnd[1] ;
    } else {
        var todayStart = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayStart = moment().hours(7).minutes(0).seconds(0);
        } else {
            todayStart = moment().subtract(1, 'days').hours(7).minutes(0).seconds(0);
        }
        var todayEnd = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayEnd = moment().add(1, 'days').hours(6).minutes(59).seconds(59);
        } else {
            todayEnd = moment().hours(6).minutes(59).seconds(59);
        }
        timeStart = todayStart.format("YYYY-MM-DD HH:mm:ss");timeEnd = todayEnd.format("YYYY-MM-DD HH:mm:ss");
    }
    var code = $('#orderNo_wechatToMatch').val();
    var userName = $('#member_wechatToMatch').val();
    var moneyFrom = $('#fromMoney_wechatToMatch').val();
    var moneyEnd = $('#toMoney_wechatToMatch').val();
    //当前页码
    var CurPage = $("#footPage_innerOrder_"+id).find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    var data = {
        "oid": oid, "type": 0, "inAccount":inAccount, "timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),
        "pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10",
        "moneyStart":moneyFrom,"moneyEnd":moneyEnd,"code":code,"userName":userName,"device":device
    };
    var url="/newpay/find10ByCondition";
    $.ajax({
        type: "post", url: url, dataType: 'json', async: false, data: JSON.stringify(data), contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res){
                var tr ='',trs='',amount=0;$('#order_tbody_'+id).empty();
                if(res.status==1 ){
                    if (res.data && res.data.length>0){
                        $.each(res.data,function (i,val) {
                            amount +=val.money;
                            tr +='<tr onclick="_selectOrderToMatch(this);"><td><input click_val="1" name="select_order_input" value="'+val.inId+'" type="radio"></td>' +
                                '<td oid_val="'+val.oid+'">'+_showHandicapNameByIdOrCode(val.oid)+'</td><td uid_val="'+(val.uid?val.uid:"")+'">'+(val.userName?val.userName:"")+'</td><td>'+val.money+'</td>'+
                                '<td>'+val.code+'</td><td>'+val.createtime+'</td>';
                            //+val.remark+'</td><td>';
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td>'
                                        + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' +_showRemarkNewPay(val.remark) + '">'
                                        + _checkObj($.trim(val.remark)).substring(0, 5)
                                        + '</a>'
                                        + '</td>';
                                } else {
                                    tr += '<td>'
                                        + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark  + '">'
                                        + _checkObj($.trim(val.remark))
                                        +'</a>'
                                        + '</td>';
                                }
                            } else {
                                tr += "<td></td>";
                            }
                            if (val.chkRemark){
                                tr += '<td>'+val.chkRemark+'</td>';
                            }else{
                                tr += '<td></td>';
                            }
                            tr += '<td>';
                            if (addRemarkRightFlag) {
                                tr += '<button id="addRemarkRightBTN1"  onclick="event.stopPropagation();_beforeAddRemark(' + val.inId + ',' + val.oid +',1);"  class="btn btn-xs btn-white btn-info" type="button" >' +
                                    '<i class="ace-icon fa fa-comments bigger-120">备注</i>' +
                                    '</button>';
                            }
                            if (cancelIncomeReqFlag) {
                                tr += '<button id="concealOrderRightBTN"   class=" btn btn-xs btn-white btn-danger" type="button" onclick="event.stopPropagation();_beforeCancel(' + val.inId + ',' + val.oid +',\''+val.code+'\');" >' +
                                    '<i class="ace-icon fa fa-remove bigger-120 red">取消</i>' +
                                    '</button>';
                            }
                            tr += '</td></tr>';
                        });
                        if(incomeCurrentPageSum){
                            trs += '<tr><td id="currentCount_order_tbody_'+id+'" colspan="3">小计：统计中..</td>' ;
                            trs +='<td bgcolor="#579EC8"  style="color: white;">'+parseFloat(amount).toFixed(3)+'</td>';
                            trs +='<td colspan="10"></td></tr>';
                        }else{
                            trs += '<tr><td id="currentCount_order_tbody_'+id+'" colspan="15">小计：统计中..</td></tr>' ;
                        }
                        if(incomeAllRecordSum){
                            trs += '<tr><td id="currentCountTotal_order_tbody_'+id+'" colspan="3">总共：统计中..</td>' ;
                            trs +='<td  bgcolor="#D6487E" style="color: white;">'+parseFloat(res.page.header.other).toFixed(3)+'</td>';
                            trs +='<td colspan="10"></td></tr>';
                        }else{
                            trs += '<tr><td id="currentCountTotal_order_tbody_'+id+'" colspan="15">总共：统计中..</td></tr>' ;
                        }
                        $('#order_tbody_'+id).empty().html(tr);
                        $('#order_tbody_'+id).append(trs);
                        if (res.status==1 && res.page){
                            $('#currentCount_order_tbody_'+id).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                            $('#currentCountTotal_order_tbody_'+id).empty().text('合计：' + res.page.totalElements + '条记录');
                        }
                    }
                }else {
                    $.gritter.add({
                        time: 10000, class_name: '', title: '系统消息', text: res.message,
                        sticky: false, image: '../images/message.png'
                    });
                }
                showPading(res.page, "footPage_innerOrder_"+id, _searchOrderDetailDisplay );$("[data-toggle='popover']").popover();
            }
        }
    });
}
function _selectOrderToMatch(obj) {
    var click_val = $(obj).find('input[name="select_order_input"]').attr('click_val');
    $(obj).siblings().each(function () {
        $(this).find('input[name="select_order_input"]').prop('checked','');
        $(this).find('input[name="select_order_input"]').attr('click_val',1);
    });
    if (click_val==1){
        $(obj).find('input[name="select_order_input"]').attr('click_val',2);
        $(obj).find('input[name="select_order_input"]').prop('checked','checked');
    }else{
        $(obj).find('input[name="select_order_input"]').attr('click_val',1);
        $(obj).find('input[name="select_order_input"]').prop('checked','');
    }
}
//取消订单
function _beforeCancel(id,oid,code) {
     $('#cancel_oid').val(oid);$('#cancel_id').val(id);$('#cancel_remark').val('');
    $('#cancel_code').val(code);$('#cancel_modal').modal('show');
}
function _afterCancel() {
    $('#cancel_oid').val('');$('#cancel_id').val('');$('#cancel_remark').val('');
    $('#cancel_code').val('');$('#cancel_modal').modal('hide');
}
function _confirmConcel() {
    var remark = $('#cancel_remark').val();
    if (!remark){
        $('#cancel_remarkPrompt').show(10).delay(500).hide(10);
        return ;
    }
    var data ={"id":$('#cancel_id').val(),"oid":$('#cancel_oid').val(),"remark":remark,"code":$('#cancel_code').val()};
    $.ajax({
        type: "post", url: '/newpay/cancel', dataType: 'json', async: false, data: JSON.stringify(data), contentType: 'application/json;charset=UTF-8',
        success:function (res) {
            if (res){
                $.gritter.add({
                    time: 5000, class_name: '', title: '系统消息', text: res.message,
                    sticky: false, image: '../images/message.png'
                });
                if (res.status==1){
                    _afterCancel();  _searchOrderDetailDisplay();
                }
            }
        }
    });
}
//查流水
function _searchFlowDetailDisplay() {
    var oid = window.localStorage.getItem("searchWechatFlowOrderOid");
    var inAccount = window.localStorage.getItem('searchWechatFlowOrderInAccount');
    var id = window.localStorage.getItem('searchWechatFlowOrderId');
    var device = window.localStorage.getItem('searchWechatFlowOrderInDevice');
    if (!inAccount || !oid ||!id || !device){
        return;
    }
    //获取时间段
    var startAndEndTime = $("#time_wechatToMatch").val();
    var timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" ~ ");
        timeStart =startAndEnd[0];
        timeEnd = startAndEnd[1] ;
    } else {
        var todayStart = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayStart = moment().hours(7).minutes(0).seconds(0);
        } else {
            todayStart = moment().subtract(1, 'days').hours(7).minutes(0).seconds(0);
        }
        var todayEnd = '';
        if ((moment() >= moment().hours(7).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
            todayEnd = moment().add(1, 'days').hours(6).minutes(59).seconds(59);
        } else {
            todayEnd = moment().hours(6).minutes(59).seconds(59);
        }
        timeStart = todayStart.format("YYYY-MM-DD HH:mm:ss");
        timeEnd = todayEnd.format("YYYY-MM-DD HH:mm:ss");
    }
    //当前页码
    var CurPage = $("#footPage_innerFlow_"+id).find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    var data = {
        "oid": oid, "type": 0, "inAccount":inAccount, "timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),
        "pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10","device":device,"flowNo":$.trim($('#flowNo_wechatToMatch').val())
    };
    var url ="/newpay/find11ByCondition";
    $.ajax({
        type: "post", url: url, dataType: 'json', async: false, data: JSON.stringify(data), contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res){
                var tr ='',trs='',amount=0;$('#flow_tbody_'+id).empty();
                if(res.status==1){
                    if(res.data && res.data.length>0){
                        $.each(res.data,function (i,val) {
                            amount +=val.money;
                            tr +='<tr onclick="_selectFlowToMatch(this);" ><td><input click_val="1" name="select_flow_input" value="'+val.id+'" type="radio"></td>' +
                                '<td>'+_showHandicapNameByIdOrCode(val.oid)+'</td><td>'+_checkObj(val.tradeCode)+'</td><td>'+(val.money?val.money:"")+'</td>'+
                                '<td>'+val.createtime+'</td><td>'+val.reporttime+'</td>';
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td>'
                                        + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _showRemarkNewPay(val.remark)  + '">'
                                        + _checkObj($.trim(val.remark)).substring(0, 5)
                                        + '</a>'
                                        + '</td>';
                                } else {
                                    tr += '<td>'
                                        + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' +val.remark  + '">'
                                        + _checkObj($.trim(val.remark))
                                        +'</a>'
                                        + '</td>';
                                }
                            } else {
                                tr += "<td></td>";
                            }
                            if (val.chkRemark){
                                tr += '<td>'+val.chkRemark+'</td>';
                            }else{
                                tr += '<td></td>';
                            }
                            if (addRemarkRightFlag) {
                                tr += '<td><button onclick="event.stopPropagation();_beforeAddRemark(' + val.id + ',' + val.oid + ',2);"  class="btn btn-xs btn-white btn-info" type="button" >' +
                                    '<i class="ace-icon fa fa-comments">备注</i>' +
                                    '</button>';
                            }
                            if (addOrderRightFlag) {
                                tr += '<button  onclick="event.stopPropagation();_addOrder(' + val.oid + ',\'' + val.inAccount + '\',\'' + (val.userName?val.userName:'' )+'\','+val.money+');" class="btn btn-xs btn-purple btn-white" type="button">' +
                                    '<i class="ace-icon fa fa-pencil ">补提单</i>' +
                                    '</button>';
                            }
                            tr +='</td></tr>';
                        });
                        if(incomeCurrentPageSum){
                            trs += '<tr><td id="currentCount_flow_tbody_'+id+'" colspan="3">小计：统计中..</td>';
                            trs+='<td bgcolor="#579EC8" style="color: white;">'+parseFloat(amount).toFixed(3)+'</td>';
                            trs+='<td colspan="10"></td></tr>';
                        }else{
                            trs += '<tr><td id="currentCount_flow_tbody_'+id+'" colspan="15">小计：统计中..</td></tr>';
                        }
                        if(incomeAllRecordSum){
                            trs += '<tr><td id="currentCountTotal_flow_tbody_'+id+'" colspan="3">总共：统计中..</td>' ;
                            trs+='<td bgcolor="#D6487E" style="color: white;">'+parseFloat(res.page.header.other).toFixed(3)+'</td>';
                            trs+='<td colspan="10"></td></tr>';
                        }else{
                            trs += '<tr><td id="currentCountTotal_flow_tbody_'+id+'" colspan="15">总共：统计中..</td></tr>' ;
                        }
                        $('#flow_tbody_'+id).empty().html(tr);
                        $('#flow_tbody_'+id).append(trs);
                        if (res.status==1 && res.page){
                            $('#currentCount_flow_tbody_'+id).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                            $('#currentCountTotal_flow_tbody_'+id).empty().text('合计：' + res.page.totalElements + '条记录');
                            $('#toMatchNumber').text(res.page.totalElements);
                        }
                    }
                }else {
                    $.gritter.add({
                        time: 10000, class_name: '', title: '系统消息', text: res.message,
                        sticky: false, image: '../images/message.png'
                    });
                }
                showPading(res.page, "footPage_innerFlow_"+id, _searchFlowDetailDisplay );$("[data-toggle='popover']").popover();
            }
        }
    });
}
function _selectFlowToMatch(obj) {
    var click_val = $(obj).find('input[name="select_flow_input"]').attr('click_val');
    $(obj).siblings().each(function () {
        $(this).find('input[name="select_flow_input"]').prop('checked','');
        $(this).find('input[name="select_flow_input"]').attr('click_val',1);
    });
    if (click_val==1){
        $(obj).find('input[name="select_flow_input"]').attr('click_val',2);
        $(obj).find('input[name="select_flow_input"]').prop('checked','checked');
    }else{
        $(obj).find('input[name="select_flow_input"]').attr('click_val',1);
        $(obj).find('input[name="select_flow_input"]').prop('checked','');
    }
}
// type =1 订单备注 2 流水备注
function _beforeAddRemark(id,oid,type) {
    $('#addRemark_Id').val(id);$('#addRemark_Oid').val(oid);$('#addRemark_remark').val('');
    $('#addRemark_Type').val(type);$('#addRemark_modal').modal('show');
}
function _afterAddRemark() {
    $('#addRemark_Id').val('');$('#addRemark_Oid').val('');
    $('#addRemark_Type').val('');$('#addRemark_modal').modal('hide');
}
function _confirmAddRemark() {
    var remark = $('#addRemark_remark').val();
    if (!remark){
        $('#addRemark_remarkPrompt').show(10).delay(3000).hide(10);
        return;
    }
    var url='',type= $('#addRemark_Type').val();
    if (type==1){
        url='/newpay/modifyRemark';
    }else{
        url='/newpay/addRemark';
    }
    var data = {"oid":$('#addRemark_Oid').val(),"id":$('#addRemark_Id').val(),"remark":remark};
    $.ajax({
        type: "post", url: url, dataType: 'json', async: false, data: JSON.stringify(data), contentType: 'application/json;charset=UTF-8',
        success:function (res) {
            if (res){
                $.gritter.add({
                    time: 5000, class_name: '', title: '系统消息', text: res.message,
                    sticky: false, image: '../images/message.png'
                });
                if (type==1){
                    _searchOrderDetailDisplay();
                }else{
                    _searchFlowDetailDisplay();
                }
                _afterAddRemark();
            }
        }
    });
}
// 补提单
function _addOrder(oid,inAccount,userName,money) {
    $('#makeUpMemberAccount').val(userName);$('#makeUpRemark').val('');
    $('#makeUpOid').val(_showHandicapNameByIdOrCode(oid));
    $('#makeUpAmount').val(money); $('#makeUpOid').attr("makeUpOid_val",oid);
    $('#makeUpAccount').val(inAccount);$('#makeUpFlow').modal('show');
}
function _afterAddOrder() {
    $('#makeUpMemberAccount').val("");$('#makeUpRemark').val('');
    $('#makeUpOid').val("");$('#makeUpAmount').val("");
    $('#makeUpAccount').val("");$('#makeUpFlow').modal('hide');
}
function _confirmAddOrder() {
    if ($('#makeUpMemberAccount').val()=='无' || !$('#makeUpMemberAccount').val()){
        $('#makeUpPrompt').text('填写会员名!'); $('#makeUpPrompt').show(10).delay(3000).hide(10);
        return;
    }
    var remark = $('#makeUpRemark').val();
    if (!remark){
        $('#makeUpPrompt').text('填写备注!'); $('#makeUpPrompt').show(10).delay(3000).hide(10);
        return;
    }
    var data = {"oid":$('#makeUpOid').attr("makeUpOid_val"),"userName":$('#makeUpMemberAccount').val()=='无'?"":$('#makeUpMemberAccount').val(),"money":$('#makeUpAmount').val(),"type":0,
                "account":$('#makeUpAccount').val(),"remark":remark,"createTime":new Date().getTime()};
    $.ajax({
        type: "post", url: '/newpay/putPlus', dataType: 'json', async: false, data: JSON.stringify(data), contentType: 'application/json;charset=UTF-8',
        success:function (res) {
            if (res){
                $.gritter.add({
                    time: 5000, class_name: '', title: '系统消息', text: res.message,
                    sticky: false, image: '../images/message.png'
                });
                if(res.status==1){
                    _afterAddOrder();
                    _searchFlowDetailDisplay();
                    _searchOrderDetailDisplay();
                }
            }
        }
    });

}
// 匹配之前
function _beforeInnerMatch(id,inAccount) {
    var order_checked = $('#order_tbody_'+id).find('input[name="select_order_input"]:checked');
    var flow_checked = $('#flow_tbody_'+id).find('input[name="select_flow_input"]:checked');
    if(!order_checked || order_checked.length==0){
        $.gritter.add({
            time: 5000, class_name: '', title: '系统消息', text: '请选择订单',
            sticky: false, image: '../images/message.png'
        });
        return;
    }

    if(!flow_checked|| flow_checked.length==0){
        $.gritter.add({
            time: 5000, class_name: '', title: '系统消息', text: '请选择流水',
            sticky: false, image: '../images/message.png'
        });
        return;
    }

    var amount1 = $(order_checked).parent().parent().find('td:nth-child(4)').text();
    var time1 = $(order_checked).parent().parent().find('td:nth-child(6)').text();
    var orderNo1 =  $(order_checked).parent().parent().find('td:nth-child(5)').text();
    $('#sysRequestBody_ali_amount').text(amount1);
    $('#sysRequestBody_ali_orderNo').text(orderNo1);
    $('#sysRequestBody_ali_time').text(time1);

    var amount2 = $(flow_checked).parent().parent().find('td:nth-child(4)').text();
    var time2= $(flow_checked).parent().parent().find('td:nth-child(5)').text();
    var summary =  $(flow_checked).parent().parent().find('td:nth-child(8)').text();
    var flowNo = $(flow_checked).parent().parent().find('td:nth-child(3)').text();
    $('#bankFlowBody_ali_amount').text(amount2);
    $('#bankFlowBody_ali_flowNo').text(flowNo);
    $('#bankFlowBody_ali_time').text(time2);
    $('#bankFlowBody_wechat_chkRemark').text(summary);
    if (amount1!=amount2){
        var member =$(order_checked).parent().parent().find('td:nth-child(3)').text();
        $('#inconsistentAmountMatchMemberAccount').val(member);//会员账号
        $('#inconsistentAmountMatchAmount').val(amount2);//存入金额
        $('#inconsistentAmountMatchBankAccount').val(inAccount);//收款支付宝
        $('#inconsistentAmountMatchBalanceGap').val(amount1-amount2);//差额

        $('#inconsistentAmountMatchInfo1').show();
        $('#inconsistentAmountMatchInfo2').show();
        $('#inconsistentAmountMatchInfo3').show();
        $('#commonMatchInfo').hide();
    }else{
        $('#inconsistentAmountMatchInfo1').hide();
        $('#inconsistentAmountMatchInfo2').hide();
        $('#inconsistentAmountMatchInfo3').hide();
        $('#commonMatchInfo').show();
    }
    $('#toMatchInfo_ali').modal('show');
    var oid_val = $(order_checked).parent().parent().find('td:nth-child(2)').attr('oid_val');
    var uid_val = $(order_checked).parent().parent().find('td:nth-child(3)').attr('uid_val');
    $('#oid_ali_match').val(oid_val);
    $('#logId_ali_match').val($(flow_checked).val());
    $('#inId_ali_match').val($(order_checked).val());
    $('#uid_ali_match').val(uid_val);
    $('#tradingFlow_ali_match').val(flowNo);

}
function _afterMatch() {
    $('#oid_ali_match').val('');
    $('#logId_ali_match').val('');
    $('#inId_ali_match').val('');
    $('#uid_ali_match').val('');
    $('#sysRequestBody_ali_amount').text('');
    $('#sysRequestBody_ali_orderNo').text('');
    $('#sysRequestBody_ali_time').text('');
    $('#inconsistentAmountMatchMemberAccount').val('');
    $('#inconsistentAmountMatchAmount').val('');
    $('#inconsistentAmountMatchBankAccount').val('');
    $('#inconsistentAmountMatchBalanceGap').val('');
    $('#bankFlowBody_ali_amount').text('');
    $('#bankFlowBody_ali_flowNo').text('');
    $('#bankFlowBody_ali_time').text('');
    $('#inconsistentAmountMatchRemarkList').val('');
    $('#commonMatchRemark').val('');
    $('#inconsistentAmountMatchRemark').val('');$('#matchRemark').val('');
    $('#toMatchInfo_ali').modal('hide');
    _searchOrderDetailDisplay();
    _searchFlowDetailDisplay();
}
function _confirmMatch() {
    var oid =  $('#oid_ali_match').val();
    var logId = $('#logId_ali_match').val();
    var inId =  $('#inId_ali_match').val();
    var uid =  $('#uid_ali_match').val();
    var amount1= $('#sysRequestBody_ali_amount').text();
    var amount2= $('#bankFlowBody_ali_amount').text();
    var flowNo = $('#tradingFlow_ali_match').val();
    var remark = '';
    if (parseFloat(amount2)!=parseFloat(amount1)){
        if(!$.trim($('#inconsistentAmountMatchRemark').val())){
            $('#remarkPrompt').show(10).delay(3000).hide(10);
            return;
        }
        remark = $.trim($('#inconsistentAmountMatchRemark').val());
    }else{
        if(!$.trim($('#matchRemark').val())){
            $('#remarkPrompt').show(10).delay(3000).hide(10);
            return;
        }
        remark = $.trim($('#matchRemark').val());
    }

    if(!oid || !logId|| !inId ||!uid || !remark){
        $.gritter.add({
            time: 5000, class_name: '', title: '系统消息', text: '参数丢失！',
            sticky: false, image: '../images/message.png'
        });
        return;
    }
    var data = {"oid":oid,"logId":logId,"inId":inId,"uid":uid,"remark":remark,"tradingFlow":flowNo};
    $.ajax({
        type: "post", url: '/newpay/matching', dataType: 'json', async: false, data: JSON.stringify(data), contentType: 'application/json;charset=UTF-8',
        success:function (res) {
            if (res){
                $.gritter.add({
                    time: 5000, class_name: '', title: '系统消息', text: res.message,
                    sticky: false, image: '../images/message.png'
                });
                if(res.status==1){
                    _afterMatch();
                }
            }
        }
    });
}

_initialSearTypeWechat('wechatToMatch');
//_disPlayTrInnerTab();