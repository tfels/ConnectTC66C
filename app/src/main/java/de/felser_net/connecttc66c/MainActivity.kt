package de.felser_net.connecttc66c

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.PermissionChecker
import de.felser_net.connecttc66c.databinding.ActivityMainBinding
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val REQUEST_ENABLE_BLUETOOTH = 101
        const val BLE_PERMISSION_REQUEST_ID = 1001
        var blePermissionGranted: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        // check if bluetooth BLE is available
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if(bluetoothAdapter==null || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_bluetooth_support, Toast.LENGTH_LONG).show()
            finish()
        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }
    }

    // - let's get BLE permission
    override fun onStart() {
        super.onStart()

        val permissionName = when {
            // Android 12
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> Manifest.permission.BLUETOOTH_SCAN
            // Android 10
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Manifest.permission.ACCESS_FINE_LOCATION
            // Android 9 or lower
            else -> Manifest.permission.ACCESS_COARSE_LOCATION
        }

        if (PermissionChecker.checkSelfPermission(this, permissionName) ==  PermissionChecker.PERMISSION_GRANTED ) {
            Log.d("MainActivity", "BLE permission already granted")
            blePermissionGranted = true
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.ble_ask_permission_title)
            builder.setMessage(R.string.ble_ask_permission_message)
            builder.setCancelable(false)
            builder.setPositiveButton(android.R.string.ok) { _,_ ->
                this.requestPermissions(arrayOf(permissionName), BLE_PERMISSION_REQUEST_ID)
            }
            builder.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BLUETOOTH -> {
                if(resultCode == RESULT_OK)
                    Log.d("MainActivity", "bluetooth enabled")
                else {
                    Log.d("MainActivity", "bluetooth NOT enabled")
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.bt_disabled_title)
                    builder.setMessage(R.string.bt_disabled_title_message)
                    builder.setCancelable(false)
                    builder.setPositiveButton(android.R.string.ok) { _, _ -> this.finish() }
                    builder.show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BLE_PERMISSION_REQUEST_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "BLE permission granted")
                    blePermissionGranted = true
                } else {
                    Log.w("MainActivity", "BLE permission NOT granted)")
                    blePermissionGranted = false
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.ble_no_permission_title)
                    builder.setMessage(R.string.ble_no_permission_message)
                    builder.setCancelable(false)
                    builder.setPositiveButton(android.R.string.ok) { _,_ -> this.finish() }
                    builder.show()
                }
            }
            else -> Toast.makeText(
                this,
                getString(R.string.unexpected_request_permission_result),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}