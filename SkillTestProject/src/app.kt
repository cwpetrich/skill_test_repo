import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import org.eclipse.egit.github.core.User
import org.eclipse.egit.github.core.service.OrganizationService
import org.eclipse.egit.github.core.service.UserService
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter


public class Main {

//    CHANGE ALL OF THESE PRIVATE PROPERTIES TO USE THE INFORMATION YOU NEED.
    private val GITHUB_USER = "github_username"
    private val GITHUB_PASS = "github_password"
    private val GITHUB_ORGANIZATION_NAME = "github_organization_name"
    private val GMAIL_SENDER_EMAIL = "youremail@gmail.com"  // GMail user name (just the part before "@gmail.com")
    private val GMAIL_SENDER_EMAIL_PASS = "your_email_password" // GMail password
    private val S3_BUCKET_NAME = "bucket_name"
    private val S3_BUCKET_OBJECT_KEY = "bucket_object_key"

    public fun main(argv: Array<String>) {
        val users = GetNoNameUsersFromGithubOrg(GITHUB_USER, GITHUB_PASS, GITHUB_ORGANIZATION_NAME)

        val loginsToSave = users.map { it.login }.toTypedArray()
        SaveListOfNoNamesInS3Bucket(S3_BUCKET_NAME, S3_BUCKET_OBJECT_KEY, loginsToSave)

        val to = users.filter { !it.email.isNullOrEmpty() }.map { it.email }.toTypedArray()
        SendAddNameEmailThroughGMail(GMAIL_SENDER_EMAIL, GMAIL_SENDER_EMAIL_PASS, to)
    }

    private fun SendAddNameEmailThroughGMail(from: String, pass: String, to: Array<String>) {
        for (email in to) iter@{
            println("${email}")
            if (email.isNullOrEmpty())
                return@iter

            val toMail = email
            val email = HtmlEmail()
            email.hostName = "smtp.gmail.com"
            email.setSmtpPort(587)
            email.setAuthenticator(DefaultAuthenticator(from, pass))
            email.isSSLOnConnect = true
            email.setFrom(from)
            email.setSubject("Give Yourself a Name on Github!")
            email.addTo(toMail)
            email.setHtmlMsg("<html><h1>Add You Name</h1></html><p>Click on the following link to update your name on Github.com<p><a href=\"https://github.com/settings/profile\">add name</a>")

            email.send()
        }
    }

    private fun GetNoNameUsersFromGithubOrg(user: String, password: String, orgName: String): List<User> {
        val orgService = OrganizationService()
        orgService.client.setCredentials(user, password)

        val userService = UserService()
        userService.client.setCredentials(user, password)

        var members = orgService.getMembers(orgName)
        val noNames = members.map {
            userService.getUser(it.login)
        }.filter { it.name.isNullOrEmpty() }

        return noNames
    }

    private fun SaveListOfNoNamesInS3Bucket(bucketName: String, bucketKey: String, logins: Array<String>) {
        val s3 = AmazonS3Client()
        val usWest2 = Region.getRegion(Regions.US_EAST_1)
        s3.setRegion(usWest2)

        val file = File.createTempFile("aws-java-sdk-", ".txt")
        file.deleteOnExit()

        val writer = OutputStreamWriter(FileOutputStream(file))
        for (login in logins) {
            writer.write("${login},")
        }
        writer.close()

        println("Uploading a new object to S3 from a file\n")
        s3.putObject(PutObjectRequest(bucketName, bucketKey, file))
    }
}

fun main(args: Array<String>) {
    Main().main(args)
}