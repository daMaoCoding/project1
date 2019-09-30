currentPageLocation = window.location.href;
var modalParentDivId = null;
/**
 * Created by leo on 2017/8/28.
 * 1 为各个订单号 点击事件 获取模态框
 * 2 审核汇总 待处理 待审核 点击审核 获取模态框
 */
function _getPathAndHtml(divId) {
    modalParentDivId =divId;
    var curWwwPath=window.document.location.origin;
    var version = getCookie('JVERSION');
    var url = curWwwPath +'/'+ version+'/html';
    if (url) {
         _getHtmlFile(url,divId);
    }
}
function _getHtmlFile(url,divId) {
    if (url) {
        $.get({
            url:url+'/outward/orderDetail.html',
            data:{},
            dataType:'html',
            success:function (res) {
                _appendModalDiv(divId,res);
            }
        });
    }
}
/**
 * 需要引入 订单详情的 div id
 * @param divId
 * @param res
 * @private
 */
function _appendModalDiv(divId,res) {
    $('#'+divId).html(res);
    if (divId =='total_toApprove_detail_modal') {//审核汇总 未审核 执行审核
        $('#'+divId ).find('#toApprove_tr').show();
        $('#'+divId ).find('#modal-orderDetail-but_transfer').hide();
        $('#modal-orderDetail-but_pass').click(function () {
            _saveForApproveTotal(1);
        });
        $('#modal-orderDetail-but_cancel').click(function () {
            _saveForApproveTotal(2);
        });
        $('#modal-orderDetail-but_refuse').click(function () {
            _saveForApproveTotal(3);
        });
        $('#modal-orderDetail-but_transfer').click(function () {
            _saveForApproveTotal(4);
        });
    }else if(divId=='toAudit_quickPage_modal'){
        //快捷查询的订单操作
        $('#modal-orderDetail-but_pass').click(function () {
            _actionInQuickPage(1);
        });
        $('#modal-orderDetail-but_cancel').click(function () {
            _actionInQuickPage(2);
        });
        $('#modal-orderDetail-but_refuse').click(function () {
            _actionInQuickPage(3);
        });
        $('#modal-orderDetail-but_transfer').click(function () {
            _actionInQuickPage(4);
        });
    }else{
        $('#'+divId ).find('#toApprove_tr').hide();
    }
}
/**
 * 初始化该订单的审核信息
 * @param orderNo
 * @param handicapName
 * @param level
 * @param member
 * @param amount
 * @param createTime
 * @private
 */
function _initialOrderText(id,data) {
    $('#'+modalParentDivId+ '  #modal-orderDetail').modal('show');
    var idToQuery = id;
    if (idToQuery.toString().indexOf('detail')>-1) {
        //只是查看 则隐藏审核通过等操作按钮
        $('#'+modalParentDivId ).find('#toApprove_tr').hide();
        idToQuery = idToQuery.substring(0,id.toString().indexOf('detail'));
    }else {
        $('#modal-orderDetail-remarkForApprove').val('');
        $('#'+modalParentDivId ).find('#toApprove_tr').show();
    }
    _initialMemberInfoHead(data);
    $.ajax({
        type: 'get',
        url: '/r/out/getRelatedInfo/' + idToQuery + '/' + data.orderNo,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var jsonData = JSON.parse(res.data);
                    _initialMemberOtherInfo(jsonData);
                }
                //$('#'+modalParentDivId+'  #modal-orderDetail').modal('hide');
            }
        }
    });
    var uid =getCookie('JUID');
    if (!(uid && uid == 'admin')) {
        $('#'+modalParentDivId+' #modal-orderDetail-but_transfer').attr('style','display:block;');
    } else {
        $('#'+modalParentDivId+' #modal-orderDetail-but_transfer').attr('style','display:none;');
    }
    // $('#'+modalParentDivId+'  #modal-orderDetail-tdIpAccs').unbind().bind('click', function () {
    //     $('#'+modalParentDivId+'   #modal-orderDetail-ipAccsDetails').modal('show');
    // });
    // $('#'+modalParentDivId+'  #modal-orderDetail-accIpsDetail').unbind().bind('click', function () {
    //     $('#'+modalParentDivId+'   #modal-orderDetail-accIpsDetails').modal('show');
    // });
}

