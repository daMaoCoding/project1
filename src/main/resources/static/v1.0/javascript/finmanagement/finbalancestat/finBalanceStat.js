currentPageLocation = window.location.href;

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
					var height=(intSpan+2);
					var ht=intSpan;
					//设置文本居中
					tabObj.rows[i].cells[0].style="line-height:"+height;
					tabObj.rows[j].cells[0].style.display = "none";
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

function SpanGridEveyDay(tabObj,colIndex){
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
					var height=(intSpan+2);
					var ht=intSpan;
					//设置文本居中
					tabObj.rows[i].cells[0].style="line-height:"+height;
					tabObj.rows[j].cells[0].style.display = "none";
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

function SpanGridEveyDayType(tabObj,colIndex){
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
					tabObj.rows[i].cells[1].rowSpan  = intSpan;
					var height=(intSpan+2);
					var ht=intSpan;
					//设置文本居中
					tabObj.rows[i].cells[1].style="line-height:"+height;
					tabObj.rows[j].cells[1].style.display = "none";
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


function initTimePickerHandicap(name){
    start=moment().add(-6,'days');
	end=moment();
   var exportStartAndEndTime = $("[name="+name+"]").daterangepicker({
		autoUpdateInput:false,
		timePicker: false, //显示时间
	    timePicker24Hour: false, //24小时制
	    timePickerSeconds:false,//显示秒
	    startDate: start, //设置开始日期
       endDate: end, //设置结束日期
		locale: {
			"format": 'YYYY-MM-DD',
			"separator": " - ",
			"applyLabel": "确定",
			"cancelLabel": "取消",
			"fromLabel": "从",
			"toLabel": "到",
			"customRangeLabel": "Custom",
			"dayNames": ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
			"daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
			"monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
			"firstDay": 1
		}
	}).val((start.format('YYYY-MM-DD')+' - '+end.format('YYYY-MM-DD')));
   exportStartAndEndTime.on('apply.daterangepicker', function(ev, picker) {
		$(this).val(picker.startDate.format('YYYY-MM-DD') + ' - ' + picker.endDate.format('YYYY-MM-DD'));
		queryEveryDay();
   });
   exportStartAndEndTime.on('cancel.daterangepicker', function(ev, picker) {
	   		$(this).val(start.format('YYYY-MM-DD') + ' - ' + end.format('YYYY-MM-DD')); 
	   		queryEveryDay();
	   });
}


	//余额明细
	var queryNowBalance=function(){
		$.ajax({
			type:"post",
			url:"/r/finbalancestat/finbalancestat",
			data:{
				"pageNo":0},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
						 var tr = '';
						 //小计
						 var counts = 0;
						 var bankUses=0;
						 var bankStops=0;
						 var bankCanUses=0;
						 var sysUses=0;
						 var sysStops=0;
						 var sysCanUses=0;
						 var bankAmountsTotal=0;
						 var sysAmountsTotal=0;
						 for(var index in jsonObject.data.arrlist){
							 var val = jsonObject.data.arrlist[index];
							 var bankAmounts=0;
							 var sysAmounts=0;
							 bankAmounts+=((val.bankUse*1)+(val.bankStop*1)+(val.bankCanUse*1));
							 sysAmounts+=((val.sysUse*1)+(val.sysStop*1)+(val.sysCanUse*1));
							 bankAmountsTotal+=bankAmounts;
							 sysAmountsTotal+=sysAmounts;
	                         tr += '<tr>'
			                        	+'<td>' + val.handicapname + '</td>'
			                        	+'<td><a href="#/finBalanceStatMatch:*?id='+val.id+'&status=1">' + val.bankUse + '</a></td>'
			                        	+'<td><a href="#/finBalanceStatMatch:*?id='+val.id+'&status=4">'+ val.bankStop +'</a></td>'
			                        	+'<td><a href="#/finBalanceStatMatch:*?id='+val.id+'&status=3">'+ val.bankCanUse +'</a></td>'
			                        	+'<td>' + bankAmounts.toFixed(2) + '</td>'
			                        	+'<td><a href="#/finBalanceStatMatch:*?id='+val.id+'&status=1">' + val.sysUse + '</a></td>'
			                        	+'<td><a href="#/finBalanceStatMatch:*?id='+val.id+'&status=4">'+ val.sysStop +'</a></td>'
			                        	+'<td><a href="#/finBalanceStatMatch:*?id='+val.id+'&status=3">'+ val.sysCanUse +'</a></td>'
			                        	+'<td>' + sysAmounts.toFixed(2) + '</td>'
			                     +'</tr>';
	                        counts +=1;
	                        bankUses+=val.bankUse;
	                        bankStops+=val.bankStop;
	                        bankCanUses+=val.bankCanUse;
	                        sysUses+=val.sysUse;
	                        sysStops+=val.sysStop;
	                        sysCanUses+=val.sysCanUse;
	                    };
						 $('#total_tbody').empty().html(tr);
	                    var trn = '<tr>'
				                    	+'<td>总计：'+counts+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankStops.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankCanUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+bankAmountsTotal.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysStops.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysCanUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+sysAmountsTotal.toFixed(2)+'</td>'
	                    	     +'</tr>';
	                    $('#total_tbody').append(trn);
				}
			}
		});
		
	}
	
	
	var queryEveryDay=function(){
		//获取盘口
		var handicap=$("#handicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		
		var startAndEndTime = $("input[name='auditCommissionTime']").val();
		var startAndEndTimeToArray = new Array();
		if(startAndEndTime){
			var startAndEnd = startAndEndTime.split(" - ");
			startAndEndTimeToArray.push($.trim(startAndEnd[0]));
			startAndEndTimeToArray.push($.trim(startAndEnd[1]));
		}
		startAndEndTimeToArray = startAndEndTimeToArray.toString();
		$.ajax({
			type:"post",
			url:"/r/finbalancestat/finbalanceEveryDay",
			data:{"pageNo":0,
				 "startAndEndTime":startAndEndTimeToArray,
				 "handicapId":handicap
			},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
						 var tr = '';
						//小计
						 var bankUses=0;
						 var bankStops=0;
						 var bankCanUses=0;
						 var sysUses=0;
						 var sysStops=0;
						 var sysCanUses=0;
						 var countts=0;
						 var time=jsonObject.data.arrlist[0].banktype;
						 var nextTime="";
						 var bankAmountsTotal=0;
						 var sysAmountsTotal=0;
						 var jo=1;
						 
						 var differenceBankUses=0;
						 var differenceBankStops=0;
						 var differenceBankCanUses=0;
						 var differenceSysUses=0;
						 var differenceSysStops=0;
						 var differenceSysCanUses=0;
						 var differenceBankAmountsTotal=0;
						 var differenceSysAmountsTotal=0;
						 for(var index in jsonObject.data.arrlist){
							var val = jsonObject.data.arrlist[index];
							nextTime=val.banktype;
							 var bankAmounts=0;
							 var sysAmounts=0;
							 var differenceBankAmounts=0;
							 var differenceSysAmounts=0;
							 bankAmounts+=((val.bankUse*1)+(val.bankStop*1)+(val.bankCanUse*1));
							 sysAmounts+=((val.sysUse*1)+(val.sysStop*1)+(val.sysCanUse*1));
							 if(nextTime!=time){
								 jo=((jo*1)+(2*1));	
								 if(jo>5){
									 tr+= "<tr id="+time+">"
				                    	+'<td id="count'+time+'" bgcolor="#579EC8" style="color:white;">.'+time+'<span style="cursor:pointer" onclick="showDeal(\''+time+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-down"></i></span><span style="cursor:pointer" onclick="showDifference(\''+time+'\','+countts+','+jo+')">&nbsp;&nbsp;差额<i class="fa fa-angle-double-down"></i></span></td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;"></td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankStops.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankCanUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+bankAmountsTotal.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysStops.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysCanUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+sysAmountsTotal.toFixed(2)+'</td>'
	                    	      +'</tr>';
								 }else{
									 tr+= "<tr id="+time+">"
				                    	+'<td id="count'+time+'" bgcolor="#579EC8" style="color:white;">.'+time+'<span style="cursor:pointer" onclick="showDeal(\''+time+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-down"></i></span></td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;"></td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankStops.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankCanUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+bankAmountsTotal.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysStops.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysCanUses.toFixed(2)+'</td>'
				                    	+'<td bgcolor="#D6487E" style="color:white;">'+sysAmountsTotal.toFixed(2)+'</td>'
	                    	        +'</tr>';
								 }
		                        	if(((jo%2)==1)){
		                        		if(jo>3&&jo<=5){
		                        			tr+="<tr style='cursor:pointer;' name="+time+'difference'+" onclick='showDifference(\""+time+"\","+countts+","+jo+")'>"
		                        				+'<td id="difference'+time+'" bgcolor="#DAA520" style="color:white;">差额<i class="fa fa-angle-double-up"></i></td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;"></td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankUses.toFixed(2)-bankUses.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankStops.toFixed(2)-bankStops.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankCanUses.toFixed(2)-bankCanUses.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankAmountsTotal.toFixed(2)-bankAmountsTotal.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysUses.toFixed(2)-sysUses.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysStops.toFixed(2)-sysStops.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysCanUses.toFixed(2)-sysCanUses.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysAmountsTotal.toFixed(2)-sysAmountsTotal.toFixed(2)).toFixed(2)+'</td>'
		                        			+'</tr>';
		                        		}else if(jo>5){
		                        			tr+="<tr style='cursor:pointer;display: none;' name="+time+'difference'+" onclick='showDifference(\""+time+"\","+countts+","+jo+")'>"
		                        			    +'<td id="difference'+time+'" bgcolor="#DAA520" style="color:white;">差额<i class="fa fa-angle-double-up"></i></td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;"></td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankUses.toFixed(2)-bankUses.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankStops.toFixed(2)-bankStops.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankCanUses.toFixed(2)-bankCanUses.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankAmountsTotal.toFixed(2)-bankAmountsTotal.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysUses.toFixed(2)-sysUses.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysStops.toFixed(2)-sysStops.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysCanUses.toFixed(2)-sysCanUses.toFixed(2)).toFixed(2)+'</td>'
		                        				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysAmountsTotal.toFixed(2)-sysAmountsTotal.toFixed(2)).toFixed(2)+'</td>'
		                        			+'</tr>';
		                        		}
		                        	}
		                        	// $('#everyDay_tbody').append(trn);
		                        	 differenceBankUses=bankUses.toFixed(2)*1;
									 differenceBankStops=bankStops.toFixed(2)*1;
									 differenceBankCanUses=bankCanUses.toFixed(2)*1;
									 differenceSysUses=sysUses.toFixed(2)*1;
									 differenceSysStops=sysStops.toFixed(2)*1;
									 differenceSysCanUses=sysCanUses.toFixed(2)*1;
									 differenceBankAmountsTotal=bankAmountsTotal.toFixed(2)*1;
									 differenceSysAmountsTotal=sysAmountsTotal.toFixed(2)*1;
		                        	
		                        	 countts=0;
		                        	 bankUses=0;
		 	                         bankStops=0;
		 	                         bankCanUses=0;
		 	                         sysUses=0;
		 	                         sysStops=0;
		 	                         sysCanUses=0;
		 	                        bankAmountsTotal=0;
		 	                        sysAmountsTotal=0;
		                        	 time=val.banktype;
		                        }
	                        tr += '<tr name='+time+' style="display: none;">'
			                        	+'<td>' + val.banktype + '</td>'
			                        	+'<td>' + val.handicapname + '</td>'
			                        	+"<td><a onclick='detail(\""+val.banktype+"\","+val.id+",1)'>" + val.bankUse.toFixed(2) + "</a></td>"
			                        	+"<td><a onclick='detail(\""+val.banktype+"\","+val.id+",4)'>"+ val.bankStop.toFixed(2) +"</a></td>"
			                        	+"<td><a onclick='detail(\""+val.banktype+"\","+val.id+",3)'>"+ val.bankCanUse.toFixed(2) +"</a></td>"
			                        	+'<td>' + bankAmounts.toFixed(2) + '</td>'
			                        	+"<td><a onclick='detail(\""+val.banktype+"\","+val.id+",1)'>" + val.sysUse.toFixed(2) + "</a></td>"
			                        	+"<td><a onclick='detail(\""+val.banktype+"\","+val.id+",4)'>"+ val.sysStop.toFixed(2) +"</a></td>"
			                        	+"<td><a onclick='detail(\""+val.banktype+"\","+val.id+",3)'>"+ val.sysCanUse.toFixed(2) +"</a></td>"
			                        	+'<td>' + sysAmounts.toFixed(2) + '</td>'
			                   +'</tr>';
	                        bankUses+=val.bankUse;
	                        bankStops+=val.bankStop;
	                        bankCanUses+=val.bankCanUse;
	                        sysUses+=val.sysUse;
	                        sysStops+=val.sysStop;
	                        sysCanUses+=val.sysCanUse;
	                        bankAmountsTotal+=bankAmounts;
							sysAmountsTotal+=sysAmounts;
	                        countts++;
	                        if((index*1+1)==jsonObject.data.arrlist.length){
	                        	tr+= "<tr id="+time+")'>"
	                        	+'<td id="count'+time+'" bgcolor="#579EC8" style="color:white;">.'+time+'<span style="cursor:pointer" onclick="showDeal(\''+time+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-down"></i></span><span style="cursor:pointer" onclick="showDifference(\''+time+'\','+countts+','+jo+')">&nbsp;&nbsp;差额<i class="fa fa-angle-double-down"></i></span></td>'
			                    	+'<td bgcolor="#579EC8" style="color:white;"></td>'
			                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankUses.toFixed(2)+'</td>'
			                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankStops.toFixed(2)+'</td>'
			                    	+'<td bgcolor="#579EC8" style="color:white;">'+bankCanUses.toFixed(2)+'</td>'
			                    	+'<td bgcolor="#D6487E" style="color:white;">'+bankAmountsTotal.toFixed(2)+'</td>'
			                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysUses.toFixed(2)+'</td>'
			                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysStops.toFixed(2)+'</td>'
			                    	+'<td bgcolor="#579EC8" style="color:white;">'+sysCanUses.toFixed(2)+'</td>'
			                    	+'<td bgcolor="#D6487E" style="color:white;">'+sysAmountsTotal.toFixed(2)+'</td>'
                    	          +'</tr>';
	                        	tr+="<tr style='cursor:pointer;display: none;' name="+time+'difference'+" onclick='showDifference(\""+time+"\","+countts+","+jo+")'>"
	                        	   +'<td id="difference'+time+'" bgcolor="#DAA520" style="color:white;">差额<i class="fa fa-angle-double-up"></i></td>'
                    				+'<td bgcolor="#DAA520" style="color:white;"></td>'
                    				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankUses.toFixed(2)-bankUses.toFixed(2)).toFixed(2)+'</td>'
                    				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankStops.toFixed(2)-bankStops.toFixed(2)).toFixed(2)+'</td>'
                    				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankCanUses.toFixed(2)-bankCanUses.toFixed(2)).toFixed(2)+'</td>'
                    				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceBankAmountsTotal.toFixed(2)-bankAmountsTotal.toFixed(2)).toFixed(2)+'</td>'
                    				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysUses.toFixed(2)-sysUses.toFixed(2)).toFixed(2)+'</td>'
                    				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysStops.toFixed(2)-sysStops.toFixed(2)).toFixed(2)+'</td>'
                    				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysCanUses.toFixed(2)-sysCanUses.toFixed(2)).toFixed(2)+'</td>'
                    				+'<td bgcolor="#DAA520" style="color:white;">'+(differenceSysAmountsTotal.toFixed(2)-sysAmountsTotal.toFixed(2)).toFixed(2)+'</td>'
                    			+'</tr>';
	                        	// $('#everyDay_tbody').append(trn);
	                        	 countts=0;
	                        	 time=val.banktype;
	                        }
	                    };
						 $('#everyDay_tbody').empty().html(tr);
						 SpanGridEveyDay(everyDay_tbody,0);
						 SpanGridEveyDayType(everyDay_tbody,1);
				}else {
					$('#everyDay_tbody').empty().html('<tr><td colspan="10">无数据！</td></tr>');
	            }
			}
		});
		
	}
	
	function showDeal(index,countts,jo){
		if($("[name='"+index+"']").is(":hidden")){
			$("[name='"+index+"']").show();
			if(jo>=5){
				if($("[name='"+index+"difference']").is(":hidden")){
					$("#count"+index).html("总计："+countts+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-up"></i></span><span style="cursor:pointer" onclick="showDifference(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;差额<i class="fa fa-angle-double-down"></i></span>');
				}else{
					$("#count"+index).html("总计："+countts+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-up"></i></span>');
				}
			}else{
				$("#count"+index).html("总计："+countts+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-up"></i></span>');
			}
		}else{
			$("[name='"+index+"']").hide();
			if(jo>=5){
				if($("[name='"+index+"difference']").is(":hidden")){
					$("#count"+index).html("."+index+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-down"></i></span><span style="cursor:pointer" onclick="showDifference(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;差额<i class="fa fa-angle-double-down"></i></span>');
				}else{
					$("#count"+index).html("."+index+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-down"></i></span>');
				}
			}else{
				$("#count"+index).html("."+index+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-down"></i></span>');
			}
		}
	}
	
	function showDifference(index,countts,jo){
		if($("[name='"+index+"difference']").is(":hidden")){
			$("[name='"+index+"difference']").show();
			$("#difference"+index).html('&nbsp;&nbsp;差额<i class="fa fa-angle-double-up"></i>');
			if(jo>=5){
				if($("[name='"+index+"']").is(":hidden")){
					$("#count"+index).html("."+index+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-down"></i></span>');
				}else{
					$("#count"+index).html("总计："+countts+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-up"></i></span>');
				}
			}
		}else{
			$("[name='"+index+"difference']").hide();
			if($("[name='"+index+"']").is(":hidden")){
				$("#count"+index).html("."+index+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-down"></i></span><span style="cursor:pointer" onclick="showDifference(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;差额<i class="fa fa-angle-double-down"></i></span>');
			}else{
				$("#count"+index).html("总计："+countts+'<span style="cursor:pointer" onclick="showDeal(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;明细<i class="fa fa-angle-double-up"></i></span><span style="cursor:pointer" onclick="showDifference(\''+index+'\','+countts+','+jo+')">&nbsp;&nbsp;差额<i class="fa fa-angle-double-down"></i></span>');
			}
		}
	}
	
	var genBankType = function(bankTypeId){
	    var ret ='<option value="">请选择</option>';
	    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
	    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true});
	    $('#'+bankTypeId+'_chosen').prop('style', 'width: 120px;')
	};
	
	var detailTime="";
	var detailType="";
	var detailStatus="";
	function detail(time,type,status){
		$('#deal').modal('show');
		//获取盘口
		var handicap=$("#handicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		if(""!=time && undefined!=time){
			detailTime=time;
			detailType=type;
			detailStatus=status;
		}
		if(""==time || undefined==time){
			time=detailTime;
			type=detailType;
			status=detailStatus;
		}
		var account=$("#account").val();
		var bankType=$("#bankType").val();
		var handicap=$("#handicap").val();
		if(handicap=="" || handicap==null){
			handicap=0;
		}
		
		//当前页码
		var CurPage=$("#dealPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		
		$.ajax({
			type:"post",
			url:"/r/finbalancestat/findBalanceDetail",
			data:{
				"pageNo":CurPage,
				"account":account,
				"bankType":bankType,
				"handicap":handicap,
				"time":time,
				"type":type,
				"status":status,
				"pageSize":$.session.get('initPageSize')},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.rebatelist.length > 0){
					var tr = '';
					 //小计
					 var counts = 0;
					 var bankAmounts=0;
					 var sysAmounts=0;
					 var idList=new Array();
					 for(var index in jsonObject.data.rebatelist){
						 var val = jsonObject.data.rebatelist[index];
						 idList.push({'id':val.id});
						 tr += '<tr>'
							 	+'<td>' + val.time + '</td>'
							 	 +"<td title='编号|银行类别'>" 
		                       	      +"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(val.account)
		                       	      +"</a></br>"
		                       	      +val.alias+"|"+val.banktype
		                   	      +"</td>"
							 	+'<td>' + val.banktype + '</td>'
			                 	+'<td>' + val.bankBalance.toFixed(2) + '</td>'
			                 	+'<td>' + val.amount.toFixed(2) + '</td>'
		                 	+'</tr>';
		                counts +=1;
		                bankAmounts+=val.bankBalance;
		                sysAmounts+=val.amount;
		            };
					 $('#deal_tbody').empty().html(tr);
					 var trs = '<tr>'
								 +'<td colspan="3">小计：'+counts+'</td>'
								 +'<td bgcolor="#579EC8" style="color:white;">'+bankAmounts.toFixed(2)+'</td>'
								 +'<td bgcolor="#579EC8" style="color:white;">'+sysAmounts.toFixed(2)+'</td>'
								 +'<td></td>'
							  +'</tr>';
		            $('#deal_tbody').append(trs);
		            var trn = '<tr>'
				            	+'<td colspan="3">总计：'+jsonObject.data.rebatePage.totalElements+'</td>'
				            	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[0]).toFixed(2)+'</td>'
				            	+'<td bgcolor="#D6487E" style="color:white;">'+Math.abs(new Array(jsonObject.data.rebateTotal[0]).toString().split(",")[1]).toFixed(2)+'</td>'
				            	+'<td></td>'
					         +'</tr>';
		            $('#deal_tbody').append(trn);
					}else {
		                $('#deal_tbody').empty();
		            }
				loadHover_accountInfoHover(idList);
				//分页初始化
				showPading(jsonObject.data.rebatePage,"dealPage",detail);
			}
});
	}
	
	
	jQuery(function($) {
		queryNowBalance();
		initTimePickerHandicap("auditCommissionTime");
		getHandicap_select($("#handicap"),null,"全部");
		genBankType('bankType');
		//if(Liquidation)
			//$("#clear").show();
	});
	
	//清算按钮
	$('#clear').click(function () {
		showClearListModal();
    });
	
	//清算
	var Liquidation=false;
	//强制清算
	var CompulsoryLiquidation=false;
	$.each(ContentRight['FinBalanceStat:*'], function (name, value) {
	    if (name == 'FinBalanceStat:Liquidation:*') {
	    	Liquidation = true;
	    } else if (name == 'FinBalanceStat:CompulsoryLiquidation:*') {
	    	CompulsoryLiquidation = true;
	    }
	});
	
	/**
	*	初始化时间控件
	*/
	var initTimePickerBalance=function(){
		var startAndEndTime = $('input.date-range-picker').daterangepicker({
			autoUpdateInput:false,
			singleDatePicker: true,
			//timePicker: true, //显示时间
		    timePicker24Hour: true, //24小时制
		   // timePickerSeconds:true,//显示秒
			locale: {
				"format": 'YYYY-MM-DD HH:mm:ss',
				"separator": " - ",
				"applyLabel": "确定",
				"cancelLabel": "取消",
				"fromLabel": "从",
				"toLabel": "到",
				"customRangeLabel": "Custom",
				"dayNames": ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
				"daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
				"monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
				"firstDay": 1
			}
		}).val('');
		startAndEndTime.on('apply.daterangepicker', function(ev, picker) {
			$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss'));
			//确定时候查询数据
			queryClearDate();
		});
		startAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
	}
	
	var showClearListModal=function(){
		//发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
		$.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
		var $div;
		$.ajax({ 
			type:"GET",
			async:false, 
			dataType:'html',
			url : "/"+sysVersoin+"/html/finmanagement/finbalancestat/clearAccountData.html", 
			success : function(html){
				var $div=$(html).find("#clearAccountDateListModal").clone().appendTo($("body"));
				//初始化时间
				initTimePickerBalance();
				//queryClearDate();
				$div.modal("toggle");
			}
		});
	};
	
	var toMinimize=function(){
		$("#clearAccountDateListModal").modal("toggle");
		$("#incomeAccountComp").show();
		$("#outwardAccountBankUsed").show();
	}
	var closeModal=function(){
		$("#clearAccountDateListModal").modal("toggle");
		$("#clearAccountDateListModal,.modal-backdrop").remove();
		$("body").removeClass('modal-open');
		$("#incomeAccountComp").hide();
		$("#outwardAccountBankUsed").hide();
	}
	
	//查询还在匹配中的数据，如果没有数据返回 则表示可以清算数据
	function queryClearDate(){
		//当前页码
		var CurPage=$("#clearAccountDateListPage").find(".Current_Page").text();
		if(!!!CurPage){
			CurPage=0;
		}else{
			CurPage=CurPage-1;
		}if(CurPage<0){
			CurPage=0;
		}
		//日期 条件封装
		var startAndEndTime = $("input[name='startAndEndTime']").val();
		if(startAndEndTime=="" || startAndEndTime==null){
			bootbox.alert("请选择需要清算的时间！");
			return;
		}
		$.ajax({
			type:"post",
			url:"/r/finbalancestat/clearaccountdate",
			data:{
				"pageNo":CurPage,
				"startAndEndTime":startAndEndTime,
				"pageSize":$.session.get('initPageSize')
				},
			dataType:'json',
			success:function(jsonObject){
				if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
					 var tr = '';
					 for(var index in jsonObject.data.arrlist){
						 var val = jsonObject.data.arrlist[index];
						 var vtype="";
						 var status="";
						 var url="";
						 if(val.type=="1"){
							 vtype="入款卡";
							 var url="setCurrMenuData('#/IncomeAccountComp:*');window.location.href='#/IncomeAccountComp:*'";
						 }else if(val.type=="2"){
							 vtype="入款第三方";
							 var url="setCurrMenuData('#/IncomeAccountThird:*');window.location.href='#/IncomeAccountThird:*'";
						 }else if(val.type=="3"){
							 vtype="入款支付宝";
							 var url="setCurrMenuData('#/IncomeAccountComp:*');window.location.href='#/IncomeAccountComp:*'";
						 }else if(val.type=="4"){
							 vtype="入款微信";
							 var url="setCurrMenuData('#/IncomeAccountComp:*');window.location.href='#/IncomeAccountComp:*'";
						 }else if(val.type=="5"){
							 vtype="出款卡";
							 if(val.bankcount=="1")
								 var url="setCurrMenuData('#/OutwardAccountBankUsed:*');window.location.href='#/OutwardAccountBankUsed:*'";
							 else if(val.bankcount=="5")
								 var url="setCurrMenuData('#/OutwardAccountBankEnabled:*');window.location.href='#/OutwardAccountBankEnabled:*'";
							 else if(val.bankcount=="4")
								 var url="setCurrMenuData('#/OutwardAccountBankStop:*');window.location.href='#/OutwardAccountBankStop:*'";
							 else if(val.bankcount=="3")
								 var url="setCurrMenuData('#/OutwardAccountBankFreezed:*');window.location.href='#/OutwardAccountBankFreezed:*'";
						 }else if(val.type=="6"){
							 vtype="出款第三方";
						 }else if(val.type=="7"){
							 vtype="下发卡";
							 var url="setCurrMenuData('#/IncomeAsignCompBank:*');window.location.href='#/IncomeAsignCompBank:*'";
						 }else if(val.type=="8"){
							 vtype="备用卡";
						 }else if(val.type=="9"){
							 vtype="现金卡";
						 }else if(val.type=="10"){
							 vtype="微信专用";
						 }else if(val.type=="11"){
							 vtype="支付宝专用";
						 }else if(val.type=="12"){
							 vtype="第三方专用";
						 }else if(val.type=="13"){
							 vtype="下发卡";
							 var url="setCurrMenuData('#/IncomeAsignComnBank:*');window.location.href='#/IncomeAsignComnBank:*'";
						 }
						 if(val.bankcount=="1"){
							 status="在用";
						 }else if(val.bankcount=="3"){
							 status="冻结";
						 }else if(val.bankcount=="4"){
							 status="停用";
						 }else if(val.bankcount=="5"){
							 status="可用";
						 }
                         tr += '<tr>'
		                        	+'<td><a onclick="'+url+'">' + val.account + '</a></td>'
		                        	+'<td>'+ vtype +'</td>'
		                        	+'<td>'+ status +'</td>'
		                        	+'<td>'+ val.counts +'</td>'
                              +'</tr>';
                    };
                    $('#clear_total_tbody').empty().html(tr);
                    if(CompulsoryLiquidation){
                    	var cent="";
     					cent+='<span style="font-size: 14pt; color: #ff3333; font-family:宋体" align="center" valign="middle">可以强制清算所选时间之前的数据！</span>';
     					cent+='<a type="button" type="button" onclick="cleardate()" class="btn btn-xs btn-white btn-primary btn-bold">'
                         +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>强制清算</a>';
     					$('#result').empty().html(cent);
                    }else{
                    	$('#result').empty().html('<span style="font-size: 14pt; color: #ff3333; font-family:宋体" align="center" valign="middle">不能清算所选时间之前的数据！</span>');
                    }
				}else{
					$('#clear_total_tbody').empty().html('<tr></tr>');
					var cent="";
					cent+='<span style="font-size: 14pt; color: green; font-family:宋体" align="center" valign="middle">可以清算所选时间之前的数据！</span>';
					cent+='<a type="button" type="button" onclick="cleardate()" class="btn btn-xs btn-white btn-primary btn-bold">'
                    +'<i class="ace-icon fa fa-list  bigger-100 orange"></i>清算</a>';
					$('#result').empty().html(cent);
				}
				//分页初始化
				showPading(jsonObject.data.page,"clearAccountDateListPage",queryClearDate);
			}
		});
	};
	//清空所选时间的数据
	function cleardate(){
		var from_date = new Date();
		var end_date = new Date($("input[name='startAndEndTime']").val());
		var time_different = (from_date - end_date) / 86400000; //也就是24*60*60*1000 单位是毫秒
		if(time_different <= 10){
			$.gritter.add({
                time: 900,
                class_name: '',
                title: '系统消息',
                text: "不能清算十天内的数据！",
                sticky: false,
                image: '../images/message.png'
            });
			return;
		}
		bootbox.dialog({
			message: '确定清算<span style="color: #ff3333; font-family:宋体" align="center" valign="middle">'+$("input[name='startAndEndTime']").val()+'</span>之前(包括所选日期当天)的数据吗？',
			buttons:{
				"click" :{"label" : "确定","className" : "btn-sm btn-primary","callback": function() {
					$.ajax({
						type:"post",
						url:"/r/finbalancestat/deleteaccountdate",
						data:{
							"startAndEndTime":$("input[name='startAndEndTime']").val()
							},
						dataType:'json',
						success:function(jsonObject){
							$.gritter.add({
			                    time: 500,
			                    class_name: '',
			                    title: '系统消息',
			                    text: jsonObject.data.message,
			                    sticky: false,
			                    image: '../images/message.png'
			                });
							queryClearDate();
						}
					});
				}},
				"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
			}
		});
	}
	
	