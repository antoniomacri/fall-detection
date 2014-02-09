package it.unipi.ing.falldetection;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;

/**
 * Listens for completion of an SMS send operation and shows a notification in case of failure.
 *
 * This is a one-shot receiver used for a single operation, so it unregisters itself when it is
 * done.
 */
public class SmsSentBroadcastReceiver extends BroadcastReceiver
{
    private static final int NOTIFICATION_ID = R.string.cannot_send_sms;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        try {
            if (getResultCode() == Activity.RESULT_OK) {
                return;
            }

            NotificationCompat.Builder b = new NotificationCompat.Builder(context);
            b.setContentTitle(context.getString(R.string.app_name));
            b.setSmallIcon(R.drawable.ic_launcher);
            Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarm != null) {
                b.setSound(alarm);
            }
            b.setAutoCancel(false);

            String name = intent.getStringExtra("contact_name");
            String number = intent.getStringExtra("contact_number");

            int errorResId;
            switch (getResultCode()) {
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    errorResId = R.string.result_generic_error;
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    errorResId = R.string.result_no_service;
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    errorResId = R.string.result_radio_off;
                    break;
                default:
                    errorResId = R.string.result_unknown_error;
            }
            String error = context.getString(errorResId);
            String message = context.getString(R.string.cannot_send_sms, name, number, error);

            // Use large-format notifications to wrap text if it fills an entire line
            b.setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, b.build());
        }
        finally {
            context.unregisterReceiver(this);
        }
    }
}
