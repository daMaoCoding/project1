currentPageLocation = window.location.href;
/**
 * 获取参数的键值对对象。
 * 
 * @returns {Object}
 */
var getParam = function() {
	try {
		var url = window.location.href;
		var position = url.lastIndexOf('?');
		var result = decodeURIComponent(url).substring((position + 1), url.length);
		var keyValue = result.split("&");
		var obj = {};
		for (var i = 0; i < keyValue.length; i++) {
			var item = keyValue[i].split("=");
			obj[item[0]] = item[1];
		}
		return obj;
	} catch (e) {
		console.warn("没有参数!");
	}
};

// 初始化时候后台获取异步加载数据 json格式的对象
var zNodes = [ {
	id : 1,
	pId : 0,
	name : "入款管理",
	open : true,
	isParent : true
},

{
	id : 11,
	pId : 1,
	name : "入款审核"
}, {
	id : 111,
	pId : 11,
	name : "公司入款"
}, {
	id : 112,
	pId : 11,
	name : "第三方入款"
},

{
	id : 1111,
	pId : 111,
	name : "正在匹配"
}, {
	id : 1112,
	pId : 111,
	name : "未匹配"
}, {
	id : 1113,
	pId : 111,
	name : "已匹配"
}, {
	id : 1114,
	pId : 111,
	name : "已取消"
}, {
	id : 1121,
	pId : 112,
	name : "未对账"
}, {
	id : 1122,
	pId : 112,
	name : "已对账"
},

{
	id : 12,
	pId : 1,
	name : "账号管理"
}, {
	id : 121,
	pId : 12,
	name : "公司"
}, {
	id : 122,
	pId : 12,
	name : "第三方"
}, {
	id : 123,
	pId : 12,
	name : "下发卡"
}, {
	id : 1211,
	pId : 121,
	name : "银行卡"
}, {
	id : 1212,
	pId : 121,
	name : "微信"
}, {
	id : 1213,
	pId : 121,
	name : "支付宝"
},

{
	id : 13,
	pId : 1,
	name : "下发管理"
}, {
	id : 131,
	pId : 13,
	name : "银行卡"
}, {
	id : 132,
	pId : 13,
	name : "支付宝"
}, {
	id : 133,
	pId : 13,
	name : "下发卡"
}, {
	id : 133,
	pId : 13,
	name : "第三方"
}, {
	id : 133,
	pId : 13,
	name : "微信"
},

{
	id : 2,
	pId : 0,
	name : "出款管理",
	isParent : true
}, {
	id : 21,
	pId : 2,
	name : "出款审核"
}, {
	id : 211,
	pId : 21,
	name : "正在审核"
}, {
	id : 212,
	pId : 21,
	name : "完成审核"
}, {
	id : 213,
	pId : 21,
	name : "已拒绝"
}, {
	id : 214,
	pId : 21,
	name : "已取消"
}, {
	id : 215,
	pId : 21,
	name : "主管处理"
},

{
	id : 22,
	pId : 2,
	name : "出款审核汇总"
}, {
	id : 221,
	pId : 22,
	name : "待处理"
}, {
	id : 222,
	pId : 22,
	name : "待审核"
}, {
	id : 223,
	pId : 22,
	name : "完成审核"
}, {
	id : 224,
	pId : 22,
	name : "已拒绝"
}, {
	id : 225,
	pId : 22,
	name : "已取消"
},

{
	id : 23,
	pId : 2,
	name : "出款任务"
}, {
	id : 231,
	pId : 23,
	name : "正在出款"
}, {
	id : 232,
	pId : 23,
	name : "机器出款"
}, {
	id : 233,
	pId : 23,
	name : "完成出款"
}, {
	id : 234,
	pId : 23,
	name : "失败任务"
},

{
	id : 24,
	pId : 2,
	name : "出款任务汇总"
},

{
	id : 25,
	pId : 2,
	name : "账号管理"
}, {
	id : 251,
	pId : 25,
	name : "在用"
}, {
	id : 252,
	pId : 25,
	name : "可用"
}, {
	id : 253,
	pId : 25,
	name : "空闲"
}, {
	id : 254,
	pId : 25,
	name : "停用"
}, {
	id : 255,
	pId : 25,
	name : "全部"
}, {
	id : 256,
	pId : 25,
	name : "第三方"
},

{
	id : 3,
	pId : 0,
	name : "财务管理",
	isParent : true
}, {
	id : 31,
	pId : 3,
	name : "出款明细"
}, {
	id : 32,
	pId : 3,
	name : "入款明细"
}, {
	id : 33,
	pId : 3,
	name : "中转明细"
}, {
	id : 34,
	pId : 3,
	name : "出入款明细"
}, {
	id : 35,
	pId : 3,
	name : "亏损统计"
}, {
	id : 36,
	pId : 3,
	name : "余额明细"
},

{
	id : 311,
	pId : 31,
	name : "按盘口统计"
}, {
	id : 312,
	pId : 31,
	name : "按账号统计"
},

{
	id : 321,
	pId : 32,
	name : "银行卡"
}, {
	id : 322,
	pId : 32,
	name : "微信"
}, {
	id : 323,
	pId : 32,
	name : "支付宝"
}, {
	id : 324,
	pId : 32,
	name : "第三方"
},

{
	id : 331,
	pId : 33,
	name : "入款银行卡中转"
}, {
	id : 332,
	pId : 33,
	name : "入款支付宝中转"
}, {
	id : 333,
	pId : 33,
	name : "入款微信中转"
}, {
	id : 334,
	pId : 33,
	name : "入款第三方中转"
}, {
	id : 335,
	pId : 33,
	name : "下发卡中转"
}, {
	id : 336,
	pId : 33,
	name : "备用卡中转"
},

{
	id : 4,
	pId : 0,
	name : "系统管理",
	isParent : true
}, {
	id : 41,
	pId : 4,
	name : "用户管理"
}, {
	id : 42,
	pId : 4,
	name : "角色管理"
}, {
	id : 43,
	pId : 4,
	name : "系统设置"
} ];
var setting = {
	view : {
		// addHoverDom: addHoverDom,
		// removeHoverDom: removeHoverDom,
		selectedMulti : false,
		dblClickExpand : true
	},
	check : {
		enable : true,
        autoCheckTrigger: true,
        chkDisabledInherit: true,
        radioType :"level"
	},
	data : {
		simpleData : {
			enable : true
		}
	},
	edit : {
		enable : false,
		showRenameBtn : false,
		showRemoveBtn : false,
		removeTitle : "删除菜单权限"
	},
	callback : {

	}
};

