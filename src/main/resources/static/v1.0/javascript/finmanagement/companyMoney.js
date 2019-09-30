var AllPermission=false;//查看全部数据（不勾选只能查看本人的申请）
$.each(ContentRight['FinanceCompanyMoney:*'], function (name, value) {
    if (name == 'FinanceCompanyMoney:AllPermission:*') {
    	AllPermission = true;
    }
});
var FlagTypeList=[];//用途类型
var companyMoney_TAB="1";//主TAB 默认查询任务数据
var companyMoney_TAB2_SUB="2_1";//审核下的第二层TAB 默认查询待审核数据
var changeTab_companyMoney=function(curr_tab){
	companyMoney_TAB=curr_tab;
	if(curr_tab=="1"){
		$("#statusArea").show();
	}else{
		$("#statusArea").hide();
	}
	showTaskList(0);
}
var changeTab_checkDiv=function(curr_tab){
	companyMoney_TAB2_SUB=curr_tab;
	showTaskList(0);
}



var showTaskList=function(CurPage){
	var $div=$("#companyMoney_data"),$filter=$div.find(".filter");
    if(!!!CurPage&&CurPage!=0){
        CurPage= $("#taskPage .Current_Page").text();
    }
    var startAndEnd = getTimeArray($div.find('[name="startAndEndTime"]').val());
    var data = {
            'handicap':$filter.find("[name='search_IN_handicapId']").val(),//盘口 为空后端会查有权限的盘口
            'createTimeStart':startAndEnd[0],
            'createTimeEnd':startAndEnd[1],
            'member':(AllPermission?$filter.find('[name=member]').val():getCookie('JUID')), //用款人 有权限查看全部或者查询条件的输入框，否则当前登录人
            'usetype':$filter.find('[name=search_IN_useName]').val(),//用途类型ID
            'receiptType':$filter.find('[name=search_receiptType]:checked').val(),//收款方式   银行卡0   第三方1
            'pageNo':CurPage<=0?0:CurPage-1,
            'pageSize':$.session.get('initPageSize')
        };
    //状态 根据TAB取值 '1-待审核,2-审核通过,5-等待到账 ,6-完成出款,7-确认到账,8-出款失败'
    if(companyMoney_TAB=="1"){//任务
    	data.status=$filter.find('[name=status]:checked').val();//全部状态
    }else if(companyMoney_TAB=="2"){//财务审核
    	if(companyMoney_TAB2_SUB=="2_1"){
    		data.status=1;//待审核
    	}else if(companyMoney_TAB2_SUB=="2_2"){
    		data.status="2,5,6,7,8";//通过
    	}
    }else if(companyMoney_TAB=="3"){//流程完成
    	data.status=7;//确认到账
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
        		//状态  '1-待审核,2-审核通过,5-等待到账 ,6-完成出款,7-确认到账,8-出款失败'
        		trs+="<td><span class='"+CompanyMoney_status[record.status].color+"'>"+CompanyMoney_status[record.status].text+"<span>";
						if(record.status==8){//出款失败原因
							trs+="&nbsp;<a class='badge badge-danger' title='公司用款收款卡信息错误'>原因</a>";
						}
    					if(record.status==2){//下发审核通过时间
    						trs+="<br/><span>"+timeStamp2yyyyMMddHHmmss(record.taskReviewerTime)+"</span>";
    					}else{
    						//其他流程取更新时间
							trs+="<br/><span>"+timeStamp2yyyyMMddHHmmss(record.updateTime)+"</span>";
    					}
        		trs+="</td>";
        		trs+="<td>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</td>";
        		trs+="<td><a title='"+(record.remark)+"'>"+(record.remark?cutFlag4remark(record.remark).substring(0, 5)+(record.remark.length>5?"...":""):"无")+"</a></td>";
        		trs+="<td>";
        		//任何状态都可以直接查看详情
				trs+="<button class='btn btn-xs btn-white btn-info btn-bold' \
								onclick='showCompanyDetailModal("+record.id+",true,showTaskList)'>\
								<i class='ace-icon fa fa-detail bigger-100 blue'></i><span>详情</span>" +
							"</button>";
        			if(record.status==6){
        				trs+="<button class='btn btn-xs btn-white btn-success btn-bold contentRight' contentRight='FinanceCompanyMoney:ConfrimFinshed:*' \
				    					onclick='do_cfoInAccountFinish("+record.id+")'>\
				    					<span>确认到账</span>" +
				    				"</button>";
        			}else if(record.status==1){
        				trs+="<button class='btn btn-xs btn-white btn-danger btn-bold contentRight' contentRight='FinanceCompanyMoney:FinanceReview:*' \
				    					onclick='showCompanyDetailModal("+record.id+",false,showTaskList)'>\
				    					<i class='ace-icon fa fa-detail bigger-100 orange'></i><span>审核</span>" +
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
			showPading(jsonObject.page,"taskPage",showTaskList);
			syncLoadCompanyTotal();//异步加载公司用款相关总计数据
			contentRight();
        }
    });
}

