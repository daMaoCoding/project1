//设定第三方 下发选中 相关代码
var isShowBindThirdModal=false;//是否点击了下发选中按钮
var isCompanyMoney=false;//是否公司下发
/**
 * 下发选中
 */
var showBindThirdModal=function(){
	isShowBindThirdModal=true;
	isCompanyMoney=false;
	var array = [];
	$('[name=checkbox_lockUnlock]:checked').each(function(index,obj){
		array.push(obj.value);
	});
	if(array && array.length ==0){
		showMessageForFail('请至少勾选一行数据.');
	    return;
	}
	var $div=$("#bindThirdModal4copy").clone().appendTo($("body")).attr("id","bindThirdModal");
	$div.modal("toggle");

	getHandicap_select($div.find("[name='third_search_EQ_handicapId']"), 0, "全部");
	change_thirdTabInit("2");
	$div.find("#choiced_accountIdList").val(array.toString());
	$div.find("[name=third_search_EQ_handicapId]").change(function () {
		searchChoiceThird(0);
	});
	$(document).keypress(function(e){
		if(event.keyCode == 13) {
			$div.find("#searchBtn button").click();
		}
	});
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model 查询数据列表
		$div.remove();
		thirdDraw(0);
	});
}

/**
 * 设定第三方
 */
