
import org.apache.cordova.CordovaInterface;//
import org.apache.cordova.CordovaPlugin;//org.apache.cordova
import org.apache.cordova.CallbackContext;//org.apache.cordova
import org.apache.cordova.CordovaWebView;//org.apache.cordova
import org.apache.cordova.PluginResult;//org.apache.cordova

import android.content.ClipData;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.accounttransfer.AccountTransfer;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.io.InputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import static android.app.Activity.RESULT_OK;

public class GoogleDrive extends CordovaPlugin {

   
    
    private final int CODE_UPLOAD_FILE = 10;
    private final int CODE_AUTH = 11;
    private final int CODE_FOLDER_PICKER = 12;
    private final String TAG = "GoogleDrivePlugin";
    private final String FILE_MIME = "application/octet-stream";
    private final String SUCCESS_NORMAL_SIGN_IN = "SignIn Success with Normal SignIn";
    private final String SUCCESS_SILENT_SIGN_IN = "SignIn Success with Silent SignIn";
    private final String FAILED_SIGN_IN = "SignIn failed";
    private final String ENTER_STREAM = "Enter To Stream";
    private final String EXIT_STREAM = "Exit From Stream";
    private final String FILE_CREATED = "File Created";
    private final String ERROR_FILE = "Unable to create File";
    private final String INITIAL = "Assaf Google Drive Plugin Initiated";
    private final String EXECUTING = "Assaf Google Drive Plugin Is Executing";
    private final String SIGN_OUT = "SignOut From User:";
    private final String SIGN_IN_ACTION = "signIn";
    private final String SIGN_OUT_ACTION = "signOut";
    private final String PICK_FOLDER_ACTION = "pickFolder";
    private final String UPLOAD_FILE_WITH_FILE_PICKER_ACTION = "uploadFileWithPicker";
    private final String UPLOAD_FILE_ACTION = "uploadFile";
    private final String SIGN_IN_PROGRESS = "Assaf Google Drive Plugin Is Trying To Login";

    private final int BUFFER_SIZE = 1024;

    private Set<Scope> appScopes = new HashSet<Scope>();

    private GoogleSignInClient mGoogleSignInClient;
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;
    private TaskCompletionSource<DriveId> mOpenItemTaskSource;

