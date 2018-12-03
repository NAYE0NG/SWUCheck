package com.example.nayeong.swucheck;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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


public class SWUCheck extends AppCompatActivity {
    static final String HOST = "westus.api.cognitive.microsoft.com";
    static final String API_KEY ="236f82fc1810454c8c95e75a5c33d641";

    static final String RECORDED_FILE = "/sdcard/recorde.wav";
    WavRecorder wavRecorder = new WavRecorder("/sdcard/recorde.wav");
    File wavfile;

    //azure private user profile and this app user id
    String userProfile, loginId;

    //Verification status
    int responseCode;
    String verificationResult;

    ProgressDialog progress;
    TextView userInfo;
    Button recordBtn, recordStopBtn, enrollBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.swucheck);

        Intent getIntent = getIntent();
        userProfile = getIntent.getStringExtra("userProfile");
        loginId = getIntent.getStringExtra("loginId");

        userInfo = (TextView)findViewById(R.id.userInfo);
        userInfo.setText("안녕하세요."+loginId+"님\n음성등록이 성공적으로 완료되어 출석체크가 가능합니다.");

        recordBtn = (Button) findViewById(R.id.recordBtn);
        recordStopBtn = (Button) findViewById(R.id.recordStopBtn);
        enrollBtn = (Button) findViewById(R.id.enrollBtn);


        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wavRecorder.startRecording();
                Toast.makeText(getApplicationContext(), "녹음시작", 2000).show();
            }
        });
        recordStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wavRecorder.stopRecording();
                Toast.makeText(getApplicationContext(), "녹음정지", 2000).show();
            }
        });

        enrollBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new downVerification().execute(null,null);
            }
        });



    }

    /*
     * wav 변환
     */
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

                //ch = "오디오 전체 시간 : "+String.valueOf(totalAudioLen);

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

    /* verification function */
    class downVerification extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... strings) {
            return createVerification();
            //return null;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(SWUCheck.this);
            progress.show();
            progress.setMessage("등록중입니다.");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("log",result);
            parserVerification(result);

            Toast.makeText(getApplicationContext(), verificationResult,2000).show();

            progress.dismiss();
            //super.onPostExecute(s);

        }
    }

    private String createVerification(){
        PrintWriter writer;
        String boundary = "^-----^";
        String LINE_FEED = "\r\n";
        OutputStream outputStream;
        String charset = "UTF-8";

        //변환돤 wav파일 참
        wavfile = new File(RECORDED_FILE);

        String conType = "application/octet-stream";
        String urlString = "https://westus.api.cognitive.microsoft.com/spid/v1.0/verify?verificationProfileId="+userProfile;

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
             */

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

    public void parserVerification(String result){
        //정상인 경우
        if(responseCode==200){
            try {

                String objVerification =  new JSONObject(result).getString("result").toString();

                if(objVerification.equals("Accept")) {
                    //혜준이 블루투스 함수 호출
                    verificationResult = "[출석체크 완료]\n본인 목소리거 확인되었습니다.";

                }else if(objVerification.equals("Reject")){
                    verificationResult = "[출석체크 실패]\n본인이 아닙니다.";
                }else{
                    verificationResult = "[출석체크 실패]\n인터넷 연결을 확인하세요.";
                }


            }catch (Exception e){}
        }//인자가 잘 못 된 경우
        else if(responseCode==400) {
            verificationResult = "[출석체크 실패]\n인터넷 연결 및 녹음파일을 확인하세요.";
        }

    }


}
