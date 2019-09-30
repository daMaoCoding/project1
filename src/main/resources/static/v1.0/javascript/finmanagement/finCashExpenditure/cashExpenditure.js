function _beforeAddCashExpenditure() {
    var handicapOpt = '<option selected="selected" value="请选择">请选择</option>'+
        '<option value="" >699</option>'+
        '<option value="" >彩33</option>'+
        '<option value="" >123</option>'+
        '<option value="" >ysc</option>'+
        '<option value="" >人人彩</option>'+
        '<option value="" >百家乐</option>';
    $('#cashExpendditureHandicap').html(handicapOpt);
    $('#cashExpendditureThirdAccount').val('');
    $('#cashExpendditureAmount').val('');
    $('#cashExpendditureThirdOwner').val('');
    var accoutOpt = '<option selected="selected" value="请选择">请选择</option>'+
        '<option value="" >农行 62120**20156</option>'+
        '<option value="" >建行 62170**85624</option>'+
        '<option value="" >工商 62270**85624</option>'+
        '<option value="" >中信 62370**85624</option>'+
        '<option value="" >中行 62570**85624</option>'+
        '<option value="" >兴业 62770**85624</option>'
    $('#cashExpendditureAccount').html(accoutOpt);
    $('#cashExpendditureFee').val('');
    $('#cashExpendditureDesc').val('');
    $('#addCashExpenceModal').modal('show');
}
function _confirmCashExpenditure() {
    $('#promptCash').text('').hide();
    var cashExpendditureHandicap='';
    if ($('#cashExpendditureHandicap').val()=='请选择'){
        $('#promptCash').text('请选择盘口').show(10).delay(1500).hide(10);
        return;
    }else{
        cashExpendditureHandicap = $('#cashExpendditureHandicap option:selected').text();
    }
    var cashExpendditureThirdAccount='';
    if (!$('#cashExpendditureThirdAccount').val()){
        $('#promptCash').text('请填写收款第三方账号').show(10).delay(1500).hide(10);
        return;
    }else{
        cashExpendditureThirdAccount = $('#cashExpendditureThirdAccount').val();
    }
    var cashExpendditureAmount='';
    if (!$('#cashExpendditureAmount').val()){
        $('#promptCash').text('请填写金额').show(10).delay(1500).hide(10);
        return;
    }else{
        cashExpendditureAmount =$('#cashExpendditureAmount').val();
    }
    var cashExpendditureThirdOwner='';
    if (!$('#cashExpendditureThirdOwner').val()){
        $('#promptCash').text('请填写收款第三方开户人').show(10).delay(1500).hide(10);
        return;
    }else {
        cashExpendditureThirdOwner =$('#cashExpendditureThirdOwner').val();
    }
    var cashExpendditureAccount='';
    if ($('#cashExpendditureAccount').val()=='请选择'){
        $('#promptCash').text('请选择出款账号').show(10).delay(1500).hide(10);
        return;
    }else {
        cashExpendditureAccount = $('#cashExpendditureAccount option:selected').text();
    }
    var cashExpendditureFee='';
    if (!$('#cashExpendditureFee').val()){
        $('#promptCash').text('请填写手续费').show(10).delay(1500).hide(10);
        return;
    }else {
        cashExpendditureFee = $('#cashExpendditureFee').val();
    }
    var cashExpendditureDesc='';
    if (!$('#cashExpendditureDesc').val()){
        $('#promptCash').text('请填写本次出现金说明').show(10).delay(1500).hide(10);
        return;
    }else{
        cashExpendditureDesc =$('#cashExpendditureDesc').val();
    }
    var addTr ='<tr><td>'+cashExpendditureHandicap+'</td><td>320180228806968</td><td>'+cashExpendditureAmount+'<i class="fa fa-copy orange bigger-110 clipboardbtn " style="cursor:pointer" data-clipboard-text="'+cashExpendditureAccount+'"></i></td><td>'+cashExpendditureFee+'<i class="fa fa-copy orange bigger-110 clipboardbtn " style="cursor:pointer" data-clipboard-text="'+cashExpendditureFee+'"></i></td><td>andy</td><td>'+cashExpendditureAccount+'<i class="fa fa-copy orange bigger-110 clipboardbtn " style="cursor:pointer" data-clipboard-text="'+cashExpendditureAccount+'"></i></td><td>'+cashExpendditureThirdAccount+'<i class="fa fa-copy orange bigger-110 clipboardbtn " style="cursor:pointer" data-clipboard-text="'+cashExpendditureThirdAccount+'"></i></td><td>'+cashExpendditureThirdOwner+'<i class="fa fa-copy orange bigger-110 clipboardbtn " style="cursor:pointer" data-clipboard-text="'+cashExpendditureThirdOwner+'"></i></td><td>未出款</td><td>2018-02-28 16:40:12</td><td></td><td>2018-02-28 16：16：40：12 andy<br> 新增出现金'+cashExpendditureDesc+'</td><td><button onmouseup="_lockClick(this);" id="#cancelBtn">锁定</button></td><td><button>确定出款</button><button>撤销</button><button>备注</button></td></tr>';
    var oldBody = $('#tbody_cashExpence').html();
    $('#tbody_cashExpence').html(addTr).append(oldBody);
    $('#addCashExpenceModal').modal('hide');
}
function _lockClick(obj) {
    if ($(obj).text()=='锁定') {
        $(obj).text('解锁');
        $(obj).parent().parent().find('#cancelBtn').hide();
        $(obj).parent().parent().find('.clipboardbtn ').show();
    }
    else{
        $(obj).text('锁定');
        $(obj).parent().parent().find('#cancelBtn').show();
        $(obj).parent().parent().find('.clipboardbtn ').hide();
    }
}
$('.clipboardbtn ').hide();