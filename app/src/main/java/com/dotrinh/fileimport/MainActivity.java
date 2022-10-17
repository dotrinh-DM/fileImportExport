package com.dotrinh.fileimport;

import static com.dotrinh.fileimport.LogUtil.LogI;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.OpenableColumns;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.dotrinh.fileimport.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public ActivityMainBinding binding;
    private static final int IMPORT_FILE = 1111;
    private static String SHARE_FOLDER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.importBtn.setOnClickListener(v -> importPatch());
        binding.exportBtn.setOnClickListener(v -> shareCommon());

        SHARE_FOLDER = getFilesDir().getPath() + "/share/";
        dirChecker(SHARE_FOLDER);

        File scanFile = new File(SHARE_FOLDER);
        if (Objects.requireNonNull(scanFile.list()).length == 0) {
            for (int i = 0; i < 5; i++) {
                try {
                    OutputStream outputStream = new FileOutputStream(SHARE_FOLDER + "abc_" + i + ".dotr");
                    String content = "noi dung " + i;
                    outputStream.write(content.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void importPatch() {
        Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        // pickIntent.setType("application/octet-stream");
        pickIntent.setType("*/*");
        pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
        //support for older than android 11
        startActivityForResult(Intent.createChooser(pickIntent, "Open with ..."), IMPORT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_FILE && resultCode == RESULT_OK) {
            ArrayList<Uri> uriArrayList = new ArrayList<>();
            if (data.getClipData() != null) {//select multiple file
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    uriArrayList.add(uri);
                }
            } else if (data.getData() != null) {//select single file
                uriArrayList.add(data.getData());
            }

            for (Uri item : uriArrayList) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = getContentResolver().openInputStream(item);
                    out = new FileOutputStream(getAppRootPath() + "/" + getFileNameFrom_Uri(item));
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        LogI("writing output: " + len);
                    }
                    LogI("DONE");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public String getFileNameFrom_Uri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public void shareCommon() {
        File scanFile = new File(SHARE_FOLDER);
        ArrayList<Uri> uriArr = new ArrayList<>();

        for (String str : Objects.requireNonNull(scanFile.list())) {
            File srcFile = new File(SHARE_FOLDER + str);
            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", srcFile);
            uriArr.add(uri);
            LogI("file da chon: " + srcFile.getAbsolutePath());
        }

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Intent intentShareFile = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intentShareFile.setType("application/octet-stream");
        intentShareFile.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriArr);
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "");
        intentShareFile.putExtra(Intent.EXTRA_TEXT, "");
        // grant uri permission
        Intent chooserIntent = Intent.createChooser(intentShareFile, "Export File");
        List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(chooserIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            for (Uri item : uriArr) {
                this.grantUriPermission(packageName, item, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
        startActivity(chooserIntent);
    }


    public String getAppRootPath() {
        PackageManager pM = getPackageManager();
        String root_folder = getPackageName();
        PackageInfo pI = null;
        try {
            pI = pM.getPackageInfo(root_folder, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        root_folder = pI.applicationInfo.dataDir;
        return root_folder;
    }

    public void copyFileUsingStream(File source, File dest) {
        try {
            InputStream is = new FileInputStream(source);
            OutputStream os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dirChecker(String _targetLocation) {
        File f = new File(_targetLocation);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }
}