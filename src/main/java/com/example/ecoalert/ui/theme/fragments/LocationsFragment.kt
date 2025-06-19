package com.example.ecoalert.ui.theme.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecoalert.R
import com.example.ecoalert.data.api.models.Location
import com.example.ecoalert.databinding.FragmentLocationsBinding
import com.example.ecoalert.ui.theme.fragments.adapters.LocationsAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.ecoalert.ui.theme.utils.Constants

class LocationsFragment : Fragment() {
    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var locationsAdapter: LocationsAdapter
    private var locationsListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "LocationsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        FirebaseFirestore.setLoggingEnabled(true)
        setupRecyclerView()
        setupClickListeners()
        loadUserLocations()
    }

    private fun setupRecyclerView() {
        locationsAdapter = LocationsAdapter(
            onLocationClick = { location ->
                navigateToMapWithLocation(location)
            },
            onDeleteClick = { location ->
                deleteLocation(location)
            },
            onToggleAlert = { location, enabled ->
                toggleLocationAlert(location, enabled)
            }
        )
        binding.recyclerViewLocations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddLocation.setOnClickListener {
            showAddLocationDialog()
        }
        binding.btnAddFirstLocation.setOnClickListener {
            showAddLocationDialog()
        }
    }

    private fun loadUserLocations() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Nie jesteś zalogowany", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_locations_to_login)
            return
        }
        showLoadingState()
        locationsListener = db.collection(Constants.LOCATIONS_COLLECTION)
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                hideLoadingState()
                if (error != null) {
                    Log.e(TAG, "Błąd wczytywania lokalizacji: ${error.message}", error)
                    Toast.makeText(
                        requireContext(),
                        "Błąd wczytywania lokalizacji: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val locations = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Location::class.java)?.copy(id = doc.id)
                    }
                    updateUI(locations)
                }
            }
    }

    private fun updateUI(locations: List<Location>) {
        if (locations.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewLocations.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewLocations.visibility = View.VISIBLE
            locationsAdapter.submitList(locations)
        }
    }

    private fun showAddLocationDialog() {
        val dialogFragment = AddLocationDialogFragment { location ->
            addLocation(location)
        }
        dialogFragment.show(parentFragmentManager, "AddLocationDialog")
    }

    private fun addLocation(location: Location) {
        val currentUser = auth.currentUser ?: return
        val locationWithUserId = location.copy(
            userId = currentUser.uid,
            createdAt = System.currentTimeMillis()
        )
        db.collection(Constants.LOCATIONS_COLLECTION)
            .add(locationWithUserId)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Lokalizacja dodana: ${documentReference.id}")
                Toast.makeText(requireContext(), "Lokalizacja dodana", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Błąd dodawania lokalizacji", e)
                Toast.makeText(
                    requireContext(),
                    "Błąd dodawania lokalizacji: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun deleteLocation(location: Location) {
        db.collection(Constants.LOCATIONS_COLLECTION)
            .document(location.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Lokalizacja usunięta")
                Toast.makeText(requireContext(), "Lokalizacja usunięta", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Błąd usuwania lokalizacji", e)
                Toast.makeText(
                    requireContext(),
                    "Błąd usuwania lokalizacji: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun toggleLocationAlert(location: Location, enabled: Boolean) {
        db.collection(Constants.LOCATIONS_COLLECTION)
            .document(location.id)
            .update("alertEnabled", enabled)
            .addOnSuccessListener {
                Log.d(TAG, "Alert zaktualizowany")
                val message = if (enabled) "Alerty włączone" else "Alerty wyłączone"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Błąd aktualizacji alertu", e)
                Toast.makeText(
                    requireContext(),
                    "Błąd zmiany ustawień alertów: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun navigateToMapWithLocation(location: Location) {
        val bundle = Bundle().apply {
            putParcelable("location", location)
        }
        findNavController().navigate(R.id.action_locations_to_map, bundle)
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoadingState() {
        binding.progressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationsListener?.remove()
        _binding = null
    }
}