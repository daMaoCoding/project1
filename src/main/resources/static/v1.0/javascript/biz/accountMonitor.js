var mousedown4InvsgtCheckRadio = function(obj){
    var radioChecked = $(obj).prop("checked");
    $(obj).prop('checked', !radioChecked);
    return false;
};

var packSearchData4Flow=function(CurPage){
    CurPage=!!!CurPage?0:CurPage;
    var pageNo =(CurPage<=0?0:CurPage-1);
    var pageSize = $.session.get('initPageSize');
    var $outAcc = $("div[name='pageContent'] div[id='invsgtFilter']");
    var minAmount = $outAcc.find("input[name='minAmount']").val();
    var maxAmount = $outAcc.find("input[name='maxAmount']").val();
    var amtBtw = [minAmount,maxAmount].toString();
    var handicapId = $outAcc.find("select[name='search_EQ_handicapId']").val();
    var accType = $outAcc.find("select[name='accType']").val();
    if(accType && accType!= accountTypeInBank && accType!= accountTypeReserveBank && accType!= accountTypeOutBank){
        handicapId = null;
    }
    //alert(handicapId);
    var aliasLike = $outAcc.find("input[name='aliasLike']").val();
    var bankTypeLike = $outAcc.find("input[name='bankTypeLike']").val();
    var bankType = null;
    if(bankTypeLike){
        $.each(bank_name_list,function (i,obj) {
            if(obj.indexOf(bankTypeLike)>=0){
                bankType = obj;
            }
        });
    }

    var flowIn = false ,flowOut = false;
    $("input[name='searchType']:checked").each(function(){
        if($(this).val() == 'flowIn'){
            flowIn = true;
        }else if($(this).val() == 'flowOut'){
            flowOut = true;
        }
    });
    var transIn0Out1 = (flowIn && flowOut || !flowIn && !flowOut )? null:( flowIn ? 0 : 1 );

    var timeBtw = [];
    var startAndEndTime = $outAcc.find("input[name='startAndEndTime']").val();
    if(startAndEndTime){
        var startAndEnd = startAndEndTime.split(" ~ ");
        timeBtw.push($.trim(startAndEnd[0]));
        timeBtw.push($.trim(startAndEnd[1]));
    }
    var statusArray = [];
    if(doing0OrDone1){
        var bankLogStatus = $("select[name='bankLogStatus']").val();
        if(bankLogStatus||bankLogStatus==0){
            statusArray.push(bankLogStatus);
        }
        $('.category').removeClass('dsn');
        $('.placeHolderCategory').removeClass('dsn').addClass('dsn');
    }else{
        statusArray.push(bankLogStatusMatching);
        $('.category').removeClass('dsn').addClass('dsn');
        $('.placeHolderCategory').removeClass('dsn');
    }
    statusArray = statusArray.toString();
    timeBtw = timeBtw.toString();
    return {pageNo:pageNo,pageSize:pageSize,handicapId:handicapId,
        aliasLike:aliasLike,bankType:bankType,accType:accType,
        timeBtw:timeBtw,amtBtw:amtBtw,transIn0Out1:transIn0Out1,statusArray:statusArray,doing0OrDone1:doing0OrDone1};
};

var modelHtml4InvsgtAcc ='\
     <li class="col-sm-2 no-margin no-padding" style="z-index:0" targetId="{id}">\
       <a>\
         <label style="color:{statusColor};" class="accInfo">{accInfo}</label></br>\
         <label style="color:{statusColor};" class="accCrawl">{accCrawlTime}</label></br>\
         <label style="color:{statusColor};" class="accCrawl">{ip}</label></br>\
        </a>\
    </li>';

var showInvsgt = function(pageNo){
    pageNo = pageNo|| $("#invsgtAccPage").find(".Current_Page").text();
    var jsonObject = SysEvent.invsgt(pageNo,6);
    $.each(jsonObject.data,function(i,obj){
        obj.accInfo =(obj.alias?obj.alias:'')+'|'+ (obj.bank?obj.bank:'');
        obj.accCrawlTime = obj.lastTime? timeStamp2yyyyMMddHHmmss(obj.lastTime):'&nbsp;&nbsp;';
        obj.ip = obj.ip?obj.ip:'&nbsp;&nbsp;';
    });
    $(".invsgtAcc ul.invsgtAccUl").html(fillDataToModel4Array(jsonObject.data,modelHtml4InvsgtAcc));
    showPading(jsonObject.page,'invsgtAccPage',showInvsgt,"监控账号 （橙色：表示在线，且15分钟内没抓流水，应尽快抓取流水；灰色：表示没有登陆）",true);
};

