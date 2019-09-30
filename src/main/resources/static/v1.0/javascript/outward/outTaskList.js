/**如果获取不到出款请求则定时n秒后自动再去获取直到获取成功*/
currentPageLocation = window.location.href;
var toOutTaskPage = false;
var currentOutTaskPage = '';//当前tab页签是否是出款任务页签
var haveReceivedTaskData = false;
var pauseTaskFlag = '';
var stopTaskFlag = '';
var playVideoForTask = false;
var playingVideoForTask = false;
var accountIdToFinishTaskLastTime = null;
var outTaskSearchType = null;
var timeIntervalTask = null;
var selectThirdAccountFlag = false;
var selectedCurrentPageThirdAccountArray = [];
var selectedCurrentPageAccountMap = new Map();//动态保存每次选中的账号id map(currentPage,accoutIdArray);
var thirdAccountOutRemarksInfo = [];
var thirdAccountOutAmount = 0;
var addOtherThirdInfoFlag = false;
var mapForSavePageHistorySelectedArray = [];
var currentPageForThirdAccountModal = 1;
var currentPageForThirdAccountData = [];//当前页的数据
var selectedPageNoArray = [];//保存的所有页面标志
var lookUpFinishedAmount = false;
$.each(ContentRight['Outward:*'], function (name, value) {
    if (name == 'Outward:currentpageSum:*') {
        outwardCurrentPageSum = true;
    }
    if (name == 'Outward:allRecordSum:*') {
        outwardAllRecordSum = true;
    }
});

function _getTaskPageAhref(obj) {
    currentOutTaskPage = $(obj).find('a').attr('href');
    if (timeIntervalTask) {
        clearTimeout(timeIntervalTask);
        timeIntervalTask = null;
    }
    if (currentOutTaskPage != '#toOut') {
        $('#changBtnDivOutTask').attr('style', 'display:none');
    } else {
        $('#changBtnDivOutTask').attr('style', 'display:block');
    }
    _continueGetTaskInterval();
}

window.onbeforeunload = function () {
    if (timeIntervalTask) {
        clearTimeout(timeIntervalTask);
        timeIntervalTask = null;
    }
};

function _continueGetTaskInterval() {
    //_checkedThird();
    if (!haveReceivedTaskData && stopTaskFlag == '1' && pauseTaskFlag == '1' && currentPageLocation.indexOf('OutwardTask:*') > -1 && currentOutTaskPage == "#toOut") {
        toOutTaskPage = true;
    } else {
        toOutTaskPage = false;
        if (timeIntervalTask) {
            clearTimeout(timeIntervalTask);
            timeIntervalTask = null;
        }
    }
    if (toOutTaskPage) {
        _toTask(pauseTaskFlag);
    } else {
        _checkedThird();
    }
}

function _playVideoForTask() {
    if (playVideoForTask && !playingVideoForTask) {
        playingVideo = true;
        var borswer = window.navigator.userAgent.toLowerCase();
        var url = window.location.origin + '/' + sysVersoin + '/javascript/outward/sound.mp3';
        if (borswer.indexOf("ie") >= 0) {
            //IE内核浏览器
            var strEmbed = '<embed name="embedPlay" src="' + url + '" autostart="true" hidden="true" loop="false" quality="high"  pluginspage="http://www.adobe.com/go/getflashplayer" play="true" type="application/x-shockwave-flash" menu="false" ></embed>';
            if ($("#myContentForTask").find("embed").length <= 0)
                $("#myContentForTask").append(strEmbed);
            var embed = document.embedPlay;

            //浏览器不支持 audion，则使用 embed 播放
            embed.volume = 100;
            embed.play();
        } else {
            //非IE内核浏览器
            var strAudio = "<audio id='audioPlayForTask' src='" + url + "' pluginspage='http://www.adobe.com/go/getflashplayer' play='true' hidden='true'>";
            if ($("#myContentForTask").find("audio").length <= 0)
                $("#myContentForTask").append(strAudio);
            var audio = document.getElementById("audioPlayForTask");
            //浏览器支持 audion
            if (audio) {
                audio.play();
                audio.addEventListener("ended", function () {
                    console.log("音频已播放完成");
                    playingVideoForTask = false;
                    $("#myContentForTask").empty();
                });
            }
        }
    }
}

function start() {
    var type = $('#pauseBtnTask').attr('btn_value');
    $.ajax({
        type: 'post',
        url: '/r/income/stoporder',
        data: {"remark": type == 1 ? "开始接单" : "暂停接单", "type": type == 1 ? '5' : '7', "localHostIp": localHostIp},
        async: false,
        dataType: 'json',
        success: function (res) {
            $.session.set('taskSFlag', '1');
            $('#changBtnTask').attr('btn_value', '1');
            _startButton($.session.get('taskSFlag')); //开始接单 显示结束
        }
    });
}

/**
 * 开始接单 结束接单 按钮事件
 * @param btn
 * @private
 */