var showChoiceThirdModal=function(){
	isShowBindThirdModal=false;
	var $div=$("#choiceThirdModal4copy").clone().appendTo($("body")).attr("id","choiceThirdModal");
	$div.modal("toggle");

	getHandicap_select($div.find("[name='third_search_EQ_handicapId']"), 0, "全部");
	change_thirdTabInit("2");
	$div.find("[name=third_search_EQ_handicapId]").change(function () {
		searchChoiceThird(0);
	});
	$(document).keypress(function(e){
		if(event.keyCode == 13) {
			$div.find("#searchBtn button").click();
		}
	});
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model 查询数据列表
		$div.remove();
		thirdDraw(0);
	});
	$('#selectAllOrNotAllSet').checked=false;
}
var searchChoiceThird=function(CurPage){
	var releaseOtherSetup=false;
    $.each(ContentRight['ThirdDrawTask:*'], function (name, value) {
        if (name == 'ThirdDrawTask:ReleaseOtherSetup:*') {
            releaseOtherSetup=true;
        }
    });
    var divName=isShowBindThirdModal?"bindThirdModal":"choiceThirdModal";
	var $div=$("#"+divName);
    if (!!!CurPage && CurPage != 0) CurPage = $div.find("#thirdAccountPage .Current_Page").text();
    var tabVal=$div.find("#thirdAccountTabType").val();//1 全部 2 我的设定
	 var data = {
		        pageNo: CurPage <= 0 ? 0 : CurPage - 1,
		        pageSize: $.session.get('initPageSize'),
		        typeToArray: [accountTypeInThird].toString(),
		        statusToArray:"1,3,4",
		        mySetup :tabVal,
		        search_IN_handicapId: $div.find("[name='third_search_EQ_handicapId']").val().toString().trim(),
		        search_LIKE_account: $div.find("[name='third_search_LIKE_account']").val().trim(),
		        search_LIKE_bankName: $div.find("[name='third_search_LIKE_bankName']").val().trim()
		    }
	 if(!isShowBindThirdModal&&tabVal==2){
		 //我的设定可查询已删除的账号
		 data.statusToArray="1,3,4,-2";
	 }
	$.ajax({
	    dataType: 'JSON',
	    type: "POST",
	    async: false,
	    url: API.r_account_list,
	    data: data,
	    success: function (jsonObject) {
	        if (jsonObject.status != 1) {
	            if (-1 == jsonObject.status) {
	                showMessageForFail("查询失败：" + jsonObject.message);
	            }
	            return;
	        }
	        var $tbody = $div.find("table tbody").html("");
	        var fromIdArray = new Array(), tdName = 'detail';
	        var totalBalanceByBank = 0, totalBalance = 0, idList = new Array;
	        $.each(jsonObject.data, function (index, record) {
	            fromIdArray.push(record.id);
	            idList.push({'id': record.id, 'type': 'third'});
	            var tr = "";
	            if(!isShowBindThirdModal){
	            	var col_checkBox="<td><input type='checkbox' name='checkboxThirdId'  class='ace' value='"+record.id+"'  /><span class='lbl'></span></td>";
	            	//需求 7994 ,无手续费规则不允许选中
	            	if(tabVal==1 && !record.accountFeeEffect){
	            		col_checkBox="<td><input type='checkbox' name='checkboxThirdId'  class='ace' value='"+record.id+"' disabled /><span style='color: red'>无手续费规则不能选中</span></td>";
	            	}
	            	var col_label_mine="<td style='color: blueviolet'>我已设定</td>";
		            if(tabVal==1){//全部
		                if(record.isSetUpFlag){//是否已设定
		                	if (record.isSetUpFlag==2){//自己
		                		tr+=col_label_mine;
		                	}else if(record.isSetUpFlag==1){//他人
		    	            	var col_btn='<td><button onclick="releaseOtherSetup('+record.id+')" class="btn btn-xs btn-white btn-round"><i class="ace-icon fa fa-times red2"></i>解除他人设定（'+_checkObj(record.thirdLockOper)+'）</button></td>';
		                		tr+=col_btn;
		                	}
                        }else{
                        	tr+=col_checkBox;
                        }
		            }else{//我的设定
		            	tr+=col_checkBox;
		            }
	            }
	            tr += "<td><span>" + record.handicapName + "</span></td>";
	            tr += "<td><span>" + record.levelNameToGroup + "</span></td>";
	            tr += "<td>" +
	                "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + record.id + "' data-placement='auto right' data-trigger='hover'  >" + hideAccountAll(record.account) +
	                "</a>" +
	                "</td>";
	            tr += "<td><span title='第三方商户'>" + (record.bankName ? record.bankName : "无") + "</span></td>";//第三方
	            tr+="<td>"+getStatusInfoHoverHTML(record)+"</td>";
	            tr += "<td><a onclick='showThirdDetailModal(" + record.id + ")' class='bind_hover_card breakByWord' data-toggle='summary" + record.id + "' data-placement='auto left' data-trigger='hover'>"
	                + (record.balance == null ? 0 : record.balance)
	                + "</a></td>";
	            record.bankBalance = record.bankBalance ? record.bankBalance : 0;
	            //第三方余额
	            if(isShowBindThirdModal){
	            	 tr+="<td>"+_checkObj(record.bankBalance)+"</td>";
	            }else{
	            	tr += "<td id='bankBalance" + record.id + "'><span name='bankBalance' >" + record.bankBalance + "</span><i class='red ace-icon fa fa-pencil-square-o' onclick='changeInput(" + record.id + "," + record.bankBalance + ")' title='校正余额' style='cursor:pointer;'  ></i></td>";
	            }
	            if(isShowBindThirdModal){
	            	//操作
	            	if(isCompanyMoney){
	            		//公司立即下发接口
	            		tr +="<td><button class='btn btn-xs btn-white btn-bold btn-info contentRight' contentRight='ThirdDrawTask:OperateLog:*' onclick='comppany_choiceOneThirdBind(\""+record.id+"\")'><i class='ace-icon fa fa-list bigger-100 blue'></i><span>立即下发</span></button></td>";
	            	}else{
	            		//普通立即下发
	            		tr +="<td><button class='btn btn-xs btn-white btn-bold btn-info contentRight' contentRight='ThirdDrawTask:OperateLog:*' onclick='choiceOneThirdBind(\""+record.id+"\")'><i class='ace-icon fa fa-list bigger-100 blue'></i><span>立即下发</span></button></td>";
	            	}
	            }else{
	            	  //提现明细
		            tr += getRecording_Td(record.id, tdName);
	            }
	            $tbody.append($("<tr id='mainTr" + record.id + "'>" + tr + "</tr>"));
	            totalBalanceByBank += record.bankBalance * 1;
	            totalBalance += record.balance * 1;
	            loadHover_accountTodayInto(record.id, record.totalAmount, record.amount, record.feeAmount, record.balance, record.mappedAmount, record.mappingAmount);
	        });
	        //异步刷新数据
	        loadInOutTransfer(fromIdArray, tdName, null);
	        //有数据时，显示总计 小计
	        if (jsonObject.page && (jsonObject.page.totalElements * 1)) {
	            var totalRows = {
	                column: 12,
	                subCount: jsonObject.data.length,
	                count: jsonObject.page.totalElements,
	                7: {subTotal: totalBalance, total: jsonObject.page.header.totalAmountBalance},
	                8: {subTotal: totalBalanceByBank, total: jsonObject.page.header.totalAmountBankBalance}
	            };
	            if(isShowBindThirdModal){
	            	totalRows["column"]=8;
	            	totalRows["6"]=totalRows["7"];
	            	totalRows["7"]=totalRows["8"];
	            	totalRows["8"]=null;
	            }
	            showSubAndTotalStatistics4Table($tbody, totalRows);
	        }
			if(jsonObject&&jsonObject.page&&jsonObject.page.header){
				var header=jsonObject.page.header;
				$div.find(".mySetUpedNumber").text(header.mySetUpedNumber);
				$div.find(".allSetUpNumber").text(header.allSetUpNumber);
			}
	        $("[data-toggle='popover']").popover();
	        //分页初始化
	        showPading(jsonObject.page, divName+" #thirdAccountPage", searchChoiceThird);
	        contentRight();
	        //加载账号悬浮提示
	        loadHover_accountInfoHover(idList);
	    }
	});
}
//解除他人设定的三方账号
function releaseOtherSetup(id) {
	if (!id)return;
	var ajaxData = {type:'get',dataType:'json',data:{id:id},url:'/r/account/releaseOtherSetup',async:false,success:function (res) {
			if (res && res.status==1){
                showMessageForSuccess("操作成功!");
			} else {
                showMessageForFail("解除失败！"+res.message,2000);
			}
            setTimeout(function(){
                $('body').addClass('modal-open'); searchChoiceThird();
            },500);
        }};
	bootbox.confirm("确定解除他人设定吗?",function (res) {
		if (res){
			$.ajax(ajaxData);
		}
    });

}
/** 全选/全不选 */
var selectAllOrNotAllSet = function(obj){
    var checked = obj.checked;
    $('[name=checkboxThirdId]').prop('checked',checked);
};
var change_thirdTabInit=function(thirdType){
	//TAB切换查询对应数据
	var $div=$("#choiceThirdModal");
	$div.find("#thirdAccountTabType").val(thirdType);
	$div.find(".lockThird,.unlockThird").hide();
	if(thirdType=="1"){
		$div.find(".lockThird").show();
	}else if(thirdType=="2"){
		$div.find(".unlockThird").show();
	}
	searchChoiceThird(0);
}


