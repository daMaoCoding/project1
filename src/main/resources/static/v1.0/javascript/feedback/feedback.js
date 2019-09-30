jQuery(function($) {
	initTimePicker(false,$("[name=startAndEndTime]"),typeCustomLatestToday);
	initTimePicker(false,$("[name=processedStartAndEndTime]"),typeCustomLatestToday);
	queryFeedBack();
	queryProcessed();
})
//备注
var remarkButton=false;
//处理
var disposeButton=false;
//完成
var finishButton=false;
//驳回
var rejectButton=false;
//驳回
var deleteButton=false;
$.each(ContentRight['FeedBack:*'], function (name, value) {
    if (name == 'FeedBack:remark:*') {
    	remarkButton = true;
    }
    if (name == 'FeedBack:dispose:*') {
    	disposeButton = true;
    }
    if (name == 'FeedBack:finish:*') {
    	finishButton = true;
    }
    if (name == 'FeedBack:reject:*') {
    	rejectButton = true;
    }
    if (name == 'FeedBack:delete:*') {
    	deleteButton = true;
    }
});
var downloadUrl = window.location.origin;
var descOrAsc="";
var orderBy="";
var processedDescOrAsc="";
var processedOrderBy="";
var timeDescOrAsc="";
var timeOrderBy="";
var queryFeedBack=function(){
	//当前页码
	var CurPage=$("#feedBackPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取未处理关键字
	var untreatedFind=$.trim($("#untreatedFind").val());
	//层级
	var level = $("input[name='level']:checked").val();
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='startAndEndTime']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	$.ajax({
		type:"post",
		url:"/r/feedback/findFeedBack",
		data:{
			"pageNo":CurPage,
			"untreatedFind":untreatedFind,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"type":"untreatedFind",
			"descOrAsc":descOrAsc,
			"orderBy":orderBy,
			"timeDescOrAsc":timeDescOrAsc,
			"timeOrderBy":timeOrderBy,
			"level":level,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				 var tr = '';
				 var counts = 0;
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
                     tr += '<tr>'
                    	 	  +"<td>";
                    	 	  	if(val.level=='建议')
                    	 	  	    tr +='<span class="label label-sm label-success">'+val.level+'</span>';
                    	 	  	if(val.level=='一般')
                       	 	  	    tr +='<span class="label label-sm label-warning">'+val.level+'</span>'
                       	 	    if(val.level=='严重')
                       	 	  	    tr +='<span class="label label-sm label-danger">'+val.level+'</span>'
                    	 	  tr+="</td>"
                    	 	 +'<td>'
		                   	      + '<a href="#/FeedBackShowDetails:*?id='+val.id+'" class="bind_hover_card breakByWord"  title="反馈信息"'
		                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="right"'
		                            + ' data-content="' + val.issue + '">'
		                             + val.issue.substring(0,100)
		                         + '</a>'
	                         +'</td>'
	                         +'<td>';
		                   	      if(val.status=='未处理')
		                   	    	  tr +='<span class="badge badge-danger">'+val.status+'</span>';
		               	    	  if(val.status=='处理中')
		                   	    	  tr +='<span class="badge badge-purple">'+val.status+'</span>';
		                   	  tr+='</td>'
                    	      +'<td>'+val.creator+'</td>'
                    	      +'<td>'+val.acceptor+'</td>'
                    	      +'<td><a onclick=feedBackPhoto("'+val.imgs+'","0","show") herf="#">查看</a></td>'
                    	      +'<td>'+val.createTime+'</td>'
                    	      +'<td>';
                    	      if(remarkButton)
                    	    	  tr +='<button type="button" onclick="addRemark('+val.id+',\'remark\')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                              if(disposeButton)
                            	  tr +='<button type="button" onclick="addRemark('+val.id+',\'cl\')" class="btn btn-xs btn-white btn-warning btn-bold "><i class="ace-icon fa fa-pencil bigger-100 yellow"></i>处理</button>';
                              if(deleteButton)
                            	  tr +="<button type='button' onclick='addRemark("+val.id+",\"delete\",\""+val.imgs+"\")' class='btn btn-xs btn-white btn-Inverse btn-bold '><i class='ace-icon fa fa-trash-o bigger-120'></i>删除</button>";
                              if(finishButton)
                            	  tr +="<button type='button' onclick=addRemark("+val.id+",'"+val.status+"') class='btn btn-xs btn-white btn-success btn-bold '><i class='ace-icon fa fa-check bigger-100 blue'></i>完成</button>";
                              tr +='</td>'
                           +'</tr>';
                     counts +=1;
                 };
				 $('#total_tbody').empty().html(tr);
				 var trs = '<tr>'
					            +'<td colspan="8">小计：'+counts+'</td>'
				         +'</tr>';
				 var trn = '<tr>'
	     	            +'<td colspan="8">总计：'+jsonObject.data.page.totalElements+'</td>'
			          +'</tr>';
			 $('#total_tbody').append(trs);
             $('#total_tbody').append(trn);
			}else{
				$('#total_tbody').empty().html('<tr></tr>');
			}
			$("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.page,"feedBackPage",queryFeedBack);
		}
	});
}


