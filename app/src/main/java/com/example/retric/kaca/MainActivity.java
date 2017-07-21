package com.example.retric.kaca;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static android.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL;
import static com.example.retric.kaca.R.id.textView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
        public ImageView mImageView;
        public String mCurrentPhotoPath;
        public String mCurrentPhotoName;
        public String mCurrentCopressedPhotoPath;
        public EditText editText;
        public String photo_description = " ";
        Handler myHandler;
        ProgressDialog progressDialog;
        ProgressDialog progressDialog_upload;
        String[] PhotoLocation;
        String PhotoTime;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mImageView = (ImageView) findViewById(R.id.imageView2);
        Button button =(Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                showcamera();
            }
        });

        Button button2 =(Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                //File f = new File(mCurrentPhotoPath);
                //Uri contentUri = Uri.fromFile(f);
                //uploadFile(contentUri);
                //先压缩图片
                //compress_photo();

                try {
                    saveExif(mCurrentPhotoPath, mCurrentPhotoPath);
                }catch (Exception e){

                }
            }
        });

        editText = (EditText) findViewById(R.id.editText);

        //detect if gps is enabled
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            displayPromptForEnablingGPS(this);
        }


        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("正在压缩图片，请稍等...");
        progressDialog_upload = new ProgressDialog(MainActivity.this);
        progressDialog_upload.setMessage("正在上传图片，请稍等...");

        myHandler = new Handler() {
            public void handleMessage(Message msg) {

                /**
                 * Progressbar to Display if you need

                final ProgressDialog progressDialog;
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("正在压缩图片，请稍等...");

                final ProgressDialog progressDialog_upload;
                progressDialog_upload = new ProgressDialog(MainActivity.this);
                progressDialog_upload.setMessage("正在上传图片，请稍等...");
                 */


                if(msg.getData().get("status") == "start"){
                    progressDialog.show();
                }
                if(msg.getData().get("status") == "finished"){
                    progressDialog.dismiss();
                    //开始上传图片
                    //File f = new File(mCurrentCopressedPhotoPath);
                    //Uri contentUri = Uri.fromFile(f);
                    progressDialog_upload.show();
                    uploadFile();
                }
                if(msg.getData().get("status") == "upload_success"){
                    progressDialog_upload.dismiss();
                    Log.v("Upload", "success");
                    deleteImage(mCurrentPhotoPath);
                    deleteImage(mCurrentCopressedPhotoPath);
                    Toast.makeText(MainActivity.this,
                            "上传成功！", Toast.LENGTH_LONG).show();
                }
                if(msg.getData().get("status") == "upload_failed"){
                    progressDialog_upload.dismiss();
                    Toast.makeText(MainActivity.this,
                            "上传失败", Toast.LENGTH_LONG).show();
                }



            }
        };








    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    final int REQUEST_IMAGE_CAPTURE = 1;
    final int REQUEST_TAKE_PHOTO = 1;

    private void showcamera(){


            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                //startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.d("TAG", "showcamera: unable to create photofile");

                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.example.android.fileprovider",
                            photoFile);


                    //for android 4.4 and below the fileprovider has some isuee...which need to grab athority..
                    //more info visit https://stackoverflow.com/a/33652695/6544410
                    List<ResolveInfo> resInfoList = MainActivity.this.getPackageManager().queryIntentActivities(takePictureIntent, getPackageManager().MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        MainActivity.this.grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    //for android 4.4 and below the fileprovider has some isuee...which need to grab athority..
                    //more info visit https://stackoverflow.com/a/33652695/6544410


                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    //Log.d("aaaaaaaa", "showcamera: ");


                }
                else{
                    Log.d("TAG", "showcamera: photoFile == null");
                }


            }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {






                Runnable mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Message msg = myHandler.obtainMessage();
                            //开始压缩及上传任务，通知ui现在show出载入框
                            Bundle b1 = new Bundle();
                            b1.putString("status", "start");
                            msg.setData(b1);
                            myHandler.sendMessage(msg);


                            //压缩图片
                            compress_photo();

                            //压缩完毕后通知ui把载入框dismiss掉
                            Message msg2 = myHandler.obtainMessage();
                            Bundle b2 = new Bundle();
                            b2.putString("status", "finished");
                            msg2.setData(b2);
                            myHandler.sendMessage(msg2);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                Thread thread = new Thread(mRunnable);
                thread.start();

            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }





    private File createImageFile() throws IOException {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            // for compressed img
            mCurrentPhotoName = imageFileName+".jpg";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = image.getAbsolutePath();
            Log.d("photopath", mCurrentPhotoPath);
            return image;
        }

    private void uploadFile() {

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .connectTimeout(120, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kacaweb.leanapp.cn/kaca/")
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        // create upload service client
        FileUploadService service = retrofit.create(FileUploadService.class);
        File file = new File(mCurrentCopressedPhotoPath);

        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(
                        //MediaType.parse(this.getContentResolver().getType(fileUri)),
                        MediaType.parse("image/jpg"),
                        file
                );

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("picture", file.getName(), requestFile);

        // add another part within the multipart request
        if(editText.getText().toString()!=""){
            photo_description =  editText.getText().toString();
        }
        String descriptionString = photo_description;
        RequestBody description =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, descriptionString);

        // finally, execute the request
        Call<ResponseBody> call = service.upload(description, body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {

                Log.d("upload-response", "onResponse: "+response.message());

                //progressDialog.dismiss();
                Message msg = myHandler.obtainMessage();
                Bundle b1 = new Bundle();
                b1.putString("status", "upload_success");
                msg.setData(b1);
                myHandler.sendMessage(msg);

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload error:", t.getMessage());
                //progressDialog.dismiss();
                Message msg = myHandler.obtainMessage();
                Bundle b1 = new Bundle();
                b1.putString("status", "upload_failed");
                msg.setData(b1);
                myHandler.sendMessage(msg);

            }
        });
    }

    public void deleteImage(String filepath) {

        File fdelete = new File(filepath);
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.e("-->", "file Deleted :" + filepath);
            } else {
                Log.e("-->", "file not Deleted :" + filepath);
            }
        }
    }

    public static void displayPromptForEnablingGPS(final Activity activity)
    {

        final AlertDialog.Builder builder =  new AlertDialog.Builder(activity);
        final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        final String message = "Do you want open GPS setting?";

        builder.setMessage(message)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                activity.startActivity(new Intent(action));
                                d.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.cancel();
                            }
                        });
        builder.create().show();
    }

    public void compress_photo(){

        // open photo
        File image = new File(mCurrentPhotoPath);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
        //压缩大小的边界值
        int maxkb = 500;
        //L.showlog(压缩图片);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        int options = 100;
        // 循环判断如果压缩后图片是否大于(maxkb)200kb,大于继续压缩
        while (baos.toByteArray().length / 1024 > maxkb) {
            // 重置baos即清空baos
            baos.reset();
            if(options-10>0){
                // 每次都减少10
                options -= 10;
            }
            // 这里压缩options%，把压缩后的数据存放到baos中
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
            Log.d("compress_photo:",""+options);
        }
        // 把压缩后的数据baos存放到ByteArrayInputStream中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        // 把ByteArrayInputStream数据生成图片
        Bitmap bitmap2 = BitmapFactory.decodeStream(isBm, null, null);
        //生成图片文件，不覆盖原图
        try {
            mCurrentCopressedPhotoPath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)+"/compressed_"+mCurrentPhotoName;
            FileOutputStream fos = new FileOutputStream(mCurrentCopressedPhotoPath);
            byte[] bitmapdata = baos.toByteArray();
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            Log.d("compress_photo:","finished!!! ");
            saveExif(mCurrentPhotoPath,mCurrentCopressedPhotoPath);

        }catch (Exception e){

        }
    }



    public static void saveExif(String oldFilePath, String newFilePath) throws Exception {
        ExifInterface oldExif=new ExifInterface(oldFilePath);
        ExifInterface newExif=new ExifInterface(newFilePath);
        Class<ExifInterface> cls = ExifInterface.class;
        Field[] fields = cls.getFields();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i].getName();
            if (!TextUtils.isEmpty(fieldName) && fieldName.startsWith("TAG")) {
                String fieldValue = fields[i].get(cls).toString();
                String attribute = oldExif.getAttribute(fieldValue);
                if (attribute != null) {
                    newExif.setAttribute(fieldValue, attribute);
                }
            }
        }
        newExif.saveAttributes();
    }





}
