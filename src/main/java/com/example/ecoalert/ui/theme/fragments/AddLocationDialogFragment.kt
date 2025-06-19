package com.example.ecoalert.ui.theme.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.example.ecoalert.R
import com.example.ecoalert.data.api.models.Location
import com.example.ecoalert.databinding.DialogAddLocationBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class AddLocationDialogFragment(
    private val onLocationAdded: (Location) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddLocationBinding? = null
    private val binding get() = _binding!!
    private var selectedLatLng: LatLng? = null
    private var locationName: String = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicjalizacja Fused Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Inicjalizacja Places
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "")
        }

        // Konfiguracja Autocomplete i przycisków
        setupAutocompleteFragment(null)
        setupButtons()
        updateSaveButtonState()

        // Pobierz bieżącą lokalizację
        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                selectedLatLng = currentLatLng
                locationName = "Moja lokalizacja"
                updateSaveButtonState()

                setupAutocompleteFragment(currentLatLng)
                Log.d("AddLocationDialog", "Pobrano lokalizację: $currentLatLng")
                Toast.makeText(requireContext(), "Pobrano lokalizację: Gdynia", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Nie udało się pobrać lokalizacji. Włącz GPS i spróbuj ponownie.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.addOnFailureListener { e ->
            Log.e("AddLocationDialog", "Błąd pobierania lokalizacji: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Błąd pobierania lokalizacji: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupAutocompleteFragment(currentLatLng: LatLng?) {
        var autocompleteFragment = childFragmentManager
            .findFragmentById(R.id.autocomplete_fragment_container) as? AutocompleteSupportFragment

        if (autocompleteFragment == null) {
            autocompleteFragment = AutocompleteSupportFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.autocomplete_fragment_container, autocompleteFragment)
                .commitNow()
        }

        autocompleteFragment?.let { fragment ->
            fragment.setPlaceFields(
                listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG
                )
            )

            currentLatLng?.let {
                val bounds = RectangularBounds.newInstance(
                    LatLng(it.latitude - 0.1, it.longitude - 0.1), // SW
                    LatLng(it.latitude + 0.1, it.longitude + 0.1)  // NE
                )
                fragment.setLocationBias(bounds)
            }

            fragment.setHint(getString(R.string.add_location))

            fragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    Log.d("AddLocationDialog", "Wybrano miejsce: ${place.name}")
                    selectedLatLng = place.latLng
                    locationName = place.name ?: ""
                    updateSaveButtonState()
                }

                override fun onError(status: Status) {
                    Log.e("AddLocationDialog", "Błąd wyboru miejsca: $status")
                    Toast.makeText(requireContext(), "Błąd wyboru miejsca: $status", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            Log.e("AddLocationDialog", "Nie udało się utworzyć AutocompleteSupportFragment")
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            val latLng = selectedLatLng
            if (latLng != null && locationName.isNotBlank()) {
                val location = Location(
                    name = locationName,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    alertEnabled = true
                )
                Log.d("AddLocationDialog", "Zapisywanie lokalizacji: $location")
                onLocationAdded(location)
                dismiss()
            } else {
                Log.w("AddLocationDialog", "Brak wybranej lokalizacji lub nazwy")
                Toast.makeText(requireContext(), "Wybierz lokalizację", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateSaveButtonState() {
        val isValid = selectedLatLng != null && locationName.isNotBlank()
        binding.btnSave.isEnabled = isValid
        Log.d("AddLocationDialog", "Stan przycisku Zapisz: $isValid (latLng: $selectedLatLng, name: '$locationName')")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Brak uprawnień do lokalizacji. Włącz uprawnienia w ustawieniach.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}