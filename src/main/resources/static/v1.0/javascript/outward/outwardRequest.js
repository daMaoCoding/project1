currentPageLocation = window.location.href;
var toApprovePage = false;
var currentPage = '';//"#toApprove"
var haveReceivedData = false;
var pauseFlag = '';
var stopFlag = '';
var playVideoForRequest = false;
var playingVideo = false;
var timeInterval = null;
var sumAmounts = 0;
var lookUpAuditedAmount = false;
var autoApproveAmountForBigWin = 0;
$.each(ContentRight['Outward:*'], function (name, value) {
    if (name == 'Outward:currentpageSum:*') {
        outwardCurrentPageSum=true;
    }
    if (name =='Outward:allRecordSum:*'){
        outwardAllRecordSum=true;
    }
});
/**获取大额审核金额作为大额中奖的标准*/
function _getBigWinCriterion() {
    $.ajax({
        type:'get',
        url:'/r/set/findAutoApproveLimit',
        data:{},
        async:false,
        dataType:'json',
        success:function (res) {
            if(res && res.data){
                autoApproveAmountForBigWin = parseFloat(res.data);
            }
        }
    });
    
}
/**查看大额中奖*/
function _searchBigWin(orderNo) {
    $.ajax({
        type:'get',
        url:'/r/synch/getInfo',
        data:{'orderNo':orderNo,'type':1},
        dataType:'json',
        success:function (res) {
            if(res&&res.data){
                var data = JSON.parse(JSON.parse(res.data).message);
                var tr = '<tr>';
                if(data){
                    $.each(data,function (i,val) {
                        tr +='<td>'+_checkObj(val.lotId)+'</td><td>'+_checkObj(val.lotName)+'</td>'+
                            '<td>'+_checkObj(val.issue)+'</td><td>'+_checkObj(val.orderMoney)+'</td>'+
                            '<td>'+_checkObj(val.orderNum)+'</td><td>'+_checkObj(val.thisReward)+'</td>'+
                            '<td>'+_checkObj(val.winMoney)+'</td><td>'+_checkObj(val.createTime)+'</td></tr>';
                    });
                    $('#modal-bigwin').find('tbody').empty().html(tr);
                }else{
                    $('#modal-bigwin').find('tbody').empty().html('<tr><td colspan="10" style="text-align: center"><h4>暂无明细</h4></td></tr>');
                }
            }else{
                $('#modal-bigwin').find('tbody').empty().html('<tr><td colspan="10" style="text-align: center"><h4>暂无明细</h4></td></tr>');
            }
            $('#modal-bigwin').modal('show');
        }
    });
}
/**如果获取不到出款请求则定时n秒后自动再去获取直到获取成功*/
//ul li点击事件
function _getCurrentLiA(obj) {
    if (timeInterval){
        clearTimeout(timeInterval);
        timeInterval = null;
    }
    currentPage = $(obj).find('a').attr('href');
    if (currentPage!='#toApprove'){
        $('#changBtnDivOutReq').attr('style','display:none');
        //_continueGetInterval();
    } else {
        $('#changBtnDivOutReq').attr('style','display:block');
        //_toApprove(pauseFlag);
    }
    _continueGetInterval();
}
window.onbeforeunload = function () {
    if (timeInterval){
        clearTimeout(timeInterval);
        timeInterval = null;
    }
}

function _continueGetInterval() {
    //当前页面且是出款审核页签 且是接单状态（非暂停，非停止） 且是没收到数据的情况下执行
    if (!haveReceivedData && stopFlag=='1' && pauseFlag=='1' && currentPageLocation.indexOf('OutwardAudit:*') > -1 && currentPage == "#toApprove") {
        toApprovePage = true;
    } else {
        toApprovePage = false;
        if (timeInterval) {
            clearTimeout(timeInterval);
        }
    }
    if (toApprovePage){
        _toApprove(pauseFlag);
    }
}
function _playVideoForRequest() {
    if (playVideoForRequest && !playingVideo) {
        playingVideo = true;
        var borswer = window.navigator.userAgent.toLowerCase();
        var url = window.location.origin + '/' + sysVersoin + '/javascript/outward/sound.mp3';
        if (borswer.indexOf("ie") >= 0) {
            //IE内核浏览器
            var strEmbed = '<embed name="embedPlay" src="' + url + '" autostart="true" hidden="true" loop="false" quality="high"  pluginspage="http://www.adobe.com/go/getflashplayer" play="true" type="application/x-shockwave-flash" menu="false" ></embed>';
            if ($("#myContentForRequest").find("embed").length <= 0)
                $("#myContentForRequest").append(strEmbed);
            var embed = document.embedPlay;

            //浏览器不支持 audion，则使用 embed 播放
            embed.volume = 100;
            embed.play();
        } else {
            //非IE内核浏览器
            var strAudio = "<audio id='audioPlayForRequest' src='" + url + "' pluginspage='http://www.adobe.com/go/getflashplayer' play='true' hidden='true'>";
            if ($("#myContentForRequest").find("audio").length <= 0)
                $("#myContentForRequest").append(strAudio);
            var audio = document.getElementById("audioPlayForRequest");
            //浏览器支持 audion
            if (audio) {
                audio.play();
                audio.addEventListener("ended", function () {
                    console.log("音频已播放完成");
                    playingVideo = false;
                    $("#myContentForRequest").empty();
                });
            }
        }
    }
}

function start(){
	$.ajax({
        type: 'post',
        url: '/r/income/stoporder',
        data: {"remark": "开始接单","type":'3',"localHostIp": localHostIp},
        async: false,
        dataType: 'json',
        success: function (res) {
        	 $.session.set('requestFlag', '1');
             $('#changBtn').attr('btn_value', '1');
             _showButton($.session.get('requestFlag')); //开始接单 显示结束
        }
    });
}

/**
 * 开始接单 结束接单 点击事件
 * @param btn
 * @private
 */
function _startWork() {
    var val = $('#changBtn').attr('btn_value');
    if (val == '0') {
    	start();
    }
    if (val == '1') {
        var toApproveId = $.session.get('toAppoveId');
        if (toApproveId) {
            bootbox.confirm("<h4 style='color: red'>您有在审核的订单，先暂停接单并完成当前审核，再结束！</h4>", function (res) {
                if (res) {
                    $('#pauseHint').attr('style', 'color:mediumvioletred;');
                    return;
                }
            });
            $('#pauseHint').attr('style', 'color:red;');
        } else {
        	$('#stopOrder').modal('show');
        }
    }
}

