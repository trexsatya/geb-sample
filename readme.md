# Example Geb and Gradle Project

[![Build Status][build_status]](https://circleci.com/gh/geb/geb-example-gradle/tree/master)

## Description

This is an example of incorporating Geb into a Gradle build. It shows the use of Spock and JUnit 4 tests.

The build is setup to work with Firefox and Chrome. Have a look at the `build.gradle` and the `src/test/resources/GebConfig.groovy` files.

## Usage

The following commands will launch the tests with the individual browsers:

    ./gradlew chromeTest
    ./gradlew firefoxTest

To run with all, you can run:

    ./gradlew test

Replace `./gradlew` with `gradlew.bat` in the above examples if you're on Windows.


test.groovy scraping
https://y2down.cc/en/youtube-playlist.html
https://downsub.com/

Inject JQUERY Bookmark:  <br>
`javascript: (function (){    function l(u, i) {        var d = document;        if (!d.getElementById(i)) {            var s = d.createElement('script');            s.src = u;            s.id = i;            d.body.appendChild(s);        }    } l('//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js', 'jquery')})();`

Get Links With Subtitles From YT: <br>
`javascript: (function (){ let urls = $('div[aria-label="Closed captions"]').map((i, e) => $(e).parents("div.ytd-grid-video-renderer").find("a#thumbnail.yt-simple-endpoint").attr("href")).toArray().map(it => %60https://www.youtube.com${it}%60); console.log(urls)  })();`