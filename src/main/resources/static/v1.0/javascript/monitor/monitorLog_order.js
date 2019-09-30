
//在线主机
var onlineHost=new Array();
//离线主机(包含所有主机  在线时新增属性 isHide=true)
var offlineHost=new Array();



/**
 * 查询所有主机信息
 */
var searchAllHost=function(){
	var result=null;
	var $filter = $("#searchByFilter");
    var accountLike = $filter.find("input[name='accountLike']").val();
    var statusArray = [];
    $filter.find("input[name='status']:checked").each(function () {
        statusArray.push(this.value);
    });
    if (statusArray.length == 0) {
        statusArray = [monitorHostStatusOffLine, monitorHostStatusNormal];
    }
    $.ajax({
        dataType: 'json',
        type: "get",
        url: API.r_host_list,
        async:false,
        data: {accountLike: accountLike, statusArray: statusArray.toString()},
        success: function (jsonObject) {
            if (jsonObject.status == 1) {
                if (!!!jsonObject.data || jsonObject.data.length == 0) {
//                	showMessageForFail("没有检索到相关数据"+jsonObject.message);
                } else {
                	result=jsonObject.data;
                }
            } else {
            	showMessageForFail("查询失败"+jsonObject.message);
            }
        },
        error: function (result) {
        	showMessageForFail("查询失败"+result);
        }
    });
	return result;
}
/**
 * 当输入了账号模糊搜索条件时 获取模糊匹配的IP集合
 */
var get_accountLike_ips=function(){
	var result=new Array();
    var accountLike = $("#searchByFilter input[name='accountLike']").val();
    if(accountLike){
		//账号模糊搜索有值时，查询拥有此账号的IP
		searchAllHost().map(function(record){
			result.push(record.host);
		});
		//已输入账号查询条件 但是IP为空时，result新增一个空元素，用来做判断
		if(result.length==0){
			result.push(" ");
		}
	}
    return result;
}
var searchByFilter = function () {
	offlineHost=orderByIp(searchAllHost());
	showOfflineList();
	SysEvent.on(SysEvent.EVENT_MONITOR,monitorlog);
};
var refreshData=function(){
	showOfflineList();
	showOnlineList();
}
//展示在线主机信息table列表
var showOnlineList=function(){
	var $tbody=$("table#hostListTable").find("tbody#online").html("");
    var $filter = $("#searchByFilter");
    var host = $filter.find("input[name='host']").val();
	var tr="";
	var accountLikeIps=get_accountLike_ips();
    var sysDate=new Date();
	for(var j=0;j<onlineHost.length;j++){
		var record=onlineHost[j];
		if(record.isHide){
			continue;
		}
		//搜索条件  ip模糊匹配
		if(host&&record.ip.indexOf(host)==-1){
			continue;
		}
		//账号输入条件生效 且当前行不在ip集合中时
		if(accountLikeIps.length>0&&$.inArray(record.ip, accountLikeIps)==-1){
			continue;
		}
		record.numOfAccount = record.data.length;//总数
		record.countOfAccountUnstart = 0;//未开始
		record.countOfAccountAcquisition = 0;//在线
		record.countOfAccountPause = 0;//暂停 
		record.countOfAccountWarn = 0;//失败
		record.countOfAccountTimeout = 0;//超时
		record.lastAccountAcquisition = null;
		var accountList=record.data;
		for(var i=0;i<accountList.length;i++){
			var account=accountList[i];
			if (account.runningStatus == monitorAccountStatusUnstart) {
				record.countOfAccountUnstart++;
            } else if (account.runningStatus == monitorAccountStatusAcquisition) {
            	record.lastAccountAcquisition = (!!!record.lastAccountAcquisition) ? account : ((account.lastTime > record.lastAccountAcquisition.lastTime) ? account : record.lastAccountAcquisition);
            	record.countOfAccountAcquisition++;
            } else if (account.runningStatus == monitorAccountStatusPause) {
            	record.countOfAccountPause++;
            } else if (account.runningStatus == monitorAccountStatusWarn){
            	record.countOfAccountWarn++;
            }
            //超时统计
            if(!account.lastTime) continue;
            if(((sysDate.getTime()-account.lastTime*1)/1000/60)>sysSetting.INCOME_LIMIT_MONITOR_TIMEOUT*1){
            	record.countOfAccountTimeout++;
            }
		}
		tr+="<tr host='"+record.ip+"'>";
		tr+="<td>" +
				"<span>" +
					"<select class='chosen-select' name='accountType' style='width:100px;'>"+
						getOptionList(monitor_accountType,record.type)+
					"</select>" +
				"</span>" +
			"</td>";
		tr+="<td>" +
				"<span>" +
					"<select class='chosen-select' name='currSysLevel' style='width:100px;'>" +
						getOptionList(monitor_currSysLevel,record.currSysLevel)+
					"</select>" +
				"</span>" +
			"</td>";
		tr+="<td>" +
				"<a  class='bind_hover_card' data-toggle='hostInfoHover"+record.ip+"' data-placement='auto right' data-trigger='hover'  href='#/MonitorLogDetail:*?host="+record.ip+"'>" +
					"<span name='host' >"+record.ip+"</span>" +
				"</a>" +
			"</td>";
		tr+="<td><span class='label label-success'		name='action' >在线</span></td>";
		tr+="<td><span name='numOfAccount' >"+record.numOfAccount+"</span></td>";
		tr+="<td><span class='badge badge-grey' 	name='countOfAccountUnstart' >"+record.countOfAccountUnstart+"</span></td>";
		tr+="<td><span class='badge badge-success' 	name='countOfAccountAcquisition' >"+record.countOfAccountAcquisition+"</span></td>";
		tr+="<td><span class='badge badge-info' 	name='countOfAccountPause' >"+record.countOfAccountPause+"</span></td>";
		tr+="<td><span class='badge badge-danger' 	name='countOfAccountWarn' >"+record.countOfAccountWarn+"</span></td>";
		tr+="<td><span class='badge badge-warning' 	name='countOfAccountTimeout' >"+record.countOfAccountTimeout+"</span></td>";
		tr+="<td><button class='btn btn-xs btn-white btn-success btn-bold'" +
				" onclick='updateHostType(\""+record.ip+"\",\""+record.numOfAccount+"\",\""+record.type+"\",\""+record.currSysLevel+"\")'>" +
					"<i class='ace-icon fa fa-edit bigger-100 green'></i>" +
					"<span>保存</span>" +
				"</button>" +
			"</td>";
		tr+="</tr>";
	}
	$tbody.html(tr);
	//加载账号悬浮提示
	loadHover_hostInfoHover(onlineHost);
}

