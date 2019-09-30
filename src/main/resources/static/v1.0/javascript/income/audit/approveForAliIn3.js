/**
 * Created by Administrator on 2018/7/21.
 */
currentPageLocation = window.location.href;
var selectedHandicap = null;
var addRemarkRightFlag = false, addOrderRightFlag = false,
    cancelIncomeReqFlag = false, matchOrderRight = false, aliMatchedOrders_li = false;
var searchAlipayType = null, searchOutInLevelFlag = true;// true 表示查外层账号
															// false表示查流水订单
var MatchingTab =false,MatchedTab=false,ToMatchTab=false,UnclaimTab=false,CanceledTab=false;
$.each(ContentRight['IncomeAuditAliIn:*'], function (name, value) {
    if (name == 'IncomeAuditAliIn:CustomerAddRemark:*') {
        addRemarkRightFlag = true;
    }
    if (name == 'IncomeAuditAliIn:AuditorAddOrder:*') {
        addOrderRightFlag = true;
    }
    if (name == 'IncomeAuditAliIn:cancelOrder:*') {
        cancelIncomeReqFlag = true;
    }
    if (name == 'IncomeAuditAliIn:MatchOrder:*') {
        matchOrderRight = true;
    }
    if (name == 'IncomeAuditAliIn:AliMatchedOrdersLi:*') {
        aliMatchedOrders_li = true;
    }
    if (name=='IncomeAuditAliIn:Matching:*'){
        MatchingTab=true;$('#alipayToMatch_li').show();
    }
    if (name=='IncomeAuditAliIn:Matched:*'){
        MatchedTab=true;$('#alipayMatched_li').show();
    }
    if (name=='IncomeAuditAliIn:ToMatch:*'){
        ToMatchTab=true;$('#alipayUnMatch_li').show();
    }
    if (name=='IncomeAuditAliIn:Unclaim:*'){
        UnclaimTab=true;$('#alipayFail_li').show();
    }
    if (name=='IncomeAuditAliIn:Canceled:*'){
        CanceledTab=true;$('#alipayCanceled_li').show();
    }
    _hideLi();
});
function _hideLi(){
    $('#approveForAliIn2Ul li:first-child').attr('class','active');
}
$.each(ContentRight['Income:*'], function (name, value) {
    if (name == 'Income:currentpageSum:*') {
        incomeCurrentPageSum = true;
    }
    if (name == 'Income:allRecordSum:*') {
        incomeAllRecordSum = true;
    }
});
// 初始化盘口
function _initialApliPayHandicaps(type) {
    $('#handicap_' + type).chosen(
        {
            allow_single_deselect: true,
            search_contains: true,
            no_results_text: "没有找到"
        });
    _getHandicapsByUserId(type);

}
function _getHandicapsByUserId(type) {
    $.ajax({
        url: '/r/out/handicap', async: false,
        type: 'get',
        dataType: 'json',
        success: function (res) {
            var opt ='';
            if (res && res.status==1 && res.data && res.data.length>0){
                $(res.data).each(function (i,val) {
                    if (selectedHandicap && val.code==selectedHandicap){
                        opt += '<option selected="selected" value="' + $.trim(val.id) + '" handicapcode="' + $.trim(val.code) + '">' + val.name + '</option>';
                    }else{
                        opt += '<option value="' + $.trim(val.id) + '" handicapcode="' + $.trim(val.code) + '">' + val.name + '</option>';
                    }
                });
            }
            if(!opt){
                opt ='<option value="-9">无盘口权限</option>';
            }
            $('#handicap_'+type).empty().html(opt);$('#handicap_'+type).trigger('chosen:updated');
            $('#handicap_'+type+'_chosen').prop('style','width:78%');
            // 选择盆口就初始化层级
            _initialLevel(type);
        }
    });
}



// 查询 进行中，成功 的支付宝入款单信息
function _searchAlipayInByStatus() {
    // 盘口
    var handicap = $("#handicap_"+searchAlipayType).val();
    if (!handicap || handicap == '-9') {
        return;
    }
    
    // 层级
    var level = $("#level_"+searchAlipayType).val();
    var member = $('#member_'+searchAlipayType).val().toString();
    var toMember = $('#toMember_'+searchAlipayType).val().toString();
    var inOrderNo = $('#inOrderNo_'+searchAlipayType).val().toString();
    var outOrderNo = $('#outOrderNo_'+searchAlipayType).val().toString();
    var toHandicapRadio = $("input[name='toHandicapRadio_"+searchAlipayType+"']:checked").val();
    var fromHandicapRadio = $("input[name='fromHandicapRadio_"+searchAlipayType+"']:checked").val();
    if(fromHandicapRadio==0){//会员出款盘口为0时查询全部出款单
    	handicap = null;
    }
    // 获取时间段
    var startAndEndTime = $("#time_"+searchAlipayType).val(), timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" ~ ");
        timeStart = startAndEnd[0];
        timeEnd = startAndEnd[1];
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
    // 当前页码
    var CurPage = $("#footPage_"+searchAlipayType).find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    // 查询参数
    var data = {
            "handicap": handicap, "level":level==null?null:level, "member": member,"toMember": toMember,"inOrderNo": inOrderNo,"outOrderNo": outOrderNo,
            "timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),"toHandicapRadio": toHandicapRadio,
            "pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10"
        };
   
    var url = searchAlipayType=="alipayMatched"?"/r/IncomeAuditAliIn/aliIncomeMatched":"/r/IncomeAuditAliIn/aliIncomeSuccess";
    $.ajax({
        type: "post",
        url: url,
        dataType: 'json',
        data: data,
        // contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            $('#'+searchAlipayType+'_tbody').empty();
            if (res.status == 1) {
                var tr = '', trs = '',amount = 0,toAmount = 0;
                if (res.data && res.data.length > 0) {
                    $.each(res.data, function (i, val) {
                    	amount = amount+val.amount;
                    	toAmount = toAmount+val.toAmount;
                        tr += '<tr>' +
                            '<td >' + val.handicapName + '</td>' +
                            '<td >' + val.member + '</td>' +
                            '<td >' + val.levelName + '</td>' +
                            '<td >' + val.amount + '</td>' +
                            '<td >' + '入款单：'+val.inOrderNo +'<br>';
                            if(val.toHandicapName=='返利网'){
                            	tr +='返代入：'+val.toOrderNo+ '</td>' ;
                            }else{
                            	tr +='出款单：'+val.toOrderNo+ '</td>' ;
                            }
                            tr += '<td >' + val.toHandicapName + '</td>' +
                            '<td >' +val.toMember+'</td>'+
                            '<td >' + val.toLevelName + '</td>' +
                            '<td >' + val.toAmount + '</td>' +
                            '<td >' + val.createTime + '</td>' ;
                            if(searchAlipayType=='alipaySuccess'){
                            	tr += '<td >' + val.finishTime + '</td>' ;
                            }
                            tr += '<td >' + changeColor4LockTime(val.waitLongTime) + '</td>' +
                            '</tr>';
                    });
                    
                    if(searchAlipayType=='alipaySuccess'){
                    trs += '<tr id="footer_tr1">' +
                        '<td id="currentCount_'+searchAlipayType+'" colspan="3">小计：统计中..</td><td bgcolor="#579EC8" style="color:white;" id="pageAmount_'+searchAlipayType+'" colspan="1"></td><td  colspan="4"></td><td bgcolor="#579EC8" style="color:white;" id="pageAmount2_'+searchAlipayType+'" colspan="1"></td><td  colspan="3"></td></tr>';
                    trs += '<tr id="footer_tr2">' +
                        '<td id="currentCountTotal_'+searchAlipayType+'" colspan="3">总共：统计中..</td><td bgcolor="#D6487E" style="color:white;" id="totalAmount_'+searchAlipayType+'" colspan="1"></td><td  colspan="4"></td><td bgcolor="#D6487E" style="color:white;" id="totalAmount2_'+searchAlipayType+'" colspan="1"></td><td  colspan="3"></td></tr>';
                    }else{
                    	 trs += '<tr id="footer_tr1">' +
                         '<td id="currentCount_'+searchAlipayType+'" colspan="3">小计：统计中..</td><td bgcolor="#579EC8" style="color:white;" id="pageAmount_'+searchAlipayType+'" colspan="1"></td><td  colspan="4"></td><td bgcolor="#579EC8" style="color:white;" id="pageAmount2_'+searchAlipayType+'" colspan="1"></td><td  colspan="2"></td></tr>';
                     trs += '<tr id="footer_tr2">' +
                         '<td id="currentCountTotal_'+searchAlipayType+'" colspan="3">总共：统计中..</td><td bgcolor="#D6487E" style="color:white;" id="totalAmount_'+searchAlipayType+'" colspan="1"></td><td  colspan="4"></td><td bgcolor="#D6487E" style="color:white;" id="totalAmount2_'+searchAlipayType+'" colspan="1"></td><td  colspan="2"></td></tr>';
                    }
                    }
                $('#'+searchAlipayType+'_tbody').empty().html(tr);
                $('#'+searchAlipayType+'_tbody').append(trs);
                if (res.status == 1 && res.page) {
                    $('#currentCount_'+searchAlipayType).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + ' 条记录');
                    $('#currentCountTotal_'+searchAlipayType).empty().text('合计：' + res.page.totalElements + ' 条记录');
                    $('#pageAmount_'+searchAlipayType).empty().text(amount.toFixed(2) +" 元");
                    $('#pageAmount2_'+searchAlipayType).empty().text(toAmount.toFixed(2) +" 元");
                    var arr = res.message.split(':');
                    $('#totalAmount_'+searchAlipayType).empty().text(arr[0]+" 元");
                    $('#totalAmount2_'+searchAlipayType).empty().text(arr[1]+" 元");
                }
                _showOutOrInner();
            } else {
                $.gritter.add({
                    time: 10000, class_name: '', title: '系统消息', text: res.message, sticky: false,
                    image: '../images/message.png'
                });
            }
            showPading(res.page, searchAlipayType+"_footPage", _searchAlipayInByStatus);
            modifyNouns();
        }
    });
}

