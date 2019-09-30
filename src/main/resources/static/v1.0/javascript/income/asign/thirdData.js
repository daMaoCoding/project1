//第三方资料
var thirdAccountList;//入款第三方账号集合
/**
 * 根据账号Type拼接对应数据
 */
var showThirdDataList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#thirdData_accountPage .Current_Page").text();
	var $filter = $("#thirdData_accountFilter");
    var data = {
    	handicapId:$filter.find("[name=third_search_IN_handicapId]").val(),
        thirdName:$filter.find("[name=thirdName]").val(),
        status:$filter.find("[name=thirdData_search_EQ_status]:checked").val(),
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize')
    };
    data.queryPage=pageType;//全部三方资料 1 我的三方资料 2
    //发送请求
	$.ajax({
        dataType:'JSON', 
        type:"GET", 
		async:false,
        url:'/r/thirdInfo/list?type=thirdData',
        data:data,
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			var $tbody=$("#thirdData_Div_accountListTable").find("tbody");
			$tbody.html("");
			var idList=new Array();
			var fromIdArray=new Array();
        	var totalSysBalance =0,totalBankBalance=0;//系统余额 第三方真实余额小计
			$.each(jsonObject.data,function(index,record){
                record.sysBalance= (record.sysBalance? record.sysBalance:0);
                record.bankBalance= (record.bankBalance? record.bankBalance:0);
                totalSysBalance+=record.sysBalance*1;
                totalBankBalance+=record.bankBalance*1;
				idList.push(record.id);
				fromIdArray.push(record.accountId);
				var tr="";
				tr+="<tr>";
				tr+="<td>"+_checkObj(record.handicapName)+"</td>";//盘口
				//需求 7458
				if(record.thirdNameUrl){
					tr+='<td><a target="_blank" href="'+$.trim(record.thirdNameUrl)+'"  >'+_checkObj(record.thirdName)+'</a></td>';
				}else{
					tr+='<td>'+_checkObj(record.thirdName)+'</td>';
				}
				//tr+="<td><a title='"+_checkObj(record.thirdNameUrl)+"' href='javascript:void(0);' onclick='jumpUrl("+$.trim(record.thirdNameUrl)+")>"+_checkObj(record.thirdName)+"</a></td>";//第三方
				tr+="<td>"+hideAccountAll(record.thirdNumber)+"</td>";//商户号
				var statusRecord={
						statusStr:(record.status==1?"在用":(record.status==4?"停用":"冻结")),
						status:record.status
				}
                tr+="<td>"+getStatusInfoHoverHTML(statusRecord)+"</td>";//状态
                tr+="<td>"+_checkObj(record.loginAccount)+getCopyHtml(record.loginAccount)+"</td>";//登录账号
                tr+="<td>"+_checkObj(record.loginPass)+getCopyHtml(record.loginPass)+"</td>";//登录密码
                tr+="<td>"+_checkObj(record.payPass)+getCopyHtml(record.payPass)+"</td>";//支付密码
                //系统余额
                tr +="<td>"+_checkObj(record.sysBalance,false,0)+"</td>";
                //第三方真实余额
				tr += "<td id='third_bankBalance" + record.accountId + "'><span name='bankBalance' >" + record.bankBalance + "</span><i class='red ace-icon fa fa-pencil-square-o' onclick='changeInput_thirdData(" + record.accountId + "," + record.bankBalance + ")' title='校正余额' style='cursor:pointer;'  ></i></td>";
                //提现明细
				tr += getRecording_Td(record.accountId, "detail");
				tr+="<td><a class='contentRight' contentRight='ThirdDrawTask:RemarkSearch:*' id='showModal_remark"+record.id+"'  title='"+_checkObj(record.latestRemark)+"'>"+(record.latestRemark?_checkObj(record.latestRemark.substring(0,20)):"无备注，点击添加")+"</a></td>";//备注
				//操作
				tr+="<td>";
				//修改
				tr+="<button data-placement='top' data-original-title='修改' data-toggle='tooltip'  class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentright='ThirdDrawTask:ThirdUpdate:*'\
					onclick='showEditThirdDataModal("+(record.id?("\""+record.id+"\""):null)+",\""+record.accountId+"\",showThirdDataList)'>\
					<i class='ace-icon fa fa-edit bigger-100 orange'></i></button>";
				tr+="</td>";
				tr+="</tr>";
				$tbody.append($(tr));
				$tbody.find("#showModal_remark"+record.id).bind("click",function(){
					//绑定修改事件
					var param={
						businessId:record.id,
						type:"thirdData",
						remarkTitle:"第三方："+_checkObj(record.thirdName),
						search_contentRight:"ThirdDrawTask:RemarkSearch:*",
						add_contentRight:"ThirdDrawTask:RemarkAdd:*",
						delete_contentRight:"ThirdDrawTask:RemarkDelete:*",
						fnName:showThirdDataList
					};
					showModalInfo_remark(param);
				});
			});
			showPading(jsonObject.page,"thirdData_accountPage",showThirdDataList,null,true);
            loadInOutTransfer(fromIdArray, "detail");
        	//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)&&jsonObject.page.header){
				var totalRows={column:15,subCount:jsonObject.data.length,count:jsonObject.page.totalElements};
				totalRows[8]={subTotal:totalSysBalance,total:jsonObject.page.header.balanceTotal};//系统余额
				totalRows[9]={subTotal:totalBankBalance,total:jsonObject.page.header.bankBalanceTotal};//第三方真实余额
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			contentRight();
            $('[data-toggle="tooltip"]').tooltip();
        }
	});
}


