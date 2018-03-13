package cz.vendasky.vikendovka

import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.Manifest;
import android.content.Context
import android.content.res.Resources
import android.support.v4.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.location.Criteria
import android.location.LocationManager
import com.google.android.gms.maps.model.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: GoogleMap? = null
    private val MY_PERMISSIONS_REQUEST_LOCATION = 1
    private val TAG = "cz.vendasky.vikendovka"
    private var markersCount = 0
    private var markersDrawn = 0

    private val activeEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            markersCount = dataSnapshot.children.count()
            dataSnapshot.children.forEach {
                val active = it.getValue(Active::class.java)!!
                val locationsRef = FirebaseDatabase.getInstance().getReference("locations")
                locationsRef.child(active.name).addValueEventListener(locationEventListener)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            // Failed to read value
        }
    }

    private val locationEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (markersDrawn == 0) {
                map?.clear()
            }
            val location = dataSnapshot.getValue(Location::class.java)!!
            val marker = LatLng(location.lat, location.lon)
            val icon = when (location.icon) {
                "parachute" -> R.drawable.parachute
                "guardian" -> R.drawable.guardian
                else -> R.drawable.airdrop
            }
            map?.addMarker(MarkerOptions()
                    .position(marker)
                    .title(location.name)
                    .icon(BitmapDescriptorFactory.fromResource(icon)))
            markersDrawn++
            if (markersDrawn == markersCount) {
                markersDrawn = 0
            }
        }

        override fun onCancelled(error: DatabaseError) {
            // Failed to read value
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val activeRef = FirebaseDatabase.getInstance().getReference("active")
        activeRef.addValueEventListener(activeEventListener)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isMapToolbarEnabled = false
        map?.uiSettings?.isZoomControlsEnabled = true
        map?.setMinZoomPreference(15f)
        map?.setMaxZoomPreference(23f)
        try {
            map?.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json))
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Resources not found.")
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map?.isMyLocationEnabled = true
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val criteria = Criteria()

            val location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false))
            val cameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(17f)
                .build()
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map?.isMyLocationEnabled = true
        }
    }
}
