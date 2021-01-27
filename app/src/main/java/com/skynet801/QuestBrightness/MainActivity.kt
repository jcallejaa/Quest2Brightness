package com.skynet801.QuestBrightness

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = javaClass.canonicalName
        val CODE_WRITE_SETTINGS_PERMISSION = 109

        val STATE_STARTUP = 0
        val STATE_REQUESTING_PERMISSION = 1
        val STATE_RUNNING = 2
    }

    val Context.canWriteSettings: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(
        this
    )
    var changingProgressOnlyVisually = false
    var brightnessLevel: Int = 150
    var context: Context = this
    lateinit var brightnessBar: SeekBar
    lateinit var permissionRequestLayout: RelativeLayout
    lateinit var bottomBarLayout: LinearLayout
    lateinit var versionTV: TextView
    var state = STATE_STARTUP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionRequestLayout = findViewById<RelativeLayout>(R.id.permissionRequestLayout)
        bottomBarLayout = findViewById<LinearLayout>(R.id.bottomBar)
        versionTV = findViewById<TextView>(R.id.version)

        brightnessBar = findViewById<SeekBar>(R.id.brightnessBar)
        brightnessBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!changingProgressOnlyVisually) {
                    brightnessLevel = seekBar!!.progress
                    applyBrightnessRequestingPermissions()
                }
                changingProgressOnlyVisually = false
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
    }

    override fun onStart() {
        super.onStart()
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = pInfo.versionName
            versionTV.text = version
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        changeState(STATE_STARTUP)
    }

    fun lowPressed(v: View) {
        brightnessLevel = 0
        applyBrightnessRequestingPermissions()
    }

    fun defaultPressed(v: View) {
        brightnessLevel = 127
        applyBrightnessRequestingPermissions()
    }

    fun highPressed(v: View) {
        brightnessLevel = 255
        applyBrightnessRequestingPermissions()
    }

    fun closePressed(v: View){
        finish()
    }

    fun openPermissionWindowClicked(v: View){
        startManageWriteSettingsPermission()
    }

    fun openSettingsClicked(v: View){
        startActivityForResult(Intent(android.provider.Settings.ACTION_SETTINGS), 0);
    }

    fun applyBrightnessRequestingPermissions() {
        if (context.canWriteSettings) {
            applyBrightnessWithPermissionGranted()
        } else {
            startManageWriteSettingsPermission()
        }
    }

    fun applyBrightnessWithPermissionGranted() {
        Log.d(TAG, "Setting brightness to: $brightnessLevel")
        try{
            val cResolver = this.applicationContext.contentResolver
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessLevel)
            setProgressBarWithoutProcessing(brightnessLevel)
        }catch (e: Exception){
            Log.e(TAG, "Error changning brightness")
        }
    }

    fun setProgressBarWithoutProcessing(progress: Int){
        changingProgressOnlyVisually = true
        brightnessBar.progress = brightnessLevel
    }


    fun changeState(newState: Int){
        when (newState){
            STATE_STARTUP -> {
                permissionRequestLayout.visibility = View.VISIBLE
                bottomBarLayout.visibility = View.GONE
                if (checkPermission()) {
                    changeState(STATE_RUNNING)
                } else {
                    changeState(STATE_REQUESTING_PERMISSION)
                }
            }
            STATE_REQUESTING_PERMISSION -> {
                permissionRequestLayout.visibility = View.VISIBLE
                bottomBarLayout.visibility = View.GONE
            }
            STATE_RUNNING -> {
                permissionRequestLayout.visibility = View.GONE
                bottomBarLayout.visibility = View.VISIBLE
                brightnessLevel = getBrightness()
                applyBrightnessRequestingPermissions()
            }
        }
        state = newState
    }




    // PERMISSIONS

    private fun startManageWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).let {
                startActivityForResult(it, CODE_WRITE_SETTINGS_PERMISSION)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CODE_WRITE_SETTINGS_PERMISSION -> {
                if (context.canWriteSettings) {
                    changeState(STATE_RUNNING)
                } else {
                    changeState(STATE_REQUESTING_PERMISSION)
                    Toast.makeText(
                        context,
                        "Write settings permission is not granted!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun checkPermission(): Boolean{
        return context.canWriteSettings
    }

    fun getBrightness(): Int {
        var brightness = 0
        if (!Settings.System.canWrite(context)) {
            // Enable write permission
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            context.startActivity(intent)
        } else {
            // Get system brightness
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            ) // enable auto brightness
            brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                -1
            ) // in the range [0, 255]
        }
        return brightness
    }





}