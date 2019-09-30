currentPageLocation = window.location.href;
var authRequest = {"pageNo": 0, "pageSize": $.session.get('initPageSize')};
var request = getRequest(); //categoryCode
authRequest.categoryCode = request.categoryCode;
function showMessageForFail(message) {
    $.gritter.add({
        title: '消息提示',
        text: message,
        sticky: false,
        time: 1000,
        speed: 500,
        position: 'bottom-right',
        class_name: 'gritter-center'// image: 'admin/clear/notif_icon.png',
    });
}

function _addRole(userId) {
    $('#rolseAdd').modal('toggle');
    $("#userIdForRole").val(userId);
    $.ajax({
        type: 'get', url: '/r/role/findByUserId', data: {"userId": userId}, dataType: 'json', success: function (res) {
            if (res.status == 1 && res.data) {
                var opt = [];
                $(res.data).each(function (i, val) {
                    opt.push('<option value="' + val.id + '" ' + ( val.selected ? 'selected = "selected"' : '') + '>' + val.name + '</option>');
                });
                $('#duallist').html(opt.join(''));
                $('#duallist').bootstrapDualListbox('refresh');
            } else {
                showMessageForFail("初始化错误,请稍后...");
            }
        }
    });
}

function _deleteUser(userId){
	bootbox.confirm("确定要删除用户及相关信息 ?", function(result) {
		if (result) {
			$.ajax({
		        type: 'post', url: '/r/role/deleteUser', data: {"userId": userId}, dataType: 'json', success: function (res) {
		            if (res.status == 1) {
		            	refreshContent(0);
		            	showMessageForSuccess(res.message);
		            } else {
		                showMessageForFail(res.message);
		            }
		        }
		    });
		}
	});
}

function _initalRoleBoxdul() {
    $('#duallist').bootstrapDualListbox({
        infoTextFiltered: '<span class="label label-purple label-lg">已过滤</span>',
        infoText: "[未选/已选]角色 {0}",
        refresh: true,
        filterPlaceHolder: '查询',
        removeAllLabel: '移除全部角色',
        moveAllLabel: '全选所有角色',
        infoTextEmpty: '空',
        infoTextFiltered: '<span class="label label-warning">查询到:</span> {0}  共有： {1}'
    }).bootstrapDualListbox('getContainer').find('.btn').addClass('btn-white btn-info btn-bold');
}

function _saveRoles() {
    var userId = $("#userIdForRole").val();
    var roleIdArray = $('#duallist').bootstrapDualListbox().val();
    $.ajax({
        type: 'post',
        url: '/r/role/alertRoleOfUser',
        data: {userId: userId, roleIdArray: roleIdArray.toString()},
        dataType: 'json',
        success: function (res) {
            if (res.status == 1) {
                showMessageForSuccess(res.message);
                refreshContent();
            }
        }
    });
}

function _editPermision(id) {
    $('#permisonId').val(id);
    _initialPan(id);
    $('#dataAdd').modal('toggle');
}

function _addPersion() {
    $('#showInfo').hide();
    var pId = $('#form-field-select-1').val();
    var levelId = $('#form-field-select-2').val();
    var userId = $('#permisonId').val();
    // if (!(pId && pId.length > 0 && pId != 'undefined') && !(levelId && levelId.length > 0 && levelId != 'undefined')) {
    //     $('#showInfo').show();
    //     return false;
    // } else {
        $('#showInfo').hide();

        $.ajax({
            type: 'post',
            url: '/r/permission/savePermission',
            data: $.extend({
                "userId": userId,
                "levelId": levelId.toString(),
                "handicapId": pId.toString()
            }, authRequest),
            dataType: 'json',
            error: function (result) {
                showMessageForFail("服务器出现异常，请稍后再试！");
            },
            success: function (data) {
                if (data) {
                    if (data.status == 1) {
                        showMessageForSuccess(data.message);
                        $('#dataAdd').prop('data-dismiss', 'modal');
                        $('#dataAdd').modal('toggle');
                    }
                } else {
                    showMessageForFail("请稍后...");
                }
            }
        });

    //}
}
// 盘口层级取消
function _clearPersion() {
    $('#dataAdd').prop('data-dismiss', 'modal');
    $('#dataAdd').modal('toggle');
}
// 初始化 层级 盘口 根据用户id初始化
function _initialPan(id) {
    var params = (currentPageLocation.split('?')[1]).split('=')[1];
    var type = parseInt(params)>400?"handicapOwn":null;
    $.ajax({
        type: "get", url: "/r/permission/find4User", data: {"userId": id,"type":null}, dataType: "json", success: function (res) {
            if (res && res.status == 1) {
                var $handicap = $('#form-field-select-1'), $level = $('#form-field-select-2'),
                    optsHandicap = [], optsLevel = [];
                if (res.data.bizHandicapList) {
                    $(res.data.bizHandicapList).each(function (i, val) {
                        optsHandicap.push('<option value="' + val.id + '" selected="selected">' + val.name + '</option>');
                    });
                }
                if (res.data.handicapList) {
                    $(res.data.handicapList).each(function (i, val) {
                        optsHandicap.push('<option value="' + val.id + '">' + val.name + '</option>');
                    });
                }
                if (res.data.bizLevelMapList) {
                    $(res.data.bizLevelMapList).each(function (i, val) {
                        optsLevel.push('<option value="' + val.id + '" selected="selected">' + '(' + val.parent + ')' + val.name + '</option>');
                    });
                }
                if (res.data.levelMapList) {
                    $(res.data.levelMapList).each(function (i, val) {
                        optsLevel.push('<option value="' + val.id + '">' + '(' + val.parent + ')' + val.name + '</option>');
                    });
                }
                $handicap.empty().html(optsHandicap.join(''));
                $level.empty().html(optsLevel.join(''));
                _initialSelect();
            } else {
                showMessageForFail(res.message);
                return false;
            }
        }
    });
}
/**
 * 盘口chang事件
 */