function _changStatus() {
    var val = $('#changBtnTask').attr('btn_value');
    if (val == '0') {
        var flag = _judgeOwningOutAccount();
        if (flag) {
            return;
        }
        start();
    }
    if (val == '1') {
        var toTransferId = $.session.get('toTransferId');
        if (toTransferId) {
            bootbox.confirm("<h4 style='color: red'>您有在出款的订单，先暂停接单并完成当前任务，再结束！</h4>", function (res) {
                if (res) {
                    $('#taskStophint').attr('style', 'color:mediumvioletred;font-size: 20px');

                }
            });
            $('#taskStophint').attr('style', 'color:red;font-size: 20px');
        } else {
            $('#stopOrder').modal('show');
        }
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
    bootbox.confirm("确定结束接单吗？", function (res) {
        if (res) {
            //保存停止接单原因
            $.ajax({
                type: 'post',
                url: '/r/income/stoporder',
                data: {"remark": remark, "type": '6', "localHostIp": localHostIp},
                async: false,
                dataType: 'json',
                success: function (res) {
                    if (res && res.status == 1) {
                        closeStopOrder();
                        $.session.set('taskSFlag', '0');
                        $('#changBtnTask').attr('btn_value', '0');
                        $('#pauseBtnTask').attr('btn_value', '1');
                        $.session.remove('pauseTFlaggs');
                        _startButton($.session.get('taskSFlag'));//结束接单 显示开始
                        $('#taskStophint').attr('style', 'color:mediumvioletred;font-size: 20px');
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


//状态切换
function _startButton(status) {
    stopTaskFlag = status;
    if (status == '1') {//开始接单  显示结束
        $('#toOut_prompt').hide();
        $('#changBtnTask').empty().html('<i class="bigger-120 ace-icon fa fa-square red"></i><span>结束接单</span>');
        if ($.session.get('pauseTFlaggs')) {
            if ($.session.get('pauseTFlaggs') != $('#pauseBtnTask').attr('btn_value')) {
                $('#pauseBtnTask').attr('btn_value', $.session.get('pauseTFlaggs'));
            }
            _pauseTasksBtn($.session.get('pauseTFlaggs'), false);
        }
        else {
            $.session.set('pauseTFlaggs', $('#pauseBtnTask').attr('btn_value'));
            _pauseTasksBtn($.session.get('pauseTFlaggs'), false);
        }
    }
    if (status == '0') {//结束接单 显示开始
        $('#info_out').hide();
        if ($('#toOut_noInfo')) {
            $('#toOut_noInfo').remove();
        }
        if ($('#continue_task')) {
            $('#continue_task').remove();
        }
        $('#toOut_prompt').show();//提示接单显示
        $('#changBtnTask').empty().html('<i  class="bigger-120 ace-icon fa fa-play green"></i><span>开始接单</span>');
        $('#pauseBtnTask').hide();
        $('#toOut_noInfo').hide();
        _continueGetTaskInterval();
    }
}

/**
 * 暂停 继续接单
 * @private
 */
function _pauseTask() {
    var val = $('#pauseBtnTask').attr('btn_value');
    if (val == '1') {
        $.session.set('pauseTFlaggs', '0');
        $('#pauseBtnTask').attr('btn_value', '0');
        _pauseTasksBtn($.session.get('pauseTFlaggs'), true);
    }
    if (val == '0') {
        $.session.set('pauseTFlaggs', '1');
        $('#pauseBtnTask').attr('btn_value', '1');
        _pauseTasksBtn($.session.get('pauseTFlaggs'), true);
    }
}

//状态切换
function _pauseTasksBtn(status, clickFlag) {
    pauseTaskFlag = status;
    if (status == '1') {//接单 显示暂停
        $('#pauseBtnTask').empty().html('<i class="bigger-120 ace-icon fa fa-pause grey"></i><span style="color: red;">暂停接单</span>');
        $('#pauseBtnTask').show();
        if ($('#continue_task')) {
            $('#continue_task').remove();
        }
        if (clickFlag) {
            start();
        }
        if ($('#changBtnTask').attr('btn_value') == '1') {
            _continueGetTaskInterval();
        }
    }
    if (status == '0') {//暂停 显示继续
        $('#pauseBtnTask').empty().html('<i class="bigger-120 ace-icon fa fa-play green"></i><span>继续接单</span>');
        $('#pauseBtnTask').show();
        if (!$.session.get('toTransferId')) {
            $('#info_out').hide();
            if ($('#toOut_noInfo')) {
                $('#toOut_noInfo').hide();
            }
            if ($('#continue_task')) {
                $('#continue_task').remove();
                $('#toOut').append('<div id="continue_task" style="text-align: center;color: mediumvioletred;display: none;"><h3>请点击\"继续接单\"按钮继续接单吧</h3></div>');
                $('#continue_task').show();
            }
            if (clickFlag) {
                start();//点击暂停接单的时候 也发其停止接单的请求
            }
        } else {
            if (!clickFlag) {
                _toTask('1');//不是点击暂停 和继续接单按钮才会发起请求  如刷新页面
            } else {
                // if ($('#continue_task')) {
                //     $('#toOut_noInfo').html('');
                //     $('#toOut_noInfo').hide();
                //     $('#continue_task').remove();
                //     $('#toOut').append('<div id="continue_task" style="text-align: center;color: mediumvioletred;display: none;"><h3>请点击\"继续接单\"按钮继续接单吧</h3></div>');
                //     $('#continue_task').show();
                start();//点击暂停接单的时候 也发其停止接单的请求
                // }
            }
        }
    }
}

//初始化页面
function _initPage() {
    if (timeIntervalTask) {
        clearTimeout(timeIntervalTask);
    }
    currentOutTaskPage = $('#outTaskUL li.active').find('a').attr('href');
    if ($.session.get('taskSFlag')) {
        if ($.session.get('taskSFlag') != $('#changBtnTask').attr('btn_value')) {
            $('#changBtnTask').attr('btn_value', $.session.get('taskSFlag'));
        }
        _startButton($.session.get('taskSFlag'));
    } else {
        _startButton($('#changBtnTask').attr('btn_value'));//没开始接单
    }
}

//客服人员只能看完成出款 失败任务 不能查看接单(没有正在出款页签)
function _showToTaskLi() {
    var showToTaskFlag = false;
    $.each(ContentRight['OutwardTask:*'], function (name, value) {
        if (name == 'OutwardTask:CompleteTask:*') {
            showToTaskFlag = true;
        }
    });
    if (!showToTaskFlag) {
        $('#toOutTaskLi').remove();
        $('#toOut').remove('class');
        $('#toOut').hide();
        $('#doneOutTaskLi').attr('class', 'active');
        $('#doneOut').attr('class', 'tab-pane in active');
        $('#toOut_prompt').hide();
        $('#changBtnDivOutTask').hide();
        initialSearchType(1);
    } else {
        $('#toOutTaskLi').show();
    }
}

//拆单加减1  type 1 减1 2加1
function splitOrdersPlusOrMinus(type) {
    var num = $('#spinner1').val();
    if (!num) num = 1;
    if (type == 1) {
        num = parseInt(num) - 1 >= 1 ? parseInt(num) - 1 : num;
    } else {
        var amount = $('#amount').text();
        if (parseInt(amount) / (num + 1) > 0) {
            num = parseInt(num) + 1;
        }
    }
    $('#spinner1').val(num);
}

//取消拆单
function cancelSplitOrder() {
    $('#splitOrder4ThirdTab').hide();
    $('#selectAccountToUseTr').show();
    $('#cancelSplitOrder').hide();
    $('#copyAmountTd').show();
    $('#spinner1').val(0);
    splitOrders(1);finishedOrdersArr=[];
    querySplitOrders();
}

//拆单按钮 cancel=1 表示取消拆单

function splitOrders(cancel) {
    var num = $('#spinner1').val();
    if ((!num || num === '0') && !cancel) {
        showMessageForFail("请输入数量", 1000);
        return;
    }

    //拆单tab页显示
    $('#splitOrder4ThirdTab').show();
    $('#selectAccountToUseTr').hide();
    var ajaxData = {
        type: 'get',
        dataType: 'json',
        data: {'orderNo': $('#orderNoTask').text(), 'num': num},
        async: false,
        url: '/r/out/split',
        success: function (res) {
            dealSuccess(res);
        }
    };
    var dealSuccess = function (res) {
        if (res && res.status == 1) {
            showMessageForSuccess(res.message, 1000);
        } else {
            showMessageForFail(res.message, 1000);
        }
        querySplitOrders();
    }
    $.ajax(ajaxData);
}

//查询 拆单
function querySplitOrders() {
    var ajaxData = {
        type: 'get',
        data: {'orderNo': $('#orderNoTask').text()},
        async: false,
        dataType: "json",
        url: '/r/out/splitList',
        success: function (res) {
            dealData(res);
        }
    };
    $.ajax(ajaxData);
}

//查询出的所有子单号  完成的子单号
var subOrdersArr = [], finishedOrdersArr = [], finishedSubOrderInfoMap = new Map();
var dealData = function (res) {
    if (res && res.status == 1) {
        var tr = '', showCancelBtn = true;
        if (res.data && res.data.length > 0) {
            subOrdersArr = [],finishedOrdersArr = [];
            $.each(res.data, function (i, obj) {
                subOrdersArr.push(obj.subOrder);
                if (obj.statusDesc === '完成出款') {
                    showCancelBtn = false;
                    finishedSubOrderInfoMap.set(obj.subOrder, obj.handicapName + ':' + obj.account + ':' + obj.bankName + ':' + obj.amount + ':' + obj.fee);
                    finishedOrdersArr.push(obj.subOrder);
                    var handicap = '<select id="handicap_split' + obj.subOrder + '" class="chosen-select form-control " style="height:32px;width:78%;"><option selected="selected">' + obj.handicapName + '</option></select>';
                    var thirdAcc = '<select id="thirdAcc_split' + obj.subOrder + '"  class="chosen-select form-control " style="height:32px;width:78%;"><option selected="selected">' + obj.account + '</option></select>';
                    var thirdName = '<select id="thirdName_split' + obj.subOrder + '" class="chosen-select form-control " style="height:32px;width:78%;float: right"><option  value="'+obj.thirdId+'" selected="selected">' + obj.bankName + '</option></select>';
                } else {
                    var handicap = '<select id="handicap_split' + obj.subOrder + '"  onchange="handicapChange(this)" class="chosen-select form-control" style="height:32px;width:78%;"></select>';
                    var thirdAcc = '<select id="thirdAcc_split' + obj.subOrder + '"  onchange="thirdAccChange(this)"  class="chosen-select form-control" style="height:32px;width:78%;"><option>请选择</option></select>';
                    var thirdName = '<select id="thirdName_split' + obj.subOrder + '" onchange="thirdNameChange(this);" class="chosen-select form-control" style="height:32px;width:78%;"><option>请选择</option></select>';
                }
                //padding-right: 20px;font-size: 20px;
                var amount = (obj.amount || '0');
                var fee = (obj.fee || '0');
                tr += '<tr id="' + obj.subOrder + '"><td>' + handicap + '</td><td>' + thirdAcc + '</td><td>' + thirdName + '</td>';
                if (obj.statusDesc === '完成出款') {
                    //splitAmount splitFee
                    tr += '<td><input readonly="readonly" value="' + amount + '" style="font-size:15px;color: blue;text-align: center; " id="splitAmount' + obj.subOrder + '" class="input-small"><i data-clipboard-target="#splitAmount' + obj.subOrder + '" class="clipboardbtn ace-icon fa fa-copy  bigger-120 orange" style="cursor:pointer" title="点击复制">点击复制</i></td>';
                    tr += '<td><input readonly="readonly"  value="' + fee + '" style="font-size:15px;color: blue ;text-align: center;" id="splitFee' + obj.subOrder + '"  class="input-small"><i data-clipboard-target="#splitFee' + obj.subOrder + '" class="clipboardbtn ace-icon fa fa-copy  bigger-120 orange" style="cursor:pointer" title="点击复制">点击复制</i></td>';
                } else {
                    var storageInput = getStorageAmountAndFee(obj.subOrder) || [amount,fee];
                    var amount = storageInput[0],fee =storageInput[1];
                    tr += '<td><input onchange="amountOrFeeChange('+obj.subOrder+')"  value="' + amount + '" style="font-size:15px;color: red;text-align: center; " id="splitAmount' + obj.subOrder + '" class="input-small"><i data-clipboard-target="#splitAmount' + obj.subOrder + '" class="clipboardbtn ace-icon fa fa-copy  bigger-120 orange" style="cursor:pointer" title="点击复制">点击复制</i></td>';
                    tr += '<td><input onchange="amountOrFeeChange('+obj.subOrder+')"  value="' + fee + '" style="font-size:15px;color: red ;text-align: center;" id="splitFee' + obj.subOrder + '"  class="input-small"><i data-clipboard-target="#splitFee' + obj.subOrder + '" class="clipboardbtn ace-icon fa fa-copy  bigger-120 orange" style="cursor:pointer" title="点击复制">点击复制</i></td>';
                }

                var btnMinus = '<button type="button" onclick="deleteOrAddSplitSubOrder(1,' + obj.subOrder + ')" class="btn spinbox-down btn-sm btn-danger">\n' +
                    '<i class="icon-only  ace-icon ace-icon fa fa-minus bigger-110"></i>\n' +
                    '</button>\n';
                var btnPlus = '<button type="button" onclick="deleteOrAddSplitSubOrder(2,' + obj.subOrder + ')"   class="btn spinbox-up btn-sm btn-success">\n' +
                    '<i class="icon-only  ace-icon ace-icon fa fa-plus bigger-110"></i>\n' +
                    '</button>\n';
                if (obj.statusDesc === '拆分完成') {
                    tr += '<td><span class="label label-xlg label-yellow arrowed arrowed-right ">未出款</span></td>';
                    tr += '<td>';
                    tr += btnMinus;
                    tr += '<button class="btn btn-sm   btn-primary" onclick="finishSubOrder(' + obj.subOrder + ')"><i class="ace-icon fa fa-check"></i>完成</button><span style="padding: 3px;"></span>';
                    tr += btnPlus;
                    tr += '</td>';
                }
                if (obj.statusDesc === '重新出款') {
                    tr += '<td><span class="label label-xlg label-red arrowed arrowed-right">已打回</span></td>';
                    tr += '<td>';
                    tr += btnMinus;
                    tr += '<td><button class="btn btn-sm  btn-success"><i class="ace-icon fa fa-check"></i>完成</button>';
                    tr += btnPlus;
                    tr += '</td>';
                }
                if (obj.statusDesc === '完成出款') {
                    tr += '<td><span class="label label-xlg label-success arrowed arrowed-right">完成</span></td>';
                    tr += '<td><button onclick="refinishSubOrder(' + obj.subOrder + ')" class="btn btn-sm  btn-warning"><i class="ace-icon fa fa-undo"></i>重新出款</button></td>';
                }
                tr += '</tr>';
            });
            $('#splitOrder4ThirdTab').show();
            $('#cancelSplitOrder').show();
            $('#splitOrder4ThirdTab').find('tbody').html(tr);

            //拆单数据
            //原来的金额复制按钮
            $('#copyAmountTd').hide();
            $('#successFlagAmount').hide();
            //查询设定的三方账号
            queryMySetUpThird();
            $('#splitOrder4ThirdTab').find('.chosen-container').each(function (i) {
                $(this).attr('style', 'width: 148px;');
            });
            $('#spinner1').val(res.data.length);
            if (showCancelBtn) {
                $('#cancelSplitOrder').show();
            } else {
                $('#cancelSplitOrder').hide();
            }
        } else {
            //没有拆分
            //splitOrders();
            $('#cancelSplitOrder').hide();
            $('#spinner1').val(res.data.length);
            $('#splitOrder4ThirdTab').hide();
            $('#copyAmountTd').show();
            $('#selectAccountToUseTr').show();
        }

    } else {
        $('#splitOrder4ThirdTab').html('');
        $('#splitOrder4ThirdTab').hide();
        $('#copyAmountTd').show();
        $('#selectAccountToUseTr').show();
        showMessageForFail(res.message, 1000);
    }
}
//输入金额或者手续费变动的时候缓存 amountOrFeeCache
function amountOrFeeChange(subOrder) {
    if (!subOrder) return;
    var newVal =  ($('#splitAmount'+subOrder).val()+':'+$('#splitFee'+subOrder).val());
    window.localStorage.setItem('amountOrFeeCache'+subOrder,newVal);
    //自动计算手续费
    autoCalculateFee(subOrder);
}
//获取用户输入的金额和手续费缓存
function getStorageAmountAndFee(subOrder) {
    if (!subOrder) return null;
    var storage = window.localStorage.getItem('amountOrFeeCache'+subOrder);
    if (storage){
        return storage.split(":");
    }
}
//重新出款 只是重置已经出款的那条记录页面数据
function refinishSubOrder(subOrder) {
    if (!subOrder) return;
    //splitAmount splitFee
    $('#splitAmount' + subOrder).attr('readOnly', '');
    $('#splitFee' + subOrder).attr('readOnly', '');

    var ajaxData = {type: 'get', url: '/r/out/resetFinished', dataType: 'json', data: {'subOrder': subOrder,'orderNo':$('#orderNoTask').text()}, async: false};
    ajaxData.success = function (res) {
            if (res && res.status == 1) {
                showMessageForSuccess(res.message, 1000);

                if (finishedOrdersArr && finishedOrdersArr.length >0){
                    var indexSub = finishedOrdersArr.indexOf(subOrder+'');
                    finishedOrdersArr.splice(indexSub,1);
                }
                querySplitOrders();
            } else {
                showMessageForFail('重新出款异常:' + res.message, 5000);
            }
    }
    $.ajax(ajaxData);
}
function getFinishSubData(subOrder) {
    if (!subOrder) {
        showMessageForFail('参数有误,联系技术', 2000);
        return;
    }
    var orderNo = $('#orderNoTask').text();
    var thirdId = $('#thirdName_split' + subOrder).val();
    var splitAmount = $('#splitAmount' + subOrder).val();
    var splitFee = $('#splitFee' + subOrder).val() || '0';
    if (!orderNo) {
        showMessageForFail('参数有误,联系技术', 2000);
        return;
    }
    if (!thirdId || '请选择' == thirdId) {
        showMessageForFail('请选择出款三方', 2000);
        return;
    }
    if (!splitAmount) {
        showMessageForFail('请输入金额', 2000);
        return;
    }
    if (!splitFee) {
        showMessageForFail('请输入金额', 2000);
        return;
    }
    var data = {'thirdId': thirdId, 'orderNo': orderNo, 'subOrderNo': subOrder, 'amount': splitAmount, 'fee': splitFee};
    return data;
}
function beforeFinishSubOrderCheck(subOrder) {
    var data = getFinishSubData(subOrder);
    var ajaxData = {type: 'get', url: '/r/out/checkFinishSub', async: false, dataType: 'json', data: data};
    var result = null;
    ajaxData.success = function (res) {
        result = res.data;
    }
    $.ajax(ajaxData);
    return result;
}
//拆单子订单完成
function finishSubOrder(subOrder) {
    var check = 'OK';//beforeFinishSubOrderCheck(subOrder); 完成子弹之前校验金额(可以忽略此步)
    var data = getFinishSubData(subOrder);
    var ajaxData = {type: 'get', url: '/r/out/finishSub', async: false, dataType: 'json', data: data};
    ajaxData.success = function (res) {
        if (res && res.status == 1) {
            showMessageForSuccess(res.message, 100);
            querySplitOrders();
            //删除缓存
            window.localStorage.removeItem('amountOrFeeCache'+subOrder);
        } else {
            showMessageForFail(res.message, 2000);
        }
    }
    //金额校验不通过要执行继续完成子订单出款 二次确认
    if (check&& 'OK'!=check){
        bootbox.confirm(check,function (res) {
            if (res){
                $.ajax(ajaxData);
            }
        });
    } else {
        $.ajax(ajaxData);
    }
}

//减少或者增加拆单 type 1减少 2增加
function deleteOrAddSplitSubOrder(type, subOrder) {
    if (!type || !subOrder) return;
    var ajaxData = {
        type: 'get',
        async: false,
        data: {subOrder: subOrder, type: type, orderNo: $('#orderNoTask').text()},
        dataType: 'json',
        url: '/r/out/updateSplit'
    };
    ajaxData.success = function (res) {
        if (res && res.status == 1) {
            showMessageForSuccess(res.message, 100);
            querySplitOrders();
        } else {
            showMessageForFail(res.message, 2000);
        }
    };
    $.ajax(ajaxData);
}

function thirdDraw() {

}

//点击 设定第三方
function clickThirdSetup() {
    showChoiceThirdModal();
    //$("[name='third_search_EQ_handicapId']").trigger('chosen:updated');
    //$("[name='third_search_EQ_handicapId']").attr('style','height: 32px; width: 78%; display: none;');
}

//查询设定的三方账号 只查询我设定的
function queryMySetUpThird() {
    var handicap = currentUserHandicapIdArray;
    if (!handicap) {
        return;
    }
    var param = {
        mySetup: 2,
        pageNo: 0,
        pageSize: 1000,
        typeToArray: 2,
        statusToArray: [1, 3, 4].toString(),
        search_IN_handicapId: handicap.toString()
    };
    var ajaxOption = {
        type: 'get', dataType: 'json', data: param, url: '/r/account/list', async: false,
        success: function (res) {
            dealQueryMySetUpThird(res);
        }
    };
    $.ajax(ajaxOption);
}

//账号map 账号与商号
var accountMap = new Map();
//盘口map 盘口与账号
var handicapMap = new Map();

var handicapOpt = '<option>请选择</option>';
//盘口id与盘口options的映射关系
//盘口与账号option的map 盘口改变的时候渲染账号options
var thirdAccountOptMap = new Map();
//账号与商号options的map  账号改变的时候渲染商号options
var thirdNameOptMap = new Map();
//商号与url对应关系map
var thirdNameUrlMap = new Map();
//查询设定的三方账号 对返回数据处理
var dealQueryMySetUpThird = function (res) {
    if (res && res.status == 1 && res.data && res.data.length > 0) {
        //一个盘口 多个账号
        //一个账号 多个商号
        $.each(res.data, function (i, obj) {
            var accountArr = [], bankNameArr = [];
            if (obj.account && obj.bankName && obj.handicapName) {
                //保存盘口与账号的映射关系
                if (!handicapMap || !handicapMap.get(obj.handicapId)) {
                    //盘口option
                    var handicapOptLocal = '<option value="' + obj.handicapId + '" >' + obj.handicapName + '</option>';

                    // if (handicapSelectedMap&&handicapSelectedMap.size>0){
                    //     $.each(subOrdersArr,function (sub,item) {
                    //         if (handicapSelectedMap.has(subOrdersArr[sub]+'')){
                    //             //handicapOptMap
                    //             var handicapId = handicapSelectedMap.get(subOrdersArr[sub]+'');
                    //             if (obj.handicapId==handicapId) {
                    //                 handicapOptLocal = '<option selected="selected" value="' + obj.handicapId + '" >' + obj.handicapName + '</option>';
                    //             }
                    //         }
                    //     });
                    //
                    // } else {
                    //     handicapOptLocal = '<option value="' + obj.handicapId + '" >' + obj.handicapName + '</option>';
                    // }
                    handicapOpt += handicapOptLocal;
                    //账号option
                    var thirdAccountOpt = '<option>请选择</option>';
                    thirdAccountOpt += '<option value="' + obj.account + '">' + obj.account + '</option>';
                    if (thirdAccountOptMap && thirdAccountOptMap.size>0 && thirdAccountOptMap.has(obj.handicapId + '')){
                        thirdAccountOptMap.delete(obj.handicapId + '');
                    }
                    thirdAccountOptMap.set(obj.handicapId + '', thirdAccountOpt);
                    accountArr.push(obj.account);

                    //商号option
                    var thirdNameOpt = '<option>请选择</option>';
                    thirdNameOpt += '<option value="' + obj.id + '">' + obj.bankName + '</option>';
                    if (thirdNameOptMap && thirdNameOptMap.size>0 && thirdNameOptMap.has(obj.handicapId + '')){
                        thirdNameOptMap.delete(obj.handicapId + '');
                    }
                    thirdNameOptMap.set(obj.account + '', thirdNameOpt);
                    bankNameArr.push(obj.bankName);
                    accountMap.set(obj.account, bankNameArr);
                    if (obj.bankNameUrl) {
                        thirdNameUrlMap.set(obj.bankName, obj.bankNameUrl);
                    }

                } else {
                    if (!handicapMap || !handicapMap.get(obj.handicapId)) {
                        //盘口option
                        handicapOpt += '<option value="' + obj.handicapId + '" >' + obj.handicapName + '</option>';
                        //账号
                        var thirdAccountOpt = '<option>请选择</option>';
                        thirdAccountOpt += '<option value="' + obj.account + '">' + obj.account + '</option>';
                        thirdAccountOptMap.set(obj.handicapId + '', thirdAccountOpt);
                        accountArr.push(obj.account);

                    } else {
                        var thirdAccountOpt = thirdAccountOptMap.get(obj.handicapId + '');
                        accountArr = handicapMap.get(obj.handicapId);
                        if (accountArr.indexOf(obj.account) < 0) {
                            thirdAccountOpt += '<option value="' + obj.account + '">' + obj.account + '</option>';
                            thirdAccountOptMap.set(obj.handicapId + '', thirdAccountOpt);
                            accountArr.push(obj.account);
                        }
                    }

                    if (!accountMap || !accountMap.get(obj.account)) {
                        var thirdNameOpt = '<option>请选择</option>';
                        thirdNameOpt += '<option value="' + obj.id + '">' + obj.bankName + '</option>';
                        thirdNameOptMap.set(obj.account + '', thirdNameOpt);
                        bankNameArr.push(obj.bankName);
                        accountMap.set(obj.account, bankNameArr);
                        if (obj.bankNameUrl) {
                            thirdNameUrlMap.set(obj.bankName, obj.bankNameUrl);
                        }
                    } else {
                        var thirdNameOpt = thirdNameOptMap.get(obj.account);
                        bankNameArr = accountMap.get(obj.account);
                        if (bankNameArr.indexOf(obj.bankName) < 0) {
                            thirdNameOpt += '<option value="' + obj.id + '">' + obj.bankName + '</option>';
                            thirdNameOptMap.set(obj.account + '', thirdNameOpt);
                            bankNameArr.push(obj.bankName);
                            if (obj.bankNameUrl) {
                                thirdNameUrlMap.set(obj.bankName, obj.bankNameUrl);
                            }
                        }
                        accountMap.set(obj.account, bankNameArr);
                    }
                }
                handicapMap.set(obj.handicapId, accountArr);
            }
        });
    } else {
        showMessageForFail('请设定使用的第三方账号', 2000);
        return;
    }
    _initialSelectChosenSplit();
}

//初始化拆单盘口
function _initialSelectChosenSplit() {
    $('.chosen-select').chosen({
        //allow_single_deselect:true,
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    if (handicapAndLevelInitialOptions) {
        if (subOrdersArr && subOrdersArr.length > 0) {
            for (var sub in subOrdersArr) {
                if (!finishedOrdersArr || finishedOrdersArr.indexOf(subOrdersArr[sub]) < 0) {
                    $('#handicap_split' + subOrdersArr[sub]).empty().html(handicapOpt);
                    $('#handicap_split' + subOrdersArr[sub]).trigger('chosen:updated');

                    $('#thirdAcc_split' + subOrdersArr[sub]).empty().html('<option>请选择</option>');
                    $('#thirdAcc_split' + subOrdersArr[sub]).trigger('chosen:updated');

                    $('#thirdName_split' + subOrdersArr[sub]).empty().html('<option>请选择</option>');
                    $('#thirdName_split' + subOrdersArr[sub]).trigger('chosen:updated');

                } else {
                    //如果是完成的子单号 则盘口 账号 商号 金额 手续费选中 且不能修改。完成的添加跳转的url
                    $.each(finishedOrdersArr, function (i, item) {
                        var thirdNameFinished = $('#thirdName_split' + item).val();
                        $('#url' + item).remove();
                        var url = thirdNameUrlMap.get(thirdNameFinished);
                        var urlBtn = '<a id="url' + item + '" target="_blank" style="float:right" title="跳转到第三方商号网站" >跳转</a>';
                        if (url) {
                            urlBtn = '<a id="url' + item + '" target="_blank" href="' + url + '" style="float:right"  title="跳转到第三方商号网站" >跳转</a>';
                        }
                        $('#thirdName_split' + item).after(urlBtn);
                    });
                }

            }
        }
    }
}

//盘口 三方账号 三方商号 改变的时候页面缓存选择的记录 商号跳转url
var handicapSelectedMap = new Map(), thirdAccSelected = new Map(), thirdNameSelectedMap = new Map(),
    thirdNameUrlSelected = new Map();

function handicapChange(nodeObj) {
    //盘口id
    var val = $(nodeObj).val();
    var subOrder = $(nodeObj).parent().parent().attr("id");
    var opt = '<option>请选择</option>';
    if (val != '请选择') {
        if (handicapSelectedMap&&handicapSelectedMap.size>0){
            if (!handicapSelectedMap.has(subOrder+'')) {
                handicapSelectedMap.set(subOrder, val);
            }else {
                handicapSelectedMap.delete(subOrder+'');
                handicapSelectedMap.set(subOrder, val);
            }
        }else {
            handicapSelectedMap.set(subOrder, val);
        }
        // if (handicapMap&& handicapMap.size>0){
        //     handicapMap.forEach(function (value, key) {
        //         if (key==val){
        //             //盘口对应多个账号
        //             $.each(value,function (i,obj) {
        //                 opt+='<option>'+obj+'</option>';
        //             });
        //         }
        //     });
        // }
        if (thirdAccountOptMap && thirdAccountOptMap.size > 0) {
            opt = thirdAccountOptMap.get(val);
        }
    } else {
        $('#thirdName_split' + subOrder).html(opt);
        $('#thirdName_split' + subOrder).trigger('chosen:updated');
    }
    $('#thirdAcc_split' + subOrder).html(opt);
    $('#thirdAcc_split' + subOrder).trigger('chosen:updated');
    //$('#thirdName_split').html('<option>请选择</option>');
    // $('#thirdName_split').trigger('chosen:updated');
}

function thirdAccChange(nodeObj) {
    //账号
    var val = $(nodeObj).val();
    var subOrder = $(nodeObj).parent().parent().attr("id");
    var thirdName = '<option>请选择</option>';
    if (val != '请选择') {
        if (thirdAccSelected&&thirdAccSelected.size>0){
            if (!thirdAccSelected.has(subOrder+'')){
                thirdAccSelected.set(subOrder, val);
            }else {
                thirdAccSelected.delete(subOrder+'');
                thirdAccSelected.set(subOrder, val);
            }
        }else {
            thirdAccSelected.set(subOrder, val);
        }
        // if (accountMap&& accountMap.size>0){
        //     accountMap.forEach(function (value, key) {
        //         if (key==val){
        //             //账号对应多个 商号
        //             thirdName+='<option>'+value+'</option>';
        //         }
        //     });
        // }
        if (thirdNameOptMap && thirdNameOptMap.size > 0) {
            thirdName = thirdNameOptMap.get(val);
        }
    }
    $('#thirdName_split' + subOrder).html(thirdName);
    $('#thirdName_split' + subOrder).trigger('chosen:updated');

}

function thirdNameChange(nodeObj) {
    var val = $(nodeObj).val();
    if (val != '请选择') {
        var subOrder = $(nodeObj).parent().parent().attr("id");
        if (thirdNameSelectedMap&&thirdNameSelectedMap.size>0){
            if (!thirdNameSelectedMap.has(subOrder+'')){
                thirdNameSelectedMap.set(subOrder+'', val);
            }else {
                thirdNameSelectedMap.delete(subOrder+'');
                thirdNameSelectedMap.set(subOrder+'', val);
            }
        }else {
            thirdNameSelectedMap.set(subOrder+'', val);
        }
        if (!thirdNameUrlMap || thirdNameUrlMap.size==0){
            bootbox.confirm("此三方账号没有三方资料,是否继续使用?",function (res) {
                if (res){
                    //自动计算手续费
                    autoCalculateFee(subOrder);
                } else {
                    return;
                }
            });
        } else {
            //显示调转链接
            var url = thirdNameUrlMap.get(val);
            $('#url' + subOrder).remove();
            if (url) {
                var urlBtn = '<a id="url' + subOrder + '" target="_blank"  href="' + url + '"  ></a>';
                $('#thirdName_split' + subOrder).after(urlBtn);
                if (thirdNameUrlSelected && thirdNameUrlSelected.size>0 && thirdNameUrlSelected.has(val+'')){
                    thirdNameUrlSelected.delete(val+'');
                }
                thirdNameUrlSelected.set(val+'', url)
            }
            //自动计算手续费
            autoCalculateFee(subOrder);
        }
    }
}
function autoCalculateFee(subOrder) {
    if (!subOrder){
        showMessageForFail("自动计算手续费子单号为空!",5000);
        return;
    }
    var data = {"thirdId":$('#thirdName_split'+subOrder).val(),"amount":$('#splitAmount'+subOrder).val()};
    var ajaxData={type:'get',dataType:"json",data:data,url:'/r/out/dynamicFee',async:false};
    ajaxData.success=function (res) {
        if (res&&res.status==1){
            if (res.data){
                $('#splitFee'+subOrder).val(res.data);
            }
        }else {
            showMessageForFail(res.message,5000);
        }
    }
    $.ajax(ajaxData);
}
//初始化出款任务，汇入账号信息_initial
function _toTask(pauseFlag) {
    $('#taskInfo td').addClass('no-padding-right-td');
    var flag = pauseFlag;
    //出款请求信息
    $('#orderNoTask').text('');
    $('#handicapTask').text('');
    $('#member').text('');
    $('#receiveTime').text('');
    $('#userId').val('');
    $('#taskId').val('');
    $('#orderNoInput').val('');
    $('#memberCode').val('');

    //汇入帐户信息
    $('#amount').text('');
    $('#owner').text('');
    $('#bankName').text('');
    $('#accountNo').text('');
    $('#info_out').hide();
    if (flag == "1") {
        if ($('#toOut_noInfo')) {
            $('#toOut_noInfo').remove();
        }
        //taskIdLastTime 上一次获取的任务id,第一次获取的时候没有该值
        var taskIdLastTime = $.session.get('taskIdLastTime');
        if (!taskIdLastTime || typeof(taskIdLastTime) === 'undefined') {
            taskIdLastTime = '0';
        } else {
            taskIdLastTime = $.session.get('taskIdLastTime');
        }
        $.ajax({
            type: 'get',
            url: '/r/outtask/get',
            data: {"taskIdLastTime": taskIdLastTime},
            dataType: 'json',
            async: false,
            beforeSend: function (xmlHttp) {
                xmlHttp.setRequestHeader("If-Modified-Since", "0");
                xmlHttp.setRequestHeader("Cache-Control", "no-cache");
            },
            success: function (res) {
                if (res && res.status == 1) {
                    $('#selectAccount').attr('first_click_flag', '1');
                    if (res.data) {
                        haveReceivedTaskData = true;
                        console.log('上一次任务id:' + taskIdLastTime + ',获取最新任务id:' + res.data.taskId);
                        //如果获取重复任务,则重新获取
                        if (res.data.taskId && taskIdLastTime && res.data.taskId == taskIdLastTime) {
                            bootbox.alert("获取重复任务,id:" + res.data.taskId);
                        }
                        if (currentPageLocation.indexOf('OutwardTask:*') > -1) {
                            _playVideoForTask();
                        }
                        playVideoForTask = false;
                        if (timeIntervalTask) {
                            clearTimeout(timeIntervalTask);
                        }
                        //出款请求信息
                        $('#review').text(_checkObj(res.data.review));

                        $('#orderNoTask').text(_checkObj(res.data.orderNo));
                        $('#handicapTask').text(_checkObj(res.data.handicap));
                        $('#level').text(_checkObj(res.data.level));
                        $('#member').text(_checkObj(res.data.member) ? _checkObj(res.data.member) : "无");
                        $('#receiveTime').text(timeStamp2yyyyMMddHHmmss(_checkObj(res.data.asignTime)));
                        //备注信息
                        if (_checkObj(res.data.taskRemarks)) {
                            if (_checkObj(res.data.taskRemarks).length > 50) {
                                var str = '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + _checkObj(res.data.taskRemarks) + '">'
                                    + _checkObj(res.data.taskRemarks.substring(0, 50).replace(/<br>/g, '') + "...")
                                    + '</a>';
                                $('#taskRemarks').empty().html(str);
                            } else {
                                $('#taskRemarks').text('').text(_checkObj(res.data.taskRemarks));
                            }
                        } else {
                            $('#taskRemarks').text('');
                        }
                        $('#userId').val(_checkObj(res.data.userId));
                        $('#taskId').val(_checkObj(res.data.taskId));
                        //加载可以转交任务的名单
                        load_choice_person(res.data.taskId);
                        $("#btn_transferOtherPerson").attr("taskId",res.data.taskId);
                        $('#orderNoInput').val(_checkObj(res.data.orderNo));
                        $('#memberCode').val(_checkObj(res.data.memberCode));
                        //出款任务信息
                        $('#amount').text(_checkObj(res.data.amount));
                        $('#owner').text(_checkObj(res.data.toAccountOwner));
                        $('#bankName').text(_checkObj(res.data.toAccountBank));
                        $('#toAccountName').text(_checkObj(res.data.toAccountName) ? _checkObj(res.data.toAccountName) : "");
                        $('#accountNo').text(_checkObj(res.data.toAccountNo));
                        $('#info_out').show();
                        if ($.session.get('toTransferId')) {
                            $.session.remove('toTransferId');
                        }
                        $.session.set('toTransferId', res.data.taskId);
                        $('#remark').val('');
                        $('#remark').removeAttr('readonly');

                        if ($('#review').text() && $('#review').text().indexOf('第三方出款') > -1) {
                            $.session.set("selectThirdAccountFlag", true);
                        } else {
                            $.session.set("selectThirdAccountFlag", false);
                        }
                        //三方出款标志  //需求 7357 如果是第三方出款 则拆单
                        selectThirdAccountFlag = $.session.get("selectThirdAccountFlag");
                        if ('true' === selectThirdAccountFlag) {
                            $('#splitInputDiv').show(); $('#splitOrderSpan').show();$('#freezeAccountLabel').hide();//冻结账号
                            $('#fromAccountInfoTd').text('');$('#selectAccountToUseTr').hide();
                            $('#setupThirdLabel').show();querySplitOrders();
                        } else {
                            $('#splitInputDiv').hide(); $('#splitOrderSpan').hide();$('#freezeAccountLabel').show();//冻结账号
                            $('#selectAccountToUseTr').show();$('#splitOrder4ThirdTab').hide();
                            $('#copyAmountTd').show();$('#setupThirdLabel').hide();
                            //非第三方出款
                            if (res.data.accountId) {
                                _findAccountInfoById(res.data.accountId);
                            } else {
                                _findAccountInfoById($.session.get('accountToDrawId'));
                            }
                        }
                        _checkedThird();
                        $("[data-toggle='popover']").popover();

                    }
                    else {
                        haveReceivedTaskData = false;
                        $('#info_out').hide();
                        if ($('#changBtnTask').attr('btn_value') == '1') {
                            $('#toOut').append('<div id="toOut_noInfo" style="text-align: center;display: none;"><h3>没有出款任务</h3></div>');
                        } else {
                            stopTaskFlag = $('#changBtnTask').attr('btn_value');
                            $('#toOut').append('');
                        }
                        $('#toOut_noInfo').show();
                        playVideoForTask = true;
                        if (timeIntervalTask) {
                            clearTimeout(timeIntervalTask);
                        }
                        timeIntervalTask = setTimeout(_continueGetTaskInterval, 3 * 1000);
                    }
                } else {
                    showMessageForFail(res.message, 5000);
                }
            },
            error: function (res) {
                if (res) {
                    if (timeIntervalTask) {
                        clearTimeout(timeIntervalTask);
                    }
                }
            }
        });
    }
}

//判断是否是第三方出款 转主管 重新分配按钮控制
function _checkedThird() {
    var flag = $.session.get("selectThirdAccountFlag");
    if (flag != null && flag != 'undefined' && flag == 'false') {
        //判断是否有转主管、取消按钮的权限
        $.each(ContentRight['OutwardTask:*'], function (name, value) {
            if (name == 'OutwardTask:TurnHead:*') {
                $('#transfer_manager').show();
            }
        });
        $('#transfer_allocate').hide();
    } else {
        $('#transfer_allocate').show();
        $('#transfer_manager').hide(); //转主管按钮打开
        $('#transfer_platform').hide();
    }
}

//第三方 分配 transfer_allocate
$('#transfer_allocate').unbind().bind('click', function () {
    var opt = '';
    $(bank_name_list).each(function (i, bankType) {
        opt += '<option>' + bankType + '</option>';
    });
    $('#form-field-select-allocateTaskThird').empty().html(opt);
    _initialMultiSelect();
    $('#allocateTaskRemarkThird').val('');
    $('input[name="distributeObjectThird"]:radio').prop('checked', false);
    $('#modal-distribution-third').modal('show');
});

//第三方重新分配
function _allocateForThird() {
    var taskId = $('#taskId').val();
    var bankTypes = $('#form-field-select-allocateTaskThird').val();
    var remark = $('#allocateTaskRemarkThird').val();
    var distributeObject = '';
    $('input[name="distributeObjectThird"]').each(function () {
        if ($(this).is(":checked")) {
            distributeObject = $(this).val();
        }
    });
    if (!distributeObject) {
        $('#noDistributionObjThird').show(10).delay(1000).hide(10);
        return;
    } else {
        $('#noDistributionObjThird').hide();
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/reallocateTask',
        data: {'taskId': taskId, 'type': distributeObject, "bankType": bankTypes.toString(), "remark": remark},
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
                if (res.status == 1) {
                    $('#modal-distribution-third').modal('hide');
                    _resetThirdOutTemperateData();
                    haveReceivedTaskData = false;
                    $.session.remove("selectThirdAccountFlag");
                    $.session.set('taskIdLastTime', $.session.get('toTransferId'));//记住本次获取的任务,以防止重复获取任务
                    $.session.remove('toTransferId');//删除本次任务
                    _continueTasks();
                }

            }
        }
    });
}

//转帐完成 按钮事件
function _transferDone() {
    var userId = $('#userId').val();
    var taskId = $('#taskId').val();
    var remark = $('#remark').val();
    //判断是否是拆单的三方出款
    if (subOrdersArr && subOrdersArr.length>0){
        if (!finishedOrdersArr||finishedOrdersArr.length==0){
            $('#selectAccoutPrompt').text('先完成拆单出款!').show(10).delay(5000).hide(10);
            return false;
        }
    }
    if (finishedOrdersArr && finishedOrdersArr.length > 0) {
        _executeTransfer(null, userId, taskId, '拆单完成出款');
    } else {
        if (selectThirdAccountFlag == 'true') {
            //第三方出款
            if (!$.trim($('#fromAccountInfoTd').find('h4').text())) {
                $('#selectAccoutPrompt').text('请选择第三方出款账号').show(10).delay(5000).hide(10);
                return false;
            }
            var msg = "确定出款吗?";
            if (thirdAccountOutAmount) {
                if (thirdAccountOutAmount < parseFloat($.trim($('#amount').text()))) {
                    $('#selectAccoutPrompt').text('实际出款金额:' + thirdAccountOutAmount + ',小于出款金额:' + parseFloat($.trim($('#amount').text())) + ',不能完成出款!').show(10).delay(5000).hide(10);
                    return false;
                }
                if (thirdAccountOutAmount < parseFloat($.trim($('#amount').text()))) {
                    msg = "使用第三方金额实际总额:" + thirdAccountOutAmount + ",小于出款金额:" + $.trim($('#amount').text()) + ",请重新填写!";
                    return false;
                }
                if (thirdAccountOutAmount > parseFloat($.trim($('#amount').text()))) {
                    msg = "使用第三方金额实际总额:<span style='color: red;'>" + thirdAccountOutAmount + "</span>,大于出款金额:<span style='color: red;'>" + $.trim($('#amount').text()) + "</span>,是否确定出款?";
                }
            }
            bootbox.confirm('<h4 class="center">' + msg + '</h4>', function (res) {
                if (res) {
                    _executeTransfer(null, userId, taskId, remark);
                }
            });
        } else {
            var fromAccountInfo = $('#fromAccountInfo').html();
            var fromAccountId = $.session.get('accountToDrawId');
            if (!fromAccountInfo || !fromAccountId) {
                $('#selectAccoutPrompt').text('请选择出款账号').show(10).delay(5000).hide(10);
                return false;
            }
            var amountToDraw = $('#amount').text();
            if ($.session.get('accountToLimit') && parseFloat(amountToDraw) > parseFloat($.session.get('accountToLimit'))) {
                $('#selectAccoutPrompt').text('出款金额不能大于当日出款额度').show(10).delay(5000).hide(10);
                return false;
            }
            if ($.session.get('outwardAmountDaily') && (parseFloat(amountToDraw) + parseFloat($.session.get('outwardAmountDaily'))) > parseFloat($.session.get('accountToLimit'))) {
                $('#selectAccoutPrompt').text('已超出当日累计出款，请重新选择账号！').show(10).delay(5000).hide(10);
                return false;
            }
            var flag = _findAccountToDraw($.session.get('accountToDraw'));//防止人工修改数据库余额
            if (flag) {
                return false;
            }
            var accountExceptionFlag = _findAccountInfoById(fromAccountId);
            if (accountExceptionFlag) {
                return;
            }
            if (!accountExceptionFlag) {
                if (accountIdToFinishTaskLastTime != fromAccountId) {
                    bootbox.confirm('<h4 style="color:red;" class="center">出款账号与上次不一致，确定出款吗？</h4>', function (res) {
                        if (res) {
                            _executeTransfer(fromAccountId, userId, taskId, remark);
                            $('#selectAccoutPrompt').text('').hide();
                        }
                    });
                } else {
                    bootbox.confirm('<h4 class="center"> 确定出款吗？</h4>', function (res) {
                        if (res) {
                            _executeTransfer(fromAccountId, userId, taskId, remark);
                        }
                    });
                }
            }
        }
    }

}

//执行转账
function _executeTransfer(fromAccountId, userId, taskId, remark) {
    var thirdRemark = '',feeData='';
    if (selectThirdAccountFlag == 'true') {
        //是否是拆单的
        var split =   finishedOrdersArr&& finishedOrdersArr.length>0 ;
        thirdRemark = split?"拆单完成出款":$.trim($('#fromAccountInfoTd').find('h4').text());
        if (!thirdRemark) {
            return;
        }
        if (split){
            //三方账号id和手续费关系
            $.each(finishedOrdersArr,function (i,item) {
                var thirdId = $('#thirdName_split'+item).val();
                var feeSub = $('#splitFee'+item).val();
                feeData+=thirdId+':'+feeSub+',';
            });
            feeData = feeData.substr(0,feeData.lastIndexOf(','));
        }
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/transfer',
        data: {
            "fromAccountId": fromAccountId,
            "userId": userId,
            "taskId": taskId,
            "remark": remark,
            "third": selectThirdAccountFlag,
            "thirdRemark": thirdRemark,'fee':feeData
        },
        dataType: 'json',
        async: false,
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    _resetThirdOutTemperateData();
                    //出款请求信息
                    $('#orderNoTask').text('');$('#handicapTask').text('');$('#member').text('');$('#receiveTime').text('');
                    $('#userId').val('');$('#taskId').val('');$('#orderNoInput').val('');$('#memberCode').val('');

                    //汇入帐户信息
                    $('#amount').text('');$('#owner').text('');$('#bankName').text('');$('#accountNo').text('');haveReceivedTaskData = false;
                    if ($.session.get("selectThirdAccountFlag")) {
                        $.session.remove("selectThirdAccountFlag");
                    }
                    $.session.set('taskIdLastTime', $.session.get('toTransferId'));$.session.remove('toTransferId');
                    _continueTasks();
                    finishedOrdersArr=[];
                    handicapSelectedMap = new Map(), thirdAccSelected = new Map(), thirdNameSelectedMap = new Map(), thirdNameUrlSelected = new Map();
                }else {
                    showMessageForFail(res.message,5000);return;
                }
            }
            else {
                showMessageForFail(res.message,5000);return;
            }
        },
        complete: function (res) {
            if (res.status == 200) {
                _findAccountInfoById(fromAccountId);
            }
        }
    });
    $('#successFlagBankName').hide();
    $('#successFlagToAccountName').hide();
    $('#successFlagOwner').hide();
    $('#successFlagAccountNo').hide();
    $('#successFlagAmount').hide();
}

var _beforeTurntoFinished = function (id) {
    $('#CustomerserviceRemark1').val('');
    $('#CustomerserviceRemark1_id').val(id);
    $('#CustomerserviceRemark1_modal').modal('show');
    $('#confirmBtnTask').attr('onclick', '_exectuTransferFromFail();');
};

//执行转账
function _exectuTransferFromFail() {
    if (!$('#CustomerserviceRemark1').val()) {
        $('#prompt_remark1').show(10).delay(1000).hide(10);
        return;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/finsh',
        data: {
            "type": "failedOutToMatched",
            "taskId": $('#CustomerserviceRemark1_id').val(),
            "remark": $('#CustomerserviceRemark1').val()
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
            if (res.status == 1) {
                initialSearchType(0);
            }
            $('#confirmBtnTask').attr('onclick', '');
            $('#CustomerserviceRemark1_modal').modal('hide');
        }
    });
}

function _continueTasks() {
    var taskFlag = $.session.get('pauseTFlaggs');
    if (taskFlag == '1') {
        _pauseTasksBtn(taskFlag, false);
    } else {
        _pauseTasksBtn($.session.get('pauseTFlaggs'), false);
    }

    $('#remark').val("");
    if ($('#remark').prop('readonly')) {
        $('#remark').removeAttr('readonly');
    }
    if ($('#remark').prop('style')) {
        $('#remark').removeAttr('style');
    }
}

//其他操作--转主管2 取消3(没有取消功能了)
function _otherOperate(status, id, remark, orderNo, memberCode, type) {
    if (!remark) {
        $('#remarkFailPrompt').show(100).delay(1000).hide(100);
        return false;
    }
    var prompt = '';
    if (status == 2) {
        prompt = '确定转主管吗？';
    }
    if (status == 3) {
        prompt = '确定取消吗？';
    }
    //二次确认
    bootbox.confirm(prompt, function (res) {
        if (res) {
            _executeOperate(id, remark, status, orderNo, memberCode, type);
        }
    })

}

function _executeOperate(id, remark, status, orderNo, memberCode, type) {
    $.ajax({
        type: 'post',
        url: '/r/outtask/status',
        data: {
            "taskId": id,
            "remark": remark,
            "status": status,
            "orderNo": orderNo,
            "memberCode": memberCode,
            "thirdOutFlag": selectThirdAccountFlag,
        },
        dataType: 'json',
        async: false,
        success: function (res) {
            if (res.status == 1) {
                if (type == 'matching') {
                    _resetThirdOutTemperateData();
                    //出款请求信息
                    $('#orderNoTask').text('');
                    $('#handicapTask').text('');
                    $('#member').text('');
                    $('#receiveTime').text('');
                    $('#userId').val('');
                    $('#taskId').val('');
                    $('#orderNoInput').val('');
                    $('#memberCode').val('');

                    //汇入帐户信息
                    $('#amount').text('');
                    $('#owner').text('');
                    $('#bankName').text('');
                    $('#accountNo').text('');
                    haveReceivedTaskData = false;
                    if ($.session.get("selectThirdAccountFlag")) {
                        $.session.remove("selectThirdAccountFlag");
                    }
                    console.log("转主管成功:任务id:" + $.session.get('toTransferId'));
                    $.session.set('taskIdLastTime', $.session.get('toTransferId'));
                    $.session.remove('toTransferId');
                    _continueTasks();
                }
                if (type == 'failure') {
                    initialSearchType(0);
                }
                $.gritter.add({
                    time: 500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });

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
            $('#successFlagBankName').hide();
            $('#successFlagToAccountName').hide();
            $('#successFlagOwner').hide();
            $('#successFlagAccountNo').hide();
            $('#successFlagAmount').hide();
        }
    });
}

//第三方出款完成或者重新分配后 临时数据置空
function _resetThirdOutTemperateData() {
    if (selectThirdAccountFlag && selectThirdAccountFlag == 'true') {
        selectThirdAccountFlag = false;
        selectedCurrentPageThirdAccountArray = [];
        selectedCurrentPageAccountMap = new Map();//动态保存每次选中的账号id map(currentPage,accoutIdArray);
        thirdAccountOutRemarksInfo = [];
        thirdAccountOutAmount = 0;
        addOtherThirdInfoFlag = false;
        mapForSavePageHistorySelectedArray = [];
        currentPageForThirdAccountModal = 1;
        currentPageForThirdAccountData = [];//当前页的数据
        selectedPageNoArray = [];//保存的所有页面标志
    }
}

function _checkClickLabel(obj) {
    var checkedFlag = $(obj).find('input').prop('checked');
    if (checkedFlag) {
        $(obj).find('input').prop('checked', false);
    } else {
        $(obj).find('input').prop('checked', true);
    }
}

function _selectAccountByClickRadio(i, obj) {
    var nodeName = obj.nodeName;
    if (nodeName == 'INPUT') {
        if ($(obj).prop('checked')) {
            $(obj).prop('checked', false);
        } else {
            $(obj).prop('checked', true);
        }
    }
}

function _selectAccountByTR(i, obj) {
    var nodeName = obj.nodeName;
    if (nodeName == 'TR') {
        if ($('input[name=outAccount]').get(i).checked) {
            $('input[name=outAccount]').get(i).checked = false;
        } else {
            $('input[name=outAccount]').get(i).checked = true;
        }
    }
}

// 二次查询出款账号
function _findAccountToDraw(accountToDraw) {
    var flag = false;
    $.ajax({
        type: 'get',
        url: '/r/account/getByAccount',
        data: {"account": accountToDraw},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    if (res.data.status == 11) {
                        if ($.session.get('accountToDrawId') && $.session.get('accountToDrawId') == res.data.id) {
                            $.session.remove('accountToDrawId');
                            $.session.set('accountToDrawId', res.data.id);
                            flag = false;
                        }
                        else {
                            $('#selectAccoutPrompt').text('账户异常重新选择').show(10).delay(3000).hide(10);
                            flag = true;
                        }
                        $.session.set('accountToDraw', res.data.account);
                        $.session.set('accountOutAlias', res.data.alias);
                        //$.session.set('accountToDrawBalance', res.data.balance);
                        $.session.set('accountToDrawBankBalance', res.data.bankBalance ? res.data.bankBalance : "0");
                        $.session.set('outwardAmountDaily', res.data.outwardAmountDaily);
                        $.session.set('accountToLimit', res.data.limitOut);
                        if (!$.session.get('accountToDraw')) {
                            console.log("accountToDraw lost:" + $.session.get('accountToDraw'));
                            return;
                        }
                        var len = $.session.get('accountToDraw').length;
                        var lenSub = 4;
                        if (len < 4) {
                            lenSub = len;
                        }
                        var accountToshow = $.session.get('accountToDraw').substring(0, lenSub) + '***' + $.session.get('accountToDraw').substring(len - lenSub);
                        var fromAccountInfo =
                            '账号：' + accountToshow + ' &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  ' +
                            '编号：<label id="aliasLabel" style="font-size:30 px;color: mediumvioletred;" >' +
                            ($.session.get('accountOutAlias') ? $.session.get('accountOutAlias') : '无') + '</label>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  ' +
                            //'系统余额：' + parseFloat($.session.get('accountToDrawBalance')).toFixed(3) + '&nbsp;&nbsp;&nbsp; ' +
                            '余额：' + _checkObj(parseFloat($.session.get('accountToDrawBankBalance')).toFixed(3));
                        $('#modal-fromAccountInfo').modal('hide');
                    }
                } else {
                    $('#selectAccoutPrompt').text('账户异常重新选择').show(10).delay(3000).hide(10);
                    flag = true;
                }
            }
        }
    });
    return flag;
}

//防止修改数据库账户余额后数据不一致
function _checkAccountToDrawInSession() {
    if ($.session.get('accountToDraw')) {
        var accountToDraw = $.session.get('accountToDraw');
        _findAccountToDraw(accountToDraw);
    }
}

/**
 * 出款完成 实时查询该账号当日累计出款 以及账号其他信息 防止和数据库信息不一致
 * @param outAccountId
 * @private
 */
function _findAccountInfoById(outAccountId) {
    var flag = false;
    if (outAccountId) {
        $.ajax({
            type: 'get',
            url: '/r/account/findById',
            data: {"id": outAccountId},
            dataType: 'json',
            async: false,
            success: function (res) {
                if (res && res.status == 1 && res.data) {
                    if (res.data.status == accountStatusNormal) {
                        accountIdToFinishTaskLastTime = $.session.get('accountToDrawId');//保存上一次出款的账号
                        if ($.session.get('accountToDrawId') && outAccountId != $.session.get('accountToDrawId')) {
                            $('#accountChangeRemind').text('当前出款账号(编号):' + res.data.alias + '与上次:' + $.session.get('accountOutAlias') + '不一致，完成出款前请核实!!!').show();
                            $('#taskStopRemind').hide();
                            $('#selectAccoutPrompt').text('出款账户变更').show();
                            flag = false;
                        } else {
                            $('#accountChangeRemind').text('').hide();
                            $('#taskStopRemind').show();
                            $('#selectAccoutPrompt').text('').hide();
                        }
                        $.session.set('accountToDrawId', res.data.id);
                        $.session.set('accountToDraw', res.data.account);
                        $.session.set('accountOutAlias', res.data.alias);

                        //$.session.set('accountToDrawBalance', res.data.balance);
                        $.session.set('accountToDrawBankBalance', _checkObj(res.data.bankBalance) ? res.data.bankBalance : '0');

                        $.session.set('accountToLimit', res.data.limitOut);
                        $.session.set('outwardAmountDaily', res.data.outwardAmountDaily);
                        if (!$.session.get('accountToDraw')) {
                            console.log("accountToDraw lost:" + $.session.get('accountToDraw'));
                            return;
                        }
                        var len = $.session.get('accountToDraw').length;
                        var lenSub = 4;
                        if (len < 4) {
                            lenSub = len;
                        }
                        var accountToshow = $.session.get('accountToDraw').substring(0, lenSub) + '***' + $.session.get('accountToDraw').substring(len - lenSub);
                        var fromAccountInfo =
                            '账号：' + accountToshow + ' &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  ' +
                            '编号：<label id="aliasLabel" style="font-size:30 px;color: mediumvioletred;" >' +
                            ($.session.get('accountOutAlias') ? $.session.get('accountOutAlias') : '无') + '</label>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  ' +
                            //'系统余额：' + parseFloat($.session.get('accountToDrawBalance')).toFixed(3) + '&nbsp;&nbsp;&nbsp; ' +
                            '余额：' + _checkObj(parseFloat($.session.get('accountToDrawBankBalance')).toFixed(3));
                        $('#fromAccountInfo').empty().html(fromAccountInfo);
                    }
                    else {
                        if (!selectThirdAccountFlag || selectThirdAccountFlag == 'false') {
                            //非第三方出款的
                            var msg = '账户异常重新选择';
                            if (accountStatusFreeze == res.data.status) {
                                msg = '此次出款账号:' + res.data.alias + '  已被冻结!';
                            }
                            if (accountStatusDelete == res.data.status) {
                                msg = '此次出款账号:' + res.data.alias + '  已被删除';
                            }
                            if (accountStatusStopTemp == res.data.status) {
                                msg = '此次出款账号:' + res.data.alias + '  已被暂停';
                            }
                            if (accountStatusEnabled == res.data.status) {
                                msg = '此次出款账号:' + res.data.alias + '  处于可用状态';
                            }
                            $('#selectAccoutPrompt').text(msg).show(10).delay(5000).hide(10);
                            $.gritter.add({
                                time: 1000000,
                                class_name: '',
                                title: '系统消息',
                                text: '您尚未分配到用于此次出款任务的出款卡(编号): <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' + res.data.alias + '  <br>请先分配！',
                                sticky: false,
                                image: '../images/message.png'
                            });
                            flag = true;
                            $.session.remove('accountToDrawId');
                            $.session.remove('accountToDraw');
                            $.session.remove('accountOutAlias');
                            $.session.remove('accountToDrawBalance');
                            $.session.remove('accountToDrawBankBalance');
                            $.session.remove('accountToLimit');
                            $.session.remove('outwardAmountDaily');
                            $('#fromAccountInfo').empty().html('');
                        }
                    }
                }

            }
        });
    }
    return flag;
}

$('#transferDone').click(function () {
    _transferDone();
});
$('#transfer_manager').click(function () {
    if ($('#remark').attr('readonly')) {
        $('#remark').val('');
        $('#remark').removeAttr('readonly');
    }
    var taskId = $('#taskId').val();
    var remark = $('#remark').val();
    var orderNoInput = $('#orderNoInput').val();
    var memberCode = $('#memberCode').val();
    _otherOperate(2, taskId, remark, orderNoInput, memberCode, 'matching');
});

$('#transfer_platform').click(function () {
    if ($('#remark').attr('readonly')) {
        $('#remark').val('');
        $('#remark').removeAttr('readonly');
    }
    var taskId = $('#taskId').val();
    var remark = $('#remark').val();
    var orderNoInput = $('#orderNoInput').val();
    var memberCode = $('#memberCode').val();
    _otherOperate(3, taskId, remark, orderNoInput, memberCode, 'matching');

});

/**
 * 获取盘口信息
 */
function _gethandicap(type) {
    var opt = '<option>请选择</option>';
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
                }
            }
            $('#handicap_' + type).empty().html(opt);
            $('#handicap_' + type).trigger("chosen:updated");
            $('#level_' + type).empty().html('<option>请选择</option>');
            $('#level_' + type).trigger('chosen:updated');
        }
    });
}

