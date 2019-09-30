currentPageLocation = window.location.href;
var orderNoQuick = '';//子窗口的订单参数
var memberNameQuick = '';//子窗口的会员名参数
var paramForPageTitle = null;
var startTime='';
var endTime='';
function _getQuickQueryData() {
    var pageParam = window.location.href.split('?')[1];
    if (pageParam.indexOf("&") > -1) {
        pageParam = pageParam.split('&');
        orderNoQuick = pageParam[0].split("=")[1];
        memberNameQuick = pageParam[1].split("=")[1];
        startTime=pageParam[2].split("=")[1];
        endTime=pageParam[3].split("=")[1];
    }
    if (!orderNoQuick && !memberNameQuick){
        return;
    }
    if (orderNoQuick=='null' && memberNameQuick=='null'){
        return;
    }
    paramForPageTitle = orderNoQuick!='null' ? orderNoQuick : memberNameQuick;
    window.document.title = paramForPageTitle;
    _refreshQuickPageAfterAction();
}
/**操作完成后刷新页面*/
function _refreshQuickPageAfterAction() {
    if (!$.trim(orderNoQuick) && !$.trim(memberNameQuick)) {
        return;
    }
    var orderNo = $.trim(orderNoQuick)!='null'?$.trim(orderNoQuick):null;
    var member = $.trim(memberNameQuick)!='null'?$.trim(memberNameQuick):null;
    $.ajax({
        url: '/r/out/quickQuery',
        type: 'get',
        data: {
            "orderNo": orderNo,
            "member": member,'startTime':decodeURIComponent(startTime),'endTime':decodeURIComponent(endTime),
            "pageNo": $('#currentPageNo').val()?0:$('#currentPageNo').val()-1,
            "pageSize": 10
        },
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1&& res.data&& res.data.retList) {
                    var dataQuick = res.data.retList;
                    //_refreshQuickCount(member,orderNo);
                    //_refreshQuickSum(member,orderNo);
                    _initialTableData(dataQuick,res.page);
                }else{
                    bootbox.alert(res.message);
                }
            }
        }
    });
}
function _refreshQuickCount(member,orderNo) {
    $.ajax({
        type:'get',
        url:'/r/out/countQuickQueryForOut',
        data:{"member":member,"orderNo":orderNo,"pageNo": 0, "pageSize": 10},
        dataType:'json',
        success:function (res) {
            if (res){
                if (res.status==1){
                    var pageCount = res.page;
                    $('#quickQueryCurrentCount').empty().text('小计：' + ((pageCount.totalElements - pageCount.pageSize * (pageCount.pageNo)) >= 0 ? pageCount.pageSize : (pageCount.totalElements - pageCount.pageSize * (pageCount.pageNo - 1) )) + '条记录');
                    $('#quickQueryAllCount').empty().text('合计：' + pageCount.totalElements + '条记录');
                    //_pageButton(pageCount);
                    window.opener.showPading(pageCount,"quickQueryFootPage",_refreshQuickPageAfterAction);
                }
            }
        }
    });
}
function _refreshQuickSum(member,orderNo) {
    $.ajax({
        type:'get',
        url:'/r/out/sumQuickQueryForOut',
        data:{"member":member,"orderNo":orderNo},
        dataType:'json',
        success:function (res) {
            if (res){
                if (res.status==1){
                    $('#quickQueryReqSum').text(parseFloat( res.data.reqSum).toFixed(3));
                    $('#quickQueryTaskSum').text(parseFloat(res.data.taskSum).toFixed(3));

                }
            }
        }
    });
}
/**由查询的数据渲染表格*/
function _initialTableData(data,pageCount) {
    var approveInQuickPage = false;//审核权限
    var noticePlatformInQuickPage = false;//通知平台权限
    var cancelTaskInQuickPage = false;//取消任务权限
    var rejectTaskInQuickPage = false;//拒绝任务权限
    var reAllocateTaskInQuickPage = false;//重新分配权限
    var turnTaskToFailInQuickPage = false;//转待排查权限
    var turnToFinishTaskInQuickPage = false;//转完成权限
    var sendMessageTaskInQuickPage = false;//发消息权限
    var reCreateTaskInQuickPage = false;//重新生成任务权限
    var addRemarksInQuickPage = false;//添加备注权限
    var uploadReceiptInQuickPage = false;
    var ReDistribution =false;//正在出款任务再分配
    var cancelDrawing =false;//正在出款任务取消
    var rejectDrawing =false;//正在出款任务拒绝
    //审核权限
    $.each(window.opener.ContentRight['OutwardAuditTotal:*'], function (name, value) {
        if (name == 'OutwardAuditTotal:Audit:*') {
            approveInQuickPage = true;
        }
    });
    //通知平台权限
    $.each(window.opener.ContentRight['OutwardAuditTotal:*'], function (name, value) {
        if (name == 'OutwardAuditTotal:NotifyPlatForm:*') {
            noticePlatformInQuickPage = true;
        }
    });
    $.each(window.opener.ContentRight['OutwardTaskTotal:*'], function (name, value) {
        if (name == 'OutwardTaskTotal:TurningPlatform:*') {
            cancelTaskInQuickPage = true;
        } else if (name == 'OutwardTaskTotal:Freeze:*') {
            rejectTaskInQuickPage = true;
        } else if (name == 'OutwardTaskTotal:Distribution:*') {
            reAllocateTaskInQuickPage = true;
        } else if (name == 'OutwardTaskTotal:ToFailure:*') {
            turnTaskToFailInQuickPage = true;
        } else if (name == 'OutwardTaskTotal:FinishTask:*') {
            turnToFinishTaskInQuickPage = true;
        } else if (name == 'OutwardTaskTotal:SendMessage:*') {
            sendMessageTaskInQuickPage = true;
        } else if (name == 'OutwardTaskTotal:NewGenerationTask:*') {
            reCreateTaskInQuickPage = true;
        } else if (name == 'OutwardTaskTotal:Note:*') {
            addRemarksInQuickPage = true;
        }else if (name == 'OutwardTaskTotal:uploadReceipt:*') {
            uploadReceiptInQuickPage = true;
        }else if (name == 'OutwardTaskTotal:ReDistribution:*'){
            ReDistribution = true;
        }else if (name == 'OutwardTaskTotal:cancelDrawing:*'){
            cancelDrawing = true;
        }else if (name == 'OutwardTaskTotal:rejectDrawing:*'){
            rejectDrawing = true;
        }
    });
    var tr = '';
    var trs = '';
    var amountReq = 0;
    var amountTask = 0;
    if (data && data.length > 0) {
        // [{"orderNo":"22018022392426879","reqStatus":2,"reqUpdateTime":1519365903000,"level":"第一层","handicap":"ysc","reqAmount":17,"remark":"","taskAmount":"","taskAsignTime":"","taskStatus":"noValue","reqId":182836,"reqMember":"hoihoi"}]
        for (var i = 0; i < data.length; i++) {
            amountReq += data[i].reqAmount;
            amountTask += data[i].taskAmount;
            var reqCreateTime = new Date(data[i].reqCreateTime).getTime();
            var reqTime = data[i].reqTimeConsuming?(reqCreateTime+parseInt(data[i].reqTimeConsuming)):reqCreateTime;
            var taskAsignTime = 0;
            if (data[i].taskAsignTime){
                taskAsignTime = data[i].taskAsignTime;
            }
            var taskTimeConsuming = 0;
            if (data[i].taskTimeConsuming){
                taskTimeConsuming = data[i].taskTimeConsuming;
            }
            var taskTime = taskAsignTime?(taskTimeConsuming?(taskAsignTime+taskTimeConsuming):taskAsignTime):0;
            var hasScreenshot = false;
            var hasPhoto = false;
            if (_checkIfNull(data[i].successPhotoUrl)){
                hasPhoto = true;
                var photoArr = (data[i].successPhotoUrl).split('/');
                if (photoArr.length>3){
                    hasScreenshot = photoArr[3].indexOf('screenshot')>-1;
                }else{
                    hasScreenshot = photoArr[2].indexOf('screenshot')>-1;
                }
            }
            tr += '<tr style="text-align: center"><td>' + _checkIfNull(data[i].handicap) + '</td>' +
                '<td>' + _checkIfNull(data[i].level) + '</td>' +
                '<td>' + _checkIfNull(data[i].reqMember) + '</td>'+
                '<td>' + _checkIfNull(data[i].orderNo) + '</td>' +
                '<td>' + _checkIfNull(data[i].reqAmount) + '</td>' +
                '<td>' + _checkIfNull(data[i].taskAmount) + '</td>' +
                '<td>' + window.opener._showReqStatus(data[i].reqStatus) + '</td>' +
                '<td>' + (data[i].taskStatus == 'ZERO' ? (data[i].accountId ? '正在出款' : '未出款') : data[i].taskStatus == 'noValue' ? '' : window.opener._showTaskStatus(data[i].taskStatus)) + '</td>' +
                '<td>' + window.opener.timeStamp2yyyyMMddHHmmss(reqTime) + '</td>' +
                '<td>' + window.opener.timeStamp2yyyyMMddHHmmss(taskTime) + '</td>';

            var reqRemark = '';
            if (data[i].reqRemark){
                reqRemark = data[i].reqRemark;
            }
            var taskRemark = '';
            if (data[i].taskRemark){
                taskRemark = data[i].taskRemark;
            }
            var remarkToShow = taskRemark?taskRemark:(reqRemark?reqRemark:"");
            tr +='<td>';
            if (_checkIfNull(remarkToShow)) {
                if (_checkIfNull(remarkToShow).toString().length > 5) {
                    tr += '<a class="bind_hover_card breakByWord"  title="备注信息"'
                        + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                        + ' data-content="' + window.opener._divideRemarks(remarkToShow) + '">'
                        + _checkIfNull(remarkToShow).toString().substring(0, 5) + '...'
                        + '</a>'
                        + '</td>';
                } else {
                    +'<a class="bind_hover_card breakByWord"  title="备注信息"'
                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                    + ' data-content="' + window.opener._divideRemarks(remarkToShow) + '">'
                    + _checkIfNull(remarkToShow).toString().substring(0, 5)
                    + '</a>'
                    + '</td>';
                }
            } else {
                tr += '</td>';
            }
            if (hasPhoto){
                if (!window.opener.isHideImg) {
                    if (hasScreenshot){
                        tr +='<td>' +
                            '<a href="javascript:void(0);" id="taskPhotoQuick" onclick="_quickQueryPhoto(\''+window.location.origin  +'\/'+_checkIfNull(data[i].successPhotoUrl)+'\')">查看</a>'+
                            '</td>';
                    }else{
                        tr +='<td>' +
                            '<a href="javascript:void(0);" id="taskPhotoQuick" onclick="_quickQueryPhoto(\''+window.location.origin  +'\/'+_checkIfNull(data[i].successPhotoUrl)+'\')">回执</a>'+
                            '</td>';
                    }
                } else{
                    tr +='<td></td>';
                }
            }else{
                tr +='<td></td>';
            }
            tr += '<td>';
            if ((window.opener._showReqStatus(data[i].reqStatus) == '正在审核' || window.opener._showReqStatus(data[i].reqStatus) == '主管处理') && data[i].reqId) {
                if (approveInQuickPage) {
                    if (window.opener._showReqStatus(data[i].reqStatus) == '正在审核') {
                        tr += '<button onclick="_approveInQuickPage(2,' + data[i].reqId + ')"  class="btn btn-xs btn-white btn-info btn-bold pass"><i class="ace-icon fa fa-check bigger-100 blue"></i>审核</button></td></tr>';
                    }
                    if (window.opener._showReqStatus(data[i].reqStatus) == '主管处理') {
                        tr += '<button onclick="_approveInQuickPage(1,' + data[i].reqId + ')"  class="btn btn-xs btn-white btn-info btn-bold pass"><i class="ace-icon fa fa-check bigger-100 blue"></i>审核</button></td></tr>';
                    }
                }
            } else if (window.opener._showReqStatus(data[i].reqStatus) == '出款成功，与平台确认失败'&& !data[i].taskStatus && data[i].reqId) {
                if (noticePlatformInQuickPage) {
                    tr += '<button onclick="_noticePlatformInQuickPage(' + data[i].reqId + ')" class=" btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa  fa-envelope-open-o  bigger-100 green"></i>通知平台</button></td></tr>';
                }
            } else {
                // && !data[i].accountId
                if ((data[i].taskStatus == 'ZERO' || window.opener._showTaskStatus(data[i].taskStatus) == '主管处理') && data[i].taskId) {
                    //主管处理
                    if (window.opener._showTaskStatus(data[i].taskStatus) == '主管处理') {
                        if (cancelTaskInQuickPage) {
                            tr += '<button onclick="_cancelOrRejectTaskInQuickPage(3,' + data[i].taskId + ',' + data[i].reqId + ')" class="btn btn-xs btn-white btn-danger btn-bold"><i class="ace-icon fa fa-remove  bigger-100 red"></i>取消</button>';
                        }
                        if (rejectTaskInQuickPage) {
                            tr += '<button onclick="_cancelOrRejectTaskInQuickPage(4,' + data[i].taskId + ',' + data[i].reqId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-bolt bigger-100 blue"></i>拒绝</button>';
                        }
                        if (reAllocateTaskInQuickPage) {
                            tr += '<button onclick="_reAllocateTaskInQuickPage(' + data[i].taskId + ')" class="btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa fa-user-circle-o  bigger-100 red"></i>分配</button>';
                        }
                        if (turnToFinishTaskInQuickPage) {
                            tr += '<button onclick="_turnToFinishTaskInQuickPage(1,' + data[i].taskId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                        }
                        if (sendMessageTaskInQuickPage) {
                            tr += '<button onclick="_sendMessageTaskInQuickPage(' + data[i].taskId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i>发消息</button>';
                        }
                    }else{
                        //正在出款的
                        // if (cancelDrawing) {
                        //     tr += '<button onclick="_cancelOrRejectTaskInQuickPage(3,' + data[i].taskId + ',' + data[i].reqId + ')" class="btn btn-xs btn-white btn-danger btn-bold"><i class="ace-icon fa fa-remove  bigger-100 red"></i>取消</button>';
                        // }
                        // if (rejectDrawing) {
                        //     tr += '<button onclick="_cancelOrRejectTaskInQuickPage(4,' + data[i].taskId + ',' + data[i].reqId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-bolt bigger-100 blue"></i>拒绝</button>';
                        // }
                        if (ReDistribution) {
                            tr += '<button onclick="_reAllocateTaskInQuickPage(' + data[i].taskId + ')" class="btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa fa-user-circle-o  bigger-100 red"></i>分配</button>';
                        }
                    }
                    if (addRemarksInQuickPage) {
                        tr += '<button onclick="_addRemarks(' + data[i].taskId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                    }

                } else if ((window.opener._showTaskStatus(data[i].taskStatus) == '已出款' || window.opener._showTaskStatus(data[i].taskStatus) == '流水匹配') && data[i].taskId) {
                    if (turnTaskToFailInQuickPage) {
                        tr += '<button onclick="_turnTaskToFailInQuickPage(' + data[i].taskId + ')"  class=" btn btn-xs btn-white btn-danger btn-bold"><i class="ace-icon fa fa-share  bigger-100 red"></i>转待排查</button>';
                    }
                    if (addRemarksInQuickPage) {
                        tr += '<button onclick="_addRemarks(' + data[i].taskId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                    }
                } else if (window.opener._showTaskStatus(data[i].taskStatus) == '转排查' && data[i].taskId) {
                    if (window.opener._showReqStatus(data[i].reqStatus) == '出款成功，与平台确认失败') {
                        if (noticePlatformInQuickPage) {
                            tr += '<button onclick="_noticePlatformInQuickPage(' + data[i].reqId + ')" class=" btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa  fa-envelope-open-o  bigger-100 green"></i>通知平台</button>';
                        }
                    }
                    if (reCreateTaskInQuickPage) {
                        tr += '<button onclick="_reCreateTaskInQuickPage(' + data[i].taskId + ')"  class=" btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa fa-reply  bigger-100 green"></i>重新生成任务</button>';
                    }
                    if (turnToFinishTaskInQuickPage) {
                        tr += '<button onclick="_turnToFinishTaskInQuickPage(2,' + data[i].taskId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                    }
                    if (addRemarksInQuickPage) {
                        tr += '<button onclick="_addRemarks(' + data[i].taskId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                    }
                } else {
                    if (window.opener._showTaskStatus(data[i].taskStatus) !='无效记录，已重新出款' && addRemarksInQuickPage && data[i].taskId) {
                        tr += '<button onclick="_addRemarks(' + data[i].taskId + ')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                    }
                }
            }
            if (!window.opener.isHideImg && hasScreenshot && uploadReceiptInQuickPage && data[i].taskId){
                tr +='<button type="button"  onclick="_uploadReceiptPhotoQuick('+data[i].taskId+');"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                    '<i class="fa fa-arrow-circle-up bigger-100 green"></i>回执</button>';
            }
            tr += '</td></tr>';
        }
        //暂时不分页
        trs += '<tr style="display: none;">' +
            '<td id="quickQueryCurrentCount" colspan="4">小计：统计中..</td>' +
            '<td bgcolor="#579EC8" style="color:white;width: 100px;">'+parseFloat(amountReq).toFixed(3)+'</td>' +
            '<td bgcolor="#579EC8" style="color:white;width: 100px;">'+parseFloat(amountTask).toFixed(3)+'</td>' +
            '<td colspan="15"></td></tr>' +
            '<tr style="display: none;">' +
            '<td id="quickQueryAllCount" colspan="4">总共：统计中..</td>' +
            '<td id="quickQueryReqSum" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td>' +
            '<td id="quickQueryTaskSum" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td>' +
            '<td colspan="15"></td>' +
            '</tr>';
        $('#tbody_QuickQueryData').html(tr).append(trs);
        $('#quickQueryCurrentCount').empty().text('小计：' + ((pageCount.totalElements - pageCount.pageSize * (pageCount.pageNo)) >= 0 ? pageCount.pageSize : (pageCount.totalElements - pageCount.pageSize * (pageCount.pageNo - 1) )) + '条记录');
        $('#quickQueryAllCount').empty().text('合计：' + pageCount.totalElements + '条记录');
        $('#quickQueryReqSum').text(parseFloat(0).toFixed(3));
        $('#quickQueryTaskSum').text(parseFloat(0).toFixed(3));
        $("[data-toggle='popover']").popover();
        window.opener.showPading(pageCount,"quickQueryFootPage",_refreshQuickPageAfterAction);
        //_pageButton(pageCount);
        window.opener._getPathAndHtml('toAudit_quickPage_modal');//加载审核页面
    } else {
        $('#tbody_QuickQueryData').html('<tr><td colspan="15" style="text-align: center"><h3>无数据</h3></td></tr>');
    }
}
function _uploadReceiptPhotoQuick(taskId) {
    if (taskId){
        window.opener.bootbox.confirm("确定上传回执吗？",function (res) {
            if (res){
                $.ajax({
                    type:'put',
                    url:'/r/outtask/uploadReceiptForTask',
                    dataType:'json',
                    data:{"taskId":taskId},
                    success:function (res) {
                        if (res){
                            if (res.status==1){
                                _refreshQuickPageAfterAction();
                            }
                            $.gritter.add({
                                time: 1500,
                                class_name: '',
                                title: '系统消息',
                                text: res.message,
                                sticky: false,
                                image: '../../images/message.png'
                            });
                        }
                    }
                });
            }
        });
    }
}
function _checkIfNull(obj) {
    if (obj){
        return obj;
    }
    return "";
}
/**分页按钮*/
function _pageButton(pageCount) {
    var first = pageCount.first;
    var hasNext = pageCount.hasNext;
    var hasPrevious = pageCount.hasPrevious;
    var last = pageCount.last;
    var nextPageNo = pageCount.nextPageNo;
    var pageNo = pageCount.pageNo;// 1开始
    var previousPageNo = pageCount.previousPageNo;
    var totalPages = pageCount.totalPages;
    $('#totalPagesQuick').val(totalPages);
    $('#previousPageNoQuick').val(previousPageNo);
    $('#nextPageNoQuick').val(nextPageNo);
    $('#currentPageAndTotalPage').text(pageNo+"/"+totalPages );
    $('#currentPageNo').val(pageNo);

    if (first && !hasPrevious){
        //第一页
        $('#step-backward').attr("class","disabled");
        $('#step-left').attr("class","disabled");
        $('#step-backward').find('a').attr("onclick","");
        $('#step-left').find('a').attr("onclick","");
    }else{
        $('#step-backward').attr("class","");
        $('#step-left').attr("class","");
        $('#step-backward').find('a').attr("onclick","_gotoFirstPage()");
        $('#step-left').find('a').attr("onclick","_subtractPageNo();");
    }
    if (last && !hasNext){
        //最后一页
        $('#step-right').attr("class","disabled");
        $('#step-forward').attr("class","disabled");
        $('#step-forward').find('a').attr("onclick","");
        $('#step-right').find('a').attr("onclick","");
    }else{
        $('#step-right').attr("class","");
        $('#step-forward').attr("class","");
        $('#step-forward').find('a').attr("onclick","_gotoLastPage();");
        $('#step-right').find('a').attr("onclick","_addPageNo();");
    }
}
var _subtractPageNo = function () {
    $('#currentPageNo').val($('#previousPageNoQuick').val());
    _refreshQuickPageAfterAction();
};
var _gotoFirstPage = function () {
    _refreshQuickPageAfterAction();
};
var _addPageNo = function () {
    $('#currentPageNo').val($('#nextPageNoQuick').val());
    _refreshQuickPageAfterAction();
};
var _gotoLastPage =function () {
    $('#currentPageNo').val($('#totalPagesQuick').val());
    _refreshQuickPageAfterAction();
};
function _quickQueryPhoto(url) {
    $('#quickQueryImg').attr('src',url);
    $('#quickQueryImgModal').modal('show');
    if (window.opener.browserIsIe()) {
        //是ie等,绑定事件
        $('#downLoadImgBtnQuick').on("click", function() {
            var imgSrc = $(this).siblings("img").attr("src");
            //调用创建iframe的函数
            window.opener._downLoadReportIMG(url);
        });
    } else {
        $('#downLoadImgBtnQuick').attr("download",url);
        $('#downLoadImgBtnQuick').attr("href",url);
    }
}
function _checkNull(obj) {
    window.opener._checkNull(obj);
}
function _showReqStatus(obj) {
    window.opener._showReqStatus(obj);
}
/**审核 type=1 主管处理的审核 type=2 是待审核的记录审核*/
function _approveInQuickPage(type, id) {
    if (type == 1) {
        window.sessionStorage.setItem("auditSubPage", 3);
    } else {
        window.sessionStorage.setItem("auditSubPage", 0);
    }
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
                    $('#toAudit_quickPage_modal  #modal-orderDetail-toAppoveId').val(id);
                    $('#toAudit_quickPage_modal  #modal-orderDetail-orderNoInput').val(res.data.orderNo);
                    $('#toAudit_quickPage_modal  #modal-orderDetail-memberCode').val(res.data.memberCode);
                    $('#toAudit_quickPage_modal  #modal-orderDetail-pageFlag').val(5);
                }
            }
        }
    });
}
/**审核按钮：1 通过 2 取消 3 拒绝 4转主管*/
function _actionInQuickPage(type) {
    var id = $('#toAudit_quickPage_modal  #modal-orderDetail-toAppoveId').val();
    var orderNoInput = $('#toAudit_quickPage_modal  #modal-orderDetail-orderNoInput').val();
    var memberCode = $('#toAudit_quickPage_modal  #modal-orderDetail-memberCode').val();
    var remark = $('#toAudit_quickPage_modal  #modal-orderDetail-remarkForApprove').val();
    if (type == 1) {
        bootbox.confirm('确定通过审核吗？', function (res) {
            if (res) {
                _executeSaveInQuickPage(id, remark, type, orderNoInput, memberCode);
            }
        });
    }
    if (type == 2 || type == 3 || type == 4) {
        if (!remark) {
            $('#modal-orderDetail-remarkPrompt2').show().delay(500).fadeOut();
            return false;
        } else {
            var info = type == 2 ? "确定取消吗？" : type == 3 ? "确定拒绝吗？" : "确定转主管吗？";
            bootbox.confirm(info, function (res) {
                if (res) {
                    _executeSaveInQuickPage(id, remark, type, orderNoInput, memberCode);
                }
            });
        }
    }
}
/**执行操作*/
function _executeSaveInQuickPage(id, remark, type, orderNoInput, memberCode) {
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
                var message = res.message;
                if (type == 2 || type == 3 || type == 4) {
                    message = '取消成功';
                } else if (type == 3) {
                    message = '拒绝成功';
                } else if (type == 4) {
                    message = '转主管成功';
                }
                $.gritter.add({
                    time: 200,
                    class_name: '',
                    title: '系统消息',
                    text: message,
                    sticky: false,
                    image: '../../images/message.png'
                });
                $('#toAudit_quickPage_modal modal-orderDetail-remarkForApprove').val("");
                $('#modal-orderDetail').modal('hide');
                setTimeout(function () {
                    _refreshQuickPageAfterAction();
                }, 500);
            }
            else {
                $.gritter.add({
                    time: 200,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../../images/message.png'
                });
                return false;
            }

        }
    });
}
/**通知平台：添加备注应该也要添加到出款任务记录里*/
function _noticePlatformInQuickPage(id) {
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
                                image: '../../images/message.png'
                            });
                            _refreshQuickPageAfterAction();
                        }
                    }
                });
            }
        })
    }
}