var newCount = 1;
function addHoverDom(treeId, treeNode) {
	var sObj = $("#" + treeNode.tId + "_span");
	if (treeNode.editNameFlag || $("#addBtn_" + treeNode.tId).length > 0)
		return;
	var addStr = "<span class='button add' id='addBtn_" + treeNode.tId
			+ "' title='新增菜单权限' onfocus='this.blur();'></span>";
	if (treeNode.pId != null && treeNode.pId != '0') {
		sObj.after(addStr);
		var btn = $("#addBtn_" + treeNode.tId);
		if (btn)
			btn.unbind().bind("click", function() {
				var zTree = $.fn.zTree.getZTreeObj("treeDemo");
				zTree.addNodes(treeNode, {
					id : (100 + newCount),
					pId : treeNode.id,
					name : "新菜单" + (newCount++)
				});
				return false;
			});
	}
};
function removeHoverDom(treeId, treeNode) {
	$("#addBtn_" + treeNode.tId).unbind().remove();
};

/**
 * 初始化菜单权限页面--根据角色id查询
 */
function _initialMenu(roleId) {
	if (roleId) {
		$.ajax({
			type : 'get',
			url : '/r/menu/initial',
			data :  {"roleId" : roleId},
			dataType : 'json',
			error : function(result) {
				showMessageForFail("服务器出现异常，请稍后再试！");
			},
			success : function(res) {
				if (res) {
					if (res.status == 1) {
						// zNodes = eval($(data.data));
						// zNodes = res.data;
						$.fn.zTree.init($("#treeDemo"), setting, eval(res.data));
					}
				} else {
					showMessageForFail('网络延迟，请稍后...');
					return false;
				}
			}
		});
	}
}
/**
 * 保存菜单权限
 */
function _saveMenuPermision() {
	var treeObj = $.fn.zTree.getZTreeObj("treeDemo");
	var nodes = treeObj.getCheckedNodes(true);
	var v = "";
	var ids = "";
	var roleId = $("#roleId").val();
	for (var i = 0; i < nodes.length; i++) {
		ids += nodes[i].id + ",";// 获取选中节点的值
		// ids.push(nodes[i].id); //获取选中节点的值
	}
	if (ids) {
		ids = ids.substring(0, ids.length - 1);
	}
	/*
	 * if(ids && ids.length>0) {
	 */

	$.ajax({
		type : 'post',
		url : '/r/menu/save',
		data :{
            "menuIdArr" : ids,
            "roleId" : roleId},
		dataType : 'json',
		error : function(result) {
			showMessageForFail("服务器出现异常，请稍后再试！");
		},
		success : function(res) {
			if (res) {
				if (res.status == 1) {
					// 保存成功提示
					showMessageForSuccess(res.message);
					// 刷新页面
					_initialMenu(roleId);

				} else {
					showMessageForFail(res.message);
					return false;
				}
			} else {
				showMessageForFail('网络繁忙，请稍后...');
				return false;
			}
		}
	});

	/*
	 * } else { bootbox.alert('请选择菜单'); return false; }
	 */

}



var param = getParam();
var val = param["roleId"];
var roleName = param["roleName"];
$('#menuAuthorityHead').text('菜单权限设置---'+roleName);
$("#roleId").val(val);
// 初始化加载
var roleId = $("#roleId").val();
_initialMenu(roleId);