$('#form-field-select-1').change(
    function () {
        var vals = $('#form-field-select-1').val();
        var selectedHandicapIds = [];
        if (vals && vals.length > 0) {
            $('#form-field-select-1 option:selected').each(function () {
                if ($(this).val()) {
                    selectedHandicapIds.push($(this).val());
                }
            });
            _initialLevel(selectedHandicapIds);
        } else {
            $('#form-field-select-1').empty();
            $('#form-field-select-2').empty();
            // 初始化
            $.ajax({
                type: 'get',
                url: '/r/permission/initial',
                data: $.extend({
                    "enabled": 1
                }, authRequest),
                dataType: 'json',
                error: function (result) {
                    showMessageForFail('服务器出现异常，请稍后再试！');
                },
                success: function (data) {
                    if (data) {
                        if (data.status == 1) {
                            var opt = '';
                            $(data.data).each(
                                function (i, val) {
                                    opt += '<option value="' + val.id
                                        + '">' + val.name
                                        + '</option>';
                                });
                            $('#form-field-select-1').html(opt);
                            _initialSelect();
                            //$('#form-field-select-1').multiselect();

                        }
                    } else {
                        showMessageForFail('网络延迟，请稍后...');
                        return false;
                    }
                }
            });
        }
    });
/**
 * 获取层级信息
 * /r/permission/find4User  只有此处传入 handicapIdArray 选中的盘口id
 * 根据选中的盘口id查询层级信息 如果用户之前有层级则显示选中没有则不选中层级
 * @param ids
 * @param name
 * @private
 */
