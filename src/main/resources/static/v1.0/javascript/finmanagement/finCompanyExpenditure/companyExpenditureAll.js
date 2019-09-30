currentPageLocation = window.location.href;
var addCompanyExpendFlag = false;
var handicapForAllEdit = null;
var addCompanyOrderFlagAll = null;//标识每一次新增公司用款,用于防止重复提交
(function () {
    $.each(ContentRight['financeCompanyExpenditure:*'], function (name, value) {
        if (name == 'financeCompanyExpenditure:addCompanyExpend:*') {
            addCompanyExpendFlag = true;
        }
    })
    if (addCompanyExpendFlag) {
        $('#addOrderBtn_cashExpence_all').show();
    } else {
        $('#addOrderBtn_cashExpence_all').hide();
    }
})();
/**初始化盘口*/
function _initialHandicapAndPurposeInCompanyExpend() {
    //可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    _gethandicapCompanyExpend();
    $('#companyExpenditureHandicap_chosen').prop('style', 'width: 78%;');
}
/**
 * 获取盘口信息
 */
function _gethandicapCompanyExpend() {
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
                        opt += '<option value="' + $.trim(val.id) + '">' + val.name + '</option>';
                    });
                    $('#companyExpenditureHandicapAll').empty().html(opt);
                    $('#companyExpenditureHandicapAll').trigger('chosen:updated');
                    $('#handicap_companyExpence_all').empty().html(opt);
                    $('#handicap_companyExpence_all').trigger('chosen:updated');
                }
            }
        }
    });
}
/**新增公司用款*/
function _beforeAddCompanyExpenditureAll() {
    $('#addCompanyExpenditureTitleAll').show();
    $('#editCompanyExpenditureTitleAll').hide();
    $('#remarkOrHandicapAll').empty().html('<label style="margin-right:3px;">用款盘口<i class="fa fa-asterisk red"></i></label>'+
        '<select style="width: 220px" id="companyExpenditureHandicapAll" ></select>');
    $('#companyExpenditureAmountAll').val('');
    $('#companyExpenditureToAccountAll').val('');
    $('#companyExpenditureToBankAll').val('');
    $('#companyExpenditureToNameAll').val('');
    $('#companyExpenditurePurposeAll').val('');
    $('#companyExpenditurePurposeChildAll').val('');
    $('#editCompanyExpenditrueReqIdAll').val('');
    _initialHandicapAndPurposeInCompanyExpend();
    _findUsersForCompanyAuditor('');
    $('#addCompanyExpenditureModalAll').modal('show');
    $('#addOreditCompanyExpenditrueAll').attr('onclick',"_confirmCompanyExpenditureAll('add');");
    addCompanyOrderFlagAll = (new Date()).valueOf();
}
/**确认新增*/
function _confirmCompanyExpenditureAll(flag) {
    $('#promptCompanyAll').text('').hide();
    if (!$('#companyExpenditureAmountAll').val()) {
        $('#promptCompanyAll').text('请填写出款金额').show(10).delay(1500).hide(10);
        return;
    }
    if (!$.trim($('#companyExpenditureToAccountAll').val())) {
        $('#promptCompanyAll').text('请填写对方账号').show(10).delay(1500).hide(10);
        return;
    }
    if (!$.trim($('#companyExpenditurePurposeAll').val()) || $('#companyExpenditurePurposeAll').val() == '请选择') {
        $('#promptCompanyAll').text('请填写用款来源大类').show(10).delay(1500).hide(10);
        return;
    }
    if (!$.trim($('#companyExpenditurePurposeChildAll').val()) || $('#companyExpenditurePurposeChildAll').val() == '请选择') {
        $('#promptCompanyAll').text('请填写用款来源细分').show(10).delay(1500).hide(10);
        return;
    }
    if (!$('#companyExpenditureToNameAll').val()) {
        $('#promptCompanyAll').text('请填写对方姓名(开户名)').show(10).delay(1500).hide(10);
        return;
    }
    if (flag =='add' && !$('#companyExpenditureHandicapAll').val() || $('#companyExpenditureHandicapAll').val() == '请选择') {
        $('#promptCompanyAll').text('请选择盘口').show(10).delay(1500).hide(10);
        return;
    }
    if (!$('#companyExpenditureToBankAll').val()) {
        $('#promptCompanyAll').text('请填写开户行').show(10).delay(1500).hide(10);
        return;
    }
    if (!$.trim($('#companyExpenditureAuditorAll').val()) || $.trim($('#companyExpenditureAuditorAll').val()) =='请选择'){
        $('#promptCompanyAll').text('请选择审核人').show(10).delay(1500).hide(10);
        return;
    }
    var amount = $.trim($('#companyExpenditureAmountAll').val());
    var toAccount = $.trim($('#companyExpenditureToAccountAll').val());
    var toBank = $.trim($('#companyExpenditureToBankAll').val());
    var toName = $.trim($('#companyExpenditureToNameAll').val());
    var purpose = $.trim($('#companyExpenditurePurposeAll').val()) +'-'+$.trim($('#companyExpenditurePurposeChildAll').val());
    var auditorId = $.trim($('#companyExpenditureAuditorAll').val());
    var editCompanyExpenditrueReqId = '';
    var remark = "";
    var msg = '';
    var handicap = '';
    if (flag=='add'){
        msg = '确定新增用款吗？';
        handicap = $('#companyExpenditureHandicapAll').val();
        if(!addCompanyOrderFlagAll){
            return ;
        }
    }
    if (flag=='edit'){
        handicap = handicapForAllEdit;
        editCompanyExpenditrueReqId = $('#editCompanyExpenditrueReqIdAll').val();
        remark = $.trim($('#companyExpenditureEditRemarkAll').val());
        if (!remark) {
            $('#promptCompanyAll').text('请填写备注').show(10).delay(1500).hide(10);
            return;
        }
        msg = '确定保存编辑用款吗？';
    }
    //收款账号信息去除空格
    var data = {
        "handicap": handicap,
        "amount": $.trim(amount),
        "toAccount": $.trim(toAccount),
        "toBank": $.trim(toBank),
        "toName": $.trim(toName),
        "purpose": $.trim(purpose),
        "localHostIp": localHostIp,
        "auditorId":auditorId,
        "reqId":editCompanyExpenditrueReqId,
        "remark":remark,
        "timeFlag":addCompanyOrderFlagAll
    };

    bootbox.confirm(msg, function (res) {
        if (res) {
            $.ajax({
                type: 'post',
                url: '/r/out/addCompanyExpend',
                data: data,
                dataType: 'json',
                success: function (res) {
                    if (res) {
                        $.gritter.add({
                            time: 1500,
                            class_name: '',
                            title: '系统消息',
                            text: res.message,
                            image: '../images/message.png'
                        });
                        if (res.status == 1) {
                            addCompanyOrderFlagAll = null;
                            $('#addCompanyExpenditureModalAll').modal('hide');
                            _queryCompanyExpenditureAll();
                        }
                    }
                }
            });
        }
    });
}
/**查询*/
function _queryCompanyExpenditureAll() {
    var handicapId = '';
    if ($('#handicap_companyExpence_all').val() && $('#handicap_companyExpence_all').val() != '请选择') {
        handicapId = $('#handicap_companyExpence_all').val();
    }
    var startTime = '';
    var endTime = '';
    var startAndEndTime = $('#timeCreate_companyExpence_all').val();
    if (startAndEndTime) {
        if (startAndEndTime.indexOf('~') > 0) {
            startAndEndTime = startAndEndTime.split('~');
            startTime = startAndEndTime[0];
            endTime = startAndEndTime[1];
        }
    }
    var purpose = '';
    if ($('#companyExpenditurePurpose1_all').val() && $('#companyExpenditurePurpose1_all').val() != '请选择') {
        purpose = $('#companyExpenditurePurpose1_all').val();
    }
    var type = null;
    if ($.trim($('input[name="type_companyExpence_all"]:checked').val())) {
        type = $.trim($('input[name="type_companyExpence_all"]:checked').val());
    }
    var amountStart = '';
    var amountEnd = '';
    if ($.trim($('#fromMoney_companyExpence_all').val())) {
        amountStart = $.trim($('#fromMoney_companyExpence_all').val());
    }
    if ($.trim($('#toMoney_companyExpence_all').val())) {
        amountEnd = $.trim($('#toMoney_companyExpence_all').val());
    }
    var outAccount = '';
    if ($.trim($('#accountOut_companyExpence_all').val())) {
        outAccount = $.trim($('#accountOut_companyExpence_all').val());
    }
    var CurPage = $("#footPage_companyExpence_all").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var data = {
        "outAccount": outAccount,
        "purpose": purpose,
        "handicapId": $.trim(handicapId),
        "startTime": $.trim(startTime),
        "endTime": $.trim(endTime),
        "amountStart": $.trim(amountStart),
        "amountEnd": $.trim(amountEnd),
        "type": type ? $.trim(type) : null,
        "pageNo": CurPage,
        "pageSize": $.session.get('initPageSize')
    };
    var approveFlag = false;
    var addRemarkFlag = false;
    var confirmFlag = false;
    var UploadReceipt = false;
    var editFlag = false;
    var deleteFlag = false;
    $.each(ContentRight['financeCompanyExpenditure:*'], function (name, value) {
        if (name == 'financeCompanyExpenditure:approveCompanyExpend:*') {
            approveFlag = true;
        } else if (name == 'financeCompanyExpenditure:remarkCompanyExpend:*') {
            addRemarkFlag = true;
        } else if (name == 'financeCompanyExpenditure:confirmCompanyExpend:*') {
            confirmFlag = true;
        } else if (name == 'financeCompanyExpenditure:uploadReceipt:*') {
            UploadReceipt = true;
        }else if (name == 'financeCompanyExpenditure:editCompanyExpend:*') {
            editFlag = true;
        } else if (name == 'financeCompanyExpenditure:deleteCompanyExpend:*') {
            deleteFlag = true;
        }
    });
    $.ajax({
        type: 'get',
        url: '/r/out/queryCompanyExpend',
        dataType: 'json',
        async:false,
        data: data,
        success: function (res) {
            if (res) {
                var tr = '';
                var trs = '';
                var amount = 0;
                if (res.status == 1) {
                    if (res.data && res.data.length > 0) {
                        var idList = new Array();
                        $(res.data).each(function (i, v) {
                            amount += parseFloat(v.taskAmount ? v.taskAmount : v.amount);
                            var remark = '';
                            if (_checkObj(v.outAccountId)) {
                                idList.push({'id': v.outAccountId});
                            }
                            if (_checkObj(v.taskRemark)) {
                                remark = _checkObj(v.taskRemark);
                            } else if (_checkObj(v.reqRemark)) {
                                remark = _checkObj(v.reqRemark);
                            }
                            tr += '<tr>' +
                                '<td>' + _checkObj(v.handicap) + '</td>' +
                                '<td>' + _checkObj(v.purpose) + '</td>' +
                                '<td>' + _checkObj(v.orderNo) + '</td>' +
                                '<td>' + (_checkObj(v.taskAmount) ? v.taskAmount : _checkObj(v.amount)) + '</td>' +
                                '<td>' + _checkOutTypeAll(v.taskOperator, v.outAccountId) + '</td>';
                            if (_checkObj(v.outAccount) && _checkObj(v.outAccountId)) {
                                tr += "<td><a  class='bind_hover_card breakByWord' data-html='true' data-toggle='accountInfoHover" + v.outAccountId + "' data-placement='left' data-trigger='hover'  data-original-title=''  title='' >" +
                                    (_checkObj(v.outAccount).substring(0, 3) + "**" + _checkObj(v.outAccount).substring(_checkObj(v.outAccount).length - 3, _checkObj(v.outAccount).length)) +
                                    "</a></td>";
                            } else {
                                tr += '<td></td>';
                            }

                            if (_checkObj(v.toAccount).length > 6) {
                                tr += '<td>'
                                    + '<a class="bind_hover_card breakByWord"  title="收款账号信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + ('收款账号:' + v.toAccount + '<br>开户人：' + v.toAccountOwner + '<br>开户行：' + v.toAccountBank ) + '">'
                                    + _checkObj(v.toAccount).substring(0, 3) + '***' + _checkObj(v.toAccount).substring(v.toAccount.length - 3, v.toAccount.length)
                                    + '</a>'
                                    + '</td>';
                            } else {
                                tr += '<td>'
                                    + '<a class="bind_hover_card breakByWord"  title="收款账号信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + ('收款账号:' + v.toAccount + '<br>开户人：' + v.toAccountOwner + '<br>开户行：' + v.toAccountBank ) + '">'
                                    + _checkObj(v.toAccount)
                                    + '</a>'
                                    + '</td>';
                            }
                            var updateTime = '';
                            if (v.asignTime && v.timeComsuming) {
                                updateTime = parseInt(v.asignTime) + parseInt(v.timeComsuming);
                            }
                            tr += '<td>' + (v.taskStatus? _showStatuMsgAll(v.taskStatus) : v.reqStatus )+ '</td>' +
                                '<td>' + _checkObj(v.reviewer) + '</td>' +
                                '<td >'
                                + '<a class="bind_hover_card breakByWord"  title="创建时间"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + 'data-content="' + _checkObj(timeStamp2yyyyMMddHHmmss(v.createTime)) + '">'
                                + _checkObj(timeStamp2yyyyMMddHHmmss(v.createTime)).substring(_checkObj(timeStamp2yyyyMMddHHmmss(v.createTime)).length - 8, _checkObj(timeStamp2yyyyMMddHHmmss(v.createTime)).length)
                                + '</a>'
                                + '</td>' +
                                '<td >'
                                + '<a  class="bind_hover_card breakByWord"  title="出款时间"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + 'data-content="' + _checkObj(timeStamp2yyyyMMddHHmmss(updateTime)) + '">'
                                + _checkObj(timeStamp2yyyyMMddHHmmss(updateTime)).substring(_checkObj(timeStamp2yyyyMMddHHmmss(updateTime)).length - 8, _checkObj(timeStamp2yyyyMMddHHmmss(updateTime)).length)
                                + '</a>'
                                + '</td>';
                            if (_checkObj(remark)) {
                                if (_checkObj(remark).length > 5) {
                                    tr += '<td>'
                                        + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + remark + '">'
                                        + _checkObj(remark).substring(0, 5)
                                        + '</a>'
                                        + '</td>';
                                } else {
                                    tr += '<td>'
                                        + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                        + ' data-content="' + remark + '">'
                                        + _checkObj(remark) +
                                        +'</a>'
                                        + '</td>';
                                }
                            } else {
                                tr += "<td></td>";
                            }
                            var hasScreenshot = false;
                            var screenshotArr = null;
                            if (_checkObj(v.screenshot)) {
                                screenshotArr = v.screenshot.split('/');
                            }
                            if (screenshotArr) {
                                if (screenshotArr.length > 3) {
                                    hasScreenshot = (screenshotArr[3]).indexOf('screenshot') > -1;
                                } else {
                                    hasScreenshot = (screenshotArr[2]).indexOf('screenshot') > -1;
                                }
                            }
                            if (_checkObj(v.screenshot)) {
                                if (hasScreenshot) {
                                    tr += '<td>' +
                                        '<a href="javascript:void(0);" id="taskPhoto" onclick="_companyImgPhotoAll(\'' + window.location.origin + '\/' + _checkObj(v.screenshot) + '\')">查看</a>' +
                                        '</td>';
                                } else {
                                    tr += '<td>' +
                                        '<a href="javascript:void(0);" id="taskPhoto" onclick="_companyImgPhotoAll(\'' + window.location.origin + '\/' + _checkObj(v.screenshot) + '\')">回执</a>' +
                                        '</td>';
                                }
                            } else {
                                tr += '<td></td>';
                            }
                            tr += '<td style="width:200px;">';
                            if (v.reqStatus == '正在审核' && approveFlag) {
                                tr += '<button   class="btn btn-xs btn-white btn-info btn-bold "  type="button" onclick="_beforeApproveCompanyExpenditureAll(' + v.reqId + ',\'' + v.handicap + '\',' + (v.amount == v.taskAmount ? v.taskAmount : v.amount) + ',\'' + v.purpose + '\',\'' + v.toAccount + '\',\'' + v.toAccountOwner + '\',\'' + v.toAccountBank + '\');">' +
                                    '<i class="fa  fa-gavel"></i>审批' +
                                    '</button>';
                            } else if ((_showStatuMsgAll(v.taskStatus) == '待确认' || v.taskStatus == '待排查' || v.taskStatus == '已出款' || v.taskStatus == '主管处理' ) && confirmFlag && v.taskId && !(remark.indexOf("执行确认") > -1)) {
                                tr += '<button type="button" onclick="_confirmCompanyExpendResultAll(' + v.taskId + ');" class="btn btn-xs btn-white btn-purple btn-bold "><i class="ace-icon fa fa-check bigger-100 blue"></i>确认</button>';
                            }
                            if (addRemarkFlag) {
                                if (v.reqStatus != '正在审核'){
                                    tr += '<button type="button" onclick="_addRemarksCompanyExpendAll(' + v.taskId + ',' + v.reqId + ');" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                                }else{
                                    tr += '<button type="button" onclick="_addRemarksCompanyExpendAll(null,' + v.reqId + ');" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                                }
                            }
                            if (UploadReceipt && _checkObj(v.screenshot)) {
                                tr += '<button type="button" onclick="_uploadReceiptPhotoCompanyExpendAll(' + v.taskId + ');" class="btn btn-xs btn-white btn-success btn-bold "><i class="fa fa-arrow-circle-up bigger-100 green"></i>回执</button>';
                            }
                            if (v.reqStatus== '正在审核' && editFlag) {
                                tr += '<button type="button" onclick="_beforeEdit(' + v.reqId +','+v.amount+',\''+v.purpose  +'\',\''+v.toAccount +'\',\''+v.toAccountOwner +'\',\''+v.toAccountBank +'\',\''+v.reviewerId+'\',\''+v.handicapId+'\');" class="btn btn-xs btn-white btn-primary btn-bold "><i class="ace-icon fa fa-pencil-square-o bigger-110 green"></i>编辑</button>';
                            }
                            if (v.reqStatus == '正在审核' && deleteFlag) {
                                tr += '<button type="button" onclick="_beforeDelete(' + v.reqId + ');" class="btn btn-xs btn-white btn-danger btn-bold "><i class="ace-icon fa fa-remove bigger-110 red"></i>删除</button>';
                            }
                            tr += '</td>';
                        });
                        trs +=
                            '<tr>' +
                            '<td id="companyExpendCurrentCount_all" colspan="3">小计：统计中..</td>' +
                            '<td bgcolor="#579EC8" style="color:white;width: 100px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                            '<td colspan="15"></td></tr>' +
                            '<tr>' +
                            '<td id="companyExpendAllCount_all" colspan="3">总共：统计中..</td>' +
                            '<td id="companyExpendSum_all" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td><td colspan="15"></td></tr>';
                    }
                    $('#tbody_companyExpence_all').empty().html(tr).append(trs);
                    //加载账号悬浮提示
                    _getCompanyExpendCountAll(data);
                    _getCompanyExpendSumAll(data);
                    $("[data-toggle='popover']").popover();
                    if (idList && idList.length > 0) {
                        loadHover_accountInfoHover(idList);
                    }
                } else {
                    $('#tbody_companyExpence_all').html('<tr><td colspan="15" style="text-align: center"><h3>' + res.message + ',请稍后。</h3></td></tr>');
                }
                showPading(res.page, 'footPage_companyExpence_all', _queryCompanyExpenditureAll);
            } else {
                $('#tbody_companyExpence_all').html('<tr><td colspan="15" style="text-align: center"><h3>网络异常,请稍后。</h3></td></tr>');
            }

        }
    });
}
function _uploadReceiptPhotoCompanyExpendAll(taskId) {
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
                            if (res.status == 1) {
                                _queryCompanyExpenditureAll();
                            }
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
                });
            }
        });
    }
}
/**最终确认公司用款*/
function _confirmCompanyExpendResultAll(taskId) {
    if (taskId) {
        $('#remarkForCompanyExpend_all').val('');
        $('#remarkForCompanyExpend_taskId_all').val(taskId);
        $('#remarkForCompanyExpend_reqId_all').val('');
        $('#titleDivCustomer_all').show();
        $('#titleDivApprove_all').hide();
        $('#approveBasicInfo_all').hide();
        $('#companyExpendModalConfirmOrRemarkBTN_all').attr('onclick', "_saveConfirmCompanyExpendResultAll();");
        $('#remarkForCompanyExpend_modal_all').modal('show');
    }
}
function _saveConfirmCompanyExpendResultAll() {
    var taskId = $('#remarkForCompanyExpend_taskId_all').val();
    var remark = null;
    if ($('#remarkForCompanyExpend_all').val()) {
        remark = $('#remarkForCompanyExpend_all').val();
    }
    remark += "(" + localHostIp + ")";
    if (taskId) {
        bootbox.confirm("是否确认最终结果？", function (res) {
            if (res) {
                $.ajax({
                    type: 'put',
                    url: '/r/out/confirmCompanyExpendResult',
                    dataType: 'json',
                    data: {"taskId": taskId, "remark": remark},
                    success: function (res) {
                        if (res) {
                            if (res.status == 1) {
                                $('#remarkForCompanyExpend_modal_all').modal('hide');
                                _queryCompanyExpenditureAll();
                            }
                            $.gritter.add({
                                time: 1500,
                                class_name: '',
                                title: '系统消息',
                                text: res.message,
                                image: '../images/message.png'
                            });
                        }
                    }
                });
            }
        });
    }
}
/**审批*/
function _beforeApproveCompanyExpenditureAll(reqId, handicap, amount, purpose, toAccount, toOwner, toBank) {
    $('#remarkForCompanyExpend_all').val('');
    $('#remarkForCompanyExpend_reqId_all').val(reqId);
    $('#handicapToUse_all').text(handicap);
    $('#amountToUse_all').text(amount);
    $('#purposeToUse_all').text(purpose);
    $('#toAccountToUse_all').text(toAccount);
    $('#toOwnerToUse_all').text(toOwner);
    $('#toBankToUse_all').text(toBank);
    $('#remarkForCompanyExpend_all').val('');
    $('#titleDivCustomer_all').hide();
    $('#titleDivApprove_all').show();
    $('#titleDivEdit_all').hide();
    $('#approveBasicInfo_all').show();
    $('#companyExpendModalConfirmOrRemarkBTN_all').attr('onclick', "_confirmApproveCompanyExpendAll();")
    $('#remarkForCompanyExpend_modal_all').modal('show');
}
/**确定审批*/
function _confirmApproveCompanyExpendAll() {
    var reqId = $('#remarkForCompanyExpend_reqId_all').val();
    var remark = $('#remarkForCompanyExpendAll').val();
    if (!remark){
        $('#prompt_remarkForCompanyExpend_all').show(10).delay(2000).hide(10);
        return ;
    }
    if (reqId) {
        bootbox.confirm("确定审批吗？", function (res) {
            if (res) {
                $.ajax({
                    type: 'put',
                    url: '/r/out/approveHandicapExpend',
                    data: {"reqId": reqId, "remark": $.trim(remark), "localHostIp": localHostIp},
                    dataType: 'json',
                    success: function (res) {
                        if (res) {
                            $.gritter.add({
                                time: 1500,
                                class_name: '',
                                title: '系统消息',
                                text: res.message,
                                image: '../images/message.png'
                            });
                            $('#remarkForCompanyExpend_modal_all').modal('hide');
                            _queryCompanyExpenditureAll();
                        }
                    }
                });
            }
        });
    }
}

