import geb.junit4.GebReportingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4)
class GebishOrgTest extends GebReportingTest {

    @Test
    void canGetToTheCurrentBookOfGeb() {
        to HomePage

        ["https://www.8notes.com/guitar_chord_chart/C.asp"].findAll {it.trim().length() > 2 }
                .each {
            scrap(it)
        }
    }

}