/**
 * 根据盘口初始化层级
 */
$('#handicap_1').change(function () {
    if ($('#handicap_1').val() != '请选择') {
        _getLevel(1);
    }
    else {
        $('#level_' + 1).empty().html('<option>请选择</option>');
        $('#level_' + 1).trigger('chosen:updated');
    }
});
$('#handicap_0').change(function () {
    if ($('#handicap_0').val() != '请选择') {
        _getLevel(0);
    }
    else {
        $('#level_' + 0).empty().html('<option>请选择</option>');
        $('#level_' + 0).trigger('chosen:updated');
    }
});

function _getLevel(type) {
    $('#level_' + type).empty();
    if ($('#handicap_' + type).val() != '请选择') {
        var handicap = $('#handicap_' + type).val();
        $.ajax({
            type: 'get',
            url: '/r/level/getByHandicap',
            data: {"handicap": handicap},
            dataType: 'json',
            success: function (res) {
                if (res) {
                    var opt = '<option>请选择</option>';
                    if (res.status == 1 && res.data) {
                        $(res.data).each(function (i, val) {
                            opt += '<option value="' + $.trim(val) + '">' + val + '</option>';
                        });
                    }
                    $('#level_' + type).empty().html(opt);
                    $('#level_' + type).trigger('chosen:updated');
                }
            }
        });
    }
}

