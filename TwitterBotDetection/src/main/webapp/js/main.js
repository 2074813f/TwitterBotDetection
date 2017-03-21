$(document).ready(function(){


    //Initial page.
    $("#content-div").load("home.html");

    //Nav content loading.
    $('ul#nav li a').click(function(){
      var page = $(this).attr('href');
      $("#content-div").load(page);

      return false;
    });
});
