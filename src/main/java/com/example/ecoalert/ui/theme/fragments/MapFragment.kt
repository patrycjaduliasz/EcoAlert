package com.example.ecoalert.ui.theme.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location as AndroidLocation
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecoalert.R
import com.example.ecoalert.data.api.models.AirQuality
import com.example.ecoalert.data.api.models.Location
import com.example.ecoalert.data.repository.AirQualityRepository
import com.example.ecoalert.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val viewModel = MapViewModel(AirQualityRepository())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enableMyLocation()
            requestCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Brak uprawnień do lokalizacji", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        if (mapFragment != null) {
            mapFragment.getMapAsync(this)
        } else {
            Toast.makeText(requireContext(), "Błąd inicjalizacji mapy", Toast.LENGTH_LONG).show()
        }

        val savedLocation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("location", Location::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable<Location>("location")
        }

        savedLocation?.let {
            viewModel.setLocation(it)
        }

        viewModel.location.observe(viewLifecycleOwner) { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
                viewModel.fetchAirQuality(it)
            }
        }

        viewModel.airQuality.observe(viewLifecycleOwner) { result ->
            result.onSuccess { airQuality ->
                viewModel.location.value?.let { location ->
                    updateMapMarker(location, airQuality)
                }
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Błąd pobierania danych: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        if (hasLocationPermission()) {
            enableMyLocation()
            requestCurrentLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        try {
            if (hasLocationPermission()) {
                googleMap.isMyLocationEnabled = true
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Błąd uprawnień", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestCurrentLocation() {
        if (!hasLocationPermission()) return

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { androidLocation: AndroidLocation? ->
                if (androidLocation != null) {
                    val loc = Location(
                        latitude = androidLocation.latitude,
                        longitude = androidLocation.longitude,
                        name = "Aktualna lokalizacja"
                    )
                    viewModel.setLocation(loc)
                } else {
                    val defaultLocation = Location(
                        latitude = 52.2297,
                        longitude = 21.0122,
                        name = "Warszawa"
                    )
                    viewModel.setLocation(defaultLocation)
                    Toast.makeText(requireContext(), "Używam domyślnej lokalizacji", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Błąd podczas pobierania lokalizacji: ${exception.message}", Toast.LENGTH_LONG).show()
                val defaultLocation = Location(
                    latitude = 52.2297,
                    longitude = 21.0122,
                    name = "Warszawa"
                )
                viewModel.setLocation(defaultLocation)
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Brak uprawnień do lokalizacji", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateMapMarker(location: Location, airQuality: AirQuality) {
        val latLng = LatLng(location.latitude, location.longitude)
        val markerTitle = "${location.name}: AQI ${airQuality.aqi}"

        googleMap.clear()
        googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(markerTitle)
                .snippet(airQuality.getHealthRecommendation())
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MapViewModel(private val repository: AirQualityRepository) : ViewModel() {
    private val _location = MutableLiveData<Location>()
    val location: MutableLiveData<Location> get() = _location

    private val _airQuality = MutableLiveData<Result<AirQuality>>()
    val airQuality: MutableLiveData<Result<AirQuality>> get() = _airQuality

    fun setLocation(loc: Location) {
        _location.value = loc
    }

    fun fetchAirQuality(location: Location) {
        viewModelScope.launch {
            try {
                val result = repository.getCurrentAirQuality(location.latitude, location.longitude)
                _airQuality.postValue(result)
            } catch (e: Exception) {
                _airQuality.postValue(Result.failure(e))
            }
        }
    }
}
