currentPageLocation = window.location.href;
//引入公司用款 HTML
$("#CompanyMoneyDiv").load("../html/income/asign/thirdDrawCompanyMoney.html");

var isCheckedRow="";//已勾选的数据ID字符串
var pageType = 1;//1 全部 2 我锁定的 3 统计下发失败
var queryStatus_all=1;//全部 子TAB
var queryStatus_mine=1;//我的锁定 子TAB
var isRecordHistory_all=false;//全部 是历史记录
var isRecordHistory_mine=false;//我的锁定 是历史记录
var isThirdData_all=false;//全部 是第三方资料
var isThirdData_mine=false;//我的锁定 是第三方资料
var _outerUlLiClick=function(page){
    pageType = page;
    if(pageType=="3"){//公司用款
        $("#refresh_thirdDrawTask").remove();//销毁定时器需要的div
        //销毁定时器
        var end = setTimeout(function(){},1);
        for(var i=1;i<=end;i++){
            clearTimeout(i);
        }
    	loadTimeLimit30DefaultN($("#companyMoney_list [name=startAndEndTime]"));
    	getHandicap_select($("#companyMoney_list [name='search_IN_handicapId']"), null, "全部");
    	getFlagType_select($("#companyMoney_list [name='search_IN_useName']"));
		changeTab_companyMoney("all");//默认加载 下发任务
	}else{//下发任务 我已锁定
	    changeTabShowHide();
	}
}
var changeSubTab=function(record,status){
	if(pageType==1){
		queryStatus_all=status;
		isRecordHistory_all=record;
		if(status==3){
			isThirdData_all=true;
		}
		changeTabShowHide();
	}else if(pageType==2){
		queryStatus_mine=status;
		isRecordHistory_mine=record;
		if(status==3){//第三方
			isThirdData_mine=true;
		}
		changeTabShowHide();
	}
}
var changeTabShowHide=function(){
	isCheckedRow="";//已勾选的行清空
	$(".finshedRecord").hide();
    $("#refresh_thirdDrawTask").remove();//销毁定时器需要的div
    //销毁定时器
    var end = setTimeout(function(){},1);
//    var start = (end -10)>0?end-10:0;
    for(var i=1;i<=end;i++){
        clearTimeout(i);
    }
    $("[name=thName1]").html("下发耗时");
    $("[name=thName2]").html("总耗时");
    $("#div_refresh").hide();//隐藏定时器
	if(pageType==1){//全部
		$(".mine").hide();
        $('.queryAll').show();
		if(queryStatus_all==3){//第三方统计
			//隐藏任务列表，展示资料列表
		    $(".draw_Div").hide();
		    $(".thirdData_Div").show();
			showThirdDataList(0);
		}else{
		    $(".thirdData_Div").hide();
		    $(".draw_Div").show();
	        if(isRecordHistory_all){//下发记录
	        	$(".historyShow").show();
	        	$(".isRecordHide").hide();
        	    $("[name=thName1]").html("下发耗时<br/>总耗时");
        	    if(queryStatus_all==1){//下发完成记录
        	    	$(".finshedRecord").show();//完成记录汇总相关
        	    }
	        }else{//下发任务
	        	$(".historyShow").hide();
	        	$(".isRecordHide").show();
	            //全部 下发任务TAB  加入识别DIV 加载定时器
	            $("#div_refresh").show();//展示定时器
	            //重新加载定时器
        	    $("#thirdDrawDiv").append($('<input type="hidden" id="refresh_thirdDrawTask"  >'));
	            initRefreshSelect($("#thirdDrawDiv #refreshthirdDrawTaskSelect"),$("#thirdDrawDiv #accountFilter #searchBtn"),80,"refresh_thirdDrawTask");
	            $("#refreshthirdDrawTaskSelect [name=refreshSelect]").val(15);
	            $("#refreshthirdDrawTaskSelect [name=refreshSelect]").change();
	        }
	        thirdDraw(0);
		}
    }else if(pageType==2){//我的锁定
		$('.queryAll').hide();
    	$(".mine").show();
    	if(queryStatus_mine==3){//第三方统计
			//隐藏资料列表，展示任务列表
		    $(".draw_Div").hide();
		    $(".thirdData_Div").show();
    		showThirdDataList(0);
		}else{
		    $(".thirdData_Div").hide();
		    $(".draw_Div").show();
	        if(isRecordHistory_mine){//下发记录
	        	$(".historyShow").show();
	        	$(".isRecordHide").hide();
        	    $("[name=thName1]").html("下发耗时<br/>总耗时");
        	    if(queryStatus_mine==1){//下发完成记录
        	    	$(".finshedRecord").show();//完成记录汇总相关
        	    }
	        }else{//下发任务
	        	$(".historyShow").hide();
	        	$(".isRecordHide").show();
	        	if(queryStatus_mine==1){//正在下发
	        	    $("[name=thName1]").html("系统余额");
	        	    $("[name=thName2]").html("第三方余额");
	        	}
	        }
	        thirdDraw(0);
		}
        
    }else  if(pageType==4){//下发失败统计
        _datePickerForAll($("#drawFailure input.date-range-picker"));
        $('.queryAll').hide();
    	$("#myLockShow").hide();
        drawFailure(0,1);
    }
}
var reset1=function(div){
	if(!div) return;
	$("#"+div).find("input[type=text],input[type=number],[type=file],[type=string]").val("");//刷新input不会触发查询事件
	_datePickerForAll($("[name=History_startAndEndTime]"));//时间input被清空
	$("#"+div).find("input:checkbox").prop("checked","checked");
	$("#"+div).find("input.defaultNoCheck:checkbox").prop("checked",false);
	$("#"+div).find("input.defaultCheck:radio").prop("checked","checked").click();
	$("#"+div).find("select").find("option:first").prop("selected", 'selected');
	$("#"+div).find('select').trigger('chosen:updated');
}

