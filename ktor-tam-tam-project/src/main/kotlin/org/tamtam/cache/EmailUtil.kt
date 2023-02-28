package org.tamtam.cache


import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


object EmailUtil {

    val codesMap = mutableMapOf<String, String>()


    fun addCodeAndEmail(email: String, code: String){
        codesMap[email] = code
    }

    fun getCode(email: String) = codesMap[email]


    fun sendEmailNew(email : String, code : String): Boolean {
        val appPassword = InMemoryCache.gmailAppPassword
        val from = "podxvat777@gmail.com"

        val props = System.getProperties()
        props.setProperty("mail.smtp.host", "smtp.gmail.com")
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
        props.setProperty("mail.smtp.socketFactory.fallback", "false")
        props.setProperty("mail.smtp.port", "465")
        props.setProperty("mail.smtp.socketFactory.port", "465")
        props["mail.smtp.auth"] = "true"
        props["mail.debug"] = "true"
        props["mail.smtp.ssl.protocols"] = "TLSv1.2"
        props["mail.store.protocol"] = "pop3"
        props["mail.transport.protocol"] = "smtp"
        val session = Session.getDefaultInstance(props,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication? {
                    return PasswordAuthentication(from, appPassword)
                }
            })

        var isSuccess = false
        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(from))
            message.addRecipient(Message.RecipientType.TO, InternetAddress(email))
            message.subject = "Подтверждение электронной почты"
            message.setText("Код подтверждения: $code")
            Transport.send(message)
            println("Sent message successfully....")
            isSuccess = true
        } catch (mex: MessagingException) {
            mex.printStackTrace()
            isSuccess = false
        }
        return isSuccess
    }
}