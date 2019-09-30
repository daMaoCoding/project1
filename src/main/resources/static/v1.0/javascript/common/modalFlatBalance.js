//平账 需与common_getInfo.js同时引用
currentPageLocation = window.location.href;

/**
 * 模态窗口展示
 * accountId：需要平账的账号ID ，fnName：关闭模态窗口后的回调函数
 */
var flatBalanceModalShow=function(accountId,fnName,modalType,fnNameWithProperties){
	//查询账号信息
	var accountInfo=getAccountInfoById(accountId);
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/flatBalance.html", 
		success : function(html){
			$div=$(html).find("#flatBalanceModal").clone().appendTo($("body"));
			$div.find("input#accountInfo_id").val(accountId);
			if(accountInfo){
				if(modalType&&modalType=='third'){
					//第三方
					$div.find("#footThird").show();
					$div.find("#footBank").hide();
				}
				$div.find("[name=accountInfo_owner]").text(accountInfo.owner);
				if(accountInfo.bankType){
					$div.find("[name=accountInfo_bankName]").text(accountInfo.bankName);
				}else{
					$div.find("[name=accountInfo_bankName]").text(accountInfo.bankName);
				}
				$div.find("[name=accountInfo_account]").text(accountInfo.account);
				$div.find("[name=accountInfo_balance]").text(accountInfo.balance?accountInfo.balance:0);
				$div.find("[name=accountInfo_bankBalance]").text(accountInfo.bankBalance?accountInfo.bankBalance:0);
				//自动填充默认平账金额
				var amount=accountInfo.bankBalance-accountInfo.balance;
				if(amount==0&&modalType!='third'){
					$div.find("#createFlat input,#createFlat button").attr("disabled","disabled");
				}else{
					$div.find("#createFlat [name=amount]").val(setAmountAccuracy(amount));
				}
			}
			//加载时间控件
			initTimePicker();
			//查询平账历史记录
			showFlatList();
			$div.modal("toggle");
			$(document).keypress(function(e){
				if(event.keyCode == 13) {
					$div.find("#searchBtn button").click();
				}
			});
			$div.on('hidden.bs.modal', function () {
				if(fnName){
					fnName();
				}
				if(fnNameWithProperties){
					fnNameWithProperties(accountId);
				}
				//清除model
				$div.remove();
			});
		}
	});
	
}




/**
 * 平账记录列表展示
 */
var showFlatList=function(){
	var $div=$("#flatBalanceModal");
	var CurPage=$("#flatBalanceListPage").find(".Current_Page").text();
	CurPage=!!!CurPage?0:CurPage;
	var startAndEndTimeToArray=getTimeArray($div.find("#filter [name=startAndEndTime]").val());
	 $.ajax({
			dataType : 'JSON',
			type : "POST",
			async:false,
			url : "/r/match/findbyfrom",
			data : {
				"id":$div.find("#filter [name=search_LIKE_account]").val(),
				"fromAccount" :$div.find("input#accountInfo_id").val(),
				"type":transactionLogTypeFlat,
				"startAndEndTimeToArray":startAndEndTimeToArray.toString(),
				"pageSize":$.session.get('initPageSize'),
				"pageNo" : CurPage<=0?0:CurPage-1
			},
			success : function(jsonObject) {
				if(jsonObject.status !=1){
					showMessageForFail("查询失败："+jsonObject.message);
					return;
				}
				var totalFlatBalance = 0;
				var tbody="";
				$.each(jsonObject.data,function(index,record){
					var tr="";
					tr+="<td style='display:none;'><span>"+record.id+"</span></td>";
					tr+="<td><span>"+record.fromAccountNO+"</span></td>";
					tr+="<td><span>"+record.amount+"</span></td>";
					tr+="<td><span>"+record.confirmorUid+"</span></td>";
					tr+="<td><span>"+record.createTimeStr+"</span></td>";
					var remark=record.remark?record.remark:"";
					tr+="<td><span>"+remark+"</span></td>";
					tbody+="<tr>"+tr+"</tr>";
					totalFlatBalance+=record.amount*1;
				});
				$div.find("table#list tbody").html(tbody);
				//有数据时，显示总计 小计
				if(jsonObject.page&&(jsonObject.page.totalElements*1)){
					var totalRows={
							column:12, 
							subCount:jsonObject.data.length,
							count:jsonObject.page.totalElements,
							2:{subTotal:totalFlatBalance,total:totalFlatBalance}
						};
					showSubAndTotalStatistics4Table($div.find("table#list tbody"),totalRows);
				}
				showPading(jsonObject.page,"flatBalanceListPage",showFlatList);
			}
	 });
}


//实现平账
var doFlatBalance=function (){
	var $div = $("#flatBalanceModal");
    var $amount = $div.find("input[name='amount']");
    var $remark = $div.find("input[name='remark']");
    //校验
    var validate=[
    	{ele:$amount,name:'金额',type:'amount'},
    	{ele:$remark,name:'备注',maxLength:200}
    	];
    if(!validateEmptyBatch(validate)||!validateInput(validate)){
    	return;
    }
    var fromAccountId = $div.find("#accountInfo_id").val();
	//系统余额平账，直接存储到已匹配表中
    bootbox.confirm("确定要平账吗 ?", function(result) {
		if (result) {
			 $.ajax({
				dataType:'JSON',
		    	type:"PUT", 
		    	async:false,
		    	url:"/r/match/flatBalance",
		    	data:{
		    		"fromAccount":fromAccountId,
		    		"amount":$.trim($amount.val()),
		    		"type":transactionLogTypeFlat,
		    		"remark":$.trim($remark.val())
		    	},
		    	success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	showMessageForSuccess("平账成功");
			            //刷新列表数据
			        	showFlatList();
			        	if(jsonObject.data){
			            	//查询账号信息
			    			var accountInfo=getAccountInfoById(jsonObject.data.fromAccount);
			            	//更新系统余额
			        		$div.find("[name=accountInfo_balance]").text(accountInfo.balance);
			        		//重置平账初始化数据
							var amount=accountInfo.bankBalance-accountInfo.balance;
							if(amount==0){
								$div.find("#createFlat [name=amount]").val("");
								$div.find("#createFlat input,#createFlat button").attr("disabled","disabled");
							}else{
								$div.find("#createFlat [name=amount]").val(amount);
							}
							$div.find("#createFlat [name=remark]").val("");
			        	}
			        }else{
			        	showMessageForFail("操作失败："+jsonObject.message);
			        }
		        
		    	}	 
			 });
		}
    });
}



/*
 * 页面初始加载数据
 */
$(function() {
    //提示框中文提示
	bootbox.setLocale("zh_CN");
	
});