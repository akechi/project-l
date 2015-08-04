package akechi.projectl.component;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import akechi.projectl.R;

public class CachedImageView
    extends ImageView
{
    public CachedImageView(Context context) {
        super(context);
    }

    public CachedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CachedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageURI(Uri uri) {
        final String scheme= uri.getScheme();
        if("http".equals(scheme))
        {
            this.setVisibility(View.INVISIBLE);
            new ImageLoader(this, uri).execute();
        }
        else
        {
            super.setImageURI(uri);
        }
    }

    private static final class ImageLoader
        extends AsyncTask<Void, Void, Bitmap>
    {
        public ImageLoader(ImageView view, Uri uri)
        {
            this.view= view;
            this.uri= uri;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            final URL url;
            try{
                url= new URL(this.uri.toString());
            }
            catch(MalformedURLException e)
            {
                return null;
            }
            final File cacheDir= new File(this.view.getContext().getCacheDir(), "icons");
            if(!cacheDir.isDirectory())
            {
                if(!cacheDir.mkdirs())
                {
                    throw new RuntimeException("Couldn't create directories " + cacheDir.getAbsolutePath());
                }
            }
            final File cacheFile= new File(cacheDir, escape(url));
            if(cacheFile.exists())
            {
                try
                {
                    return BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
                }
                catch(Exception e)
                {
                    Log.e("CachedImageView", "Illegal icon cache?", e);
                }
            }

            // ensure load once
            final boolean loading;
            synchronized(lock)
            {
                loading= loadingUris.contains(this.uri);
                if(!loading)
                {
                    loadingUris.add(this.uri);
                }
            }
            if(loading) {
                while (loadingUris.contains(this.uri)) {
                    Log.i(LOG_TAG, "Loading " + this.uri + ", wait it");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                // expects it was cached
                return this.doInBackground();
            }

            InputStream in= null;
            try
            {
                in= url.openStream();
                final Bitmap bitmap= BitmapFactory.decodeStream(in);
                OutputStream out= null;
                try
                {
                    out= new FileOutputStream(cacheFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                catch(IOException e)
                {
                    Log.e(LOG_TAG, "Couldn't write cache image to " + cacheFile, e);
                }
                finally
                {
                    if(out != null)
                    {
                        try
                        {
                            out.close();
                        }
                        catch(IOException e)
                        {
                            Log.e(LOG_TAG, "Couldn't close " + cacheFile, e);
                        }
                    }
                }
                return bitmap;
            }
            catch(IOException e) {
                Log.e(LOG_TAG, "Couldn't retrieve image content from url " + url, e);
                return null;
            }
            finally
            {
                if(in != null)
                {
                    try
                    {
                        in.close();
                    }
                    catch (IOException e) {
                        Log.e(LOG_TAG, "Couldn't close stream of " + url, e);
                    }
                }
                synchronized(lock)
                {
                    loadingUris.remove(this.uri);
                }
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            this.view.setImageBitmap(bitmap);
            this.view.setVisibility(View.VISIBLE);
        }

        private static String escape(URL url)
        {
            try {
                return URLEncoder.encode(url.toString(), "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }

        private static final Object lock= new Object();

        private static final Set<Uri> loadingUris= new HashSet<>();

        private final ImageView view;
        private Uri uri;
    }

    private static final String LOG_TAG= CachedImageView.class.getSimpleName();
}