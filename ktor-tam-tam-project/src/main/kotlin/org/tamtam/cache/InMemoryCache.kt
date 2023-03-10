package org.tamtam.cache

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.tamtam.model.location.LocalCityModel
import org.tamtam.model.location.LocationRequestModel
import org.tamtam.model.location.LocationResponseModel
import org.tamtam.model.notes.*
import org.tamtam.model.speciality.SpecialityResponse
import org.tamtam.model.story.StoriesModel
import org.tamtam.model.story.StoryModel
import org.tamtam.model.story.StoryPhoneModel
import java.util.*
import javax.mail.Authenticator


import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


object InMemoryCache {
    const val gmailAppPassword = "indbfijdyjgjnntc"
    const val mapQuestApiKey = "dy0Q8BCh4uEAWFhMHg8JgAokDlGSjAI0"
    const val mongoPassword = "7EI3uJqVo11CgwoS"
    const val mongoName = "tamtam"
    const val dbName = "tam-tam-db"

    private val cities = arrayListOf<LocalCityModel>()
    private val specialities = arrayListOf<SpecialityResponse>()

    fun saveSpecialitiesList(list: List<SpecialityResponse>){
        if (specialities.isNullOrEmpty())
            specialities.addAll(list.distinctBy { x -> x.name })
    }

    fun getSpecialitiesList() = specialities

    fun saveCitiesList(list: List<LocalCityModel>) {
        if (cities.isNullOrEmpty())
            cities.addAll(list)
    }

    fun getCitiesList() = cities

    fun findCitiesVariants(variant: String?, list: List<LocalCityModel>): List<LocationResponseModel> {
        if (variant.isNullOrEmpty()) {
            return list.map {
                LocationResponseModel(
                    LocationRequestModel(it.coords.lat.toDouble(), it.coords.lon.toDouble()),
                    it.name,
                    it.subject,
                    "Russia"
                )
            }
        } else return list.filter { x -> x.name.contains(variant, true) }.map {
            LocationResponseModel(
                LocationRequestModel(it.coords.lat.toDouble(), it.coords.lon.toDouble()),
                it.name,
                it.subject,
                "Russia"
            )
        }
    }


    fun getStories(): List<StoriesModel> {
        return arrayListOf(
            StoriesModel(
                "?????????????? + ???????? - 600??????!",
                "https://jardam1.ru/media/images_boards/big/63d7b601eab1c.webp",
                listOf(
                    StoryModel(
                        "?????????????????????? ???????????? ?? ????????????",
                        "??????????, ??????????, ??????????\n" +
                                "??????????????+?????????????? =2500\n" +
                                "?????????? ?????????????? ???????????????????? ?????? -????????????????\n" +
                                "?????????? ???????? ?????????? ?????????? ????????????????????,?????????????????????????? ??????????????",
                        "https://img.staticdj.com/f3710ffaa3530d3baf926cef994f35f2_750x.jpeg",
                        StoryPhoneModel("+79998887766", "whatsapp"),
                        System.currentTimeMillis().toString()
                    ),
                    StoryModel(
                        "??????????????, ??????????????",
                        "?????????????????????????????? ?????????????? ?????????? ?????????? ???????????????? ?????????? ??????????",
                        "https://i.pinimg.com/564x/3e/5e/5b/3e5e5bcc8032aae02b9deba9a8c77f74.jpg",
                        StoryPhoneModel("", ""),
                        System.currentTimeMillis().toString()
                    ),
                    StoryModel(
                        "?????????????? ?? ?????????????????? 490???, ???????????????? ???????????? 490???",
                        "?????????????? ?? ?????????????????? 490??? ???????????????? ???????????? 490???, ?????????????????? ???????????? ???????????? 490???. ???????????? 89684451578",
                        "https://i.pinimg.com/564x/d5/0f/9e/d50f9e90248dfe2071c0b6a352f64016.jpg",
                        StoryPhoneModel("", ""),
                        System.currentTimeMillis().toString()
                    )
                )
            ),
            StoriesModel(
                "?????????????????? ?????????? ??????????",
                "https://yntymak.ru/oc-content/uploads/1011/49644.jpg",
                listOf(
                    StoryModel(
                        "?????????????????????? ???????????????????????? ?????????? ?????????? ??????????????",
                        "???????????????? ?????????????? ???????????? ???????? ?????????????????????? ?????????????????????? ?????????????????????? ???????????????????????? ?????????????? ?????? ?????????? ?????????? ?????????????? ?????????????? ?????????? ?????????????????????????? ?????????????????????? ???????? ???????? ?????????????? ?????????? ??????????????????????",
                        "https://yntymak.ru/oc-content/uploads/1011/49644.jpg",
                        StoryPhoneModel("", ""),
                        System.currentTimeMillis().toString()
                    ),
                    StoryModel(
                        "?????????????????????? ?????????? ?????????????? ???????????? ??????????????????",
                        "",
                        "https://yntymak.ru/oc-content/uploads/1164/61915.jpg",
                        StoryPhoneModel("", ""),
                        System.currentTimeMillis().toString()
                    )
                )
            )
        )
    }


    fun sendSimpleNotification(token : String, title : String, body : String): String? {

//        val message: Message = Message.builder()
//            .putData("title", "Note published")
//            .putData("body", "Your note was published successfully")
//            .setToken(registrationToken)
//            .build()

        val message = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .build()

        return FirebaseMessaging.getInstance().send(message)
    }



}

