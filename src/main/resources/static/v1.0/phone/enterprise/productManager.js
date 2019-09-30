//商品说明库 商品类型用js

/** 形容词名词 分页查询 */
var showModalProductList=function(){
	var $div = $("#productContenModal");
	reset('searchContent_filter');
	$div.modal("toggle");
	searchContent_infoLoad(0);
}
var searchContent_infoLoad=function(CurPage){
	var $div=$("#productContenModal");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#contentPage .Current_Page").text();
	var params={
			oid:$.trim($("#accountFilter").find("select[name=search_EQ_handicapCode]").val()),//盘口id读取当前查询条件
			content:$.trim($div.find("[name=content]").val()),//商品说明文字
			typeName:$.trim($div.find("[name=typeName]").val()),//商品类型
			adminName:$.trim($div.find("[name=adminName]").val()),//最后操作人
			pageNo:(!CurPage||CurPage<=0)?0:CurPage-1,
			pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
	};
	var jsonObject=findPayOwnerWordList(params);
	if (jsonObject&&jsonObject.status == 1 ) {
    	result = jsonObject.data;
		var trs="";
		$.each(result,function(index,record){
			trs+="<tr>";
			trs+="<td>"+(index+1)+"</td>";
			trs+="<td>"+_checkObj(record.content)+"</td>";
			trs+="<td>"+_checkObj(record.typeName)+"</td>";
			trs+="<td>"+_checkObj(record.adminName)+"</td>";
			trs+="<td>"+_checkObj(record.adminTime)+"</td>";
			trs+="<td>";
			trs+="<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='AlipayManager:ContentSetting:*'  \
				onclick='showModalUpdateContent(" + record.oid + "," + record.id + "," + record.typeId +  ",\"" + _checkObj(record.content) + "\");' >\
				<span>修改</span>\
				</a>";
			trs+="<a class='btn btn-xs btn-white btn-inverse btn-bold black contentRight' contentright='AlipayManager:ContentSetting:*'  \
				onclick='showModalRemoveContent(" + record.oid + "," + record.id + ");' >\
				<span>删除</span>\
				</a>";
			trs+="</td>";
			trs+="</tr>";
		});
		$div.find("tbody").html(trs);
		//分页初始化
		showPading(jsonObject.page,"contentPage",searchContent_infoLoad,null,true);
		contentRight();
	} else {
		showMessageForFail("查询失败：" + jsonObject.message);
	}
}

/** 商品说明 内容新增 */
var showModalAddContent=function(){
    var $div = $("#addProduct4clone").clone().attr("id", "addProduct");
    var oid=$.trim($("select[name=search_EQ_handicapCode]").val());
    $div.find("td").css("padding-top", "10px");
    //初始化
    $div.find("#oid").val(oid);
    getProductTypeByOid($div.find("select[name='type_select']"),oid);
    
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () {
        //关闭窗口清除内容;
        $div.remove();
    });
}
var doAddContent=function(){
	var $div = $("#addProduct");
    var $type_select = $div.find("[name=type_select]");
    var $content = $div.find("[name=content]");
    var validate = [
        {ele: $type_select, name: '类型编号'},
        {ele: $content, name: '商品说明文字'}
    ];
    if (!validateEmptyBatch(validate)) {//非空校验
        return;
    }
	var params = {
		oid:$div.find("#oid").val(),//盘口
		typeId:$.trim($type_select.val()),//类型编号
		content:$.trim($content.val())//商品说明文字
	}
	addPayOwnerWord(params,$div,searchContent_infoLoad);
}
/** 商品说明 内容修改 */
var showModalUpdateContent=function(oid,id,typeId,oldContent){
	var $div = $("#updateProduct4clone").clone().attr("id", "updateProduct");
	$div.find("td").css("padding-top", "10px");
	//初始化
	$div.find("#oid").val(oid);
	$div.find("#id").val(id);
	getProductTypeByOid($div.find("select[name='type_select']"),oid,typeId);
	$div.find("[name=content]").val(oldContent);
	
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除内容;
		$div.remove();
	});
}
var doUpdateContent=function(){
	var $div = $("#updateProduct");
	var $type_select = $div.find("[name=type_select]");
	var $content = $div.find("[name=content]");
	var validate = [
		{ele: $type_select, name: '类型编号'},
		{ele: $content, name: '商品说明文字'}
		];
	if (!validateEmptyBatch(validate)) {//非空校验
		return;
	}
	var params = {
			oid:$div.find("#oid").val(),//盘口
			id:$div.find("#id").val(),//主键ID
			typeId:$.trim($type_select.val()),//类型编号
			content:$.trim($content.val())//商品说明文字
	}
	modifyPayOwnerWord(params,$div,searchContent_infoLoad);
}
/** 商品说明 删除 */
var showModalRemoveContent=function(oid,id){
	var params={
			oid:oid,
			id:id
	}
	removePayOwnerWord(params,searchContent_infoLoad);
}



