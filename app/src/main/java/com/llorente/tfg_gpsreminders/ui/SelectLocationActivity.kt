package com.llorente.tfg_gpsreminders.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.llorente.tfg_gpsreminders.R

class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2001
    }

    private lateinit var editTextSearchLocation: TextInputEditText
    private lateinit var recyclerViewPredictions: RecyclerView
    private lateinit var textViewLocationPreview: TextView
    private lateinit var buttonConfirmLocation: MaterialButton

    private lateinit var predictionAdapter: LocationPredictionAdapter
    private lateinit var placesClient: PlacesClient

    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null

    private var selectedPlaceName: String? = null
    private var selectedAddress: String? = null
    private var suppressSearchTextWatcher = false

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_location)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSelectLocation)
        val buttonUseMyLocation = findViewById<MaterialButton>(R.id.buttonUseMyLocation)

        editTextSearchLocation = findViewById(R.id.editTextSearchLocation)
        recyclerViewPredictions = findViewById(R.id.recyclerViewPredictions)
        textViewLocationPreview = findViewById(R.id.textViewLocationPreview)
        buttonConfirmLocation = findViewById(R.id.buttonConfirmLocation)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        initializePlacesIfNeeded()
        placesClient = Places.createClient(this)

        setupPredictionsList()
        setupSearchField()
        readInitialLocationFromIntent()

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment
        mapFragment.getMapAsync(this)

        buttonUseMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }

        buttonConfirmLocation.setOnClickListener {
            returnSelectedLocation()
        }

        updateSelectionUI()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMapToolbarEnabled = true

        googleMap?.setOnMapClickListener { latLng ->
            selectedLatLng = latLng
            selectedAddress = "Ubicación seleccionada manualmente"

            updateMarkerAndCamera(latLng, selectedAddress)
            updateSelectionUI()
        }

        enableMyLocationIfGranted()

        if (selectedLatLng != null) {
            updateMarkerAndCamera(selectedLatLng!!, selectedAddress)
        } else {
            val defaultLocation = LatLng(40.4168, -3.7038)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))
        }
    }

    private fun setupPredictionsList() {
        predictionAdapter = LocationPredictionAdapter(emptyList()) { item ->
            fetchPlaceDetails(item)
        }

        recyclerViewPredictions.apply {
            layoutManager = LinearLayoutManager(this@SelectLocationActivity)
            adapter = predictionAdapter
        }
    }

    private fun setupSearchField() {
        editTextSearchLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (suppressSearchTextWatcher) {
                    return
                }

                val query = s?.toString()?.trim().orEmpty()

                if (query.length < 2) {
                    predictionAdapter.updateItems(emptyList())
                    recyclerViewPredictions.visibility = View.GONE
                    return
                }

                searchPredictions(query)
            }
        })
    }

    private fun searchPredictions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val items = response.autocompletePredictions.map { prediction ->
                    LocationPredictionItem(
                        placeId = prediction.placeId,
                        primaryText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null)?.toString()
                    )
                }

                predictionAdapter.updateItems(items)
                recyclerViewPredictions.visibility =
                    if (items.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { exception ->
                recyclerViewPredictions.visibility = View.GONE

                android.util.Log.e(
                    "PlacesSearch",
                    "Error al obtener sugerencias: ${exception.message}",
                    exception
                )

                Toast.makeText(
                    this,
                    "Error Places: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun fetchPlaceDetails(item: LocationPredictionItem) {
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LAT_LNG
        )

        val request = FetchPlaceRequest.builder(item.placeId, placeFields).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng

                if (latLng == null) {
                    Toast.makeText(
                        this,
                        "No se pudo obtener la ubicación del lugar seleccionado",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                selectedLatLng = latLng
                selectedPlaceName = place.displayName
                selectedAddress = place.formattedAddress

                suppressSearchTextWatcher = true
                editTextSearchLocation.setText(item.primaryText)
                editTextSearchLocation.setSelection(editTextSearchLocation.text?.length ?: 0)
                suppressSearchTextWatcher = false

                predictionAdapter.updateItems(emptyList())
                recyclerViewPredictions.visibility = View.GONE

                updateMarkerAndCamera(latLng, selectedPlaceName ?: selectedAddress)
                updateSelectionUI()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se pudo obtener el detalle del lugar seleccionado",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun initializePlacesIfNeeded() {
        if (Places.isInitialized()) {
            return
        }

        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()

        if (apiKey.isBlank()) {
            Toast.makeText(
                this,
                "No se ha encontrado la API Key de Google Maps",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
    }

    private fun readInitialLocationFromIntent() {
        if (intent.hasExtra("task_latitude") && intent.hasExtra("task_longitude")) {
            val latitude = intent.getDoubleExtra("task_latitude", 0.0)
            val longitude = intent.getDoubleExtra("task_longitude", 0.0)

            selectedLatLng = LatLng(latitude, longitude)
            selectedAddress = intent.getStringExtra("task_location_address")
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        if (!hasLocationPermission()) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        enableMyLocationIfGranted()

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(
                        this,
                        "No se pudo obtener la ubicación actual",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                val latLng = LatLng(location.latitude, location.longitude)
                selectedLatLng = latLng
                selectedAddress = "Ubicación actual"

                updateMarkerAndCamera(latLng, selectedAddress)
                updateSelectionUI()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Error al obtener la ubicación actual",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateMarkerAndCamera(latLng: LatLng, title: String?) {
        googleMap?.clear()
        googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title ?: "Ubicación seleccionada")
        )
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
    }

    private fun updateSelectionUI() {
        val latLng = selectedLatLng

        if (latLng == null) {
            textViewLocationPreview.text = "No se ha seleccionado ninguna ubicación"
            buttonConfirmLocation.isEnabled = false
            return
        }

        textViewLocationPreview.text = buildLocationPreview(latLng)
        buttonConfirmLocation.isEnabled = true
    }

    private fun buildLocationPreview(latLng: LatLng): String {
        if (!selectedAddress.isNullOrBlank()) {
            return selectedAddress!!
        }

        return "Latitud: ${latLng.latitude}\nLongitud: ${latLng.longitude}"
    }

    private fun returnSelectedLocation() {
        val latLng = selectedLatLng ?: return

        val resultIntent = Intent().apply {
            putExtra("selected_latitude", latLng.latitude)
            putExtra("selected_longitude", latLng.longitude)
            putExtra("selected_place_name", selectedPlaceName)
            putExtra("selected_address", selectedAddress)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationIfGranted() {
        if (!hasLocationPermission()) {
            return
        }

        googleMap?.isMyLocationEnabled = true
    }

    @Deprecated("Se mantiene por compatibilidad con este flujo sencillo de permisos")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }

        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            Toast.makeText(
                this,
                "Sin permisos de ubicación no se puede usar esta función",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        enableMyLocationIfGranted()
        moveToCurrentLocation()
    }
}