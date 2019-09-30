currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var trHtml='<tr class="noLeftRightPadding">\
                   <td><span>{handicapName}</span></td>\
				   <td><span>{currSysLevelName}</span></td>\
				   <td><span>{mobile}</span><span target="{id}" class="time4St bal label label-grey" style="display:block;width:99%;float:left;" title="">离线</span></td>\
                   <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a><span class="errorAlarm error{id}">{error}</span><span target="{id}" class="time4St flow label label-grey" style="display:block;width:99%;float:left;" title="">&nbsp;&nbsp;</span></td>\
                   <td><span>{typeStr}</span></td>\
                   <td><span class="mode4Acc" target="{id}">默认</span></td>\
                   <td><span class="label label-sm {classOfStatus}">{statusName}</span></td>\
                   <td><span>{peakBalance}</span></td>\
                   <td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span></div></td>\
                   <td>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" contentRight="BizMobileMonitor:CommandLogin:*" onclick="showLogin({id})"><i class="ace-icon fa fa-exchange bigger-100 orange"></i><span>测试</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" contentRight="BizMobileMonitor:CommandConciliate:*" onclick="showConciliate({id});" ><i class="ace-icon fa fa-check bigger-100 orange"></i><span>对账</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" contentRight="BizMobileMonitor:CommandConciliate:*" onclick="showCacheFlow({id});" ><i class="ace-icon fa fa-check bigger-100 orange"></i><span>补发流水</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" contentRight="BizMobileMonitor:CommandReAck:*" onclick="showReAck({id});" ><i class="ace-icon fa fa-reply bigger-100 orange"></i><span>任务回收</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" contentRight="BizMobileMonitor:CommandShowLogs:*" onclick="showLogDate({id});" ><i class="ace-icon fa fa-download bigger-100 orange"></i><span>日志</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" contentRight="BizMobileMonitor:CommandUpdPWD:*" onclick="showUpdPWD({id});" ><i class="ace-icon fa fa-asterisk bigger-100 orange"></i><span>密码</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" contentRight="BizMobileMonitor:CommandChangeModel:*" onclick="showSetting4ChangeModel({id});" ><i class="ace-icon fa fa-cog bigger-100 orange"></i><span>设置</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight" contentRight="BizMobileMonitor:CommandDoScreen:*" onclick="doScreen({id});" ><i class="ace-icon fa fa-film bigger-100 orange"></i><span>截屏</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold contentRight orange" contentRight="BizMobileMonitor:CommandShowInOutList:*" onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>'
                    +'<button class="dsn contentRight" contentRight="BizMobileMonitor:SMS:*" smstarget="{id}" onclick="showSMS({id},\'{mobile}\',\'{accountInfo}\')"><i class="ace-icon fa fa-envelope-o bigger-100 orange"></i><span>短信</span></button>'
                    +'</td>\
               </tr>';

var showLogin = function(accId){
    bootbox.confirm("确定测试转账&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/outtask/out/test',data: {"accId":accId},dataType: 'json',success: function (res) {
                    showMessageForSuccess(res.message);
            }});
        }
    });
};