var doBankFlow = function(flowId,fromAccountType,oriAccInfo,destAccInfo,tradeTime,crawlTime,amount,sum,destAcc,destOwner,fromAccountHide,fromOwner,falg){
    goFromMakeUp(fromAccountType,amount);
    var $div = $("#invsgtModal");
    $div.find("span.oriAccInfo").text(fromOwner+'|'+fromAccountHide);
    $div.find("span.oriAccInfoTitle").text(oriAccInfo+'|'+fromAccountHide);
    $div.find("span.destAccInfo").text(destAccInfo);
    $div.find("span.tradeTime").text(tradeTime);
    $div.find("span.crawlTime").text(crawlTime);
    $div.find("span.amount").text(amount);
    $div.find("span.descAcc").text(destAcc);
    $div.find("span.destOwner").text(destOwner);
    $div.find("span.sum").text(sum).attr('title',sum);
    $div.find("input[name='flowId']").val(flowId);
    $div.find("input[name='remark']").val('');
    $div.find("tr.fromFlow").remove();
    var $body = $div.find("#table4RecList tbody");
    $body.html('');
    $.ajax({dataType:'json',type:"POST",url:API.r_accountMonitor_findRecList4OutAcc,data:{flowId:flowId},success:function(jsonObject) {
        if (jsonObject.status != 1) {
            showMessageForFail("查询失败：" + jsonObject.message);
            return;
        }
        var html = '<tr><td style="width:50px;">&nbsp;记&nbsp;<br/>&nbsp;录&nbsp;</td><td class="col-sm-2">监控账号</td><td class="col-sm-1">金额</td><td class="col-sm-2">交易时间</td><td class="col-sm-2">分类-盘口-会员-订单号</td><td class="col-sm-2">对方账号</td><td class="col-sm-1">对方姓名</td><td  class="col-sm-2">备注</td></tr>';
        $.each(jsonObject.data,function(i,rec){
            rec.recType = rec.req0OTask1?'出款':'下发';
            rec.fromOwner = rec.fromOwner?rec.fromOwner:'';
            rec.fromAccount = rec.fromAccount?rec.fromAccount:'';
            rec.handicapCode = rec.handicapCode?rec.handicapCode:'';
            rec.member = rec.member?rec.member:'';
            rec.orderNo = rec.orderNo?rec.orderNo:'';
            rec.fromAccInfo = (rec.fromAlias?rec.fromAlias:'')+'|'+(rec.fromCurrSysLevelName?rec.fromCurrSysLevelName:'')+'|'+(rec.fromTypeName?rec.fromTypeName:'');
            rec.toAccInfo = (rec.toAlias?rec.toAlias:'')+'|'+(rec.toOwner?rec.toOwner:'')+'|'+(rec.toBankType?rec.toBankType:'')+'|...'+rec.toAccount.slice(-4);
            rec.capture = rec.capture?rec.capture:'';
            rec.transTimeStr=rec.transTime? timeStamp2yyyyMMddHHmmss(rec.transTime):'';
            rec.remark = rec.remark?rec.remark:'';
            rec.remarkShort = rec.remark?('...'+rec.remark.slice(-10)):rec.remark;
            html =html+'<tr>\
                    <td style="width:30px;"><input type="radio" name="sysRecId" value="'+rec.id+'" style="width:18px;height:18px;cursor: pointer;" onmousedown="mousedown4InvsgtCheckRadio(this)" onclick="return false;"></td>\
                    <td>'+(rec.fromOwner+'|'+hideAccount(rec.fromAccount))+'</td>\
                    <td>'+rec.amount+'</td>\
                    <td>'+rec.transTimeStr+'</td>\
                    <td>'+rec.recType+'-'+rec.handicapCode+'-'+rec.member+'-'+rec.orderNo+'</td>\
            <td>'+rec.toAccount+'</td>\
            <td>'+rec.toOwner+'</td>\
                    <td><span title="'+rec.remark+'">'+rec.remarkShort+'</span></td>\
                  </tr>';
        });
        $body.html(html);
        $div.modal("toggle");
    }});
    if(falg==2){
    	$("#derate").css('display','block'); 
    	$("#derateTip").css('display','block'); 
    }
};

