$(function() {
  console.log("hello comgate");  
});

  function post() {
    console.log("post");
    const urlParams = new URLSearchParams(window.location.search);
    const method = urlParams.get('method');
    console.log(method);
    $.get('/pay', function(response) {
        console.log(response.headers);
    });
//    window.location.href = "https://www.seznam.cz/";
//    
//    
//    
  }
