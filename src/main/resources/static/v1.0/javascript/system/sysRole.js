currentPageLocation = window.location.href;
/*
 * 登录信息，所有查询的请求的父类，包含当前页码、uid，token认证信息
 */
// 新增角色之前
function addBefore() {

	$("#modal-title").html("<h2>新增角色</h2>");
	$("#roleId").val("");
	$("#level").val("");
	$('#addOrEditForm').bootstrapValidator('resetForm', true);
	$("#addRole").modal('toggle');

}

// 编辑按钮点击
function _editRole(id) {
	$.ajax({
		type : "post",
		url : "/r/role/get",
		data : {
			"uid" : "1000",
			"token" : "sdwefwewef",
			"id" : id
		},
		dataType : 'json',
		beforeSend : function() {

		},
		success : function(jsonObject) {
			if (jsonObject.status == 1) {
				$('#addOrEditForm').bootstrapValidator('resetForm', true);

				$("#roleName").val(jsonObject.data.name);
				$("#roleDesc").val(jsonObject.data.description);
				$("#level").val(jsonObject.data.level);
				$("#modal-title").html("<h2>编辑角色</h2>");
				$("#roleId").val(jsonObject.data.id);

				$('#addRole').modal("toggle");
			} else {
				bootbox.alert(jsonObject.message);
			}
		},
		error : function(result) {
			bootbox.alert("服务器出现异常，请稍后再试！");
		},
		complete : function() {

		}
	});
}

// 删除
function _deleteRole(id) {
	bootbox.confirm("确定要删除 ?", function(result) {
		if (result) {
			$.ajax({
				type : "post",
				url : "/r/role/delete",
				async:false,
				data : {
					"uid" : "1000",
					"token" : "sdwefwewef",
					"id" : id
				},
				dataType : 'json',
				beforeSend : function() {

				},
				success : function(jsonObject) {
					if (jsonObject.status == 1) {
						_refreshPage(0);
						showMessageForSuccess(jsonObject.message);
					} else {
						showMessageForFail(jsonObject.message);
						// bootbox.alert(jsonObject.message);
					}
				},
				error : function(result) {
					showMessageForFail("服务器出现异常，请稍后再试！");
					// bootbox.alert("服务器出现异常，请稍后再试！");
				},
				complete : function() {

				}
			});
		}
	});
}
/*
 * 刷新页面内容
 */
function _refreshPage(pageNo) {
	//当前页码
	var CurPage=$("#Page").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	$.ajax({
		type : "get",
		url : "/r/role/list",
		data : {"pageNo":CurPage,"pageSize":$.session.get('initPageSize')},
		dataType : 'json',
		beforeSend : function() {
		},
		success : function(jsonObject) {
			if (jsonObject.status == 1) {
				_showPage(jsonObject.data);
				var trn = '<tr>'
    	            +'<td colspan="3">总计：'+jsonObject.page.totalElements+'</td>'
    	            +'</tr>';
				$('#roleBody').append(trn);
				_refreshPaging(jsonObject.page);
			} else {
				showMessageForFail(jsonObject.message);
				// bootbox.alert(jsonObject.message);
			}
		},
		error : function(result) {
			showMessageForFail("服务器出现异常，请稍后再试！");
			// bootbox.alert("服务器出现异常，请稍后再试！");
		},
		complete : function() {
		}
	});
};
// 显示表格
function _showPage(data) {
	var tr = '';
	for ( var i in data) {
		var categoryCode=400;
		if(data[i].name.indexOf("出款")!=-1){
			var categoryCode=100;
		}if(data[i].name.indexOf("入款")!=-1){
			var categoryCode=200;
		}
		tr +=
			'<tr>' +
			// '<td class="center">'+ data[i].id+ '</td>'+
			'<td class="center">'+ data[i].name+ '</td>'+
			'<td class="center">'+ data[i].description+ '</td>'+
			'<td class="center">'+ (data[i].level==null?0:data[i].level)+ '</td>'+
			'<td style="width:10 %" class="center">' +
			'<div style="padding-left:5px " class="hidden-sm hidden-xs btn-group">'
				+ '<button  class="btn btn-xs btn-info btn-white  btn-bold" onclick="_editRole('
				+ data[i].id
				+ ');">'
				+ '<i class="ace-icon fa fa-pencil bigger-120 green">编辑</i>'
				+ '</button></div>'
				+ '<div style="padding-left:5px " class="hidden-sm hidden-xs btn-group">'
				+ '<a href="#/SystemRoleMenu:*?roleId='+ data[i].id+'&'+'roleName='+data[i].name+'">'
                +'<button  class="btn btn-xs btn-info btn-white  btn-bold " >'
				+ '<i class="ace-icon fa fa-lock bigger-120 blue">菜单权限</i>'
				+ '</button></a></div>'
				+ '<div style="padding-left:5px "class="hidden-sm hidden-xs btn-group">'
				+ '<button class="btn btn-xs btn-info btn-white  btn-bold " onclick="_deleteRole('
				+ data[i].id + ');" >'
				+ '<i class="ace-icon fa fa-trash bigger-120 red">删除</i>'
				+ '</button></div>'
				+ '<div style="padding-left:5px "class="hidden-sm hidden-xs btn-group">'
				+ '<a href="#/SystemPersonnelRole:*?roleId='+data[i].id+'&roleName='+encodeURI(encodeURI(data[i].name))+'&categoryCode='+categoryCode+'" type="button" class="btn btn-xs btn-white btn-primary btn-bold">'
				+ '<i class="ace-icon fa fa-list bigger-120">详情'+data[i].counts+'</i>'
				+ '</a></div>' +
			'</td>' +
			'</tr>';

	}
	$('#roleBody').empty().append(tr);
};
/*
 * 刷新分页信息
 */
