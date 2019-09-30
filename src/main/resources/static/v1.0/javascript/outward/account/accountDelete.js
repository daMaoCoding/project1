jQuery(function($) {
	getHandicap_select($("#goldenerHandicap"),null,"全部");
	getHandicap_select($("#finHandicap"),null,"全部");
	queryGoldenerDrawing();
	queryfin();
})



function GoldenerSelect(index){
	if($("#ckb"+index).prop("checked")){
		$("#ckb"+index).prop("checked",false);
	}else{
		$("#ckb"+index).prop("checked",true);
	}
}

function finSelect(index){
	if($("#fin"+index).prop("checked")){
		$("#fin"+index).prop("checked",false);
	}else{
		$("#fin"+index).prop("checked",true);
	}
}


//金流处理页签
var cashFlowProcessing=false;
//财务处理页签
var financial=false;
//金流处理删除按钮
var cashFlowProcessingBT=false;
//财务处理删除按钮
var financialBT=false;
$.each(ContentRight['AccountDelete:*'], function (name, value) {
    if (name == 'AccountDelete:financialGoldener:*') {
    	cashFlowProcessing=true;
    }
    if (name == 'AccountDelete:financialFin:*') {
    	financial=true;
    }
    if (name == 'AccountDelete:goldenerDeleteBt:*') {
    	$("#goldenerDeleteBT").show();
    	cashFlowProcessingBT=true;
    }
    if (name == 'AccountDelete:FinDeleteBt:*') {
    	$("#finDeleteBT").show();
    	financialBT=true;
    }
    if(financial&&cashFlowProcessing){
    	$("#goldenerLi").show();
    	$("#goldenerTab").show();
    	$("#finLi").show();
    }else if(cashFlowProcessing){
    	$("#goldenerLi").show();
    	$("#goldenerTab").show();
    }else if(financial){
    	$("#finLi").show();
    	$("#finTab").show();
    }
});

function deleteAccounts(type){
	var accountIds = new Array();
	var counts=0;
	if(type=="fin"){
		$("input:checkbox[name=finDelete_id]:checked").each(function(){
			accountIds.push($(this).val());
		});
	}else{
		$("input:checkbox[name=delete_id]:checked").each(function(){
			accountIds.push($(this).val());
		});
	}
	if(accountIds.length<=0){
		showMessageForFail("请选择要批量删除的账号");
	}else if(accountIds.length>50){
		showMessageForFail("单次最多允许删除50条记录");
	}else{
		counts=accountIds.length;
		accountIds = accountIds.toString(); 
		bootbox.confirm("确定要删除所选"+counts+"个账号吗？", function (result) {
			 if (result) {
				 if(type=="fin"){
					 $.ajax({
      	        		type:"post",
      	        		url:"/r/account/deleteAccount",
      	        		data:{
      	        			"accountIds":accountIds,
      	        			"type":type
      	        			},
      	        		dataType:'json',
      	        		success:function(jsonObject){
      	        			if(jsonObject.status == 1){
      	        				queryGoldenerDrawing();
      	        				queryfin();
      	        				$.gritter.add({
      	                            time: 1500,
      	                            class_name: '',
      	                            title: '系统消息',
      	                            text: '删除成功',
      	                            sticky: false,
      	                            image: '../images/message.png'
      	                        });
      	        			}else{
      	        				showMessageForFail(jsonObject.message);
      	        			}
      	        		}
      	        	})
				 }else{
					 $.ajax({
			        		type:"post",
			        		url:"/r/finLessStat/findCountsById",
			        		data:{
			        			"accountIds":accountIds
			        			},
			        		dataType:'json',
			        		success:function(jsonObject){
			        			if(jsonObject.status == 1){
			        				$.ajax({
			        	        		type:"post",
			        	        		url:"/r/account/deleteAccount",
			        	        		data:{
			        	        			"accountIds":accountIds,
			        	        			"type":type
			        	        			},
			        	        		dataType:'json',
			        	        		success:function(jsonObject){
			        	        			if(jsonObject.status == 1){
			        	        				queryGoldenerDrawing();
			        	        				queryfin();
			        	        				$.gritter.add({
			        	                            time: 1500,
			        	                            class_name: '',
			        	                            title: '系统消息',
			        	                            text: '删除成功',
			        	                            sticky: false,
			        	                            image: '../images/message.png'
			        	                        });
			        	        			}else{
			        	        				showMessageForFail(jsonObject.message);
			        	        			}
			        	        		}
			        	        	})
			        			
			        			}else if(jsonObject.status == 3){
			        				showMessageForFail(jsonObject.message); 
			        			}
			        		}
					 }) 
				 } 
			 }
		 })
	}
}

