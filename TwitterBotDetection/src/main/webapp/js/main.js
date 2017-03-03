$(document).ready(function(){
    $("#get-data").click(function(){
        var showData = $('#show-data');

        $.getJSON("http://localhost:8080/rest/classify",
        {
            userid: "791455969016442881"
        },
        function(data) {
            showData.text('userid:'+data.userid+' ,'+'label:'+data.label);
        })
    });
});