function _initalAccountOutTask(type) {
    $.ajax({
        type: 'get',
        url: '/r/account/getAllOutAccount',
        data: {},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                var opt = '<option>请选择</option>';
                if (res.status == 1 && res.data) {
                    $(res.data).each(function (i, val) {
                        if (val.type == 5) {//出款账号
                            opt += '<option value="' + val.alias + '" >' + val.alias + '</option>';
                        }
                    });
                }
                $('#dataList_account_' + type).empty().html(opt);
                //$('#dataList_account_' + type).trigger('chosen:updated');
            }
        }
    });
}

function initialSearchType(type) {
    outTaskSearchType = type;
    _getPathAndHtml('taskOrderNoDetail');
    _datePickerForAll($("input.date-range-picker"));
    _initialSelectChosen(type);
    searchExecute();
}

/**
 * 查询 type 1 已完成 0 失败(未出款)
 */
function searchExecute() {
    var type = outTaskSearchType;
    //备注
    var Noteflag = false, FinishTask = false, Notify = false, flag = false, reCreateTaskFlag = false;
    $.each(ContentRight['OutwardTask:*'], function (name, value) {
        if (name == 'OutwardTask:ToFailure:*') {
            flag = true;
        }
        if (name == 'OutwardTask:TurnHead:*') {
            $('#transfer_manager').show();
        }
        if (name == 'OutwardTask:NewGenerationTask:*') {
            reCreateTaskFlag = true;
        }
        if (name == 'OutwardTask:AddRemark:*') {
            Noteflag = true;
        }
        if (name == 'OutwardTask:CompleteTask:*') {
            FinishTask = true;
        }
        if (name == 'OutwardTask:NotifyPlatForm:*') {
            Notify = true;
        }
    });
    if ($.session.get('accountToDrawId')) {
        _findAccountInfoById($.session.get('accountToDrawId'));
    }
    var handicap = $('#handicap_' + type+' option:checked').attr('handicap_code');
    //task表保存的是盘口code
    //var handId = $('#handicap_' + type).val();
    if (!handicap || (handicap.indexOf('请选择') >= 0)) {
       handicap = '';
        //handicap = $('#handicap_toOutDraw option[value="' + handId + '"]').attr('handicap_code');
    }
    var levelId = $('#level_' + type).val();
   // var level = $('#level_' + type).val();
    if (!levelId || (levelId.indexOf('请选择') >= 0)) {
        levelId = '';
        //levelId = $('#level_failedOutDealed option[value="' + level + '"]').text();
    }
    var orderNoV = $('#orderNo_' + type).val();
    var orderNo = '';
    if (orderNoV) {
        orderNo = orderNoV;
    }
    var memberV = $('#member_' + type).val();
    var member = '';
    if (memberV) {
        member = memberV;
    }
    var accountAlias = '';
    if ($('#account_' + type).val() && $('#account_' + type).val().indexOf('请选择') < 0) {
        accountAlias = $('#account_' + type).val();
    }
    var startAndEnd = $('#timeScopeTask_' + type).val();
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
    var fromMoney = '';
    var toMoney = '';
    if ($('#fromMoney_' + type).val()) {
        fromMoney = $('#fromMoney_' + type).val();
    }
    if ($('#toMoney_' + type).val()) {
        toMoney = $('#toMoney_' + type).val();
    }
    var CurPage = $("#doneOutward_footPage_" + type).find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var sysLevel = $('input[name="sysLevel_' + type + '"]:checked').val();
    sysLevel = !sysLevel || sysLevel == 3 ? "" : sysLevel;
    var data =
        {
            "handicapId": handicap, "levelId": levelId, "member": member, "orderNo": orderNo,
            "accountAlias": accountAlias, "fromMoney": fromMoney, "toMoney": toMoney,
            "startTime": startTime, "endTime": endTime, "type": type, "sysLevel": sysLevel,
            "pageNo": CurPage, "pageSize": $.session.get('initPageSize')
        };
    $.ajax({
        type: 'get',
        url: '/r/outtask/task',
        data: data,
        dataType: 'json',
        success: function (res) {
            var idListThird = [], idList = [];
            if (res) {
                if (res.status == 1 && res.data) {
                    var tr = '', trs = '', mount = 0;
                    $(res.data).each(function (i, val) {
                        var third = val.thirdInsteadPay && val.accountId;
                        if (third) {
                            idListThird.push(val.accountId);
                        } else {
                            idList.push({'id': val.accountId});
                        }
                        tr +=
                            '<tr>' +
                            '<td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                            '<td>' + _checkObj(val.level) + '</td>' +
                            '<td>' + _checkObj(val.member) + '</td>';
                        if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                            if (!isHideAccount)
                                tr += '<td id="td_flag' + i + '"><a href="javascript:_showTaskOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                            else tr += '<td id="td_flag' + i + '">' + _checkObj(val.orderNo) + '</td>';
                        } else {
                            tr += '<td id="td_flag' + i + '">' + _checkObj(val.orderNo) + '</td>';

                        }
                        if (third) {
                            if (_checkObj(val.account).length > 4) {
                                tr += "<td>" +
                                    "<a class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.accountId + "' data-placement='auto right' data-trigger='hover'  >" + _checkObj(val.account).substring(0, 4) + "***" + _checkObj(val.account).substring(_checkObj(val.account).length - 4, _checkObj(val.account).length) +
                                    "</a>" +
                                    "</td>";
                            } else {
                                tr += "<td>" +
                                    "<a class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.accountId + "' data-placement='auto right' data-trigger='hover'  >" + _checkObj(val.account).substring(0, 4) +
                                    "</a>" +
                                    "</td>";
                            }
                        } else {
                            if (_checkObj(val.account).length > 4) {
                                tr += "<td>" +
                                    "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto right' data-trigger='hover'  >" + _checkObj(val.account).substring(0, 4) + "***" + _checkObj(val.account).substring(_checkObj(val.account).length - 4, _checkObj(val.account).length) +
                                    "</a>" +
                                    "</td>";
                            } else {
                                tr += "<td>" +
                                    "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto right' data-trigger='hover'  >" + _checkObj(val.account).substring(0, 4) +
                                    "</a>" +
                                    "</td>";
                            }
                        }

                        tr += '<td>' + _checkObj(val.amount) + '</td>' +
                            '<td>' + _showTaskStatu(val.taskStatus) + '</td>' +
                            '<td>' + _show(val.asignTime) + '</td>';
                        if (type == 1) {
                            tr += '<td>' + _show(val.timeconsuming) + '</td>';
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td><a class="bind_hover_card breakByWord "  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark + '">'
                                        + _checkObj(val.remark).substring(0, 5) + "..."
                                        + '</a></td>';
                                } else {
                                    tr += '<td><a class="bind_hover_card breakByWord "  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark + '">'
                                        + _checkObj(val.remark)
                                        + '</a></td>';
                                }
                            } else {
                                tr += '<td></td>';
                            }
                            if (val.requestStatus != 5 || (val.requestStatus == 6 && val.taskStatus != 5)) {
                                tr += '<td style="width: 210px;text-align: center">';
                            } else {
                                tr += '<td style="width: 130px;text-align: center">';
                            }
                            if (flag && val.remark.indexOf('三方') < -1) {
                                tr += '<button onclick="_turnToFail(' + val.id + ');" type="button" class=" btn btn-xs btn-white btn-danger btn-bold">' +
                                    '<i class="ace-icon fa fa-share  bigger-100 red"></i>转待排查</button>';
                            }
                            if (Notify && val.requestStatus == 6 && val.remark.indexOf('三方') < -1) {
                                tr +=
                                    '<button onclick="_noticePlatform(' + val.id + ');"  type="button"  class=" btn btn-xs btn-white btn-info btn-bold">' +
                                    '<i class="ace-icon fa  fa-envelope-open-o  bigger-100 green"></i>通知平台</button>';
                            }
                            if (Noteflag) {
                                tr += '<button type="button"  onclick="SCustomerserviceRemark1(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                                    '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                            }
                            tr += '</td>';
                        }
                        if (type == 0) {
                            if (_checkObj(val.remark)) {
                                if (_checkObj(val.remark).length > 5) {
                                    tr += '<td><a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + val.remark + '">'
                                        + _checkObj(val.remark).substring(0, 5) + "..."
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
                            tr += '<td style="width: 210px;text-align: center">';
                            if (reCreateTaskFlag && val.taskStatus == 6 && val.taskStatus != 5 && val.remark.indexOf('三方') < -1) {
                                tr +=
                                    '<button onclick="_recreateTask(' + val.id + ');" type="button"  class="btn btn-xs btn-white btn-info btn-bold">' +
                                    '<i class="ace-icon fa fa-reply  bigger-100 green"></i>重新生成任务</button>';
                            }
                            if (Noteflag) {
                                tr += '<button type="button" onclick="SCustomerserviceRemark1(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                                    '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                            }
                            if (FinishTask && val.remark.indexOf('三方') < -1) {
                                tr += '<button type="button" style="margin-right:5px;" onclick="_beforeTurntoFinished(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                                    '<i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                            }
                            tr += '</td>';
                        }
                        tr += '</tr>';
                        mount += val.amount;
                    });
                    if (outwardCurrentPageSum) {
                        trs += '<tr><td id="outwardTaskCurrentCount' + type + '" colspan="5">小计：统计中..</td>' +
                            '<td bgcolor="#579EC8" style="color:white;width: 130px;text-align: left;">' + parseFloat(mount).toFixed(3) + '</td>' +
                            '<td colspan="10"></td>' +
                            '</tr>';
                    } else {
                        trs += '<tr><td id="outwardTaskCurrentCount' + type + '" colspan="15">小计：统计中..</td></tr>';
                    }
                    if (outwardAllRecordSum) {
                        trs += '<tr>' +
                            '<td id="outwardTaskAllCount' + type + '" colspan="5">总共：统计中..</td>' +
                            '<td id="outwardTaskSum' + type + '" bgcolor="#D6487E" style="color:white;width: 130px;text-align: left;display: none;">统计中..</td>' +
                            '<td colspan="10"></td>' +
                            '</tr>';
                    } else {
                        trs += '<tr>' +
                            '<td id="outwardTaskAllCount' + type + '" colspan="15">总共：统计中..</td></tr>';
                    }
                }
                $('#tbody_' + type).empty().html(tr).append(trs);
                //$('#tbody_' + type + ' th').addClass('no-padding-right-td');
                //$('#tbody_' + type + ' td').addClass('no-padding-right-td');
                showPading(res.page, 'doneOutward_footPage_' + type, searchExecute);
                _getOutwardTaskCount(data, type);
                if (outwardAllRecordSum) {
                    _getOutwardTaskSum(data, type);
                }
                //加载账号悬浮提示
                loadHover_accountInfoHover(idList);
                loadHover_thirdDaiFuHover(idListThird);
                if (type == 1) {
                    if ($('#promptMessage')) {
                        $('#promptMessage').remove();
                    }
                    $('#doneOut').append('<div id="promptMessage"><span style="color: mediumvioletred;font-size: 15px">温馨提示：如果"操作"栏有"通知平台按钮，说明此订单出款成功但通知平台失败！您可以继续通知，当通知成功该按钮消失。</span></div>');
                }
                $("[data-toggle='popover']").popover();
            }
        }
    });
}