//展示离线主机信息table列表
var showOfflineList=function(){
	var $tbody=$("table#hostListTable").find("tbody#offline").html("");
    var $filter = $("#searchByFilter");
    var host = $filter.find("input[name='host']").val();
	var tr="";
	var accountLikeIps=get_accountLike_ips();
	for(var i=0;i<offlineHost.length;i++){
		var record=offlineHost[i];
		if(record.isHide){
			continue;
		}
		//搜索条件  ip模糊匹配
		if(host&&record.host.indexOf(host)==-1){
			continue;
		}
		//账号输入条件生效 且当前行不在ip集合中时
		if(accountLikeIps.length>0&&$.inArray(record.host, accountLikeIps)==-1){
			continue;
		}
		tr+="<tr host='"+record.host+"'>";
		tr+="<td>" +
				"<span>" +
					"<select class='chosen-select' name='accountType' style='width:100px;'>"+
						getOptionList(monitor_accountType,record.type)+
					"</select>" +
				"</span>" +
			"</td>";
		tr+="<td>" +
				"<span>" +
					"<select class='chosen-select' name='currSysLevel' style='width:100px;'>" +
						getOptionList(monitor_currSysLevel,record.currSysLevel)+
					"</select>" +
				"</span>" +
			"</td>";
		tr+="<td>" +
				"<a  class='bind_hover_card' data-toggle='hostInfoHover"+record.host+"' data-placement='auto right' data-trigger='hover'  href='#/MonitorLogDetail:*?host="+record.host+"'>" +
					"<span name='host' >"+record.host+"</span>" +
				"</a>" +
			"</td>";
		tr+="<td><span class='label label-grey'		name='action' >离线</span></td>";
		tr+="<td><span name='numOfAccount' >"+record.numOfAccount+"</span></td>";
		tr+="<td><span class='badge badge-grey' 	name='countOfAccountUnstart' >"+record.numOfAccount+"</span></td>";
		tr+="<td><span class='badge badge-success' 	name='countOfAccountAcquisition' >0</span></td>";
		tr+="<td><span class='badge badge-info' 	name='countOfAccountPause' >0</span></td>";
		tr+="<td><span class='badge badge-danger' 	name='countOfAccountWarn' >0</span></td>";
		tr+="<td><span class='badge badge-warning' 	name='countOfAccountTimeout' >0</span></td>";
		tr+="<td><button class='btn btn-xs btn-white btn-success btn-bold'" +
				" onclick='updateHostType(\""+record.host+"\",\""+record.numOfAccount+"\",\""+record.type+"\",\""+record.currSysLevel+"\")'>" +
					"<i class='ace-icon fa fa-edit bigger-100 green'></i>" +
					"<span>保存</span>" +
				"</button>" +
			"</td>";
		tr+="</tr>";
	}
	$tbody.html(tr);
	//加载账号悬浮提示
	loadHover_hostInfoHover(offlineHost);
}

