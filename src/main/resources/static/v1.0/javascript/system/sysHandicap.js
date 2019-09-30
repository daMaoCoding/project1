currentPageLocation = window.location.href;

function _refreshPage() {
    $.ajax({
        type: "get", url: "/r/handicap/list", dataType: "json", success: function (res) {
            if (res && res.status == 1) {
                var arr = new Array();
                for (var index in res.data) {
                    arr.push('<tr><td class="center" style="width:49.5%;">' + res.data[index].name + '</td><td class="center" style="width:49.5%;"><div class="hidden-sm hidden-xs btn-group"><select class="chosen-select width140" name="search_EQ_handicapId" id="' + res.data[index].id + '" title="' + res.data[index].zone + '">' + getZoneOption(res.data[index].zone) + '</select></div></td></tr>');
                }
                $('#dynamic-table tbody').html(arr.join(''));
            }
        }
    });
}

bootbox.setLocale("zh_CN");
getZoneOption = function (handicapId) {
    var options = "";
    $.each(zone_list_all, function (index, record) {
        if (handicapId && record.id == handicapId) {
            options += "<option selected value=" + record.id + " code=" + record.code + " >" + record.name + "</option>";
        } else {
            options += "<option value=" + record.id + " >" + record.name + "</option>";
        }
    });
    return options;
}
_refreshPage();

/**
 * 刷新盘口的缓存信息
 */
var flushCache = function () {
    $.ajax({
        type: "POST", url: '/r/handicap/flushCache', dataType: 'JSON', success: function (res) {
            if (res.status != 1) {
                return;
            }
            sysSetting = res.data;
        }
    });
};

/**
 * 保存更新盘口所属区域
 */
var saveHandicapZone = function () {
    bootbox.confirm("<span class='red bolder'>确定保存盘口区域信息？", function (result) {
        if (result) {
            var keysArray = new Array()
            $("select[name='search_EQ_handicapId']").each(function (j, item) {
                var index = item.selectedIndex;
                var value = item.options[index].value;
                if (value != item.title) {
                    keysArray.push(item.id + ";" + value);
                }
            });
            $.ajax({
                type: "PUT",
                dataType: 'JSON',
                url: '/r/handicap/updateZone',
                async: false,
                data: {
                    "keysArray": keysArray.toString()
                },
                success: function (jsonObject) {
                    if (jsonObject && jsonObject.status == 1) {
                        showMessageForSuccess("保存成功");
                        _refreshPage();
                        flushCache();
                    } else {
                        showMessageForFail("保存失败" + jsonObject.message);
                    }
                }
            });
        }
        setTimeout(function () {
            $('body').addClass('modal-open');
        }, 500);
    });
}