function SCustomerserviceRemark1(id) {
    $('#CustomerserviceRemark1').val('');
    $('#CustomerserviceRemark1_id').val(id);
    $('#CustomerserviceRemark1_modal').modal('show');
    $('#confirmBtnTask').attr('onclick', 'save_CustomerserviceRemark1();');
}

function save_CustomerserviceRemark1() {
    var remark = $.trim($('#CustomerserviceRemark1').val());
    if (!remark) {
        $('#prompt_remark1').show(10).delay(1000).hide(10);
        return;
    }
    var taskId = $('#CustomerserviceRemark1_id').val();
    $.ajax({
        type: 'post',
        url: '/r/outtask/remark',
        data: {"taskId": taskId, "remark": remark},
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
            //隐藏
            $('#CustomerserviceRemark1_modal').modal('hide');
            //成功后把备注清空
            $('#CustomerserviceRemark1').val("");
            //刷新
            initialSearchType(outTaskSearchType);
            $('#confirmBtnTask').attr('onclick', '');
        }
    });
}

function _taskPhoto(url) {
    $('#taskImg').attr('src', url);
    $('#taskImgModal').modal('show');
}

function _getOutwardTaskSum(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/outtask/getOutwardTaskSum',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#outwardTaskSum' + type).text(parseFloat(res.data.sumAmount).toFixed(3));
                    if (type == 1) {
                        $.each(ContentRight['OutwardTask:*'], function (name, value) {
                            if (name == 'OutwardTask:lookUpFinishedAmount:*') {
                                lookUpFinishedAmount = true;
                            }
                        });
                        if (!lookUpFinishedAmount) {
                            $('#outwardTaskSum' + type).text('无权限查看');
                        }
                    }
                    $('#outwardTaskSum' + type).show();
                }
            }
        }
    });
}

