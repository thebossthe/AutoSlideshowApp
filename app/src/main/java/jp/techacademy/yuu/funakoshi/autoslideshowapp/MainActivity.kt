package jp.techacademy.yuu.funakoshi.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import jp.techacademy.yuu.funakoshi.autoslideshowapp.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager
    private val images = mutableListOf<String>() // 画像のURIを格納するリスト
    private var currentPage = 0
    private var timer: Timer? = null
    private var handler = Handler(Looper.getMainLooper())
    private var isAutoRunning = false

    private val PERMISSIONS_REQUEST_CODE = 100
    private val readImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewPager = binding.viewPager

        // パーミッションの許可状態を確認する
        if (ContextCompat.checkSelfPermission(this, readImagesPermission) == PackageManager.PERMISSION_GRANTED) {
            // 許可されている場合は画像情報を取得
            getContentsInfo()
        } else {
            // 許可されていない場合はパーミッションをリクエスト
            ActivityCompat.requestPermissions(
                this,
                arrayOf(readImagesPermission),
                PERMISSIONS_REQUEST_CODE
            )
        }

        // Prevボタン
        binding.PrevButton.setOnClickListener {
            if (isAutoRunning == false){
                if (currentPage > 0) {
                    currentPage--
                    viewPager.currentItem = currentPage
                } else {
                    currentPage = images.size
                    viewPager.currentItem = currentPage
                }
            }
        }

        // Nextボタン
        binding.NextButton.setOnClickListener {
            if (isAutoRunning == false) {
                if (currentPage < images.size - 1) {
                    currentPage++
                    viewPager.currentItem = currentPage
                } else {
                    currentPage = 0
                    viewPager.currentItem = currentPage
                }
            }
        }

        // Autボタン
        binding.AutButton.setOnClickListener {
            if (isAutoRunning) {
                stopAut()
            } else {
                startAut()
            }
        }
    }

    // パーミッションリクエストの結果を受け取るメソッド
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getContentsInfo()
            }
        }
    }

    // 画像の情報を取得するメソッド
    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目（null = 全項目）
            null, // フィルタ条件（null = フィルタなし）
            null, // フィルタ用パラメータ
            null // ソート (nullソートなし）
        )

        cursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                images.add(imageUri)
            }
        }

        cursor?.close()

        // ViewPager を操作して画像を表示する
        viewPager.adapter = object : androidx.viewpager.widget.PagerAdapter() {
            override fun getCount(): Int {
                return images.size
            }

            override fun isViewFromObject(view: android.view.View, `object`: Any): Boolean {
                return view == `object`
            }

            override fun instantiateItem(container: android.view.ViewGroup, position: Int): Any {
                val imageView = android.widget.ImageView(applicationContext)
                imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                container.addView(imageView)

                // 画像を表示する方法に応じて、Glide などのライブラリを使用する例
                com.bumptech.glide.Glide.with(applicationContext)
                    .load(images[position])
                    .into(imageView)

                return imageView
            }

            override fun destroyItem(container: android.view.ViewGroup, position: Int, `object`: Any) {
                container.removeView(`object` as android.view.View)
            }
        }
    }

    // 再生
    private fun startAut() {
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    if (currentPage < images.size - 1) {
                        currentPage++
                        viewPager.currentItem = currentPage
                    } else {
                        currentPage = 0
                        viewPager.currentItem = currentPage
                    }
                }
            }
        }, 2000, 2000) // 最初に始動させるまで2000ミリ秒、ループの間隔を2000ミリ秒に設定

        isAutoRunning = true
        binding.AutButton.text = "停止"
    }

    // 停止
    private fun stopAut() {
        timer?.cancel()
        timer = null

        isAutoRunning = false
        binding.AutButton.text = "再生"
    }
}