package co.armstart.openwicam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.veinhorn.scrollgalleryview.loader.MediaLoader;

/**
 * Created by lyf on 2016-09-26.
 */

public class PhotoLoader implements MediaLoader {
    protected String mURL;

    public PhotoLoader(String url) {
        mURL = url;
    }
    @Override
    public boolean isImage() {
        return true;
    }

    @Override
    public void loadMedia(Context context, ImageView imageView, SuccessCallback callback) {
        imageView.setImageBitmap(BitmapFactory.decodeFile(mURL));
        if (callback != null) {
            callback.onSuccess();
        }
    }

    @Override
    public void loadThumbnail(Context context, ImageView thumbnailView, SuccessCallback callback) {
        thumbnailView.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeFile(mURL), 100, 100, false));
        if (callback != null) {
            callback.onSuccess();
        }
    }
}
