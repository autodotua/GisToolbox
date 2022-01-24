package com.autod.gis.ui.part;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class SimpleTableView extends TableLayout
{//一个用于显示简易表格的VIEW

    protected int m_ColumnN = 2;//列的数目。该值只能在构造函数中设置，设置之后不能修改。

    int m_LineColor = Color.BLACK;//线的颜色
    int m_LineWidth = 2;//线宽

    protected List<TableRow> m_Rows;
    protected List<List<View>> m_Views;

    public int getM_ColumnN()
    {
        return m_ColumnN;
    }

    public SimpleTableView(Context context)
    {
        super(context);
        // TODO Auto-generated constructor stub
        m_Rows = new ArrayList<>();
        m_Views = new ArrayList<>();
        this.setWillNotDraw(false);
    }

    public SimpleTableView(Context context, int n)
    {//指定列的数目
        super(context);
        // TODO Auto-generated constructor stub
        m_Rows = new ArrayList<TableRow>();
        m_Views = new ArrayList<List<View>>();
        if (n > 0) m_ColumnN = n;
        else m_ColumnN = 2;
        this.setWillNotDraw(false);
    }

    public void ClearRows()
    {
        if (m_Rows != null) m_Rows.clear();
        if (m_Views != null) m_Views.clear();
        m_Rows = new ArrayList<TableRow>();
        m_Views = new ArrayList<List<View>>();

        this.removeAllViews();
    }

    public int AddRow(java.lang.Object[] objects)//添加一行，返回行数。如果objects的数目小于m_ColumnN则返回0。
    {
    return     AddRow(false, objects);
    }

    public int AddRow(boolean title, java.lang.Object[] objects)//添加一行，返回行数。如果objects的数目小于m_ColumnN则返回0。
    {
        if (objects == null) return 0;
        if (objects.length < m_ColumnN) return 0;

        List<View> CRowViews;
        int i, nRows;
        TableRow CRow;
        String s1 = null, ss[] = {" "};
        View v1 = null;

        m_Rows.add(new TableRow(this.getContext()));
        m_Views.add(new ArrayList<View>());
        nRows = m_Rows.size();
        CRowViews = m_Views.get(nRows - 1);
        CRow = m_Rows.get(nRows - 1);

        for (i = 0; i < m_ColumnN; i++)
        {
            if (objects[i] != null) v1 = CreateCellView(title,objects[i]);
            if (v1 == null) v1 = new View(getContext());
            CRow.addView(v1);
            CRowViews.add(v1);
        }
        this.addView(CRow);

        return nRows;
    }

    public View GetCellView(int row, int column)//获得某一个单元格的View，row为行数，column为列数，从0开始
    {
        if (row < 0 || row >= m_Rows.size()) return null;
        else
        {
            if (column < 0 || column >= m_Views.get(row).size()) return null;
            else return m_Views.get(row).get(column);
        }
    }

    protected View CreateCellView(boolean title, Object obj)//根据obj的类型创建一个VIEW并返回之，如果无法识别Object的类型返回null
    {
        View rView = null;
        String classname = obj.getClass().toString();

        switch (classname)
        {
            case "class java.lang.String"://这个值是String.class.toString()的结果

                TextView tView = new TextView(getContext());
                tView.setText((String) obj);
                tView.setTextSize(20);
                if(title)
                {
                    tView.setTypeface(null, Typeface.BOLD);
                }
                rView = tView;
                break;

            case "class android.graphics.Bitmap":
                ImageView iView = new ImageView(getContext());
                iView.setImageBitmap((Bitmap) obj);
                rView = iView;
                break;

            //在此处识别其它的类型，创建一个View并附给rView

            default:
                rView = null;
                break;
        }
        if (rView != null)
        {
            rView.setPadding(8, 8, 8, 8);
        }
        return rView;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        //
        if (m_Rows.size() < 1) return;

        Paint paint1 = new Paint();
        int i, nRLinePosition = 0, nCLinePosition = 0, width = getWidth(), height = getHeight();
        paint1.setStyle(Paint.Style.STROKE);
        paint1.setStrokeWidth(m_LineWidth);
        paint1.setColor(m_LineColor);

        canvas.drawRect(new Rect(1, 1, width, height), paint1);

        for (i = 0; i < m_Rows.size(); i++)
        {
            nRLinePosition += m_Rows.get(i).getHeight();
            canvas.drawLine(0, nRLinePosition, width, nRLinePosition, paint1);
        }
        for (i = 0; i < m_Views.get(0).size(); i++)
        {
            nCLinePosition += m_Views.get(0).get(i).getWidth();
            canvas.drawLine(nCLinePosition, 0, nCLinePosition, height, paint1);
        }
    }
}