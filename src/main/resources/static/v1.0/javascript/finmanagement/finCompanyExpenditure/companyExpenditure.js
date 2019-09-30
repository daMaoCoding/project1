currentPageLocation = window.location.href;
var addCompanyExpendFlag = false;
var handicapIdForDetail = null;
var handicapNameForAdd = null;
var data = {};
var addCompanyOrderFlag = null;//标识每一次新增公司用款,用于防止重复提交
(function () {
    $.each(ContentRight['financeCompanyExpenditure:*'], function (name, value) {
        if (name == 'financeCompanyExpenditure:addCompanyExpend:*') {
            addCompanyExpendFlag = true;
        }
    })
    if (addCompanyExpendFlag) {
        $('#addOrderBtn_cashExpence').show();
    } else {
        $('#addOrderBtn_cashExpence').hide();
    }
})();
//编辑公司用款
function _beforeEdit(id, amount, purpose, toAccount, toAccountOwner, toAccountBank, reviwer) {
    if (id) {
        var flag = _beforeAction(4);
        if (!flag){
            return ;
        }
        $('#addCompanyExpenditureTitle').hide();
        $('#editCompanyExpenditureTitle').show();
        $('#remarkOrHandicap').empty().html('<div class="form-group">' +
            '<label style="margin-right:3px;">编辑备注<i class="fa fa-asterisk red"></i></label>' +
            '<input style="width: 220px;" class="input-sm" id="companyExpenditureEditRemark" type="text" placeholder="编辑备注">' +
            '</div>');
        $('#companyExpenditureAmount').val(amount);
        $('#companyExpenditureToAccount').val(toAccount);
        $('#companyExpenditureToBank').val(toAccountBank);
        $('#companyExpenditureToName').val(toAccountOwner);
        $('#companyExpenditurePurpose').val(purpose.split('-')[0]);
        $('#companyExpenditurePurposeChild').val(purpose.split('-')[1]);
        $('#editCompanyExpenditrueReqId').val(id);
        _findUsersForCompanyAuditor(reviwer);
        $('#addCompanyExpenditureModal').modal('show');
        $('#addOreditCompanyExpenditrue').attr('onclick', '_confirmCompanyExpenditure("edit");');
    }
}
//删除公司用款
function _beforeDelete(reqId) {
    if (reqId) {
        var flag = _beforeAction(3);
        if (!flag){
            return ;
        }
        //删除 备注
        $('#remarkForCompanyExpend').val('');
        $('#remarkForCompanyExpend_taskId').val('');
        $('#remarkForCompanyExpend_reqId').val(reqId);
        $('#titleDivCustomer').hide();
        $('#titleDivApprove').hide();
        $('#titleDivEdit').show();
        $('#approveBasicInfo').hide();
        $('#companyExpendModalConfirmOrRemarkBTN').attr('onclick', "_deleteCompanyExpenditure();");
        $('#remarkForCompanyExpend_modal').modal('show');
    }
}
// 删除2 公司用款
function _deleteCompanyExpenditure() {
    var remark = $('#remarkForCompanyExpend').val();
    if (!remark) {
        $('#prompt_remarkForCompanyExpend').show(10).delay(2000).hide(10);
        return;
    }
    var reqId = $('#remarkForCompanyExpend_reqId').val();
    $.ajax({
        type: 'put',
        url: '/r/out/delete',
        dataType: 'json',
        data: {"remark": remark, "reqId": reqId, "localHostIp": localHostIp},
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 1500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    image: '../images/message.png'
                });
                _postAction(3);
                $('#remarkForCompanyExpend_modal').modal('hide');
                _queryCompanyExpenditure();
            }
        }
    });

}

