var $hosts=$("#hostList");

/** 加载数据库中所有主机 */
var loadHostALL=function(){
	$.ajax({
	    dataType:'json', 
	    type:"get",
	    url:"/r/newhost/list",
	    async:false,
	    data:{
	    	seachStr:$.trim($("[name=search_filter_all]").val())
	    },
	    success:function(jsonObject){
	    	if(jsonObject.status==1){
	    		var divs='<div class="no-margin-bottom no-margin-top alert alert-success">',lastX=false;//上一排的X坐标;
	    		var colorRow=0;//隔行换色
	    		$.each(jsonObject.data,function(index,record){
	    			if(lastX&&lastX!=record.x){
	    				//X坐标已发生变化，换行
		    			divs+='</div>';
		    			colorRow++;
		    			divs+='<div class="no-margin-bottom alert '+(colorRow%2==0?'alert-success':'alert-info')+'">';
	    			}
	    			if(!lastX||lastX!=record.x){
	    				//X坐标已发生变化，换行
		    			divs+='<button class="btn btn-app btn-purple btn-sm"><i class="ace-icon fa fa-desktop bigger-130"></i><small>'+record.x+'<br>排</small></button>';
	    			}
	    			//查询条件有输入值时 传递入下一层
	    			var filterStr=$.trim($("[name=search_filter_all]").val());
	    			divs+='<a id="'+record.id+'" href="#/HostDetail:*?id='+record.id+(filterStr?'&search_filter_all='+filterStr:'')+'" class="btn btn-app  '+(record.operator?' btn-danger ':' btn-success ')+' btn-sm" '+(record.hostInfo?('hostInfo="'+record.hostInfo+'"'):'')+' >';
	    			divs+='<i class="ace-icon fa '+(record.operator?' fa-lock ':' fa-unlock ')+' bigger-130"></i>';
	    			divs+='<small>'+record.name+'<br>'+record.y+'&nbsp;号'+(record.operator?'<br/>'+record.operator+'&nbsp;锁定':'')+'</small>';
	    			divs+='<span class="badge badge-pink">'+(record.hostNum?record.hostNum:0)+'</span>';
	    			divs+='</a>';
	    			//当前X坐标分配，勿挪动位置！
	    			lastX=record.x;
	    		});
    			divs+='</div>';
	    		$hosts.html(divs);
	    		//获取在线主机
	    		SysEvent.on(SysEvent.EVENT_MONITOR,monitorlog);
	    	}
	    	
	    }
	});
}

/** 在线主机 */
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
	
}

/** 新增主机 */
var showModal_addHost=function(){
	var $div=$("#addHost4clone").clone().attr("id","addHost").appendTo($("body"));
	$div.find("#tableAdd td").css("padding-top","10px");
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model
		$div.remove();
	});
}
var doAddHost=function(){
	var $div=$("#addHost");
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
	bootbox.confirm("确定要新增吗 ?", function(result) {
		if (result) {
			//执行新增
		    $.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/newhost/create',
				async:false,
				data:{
					"name":$.trim($name.val()),
					"ip":$.trim($ip.val(),true),//去掉中间空格
					"x":$.trim($x.val()),
					"y":$.trim($y.val()),
					"hostInfo":$.trim($hostInfo.val(),true)//去掉中间空格
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	loadHostALL();
			        	showMessageForSuccess("新增成功");
			        	$div.modal("toggle");
			        }else{
			        	showMessageForFail(jsonObject.message);
			        }
				}
		    });
		}
	});
}

/** 异步加载主机总计信息 */
var loadHostTotal=function(){
	$.ajax({
	    dataType:'json', 
	    type:"get",
	    url:"/r/newhost/loadHostTotal",
	    async:true,
	    data:{},
	    success:function(jsonObject){
	    	if(jsonObject.status==1&&jsonObject.data){
	    		$("#host_total_info").find("[name=total1]").text(jsonObject.data[0]);
	    		$("#host_total_info").find("[name=total2]").text(jsonObject.data[1]);
	    		$("#host_total_info").find("[name=total3]").text(jsonObject.data[2]);
	    		$("#host_total_info").find("[name=total4]").text(jsonObject.data[3]);
	    		$("#host_total_info").find("[name=total5]").text(jsonObject.data[4]);
	    		$("#host_total_info").find("[name=total6]").text(jsonObject.data[5]);
	    	}
	    }
	});
}

loadHostALL();
//异步加载主机统计信息
loadHostTotal();
var resetSearch=function(){
	$("[name=search_filter_all]").val("");
}
$(".page-header").keypress(function(e){
	if(event.keyCode == 13) {
		loadHostALL();
	}
});