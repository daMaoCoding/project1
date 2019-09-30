jQuery(function ($) {
    $('[data-rel=tooltip]').tooltip();
    $('.select2').css('width', '200px').select2({allowClear: true})
        .on('change', function () {
            $(this).closest('form').validate().element($(this));
        });
    $('#fuelux-wizard-container')
        .ace_wizard({
            //step: 2 //optional argument. wizard will jump to step "2" at first
            //buttons: '.wizard-actions:eq(0)'
        })
        .on('actionclicked.fu.wizard', function (e, info) {
            if (info.step == 2) {
                if (checkFileType()) {
                    importFile();
                } else {
                    e.preventDefault();//this will prevent clicking and selecting steps
                }
            }
            if (info.step == 3) {
                window.location.reload();
            }
        }).on('stepclick.fu.wizard', function (e) {
        //e.preventDefault();//this will prevent clicking and selecting steps
    });
}, "");
$('#modal-wizard-container').ace_wizard();
$('#modal-wizard .wizard-actions .btn[data-dismiss=modal]').removeAttr('disabled');

/**
 * 导出Excel模板
 */
var exceportTemplate = function () {
    window.location.href = '/r/importbanklog/export';
}

/**
 * 校验选择文件格式是否正确，正确返回true，否则为false
 */
var checkFileType = function () {
    var uploadfile = $("#uploadfile").val();
    $("#selectfile").val(uploadfile);
    if (uploadfile == '' || uploadfile == null) {
        $("#checkinfo").html("请选择要上传的文件！");
        return false;
    } else {
        var index = uploadfile.lastIndexOf(".");
        if (index < 0) {
            $("#checkinfo").html("上传的文件格式不正确，请选择Excel文件！");
            return false;
        } else {
            var suffix = uploadfile.substring(index + 1, uploadfile.length);
            var suffixupper = suffix.toUpperCase();
            if (suffixupper != "XLS" && suffixupper != "XLSX") {
                $("#checkinfo").html("上传的文件格式不正确，请选择Excel文件！");
                return false;
            } else {
                $("#checkinfo").html("");
                return true;
            }
        }
    }
}
/**
 * 导入的处理方式
 */
var importFile = function (){
    var $div=$("#importinfo");
    var formData = new FormData();
    var name = $("#uploadfile").val();
    formData.append("file",$("#uploadfile")[0].files[0]);
    formData.append("filename",name);
    $.ajax({
        url : '/r/importbanklog/import',
        type : 'POST',
        async : false,
        data : formData,
        // 告诉jQuery不要去处理发送的数据
        processData : false,
        // 告诉jQuery不要去设置Content-Type请求头
        contentType : false,
        success : function(responseText) {
            var isbanklog = "1";
            var jsonObject = eval('(' + responseText + ')');
            $('.btn-prev').attr('disabled', true);
            if(jsonObject.status !=1){
                showMessageForFail("导入银行流水失败："+jsonObject.message);
                return;
            }
            var tbody="";
            $.each(jsonObject.data,function(index,record){
                var tr="";
                if(typeof(record.fromAccount)=="undefined"){
                    isbanklog = "0";
                    tr+="<td><span>"+record+"</span></td>";//异常的数据封装显示
                }else{
                    tr+="<td><span>"+timeStamp2yyyyMMddHHmmss(record.tradingTime)+"</span></td>";
                    tr+="<td><span>"+record.amount+"</span></td>";
                    tr+="<td><span>"+(record.balance?record.balance:'')+"</span></td>";
                    tr+="<td><span>"+(record.toAccount?record.toAccount:'')+"</span></td>";
                    tr+="<td><span>"+(record.toAccountOwner?record.toAccountOwner:'')+"</span></td>";
                    tr+="<td><span>"+(record.remark?record.remark:'')+"</span></td>";
                }
                tbody+="<tr>"+tr+"</tr>";
            });
            if(isbanklog=="1"){
                $div.find("table#listbanklog").show();
                $div.find("table#listbanklog tbody").html(tbody);
                $div.find("table#listwarn").hide();
            }else{
                $div.find("table#listwarn").show();
                $div.find("table#listwarn tbody").html(tbody);
                $div.find("table#listbanklog").hide();
            }

        }
    });
}

var selectFile = function () {
    $("#uploadfile").trigger("click");
}