/**新增公司用款*/
function _beforeAddCompanyExpenditure() {
    $('#addCompanyExpenditureTitle').show();
    $('#editCompanyExpenditureTitle').hide();
    $('#remarkOrHandicap').empty().html('<div class="form-group">' +
        '<label style="margin-right:3px;">用款盘口<i class="fa fa-asterisk red"></i></label>' +
        '<input style="width: 220px;" class="input-sm" id="companyExpenditureHandicap" type="text" placeholder="用款盘口">' +
        '</div>');
    $('#companyExpenditureAmount').val('');
    $('#companyExpenditureToAccount').val('');
    $('#companyExpenditureToBank').val('');
    $('#companyExpenditureToName').val('');
    $('#companyExpenditurePurpose').val('');
    $('#companyExpenditurePurposeChild').val('');
    $('#editCompanyExpenditrue').val('');
    $('#companyExpenditureHandicap').val(handicapNameForAdd);
    $('#companyExpenditureHandicap').prop('readonly', 'readonly');
    //_initialHandicapAndPurposeInCompanyExpend();
    _findUsersForCompanyAuditor("");
    $('input[name="type_companyExpenditure"]:checked').prop('checked', '');
    $('#addCompanyExpenditureModal').modal('show');
    $('#addOreditCompanyExpenditrue').attr('onclick', '_confirmCompanyExpenditure("add");');
    addCompanyOrderFlag = (new Date()).valueOf();
}
/**查找审核人*/
function _findUsersForCompanyAuditor(reviewer) {
    $.ajax({
        type: 'get',
        url: '/r/out/findUsersForCompanyAuditor',
        dataType: 'json',
        async: false,
        data: {"handicapId": handicapIdForDetail},
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    var opt = '<option>请选择</option>';
                    if (res.data && res.data.length > 0) {
                        $.each(res.data, function (i, v) {
                            if (reviewer && reviewer == v.id) {
                                opt += '<option value="' + v.id + '" selected="selected">' + v.uid + '</option>';
                            } else {
                                opt += '<option value="' + v.id + '">' + v.uid + '</option>';
                            }
                        });
                        $('#companyExpenditureAuditor').empty().html(opt);
                    }
                }
            }
        }
    });
}
/**确认新增*/
function _confirmCompanyExpenditure(flag) {
    $('#promptCompany').text('').hide();
    if (!$('#companyExpenditureAmount').val()) {
        $('#promptCompany').text('请填写出款金额').show(10).delay(1500).hide(10);
        return;
    }
    if (!$.trim($('#companyExpenditureToAccount').val())) {
        $('#promptCompany').text('请填写对方账号').show(10).delay(1500).hide(10);
        return;
    }
    if (!$.trim($('#companyExpenditurePurpose').val()) || $('#companyExpenditurePurpose').val() == '请选择') {
        $('#promptCompany').text('请填写用款来源大类').show(10).delay(1500).hide(10);
        return;
    }
    if (!$.trim($('#companyExpenditurePurposeChild').val()) || $('#companyExpenditurePurposeChild').val() == '请选择') {
        $('#promptCompany').text('请填写用款来源细分').show(10).delay(1500).hide(10);
        return;
    }
    if (!$('#companyExpenditureToName').val()) {
        $('#promptCompany').text('请填写对方姓名(开户名)').show(10).delay(1500).hide(10);
        return;
    }
    if (flag == 'add' && !$('#companyExpenditureHandicap').val() || $('#companyExpenditureHandicap').val() == '请选择') {
        $('#promptCompany').text('请选择盘口').show(10).delay(1500).hide(10);
        return;
    }
    if (!$('#companyExpenditureToBank').val()) {
        $('#promptCompany').text('请填写开户行').show(10).delay(1500).hide(10);
        return;
    }
    if (!$.trim($('#companyExpenditureAuditor').val()) || $.trim($('#companyExpenditureAuditor').val())=='请选择') {
        $('#promptCompany').text('请选择审核人').show(10).delay(1500).hide(10);
        return;
    }
    var amount = $.trim($('#companyExpenditureAmount').val());
    var toAccount = $.trim($('#companyExpenditureToAccount').val());
    var toBank = $.trim($('#companyExpenditureToBank').val());
    var toName = $.trim($('#companyExpenditureToName').val());
    var purpose = $.trim($('#companyExpenditurePurpose').val()) + '-' + $.trim($('#companyExpenditurePurposeChild').val());
    var auditorId = $.trim($('#companyExpenditureAuditor').val());
    var editCompanyExpenditrueReqId = '';
    var remark = "";
    var msg = '';
    if (flag == 'add') {
        msg = '确定新增用款吗？';
        if(!addCompanyOrderFlag){
            return ;
        }
    }
    if (flag == 'edit') {
        editCompanyExpenditrueReqId = $('#editCompanyExpenditrueReqId').val();
        handicap = handicapIdForDetail;
        remark = $.trim($('#companyExpenditureEditRemark').val());
        if (!remark) {
            $('#promptCompany').text('请填写备注').show(10).delay(1500).hide(10);
            return;
        }
        msg = '确定保存编辑用款吗？';
    }
    //收款账号信息去除空格
    var data = {
        "handicap": handicapIdForDetail,
        "amount": $.trim(amount),
        "toAccount": $.trim(toAccount),
        "toBank": $.trim(toBank),
        "toName": $.trim(toName),
        "purpose": $.trim(purpose),
        "localHostIp": localHostIp,
        "auditorId": auditorId,
        "reqId": editCompanyExpenditrueReqId,
        "remark": remark,
        "timeFlag":addCompanyOrderFlag
    };
    bootbox.confirm(msg, function (res) {
        if (res) {
            $.ajax({
                type: 'post',
                url: '/r/out/addCompanyExpend',
                data: data,
                async: false,
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
                            addCompanyOrderFlag = null;
                            $('#addCompanyExpenditureModal').modal('hide');
                            _queryCompanyExpenditure();
                        }
                        if (flag=='edit'){
                           _postAction(4);
                        }
                    }
                }
            });
        }
    });
}
function _initialParamDataAndQuery() {
    var params = currentPageLocation.split('*?')[1].split('&');
    var handicapId = params[0].split('=')[1];
    handicapNameForAdd = params[1].split('=')[1];
    handicapIdForDetail = handicapId;
    var purpose = '';
    if ($('#companyExpenditurePurpose1').val() && $('#companyExpenditurePurpose1').val() != '请选择') {
        purpose = $('#companyExpenditurePurpose1').val();
    }
    var type = null;
    if ($.trim($('input[name="type_companyExpence"]:checked').val())) {
        type = $.trim($('input[name="type_companyExpence"]:checked').val());
    }
    var amountStart = '';
    var amountEnd = '';
    if ($.trim($('#fromMoney_companyExpence').val())) {
        amountStart = $.trim($('#fromMoney_companyExpence').val());
    }
    if ($.trim($('#toMoney_companyExpence').val())) {
        amountEnd = $.trim($('#toMoney_companyExpence').val());
    }
    var outAccount = '';
    if ($.trim($('#accountOut_companyExpence').val())) {
        outAccount = $.trim($('#accountOut_companyExpence').val());
    }
    var CurPage = $("#footPage_companyExpence").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    data.pageNo = CurPage;
    data.pageSize = $.session.get('initPageSize');
    data.handicapId = $.trim(handicapIdForDetail);
    data.outAccount = outAccount;
    data.purpose = purpose;
    data.amountStart = $.trim(amountStart);
    data.amountEnd = $.trim(amountEnd);
    data.type =type ? $.trim(type) : null;
    var startAndEndTime = $('#timeCreate_companyExpence').val();
    var startTime = '';
    var endTime = '';
    if (startAndEndTime) {
        if (startAndEndTime.indexOf('~') > 0) {
            startAndEndTime = startAndEndTime.split('~');
            startTime = startAndEndTime[0];
            endTime = startAndEndTime[1];
        }
    }else{
        if (params.length>2){
            startTime = params[2].split('=')[1];
            endTime = params[3].split('=')[1];
        }
    }
    data.startTime =$.trim(decodeURI(startTime));
    data.endTime =$.trim(decodeURI(endTime));
    _queryCompanyExpenditure();
}
/**查询*/
function _queryCompanyExpenditure() {
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
        } else if (name == 'financeCompanyExpenditure:editCompanyExpend:*') {
            editFlag = true;
        } else if (name == 'financeCompanyExpenditure:deleteCompanyExpend:*') {
            deleteFlag = true;
        }
    });
    $.ajax({
        type: 'get',
        url: '/r/out/queryCompanyExpend',
        dataType: 'json',
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
                                // '<td>' + _checkObj(v.handicap) + '</td>' +
                                '<td>' + _checkObj(v.purpose) + '</td>' +
                                '<td>' + _checkObj(v.orderNo) + '</td>' +
                                '<td>' + (_checkObj(v.taskAmount) ? v.taskAmount : _checkObj(v.amount)) + '</td>' +
                                '<td>' + _checkOutType(v.taskOperator, v.outAccountId) + '</td>';
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
                            tr += '<td>' + ( v.taskStatus  ? _showStatuMsg(v.taskStatus) : v.reqStatus) + '</td>' +
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
                                        '<a href="javascript:void(0);" id="taskPhoto" onclick="_companyImgPhoto(\'' + window.location.origin + '\/' + _checkObj(v.screenshot) + '\')">查看</a>' +
                                        '</td>';
                                } else {
                                    tr += '<td>' +
                                        '<a href="javascript:void(0);" id="taskPhoto" onclick="_companyImgPhoto(\'' + window.location.origin + '\/' + _checkObj(v.screenshot) + '\')">回执</a>' +
                                        '</td>';
                                }
                            } else {
                                tr += '<td></td>';
                            }
                            tr += '<td style="width:200px;">';
                            if (v.reqStatus== '正在审核' && approveFlag) {
                                tr += '<button  id="approveBtnForCompanyExpenditure" class="btn btn-xs btn-white btn-info btn-bold "  type="button" onclick="_beforeApproveCompanyExpenditure(' + v.reqId + ',\'' + v.handicap + '\',' + (v.amount == v.taskAmount ? v.taskAmount : v.amount) + ',\'' + v.purpose + '\',\'' + v.toAccount + '\',\'' + v.toAccountOwner + '\',\'' + v.toAccountBank + '\');">' +
                                    '<i class="fa  fa-gavel"></i>审批' +
                                    '</button>';
                            } else if ((_showStatuMsg(v.taskStatus) == '待确认' || v.taskStatus == '待排查' || v.taskStatus == '已出款' || v.taskStatus == '主管处理' ) && confirmFlag && v.taskId && !(remark.indexOf("执行确认") > -1)) {
                                tr += '<button type="button" onclick="_confirmCompanyExpendResult(' + v.taskId + ');" class="btn btn-xs btn-white btn-purple btn-bold "><i class="ace-icon fa fa-check bigger-100 blue"></i>确认</button>';
                            }
                            if (addRemarkFlag) {
                                if (v.taskId) {
                                    tr += '<button type="button" id="remarkBtnForCompanyExpenditure" onclick="_addRemarksCompanyExpend(' + v.taskId + ',' + v.reqId + ');" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                                } else {
                                    tr += '<button type="button" id="remarkBtnForCompanyExpenditure"  onclick="_addRemarksCompanyExpend(null,' + v.reqId + ');" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                                }
                            }
                            if (UploadReceipt && _checkObj(v.screenshot)) {
                                tr += '<button type="button" onclick="_uploadReceiptPhotoCompanyExpend(' + v.taskId + ');" class="btn btn-xs btn-white btn-success btn-bold "><i class="fa fa-arrow-circle-up bigger-100 green"></i>回执</button>';
                            }
                            if (v.reqStatus == '正在审核' && editFlag) {
                                tr += '<button id="editBtnForCompanyExpenditure" type="button" onclick="_beforeEdit(' + v.reqId + ',' + v.amount + ',\'' + v.purpose + '\',\'' + v.toAccount + '\',\'' + v.toAccountOwner + '\',\'' + v.toAccountBank + '\',\'' + v.reviewerId + '\');" class="btn btn-xs btn-white btn-primary btn-bold "><i class="ace-icon fa fa-pencil-square-o bigger-110 green"></i>编辑</button>';
                            }
                            if (v.reqStatus == '正在审核' && deleteFlag) {
                                tr += '<button id="deleteBtnForCompanyExpenditure" type="button" onclick="_beforeDelete(' + v.reqId + ');" class="btn btn-xs btn-white btn-danger btn-bold "><i class="ace-icon fa fa-remove bigger-110 red"></i>删除</button>';
                            }
                            tr += '</td>';
                        });
                        trs +=
                            '<tr>' +
                            '<td id="companyExpendCurrentCount" colspan="2">小计：统计中..</td>' +
                            '<td bgcolor="#579EC8" style="color:white;width: 100px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                            '<td colspan="15"></td></tr>' +
                            '<tr>' +
                            '<td id="companyExpendAllCount" colspan="2">总共：统计中..</td>' +
                            '<td id="companyExpendSum" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td><td colspan="15"></td></tr>';
                    }
                    $('#tbody_companyExpence').empty().html(tr).append(trs);
                    //加载账号悬浮提示
                    _getCompanyExpendCount(data);
                    _getCompanyExpendSum(data);
                    $("[data-toggle='popover']").popover();
                    if (idList && idList.length > 0) {
                        loadHover_accountInfoHover(idList);
                    }
                } else {
                    $('#tbody_companyExpence').html('<tr><td colspan="15" style="text-align: center"><h3>' + res.message + ',请稍后。</h3></td></tr>');
                }
                showPading(res.page, 'footPage_companyExpence', _initialParamDataAndQuery);
            } else {
                $('#tbody_companyExpence').html('<tr><td colspan="15" style="text-align: center"><h3>网络异常,请稍后。</h3></td></tr>');
            }

        }
    });
}
function _uploadReceiptPhotoCompanyExpend(taskId) {
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
                                _queryCompanyExpenditure();
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
function _confirmCompanyExpendResult(taskId) {
    if (taskId) {
        $('#remarkForCompanyExpend').val('');
        $('#remarkForCompanyExpend_taskId').val(taskId);
        $('#remarkForCompanyExpend_reqId').val('');
        $('#titleDivCustomer').show();
        $('#titleDivApprove').hide();
        $('#titleDivEdit').hide();
        $('#approveBasicInfo').hide();
        $('#companyExpendModalConfirmOrRemarkBTN').attr('onclick', "_saveConfirmCompanyExpendResult();");
        $('#remarkForCompanyExpend_modal').modal('show');
    }
}
function _saveConfirmCompanyExpendResult() {
    var taskId = $('#remarkForCompanyExpend_taskId').val();
    var remark = null;
    if ($('#remarkForCompanyExpend').val()) {
        remark = $('#remarkForCompanyExpend').val();
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
                                $('#remarkForCompanyExpend_modal').modal('hide');
                                _queryCompanyExpenditure();
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
function _beforeApproveCompanyExpenditure(reqId, handicap, amount, purpose, toAccount, toOwner, toBank) {
    var flag = _beforeAction(1);
    if (!flag){
        return ;
    }
    $('#remarkForCompanyExpend').val('');
    $('#remarkForCompanyExpend_reqId').val(reqId);
    $('#handicapToUse').text(handicap);
    $('#amountToUse').text(amount);
    $('#purposeToUse').text(purpose);
    $('#toAccountToUse').text(toAccount);
    $('#toOwnerToUse').text(toOwner);
    $('#toBankToUse').text(toBank);

    $('#titleDivCustomer').hide();
    $('#titleDivApprove').show();
    $('#titleDivEdit').hide();
    $('#approveBasicInfo').show();
    $('#companyExpendModalConfirmOrRemarkBTN').attr('onclick', "_confirmApproveCompanyExpend();")
    $('#remarkForCompanyExpend_modal').modal('show');
}
/**确定审批*/
function _confirmApproveCompanyExpend() {
    var reqId = $('#remarkForCompanyExpend_reqId').val();
    var remark = $('#remarkForCompanyExpend').val();
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
                            _postAction(1);
                            $('#remarkForCompanyExpend_modal').modal('hide');
                            _queryCompanyExpenditure();
                        }
                    }
                });
            }
        });
    }
}