function _initialMemberInfoHead(data) {
    var orderNo = data.orderNo;
    var handicapName = data.handicapName;
    var level=data.level;
    var member= data.member;
    var amount= data.amount;
    var toAccount = data.toAccount;
    //如果是子窗口弹出的 则调用父窗口的方法
    var createTime = window&&window.opener?window.opener.timeStamp2yyyyMMddHHmmss(data.createTime):timeStamp2yyyyMMddHHmmss(data.createTime);
    var approveReason = data.approveReason ;
    var toAccountOwner = data.toAccountOwner ;
    var toAccountBank = data.toAccountBank ;
    var toAccountName = data.toAccountName;

    $('#'+modalParentDivId+'    #modal-orderDetail-orderNo').text(orderNo?orderNo:'');
    $('#'+modalParentDivId+'    #modal-orderDetail-handicap').text(handicapName?handicapName:'');
    $('#'+modalParentDivId+'    #modal-orderDetail-level').text(level?level:"");
    $('#'+modalParentDivId+'    #modal-orderDetail-memberName').text(member?member:"");
    $('#'+modalParentDivId+'    #modal-orderDetail-amountToApprove').text(amount?amount:"");
    $('#'+modalParentDivId+'    #modal-orderDetail-createTime').text(createTime?createTime:"");
    $('#'+modalParentDivId+'    #modal-orderDetail-toAccount').text(toAccount?toAccount:"");
    $('#'+modalParentDivId+'    #modal-orderDetail-toAccountOwnerReq').text(toAccountOwner?toAccountOwner:"");
    $('#'+modalParentDivId+'    #modal-orderDetail-toAccountBankReq').text(toAccountBank?toAccountBank:"");
    $('#'+modalParentDivId+'    #modal-orderDetail-toAccountNameReq').text(toAccountName?toAccountName:"");
    if(approveReason){
        $('#'+modalParentDivId+'    #DetailReason').empty().text(approveReason?approveReason:"");
        $('#'+modalParentDivId+'    #approveReasonLabelInOrderDetail').show();
    } else {
        $('#'+modalParentDivId+'    #approveReasonLabelInOrderDetail').hide();
    }
}
/**
 * 获取会员下单信息
 * @param jsonData
 * @private
 */
