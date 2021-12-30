package de.felser_net.connecttc66c

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.PermissionChecker
import de.felser_net.connecttc66c.databinding.ActivityMainBinding
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
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
    }

    // - let's get BLE permission
    override fun onStart() {
        super.onStart()

        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==  PermissionChecker.PERMISSION_GRANTED ) {
            Log.d("MainActivity", "BLE permission already granted")
            blePermissionGranted = true
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.ble_ask_permission_title)
            builder.setMessage(R.string.ble_ask_permission_message)
            builder.setPositiveButton(android.R.string.ok) { _,_ ->
                this.requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), BLE_PERMISSION_REQUEST_ID)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            BLE_PERMISSION_REQUEST_ID -> {
                if (grantResults == intArrayOf(PackageManager.PERMISSION_GRANTED)) {
                    Log.d("MainActivity", "BLE permission granted")
                    blePermissionGranted = true
                } else {
                    Log.w("MainActivity", "BLE permission NOT granted)")
                    blePermissionGranted = false
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.ble_no_permission_title)
                    builder.setMessage(R.string.ble_no_permission_message)
                    builder.setPositiveButton(android.R.string.ok) { _,_ -> this.finish() }
                    builder.show()
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}