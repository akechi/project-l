package akechi.projectl.async;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.util.GenericData;
import com.google.api.client.util.IOUtils;
import com.google.api.client.util.Maps;

import org.apache.http.client.utils.URLEncodedUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class GyazoUploader
    extends AsyncTaskLoader<Uri>
{
    public GyazoUploader(Context context, Uri imageUri)
    {
        super(context);

        checkNotNull(imageUri);

        this.imageUri= imageUri;
    }

    @Override
    public Uri loadInBackground()
    {
        try
        {
            final GenericUrl url= new GenericUrl("https://upload.gyazo.com");
            url.appendRawPath("/upload.cgi");

            final HttpContent content;
            {
                final MultipartContent data= new MultipartContent();
                data.setMediaType(new HttpMediaType("multipart/form-data"));
                data.setBoundary("__END_OF_PART__");
                data.addPart(new MultipartContent.Part(
                    new HttpHeaders()
                        .set("Content-Disposition", "form-data; name=\"id\""),
                    new EmptyContent()
                ));
                data.addPart(new MultipartContent.Part(
                    new HttpHeaders()
                        .set("Content-Disposition", "form-data; name=\"imagedata\"; filename=\"gyazo.com\""),
                    new ByteArrayContent(null, this.readBytes())
                ));

                content= data;
            }

            final HttpRequestFactory factory= transport.createRequestFactory(new HttpRequestInitializer(){
                @Override
                public void initialize(HttpRequest request)
                    throws IOException
                {
                    request.setLoggingEnabled(true);
                }
            });
            final HttpRequest request= factory.buildPostRequest(url, content);
            final HttpResponse response= request.execute();
            Log.i("gyazo", String.format("code=%d, content=%s", response.getStatusCode(), response.toString()));

            if(response.getStatusCode() >= 200 && response.getStatusCode() < 300)
            {
                ByteArrayOutputStream out= null;
                try
                {
                    out= new ByteArrayOutputStream();

                    IOUtils.copy(response.getContent(), out);
                    return Uri.parse(out.toString());
                }
                finally
                {
                    response.disconnect();

                    if(out != null)
                    {
                        out.close();
                    }
                }
            }
            else
            {
                return null;
            }
        }
        catch(IOException e)
        {
            Log.e("gyazo", "Oops", e);
            return null;
        }
    }

    private byte[] readBytes()
        throws IOException
    {
        final ByteArrayOutputStream ostream= new ByteArrayOutputStream();
        IOUtils.copy(this.getContext().getContentResolver().openInputStream(this.imageUri), ostream);
        return ostream.toByteArray();
    }

    private File writeFile()
        throws IOException
    {
        final File file= File.createTempFile("project-l-gyazo-upload", "", this.getContext().getCacheDir());
        OutputStream out= null;
        try
        {
            out= new FileOutputStream(file);
            IOUtils.copy(this.getContext().getContentResolver().openInputStream(this.imageUri), out);
            return file;
        }
        finally
        {
            if(out != null)
            {
                out.close();
            }
        }
    }

    private static final HttpTransport transport= AndroidHttp.newCompatibleTransport();

    private final Uri imageUri;
}
