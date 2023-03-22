package com.intellus.intellusdesign.activtiy

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.intellus.intellusdesign.R
import com.intellus.intellusdesign.constant.App
import com.intellus.intellusdesign.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var context: Context

    //val webUrl: String = "https://jansath.com/"
    var current_url: String = App.URL
    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }


    private val FILECHOOSER_RESULTCODE = 1
    private var mUploadMessage: ValueCallback<Uri>? = null
    private var mCapturedImageURI: Uri? = null

    // the same for Android 5.0 methods only
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        context = this

        UpdateApp()

        binding.swipeRefresh.setOnRefreshListener {
            showProgressBar()
            bindWeb(current_url)
            binding.swipeRefresh.isRefreshing = false
        }

        bindWeb(App.URL)

    }

    fun bindWeb(webUrl: String) {
        var loadingFinished = true;
        var redirect = false;
        binding.webView.apply {
            loadUrl(webUrl)
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true  // api 26
            }

        }
        binding.webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                url: String
            ): Boolean {

                if (url.contains("tel:") || url.contains("whatsapp:")) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    Log.e("TAG", "shouldOverrideUrlLoading: whstapp and telephone ${intent.data}")
                    startActivity(intent)

                }

                if (url.startsWith("http:") || url.startsWith("https:")) {
                    if (url.contains("https://m.facebook.com/"))
                        isPackageInstalled("com.facebook.android", context, url)
                    else if (url.contains("https://www.instagram.com/"))
                        isPackageInstalled("com.instagram.android", context, url)
                    else if (url.contains("https://api.whatsapp.com/"))
                        isPackageInstalled("com.whatsapp", context, url)
                    else if (url.contains("https://twitter.com/"))
                        isPackageInstalled("com.twitter.android", context, url)
                    else
                        view?.loadUrl(url)


                }

                return true

            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                showProgressBar()
                current_url = url.toString()

                /*  if ((current_url).contains("tel:")) {
                      Log.e("TAG", "shouldOverrideUrlLoading: current url $current_url ")
                      val intent = Intent(Intent.ACTION_VIEW)
                      intent.data = Uri.parse(current_url)
                      startActivity(intent)
                      //finish()
                  } else
                      showProgressBar()
  */


                Log.e("TAG", "onPageStarted:$current_url ")
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                hideProgressBar()
                super.onPageFinished(view, url)
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            @Deprecated("Deprecated in Java")


            override// for Lollipop, all in one
            fun onShowFileChooser(
                webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                mFilePathCallback?.onReceiveValue(null)
                mFilePathCallback = filePathCallback
                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(packageManager) != null) {

                    // create the file where the photo should go
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent!!.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.e("TAG", "Unable to create Image File", ex)
                    }

                    // continue only if the file was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile!!.getAbsolutePath()
                        takePictureIntent!!.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile)
                        )
                    } else {
                        takePictureIntent = null
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"
                val intentArray: Array<Intent?> =
                    takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.image_chooser))
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
                return true
            }

            // creating image files (Lollipop only)
            @Throws(IOException::class)
            private fun createImageFile(): File? {
                var imageStorageDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "DirectoryNameHere"
                )
                if (!imageStorageDir.exists()) {
                    imageStorageDir.mkdirs()
                }

                // create an image file name
                imageStorageDir = File(
                    (File.separator + imageStorageDir).toString() + "IMG_" + System.currentTimeMillis()
                        .toString() + ".jpg"
                )
                return imageStorageDir
            }

            // openFileChooser for Android 3.0+
            fun openFileChooser(uploadMsg: ValueCallback<Uri>?, acceptType: String?) {
                mUploadMessage = uploadMsg
                try {
                    val imageStorageDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "DirectoryNameHere"
                    )
                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs()
                    }
                    val file = File(
                        (File.separator + imageStorageDir).toString() + "IMG_" + System.currentTimeMillis()
                            .toString() + ".jpg"
                    )
                    mCapturedImageURI = Uri.fromFile(file) // save to the private variable
                    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
                    // captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    i.type = "*/*"
                    val chooserIntent = Intent.createChooser(i, getString(R.string.image_chooser))
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))
                    startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
                } catch (e: Exception) {
                    Toast.makeText(baseContext, "Camera Exception:$e", Toast.LENGTH_LONG).show()
                }
            }

            // openFileChooser for Android < 3.0
            fun openFileChooser(uploadMsg: ValueCallback<Uri>?) {
                openFileChooser(uploadMsg, "")
            }

            // openFileChooser for other Android versions
            /* may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
               https://code.google.com/p/android/issues/detail?id=62220
               however newer versions of KitKat fixed it on some devices */
            fun openFileChooser(
                uploadMsg: ValueCallback<Uri>?,
                acceptType: String?,
                capture: String?
            ) {
                openFileChooser(uploadMsg, acceptType)
            }

        }


    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack())
            binding.webView.goBack()
        else
            super.onBackPressed()
    }

    fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
        binding.webView.visibility = View.VISIBLE

    }

    fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
        binding.webView.visibility = View.INVISIBLE

    }


    fun UpdateApp() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            if ((it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) && it.isUpdateTypeAllowed(
                    AppUpdateType.IMMEDIATE
                )
            )
                appUpdateManager.startUpdateFlowForResult(
                    it,
                    AppUpdateType.IMMEDIATE,
                    this,
                    App.UPDATE_RESULT_CODE
                )
            Log.e("TAG", "UpdateApp: Success to check for an update  ")
        }
            .addOnFailureListener {
                Log.e("TAG", "UpdateApp: Failed to check for an update  ")
            }
    }


    fun isPackageInstalled(packageName: String, context: Context, url: String) {
        val uri = Uri.parse(url)
        val linking = Intent(Intent.ACTION_VIEW, uri)
        linking.setPackage(packageName)
        Log.e("TAG", "isPackageInstalled: $packageName ")
        try {
            startActivity(linking)
            Log.e("TAG", "isPackageInstalled: linking $linking ")

        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            Log.e("TAG", "isPackageInstalled:  exception ${e.message}")

        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                appUpdateManager.startUpdateFlowForResult(
                    it,
                    AppUpdateType.IMMEDIATE,
                    this,
                    App.UPDATE_RESULT_CODE
                )

        }
    }

    /*  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
          super.onActivityResult(requestCode, resultCode, data)
          if (resultCode == App.UPDATE_RESULT_CODE)
              finish()
      }*/

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // code for all versions except of Lollipop
        // end of code for all versions except of Lollipop

        // start of code for Lollipop only
        if (requestCode != FILECHOOSER_RESULTCODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        var results: Array<Uri>? = null

        // check that the response is a good one
        if (resultCode == RESULT_OK) {
            if (data == null || data.data == null) {
                // if there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
        // end of code for Lollipop only
    }


}