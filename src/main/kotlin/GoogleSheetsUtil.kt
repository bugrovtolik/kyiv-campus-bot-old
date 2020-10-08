import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.PemReader
import com.google.api.client.util.SecurityUtils
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import java.io.StringReader
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDate

class GoogleSheetsUtil {
    private fun readFromSheet(range: String): Any? {
        return getSheets().spreadsheets().values()[System.getenv("documentId"), range].execute()["values"]
    }

    fun appendToSheet(range: String, content: ValueRange): Any? {
        return getSheets().spreadsheets().values().append(System.getenv("documentId"), range, content)
            .setValueInputOption("USER_ENTERED").execute()
    }

    fun updateColumn(column: String, id: Long, text: String) {
        val students = getStudents()
        val row = students.indexOfFirst { it.id == id.toString() } + 2
        getSheets().spreadsheets().values().update(System.getenv("documentId"), "${System.getenv("sheetName")}!$column$row",
                ValueRange().setValues(listOf(listOf(text)))).setValueInputOption("USER_ENTERED").execute()
    }

    fun checkedInToday(chatId: Long): Boolean {
        return getStudents().any { it.id == chatId.toString() && it.lastCheckinDate.substring(0..9) == LocalDate.now().toString() }
    }

    fun isSubscribed(chatId: Long): Boolean {
        return getStudents().any { it.id == chatId.toString() && it.subscribed == "+" }
    }

    fun getStudents(): List<Student> {
        return (readFromSheet(System.getenv("sheetName")) as List<*>).drop(1).filterIsInstance<List<String>>().map {
            Student(
                id = it[0],
                firstName = it[1],
                lastName = it[2],
                lastCheckinDate = it[3],
                checkinCount = it[4],
                registerDate = it[5],
                wantWhoIAm = it[6],
                wantLectorium = it[7],
                subscribed = it[8],
                university = it[9],
                yearStudy = it[10],
                studProInfo = it[11]
            )
        }
    }

    private fun getSheets(): Sheets {
        val credentials = ServiceAccountCredentials.newBuilder().apply {
            clientEmail = System.getenv("clientEmail")
            privateKey = getPrivateKey(System.getenv("privateKey").replace("\\n", "\n"))
            scopes = listOf(SheetsScopes.SPREADSHEETS)
        }.build()

        return Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        ).apply { applicationName = "app" }.build()
    }

    private fun getPrivateKey(privateKey: String): PrivateKey {
        val section = PemReader.readFirstSectionAndClose(StringReader(privateKey), "PRIVATE KEY")
        return SecurityUtils.getRsaKeyFactory().generatePrivate(PKCS8EncodedKeySpec(section.base64DecodedBytes))
    }
}
