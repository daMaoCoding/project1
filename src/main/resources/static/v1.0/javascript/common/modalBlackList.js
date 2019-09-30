//用户黑名单的新增、修改js，引入此js请同时引入common_getInfo.js
currentPageLocation = window.location.href;

var showBlackList_Modal=function(CurPage){
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#BlackListModal").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			//初始化盘口
			getHandicap_select($div.find("select[name='search_EQ_handicapId']"),null,"全部");
			$div.find("select[name='search_EQ_handicapId']").change(function(){
				showBlackList(0);
			})
			$div.modal("toggle");
			showBlackList(0);
			contentRight();
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}
var showBlackList=function(CurPage){
	//查询条件
	var $div = $("#BlackListModal");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#BlackListPage .Current_Page").text();
    var search_EQ_handicapId = $div.find("[name='search_EQ_handicapId']").val();
    var search_LIKE_memberCode = $div.find("[name='search_LIKE_memberCode']").val();
    var search_LIKE_name = $div.find("[name='search_LIKE_name']").val();
    var search_LIKE_account = $div.find("[name='search_LIKE_account']").val();
    var search_EQ_operator = $div.find("[name='search_EQ_operator']").val();
    var data = {
            pageNo:CurPage<=0?0:CurPage-1,
            pageSize:$.session.get('initPageSize'),
            search_EQ_handicapId:$div.find("[name='search_EQ_handicapId']").val(),
            search_LIKE_memberCode:$div.find("[name='search_LIKE_memberCode']").val(),
            search_LIKE_name:$div.find("[name='search_LIKE_name']").val(),
            search_LIKE_account:$div.find("[name='search_LIKE_account']").val(),
            search_EQ_operator:$div.find("[name='search_EQ_operator']").val()
    }
    $.ajax({
		type:"PUT",
		dataType:'JSON',
		url:'/r/blacklist/list',
		async:false,
		data:data,
		success:function(jsonObject){
	        if(jsonObject.status == 1){
	        	var tbody="";
	        	$.each(jsonObject.data,function(index,record){
					var oppHandicap=getHandicapInfoById(record.handicapId);
	        		var tr="";
					tr+="<td><span>"+_checkObj(oppHandicap.name)+"</span></td>";
					tr+="<td><span>"+_checkObj(record.memberCode)+"</span></td>";
					tr+="<td><span>"+_checkObj(record.name)+"</span></td>";
					tr+="<td><span>"+_checkObj(record.account)+"</span></td>";
					tr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</span></td>";
					tr+="<td><span>"+_checkObj(record.operator)+"</span></td>";
					tr+="<td>"+getHTMLremark(record.remark,280)+"</td>";
					tbody+="<tr id='tr"+record.id+"'>"+tr+"</tr>";
	        	});
	        	$div.find("tbody").html(tbody);
				//分页初始化
				showPading(jsonObject.page,"BlackListPage",showBlackList,null,true);
	        }else{
	        	showMessageForFail("读取失败："+jsonObject.message);
	        }
	    }
	});
}
/**
 * 新增
 */
var showAddBlackList=function(){
	//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
	$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#addBlackList").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			//初始化盘口
			getHandicap_select($div.find("select[name='handicap_select']"));
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}
var doAddBlackList=function(){
	var $div=$("#addBlackList");
	var $handicapId = $("select[name='handicap_select']");
    var $memberCode = $div.find("input[name='memberCode']");
    var $name = $div.find("input[name='name']");
    var $account = $div.find("input[name='account']");
	var $remark = $div.find("[name='remark']");
    //校验非空和输入校验
    var validateEmpty=[
    	{ele:$handicapId,name:'盘口'},
    	{ele:$memberCode,name:'会员账号'},
    	{ele:$name,name:'真实姓名'},
    	{ele:$account,name:'账号'},
    	{ele:$remark,name:'备注'}
    ];
    var validatePrint=[
    	{ele:$memberCode,name:'会员账号',minLength:2,maxLength:20},
    	{ele:$name,name:'真实姓名',minLength:2,maxLength:10},
    	{ele:$account,name:'账号',maxLength:25},
    	{ele:$remark,name:'备注',minLength:5,maxLength:300}
    ];
    if(!validateEmptyBatch(validateEmpty)||
			!validateInput(validatePrint)){
    	return;
    }
	bootbox.confirm("确定新增 ?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/blacklist/create',
				async:false,
				data:{
		    		"handicapId":$handicapId.val(),
					"memberCode":$.trim($memberCode.val(),true),
					"name":$.trim($name.val(),true),
					"account":$.trim($account.val(),true),
					"remark":$.trim($remark.val())
				},
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	//操作成功提示
			        	showMessageForSuccess("新增成功！");
			            $div.modal("toggle");
			        	//刷新数据列表
			            showBlackList();
			        }else{
			        	showMessageForFail("新增失败："+jsonObject.message);
			        }
			    }
			});
		}
	});
}