// 全部 我锁定的 查询
function thirdDraw(CurPage){
	var $div=$("#accountFilter");
    if(!!!CurPage&&CurPage!=0){
        CurPage= $("#accountPageThirdTask .Current_Page").text();
    }
    var queryStatus=(pageType==1?queryStatus_all:queryStatus_mine);//不同TAB的子TAB
    var isRecordHistory=(pageType==1?isRecordHistory_all:isRecordHistory_mine);//true:完成/失败  false:下发任务
    var isAll=(pageType==1);//是全部TAB true
    var isMine=(pageType==2);//是我已锁定TAB true
    var isSuccessFinshed=(isRecordHistory&&(queryStatus_all==1||queryStatus_mine==1));//下发完成记录
    var subTab_url=isRecordHistory?'/r/account/findDrawRecord':'/r/account/findDrawTask';
    var data = {
        'pageSize':$.session.get('initPageSize'),
        'handicap':$div.find("select[name='search_EQ_handicapId']").val(),//盘口 为空后端会查有权限的盘口
        'alias':$div.find('[name=alias]').val(),
        'account':$div.find('[name=account]').val(),
        'bankType':$div.find('[name=choiceBankBrand]').val(),
        'owner':$div.find('[name=owner]').val(),
        'status':$div.find('[name=search_IN_status]:checked').val(),
        'cardType':$div.find('[name=cardType]:checked').val(),
        'pageFlag':pageType,
        'pageNo':CurPage<=0?0:CurPage-1
    };
    if(isRecordHistory){
    	data.drawRecordStatus=queryStatus;//查询下发任务  1/2
    	//起始时间
    	var startAndEndTimeToArray=getTimeArray($('[name="History_startAndEndTime"]').val(),'~')
    	if(!startAndEndTimeToArray.toString()){
    		showMessageForFail("请先选择时间范围（最多可查三天）");
    		return;
    	}
        data.startTime=startAndEndTimeToArray[0];
        data.endTime=startAndEndTimeToArray[1];
    }else{
    	data.queryStatus=queryStatus;//查询下发记录 完成 1 失败 2
    	data.currSysLevel=$div.find('[name=currSysLevel]:checked').val();//层级 外层1 内层2 指定层8 不传则全部
    }
    var ajaxData = {
        type:'post',contentType:"application/json;charset=UTF-8",dataType:'json',async:false,data:JSON.stringify(data),url:subTab_url,
        success:function (res) {
            if (res){
                if (-1 == res.status) {
                    showMessageForFail("查询失败：" + res.message);
                    return;
                }
                var tbodyStr = "", idList = new Array(),thirdIdList=new Array();//idListStatus = new Array();
                var bankBalanceCurrentPage = 0,limitOutCurrentPage=0;//下发任务小计
                var amountCurrentPage=0,feeCurrentPage=0;//下发记录小计
                $.each(res.data, function (index, record) {
                    var tr = "";
                    idList.push({'id': record.id});
                    //非历史记录 有复选框
                    if(!isRecordHistory){
                        var choiceInput="";
                        //全部TAB-未下发1（勾选做锁定） || 我已锁定-已锁定2 （勾选做解锁）
                    	if((isAll&&record.isDrawing==1) || (isMine&&record.isDrawing==2) ){
                    		tr +="<td><input type='checkbox' name='checkbox_lockUnlock' class='ace' value='"+record.id+"'><span class='lbl'></span></td>";
                    	}else{
                    		tr +="<td></td>";
                    	}
                    }
                    if(isSuccessFinshed&&(record.payee==2||record.payee==3)){//下发完成记录&&(公司用款||会员出款)
                    	//无编号 层级 状态
                		tr +="<td>无</td>";
                		tr +="<td>无</td>";
                		tr +="<td>无</td>";
                		//只能第三方出款
                		tr +="<td>第三方</td>";
            	    }else{
            	    	 if(isRecordHistory){
                         	//下发记录
                         	 tr +="<td>"+_checkObj(record.alias)+"</td>";
                         }else{
                         	 //非下发记录，编号下展示第三方
                             var showThirdName=(record.thirdAccountId?"<br/>"+record.thirdName:"");//已设定的第三方
                         	tr += "<td><a class='bind_hover_card breakByWord' data-toggle='third_bind_" + record.id + "' data-placement='auto left' data-trigger='hover'>"+ _checkObj(record.alias)+"</a>"+showThirdName+"</td>";
                         }
                         tr += "<td><span>" + (record.currSysLevel?(record.currSysLevel==currentSystemLevelOutter?"外层":(record.currSysLevel==currentSystemLevelInner?"内层":"指定层")):"") + "</span></td>";
                         //状态
                         tr +="<td>"+getStatusInfoHoverHTML(record)+"</td>";
                         //类型 来源
                         tr +="<td>"+_checkObj(record.typeStr)+"<br/>"+(record.flag&&record.flag==2?"<span class='green'>返利网</span>":"<span class='pink'>PC</span>")+"</td>";
            	    }
                  //账号信息
                    tr +="<td style='text-align: left !important;padding-left: 10px !important;'>";
                    if (isAll){
                        //账号
                    	if(isSuccessFinshed&&(record.payee==2||record.payee==3)){//下发完成记录&&(公司用款||会员出款)
                    		tr +=hideAccountAll(record.account);//账号无超链接
                    	}else{
                    		tr +=getAccountInfoHoverHTML(record,"onlyAccount") + (isRecordHistory?"":getCopyHtml( record.account));
                    	}
                        tr +="<br/>";
                        //银行类别 开户人
                        tr +=_checkObj(record.bankType) + (isRecordHistory?"":getCopyHtml(record.bankType));
                        tr +="<br/>";
                        tr +=  hideName(record.owner) + (isRecordHistory?"":getCopyHtml(record.owner));// hideName(record.owner) 需求7455
					} else {
						//账号
						if(isSuccessFinshed&&(record.payee==2||record.payee==3)){//下发完成记录&&(公司用款||会员出款)
	                   		 tr +=hideAccountAll(record.account);//账号无超链接
	                   	}else{
	                   		//hideName(record.owner) 需求7455
	                        tr +=  "<a class='bind_hover_card' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"+
											"<span name='bankType_owner"+record.id+"' >"+record.account+
										"</a>" +(isRecordHistory?"":getCopyHtml( record.account));
	                   	}
                        tr +="<br/>";
                        //银行类别 开户人
                        tr += record.bankType + (isRecordHistory?"":getCopyHtml(record.bankType)); // hideName(record.owner) 需求7455
                        tr +="<br/>";
                        tr += record.owner + (isRecordHistory?"":getCopyHtml(record.owner));// hideName(record.owner) 需求7455
					}
                    tr +="</td>";
                    //第三方信息
                    if(isRecordHistory){
                        thirdIdList.push({'id': record.thirdAccountId, 'type': 'third'});
                        tr += "<td  style='text-align: left !important;padding-left: 10px !important;' >" +
	                        "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + record.thirdAccountId + "' data-placement='auto right' data-trigger='hover'  >" + hideAccountAll(record.thirdAccount) + 
	                        "</a>" +(isRecordHistory?"":getCopyHtml(record.thirdAccount))+"<br/>"+_checkObj(record.thirdName)+(isRecordHistory?"":getCopyHtml(record.thirdName))+
                        "</td>";
                    }
                    //支行
                    tr += "<td>"+_checkObj (record.bankName) +(isRecordHistory?"":getCopyHtml(record.bankName))+ "</td>";
                    bankBalanceCurrentPage  = (parseFloat(bankBalanceCurrentPage)+parseFloat((record.bankBalance ? record.bankBalance : '0'))).toFixed(2);
                    limitOutCurrentPage   = (parseFloat(limitOutCurrentPage)+parseFloat((record.limitOut ? record.limitOut : '0'))).toFixed(0);
                    //收款流水
                    if(!isRecordHistory)tr +="<td>"+_checkObj(record.allInFlowAmount,null,0)+"</td>";
                    //本次下发金额
                    if(!isRecordHistory)tr += "<td>"+ (record.limitOut?record.limitOut:"0") + "</td>";
               	 	//金额 手续费
                    if(isRecordHistory){//完成/失败记录
                        amountCurrentPage+=(record.amount||0);
                        feeCurrentPage+=(record.fee||0);
                        tr +="<td>"+(record.amount||0)+"</td>";
                	    if(isSuccessFinshed){//下发完成记录 新增收款方
                	    	tr +="<td>";
                	    		if(record.payee==0){//下发卡（返利网）
                	    			tr +="下发卡<span class='red'>（返利网）</span>";
                	    		}else if(record.payee==1){//下发卡（PC）
                	    			tr +="下发卡<span class='red'>（PC）</span>";
                	    		}else if(record.payee==2){//公司用款
                	    			tr +="公司用款";
                	    		}else if(record.payee==3){//会员 有会员名称
                	    			tr +="会员<br/><span class='red'>"+_checkObj(record.memberRealName)+"</span>";
                	    		}
                	    	tr +="</td>";
                	    }
                        tr +="<td>"+(record.fee||0)+"</td>";
                    }else{
                    	var amountTd1="<td>--</td><td>--</td>";
                        var amountTd2="<td><span style='display:none' value=" + record.id + " name='ids'>" + record.id + "</span><span id='drawAmount" + record.id + "' >" + (record.drawedAmount?record.drawedAmount:'0') + "</span>&nbsp;&nbsp;<i class='fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target='#drawAmount" + record.id + "'></i></td>";
                        amountTd2+= "<td><span style='display:none' value=" + record.id + " name='ids'>" + record.id + "</span><span id='drawedFee" + record.id + "' >" + (record.drawedFee?record.drawedFee:'0') + "</span>&nbsp;&nbsp;<i class='fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target='#drawedFee" + record.id + "'></i></td>";

                        var oldVal = getTemplateOldValue(record.id);
                        var amountTd3Old = !oldVal || !oldVal[0] || '无'==oldVal[0] ?record.drawedAmount:oldVal[0];
                       	var feeTd3Old = !oldVal || !oldVal[1] || '无'==oldVal[1] ?record.drawedFee:oldVal[1];
                        var amountTd3= "<td>" +
    			                   "<span>" +
    			                   "<input min='0' value='"+amountTd3Old+"' name='drawAmount' onchange='saveAmountOrFee(this,1)'   onkeyup='clearNoNumPoint2(this);' style='width:70px;' value='' class='input-sm' type='text'  id='amount" + record.id + "' >" +
    			                   "</span>" +
                            "<i class='ace-icon fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target=\"#amount" + record.id + "\" ></i>" +
    		                   "</td>"; //金额
                       		amountTd3+="<td>" +
    			                   "<span>" +
    			                   "<input  min='0' value='"+feeTd3Old+"'  name='drawFee' onchange='saveAmountOrFee(this,2)'   onkeyup='clearNoNumPoint2(this);' style='width:50px;' value='' class='input-sm' type='text' id='fee" + record.id + "' >" +
    			                   "</span>" +
                                "<i class='ace-icon fa fa-copy orange  clipboardbtn' style='cursor:pointer' data-clipboard-target=\"#fee" + record.id + "\" ></i>" +
    			               "</td>";//手续费
                        if(isAll){//全部
                    	   if(record.isDrawing==3){//待到账3 可以看到金额
                    		   tr += amountTd2;
                            }else{
                            	//未下发1 已锁定2 什么也看不见
                     		   tr += amountTd1;
                            }
                        }else if(isMine){//我的设定
                        	if(record.isDrawing==3){//待到账3 不可编辑
                     		   tr += amountTd2;
                        	}else{//未下发1 已锁定2  可以手动编辑
                     		   tr += amountTd3;
                        	}
                        }
                    }
                    if(isRecordHistory){//下发记录
                    	//系统余额
                        tr += "<td>"+_checkObj(record.thirdBalance,true)+"</td>";
                        //第三方余额
                        tr += "<td>"+_checkObj(record.thirdBankBalance,false,"0")+"</td>";
                        //下发耗时  总耗时 悬浮查看开始时间，结束时间
                    	var timeTitle1="开始："+timeStamp2yyyyMMddHHmmss(record.createTime)+"&#13;结束："+timeStamp2yyyyMMddHHmmss(record.updateTime);
                    	var timeTitle2="开始："+timeStamp2yyyyMMddHHmmss(record.createTimeTotal)+"&#13;结束："+timeStamp2yyyyMMddHHmmss(record.updateTime);
                    	tr += "<td><a title='"+timeTitle1+"'>";
	                    	tr += changeColor4LockTime(record.timeConsuming*1000);
	                    	tr += "</a><br/><a title='"+timeTitle2+"'>";
	                    	tr += formatDuring(record.addTime*1000);//总耗时不带颜色
                    	tr += "</a></td>";
                    	//下发人
                        tr +="<td>"+_checkObj(record.lockerName)+"</td>";
                    }else{//下发任务
                    	//银行余额
                        tr += "<td><div class='BankLogEvent' target="+record.id+"><span class='amount'>"+ (record.bankBalance ? record.bankBalance : '0') +"</span><span class='time'></span></div></td>";
                    	if(queryStatus_mine==1&&pageType==2){//我已锁定 正在下发
                    		//系统余额
                            tr += "<td>"+_checkObj(record.thirdBalance,true)+"</td>";
                            //第三方余额
                            if(record.thirdAccountId){
                            	record.thirdBankBalance=(record.thirdBankBalance?record.thirdBankBalance:0);//已绑定第三方，但是没有初始化过第三方
                            	var value_thirdBankBalance=getThirdBankBalance(record.id);
                            	var value_thirdBankBalance=value_thirdBankBalance?value_thirdBankBalance:"";
                            	tr+="<td>" +
			 			                   "<span>" +
			 			                   "<input  min='0'  value='"+value_thirdBankBalance+"' onchange='saveThirdBankBalance(this,"+record.id+")'     name='thirdBankBalance'  onkeyup='clearNoNumPoint2(this);' style='width:70px;'  style='width:50px;' value='' class='input-sm' type='text' id='thirdBankBalance" + record.id + "' >" +
			 			                   "</span>" +
			 			               "</td>";
                            }else{
                            	tr += "<td>--</td>";
                            }
                    	}else{//等待到账
                    		 //全部TAB 正在下发才展示状态  isDrawing：未下发 1 已锁定 2 待到账 3
                            var drawStatus=((isAll&&queryStatus_all==2)?(record.isDrawing==1?"<span class='blue'>未下发</span>":(record.isDrawing==2?"<span class='green'>已锁定</span>":"<span class='red'>待到账</span>")):"");
                            //下发耗时
                            tr += "<td>"+changeColor4LockTime(record.lockTime)+"<br/>"+drawStatus+"</td>";
                            // 总耗时
                            tr += "<td>"+formatDuring(record.addTime)+"</td>";
                    	}
                    }
                    //操作
                    if(!isRecordHistory){
                    	tr +="<td> ";
                    	var lockBtn="<button data-placement='top' data-original-title='锁定' data-toggle='tooltip' class='btn btn-xs btn-white btn-bold btn-danger orange contentRight' contentRight='ThirdDrawTask:LockUnlock:*' onclick='lockThirdDraw("+record.id+",1)'><i class='ace-icon fa fa-lock bigger-100 orange'></i></button>&nbsp;";
                     	var unlockBtn="<button data-placement='top' data-original-title='解锁' data-toggle='tooltip' class='btn btn-xs btn-white btn-bold btn-success green contentRight' contentRight='ThirdDrawTask:LockUnlock:*' onclick='lockThirdDraw("+record.id+",2)'><i class='ace-icon fa fa-unlock bigger-100 green'></i></button>&nbsp;";
                     	var encashBtn="<button data-placement='top' data-original-title='完成提现' data-toggle='tooltip' class='btn btn-xs btn-white btn-danger btn-bold contentRight' contentRight='ThirdDrawTask:DoEncash:*' onclick='doEnCash_saveThirdTrans(" + record.id + "," +( record.thirdBalance ? record.thirdBalance :0)+ ")'><i class='ace-icon fa fa-check bigger-100 red'></i></button>";
                        var drawFailedBtn="<button data-placement='top' data-original-title='下发失败' data-toggle='tooltip' class='btn btn-xs btn-white btn-danger btn-bold contentRight' contentRight='ThirdDrawTask:DrawFailed:*' onclick='do_reject2CurrSys(" + record.id + ")'><i class='ace-icon fa fa-undo bigger-100 red'></i></button>";
                     	if(isAll){//全部
                         	if (record.lockerName){
                                 tr +="<span class='badge badge-warning'><i class='ace-icon fa fa-lock bigger-100'></i>&nbsp;"+_checkObj(record.lockerName)+"</span>";
                             }else {
                                 tr +=lockBtn;
                             }
                     	}else{//我的锁定         isDrawing： 已锁定 2 待到账 3
                     		if(record.isDrawing==2){//已锁定2
                    			tr +=unlockBtn;//解锁
                     			tr +=encashBtn;//完成提现
                     		}else	if(record.isDrawing==3){
                     			tr +=drawFailedBtn;//下发失败按钮
                     		}
                     	}
                     	//操作记录 明细
                         tr +=" <button data-placement='top' data-original-title='操作记录' data-toggle='tooltip' class='btn btn-xs btn-white btn-bold btn-info contentRight' contentRight='ThirdDrawTask:OperateLog:*' onclick='showModal_accountClick(\""+record.id+"\")'><i class='ace-icon fa fa-list bigger-100 blue'></i></button>";
                         tr +=" <button data-placement='top' data-original-title='明细' data-toggle='tooltip' class='btn btn-xs btn-white btn-bold btn-info ' onclick='showInOutListModal(\""+record.id+"\")'><i class='ace-icon fas fa-ellipsis-h bigger-100 blue'></i></button></td>";
                    }
                    tbodyStr+="<tr>"+tr+"</tr>";
                });
                if (tbodyStr){
                	if(isRecordHistory){
                		var tr1 = "<tr><td colspan='7'>小计:"+res.data.length+"</td>" +
                				"<td  bgcolor='#579EC8' style='color:white;font-size:10px;'>"+setAmountAccuracy(amountCurrentPage)+"</td>" +
                				"<td></td>" +
                				"<td  bgcolor='#579EC8' style='color:white;font-size:10px;'>"+setAmountAccuracy(feeCurrentPage)+"</td>" +
                				"<td colspan='4'></td>" +
                			"</tr>";
            			var tr2 = "<tr><td colspan='7'>总计:"+res.page.totalElements+"</td>" +
            					"<td  bgcolor='#D6487E' style='color:white;font-size:10px;'>"+setAmountAccuracy(res.page.header.sumAmount?res.page.header.sumAmount:0)+"</td>" +
                				"<td></td>" +
                				"<td  bgcolor='#D6487E' style='color:white;font-size:10px;'>"+setAmountAccuracy(res.page.header.sumFee?res.page.header.sumFee:0)+"</td>" +
            					"<td colspan='4'></td>" +
            				"</tr>";
                	}else{
            			var tr1 = "<tr><td colspan='8'>小计:"+res.data.length+"</td>" +
            					"<td  bgcolor='#579EC8' style='color:white;font-size:10px;'>"+setAmountAccuracy(limitOutCurrentPage)+"</td>" +
            					"<td colspan='2'></td>" +
            					"<td  bgcolor='#579EC8' style='color:white;font-size:10px;'>"+setAmountAccuracy(bankBalanceCurrentPage)+"</td>" +
            					"<td colspan='3'></td>" +
            				"</tr>";
            			var tr2 = "<tr><td colspan='8'>总计:"+res.page.totalElements+"</td>" +
            					"<td  bgcolor='#D6487E' style='color:white;font-size:10px;'>"+setAmountAccuracy(res.page.header.singleDrawSum?res.page.header.singleDrawSum:0)+"</td>" +
            					"<td colspan='2'></td>" +
            					"<td  bgcolor='#D6487E' style='color:white;font-size:10px;'>"+setAmountAccuracy(res.page.header.sumBankBalance?res.page.header.sumBankBalance:0)+"</td>" +
            					"<td colspan='3'></td>" +
            				"</tr>";
                	}
                    tbodyStr = tbodyStr+tr1+tr2;
                }
                $('#draw_Div_accountListTable').find('tbody').html(tbodyStr);
    			contentRight();
                showPading(res.page,"accountPageThirdTask",thirdDraw);
                loadHover_accountInfoHover(idList);//账号悬浮提示
                loadHover_accountBindThird(idList);//指定第三方悬浮提示
                if(isRecordHistory){//完成记录
                    loadHover_accountInfoHover(thirdIdList);//第三方账号悬浮提示
                }
                //加载总计
                if(res.page&&res.page.header){
                	var total=res.page.header;
                	$("#allTotal").text(total.queryStatus1InAllCount*1+total.queryStatus2InAllCount*1);//全部
                	$("#queryStatus1InAllCount").text(total.queryStatus1InAllCount);//新任务
                	$("#queryStatus2InAllCount").text(total.queryStatus2InAllCount);//下发中
                	
                	$("#mineTotal").text(total.queryStatus1ByOneCount*1+total.queryStatus2ByOneCount*1);//我的锁定
                	$("#queryStatus1ByOneCount").text(total.queryStatus1ByOneCount);//下发中
                	$("#queryStatus2ByOneCount").text(total.queryStatus2ByOneCount);//等待到账
                	
                    if(isRecordHistory&&(queryStatus_all==1||queryStatus_mine==1)){//完成记录
                    	$("#total_inType0").html(setAmountAccuracy(total.inType0));
                    	$("#total_inType1").html(setAmountAccuracy(total.inType1));
                    	$("#total_inType2").html(setAmountAccuracy(total.inType2));
                    	$("#total_inType3").html(setAmountAccuracy(total.inType3));
                    }
                }
                $('[data-toggle="tooltip"]').tooltip();
                //已记录的行加载到数据列
                $("[name=checkbox_lockUnlock]").each(function(index,record){
                	if(isCheckedRow.indexOf(";"+$(this).val()+";")!=-1){
                		//选中该行
                		$(this).prop("checked","checked");
                	}
                });
                //记录已勾选的数据行
                $("[name=checkbox_lockUnlock]").click(function(){
                	var row_id=$(this).val();
                	if($(this).prop("checked")){
                		isCheckedRow+=";"+row_id+";";
                	}else{
                		isCheckedRow=isCheckedRow.replace(";"+row_id+";", "");
                	}
                });
                syncLoadCompanyTotal();//异步加载公司用款相关总计数据
            }
        }
    };
    $.ajax(ajaxData);
}
var templateMap = new Map();
/**根据输入金额动态获取手续费 需求 7595**/
function dynamicFee(id,amount) {
    if (!id || !amount) return;
    var url = '/r/account/dynamicFee';
    var ajaxData = {type:'get',url:url,dataType:'json',data:{id:id,amount:amount},async:false,success:function (res) {
            if (res && res.status==1){
                var fee = res.data;
                $('#fee'+id).val(fee);
                saveAmountOrFee($('#fee'+id),2);
            } else {
                showMessageForFail("动态计算手续费失败:"+res.message,5000);
            }
        }};
    $.ajax(ajaxData);
}
/**保存临时的金额 和手续费 需求 7453 type=1 金额 2 手续费 */
function saveAmountOrFee(obj,type) {
	if (!obj || !type){
		return;
	}
	var id=$(obj).attr('id');;
    if (type==1){
        id = id.split("amount")[1];
    } else {
        id = id.split("fee")[1];
    }
    var value = $(obj).val();
    //新增的时候
    if (!templateMap || templateMap.size==0){
        if (type==1){
            value = value+':'+'无';
        } else {
            value =  '无'+':'+value;
        }
        templateMap.set(id,value);
    }else {
        var oldVal = templateMap.get(id);
        if (oldVal) {
            var oldValArr=oldVal.split(":");
            if (type==1){
                value = value?value:oldValArr[0] && '无'!=oldValArr[0] ?oldValArr[0]:'无';
                value = value+":"+oldValArr[1];
            } else {
                value= value?value:oldValArr[1] && '无'!=oldValArr[1] ?oldValArr[1]:'无';
                value = oldValArr[0]+":"+value;
            }
        }else {
            if (type==1){
                value = value+':'+'无';
            } else {
                value =  '无'+':'+value;
            }
        }
        templateMap.set(id,value);
    }
    if (type==1){
        //根据输入金额动态获取手续费 7595
        dynamicFee(id,value.split(":")[0]);
    }
}
/**保存临时的金额 和手续费 需求 7453 */
/**获取页面之前输入的金额*/
function getTemplateOldValue(id) {
	if (!id){
		return null;
	}
	if (!templateMap){
		return null;
	}
	var oldVal = templateMap.get(id+'');
	if (oldVal){
	    return oldVal.split(":");
    }
	return oldVal;
}
/** 存储输入的第三方银行余额值 */
var thirdBankBalance_log = new Map();
var saveThirdBankBalance=function(obj,rowId){
	thirdBankBalance_log.set(rowId+"",$(obj).val());
}
/** 读取输入的第三方银行余额值 */
var getThirdBankBalance=function(rowId){
	if(!rowId) return "";
	return thirdBankBalance_log.get(rowId+"");
}

