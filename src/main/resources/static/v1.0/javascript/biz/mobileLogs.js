var $logsList=$("#logsList");
var logListParent = "";
var loadLogList = function(path){
	$.ajax({dataType:'json',type:"get",url:"/api/file/fileList", async:false, data:{path:path},success:function(jsonObject){
			if(jsonObject.status!=1)
				return;
			var divs='';
		    logListParent = jsonObject.data.parent;
		    var dataList = jsonObject.data.dataList;
			$.each(dataList?dataList:[],function(index,record){
				   divs+='<div title="'+record.name+'" class="btn btn-app btn-lg  '+(record.subCount>0?'btn-primary':'')+'"  >';
                   if(record.subCount>0){
                       divs+=' <i class="ace-icon fa '+(record.file?' fa-file ':' fa-folder-open-o ')+' bigger-130" onclick="loadLogNextList('+record.subCount+',\''+record.next+'\')"></i>';
                   }else{
                       divs+=' <i class="ace-icon fa '+(record.file?' fa-file ':' fa-folder-open-o ')+' bigger-130"></i>';
                   }
				   divs+=' <small>'+((record.name.length>12)?('...'+record.name.slice(record.name.length-8)):record.name)+'</small><span class="badge badge-pink">'+(record.subCount?record.subCount:'')+'</span>';
                   if(record.name!='app'&&record.name!='pc'){
					   divs+=' </br>';
					   if(record.subCount>0){
						   divs+=' <small><a style="text-decoration: none;color: white;font-size:10px;" onclick="showDel(\''+record.path+'\')">删除</a><span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span></small>';
					   }else{
						   divs+=' <small><a style="text-decoration: none;color: white;font-size:10px;" onclick="showDel(\''+record.path+'\')">删除</a><span>&nbsp;&nbsp;</span><a href="/api/file/getFile?path='+record.path+'" style="text-decoration: none;color: white;font-size:10px;">下载</a><span></small>';
					   }
				   }
				   divs+='</div>';
			});
			$logsList.html(divs);
		    var nav = '<li><a onclick="loadLogList(\'\')">根目录</a></li>';
		    if(logListParent){
				var paths = logListParent.split('/');
				var div = '';
				$.each(paths,function(index,record){
					div = div + (index == 0 ? record :'/'+record);
					nav = nav + '<li><a onclick="loadLogList(\''+div+'\')">'+record+'</a></li>';
				});
			}
			$('.breadcrumb').html(nav);
	}});
};

var loadLogParentList = function(){
	loadLogList(logListParent);
};

var loadLogNextList  = function(subCount,nextPath){
	if(!subCount||subCount<=0)
		return;
	loadLogList(nextPath);
};

var showDel = function(path){
	bootbox.confirm("确定删除该文件或目录&nbsp;?&nbsp;", function(result) {
		if (result) {
			$.ajax({type: 'get',url: '/api/file/delete',data: {"path":path},dataType: 'json',success: function (res) {
				if (res.status==1) {
					showMessageForSuccess('操作成功');
					loadLogParentList();
				}
			}});
		}
	});
};

loadLogList(null);
