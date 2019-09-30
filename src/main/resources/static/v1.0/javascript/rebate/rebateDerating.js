jQuery(function($) {
	getHandicap_select($("#deratingHandicap"),null,"全部");
	queryDerating();
});



var queryDerating=function(){
//当前页码
var CurPage=$("#deratingTabPage").find(".Current_Page").text();
if(!!!CurPage){
	CurPage=0;
}else{
	CurPage=CurPage-1;
}if(CurPage<0){
	CurPage=0;
}
//订单号
var orderNo=$("#deratingOrderNo").val();
//金额
var fromAmount=$("#deratingFromMoney").val();
var toMoney=$("#deratingToMoney").val();
if(fromAmount*1>toMoney*1 && toMoney*1>0){
	showMessageForFail("金额有误！");
    return
}
//获取盘口
var handicap=$("#deratingHandicap").val();
if(handicap=="" || handicap==null){
	handicap=0;
}
var uName=$("#uName").val();
var status=$("#status").val();
$.ajax({
	type:"post",
	url:"/r/rebate/findDerating",
	data:{
		"pageNo":CurPage,
		"type":"rebated",
		"orderNo":orderNo,
		"uname":uName,
		"fromAmount":fromAmount,
		"toMoney":toMoney,
		"status":status,
		"startAndEndTime":"",
		"handicap":handicap,
		"pageSize":$.session.get('initPageSize')},
	dataType:'json',
	success:function(jsonObject){
		if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
			var tr = '';
			 //小计
			 var counts = 0;
			 var amounts=0;
			 var idList=new Array();
			 var statusStr="";
			 for(var index in jsonObject.data.rebatelist){
				 var val = jsonObject.data.rebatelist[index];
				 if(val.status==888){
					 statusStr="待审核";
				 }else if(val.status==999){
					 statusStr="不通过";
				 }else{
					 statusStr="通过";
				 }
                tr += '<tr>'
                			+'<td>' + val.uName + '</td>'
                			+'<td>' + val.handicapName + '</td>'
                        	+'<td>' + val.toHolder + '</td>'
                        	+'<td>' + val.toAccountType +"</br>"+val.toAccountInfo+ '</td>'
                        	//+'<td>'+ val.toAccountInfo +'</td>'
                        	+'<td>' + val.toAccount + '</td>'
                        	+'<td>' + val.tid + '</td>'
                        	+'<td>' + statusStr + '</td>'
                        	+'<td>' + val.amount + '</td>'
                        	+'<td>' + val.createTimeStr + '</td>'
                        	+'<td>'
	                   	      + '<a class="bind_hover_card breakByWord"  title="备注"'
	                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
	                            + ' data-content="' + val.remark + '">'
	                             + val.remark.substring(0,10)
	                         + '</a>'
                          +'</td>'
                        	+'<td>';
                        		if(val.status==888){
                        			tr+='<button onclick="audit('+val.id+')" type="button" class=" btn btn-xs btn-white btn-warning  btn-bold"><i class="ace-icon fa fa-reply  bigger-100 red"></i>审核</button>';
                        		}
                        		tr+='<button type="button" onclick="addRemark('+val.id+')" class="btn btn-xs btn-white btn-info btn-bold "><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>'
                        	+'</td>'
                	 +'</tr>';
                counts +=1;
                amounts+=val.amount;
            };
			 $('#deratingTab_tbody').empty().html(tr);
			 var trs = '<tr>'
							 +'<td colspan="7">小计：'+counts+'</td>'
							 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts.toFixed(2)+'</td>'
						     +'<td colspan="3"></td>'
					  +'</tr>';
            $('#deratingTab_tbody').append(trs);
            var trn = '<tr>'
		                	+'<td colspan="7">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
		                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+jsonObject.data.rebateTotal+'</td>'
						    +'<td colspan="3"></td>'
			         +'</tr>';
            $('#deratingTab_tbody').append(trn);
			}else {
                $('#deratingTab_tbody').empty();
            }
		$("[data-toggle='popover']").popover();
		//分页初始化
		showPading(jsonObject.data.rebatePage,"deratingTabPage",queryDerating);
	}
});
}

function addRemark(id,type){
	if(type==3){
		$('#typeName').empty().html("取消");
	}else if(type==6){
		$('#typeName').empty().html("转待排查");
	}else if(type==1){
		$('#typeName').empty().html("完成");
	}else if(type==0){
		$('#typeName').empty().html("重新生成任务");
	}else if(type==8){
		$('#typeName').empty().html("分配");
	}else{
		$('#typeName').empty().html("备注");
	}
	$("#Remark").val("");
	$('#Remark_modal').modal('show');
	$('#totalTaskFinishBTN').attr('onclick', 'save_Remark('+id+','+type+');');
}

function save_Remark(id,status){
	var remark=$.trim($("#Remark").val());
	if(remark==""){
		  $('#prompt_remark').show(10).delay(1500).hide(10);
		  return;
	  }
	$.ajax({
		async:true,
		type:'post',
        url:'/r/rebate/saveRemarkAndUpdataStatus',
        data:{'id':id,'remark':remark,'status':status},
        dataType:'json',
        success:function (res) {
        	if(res.status == 1){
        		$('#Remark_modal').modal('hide');
        		queryDerating();
            	$("#Remark").val("");
        	}
        }
    });
}

function audit(id){
	$('#derating_modal').modal('show');
	$("#auditRemark").val("");
	$('#auditFinishBTN').attr('onclick', 'save_audit('+id+')');
}

function save_audit(id){
	$('#auditFinishBTN').attr("disabled",true); 
	$('#auditFinishBTN').attr('onclick', "");
	var remark=$.trim($("#auditRemark").val());
	if(remark==""||remark==null||remark.length<5){
		showMessageForFail("请填写备注且长度大于5！");
		$('#auditFinishBTN').attr("disabled",false); 
		$('#auditFinishBTN').attr('onclick', "save_audit(\""+caclTime+"\","+type+");");
		return
	}
	var status=$("#auditResults").val();
	$.ajax({
		async:false,
		type:'post',
        url:'/r/rebate/saveDeratingAudit',
        data:{'id':id,'status':status,'remark':remark},
        dataType:'json',
        success:function (res) {
        	if(res.status == 1){
        		$('#derating_modal').modal('hide');
        		$('#auditFinishBTN').attr("disabled",false); 
        		showMessageForSuccess(res.message,2000);
        		queryDerating();
        	}else{
        		$('#auditFinishBTN').attr("disabled",false); 
        		showMessageForFail(res.message);
        	}
        }
    });
}