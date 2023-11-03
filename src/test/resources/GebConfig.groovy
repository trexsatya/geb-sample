/*
	This is the Geb configuration file.

	See: http://www.gebish.org/manual/current/#configuration
*/


import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver

waiting {
	timeout = 2
}

environments {
	firefox {
		driver = { new FirefoxDriver() }
	}
//	phantomJs {
//		driver = { new PhantomJSDriver() }
//	}
	chrome {
		driver = { new ChromeDriver() }
	}
}

// To run the tests with all browsers just run “./gradlew SrtDownloadAndTranslation”

baseUrl = "https://www.8notes.com/guitar_chord_chart/C.asp"
