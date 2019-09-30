
/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	//不指定账号类型时，获取隐藏input的类型值
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountList_page .Current_Page").text();
	//封装data
	var $div = $("#accountFilter");
    var search_EQ_handicapId = $div.find("[name='search_EQ_handicapId']").val();
    var search_LIKE_accountNo = $div.find("[name='search_LIKE_accountNo']").val();
    var search_LIKE_owner = $div.find("[name='search_LIKE_owner']").val();
    var statusToArray = new Array();
    $div.find("input[name='search_IN_status']:checked").each(function(){
        statusToArray.push(this.value);
    });
    var ownTypeToArray = new Array();
    $div.find("input[name='search_IN_ownType']:checked").each(function(){
    	ownTypeToArray.push(this.value);
    });
    var params = {
//            search_EQ_handicapId:$div.find("select[name='search_EQ_handicapId']").val(),
            accountNo:$div.find("select[name='search_LIKE_accountNo']").val(),
            owner:$div.find("select[name='search_LIKE_owner']").val(),
            status:statusToArray,
            ownType:ownTypeToArray,
            pageNo:CurPage<=0?0:CurPage-1,
            pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
        };
    //发送请求
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:"/ysfQrcode/list",
        data:JSON.stringify(params),
        success:function(jsonObject){
            if(jsonObject.status == 1&&jsonObject.data){
            	  var trs="";
            	  $.map(jsonObject.data, function (record) {
            		  var tr="";
            		  tr+="<td>"+_checkObj(record.handicap)+"</td>";
            		  tr+="<td>"+_checkObj(record.accountNo)+"</td>";
            		  tr+="<td>"+_checkObj(record.owner)+"</td>";
            		  tr+="<td>"+_checkObj(0)+"</td>";
            		  tr+="<td>"+_checkObj(0)+"</td>";
            		  tr+="<td>"+_checkObj(0)+"</td>";
            		  tr+="<td><a  title='"+_checkObj(record.remark)+"'>"+timeStamp2yyyyMMddHHmmss(record.updateTime)+"</a></td>";
            		 //1自有 2兼职
            		 tr+="<td><span class='label label-sm label-white middle "+(record.ownType==1?" label-success ":" label-purple ")+"'>"+_checkObj(record.ownTypeDesc)+"</span></td>";
            		 //1启用 2停用
            		 tr+="<td><span class='label label-sm "+(record.status==1?" label-success ":" label-danger ")+"'>"+_checkObj(record.statusDesc)+"</span></td>";
            		 //操作
 					var btn_update_ysf="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight'" +
 						"onclick='showModal_updateYSF("+record.id+")' contentRight=''>" +
 						"<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i>" +
 						"<span>修改</span>" +
 					"</button>";
 					var btn_status_ysf="<button class='btn btn-xs btn-white btn-success btn-bold contentRight'" +
 						"onclick='showModal_updateStatus_YSF("+record.id+","+record.status+",\""+_checkObj(record.operator)+"\",\""+timeStamp2yyyyMMddHHmmss(record.updateTime)+"\")' contentRight=''>" +
 						"<i class='ace-icon fa fa-edit bigger-100 green'></i>" +
 						"<span>状态</span>" +
 					"</button>";
 					var btn_del_ysf="<button class='btn btn-xs btn-white btn-inverse btn-bold contentRight'" +
	 					"onclick='showModal_delYSF("+record.id+")' contentRight=''>" +
	 					"<i class='ace-icon fa fa-trash-o bigger-100 dark'></i>" +
	 					"<span>删除</span>" +
 					"</button>";
            		 tr+="<td>"+btn_update_ysf+btn_status_ysf+btn_del_ysf+"</td>";
            		 trs+=("<tr id='mainTr" + record.id + "'>" + tr + "</tr>");
            	  });
            	  $("#accountListTable").find("tbody").html(trs);
            }else{
            	showMessageForFail("查询失败："+jsonObject.message);
            }
        }
	});
    
}


