
//国际化初始化例子
var searchComponent='<h3 id="inFrom" class="row header smaller lighter blue">\
			<div class="col-sm-9">\
				<form class="form-inline">\
					<span class="label label-lg label-pink arrowed-right">'+intl.handicap+'</span>\
					<select class="chosen-select form-control" name="search_EQ_handicapId" style="min-width: 98px;">\
						<option value="" selected="selected" handicapCode>'+intl.all+'</option>\
					</select>\
					<span class="label label-lg label-primary arrowed-right">'+intl.hierarchy+'</span>\
					<select class="chosen-select form-control" name="search_EQ_LevelId" style="min-width: 98px;">\
						<option value="" selected="selected" LevelCode>'+intl.all+'</option>\
					</select>\
					<span class="label label-lg label-purple arrowed-right">'+intl.accountNumber+'</span>\
					<input type="text" style="height:32px" name="search_LIKE_account" class="input-medium" placeholder="'+intl.accountNumber+'" />\
					<span class="label label-lg label-yellow  arrowed-right">'+intl.state+'</span>\
					<label class="inline"><input type="checkbox" name="search_IN_status_type" class="ace" value="1"  accountType="3" checked="checked"/><span class="lbl">'+intl.normal+'</span></label>\
					<label class="inline"><input type="checkbox" name="search_IN_status_type" class="ace" value="3"  accountType="3" checked="checked"/><span class="lbl">'+intl.suspend+'</span></label>\
					<label class="inline"><input type="checkbox" name="search_IN_status_type" class="ace" value="-1"  accountType="3" checked="checked"/><span class="lbl">'+intl.abnormal+'</span></label>\
				</form>\
			</div>\
			<span class="col-sm-3">\
				<label class="pull-right inline">\
					<button id="search-button" class="btn btn-xs btn-white btn-info btn-bold"><i class="ace-icon fa fa-search bigger-100 green"></i>查询</button>\
				</label>\
			</span>\
		</h3>';
		var incomeAsignBank='',flag=false,addOne=7;
		if(window.location.hash!='#/IncomeAsignBank:*'){
			flag=true;
			addOne=8;	
			incomeAsignBank='<th style="width:86px">提现对账</th>';
		}	
var tableHeader='<table id="dynamic-table" class="table table-striped table-bordered table-hover">\
			<thead><tr>\
			<th>'+intl.id+'</th>\
			<th>'+intl.handicap+'</th>\
			<th>'+intl.hierarchy+'</th>\
			<th>'+intl.accountNumber+'</th>\
			<th>'+intl.accountHolder+'</th>\
			<th>'+intl.state+'</th>\
			<th>'+intl.collectToday+'</th>\
			<th>'+intl.systemBalance+'</th>\
			<th>'+intl.bankBalance+'</th>'+incomeAsignBank+'\
			<th style="width:86px">'+intl.downStat+'</th>\
			<th>'+intl.operation+'</th>\
			</tr></thead>\
			<tbody></tbody>\
		</table>';
		
var paging='<div class="message-footer clearfix">\
			<div class="pull-left">\
				<i class="ace-icon fa fa-circle bigger-100 green" title="已确认" style="cursor:pointer"></i><span>已确认&nbsp;&nbsp;&nbsp;&nbsp;</span>\
				<i class="ace-icon fa  fa-circle-o  bigger-100 orange" title="未确认" style="cursor:pointer"></i><span>未确认&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>\
			</div>\
			<div id="paging_total_elements" class="pull-left"> 总共 0 条记录 </div>\
			<input type="hidden" id="pagingPrevious" /><input type="hidden" id="pagingNext" /><input type="hidden" id="pagingLast"  />\
			<div class="pull-right">\
				<div id="paging_from_to" class="inline middle"> 第 0 / 0 页</div>&nbsp; &nbsp;\
				<ul class="pagination middle">\
					<li id="paging_first" ><a href="javascript:refreshContent(0);"><i class="ace-icon fa fa-step-backward middle"></i></a></li>\
					<li id="paging_previous" ><a href="javascript:refreshContent($('+"#pagingPrevious"+').val());"><i class="ace-icon fa fa-caret-left bigger-140 middle"></i></a></li>\
					<li><span><input id="paging_page_no" value="1" maxlength="9" type="text" /></span></li>\
					<li id="paging_next"><a href="javascript:refreshContent($('+"#pagingNext"+').val());" ><i class="ace-icon fa fa-caret-right bigger-140 middle"></i></a></li>\
					<li id="paging_last" ><a href="javascript:refreshContent($('+"#pagingLast"+').val());"><i class="ace-icon fa fa-step-forward middle"></i></a></li>\
				</ul>\
			</div>\
		</div>'
