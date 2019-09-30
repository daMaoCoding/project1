/** 公司用款 第三方资料JS */

/**
 * 根据账号Type拼接对应数据
 */
var company_showThirdDataList=function(CurPage){
	var $div=$("#company_third_div");
	if(!!!CurPage&&CurPage!=0) CurPage=$div.find("#company_thirdData_accountPage .Current_Page").text();
	var $filter = $div.find(".filter");
    var data = {
    	handicapId:$filter.find("[name=third_search_IN_handicapId]").val(),
        thirdName:$filter.find("[name=thirdName]").val(),
        status:$filter.find("[name=thirdData_search_EQ_status]:checked").val(),
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize')
    };
    data.queryPage=(companyTab=="all"?1:2);//全部三方资料 1 我的三方资料 2
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
			var $tbody=$("#company_thirdData_Div_accountListTable").find("tbody");
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
				tr+="<td>"+_checkObj(record.sysBalance,false,0)+"</td>";//系统余额
				  //第三方真实余额
				tr += "<td id='company_bankBalance" + record.accountId + "'><span name='bankBalance' >" + record.bankBalance + "</span><i class='red ace-icon fa fa-pencil-square-o' onclick='changeInput_company(" + record.accountId + "," + record.bankBalance + ")' title='校正余额' style='cursor:pointer;'  ></i></td>";
				//提现明细
                tr += getRecording_Td(record.accountId, "detail");
				tr+="<td><a class='contentRight' contentRight='ThirdDrawTask:RemarkSearch:*' id='showModal_remark"+record.id+"'  title='"+_checkObj(record.latestRemark)+"'>"+(record.latestRemark?_checkObj(record.latestRemark.substring(0,20)):"无备注，点击添加")+"</a></td>";//备注
				//操作
				tr+="<td>";
				//修改
				tr+="<button data-placement='top' data-original-title='修改' data-toggle='tooltip'  class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentright='ThirdDrawTask:ThirdUpdate:*'\
					onclick='showEditThirdDataModal("+(record.id?("\""+record.id+"\""):null)+",\""+record.accountId+"\",company_showThirdDataList)'>\
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
			showPading(jsonObject.page,"company_thirdData_accountPage",company_showThirdDataList,null,true);
            loadInOutTransfer(fromIdArray, "detail");
            syncLoadCompanyTotal();//异步加载公司用款相关总计数据
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
function changeInput_company(id, value) {
  $("#company_thirdData_Div_accountListTable").find("#company_bankBalance" + id).find("span").html("<input onkeyup='clearNoNum(this)' id='company_bankBalanceInput" + id + "' class='input-sm' style='width:80px;' value='" + value + "'>");
  $("#company_thirdData_Div_accountListTable").find("#company_bankBalance" + id).find("i").attr("class", "green ace-icon fa fa-check");
  $("#company_thirdData_Div_accountListTable").find("#company_bankBalance" + id).find("i").attr("onclick", "savaBankBalance_company(" + id + ")");
}
function savaBankBalance_company(id) {
  var data = {
      "id": id,
      "bankBalance": $("#company_thirdData_Div_accountListTable").find("#company_bankBalanceInput" + id).val(),
  };
  $.ajax({
      type: "PUT",
      dataType: 'JSON',
      url: '/r/account/update',
      async: false,
      data: data,
      success: function (jsonObject) {
          if (jsonObject.status == 1 && jsonObject.data) {
        	  company_showThirdDataList();
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

getHandicap_select($("#company_third_div [name='third_search_IN_handicapId']"),0,"全部");//下发记录

$("#company_third_div").find("[name=third_search_IN_handicapId]").change(function(){
	company_showThirdDataList(0);
});
$("#company_third_div").find("[name=thirdData_search_EQ_status]").click(function(){
	company_showThirdDataList(0);
});