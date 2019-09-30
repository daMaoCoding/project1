currentPageLocation = window.location.href;
var request=getRequest();
var HOST_INFO;//当前主机信息
var sysDate=new Date();

/** 加载机器信息 */
var load_host_table=function(){
	$("#hostList").html("");
	//主机
	load_bankHostInfo_table(HOST_INFO.ip,true);
	if(HOST_INFO.hostInfo){
		//虚拟机
		var hosts=HOST_INFO.hostInfo.split(",");
		$.map(hosts,function(hostIp){
			if(hostIp){
				load_bankHostInfo_table(hostIp);
			}
		});
	}
    SysEvent.on(SysEvent.EVENT_MONITOR_DETAIL,monitorDetail);
}

/** 根据搜索条件获取相关IP的匹配信息 */
var validateSearch=function(ip,curr_info,filter){
	var result={
			matchedHost:false,//当前IP是否匹配上
			hasAccount:false,//该主机是否有账号
			matchedAccountCount:0,//已匹配上的账号总数
			matchedAccounts:[]//已匹配上的账号集合
		};
	if(ip==filter){
		result.matchedHost=true;
	}
	if(curr_info&&curr_info.length>0){
		result.hasAccount=true;
		$.map(curr_info,function(account){
			var bankAccount=account.account,ismatchedAccount=false;
			//别名匹配；【账号匹配】搜索条件大于6个时，截取后六位进行匹配;【账号匹配】小于等于6个时，直接进行匹配
			if(account.alias==filter||
					(filter.length>6&&bankAccount.indexOf(filter.substr(filter.length-6,6))!=-1)||
					(filter.length<=6&&bankAccount.indexOf(filter)!=-1)){
				result.matchedAccountCount++;
				result.matchedAccounts.push(account.id);
			}
		});
	}
	return result;
}
/** table数据填充 */
var load_bankHostInfo_table=function(ip,isMainHost){
	var filter=$.trim($("[name=search_filter_all]").val());
	var validateResult={}; 
	ip=$.trim(ip,true);
	var curr_info=get_bankAccountList(ip);
	//有输入搜索条件 才进行数据筛选匹配
	if(filter){
		validateResult =validateSearch(ip,curr_info,filter);
		//1.主机未匹配上
		if(!validateResult.matchedHost){
			// 有账号 且 匹配不到任何账号
			if(validateResult.hasAccount&&validateResult.matchedAccountCount==0)return;
			//无账号
			if(!validateResult.hasAccount)return;
		}
	}
	var $div=$("#hostBankAccountTable4clone").clone().attr("id","host_div"+ip).addClass("hostBankAccountTable").show().appendTo($("#hostList"));
	$tbody=$div.find("tbody");
	//新增账号到主机 绑定事件
	$div.find("[name=btnAddAccountHost]").click(function(){
		showAccountModal(ip);
	});
	//下载日志 绑定事件
	$div.find("[name=btnUploadLog]").click(function(){
		showUploadLogDate(ip);
	});
	//补发流水  绑定事件
	$div.find("[name=btnCacheFlow]").click(function(){
		showCacheFlow(ip);
	});
	$div.find(".hostInfo_ip").html(ip);
	$div.find(".hostInfo_type").html(isMainHost?"物理机":"虚拟机");
	if(isMainHost){
		$div.find(".hostInfo_type").html("物理机");
		$div.find(".hostInfo_title").css("background-color","#82AF6F");
		$div.find(".hostInfo_title").css("border-color","#82AF6F");
	}else{
		$div.find(".hostInfo_type").html("虚拟机");
		
	}
	if(!curr_info||curr_info.length==0){
		$div.find("div.mainfootPage").html('<div style="margin-bottom:0px;" class="alert alert-success center">无数据</div>');
		return;
	}
	var trs="",idList=new Array;
	$.map(curr_info,function(record){
		//有搜索条件 且 主机未匹配上时才去校验每一行的账号【主机如果匹配上，则整个数据列表都展示】
		if(filter&&(!validateResult.matchedHost)){
			//当前账号未被匹配时 跳转到下一个循环
			if($.inArray(record.id, validateResult.matchedAccounts)==-1){
				return true;
			}
		}
		idList.push({'id':record.id});
		trs+="<tr id='accountTr"+record.id+"'>";
		trs+="<td>"+_checkObj(record.alias)+"</td>";	
		trs+="<td>" +
				"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"+record.account+
				"</a>" +
			"</td>";
		trs+="<td>"+_checkObj(record.bank)+"</td>";
		trs+="<td>"+hideName(record.owner)+"</td>";
		trs+="<td name='lastTime'>"+getLastUpdateTimeHtml(record.lastTime,sysDate)+"</td>";
    	record.interval=record.interval?record.interval+'(秒)':'未设置';
		trs+="<td>"+record.interval+"</td>";
		trs+="<td name='status"+record.id+"'><span class='label label-sm label-default'>未开始</span></td>";
		trs+="<input type='hidden' name='accountModeType'>";
		trs+="<td><span name='mode'></span></td>";
		//工具按钮
		trs+="<td>";
		trs+="<button class='hide btn btn-xs btn-primary actionActionGetBankLogNow' onclick='obtainBankFlows("+record.id+")'>" +
					"<i class='ace-icon fa fa-clock-o bigger-120'></i>立即抓取" +
				"</button>";
		trs+="<button class='btn btn-xs btn-success actionActionAcquisition' onclick='startByCommand("+record.id+")'>" +
					"<i class='ace-icon fa fa-play bigger-120'></i>开始" +
				"</button>";
		trs+="<button class='hide btn btn-xs btn-danger actionActionStop' onclick='stopByCommand("+record.id+")'>" +
					"<i class='ace-icon fa fa-pause bigger-120'></i>停止" +
				"</button>";
		trs+="<button class='hide btn btn-xs btn-primary actionActionResume' onclick='resumeByCommand("+record.id+")'>" +
					"<i class='ace-icon fa fa-mail-reply bigger-120'></i>恢复" +
				"</button>";
		trs+="<button class='hide btn btn-xs btn-info actionActionPause' onclick='pauseByCommand("+record.id+")'>" +
					"<i class='ace-icon fa fa-stop bigger-120'></i>暂停" +
				"</button>";
		trs+="<button class='hide btn btn-xs btn-success actionActionDoIssued' onclick='doIssued("+record.id+")'>" +
					"<i class='ace-icon fa fa-credit-card bigger-120'></i>下发" +
				"</button>";
		trs+="<button class=' btn btn-xs btn-success actionActionDoChangeMode' onclick='showSetting4ChangeModel("+record.id+")'>" +
					"<i class='ace-icon fa fa-cog bigger-120'></i>模式" +
				"</button>";
		//其它按钮
		trs+="<button class='btn btn-xs btn-info' onclick='showInOutListModal("+record.id+")'>抓取记录</button>"
		trs+="<button class='btn btn-xs btn-purple' onclick='showSignAndHookModal("+record.id+",\""+ip+"\")'>" +
					"<i class='ace-icon fa fa-pencil-square-o bigger-120'></i>修改" +
				"</button>";

		trs+="<button class='btn btn-xs btn-danger  actionActionDel' onclick='doRemoveAccountFromHost("+record.id+",\""+ip+"\")'>" +
					"<i class='ace-icon fa fa-trash-o bigger-120'></i>删除" +
				"</button>";
		trs+="</td>";
		trs+="</tr>";
	});
	$tbody.html(trs);
	//加载账号悬浮提示
	loadHover_accountInfoHover(idList);
}