$uiViewContent=$(".ui-view>div.row>div");
$uiViewContent.html(searchComponent+tableHeader+paging);
//国计划初始化例子

var authRequest = {"pageNo": 0,"pageSize":$.session.get('initPageSize')};

function showContent(data) {
	var $tbody = $dynamicTable.html('');
	var totalBalanceBySys = 0;
	var totalBalanceByBank = 0;
	var strHtml = '',totalList=0;
	for(var index in data) {
		totalList++;
		var item = data[index];
		totalBalanceBySys = totalBalanceBySys + item.balance;
		totalBalanceByBank = totalBalanceByBank + item.bankBalance;

		var _td0 = "<td><span>" + item.id + "</span></td>";
		var _td1 = "<td><span>" + item.handicapName + "</span></td>";
		var _td2 = "<td><span>" + item.levelNameToGroup + "</span></td>";
		var _td3 = "<td><span>" + item.account + "</span></td>";
		var _td4 = "<td><span>" + item.owner + "</span></td>";

		var _td5Class = '';
		if(item.status == accountStatusFreeze || item.status == accountStatusStopTemp) {
			_td5Class = 'label-danger';
		} else {
			_td5Class = 'label-success';
		}
		var _td5 = "<td><span class='label label-sm " + _td5Class + "'>" + item.statusName + "</span></td>";
		var _td6 = "<td><span>" + 0 + "</span><i class='fa fa-flag red bigger-130'></i></td>";

		!item.balance?item.balance=0:null;
		!item.bankBalance?item.bankBalance=0:null;
		var _td7 = "<td><span>" + item.balance + "</span></td>"; //系统余额
		var _td8 = "<td><span>" + item.bankBalance + "</span></td>"; //银行余额
		//小技巧  如果觉得字符串太长  可以用	\隔开 直接换行，这样就可以直接拼接下来了。
		var _td9 = '<td>\
				<span><a href="#" target="_self">\
				<i class="ace-icon fa fa-circle bigger-100 green" title="已确认" style="cursor: pointer;"></i>\
				<span>13</span></a><span>/</span>\
				<a href="" target="_self">\
				<i class="ace-icon fa fa-circle-o bigger-100 orange" title="未确认" style="cursor: pointer;"></i>\
				<span>14</span></a></span>\
			</td>';

		if(flag){
			_td9= '<td><span><a href="#/enchashmentCheck:*?account='+item.id+'&match=Y" target="_self">\
					<i class="ace-icon fa fa-circle bigger-100 green" title="已确认" style="cursor: pointer;width:\'16px\'"></i>\
					<span>13</span></a><span>/</span>\
					<a  href="#/enchashmentCheck:*?account='+item.id+'" target="_self">\
					<i class="ace-icon fa fa-circle-o bigger-100 orange" title="未确认" style="cursor: pointer;"></i>\
					<span>14</span></a></span>\
				</td>'+_td9;
		}

		var _td10BtnTransfer = '<button class="btn btn-xs btn-white btn-warning btn-bold orange" onclick="showExchangeBalanceModal(' + item.id + ',' + item.type + ')"><i class="ace-icon fa fa-exchange bigger-100"></i><span>转账</span></button>';
		var _td10BtnFlowing = '<button class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水</span></button>';
		var _td10BtnRecord = '<button class="btn btn-xs btn-white btn-primary btn-bold"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>记录</span></button>';
		var _td10 = '<td>' + _td10BtnTransfer + _td10BtnFlowing + _td10BtnRecord + '</td>';

		strHtml += "<tr>" + _td0 + _td1 + _td2 + _td3 + _td4 + _td5 + _td6 + _td7 + _td8 + _td9 + _td10 + "</tr>";
	}

	var _trSubtotal='<tr>\
		<td colspan="'+addOne+'"><span>小计：'+totalList+' 条</span></td>\
		<td bgcolor="#579EC8" style="color: white;"><span>小计：'+totalBalanceBySys+'</span></td>\
		<td bgcolor="#579EC8" style="color: white;"><span>'+totalBalanceByBank+'</span></td>\
		<td colspan="2"></td>\
	</tr>';
	var _trSuptotal='<tr>\
		<td colspan="'+addOne+'"><span>总计：'+totalList+' 条</span></td>\
		<td bgcolor="#D6487E" style="color: white;"><span>小计：'+totalBalanceBySys+'</span></td>\
		<td bgcolor="#D6487E" style="color: white;"><span>'+totalBalanceByBank+'</span></td>\
		<td colspan="2"></td>\
	</tr>';
	strHtml +=_trSubtotal+_trSuptotal;
	$dynamicTable.html(strHtml);
}

