currentPageLocation = window.location.href;
var accountIdWhole = null;//当前页面的全局accountid
var accountNoWhole = null;
var ws_incomeCompany = null;
var playVideo = false;
var oldAccountIdArray = [];
var oldAccountsArray = [];
var oldAccountBankType = [];
var oldAccountOwener = [];
var oldAccountAlias = [];
var bankFlowAjaxDataFillter = {};
var incomeRequestAjaxDataFillter = {};
var startWork = false;
var accountBaseInfoMap = new Map();
var matchRightFlag = false;
var sendMessageRightFlag = false;
var addRemarkRightFlag = false;
var concealOrderRightFlag = false;
var addOrderRightFlag = false;
var finalDealBankFlowFlag = false;
var executeEventType = null;
var currentIncomeRequestPag = null;
var searchCompanyInType = null;
var lookUpMatchedTotalAmount = false;
var cancelIncomeReqFlag = false;
var generateIncomeOrder = null;//补提单标识，防止连续确定发起重复请求。
$.each(ContentRight['Income:*'], function (name, value) {
    if (name == 'Income:currentpageSum:*') {
        incomeCurrentPageSum=true;
    }
    if (name =='Income:allRecordSum:*'){
        incomeAllRecordSum=true;
    }
});
$('#incomeRequestPageUL li').each(function () {
    if ($(this).prop('class') == 'active') {
        currentIncomeRequestPag = $(this).find('a').attr('href');
    }
    if (currentIncomeRequestPag != '#toMatch') {
        $('#changBtnDivIncomeReq').attr('style', 'display:none');
    } else {
        $('#changBtnDivIncomeReq').attr('style', 'display:block');
    }
});
function _getIncomeRequestPageAhref(obj) {
    currentIncomeRequestPag = $(obj).find('a').attr('href');
    if (currentIncomeRequestPag != '#toMatch') {
        $('#changBtnDivIncomeReq').attr('style', 'display:none');
        $('#accountStatus_li').hide();
    } else {
        $('#changBtnDivIncomeReq').attr('style', 'display:block');
        $('#accountStatus_li').show();
    }
}
function _connect_incomeCompany() {
    if (window.location.protocol == 'http:') {
        ws_incomeCompany = new WebSocket('ws://' + window.location.host + '/ws/income?Audit=Comp&timeStamp=' + new Date());
    } else {
        ws_incomeCompany = new WebSocket('wss://' + window.location.host + '/ws/income?Audit=Comp');
    }
    ws_incomeCompany.onmessage = function (event) {
        var jsonObj = JSON.parse(event.data);
        if (jsonObj) {
            if (jsonObj instanceof Array) {
                startWork = true;
                var accountIdArray = [];//账号id
                var accountsArray = [];//账号
                var accountStatus = [];//账号状态 与账号id一一对应
                //var incomeAuditFrom = [];//提单 或者 流水 与账号id顺序一致且与monitorStatus一一对应
                var monitorStatus = [];//监控状态 与账号id顺序一致
                var accountBankType = [];
                var accountAlias = [];
                var accountOwner = [];
                for (var i = 0, L = jsonObj.length; i < L; i++) {
                    accountIdArray[i] = jsonObj[i].accountId;
                    accountsArray[i] = jsonObj[i].account;
                    accountStatus[i] = jsonObj[i].status;
                    //incomeAuditFrom[i] = jsonObj[i].incomeAuditWsFrom;
                    monitorStatus[i] = jsonObj[i].monitor;
                    accountBankType[i] = jsonObj[i].bankType;
                    accountAlias[i] = jsonObj[i].alias;
                    accountOwner[i] = jsonObj[i].owner;
                    accountBaseInfoMap.set(jsonObj[i].accountId, jsonObj[i]);
                }
                console.log("页面保存的原有的账号：" + oldAccountIdArray + ",socket新发来的账号：" + accountIdArray);
                //账号分配 渲染页面  之前的逻辑是：后台按顺序发送来账号，如果账号长度不变则表示账号没有发生改变
                //但是现在的逻辑是：没有按顺序发送，而是按照正常，停用，冻结的顺序发送来的
                if (oldAccountIdArray && oldAccountIdArray.length > 0) {
                    if (_refreshPage(accountIdArray, accountsArray, accountBankType, accountAlias, accountOwner)) {
                        _showLi();// 接单成功 显示页签
                        _initialPageByAccounts();
                    }
                } else {
                    oldAccountIdArray = accountIdArray;
                    oldAccountsArray = accountsArray;
                    oldAccountBankType = accountBankType;
                    oldAccountAlias = accountAlias;
                    oldAccountOwener = accountOwner;
                    _showLi();// 接单成功 显示页签
                    _initialPageByAccounts();
                    _monitorBankFlowAccount();
                }
                //2.账号状态 显示  //3.监控银行流水抓取状态 显示
                for (var i = 0, L = accountStatus.length; i < L; i++) {
                    _showAccountStatus(accountIdArray[i], accountStatus[i]);    _showLiAccount();
                    _showMonitorStatus(accountIdArray[i], monitorStatus[i]);
                }
                if (!startWork) {
                    $('#accountInfoDiv').empty().html('<div><h3 style="text-align: center;color: #6243ff">接单服务异常，请联系技术人员！</h3></div>').show();
                }
                _initialSearchByAccountId();
            }
            else {
                if (oldAccountIdArray && oldAccountIdArray.length>0 && oldAccountIdArray.indexOf(jsonObj.accountId)<0){
                    return;
                }
                //刷新 通知 0 1 3  入款请求 银行流水 全部
                //jsonObj : message  incomeAuditWsFrom  accountId
                var messageFromPlat = '';
                if ((Number(jsonObj.incomeAuditWsFrom) == 6 || Number(jsonObj.incomeAuditWsFrom) == 7) && jsonObj.owner == getCookie('JUID')) {
                    _getMessage(jsonObj.accountId, Number(jsonObj.incomeAuditWsFrom), jsonObj.message);
                    return;
                }
                var message = '';
                if (Number(jsonObj.incomeAuditWsFrom) == 3) {
                    if (jsonObj.message) {
                        messageFromPlat = jsonObj.message;
                    }
                    if (messageFromPlat =='1') {
                        message = '匹配成功';
                    } else {
                        message = messageFromPlat;
                    }
                } else {
                    message = jsonObj.message;
                }
                _getMessage(jsonObj.accountId, Number(jsonObj.incomeAuditWsFrom), message);
                _initialSearchByAccountId();
            }
        }
    };
    ws_incomeCompany.onclose = function (e) {
        if (e.code && e.reason != 'History') {
            ws_incomeCompany = null;
            oldAccountIdArray = null;
            oldAccountsArray = null;
            $('#P' + accountIdWhole + ' #connectStatus').text('断开');
            $('#P' + accountIdWhole + '   #connectStatus').attr('style', 'color:white;');
            $('#P' + accountIdWhole + '   #connectStatus').parent().attr('class', 'btn btn-danger btn-sm pull-left');

        }
        if (e.code != 1000) {
            _keepConnection();
        }
    }
}
function _initialSearchByAccountId() {
    if (oldAccountIdArray&&oldAccountIdArray.length>0){
        $.each(oldAccountIdArray,function (i,val) {
            if(val!=accountIdWhole){
                _searchBtn(val, 4, 0);
            }
        });
    }
}
function disconnectForIncompany() {
    if (ws_incomeCompany != null) {
        ws_incomeCompany.close();
        console.log("关闭socket");
    }
}
window.onbeforeunload = function () {
    disconnectForIncompany();
};
function _getMessage(id, type, message) {
    var accountIds = '';
    if (oldAccountIdArray && oldAccountIdArray.length > 0) {
        accountIds = oldAccountIdArray;
    }
    if (accountIds && accountIds.indexOf(id) >= 0) {
        $.session.set("latestUpdateTime" + id, _latestUpdateTime(new Date().getTime()));
        var latestUpdateTime = $.session.get("latestUpdateTime" + id);
        $('#P' + id + '  #latestUpdateTime').html(latestUpdateTime);
        _getDataList(id, type, message);
    }
}
function _setButtonRight(accountId) {
    $.each(ContentRight['IncomeAuditComp:*'], function (rightKey, value) {
        if (rightKey == IncomeRequestMatchRight) {
            //匹配按钮权限
            matchRightFlag = true;
        }
    });
    if (matchRightFlag) {
        $('#P' + accountId + '   #matchRightFlagBtn').show();
    }
}
/**
 * 根据服务端消息刷新页面 参数 银行账号id 查询表的类型
 * @private
 */
function _getDataList(id, type, message) {
    //不论是当前账号 还是其他页签的账号来单都提示语音  银行流水和已匹配来的消息不提示语音
    //而且不是取消和匹配动作时候： "matchAction" 取消动作 "cancelAction";
    //改为有流水提示声音 和数字显示 type != 3 && type != 0 && type != 4 && type != 5 && type != 6 && type != 7
    if (id && type == 1) {
        //未读信息 语音提示
        if (!playVideo && currentPageLocation.indexOf('IncomeAuditComp:*') > -1) {
            playSound();
        }
    }
    if (type == 4) {
        $.gritter.add({
            time: 10000,
            class_name: '',
            title: '系统消息',
            text: message,
            sticky: false,
            image: '../images/message.png'
        });
        _searchBtn(id, 0, 0);//刷新流水
        return;
    }
    if (type == 5) {
        $.gritter.add({
            time: 10000,
            class_name: '',
            title: '系统消息',
            text: message,
            sticky: false,
            image: '../images/message.png'
        });
        return;
    }
    if (type == 6) {
        //不是客服才会接到消息 && !sendMessageRightFlag
        $.gritter.add({
            time: '',
            class_name: '',
            title: '客服消息',
            text: message,
            sticky: true,
            image: '../images/message.png'
        });
        return;
    }
    if (type == 7) {
        //客服补提单
        $.gritter.add({
            time: '',
            class_name: '',
            title: '客服补提单消息',
            text: message ? message : '补提单失败!',
            sticky: true,
            image: '../images/message.png'
        });
        return;
    }
    //获取每个li 判断点击状态  如果是当前页签则查询刷新 否则根据websocket消息添加未读数量提示
    // 只显示流水的来单数量(message && type == 3) || type != 3 )
    if (type == 1) {
        var lis = $('#accountsNavUL li');
        for (var i = 0; i < lis.length; i++) {
            var active_accountId = $(lis[i]).attr('li_acccountId');
            if (active_accountId == id) {
                if (lis[i].className == 'active') {
                    $(lis[i]).find('span').text('');
                } else {
                    $(lis[i]).find('span').text(Number($(lis[i]).find('span').text()) + 1);
                }
            }
        }
    }
    if ((type == 1 || type == 0 || type == 3) && $('#account' + id + '  input[name="autoFlush"]:checked').val() == 1) {
        _searchBtn(id, type, 0);//刷新
        // if (type == 3) {
        //     console.log(message);
        //     $.gritter.add({
        //         time: 10000,
        //         class_name: '',
        //         title: '系统消息',
        //         text: message,
        //         sticky: false,
        //         image: '../images/message.png'
        //     });
        // }
    }
}
/**
 * 判断是否重新渲染页面
 * @param accountIdArray
 * @param accountsArray
 * @returns {boolean}
 * @private
 */
function _refreshPage(accountIdArray, accountsArray, bankType, accountAlias, accountOwner) {
    var refreshFlag = true;
    var L1 = oldAccountIdArray.length;
    var L2 = accountIdArray.length;
    var count = 0;
    if (accountIdArray && accountsArray) {
        if (L1 == L2) {
            for (var i = 0; i < L1; i++) {
                for (var j = 0; j < L2; j++) {
                    if (oldAccountIdArray[i] == accountIdArray[j]) {
                        count++;
                    }
                }
            }
        }
    }
    if (count > 0 && count == L1) {
        refreshFlag = false;
    } else {
        oldAccountIdArray = accountIdArray;
        oldAccountsArray = accountsArray;
        oldAccountBankType = bankType;
        oldAccountAlias = accountAlias;
        oldAccountOwener = accountOwner;
    }
    return refreshFlag;
}
/**
 * 显示 账号状态
 * @param accountId
 * @param accountStatus
 * @private
 */
function _showAccountStatus(accountId, accountStatus) {
    //accountStatus 1 正常  4 停用 3 冻结 'style','background-color: #00B83F;color: #FFF;'
    if (accountId && accountStatus) {
        switch (accountStatus) {
            case 1:
                $('#accountColor' + accountId).attr('style', 'color: #00af0e');
                $('#accountColor' + accountId).attr('color_value', 1);
                break;
            case 3:
                $('#accountColor' + accountId).attr('style', 'font-weight:bolder;color: red;');
                $('#accountColor' + accountId).attr('color_value', 3);
                break;
            case 4:
                $('#accountColor' + accountId).attr('style', 'font-weight:bolder;color: orange;');
                $('#accountColor' + accountId).attr('color_value', 4);
                break;
        }
    }
    //停用 冻结的账号 查询同一个层级下其他账号的提单记录
}
//选择性显示接单的入款账号
$('input[name="accountStatus_show"]').on('click', function () {

    if (this.value == 1) {
        this.value = -1;
        $(this).prop('checked', '');
    } else if (this.value == -1) {
        this.value = 1;
        $(this).prop('checked', 'checked');
    } else if (this.value == 3) {
        this.value = -3;
        $(this).prop('checked', '');
    } else if (this.value == -3) {
        this.value = 3;
        $(this).prop('checked', 'checked');
    } else if (this.value == 4) {
        this.value =-4;
        $(this).prop('checked', '');
    } else if (this.value == -4) {
        this.value = 4;
        $(this).prop('checked', 'checked');
    }
    _showLiAccount();
});
function _showLiAccount() {
    var lis = $('#accountsNavUL li');
    if(!lis || !lis.html()|| lis.html().length==0){
        return ;
    }
    var statusSelected =[];
    $('input[name="accountStatus_show"]:checked').each(function () {
        statusSelected.push(this.value);
    });
    if(statusSelected.length>0){
        for (var i = 0; i < lis.length; i++) {
            if (statusSelected.indexOf($(lis[i]).find('label').attr('color_value'))>-1) {
                $(lis[i]).show();
            } else {
                $(lis[i]).hide();
            }
        }
    }else{
        $('#accountsNavUL li').each(function () {
            $(this).show();
        });
    }
    $('#accountsNavUL li:first-child').attr('class', 'active');
    var accountIdToDiv = $('#accountsNavUL li:first-child').attr('li_acccountid');
    $('#selectedAccountsDiv div[account=' + $.trim(accountIdToDiv) + ']').attr('class', 'tab-pane in active');
}
/**
 * 显示 监控状态：抓流水状态
 * @param incomeAuditWsFrom
 * @param monitorStatus
 * @private
 */