var doRemarkFlow = function(flowId,fromAccountType,oriAccInfo,destAccInfo,tradeTime,crawlTime,amount,sum,destAcc,destOwner,fromAccountHide,fromOwner){
    var $div = $("#remarkFlowModal");
    $div.find("span.oriAccInfo").text(oriAccInfo);
    $div.find("span.oriAccInfoTitle").text(oriAccInfo+'|'+fromAccountHide);
    $div.find("span.destAccInfo").text(destAccInfo);
    $div.find("span.tradeTime").text(tradeTime);
    $div.find("span.crawlTime").text(crawlTime);
    $div.find("span.amount").text(amount);
    $div.find("span.sum").text(sum).attr('title',sum);
    $div.find("span.descAcc").text(destAcc);
    $div.find("span.destOwner").text(destOwner);
    $div.find("input[name='flowId']").val(flowId);
    $div.find("input[name='remark']").val('');
    $div.modal("toggle");
};

var modelHtml4AccFlow_Doing = '\
	   <tr class="accFlowTr{id}">\
		   <td><a class="bind_hover_card" data-toggle="accountInfoHover{fromAccount}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td>\
		   <td name="createTimeStr"><span style="color:#000;">{tradingTimeStr}</span></td><td><span style="color:#000;">{createTimeStr}</span>\
		   <td name="amount"><span style="color:#000;">{amount}</span></td>\
		   <td><span style="color:#000;">{balance}</a></td>\
           <td><span style="color:#000;">{toAccount}</a></td>\
           <td><span style="color:#000;">{toAccountOwner}</a></td>\
		   <td><span style="color:#000;">{summary}</span></td>\
		   <td><a width:40px; overflow:hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis; title="{remark}">{remarkShort}</a></td>\
		   <td>'+
                '<button type="button doBankFlow" class="btn btn-xs btn-white btn-bold btn-info contentRight" contentRight="BizAccountMonitor:ProccessLog:*"  onclick="doBankFlow(\'{id}\',\'{fromAccountType}\',\'{fromAccountInfo}\',\'{toAccountInfo}\',\'{tradingTimeStr}\',\'{createTimeStr}\',\'{amount}\',\'{summary}\',\'{toAccountHide}\',\'{toAccountOwner}\',\'{fromAccountHide}\',\'{fromOwner}\',\'{flag}\');"><i class="ace-icon fa fa-check bigger-100 primary"></i>处理</button>\
                 <button type="button" class="btn btn-xs btn-white btn-bold btn-primary contentRight"  contentRight="BizAccountMonitor:RemarkLog:*"  onclick="doRemarkFlow(\'{id}\',\'{fromAccountType}\',\'{fromAccountInfo}\',\'{toAccountInfo}\',\'{tradingTimeStr}\',\'{createTimeStr}\',\'{amount}\',\'{summary}\',\'{toAccountHide}\',\'{toAccountOwner}\',\'{fromAccountHide}\',\'{fromOwner}\')"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>备注</span></button>\
                 <button type="button" class="btn btn-xs btn-white btn-bold btn-primary"  onclick="showInOutListModal(\'{fromAccount}\')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>\
</td>\
</tr>';

var modelHtml4AccFlow_Done = '\
	   <tr class="accFlowTr{id}">\
		   <td><a class="bind_hover_card" data-toggle="accountInfoHover{fromAccount}" data-placement="auto right" data-trigger="hover">{fromAccountInfo}</a></td>\
		   <td name="createTimeStr"><span style="color:#000;">{tradingTimeStr}</span></td><td><span style="color:#000;">{createTimeStr}</span>\
		   <td name="amount"><span style="color:#000;">{amount}</span></td>\
		   <td><span style="color:#000;">{balance}</a></td>\
           <td><span style="color:#000;">{toAccount}</a></td>\
           <td><span style="color:#000;">{toAccountOwner}</a></td>\
		   <td><span style="color:#000;">{summary}</span></td>\
		   <td><a width:40px; overflow:hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis; title="{remark}">{remarkShort}</a></td>\
		   <td>\
		        <button type="button" class="btn btn-xs btn-white btn-bold btn-primary contentRight"   contentRight="BizAccountMonitor:RemarkLog:*"  onclick="doRemarkFlow(\'{id}\',\'{fromAccountType}\',\'{fromAccountInfo}\',\'{toAccountInfo}\',\'{tradingTimeStr}\',\'{createTimeStr}\',\'{amount}\',\'{summary}\',\'{toAccountHide}\',\'{toAccountOwner}\',\'{fromAccountHide}\',\'{fromOwner}\')"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>备注</span></button>\
		        <button type="button" class="btn btn-xs btn-white btn-bold btn-primary"  onclick="showInOutListModal(\'{fromAccount}\')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>\
		   </td>\
	   </tr>';