function _initialMemberOtherInfo(jsonData) {
    console.log(jsonData);
    //上次出款后余额
    //上次出款后余额 如果没有 则计算上次出款前余额-上次出款余额
    var balanceAfterLastDraw = jsonData.balanceAfterLastDraw?parseFloat(jsonData.balanceAfterLastDraw):0;
    var fUpWithdrawBalance = jsonData.fUpWithdrawBalance?parseFloat(jsonData.fUpWithdrawBalance):0;
    var fUpWithdrawAmount = jsonData.fUpWithdrawAmount?parseFloat(jsonData.fUpWithdrawAmount):0;
    var afterDraw = fUpWithdrawBalance-fUpWithdrawAmount;
    $('#'+modalParentDivId+'    #modal-orderDetail-balanceAfterLastDraw').text(parseFloat(balanceAfterLastDraw?balanceAfterLastDraw:afterDraw).toFixed(3));
    //当前余额
    var fWithdrawBalance =jsonData.fWithdrawBalance?parseFloat(jsonData.fWithdrawBalance):0;
    $('#'+modalParentDivId+'    #modal-orderDetail-balanceBeforeDraw').text(fWithdrawBalance);
    //本次出款金额
    var fWithdrawAmount = jsonData.fWithdrawAmount?parseFloat(jsonData.fWithdrawAmount):parseFloat($('#'+modalParentDivId+'    #modal-orderDetail-amountToApprove').text());
    //本次出款后余额
    var afterDrawCurrent =fWithdrawBalance-fWithdrawAmount;
    $('#'+modalParentDivId+'    #modal-orderDetail-balanceAfterDraw').text(parseFloat(afterDrawCurrent).toFixed(3));
    //会员总入款总额  最近入款总额 平台目前没有送来  需要计算
    var fAllDepositAmount = jsonData.fAllDepositAmount?parseFloat(jsonData.fAllDepositAmount):0;
    //最近入款明细总额 jsonData.recentDepositTotalAmount
    //最近入款明细 DepositLists 已删除
    // if (jsonData.DepositLists && jsonData.DepositLists.length>0) {
    //     _showDepositDetailModalHide(jsonData.DepositLists);
    //     $('#'+modalParentDivId+'    #modal-incomeDetail').find('tbody').empty().html('<tr><td colspan="4" style="text-align: center"><h3>暂无入款明细</h3></td></tr>');
    // }
    var str = parseFloat(jsonData.recentDepositTotalAmount?jsonData.recentDepositTotalAmount:0).toFixed(3)+'<a id="modal-orderDetail-incomeDetail" href="javascript:void (0);" title="上一次出款成功到本次出款之间会员所有的入款和福利。" style="color: #00B83F">(入款详情)</a>';
    $('#'+modalParentDivId).find('#modal-orderDetail-sumAmountsRecentDeposit').empty().html(str);

    //本次下注金额 ,本次已达投注量
    var fBetAmount = jsonData.fBetAmount?parseFloat(jsonData.fBetAmount):0;
    $('#'+modalParentDivId+'    #modal-orderDetail-fBetAmount').text(fBetAmount);
    //本次中奖金额 --改为本次盈利
    var fOmProfit = jsonData.fOmProfit?parseFloat(jsonData.fOmProfit):0;
    $('#'+modalParentDivId+'    #modal-orderDetail-fWinAmount').text(fOmProfit);
    // //本次已达投注量
    // $('#'+modalParentDivId+'    #modal-orderDetail-needCodesAll').text(fBetAmount);
    // //本次要求打码量
    // var fBetNeed =jsonData.fBetNeed?parseFloat(jsonData.fBetNeed):0;
    // $('#'+modalParentDivId+'    #modal-orderDetail-needCodes').text(fBetNeed);
    //几倍打码量: 如果有最近的入款明细,则为入款总额除以本次已达打码量,否则显示0
    // fAllWithdrawAmount 会员总出款 fAllDepositAmount 总入款
    var ratio = fBetAmount!=0?parseFloat(jsonData.recentDepositTotalAmount?jsonData.recentDepositTotalAmount:0)/parseFloat(fBetAmount):0;
    $('#'+modalParentDivId+'    #modal-orderDetail-timesCodes').text(parseFloat(ratio).toFixed(3));
    //中奖率 本次中奖金额/已达投注量
    var fWinAmount = jsonData.fWinAmount?parseFloat(jsonData.fWinAmount):0;
    var winOdds = fBetAmount!=0?parseFloat(parseFloat(fWinAmount)-parseFloat(fBetAmount))/parseFloat(fBetAmount):0;
    $('#'+modalParentDivId+'    #modal-orderDetail-winRate').text(parseFloat(winOdds*100).toFixed(3) + '%');
    //会员备注 新平台传来 ₩单行字符分隔符  ₦多行间分隔符
    var sUserRemark =jsonData.sUserRemark?jsonData.sUserRemark:'';
    if(sUserRemark){
        var reg1 =new RegExp("₩","g");
        var reg2 =new RegExp("₦","g");
        sUserRemark = sUserRemark.replace(reg1," ");
        sUserRemark = sUserRemark.replace(reg2,"<br>");
    }
    $('#'+modalParentDivId+'    #modal-orderDetail-remarkForMember').html(sUserRemark);
    //是否首次出款
    var iWithdrawStatus = jsonData.iWithdrawStatus==1?"是":"否";
    $('#'+modalParentDivId+'    #modal-orderDetail-isFirstOut').text(iWithdrawStatus);
    //注册时间
    var dRegTimeTEXT = jsonData.dRegTimeTEXT?jsonData.dRegTimeTEXT:'';
    $('#'+modalParentDivId+'    #modal-orderDetail-registerTime').text(dRegTimeTEXT);
    //最后登陆时间
    var dLoginTimeTEXT =jsonData.dLoginTimeTEXT?jsonData.dLoginTimeTEXT:'';
    $('#'+modalParentDivId+'    #modal-orderDetail-lastLoginTime').text(dLoginTimeTEXT);
    //注册IP
    var registerIp = jsonData.registerIp?jsonData.registerIp:'';
    $('#'+modalParentDivId+'    #modal-orderDetail-regesterIp').text(registerIp);
    if(registerIp){
        var butt = '<button onclick="_lookupIPAttributionForDetail(\''+registerIp+'\');" class="pull-right btn btn-xs btn-white btn-info">归属地</button>';
        $('#'+modalParentDivId+'    #modal-orderDetail-regesterIp').empty().html(registerIp+butt);
    }else{
        $('#'+modalParentDivId+'    #modal-orderDetail-regesterIp').text(registerIp);
    }
    //最后登陆IP
    var lastLoginIp =jsonData.lastLoginIp?jsonData.lastLoginIp:'';
    if(lastLoginIp){
        var butt = '<button onclick="_lookupIPAttributionForDetail(\''+lastLoginIp+'\');" class="pull-right btn btn-xs btn-white btn-info">归属地</button>';
        $('#'+modalParentDivId+'    #modal-orderDetail-lastLoginIp').empty().html(lastLoginIp+butt);
    }else{
        $('#'+modalParentDivId+'    #modal-orderDetail-lastLoginIp').text(lastLoginIp);
    }
    if (modalParentDivId=='total_toApprove_detail_modal' || modalParentDivId=='toAudit_quickPage_modal' ){
        if (window.sessionStorage.getItem("auditSubPage")==3){
            //主管处理页签 没有转主管按钮
            $('#'+modalParentDivId).find('#modal-orderDetail-but_transfer').hide();
        }else if(window.sessionStorage.getItem("auditSubPage")==0){
            $('#'+modalParentDivId).find('#modal-orderDetail-but_transfer').show();
        }
    }
    //总盈利 = 总出款-总入款
    var fAllWithdrawAmount = jsonData.fAllWithdrawAmount;
    var allWin = fAllWithdrawAmount?(fAllDepositAmount?parseFloat(parseFloat(fAllWithdrawAmount)-parseFloat(fAllDepositAmount)).toFixed(3):parseFloat(fAllWithdrawAmount)):0;
    var title = '总盈利('+parseFloat(allWin).toFixed(3)+')=总出款('+parseFloat(fAllWithdrawAmount).toFixed(3)+')-总入款('+parseFloat(fAllDepositAmount).toFixed(3)+')';
    $('#'+modalParentDivId+'    #modal-orderDetail-winTotal').empty().html('<a title="'+title+'" style="color: red">'+parseFloat(allWin).toFixed(3)+'</a>');
    //最近入款明细 点击事件
    $('#'+modalParentDivId+'    #modal-orderDetail-incomeDetail').unbind().bind('click',function () {
        _showDepositDetailModalHide();
    });
    //IP账号明细 点击事件
    $('#'+modalParentDivId+'    #modal-orderDetail-ipAccsDetail').unbind().bind('click',function () {
        _memberLoginiPInfoDetail(4);
    });
    //账号IP明细 点击事件
    $('#'+modalParentDivId+'    #modal-orderDetail-accIpsDetail').unbind().bind('click',function () {
        _memberLoginiPInfoDetail(3);
    });
    //查看会员总盈利权限
    if(lookUpWinTotalPageFlag){
        $('#'+modalParentDivId+'    #modal-orderDetail-winTotalTr').show();
    }else{
        $('#'+modalParentDivId+'    #modal-orderDetail-winTotalTr').hide();
    }
}
//modal-orderDetail-ipAccsDetails
function _closeDepositModal() {
    $('#modal-orderDetail-depositDetail').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}