//选中一行作为指定下发第三方
var choiceOneThirdBind=function(thirdId){
	var $div=$("#bindThirdModal");
	$.ajax({
		type:"PUT",
		dataType:'JSON',
		url:'/r/account/saveSelectThirdRecord',
		async:false,
		data:{
			"thirdId":thirdId,
			"accountIds":$div.find("#choiced_accountIdList").val()
		},
		success:function(jsonObject){
	        if(jsonObject&&jsonObject.status == 1){
	        	showMessageForSuccess("操作成功！");
	            $div.modal("toggle");
	        }else{
	        	showMessageForFail("操作失败："+jsonObject.message);
	        }
	    }
	});
}



/**  选中解除我的设定  选中加入我的设定    */
var saveSelectThirdRecord = function(type){
	var array = [];
	var $div=$("#choiceThirdModal");
	$div.find('[name=checkboxThirdId]:checked').each(function(index,obj){
		array.push(obj.value);
	});
	if(array && array.length ==0){
		showMessageForFail('请至少勾选一行数据.');
	    return;
	}
	//设定不用确认提示
	$.ajax({
		type:"PUT",
		dataType:'JSON',
		url:'/r/account/setThirdAccount',
		async:false,
		data:{
			"type":type,//1添加2解除
			"thirdAccount":array.toString()
		},
		success:function(jsonObject){
	        if(jsonObject&&jsonObject.status == 1){
	        	showMessageForSuccess("操作成功！");
	        	searchChoiceThird(0);
	        }else{
	        	showMessageForFail("操作失败："+jsonObject.message);
	        }
            $('#selectAllOrNotAllSet').checked=false;
	    }
	});
};


