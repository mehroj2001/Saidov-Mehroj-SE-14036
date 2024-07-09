package com.example.shop.Activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.http.HttpException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.codebyashish.googledirectionapi.AbstractRouting
import com.codebyashish.googledirectionapi.AbstractRouting.TravelMode
import com.codebyashish.googledirectionapi.ErrorHandling
import com.codebyashish.googledirectionapi.RouteDrawing
import com.codebyashish.googledirectionapi.RouteInfoModel
import com.codebyashish.googledirectionapi.RouteListener
import com.example.shop.R
import com.example.shop.RetrofitInstance
import com.example.shop.Shop
import com.example.shop.ShopAdapter
import com.example.shop.databinding.ActivityYandexBinding
import com.example.shop.databinding.DialogRadioViewBinding
import com.example.shop.databinding.DialogViewBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.TextStyle
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.ArrayList


class YandexActivity : AppCompatActivity(), RouteListener {


    private var currentLocation: Point? = null
    private val TAG: String? = "SSSS"
    private lateinit var binding: ActivityYandexBinding

    private lateinit var mapKit: MapKit

    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }

    private lateinit var shopAdapter: ShopAdapter

    private var enabledShops: MutableList<Shop> = mutableListOf()
    private var allShops: MutableList<Shop> = mutableListOf()

    private var markerLocation: Point = Point(0.0, 0.0)
    private var travelMode: TravelMode = TravelMode.DRIVING

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("0d000129-e96e-45d1-b5ee-0dae0ce77457")
        MapKitFactory.initialize(this)

        binding = ActivityYandexBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapKit = MapKitFactory.getInstance()

        setupMap()
        setupRecyclerView()
        loadData()

        binding.closeButton.setOnClickListener {
            binding.listlayout.isVisible = false
        }

        binding.btnRoute.setOnClickListener {
            binding.listlayout.isVisible = true
        }

        binding.btnAdd.setOnClickListener {
            if (binding.markerImage.isVisible) {
                markerLocation = binding.mapview.mapWindow.map.cameraPosition.target
                showAddDialog()
            } else {
                binding.btnCancel.isVisible = true
                binding.markerImage.isVisible = true
            }
        }

        binding.btnRoute.setOnClickListener {
            if (!binding.listlayout.isVisible) {
                binding.listlayout.isVisible = true
            }
        }

        binding.drawButton.setOnClickListener {
            drawRoute()
        }

        binding.btnCancel.setOnClickListener {
            binding.markerImage.isVisible = false
            binding.btnCancel.isVisible = false
        }
    }

    fun clearRoutes() {
        if (currentLocation != null) {
            binding.mapview.map.mapObjects.clear()

            binding.mapview.map.move(
                CameraPosition(currentLocation!!, 14.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )

            val placemark = binding.mapview.map.mapObjects.addPlacemark().apply {
                geometry = currentLocation!!
                setIcon(ImageProvider.fromResource(this@YandexActivity, R.drawable.person))
            }
            placemark.addTapListener(placemarkTapListener)
            drawMarkers()
        }

    }

    fun drawRoute() {
        clearRoutes()
        if (enabledShops.isEmpty()) {
            Toast.makeText(this, "Select at least 1 shop", Toast.LENGTH_SHORT).show()
            return
        }

        val waypoints: MutableList<LatLng> = mutableListOf()

        enabledShops.forEach {
            waypoints.add(LatLng(it.latitude, it.longitude))
        }
        if (currentLocation != null) {
            waypoints.add(LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
        } else {
            setupMap()
            return
        }
        getTravelMode(waypoints)
    }

    fun getTravelMode(waypoints: MutableList<LatLng>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add point")
        val dialogRadioViewBinding = DialogRadioViewBinding.inflate(layoutInflater)
        builder.setView(dialogRadioViewBinding.root)

        builder.setPositiveButton("Show Shops") { dialog, which ->
            var selectedID = dialogRadioViewBinding.rgMethod.checkedRadioButtonId
            if (selectedID == dialogRadioViewBinding.rbCar.id) {
                travelMode = TravelMode.DRIVING
            } else {
                travelMode = TravelMode.WALKING
            }
            val routeDrawing = RouteDrawing.Builder()
                .context(this)
                .travelMode(travelMode)
                .withListener(this).alternativeRoutes(false)
                .waypoints(
                    waypoints
                )
                .build()
            routeDrawing.execute()
        }

        builder.setNegativeButton(android.R.string.no) { dialog, which -> }
        builder.show()
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    fun showAddDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add point")
        val dialogViewBinding = DialogViewBinding.inflate(layoutInflater)
        dialogViewBinding.tvLocation.text =
            "Location: " + markerLocation.latitude.toString() + ", " + markerLocation.longitude.toString()
        builder.setView(dialogViewBinding.root)

        builder.setPositiveButton("Add") { dialog, which ->
            if (dialogViewBinding.edName.text.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please, enter name", Toast.LENGTH_SHORT
                ).show()
            }
            if (dialogViewBinding.edContact.text.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please, enter contact person", Toast.LENGTH_SHORT
                ).show()
            }

            if (dialogViewBinding.edNumber.text.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please, enter phone number", Toast.LENGTH_SHORT
                ).show()
            }

            val newShop = Shop(
                contact_person = dialogViewBinding.edContact.text.toString(),
                id = 0,
                isEnabled = true,
                latitude = markerLocation.latitude,
                longitude = markerLocation.longitude,
                name = dialogViewBinding.edName.text.toString(),
                phone = dialogViewBinding.edNumber.text.toString()
            )

            if (dialogViewBinding.edName.text.isNotEmpty() && dialogViewBinding.edNumber.text.isNotEmpty() && dialogViewBinding.edContact.text.isNotEmpty())
                lifecycleScope.launch {
                    val response = try {
                        RetrofitInstance.api.addShop(newShop)
                    } catch (e: IOException) {
                        return@launch
                    } catch (e: HttpException) {
                        Log.e("", "HTTPException")
                        return@launch
                    }

                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(
                            this@YandexActivity,
                            "Shop has been added!", Toast.LENGTH_SHORT
                        ).show()
                        allShops.add(newShop)
                        binding.markerImage.isVisible = false
                        binding.btnCancel.isVisible = false


                        val placemark = binding.mapview.map.mapObjects.addPlacemark().apply {
                            geometry = Point(newShop.latitude, newShop.longitude)
                            setIcon(ImageProvider.fromResource(this@YandexActivity, R.drawable.marker30))
                        }
                        placemark.setText(
                            newShop.name,
                            TextStyle(
                                10f,
                                Color.BLACK,
                                10f,
                                Color.WHITE,
                                TextStyle.Placement.BOTTOM,
                                2f,
                                true,
                                true
                            )
                        )

                        placemark.addTapListener(placemarkTapListener)

                    } else {
                        Toast.makeText(
                            this@YandexActivity,
                            "Could not add shop", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        builder.setNegativeButton(android.R.string.no) { dialog, which -> }
        builder.show()
    }


    fun drawMarkers() {
        allShops.forEach { shop ->
            val placemark = binding.mapview.map.mapObjects.addPlacemark().apply {
                geometry = Point(shop.latitude, shop.longitude)
                setIcon(ImageProvider.fromResource(this@YandexActivity, R.drawable.marker30))
            }
            placemark.setText(
                shop.name,
                TextStyle(
                    10f,
                    Color.BLACK,
                    10f,
                    Color.WHITE,
                    TextStyle.Placement.BOTTOM,
                    2f,
                    true,
                    true
                )
            )

            placemark.addTapListener(placemarkTapListener)
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    private fun loadData() {
        lifecycleScope.launchWhenCreated {
            binding.progressBar.isVisible = true
            binding.rvTodos.isVisible = false

            val response = try {
                RetrofitInstance.api.getShops()
            } catch (e: IOException) {
                Log.e(TAG, "IOException")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG, "HTTPException")
                return@launchWhenCreated
            }

            if (response.isSuccessful && response.body() != null) {
                shopAdapter.shops = response.body()!!.shops
                allShops = response.body()!!.shops as MutableList<Shop>
                drawMarkers()
            } else {
                Log.d(TAG, "Response not successfull")
            }
            binding.rvTodos.isVisible = true
            binding.progressBar.isVisible = false
        }
    }

    private val placemarkTapListener = MapObjectTapListener { _, point ->
        binding.mapview.map.move(
            CameraPosition(point, 14.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1f),
            null
        )
        true
    }

    private fun setupRecyclerView() = binding.rvTodos.apply {
        shopAdapter = ShopAdapter(object : ShopAdapter.OnShopCheckedChangeListener {
            override fun onShopChecked(shop: Shop, isChecked: Boolean) {
                if (isChecked) {
                    if (!enabledShops.contains(shop))
                        enabledShops.add(shop)
                } else {
                    if (enabledShops.contains(shop))
                        enabledShops.remove(shop)
                }
            }
        })
        adapter = shopAdapter
        layoutManager = LinearLayoutManager(this@YandexActivity)
    }


    private fun setupMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        } else {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    lastLocation = location
                    currentLocation = Point(lastLocation.latitude, lastLocation.longitude)
                    binding.mapview.map.move(
                        CameraPosition(currentLocation!!, 14.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 1f),
                        null
                    )

                    val placemark = binding.mapview.map.mapObjects.addPlacemark().apply {
                        geometry = currentLocation!!
                        setIcon(ImageProvider.fromResource(this@YandexActivity, R.drawable.person))
                    }
                    placemark.addTapListener(placemarkTapListener)

                } else {
                    Toast.makeText(this, "Location was null", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapview.onStart()
    }

    override fun onStop() {
        binding.mapview.onStart()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onRouteFailure(p0: ErrorHandling?) {
        Toast.makeText(this, "Could not draw route", Toast.LENGTH_SHORT).show()
    }

    override fun onRouteStart() {

    }

    override fun onRouteSuccess(
        routeInfoModelArrayList: ArrayList<RouteInfoModel>?,
        routeIndexing: Int
    ) {
        if (routeInfoModelArrayList.isNullOrEmpty()) {
            Toast.makeText(this, "Could not draw route", Toast.LENGTH_SHORT).show()
        } else {
            for (i in 0 until routeInfoModelArrayList.count()) {
                if (i == routeIndexing) {
                    var pointsG = routeInfoModelArrayList[routeIndexing].points
                    var pointsY: MutableList<Point> = mutableListOf()
                    pointsG.forEach {
                        pointsY.add(Point(it.latitude, it.longitude))
                    }

                    binding.mapview.map.mapObjects.addPolyline(
                        Polyline(
                            pointsY
                        )
                    )
                }
            }

            binding.listlayout.isVisible = false
        }

    }

    override fun onRouteCancelled() {
        Toast.makeText(this, "Draw route cancel", Toast.LENGTH_SHORT).show()
    }
}