package creativeLab.samsung.mbf.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.utils.AnimationInfo;

import static creativeLab.samsung.mbf.utils.json.AssetJSONFile;

public class EpisodeCharacterActivity extends AppCompatActivity {
    private static final String TAG = EpisodeCharacterActivity.class.getSimpleName();

    private RecyclerView recyclerView;
    private EpisodeListAdapter adapter;
    private List<AnimationInfo> episodeInfoList;
    private View decorView;
    private int uiOption;
    private ImageButton btnSetting;
    private ImageButton btnBack;
    private String selectedCategoryID;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(uiOption);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        decorView = getWindow().getDecorView();
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        uiOption = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        setContentView(R.layout.activity_episode_character);
        recyclerView = findViewById(R.id.recycler_view);
        btnSetting = findViewById(R.id.btn_setting);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFingerPrintScreen();
            }
        });

        btnBack = findViewById(R.id.btn_Back);
        btnBack.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        selectedCategoryID = this.getIntent().getStringExtra("CATEGORY_ID");
        if(selectedCategoryID == null) {
            Log.e(TAG, "selectedCategoryID == null ");
            selectedCategoryID = "pororo"; // default animation categoryID
        }

        if(selectedCategoryID.equals("poli"))
        {
            TextView charText = (TextView)findViewById(R.id.txt_character_name);
            charText.setText("로보카폴리");
            int textColor = getResources().getColor(R.color.white);
            charText.setTextColor(textColor);

            LinearLayout RLayout = (LinearLayout)findViewById(R.id.bg_layout_group);
            int bgColor = getResources().getColor(R.color.cerulean_blue);
            RLayout.setBackgroundColor(bgColor);

            ImageView IView = (ImageView)findViewById(R.id.characterImage);
            //Drawable characterImage = getResources().getDrawable(R.drawable.thumbnail_poli);
            IView.setImageResource(R.drawable.character_poli);

            ImageView BView = (ImageView)findViewById(R.id.backgroundImage);
            BView.setBackground(getDrawable(R.drawable.character_bg_poli));
            //IView.setImageDrawable(characterImage);
        }else if(selectedCategoryID.equals("pororo")){
            TextView charText = (TextView)findViewById(R.id.txt_character_name);
            charText.setText("뽀로로");

            int textColor = getResources().getColor(R.color.black);
            charText.setTextColor(textColor);

            LinearLayout RLayout = (LinearLayout)findViewById(R.id.bg_layout_group);
            int bgColor = getResources().getColor(R.color.light_gold);
            RLayout.setBackgroundColor(bgColor);

            ImageView IView = (ImageView)findViewById(R.id.characterImage);
            //Drawable characterImage = getResources().getDrawable(R.drawable.thumbnail_poli);
            IView.setImageResource(R.drawable.thumbnail_pororo);

            ImageView BView = (ImageView)findViewById(R.id.backgroundImage);
            BView.setBackground(getDrawable(R.drawable.bg_blue));
            //IView.setImageDrawable(characterImage);
        }else if(selectedCategoryID.equals("frienzoo")){
            TextView charText = (TextView)findViewById(R.id.txt_character_name);
            charText.setText("프렌쥬");
            int textColor = getResources().getColor(R.color.white);
            charText.setTextColor(textColor);

            LinearLayout RLayout = (LinearLayout)findViewById(R.id.bg_layout_group);
            int bgColor = getResources().getColor(R.color.strawberry);
            RLayout.setBackgroundColor(bgColor);

            ImageView IView = (ImageView)findViewById(R.id.characterImage);
            //Drawable characterImage = getResources().getDrawable(R.drawable.thumbnail_poli);
            IView.setImageResource(R.drawable.character_frinedzoo);

            ImageView BView = (ImageView)findViewById(R.id.backgroundImage);
            BView.setBackground(getDrawable(R.drawable.character_bg_frinedzoo));
            //IView.setImageDrawable(characterImage);
        } else if(selectedCategoryID.equals("cloudbread")){
            TextView charText = (TextView)findViewById(R.id.txt_character_name);
            charText.setText("구름빵");
            int textColor = getResources().getColor(R.color.black);
            charText.setTextColor(textColor);

            LinearLayout RLayout = (LinearLayout)findViewById(R.id.bg_layout_group);
            int bgColor = getResources().getColor(R.color.light_gold);
            RLayout.setBackgroundColor(bgColor);

            ImageView IView = (ImageView)findViewById(R.id.characterImage);
            //Drawable characterImage = getResources().getDrawable(R.drawable.thumbnail_poli);
            IView.setImageResource(R.drawable.character_cloudbread);

            ImageView BView = (ImageView)findViewById(R.id.backgroundImage);
            BView.setBackground(getDrawable(R.drawable.character_bg_cloudbread));
            //IView.setImageDrawable(characterImage);
        } else if(selectedCategoryID.equals("banji")){
            TextView charText = (TextView)findViewById(R.id.txt_character_name);
            charText.setText("반지의 비밀일기");
            int textColor = getResources().getColor(R.color.white);
            charText.setTextColor(textColor);

            LinearLayout RLayout = (LinearLayout)findViewById(R.id.bg_layout_group);
            int bgColor = getResources().getColor(R.color.bubble_gum_pink);
            RLayout.setBackgroundColor(bgColor);

            ImageView IView = (ImageView)findViewById(R.id.characterImage);
            //Drawable characterImage = getResources().getDrawable(R.drawable.thumbnail_poli);
            IView.setImageResource(R.drawable.character_banji);

            ImageView BView = (ImageView)findViewById(R.id.backgroundImage);
            BView.setBackground(getDrawable(R.drawable.character_bg_banji));
            //IView.setImageDrawable(characterImage);
        } else if(selectedCategoryID.equals("woodang")){
            TextView charText = (TextView)findViewById(R.id.txt_character_name);
            charText.setText("우당탕탕 아이쿠");
            int textColor = getResources().getColor(R.color.white);
            charText.setTextColor(textColor);

            LinearLayout RLayout = (LinearLayout)findViewById(R.id.bg_layout_group);
            int bgColor = getResources().getColor(R.color.azure);
            RLayout.setBackgroundColor(bgColor);

            ImageView IView = (ImageView)findViewById(R.id.characterImage);
            //Drawable characterImage = getResources().getDrawable(R.drawable.thumbnail_poli);
            IView.setImageResource(R.drawable.character_woodang);

            ImageView BView = (ImageView)findViewById(R.id.backgroundImage);
            BView.setBackground(getDrawable(R.drawable.character_bg_woodang));
            //IView.setImageDrawable(characterImage);
        }

        episodeInfoList = setEpisodeList(selectedCategoryID);
        adapter = new EpisodeListAdapter(this, episodeInfoList);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(0), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
    }

    private void launchFingerPrintScreen() {
        // startActivity(new Intent(EpisodeCharacterActivity.this, SettingsActivity.class)); // FingerprintActivity.class));
        //finish();
    }

    private void launchSearchScreen() {
        startActivity(new Intent(EpisodeCharacterActivity.this, SearchActivity.class)); // FingerprintActivity.class));
        //finish();
    }

    private List<AnimationInfo> setEpisodeList(String category_id){
        List<AnimationInfo> InfoList = new ArrayList<>();

        try {
            String jsonFileLocation =  AssetJSONFile("data/episode_lists.json",this);
            JSONObject jsonMainObject = new JSONObject(jsonFileLocation);
            JSONArray jsonMainArray = jsonMainObject.getJSONArray("episode_list");

            for (int i = 0; i < jsonMainArray.length(); i++) {

                JSONObject jsonObject = (JSONObject) jsonMainArray.get(i);
                JSONArray jsonArray = jsonObject.getJSONArray(category_id);

                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONObject jsonSubObject = (JSONObject) jsonArray.get(j);
                    String id = jsonSubObject.getString("id");
                    String title = jsonSubObject.getString("title");
                    String file_name = jsonSubObject.getString("file_name");
                    String image = jsonSubObject.getString("thumbnail");
                    String contentsAddress = jsonSubObject.getString("contents_address");
                    String subtitleAddress = jsonSubObject.getString("subtitle_address");
                    int thumbnail_image = this.getResources().getIdentifier(image, "drawable", this.getPackageName());

                    AnimationInfo episodeInfo = new AnimationInfo();
                    episodeInfo.setSelected_categoryID(category_id);
                    episodeInfo.setID(id);
                    episodeInfo.setTitle(title);
                    episodeInfo.setThumbnail(thumbnail_image);
                    episodeInfo.setFileName(file_name);
                    episodeInfo.setEpisodeNum(i);
                    episodeInfo.setContentsAddres(contentsAddress);
                    episodeInfo.setSubtitleAddress(subtitleAddress);

                    InfoList.add(episodeInfo);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error IOException " + e);
        } catch (JSONException e) {
            Log.e(TAG, "error JSONException " + e);
        }
        return InfoList;
    }
    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    /**
     * RecyclerView item decoration - give equal margin around grid item
     */
    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }
}
