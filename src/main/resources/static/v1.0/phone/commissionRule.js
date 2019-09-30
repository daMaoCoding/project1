var HandicapBatch=getHandicapBatchInfoByCode();

var showPage=function(CurPage){
	var $div = $("#accountFilter");
    if (!CurPage && CurPage != 0) CurPage = $("#commissionRule_page").find(".Current_Page").text();
    var oid=$.trim($div.find("select[name=search_EQ_handicapCode]").val());
    if(!oid){
    	return;
    }
    var $handicapCode = $div.find("[name=search_EQ_handicapCode]");
    var $inType = $div.find("[name=inTypeRadio]:checked");
    var $commissionPercentStart = $div.find("[name=commissionPercentStart]");
    var $commissionPercentEnd = $div.find("[name=commissionPercentEnd]");
    var $commissionMaxStart = $div.find("[name=commissionMaxStart]");
    var $commissionMaxEnd = $div.find("[name=commissionMaxEnd]");
    var params = {
	    pageNo: CurPage <= 0 ? 0 : CurPage - 1,
	    pageSize: $.session.get('initPageSize')?$.session.get('initPageSize'):10,
	    oid:oid,//盘口编码
		inType:$inType.val(),//入款方式
		commissionPercentStart:$commissionPercentStart.val(),
		commissionPercentEnd:$commissionPercentEnd.val(),
		commissionMaxStart:$commissionMaxStart.val(),
		commissionMaxEnd:$commissionMaxEnd.val()
    };
    $.ajax({
        contentType: 'application/json;charset=UTF-8',
        dataType: 'JSON',
        type: "POST",
        async: false,
        url: "/newpay/findCRByCondition",
        data: JSON.stringify(params),
        success: function (jsonObject) {
            if (jsonObject.status != 1) {
                showMessageForFail("查询失败：" + jsonObject.message);
                return;
            }
            var $tbody = $("table#accountListTable").find("tbody").html("");
            var trs="";
            $(jsonObject.data).map(function (index,record) {
            	trs+="<tr>";
            	trs+="<td><span>"+(index+1)+"</span></td>";
            	trs+="<td>" + (HandicapBatch&&HandicapBatch[record.oid]?HandicapBatch[record.oid].name:"") + "</td>";
            	trs+="<td><span>"+(record.inType==0?"微信":(record.inType==1?"支付宝":"银行卡"))+"</span></td>";
            	trs+="<td><span>"+record.startMoney+"&nbsp;-&nbsp;"+record.endMoney+"</span></td>";
            	trs+="<td><span>"+record.commissionPercent+"%</span></td>";
            	trs+="<td><span>"+record.commissionMax+"</span></td>";
            	trs+="<td><span>"+(record.adminName?record.adminName:"")+"</span></td>";
            	trs+="<td><span>"+(record.uptime?record.uptime:"")+"</span></td>";
            	//操作
                trs+= "<td>";
                trs+= "<a class='btn btn-xs btn-white btn-warning btn-bold orange contentRight' contentright='CommissionRule:Update:*' \
	                		onclick='showModalUpdateCommissionRule(" + record.oid + "," + record.id +"," + record.inType+ "," + record.startMoney +"," 
	                		+ record.endMoney +"," + record.commissionPercent + "," + record.commissionMax+ ");' >\
	        				<i class='ace-icon fa fa-pencil-square-o bigger-100 red'></i>\
	        				<span>修改</span>\
                		</a>";

                trs+="<a class='btn btn-xs btn-white btn-danger btn-bold contentRight' contentRight='CommissionRule:Delete:*' " +
						"onclick='do_deleteCommissionRule(" + record.oid + "," + record.id + ")'>" +
						"<i class='ace-icon fa fa-close bigger-100 red'></i>" +
						"<span>删除</span>" +
					"</a>";
                trs += "</td>";
            	trs+="</tr>";
            });
            $tbody.html(trs);
            showPading(jsonObject.page, "commissionRule_page", showPage, null, true);
            contentRight();
        }
    });
}

