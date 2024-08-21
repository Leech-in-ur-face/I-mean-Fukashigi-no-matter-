package com.reco1l.osu

import android.util.Log
import com.reco1l.osu.data.DatabaseManager
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.difficulty.BeatmapDifficultyCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

object DifficultyCalculationManager {


    private var isRunning = false


    @JvmStatic
    fun calculateDifficulties() {

        if (isRunning) {
            return
        }
        isRunning = true

        CoroutineScope(Dispatchers.Default).launch {

            val beatmaps = LibraryManager.getLibrary().flatMap { set -> set.beatmaps.filter { it.needsDifficultyCalculation } }

            // No beatmaps difficulties pending to calculate.
            if (beatmaps.isEmpty()) {
                isRunning = false
                return@launch
            }

            ToastLogger.showText("Running background difficulty calculation. Song select menu's sort order may not be accurate during this process.", true)

            var processedBeatmaps = 0

            beatmaps.fastForEach { beatmapInfo ->

                launch {

                    val msStartTime = System.currentTimeMillis()

                    BeatmapParser(beatmapInfo.path).use { parser ->

                        val data = parser.parse(true)!!

                        val droidAttributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(data)
                        val standardAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(data)

                        beatmapInfo.droidStarRating = GameHelper.Round(droidAttributes.starRating, 2)
                        beatmapInfo.standardStarRating = GameHelper.Round(standardAttributes.starRating, 2)
                        beatmapInfo.hitCircleCount = data.hitObjects.circleCount
                        beatmapInfo.sliderCount = data.hitObjects.sliderCount
                        beatmapInfo.spinnerCount = data.hitObjects.spinnerCount
                        beatmapInfo.maxCombo = data.maxCombo
                    }

                    DatabaseManager.beatmapInfoTable.update(beatmapInfo)

                    if (BuildConfig.DEBUG) {
                        Log.i("DifficultyCalculation", "Calculated difficulty for ${beatmapInfo.path}, took ${System.currentTimeMillis() - msStartTime}ms.")
                    }

                    processedBeatmaps++

                    // When it's the last one, reload the library and notify the song menu.
                    if (processedBeatmaps >= beatmaps.size) {

                        LibraryManager.loadLibrary()
                        GlobalManager.getInstance().songMenu?.onDifficultyCalculationEnd()
                        isRunning = false

                        ToastLogger.showText("Background difficulty calculation has finished.", true)
                    }
                }

            }

        }

    }

}