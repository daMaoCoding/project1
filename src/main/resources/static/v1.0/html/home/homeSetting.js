//公告设置
var showNoticeList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#noticePage .Current_Page").text();
	var $filter = $("#noticeFilter");
    var data = {
        search_LIKE_title:$filter.find("[name=search_LIKE_title]").val(),
        search_LIKE_contant:$filter.find("[name=search_LIKE_contant]").val(),
        search_LIKE_publishNo:$filter.find("[name=search_LIKE_publishNo]").val(),
        search_IN_type:$filter.find("[name=search_IN_type]:checked").val().toString(),
        search_EQ_status:1,
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize')
    };
    //发送请求
	$.ajax({
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:'/r/notice/list',
        data:data,
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			var $tbody=$("#noticeListTable").find("tbody");
			$tbody.html("");
			var idList=new Array();
			var tr="";
			$.each(jsonObject.data,function(index,record){
				idList.push(record.id);
				tr+="<tr>";
				var typeStr=(record.type==1?"自动出入款系统":(record.type==2?"返利网工具":"PC工具"));
				tr+="<td>"+_checkObj(typeStr)+"</td>";
				tr+="<td>"+_checkObj(record.publishNo)+"</td>";
				tr+="<td>"+getHTMLremark(record.title,95)+"</td>";
				tr+="<td><span style='width:295px; overflow: hidden; display:block;word-break:keep-all;white-space:nowrap;text-overflow:ellipsis;' >"+_checkObj(record.contant)+"</span></td>";
				tr+="<td>"+timeStamp2yyyyMMddHHmmss(record.publishTime)+"</td>";
				tr+="<td>"+timeStamp2yyyyMMddHHmmss(record.updateTime)+"</td>";
				tr+="<td>"+_checkObj(record.operator)+"</td>";
				//操作
				tr+="<td>";
				//修改
				tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' contentright=''\
					onclick='showUpdateNoticeModal("+record.id+")'>\
					<i class='ace-icon fa fa-edit bigger-100 orange'></i><span>修改</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-danger btn-bold contentRight' contentright=''\
					onclick='doDeleteNotice(\""+record.id+"\")'>\
					<i class='ace-icon fa fa-trash-o bigger-100 red'></i><span>删除</span></button>";
				tr+="</td>";
				tr+="</tr>";
			});
			$tbody.html(tr);
			showPading(jsonObject.page,"noticePage",showNoticeList,null,true);
			contentRight();
        }
	});
}

