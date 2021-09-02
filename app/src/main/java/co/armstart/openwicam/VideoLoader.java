package co.armstart.openwicam;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import com.veinhorn.scrollgalleryview.Constants;
import com.veinhorn.scrollgalleryview.VideoPlayerActivity;
import com.veinhorn.scrollgalleryview.loader.MediaLoader;

/**
 * Created by lyf on 2016-09-26.
 */

public class VideoLoader implements MediaLoader {
    protected String mURL;

    public VideoLoader(String url) {
        mURL = url;
    }
    @Override
    public boolean isImage() {
        return false;
    }

    @Override
    public void loadMedia(final Context context, ImageView imageView, SuccessCallback callback) {
        Bitmap bm = ThumbnailUtils.createVideoThumbnail(mURL, MediaStore.Video.Thumbnails.MINI_KIND);
        if (bm == null) {
            bm = ((BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.video_placeholder)).getBitmap();
        }
        imageView.setImageBitmap(bm);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayVideo(context, mURL);
            }
        });
    }

    @Override
    public void loadThumbnail(Context context, ImageView thumbnailView, SuccessCallback callback) {
        Bitmap bm = ThumbnailUtils.createVideoThumbnail(mURL, MediaStore.Video.Thumbnails.MICRO_KIND);
        if (bm == null) {
            bm = ((BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.video_thumbnail)).getBitmap();
        }
        thumbnailView.setImageBitmap(bm);
        callback.onSuccess();
    }

    private void displayVideo(Context context, String url) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(Constants.URL, url);
        context.startActivity(intent);
    }
}
