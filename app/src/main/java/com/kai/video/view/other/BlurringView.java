package com.kai.video.view.other;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.view.View;

import com.kai.video.R;

public class BlurringView extends View {

    private int mDownsampleFactor;
    private int mOverlayColor;
    private View mBlurredView;
    private int mBlurredViewWidth, mBlurredViewHeight;
    private boolean mDownsampleFactorChanged;
    private Bitmap mBitmapToBlur, mBlurredBitmap;
    private Canvas mBlurringCanvas;
    private RenderScript mRenderScript;
    private ScriptIntrinsicBlur mBlurScript;
    private Allocation mBlurInput, mBlurOutput;

    public BlurringView(Context context) {
        this(context, null);
    }

    public BlurringView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources res = getResources();
        final int defaultBlurRadius = res.getInteger(R.integer.default_blur_radius);
        final int defaultDownsampleFactor = res.getInteger(R.integer.default_downsample_factor);
        final int defaultOverlayColor = res.getColor(R.color.colorPrimaryDark);

        initializeRenderScript(context);

        @SuppressLint("CustomViewStyleable") TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PxBlurringView);
        setBlurRadius(a.getInt(R.styleable.PxBlurringView_blurRadius, defaultBlurRadius));
        setDownsampleFactor(a.getInt(R.styleable.PxBlurringView_downsampleFactor,
                defaultDownsampleFactor));
        setOverlayColor(a.getColor(R.styleable.PxBlurringView_overlayColor, defaultOverlayColor));
        a.recycle();
    }

    public void setBlurredView(View blurredView) {
        mBlurredView = blurredView;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBlurredView != null) {
            if (prepare()) {
                // If the background of the blurred view is a color drawable, we use it to clear
                // the blurring canvas, which ensures that edges of the child views are blurred
                // as well; otherwise we clear the blurring canvas with a transparent color.
                if (mBlurredView.getBackground() != null && mBlurredView.getBackground() instanceof ColorDrawable) {
                    mBitmapToBlur.eraseColor(((ColorDrawable) mBlurredView.getBackground()).getColor());
                } else {
                    mBitmapToBlur.eraseColor(Color.TRANSPARENT);
                }

                mBlurredView.draw(mBlurringCanvas);
                blur();

                canvas.save();
                canvas.translate(mBlurredView.getX() - getX(), mBlurredView.getY() - getY());
                canvas.scale(mDownsampleFactor, mDownsampleFactor);
                canvas.drawBitmap(mBlurredBitmap, 0, 0, null);
                canvas.restore();
            }
            canvas.drawColor(mOverlayColor);
        }
    }

    public void setBlurRadius(int radius) {
        mBlurScript.setRadius(radius);
    }

    public void setDownsampleFactor(int factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("Downsample factor must be greater than 0.");
        }

        if (mDownsampleFactor != factor) {
            mDownsampleFactor = factor;
            mDownsampleFactorChanged = true;
        }
    }

    public void setOverlayColor(int color) {
        mOverlayColor = color;
    }

    private void initializeRenderScript(Context context) {
        mRenderScript = RenderScript.create(context);
        mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
    }

    protected boolean prepare() {
        final int width = mBlurredView.getWidth();
        final int height = mBlurredView.getHeight();

        if (mBlurringCanvas == null || mDownsampleFactorChanged
                || mBlurredViewWidth != width || mBlurredViewHeight != height) {
            mDownsampleFactorChanged = false;

            mBlurredViewWidth = width;
            mBlurredViewHeight = height;

            int scaledWidth = width / mDownsampleFactor;
            int scaledHeight = height / mDownsampleFactor;

            // The following manipulation is to avoid some RenderScript artifacts at the edge.
            scaledWidth = scaledWidth - scaledWidth % 4 + 4;
            scaledHeight = scaledHeight - scaledHeight % 4 + 4;

            if (mBlurredBitmap == null
                    || mBlurredBitmap.getWidth() != scaledWidth
                    || mBlurredBitmap.getHeight() != scaledHeight) {
                mBitmapToBlur = Bitmap.createBitmap(scaledWidth, scaledHeight,
                        Bitmap.Config.ARGB_8888);
                if (mBitmapToBlur == null) {
                    return false;
                }

                mBlurredBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight,
                        Bitmap.Config.ARGB_8888);
                if (mBlurredBitmap == null) {
                    return false;
                }
            }

            mBlurringCanvas = new Canvas(mBitmapToBlur);
            mBlurringCanvas.scale(1f / mDownsampleFactor, 1f / mDownsampleFactor);
            mBlurInput = Allocation.createFromBitmap(mRenderScript, mBitmapToBlur,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            mBlurOutput = Allocation.createTyped(mRenderScript, mBlurInput.getType());
        }
        return true;
    }

    protected void blur() {
        mBlurInput.copyFrom(mBitmapToBlur);
        mBlurScript.setInput(mBlurInput);
        mBlurScript.forEach(mBlurOutput);
        mBlurOutput.copyTo(mBlurredBitmap);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRenderScript != null) {
            mRenderScript.destroy();
        }
    }
}