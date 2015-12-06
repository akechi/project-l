package akechi.projectl.async;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.v4.util.Pair;
import android.text.BoringLayout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Collections;
import java.util.WeakHashMap;

public class InlineImageHandler
    extends AsyncTask<URLSpan, Void, Iterable<Pair<URLSpan, Bitmap>>>
{
    public InlineImageHandler(TextView view)
    {
        this.view= view;

        this.execute(this.view.getUrls());
    }

    @Override
    protected void onPostExecute(Iterable<Pair<URLSpan, Bitmap>> pairs)
    {
        if(!(this.view.getText() instanceof Spannable))
        {
            return;
        }
        for(final Pair<URLSpan, Bitmap> pair : pairs)
        {
            final URLSpan url= pair.first;
            final Bitmap bitmap= pair.second;
            if(url == null || bitmap == null)
            {
                continue;
            }
            final Spannable text= (Spannable)this.view.getText();
            final int start= text.getSpanStart(url);
            final int end= text.getSpanEnd(url);
            if(start == -1 || end == -1)
            {
                continue;
            }
            final ImageSpan imageSpan;
            if(bitmap.getWidth() <= this.view.getWidth())
            {
                imageSpan= new ImageSpan(this.view.getContext(), bitmap);
            }
            else
            {
                final Matrix mat= new Matrix();
                final float scale= this.view.getWidth() / (float)bitmap.getWidth();
                mat.postScale(scale, scale);
                final Bitmap scaled= Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
                imageSpan= new ImageSpan(this.view.getContext(), scaled);
            }
            text.setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    protected Iterable<Pair<URLSpan, Bitmap>> doInBackground(URLSpan... params)
    {
        if(params.length == 0)
        {
            return Collections.emptyList();
        }
        final ImmutableList.Builder<Pair<URLSpan, Bitmap>> builder= ImmutableList.builder();
        for(final URLSpan span : params)
        {
            final GenericUrl url= new GenericUrl(span.getURL());
            try
            {
                if(!this.isImageType(url))
                {
                    continue;
                }
                final Bitmap bitmap= this.loadBitmap(url);
                bitmapCache.put(span.getURL(), (bitmap != null)
                    ? bitmap
                    : BitmapFactory.decodeResource(this.view.getContext().getResources(), android.R.drawable.ic_delete)
                );
                builder.add(Pair.create(span, bitmap));
            }
            catch(IOException e)
            {
                Log.e("InlineImageHandler", "url: " + url);
                Log.e("InlineImageHandler", "Error", e);
                return Collections.emptyList();
            }
        }
        return builder.build();
    }

    private Bitmap loadBitmap(GenericUrl url)
    {
        {
            final Bitmap bitmap= bitmapCache.get(url.toString());
            if(bitmap != null)
            {
                return bitmap;
            }
        }

        HttpResponse response= null;
        try
        {
            final HttpRequestFactory factory= transport.createRequestFactory();
            final HttpRequest request= factory.buildGetRequest(url);
            response= request.execute();

            return BitmapFactory.decodeStream(response.getContent());
        }
        catch(IOException e)
        {
            Log.e("InlineImageHandler", "Network error", e);
            return null;
        }
        finally
        {
            if(response != null)
            {
                try
                {
                    response.disconnect();
                }
                catch(IOException e)
                {
                    Log.e("InlineImageHandler", "Error closing", e);
                }
            }
        }
    }

    private boolean isImageType(GenericUrl url)
        throws IOException
    {
        {
            final String contentType= mimeCache.get(url.toString());
            if(contentType != null)
            {
                return contentType.startsWith("image");
            }
        }

        final HttpRequestFactory factory= transport.createRequestFactory();
        HttpResponse response= null;
        try
        {
            final HttpRequest request= factory.buildHeadRequest(url);
            response= request.execute();
            final String contentType= Strings.nullToEmpty(response.getHeaders().getContentType());
            mimeCache.put(url.toString(), contentType);

            return contentType.startsWith("image");
        }
        finally
        {
            if(response != null)
            {
                response.disconnect();
            }
        }
    }

    private static final HttpTransport transport= AndroidHttp.newCompatibleTransport();

    private static final LruCache<String, String> mimeCache= new LruCache<>(1024);

    private static final LruCache<String, Bitmap> bitmapCache= new LruCache<String, Bitmap>(8 * 1024 * 1024){
        @Override
        protected int sizeOf(String key, Bitmap value)
        {
            if(value == null)
            {
                return 1;
            }
            return value.getRowBytes() * value.getHeight();
        }
    };

    private final TextView view;
}