function _getOutwardTaskCount(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/outtask/getOutwardTaskCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#outwardTaskCurrentCount' + type).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1))) + '条记录');
                    $('#outwardTaskAllCount' + type).empty().text('合计：' + res.page.totalElements + '条记录');
                    showPading(res.page, 'doneOutward_footPage_' + type, searchExecute);
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
function _showTaskOrderDetail(id, orderNo) {
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

function _showTaskStatu(status) {
    var desc = "";
    if (status) {
        switch (status) {
            case 1 :
                desc = "已出款未匹配流水";
                break;
            case 5 :
                desc = "已匹配流水";
                break;
            case 6 :
                desc = "待排查";
                break;
        }
    }
    return desc;
}

/**转失败任务*/
function _turnToFail(id) {
    $('#turnFailId').val(id);
    $('#turnToFailRemark').val('');
    $('#modal-turnToFail').modal('show');
    $('#confirmTurnToFail').attr('onclick', '_confirmTurnToFail();');
}

function _confirmTurnToFail() {
    if (!$.trim($('#turnToFailRemark').val())) {
        $('#remark-turnToFail').show(10).delay(1000).hide(10);
        return false;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/turnToFail',
        data: {"taskId": $('#turnFailId').val(), "remark": $('#turnToFailRemark').val()},
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
                initialSearchType(1);
                $('#modal-turnToFail').modal('hide');
            }
        }
    })
}

/**通知平台*/
function _noticePlatform(id) {
    if (id) {
        bootbox.confirm('确定通知平台吗？', function (res) {
            if (res) {
                $.ajax({
                    type: 'post',
                    url: '/r/outtask/noticePlatForm',
                    data: {"taskId": id},
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
                            initialSearchType(1);
                        }
                    }
                });
            }
        })
    }
}

/**重新生成任务*/
function _recreateTask(id) {
    $('#turnFailId').val(id);
    $('#turnToFailRemark').val('');
    $('#modal-turnToFail').modal('show');
    $('#confirmTurnToFail').attr('onclick', '_confirmGener();');
}

function _confirmGener() {
    if (!$.trim($('#turnToFailRemark').val())) {
        $('#remark-turnToFail').show(10).delay(1000).hide(10);
        return false;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/recreate',
        data: {'taskId': $('#turnFailId').val(), "remark": $.trim($('#turnToFailRemark').val())},
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
                initialSearchType(0);
                $('#modal-turnToFail').modal('hide');
            }
        }
    });
}

/**
 * 重置
 * @param type
 * @private
 */
function _resetValueB(type) {
    initPaging($('#doneOutward_footPage_' + type), pageInitial);
    // var handId = $('#handicap_' + type).val();
    // if (handId && handId.indexOf('请选择') < 0) {
    //     _gethandicap(type);
    // }
    // var level = $('#level_' + type).val();
    // if (level) {
    //     $('#handicap_' + type).change();
    // }
    // $('#account_' + type).val('');
    // _initalAccountOutTask(type);
    _initialSelectChosen(type);
    var orderNoV = $('#orderNo_' + type).val();
    if (orderNoV) {
        $('#orderNo_' + type).val('');
    }
    var memberV = $('#member_' + type).val();
    if (memberV) {
        $('#member_' + type).val('');
    }
    var amountV = $('#amount_' + type).val();
    if (amountV) {
        $('#amount_' + type).val('');
    }
    _datePickerForAll($("input.date-range-picker"));
    if ($('#fromMoney_' + type).val()) {
        $('#fromMoney_' + type).val('');
    }
    if ($('#toMoney_' + type).val()) {
        $('#toMoney_' + type).val('');
    }
    var operatorNameV = $('#operator_' + type).val();
    if (operatorNameV) {
        $('#operator_' + type).val('');
    }
    if ($('#checkbox_robot_' + type).prop('checked')) {
        $('#checkbox_robot_' + type).prop('checked', false);
    }
    if ($('#checkbox_manual_' + type).prop('checked')) {
        $('#checkbox_manual_' + type).prop('checked', false);
    }
    searchExecute();
}

/**
 * 失败任务  反馈 转平台 主管处理操作之前
 * @param id
 * @private
 */
function _beforeFeedBack(status, id) {
    $('#idFeedBack').val(id);
    $('#statusFeedBack').val(status);
    $('#modal-remarkFeedBack').modal('show');
    $.ajax({
        type: 'get',
        url: '/r/outtask/findById',
        data: {"id": id},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $('#orderNoFeedBack').val(res.data.orderNo);
                    $('#memberCodeFeedBack').val(res.data.memberCode);
                }
                else {
                    $.gritter.add({
                        time: 500,
                        class_name: '',
                        title: '系统消息',
                        text: '该出款任务已变更',
                        sticky: false,
                        image: '../images/message.png'
                    });
                }
            }
        }
    });
}

$('#confirmFeedBack').unbind().bind('click', function () {
    var id = $('#idFeedBack').val();
    var status = $('#statusFeedBack').val();
    _feedBack(status, id);
    $('#remarkFeedBack').val('');
    $('#modal-remarkFeedBack').modal('hide');
});
$('#cancelFeedBack').unbind().bind('click', function () {
    $('#remarkFeedBack').val('');
    $('#modal-remarkFeedBack').modal('hide');
});

/**
 * 反馈 转平台 modal  确定操作
 * @param status
 * @param id
 * @returns {boolean}
 * @private
 */
function _feedBack(status, id) {
    if (!$('#remarkFeedBack').val()) {
        $('#remark-feedback').show(10).delay(1000).hide(10);
        return false;
    }
    var orderNo = $('#orderNoFeedBack').val();
    var memberCode = $('#memberCodeFeedBack').val();
    var remark = $('#remarkFeedBack').val();
    _otherOperate(status, id, remark, orderNo, memberCode, "failure");
}

$('#search_0').click(function () {
    searchExecute();
});
$('#search_1').click(function () {
    searchExecute();
});

function _hide() {
    var userId = getCookie('JUID');
    if (false) {// userId && userId =='admin'
        $('#transfer_manager').remove();
    }
}

function _show(obj) {
    var ob = '';
    if (obj)
        ob = obj;
    return ob;
}

function _initialSelectChosen(type) {
    //可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        //allow_single_deselect:true,
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    //_gethandicap(type);

    if (handicapAndLevelInitialOptions) {
        $('#handicap_' + type).empty().html(handicapAndLevelInitialOptions[0]);
        $('#handicap_' + type).trigger('chosen:updated');
        $('#level_' + type).empty().html(handicapAndLevelInitialOptions[1]);
        $('#level_' + type).trigger('chosen:updated');
    }
    $('#handicap_' + type + '_chosen').prop('style', 'width: 70%;');
    $('#level_' + type + '_chosen').prop('style', 'width: 70%;');
    //_initalAccountOutTask(type);

    // $('#handicap_'+type+'_chosen').prop('style', 'width: 78%;');
    // $('#handicap_'+type+'_chosen').prop('style', 'width: 78%;');
    // $('#level_'+type+'_chosen').prop('style', 'width: 78%;');
    // $('#level_'+type+'_chosen').prop('style', 'width: 78%;');
    // $('#account_'+type+'_chosen').prop('style', 'width: 67%;');
    // $('#account_'+type+'_chosen').prop('style', 'width: 67%;');
}

function clearNoNum(obj) {
    //先把非数字的都替换掉，除了数字和.
    obj.value = obj.value.replace(/[^\d.]/g, "");
    //必须保证第一个为数字而不是.
    obj.value = obj.value.replace(/^\./g, "");
    //保证只有出现一个.而没有多个.
    obj.value = obj.value.replace(/\.{2,}/g, ".");
    //保证.只出现一次，而不能出现两次以上
    obj.value = obj.value.replace(".", "$#$").replace(/\./g, "").replace("$#$", ".");
    obj.value = obj.value.replace(/^(\-)*(\d+)\.(\d\d).*$/, '$1$2.$3');//只能输入两个小数
    if (obj.value.indexOf(".") < 0 && obj.value != "") {//以上已经过滤，此处控制的是如果没有小数点，首位不能为类似于 01、02的金额
        obj.value = parseFloat(obj.value);
    }
}

/**
 * 机器出款
 */
function _machine() {
    $('#machine_tbody').empty();
    $.ajax({
        type: 'get',
        url: '/r/outtask/machine',
        data: {},
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data && res.data.length > 0) {
                    var tr = '';
                    var balance = 0;
                    var count = 0;
                    $(res.data).each(function (i, val) {
                        tr += '<tr><td>' + val.robotNo + '</td><td>' + val.bankType + val.bankName + '</td><td>'
                            + val.balance + '</td><td>' + val.outCount + '</td><td><span class="label label-sm label-success">' + val.status + '</span></td><td>'
                            + '订单号：' + val.orderNo + '，盘口：' + val.handicap + '，会员名：' + val.member + '</td></tr>';

                        balance += val.balance;
                        count += val.outCount;
                    });
                    $('#machine_tbody').empty().html(tr);
                    var trs = '<tr><td colspan="2">总共：' + res.data.length + ' 个机器人</td><td bgcolor="#D6487E" style="color:white;">合计：' + parseFloat(balance).toFixed(3) +
                        '  元</td><td bgcolor="#579EC8" style="color:white;">合计：' + count + '   笔</td><td colspan="5"></td></tr>';
                    $('#machine_tbody').append(trs);
                }
                else {
                    $('#machine_tbody').empty().html('<tr><td style="text-align: center" colspan="7"><h3>无机器出款记录</h3></td></tr>');
                }
            }
        }
    });
}

//获取转出账号信息(后台查询当前人的 出款账号或者入款第三方账号)
function _getFromAccountInfo() {
    if (selectThirdAccountFlag == 'true') {
        $('#notThridOutAccountDiv').hide();
        $('#thirdOutAccountDiv').show();
        $('#confirmAccoutToDraw').show();
        $('#useOtherThirdAccountSpan').show();
    } else {
        $('#modal-fromAccountInfo-tbody').empty();
        $('#notThridOutAccountDiv').show();
        $('#thirdOutAccountDiv').hide();
        $('#confirmAccoutToDraw').hide();
        $('#out-account-prompt').hide();
        $('#useOtherThirdAccountSpan').hide();
    }
    var accountNo = $('#accountToDraw').val();
    var CurPage = '';
    if (selectThirdAccountFlag == 'true') {
        $('#fromAccountInfoTd').text('');
        CurPage = $("#modal-thirdAccountInfo-foot").find(".Current_Page").text();
        if (!CurPage) {
            CurPage = 0;
        } else {
            CurPage = CurPage - 1;
        }
        if (CurPage < 0) {
            CurPage = 0;
        }
        var first_click_flag = $('#selectAccount').attr('first_click_flag');
        if (first_click_flag == '2') {
            //1 首次点击选择出款账号按钮 2 不是首次
            //为了分页查询时候记录某一行选中之后如果次行的实际出款金额输入框有输入实际金额 toPayAccountUseAmount 则先保存
            _saveCurrentPageData();
        }
    } else {
        CurPage = $("#modal-fromAccountInfo-foot").find(".Current_Page").text();
        if (!CurPage) {
            CurPage = 0;
        } else {
            CurPage = CurPage - 1;
        }
        if (CurPage < 0) {
            CurPage = 0;
        }
    }
    var data = {"pageNo": CurPage, "pageSize": $.session.get('initPageSize'), "accountNo": accountNo};
    $.ajax({
        type: 'get',
        url: '/r/account/findAllocateAccount4OutwardAsign',
        data: data,
        dataType: 'json',
        async: false,
        success: function (res) {
            if (res) {
                if (!selectThirdAccountFlag || selectThirdAccountFlag == 'false') {
                    //非第三方出款
                    _renderNotThirdPartyOutAccountPage(res, data);
                } else {
                    if (first_click_flag == '1') {
                        $('#selectAccount').attr('first_click_flag', '2');
                    }
                    selectedCurrentPageAccountMap = new Map();
                    _renderOutAccountCount(data, 'third');
                    _renderThirdPartyOutAccountPage(res, data);
                }
            }
        }
    });
}

