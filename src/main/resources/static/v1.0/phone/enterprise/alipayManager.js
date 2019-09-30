var idCheckedList=new Array();//已选中的通道资料ID集合
/** 1.1.11 客户资料列表 */
var showPage = function (CurPage) {
    var $div = $("#accountFilter");
    if (!CurPage && CurPage != 0) CurPage = $("#accountList_page").find(".Current_Page").text();
    var oid=$.trim($div.find("select[name=search_EQ_handicapCode]").val());
    if(!oid){
    	return;
    }
    var params = {
        oid:oid,//盘口编码
        name:$div.find("[name=name]").val(),
        code:$div.find("[name=code]").val(),
        stopMoneyStart:$div.find("[name=stopMoneyStart]").val(),
        stopMoneyEnd:$div.find("[name=stopMoneyEnd]").val(),
        aisleName:$div.find("[name=aisleName]").val(),
        pageNo: CurPage <= 0 ? 0 : CurPage - 1,
        pageSize: $.session.get('initPageSize')?$.session.get('initPageSize'):10
    };
    if($div.find("[name=status]:checked").length==1){
    	params.status=$div.find("[name=status]:checked").val();
    }
    $.ajax({
        contentType: 'application/json;charset=UTF-8',
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/ali4enterprise/findEpByCondition",
        data: JSON.stringify(params),
        success: function (jsonObject) {
            if (jsonObject.status != 1) {
                showMessageForFail("查询失败：" + jsonObject.message);
                return;
            }
            //查询成功后 绑定所需oid赋值
            $("#bindEpAisle4clone #oid").val(oid);
            var $tbody = $("table#accountListTable").find("tbody").html("");
            var trs="";
            $.map(jsonObject.data, function (record) {
            	var tr='<td>\
    					<label class="pos-rel">\
							<input type="checkbox" class="ace" value="'+record.id+'" name="bindEpAisleCk">\
							<span class="lbl"></span>\
						</label></td>' ;
            	tr+="<td><span>" + _checkObj(record.aisleName,true) + "</span></td>" ;
            	tr+="<td><span>" + _checkObj(record.name) + "</span></td>" ;
            	tr+="<td><span>" + _checkObj(record.code) + "</span></td>" ;
            	tr+="<td><span>" + _checkObj(record.onlyCode) + "</span></td>" ;
            	tr+="<td>";
            	tr+="<span style='float:left'>" + _checkObj(record.minMoney,true)+ "</span>";
            	tr+="<span style='float:right'>" + _checkObj(record.maxMoney,true) + "</span>" ;
            	tr+="</td>";
            	//color=0显示无色，color=1显示颜色代码为F7BA2A
            	tr+="<td "+((record.color&&record.color*1==1)?"style='background-color:#F7BA2A;'":"")+"><span>" + _checkObj(record.stopMoney) + "</span></td>" ;
            	tr+="<td><span>" + _checkObj(record.totalMoney) + "</span></td>" ;
                tr+="<td>";
            	if(record.status==1){//1启用 0停用
            		tr+="<span class='label label-sm label-success'>在用</span><br/><br/>";
            	}else{
            		tr+="<span class='label label-sm label-danger'>停用</span><br/><br/>";
            	}
            	tr+="</td>";
            	tr+="<td><span>" + _checkObj(record.createTime) + "</span></td>" ;
                //操作
                tr+= "<td>";
                tr+='<button onclick="showModalUpdate('+record.oid+','+record.id+')"\
		                class="btn btn-xs btn-white btn-bold btn-warring contentRight" contentRight="AlipayManager:Update:*" >\
		                <span>修改</span>\
		            </button>';
                tr+='<button onclick="showModalStatus('+record.oid+','+record.id+')"\
						class="btn btn-xs btn-white btn-bold btn-success contentRight" contentRight="AlipayManager:Status:*" >\
						<span>状态</span>\
					</button>';
                tr+='<button onclick="doCleanEpInData('+record.oid+','+record.id+')"\
		                class="btn btn-xs btn-white btn-bold btn-success contentRight" contentRight="AlipayManager:Clean:*" >\
		                <span>清空金额</span>\
	                </button>';
                tr+='<button onclick="showModalUpdateStopAlarm('+record.oid+','+record.id+')"\
		                class="btn btn-xs btn-white btn-bold btn-danger contentRight" contentRight="AlipayManager:Warnning:*" >\
		                <span>停用告警</span>\
	                </button>';
                tr+='<button onclick="showModalTypeBind('+record.oid+','+record.id+')"\
		                class="btn btn-xs btn-white btn-bold btn-success contentRight" contentRight="AlipayManager:BindType:*" >\
		                <span>绑定商品类型</span>\
		            </button>';
                //解绑支付通道
                if(record.aisleId){
                    tr+='<button title="解绑支付通道" onclick="doBindEpAisle(\''+record.oid+'\',\''+record.aisleId+'\',[\''+record.id+'\'],1)"\
    		                class="btn btn-xs btn-white btn-bold btn-danger contentRight" contentRight="AlipayManager:BindEp:*" >\
    		                <span>解绑支付通道</span>\
    	                </button>';
                }
                tr += "</td>";
				trs+=("<tr id='mainTr" + record.id + "'>" + tr + "</tr>");
            });
            $tbody.append($(trs));
            showPading(jsonObject.page, "accountList_page", showPage, null, true);
            $("#phoneNumber_page").find(".showImgTips").children().each(function (index, record) {
                if (index <= 4) {
                    $(record).remove();
                }
            });
            contentRight();
        }
    });
};

