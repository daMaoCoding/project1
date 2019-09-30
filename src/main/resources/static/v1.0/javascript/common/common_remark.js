//备注增删改查公用JS


/**
 * 返利网新增备注，例：param={businessId:151,type:'phone',remarkTitle:'返利网账号：ylw',search_contentRight:'IncomeAccountComp:search:*'}，param里面的参数：
 * businessId：备注对应其它表的ID 必填
 * type：备注对应哪个表 必填 （自定义，如："phone"，则读取，新增的 都会是此类型的备注）
 * remarkTitle：备注标题，选填
 * search_contentRight：备注查询权限，选填
 * add_contentRight：备注新增权限，选填
 * delete_contentRight：备注删除权限，选填
 * fnName：完成操作后是否需要执行刷新页面事件 选填（如  fnName:showAccountList   值不可以加引号）
 */
var showModalInfo_remark=function(param){
	if(!param||!param.businessId||!param.type){
		return;
	}
	var html='<div class="modal fade" aria-hidden="false"  id="div_ModalInfo_remark">\
		<div class="modal-dialog modal-lg"  style="width:780px;">\
			<div class="modal-content">\
				<div class="modal-header no-padding text-center">\
					<div class="table-header">\
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button>备注\
					</div>\
				</div>\
				<div class="modal-body no-padding-bottom">\
					<input id="param_businessId" type="hidden"/>\
					<input id="param_type" type="hidden"/>\
					<input id="param_delete_contentRight" type="hidden"/>\
					<h5 class="header smaller center lighter blue no-padding no-margin-top" id="remarkTitle" style="display:none;"></h5>\
					<div id="div_add_remarkList">\
						<h6 class="header smaller center lighter blue no-padding no-margin-top">\
							添加新备注<span class="red">（最多输入120个字）</span>\
							<label class="pull-right inline">\
								<button class="btn btn-xs btn-white btn-info btn-bold" onclick="doAddRemarkHistory();">\
									<i class="ace-icon fa fa-plus bigger-100 green"></i>新增\
								</button>\
							</label><label class="pull-right inline">&nbsp;&nbsp;</label>\
							<label onclick="reset(\'div_add_remarkList\');" class="pull-right inline">\
								<button class="btn btn-xs btn-white btn-info btn-bold">\
									<i class="ace-icon fa fa-refresh bigger-100 green"></i>清空\
								</button>\
							</label>\
						</h6>\
						<div><input class="input-sm" style="width:750px;" name="remark" type="text" placeholder="5~120个字"></div><br/>\
					</div>\
					<div id="div_search_remarkList">\
						<h6 class="header smaller center lighter blue no-padding no-margin-top">备注历史</span></h6>\
						<table class="table table-bordered" style="width:750px;">\
							<thead>\
								<tr>\
									<th style="width:160px;">创建时间</th>\
									<th style="width:110px;">操作人</th>\
									<th style="width:400px;">备注内容</th>\
									<th style="width:80px;">操作</th>\
								</tr>\
							</thead>\
							<tbody></tbody>\
						</table>\
						<div id="showModalInfo_remark_page"></div>\
					</div>\
					<br/>\
				</div>\
			</div>\
		</div>\
	</div>';
	var $div=$(html).appendTo($("body"));
	$div.find("#param_businessId").val(param.businessId);
	$div.find("#param_type").val(param.type);
	if(param.remarkTitle){//标题
		$div.find("#remarkTitle").show().html(param.remarkTitle);
	}
	if(param.add_contentRight){//新增权限
		$div.find("#div_add_remarkList").addClass("contentRight").attr("contentRight",param.add_contentRight);
	}
	if(param.delete_contentRight){//删除权限
		$div.find("#param_delete_contentRight").val(param.delete_contentRight);
	}
	if(param.search_contentRight){//查询权限
		$div.find("#div_search_remarkList").addClass("contentRight").attr("contentRight",param.search_contentRight);
		//加载数据列表
	}
	searchModalInfo_remark(0);
	
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		// 关闭窗口清除model
		if(param.fnName){
			param.fnName();
		}
		$div.remove();
	});
	contentRight();
}
/**
 * 新增备注
 */
