package it.unipi.ing.falldetection;

import it.unipi.ing.falldetection.ContactListPreference.Contact;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class FallDetectedActivity extends Activity
{
    private TextView textview_countdown;
    private Vibrator vibrator;
    private Ringtone alarmRingtone;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fall_detected);

        TableLayout actions_table = (TableLayout)findViewById(R.id.actions_table);
        TextView action_list_header = (TextView)findViewById(R.id.action_list_header);
        textview_countdown = (TextView)findViewById(R.id.textview_countdown);
        Button confirm_fall = (Button)findViewById(R.id.confirm_fall);
        Button deny_fall = (Button)findViewById(R.id.deny_fall);

        confirm_fall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                confirm(true);
            }
        });

        deny_fall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                confirm(false);
            }
        });

        boolean smsAlert = UserPreferencesHelper.isSmsAlertEnabled(this);
        boolean phoneCallAlert = UserPreferencesHelper.isPhoneCallAlertEnabled(this);
        if (smsAlert || phoneCallAlert) {
            action_list_header.setText(R.string.action_list_header);
        }
        if (smsAlert) {
            Contact[] recipients = UserPreferencesHelper.getSmsRecipients(this);
            String list = null;
            if (recipients.length == 1) {
                list = recipients[0].title + " (" + recipients[0].value + ")";
            }
            else if (recipients.length > 1) {
                StringBuilder sb = new StringBuilder();
                boolean firstTime = true;
                for (Contact c : recipients) {
                    if (firstTime) {
                        firstTime = false;
                    }
                    else {
                        sb.append(", ");
                    }
                    sb.append(c.title);
                }
                list = sb.toString();
            }
            if (list != null) {
                TextView textViewBullet = new TextView(this);
                TextView textViewText = new TextView(this);
                textViewBullet.setText("\u2022");
                textViewBullet.setPadding(10, 0, 10, 0);
                textViewText.setText(getString(R.string.sms_will_be_sent, list));
                TableRow row = new TableRow(this);
                row.addView(textViewBullet);
                row.addView(textViewText);
                actions_table.addView(row);
            }
        }
        if (phoneCallAlert) {
            Contact recipient = UserPreferencesHelper.getPhoneCallRecipient(this);
            if (recipient != null) {
                TextView textViewBullet = new TextView(this);
                TextView textViewText = new TextView(this);
                textViewBullet.setText("\u2022");
                textViewBullet.setPadding(10, 0, 10, 0);
                textViewText.setText(getString(R.string.phone_call_will_start, recipient));
                TableRow row = new TableRow(this);
                row.addView(textViewBullet);
                row.addView(textViewText);
                actions_table.addView(row);
            }
        }

        if (UserPreferencesHelper.isVibrateEnabled(this)) {
            vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(new long[] { 0, 500, 1000 }, 0);
        }

        String ringtone = UserPreferencesHelper.getAlarmSound(this);
        if (!TextUtils.isEmpty(ringtone)) {
            alarmRingtone = RingtoneManager.getRingtone(this, Uri.parse(ringtone));
            if (alarmRingtone == null) {
                // Ringtone not found (old settings?)
            }
            else {
                alarmRingtone.play();
            }
        }

        int timeout = 1000 * UserPreferencesHelper.getAlertTimeout(this);
        countDownTimer = new CountDownTimer(timeout, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                textview_countdown.setText(DateUtils.formatElapsedTime(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                confirm(true);
            }
        };
        countDownTimer.start();
    }

    private void confirm(boolean confirmed) {
        // TODO: implement confirm()
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (alarmRingtone != null) {
            alarmRingtone.stop();
            alarmRingtone = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
    }
}
