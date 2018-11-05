package creativeLab.samsung.mbf.mbf;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import creativeLab.samsung.mbf.mbf.extractor.AudioExtractor;
import creativeLab.samsung.mbf.utils.DBManager;
import creativeLab.samsung.mbf.utils.FileManager;
import creativeLab.samsung.mbf.utils.MBFLog;
import creativeLab.samsung.mbf.utils.UserInfo;

public class MBFAIDataBase {
    private static final String TAG = "MBFAIDataBase";

    public static final int MBF_SUCCESS = 0;
    public static final int MBF_ERROR = -1;
    public static final int MBF_NO_DATA = 1;

    public static final int MBF_STATE_CONTENTS_PLAY = 0;
    public static final int MBF_STATE_MBF_READY = 1;
    public static final int MBF_STATE_MBF_PLAY = 2;

    private static final String DATABASE_FILE_NAME = "mbfDB.db";
    private static final String DATABASE_TABLE_VOICE_KEYWORD = "voiceKeywordTable";
    private static final String DATABASE_TABLE_REACTION = "reactionTable";
    private static final String DATABASE_TABLE_SCENE = "sceneTable";
    private static final String DATABASE_PREDEFINED_STRING_NAME = "[Name_id]";

    private ArrayList<String> voiceKeywordList;
    private ArrayList<String> reactionMentList;
    private String reactionMent;
    private String reactionMent2 = "";
    private ArrayList<String> subTitleFromURLList;

    private int catchCount;
    private boolean isReactionUsed;
    private int videoCurrentPosition;
    private boolean isObjectDetected;

    private DBManager mbfDB;
    private Context context;
    private int playMin;
    private int playSec;
    private boolean isFirst = true;
    private boolean isCreateKewordFinished = false;

    public MBFAIDataBase(Context context) {
        this.context = context;

        if(context == null)
        {
            /*
               need new open DB on Server
            * */
        }else{
            mbfDB = new DBManager(context, DATABASE_FILE_NAME);
            boolean ret = mbfDB.dbFileCheck(DATABASE_FILE_NAME);
            if (ret == true) {
                mbfDB.dbRemove(DATABASE_FILE_NAME); // remove DB for reinstall
            }
            mbfDB.dbOpen();
        }
        subTitleFromURLList = new ArrayList<String>();
    }