var monitorDetail=function(event){
	if("unauthentication" == event.data){  bootbox.alert("您尚未登录（或会话已过期），请重新登录", function () {  window.location.href = '/auth/logout'; });return; }
	if("unauthorized" == event.data){  bootbox.alert("访问被拒绝，未授权，请联系管理员"); return; }
	var data =  JSON.parse(event.data);
    //当前主机table
    $ip_table=$("#host_div");
	var $ip_table=$("[id='host_div"+data.ip+"']")
    if($ip_table.length!=1){
    	//当前主机没有该IP则不执行
        return;
    }
    if(data.action==monitorHostStatusOffLine){
    	$ip_table.find(".hostInfo_status").html("离线");
    	//离线时隐藏指定按钮
    	$ip_table.find("[name=status]").html("<span class='label label-sm label-default'>离线</span>");
    	$ip_table.find(".actionActionGetBankLogNow").addClass("hide");
//    	$ip_table.find(".actionActionAcquisition").addClass("hide");
    	$ip_table.find(".actionActionStop").addClass("hide");
    	$ip_table.find(".actionActionResume").addClass("hide");
    	$ip_table.find(".actionActionPause").addClass("hide");
    	//$ip_table.find(".actionActionDoChangeMode").addClass("hide");
//    	$ip_table.find(".actionActionDoIssued").addClass("hide");
        return;
    }else if(data.action==monitorLog_TRANSFERINFO&&data.data){
    	//循环账号
    	
    }else{
    	//在线时
    	$ip_table.find(".hostInfo_status").html("在线");
    	sysDate=new Date();
	    $.map(data.data,function(record){
	    	var $tr=$ip_table.find("#accountTr"+record.id),curStatus=record.runningStatus;
	    	$tr.find("[name=accountModeType]").val(record.runningMode?record.runningMode:"");
	    	if(record.runningMode==monitorAccountStatusNORMALMODE){
	    		$tr.find("[name=mode]").html("<span class='label label-sm label-success'>正常</span>");
	    	}else if(record.runningMode==monitorAccountStatusCAPTUREMODE){
	    		$tr.find("[name=mode]").html("<span class='label label-sm label-primary'>抓流水</span>");
	    	}else if(record.runningMode==monitorAccountStatusTRANSMODE){
	    		$tr.find("[name=mode]").html("<span class='label label-sm label-purple'>转账</span>");
	    	}
	    	//最后抓取时间
	        $tr.find("[name=lastTime]").html(getLastUpdateTimeHtml(record.lastTime,sysDate));
	    	//状态
	        var statusName='未开始',classOfStatus='label-default';
	        switch (curStatus){
	            case monitorAccountStatusUnstart:statusName='未开始',classOfStatus='label-default';break;
	            case monitorAccountStatusAcquisition:statusName='抓取中',classOfStatus='label-success';break;
	            case monitorAccountStatusPause:statusName='&nbsp;&nbsp;暂停&nbsp;&nbsp;',classOfStatus='label-info';break;
	            case monitorAccountStatusWarn:statusName='&nbsp;&nbsp;失败&nbsp;&nbsp;',classOfStatus='label-danger';break;
	        }
	        $tr.find("[name=status"+record.id+"]").html("<span class='label label-sm "+classOfStatus+"'>"+statusName+"</span>");
	    	//操作按钮
	        //立即抓取/停止按钮	抓取中/暂停/失败 显示
	        var classOfActionStop =$.inArray(curStatus, [monitorAccountStatusAcquisition,monitorAccountStatusPause,monitorAccountStatusWarn])>=0?'':'hide';
	        //开始按钮	未开始 显示
	        var classOfActionAcquisition =$.inArray(curStatus, [monitorAccountStatusUnstart])>=0?'':'hide';
	        //恢复按钮	暂停 可以显示
	        var classOfActionResume =$.inArray(curStatus, [monitorAccountStatusPause])>=0?'':'hide';
	        //暂停按钮	抓取中/失败 可以显示
	        var classOfActionPause =$.inArray(curStatus, [monitorAccountStatusAcquisition,monitorAccountStatusWarn])>=0?'':'hide';
	        $tr.find(".actionActionAcquisition").removeClass("hide").addClass(classOfActionAcquisition);//开始
	        $tr.find(".actionActionGetBankLogNow").removeClass("hide").addClass(classOfActionStop);//立即抓取
//	    	$ip_table.find(".actionActionDoIssued").removeClass("hide").addClass(classOfActionStop);//下发
	        $tr.find(".actionActionStop").removeClass("hide").addClass(classOfActionStop);//停止
	        $tr.find(".actionActionResume").removeClass("hide").addClass(classOfActionResume);//恢复
	        $tr.find(".actionActionPause").removeClass("hide").addClass(classOfActionPause);//暂停
	        //$tr.find(".actionActionDoChangeMode").removeClass("hide").addClass(classOfActionStop);//工作模式切换
	        $tr.find(".actionActionDel").removeClass("hide");//删除
	    });
    }
  
}

