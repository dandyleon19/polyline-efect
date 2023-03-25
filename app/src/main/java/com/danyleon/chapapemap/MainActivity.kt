package com.danyleon.chapapemap

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    var currentIndex = 0

    var polyline: Polyline? = null

    private val polylineOptions = PolylineOptions()

    val mHandler: Handler = Handler()

    var markers = arrayListOf<LatLng>()

    private val ANIMATE_SPEEED = 500
    private val BEARING_OFFSET = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        addDefaultLocations()

        val polylineOptions = PolylineOptions()
            .add(LatLng(50.961813797827055, 3.5168474167585373))
            .add(LatLng(50.96085423274633, 3.517405651509762))
            .add(LatLng(50.96020550146382, 3.5177918896079063))
            .add(LatLng(50.95936754348453, 3.518972061574459))
            .add(LatLng(50.95877285446026, 3.5199161991477013))
            .add(LatLng(50.958179213755905, 3.520646095275879))
            .add(LatLng(50.95901719316589, 3.5222768783569336))
            .add(LatLng(50.95954430150347, 3.523542881011963))
            .add(LatLng(50.95873336312275, 3.5244011878967285))
            .add(LatLng(50.95955781702322, 3.525688648223877))
            .add(LatLng(50.958855004782116, 3.5269761085510254))
            .width(15f)
            .color(ContextCompat.getColor(this, R.color.grey))
            .geodesic(true)

        polyline = map.addPolyline(polylineOptions)

        initialize()
    }

    private fun initialize() {

        polyline = initializePolyLine()

        val markerPos = markers[0]
        val secondPos = markers[1]

        setupCameraPositionForMovement(markerPos, secondPos);
    }

    private fun setupCameraPositionForMovement(
        markerPos: LatLng,
        secondPos: LatLng
    ) {
        val bearing: Float = bearingBetweenLatLng(markerPos, secondPos)

        val setCameraPosition = CameraPosition.Builder()
            .target(markerPos)
            .bearing(bearing + BEARING_OFFSET)
            .zoom(14f)
            .build()

        val builder = LatLngBounds.Builder()
        for (marker in markers) {
            builder.include(marker)
        }

        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val padding = (width * 0.10).toInt()

        val bounds = builder.build()

        val moveCamera = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding)

        map.animateCamera(moveCamera, ANIMATE_SPEEED, object : CancelableCallback {
            override fun onFinish() {
                val handler = Handler()
                val runnable = object : Runnable {

                    var start = SystemClock.uptimeMillis()

                    private val interpolator: Interpolator = LinearInterpolator()

                    var endLatLng: LatLng = getEndLatLngXd()
                    var beginLatLng: LatLng = getBeginLatLngXd()

                    override fun run() {
                        val elapsed = SystemClock.uptimeMillis() - start
                        val t = interpolator.getInterpolation(elapsed.toFloat() / 800)

                        val lat = t * endLatLng.latitude + (1 - t) * beginLatLng.latitude
                        val lng = t * endLatLng.longitude + (1 - t) * beginLatLng.longitude

                        val newPosition = LatLng(lat, lng)

                        updatePolyLine(newPosition)

                        if (t < 1) {
                            mHandler.postDelayed(this, 16)
                        } else {
                            if (currentIndex < markers.size - 2) {
                                currentIndex++
                                endLatLng = getEndLatLngXd()
                                beginLatLng = getBeginLatLngXd()
                                start = SystemClock.uptimeMillis()
                                val begin: LatLng = getBeginLatLngXd()
                                val end: LatLng = getEndLatLngXd()
                                val bearingL = bearingBetweenLatLng(begin, end)

                                start = SystemClock.uptimeMillis()
                                mHandler.postDelayed(this, 16)
                            } else {
                                currentIndex++
                                reset()
                            }
                        }
                    }

                    private fun updatePolyLine(latLng: LatLng) {
                        val points = polyline?.points
                        points?.add(latLng)
                        if (points != null) {
                            polyline?.points = points
                        }
                    }

                    private fun getEndLatLngXd(): LatLng {
                        return markers[currentIndex + 1]
                    }

                    private fun getBeginLatLngXd(): LatLng {
                        return markers[currentIndex]
                    }

                    fun stopAnimation() {
                        stop()
                    }

                    fun stop() {
                        mHandler.removeCallbacks(this)
                    }

                    fun reset() {

                        mHandler.removeCallbacks(this)

                        polyline?.points = arrayListOf()

                        start = SystemClock.uptimeMillis()
                        currentIndex = 0
                        endLatLng = getEndLatLngXd()
                        beginLatLng = getBeginLatLngXd()

                        handler.postDelayed(this, 16)
                    }
                }

                handler.post(runnable)
            }

            override fun onCancel() {}
        })

    }

    private fun initializePolyLine(): Polyline {
        polylineOptions.add(markers[0])
        return map.addPolyline(polylineOptions)
    }

    private fun bearingBetweenLatLng(begin: LatLng, end: LatLng): Float {
        val beginL: Location = convertLatLngToLocation(begin)
        val endL: Location = convertLatLngToLocation(end)
        return beginL.bearingTo(endL)
    }

    private fun convertLatLngToLocation(latLng: LatLng): Location {
        val loc = Location("someLoc")
        loc.latitude = latLng.latitude
        loc.longitude = latLng.longitude
        return loc
    }

    private fun addDefaultLocations() {
        addMarkerToMap(LatLng(50.961813797827055, 3.5168474167585373))
        addMarkerToMap(LatLng(50.96085423274633, 3.517405651509762))
        addMarkerToMap(LatLng(50.96020550146382, 3.5177918896079063))
        addMarkerToMap(LatLng(50.95936754348453, 3.518972061574459))
        addMarkerToMap(LatLng(50.95877285446026, 3.5199161991477013))
        addMarkerToMap(LatLng(50.958179213755905, 3.520646095275879))
        addMarkerToMap(LatLng(50.95901719316589, 3.5222768783569336))
        addMarkerToMap(LatLng(50.95954430150347, 3.523542881011963))
        addMarkerToMap(LatLng(50.95873336312275, 3.5244011878967285))
        addMarkerToMap(LatLng(50.95955781702322, 3.525688648223877))
        addMarkerToMap(LatLng(50.958855004782116, 3.5269761085510254))
    }

    private fun addMarkerToMap(latLng: LatLng) {
        markers.add(latLng)
    }
}