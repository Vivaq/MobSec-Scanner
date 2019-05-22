package com.wpam.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.arch.persistence.room.Room
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import android.widget.*
import com.wpam.scanner.attack.BluetoothAttack
import com.wpam.scanner.attack.WifiAttack
import com.wpam.scanner.db.*
import com.wpam.scanner.scan.*
import com.wpam.scanner.utils.DisplayUtils
import com.wpam.scanner.utils.NetUtils
import kotlinx.android.synthetic.main.content_show_db.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.uiThread
import com.wpam.scanner.tools.WifiTunnel
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.ArrayAdapter


@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var threads: Int? = null
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle:ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ){ }
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 86)

        nav_view.setNavigationItemSelectedListener(this)
        val frameParent = frame.parent as ViewGroup

        val view = layoutInflater.inflate(R.layout.content_show_db, frameParent, false)
        frameParent.addView(view, frameParent.indexOfChild(frame))

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "scan_results").build()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> let{

                val builder = AlertDialog.Builder(it)

                val dialogView = View.inflate(it, R.layout.settings, null)
                builder.setView(dialogView)
                    .setPositiveButton("OK"
                    ) { _, _ ->
                        var editTxt: String

                        val threadsEditText = dialogView.findViewById<EditText> (R.id.threadsNum)
                        if (!threadsEditText.text.isEmpty()) {
                            editTxt = threadsEditText.text.toString()
                            val editNum = Integer.parseInt(editTxt)
                            if(editNum in 1..100) {
                                threads = editNum
                            }
                            else {
                                DisplayUtils.toast(this, "Number must be greater than 1 and smaller than 100")
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .setTitle("Settings")
                builder.create()
                builder.show()
                true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        db_table.removeAllViews()

        when (item.itemId) {
            R.id.host_scan -> {
                hostScan()
            }
            R.id.port_scan -> {
                portScan()
            }
            R.id.bt_scan -> {
                btScan()
            }
            R.id.wifi_scan -> {
                wifiScan()
            }
            R.id.wifi_attack ->  {
                wifiAttack()
            }
            R.id.bt_attack -> {
                btAttack()
            }
            R.id.wifi_tunnel -> {
                wifiTunnel()
            }
            R.id.get_scan_results -> {
                showScanResults()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun hostScan() {
        val builder = AlertDialog.Builder(this)

        val networksAddresses = NetUtils.getNetworksAddresses()

        val adapter = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item, networksAddresses
        )

        val spinnerView = Spinner(this)
        spinnerView.adapter = adapter

        builder
            .setTitle("Select interface to scan")
            .setView(spinnerView)
            .setPositiveButton("OK") { _, _ ->
                doAsyncResult {
                    db.hostDao().deleteByNet(spinnerView.selectedItem.toString())
                }.get()
                val title = "Active hosts"
                val scanner = HostsScanner(this, spinnerView.selectedItem.toString(), db, threads)

                handleScan(scanner, title)
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .setCancelable(false)
        builder.create()
        builder.show()
    }

    private fun portScan() {
        val builder = AlertDialog.Builder(this)

        val asyncDbTask = doAsyncResult {
            db.hostDao().getAllIps()
        }
        val ipAddresses = asyncDbTask.get()

        val adapter = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item, ipAddresses
        )

        val spinnerView = Spinner(this)
        spinnerView.adapter = adapter

        val editPortRange = EditText(this)
        editPortRange.hint = "Ports range, eg. 1-1023"

        val viewContainer = LinearLayout(this)
        viewContainer.orientation = LinearLayout.VERTICAL

        viewContainer.addView(spinnerView)
        viewContainer.addView(editPortRange)

        builder
            .setTitle("Select IP address and ports range")
            .setView(viewContainer)
            .setPositiveButton("OK") { dialog, _ ->
                val ipAddress = spinnerView.selectedItem.toString()
                if (
                        editPortRange.text.isEmpty() or
                        editPortRange.text.split("-").any { n -> n.toIntOrNull() == null } or
                        (editPortRange.text.split("-").size != 2)
                ) {
                    DisplayUtils.toast(this, "Incorrect port range format")
                    return@setPositiveButton
                }

                val portRange = editPortRange.text.toString()
                dialog.dismiss()

                val title = "Opened ports on host $ipAddress"

                doAsyncResult {
                    db.portDao().deleteByIp(ipAddress)
                }.get()
                val scanner = PortScanner(this, ipAddress, portRange, db)
                handleScan(scanner, title)
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .setCancelable(false)
        builder.create()
        builder.show()
    }

    private fun btScan() {
        val title = "Active Bluetooth devices"

        doAsyncResult {
            db.bluetoothDao().clear()
        }.get()
        val scanner = BluetoothScanner(this, db)
        handleScan(scanner, title)
    }

    private fun wifiScan() {
        val title = "Active Wifi"
        doAsyncResult {
            db.wifiDao().clear()
        }.get()
        val scanner = WifiScanner(this, db)
        handleScan(scanner, title)
    }

    private fun wifiAttack() {
        val builder = AlertDialog.Builder(this)

        val asyncDb1 = doAsyncResult {
            db.wifiDao().getAllSSID()
        }
        val ssids = asyncDb1.get()
        val adapterSSIDS = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item, ssids
        )

        val spinnerViewSSIDS = Spinner(this)
        spinnerViewSSIDS.adapter = adapterSSIDS
        spinnerViewSSIDS.prompt = "abc"

        val adapterDicts = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item, arrayOf("custom_dict", "nmap_dict")
        )

        val spinnerViewDicts = Spinner(this)
        spinnerViewDicts.adapter = adapterDicts

        val spinnerContainer = LinearLayout(this)
        spinnerContainer.orientation = LinearLayout.VERTICAL

        spinnerContainer.addView(spinnerViewSSIDS)
        spinnerContainer.addView(spinnerViewDicts)

        builder
            .setTitle("Select SSID and dictionary")
            .setView(spinnerContainer)
            .setPositiveButton("OK") { _, _ ->
                val wifiAttack = WifiAttack(this)
                val asyncDb2 = doAsyncResult {
                    db.wifiDao().getWifi(spinnerViewSSIDS.selectedItem.toString())
                }
                val wifi = asyncDb2.get()

                var password = ""
                val thread = Thread {
                    password = wifiAttack.bruteForce(wifi, spinnerViewDicts.selectedItem.toString())
                }

                thread.start()

                val context = this
                doAsync {
                    while (thread.isAlive) {
                        uiThread {
                            Toast.makeText(context, "Please wait. Scan in progress...", Toast.LENGTH_LONG).show()
                        }
                        Thread.sleep(4000)
                    }
                    if (password != "") {
                        uiThread {
                            DisplayUtils.prettyInformation(context, "Password found: $password")
                        }
                    }
                    else {
                        uiThread {
                            DisplayUtils.prettyInformation(context, "No password found.")
                        }
                    }
                }
            }
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> }
        builder.create()
        builder.show()
    }

    private fun btAttack() {
        val btAttack = BluetoothAttack(this)
        btAttack.sendMaliciousFile()
    }

    private fun wifiTunnel() {
        val tunnel = WifiTunnel()

        val builder = AlertDialog.Builder(this)

        val editReverseSshConnectionString = EditText(this)
        editReverseSshConnectionString.hint = "Username@host"

        val editReverseSshPassword = EditText(this)
        editReverseSshPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        editReverseSshPassword.hint = "Password"

        val editReverseSshPort = EditText(this)
        editReverseSshPort.inputType = InputType.TYPE_CLASS_NUMBER
        editReverseSshPort.hint = "Port"
        editReverseSshPort.setText("22")

        val editTextContainer = LinearLayout(this)
        editTextContainer.orientation = LinearLayout.VERTICAL

        editTextContainer.addView(editReverseSshConnectionString)
        editTextContainer.addView(editReverseSshPassword)
        editTextContainer.addView(editReverseSshPort)

        builder
            .setTitle("Authenticate to remote server")
            .setView(editTextContainer)
            .setPositiveButton("OK"
            ) { _, _ ->
                doAsync {
                    val connectionString = editReverseSshConnectionString.text.toString()
                    val rport = 2220

                    var success: Boolean
                    try {
                        success = tunnel.setupSShTunnel(
                            connectionString,
                            editReverseSshPassword.text.toString(),
                            Integer.parseInt(editReverseSshPort.text.toString()),
                            rport
                        )
                    } catch (e: Exception) {
                        uiThread {
                            DisplayUtils.toast(it, "Connection failed")
                        }
                        success = false
                        tunnel.stopTunnelling()
                    }

                    if (!success) return@doAsync

                    val username = "user"
                    val password = tunnel.startSshServer(username)

                    uiThread {

                        val msgText = "Run 'ssh -ND 127.0.0.1:8080 $username@127.0.0.1 -p $rport' on server.\n" +
                                "Password: $password"
                        AlertDialog.Builder(it)
                            .setCancelable(false)
                            .setMessage(msgText)
                            .setPositiveButton("Stop tunnel") { _, _ ->  tunnel.stopTunnelling()}
                            .create()
                            .show()
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ ->  }
            .setCancelable(false)
        builder.create()

        builder.show()
    }

    private fun showScanResults() {
        val builder = AlertDialog.Builder(this)

        val dbTypes = arrayOf("Host", "Port", "Bluetooth", "Wifi")
        val scanTypes = arrayOf(ScanType.HOSTS, ScanType.PORTS, ScanType.BLUETOOTH, ScanType.WIFI)

        builder.setItems(dbTypes) { dialog, which ->
            dialog.dismiss()
            Thread {
                handleDbShow(scanTypes[which])
            }.start()
        }.setTitle("Select Database")
        builder.create()
        builder.show()
    }

    private fun handleDbShow(type: ScanType) {
        when (type) {
            ScanType.HOSTS -> {
                fillTable(Host.columnsNames, db.hostDao().getAll())
            }
            ScanType.PORTS -> {
                fillTable(Port.columnsNames, db.portDao().getAll())
            }
            ScanType.BLUETOOTH -> {
                fillTable(Bluetooth.columnsNames, db.bluetoothDao().getAll())
            }
            ScanType.WIFI -> {
                fillTable(Wifi.columnsNames, db.wifiDao().getAll())
            }
        }
    }

    private fun fillTable(columnsNames: Array<String>, dbObjects: List<Any>) {
        val tableRowColumnsNames = TableRow(this)

        for (columnName in columnsNames){
            val tv = TextView(this)
            tv.text = columnName
            tv.setPadding(20, 20, 20, 20)
            tv.setTypeface(null, Typeface.BOLD)
            tableRowColumnsNames.addView(tv)
            tableRowColumnsNames.background = getDrawable(R.drawable.cell_shape)
        }
        runOnUiThread {
            db_table.addView(tableRowColumnsNames)
        }

        for (dbObj in dbObjects) {
            val tableRowColumnsValues = TableRow(this)

            for (columnName in columnsNames) {
                val columnValue = dbObj.javaClass
                    .getMethod("get${columnName.capitalize()}")
                    .invoke(dbObj)
                    .toString()

                val tv = TextView(this)
                tv.text = columnValue
                tv.setPadding(20, 20, 20, 20)

                tableRowColumnsValues.addView(tv)
            }

            runOnUiThread {
                db_table.addView(tableRowColumnsValues)
            }
        }
    }

    private fun handleScan(scanner: Scanner, titleText: String) {

        var success = false
        val thread = Thread {
            success = scanner.doScan()
        }

        thread.start()
        drawer_layout.isEnabled = false
        val context = this

        doAsync {
            while (thread.isAlive) {
                uiThread {
                    Toast.makeText(context, "Please wait. Scan in progress...", Toast.LENGTH_LONG).show()
                }
                Thread.sleep(4000)
            }
            if (success) {
                uiThread {
                    drawer_layout.isEnabled = true
                    DisplayUtils.prettyInformation(context, "Scan completed successfully.")
                    Thread {
                        handleDbShow(scanner.type)
                    }.start()
                }
            }
            else {
                uiThread {
                    DisplayUtils.prettyInformation(context, "Scan failed.")
                }
            }
        }
    }
}
