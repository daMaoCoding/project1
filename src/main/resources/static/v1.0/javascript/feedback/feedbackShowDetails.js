jQuery(function($) {
	queryFinFeedBackShow();
})
var downloadUrl = window.location.origin;
//问题反馈详情
var queryFinFeedBackShow=function(){
	var request=getRequest();
	//问题id
	var id;
	if(request&&request.id){
		id=request.id;
	}
	$.ajax({
		type:"post",
		url:"/r/feedback/showFeedBackDetails",
		data:{
			"id":id
			},
		dataType:'json',
		success:function(jsonObject){
			if(jsonObject.status == 1 && jsonObject.data && jsonObject.data.arrlist.length > 0){
				 for(var index in jsonObject.data.arrlist){
					 var val = jsonObject.data.arrlist[index];
					 $('#conxt').empty().html(val.issue);
					 for(var i=0;i<(val.imgs).split(",").length;i++){
						 var pic=val.imgs.split(",")[i];
						 var result = "<div onclick=showfeedBackPhoto('"+val.imgs+"','"+i+"','show') class='result'><img style='cursor: pointer;width: 80%;height: 17%;margin-top: 3%;' src='"+downloadUrl+pic+"' alt=''/></div>";
						 $('#pic').append(result);
					 }
				 }
			}
		}
	});
	
}

function showfeedBackPhoto(url,index,type) {
	if("show"==type){
		$('#feedBackImg').attr('src', downloadUrl+url.split(",")[index]);
	    $('#feedBackImg').attr('href', downloadUrl+url.split(",")[index]);
	    $('#feedBackImg').attr('name', url.split(",")[0]);
	    $('#feedImg').click(function(){
	    	showfeedBackPhoto(url,parseInt(index)+1,'next');
	    });
	    $('#feedBackImgModal').modal('show');
	}else{
	    if(parseInt(index)+1>=url.split(",").length)
	    	index=0
	    $('#feedBackImg').attr('src', downloadUrl+url.split(",")[index]);
	    $('#feedBackImg').attr('href', downloadUrl+url.split(",")[index]);
	    $('#feedBackImg').attr('name', url.split(",")[0]);
	    $('#feedImg').one("click",function(){
	    	showfeedBackPhoto(url,parseInt(index)+1,'next');
	    });
	}
}