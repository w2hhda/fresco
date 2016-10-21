package com.example.demo;

import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Uri uri = Uri.parse("android.resource://com.meizu.yellowpage/drawable/mz_avastar_person_06");
        SimpleDraweeView draweeView = (SimpleDraweeView) findViewById(R.id.my_image_view);
        draweeView.setImageURI(uri);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);

        try {
            Resources resources = getResources(uri);
            int id = getResourceId(resources, uri);
            Log.d("hhda", "=== " + id + " ===> " + getLength(uri));
            InputStream inputStream = resources.openRawResource(id);
            imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    private int getLength(Uri imageRequest) {
        AssetFileDescriptor fd = null;
        try {
            Resources resources = getResources(imageRequest);
            fd = resources.openRawResourceFd(getResourceId(resources, imageRequest));
            return (int) fd.getLength();
        } catch (Resources.NotFoundException e) {
            return -1;
        } catch (FileNotFoundException e) {
            return -1;
        } finally {
            try {
                if (fd != null) {
                    fd.close();
                }
            } catch (IOException ignored) {
                // There's nothing we can do with the exception when closing descriptor.
            }
        }
    }

    static int getResourceId(Resources resources, Uri data) throws FileNotFoundException {

        String pkg = data.getAuthority();
        if (pkg == null) throw new FileNotFoundException("No package provided: " + data);

        int id;
        List<String> segments = data.getPathSegments();
        if (segments == null || segments.isEmpty()) {
            throw new FileNotFoundException("No path segments: " + data);
        } else if (segments.size() == 1) {
            try {
                id = Integer.parseInt(segments.get(0));
            } catch (NumberFormatException e) {
                throw new FileNotFoundException("Last path segment is not a resource ID: " + data);
            }
        } else if (segments.size() == 2) {
            String type = segments.get(0);
            String name = segments.get(1);

            id = resources.getIdentifier(name, type, pkg);
        } else {
            throw new FileNotFoundException("More than two path segments: " + data);
        }
        return id;
    }

    Resources getResources(Uri data) throws FileNotFoundException {

        String pkg = data.getAuthority();
        if (pkg == null) throw new FileNotFoundException("No package provided: " + data);
        try {
            return getPackageManager().getResourcesForApplication(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            throw new FileNotFoundException("Unable to obtain resources for package: " + data);
        }
    }
}
