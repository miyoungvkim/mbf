package creativeLab.samsung.mbf.mbf;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.TextureView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowObjectDetectionAPIModel;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.tracking.MultiBoxTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.utils.MBFLog;

import static com.google.android.exoplayer2.Player.STATE_ENDED;

public class MBFAIController {
    private static final String TAG = MBFAIController.class.getSimpleName();
    private String videoUrl = null;
    private Context context = null;
    private FullscreenVideoView videoView = null;
    private SimpleExoPlayer player = null;
    private SimpleExoPlayerView playerView = null;
    private long finalDuration, currentVideoPosition = 0;
    private long currentReactionTime, nextReactionTime = 0;
    private int [] reactionTime = null;
    private String [] reactionMent = null;
    private int reactionIndex = 0;
    private Handler videoFrameHandler = null;
    private Handler exoPlayerHandler = null;
    private int currentStatus = MBFInfo.MBF_NO_DATA;
    private boolean needRearangeCurrentReaction = false;
    private int totalReactionNum = 0;
    private String keyWord = null;
    private boolean isFinalMent = false;
    static String selectedWaveFilePath = null;
    static String selectedMention= null;
    int currentVolume = 0;
    //AudioManager audioManagerMBF = null;

    private static int KEYWORD_DURATION_SEC = 10;

    public MBFAIController(Context c, Handler vfh, Handler evh, SimpleExoPlayer sp) {
        this.player = sp;
        this.context = c;
        this.videoFrameHandler = vfh;
        this.exoPlayerHandler = evh;
    }

    /*geonhui83.lee*/
    /*tensorflow speech recognition*/
    /*tensorflow object detection declaraion*/
    private static final int TF_OD_API_INPUT_SIZE = 300;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    /*speech recognition*/
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 5000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    //objectDetectModelDir + "/all_animation_label_map.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/q_wavenet_mobile.pb";
    private static final String INPUT_DATA_NAME = "Placeholder:0";
    private static final String OUTPUT_SCORES_NAME = "output";
    private static String[] SCENE_CATEGORY_CLASSES;
    private final Logger logger = new Logger();
    File sdCard = Environment.getExternalStorageDirectory();
    String objectDetectModelDir = sdCard.getAbsolutePath() + "/tensorflow_mobile";
    Bitmap origBitmap = null;
    //File sdcard = Environment.getExternalStorageDirectory();
    String sceneCategoryModelDir = sdCard.getAbsolutePath() + "/caffe_mobile";
    //String modelDir = "/sdcard/caffe_mobile/bvlc_reference_caffenet";
    String modelProto = sceneCategoryModelDir + "/deploy_googlenet_places365.prototxt";
    String modelBinary = sceneCategoryModelDir + "/googlenet_places365.caffemodel";
    String sceneLabel = "categories_places365.txt";

    private String TF_OD_API_MODEL_FILE = "file:///android_asset/frozen_inference_graph.pb";
    //objectDetectModelDir + "/frozen_inference_graph.pb";
    private String TF_OD_API_LABELS_FILE = "file:///android_asset/all_animation_label_map.txt";
    private int cropSize;
    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;
    private Bitmap cropCopyBitmap;
    private long detectionTimeStamp;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private int playWidth;
    private int playHeight;
    private Classifier detector;
    private MultiBoxTracker tracker;
    private ArrayList <String> recognitionMBFKewordWithTime = null;

    /*tensorflow speech recognition*/
    //private TensorFlowSpeechRecognitionAPIModel speechRecog;
    private CaffeMobile caffeMobile;


