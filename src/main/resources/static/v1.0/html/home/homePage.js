//公告页

/** 加载公告 */
var initNotice=function(){
	//查询数据库中各类型的历史数据
	var fundsList=getNoticeByType(1);
	var flwList=getNoticeByType(2);
	var pcList=getNoticeByType(3);
	if(fundsList){
		//自动出入款系统
		if(fundsList.length>0){
			$("#currDiv").append($(getHTMLbyNotice(fundsList[0],true)));
		}
		if(fundsList.length>1){
			$("#historyDiv").append($(getHTMLbyNotice(fundsList[1])));
		}
	}
	if(flwList){
		//返利网工具
		if(flwList.length>0){
			$("#currDiv").append($(getHTMLbyNotice(flwList[0],true)));
		}
		if(flwList.length>1){
			$("#historyDiv").append($(getHTMLbyNotice(flwList[1])));
		}
	}
	if(pcList){
		//PC工具
		if(pcList.length>0){
			$("#currDiv").append($(getHTMLbyNotice(pcList[0],true)));
		}
		if(pcList.length>1){
			$("#historyDiv").append($(getHTMLbyNotice(pcList[1])));
		}
	}
	
}

var getHTMLbyNotice=function(record,isNew){
	var result="";
	if(record&&record.type&&record.title&&record.publishTime&&record.contant){
		var typeStr=(record.type==1?"自动出入款系统":(record.type==2?"返利网工具":"PC工具"));
		var newHTML=isNew?'<span class="label label-danger arrowed"><i class="ace-icon fa fa-bell icon-animated-bell" style="color:yellow;"></i><span style="color:yellow;">&nbsp;最新&nbsp;</span></span>':'';
		result='<div class="widget-main" id="div4copy">\
		<h3 class="center blue"></h3>\
		<h5 class="header smaller center lighter red no-padding no-margin-top">\
			<label class="pull-left">'+typeStr+'（'+record.publishNo+'）'+newHTML+'</label>\
			<label class="center bolder">'+record.title+'</label>\
			<label class="pull-right">'+timeStamp2yyyyMMddHHmmss(record.publishTime)+'</label>\
		 </h5><div class="alert alert-success" style="font-size:12px">'+record.contant+'</div>\
	</div>';
	}
	return result;
}

/** 查询不同类型下的公告 */
var getNoticeByType=function(type){
	var result=null;
	var data={
        search_EQ_type:type,
        search_EQ_status:1,
        pageNo:0,
        pageSize:2
	}
	$.ajax({
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:'/r/notice/list',
        data:data,
        success:function(jsonObject){
			if(jsonObject.status ==1 && jsonObject.data && jsonObject.data.length>0){
				result= jsonObject.data;
			}
        }
	});
	return result;
}

if(!isHideOutAccountAndModifyNouns){
	//非海外版 显示公告
	$("#noticeDiv").show();
	initNotice();
}