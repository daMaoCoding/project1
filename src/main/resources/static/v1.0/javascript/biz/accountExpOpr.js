currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};

var trHtml='<tr class="noLeftRightPadding">\
                <td>{handicap}</td>\
                <td>{alias}</td>\
				<td><a class="bind_hover_card" data-toggle="accountInfoHover{accountId}" data-placement="auto right" data-trigger="hover" data-original-title="" title=""><span>{account}</span></a></td>\
				<td><span>{clientPosition}</span></td>\
				<td><span>{clientIp}</span></td>\
				<td><span>{content}</span></td>\
				<td><span>{clientTime}</span></td>\
				<td><span>{createTime}</span></td>\
				<td><span title="{remark}">{remarkSlice}</span></td>\
				<td><span>{operator}</span></td>\
                <td>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="operator4ExpOpr({id})"><i class="ace-icon fa fa-user bigger-100 orange"></i><span>操作人</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="remark4ExpOpr({id})"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>备注</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>\
                </td>\
           </tr>';

function showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    if(!data||data.length ==0){
        $tbody.html('');
        return;
    }
    var idList=new Array;
    $.each(data,function(idx, obj) {
        obj.clientTime =timeStamp2yyyyMMddHHmmss(obj.clientTime);
        obj.createTime =timeStamp2yyyyMMddHHmmss(obj.createTime);
        obj.remarkSlice = obj.remark ?obj.remark.substring(0,6)+'...':'';
        idList.push({'id':obj.accountId});
    });
    $tbody.html(fillDataToModel4Array(data,trHtml));
    loadHover_accountInfoHover(idList);
    showSubAndTotalStatistics4Table($tbody,{column:11, subCount:data.length,count:page.totalElements});

}

function refreshContent(pageNo){
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accountPage .Current_Page").text()?$("#accountPage .Current_Page").text()-1:0);
    authRequest.pageNo = authRequest.pageNo<0?0:authRequest.pageNo;
    authRequest.search_LIKE_operator = $("input[name='search_LIKE_operator']").val();
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.handiArray = $("select[name='search_EQ_handicapId']").val();
    if(! authRequest.handiArray){
        var search_EQ_handicapId = [];
        $('select[name="search_EQ_handicapId"]').find('option:not(:first-child)').each(function () {
            search_EQ_handicapId.push($(this).val());
        });
        authRequest.handiArray = search_EQ_handicapId.toString();
    }else{
        authRequest.handiArray = [authRequest.handiArray].toString();
    }
    authRequest.oprLike = $("input[name='search_LIKE_operator']").val();
    authRequest.alias = $("input[name='search_EQ_alias']").val();
    authRequest.accLike = $("input[name='search_LIKE_account']").val();
    authRequest.bankType=$.trim($("select[name='search_EQ_bankType']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    var startAndEndTimeToArray = new Array();
    var startAndEndTime = $("input[name='startAndEndTime']").val();
    if(startAndEndTime){
        var startAndEnd = startAndEndTime.split(" - ");
        startAndEndTimeToArray.push($.trim(startAndEnd[0]));
        startAndEndTimeToArray.push($.trim(startAndEnd[1]));
    }
    authRequest.startAndEndTimeToArray = startAndEndTimeToArray.toString();
    $.ajax({ dataType:'json', type:"get",url:'/r/accountExpOpr/list',data:authRequest, success:function(jsonObject){
        if(jsonObject.status == 1){
            showContent(jsonObject.data,jsonObject.page);
            showPading(jsonObject.page,"accountPage",refreshContent,null,false,false);
            loadContentRight();
        }else {
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){ bootbox.alert(result);},initPage:function(his){
        var $form = $('#accountFilter');
        $form.find('input[name=search_LIKE_account]').val(his.search_LIKE_account);
        $form.find('input[name=search_LIKE_alias]').val(his.search_LIKE_alias);
        $form.find('input[name=search_LIKE_operator]').val(his.operator);
        $form.find('select[name=search_LIKE_bankType]').val(his.bankType);
    }});
}

function remark4ExpOpr(id) {
    $('#remarkModal4ExpOpr').val('');
    $('#remarkModal4ExpOpr_id').val(id);
    $('#remarkModal4ExpOpr').modal('show');
}

function addRemark4ExpOpr () {
    var id =  $('#remarkModal4ExpOpr_id').val();
    var remark =  $('#remarkModal4ExpOpr_remark').val();
    if(!id||!remark){
        showMessageForFail('信息不全。')
        return;
    }
    $.ajax({ dataType:'json', type:"get",url:'/r/accountExpOpr/addRemark',data:{
        id:id,
        remark:remark
    }, success:function(jsonObject){
        if(jsonObject.status == 1){
            refreshContent();
        }else {
            bootbox.alert(jsonObject.message);
        }
        $('#remarkModal4ExpOpr').modal('hide');
    },error:function(result){ bootbox.alert(result);},initPage:function(his){
    }});
}

function operator4ExpOpr(id) {
    $('#operatorModal4ExpOpr_operator').val('');
    $('#operatorModal4ExpOpr_id').val(id);
    $('#operatorModal4ExpOpr').modal('show');
}

function addOperator4ExpOpr () {
    var id =  $('#operatorModal4ExpOpr_id').val();
    var operator =  $('#operatorModal4ExpOpr_operator').val();
    if(!id||!operator){
        showMessageForFail('信息不全。')
        return;
    }
    $.ajax({ dataType:'json', type:"get",url:'/r/accountExpOpr/addOperator',data:{
        id:id,
        operator:operator
    }, success:function(jsonObject){
        if(jsonObject.status == 1){
            refreshContent();
        }else {
            bootbox.alert(jsonObject.message);
        }
        $('#operatorModal4ExpOpr').modal('hide');
    },error:function(result){ bootbox.alert(result);},initPage:function(his){
    }});
}

function loadContentRight(){
    contentRight({'OutwardAccountBankUsed:CheckTransferIn:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }, 'OutwardAccountBankUsed:CheckTransferOut:*':function($currObj,hasRight){
        !hasRight?$currObj.removeAttr("href").css('cursor','not-allowed'):null;
    }});
}

function searchByFilter(){
    authRequest.operator = $("input[name='search_LIKE_operator']").val();
    authRequest.search_LIKE_alias = $("input[name='search_LIKE_alias']").val();
    authRequest.search_LIKE_account = $("input[name='search_LIKE_account']").val();
    authRequest.bankType=$.trim($("select[name='search_LIKE_bankType']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    refreshContent(0);
}
bootbox.setLocale("zh_CN");

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});
var showModal_outwardInfo=function(){
    var $div=$("#outwardInfoModal").modal("toggle");
    //重置参数
    resetOutwardInfo();
}
initTimePicker(true,$("[name=startAndEndTime]"),7);
contentRight();

initRefreshSelect($("#refreshAccountExpOprSelect"),$("#searchAccountExpOprBtn"),null,"refresh_account_expOpr");

getHandicap_select($("select[name='search_EQ_handicapId']"),null,"全部");
getBankTyp_select($("select[name='search_EQ_bankType']"),null,"全部");

refreshContent(0);