//第三方出款查询账号模态框数据渲染
function _renderThirdPartyOutAccountPage(res) {
    $('#selectedAllThirdAccount').prop('checked', '');
    $('#modal-thirdAccountInfo-tbody').empty();
    if (res.data && res.data.length > 0) {
        var tr = '', idListThird = [], idList = [];
        $(res.data).each(function (i, val) {
            var third = val.thirdInsteadPay && val.accountId;
            if (third) {
                idListThird.push(val.accountId);
            } else {
                idList.push({'id': val.id, 'type': 'third'});
            }
            tr += '<tr style="cursor:pointer;" onclick="_selectedTrChecked(this);" ><td ><input type="checkbox" onclick="_selectInputCheckeboxInsideTr(event,this);"  name="toPayAccountId" id="toPayAccountId' + val.id + '" value="' + val.id + '" click_value=""></td>' +
                '<td id="accountNoToPayHandicap' + val.id + '">' + _checkObj(val.handicapName) + '</td>' +
                '<td>' + _checkObj(val.currSysLevelName) + '</td>' +
                '<td id="accountNoToPayAlias">' + _checkObj(val.alias) + '</td>' +
                '<td id="accountNoToPayName' + val.id + '">' + _checkObj(val.bankName) + '</td>' +
                "<td>";
            if (third) {
                tr += "<a class='bind_hover_card breakByWord' data-toggle='thirdDaiFuHover" + val.id + "' data-placement='auto right' data-trigger='hover'  >" + hideAccountAll(val.account) +
                    "</a>";
            } else {

                tr += "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.id + "' data-placement='auto right' data-trigger='hover'  >" + hideAccountAll(val.account) +
                    "</a>";
            }
            tr += "</td>" +
                '<td id="bankBalanceAccountNoToPay' + val.id + '">' + (val.bankBalance ? val.bankBalance.toFixed(3) : "") + '</td>' +
                '<td ><input style="width: auto" onclick="disabledEventPropagation(event);" onkeyup="clearNoNum(this)" name="toPayAccountUseAmount" id="toPayAccountUseAmount' + val.id + '" ><span>元</span></td>' +
                '</tr>';
            //idList.push({'id': val.id, 'type': 'third'});

            // if (selectedCurrentPageThirdAccountArray && selectedCurrentPageThirdAccountArray.length > 0) {
            //     if (selectedCurrentPageThirdAccountArray.indexOf(val.id.toString()) > -1) {
            //         checkedAccountIdList.push(val.id);
            //     }
            // }
        });
        $('#modal-thirdAccountInfo-tbody').empty().html(tr);
        var first_click_flag = $('#selectAccount').attr('first_click_flag');
        var mapForSavePageHistorySelectedArrayLength = mapForSavePageHistorySelectedArray.length;
        var checkedAccountIdsAndAmountsMapArray = [];//array [[],[],[]]
        if (mapForSavePageHistorySelectedArrayLength > 0 && first_click_flag == '2') {
            var breakFlag = false;
            for (var i = 0; i < mapForSavePageHistorySelectedArrayLength; i++) {
                var mapForSavePageHistorySelected = mapForSavePageHistorySelectedArray[i];//list->map
                mapForSavePageHistorySelected.forEach(function (value, key) {
                    if (key == currentPageForThirdAccountModal) {
                        checkedAccountIdsAndAmountsMapArray = value;//map->array [[],[],[]]
                        breakFlag = true;
                    }
                });

                if (breakFlag) {
                    break;
                }
            }
            if (checkedAccountIdsAndAmountsMapArray.length > 0) {
                for (var j = 0; j < checkedAccountIdsAndAmountsMapArray.length; j++) {
                    var record = checkedAccountIdsAndAmountsMapArray[j];//[id,amount,name]
                    if (record.length > 0) {
                        $('#toPayAccountId' + record[3]).prop('checked', 'checked');
                        $('#toPayAccountUseAmount' + record[3]).val(record[2]);
                    }
                }
            }
        }
        //加载账号悬浮提示
        loadHover_accountInfoHover(idList);
        loadHover_thirdDaiFuHover(idListThird);
    }
    showPading(res.page, 'modal-thirdAccountInfo-foot', _getFromAccountInfo);
    $('#modal-thirdAccountInfo-foot').find('span.showTotal').show();
}

//非第三方出款查询账号模态框数据渲染
function _renderNotThirdPartyOutAccountPage(res, param) {
    $('#modal-fromAccountInfo-tbody').empty();
    if (res.data && res.data.length > 0) {
        var tr = '';
        $(res.data).each(function (i, val) {
            if ($.session.get('accountToDrawId')) {
                if ($.session.get('accountToDrawId') == val.id) {
                    //出款卡 选中由出款任务带来的出款账号id指定 不管余额多少 出不了就转主管
                    //input -->_selectAccountByClickRadio('+i+','+'this); tr--> cursor:pointer; onclick="_selectAccountByTR('+i+','+'this);"
                    tr += '<tr style="background-color: lawngreen;" ><td ><input type="radio" onclick="disabledEventPropagation(event);" checked="checked"  name="outAccount" value="' + val.id + '"></td>';
                }
                else {
                    tr += '<tr style="cursor:pointer;" ><td ><input type="radio" onclick="disabledEventPropagation(event);" name="outAccount" value="' + val.id + '"></td>';
                }
            } else {
                tr += '<tr style="cursor:pointer;" ><td ><input type="radio" onclick="disabledEventPropagation(event);"  name="outAccount" value="' + val.id + '"></td>';
            }
            tr +=
                '<td id="accountOut">' + _checkObj(val.account) + '</td>' +
                '<td id="accountOutAlias">' + _checkObj(val.alias) + '</td>' +
                '<td id="currSysLevelName">' + _checkObj(val.currSysLevelName) + '</td>' +
                '<td >' + _checkObj(val.statusName) + '</td>' +
                '<td id="bankNameOut">' + _checkObj(val.bankType) + _checkObj(val.bankName) + '</td>' +
                '<td >' + _checkObj(val.owner) + '</td>' +
                '<td id="bankBalanceOut">' + _checkObj(val.bankBalance).toFixed(3) + '</td>' +
                //'<td id="balanceOut">' + _checkObj(val.balance) + '</td>' +
                '<td id="limitOut">' + _checkObj(val.limitOut) + '</td>' +
                '<td id="outwardAmountDaily">' + _checkObj(val.outwardAmountDaily) + '</td>' +
                '</tr>';
        });
        $('#modal-fromAccountInfo-tbody').empty().html(tr);
        _renderOutAccountCount(param, 'normal');
    }
    showPading(res.page, 'modal-fromAccountInfo-foot', _getFromAccountInfo);
    $('#modal-fromAccountInfo-foot').find('span.showTotal').show();
}

//保存当前页的数据 [[id,amount,name],[],[]...]
function _saveCurrentPageData() {
    //保存当前页的数据 只有输入金额的选中行才记录
    currentPageForThirdAccountData = [];//[[],[],[]]
    $('#modal-thirdAccountInfo-tbody').find('input[name="toPayAccountId"]:checked').each(function () {
        if (parseFloat($.trim($('#toPayAccountUseAmount' + this.value).val())) > 0) {
            var currentPageData = [];
            currentPageData.push($('#accountNoToPayHandicap' + this.value).text());// id ->盘口
            currentPageData.push($('#accountNoToPayName' + this.value).text()); //name  商号
            currentPageData.push(parseFloat($.trim($('#toPayAccountUseAmount' + this.value).val())));// amount
            currentPageData.push(this.value);
            currentPageForThirdAccountData.push(currentPageData);
        }
    });
    _recordSelectedAccountIds(currentPageForThirdAccountData);
    currentPageForThirdAccountData = [];//[[],[],[]]
}

//增加或者减少 currentPageForThirdAccountData 到所有的记录的数据
function _recordSelectedAccountIds(currentPageData) {
    if (mapForSavePageHistorySelectedArray && mapForSavePageHistorySelectedArray.length > 0) {
        //查当前页之前保存的数据
        for (var i = 0; i < mapForSavePageHistorySelectedArray.length; i++) {
            var map = mapForSavePageHistorySelectedArray[i];//list->map
            var breakFlag = false;
            map.forEach(function (value, key) {
                if (key == currentPageForThirdAccountModal) {
                    //当前页之前保存的数据:[[],[],[].....]
                    if (currentPageData.length > 0) {
                        mapForSavePageHistorySelectedArray.splice(mapForSavePageHistorySelectedArray[i], 1);
                        var map2 = new Map();
                        map2.set(currentPageForThirdAccountModal, currentPageData);
                        mapForSavePageHistorySelectedArray.push(map2);
                        if (selectedPageNoArray.indexOf(currentPageForThirdAccountModal) <= -1) {
                            selectedPageNoArray.push(currentPageForThirdAccountModal);
                        }
                    } else {
                        //如果有旧的记录则 删除
                        mapForSavePageHistorySelectedArray.splice(mapForSavePageHistorySelectedArray[i], 1);
                        if (selectedPageNoArray && selectedPageNoArray.length > 0) {
                            if (selectedPageNoArray.indexOf(currentPageForThirdAccountModal) > -1) {
                                selectedPageNoArray.remove(currentPageForThirdAccountModal);
                            }
                        }

                    }
                    breakFlag = true;
                } else {
                    //如果保存的页码包含当前页则继续循环
                    if (selectedPageNoArray.indexOf(currentPageForThirdAccountModal) > 0) {

                    } else {
                        if (currentPageData.length > 0) {
                            var map1 = new Map();
                            map1.set(currentPageForThirdAccountModal, currentPageData);
                            mapForSavePageHistorySelectedArray.push(map1);
                        }
                    }
                }
            });
            if (breakFlag) {
                break;
            }
        }
    } else {
        //无数据 新增
        if (currentPageData.length > 0) {
            var map = new Map();
            map.set(currentPageForThirdAccountModal, currentPageData);
            mapForSavePageHistorySelectedArray.push(map);
            if (selectedPageNoArray.indexOf(currentPageForThirdAccountModal) <= -1) {
                selectedPageNoArray.push(currentPageForThirdAccountModal);
            }
        }
    }
}

//第三方出款账号模态框确定事件
function _confirmThirdAccountToUse() {
    if (!$('#modal-thirdAccountInfo-tbody').html()) {
        $('#out-account-prompt').text('目前没有可用的账号，请联系主管！').show(10).delay(6000).hide(10);
        return;
    }
    var flag = false;
    _saveCurrentPageData();
    var mapForSavePageHistorySelectedArrayLength = mapForSavePageHistorySelectedArray.length;
    if (mapForSavePageHistorySelectedArrayLength == 0) {
        var msg = '请先选中并填写实际使用金额再确定!';
        $('#out-account-prompt').text(msg).show(10).delay(3000).hide(10);
        flag = true;
        return false;
    }
    bootbox.confirm('是否确定?', function (res) {
        if (res) {
            // list->map->list->map   Array.from(new Set(a));
            //实际使用金额 账号id 商号名称
            var selectedAccountIdAmountArrays = [];
            if (mapForSavePageHistorySelectedArrayLength > 0) {
                for (var i = 0; i < mapForSavePageHistorySelectedArrayLength; i++) {
                    var map = mapForSavePageHistorySelectedArray[i];//list->map
                    if (map.size > 0) {
                        map.forEach(function (value) {
                            selectedAccountIdAmountArrays = selectedAccountIdAmountArrays.concat(value);// id ,金额 组成的数组 array [[],[],[]...]
                        });
                    }
                }
            }
            if (selectedAccountIdAmountArrays.length > 0) {
                for (var i = 0; i < selectedAccountIdAmountArrays.length; i++) {
                    var arrayRecord = selectedAccountIdAmountArrays[i];//id,金额,商户名称 [id,amount,name]
                    var remark = '盘口:' + arrayRecord[0] + ',商号:<span style="color: red;">' + arrayRecord[1] + '</span>,金额:<span style="color: red;">' + arrayRecord[2] + '</span>' + ',id:' + arrayRecord[3];
                    if ($.trim(remark)) {
                        thirdAccountOutRemarksInfo.push(remark);
                    }
                }
            }
            if (flag) {
                return false;
            }
            _renderThirdOutRemarks();
            $('#modal-fromAccountInfo').modal('hide');
        } else {
            //已经有使用其他第三方的,那么要保存
            if (addOtherThirdInfoFlag) {
                _renderThirdOutRemarks();
            }
            setTimeout(function () {
                $('body').addClass('modal-open');
            }, 500);
        }
    });

}

//渲染备注信息
function _renderThirdOutRemarks() {
    if (thirdAccountOutRemarksInfo && thirdAccountOutRemarksInfo.length > 0) {
        var remarks = '';
        var infoLength = thirdAccountOutRemarksInfo.length;
        for (var i = 0; i < infoLength; i++) {
            //thirdAccountOutAmount 总用款金额
            var amount = $(thirdAccountOutRemarksInfo[i].split('金额:')[1]).text();
            thirdAccountOutAmount += parseFloat($.trim(amount));
            if (infoLength > 1) {
                if (i < infoLength - 1) {
                    remarks += thirdAccountOutRemarksInfo[i] + '|';
                } else {
                    remarks += thirdAccountOutRemarksInfo[i];
                }
            } else {
                remarks += thirdAccountOutRemarksInfo[i];
            }
        }
        $('#fromAccountInfoTd').html('<h4 >' + remarks + '</h4>');
    }
}

//确定第三方出款账号 按钮 点击
$('#confirmAccoutToDraw').unbind().bind('click', function () {
    _confirmThirdAccountToUse();
});
//取消模态框
$('#cancelModal').unbind().bind('click', function () {
    $('#modal-fromAccountInfo').modal('hide');
    //selectedCurrentPageThirdAccountArray = [];
    _renderThirdOutRemarks();
});
//×事件
$('#closeModal').unbind().bind('click', function () {
    $('#modal-fromAccountInfo').modal('hide');
    //selectedCurrentPageThirdAccountArray = [];
    //已经有使用其他第三方的,那么要保存
    _renderThirdOutRemarks();
});

//某一行点击事件
function _selectedTrChecked(obj) {
    if ($(obj).find('input[type="checkbox"]').prop('checked')) {
        //不选
        $(obj).find('input[type="checkbox"]').prop('checked', '');
        //_selectedAllThirdAccountArrayRemove($(obj).find('input[name="toPayAccountId"]').val());
    } else {
        //选中
        $(obj).find('input[type="checkbox"]').prop('checked', 'checked');
        //_selectedAllThirdAccountArrayAdd($(obj).find('input[name="toPayAccountId"]:checked').val());
    }
    _saveCurrentPageData();
}

