package com.nit.vicky;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.*;
import android.widget.*;
import com.nit.async.DeckTask;
import com.nit.libanki.*;
import com.nit.libanki.Collection;
import com.nit.themes.StyledDialog;
import com.nit.themes.Themes;
import com.nit.widget.ScaleImageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.origamilabs.library.views.StaggeredGridView;
import org.json.JSONException;

import java.util.*;

/**
 * Created by gengke on 14-3-19.
 */
public class QuizActivity extends AnkiActivity {
    private TextView mTextBarRed;
    private TextView mTextBarBlack;
    private TextView mTextBarBlue;
    private Chronometer mCardTimer;

    private LinearLayout mProgressBars;
    private View mSessionProgressTotalBar;
    private View mSessionProgressBar;

    private StaggeredGridView listView;
    private ImageAdapter mAdapter = new ImageAdapter();
    private Sched mSched;
    private String mBaseUrl;
    private List<String> imageUrls = new ArrayList<String>();
    private Collection mCol;

    public static final int EASE_FAILED = 1;
    public static final int EASE_HARD = 2;
    public static final int EASE_MID = 3;
    public static final int EASE_EASY = 4;
    public static final int EASE_SKIP = 5;

    private int mStatisticBarsMax;
    private int mStatisticBarsHeight;

