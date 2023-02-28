package org.tamtam.routes

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.tamtam.cache.InMemoryCache
import org.tamtam.cache.LocationCache
import org.tamtam.createHttpClient
import org.tamtam.db.UserDataSource
import org.tamtam.distanceInKm
import org.tamtam.model.exception.ExceptionResponse
import org.tamtam.model.location.*
import org.tamtam.model.speciality.SpecialityModel
import org.tamtam.model.speciality.SpecialityResponse
import kotlin.math.*


fun Route.getLocation(userDataSource: UserDataSource) {
    get("v1/find-cities") {
        val param = call.parameters["city"]
        val citiesList = if (InMemoryCache.getCitiesList().isNullOrEmpty()) {
            LocationCache.getCitiesList(call).apply { InMemoryCache.saveCitiesList(this) }
        } else InMemoryCache.getCitiesList()

        val foundList = InMemoryCache.findCitiesVariants(param, citiesList)
        call.respond(foundList)
    }

    post("v1/check-user-location") {
        val client = createHttpClient()
        val location = call.receiveNullable<LocationRequestModel>() ?: kotlin.run {
            call.respond(HttpStatusCode.BadRequest, "Body cannot be null!")
            return@post
        }
        val localLocation = getLocationFromResources(call, location)
        if (localLocation != null) {
            call.respond(localLocation)
        } else {
            val mapQuestResponse = getLocationFromMapQuest(client, location)
            if (mapQuestResponse == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ExceptionResponse("Location not found", HttpStatusCode.NotFound.value)
                )
            } else {
                call.respond(mapQuestResponse)
            }
        }
    }

    get("v1/metro-stations") {
        val param = call.parameters["city"]
        val dataArea = findArea(param)
        val listStations = findMetroByArea(dataArea)
        if (listStations.isNullOrEmpty() && param == "Москва") {
            call.respond(LocationCache.getMetroStations(call))
        } else call.respond(listStations)
        //call.respond(LocationCache.getMetroStations(call))
    }

    get("v1/specialities-list") {
        val speciality = call.request.queryParameters["speciality"]
        if (InMemoryCache.getSpecialitiesList().isNullOrEmpty()){
            InMemoryCache.saveSpecialitiesList(LocationCache.getSpecialities(call))
        }
        val listSpec = InMemoryCache.getSpecialitiesList()
        if (speciality.isNullOrEmpty()){
            call.respond(listSpec)
        }else {
            val filteredList = listSpec.filter { x -> x.name.contains(speciality, true) }
            call.respond(filteredList)
        }

    }
}


private suspend fun findMetroByArea(area: DataArea?): List<MetroStationsResponseModel> {
    val url = if (area == null) "https://api.hh.ru/metro/1"
    else "https://api.hh.ru/metro/" + area.id

    val client = createHttpClient()
    val response = client.get(url) {
        contentType(ContentType.Application.Json)
    }

    val listStations = arrayListOf<MetroStationsResponseModel>()
    if (response.status == HttpStatusCode.OK) {
        val model = response.body<MetroStationsData>()
        model.lines.forEach { line ->
            line.stations.forEach { station ->
                listStations.add(
                    MetroStationsResponseModel(
                        station.name,
                        arrayListOf(line.hex_color) ,
                        station.lat.toString(),
                        station.lng.toString()
                    )
                )
            }
        }
    }

    client.close()
    //return listStations.sortedBy { x -> x.name }.distinctBy { x -> x.name }
    return LocationCache.unitListsByName(listStations.sortedBy { x -> x.name })
}

private suspend fun findArea(city: String?): DataArea? {
    val client = createHttpClient()
    var needArea: DataArea? = null
    if (city.isNullOrEmpty()) {
        needArea = DataArea("1", "113", "Москва", emptyList())
    } else {
        val response = client.get(" https://api.hh.ru/areas/113") {
            contentType(ContentType.Application.Json)
        }
        val model = response.body<DataArea>()
        val area = model.areas.find { x -> x.name.contains(city, true) }

        if (area != null) {
            needArea = area
        } else {
            model.areas.forEach {
                val obj = it.areas.find { x -> x.name.contains(city, true)}
                if (obj != null) {
                    needArea = obj
                    return@forEach
                }
            }
        }
        client.close()
    }

    return needArea
}


private suspend fun getLocationFromMapQuest(
    client: HttpClient,
    locationRequest: LocationRequestModel
): LocationResponseModel? {
    val mapQuestApiKey = InMemoryCache.mapQuestApiKey
    val requestUrl = "http://www.mapquestapi.com/geocoding/v1/reverse?key=$mapQuestApiKey"
    val response = client.post(requestUrl) {
        contentType(ContentType.Application.Json)
        setBody(
            MapQuestRequestModel(
                MapQuestLocationCoord(LatLng(locationRequest.lat, locationRequest.lon)),
                MapQuestOptions(false),
                false,
                false
            )
        )
    }
    var locationResponse: LocationResponseModel? = null
    if (response.status == HttpStatusCode.OK) {
        val mapResponse: MapQuestResponseModel = response.body()
        val location = mapResponse.results.firstOrNull()?.locations?.firstOrNull()
        locationResponse = LocationResponseModel(
            locationRequest,
            location?.adminArea5,
            location?.adminArea4,
            location?.adminArea1
        )
    }
    client.close()
    return locationResponse
}

private suspend fun getLocationFromResources(
    call: ApplicationCall,
    locationRequest: LocationRequestModel
): LocationResponseModel? {
    val citiesList = if (InMemoryCache.getCitiesList().isNullOrEmpty()) {
        LocationCache.getCitiesList(call).apply { InMemoryCache.saveCitiesList(this) }
    } else InMemoryCache.getCitiesList()

    val city = citiesList.find { x ->
        distanceInKm(
            x.coords.lat.toDouble(),
            x.coords.lon.toDouble(),
            locationRequest.lat,
            locationRequest.lon
        ) <= 5
    }
    if (city != null) {
        return LocationResponseModel(
            LocationRequestModel(city.coords.lat.toDouble(), city.coords.lon.toDouble()),
            city.name,
            city.subject,
            "Russia"
        )
    } else return null
}