/** 商品类型列表 */
var showModalProductTypeList=function(){
	var $div = $("#productTypeModal");
	reset('searchType_filter');
	$div.modal("toggle");
	searchType_infoLoad(0);
}
var searchType_infoLoad=function(CurPage){
	var $div=$("#productTypeModal");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#typePage .Current_Page").text();
	var params={
			oid:$.trim($("#accountFilter").find("select[name=search_EQ_handicapCode]").val()),//盘口id读取当前查询条件
			typeName:$.trim($div.find("[name=typeName]").val()),//商品类型
			adminName:$.trim($div.find("[name=adminName]").val()),//最后操作人
			pageNo:(!CurPage||CurPage<=0)?0:CurPage-1,
			pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
	};
	var jsonObject=findPayOwnerWordTypeList(params);
	if (jsonObject&&jsonObject.status == 1 ) {
    	result = jsonObject.data;
		var trs="";
		$.each(result,function(index,record){
			trs+="<tr>";
			trs+="<td>"+(index+1)+"</td>";
			trs+="<td>"+_checkObj(record.typeName)+"</td>";
			trs+="<td>"+_checkObj(record.adminName)+"</td>";
			trs+="<td>"+_checkObj(record.adminTime)+"</td>";
			trs+="<td>";
			trs+="<a class='btn btn-xs btn-white btn-inverse btn-bold black contentRight' contentright='AlipayManager:TypeSetting:*'  \
					onclick='showModalRemoveType(" + record.oid + "," + record.id + ");' >\
					<span>删除</span>\
				</a>";
			trs+="</td>";
			trs+="</tr>";
		});
		$div.find("tbody").html(trs);
		//分页初始化
		showPading(jsonObject.page,"typePage",searchType_infoLoad,null,true);
		contentRight();
	} else {
		showMessageForFail("查询失败：" + jsonObject.message);
	}
}
/** 商品类型 新增 */
var showModalAddType=function(){
	bootbox.dialog({
		title:"<span class='blue bolder' >新增商品类型<span>",
		message: "<input type='text' name='typeName' class='input-sm width200' placeholder='仅支持：中文/字母/数字'  >",
		buttons:{
			"click" :{
				"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
					var $typeName=$(this).find("[name=typeName]");
					 var validateEmpty=[
					    	{ele:$typeName,name:'类型名称',minLength:1,maxLength:10}
					    ];
				    if(!validateEmptyBatch(validateEmpty)||
							!validateInput(validateEmpty)){
				    	return false;
				    }
				    if(!checkInputContent($typeName.val())){
				    	showMessageForCheck("请勿输入中文/字母/数字外的任何特殊字符");
				    	return false;
				    }
					var params={
							oid:$.trim($("select[name=search_EQ_handicapCode]").val()),
							typeName:$.trim($typeName.val())
					}
					addPayOwnerWordType(params,searchType_infoLoad);
					setTimeout(function(){       
						$('body').addClass('modal-open');
					},500);
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default",
				"callback": function() {
					setTimeout(function(){       
						$('body').addClass('modal-open');
					},500);
				}
			}
		},
		closeButton:false
	});
}
/** 商品类型 删除 */
var showModalRemoveType=function(oid,id){
	var params={
			oid:oid,
			id:id
	}
	removePayOwnerWordType(params,searchType_infoLoad);
}
/** 商品类型 绑定弹出框 */
var showModalTypeBind=function(oid,id){
	var $div=$("#typeBindModal").modal("toggle");
	$div.find("#oid").val(oid);
	$div.find("#id").val(id);
	//重置参数
	resetProductTypeInfo();
}
var resetProductTypeInfo=function(){
	var $div=$("#typeBindModal");
	var params={
			oid:$div.find("#oid").val(),
			id:$div.find("#id").val()
	};
	var HTML="";
	var closeBtn='';
	var jsonObject=findPayOwnerWordBindList(params);
	if (jsonObject&&jsonObject.status == 1 ) {
    	result = jsonObject.data;
		$.each(result,function(index,record){
			if(!record||!record.typeId||!record.typeName){
				//数据不对跳出本次循环
				return true;
			}
			var lable_color=' label-grey ';
			var checked='';
			if(record.isBind*1==1){
				//已绑定的类型
				lable_color=' label-primary ';
				checked=' checked="checked" ';
				closeBtn='';
			}
			//是否有商品说明(0:没有，1:有 注:返回0则前端不允许勾选)
			var disabled=" disabled ";
			if(record.isHaveDesc*1){
				disabled="";
			}else{
				lable_color=' label-light ';
			}
			HTML+='<div class="col-sm-3">'+
					'<span style="margin-bottom:10px;" class="label label-xlg '+lable_color+' ">'+
						'<label style="width:150px;text-align:left;">'+
							'<input type="checkbox" '+checked+' name="checkbox_typeId" class="ace" '+disabled+' value="'+record.typeId+'"/>'+
							'&nbsp;<span class="lbl">&nbsp;&nbsp;'+record.typeName+'</span>'+
						'</label>'+
					'</span>'+closeBtn+
				'</div>';
			
			
		});
	}
	$div.find("#productTypeList").html(HTML);
}
var saveTypeBind=function(){
	var $div=$("#typeBindModal");
	var typeIdList=new Array();
	$.each($div.find("[name=checkbox_typeId]"),function(i,result){
		if($(result).is(":checked")){
			typeIdList.push($(result).val());
		}
	});
	var param={
			oid:$div.find("#oid").val(),
			id:$div.find("#id").val(),
			typeIdList:typeIdList
	}
	savePayOwnerWordBind(param,$div);
}