var showConciliate = function(accId){
    var html =   '<div id="choiceExportModal_InOut" class="modal fade " tabindex="-1">';
    html = html+ '   <input type="hidden" id="accountId"/>';
    html = html+ '   <input type="hidden" id="operaType"/>';
    html = html+ '   <input type="hidden" id="exBankType"/>';
    html = html+ '   <div class="modal-dialog modal-lg" style="width:400px;">';
    html = html+ '      <div class="modal-content">';
    html = html+ '         <div class="modal-header no-padding text-center">';
    html = html+ '            <div class="table-header">';
    html = html+ '               <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>请选择对账时间</span>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '         <div class="modal-body">';
    html = html+ '            <div class="widget-box">';
    html = html+ '               <div class="widget-header hide"><h4 class="smaller"></h4></div>';
    html = html+ '                  <div class="widget-body">';
    html = html+ '                      <div class="widget-main">';
    html = html+ '                          <label class="control-label bolder blue">时间</label>&nbsp;&nbsp;';
    html = html+ '                          <span class="input-group-addon sr-only"><i class="fa fa-calendar"></i></span>';
    html = html+ '                          <input class="date-range-picker" type="text" placeholder="请选择对账日期" name="startAndEndTime_export" style="height: 32px;width:280px;"/>';
    html = html+ '                          <div class="control-group">&nbsp;&nbsp;&nbsp;</div>';
    html = html+ '                          <div style="text-align:center;">';
    html = html+ '                              <a class="btn btn-sm btn-success" id="checkButton">';
    html = html+ '                                  <i class="fa fa-check" aria-hidden="true"></i><span>对账</span>';
    html = html+ '                              </a>';
    html = html+ '                          </div>';
    html = html+ '                      </div>';
    html = html+ '                  </div>';
    html = html+ '              </div>';
    html = html+ '         </div>';
    html = html+ '      </div>';
    html = html+ '   </div>';
    html = html+ '</div>';
    var $div=$(html).clone().appendTo($("body"));
    $div.find("#accountId").val(accountId);
    var $timer = $div.find("[name=startAndEndTime_export]");
    $timer.daterangepicker({
        cancel: 'cancel.daterangepicker',
        apply: 'apply.daterangepicker',
        singleDatePicker: true,timePicker: false,
        locale: {
            "format": "YYYY-MM-DD", "separator": " ~ ",
            "applyLabel": "确定", "cancelLabel": "取消", "fromLabel": "从", "toLabel": "到",
            "customRangeLabel": "自定义", "dayNames": ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
            "daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
            "monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
            "firstDay": 1
        }
    });
    $div.find("#checkButton").click(function(){
        $.ajax({type: 'get',url: '/r/cabana/conciliate',data: {"accId":accId,date:$timer.val().split(' ')[0]},dataType: 'json',success: function (res) {
            if (res.status==1) {
                showMessageForSuccess('操作成功');
                $div.modal("toggle");
            }else{
                showMessageForFail(res.message);
            }
        }});
    });
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () { $div.remove(); });
};

var showCacheFlow = function(accId){
    var html =   '<div id="choiceExportModal_InOut" class="modal fade " tabindex="-1">';
    html = html+ '   <input type="hidden" id="accountId"/>';
    html = html+ '   <input type="hidden" id="operaType"/>';
    html = html+ '   <input type="hidden" id="exBankType"/>';
    html = html+ '   <div class="modal-dialog modal-lg" style="width:400px;">';
    html = html+ '      <div class="modal-content">';
    html = html+ '         <div class="modal-header no-padding text-center">';
    html = html+ '            <div class="table-header">';
    html = html+ '               <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>请选择对账时间</span>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '         <div class="modal-body">';
    html = html+ '            <div class="widget-box">';
    html = html+ '               <div class="widget-header hide"><h4 class="smaller"></h4></div>';
    html = html+ '                  <div class="widget-body">';
    html = html+ '                      <div class="widget-main">';
    html = html+ '                          <label class="control-label bolder blue">时间</label>&nbsp;&nbsp;';
    html = html+ '                          <span class="input-group-addon sr-only"><i class="fa fa-calendar"></i></span>';
    html = html+ '                          <input class="date-range-picker" type="text" placeholder="请选择对账日期" name="startAndEndTime_export" style="height: 32px;width:280px;"/>';
    html = html+ '                          <div class="control-group">&nbsp;&nbsp;&nbsp;</div>';
    html = html+ '                          <div style="text-align:center;">';
    html = html+ '                              <a class="btn btn-sm btn-success" id="checkButton">';
    html = html+ '                                  <i class="fa fa-check" aria-hidden="true"></i><span>确定</span>';
    html = html+ '                              </a>';
    html = html+ '                          </div>';
    html = html+ '                      </div>';
    html = html+ '                  </div>';
    html = html+ '              </div>';
    html = html+ '         </div>';
    html = html+ '      </div>';
    html = html+ '   </div>';
    html = html+ '</div>';
    var $div=$(html).clone().appendTo($("body"));
    $div.find("#accountId").val(accountId);
    var $timer = $div.find("[name=startAndEndTime_export]");
    $timer.daterangepicker({
        cancel: 'cancel.daterangepicker',
        apply: 'apply.daterangepicker',
        singleDatePicker: true,timePicker: false,
        locale: {
            "format": "YYYY-MM-DD", "separator": " ~ ",
            "applyLabel": "确定", "cancelLabel": "取消", "fromLabel": "从", "toLabel": "到",
            "customRangeLabel": "自定义", "dayNames": ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
            "daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
            "monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
            "firstDay": 1
        }
    });
    $div.find("#checkButton").click(function(){
        $.ajax({type: 'get',url: '/r/cabana/getCacheFlow',data: {"accId":accId,date:$timer.val().split(' ')[0]},dataType: 'json',success: function (res) {
            if (res.status==1) {
                showMessageForSuccess('操作成功');
                $div.modal("toggle");
            }else{
                showMessageForFail(res.message);
            }
        }});
    });
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () { $div.remove(); });
};