//矫正余额
function changeInput_thirdData(id, value) {
  $("#thirdData_Div_accountListTable").find("#third_bankBalance" + id).find("span").html("<input onkeyup='clearNoNum(this)' id='third_bankBalanceInput" + id + "' class='input-sm' style='width:80px;' value='" + value + "'>");
  $("#thirdData_Div_accountListTable").find("#third_bankBalance" + id).find("i").attr("class", "green ace-icon fa fa-check");
  $("#thirdData_Div_accountListTable").find("#third_bankBalance" + id).find("i").attr("onclick", "savaBankBalance_thirdData(" + id + ")");
}
function savaBankBalance_thirdData(id) {
  var data = {
      "id": id,
      "bankBalance": $("#thirdData_Div_accountListTable").find("#third_bankBalanceInput" + id).val(),
  };
  $.ajax({
      type: "PUT",
      dataType: 'JSON',
      url: '/r/account/update',
      async: false,
      data: data,
      success: function (jsonObject) {
          if (jsonObject.status == 1 && jsonObject.data) {
        	  showThirdDataList();
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
function jumpUrl(url) {
	if (url){
		window.open(url);
	}
}
/**
 * 新增或修改第三方资料
 * thirdDataId：有则是修改，没有是新增
 * accountId：是第三方账号，还未初始化资料相关信息
 */
var showEditThirdDataModal=function(thirdDataId,accountId,fnName){
	var html1nbsp="&nbsp;&nbsp;&nbsp;&nbsp;";
	var html='<div class="modal fade" aria-hidden="false"  id="div_ModalInfo_edit_thirdData">\
		<div class="modal-dialog modal-lg"  style="width:350px;">\
			<div class="modal-content">\
				<div class="modal-header no-padding text-center">\
					<div class="table-header">\
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button>\
						<span name="title"></span>\
					</div>\
				</div>\
				<div class="modal-body no-padding-bottom">\
					<div id="div_edit_content">\
						<input type="hidden" id="accountId" >\
						<span class="control-label blue ">三方名称：<i class="ace-icon fa fa-asterisk red"></i></span>\
						<select name="thirdName_multiselect" ></select><br/><div style="height:10px;">&nbsp;</div>\
						<span class="control-label blue ">'+html1nbsp+html1nbsp+'盘口：<i class="ace-icon fa fa-asterisk red"></i></span>\
						<select class="chosen-select" name="handicapId"  style="width:159px;"><option value="">请先选择第三方</option></select><br/><div style="height:10px;">&nbsp;</div>\
						<span class="control-label blue ">'+html1nbsp+'商户号：<i class="ace-icon fa fa-asterisk red"></i></span>\
						<select class="chosen-select" name="thirdNumber"  style="width:159px;"><option value="">请先选择盘口</option></select><br/><div style="height:10px;">&nbsp;</div>\
						<span class="control-label blue ">'+html1nbsp+html1nbsp+'网址：<i class="ace-icon fa fa-asterisk red"></i></span>\
						<input class="input-sm" name="thirdNameUrl" type="text" placeholder="例:http://www.baidu.com" ><br/><div style="height:10px;">&nbsp;</div>\
						<span class="control-label blue ">登陆账号：<i class="ace-icon fa fa-asterisk red"></i></span>\
						<input class="input-sm" name="loginAccount" type="text"><br/><div style="height:10px;">&nbsp;</div>\
						<span class="control-label blue ">登陆密码：<i class="ace-icon fa fa-asterisk red"></i></span>\
						<input class="input-sm" name="loginPass" type="text" ><br/><div style="height:10px;">&nbsp;</div>\
						<span class="control-label blue ">支付密码：<i class="ace-icon fa fa-asterisk red"></i></span>\
						<input class="input-sm" name="payPass" type="text"><br/><div style="height:10px;">&nbsp;</div>\
						<br/><div style="height:10px;">&nbsp;</div>\
					</div>\
				</div>\
				<div class="col-sm-12 modal-footer no-margin center">\
					<button class="btn btn-primary" type="button" id="btnDoEditThirdData" >确认</button>\
					<button class="btn btn-danger" type="button" data-dismiss="modal">取消</button>\
				</div>\
			</div>\
		</div>\
	</div>';

	var $div=$(html).appendTo($("body"));
	var thirdDataInfo=thirdDataId?getThirdDataInfoById(thirdDataId):null;
	var unInit=(!thirdDataId&&accountId)?true:false;//还未初始化的第三方账号，未设置过相关信息
	var isUpdate=(thirdDataId||unInit?true:false);
	if(unInit){//是入款第三方账号
		var tempData=getAccountInfoById(accountId);
		if(!tempData){
			//查询不到第三方数据则直接返回
			$div.remove();
			 return;
		}
		thirdDataInfo={
				"accountId":accountId,//关联的账号ID
				"thirdName":tempData.bankName,//第三方名
				"thirdNumber":tempData.account,//商户号
				"handicapId":tempData.handicapId,//盘口ID
				"handicapName":tempData.handicapName//盘口名
		}
	}
	$div.find("[name=title]").html(isUpdate?"修改第三方资料":"新增第三方资料");
	//第三方下拉框加载
	var options=new Array();
	if(!thirdAccountList){
		getInThirdAccountList(false);//如果未查询过，同步查询第三方账号列表
	}
	 if(!thirdAccountList||(isUpdate&&!thirdDataInfo)){
		 //查询不到可绑定的第三方列表||是修改却查不出第三方资料信息直接返回
		$div.remove();
		 return;
	 }
	$div.modal("toggle");
	 if(isUpdate){//修改，第一行加载已绑定的入款第三方
		 options+="<option  value="+thirdDataInfo.thirdName+"  selected >"+thirdDataInfo.thirdName+"</option>";
	 }else{//新增
		 options+='<option value="">请选择第三方</option>';
	 }
	 $.each(thirdAccountList,function(index,record){
 		//修改时 如果当前数据行是已经拼接好的第一行，则跳过 不拼接
		 if(!isUpdate||thirdDataInfo.thirdName!=record.bankName){
			 options+="<option  value="+record.bankName+"  >"+record.bankName+"</option>";
		 }
	});
	var $thirdName_multiselect=$div.find("[name=thirdName_multiselect]");
	$thirdName_multiselect.html(options).multiselect({
		multiple:false,//单选
		enableFiltering: true,
		enableHTML: true,
		nonSelectedText :'-----------请选择-----------',
		nSelectedText :'已选中',
		buttonClass: 'btn btn-white btn-primary',
		buttonWidth: '159px',
		buttonHeight:'20px',
		maxHeight:250,
		templates: {
			button: '<button type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>',
			ul: '<ul class="multiselect-container dropdown-menu"></ul>',
			filter: '<li class="multiselect-item filter"><div class="input-group"><span class="input-group-addon"><i class="fa fa-search"></i></span><input class="form-control multiselect-search" type="text"></div></li>',
			filterClearBtn: '<span class="input-group-btn"><button class="btn btn-default btn-white btn-grey multiselect-clear-filter" type="button"><i class="fa fa-times-circle red2"></i></button></span>',
			li: '<li><a tabindex="0"><label></label></a></li>',
			divider: '<li class="multiselect-item divider"></li>',
			liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
		},
        onChange: function(option, checked) {  
        	//先清空盘口和商户号
        	$div.find("[name=handicapId]").html('<option value="">请先选择第三方</option>');
        	$div.find("[name=thirdNumber]").html('<option value="">请先选择盘口</option>');
        	//查询选择的第三方下所拥有的盘口
        	var result4handicap=searchThirdInfoCommon({"handicapId":handicapId_list.toString(),"thirdName":$(option).val() });
        	if(result4handicap){
        		//重新加载盘口
            	var handicap_options='<option value="">请选择</option>';
            	 $.each(result4handicap,function(index,record){
            		 handicap_options+="<option  value="+record.handicapId+" >"+record.handicapName+"</option>";
            	});
            	$div.find("[name=handicapId]").html(handicap_options);
        	}
        }
	});
	//盘口切换时候，重新加载第三方
	$div.find("[name=handicapId]").bind("change",function(){
    	//先清空商户号
    	$div.find("[name=thirdNumber]").html('<option value="">请先选择盘口</option>');
    	//查询选择的第三方、盘口下所拥有的商户号
		var result4thirdNumber=searchThirdInfoCommon({"handicapId":$div.find("[name=handicapId]").val(),"thirdName":$div.find('[name=thirdName_multiselect]').val() });
		if(result4thirdNumber){
    		//重新加载商户号
        	var thirdNumber_options='<option value="">请选择</option>';
        	$.each(result4thirdNumber,function(index,record){
        		thirdNumber_options+="<option  value='"+record.account+"' accountId='"+record.id+"' >"+record.account+"</option>";
        	});
        	$div.find("[name=thirdNumber]").html(thirdNumber_options);
    	}
	});
	//选择的商户号切换时
	$div.find("[name=thirdNumber]").bind("change",function(){
		$div.find("#accountId").val($div.find("[name=thirdNumber] option:selected").attr("accountId"));
	});
	$div.find("#btnDoEditThirdData").bind("click",function(){
		//确定按钮绑定事件
	    var validateEmpty=[
	    	{ele:$div.find("[name=handicapId]"),name:'盘口'},
	    	{ele:$div.find("[name=thirdName_multiselect]"),name:'第三方名称'},
	    	{ele:$div.find("[name=thirdNumber]"),name:'商户号'},
	    	{ele:$div.find("[name=thirdNameUrl]"),name:'第三方网址'},
	    	{ele:$div.find("[name=loginAccount]"),name:'登录账号'},
	    	{ele:$div.find("[name=loginPass]"),name:'登录密码'},
	    	{ele:$div.find("[name=payPass]"),name:'支付密码'}
	    ];
	    if(!validateEmptyBatch(validateEmpty)){
	        setTimeout(function(){       
	            $('body').addClass('modal-open');
	        },500);
	    	return;
	    }
		var param={
				"accountId":$div.find("#accountId").val(),
				"handicapId":$div.find("[name=handicapId]").val(),
				"thirdName":$div.find('[name=thirdName_multiselect]').val(),
				"thirdNumber":$div.find("[name=thirdNumber]").val().trim(),
				"thirdNameUrl":$div.find("[name=thirdNameUrl]").val().trim(),
				"loginAccount":$div.find("[name=loginAccount]").val().trim(),
				"loginPass":$div.find("[name=loginPass]").val().trim(),
				"payPass":$div.find("[name=payPass]").val().trim()
		}
		if(thirdDataId){//修改 且已初始化过
			param.id=thirdDataId;
		}
		$.ajax({
	        contentType: 'application/json;charset=UTF-8',
			type:"POST",
			dataType:'JSON',
			url:'/r/thirdInfo/edit',
			async:false,
	        data:JSON.stringify(param),
			success:function(jsonObject){
		        if(jsonObject.status == 1){
		        	//操作成功提示
		        	showMessageForSuccess("保存成功！");
		        	$div.modal("toggle");
		        	getInThirdAccountList(true);//异步刷新可绑定的数据
		        }else{
		        	showMessageForFail("保存失败："+jsonObject.message);
		        }
		    }
		});
	});
	if(isUpdate){//修改
		//值填充
		$div.find("#accountId").val(thirdDataInfo.accountId);
		$div.find("[name=thirdName]").val(thirdDataInfo.thirdName);
		$div.find("[name=handicapId]").html("<option  value="+thirdDataInfo.handicapId+" >"+thirdDataInfo.handicapName+"</option>");
		$div.find("[name=thirdNumber]").html("<option  value="+thirdDataInfo.thirdNumber+" >"+thirdDataInfo.thirdNumber+"</option>");
		if(thirdDataId){
			$div.find("[name=thirdNameUrl]").val(thirdDataInfo.thirdNameUrl);
			$div.find("[name=loginAccount]").val(thirdDataInfo.loginAccount);
			$div.find("[name=loginPass]").val(thirdDataInfo.loginPass);
			$div.find("[name=payPass]").val(thirdDataInfo.payPass);
		}
	}
	$div.on('hidden.bs.modal', function () {
		// 关闭窗口清除model
		if(fnName){
			fnName(0);
		}
		$div.remove();
	});
}

var getThirdDataInfoById = function (thirdDataId) {
	var result='';
	$.ajax({type:"GET",url:"/r/thirdInfo/findById?id="+thirdDataId,dataType:'json',async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取第三方资料信息异常");
			}
		}
	});
	return result;
}

$("#thirdData_accountFilter").find("[name=third_search_IN_handicapId]").change(function(){
	showThirdDataList(0);
});
$("#thirdData_accountFilter").find("[name=thirdData_search_EQ_status]").click(function(){
	showThirdDataList(0);
});
//查询 盘口参数下 在用停用冻结  未被绑定的入款第三方（去重）：
var getInThirdAccountList=function(is_async){
	$.ajax({
	       dataType: 'JSON',
	       type: "GET",
	       async: is_async,//同步或异步
	       url: "/r/thirdInfo/unBindThirdAccount?handicapId="+handicapId_list.toString(),
	       success: function (jsonObject) {
	           if (jsonObject.status == 1) {
	           		thirdAccountList=jsonObject.data;
	           }
	       }
	});
}
/**
 * 查询第三方所属的盘口列表（param:handicapId  thirdName) 
 * 查询第三方和盘口拥有的商户号列表（param:handicapId单个id  thirdName) 
 */
var searchThirdInfoCommon=function(param){
	var result="";
	$.ajax({
	       dataType: 'JSON',
	       type: "GET",
	       async: false,//同步
	       url: "/r/thirdInfo/unBindThirdAccount",
	       data:param,
	       success: function (jsonObject) {
	           if (jsonObject.status == 1) {
	        	   result=jsonObject.data;
	           }
	       }
	});
	return result;
}
setTimeout(function(){
	getInThirdAccountList(true);//页面加载 异步查询第三方账号列表
},1000);