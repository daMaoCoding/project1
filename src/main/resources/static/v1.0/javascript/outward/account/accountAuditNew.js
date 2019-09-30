currentPageLocation = window.location.href;
var typeALL=[accountTypeInBank,accountTypeOutBank,accountTypeReserveBank,accountTypeBindWechat,accountTypeBindAli,accountTypeThirdCommon,accountTypeBindCommon,accountTypeBindCustomer];

function SpanGrid(tabObj,colIndex){
	if(tabObj != null){
		var i,j;
		var intSpan;
		var strTemp;
		for(i = 0; i < tabObj.rows.length; i++){
			intSpan = 1;
			strTemp = tabObj.rows[i].cells[colIndex].innerText;
			if(strTemp==""||strTemp==null){
				continue;
			}
			for(j = i + 1; j < tabObj.rows.length; j++){
				if(strTemp == tabObj.rows[j].cells[colIndex].innerText){
					intSpan++;
					tabObj.rows[i].cells[0].rowSpan  = intSpan;
					tabObj.rows[i].cells[7].rowSpan  = intSpan;
					tabObj.rows[i].cells[6].rowSpan  = intSpan;
					tabObj.rows[i].cells[8].rowSpan  = intSpan;
					tabObj.rows[i].cells[9].rowSpan  = intSpan;
					var height=(intSpan+2);
					var ht=intSpan;
					//设置文本居中
					tabObj.rows[i].cells[0].style="line-height:"+height;
					tabObj.rows[i].cells[7].style="line-height:"+height;
					tabObj.rows[i].cells[6].style="line-height:"+height;
					tabObj.rows[i].cells[8].style="line-height:"+height;
					tabObj.rows[i].cells[9].style="line-height:"+height;
					tabObj.rows[j].cells[0].style.display = "none";
					tabObj.rows[j].cells[7].style.display = "none";
					tabObj.rows[j].cells[6].style.display = "none";
					tabObj.rows[j].cells[8].style.display = "none";
					tabObj.rows[j].cells[9].style.display = "none";
				}
				else
				{
					break;
				}
			}
			i = j - 1;
		}
	}
}


function derating(id,margin){
	$('#typeName').empty().html("降额");
	$('#contn').empty().html("降低金额");
	$("#Remark").val("");
	$("#derating_amount").val("");
	$('#showTips').hide();
	$('#Remark_modal').modal('show');
	$('#totalTaskFinishBTN').attr('onclick', 'save_derating('+id+','+margin+');');
}

function deductAmount(uid){
	$('#typeName').empty().html("扣除佣金");
	$('#contn').empty().html("扣除佣金");
	$("#Remark").val("");
	$("#derating_amount").val("");
	$('#Remark_modal').modal('show');
	$('#showTips').show();
	$('#totalTaskFinishBTN').attr('onclick', 'save_deductAmount('+uid+');');
}

function save_derating(id,margin){
	$('#totalTaskFinishBTN').attr("disabled",true);
	var remark=$.trim($("#Remark").val());
	if(remark==""){
		$('#prompt_remark').show(10).delay(1500).hide(10);
		$('#totalTaskFinishBTN').attr("disabled",false);
		return;
	}
	var amount=$.trim($("#derating_amount").val());
	if(amount>margin||amount==""){
		$('#prompt_derating_amount').show(10).delay(1500).hide(10);
		$('#totalTaskFinishBTN').attr("disabled",false);
		return;
	}

	$.ajax({
		async:true,
		type:'post',
		url:'/r/account/derating',
		data:{'id':id,'amount':amount,'remark':remark},
		dataType:'json',
		success:function (res) {
			if(res.status == 1){
				$('#totalTaskFinishBTN').attr("disabled",false);
				$('#Remark_modal').modal('hide');
				showRebateUser();
				$("#Remark").val("");
			}else{
				$('#totalTaskFinishBTN').attr("disabled",false);
				showMessageForFail(res.message);
			}
		}
	});
}

