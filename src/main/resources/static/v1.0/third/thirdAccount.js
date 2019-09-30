currentPageLocation = window.location.href;
/**
 * 根据账号Type拼接对应数据
 */
var showAccountList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountPage .Current_Page").text();
	var $filter = $("#accountFilter");
    var data = {
        search_EQ_handicapId:$filter.find("select[name='search_EQ_handicapId']").val(),
    	search_LIKE_aliasName:$filter.find("[name=search_LIKE_aliasName]").val(),
    	search_LIKE_channelName:$filter.find("[name=search_LIKE_channelName]").val(),
    	search_LIKE_memberId:$filter.find("[name=search_LIKE_memberId]").val(),
    	search_IN_platformStatus:$filter.find("[name=search_IN_platformStatus]:checked").val().toString(),
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize')
    };
    //发送请求
	$.ajax({
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:'/passage/list',
        data:data,
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			var $tbody=$("#accountListTable").find("tbody");
			$tbody.html("");
			var idList=new Array();
			var tr="";
			$.each(jsonObject.data,function(index,record){
				idList.push(record.id);
				tr+="<tr>";
				//盘口
				tr+="<td>"+_checkObj(record.handicapName)+"</td>";
				//通道名称
				tr+="<td>"+_checkObj(record.aliasName)+"</td>";
				//服务商
				tr+="<td>"+_checkObj(record.channelName)+"</td>";
				//商号 悬浮提示
				tr+="<td style='padding-left:0px;padding-right:0px;'>" +
						"<a class='bind_hover_card' data-toggle='thirdDaiFuHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"
				tr+=hideMemberId(record.memberId);
				tr+=	"</a></td>"
				//层级;
				tr+="<td>"+(record.levelNameList&&record.levelNameList.length>0?getHTMLremark(record.levelNameList.join(" | "),100):"暂无")+"</td>";
				//平台状态 1在用 2停用
				var statusInfo={};
				if(record.platformStatus==1){
					statusInfo.platStatus="在用";
					statusInfo.color=" label-success ";
				}else{
					statusInfo.platStatus="停用";
					statusInfo.color=" label-warning ";
				}
				tr+="<td><a class='label label-sm "+statusInfo.color+" '>"+_checkObj(statusInfo.platStatus)+"</a></td>";
				//累计成功转出金额 
				tr+="<td><a title='平台："+(record.platformOutMoney?record.platformOutMoney:"0.00")+"' >"+(record.crkOutMoney?record.crkOutMoney:"0.00")+"</a></td>";
				//累计成功转出笔数
				tr+="<td><a title='平台："+(record.platformOutTimes?record.platformOutTimes:"0.00")+"' >"+(record.crkOutTimes?record.crkOutTimes:"0")+"</a></td>";
				//第三方处理汇总
				tr+="<td><a onclick='showModalDaiFuDetail("+record.id+","+record.handicapId+",\"0\")' class='badge badge-warning'>"+(record.countPaying?record.countPaying:0)+
					"</a>&nbsp;<a onclick='showModalDaiFuDetail("+record.id+","+record.handicapId+",\"1\")' class='badge badge-success'>"+(record.countSuccess?record.countSuccess:0)+
					"</a>&nbsp;<a onclick='showModalDaiFuDetail("+record.id+","+record.handicapId+",\"2\")' class='badge badge-inverse'>"+(record.countError?record.countError:0)+
					"</a></td>";
				//操作
				tr+="<td>";
				//查询余额
				tr+="<button class='btn btn-xs btn-white btn-success btn-bold' \
					onclick='searchNewBalance("+record.id+","+record.handicapId+")'>\
					<i class='ace-icon fa fa-search bigger-100 green'></i><span>查询余额</span></button>";
				//根据服务商同步平台银行类别
				tr+="<button class='btn btn-xs btn-white btn-success btn-bold' \
					onclick='sync_BankTypeByChannelName(\""+record.channelName+"\")'>\
					<i class='ace-icon fa fa-share bigger-100 green'></i><span>同步银行</span></button>";
				//根据服务商修改平台银行类别
				tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentright='ThirdAccount:updateBankType:*'\
					onclick='showUpdateBankTypeInfoModal(\""+record.channelName+"\")'>\
					<i class='ace-icon fa fa-edit bigger-100 orange'></i><span>修改银行</span></button>";
				tr+="</td>";
				tr+="</tr>";
			});
			$tbody.html(tr);
			showPading(jsonObject.page,"accountPage",showAccountList,null,true);
			//加载账号悬浮提示
			loadHover_thirdDaiFuHover(idList);
			contentRight();
        }
	});
}

getHandicap_select($("select[name='search_EQ_handicapId']"),null,"全部");
showAccountList(0);
$("#accountFilter").find("[name=search_EQ_handicapId]").change(function(){
	showAccountList();
});
$("#accountFilter").find("[name=search_IN_platformStatus]").click(function(){
	showAccountList();
});
$("#accountFilter").keypress(function(e){
	if(event.keyCode == 13) {
		$("#accountFilter #searchBtn button").click();
	}
});