/**提现完成清除缓存*/
function clearTemplateData(id) {
    if (!id)return;
    if (!templateMap || templateMap.size==0)return;
    if(templateMap.has(id)){
        templateMap.delete(id);
    }
    thirdBankBalance_log.delete(id);
}
/** 下发耗时控制变色 毫秒转换*/
var changeColor4LockTime=function(lockTime_mss){
	if(!lockTime_mss){
		return '0';
	}
	var is_red=(lockTime_mss/(1000*60*30)>=1);//大于30分钟红色
	var is_green=(lockTime_mss/(1000*60*15)>=1);//大于15分钟绿色
	var  lockTime_class='';
	if(is_red){
		lockTime_class=' class="label label-danger" ';
	}else if(is_green){
		lockTime_class=' class="label label-success" ';
	}
	return '<span style="width:100%" '+lockTime_class+'>'+formatDuring(lockTime_mss)+'</span>' ;
}

/** 完成提现 */
var doEnCash_saveThirdTrans=function(accountId,thirdBalance){
	//验证是否绑定了第三方
	var thirdId=getOneThirdBindById(accountId);
	if(!thirdId){
    	showMessageForFail("没有指定下发第三方，请点击【选择第三方】按钮设定");
    	return;
	}
	var amount=$("#amount"+accountId).val();
	var fee=$("#fee"+accountId).val();
	var thirdBankBalance=$("#thirdBankBalance"+accountId).val();
	if(!thirdBankBalance){
    	showMessageForFail("请先输入第三方真实余额");
    	return;
	}
	$.ajax({
		type:"GET",
		dataType:'JSON',
		url:'/r/income/saveThirdTransInDrawTask',
		async:false,
		data:{
			"thirdBalance":thirdBalance,
			"thirdBankBalance":thirdBankBalance,
			"accountId":accountId,
			"amount":amount,
			"fee":fee
		},
		success:function(jsonObject){
	        if(jsonObject&&jsonObject.status == 1){
	        	showMessageForSuccess("提现成功！");
	        	thirdDraw(0);
	        }else{
	        	showMessageForFail("提现失败："+jsonObject.message);
	        }
            clearTemplateData(accountId);
	    }
	});
}
/** 下发失败 */
var do_reject2CurrSys=function(accountId){
	//验证是否绑定了第三方
	var thirdId=getOneThirdBindById(accountId);
	var amount=$("#amount"+accountId).val();
	var fee=$("#fee"+accountId).val();
	$.ajax({
		type:"GET",
		dataType:'JSON',
		url:'/r/income/reject2CurrSys',
		async:false,
		data:{
			"newManner":1,
			"accountId":accountId
		},
		success:function(jsonObject){
	        if(jsonObject&&jsonObject.status == 1){
	        	showMessageForSuccess("操作成功！");
	        	thirdDraw(0);
	        }else{
	        	showMessageForFail("操作失败："+jsonObject.message);
	        }
	    }
	});
}



