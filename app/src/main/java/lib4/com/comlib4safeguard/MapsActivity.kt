package lib4.com.comlib4safeguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.fence_names_alert.view.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val dummyPaths = ArrayList<LatLng>(1)

    private val DEFAULT_RADIUS_METERS = 100.0
    private var fillColorArgb: Int = Color.parseColor("#AA8CE0F9")
    private var strokeColorArgb: Int = Color.parseColor("#AA6391E8")
    var fenceNames = arrayOf("Home", "School", "Tuition", "Park", "Play School", "Sports Centre", "Restricted Place")

    private lateinit var map: GoogleMap
    private lateinit var fenceNamesTextView: AutoCompleteTextView
    private val minZoom: Float = 10.0f
    private val maxZoom: Float = 17.0f
    private val defaultLocation = LatLng(25.2989, 55.4565)
    private val defaultHomeLocation = LatLng(25.3006416975452, 55.3795777)
    private val homeLocationLat = 25.3006416975452
    private val homeLocationLng = 55.3795777

    private val circles = ArrayList<DraggableCircle>(1)

    private lateinit var childMarker: Marker


    /**
     * This class contains information about a circle, including its markers
     */
    private inner class DraggableCircle(center: LatLng, private var radiusMeters: Double, name: String) {
        private val fenceName = name
        private val centerMarker: Marker = map.addMarker(MarkerOptions().apply {
            position(center)
            title(fenceName)
            draggable(true)
        })

        private val radiusMarker: Marker = map.addMarker(
                MarkerOptions().apply {
                    position(center.getPointAtDistance(radiusMeters))
                    icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
                    draggable(true)
                }
        )


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
                    showFenceName
                }
                radiusMarker -> {
                    radiusMeters = centerMarker.position.distanceFrom(radiusMarker.position)
                    circle.radius = radiusMeters
                    showFenceName
                }
                else -> return false
            }
            return true
        }

        private val showFenceName: Unit = centerMarker.showInfoWindow()
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


        val autocompleteFragment = fragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as PlaceAutocompleteFragment
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                map.addMarker(MarkerOptions().position(place.latLng).title(place.name.toString()))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(place.latLng, maxZoom))
            }

            override fun onError(status: Status) {

            }
        })
    }

    /**
     * When the map is ready, move the camera to put the Circle in the middle of the screen,
     * create a circle in Sydney, and set the listeners for the map, circles, and SeekBars.
     */
    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap ?: return
        // we need to initialise map before creating a circle
        with(map) {
            moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(defaultHomeLocation, maxZoom))
            setOnMapLongClickListener { point ->

                // We know the center, let's place the outline at a point 3/4 along the view.
                val view: View = supportFragmentManager.findFragmentById(R.id.map)?.view
                        ?: return@setOnMapLongClickListener
                val radiusLatLng = map.projection.fromScreenLocation(
                        android.graphics.Point(view.height * 3 / 9, view.width * 3 / 9))

                val builder = AlertDialog.Builder(this@MapsActivity)
                // Set the alert dialog title
                builder.setTitle("Guard")
                // Display a message on alert dialog
                builder.setMessage("Would you like to add new geo fencing protection on the selected location?")
                // Set a positive button and its click listener on alert dialog
                builder.setPositiveButton("YES") { dialog, which ->
                    showInputFenceAlert(point, radiusLatLng)
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

            val defaultFence = DraggableCircle(defaultHomeLocation, DEFAULT_RADIUS_METERS, "Home")
            circles.add(defaultFence)
            map.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(defaultHomeLocation, maxZoom))
        }
    }


    private fun updateChildMarker(latlng: LatLng) {
        map.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latlng, maxZoom))
        childMarker.position = latlng
    }

    private fun createChildMarker() {
        childMarker = map.addMarker(MarkerOptions().position(defaultHomeLocation)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.student))
                .title("Ameesh Thanheer"))
        childMarker.showInfoWindow()
        map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(defaultHomeLocation, maxZoom))
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

        var tempLat = homeLocationLat
        var tempLng = homeLocationLng
        while (tempLat <= defaultLocation.latitude || tempLng <= defaultLocation.longitude) {
            if (tempLat <= defaultLocation.latitude)
                tempLat += 0.0005
            if (tempLng <= defaultLocation.longitude)
                tempLng += 0.0005

            dummyPaths.add(LatLng(tempLat, tempLng))
        }