/** 根据IP获取已绑定的银行账号 */
var get_bankAccountList=function(ip){
	var result;
	 $.ajax({ 
		 dataType:'json',
		 async:false,
		 type:"get",
		 url:API.r_host_findAccountListOfHost,
		 data:{host:ip},
		 success:function(jsonObject){
			 if(jsonObject.status==1){
				 result=jsonObject.data;
			 }
		 }
	 });
	 return result;
}

/** 从主机移除账号 */
var doRemoveAccountFromHost = function(accountId,host){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定从该主机上删除该账号："+((accountInfo&&accountInfo.account)?hideAccountAll(accountInfo.account):'')+"【在用卡不可删除！】</span>",
        buttons:{
            "click0" :{"label":"删除","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ 
                	dataType:'json', 
                	type:"get",
                	url:API.r_host_removeAccountFromHost,
                	data:{
                		host:host,
                		accountId:accountId
                	},
                	success:function(jsonObject){
	                    if(jsonObject.status == 1){
	                    	showMessageForSuccess("删除成功");
	                    	init_hostDetail();//页面初始化
	                    }else {
	                    	showMessageForFail(jsonObject.message);
	                    }
	                },
	                error:function(result){
	                	showMessageForFail(result);
	                }
	        	});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}

/** 修改主机 */
var showModal_updateHost=function(){
	var $div=$("#updateHost4clone").clone().attr("id","updateHost").appendTo($("body"));
	$div.find("#tableAdd td").css("padding-top","10px");
	$div.find("#hostId_update").val(HOST_INFO.id);
	$div.find("[name=name]").val(HOST_INFO.name);
	$div.find("[name=ip]").val(HOST_INFO.ip);
	$div.find("[name=x]").val(HOST_INFO.x);
	$div.find("[name=y]").val(HOST_INFO.y);
	$div.find("[name=hostInfo]").val(HOST_INFO.hostInfo?HOST_INFO.hostInfo:"");
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model
		$div.remove();
	});
}
var doUpdateHost=function(){
	var $div=$("#updateHost");
	var hostId_update=$div.find("#hostId_update").val();
	if(!hostId_update){
		return;
	}
	var $name=$div.find("[name=name]");
	var $ip=$div.find("[name=ip]");
	var $x=$div.find("[name=x]");
	var $y=$div.find("[name=y]");
	var $hostInfo=$div.find("[name=hostInfo]");
	//校验
	 var validate=[
    	{ele:$name,name:'主机名'},
    	{ele:$ip,name:'IP'},
    	{ele:$x,name:'排',type:'amountPlus',min:0,maxEQ:1000},
    	{ele:$y,name:'列',type:'amountPlus',min:0,maxEQ:1000},
    ];
    if(!validateEmptyBatch(validate)){
    	return;
    }
    validate.push({ele:$hostInfo,name:'虚拟机'});
    if(!validateInput(validate)){
    	return;
    }
	bootbox.confirm("确定要修改吗 ?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/newhost/update',
				async:false,
				data:{
					"id":hostId_update,
					"name":$.trim($name.val()),
					"ip":$.trim($ip.val(),true),//去掉中间空格
					"x":$.trim($x.val()),
					"y":$.trim($y.val()),
					"hostInfo":$.trim($hostInfo.val(),true)//去掉中间空格
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	init_hostDetail();
			        	showMessageForSuccess("修改成功");
			        	$div.modal("toggle");
			        }else{
			        	showMessageForFail(jsonObject.message);
			        }
				}
		    });
		}
	});
}