/** 1.1.1 新增 */
var showModalAdd = function () {
    var $div = $("#add4clone").clone().attr("id", "modalAdd");
    $div.find("td").css("padding-top", "10px");
    //初始化
    getHandicapCode_select($div.find("select[name='handicap_select']"));
    $div.find("select[name='handicap_select']").change(function(){
    	getEp_select_one($div.find("select[name='ep_select']"),$div.find("select[name='handicap_select']").val());
    });
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () {
        //关闭窗口清除内容;
        $div.remove();
    });
}
var doAdd = function () {
    var $div = $("#modalAdd");
    var $handicapId = $div.find("[name=handicap_select]");
    var $id = $div.find("[name=ep_select]");
    var $name = $div.find("[name=name]");
    var $code = $div.find("[name=code]");
    var $epKey = $div.find("[name=epKey]");
    var $epUrl = $div.find("[name=epUrl]");
    var $epUrl1 = $div.find("[name=epUrl1]");
    var $proxyPort = $div.find("[name=proxyPort]");
    var $minMoney = $div.find("[name=minMoney]");
    var $maxMoney = $div.find("[name=maxMoney]");
    var $stopMoney = $div.find("[name=stopMoney]");
    var $pubKey = $div.find("[name=pubKey]");
    var $apiGateway = $div.find("[name=apiGateway]");
    var validate = [
        {ele: $handicapId, name: '盘口'},
        {ele: $name, name: '商城名称'},
        {ele: $code, name: '商号'},
        {ele: $epKey, name: '企业支付宝私钥'},
        {ele: $epUrl, name: '企业支付宝回调地址'},
        {ele: $epUrl1, name: '代理地址'},
        {ele: $proxyPort, name: '代理地址端口'},
        {ele: $pubKey, name: '公钥'},
        {ele: $apiGateway, name: '企业支付宝网关'},
    ];
    if (!validateEmptyBatch(validate)) {//非空校验
        return;
    }
    bootbox.confirm("确定新增通道资料?", function (result) {
    	if (result) {
    		 var params = {
				oid:$handicapId.val(),
				id:$id.val(),//支付通道ID
				name1:$.trim($name.val()),
				code:$.trim($code.val()),
				epKey:$.trim($epKey.val()),
				epUrl:$.trim($epUrl.val()),
				epUrl1:$.trim($epUrl1.val()),
				proxyPort:$.trim($proxyPort.val()),
				minMoney:$.trim($minMoney.val()),
				maxMoney:$.trim($maxMoney.val()),
				stopMoney:$.trim($stopMoney.val()),
				pubKey:$.trim($pubKey.val()),
				apiGateway:$.trim($apiGateway.val())
    		 }
    		 $.ajax({
 		        type: "POST",
                contentType: 'application/json;charset=UTF-8',
 		        dataType: 'JSON',
 		        url: '/ali4enterprise/addEpAisleDetail',
 		        async: false,
 		        data: JSON.stringify(params),
 		        success: function (jsonObject) {
 		            if (jsonObject.status == 1) {
		                showMessageForSuccess("新增成功");
		                $div.modal("toggle");
		                showPage();
		            } else {
		                showMessageForFail("新增失败：" + jsonObject.message);
		            }
 		        }
    		 });
    	}
    });
    
}