/** 修改云闪付 */
var showModal_updateYSF=function (ysfId) {
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/phone/ysf/ysf_common.html", 
		success : function(html){
			var $div=$(html).find("#updateYSF_Modal").clone().appendTo($("body"));
			$div.find("td").css("padding-top", "5px");
			var ysfInfo=getYsfAndBindBankInfoById(ysfId+3);
			if(!ysfInfo){
				return;
			}
			getHandicap_select($div.find("select[name='ysf_handicapId']"),null,"--------请选择--------");
			getBankTyp_select($div.find("select[name='add_bankType']"),null,"------------请选择------------");
			$div.modal("toggle");
			
			$div.find("[name=ysf_handicapId]").change(function(){
				reloadLevelTable();
				$(".bindAcc_tr").remove();//避免层级不一致，清空table数据
			});
		    $div.on('hidden.bs.modal', function () {
		        //关闭窗口清除内容;
		        $div.find("[name=type]").unbind();
		        $div.remove();
		    });
		}
	});
}
/** 新增云闪付 */
var showModalAddYSF = function () {
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/phone/ysf/ysf_common.html", 
		success : function(html){
			var $div=$(html).find("#addYSF_Modal").clone().appendTo($("body"));
			$div.find("td").css("padding-top", "5px");
			getHandicap_select($div.find("select[name='ysf_handicapId']"),null,"--------请选择--------");
			getBankTyp_select($div.find("select[name='add_bankType']"),null,"------------请选择------------");
			$div.modal("toggle");

			$div.find("[name=ysf_handicapId]").change(function(){
				reloadLevelTable();
				$(".bindAcc_tr").remove();//避免层级不一致，清空table数据
			});
		    $div.on('hidden.bs.modal', function () {
		        //关闭窗口清除内容;
		        $div.find("[name=type]").unbind();
		        $div.remove();
		    });
		}
	});
}
var doAddYSF_All=function(){
	var $div=$("#addYSF_Modal");
	var $ysf_handicapId=$div.find("[name=ysf_handicapId]");
	var $ownType=$div.find("[name=ysf_ownType]");
	var $ysf_accountNo=$div.find("[name=ysf_accountNo]");
	var $ysf_owner=$div.find("[name=ysf_owner]");
	var $ysf_loginPWD=$div.find("[name=ysf_loginPWD]");
	var $ysf_payPWD=$div.find("[name=ysf_payPWD]");
	var $ysf_remark=$div.find("[name=ysf_remark]");
	//输入校验
	var validatePrint=[
    	{ele:$ysf_handicapId,name:'盘口'},
    	{ele:$ownType,name:'类型'},
    	{ele:$ysf_accountNo,name:'手机号',minLength:11,maxLength:11},
    	{ele:$ysf_owner,name:'真实姓名',minLength:2,maxLength:10},
    	{ele:$ysf_loginPWD,name:'登录密码',maxLength:50},
    	{ele:$ysf_payPWD,name:'支付密码',maxLength:50},
    	{ele:$ysf_remark,name:'备注',maxLength:50}
    ];
	 if(!validateEmptyBatch(validatePrint)||
				!validateInput(validatePrint)){
	    	return;
	}
	var inAccounts=new Array();
	$.map($div.find(".bindAcc_tr"),function(tr){
		inAccounts.push({
			"levelIds":$.trim($(tr).find(".bindAcc_levels").val()).split(","),//层级编码
			"account":$.trim($(tr).find(".bindAcc_account").text()),
	    	"bankType":$.trim($(tr).find(".bindAcc_bankType").text()),
	    	"bankName":$.trim($(tr).find(".bindAcc_bankName").text()),
	    	"owner":$.trim($(tr).find(".bindAcc_owner").text()),
	    	"type":1,
	    	"subType":3,
	    	"status":4
		});
	});
	if(inAccounts.length<1){
     	showMessageForCheck("必须至少添加一张银行卡");
		 return;
	}
	var params={
			"ysfAccountInputDTO": {
				"accountNo":$.trim($ysf_accountNo.val()),
				"owner":$.trim($ysf_owner.val()),
				"loginPWD":$.trim($ysf_loginPWD.val()),
				"payPWD":$.trim($ysf_payPWD.val()),
				"handicapId":$.trim($ysf_handicapId.val()),
				"ownType":$.trim($ownType.val()),
				"remark":$.trim($ysf_remark.val())
			}, 
			"inAccounts": inAccounts 
	}
	//执行新增
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:"/ysfQrcode/addYSF",
        data:JSON.stringify(params),
        success:function(jsonObject){
            if(jsonObject.status == 1&&jsonObject.data){
				showMessageForSuccess("新增成功");
				$div.modal("toggle");
				showAccountList();
            }else{
				showMessageForFail("新增失败" + jsonObject.message);
            }
        }
	});
}
var addAccountToBind=function(){
	var $table=$("#bindYSFAccountListTable"),$div=$("#addAccountToBind");
	var $add_bankType=$div.find("[name=add_bankType]");
	var $add_account=$div.find("[name=add_account]");
	var $add_bankName=$div.find("[name=add_bankName]");
	var $add_owner=$div.find("[name=add_owner]");
	//输入校验
	var validatePrint=[
    	{ele:$add_bankType,name:'银行类别'},
    	{ele:$add_account,name:'银行账号',maxLength:25},
    	{ele:$add_bankName,name:'开户支行',maxLength:50},
    	{ele:$add_owner,name:'开户姓名',minLength:2,maxLength:10}
    ];
	 if(!validateEmptyBatch(validatePrint)||
				!validateInput(validatePrint)){
	    	return;
	}
	 //校验层级
	var selected_leves_id=new Array(),selected_leves_name=new Array();
	$.each($div.find("[name=levelId]:checked"),function(i,result){
		selected_leves_id.push($(result).val());
		selected_leves_name.push($(result).attr("levelName"));
	});
	if(selected_leves_id.length<1){
		showMessageForCheck("请先选择账号层级");
		 return;
	}
	 if($("[name=account"+_checkObj($add_account.val())+"]").length>0){
     	showMessageForCheck("“银行账号”在下方列表已存在！请勿重复");
		 return;
	 }
	 //校验银行卡是否在系统存在
	 if(getAccountInfoByAcc(_checkObj($add_account.val()))){
	     	showMessageForCheck("“银行账号”在系统已存在！请勿重复");
			 return;
	}
    var tbody = "";
    for (var i = 1; i <= 1000; i++) {
        if (checkedTr(i)) {
            var tr = "";
            tr += "<td><span class='bindAcc_bankType' >"+_checkObj($add_bankType.val())+"</span></td>";
            tr += "<td><span class='bindAcc_account' name='account"+_checkObj($add_account.val())+"'>"+_checkObj($add_account.val())+"</span></td>";
            tr += "<td><span class='bindAcc_bankName' >"+_checkObj($add_bankName.val())+"</span></td>";
            tr += "<td><span class='bindAcc_owner' >"+_checkObj($add_owner.val())+"</span></td>";
            tr += "<td><input type='hidden' class='bindAcc_levels' value='"+_checkObj(selected_leves_id.toString())+"'/><span>"+_checkObj(selected_leves_name.toString())+"</span></td>";
            //操作
            tr += "<td>";
            //提现
            tr += "<button onclick=doRemoveTr(" + i + ") type='button' class='btn btn-xs btn-white btn-bold' >"
                + "<i class='ace-icon fa fa-close bigger-100 green'></i><span>移除</span></button>";
            tr += "</td>";
            tbody += "<tr class='bindAcc_tr' id='bindAccount_Tr" + i + "'>" + tr + "</tr>";
            break;
        }
    }
    $table.find("tbody").append($(tbody));
    resetBindBank();
}