var queryProcessed=function(){
	//当前页码
	var CurPage=$("#processedPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	//获取未处理关键字
	var processedFind=$.trim($("#processedFind").val());
	//层级
	var level = $("input[name='processedLevel']:checked").val();
	//获取时间段
	//日期 条件封装
	var startAndEndTime = $("input[name='processedStartAndEndTime']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	$.ajax({
		type:"post",
		url:"/r/feedback/findFeedBack",
		data:{
			"pageNo":CurPage,
			"untreatedFind":processedFind,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"type":"processedFind",
			"processedDescOrAsc":processedDescOrAsc,
			"processedOrderBy":processedOrderBy,
			"level":level,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				 var tr = '';
				 var counts = 0;
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
                     tr += '<tr>'
                    	 +"<td>";
		         	 	  	if(val.level=='建议')
		         	 	  	    tr +='<span class="label label-sm label-success">'+val.level+'</span>';
		         	 	  	if(val.level=='一般')
		            	 	  	    tr +='<span class="label label-sm label-warning">'+val.level+'</span>'
		            	 	    if(val.level=='严重')
		            	 	  	    tr +='<span class="label label-sm label-danger">'+val.level+'</span>'
		         	 	  tr+="</td>"
                    	 	 +'<td>'
		                   	      + '<a href="#/FeedBackShowDetails:*?id='+val.id+'" class="bind_hover_card breakByWord"  title="反馈信息"'
		                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="right"'
		                            + ' data-content="' + val.issue + '">'
		                             + val.issue.substring(0,100)
		                          + '</a>'
	                         +'</td>'
                    	      +'<td>';
	                    	      if(val.status=='已处理')
	                    	    	  tr +='<span class="badge badge-success">'+val.status+'</span>';
		         	 	     tr +='</td>'
                    	      +'<td>'+val.creator+'</td>'
                    	      +'<td>'+val.acceptor+'</td>'
                    	      +'<td><a onclick=feedBackPhoto("'+val.imgs+'","0","show") herf="#">查看</a></td>'
                    	      +'<td>'+val.createTime+'</td>'
                    	      +'<td>'+val.updateTime+'</td>'
                    	      +'<td>';
                    	      if(remarkButton)
                    	    	  tr +='<button type="button" onclick="addRemark('+val.id+',\'remark\')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                    	      if(rejectButton)
                    	    	  tr +='<button type="button" onclick="addRemark('+val.id+',\'return\')" class=" btn btn-xs btn-white btn-danger btn-bold"><i class="ace-icon fa fa-share  bigger-100 red"></i>驳回</button>';
                    	    tr +='</td>';
                           +'</tr>';
                     counts +=1;
                 };
				 $('#Processed_tbody').empty().html(tr);
				 var trs = '<tr>'
					            +'<td colspan="9">小计：'+counts+'</td>'
				         +'</tr>';
				 var trn = '<tr>'
     	            +'<td colspan="9">总计：'+jsonObject.data.page.totalElements+'</td>'
		          +'</tr>';
				 $('#Processed_tbody').append(trs);
             $('#Processed_tbody').append(trn);
			}else{
				$('#Processed_tbody').empty().html('<tr></tr>');
			}
			$("[data-toggle='popover']").popover();
			//分页初始化
			showPading(jsonObject.data.page,"processedPage",queryProcessed);
		}
	});
}

