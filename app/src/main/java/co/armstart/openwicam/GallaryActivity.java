package co.armstart.openwicam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.veinhorn.scrollgalleryview.MediaInfo;
import com.veinhorn.scrollgalleryview.ScrollGalleryView;
import com.veinhorn.scrollgalleryview.loader.DefaultImageLoader;
import com.veinhorn.scrollgalleryview.loader.DefaultVideoLoader;
import com.veinhorn.scrollgalleryview.loader.MediaLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GallaryActivity extends AppCompatActivity {


    protected ScrollGalleryView mScrollGalleryView;

    private static final ArrayList<String> images = new ArrayList<>(Arrays.asList(
            "http://img1.goodfon.ru/original/1920x1080/d/f5/aircraft-jet-su-47-berkut.jpg",
            "http://www.dishmodels.ru/picture/glr/13/13312/g13312_7657277.jpg",
            "http://img2.goodfon.ru/original/1920x1080/b/c9/su-47-berkut-c-37-firkin.jpg"
    ));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallary);

        mScrollGalleryView = (ScrollGalleryView) findViewById(R.id.scroll_gallery_view);

        //
        List<MediaInfo> infos = new ArrayList<>(images.size());
        Wicam.MEDIA_PATH.mkdirs();
        File[] files = Wicam.MEDIA_PATH.listFiles();
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".jpeg")) {
                infos.add(MediaInfo.mediaLoader(new PhotoLoader(file.getAbsolutePath())));
            } else if (file.getName().toLowerCase().endsWith(".mp4")) {
                infos.add(MediaInfo.mediaLoader(new VideoLoader(file.getAbsolutePath())));
            }
        }
        mScrollGalleryView
                .setThumbnailSize(100)
                .setZoom(true)
                .setFragmentManager(getSupportFragmentManager())
                .addMedia(infos);


    }


}