var showReAck = function(accId){
    bootbox.confirm("确定进行回收任务&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/cabana/reAck',data: {"accId":accId},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                }
            }});
        }
    });
};

var showLogs = function(accId){
    bootbox.confirm("确定同步该账号的日志&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/cabana/logs',data: {"accId":accId},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                }
            }});
        }
    });
};

/** 已绑账号密码弹出框 */
var showUpdPWD = function (id) {
    var $div = $("#updatePWD4clone").clone().attr("id", "updatePWD");
    $div.find("#accId").val(id);
    $div.find("td").css("padding-top", "10px");
    var accInfo = getAccountInfoById(id);
    if(accInfo.flag&&accInfo.flag ==2){
        showMessageForFail('返利网账号不能修改密码信息')
        return;
    }
    $div.modal("toggle");
    if(accInfo){
        $div.find("[name=bankHide]").val(accInfo.alias+'-'+accInfo.bankType);
        $div.find("[name=signBank]").attr("placeholder",accInfo.sign?"********":"");//登陆账号
        $div.find("[name=hookBank]").attr("placeholder",accInfo.hook?"********":"");//登陆密码
        $div.find("[name=hubBank]").attr("placeholder",accInfo.hub?"********":"");//交易密码
    }else{
        $div.find("[name=signBank],[name=hookBank],[name=hubBank]").attr('disabled','disabled');
    }
    $div.on('hidden.bs.modal', function () { $div.remove();});
};

/** 修改已绑账号密码 */
var doUpdPWD=function(){
    var $div=$("#updatePWD");
    bootbox.confirm("确定修改密码信息?", function (result) {
        if (result) {
            var params = {
                accountId:$div.find("#accId").val(),
                sign:$.trim($div.find("[name=signBank]").val(),true),
                hook:$.trim($div.find("[name=hookBank]").val(),true),
                hub:$.trim($div.find("[name=hubBank]").val(),true)
            };
            $.ajax({type:'get',url:'/r/host/alterPWD4Trans',data: params,dataType:'json',success:function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                    $div.modal("toggle");
                } else {
                    showMessageForFail("修改失败：" + res.message);
                }
            }});
        }
    });
};

