currentPageLocation = window.location.href;
var request=getRequest(); //host
var hostInfo;

var monitorDetail = function(event){
    if("unauthentication" == event.data){  bootbox.alert("您尚未登录（或会话已过期），请重新登录", function () {  window.location.href = '/auth/logout'; });return; }
    if("unauthorized" == event.data){  bootbox.alert("访问被拒绝，未授权，请联系管理员"); return; }
    var data =  JSON.parse(event.data);
    data.host= data.ip;
    if(data.host != request.host){
        return;
    }
    if(data.action==monitorHostStatusOffLine){
        //离线时隐藏指定按钮
        $("tr td span.classOfStatus").removeClass("label-default").removeClass("label-success").removeClass("label-warning").addClass("label-default").find("span.statusName").text("离线");
        $("tr td button.actionActionAcquisition").addClass("hidden");
        $("tr td button.actionActionStop").addClass("hidden");
        $("tr td button.actionActionResume").addClass("hidden");
        $("tr td button.actionActionPause").addClass("hidden");
        $("tr td button.actionActionExchangeLog").addClass("hidden");
        $("tr td button.actionActionGetBankLogNow").addClass("hidden");
        $("tr td button.actionActionDoIssued").addClass("hidden");
        return;
    }else if(data.action==monitorLog_TRANSFERINFO&&data.data){
        var tbody="";
        $.each(data.data,function(index,record){
            var tr="";
            tr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.time)+"</span></td>";
            tr+="<td><span>"+(record.amount?setAmountAccuracy(record.amount):0)+"</span></td>";
            tr+="<td><span>"+(record.balance?setAmountAccuracy(record.balance):0)+"</span></td>";
            tr+="<td><span>"+(record.account?record.account:"")+"</span></td>";
            tr+="<td><span>"+(record.owner?record.owner:"")+"</span></td>";
            tr+="<td><span>"+(record.bankType?record.bankType:"")+"</span></td>";
            tr+="<td>" +
                "<a style='width:135px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+record.bankAddr+"'>"+
                (record.bankAddr?record.bankAddr:'')+
                "</a>" +
                "</td>";
            var taskIdStr="";
            if(record.taskId){
                taskIdStr="（"+record.taskId+"）";
            }
            tr+="<td>" +
                "<a style='width:135px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' title='"+(record.remark?record.remark:'')+taskIdStr+"'>"+
                (record.remark?record.remark:'')+taskIdStr+
                "</a>" +
                "</td>";
            var imgURL=window.location.origin+"/"+record.screenshot;
            tr+="<td>" +
                "<a onclick='showExchangePicModal(\""+imgURL+"\");' title='"+imgURL+"'>" +
                "<img width='70' height='20' alt='加载失败' src='"+imgURL+"'>" +
                "</a>" +
                "</td>";
            if(record.result==1){
                tr+="<td><span class='label label-sm label-success arrowed arrowed-right'>成功</span></td>";
            }else{
                tr+="<td><span class='label label-sm label-inverse arrowed arrowed-right'>失败</span></td>";
            }
            tbody+="<tr>"+tr+"</tr>";
        });
        var $div=$("#exchangeLogModal").find("tbody").html(tbody);
        return;
    }
    var dataArray = data.data,sysDate=new Date();
    if(!dataArray||dataArray.length==0){
        return;
    }
    //状态隐藏与显示
    for(var index in dataArray){
        var record=dataArray[index],curStatus=record.runningStatus;
        var $tr = $("tr[accountId='"+record.id+"']");
        //停止按钮  抓取中/暂停/失败 显示
        var classOfActionStop =$.inArray(curStatus, [monitorAccountStatusAcquisition,monitorAccountStatusPause,monitorAccountStatusWarn])>=0?'':'hidden';
        //开始按钮  未开始 显示
        var classOfActionAcquisition =$.inArray(curStatus, [monitorAccountStatusUnstart])>=0?'':'hidden';
        //恢复按钮 暂停 可以显示
        var classOfActionResume =$.inArray(curStatus, [monitorAccountStatusPause])>=0?'':'hidden';
        //暂停按钮 抓取中/失败 可以显示
        var classOfActionPause =$.inArray(curStatus, [monitorAccountStatusAcquisition,monitorAccountStatusWarn])>=0?'':'hidden';

        var statusName='未开始',classOfStatus='label-default';
        switch (curStatus){
            case monitorAccountStatusUnstart:statusName='未开始',classOfStatus='label-default';break;
            case monitorAccountStatusAcquisition:statusName='抓取中',classOfStatus='label-success';break;
            case monitorAccountStatusPause:statusName='&nbsp;&nbsp;暂停&nbsp;&nbsp;',classOfStatus='label-info';break;
            case monitorAccountStatusWarn:statusName='&nbsp;&nbsp;失败&nbsp;&nbsp;',classOfStatus='label-danger';break;
        }
        $tr.find("td span.classOfStatus").removeClass("label-default").removeClass("label-success").removeClass("label-warning").removeClass("label-danger").addClass(classOfStatus).find("span.statusName").html(statusName);
        $tr.find("td span.lastUpdateTime").html(getLastUpdateTimeHtml(record.lastTime,sysDate));
        $tr.find("td button.actionActionStop").removeClass("hidden").addClass(classOfActionStop);
        $tr.find("td button.actionActionAcquisition").removeClass("hidden").addClass(classOfActionAcquisition);
        $tr.find("td button.actionActionResume").removeClass("hidden").addClass(classOfActionResume);
        $tr.find("td button.actionActionPause").removeClass("hidden").addClass(classOfActionPause);//暂停
        $tr.find("td button.actionActionDel").removeClass("hidden");
        $tr.find("td button.actionActionExchangeLog").removeClass("hidden");
        $tr.find("td button.actionActionGetBankLogNow").removeClass("hidden");
        $tr.find("td button.actionActionDoIssued").removeClass("hidden");
    }
};