/** 1.2.1 商品说明列表查询 */
var findPayOwnerWordList=function(param){
	var result;
	$.ajax({
        dataType: 'JSON',
        contentType: 'application/json;charset=UTF-8',
        type: "POST",
        url: "/ali4enterprise/findPayOwnerWordList",
        async:false, 
        data: JSON.stringify(param),
        success: function (jsonObject) {
        	result=jsonObject;
        }
    });
	return result;
}
/** 1.2.2 新增商品说明 */
var addPayOwnerWord=function(param,$div,fnName){
	bootbox.confirm("确定新增商品说明?", function (result) {
    	if (result) {
    		$.ajax({
    	        dataType: 'JSON',
                contentType: 'application/json;charset=UTF-8',
    	        type: "POST",
    	        url: "/ali4enterprise/addPayOwnerWord",
    	        data: JSON.stringify(param),
    	        success: function (jsonObject) {
    	            if (jsonObject.status == 1) {
    	        		showMessageForSuccess("新增成功");
    	        		if($div){
						    $div.modal("toggle");
						}
    	        		if(fnName){
    	        			fnName();
    	        		}
    	            } else {
    	                showMessageForFail("新增失败：" + jsonObject.message);
    	            }
    	        }
    	    });
    	}
		setTimeout(function(){       
			$('body').addClass('modal-open');
		},500);
	});
}
/** 1.2.3 修改商品说明 */
var modifyPayOwnerWord=function(param,$div,fnName){
	bootbox.confirm("确定修改商品说明?", function (result) {
		if (result) {
			$.ajax({
				dataType: 'JSON',
				contentType: 'application/json;charset=UTF-8',
				type: "POST",
				url: "/ali4enterprise/modifyPayOwnerWord",
				data: JSON.stringify(param),
				success: function (jsonObject) {
					if (jsonObject.status == 1) {
						showMessageForSuccess("修改成功");
						if($div){
						    $div.modal("toggle");
						}
						if(fnName){
							fnName();
						}
					} else {
						showMessageForFail("修改失败：" + jsonObject.message);
					}
				}
			});
		}
		setTimeout(function(){       
			$('body').addClass('modal-open');
		},500);
	});
}
/** 1.2.4 删除商品说明 */
var removePayOwnerWord=function(param,fnName){
	bootbox.confirm("确定删除商品说明?", function (result) {
		if (result) {
			$.ajax({
				dataType: 'JSON',
				contentType: 'application/json;charset=UTF-8',
				type: "POST",
				url: "/ali4enterprise/removePayOwnerWord",
				data: JSON.stringify(param),
				success: function (jsonObject) {
					if (jsonObject.status == 1) {
						showMessageForSuccess("删除成功");
						if(fnName){
							fnName();
						}
					} else {
						showMessageForFail("删除失败：" + jsonObject.message);
					}
				}
			});
		}
		setTimeout(function(){       
			$('body').addClass('modal-open');
		},500);
	});
}