var doScreen=function(accId){
    bootbox.confirm("确定截图该信息?", function (result) {
        if (result) {
            $.ajax({type:'get',url:'/r/cabana/screen',data:{accId:accId},dataType:'json',success:function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                    $div.modal("toggle");
                } else {
                    showMessageForFail("修改失败：" + res.message);
                }
            }});
        }
    });
};

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var  totalBalanceByBank =0 ,idList=new Array,idArray = new Array();
    $.each(data,function(idx, obj) {
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
        obj.classOfStatus=(obj.status == accountStatusFreeze ||obj.status == accountStatusStopTemp)?'label-danger':(obj.status == accountStatusEnabled?'label-warning':'label-success');
        obj.DailyAmount =htmlDailyAmount(1,obj.limitOut,obj.outwardAmountDaily);
        obj.balance = obj.balance?obj.balance:'0';
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.bankName = obj.bankName?obj.bankName:'';
        obj.bankType = obj.bankType?obj.bankType:'';
        obj.bankName = obj.type==accountTypeOutThird?obj.bankName:(obj.bankType?obj.bankType:obj.bankName);
        obj.owner=obj.owner?obj.owner:'';
        obj.alias =$.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName =obj.currSysLevelName?obj.currSysLevelName:'';
        obj.handicapName =obj.handicapName?obj.handicapName:'';
        obj.isThird = obj.type==accountTypeOutThird;
        obj.hideAccount=hideAccountAll(obj.account);
        obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.alias?obj.alias:'无')+'|'+(obj.hideAccount?obj.hideAccount:'无')+'|'+(obj.owner?obj.owner:'无') ;
        obj.error = '';
        obj.peakBalance = obj.peakBalance?obj.peakBalance:'0';
        idList.push({'id':obj.id,'type':(obj.type==accountTypeOutThird||obj.type==accountTypeInThird?'third':'Bank')});
        idArray.push(obj.id);
    });
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
    if(!page){
        page = {totalElements:0,header:{totalAmountBankBalance:0}};
    }
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:data.length,count:page.totalElements,9:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance}});
    rereshAccSt(idArray);
    contentRight();
}