var trModal ='<tr accountId="{id}" class="accountTr">\
                <td><span>{alias}</span></td>")\
				<td>\
					<a class="bind_hover_card breakByWord" data-toggle="accountInfoHover{id}" \
						data-placement="auto right" data-trigger="hover"  >{account}\
					</a>\
				</td>\
                <td class="hidden-480">{bank}</td>\
                <td>{owner}</td>\
                <td><span class="lastUpdateTime">{lastUpdateTime}</span></td>\
				<td><span>{interval}</span></td>\
                <td class="hidden-480"><span class="label label-sm {classOfStatus} classOfStatus"><span class="statusName">{statusName}</span></span></td>\
                <td>\
                    <div class="hidden-sm hidden-xs btn-group">\
                     <button class="btn btn-xs btn-info" onclick="showInOutListModal({id})">抓取记录</button>\
					 <button class="btn btn-xs btn-info {classOfActionExchangeLog} actionActionExchangeLog" onclick="getExchangeLog({id})">转账记录</button>\
					 <button class="btn btn-xs btn-primary {classOfActionGetBankLogNow} actionActionGetBankLogNow" onclick="obtainBankFlows({id})"><i class="ace-icon fa fa-clock-o bigger-120"></i>立即抓取</button>\
                     <button class="btn btn-xs btn-primary {classOfActionAcquisition} actionActionAcquisition" onclick="startByCommand({id})"><i class="ace-icon fa fa-play bigger-120"></i>开始</button>\
                     <button class="btn btn-xs btn-danger  {classOfActionStop} actionActionStop" onclick="stopByCommand({id})"><i class="ace-icon fa fa-pause bigger-120"></i>停止</button>\
                     <button class="btn btn-xs btn-primary {classOfActionResume} actionActionResume" onclick="resumeByCommand({id})"><i class="ace-icon fa fa-mail-reply bigger-120"></i>恢复</button>\
                     <button class="btn btn-xs btn-info {classOfActionPause} actionActionPause" onclick="pauseByCommand({id})"><i class="ace-icon fa fa-stop bigger-120"></i>暂停</button>\
	 				 <button class="btn btn-xs btn-success {classOfActionDoIssued} actionActionDoIssued" onclick="doIssued({id})"><i class="ace-icon fa fa-credit-card bigger-120"></i>下发</button>\
                     <button class="btn btn-xs btn-purple " onclick="showSignAndHookModal({id})"><i class="ace-icon fa fa-pencil-square-o bigger-120"></i>修改</button>\
                     <button class="btn btn-xs btn-danger {classOfActionDel} actionActionDel" onclick="doRemoveAccountFromHost({id})"><i class="ace-icon fa fa-trash-o bigger-120"></i>删除</button>\
                    </div>\
                </td>\
            </tr>';

//第三方
var trModalThird='<tr accountId="{id}" class="accountTr">\
					<td>\
						<a class="bind_hover_card breakByWord" data-toggle="accountInfoHover{id}" \
							data-placement="auto right" data-trigger="hover"  >{account}\
						</a>\
					</td>\
				    <td class="hidden-480">{bank}</td>\
				    <td><span class="lastUpdateTime">{lastUpdateTime}</span></td>\
					<td><span>{interval}</span></td>\
				    <td class="hidden-480"><span class="label label-sm {classOfStatus} classOfStatus"><span class="statusName">{statusName}</span></span></td>\
				    <td>\
				        <div class="hidden-sm hidden-xs btn-group">\
				         <button class="btn btn-xs btn-info" onclick="show_third_InOutListModal({id})"><i class="ace-icon fa fa-list bigger-120"></i>抓取记录</button>\
				         <button class="btn btn-xs btn-primary {classOfActionAcquisition} actionActionAcquisition" onclick="startByCommand({id})"><i class="ace-icon fa fa-play bigger-120"></i>开始</button>\
				         <button class="btn btn-xs btn-danger  {classOfActionStop} actionActionStop" onclick="stopByCommand({id})"><i class="ace-icon fa fa-pause bigger-120"></i>停止</button>\
				         <button class="btn btn-xs btn-primary {classOfActionResume} actionActionResume" onclick="resumeByCommand({id})"><i class="ace-icon fa fa-mail-reply bigger-120"></i>恢复</button>\
				         <button class="btn btn-xs btn-info {classOfActionPause} actionActionPause" onclick="pauseByCommand({id})"><i class="ace-icon fa fa-stop bigger-120"></i>暂停</button>\
				         <button class="btn btn-xs btn-purple " onclick="showSignAndHookModal({id})"><i class="ace-icon fa fa-pencil-square-o bigger-120"></i>修改</button>\
				         <button class="btn btn-xs btn-danger {classOfActionDel} actionActionDel" onclick="doRemoveAccountFromHost({id})"><i class="ace-icon fa fa-trash-o bigger-120"></i>删除</button>\
				        </div>\
				    </td>\
				</tr>';

