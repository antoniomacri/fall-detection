package it.unipi.ing.falldetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.telephony.TelephonyManager;

/**
 * Monitor for changes to the state of the phone and set the speaker phone loud when the outgoing
 * call to a specific number is accepted.
 *
 * A SpeakerPhoneBroadcastReceiver object must be registered for both the intents
 * <c>Intent.ACTION_NEW_OUTGOING_CALL</c> and <c>TelephonyManager.ACTION_PHONE_STATE_CHANGED</c>.
 * The former is needed to intercept the outgoing call and match it against a given phone number.
 * The latter is required to turn the speakers on when the call is accepted and to turn them off
 * when the call is finished.
 */
public class SpeakerPhoneBroadcastReceiver extends BroadcastReceiver
{
    private String number;
    private boolean calling;

    public SpeakerPhoneBroadcastReceiver(String number) {
        this.number = number;
        this.calling = false;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Notice that the phone number is provided only with ACTION_NEW_OUTGOING_CALL.

        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction()))
        {
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            if (number.equals(phoneNumber)) {
                calling = true;
            }
        }
        else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
        {
            String extraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (!calling) {
                // Ignore (can this actually happen?)
            }
            else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(extraState)) {
                // The call has been accepted: now we can set the speakers loud.

                // Create an handler to set on the speakers after a little interval (more reliable).
                Handler handler = new Handler();
                class LoudSpeakerPhoneHandler implements Runnable
                {
                    private Context context;

                    public LoudSpeakerPhoneHandler(Context context) {
                        this.context = context;
                    }

                    @Override
                    public void run() {
                        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                        audioManager.setMode(AudioManager.MODE_IN_CALL);
                        audioManager.setSpeakerphoneOn(true);
                    }
                }
                handler.postDelayed(new LoudSpeakerPhoneHandler(context), 500);
            }
            else if (TelephonyManager.EXTRA_STATE_IDLE.equals(extraState)) {
                // The call has finished.

                // Starting from Android KitKat this is not called anymore, so the speaker actually
                // remains loud, but Android itself will reset the speaker to normal for the next
                // call.

                // Unregister the broadcast receiver.
                // On KitKat, not unregistering the BroadcastReceiver seems not to cause any
                // exception. Should we worry?
                context.unregisterReceiver(this);

                AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                am.setMode(AudioManager.MODE_NORMAL);

                calling = false; // Not strictly necessary
            }
        }
    }
}
