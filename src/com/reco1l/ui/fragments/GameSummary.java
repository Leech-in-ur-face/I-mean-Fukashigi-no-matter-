package com.reco1l.ui.fragments;
// Created by Reco1l on 22/11/2022, 01:37

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reco1l.global.Game;
import com.reco1l.global.Scenes;
import com.reco1l.tables.AnimationTable;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;

import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.view.RoundedImageView;
import com.reco1l.view.custom.StatisticLayout;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyReCalculator;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class GameSummary extends BaseFragment {

    public static final GameSummary instance = new GameSummary();

    private StatisticLayout
            m0Text,
            m50Text,
            m100Text,
            m300Text,

            mComboText,
            mAccuracyText,
            mPPText;

    private TextView
            mDate,
            mUsername,

            mTitleText,
            mStarsText,
            mArtistText,
            mMapperText,
            mDifficultyText,

            mURText,
            mErrorText,
            mScoreText;

    private ImageView mMarkImage;
    private RoundedImageView mAvatarImage;
    private LinearLayout mModsContainer;

    private RankingSection mRankingSection;
    private DifficultyReCalculator mCalculator;

    private TrackInfo mTrack;
    private StatisticV2 mStats;

    //--------------------------------------------------------------------------------------------//

    public GameSummary() {
        super(Scenes.summary);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "gr";
    }

    @Override
    protected int getLayout() {
        return R.layout.summary_layout;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mTitleText = find("title");
        mArtistText = find("artist");
        mMapperText = find("mapper");
        mStarsText = find("stars");
        mDifficultyText = find("difficulty");

        mUsername = find("username");
        mAvatarImage = find("avatar");

        m300Text = find("300");
        m100Text = find("100");
        m50Text = find("50");
        m0Text = find("0");

        mMarkImage = find("markIv");
        mScoreText = find("score");

        mAccuracyText = find("accuracy");
        mComboText = find("combo");

        mModsContainer = find("mods");
        mDate = find("date");

        mPPText = find("pp");
        mURText = find("ur");
        mErrorText = find("error");

        if (mRankingSection == null) {
            mRankingSection = new RankingSection(this);
        }

        if (mTrack != null && mStats != null) {
            loadData();
        }
    }

    public void setData(TrackInfo track, StatisticV2 stats) {
        this.mTrack = track;
        this.mStats = stats;
    }

    public void loadData() {
        if (mTrack == null || mStats == null || !isAdded()) {
            return;
        }

        mCalculator = new DifficultyReCalculator();

        Game.activity.runOnUiThread(() -> {
            loadTrackData(mTrack, mStats);

            int totalScore = mStats.getModifiedTotalScore();
            if (totalScore == 0) {
                totalScore = mStats.getAutoTotalScore();
            }

            mScoreText.setText(new DecimalFormat("###,###,###,###").format(totalScore));

            m300Text.setText(mStats.getHit300() + "x");
            m100Text.setText(mStats.getHit100() + "x");
            m50Text.setText(mStats.getHit50() + "x");
            m0Text.setText(mStats.getMisses() + "x");

            mComboText.setText(mStats.getMaxCombo() + "x");
            mAccuracyText.setText(String.format("%.2f%%", mStats.getAccuracy() * 100f));

            mMarkImage.setImageBitmap(Game.bitmapManager.get("ranking-" + mStats.getMark()));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            mDate.setText("Played on " + sdf.format(new Date(mStats.getTime())));
            mUsername.setText(mStats.getPlayerName());

            loadPerformanceStatistics(mTrack, mStats);
        });
    }

    private void loadTrackData(TrackInfo track, StatisticV2 stats) {

        mTitleText.setText(BeatmapHelper.getTitle(track));
        mArtistText.setText(BeatmapHelper.getArtist(track));
        mMapperText.setText(track.getBeatmap().getCreator());
        mDifficultyText.setText(track.getMode());

        float cs = mCalculator.getCS(stats, track);
        float sr = mCalculator.recalculateStar(track, cs, stats.getSpeed());

        mStarsText.setText("" + sr);
    }

    private void loadPerformanceStatistics(TrackInfo track, StatisticV2 stats) {
        mCalculator.calculatePP(stats, track);

        mPPText.setText(String.format("%.2f", mCalculator.getTotalPP()));

        if (stats.getUnstableRate() <= 0) {
            find("urLayout").setVisibility(View.GONE);
            find("errorLayout").setVisibility(View.GONE);
            return;
        }

        mURText.setText(String.format("%.2f", stats.getUnstableRate()));
        mErrorText.setText(String.format("%.2fms - %.2fms", stats.getNegativeHitError(), stats.getPositiveHitError()));
    }

    //--------------------------------------------------------------------------------------------//

    public void retrieveOnlineData() {
        Game.activity.runOnUiThread(mRankingSection::retrieveOnlineData);
    }

    public void updateOnlineData(boolean success) {
        Game.activity.runOnUiThread(() ->
                mRankingSection.updateOnlineData(success)
        );
    }

    //--------------------------------------------------------------------------------------------//

    private enum Difference {
        Positive(0x4D59B32D, 0xFFBBFF99),
        Negative(0x4DB32D2D, 0xFFFF9999);

        final int backgroundColor;
        final int textColor;

        Difference(int backgroundColor, int textColor) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }
    }

    //--------------------------------------------------------------------------------------------//

    private class RankingSection {

        private final TextView
                mapRankText,
                rankText,
                accuracyText,
                scoreText;

        // Overall
        private long score, rank;
        private float accuracy;

        //----------------------------------------------------------------------------------------//

        public RankingSection(GameSummary parent) {

            mapRankText = parent.find("mapRank");
            rankText = parent.find("overallRank");
            scoreText = parent.find("overallScore");
            accuracyText = parent.find("overallAccuracy");
        }

        //----------------------------------------------------------------------------------------//

        private void applyColoring(TextView text, Difference difference) {
            Drawable background = Res.drw(R.drawable.shape_rounded).mutate();
            background.setTint(difference.backgroundColor);

            text.setTextColor(difference.textColor);
            text.setBackground(background);
        }

        //----------------------------------------------------------------------------------------//

        public void retrieveOnlineData() {
            score = Game.onlineManager.getScore();
            accuracy = GameHelper.Round(Game.onlineManager.getAccuracy() * 100f, 2);
            rank = Game.onlineManager.getRank();

            if (isAdded()) {
                find("ranking").setVisibility(View.VISIBLE);
                find("rankingStats").setAlpha(0f);
            }
        }

        public void updateOnlineData(boolean success) {
            if (!success) {
                find("rankingStats").setAlpha(0f);
                find("rankingFail").setAlpha(1f);
                return;
            }

            long newRank = Game.onlineManager.getRank();
            long newScore = Game.onlineManager.getScore();
            float newAccuracy = Game.onlineManager.getAccuracy();

            mapRankText.setText("#" + Game.onlineManager.getMapRank());

            if (newScore > score) {
                applyColoring(mapRankText, Difference.Positive);
            }

            String string = "";

            if (newRank < rank) {
                string = "\n(+" + (rank - newRank) + ")";
                applyColoring(rankText, Difference.Positive);
            }
            if (newRank > rank) {
                string = "\n(" + (rank - newRank) + ")";
                applyColoring(rankText, Difference.Negative);
            }
            rankText.setText("#" + newRank + string);

            string = "";
            if (newAccuracy < accuracy) {
                string = "\n(" + (newAccuracy - accuracy) + "%)";
                applyColoring(accuracyText, Difference.Negative);
            }
            if (newAccuracy > accuracy) {
                string = "\n(+" + (newAccuracy - accuracy) + "%)";
                applyColoring(accuracyText, Difference.Positive);
            }
            accuracyText.setText(newAccuracy + "%" + string);

            DecimalFormat df = new DecimalFormat("###,###,###,###,###");
            string = "";

            if (newScore < score) {
                string = "\n(" + df.format(newScore - score) + ")";
                applyColoring(scoreText, Difference.Negative);
            } else if (newScore > score) {
                string = "\n(+" + df.format(newScore - score) + ")";
                applyColoring(scoreText, Difference.Positive);
            }
            scoreText.setText(df.format(newScore) + string);

            AnimationTable.fadeOutScaleOut(find("rankingLoading"));
            find("rankingStats").setAlpha(1f);
        }
    }
}
