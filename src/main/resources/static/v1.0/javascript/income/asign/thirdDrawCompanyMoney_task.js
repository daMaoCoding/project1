/** 公司用款 下发任务JS */

var company_showTaskDataList=function(CurPage){
	var $div=$("#company_task_div"),$filter=$div.find(".filter");
    if(!!!CurPage&&CurPage!=0){
        CurPage= $div.find("#company_task_accountPage .Current_Page").text();
    }
    var startAndEnd = getTimeArray($div.find('[name="task_startAndEndTime"]').val());
    var data = {
            'handicap':$filter.find("[name='task_search_IN_handicapId']").val(),//盘口 为空后端会查有权限的盘口
            'createTimeStart':startAndEnd[0],
            'createTimeEnd':startAndEnd[1],
            'member':$filter.find('[name=task_member]').val(), //用款人
            'usetype':$filter.find('[name=task_search_IN_useName]').val(),//用途类型ID
            'receiptType':$filter.find('[name=task_search_receiptType]:checked').val(),//收款方式   银行卡0   第三方1
            'pageNo':CurPage<=0?0:CurPage-1,
            'pageSize':$.session.get('initPageSize')
        };
	if(companyTab=="all"){//全部
		if(all_companyTab_Sub=="1"){//新下发任务
			data.flag="all_1";
		}else if(all_companyTab_Sub=="2"){//正在下发
			data.flag="all_2";
		}
	}else if(companyTab=="mine"){//我的锁定
		if(mine_companyTab_Sub=="1"){//正在下发
			data.flag="mine_1";
		}else if(mine_companyTab_Sub=="2"){//等待到账
			data.flag="mine_2";
		}
	}
    $.ajax({
        type:'POST',contentType:"application/json;charset=UTF-8",dataType:'json',async:false,
        url:"/r/outNew/findOutWardNewList",
        data:JSON.stringify(data),
        success:function (jsonObject) {
        	if(jsonObject.status !=1){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
        	var $tbody = $div.find("table tbody").html("");
        	var totalAmount =0 ,totalFee=0;
        	$.each(jsonObject.data, function (index, record) {
        		totalAmount+=record.amount*1;
				totalFee+=record.fee*1;
        		var trs="<tr>";
        		//全部TAB-新下发任务（勾选做锁定） || 我已锁定-正在下发1-未到等待到账的状态 （勾选做解锁、下发选中）
            	if((companyTab=="all"&&all_companyTab_Sub=="1") || (companyTab=="mine"&&mine_companyTab_Sub=="1"&&record.status==2) ){
            		trs+="<td><input type='checkbox' name='company_checkbox_lockUnlock' class='ace' value='"+record.id+"'><span class='lbl'></span></td>";
            	}else{
            		trs+="<td></td>";
            	}
        		//编码 盘口
                var showThirdName=(record.thirdName?"<br/><span class='red' >"+record.thirdName+"</span>":"");//已设定的第三方
        		trs+="<td>"+_checkObj(record.code)+showThirdName+"</td>";
        		trs+="<td>"+_checkObj(record.handicapName)+"</td>";
        		//状态 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认到账,8-出款失败
        		trs+="<td><span class='"+CompanyMoney_status[record.status].color+"'>"+CompanyMoney_status[record.status].text+"<span></td>";
        		//类型
        		trs+="<td>第三方</td>";
        		//账号信息
        		trs+="<td><span>"+_checkObj(record.toAccountBank)+"&nbsp;|&nbsp;"+_checkObj(record.toAccountOwner)+"<br></span>"+hideAccountAll(record.toAccount)+"</td>";
        		//金额 手续费
        		trs+="<td>"+_checkObj(record.amount)+"</td>";
        		if(record.thirdCode){//已设定了第三方
            		trs+="<td>"+_checkObj(record.fee)+"</td>";
        		}else{
        			trs+="<td>--</td>";
        		}
        		//耗时 下发耗时
        		trs+="<td>"+changeColor4LockTime(record.sentConsumingTime)+"</td>";
        		trs+="<td>"+changeColor4LockTime(record.consumingTime)+"</td>";
        		//操作
        		trs+="<td>"
        			var lockBtn="<button data-placement='top' data-original-title='锁定' data-toggle='tooltip' class='btn btn-xs btn-white btn-bold btn-danger orange contentRight' onclick='company_lockThirdDraw("+record.id+",1)'><i class='ace-icon fa fa-lock bigger-100 orange'></i></button>&nbsp;";
	             	var unlockBtn="<button data-placement='top' data-original-title='解锁' data-toggle='tooltip' class='btn btn-xs btn-white btn-bold btn-success green contentRight' onclick='company_lockThirdDraw("+record.id+",0)'><i class='ace-icon fa fa-unlock bigger-100 green'></i></button>&nbsp;";
	             	var encashBtn="<button data-placement='top' data-original-title='开始出款' data-toggle='tooltip' class='btn btn-xs btn-white btn-danger btn-bold contentRight' onclick='company_takeMoneyThird(" + record.id + "," + record.fee + ")'><i class='ace-icon fa fa-check bigger-100 red'></i></button>&nbsp;";
	             	var encashSuccessBtn="<button data-placement='top' data-original-title='出款成功' data-toggle='tooltip' class='btn btn-xs btn-white btn-success btn-bold contentRight' onclick='company_drawFinshed(" + record.id + ")'><i class='ace-icon fa fa-check bigger-100 green'></i></button>&nbsp;";
	                var drawFailedBtn="<button data-placement='top' data-original-title='下发失败' data-toggle='tooltip' class='btn btn-xs btn-white btn-danger btn-bold contentRight' onclick='company_drawFailed(" + record.id + ")'><i class='ace-icon fa fa-undo bigger-100 red'></i></button>&nbsp;";
	                var operaLogBtn="<button data-placement='top' data-original-title='操作记录' data-toggle='tooltip' class='btn btn-xs btn-white btn-bold btn-info contentRight' contentRight='ThirdDrawTask:OperateLog:*' id='showModal_remark"+record.id+"'><i class='ace-icon fa fa-list bigger-100 blue'></i></button>&nbsp;";
        			if(companyTab=="all"){//全部
        				if(all_companyTab_Sub=="1"){//新下发任务
        					trs+=lockBtn;//锁定
        				}else if(all_companyTab_Sub=="2"){//正在下发
        					trs+="<span class='badge badge-warning'><i class='ace-icon fa fa-lock bigger-100'></i>&nbsp;"+_checkObj(record.lockName)+"</span>";
        				}
        			}else if(companyTab=="mine"){//我的锁定
        				if(mine_companyTab_Sub=="1"){//正在下发
        					trs+=unlockBtn;//解锁
        					trs+=encashBtn;//完成提现
        				}else if(mine_companyTab_Sub=="2"){//等待到账
        					trs+=drawFailedBtn;//下发失败
        					trs+=encashSuccessBtn;//出款成功
        				}
        			}
					trs+=operaLogBtn;//操作记录
        		trs+="</td>"
        		trs+="</tr>";
				$tbody.append($(trs));
				//操作记录  绑定事件
				$tbody.find("#showModal_remark"+record.id).bind("click",function(){
					 var companyOperateLog_param={
								businessId:record.id,
								type:"BizUseMoneyRequestData",
								add_contentRight:"ThirdDrawTask:xxxxxxx:*",//不可以新增 给一个肯定没有的权限
								delete_contentRight:"ThirdDrawTask:xxxxxxx:*",//不可以删除 给一个肯定没有的权限
								fnName:company_showTaskDataList
		                }
					showModalInfo_remark(companyOperateLog_param);
				});
        	});
        	//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var totalRows={column:15,subCount:jsonObject.data.length,count:jsonObject.page.totalElements};
				totalRows[7]={subTotal:totalAmount,total:jsonObject.page.header.sumAmount};
				totalRows[8]={subTotal:totalFee,total:jsonObject.page.header.sumFee};
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			showPading(jsonObject.page,"company_task_div #company_task_accountPage",company_showTaskDataList);
            $("[data-toggle='popover']").popover();
            //已记录的行加载到数据列
            $("[name=company_checkbox_lockUnlock]").each(function(index,record){
            	if(company_isCheckedRow.indexOf(";"+$(this).val()+";")!=-1){
            		//选中该行
            		$(this).prop("checked","checked");
            	}
            });
            //记录已勾选的数据行
            $("[name=company_checkbox_lockUnlock]").click(function(){
            	var row_id=$(this).val();
            	if($(this).prop("checked")){
            		company_isCheckedRow+=";"+row_id+";";
            	}else{
            		company_isCheckedRow=isCheckedRow.replace(";"+row_id+";", "");
            	}
            });
            syncLoadCompanyTotal();//异步加载公司用款相关总计数据
			contentRight();
        }
    });
}
/** 下发选中 */
var company_showBindThirdModal=function(){
	isShowBindThirdModal=true;
	isCompanyMoney=true;
	var array = [];
	$('[name=company_checkbox_lockUnlock]:checked').each(function(index,obj){
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
		company_showTaskDataList(0);
	});
}
var comppany_choiceOneThirdBind=function(thirdId){
	//选中一行作为指定下发第三方
	var $div=$("#bindThirdModal");
	var ids=$div.find("#choiced_accountIdList").val();
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
		type:"POST",
		dataType:'JSON',
		url:'/r/outNew/beSentThird',
		async:false,
		data:JSON.stringify({
			"thirdCode":thirdId,
			"ids":ids
		}),
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
/** 全选/全不选 */
var company_selectAllOrNotAll = function(obj){
	var checked = obj.checked;
	$('[name=company_checkbox_lockUnlock]').prop('checked',checked);
	$('[name=company_checkbox_lockUnlock]').each(function(index,record){
		if($(record).prop("checked")){
			company_isCheckedRow+=";"+$(record).val()+";";
		}else{
			company_isCheckedRow=isCheckedRow.replace(";"+$(record).val()+";", "");
		}
	});
}

/** 批量锁定/解锁 */
var companyBatch_lockThirdDraw = function(type){
	var array = [];
	$('[name=company_checkbox_lockUnlock]:checked').each(function(index,obj){
		array.push(obj.value);
	});
	if(array && array.length ==0){
		showMessageForFail('请至少勾选一行数据.');
	    return;
	}
	company_lockThirdDraw(array.toString(),type);
};
/** 锁定1/解锁0 */
var company_lockThirdDraw=function(ids,lockStatus){
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
		type:"POST",
		dataType:'JSON',
		url:'/r/outNew/outWardBeSentLock',
		async:false,
        data:JSON.stringify({
    		"ids":ids,
    		"lockStatus":lockStatus
		}),
		success:function(jsonObject){
			if(jsonObject.status == 1){
				showMessageForSuccess("操作成功！");
				company_showTaskDataList();//刷新数据列表
			}else{
				showMessageForFail("操作失败："+jsonObject.message);
			}
		}
	});
}
/** 提现 */
var company_takeMoneyThird=function(id,fee){
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
		type:"POST",
		dataType:'JSON',
		url:'/r/outNew/takeMoneyThird',
		async:false,
        data:JSON.stringify({
    		"id":id,
    		"fee":fee
		}),
		success:function(jsonObject){
			if(jsonObject.status == 1){
				showMessageForSuccess("操作成功！");
				company_showTaskDataList();//刷新数据列表
			}else{
				showMessageForFail("操作失败："+jsonObject.message);
			}
		}
	});
}
/** 出款成功 */
var company_drawFinshed=function(id){
	$.ajax({
        contentType: 'application/json;charset=UTF-8',
		type:"POST",
		dataType:'JSON',
		url:'/r/outNew/thirdOutAccountFinish',
		async:false,
        data:JSON.stringify({
    		"id":id
		}),
		success:function(jsonObject){
			if(jsonObject.status == 1){
				showMessageForSuccess("操作成功！");
				company_showTaskDataList(0);//刷新数据列表
			}else{
				showMessageForFail("操作失败："+jsonObject.message);
			}
		}
	});
}
/** 下发失败 */
var company_drawFailed=function(id){
	var html='<div class="modal fade" aria-hidden="false"  id="div_ModalInfo_remark">\
		<div class="modal-dialog modal-lg"  style="width:600px;">\
			<div class="modal-content">\
				<div class="modal-header no-padding text-center">\
					<div class="table-header">\
							<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button>\
							请再次确认下发失败\
					</div>\
				</div>\
				<div class="modal-body no-padding-bottom">\
					<div class="alert alert-success left">\
						<label>\
							<input type="radio" name="outFailing" class="ace" value="0" checked>\
							<span class="lbl">&nbsp;&nbsp;单笔下发失败</span>\
							<span class="red">（单次下发失败，可以更换第三方重新下发）</span>\
						</label>&nbsp;&nbsp;\
					</div>\
					<div class="alert alert-success left">\
						<label>\
							<input type="radio" name="outFailing" class="ace" value="1">\
							<span class="lbl">&nbsp;&nbsp;公司用款收款卡信息错误</span>\
							<span class="red">（用款申请单会被关闭，该下发任务不可以再操作）</span>\
						</label><br/>\
					</div>\
				</div>\
				<div class="col-sm-12 modal-footer no-margin center">\
					<button class="btn btn-primary" type="button" id="btnDoSuccess" >确认</button>\
					<button class="btn btn-danger" type="button" data-dismiss="modal">取消</button>\
				</div>\
			</div>\
		</div>\
	</div>';
	var $div=$(html).appendTo($("body"));
	$div.modal("toggle");
	//确定按钮绑定事件
	$div.find("#btnDoSuccess").bind("click",function(){
		$.ajax({
	        contentType: 'application/json;charset=UTF-8',
			type:"POST",
			dataType:'JSON',
			url:'/r/outNew/thirdOutAccountFailing',
			async:false,
	        data:JSON.stringify({
	    		"id":id,
	    		"outFailing":$div.find("[name=outFailing]:checked").val()
			}),
			success:function(jsonObject){
				if(jsonObject.status == 1){
					showMessageForSuccess("操作成功！");
		            $div.modal("toggle");
					company_showTaskDataList(0);//刷新数据列表
				}else{
					showMessageForFail("操作失败："+jsonObject.message);
				}
			}
		});
	});
	$div.on('hidden.bs.modal', function () {
		// 关闭窗口清除model
		company_showTaskDataList(0);
		$div.remove();
	});
}
getHandicap_select($("#company_task_div [name='task_search_IN_handicapId']"),null,"全部");
loadTimeLimit30DefaultN($("#company_task_div [name=task_startAndEndTime]"));
getFlagType_select($("#company_task_div [name='task_search_IN_useName']"));

$("#company_task_div").find("[name=task_search_IN_handicapId],[name=task_search_IN_useName]").change(function(){
	company_showTaskDataList(0);
});
$("#company_task_div").find("[name=task_search_receiptType]").click(function(){
	company_showTaskDataList(0);
});