    private MediaPlayer mediaPlayer = null;
    private MediaPlayer mediaInitialMentionTTSVoicePlayer = null;
    private MediaPlayer mediaTTSVoicePlayer = null;
    UtteranceProgressListener ttsUtteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
        }

        @Override
        public void onDone(String utteranceId) {
            MBFLog.d("UtteranceProgressListener onDone " + utteranceId);
        }

        @Override
        public void onError(String utteranceId) {
            MBFLog.d("UtteranceProgressListener onError " + utteranceId);
            setExoPlayState(true);
            //player.setPlayWhenReady(true);
            //player.getPlaybackState();
        }
    };

   /* MediaPlayer.OnCompletionListener mediaMBFAnimationEnding = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            setExoPlayState(true);
            setMBFPlayState(MBFInfo.MBF_STATE_CONTENTS_PLAY);
        }
    };*/

    public void setFinalDuration(long fd)
    {
        finalDuration = fd;
    }
    public void setCurrentStatus(int status)
    {
        currentStatus = status;
    }

    MediaPlayer.OnCompletionListener mediaTTSVoiceCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            MBFLog.d("mediaTTSVoiceCompletionListener end");
            //player.setPlayWhenReady(true);
            //player.getPlaybackState();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setMBFPlayState(MBFInfo.MBF_STATE_MBF_PLAY);
            //isFinalMent = false;
            //mediaTTSVoicePlayer.setOnCompletionListener(mediaMBFAnimationEnding);
        }
    };

    MediaPlayer.OnCompletionListener mediaPlayerCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {

            String reactionMent = mbfInfo.getReactionMent();
            //mbfttsStart();
            mbfTTSWaveFileRandomPlayAssets();
            /*if (reactionMent != null && reactionMent.length() > 0) {
                // tts.speak(reactionMent);
                //mbfttsStart();
            } else {
                MBFLog.e("[error!!] no reaction Ment...." + reactionMent);
            }*/
        }
    };
    MediaPlayer.OnCompletionListener mediaFinalTTSCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mbfFinalttsStart();
        }
    };

    MediaPlayer.OnCompletionListener mediaInitialMentionTTSCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mbfttsStart();
        }
    };
    public void setReactionArrange()
    {
        needRearangeCurrentReaction = true;
    }
    public void start(AssetManager assetManager, String subTitleURL, SimpleExoPlayer p, SimpleExoPlayerView sepv) {
        mbfInfo = new MBFAIDataBase(context);
        initDetector(assetManager);
        //initSceneCategorization(assetManager);
        //initSpeechRecognition(assetManager);
        initKewordRecognition(subTitleURL);

        player = p;
        playerView = sepv;
        mediaTTSVoicePlayer = new MediaPlayer();

        //audioManagerMBF = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if(player == null || playerView == null)
        {
            Log.i(TAG, "exoplayer is not working!");
            return;
        }else{
            //finalDuration = player.getDuration();
            startMBFProcess();
        }
        currentStatus = MBFInfo.MBF_STATE_CONTENTS_PLAY;
        /*geonhui83.lee*/
    }

    public void stop() {
        stopMBFProcess();
    }

    private void stopMBFProcess() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
        if (tts != null) {
            tts.shutdown();
        }
        tts = null;

        if(mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer =null;
        }

        if(mediaTTSVoicePlayer != null)
        {
            mediaTTSVoicePlayer.stop();
            mediaTTSVoicePlayer.release();
            mediaTTSVoicePlayer= null;
        }

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void startMBFProcess() {
        //reactionIndex = 0;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    int ret = MBFInfo.MBF_ERROR;
                    if(player.getPlaybackState() == STATE_ENDED)
                    {
                        reactionIndex = 0;
                        nextReactionTime = reactionTime[reactionIndex];
                    }

                    currentVideoPosition = player.getCurrentPosition();
                    if (currentVideoPosition > (finalDuration-1000) && finalDuration > 0 && isFinalMent == false) {
                        isFinalMent = true;
                        video_pause_and_play_mbf_for_demo_sound_play(null,null,"final");
                        //return;
                    }

                    if(mbfInfo.getKewordFinishedFlag() == true && currentReactionTime == 0 && reactionTime == null)
                    {
                        ArrayList <String> reactions = mbfInfo.getVoiceKeywordList();
                        reactionTime = new int[reactions.size()];
                        reactionMent = new String[reactions.size()];

                        int currentReactionTime = 0;
                        for(String reaction : reactions)
                        {
                            String [] time = reaction.split(":");
                            int tempTime = Integer.parseInt(time[0]);
                            tempTime = tempTime + 2;

                            if(tempTime - currentReactionTime > KEYWORD_DURATION_SEC)
                            {
                                reactionTime[totalReactionNum] = tempTime*1000;
                                reactionMent[totalReactionNum] = time[1];
                                currentReactionTime = tempTime;
                                totalReactionNum++;
                            }
                        }
                        reactionTime[totalReactionNum] = reactionTime[totalReactionNum-1] - 2000;
                        nextReactionTime = reactionTime[reactionIndex];
                    }

                    if(needRearangeCurrentReaction == true)
                    {
                        for(int i = 0; i < totalReactionNum; i++)
                        {
                            if(reactionTime[i] > currentVideoPosition) {
                                reactionIndex = i;
                                nextReactionTime = reactionTime[reactionIndex];
                                needRearangeCurrentReaction = false;
                                break;
                            }
                        }
                    }

                    if(currentVideoPosition - nextReactionTime > 0 && currentVideoPosition - nextReactionTime < 1000)
                    {
                        reactionIndex++;
                        nextReactionTime = reactionTime[reactionIndex];
                        //Log.d(TAG, "in If current position : " + currentVideoPosition + "   next reaction position : " + nextReactionTime);
                        if (reactionTime != null && currentStatus == MBFInfo.MBF_STATE_CONTENTS_PLAY && reactionIndex < totalReactionNum)
                        {
                            TextureView textureView = (TextureView) playerView.getVideoSurfaceView();
                            Bitmap bitmap = textureView.getBitmap();

                            final long startTime = SystemClock.uptimeMillis();
                            String results = getObjectDetectorMent(bitmap);
                            final long stopTime = SystemClock.uptimeMillis();
                            int tempIndex = reactionIndex-1;
                            video_pause_and_play_mbf_for_demo_sound_play(null, null, reactionMent[tempIndex]);
                            //Log.d(TAG,  "current Reaction!! : " +  reactionMent[reactionIndex]);
                        }else{
                            Log.d(TAG, "reaction Time array is null");
                        }

                    }else {
                        //Log.d(TAG, "else current position : " + currentVideoPosition + "     next reaction position : " + nextReactionTime);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error in mbf Controller " + e);
                }
                ;
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 0, 500);
        //////////////////////////////////////////////////////
    }

    /*public long getMediaTTSDuration() {
        return selectedWaveFilePath;
    }*/

    private void mbfTTSWaveFileRandomPlayAssets()
    {
/* 20181113 - tmp change...
        //long startTime = SystemClock.uptimeMillis();
        ArrayList <String> playWaves = mbfInfo.getReactionMentListFromDB(keyWord, "scriptFiles");
        //long stopTime = SystemClock.uptimeMillis();

        //long dur = stopTime - startTime;
        //MBFLog.d("geonhui83 getReactionMentListFromDB : " + dur);
        //startTime = SystemClock.uptimeMillis();
        int numWaves = playWaves.size();

        Random rand = new Random();
        int voice_index = rand.nextInt() % numWaves;
        if (voice_index < 0)
            voice_index = -voice_index;
        selectedWaveFilePath = playWaves.get(voice_index);
*/
        //stopTime = SystemClock.uptimeMillis();
        //dur = stopTime - startTime;
        //MBFLog.d("geonhui83 Random time : " + dur);
        //selectedWaveFilePath = "tts_voice_170_2.wav";

        MBFLog.d("selectedWaveFilePath " + selectedWaveFilePath);
        if(mediaTTSVoicePlayer != null)
        {
            mediaTTSVoicePlayer.stop();
            mediaTTSVoicePlayer.release();
            mediaTTSVoicePlayer = new MediaPlayer();
        }

        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor = assetManager.openFd("waves/" + selectedWaveFilePath);
            if(descriptor == null)
            {
                mediaTTSVoicePlayer.setOnCompletionListener(mediaTTSVoiceCompletionListener);
                return;
            }
            mediaTTSVoicePlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            mediaTTSVoicePlayer.prepare();
            //mediaTTSVoicePlayer.setVolume(1f, 1f);
            //mediaTTSVoicePlayer.setLooping(true);
            //startTime = SystemClock.uptimeMillis();
            //MBFLog.d("geonhui83 Setup Player time : " + (startTime-stopTime));
            mediaTTSVoicePlayer.setVolume(5.0f, 5.0f);
            mediaTTSVoicePlayer.start();
            mediaTTSVoicePlayer.setOnCompletionListener(mediaTTSVoiceCompletionListener);
        } catch (IOException e) {
            mediaTTSVoicePlayer.setOnCompletionListener(mediaTTSVoiceCompletionListener);
            e.printStackTrace();
        }
    }

    private void mbfMakeTTSInfo(){
        //long startTime = SystemClock.uptimeMillis();
        ArrayList <String> playWaves = mbfInfo.getReactionMentListFromDB(keyWord, "scriptFiles");
        //long stopTime = SystemClock.uptimeMillis();

        //long dur = stopTime - startTime;
        //MBFLog.d("geonhui83 getReactionMentListFromDB : " + dur);
        //startTime = SystemClock.uptimeMillis();
        int numWaves = playWaves.size();

        Random rand = new Random();
        int voice_index = rand.nextInt() % numWaves;
        if (voice_index < 0)
            voice_index = -voice_index;
        selectedWaveFilePath = playWaves.get(voice_index);

        ArrayList <String> playMention = mbfInfo.getVoiceMentionFromDB(selectedWaveFilePath, "ment");
        if(playMention.size() == 0) {
            selectedMention = "";
        } else {
            selectedMention= playMention.get(0);
        }
    }

    public static String getSelectedMention(){
        MBFLog.d("kmi getSelectedMention" + selectedMention);
        return selectedMention;
    }

    public void mbfttsStart() {
        String reactionMent2 = mbfInfo.getReactionMent2();
        MBFLog.d("kmi reactionMent2" + reactionMent2);

        /*int[] voice_id = {R.raw.tts_voice_32_1, R.raw.tts_voice_32_2, R.raw.tts_voice_32_3, R.raw.tts_voice_32_4, R.raw.tts_voice_32_5,
                R.raw.tts_voice_67_1, R.raw.tts_voice_67_2, R.raw.tts_voice_67_3, R.raw.tts_voice_74_1, R.raw.tts_voice_74_2, R.raw.tts_voice_74_3, R.raw.tts_voice_74_4, R.raw.tts_voice_170_1,
                R.raw.tts_voice_170_2, R.raw.tts_voice_170_3, R.raw.tts_voice_170_4, R.raw.tts_voice_195_1, R.raw.tts_voice_195_2, R.raw.tts_voice_195_3, R.raw.tts_voice_195_4, R.raw.tts_voice_217_1,
                R.raw.tts_voice_217_2, R.raw.tts_voice_217_3, R.raw.tts_voice_217_4, R.raw.tts_voice_217_5, R.raw.tts_voice_217_6,
                R.raw.tts_voice_320_1, R.raw.tts_voice_320_2, R.raw.tts_voice_320_3, R.raw.tts_voice_320_4, R.raw.tts_voice_487_1,
                R.raw.tts_voice_487_2, R.raw.tts_voice_487_3, R.raw.tts_voice_487_4, R.raw.tts_voice_487_5, R.raw.tts_voice_487_6};
        Random rand = new Random();
        int voice_index = rand.nextInt() % 35;
        if (voice_index < 0)
            voice_index = -voice_index;*/
        //MBFLog.d("kmi  voice_index " + voice_index + "mediaTTSVoicePlayer start");

        if(mediaInitialMentionTTSVoicePlayer != null)
        {
            mediaInitialMentionTTSVoicePlayer.stop();
            mediaInitialMentionTTSVoicePlayer.release();
            mediaInitialMentionTTSVoicePlayer =null;
        }

        mediaInitialMentionTTSVoicePlayer = MediaPlayer.create(context, R.raw.name10);
        mediaInitialMentionTTSVoicePlayer.setVolume(5.0f, 5.0f);
        mediaInitialMentionTTSVoicePlayer.start();
        mediaInitialMentionTTSVoicePlayer.setOnCompletionListener(mediaPlayerCompletionListener);
    }

    public void mbfFinalttsStart() {
        String reactionMent2 = mbfInfo.getReactionMent2();
        MBFLog.d("kmi reactionMent2" + reactionMent2);

        /*int[] voice_id = {R.raw.tts_voice_32_1, R.raw.tts_voice_32_2, R.raw.tts_voice_32_3, R.raw.tts_voice_32_4, R.raw.tts_voice_32_5,
                R.raw.tts_voice_67_1, R.raw.tts_voice_67_2, R.raw.tts_voice_67_3, R.raw.tts_voice_74_1, R.raw.tts_voice_74_2, R.raw.tts_voice_74_3, R.raw.tts_voice_74_4, R.raw.tts_voice_170_1,
                R.raw.tts_voice_170_2, R.raw.tts_voice_170_3, R.raw.tts_voice_170_4, R.raw.tts_voice_195_1, R.raw.tts_voice_195_2, R.raw.tts_voice_195_3, R.raw.tts_voice_195_4, R.raw.tts_voice_217_1,
                R.raw.tts_voice_217_2, R.raw.tts_voice_217_3, R.raw.tts_voice_217_4, R.raw.tts_voice_217_5, R.raw.tts_voice_217_6,
                R.raw.tts_voice_320_1, R.raw.tts_voice_320_2, R.raw.tts_voice_320_3, R.raw.tts_voice_320_4, R.raw.tts_voice_487_1,
                R.raw.tts_voice_487_2, R.raw.tts_voice_487_3, R.raw.tts_voice_487_4, R.raw.tts_voice_487_5, R.raw.tts_voice_487_6};
        Random rand = new Random();
        int voice_index = rand.nextInt() % 35;
        if (voice_index < 0)
            voice_index = -voice_index;*/
        //MBFLog.d("kmi  voice_index " + voice_index + "mediaTTSVoicePlayer start");

        if(mediaInitialMentionTTSVoicePlayer != null)
        {
            mediaInitialMentionTTSVoicePlayer.stop();
            mediaInitialMentionTTSVoicePlayer.release();
            mediaInitialMentionTTSVoicePlayer =null;
        }

        mediaInitialMentionTTSVoicePlayer = MediaPlayer.create(context, R.raw.goodbye);
        mediaInitialMentionTTSVoicePlayer.start();
        mediaInitialMentionTTSVoicePlayer.setOnCompletionListener(mediaTTSVoiceCompletionListener);
    }

    public void video_pause_and_play_mbf_for_demo_sound_play(String ment, String reactionMention, String keyword) {
        MBFLog.d("video_pause_and_play_mbf_for_demo_sound_play start " + ment);


        setExoPlayState(false);
        //  RandomSelect();
        mbfInfo.setReactionMent(ment, reactionMention);
        keyWord = keyword;

        mbfMakeTTSInfo();

        MBFLog.d("selectedWaveFilePath " + selectedWaveFilePath + "selectedMention " + selectedMention);

        if(mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer =null;
        }

        setMBFPlayState(MBFInfo.MBF_STATE_MBF_READY);
        mediaPlayer = MediaPlayer.create(context, R.raw.sound_start);
        mediaPlayer.start();
        if(keyword.equals("final"))
        {
            mediaPlayer.setOnCompletionListener(mediaFinalTTSCompletionListener);
        }else{
            mediaPlayer.setOnCompletionListener(mediaInitialMentionTTSCompletionListener);
        }

        //}

    }

    void setMBFPlayState(int playState) {
        Message visible_message = new Message();
        visible_message.what = playState;
        currentStatus = playState;
        videoFrameHandler.sendMessage(visible_message);
    }

    void setExoPlayState(boolean playState) {
        Message visible_message = new Message();
        visible_message.obj = playState;
        //currentStatus = playState;
        exoPlayerHandler.sendMessage(visible_message);
    }

    private void initKewordRecognition(String subTitleURL)
    {
        //recognitionMBFKewordWithTime = new ArrayList<String>();
        mbfInfo.getKewordListFromURL(subTitleURL);

    }

    private void initDetector(AssetManager assetManager) {
        rgbFrameBitmap = null;
        croppedBitmap = null;
        cropCopyBitmap = null;
        lastProcessingTimeMs = 0;

        cropSize = TF_OD_API_INPUT_SIZE;
        detectionTimeStamp = 0;

        playWidth = 0;
        playHeight = 0;

        try {
            detector = TensorFlowObjectDetectionAPIModel.create(assetManager, TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            Log.e(TAG, "Exception initializing classifier!", e);
        }
        tracker = new MultiBoxTracker(context);
    }

    public List<Classifier.Recognition> getObjectDetectorResult(Bitmap origBitmap)
    {
        MBFLog.d("KMI getObjectDetectorResult start");
        final List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();

        rgbFrameBitmap = Bitmap.createBitmap(origBitmap.getWidth(), origBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(origBitmap.getWidth(), origBitmap.getHeight(), cropSize, cropSize, 0, false);
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(origBitmap, frameToCropTransform, null);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);


        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                //logger.i(TAG, "geonhui83.lee %s : %d, %d, %d, %d" + result.getTitle(),location.left, location.right, location.bottom, location.top);
                MBFLog.d("geonhui83.lee " + result.getTitle() + location.left + location.right + location.bottom + location.top);

                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
            }
            //tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
        }

        return mappedRecognitions;
    }

    public String getObjectDetectorMent(Bitmap origBitmap) {
        MBFLog.d("KMI getObjectDetectorMent start");
        String ret = " ";
        String debugStr = "";
        int chracterMaxNum = 3;
        String characterObject[] = new String[chracterMaxNum];

        rgbFrameBitmap = Bitmap.createBitmap(origBitmap.getWidth(), origBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(origBitmap.getWidth(), origBitmap.getHeight(), cropSize, cropSize, 0, true);
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(origBitmap, frameToCropTransform, null);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        final long startTime = SystemClock.uptimeMillis();
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
        final long stopTime = SystemClock.uptimeMillis();
        final long dur = stopTime - startTime;

        final List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();
        int numObject = 0;

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= 0.3) {
                //logger.i(TAG, "geonhui83.lee %s : %d, %d, %d, %d" + result.getTitle(),location.left, location.right, location.bottom, location.top);
                MBFLog.d("geonhui83.lee " + result.getTitle(), location.left, location.right, location.bottom, location.top);

                // kmi add for 1st demo (2018.09.06)
                characterObject[numObject] = result.getTitle();
                if (numObject > chracterMaxNum)
                    break;
                //
                numObject++;
                debugStr += result.getTitle() + "    " + result.getConfidence() + "\n";
                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
            }
            //tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
        }

        // kmi add for 1st demo (2018.09.06)
        if (numObject == 1) {
            ret = "우와~" + characterObject[0] + "다~!";
        } else if (numObject > 1) {
            ret = "우와~" + characterObject[0] + "와" + characterObject[1] + "다~!";
        } else if (numObject > 2) {
            ret = "우와~" + characterObject[0] + "와 친구들이다~!";
        }
        //

        return ret;
    }


    private void initSceneCategorization(AssetManager assetManager) {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");

        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(1);
        caffeMobile.loadModel(modelProto, modelBinary);

        float[] meanValues = {104, 117, 123};
        caffeMobile.setMean(meanValues);

        AssetManager am = assetManager;
        try {
            InputStream is = am.open("categories_places365.txt");
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            SCENE_CATEGORY_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Timer timer;
    private long timer_called_sec = 0;
    private TimerTask timerTask;
    private MBFAIDataBase mbfInfo;
    private TextToSpeechTask tts = null;

    public String getSceneCategory(Bitmap origBitmap) {
        String ret = "";
        String sceneID = "";
        String tempJPEGFileName = sceneCategoryModelDir + "/tempJPG.jpg";
        try {
            FileOutputStream outStream = new FileOutputStream(tempJPEGFileName);
            origBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
            //img.compressToJpeg(rect_rsz, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int sceneClass = caffeMobile.predictImage(tempJPEGFileName)[0];
        sceneID = SCENE_CATEGORY_CLASSES[sceneClass];
        //
        // need to mention for scean explain
        ret = mbfInfo.getSceneTextFromDB(sceneID);
        //
        return ret;
    }

    public String getSceneCategoryMent(Bitmap origBitmap) {
        String ret = "";
        String sceneID = "";
        String tempJPEGFileName = sceneCategoryModelDir + "/tempJPG.jpg";
        try {
            FileOutputStream outStream = new FileOutputStream(tempJPEGFileName);
            origBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
            //img.compressToJpeg(rect_rsz, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int sceneClass = caffeMobile.predictImage(tempJPEGFileName)[0];
        sceneID = SCENE_CATEGORY_CLASSES[sceneClass];
        //
        // need to mention for scean explain
        ret = mbfInfo.getSceneTextFromDB(sceneID);
        ret += "이다~!";
        //
        return ret;
    }
}