function _initialLevel(ids) {
    if (!ids) {
        return;
    }
    $.ajax({
        type: 'post',
        url: '/r/permission/find4User',
        dataType: "json",
        data: {userId: $('#permisonId').val(), handicapIdArray: ids.toString()},
        success: function (res) {
            if (res.status == 1 && res.data) {
                var $level = $('#form-field-select-2'), optsLevel = [];
                if (res.data.result) {
                    $(res.data.result).each(function (i, val) {
                        if(val.selected=='yes'){
                            optsLevel.push('<option value="' + val.id + '" selected="selected">' + '(' + val.parent + ')' + val.name + '</option>');
                        }else{
                            optsLevel.push('<option value="' + val.id + '">' + '(' + val.parent + ')' + val.name + '</option>');
                        }
                    });
                }
                $level.empty().html(optsLevel.join(''));
                _initialSelect();
            } else {
                showMessageForFail('请稍后...');
            }
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

function beforeEditUser(id) {
    initUserCategory(false);
    var code = window.location.href.split('categoryCode=')[1];
    if (code == userCategoryIncomeAudit) {
        $('#singleOutLimit').hide();
    }
    if (code == userCategoryFinance || code.startsWith("40")) {
        $('#approveLimit').hide();
        $('#singleOutLimit').hide();
    }
    $('#editUser').modal("toggle");
    $('#passwordGroup').removeClass('hidden').addClass('hidden');
    $('#resetPasswordGroup').removeClass('hidden');
    $('#categoryGroup input:radio[value="0"]').prop('checked", "checked');
    $('#statusGroup input:radio[value="0"]').prop('checked", "checked');
    $('#resetPasswordGroup input:radio[value="0"]').prop('checked", "checked');
    $.ajax({
        type: "post", url: "/r/user/findById", data: {"id": id}, dataType: 'json', success: function (jsonObject) {
            if (jsonObject.status == 1) {
                $('#addOrEditUserForm').bootstrapValidator('resetForm', true);
                $("#id").val(jsonObject.data.id == id ? id : jsonObject.data.id);
                $('#permisonId').val(id);
                $("#uid").val(jsonObject.data.uid).prop("readonly", "readonly");
                $("#password").val(jsonObject.data.password).prop("readonly", "readonly");
                $("#username").val(jsonObject.data.username);
                $("#auditlimit").val(jsonObject.data.auditLimit);
                $("#moneylimit").val(jsonObject.data.moneyLimit);
                $("#modal-title").html("<h2>编辑用户</h2>");
                if (jsonObject.data.status == 0) {
                    $('#statusGroup input:radio[value="0"]').prop("checked", "checked");
                } else if (jsonObject.data.status == 1) {
                    $('#statusGroup input:radio[value="1"]').prop("checked", "checked");
                }
                $('#categoryGroup input:radio[value="' + jsonObject.data.category + '"]').prop("checked", "checked");
            } else {
                showMessageForFail(jsonObject.message);
            }
        }
    });
}
(function () {
    var code = window.location.href.split('categoryCode=')[1];
    if (code == userCategoryIncomeAudit) {
        $('#singleOutLimit').hide();
        $('#addOrEditUserForm').bootstrapValidator({
            message: 'This value is not valid',
            excluded: [':disabled'],
            feedbackIcons: {
                valid: 'glyphicon glyphicon-ok',
                invalid: 'glyphicon glyphicon-remove',
                validating: 'glyphicon glyphicon-refresh'
            },
            fields: {
                uid: {
                    validators: {
                        notEmpty: {message: '登录账号不能为空！'},
                        stringLength: {min: 1, max: 45, message: '登录账号长度必须在1到45之间！'}
                    }
                },
                username: {
                    validators: {
                        notEmpty: {message: '姓名不能为空！'},
                        stringLength: {min: 1, max: 32, message: '姓名长度必须在1到32之间！'}
                    }
                },
                password: {
                    validators: {
                        notEmpty: {message: '密码不能为空！'},
                        stringLength: {min: 1, max: 64, message: '密码长度必须在1到64之间！'}
                    }
                },
                auditLimit: {
                    validators: {
                        notEmpty: {message: '最大审核额度不能为空'},
                        numeric: {message: '必须是数字'},
                        stringLength: {min: 1, max: 200, message: '最大审核额度长度必须在1到200之间！'}
                    }
                }
            }
        });
    }
    if (code == userCategoryFinance || code.startsWith("40")) {
        $('#approveLimit').hide();
        $('#singleOutLimit').hide();
        $('#addOrEditUserForm').bootstrapValidator({
            message: 'This value is not valid',
            excluded: [':disabled'],
            feedbackIcons: {
                valid: 'glyphicon glyphicon-ok',
                invalid: 'glyphicon glyphicon-remove',
                validating: 'glyphicon glyphicon-refresh'
            },
            fields: {
                uid: {
                    validators: {
                        notEmpty: {message: '登录账号不能为空！'},
                        stringLength: {min: 1, max: 45, message: '登录账号长度必须在1到45之间！'}
                    }
                },
                username: {
                    validators: {
                        notEmpty: {message: '姓名不能为空！'},
                        stringLength: {min: 1, max: 32, message: '姓名长度必须在1到32之间！'}
                    }
                },
                password: {
                    validators: {
                        notEmpty: {message: '密码不能为空！'},
                        stringLength: {min: 1, max: 64, message: '密码长度必须在1到64之间！'}
                    }
                }
            }
        });
    }
})();


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
                    showMessageForSuccess("操作成功");
                    $("#editUser").modal('hide');
                    $("#id").val('');
                    refreshContent();
                } else {
                    showMessageForFail("操作失败，" + jsonObject.message);
                }
            }
        });
    } else {

    }
}

/*
 * 往表格添加内容
 */