var queryFlag = 1;//全部 1 我的 2
var queryAccount = 1;//第三方1 银行 2
var changeTab_failedTotal_a=function(tab_a){
	queryFlag=tab_a;
    drawFailure (0);
}
var changeTab_failedTotal_b=function(tab_b){
	queryAccount=tab_b;
    drawFailure (0);
}
//统计下发失败 查询
var drawFailure=function(CurPage) {
	if(queryAccount==2){
		//银行 未开发直接返回无数据
		$('#table_FailedTotal tbody').html("");
        $('#drawFailurePage').html('<div style="margin-bottom:0px;font-size: 20px;" class="alert alert-success center">开发中</div>');
        return;
	}
	var $filter=$("#failedTotalFilter");
    var startAndEnd = $filter.find('[name="startAndEndTime"]').val();
    var startTime = '';
    var endTime = '';
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0].trim().substring(0,10);
            endTime = startAndEnd[1].trim().substring(0,10);
        }
    }
    if(!startTime){
        startTime = _getDefaultTime()[0].trim().substring(0,10);
    }
    if(!endTime){
    	endTime =  _getDefaultTime()[1].trim().substring(0,10);
    }
    if(!!!CurPage&&CurPage!=0) CurPage= $("#drawFailurePage .Current_Page").text();
    var pageSize =$.session.get('initPageSize');
    var data = {
    	'queryFlag':queryFlag,//全部 1 我的 2
        'queryAccount':queryAccount,//第三方1 银行 2
    	'thirdAccount':$filter.find('[name="fromAccount"]').val().trim(),
    	'thirdName':$filter.find('input[name="owner"]').val().trim(),
        'startTime':startTime.trim(),
        'endTime':endTime.trim(),
        'pageNo':CurPage<=0?0:CurPage-1,
        'pageSize':$.session.get('initPageSize')
    };
