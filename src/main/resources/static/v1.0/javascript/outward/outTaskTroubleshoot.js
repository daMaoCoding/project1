/**
 * Created by Administrator on 2018/7/2.
 */
currentPageLocation = window.location.href;
var taskReviewSocket = null;
var btnActionFlag = null;
var ShootTrouble = false,ReviweTasksLock=false, FinishTask = false, UploadReceipt = false, Noteflag = false, NewGenerationTask = false,
    Distributionflag =false,TurningPlatformflag=false,Freezeflag=false,QuickQueryTab=false;
$.each(ContentRight['Outward:*'], function (name, value) {
    if (name == 'Outward:currentpageSum:*') {
        outwardCurrentPageSum=true;
    }
    if (name =='Outward:allRecordSum:*'){
        outwardAllRecordSum=true;
    }

});
($.each(ContentRight['TaskTroubleshoot:*'], function (name, value) {
    if (name == 'TaskTroubleshoot:ShootTrouble:*') {
        ShootTrouble = true;
        if (ShootTrouble) {
            $('#changBtnTroubleShootLi').show();
        } else {
            $('#changBtnTroubleShootLi').hide();
        }
    }
    if (name == 'TaskTroubleshoot:FinishTask:*') {
        FinishTask = true;
    }
    if (name == 'TaskTroubleshoot:UploadReceipt:*') {
        UploadReceipt = true;
    }
    if (name == 'TaskTroubleshoot:AddRemark:*') {
        Noteflag = true;
    }
    if (name == 'TaskTroubleshoot:NewGenerationTask:*') {
        NewGenerationTask = true;
    }
    if (name == 'TaskTroubleshoot:ReviweTasksLock:*' ){
        ReviweTasksLock = true;
    }
    if (name == 'TaskTroubleshoot:Distributionflag:*' ){
        Distributionflag = true;
    }
    if (name == 'TaskTroubleshoot:TurningPlatformflag:*' ){
        TurningPlatformflag = true;
    }
    if (name == 'TaskTroubleshoot:Freezeflag:*' ){
        Freezeflag = true;
    }
    if (name == 'TaskTroubleshoot:QuickQueryTab:*') {
        QuickQueryTab = true;$('#QuickQueryTab').show();
    }else {
        QuickQueryTab = false;$('#QuickQueryTab').hide();
    }
}));
function _createTaskReviewWS() {
    var socketUrl = window.location.protocol == 'http:' ? 'ws://' + window.location.host + '/ws/taskReview' :
        'wss://' + window.location.host + '/ws/taskReview';
    taskReviewSocket = new WebSocket(socketUrl);
    taskReviewSocket.onopen = function (evt) {
        if (evt.type=='open'){
            _reportReceiveStatus();
        }
    };
    taskReviewSocket.onclose = function (evt) {
        if (evt.type=='close'){
            if( evt.code!=4888){
                _reportReceiveStatus();
            }
            //4888 结束接单
            setTimeout(function () {
                _searchTrouble('troubleShooting');
                _checkTroubleShootingOnlineUsersInfo(0);
            },3000);
        }
    };
    taskReviewSocket.onmessage = function (evt) {
        //收到服务器消息，使用evt.data提取
        console.log(evt.data);
        if (evt.data=='FRESH_PAGE') {
            setTimeout(function () {
                _searchTrouble('troubleShooting');
                _checkTroubleShootingOnlineUsersInfo(0);
            },500);
        }
    };
    taskReviewSocket.onerror = function (evt) {
        if (evt.type=='error'){
            _closeWebSocket();
        }
    };
}
//关闭浏览器关闭socket
window.onbeforeunload = function () {
    console.log("退出");
    _closeWebSocket();
}
//关闭WebSocket连接
function _closeWebSocket() {
    if (taskReviewSocket) {
        taskReviewSocket.close();
        taskReviewSocket = null;
    }
}
function _autoUpdateTime() {
    var opt = '<option  value="0" >不刷新</option><option selected="selected"  value="15">15秒</option><option  value="30">30秒</option>' +
        '<option  value="60">60秒</option><option  value="120">120秒</option><option  value="180">180秒</option>';
    $('#autoUpdateTimeTroubleShooting').empty().append(opt);
}