/** 删除主机 */
var deleteHost=function(){
	bootbox.confirm("确定要删除吗 ?", function(result) {
		if (result) {
			 $.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/newhost/delete',
				async:false,
				data:{
					"hostId":request.id,
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	showMessageForSuccess("删除成功");
			        	window.history.back();//返回上一页
			        }else{
			        	showMessageForFail(jsonObject.message);
			        }
				}
		    });
		}
	});
   
}

/** 根据ID获取主机信息 */
var getHostById=function(id){
	var result;
	$.ajax({
	    dataType:'json', 
	    type:"get",
	    url:"/r/newhost/findbyid",
	    async:false,
	    data:{
	        id:id
	    },
	    success:function(jsonObject){
	    	if(jsonObject.status==1){
	    		result=jsonObject.data;
	    	}
	    }
	});
	return result;
}

/** 锁定：1/解锁：0 */
var lockOrUnlock=function(lockStatus){
	var locakStatusStr=(lockStatus==1?"锁定":"解锁");
	bootbox.confirm("确定"+locakStatusStr+"主机 ?", function(result) {
		if (result) {
			$.ajax({
			    dataType:'json', 
			    type:"get",
			    url:"/r/newhost/lockOrUnlock",
			    async:false,
			    data:{
			    	hostId:request.id,
			    	lockStatus:lockStatus
			    },
			    success:function(jsonObject){
			    	if(jsonObject.status==1){
			    		init_hostDetail();
						showMessageForSuccess(locakStatusStr+"成功");
					}else{
			    		showMessageForFail(locakStatusStr+"失败："+jsonObject.message);
					}
			    }
			});
		}
	});
}