function showContent(data) {
    var cont = '<thead><tr><th>帐号</th><th>姓名</th><th>分类<th>角色</th><th>出款额度</th><th>审核额度</th><th>创建时间</th><th>状态</th><th  class="center">操作</th></tr></thead>' +
        '<tbody id="table_body"></tbody>';
    var code = window.location.href.split('categoryCode=')[1];
    if (code == userCategoryIncomeAudit) {
        $('#singleOutLimit').hide();
        cont = '<thead><tr><th>帐号</th><th>姓名</th><th>分类<th>角色</th><th>审核额度</th><th>创建时间</th><th>状态</th><th  class="center" style="width:30%;">操作</th></tr></thead>' +
            '<tbody id="table_body"></tbody>';
    }
    if (code == userCategoryFinance || code.startsWith("40")) {
        $('#approveLimit').hide();
        $('#singleOutLimit').hide();
        cont = '<thead><tr><th>帐号</th><th>姓名</th><th>分类<th>角色</th><th>创建时间</th><th>状态</th><th  class="center">操作</th></tr></thead>' +
            '<tbody id="table_body"></tbody>';
    }
    $("#simple-table-userDetail").html(cont);
    var content = "";
    for (var o in data) {
        content += '<tr><td>' + data[o].uid + '</td>' +
            '<td>' + data[o].username + '</td>' +
            '<td style="width: 10%">' + data[o].categoryName + '</td>' +
            '<td style="width: 10%">' + (data[o].role == null ? '' : data[o].role) + '</td>';
        if (code == userCategoryIncomeAudit) {
            //入款人员
            content += '<td>' + data[o].auditLimit + '</td>';
        } else if (code == userCategoryFinance || code.startsWith("40")) {
            //财务  客服人员
            content += '';
        } else {
            content += '<td>' + data[o].moneyLimit + '</td>' +
                '<td>' + data[o].auditLimit + '</td>';
        }
        content += '<td>' + timeStamp2yyyyMMddHHmmss(data[o].createTime) + '</td>' +
            '<td>' + (data[o].status == 0 ? '<span class="label label-sm label-success">启用</span>' : '<span class="label label-sm label-inverse">停用</span>') + '</td>' +
            '<td style="width: 21%" class="center">' +
            '<div class="hidden-sm hidden-xs btn-group" >';
        if (code == userCategoryIncomeAudit || code == userCategoryOutward) {
        	//入款人员有区域设置按钮
        	content+='<button class="btn btn-xs btn-white btn-default btn-bold contentRight" contentRight="SystemUser:SettingZone:*" onclick="showModalSettingZone(' + data[o].id + ');"><i class="ace-icon fa fa-globe bigger-120">区域</i></button>';
        }
        content+='<button class="btn btn-xs btn-white btn-default btn-bold contentRight" contentRight="SystemUser:Edit:*" onclick="beforeEditUser(' + data[o].id + ');"><i class="ace-icon fa fa-pencil bigger-120">编辑</i></button>' +
            '<button class="btn btn-xs btn-white btn-danger btn-bold contentRight" contentRight="SystemUser:Permission:*" onclick="_editPermision(' + data[o].id + ');"><i class="ace-icon fa  fa-lock bigger-120">权限</i></button>' +
            '<button class="btn btn-xs btn-white btn-success btn-bold contentRight" contentRight="SystemUser:Role:*" onclick="_addRole(' + data[o].id + ');"><i class="ace-icon fa fa-street-view bigger-120">角色</i></button>' +
            '<button class="btn btn-xs btn-white btn-error btn-bold contentRight" contentRight="SystemUser:delete:*" onclick="_deleteUser(' + data[o].id + ');"><i class="ace-icon fa fa-trash bigger-120 red">删除</i></button>' +
            '</div>' +
            '</td>' +
            '</tr>';
    }
    $("#simple-table-userDetail").find("#table_body").empty().html(content);
    contentRight();
}