function _showMonitorStatus(accountId, monitorStatus) {
    //monitorStatus 0 1 2 3 4 离线，未抓取-OFFLINE，正常，抓取中-NORMAL，异常，超时未上报-ERROR, 暂停-PAUSE
    //monitorAccountStatusUnstart=0,monitorAccountStatusAcquisition=1,monitorAccountStatusTimeout=2,monitorAccountStatusPause=3;
    switch (monitorStatus) {
        case monitorAccountStatusUnstart:
            //var latestGetFlowTime = $.session.get("latestGetFlowTime" + accountId);
            // $('#P' + accountId + '   #flowConnectStatus').text('离线');
            // $('#P' + accountId + '   #flowConnectStatus').attr('style', 'color:white;');
            // $('#P' + accountId + '   #flowConnectStatus').parent().attr('class', 'btn btn-default btn-sm pull-right');
            // if (latestGetFlowTime) {
            //     $('#P' + accountId + ' #latestGetFlowTime').html(latestGetFlowTime);
            // } else {
            //     $('#P' + accountId + ' #latestGetFlowTime').html(_latestUpdateTime(new Date().getTime()));
            // }
            break;
        case monitorAccountStatusAcquisition:
            //$.session.set("latestGetFlowTime" + accountId, _latestUpdateTime(new Date().getTime()));
            //var latestGetFlowTime = $.session.get("latestGetFlowTime" + accountId);
            // $('#P' + accountId + '   #flowConnectStatus').text('正常');
            // $('#P' + accountId + '   #flowConnectStatus').attr('style', 'color:white;');
            // $('#P' + accountId + '   #flowConnectStatus').parent().attr('class', '').attr('class', 'btn btn-success btn-sm pull-right');

            //$('#P' + accountId + '  #latestGetFlowTime').html(latestGetFlowTime);
            // if (latestGetFlowTime) {
            //     $('#P' + accountId + ' #latestGetFlowTime').html(latestGetFlowTime);
            // } else {
            //     $('#P' + accountId + ' #latestGetFlowTime').html(_latestUpdateTime(new Date().getTime()));
            // }
            break;
        case monitorAccountStatusTimeout:
            //var latestGetFlowTime = $.session.get("latestGetFlowTime" + accountId);
            // $('#P' + accountId + '   #flowConnectStatus').text('异常');
            // $('#P' + accountId + '   #flowConnectStatus').attr('style', 'color:white;');
            // $('#P' + accountId + '   #flowConnectStatus').parent().attr('class', 'btn btn-danger btn-sm pull-right');
            // if (latestGetFlowTime) {
            //     $('#P' + accountId + ' #latestGetFlowTime').html(latestGetFlowTime);
            // } else {
            //     $('#P' + accountId + ' #latestGetFlowTime').html(_latestUpdateTime(new Date().getTime()));
            // }
            break;
        case monitorAccountStatusPause:
            //var latestGetFlowTime = $.session.get("latestGetFlowTime" + accountId);
            // $('#P' + accountId + '   #flowConnectStatus').text('暂停');
            // $('#P' + accountId + '   #flowConnectStatus').attr('style', 'color:white');
            // $('#P' + accountId + '   #flowConnectStatus').parent().attr('class', 'btn btn-grey btn-sm pull-right');

            // if (latestGetFlowTime) {
            //     $('#P' + accountId + ' #latestGetFlowTime').html(latestGetFlowTime);
            // } else {
            //     $('#P' + accountId + ' #latestGetFlowTime').html(_latestUpdateTime(new Date().getTime()));
            // }
            break;
    }
}

function _keepConnection() {
    if ($.session.get('connetFlag')) {
        if (ws_incomeCompany != null) {
            ws_incomeCompany.close();
            ws_incomeCompany = null;
        }
        _connect_incomeCompany();
        $('#P' + accountIdWhole + '   #connectStatus').text('正常');
        $('#P' + accountIdWhole + '   #connectStatus').attr('style', 'color:white;');
        $('#P' + accountIdWhole + '   #connectStatus').parent().attr('class', 'btn btn-success btn-sm pull-left');
    } else {
        disconnectForIncompany();
    }
}
/**
 * 页面初始化 刷新
 * @private
 */
function _initialPage() {
    if (getCookie('JUID') == 'admin') {
        $('#matchedLi').show();
        $('#unmatchLi').show();
        $('#canceledLi').show();
    }
    _datePickerForAll($('#toMatch').find("input.date-range-picker"));
    if ($.session.get('connetFlag') && $.session.get('connetFlag') == 'true') {
        _startApprove();
        _keepConnection();

    } else {
        _stopApprove();
    }
}
function changeReadonly() {
    var valueWhy = $('input:radio[name=radioWhy]:checked').val();
    if (valueWhy == '其它') {
        $('textarea[name=whyText]').removeAttr("readonly");
    } else {
        $('textarea[name=whyText]').attr("readonly", "readonly");
        $("#whyText").val("");
    }
}
function closeStopOrder() {
    $('#stopOrder').modal('hide');
    $('#modal_choose').show();
}
function subStopOrder() {
    var remark = "";
    if ($('input:radio[name=radioWhy]:checked').val() == '其它') {
        remark = $.trim($("#whyText").val());
    } else {
        remark = $('input:radio[name=radioWhy]:checked').val();
    }
    if (remark == "" || remark == undefined) {
        $('#remarkWhyText').show(100).delay(2000).hide(100);
        return false;
    }
    bootbox.confirm("确定停止审核吗？", function (res) {
        if (res) {
            //保存停止接单原因
            $.ajax({
                type: 'post',
                url: '/r/income/stoporder',
                data: {"remark": remark, "type": '2', "localHostIp": localHostIp},
                async: false,
                dataType: 'json',
                success: function (res) {
                    if (res && res.status == 1) {
                        closeStopOrder();
                        //停止接单关闭socket
                        _stopApprove();
                        $.session.remove('connetFlag');
                        oldAccountIdArray = null,oldAccountsArray = null, accountIdWhole = null;
                        accountNoWhole = null;
                        disconnectForIncompany();
                        $.gritter.add({
                            time: 400,
                            class_name: '',
                            title: '系统消息',
                            text: '保存成功，停止接单成功！',
                            sticky: false,
                            image: '../images/message.png'
                        });
                    }
                }
            });
        }
    });
}

