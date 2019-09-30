currentPageLocation = window.location.href;
	$('#searhByCondition').click(function () {
		queryFinBalanceStatCard();
    });

	//绑定按键事件，回车查询数据
	$('#fczl').bind('keypress',getKeyCodePK);   
	function getKeyCodePK(e) {  
	    var evt = e || window.event;  
	    var keyCode = evt.keyCode || evt.which || evt.charCode;
	    if(keyCode==13){
	    	queryFinBalanceStatCard();
	    }
	}
	
	var genBankType = function(bankTypeId){
	    var ret ='<option value="">请选择</option>';
	    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
	    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true});
	    $('#'+bankTypeId+'_chosen').prop('style', 'width: 120px;')
	};
	
	var request=getRequest();
	//余额明细>明细
	var queryFinBalanceStatCard=function(){
		
		//当前页码
		var CurPage=$("#BalancePage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//如果子页面传过来的 分页页数不为空
		if(request&&request.parentCurPage){
			CurPage=request.parentCurPage;
		}
		
		//明细传过来的值 获取传过来的参数，判断具体查询哪一个的明细
		//这个id是哪个类型的标志，入款账号、出款账号等等
		var showMessage="";
		var id=0;
		var status=0;
		if(request&&request.id){
			id=request.id;
			status=request.status;
		}
		//系统明细返回时，传过来的值
		if(request&&request.type){
			id=request.type;
			status=request.status;
		}
		if(id==1)
			showMessage="入款卡";
		if(id==2)
			showMessage="出款卡";
		if(id==3)
			showMessage="下发卡";
		if(id==4)
			showMessage="备用卡";
		if(id==5)
			showMessage="现金卡";
		if(id==6)
			showMessage="公司入款余额";
		if(id==7)
			showMessage="第三方入款余额";
		if(status==1)
			showMessage+=" - 在用";
		if(status==4)
			showMessage+=" - 停用";	
		if(status==3)
			showMessage+=" - 冻结";
		$('#showType').empty().html(showMessage);
		//获取账号
		var account=$("#account").val();
		var bankType=$("#detailBankType").val();
		
		$.ajax({
			type:"post",
			url:"/r/finbalancestat/finbalancestatcard",
			data:{
				"pageNo":CurPage,
				"id":id,
				"account":account,
				"bankType":bankType,
				"status":status,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
					     //查询成功后，把子页面带过来的当前页码置空
					     request.parentCurPage=null
						 var tr = '';
						 //小计
						 var counts = 0;
						 var balance=0;
						 var bankbalance=0;
						 var idList=new Array();
						 for(var index in jsonObject.data.arrlist){
							 var val = jsonObject.data.arrlist[index];
							 idList.push({'id':val.id});
							 var typee="";
							 if(val.type==1){
								 typee="入款卡";
							 }else if(val.type==2){
								 typee="入款第三方";
							 }else if(val.type==3){
								 typee="入款支付宝";
							 }else if(val.type==4){
								 typee="入款微信";
							 }else if(val.type==5){
								 typee="出款卡";
							 }else if(val.type==6){
								 typee="出款第三方";
							 }else if(val.type==7){
								 typee="入款绑定银行卡";
							 }else if(val.type==8){
								 typee="备用卡";
							 }else if(val.type==9){
								 typee="现金卡";
							 }else if(val.type==10){
								 typee="微信专用";
							 }else if(val.type==11){
								 typee="支付宝专用";
							 }else if(val.type==12){
								 typee="第三方专用";
							 }else if(val.type==13){
								 typee="公用绑定";
							 }
	                        tr += '<tr>'
			                        	+"<td>" 
			                        	+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
			                        	+"</a>";
	                                    if(typee!='入款第三方' && typee!='入款微信' && typee!='入款支付宝')
	                                    	tr +="<span title='编号|开户人|银行类别'><br/>"+val.alias+"|"+val.owner+"|"+val.banktype+"</span>";
	                                    tr +="</td>"
			                        	+'<td>'+ typee +'</td>'
			                        	+'<td>'+ val.bankname +'</td>'
			                        	+'<td>'+ val.bankbalance +'</td>'
			                        	+'<td>'+ val.balance +'</td>'
			                        	+'<td>';
			                        		if(typee=='入款第三方'){
			                        			 tr+='<a href="#/finTransBalanceSys:*?accountname='+val.account+'&id='+id+'&account='+account+'&CurPage='+CurPage+'&type='+val.type+'&status='+status+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                                         +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</a>';
			                        		}else{
			                        			 tr+='<a href="#/finTransBalanceSys:*?accountid='+val.id+'&id='+id+'&account='+account+'&CurPage='+CurPage+'&type='+val.type+'&status='+status+'" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                                         +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>系统明细</a>';
			                        		}
			                        	    
		                                    //第三方、微信、支付宝没有银行流水、
		                                    if(typee!='入款第三方' && typee!='入款微信' && typee!='入款支付宝'){
		                                    	tr +='<a onclick="showInOutListModal('+val.id+')" type="button"  class="btn btn-xs btn-white btn-primary btn-bold">'
		                                        +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>明细</button>'
		                                    }     
	                               tr +='</td>'
                                 +'</tr>';
	                        counts +=1;
	                        balance+=val.balance;
	                        bankbalance+=val.bankbalance;
	                    };
						 $('#total_tbody').empty().html(tr);
						 var trs = '<tr>'
										 +'<td colspan="3">小计：'+counts+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+bankbalance.toFixed(2)+'</td>'
										 +'<td bgcolor="#579EC8" style="color:white;">'+balance.toFixed(2)+'</td>'
									     +'<td></td>'
						          +'</tr>';
	                    $('#total_tbody').append(trs);
	                    var trn = '<tr>'
				                    	+'<td colspan="3" >总计：'+jsonObject.data.page.totalElements+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[1]+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+new Array(jsonObject.data.total[0]).toString().split(",")[0]+'</td>'
				                    	+'<td></td>'
	                    	     +'</tr>';
	                    $('#total_tbody').append(trn);
				}else{
					$('#total_tbody').empty().html('<tr></tr>');
				}
				//加载账号悬浮提示
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.page,"BalancePage",queryFinBalanceStatCard);
			}
		});
		
	}
	
	
	
	jQuery(function($) {
		//返回按钮处理，设置查询时的值  账号统计
		if(request&&request.parentaccount){
			$("#account").val(request.parentaccount);
		}
		genBankType("detailBankType");
		queryFinBalanceStatCard();
	})