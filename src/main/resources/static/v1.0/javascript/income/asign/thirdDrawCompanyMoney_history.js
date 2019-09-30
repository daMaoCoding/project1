/** 公司用款 下发记录JS */

var company_history_status="0";//状态0：已确认，1：被驳回，2：确认出款失败
var company_showHistoryDataList=function(CurPage){
	var $div=$("#company_history_div"),$filter=$div.find(".filter");
    if(!!!CurPage&&CurPage!=0){
        CurPage= $div.find("#company_history_accountPage .Current_Page").text();
    }
    var startAndEnd = getTimeArray($div.find('[name="history_startAndEndTime"]').val());
    var data = {
    		'status':company_history_status,//状态0：已确认，1：已取消，2：确认失败
            'handicap':$filter.find("[name='history_search_IN_handicapId']").val(),//盘口 为空后端会查有权限的盘口
            'createTimeStart':startAndEnd[0],
            'createTimeEnd':startAndEnd[1],
            'member':$filter.find('[name=history_member]').val(), //用款人           
            'receiptType':$filter.find('[name=history_search_receiptType]:checked').val(),//收款方式   银行卡0   第三方1
            'code':$filter.find('[name=history_code]').val(), //用款单编码
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
        url:"/r/outNew/findUsemoneyTakeList",
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
        		//编码 盘口
                var showThirdName=(record.thirdName?"<br/><span class='red' >"+record.thirdName+"</span>":"");//已设定的第三方
        		trs+="<td>"+_checkObj(record.code)+showThirdName+"</td>";
        		trs+="<td>"+_checkObj(record.handicapName)+"</td>";
        		//类型
        		trs+="<td>第三方</td>";
        		//账号信息
        		trs+="<td><span>"+_checkObj(record.toAccountBank)+"&nbsp;|&nbsp;"+_checkObj(record.toAccountOwner)+"<br></span>"+hideAccountAll(record.toAccount)+"</td>";
        		//金额 手续费
        		trs+="<td>"+_checkObj(record.amount)+"</td>";
        		trs+="<td>"+_checkObj(record.fee)+"</td>";
        		//第三方系统余额 真实余额
        		trs+="<td>"+_checkObj(record.balance)+"</td>";
        		trs+="<td>"+_checkObj(record.bankBalance)+"</td>";
                //下发耗时  总耗时 悬浮查看开始时间，结束时间
            	var timeTitle1="开始："+timeStamp2yyyyMMddHHmmss(record.createTime)+"&#13;结束："+timeStamp2yyyyMMddHHmmss(record.updateTime);
            	var timeTitle2="开始："+timeStamp2yyyyMMddHHmmss(record.createTimeTotal)+"&#13;结束："+timeStamp2yyyyMMddHHmmss(record.updateTime);
            	trs += "<td><a title='"+timeTitle1+"'>";
            	trs += changeColor4LockTime(record.timeConsuming*1000);
            	trs += "</a><br/><a title='"+timeTitle2+"'>";
            	trs += formatDuring(record.consumingTime*1000);//总耗时不带颜色
            	trs += "</a></td>";
            	//下发人
            	trs +="<td>"+_checkObj(record.lockerName)+"</td>";
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
								fnName:company_showHistoryDataList
		                }
					showModalInfo_remark(companyOperateLog_param);
				});
        	});
        	//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
				var totalRows={column:15,subCount:jsonObject.data.length,count:jsonObject.page.totalElements};
				totalRows[5]={subTotal:totalAmount,total:jsonObject.page.header.sumAmount};
				totalRows[6]={subTotal:totalFee,total:jsonObject.page.header.sumFee};
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			showPading(jsonObject.page,"company_history_div #company_history_accountPage",company_showHistoryDataList);
            $("[data-toggle='popover']").popover();
            syncLoadCompanyTotal();//异步加载公司用款相关总计数据
			contentRight();
        }
    });
}
getHandicap_select($("#company_history_div [name='history_search_IN_handicapId']"),null,"全部");
loadTimeLimit30DefaultN($("#company_history_div [name=history_startAndEndTime]"));

$("#company_history_div").find("[name=history_search_IN_handicapId]").change(function(){
	company_showHistoryDataList(0);
});
$("#company_history_div").find("[name=history_search_receiptType]").click(function(){
	company_showHistoryDataList(0);
});