function save_deductAmount(uid){
	$('#totalTaskFinishBTN').attr("disabled",true);
	var remark=$.trim($("#Remark").val());
	if(remark==""){
		$('#prompt_remark').show(10).delay(1500).hide(10);
		$('#totalTaskFinishBTN').attr("disabled",false);
		return;
	}
	var amount=$.trim($("#derating_amount").val());
	if(amount==""){
		$('#prompt_derating_amount').show(10).delay(1500).hide(10);
		$('#totalTaskFinishBTN').attr("disabled",false);
		return;
	}

	$.ajax({
		async:true,
		type:'post',
		url:'/r/account/deductamount',
		data:{'uid':uid,'amount':amount,'remark':remark},
		dataType:'json',
		success:function (res) {
			if(res.status == 1){
				$('#totalTaskFinishBTN').attr("disabled",false);
				$('#Remark_modal').modal('hide');
				showRebateUser();
				$("#Remark").val("");
			}else{
				$('#totalTaskFinishBTN').attr("disabled",false);
				showMessageForFail(res.message);
			}
		}
	});
}


//兼职信息页签
var information=false;
//降额按钮
var deratingButton=false;

$.each(ContentRight['AccountAudit:*'], function (name, value) {
	if (name == 'AccountAudit:information:*') {
		information = true;
	}
	if (name == 'AccountAudit:derating:*') {
		deratingButton = true;
	}
});


var showRebateUser=function(){
	$("#tabStatus").val(5);
	var CurPage=$("#accountList_page").find(".Current_Page").text();
	$("#hideShowHtml").show();
	$("#rebate_s").show();
	$("#rebate_i").show();
	$("#status").show();
	$("#purpose").hide();
	$("#userName").show();
	$("#quota").show();
	$("#type").hide();
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountList_page .Current_Page").text();
	var $div = $("#accountFilter");
	var statusToArray = new Array();
	var type=$div.find("[name=search_EQ_accountType]").val();
	if(!type){
		type=typeALL;
	}
	//待提额 汇总
	$div.find("input[name='search_IN_status']:checked").each(function(){
		statusToArray.push(this.value);
	});
	if(statusToArray.length==0){
		statusToArray=[accountStatusNormal,accountStatusStopTemp,accountStatusEnabled,accountStatusFreeze,accountStatusDelete,accountInactivated,accountActivated];
	}
	var data = {
		handicapId:$div.find("select[name='search_EQ_handicapId']").val(),
		bankType:$div.find("select[name='search_LIKE_bankType']").val(),
		typeToArray:type.toString(),
		subType:"",
		alias:$.trim($div.find("input[name='search_EQ_alias']").val()),
		account:$.trim($div.find("input[name='search_LIKE_account']").val()),
		owner:$.trim($div.find("input[name='search_LIKE_owner']").val()),
		flag:$div.find("input[name='search_EQ_flag']:checked").val(),
		currSysLevel:$div.find("input[name='currSysLevel']:checked").val(),
		statusToArray:statusToArray.toString(),
		rebateUser:$.trim($div.find("input[name='rebateUsername']").val()),
		startAmount:$.trim($div.find("input[name='startamount']").val()),
		endAmount:$.trim($div.find("input[name='endamount']").val()),
		pageNo:CurPage<=0?0:CurPage-1,
		pageSize:$.session.get('initPageSize')
	};
	var requesturl = "/r/account/showRebateUser";
	//发送请求
	$.ajax({
		dataType:'JSON',
		type:"POST",
		async:false,
		url:requesturl,
		data:data,
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				$("#activing_show,#actived_show,#others_show").hide();
				$("#rebateUser_show").show();
				//小计
				var counts=0;
				var tr = '';
				var idList=new Array();
				for(var index in jsonObject.data.arrlist){
					var val = jsonObject.data.arrlist[index];
					idList.push({'id':val.id});
					tr += '<tr>'
						+'<td>' + val.userName + '</td>'
						+'<td>'+ val.handicapName +'</td>'
						+'<td>' + val.currSysLevelName + '</td>'
						+'<td>' + (null==val.alias?"":val.alias) + '</td>';

					tr+="<td>" +getAccountInfoHoverHTML(val) +"</td>";
					if(val.status==accountStatusFreeze){
						tr+="<td><span class='label label-sm label-danger'>"+val.createTimeStr+"</span></td>";
					}else if(val.status==accountStatusStopTemp){
						tr+="<td><span class='label label-sm label-primary'>"+val.createTimeStr+"</span></td>";
					}else if(val.status==accountInactivated){
						tr+="<td><span class='label label-sm label-warning'>"+val.createTimeStr+"</span></td>";
					}else if(val.status==accountStatusExcep){
						tr+="<td><span class='label label-sm label-danger'>"+val.createTimeStr+"</span></td>";
					}else if(val.status==accountStatusDelete){
						tr+="<td><span class='label label-sm label-inverse'>"+val.createTimeStr+"</span></td>";
					}else{
						tr+="<td><span class='label label-sm label-success'>"+val.createTimeStr+"</span></td>";
					}
					if (_checkObj(val.remark)) {
						if (_checkObj(val.remark).length > 23) {
							tr += '<td>'
								+ '<a  class="bind_hover_card breakByWord"  title="备注信息"'
								+ 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
								+ ' data-content="' + val.remark + '">'
								+ val.remark.replace(/<br>/g, "").substring(0, 4)
								+ '</a>'
								+ '</td>';

						} else {
							tr += '<td>'
								+ '<a class="bind_hover_card breakByWord"  title="备注信息"'
								+ 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
								+ ' data-content="' + val.remark + '">'
								+ val.remark
								+ '</a>'
								+ '</td>';
						}
					} else {
						tr += '<td></td>';
					}
					tr+='<td>' + val.margin + '</td>'
						+'<td>' + val.limitPercentage + '</td>'
						+ '<td>'
						+ '<a  class="bind_hover_card breakByWord"  title="扣佣金信息"'
						+ 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
						+ ' data-content="' + val.city + '">'
						+ val.minInAmount
						+ '</a>'
						+ '</td>';
					tr+='<td>';
					if(val.userName!=""&&val.userName!=null&&val.margin>1000 && deratingButton){
						tr+='<button type="button" onclick="derating('+val.id+','+val.margin+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-reply  bigger-100 red"></i>降额</button>';
					}
					if(val.userName!=""&&val.userName!=null && deratingButton){
						tr+='<button type="button" onclick="deductAmount('+val.uid+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon glyphicon glyphicon-minus  bigger-100 orange"></i>扣除佣金</button>';
					}
					if(deratingButton){
						tr+='<button type="button" onclick="resetbankAmount('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon glyphicon glyphicon-time  bigger-100 green"></i>余额清零</button>';
					}
					if(deratingButton){
						tr+='<button type="button" onclick="recalculate('+val.uid+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-exchange bigger-100 orange"></i>计算额度</button>';
					}
					tr+="<button class='btn btn-xs btn-white btn-primary btn-bold "+OperatorLogBtn+"' " +
						"onclick='showModal_accountExtra("+val.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
					tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
						"onclick='showInOutListModal("+val.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
					tr+='</td>';
					+'</tr>';
					counts +=1;
				};
				var $tbody=$("#accountListTable tbody");
				$tbody.empty().html(tr);
				SpanGrid(total_tbody_match,0);
				var trs = '<tr>'
					+'<td colspan="11">小计：'+counts+'</td>'
					+'</tr>';
				$tbody.append(trs);
				var trn = '<tr>'
					+'<td colspan="11">总计：'+jsonObject.data.page.totalElements+'</td>'
					+'</tr>';
				$tbody.append(trn);
				showPading(jsonObject.data.page,"accountList_page",showRebateUser);
			}else{
				var $tbody=$("#accountListTable tbody");
				$tbody.empty().html('<tr><td colspan="10">无数据</td></tr>');
			}
			$("[data-toggle='popover']").popover();
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
		}
	});
}



