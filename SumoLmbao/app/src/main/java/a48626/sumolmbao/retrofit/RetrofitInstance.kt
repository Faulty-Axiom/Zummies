package a48626.sumolmbao.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance
{
    private const val BASE_URL = "https://www.sumo-api.com/"

    val api: SumoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SumoApiService::class.java)
    }
}