var doing0OrDone1 =0;

var searchByFilter = function(doingOrdone){
    if(doingOrdone && doingOrdone!='doingdone'){
        doing0OrDone1 = doingOrdone?(doingOrdone=='doing'?0:1):doing0OrDone1;
    }
    var CurPage=doingOrdone?0:($("#tabMatchingPage").find(".Current_Page").text());
    var params = packSearchData4Flow(CurPage);
    $.ajax({dataType:'json',type:"POST",async:true,url:API.r_accountMonitor_findFlowList,data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail("查询失败："+jsonObject.message);return;
        }
        var idList= [];
        $.each(jsonObject.data,function(index,obj){
            obj.amount= obj.amount?obj.amount:'0';
            obj.balance = obj.balance?obj.balance:'0';
            obj.toAccount= hideAccountAll(obj.toAccount);
            obj.toAccountHide = hideAccount(obj.toAccount);
            obj.toAccountOwner = obj.toAccountOwner?obj.toAccountOwner:'';
            obj.toAccountInfo = obj.toAccountOwner+'|'+(obj.toAccountHide?obj.toAccountHide:'');
            obj.fromAccountNO= obj.fromAccountNO?obj.fromAccountNO:'';
            obj.fromOwner = obj.fromOwner?obj.fromOwner:'';
            obj.fromAccountHide =  obj.fromAccountNO?hideAccount(obj.fromAccountNO):'';
            obj.fromAccountInfo = (obj.fromAlias?obj.fromAlias:'')+'|'+(obj.fromCurrSysLevelName?obj.fromCurrSysLevelName:'')+'|'+(obj.fromAccountTypeName?obj.fromAccountTypeName:'');
            obj.fromAccount= obj.fromAccount?obj.fromAccount:'';
            obj.tradingTimeStr=obj.tradingTimeStr? obj.tradingTimeStr:'';
            obj.remark= obj.remark?obj.remark:'';
            obj.remarkShort= obj.remark?obj.remark.substring(0,5)+'...':'';
            obj.tradingTimeStr=obj.tradingTime? timeStamp2yyyyMMddHHmmss(obj.tradingTime):'';
            obj.createTimeStr=obj.createTime? timeStamp2yyyyMMddHHmmss(obj.createTime):'';
            obj.summary= obj.summary?obj.summary:'';
            idList.push({'id':obj.fromAccount});
        });
        $("div[name='bankFlow'] table.table tbody").html(fillDataToModel4Array(jsonObject.data,doing0OrDone1?modelHtml4AccFlow_Done:modelHtml4AccFlow_Doing));
        loadHover_accountInfoHover(idList);
        showPading(jsonObject.page,'tabMatchingPage',searchByFilter,"银行流水 （未自动匹配的流水，可能是重复出款或恶意转出，请速处理）",true);
        contentRight();
    },initPage:function (his) {}});
    showInvsgt(1);
};

var doRemark4Invsgt = function(){
    var $div = $("#remarkFlowModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div input[name='remark']").val();
    if(!flowId||!remark){
        if(!remark){
            showMessageForFail("备注不能为空");
        }
        return;
    }
    var params = {flowId:flowId,remark:remark};
    $.ajax({dataType:'json',type:"POST",url:API.r_accountMonitor_remark4Flow,data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail(jsonObject.message);
            return;
        }
        searchByFilter();
        $div.modal("toggle");
    }});
} ;

var doInterest4Invsgt = function(){
    var $div = $("#invsgtModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div.foot4Actions input[name='remark']").val();
    if(!remark){
        showMessageForFail("备注不能为空");
        return;
    }
    var params = {flowId:flowId,remark:remark};
    $.ajax({dataType:'json',type:"POST",url:API.r_accountMonitor_alterFlowToInterest,data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail(jsonObject.message);
            return;
        }
        $("tr.accFlowTr"+flowId).remove();
        searchByFilter();
        $div.modal("toggle");
    }});
};