/**取消 type=3 拒绝 type=4 添加备注应该也要添加到出款任务记录里*/
function _cancelOrRejectTaskInQuickPage(type, taskId, reqId) {
    $('#cancelOrReject_remark').val('');
    $('#cancelOrReject_taskId').val(taskId);
    $('#cancelOrReject-reqId').val(reqId);
    $('#button_type').val(type);
    if (type == 4) {
        $('#titleDiv').html('<button type="button" class="close" data-dismiss="modal" aria-hidden="true">' +
            '<span class="white">&times;</span></button>拒绝任务');
    }
    $.ajax({
        type: 'get',
        url: '/r/outtask/getById',
        data: {"id": taskId, "outRequestId": reqId, "type": null},
        async: false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $('#cancelOrReject_orderNo').text(res.data.orderNo);
                    $('#cancelOrReject_handicap').text(res.data.handicap);
                    $('#cancelOrReject_level').text(res.data.level);
                    $('#cancelOrReject_member').text(res.data.member);
                    $('#cancelOrReject_type').text(res.data.type);
                    var asignTime = window&&window.opener?window.opener.timeStamp2yyyyMMddHHmmss(res.data.asignTime):timeStamp2yyyyMMddHHmmss(res.data.asignTime);
                    $('#cancelOrReject_asignTime').text(asignTime);
                    $('#cancelOrReject_amount').val(res.data.amount);

                    $('#cancelOrReject_userId').val(window.opener._checkObj(res.data.userId));
                    $('#cancelOrReject_taskId').val(window.opener._checkObj(res.data.taskId));
                    $('#cancelOrReject_orderNoInput').val(window.opener._checkObj(res.data.orderNo));
                    $('#cancelOrReject_memberCode').val(window.opener._checkObj(res.data.memberCode));
                    $('#modal-quickpage-cancelOrReject').modal('show');
                } else {
                    $.gritter.add({
                        time: 300,
                        class_name: '',
                        title: '系统消息',
                        text: res.message,
                        sticky: false,
                        image: '../../images/message.png'
                    });
                }
            }
        }
    });

}
function _executeCancelOrRejectTask() {
    var taskId = $('#cancelOrReject_taskId').val();
    var remark = $('#cancelOrReject_remark').val();
    var type = $('#button_type').val();
    if (!remark) {
        $('#prompt').show(10).delay(1500).hide(10);
        return;
    }
    var flag = '';
    if (type == 3) {
        flag = "确定要取消吗？";
    }
    if (type == 4) {
        flag = "确定要拒绝吗？";
    }
    bootbox.confirm(flag, function (res) {
        if (res && type) {
            $.ajax({
                type: 'post',
                url: '/r/outtask/status',
                data: {
                    "taskId": taskId, "remark": remark, "status": type
                },
                async: false,
                dataType: 'json',
                success: function (res) {
                    if (res) {
                        if (res.status == 1) {
                            $('#modal-quickpage-cancelOrReject').modal('hide');
                            $('#cancelOrReject_remark').val('');

                        }
                        $.gritter.add({
                            time: '',
                            class_name: '',
                            title: '系统消息',
                            text: res.message,
                            sticky: false,
                            image: '../../images/message.png'
                        });
                        setTimeout(function () {
                            _refreshQuickPageAfterAction();
                        }, 500);//解决无法同步刷新的问题
                    }
                }

            });
        }
    });
}
/**分配*/
function _reAllocateTaskInQuickPage(taskId) {
    $('#distributionTaskId').val(taskId);
    var opt = '';
    $(window.opener.bank_name_list).each(function (i, bankType) {
        opt += '<option>' + bankType + '</option>';
    });
    $('#form-field-select-allocateTask').empty().html(opt);
    _initialMultiSelectQuick();
    $('#allocateTaskRemark').val('');
    $('input[name="distributeObject"]:radio').prop('checked', false);

    $('#modal-distribution').modal('show');
}
function _saveReallocateTask() {
    var taskId = $('#distributionTaskId').val();
    var bankTypes = $('#form-field-select-allocateTask').val();
    var remark = $('#allocateTaskRemark').val();
    if(!remark){
        $('#noDistributionObj').text('请添加备注!');
        $('#noDistributionObj').show(10).delay(1000).hide(10);;
        return;
    }else{
        $('#noDistributionObj').text('');
        $('#noDistributionObj').hide();
    }
    var distributeObject = '';
    $('input[name="distributeObject"]').each(function () {
        if ($(this).is(":checked")) {
            distributeObject = $(this).val();
        }
    });
    if (!distributeObject) {
        $('#noDistributionObj').text('请选择分配对象!');
        $('#noDistributionObj').show(10).delay(1500).hide(10);
        return;
    } else {
        $('#noDistributionObj').text('');
        $('#noDistributionObj').hide();
    }
    bootbox.confirm('有重复出款的风险,确定重新分配么?',function (res) {
        if (res){
            $.ajax({
                type: 'post',
                url: '/r/outtask/reallocateTask',
                data: {'taskId': taskId, 'type': distributeObject, "bankType": bankTypes.toString(), "remark": remark,'actionPage':'quick'},
                dataType: 'json',
                success: function (res) {
                    if (res) {
                        $.gritter.add({
                            time: 500,
                            class_name: '',
                            title: '系统消息',
                            text: res.message,
                            sticky: false,
                            image: '../../images/message.png'
                        });
                        $('#modal-distribution').modal('hide');
                        _refreshQuickPageAfterAction();
                    }
                }
            });
        }
    })
}
/**转排查*/
function _turnTaskToFailInQuickPage(taskId) {
    $('#turnFailIdTotal').val(taskId);
    $('#turnToFailRemarkTotal').val('');
    $('#modal-turnToFailTotal').modal('show');
    $('#confirmTurnToFailTotal').attr('onclick', '_confirmTurnToFailInQuickPage();');//与重新生成任务共用一个modal
}
function _confirmTurnToFailInQuickPage() {
    if (!$.trim($('#turnToFailRemarkTotal').val())) {
        $('#remark-turnToFailTotal').show(10).delay(1500).hide(10);
        return false;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/turnToFail',
        data: {"taskId": $('#turnFailIdTotal').val(), "remark": $('#turnToFailRemarkTotal').val()},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../../images/message.png'
                });
                $('#modal-turnToFailTotal').modal('hide');
                $('#confirmTurnToFailTotal').attr('onclick', '');
                $('#turnFailIdTotal').val('');
                _refreshQuickPageAfterAction();
            }
        }
    });
}
/**转完成 type=1 表示主管处理的完成 2表示待排查的完成*/
function _turnToFinishTaskInQuickPage(type, taskId) {
    $('#turnToFinishType').val(type);
    $('#remarkInQuick').val('');
    $('#addRemarkInQuickTaskId').val(taskId);
    $('#addRemarkInQuick_modal').modal('show');
    $('#totalTaskFinishBTN').attr('onclick', '_execturnToFinishTaskInQuickPage();');//与加备注共用一个modal
}
function _execturnToFinishTaskInQuickPage() {
    var type = $('#turnToFinishType').val() == 1 ? 'masterOutToMatched' : 'failedOutToMatched';
    if (!$('#remarkInQuick').val()) {
        $('#prompt_remark').show(10).delay(1500).hide(10);
        return;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/finsh',
        data: {"type": type, "taskId": $('#addRemarkInQuickTaskId').val(), "remark": $('#remarkInQuick').val()},
        dataType: 'json',
        async: false,
        success: function (res) {
            $.gritter.add({
                time: 2000,
                class_name: '',
                title: '系统消息',
                text: res.message,
                sticky: false,
                image: '../../images/message.png'
            });
            $('#addRemarkInQuick_modal').modal('hide');
            $('#totalTaskFinishBTN').attr('onclick', '');
            _refreshQuickPageAfterAction();
        }
    });
}
/**发消息*/
function _sendMessageTaskInQuickPage() {
    $('#turnToFinishType').val('');
    $('#remarkInQuick').val('');
    $('#addRemarkInQuickTaskId').val('');
    $('#addRemarkInQuick_modal').modal('show');
    $('#totalTaskFinishBTN').attr('onclick', '_sendMessageInQuickPage();');//与加备注共用一个modal
}
function _sendMessageInQuickPage() {
    var message = $.trim($('#remarkInQuick').val());
    if (!message) {
        $('#prompt_remark').show(10).delay(1500).hide(10);
        return;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/customersendmessage',
        data: {"message": message},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../../images/message.png'
                });
            }
            $('#addRemarkInQuick_modal').modal('hide');
            $('#totalTaskFinishBTN').attr('onclick', '');
            _refreshQuickPageAfterAction();
        }
    });
}
/**重新生成任务*/
function _reCreateTaskInQuickPage(taskId) {
    $('#turnFailIdTotal').val(taskId);
    $('#turnToFailRemarkTotal').val('');
    $('#modal-turnToFailTotal').modal('show');
    $('#confirmTurnToFailTotal').attr('onclick', '_saveReCreateTaskInQuickPage();');
}
function _saveReCreateTaskInQuickPage() {
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
                    image: '../../images/message.png'
                });
                $('#modal-turnToFailTotal').modal('hide');
                $('#confirmTurnToFailTotal').attr('onclick', '');
                $('#turnFailIdTotal').val('');
                _refreshQuickPageAfterAction();
            }
        }
    });

}
/**备注*/
function _addRemarks(id) {
    $('#turnToFinishType').val('');
    $('#remarkInQuick').val('');
    $('#addRemarkInQuickTaskId').val(id);
    $('#addRemarkInQuick_modal').modal('show');
    $('#totalTaskFinishBTN').attr('onclick', 'saveRemarkInQuick();');
}
/**保存备注*/
function saveRemarkInQuick() {
    var remark = $.trim($('#remarkInQuick').val());
    if (!remark) {
        $('#prompt_remark').show(10).delay(2000).hide(10);
        return;
    }
    var taskId = $('#addRemarkInQuickTaskId').val();
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
                    image: '../../images/message.png'
                });
            }
            $('#addRemarkInQuick_modal').modal('hide');
            $('#totalTaskFinishBTN').attr('onclick', '');
            _refreshQuickPageAfterAction();
        }
    });
}

