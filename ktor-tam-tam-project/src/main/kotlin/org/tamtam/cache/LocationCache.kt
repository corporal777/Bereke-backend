package org.tamtam.cache

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.server.application.*
import org.tamtam.model.location.LocalCitiesModel
import org.tamtam.model.location.LocalCityModel
import org.tamtam.model.location.MetroStationsResponseModel
import org.tamtam.model.speciality.SpecialityResponse
import java.lang.reflect.Type


object LocationCache {

    suspend fun getSpecialities(call : ApplicationCall) : List<SpecialityResponse> {
        val listType: Type = object : TypeToken<ArrayList<SpecialityResponse>>() {}.type
        val fileContent = call::class.java.classLoader.getResource("specialities-list.json")?.readText()
        return Gson().fromJson(fileContent, listType)
    }

    suspend fun getCitiesList(call : ApplicationCall): List<LocalCityModel> {
        val fileContent = call::class.java.classLoader.getResource("russian-cities.json")?.readText()
        return Gson().fromJson(fileContent, LocalCitiesModel::class.java).data
    }

    fun getMetroStations(call : ApplicationCall) : List<MetroStationsResponseModel> {
        val listType: Type = object : TypeToken<ArrayList<MetroStationsResponseModel>>() {}.type
        val fileContent = call::class.java.classLoader.getResource("metro-stations.json")?.readText()
        return Gson().fromJson(fileContent, listType)
    }

    fun unitListsByName(list : List<MetroStationsResponseModel>): ArrayList<MetroStationsResponseModel> {
        val unitList = arrayListOf<MetroStationsResponseModel>()
        unitList.addAll(list.distinctBy { x -> x.name })
        list.forEach { metro ->
            val index = unitList.indexOfFirst { x -> x.name == metro.name }
            if (!unitList[index].color.contains(metro.color[0])){
                unitList[index].color.add(metro.color[0])
            }
        }
        return unitList
    }
}