function changeReadonly(){
	var valueWhy=$('input:radio[name=radioWhy]:checked').val();
	if(valueWhy=='其它'){
		$('textarea[name=whyText]').removeAttr("readonly");
	}else{
		$('textarea[name=whyText]').attr("readonly","readonly");
		$("#whyText").val("");
	}
}
function closeStopOrder(){
	$('#stopOrder').modal('hide');
	$('#modal_choose').show();
}
function subStopOrder(){
	var remark="";
	if($('input:radio[name=radioWhy]:checked').val()=='其它'){
		remark=$.trim($("#whyText").val());
	}else{
		remark=$('input:radio[name=radioWhy]:checked').val();
	}
	if(remark=="" || remark==undefined){
		$('#remarkWhyText').show(100).delay(2000).hide(100);
        return false;
	}
    bootbox.confirm("确定结束接单吗？", function (res) {
        if (res) {
        	//保存停止接单原因
        	$.ajax({
                type: 'get',
                url: '/r/income/stoporder',
                data: {"remark": remark,"type":'4',"localHostIp": localHostIp},
                async: false,
                dataType: 'json',
                success: function (res) {
                    if (res && res.status == 1) {
                    	closeStopOrder();
                    	 $.session.set('requestFlag', '0');
                         $.session.remove('pauseFlaggs');
                         $('#pauseBtn').attr('btn_value', '1');
                         $('#changBtn').attr('btn_value', '0');
                         //outwardrequestConnetFlag = false;
                         //disconnectForoutwardrequest();
                         _showButton($.session.get('requestFlag'));//结束接单 显示开始
                         $('#pauseHint').attr('style', 'color:mediumvioletred;');
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

function _showButton(status) {
    stopFlag = status;
    if (status == '1') {//开始接单  显示结束
        $('#tab1_init').hide();
        $('#changBtn').empty().html('<i class="bigger-120 ace-icon fa fa-square red"></i><span>结束接单</span>');
        if ($.session.get('pauseFlaggs')) {
            if ($.session.get('pauseFlaggs') != $('#pauseBtn').attr('btn_value')) {
                $('#pauseBtn').attr('btn_value', $.session.get('pauseFlaggs'));
            }
            _showPauseButton($.session.get('pauseFlaggs'),false);
        }
        else {
            $.session.set('pauseFlaggs', $('#pauseBtn').attr('btn_value'));
            _showPauseButton($('#pauseBtn').attr('btn_value'),false);

        }
    }
    if (status == '0') {//结束接单 显示开始
        $('#info_in1').hide();
        $('#info_in2').hide();
        if ($('#none_info')) {
            $('#none_info').hide();
        }
        if ($('#continue_approve')) {
            $('#continue_approve').remove();
        }
        $('#tab1_init').show();//提示接单显示
        $('#changBtn').empty().html('<i  class="bigger-120 ace-icon fa fa-play green"></i><span>开始接单</span>');
        $('#pauseBtn').hide();
        _continueGetInterval();
    }
}

/**
 * 暂停 继续接单 点击事件
 * 初始是 接单 显示暂停
 * @param obj
 * @private
 */
function _pauseWork() {
    var val = $('#pauseBtn').attr('btn_value');
    if (val == '1') {
        $.session.set('pauseFlaggs', '0');
        $('#pauseBtn').attr('btn_value', '0');
        _showPauseButton($.session.get('pauseFlaggs'),true);
    }
    if (val == '0') {
        $.session.set('pauseFlaggs', '1');
        $('#pauseBtn').attr('btn_value', '1');
        _showPauseButton($.session.get('pauseFlaggs'),true);
    }
}
function _showPauseButton(status,clickPauseFlag) {
    pauseFlag = status;
    if (status == '1') {//接单 显示暂停
        $('#pauseBtn').empty().html('<i class="bigger-120 ace-icon fa fa-pause grey"></i><span style="color: red;">暂停接单</span>');
        $('#pauseBtn').show();
        if ($('#continue_approve')) {
            $('#continue_approve').remove();
        }
        if ($('#changBtn').attr('btn_value')=='1') {
            _continueGetInterval();
        }
    }
    if (status == '0') {//暂停 显示继续
        $('#pauseBtn').empty().html('<i class="bigger-120 ace-icon fa fa-play green"></i><span>继续接单</span>');
        $('#pauseBtn').show();
        if (!$.session.get('toAppoveId')) {
            $('#info_in1').hide();
            $('#info_in2').hide();
            if ($('#none_info')) {
                $('#none_info').hide();
            }
            if ($('#continue_approve')) {
                $('#continue_approve').remove();
                $('#toApprove').append('<div id="continue_approve" style="text-align: center;color: mediumvioletred;display: none;"><h3>请点击\"继续接单\"按钮继续接单吧</h3></div>');
                $('#continue_approve').show();
            }
        } else {
            if (!clickPauseFlag){
                _toApprove('1');//接单
            }else{
                // if ($('#continue_approve')) {
                //     $('#none_info').html('');
                //     $('#none_info').hide();
                //     $('#continue_approve').remove();
                //     $('#toApprove').append('<div id="continue_approve" style="text-align: center;color: mediumvioletred;display: none;"><h3>请点击\"继续接单\"按钮继续接单吧</h3></div>');
                //     $('#continue_approve').show();
                // }
            }
        }
    }
}

/**
 * 初始化页面  刷新
 * @private
 */
function _initPageRequest() {
    if (timeInterval){
        clearTimeout(timeInterval);
        timeInterval = null;
    }
    currentPage = $('#outwardRequestUL li.active').find('a').attr('href');
    if ($.session.get('requestFlag')) {
        if ($.session.get('requestFlag') != $('#changBtn').attr('btn_value')) {
            $('#changBtn').attr('btn_value', $.session.get('requestFlag'));
        }
        _showButton($.session.get('requestFlag'));
    } else {
        _showButton($('#changBtn').attr('btn_value'));//没开始接单
    }
}
//客服不应该有正在审核页面
function _showToAuditPage() {
    var  showToAuditFlag =false;
    $.each(ContentRight['OutwardAudit:*'],function(name,value) {
        if(name=='OutwardAudit:CompleteAudit:*'){
            showToAuditFlag =true;
        }
    });
    if (!showToAuditFlag) {
        $('#toApproveAuditLi').remove();
        $('#toApprove').remove('class');
        $('#toApprove').hide();
        $('#doneApproveAuditLi').attr('class','active');
        $('#doneApprove').attr('class','tab-pane in active');
        $('#tab1_init').hide();
        $('#changBtnDivOutReq').hide();
        _doneApprove();
    }else {
        $('#toApproveAuditLi').show();
    }
}
/**
 * 获取正在审核的记录 pauseFlaggs=1 表示正在接单 显示暂停接单  0 表示暂停 显示继续接单
 */
function _toApprove(pauseFlaggs) {
    var cancelAuditFlag = false;
    var refuseAuditFlag = false;
    var transmitTomasterFlag = false;
    var lookUpWinTotalFlag = false;//标识获取盈利权限
    $.each(ContentRight['OutwardAudit:*'],function(name,value) {
        if(name=='OutwardAudit:CancelAudit:*'){
            cancelAuditFlag =true;
        }
        if(name=='OutwardAudit:RefuseAudit:*'){
            refuseAuditFlag =true;
        }
        if(name=='OutwardAudit:ToMasterAudit:*'){
            transmitTomasterFlag =true;
        }
        if(name=='OutwardAudit:lookUpWinTotal:*'){
            lookUpWinTotalFlag =true;
        }

    });
    if (cancelAuditFlag) {
        $('#but_cancel').show();
    }
    if (refuseAuditFlag) {
        $('#but_refuse').show();
    }
    if (transmitTomasterFlag) {
        $('#but_transfer').show();
    }
    if ($('#none_info')) {
        $('#none_info').hide();
    }
    $('#info_in1').hide();
    $('#info_in2').hide();
    if (pauseFlaggs && pauseFlaggs == "1") {
        //点击开始接单按钮之后才初始化
        $.ajax({
            type: 'get',
            url: '/r/out/getTask',
            data: {},
            dataType: 'json',
            async:false,
            success: function (res) {
                if (res.status == 1) {
                    if (res.data) {
                        haveReceivedData = true;
                        if (currentPageLocation.indexOf('OutwardAudit:*')>-1){
                            _playVideoForRequest();
                        }
                        playVideoForRequest = false;
                        //前端定时取获取出款请求 由于异步调用所以要立刻调用该方法
                        if (timeInterval) {
                            clearTimeout(timeInterval);
                        }
                        //需要审核的隐藏域的关键值
                        $('#toAppoveIdRequest').val(res.data.id);//审核出款请求id
                        $('#orderNoInputRequest').val(res.data.orderNo);
                        $('#memberCodeRequest').val(res.data.memberCode);
                        //需要显示的审核信息
                        $('#toAccount').text(res.data.toAccount);
                        //受审原因
                        if (res.data.approveReason) {
                            $('#approveReason').empty().text(res.data.approveReason);
                            $('#approveReasonLabel').show();
                        } else {
                            $('#approveReasonLabelInOrderDetail').hide();
                        }
                        $('#orderNo').text(res.data.orderNo);
                        $('#handicap').text(res.data.handicapName);
                        $('#level').text(res.data.level);
                        $('#memberName').text(res.data.member);
                        //判断是否大额出款
                        if(autoApproveAmountForBigWin && autoApproveAmountForBigWin<=parseFloat(res.data.amount)){
                            $('#amountToApprove').empty().html(parseFloat(res.data.amount).toFixed(3)+'<a href="javascript:void(0);" title="大额中奖:以系统设置的大额审核出款为基准：'+autoApproveAmountForBigWin+'" onclick="_searchBigWin(\''+res.data.orderNo+'\');">大额中奖</a>')
                        }else{
                            $('#amountToApprove').text(parseFloat(res.data.amount).toFixed(3));
                        }
                        $('#createTime').text(timeStamp2yyyyMMddHHmmss(res.data.createTime));
                        $('#toAccountOwnerReq').text(res.data.toAccountOwner);
                        $('#toAccountBankReq').text(res.data.toAccountBank);
                        $('#toAccountNameReq').text(res.data.toAccountName);
                        $('#info_in1').show();
                        if ($.session.get('toAppoveId')) {
                            $.session.remove('toAppoveId');
                        }
                        $.session.set('toAppoveId', res.data.id);
                        try {
                            _getRelatedInfo();
                        }catch (e){

                        }
                        if(lookUpWinTotalFlag){
                            $('#winTotalTr').show();
                        }else{
                            $('#winTotalTr').hide();
                        }
                        $('#info_in2').show();
                    } else {
                        $('#toAppoveIdRequest').val('');//审核出款请求id
                        $('#orderNoInputRequest').val('');

                        $('#info_in1').hide();
                        $('#info_in2').hide();
                        if ($('#changBtn').attr('btn_value')=='1') {
                            if (!document.getElementById('none_info')) {
                                $('#toApprove').append('<div id="none_info" style="text-align: center;display: none;"><h3>'+res.message+'</h3></div>');
                            } else {
                                if (!$('#none_info').html()) {
                                    $('#none_info').html('<h3>'+res.message+'</h3>');
                                }
                            }
                            $('#none_info').show();
                        } else {
                            stopFlag = $('#changBtn').attr('btn_value');
                        }
                        haveReceivedData = false;
                        playVideoForRequest = true;
                        //前端定时取获取出款请求
                        if (timeInterval) {
                            clearTimeout(timeInterval);
                        }
                        timeInterval = setTimeout(_continueGetInterval, 5*1000);
                    }
                }
                else {
                    $('#info_in1').hide();
                    $('#info_in2').hide();

                    if (!document.getElementById('none_info')) {
                        $('#toApprove').append('<div id="none_info" style="text-align: center;display: none;"><h3>没有出款审核数据</h3></div>');
                    } else {
                        if (!$('#none_info').html()) {
                            $('#none_info').html('<h3>没有出款审核数据</h3>');
                        }
                    }
                    $('#none_info').show();
                }
            },
            error: function (res) {
                if (res) {
                    haveReceivedData =false;
                    if (timeInterval){
                        clearTimeout(timeInterval);
                        timeInterval = null;
                    }
                }
            }
        });
    }
}
/**
 * 根据审核id 获取会员出款相关信息
 * @param id
 * @private
 */
function _getRelatedInfo() {
    var id = $('#toAppoveIdRequest').val();
    var orderNo = $('#orderNoInputRequest').val();
    if (id) {
        $.ajax({
            type: 'get',
            url: '/r/out/getRelatedInfo/' + id + '/' + orderNo,
            dataType: 'json',
            //async:false,
            success: function (res) {
                if (res) {
                    if (res.status == 1) {
                        if (res.data){
                            var jsonData = JSON.parse(res.data);
                            //上次出款后余额 如果没有 则计算上次出款前余额-上次出款余额
                            var balanceAfterLastDraw = jsonData.balanceAfterLastDraw?parseFloat(jsonData.balanceAfterLastDraw):0;
                            var fUpWithdrawBalance = jsonData.fUpWithdrawBalance?parseFloat(jsonData.fUpWithdrawBalance):0;
                            var fUpWithdrawAmount = jsonData.fUpWithdrawAmount?parseFloat(jsonData.fUpWithdrawAmount):0;
                            var afterDraw = fUpWithdrawBalance-fUpWithdrawAmount
                            $('#balanceLastDraw').text(parseFloat(balanceAfterLastDraw?balanceAfterLastDraw:afterDraw).toFixed(3));
                            //当前余额
                            var fWithdrawBalance =jsonData.fWithdrawBalance?parseFloat(jsonData.fWithdrawBalance):0;
                            $('#balanceBeforeDraw').text(fWithdrawBalance);
                            //本次出款金额
                            var fWithdrawAmount = jsonData.fWithdrawAmount?parseFloat(jsonData.fWithdrawAmount):parseFloat($('#amountToApprove').text());
                            //本次出款后余额
                            var afterDrawCurrent =parseFloat(fWithdrawBalance)-parseFloat(fWithdrawAmount);
                            $('#balanceAfterDraw').text(parseFloat(afterDrawCurrent).toFixed(3));
                            //会员入款总额
                            var fAllDepositAmount = jsonData.fAllDepositAmount?parseFloat(jsonData.fAllDepositAmount):0;
                            //最近入款明细总额  目前没有传来 最近入款总额 jsonData.recentDepositTotalAmount
                            var str = parseFloat(jsonData.recentDepositTotalAmount?jsonData.recentDepositTotalAmount:0).toFixed(3)+'<a onclick="_incomeDetailClick();" href="javascript:void (0);" title="上一次出款成功到本次出款之间会员所有的入款和福利。" style="color: #00B83F">(入款详情)</a>';
                            $('#sumAmountsRecentDeposit').empty().html(str);

                            //本次下注金额 ,本次已达投注量
                            var fBetAmount = jsonData.fBetAmount?parseFloat(jsonData.fBetAmount):0;
                            $('#fBetAmount').text(fBetAmount);
                            //本次中奖金额 改为本次盈利
                            var fOmProfit = jsonData.fOmProfit?parseFloat(jsonData.fOmProfit):0;
                            $('#fWinAmount').text(fOmProfit);
                            // //本次已达投注量
                            // $('#needCodesAll').text(fBetAmount);
                            // //本次要求打码量
                            // var fBetNeed =jsonData.fBetNeed?parseFloat(jsonData.fBetNeed):0;
                            // $('#needCodes').text(fBetNeed);
                            //几倍打码量: 如果有最近的入款明细,则为入款总额除以本次已达打码量,否则为本次已达打码量
                            var ratio = fBetAmount!=0?parseFloat(jsonData.recentDepositTotalAmount?jsonData.recentDepositTotalAmount:0)/parseFloat(fBetAmount):0;
                            $('#timesCodes').text(parseFloat(ratio).toFixed(3));
                            //中奖率 本次中奖金额/已达投注量  盈利 平台目前没有传来
                            var fWinAmount = jsonData.fWinAmount?parseFloat(jsonData.fWinAmount):0;
                            var winOdds = fBetAmount!=0?parseFloat(parseFloat(fWinAmount)-parseFloat(fBetAmount))/parseFloat(fBetAmount):0;
                            $('#winRate').text(parseFloat(winOdds*100).toFixed(2) + '%');
                            //会员备注 新平台传来 ₩单行字符分隔符  ₦多行间分隔符
                            var sUserRemark =jsonData.sUserRemark?jsonData.sUserRemark:'';
                            if(sUserRemark){
                                var reg1 =new RegExp("₩","g");
                                var reg2 =new RegExp("₦","g");
                                sUserRemark = sUserRemark.replace(reg1," ");
                                sUserRemark = sUserRemark.replace(reg2,"<br>");
                            }
                            $('#remarkForMember').html(sUserRemark);
                            //是否首次出款
                            var iWithdrawStatus = jsonData.iWithdrawStatus==1?"是":"否";
                            $('#isFirstOut').text(iWithdrawStatus);
                            //注册时间
                            var dRegTimeTEXT = jsonData.dRegTimeTEXT?jsonData.dRegTimeTEXT:'';
                            $('#registerTime').text(dRegTimeTEXT);
                            //最后登陆时间
                            var dLoginTimeTEXT =jsonData.dLoginTimeTEXT?jsonData.dLoginTimeTEXT:'';
                            $('#lastLoginTime').text(dLoginTimeTEXT);
                            //注册IP
                            var registerIp = jsonData.registerIp?jsonData.registerIp:'';
                            $('#regesterIp').empty().html(registerIp+'<button onclick="_lookupIPAttribution(\''+registerIp+'\');" class="pull-right btn btn-xs btn-white btn-info">归属地</button>');
                            //最后登陆IP
                            var lastLoginIp =jsonData.lastLoginIp?jsonData.lastLoginIp:'';
                            $('#lastLoginIp').empty().html(lastLoginIp+'<button onclick="_lookupIPAttribution(\''+lastLoginIp+'\');" class="pull-right btn btn-xs btn-white btn-info">归属地</button>');
                            //总盈利 = 总出款-总入款
                            var fAllWithdrawAmount = jsonData.fAllWithdrawAmount;
                            var allWin = fAllWithdrawAmount?(fAllDepositAmount?parseFloat(fAllWithdrawAmount)-parseFloat(fAllDepositAmount):parseFloat(fAllWithdrawAmount)):0;
                            var title = '总盈利('+parseFloat(allWin)+')=总出款('+fAllWithdrawAmount+')-总入款('+fAllDepositAmount+')';
                            $('#winTotal').empty().html('<a title="'+title+'" style="color: red">'+parseFloat(allWin).toFixed(3)+'</a>');
                        }
                    } else {
                        $('#balanceLastDraw').text('');
                        //当前余额
                        $('#balanceBeforeDraw').text('');
                        //本次出款金额
                        //本次出款后余额
                        $('#balanceAfterDraw').text('');
                        //最近入款总额
                        //最近入款明细总额
                        $('#sumAmountsRecentDeposit').empty().html('');
                        //最近入款明细
                        //$('#modal-incomeDetail').find('tbody').empty().html('<tr><td colspan="8" style="text-align: center"><h3>暂无入款明细</h3></td></tr>');
                        //本次下注金额 ,本次已达投注量
                        $('#fBetAmount').text('');
                        //本次中奖金额
                        $('#fWinAmount').text('');
                        //本次已达投注量
                        $('#needCodesAll').text('');
                        //本次要求打码量
                        $('#needCodes').text('');
                        //几倍打码量: 如果有最近的入款明细,则为入款总额除以本次已达打码量,否则为本次已达打码量
                        $('#timesCodes').text('');
                        //中奖率 本次中奖金额/已达投注量
                        $('#winRate').text('');
                        //会员备注
                        $('#remarkForMember').text('');
                        //是否首次出款
                        $('#isFirstOut').text('');
                        //注册时间
                        $('#registerTime').text('');
                        //最后登陆时间
                        $('#lastLoginTime').text('');
                        //注册IP
                        $('#regesterIp').text('');
                        //最后登陆IP
                        $('#lastLoginIp').text('');

                        //ip-accounts
                        $('#modal-ipAccsDetail').find('tbody').empty().html('<tr><td colspan="4" style="text-align: center"><h3>暂无明细</h3></td></tr>');
                        //account-ips
                        $('#modal-accIpsDetail').find('tbody').empty().html('<tr><td colspan="4" style="text-align: center"><h3>暂无明细</h3></td></tr>');
                        $('#winTotal').empty().html('');
                    }
                }else{
                    $('#memberOutDrawInfo').empty().html('<tr><td colspan="16"><h4>'+res.message+'</h4></td></tr>');
                }
            },
            error:function (XMLHttpRequest, textStatus) {
                if (XMLHttpRequest.readyState == 4) {
                    if ((XMLHttpRequest.status >= 200 && XMLHttpRequest.status < 300) || XMLHttpRequest.status == 304) {
                        $.gritter.add({
                            time: 5000,
                            class_name: '',
                            title: '系统消息',
                            text: "网络连接异常"+textStatus+",请稍后刷新页面",
                            sticky: false,
                            image: '../images/message.png'
                        });
                    }
                }
            }
        });
    }
}
//查看IP归属地
function _lookupIPAttribution(ip) {
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
                $('#modal-attribution-ip-tbody').empty().html(tr);
                $('#modal-attribution-ip').modal('show');
            }
        })
    }
}
//最近入款明细
function _showDepositDetail(data) {
    sumAmounts = 0;
    if (data) {
        var tr = '';
        for(var i=0;i<data.length;i++){
            sumAmounts += parseFloat(data[i].fAmount);
            tr +=
                '<tr>' +
                '<td>' + (data[i].sOrderId?data[i].sOrderId:'') + '</td>' +
                '<td>' + (data[i].fAmount?data[i].fAmount:'' )+ '</td>' +
                '<td>' + (data[i].sDepositType?data[i].sDepositType:'')+ '</td>' +
                // '<td>' + (data[i].fBetNeeds? data[i].fBetNeeds:'' )+ '</td>' +
                '<td>' + (data[i].dCreateTimeTEXT? data[i].dCreateTimeTEXT:'' )+ '</td>' +
                '</tr>';
        }
        $('#modal-incomeDetail').find('tbody').empty().html(tr);
        var trs = '<tr>'
            + '<td>合 计：</td>'
            + '<td id="recentlyInAmount" bgcolor="#D6487E" style="color:white;">' + parseFloat(sumAmounts).toFixed(3) + '</td>'
            + '<td colspan="3"></td>'
            + '</tr>';
        $('#modal-incomeDetail').find('tbody').append(trs);
    }else{
        $('#modal-incomeDetail').find('tbody').empty().append('<tr><td colspan="10" style="text-align: center"><h4>暂无明细</h4></td></tr>');
    }
    $('#modal-incomeDetail').modal('show');
}
//获取会员登陆信息和IP信息
function _memberLoginiPInfo(type) {
    if(type){
        $.ajax({
            type:'get',
            url:'/r/synch/getInfo',
            data:{"type":type,"orderNo":$.trim($('#orderNo').text())},
            dataType:'json',
            success:function (res) {
                if(res && res.data &&res.message.indexOf('失败')<0){
                    var data =  JSON.parse(JSON.parse(res.data).message);//[{},{}]
                    var tr = '';
                    if(type==3){
                        if(data){
                            for (var i=0;i<data.length;i++){
                                tr +=
                                    '<tr>' +
                                    '<td>' + _checkObj(data[i].account)+ '</td>' +
                                    '<td>' + _checkObj(data[i].loginIp) + '<button onclick="_lookupIPAttribution(\''+_checkObj(data[i].loginIp)+'\');" class="pull-right btn btn-xs btn-white btn-info">归属地</button></td>' +
                                    '<td>' +_checkObj(data[i].loginTime) + '</td>' +
                                    '</tr>';
                            }
                            $('#modal-accIpsDetail').find('tbody').empty().html(tr);
                        }else{
                            $('#modal-accIpsDetail').find('tbody').empty().html('<tr style="text-align: center"><td colspan="10"><h4>暂无明细</h4></td></tr>');
                        }
                        $('#modal-accIpsDetail').modal('show');
                    }else{
                        if(data){
                            for (var i=0;i<data.length;i++){
                                tr +=
                                    '<tr>' +
                                    '<td>' + _checkObj(data[i].ip )+ '<button onclick="_lookupIPAttribution(\''+_checkObj(data[i].ip )+'\');" class="pull-right btn btn-xs btn-white btn-info">归属地</button></td>' +
                                    '<td>' + _checkObj(data[i].userName) + '</td>' +
                                    '<td>' +_checkObj(data[i].loginTime) + '</td>' +
                                    '</tr>';
                            }
                            $('#modal-ipAccsDetail').find('tbody').empty().html(tr);
                        }else{
                            $('#modal-ipAccsDetail').find('tbody').empty().html('<tr style="text-align: center"><td colspan="10"><h4>暂无明细</h4></td></tr>');
                        }
                        $('#modal-ipAccsDetail').modal('show');
                    }
                }else{
                    if(res.message.indexOf('失败')<0){
                        if(type==3){
                            $('#modal-accIpsDetail').find('tbody').empty().html('<tr><td colspan="10" style="text-align: center"><h4>暂无明细</h4></td></tr>');
                        } else{
                            $('#modal-ipAccsDetail').find('tbody').empty().html('<tr><td colspan="10" style="text-align: center"><h4>暂无明细</h4></td></tr>');
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
//审核操作 1 审核通过 2 取消 3 拒绝 4 转主管
function _save(type) {
    var id = $('#toAppoveIdRequest').val();
    var remark = $('#remarkForApprove').val();
    var orderNoInput = $('#orderNoInputRequest').val();
    var memberCode = null;
    if ($('#memberCodeRequest').val()) {
        memberCode = $('#memberCodeRequest').val();
    }
    var prompt = '';
    if (type == 1) {
        prompt = '确定审核通过吗？';
    }
    if (type == 2 || type == 3 || type == 4) {
        if (!remark) {
            $('#remarkPrompt2').show().delay(500).fadeOut();
            return false;
        }
        switch (type) {
            case 2:
                prompt = '确定取消吗？';
                break;
            case 3:
                prompt = '确定拒绝吗？';
                break;
            case 4:
                prompt = '确定转主管吗？';
                break;
            default:
                prompt = '';
        }
    }

    bootbox.confirm(prompt, function (res) {
        if (res) {
            _executeSave(id, $.trim(remark), type, $.trim(orderNoInput), $.trim(memberCode));
        }
    });

}
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
                $('#toAppoveIdRequest').val('');//审核出款请求id
                $('#orderNoInputRequest').val('');
                haveReceivedData = false;
                $.session.remove('toAppoveId');
                _continueTask();//操作完成 接续接单

            }
            $.gritter.add({
                time: 600,
                class_name: '',
                title: '系统消息',
                text: res.message,
                sticky: false,
                image: '../images/message.png'
            });
            return false;
        }
    });
}
/**操作完成继续接单*/
function _continueTask() {
    var pauseFlaggs = $.session.get("pauseFlaggs");
    if (pauseFlaggs == '1') {
        _showPauseButton(pauseFlaggs,false);
    } else {
        _showPauseButton($.session.get('pauseFlaggs'),false);
    }
    $('#remarkForApprove').val("");
}
$('#but_pass').click(function () {
    _save(1);
});
$('#but_cancel').click(function () {
    _save(2);
});
$('#but_refuse').click(function () {
    _save(3);
});
$('#but_transfer').click(function () {
    _save(4);
});
//查看各个明细
$('#lastTimeDrawDetail').unbind().bind('click', function () {
    $('#modal-lastDepositDetail').modal('show');
});
function _incomeDetailClick() {
    $.ajax({
        type:'post',
        url:'/r/synch/getInfo',
        dataType:'json',
        async:false,
        data:{'type':2,'orderNo':$.trim($('#orderNo').text())},
        success:function (res) {
            if(res && res.data && res.message.indexOf('失败')<0 ){
                var data = JSON.parse(JSON.parse(res.data).message);//[{},{}]
                _showDepositDetail(data);
            }else{
                if(res.message.indexOf('失败')<0 ){
                    $('#modal-incomeDetail').find('tbody').empty().append('<tr><td colspan="10" style="text-align: center"><h4>暂无明细</h4></td></tr>');
                    $('#modal-incomeDetail').modal('show');
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
$('#accIpsDetail').unbind().bind('click', function () {
    _memberLoginiPInfo(3);
});
$('#ipAccsDetail').unbind().bind('click', function () {
    _memberLoginiPInfo(4);
});

/**
 * 已审核
 */
function _doneApprove() {
    var member = $('#member2').val();
    if (member && member.indexOf('%') >= 0) {
        member = $.trim(member.replace(new RegExp(/%/g), '?'));
    }
    var orderNo = $('#orderNo2').val();
    if (orderNo && orderNo.indexOf('%') >= 0) {
        orderNo = orderNo.replace(new RegExp(/%/g), '?');
    }
    var startTime = "", endTime = "";
    var startAndEnd = $('#timeScope2').val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    }else {
        startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
        endTime = moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    }
    var fromMoney = $('#fromMoney2').val();
    var toMoney = $('#toMoney2').val();
    var handicap = $('#handicap_1').val() == '请选择' ? '' : $('#handicap_1').val();
    var level = $('#level_1').val() == '请选择' ? '' : $('#level_1').val();

    var CurPage = $("#approved_footPage").find(".Current_Page").text();
    if (!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var params = {
        "member": member,"orderNo": orderNo,"startTime": startTime,"endTime": endTime,
        "fromMoney": fromMoney,"toMoney": toMoney,"handicap": handicap,"level": level,
        "pageNo": CurPage,"pageSize": $.session.get('initPageSize')
    };
    var data = $.extend({"flag": 2}, params);
    $.each(ContentRight['OutwardAudit:*'], function (name, value) {
        if (name == 'OutwardAudit:lookUpAuditedAmount:*') {
            lookUpAuditedAmount = true;
        }
    });
    $.ajax({
        type: 'get',
        url: '/r/out/get',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res.status == 1) {
                var str = '', trs = '' ,amount = 0;
                if (res.data && res.data[0].dataList && res.data[0].dataList.length > 0) {
                    //完成审核 tab2
                    $(res.data[0].dataList).each(function (i, val) {
                        str +=
                            '<tr>'
                            + '<td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>'
                            + '<td>' + _showLevelNameByIdOrCode(val.level )+ '</td>'
                            + '<td>' + _checkObj(val.member) + '</td>';
                        if (_checkObj(val.member)&&_checkObj(val.member)!='公司用款'){
                            if (!isHideAccount)
                            str += '<td><a href="javascript:_showOrderDetail(' + val.id + ',\'' + val.orderNo + '\');">' + val.orderNo + '</a></td>';
                            else str += '<td>'+ _checkObj(val.orderNo) + '</td>';
                        }else{
                            str += '<td>'+ _checkObj(val.orderNo) + '</td>';
                        }
                        str += '<td>' + _checkObj(val.amount) + '</td>'
                            + '<td>' + timeStamp2yyyyMMddHHmmss(val.applyTime) + '</td>'
                            + '<td>' + timeStamp2yyyyMMddHHmmss(val.approveTime) + '</td>'
                            + '<td>' + _show(val.timeConsuming) + '</td>';
                        if (val.remark) {
                            if (_checkObj(val.member)&&_checkObj(val.member)!='公司用款'){
                                str +=
                                    '<td>'
                                    + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + val.remark + '">'
                                    + _checkObj(val.remark).substring(0, 5)
                                    + '</a>'
                                    +'</td>'
                                    +'</tr>';
                            }else{
                                str +=
                                    '<td>'
                                    + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + val.remark + '">'
                                    + '盘口用款'
                                    + '</a>'
                                    +'</td>'
                                    +'</tr>';
                            }

                        }else {
                            str += '<td></td></tr>';
                        }
                        amount += val.amount;//review
                    });
                    if(outwardCurrentPageSum){
                        trs +='<tr><td id="outwardReqcurrentPageCount_2" colspan="4">小计：统计中..</td>' +
                            '<td id="amount_2" bgcolor="#579EC8" style="color:white;">' + new Number(amount).toFixed(3) + '</td>' +
                            '<td colspan="6"></td></tr> ' ;
                    }else{
                        trs +='<tr><td id="outwardReqcurrentPageCount_2" colspan="15">小计：统计中..</td></tr> ' ;
                    }
                    if(outwardAllRecordSum){
                        trs +='<tr><td id="outwardReqPageCount_2" colspan="4">总计：统计中..</td>' +
                            '<td id="sumAmount_2" bgcolor="#D6487E" style="color:white;width: 130px;display: none;">统计中..</td>' +
                            '<td colspan="10"></td></tr>';
                    }else{
                        trs +='<tr><td id="outwardReqPageCount_2" colspan="19">总计：统计中..</td></tr>';
                    }
                }
                $('#tbody2').empty().html(str).append(trs);
                $("[data-toggle='popover']").popover();
                if(outwardAllRecordSum){
                    _getOutWardRequestSum(data,2);
                }
                _getOutWardRequestCount(data,2);
                showPading(res.data[0].page, 'approved_footPage', _doneApprove);
            }
        }
    });
};
function _getOutWardRequestSum(data,flag) {
    $.ajax({
        type:'get',
        url:'/r/out/getOutWardRequestSum',
        data:data,
        dataType:'json',
        success:function (res) {
            if (res) {
                if (res.status==1) {
                    $('#sumAmount_'+flag).text(parseFloat(res.data.sumAmount).toFixed(3));
                    if(flag==2){
                        if(lookUpAuditedAmount){
                            $('#sumAmount_2').show();
                        }else{
                            $('#sumAmount_2').text('无权限查看');
                            $('#sumAmount_2').show();
                        }
                    }
                }
            }
        }
    });
}
function _getOutWardRequestCount(data,type) {
    $.ajax({
        type:'get',
        url:'/r/out/getOutWardRequestCount',
        data:data,
        dataType:'json',
        success:function (res) {
            if (res) {
                if (res.status==1 && res.page) {
                    $('#outwardReqcurrentPageCount_'+type ).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#outwardReqPageCount_'+type ).empty().text('合计：' + res.page.totalElements + '条记录');
                    if (type==2) {
                        showPading(res.page, 'approved_footPage', _doneApprove);
                    }
                    if (type==3) {
                        showPading(res.page, 'refused_footPage', _refused);
                    }
                    if (type==4) {
                        showPading(res.page, 'master_footPage', _masterHandle);
                    }
                    if (type==5) {
                        showPading(res.page, 'canceled_footPage', _canceled);
                    }
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
                    _initialOrderText(id+'detail', res.data);
                }
            }
        }
    });
}

/**
 * 显示 审核耗时
 * @param obj
 * @returns {string}
 * @private
 */
function _show(obj) {
    var ob = '';
    if (obj) {
        ob = obj;
    }
    return ob;
};
/**
 * 已拒绝
 */
function _refused() {
    var member = $('#member3').val();
    if (member && member.indexOf('%') >= 0) {
        member = member.replace(new RegExp(/%/g), '?');
    }
    var orderNo = $('#orderNo3').val();
    if (orderNo && orderNo.indexOf('%') >= 0) {
        orderNo = orderNo.replace(new RegExp(/%/g), '?');
    }
    var startTime = "", endTime = "";
    var startAndEnd = $('#timeScope3').val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    }else {
        startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
        endTime = moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    }
    var fromMoney = $('#fromMoney3').val();
    var toMoney = $('#toMoney3').val();
    var handicap = $('#handicap_2').val() == '请选择' ? '' : $('#handicap_2').val();
    var level = $('#level_2').val() == '请选择' ? '' : $('#level_2').val();
    var CurPage = $("#refused_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var params = {
        "member": member,"orderNo": orderNo, "startTime": startTime,"endTime": endTime,
        "fromMoney": fromMoney, "toMoney": toMoney,"handicap": handicap,"level": level,
        "pageNo": CurPage,"pageSize": $.session.get('initPageSize')
    };
    var data = $.extend({"flag": 3}, params);
    $.ajax({
        type: 'get',
        url: '/r/out/get',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res.status == 1) {
                var str = '', trs = '',amount = 0;
                if (res.data && res.data[0].dataList && res.data[0].dataList.length > 0) {
                    //已拒绝
                    $(res.data[0].dataList).each(function (i, val) {
                        str +=
                            '<tr>'
                            + '<td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>'
                            + '<td>' + _showLevelNameByIdOrCode(val.level )+ '</td>'
                            + '<td>' + _checkObj(val.member )+ '</td>';
                        if (_checkObj(val.member)&& _checkObj(val.member)!='公司用款'){
                            if (!isHideAccount)
                            str  += '<td><a href="javascript:_showOrderDetail(' + val.id + ',\'' + val.orderNo + '\');">' + val.orderNo + '</a></td>';
                            else  str += '<td>' + _checkObj(val.orderNo )+ '</td>';
                        }else{
                            str += '<td>' + _checkObj(val.orderNo )+ '</td>';
                        }
                        str +='<td>' + _checkObj(val.amount )+ '</td>'
                            + '<td>' + timeStamp2yyyyMMddHHmmss(val.applyTime) + '</td>'
                            + '<td>' + timeStamp2yyyyMMddHHmmss(val.approveTime) + '</td>';
                        if (val.remark) {
                            str +=
                               '<td>'
                                + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + val.remark + '">'
                                + _checkObj(val.remark).substring(0, 5) + "..."
                                + '</a>'
                                +'</td>'
                                +'</tr>';
                        }else {
                            str += '<td></td></tr>';
                        }
                        amount += val.amount;
                    });
                    if(outwardCurrentPageSum){
                        trs +='<tr><td id="outwardReqcurrentPageCount_3" colspan="4">小计：统计中..</td>' +
                            '<td id="amount_3" bgcolor="#579EC8" style="color:white;">' + parseFloat(amount).toFixed(3) + '</td>' +
                            '<td colspan="6"></td></tr> ' ;
                    }else{
                        trs +='<tr><td id="outwardReqcurrentPageCount_3" colspan="15">小计：统计中..</td></tr> ' ;
                    }
                    if(outwardAllRecordSum){
                        trs +='<tr><td id="outwardReqPageCount_3" colspan="4">总共：统计中..</td>' +
                            '<td id="sumAmount_3" bgcolor="#D6487E" style="color:white;width: 130px;">统计中..</td>' +
                            '<td colspan="6"></td></tr>';
                    }else{
                        trs +='<tr><td id="outwardReqPageCount_3" colspan="19">总共：统计中..</td></tr>';
                    }
                }
                $('#tbody3').empty().html(str).append(trs);
                if(outwardAllRecordSum){
                    _getOutWardRequestSum(data,3);
                }
                _getOutWardRequestCount(data,3);
                showPading(res.data[0].page, 'refused_footPage', _refused);
                $("[data-toggle='popover']").popover();
            }
        }
    });
}
/**
 * 已取消
 */
function _canceled() {
    var member = $('#member4').val();
    if (member && member.indexOf('%') >= 0) {
        member = member.replace(new RegExp(/%/g), '?');
    }
    var orderNo = $('#orderNo4').val();
    if (orderNo && orderNo.indexOf('%') >= 0) {
        orderNo = orderNo.replace(new RegExp(/%/g), '?');
    }
    var startTime = "", endTime = "";
    var startAndEnd = $('#timeScope4').val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    }else {
        startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
        endTime = moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    }
    var fromMoney = $('#fromMoney4').val();
    var toMoney = $('#toMoney4').val();
    var handicap = $('#handicap_3').val() == '请选择' ? '' : $('#handicap_3').val();
    var level = $('#level_3').val() == '请选择' ? '' : $('#level_3').val();
    var CurPage = $("#canceled_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var params = {
        "member": member,"orderNo": orderNo,"startTime": startTime, "endTime": endTime,
        "fromMoney": fromMoney, "toMoney": toMoney, "handicap": handicap,"level": level,
        "pageNo": CurPage,"pageSize": $.session.get('initPageSize')
    };
    var data = $.extend({"flag": 5}, params);
    $.ajax({
        type: 'get',
        url: '/r/out/get',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res.status == 1) {
                var str = '', trs = '', amount = 0;
                if (res.data && res.data[0].dataList && res.data[0].dataList.length > 0) {
                    //已取消
                    $(res.data[0].dataList).each(function (i, val) {
                        str +=
                            '<tr>'
                            + '<td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>'
                            + '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>'
                            + '<td>' + _checkObj(val.member) + '</td>';
                        if (_checkObj(val.member)&& _checkObj(val.member)!='公司用款'){
                            if (!isHideAccount)
                            str  += '<td><a href="javascript:_showOrderDetail(' + val.id + ',\'' + val.orderNo + '\');">' + val.orderNo + '</a></td>';
                            else  str += '<td>' + _checkObj(val.orderNo) + '</td>';
                        }else{
                            str += '<td>' + _checkObj(val.orderNo) + '</td>'
                        }
                        str += '<td>' + _checkObj(val.amount) + '</td>'
                            + '<td>' + timeStamp2yyyyMMddHHmmss(val.applyTime) + '</td>'
                            + '<td>' + timeStamp2yyyyMMddHHmmss(val.approveTime) + '</td>';
                        if (val.remark) {
                            str +=
                                '<td>'
                                + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                + ' data-content="' + val.remark + '">'
                                + _checkObj(val.remark).substring(0, 5) + "..."
                                + '</a>'
                                +'</td>'
                                +'</tr>';
                        }else {
                            str += '<td></td></tr>';
                        }
                        amount += val.amount;
                    });
                    if(outwardCurrentPageSum){
                        trs +='<tr><td id="outwardReqcurrentPageCount_5" colspan="4">小计：统计中..</td>' +
                            '<td id="amount_5" bgcolor="#579EC8" style="color:white;">' + parseFloat(amount).toFixed(3) + '</td>' +
                            '<td colspan="6"></td></tr> ' ;
                    }else{
                        trs +='<tr><td id="outwardReqcurrentPageCount_5" colspan="19">小计：统计中..</td></tr> ' ;
                    }
                    if(outwardAllRecordSum){
                        trs +='<tr><td id="outwardReqPageCount_5" colspan="4">总共：统计中..</td>' +
                            '<td id="sumAmount_5" bgcolor="#D6487E" style="color:white;width: 130px;">统计中..</td>' +
                            '<td colspan="6"></td></tr>';
                    }else{
                        trs +='<tr><td id="outwardReqPageCount_5" colspan="19">总共：统计中..</td></tr>';
                    }
                }
                $('#tbody4').empty().html(str).append(trs);
                if(outwardAllRecordSum){
                    _getOutWardRequestSum(data,5);
                }
                _getOutWardRequestCount(data,5);
                showPading(res.data[0].page, 'canceled_footPage', _canceled);
                $("[data-toggle='popover']").popover();
            }
        }
    });
}
/**
 * 主管处理
 */
function _masterHandle() {
    var member = $('#member5').val();
    if (member && member.indexOf('%') >= 0) {
        member = member.replace(new RegExp(/%/g), '?');
    }
    var orderNo = $('#orderNo5').val();
    if (orderNo && orderNo.indexOf('%') >= 0) {
        orderNo = orderNo.replace(new RegExp(/%/g), '?');
    }
    var startTime = "", endTime = "";
    var startAndEnd = $('#timeScope5').val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    }else {
        startTime = moment().hours(07).minutes(0).seconds(0).format("YYYY-MM-DD HH:mm:ss");
        endTime = moment().add(1, 'days').hours(06).minutes(59).seconds(59).format("YYYY-MM-DD HH:mm:ss");
    }
    var fromMoney = $('#fromMoney5').val();
    var toMoney = $('#toMoney5').val();
    var handicap = $('#handicap_4').val() == '请选择' ? '' : $('#handicap_4').val();
    var level = $('#level_4').val() == '请选择' ? '' : $('#level_4').val();
    var CurPage = $("#master_footPage").find(".Current_Page").text();
    if (!!!CurPage) {
        CurPage = 0;
    } else {
        CurPage = CurPage - 1;
    }
    if (CurPage < 0) {
        CurPage = 0;
    }
    var params = {
        "member": member, "orderNo": orderNo,"startTime": startTime,"endTime": endTime,
        "fromMoney": fromMoney, "toMoney": toMoney,"handicap": handicap,"level": level,
        "pageNo": CurPage,"pageSize": $.session.get('initPageSize')
    };
    var data = $.extend({"flag": 4}, params);
    $.ajax({
        type: 'get',
        url: '/r/out/get',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res.status == 1) {
                var str = '',trs = '',amount = 0;
                if (res.data && res.data[0].dataList && res.data[0].dataList.length > 0) {
                    //主管处理
                    $(res.data[0].dataList).each(function (i, val) {
                        str +=
                            '<tr>'
                            + '<td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>'
                            + '<td>' + _showLevelNameByIdOrCode(val.level) + '</td>'
                            + '<td>' + _checkObj(val.member) + '</td>';
                        if (_checkObj(val.member)&& _checkObj(val.member)!='公司用款'){
                            if (!isHideAccount)
                            str  += '<td><a href="javascript:_showOrderDetail(' + val.id + ',\'' + val.orderNo + '\');">' + val.orderNo + '</a></td>';
                            else str += '<td>' + _checkObj(val.orderNo) + '</td>';
                        }else{
                            str += '<td>' + _checkObj(val.orderNo) + '</td>';
                        }
                        str +='<td>' + _checkObj(val.amount) + '</td>'
                            + '<td>' + timeStamp2yyyyMMddHHmmss(val.applyTime) + '</td>'
                            + '<td>' + timeStamp2yyyyMMddHHmmss(val.approveTime) + '</td>';
                        if (val.remark) {
                            if (_checkObj(val.member)){
                                str +=
                                    '<td>'
                                    + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + val.remark + '">'
                                    + _checkObj(val.remark).substring(0, 5)
                                    + '</a>'
                                    +'</td>'
                                    +'</tr>';
                            }else{
                                str +=
                                    '<td>'
                                    + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                                    + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                                    + ' data-content="' + val.remark + '">'
                                    + '盘口用款'
                                    + '</a>'
                                    +'</td>'
                                    +'</tr>';
                            }

                        }else {
                            str += '<td></td></tr>';
                        }
                        amount += val.amount;
                    });
                    if(outwardCurrentPageSum){
                        trs +='<tr><td id="outwardReqcurrentPageCount_4" colspan="4">小计：统计中..</td>' +
                            '<td id="amount_4" bgcolor="#579EC8" style="color:white;">' + parseFloat(amount).toFixed(3) + '</td>' +
                            '<td colspan="6"></td></tr> ' ;
                    }else{
                        trs +='<tr><td id="outwardReqcurrentPageCount_4" colspan="19">小计：统计中..</td></tr> ' ;
                    }
                    if(outwardAllRecordSum){
                        trs +='<tr><td id="outwardReqPageCount_4" colspan="4">总共：统计中..</td>' +
                            '<td id="sumAmount_4" bgcolor="#D6487E" style="color:white;width: 130px;">统计中..</td>' +
                            '<td colspan="6"></td></tr>';
                    }else{
                        trs +='<tr><td id="outwardReqPageCount_4" colspan="19">总共：统计中..</td></tr>';
                    }
                }
                $('#tbody5').empty().html(str).append(trs);
                $("[data-toggle='popover']").popover();
                if(outwardAllRecordSum){
                    _getOutWardRequestSum(data,4);
                }
                _getOutWardRequestCount(data,4);
                showPading(res.data[0].page, 'master_footPage', _masterHandle);
            }
        }
    });
}
/**
 * 重置
 * @param type
 * @private
 */
function _resetValueR(type) {
    initPaging($('#'+type + '_footPage'),pageInitial);
    var member = $('#member' + type).val();
    if (member) {
        $('#member' + type).val('');
    }
    var orderNo = $('#orderNo' + type).val();
    if (orderNo) {
        $('#orderNo' + type).val('');
    }
    _datePickerForAll($("input.date-range-picker"));
    var fromMoney = $('#fromMoney' + type).val();
    if (fromMoney) {
        $('#fromMoney' + type).val('');
    }
    var toMoney = $('#toMoney' + type).val();
    if (toMoney) {
        $('#toMoney' + type).val('');
    }
    var handicap = $('#handicap_' + (type - 1)).val();
    var level = $('#level_' + (type - 1)).val();
    if (handicap && handicap!="请选择") {
        _initialHandicap(type);
    }
    if (level) {
        $('#handicap_' + (type - 1)).change();
    }
    if (type == 2) {
        _doneApprove();
    }
    if (type == 3) {
        _refused();
    }
    if (type == 4) {
        _canceled();
    }
    if (type == 5) {
        _masterHandle();
    }
}
/**
 * 查询按钮事件
 */
$('#button2').on(ace.click_event, function () {
    _doneApprove();
});
$('#button3').on(ace.click_event, function () {
    _refused();
});
$('#button4').on(ace.click_event, function () {
    _canceled();
});
$('#button5').on(ace.click_event, function () {
    _masterHandle();
});
/**
 * 盘口初始化
 */
function _initialHandicap(type) {
    $.ajax({
        type: 'get',
        url: '/r/out/handicap',
        data: {},
        async:false,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    var opt = '<option>请选择</option>';
                    $(res.data).each(function (i, val) {
                        opt += '<option value="' + val.id + '">' + val.name + '</option>';
                    });

                    $('#handicap_' + (type - 1)).empty().html(opt);
                    $('#handicap_' + (type - 1)).trigger('chosen:updated');

                }
            }
        }
    });
}
$('#handicap_1').change(function () {
    var handicap = $(this).val();
    if (handicap.indexOf('请选择') < 0) {
        _initialLevel(handicap, 1);_doneApprove();
    } else {
        $('#level_1').empty().html('<option>请选择</option>');
        $('#level_1').trigger('chosen:updated');
    }
});
$('#handicap_2').change(function () {
    var handicap = $(this).val();
    if (handicap.indexOf('请选择') < 0) {
        _initialLevel(handicap, 2);_refused();

    } else {
        $('#level_2').empty().html('<option>请选择</option>');
        $('#level_2').trigger('chosen:updated');
    }
});
$('#handicap_3').change(function () {
    var handicap = $(this).val();
    if (handicap.indexOf('请选择') < 0) {
        _initialLevel(handicap, 3);_canceled();

    } else {
        $('#level_3').empty().html('<option>请选择</option>');
        $('#level_3').trigger('chosen:updated');
    }
});
$('#handicap_4').change(function () {
    var handicap = $(this).val();
    if (handicap.indexOf('请选择') < 0) {
        _initialLevel(handicap, 4);_masterHandle();

    }
    else {
        $('#level_4').empty().html('<option>请选择</option>');
        $('#level_4').trigger('chosen:updated');
    }
});
/**
 * 初始化层级
 */
