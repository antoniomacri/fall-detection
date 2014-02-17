package it.unipi.ing.falldetection.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.DateFormat;
import android.util.Log;
import it.unipi.ing.falldetection.R;
import it.unipi.ing.falldetection.UserInformationHelper;

public class Uploader
{
    protected String postUrl = "https://posttestserver.com/post.php";
    protected Context context;
    protected Timer timer;
    protected String userAgent;

    public Uploader(Context context) {
        this.context = context;
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new DoUploadTimerTask(), 10 * 60 * 1000, 20 * 60 * 1000);

        try {
            String v = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            this.userAgent = context.getString(R.string.app_name) + "/" + v;
        }
        catch (NameNotFoundException e) {
            this.userAgent = context.getString(R.string.app_name);
        }
    }

    public void stop() {
        timer.cancel();
    }

    public void enqueue(FallDetectionEvent event)
    {
        try {
            saveLocally(event);
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.i(getClass().getSimpleName(), sw.toString());
        }
    }

    private class DoUploadTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            String[] list = context.getFilesDir().list();
            for (int i = 0; i < list.length; i++) {
                try {
                    if (upload(list[i])) {
                        if (context.deleteFile(list[i]) == false) {
                            Log.w(getClass().getSimpleName(), "Cannot delete file: " + list[i]);
                        }
                    }
                }
                catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    Log.i(getClass().getSimpleName(), sw.toString());
                }
            }
        }
    }

    private void saveLocally(FallDetectionEvent event) throws IOException
    {
        String filename = DateFormat.format("yyyy-MM-dd-HH-mm-ss", new Date()).toString() + ".arff";

        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        PrintWriter writer = new PrintWriter(fos);

        writer.println("% Class: " + (event.confirmed ? "Fall" : "False_Alarm"));
        String sex = UserInformationHelper.getUserSex(context);
        String age = UserInformationHelper.getUserAge(context) + "";
        String h = UserInformationHelper.getUserHeight(context) + "";
        String w = UserInformationHelper.getUserWeight(context) + "";
        writer.println("% User (sex,age,height[cm],weight[kg]): " + sex + "," + age + "," + h + "," + w);
        writer.println("% Notes: " + event.notes);

        writer.println("@RELATION  LinearAcceleration");
        for (int i = 0; i < event.snapshot.descriptions.length; i++) {
            writer.println("@ATTRIBUTE " + event.snapshot.descriptions[i] + " NUMERIC");
        }

        writer.println("@DATA");
        for (int i = 0; i < event.snapshot.values.length; i++) {
            for (int j = 0; j < event.snapshot.values[i].length; j++) {
                if (j > 0) {
                    writer.print(",");
                }
                writer.print(event.snapshot.values[i][j]);
            }
            writer.println();
        }

        fos.close();
    }

    private boolean upload(String file) throws IOException
    {
        HttpsURLConnection con = (HttpsURLConnection)new URL(postUrl).openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", userAgent);
        con.setRequestProperty("Content-Type", "text/x-arff");
        con.setDoOutput(true);

        FileInputStream fis = context.openFileInput(file);
        byte[] buffer = new byte[256];
        int bytesRead = 0;
        while ((bytesRead = fis.read(buffer)) != -1) {
            con.getOutputStream().write(buffer, 0, bytesRead);
        }

        int responseCode = con.getResponseCode();
        return responseCode == 200;
    }
}