/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	if(information)
		$('#info').show();
	if($("#tabStatus").val()==5){
		showRebateUser();
		$("#rebate_s").show();
		$("#rebate_i").show();
		$("#status").show();
		$("#purpose").hide();
		$("#userName").show();
		$("#quota").show();
		$("#type").hide();
		return;
	}else{
		$("#rebate_s").hide();
		$("#rebate_i").hide();
		$("#status").hide();
		$("#purpose").show();
		$("#userName").hide();
		$("#quota").hide();
		$("#type").show();
	}
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountList_page .Current_Page").text();
	//封装data
	var $div = $("#accountFilter");
	var statusToArray = new Array();
	if($("#tabStatus").val()*1==1){
		//未激活
		statusToArray=[accountInactivated];
	}else if($("#tabStatus").val()*1==2){
		//已激活
		statusToArray=[accountActivated];
	}else{
		//待提额 汇总
		$div.find("input[name='search_IN_status']:checked").each(function(){
			statusToArray.push(this.value);
		});
		if(statusToArray.length==0){
			statusToArray=[accountStatusNormal,accountStatusFreeze,accountStatusExcep,accountStatusStopTemp,accountStatusEnabled];
		}
	}
	var type=$div.find("[name=search_EQ_accountType]").val();
	if(!type){
		type=typeALL;
	}
	var data = {
		search_LIKE_account:$.trim($div.find("input[name='search_LIKE_account']").val()),
		search_LIKE_bankType:$.trim($div.find("[name='search_LIKE_bankType']").val()),
		search_LIKE_owner:$.trim($div.find("input[name='search_LIKE_owner']").val()),
		search_EQ_alias:$.trim($div.find("input[name='search_EQ_alias']").val()),
		currSysLevel:$div.find("input[name='currSysLevel']:checked").val(),
		statusToArray:statusToArray.toString(),
		typeToArray:type.toString(),
		search_EQ_flag:$div.find("input[name='search_EQ_flag']:checked").val(),
		search_NOTEQ_flag:0,//不查PC
		pageNo:CurPage<=0?0:CurPage-1,
		pageSize:$.session.get('initPageSize')
	};
	if($div.find("[name=search_EQ_accountType]").val()==accountTypeInBank){
		data.search_EQ_subType=$div.find("[name='search_EQ_accountType'] option:selected").attr("subType");
	}
	var requesturl = "";
	if($("#tabStatus").val()*1==4){
		//待提额
		requesturl = "/r/account/toberaised";
		data.handicapId=$div.find("select[name='search_EQ_handicapId']").val();
	}else{
		//未激活 已激活 汇总
		requesturl = "/r/account/list";
		data.search_IN_handicapId=$div.find("select[name='search_EQ_handicapId']").val();
		if(!data.search_IN_handicapId){
			data.search_IN_handicapId=handicapId_list.toString();
		}
	}
	//发送请求
	$.ajax({
		dataType:'JSON',
		type:"POST",
		async:false,
		url:requesturl,
		data:data,
		success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			//已匹配和汇总 增加信用额度
			var showActiving=($("#tabStatus").val()*1==1);
			var showActived=($("#tabStatus").val()*1==2);
			var showOther=($("#tabStatus").val()*1==3||$("#tabStatus").val()*1==4);
			$("#activing_show,#actived_show,#others_show").hide();
			if(showActiving){
				$("#activing_show").show();
			}else if(showActived){
				$("#actived_show").show();
			}else{
				$("#others_show").show();
			}
			var $tbody=$("#accountListTable tbody");
			$tbody.html("");
			var totalBalanceByBank =0 ,idList=new Array(),idArray = new Array();
			$.each(jsonObject.data,function(index,record){
				!record.bankBalance?record.bankBalance=0:null;
				totalBalanceByBank+=record.bankBalance*1;
				idArray.push(record.id);
				var tr="";
				tr+="<td><span>"+record.handicapName+"</span></td>";
				if(showOther) {
					tr += "<td><span>" + _checkObj(record.currSysLevelName) + "</span></td>";
				}
				tr+="<td><span>"+_checkObj(record.alias)+"</span></td>";
				//手机余额状态
				tr+="<td><span>"+hideAccountAll(record.mobile,true)+"</span><span target='"+record.id+"' class='time4St bal label label-grey' style='display:block;width:99%;float:left;' >离线</span></td>";
				idList.push({'id':record.id});
				tr+="<td>" +getAccountInfoHoverHTML(record) +"</td>";
				//不同状态使用不同颜色
				if(record.status==accountStatusFreeze){
					tr+="<td><span class='label label-sm label-danger'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountStatusStopTemp){
					tr+="<td><span class='label label-sm label-primary'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountInactivated){
					tr+="<td><span class='label label-sm label-warning'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountStatusExcep){
					tr+="<td><span class='label label-sm label-inverse'>"+record.statusStr+"</span></td>";
				}else{
					tr+="<td><span class='label label-sm label-success'>"+record.statusStr+"</span></td>";
				}
				if(showOther) {
					tr += "<td><span class=''>" + record.typeStr + "</span></td>";
				}
				//余额
				tr+='<td>';
				tr+="<span>"+record.bankBalance+htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily)+"</span>";
				//手机流水状态
				tr+='<span class="errorAlarm error'+record.id+'"></span><span target="'+record.id+'" class="time4St flow label label-grey" style="display:block;width:99%;float:left;" >&nbsp;&nbsp;</span>';
				tr+='</td>';
				if(showOther || showActived){
					tr+="<td><span class=''>"+_checkPeakBalance(record.peakBalance)+"</span></td>";
				}
				//操作  修改/修改 冻结/激活 操作记录  明细
				tr+="<td>";
				var enableBtn="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAudit:Enable:*' \
                        onclick='showUpdateOutAccount("+record.id+",showAccountList,false,true)'>\
                        <i class='ace-icon fa fa-exchange bigger-100 red'></i><span>启用</span></button>";
				var enableLimitBtn="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAudit:Enable:*'  \
                        onclick='showUpdateflag2Account("+record.id+",showAccountList,true)'>\
                        <i class='ace-icon fa fa-exchange bigger-100 red'></i><span>启用</span></button>";
				if($("#tabStatus").val()*1==1){
					//未激活
					tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAudit:Activation:*'  \
                        onclick='activeAccount("+record.id+")'>\
                        <i class='ace-icon fa fa-check-square-o bigger-100 orange'></i><span>激活</span></button>";
				}else if($("#tabStatus").val()*1==2){
					//已激活
					if(record.flag&&record.flag*1==2){
						tr+=enableLimitBtn;
					}else{
						//其它账号
						tr+=enableBtn;
					}
				}else{
					//汇总
					tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentRight='AccountAudit:Update:*' \
	                        onclick='showUpdateAccountNewModal("+record.id+",showAccountList)'>\
	                        <i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
				}
				tr+="<button class='btn btn-xs btn-white btn-success btn-bold "+isHideAccountBtn+"'  \
	                onclick='showModal_accountBaseInfo("+record.id+")'>\
	                <i class='ace-icon fa fa-eye bigger-100 green'></i><span>状态</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-info btn-bold "+OperatorLogBtn+" ' \
                    onclick='showModal_accountExtra("+record.id+")' >\
                    <i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
					"onclick='showInOutListModal("+record.id+")'>"+
					"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+="</td>";
				$tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
			});
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var status = $("#tabStatus").val();
				var totalRows;
				if(status == 1 || status == 2){
					totalRows={
						column:12,
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements
					};
					totalRows[6]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
				}else{
					totalRows={
						column:13,
						subCount:jsonObject.data.length,
						count:jsonObject.page.totalElements
					};
					totalRows[8]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
				}
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.page,"accountList_page",showAccountList,null,true);
			contentRight();
			rereshAccSt(idArray);
		}
	});
}

