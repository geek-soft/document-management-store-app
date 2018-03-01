package uk.gov.hmcts.dm.functional

import io.restassured.RestAssured
import io.restassured.response.Response
import net.jcip.annotations.NotThreadSafe
import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import uk.gov.hmcts.dm.functional.utilities.Classifications
import uk.gov.hmcts.dm.functional.utilities.FileUtils
import uk.gov.hmcts.dm.functional.utilities.V1MediaTypes

import javax.annotation.PostConstruct
import uk.gov.hmcts.dm.functional.config.FunctionalTestContextConfiguration

import static io.restassured.RestAssured.given
import static io.restassured.RestAssured.expect
import static org.hamcrest.Matchers.equalTo

@ContextConfiguration(classes = FunctionalTestContextConfiguration)
@NotThreadSafe
class BaseIT {

    @Autowired
    AuthTokenProvider authTokenProvider

    FileUtils fileUtils = new FileUtils()

//    @Value('${base-urls.dm-api-gw-web}')
//    String dmApiGwBaseUri

    @Value('${base-urls.dm-store-app}')
    String dmStoreAppBaseUri

    @Value('${base-urls.idam-user}')
    String idamUserBaseUri

    final String PASSWORD = '123'

    String CITIZEN = 'test12@test.com'

    String CITIZEN_2 = 'test2@test.com'

    String CASE_WORKER = 'test3@test.com'

    String CASE_WORKER_ROLE_PROBATE = 'caseworker-probate'
    String CASE_WORKER_ROLE_CMC = 'caseworker-cmc'
    String CASE_WORKER_ROLE_SSCS = 'caseworker-sscs'
    String CASE_WORKER_ROLE_DIVORCE = 'caseworker-divorce'

    final String NOBODY = null

    final String FILES_FOLDER = 'files/'
    final String ATTACHMENT_1 = 'Attachment1.txt'
    final String ATTACHMENT_2 = 'Attachment2.txt'
    final String ATTACHMENT_3 = 'Attachment3.txt'
    final String ATTACHMENT_4_PDF = '1MB.PDF'
    final String ATTACHMENT_5 = 'Attachment1.csv'
    final String ATTACHMENT_6_GIF = 'marbles.gif'
    final String ATTACHMENT_7_PNG = 'png.png'
    final String ATTACHMENT_8_TIF = 'tif.tif'
    final String ATTACHMENT_9_JPG = 'jpg.jpg'
    final String ATTACHMENT_10 = 'svg.svg'
    final String ATTACHMENT_11 = 'rtf.rtf'
    final String ATTACHMENT_12 = 'docx.docx'
    final String ATTACHMENT_13 = 'pptx.pptx'
    final String ATTACHMENT_14 = 'xlsx.xlsx'
    final String ATTACHMENT_15 = 'odt.odt'
    final String ATTACHMENT_16 = 'ods.ods'
    final String ATTACHMENT_17 = 'odp.odp'
    final String ATTACHMENT_18 = 'xml.xml'
    final String ATTACHMENT_19 = 'wav.wav'
    final String ATTACHMENT_20 = 'mid.mid'
    final String ATTACHMENT_21 = 'mp3.mp3'
    final String ATTACHMENT_22 = 'webm.webm'
    final String ATTACHMENT_23 = 'ogg.ogg'
    final String ATTACHMENT_24 = 'mp4.mp4'
    final String ATTACHMENT_25_TIFF = 'tiff.tiff'
    final String ATTACHMENT_26_BMP = 'bmp.bmp'
    final String ATTACHMENT_27_JPEG = 'jpeg.jpeg'
    final String THUMBNAIL_PDF = 'thumbnailPDF.jpg'
    final String THUMBNAIL_BMP = 'thumbnailBMP.jpg'
    final String THUMBNAIL_GIF = 'thumbnailGIF.jpg'


    final String BAD_ATTACHMENT_1 = '1MB.exe'
    final String BAD_ATTACHMENT_2 = 'Attachment3.zip'
    final String MAX_SIZE_ALLOWED_ATTACHMENT = '90MB.pdf'
    final String TOO_LARGE_ATTACHMENT = '100MB.pdf'
    final String ILLEGAL_NAME_FILE = 'uploadFile~@$!.jpg'
    final String ILLEGAL_NAME_FILE1 = 'uploadFile~`\';][{}!@£$%^&()}{_-.jpg'
    final String ILLEGAL_NAME_FILE2 = 'uploadFile9 @_-.jpg'
    final String VALID_CHAR_FILE1= 'uploadFile 9.txt'

