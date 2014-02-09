package it.unipi.ing.falldetection;

import it.unipi.ing.falldetection.core.*;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class MainActivity extends Activity
{
    private ImageView activate_image;
    private TextView activate_title;
    private TextView activate_summary;
    private RelativeLayout activate_button;
    private TextView falls_detected;
    private TextView falls_confirmed;

    private FallDetectionService service;
    private boolean bound = false;
    private boolean active = false;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activate_button = (RelativeLayout)findViewById(R.id.activate_button);
        activate_button.setOnClickListener(uiListener);
        activate_image = (ImageView)findViewById(R.id.activate_image);
        Button bt = new Button(this);
        activate_button.setBackgroundDrawable(bt.getBackground());
        activate_title = (TextView)findViewById(R.id.activate_title);
        activate_title.setTextColor(bt.getTextColors());
        activate_summary = (TextView)findViewById(R.id.activate_summary);
        activate_summary.setTextColor(bt.getTextColors());

        falls_detected = (TextView)findViewById(R.id.number_of_falls_detected);
        falls_confirmed = (TextView)findViewById(R.id.number_of_falls_confirmed);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to the service
        Intent intent = new Intent(this, FallDetectionService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (bound) {
            unbindService(connection);
            service.removeListener(fallServiceListener);
            service = null;
            bound = false;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            FallDetectionService.Binder b = (FallDetectionService.Binder)binder;
            service = b.getService();
            service.addListener(fallServiceListener);
            bound = true;
            active = service.isActive();
            updateActivityButton(false, null);
            updateStatistics();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // This is not called when the client unbinds explicitly,
            // but when the connection to the service is unexpectedly lost:
            // so just mark this activity as unbound.
            service = null;
            bound = false;
        }
    };

    private void updateActivityButton(boolean error, String errorMessage) {
        if (!bound) {
            return;
        }
        if (active) {
            activate_image.setImageResource(android.R.drawable.ic_media_pause);
            activate_title.setText(getResources().getString(R.string.status_active));
            activate_summary.setText(getResources().getString(R.string.tap_to_deactivate));
        }
        else if (!error) {
            activate_image.setImageResource(android.R.drawable.ic_media_play);
            activate_title.setText(getResources().getString(R.string.status_inactive));
            activate_summary.setText(getResources().getString(R.string.tap_to_activate));
        }
        else {
            activate_button.setEnabled(false);
            activate_image.setImageResource(android.R.drawable.stat_notify_error);
            activate_title.setText(getResources().getString(R.string.status_inactive));
            activate_summary.setText(errorMessage);
        }
    }

    private void updateStatistics() {
        if (!bound) {
            return;
        }
        falls_detected.setText(Integer.toString(StatisticsHelper.getFallDetectedCount(this)));
        falls_confirmed.setText(Integer.toString(StatisticsHelper.getFallConfirmedCount(this)));
    }

    /**
     * Defines the onClick callback for the activity button.
     */
    private OnClickListener uiListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!bound) {
                return;
            }
            if (active) {
                service.stopFallDetection();
            }
            else {
                service.startFallDetection();
            }
        }
    };

    /**
     * Defines callbacks for the FallDetectionService.
     */
    private IFallDetectionServiceListener fallServiceListener = new IFallDetectionServiceListener()
    {
        private Runnable updateStatisticsRunnable = new Runnable() {
            public void run() {
                updateStatistics();
            }
        };

        @Override
        public void onFallDetected(IFallDetectionStrategy sender, FallDetectionEvent event) {
            MainActivity.this.runOnUiThread(updateStatisticsRunnable);
        }

        @Override
        public void onFallConfirmed(IFallDetectionStrategy sender, FallDetectionEvent event) {
            MainActivity.this.runOnUiThread(updateStatisticsRunnable);
        }

        @Override
        public void onFallDetectionStarted() {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    active = true;
                    updateActivityButton(false, null);
                }
            });
        }

        @Override
        public void onFallDetectionStopped(boolean error, String errorMessage) {
            class handler implements Runnable
            {
                private boolean error;
                private String errorMessage;

                public handler(boolean error, String errorMessage) {
                    this.error = error;
                    this.errorMessage = errorMessage;
                }

                public void run() {
                    active = false;
                    updateActivityButton(error, errorMessage);
                }
            }
            MainActivity.this.runOnUiThread(new handler(error, errorMessage));
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_settings:
            Intent i = new Intent(this, UserPreferencesActivity.class);
            startActivityForResult(i, -1);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