/** 耗时控制变色 毫秒转换 */
var changeColor4LockTime=function(lockTime_mss){
	if(!lockTime_mss){
		return '0';
	}
	var is_red=(lockTime_mss/(1000*60*10)>=1);// 大于十分钟红色
	var is_green=(lockTime_mss/(1000*60*5)>=1);// 大于五分钟绿色
	var  lockTime_class='';
	if(is_red){
		lockTime_class=' class="label label-danger" ';
	}else if(is_green){
		lockTime_class=' class="label label-success" ';
	}
	return '<span style="width:100%" '+lockTime_class+'>'+formatDuring2(lockTime_mss)+'</span>' ;
}
/** 耗时控制变色 毫秒转换 显示时分秒 */
function formatDuring2(mss) {
    var str = '';
    var days = parseInt(mss / (1000 * 60 * 60 * 24));
    var dh = 0;
    var hours = parseInt((mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    var minutes = parseInt((mss % (1000 * 60 * 60)) / (1000 * 60));
    var seconds = Math.round(parseFloat((mss % (1000 * 60)) / 1000));
    if (days) {
        dh += days * 24;
    }
    hours += dh;
    if (hours) {
        if (hours > 0 && hours < 10) {
            str += "0" + hours;
        } else {
            str += hours;
        }
    } else {
        str += "00"
    }
    str += "时";
    if (minutes) {
        if (hours) {
            if (minutes > 0 && minutes < 10) {
                str += "0" + minutes;
            } else {
                str += minutes;
            }
        } else {
            if (minutes > 0 && minutes < 10) {
                str += "0" + minutes;
            } else {
                str += "" + minutes;
            }
        }
    } else {
        str += "00";
    }
    str += "分";
    if (seconds) {
        if (minutes) {
            if (seconds > 0 && seconds < 10) {
                str += "0" + seconds;
            } else {
                str += seconds;
            }
        } else {
            if (seconds > 0 && seconds < 10) {
                str += "0" + seconds;
            } else {
                str += "" + seconds;
            }
        }

    } else {
        str += "00";
    }
    str += "秒";
    return str;
}

function _handicap_change() {
    $('#handicap_' + searchAlipayType).change(function () {
        if (this.value != '-9') {
            selectedHandicap = this.value;
            // 选择盆口就初始化层级
            _initialLevel(searchAlipayType);
            if(searchAlipayType=='alipayToMatch'||searchAlipayType=='alipayFail'){// 正在匹配
                _searchAlipayInByStatus2();
            }else{// 成功记录 ，进行中
            	_searchAlipayInByStatus();
            }
        }
    });
}
// 查询 正在匹配，失败的 支付宝入款记录
function _searchAlipayInByStatus2() {
    // 盘口
    var handicap = $("#handicap_"+searchAlipayType).val();
    if (!handicap || handicap == '-9') {
        return;
    }
    // 层级
    var level = $("#level_"+searchAlipayType).val();
    // 获取时间段
    var startAndEndTime = $("#time_"+searchAlipayType).val(), timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" ~ ");
        timeStart = startAndEnd[0];
        timeEnd = startAndEnd[1];
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
    // 当前页码
    var CurPage = $("#footPage_"+searchAlipayType).find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    // 查询支付宝入款单信息
    var data = {
            "handicap": handicap, "level": level==null?null:level, "incomeMember": $('#member_'+searchAlipayType).val().toString(),"incomeOrder": $('#orderNo_'+searchAlipayType).val().toString(),
            "timeStart": new Date(timeStart).getTime(), "timeEnd": new Date(timeEnd).getTime(),
            "pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10"
        };
    var url = searchAlipayType=="alipayToMatch"?"/r/IncomeAuditAliIn/aliIncomeToMatch":"/r/IncomeAuditAliIn/aliIncomeFail";
    $.ajax({
        type: "post",
        url: url,
        dataType: 'json',
        data: data,
        success: function (res) {
            $('#'+searchAlipayType+'_tbody').empty();
            if (res.status == 1) {
                var tr = '', trs = '',amount = 0;
                if (res.data && res.data.length > 0) {
                    $.each(res.data, function (i, val) {
                    	amount = amount+val.amount;
                        tr += '<tr>' +
                            '<td >' + val.handicapName + '</td>' +
                            '<td >' + val.member_user_name + '</td>' +
                            '<td >' + val.levelName + '</td>' +
                            '<td >' + val.amount + '</td>' +
                            '<td >' + val.order_no + '</td>' +
                            '<td >' + val.create_time + '</td>' +
                            '<td >' +changeColor4LockTime(val.waitLongTime)+'</td>'+
                            '</tr>';
                    });
                    trs += '<tr id="footer_tr1">' +
                        '<td id="currentCount_'+searchAlipayType+'" colspan="3">小计：统计中..</td><td bgcolor="#579EC8" style="color:white;" id="pageAmount_'+searchAlipayType+'" colspan="1"></td><td  colspan="3"></td></tr>';
                    trs += '<tr id="footer_tr2">' +
                        '<td id="currentCountTotal_'+searchAlipayType+'" colspan="3">总共：统计中..</td><td bgcolor="#D6487E" style="color:white;" id="totalAmount_'+searchAlipayType+'" colspan="1"></td><td  colspan="3"></td></tr>';
                }
                $('#'+searchAlipayType+'_tbody').empty().html(tr);
                $('#'+searchAlipayType+'_tbody').append(trs);
                if (res.status == 1 && res.page) {
                    $('#currentCount_'+searchAlipayType).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + ' 条记录');
                    $('#currentCountTotal_'+searchAlipayType).empty().text('合计：' + res.page.totalElements + ' 条记录');
                    $('#pageAmount_'+searchAlipayType).empty().text(amount.toFixed(2) +" 元");
                    $('#totalAmount_'+searchAlipayType).empty().text(res.message+" 元");
                }
                _showOutOrInner();
            } else {
                $.gritter.add({
                    time: 10000, class_name: '', title: '系统消息', text: res.message, sticky: false,
                    image: '../images/message.png'
                });
            }
            showPading(res.page, searchAlipayType+"_footPage", _searchAlipayInByStatus2);
            modifyNouns();
        }
    });
}
function _renderAlipayMatchedOrUnMatch(res, param) {
    if (res.status == 1 && res.data && res.data.length > 0) {
        var amount = 0;
        $.each(res.data, function (i, val) {
            amount += parseFloat(val.money);
            param.tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.oid) + '</td><td>' + val.level + '</td>' +
                '<td>' + val.userName + '</td><td>' + val.code + '</td><td>' + val.money + '</td>' +
                '<td>' + _ellipsisAccount(val.inAccount) + '</td><td>' + val.createtime + '</td>';
            if (param.type == 'alipayMatched') {
                param.tr += '<td>' + val.admintime + '</td>';
                param.tr += '<td>' + (new Date(val.admintime).getTime() - new Date(val.createtime)) / 1000 + '秒</td>';
            }
            // param.tr+='<td>'+_checkObj(val.remark)+'</td></tr>';
            if (_checkObj(val.remark).length > 6) {
                param.tr +=
                    '<td><a    class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + _showRemarkNewPay(val.remark) + '">'
                    + _checkObj(val.remark).substring(0, 6) + "..."
                    + '</a></td>';
            } else {
                param.tr +=
                    '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + val.remark + '">'
                    + _checkObj(val.remark)
                    + '</a></td>';
            }
            if (val.chkRemark) {
                param.tr += '<td>' + val.chkRemark + '</td>';
            } else {
                param.tr += '<td></td>';
            }
            param.tr += '</tr>';
        });
        param.amount = amount;
        if (incomeCurrentPageSum) {
            param.trs += '<tr>' +
                '<td id="currentCount_' + param.type + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(param.amount).toFixed(3) + '</td>' +
                '<td colspan="9"></td>' +
                '</tr>';
        } else {
            param.trs += '<tr><td id="currentCount_' + param.type + '" colspan="15">小计：统计中..</td></tr>';
        }
        if (incomeAllRecordSum) {
            param.trs += '<tr><td id="currentCountTotal_' + param.type + '" colspan="4">总共：统计中..</td>' +
                '<td id="currentSumTotal_' + param.type + '" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">' + parseFloat(res.page.header.other).toFixed(3) + '</td>' +
                '<td colspan="9"></td>' +
                '</tr>';
        } else {
            param.trs += '<tr><td id="currentCountTotal_' + param.type + '" colspan="15">总共：统计中..</td></tr>';
        }
    }
    return param;
}
function _renderAlipayUnClaim(res, param) {
    if (res.status == 1 && res.data && res.data.length > 0) {
        var amount = 0;
        $.each(res.data, function (i, val) {
            amount += parseFloat(val.money);
            param.tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.oid) + '</td><td>' + _ellipsisAccount(val.inAccount) + '</td>' +
                '<td>' + val.money + '</td><td>' + (val.tradeCode ? val.tradeCode : "") + '</td><td>' + val.createtime + '</td><td>' + (val.reporttime ? val.reporttime : "") + '</td>';
            if (_checkObj(val.remark).length > 6) {
                param.tr +=
                    '<td><a    class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + _showRemarkNewPay(val.remark) + '">'
                    + _checkObj(val.remark).substring(0, 6) + "..."
                    + '</a></td>';
            } else {
                param.tr +=
                    '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + val.remark + '">'
                    + _checkObj(val.remark)
                    + '</a></td>';
            }
            if (val.chkRemark) {
                param.tr += '<td>' + val.chkRemark + '</td>';
            } else {
                param.tr += '<td></td>';
            }
            param.tr += '</tr>';
        });
        param.amount = amount;
        if (incomeCurrentPageSum) {
            param.trs += '<tr>' +
                '<td id="currentCount_alipayUnFail" colspan="2">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(param.amount).toFixed(3) + '</td>' +
                '<td colspan="9"></td>' +
                '</tr>';
        } else {
            param.trs += '<tr>' +
                '<td id="currentCount_alipayUnFail" colspan="15">小计：统计中..</td></tr>';
        }
        if (incomeAllRecordSum) {
            param.trs += '<tr><td id="currentCountTotal_alipayUnFail" colspan="2">总共：统计中..</td>' +
                '<td id="currentSumTotal_alipayUnFail" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">' + parseFloat(res.page.header.other).toFixed(3) + '</td>' +
                '<td colspan="9"></td>' +
                '</tr>';
        } else {
            param.trs += '<tr><td id="currentCountTotal_alipayUnFail" colspan="15">总共：统计中..</td></tr>';
        }
    }
    return param;
}
function _renderAlipayCanceled(res, param) {
    if (res.status == 1 && res.data && res.data.length > 0) {
        var amount = 0;
        $.each(res.data, function (i, val) {
            amount += parseFloat(val.money);
            param.tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.oid) + '</td><td>' + val.level + '</td>' +
                '<td>' + _ellipsisAccount(val.inAccount) + '</td><td>' + val.userName + '</td><td>' + val.code + '</td>' +
                '<td>' + val.money + '</td><td>' + val.createtime + '</td><td>' + val.admintime + '</td>';
            if (_checkObj(val.remark).length > 6) {
                param.tr +=
                    '<td><a    class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + _showRemarkNewPay(val.remark) + '">'
                    + _checkObj(val.remark).substring(0, 6) + "..."
                    + '</a></td>';
            } else {
                param.tr +=
                    '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + val.remark + '">'
                    + _checkObj(val.remark)
                    + '</a></td>';
            }
            if (val.chkRemark) {
                param.tr += '<td>' + val.chkRemark + '</td>';
            } else {
                param.tr += '<td></td>';
            }
            param.tr += '</tr>';
        });
        param.amount = amount;
        if (incomeCurrentPageSum) {
            param.trs += '<tr>' +
                '<td id="currentCount_alipayCanceled" colspan="5">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(param.amount).toFixed(3) + '</td>' +
                '<td colspan="9"></td>' +
                '</tr>';
        } else {
            param.trs += '<tr>' +
                '<td id="currentCount_alipayCanceled" colspan="15">小计：统计中..</td></tr>';
        }
        if (incomeAllRecordSum) {
            param.trs += '<tr><td id="currentCountTotal_alipayCanceled" colspan="5">总共：统计中..</td>' +
                '<td id="currentSumTotal_alipayCanceled" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;">' + parseFloat(res.page.header.other).toFixed(3) + '</td>' +
                '<td colspan="9"></td>' +
                '</tr>';
        } else {
            param.trs += '<tr><td id="currentCountTotal_alipayCanceled" colspan="15">总共：统计中..</td></tr>';
        }
    }
    return param;
}
/** 正在匹配重置条件 */
function _resetConditions(type) {
    /*
	 * if (type == 'alipayToMatch_order_flow') {
	 * _initialApliPayHandicaps('alipayToMatch');
	 * $('#member_alipayToMatch').val(''); $('#orderNo_alipayToMatch').val('');
	 * $('#fromMoney_alipayToMatch').val('');
	 * $('#toMoney_alipayToMatch').val('');
	 * $('#alipayNumber_alipayToMatch').val('');
	 * _datePickerForAll($('#time_alipayToMatch'));
	 * $('#flowNo_alipayToMatch').val(''); _searchOrderDetailDisplay();
	 * _searchFlowDetailDisplay(); } else {
	 */
	
        if(type=='alipayToMatch'||type=='alipayFail'){
        	$('#orderNo_' + type).val('');
            
        }else{
        	$('#inOrderNo_' + type).val('');
        	 $('#outOrderNo_' + type).val('');
             $('#toMember_' + type).val('');
             $("input[name='toHandicapRadio_"+searchAlipayType+"']").attr("checked",false);
             $("input[name='toHandicapRadio_"+searchAlipayType+"']")[0].checked = true;
        }
        
        $('#member_' + type).val('');
        _datePickerForAll($('#time_' + type));
       _initialApliPayHandicaps(type);
     
       if(searchAlipayType=='alipayToMatch'||searchAlipayType=='alipayFail'){// 正在匹配
                   _searchAlipayInByStatus2();
               }else{// 成功记录 ，进行中
               	_searchAlipayInByStatus();
               }
       

    // }
}
function _addTimeSelect(type) {
    /*
	 * if(type != 'alipayToMatch'){ var opt = '<option selected="selected"
	 * value="0" >不刷新</option><option value="15">15秒</option><option
	 * value="30">30秒</option>' + '<option value="60">60秒</option><option
	 * value="120">120秒</option><option value="180">180秒</option>';
	 * $('#autoUpdateTimeAlipayIn').empty().append(opt); }else{
	 */
        var opt = '<option  value="0" >不刷新</option><option  selected="selected" value="15">15秒</option><option  value="30">30秒</option>' +
            '<option  value="60">60秒</option><option  value="120">120秒</option><option  value="180">180秒</option>';
        $('#autoUpdateTimeAlipayIn').empty().append(opt);
   // }

}
$('#autoUpdateTimeAlipayIn').unbind().bind('change', function () {
    _searchIncomeAuditAlipayInTimeTask2(searchAlipayType);
});
function _searchAliSummaryMatchedByFilter() {
    var handicap = $('#handicap_' + searchAlipayType).val();
    var level = $('#level_' + searchAlipayType).val();
    var member = $('#member_' + searchAlipayType).val();
    var aliAccount = $('#alipayNumber_' + searchAlipayType).val();
    var startAndEnd = $("#timeScope_" + searchAlipayType).val();
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
    if ($('#fromMoney_' + searchAlipayType).val()) {
        fromMoney = $('#fromMoney_' + searchAlipayType).val();
    }
    var toMoney = null;
    if ($('#toMoney_' + searchAlipayType).val()) {
        toMoney = $('#toMoney_' + searchAlipayType).val();
    }
    var orderNo = '';
    if ($('#orderNo_' + searchAlipayType).val()) {
        if ($('#orderNo_' + searchAlipayType).val().indexOf('%') >= 0)
            orderNo = $.trim($('#orderNo_' + searchAlipayType).val().replace(new RegExp(/%/g), '?'));
        else
            orderNo = $.trim($('#orderNo_' + searchAlipayType).val());
    }
    var CurPage = $("#" + searchAlipayType + "_pageFoot").find(".Current_Page").text();
    if (!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    // (aliAccount=='全部'||aliAccount=='请选择')?"":$.trim(aliAccount)
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
        "pageNo": CurPage,"type":1,
        "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : 10
    };
    $.ajax({
        type: 'POST', contentType: 'application/json;charset=UTF-8',
        url: '/r/aliIn/findPage',
        data: JSON.stringify(data), dataType: 'json',
        success: function (res) {
            if (res) {
                $('#tbody_' + searchAlipayType).empty();
                var tr = '', trs = '', amount = 0, idList = [];
                if (res.status == 1 && res.data) {
                    $(res.data).each(function (i, val) {
                        idList.push({'id': val.accountId});
                        amount += parseFloat(val.amount);
                        // "<td><a class='bind_hover_card breakByWord'
						// data-toggle='accountInfoHover" + val.toId + "'
						// data-placement='auto right' data-trigger='hover' >"
                        // + _ellipsisForBankName(_checkObj(val.account))
                        // + "</a></td>" +
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
                    trs += '<tr><td id="CurrentCount' + searchAlipayType + '" colspan="5">小计：统计中..</td>' +
                        '<td bgcolor="#579EC8" style="color:white;width:  auto;">' + parseFloat(amount).toFixed(3) + '</td>' +
                        '<td colspan="15"></td></tr>';
                    trs += '<tr><td id="AllCount' + searchAlipayType + '" colspan="5">总共：统计中..</td>' +
                        '<td id="TotalSum' + searchAlipayType + '" bgcolor="#D6487E" style="color:white;width:  auto;">统计中..</td>' +
                        '<td colspan="15"></td></tr>';
                    $('#tbody_' + searchAlipayType).empty().html(tr).append(trs);
                    showPading(res.page, searchAlipayType + '_pageFoot', _searchAliSummaryMatchedByFilter);
                    _findCount(data, searchAlipayType);
                    _findSum(data, searchAlipayType);
                } else {
                    showPading(res.page, searchAlipayType + '_pageFoot', _searchAliSummaryMatchedByFilter);
                }
                $("[data-toggle='popover']").popover();
                // 加载账号悬浮提示
                loadHover_accountInfoHover(idList);
            } else {
                showPading(null, searchAlipayType + '_pageFoot', _searchAliSummaryMatchedByFilter);
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
                showPading(res.page, type + '_pageFoot', _searchAliSummaryMatchedByFilter);
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
/** 太长的商号省略显示 */
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
/**
 * 初始化层级
 * 
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
    $('#level_aliMatchedOrders').empty().html(opt);
    $('#level_aliMatchedOrders').trigger('chosen:updated');
    $('#level_aliMatchedOrders_chosen').prop('style', 'width:78%');

}
function _initialHandicap4Ali(type) {
    // 可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        // allow_single_deselect:true,
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
(function () {
    if (aliMatchedOrders_li) {
        $('#aliMatchedOrders_li').show();
    } else {
        $('#aliMatchedOrders_li').hide();
    }
})();
// 点击ul-li事件 正在匹配，进行中，成功记录，失败记录
function _initialSearTypeAlipay(type) {
        searchAlipayType = type;// 赋值类型变量：正在匹配，进行中，成功记录，失败记录
        _datePickerForAll($('#time_' + type));// 初始化查询时间控件
        _addTimeSelect(type);// 初始化查询刷新时间控件
        $('#freshAlipayInLi').show();// 显示初始化查询刷新时间控件
        _initialApliPayHandicaps(type);// 初始化查询盘口
        _handicap_change();// 盘口选择后初始化层级
        _toHandicapRadioChange();//收款人盘口 单选框 选择事件
        _fromHandicapRadioChange();//入款会员盘口 单选框 选择事件
        _searchIncomeAuditAlipayInTimeTask2(type);// 定时刷新任务
        if(searchAlipayType=='alipayToMatch'||searchAlipayType=='alipayFail'){// 正在匹配
            _searchAlipayInByStatus2();
        }else{// 成功记录 ，进行中
        	_searchAlipayInByStatus();
        }
}
$('#handicap_aliMatchedOrders').change(function () {
    _initalLevel(this.value, 'aliMatchedOrders');
    _searchAliSummaryMatchedByFilter();
});
$('#level_aliMatchedOrders').change(function () {
    _searchAliSummaryMatchedByFilter();
});
// tr 点击事件
function _trClickEvent(id, oid, inAccount, device) {
    _disPlayTrInnerTab(id, oid, inAccount, device);
}
function _disPlayTrInnerTab(id, oid, inAccount, device) {
    if ($('.afterTr') && $('.afterTr').length > 0) {
        $('.afterTr').each(function () {
            $(this).remove();
        });
    }

    if (!inAccount || !id) {
        $('#aliIn_outer_btn').show();
        $('#aliIn_inner_btn').hide();
        return;
    }
    var tr_id = '#tr_' + id;
    var className = $(tr_id).find('td:nth-child(2)').attr('class');
    if (className && className.indexOf('tdBgcolor') > -1) {
        // 点击 关闭
        var saveAccountIdBeforeClose = window.localStorage.getItem('saveAccountIdBeforeClose');
        if (saveAccountIdBeforeClose) {
            window.localStorage.removeItem('searchAlipayFlowOrderId');
            window.localStorage.removeItem('searchAlipayFlowOrderOid');
            window.localStorage.removeItem('searchAlipayFlowOrderInAccount');
            window.localStorage.removeItem('searchAlipayFlowOrderDevice');
        }
        _searchalipayAccount();
    } else {
        // 点击 展开
        window.localStorage.setItem('searchAlipayFlowOrderId', id);
        window.localStorage.setItem('searchAlipayFlowOrderOid', oid);
        window.localStorage.setItem('searchAlipayFlowOrderInAccount', inAccount);
        window.localStorage.setItem('searchAlipayFlowOrderDevice', device);
    }
    window.localStorage.setItem('saveAccountIdBeforeClose', id);
    _showOutOrInner();
}
function _showOutOrInner() {
    var id = window.localStorage.getItem('searchAlipayFlowOrderId');
    var saveAccountIdBeforeClose = window.localStorage.getItem('saveAccountIdBeforeClose');
    var tr_id = '#tr_' + saveAccountIdBeforeClose;
    if (!id) {
        // 关闭
        searchOutInLevelFlag = true;
        if (saveAccountIdBeforeClose) {
            $(tr_id).find('td:nth-child(2)').prop('class', 'clickedTd');
        }
        $('#aliIn_outer_btn').show();
        $('#aliIn_inner_btn').hide();
    } else {
        if ($('#alipayToMatch_tbody') && $('#alipayToMatch_tbody').html().length > 0) {
            // 打开
            searchOutInLevelFlag = false;
            $('#aliIn_outer_btn').hide();
            $('#aliIn_inner_btn').show();
            var className2Td = $(tr_id).find('td:nth-child(2)').prop('class');
            if (className2Td && className2Td.indexOf('clickedTd') > -1) {
                $(tr_id).find('td:nth-child(2)').prop('class', '');
            }
            _generateInnerTrForOrder();
            $(tr_id).find('td:nth-child(2)').prop('class', 'tdBgcolor');
            $(tr_id).siblings().not('.detail-row').find('td:nth-child(2)').prop('class', '');
            _searchOrderDetailDisplay();
            _searchFlowDetailDisplay();

            $(tr_id).closest('tr').next().toggleClass('open');// 内层的tab打开
            $(tr_id).siblings().not('.detail-row').toggleClass('siblingsDisplay');
            // $(tr_id).closest('tr').next().siblings().each(function () {
            // if($(this).prop('id')=='tr_'+id ||
			// $(this).prop('id')=='footer_tr1' ||
			// $(this).prop('id')=='footer_tr2'){
            // }else{
            // $(this).toggleClass('siblingsDisplay');
            // }
            // });//该tr之下的其他tr隐藏
            $(tr_id).find(ace.vars['.icon']).toggleClass('fa-angle-double-down').toggleClass('fa-angle-double-up');
        } else {
            $('#aliIn_outer_btn').show();
            $('#aliIn_inner_btn').hide();
        }

    }
}
// order 生成内层公共的tr table
function _generateInnerTrForOrder() {
    var inAccount = window.localStorage.getItem('searchAlipayFlowOrderInAccount');
    var id = window.localStorage.getItem('searchAlipayFlowOrderId');
    var device = window.localStorage.getItem('searchAlipayFlowOrderDevice');
    if (!inAccount || !id || !device) {
        return;
    }
    if ($('.afterTr') && $('.afterTr').length > 0) {
        $('.afterTr').remove();
    }
    var tr_id = '#tr_' + id;
    if (!$('#afterTr_' + id) || $('#afterTr_' + id).length == 0) {
        var trs =
            '<tr id="afterTr_' + id + '" class="detail-row afterTr"><td colspan="8">' +
            '<div style="padding-top: 0px" class="table-detail">' +
            '<div><table class="table  table-bordered table-hover no-margin-bottom"><thead><tr>' +
            '<th>选择</th><th class="modifyHandicap">盘口</th><th>会员账号</th><th class="modifyInAmount">金额</th><th>订单号</th><th>提单时间</th><th>备注</th><th>收款理由</th> <th>操作</th>' +
            '</tr></thead><tbody id="order_tbody_' + id + '"></tbody></table>' +
            '<div id="footPage_innerOrder_' + id + '"></div>' +
            '</div>' +
            '<div id="match_aliIn_div' + id + '" style="display: none;" class="row hr-12 match_aliIn_div">' +
            '<button onclick="_beforeInnerMatch(' + id + ',\'' + inAccount + '\');" type="button" class="btn btn-sm btn-purple">' +
            '<i class="ace-icon fa fa-check-circle bigger-80"></i>' +
            '<span>匹配</span>' +
            '</button>' +
            '</div>' +
            '<div><table class="table  table-bordered table-hover no-margin-bottom"><thead>' +
            '<tr><th>选择</th><th class="modifyHandicap">盘口</th><th>流水号</th><th class="modifyInAmount">金额</th><th>支付时间</th><th>最新抓取时间</th>' +
            '<th>备注</th><th class="modifyThChRemark">收款理由</th><th>操作</th></tr></thead>' +
            '<tbody id="flow_tbody_' + id + '"></tbody>' +
            '</table><div id="footPage_innerFlow_' + id + '"></div>' +
            '</div>' +
            '</td></tr>';
        $(tr_id).after(trs);
    }
    if (matchOrderRight) {
        $('div.match_aliIn_div').show();
    }
}
// 查订单
function _searchOrderDetailDisplay() {
    var oid = window.localStorage.getItem("searchAlipayFlowOrderOid");
    var id = window.localStorage.getItem('searchAlipayFlowOrderId');
    var inAccount = window.localStorage.getItem('searchAlipayFlowOrderInAccount');
    var device = window.localStorage.getItem('searchAlipayFlowOrderDevice');
    if (!inAccount || !oid || !id || !device) {
        return;
    }
    // 获取时间段
    var startAndEndTime = $("#time_alipayToMatch").val(), timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" ~ ");
        timeStart = startAndEnd[0];
        timeEnd = startAndEnd[1];
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
    var code = $('#orderNo_alipayToMatch').val();
    var userName = $('#member_alipayToMatch').val();
    var moneyFrom = $('#fromMoney_alipayToMatch').val();
    var moneyEnd = $('#toMoney_alipayToMatch').val();
    // 当前页码
    var CurPage = $("#footPage_innerOrder_" + id).find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    var data = {
        "oid": oid,
        "type": 1,
        "inAccount": inAccount,
        "timeStart": new Date(timeStart).getTime(),
        "timeEnd": new Date(timeEnd).getTime(),
        "pageNo": CurPage,
        "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10",
        "code": code,
        "userName": userName,
        "moneyStart": moneyFrom,
        "moneyEnd": moneyEnd,
        "device": device
    };

    var url = "/newpay/find10ByCondition";
    $.ajax({
        type: "post",
        url: url,
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                var tr = '', trs = '', amount = 0;
                if (res.status == 1) {
                    if (res.data && res.data.length > 0) {
                        $.each(res.data, function (i, val) {
                            amount += val.money;
                            tr += '<tr onclick="_selectOrderToMatch(this);"><td><input click_val="1" name="select_order_input" value="' + val.inId + '" type="radio"></td>' +
                                '<td oid_val ="' + val.oid + '">' + _showHandicapNameByIdOrCode(val.oid) + '</td><td  uid_val="' + (val.uid ? val.uid : "") + '">' + (val.userName ? val.userName : "") + '</td><td>' + val.money + '</td>' +
                                '<td>' + val.code + '</td><td>' + val.createtime + '</td>';
                            // _checkObj(val.remark)+'</td><td>';
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td>'
                                        + '<a id="order_remark' + val.inId + '"  class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _showRemarkNewPay(val.remark) + '">'
                                        + _checkObj(val.remark).substring(0, 5)
                                        + '</a>'
                                        + '</td>';
                                } else {
                                    tr += '<td>'
                                        + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark + '">'
                                        + _checkObj(val.remark)
                                        + '</a>'
                                        + '</td>';
                                }
                            } else {
                                tr += "<td></td>";
                            }
                            if (val.chkRemark) {
                                tr += '<td>' + val.chkRemark + '</td>';
                            } else {
                                tr += '<td></td>';
                            }
                            tr += '<td>';
                            if (addRemarkRightFlag) {
                                tr += '<button id="addRemarkRightBTN1"  onclick="event.stopPropagation();_beforeAddRemark(' + val.inId + ',' + val.oid + ',1);"  class="btn btn-xs btn-white btn-info" type="button" >' +
                                    '<i class="ace-icon fa fa-comments bigger-120">备注</i>' +
                                    '</button>';
                            }
                            if (cancelIncomeReqFlag) {
                                tr += '<button id="concealOrderRightBTN"   class=" btn btn-xs btn-white btn-danger" type="button" onclick="event.stopPropagation();_beforeCancel(' + val.inId + ',' + val.oid + ',\'' + val.code + '\');" >' +
                                    '<i class="ace-icon fa fa-remove bigger-120 red">取消</i>' +
                                    '</button>';
                            }
                            tr += '</td></tr>';
                        });
                        if (incomeCurrentPageSum) {
                            trs += '<tr><td id="currentCount_order_tbody_' + id + '" colspan="3">小计：统计中..</td>';
                            trs += '<td bgcolor="#579EC8"  style="color: white;">' + parseFloat(amount).toFixed(3) + '</td>';
                            trs += '<td colspan="10"></td></tr>';
                        } else {
                            trs += '<tr><td id="currentCount_order_tbody_' + id + '" colspan="15">小计：统计中..</td></tr>';
                        }
                        if (incomeAllRecordSum) {
                            trs += '<tr><td id="currentCountTotal_order_tbody_' + id + '" colspan="3">总共：统计中..</td>';
                            trs += '<td  bgcolor="#D6487E" style="color: white;">' + parseFloat(res.page.header.other).toFixed(3) + '</td>';
                            trs += '<td colspan="10"></td></tr>';
                        } else {
                            trs += '<tr><td id="currentCountTotal_order_tbody_' + id + '" colspan="15">总共：统计中..</td></tr>';
                        }
                    }
                } else {
                    $.gritter.add({
                        time: 10000, class_name: '', title: '系统消息', text: res.message,
                        sticky: false, image: '../images/message.png'
                    });
                }
                $('#order_tbody_' + id).empty().html(tr).append(trs);
                if (res.status == 1 && res.data && res.data.length > 0 && res.page) {
                    $('#currentCount_order_tbody_' + id).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条');
                    $('#currentCountTotal_order_tbody_' + id).empty().text('合计：' + res.page.totalElements + '条');
                }
                showPading(res.page, "footPage_innerOrder_" + id, _searchOrderDetailDisplay);
                $("[data-toggle='popover']").popover();
            }
        }
    });
}
function _selectOrderToMatch(obj) {
    var click_val = $(obj).find('input[name="select_order_input"]').attr('click_val');
    $(obj).siblings().each(function () {
        $(this).find('input[name="select_order_input"]').prop('checked', '');
        $(this).find('input[name="select_order_input"]').attr('click_val', 1);
    });
    if (click_val == 1) {
        $(obj).find('input[name="select_order_input"]').attr('click_val', 2);
        $(obj).find('input[name="select_order_input"]').prop('checked', 'checked');
    } else {
        $(obj).find('input[name="select_order_input"]').attr('click_val', 1);
        $(obj).find('input[name="select_order_input"]').prop('checked', '');
    }
}
// 取消订单
function _beforeCancel(id, oid, code) {
    $('#cancel_oid').val(oid);
    $('#cancel_id').val(id);
    $('#cancel_remark').val('');
    $('#cancel_code').val(code);
    $('#cancel_modal').modal('show');
}
function _afterCancel() {
    $('#cancel_oid').val('');
    $('#cancel_id').val('');
    $('#cancel_remark').val('');
    $('#cancel_code').val('');
    $('#cancel_modal').modal('hide');
}
function _confirmConcel() {
    var remark = $('#cancel_remark').val();
    if (!remark) {
        $('#cancel_remarkPrompt').show(10).delay(500).hide(10);
        return;
    }
    var data = {
        "id": $('#cancel_id').val(),
        "oid": $('#cancel_oid').val(),
        "remark": remark,
        "code": $('#cancel_code').val()
    };
    $.ajax({
        type: "post",
        url: '/newpay/cancel',
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 5000, class_name: '', title: '系统消息', text: res.message,
                    sticky: false, image: '../images/message.png'
                });
                if (res.status == 1) {
                    _afterCancel();
                    _searchOrderDetailDisplay();
                }
            }
        }
    });
}
// 查流水
function _searchFlowDetailDisplay() {
    var oid = window.localStorage.getItem("searchAlipayFlowOrderOid");
    var id = window.localStorage.getItem('searchAlipayFlowOrderId');
    var inAccount = window.localStorage.getItem('searchAlipayFlowOrderInAccount');
    var device = window.localStorage.getItem('searchAlipayFlowOrderDevice');
    if (!inAccount || !oid || !id || !device) {
        return;
    }
    // 获取时间段
    var startAndEndTime = $("#time_alipayToMatch").val();
    var timeStart = '', timeEnd = '';
    if (startAndEndTime) {
        var startAndEnd = startAndEndTime.split(" ~ ");
        timeStart = startAndEnd[0];
        timeEnd = startAndEnd[1];
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
    // 当前页码
    var CurPage = $("#footPage_innerFlow_" + id).find(".Current_Page").text();
    CurPage = CurPage ? CurPage > 0 ? CurPage - 1 : 0 : 0;
    var data = {
        "oid": oid,
        "type": 1,
        "inAccount": inAccount,
        "timeStart": new Date(timeStart).getTime(),
        "timeEnd": new Date(timeEnd).getTime(),
        "pageNo": CurPage,
        "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : "10",
        "device": device,
        "flowNo": $.trim($('#flowNo_alipayToMatch').val())
    };
    var url = "/newpay/find11ByCondition";
    $.ajax({
        type: "post",
        url: url,
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                var tr = '', trs = '', amount = 0;
                if (res.status == 1) {
                    if (res.data && res.data.length > 0) {
                        $.each(res.data, function (i, val) {
                            amount += val.money;
                            tr += '<tr onclick="_selectFlowToMatch(this);" ><td><input click_val="1" name="select_flow_input" value="' + val.id + '" type="radio"></td>' +
                                '<td>' + _showHandicapNameByIdOrCode(val.oid) + '</td><td>' + _checkObj(val.tradeCode) + '</td><td>' + val.money + '</td>' +
                                '<td>' + val.createtime + '</td><td>' + val.reporttime + '</td>';
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td>'
                                        + '<a id="flow_remark' + val.id + '" class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _showRemarkNewPay(val.remark) + '">'
                                        + _checkObj(val.remark).substring(0, 5)
                                        + '</a>'
                                        + '</td>';
                                } else {
                                    tr += '<td>'
                                        + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark + '">'
                                        + _checkObj(val.remark)
                                        + '</a>'
                                        + '</td>';
                                }
                            } else {
                                tr += "<td></td>";
                            }
                            if (val.chkRemark) {
                                tr += '<td>' + (val.chkRemark?val.chkRemark:'')+ '</td>';
                            } else {
                                tr += '<td></td>';
                            }
                            tr += '<td>';
                            if (addRemarkRightFlag) {
                                tr += '<button onclick="event.stopPropagation();_beforeAddRemark(' + val.id + ',' + val.oid + ',2);"  class="btn btn-xs btn-white btn-info" type="button" >' +
                                    '<i class="ace-icon fa fa-comments">备注</i>' +
                                    '</button>';
                            }
                            if (addOrderRightFlag) {
                                tr += '<button  onclick="event.stopPropagation();_addOrder(' + val.oid + ',\'' + val.inAccount + '\',\'' + (val.userName ? val.userName : '') + '\',' + val.money + ');" class="btn btn-xs btn-purple btn-white" type="button">' +
                                    '<i class="ace-icon fa fa-pencil ">补提单</i>' +
                                    '</button>';
                            }
                            tr += '</td></tr>';
                        });
                        if (incomeCurrentPageSum) {
                            trs += '<tr><td id="currentCount_flow_tbody_' + id + '" colspan="3">小计：统计中..</td>';
                            trs += '<td bgcolor="#579EC8" style="color: white;">' + parseFloat(amount).toFixed(3) + '</td>';
                            trs += '<td colspan="10"></td></tr>';
                        } else {
                            trs += '<tr><td id="currentCount_flow_tbody_' + id + '" colspan="15">小计：统计中..</td></tr>';
                        }
                        if (incomeAllRecordSum) {
                            trs += '<tr><td id="currentCountTotal_flow_tbody_' + id + '" colspan="3">总共：统计中..</td>';
                            trs += '<td bgcolor="#D6487E" style="color: white;">' + parseFloat(res.page.header.other).toFixed(3) + '</td>';
                            trs += '<td colspan="10"></td></tr>';
                        } else {
                            trs += '<tr><td id="currentCountTotal_flow_tbody_' + id + '" colspan="15">总共：统计中..</td></tr>';
                        }
                    }
                } else {
                    $.gritter.add({
                        time: 10000, class_name: '', title: '系统消息', text: res.message,
                        sticky: false, image: '../images/message.png'
                    });
                }
                $('#flow_tbody_' + id).empty().html(tr).append(trs);
                if (res.status == 1 && res.data && res.data.length > 0 && res.page) {
                    $('#currentCount_flow_tbody_' + id).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条');
                    $('#currentCountTotal_flow_tbody_' + id).empty().text('合计：' + res.page.totalElements + '条');
                    $('#toMatchNumber').text(res.page.totalElements);
                }
                showPading(res.page, "footPage_innerFlow_" + id, _searchFlowDetailDisplay);
                $("[data-toggle='popover']").popover();
            }
        }
    });
}
function _selectFlowToMatch(obj) {
    var click_val = $(obj).find('input[name="select_flow_input"]').attr('click_val');
    $(obj).siblings().each(function () {
        $(this).find('input[name="select_flow_input"]').prop('checked', '');
        $(this).find('input[name="select_flow_input"]').attr('click_val', 1);
    });
    if (click_val == 1) {
        $(obj).find('input[name="select_flow_input"]').attr('click_val', 2);
        $(obj).find('input[name="select_flow_input"]').prop('checked', 'checked');
    } else {
        $(obj).find('input[name="select_flow_input"]').attr('click_val', 1);
        $(obj).find('input[name="select_flow_input"]').prop('checked', '');
    }
}
// type =1 订单备注 2 流水备注
function _beforeAddRemark(id, oid, type) {
    $('#addRemark_Id').val(id);
    $('#addRemark_Oid').val(oid);
    $('#addRemark_remark').val('');
    $('#addRemark_Type').val(type);
    $('#addRemark_modal').modal('show');
}
function _afterAddRemark() {
    $('#addRemark_Id').val('');
    $('#addRemark_Oid').val('');
    $('#addRemark_Type').val('');
    $('#addRemark_modal').modal('hide');
}
function _confirmAddRemark() {
    var remark = $('#addRemark_remark').val();
    if (!remark) {
        $('#addRemark_remarkPrompt').show(10).delay(3000).hide(10);
        return;
    }
    var url = '', type = $('#addRemark_Type').val();
    if (type == 1) {
        // 订单备注
        url = '/newpay/modifyRemark';
    } else {
        // 流水备注
        url = '/newpay/addRemark';
    }
    var data = {"oid": $('#addRemark_Oid').val(), "id": $('#addRemark_Id').val(), "remark": remark};
    $.ajax({
        type: "post",
        url: url,
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 5000, class_name: '', title: '系统消息', text: res.message,
                    sticky: false, image: '../images/message.png'
                });
                if (type == 1) {
                    _searchOrderDetailDisplay();
                } else {
                    _searchFlowDetailDisplay();
                }
                _afterAddRemark();
            }
        }
    });
}
// 补提单
function _addOrder(oid, inAccount, userName, money) {
    $('#makeUpMemberAccount').val(userName);
    $('#makeUpRemark').val('');
    $('#makeUpOid').val(_showHandicapNameByIdOrCode(oid));
    $('#makeUpAmount').val(money);
    $('#makeUpOid').attr("makeUpOid_val", oid);
    $('#makeUpAccount').val(inAccount);
    $('#makeUpFlow').modal('show');
}
function _afterAddOrder() {
    $('#makeUpMemberAccount').val("");
    $('#makeUpRemark').val('');
    $('#makeUpOid').val("");
    $('#makeUpAmount').val("");
    $('#makeUpAccount').val("");
    $('#makeUpFlow').modal('hide');
}
function _confirmAddOrder() {
    if ($('#makeUpMemberAccount').val() == '无' || !$('#makeUpMemberAccount').val()) {
        $('#makeUpPrompt').text('填写会员名!');
        $('#makeUpPrompt').show(10).delay(3000).hide(10);
        return;
    }
    var remark = $('#makeUpRemark').val();
    if (!remark) {
        $('#makeUpPrompt').text('填写备注!');
        $('#makeUpPrompt').show(10).delay(3000).hide(10);
        return;
    }
    var data = {
        "oid": $('#makeUpOid').attr("makeUpOid_val"),
        "userName": $('#makeUpMemberAccount').val() == '无' ? "" : $('#makeUpMemberAccount').val(),
        "money": $('#makeUpAmount').val(),
        "type": 1,
        "account": $('#makeUpAccount').val(),
        "remark": remark,
        "createTime": new Date().getTime()
    };
    $.ajax({
        type: "post",
        url: '/newpay/putPlus',
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 5000, class_name: '', title: '系统消息', text: res.message,
                    sticky: false, image: '../images/message.png'
                });
                if (res.status == 1) {
                    _afterAddOrder();
                    _searchFlowDetailDisplay();
                    _searchOrderDetailDisplay();
                }
            }
        }
    });

}
// 匹配之前
function _beforeInnerMatch(id, inAccount) {
    var order_checked = $('#order_tbody_' + id).find('input[name="select_order_input"]:checked');
    var flow_checked = $('#flow_tbody_' + id).find('input[name="select_flow_input"]:checked');
    if (!order_checked || order_checked.length == 0) {
        $.gritter.add({
            time: 5000, class_name: '', title: '系统消息', text: '请选择订单',
            sticky: false, image: '../images/message.png'
        });
        return;
    }

    if (!flow_checked || flow_checked.length == 0) {
        $.gritter.add({
            time: 5000, class_name: '', title: '系统消息', text: '请选择流水',
            sticky: false, image: '../images/message.png'
        });
        return;
    }

    var amount1 = $(order_checked).parent().parent().find('td:nth-child(4)').text();
    var time1 = $(order_checked).parent().parent().find('td:nth-child(6)').text();
    var orderNo1 = $(order_checked).parent().parent().find('td:nth-child(5)').text();
    $('#sysRequestBody_ali_amount').text(amount1);
    $('#sysRequestBody_ali_orderNo').text(orderNo1);
    $('#sysRequestBody_ali_time').text(time1);

    var amount2 = $(flow_checked).parent().parent().find('td:nth-child(4)').text();
    var time2 = $(flow_checked).parent().parent().find('td:nth-child(5)').text();
    var summary = $(flow_checked).parent().parent().find('td:nth-child(8)').text();
    var flowNo = $(flow_checked).parent().parent().find('td:nth-child(3)').text();
    $('#bankFlowBody_ali_amount').text(amount2);
    $('#bankFlowBody_ali_flowNo').text(flowNo);
    $('#bankFlowBody_ali_time').text(time2);
    $('#bankFlowBody_ali_chkRemark').text(summary);
    if (amount1 != amount2) {
        var member = $(order_checked).parent().parent().find('td:nth-child(3)').text();
        $('#inconsistentAmountMatchMemberAccount').val(member);// 会员账号
        $('#inconsistentAmountMatchAmount').val(amount2);// 存入金额
        $('#inconsistentAmountMatchBankAccount').val(inAccount);// 收款支付宝
        $('#inconsistentAmountMatchBalanceGap').val(amount1 - amount2);// 差额

        $('#inconsistentAmountMatchInfo1').show();
        $('#inconsistentAmountMatchInfo2').show();
        $('#inconsistentAmountMatchInfo3').show();
        $('#commonMatchInfo').hide();
    } else {
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
    $('#inconsistentAmountMatchRemark').val('');
    $('#matchRemark').val('');
    $('#toMatchInfo_ali').modal('hide');
    _searchOrderDetailDisplay();
    _searchFlowDetailDisplay();
}
function _confirmMatch() {
    var oid = $('#oid_ali_match').val();
    var logId = $('#logId_ali_match').val();
    var inId = $('#inId_ali_match').val();
    var uid = $('#uid_ali_match').val();
    var amount1 = $('#sysRequestBody_ali_amount').text();
    var amount2 = $('#bankFlowBody_ali_amount').text();
    var flowNo = $('#tradingFlow_ali_match').val();
    var remark = '';
    if (parseFloat(amount2) != parseFloat(amount1)) {
        if (!$.trim($('#inconsistentAmountMatchRemark').val())) {
            $('#remarkPrompt').show(10).delay(3000).hide(10);
            return;
        }
        remark = $.trim($('#inconsistentAmountMatchRemark').val());
    } else {
        // if(!$.trim($('#matchRemark').val())){
        // $('#remarkPrompt').show(10).delay(3000).hide(10);
        // return;
        // }
        remark = $.trim($('#matchRemark').val());// 非必填
    }

    if (!oid || !logId || !inId || !uid) {
        $.gritter.add({
            time: 5000, class_name: '', title: '系统消息', text: '参数丢失！',
            sticky: false, image: '../images/message.png'
        });
        return;
    }
    var data = {"oid": oid, "logId": logId, "inId": inId, "uid": uid, "remark": remark, "tradingFlow": flowNo};
    $.ajax({
        type: "post",
        url: '/newpay/matching',
        dataType: 'json',
        async: false,
        data: JSON.stringify(data),
        contentType: 'application/json;charset=UTF-8',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 5000, class_name: '', title: '系统消息', text: res.message,
                    sticky: false, image: '../images/message.png'
                });
                if (res.status == 1) {
                    _afterMatch();
                }
            }
        }
    });
}
function _modifyStyle(){
    $('.modifyInAmountInput').attr('style','height: 32px; width: 29.5%;');
    $('.modifyInAmountInDiv').attr('style','height:32px;width:60%;');
    $('.modifyInAmountInDivInput').attr('style',' height: 32px; width: 180%;');

}
/**
 * 设置支付宝入款参数
 */
var showModalPatch=function(){
	    // 弹出窗口对象
		var APPpatchModal2='<div id="APPpatchModal2" class="modal fade">\
		<div class="modal-dialog modal-lg" style="width:1000px;height:600px;">\
		<div class="modal-content" >\
			<div class="modal-header no-padding text-center">\
				<div class="table-header"><button class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>支付宝入款配置</span></div>\
			</div>\
		<div class="modal-body" style="height:700px;">\
			 <h5><span class="red">注意：</span></h5>\
	        <h5><span class="red">1.不同盘口的会员可以匹配到同一个聊天室（注意：返利网兼职代收、代付不需要区分盘口）</span></h5>\
	        <h5><span class="red">2.如果出款没有完成，出款人就提前离开了聊天室，入款人可继续入款，并使用工具确认到账，或申请客服介入确认</span></h5>\
			<h5><span class="blue"><input type="checkbox" name="noUseBlackForAliIn"  />&nbsp;&nbsp;自动出款系统中的黑名单会员入款（姓名、银行卡号只要符合其中一项），不能使用聊天室入款方式</span></h5>\
			<h5><span class="blue">（注意：指的是出款管理——账号管理——出款银行卡页面的黑名单会员）</span></h5>\
			<h5><span class="blue">同一个支付宝账号每日累计最大入款金额不能超过&nbsp;&nbsp;<input  type="number" name="maxAmountForAliIn" class="input-sm" />&nbsp;&nbsp;元</span></h5>\
			 <h5><span class="blue">会员提交一笔入款，等待&nbsp;&nbsp;<input type="number" name="matchSecondForAliIn" class="input-sm" />&nbsp;&nbsp;秒内提交的会员出款单匹配聊天室，如果没有匹配到会员出款，就自动匹配返利兼职代收,</span></h5>\
			<h5><span class="blue"><input type="number" name="totalMatchSecondForAliIn" class="input-sm" />&nbsp;&nbsp;秒内如果还没有匹配到出款会员或兼职代收，系统提示入款会员系统繁忙稍后再试或更换其他入款通道</span></h5>\
			<h5><span class="blue">从系统匹配到入款任务进入聊天室开始&nbsp;&nbsp;<input type="number" name="incomeTimeOutMinute" class="input-sm" />&nbsp;&nbsp;分钟内（倒计时显示在聊天室）入款人没有点击【已确认付款】按钮</span></h5>\
			<h5><span class="blue">并且收款人没有点击【确认到账】，聊天室提示【继续入款】，【已确认付款】,【离开】（如果选择继续入款，重新倒数计时并弹提示）。</span></h5>\
			<h5><span class="blue">从系统匹配到入款任务进入聊天室开始&nbsp;&nbsp;<input type="number" name="incomeTimeOutTotalMinute" class="input-sm" />&nbsp;&nbsp;分钟内（倒计时不需显示在聊天室）入款人没有完成入款</span></h5>\
			<h5><span class="blue">并且收款人没有点击【确认到账】，聊天室提示【已确认付款】，【离开】（选择【已确认付款】继续留在聊天室并提示收款人尽快确认是否到账</span></h5>\
			<h5><span class="blue">选择【离开】将离开聊天室，系统会取消该笔入款单。如果该聊天室匹配了出款会员，那么该入款会员离开后，</span></h5>\
			<h5><span class="blue">系统会匹配新的入款会员或兼职代付进入聊天室）。</span></h5>\
			<hr><span class="blue"></span></hr>\
			<h5><span class="blue">返利网兼职的支付宝账号余额大于或等于&nbsp;&nbsp;<input type="number" name="balanceMaxForAliIn" class="input-sm" />&nbsp;&nbsp;元（“0”表示不限制），不能接代收任务</span></h5>\
	        <h5><span class="blue">返利网兼职的支付宝账号余额小于或等于信用额度的&nbsp;&nbsp;<input type="number" name="lessThanPercentForAliIn" class="input-sm" />&nbsp;&nbsp;%（“0”表示不限制）只能接会员代收任务</span></h5>\
	        <h5><span class="blue">返利网兼职支付宝最开始的&nbsp;&nbsp;<input type="number" name="firstOrderMaxForAliIn" class="input-sm" />&nbsp;&nbsp;笔(“0”表示不限制）代收任务不能超过&nbsp;&nbsp<input type="number" name="firstOrderMaxMoneyForAliIn" class="input-sm" />&nbsp;&nbsp元</span></h5>\
			<h5><span class="blue">返利网兼职代收时，最多可同时接&nbsp;&nbsp;<input type="number" name="rebateMaxOderForAliIn" class="input-sm" />&nbsp;&nbsp;个代收任务</span></h5>\
			</div>\
				<div class="col-sm-12 modal-footer">\
					<button class="btn btn-primary" type="button" onclick="updateSetting();">确认</button>\
					<button class="btn btn-danger" type="button" data-dismiss="modal">取消</button>\
				</div>\
			</div>\
		</div>\
	</div>';
	var $div = $(APPpatchModal2).appendTo($("body"));
	$div.modal("toggle");
	// 关闭窗口事件清除model
	$div.on('hidden.bs.modal', function () {
		$div.remove();
	});
	// 只能输入数字设置
	inputSet();
	
	// 查询支付宝入款参数填充弹出窗口
	$.ajax({
        type: "POST", url: '/r/set/findAliInConfig', data: {}, dataType: 'JSON', success: function (res) {
            if (res.status != 1) {
            	showMessageForFail("配置信息获取失败，请重新打开："+jsonObject.message);
            }
            var incomeAuditAliIn = res.data;
            if(incomeAuditAliIn.noUseBlackForAliIn==1){
            	$div.find("[name=noUseBlackForAliIn]").prop("checked",true);
            }else{
            	$div.find("[name=noUseBlackForAliIn]").prop("checked",false);
            }
            $div.find("[name=maxAmountForAliIn]").val(incomeAuditAliIn.maxAmountForAliIn);
            $div.find("[name=matchSecondForAliIn]").val(incomeAuditAliIn.matchSecondForAliIn);
            $div.find("[name=totalMatchSecondForAliIn]").val(incomeAuditAliIn.totalMatchSecondForAliIn);
            $div.find("[name=incomeTimeOutMinute]").val(incomeAuditAliIn.incomeTimeOutMinute);
            $div.find("[name=incomeTimeOutTotalMinute]").val(incomeAuditAliIn.incomeTimeOutTotalMinute);
            $div.find("[name=balanceMaxForAliIn]").val(incomeAuditAliIn.balanceMaxForAliIn);
            $div.find("[name=lessThanPercentForAliIn]").val(incomeAuditAliIn.lessThanPercentForAliIn);
            $div.find("[name=firstOrderMaxForAliIn]").val(incomeAuditAliIn.firstOrderMaxForAliIn);
            $div.find("[name=firstOrderMaxMoneyForAliIn]").val(incomeAuditAliIn.firstOrderMaxMoneyForAliIn);
            $div.find("[name=rebateMaxOderForAliIn]").val(incomeAuditAliIn.rebateMaxOderForAliIn);
        }
    });
	 
}

/**
 * 更新系统设置
 */
var updateSetting = function () {
    var paramArray = new Array()
    var object ={}
    var validate = new Array();
    var $divAPPpatchModal2 = $("#APPpatchModal2");
   
        var $noUseBlackForAliIn = $divAPPpatchModal2.find("[name=noUseBlackForAliIn]");
        var $maxAmountForAliIn = $divAPPpatchModal2.find("[name=maxAmountForAliIn]");
        var $matchSecondForAliIn = $divAPPpatchModal2.find("[name=matchSecondForAliIn]");
        var $totalMatchSecondForAliIn = $divAPPpatchModal2.find("[name=totalMatchSecondForAliIn]");
        var $incomeTimeOutMinute = $divAPPpatchModal2.find("[name=incomeTimeOutMinute]");
        var $incomeTimeOutTotalMinute = $divAPPpatchModal2.find("[name=incomeTimeOutTotalMinute]");
        var $balanceMaxForAliIn = $divAPPpatchModal2.find("[name=balanceMaxForAliIn]");
        var $lessThanPercentForAliIn = $divAPPpatchModal2.find("[name=lessThanPercentForAliIn]");
        var $firstOrderMaxForAliIn = $divAPPpatchModal2.find("[name=firstOrderMaxForAliIn]");
        var $firstOrderMaxMoneyForAliIn = $divAPPpatchModal2.find("[name=firstOrderMaxMoneyForAliIn]");
        var $rebateMaxOderForAliIn = $divAPPpatchModal2.find("[name=rebateMaxOderForAliIn]");
        
        // 校验
        var incomeTimeOutMinuteVal = parseInt($incomeTimeOutMinute.val())+1;
        var incomeTimeOutTotalMinuteVal = parseInt($incomeTimeOutTotalMinute.val())-1;
        validate.push({ele: $maxAmountForAliIn,name:"",minEQ: 0, maxEQ: 5000000});
        validate.push({ele: $matchSecondForAliIn,name:"",minEQ: 0, maxEQ: 5000000});
        validate.push({ele: $totalMatchSecondForAliIn,name:"",minEQ: 0, maxEQ: 5000000});
        validate.push({ele: $incomeTimeOutMinute, name: '从系统匹配到入款任务进入聊天室开始，第一个框须小于第二个框的值 ： ',  minEQ: 0, maxEQ: incomeTimeOutTotalMinuteVal});
        validate.push({ele: $incomeTimeOutTotalMinute, name: '从系统匹配到入款任务进入聊天室开始，第二个框须小于第一个框的值 ： ',  minEQ: incomeTimeOutMinuteVal, maxEQ: 500000});
        validate.push({ele: $balanceMaxForAliIn,name:"",minEQ: 0, maxEQ: 5000000});
        validate.push({ele: $lessThanPercentForAliIn,name:"",minEQ: 0, maxEQ: 5000000});
        validate.push({ele: $firstOrderMaxForAliIn,name:"",minEQ: 0, maxEQ: 5000000});
        validate.push({ele: $firstOrderMaxMoneyForAliIn,name:"",minEQ: 0, maxEQ: 5000000});
        validate.push({ele: $rebateMaxOderForAliIn,name:"",minEQ: 0, maxEQ: 5000000});
       
        if (!validateEmptyBatch(validate) || !validateInput(validate)) {
            return;
        }
        
        var noUseBlackForAliIn;
        if($noUseBlackForAliIn.is(":checked")){
        	noUseBlackForAliIn = 1; 
		 }else{
			noUseBlackForAliIn = 0; 
		 }
        
        object.propertyKey = 'INCOME_ALI_CONFIG';
        object.isEnable = '1';
        var jsonStr = {noUseBlackForAliIn:noUseBlackForAliIn,
        		maxAmountForAliIn:$maxAmountForAliIn.val(),
        		matchSecondForAliIn:$matchSecondForAliIn.val(),
        		totalMatchSecondForAliIn:$totalMatchSecondForAliIn.val(),
        		incomeTimeOutMinute:$incomeTimeOutMinute.val(),
        		incomeTimeOutTotalMinute:$incomeTimeOutTotalMinute.val(),
        		balanceMaxForAliIn:$balanceMaxForAliIn.val(),
        		lessThanPercentForAliIn:$lessThanPercentForAliIn.val(),
        		firstOrderMaxForAliIn:$firstOrderMaxForAliIn.val(),
        		firstOrderMaxMoneyForAliIn:$firstOrderMaxMoneyForAliIn.val(),
        		rebateMaxOderForAliIn:$rebateMaxOderForAliIn.val()
                   }
        object.propertyValue = JSON.stringify(jsonStr);
        paramArray.push(object);
       
   
    $.ajax({
        type: "PUT",
        dataType: 'JSON',
        url: '/r/set/updateParamByKey',
        async: false,
        data: {
            "param": JSON.stringify(paramArray),
        },
        success: function (jsonObject) {
            if (jsonObject && jsonObject.status == 1) {
                showMessageForSuccess("保存成功");
            } else {
                showMessageForFail("保存失败" + jsonObject.message);
            }
        }
    });
}

//收款人盘口 单选框 选择事件
var _toHandicapRadioChange = function () {
	$("input[name='toHandicapRadio_"+searchAlipayType+"']").change(function(){
	_searchAlipayInByStatus();
})
}

//入款会员盘口 单选框 选择事件
var _fromHandicapRadioChange = function () {
	$("input[name='fromHandicapRadio_"+searchAlipayType+"']").change(function(){
	_searchAlipayInByStatus();
})
}
	
// 只能输入数字设置
var inputSet = function () {
	$("input[name='maxAmountForAliIn']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='matchSecondForAliIn']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='totalMatchSecondForAliIn']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='incomeTimeOutMinute']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='incomeTimeOutTotalMinute']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='balanceMaxForAliIn']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='lessThanPercentForAliIn']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='firstOrderMaxForAliIn']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='firstOrderMaxMoneyForAliIn']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
	$("input[name='rebateMaxOderForAliIn']").keyup(function(){  // keyup事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).bind("paste",function(){  // CTR+V事件处理
	    $(this).val($(this).val().replace(/\D|^0/g,''));  
	}).css("ime-mode", "disabled"); 
}

/**
 * 初始化层级
 */
function _initialLevel(type) {
    // 根据盘口id查询层级
	var handicap = $("#handicap_"+  type).val();
	$('#level_' + type).empty();
	 var opt = '<option value="0">全部</option>';
    if (handicap) {
        $.ajax({
            dataType: 'json',
            type: "get",
            url: "/r/level/getByHandicapId",
            data: {"handicapId": handicap},
            success: function (res) {
                if (res.status == 1 && res.data && res.data.length > 0) {
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + val.id + '">' + val.name + '</option>';
                    });
                }
                $('#level_' + type).html(opt);
                $('#level_' + type).chosen(
                        {
                            allow_single_deselect: true,
                            search_contains: true,
                            no_results_text: "没有找到"
                        });
                $('#level_' + type).trigger('chosen:updated');
                $('#level_'+type+'_chosen').prop('style','width:78%');
            }
        });
    }
   
}
_initialSearTypeAlipay('alipayToMatch');
modifyNouns(); _modifyStyle();
