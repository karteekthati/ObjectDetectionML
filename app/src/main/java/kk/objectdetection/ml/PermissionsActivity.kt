package kk.objectdetection.ml

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import kk.objectdetection.ml.databinding.ActivityPermissionsBinding


class PermissionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionsBinding
    private var isPermissionRequestOngoing = false


    companion object {
        private const val APP_SETTINGS_REQUEST = 123 // You can use any unique request code
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermission()

        binding.btnPermissions.setOnClickListener { requestPermission() }

        // Check if the permission is already granted, and if so, navigate to the main activity
        if (isCameraPermissionGranted()) {
            val intent = Intent(this@PermissionsActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermission() {
        if (isPermissionRequestOngoing) {
            // A permission request is already ongoing, don't start another
            return
        }

        isPermissionRequestOngoing = true

        Dexter.withContext(this)
            .withPermission(android.Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    isPermissionRequestOngoing = false
                    val intent = Intent(this@PermissionsActivity, MainActivity::class.java)
                    startActivity(intent)
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    isPermissionRequestOngoing = false
                    // Handle the case where the user denied the permission
                    if (response.isPermanentlyDenied) {
                        // Permission is permanently denied, show a dialog to guide the user to app settings
                        showPermissionRationale()
                    } else {
                        // Permission is denied but not permanently, you can request it again
                        requestPermission()
                    }

                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: com.karumi.dexter.listener.PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    isPermissionRequestOngoing = false
                    p1?.continuePermissionRequest()
                }
            }).check()
    }

    private fun showPermissionRationale() {

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("You have denied camera permission. You must grant the permission in settings to proceed.")
            .setPositiveButton("Settings") { _, _ ->
                // Open the app settings using the new ActivityResultLauncher
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                appSettingsLauncher.launch(intent)
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }

    private val appSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Check the result and handle it if needed
            if (result.resultCode == Activity.RESULT_OK) {
                // The user might have granted the permission from the app settings
                // You can check the permission status here and take appropriate action
                if (isCameraPermissionGranted()) {
                    // Permission granted, proceed with your logic
                    val intent = Intent(this@PermissionsActivity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Permission is still not granted, you can show a message or take further action
                    Toast.makeText(
                        applicationContext,
                        "Camera Permission not granted cannot proceed further",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
}