function refreshContent(pageNo) {
    authRequest.pageNo = (pageNo && pageNo > 0 || pageNo == 0) ? pageNo : ($("#userPage .Current_Page").text() ? $("#userPage .Current_Page").text() - 1 : 0);
    authRequest.pageNo = authRequest.pageNo < 0 ? 0 : authRequest.pageNo;
    authRequest.roleId = $("select[name='roleSelect']").val();
    if (window.sessionStorage.getItem("userNameLikeForStorage") && !$("input[name='search_LIKE_uid']").val()) {
        authRequest.search_LIKE_uid = window.sessionStorage.getItem("userNameLikeForStorage");
    } else {
        authRequest.search_LIKE_uid = $("input[name='search_LIKE_uid']").val();
    }
    var hrefArray = window.location.href.split("?");
    authRequest.categoryCode = hrefArray[2] ? hrefArray[2].split("=")[1] : hrefArray[1].split("=")[1];
    authRequest.pageSize = $.session.get('initPageSize');
    $.ajax({
        type: "get", url: "/r/user/list", data: authRequest, dataType: 'json', success: function (res) {
            if (res.status == 1) {
                showContent(res.data);
                showPading(res.page, "userPage", refreshContent, null, true, false);
            } else {
                showMessageForFail(res.message);
            }
        },
    });
}
/** 区域设置 */
var showModalSettingZone=function(userId){
    var $div = $("#updateUserZone4clone").clone().attr("id", "updateUserZone");
    $div.modal("toggle");
    $div.find("#userId").val(userId);
    var code = window.location.href.split('categoryCode=')[1];
    if(code == userCategoryOutward){
        $div.find("#zone_category").html("出款分组设置");
    }
    var data=findByUserIdAndPropertyKey(userId,"HANDICAP_ZONE_MANILA0_TAIWAN1");
    if(data){
    	//有存储过用户分类
    	$div.find("[name=HANDICAP_ZONE_MANILA0_TAIWAN1][value="+data.propertyValue+"]").attr("checked","checked");
    }
	$div.on('hidden.bs.modal', function () {
	    //关闭窗口清除内容;
	    $div.remove();
	});
}
var doSettingZone=function(){
	var $div = $("#updateUserZone");
	var propertyValue=$div.find("[name=HANDICAP_ZONE_MANILA0_TAIWAN1]:checked").val();
	if(propertyValue!="0"&&propertyValue!="1"){
        showMessageForFail("请先选择再保存");
        return;
	}
	bootbox.confirm("确定保存 ?", function (result) {
		if (result) {
		    $.ajax({
		        type: "POST",
		        dataType: 'JSON',
		        url: '/r/user/saveByUserIdAndPropertyKey',
		        async: false,
		        data: {
		        	userId:$div.find("#userId").val(),
		        	propertyKey:"HANDICAP_ZONE_MANILA0_TAIWAN1",
		        	propertyValue:propertyValue,
		        	propertyName:"用户管理，入款用户的区域分类"
		        },
		        success: function (jsonObject) {
		            if (jsonObject.status == 1) {
		                showMessageForSuccess("保存成功");
		                $div.modal("toggle");
		            } else {
		                showMessageForFail("保存失败：" + jsonObject.message);
		            }
		        }
		    });
	  }
    });
}
function _initialSelect() {
    $('.multiselect').multiselect('destroy').multiselect({
        nonSelectedText: '请选择',
        filterPlaceholder: '请选择',
        selectAllText: '全选',
        nSelectedText: '已选',
        nonSelectedText: '请选择',
        allSelectedText: '全选',
        numberDisplayed: 5,
        enableFiltering: true,
        includeSelectAllOption: true,
        enableFiltering: true,
        enableHTML: true,
        buttonClass: 'btn btn-white btn-primary',
        templates: {
            button: '<button style="width: 500px;" type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>',
            ul: '<ul class="multiselect-container dropdown-menu"></ul>',
            filter: '<li class="multiselect-item filter"><div class="input-group"><span class="input-group-addon"><i class="fa fa-search"></i></span><input class="form-control multiselect-search" type="text"></div></li>',
            filterClearBtn: '<span class="input-group-btn"><button class="btn btn-default btn-white btn-grey multiselect-clear-filter" type="button"><i class="fa fa-times-circle red2"></i></button></span>',
            li: '<li><a tabindex="0"><label></label></a></li>',
            divider: '<li class="multiselect-item divider"></li>',
            liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
        }
    });
    $(document).one('ajaxloadstart.page', function (e) {
        $('.multiselect').multiselect('destroy');
    });
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

$.ajax({
    type: 'get',
    url: '/r/role/findByUserId',
    data: {userId: 88888888},
    dataType: 'json',
    success: function (res) {
        if (res.status == 1 && res.data) {
            var opt = [];
            opt.push('<option selected="selected" value="">请选择</option>');
            $(res.data).each(function (i, val) {
                opt.push('<option value="' + val.id + '">' + val.name + '</option>');
            });
            $('#roleSelect').html(opt.join(''));
        }
    }
});

bootbox.setLocale("zh_CN");
refreshContent(0);
contentRight();
_initialSelect();
_initalRoleBoxdul();
$('#rolseAdd').find('div.box1').find('span.info').empty().text('未选角色');
$('#rolseAdd').find('div.box2').find('span.info').empty().text('已选角色');
