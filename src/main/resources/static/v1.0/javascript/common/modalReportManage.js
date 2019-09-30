currentPageLocation = window.location.href;

var report_showInOutListModal = function (accountId) {
    $.ajax({
        type: "GET",
        async: false,
        dataType: 'html',
        url: "/" + sysVersoin + "/html/common/reportManage.html",
        success: function (html) {
            var $div = $(html).find("#reportManage_modal").clone().appendTo($("body"));
            $div.find("input#accountInfo_id").val(accountId);
            $div.modal("toggle");
            //数据列表
            report_sysLogList_In_OutList(0);
            //查询账号信息
            var result = getAccountMoreInfoById(accountId);
            if (result && result.account) {
                $div.find("input#accountInfo_alias").val(result.account.alias);
                var accountInfo = result.account;
                if (accountInfo) {
                    $div.find("#accountInfo_account").text((accountInfo.alias ? accountInfo.alias : '无') + '|' + (accountInfo.bankType ? accountInfo.bankType : '无') + '|' + (accountInfo.owner ? accountInfo.owner : '无') + '|' + hideAccountAll(accountInfo.account));
                    $div.find("#accountInfo_id_span").text(accountInfo.id);
                }
            } else {
                showMessageForFail("账号信息查询异常，请刷新页面");
            }
            //加载时间控件
            initTimePicker();
            $(document).keypress(function (e) {
                if (event.keyCode == 13) {
                    $div.find("#searchBtn button").click();
                }
            });
            $div.on('hidden.bs.modal', function () {
                $div.remove();
            });
        }
    });
}


/**
 * 银行流水明细列表
 */
