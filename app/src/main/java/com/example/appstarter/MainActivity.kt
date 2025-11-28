package com.example.appstarter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnSave: Button
    private lateinit var btnLaunch: Button
    private lateinit var btnSaveView: Button
    private lateinit var btnSettings: Button
    private lateinit var txtCountdown: TextView
    private lateinit var txtCurrentSettings: TextView
    private var appList: List<AppInfo> = emptyList()

    private var selectedPackages: MutableSet<String> = mutableSetOf()
    private var isDataLoaded = false

    private var countDownTimer: CountDownTimer? = null
    private var launchJob: Job? = null
    private var floatingTimer: FloatingTimerOverlay? = null

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun Int.dp(): Int =
        (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra và xin quyền

        if (!PermissionManager.hasAllRequiredPermissions(this)) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }

        initializeSelectedPackages()
        requestBatteryOptimizationExemption()
        setupUI()
        loadAppList()
        setupEventListeners()
        updateSettingsDisplay()

        autoStartIfFirstLaunchAfterBoot()
    }

    private fun initializeSelectedPackages() {
        try {
            selectedPackages = AppUtils.loadSelectedApps(this).toMutableSet()
            isDataLoaded = true
        } catch (e: Exception) {
            selectedPackages = mutableSetOf()
            isDataLoaded = false
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Vui lòng tắt tối ưu hóa pin cho ứng dụng này", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupUI() {
        // 📌 Root là ScrollView để có thể scroll toàn bộ nội dung
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Layout bên trong ScrollView
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48) // Padding lớn hơn cho TV
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT   // WRAP_CONTENT để ScrollView tính chiều cao
            )
            setBackgroundColor(0xFF0F1419.toInt()) // Very dark blue-black
        }

        scrollView.addView(mainLayout)

        // Title với font size lớn hơn cho TV
        val title = TextView(this).apply {
            text = "🚀 Auto App Launcher"
            textSize = 36f
            setTextColor(0xFF00D4FF.toInt())
            setPadding(0, 0, 0, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Subtitle
        val subtitle = TextView(this).apply {
            text = "Tự động khởi chạy ứng dụng khi khởi động"
            textSize = 16f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, 0, 0, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Current settings display
        txtCurrentSettings = TextView(this).apply {
            textSize = 18f
            setPadding(24, 16, 24, 16)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1A2332.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        }

        // Hàng nút 1
        val buttonLayout1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        btnSettings = createTVButton("⚙️ Cài đặt")
        btnSaveView = createTVButton("📦 Xem danh sách")

        buttonLayout1.addView(
            btnSettings,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, 8, 0) }
        )
        buttonLayout1.addView(
            btnSaveView,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(8, 0, 0, 0) }
        )

        // Hàng nút 2
        val buttonLayout2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        }

        btnSave = createTVButton("💾 Lưu")
        btnLaunch = createTVButton("🚀 Chạy ngay")

        buttonLayout2.addView(
            btnSave,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, 8, 0) }
        )
        buttonLayout2.addView(
            btnLaunch,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT , 1f).apply { setMargins(8, 0, 0, 0) }
        )

        txtCountdown = TextView(this).apply {
            textSize = 22f
            setTextColor(0xFF00FF88.toInt())
            visibility = View.GONE
            setPadding(0, 16, 0, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // List header
        val listHeader = TextView(this).apply {
            text = "📱 Chọn ứng dụng:"
            textSize = 20f
            setTextColor(0xFF00D4FF.toInt())
            setPadding(0, 0, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // ✅ ListView có chiều cao cố định (vd: 300dp) và tự scroll riêng
        listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                this@MainActivity.dp(300)   // cao ~300dp, đủ để nhìn thoáng, và vẫn scroll được
            )
            dividerHeight = 1
            divider = null
            setPadding(0, 16, 0, 16)
            setBackgroundColor(0xFF1A1A1A.toInt())
            isFocusable = true
            isFocusableInTouchMode = true
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
        }

        val statusInfo = TextView(this).apply {
            text = "💡 Ứng dụng sẽ tự động chạy khi khởi động TV"
            textSize = 14f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 24, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Thêm views vào mainLayout
        mainLayout.addView(title)
        mainLayout.addView(subtitle)
        mainLayout.addView(txtCurrentSettings)
        mainLayout.addView(buttonLayout1)
        mainLayout.addView(buttonLayout2)
        mainLayout.addView(txtCountdown)
        mainLayout.addView(listHeader)
        mainLayout.addView(listView)
        mainLayout.addView(statusInfo)

        // Cuối cùng setContentView là scrollView
        setContentView(scrollView)
    }


    private fun createTVButton(text: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1E90FF.toInt()) // Dodger blue
            // Tối ưu cho TV remote
            isFocusable = true
            isFocusableInTouchMode = true
            setPadding(16, 16, 16, 16)

            // Hiệu ứng focus cho TV
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    setBackgroundColor(0xFF00D4FF.toInt()) // Bright cyan
                    scaleX = 1.08f
                    scaleY = 1.08f
                } else {
                    setBackgroundColor(0xFF1E90FF.toInt())
                    scaleX = 1.0f
                    scaleY = 1.0f
                }
            }
        }
    }

    // Override key events để hỗ trợ TV remote
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                // Xử lý nút OK/Enter trên remote
                val focusedView = currentFocus
                if (focusedView is Button) {
                    focusedView.performClick()
                    return true
                } else if (focusedView is ListView) {
                    val position = listView.selectedItemPosition
                    if (position >= 0) {
                        listView.performItemClick(listView, position, listView.getItemIdAtPosition(position))
                        return true
                    }
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                // Xử lý nút Back
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateSettingsDisplay() {
        val initialDelay = AppUtils.getInitialDelay(this)
        val betweenDelay = AppUtils.getBetweenAppsDelay(this)
        txtCurrentSettings.text = "⏱️ Delay ban đầu: ${initialDelay}s | Giữa các app: ${betweenDelay}s"
    }

    private fun autoStartIfFirstLaunchAfterBoot() {
        // Không làm gì nếu chưa chọn app nào
        if (selectedPackages.isEmpty()) return

        val prefs = getSharedPreferences("appstarter_prefs", Context.MODE_PRIVATE)

        val currentBootCount = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0)
            } else {
                0
            }
        } catch (e: Exception) {
            // Nếu đọc BOOT_COUNT bị lỗi thì bỏ qua, không auto start
            0
        }

        val lastHandledBoot = prefs.getInt("last_handled_boot", -1)

        val isFirstLaunchAfterThisBoot =
            currentBootCount != 0 && currentBootCount != lastHandledBoot

        if (isFirstLaunchAfterThisBoot) {
            // Ghi nhớ đã xử lý boot này
            prefs.edit().putInt("last_handled_boot", currentBootCount).apply()

            // Gọi đếm ngược & chạy app
            startCountdownAndLaunch()
        }
    }


    private fun loadAppList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appList = AppUtils.getLaunchableApps(this@MainActivity)

                withContext(Dispatchers.Main) {
                    setupListView()
                    isDataLoaded = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ Lỗi tải danh sách ứng dụng", Toast.LENGTH_LONG).show()
                    appList = emptyList()
                    setupListView()
                }
            }
        }
    }

    private fun setupListView() {
        try {
            // Custom adapter cho TV với text size lớn hơn
            val adapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_multiple_choice,
                appList.map { "${it.name}" }
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.textSize = 18f // Lớn hơn cho TV
                    textView.setTextColor(0xFFFFFFFF.toInt())
                    textView.setPadding(24, 16, 24, 16)

                    // Background cho items
                    view.setBackgroundColor(0xFF252A35.toInt()) // Dark blue-gray

                    // Hiệu ứng focus cho ListView items
                    view.setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            view.setBackgroundColor(0xFF1E90FF.toInt()) // Dodger blue khi focus
                        } else {
                            view.setBackgroundColor(0xFF252A35.toInt())
                        }
                    }

                    return view
                }
            }

            listView.adapter = adapter

            // Đánh dấu những app đã chọn trước
            appList.forEachIndexed { index, appInfo ->
                if (selectedPackages.contains(appInfo.packageName)) {
                    listView.setItemChecked(index, true)
                }
            }

            // Cập nhật danh sách khi người dùng chọn/bỏ chọn
            listView.setOnItemClickListener { _, _, position, _ ->
                if (position < appList.size) {
                    val pkg = appList[position].packageName
                    if (selectedPackages.contains(pkg)) {
                        selectedPackages.remove(pkg)
                        Toast.makeText(this, "❌ Bỏ chọn: ${appList[position].name}", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedPackages.add(pkg)
                        Toast.makeText(this, "✅ Đã chọn: ${appList[position].name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Hiển thị số lượng apps
            val totalApps = appList.size
            val selectedCount = selectedPackages.size
            if (totalApps > 0) {
                Toast.makeText(this, "📱 Tải được $totalApps ứng dụng, đã chọn $selectedCount", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Lỗi setup ListView", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEventListeners() {
        btnSettings.setOnClickListener {
            showTimeSettingsDialog()
        }

        btnSave.setOnClickListener {
            if (selectedPackages.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ứng dụng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                AppUtils.saveSelectedApps(this, selectedPackages.toList())
                Toast.makeText(this, "✅ Đã lưu ${selectedPackages.size} ứng dụng", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Lỗi lưu", Toast.LENGTH_SHORT).show()
            }
        }

        btnLaunch.setOnClickListener {
            if (selectedPackages.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ứng dụng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startCountdownAndLaunch()
        }

        btnSaveView.setOnClickListener {
            showSavedAppsDialog()
        }
    }

    private fun showTimeSettingsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48) // Padding lớn hơn cho TV
            setBackgroundColor(0xFF0F1419.toInt()) // Dark blue-black
        }

        val currentInitialDelay = AppUtils.getInitialDelay(this)
        val currentBetweenDelay = AppUtils.getBetweenAppsDelay(this)

        // Initial delay setting
        val initialDelayLabel = TextView(this).apply {
            text = "⏱️ Thời gian chờ ban đầu (hiện tại: ${currentInitialDelay}s)"
            textSize = 18f
            setTextColor(0xFF00D4FF.toInt()) // Cyan
            setPadding(0, 0, 0, 16)
        }

        val initialDelaySeekBar = SeekBar(this).apply {
            max = 120
            progress = currentInitialDelay
            // Tối ưu cho TV remote
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val initialDelayValue = TextView(this).apply {
            text = "${currentInitialDelay} giây"
            textSize = 16f
            setTextColor(0xFF00FF88.toInt()) // Green
            setPadding(0, 16, 0, 32)
        }

        initialDelaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                initialDelayValue.text = "$progress giây"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Between apps delay setting
        val betweenDelayLabel = TextView(this).apply {
            text = "⏱️ Thời gian chờ giữa các app (hiện tại: ${currentBetweenDelay}s)"
            textSize = 18f
            setTextColor(0xFF00D4FF.toInt()) // Cyan
            setPadding(0, 16, 0, 16)
        }

        val betweenDelaySeekBar = SeekBar(this).apply {
            max = 30
            progress = currentBetweenDelay
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val betweenDelayValue = TextView(this).apply {
            text = "${currentBetweenDelay} giây"
            textSize = 16f
            setTextColor(0xFF00FF88.toInt()) // Green
            setPadding(0, 16, 0, 0)
        }

        betweenDelaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                betweenDelayValue.text = "$progress giây"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        layout.addView(initialDelayLabel)
        layout.addView(initialDelaySeekBar)
        layout.addView(initialDelayValue)
        layout.addView(betweenDelayLabel)
        layout.addView(betweenDelaySeekBar)
        layout.addView(betweenDelayValue)

        AlertDialog.Builder(this)
            .setTitle("⚙️ Cài đặt thời gian khởi chạy")
            .setView(layout)
            .setPositiveButton("💾 Lưu") { _, _ ->
                val newInitialDelay = initialDelaySeekBar.progress
                val newBetweenDelay = betweenDelaySeekBar.progress

                AppUtils.saveInitialDelay(this, newInitialDelay)
                AppUtils.saveBetweenAppsDelay(this, newBetweenDelay)
                updateSettingsDisplay()

                Toast.makeText(this, "✅ Đã lưu: Delay ban đầu ${newInitialDelay}s, giữa apps ${newBetweenDelay}s", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("❌ Hủy", null)
            .setNeutralButton("🔄 Mặc định") { _, _ ->
                AppUtils.saveInitialDelay(this, 15)
                AppUtils.saveBetweenAppsDelay(this, 5)
                updateSettingsDisplay()
                Toast.makeText(this, "🔄 Đã khôi phục cài đặt mặc định!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun startCountdownAndLaunch() {
        countDownTimer?.cancel()
        launchJob?.cancel()

        val initialDelay = AppUtils.getInitialDelay(this)
        val initialDelayMs = initialDelay * 1000L

        Toast.makeText(this, "🕐 Sẽ khởi chạy sau ${initialDelay} giây", Toast.LENGTH_SHORT).show()

        txtCountdown.visibility = View.VISIBLE
        btnLaunch.isEnabled = false

        // Hiển thị overlay timer
        floatingTimer = FloatingTimerOverlay(this)
        floatingTimer?.show(initialDelay.toLong())

        countDownTimer = object : CountDownTimer(initialDelayMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                txtCountdown.text = "⏳ Khởi chạy sau: $secondsLeft giây"
            }

            override fun onFinish() {
                txtCountdown.text = "🚀 Đang khởi chạy ứng dụng..."
                launchAppsSequentially()
            }
        }.start()
    }

    private fun launchAppsSequentially() {
        launchJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val appsToLaunch = selectedPackages.toList()
                val betweenDelay = AppUtils.getBetweenAppsDelay(this@MainActivity) * 1000L

                for ((index, packageName) in appsToLaunch.withIndex()) {
                    try {
                        if (AppUtils.isAppInstalled(this@MainActivity, packageName)) {
                            launchApp(packageName)
                            txtCountdown.text = "🚀 Đã khởi chạy ${index + 1}/${appsToLaunch.size} ứng dụng"
                            floatingTimer?.updateProgress(index + 1, appsToLaunch.size)

                            if (index < appsToLaunch.size - 1) {
                                delay(betweenDelay)
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "❌ Không tìm thấy: $packageName", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "❌ Lỗi khởi chạy: $packageName", Toast.LENGTH_SHORT).show()
                    }
                }

                delay(2000)
                floatingTimer?.dismiss()
                txtCountdown.visibility = View.GONE
                btnLaunch.isEnabled = true
                Toast.makeText(this@MainActivity, "✅ Hoàn tất khởi chạy tất cả ứng dụng!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                floatingTimer?.dismiss()
                txtCountdown.visibility = View.GONE
                btnLaunch.isEnabled = true
                Toast.makeText(this@MainActivity, "❌ Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let { intent ->
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "❌ Không thể khởi chạy: $packageName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSavedAppsDialog() {
        try {
            val savedApps = AppUtils.getSavedApps(this).toMutableList()

            if (savedApps.isEmpty()) {
                Toast.makeText(this, "📦 Không có ứng dụng nào đã lưu!", Toast.LENGTH_SHORT).show()
                return
            }

            val appNames = savedApps.map { packageName ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    "$appName"
                } catch (e: Exception) {
                    packageName
                }
            }.toTypedArray()

            val checkedItems = BooleanArray(savedApps.size) { true }

            AlertDialog.Builder(this)
                .setTitle("📦 Ứng dụng sẽ khởi chạy khi boot (${savedApps.size})")
                .setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
                    checkedItems[which] = isChecked
                }
                .setPositiveButton("💾 Lưu thay đổi") { _, _ ->
                    val updatedList = savedApps.filterIndexed { index, _ -> checkedItems[index] }
                    AppUtils.saveSelectedApps(this, updatedList)
                    Toast.makeText(this, "✅ Đã cập nhật danh sách (${updatedList.size} ứng dụng)!", Toast.LENGTH_SHORT).show()

                    selectedPackages.clear()
                    selectedPackages.addAll(updatedList)
                    setupListView()
                }
                .setNegativeButton("❌ Hủy", null)
                .setNeutralButton("🗑️ Xóa tất cả") { _, _ ->
                    AppUtils.saveSelectedApps(this, emptyList())
                    selectedPackages.clear()
                    setupListView()
                    Toast.makeText(this, "🗑️ Đã xóa tất cả ứng dụng đã lưu!", Toast.LENGTH_SHORT).show()
                }
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Lỗi hiển thị dialog", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        launchJob?.cancel()
        floatingTimer?.dismiss()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (isDataLoaded && selectedPackages.isNotEmpty()) {
                AppUtils.saveSelectedApps(this, selectedPackages.toList())
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }
}