var showContent = function(dataArray){
	//是否第三方
	var isThird=$("#accountTabType").val()==2;
	var $body,idList=new Array();
	if(isThird){
		$body= $("#accountListDiv_Third").find("table tbody").html("");
	}else{
		$body= $("#accountListDiv").find("table tbody").html("");
	}
    if(!dataArray||dataArray.length==0){
        return;
    }
    var dataArray_Bank=new Array(),dataArray_Third=new Array(),sysDate=new Date();
    for(var index in dataArray){
    	var record=dataArray[index];
    	//当前行是否第三方数据
    	var recordIsThird=accountTypeInThird==record.type||accountTypeOutThird==record.type;
    	//只装取所需数据，不需要的数据无需push 与赋值
    	if(isThird&&recordIsThird){
    		idList.push({'id':record.id,'type':'third'});
    		//需要加载第三方数据，且当前行是第三方时
            record.classOfActionResume = 'hidden';
            record.classOfActionPause = 'hidden';
            record.classOfActionAcquisition ='hidden';
            record.classOfActionStop = 'hidden';
            record.classOfActionDel ='';
            record.lastUpdateTime =  record.lastTime?timeStamp2yyyyMMddHHmmss(record.lastTime):'';
            record.statusName= '未开始';
            record.classOfStatus = 'label-default';
        	record.interval=record.interval?record.interval:'未设置';
    		dataArray_Third.push(record);
    	}else if(!isThird&&!recordIsThird){
    		idList.push({'id':record.id});
    		//加载非第三方数据，且当前行不是第三方时
    		if(record.type==accountTypeOutBank){
    			//非出款卡展示下发按钮
    			record.classOfActionDoIssued = 'hidden' ;
    		}else{
    			record.classOfActionDoIssued = '' ;
    		}
            record.classOfActionResume = 'hidden';
            record.classOfActionPause = 'hidden';
            record.classOfActionAcquisition ='hidden';
            record.classOfActionStop = 'hidden';
            record.classOfActionExchangeLog = 'hidden';
            record.classOfActionGetBankLogNow = 'hidden';
            record.classOfActionDoIssued = 'hidden';
            record.classOfActionDel ='';
            record.lastUpdateTime = getLastUpdateTimeHtml(record.lastTime,sysDate);
            record.statusName= '未开始';
            record.classOfStatus = 'label-default';
        	record.interval=record.interval?record.interval+'(秒)':'未设置';
            record.alias = record.alias?record.alias:'';
    		dataArray_Bank.push(record);
    	}
    }
    if(isThird){
    	$body.html(fillDataToModel4Array(dataArray_Third,trModalThird));
    }else{
    	$body.html(fillDataToModel4Array(dataArray_Bank,trModal));
    }
	//加载账号悬浮提示
	loadHover_accountInfoHover(idList);
    SysEvent.on(SysEvent.EVENT_MONITOR_DETAIL,monitorDetail);
}