function addRemark(id,type,imgs){
	if(type=="remark" || type=="cl" || type=="return"){
		$('#Remark_modal').modal('show');
		$('#totalTaskFinishBTN').attr('onclick', 'save_Remark('+id+',"'+type+'");');
	}else{
		if(type=="未处理"){
			$.gritter.add({
                time: 400,
                class_name: '',
                title: '系统消息',
                text: '请先处理',
                sticky: false,
                image: '../images/message.png'
            });
			return;
		}
		 bootbox.confirm(type=="delete"?"确定要删除吗？":"确定要完成吗？", function (res) {
	        if (res) {
	        	save_Remark(id,type,imgs);
	        }
		 });
	}
}

function save_Remark(id,type,imgs){
	var remark=$.trim($("#Remark").val());
	if(type=="remark" || type=="cl" || type=="return"){
		if(remark==""){
			  $('#prompt_remark').show(10).delay(1500).hide(10);
			  return;
		  }
	}
	$.ajax({
		async:true,
		type:'post',
        url:'/r/feedback/saveRemark',
        data:{'id':id,'remark':remark,'type':type},
        dataType:'json',
        success:function (res) {
        	if(res.status == 1){
        		$('#Remark_modal').modal('hide');
            	queryFeedBack();
            	queryProcessed();
            	$("#Remark").val("");
            	if(type=="delete"){
            		$.ajax({
            			async:true, 
            			type:'post',
            	        url:'/r/feedback/deleteImgs',
            	        data:{'imgs':imgs},
            	        dataType:'json',
            	        success:function (res) {
            	        	
            	        }
            	    });
            	}
        	}
        }
    });
}


function feedBackPhoto(url,index,type) {
	if(index+1>=url.split(",").length)
    	index=0
	var counts=url.split(",").length;
	$('#counts').empty().html("总共"+(parseInt(counts)-1)+"张,第"+(parseInt(index)+1)+"张");
	if("show"==type){
		$('#feedBackImg').attr('src', downloadUrl+url.split(",")[0]);
	    $('#feedBackImg').attr('href', downloadUrl+url.split(",")[0]);
	    $('#feedBackImg').attr('name', url.split(",")[0]);
	    $('#feedImg').click(function(){
	    	feedBackPhoto(url,1,'next');
	    });
	    $('#feedBackImgModal').modal('show');
	}else{
	    $('#feedBackImg').attr('src', downloadUrl+url.split(",")[index]);
	    $('#feedBackImg').attr('href', downloadUrl+url.split(",")[index]);
	    $('#feedBackImg').attr('name', url.split(",")[0]);
	    $('#feedImg').one("click",function(){
	    	feedBackPhoto(url,index+1,'next');
	    });
	}
}

function orderByType(upOrDown,type){
	if(type=='level'){
		if(upOrDown!='down'){
			$("#tradingDown").show();
			$("#tradingUp").hide();
			descOrAsc="desc";
			orderBy="level";
		}else{
			$("#tradingDown").hide();
			$("#tradingUp").show();
			descOrAsc="asc";
			orderBy="level";
		}
		timeDescOrAsc=null;
		timeOrderBy=null;
		processedDescOrAsc=null;
		processedOrderBy=null;
		queryFeedBack();
	}else if(type=="time"){
		if(upOrDown!='down'){
			$("#timeDown").show();
			$("#timeUp").hide();
			timeDescOrAsc="desc";
			timeOrderBy="create_time";
		}else{
			$("#timeDown").hide();
			$("#timeUp").show();
			timeDescOrAsc="asc";
			timeOrderBy="create_time";
		}
		descOrAsc=null;
		orderBy=null;
		processedDescOrAsc=null;
		processedOrderBy=null;
		queryFeedBack();
	}else{
		if(upOrDown!='down'){
			$("#processedDown").show();
			$("#processedUp").hide();
			processedDescOrAsc="desc";
			processedOrderBy="level";
		}else{
			$("#processedDown").hide();
			$("#processedUp").show();
			processedDescOrAsc="asc";
			processedOrderBy="level";
		}
		descOrAsc=null;
		orderBy=null;
		timeDescOrAsc=null;
		timeOrderBy=null;
		queryProcessed();
	}
}