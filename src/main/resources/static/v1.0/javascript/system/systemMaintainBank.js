var bankTypeList=[],peerBankTypeList=[];

/**
 * 加载银行类别列表
 */
var loadBankList=function(){
	var $div=$("#income_widget #bankTypeList");
	//正在维护的银行数组 英文分号切割
	var sys_BankType_Maintain=sysSetting.OUTDRAW_SYS_MAINTAIN_BANKTYPE.split(",");
	var bankHTML="";
	var closeBtn='';
	$.each(bankTypeList,function(i,bankType){
		bankType=$.trim(bankType);
		if(bankType){
			var search_bankType_like=$.trim($("#search_bankType_like").val());
			var lable_color=' label-grey ';
			var checked='';
			if($.inArray(bankType, sys_BankType_Maintain)!=-1){
				//银行已在维护列表
				lable_color=' label-primary ';
				checked=' checked="checked" ';
				closeBtn='';
			}else{
				closeBtn='<span onclick="deleteBankType(\''+bankType+'\')" style="cursor:pointer;">&nbsp;&nbsp;&nbsp;<i class="light-red ace-icon fa fa-close"></i>&nbsp;&nbsp;&nbsp;<span>';
			} 
			if(search_bankType_like&&bankType.indexOf(search_bankType_like)>-1){
				//搜索内容存在
				lable_color=' label-danger ';
			}
			bankHTML+='<div class="col-sm-3">'+
						'<span style="margin-bottom:10px;" class="label label-xlg '+lable_color+' arrowed-in-right arrowed-in">'+
							'<label class="inline">'+
								'<input type="checkbox" '+checked+' name="checkbox_bankType" class="ace" value="'+bankType+'"/>'+
								'&nbsp;<span class="lbl">'+bankType+'</span>'+
							'</label>'+
						'</span>'+closeBtn+
					'</div>';
		}
	});
	$div.html(bankHTML);
}

/**
 * 保存已勾选的银行类别保存到数据库
 */
