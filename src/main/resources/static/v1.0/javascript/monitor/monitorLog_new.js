//所有主机
var allHost=new Array();
//在线主机
var onlineHost=new Array();



/**
 * 查询所有主机信息
 */
var searchByFilter = function () {
    var $filter = $("#searchByFilter");
    var host = $filter.find("input[name='host']").val();
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
        data: {host: host, accountLike: accountLike, statusArray: statusArray.toString()},
        success: function (jsonObject) {
            if (jsonObject.status == 1) {
                if (!!!jsonObject.data || jsonObject.data.length == 0) {
                	showMessageForFail("没有检索到相关数据"+jsonObject.message);
                } else {
                	//主机排序
                	orderByIp(jsonObject.data);
                	showHostList();
                }
            } else {
            	showMessageForFail("查询失败"+jsonObject.message);
            }
        },
        error: function (result) {
        	howMessageForFail("查询失败"+result);
        }
    });
};

//冒泡排序
var orderByIp=function(hostList){
	for (var i = 0;i<hostList.length;i++) {
		for (var j = 0; j < hostList.length-i-1; j++) {
			//比较前后IP数字大小（最后一个号段）  冒泡正序
			var str1=hostList[j].host.split(".");
			var str2=hostList[j+1].host.split(".");
			if (str1[str1.length-1]*1>str2[str2.length-1]) {
				var temp=hostList[j];
				hostList[j]=hostList[j+1];
				hostList[j+1]=temp;
			}
		}
	}
	allHost=hostList;
}

//展示所有主机信息table列表
var showHostList=function(){
	var $tbody=$("table#hostListTable").find("tbody").html("");
	var tr="";
	for(var i=0;i<allHost.length;i++){
		var record=allHost[i];
		tr+="<tr host='"+record.host+"'>";
		//tr+="<td>" +
		//		"<span>" +
		//			"<select class='chosen-select' name='accountType' style='width:100px;'>"+
		//				getOptionList(monitor_accountType,record.type)+
		//			"</select>" +
		//		"</span>" +
		//	"</td>";
		//tr+="<td>" +
		//		"<span>" +
		//			"<select class='chosen-select' name='currSysLevel' style='width:100px;'>" +
		//				getOptionList(monitor_currSysLevel,record.currSysLevel)+
		//			"</select>" +
		//		"</span>" +
		//	"</td>";
		tr+="<td>" +
				"<a  class='bind_hover_card' data-toggle='hostInfoHover"+record.host+"' data-placement='auto right' data-trigger='hover'  href='#/MonitorLogDetail:*?host="+record.host+"'>" +
					"<span name='host' >"+record.host+"</span>" +
				"</a>" +
			"</td>";
		//tr+="<td><span class='label label-grey'		name='action' >离线</span></td>";
		tr+="<td><span name='numOfAccount' >"+record.numOfAccount+"</span></td>";
		//tr+="<td><span class='badge badge-grey' 	name='countOfAccountUnstart' >"+record.numOfAccount+"</span></td>";
		//tr+="<td><span class='badge badge-success' 	name='countOfAccountAcquisition' >0</span></td>";
		//tr+="<td><span class='badge badge-info' 	name='countOfAccountPause' >0</span></td>";
		//tr+="<td><span class='badge badge-danger' 	name='countOfAccountWarn' >0</span></td>";
		//tr+="<td><span class='badge badge-warning' 	name='countOfAccountTimeout' >0</span></td>";
		//tr+="<td><button class='btn btn-xs btn-white btn-success btn-bold'" +
		//		" onclick='updateHostType(\""+record.host+"\",\""+record.numOfAccount+"\",\""+record.type+"\",\""+record.currSysLevel+"\")'>" +
		//			"<i class='ace-icon fa fa-edit bigger-100 green'></i>" +
		//			"<span>保存</span>" +
		//		"</button>" +
		//	"</td>";
		tr+="</tr>";
	}
	$tbody.html(tr);
	//加载账号悬浮提示
	loadHover_hostInfoHover(allHost);
	//SysEvent.on(SysEvent.EVENT_MONITOR,monitorlog);
}

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
//    debugger
    //统计银行卡张数
    var sysDate=new Date();
	var $tr=$("table#hostListTable").find("tr[host='"+data.ip+"']");
    if (data.ip&&data.action != monitorHostStatusOffLine&&$tr.length==1) {
    	//抓取中
    	$tr.find("[name=action]").removeClass("label-success").removeClass("label-gery").addClass("label-success").text("在线");
        data.countOfAccountUnstart = 0;//未开始
        data.countOfAccountAcquisition = 0;//在线
        data.countOfAccountPause = 0;//暂停 
        data.countOfAccountWarn = 0;//失败
        data.countOfAccountTimeout = 0;//超时
        data.lastAccountAcquisition = null;
        if (data.data && data.data.length > 0) {
            for (var index in data.data) {
                var account = data.data[index];
                if (account.runningStatus == monitorAccountStatusUnstart) {
                    data.countOfAccountUnstart++;
                } else if (account.runningStatus == monitorAccountStatusAcquisition) {
                    data.lastAccountAcquisition = (!!!data.lastAccountAcquisition) ? account : ((account.lastTime > data.lastAccountAcquisition.lastTime) ? account : data.lastAccountAcquisition);
                    data.countOfAccountAcquisition++;
                } else if (account.runningStatus == monitorAccountStatusPause) {
                    data.countOfAccountPause++;
                } else if (account.runningStatus == monitorAccountStatusWarn){
                    data.countOfAccountWarn++;
                }
                //超时统计
                if(!account.lastTime) continue;
                if(((sysDate.getTime()-account.lastTime*1)/1000/60)>sysSetting.INCOME_LIMIT_MONITOR_TIMEOUT*1){
                    data.countOfAccountTimeout++;
                }
            }
        	$tr.find("[name=numOfAccount]").text(data.numOfAccount);
        	$tr.find("[name=countOfAccountUnstart]").text(data.countOfAccountUnstart);
        	$tr.find("[name=countOfAccountAcquisition]").text(data.countOfAccountAcquisition);
        	$tr.find("[name=countOfAccountPause]").text(data.countOfAccountPause);
        	$tr.find("[name=countOfAccountWarn]").text(data.countOfAccountWarn);
        	$tr.find("[name=countOfAccountTimeout]").text(data.countOfAccountTimeout);
        }
    }else  if (data.ip&&data.action == monitorHostStatusOffLine&&$tr.length==1) {
    	//离线
    	$tr.find("[name=action]").removeClass("label-success").removeClass("label-gery").addClass("label-gery").text("离线");
    	$tr.find("[name=numOfAccount]").text(data.numOfAccount);
    	$tr.find("[name=countOfAccountUnstart],[name=countOfAccountAcquisition],[name=countOfAccountPause],[name=countOfAccountWarn],[name=countOfAccountTimeout]").text(0);
    }
	
}

searchByFilter();