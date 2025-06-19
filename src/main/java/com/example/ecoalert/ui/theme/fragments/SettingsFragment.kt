package com.example.ecoalert.ui.theme.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.ecoalert.R
import com.example.ecoalert.databinding.FragmentSettingsBinding
import com.example.ecoalert.ui.theme.activities.LoginActivity
import com.example.ecoalert.ui.theme.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        setupUserInfo()
        setupClickListeners()
        loadSettings()
    }

    private fun setupUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.tvUserName.text = currentUser.displayName ?: "Użytkownik"
            binding.tvUserEmail.text = currentUser.email
        }
    }

    private fun setupClickListeners() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSettings(isChecked)
        }
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
        binding.layoutAboutApp.setOnClickListener {
            showAboutDialog()
        }
        binding.layoutPrivacyPolicy.setOnClickListener {
            Toast.makeText(requireContext(), "Polityka prywatności - w przygotowaniu", Toast.LENGTH_SHORT).show()
        }
        binding.layoutTermsOfService.setOnClickListener {
            Toast.makeText(requireContext(), "Warunki użytkowania - w przygotowaniu", Toast.LENGTH_SHORT).show()
        }
        binding.layoutContactSupport.setOnClickListener {
            openContactSupport()
        }
        binding.layoutExportData.setOnClickListener {
            Toast.makeText(requireContext(), "Eksport danych - w przygotowaniu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        val notificationsEnabled = sharedPreferences.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, true)
        binding.switchNotifications.isChecked = notificationsEnabled
    }

    private fun updateNotificationSettings(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, enabled)
            .apply()
        if (enabled) {
            FirebaseMessaging.getInstance().subscribeToTopic("air_quality_alerts")
                .addOnCompleteListener { task ->
                    val message = if (task.isSuccessful) "Powiadomienia włączone" else "Błąd włączania powiadomień"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("air_quality_alerts")
                .addOnCompleteListener { task ->
                    val message = if (task.isSuccessful) "Powiadomienia wyłączone" else "Błąd wyłączania powiadomień"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
        }
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection(Constants.USERS_COLLECTION)
                .document(currentUser.uid)
                .update("notificationsEnabled", enabled)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating notification settings", e)
                }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Wylogowanie")
            .setMessage("Czy na pewno chcesz się wylogować?")
            .setPositiveButton("Wyloguj") { _, _ ->
                logout()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun logout() {
        auth.signOut()
        sharedPreferences.edit().clear().apply()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Usuwanie konta")
            .setMessage("Czy na pewno chcesz usunąć swoje konto? Ta operacja jest nieodwracalna.")
            .setPositiveButton("Usuń konto") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return
        db.collection(Constants.USERS_COLLECTION)
            .document(currentUser.uid)
            .delete()
            .addOnSuccessListener {
                db.collection(Constants.LOCATIONS_COLLECTION)
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            document.reference.delete()
                        }
                        currentUser.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(requireContext(), "Konto usunięte", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(requireContext(), LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    requireActivity().finish()
                                } else {
                                    Toast.makeText(requireContext(), "Błąd usuwania konta: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user data", e)
                Toast.makeText(requireContext(), "Błąd usuwania danych: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("O aplikacji")
            .setMessage("""
                Aplikacja do monitorowania jakości powietrza.
                Funkcje:
                - Monitorowanie jakości powietrza w czasie rzeczywistym
                - Mapa z danymi o zanieczyszczeniu
                - Powiadomienia o złej jakości powietrza
                - Zapisywanie ulubionych lokalizacji
                Dane pochodzą z OpenWeatherMap API.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openContactSupport() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@ecoalert.com"))
            putExtra(Intent.EXTRA_SUBJECT, "EcoAlert - Wsparcie")
            putExtra(Intent.EXTRA_TEXT, """
                Proszę opisać problem lub pytanie:
                
                Dane użytkownika:
                Email: ${auth.currentUser?.email ?: "Brak"}
                Wersja aplikacji: 1.0
            """.trimIndent())
        }
        try {
            startActivity(Intent.createChooser(intent, "Wybierz aplikację email"))
        } catch (e: Exception) {
            Log.e(TAG, "Error opening email client", e)
            Toast.makeText(requireContext(), "Błąd otwierania klienta email", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}