package com.autod.gis.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;

import com.autod.gis.programming.GetString;

public class DialogHelper
{

    public static void showSetValueDialog(Context context, String title, String message, String value, int type, GetString r)
    {
        final EditText editText = new EditText(context);
        editText.setInputType(type);
        editText.setText(value);
        editText.setSingleLine(false);
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> r.get(editText.getText().toString()))
                .create().show();
    }
}
