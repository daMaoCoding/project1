var authRequest = {"pageNo": 0, "pageSize": $.session.get('initPageSize')};
var request = getRequest(); //categoryCode
authRequest.categoryCode = request.categoryCode;
//定义公共的变量，返回的时候进行传参。判断需要查询的数据，已经参数的值显示
var roleId="";
var roleName="";

//绑定按键事件，回车查询数据
$('#bindKeyCode').bind('keypress',getKeyCodePK);   
function getKeyCodePK(e) {  
    var evt = e || window.event;  
    var keyCode = evt.keyCode || evt.which || evt.charCode;
    if(keyCode==13){
    	if($("#addUserRole_modal").is(":hidden")){findPerplesByRoleId();return;}
    	addUserRole();
    }
}

//根据角色获取人员
var findPerplesByRoleId=function(){
	//获取账号
	var account=$("#account").val();
	//当前页码
	var CurPage=$("#userPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	var request=getRequest();
	//获取父页面的参数
	if(request&&request.roleId){
		roleId=request.roleId;
		roleName=decodeURI(decodeURI(request.roleName));
	}
	$("#roleName").text(roleName);
	$.ajax({
		type:"post",
		url:"/r/role/findPerplesByRoleId",
		data:{
			"pageNo":CurPage,
			"roleId":roleId,
			"account":account,
			"type":1,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				var tr = '';
				 //小计
				 var counts = 0;
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
					 var statusStr=val.status==0?"<span class='label label-sm label-success'>启用</span>":"<span class='label label-sm label-inverse'>停用</span>";
                    tr += '<tr>'
                    	     +'<td><input type="checkBox" value="'+val.id+'" name="undo" /></td>'
                    	     +'<td>' + val.uid + '</td>'
                    	     +'<td>'+ val.username +'</td>'
                    	     +'<td>' + val.classification + '</td>'
                    	     +'<td>'+val.outLimit+'</td>'
                    	     +'<td>' + val.auditLimit + '</td>'
                    	     +'<td>' + val.createTimeStr + '</td>'
                    	     +'<td>'+statusStr+'</td>'
                    	 +'</tr>';
                    counts +=1;
                };
				 $('#total_tbody').empty().html(tr);
				 var trs = '<tr>'
							 +'<td colspan="8">小计：'+counts+'</td>'
						  +'</tr>';
                $('#total_tbody').append(trs);
                var trn = '<tr>'
		                	+'<td colspan="8">总计：'+jsonObject.data.page.totalElements+'</td>'
						 +'</tr>';
                $('#total_tbody').append(trn);
			}else{
				$('#total_tbody').empty().html('<tr></tr>');
			}
			//分页初始化
			showPading(jsonObject.data.page,"userPage",findPerplesByRoleId);
		}
	});
}

function undo(){
	//查询所有选择的用户进行撤销
	var $divOutdraw=$("#showCont");
	var undoUserIds= new Array();
	$.each($divOutdraw.find($('input:checkbox')),function(){
        if(this.checked){
        	undoUserIds.push($(this).val());
        }
    });
	if(undoUserIds.length<=0){
		showMessageForFail("请选择需要撤销的用户.");
		return;
	}
	 bootbox.confirm('确定要删除该用户的'+roleName+'角色吗？', function (res) {
		 if(res){
			 $.ajax({
			        type: 'post',
			        url: '/r/role/undo',
			        data: {userIds: undoUserIds.toString(), roleId: roleId},
			        dataType: 'json',
			        success: function (res) {
			            if (res.status == 1) {
			            	 showMessageForSuccess("操作成功");
			                 findPerplesByRoleId();
			            }
			        }
			    });
		 }
	 });
	
}

