currentPageLocation = window.location.href;
var outwardRequstTotalSearchType = null;
var lookUpAuditedTotalAmount =false;
var finishedAuditedSubPage =  false;//出款审核汇总 已审核页签权限
var ManagerDealTab=false,ToAuditTab=false,AuditedTab=false,RefusedTab=false,CanceledTab=false;
$.each(ContentRight['OutwardAuditTotal:*'], function (name, value) {
    if (name == 'OutwardAuditTotal:finishedAuditedSubPage:*') {
        finishedAuditedSubPage = true;
    }
    if (name == 'OutwardAuditTotal:lookUpWinTotal:*'){
        lookUpWinTotalPageFlag = true;
    }
    if (finishedAuditedSubPage){
        $('#approved').show();
    }else{
        $('#approved').hide();
    }
    if (name == 'OutwardAuditTotal:ManagerDeal:*') {
        ManagerDealTab = true;$('#todeal').show();
    }
    if (name == 'OutwardAuditTotal:ToAudit:*'){
        ToAuditTab = true;$('#toapprove').show();
    }
    if (name == 'OutwardAuditTotal:Refused:*'){
        RefusedTab = true;$('#refused').show();
    }
    if (name == 'OutwardAuditTotal:Canceled:*'){
        CanceledTab = true;$('#canceled').show();
    }
    _acitivateFirstLi();
});
function _acitivateFirstLi(){
    $('#reqAuditTotalUl li:first-child').attr('class','active');
}
$.each(ContentRight['Outward:*'], function (name, value) {
    if (name == 'Outward:currentpageSum:*') {
        outwardCurrentPageSum=true;
    }
    if (name =='Outward:allRecordSum:*'){
        outwardAllRecordSum=true;
    }
});
function _addTimeSelect() {
    var opt = '<option  value="0" >不刷新</option><option selected="selected"  value="15">15秒</option><option  value="30">30秒</option>' +
        '<option  value="60">60秒</option><option  value="120">120秒</option><option  value="180">180秒</option>';
    $('#autoUpdateTimeReq').empty().append(opt);
}
$('#autoUpdateTimeReq').unbind().bind('change',function () {
    _searchAfterAutoUpdateTimeReq();
});
/**
 * 盘口初始化
 */
function _initialHandicap(type) {
    $.ajax({
        type: 'get',
        url: '/r/out/handicap',
        data: {},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var opt = '<option>请选择</option>';
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + val.id + '">' + val.name + '</option>';
                    });
                    $('#handicap_' + type).empty().html(opt);
                    $('#handicap_' + type).trigger('chosen:updated');
                }
            }
        }
    });
}
/**
 * 初始化层级
 */
function _initialLevel(handicap, type) {
    $('level_' + type).empty();
    if (handicap) {
        $.ajax({
            dataType: 'json',
            type: "get",
            url: "/r/level/getByHandicapId",
            data: {"handicapId": handicap},
            success: function (res) {
                if (res.status == 1 && res.data && res.data.length > 0) {
                    var opt = '<option>请选择</option>'
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + val.id + '">' + val.name + '</option>';
                    });
                    $('#level_' + type).empty().html(opt);
                    $('#level_' + type).trigger('chosen:updated');
                }
            }
        });
    }
}
/**
 * 查询按钮  type = 3,0,1,2,4  待处理 待审核 已审核 已拒绝 已取消 对应数据库的字段状态 status
 */
function _setClickType(type) {
    outwardRequstTotalSearchType = type;
    _initialHandicapsReq(type);
    initPaging($('#total_footPage_' +type), pageInitial);
    if (type==3 || type==0){
        _addTimeSelect();
        _searchAfterAutoUpdateTimeReq();
    }else{
        if (timeOutSearchReq) {
            clearInterval(timeOutSearchReq);
            timeOutSearchReq = null;
        }
    }
    if (type!=0 && type!=3) {
        if (window.sessionStorage.getItem("auditSubPage")){
            window.sessionStorage.removeItem("auditSubPage");
        }
        _getPathAndHtml('historyDetail_modal');
    }else{
        window.sessionStorage.setItem("auditSubPage",type);
        _getPathAndHtml('total_toApprove_detail_modal');
    }
    if (type==1||type==2 || type==4){
        if (timeOutSearchReq) {
            clearInterval(timeOutSearchReq);
            timeOutSearchReq = null;
        }
    }
    _datePickerForAll($('#timeScope_'+outwardRequstTotalSearchType));
    setTimeout('_searchOutRequestTotal',100);//进入页面先查询
    _searchOutRequestTotal();
}
/**
 * 显示 订单详情
 * @param id
 * @private
 */
