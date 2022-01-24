package com.autod.gis.ui.fragment;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.esri.arcgisruntime.data.Field;
import com.autod.gis.R;
import com.autod.gis.ui.activity.MainActivity;



/**
 * 属性域设置类（未使用）
 */
public class AttributeDomainFragment extends Fragment
{
    private static AttributeDomainFragment instance;
    public  static  AttributeDomainFragment getInstance()
    {
        return instance;
    }

    private View control;
    public View getControl()
    {
        return  control;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
   instance = this;
           control=MainActivity.getInstance().findViewById(R.id.main_fgm_domain);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_attribution_domain, container, false);


    }

    public void show(Field.Type type)
    {
        if (control.getVisibility() == View.INVISIBLE)
        {
            control.setVisibility(View.VISIBLE);
        }
        if (control.getTranslationY() == 0)
        {
            ObjectAnimator.ofFloat(control, "translationY", 0, control.getHeight()).setDuration(500).start();
        }
        LinearLayout lltNumber = MainActivity.getInstance().findViewById(R.id.domain_llt_number_select);
        LinearLayout lltString = MainActivity.getInstance().findViewById(R.id.domain_llt_number_select);
        if (type == Field.Type.DOUBLE || type == Field.Type.FLOAT || type == Field.Type.INTEGER)
        {
            lltNumber.setVisibility(View.VISIBLE);
            lltString.setVisibility(View.INVISIBLE);
        }
        else
        {
            lltString.setVisibility(View.VISIBLE);
            lltNumber.setVisibility(View.INVISIBLE);
        }
    }
}