//某一行的复选框点击事件
function _selectInputCheckeboxInsideTr(event, obj) {
    disabledEventPropagation(event);
    if ($(obj).attr('click_value') == '1') {
        $(obj).attr('click_value', '2');
        $(obj).prop('checked', '');
        //不选
        // _selectedAllThirdAccountArrayRemove($(obj).val().toString());
    } else {
        //选中
        $(obj).attr('click_value', '1');
        $(obj).prop('checked', 'checked');
        // _selectedAllThirdAccountArrayAdd($(obj).val().toString());
    }
    _saveCurrentPageData();
}

//全部th全选点击事件
function _selectedAllThirdAccount(obj) {
    if ($(obj).find('input[type="checkbox"]').prop('checked')) {
        //全不选
        $(obj).find('input[type="checkbox"]').prop('checked', '');
        $('#modal-thirdAccountInfo-tbody').find('input[type="checkbox"]').prop('checked', '');
        //selectedCurrentPageThirdAccountArray = [];
        //selectedCurrentPageAccountMap = new Map();
    } else {
        //全选
        $(obj).find('input[type="checkbox"]').prop('checked', 'checked');
        $('#modal-thirdAccountInfo-tbody').find('input[type="checkbox"]').prop('checked', 'checked');
        //保存选中的账号id
        // $('#modal-thirdAccountInfo-tbody').find('input[type="checkbox"]:checked').each(function () {
        //     selectedCurrentPageThirdAccountArray.push(this.value);
        //     selectedCurrentPageAccountMap.set(currentPageForThirdAccountModal,selectedCurrentPageThirdAccountArray);
        // });
        //_recordSelectedAccountIds();
    }
    _saveCurrentPageData();
}

//'全部'复选框 点击事件
function _inputCheckboxInsideThClick(event, obj) {
    disabledEventPropagation(event);
    if ($(obj).val() == 1) {
        //全不选
        $(obj).val(2);
        $(obj).prop('checked', '');
        $('#modal-thirdAccountInfo-tbody').find('input[type="checkbox"]').prop('checked', '');
        //selectedCurrentPageThirdAccountArray = [];
        //selectedCurrentPageAccountMap = new Map();
    } else {
        //全选
        $(obj).val(1);
        $(obj).prop('checked', 'checked');
        $('#modal-thirdAccountInfo-tbody').find('input[type="checkbox"]').prop('checked', 'checked');
        // $('#modal-thirdAccountInfo-tbody').find('input[type="checkbox"]:checked').each(function () {
        //     selectedCurrentPageThirdAccountArray.push(this.value);
        //     selectedCurrentPageAccountMap.set(currentPageForThirdAccountModal,selectedCurrentPageThirdAccountArray);
        // });
        // _recordSelectedAccountIds();
    }
    _saveCurrentPageData();
}

// Array.prototype.splice = function (val) {
//     var index = this.indexOf(val);
//     if (index > -1) {
//         this.splice(index, 1);
//     }
// };
//出款账号总记录数
function _renderOutAccountCount(param, type) {
    if (param) {
        $.ajax({
            url: '/r/account/findAllocateAccount4OutwardAsignCount',
            data: param,
            dataType: 'json',
            async: false,
            success: function (res) {
                if (res && res.status == 1) {
                    if (type == 'third') {
                        showPading(res.page, 'modal-thirdAccountInfo-foot', _getFromAccountInfo);
                    } else {
                        showPading(res.page, 'modal-fromAccountInfo-foot', _getFromAccountInfo);
                    }
                    currentPageForThirdAccountModal = parseInt($('#thirdOutAccountDiv').find(".Current_Page").text());
                }
            }
        });
    }
}

//使用其他第三方账号按钮
function _useOtherThirdAccount() {
    var trs = '';
    for (var i = 0; i < 5; i++) {
        if (i < 4) {
            trs += '<tr><td><input id="handicapOtherThirdAccount' + i + '" name="handicapOtherThirdAccount" placeholder="必填(如:699)"></td>' +
                '<td><input id="nameOtherThirdAccount' + i + '" name="nameOtherThirdAccount" placeholder="必填(如:百付通/财付通)"></td>' +
                '<td><input id="accountOtherThirdAccount' + i + '" name="accountOtherThirdAccount"></td>' +
                '<td><input onkeyup="clearNoNum(this);" id="amountUseOtherThirdAccount' + i + '" name="amountUseOtherThirdAccount" placeholder="必填(如:199)"></td>' +
                '<td><button onclick="_deleteTrFromOtherThirdAccount(this);" class="btn btn-white btn-sm  btn-warning"><i class="ace-icon fa fa-remove"></i>删除</button></td></tr>';
        } else {
            trs += '<tr><td><input id="handicapOtherThirdAccount' + i + '" name="handicapOtherThirdAccount"  placeholder="必填(如:699)"></td>' +
                '<td><input id="nameOtherThirdAccount' + i + '" name="nameOtherThirdAccount" placeholder="必填(如:百付通/财付通)"></td>' +
                '<td><input id="accountOtherThirdAccount' + i + '" name="accountOtherThirdAccount"></td>' +
                '<td><input onkeyup="clearNoNum(this);" id="amountUseOtherThirdAccount' + i + '" name="amountUseOtherThirdAccount"  placeholder="必填(如:199)"></td>' +
                '<td><button onclick="_deleteTrFromOtherThirdAccount(this);" class="btn  btn-white btn-sm  btn-warning"><i class="ace-icon fa fa-remove"></i>删除</button><button onclick="_addNewTrForOtherThirdAccount(this);" class="btn  btn-white btn-sm  btn-success"><i class="ace-icon fa fa-plus"></i>增加</button></td></tr>';
        }
    }
    $('#modal-otherThird-tbody').empty().html(trs);
    $('#modal-fromAccountInfo-otherThird').modal('show');
}

//增加一行
function _addNewTrForOtherThirdAccount(obj) {
    var newTr = $(obj).parent().parent().parent().children("tr:first-child").clone();
    newTr.insertBefore($(obj).parent().parent());
    if ($(obj).parent().parent().parent().children().length > 1) {
        $(obj).parent().parent().prev().children("td:last-child").empty().html('<button onclick="_deleteTrFromOtherThirdAccount(this);" class="btn  btn-white btn-sm  btn-warning"><i class="ace-icon fa fa-remove"></i>删除</button>');
    }
}

//删除一行
function _deleteTrFromOtherThirdAccount(obj) {
    if ($('#modal-otherThird-tbody').find('tr').length > 1) {
        $(obj).parent().parent().remove();
    } else {
        $(obj).parent().parent().find('input').each(function () {
            this.value = '';
        });
    }
    $('#modal-otherThird-tbody').children("tr:last-child").children('td:last-child').empty().html('<button onclick="_deleteTrFromOtherThirdAccount(this);" class="btn btn-sm  btn-white btn-warning"><i class="ace-icon fa fa-remove"></i>删除</button><button onclick="_addNewTrForOtherThirdAccount(this);" class="btn  btn-white btn-sm btn-success"><i class="ace-icon fa fa-plus"></i>增加</button>');
}

//关闭其他第三方账号模态框
function _closeUseOtherThirdAccount() {
    $('#modal-fromAccountInfo-otherThird').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}

//确定其他第三方账号模态框
function _confirmUseOtherThirdAccount() {
    var nameOtherThirdAccount = [];
    var amountUseOtherThirdAccount = [];
    $('#modal-otherThird-tbody').find('input[name="nameOtherThirdAccount"]').each(function () {
        if ($.trim(this.value)) {
            nameOtherThirdAccount.push(this.value);
        }
    });
    $('#modal-otherThird-tbody').find('input[name="amountUseOtherThirdAccount"]').each(function () {
        if (this.value && this.value > 0) {
            amountUseOtherThirdAccount.push(this.value);
        }
    });

    var handicapOtherThirdAccount = [];
    var accountOtherThirdAccount = [];
    $('#modal-otherThird-tbody').find('input[name="handicapOtherThirdAccount"]').each(function () {
        handicapOtherThirdAccount.push(this.value ? this.value : " ");
    });
    $('#modal-otherThird-tbody').find('input[name="accountOtherThirdAccount"]').each(function () {
        accountOtherThirdAccount.push(this.value ? this.value : " ");
    });
    //thirdAccountOutRemarksInfo=[]
    var otherThirdAccountOutRemarksInfo = [];
    var lengthFori = nameOtherThirdAccount.length <= amountUseOtherThirdAccount.length ? nameOtherThirdAccount.length : amountUseOtherThirdAccount.length;
    if (lengthFori == 0) {
        $('#out-account-prompt-otherThird').text('请填写使用信息!').show(10).delay(2000).hide(10);
        return false;
    }
    var flag = false;
    for (var i = 0; i < lengthFori; i++) {
        var remark = '';
        if ($.trim(nameOtherThirdAccount[i]) && $.trim(amountUseOtherThirdAccount[i]) && parseFloat($.trim(amountUseOtherThirdAccount[i])) > 0) {
            if ($.trim(handicapOtherThirdAccount[i])) {
                remark += '盘口:<span style="color: red;">' + handicapOtherThirdAccount[i] + '</span>';
            } else {
                $('#out-account-prompt-otherThird').text('请填写盘口!').show(10).delay(2000).hide(10);
                flag = true;
                return false;
            }
            if ($.trim(nameOtherThirdAccount[i])) {
                remark += ',商号:<span style="color: red;">' + nameOtherThirdAccount[i] + '</span>';
            } else {
                $('#out-account-prompt-otherThird').text('请填写商号!').show(10).delay(2000).hide(10);
                flag = true;
                return false;
            }
            if ($.trim(amountUseOtherThirdAccount[i])) {
                remark += ',金额:<span style="color: red;">' + amountUseOtherThirdAccount[i] + '</span>';
            } else {
                $('#out-account-prompt-otherThird').text('请填写金额!').show(10).delay(2000).hide(10);
                flag = true;
                return false;
            }
            if ($.trim(accountOtherThirdAccount[i])) {
                remark += ',账号:' + accountOtherThirdAccount[i];
            }
            if ($.trim(remark)) {
                otherThirdAccountOutRemarksInfo.push(remark);
            }
        } else {
            $('#out-account-prompt-otherThird').text('第三方商号和使用金额不对应,请填写!').show(10).delay(2000).hide(10);
            flag = true;
            return false;
        }
    }
    if (flag) {
        return;
    }
    bootbox.confirm('是否确定?', function (res) {
        if (res) {
            //合并数组
            thirdAccountOutRemarksInfo = thirdAccountOutRemarksInfo.concat(otherThirdAccountOutRemarksInfo);
            _closeUseOtherThirdAccount();
        } else {
            otherThirdAccountOutRemarksInfo = [];
            addOtherThirdInfoFlag = false;
            setTimeout(function () {
                $('body').addClass('modal-open');
            }, 500);
        }
    });
}

// 选择出款账号 按钮 点击
$('#selectAccount').bind('click', function () {
    var first_click_flag = $('#selectAccount').attr('first_click_flag');
    // if (first_click_flag=='1'){
    //     selectedCurrentPageThirdAccountArray = [];
    // }
    thirdAccountOutRemarksInfo = [];
    thirdAccountOutAmount = null;
    $('#accountToDraw').val('');
    _getFromAccountInfo();
    $('#modal-fromAccountInfo').modal('show');
});

// 冻结账号 按钮 点击
$('#freezeAccount').bind('click', function () {
    if (!$.session.get('accountToDrawId')) {
        $.gritter.add({
            time: 100000,
            title: '系统消息',
            text: '没有可冻结的出款卡',
            sticky: false,
            image: '../images/message.png'
        });
        return;
    }
    $('#prompt_freezeAccount').show();
    $('#remarkPrompt_freezeAccount').hide();
    $('#freezeAccount_remark').val('');
    $('#freezeAccount_modal').modal('show');
    var opt = '';
    $(manualFreezeAccountRemarks).each(function (i, val) {
        opt += '<option>' + val + '</option>';
    });
    $('#freezeAccountRemark').empty().html(opt);
});

function _freezeAccountConfirm() {
    if (!$('#freezeAccount_remark').val()) {
        $('#remarkPrompt_freezeAccount').show(10).delay(3000).hide(10);
        return;
    }
    bootbox.confirm('确定冻结该账号吗？', function (res) {
        if (res) {
            _freezeAccountExecute();
        }
    });
}

function _freezeAccountExecute() {
    var accountId = $.session.get('accountToDrawId');
    if (accountId) {
        $.ajax({
            type: 'post',
            url: API.r_account_toFreezeForver,
            data: {"accountId": accountId},
            async: false,
            dataType: 'json',
            success: function (res) {
                if (res) {
                    $.gritter.add({
                        time: 500,
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../images/message.png'
                    });
                    $('#freezeAccount_modal').modal('hide');
                }
            }
        });
    }
}

//选择出款账号模态框 查询按钮
$('#findAccount').bind('click', function (event) {
    _getFromAccountInfo();

});
$('#findAccountReset').bind('click', function (event) {
    $('#accountToDraw').val('');
    _getFromAccountInfo();
});

//点击接单的时候判断出款人是否有在用的出款账号
function _judgeOwningOutAccount() {
    var haveNoAccount = false;
    $.ajax({
        type: 'get',
        url: '/r/account/findAllocateAccount4OutwardAsign',
        data: {"pageNo": 0},
        dataType: 'json',
        async: false,
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data && res.data.length > 0) {

                } else {
                    haveNoAccount = true;
                    $.gritter.add({
                        time: 1000000,
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
    return haveNoAccount;
}
/** 加载可以转交任务的名单 */
var load_choice_person=function(taskId){
	$.ajax({
		type: "POST",
		dataType: 'JSON',
		url: '/r/outtask/outwardUserList',
		async: true,
		data:{taskId:taskId},
		success: function (jsonObject) {
			if(jsonObject.status==1){
	        	var curr_uid=getCookie('JUID');
				var options="<option value='' >接收人</option>";
				$.each(jsonObject.data,function(index,record){
					//过滤掉当前登录人
					if(record&&curr_uid!=record.uid){
						options+="<option value="+record.id+"  >"+record.uid+"</option>";
					}
				});
				$("[name=choice_person]").html(options);
			}
		}
	});
}
/** 转单 */
var do_transferOtherPerson=function(e){
	if(!$("[name=choice_person]").val()){
		showMessageForFail("请先选中接单人");
		return;
	}
	if(!$("[name=choice_remark]").val()){
		showMessageForFail("请先输入备注");
		return;
	}
	$.ajax({
		type: "POST",
		dataType: 'JSON',
		url: '/r/outtask/transToOther',
		async: false,
		data:{
			taskId:$(e).attr("taskId"),
			transferToOther:$("[name=choice_person]").val(),
			remark:$("[name=choice_remark]").val()
		},
		success: function (jsonObject) {
			if(jsonObject.status==1){
	        	//showMessageSuccess("转单成功");
                showMessageForSuccess("转单成功");
                _resetThirdOutTemperateData();
                haveReceivedTaskData = false;
                $.session.remove("selectThirdAccountFlag");
                $.session.set('taskIdLastTime', $.session.get('toTransferId'));//记住本次获取的任务,以防止重复获取任务
                $.session.remove('toTransferId');//删除本次任务
                $("[name=choice_remark]").val('');
                _continueTasks();
			}else{
	        	showMessageForFail("转单失败："+jsonObject.message);
			}
		}
	});
	
	
}
_showToTaskLi();
_initPage();
_hide();
_checkAccountToDrawInSession();