function startIncomeAppove() {
    if (currentIncomeRequestPag != '#toMatch') {
        return;
    }
    if (!ContentRight['IncomeAuditComp:*'] || !ContentRight['Income:*']) {
        $.gritter.add({
            time: 1400,
            class_name: '',
            title: '系统消息',
            text: '没有权限接单,请主管分配权限！',
            sticky: false,
            image: '../images/message.png'
        });
        return false;
    } else {
        var flag = false;
        $.each(ContentRight['IncomeAuditComp:*'], function (name, value) {
            if (name == 'IncomeAuditComp:StartWork:*') {
                flag = true;
            }
        });
        if (!flag) {
            $.gritter.add({
                time: 1400,
                class_name: '',
                title: '系统消息',
                text: '没有权限接单,请主管分配权限！',
                sticky: false,
                image: '../images/message.png'
            });
            return false;
        }
    }
    $.ajax({
        type: 'post',
        url: '/r/income/stoporder',
        data: {"remark": "开始接单", "type": '1', "localHostIp": localHostIp},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res && res.status == 1) {
                _startApprove();
                $.session.set('connetFlag', true);
                _keepConnection();
            } else {
                $.gritter.add({
                    time: 2000,
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
 * 接单按钮
 * @param btn
 * @private
 */
function _startClick(btn) {
    if (btn) {
        var val = $(btn).attr('btn_value');
        if (val == 0) {
            //记录开始接单人 到数据库
            startIncomeAppove();
        }
        if (val == 1) {
            var isStoppable = false;
            $.ajax({
                type: 'get',
                url: '/r/account/validStopAudit4Income',
                data: {},
                dataType: 'json',
                async: false,
                success: function (res) {
                    if (res) {
                        if (res.data == 1) {
                            isStoppable = true;
                        } else {
                            $.gritter.add({
                                time: 400,
                                class_name: '',
                                title: '系统消息',
                                text: '您现在无法停止接单，请稍后！',
                                sticky: false,
                                image: '../images/message.png'
                            });
                            return false;
                        }
                    }
                }
            });
            if (isStoppable) {
                $('#stopOrder').modal('show');
            }
        }
        // _keepConnection();
    }
}
//语音提醒
function playSound() {
    playVideo = true;
    var borswer = window.navigator.userAgent.toLowerCase();
    var url = window.location.origin + '/' + sysVersoin + '/javascript/income/audit/streamsound.mp3';
    if (borswer.indexOf("ie") >= 0) {
        //IE内核浏览器
        var strEmbed = '<embed name="embedPlay" src="' + url + '" autostart="true" hidden="true" loop="false" quality="high"  pluginspage="http://www.adobe.com/go/getflashplayer" play="true" type="application/x-shockwave-flash" menu="false" ></embed>';
        if ($("#myContent").find("embed").length <= 0)
            $("#myContent").append(strEmbed);
        var embed = document.embedPlay;

        //浏览器不支持 audion，则使用 embed 播放
        embed.volume = 100;
        embed.play();
    } else {
        //非IE内核浏览器
        var strAudio = "<audio id='audioPlay' src='" + url + "' pluginspage='http://www.adobe.com/go/getflashplayer' play='true' hidden='true'>";
        if ($("#myContent").find("audio").length <= 0)
            $("#myContent").append(strAudio);
        var audio = document.getElementById("audioPlay");
        //浏览器支持 audion
        if (audio) {
            audio.play();
            audio.addEventListener("ended", function () {
                console.log("音频已播放完成");
                playVideo = false;
                $("#myContent").empty();
            });
        }
    }
}
/**
 * 显示停止接单
 * @private
 */
function _startApprove() {
    $('#changBtnIncomeRequest').empty().html('<i id="start-websocket-i" class="bigger-120 ace-icon fa fa-pause red"></i><span>停止接单</span>');
    $('#changBtnIncomeRequest').attr('btn_value', 1);
    $('#startPrompt').hide();
}
/**
 * 显示开始接单
 * @private
 */
function _stopApprove() {
    $('#changBtnIncomeRequest').empty().html('<i id="start-websocket-i" class="bigger-120 ace-icon fa fa-play green"></i><span>开始接单</span>');
    $('#changBtnIncomeRequest').attr('btn_value', 0);
    $('#accountInfoDiv').hide();
    $('#startPrompt').show();
}
/**
 * 根据登陆用户信息显示是否显示页签
 * @private
 */
function _showLi() {
    var uid = getCookie('JUID');
    if (uid == 'admin') {
        $('#matchedLi').attr('style', 'display:block;');
        $('#unmatchLi').attr('style', 'display:block;');
        $('#canceledLi').attr('style', 'display:block;');
    } else {
        if (oldAccountIdArray && oldAccountIdArray.length > 0) {
            $('#matchedLi').attr('style', 'display:block;');
            $('#unmatchLi').attr('style', 'display:block;');
            $('#canceledLi').attr('style', 'display:block;');
            $('#accountStatus_li').show();
        } else {
            $('#matchedLi').attr('style', 'display:none;');
            $('#unmatchLi').attr('style', 'display:none;');
            $('#canceledLi').attr('style', 'display:none;');$('#accountStatus_li').hide();
        }
    }
}
/**
 * 点击开始接单 或者刷新 的时候初始化页面
 * @private
 */
function _initialPageByAccounts() {
    //根据账号个数来制定页签
    var accountIds = oldAccountIdArray;
    var accounts = oldAccountsArray;
    var accountBankType = oldAccountBankType;
    var accountAlias = oldAccountAlias;
    var accountOwner = oldAccountOwener;
    $('#accountInfoDiv').empty();
    if (accountIds && accountIds.length > 0) {
        var subLi = '';
        var selectedAccountsDiv = '<div id="selectedAccountsDiv" style="padding-top: 0px;padding-bottom: 0px;" class="tab-content">';
        var allAccounts = '';
        for (var index = 0; index < accountIds.length; index++) {
            //tab标签
            if (index == 0) {
                subLi +=
                    '<li id="firstLi" li_acccountNo="' + accounts[index] + '" li_acccountId="' + accountIds[index] + '" onclick="_liOnclick(\'' + accountIds[index] + '\',\'' + accounts[index] + '\');">';
            }
            else {
                subLi +=
                    '<li li_acccountNo="' + accounts[index] + '" li_acccountId="' + accountIds[index] + '"  onclick="_liOnclick(\'' + accountIds[index] + '\',\'' + accounts[index] + '\');">';
            }
            subLi +=
                '<a  data-toggle="tab" href="#account' + accountIds[index] + '">' +
                '<span class="badge badge-important" id="span' + accountIds[index] + '"></span>' +
                '<label id="accountColor' + accountIds[index] + '">';
            if (accountAlias[index]) {
                subLi += accountAlias[index] + '|';
            } else {
                if (accounts[index].length > 6) {
                    subLi += accounts[index].substring(0, 2) + '*' + accounts[index].substring(accounts[index].length - 3) + '|';
                } else {
                    subLi += accounts[index] + '|';
                }
            }
            if (accountOwner[index]) {
                subLi += accountOwner[index] + '|';
            } else {
                subLi += '无|';
            }
            if (accountBankType[index]) {
                if (accountBankType[index].length > 4) {
                    subLi += accountBankType[index].substring(accounts[index].length - 4);
                } else {
                    subLi += accountBankType[index];
                }
            } else {
                subLi += '无';
            }
            subLi += '</label></a></li>';
            //每个账号的搜索栏
            var search =
                '<h3 class="row  smaller lighter blue  less-margin no-margin-left">' +
                '<div class="col-sm-12">' +
                '<form class="form-inline">' +
                '<div class="col-sm-3">' +
                '<span class="label label-lg label-primary arrowed-right">层级</span>' +
                '<select id="level' + accountIds[index] + '" style="height:32px;width: 181px" class="chosen-select form-control" placeholder="层级">' +
                '</select>' +
                '</div>' +
                '<div class="col-sm-3">' +
                '<span class="label label-lg label-primary arrowed-right">订单号</span>' +
                '<input id="orderNo' + accountIds[index] + '" type="text" style="height:32px" class="input-md" placeholder="提单号"/>' +
                '</div>' +
                '<div class="col-sm-3">' +
                '<span style="width: 42px" class="label label-lg label-primary  arrowed-right "><i class="fa fa-calendar bigger-110"></i></span>' +
                '<input id="timeScopeToMatch' + accountIds[index] + '" class="date-range-picker" type="text" name="date-range-picker" style="height: 32px;width:75%;"/>' +
                '</div>' +
                '<div class="col-sm-3">' +
                '<span class="label label-lg label-primary arrowed-right">会员账号</span>' +
                '<input id="member' + accountIds[index] + '" type="text" style="height:32px;width: 67%;" class="input-medium" placeholder="会员账号"/>' +
                '</div>' +
                '<div class="col-sm-12" style="height:2px;"></div>' +
                '<div class="col-sm-3">' +
                '<span class="label label-lg label-primary arrowed-right">金额</span>' +
                '<div class="input-group form-group" >' +
                '<input onkeyup="clearNoNum(this)" id="fromMoney' + accountIds[index] + '" type="text" class="form-control" style="height:32px;width:80px;">' +
                '<span class="input-group-addon" style="width:20px;">~</span>' +
                '<input onkeyup="clearNoNum(this)" id="toMoney' + accountIds[index] + '" type="text" class="form-control " style="height:32px;width:66px;">' +
                '</div>' +
                '</div>' +
                '<div class="col-sm-3">' +
                '<span class="label label-lg label-primary arrowed-right">存款人</span>' +
                '<input id="payMan' + accountIds[index] + '" type="text" style="height:32px" class="input-md" placeholder="存款人(付款人)"/>' +
                '</div>' +
                '<div class="col-sm-3">' +
                '<span class="label label-md label-important">自动刷新：</span>' +
                '<span style="padding-left: 30px;">' +
                '<label>' +
                '<input name="autoFlush" class="ace" value="1" checked="checked"  type="radio"><span class="lbl">是</span></label>&nbsp;&nbsp;' +
                '<label>' +
                '<input name="autoFlush" class="ace" value="0"  type="radio"><span class="lbl">否</span></label>' +
                '</span>' +
                '</div>' +
                '<div class="col-sm-3">' +
                '<label class="inline"><input id="searchSys' + accountIds[index] + '" type="checkbox" name="search_IN_status" class="ace" value="1" ><span class="lbl">查提单</span></label>' +
                '<label class="inline"><input id="searchBank' + accountIds[index] + '" type="checkbox" name="search_IN_status" class="ace" value="2" ><span class="lbl">查流水</span></label>' +
                '<span style="padding-left:3px;">' +
                '<button type="button" id="searchBtn' + accountIds[index] + '" onclick="_searchBtnClick();" class="btn btn-xs btn-white btn-info btn-bold ">' +
                '<i class="ace-icon fa fa-search bigger-100 green"></i>查询' +
                '</button>' +
                '<span>&nbsp;&nbsp;</span>' +
                '<button type="button" id="resetValue' + accountIds[index] + '" onclick="_resetValue(' + accountIds[index] + ');" class="btn btn-xs btn-white btn-info btn-bold ">' +
                '<i class="fa fa-refresh bigger-100 red"></i>重置' +
                '</button>' +
                '</span>' +
                '</div>' +
                '       </form>' +
                '   </div>' +
                '</h3>';
            //每个tab标签的div
            var accountDiv = '<div id="account' + accountIds[index] + '" ';
            if (index != 0) {
                accountDiv += 'class="tab-pane in ">';
            } else {
                accountDiv += 'class="tab-pane in active">';
            }
            accountDiv += search;
            var sysRequestDiv = '<div  id="inComeRequest' + accountIds[index] + '" style="margin-right: -12px;margin-left: -12px" class="">';
            accountDiv += sysRequestDiv;
            var sysTable =
                '<table class="table  table-bordered table-hover no-margin-bottom">' +
                '<thead><tr><th>选择</th><th>盘口</th><th>层级</th><th>会员号</th><th>存款人</th>' +
                '<th>充值金额</th><th>订单号</th><th>提单时间</th><th>备注</th><th>操作</th></tr></thead>' +
                '<tbody></tbody>' +
                '</table>';
            //<th>支付确认码</th>
            accountDiv += sysTable;
            var sysFooter =
                '<div id="sysPageFooter" class="message-footer clearfix no-padding" >' +
                '   <input type="hidden"  name="pagingPrevious">' +
                '   <input type="hidden"  name="pagingNext">' +
                '   <input type="hidden"  name="pagingLast">' +
                '   <div class="pull-left">' +
                '       <span class="label label-info">提单记录</span>' +
                '   </div>' +
                '   <div id="obtainBankFlowsPromt" style="text-align: center;display: none;" class="col-sm-8">' +
                '       <span style="color:orangered;font-size: 15px">温馨提示：若想尽快抓取流水以加快匹配速度，可以点击"立即抓取流水"按钮!</span> ' +
                '   </div>' +
                '   <div class="pull-right">' +
                '       <div class="paging_from_to inline middle">第 0 / 0 页</div>' +
                '       <ul class="pagination middle">' +
                '           <li class="paging_first ">' +
                '               <a href="javascript:void(0);" class="paging_first_a" >' +
                '                   <i class="ace-icon fa fa-step-backward middle"></i>' +
                '               </a>' +
                '           </li>' +
                '           <li class="paging_previous "><a href="javascript:void(0);" class="paging_previous_a"><i class="ace-icon fa fa-caret-left bigger-140 middle"></i></a></li>' +
                '           <li><span><input name="paging_page_no" value="0" maxlength="3" type="text"></span></li>' +
                '           <li class="paging_next"><a href="javascript:void(0);" class="paging_next_a" ><i class="ace-icon fa fa-caret-right bigger-140 middle"></i></a></li>' +
                '           <li class="paging_last"><a href="javascript:void(0);" class="paging_last_a" ><i class="ace-icon fa fa-step-forward middle"></i></a></li>' +
                '       </ul>' +
                '       </div>' +
                '   </div>' +
                '</div>';
            accountDiv += sysFooter;
            var matchDiv =
                '<div id="P' + accountIds[index] + '"  class="center" style="margin-right: -12px;margin-left: -12px">' +
                '<label class="btn btn-yellow btn-sm pull-left">最新更新时间:<span id="latestUpdateTime"></span></label>' +
                '<label class="btn btn-success btn-sm pull-left">连接状态:<span id="connectStatus"></span></label>' +
                '<button id="matchRightFlagBtn" style="margin-right: -320px;display: none;" onclick="_beforeMatch(this);" type="button" class="btn btn-sm btn-purple">' +
                '<i class="ace-icon fa fa-check-circle bigger-80"></i>' +
                '<span>匹配</span>' +
                '</button>' +
                '<label style="width:20%" id="matchPromptLabel">' +
                '<span id="matchPrompt" style="color: #ff3749;display: none"></span>' +
                '</label>' +
                '<button id="obtainBankFlowsBtn" onclick="_obtainBankFlows();" type="button" style="display: none;" class="btn  btn-sm btn-pink btn-round pull-right"><i class="fa fa-hand-rock-o" aria-hidden="true"></i>&nbsp;&nbsp;立即抓取流水</button>' +
                '<label style="display: none;" class="btn btn-yellow btn-sm pull-right">最新抓取时间:<span id="latestGetFlowTime"></span></label>' +
                '<span class="btn btn-success btn-sm pull-right"  id="accoutStatusInfo" style="display: none;"></span>' +
                '</div>';
            //<span id="flowConnectStatus"></span>
            accountDiv += matchDiv;
            var bankDiv = '<div id="bankFlow' + accountIds[index] + '" style="margin-right: -12px;margin-left: -12px" class="" >';
            accountDiv += bankDiv;
            var bankTable =
                '<table  class="table  table-bordered table-hover no-margin-bottom">' +
                '<thead>' +
                '<tr><th>选择</th><th>存款人</th><th>存款账号</th>' +
                '<th>存款金额</th><th>存款时间</th><th>抓取时间</th><th>摘要</th><th>备注</th><th>操作</th></tr>' +
                '</thead>' +
                '<tbody></tbody>' +
                '</table>';
            //<th>支付确认码</th>
            accountDiv += bankTable;
            var bankFooter =
                '<div id="bankFlowFooter" class="message-footer clearfix no-padding" >' +
                '<input type="hidden"  name="pagingPrevious">' +
                '<input type="hidden"  name="pagingNext">' +
                '<input type="hidden"  name="pagingLast">' +
                '<div class="pull-left">' +
                '<span class="label label-info">银行流水</span>' +
                '</div>' +
                '<div class="pull-right">' +
                '<div class="paging_from_to inline middle">第 0/ 0 页</div>' +
                '<ul class="pagination middle"><li class="paging_first ">' +
                '<a href="javascript:void(0);" class="paging_first_a">' +
                '<i class="ace-icon fa fa-step-backward middle"></i></a></li>' +
                '<li class="paging_previous ">' +
                '<a href="javascript:void(0);" class="paging_previous_a">' +
                '<i class="ace-icon fa fa-caret-left bigger-140 middle"></i></a></li>' +
                '<li><span>' +
                '<input name="paging_page_no" value="0" maxlength="3" type="text">' +
                '</span>' +
                '</li>' +
                '<li class="paging_next">' +
                '<a href="javascript:void(0);" class="paging_next_a">' +
                '<i class="ace-icon fa fa-caret-right bigger-140 middle"></i></a>' +
                '</li>' +
                '<li class="paging_last"><a href="javascript:void(0);" class="paging_last_a">' +
                '<i class="ace-icon fa fa-step-forward middle"></i></a>' +
                '</li>' +
                '</ul>' +
                '</div>' +
                '</div>' +
                '</div>';
            var promptDiv = '<div style="text-align: center"><span style="color: mediumvioletred;font-size: 15px;">提示:1.页签账号显示顺序：编号(账号)|开户人|开户行 2.流水红色表示存款时间已超过1小时</span></div></div>';
            bankFooter += promptDiv;
            accountDiv += bankFooter;
            allAccounts += accountDiv;
        }

        selectedAccountsDiv += allAccounts + '</div>';
        var ulStr =
            '<ul id="accountsNavUL" class="nav nav-tabs">' + subLi + '</ul>';
        ulStr += selectedAccountsDiv;
        $('#accountInfoDiv').append(ulStr);
        $('#accountInfoDiv').show();//整个框显示
        $('th').attr('style', 'text-align:center');
        for (var i in accountIds) {
            var latestUpdateTime = $.session.get("latestUpdateTime" + accountIds[i]);
            $('#P' + accountIds[i] + '   #connectStatus').text('正常');
            $('#P' + accountIds[i] + '   #connectStatus').attr('style', 'color:white;');
            $('#P' + accountIds[i] + '   #connectStatus').parent().attr('class', 'btn btn-success btn-sm pull-left');
            if (latestUpdateTime) {
                $('#P' + accountIds[i] + ' #latestUpdateTime').html(latestUpdateTime);
            } else {
                $('#P' + accountIds[i] + ' #latestUpdateTime').html(_latestUpdateTime(new Date().getTime()));
            }
        }
        _initialLiPage();//初始化数据
    } else {
        $('#accountInfoDiv').empty().html('<div><h3 style="text-align: center;color: #6243ff">单子在路上，请稍等...</h3></div>').show();
    }
}
/**
 * 根据账号初始化层级条件
 * @private
 */
function _initalLevelCondition() {
    var accountId = [];
    accountId.push(accountIdWhole);
    var opt = '<option>全部</option>';
    if (accountId) {
        $.ajax({
            type: 'get',
            url: '/r/account/getLevelsOrHandicapByAccountIdArray',
            data: {"accountIdArr": accountId.toString()},
            dataType: "json",
            success: function (res) {
                if (res) {
                    if (res.status == 1 && res.data) {
                        $(res.data[1]).each(function (i, val) {
                            opt += '<option value="' + val.id + '">' + val.name + '</option>';
                        });
                    }
                    $('#level' + accountId + '_chosen').prop('style', 'width:180px;');
                    $('#level' + accountId).empty().html(opt);
                    $('#level' + accountId).trigger("chosen:updated");
                }
            }
        });
    }
    $('.chosen-select').chosen({
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
}

/**
 * li 页签点击事件 刷新当前页 id是accounId
 * @private
 */
function _liOnclick(id, accountNo) {
    $('#span' + id).text('');
    accountIdWhole = id;
    accountNoWhole = accountNo;
    _datePickerForAll($('#account' + accountIdWhole).find("input.date-range-picker"));
    if (accountIdWhole) {
        _changeSearch();
    }
    _initalLevelCondition();//初始化层级条件
    _searchBtnClick();
}
/**
 * 初始化Li页签
 * @private
 */
function _initialLiPage() {
    _datePickerForAll($('#toMatch').find("input.date-range-picker"));
    var lis = $('#accountsNavUL li');
    if (accountIdWhole && oldAccountIdArray.indexOf(parseInt(accountIdWhole)) > -1) {
        var lis = $('#accountsNavUL li');
        var hasCurrentAction = false;
        for (var i = 0; i < lis.length; i++) {
            var accountId = $(lis[i]).attr('li_acccountId');
            if (accountId == accountIdWhole) {
                hasCurrentAction = true;
                $(lis[i]).prop('class', 'active');
            }
        }
        if (hasCurrentAction) {

        } else {
            $('#firstLi').prop('class', 'active');
        }
    } else {
        $('#firstLi').prop('class', 'active');
    }
    for (var i = 0; i < lis.length; i++) {
        var accountId = $(lis[i]).attr('li_acccountId');
        var accoutNo = $(lis[i]).attr('li_acccountNo');
        if (lis[i].className == 'active' && oldAccountIdArray.indexOf(parseInt(accountId)) >= 0) {
            $(lis[i]).find('span').text('');
            _liOnclick(parseInt(accountId), accoutNo);//页签点击 查询
        }
    }
    var activeLi = $('#accountsNavUL li.active');
    accountIdWhole = $(activeLi).attr('li_acccountId');
    accountNoWhole = $(activeLi).attr('li_acccountNo');
    if (accountIdWhole) {
        _changeSearch();
    }
}
/**
 * 查询按钮点击
 * @private
 */
function _searchBtnClick() {
    var searchSys = $('#searchSys' + accountIdWhole).prop('checked');
    var searchBank = $('#searchBank' + accountIdWhole).prop('checked');
    if (searchSys && !searchBank) {
        _searchBtn(accountIdWhole, 0, 0);
    }
    if (searchBank && !searchSys) {
        _searchBtn(accountIdWhole, 1, 0);
    }
    if (!searchBank && !searchSys) {
        _searchBtn(accountIdWhole, 3, 0);
    }
    if (searchBank && searchSys) {
        _searchBtn(accountIdWhole, 3, 0);
    }
}
/**
 * 绑定事件 选择查询： 查询提单 查询流水
 * @param id
 * @private
 */
function _changeSearch() {
    $('#searchSys' + accountIdWhole).unbind().bind('click', function () {//查提单 点击
        var searchSys = $('#searchSys' + accountIdWhole).prop('checked');
        var searchBank = $('#searchBank' + accountIdWhole).prop('checked');
        if (searchSys && !searchBank) {
            //只查提单
            $('#level' + accountIdWhole).removeAttr("disabled").css("background-color", "#FFF");
            //$('#member' + accountIdWhole).attr("readOnly", false);
            $('#orderNo' + accountIdWhole).attr("readOnly", false);
            //_searchBtn(accountIdWhole, 0, 0);
        }
        if (!searchSys && searchBank) {//只查询流水
            $('#level' + accountIdWhole).attr("disabled", "disabled").css("background-color", "#F5F5F5");
            //$('#member' + accountIdWhole).attr("readOnly", true);
            $('#orderNo' + accountIdWhole).attr("readOnly", true);
            //_searchBtn(accountIdWhole, 1, 0);
        }
        if ((searchSys && searchBank) || (!searchSys && !searchBank)) {//全部查询
            $('#level' + accountIdWhole).removeAttr("disabled").css("background-color", "#FFF");
            //$('#member' + accountIdWhole).attr("readOnly", false);
            $('#orderNo' + accountIdWhole).attr("readOnly", false);
            //_searchBtn(accountIdWhole, 3, 0);

        }
    });
    $('#searchBank' + accountIdWhole).unbind().bind('click', function () {//查询流水点击
        var searchSys = $('#searchSys' + accountIdWhole).prop('checked');
        var searchBank = $('#searchBank' + accountIdWhole).prop('checked');
        if (searchBank && !searchSys) {//只查询银行流水
            $('#level' + accountIdWhole).attr("disabled", "disabled").css("background-color", "#F5F5F5");
            //$('#member' + accountIdWhole).attr("readOnly", true);
            $('#orderNo' + accountIdWhole).attr("readOnly", true);
            //_searchBtn(accountIdWhole, 1, 0);
        }
        if (!searchBank && searchSys) {//只查询提单

            $('#level' + accountIdWhole).removeAttr("disabled").css("background-color", "#FFF");
            //$('#member' + accountIdWhole).attr("readOnly", false);
            $('#orderNo' + accountIdWhole).attr("readOnly", false);
            //_searchBtn(accountIdWhole, 0, 0);
        }
        if ((searchSys && searchBank) || (!searchSys && !searchBank)) {//全部查询
            $('#level' + accountIdWhole).removeAttr("disabled").css("background-color", "#FFF");
            //$('#member' + accountIdWhole).attr("readOnly", false);
            $('#orderNo' + accountIdWhole).attr("readOnly", false);
            //_searchBtn(accountIdWhole, 3, 0);

        }
    });
}
function _showMatchAndCancelRemark() {
    var opt = '';
    $(manualToMatchRemarks).each(function (i, val) {
        opt += '<option>' + val + '</option>';
    });
    $('#commonMatchRemark').empty().html(opt);
    var opt2 = '';
    $(manualToCancelRemarks).each(function (i, val) {
        opt2 += '<option>' + val + '</option>';
    });
    $('#commonCancelRemark').empty().html(opt2);
}
/**
 * 匹配按钮
 * @private
 */
function _beforeMatch(obj) {
    $('input[name="matchRemark"]').val('');
    var btnDivId = $(obj).parent().prop('id');
    var accountId = btnDivId.substring(btnDivId.indexOf('P') + 1);
    if ($('#inComeRequest' + accountId + ' tbody tr').length <= 1 &&
        $('div[id="bankFlow' + accountId + '"]  tbody tr').length <= 1) {
        $('#P' + accountId + ' #matchPrompt').text('没有能匹配的提单记录和银行流水').show().delay('slow').fadeOut();
        return false;
    }
    if ($('#inComeRequest' + accountId + ' tbody tr').length <= 1) {
        $('#P' + accountId + ' #matchPrompt').text('没有能匹配的提单记录').show().delay('slow').fadeOut();
        return false;
    }
    var sysRequestId = $('#inComeRequest' + accountId + ' tbody  input:checked').val();
    if (!sysRequestId) {
        $('#P' + accountId + ' #matchPrompt').text('请选择要匹配的提单记录').show().delay('slow').fadeOut();
        return false;
    }

    if ($('div[id="bankFlow' + accountId + '"]  tbody tr').length <= 1) {
        $('#P' + accountId + ' #matchPrompt').text('没有能匹配的银行流水').show().delay('slow').fadeOut();
        return false;
    }
    var bankFlowId = $('div[id="bankFlow' + accountId + '"] tbody  input:checked').val();
    if (!bankFlowId) {
        $('#P' + accountId + ' #matchPrompt').text('请选择要匹配的银行流水').show().delay('slow').fadeOut();
        return false;
    }

    var sys_orderNo = $('#inComeRequest' + accountId + ' tbody  input:checked').parent().parent().find('td[id="sys_orderNo"]').text();
    var sys_handicapId = $('#inComeRequest' + accountId + ' td  input[id="sys_handicap"]').val();
    var memberCode = $('#inComeRequest' + accountId + ' tbody  input:checked').parent().parent().find('input[id="memberCode"]').val();
    var memberAccount = $('#inComeRequest' + accountId + ' tbody  input:checked').parent().parent().find('input[id="memberAccount"]').val();
    $('#orderNo_match').val(sys_orderNo);//匹配订单号
    $('#handicapId_match').val(sys_handicapId);//盘口id
    $('#memberCode_match').val(memberCode);//会员编码
    $('#accountId_match').val(accountId);//入款账号id
    //提单金额
    var incomeAmount = $('#inComeRequest' + accountId + ' tbody  input:checked').parent().parent().find('td[id="amountInComeRequest"]').text();
    //流水金额
    var bankAmount = $('#bankFlow' + accountId + '  tbody  input:checked').parent().parent().find('td[id="amountBankFlowTd"]').text();
    var balanceGap = parseFloat((parseFloat(incomeAmount) - parseFloat(bankAmount))).toFixed(3);
    //提单时间
    var incomeRequestTime = $('#inComeRequest' + accountId + ' tbody  input:checked').parent().parent().find('td[id="timeInComeRequest"]').text();
    var bankLogCatchTime = $('#bankFlow' + accountId + '  tbody  input:checked').parent().parent().find('td[id="getBankLogTimeTd"]').text();
    //流水摘要
    var bankLogSummary = $('#bankFlow' + accountId + '  tbody  input:checked').parent().parent().find('td[id="bankLogSummary"]').find('a').text();
    if (!$.trim(bankLogSummary)) {
        bankLogSummary = $('#bankFlow' + accountId + '  tbody  input:checked').parent().parent().find('td[id="bankLogSummary"]').text();
    }
    //匹配订单 和流水 id
    $('#sysRequestId_match').val(sysRequestId);
    $('#bankFlowId_match').val(bankFlowId);
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
        $('#inconsistentAmountMatchInfo1').show();
        $('#inconsistentAmountMatchInfo2').show();
        $('#inconsistentAmountMatchInfo3').show();
        $('#inconsistentAmountMatchInfo4').show();
        $('#commonMatchInfo').hide();
        //(bankAmount<=100 && (balanceGap/bankAmount > sysSetting.INCOME_PERCENT))|| (bankAmount>100 && balanceGap >100)
        if (parseFloat(balanceGap) >= parseFloat(sysSetting.INCOME_BALANCE)) {
            $('#P' + accountId + ' #matchPrompt').text('差额太大请重新选择').show(10).delay(5000).fadeOut();
            return false;
        } else {
            var depositor = $('#bankFlow' + accountId + '  tbody  input:checked').parent().parent().find('td[id="toAccountOwnerTd"]').text();
            $('#inconsistentAmountMatchAmount').val(bankAmount);//流水金额
            $('#inconsistentAmountMatchName').val(depositor);//存款人姓名
            $('#inconsistentAmountMatchBankAccount').val(accountNoWhole);//公司收款账号
            $('#inconsistentAmountMatchBalanceGap').val(balanceGap);//差额
            $('#inconsistentAmountMatchMemberAccount').val(memberAccount);//会员账号
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
    var orderNo = $('#orderNo_match').val();
    var handicapId = $('#handicapId_match').val();
    var memberCode = $('#memberCode_match').val();
    var accountId = $('#accountId_match').val();

    var memberAccount = $('#inconsistentAmountMatchMemberAccount').val();
    var amount = $('#inconsistentAmountMatchAmount').val();//流水金额
    var name = $('#inconsistentAmountMatchName').val();//会员名
    var companyAccount = $('#inconsistentAmountMatchBankAccount').val();//公司收款账号
    var remark = $('#inconsistentAmountMatchRemark').val();//金额不一致匹配备注
    var depositType = $('input[name="makeUpDepositType"]:checked').val();//存款类型
    var data = {};
    var url = '/r/income/match';
    if (!companyAccount) {
        //金额一致匹配 companyAccount值不存在
        matchRemark = $('input[name="matchRemark"]').val();
        if (!matchRemark) {
            $('#remarkPrompt').empty().text('请填写备注再提交').show(100).delay(2000).hide(100);
            return false;
        }
        data = {
            "flowId": bankFlowId, "incomeReqId": sysRequestId, "remark": matchRemark,
            "orderNo": orderNo, "handicapId": handicapId, 'memberCode': memberCode, 'accountId': accountId
        };
    } else {
        if (!memberAccount) {
            $('#remarkPrompt').empty().text('请填写会员账号').show(100).delay(2000).hide(100);
            return false;
        }
        if (!remark) {
            $('#remarkPrompt').empty().text('请填写备注').show(100).delay(2000).hide(100);
            return false;
        }
        if (!depositType) {
            $('#remarkPrompt').empty().text('请选择存款类型').show(100).delay(2000).hide(100);
            return false;
        }
        data = {
            "amount": amount, "type": depositType, "name": name, "memberAccount": memberAccount, "accountId": accountIdWhole,
            "bankLogId": bankFlowId, "incomeReqId": sysRequestId, "localHostIp": localHostIp, "accountNo": companyAccount, "remark": remark
        };
        url = '/r/income/matchForInconsistentAmount';
    }
    $.ajax({
        type: 'post',
        url: url,
        data: data,
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res && res.status == 1) {
                $('#toMatchInfo').modal('hide');
            }
            _searchBtn(accountIdWhole, 3, 0);//刷新
            $('input[name="matchRemark"]').val('');
            $('#inconsistentAmountMatchAmount').val('');//流水金额
            $('#inconsistentAmountMatchName').val('');//存款人姓名
            $('#inconsistentAmountMatchBankAccount').val('');//公司收款账号
            $('#inconsistentAmountMatchRemark').val('');//金额不一致匹配备注
            $('#inconsistentAmountMatchBalanceGap').val('');//差额
            $('#inconsistentAmountMatchMemberAccount').val('');//会员账号
        }
    });
}
/**
 * 匹配操作后的提示信息
 * @param status
 * @param message
 * @param accountId
 * @private
 */
function _showMsg(status, message, accountId) {
    var msgDiv = '';
    if (status == 1) {
        msgDiv = '<span id="successMsg" style="display: none;"  class="btn-sm alert alert-block alert-success">' +
            '<strong><i class="ace-icon fa fa-check"></i>' + message + '</strong></span>';
    }
    else {
        msgDiv = '<span id="errorMsg" style="display: none;" class="btn-sm alert alert-danger"><strong>' +
            '<i class="ace-icon fa fa-times"></i>' + message + '</strong></span>';
    }
    $('#P' + accountId + ' #matchPromptLabel').html(msgDiv);
    if (status == 1)
        $('#successMsg').show(300).delay(2000).hide(300);
    else
        $('#errorMsg').show(300).delay(2000).hide(300);
}
/**
 * 正在匹配li 绑定点击事件
 */
$('#toMatchLi').unbind().bind('click', function () {
    _datePickerForAll($('#account' + accountIdWhole).find("input.date-range-picker"));
    _searchBtn(accountIdWhole, 3, 0);
});
function _searchAfterDealFlow() {
    _searchBtn(accountIdWhole, 1, 0);
}
/**
 * 查询点击事件 type=0 1 表示查询提单记录 和 银行流水 3表示全部查询
 * @private
 */
function _searchBtn(id, type, pageNo) {
    var pageNo = pageNo > 0 ? pageNo - 1 : 0;
    if (type == 111) {
        //111 表示根据选中的提单查流水
        bankFlowAjaxDataFillter.pageNo = pageNo;
        _bankFlowAjax(bankFlowAjaxDataFillter, id, type);
    }
    if (type == 100 || type == 101) {
        //100 根据异常流水(超过一定时间未匹配或者账号异常后获取的流水)查提单 101 根据正常流水查提单
        incomeRequestAjaxDataFillter.pageNo = pageNo;
        _inComeRequestAjax(incomeRequestAjaxDataFillter, id, type);
    }
    var level = '';
    if ($('#level' + id).val() && $('#level' + id).val() != '全部') {
        level = $('#level' + id).val();
    }
    var payMan = '';
    if ($('#payMan' + id).val()) {
        payMan = $('#payMan' + id).val();
    }
    var orderNo = '';
    if ($('#orderNo' + id).val()) {
        orderNo = $('#orderNo' + id).val();
    }
    var member = '';
    if ($('#member' + id).val()) {
        member = $('#member' + id).val();
    }
    var fromMoney = '';
    if ($('#fromMoney' + id).val()) {
        fromMoney = $('#fromMoney' + id).val();
    }
    var toMoney = '';
    if ($('#toMoney' + id).val()) {
        toMoney = $('#toMoney' + id).val();
    }
    var startAndEnd = $('#timeScopeToMatch' + id).val();
    var startTime = '';
    var endTime = '';
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
    type = parseInt(type);
    var inComeRequestAjaxData = {
        "pageNo": pageNo,
        "accountId": id,
        "payMan": payMan,
        "level": level,
        "orderNo": orderNo,
        "member": member,
        "fromMoney": fromMoney,
        "toMoney": toMoney,
        "startTime": startTime,
        "endTime": endTime
    };
    var bankFlowAjaxData = {
        "pageNo": pageNo,
        "accountId": id,
        "member": member,
        "payMan": payMan,
        "fromMoney": fromMoney,
        "toMoney": toMoney,
        "startTime": startTime,
        "endTime": endTime
    };
    //银行流水只查当天时间内的，不查询历史记录
    //bankFlowAjaxData.startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
    //bankFlowAjaxData.endTime =moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    if (type === 3) {
        _inComeRequestAjax(inComeRequestAjaxData, id, 0);
        _bankFlowAjax(bankFlowAjaxData, id, 1);
    }
    if (type === 0) {
        _inComeRequestAjax(inComeRequestAjaxData, id, 0);
    }
    if (type === 1) {
        _bankFlowAjax(bankFlowAjaxData, id, 1);
    }
    if (type==4){
        _getBankFlowCount(id, bankFlowAjaxData, 1);
    }
}
/**
 * 重置查询条件
 * @private
 */
function _resetValue(id) {
    $('#searchSys' + id).prop('checked', '');
    $('#searchBank' + id).prop('checked', '');
    if ($('#orderNo' + accountIdWhole).attr("readOnly")) {
        $('#orderNo' + accountIdWhole).attr("readOnly", false);
    }
    if ($('#level' + id).val()) {
        _initalLevelCondition();
    }
    if ($('#payMan' + id).val()) {
        $('#payMan' + id).val('');
    }
    if ($('#orderNo' + id).val()) {
        $('#orderNo' + id).val('');
    }
    if ($('#member' + id).val()) {
        $('#member' + id).val('');
    }
    if ($('#fromMoney' + id).val()) {
        $('#fromMoney' + id).val('');
    }
    if ($('#toMoney' + id).val()) {
        $('#toMoney' + id).val('');
    }
    if ($('#timeScopeToMatch' + id).val()) {
        _datePickerForAll($('#timeScopeToMatch' + id + "  input.date-range-picker"));
    }
    _searchBtn(accountIdWhole, 3, 0);
}

/**
 * 查询系统提单
 * @private
 */
function _inComeRequestAjax(data, id, type) {
    $.each(ContentRight['IncomeAuditComp:*'], function (name, value) {
        if (name == 'IncomeAuditComp:AuditorConceal:*') {
            concealOrderRightFlag = true;
        }
        if (name == 'IncomeAuditComp:CustomerSendMessage:*') {
            sendMessageRightFlag = true;
        }
        if (name == 'IncomeAuditComp:CustomerAddRemark:*') {
            addRemarkRightFlag = true;
        }
        if (name == 'IncomeAuditComp:CancelIncomeReq:*') {
            cancelIncomeReqFlag = true;
        }

    });
    $.ajax({
        type: 'get',
        url: '/r/income/search',
        data: data,
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var tr = '',trs='';
                    var amount = 0;
                    if (res.data.sysOrderList && res.data.sysOrderList.length > 0) {
                        $('#inComeRequest' + id).find('tbody').attr('class',"");
                        $(res.data.sysOrderList).each(function (i, val) {
                            if (id != val.toId) {
                                //红色字体显示同一层级的
                                tr += '<tr style="color: red;cursor: pointer;" onclick="_checkSysOrderTr(this);">' +
                                    '<td><input onclick="_selectInputRadioForIncome(event,this);"  click_value=""  style="width:18px;height:18px;cursor: pointer;" type="radio"  name="sysRequestId" value="' + val.id + '"/></td>' +
                                    '<td><input type="hidden" id="sys_handicap" value="' + val.handicap + '">' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                                    '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>';
                            } else {
                                //补提单背景色
                                if (_checkObj(val.remark).indexOf('补提单') > -1) {
                                    tr +=
                                        '<tr style="color:white;background-color: sandybrown;cursor: pointer;" onclick="_checkSysOrderTr(this);">' +
                                        '<td><input onclick="_selectInputRadioForIncome(event,this);" click_value="" style="width:18px;height:18px;cursor: pointer;"  type="radio"  name="sysRequestId" value="' + val.id + '"/></td>' +
                                        '<td><input type="hidden" id="sys_handicap" value="' + val.handicap + '">' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                                        '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>';
                                } else {
                                    tr +=
                                        '<tr style="cursor: pointer;" onclick="_checkSysOrderTr(this);"><td><input onclick="_selectInputRadioForIncome(event,this);" click_value="" style="width:18px;height:18px;cursor: pointer;" type="radio"  name="sysRequestId" value="' + val.id + '"/></td>' +
                                        '<td><input type="hidden" id="sys_handicap" value="' + val.handicap + '">' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                                        '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>';
                                }
                            }
                            if (_checkObj(val.member)) {
                                tr += '<td id="sys_member">' +
                                    '<input id="memberCode" type="hidden" value="' + val.memberCode + '">' +
                                    '<input id="memberAccount" type="hidden" value="' + val.member + '">' +
                                    _checkObj(val.member) +
                                    '&nbsp;&nbsp;' +
                                    '<a href="javascript:void(0);" onclick="event.stopPropagation();_beforeShowDetail(this);" style="cursor:pointer;" title="点击查看今日提单" >' +
                                    '<span class="ace-icon fa fa-plus-circle purple bigger-120"></span></a>' +
                                    '</td>';
                            } else {
                                tr += '<td></td>';
                            }
                            tr += '<td id="realNameInComeRequest">' + _checkObj(val.realName) + '</td>' +
                                '<td id="amountInComeRequest">' + _checkObj(val.amount) + '</td>' +
                                '<td id="sys_orderNo">' + _checkObj(val.orderNo) + '</td>' +
                                '<td id="timeInComeRequest">' + timeStamp2yyyyMMddHHmmss(_checkObj(val.createTime)) + '</td>';
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    if (id != val.toId) {
                                        tr +=
                                            '<td><a style="color: red;" class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.remark) + '">'
                                            + _checkObj(val.remark).substring(0, 5) + "..."
                                            + '</a></td>';
                                    } else {
                                        tr +=
                                            '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.remark) + '">'
                                            + _checkObj(val.remark).substring(0, 5) + "..."
                                            + '</a></td>';
                                    }
                                } else {
                                    if (id != val.toId) {
                                        tr +=
                                            '<td><a style="color: red;" class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.remark) + '">'
                                            + _checkObj(val.remark).substring(0, 5)
                                            + '</a></td>';
                                    } else {
                                        tr +=
                                            '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.remark) + '">'
                                            + _checkObj(val.remark).substring(0, 5)
                                            + '</a></td>';
                                    }
                                }

                            } else {
                                tr += '<td ></td>';
                            }
                            //addRemarkRightFlag && sendMessageRightFlag && concealOrderRightFlag && cancelIncomeReqFlag

                            tr += '<td id="sysOrderOperateTd" style="padding-right:0px;padding-left:0px;">';
                            if (sendMessageRightFlag) {
                                tr += '<button id="SendMessageRightBTN1"   onclick="event.stopPropagation();_beforeAddRemarkOrSendMessage(' + val.id + ',\'1\'' + ',\'sendMsg\'' + ');" class="btn btn-xs btn-white btn-success" type="button">' +
                                    '<i class="ace-icon fa fa-envelope-o bigger-120">发消息</i>' +
                                    '</button>';
                            }
                            if (addRemarkRightFlag) {
                                tr +=
                                    '<button id="addRemarkRightBTN1"  onclick="event.stopPropagation();_beforeAddRemarkOrSendMessage(' + val.id + ',\'1\'' + ',\'remark\'' + ');"  class="btn btn-xs btn-white btn-info" type="button" >' +
                                    '<i class="ace-icon fa fa-comments bigger-120">备注</i>' +
                                    '</button>';
                            }
                            if (concealOrderRightFlag) {
                                tr += '<button id="concealOrderRightBTN"   class=" btn btn-xs btn-white btn-warning" type="button" onclick="event.stopPropagation();_beforeConceal(' + val.id + ');" >' +
                                    '<i class="ace-icon fa fa-flask bigger-120">隐藏</i>' +
                                    '</button>';
                            }
                            if (cancelIncomeReqFlag) {
                                tr += '<button id="concealOrderRightBTN"   class=" btn btn-xs btn-white btn-danger" type="button" onclick="event.stopPropagation();_beforeCancel(' + val.id + ',' + val.handicap + ',\'' + val.orderNo + '\',' + val.memberCode + ');" >' +
                                    '<i class="ace-icon fa fa-remove bigger-120 red">取消</i>' +
                                    '</button>';
                            }
                            tr += '</td></tr>';
                            amount += parseFloat(_checkAmount(val.amount));
                        });
                        if(incomeCurrentPageSum){
                            trs +='<tr><td colspan="5" id="inComeRequestCurrentCount">小计：统计中...</td>' ;
                            trs+='<td bgcolor="#579EC8" id="inComeRequestTd1" style="color:white;text-align:center;width: 140px;">' + amount.toFixed(3) + '</td>' ;
                            trs+='<td colspan="10"></td></tr>';
                        }else{
                            trs +='<tr><td colspan="15" id="inComeRequestCurrentCount">小计：统计中...</td></tr>' ;
                        }
                        if(incomeAllRecordSum){
                            trs+= '<tr><td colspan="5" id="inComeRequestTotalCount">合计：统计中...</td>';
                            trs+='<td bgcolor="#D6487E" id="inComeRequestTd2" style="color:white;text-align:center;width: 140px;">统计中...</td>' ;
                            trs+= '<td colspan="10"></td></tr>';
                        }else{
                            trs+= '<tr><td colspan="15" id="inComeRequestTotalCount">合计：统计中...</td></tr>';
                        }
                        $('#inComeRequest' + id).find('tbody').empty().html(tr).append(trs);
                        $('#inComeRequest' + id).find('tbody').find('td').attr('style', 'text-align:center');
                        $('#inComeRequest' + id + '  #inComeRequestTd1').attr('style', 'color:white;text-align:center;');
                        $('#inComeRequest' + id + '  #inComeRequestTd2').attr('style', 'color:white;text-align:center;width: 140px;');
                        getCompanyInMatchingCount(data, id, type);
                        if(incomeAllRecordSum){
                            _getIncomeRequestSum(data, id);
                        }
                        _setButtonRight(id);
                        $("[data-toggle='popover']").popover();
                        if (addRemarkRightFlag && sendMessageRightFlag && concealOrderRightFlag) {
                            if (cancelIncomeReqFlag) {
                                $('#inComeRequest' + id + '  #sysOrderOperateTd').attr('style', 'width:250px;');
                            } else {
                                $('#inComeRequest' + id + '  #sysOrderOperateTd').attr('style', 'width:190px;');
                            }
                        }
                        if (addRemarkRightFlag && sendMessageRightFlag && !concealOrderRightFlag) {
                            $('#inComeRequest' + id + '  #sysOrderOperateTd').attr('style', 'width:160px;');
                        }
                        if (addRemarkRightFlag && !sendMessageRightFlag && concealOrderRightFlag) {
                            $('#inComeRequest' + id + '  #sysOrderOperateTd').attr('style', 'width:160px;');
                        }
                        if (!addRemarkRightFlag && sendMessageRightFlag && concealOrderRightFlag) {
                            $('#inComeRequest' + id + '  #sysOrderOperateTd').attr('style', 'width:160px;');
                        }
                        if (!addRemarkRightFlag && !sendMessageRightFlag && concealOrderRightFlag) {
                            $('#inComeRequest' + id + '  #sysOrderOperateTd').attr('style', 'width:80px;');
                        }
                        $('#inComeRequest' + id).find('tbody').removeAttr('class');
                    }
                    else {
                        $('#inComeRequest' + id).find('tbody').empty().html('<tr><td colspan="13"><h3 class="alert alert-success center">无记录</h3></td></tr>');
                        $('#inComeRequest' + id).find('tbody').attr('class',"alert alert-success center");
                    }
                } else {
                    $('#inComeRequest' + id).find('tbody').empty().html('<tr><td colspan="13"><h3 >无记录</h3></td></tr>');
                    $('#inComeRequest' + id).find('tbody').attr('class',"alert alert-success center");
                }
            }
        },
        complete: function (res) {
            if (res.status == 200) {

            }
        }
    });
}
function _checkAmount(amount) {
    if (amount) {
        return amount;
    } else {
        return 0;
    }
}
/**立即抓取流水事件*/
function _obtainBankFlows() {
    //抓取当前账号的流水 accountIdWhole
    $.ajax({
        type: 'get',
        url: '/r/income/activateTools',
        data: {"accountId": accountIdWhole},
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
            }
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
function _checkClickLabelCompanyIn(obj) {
    var checkedFlag = $(obj).find('input').prop('checked');
    if (checkedFlag) {
        $(obj).find('input').prop('checked', false);
    } else {
        $(obj).find('input').prop('checked', true);
    }
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
            url = '/r/income/customerSendMsg';
        }
        if (operate == 'remark') {
            //公司入款 添加备注
            data = {'id': $('#sysRequestId_customer').val(), 'remark': $('#customer_remark').val()};
            url = '/r/income/customerAddRemark';
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
            url = '/r/banklog/customerSendMsg';
        }
        if (operate == 'remark') {
            //银行流水 添加备注
            data = {'id': $('#bankFlowId_customer').val(), 'remark': $('#customer_remark').val()};
            url = '/r/banklog/customerAddRemark';
        }
        if (operate == 'deal') {
            //银行流水 已处理 手续费
            data = {
                'bankLogId': $('#bankFlowId_customer').val(),
                'remark': $.trim($('#customer_remark').val()),
                'status': eventValue
            };
            url = '/r/banklog/doDisposedFee';
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
                        _searchBtn(accountIdWhole, 0, 0);
                    }
                    if (type == 2 && (operate == 'deal' || operate == 'remark')) {
                        //刷新银行流水 1
                        _searchBtn(accountIdWhole, 1, 0);
                    }
                    if (type == 3 && (operate == 'deal' || operate == 'remark')) {
                        //刷新未认领流水
                        _searchUnMatch();
                    }
                }
            }
        }
    });
}
/**获取总金额*/
function _getIncomeRequestSum(data, id) {
    if (data) {
        $.ajax({
            type: 'get',
            url: '/r/income/getMatchingCompanyInSum',
            data: data,
            dataType: 'json',
            success: function (res) {
                if (res) {
                    if (res.status == 1) {
                        $('#inComeRequest' + id + ' #inComeRequestTd2').empty().text(parseFloat(res.data).toFixed(3));
                    }
                }
            }
        });
    }
}
/**获取总记录数*/
function getCompanyInMatchingCount(data, id, type) {
    $.ajax({
        type: 'get',
        url: '/r/income/getCompanyInMatchingCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#inComeRequest' + id + '  #inComeRequestCurrentCount').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#inComeRequest' + id + '  #inComeRequestTotalCount').empty().text('合计：' + res.page.totalElements + '条记录');
                    refreshPaging(type, id, res.page, $('div[id="inComeRequest' + id + '"]').find('div[id="sysPageFooter"]'));
                }
            }
        }
    });
}
function _beforeShowDetail(obj) {
    var member = $(obj).parent().find('input[id="memberAccount"]').val();
    $.session.set('memberAccount', member);
    _showDetailByMemebr();
}
/***
 * 根据会员号显示 当天的提单记录
 * @private
 */