/**
 * 更新主机类型 内外层信息
 */
var updateHostType=function(host,numOfAccount,type,currSysLevel){
	if(!host) return;
	var $tr=$("table#hostListTable").find("tr[host='"+host+"']");
	var new_accountType=$tr.find("[name=accountType]").val();
	var new_currSysLevel=$tr.find("[name=currSysLevel]").val();
	//校验 是否变更（类型/层级） 不是更改为全部 且银行卡张数大于0 则返回
	if(numOfAccount>0){
		//类型修改为全部 无需校验
		if(new_accountType&&new_accountType!=type){
			showMessageForFail("正在更改主机类型，请先清除账号下的银行卡");
			return;
		}
		if(new_currSysLevel&&new_currSysLevel!=currSysLevel){
			showMessageForFail("正在更改内外层级，请先清除账号下的银行卡");
			return;
		}
	}
	$.ajax({ 
		dataType:'json', 
		type:"get",
		url:"/r/host/updateHostType",
		data:{
			host:host,
			accountType:new_accountType,
			currSysLevel:new_currSysLevel
		},
		success:function(jsonObject){
			if(jsonObject.status==1){
				showMessageForSuccess(host+"主机信息保存成功");
				//刷新数据列表
				searchByFilter();
			}else{
				showMessageForFail("保存失败："+jsonObject.message);
			}
		}
	});
}

var monitorlog = function(event){
    if ("unauthentication" == event.data) {
        bootbox.alert("您尚未登录（或会话已过期），请重新登录", function () {
            window.location.href = '/auth/logout';
        });
        return;
    }
    if ("unauthorized" == event.data) {
        bootbox.alert("访问被拒绝，未授权，请联系管理员");
        return;
    }
    //在线的机器
    var data = JSON.parse(event.data);
    //统计银行卡张数
    var sysDate=new Date();
	var $tr=$("table#hostListTable").find("tr[host='"+data.ip+"']");
    if (data.ip&&data.action != monitorHostStatusOffLine&&$tr.length==1) {
    	$tr.remove();
    	//离线主机数组隐藏元素  传送类型信息
    	for(var i=0;i<offlineHost.length;i++){
    		if(offlineHost[i].host==data.ip){
    			offlineHost[i].isHide=true;
    			data.type=offlineHost[i].type;
    			data.currSysLevel=offlineHost[i].currSysLevel;
    			break;
    		}
    	}
    	//在线主机数组中 不存在此IP 则新增元素
    	var index=-1;
    	for(var i=0;i<onlineHost.length;i++){
    		if(onlineHost[i].ip==data.ip){
    			index=i;
    			break;
    		}
    	}
    	if(index==-1){
        	onlineHost.push(data);
    	}
    	//主机排序
    	onlineHost=orderByIp(onlineHost);
    	showOnlineList();
    }else  if (data.ip&&data.action == monitorHostStatusOffLine&&$tr.length==1) {
    	$tr.remove();
    	//离线主机数组显示元素
    	for(var i=0;i<offlineHost.length;i++){
    		if(offlineHost[i].host==data.ip){
    			offlineHost[i].isHide=false;
    			break;
    		}
    	}
    	showOfflineList();
    	//在线主机数组删除元素
    	var index=-1;
    	for(var i=0;i<onlineHost.length;i++){
    		if(onlineHost[i].ip==data.ip){
    			index=i;
    			break;
    		}
    	}
    	if(index!=-1){
    		onlineHost.splice(index,1);
    	}
    }
	
}

//冒泡排序
var orderByIp=function(hostList){
	for (var i = 0;i<hostList.length;i++) {
		for (var j = 0; j < hostList.length-i-1; j++) {
			//比较前后IP数字大小（最后一个号段）  冒泡正序
			var str1=hostList[j].host?hostList[j].host.split("."):hostList[j].ip.split(".");
			var str2=hostList[j+1].host?hostList[j+1].host.split("."):hostList[j+1].ip.split(".");
			if (str1[str1.length-1]*1>str2[str2.length-1]) {
				var temp=hostList[j];
				hostList[j]=hostList[j+1];
				hostList[j+1]=temp;
			}
		}
	}
	return hostList;
}
searchByFilter();