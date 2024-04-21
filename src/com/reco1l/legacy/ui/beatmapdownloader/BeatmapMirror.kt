package com.reco1l.legacy.ui.beatmapdownloader

import org.json.JSONObject


/**
 * Defines an action to be performed on a mirror API.
 */
data class MirrorAction<R, M>(

    /**
     * The action API endpoint.
     */
    // TODO replace with a request creation function, some APIs have different query arguments.
    val endpoint: String,

    /**
     * A function to map the response into a model.
     */
    val mapResponse: (R) -> M

)

/**
 * Defines a beatmap mirror API and its actions.
 */
enum class BeatmapMirror(

    /**
     * The search query action.
     */
    val search: MirrorAction<JSONObject, BeatmapSetModel>,

    val downloadEndpoint: (Long) -> String

) {

    /**
     * osu.direct beatmap mirror.
     *
     * [See documentation](https://old.osu.direct/doc)
     */
    OSU_DIRECT(
        search = MirrorAction(
            endpoint = "https://api.osu.direct/v2/search",
            mapResponse = {

                BeatmapSetModel(
                    id = it.getLong("id"),
                    title = it.getString("title"),
                    titleUnicode = it.getString("title_unicode"),
                    artist = it.getString("artist"),
                    artistUnicode = it.getString("artist_unicode"),
                    status = it.getString("status"),
                    creator = it.getString("creator"),
                    thumbnail = it.optJSONObject("covers")?.optString("card"),
                    beatmaps = run {

                        val beatmaps = mutableListOf<BeatmapModel>()
                        val array = it.getJSONArray("beatmaps")

                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)

                            beatmaps.add(
                                BeatmapModel(

                                    version = obj.getString("version"),
                                    starRating = obj.getDouble("difficulty_rating"),
                                    ar = obj.getDouble("ar"),
                                    cs = obj.getDouble("cs"),
                                    hp = obj.getDouble("drain"),
                                    od = obj.getDouble("accuracy"),
                                    bpm = obj.getDouble("bpm"),
                                    lengthSec = obj.getLong("hit_length"),
                                    circleCount = obj.getInt("count_circles"),
                                    sliderCount = obj.getInt("count_sliders"),
                                    spinnerCount = obj.getInt("count_spinners")
                                )
                            )
                        }
                        beatmaps
                    }
                )
            }
        ),
        downloadEndpoint = { "https://api.osu.direct/d/$it" }
    );

}
