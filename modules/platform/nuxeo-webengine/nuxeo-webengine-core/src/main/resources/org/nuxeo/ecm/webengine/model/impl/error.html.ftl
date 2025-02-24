<#escape x as x?html>
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <title>An error occurred</title>
  <meta name="viewport" content="width=device-width">

  <link rel="icon" type="image/png" href="${contextPath}/icons/favicon.png" />
  <link rel="shortcut icon" type="image/x-icon" href="${contextPath}/icons/favicon.ico" />
  <style type="text/css">
      <!--
      body {
        background: url("${contextPath}/img/error_pages/page_background.gif") repeat scroll 0 0 transparent;
        color: #999;
        font: normal 100%/1.5 "Lucida Grande", Arial, Verdana, sans-serif;
        margin: 0;
        text-align: center
      }

      .container {
        margin: 2em auto;
        text-align: center;
        width: 70%
      }

      h1 {
        color: #000;
        font-size: 150%;
        margin: 3.5em 0 .5em 0
      }

      h2 {
        color: #b20000;
        font-size: 110%;
        margin: 1em
      }

      h1, h2 {
        font-weight: bold
      }

      p {
        max-width: 600px;
        margin: .4em auto
      }

      .errorDetail {
        background-color: #fff;
        height: 40%;
        margin: 1em auto;
        overflow: auto;
        text-align: left;
        width: 100%
      }

      .errorDetail .scrollableBlock {
        max-height: 50em;
        overflow-y: scroll;
      }

      .block {
        border: 1px solid #ccc;
        border-radius: 5px;
        margin: 0.5em;
        padding: 0.5em;
      }
      -->
  </style>
</head>
<body>

<section>
  <div class="container">
    <h1>An error occurred</h1>
    <h2>${exception.statusCode} - ${exception.message}</h2>
    <#if isDevModeSet>
      <div class="errorDetail block">
        <h4>Stack Trace:</h4>
        <div class="scrollableBlock block">
          <pre>${exception.stackTrace}</pre>
        </div>
      </div>
    </#if>
  </div>
</section>

</body>
</html>
</#escape>