function deleteAccount(accountId,type){
	 bootbox.confirm("确定要删除该账号吗？", function (result) {
        if (result) {
        	if(type=="fin"){
        		$.ajax({
	        		type:"post",
	        		url:"/r/account/deleteAccount",async:false,
	        		data:{
	        			"accountId":accountId,
	        			"type":type
	        			},
	        		dataType:'json',
	        		success:function(jsonObject){
	        			if(jsonObject.status == 1){
	        				queryGoldenerDrawing();
	        				queryfin();
	        				$.gritter.add({
	                            time: 1500,
	                            class_name: '',
	                            title: '系统消息',
	                            text: jsonObject.message,
	                            sticky: false,
	                            image: '../images/message.png'
	                        });
	        			}else{
	        				showMessageForFail(jsonObject.message);
	        			}
	        		}
	        	})
        	}else{
        		//检查在亏损里面是否已经处理、如果没有处理则给提示
            	$.ajax({
            		type:"post",
            		url:"/r/finLessStat/findCountsById",
            		data:{
            			"accountId":accountId
            			},
            		dataType:'json',
            		success:function(jsonObject){
            			if(jsonObject.status == 1){
            				$.ajax({
            	        		type:"post",
            	        		url:"/r/account/deleteAccount",
            	        		data:{
            	        			"accountId":accountId,
            	        			"type":type
            	        			},
            	        		dataType:'json',
            	        		success:function(jsonObject){
            	        			if(jsonObject.status == 1){
            	        				queryGoldenerDrawing();
            	        				queryfin();
            	        				$.gritter.add({
            	                            time: 1500,
            	                            class_name: '',
            	                            title: '系统消息',
            	                            text: '删除成功',
            	                            sticky: false,
            	                            image: '../images/message.png'
            	                        });
            	        			}else{
            	        				showMessageForFail(jsonObject.message);
            	        			}
            	        		}
            	        	})
            			}else if(jsonObject.status == 3){
            				showMessageForFail(jsonObject.message);
            			}
            		}
            	})
        	}
        }
	 })
}


//待金流主管删除
var queryGoldenerDrawing=function(){
	//当前页码
	var CurPage=$("#goldenerPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	var alias=$("#alias").val();
	//获取盘口
	var handicap=$("#goldenerHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	var flag=$('input:radio[name="flag"]:checked').val();
	var status=$('input:radio[name="status"]:checked').val();
	
	$.ajax({
		type:"post",
		url:"/r/account/findDeleteAccount",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"alias":alias,
			"type":"goldener",
			"flag":flag,
			"status":status,
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.list.length > 0){
				var tr = '';
				 //小计
				 var counts = 0;
				 var amounts=0;
				 var idList=new Array();
				 for(var index in jsonObject.data.list){
					 var val = jsonObject.data.list[index];
					 idList.push({'id':val.id});
                    tr += '<tr onclick="GoldenerSelect('+index+')" style="cursor:pointer">'
                    			+'<td><input id="ckb'+index+'" onclick="event.stopPropagation();" type="checkbox" name="delete_id" value="'+val.id+'"></td>'
                    			+'<td>' + val.handicapName + '</td>'
                    			+"<td>"  
	                      	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
	                      	      +"</a>"
	                      	    +"</td>"
	                      	    +'<td>' + (val.flag==0?"PC":"手机")+ '</td>'
	                        	+'<td>' + val.alias+ '</td>'
	                        	+'<td>' + val.typeStr + '</td>'
	                        	+'<td>' + val.statusStr + '</td>'
	                        	+'<td>' + (val.bankBalance==null?0:val.bankBalance) + '</td>'
	                        	+'<td>' + val.updateTimeStr + '</td>'
	                        	+'<td>';
                    				if(cashFlowProcessingBT)
                    					tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="deleteAccount('+val.id+',\'goldene\')"><i class="ace-icon fa fa-trash-o bigger-100 dark"></i><span>删除</span></button>';
                    				tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange  '+OperatorLogBtn+'  " onclick="showModal_accountExtra('+val.id+')"><i class="ace-icon fa fa-book"></i><span>操作记录</span></button>';
	                        		if(val.type!=2 && val.type!=3 && val.type!=4 &&val.type!=15){
	                        			tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal('+val.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>';
	                        		}
	                        		tr +='</td>'
                    	 +'</tr>';
                    counts +=1;
                    amounts+=val.bankBalance;
                };
				 $('#goldener_tbody').empty().html(tr);
				 var trs = '<tr>'
								 +'<td colspan="7">小计：'+counts+'</td>'
								 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts.toFixed(2)+'</td>'
							     +'<td colspan="2"></td>'
						  +'</tr>';
                $('#goldener_tbody').append(trs);
                var trn = '<tr>'
			                	+'<td colspan="7">总计：'+jsonObject.data.Page.totalElements+'</td>'
			                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+jsonObject.data.Total+'</td>'
							    +'<td colspan="2"></td>'
				         +'</tr>';
                $('#goldener_tbody').append(trn);
				}else {
	                $('#goldener_tbody').empty();
	            }
			//加载账号悬浮提示
			$("[data-toggle='popover']").popover();
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.Page,"goldenerPage",queryGoldenerDrawing);
		}
	});	
}