/** 新增佣金规则 */
var showModalAddCommissionRule = function () {
    var $div = $("#addCommissionRule4clone").clone().attr("id", "addCommissionRule");
    $div.find("td").css("padding-top", "10px");
    getHandicapCode_select($div.find("select[name='handicap_select']"));
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () {
        $div.remove();
    });
}
var doAddCommissionRule=function(){
	 var $div = $("#addCommissionRule");
     var $handicapCode = $div.find("[name=handicap_select]");
     var $inType = $div.find("[name=inType]");
     var $startMoney = $div.find("[name=startMoney]");
     var $endMoney = $div.find("[name=endMoney]");
     var $commissionPercent = $div.find("[name=commissionPercent]");
     var $commissionMax = $div.find("[name=commissionMax]");
     var validate = [
         {ele: $handicapCode, name: '盘口'},
         {ele: $inType, name: '入款方式'},
         {ele: $startMoney, name: '流水范围开始值'},
         {ele: $endMoney, name: '流水范围结束值'},
         {ele: $commissionPercent, name: '佣金比例'},
         {ele: $commissionMax, name: '佣金最高限额'}
     ];
     if (!validateEmptyBatch(validate)) {//非空校验
         return;
     }
     if($startMoney.val()*1>$endMoney.val()*1){
    	 showMessageForCheck("流水范围开始值不可大于结束值");
    	 return;
     }
     if($commissionPercent.val()*1<0||$commissionPercent.val()*1>100){
    	 showMessageForCheck("佣金比例值在 0 ~ 100 之间");
    	 return;
     }
	bootbox.confirm("确定新增佣金规则 ?", function (result) {
		if (result) {
			 var params = {
				oid:$handicapCode.val(),//盘口编码
				inType:$inType.val(),//入款方式
				startMoney:$startMoney.val(),//流水范围开始值
				endMoney:$endMoney.val(),//流水范围结束值
				commissionPercent:$commissionPercent.val(),//佣金比例
				commissionMax:$commissionMax.val()//佣金最高限额
		    };
		    $.ajax({
		        type: "POST",
               contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/addCR',
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

/** 修改佣金规则基本信息*/
var showModalUpdateCommissionRule=function(handicapCode,id,inType,startMoney,endMoney,commissionPercent,commissionMax){
	var $div = $("#updateCommissionRule4clone").clone().attr("id", "updateCommissionRule");
    $div.find("td").css("padding-top", "10px");
    $div.modal("toggle");
    //基本信息
    getHandicapCode_select($div.find("[name=handicap_select]"),handicapCode);
    $div.find("[name=id]").val(id);
    $div.find("[name=inType]").val(inType);
    $div.find("[name=startMoney]").val(startMoney);
    $div.find("[name=endMoney]").val(endMoney);
    $div.find("[name=commissionPercent]").val(commissionPercent);
    $div.find("[name=commissionMax]").val(commissionMax);
	$div.on('hidden.bs.modal', function () {
	    $div.remove();
	});
}
var doUpdateCommissionRule=function(){
	var $div = $("#updateCommissionRule");
    var $handicapCode = $div.find("[name=handicap_select]");
    var $inType = $div.find("[name=inType]");
    var $startMoney = $div.find("[name=startMoney]");
    var $endMoney = $div.find("[name=endMoney]");
    var $commissionPercent = $div.find("[name=commissionPercent]");
    var $commissionMax = $div.find("[name=commissionMax]");
    var validate = [
        {ele: $handicapCode, name: '盘口'},
        {ele: $inType, name: '入款方式'},
        {ele: $startMoney, name: '流水范围开始值'},
        {ele: $endMoney, name: '流水范围结束值'},
        {ele: $commissionPercent, name: '佣金比例'},
        {ele: $commissionMax, name: '佣金最高限额'}
    ];
    if (!validateEmptyBatch(validate)) {//非空校验
        return;
    }
    if($startMoney.val()*1>$endMoney.val()*1){
   	 showMessageForCheck("流水范围开始值不可大于结束值");
   	 return;
    }
    if($commissionPercent.val()*1<0||$commissionPercent.val()*1>100){
   	 showMessageForCheck("佣金比例值在 0 ~ 100 之间");
   	 return;
    }
	bootbox.confirm("确定修改 ?", function (result) {
		if (result) {
			 var params = {
				id: $div.find("[name=id]").val(),
				oid:$handicapCode.val(),//盘口编码
				inType:$inType.val(),//入款方式
				startMoney:$startMoney.val(),//流水范围开始值
				endMoney:$endMoney.val(),//流水范围结束值
				commissionPercent:$commissionPercent.val(),//佣金比例
				commissionMax:$commissionMax.val()//佣金最高限额
		    };
		    $.ajax({
		        type: "POST",
		        contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/modifyCR',
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

/** 删除返佣信息 */
var do_deleteCommissionRule=function(handicapCode,id){
	bootbox.confirm("确定删除 ?", function (result) {
		if (result) {
			var params={
				oid:handicapCode,
				id:id
			}
			$.ajax({
		        type: "POST",
		        contentType: 'application/json;charset=UTF-8',
		        dataType: 'JSON',
		        url: '/newpay/removeCR',
		        async: false,
		        data: JSON.stringify(params),
		        success: function (jsonObject) {
		            if (jsonObject.status == 1) {
		                showMessageForSuccess("删除成功");
		                showPage();
		            } else {
		                showMessageForFail("删除失败：" + jsonObject.message);
		            }
		        }
		   });
		}
	});
}
getHandicapCode_select_one($("select[name='search_EQ_handicapCode']"),null,"全部");
showPage(0);
contentRight();

$("#accountFilter").find("[name=inTypeRadio]").click(function(){
	showPage(0);
});
$("#accountFilter").find("[name=search_EQ_handicapCode]").change(function(){
	showPage(0);
});