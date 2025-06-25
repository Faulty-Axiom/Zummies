package a48626.sumolmbao.retrofit

import a48626.sumolmbao.data.Banzuke
import a48626.sumolmbao.data.Basho
import a48626.sumolmbao.data.RankChange
import a48626.sumolmbao.data.RikishiId
import a48626.sumolmbao.data.RikishiMatchesListResponse
import a48626.sumolmbao.data.RikishiMatchesResponse
import a48626.sumolmbao.data.RikishiStats
import a48626.sumolmbao.data.RikishiVersusMatchesResponse
import a48626.sumolmbao.data.Rikishis
import a48626.sumolmbao.data.TorikumiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SumoApiService
{
    @GET("api/basho/{bashoId}")
    fun getBasho(@Path("bashoId") bashoId: String): Call<Basho>

    @GET("api/basho/{bashoId}/banzuke/{division}")
    fun getBanzuke(
        @Path("bashoId") bashoId: String,
        @Path("division") division: String
    ): Call<Banzuke>

    @GET("api/basho/{bashoId}/torikumi/{division}/{day}")
    fun getTorikumi(
        @Path("bashoId") bashoId: String,
        @Path("division") division: String,
        @Path("day") day: Int
    ): Call<TorikumiResponse>

    //TODO SUBSTITUIR rikishis por rikishis?intai=true
    @GET("api/rikishis")
    fun getRikishis(
        @Query("shikonaEn") shikonaEn: String? = null,
        @Query("heya") heya: String? = null,
        @Query("sumodbId") sumodbId: Int? = null,
        @Query("nskId") nskId: Int? = null,
        @Query("intai") intai: Boolean? = true,
        @Query("measurements") measurements: Boolean? = null,
        @Query("ranks") ranks: Boolean? = null,
        @Query("shikonas") shikonas: Boolean? = null,
        @Query("limit") limit: Int = 1000,
        @Query("skip") skip: Int = 0
    ): Call<Rikishis>

    @GET("api/rikishi/{rikishiId}")
    suspend fun getRikishiById(
        @Path("rikishiId") rikishiId: Int,
        @Query("measurements") measurements: Boolean = false,
        @Query("ranks") ranks: Boolean = false,
        @Query("shikonas") shikonas: Boolean? = null
    ): RikishiId

    @GET("api/rikishi/{rikishiId}/stats")
    suspend fun getRikishiStats(
        @Path("rikishiId") rikishiId: Int
    ): RikishiStats

    @GET("api/rikishi/{rikishiId}/matches")
    suspend fun getRikishiMatches(
        @Path("rikishiId") rikishiId: Int
    ): RikishiMatchesListResponse

    @GET("api/rikishi/{rikishiId}/matches/{opponentId}")
    suspend fun getRikishiVersusMatches(
        @Path("rikishiId") rikishiId: Int,
        @Path("opponentId") opponentId: Int,
        @Query("bashoId") bashoId: String? = null
    ): RikishiVersusMatchesResponse

    @GET("api/ranks")
    suspend fun getRanks(
        @Query("rikishiId") rikishiId: Int,
    ): List<RankChange>
}