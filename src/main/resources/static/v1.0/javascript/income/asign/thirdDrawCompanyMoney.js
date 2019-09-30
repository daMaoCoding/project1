/** 公司用款 下发审核模块 */
var FlagTypeList=[];//用途类型
var companyTab="";//公司用款list 下发任务all 我已锁定mine

//公司用款list 下发任务task 我已锁定mine 的切换
var changeTab_companyMoney=function(tab_val){
	if(tab_val){
		companyTab=tab_val;
	}
	if(companyTab=="list"){//用款审核
		$("#companyMoneyTable_div").hide();
		showTaskList(0);
	}else{//下发
		$("#companyMoneyTable_div").show();
		showHide4TabChange();
	}
}
//状态 根据TAB取值 '0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认到账,8-出款失败'
var tab_status="1";
var changeTab_4status=function(tab_val){
	tab_status=tab_val;
	showTaskList(0);
}
//公司用款列表查询
var showTaskList=function(CurPage){
	var $div=$("#companyMoney_list"),$filter=$div.find(".filter");
    if(!!!CurPage&&CurPage!=0){
        CurPage= $div.find("#taskPage .Current_Page").text();
    }
    var startAndEnd = getTimeArray($div.find('[name="startAndEndTime"]').val());
    var data = {
            'handicap':$filter.find("[name='search_IN_handicapId']").val(),//盘口 为空后端会查有权限的盘口
            'status':tab_status,
            'createTimeStart':startAndEnd[0],
            'createTimeEnd':startAndEnd[1],
            'member':$filter.find('[name=member]').val(), //用款人
            'usetype':$filter.find('[name=search_IN_useName]').val(),//用途类型ID
            'receiptType':1,//收款方式   银行卡0   第三方1（默认）
            'pageNo':CurPage<=0?0:CurPage-1,
            'pageSize':$.session.get('initPageSize')
        };
    $.ajax({
        type:'POST',contentType:"application/json;charset=UTF-8",dataType:'json',async:false,
        url:"/r/outNew/findOutWardNewList",
        data:JSON.stringify(data),
        success:function (jsonObject) {
        	if(jsonObject.status !=1){
				showMessageForFail("查询失败："+jsonObject.message);
				return;
			}
        	var $tbody = $div.find("table tbody").html(""),trs="";
        	var totalAmount =0;
        	$.each(jsonObject.data, function (index, record) {
        		totalAmount+=record.amount*1;
        		trs+="<tr>";
        		trs+="<td>"+_checkObj(record.code)+"</td>";
        		trs+="<td>"+_checkObj(record.handicapName)+"</td>";
        		trs+="<td><span>"+_checkObj(record.toAccountBank)+"&nbsp;|&nbsp;"+_checkObj(record.toAccountOwner)+"<br></span>"+hideAccountAll(record.toAccount)+"</td>";
        		trs+="<td>"+_checkObj(record.amount)+"</td>";
        		trs+="<td>"+_checkObj(record.member)+"</td>";
        		trs+="<td>"+_checkObj(record.useName)+"</td>";
        		//状态 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认到账,8-出款失败
        		trs+="<td><span class='"+CompanyMoney_status[record.status].color+"'>"+CompanyMoney_status[record.status].text+"<span>";
    					if(record.status==1){//财务审核通过
    						trs+="<br/><span>"+timeStamp2yyyyMMddHHmmss(record.financeReviewerTime)+"</span>";
    					}else if(record.status==2){//下发审核通过
    						trs+="<br/><span>"+timeStamp2yyyyMMddHHmmss(record.taskReviewerTime)+"</span>";
    					}else if(record.status==3){//财务审核不通过
        					trs+="&nbsp;<a class='badge badge-danger' title='"+_checkObj(record.financeReviewerName)+"："+_checkObj(record.review)+"'>原因</a>";
    						trs+="<br/><span>"+timeStamp2yyyyMMddHHmmss(record.financeReviewerTime)+"</span>";
    					}else if(record.status==4){//下发审核不通过
        					trs+="&nbsp;<a class='badge badge-danger' title='"+_checkObj(record.taskReviewerName)+"："+_checkObj(record.review)+"'>原因</a>";
    						trs+="<br/><span>"+timeStamp2yyyyMMddHHmmss(record.taskReviewerTime)+"</span>";
    					}else{
        					trs+="<br/><span>"+timeStamp2yyyyMMddHHmmss(record.updateTime)+"</span>";
    					}
        		trs+="</td>";
        		trs+="<td>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</td>";
        		trs+="<td><a title='"+(record.remark)+"'>"+(record.remark?cutFlag4remark(record.remark).substring(0, 5)+(record.remark.length>5?"...":""):"无")+"</a></td>";
        		trs+="<td>";
				trs+="<button class='btn btn-xs btn-white btn-info btn-bold' \
								onclick='showCompanyDetailModal("+record.id+",true,showTaskList)'>\
								<i class='ace-icon fa fa-detail bigger-100 blue'></i><span>详情</span>" +
							"</button>";
        			if(record.status=="1"){
        				trs+="<button class='btn btn-xs btn-white btn-danger btn-bold contentRight' contentRight='ThirdDrawTask:CompanyMoneyReviewBtn:*'  \
				    					onclick='showCompanyDetailModal("+record.id+",false,showTaskList)'>\
				    					<i class='ace-icon fa fa-detail bigger-100 orange'></i><span>下发审核</span>" +
				    				"</button>";
        			}
        		trs+="</td>";
        		trs+="</tr>";
        	});
        	$tbody.html(trs);
        	//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var totalRows={column:15,subCount:jsonObject.data.length,count:jsonObject.page.totalElements};
				totalRows[4]={subTotal:totalAmount,total:jsonObject.page.header.sumAmount};//金额
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
            $("[data-toggle='popover']").popover();
			showPading(jsonObject.page,"companyMoney_list #taskPage",showTaskList);
			contentRight();
        }
    });
}