var rereshAccSt = function(idList){
	$.ajax({type:'get',url:'/r/cabana/status',data: {accIdArray:idList.toString()},dataType:'json',success:function (res) {
			if (res.status==1&&res.data&&res.data.length>0) {
				var nowTm = new Date().getTime();
				$('span.errorAlarm').html('');
				$('span.time4St.bal').text('离线').attr('class','time4St bal label label-grey').attr('style','display:block;width:99%;float:left;');
				$('span.time4St.flow').html('&nbsp;&nbsp;').attr('class','time4St flow label label-grey').attr('style','display:block;width:99%;float:left;');
				res.data.forEach(function(val,index) {
					if(val.error){
						var error = '';
						error = error + '<span class="badge badge-transparent tooltip-error" title="'+val.error+'" onclick="doError('+val.id+',\''+val.error+'\');">';
						error = error + '  <i class="ace-icon fa fa-exclamation-triangle red bigger-130"></i>';
						error = error + '</span>';
						$('span.errorAlarm.error'+val.id).html(error);
					}
					{
						var classInf = val.time ==0||(val.time > 0 && (nowTm-val.time)< 180000)?'label-success':'label-warning';
						if(val.time && val.time > 0 &&((nowTm-val.time)>= 600000)){
							classInf = 'label-danger';
						}
						$('span.time4St.bal[target='+val.id+']').each(function(){
							$(this).text(val.time >0?(geeTime4Crawl(val.time)):'已连接').attr('class','time4St bal label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
						});
					}
					{
						var classInf = val.logtime ==0||(val.logtime > 0 && (nowTm-val.logtime)< 180000)?'label-success':'label-warning';
						if(val.logtime && val.logtime > 0 &&((nowTm-val.logtime)>= 600000)){
							classInf = 'label-danger';
						}
						$('span.time4St.flow[target='+val.id+']').each(function(){
							$(this).html(val.logtime >0?(geeTime4Crawl(val.logtime)):'&nbsp;&nbsp;').attr('class','time4St flow label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
						});
					}
					$('span.mode4Acc[target='+val.id+']').text((val.time?(val.mode ==1 ?'抓流水':(val.mode ==2?'转账':'默认')):'---'));
					{
						var flag =  val.flag;//1-simulator, 0-phone
						if(flag && flag ==1){
							$('button[smstarget='+val.id+']').attr('class','btn btn-xs btn-white btn-primary btn-bold orange contentRight');
						}else{
							$('button[smstarget='+val.id+']').attr('class','dsn contentRight');
						}
					}
				});
			}
		}});
};



function activeAccount(accountId){
	var accountInfo = getAccountInfoById(accountId);
	bootbox.confirm("请先确保账户（"+accountInfo.bankType+"|"+ accountInfo.owner+"|"+ accountInfo.account.substr(accountInfo.account.length-4)+"）处于正常连接状态，确定向此账户推送一笔测试任务？", function(result) {
		if (result) {
			$.ajax({ dataType:'json',type:"get", url:'/r/account/getActiveTrans',data:{accountId:accountId},success:function(jsonObject){
					if(jsonObject.status == 1){
						if(jsonObject.data){
							var message = "账户（"+accountInfo.bankType+"|"+ accountInfo.owner+"|"+ accountInfo.account.substr(accountInfo.account.length-4)+"）将向"+"<br>"+"账户（"+
								jsonObject.data.bankType+"|"+ jsonObject.data.owner+"|"+ jsonObject.data.account.substr(jsonObject.data.account.length-4)+"）转一笔账"
							showMessageForSuccess(message,2000);
						}else{
							showMessageForSuccess("正在获取到测试转账任务！",2000);
						}
					}
				},error:function(result){  bootbox.alert(result); }});
		}
	});
}

function initAccount(accountId){
	var troubleshoot = "1";
	$.ajax({ dataType:'json',type:"get",async:false, url:'/r/problem/getSysErr',data:{accId:accountId},success:function(jsonObject){
			if(jsonObject.status == -1 && jsonObject.message == "存在待排查数据"){
				troubleshoot = "2";
			}
		},error:function(result){  bootbox.alert(result); }});
	if(troubleshoot == "2"){
		showMessageForSuccess("请移步 问题排查汇总——》账目排查 进行初始化！",2000);
	}else{
		bootbox.confirm("确定对账号ID（"+accountId+"）进行初始化？", function(result) {
			if (result) {
				$.ajax({ dataType:'json',type:"get", url:'/auto/test/initByAccountId',async:false,data:{id:accountId},success:function(jsonObject){
						if(jsonObject.status == 1){
							showMessageForSuccess("初始化账号成功！",2000);
						}
					},error:function(result){  bootbox.alert(result); }});
			}
		});
	}
}

function resetbankAmount(accountId){
	bootbox.confirm("确定对账号ID（"+accountId+"）进行余额清零？", function(result) {
		if (result) {
			$.ajax({ dataType:'json',type:"get", url:'/r/account/resetbankAmount',async:false,data:{id:accountId},success:function(jsonObject){
					if(jsonObject.status == 1){
						showMessageForSuccess(jsonObject.message,2000);
						showRebateUser();
					}
				},error:function(result){  bootbox.alert(result); }});
		}
	});
}

function recalculate(uid){
	$.ajax({ dataType:'json',type:"get", url:'/r/account/recalculate',async:false,data:{uid:uid},success:function(jsonObject){
			if(jsonObject.status == 1){
				showMessageForSuccess(jsonObject.message,2000);
				showRebateUser();
			}else{
				showMessageForFail(jsonObject.message,2000);
			}
		},error:function(result){  bootbox.alert(result); }});
}


function changeStatus(status) {
	$("#tabStatus").val(status);
	$("#rebateUser_show").hide();
	if(status==1){
		//未激活
		$("#hideShowHtml").hide();
	}else if(status==2){
		//已激活
		$("#hideShowHtml").hide();
	}else{
		//汇总 待提额
		$("#hideShowHtml").show();
	}
	showAccountList(0);
}

function _checkPeakBalance(obj) {
	var ob = 0;
	if (obj) {
		ob = obj;
	}
	return ob;
}
function replacePos(strObj, pos, replacetext){
	var str = strObj.substr(0, pos-1) + replacetext + strObj.substring(pos, strObj.length);
	return str;
}
getHandicap_select($("#accountFilter select[name='search_EQ_handicapId']"),null,"全部");
getBankTyp_select($("#accountFilter select[name='search_LIKE_bankType']"),null,"全部");
getAccountType_select_search($("#accountFilter select[name='search_EQ_accountType']"),"全部");
$("#accountFilter").find("[name=search_EQ_handicapId],[name='search_EQ_accountType'],[name='search_LIKE_bankType']").change(function(){
	showAccountList(0);
});
$("#accountFilter").find("[name=search_EQ_flag],[name='search_IN_status'],[name=currSysLevel]").click(function(){
	showAccountList(0);
});
showAccountList(0);