/*
 * 刷新分页信息
 */
function refreshPaging(paging) {
	$("#paging_page_no").val(paging.pageNo);
	$("#paging_total_elements").html("总共 " + paging.totalElements + " 条");
	$("#paging_from_to").html("第 " + paging.pageNo + " / " + paging.totalPages + " 页");
	$("#pagingPrevious").val(paging.previousPageNo);
	$("#pagingNext").val(paging.nextPageNo);
	$("#pagingLast").val(paging.totalPages - 1);
	if(paging.hasNext) {
		$("#paging_next").removeClass("disabled");
		$("#paging_last").removeClass("disabled");
	} else {
		$("#paging_next").addClass("disabled");
		$("#paging_last").addClass("disabled");
	}
	if(paging.hasPrevious) {
		$("#paging_previous").removeClass("disabled");
		$("#paging_first").removeClass("disabled");
	} else {
		$("#paging_previous").addClass("disabled");
		$("#paging_first").addClass("disabled");
	}
}

// 调用AJAX 请求渲染 tbody
function refreshContent(pageNo) {
	authRequest.pageNo = pageNo;
	if(!authRequest.typeToArray) {
		authRequest.typeToArray = [accountTypeInAli].toString();
	}
	if(!authRequest.statusToArray) {
		authRequest.statusToArray = [accountStatusNormal, accountStatusFreeze, accountStatusStopTemp].toString();
	}
	authRequest.search_IN_handicapId=handicapId_list.toString();
    authRequest.pageSize=$.session.get('initPageSize');
	$.ajax({
		type: "get",
		url: API.r_account_list,
		data: authRequest,
		dataType: 'json',
		success: function(jsonObject) {
			if(jsonObject.status == 1) {
				showContent(jsonObject.data);
				refreshPaging(jsonObject.page);
			} else {
				bootbox.alert(jsonObject.message);
			}
		},
		error: function(result) {
			bootbox.alert(result);
		}
	});
}

// 调用AJAX 请求渲染 tbody
function refreshContent(pageNo) {
	authRequest.pageNo = pageNo;
	if(!authRequest.typeToArray) {
		authRequest.typeToArray = [accountType].toString();
	}
	if(!authRequest.statusToArray) {
		authRequest.statusToArray = [accountStatusNormal, accountStatusFreeze, accountStatusStopTemp].toString();
	}
	authRequest.search_IN_handicapId=handicapId_list.toString();
    authRequest.pageSize=$.session.get('initPageSize');
	$.ajax({
		type: "get",
		url: API.r_account_list,
		data: authRequest,
		dataType: 'json',
		success: function(jsonObject) {
			console.log(jsonObject);
			if(jsonObject.status == 1) {
				showContent(jsonObject.data);
				refreshPaging(jsonObject.page);
			} else {
				bootbox.alert(jsonObject.message);
			}
		},
		error: function(result) {
			bootbox.alert(result);
		}
	});
}

function changTabInit(obj) {
	var activeType = $('#exchangeBalance ul li.active').attr("accountType");
	var $tabli = $(obj);
	var accountType = $tabli.attr("accountType");
	var init = $tabli.attr("init");
	if(init == 1 || accountType == activeType) {
		return;
	}
	searchForExchangeBalanceByFilter(0, accountType);
}

