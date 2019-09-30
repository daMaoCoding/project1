/** 用途类型管理 */

//查询
var showModal_FlagTypeSetting=function(){
	var $div=$("#FlagTypeSettingModal");
	search_FlagTypeSetting();
	$div.modal("toggle");
}
var search_FlagTypeSetting=function(){
	var $div=$("#FlagTypeSettingModal");
	$.ajax({
		type:"GET",
		dataType:'JSON',
		url:'/r/outNew/findOutUseManage',
		async:false,
		success:function(jsonObject){
	        if(jsonObject.status == 1){
	        	FlagTypeList=jsonObject.data;
	        	var tbody="";
	        	$.each(jsonObject.data,function(index,record){
	        		var tr="";
					tr+="<td><span>"+(index+1)+"</span></td>";
					tr+="<td>";
					tr+="<span id='useName_span"+record.id+"'>"+_checkObj(record.useName)+"&nbsp;<i class='red ace-icon fa fa-pencil-square-o' onclick='clickUpdateFlagType("+record.id+")' title='修改用途类型名称' style='cursor:pointer;' ></i></span>";
					tr+="<span id='useName_input"+record.id+"' style='display:none;'><input value='"+_checkObj(record.useName)+"' placeholder='"+_checkObj(record.useName)+"'  type='text' >&nbsp;<i class='green ace-icon fa fa-check-square-o' onclick='doUpdateFlagType("+record.id+")' title='保存修改' style='cursor:pointer;' ></i>";
					tr+="</td>";
					tr+="<td><span>"+_checkObj(record.handelName)+"</span></td>";
					tr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.handelTime)+"</span></td>";
					tr+="<td><button class='btn btn-xs btn-danger btn-info' " +
								"onclick='doDeleteFlagType("+record.id+")' >" +
								"<i class='ace-icon glyphicon glyphicon-trash bigger-100'></i>" +
								"<span>删除</span>" +
							"</button></td>";
					tbody+="<tr id='tr"+record.id+"'>"+tr+"</tr>";
	        	});
	        	if(tbody){
	        		$div.find("#noDataDiv").hide();
		        	$div.find("tbody").html(tbody);
	        	}else{
		        	$div.find("tbody").html(tbody);
	        		$div.find("#noDataDiv").show();
	        	}
	        }else{
	        	showMessageForFail("读取失败："+jsonObject.message);
	        }
	    }
	});
}

//新增
var doAddFlagType=function(){
	var $div=$("#FlagTypeSettingModal");
	var $useName=$div.find("input[name=useName]");
	var validatePrint=[{ele:$useName,name:'用途类型名称',minLength:2,maxLength:120}];
	 if(!validateEmptyBatch(validatePrint)||!validateInput(validatePrint)){
	        setTimeout(function(){       
	            $('body').addClass('modal-open');
	        },500);
    	return;
    }
	 $.ajax({
	        contentType: 'application/json;charset=UTF-8',
			type:"POST",
			dataType:'JSON',
			url:'/r/outNew/addOutUseManage',
			async:false,
	        data:JSON.stringify({
	    		"useName":$useName.val().trim()
			}),
			success:function(jsonObject){
		        if(jsonObject.status == 1){
		        	//操作成功提示
		        	showMessageForSuccess("新增成功！");
		        	$useName.val("");//清空
		        	search_FlagTypeSetting();//刷新数据列表
		        }else{
		        	showMessageForFail("新增失败："+jsonObject.message);
		        }
		    }
		});
}
//修改
var clickUpdateFlagType=function(id){
	$("#useName_span"+id).remove();
	$("#useName_input"+id).show();
}
var doUpdateFlagType=function(id){
	var $div=$("#FlagTypeSettingModal");
	var $useName=$div.find("#useName_input"+id+" input");
	var validatePrint=[{ele:$useName,name:'用途类型名称',minLength:2,maxLength:120}];
	 if(!validateEmptyBatch(validatePrint)||!validateInput(validatePrint)){
	        setTimeout(function(){       
	            $('body').addClass('modal-open');
	        },500);
    	return;
    }
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
		type:"POST",
		dataType:'JSON',
		url:'/r/outNew/modifyOutUseManage',
		async:false,
        data:JSON.stringify({
        	"id":id,
    		"useName":$useName.val().trim()
		}),
		success:function(jsonObject){
	        if(jsonObject.status == 1){
	        	//操作成功提示
	        	showMessageForSuccess("修改成功！");  
	        	search_FlagTypeSetting();//刷新数据列表
	        }else{
	        	showMessageForFail("修改失败："+jsonObject.message);
	        }
	    }
	});
}
//删除
var doDeleteFlagType=function(id){
	if(!id){
		return;
	}
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
		type:"POST",
		dataType:'JSON',
		url:'/r/outNew/deleteOutUseManage',
		async:false,
        data:JSON.stringify({
    		"id":id
		}),
		success:function(jsonObject){
			if(jsonObject.status == 1){
				showMessageForSuccess("删除成功！");
				search_FlagTypeSetting();//刷新数据列表
			}else{
				showMessageForFail("删除失败："+jsonObject.message);
			}
		}
	});
}


//自动加载用途选择列表
var getFlagType_select=function($div,title){
	if(!FlagTypeList||FlagTypeList.length<1){
		//重查用途类型
		$.ajax({
			type:"GET",
			dataType:'JSON',
			url:'/r/outNew/findOutUseManage',
			async:false,
			success:function(jsonObject){
		        if(jsonObject.status == 1){
		        	FlagTypeList=jsonObject.data;
		        }
			}
		});
	}
	var options="";
	options+="<option value='' >"+(title?title:"全部")+"</option>";
	$.each(FlagTypeList,function(index,record){
		options+="<option value="+record.id+" >"+record.useName+"</option>";
	});
	$div.html(options);
}
