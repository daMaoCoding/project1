

var trHtml='<tr class="noLeftRightPadding">\
                   <td><span>{handicapName}</span></td>\
				   <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
                   <td class="{classOfLogTime}"><span>{logTmStr}</span></td>\
                   <td class="{classOfBalTime}"><span>{balTmStr}</span></td>\
                   <td class="{classOfTaskTime}"><span>{taskTmStr}</span></td>\
                   <td class="{classOfOffLine}"></td>\
                   <td style="color:red">{error}</td>\
                   <td>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showConciliate({id});" ><i class="ace-icon fa fa-check bigger-100 orange"></i><span>对账</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showReAck({id});" ><i class="ace-icon fa fa-reply bigger-100 orange"></i><span>任务回收</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange" ><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange" ><i class="ace-icon fa fa-list bigger-100 orange"></i><span>联系方式</span></button>'
                    +'</td>\
               </tr>';

var queryProblem4Mobile =function(pageNo){
    var pageId = 'mobilePage';
    pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#"+pageId+" .Current_Page").text()?$("#"+pageId+" .Current_Page").text()-1:0);
    pageNo = pageNo<0?0:pageNo;
    var pageSize=$.session.get('initPageSize');
    $.ajax({type:"post",url:"/r/problem/eqpInv4Mobile",data:{pageSize:pageSize,pageNo:pageNo},dataType:'json',success:function(jsonObject){
        if(jsonObject.status!=1){
            showMessageForFail(jsonObject.message);
            return;
        }
        if(jsonObject.status=1 &&(!jsonObject.data || jsonObject.data.length == 0)){
            return;
        }
        var idList=new Array();
        $.each(jsonObject.data,function(idx, obj) {
            idList.push({'id':obj.id});
            obj.logTmStr = obj.logTmOut?(geeTime4Crawl(obj.logTm)):'';
            var classInf = obj.logTmOut?'label-warning':'';
            obj.classOfLogTime = classInf;

            obj.balTmStr = obj.balTmOut?(geeTime4Crawl(obj.balTm)):'';
            var classInf_1 = obj.balTmOut?'label-warning':'';
            obj.classOfBalTime = classInf_1;

            obj.taskTmStr = obj.taskTmOut?(geeTime4Crawl(obj.taskTm)):'';
            var classInf_2 = obj.taskTmOut?'label-warning':'';
            obj.classOfTaskTime = classInf_2;

            var classInf_3 = obj.offLine?'label-grey':'';
            obj.classOfOffLine = classInf_3;

            var classInf_4 = obj.error?'label-warning':'';
            obj.classOfError = classInf_4;
            obj.hideAccount=hideAccountAll(obj.account);
            obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.alias?obj.alias:'无')+'|'+(obj.hideAccount?obj.hideAccount:'无')+'|'+(obj.owner?obj.owner:'无') ;
            obj.peakBalance = obj.peakBalance?obj.peakBalance:'0';
        });
        var $tbody = $('#mobileTab').find('tbody').html('');
        $tbody.html(fillDataToModel4Array(jsonObject.data,trHtml));
        //加载账号悬浮提示
        loadHover_accountInfoHover(idList);
        showPading(jsonObject.page,'mobilePage',queryProblem4Mobile,null,false,false);
    }});
};


var showLogin = function(accId){
    bootbox.confirm("确定重新登陆&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/cabana/login',data: {"accId":accId},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                }
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

var pctrHtml='<tr class="noLeftRightPadding">\
                   <td><span>{handicapName}</span></td>\
				   <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
                   <td class="{classOfLogTime}"><span>{logTmStr}</span></td>\
                   <td class="{classOfBalTime}"><span>{balTmStr}</span></td>\
                   <td class="{classOfTaskTime}"><span>{taskTmStr}</span></td>\
                   <td class="{classOfOffLine}"></td>\
                   <td style="color:red">{error}</td>\
                   <td>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showConciliate({id});" ><i class="ace-icon fa fa-check bigger-100 orange"></i><span>对账</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showReAck({id});" ><i class="ace-icon fa fa-reply bigger-100 orange"></i><span>任务回收</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange" ><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
    +'</td>\
    </tr>';

var queryProblem =function(pageNo){
    var pageId = 'pcPage';
    pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#"+pageId+" .Current_Page").text()?$("#"+pageId+" .Current_Page").text()-1:0);
    pageNo = pageNo<0?0:pageNo;
    var pageSize=$.session.get('initPageSize');
    $.ajax({type:"post",url:"/r/problem/eqpInv",data:{pageSize:pageSize,pageNo:pageNo},dataType:'json',success:function(jsonObject){
        if(jsonObject.status!=1){
            showMessageForFail(jsonObject.message);
            return;
        }
        if(jsonObject.status=1 &&(!jsonObject.data || jsonObject.data.length == 0)){
            return;
        }
        var idList=new Array();
        $.each(jsonObject.data,function(idx, obj) {
            idList.push({'id':obj.id});
            obj.logTmStr = obj.logTmOut?(geeTime4Crawl(obj.logTm)):'';
            var classInf = obj.logTmOut?'label-warning':'';
            obj.classOfLogTime = classInf;

            obj.balTmStr = obj.balTmOut?(geeTime4Crawl(obj.balTm)):'';
            var classInf_1 = obj.balTmOut?'label-warning':'';
            obj.classOfBalTime = classInf_1;

            obj.taskTmStr = obj.taskTmOut?(geeTime4Crawl(obj.taskTm)):'';
            var classInf_2 = obj.taskTmOut?'label-warning':'';
            obj.classOfTaskTime = classInf_2;

            var classInf_3 = obj.offLine?'label-grey':'';
            obj.classOfOffLine = classInf_3;

            var classInf_4 = obj.error?'label-warning':'';
            obj.classOfError = classInf_4;
            obj.hideAccount=hideAccountAll(obj.account);
            obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.alias?obj.alias:'无')+'|'+(obj.hideAccount?obj.hideAccount:'无')+'|'+(obj.owner?obj.owner:'无') ;
            obj.peakBalance = obj.peakBalance?obj.peakBalance:'0';
        });
        var $tbody = $('#pcTab').find('tbody').html('');
        $tbody.html(fillDataToModel4Array(jsonObject.data,pctrHtml));
        //加载账号悬浮提示
        loadHover_accountInfoHover(idList);
        showPading(jsonObject.page,'pcPage',queryProblem,null,false,false);
    }});
};
queryProblem(0);
queryProblem4Mobile(0);