function _showDetailByMemebr() {
    var member = $.session.get('memberAccount');
    if (member && accountIdWhole) {
        var CurPage = $("#showDetailOrdersByMember_footPage").find(".Current_Page").text();
        if (!CurPage) {
            CurPage = 0;
        } else {
            CurPage = CurPage - 1;
        }
        if (CurPage < 0) {
            CurPage = 0;
        }
        var data = {
            "member": member,"toid": accountIdWhole,
            "pageNo": CurPage,"pageSize": $.session.get('initPageSize')
        };
        $.ajax({
            type: 'get',
            url: '/r/fininstat/incomesearbyaccount',
            data: data,
            dataType: 'json',
            //async:false,
            success: function (res) {
                if (res) {
                    if (res.status == 1) {
                        var tr = '';
                        var trs = '';
                        if (res.data) {
                            var amount = 0;
                            $(res.data.IncomeByMemberlist).each(function (i, val) {
                                tr +=
                                    '<tr>' +
                                    '<td>' + _checkObj(val.handicapName) + '</td>' +
                                    '<td>' + _checkObj(val.levelName) + '</td>' +
                                    '<td>' + _checkObj(val.memberRealName) + '</td>' +
                                    '<td>' + _checkObj(val.amount) + '</td>' +
                                    '<td>' + _checkObj(val.toAccount).substring(0, 4) + "******" + _checkObj(val.toAccount).substring(_checkObj(val.toAccount).length - 4, _checkObj(val.toAccount).length) + '</td>' +
                                    '<td>' + _checkObj(val.toAccountBank) + '</td>' +
                                    '<td>' + _checkObj(val.orderNo) + '</td>' +
                                    '<td>' + _checkObj(timeStamp2yyyyMMddHHmmss(val.createTime)) + '</td>' +
                                    '</tr>';
                                amount += val.amount;
                            });
                            if(incomeCurrentPageSum){
                                trs +='<tr><td colspan="3">小计：' + ((res.data.IncomeByMemberPage.totalElements - res.data.IncomeByMemberPage.pageSize * res.data.IncomeByMemberPage.pageNo) >= 0 ? res.data.IncomeByMemberPage.pageSize : (res.data.IncomeByMemberPage.totalElements - res.data.IncomeByMemberPage.pageSize * (res.data.IncomeByMemberPage.pageNo - 1))) + '条记录</td>'+
                                '<td bgcolor="#579EC8" style="color: white;">' + parseFloat(amount).toFixed(3) + '</td>' +
                                '<td colspan="6"></td>' +
                                '</tr>' ;
                            }else{
                                trs +='<tr><td colspan="15">小计：' + ((res.data.IncomeByMemberPage.totalElements - res.data.IncomeByMemberPage.pageSize * res.data.IncomeByMemberPage.pageNo) >= 0 ? res.data.IncomeByMemberPage.pageSize : (res.data.IncomeByMemberPage.totalElements - res.data.IncomeByMemberPage.pageSize * (res.data.IncomeByMemberPage.pageNo - 1))) + '条记录</td></tr>';
                            }
                            if(incomeAllRecordSum){
                                trs+= '<tr><td colspan="3">合计：' + res.data.IncomeByMemberPage.totalElements + '条记录</td>' +
                                    '<td bgcolor="#D6487E"  style="color: white;">' + parseFloat(res.data.IncomeByMembertotal).toFixed(3) + '</td><td colspan="6"></td>' +
                                    '</tr>';
                            }else{
                                trs+= '<tr><td colspan="15">合计：' + res.data.IncomeByMemberPage.totalElements + '条记录</td></tr>';
                            }
                        }
                        $('#showDetailOrdersByMember_tbody').empty().html(tr).append(trs);
                        showPading(res.data.IncomeByMemberPage, 'showDetailOrdersByMember_footPage', _showDetailByMemebr);
                    }
                    $('#showDetailOrdersByMember').modal('show');
                }
            }
        });
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
/**
 * 查询银行流水
 */
function _bankFlowAjax(data, id, type) {
    //addRemarkRightFlag && sendMessageRightFlag && addOrderRightFlag && finalDealBankFlowFlag
    $.each(ContentRight['IncomeAuditComp:*'], function (name, value) {
        if (name == 'IncomeAuditComp:bankFlowFinalDeal:*') {
            finalDealBankFlowFlag = true;
        }
        if (name == 'IncomeAuditComp:CustomerSendMessage:*') {
            sendMessageRightFlag = true;
        }
        if (name == 'IncomeAuditComp:CustomerAddRemark:*') {
            addRemarkRightFlag = true;
        }
        if (name == 'IncomeAuditComp:AuditorAddOrder:*') {
            addOrderRightFlag = true;
        }

    });
    $.ajax({
        type: 'get',
        url: '/r/banklog/search',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var tr = '',trs='';
                    var amount = 0;
                    if (res.data.bankFlowList && res.data.bankFlowList.length > 0) {
                        $('div[id="bankFlow' + id + '"]').find('tbody').attr('class','');
                        $(res.data.bankFlowList).each(function (i, val) {
                            if (id != val.fromAccount) {
                                tr +=
                                    '<tr style="color: red;cursor:pointer;" onclick="_checkBankFlowTr(this)">' +
                                    '<td><input onclick="_selectInputRadioForIncome(event,this);" click_value="" style="width:18px;height:18px;cursor: pointer;" type="radio"  name="bankFlowId" value="' + val.id + '"/></td>' +
                                    '<td id="toAccountOwnerTd">' + _checkObj(val.toAccountOwner) + '</td>' +
                                    '<td>' + _checkObj(val.toAccount) + '</td>' +
                                    '<td id="amountBankFlowTd">' + _checkObj(val.amount) + '</td>' +
                                    '<td id="tradingTimeTd">' + timeStamp2yyyyMMddHHmmss(val.tradingTime) + '</td>' +
                                    '<td id="getBankLogTimeTd">' + timeStamp2yyyyMMddHHmmss(val.createTime) + '</td>'
                                ;
                            }
                            else {
                                tr +=
                                    '<tr style="cursor: pointer;" onclick="_checkBankFlowTr(this)">' +
                                    '<td><input onclick="_selectInputRadioForIncome(event,this);"  click_value="" style="width:18px;height:18px;cursor: pointer;" type="radio"  name="bankFlowId" value="' + val.id + '"/></td>' +
                                    '<td id="toAccountOwnerTd">' + _checkObj(val.toAccountOwner) + '</td>' +
                                    '<td>' + _checkObj(val.toAccount) + '</td>' +
                                    '<td id="amountBankFlowTd">' + _checkObj(val.amount) + '</td>' +
                                    '<td id="tradingTimeTd">' + timeStamp2yyyyMMddHHmmss(val.tradingTime) + '</td>' +
                                    '<td id="getBankLogTimeTd">' + timeStamp2yyyyMMddHHmmss(val.createTime) + '</td>'
                                ;
                            }
                            if (_checkObj(val.summary)) {
                                if (_checkObj(val.summary).length > 12) {
                                    if (id != val.fromAccount) {
                                        tr +=
                                            '<td id="bankLogSummary"><a style="color: red;" class="bind_hover_card breakByWord"  title="支付摘要"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.summary) + '">'
                                            + _checkObj(val.summary).substring(0, 12) + "..."
                                            + '</a></td>';
                                    } else {
                                        tr +=
                                            '<td id="bankLogSummary"><a class="bind_hover_card breakByWord"  title="支付摘要"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.summary) + '">'
                                            + _checkObj(val.summary).substring(0, 12) + "..."
                                            + '</a></td>';
                                    }
                                } else {
                                    if (id != val.fromAccount) {
                                        tr +=
                                            '<td id="bankLogSummary"><a style="color: red;" class="bind_hover_card breakByWord"  title="支付摘要"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.summary) + '">'
                                            + _checkObj(val.summary)
                                            + '</a></td>';
                                    } else {
                                        tr +=
                                            '<td id="bankLogSummary"><a class="bind_hover_card breakByWord"  title="支付摘要"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.summary) + '">'
                                            + _checkObj(val.summary)
                                            + '</a></td>';
                                    }
                                }
                            } else {
                                tr += '<td id="bankLogSummary"></td>';
                            }
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 6) {
                                    if (id != val.fromAccount) {
                                        tr +=
                                            '<td><a style="color: red" class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.remark) + '">'
                                            + _checkObj(val.remark).substring(0, 6) + "..."
                                            + '</a></td>';
                                    } else {
                                        tr +=
                                            '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.remark) + '">'
                                            + _checkObj(val.remark).substring(0, 6) + "..."
                                            + '</a></td>';
                                    }
                                } else {
                                    if (id != val.fromAccount) {
                                        tr +=
                                            '<td><a style="color: red" class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.remark) + '">'
                                            + _checkObj(val.remark).substring(0, 6)
                                            + '</a></td>';
                                    } else {
                                        tr +=
                                            '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                            + ' data-content="' + _checkObj(val.remark) + '">'
                                            + _checkObj(val.remark).substring(0, 6)
                                            + '</a></td>';
                                    }
                                }
                            } else {
                                tr += '<td></td>';
                            }
                            tr += '<td id="bankFlowOperateTd">';
                            ////addRemarkRightFlag && sendMessageRightFlag && addOrderRightFlag && finalDealBankFlowFlag
                            if (sendMessageRightFlag) {
                                tr += '<button id="SendMessageRightBTN2"   onclick="event.stopPropagation();_beforeAddRemarkOrSendMessage(' + val.id + ',\'2\'' + ',\'sendMsg\'' + ');" class="btn btn-xs btn-white btn-success" type="button">' +
                                    '<i class="ace-icon fa fa-envelope-o ">发消息</i>' +
                                    '</button>';
                            }
                            if (addRemarkRightFlag) {
                                tr += '<button id="addRemarkRightBTN2"   onclick="event.stopPropagation();_beforeAddRemarkOrSendMessage(' + val.id + ',\'2\'' + ',\'remark\'' + ');"  class="btn btn-xs btn-white btn-info" type="button" >' +
                                    '<i class="ace-icon fa fa-comments">备注</i>' +
                                    '</button>';
                            }
                            if (addOrderRightFlag) {
                                tr += '<button id="addOrderRightBTN"  onclick="event.stopPropagation();_addIncomeRequest(\'' + val.id + '\',\'' + _checkObj(val.toAccountOwner) + '\',\'' + val.amount + '\');" class="btn btn-xs btn-purple btn-white" type="button">' +
                                    '<i class="ace-icon fa fa-pencil ">补提单</i>' +
                                    '</button>';
                            }
                            if (finalDealBankFlowFlag) {
                                tr += '<button id="finalDealBTN" onclick="event.stopPropagation();_beforeAddRemarkOrSendMessage(' + val.id + ',\'2\'' + ',\'deal\'' + ');" class="btn btn-xs btn-danger btn-white" type="button">' +
                                    '<i class="ace-icon fa fa-lock ">处理</i>' +
                                    '</button>';
                            }
                            tr += '</td></tr>';
                            amount += _checkObj(val.amount);
                        });
                        if(incomeCurrentPageSum){
                            trs +='<tr><td colspan="3" id="bankFlowCurrentCount">统计中...</td>' ;
                            trs+='<td id="bankFlowTd1" bgcolor="#579EC8" style="color: white;width: 140px">' + amount.toFixed(3) + '</td>' ;
                            trs+='<td colspan="10"></td></tr>';
                        }else{
                            trs +='<tr><td colspan="15" id="bankFlowCurrentCount">统计中...</td></tr>' ;
                        }
                        if (incomeAllRecordSum){
                            trs+= '<tr><td colspan="3" id="bankFlowTotalCount" style="color: white">统计中...</td>';
                            trs +='<td bgcolor="#D6487E" id="bankFlowTd2" style="color: white;width: 140px">统计中...</td>' ;
                            trs +='<td colspan="10"></td></tr>';
                        }else{
                            trs+= '<tr><td colspan="15" id="bankFlowTotalCount" style="color: white">统计中...</td></tr>';
                        }
                        $('#bankFlow' + id).find('tbody').empty().html(tr).append(trs);
                        $('#bankFlow' + id).find('tbody').find('td').attr('style', 'text-align:center;');
                        $('#bankFlow' + id + '  #bankFlowTd1').attr('style', 'color:white;text-align:center;width: 140px;');
                        $('#bankFlow' + id + '  #bankFlowTd2').attr('style', 'color:white;text-align:center;width: 140px;');
                        _setButtonRight(id);
                        if (addRemarkRightFlag && sendMessageRightFlag && addOrderRightFlag && finalDealBankFlowFlag) {
                            $('#bankFlow' + id + '  #bankFlowOperateTd').attr('style', 'width:240px;');
                        }
                        if (addRemarkRightFlag && sendMessageRightFlag && !addOrderRightFlag && finalDealBankFlowFlag) {
                            $('#bankFlow' + id + '  #bankFlowOperateTd').attr('style', 'width:160px;');
                        }
                        if (addRemarkRightFlag && !sendMessageRightFlag && addOrderRightFlag && finalDealBankFlowFlag) {
                            $('#bankFlow' + id + '  #bankFlowOperateTd').attr('style', 'width:160px;');
                        }
                        if (!addRemarkRightFlag && sendMessageRightFlag && addOrderRightFlag && finalDealBankFlowFlag) {
                            $('#bankFlow' + id + '  #bankFlowOperateTd').attr('style', 'width:160px;');
                        }
                        if (addRemarkRightFlag && sendMessageRightFlag && addOrderRightFlag && !finalDealBankFlowFlag) {
                            $('#bankFlow' + id + '  #bankFlowOperateTd').attr('style', 'width:160px;');
                        }
                        if (!addRemarkRightFlag && !sendMessageRightFlag && addOrderRightFlag && !finalDealBankFlowFlag) {
                            $('#bankFlow' + id + '  #bankFlowOperateTd').attr('style', 'width:60px;');
                        }
                        if (!addRemarkRightFlag && !sendMessageRightFlag && !addOrderRightFlag && finalDealBankFlowFlag) {
                            $('#bankFlow' + id + '  #bankFlowOperateTd').attr('style', 'width:60px;');
                        }
                        $("[data-toggle='popover']").popover();
                        if(incomeAllRecordSum){
                            _getBankFlowSum(id, data);
                        }
                        _getBankFlowCount(id, data, type);
                    }
                    else {
                        if (type == 1) {
                            $('div[id="bankFlow' + id + '"]').find('tbody').empty().html('<tr><td colspan="10"><h3 >无流水</h3></td></tr>');
                            $('div[id="bankFlow' + id + '"]').find('tbody').attr('class','"alert alert-success center"');
                        }
                    }
                }
                else {
                    $('div[id="bankFlow' + id + '"]').find('tbody').empty().html('<tr><td colspan="10"><h3 >无流水</h3></td></tr>');
                    $('div[id="bankFlow' + id + '"]').find('tbody').attr('class','"alert alert-success center"');
                }
            }
        },
        complete: function (res) {
            if (res && res.status == 200) {

            }
            _checkFlowException();
        }
    });
}
function _checkSysOrderTr(obj) {
    var checkedFlag = $(obj).find('input').prop('checked');
    if (checkedFlag) {
        $(obj).find('input').attr('click_value', '1');
        $(obj).find('input').prop('checked', false);
    } else {
        $(obj).find('input').attr('click_value', '2');
        $(obj).find('input').prop('checked', true);
        $(obj).siblings().each(function () {
            $(this).find('input').attr('click_value', '2');
            $(this).find('input').prop('checked', '');
        });
    }
}
/**点击行选中*/
function _checkBankFlowTr(obj) {
    var click_value = $(obj).find('input').attr('click_value');
    if (click_value == '1') {
        $(obj).find('input').attr('click_value', '2');
        $(obj).find('input').prop('checked', '');
    } else {
        $(obj).find('input').attr('click_value', '1');
        $(obj).find('input').prop('checked', 'checked');
        $(obj).siblings().each(function () {
           $(this).find('input').attr('click_value', '2');
            $(this).find('input').prop('checked', '');
        });
    }
}
function _removeTrOnclick(obj) {
    $(obj).parent().parent().remove('onclick');
}
/**补提单信息*/
function _addIncomeRequest(id, payer, amount) {
    $('#bankLogIdToReBuild').val(id);
    $('#makeUpMemberAccount').val('');
    $('#makeUpName').val(payer == null ? "" : payer).prop('readonly', 'readonly');
    $('#makeUpAmount').val(amount).prop('readonly', 'readonly');
    $('#makeUpAccount').val(accountNoWhole).prop('readonly', 'readonly');
    $('#makeUpRemark').val('');
    $('#makeUpDepositType').val('');
    $('input[name="makeUpDepositType"]').prop('checked', '');
    $('#makeUpPrompt').text('');
    $('#makeUpFlow').modal('show');
}
/**确定补提单信息*/
function _confirmAddIncomeRequest() {
    var bankLogId = $('#bankLogIdToReBuild').val();
    if(!bankLogId){
        return;
    }
    if (bankLogId &&generateIncomeOrder && generateIncomeOrder == bankLogId) {
        $('#makeUpPrompt').text("20秒内不能重复补单！").show(10).delay(800).hide(10);
        return false;
    } else {
        var memberAccount = $('#makeUpMemberAccount').val();
        var makeUpName = $('#makeUpName').val();
        var makeUpAmount = $('#makeUpAmount').val();
        var makeUpAccount = $('#makeUpAccount').val();
        var makeUpRemark = $('#makeUpRemark').val();
        var makeUpDepositType = $('#makeUpDepositType').val();
        $('input[name="makeUpDepositType"]').each(function () {
            if ($(this).prop('checked')) {
                makeUpDepositType = $(this).val();
            }
        });
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
        if (!makeUpDepositType) {
            $('#makeUpPrompt').text("请选择存款类型！").show(10).delay(1000).hide(10);
            return false;
        }
        var data = {
            "memberAccount": memberAccount,"name": makeUpName, "amount": makeUpAmount,
            "accountNo": makeUpAccount,"accountId": accountIdWhole,"remark": makeUpRemark,
            "type": makeUpDepositType,"localHostIp": localHostIp, "bankLogId": bankLogId
        };
        generateIncomeOrder = bankLogId;
        $.ajax({
            type: 'post', url: '/r/income/generateIncomeRequestOrder',
            data: data,async:false,dataType: 'json',
            success: function (res) {
                if (res && res.status == 1) {
                    $('#makeUpFlow').modal('hide');
                    _searchBtn(accountIdWhole, 0, 0);
                    $('#bankLogIdToReBuild').val('');
                    setTimeout(function () {
                        generateIncomeOrder = null;
                    }, 20 * 1000);
                } else {
                    generateIncomeOrder = null;
                    $.gritter.add({
                        time: '',
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
}

/**获取总金额*/
function _getBankFlowSum(id, data) {
    if (data) {
        $.ajax({
            type: 'get',
            url: '/r/banklog/getBankFlowSum',
            data: data,
            dataType: 'json',
            success: function (res) {
                if (res && res.status == 1) {
                    $('div[id="bankFlow' + id + '"]  #bankFlowTd2').text(parseFloat(res.data).toFixed(3));
                }
            }
        });
    }
}
/**获取总记录数*/
function _getBankFlowCount(id, data, type) {
    $.ajax({
        type: 'get',
        url: '/r/banklog/getBankFlowCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.page) {
                    $('#bankFlow' + id + '  #bankFlowCurrentCount').text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#bankFlow' + id + '  #bankFlowTotalCount').text('合计：' + res.page.totalElements + '条记录');
                    refreshPaging(type, id, res.page, $('div[id="bankFlow' + id + '"]').find('div[id="bankFlowFooter"]'));
                    if(res.page&&res.page.totalElements>0){
                        $('li[li_acccountid="'+id+'"]').find('#span'+id).text(res.page.totalElements);
                    }
                }
            }
        }
    });
}
/**根据当前页签账号判断异常流水，
 * 规则：1. 1小时以后还未被匹配的流水视为异常流水 2.冻结时间比流水交易时间小则判断为异常流水*/
function _checkFlowException() {
    var tradingTimeTd = $('div[id="bankFlow' + accountIdWhole + '"]').find('tbody').find('td[id="tradingTimeTd"]');
    $(tradingTimeTd).each(function (i) {
        var timeTd = Date.parse(new Date($(this).text()));
        var timeNow = Date.parse(new Date());
        if (timeNow - timeTd >= 60 * 60 * 1000) {
            $(this).parent().attr('style', 'color:red;');
            $(this).parent().attr('exception_tr', 'yes');
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
    $('#cancel_memberCode').val($.trim(memberCode));
    $('#cancel_modal').modal('show');
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
            _executeCancelOrConceal(accountIdWhole, incomeRequestId, null, null, 'conceal', null);
        }
    });
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
    var memberCode = $('#cancel_memberCode').val();
    if (!remark) {
        $('#cancel_remarkPrompt').show(10).delay(1000).hide(10);
        return;
    }
    bootbox.confirm('是否确定取消?', function (res) {
        if (res) {
            _executeCancelOrConceal(accountIdWhole, incomeRequestId, handicap, orderNo, 'cancel', remark, memberCode);
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
function _executeCancelOrConceal(ID, id, handicapId, orderNo, type, remark, memberCode) {
    $.ajax({
        type: 'post',
        url: '/r/income/reject2Platform',
        async: false,
        data: {
            "accountId": ID,
            "incomeRequestId": id,
            "remark": remark,
            "type": type,
            "orderNo": orderNo,
            "handicapId": handicapId,
            "memberCode": memberCode
        },
        dataType: 'json',
        success: function (res) {
            if (type == 'cancel') {
                $('#cancel_modal').modal('hide');
            }
            //成功失败redis发消息,故此处不必显示消息
            // $.gritter.add({
            //     time: '',
            //     class_name: '',
            //     title: '系统消息',
            //     text: res.message,
            //     sticky: false,
            //     image: '../images/message.png'
            // });
        },
        complete: function (res) {
            if (res.status == 200) {
                _searchBtn(accountIdWhole, 0, 0);//刷新 提单记录
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


/*
 * 页脚分页
 */
function refreshPaging(type, id, paging, pageFooter) {
    pageFooter.find('input[name="pagingPrevious"]').empty().val(paging.previousPageNo);
    pageFooter.find('input[name="pagingNext"]').empty().val(paging.nextPageNo);
    pageFooter.find('input[name="pagingLast"]').empty().val(paging.totalPages);
    //obj.find('.paging_total_elements').html("总共 " + paging.totalElements + " 条");
    pageFooter.find('.paging_from_to').empty().html("第 " + (paging.pageNo) + " / " + paging.totalPages + " 页");
    pageFooter.find('input[name="paging_page_no"]').empty().val(paging.pageNo);

    if (paging.hasNext) {
        pageFooter.find('.paging_next').removeClass("disabled").find('.paging_next_a').attr('onclick', '_searchBtn(' + id + ',"' + type + '",' + pageFooter.find('input[name="pagingNext"]').val() + ');');
        pageFooter.find('.paging_last').removeClass("disabled").find('.paging_last_a').attr('onclick', '_searchBtn(' + id + ',"' + type + '",' + pageFooter.find('input[name="pagingLast"]').val() + ');');
    } else {
        pageFooter.find('.paging_next').addClass("disabled").find('.paging_next_a').attr('onclick', '');
        pageFooter.find('.paging_last').addClass("disabled").find('.paging_last_a').attr('onclick', '');
    }
    if (paging.hasPrevious) {
        pageFooter.find('.paging_previous').removeClass("disabled").find('.paging_previous_a').attr('onclick', '_searchBtn(' + id + ',"' + type + '",' + pageFooter.find('input[name="pagingPrevious"]').val() + ');');
        pageFooter.find('.paging_first').removeClass("disabled").find('.paging_first_a').attr('onclick', '_searchBtn(' + id + ',"' + type + '",' + '0);');
    } else {
        pageFooter.find('.paging_previous').addClass("disabled").find('.paging_previous_a').attr('onclick', '');
        pageFooter.find('.paging_first').addClass("disabled").find('.paging_first_a').attr('onclick', '');
    }
}
//----------------------------已匹配 已取消 未认领-------------------
$('#handicap_matched').change(function () {
    if ($('#handicap_matched').val() && $('#handicap_matched').val() != '全部') {
        _getLevel($('#handicap_matched').val(), 'matched');
    } else {
        $('#level_matched').empty().html('<option>全部</option>');
        $('#level_matched').trigger('chosen:updated');
    }
});
$('#handicap_canceled').change(function () {
    if ($('#handicap_canceled').val() && $('#handicap_canceled').val() != '全部') {
        _getLevel($('#handicap_canceled').val(), 'canceled');
    } else {
        $('#level_canceled').empty().html('<option>全部</option>');
        $('#level_canceled').trigger('chosen:updated');
    }
});
$('#handicap_unmatch').change(function () {
    if ($('#handicap_unmatch').val() && $('#handicap_unmatch').val() != '全部') {
        _getLevel($('#handicap_unmatch').val(), 'unmatch');
    } else {
        $('#level_unmatch').empty().html('<option>全部</option>');
        $('#level_unmatch').trigger('chosen:updated');
    }
});
/**
 * 初始化层级
 * @private
 */
function _getLevel(handicapId, type) {
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
// matched  canceled  unmatch
$('#level_matched').change(function () {
    if (getCookie('JUID') == 'admin') {
        _initialAdminInAccount('matched');
    } else {
        _getAccount('matched');
    }
});
$('#level_canceled').change(function () {
    if (getCookie('JUID') == 'admin') {
        _initialAdminInAccount('canceled');
    } else {
        _getAccount('canceled');
    }
});
$('#level_unmatch').change(function () {
    if (getCookie('JUID') == 'admin') {
        _initialAdminInAccount('unmatch');
    } else {
        _getAccount('unmatch');
    }
});
/**
 * 查询账号
 * @private
 */
function _getAccount(type) {
    var levelId = $('#level_' + type).val();
    var url = '';
    var data = {};
    if (levelId && levelId != '全部') {
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
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var opt = '<option>全部</option>';
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + val.id + '">' + val.account + '</option>';
                    });
                    $('#account_' + type).empty().html(opt);
                    $('#account_' + type).trigger("chosen:updated");
                }
            }
        }
    });
}
/**
 * 初始化账号 根据接单的账号初始化 非管理员的
 * @private
 */
function _initialInAccount(type) {
    var accountIds = '';
    var accounts = '';

    if (oldAccountIdArray && oldAccountIdArray.length > 0) {
        accountIds = oldAccountIdArray;
    }
    if (oldAccountsArray && oldAccountsArray.length > 0) {
        accounts = oldAccountsArray;
    }
    var opt = '<option>全部</option>';
    var opt1 = '<option>全部</option>';
    var opt2 = '<option>全部</option>';
    if (accountIds && accounts) {
        for (var i in accountIds) {
            opt += '<option value="' + accountIds[i] + '">' + accounts[i] + '</option>';
        }
    }
    $('#account_' + type).empty().html(opt);
    $('#account_' + type).trigger("chosen:updated");
    $('#level_' + type).empty().html(opt1);
    $('#level_' + type).trigger('chosen:updated');
    $('#handicap_' + type).empty().html(opt2);
    $('#handicap_' + type).trigger("chosen:updated");
    //根据账号 初始化层级 盘口
    $.ajax({
        type: 'get',
        url: '/r/account/getLevelsOrHandicapByAccountIdArray',
        data: {'accountIdArr': accountIds.toString()},
        //async:false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data && res.data.length > 0) {

                    for (var index in res.data[1]) {
                        opt1 += '<option value="' + res.data[1][index].id + '">' + res.data[1][index].name + '</option>';
                    }
                    for (var index in res.data[0]) {
                        opt2 += '<option value="' + res.data[0][index].id + '">' + res.data[0][index].name + '</option>';
                    }
                    $('#level_' + type).empty().html(opt1);
                    $('#level_' + type).trigger('chosen:updated');
                    $('#handicap_' + type).empty().html(opt2);
                    $('#handicap_' + type).trigger("chosen:updated");
                }
            }
        }
    });

}
/**
 * 点击页签 初始化盘口 层级--根据用户id查询该用户拥有的盘口和层级权限  管理员的
 * @private
 */
function _initialAdminHandicapAndLevel(type) {
    var userId = getCookie('JUSERID');
    $.ajax({
        type: 'get',
        url: '/r/permission/getPermission',
        data: {"userId": userId},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    //盘口
                    if (res.data.bizHandicapList && res.data.bizHandicapList.length > 0) {
                        var opt = '<option>全部</option>';
                        $(res.data.bizHandicapList).each(function (i, val) {
                            opt += '<option  value="' + val.id + '">' + val.name + '</option>';
                        });

                        $('#handicap_' + type).empty().html(opt);
                        $('#handicap_' + type).trigger("chosen:updated");
                    } else if (res.data.handicapList && res.data.handicapList.length > 0) {
                        var opt = '<option>全部</option>';
                        $(res.data.handicapList).each(function (i, val) {
                            opt += '<option  value="' + val.id + '">' + val.name + '</option>';
                        });
                        $('#handicap_' + type).empty().html(opt);
                        $('#handicap_' + type).trigger("chosen:updated");
                    }
                    //层级
                    if (res.data.bizLevelList && res.data.bizLevelList.length > 0) {
                        var opt = '<option>全部</option>';
                        $(res.data.bizLevelList).each(function (i, val) {
                            opt += '<option  value="' + val.id + '">(' + val.parent + ')' + _checkObj(val.name) + '</option>';
                        });
                        $('#level_' + type).empty().html(opt);
                        $('#level_' + type).trigger("chosen:updated");

                    } else if (res.data.levelListByHandicap && res.data.levelListByHandicap.length > 0) {
                        var opt = '<option>全部</option>';
                        $(res.data.levelListByHandicap).each(function (i, val) {
                            opt += '<option  value="' + val.id + '">(' + val.parent + ')' + _checkObj(val.name) + '</option>';
                        });
                        $('#level_' + type).empty().html(opt);
                        $('#level_' + type).trigger("chosen:updated");
                    }
                    _initialAdminInAccount(type);
                }
            }
        }
    });
}
/**
 * 初始化收款账号 管理员的
 * @private
 */
function _initialAdminInAccount(type) {
    var levelId = $('#level_' + type).val();
    var url = '';
    var data = {};
    if (levelId && levelId != '全部') {
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
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var opt = '<option>全部</option>';
                    $(res.data).each(function (i, val) {
                        if (val.type == 1) {
                            //公司入款银行卡
                            opt += '<option value="' + val.id + '">' + val.account + '</option>';
                        }
                    });
                    $('#account_' + type).empty().html(opt);
                    $('#account_' + type).trigger("chosen:updated");
                }
            }
        }
    });
}
/**
 * 已匹配  根据接单的账号id来查询
 * @private
 */