function _initialLevel(handicap, type) {
    $('level_' + type).empty();
    if (handicap) {
        $.ajax({
            dataType: 'json',
            type: "get",async:false,
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
function initalHandicaps(type) {
    _initialSelectChosen(type);
    //_initialSelectChosen(3);_initialSelectChosen(4);_initialSelectChosen(5);


}
function _initialSelectChosen(type) {
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
    //_initialHandicap(type);
    //$('#handicap_'+(type-1)+'_chosen').prop('style', 'width: 78%;');
    //$('#level_'+(type-1)+'_chosen').prop('style', 'width: 78%;');

}
//modal-orderDetail-ipAccsDetails
function _closeDepositModalReq() {
    $('#modal-winLoseDetail').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}
function _closeIP2AccountModalReq() {
    $('#modal-ipAccsDetail').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}
function _closeAttribution4IP() {
    $('#modal-attribution-ip').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}
function _closeAccount2IpModalReq() {
    $('#modal-accIpsDetail').modal('hide');
    setTimeout(function () {
        $('body').addClass('modal-open');
    }, 500);
}
function _reqLiClick(type){
    initalHandicaps(type);
    if (type){
        switch (type) {
            case 1:_doneApprove();break;
            case 2:_refused();break;
            case 3:_canceled();break;
            case 4:_masterHandle();break;
            default:break;
        }
    }
}
_showToAuditPage();
$('#info_in2_table').find('td').addClass('tds');
_initPageRequest();
_getBigWinCriterion();
_datePickerForAll($("input.date-range-picker"));
_getPathAndHtml('historyDetail');