function beforeExchangeBalance(accountId, account) {
	var $exchangeBalance = $('#exchangeBalance').modal("toggle");
	$exchangeBalance.find("input[name='fromAccountId']").val(accountId);
	$.ajax({
		type: "get",
		url: API.r_account_findById,
		data: {
			"uid": "admin",
			"token": "sdwefwewef",
			"id": accountId
		},
		dataType: 'json',
		success: function(jsonObject) {
			if(jsonObject.status == 1) {
				var data = jsonObject.data;
				var footerHtml = "汇出账号：" + data.account + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				footerHtml = footerHtml + "开户人：" + data.owner + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				footerHtml = footerHtml + "系统余额：" + data.balance + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				footerHtml = footerHtml + "银行余额：" + data.bankBalance + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				$exchangeBalance.find("div[name='modal-footer']").html(footerHtml);
			}
		}
	});
	$exchangeBalance.find("input[name='search_LIKE_account']").val('');
	$exchangeBalance.find("input[name='search_LIKE_owner']").val('');
	searchForExchangeBalanceByFilter(0, null);
}


function searchByFilter(){
    authRequest.operator = $("input[name='search_LIKE_operator']").val();
    authRequest.search_LIKE_account = $("input[name='search_LIKE_account']").val();
    authRequest.search_IN_handicapId = $("select[name='search_EQ_handicapId']").val().toString();
    authRequest.levelId = $("select[name='search_EQ_LevelId']").val();
    var tempStatusToArray = new Array();
    var tempTypeToArray = new Array();

    $("input[name='search_IN_status_type']:checked").each(function(){
        tempStatusToArray.push(this.value);
        tempTypeToArray.push($(this).attr("accountType"));
    });
    var statusToArray = new Array();
    var typeToArray = new Array();
    for(var index0 in tempStatusToArray){
        var item0 = tempStatusToArray[index0].split(",");
        for(var index1 in item0){
            statusToArray.push(item0[index1]);
        }
    }
    for(var index0 in tempTypeToArray){
        var item0 = tempTypeToArray[index0].split(",");
        for(var index1 in item0){
            typeToArray.push(item0[index1]);
        }
    }
    statusToArray = removeDuplicatedItem(statusToArray);
    typeToArray =  removeDuplicatedItem(typeToArray);
    authRequest.statusToArray=statusToArray.toString();
    authRequest.typeToArray=typeToArray.toString();
    refreshContent(0);
}


/*
 * 为按钮添加事件
 */
$("#search-button").on(ace.click_event, function() {
	searchByFilter();
});


var $dynamicTable = $('#dynamic-table>tbody');
bootbox.setLocale("zh_CN");

//盘口  1. 初始化节点  修改了几点 第一  select 的宽度  避免替换数据 select 会伸缩。
//		2. 为了避免过多的节点获取  影响性能  修改成字符串拼接。
//      3. JQ 有个指定  只要一调用$()就会有获取节点的行为
var $selectHandicap = $("select[name='search_EQ_handicapId']");
$.ajax({
	type: "get",
	url: API.r_handicap_list,
	data: {
		enabled: 1
	},
	dataType: 'json',
	success: function(jsonObject) {
		if(jsonObject.status == 1) {
			var options = [];
			for(var index in jsonObject.data) {
				var item = jsonObject.data[index];
				var potion = '<option value=' + item.id + ' handicapCode=' + item.code + '>' + item.name + '</option>'
				options.push(potion);
			}
			$selectHandicap.html($selectHandicap.html() + options);
		}
	}
});
//层级   修改方式同理。
var $selectLevel = $("select[name='search_EQ_LevelId']");
$selectHandicap.change(function() {
	$selectLevel.html('');
	var handicapId = this.value;
	if(!handicapId) {
		$selectLevel.html('<option value="" selected=selected>全部</option>');
	} else {
		var handicapCode = $(this).find("option[value='" + handicapId + "']").attr("handicapCode");
		$.ajax({
			dataType: 'json',
			type: "get",
			url: API.r_level_list,
			data: {
				handicapCode: handicapCode,
				enabled: 1
			},
			success: function(jsonObject) {
				if(jsonObject.status == 1) {
					var options = ['<option value="" selected=selected>全部</option>'];
					for(var index in jsonObject.data) {
						var item = jsonObject.data[index];
						var potion = '<option value=' + item.id + ' handicapCode=' + item.code + '>' + item.name + '</option>'
						options.push(potion);
					}
					$selectLevel.html($selectLevel.html() + options);
				}
			}
		});
	}
});
//直接在HTML 默认为 true 就可以了  ，这样浪费内存，耗损性能
//$("input[name='search_IN_status_type']").attr("checked",true);
refreshContent(0);