function _searchCompanyIn() {
    var type = searchCompanyInType;
    var accountIds = '';
    if (oldAccountIdArray && oldAccountIdArray.length > 0) {
        accountIds = oldAccountIdArray.toString();
    }
    var level = '';
    if ($('#level_' + type).val() != '全部') {
        level = $('#level_' + type).val();
    }
    var handicap = '';
    if ($('#handicap_' + type).val() != '全部') {
        handicap = $('#handicap_' + type).val();
    }else{
        var handicapArray = [];
        $('#handicap_' + type).find('option').each(function () {
            if(this.value!='全部'){
                handicapArray.push(this.value);
            }
        });
        handicap = handicapArray.length>0?handicapArray.toString:'';
    }
    var member = '';
    if ($('#member_' + type).val()) {
        member = $('#member_' + type).val();
    }
    var orderNo = '';
    if ($('#orderNo_' + type).val()) {
        orderNo = $('#orderNo_' + type).val();
    }
    var fromMoney = '';
    if ($('#fromMoney_' + type).val()) {
        fromMoney = $('#fromMoney_' + type).val();
    }
    var toMoney = '';
    if ($('#toMoney_' + type).val()) {
        toMoney = $('#toMoney_' + type).val();
    }
    var startAndEnd = $('#timeScope_' + type).val();
    var startTime = null;
    var endTime = null;
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = $.trim(startAndEnd[0]);
            endTime = $.trim(startAndEnd[1]);
        }
    } else {
        startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
        endTime = moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    }
    var account = '';
    if ($('#account_' + type).val() != '全部') {
        account = $('#account_' + type).val();
    }
    var CurPage = $("#" + searchCompanyInType + "_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var data = {
        'accountIds': accountIds, 'handicap': handicap,  'level': level, 'account': account,
        'member': member,  'orderNo': orderNo, "fromMoney": fromMoney,  "toMoney": toMoney,
        'startTime': startTime, 'endTime': endTime, "pageNo": CurPage, "type": searchCompanyInType,
        "pageSize": $.session.get('initPageSize')
    };
    $.each(ContentRight['IncomeAuditComp:*'], function (name, value) {
        if (name == 'IncomeAuditComp:lookUpMatchedTotalAmount:*') {
            lookUpMatchedTotalAmount = true;
        }
    });
    $.ajax({
        type: 'get',
        url: '/r/income/matchedOrCanceled',
        data: data,
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    var tr = '',trs = '',infoArray = [], accountIdList = [];
                    if (res.data && res.data.list && res.data.list.length > 0) {
                        var amount = 0,balanceGap = 0;
                        if (type == 'matched') {
                            $(res.data.list).each(function (i, val) {
                                accountIdList.push({'id': val.toAccountId}); infoArray.push({"reqId": val.id});
                                tr +=
                                    '<tr>' +
                                    '<td >' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                                    '<td >' + _showLevelNameByIdOrCode(val.level) + '</td>' +
                                    '<td >' + _checkObj(val.member) + '</td>' +
                                    '<td >' +
                                    "<a class='bind_hover_card breakByWord' data-html='true' title='账号信息' data-toggle='accountInfoHover" + val.toAccountId + "' data-placement='right' data-trigger='hover'  >"
                                    + _checkObj(val.toAccountNo).substring(0, 4) + "***" + _checkObj(val.toAccountNo).substring(_checkObj(val.toAccountNo).length - 4, _checkObj(val.toAccountNo).length) +
                                    "</a>" +
                                    '</td>' +
                                    '<td >' + _checkObj(val.amount) + '</td>' +
                                    // '<td >' + val.balanceGap + '</td>' +
                                    '<td >' +
                                    '<a class="bind_hover_card breakByWord" data-html="true" data-trigger="hover" ' +
                                    ' data-toggle="incomeRequestOrderNoHover_' + val.id + '" data-placement="left" data-original-title="匹配详情" title="">' + _checkObj(val.orderNo) + '</a>' +
                                    '</td>';
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
                                tr +=
                                    '<td>' + _checkObj(val.operator) + '</td>' +
                                    '<td >' + _checkObj(val.timeConsume) + '</td>' +
                                    '</tr>';
                                amount += _checkObj(val.amount);
                                balanceGap += _checkObj(val.balanceGap) == "" ? 0 : parseFloat(_checkObj(val.balanceGap));

                            });
                            if(incomeCurrentPageSum){
                                trs +='<tr><td colspan="4" id="matchedCompanyInCurrentCount">小计：统计中..</td>' ;
                                trs+= '<td bgcolor="#579EC8" id="matched_tbodyTdamount" style="color: white;width:130px;text-align: left">' + amount.toFixed(3) + '</td>' ;
                                trs+= '<td colspan="10"></td></tr>';
                            }else{
                                trs +='<tr><td colspan="15" id="matchedCompanyInCurrentCount">小计：统计中..</td></tr>';
                            }
                            if(incomeAllRecordSum){
                                trs+= '<tr><td colspan="4" id="matchedCompanyInAllCount">合计：统计中..</td>' ;
                                trs+='<td bgcolor="#D6487E" id="matched_tbodyTdsumAmount" style="color: white;width:130px;text-align: left;display: none;">统计中..</td>' ;
                                trs+= '<td colspan="10"></td></tr>';
                            }else{
                                trs+= '<tr><td colspan="15" id="matchedCompanyInAllCount">合计：统计中..</td></tr>' ;
                            }
                        }

                        if (type == 'canceled') {
                            $(res.data.list).each(function (i, val) {
                                accountIdList.push({'id': val.toAccountId});
                                tr +=
                                    '<tr>' +
                                    '<td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                                    '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>' +
                                    '<td>' + _checkObj(val.member) + '</td>' +
                                    '<td>' + _checkObj(val.orderNo) + '</td>' +
                                    '<td>' + _checkObj(val.payer) + '</td>' +
                                    '<td>' + _checkObj(val.amount) + '</td>' +
                                    '<td >' +
                                    "<a class='bind_hover_card breakByWord' data-html='true' title='账号信息' data-toggle='accountInfoHover" + val.toAccountId + "' data-placement='left' data-trigger='hover'  >"
                                    + _checkObj(val.toAccountNo).substring(0, 4) + "***" + _checkObj(val.toAccountNo).substring(_checkObj(val.toAccountNo).length - 4, _checkObj(val.toAccountNo).length) +
                                    "</a>" +
                                    '</td>' +
                                    '<td>' + timeStamp2yyyyMMddHHmmss(val.orderTime) + '</td>';

                                if (_checkObj(val.remark)) {
                                    tr +=
                                        '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.remark) + '">'
                                        + _checkObj(val.remark).substring(0, 5) + "..."
                                        + '</a></td></tr>';
                                } else {
                                    tr += '<td>' + _checkObj(val.remark) + '</td></tr>';

                                }
                                amount += val.amount;
                            });
                            if(incomeCurrentPageSum){
                                trs ='<tr><td colspan="5" id="canceledCompanyInCurrentCount">小计：统计中..</td>';
                                trs+='<td bgcolor="#579EC8" id="canceled_tbodyTd2" style="color: white;width: 130px">' + amount.toFixed(3) + '</td>' ;
                                trs+= '<td colspan="10"></td></tr>' ;
                            }else{
                                trs ='<tr><td colspan="15" id="canceledCompanyInCurrentCount">小计：统计中..</td></tr>';
                            }
                            if(incomeAllRecordSum){
                                trs+='<tr><td colspan="5" id="canceledCompanyInAllCount">合计：统计中..</td>' ;
                                trs+='<td bgcolor="#D6487E" id="canceled_tbodyTd1" style="color: white;width: 130px;">统计中..</td>' ;
                                trs+='<td colspan="10"></td></tr>';
                            }else{
                                trs+='<tr><td colspan="15" id="canceledCompanyInAllCount">合计：统计中..</td></tr>' ;
                            }
                            $('#canceled_tbodyTd1').attr('style', 'color:white;text-align:center;');
                            $('#canceled_tbodyTd2').attr('style', 'color:white;text-align:center;');
                        }
                        if(incomeAllRecordSum){
                            _getCompanyInSum(data, type);
                        }
                        _getCompanyInCount(data, type);
                    }
                    $('#' + type + '_tbody').empty().html(tr).append(trs);
                    $("[data-toggle='popover']").popover();
                    loadHover_accountInfoHover(accountIdList);
                    if (type == 'matched') {
                        loadHover_IncomeRequestByHoverOrderNo(infoArray);
                        $('#matchedCompanyInCurrentCount').attr('style', 'text-align:left');
                        $('#matchedCompanyInAllCount').attr('style', 'text-align:left');
                        $('#matched_tbodyTdamount').attr('style', 'color:white;text-align:left;width:130px;');
                        $('#matched_tbodyTdbalanceGap').attr('style', 'color:white;text-align:left;width:130px;');
                        $('#matched_tbodyTdsumAmount').attr('style', 'color:white;text-align:left;width:130px;');
                        $('#matched_tbodyTdsumBalanceGap').attr('style', 'color:white;text-align:left;width:130px;');
                    }
                    showPading(res.page, type + '_footPage', _searchCompanyIn);
                }
            }
        }
    });
}
function _getCompanyInSum(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/income/getCompanyInSum',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    if (type == 'matched') {
                        if (lookUpMatchedTotalAmount) {
                            $('#matched_tbodyTdsumAmount').text(parseFloat(res.data.sumAmount).toFixed(3));
                        } else {
                            $('#matched_tbodyTdsumAmount').text('无权限查看');
                        }
                        $('#matched_tbodyTdsumAmount').show();
                    }
                    if (type == 'canceled') {
                        $('#canceled_tbodyTd1').text(parseFloat(res.data.sumAmount).toFixed(3));
                    }
                }
            }
        }
    });
}
function _getCompanyInCount(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/income/getCompanyInCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.page) {
                    if (type == 'matched') {
                        $('#matchedCompanyInCurrentCount').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                        $('#matchedCompanyInAllCount').empty().text('合计：' + res.page.totalElements + '条记录');
                    }
                    if (type == 'canceled') {
                        $('#canceledCompanyInCurrentCount').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                        $('#canceledCompanyInAllCount').empty().text('合计：' + res.page.totalElements + '条记录');
                    }
                    showPading(res.page, type + '_footPage', _searchCompanyIn);
                }
            }
        }
    });
}

