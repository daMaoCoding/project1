var authRequest = {"pageNo": 0,"pageSize":$.session.get('initPageSize')};
currentPageLocation = window.location.href;
function showContent(data) {
    var content = "";
    var rowspan1 = 0,rowspan2=0, rowspan3=0,rowspan4=0;
    var tr1='',tr2='',tr3='',tr4='';
    for (var o in data) {

        if (data[o].code == '100') {
            rowspan1 += 1;
            tr1 += '<td style="text-align: center;">'+ data[o].name+ '</td>'+
                '<td style="text-align: center;">'+ data[o].total+ '</td>'+
                '<td style="text-align: center;">'+  data[o].online + '</td>'+
                '<td style="width: 21%" class="center">'+
                '<a href="#/SystemUserDetail:*?categoryCode='+data[o].code+'" target="_self"><button onclick="setLocalStorage();" class="btn btn-xs btn-white btn-default btn-bold"><i class="ace-icon fa fa-list bigger-120 red">查看详情</i></button></a>'+
                '</td>'+
                '</tr>';
        }
        if (data[o].code == '200') {
            rowspan2 += 1;
            tr2 += '<td style="text-align: center;">'+ data[o].name+ '</td>'+
                '<td style="text-align: center;">'+ data[o].total+ '</td>'+
                '<td style="text-align: center;">'+  data[o].online + '</td>'+
                '<td style="width: 21%" class="center">'+
                '<a href="#/SystemUserDetail:*?categoryCode='+data[o].code+'" target="_self"><button onclick="setLocalStorage();" class="btn btn-xs btn-white btn-default btn-bold"><i class="ace-icon fa fa-list bigger-120 red">查看详情</i></button></a>'+
                '</td>'+
                '</tr>';
        }
        if (data[o].code == '300') {
            rowspan3 += 1;
            tr3 += '<td style="text-align: center;">'+ data[o].name+ '</td>'+
                '<td style="text-align: center;">'+ data[o].total+ '</td>'+
                '<td style="text-align: center;">'+  data[o].online + '</td>'+
                '<td style="width: 21%" class="center">'+
                '<a href="#/SystemUserDetail:*?categoryCode='+data[o].code+'" target="_self"><button onclick="setLocalStorage();" class="btn btn-xs btn-white btn-default btn-bold"><i class="ace-icon fa fa-list bigger-120 red">查看详情</i></button></a>'+
                '</td>'+
                '</tr>';
        }
        if (parseInt(data[o].code.toString())>=400) {
            rowspan4 += 1;
            tr4 += '<td style="text-align: center;">'+ data[o].name+ '</td>'+
                '<td style="text-align: center;">'+ data[o].total+ '</td>'+
                '<td style="text-align: center;">'+  data[o].online + '</td>'+
                '<td style="width: 21%" class="center">'+
                '<a href="#/SystemUserDetail:*?categoryCode='+data[o].code+'" target="_self"><button onclick="setLocalStorage();" class="btn btn-xs btn-white btn-default btn-bold"><i class="ace-icon fa fa-list bigger-120 red">查看详情</i></button></a>'+
                '</td>'+
                '</tr>';
        }
    }
    if (rowspan1>0){
        content +='<tr><td style="text-align: center;vertical-align:middle;font-weight: 800;" rowspan="'+rowspan1+'">出款人员</td>'+tr1;
    }
    if (rowspan2>0){
        content +='<tr><td style="text-align: center;vertical-align:middle;font-weight: 800" rowspan="'+rowspan2+'">入款人员</td>'+tr2;
    }
    if (rowspan3>0){
        content +='<tr><td style="text-align: center;vertical-align:middle;font-weight: 800" rowspan="'+rowspan3+'">财务人员</td>'+tr3;
    }
    if (rowspan4>0){
        content +='<tr><td style="text-align: center;vertical-align:middle;font-weight: 800" rowspan="'+rowspan4+'">主管|客服|技术|下发人员</td>'+tr4;
    }

    $("#table_body").empty().html(content);
}

function refreshContent() {
    authRequest.userNameLike = $("input[name='userNameLike']").val();
    $.ajax({type: "get", url: "/r/user/findUserCategoryInfo", data: authRequest, dataType: 'json', success: function (res) {
        if (res.status == 1) {
            showContent(res.data);
        } else {
            showMessageForFail(res.message);
        }
     },initPage:function(his){
        $('#userFilter').find('input[name=userNameLike]').val(his.userNameLike);
    }});
}
var setLocalStorage = function () {
    window.sessionStorage.removeItem("userNameLikeForStorage");
    if (window.sessionStorage && $('input[name="userNameLike"]').val()){
        window.sessionStorage.setItem("userNameLikeForStorage",$('input[name="userNameLike"]').val());
    }
}
bootbox.setLocale("zh_CN");
(function () {
//    if (window.sessionStorage.getItem("userNameLikeForStorage")){
//        $('input[name="userNameLike"]').val(window.sessionStorage.getItem("userNameLikeForStorage"));
//    }
    $('input[name="userNameLike"]').bind('keydown',function(event){
        if(event.keyCode == "13") {
            refreshContent();
        }
    });
})();
refreshContent();