function beforeAddUser(id) {
    initUserCategory(true);
    var code = window.location.href.split('categoryCode=')[1];
    if (code == userCategoryIncomeAudit) {
        $('#singleOutLimit').hide();
    }
    if (code == userCategoryFinance || code.startsWith("40")) {
        $('#approveLimit').hide();
        $('#singleOutLimit').hide();
    }
    $("#id").val("");
    $("#uid").val('').prop('readonly', '');
    $("#password").val('').prop('readonly', '');
    $('#passwordGroup').removeClass('hidden');
    $('#resetPasswordGroup').removeClass('hidden').addClass('hidden');
    $('#categoryGroup input:radio[value="0"]').prop('checked", "checked');
    $('#statusGroup input:radio[value="0"]').prop('checked", "checked');
    $('#resetPasswordGroup input:radio[value="0"]').prop('checked", "checked');
    $("#modal-title").html("<h2>添加用户</h2>");
    $('#addOrEditUserForm').bootstrapValidator('resetForm', true);
    $('#editUser').modal("toggle");
}


function addOrEditUser() {
    var bootstrapValidator = $("#addOrEditUserForm").data('bootstrapValidator');
    bootstrapValidator.validate();
    if (bootstrapValidator.isValid()) {
        var initid = $('#id').val();
        var category = $('input[name="category"]:checked').val();
        category = category == userCategoryRobot ? userCategoryRobot : (initid ? category : authRequest.categoryCode);
        if (!$.trim($('#uid').val())) {
            bootbox.alert('<h4 style="text-align: center;color: red;">请填写登录账号</h4>');
            return;
        }
        if (!$("#id").val() && !_checkUserName($('#uid').val())) {
            //编辑不需要校验登陆账号
            bootbox.alert('<h5 style="text-align: center;color: red;">登录账号须数字和字母组合长度大于1,不能数字开头,不能纯数字</h5>');
            return;
        }
        if (!$.trim($('#username').val())) {
            bootbox.alert('<h4 style="text-align: center;color: red;">请填写姓名</h4>');
            return;
        }
        if (!$("#id").val() && !_checkUserName($('#username').val())) {
            bootbox.alert('<h5 style="text-align: center;color: red;">姓名须数字和字母组合长度大于3,不能数字开头,不能纯数字</h5>');
            return;
        }
        if (!$("#id").val()) {
            //编辑不需要校验密码
            if (!$.trim($('#password').val())) {
                bootbox.alert('<h4 style="text-align: center;color: red;">请填写密码</h4>');
                return;
            }
            if (!_checkPassword($('#password').val())) {
                bootbox.alert('<h5 style="text-align: center;color: red;">密码必须包含数字和字母组合，长度大于6小于20</h5>');
                return;
            }
        }
        var data = {
            id: initid,
            uid: $('#uid').val(),
            username: $('#username').val(),
            password: $('#password').val(),
            status: $('input[name="status"]:checked').val(),
            category: category,
            moneyLimit: $("#moneylimit").val(),
            auditLimit: $("#auditlimit").val(),
            resetPassword: $('input[name="resetPassword"]:checked').val()
        };
        $.ajax({
            type: "get", url: "/r/user/update", data: data, dataType: 'json', success: function (jsonObject) {
                if (jsonObject.status == 1) {
                	//如果操作成功则赋相应的角色权限.
                	 var roleIdArray = roleId;
                	$.ajax({
                        type: 'post',
                        url: '/r/role/alertRoleOfUser',
                        data: {userId: jsonObject.message, roleIdArray: roleIdArray.toString()},
                        dataType: 'json',
                        success: function (res) {
                            if (res.status == 1) {
                            	 showMessageForSuccess("操作成功");
                                 $("#editUser").modal('hide');
                                 $("#id").val('');
                                 findPerplesByRoleId();
                            }
                        }
                    });
                } else {
                    showMessageForFail("操作失败，" + jsonObject.message);
                }
            }
        });
    } else {

    }
}


