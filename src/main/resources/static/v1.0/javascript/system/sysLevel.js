currentPageLocation = window.location.href;
function _refreshPage() {
	$.ajax({type:"get", url: "/r/level/listCurrentSystemLevel",dataType: "json", success: function (res) {
		if (res && res.status == 1) {
			var arr = new Array();
			for(var index in res.data){
				if(res.data[index].value!=4){
					//屏蔽中层
					arr.push('<tr><td class="center">'+res.data[index].name+'</td><td class="center"><div class="hidden-sm hidden-xs btn-group"><button class="btn btn-xs btn-purple" onclick="_bindSystemLevel('+res.data[index].value+',\''+res.data[index].name+'\');">绑定平台层级</i></button></div></td></tr>');
				}
			}
			$('#dynamic-table tbody').html(arr.join(''));
		}
	}});
}

function  _refreshPage4PlatLevel(CurPage){
   var handicapId= $('#platLevelBind #handicapGroup select[name=search_EQ_handicapId]').val();
   var innerOrOuterFlag = $("#platLevelBind input[name=currentSystemLevelValue]").val();
	if(!!!CurPage&&CurPage!=0) CurPage=$("#platLevelBindPage .Current_Page").text();
    $.ajax({
    		type:"get", 
    		url: "/r/level/findPage",
    		data:{pageNo:CurPage<=0?0:CurPage-1,
        	pageSize:$.session.get('initPageSize'),
        	handicapId:handicapId,type:innerOrOuterFlag
        },
        dataType: "json",
        success: function (res) {
	    	if (res && res.status == 1) {
	            var arr = new Array();
	            for(var index in res.data){
	                arr.push('<tr><td class="center"><input  name="levelIdCheckBox" type="checkbox" value="'+res.data[index].id+'"  oposValue="'+(res.data[index].currSysLevel?res.data[index].currSysLevel:'')+'" onclick="_clickCheckboxOnTd()"/></td><td class="center">'+res.data[index].currSysLevelName+'</td><td class="center">'+res.data[index].handicapName+'</td><td class="center">'+res.data[index].name+'</td><td class="center">'+(res.data[index].status==0?"停用":res.data[index].status==1?"启用":"")+'</td></tr>');
	            }
	            $('#platLevelBind table tbody').html(arr.join(''));
	            $("#platLevelBind td").css("padding","2px")
	        }
	        showPading(res.page,"platLevelBindPage",_refreshPage4PlatLevel,null,false,false);
	    	$('#platLevelBindPage').find('span.showTotal').show();
        }
    });
}

function _bindSystemLevel(val,name) {
    $("#platLevelBind input[name=currentSystemLevelValue]").val(val);
	$('.modal-header .hText').html('<span class="red" style="font-weight: bold">（'+name+'）</span>&nbsp;&nbsp;绑定平台层级');
	var handicapGroup = new Array();
	handicapGroup.push('<span class="label label-lg label-purple arrowed-right">盘口&nbsp;&nbsp;</span>');
	handicapGroup.push('<select class="chosen-select width150" name="search_EQ_handicapId">');
	handicapGroup.push('	<option value="" selected="selected" handicapcode="">全部</option>');
	$.each(handicap_list,function(i,temp){
		handicapGroup.push('<option value="'+temp.id+'" handicapcode="'+temp.code+'">'+temp.name+'</option>');
	});
	handicapGroup.push('</select>');
	$('#platLevelBind #handicapGroup').html(handicapGroup.join(''));
    _refreshPage4PlatLevel(0);
	$('#platLevelBind').modal('toggle');
}

function _clickCheckboxOnTd(){
   var checked =  $('#platLevelBind input[name=levelIdCheckBox]:checked').length == $('#platLevelBind input[name=levelIdCheckBox]').length;
   $('#platLevelBind input[name=levelIdCheckBoxHead]').prop('checked',checked);
}

function _clickCheckboxOnHead(obj){
    $('#platLevelBind input[name=levelIdCheckBox]').removeAttr('checked').prop('checked',$(obj).prop('checked'));
}

function _saveBindRelation(){
   var currentSystemLevelValue = $("#platLevelBind input[name=currentSystemLevelValue]").val();
    var array = new Array();
    $('#platLevelBind input[name=levelIdCheckBox]:checked').each(function(){
        var t = $(this);
        var levelId = t.val();
        var sysLevel = currentSystemLevelValue;
        array.push(levelId+':'+sysLevel);
    });
    $.ajax({type:"get", url: "/r/level/saveBindRelation",data:{bindRelation:array.toString()},dataType: "json", success: function (res) {
        if (res && res.status == 1) {
			_refreshPage4PlatLevel();
            bootbox.alert("保存成功.");
        }else{
            bootbox.alert(res.message);
        }
    }});
}
/**解除绑定*/
function _unbindInnerOrOuterLevel() {
    var type = $("#platLevelBind input[name=currentSystemLevelValue]").val();//1 外层 2 内层 8指定层
    var levelIdArray = [];
    $.each($('#platLevelBind').find('#list').find('input[name="levelIdCheckBox"]:checked'),function (i,obj) {
        levelIdArray.push($(obj).val());
    });
    if (levelIdArray.length==0){
        bootbox.setDefaults("locale","zh_CN");
        bootbox.alert('请选择解绑的层级!',function (res) {
           if(res){

           }else{
               setTimeout(function () {
                   $('body').addClass('modal-open');
               }, 500);
           }
        });
        return;
    }
    $.ajax({
        url:'/r/level/unbindInnerOrOuterLevel',
        type:'put',
        dataType:'json',
        data:{'type':type,"levelIds":levelIdArray.toString()},
        success:function (res) {
            if (res){
                $.gritter.add({
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    time: 2000,
                    speed: 500,
                    position: 'bottom-right',
                    class_name: 'gritter-center',
                });
                if (res.status==1){
                    _refreshPage4PlatLevel(1);//1 任意传的
                }
            }
        }
    });

}
bootbox.setLocale("zh_CN");
_refreshPage();