    public void getSubTitleFromURLList(String fromURL) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(fromURL);
                    HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                    connection.setConnectTimeout(100000);

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    String str;
                    while ((str = in.readLine()) != null) {
                        //subTitleFromURLList.add(str);
                        createVoiceKeywordList(str);
                    }
                    in.close();
                    isCreateKewordFinished = true;
                }catch (Exception e){
                    Log.v(TAG, "fail open Subtitle from url" + fromURL);
                }
            }
        }).start();
        return;
    }

    public boolean getKewordFinishedFlag()
    {
        return isCreateKewordFinished;
    }

    public static String LogChecker(int num) {
        String ret = null;
        switch (num) {
            case MBF_ERROR: {
                ret = " error";
                break;
            }
            case MBF_NO_DATA:
                ret = " no data";
                break;
            case MBF_SUCCESS:
                ret = " success";
                break;
            default:
                ret = " error in Log checker input value";
                break;
        }
        return ret;
    }

    public void Init() {
        reactionMent2 = "";
        catchCount = 0;
        isReactionUsed = false;
        videoCurrentPosition = 0;
        isObjectDetected = false;

        playMin = 0;
        playSec = 0;
    }

    public ArrayList<String> getVoiceKeywordList() {
        if (voiceKeywordList != null && voiceKeywordList.size() > 0) {
            for (int i = 0; i < voiceKeywordList.size(); i++) {
                MBFLog.d(" [getVoiceKeywordList] voiceKeyword[" + i + "] = " + voiceKeywordList.get(i));
            }
        }
        return voiceKeywordList;
    }

    public void setVoiceKeywordList(ArrayList<String> voiceKeywordList) {
        this.voiceKeywordList = voiceKeywordList;
    }

    public void getKewordListFromURL(String subTitleURL) {
        voiceKeywordList = new ArrayList<String>();
        getSubTitleFromURLList(subTitleURL);
        /*for(String subStr : subTitleFromURLList)
        {
            createVoiceKeywordList(subStr);
        }*/
    }

    private void createVoiceKeywordList(String strSTT) {
        int ret = MBF_ERROR;
        //ArrayList<String> mVoiceKeywordList = new ArrayList<String>();
        String[] parsedString = strSTT.split(" ");
        String[] time = parsedString[0].split(":");

        int tempTime = 0;
        if (isFirst == true)
        {
            tempTime = Integer.parseInt(time[0].substring(1));
            isFirst = false;
        }else{
            tempTime = Integer.parseInt(time[0]);
        }
        int tempTime2 =  Integer.parseInt(time[1]);
        int timeSec = tempTime*60 + tempTime2;

        String resultStr = String.valueOf(timeSec);

        for (int i = 0; i < parsedString.length; i++) {

            String str = getVoiceKeywordFromDB(parsedString[i]);
            if (str != null) {
                resultStr = resultStr +":"+str;
                voiceKeywordList.add(resultStr);
            }
        }
    }

    public String getReactionMent() {
        if (reactionMentList != null && reactionMentList.size() > 0) {
            this.reactionMent = reactionMentList.get(0);
        }
        return reactionMent;
    }

    public String getReactionMent2() {
        return reactionMent2;
    }

    public void setReactionMent(String ment, String reactionMent2) {
        if (reactionMentList != null) {
            reactionMentList.clear();
            reactionMentList = null;
        }
        this.reactionMent = ment;
        this.reactionMent2 = reactionMent2;
        MBFLog.d("set reaction Ment = " + ment);
    }

    public void setReactionMent(String reactionMent) {
        if (reactionMentList != null) {
            reactionMentList.clear();
            reactionMentList = null;
        }
        this.reactionMent = reactionMent;
        MBFLog.d("set reaction Ment = " + reactionMent);
    }


    public ArrayList<String> getReactionMentList() {
        return reactionMentList;
    }

    public void setReactionMentList(ArrayList<String> reactionMentList) {
        this.reactionMentList = reactionMentList;
    }

    public int createReactionMentList(String strSTT) {
        int ret = MBF_ERROR;
        if (strSTT.length() <= 0) {
            ret = MBF_NO_DATA;
            return ret;
        }

        ArrayList<String> mReactionMentList = getReactionMentListFromDB(strSTT);
        if (mReactionMentList != null && mReactionMentList.size() > 0) {
            for (int i = 0; i < mReactionMentList.size(); i++) {
                String UserInfo_name = UserInfo.getUserName();
                if (UserInfo_name == null)
                    UserInfo_name = UserInfo.USER_DEFAULT_NAME;
                String replacedStr = mReactionMentList.get(i).replace(DATABASE_PREDEFINED_STRING_NAME, UserInfo_name);
                mReactionMentList.set(i, replacedStr);
                MBFLog.d("[Replaced str] reactionMentList[" + i + "] = " + mReactionMentList.get(i));
            }
            this.reactionMentList = mReactionMentList;
            ret = MBF_SUCCESS;
        } else {
            String defaultRecationStr = "우리  " + strSTT + "  라고 함께 말해 볼까?";
            mReactionMentList.add(defaultRecationStr);
            this.reactionMentList = mReactionMentList;
            ret = MBF_SUCCESS;
        }
        return ret;
    }

    public String getVoiceKeywordFromDB(String str) {
        if (str.length() <= 0) {
            return null;
        }

        String query = "SELECT * FROM " + DATABASE_TABLE_VOICE_KEYWORD + " WHERE keyword LIKE \'" + str + "%\'";
        Cursor cursor = mbfDB.getDBCusor(query);
        String res = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                res = cursor.getString(cursor.getColumnIndex("keyword"));
                break;
            } while (cursor.moveToNext());
        }
        cursor.close();

        MBFLog.d("[getVoiceKeywordFromDBgetKeyword Result " + res);
        return res;
    }

    public ArrayList<String> getReactionMentListFromDB(String voiceKeyword) {
        if (voiceKeyword.length() <= 0) {
            return null;
        }

        String query = "SELECT * FROM " + DATABASE_TABLE_REACTION + " WHERE voiceKeyword =\'" + voiceKeyword + "\'";
        Cursor cursor = mbfDB.getDBCusor(query);
        ArrayList<String> list = new ArrayList<String>();
        String res = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                res = cursor.getString(cursor.getColumnIndex("ment"));
                if (res.length() > 0) {
                    list.add(res);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public String getSceneTextFromDB(String str) {
        if (str.length() <= 0) {
            return null;
        }

        String query = "SELECT * FROM " + DATABASE_TABLE_SCENE + " WHERE sceneID LIKE \'" + str + "\'";
        Cursor cursor = mbfDB.getDBCusor(query);
        String res = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                res = cursor.getString(cursor.getColumnIndex("sceneNameKR"));
                break;
            } while (cursor.moveToNext());
        }
        cursor.close();

        MBFLog.d("[getSceneTextFromDB Result] " + res);
        return res;
    }

    public int getCatchCount() {
        return catchCount;
    }

    public void setCatchCount(int catchCount) {
        this.catchCount = catchCount;
    }

    public boolean getIsReactionUsed() {
        return isReactionUsed;
    }

    public void setIsReactionUsed(boolean isReactionUsed) {
        this.isReactionUsed = isReactionUsed;
    }

    public int getVideoCurrentPosition() {
        return videoCurrentPosition;
    }

    public void setVideoCurrentPosition(int videoCurrentPosition) {
        SimpleDateFormat time = new SimpleDateFormat("mm:ss:SS");
        String strVideoCurrentPosition = time.format(videoCurrentPosition);

        String[] parsedTimeInfo = strVideoCurrentPosition.split(":");

        this.videoCurrentPosition = videoCurrentPosition;
        this.playMin = Integer.parseInt(parsedTimeInfo[0]);
        this.playSec = Integer.parseInt(parsedTimeInfo[1]);
    }

    public int getPlayMin() {
        return playMin;
    }

    public int getPlaySec() {
        return playSec;
    }

    public boolean getIsObjectDetected() {
        return isObjectDetected;
    }

    public void setIsObjectDetected(boolean isObjectDetected) {
        this.isObjectDetected = isObjectDetected;
    }


    private void saveBitmapToFileCache(Bitmap bitmap, String outputFilename) {

        String dstMediaPath = FileManager.getExternalCacheFilePath(context, "image", outputFilename, "jpg");

        File fileCacheItem = new File(dstMediaPath);
        OutputStream out = null;
        try {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            MBFLog.e("error!!, faile to save Bitmap data as a File");
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                MBFLog.e("error!!, faile to save Bitmap data as a File" + e);
            }
        }
    }
    // Todo : add STT
    String getSpeechToTextFromAudioData(String ouputAudioPath) {
        String[] tmp_str = {"우리는 친구", "하지만 뽀로로는 아기 공룡을", "공룡에게 사과", "새내기 버스의 하루",
                "미안해 이제 조용히 할게", "내 이름은 타요야", "만나서 반가워"
                , "무서운 괴물이라고 생각합니다", "반가워", "함께 준비 해보자",
                "이 버스 타자", "뽀로로 같이 준비 해보자", "안녕 난 뽀로로야", "뭐하는 거야", "같이 가", "아이쿠"};
        String str = tmp_str[videoCurrentPosition % 10];
        return str;
    }
}