//    data.testFlag=1;//查看测试数据
    var ajaxData ={
        url:'/r/account/findThirdDrawFail',type:'post',
        contentType:"application/json;charset=UTF-8",dataType:'json',async:false,data:JSON.stringify(data),
        success:function (res) {
            if (res){
                if (res.status==1){
                    var failCountsCurrent =0 ,tr='';
                    var idList = [];
                    if (res.data && res.data.length>0){
                        $.each(res.data, function (index, record) {
                            idList.push({'id': record.fromId, 'type': 'third'});
                            idList.push({'id': record.toId});
                            tr+='<tr><td>'+ record.time +'</td>';
                            tr += "<td>" +
                                "<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + record.fromId + "' data-placement='auto right' data-trigger='hover'  >" + (record.thirdAccount) +
                                "</a>" +"<br>"+record.thirdName+
                                "</td>";
                            tr+='<td><a>'+(record.failCounts||'0')+'</a></td></tr>';
                            failCountsCurrent+=(record.failCounts||0);
                        });
                    }
                    if (tr){
                        var tr1 = "<tr><td colspan='2'>小计:"+res.data.length+"</td>" +
                        		"<td  bgcolor='#579EC8' style='color:white;font-size:10px;'>"+failCountsCurrent+"</td>" +
                        	"</tr>";
                        var tr2 = "<tr><td colspan='2'>总计:"+res.page.totalElements+"</td>" +
                        		"<td  bgcolor='#D6487E' style='color:white;font-size:10px;'>"+(res.page.header.countAllFail?res.page.header.countAllFail:0)+"</td>" +
                        	"</tr>";
                        tr = tr+tr1+tr2;
                    }
                    $('#table_FailedTotal tbody').html(tr);
                }
                showPading(res.page,"drawFailurePage",drawFailure);
                //加载账号悬浮提示
                loadHover_accountInfoHover(idList);
            }
        }
    };
    $.ajax(ajaxData);
}
/** 只可以输入2位数小数的正数 */
var clearNoNumPoint2=function(obj){
	obj.value = obj.value.replace(/[^\d.]/g, "");  //清除“数字”和“.”以外的字符   
    obj.value = obj.value.replace(/\.{2,}/g, "."); //只保留第一个. 清除多余的   
    obj.value = obj.value.replace(".", "$#$").replace(/\./g, "").replace("$#$", ".");
    obj.value = obj.value.replace(/^(\-)*(\d+)\.(\d\d).*$/, '$1$2.$3');//只能输入两个小数   
    if (obj.value.indexOf(".") < 0 && obj.value != "") {//以上已经过滤，此处控制的是如果没有小数点，首位不能为类似于 01、02的金额  
        obj.value = parseFloat(obj.value);
    }
}
/** 全选/全不选 */
var selectAllOrNotAll = function(obj){
	var checked = obj.checked;
	$('[name=checkbox_lockUnlock]').prop('checked',checked);
	$('[name=checkbox_lockUnlock]').each(function(index,record){
		if($(record).prop("checked")){
			isCheckedRow+=";"+$(record).val()+";";
		}else{
			isCheckedRow=isCheckedRow.replace(";"+$(record).val()+";", "");
		}
	});
};
/** 一键锁定/解锁 type 1 锁定 2 解锁  */
var saveRecycleByBatch = function(type){
	var array = [];
	$('[name=checkbox_lockUnlock]:checked').each(function(index,obj){
		array.push(obj.value);
	});
	if(array && array.length ==0){
		showMessageForFail('请至少勾选一行数据.');
	    return;
	}
	lockThirdDraw(array.toString(),type);
};
//accountId:数字字符串 或者单个ID   type 1 锁定 2 解锁 
function lockThirdDraw(accountId,type) {
  if (!accountId || !type){
      return ;
  }
  var ajaxData= {type:'get',dataType:'json',data:{"accountId":accountId,"type":type},async:false,url:'/r/account/lockOrUnlockInDrawTaskPage',
      success:function (res) {
          if (res){
              if ( 1!= res.status) {
                  showMessageForFail("操作失败：" + res.message);
                  return;
              }
              showMessageForSuccess("操作成功!" );
              thirdDraw(0);
          }
      }
  };
  $.ajax(ajaxData);
}