var deleteHost = function(){
    bootbox.dialog({ message: "<span class='bigger-140'>确定删除该主机："+request.host+"</span>",
        buttons:{
            "click" :{"label" : "删除","className" : "btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_delete,data:{host:request.host}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        window.history.back();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}



var accountModal ='<div id="accountModal" class="modal fade" data-backdrop="static">\
                        <input type="hidden" id="defaultAccountType" value="{defaultAccountType}"/>\
                        <div class="modal-dialog modal-lg" >\
                            <div class="modal-content">\
                                <div class="modal-header no-padding text-center"><div class="table-header"><button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>账号列表</span></div></div>\
                                <div class="modal-body no-padding">\
                                    <div id="bindAccountFilter" class="col-sm-12 row header smaller lighter blue less-margin no-margin-left" style="z-index:1">\
                                        <div class="col-sm-4"><span class="label label-lg label-danger arrowed-right">帐号</span><input type="text" style="height:32px" name="search_LIKE_account"  placeholder="帐号" /></div>\
                                        <div class="col-sm-3"><span class="label label-lg label-purple arrowed-right">开户人</span><input type="text" style="height:32px" name="search_LIKE_owner" class="input-small" placeholder="开户人" /></div>\
                                        <div class="col-sm-1"><button onclick="showAccountList(0)" type="button" class="btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa fa-search bigger-100 green"></i>查询</button></div>\
                                    </div>\
                                    <div class="tabbable">\
                                        <ul class="nav nav-tabs" id="myTab3">\
                                            <li class="incomeType active" onclick="changeAccountTabInit({accountTypeInBank})"><a data-toggle="tab" href="#tab5">入款银行卡</a></li>\
			                            	<li class="outType" onclick="changeAccountTabInit({accountTypeOutBank})"><a data-toggle="tab" href="#tab5">出款银行卡</a></li>\
				                            <li class="otherType" onclick="changeAccountTabInit({accountTypeReserveBank})"><a data-toggle="tab" href="#tab5">备用卡</a></li>\
				                            <li class="otherType" onclick="changeAccountTabInit({accountTypeCashBank})"><a data-toggle="tab" href="#tab5">现金卡</a></li>\
				                            <li class="otherType" onclick="changeAccountTabInit({accountTypeBindWechat})"><a data-toggle="tab" href="#tab5">微信专用</a></li>\
				                            <li class="otherType" onclick="changeAccountTabInit({accountTypeBindAli})"><a data-toggle="tab" href="#tab5">支付宝专用</a></li>\
				                            <li class="otherType" onclick="changeAccountTabInit({accountTypeThirdCommon})"><a data-toggle="tab" href="#tab5">第三方专用</a></li>\
				                            <li class="otherType" onclick="changeAccountTabInit({accountTypeBindCommon})"><a data-toggle="tab" href="#tab5">下发卡</a></li>\
											<li class="thirdType" onclick="changeAccountTabInit([{accountTypeInThird},{accountTypeOutThird}])"><a data-toggle="tab" href="#tab5">第三方账号</a></li>\
                                        </ul>\
                                        <div class="tab-content padding-2">\
                                            <div id="tab5" class="tab-pane in active">\
                                                <table id="accountModalTable" class="table table-striped table-bordered table-hover no-margin-bottom">\
                                                    <thead><tr><th style="width:60px;">编号</th><th style="width:150px;">账号</th><th  style="width:80px;">银行类型</th><th  style="width:190px;">开户行</th><th  style="width:85px;">开户人</th><th style="width:95px;">绑定</th></tr></thead>\
                                                    <tbody></tbody>\
                                                </table>\
                                                <div id="accountModalPage"></div>\
                                            </div>\
                                        </div>\
                                    </div>\
                                </div>\
                                <div class="col-sm-12 modal-footer no-margin center">主机：{host}</div>\
                            </div>\
                        </div>\
                     </div>';

var showAccountModal=function(){
    $("body").find("#accountModal").remove();
    var $div =$(fillDataToModel4Item({host:request.host,defaultAccountType:accountTypeInBank,accountTypeInBank:accountTypeInBank,accountTypeOutBank:accountTypeOutBank,accountTypeReserveBank:accountTypeReserveBank,accountTypeCashBank:accountTypeCashBank,
        accountTypeBindWechat:accountTypeBindWechat,accountTypeBindAli:accountTypeBindAli,accountTypeThirdCommon:accountTypeThirdCommon,accountTypeBindCommon:accountTypeBindCommon,accountTypeInThird:accountTypeInThird,accountTypeOutThird:accountTypeOutThird},accountModal)).appendTo("body");
    //只展示当前主机指定的账号类型
    do_hide_accountType();
    $div.modal("toggle");
    showAccountList(0);
}

/**
 * 只展示当前主机指定的账号类型
 */
var do_hide_accountType=function(){
	var $div=$("#myTab3");
	//有指定主机类型时
	if(hostInfo&&hostInfo.type){
		$div.find("li").hide();
		$div.find("li.thirdType").show();//所有主机都可以添加第三方
		$div.find("li.active").removeClass("active");
		if(hostInfo.type==1){
			//入款
			$div.find("li.incomeType").addClass("active").show();
			changeAccountTabInit([accountTypeInBank]);
		}else if(hostInfo.type==2){
			//出款
			$div.find("li.outType").addClass("active").show();
			changeAccountTabInit([accountTypeOutBank]);
		}else{
			//下发及其它
			$div.find("li.otherType:first").addClass("active");
			$div.find("li.otherType").show();
			changeAccountTabInit([accountTypeBindCommon]);
		}
	}
    //如果是第三方 默认打开第三方账号TAB页
    if($("#accountTabType").val()==2){
    	$div.find("li.active").removeClass("active");
    	$div.find("li.thirdType").addClass("active");
    	changeAccountTabInit([accountTypeInThird,accountTypeOutThird]);
    }
}

var changeAccountTabInit = function(accountType){
	if(accountType&&accountType.length&&accountType.length>1){
		var accountTypeStr=accountType.join(",");
		$("body").find("#accountModal").find("input[id='defaultAccountType']").val(accountTypeStr);
	}else{
	    $("body").find("#accountModal").find("input[id='defaultAccountType']").val(accountType);
	}
    showAccountList(0);
}

var showAccountList=function(CurPage){
    var $div = $('#accountModal');
    if(!!!CurPage) CurPage=$div.find("#accountModalPage .Current_Page").text();
    var defaultAccountType=$div.find("input#defaultAccountType").val();
    if(!defaultAccountType){
        return;
    }else{
    	defaultAccountType=defaultAccountType.split(",");
    }
    var search_LIKE_account = $div.find("input[name='search_LIKE_account']").val();
    search_LIKE_account = search_LIKE_account?search_LIKE_account.replace(/(^\s*)|(\s*$)/g, ""):"";
    var search_LIKE_owner = $div.find("input[name='search_LIKE_owner']").val();
    search_LIKE_owner = search_LIKE_owner?search_LIKE_owner.replace(/(^\s*)|(\s*$)/g, ""):"";
    var statusToArray=[accountStatusNormal,accountStatusStopTemp,accountStatusEnabled,accountInactivated].toString();
    var $tbody =$div.find("table[id='accountModalTable'] tbody").html(""),$thead=$div.find("table[id='accountModalTable'] thead");
    var accountType=$div.find("input[id='defaultAccountType']").val(),accountTypeArray=[],isThird=false;
    if(accountType&&accountType.length&&accountType.length>1){
    	accountTypeArray=accountType.split(",");
    	$.each(accountTypeArray,function(i,result){
    		if(accountTypeInThird==result||accountTypeOutThird==result){
    			isThird=true;
    		}
    	})
	}
    if(!isThird){
    	$thead.html('<tr><th style="width:60px;">编号</th><th style="width:150px;">账号</th><th  style="width:80px;">银行类型</th><th  style="width:190px;">开户行</th><th  style="width:85px;">开户人</th><th style="width:95px;">绑定</th></tr>');
    }else{
    	$thead.html('<tr><th style="width:150px;">第三方账号</th><th  style="width:80px;">第三方类别</th><th  style="width:190px;">第三方</th><th style="width:95px;">绑定</th></tr>');
    }
    var data={
    		"pageNo":CurPage<=0?0:CurPage-1, 
    		"search_LIKE_account":search_LIKE_account, 
    		"search_LIKE_owner":search_LIKE_owner, 
    		"type":defaultAccountType.toString(),
    		"statusToArray":statusToArray
    	};
    if(hostInfo&&hostInfo.currSysLevel){
    	data.currSysLevel=hostInfo.currSysLevel;
    }
    $.ajax({dataType:'json',type:"get",async:false, url:API.r_account_list4Host, data:data,
        success:function(jsonObject){
            if(jsonObject.status==-1){
                showMessageForFail("查询警告："+jsonObject.message);return;
            }
            $.each(jsonObject.data,function(index,record){
                var array = new Array();
                if(!isThird){
                    array.push("<td><span>"+(record.alias?record.alias:'')+"</span></td>");
                }
                array.push("<td><span>"+record.account+"</span></td>");
                array.push("<td><span>"+(record.bankType?record.bankType:'')+"</span></td>");
                array.push("<td><span>"+record.bankName+"</span></td>");
                if(!isThird){
                    array.push("<td><span>"+record.owner+"</span></td>");
                }
                array.push("<td><button type='button' class='btn btn-xs btn-white btn-warning btn-bold green' onclick='doAddAccountToHost("+record.id+","+record.signAndHook+",\""+(record.bankType?record.bankType:'')+"\")'><span>添加</span></button></td>");
                $("<tr>"+array.join('')+"</tr>").appendTo($tbody);
            });
            showPading(jsonObject.page,"accountModalPage",showAccountList);
        }
    });
}

var doAddAccountToHost = function(accountId,signAndHook,bankType){
    if(signAndHook&&bankType){
        $.ajax({ dataType:'json', type:"get",url:API.r_host_addAccountToHost,data:{host:request.host,accountId:accountId}, success:function(jsonObject){
            if(jsonObject.status == 1){
                showAccountList();
                doFillter();
            }else {
                bootbox.alert(jsonObject.message);
            }
        },error:function(result){ bootbox.alert(result);}});
    }else{
        showSignAndHookModal(accountId,'应先设置账号及密码与账号类别',true);
    }
}

var doRemoveAccountFromHost = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定从该主机上删除该账号："+((accountInfo&&accountInfo.account)?accountInfo.account:'')+"</span>",
        buttons:{
            "click0" :{"label":"删除","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_removeAccountFromHost,data:{host:request.host,accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        doFillter();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}

var startByCommand = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定该账号开始抓取："+((accountInfo&&accountInfo.account)?accountInfo.account:'')+"</span>",
        buttons:{
            "click0" :{"label":"确定","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_startByCommand,data:{accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        doFillter();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}

var stopByCommand = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定该账号停止抓取："+((accountInfo&&accountInfo.account)?accountInfo.account:'')+"</span>",
        buttons:{
            "click0" :{"label":"确定","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_stopByCommand,data:{accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        doFillter();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}

var pauseByCommand = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定该账号暂停抓取："+((accountInfo&&accountInfo.account)?accountInfo.account:'')+"</span>",
        buttons:{
            "click0" :{"label":"确定","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_pauseByCommand,data:{accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        doFillter();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}

var resumeByCommand = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定该账号恢复抓取："+((accountInfo&&accountInfo.account)?accountInfo.account:'')+"</span>",
        buttons:{
            "click0" :{"label":"确定","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_resumeByCommand,data:{accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        doFillter();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}

var doFillter = function(){
    $.ajax({ dataType:'json',async:false, type:"get",url:API.r_host_findAccountListOfHost,data:{host:request.host}, success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data);
        }else {
            bootbox.alert(jsonObject.message);
        }
        if(!jsonObject.data||jsonObject.data.length==0){
            $("div.mainfootPage").html('<div style="margin-bottom:0px;" class="alert alert-success center">无数据</div>');
        }else{
            $("div.mainfootPage").html('');
        }
    },error:function(result){ bootbox.alert(result);}});
}


var refreshThirdLogList=function(accountId,CurPage){
	var $div=$("#thirdLogList_modal");
	if(!accountId) accountId=$div.find("#accountFromId").val();
	if(!!!CurPage&&CurPage!=0) CurPage=$("#thirdLogList_Page .Current_Page").text();
	$.ajax({
		 dataType:'JSON',
		 type:"POST",
		 async:false,
		 url:"/r/thirdlog/findbyfrom", 
		 data:{
			 "pageNo":CurPage<=0?0:CurPage-1,
			 "pageSize":$.session.get('initPageSize'),
			 "fromAccount":accountId
		 },
		 success:function(jsonObject){
			if(jsonObject.status==1){
				var $tbody=$div.find("tbody"),htmlStr="";
				$.each(jsonObject.data,function(index,record){
					var tr="";
					tr+="<td><span>"+(record.balance?record.balance:0)+"</span></td>";
					tr+="<td><span>"+(record.amount?record.amount:0)+"</span></td>";
					tr+="<td><span>"+(record.fee?record.fee:0)+"</span></td>";
					tr+="<td><span>"+(record.orderNo?record.orderNo:'无')+"</span></td>";
					tr+="<td><span>"+(record.tradingTime?record.tradingTime:'无')+"</span></td>";
					tr+="<td><span>"+(record.remark?record.remark:'无')+"</span></td>";
					htmlStr+="<tr id='"+record.id+"'>"+tr+"</tr>";
				});
				$tbody.html(htmlStr);
				showPading(jsonObject.page,"thirdLogList_modal #thirdLogList_Page",refreshThirdLogList,false,true);
			}else{
				showMessageForFail("获取第三方流水信息异常，"+jsonObject.message);
			}
		 }
	});
}

var showRecentThirdLogMedal= function(accountId){
    var accountInfo =  getAccountInfoById(accountId);
    if(!accountInfo) return false;
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
    $.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/bankLogList.html", 
		success : function(html){
			var $div=$(html).find("#thirdLogList_modal").clone().appendTo($("body"));
			$div.find("[name=request_host]").text(request.host);
			$div.find("[name=accountInfo_account]").text(accountInfo.account);
			$div.find("[name=accountInfo_bankName]").text(accountInfo.bankName);
			$div.find("#accountFromId").val(accountId);
			refreshThirdLogList(accountId,0);
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
    });
}

var signAndHookModal = '<div id="signAndHookModal" class="modal fade" aria-hidden="false" data-backdrop="static">\
                            <div class="modal-dialog width-30">\
                                <div class="modal-content">\
                                    <div class="modal-header text-center no-padding"><h5>账号：{account}</h5></div>\
                                    <div class="modal-body">\
                                        <div class="row">\
                                            <div class="col-sm-13" style="">\
                                                <form role="form" class="form-horizontal">\
                                                    <div class="form-group">\
                                                       <label class="col-sm-4 control-label"><span>账号类别</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><select class="form-control" name="bankType">{options}</select></div>\
                                                    </div>\
                                                    <div class="form-group">\
                                                       <label class="col-sm-4 control-label"><span>登陆账号</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><input placeholder="请输入账号" class="form-control" name="sign"  type="text"/></div>\
                                                    </div>\
                                                    <div class="form-group">\
                                                       <label class="col-sm-4 control-label"><span>登陆密码</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><input placeholder="请输入密码" class="form-control" name="hook"  onfocus="this.type=\'password\'"  type="text"/></div>\
                                                    </div>\
													<div class="form-group">\
														<label class="col-sm-4 control-label"><span>支付密码</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><input placeholder="请输入密码" class="form-control" name="hub"  onfocus="this.type=\'password\'"  type="text"/></div>\
													</div>\
													<div class="form-group">\
														<label class="col-sm-4 control-label"><span>U盾密码</span>&nbsp;&nbsp;&nbsp;</label><div class="col-sm-7"><input placeholder="请输入密码" class="form-control" name="bing"  onfocus="this.type=\'password\'"  type="text"/></div>\
													</div>\
													<div class="form-group">\
														<label class="col-sm-4 control-label"><span>抓取间隔/秒</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><input placeholder="请输入抓取间隔" class="form-control" name="interval"  type="number"/></div>\
													</div>\
													<div class="form-group">\
														<label class="col-sm-4 control-label"><span>明文显示密码</span></label>\
														&nbsp;&nbsp;&nbsp;&nbsp;<input onchange="changePwdShowType(this);" type="checkbox" class="ace"><span class="lbl" style="padding-top:7px;"></span>\
													</div>\
                                                </form>\
                                            </div>\
                                        </div>\
                                    </div>\
                                    <div class="modal-footer">\
                                        <div class="red">{titleTip}&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</div>\
                                        <button class="btn btn-primary" id="updateIintervalBtn" onclick="updateIinterval({id});" type="button">修改抓取间隔</button>\
										<button class="btn btn-primary" onclick="signAndHook({id});" type="button">修改</button>\
                                        <button class="btn btn-default" type="button" data-dismiss="modal">取消</button>\
                                    </div>\
                                </div>\
                            </div>\
                        </div>';

var changePwdShowType=function(e){
	if($(e).prop("checked")==true){
		$("#signAndHookModal").find("input[name=hub],input[name=hook],input[name=bing]").attr("onfocus","").attr("type","text");
	}else{
		$("#signAndHookModal").find("input[name=hub],input[name=hook],input[name=bing]").attr("onfocus","this.type=\'password\'").attr("type","password");
	}
}

var showSignAndHookModal = function(accountId,titleTip,hideBtn_updateIinterval){
	//判断是否是第三方
	var isThird=false;
    var accountInfo =  getAccountInfoById(accountId);
	if(accountInfo.type==accountTypeInThird||accountInfo.type==accountTypeOutThird){
		isThird=true;
	}
    accountInfo.titleTip=titleTip?titleTip:'';
    $("body").find("#signAndHookModal").remove();
    var options= new Array();
    options.push("<option value=''>--请选择--</option>");
    var resouse_list=isThird?third_name_list:bank_name_list;
    $.each(resouse_list,function(index,record){
        if(accountInfo.bankType == record){
            options.push("<option value='"+record+"' selected>"+record+"</option>");
        }else{
            options.push("<option value='"+record+"'>"+record+"</option>");
        }
    });
    accountInfo.options=options.join('');
    var $div =$(fillDataToModel4Item(accountInfo,signAndHookModal)).appendTo("body");
    if(hideBtn_updateIinterval){
    	//添加时无修改抓取时间间隔按钮
    	$div.find('#updateIintervalBtn').hide();
    }
    //循环填充单个账号抓取时间间隔  如果没有，默认填充180s
    var messageEntity=getMessageEntity(),isUpdate=false;
    $.each(messageEntity.accounts,function(index,result){
    	if(accountId==result.id){
    		isUpdate=true;
    		$div.find("input[name=interval]").val(result.interval?result.interval:'180');
    	}
    });
    if(!isUpdate){
    	$div.find("input[name=interval]").val('180');
    }
    $div.find("form[role='form']").bootstrapValidator({
        message : 'This value is not valid',
        feedbackIcons : { valid : 'glyphicon glyphicon-ok', invalid : 'glyphicon glyphicon-remove', validating : 'glyphicon glyphicon-refresh' },
        excluded : [ ':disabled' ],
        fields : {
            bankType:{ validators : { notEmpty : {message : '请选择账号类别！'}  }  },
            sign : {
                validators : {
                    notEmpty : {message : '账号不能为空！'},
                    stringLength : {min : 4, max : 45, message : '账号长度必须在4到45之间！'}
                }
            },
            hook : {
                validators : {
                    notEmpty : {message : '登录密码不能为空！'},
                    stringLength : {min : 4, max : 45, message : '登录密码长度必须在4到45之间！'}
                }
            },
            hub : {
                validators : {
                    notEmpty : {message : '支付密码不能为空！'},
                    stringLength : {min : 4, max : 45, message : '支付密码长度必须在4到45之间！'}
                }
            },
            bing : {
                validators : {
                    stringLength : {min : 4, max : 45, message : 'U盾密码长度必须在4到45之间！'}
                }
            }            
        }
    });
    $div.modal("toggle");
}
/**
 * 修改抓取时间间隔
 */
var updateIinterval=function(accountId){
    var $div = $("body").find("#signAndHookModal");
    var interval = $div.find("input[name='interval']").val();
	if(!interval||interval<5||interval>600){
        showMessageForFail("请输入抓取时间间隔，值应该在[5-600]秒之间");return;
    }
	 $.ajax({
		 async:false,dataType:'json',type:"get",
		 url:API.r_host_updateIinterval,
		 data:{host:request.host,accountId:accountId,interval:interval},
		 success:function(jsonObject){
	         if(jsonObject.status != 1){
	             bootbox.alert(jsonObject.message);
	         }else{
	             doFillter();
	             showAccountList(0);
	         }
	         $div.modal("toggle");
	     },error:function(result){ bootbox.alert(result);}});
}
var signAndHook = function(accountId){
    var $div = $("body").find("#signAndHookModal");
    var bootstrapValidator = $div.find("form[role='form']").data('bootstrapValidator');
    bootstrapValidator.validate();
    if (bootstrapValidator.isValid()) {
        var interval = $div.find("input[name='interval']").val();
    	if(!interval||interval<5||interval>180){
            showMessageForFail("请输入抓取时间间隔，值应该在[5-180]之间");return;
        }
        var sign = $.trim($div.find("input[name='sign']").val(),true);
        var hook = $.trim($div.find("input[name='hook']").val(),true);
        var hub = $.trim($div.find("input[name='hub']").val(),true);
        var bing = $.trim($div.find("input[name='bing']").val(),true);
        var bankType = $div.find("select[name='bankType']").val();
        if(!!!sign||!!!hook||!!!bankType||!!!hub){
            showMessageForFail("账号类别、登录账号、登录密码、支付密码不可为空！");return ;
        }
        $.ajax({async:false,dataType:'json',type:"get",url:API.r_host_alterSignAndHook,data:{host:request.host,accountId:accountId,sign:sign,hook:hook,hub:hub,bing:bing,bankType:bankType,interval:interval},success:function(jsonObject){
            if(jsonObject.status != 1){
                bootbox.alert(jsonObject.message);
            }else{
                doFillter();
                showAccountList(0);
            }
            $div.modal("toggle");
        },error:function(result){ bootbox.alert(result);}});
    }
}

/**立即抓取流水事件*/
var obtainBankFlows=function(accountId) {
    //抓取当前账号的流水 accountIdWhole
    $.ajax({
        type: 'get',
        url: '/r/income/activateTools',
        data: {"accountId":accountId},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 800,
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

/**下发按钮*/
var doIssued=function(accountId) {
    //抓取当前账号的流水 accountIdWhole
    $.ajax({
        type: 'get',
        url: '/r/income/doIssued',
        data: {"accountId":accountId},
        dataType: 'json',
        success: function (jsonObject) {
            if (jsonObject&&jsonObject.status == 1) {
            	console.log("下发指令传递成功");
            }
        }
    });
}

/**转账记录*/
var getExchangeLog=function(accountId) {
    //抓取当前账号的流水 accountIdWhole
    $.ajax({
        type: 'get',
        url: '/r/income/getExchangeLog',
        data: {"accountId":accountId},
        dataType: 'json',
        success: function (jsonObject) {
            if (jsonObject&&jsonObject.status == 1) {
            	showExchangeLogModal(accountId);
            }
        }
    });
}

/**
 * 转账窗口展示
 */
var showExchangeLogModal=function(accountId){
	var $div=$("#exchangeLogModal");
	var accountInfo=getAccountInfoById(accountId);
	if(!accountInfo){
		showMessageForFail("账号信息查询失败，请稍后再试");
	}
	$div.find("[name=bankType]").text(accountInfo.bankType?accountInfo.bankType:'');
	$div.find("[name=account]").text(accountInfo.account?hideAccount(accountInfo.account):'');
	$div.find("[name=alias]").text(accountInfo.alias?accountInfo.alias:'');
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除参数信息
		$div.find("tbody").html("");
		$div.find("[name=bankType],[name=account],[name=alias]").html("");
	});
}

/**
 * 图片窗口显示
 */
var showExchangePicModal=function(URL){
	var $div=$("#exchangePicModal");
	$div.find("img").attr("src",URL);
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除参数信息
		$div.find("img").removeAttr("src");
	});
}
/**
 * 返回最后更新时间字符串
 */
var getLastUpdateTimeHtml=function(lastTime,sysDate){
	if(!lastTime) return '';
	if(!sysDate) sysDate=new Date();
	var lastUpdateTime = timeStamp2yyyyMMddHHmmss(lastTime);
    if(((sysDate.getTime()-lastTime*1)/1000/60)>sysSetting.INCOME_LIMIT_MONITOR_TIMEOUT*1){
    	lastUpdateTime='<span title="超时" style="cursor:pointer;" class="orange">'+lastUpdateTime+'&nbsp;<i class="fa fa-clock-o" aria-hidden="true" ></i></span>';
    }
    return lastUpdateTime;
}
/**
 * 获取本地host redis上存储的信息
 */
var getMessageEntity=function(){
	var messageEntity;
    $.ajax({ 
    	dataType:'JSON', 
    	type:"GET",
    	url:API.r_host_getMessageEntity,
    	data:{host:request.host},
    	async:false,
    	success:function(jsonObject){
	    	if(jsonObject.status == 1&&jsonObject.data){
	    		messageEntity=jsonObject.data;
	    	}
    	}
   });
	return messageEntity;
}
var changeTabInit=function(tabType){
	$("#accountTabType").val(tabType);
	//刷新数据列表
	doFillter();
}
/**
 * 加载页头主机信息
 */
var loadHostInfo=function(){
	  $.ajax({
	        dataType: 'json',
	        type: "get",
	        url: API.r_host_list,
	        async:false,
	        data: {host: request.host, accountLike:"" , statusArray: [1,0]},
	        success: function (jsonObject) {
	            if (jsonObject.status == 1&&jsonObject.data&&jsonObject.data[0]) {
	            	hostInfo=jsonObject.data[0];
	            	hostInfo.typeStr="全部";
	            	hostInfo.currSysLevelStr="全部";
	            	monitor_accountType.map(function(item){
	            		if(item.id==hostInfo.type){
	            			hostInfo.typeStr=item.msg;
	            		}
	            	});
	            	monitor_currSysLevel.map(function(item){
	            		if(item.id==hostInfo.currSysLevel){
	            			hostInfo.currSysLevelStr=item.msg;
	            		}
	            	});
	            	$("#hostInnfo_host").text(hostInfo.host);
	            	$("#hostInnfo_type").text(hostInfo.typeStr);
	            	$("#hostInnfo_currSysLevel").text(hostInfo.currSysLevelStr);
	            	$("#hostInnfo_numOfAccount").text(hostInfo.numOfAccount);
	            	$("#hostInfo").removeClass("hide");
	            }else{
					showMessageForFail("主机信息获取异常：");
	            }
	        }
	  });
}
doFillter();
loadHostInfo();
