package com.autod.gis.ui;

import android.app.Activity;
import android.app.AlertDialog;

public class UIHelper
{
    public static void showSimpleErrorDialog(Activity activity, String message)
    {
        activity.runOnUiThread(() -> {
            AlertDialog alertDialog = new AlertDialog.Builder(activity)
                    .setTitle("错误")
                    .setMessage(message)
                    .setPositiveButton("确定",
                            (dialog, which) -> dialog.dismiss()).create();
            alertDialog.show();
        });

    }
}