/**
 * 重置查询条件
 * @param type
 * @private
 */
function _resetValueS(type) {
    initPaging($('#' + type + '_footPage'), pageInitial);
    if ($('#level_' + type).val() || $('#handicap_' + type).val()) {
        _initialInAccount(type);
    }
    if ($('#account_' + type)) {
        _initialInAccount(type);
    }
    if ($('#member_' + type).val()) {
        $('#member_' + type).val('');
    }
    if ($('#orderNo_' + type).val()) {
        $('#orderNo_' + type).val('');
    }
    if (type == 'matched' && $('#account_matched').val()) {
        _initialInAccount('matched');
    }
    if ($('#fromMoney_' + type).val()) {
        $('#fromMoney_' + type).val('');
    }
    if ($('#toMoney_' + type).val()) {
        $('#toMoney_' + type).val('');
    }
    if (type != 'unmatch') {
        _datePickerForAll($("#" + type).find('input.date-range-picker'));
    } else {
        _datePickerForUnmatch($("#" + type).find('input.date-range-picker'));
    }

    if (type == 'unmatch') {
        if ($('#pay_unmatch').val()) {
            $('#pay_unmatch').val('');
        }
    }
    if (type == 'unmatch') {
        _searchUnMatch();
    } else {
        _searchCompanyIn();
    }
}