function _showOrderDetail(id, orderNo) {
    $.ajax({
        type: "get",
        url: '/r/out/getById',
        data: {"id": id},
        dataType: "json",
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    _initialOrderText(id+'detail', res.data);
                }
            }
        }
    });
}
/**盘口事件*/
function _handicapAuditTotalChange() {
    if ($('#handicap_' + outwardRequstTotalSearchType).val() != '请选择'){
        _searchOutRequestTotal();
        _initialLevel($('#handicap_' + outwardRequstTotalSearchType).val(), outwardRequstTotalSearchType);
    }else {
        $('#level_' + outwardRequstTotalSearchType).empty().html('<option>请选择</option>');
        $('#level_' + outwardRequstTotalSearchType).trigger('chosen:updated');
    }

}
/**层级事件*/
function _levelAuditTotalChange() {
    if ($('#level_' + outwardRequstTotalSearchType).val() != '请选择') {
        _searchOutRequestTotal();
    }
}
function _searchOutRequestTotal() {
    if (timeOutSearchReq && currentPageLocation.indexOf('OutwardAuditTotal:*')<=-1) {
        clearInterval(timeOutSearchReq);
        timeOutSearchReq = null;
        return;
    }
    var type = outwardRequstTotalSearchType;
    var flag = false,Noteflag=false;
    var notifyFlag = false;
    var toAuditSubPageAuthority = false;
    //判断是否有审核按钮的权限
    $.each(ContentRight['OutwardAuditTotal:*'], function (name, value) {
        if (name == 'OutwardAuditTotal:Audit:*') {
            flag = true;
        }else if (name == 'OutwardAuditTotal:NotifyPlatForm:*') {
            //通知平台权限
            notifyFlag = true;
        }else if (name == 'OutwardAuditTotal:toAuditSubPageAuthority:*') {
            //通知平台权限
            toAuditSubPageAuthority = true;
        }
    });
    $.each(ContentRight['OutwardTaskTotal:*'], function (name, value) {
        if (name == 'OutwardTaskTotal:Note:*') {
            Noteflag = true;
        }
    });
    var handicap = '';
    if ($('#handicap_' + type).val() && $('#handicap_' + type).val() != '请选择') {
        handicap = $('#handicap_' + type).val();
    }
    var level = '';
    if ($('#level_' + type).val() && $('#level_' + type).val() != '请选择') {
        level = $('#level_' + type).val();
    }
    var member = '';
    if ($('#member_' + type).val()) {
        if (member.indexOf('%') >= 0) {
            member = member.replace(new RegExp(/%/g), '?');
        }
        else {
            member = $('#member_' + type).val();
        }
    }
    var orderNo = '';
    if ($('#orderNo_' + type).val()) {
        if (orderNo.indexOf('%') >= 0)
            orderNo = orderNo.replace(new RegExp(/%/g), '?');
        else
            orderNo = $('#orderNo_' + type).val();
    }
    var operator = '';
    if (type != 0) {
        if ($('#operator_' + type).val()) {
            if (operator.indexOf('%') >= 0)
                operator = operator.replace(new RegExp(/%/g), '?');
            else
                operator = $('#operator_' + type).val();
        }
    }

    var startTime = '';
    var endTime = '';
    var startAndEnd = $('#timeScope_' + type).val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    } else {
        startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
        endTime = moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    }
    var famount = null;
    if ($('#famount_' + type).val()) {
        famount = $('#famount_' + type).val();
    }
    var tamount = null;
    if ($('#tamount_' + type).val()) {
        tamount = $('#tamount_' + type).val();
    }
    var reviwerType = '';
    var robot = '';
    var manual = '';
    if ($('#robot_' + type).prop('checked')) {
        robot = $('#robot_' + type).val();
        reviwerType = 'robot';
    }
    if ($('#manual_' + type).prop('checked')) {
        manual = $('#manual_' + type).val();
        reviwerType = 'manual';
    }
    if (robot && manual) {
        reviwerType = '';
    }
    var status = type;
    var CurPage = $("#total_footPage_" + type).find(".Current_Page").text();
    if (!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var data = {
        "handicap": handicap, "level": level, "member": member, "orderNo": orderNo, "auditor": operator,
        "startTime": $.trim(startTime), "endTime": $.trim(endTime), "famount": famount,
        "tamount": tamount, "reviwerType": reviwerType, "status": status,
        "pageNo": CurPage, "pageSize": $.session.get('initPageSize')
    };
    $.ajax({
        type: 'get',
        url: '/r/out/total',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    //拼接
                    var tr = '', amount = 0, trs = '';
                    if (res.data && res.data.length > 0) {
                        $(res.data).each(function (i, val) {
                            tr +=
                                '<tr>' +
                                '<td>' + _checkObj(val.handicapName) + '</td>' +
                                '<td>' + _checkObj(val.levelName) + '</td>' +
                                '<td>' + _checkObj(val.member) + '</td>' ;

                            if (_checkObj(val.member)&&_checkObj(val.member)!='公司用款' ){
                                if (!isHideAccount)
                                tr +='<td><a href="javascript:_showOrderDetail(' + val.id + ',\'' + val.orderNo + '\');">' + val.orderNo + '</a></td>';
                                else  tr +='<td>' + _checkObj(val.orderNo) + '</td>';
                            }else{
                                tr +='<td>' + _checkObj(val.orderNo) + '</td>';
                            }
                            if (type==0 || type==3){
                                tr += '<td>' + _checkObj(val.amount) + '</td><td>' + _checkObj(val.reviewer) + '</td><td>' + timeStamp2yyyyMMddHHmmss(val.createTime) + '</td>';
                                if (type==3){
                                    tr += '<td>' + timeStamp2yyyyMMddHHmmss(val.approveTime) + '</td>';
                                }
                            }else{
                                tr += '<td>' + _checkObj(val.amount) + '</td><td>' + _checkObj(val.reviewer) + '</td><td>' + timeStamp2yyyyMMddHHmmss(val.createTime) +'</td><td>' + timeStamp2yyyyMMddHHmmss(val.approveTime) + '</td>';
                            }
                            if (type == 0 || type == 3){
                                if (val.remark) {
                                    if (_checkObj(val.member)){
                                        if (_checkObj(val.remark).length>5) {
                                            tr += '<td>'
                                                + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                                + ' data-content="' + val.remark + '">'
                                                + _checkObj(val.remark).substring(0, 5)
                                                + '</a>'
                                                + '</td>';
                                        }else {
                                            tr += '<td>'
                                                + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                                + ' data-content="' + val.remark + '">'
                                                + _checkObj(val.remark)
                                                + '</a>'
                                                + '</td>';
                                        }
                                    }else{
                                        tr += '<td>'
                                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + val.remark + '">'
                                            + '盘口用款'
                                            + '</a>'
                                            + '</td>';
                                    }

                                } else {
                                    tr += '<td></td>';
                                }
                            }
                            if (type == 1) {
                                tr += '<td>' + _check(val.timeConsuming) + '</td>';
                            }
                            if (type == 2 || type == 4) {
                                if (val.remark) {
                                    if (_checkObj(val.member)){
                                        if (_checkObj(val.remark).length>5) {
                                            tr += '<td>'
                                                + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                                + ' data-content="' + val.remark + '">'
                                                + _checkObj(val.remark).substring(0, 5)
                                                + '</a>'
                                                + '</td>';
                                        }else {
                                            tr += '<td>'
                                                + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                                + ' data-content="' + val.remark + '">'
                                                + _checkObj(val.remark)
                                                + '</a>'
                                                + '</td>';
                                        }
                                    }else{
                                        tr += '<td>'
                                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + val.remark + '">'
                                            + '盘口用款'
                                            + '</a>'
                                            + '</td>';
                                    }
                                } else {
                                    tr += '<td></td>';
                                }
                            }
                            if (type == 3 || type == 0 ) {
                                //type == 0 待审核页面的审核按钮
                                //判断是否有审核按钮的权限
                                //主管处理页签
                                if ( type==3 ) {
                                    //公司用款的member为空 不能审核
                                    tr += '<td style="width: 65px;">';
                                    if(flag && _checkObj(val.member)){
                                        tr += '<button onclick="_approveBefore(' + val.id + ',' + type + ')" style="padding-bottom: 0px;padding-top: 0px;" class="btn btn-xs btn-white btn-info btn-bold pass">' +
                                            '<i class="ace-icon fa fa-check bigger-100 blue"></i>审核</button>';
                                    }
                                    if (Noteflag) {
                                        tr += '<button onclick="SCustomerserviceRemark(' + val.id + ');"  style="padding-bottom: 0px;padding-top: 0px;" class="btn btn-xs btn-white btn-warning btn-bold ">' +
                                            '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                                    }
                                    tr +='</td>';
                                } else if ( type==0 && toAuditSubPageAuthority && _checkObj(val.member)) {
                                    //待审核页签 公司用款的member为空 不能审核
                                    tr += '<td style="width: 65px;"><button onclick="_approveBefore(' + val.id + ',' + type + ')" style="padding-bottom: 0px;padding-top: 0px;" class="btn btn-xs btn-white btn-info btn-bold pass">' +
                                        '<i class="ace-icon fa fa-check bigger-100 blue"></i>审核</button></td>';
                                } else {
                                    tr += '<td></td>';
                                }
                            }
                            if (type == 1) {
                                if (val.remark) {
                                    if (_checkObj(val.member)){
                                        if (_checkObj(val.remark).length>5) {
                                            tr += '<td>'
                                                + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                                + ' data-content="' + val.remark + '">'
                                                + _checkObj(val.remark).substring(0, 5)
                                                + '</a>'
                                                + '</td>';
                                        }else {
                                            tr += '<td>'
                                                + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                                + ' data-content="' + val.remark + '">'
                                                + _checkObj(val.remark)
                                                + '</a>'
                                                + '</td>';
                                        }
                                    }else{
                                        tr += '<td>'
                                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + val.remark + '">'
                                            + '盘口用款'
                                            + '</a>'
                                            + '</td>';
                                    }

                                } else {
                                    tr += '<td></td>';
                                }
                                tr +='<td>'+val.status+'</td>';
                                if (_checkObj(val.member) && val.status=='出款成功，与平台确认失败' && notifyFlag){
                                    tr +='<td><button onclick="_noticePlatformTotal(' + val.id + ');"  type="button"  class=" btn btn-xs btn-white btn-info btn-bold">' +
                                    '<i class="ace-icon fa  fa-envelope-open-o  bigger-100 green"></i>通知平台</button></td>';
                                }else{
                                    tr +='<td></td>';
                                }
                            }
                            tr += '</tr>';
                            amount += val.amount;
                        });
                        if(outwardCurrentPageSum){
                            trs +='<tr><td id="toTalCurrentPageCount_' + type + '" colspan="4">小计：统计中..</td>' +
                                '<td bgcolor="#579EC8" style="color:white; width:130px ;">' + parseFloat(amount).toFixed(3) + '</td>' +
                                '<td colspan="7"></td></tr>';
                        }else{
                            trs +='<tr><td id="toTalCurrentPageCount_' + type + '" colspan="18">小计：统计中..</td></tr>' ;
                        }
                        if(outwardAllRecordSum){
                            trs +='<tr><td id="toTalPageAllCount_' + type + '" colspan="4">总共：统计中..</td>' +
                            '<td id="toTalSumAmount_' + type + '" bgcolor="#D6487E" style="color:white;width:130px ;display: none;">统计中..</td>' +
                            '<td colspan="7"></td></tr>';
                        }else{
                            trs +='<tr><td id="toTalPageAllCount_' + type + '" colspan="15">总共：统计中..</td></tr>';
                        }
                    }
                }
                if (type == 3) {
                    $('#tab_todeal_tbody').empty().html(tr).append(trs);
                }
                if (type == 0) {
                    $('#tab_toApprove_tbody').empty().html(tr).append(trs);
                }
                if (type == 1) {
                    $('#tab_approved_tbody').empty().html(tr).append(trs);
                }
                if (type == 2) {
                    $('#tab_refused_tbody').empty().html(tr).append(trs);
                }
                if (type == 4) {
                    $('#tab_canceled_tbody').empty().html(tr).append(trs);
                }
                if(outwardAllRecordSum){
                    _getOutwardRequestTotalSumAmount(data, type);
                }
                _getOutwardRequestTotalCount(data, type);
                showPading(res.page, 'total_footPage_' + type, _searchOutRequestTotal);
                $("[data-toggle='popover']").popover();
            }
        }
    });
}
//添加备注
function SCustomerserviceRemark(id) {
    $('#CustomerserviceReqRemark').val('');
    $('#CustomerserviceReqRemark_id').val(id);
    $('#CustomerserviceReqRemark_modal').modal('show');
    $('#totalReqFinishBTN').attr('onclick', 'save_CustomerserviceReqRemark();');

}
function save_CustomerserviceReqRemark() {
    var remark = $.trim($('#CustomerserviceReqRemark').val());
    if (!remark) {
        $('#prompt_Reqremark').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    var reqId = $('#CustomerserviceReqRemark_id').val();
    $.ajax({
        type: 'post',
        url: '/r/outtask/remark',
        data: {"taskId": reqId, "remark": remark,"type":'req'},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            $('#CustomerserviceReqRemark_modal').modal('hide');
            _searchOutRequestTotal();
        }
    });
}
/**通知平台*/
function _noticePlatformTotal(id) {
    if (id) {
        bootbox.confirm('确定通知平台吗？', function (res) {
            if (res) {
                $.ajax({
                    type: 'post',
                    url: '/r/out/noticePlatForm',
                    data: {"requestId": id},
                    dataType: 'json',
                    success: function (res) {
                        if (res) {
                            $.gritter.add({
                                time: 800,
                                class_name: '',
                                title: '系统消息',
                                text: res.message,
                                sticky: false,
                                image: '../images/message.png'
                            });
                            _searchOutRequestTotal();
                        }
                    }
                });
            }
        })
    }
}
function _getOutwardRequestTotalSumAmount(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/out/getOutwardRequestTotalSumAmount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                $('#toTalSumAmount_' + type).text(parseFloat(res.data.sumAmount).toFixed(3));
                $.each(ContentRight['OutwardAuditTotal:*'], function (name, value) {
                    if (name == 'OutwardAuditTotal:lookUpAuditedTotalAmount:*') {
                        lookUpAuditedTotalAmount = true;
                    }
                });
                if(type==1){
                    if(!lookUpAuditedTotalAmount){
                        $('#toTalSumAmount_' + type).text('无权限查看');
                    }
                }
                $('#toTalSumAmount_' + type).show();
            }
        }
    });
}
function _getOutwardRequestTotalCount(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/out/getOutwardRequestTotalCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.page) {
                    $('#toTalCurrentPageCount_' + type).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#toTalPageAllCount_' + type).empty().text('合计：' + res.page.totalElements + '条记录');
                    showPading(res.page, 'total_footPage_' + type, _searchOutRequestTotal);
                }
            }
        }
    });
}
/**
 * 重置
 * @param type
 * @private
 */