function _refreshPaging(paging) {
	//分页初始化
	showPading(paging,"Page",_refreshPage);
}

$('#addOrEditForm').bootstrapValidator({
	message : 'This value is not valid',
	feedbackIcons : {
		valid : 'glyphicon glyphicon-ok',
		invalid : 'glyphicon glyphicon-remove',
		validating : 'glyphicon glyphicon-refresh'
	},
	excluded : [ ':disabled' ],
	fields : {

		name : {
			validators : {
				notEmpty : {
					message : '角色名不能为空！'
				},
				stringLength : {
					min : 1,
					max : 45,
					message : '角色名长度必须在1到45之间！'
				},
			}
		},level : {
			validators : {
				notEmpty : {
					message : '等级不能为空！'
				}
			}
		},
		description : {
			validators : {

				stringLength : {
					max : 256,
					message : '描述长度不得超过256！'
				},
			}
		}

	}
});

function addOrEditRole() {

	var bootstrapValidator = $("#addOrEditForm").data('bootstrapValidator');
	bootstrapValidator.validate();

	if (bootstrapValidator.isValid()) {// 验证通过
		var data = getFormJson($("#addOrEditForm"));
		$.ajax({type : 'post', url : '/r/role/save',
			data : data,
			dataType : 'json',
			beforeSend : function() {

			},
			success : function(jsonObject) {
				if (jsonObject.status == 1) {
					$("#addRole").modal('hide');
					showMessageForSuccess(jsonObject.message);
					_refreshPage(0);
				} else {
					showMessageForFail(jsonObject.message);
					// bootbox.alert(jsonObject.message)
				}
			},
			error : function(result) {
				showMessageForFail("服务器出现异常，请稍后再试！");
				// bootbox.alert("服务器出现异常，请稍后再试！");
			},
			complete : function() {

			}
		});

	}

}
// 获得表单数据
function getFormJson(form) {
	var o = {};
	var a = $(form).serializeArray();
	$.each(a, function() {
		if (o[this.name] !== undefined) {
			if (!o[this.name].push) {
				o[this.name] = [ o[this.name] ];
			}
			o[this.name].push(this.value || '');
		} else {
			o[this.name] = this.value || '';
		}
	});
	return o;
}

function clearNoNum(obj) {
    //先把非数字的都替换掉，除了数字和.
    obj.value = obj.value.replace(/[^\d]/g, "");
    //必须保证第一个为数字而不是.
    obj.value = obj.value.replace(/^\./g, "");
    //保证只有出现一个.而没有多个.
    obj.value = obj.value.replace(/\.{2,}/g, ".");
    //保证.只出现一次，而不能出现两次以上
    obj.value = obj.value.replace(".", "$#$").replace(/\./g, "").replace("$#$", ".");
}


$(function() {
	// 设置弹出框的文字为中文
	bootbox.setLocale("zh_CN");
	// 初始化页面数据
	_refreshPage(0);

});