$('#autoUpdateTimeTroubleShooting').unbind().bind('change', function () {
    _autoUpdateTimeTroubleShooting();
});
function _showBtnStatus(type) {
    var status = window.sessionStorage.getItem('troubleShootStatus');
    //var status = $('#changBtnTroubleShoot').attr('btn_value');
    if (status) {
        $('#stopTroubleShoot').show();
        if (status == '1') {
            $('#changBtnTroubleShoot').attr('btn_value', '2');
            $('#changBtnTroubleShoot').html('<i class="bigger-120 ace-icon fa fa-pause grey"></i><span>暂停接单</span>');
            $('#freshTroubleShootingLi').show();//刷新元素
            if (type != 'click') {
                if (!taskReviewSocket) {
                    _createTaskReviewWS();
                }
            }
        } else {
            //$('#freshTroubleShootingLi').hide();//刷新元素
            if (troubleShootingTime) {
                clearInterval(troubleShootingTime);
            }
            //$('#stopTroubleShoot').hide();
            $('#changBtnTroubleShoot').attr('btn_value', '1');
            $('#changBtnTroubleShoot').html('<i class="bigger-120 ace-icon fa fa-play green"></i><span>开始接单</span>');
            $('#changBtnTroubleShoot').show();
        }
    }
}
function _buttonClick(obj) {
    var status = $(obj).attr('btn_value');
    if (status == '1') {
        _createTaskReviewWS();
        btnActionFlag = 1
        //_reportReceiveStatus(); //开始接单
        window.sessionStorage.setItem('troubleShootStatus', 1);
    } else {
        _closeWebSocket();
        btnActionFlag = 2
        //_reportReceiveStatus();//暂停
        window.sessionStorage.setItem('troubleShootStatus', 2);
    }
    _showBtnStatus('click');
}
//结束接单
function _stopBtnAction() {
    $('#stopTroubleShoot').hide();
    $('#changBtnTroubleShoot').attr('btn_value', '1');
    $('#changBtnTroubleShoot').html('<i class="bigger-120 ace-icon fa fa-play green"></i><span>开始接单</span>');
    window.sessionStorage.removeItem('troubleShootStatus');
    $('#changBtnTroubleShootLi').show();
    $('#changBtnTroubleShootLi').attr('btn_value', '1');
    $('#freshTroubleShootingLi').hide();
    if (troubleShootingTime) {
        clearInterval(troubleShootingTime);
    }
    if (taskReviewSocket){
        taskReviewSocket.send("STOP");
    }
    btnActionFlag = 3
    _reportReceiveStatus();//刷新出款管理汇总待排查在线数据
}
function _reportReceiveStatus() {
    if (btnActionFlag) {
        $.ajax({
            type: 'get', data: {'type': btnActionFlag,"userId":getCookie('JUSERID')}, dataType: 'json',
            url: '/r/taskReview/troubleShootAction', async: false,
            success: function (res) {
                if (res) {
                    $.gritter.add({
                        time: 1000,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                }
                if(btnActionFlag==3){
                    if (taskReviewSocket){
                        //没有先暂停再结束接单
                        taskReviewSocket.close(4888);
                        taskReviewSocket = null;
                    }
                    _searchTrouble('troubleShooting');
                    _checkTroubleShootingOnlineUsersInfo(0);
                }

            }
        });
    }
}
function _searchTrouble(type) {
    if (!type) {
        type = 'troubleShooting';
    }
    if (type != 'troubleShooting') {
        if (troubleShootingTime) {
            clearInterval(troubleShootingTime);
        }
    }
    var handicap = '';
    var handId = $('#handicap_' + type).attr('handicap_code');
    if (handId && handId.indexOf('请选择') < 0) {
        handicap = handId;
    }
    var level = '';
    var levelId = $('#level_' + type).text();
    if (levelId && levelId.indexOf('请选择') < 0) {
        level = levelId;
    }
    var orderNo = null;
    var orderNoR = $('#orderNo_' + type).val();
    if (orderNoR) {
        if (orderNoR.indexOf('%') >= 0)
            orderNo = orderNoR.replace(new RegExp(/%/g), "?");
        else
            orderNo = orderNoR;
    }
    var member = null;
    var memberR = $('#member_' + type).val();
    if (memberR) {
        if (memberR.indexOf('%') >= 0)
            member = memberR.replace(new RegExp(/%/g), "?");
        else
            member = memberR;
    }
    var fromAccount = null;
    var operator = null;
    if ($('#operator_' + type).val()) {
        operator = $('#operator_' + type).val();
    }
    if ($('#account_' + type).val() && $('#account_' + type).val().indexOf('请选择') < 0) {
        fromAccount = $('#account_' + type).val();
    }
    var robot = '';
    var manual = '';
    var type1 = '';
    if ($('#robot_' + type).prop('checked')) {
        robot = $('#robot_' + type).val();
        type1 = robot;
    }
    if ($('#manual_' + type).prop('checked')) {
        manual = $('#manual_' + type).val();
        type1 = manual;
    }
    if (robot && manual) {
        type1 = '';
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
    }
    var fromMoney = '';
    var toMoney = '';
    if ($('#fromMoney_' + type).val()) {
        fromMoney = $('#fromMoney_' + type).val();
    }
    if ($('#toMoney_' + type).val()) {
        toMoney = $('#toMoney_' + type).val();
    }
    var CurPage = $("#" + type + "_footPage").find(".Current_Page").text();
    if (!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var data = {
        "handicap": handicap, "level": level, "orderNo": orderNo, "startTime": $.trim(startTime),
        "endTime": $.trim(endTime), "member": member, "amountStart": fromMoney,
        "amountEnd": toMoney, "outAccount": fromAccount, "operator": operator,
        "type": type1, "pageType": 1, "queryType": type == 'troubleShooting' ? 1 : type == 'dealingTask'?3:2,
        "pageNo": CurPage, "pageSize": $.session.get('initPageSize') ? $.session.get('initPageSize') : 10
    };
    $.ajax({
        type: 'get',
        url: '/r/taskReview/troubleShoot',
        data: data,
        async: false,
        dataType: 'json',
        success: function (res) {
            $('#tbody_' + type).empty();
            if(btnActionFlag==3 &&(type!='troubleShooted')){
                showPading(null, type + '_footPage', _searchTrouble);
            }else{
                if (res && res.status==1) {
                    _fillData(res, type);
                    if ($('#' + type + ' #promptMessageTotalFailedOut')) {
                        $('#' + type + ' #promptMessageTotalFailedOut').remove();
                    }
                    if(type!='troubleShooted'){
                        $('#' + type).append('<div id="promptMessageTotalFailedOut" ><span style="color: mediumvioletred;font-size: 15px" >温馨提示：红色<span style="background-color: indianred">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过5分钟。</span></div>');
                    }else{
                        $('#' + type).append('<div id="promptMessageTotalFailedOut" ><span style="color: mediumvioletred;font-size: 15px" >温馨提示：红色<span style="background-color: indianred">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过5分钟。</span><span style="background-color: lightgreen">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>浅绿色的表示该条记录状态为"已匹配流水"。</div>');
                    }
                    _getSum(data, type);
                    _getCount(data, type);
                } else {
                    $.gritter.add({
                        time: 1500,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                }
            }

        }
    });

}
//待排查数据渲染
function _fillData(res, subPageType) {
    var data = res.data;
    var idList = [],idArray=[];
    var tr = '';
    var trs = '';
    //$('#tbody_' + subPageType).empty();
    if (data && data.length > 0) {
        var amount = 0;
        $(data).each(function (i, val) {
            var screenshotArr = null;
            if (_checkObj(val.photo)) {
                screenshotArr = val.photo.split('/');
            }
            var hasScreenshot = false;
            if (screenshotArr) {
                if (screenshotArr.length > 3) {
                    hasScreenshot = (screenshotArr[3]).indexOf('screenshot') > -1;
                } else {
                    hasScreenshot = (screenshotArr[2]).indexOf('screenshot') > -1;
                }
            }
            idList.push({'id': val.accountId});
            if (val) {
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                    //是否是公司用款
                    if (!isHideAccount)
                    tr += '<td><a  href="javascript:_showOrderDetail(' + val.reqId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                    else tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                }
                tr += '<td>' + _checkObj(val.amount) + '</td>';
                if (val.taskStatus == '流水匹配') {
                    //已匹配流水的记录 浅绿色背景标识
                    tr += '<td style="background-color: lightgreen">' + _checkObj(val.taskStatus) + '</td>';
                } else if(val.taskStatus == '待排查') {
                    tr += '<td>' + _checkObj(val.taskStatus) + '</td>';
                }else if(val.taskStatus == '主管处理'){
                    tr += '<td style="background-color: #9ABC32;color: white">' + _checkObj(val.taskStatus) + '</td>';
                }else{
                    tr += '<td>' + _checkObj(val.taskStatus) + '</td>';
                }
                tr += '<td>' + _checkObj(val.drawer) + '</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccount)) {
                    outAccount += "|" + _checkObj(val.outAccount).substring(0, 3) + "**" + _checkObj(val.outAccount).substring(_checkObj(val.outAccount).length - 4, _checkObj(val.outAccount).length);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountOwner)) {
                    outAccount += "|" + _checkObj(val.outAccountOwner);
                } else {
                    outAccount += '无';
                }
                if (!outAccount || outAccount == '无无无') {
                    outAccount = '';
                }
                if (outAccount) {
                    tr += '<td>' +
                        "<a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                        + outAccount +
                        "</a>" +
                        '</td>';
                } else {
                    tr += '<td></td>';
                }
                if(subPageTabType=='dealingTask'){
                    idArray.push(val.accountId);
                    tr +='<td><div class="BankLogEvent" target="'+val.accountId+'"><span class="time"></span></div></td>';
                    //tr += '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + timeStamp2yyyyMMddHHmmss(_checkObj(val.asignTime)) + '" data-original-title="出款账号最新流水抓取时间">' + timeStamp2yyyyMMddHHmmss(_checkObj(val.asignTime)) + '</a></td>';
                }
                tr += '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + timeStamp2yyyyMMddHHmmss(_checkObj(val.asignTime)) + '" data-original-title="接单时间">' + timeStamp2yyyyMMddHHmmss(_checkObj(val.asignTime)) + '</a></td>';
                if (val.failedOutTime5) {
                    tr += '<td style="background-color: indianred;color: white">' + _checkObj(val.timeConsume) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.timeConsume) + '</td>';
                }
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td>'
                            + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
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
                    tr += '<td></td>';
                }
                if (!isHideImg&&_checkObj(val.photo)) {
                    if (hasScreenshot) {
                        tr += '<td>' +
                            '<a  href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + window.location.origin + '\/' + _checkObj(val.photo) + '\')">查看</a>' +
                            '</td>';
                    } else {
                        tr += '<td>' +
                            '<a   href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + window.location.origin + '\/' + _checkObj(val.photo) + '\')">回执</a>' +
                            '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                if (subPageType == 'troubleShooting'){
                    //接单中
                    if (ReviweTasksLock) {
                        tr += '<td><button type="button"  onclick="_lockTaskForCheck(' + val.taskId + ');"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                            '<i class="ace-icon fa fa-check bigger-100 blue"></i>排查</button></td>';
                    }else{
                        tr += '<td></td>';
                    }
                }
                if (subPageType=='dealingTask' ) {
                    if(val.taskStatus == '主管处理'){
                        tr += '<td style="width: 270px;text-align: center" id="masterOutOperate">';
                        if (Distributionflag) {
                            tr += '<button type="button" onclick="_distribution(' + val.taskId +');"  class="btn btn-xs btn-white btn-info btn-bold">' +
                                '<i class="ace-icon fa fa-user-circle-o  bigger-100 red"></i>分配</button>';
                        }
                        if (_checkObj(val.member) && TurningPlatformflag) {
                            tr += '<button type="button"  onclick="_beforeFeedBack(3,' + val.taskId + ',' + val.reqId + ');"  class="btn btn-xs btn-white btn-danger btn-bold">' +
                                '<i class="ace-icon fa fa-remove  bigger-100 red"></i>取消</button>';
                        }
                        if (_checkObj(val.member) && Freezeflag) {
                            tr += '<button type="button" onclick="_beforeFeedBack(4,' + val.taskId + ',' + val.reqId + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                                '<i class="ace-icon fa fa-hand-paper-o bigger-100 blue"></i>拒绝</button>';
                        }
                        if (Noteflag) {
                            tr += '<button type="button"  onclick="_saveRemark(' + val.taskId + ',\''+val.thirdRemarkFlag+'\');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                                '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                        }
                        if (FinishTask && val.accountId && val.thirdRemarkFlag == 'no') {
                            tr += '<button type="button"  onclick="_beforeTotalTurntoFinished(' + val.taskId + ',2);"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                                '<i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                        }
                        tr += '<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal('+val.accountId+',true)"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button></td>';
                    }else{
                        //待排查
                        if (hasScreenshot && UploadReceipt && _checkObj(val.member) && NewGenerationTask) {
                            tr += '<td style="width: 254px;text-align: center" class="no-padding-right">';
                        } else {
                            tr += '<td style="width: 210px;text-align: center" >';
                        }
                        if (_checkObj(val.member) && NewGenerationTask) {
                            //_distribution(' + val.id + ',' + '\'2\');
                            //_recreateTask(' + val.taskId + ',' + val.accountId + ');
                            tr += '<button onclick="_distribution(' + val.taskId + ',' + '\'3\');" type="button"  class=" btn btn-xs btn-white btn-warning  btn-bold">' +
                                '<i class="ace-icon fa fa-reply  bigger-100 red"></i>重新生成任务</button>';
                        }
                        if (Noteflag) {
                            tr += '<button type="button" onclick="_saveRemark(' + val.taskId +',\''+val.thirdRemarkFlag+ '\');"  class="btn btn-xs btn-white btn-info btn-bold ">' +
                                '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                        }
                        if (FinishTask) {
                            tr += '<button type="button"  onclick="_beforeTotalTurntoFinished(' + val.taskId + ',6);"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                                '<i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                        }
                        if (hasScreenshot && UploadReceipt) {
                            tr += '<button type="button"  onclick="_uploadReceiptPhoto(' + val.taskId + ');"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                                '<i class="fa fa-arrow-circle-up bigger-100 green"></i>回执</button>';
                        }
                        tr += '<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal('+val.accountId+',true)"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button></td>';
                    }

                }

                tr += '</tr>';
                amount += val.amount;
            }
        });
        trs =
            '<tr>' +
            '<td id="CurrentCount' + subPageType + '" colspan="4">小计：统计中..</td>' +
            '<td bgcolor="#579EC8" style="color:white;width:  80px;">' + parseFloat(amount).toFixed(3) + '</td>' +
            '<td colspan="15"></td></tr>' +
            '<tr>' +
            '<td id="AllCount' + subPageType + '" colspan="4">总共：统计中..</td>' +
            '<td id="TotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width:  80px;">统计中..</td>' +
            '<td colspan="15"></td></tr>';
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);
    showPading(res.page, subPageType + '_footPage', _searchTrouble);
    $("[data-toggle='popover']").popover();
    //加载账号悬浮提示
    loadHover_accountInfoHover(idList);
    if (subPageType=='dealingTask'){
        SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
    }
}
//取消  拒绝
function _beforeFeedBack(type, id, outwardRequest) {
    $('#feedback_taskId').val(id);
    $('#button_type').val(type);
    $('#feedback_remark').val('');

    if (type == 4) {
        $('#titleDiv').empty().append(
            '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">' +
            '<span class="white">&times;</span></button>任务拒绝');
    }
    if (type == 3) {
        $('#titleDiv').empty().append(
            '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">' +
            '<span class="white">&times;</span></button>任务取消');
    }

    $('#modal-table-toOut #successFlagBankName').hide();
    $('#modal-table-toOut #successFlagToAccountName').hide();
    $('#modal-table-toOut #successFlagOwner').hide();
    $('#modal-table-toOut #successFlagAccountNo').hide();
    $('#modal-table-toOut #successFlagAmount').hide();
    $('#remark').val('').prop('readonly', '').prop('style', '');
    //初始化模态框
    var flagM = false;
    $.ajax({
        type: 'get',
        url: '/r/outtask/getById',
        data: {"id": id, "outRequestId": outwardRequest, "type": null},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    flagM = true;
                    if (type != 1) {
                        $('#feedback_orderNo').text(res.data.orderNo);
                        $('#feedback_handicap').text(res.data.handicap);
                        $('#feedback_level').text(res.data.level);
                        $('#feedback_member').text(res.data.member);
                        $('#feedback_type').text(res.data.type);
                        $('#feedback_asignTime').text(timeStamp2yyyyMMddHHmmss(res.data.asignTime));
                        $('#feedback_amount').val(res.data.amount);

                        $('#feedback_userId').val(_checkObj(res.data.userId));
                        $('#feedback_taskId').val(_checkObj(res.data.taskId));
                        $('#feedback_orderNoInput').val(_checkObj(res.data.orderNo));
                        $('#feedback_memberCode').val(_checkObj(res.data.memberCode));
                    }

                }
            }
        }
    });
    $('#toAccount_error').prop('checked', '');
    $('#modal-table-feedback').modal('show');
}
// 主管 取消  拒绝
function _sureButton() {
    var taskId = $('#feedback_taskId').val();
    var remark = $('#feedback_remark').val();
    var type = $('#button_type').val();
    if (!taskId) {
        bootbox.alert("taskId参数丢失,请联系技术");
        return;
    }
    if (!remark) {
        $('#prompt').show(10).delay(1000).hide(10);
        return;
    }
    var flag = '';
    if (type == 3) {
        flag = "确定要取消吗？";
    }
    if (type == 4) {
        flag = "确定要拒绝吗？";
    }
    var accountError = '';
    if ($('#toAccount_error').prop('checked')) {
        accountError = $('#toAccount_error').val();
    }
    bootbox.confirm(flag, function (res) {
        if (res && type) {
            $.ajax({
                type: 'post',
                url: '/r/outtask/status',
                data: {
                    "taskId": taskId, "remark": remark, "status": type, "accountError": accountError
                },
                dataType: 'json',
                async: false,
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
                            $('#modal-table-feedback').modal('hide');
                            $('#feedback_remark').val('');
                            _searchTrouble(subPageTabType);
                        }
                    }
                }
            });
        }
    });
}
//分配之前--获取开户行信息  主管处理页签分配
function _distribution(id,type) {
    $('#distributionTaskId').val(id);
    $('#operatePage').val(type);
    var opt = '';
    $(bank_name_list).each(function (i, bankType) {
        opt += '<option>' + bankType + '</option>';
    });
    $('#form-field-select-allocateTask1').empty().html(opt);
    _initialMultiSelect();
    $('#allocateTaskRemark').val('');
    $('input[name="distributeObject"]:radio').prop('checked', false);
    if(type==3){
        $('#modal-distribution-header').text('重新生成并分配任务');
    }else{
        $('#modal-distribution-header').text('任务分配');
    }
    $('#modal-distribution').modal('show');
}
//分配操作  主管处理页签的分配
function _distributionTask() {
    var type = $('#operatePage').val();
    var taskId = $('#distributionTaskId').val();
    var bankTypes = $('#form-field-select-allocateTask1').val();
    var remark = $('#allocateTaskRemark').val();
    var distributeObject = '';
    $('input[name="distributeObject"]').each(function () {
        if ($(this).is(":checked")) {
            distributeObject = $(this).val();
        }
    });
    if(type!=3){
        if (!distributeObject) {
            $('#noDistributionObj').show(10).delay(1000).hide(10);
            return;
        } else {
            $('#noDistributionObj').hide();
        }
    }
    var url = '/r/outtask/reallocateTask';
    if(type=='3'){
        url= '/r/outtask/recreate';
    }
    $.ajax({
        type: 'post',
        url: url,
        data: {'taskId': taskId, 'type': distributeObject, "bankType": bankTypes.toString(), "remark": remark},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                if (res.status == 1) {
                    $('#modal-distribution').modal('hide');
                    $('#distributionTaskId').val('');
                    _searchTrouble(subPageTabType);
                }
            }
        }
    });
}
//点击排查，锁定任务，防止重新分配
function _lockTaskForCheck(taskId) {
    if (taskId){
        $.ajax({
            type:'get',data:{'taskId':taskId},dataType:'json',
            url:'/r/taskReview/lockTaskForCheck',
            success:function (res) {
                if (res){
                    $.gritter.add({
                        time: 1500,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                }
                _searchTrouble('troubleShooting');
            }
        });
    }
}
function _taskTotalPhoto(url) {
    $('#taskTotalImg').attr('src', url);
    $('#taskTotalImg').attr('download', url);
    $('#taskTotalImg').attr('href', url);
    $('#taskTotalImgModal').modal('show');
    if (browserIsIe()) {
        //是ie等,绑定事件
        $('#downLoadImgBtn').on("click", function () {
            var imgSrc = $(this).siblings("img").attr("src");
            //调用创建iframe的函数
            _downLoadReportIMG(url);
        });
    } else {
        $('#downLoadImgBtn').attr("download", url);
        $('#downLoadImgBtn').attr("href", url);
    }
}
function _uploadReceiptPhoto(taskId) {
    if (taskId) {
        bootbox.confirm("是否向工具发起上传回执消息？", function (res) {
            if (res) {
                $.ajax({
                    type: 'put',
                    url: '/r/outtask/uploadReceiptForTask',
                    dataType: 'json',
                    data: {"taskId": taskId},
                    success: function (res) {
                        if (res) {
                            $.gritter.add({
                                time: 1500,
                                class_name: '',
                                title: '系统消息',
                                text: res.message,
                                sticky: false,
                                image: '../images/message.png'
                            });
                            _searchTrouble('dealingTask');
                        }
                    }
                });
            }
        });
    }
}
function _getSum(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/taskReview/troubleShootSum', async: false,
        data: data,
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
function _getCount(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/taskReview/troubleShootCount', async: false,
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#CurrentCount' + type).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#AllCount' + type).empty().text('合计：' + res.page.totalElements + '条记录');
                    showPading(res.page, type + '_footPage', _searchTrouble);
                }
            }
        }
    });
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
                    _initialOrderText(id + 'detail', res.data);
                }
            }
        }
    });
}
var _beforeTotalTurntoFinished = function (id,type) {
    $('#CustomerserviceRemark').val('');
    $('#CustomerserviceRemark_id').val(id);
    $('#CustomerserviceRemark_modal').modal('show');
    $('#totalTaskFinishBTN').attr('onclick', '_exectuTransferFromFailTotal('+type+');');
};
//完成
function _exectuTransferFromFailTotal(type) {
    if (!$('#CustomerserviceRemark').val()) {
        $('#prompt_remark').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/finsh',
        data: {
            "type": (type==6?'failedOutToMatched':'mngdealToMatched'),
            "taskId": $('#CustomerserviceRemark_id').val(),
            "remark": $('#CustomerserviceRemark').val()
        },
        dataType: 'json',
        async: false,
        success: function (res) {
            $.gritter.add({
                time: 2000,
                class_name: '',
                title: '系统消息',
                text: res.message,
                sticky: false,
                image: '../images/message.png'
            });
            $('#totalTaskFinishBTN').attr('onclick', '');
            $('#CustomerserviceRemark_modal').modal('hide');
            _searchTrouble('dealingTask');
        }
    });
}
//添加备注
function _saveRemark(id, third) {
    if (third=='no') {
        $('#CustomerserviceRemark').val('');
        $('#CustomerserviceRemark_id').val(id);
        $('#CustomerserviceRemark_modal').modal('show');
        $('#totalTaskFinishBTN').attr('onclick', 'save_CustomerserviceRemark();');
        $('#thirdHandiacp').hide();
        $('#thirdAccountName').hide();
        $('#thirdOutAmount').hide();
        $('#reOutForThird').hide();
        $('#normalRemarkForThird').hide();
    } else {
        //第三方出款备注
        $('input[name="distributeObject"]').each(function () {
            $(this).prop('checked', '');
        });
        $('#thirdHandiacp').show();
        $('#thirdAccountName').show();
        $('#thirdOutAmount').show();
        $('#CustomerserviceRemark').val('');
        $('#CustomerserviceRemark_id').val(id);
        $('#reOutForThird').show();
        $('#normalRemarkForThird').show();
        $('#CustomerserviceRemark_modal').modal('show');
        $('#totalTaskFinishBTN').attr('onclick', '_saveRemarkForThird();');
    }
}
//保存第三方备注
function _saveRemarkForThird() {
    var taskId = $('#CustomerserviceRemark_id').val();
    var remarkTpe = $('input[name="distributeObject"]:checked').val();
    var remark;
    if (!remarkTpe) {
        $('#prompt_remark').text('请选择第三方出款的备注类型').show(10).delay(1500).hide(10);
        return;
    }
    remark = $('#CustomerserviceRemark').val();
    if (remarkTpe == 'normalRemark') {
        if (!remark) {
            $('#prompt_remark').text('请填写备注!').show(10).delay(1500).hide(10);
            return;
        }
    }
    var handicapName = '', accountName = '', amount = '';
    if (remarkTpe == 'reThirdOut') {
        handicapName = $.trim($('#thirdHandiacpValue').val());
        accountName = $.trim($('#thirdAccountNameValue').val());
        amount = $.trim($('#thirdOutAmountValue').val());
        if (!handicapName || !accountName || !amount) {
            $('#prompt_remark').text('重新出款,请填写相关信息!').show(10).delay(1500).hide(10);
            return;
        }
        if (!remark) {
            $('#prompt_remark').text('请填写备注!').show(10).delay(1500).hide(10);
            return;
        }
        remark = '{盘口:' + handicapName + ',商号:' + accountName + ',金额:' + amount + ',(备注:)' + remark + '(第三方出款)}';
    }

    $.ajax({
        type: 'post',
        url: '/r/outtask/remark',
        data: {"taskId": taskId, "remark": replaceIllegalChar(remark), "type": "review"},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 1000,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            $('#CustomerserviceRemark_id').val('');
            $('#CustomerserviceRemark_modal').modal('hide');
            _searchTrouble('dealingTask');
        }
    });
}
function save_CustomerserviceRemark() {
    var remark = $.trim($('#CustomerserviceRemark').val());
    if (!remark) {
        $('#prompt_remark').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    var taskId = $('#CustomerserviceRemark_id').val();
    $.ajax({
        type: 'post',
        url: '/r/outtask/remark',
        data: {"taskId": taskId, "remark": replaceIllegalChar(remark),"type": "review"},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            $('#CustomerserviceRemark_modal').modal('hide');
            _searchTrouble('dealingTask');
        }
    });
}
/**重新生成任务*/
function _recreateTask(taskId, accountId) {
    //重新生成任务时 检查是否存在流水，如果存在流水则给提示信息，避免重复出款
    $.ajax({
        type: 'post',
        url: '/r/outtask/checkBankLog',
        data: {'taskId': taskId},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    bootbox.confirm("存在流水，前去排查？", function (res) {
                        if (res) {
                            showInOutListModal(accountId);
                        } else {
                            $('#turnFailIdTotal').val(taskId);
                            $('#turnToFailRemarkTotal').val('');
                            $('#modal-turnToFailTotal').modal('show');
                            $('#confirmTurnToFailTotal').attr('onclick', '_confirmReGenernate();');
                        }
                    });
                } else {
                    $('#turnFailIdTotal').val(taskId);
                    $('#turnToFailRemarkTotal').val('');
                    $('#modal-turnToFailTotal').modal('show');
                    $('#confirmTurnToFailTotal').attr('onclick', '_confirmReGenernate();');
                }
            }
        }
    });
}
//确认生成任务
function _confirmReGenernate() {
    if (!$.trim($('#turnToFailRemarkTotal').val())) {
        $('#remark-turnToFailTotal').show(10).delay(1000).hide(10);
        return false;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/recreate',
        data: {'taskId': $('#turnFailIdTotal').val(), "remark": $.trim($('#turnToFailRemarkTotal').val())},
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
                _searchTrouble('dealingTask');
                $('#modal-turnToFailTotal').modal('hide');
            }
        }
    });
}