function _resetValueForAuditTotal(type) {
    initPaging($('#total_footPage_'+type),pageInitial);
    if ($('#handicap_' + type).val()) {
        _initialHandicap(type);
    }
    if ($('#level_' + type).val()) {
        $('#handicap_' + type).change();
    }
    var member = $('#member_' + type).val();
    if (member) {
        $('#member_' + type).val('');
    }
    var orderNo = $('#orderNo_' + type).val();
    if (orderNo) {
        $('#orderNo_' + type).val('');
    }
    var operator = '';
    if (type != 0) {
        operator = $('#operator_' + type).val();
        if (operator) {
            $('#operator_' + type).val('');
        }
    }
    _datePickerForAll($("#timeScope_"+type));
    var famount = $('#famount_' + type).val();
    var tamount = $('#tamount_' + type).val();
    if (famount) {
        $('#famount_' + type).val('');
    }
    if (tamount) {
        $('#tamount_' + type).val('');
    }
    if ($('#robot_' + type).prop('checked')) {
        $('#robot_' + type).prop('checked', false);
    }
    if ($('#manual_' + type).prop('checked')) {
        $('#manual_' + type).prop('checked', false);
    }
    _setClickType(type);
}
function _check(val) {
    var vl = '';
    if (val) {
        vl = val;
    }
    return vl;
}

