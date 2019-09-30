/** 公司用款 下发模块 */
var company_isCheckedRow="";//已勾选的数据ID字符串
var companyTab="list";//公司用款list 下发任务all 我已锁定mine
var all_companyTab_Sub="1";//下发任务子TAB 新下发任务1 正在下发2 第三方资料3 下发完成记录4 下发失败记录5
var mine_companyTab_Sub="1";//我已锁定子TAB 正在下发1 等待到账2 第三方资料3 下发完成记录4 下发失败记录5
$("#company_task_all").load("../html/income/asign/thirdDrawCompanyMoney_task.html");


var changeTab_all_mine_SUB=function(tab_val){
	if(companyTab=="all"){
		all_companyTab_Sub=tab_val;
	}else if(companyTab=="mine"){
		mine_companyTab_Sub=tab_val;
	}
	showHide4TabChange();
}
var showHide4TabChange=function(){
	company_isCheckedRow="";//已勾选的行清空
	var url_task="../html/income/asign/thirdDrawCompanyMoney_task.html";
	var url_history="../html/income/asign/thirdDrawCompanyMoney_history.html";
	var url_third="../html/income/asign/thirdDrawCompanyMoney_third.html";
	var temp_companyTab_Sub;
	if(companyTab=="all"||companyTab=="mine"){
		if(companyTab=="all"){//全部
			temp_companyTab_Sub=all_companyTab_Sub;
		}else if(companyTab=="mine"){//我的锁定
			temp_companyTab_Sub=mine_companyTab_Sub;
		}
		//加载不同HTML
		if(temp_companyTab_Sub=="1"||temp_companyTab_Sub=="2"){//下发任务
			$("#companyMoneyTable_div").load(url_task);
			$.get(url_task,function(){
				if(temp_companyTab_Sub=="1"){//全部-新下发任务 我已锁定-正在下发
					var $div=$("#company_task_div");
					$div.find(".all,.mine").hide();
					if(companyTab=="all"){
						$div.find(".all").show();
						$div.find(".mine").hide();
					}else if(companyTab=="mine"){//我的锁定
						$div.find(".all").hide();
						$div.find(".mine").show();
					}
				}
				company_showTaskDataList(0);
			});
		}else if(temp_companyTab_Sub=="3"){//第三方资料
			$("#companyMoneyTable_div").load(url_third);
			$.get(url_third,function(){
				company_showThirdDataList(0);
			});
		}else if(temp_companyTab_Sub=="4"){//下发完成记录
			$("#companyMoneyTable_div").load(url_history);
			$.get(url_third,function(){
				company_history_status="0";
				company_showHistoryDataList(0);
			});
		}else if(temp_companyTab_Sub=="5"){//下发失败记录
			$("#companyMoneyTable_div").load(url_history);
			$.get(url_third,function(){
				company_history_status="1,2";
				company_showHistoryDataList(0);
			});
		}
	}else{//公司审核
		$div.find("#companyMoneyTable_div").html("");
	}
	
}