/** 1.1.4 修改状态 */
var showModalStatus=function(oid,id){
	var epDetail = findEpDetail(oid,id);
	if(!epDetail){
        return;
	}
	//停用
	var statusInfo={
			color:" btn-success ",
			changeStr:"在用",
			changeStatus:"1"
	};
	if(epDetail.status&&epDetail.status==1){
		//在用
		statusInfo.color=" btn-danger ";
		statusInfo.changeStr="停用";
		statusInfo.changeStatus="0";
	}
	bootbox.dialog({
		title:		"状态修改",
		message: 	"<span class='bigger-110'>" +
						"<br/>" +
						"<label class='control-label bolder blue'>账号：&nbsp;&nbsp;&nbsp;&nbsp;</label>"
						+getEpInfoStr(epDetail)+"&nbsp;&nbsp;&nbsp;&nbsp;" +
						"<label class='control-label bolder blue'>当前状态：&nbsp;&nbsp;&nbsp;&nbsp;</label>"
						+(epDetail.status&&epDetail.status==1?"在用":"停用")+
					"</span>" +
					"<br/><br/>",
		buttons:{
			"change" :{"label" : statusInfo.changeStr,"className" : "btn btn-sm "+statusInfo.color,"callback": function() {
				doStatusUpdate(oid,id,statusInfo.changeStatus);
			}},
			"cancel" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
}
var doStatusUpdate=function(oid,id,status){
	bootbox.confirm("确定修改状态?", function (result) {
    	if (result) {
    		$.ajax({
    	        dataType: 'JSON',
                contentType: 'application/json;charset=UTF-8',
    	        type: "POST",
    	        url: "/ali4enterprise/modifyEpStatus",
    	        data: JSON.stringify({
    	        	"oid": oid,//盘口编码
    	        	"id":id,//资料编号
    	        	"status":status//状态 0:停用 1:启用
    	        }),
    	        success: function (jsonObject) {
    	            if (jsonObject.status == 1) {
    	        		showMessageForSuccess("状态修改成功");
    	           		showPage();
    	            } else {
    	                showMessageForFail("状态修改失败：" + jsonObject.message);
    	            }
    	        }
    	    });
    	}
	});
}

/** 1.1.5 修改停用金额告警 */
var showModalUpdateStopAlarm=function(oid,id){
	var epDetail = findEpDetail(oid,id);
	if(!epDetail){
        return;
	}
	var epStopAlarm=findEpStopAlarm(oid,id);
    var $div = $("#updateStopAlarm4clone").clone().attr("id", "modalUpdateStopAlarm");
    //初始化
    $div.find("#oid").val(oid);
    $div.find("#id").val(id);
    if(epStopAlarm){
        $div.find("[name=money]").val(epStopAlarm.money);
        $div.find("[name=rate]").val(epStopAlarm.rate*100);
    }
    $div.find("[name=stopMoney]").html(epDetail.stopMoney?epDetail.stopMoney:"未设置");
    $div.find("[name=epInfoStr]").html(getEpInfoStr(epDetail));
    
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () {
        //关闭窗口清除内容;
        $div.remove();
    });
}
var doUpdateStopAlarm=function(type){
	if(!type) return;
	var $div = $("#modalUpdateStopAlarm");
    var money = $div.find("[name=money]").val();
    var rate = $div.find("[name=rate]").val();
	 var params = {
		oid:$div.find("#oid").val(),//盘口
		id:$div.find("#id").val()//主键ID
	 }
    if(type=='money'){
    	if(money){
    		params.money=$.trim(money);
    	}else{
            showMessageForFail("请输入金额");
        	return;
    	}
    }else if(type=='rate'){
    	if(rate){
    		if(rate*1>0&&rate*1<100){
        		params.rate=$.trim(rate)*0.01;
    		}else{
        		showMessageForFail("请输入1-100的百分比");
            	return;
        	}
    	}else{
    		showMessageForFail("请输入百分比");
        	return;
    	}
    }
    bootbox.confirm("确定修改停用金额告警?", function (result) {
    	if (result) {
    		 $.ajax({
 		        type: "POST",
                contentType: 'application/json;charset=UTF-8',
 		        dataType: 'JSON',
 		        url: '/ali4enterprise/modifyEpStopAlarm',
 		        async: false,
 		        data: JSON.stringify(params),
 		        success: function (jsonObject) {
 		            if (jsonObject.status == 1) {
		                showMessageForSuccess("修改成功");
		                $div.modal("toggle");
		                showPage();
		            } else {
		                showMessageForFail("修改失败：" + jsonObject.message);
		            }
 		        }
    		 });
    	}
    });
}
/** 1.1.6 查询停用金额告警 */
var findEpStopAlarm=function(oid,id){
	var result;
	$.ajax({
		dataType: 'JSON',
        contentType: 'application/json;charset=UTF-8',
		type: "POST",
		url: "/ali4enterprise/findEpStopAlarm",
        async:false, 
		data: JSON.stringify({
			"oid":oid,
			"id":id//资料编号
		}),
		success: function (jsonObject) {
			if (jsonObject.status == 1 && jsonObject.data && jsonObject.data) {
				result = jsonObject.data;
			}
		}
	});
	return result;
}

/** 1.1.7 清空累计金额 */
var doCleanEpInData=function(oid,id){
	bootbox.confirm("确定清空累计金额?", function (result) {
    	if (result) {
    		$.ajax({
    	        dataType: 'JSON',
                contentType: 'application/json;charset=UTF-8',
    	        type: "POST",
    	        url: "/ali4enterprise/cleanEpInData",
    	        data: JSON.stringify({
    	        	"oid": oid,//盘口编码
    	        	"id":id//资料编号
    	        }),
    	        success: function (jsonObject) {
    	            if (jsonObject.status == 1) {
    	        		showMessageForSuccess("清除成功");
    	           		showPage();
    	            } else {
    	                showMessageForFail("清除失败：" + jsonObject.message);
    	            }
    	        }
    	    });
    	}
	});
}

/** 1.1.8 修改 */
var showModalUpdate=function(oid,id){
	var epDetail=findEpDetail(oid,id);
	if(!epDetail) return;
    var $div = $("#update4clone").clone().attr("id", "modalUpdate");
    $div.find("td").css("padding-top", "10px");
    //初始化
    $div.find("#oid").val(oid);
    $div.find("#id").val(id);
    $div.find("[name=epInfoStr]").html(getEpInfoStr(epDetail));
    getHandicapCode_select($div.find("select[name='handicap_select']"),oid);
//    getEp_select_one($div.find("select[name='ep_select']"),$div.find("select[name='handicap_select']").val(),epDetail.aisleId);
    $div.find("[name=name]").val(epDetail.name);
    $div.find("[name=code]").val(epDetail.code);
    $div.find("[name=epKey]").val(epDetail.epKey);
    $div.find("[name=epUrl]").val(epDetail.epUrl);
    $div.find("[name=epUrl1]").val(epDetail.epUrl1);
    $div.find("[name=proxyPort]").val(epDetail.proxyPort);
    $div.find("[name=minMoney]").val(epDetail.minMoney);
    $div.find("[name=maxMoney]").val(epDetail.maxMoney);
    $div.find("[name=stopMoney]").val(epDetail.stopMoney);
    $div.find("[name=pubKey]").val(epDetail.pubKey);
    $div.find("[name=apiGateway]").val(epDetail.apiGateway);
    
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () {
        //关闭窗口清除内容;
        $div.remove();
    });
}
var doUpdate=function(){
	var $div = $("#modalUpdate");
    var $name = $div.find("[name=name]");
    var $code = $div.find("[name=code]");
    var $epKey = $div.find("[name=epKey]");
    var $epUrl = $div.find("[name=epUrl]");
    var $epUrl1 = $div.find("[name=epUrl1]");
    var $proxyPort = $div.find("[name=proxyPort]");
    var $minMoney = $div.find("[name=minMoney]");
    var $maxMoney = $div.find("[name=maxMoney]");
    var $stopMoney = $div.find("[name=stopMoney]");
    var $pubKey = $div.find("[name=pubKey]");
    var $apiGateway = $div.find("[name=apiGateway]");
    var validate = [
        {ele: $name, name: '商城名称'},
        {ele: $code, name: '商号'},
        {ele: $epKey, name: '企业支付宝私钥'},
        {ele: $epUrl, name: '企业支付宝回调地址'},
        {ele: $epUrl1, name: '代理地址'},
        {ele: $proxyPort, name: '代理地址端口'},
        {ele: $pubKey, name: '公钥'},
        {ele: $apiGateway, name: '企业支付宝网关'},
    ];
    if (!validateEmptyBatch(validate)) {//非空校验
        return;
    }
    bootbox.confirm("确定修改通道资料?", function (result) {
    	if (result) {
    		 var params = {
				oid:$div.find("#oid").val(),//盘口
				id:$div.find("#id").val(),//主键ID
				name1:$.trim($name.val()),
				code:$.trim($code.val()),
				epKey:$.trim($epKey.val()),
				epUrl:$.trim($epUrl.val()),
				epUrl1:$.trim($epUrl1.val()),
				proxyPort:$.trim($proxyPort.val()),
				minMoney:$.trim($minMoney.val()),
				maxMoney:$.trim($maxMoney.val()),
				stopMoney:$.trim($stopMoney.val()),
				pubKey:$.trim($pubKey.val()),
				apiGateway:$.trim($apiGateway.val())
    		 }
    		 $.ajax({
 		        type: "POST",
                contentType: 'application/json;charset=UTF-8',
 		        dataType: 'JSON',
 		        url: '/ali4enterprise/modifyEpAisleDetail',
 		        async: false,
 		        data: JSON.stringify(params),
 		        success: function (jsonObject) {
 		            if (jsonObject.status == 1) {
		                showMessageForSuccess("修改成功");
		                $div.modal("toggle");
		                showPage();
		            } else {
		                showMessageForFail("修改失败：" + jsonObject.message);
		            }
 		        }
    		 });
    	}
    });
}

/** 批量绑定支付通道 */
var showModalbindEpAisle=function(){
	if($("[name=bindEpAisleCk]:checked").length<1){
        showMessageForFail("请至少选中一行通道资料");
		return;
	}
    var $div = $("#bindEpAisle4clone").clone().attr("id", "modalBindEpAisle");
    $div.find("td").css("padding-top", "10px");
	var oid=$div.find("#oid").val();
	//初始化支付通道下拉框
	getEp_select_one($div.find("select[name='ep_select']"),oid,null,"---------请选择支付通道---------");
	//初始化选中账号信息
    idCheckedList=new Array();
    var detailHTML="";
    $.map($("[name=bindEpAisleCk]:checked"),function(record){
    	var epDetail=findEpDetail(oid,$(record).val());
    	if(epDetail){
    		idCheckedList.push($(record).val());
        	detailHTML+=getEpInfoStr(epDetail)+"<br/>";
    	}
    });
    $div.find("#detailList").html(detailHTML);
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () {
        //关闭窗口清除内容;
        $div.remove();
    });
}
var doBatchBind=function(){
	var $div=$("#modalBindEpAisle");
	//支付通道ID
	var id=$div.find("[name=ep_select]").val();
	if(!id){
        showMessageForFail("请先选择支付通道");
		return;
	}
	doBindEpAisle($div.find("#oid").val(),id,idCheckedList,0);
}