//        dummyPaths.add(LatLng(25.3006416975452, 55.3760777))
//        dummyPaths.add(LatLng(25.3006416975452, 55.3761207))
//        dummyPaths.add(LatLng(25.3006416975452, 55.3761747))
//        dummyPaths.add(LatLng(25.3006416975452, 55.3764967))
//        dummyPaths.add(LatLng(25.3006416975452, 55.3781788))
//        dummyPaths.add(LatLng(25.3006416975452, 55.3808506))
//        dummyPaths.add(LatLng(25.3006416975452, 55.3811513))
//        dummyPaths.add(LatLng(25.3056195975452, 55.3827519))
//        dummyPaths.add(LatLng(25.2862362975452, 55.4295738))
//        dummyPaths.add(LatLng(25.3022431975452, 55.4413686))
//        dummyPaths.add(LatLng(25.3071570975452, 55.4473192))
//        dummyPaths.add(LatLng(25.3039629975452, 55.4543536))
//        dummyPaths.add(LatLng(25.3006827975452, 55.4558628))
        dummyPaths.add(defaultLocation)

    }

    fun mockTheChildLocation() {

        var index = 0
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (index < dummyPaths.size) {
                    updateChildMarker(dummyPaths.get(index))
                    index++
                    handler.postDelayed(this, 1000)
                } else if (index == dummyPaths.size) {
                    index++
                    notifyUser("Ameesh arrived school just now!")
                }
                if (index == 2) {
                    notifyUser("Ameesh left home just now!")
                }

            }
        }, 5000)

    }

    fun notifyUser(message: String) {
        val intent = Intent(this, MapsActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val CHANNEL_ID = "my_channel_01"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "my_channel"
            val Description = "This is my channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = Description
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(mChannel)
        }

        val b = NotificationCompat.Builder(this, CHANNEL_ID)
        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setTicker("Hearty365")
                .setContentTitle("Safeguard")
                .setContentText(message)
                .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                .setContentIntent(contentIntent)
                .setContentInfo("Info")

        notificationManager.notify(1, b.build())
    }

    fun setUpFenceNameSpinner(view: View) {
        val adapter = ArrayAdapter<String>(view.context, android.R.layout.select_dialog_singlechoice, fenceNames)
        //Find TextView control
        fenceNamesTextView = view.findViewById<AutoCompleteTextView>(R.id.fence_names)
        //Set the number of characters the user must type before the drop down list is shown
        fenceNamesTextView.threshold = 1
        //Set the adapter
        fenceNamesTextView.setAdapter<ArrayAdapter<String>>(adapter)
    }

    fun showInputFenceAlert(point: LatLng, radiusLatLng: LatLng) {

        val builder = AlertDialog.Builder(this)
        // Get the layout inflater
        val view = layoutInflater.inflate(R.layout.fence_names_alert, null)
        builder.setView(view)
        builder.setTitle("Choose name for your new security fence")
        val alert = builder.create()
        alert.show()
        setUpFenceNameSpinner(view)

        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                fenceNamesTextView.showDropDown()
            }
        }, 200)

        view.done_fence_name.setOnClickListener {
            // Create the circle.
            val newCircle = DraggableCircle(point, point.distanceFrom(radiusLatLng), fenceNamesTextView.text.toString())
            circles.add(newCircle)

            if (circles.size > 0) {
                demo.visibility = View.VISIBLE
                demo.isEnabled = true
            }
            alert.dismiss()
        }
    }


}
