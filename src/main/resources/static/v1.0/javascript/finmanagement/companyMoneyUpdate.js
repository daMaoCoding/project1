/** 公司用款流程处理 */

/**
 * 根据ID获取公司用款信息
 */
var getCompanyMoneyById=function(id){
	var result;
	$.ajax({
        type:'POST',contentType:"application/json;charset=UTF-8",dataType:'json',async:false,
        url:"/r/outNew/findOutWardNewInfo", 
        data:JSON.stringify({"id":id}),
        success:function(jsonObject){
			if(jsonObject.status !=1){
				showMessageForFail("获取公司用款信息异常，"+jsonObject.message);
			}else{
				result=jsonObject.data;
			}
		}
	});
	return result;
}

/**
 * 公司用款 详情/审核 公用弹窗
 * id：用款ID
 * isDetail：是否只查看详情
 * fnName：有则在窗口关闭后执行
 */
var showCompanyDetailModal=function(id,isDetail,fnName){
	var result=getCompanyMoneyById(id);
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#CompanyDetailModal").clone().appendTo($("body"));
			// 表单填充值
			if(result){
				$div.find("[name=member]").html(result.member);
				$div.find("[name=useName]").html(result.useName);
				$div.find("[name=handicapName]").html(result.handicapName);
				$div.find("[name=amount]").html(result.amount);
				$div.find("[name=receiptType]").html(result.receiptType=="1"?"第三方":"出款卡");
				$div.find("[name=toAccountBank]").html(result.toAccountBank);
				$div.find("[name=toAccount]").html(result.toAccount);
				$div.find("[name=toAccountOwner]").html(result.toAccountOwner);
				$div.find("[name=remark]").html((result.remark?result.remark.replace(/\n/g,'<br/>'):"无"));
				//状态  '1-待审核,2-审核通过,5-等待到账 ,6-完成出款,7-确认到账,8-出款失败'
				if(isDetail){//查看详情
					$div.find("[name=title]").html("详细");
					$div.find("[name=btn_success]").remove();
					if(result.status==3||result.status==4||result.status==8){
						$div.find("[name=review]").html(result.review);
						$div.find(".review_div").show();
					}
					$div.find("#btn_success").hide();
				}else{//审核
					$div.find("[name=title]").html("审核");
					if(result.status==0){//待财务审核
						$div.find(".div3").show();
						//点击事件
						$div.find("#btn_success").bind("click",function(){
							var status=$div.find(".div3").find("[name=status]:checked").val();
							var review=$div.find(".div3").find("[name=review]").val().trim();
							if(status==3&&!review){
								showMessageForFail("请输入备注");
								return;
							}
							$.ajax({
						        type:'POST',contentType:"application/json;charset=UTF-8",dataType:'json',async:false,
						        url:"/r/outNew/auditOutWardNew", 
						        data:JSON.stringify({
						        	"id":id,
						        	"status":status,
						        	"review":review
						        }),
						        success:function(jsonObject){
						        	if(jsonObject.status ==1){
							        	showMessageForSuccess("操作成功");
			                            $div.modal("toggle");
						        	}else{
						        		showMessageForFail("操作失败"+jsonObject.message);
						        	}
								}
							});
						});
					}else if(result.status==1){//待下发审核
						$div.find(".div4").show();
						//点击事件
						$div.find("#btn_success").bind("click",function(){
							var status=$div.find(".div4").find("[name=status]:checked").val();
							var review=$div.find(".div4").find("[name=review]").val().trim();
							if(status==4&&!review){
								showMessageForFail("请输入备注");
								return;
							}
							$.ajax({
						        type:'POST',contentType:"application/json;charset=UTF-8",dataType:'json',async:false,
						        url:"/r/outNew/auditOutWardNewBeSent", 
						        data:JSON.stringify({
						        	"id":id,
						        	"status":status,
						        	"review":review
						        }),
						        success:function(jsonObject){
						        	if(jsonObject.status ==1){
							        	showMessageForSuccess("操作成功");
			                            $div.modal("toggle");
						        	}else{
						        		showMessageForFail("操作失败"+jsonObject.message);
						        	}
								}
							});
						});
					}
				}
				if($.inArray(result.status, [1,2,3,4,5,6,7,8])!=-1){//财务审核记录模块
					$div.find(".div1").show();
					$div.find(".div1").find("[name=financeReviewerName]").html(result.financeReviewerName);
					$div.find(".div1").find("[name=financeReviewerTime]").html(timeStamp2yyyyMMddHHmmss(result.financeReviewerTime));
					$div.find(".div1").find("[name=statusStr]").html(result.status==3?"不通过":"通过");
				}
				if($.inArray(result.status, [2,4,5,6,7,8])!=-1){//下发审核记录模块
					$div.find(".div2").show();
					$div.find(".div2").find("[name=taskReviewerName]").html(result.taskReviewerName);
					$div.find(".div2").find("[name=taskReviewerTime]").html(timeStamp2yyyyMMddHHmmss(result.taskReviewerTime));
					$div.find(".div2").find("[name=statusStr]").html(result.status==4?"不通过":"通过");
				}
			}
			
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				// 关闭窗口清除model
				$div.remove();
				if(fnName){
					fnName();
				}
			});
//			$div.find("#btn_success").bind("click",function(){
//				do_updateIncomeAccount(accountInfo.type,accountInfo.flag,fnName,accountInfo.status);
//			});
		}
	});
}

/** 异步加载公司用款小圆圈统计数据 */
var syncLoadCompanyTotal=function(){
	  $.ajax({
	        type:'GET',contentType:"application/json;charset=UTF-8",dataType:'json',async:true,
	        url:"/r/outNew/statistics",
	        success:function (jsonObject) {
	        	if(jsonObject.status ==1){
	        		var result=jsonObject.data;
	        		$("#company_status0,#company_status0A").text(result.status0);//财务待审核
	        		$("#company_status1,#company_status1A").text(result.status1);//下发待审核
	        		$("#company_status2").text(result.status2);//下发任务 - 新下发任务（下发审核通过未锁定）
	        		$("#company_status3").text(result.status3);//下发任务 - 正在下发（锁定未下发完成）
	        		$("#company_status4").text(result.status4);//我已锁定 - 正在下发（我已锁定未下发完成）
	        		$("#company_status5").text(result.status5);//我已锁定 - 等待到账（我已锁定绑定点击第三方提现正在出款中）
	        		$("#company_status6").text(result.status2+result.status3);//下发任务
	        		$("#company_status7").text(result.status4+result.status5);//我已锁定
	        		$("#company_status8").text(result.status1+result.status2+result.status3);//公司用款下发
				}
	        }
	  });
}