var doDeficit4Invsgt = function(){
    var $div = $("#invsgtModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div.foot4Actions input[name='remark']").val();
    if(!flowId||!remark){
        if(flowId){
            showMessageForFail("备注不能为空");
        }
        return;
    }

    var deficit = function(reasonCode){
        var params = {flowId:flowId,remark:remark,reasonCode:reasonCode};
        $.ajax({dataType:'json',type:"POST",async:true,url:API.r_accountMonitor_alterFlowToDeficit,data:params,success:function(jsonObject){
            if(jsonObject.status !=1){
                showMessageForFail("操作失败："+jsonObject.message);
                return;
            }
            $("tr.accFlowTr"+flowId).remove();
            searchByFilter();
            $div.modal("toggle");
        }});
    };

    bootbox.dialog({
            message: "<span class='bigger-80'>亏损原因： "+remark+"</span>",
            buttons:{
                "click1":{"label":"人工","className":"btn btn-sm btn-primary","callback": function(){deficit(9);}},
                "click2":{"label":"系统","className":"btn btn-sm btn-primary","callback":function(){deficit(10);}},
                "click3":{"label":"其他","className":"btn btn-sm btn-primary","callback":function(){deficit(11);}},
                "click5":{"label":"取消","className":"btn btn-sm"}
            }
    });
};

var doFee4Invsgt = function(){
    var $div = $("#invsgtModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div.foot4Actions input[name='remark']").val();
    if(!flowId||!remark){
        if(flowId){
            showMessageForFail("备注不能为空");
        }
        return;
    }
    var params = {flowId:flowId,remark:remark};
    $.ajax({dataType:'json',type:"POST",url:API.r_accountMonitor_alterFlowToFee,data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail(jsonObject.message);
            return;
        }
        $("tr.accFlowTr"+flowId).remove();
        searchByFilter();
        $div.modal("toggle");
    }});
};

var doMatch4Invsgt = function () {
    var $div = $("#invsgtModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div.foot4Actions input[name='remark']").val();
    if(!flowId||!remark){
        if(flowId){
            showMessageForFail("备注不能为空");
        }
        return;
    }
    var recId  = $div.find("tr td input[name='sysRecId']:checked").val();
    if(!recId){
        showMessageForFail("操作记录不能为空");
        return;
    }
    var params = {flowId:flowId,remark:remark,recId:recId};
    $.ajax({dataType:'json',type:"POST",async:true,url:API.r_accountMonitor_alterFlowToMatched,data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail("操作失败："+jsonObject.message);
            return;
        }
        $("tr.accFlowTr"+flowId).remove();
        searchByFilter();
        $div.modal("toggle");
    }});
};

var doRefund4Invsgt = function(){
    var $div = $("#invsgtModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div.foot4Actions input[name='remark']").val();
    if(!flowId||!remark){
        if(flowId){
            showMessageForFail("备注不能为空");
        }
        return;
    }
    var params = {flowId:flowId,remark:remark};
    $.ajax({dataType:'json',type:"POST",async:true,url:API.r_accountMonitor_alterFlowToRefunding,data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail("操作失败："+jsonObject.message);
            return;
        }
        $("tr.accFlowTr"+flowId).remove();
        searchByFilter();
        $div.modal("toggle");
    }});
};

var derate=function(){
    var $div = $("#invsgtModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div.foot4Actions input[name='remark']").val();
    if(!flowId||!remark){
        if(flowId){
            showMessageForFail("备注不能为空");
        }
        return;
    }
    var params = {flowId:flowId,remark:remark};
    $.ajax({dataType:'json',type:"POST",async:true,url:"/r/rebate/derate",data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail("操作失败："+jsonObject.message);
            return;
        }
        searchByFilter();
        $div.modal("toggle");
    }});
}

var doExtFunds4Invsgt = function(){
    var $div = $("#invsgtModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div.foot4Actions input[name='remark']").val();
    if(!flowId||!remark){
        if(flowId){
            showMessageForFail("备注不能为空");
        }
        return;
    }
    var params = {flowId:flowId,remark:remark};
    $.ajax({dataType:'json',type:"POST",url:API.r_accountMonitor_alterFlowToExtFunds,data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            showMessageForFail("操作失败："+jsonObject.message);
            return;
        }
        $("tr.accFlowTr"+flowId).remove();
        searchByFilter();
        $div.modal("toggle");
    }});
};

var doMakeUp4Invsgt = function(){
    var $div = $("#invsgtModal");
    var flowId = $div.find("input[name='flowId']").val();
    var remark = $div.find("div.foot4MakeUp input[name='remark']").val();
    if(!flowId||!remark){
        if(flowId){
            showMessageForFail("备注不能为空");
        }
        return;
    }
    var fromFlowId = $div.find("tr td input[name='fromFlowId']:checked").val();
    if(!fromFlowId){
        showMessageForFail("请选择汇款流水");
        return;
    }
    var params = {fromFlowId:fromFlowId,toFlowId:flowId,remark:remark};
    $.ajax({dataType:'json',type:"POST",url:API.r_accountMonitor_makeUpRec4Issue,data:params,success:function(jsonObject) {
        if (jsonObject.status != 1) {
            showMessageForFail(jsonObject.message);
            return false;
        }
        $("tr.accFlowTr"+flowId).remove();
        searchByFilter();
        $div.modal("toggle");
    }});
};