/** 公告新增 */
var showAddModal=function(){
	var $div=$("#addNoticeModal");
	$div.find("table td").css("padding-top","10px");
	//清除之前数据
	reset("addNoticeModal");
	$div.modal("toggle");
	//时间控件
	initTime4Publish($div.find("[name=publishTime]"));
	//富文本编辑框
	$div.find("#contant_head").html("");
	$div.find("#contant").cleanHtml();
	$div.find("#contant").html("");
	loadingEdit($div.find("#contant"));
}
var doAddNotice=function(){
	var $div=$("#addNoticeModal");
	var data={
			"title":$div.find("[name=title]").val().trim(),
			"contant":$div.find("#contant").html(),
			"publishNo":$div.find("[name=publishNo]").val().trim(),
			"publishTime":new Date($div.find("[name=publishTime]").val()),
			"type":$div.find("[name=type]:checked").val()
	}
	if(!$div.find("[name=publishTime]").val()){
		showMessageForFail("新增失败：上线时间不能为空");
		return;
	}
	bootbox.confirm("确定新增?", function(result) {
		if (result) {
			$.ajax({
				type: "POST",
				dataType: 'JSON',
				url: '/r/notice/create',
				async: false,
				data:data,
				success: function (jsonObject) {
					if (jsonObject && jsonObject.status == 1) {
						showNoticeList();
						showMessageForSuccess("新增成功");
						$div.modal("toggle");
					} else {
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

/** 公告修改 */
var showUpdateNoticeModal=function(id){
	$.ajax({
		type: "POST",
		dataType: 'JSON',
		url: '/r/notice/findbyid',
		async: false,
		data:{"id":id},
		success: function (jsonObject) {
			if (jsonObject && jsonObject.status == 1) {
				var $div=$("#updateNoticeModal");
				$div.find("table td").css("padding-top","10px");
				$div.find("#noticeId").val(id);
				var noticeInfo=jsonObject.data;
				$div.find("[name=title]").val(_checkObj(noticeInfo.title));
				$div.find("[name=type][value="+noticeInfo.type+"]").prop("checked",true);
				$div.find("[name=publishNo]").val(_checkObj(noticeInfo.publishNo));
				//时间控件
				initTime4Publish($div.find("[name=publishTime]"),timeStamp2yyyyMMddHHmmss(noticeInfo.publishTime));
				//富文本编辑框
				var $updateHtml=$div.find("#contant");
				$div.find("#contant_head").html("");
				loadingEdit($updateHtml);
				$updateHtml.html(_checkObj(noticeInfo.contant));
				$div.modal("toggle");
			} else {
				showMessageForFail("公告信息获取失败："+jsonObject.message);
			}
		}
	});
}
var doUpdateNotice=function(){
	var $div=$("#updateNoticeModal");
	var data={
			"title":$div.find("[name=title]").val().trim(),
			"contant":$div.find("#contant").html(),
			"publishNo":$div.find("[name=publishNo]").val().trim(),
			"publishTime":new Date($div.find("[name=publishTime]").val()),
			"type":$div.find("[name=type]:checked").val(),
			"id":$div.find("#noticeId").val()
	}
	if(!$div.find("[name=publishTime]").val()){
		showMessageForFail("新增失败：上线时间不能为空");
		return;
	}
	bootbox.confirm("确定修改?", function(result) {
		if (result) {
			$.ajax({
				type: "POST",
				dataType: 'JSON',
				url: '/r/notice/update',
				async: false,
				data:data,
				success: function (jsonObject) {
					if (jsonObject && jsonObject.status == 1) {
						showNoticeList();
						showMessageForSuccess("修改成功");
						$div.modal("toggle");
					} else {
						showMessageForFail("修改失败："+jsonObject.message);
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
 *     初始化 富文本框
 */
var loadingEdit=function($div){
	$div.css({'height': '200px'}).ace_wysiwyg({
	    toolbar_place: function (toolbar) {
	        return $(this).closest('.widget-box')
	            .find('.widget-header').prepend(toolbar)
	            .find('.wysiwyg-toolbar').addClass('inline');
	    },
	    toolbar: [
	        {name: 'bold', title: '加粗', icon: 'ace-icon fa fa-bold'},
//	        'foreColor',
	        null,
	        {name: 'strikethrough', title: '删除线'},
	        null,
	        {name: 'insertunorderedlist', title: '图形标记'},
	        {name: 'insertorderedlist', title: '数字标记'},
	        null,
	        {name: 'justifyleft', title: '左对齐'},
	        {name: 'justifycenter', title: '居中'},
	        {name: 'justifyright', title: '右对齐'}
	    ]
	});
}

/**
 *    初始化 时间控件
 */
var initTime4Publish = function ($div,currTime ) {
	if(!$div){
		//不指定div则默认加载全部时间控件
		$div=$('input.date-range-picker');
	}
    var startTime = $div.daterangepicker({
        autoUpdateInput: false,
        singleDatePicker: true,
        timePicker: true, //显示时间
        timePicker24Hour: true, //24小时制
        timePickerSeconds: true,//显示秒
        startDate:currTime,
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
    }).val(currTime);
    startTime.on('apply.daterangepicker', function (ev, picker) {
        $(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:ss'));
    });
    startTime.on('cancel.daterangepicker', function (ev, picker) {
        $(this).val('');
    });
}

/** 公告删除 */
var doDeleteNotice=function(id){
	bootbox.confirm("确定删除?", function(result) {
		if (result) {
			$.ajax({
				type: "POST",
				dataType: 'JSON',
				url: '/r/notice/delete',
				async: false,
				data:{"id" : id},
				success: function (jsonObject) {
					if (jsonObject && jsonObject.status == 1) {
						showNoticeList();
						showMessageForSuccess("删除成功");
					} else {
						showMessageForFail("删除失败："+jsonObject.message);
					}
				}
			});
		}
	});
}

showNoticeList();

$("#noticeFilter").find("[name=search_IN_type]").click(function(){
	showNoticeList();
});
$("#noticeFilter").keypress(function(e){
	if(event.keyCode == 13) {
		$("#noticeFilter #searchBtn button").click();
	}
});