function _closeIP2AccountModal() {
    $('#modal-orderDetail-ipAccsDetails').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}
function _closeAccount2IpModal() {
    $('#modal-orderDetail-accIpsDetails').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}
//获取会员登陆信息和IP信息
function _memberLoginiPInfoDetail(type) {
    debugger;
    if(type){
        $.ajax({
            type: 'get',
            url: '/r/synch/getInfo',
            data: {"type": type, "orderNo": $.trim($('#'+modalParentDivId+'    #modal-orderDetail-orderNo').text())},
            dataType: 'json',
            async:false,
            success: function (res) {
                if (res && res.data && res.message.indexOf('失败')<0 ) {
                    var data = JSON.parse(JSON.parse(res.data).message);//[{},{}]
                    var tr = '';
                    if(type==3){
                        if(data){
                            for (var i=0;i<data.length;i++){
                                var butt = data[i].loginIp?'<button onclick="_lookupIPAttributionForDetail(\''+_checkObj(data[i].loginIp)+'\');" class="pull-right btn btn-xs btn-white btn-info">归属地</button>':'';
                                tr +=
                                    '<tr>' +
                                    '<td>' + _checkObj(data[i].account)+ '</td>' +
                                    '<td>' + _checkObj(data[i].loginIp)+butt+'</td>' +
                                    '<td>' +_checkObj(data[i].loginTime) + '</td>' +
                                    '</tr>';
                            }
                            $('#'+modalParentDivId).find('#modal-orderDetail-accIpsDetails').find('tbody').empty().html(tr);
                        }else{
                            $('#'+modalParentDivId).find('#modal-orderDetail-accIpsDetails').find('tbody').empty().html('<tr style="text-align: center"><td colspan="10"><h3>暂无明细</h3></td></tr>');
                        }
                        $('#'+modalParentDivId+'    #modal-orderDetail-accIpsDetails').modal('show');
                    }else{
                        if(data){
                            for (var i=0;i<data.length;i++){
                                var butt = data[i].ip?'<button onclick="_lookupIPAttributionForDetail(\''+_checkObj(data[i].ip)+'\');" class="pull-right btn btn-xs btn-white btn-info">归属地</button>':'';
                                tr +=
                                    '<tr>' +
                                    '<td>' + _checkObj(data[i].ip)+butt+ '</td>' +
                                    '<td>' + _checkObj(data[i].userName) + '</td>' +
                                    '<td>' +_checkObj(data[i].loginTime) + '</td>' +
                                    '</tr>';
                            }
                            $('#'+modalParentDivId).find('#modal-orderDetail-ipAccsDetails').find('tbody').empty().html(tr);
                        }else{
                            $('#'+modalParentDivId).find('#modal-orderDetail-ipAccsDetails').find('tbody').empty().html('<tr style="text-align: center"><td colspan="10"><h3>暂无明细</h3></td></tr>');
                        }
                        $('#'+modalParentDivId+'    #modal-orderDetail-ipAccsDetails').modal('show');
                    }

                }else{
                    debugger;
                    if(res.message.indexOf('失败')<0){
                        if(type==3){
                            $('#'+modalParentDivId).find('#modal-orderDetail-accIpsDetails').find('tbody').empty().html('<tr style="text-align: center"><td colspan="10"><h3>暂无明细</h3></td></tr>');
                            $('#'+modalParentDivId+'    #modal-orderDetail-accIpsDetails').modal('show');
                        }else{
                            $('#'+modalParentDivId).find('#modal-orderDetail-ipAccsDetails').find('tbody').empty().html('<tr style="text-align: center"><td colspan="10"><h3>暂无明细</h3></td></tr>');
                            $('#'+modalParentDivId+'    #modal-orderDetail-ipAccsDetails').modal('show');
                        }
                    }else{
                        $.gritter.add({
                            time: 2000,
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
}
//最近入款明细
function _showDepositDetailModalHide() {
    var orderNo = $('#'+modalParentDivId+'    #modal-orderDetail-orderNo').text();
    $.ajax({
        type:'post',
        url:'/r/synch/getInfo',
        dataType:'json',
        async:false,
        data:{'type':2,'orderNo':$.trim(orderNo)},
        success:function (res) {
            if(res && res.data && res.message.indexOf('失败')<0 ){
                debugger;
                var data = JSON.parse(JSON.parse(res.data).message);//[{},{}]
                var sumAmountsDetail = 0;
                if (data) {
                    var tr = '';
                    for (var i=0;i<data.length;i++){
                        // sDepositType ->iDepositType
                        tr +=
                            '<tr>' +
                            '<td>'+(data[i].sOrderId?data[i].sOrderId:'')+'</td>'+
                            '<td>'+(data[i].fAmount?data[i].fAmount:'')+'</td>'+
                            '<td>'+(data[i].sDepositType?data[i].sDepositType:'')+'</td>'+
                            // '<td>'+(data[i].fBetNeeds?data[i].fBetNeeds:'')+'</td>'+
                            '<td>'+(data[i].dCreateTimeTEXT?data[i].dCreateTimeTEXT:'')+'</td>' +
                            '</tr>';
                        sumAmountsDetail += parseFloat(data[i].fAmount);
                    }
                    $('#'+modalParentDivId+'    #modal-orderDetail-depositDetail').find('tbody').empty().html(tr);
                    var trs = '<tr>'
                        +'<td></td>'
                        +'<td id="depositDetailAmount" bgcolor="#D6487E" style="color:white;">'+parseFloat(sumAmountsDetail).toFixed(3)+'</td>'
                        +'<td colspan="4"></td>'
                        +'</tr>';
                    $('#'+modalParentDivId+'    #modal-orderDetail-depositDetail').find('tbody').append(trs);
                }else{
                    $('#'+modalParentDivId+'    #modal-orderDetail-depositDetail').find('tbody').append('<tr><td colspan="10" style="text-align: center"><h4>暂无明细</h4></td></tr>');
                }
                $('#'+modalParentDivId+'    #modal-orderDetail-depositDetail').modal('show');
            }else{
                if(res.message.indexOf('失败')<0){
                    $('#'+modalParentDivId+'    #modal-orderDetail-depositDetail').find('tbody').empty().html('<tr><td colspan="10" style="text-align: center"><h4>暂无明细</h4></td></tr>');
                    $('#'+modalParentDivId+'    #modal-orderDetail-depositDetail').modal('show');
                }else{
                    $.gritter.add({
                        time:2000,
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
//查看IP归属地
function _lookupIPAttributionForDetail(ip) {
    if (ip){
        $.ajax({
            type:'get',
            url:'/r/out/getIpAttribution/'+ip,
            dataType:'json',
            success:function (res) {
                var tr = '<tr><td colspan="8"><h4>查不到信息</h4></td></tr>';
                if(res.status==1 && res.data){
                    //nation: "菲律宾",provice: "菲律宾",city: "
                    var jsonRet = JSON.parse(res.data);
                    tr = '<tr><td>'+_checkObj(jsonRet.nation)+'</td>'+
                        '<td>'+_checkObj(jsonRet.provice)+'</td>'+
                        '<td>'+_checkObj(jsonRet.city)+'</td></tr>';
                }
                $('#'+modalParentDivId  +'  #modal-orderDetail-attribution-ip-tbody').empty().html(tr);
                $('#'+modalParentDivId  +'  #modal-orderDetail-attribution-ip').modal('show');
            }
        })
    }
}
function _closeAttribution4IP() {
    $('#'+modalParentDivId  +'  #modal-orderDetail-attribution-ip').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}