/**
 * 审核汇总 待审核页签 审核 操作之前
 * pageFlag: 3表示主管处理的页签 0表示待审核页签
 */
function _approveBefore(id, pageFlag) {
    //通过审核的id查询审核信息 初始化modal框信息
    $.ajax({
        type: 'get',
        url: '/r/out/getById',
        data: {"id": id},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    _initialOrderText(id, res.data);
                    $('#total_toApprove_detail_modal  #modal-orderDetail-toAppoveId').val(id);
                    $('#total_toApprove_detail_modal  #modal-orderDetail-orderNoInput').val(res.data.orderNo);
                    $('#total_toApprove_detail_modal  #modal-orderDetail-memberCode').val(res.data.memberCode);
                    $('#total_toApprove_detail_modal  #modal-orderDetail-pageFlag').val(pageFlag);
                }
            }
        }
    });
}
//审核汇总的 审核 审核完成后模态框消失 返回原来的页面 type审核类型  pageFlag页面标志;
function _saveForApproveTotal(type) {
    var id = $('#total_toApprove_detail_modal  #modal-orderDetail-toAppoveId').val();
    var orderNoInput = $('#total_toApprove_detail_modal  #modal-orderDetail-orderNoInput').val();
    var memberCode = $('#total_toApprove_detail_modal  #modal-orderDetail-memberCode').val();
    var remark = $('#total_toApprove_detail_modal  #modal-orderDetail-remarkForApprove').val();
    if (type == 1) {
        bootbox.confirm('确定通过审核吗？', function (res) {
            if (res) {
                _executeSaveForTotal(id, remark, type, orderNoInput, memberCode);
            }
        });
    }
    if (type == 2 || type == 3 || type == 4) {
        if (!remark) {
            $('#modal-orderDetail-remarkPrompt2').show().delay(500).fadeOut();
            return false;
        } else {
            var info = type==2?"确定取消吗？":type==3?"确定拒绝吗？":"确定转主管吗？";
            bootbox.confirm(info, function (res) {
                if (res) {
                    _executeSaveForTotal(id, remark, type, orderNoInput, memberCode);
                }
            });
        }
    }
}
//审核汇总 审核 通过 取消 拒绝 转主管 完成之后 模态框消失 返回原页面
function _executeSaveForTotal(id, remark, type, orderNoInput, memberCode) {
    if (!id){
        bootbox.alert("参数id丢失,请联系技术");
        return ;
    }
    $.ajax({
        type: 'post',
        url: '/r/out/save',
        data: {
            "id": id, "remark": remark, "type": type,
            "orderNo": orderNoInput, "memberCode": memberCode
        },
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res && res.status == 1) {
                if (type == 1) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                }
                if (type == 2) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: '取消成功',
                        sticky: false,
                        image: '../images/message.png'
                    });
                }
                if (type == 3) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: '拒绝成功',
                        sticky: false,
                        image: '../images/message.png'
                    });

                }
                if (type == 4) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: '转主管成功',
                        sticky: false,
                        image: '../images/message.png'
                    });
                }
                $('#modal-orderDetail-remarkForApprove').val("");
                $('#modal-orderDetail').modal('hide');
                var pageFlag = $('#modal-orderDetail-pageFlag').val();
                if (pageFlag) {
                    _searchOutRequestTotal();
                }
            }
            else {
                $.gritter.add({
                    time: 1000,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                //$('#total_toApprove_detail_modal').modal('hide');
                return false;
            }

        }
    });
}
//审核 校验
function _save(type) {
    var id = $('#toAppoveId').val();
    var orderNoInput = $('#orderNoInput').val();
    var memberCode = $('#memberCode').val();
    var remark = $('#remarkForApprove').val();
    var flag = '';
    if (type == 1) {
        flag = confirm('确定通过审核吗？', function (res) {
            return res;
        });
    }
    if (type == 2 || type == 3 || type == 4) {
        if (!remark) {
            $('#remarkPrompt2').show().delay(500).fadeOut();
            return false;
        }
    }
    if (type == 1 && flag) {
        _executeSave(id, remark, type, orderNoInput, memberCode);
    }
    if (type == 2 || type == 3 || type == 4) {
        _executeSave(id, remark, type, orderNoInput, memberCode);
    }
}
//执行操作
function _executeSave(id, remark, type, orderNoInput, memberCode) {
    $.ajax({
        type: 'post',
        url: '/r/out/save',
        data: {
            "id": id, "remark": remark, "type": type,
            "orderNo": orderNoInput, "memberCode": memberCode
        },
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res && res.status == 1) {
                if (type == 1) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                    $('#remarkForApprove').val("");
                    _searchOutRequestTotal();
                }
                if (type == 2) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: '取消成功',
                        sticky: false,
                        image: '../images/message.png'
                    });
                    $('#remarkForApprove').val("");
                    _searchOutRequestTotal();
                }
                if (type == 3) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: '拒绝成功',
                        sticky: false,
                        image: '../images/message.png'
                    });
                    $('#remarkForApprove').val("");
                    _searchOutRequestTotal();
                }
                if (type == 4) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: '转主管成功',
                        sticky: false,
                        image: '../images/message.png'
                    });
                    $('#remarkForApprove').val("");
                    _searchOutRequestTotal();
                }
            }
            else {
                $.gritter.add({
                    time: 1000,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                return false;
            }

        }
    });
}

