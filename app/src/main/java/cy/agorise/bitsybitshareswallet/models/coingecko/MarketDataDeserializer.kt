package cy.agorise.bitsybitshareswallet.models.coingecko

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import junit.framework.Assert
import java.lang.reflect.Type

class MarketDataDeserializer : JsonDeserializer<MarketData> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MarketData {
        val hashMap = HashMap<String, Double>()
        val obj = json?.asJsonObject?.get("current_price")?.asJsonObject
        if(obj != null){
            val keySet = obj.asJsonObject.keySet()
            for(key in keySet){
                println("$key -> : ${obj[key].asDouble}")
                hashMap[key] = obj[key].asDouble
            }
        }
        return MarketData(hashMap)
    }
}