    private int tryCount = 3;
    private int showCount = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Themes.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz);

        SharedPreferences prefs = AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance().getBaseContext());
        showCount = prefs.getInt("showCount", 10);

        // The hardware buttons should control the music volume while reviewing.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mTextBarRed = (TextView) findViewById(R.id.red_number);
        mTextBarBlack = (TextView) findViewById(R.id.black_number);
        mTextBarBlue = (TextView) findViewById(R.id.blue_number);

            mSessionProgressTotalBar = (View) findViewById(R.id.daily_bar);
            mSessionProgressBar = (View) findViewById(R.id.session_progress);
            mProgressBars = (LinearLayout) findViewById(R.id.progress_bars);

        mCardTimer = (Chronometer) findViewById(R.id.card_time);

        listView = (StaggeredGridView) findViewById(R.id.gridview);
        listView.setColumnCount(showCount/2);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new StaggeredGridView.OnItemClickListener() {
            @Override
            public void onItemClick(StaggeredGridView parent, View view, int position, long id) {
                if (mCurrentURL.equals(imageUrls.get(position))){

                    Sound.playOkSound(parent.getContext().getAssets(), new Sound.PlayAllCompletionListener(0) {
                        public void onCompletion(MediaPlayer mp) {
                            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_ANSWER_CARD, mRenderCardHandler, new DeckTask.TaskData(mSched,
                                    mCurrentCard, EASE_HARD));
                        }
                    });

                } else {
                    Sound.playErrSound(parent.getContext().getAssets(), new Sound.PlayAllCompletionListener(0) {
                        public void onCompletion(MediaPlayer mp) {
                            if (tryCount != 1) {
                                tryCount--;
                                return;
                            }

                            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_ANSWER_CARD, mRenderCardHandler, new DeckTask.TaskData(mSched,
                                    mCurrentCard, tryCount));
                        }
                    });
                }
            }
        });

        mCol = AnkiDroidApp.getCol();
        if (mCol == null) {
            return;
        } else {
            mSched = mCol.getSched();

            mBaseUrl = Utils.getBaseUrl(mCol.getMedia().getDir());
        }
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_ANSWER_CARD, mRenderCardHandler, new DeckTask.TaskData(mSched,
                null, 0));

        initTimer();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_quiz, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showhint:
                showHint();
                return true;
            case R.id.playsound:
                playSound();
                return true;

            case R.id.nextcard:
                DeckTask.launchDeckTask(DeckTask.TASK_TYPE_ANSWER_CARD, mRenderCardHandler, new DeckTask.TaskData(mSched,
                        mCurrentCard, EASE_SKIP));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHint() {

        StyledDialog.Builder builder = new StyledDialog.Builder(QuizActivity.this);
        builder.setTitle("提示");
        builder.setIcon(R.drawable.ic_dialog_alert);
        builder.setMessage(mCurrentCard.note().getmData());
        builder.setPositiveButton("关闭",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.show();
    }

    public class ImageAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return imageUrls.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;

            if (convertView == null) {
                LayoutInflater layoutInflator = LayoutInflater.from(parent.getContext());
                convertView = layoutInflator.inflate(R.layout.quiz_grid_image, null);
                holder = new ViewHolder();
                holder.imageView = (ScaleImageView) convertView.findViewById(R.id.img_thumb);
                holder.contentView = (TextView) convertView.findViewById(R.id.img_size);
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ImageLoader.getInstance().displayImage(imageUrls.get(position), holder.imageView, new SimpleImageLoadingListener(){
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    if (position == showCount - 1) playSound();
                }
            });

            holder.contentView.setVisibility(View.INVISIBLE);

            return convertView;
        }

        class ViewHolder {
            public ScaleImageView imageView;
            TextView contentView;
        }
    }

    private Card mCurrentCard;

    private String mCurrentURL;
    private DeckTask.TaskListener mRenderCardHandler = new DeckTask.TaskListener() {
        private boolean mNoMoreCards;

        @Override
        public void onPreExecute() {

        }

        @Override
        public void onPostExecute(DeckTask.TaskData result) {
            tryCount = 3;

            if (!result.getBoolean()) {
                // RuntimeException occured on answering cards
                closeQuiz(DeckPicker.RESULT_DB_ERROR, false);
                return;
            }
            // Check for no more cards before session complete. If they are both true, no more cards will take
            // precedence when returning to study options.
            if (mNoMoreCards) {
                closeQuiz(Reviewer.RESULT_NO_MORE_CARDS, true);
                return;
            }

            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_SEARCH_CARDS, mGetCardsHandler, new DeckTask.TaskData(
                    new Object[] { mCol, new HashMap<String, String>(), "", "" }));
        }

        @Override
        public void onProgressUpdate(DeckTask.TaskData... values) {
            updateForNewCard();

            imageUrls.clear();
            mAdapter.notifyDataSetChanged();

            mCurrentCard = values[0].getCard();

            if (mCurrentCard == null) {
                // If the card is null means that there are no more cards scheduled for review.
                mNoMoreCards = true;
            } else {
                mCurrentURL = mBaseUrl + Uri.encode(mCurrentCard.getQuestion(true).split("'")[1]);
                imageUrls.add(mCurrentURL);
            }

            // Since reps are incremented on fetch of next card, we will miss counting the
            // last rep since there isn't a next card. We manually account for it here.
            if (mNoMoreCards) {
                mSched.setReps(mSched.getReps() + 1);
            }
        }
    };

    private void closeQuiz(int result, boolean b) {

        QuizActivity.this.setResult(result);
        finish();
    }

    private void playSound() {
        Sound.resetSounds();
        Sound.parseSounds(mBaseUrl, mCurrentCard.getAnswer(true), false, 0);
        Sound.playSounds(0);
    }

    private DeckTask.TaskListener mGetCardsHandler = new DeckTask.TaskListener() {
        @Override
        public void onPreExecute() {

        }

        @Override
        public void onPostExecute(DeckTask.TaskData result) {

        }

        @Override
        public void onProgressUpdate(DeckTask.TaskData... values) {

            ArrayList<HashMap<String, String>> mCards = new ArrayList<HashMap<String, String>>();
            ArrayList<Integer> nums = new ArrayList<Integer>();
            Random rd = new Random();
            do {
                Integer num = rd.nextInt(values[0].getCards().size());
                if (!nums.contains(num)) nums.add(num);
            } while (nums.size() < showCount);

            for(Integer num : nums) {
                mCards.add(values[0].getCards().get(num));
            }

            for(HashMap<String, String> map : mCards) {
                long cardId = Long.parseLong(map.get("id"));
                Card card = mCol.getCard(cardId);
                String imgURL = getImgURL(card);
                if (!imageUrls.contains(imgURL))    imageUrls.add(imgURL);
            }

            Collections.shuffle(imageUrls);

            if (imageUrls.size() > showCount) {
                int rid = 0;
                int lid = imageUrls.indexOf(mCurrentURL);
                while (rid == lid) {
                    rid = rd.nextInt(showCount);
                }

                imageUrls.remove(rid);
            }



            mAdapter.notifyDataSetChanged();
        }
    };

    private String getImgURL(Card card) {
        String imgURL = "error.jpg";
        String[] img = card.getQuestion(true).split("'");
        if (img.length > 1)  imgURL = mBaseUrl + Uri.encode(img[1]);

        return imgURL;
    }

    private void updateForNewCard() {
        updateScreenCounts();

        updateStatisticBars();

    }

    private void updateScreenCounts() {
        if (mCurrentCard == null) {
            return;
        }

        try {
            String[] title = mSched.getCol().getDecks().get(mCurrentCard.getDid()).getString("name").split("::");
            //AnkiDroidApp.getCompat().setTitle(this, title[title.length - 1], mInvertedColors);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        int[] counts = mSched.counts(mCurrentCard);

        int eta = mSched.eta(counts, false);
        //AnkiDroidApp.getCompat().setSubtitle(this, getResources().getQuantityString(R.plurals.reviewer_window_title, eta, eta), mInvertedColors);

        SpannableString newCount = new SpannableString(String.valueOf(counts[0]));
        SpannableString lrnCount = new SpannableString(String.valueOf(counts[1]));
        SpannableString revCount = new SpannableString(String.valueOf(counts[2]));
//        if (mPrefHideDueCount) {
//            revCount = new SpannableString("???");
//        }

        switch (mCurrentCard.getQueue()) {
            case Card.TYPE_NEW:
                newCount.setSpan(new UnderlineSpan(), 0, newCount.length(), 0);
                break;
            case Card.TYPE_LRN:
                lrnCount.setSpan(new UnderlineSpan(), 0, lrnCount.length(), 0);
                break;
            case Card.TYPE_REV:
                revCount.setSpan(new UnderlineSpan(), 0, revCount.length(), 0);
                break;
        }

        mTextBarRed.setText(newCount);
        mTextBarBlack.setText(lrnCount);
        mTextBarBlue.setText(revCount);
    }

    private void updateStatisticBars() {
        if (mStatisticBarsMax == 0) {
            View view = findViewById(R.id.progress_bars_back1);
            mStatisticBarsMax = view.getWidth();
            mStatisticBarsHeight = view.getHeight();
        }
        float[] progress = mSched.progressToday(null, mCurrentCard, false);
        Utils.updateProgressBars(mSessionProgressBar,
                (int) (mStatisticBarsMax * progress[0]), mStatisticBarsHeight);
        Utils.updateProgressBars(mSessionProgressTotalBar,
                (int) (mStatisticBarsMax * progress[1]), mStatisticBarsHeight);
    }

    private void initTimer() {
        mCardTimer.setBase(SystemClock.elapsedRealtime());
        mCardTimer.start();
    }
}