/** 1.1.9 通道资料绑定/解绑支付通道 */
var doBindEpAisle=function(oid,id,bindEpList,flag){
	var $div=$("#modalBindEpAisle");
	if(!oid||!id||!bindEpList||bindEpList.length<1){
		return;
	}
    bootbox.confirm("确定"+(flag?"解绑":"绑定")+"?", function (result) {
    	if (result) {
    		 var params = {
				oid:oid,//盘口
				id:id,//支付通道ID
				idList:bindEpList,//通道资料编号集合
				flag:flag//0 : 绑定 1 : 解绑
    		 }
    		 $.ajax({
 		        type: "POST",
                contentType: 'application/json;charset=UTF-8',
 		        dataType: 'JSON',
 		        url: '/ali4enterprise/bindEpAisle',
 		        async: false,
 		        data: JSON.stringify(params),
 		        success: function (jsonObject) {
 		            if (jsonObject.status == 1) {
		                showMessageForSuccess("操作成功");
		                $div.modal("toggle");
		                showPage();
		            } else {
		                showMessageForFail("操作失败：" + jsonObject.message);
		            }
 		        }
    		 });
    	}
    });
}

/** 通道信息字符串 */
var getEpInfoStr=function(epDetail,oid,id){
	var result="";
	if(!epDetail){
		epDetail = findEpDetail(oid,id);
	}
	result+=(epDetail.name?epDetail.name:"--");
	result+="&nbsp;|&nbsp;"+(epDetail.code?epDetail.code:"--");
	return result;
}

