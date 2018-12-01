package com.example.nayeong.swucheck;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class Enrollment extends Activity {
    static final String HOST = "westus.api.cognitive.microsoft.com";
    static final String API_KEY ="58d7f66f03ae4e818c5bb1fcd3ee04b7";

    static final String RECORDED_FILE = "/sdcard/recorde.wav";
    WavRecorder wavRecorder = new WavRecorder("/sdcard/recorde.wav");
    File wavfile;

    //azure private user profile
    String userProfile;

    //phrase 저장
    ArrayList<HashMap<String,String>> arrayPhrase;

    //Enroll error code
    int responseCode;
    String enrollMessage;
    int remainingEnrollments = -1;

    ProgressDialog progress;
    phraseAdapter phrasesAdapter;
    ListView phraseList;
    Button recordBtn, recordStopBtn, enrollBtn;
    TextView enrollNum;
    int count;

    String ch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enrollment);

        Intent intent = getIntent();
        userProfile = intent.getStringExtra("userProfile");

        phraseList = (ListView) findViewById(R.id.phraseList);
        //phrase api 호출, 리스트 뷰 출력
        new downPhrase().execute(null,null);

        //권한 체크
        checkPermission();

        recordBtn = (Button)findViewById(R.id.recordBtn);
        recordStopBtn = (Button)findViewById(R.id.recordStopBtn);
        enrollBtn = (Button) findViewById(R.id.enrollBtn);


        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wavRecorder.startRecording();
                Toast.makeText(getApplicationContext(), ch, 2000).show();

            }
        });
        recordStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wavRecorder.stopRecording();
                Toast.makeText(getApplicationContext(), ch, 2000).show();
            }
        });

        enrollBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new downEnroll().execute(null,null);




            }
        });

    }
    /* 권한 체크 */
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 마시멜로우 버전과 같거나 이상이라면
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]
                                {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO},
                        3);  //마지막 인자는 체크해야될 권한 갯수

            }
        }
    }

    /* verificationPhrase를 이용하여 사용가능한 읽기 목록 불러오기 */
    class downPhrase extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... strings) {
            return connectPhrase();
            //return null;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(Enrollment.this);
            progress.show();
            progress.setMessage("등록문을 검색중입니다.");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("log",result);
            parserPhrase(result);
            phrasesAdapter = new phraseAdapter(R.layout.phrase_list, arrayPhrase);
            phraseList.setAdapter(phrasesAdapter);
            progress.dismiss();
            //super.onPostExecute(s);
        }
    }

    private String connectPhrase() {
        String urlString = "https://westus.api.cognitive.microsoft.com/spid/v1.0/verificationPhrases?locale=en-US";

        try {

            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Host", HOST);
            con.setRequestProperty("Ocp-Apim-Subscription-Key", API_KEY);

            int responseCode = con.getResponseCode();

            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }else{
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }

            String inputLine;
            StringBuffer response = new StringBuffer();
            while((inputLine = br.readLine())!=null){
                response.append(inputLine);
            }
            br.close();
            return response.toString();

        } catch (IOException e) {
            return e.toString();
        }
    }

    public void parserPhrase(String result){

        arrayPhrase = new ArrayList<HashMap<String,String>>();
        try{
            //JSONArray jArray = new JSONObject(result).getJSONArray("phrase");
            JSONArray jArray = new JSONArray(result);

            for(int i =0;i<jArray.length();i++){
                JSONObject data = jArray.getJSONObject(i);
                String phrase = data.getString("phrase").toString();
                HashMap<String,String> map  = new HashMap<String, String>();
                map.put("phrase",phrase);
                arrayPhrase.add(map);
            }
        }catch (Exception e) { }

    }


    class phraseAdapter extends BaseAdapter {
        int layout;
        ArrayList<HashMap<String,String>> array;
        public phraseAdapter(int layout, ArrayList<HashMap<String,String>> array){
            this.layout = layout;
            this.array = array;
        }

        @Override
        public int getCount() {
            return array.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            view = inflater.inflate(R.layout.phrase_list,null);
            HashMap<String,String> map = array.get(position);
            TextView phrase = (TextView)view.findViewById(R.id.phrase);
            phrase.setText(map.get("phrase"));
            return view;
        }
    }

    /*
     * wav 변환
     * http://selvaline.blogspot.com/2016/04/record-audio-wav-format-android-how-to.html
     * */
    public class WavRecorder {
        private static final int RECORDER_BPP = 16;
        private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
        private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
        private static final int RECORDER_SAMPLERATE = 16000;//44100;
        private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        short[] audioData;

        private AudioRecord recorder = null;
        private int bufferSize = 0;
        private Thread recordingThread = null;
        private boolean isRecording = false;
        int[] bufferData;
        int bytesRecorded;

        private String output;

        public WavRecorder(String path) {
            bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 3;

            audioData = new short[bufferSize]; // short array that pcm data is put
            // into.
            output = path;

        }

        private String getFilename() {
            return (output);
        }

        private String getTempFilename() {
            String filepath = Environment.getExternalStorageDirectory().getPath();

            File file = new File(filepath, AUDIO_RECORDER_FOLDER);

            if (!file.exists()) {
                file.mkdirs();
            }
            /*
            File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);


            if (tempFile.exists())
                tempFile.delete();
            */
            return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
        }

        public void startRecording() {

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, bufferSize);
            int i = recorder.getState();
            if (i == 1)
                recorder.startRecording();

            isRecording = true;

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");

            recordingThread.start();
        }

        private void writeAudioDataToFile() {

            byte data[] = new byte[bufferSize];
            String filename = getTempFilename();

            FileOutputStream os = null;

            try {
                os = new FileOutputStream(filename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            int read = 0;
            if (null != os) {
                while (isRecording) {
                    read = recorder.read(data, 0, bufferSize);
                    if (read > 0) {
                    }

                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        try {
                            os.write(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stopRecording() {
            if (null != recorder) {
                isRecording = false;

                int i = recorder.getState();
                if (i == 1)
                    recorder.stop();
                recorder.release();

                recorder = null;
                recordingThread = null;
            }

            copyWaveFile(getTempFilename(), getFilename());

            //deleteTempFile();
        }

        private void deleteTempFile() {
            File file = new File(getTempFilename());
            file.delete();
        }

        private void copyWaveFile(String inFilename, String outFilename) {

            FileInputStream in = null;
            FileOutputStream out = null;
            long totalAudioLen = 0;
            long totalDataLen = totalAudioLen + 36;
            long longSampleRate = RECORDER_SAMPLERATE;
            int channels = ((RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1
                    : 2);
            long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

            byte[] data = new byte[bufferSize];

            try {
                in = new FileInputStream(inFilename);
                out = new FileOutputStream(outFilename);
                totalAudioLen = in.getChannel().size();
                totalDataLen = totalAudioLen + 36;

                ch = "오디오 전체 시간 : "+String.valueOf(totalAudioLen);

                WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                        longSampleRate, channels, byteRate);

                while (in.read(data) != -1) {
                    out.write(data);
                }

                in.close();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                         long totalDataLen, long longSampleRate, int channels, long byteRate)
                throws IOException {
            byte[] header = new byte[44];

            header[0] = 'R'; // RIFF/WAVE header
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f'; // 'fmt ' chunk
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16; // 4 bytes: size of 'fmt ' chunk
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1; // format = 1
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) (longSampleRate & 0xff);
            header[25] = (byte) ((longSampleRate >> 8) & 0xff);
            header[26] = (byte) ((longSampleRate >> 16) & 0xff);
            header[27] = (byte) ((longSampleRate >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (((RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1
                    : 2) * 16 / 8); // block align
            header[33] = 0;
            header[34] = RECORDER_BPP; // bits per sample
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

            out.write(header, 0, 44);
        }
    }


    /* Enrollment function */
    class downEnroll extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... strings) {
            return createEnrollment();
            //return null;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(Enrollment.this);
            progress.show();
            progress.setMessage("등록중입니다.");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("log",result);
            parserEnroll(result);

            Toast.makeText(getApplicationContext(), "[에러 발생]\n"+enrollMessage,2000).show();

            if(remainingEnrollments==0){
                //페이지 이동
                Toast.makeText(getApplicationContext(), "페이지를 이동합니다.",2000).show();
            }else if(remainingEnrollments==-1){

            }else{
                //남은 횟수 변경
                enrollNum = (TextView) findViewById(R.id.count);
                enrollNum.setText(String.valueOf(remainingEnrollments));
            }

            //에러코드 초기화
            responseCode = 0;
            remainingEnrollments=-1;
            progress.dismiss();
            //super.onPostExecute(s);
        }
    }

    private String createEnrollment(){
        PrintWriter writer;
        String boundary = "^-----^";
        String LINE_FEED = "\r\n";
        OutputStream outputStream;
        String charset = "UTF-8";

        //변환돤 wav파일 참
        wavfile = new File(RECORDED_FILE);

        String host = "westus.api.cognitive.microsoft.com";
        String conType = "multipart/form-data";
        String urlString = "https://westus.api.cognitive.microsoft.com/spid/v1.0/verificationProfiles/"+userProfile+"/enroll";

        try {

            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Host", HOST);
            con.setRequestProperty("Content-Type", conType);
            con.setRequestProperty("Content-Type", "multipart/form-data;charset=utf-8;boundary=" + boundary);
            con.setRequestProperty("Ocp-Apim-Subscription-Key", API_KEY);

            con.setDoOutput(true);
            con.setDoInput(true);

            outputStream = con.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);

            /*
             ** 파일 데이터를 넣는 부분
             ** http://kwon8999.tistory.com/entry/HttpURLConnection-Multipart-%ED%8C%8C%EC%9D%BC-%EC%97%85%EB%A1%9C%EB%93%9C
             **/

            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + wavfile.getName()  + "\"").append(LINE_FEED);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(wavfile.getName())).append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream inputStream = new FileInputStream(wavfile.getPath());

            byte[] buffer = new byte[(int)wavfile.length()];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();
            writer.append(LINE_FEED);
            writer.flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();

            responseCode = con.getResponseCode();

            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }else{
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }

            String inputLine;
            StringBuffer response = new StringBuffer();
            while((inputLine = br.readLine())!=null){
                response.append(inputLine);
            }
            br.close();

            return response.toString();

        } catch (Exception e) {
            return e.toString();
        }
    }

    public void parserEnroll(String result){
        //정상인 경우
        if(responseCode==200){
            try {
                int enrollmentsCount = new JSONObject(result).getInt("enrollmentsCount");
                //남은 횟수는 쓰레드에서 사용해야 함
                remainingEnrollments = new JSONObject(result).getInt("remainingEnrollments");
                String phrase = new JSONObject(result).getString("phrase");
                enrollMessage = String.valueOf(enrollmentsCount)+"회 등록 성공\n남은 횟수: "+String.valueOf(remainingEnrollments)+"\n인식된 문구: "+phrase;

            }catch (Exception e){}
        }//인자가 잘 못 된 경우
        else if(responseCode==400) {
            try {

                JSONObject jObj = new JSONObject(result).getJSONObject("error");

                //JSONObject data = jObj.getJSONObject("message");
                enrollMessage = jObj.getString("message");

                if (enrollMessage.equals("InvalidPhrase")) {
                    enrollMessage = "발음을 인식할 수 없습니다.ㅋ";
                } else if (enrollMessage.equals("SpeechNotRecognized")) {
                    enrollMessage = "음성이 확인되지 않습니다.\n(녹음 시, 말을 하세요!)";
                } else if (enrollMessage.equals("Audio too short")) {
                    enrollMessage = "녹음파일이 너무 짧습니다. \n최소 10초 녹음해주세요.";
                }

            } catch (Exception e) {
            }
        }

    }


}