//新增公司用款
var showModal_doAddCompany=function(){
	var $div=$("#doAddCompanyModal4copy").clone().appendTo($("body")).attr("id","doAddCompanyModal");
	$div.modal("toggle");
	$div.find("[name=member]").html(getCookie('JUID'));//用款人
	getHandicap_select($div.find("[name='handicap']"), null, "-----------请选择-----------");//盘口
	getFlagType_select($div.find("[name='usetype']"),"-----------请选择-----------");//用途类型
	getBankTyp_select($div.find("[name='toAccountBank']"),null,"----------------------------请选择----------------------------");//银行类别
	
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model 查询数据列表
		$div.remove();
	});
}
var doAddCompanyMoney=function(){
	var $div=$("#doAddCompanyModal");
	var $usetype=$div.find("[name=usetype]");//用途类型ID
	var $handicap=$div.find("[name=handicap]");//盘口ID
	var $amount=$div.find("[name=amount]");//出款金额
	var $receiptType=$div.find("[name=add_receiptType]:checked");//出款方式
	var $toAccountBank=$div.find("[name=toAccountBank]");//银行类别
	var $toAccount=$div.find("[name=toAccount]");//银行卡号
	var $toAccountOwner=$div.find("[name=toAccountOwner]");//开户人
	var $remark=$div.find("[name=remark]");//备注
    //校验非空和输入校验
    var validatePrint=[
    	{ele:$usetype,name:'用途类型'},
    	{ele:$handicap,name:'盘口'},
    	{ele:$amount,name:'出款金额',type:'amountPlus'},
    	{ele:$receiptType,name:'出款方式'},
    	{ele:$toAccountBank,name:'银行类别'},
    	{ele:$toAccount,name:'银行卡号',maxLength:25},
    	{ele:$toAccountOwner,name:'开户人',minLength:2,maxLength:10}
    ];
    if(!validateEmptyBatch(validatePrint)||!validateInput(validatePrint)||!validateInput([{ele:$remark,name:'备注',minLength:5,maxLength:300}])){
    	setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
    	return;
    }
	var data={
			"usetype":$usetype.val(),
			"handicap":$handicap.val(), 
			"amount":$amount.val(),
			"receiptType":$receiptType.val(), //   收款方式 0-银行卡 1-第三方 
			"toAccountBank":$toAccountBank.val(),      //   银行卡名称
			"toAccount":$toAccount.val().trim(),   //   银行卡号
			"toAccountOwner":$toAccountOwner.val().trim(),      //   开户人
			"remark":$remark.val().trim()       //   备注
	}
	$.ajax({
		type:"POST",
		dataType:'JSON',
		url:'/r/outNew/addOutWardNew',
		contentType: 'application/json',
		async:false,
		data:JSON.stringify(data),
		success:function(jsonObject){
			if(jsonObject.status == 1){
				$div.modal("toggle");
				showTaskList(0);
	        	showMessageForSuccess("新增成功");
	        }else{
	        	showMessageForFail("新增失败："+jsonObject.message);
	        }
	    },
	    error:function(result){
        	showMessageForFail("添加失败："+jsonObject.message);
        	 setTimeout(function(){       
 	            $('body').addClass('modal-open');
 	        },500);
	    }
	});
}


//确认到账
var do_cfoInAccountFinish=function(id){
	$.ajax({
        type:'POST',contentType:"application/json;charset=UTF-8",dataType:'json',async:false,
        url:"/r/outNew/cfoInAccountFinish", 
        data:JSON.stringify({"id":id}),
        success:function(jsonObject){
        	if(jsonObject.status ==1){
	        	showMessageForSuccess("操作成功");
	        	showTaskList(0);
        	}else{
        		showMessageForFail("操作失败"+jsonObject.message);
        	}
		}
	});
}



loadTimeLimit30DefaultN($("[name=startAndEndTime]"));
getHandicap_select($("[name='search_IN_handicapId']"), null, "全部");
getFlagType_select($("[name='search_IN_useName']"));
changeTab_companyMoney("1");
contentRight();

$("#companyMoney_data").find("[name=status],[name=search_receiptType]").click(function(){
	showTaskList(0);
});
$("#companyMoney_data").find("[name=search_IN_handicapId],[name=search_IN_useName]").change(function(){
	showTaskList(0);
});