function initUserCategory(isAddUser) {
    $.ajax({
        type: 'get', url: '/r/user/findUserCategory', dataType: 'json', async: false, success: function (res) {
            if (res.status == 1 && res.data) {
                var arr = [];
                arr.push('<label class="col-lg-4 control-label">用户类别</label>');
                arr.push('<div class="col-lg-8">');
                // if( request.categoryCode==userCategoryOutward){
                //     arr.push('    <label><input name="category" type="radio" class="ace input-lg" value="'+userCategoryRobot+'" /><span class="lbl bigger-120"> 机器人</span></label>');
                // }
                for (var _code in res.data) {

                    if (!isAddUser) {
                        if (_code == window.location.href.split('categoryCode=')[1]) {
                            arr.push('<label><input name="category" type="radio" class="ace input-lg" value="' + _code + '" ' + (_code == window.location.href.split('categoryCode=')[1] ? 'checked="checked"' : '') + '/><span class="lbl bigger-120">' + res.data[_code] + '</span></label>');
                        } else {
                            arr.push('<label><input name="category" type="radio" class="ace input-lg" value="' + _code + '" /><span class="lbl bigger-120">' + res.data[_code] + '</span></label>');
                        }
                    } else {
                        if (_code == window.location.href.split('categoryCode=')[1]) {
                            arr.push('<label><input name="category" type="radio" class="ace input-lg" value="' + _code + '" ' + (_code == window.location.href.split('categoryCode=')[1] ? 'checked="checked"' : '') + '/><span class="lbl bigger-120">' + res.data[_code] + '</span></label>');
                        }
                    }

                }
                arr.push('</div>');
                $('#categoryGroup').html(arr.join(''));
            }
        }
    });
}

function addUserRole(){
	$('#addUserRole_modal').modal('show');
	//获取账号
	var account=$("#addAccount").val();
	//当前页码
	var CurPage=$("#adduserPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	$.ajax({
		type:"post",
		url:"/r/role/findPerplesByRoleId",
		data:{
			"pageNo":CurPage,
			"roleId":roleId,
			"account":account,
			"type":2,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				var tr = '';
				 //小计
				 var counts = 0;
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
					 var statusStr=val.status==0?"<span class='label label-sm label-success'>启用</span>":"<span class='label label-sm label-inverse'>停用</span>";
                    tr += '<tr>'
                    	     +'<td><input type="checkBox" value="'+val.id+'" name="addUserToRole" /></td>'
                    	     +'<td>' + val.uid + '</td>'
                    	     +'<td>'+ val.username +'</td>'
                    	     +'<td>' + val.classification + '</td>'
                    	     +'<td>'+val.outLimit+'</td>'
                    	     +'<td>' + val.auditLimit + '</td>'
                    	     +'<td>' + val.createTimeStr + '</td>'
                    	     +'<td>'+statusStr+'</td>'
                    	 +'</tr>';
                    counts +=1;
                };
				 $('#addtotal_tbody').empty().html(tr);
				 var trs = '<tr>'
							 +'<td colspan="8">小计：'+counts+'</td>'
						  +'</tr>';
                $('#addtotal_tbody').append(trs);
                var trn = '<tr>'
		                	+'<td colspan="8">总计：'+jsonObject.data.page.totalElements+'</td>'
						 +'</tr>';
                $('#addtotal_tbody').append(trn);
			}else{
				$('#addtotal_tbody').empty().html('<tr></tr>');
			}
			//分页初始化
			showPading(jsonObject.data.page,"adduserPage",addUserRole);
		}
	});
	$('#addUserTitle').html("添加所选择的用户到"+roleName+"角色");
}

function addUserToRole(){
	//查询所有选择的用户进行添加到当前角色
	var $divOutdraw=$("#addUserRole_modal");
	var userIds= new Array();
	$.each($divOutdraw.find($('input:checkbox')),function(){
        if(this.checked){
        	userIds.push($(this).val());
        }
    });
	if(userIds.length<=0){
		showMessageForFail("请选择需要添加的用户.");
		return;
	}
	 bootbox.confirm('确定要添加用户到'+roleName+'角色吗？', function (res) {
		 if(res){
			 $.ajax({
			        type: 'post',
			        url: '/r/role/addUserToRole',
			        data: {userIds: userIds.toString(), roleId: roleId},
			        dataType: 'json',
			        success: function (res) {
			            if (res.status == 1) {
			            	 showMessageForSuccess("操作成功");
			            	 $('#addUserRole_modal').modal('hide');
			                 findPerplesByRoleId();
			            }
			        }
			    });
		 }
	 });
}

$.each(ContentRight['SystemRole:*'], function (name, value) {
    if (name =='SystemRole:addUserToRole:*'){
    	$("#addUserRoleButton").show();
    }
});
$(function() {
	contentRight();
	findPerplesByRoleId();
});