/**
 * 未认领
 * @param pageNo
 * @param type
 * @private
 */
function _searchUnMatch() {
    var accountIds = '';
    if (getCookie('JUID') == 'admin') {
        _initialAdminHandicapAndLevel('unmatch');
    } else {
        accountIds = oldAccountIdArray.toString();
    }
    var pay_unmatch = '';
    if ($('#pay_unmatch').val()) {
        pay_unmatch = $('#pay_unmatch').val();
    }
    var startAndEnd = $('#timeScope_unmatch').val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = $.trim(startAndEnd[0]);
            endTime = $.trim(startAndEnd[1]);
        }
    } else {
        startTime = $.trim(moment().subtract(2, 'days').format("YYYY-MM-DD HH:mm:ss"));
        endTime = $.trim(moment().subtract(1, 'days').format("YYYY-MM-DD HH:mm:ss"));
    }
    var account = '';
    if ($('#account_unmatch').val() != '全部') {
        account = $('#account_unmatch').val();
    }
    var fromMoney_unmatch = '';
    if ($('#fromMoney_unmatch').val()) {
        fromMoney_unmatch = $('#fromMoney_unmatch').val();
    }
    var toMoney_unmatch = '';
    if ($('#toMoney_unmatch').val()) {
        toMoney_unmatch = $('#toMoney_unmatch').val();
    }
    var CurPage = $("#unmatch_pageFoot").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var DealFlag = false;
    var CommentUnmatch = false;
    $.each(ContentRight['IncomeAuditComp:*'], function (name, value) {
        if (name == 'IncomeAuditComp:bankFlowFinalDeal:*') {
            DealFlag = true;
        }
        if (name == 'IncomeAuditComp:CustomerAddRemark:*') {
            CommentUnmatch = true;
        }
    });
    $.ajax({
        type: 'get',
        url: '/r/banklog/unmatch',
        data: {
            "accountIds": accountIds, "payer": $.trim(pay_unmatch),
            "account": $.trim(account), "fromMoney": $.trim(fromMoney_unmatch), "toMoney": $.trim(toMoney_unmatch),
            "startTime": startTime, "endTime": endTime, "pageNo": CurPage, "pageSize": $.session.get('initPageSize')
        },
        dataType: 'json',
        success: function (res) {
            if (res) {
                var tr = '',trs = '', fromAccountIdList = [];
                if (res.status == 1) {
                    if (res.data && res.data.length > 0) {
                        var amount = 0;
                        $(res.data).each(function (i, val) {
                            tr +=
                                '<tr><td>' +
                                "<a class='bind_hover_card breakByWord' data-html='true' title='账号信息' data-toggle='accountInfoHover" + val.fromAccountId + "' data-placement='right' data-trigger='hover'  >"
                                + _checkObj(val.fromAccount).substring(0, 4) + "***" + _checkObj(val.fromAccount).substring(_checkObj(val.fromAccount).length - 4, _checkObj(val.fromAccount).length) +
                                "</a>" +
                                '</td>' +
                                '<td>' + _checkObj(val.toAcount) + '</td>' +
                                '<td>' + _checkObj(val.payer) + '</td>' +
                                '<td>' + _checkObj(val.amount) + '</td>' +
                                '<td>' + _checkObj(timeStamp2yyyyMMddHHmmss(val.tradeTime)) + '</td>' +
                                '<td>' + _checkObj(timeStamp2yyyyMMddHHmmss(val.createTime)) + '</td>';
                            if (_checkObj(val.summary)) {
                                if (_checkObj(val.summary).length > 10) {
                                    tr +=
                                        '<td><a class="bind_hover_card breakByWord"  title="支付摘要"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.summary) + '">'
                                        + _checkObj($.trim(val.summary)).substring(0, 10) + "..."
                                        + '</a></td>';
                                } else {
                                    tr +=
                                        '<td><a class="bind_hover_card breakByWord"  title="支付摘要"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.summary) + '">'
                                        + _checkObj($.trim(val.summary))
                                        + '</a></td>';
                                }
                            } else {
                                tr += '<td></td>';
                            }
                            if (_checkObj(val.remarK)) {
                                if (_checkObj(val.remark).length > 10) {
                                    tr +=
                                        '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.remarK) + '">'
                                        + _checkObj(val.remarK).substring(0, 10) + "..."
                                        + '</a></td>';
                                } else {
                                    tr +=
                                        '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + _checkObj(val.remarK) + '">'
                                        + _checkObj(val.remarK).substring(0, 10)
                                        + '</a></td>';
                                }
                            } else {
                                tr += '<td></td>';
                            }
                            tr += '<td>';
                            if (DealFlag) {
                                tr += '<button id="finalDealBTNBank"  onclick="event.stopPropagation();_beforeAddRemarkOrSendMessage(' + val.id + ',\'3\'' + ',\'deal\'' + ');" class="btn btn-xs btn-warning btn-white" type="button">' +
                                    '<i class="ace-icon fa fa-lock  ">处理</i>' +
                                    '</button>';
                            }
                            if (CommentUnmatch) {
                                tr += '<button  onclick="event.stopPropagation();_beforeAddRemarkOrSendMessage(' + val.id + ',\'3\'' + ',\'remark\'' + ');"  class="btn btn-xs btn-white btn-info" type="button" >' +
                                    '<i class="ace-icon fa fa-comments">备注</i>' +
                                    '</button>';
                            }
                            tr += '</td></tr>'; amount += val.amount; fromAccountIdList.push({'id': val.fromAccountId});
                        });
                        if(incomeCurrentPageSum){
                            trs +='<tr><td colspan="3">小计：' + ((res.page.totalElements - res.page.pageSize * res.page.pageNo) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1))) + '条记录</td>' ;
                            trs += '<td bgcolor="#579EC8" id="unmatch_tbodyTd2" style="color: white;">' + amount.toFixed(3) + '</td>' ;
                            trs += '<td colspan="10"></td></tr>' ;
                        }else{
                            trs +='<tr><td colspan="15">小计：' + ((res.page.totalElements - res.page.pageSize * res.page.pageNo) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1))) + '条记录</td></tr>' ;
                        }
                        if(incomeAllRecordSum){
                            trs += '<tr><td colspan="3">合计：' + res.page.totalElements + '条记录</td>';
                            trs +='<td bgcolor="#D6487E" id="unmatch_tbodyTd1" style="color: white;">' + parseFloat(res.data[0].sumAmount).toFixed(3) + '</td>' ;
                            trs += '<td colspan="10"></td></tr>' ;
                        }else{
                            trs += '<tr><td colspan="15">合计：' + res.page.totalElements + '条记录</td></tr>';
                        }
                        $('td').attr('style', 'text-align:center');
                        $('#unmatch_tbodyTd1').attr('style', 'color:white;text-align:center;');
                        $('#unmatch_tbodyTd2').attr('style', 'color:white;text-align:center;');
                    }
                }
                $('#unmatch_tbody').empty().html(tr).append(trs);
                showPading(res.page, 'unmatch_pageFoot', _searchUnMatch);
                contentRight();
                $("[data-toggle='popover']").popover();
                //加载账号悬浮提示
                loadHover_accountInfoHover(fromAccountIdList);
            }
        }
    });
}
/**
 * 未认领恢复匹配
 * @param pageNO
 * @param type
 * @private
 */