var saveBankList=function(){
	bootbox.dialog({
		message: '<span style="color:red;">确定对勾选的银行暂停出款吗？</span>',
		buttons:{
			"click" :{"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
				var list=new Array();
				$.each($("#income_widget [name=checkbox_bankType]"),function(i,result){
					if($(result).is(':checked')){
						list.push($(result).val());
					}
				});
				$.ajax({
					type:"PUT",
					dataType:'JSON',
					url:'/r/set/update',
					async:false,
					data:{
						"keysArray":["OUTDRAW_SYS_MAINTAIN_BANKTYPE"].toString(),
						"valsArray":[list.join(",")].toString()//空时传递任意字符，以免解析过去的是空数组，导致保存失败
					},
					success:function(jsonObject){
						if(jsonObject&&jsonObject.status==1){
							reloadSetting();
							loadBankList();
							showMessageForSuccess("保存成功");
						}else{
							showMessageForFail("保存失败"+jsonObject.message);
						}
					}
				});
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
}

/**
 * 加载同行转账列表
 */
var loadPeerBankList=function(){
	var $div=$("#peer_widget #bankTypeList");
	var bankHTML="";
	$.each(bank_name_list,function(i,bankType){
		bankType=$.trim(bankType);
		var search_bankType_like=$.trim($("#search_bankType_like").val());
		var lable_color=' label-grey ';
		var checked='';
		if($.inArray(bankType, peerBankTypeList)!=-1){
			//银行已在维护列表
			lable_color=' label-primary ';
			checked=' checked="checked" ';
		}
		if(search_bankType_like&&bankType.indexOf(search_bankType_like)>-1){
			//搜索内容存在
			lable_color=' label-danger ';
		}
		bankHTML+='<div class="col-sm-3">'+
					'<span style="margin-bottom:10px;" class="label label-xlg '+lable_color+' arrowed-in-right arrowed-in">'+
						'<label class="inline">'+
							'<input type="checkbox" '+checked+' name="checkbox_bankType" class="ace" value="'+bankType+'"/>'+
							'&nbsp;<span class="lbl">'+bankType+'</span>'+
						'</label>'+
					'</span>'+
				'</div>';
	});
	$div.html(bankHTML);
}

/**
 * 保存已勾选的同行转账保存到数据库
 */
var savePeerBankList=function(){
	bootbox.dialog({
		message: '<span style="color:red;">确定对勾选的银行开启同行转账吗？</span>',
		buttons:{
			"click" :{"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
				var list=new Array();
				$.each($("#peer_widget [name=checkbox_bankType]"),function(i,result){
					if($(result).is(':checked')){
						list.push($(result).val());
					}
				});
				$.ajax({
					type:"PUT",
					dataType:'JSON',
					url:'/r/set/update',
					async:false,
					data:{
						"keysArray":["OUTDRAW_SYS_PEER_TRANSFER"].toString(),
						"valsArray":[list.join(",")].toString()//空时传递任意字符，以免解析过去的是空数组，导致保存失败
					},
					success:function(jsonObject){
						if(jsonObject&&jsonObject.status==1){
							reloadSetting();
							loadPeerBankList();
							showMessageForSuccess("保存成功");
						}else{
							showMessageForFail("保存失败"+jsonObject.message);
						}
					}
				});
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
}

/**
 * 删除所有存储的银行列表
 */
var deleteBankType=function(bankType){
	bootbox.confirm("确定要删除银行："+bankType+"?", function(result) {
		if (result) {
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/set/deleteMaintainBank',
				async:false,
				data:{
					"bankType":$.trim(bankType)
				},
				success:function(jsonObject){
					if(jsonObject&&jsonObject.status==1){
						reloadSetting();
						loadBankList();
						showMessageForSuccess("删除成功");
					}else{
						showMessageForFail("删除失败"+jsonObject.message);
					}
				}
			});
		}
	});
}

/**
 * 新增银行类别窗口展示
 */
var showModal_addBankType=function(){
	var $div=$("#addBankTypeModal");
	$div.find("[name=bankTypeName]").val("");
	$div.modal("toggle");
}
/**
 * 新增银行类别
 */
var do_addBankType=function(){
	var $div=$("#addBankTypeModal");
	var $bankTypeName=$div.find("[name=bankTypeName]");
	var bankTypeName=$.trim($bankTypeName.val());
	//校验
	if(!validateEmpty($bankTypeName,"备注")){
		return ;
	}
	if(bankTypeName.length<2||bankTypeName.length>20){
		showMessageForCheck("银行名长度应该在2 ~ 20之间",$bankTypeName);
		return;
	}
	if($.inArray(bankTypeName,bankTypeList)!=-1){
		showMessageForCheck("已存在此银行名，请检查！",$bankTypeName);
		return;
	}
	bootbox.confirm("确定要新增银行："+$.trim($bankTypeName.val())+"?", function(result) {
		if (result) {
			var bankTypeStr="";
			if(sysSetting.OUTDRAW_SYS_ALL_BANKTYPE&&sysSetting.OUTDRAW_SYS_ALL_BANKTYPE.length>0){
				bankTypeStr=sysSetting.OUTDRAW_SYS_ALL_BANKTYPE+";"+bankTypeName;
			}else{
				bankTypeStr=bankTypeName;
			}
			$.ajax({
				type:"PUT",
				dataType:'JSON',
				url:'/r/set/update',
				async:false,
				data:{
					"keysArray":["OUTDRAW_SYS_ALL_BANKTYPE"].toString(),
					"valsArray":[bankTypeStr].toString()//空时传递任意字符，以免解析过去的是空数组，导致保存失败
				},
				success:function(jsonObject){
					if(jsonObject&&jsonObject.status==1){
						reloadSetting();
						loadBankList();
						showMessageForSuccess("新增成功");
						$div.modal("toggle");
					}else{
						showMessageForFail("新增失败"+jsonObject.message);
					}
				}
			});
		}
	});
}
/**
 * 同步刷新系统设置
 */
var reloadSetting=function(){
	$.ajax({  type:"POST", async:false,url:'/r/set/findAllToMap',dataType:'JSON',success:function(res){
        if(res.status !=1){
		    return;
        }
		sysSetting=res.data;
		if(sysSetting&&sysSetting.OUTDRAW_SYS_ALL_BANKTYPE){
			bankTypeList=sysSetting.OUTDRAW_SYS_ALL_BANKTYPE.split(";");
			peerBankTypeList=sysSetting.OUTDRAW_SYS_PEER_TRANSFER.split(",");
		}
     }});
}
var reloadAll=function(){
	reloadSetting();
	loadPeerBankList();
	loadBankList();
}
$("#page_bankType_maintain").keypress(function(e){
	if(event.keyCode == 13) {
		reloadAll();
	}
});
$(document).ready(function(){
	reloadAll();
});

$("#checkedAll_peer_widget").click(function(){
	$("#peer_widget").find("[name=checkbox_bankType]").prop("checked",$("#checkedAll_peer_widget").prop("checked"))
});
$("#checkedAll_income_widget").click(function(){
	$("#income_widget").find("[name=checkbox_bankType]").prop("checked",$("#checkedAll_income_widget").prop("checked"))
});