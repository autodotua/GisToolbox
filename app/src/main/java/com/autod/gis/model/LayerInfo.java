package com.autod.gis.model;

public class LayerInfo
{
    public LayerInfo()
    {
    }

    public LayerInfo(String path, boolean visible, float opacity)
    {
        this.path = path;
        this.visible = visible;
        this.opacity = opacity;
    }
    public LayerInfo(String path)
    {
        this(path,true,1f);
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public float getOpacity()
    {
        return opacity;
    }

    public void setOpacity(float opacity)
    {
        this.opacity = opacity;
    }

    private String path;
    private boolean visible;
    private float opacity;
}