function _unMatchToMatch(id) {
    $.ajax({
        type: 'post',
        url: '/r/banklog/recover',
        data: {"id": id},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: '',
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                _searchUnMatch(0, 'unmatch');
                if (res.status == 1 && res.data) {
                    if (res.data.fromAccount == accountIdWhole) {
                        _searchBtn(accountIdWhole, 1, 0);
                    }
                }
            }
        }
    });
}
//----------------------------已匹配 已取消 未认领 end-------------------
/**
 * 时间处理
 */
function _latestUpdateTime(time) {
    var datetime = new Date();
    datetime.setTime(time);
    var month = datetime.getMonth() + 1;
    if (month >= 0 && month < 10) {
        month = '0' + month;
    }
    var date = datetime.getDate();
    if (date >= 0 && date < 10) {
        date = '0' + date;
    }
    var hour = datetime.getHours();
    if (hour && hour >= 0 && hour <= 9) {
        hour = '0' + hour;
    }
    var minute = datetime.getMinutes();
    if (minute && minute > 0 && minute <= 9) {
        minute = '0' + minute;
    }
    if (minute && minute == 0) {
        minute = '00';
    }
    var second = datetime.getSeconds();
    if (second && second >= 0 && second <= 9) {
        second = '0' + second;
    }
    return hour + ":" + minute + ":" + second;
}
/**
 * 取消选择的账号
 * @private
 */
function _notSelectAcount() {
    $('#acccountModal').modal('hide');
}
/*
 * 页面初始加载数据
 */
function _initialMultiple() {
    $('#form-field-select-handicap').chosen(
        {
            allow_single_deselect: true,
            search_contains: true,
            no_results_text: "没有找到"
        });
    $('#form-field-select-level').chosen(
        {
            allow_single_deselect: true,
            search_contains: true,
            no_results_text: "没有找到"
        });
    $('#form-field-select-account').chosen(
        {
            allow_single_deselect: true,
            search_contains: true,
            no_results_text: "没有找到"
        }
    );
}

function _initailSelectChosen(type) {
    searchCompanyInType = type;
    //可查询的单选框
    $('.chosen-select').chosen({
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    if (type != 'unmatch') {
        _datePickerForAll($('#' + type).find("input.date-range-picker"));
    } else {
        _datePickerForUnmatch($('#' + type).find("input.date-range-picker"));
    }

    _initialInAccount(type);
    $('#account_' + type + '_chosen').prop('style', 'width:158px;');
    $('#handicap_' + type + '_chosen').prop('style', 'width:180px;');
    $('#level_' + type + '_chosen').prop('style', 'width:180px;');
    if (getCookie('JUID') == 'admin') {
        _initialAdminHandicapAndLevel(type);
    }
}
function _showDefaultForUnmatch(timeInputObj, start, end) {
    if (start && end) {
        $(timeInputObj).val(start.format('YYYY-MM-DD HH:mm:ss') + '~' + end.format('YYYY-MM-DD HH:mm:ss'));
        if (!$(timeInputObj).val()) {
            $(timeInputObj).prop('placeholder', start.format('YYYY-MM-DD HH:mm:ss') + '~' + end.format('YYYY-MM-DD HH:mm:ss'));
        }
    }
}
//时间控件
function _datePickerForUnmatch(timeInputObj) {
    var startTime = moment().subtract(2, 'days');
    var endTime = moment().subtract(1, 'days');
    $('input.date-range-picker').daterangepicker({
        timePicker: true,
        timePickerIncrement: 1,
        timePicker24Hour: true,
        autoUpdateInput: true,
        timePickerSeconds: true,
        startDate: startTime, //设置开始日期
        endDate: endTime, //设置开始日期
        ranges: {
            '最近1小时': [moment().subtract(1, 'hours'), moment()],
            '今日': [moment().startOf('day'), moment()],
            '昨日': [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')],
            '最近7日': [moment().subtract(6, 'days'), moment()]
        },
        opens: 'left', //日期选择框的弹出位置
        locale: {
            "format": "YYYY-MM-DD HH:mm:ss",
            "separator": " ~ ",
            "applyLabel": "确定",
            "cancelLabel": "取消",
            "fromLabel": "从",
            "toLabel": "到",
            "customRangeLabel": "自定义",
            "dayNames": [
                "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
            ],
            "daysOfWeek": [
                "日", "一", "二", "三", "四", "五", "六"
            ],
            "monthNames": [
                "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"
            ],
            "firstDay": 1
        }
    }, _showDefaultForUnmatch(timeInputObj, startTime, endTime)).on('apply.daterangepicker', function (ev, picker) {
        $(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' ~ ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
    }).on('cancel.daterangepicker', function (ev, picker) {
        $(this).val('');
    });
}
function _monitorBankFlowAccount() {
    if (oldAccountIdArray) {
        SysEvent.on(SysEvent.EVENT_OFFLINE, function (data) {
            if (data) {
                var detail = JSON.parse(data);
                var accountId = detail.id;
                var status = '';
                var classStr = "btn btn-success btn-sm pull-right";
                switch (detail.runningStatus) {
                    case monitorAccountStatusUnstart:
                        status = '未开始';
                        classStr = "btn btn-grey btn-sm pull-right";
                        break;
                    case monitorAccountStatusAcquisition:
                        status = '抓取中';
                        classStr = "btn btn-success btn-sm pull-right";
                        break;
                    case monitorAccountStatusPause:
                        status = '暂停';
                        classStr = "btn btn-info btn-sm pull-right";
                        break;
                    case monitorAccountStatusWarn :
                        status = '失败';
                        classStr = "btn btn-warning btn-sm pull-right";
                        break;
                    default:
                        status = '';
                        break;
                }
                var msg = '';
                if (status) {
                    msg += "状态:" + status;
                }
                if (detail.lastTime) {
                    if (status)
                        msg += "|最后抓取时间:" + timeStamp2yyyyMMddHHmmss(detail.lastTime);
                    else
                        msg += "最后抓取时间:" + timeStamp2yyyyMMddHHmmss(detail.lastTime);
                }
                if (detail.ip) {
                    if (status || detail.lastTime)
                        msg += "|主机:" + detail.ip;
                    else {
                        msg += "主机:" + detail.ip;
                    }
                }
                if (msg) {
                    for (var i in oldAccountIdArray) {
                        if (oldAccountIdArray[i] == accountId) {
                            $('#P' + accountId + '    #accoutStatusInfo').attr('class', classStr);
                            $('#P' + accountId + '    #accoutStatusInfo').empty().html('工具' + msg);
                            $('#P' + accountId + '    #accoutStatusInfo').show();
                        }
                    }
                }
            }

        }, oldAccountIdArray);
    }
}
_initialMultiple();
_initialPage();
_showMatchAndCancelRemark();