//根据id查询账号信息
var getOneThirdBindById=function(accountId){
	var result='';
	$.ajax({
		type:"POST",
		url:"/r/account/getSelectedThirdIdByLockedId",
		data:{"accountId":accountId},
		dataType:'json',
		async:false,
		success:function(jsonObject){
			if(jsonObject.status == 1){
				result=jsonObject.data;
			}else{
				showMessageForFail("获取指定下发第三方账号信息异常，"+jsonObject.message);
			}
		}
	});
	return result;
}

/** 绑定的第三方 */
var loadHover_accountBindThird= function (accountIdList) {
	$.each(accountIdList,function(index,record){
	    $("[data-toggle='third_bind_" + record.id + "']").popover({
	        html: true,
	        title: function () {
	            return '<center class="blue">已指定第三方</center>';
	        },
	        delay: {show: 0, hide: 100},
	        content: function () {
	    		var thirdId=getOneThirdBindById(record.id);
	        	var result="无指定第三方";
	        	if(thirdId){
	        		var thirdInfo=getAccountInfoById(thirdId);
	        		result=_checkObj(thirdInfo.statusStr)+"&nbsp;&nbsp;"+hideAccountAll(thirdInfo.account)+"&nbsp;&nbsp;"+_checkObj(thirdInfo.bankName)+"&nbsp;&nbsp;银行余额"+_checkObj(thirdInfo.bankBalance)+"&nbsp;&nbsp;系统余额"+_checkObj(thirdInfo.balance);
	        	}
	            return '<div style="margin-bottom:0px;width:500px;" class="alert alert-success center">'+result+'</div>';
	        }
	    });
	});
}

$("#accountFilter").find("[name=choiceBankBrand],[name=search_EQ_handicapId]").change(function(){
	thirdDraw(0);
});
$("#accountFilter").find("[name=locked],[name=search_IN_status],[name=cardType],[name=currSysLevel]").click(function(){
	thirdDraw(0);
});


_datePickerForAll($("[name=History_startAndEndTime]"));
getHandicap_select($("select[name='search_EQ_handicapId']"),null,"全部");//下发记录
getHandicap_select($("[name=third_search_IN_handicapId]"),0,"全部");//第三方资料
getBankTyp_select($("[name=choiceBankBrand]"),null,"全部");
contentRight();
_outerUlLiClick(1);