/**
 * 操作类型 通过 1，拒绝3，取消2, 转主管 4
 */
function _handler(type) {
    var id = $('#approveId').val();
    var remark = $('#remark').val();
    if (type == 1) {
        if (!remark) {
            bootbox.alert('请填写审核备注！');
            return false;
        }
    }
    if (type == 2 || type == 3 || type == 4) {
        if (!remark) {
            bootbox.alert('请填写原因！');
            return false;
        }
    }
    var data = {"id": id, "type": type, "remark": remark};
    $.ajax({
        type: 'post',
        url: '/r/out/save',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res && res.status == 1) {
                //页面类型 type = 3,0,1,2,4 待处理 待审核 已审核 已拒绝 已取消
                //操作类型 通过 1，拒绝3，取消2, 转主管 4
                if (type == 1) {
                    _searchOutRequestTotal();
                }
                if (type == 3) {
                    _searchOutRequestTotal();
                }
                if (type == 4) {
                    _searchOutRequestTotal();
                }
                if (type == 2) {
                    _searchOutRequestTotal();
                }
                bootbox.alert(res.message);
                $('#approve_total_details').modal('hide');
            }
        }

    });
}

/**
 * 待处理页面--只有主管或者管理员才能看见，其他没有
 */
function _hideTotalRequest() {
    var toViewFlag = false;
    $.each(ContentRight['OutwardAuditTotal:*'], function (name, value) {
        if (name.indexOf('OutwardAuditTotal') == -1) {
            toViewFlag = false;
        } else {
            toViewFlag = true;
        }
    });
    if (toViewFlag) {
        $('#but_transfer').remove();//转主管的按钮
        //_setClickType(3);
    } else {
        $('#tab_todeal').removeClass('in').removeClass('active');
        $('#tab_toApprove').addClass('in').addClass('active');
        $('#todeal').remove();
        $('#toapprove').addClass('active');
        //_setClickType(0);
    }
}
function _initialHandicapsReq(type) {
    _initialSelectChosen(type);
    // _initialSelectChosen(0);
    // _initialSelectChosen(1);
    // _initialSelectChosen(2);
    // _initialSelectChosen(4);
}
function _initialSelectChosen(type) {
    //可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        //allow_single_deselect:true,
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    // _initialHandicap(type);
    // $('#handicap_' + type + '_chosen').prop('style', 'width: 78%;');
    // $('#level_' + type + '_chosen').prop('style', 'width: 78%;');
    if (handicapAndLevelInitialOptions) {
        $('#handicap_' + type).empty().html(handicapAndLevelInitialOptions[0]);
        $('#handicap_' + type).trigger('chosen:updated');
        $('#level_' + type).empty().html(handicapAndLevelInitialOptions[1]);
        $('#level_' + type).trigger('chosen:updated');
    }
    $('#handicap_' + type + '_chosen').prop('style', 'width: 78%;');
    $('#level_' + type + '_chosen').prop('style', 'width: 78%;');
}
_datePickerForAll($("input.date-range-picker"));
_setClickType(3);
_hideTotalRequest();
_addTimeSelect();
$('#freshLiReq').show();
modifyNouns();