    private CallbackContext mCallbackContext;
    private String mAction;
    private JSONArray mArgs;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.i(TAG, INITIAL);
    }

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext)
            throws JSONException {
        super.execute(action, args, callbackContext);
        Log.i(TAG, EXECUTING);
        mCallbackContext = callbackContext;
        mAction = action;
        mArgs = args;
        return selectActionToExecute();
    }

    private boolean selectActionToExecute() {

        if (SIGN_IN_ACTION.equals(mAction)) {
            Log.i(TAG, "executing: " +mAction);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        signIn();
                    } catch (Exception ex) {
                        mCallbackContext.error("Error: " + ex.getLocalizedMessage());
                    }

                }
            });
            return true;
        } else if (SIGN_OUT_ACTION.equals(mAction)) {
            Log.i(TAG, "executing: " +mAction);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        signOut();
                    } catch (Exception ex) {
                        mCallbackContext.error("Error: " + ex.getLocalizedMessage());
                    }

                }
            });
            return true;
        } else if (PICK_FOLDER_ACTION.equals(mAction)) {
            Log.i(TAG, "executing: " +mAction);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        pickFolder();
                    } catch (Exception ex) {
                        Log.i(TAG, "exception");
                        mCallbackContext.error("Error: " + ex.getLocalizedMessage());
                    }

                }
            });
            return true;

        } else if (UPLOAD_FILE_WITH_FILE_PICKER_ACTION.equals(mAction)) {
            Log.i(TAG, "executing: " +mAction);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {

                    try {
                        String driveFolderIdStr = mArgs.getString(0);
                        Log.i(TAG, driveFolderIdStr);
                        selectImage();
                    } catch (Exception e) {
                        Log.e(TAG, "Error: ", e);
                        mCallbackContext.error("Error " + e.getLocalizedMessage());
                    }
                }
            });
            return true;

        } else if (UPLOAD_FILE_ACTION.equals(mAction)) {
            Log.i(TAG, "executing: " +mAction);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {

                    try {
                        if (! silentSignIn()) {
                            throw new Exception("Error In Login");
                        }
                        final String driveFolderIdStr = mArgs.getString(0);
                        final JSONArray fileDetails = mArgs.getJSONArray(1);

                        Log.i(TAG, driveFolderIdStr);
                        Log.i(TAG, fileDetails.toString());

                        for (int i = 0; i < fileDetails.length(); i++) {
                            final JSONObject jsonObject=fileDetails.getJSONObject(i);
                            final String fileTitle = jsonObject.getString("title");
                            final String fileDescription = jsonObject.getString("description");
                            final String fileUriStr=jsonObject.getString("uri");
                            Uri uriFile = Uri.parse(fileUriStr);
                            uploadFile(driveFolderIdStr, uriFile, fileTitle, fileDescription);
                        }
                        Log.i(TAG, driveFolderIdStr);
                    } catch (Exception e) {
                        Log.e(TAG, "Error: ", e);
                        mCallbackContext.error("Error " + e.getLocalizedMessage());
                    }
                }
            });
            return true;

        }
        return false;
    }

    private boolean silentSignIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(cordova.getActivity());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
                .requestScopes(Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER).requestProfile().build();

        if (account != null && account.getGrantedScopes().containsAll(this.appScopes)) {

            mGoogleSignInClient = GoogleSignIn.getClient(cordova.getActivity(), gso);
            initializeDriveClient(account, SUCCESS_SILENT_SIGN_IN,false);
            return true;
        }
        return false;
    }

    private boolean silentSignIn(GoogleSignInAccount account, GoogleSignInOptions gso) {
        if (account != null && account.getGrantedScopes().containsAll(this.appScopes)) {

            mGoogleSignInClient = GoogleSignIn.getClient(cordova.getActivity(), gso);
            initializeDriveClient(account, SUCCESS_SILENT_SIGN_IN,true);
            return true;
        }
        return false;
    }

    private void signIn() {
        Log.i(TAG, SIGN_IN_PROGRESS);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(cordova.getActivity());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
                .requestScopes(Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER).requestProfile().build();

        if (! silentSignIn(account, gso)) {
            mGoogleSignInClient = GoogleSignIn.getClient(cordova.getActivity(), gso);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();

            cordova.setActivityResultCallback(this);
            cordova.getActivity().startActivityForResult(signInIntent, CODE_AUTH);

        }
    }

    private void initializeDriveClient(GoogleSignInAccount signInAccount, String type, boolean toSendBack) {
        mDriveClient = Drive.getDriveClient(cordova.getActivity(), signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(cordova.getActivity(), signInAccount);
        Log.i(TAG, type + " With Email: " + signInAccount.getEmail());
        if (toSendBack) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("email", signInAccount.getEmail());
                mCallbackContext.success(jsonObject);
            } catch (Exception ex) {
                Log.e(TAG, "Error: ", ex);
                mCallbackContext.error("Error: " + ex.getLocalizedMessage());
            }
        }

    }

    private void googleSignInAccountTask(Task<GoogleSignInAccount> task) {
        task.addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
            @Override
            public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                initializeDriveClient(googleSignInAccount, SUCCESS_NORMAL_SIGN_IN,true);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG, FAILED_SIGN_IN, e);
            }
        });
    }

    private void signOut() throws Exception {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(cordova.getActivity());
        if (account != null) {
            String accountEmail = account.getEmail();
            mGoogleSignInClient.signOut();
            Log.i(TAG, SIGN_OUT + " " + accountEmail);
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("email", accountEmail);
                mCallbackContext.success(jsonObject);
            } catch (JSONException e) {
                mCallbackContext.error("Error: " + e.getLocalizedMessage());
            }

        }
    }

    private void selectImage() throws Exception {
        if (! silentSignIn()) {
            throw new Exception("Error In Login");
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        cordova.setActivityResultCallback(GoogleDrive.this);
        cordova.getActivity().startActivityForResult(intent, CODE_UPLOAD_FILE);

    }

    private void uploadFile(final String folderId, final Uri filePath, final String fileName, final String description) {
        try {
            Log.i(TAG, "enter uploadFile");
            final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
            final DriveFolder driveFolder = getDriveFolder(folderId);
            Log.i(TAG, driveFolder.toString());

            Tasks.whenAll(createContentsTask).continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                @Override
                public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                    DriveFolder parent = driveFolder;
                    DriveContents contents = createContentsTask.getResult();
                    try {
                        OutputStream outputStream = contents.getOutputStream();
                        InputStream inputStream = cordova.getActivity().getContentResolver().openInputStream(filePath);
                        Log.i(TAG, ENTER_STREAM);
                        byte[] data = new byte[BUFFER_SIZE];

                        while (inputStream.read(data) != - 1) {
                            outputStream.write(data);
                        }

                        inputStream.close();
                        outputStream.close();
                        Log.i(TAG, EXIT_STREAM);

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(fileName)
                                .setMimeType(FILE_MIME).setDescription(description).build();

                        return mDriveResourceClient.createFile(parent, changeSet, contents);
                    } catch (Exception ex) {
                        Log.i(TAG, "Error:", ex);
                        return null;
                    }
                }
            }).addOnSuccessListener(cordova.getActivity(), new OnSuccessListener<DriveFile>() {
                @Override
                public void onSuccess(DriveFile driveFile) {
                    Log.i(TAG, FILE_CREATED + " With FileId: " + driveFile.getDriveId().toInvariantString());
                    mCallbackContext.success("Success File Uploaded");
                }
            }).addOnFailureListener(cordova.getActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, ERROR_FILE, e);
                    mCallbackContext.error("Failed File Uploaded " + e.getLocalizedMessage());
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, "Error", ex);
            mCallbackContext.error(ex.getLocalizedMessage());
        }
    }

    private DriveFolder getDriveFolder(String folderIdStr) {
        return DriveId.decodeFromString(folderIdStr).asDriveFolder();
    }

    private Task<DriveId> pickFolder() throws Exception {
        Log.i(TAG, "enter pick folder");
        if (! silentSignIn()) {
            throw new Exception("Error In Login");
        }
        OpenFileActivityOptions openOptions = new OpenFileActivityOptions.Builder()
                .setSelectionFilter(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE))
                .setActivityTitle("pick Folder").build();
        return pickItem(openOptions);
    }

    private Task<DriveId> pickItem(OpenFileActivityOptions openOptions) {

        mOpenItemTaskSource = new TaskCompletionSource<DriveId>();
        mDriveClient.newOpenFileActivityIntentSender(openOptions)
                .continueWith(new Continuation<IntentSender, Object>() {
                    @Override
                    public Object then(@NonNull Task<IntentSender> task) throws Exception {
                        Log.i(TAG, "enter continueWith");
                        cordova.setActivityResultCallback(GoogleDrive.this);
                        cordova.getActivity().startIntentSenderForResult(task.getResult(), CODE_FOLDER_PICKER, null, 0,
                                0, 0);
                        return null;
                    }
                });
        Log.i(TAG, "exit pickItem");
        return mOpenItemTaskSource.getTask();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            switch (requestCode) {

                case CODE_AUTH: {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
                    googleSignInAccountTask(task);
                    break;
                }

                case CODE_UPLOAD_FILE: {
                    final String driveFolderIdStr = mArgs.getString(0);
                    final String fileTitle = mArgs.getString(1);
                    final String fileDescription = mArgs.getString(2);
                    if (intent.getData() != null) {
                        Uri fileUri = intent.getData();
                        uploadFile(driveFolderIdStr, fileUri, fileTitle, fileDescription);

                    } else if (intent.getClipData() != null) {

                        ClipData clipData = intent.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri fileUri = clipData.getItemAt(i).getUri();
                            uploadFile(driveFolderIdStr, fileUri, fileTitle, fileDescription);
                        }
                    }
                }

                break;

                case CODE_FOLDER_PICKER: {
                    if (resultCode == RESULT_OK) {
                        DriveId driveId = intent.getParcelableExtra(OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID);
                        Log.i(TAG, driveId.toString());

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("id", driveId.toString());
                        mCallbackContext.success(jsonObject);

                        mOpenItemTaskSource.setResult(driveId);
                    } else {
                        mOpenItemTaskSource.setException(new RuntimeException("Unable to open file"));
                    }
                    break;
                }
            }
        } catch (
                Exception ex)

        {
            mCallbackContext.error("error " + ex.getLocalizedMessage());
        }
    }
}