/** 返回最后更新时间字符串 */
var getLastUpdateTimeHtml=function(lastTime,sysDate){
	if(!lastTime) return '';
	if(!sysDate) sysDate=new Date();
	var lastUpdateTime = timeStamp2yyyyMMddHHmmss(lastTime);
    if(((sysDate.getTime()-lastTime*1)/1000/60)>sysSetting.INCOME_LIMIT_MONITOR_TIMEOUT*1){
    	lastUpdateTime='<span title="超时" style="cursor:pointer;" class="orange">'+lastUpdateTime+'&nbsp;<i class="fa fa-clock-o" aria-hidden="true" ></i></span>';
    }
    return lastUpdateTime;
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

/** 开始 */
var startByCommand = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定该账号开始抓取："+((accountInfo&&accountInfo.account)?hideAccountAll(accountInfo.account):'')+"</span>",
        buttons:{
            "click0" :{"label":"确定","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_startByCommand,data:{accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        init_hostDetail();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}
/** 停止 */
var stopByCommand = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定该账号停止抓取："+((accountInfo&&accountInfo.account)?accountInfo.account:'')+"</span>",
        buttons:{
            "click0" :{"label":"确定","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_stopByCommand,data:{accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        init_hostDetail();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}
/** 暂停 */
var pauseByCommand = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定该账号暂停抓取："+((accountInfo&&accountInfo.account)?accountInfo.account:'')+"</span>",
        buttons:{
            "click0" :{"label":"确定","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_pauseByCommand,data:{accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        init_hostDetail();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}
/** 恢复 */
var resumeByCommand = function(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.dialog({
        message: "<span class='bigger-160'>确定该账号恢复抓取："+((accountInfo&&accountInfo.account)?accountInfo.account:'')+"</span>",
        buttons:{
            "click0" :{"label":"确定","className":"btn-sm btn-primary","callback": function() {
                $.ajax({ dataType:'json', type:"get",url:API.r_host_resumeByCommand,data:{accountId:accountId}, success:function(jsonObject){
                    if(jsonObject.status == 1){
                        init_hostDetail();
                    }else {
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){ bootbox.alert(result);}});
            }},
            "click1":{"label" : "取消","className" : "btn-sm btn-default"}
        }
    });
}
/** 下发 */
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

/** 页面初始化 */
var init_hostDetail=function(){
	HOST_INFO=getHostById(request.id);
	if(!HOST_INFO){
    	showMessageForFail("主机不存在");
    	window.history.back();//返回上一页
    	return;
	}
	//主机列表页面存在搜索条件，且不是根据主机名来定位时 按搜索条件过滤 IP 账号 编号
	if(request.search_filter_all&&HOST_INFO.name.indexOf(request.search_filter_all)==-1){
		$("[name=search_filter_all]").val(request.search_filter_all);
	}
	load_host_table();
	//数据加载
	$("#hostInfo_name").text(HOST_INFO.name);
	$("#hostInfo_xy").text(HOST_INFO.x+"排"+HOST_INFO.y+"号");
	//锁定，解锁按钮的隐藏与显示
	if(HOST_INFO.operator){
		$("#btnLock").hide();
		if(getCookie('JUID')==HOST_INFO.operator){
			//锁定人是当前用户才可以显示解锁按钮
			$("#btnUnlock").show();
		}else{
			$("#btnUnlock").hide();
		}
	}else{
		$("#btnUnlock").hide();
		$("#btnLock").show();
	}
}


init_hostDetail();//页面初始化

/** 下载日志 */
var showUploadLogDate = function(ip){
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
        var data = {"virtualIp":ip,"time":$timer.val().split(' ')[0]};
        $.ajax({type: 'POST',url: '/r/tool/uploadLog',data: JSON.stringify(data),contentType:'application/json',dataType: 'json',success: function (res) {
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

/** 补发流水 */
var showCacheFlow = function(ip){
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
    html = html+ '                                  <i class="fa fa-check" aria-hidden="true"></i><span>补发流水</span>';
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
        var data = {"virtualIp":ip,"time":$timer.val().split(' ')[0]};
        $.ajax({type: 'POST',url: '/r/tool/cacheFlow',data: JSON.stringify(data),contentType:'application/json',dataType: 'json',success: function (res) {
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


var resetSearch=function(){
	$("[name=search_filter_all]").val("");
}
$("#currHost_div").keypress(function(e){
	if(event.keyCode == 13) {
		load_host_table();
	}
});