function _initialMultiSelectQuick() {
    $('.multiselect').multiselect('destroy').multiselect({
        nonSelectedText: '请选择',
        filterPlaceholder: '请选择',
        selectAllText: '全选',
        nSelectedText: '已选',
        nonSelectedText: '请选择',
        allSelectedText: '全选',
        numberDisplayed: 5,
        enableFiltering: true,
        includeSelectAllOption: true,
        enableFiltering: true,
        enableHTML: true,
        buttonClass: 'btn btn-white btn-primary',
        templates: {
            button: '<button style="width: 500px;" type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>',
            ul: '<ul class="multiselect-container dropdown-menu"></ul>',
            filter: '<li class="multiselect-item filter"><div class="input-group"><span class="input-group-addon"><i class="fa fa-search"></i></span><input class="form-control multiselect-search" type="text"></div></li>',
            filterClearBtn: '<span class="input-group-btn"><button class="btn btn-default btn-white btn-grey multiselect-clear-filter" type="button"><i class="fa fa-times-circle red2"></i></button></span>',
            li: '<li><a tabindex="0"><label></label></a></li>',
            divider: '<li class="multiselect-item divider"></li>',
            liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
        }
    });
    $(document).one('ajaxloadstart.page', function (e) {
        $('.multiselect').multiselect('destroy');
    });
}
function _modifyQuickNouns(){
    if (window.opener.isHideOutAccountAndModifyNouns){
        $('.modifyQuickHandicap').text('商家');
        $('.modifyQuickAmount1').text('订单点数');
        $('.modifyQuickAmount2').text('任务点数');
        $('.modifyQuickStatus').text('出货状态');
        $('.modifyQuickTime').text('出货时间');//出款时间
    }
}
_modifyQuickNouns();
_initialMultiSelectQuick();
_getQuickQueryData();