/** 今日入款汇总 */
var loadHover_accountTodayInto = function (id, totalAmount, amount, feeAmount, balance, mappedAmount, mappingAmount) {
    $("[data-toggle='summary" + id + "']").popover({
        html: true,
        title: function () {
            return '<center class="blue">今日入款汇总</center>';
        },
        delay: {show: 0, hide: 100},
        content: function () {
            return "<div id='accountInfoHover' style='width:400px' >"
                + "<div class='col-sm-12'>"
                + "	<div class='col-xs-4 text-right'><strong>入款金额：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + totalAmount + "</span></div>"
                + "	<div class='col-xs-4 text-right'><strong>实际入款：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + amount + "</span></div>"
                + "</div>"
                + "<div class='col-sm-12'>"
                + "	<div class='col-xs-4 text-right'><strong>手续费：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + feeAmount + "</span></div>"
                + "	<div class='col-xs-4 text-right'><strong>系统余额：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + (balance == null ? 0 : balance) + "</span></div>"
                + "</div>"
                + "<div class='col-sm-12'>"
                + "	<div class='col-xs-4 text-right'><strong>已匹配：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + (mappedAmount == null ? 0 : mappedAmount) + "</span></div>"
                + "	<div class='col-xs-4 text-right'><strong>未匹配：</strong></div>"
                + "	<div class='col-xs-2 no-padding-lr'><span>" + (mappingAmount == null ? 0 : mappingAmount) + "</span></div>"
                + "</div>";
            // return "<table border='1'><tr><td align='left' width='500px;'>入款金额："+totalAmount+"</td><td width='200px;'>实际入款："+amount+"</td></tr><tr><td width='200px;'>手续费："+feeAmount+"</td><td width='200px;'>系统余额："+balance+"</td></tr><tr><td width='200px;'>已匹配："+mappedAmount+"</td><td width='200px;'>未匹配："+mappingAmount+"</td></tr></table>";
        }
    });
}

//矫正余额
function changeInput(id, value) {
    $("#choiceThirdModal #bankBalance" + id).find("span").html("<input onkeyup='clearNoNum(this)' id='bankBalanceInput" + id + "' class='input-sm' style='width:80px;' value='" + value + "'>");
    $("#choiceThirdModal #bankBalance" + id).find("i").attr("class", "green ace-icon fa fa-check");
    $("#choiceThirdModal #bankBalance" + id).find("i").attr("onclick", "savaBankBalance(" + id + ")");
}
function savaBankBalance(id) {
    var data = {
        "id": id,
        "bankBalance": $("#choiceThirdModal #bankBalanceInput" + id).val(),
    };
    $.ajax({
        type: "PUT",
        dataType: 'JSON',
        url: '/r/account/update',
        async: false,
        data: data,
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data) {
            	searchChoiceThird();
            } else {
                showMessageForFail("账号修改失败：" + jsonObject.message);
                setTimeout(function(){
                    $('body').addClass('modal-open');
                },500);
            }
        },
        error: function (result) {
            showMessageForFail("修改失败：" + jsonObject.message);
            setTimeout(function(){
                $('body').addClass('modal-open');
            },500);
        }
    });
}


$("#choiceThirdModal,#bindThirdModal").find("[name=third_search_EQ_handicapId]").change(function(){
	searchChoiceThird(0);
});