var report_bankLogList_In_OutList = function (CurPage) {
    var $div = $("#reportManage_modal");
    //封装查询条件
    if (!!!CurPage && CurPage != 0) CurPage = $("#bankLogList_In_OutPage .Current_Page").text();
    var startAndEndTimeToArray = getTimeArray($div.find("#filter [name=startAndEndTime]").val());
    var fromAccount = $div.find("#accountInfo_id").val();
    var formData = {
        "fromAccount": fromAccount,
        "status": bankLogStatusMatching,
        "startAndEndTimeToArray": startAndEndTimeToArray.toString(),
        "pageNo": CurPage <= 0 ? 0 : CurPage - 1,
        "pageSize": $.session.get('initPageSize'),
        "orderBy": $div.find("#orderByTh").val()
    };
    //转入转出
    if ($div.find("[name=searchTypeIn0Out1]:checked").length == 1) {
        if ($div.find("[name=searchTypeIn0Out1]:checked").val() == 1) {
            //转出时  金额是负数
            formData.maxAmount = formData.maxAmount < 0 ? formData.maxAmount : 0;
        } else {
            //转入时  金额是正数
            formData.minAmount = formData.minAmount > 0 ? formData.minAmount : 0;
        }
    }
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/r/banklog/bankLogList",
        data: formData,
        success: function (jsonObject) {
            if (jsonObject.status != 1) {
                showMessageForFail("查询失败：" + jsonObject.message);
                return;
            }
            var tbody = "", totalBankLogAmount_Plus = 0, totalBankLogAmount_Nagetive = 0, idList = new Array();
            $.each(jsonObject.data, function (index, record) {
                var tr = "";
                tr += "<td style='display:none;'><span>" + record.id + "</span></td>";
                tr += "<td><span>" + (record.tradingTimeStr ? record.tradingTimeStr.slice(record.tradingTimeStr.length - 14) : '') + "</span></td>";
                tr += "<td><span>" + (record.createTimeStr ? record.createTimeStr.slice(record.createTimeStr.length - 14) : '') + "</span></td>";
                if (record.amount > 0) {
                    tr += "<td><span>" + record.amount + "</span></td>";
                    tr += "<td><span>--</span></td>";
                    totalBankLogAmount_Plus += record.amount * 1;
                } else {
                    tr += "<td><span>--</span></td>";
                    tr += "<td><span>" + record.amount + "</span></td>";
                    totalBankLogAmount_Nagetive += record.amount * 1;
                }
                tr += "<td><span>" + (record.balance ? record.balance : 0) + "</span></td>";
                var sim = record.toAccountOwner ? '**' + record.toAccountOwner.slice(record.toAccountOwner.length - 1) : '';
                if (record.toAccount) {
                    sim = sim ? (sim + '|') : '';
                    sim = sim + (record.toAccount ? '**' + record.toAccount.slice(record.toAccount.length - 3) : '');
                }
                tr += "<td><span>" + sim + "</span></td>";
                tr += "<td>" + getHTMLremark(record.summary, 90) + "</td>";
                tr += "<td><span>" + getHTMLremark(record.remark, 90) + "</span></td>";
                tr += "<td>";
                tr += "<button type=\"button\" class=\"btn btn-xs btn-white btn-warning btn-round btn-confirmRemark\" onclick=\"doFlogDupliDraw(" + fromAccount + "," + record.id + ")\">重复出款</button>";
                tr += "<button type=\"button\" class=\"btn btn-xs btn-white btn-warning btn-round\" onclick=\"doFlogDupliFlow(" + fromAccount + "," + record.id + ")\">重复流水</button>";
                tr += "<button type=\"button\" class=\"btn btn-xs btn-white btn-warning btn-round\" onclick=\"doFlowSteal(" + fromAccount + "," + record.id + ")\">盗刷</button>";
                tr += "<button type=\"button\" class=\"btn btn-xs btn-white btn-warning btn-round\" onclick=\"doFlowFee(" + fromAccount + "," + record.id + ")\">手续费</button>";
                tr += "<button type=\"button\" class=\"btn btn-xs btn-white btn-warning btn-round\" onclick=\"doFlowRefund(" + fromAccount + "," + record.id + ")\">回冲</button>";
                tr += "<button type=\"button\" class=\"btn btn-xs btn-white btn-warning btn-round\" onclick=\"doFlowOhter(" + fromAccount + "," + record.id + ")\">其他</button>";
                tr += "";
                tr += "";
                tr += "</td>";
                if (record.status == bankLogStatusMatched) {
                    idList.push(record.id);
                }
                tbody += "<tr id='tr" + record.id + "'>" + tr + "</tr>";
            });
            $div.find("table#list tbody").html(tbody);
            loadHover_InOutInfoHover(idList);
            showPading(jsonObject.page, "bankLogList_In_OutPage", report_bankLogList_In_OutList);
        }
    });
}
var report_sysLogList_In_OutList = function (CurPage) {
    var $div = $("#reportManage_modal");
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: '/r/problem/taskInv',
        data: {accId: $div.find("#accountInfo_id").val()},
        success: function (res) {
            if (res.status == 1) {
                if (!res.data || res.data.length == 0)
                    return;
                var html = '';
                $.each(res.data, function (idx, obj) {
                    html = html + '   <tr>';
                    html = html + '     <td>' + (obj.time ? obj.time.slice(obj.time.length - 14) : '') + '</td>';
                    html = html + '     <td>---</td>';
                    html = html + '     <td>' + obj.amt + '</td>';
                    html = html + '     <td>' + obj.toSim + '</td>';
                    html = html + '     <td>' + obj.taskTypeName + '</td>';
                    html = html + '     <td>---</td>';
                    html = html + '     <td>';
                    html = html + '         <button class="btn btn-xs btn-white btn-warning btn-round" onclick="doSysAck(\'' + obj.msg + '\')"><span>成功</span></button>';
                    html = html + '         <button class="btn btn-xs btn-white btn-warning btn-round" onclick="doSysCancel(\'' + obj.msg + '\')"><span>失败</span></button>';
                    html = html + '     </td>';
                    html = html + '   </tr>';
                });
                $div.find("table#sysLog_inOut tbody").html(html);
            } else {
                $div.find("table#sysLog_inOut tbody").html(tbody);
                showMessageForFail("修改失败：" + res.message);
            }
            showPading(jsonObject.page, "sysLogPage_inOut", report_sysLogList_In_OutList);
            loadHover_InOutInfoHover(idList);
        }
    });
}