    @PostConstruct
    void init() {
        RestAssured.baseURI = dmStoreAppBaseUri
    }

    def givenUnauthenticatedRequest() {
        def request = given().log().all()
        request
    }

    def givenRequest(username = null, userRoles = null) {

        def request = given().log().all()

        if (username) {
            request = request.header("serviceauthorization", serviceToken())
            if (username) {
                request = request.header("user-id", username)
            }
            if (userRoles) {
                request = request.header("user-roles", userRoles.join(','))
            }
        }

        request
    }

    def givenS2SRequest() {
        given().log().all()
            .header("serviceauthorization", serviceToken())
            .header("cache-control", "no-cache")
    }

    def expectRequest() {
        expect().log().all()
    }

    def file(fileName) {
        fileUtils.getResourceFile(FILES_FOLDER+fileName)
    }

    def authToken(username) {
        def token = authTokenProvider.getTokens(username, PASSWORD).getUserToken()
        token
    }

    def userId(token) {
        authTokenProvider.findUserId(token).toString()
    }

    def serviceToken() {
        authTokenProvider.findServiceToken()
    }

    def createDocument(username,  filename = null, classification = null, roles = null, metadata = null) {
        def request = givenRequest(username)
                        .multiPart("files", file( filename ?: ATTACHMENT_9_JPG), MediaType.IMAGE_JPEG_VALUE)
                        .multiPart("classification", classification ?: "PUBLIC")

        roles?.each { role ->
            request.multiPart("roles", role)
        }

        if (metadata) {
            request.accept(V1MediaTypes.V1_HAL_DOCUMENT_AND_METADATA_COLLECTION_MEDIA_TYPE_VALUE)
            metadata?.each { key, value ->
                request.multiPart("metadata[${key}]", value)
            }
        }

        request
            .expect()
                .statusCode(200)
            .when()
                .post("/documents")
    }

    def createDocumentAndGetUrlAs(username, filename = null, classification = null, roles = null, metadata = null) {
        createDocument(username, filename, classification, roles, metadata)
            .path("_embedded.documents[0]._links.self.href")
    }

    def createDocumentAndGetBinaryUrlAs(username,  filename = null, classification = null, roles = null) {
        createDocument(username, filename, classification, roles)
            .path("_embedded.documents[0]._links.binary.href")
    }

    def createDocumentContentVersion(documentUrl, username, filename = null) {
        givenRequest(username)
            .multiPart("file", file( filename ?: ATTACHMENT_9_JPG), MediaType.IMAGE_JPEG_VALUE)
            .expect()
                .statusCode(201)
            .when()
                .post(documentUrl)
    }

    def createDocumentContentVersionAndGetUrlAs(documentUrl, username, filename = null) {
        createDocumentContentVersion(documentUrl, username, filename).path('_links.self.href')
    }

    def createDocumentContentVersionAndGetBinaryUrlAs(documentUrl, username, filename = null) {
        createDocumentContentVersion(documentUrl, username, filename).path('_links.binary.href')
    }

    def CreateAUserforTTL(username) {
        Response response = givenRequest(username)
            .multiPart("files", file(ATTACHMENT_9_JPG), MediaType.IMAGE_JPEG_VALUE)
            .multiPart("classification", Classifications.PUBLIC as String)
            .multiPart("roles", "citizen")
            .multiPart("roles", "caseworker")
            .multiPart("ttl", "2018-10-31T10:10:10+0000")
            .expect().log().all()
            .statusCode(200)
            .contentType(V1MediaTypes.V1_HAL_DOCUMENT_COLLECTION_MEDIA_TYPE_VALUE)
            .body("_embedded.documents[0].originalDocumentName", equalTo(ATTACHMENT_9_JPG))
            .body("_embedded.documents[0].mimeType", equalTo(MediaType.IMAGE_JPEG_VALUE))
            .body("_embedded.documents[0].classification", equalTo(Classifications.PUBLIC as String))
            .body("_embedded.documents[0].roles[0]", equalTo("caseworker"))
            .body("_embedded.documents[0].ttl", equalTo("2018-10-31T10:10:10.000+0000"))
            .when()
            .post("/documents")

        response
    }

    void assertByteArrayEquality(String fileName, byte[] response) {
        Assert.assertTrue(Arrays.equals(file(fileName).bytes, response))
    }

}