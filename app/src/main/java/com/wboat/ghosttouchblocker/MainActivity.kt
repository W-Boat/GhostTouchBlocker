package com.wboat.ghosttouchblocker

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wboat.ghosttouchblocker.databinding.ActivityMainBinding
import java.io.DataOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isBlocking = false
    private var selectedLeft = 0
    private var selectedTop = 0
    private var selectedRight = 0
    private var selectedBottom = 0
    private var overlayColor = 0x30FF0000

    private val regionSelector = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let {
                selectedLeft = it.getIntExtra("left", 0)
                selectedTop = it.getIntExtra("top", 0)
                selectedRight = it.getIntExtra("right", 0)
                selectedBottom = it.getIntExtra("bottom", 0)
            }
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { checkPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        checkPermissions()
        setupColorPicker()

        binding.btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            }
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.btnRootGrant.setOnClickListener {
            grantPermissionsWithRoot()
        }

        binding.btnSelectRegion.setOnClickListener {
            regionSelector.launch(Intent(this, RegionSelectorActivity::class.java))
        }

        binding.btnFullScreen.setOnClickListener {
            val metrics = resources.displayMetrics
            selectedLeft = 0
            selectedTop = 0
            selectedRight = metrics.widthPixels
            selectedBottom = metrics.heightPixels
        }

        binding.btnToggle.setOnClickListener {
            if (isBlocking) {
                stopBlocking()
            } else {
                startBlocking()
            }
        }
    }

    private fun setupColorPicker() {
        binding.seekAlpha.max = 255
        binding.seekAlpha.progress = 48

        val updateColor = {
            val alpha = binding.seekAlpha.progress
            overlayColor = Color.argb(alpha, 255, 0, 0)
            binding.colorPreview.setBackgroundColor(overlayColor)
        }

        binding.seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateColor()
    }

    private fun grantPermissionsWithRoot() {
        Thread {
            try {
                val process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                
                os.writeBytes("pm grant $packageName android.permission.SYSTEM_ALERT_WINDOW\n")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    os.writeBytes("pm grant $packageName android.permission.POST_NOTIFICATIONS\n")
                }
                os.writeBytes("settings put secure enabled_accessibility_services ${packageName}/.BlockerAccessibilityService\n")
                os.writeBytes("settings put secure accessibility_enabled 1\n")
                os.writeBytes("exit\n")
                os.flush()
                process.waitFor()
                
                runOnUiThread { checkPermissions() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val accessibilityGranted = isAccessibilityServiceEnabled()
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        binding.btnOverlay.text = if (overlayGranted) getString(R.string.enabled) else getString(R.string.grant)
        binding.btnAccessibility.text = if (accessibilityGranted) getString(R.string.enabled) else getString(R.string.grant)
        binding.btnNotification.text = if (notificationGranted) getString(R.string.enabled) else getString(R.string.grant)

        binding.btnOverlay.isEnabled = !overlayGranted
        binding.btnAccessibility.isEnabled = !accessibilityGranted
        binding.btnNotification.isEnabled = !notificationGranted
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
                    it.resolveInfo.serviceInfo.name == BlockerAccessibilityService::class.java.name
        }
    }

    private fun startBlocking() {
        if (selectedRight == 0 || selectedBottom == 0) return

        val showFloatingButton = binding.switchFloatingButton.isChecked
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
            putExtra("left", selectedLeft)
            putExtra("top", selectedTop)
            putExtra("right", selectedRight)
            putExtra("bottom", selectedBottom)
            putExtra("color", overlayColor)
            putExtra("showFloatingButton", showFloatingButton)
        }
        startForegroundService(intent)

        isBlocking = true
        binding.btnToggle.text = getString(R.string.stop_blocking)
    }

    private fun stopBlocking() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        startService(intent)

        isBlocking = false
        binding.btnToggle.text = getString(R.string.start_blocking)
    }
}