/**加备注*/
function _addRemarksCompanyExpend(taskId, reqId) {
    var flag = _beforeAction(2);
    if (!flag){
        return ;
    }
    $('#remarkForCompanyExpend').val('');
    $('#remarkForCompanyExpend_taskId').val(taskId ? taskId : "");
    $('#remarkForCompanyExpend_reqId').val(reqId);
    $('#titleDivCustomer').show();
    $('#titleDivApprove').hide();
    $('#titleDivEdit').hide();
    $('#approveBasicInfo').hide();
    $('#companyExpendModalConfirmOrRemarkBTN').attr('onclick', "_saveRemarkForCompanyExpend();");
    $('#remarkForCompanyExpend_modal').modal('show');
}
/**保存备注*/
function _saveRemarkForCompanyExpend() {
    if (!$.trim($('#remarkForCompanyExpend').val())) {
        $('#prompt_remarkForCompanyExpend').show(10).delay(2000).hide(10);
        return;
    }
    var remark = $.trim($('#remarkForCompanyExpend').val());
    var taskId = $('#remarkForCompanyExpend_taskId').val();
    var reqId = $('#remarkForCompanyExpend_reqId').val();
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
                _postAction(2);
                $('#remarkForCompanyExpend_modal').modal('hide');
                _queryCompanyExpenditure();
            }
        }
    });
}
/**总记录*/
function _getCompanyExpendCount(data) {
    $.ajax({
        type: 'get',
        url: '/r/out/countCompanyExpend',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#companyExpendCurrentCount').empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#companyExpendAllCount').empty().text('合计：' + res.page.totalElements + '条记录');
                    showPading(res.page, 'footPage_companyExpence', _initialParamDataAndQuery);
                }
            }
        }
    });
}
/**总金额*/
function _getCompanyExpendSum(data) {
    $.ajax({
        type: 'get',
        url: '/r/out/sumCompanyExpend',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#companyExpendSum').text(parseFloat(res.data).toFixed(3));
                }
            }
        }
    });
}
function _companyImgPhoto(url) {
    $('#companyImg').attr('src', url);
    $('#companyImg').attr('download', url);
    $('#companyImg').attr('href', url);
    $('#companyImgModal').modal('show');
    if (browserIsIe()) {
        //是ie等,绑定事件
        $('#downLoadImgCompanyExpendBtn').on("click", function () {
            var imgSrc = $(this).siblings("img").attr("src");
            //调用创建iframe的函数
            _downLoadReportIMG(url);
        });
    } else {
        $('#downLoadImgCompanyExpendBtn').attr("download", url);
        $('#downLoadImgCompanyExpendBtn').attr("href", url);
    }
}
function _checkOutType(taskOperator, outAccountId) {
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

/**重置*/
function _resetValueCompanyExpenditure() {
    $('#companyExpenditurePurpose1').val('');
    $('#accountOut_companyExpence').val('');
    $('#timeCreate_companyExpence').val('');
    _initialHandicapAndPurposeInCompanyExpend();
    $('input[name="Money_companyExpence"]').val('');
    $('input[name="type_companyExpence"]:checked').prop('checked', '');
    _initialParamDataAndQuery();
}
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
    var purposeOpt = '<option selected="selected" value="请选择">请选择</option>';
    $.each(companyExpencePurpose, function (i, v) {
        purposeOpt += '<option value="' + v + '" >' + v + '</option>';
    });
    $('#companyExpenditurePurposeList').empty().html(purposeOpt);
    var purposeOpt1 = '<option selected="selected" value="请选择">请选择</option>';
    $.each(companyExpencePurposeOperationCharges, function (i, v) {
        purposeOpt1 += '<option value="' + v + '" >' + v + '</option>';
    });
    $('#companyExpenditurePurposeList1').empty().html(purposeOpt1);
    $('#companyExpenditurePurposeListChild').empty().html(purposeOpt1);
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
                    $('#companyExpenditureHandicap').empty().html(opt);
                    $('#companyExpenditureHandicap').trigger('chosen:updated');
                    $('#handicap_companyExpence').empty().html(opt);
                    $('#handicap_companyExpence').trigger('chosen:updated');
                }
            }
        }
    });
}
//取消操作动作 type 1 审批 2 备注 3 删除 4 编辑  5表示按钮可能有两种操作情况
function _cancelAction(type) {
    if(type==1){
        approveFlagForCompanyExpenditure = false;
        $('#editBtnForCompanyExpenditure').removeAttr('disabled');
        $('#deleteBtnForCompanyExpenditure').removeAttr('disabled');
        $('#remarkBtnForCompanyExpenditure').removeAttr('disabled');

    }
    else if(type==2){
        remarkFlagForCompanyExpenditure = false;
        $('#editBtnForCompanyExpenditure').removeAttr('disabled');
        $('#deleteBtnForCompanyExpenditure').removeAttr('disabled');
        $('#approveBtnForCompanyExpenditure').removeAttr('disabled');
    }
    else if(type==3){
        deleteFlagForCompanyExpenditure = false;
        $('#editBtnForCompanyExpenditure').removeAttr('disabled');
        $('#remarkBtnForCompanyExpenditure').removeAttr('disabled');
        $('#approveBtnForCompanyExpenditure').removeAttr('disabled');
    }
    else if(type==4){
        editFlagForCompanyExpenditure = false;
        $('#deleteBtnForCompanyExpenditure').removeAttr('disabled');
        $('#remarkBtnForCompanyExpenditure').removeAttr('disabled');
        $('#approveBtnForCompanyExpenditure').removeAttr('disabled');
    }
    else if(type==5){
        //新增 编辑的模态框同一个所以取消按钮同一个
        //备注 删除 审批 的模态框也是一个所以取消按钮同一个
        if(editFlagForCompanyExpenditure){
            editFlagForCompanyExpenditure = false;
            $('#deleteBtnForCompanyExpenditure').removeAttr('disabled');
            $('#remarkBtnForCompanyExpenditure').removeAttr('disabled');
            $('#approveBtnForCompanyExpenditure').removeAttr('disabled');
        }
        else if(approveFlagForCompanyExpenditure){
            approveFlagForCompanyExpenditure = false;
            $('#editBtnForCompanyExpenditure').removeAttr('disabled');
            $('#deleteBtnForCompanyExpenditure').removeAttr('disabled');
            $('#remarkBtnForCompanyExpenditure').removeAttr('disabled');
        }
        else if(remarkFlagForCompanyExpenditure){
            remarkFlagForCompanyExpenditure = false;
            $('#editBtnForCompanyExpenditure').removeAttr('disabled');
            $('#deleteBtnForCompanyExpenditure').removeAttr('disabled');
            $('#approveBtnForCompanyExpenditure').removeAttr('disabled');
        }
        else if(deleteFlagForCompanyExpenditure){
            deleteFlagForCompanyExpenditure = false;
            $('#editBtnForCompanyExpenditure').removeAttr('disabled');
            $('#remarkBtnForCompanyExpenditure').removeAttr('disabled');
            $('#approveBtnForCompanyExpenditure').removeAttr('disabled');
        }
    }
}
//点击操作动作之前
function _beforeAction(type) {
    if(type==1){
        if(editFlagForCompanyExpenditure||deleteFlagForCompanyExpenditure||remarkFlagForCompanyExpenditure){
            bootbox.alert("该记录暂时无法审核,请稍后");
            return false;
        }else{
            approveFlagForCompanyExpenditure = true;
            $('#editBtnForCompanyExpenditure').attr('disabled','disabled');
            $('#deleteBtnForCompanyExpenditure').attr('disabled','disabled');
            $('#remarkBtnForCompanyExpenditure').attr('disabled','disabled');
            return true;
        }

    }
    if(type==2){
        if(editFlagForCompanyExpenditure||deleteFlagForCompanyExpenditure||approveFlagForCompanyExpenditure){
            bootbox.alert("该记录暂时无法备注,请稍后");
            return false;
        }else{
            remarkFlagForCompanyExpenditure = true;
            $('#editBtnForCompanyExpenditure').attr('disabled','disabled');
            $('#deleteBtnForCompanyExpenditure').attr('disabled','disabled');
            $('#approveBtnForCompanyExpenditure').attr('disabled','disabled'); return true;
        }
    }
    if(type==3){
        if(editFlagForCompanyExpenditure||remarkFlagForCompanyExpenditure||approveFlagForCompanyExpenditure){
            bootbox.alert("该记录暂时无法删除,请稍后");
            return false;
        }else{
            deleteFlagForCompanyExpenditure = true;
            $('#editBtnForCompanyExpenditure').attr('disabled','disabled');
            $('#remarkBtnForCompanyExpenditure').attr('disabled','disabled');
            $('#approveBtnForCompanyExpenditure').attr('disabled','disabled'); return true;
        }
    }
    if(type==4){
        if(remarkFlagForCompanyExpenditure||deleteFlagForCompanyExpenditure||approveFlagForCompanyExpenditure){
            bootbox.alert("该记录暂时无法编辑,请稍后");
            return false;
        }else{
            editFlagForCompanyExpenditure = true;
            $('#remarkBtnForCompanyExpenditure').attr('disabled','disabled');
            $('#deleteBtnForCompanyExpenditure').attr('disabled','disabled');
            $('#approveBtnForCompanyExpenditure').attr('disabled','disabled'); return true;
        }
    }
}
//完成操作动作
function _postAction(type) {
    _cancelAction(type);
}
$('#timeCreate_companyExpence').on('mouseup',function () {
    _datePickerForAll($('#timeCreate_companyExpence'));
});
_initialHandicapAndPurposeInCompanyExpend();
_initialParamDataAndQuery();
$('#handicap_companyExpence_chosen').attr('style', 'width:190px;');
$('#companyExpencePurpose_chosen').attr('style', 'width:190px;');
