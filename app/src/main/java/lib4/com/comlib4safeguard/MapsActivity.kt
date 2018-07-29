package lib4.com.comlib4safeguard

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val dummyPaths = ArrayList<LatLng>(1)

    private val DEFAULT_RADIUS_METERS = 100.0
    private var fillColorArgb: Int = Color.parseColor("#AA8CE0F9")
    private var strokeColorArgb: Int = Color.parseColor("#AA6391E8")

    private lateinit var map: GoogleMap
    private val minZoom: Float = 10.0f
    private val maxZoom: Float = 17.0f
    private val defaultLocation = LatLng(25.2989, 55.4565)
    private val circles = ArrayList<DraggableCircle>(1)

    private lateinit var childMarker: Marker


    /**
     * This class contains information about a circle, including its markers
     */
    private inner class DraggableCircle(center: LatLng, private var radiusMeters: Double) {
        private val centerMarker: Marker = map.addMarker(MarkerOptions().apply {
            position(center)
            draggable(true)
        })

        private val radiusMarker: Marker = map.addMarker(
                MarkerOptions().apply {
                    position(center.getPointAtDistance(radiusMeters))
                    icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
                    draggable(true)
                })

        private val circle: Circle = map.addCircle(
                CircleOptions().apply {
                    center(center)
                    radius(radiusMeters)
                    strokeWidth(10.0f)
                    strokeColor(strokeColorArgb)
                    fillColor(fillColorArgb)
                    clickable(true)
                })

        fun onMarkerMoved(marker: Marker): Boolean {
            when (marker) {
                centerMarker -> {
                    circle.center = marker.position
                    radiusMarker.position = marker.position.getPointAtDistance(radiusMeters)
                }
                radiusMarker -> {
                    radiusMeters = centerMarker.position.distanceFrom(radiusMarker.position)
                    circle.radius = radiusMeters
                }
                else -> return false
            }
            return true
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        loadDummyPaths()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        Toast.makeText(this, "Tap on hold to add new Geo fencing tracking!", Toast.LENGTH_LONG).show()
        demo.setOnClickListener {
            createChildMarker()
            mockTheChildLocation()
        }
        demo.isEnabled = false
    }

    /**
     * When the map is ready, move the camera to put the Circle in the middle of the screen,
     * create a circle in Sydney, and set the listeners for the map, circles, and SeekBars.
     */
    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap ?: return
        // we need to initialise map before creating a circle
        with(map) {
            moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(defaultLocation, maxZoom))
            setOnMapLongClickListener { point ->

                // We know the center, let's place the outline at a point 3/4 along the view.
                val view: View = supportFragmentManager.findFragmentById(R.id.map)?.view
                        ?: return@setOnMapLongClickListener
                val radiusLatLng = map.projection.fromScreenLocation(
                        android.graphics.Point(view.height * 3 / 6, view.width * 3 / 6))

                val builder = AlertDialog.Builder(this@MapsActivity)
                // Set the alert dialog title
                builder.setTitle("Guard")
                // Display a message on alert dialog
                builder.setMessage("Would you like to add new geo fencing protection on the selected location?")
                // Set a positive button and its click listener on alert dialog
                builder.setPositiveButton("YES") { dialog, which ->
                    // Create the circle.
                    val newCircle = DraggableCircle(point, point.distanceFrom(radiusLatLng))
                    circles.add(newCircle)
                    if (circles.size > 0) {
                        demo.isEnabled = true
                    }
                }
                // Display a negative button on alert dialog
                builder.setNegativeButton("No") { dialog, which ->
                }

                builder.show()
            }

            setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {
                    onMarkerMoved(marker)
                }

                override fun onMarkerDragEnd(marker: Marker) {
                    onMarkerMoved(marker)
                }

                override fun onMarkerDrag(marker: Marker) {
                    onMarkerMoved(marker)
                }
            })

            // Flip the red, green and blue components of the circle's stroke color.
            setOnCircleClickListener { c -> c.strokeColor = c.strokeColor xor 0x00ffffff }
        }
//        val defaultFence = DraggableCircle(defaultLocation, DEFAULT_RADIUS_METERS)
//        circles.add(defaultFence)
    }


    private fun updateChildMarker(latlng: LatLng) {
        childMarker.position = latlng
        map.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latlng, maxZoom))

    }

    private fun createChildMarker() {
        childMarker = map.addMarker(MarkerOptions().position(LatLng(25.3006416975452, 55.3795777))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.student))
                .title("Ameesh Thanheer"))
        childMarker.showInfoWindow()

    }

    private fun onMarkerMoved(marker: Marker) {
        circles.forEach { if (it.onMarkerMoved(marker)) return }
    }

    /**
     * Extension function to find the distance from this to another LatLng object
     */
    private fun LatLng.distanceFrom(other: LatLng): Double {
        val result = FloatArray(1)
        Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, result)
        return result[0].toDouble()
    }

    private fun LatLng.getPointAtDistance(distance: Double): LatLng {
        val radiusOfEarth = 6371009.0
        val radiusAngle = (Math.toDegrees(distance / radiusOfEarth)
                / Math.cos(Math.toRadians(latitude)))
        return LatLng(latitude, longitude + radiusAngle)
    }

    fun loadDummyPaths() {
        dummyPaths.add(LatLng(25.3006416975452, 55.3795777))
        dummyPaths.add(LatLng(25.3006416975452, 55.3781207))
        dummyPaths.add(LatLng(25.3006416975452, 55.3761747))
        dummyPaths.add(LatLng(25.3006416975452, 55.3764967))
        dummyPaths.add(LatLng(25.3006416975452, 55.3781788))
        dummyPaths.add(LatLng(25.3006416975452, 55.3808506))
        dummyPaths.add(LatLng(25.3006416975452, 55.3811513))
        dummyPaths.add(LatLng(25.3056195975452, 55.3827519))
        dummyPaths.add(LatLng(25.2862362975452, 55.4295738))
        dummyPaths.add(LatLng(25.3022431975452, 55.4413686))
        dummyPaths.add(LatLng(25.3071570975452, 55.4473192))
        dummyPaths.add(LatLng(25.3039629975452, 55.4543536))
        dummyPaths.add(LatLng(25.3006827975452, 55.4558628))
        dummyPaths.add(LatLng(25.2989, 55.4558))

    }

    fun mockTheChildLocation() {

        var index = 0
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (index < dummyPaths.size) {
                    updateChildMarker(dummyPaths.get(index))
                    index++
                    handler.postDelayed(this, 3000)
                } else if (index == 14) {
                    index++
                    notifyUser()
                }

            }
        }, 1000)

    }

    fun notifyUser() {
        val intent = Intent(this, MapsActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val b = NotificationCompat.Builder(this)

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setTicker("Hearty365")
                .setContentTitle("Safeguard")
                .setContentText("Ammesh just reached the school")
                .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                .setContentIntent(contentIntent)
                .setContentInfo("Info")


        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, b.build())
    }

}