var reloadLevelTable=function(){
	var handicapId=$("#addYSF_Modal [name=ysf_handicapId]").val();
	var $levelListDiv=$("#levelListDiv"),HTML="";
	if(handicapId){
		var allLevelList=getLevelByHandicapId(handicapId);
		if(allLevelList&&allLevelList.length>0){
			allLevelList.map(function(record){
				if(record.code){
					HTML+='<div class="col-sm-3">'+
					'<span style="margin-bottom:10px;" class="label label-xlg label-success" title="'+record.name+'">'+
						'<label style="width:110px;text-align:left;">'+
							'<input type="checkbox"  name="levelId" levelName="'+record.name+'" class="ace" value="'+record.id+'"/>'+
							'&nbsp;<span class="lbl">'+record.name+'</span>'+
						'</label>'+
					'</span>'+
				'</div>';
				}
			});
		}
	}
	if(!HTML){
		HTML="<p class='alert alert-danger center'>请先选择云闪付的盘口，以获取绑定账号的层级信息</p>";
	}
	$levelListDiv.html(HTML);
}
var doRemoveTr=function(row){
	$("#bindYSFAccountListTable").find("#bindAccount_Tr"+row).remove();
}
var resetYSF=function(){
	//顺序勿调换
	reset('tableAdd');
	reloadLevelTable();
	$(".bindAcc_tr").remove();//避免层级不一致，清空table数据
}
var resetBindBank=function(){
	//顺序勿调换
	reset('addAccountToBind');
	reloadLevelTable();
}
var checkedTr = function (row) {
    if ($("#bindAccount_Tr" + row).length == 1) {
        return false;
    } else {
        return true;
    }
}

