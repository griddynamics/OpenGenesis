(function($, window) {
  $(function() {
    var errorMessagePanel = $(".error-message-local");

    if(location.href.indexOf("expire=true") !== -1) {
      errorMessagePanel.text("Your authentication has expired");
      errorMessagePanel.fadeTo(500, 1.0);
    }

    $('.close-button').click(function(){
      errorMessagePanel.fadeTo(500, 0);
  });

    $("form").submit(function(e) {
      e.preventDefault();
      var userName = $('input[name=j_username]').val();
      var password = $('input[name=j_password]').val();
      var rememberme = $('input[name=_spring_security_remember_me]:checked').val();
      $.ajax({
         url: "/signin",
         type: "POST",
         dataType: "json",
         data: { j_username: userName, j_password: password, _spring_security_remember_me: rememberme },
         timeout: 4000,
         success: function(data, textStatus, jqXHR) {
           if(data.success) {
             window.location = "/";
           } else {
             errorMessagePanel.text(data.errors);
             errorMessagePanel.fadeTo(500, 1.0);
           }
         },
         error: function(jqXHR, textStatus, errorThrown) {
           errorMessagePanel.html("Failed to login: " + textStatus);
           errorMessagePanel.fadeTo(500, 1.0);
         }
       });
    });
  });

})(jQuery, window);