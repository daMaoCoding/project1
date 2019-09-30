/**
 * 初始化时间控件(按盘口统计)
 */
var initTimePickerHandicap=function(){
	var startAndEndTime = $('input.date-range-picker').daterangepicker({
		autoUpdateInput:false,
		timePicker: true, // 显示时间
	    timePicker24Hour: true, // 24小时制
	    timePickerSeconds:true,//显示秒
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
		$(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
		// 清空的同时查询数据
		_searchByFilter();
	});
	startAndEndTime.on('cancel.daterangepicker', function(ev, picker) { $(this).val(''); });
}	

// 初始化盘口
var inithancipad=function(){
	var $selectHandicap = $("select[name='handicap_matched']").html("");
	$.ajax({dataType:'json',
		type:"get",
		async:false,
		url:"/r/handicap/list",
		data:{enabled:1},
		success:function(jsonObject){
		if(jsonObject.status == 1){
			$selectHandicap.html("");
			$('<option></option>').html('全部').attr('value','0').attr('selected','selected').attr("handicapCode","").appendTo($selectHandicap);
			for(var index in jsonObject.data){
				var item = jsonObject.data[index];
				$('<option></option>').html(item.name).attr('value',item.id).attr("handicapCode",item.code).appendTo($selectHandicap);
			}
		}
	}});
}


// 如果盘口为全部，层级不能选
function changeLevel(){
	var handicap=$("#handicap_matched").val();
		// 初始化层级
		var $selectLevel =  $("select[name='level_matched']").html('');
		$.ajax({dataType:'json',
			async:false,
			type:"get",url:"/r/finoutstat/findbyhandicapid",
			data:{"handicap":handicap},
			success:function(jsonObject){
				if(jsonObject.status == 1){
					$selectLevel.html('');
					$('<option></option>').html('全部').attr('value','0').attr('selected','selected').attr("levelCode","").appendTo($selectLevel);
					for(var index in jsonObject.data){
						var item = jsonObject.data[index];
						$('<option></option>').html(item.name).attr('value',item.id).attr("levelCode",item.code).appendTo($selectLevel);
					}
				}
		}});
}


function clearNoNum(obj)
{
    //先把非数字的都替换掉，除了数字和.
    obj.value = obj.value.replace(/[^\d.]/g,"");
    //必须保证第一个为数字而不是.
    obj.value = obj.value.replace(/^\./g,"");
    //保证只有出现一个.而没有多个.
    obj.value = obj.value.replace(/\.{2,}/g,".");
    //保证.只出现一次，而不能出现两次以上
    obj.value = obj.value.replace(".","$#$").replace(/\./g,"").replace("$#$",".");
}

var timer="";
// 定时循环执 行
function search(){
	if(timer!=null){
		clearInterval(timer);
	}
	timer = setInterval(function(){
		_searchByFilter(); 
	}, $("#chooseSecond").val());
}
//绑定查询按钮事件
$('#searchThird').click(function () {
	_searchByFilter();
});

//绑定按键事件，回车查询数据
$('#fczl').bind('keypress',getKeyCodePK);   
function getKeyCodePK(e) {  
    var evt = e || window.event;  
    var keyCode = evt.keyCode || evt.which || evt.charCode;
    if(keyCode==13){
    	$('#searchThird').click();
    }
}

/**
 * 查询
 * 
 * @param pageNo
 */
function _searchByFilter() {
	// 当前页码
	var CurPage=$("#thirdmatchPage").find(".Current_Page").text();
	if(!!!CurPage){
		CurPage=0;
	}else{
		CurPage=CurPage-1;
	}if(CurPage<0){
		CurPage=0;
	}
	// 获取盘口
	var handicap=$("#handicap_matched").val();
	if(handicap=="" || handicap==null || handicap=='全部'){
		handicap=0;
	}
	// 获取层级
	var level=$("#level_matched").val();
	if(level=="" || level==null || level=='全部'){
		level=0;
	}
	
	// 会员账号
	var account=$("#member_matched").val();
	// 第三方名称
	var thirdaccount=$("#thirdName_matched").val();
	// 获取时间段
	// 日期 条件封装
	var startAndEndTime = $("input[name='startAndEndTime']").val();
	var startAndEndTimeToArray = new Array();
	if(startAndEndTime){
		var startAndEnd = startAndEndTime.split(" - ");
		startAndEndTimeToArray.push($.trim(startAndEnd[0]));
		startAndEndTimeToArray.push($.trim(startAndEnd[1]));
	}
	startAndEndTimeToArray = startAndEndTimeToArray.toString();
	
	// 收款账号
	var toaccount=$("#toAccount_matched").val();
	
	// 金额
	var startamount=$("#fromMoney_matched").val();
	if(startamount=="" || startamount==null){
		startamount=0;
	}
	var endamount=$("#toMoney_matched").val();
	if(endamount=="" || endamount==null){
		endamount=0;
	}
	if((startamount!="" && startamount!=null) || (endamount!="" && endamount!=null)){
		if(startamount==0){
			bootbox.alert("不能从0开始！");
			return;
		}
		if(endamount=="" || endamount==null || startamount=="" || startamount==null){
			bootbox.alert("请输入金额范围！");
			return;
		}else if(Math.abs(startamount)>Math.abs(endamount)){
			bootbox.alert("结束金额不能小于开始金额！");
			return;
		}
	}
	
    $.ajax({
        dataType: 'json',
        type: "post",
        url: "/r/fininstat/incomethird",
        data: {
        	"pageNo":CurPage,
			"handicap":handicap,
			"level":level,
			"account":account,
			"thirdaccount":thirdaccount,
			"startAndEndTimeToArray":startAndEndTimeToArray,
			"toaccount":toaccount,
			"startamount":startamount,
			"endamount":endamount,
			"type":"thirdin",
			"pageSize":$.session.get('initPageSize')
        },
        success: function (jsonObject) {
                if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.BizIncomelist.length > 0){
                	var tr = '';
					 // 小计
					var counts = 0;
					var amounts=0;
					var idList=new Array;
                    for(var index in jsonObject.data.BizIncomelist){
						 var val = jsonObject.data.BizIncomelist[index];
	                     tr += '<tr>'
	                        	    +'<td>' + val.handicapName + '</td>'
	                        	    +'<td>'+  val.levelName +'</td>'
		                        	+'<td>' + val.memberUserName + '</td>'
		                        	+'<td>' + val.orderNo + '</td>'
		                        	+'<td>' + val.toAccountBank + '</td>'
		                        	+"<td>" 
			    						+"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+val.toId+"' data-placement='auto right' data-trigger='hover'  >"
			    							+val.toAccount
			    						+"</a>" 
		    					    +"</td>"
		                        	+'<td>' + val.amount +'</td>'
		                        	+'<td>' + val.remark +'</td>'
		                        	+'<td>' + ""+'</td>'
	                       	 +'</tr>';
	                       counts +=1;
	                       amounts+=val.amount;
	                       idList.push({'id':val.toId,'type':'ali_wechat'});
                   };
                   $('#third_matched_tbody').empty().html(tr);
                    var trs =
                        '<tr>' +
                        '<td colspan="6">小计：' + counts+ '条记录</td>' +
                        '<td bgcolor="#579EC8" style="color:white;">' + amounts.toFixed(2) + '</td>' +
                        '<td colspan="2"></td>';
                    $('#third_matched_tbody').append(trs);
                    
                     var trn= '<tr><td colspan="6">总计：' + jsonObject.data.IncomeThirdPage.totalElements + '条记录</td>' +
                        '<td bgcolor="#D6487E" style="color:white;">' + jsonObject.data.IncomeThirdtotal[0]+ '</td>' +
                        '<td colspan="2"></td>' +
                        '</tr>';
                    $('#third_matched_tbody').append(trn);
                }
                else {
                	$('#third_matched_tbody').empty().html('<tr></tr>');
                }
                loadHover_accountInfoHover(idList);
              //分页初始化
			 showPading(jsonObject.data.IncomeThirdPage,"thirdmatchPage",_searchByFilter);
        }
    });
}

/**
 * 重置
 * 
 * @private
 */
function _resetValueS() {
	$("#member_matched").val("");
	$("#thirdName_matched").val("");
	$("#toAccount_matched").val("");
	$("#fromMoney_matched").val("");
	$("#toMoney_matched").val("");
	$("#startAndEndTime").val("");
	_searchByFilter();
}


jQuery(function($) {
	// 初始化盘口
	inithancipad();
	// 初始化时间
	initTimePickerHandicap();
	_searchByFilter();
});
