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

public class Main2Activity extends AppCompatActivity {
    String LOG_TAG="Tag";
    HandlerClass handlerClass;
    GraphView graphView;
    LineGraphSeries series;
    boolean storing=false;
    boolean mSnore=false,mNormal=false;
    boolean mShouldContinue=true;
    List<List<Short>>Snore=new ArrayList(80000);
    List<List<Short>>Normal=new ArrayList(80000);
    final int SAMPLE_RATE = 4000;
    TextView SnoreDataLength,NormalDataLength;
    ExecutorService oneThreade;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        handlerClass=new HandlerClass();

        this.oneThreade=Executors.newFixedThreadPool(1);

        graphView=(GraphView)findViewById(R.id.graphView);
        graphView.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        // graph_x.getViewport().setScalableY(true); // enables vertical zooming and scrolling

        // data
        series = new LineGraphSeries<DataPoint>();
        series.setTitle("VoiceData");
        series.setColor(Color.RED);

        graphView.addSeries(series);

        graphView.getLegendRenderer().setVisible(true);
        graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

    }



    void recordAudio() {
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
                while (mShouldContinue) {
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    final List<Short> newData=new ArrayList<>(4000);
                    shortsRead += numberOfShort;
                    for (int i=0;i<numberOfShort;i++){
                        newData.add(audioBuffer[i]);
                    }
                    newData.add((short)1);
                    Snore.add(newData);
                    oneThreade.execute(new Runnable() {
                        @Override
                        public void run() {
                            GetDataPoints(newData);
                        }
                    });
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
    int index=0;
    public void GetDataPoints(List<Short> data){
        List<DataPoint>dataPoints=new ArrayList<>();
        for(int j=0;j<data.size();j+=100){
            dataPoints.add(new DataPoint(index,GetMeanValue(j,j+100,data)));
            index++;
        }
        handlerClass.obtainMessage(5,dataPoints).sendToTarget();

    }
    public double GetMeanValue(int i,int j,List<Short>data){
        double mean=0;
        int count=0;
        while(i<=j&& i<data.size()){
            mean+=(double)data.get(i);
            i++;
            count++;
        }
        mean=mean/count;
        return mean;
    }
    class HandlerClass extends Handler {
        HandlerClass() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Toast.makeText(Main2Activity.this,Integer.toString(msg.arg1),Toast.LENGTH_SHORT).show();
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
                case 5:
                    List<DataPoint>dataPoints=(List<DataPoint>)msg.obj;
                    for (int i=0;i<dataPoints.size();i++) {
                        series.appendData(dataPoints.get(i),true,500);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void OnStartClick(View view){
        mShouldContinue=true;
        recordAudio();
    }
    public void OnStopClick(View view){
        mShouldContinue=false;
    }

}