var rereshAccSt = function(idList){
    $.ajax({type:'get',url:'/r/cabana/status',data: {accIdArray:idList.toString()},dataType:'json',success:function (res) {
        if (res.status==1&&res.data&&res.data.length>0) {
            var nowTm = new Date().getTime();
            $('span.errorAlarm').html('');
            $('span.time4St.bal').text('离线').attr('class','time4St bal label label-grey').attr('style','display:block;width:99%;float:left;');
            $('span.time4St.flow').html('&nbsp;&nbsp;').attr('class','time4St flow label label-grey').attr('style','display:block;width:99%;float:left;');
            res.data.forEach(function(val,index) {
                if(val.error){
                    var error = '';
                    error = error + '<span class="badge badge-transparent tooltip-error" title="'+val.error+'" onclick="doError('+val.id+',\''+val.error+'\');">';
                    error = error + '  <i class="ace-icon fa fa-exclamation-triangle red bigger-130"></i>';
                    error = error + '</span>';
                    $('span.errorAlarm.error'+val.id).html(error);
                }
                {
                    var classInf = val.time ==0||(val.time > 0 && (nowTm-val.time)< 180000)?'label-success':'label-warning';
                    if(val.time && val.time > 0 &&((nowTm-val.time)>= 600000)){
                        classInf = 'label-danger';
                    }
                    $('span.time4St.bal[target='+val.id+']').each(function(){
                        $(this).text(val.time >0?('余额：'+geeTime4Crawl(val.time)):'已连接').attr('class','time4St bal label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
                    });
                }
                {
                    var classInf = val.logtime ==0||(val.logtime > 0 && (nowTm-val.logtime)< 180000)?'label-success':'label-warning';
                    if(val.logtime && val.logtime > 0 &&((nowTm-val.logtime)>= 600000)){
                        classInf = 'label-danger';
                    }
                    $('span.time4St.flow[target='+val.id+']').each(function(){
                        $(this).html((val.logtime >0?('流水：'+geeTime4Crawl(val.logtime)):'&nbsp;&nbsp;')+(val.checkLogTime >0?('&nbsp;&nbsp;&nbsp;&nbsp;对账：'+geeTime4Crawl(val.checkLogTime)):'&nbsp;&nbsp;')).attr('class','time4St flow label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
                    });
                }
                $('span.mode4Acc[target='+val.id+']').text(((val.time||val.logtime)?(val.mode ==1 ?'抓流水':(val.mode ==2?'转账':'默认')):'---'));
                {
                    var flag =  val.flag;//1-simulator, 0-phone
                    if(flag && flag ==1){
                        $('button[smstarget='+val.id+']').attr('class','btn btn-xs btn-white btn-primary btn-bold orange contentRight');
                    }else{
                        $('button[smstarget='+val.id+']').attr('class','dsn contentRight');
                    }
                }
            });
        }
    }});
};

var doError = function(id,error){
    bootbox.dialog({
        message: "<span class='bigger-120'>确定该账号的告警&nbsp;&nbsp;&nbsp;&nbsp;"+error+"&nbsp;&nbsp;&nbsp;&nbsp;已处理</span>",
        buttons:{
            "click0":{"label":"确定","className":"btn btn-sm btn-primary","callback": function(){
                $.ajax({ dataType:'json',type:"get", url:'/r/cabana/error',data:{accId:id},success:function(jsonObject){
                    if(jsonObject.status == 1){
                        refreshContent();
                    }else{
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){  bootbox.alert(result); }});
            }},
            "click1":{"label":"取消","className":"btn btn-sm"}
        }
    });
};
var $dilogSMS = null;
var showSMS = function(accId,mobile,accountInfo){
    var html =   '<div id="choiceExportModal_SMS" class="modal fade " tabindex="-1">';
    html = html+ '   <div class="modal-dialog modal-lg" style="width:400px;">';
    html = html+ '      <div class="modal-content">';
    html = html+ '         <div class="modal-header no-padding text-center">';
    html = html+ '            <div class="table-header">';
    html = html+ '               <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>'+accountInfo +"|"+mobile+'</span>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '         <div class="modal-body">';
    html = html+ '            <div class="widget-box">';
    html = html+ '               <div class="widget-body">';
    html = html+ '                  <div class="widget-main">';
    html = html+ '                      <div class="smsContent"></div>';
    html = html+ '                      <div style="text-align:center;">';
    html = html+ '                          <a class="btn btn-xs btn-bold btn-success" onclick="showSMSContent('+mobile+')"><i class="fa fa-refresh" aria-hidden="true"></i><span>刷新</span></a>';
    html = html+ '                      </div>';
    html = html+ '                  </div>';
    html = html+ '               </div>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '      </div>';
    html = html+ '   </div>';
    html = html+ '</div>';
    $dilogSMS=$(html).clone().appendTo($("body"));
    $dilogSMS.modal("toggle");
    var $sms = $dilogSMS.find("div.smsContent").html('正在加载,请等待...');
    $.ajax({type: 'get',url: '/r/cabana/hisSMS',data: {"mobile":mobile},dataType:'json',success: function (res) {
        if (res.status==1) {
            if(!res.data||res.data.length==0){
                $sms.html('暂时无新的短信，请等待...');
            }else{
                var html = '';
                for(var index in res.data){
                    html = html +res.data[index] +'</br>';
                }
                $sms.html(html);
            }
        }else{
            $sms.html('加载失败...');
        }
    }});
    $dilogSMS.on('hidden.bs.modal', function () { $dilogSMS.remove();$dilogSMS = null;});
};

var showSMSContent = function(mobile){
    $.ajax({type: 'get',url: '/r/cabana/hisSMS',data: {"mobile":mobile},dataType:'json',success: function (res) {
        if(!$dilogSMS)
            return;
        var $sms = $dilogSMS.find("div.smsContent");
        if (res.status==1) {
            if(!res.data||res.data.length==0){
                $sms.html('暂时无新的短信，请等待...');
            }else{
                var html = '';
                for(var index in res.data){
                    html = html +res.data[index] +'</br>';
                }
                $sms.html(html);
            }
        }else{
            $sms.html('加载失败...');
        }
    }});
};

var showLogDate = function(accId){
    var html =   '<div id="choiceLogDateModal_InOut" class="modal fade " tabindex="-1">';
    html = html+ '   <input type="hidden" id="accountId"/>';
    html = html+ '   <input type="hidden" id="operaType"/>';
    html = html+ '   <input type="hidden" id="exBankType"/>';
    html = html+ '   <div class="modal-dialog modal-lg" style="width:400px;">';
    html = html+ '      <div class="modal-content">';
    html = html+ '         <div class="modal-header no-padding text-center">';
    html = html+ '            <div class="table-header">';
    html = html+ '               <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>请选择日志文件日期</span>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '         <div class="modal-body">';
    html = html+ '            <div class="widget-box">';
    html = html+ '               <div class="widget-header hide"><h4 class="smaller"></h4></div>';
    html = html+ '                  <div class="widget-body">';
    html = html+ '                      <div class="widget-main">';
    html = html+ '                          <label class="control-label bolder blue">日期</label>&nbsp;&nbsp;';
    html = html+ '                          <span class="input-group-addon sr-only"><i class="fa fa-calendar"></i></span>';
    html = html+ '                          <input class="date-range-picker" type="text" placeholder="请选择日志文件日期" name="startAndEndTime_export" style="height: 32px;width:280px;"/>';
    html = html+ '                          <div class="control-group">&nbsp;&nbsp;&nbsp;</div>';
    html = html+ '                          <div style="text-align:center;">';
    html = html+ '                              <a class="btn btn-sm btn-success" id="checkButton">';
    html = html+ '                                  <i class="fa fa-check" aria-hidden="true"></i><span>下载日志</span>';
    html = html+ '                              </a>';
    html = html+ '                          </div>';
    html = html+ '                      </div>';
    html = html+ '                  </div>';
    html = html+ '              </div>';
    html = html+ '         </div>';
    html = html+ '      </div>';
    html = html+ '   </div>';
    html = html+ '</div>';
    var $div=$(html).clone().appendTo($("body"));
    $div.find("#accountId").val(accountId);
    var $timer = $div.find("[name=startAndEndTime_export]");
    $timer.daterangepicker({
        cancel: 'cancel.daterangepicker',
        apply: 'apply.daterangepicker',
        singleDatePicker: true,timePicker: false,
        locale: {
            "format": "YYYY-MM-DD", "separator": " ~ ",
            "applyLabel": "确定", "cancelLabel": "取消", "fromLabel": "从", "toLabel": "到",
            "customRangeLabel": "自定义", "dayNames": ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
            "daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
            "monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
            "firstDay": 1
        }
    });
    $div.find("#checkButton").click(function(){
        $.ajax({type: 'get',url: '/r/cabana/logs',data: {"accId":accId,date:$timer.val().split(' ')[0]},dataType: 'json',success: function (res) {
            if (res.status==1) {
                showMessageForSuccess('操作成功');
                $div.modal("toggle");
            }else{
                showMessageForFail(res.message);
            }
        }});
    });
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () { $div.remove(); });
};

function refreshContent(pageNo){
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:$("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0;
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    if(!authRequest.typeToArray){
        authRequest.typeToArray= [accountTypeInBank,accountTypeOutBank,accountTypeReserveBank,accountTypeBindAli,accountTypeBindWechat,accountTypeBindCommon,accountTypeThirdCommon,accountTypeBindCustomer].toString();
    }
    if(!authRequest.search_EQ_status){
        authRequest.statusToArray= [accountStatusNormal,accountStatusEnabled,accountStatusFreeze,accountStatusStopTemp,accountInactivated].toString();
    }else{
        authRequest.statusToArray = [authRequest.search_EQ_status].toString();
    }
    var search_IN_flag = [];
    $("input[name='search_IN_flag']:checked").each(function(){
        search_IN_flag.push($(this).val());
    });
    if(search_IN_flag&&search_IN_flag.length==0){
        search_IN_flag.push(1);
        search_IN_flag.push(2);
    }
    authRequest.search_IN_flag = search_IN_flag.join(',');
    authRequest.sortProperty='mobile';
    authRequest.sortDirection=1;
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.search_IN_handicapId = $("select[name='search_EQ_handicapId']").val().toString();
    $.ajax({ dataType:'json',type:"get", url:API.r_account_list,data:authRequest,success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data?jsonObject.data:[],jsonObject.page);
            showPading(jsonObject.page,"accountPage",refreshContent,null,false,false);
            contentRight();
        }else{
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){  bootbox.alert(result); }});
}

function addAccount(){
    bootbox.dialog({
        message: "<span class='bigger-180'>请选择新增账号类型</span>",
        buttons:{
            "click1":{"label":"出款银行卡","className":"btn btn-sm btn-primary","callback": function(){showAddOutAccount(accountTypeOutBank,accountInactivated ,function(){ refreshContent();});}},
            "click2":{"label":"出款第三方","className":"btn btn-sm btn-primary","callback":function(){ showAddOutAccount(accountTypeOutThird,accountStatusEnabled,function(){ refreshContent();},true);}},
            "click3":{"label":"备用卡","className":"btn btn-sm btn-primary","callback":function(){showAddOutAccount(accountTypeReserveBank,accountInactivated ,function(){refreshContent();});}},
            "click4":{"label":"现金卡","className":"btn btn-sm btn-primary","callback":function(){showAddOutAccount(accountTypeCashBank,accountInactivated ,function(){refreshContent();});}},
            "click5":{"label":"取消","className":"btn btn-sm"}
        }
    });
}

function searchByFilter(){
    authRequest.search_EQ_status = $("input[name='search_EQ_status']:checked").val();
    authRequest.search_LIKE_alias = $("input[name='search_LIKE_alias']").val();
    authRequest.search_LIKE_account = $("input[name='search_LIKE_account']").val();
    authRequest.search_EQ_currSysLevel = $("input[name='search_EQ_currSysLevel']:checked").val();
    authRequest.bankType=$.trim($("select[name='search_LIKE_bankType']").val());
    authRequest.search_LIKE_mobile=$.trim($("input[name='search_LIKE_mobile']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    var typeToArray = new Array();
    $("input[name='search_IN_type']:checked").each(function(){
        if(this.value){
            var tmp0 = this.value.split(",");
            for(var index1 in tmp0){
                typeToArray.push(tmp0[index1]);
            }
        }
    });
    authRequest.typeToArray= (typeToArray && typeToArray.length>0)? typeToArray.toString():null;
    refreshContent(0);
}

bootbox.setLocale("zh_CN");

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});

$("#bankType_multiselect").html(options.join('')).multiselect({
    enableFiltering: true,enableHTML: true,nonSelectedText :'----全部----',nSelectedText :'已选中',buttonClass: 'btn btn-white btn-primary',buttonWidth: '160px',
    templates: {
        button: '<button type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>',
        ul: '<ul class="multiselect-container dropdown-menu"></ul>',
        filter: '<li class="multiselect-item filter"><div class="input-group"><span class="input-group-addon"><i class="fa fa-search"></i></span><input class="form-control multiselect-search" type="text"></div></li>',
        filterClearBtn: '<span class="input-group-btn"><button class="btn btn-default btn-white btn-grey multiselect-clear-filter" type="button"><i class="fa fa-times-circle red2"></i></button></span>',
        li: '<li><a tabindex="0"><label></label></a></li>',
        divider: '<li class="multiselect-item divider"></li>',
        liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
    }
});

$("#accountFilter [name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
    searchByFilter();
});
$("#accountFilter [name=search_EQ_currSysLevel],[name=search_IN_type],[name=search_EQ_status]").click(function(){
    searchByFilter();
});
getHandicap_select($("select[name='search_EQ_handicapId']"),0,"全部");
searchByFilter();
genBankTypeHtml('tabTransing_bankType');

initRefreshSelect($("#accountFilter #refreshAccountListSelect"),$("#accountFilter #search-button"),75,"refresh_monitorMobile");