var report_changeSysLogTab = function (sysLogType) {
    var $div = $("#reportManage_modal");
    if (!sysLogType) {
        //其它TAB页切换来时保留上次查询
        sysLogType = $div.find("#sysLogType").val();
    }
    $div.find("#sysLogType").val(sysLogType);
    if (sysLogType == 'in' || sysLogType == 'out') {
        report_sysLogList_In_OutList(0);
    } else if (sysLogType == 'member') {
        report_sysLogList_outTask(0);
    }
}
var report_changeLogTab = function (logType) {
    var $div = $("#reportManage_modal");
    $div.find("#logTab").val(logType);
    if (logType == 'sysLog') {//系统流水
        report_changeSysLogTab($div.find("#sysLogType").val());
    } else if (logType == 'type0') {//匹配中
        $div.find(".statusSearch").hide();
        report_bankLogList_In_OutList(0);
    }
};

var doSysAck = function (msg) {
    bootbox.prompt("确认：该订单出款成功？", function (result) {
        if (!result)
            return;
        $.ajax({dataType:'JSON',type:"POST",async:false,url:'/r/problem/doSysAck',data:{msg:msg,remark:result},success:function (res) {
            if(res.status!=1){
                showMessageForFail(res.message);
            }else{
                showMessageForSuccess('操作成功');
            }
        }});
    });
};

var doSysCancel = function (msg) {
    bootbox.prompt("确认：该订单出款失败？", function (result) {
        if (!result)
            return;
        $.ajax({dataType:'JSON',type:"POST",async:false,url:'/r/problem/doSysCancel',data:{msg:msg,remark:result},success:function (res) {
            if(res.status!=1){
                showMessageForFail(res.message);
            }else{
                showMessageForSuccess('操作成功');
            }
        }});
    });
};

var doFlogDupliDraw = function (accId, logId) {
    bootbox.prompt("确定该笔流水为重复出款？", function (result) {
        if (!result)
            return;
        $.ajax({dataType:'JSON',type:"POST",async:false,url:'/r/problem/doFlogDupliDraw',data:{accId:accId,logId:logId,remark:result},success:function (res) {
            if(res.status!=1){
                showMessageForFail(res.message);
            }else{
                showMessageForSuccess('操作成功');
            }
        }});
    });
};

var doFlogDupliFlow = function (accId, logId) {
    bootbox.prompt("确定该笔流水为重复流水？", function (result) {
        if (!result)
            return;
        $.ajax({dataType:'JSON',type:"POST",async:false,url:'/r/problem/doFlogDupliFlow',data:{accId:accId,logId:logId,remark:result},success:function (res) {
            if(res.status!=1){
                showMessageForFail(res.message);
            }else{
                showMessageForSuccess('操作成功');
            }
        }});
    });
};

var doFlowSteal = function (accId, logId) {
    bootbox.prompt("确定该笔流水为盗刷流水？", function (result) {
        if (!result)
            return;
        $.ajax({dataType:'JSON',type:"POST",async:false,url:'/r/problem/doFlowSteal',data:{accId:accId,logId:logId,remark:result},success:function (res) {
            if(res.status!=1){
                showMessageForFail(res.message);
            }else{
                showMessageForSuccess('操作成功');
            }
        }});
    });
};

var doFlowFee = function (accId, logId) {
    bootbox.prompt("确定该笔流水为手续费流水？", function (result) {
        if (!result)
            return;
        $.ajax({dataType:'JSON',type:"POST",async:false,url:'/r/problem/doFlowFee',data:{accId:accId,logId:logId,remark:result},success:function (res) {
            if(res.status!=1){
                showMessageForFail(res.message);
            }else{
                showMessageForSuccess('操作成功');
            }
        }});
    });
};


var doFlowRefund = function (accId, logId) {
    bootbox.prompt("确定该笔流水为回冲流水？", function (result) {
        if (!result)
            return;
        $.ajax({dataType:'JSON',type:"POST",async:false,url:'/r/problem/doFlowRefund',data:{accId:accId,logId:logId,remark:result},success:function (res) {
            if(res.status!=1){
                showMessageForFail(res.message);
            }else{
                showMessageForSuccess('操作成功');
            }
        }});
    });
};

var doFlowOhter = function (accId, logId) {
    bootbox.prompt("确定该笔流水为其他流水(结息/不明收入)？", function (result) {
        if (!result)
            return;
        $.ajax({dataType:'JSON',type:"POST",async:false,url:'/r/problem/doFlowOhter',data:{accId:accId,logId:logId,remark:result},success:function (res) {
            if(res.status!=1){
                showMessageForFail(res.message);
            }else{
                showMessageForSuccess('操作成功');
            }
        }});
    });
};
