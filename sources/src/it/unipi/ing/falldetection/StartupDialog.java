package it.unipi.ing.falldetection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class StartupDialog
{
    /**
     * Checks if the basic user information is available and, if it is not, prompts a dialog
     * requesting the user to fill it. If the user tries to cancel the operation, the app quits.
     *
     * @param activity The main activity. It will be finish()'ed if the user tries to cancel the
     *        operation without filling the required information.
     */
    public static void validate(Activity activity)
    {
        if (!isPersistedInformationOk(activity)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setView(activity.getLayoutInflater().inflate(R.layout.dialog_startup, null));
            builder.setCancelable(false);
            builder.setNegativeButton(R.string.quit, new DismissOnClickListener(activity));
            builder.setPositiveButton(android.R.string.ok, new EmptyOnClickListener());

            AlertDialog alert = builder.create();
            alert.setOnShowListener(new SetupViewOnShowListener(activity));
            alert.show();
        }
    }

    private static class DismissOnClickListener implements DialogInterface.OnClickListener
    {
        private Activity activity;

        public DismissOnClickListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            activity.finish();
        }
    }

    private static class EmptyOnClickListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
    }

    private static class SetupViewOnShowListener implements DialogInterface.OnShowListener
    {
        private Context context;
        private Boolean alertReady = false;

        public SetupViewOnShowListener(Context context) {
            this.context = context;
        }

        @Override
        public void onShow(DialogInterface di) {
            if (alertReady == false) {
                AlertDialog dialog = (AlertDialog)di;

                ((TextView)dialog.findViewById(R.id.label_user_sex)).append(":");
                ((TextView)dialog.findViewById(R.id.label_user_age)).append(":");
                ((TextView)dialog.findViewById(R.id.label_user_height)).append(":");
                ((TextView)dialog.findViewById(R.id.label_user_weight)).append(":");

                loadPersistedInformation(context, dialog);

                Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(new ValidateOnClickListener(context, dialog));
                alertReady = true;
            }
        }
    }

    private static class ValidateOnClickListener implements View.OnClickListener
    {
        private Context context;
        private Dialog dialog;

        public ValidateOnClickListener(Context context, Dialog dialog) {
            this.context = context;
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {
            if (trySaveProvidedInformation(context, dialog)) {
                dialog.dismiss();
            }
        }
    }

    private static boolean isPersistedInformationOk(Context context)
    {
        String userSex = UserInformationHelper.getUserSex(context);
        String[] genderValues = context.getResources().getStringArray(R.array.gender_values);
        int i = 0;
        while (i < genderValues.length && !genderValues[i].equals(userSex)) {
            i++;
        }
        if (i >= genderValues.length) {
            return false;
        }

        if (UserInformationHelper.getUserAge(context) == 0.0f) {
            return false;
        }
        if (UserInformationHelper.getUserHeight(context) == 0.0f) {
            return false;
        }
        if (UserInformationHelper.getUserWeight(context) == 0.0f) {
            return false;
        }

        return true;
    }

    private static void loadPersistedInformation(Context context, AlertDialog dialog)
    {
        // Update views in reverse order to set focus to the first invalid input

        float userWeight = UserInformationHelper.getUserWeight(context);
        if (userWeight == 0.0f) {
            ((EditText)dialog.findViewById(R.id.user_weight)).requestFocus();
        }
        else {
            ((EditText)dialog.findViewById(R.id.user_weight)).setText(Float.toString(userWeight));
        }

        float userHeight = UserInformationHelper.getUserHeight(context);
        if (userHeight == 0.0f) {
            ((EditText)dialog.findViewById(R.id.user_height)).requestFocus();
        }
        else {
            ((EditText)dialog.findViewById(R.id.user_height)).setText(Float.toString(userHeight));
        }

        float userAge = UserInformationHelper.getUserAge(context);
        if (userAge == 0.0f) {
            ((EditText)dialog.findViewById(R.id.user_age)).requestFocus();
        }
        else {
            ((EditText)dialog.findViewById(R.id.user_age)).setText(Float.toString(userAge));
        }

        String userSex = UserInformationHelper.getUserSex(context);
        String[] genderValues = context.getResources().getStringArray(R.array.gender_values);
        int i = 0;
        while (i < genderValues.length && !genderValues[i].equals(userSex)) {
            i++;
        }
        if (i < genderValues.length) {
            ((Spinner)dialog.findViewById(R.id.user_sex)).setSelection(i);
        }
    }

    private static boolean trySaveProvidedInformation(Context context, Dialog dialog)
    {
        String userSex;
        Spinner userSexSpinner = (Spinner)dialog.findViewById(R.id.user_sex);
        try {
            String[] genderValues = context.getResources().getStringArray(R.array.gender_values);
            userSex = genderValues[userSexSpinner.getSelectedItemPosition()];
        }
        catch (Throwable e) {
            // Simply select the first item (in fact, it may have been selected automatically)
            userSexSpinner.setSelection(0);
            return false;
        }

        float userAge;
        EditText userAgeEditText = (EditText)dialog.findViewById(R.id.user_age);
        try {
            userAge = Float.parseFloat(userAgeEditText.getText().toString());
        }
        catch (Throwable e) {
            userAgeEditText.setError(context.getString(R.string.user_age_required));
            userAgeEditText.requestFocus();
            return false;
        }

        float userHeight;
        EditText userHeightEditText = (EditText)dialog.findViewById(R.id.user_height);
        try {
            userHeight = Float.parseFloat(userHeightEditText.getText().toString());
        }
        catch (Throwable e) {
            userHeightEditText.setError(context.getString(R.string.user_height_required));
            userHeightEditText.requestFocus();
            return false;
        }

        float userWeight;
        EditText userWeightEditText = (EditText)dialog.findViewById(R.id.user_weight);
        try {
            userWeight = Float.parseFloat(userWeightEditText.getText().toString());
        }
        catch (Throwable e) {
            userWeightEditText.setError(context.getString(R.string.user_weight_required));
            userWeightEditText.requestFocus();
            return false;
        }

        // Save information only if everything is OK
        UserInformationHelper.setUserSex(context, userSex);
        UserInformationHelper.setUserAge(context, userAge);
        UserInformationHelper.setUserHeight(context, userHeight);
        UserInformationHelper.setUserWeight(context, userWeight);
        return true;
    }

    private StartupDialog() {
    }
}
