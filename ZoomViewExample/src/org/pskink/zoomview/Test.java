package org.pskink.zoomview;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Random;

import org.pskink.zoomview.view.ZoomView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ComposeShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

public class Test extends Activity {
    private static final float FACTOR_X = 2f;
    private static final float FACTOR_Y = 1.5f;

    private final static String TAG = "Test";

    private Drawable drawable;
    private ZoomView view;
    private DisplayMetrics dm;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        dm = getResources().getDisplayMetrics();
        view = new ZoomView(this);
        drawable = new TestDrawable(dm.widthPixels * FACTOR_X, dm.heightPixels * FACTOR_Y);
        view.setImageDrawable(drawable);
        setContentView(view);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int w = dm.widthPixels;
        int h = dm.heightPixels;
        
        int id = item.getItemId();
        switch (id) {
            case R.id.big:
                drawable = new TestDrawable(w * FACTOR_X, h * FACTOR_Y);
                break;
            case R.id.small:
                drawable = new TestDrawable(w / FACTOR_X, h / FACTOR_Y);
                break;
            case R.id.tall:
                drawable = new TestDrawable(w / FACTOR_X, h * FACTOR_Y);
                break;
            case R.id.wide:
                drawable = new TestDrawable(w * FACTOR_X, h / FACTOR_Y);
                break;
            case R.id.image:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        if (id != R.id.image) {
            view.setImageDrawable(drawable);
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                InputStream stream = getContentResolver().openInputStream(data.getData());
                drawable = Drawable.createFromStream(stream, data.getData().toString());
                view.setImageDrawable(drawable);
            } catch (FileNotFoundException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    class TestDrawable extends Drawable {

        private int w;
        private int h;
        private Paint paint;
        private RectF rect;
        private Paint outlinePaint;

        public TestDrawable(float w, float h) {
            this.w = (int) w;
            this.h = (int) h;
            final int SIDE = 20;
            float scale = Math.max(w, h) / SIDE;
            int[] colors = new int[SIDE * SIDE];
            float[] hsv = {
                    0, 1f, 0.8f
            };
            Random r = new Random();
            for (int i = 0; i < colors.length; i++) {
                int row = i / SIDE;
                if ((i + row) % 2 == 0) {
                    hsv[0] = r.nextInt(360);
                    hsv[2] = 0.5f + 0.4f * r.nextFloat();
                    colors[i] = Color.HSVToColor(hsv);
                } else {
                    colors[i] = 0xffeeeeee;
                }
            }
            Bitmap b = Bitmap.createBitmap(colors, SIDE, SIDE, Config.RGB_565);
            BitmapShader shader0 = new BitmapShader(b, TileMode.REPEAT, TileMode.REPEAT);
            Matrix m = new Matrix();
            m.preScale(scale, scale);
            m.preRotate(10);
            shader0.setLocalMatrix(m);
            paint = new Paint();
            outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outlinePaint.setStrokeWidth(5);
            outlinePaint.setColor(0xccff8800);
            outlinePaint.setStyle(Style.STROKE);
            
            colors = new int[] {
                    0x00ffffff, 0x55000088, 0x88000088,
            };
            float[] positions = {
                    0, 0.7f, 1
            };
            RadialGradient shader1 = new RadialGradient(w * 0.4f, h * 0.5f, 1 + Math.min(w, h) * 0.6f, colors, positions, TileMode.CLAMP);

            ComposeShader compose = new ComposeShader(shader0, shader1, Mode.SRC_OVER);
            paint.setShader(compose);
            paint.setDither(true);
            rect = new RectF();
        }
        
        @Override
        public int getIntrinsicWidth() {
            return w;
        }
        
        @Override
        public int getIntrinsicHeight() {
            return h;
        }

        @Override
        public void draw(Canvas canvas) {
            rect.set(getBounds());
            Rect b = getBounds();
            float side2 = Math.min(b.width(), b.height()) / 2f;
            float r = Math.min(80, side2);
            canvas.drawRoundRect(rect, r, r, paint);
            float delta = outlinePaint.getStrokeWidth() / 2;
            rect.inset(delta, delta);
            r -= delta;
            canvas.drawRoundRect(rect, r, r, outlinePaint);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
