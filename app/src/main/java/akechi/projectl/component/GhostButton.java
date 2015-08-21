package akechi.projectl.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;

public class GhostButton
    extends ImageButton
{
    public GhostButton(Context context)
    {
        super(context);
    }

    public GhostButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public GhostButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void show()
    {
        this.setVisibility(View.VISIBLE);
    }

    public void hide()
    {
        this.setVisibility(View.GONE);
    }
}
