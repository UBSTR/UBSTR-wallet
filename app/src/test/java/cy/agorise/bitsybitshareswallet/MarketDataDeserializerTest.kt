package cy.agorise.bitsybitshareswallet

import com.google.gson.GsonBuilder
import cy.agorise.bitsybitshareswallet.models.coingecko.MarketData
import cy.agorise.bitsybitshareswallet.models.coingecko.MarketDataDeserializer
import org.junit.Assert
import org.junit.Test

class MarketDataDeserializerTest {
    @Test
    fun marketDataDeserializationTest(){
        val str = "{\"current_price\": {\"aed\": 0.14139359620401012,\"ars\": 1.476552955052185,\"aud\": 0.05410080634896981,\"bch\": 0.0003021370317928406,\"bdt\": 3.2298217535732276,\"bhd\": 0.01451147244444769,\"bmd\": 0.03849350092032233,\"bnb\": 0.007113127493734956,\"brl\": 0.15000509277539803,\"btc\": 0.00001043269732289735,\"cad\": 0.051866143140042266,\"chf\": 0.03825734329217614,\"clp\": 26.587581916766037,\"cny\": 0.2652895096426772,\"czk\": 0.8706365729081245,\"dkk\": 0.25236393094264586,\"eos\": 0.01566778197589746,\"eth\": 0.0003870069548974383,\"eur\": 0.033804376612212375,\"gbp\": 0.030484350651335475,\"hkd\": 0.3012660745118239,\"huf\": 10.909058160819312,\"idr\": 558.1942568455942,\"ils\": 0.14452962323048843,\"inr\": 2.721290348862006,\"jpy\": 4.327150672205728,\"krw\": 43.47379006939362,\"kwd\": 0.011703102097803801,\"lkr\": 6.939897047172613,\"ltc\": 0.0013225337650442446,\"mmk\": 60.56217136246436,\"mxn\": 0.7738105980956592,\"myr\": 0.1608450935955668,\"nok\": 0.335428517669597,\"nzd\": 0.056803550529088344,\"php\": 2.046274976098886,\"pkr\": 5.3730315641051885,\"pln\": 0.1449376543402434,\"rub\": 2.596498268228413,\"sar\": 0.1444545609036934,\"sek\": 0.3498212376637053,\"sgd\": 0.05281188996415366,\"thb\": 1.2598922851221481,\"try\": 0.20393883733037357,\"twd\": 1.1869880579631216,\"usd\": 0.03849350092032233,\"vef\": 9565.159285292651,\"xag\": 0.002632124388265174,\"xau\": 0.00003094261577979185,\"xdr\": 0.02769368731511483,\"xlm\": 0.3411570542267162,\"xrp\": 0.11074614753363282,\"zar\": 0.5534635980499906}}"
        val gson = GsonBuilder().registerTypeAdapter(MarketData::class.java, MarketDataDeserializer())
            .create()
        val marketData = gson.fromJson<MarketData>(str, MarketData::class.java)
        Assert.assertEquals(0.03849350092032233, marketData.current_price["usd"])
        Assert.assertEquals(0.033804376612212375, marketData.current_price["eur"])
    }
}