/** 1.2.5 商品说明库-统计 */
var findPayOwnerWordSta=function(param){
	var result;
	$.ajax({
        dataType: 'JSON',
        contentType: 'application/json;charset=UTF-8',
        type: "POST",
        url: "/ali4enterprise/findPayOwnerWordSta",
        async:false, 
        data: JSON.stringify(param),
        success: function (jsonObject) {
        	result=jsonObject;
        }
    });
	return result;
}
/** 1.2.6 商品类型 查询 参数多 返回值多 */
var findPayOwnerWordTypeList=function(param){
	var result;
	$.ajax({
		dataType: 'JSON',
		contentType: 'application/json;charset=UTF-8',
		type: "POST",
		url: "/ali4enterprise/findPayOwnerWordTypeList",
		async:false, 
		data: JSON.stringify(param),
		success: function (jsonObject) {
			result=jsonObject;
		}
	});
	return result;
}
/** 1.2.7 商品类型 查询 参数少 返回值少 */
var findPayOwnerWordTypeNameList=function(param){
	var result;
	$.ajax({
		dataType: 'JSON',
		contentType: 'application/json;charset=UTF-8',
		type: "POST",
		url: "/ali4enterprise/findPayOwnerWordTypeNameList",
		async:false, 
		data: JSON.stringify(param),
		success: function (jsonObject) {
			if (jsonObject.status == 1 && jsonObject.data && jsonObject.data) {
				result = jsonObject.data;
			}
		}
	});
	return result;
}
/** 1.2.8 商品类型 新增 */
var addPayOwnerWordType=function(param,fnName){
	$.ajax({
		dataType: 'JSON',
		contentType: 'application/json;charset=UTF-8',
		type: "POST",
		url: "/ali4enterprise/addPayOwnerWordType",
		data: JSON.stringify(param),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				showMessageForSuccess("新增成功");
				if(fnName){
					fnName();
				}
			} else {
				showMessageForFail("新增失败：" + jsonObject.message);
			}
		}
	});
}
/** 1.2.9 商品类型 删除 */
var removePayOwnerWordType=function(param){
	bootbox.confirm("确定删除商品说明?", function (result) {
		if (result) {
			$.ajax({
				dataType: 'JSON',
				contentType: 'application/json;charset=UTF-8',
				type: "POST",
				url: "/ali4enterprise/removePayOwnerWordType",
				data: JSON.stringify(param),
				success: function (jsonObject) {
					if (jsonObject.status == 1) {
						showMessageForSuccess("删除成功");
						if(fnName){
							fnName();
						}
					} else {
						showMessageForFail("删除失败：" + jsonObject.message);
					}
				}
			});
		}
		setTimeout(function(){       
			$('body').addClass('modal-open');
		},500);
	});
}
/** 1.2.10 绑定商品类型-查询 */
var findPayOwnerWordBindList=function(param){
	var result;
	$.ajax({
		dataType: 'JSON',
		contentType: 'application/json;charset=UTF-8',
		type: "POST",
		url: "/ali4enterprise/findPayOwnerWordBindList",
		async:false, 
		data: JSON.stringify(param),
		success: function (jsonObject) {
			result=jsonObject;
		}
	});
	return result;
}
/** 1.2.11 商品类型 绑定商品类型-保存 */
var savePayOwnerWordBind=function(param,$div,fnName){
	bootbox.confirm("确定绑定商品类型?", function (result) {
		if (result) {
			$.ajax({
				dataType: 'JSON',
				contentType: 'application/json;charset=UTF-8',
				type: "POST",
				url: "/ali4enterprise/savePayOwnerWordBind",
				data: JSON.stringify(param),
				success: function (jsonObject) {
					if (jsonObject.status == 1) {
						showMessageForSuccess("绑定成功");
						if($div){
							$div.modal("toggle");
						}
						if(fnName){
							fnName();
						}
					} else {
						showMessageForFail("绑定失败：" + jsonObject.message);
					}
				}
			});
		}
		setTimeout(function(){       
			$('body').addClass('modal-open');
		},500);
	});
}
/** 自动加载商品类别选择列表 */
var getProductTypeByOid=function($div,oid,key,titleName){
	var options="";
	var list=new Array();
	list=findPayOwnerWordTypeNameList({oid:oid});
	options+="<option value='' >"+(titleName?titleName:"--------------请选择--------------")+"</option>";
	if(oid){
		$.each(list,function(index,record){
			if(key&&record.key==key){
				options+="<option selected value="+record.key+" >"+record.value+"</option>";
			}else{
				options+="<option value="+record.key+" >"+(record.value?record.value:"")+"</option>";
			}
		});
	}
	$div.html(options);
}