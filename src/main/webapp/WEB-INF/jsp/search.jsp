<!DOCTYPE html>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>${res.query} - BarOLOLOmeter</title>

    <!-- Bootstrap -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css" integrity="sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r" crossorigin="anonymous">
    <link rel="stylesheet" href="http://getbootstrap.com/examples/starter-template/starter-template.css" />
    <link rel="stylesheet" href="/style.css" />

    <script>
      var rootPath = 'http://${pageContext.request.getHeader("host")}<spring:url value="/"/>';
    </script>
  </head>
  <body ng-app="contentopt">
    <nav class="navbar navbar-inverse navbar-fixed-top">
      <div class="container">
        <div class="navbar-header">
          <span class="navbar-brand" href="#">BarOLOLOmeter</span>
        </div>
        <div id="navbar" class="collapse navbar-collapse">
          <ul class="nav navbar-nav">
          </ul>
        </div>
      </div>
    </nav>

    <div class="container">
      <div class="starter-template">
        <form action="/search">
          <div class="input-group" style="padding-bottom: 25px;">
            <input name="q" type="text" class="form-control" placeholder="Search for..." value="${res.query}">
            <span class="input-group-btn">
              <button class="btn btn-primary" type="submit">Go!</button>
            </span>
          </div>
        </form>

        <div class="text-left">
          <ul class="list-unstyled" id="results">
            <c:forEach var="page" items="${res.pages}">
              <li style="padding-bottom: 13px;">
                <div>
                  <div><big><a href="${page.url}" target="_blank">${page.title}</a></big></div>
                  <div><small><a href="${page.url}" target="_blank">${page.url}</a></small></div>
                  <div>${page.snippet}</div>
                </div>
              </li>
            </c:forEach>
          <ul>
          <div><a href="javascript:void(0);">Load more...</a></div>
        </div>
      </div>
    </div>

    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" integrity="sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS" crossorigin="anonymous"></script>
  </body>
</html>