//待金财务主管处理
var queryfin=function(){
	//当前页码
	var CurPage=$("#finPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	var alias=$("#finAlias").val();
	//获取盘口
	var handicap=$("#finHandicap").val();
	if(handicap=="" || handicap==null){
		handicap=0;
	}
	var flag=$('input:radio[name="finFlag"]:checked').val();
	$.ajax({
		type:"post",
		url:"/r/account/findDeleteAccount",
		data:{
			"pageNo":CurPage,
			"handicap":handicap,
			"alias":alias,
			"type":"fin",
			"flag":flag,
			"status":"",
			"pageSize":$.session.get('initPageSize')},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.list.length > 0){
				var tr = '';
				 //小计
				 var counts = 0;
				 var amounts=0;
				 var idList=new Array();
				 for(var index in jsonObject.data.list){
					 var val = jsonObject.data.list[index];
					 idList.push({'id':val.id});
                    tr += '<tr onclick="finSelect('+index+')" style="cursor:pointer">'
                    			+'<td><input id="fin'+index+'" onclick="event.stopPropagation();" type="checkbox" name="finDelete_id" value="'+val.id+'"></td>'
                    			+'<td>' + val.handicapName + '</td>'
                    			+"<td>"  
	                      	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
	                      	      +"</a>"
	                      	    +"</td>"
	                      	    +'<td>' + (val.flag==0?"PC":"手机")+ '</td>'
	                      	    +'<td>' + val.alias+ '</td>'
	                        	+'<td>' + val.typeStr + '</td>'
	                        	+'<td>' + val.statusStr + '</td>'
	                        	+'<td>' + (val.bankBalance==null?0:val.bankBalance) + '</td>'
	                        	+'<td>' + val.updateTimeStr + '</td>'
	                        	+'<td>'
	                        		if(financialBT)
	                        			tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="deleteAccount('+val.id+',\'fin\')"><i class="ace-icon fa fa-trash-o bigger-100 dark"></i><span>删除</span></button>';
	                        		tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange  '+OperatorLogBtn+'  " onclick="showModal_accountExtra('+val.id+')"><i class="ace-icon fa fa-book"></i><span>操作记录</span></button>';
				                    if(val.type!=2 && val.type!=3 && val.type!=4 &&val.type!=15){
				            			tr +='<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal('+val.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>流水明细</span></button>';
				            		}
				              tr +='</td>'
                    	 +'</tr>';
                    counts +=1;
                    amounts+=val.bankBalance;
                };
				 $('#fin_tbody').empty().html(tr);
				 var trs = '<tr>'
								 +'<td colspan="7">小计：'+counts+'</td>'
								 +'<td bgcolor="#579EC8" style="color:white;">小计：'+amounts.toFixed(2)+'</td>'
							     +'<td colspan="2"></td>'
						  +'</tr>';
                $('#fin_tbody').append(trs);
                var trn = '<tr>'
			                	+'<td colspan="7">总计：'+jsonObject.data.Page.totalElements+'</td>'
			                	+'<td bgcolor="#D6487E" style="color:white;">总计：'+jsonObject.data.Total+'</td>'
							    +'<td colspan="2"></td>'
				         +'</tr>';
                $('#fin_tbody').append(trn);
				}else {
	                $('#fin_tbody').empty();
	            }
			//加载账号悬浮提示
			$("[data-toggle='popover']").popover();
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.data.Page,"finPage",queryfin);
		}
	});	
}