/**加备注*/
function _addRemarksCompanyExpendAll(taskId, reqId) {
    $('#remarkForCompanyExpendAll').val('');
    $('#remarkForCompanyExpend_taskId_all').val(taskId);
    $('#remarkForCompanyExpend_reqId_all').val(reqId);
    $('#titleDivCustomer_all').show();
    $('#titleDivApprove_all').hide();
    $('#approveBasicInfo_all').hide();
    $('#companyExpendModalConfirmOrRemarkBTN_all').attr('onclick', "_saveRemarkForCompanyExpendAll();");
    $('#remarkForCompanyExpend_modal_all').modal('show');
}
/**保存备注*/
function _saveRemarkForCompanyExpendAll() {
    if (!$.trim($('#remarkForCompanyExpendAll').val())) {
        $('#prompt_remarkForCompanyExpend_all').show(10).delay(2000).hide(10);
        return;
    }
    var remark = $.trim($('#remarkForCompanyExpendAll').val());
    var taskId = $('#remarkForCompanyExpend_taskId_all').val();
    var reqId = $('#remarkForCompanyExpend_reqId_all').val();
    $.ajax({
        type: 'put',
        url: '/r/out/addRemarkCompanyExpend',
        dataType: 'json',
        data: {"remark": remark, "taskId": taskId, "reqId": reqId},
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 1500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    image: '../images/message.png'
                });
                if (res.status == 1) {
                    $('#remarkForCompanyExpend_modal_all').modal('hide');
                    _queryCompanyExpenditureAll();
                }
            }
        }
    });
}
/**总记录*/
function _getCompanyExpendCountAll(data) {
    $.ajax({
        type: 'get',
        url: '/r/out/countCompanyExpend',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#companyExpendCurrentCount_all').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#companyExpendAllCount_all').empty().text('合计：' + res.page.totalElements + '条记录');
                    showPading(res.page, 'footPage_companyExpence_all', _queryCompanyExpenditureAll);
                }
            }
        }
    });
}
/**总金额*/
function _getCompanyExpendSumAll(data) {
    $.ajax({
        type: 'get',
        url: '/r/out/sumCompanyExpend',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#companyExpendSum_all').text(parseFloat(res.data).toFixed(3));
                }
            }
        }
    });
}
function _companyImgPhotoAll(url) {
    $('#companyImg_all').attr('src', url);
    $('#companyImg_all').attr('download', url);
    $('#companyImg_all').attr('href', url);
    $('#companyImgModal_all').modal('show');
    if (browserIsIe()) {
        //是ie等,绑定事件
        $('#downLoadImgCompanyExpendBtn_all').on("click", function () {
            var imgSrc = $(this).siblings("img").attr("src");
            //调用创建iframe的函数
            _downLoadReportIMGAll(url);
        });
    } else {
        $('#downLoadImgCompanyExpendBtn_all').attr("download", url);
        $('#downLoadImgCompanyExpendBtn_all').attr("href", url);
    }
}
function _checkOutTypeAll(taskOperator, outAccountId) {
    var type = '';
    if (outAccountId) {
        if (taskOperator) {
            type = "人工";
        } else {
            type = "机器";
        }
    }
    return type;
}
function _showStatuMsgAll(taskStatus) {
    //taskStatus  0 未出款 7无效记录  5 流水匹配
    //reqStatus   0 待审核 1已审核 3已取消    5出款成功，平台已确认  6出款成功，与平台确认失败
    var status = '';
    if (taskStatus == '待审核') {
        status = '待审核';
    } else if (taskStatus == '流水匹配') {
        status = '待确认';
    } else {
        status = taskStatus;
    }
    return status;
}
/**重置*/
function _resetValueCompanyExpenditureAll() {
    $('#companyExpenditurePurpose1_all').val('');
    $('#accountOut_companyExpence_all').val('');
    _initialHandicapAndPurposeInCompanyExpendAll();
    _datePickerForAll($('#timeCreate_companyExpence_all'));
    $('input[name="Money_companyExpence_all"]').val('');
    $('input[name="type_companyExpence_all"]:checked').prop('checked', '');
    _queryCompanyExpenditureAll();
}
/**初始化盘口*/
function _initialHandicapAndPurposeInCompanyExpendAll() {
    //可查询的单选框 调用chosen 初始化
    $('.chosen-select').chosen({
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    _gethandicapCompanyExpendAll();
    //$('#companyExpenditureHandicap_all_chosen').prop('style', 'width: 78%;');
    var purposeOpt = '<option selected="selected" value="请选择">请选择</option>';
    $.each(companyExpencePurpose, function (i, v) {
        purposeOpt += '<option value="' + v + '" >' + v + '</option>';
    });
    //$('#companyExpenditurePurposeListAll').empty().html(purposeOpt);
    $('#companyExpenditurePurposeListAll').empty().html(purposeOpt);
    var purposeOpt1 = '<option selected="selected" value="请选择">请选择</option>';
    $.each(companyExpencePurposeOperationCharges, function (i, v) {
        purposeOpt1 += '<option value="' + v + '" >' + v + '</option>';
    });
    $('#companyExpenditurePurposeList1_all').empty().html(purposeOpt1);
    $('#companyExpenditurePurposeListChildAll').empty().html(purposeOpt1);

}
/**
 * 获取盘口信息
 */
function _gethandicapCompanyExpendAll() {
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
                        opt += '<option value="' + $.trim(val.id) + '">' + val.name + '</option>';
                    });
                    $('#companyExpenditureHandicap_all').empty().html(opt);
                    $('#companyExpenditureHandicap_all').trigger('chosen:updated');
                    $('#handicap_companyExpence_all').empty().html(opt);
                    $('#handicap_companyExpence_all').trigger('chosen:updated');
                }
            }
        }
    });
}
/**查找审核人*/
function _findUsersForCompanyAuditor(reviewer) {
    $.ajax({
        type:'get',
        url:'/r/out/findUsersForCompanyAuditor',
        dataType:'json',
        async:false,
        data:{"handicapId":""},
        success:function (res) {
            if (res){
                if (res.status==1){
                    var opt ='<option>请选择</option>';
                    if(res.data && res.data.length>0){
                        $.each(res.data,function (i,v) {
                            if (reviewer && reviewer==v.id){
                                opt +='<option value="'+v.id+'" selected="selected">'+v.uid+'</option>';
                            }else{
                                opt +='<option value="'+v.id+'">'+v.uid+'</option>';
                            }
                        });
                        $('#companyExpenditureAuditorAll').empty().html(opt);
                    }
                }
            }
        }
    });
}
function _showStatuMsg(taskStatus) {
    //taskStatus  0 未出款 7无效记录  5 流水匹配
    //reqStatus   0 待审核 1已审核 3已取消    5出款成功，平台已确认  6出款成功，与平台确认失败
    var status = '';
    if (taskStatus == '待审核') {
        status = '待审核';
    } else if (taskStatus == '流水匹配') {
        status = '待确认';
    } else {
        status = taskStatus;
    }
    return status;
}
//删除公司用款
function _beforeDelete(reqId) {
    if (reqId){
        //删除 备注
        $('#remarkForCompanyExpend_all').val('');
        $('#remarkForCompanyExpend_taskId_all').val('');
        $('#remarkForCompanyExpend_reqId_all').val(reqId);
        $('#remarkForCompanyExpendAll').val('');
        $('#titleDivCustomer_all').hide();
        $('#titleDivApprove_all').hide();
        $('#titleDivEdit_all').show();
        $('#approveBasicInfo_all').hide();
        $('#companyExpendModalConfirmOrRemarkBTN_all').attr('onclick', "_deleteCompanyExpenditure();");
        $('#remarkForCompanyExpend_modal_all').modal('show');
    }
}
// 删除2 公司用款
function _deleteCompanyExpenditure() {
    var remark =  $('#remarkForCompanyExpendAll').val();
    if (!remark){
        $('#prompt_remarkForCompanyExpend_all').show(10).delay(2000).hide(10);
        return ;
    }
    var reqId = $('#remarkForCompanyExpend_reqId_all').val();
    $.ajax({
        type:'put',
        url:'/r/out/delete',
        dataType:'json',
        data:{"remark":remark,"reqId":reqId,"localHostIp":localHostIp},
        success:function (res) {
            if (res){
                $.gritter.add({
                    time: 1500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    image: '../images/message.png'
                });
                $('#remarkForCompanyExpend_modal_all').modal('hide');
                _queryCompanyExpenditureAll();
            }
        }
    });

}
//编辑公司用款
function _beforeEdit(id,amount,purpose,toAccount,toAccountOwner,toAccountBank,reviwer,handicapId) {
    if (id){
        handicapForAllEdit = handicapId;
        $('#addCompanyExpenditureTitleAll').hide();
        $('#editCompanyExpenditureTitleAll').show();
        $('#remarkOrHandicapAll').empty().html('<div class="form-group">' +
            '<label style="margin-right:3px;">编辑备注<i class="fa fa-asterisk red"></i></label>'+
            '<input style="width: 220px;" class="input-sm" id="companyExpenditureEditRemarkAll" type="text" placeholder="编辑备注">' +
            '</div>');
        $('#companyExpenditureAmountAll').val(amount);
        $('#companyExpenditureToAccountAll').val(toAccount);
        $('#companyExpenditureToBankAll').val(toAccountBank);
        $('#companyExpenditureToNameAll').val(toAccountOwner);
        $('#companyExpenditurePurposeAll').val(purpose.split('-')[0]);
        $('#companyExpenditurePurposeChildAll').val(purpose.split('-')[1]);
        $('#editCompanyExpenditrueReqIdAll').val(id);
        $('#companyExpenditureEditRemark').val('');
        _findUsersForCompanyAuditor(reviwer);
        $('#addCompanyExpenditureModalAll').modal('show');
        $('#addOreditCompanyExpenditrueAll').attr('onclick',"_confirmCompanyExpenditureAll('edit');");
    }
}
_findUsersForCompanyAuditor("");
_datePickerForAll($('#timeCreate_companyExpence_all'));
_initialHandicapAndPurposeInCompanyExpendAll();
_queryCompanyExpenditureAll();
$('#handicap_companyExpence_all_chosen').attr('style', 'width:190px;');
$('#companyExpencePurpose_all_chosen').attr('style', 'width:190px;');
