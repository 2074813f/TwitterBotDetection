$(document).ready(function(){
    $("#get-data").click(function(){

        $.getJSON("http://localhost:8080/rest/classify",
        {
            userid: "791455969016442881"
        },
        function(data) {
            $("#data-userid").text('userid:'+data.userid);
            $("#data-label").text('label:'+data.label);
        })
    });
});