function _resetTrouble(type) {
    $('#timeScope_' + type).val('');
    $('#member_' + type).val('');
    $('#manual_' + type).prop('checked', 'checked');
    $('#timeScope_' + type).val('');
    $('#operator_' + type).val('');
    $('#robot_' + type).prop('checked', 'checked');
    $('#fromMoney_' + type).val('');
    $('#toMoney_' + type).val('');
    $('#account_' + type).val('');
    $('#orderNo_' + type).val('');
    _initialPageOnFresh(type);
}
//初始化时间控件
$('#timeScope_dealingTask').on("mouseup", function () {
    _datePickerForAll($('#dealingTask').find('input.date-range-picker'));
});
$('#timeScope_troubleShooting').on("mouseup", function () {
    _datePickerForAll($('#troubleShooting').find('input.date-range-picker'));
});
_datePickerForAll($('#troubleShooted').find('input.date-range-picker'));
var subPageTabType = null;
function _initialPageOnFresh(type) {
    subPageTabType = type;
    if (type == 'troubleShooting') {
        $('#changBtnTroubleShootLi').show();
        _showBtnStatus('fresh');
        _autoUpdateTime();//$('#freshTroubleShootingLi').show();
        _autoUpdateTimeTroubleShooting();
    } else {
        $('#changBtnTroubleShootLi').hide();
        $('#autoUpdateTimeTroubleShooting').empty();
        $('#freshTroubleShootingLi').hide();
        if (troubleShootingTime) {
            clearInterval(troubleShootingTime);
        }
    }
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
    _searchTrouble(type);
}
$('#handicap_' + subPageTabType).change(function () {
    _handicapTypeChange(this, type);
});
function _handicapTypeChange(obj, type) {
    var handicapId = $(obj).val();
    var levelOptions = '<option>请选择</option>';
    if (handicapId != '请选择') {
        levelOptions = getCurrentUserLevelByHandicapId(handicapId);
    }
    $('#level_' + type).empty().html(levelOptions);
    $('#level_' + type).trigger('chosen:updated');
    _searchTrouble(type);
}
//--------------------------快捷查询----start---------------------------
function _resetValuesForQuickQuery(){
    $('#member_QuickQuery').val('');$('#orderNo_QuickQuery').val('');
}
/**快捷查询*/
function _quickQueryClick() {
    if (troubleShootingTime) {
        clearInterval(troubleShootingTime);
    }
    if (!$.trim($('#orderNo_QuickQuery').val()) && !$.trim($('#member_QuickQuery').val())){
        $.gritter.add({
            time: 1500,
            class_name: '',
            title: '系统消息',
            text: '请输入订单号或者会员名',
            sticky: false,
            image: '../images/message.png'
        });
        return;
    }
    var openUrl = '/html/outward/quickQueryForOut.html?';//两个参数肯定其中有一个所以url默认带'?'
    if ($.trim($('#orderNo_QuickQuery').val())) {
        openUrl += 'orderNo=' + $.trim($('#orderNo_QuickQuery').val());
    } else {
        openUrl += 'orderNo=null';
    }
    if ($.trim($('#member_QuickQuery').val())) {
        openUrl += '&memberName=' + $.trim($('#member_QuickQuery').val());
    } else {
        openUrl += '&memberName=null';
    }
    var startTime = '', endTime = '';
    var startAndEnd = $('#timeScope_QuickQuery').val();
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
    if (startTime && endTime){
        openUrl += '&startTime=' + $.trim(startTime)+'&endTime='+$.trim(endTime);
    }else{
        openUrl += '&startTime=null&endTime=null';
    }
    //一个页面打开不同的子窗口 sessionStorage 对子父窗口都有效
    _openNewWindow(openUrl, "");
    //_searchQuickQuery(openUrl);
}
function _openNewWindow(url, winName) {
    var urlPrefix = window.location.origin + '/' + getCookie('JVERSION');
    url = urlPrefix + url;
    var winSet = "scrollbars=yes,menubar=no,resizable=yes,status=no,location=no,toolbar=no,alwaysRaised=yes,depended=yes";
    var _w = getSubWindow();
    var left = (_w.document.body.clientWidth ? _w.document.body.clientWidth : _w.document.documentElement.clientWidth) - 1100;
    if (left < 0) {
        left = 0;
    }
    var positions = ",width=1000,height=300,left=" + left + ",top=230";
    var winSizeStr = winSet + positions;
    _w.open(url, winName + '_blank', winSizeStr);
}
function getSubWindow() {
    var w = window;
    if (window != window.parent) {
        if (window.parent != window.parent.parent) {
            w = window.parent.parent;
        }
        else {
            w = window.parent;
        }
    }
    return w;
}
//    Processing(0, "正在审核"), Approved(1, "审核通过"), Reject(2, "拒绝"), ManagerProcessing(3, "主管处理"), Canceled(4, "已取消"), Acknowledged(5, "出款成功，平台已确认"), Failure(6, "出款成功，与平台确认失败");
function _showReqStatus(obj) {
    var status = '';
    if (obj == 0) {
        obj = 10110000;
    }
    switch (obj) {
        case 10110000:
            status = "正在审核";
            break;
        case 1:
            status = "审核通过";
            break;
        case 2:
            status = "拒绝";
            break;
        case 3:
            status = "主管处理";
            break;
        case 4:
            status = "已取消";
            break;
        case 5:
            status = "出款成功，平台已确认";
            break;
        case 6:
            status = "出款成功，与平台确认失败";
            break;
        default:
            status = '';
            break;
    }
    return status;
}
//Undeposit(0, "未出款"), Deposited(1, "已出款"), ManagerDeal(2, "主管处理"), ManageCancel(3, "主管取消"), ManageRefuse(4,
//"主管拒绝"), Matched(5, "流水匹配"), Failure(6, "转排查"), Invalid(7, "无效记录，已重新出款"), ManageDiscard(8, "主管丢弃");
function _showTaskStatus(obj) {
    var status = '';
    if (obj == 0) {
        obj = 10110000;
    }
    switch (obj) {
        case 10110000:
            status = "未出款";
            break;
        case 1:
            status = "已出款";
            break;
        case 2:
            status = "主管处理";
            break;
        case 3:
            status = "主管取消";
            break;
        case 4:
            status = "主管拒绝";
            break;
        case 5:
            status = "流水匹配";
            break;
        case 6:
            status = "转排查";
            break;
        case 7:
            status = "无效记录，已重新出款";
            break;
        case 8:
            status = "银行维护";
            break;
        default:
            status = '';
            break;
    }
    return status;
}
//--------------------------快捷查询----end---------------------------
_getPathAndHtml('historyDetail_troubleShoot');
_initialPageOnFresh('dealingTask');
