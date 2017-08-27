package com.surya945.cc.audiorecord;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordTrainingActivity extends AppCompatActivity {
    String LOG_TAG="Tag";
    HandlerClass handlerClass;
    GraphView graphView;
    LineGraphSeries series;
    boolean storing=false;
    boolean mSnore=false,mNormal=false;
    boolean mShouldContinue=true;
    List<List<Short>>Snore=new ArrayList();
    List<List<Short>>Normal=new ArrayList();
    final int SAMPLE_RATE = 4000;
    TextView SnoreDataLength,NormalDataLength;
    ExecutorService oneThreade;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordtraining);
        handlerClass=new HandlerClass();

        this.oneThreade=Executors.newFixedThreadPool(1);

        Button btnSave=(Button)findViewById(R.id.btnSave);
        Button btnNormal=(Button)findViewById(R.id.btnNormal);
        Button btnSnore=(Button)findViewById(R.id.btnSnore);
        Button ClearData=(Button)findViewById(R.id.ClearData);
        ClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snore=new ArrayList<List<Short>>();
                Normal=new ArrayList<List<Short>>();
                handlerClass.obtainMessage(3).sendToTarget();
                handlerClass.obtainMessage(4).sendToTarget();
            }
        });
        SnoreDataLength=(TextView)findViewById(R.id.snoreText);
        NormalDataLength=(TextView)findViewById(R.id.normalText);

        final TextView textView=(TextView)findViewById(R.id.textView);
        final EditText fileName=(EditText)findViewById(R.id.editTextFileName);



        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShouldContinue=false;
                generateNoteOnSD(RecordTrainingActivity.this,fileName.getText().toString()+"_Snore",GetStringValue(Snore));
                generateNoteOnSD(RecordTrainingActivity.this,fileName.getText().toString()+"_Normal",GetStringValue(Normal));
            }
        });
        btnSnore.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    textView.setText("Button Pressed");
                    mSnore=true;
                    recordAudioSnore();
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    textView.setText(""); //finger was lifted
                    mSnore=false;
                }
                return true;
            }

        });

        btnNormal.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    textView.setText("Button Pressed");
                    mNormal=true;
                    recordAudioNormal();
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    textView.setText(""); //finger was lifted
                    mNormal=false;
                }
                return true;
            }

        });


    }



    void recordAudioSnore() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                // buffer size in bytes
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    bufferSize = SAMPLE_RATE * 2;
                }

                short[] audioBuffer = new short[bufferSize / 2];

                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(LOG_TAG, "Audio Record can't initialize!");
                    return;
                }
                record.startRecording();

                Log.v(LOG_TAG, "Start recording");
                short snore=0;
                long shortsRead = 0,count=0;
                while (mSnore) {
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    List<Short> newData = new ArrayList<>();
                    shortsRead += numberOfShort;
                    for (int i = 0; i < numberOfShort; i++) {
                        newData.add(audioBuffer[i]);
                    }
                    newData.add((short) 1);
                    Snore.add(newData);
                    handlerClass.obtainMessage(3).sendToTarget();
                }
                record.stop();
                record.release();

                Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
            }
        }).start();
    }
    void recordAudioNormal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                // buffer size in bytes
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    bufferSize = SAMPLE_RATE * 2;
                }

                short[] audioBuffer = new short[bufferSize / 2];

                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(LOG_TAG, "Audio Record can't initialize!");
                    return;
                }
                record.startRecording();

                Log.v(LOG_TAG, "Start recording");
                short snore=0;
                long shortsRead = 0,count=0;
                while (mNormal) {
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    List<Short> newData=new ArrayList<>();
                    shortsRead += numberOfShort;
                    for (int i=0;i<numberOfShort;i++){
                        newData.add(audioBuffer[i]);
                    }
                    newData.add((short)0);
                    Normal.add(newData);
                    handlerClass.obtainMessage(4).sendToTarget();

                }

                record.stop();
                record.release();

                Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
            }
        }).start();
    }
    public void generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "AudioNotes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String GetStringValue(List<List<Short>>data){
        String s="";
        for (int i=0;i<data.size();i++){
            String row="";
            for (int j=0;j<data.get(i).size();j++){
                row+=data.get(i).get(j)+" ";
            }
            s+=row+"\n";
        }
        return s;
    }
    public void GetDataPoints(List<Short>data){
        //List<DataPoint>
    }
    class HandlerClass extends Handler {
        HandlerClass() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Toast.makeText(RecordTrainingActivity.this,Integer.toString(msg.arg1),Toast.LENGTH_SHORT).show();
                    return;
                case 2:
                    List<DataPoint>series1=(List<DataPoint>)msg.obj;
                    for (int i=0;i<series1.size();i++){
                        series.appendData(series1.get(i),true,5000);
                    }
                    //series.appendData(new DataPoint(msg.arg1,0),true,5000);
                    return;
                case 3:
                    SnoreDataLength.setText(Integer.toString(Snore.size()));
                    return;
                case 4:
                    NormalDataLength.setText((Integer.toString(Normal.size())));
                    return;
                default:
                    return;
            }
        }
    }

}