var doAddRemarkHistory=function(){
	var $div=$("#div_ModalInfo_remark");
	var $remark=$div.find("[name=remark]");
	var validatePrint=[{ele:$remark,name:'备注',minLength:5,maxLength:120}];
	 if(!validateEmptyBatch(validatePrint)||!validateInput(validatePrint)){
	        setTimeout(function(){       
	            $('body').addClass('modal-open');
	        },500);
    	return;
    }
	bootbox.confirm("确定新增 ?", function(result) {
		if (result) {
			$.ajax({
		        contentType: 'application/json;charset=UTF-8',
				type:"POST",
				dataType:'JSON',
				url:'/r/commonRemark/add',
				async:false,
		        data:JSON.stringify({
		    		"businessId":$div.find("#param_businessId").val(),
					"remark":$.trim($remark.val()),
					"type":$div.find("#param_type").val()
				}),
				success:function(jsonObject){
			        if(jsonObject.status == 1){
			        	//操作成功提示
			        	showMessageForSuccess("新增成功！");
			        	reset('div_add_remarkList');//清空备注
			            searchModalInfo_remark();//刷新数据列表
			        }else{
			        	showMessageForFail("新增失败："+jsonObject.message);
			        }
			    }
			});
		}
        setTimeout(function(){       
            $('body').addClass('modal-open');
        },500);
	});
}
/**
 * 删除备注
 */
var doDeleteRemarkHistory=function(id){
	if(!id){
		return;
	}
	bootbox.confirm("确定删除 ?", function(result) {
		if (result) {
			$.ajax({
				type:"GET",
				dataType:'JSON',
				url:'/r/commonRemark/delete?id='+id,
				async:false,
				success:function(jsonObject){
					if(jsonObject.status == 1){
						showMessageForSuccess("删除成功！");
						searchModalInfo_remark();//刷新数据列表
					}else{
						showMessageForFail("删除失败："+jsonObject.message);
					}
				}
			});
		}
		setTimeout(function(){       
			$('body').addClass('modal-open');
		},500);
	});
}
/**
 * 分页查询备注信息
 */
var searchModalInfo_remark=function(CurPage){
	var $div=$("#div_ModalInfo_remark");
	if(!!!CurPage&&CurPage!=0) CurPage=$("#showModalInfo_remark_page .Current_Page").text();
	$.ajax({
		type:"GET",
		dataType:'JSON',
		url:'/r/commonRemark/list?businessId='+$div.find("#param_businessId").val()+'&type='+$div.find("#param_type").val()+'&pageNo='+(CurPage<=0?0:CurPage-1)+'&pageSize='+($.session.get('initPageSize')?$.session.get('initPageSize'):10),
		async:false,
		success:function(jsonObject){
	        if(jsonObject.status == 1){
	        	var curr_uid=getCookie('JUID');
	        	var tbody="";
	        	$.each(jsonObject.data,function(index,record){
	        		var tr="";
					tr+="<td><span>"+(record.createTime?record.createTime.substring(0,19):"")+"</span></td>";
					tr+="<td><span>"+_checkObj(record.createUid)+"</span></td>";
					tr+="<td style='text-align:left !important;padding: 5px !important;;'>"+_checkObj(record.remark)+"</td>";
					if(record.createUid==curr_uid){
						tr+="<td><button class='btn btn-xs btn-danger btn-info  contentRight' " +
									"onclick='doDeleteRemarkHistory("+record.id+")' contentRight='"+($div.find("#param_delete_contentRight").val()||"")+"'>" +
									"<i class='ace-icon glyphicon glyphicon-trash bigger-100'></i>" +
									"<span>删除</span>" +
								"</button></td>";
					}else{
						//当前登录人不是创建人时，不允许删除备注
						tr+="<td><button disabled class='btn btn-xs btn btn-grey  contentRight'  contentRight='"+($div.find("#param_delete_contentRight").val()||"")+"'>" +
									"<i class='ace-icon glyphicon glyphicon-trash bigger-100'></i>" +
									"<span>删除</span>" +
								"</button></td>";
					}
					tbody+="<tr id='tr"+record.id+"'>"+tr+"</tr>";
	        	});
	        	$div.find("tbody").html(tbody);
				//分页初始化
				showPading(jsonObject.page,"showModalInfo_remark_page",searchModalInfo_remark,null,true);
				contentRight();
	        }else{
	        	showMessageForFail("读取失败："+jsonObject.message);
	        }
	    }
	});
}


