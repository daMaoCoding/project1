var dataArr = []; // 储存所选图片的结果(文件名和base64数据)  
var urls = []; // 储存所选图片的url
var names = []; // 储存所选图片的url
var downloadUrl = window.location.origin;
jQuery(function($) {
        var input = document.getElementById("file_input");
        var result;        
		var fd;  //FormData方式发送请求        
		//var oSelect = document.getElementById("select");      
		var oAdd = document.getElementById("file_input");      
		var oInput = document.getElementById("file_input");           
		if(typeof FileReader==='undefined'){            
			alert("抱歉，你的浏览器不支持 FileReader");            
			input.setAttribute('disabled','disabled');        
		}else{            
		    input.addEventListener('change',readFile,false);        
		}　　　　　//handler                   
		function readFile(){
		    fd = new FormData();            
			var iLen = this.files.length;          
			var index = 0;          
			for(var i=0;i<iLen;i++){              
			    if (!input['value'].match(/.jpg|.gif|.png|.jpeg|.bmp/i)){ //判断上传文件格式  
				   return alert("上传的图片格式不正确，请重新选择");               
				}              
				var reader = new FileReader();              
				reader.index = i;                
				fd.append(i,this.files[i]);              
				reader.readAsDataURL(this.files[i]);  //转成base64                
				reader.fileName = this.files[i].name;                  
				reader.onload = function(e){                   
				var imgMsg = { 
				     name : this.fileName,//获取文件名                        
					 base64 : this.result   //reader.readAsDataURL方法执行完后， base64数据储存在reader.result里              
				}                   
				dataArr.push(imgMsg);
				urls.push(this.result);
				names.push((this.fileName).replace(".", ''));
				var filename=(this.fileName).replace(".", '');
				result = "<span onclick=deletePic('"+filename+"') id='delete"+filename+"' class='delete'><i class='ace-icon fa fa-times red'></i></span><span id='delete1"+filename+"' onclick=feedBackPhoto('"+this.result+"','"+filename+"','show') style='width: 200px;' class='result'><img src='"+this.result+"' alt=''/></span>";                   
				var div = document.createElement('span');                  
				div.innerHTML = result;                    
				div['className'] = 'float inline';                  
				div['index'] = index;                    
				document.getElementById('showPic').appendChild(div);  　　//插入dom树                    
				var img = div.getElementsByTagName('img')[0];                  
				img.onload = function(){    
				  var nowHeight = ReSizePic(this); //设置图片大小                        
				  this.parentNode.style.display = 'block';                        
				  var oParent = this.parentNode;                        
				  if(nowHeight){               
				    oParent.style.paddingTop = (oParent.offsetHeight - nowHeight)/2 + 'px';                       
				  }                    
				}                       
//				document.getElementById('delete'+i+'').onclick = function(){                      
//				    this.remove();                  // 在页面中删除该图片元素                     
//				    dataArr.splice(this.index,1); 
//				    //dataArr.rem[this.index];  // 删除dataArr对应的数据                                        
//				}                  
				index++;              
				}            
				}        
				}                        
//				oSelect.onclick=function(){           
//				     oInput.value = "";   // 先将oInput值清空，否则选择图片与上次相同时change事件不会触发          //清空已选图片          
//					 $('.float').remove();          
//					 dataArr = [];           
//					 index = 0;                  
//					 oInput.click();       
//				}           
				oAdd.onclick=function(){      
				     oInput.value = "";   // 先将oInput值清空，否则选择图片与上次相同时change事件不会触发          
					 oInput.click();       
				}           
})    /*     用ajax发送fd参数时要告诉jQuery不要去处理发送的数据，     不要去设置Content-Type请求头才可以发送成功，否则会报“Illegal invocation”的错误，     也就是非法调用，所以要加上“processData: false,contentType: false,”     * */                        

function send(){
  $('#save').attr("disabled",true);
  var submitArr = [];
  if(dataArr.length>5 || dataArr<=0){
	  $('#prompt_pic').show(10).delay(1500).hide(10);
	  $('#save').attr("disabled",false);
	  return;
  }
  for (var i = 0; i < dataArr.length; i++) {       
    if (dataArr[i]) {                 
	   submitArr.push(dataArr[i]);              
	}          
  }     
  var level = $("input[name='level']:checked").val();
  var describe=$.trim($("#describe").val());
  if(describe=="" || describe==null){
	  $('#prompt_describe').text('请填写备注').show(10).delay(1500).hide(10);
	  $('#save').attr("disabled",false);
	  return;
  }
  $.ajax({  
	     url:"/r/feedback/saveFeedBack",      
		 type : 'post',             
		 data :{ 
			 "images":JSON.stringify(submitArr),
			 "level":level,
			 "describe":describe},                
		 dataType: 'json',      
		 success : function(data){                    
			 if(data.status == 1){
				 history.back(-1);
			 }    　
		 }         
	})       
}                   

function deletePic(filename){
	$('#delete'+filename+'').remove();
	$('#delete1'+filename+'').remove();
	var i = names.length;
	var index=0;
    while (i--) {
        if (names[i] == filename) {
        	index=i;
        	break;
        }
    }
	dataArr.splice(index,1);
	urls.splice(index,1);
	names.splice(index,1);
}

function feedBackPhoto(url,name,type) {
	if("show"==type){
		$('#feedBackImg').attr('src', url);
	    $('#feedBackImg').attr('href', url);
	    $('#feedBackImg').attr('name', name);
	    $('#feedImg').click(function(){
	    	feedBackPhoto('1',name,'next');
	    });
	    $('#feedBackImgModal').modal('show');
	}else{
		var i = names.length;
		var length = names.length;
		var index=0;
	    while (i--) {
	        if (names[i] == name) {
	        	index=i+1;
	        	break;
	        }
	    }
	    if(index>=length)
	    	index=0
	    $('#feedBackImg').attr('src', urls[index]);
	    $('#feedBackImg').attr('href', urls[index]);
	    $('#feedBackImg').attr('name', names[index]);
	    $('#feedImg').one("click",function(){
	    	feedBackPhoto('1',names[index],'next');
	    });
	}
}

function ReSizePic(ThisPic) {        
     var RePicWidth = 150; //这里修改为您想显示的宽度值            
	 var TrueWidth = ThisPic.width; //图片实际宽度        
	 var TrueHeight = ThisPic.height; //图片实际高度                
	 if(TrueWidth>TrueHeight){            //宽大于高            
		 var reWidth = RePicWidth;            
		 ThisPic.width = reWidth;            //垂直居中            
		 var nowHeight = TrueHeight * (reWidth/TrueWidth);            
		 return nowHeight;  //将图片修改后的高度返回，供垂直居中用        
	 }else{            //宽小于高            
	     var reHeight = RePicWidth;            
		 ThisPic.height = reHeight;        
	 }    
}

