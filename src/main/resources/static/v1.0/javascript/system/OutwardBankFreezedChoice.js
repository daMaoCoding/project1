var freezedAccount=new Array();
/**
 * 加载选中冻结账号的list
 */
var loadTableList=function(){
	var $tbody=$("#exportFilter").find("tbody");
	$tbody.html("");
	$('#account_multiselect option:selected').each(function () {
		var choiceId=this.value*1;
		$.each(freezedAccount,function(i,record){
			if(record.id*1==choiceId){
				var tr="";
				tr+="<td><span>" +
						"<input name='startAndEndTime' id='datePicker"+record.id+"' class='date-range-picker input-sm'" +
						" type='text' placeholder='请选择起始日期' style='width:285px;'/>" +
					"</span></td>";
				tr+="<td><span>"+record.currSysLevelName+"</span></td>";
				tr+="<td><span>"+record.alias+"</span></td>";
				tr+="<td style='padding-left:0px;padding-right:0px;'>" +
						"<a class='bind_hover_card breakByWord' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"+hideAccountAll(record.account)+
						"</a>" +
					"</td>";
				tr+="<td><span>"+record.owner+"</span></td>";
				tr+="<td><span>"+record.bankType+"</span></td>";
				tr+="<td><button onclick='removeTr(\""+record.id+"\")' class='btn btn-white btn-warning btn-bold btn-sm'>\
							<i class='ace-icon fa fa-trash-o bigger-120'></i>\
							删除\
						</button>\
					</td>";
				$tbody.append($("<tr id='"+record.id+"'>"+tr+"</tr>"));
				loadHover_accountInfoHover([{"id":record.id}]);
				initTimePicker(false,$("#datePicker"+record.id));
			}
		});
	});
}

var do_export=function(){
	var $table=$("#exportFilter table");
	var $trList=$table.find("tbody tr");
	//校验
	if(!$trList||$trList.length==0){
		showMessageForFail("请先选择需要导出的银行卡。");
		return false;
	}else if($trList.length>50){
		showMessageForFail("导出卡数超出限制（50张），当前已选冻结卡 "+$trList.length+" 张。");
		return false;
	}
	var isOk=true,idArray=new Array(),dateArray=new Array();
	$trList.each(function(){
		var accountId=$(this).attr("id");
		var date=$(this).find("[name=startAndEndTime]").val();
		idArray.push(accountId);
		dateArray.push(date);
		if(date.length==0){
			isOk=false;
			showMessageForFail("请选择时间");
			$(this).find("[name=startAndEndTime]").focus();
			return false;
		}
	});
	if(isOk){
		//拼接url
		var url="/r/exportaccount/freezedOutwardLog/"+idArray.toString()+"/"+dateArray.toString();
		$("#exportUrl").attr("href",url).click();
	}
}
/**
 * 删除勾选的当前行
 */
var removeTr=function(recordId){
	//重新加载已选中的值
	var choiceAccount=new Array();
	$('#account_multiselect option:selected').each(function () {
		//重新装载其它值
		if(this.value*1!=recordId*1){
			choiceAccount.push(this.value);
		}
	});
	$('#account_multiselect').val(choiceAccount);
	$('#account_multiselect').multiselect('refresh');
	//删除当前tr行
	$("tr#"+recordId).remove();
}

/**
 * 加载冻结账号下拉框
 */
var loadAccountList=function(){
	var currSysLevel=[];
//    if($("#accountFilter [name=currSysLevel]:checked").length==1){
//    	currSysLevel=$("#accountFilter [name=currSysLevel]:checked").val();
//    }
    $.map($("#accountFilter").find("[name=currSysLevel]:checked"),function(record){
    	currSysLevel.push($(record).val());
    })
	$.ajax({
		dataType:'JSON',
		type:"POST",
		url:"/r/account/searchFreezed",
		data:{
			currSysLevelList:currSysLevel.toString()
		},
		success:function(jsonObject){
			if(jsonObject.status==-1){
				showMessageForFail("账号信息查询失败，请联系技术员");
			}
			freezedAccount=jsonObject.data;
			var options=new Array();
			$.each(freezedAccount,function(i,record){
				options.push("<option value='"+record.id+"'>"+record.alias+"</option>");
			});
			var $account_multiselect=$("#account_multiselect");
			$account_multiselect.multiselect("destroy");
			$account_multiselect.html(options.join('')).multiselect({
				enableFiltering: true,
				enableHTML: true,
				nonSelectedText :'----全部----',
				nSelectedText :'已选中',
				buttonClass: 'btn btn-white btn-primary',
				buttonWidth: '300px',
				templates: {
					button: '<button type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>',
					ul: '<ul class="multiselect-container dropdown-menu"></ul>',
					filter: '<li class="multiselect-item filter"><div class="input-group"><span class="input-group-addon"><i class="fa fa-search"></i></span><input class="form-control multiselect-search" type="text"></div></li>',
					filterClearBtn: '<span class="input-group-btn"><button class="btn btn-default btn-white btn-grey multiselect-clear-filter" type="button"><i class="fa fa-times-circle red2"></i></button></span>',
					li: '<li><a tabindex="0"><label></label></a></li>',
					divider: '<li class="multiselect-item divider"></li>',
					liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
				}
			});

		}
	});
}
var resetPage=function(divName){
	reset(divName);
	loadAccountList();
}
/**
 * 一键刷新table列所有时间控件的值
 */
var updateAllTime=function(){
	var dateAll=$("#startAndEndTimeAll").val();
	if(!dateAll){
		showMessageForFail("请输入时间！");
		return false;
	}
	$("table [name=startAndEndTime]").each(function(){
		$(this).val(dateAll);
	});
}
loadHandicap_Level($("select[name='search_EQ_handicapId']"));
loadAccountList();
initTimePicker();