/** 1.1.3 查询单个通道资料 */
var findEpDetail=function(oid,id){
	var result;
	$.ajax({
        dataType: 'JSON',
        contentType: 'application/json;charset=UTF-8',
        type: "POST",
        url: "/ali4enterprise/findEpDetail",
        async:false, 
        data: JSON.stringify({
        	"oid": oid,//盘口编码
        	"id":id//资料编号
        }),
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data) {
            	result = jsonObject.data;
            }
        }
    });
	return result;
}
/** 1.1.10 查询支付通道列表 */
var findEpAisle=function(oid,name){
	var result;
	$.ajax({
        dataType: 'JSON',
        contentType: 'application/json;charset=UTF-8',
        type: "POST",
        url: "/ali4enterprise/findEpAisle",
        async:false, 
        data: JSON.stringify({
        	"oid": oid,
        	"name":name
        }),
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data) {
            	result = jsonObject.data;
            }
        }
    });
	return result;
}
/** 自动加载支付通道选择列表 */
var getEp_select_one=function($div,oid,epId,titleName){
	var options="";
	var EqList=new Array();
	EqList=findEpAisle(oid);
	options+="<option value='' >"+(titleName?titleName:"--------------请选择--------------")+"</option>";
	if(oid){
		$.each(EqList,function(index,record){
			if(epId&&record.id==epId){
				options+="<option selected value="+record.id+" >"+record.name+"</option>";
			}else{
				options+="<option value="+record.id+" >"+(record.name?record.name:"")+"</option>";
			}
		});
	}
	$div.html(options);
}

getHandicapCode_select_one($("#accountFilter").find("select[name='search_EQ_handicapCode']"));
getHandicap_select($("[name=search_EQ_handicapId_sync]"),null,"全部");
showPage(0);
//数据列表的全选事件
$("#accountListTable #ckAll").click(function(){
	var $childenBox=$("[name=bindEpAisleCk]");
	if(this.checked){   
		$childenBox.prop("checked", true);  
	}else{   
		$childenBox.prop("checked", false);
	}  
});
$("#accountFilter").find("[name=status]").click(function(){
	showPage(0);
});
$("#accountFilter").find("[name=search_EQ_handicapCode],[name=search_EQ_id]").change(function(){
	showPage(0);
});
