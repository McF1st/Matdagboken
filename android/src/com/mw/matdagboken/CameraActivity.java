package com.mw.matdagboken;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class CameraActivity extends Activity
{
	private LinearLayout mLayout;
	private ImageView mImageView;
	private static final int REQUEST_CODE_CAMERA_PHOTO = 0x40a46757; //generated from http://www.guidgen.com/
	private File mImageFile = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int size = Math.min(metrics.heightPixels, metrics.widthPixels);
		
		mImageView = new ImageView(this);
		mImageView.setImageResource(R.drawable.ic_camera_background);
		mImageView.setScaleType(ScaleType.CENTER_INSIDE);
		mImageView.setLayoutParams(new LayoutParams(size, size));
		mImageView.requestLayout();

		mLayout = new LinearLayout(this);
		mLayout.setBackgroundColor(0xFFFFFFFF);
		mLayout.setOrientation(LinearLayout.VERTICAL);
		mLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mLayout.addView(mImageView);

		setContentView(mLayout);
		
		tryStartCameraActivity();
	}
	
	private static File tryCreateCameraImageFile()
	{
		if (!FileHelper.isExternalStorageWritable())
			return null;
		
	    // Create an image file name
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
	    String timeStamp = dateFormat.format(new Date());
	    String imageFileName = timeStamp + ".jpg";
	    File storageDir = new File(FileHelper.getCameraStoragePath().getAbsoluteFile() + "/Camera/" );
	    
	    if (storageDir.mkdirs() || storageDir.exists())
	    {
		    try
	        {
		    	File image = new File(storageDir, imageFileName);
		    	image.createNewFile();
		    	return image;
	        } 
	        catch (IOException ex) 
	        {
	            //TODO make dialogue saying there is not enough space left or whatever fails...
	        	ex.printStackTrace();
	        }
	    }
	    
	    return null;
	}
	
	private static Bitmap decodeAndCropFileToFitSize(File imageFile, int targetSize)
	{
	    // Get the dimensions of the bitmap
	    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	    bmOptions.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);
	    int photoW = bmOptions.outWidth;
	    int photoH = bmOptions.outHeight;
	    int minPhotoSize = Math.min(photoW, photoH);
	    
	    //This indicates that we failed to decode the image and can not proceed
	    if (minPhotoSize <= 0)
	    	return null;
	    
	    //Try to get target size
	    targetSize = Math.min(targetSize, minPhotoSize);

	    // Determine how much to scale down the image
	    int scaleFactor = Math.max(1, Math.min(photoW/targetSize, photoH/targetSize));

	    // Decode the image file into a Bitmap sized to fill the View
	    bmOptions.inJustDecodeBounds = false;
	    bmOptions.inSampleSize = scaleFactor;
	    bmOptions.inPurgeable = true;
	    
	    //This will decode the bitmap into memory
	    Bitmap fullBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);
	    
	    photoW = bmOptions.outWidth;
	    photoH = bmOptions.outHeight;
	    minPhotoSize = Math.min(photoW, photoH);
	    if (minPhotoSize <= 0)
	    	return null;
	    
	    //Create the target bitmap
	    Bitmap croppedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
	    Canvas canvas = new Canvas(croppedBitmap);
	    
	    //Paint the loaded image into the new bitmap which later on will be saved
	    int size = Math.min(photoW, photoH);
	    int x = (photoW - size) / 2;
	    int y = (photoH - size) / 2;
	    Rect srcRect = new Rect(x, y, x + size, y + size);
	    Rect dstRect = new Rect(0, 0, targetSize, targetSize);
	    canvas.drawBitmap(fullBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));

	    fullBitmap.recycle();
	    return croppedBitmap;
	}
	
	private void tryStartCameraActivity()
	{
		PackageManager pm = getPackageManager();
		if (pm != null)
		{
			if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
			{
				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				ComponentName cameraActivity = cameraIntent.resolveActivity(pm);
			    if (cameraActivity != null) 
			    {
			    	//note this function may return null if it fails
			    	mImageFile = tryCreateCameraImageFile();
			   
			        // Continue only if the File was successfully created
			        if (mImageFile != null) 
			        {
			        	cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mImageFile));
			        	startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA_PHOTO);
			        }
			    }
			}
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
	    if (requestCode == REQUEST_CODE_CAMERA_PHOTO && resultCode == Activity.RESULT_OK)
	    {
	    	if (mImageFile != null && mImageFile.exists())
	    	{
	    		Bitmap bitmap = CameraActivity.decodeAndCropFileToFitSize(mImageFile, 512);
	    		mImageView.setImageBitmap(bitmap);
	    	}
	    }
	}
}
