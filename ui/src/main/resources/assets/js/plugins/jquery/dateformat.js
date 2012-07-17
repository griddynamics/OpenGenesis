/**
 *
 *  https://github.com/phstc/jquery-dateFormat (version 1.0)
 *
 *  IMPORTANT: https://github.com/phstc/jquery-dateFormat/issues/27
 *             TO avoid name collision with jquery validate plugin (jquery.validate.js occupy jQuery.format namespace) the original source file was modified:
 *             jQuery.format was renamed to jQuery.formatDate
 *
 */(function(n){var u=["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"],i=["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"],r=["January","February","March","April","May","June","July","August","September","October","November","December"],t=[];t.Jan="01",t.Feb="02",t.Mar="03",t.Apr="04",t.May="05",t.Jun="06",t.Jul="07",t.Aug="08",t.Sep="09",t.Oct="10",t.Nov="11",t.Dec="12",n.formatDate=function(){var n="";function o(n){return u[parseInt(n,10)]||n}function s(n){var t=parseInt(n,10)-1;return i[t]||n}function h(n){var t=parseInt(n,10)-1;return r[t]||n}var e=function(n){return t[n]||n},f=function(t){var r=t,f=n,u,i;return r.indexOf(".")!==-1&&(u=r.split("."),r=u[0],f=u[1]),i=r.split(":"),i.length===3?(hour=i[0],minute=i[1],second=i[2],{time:r,hour:hour,minute:minute,second:second,millis:f}):{time:n,hour:n,minute:n,second:n,millis:n}};return{date:function(t,i){var g=null,a,c,w,nt,k;try{var b=g,p=g,v=g,y=g,d=g,l=g;if(typeof t=="number")return this.date(new Date(t),i);if(typeof t.getFullYear=="function")p=t.getFullYear(),v=t.getMonth()+1,y=t.getDate(),d=t.getDay(),l=f(t.toTimeString());else if(t.search(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.?\d{0,3}[-+]?\d{2}:?\d{2}/)!=-1)a=t.split(/[T\+-]/),p=a[0],v=a[1],y=a[2],l=f(a[3].split(".")[0]),b=new Date(p,v-1,y),d=b.getDay();else{a=t.split(" ");switch(a.length){case 6:p=a[5],v=e(a[1]),y=a[2],l=f(a[3]),b=new Date(p,v-1,y),d=b.getDay();break;case 2:c=a[0].split("-"),p=c[0],v=c[1],y=c[2],l=f(a[1]),b=new Date(p,v-1,y),d=b.getDay();break;case 7:case 9:case 10:p=a[3],v=e(a[1]),y=a[2],l=f(a[4]),b=new Date(p,v-1,y),d=b.getDay();break;case 1:c=a[0].split(n),p=c[0]+c[1]+c[2]+c[3],v=c[5]+c[6],y=c[8]+c[9],l=f(c[13]+c[14]+c[15]+c[16]+c[17]+c[18]+c[19]+c[20]),b=new Date(p,v-1,y),d=b.getDay();break;default:return t}}var r=n,u=n,tt=n;for(w=0;w<i.length;w++){nt=i.charAt(w),r+=nt,tt=n;switch(r){case"ddd":u+=o(d),r=n;break;case"dd":if(i.charAt(w+1)=="d")break;String(y).length===1&&(y="0"+y),u+=y,r=n;break;case"d":if(i.charAt(w+1)=="d")break;u+=parseInt(y,10),r=n;break;case"MMMM":u+=h(v),r=n;break;case"MMM":if(i.charAt(w+1)==="M")break;u+=s(v),r=n;break;case"MM":if(i.charAt(w+1)=="M")break;String(v).length===1&&(v="0"+v),u+=v,r=n;break;case"M":if(i.charAt(w+1)=="M")break;u+=parseInt(v,10),r=n;break;case"yyyy":u+=p,r=n;break;case"yy":if(i.charAt(w+1)=="y"&&i.charAt(w+2)=="y")break;u+=String(p).slice(-2),r=n;break;case"HH":u+=l.hour,r=n;break;case"hh":k=l.hour==0?12:l.hour<13?l.hour:l.hour-12,k=String(k).length==1?"0"+k:k,u+=k,r=n;break;case"h":if(i.charAt(w+1)=="h")break;k=l.hour==0?12:l.hour<13?l.hour:l.hour-12,u+=parseInt(k,10),r=n;break;case"mm":u+=l.minute,r=n;break;case"ss":u+=l.second.substring(0,2),r=n;break;case"SSS":u+=l.millis.substring(0,3),r=n;break;case"a":u+=l.hour>=12?"PM":"AM",r=n;break;case" ":u+=nt,r=n;break;case"/":u+=nt,r=n;break;case":":u+=nt,r=n;break;default:r.length===2&&r.indexOf("y")!==0&&r!="SS"?(u+=r.substring(0,1),r=r.substring(1,2)):r.length===3&&r.indexOf("yyy")===-1?r=n:tt=r}}return u+=tt}catch(it){return console.log(it),t}}}}()})(jQuery),jQuery.formatDate.date.defaultShortDateFormat="dd/MM/yyyy",jQuery.formatDate.date.defaultLongDateFormat="dd/MM/yyyy hh:mm:ss",jQuery(document).ready(function(){jQuery(".shortDateFormat").each(function(n,t){jQuery(t).is(":input")?jQuery(t).val(jQuery.formatDate.date(jQuery(t).val(),jQuery.formatDate.date.defaultShortDateFormat)):jQuery(t).text(jQuery.formatDate.date(jQuery(t).text(),jQuery.formatDate.date.defaultShortDateFormat))}),jQuery(".longDateFormat").each(function(n,t){jQuery(t).is(":input")?jQuery(t).val(jQuery.formatDate.date(jQuery(t).val(),jQuery.formatDate.date.defaultLongDateFormat)):jQuery(t).text(jQuery.formatDate.date(jQuery(t).text(),jQuery.formatDate.date.defaultLongDateFormat))})});