var showModal_updateStatus_YSF=function(ysfId,currStatus,operator,updateTime){
	//停用 2
	var btn_param={
			"label":"在用",
			"className":"btn btn-sm btn-success",
			"status":1
	}
	if(currStatus==1){//启用 1
		var btn_param={
				"label":"停用",
				"className":"btn btn-sm btn-primary",
				"status":2
		}
	}
	bootbox.dialog({
		title:		"云闪付状态修改",
		message: 	"<span class='bigger-110'>" +
						"<br/>" +
						"<label class='control-label bolder blue'>更新：&nbsp;&nbsp;&nbsp;&nbsp;</label>"
						+updateTime+"&nbsp;&nbsp;&nbsp;&nbsp;"+operator +
					"</span>" +
					"<br/>" +
					"<label class='control-label bolder blue'>备注：&nbsp;&nbsp;&nbsp;&nbsp;</label>" +
					"<input name='updateStatus_remark' style='height:32px;width:400px;'class='input-medium' placeholder='请输入备注（5-300字）' >" +
					"<br/><br/>",
		buttons:{
			btn_updateStatus:{"label" : btn_param.label,"className" :btn_param.className,"callback": function() {
				var remark=$("[name=updateStatus_remark]").val();
				// 不强制写备注 但是如果写了 就要按字数来
				if(remark&&(remark.length<5||remark.length>300)){
					showMessageForFail("请输入备注，5-300字之间");
					return false;
				}
				let data = { 
						"id":ysfId,
						"status":btn_param.status,
						"remark":remark
					};
				$.ajax({ 
					dataType:'json',
					contentType:"application/json",
					type:"post", 
					url:"/ysfQrcode/updateYSFStatus",
					data:JSON.stringify(data), 
					success:function(jsonObject){
						if(jsonObject.status == 1){
							showMessageForSuccess("本系统状态修改成功");
							showAccountList();
						}else{
							showMessageForFail(jsonObject.message);
						}
					}
				});
			}},
			btn_cancel:{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
}

var showModal_delYSF=function(ysfId){
	bootbox.confirm("确定删除<span></span>?", function(result) {
		if (result) {
			//执行删除
			$.ajax({
		        dataType:'JSON', 
		        type:"DELETE", 
				async:false,
		        url:"/ysfQrcode/deleteYSF/" + ysfId,
		        success:function(jsonObject){
		            if(jsonObject.status == 1){
						showMessageForSuccess("删除成功");
						showAccountList();
		            }else{
						showMessageForFail("删除失败" + jsonObject.message);
		            }
		        }
			});
		}
	});
}
var getYsfAndBindBankInfoById=function(ysfId){
	var result='';
    var params = {
            id:ysfId,
            pageNo:0,
            pageSize:10
        };
    //发送请求
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:"/ysfQrcode/list",
        data:JSON.stringify(params),
        success:function(jsonObject){
        	if(jsonObject.status == 1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取银行账号信息异常，"+jsonObject.message);
			}
        }
	});
	return result;
}

getHandicap_select($("#accountFilter").find("select[name='search_EQ_handicapId']"),null,"全部");
showAccountList(0);
$("#accountFilter").find("[name=search_EQ_handicapId]").change(function(){
	showAccountList(0);
});
$("#accountFilter").find("[name=search_IN_status],[name=search_IN_ownType]").click(function(){
	showAccountList(0);
});
$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		$("#accountFilter #searchBtn button").click();
	}
});