var goFromMakeUp = function(fromAccountType,amount){
    var $div = $("#invsgtModal");
    $div.find("div.invsgtSpilter").removeClass('dsn');
    $div.find("table.table4RecList").addClass('dsn').removeClass('dsn');
    $div.find("tr.fromFlow").remove();
    $div.find("div.foot4MakeUp").removeClass('dsn').addClass('dsn');
    $div.find("div.foot4Actions").addClass('dsn').removeClass('dsn');
    if(fromAccountType){
        if( (fromAccountType==accountTypeOutBank||fromAccountType==accountTypeReserveBank) && amount>0){
            $div.find("div.divGoToMakeUp").addClass('dsn').removeClass('dsn');
        }else{
            $div.find("div.divGoToMakeUp").removeClass('dsn').addClass('dsn');
        }
    }
};

var goToMakeUp = function(){
    var $div = $("#invsgtModal");
    $div.find("tr.fromFlow").remove();
    var flowId = $div.find("input[name='flowId']").val();
    var params = {toFlowId:flowId};
    $.ajax({dataType:'json',type:"POST",url:API.r_accountMonitor_findFrFlowList4ToFlow,data:params,success:function(jsonObject){
        if(jsonObject.status !=1){
            return;
        }
        var html='';
        $.each(jsonObject.data,function(i,obj){
            obj.summary=obj.summary?obj.summary:'';
            obj.summaryShort=obj.summary?('...'+obj.summary.slice(-10)):'';
            obj.amount= obj.amount?obj.amount:'0';
            obj.toAccount= obj.toAccount?obj.toAccount:'';
            obj.toAccountOwner = obj.toAccountOwner?obj.toAccountOwner:'';
            obj.toAccountHide = hideAccount(obj.toAccount);
            obj.toAccountInfo = obj.toAccountOwner+'|'+obj.toAccountHide;
            obj.fromAccountNO= obj.fromAccountNO?obj.fromAccountNO:'';
            obj.fromAccountInfo = (obj.fromOwner?obj.fromOwner:'')+'|'+ hideAccount(obj.fromAccountNO);
            html=html+'\
            <tr class="fromFlow">\
              <td style="width:30px;"><input type="radio" name="fromFlowId" value="'+obj.id+'" style="width:18px;height:18px;cursor: pointer;" onmousedown="mousedown4InvsgtCheckRadio(this)" onclick="return false;"></td>\
              <td class="col-sm-2">'+obj.fromAccountInfo+'</td>\
              <td class="col-sm-1">'+obj.amount+'</td>\
              <td class="col-sm-2">'+timeStamp2yyyyMMddHHmmss(obj.tradingTime)+'</td>\
              <td class="col-sm-2">'+timeStamp2yyyyMMddHHmmss(obj.createTime)+'</td>\
              <td class="col-sm-2">'+obj.toAccount+'</td>\
              <td class="col-sm-2">'+obj.toAccountOwner+'</td>\
              <td class="col-sm-2"><span title="'+obj.summary+'">'+obj.summary+'</span></td>\
             </tr>';
        });
        $div.find("table tbody.fromToFlow").append(html);
        $div.find("div.invsgtSpilter").addClass('dsn');
        $div.find("div.foot4MakeUp").addClass('dsn').removeClass('dsn');
        $div.find("div.foot4Actions").removeClass('dsn').addClass('dsn');
        $div.find("table.table4RecList").removeClass('dsn').addClass('dsn');
    }});
};

loadHandicap_Level($("select[name='search_EQ_handicapId']"),null,$("select[name='search_EQ_id']"),null);
initRefreshSelect($("#refreshInvsgtSelect"),$("#searchInvsgtBtn"),null,"refresh_accountInvsgt");
//initTimePicker(true,$('#invsgtFilter input[name="startAndEndTime"]'),7,new Date(new Date().getTime() - 24*60*60*1000*3).format("yyyy-MM-dd hh:mm:ss"));
_datePickerForAll($('#invsgtFilter input[name="startAndEndTime"]'));
searchByFilter();
