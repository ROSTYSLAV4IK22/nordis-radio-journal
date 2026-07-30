package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nordisapps.nordisradiojournal.data.model.AdminState

class AuthViewModel(
    application: Application,
    private val shared: SharedStateHolder,
    private val onUserLoggedIn: (uid: String) -> Unit,
    private val onGuestSession: () -> Unit
) : AndroidViewModel(application) {
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            shared.update {
                it.copy(
                    isUserLoggedIn = true,
                    adminState = AdminState.Unknown
                )
            }
            checkAdminStatus(user.uid)
            onUserLoggedIn(user.uid)
        } else {
            shared.update {
                it.copy(
                    isUserLoggedIn = false,
                    adminState = AdminState.NotAdmin
                )
            }
            onGuestSession()
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    private fun checkAdminStatus(uid: String) {
        FirebaseDatabase.getInstance()
            .getReference("admins")
            .get()
            .addOnSuccessListener { snapshot ->
                val isAdmin = snapshot.children.any { it.value == uid }
                shared.update {
                    it.copy(
                        adminState = if (isAdmin)
                            AdminState.Admin
                        else
                            AdminState.NotAdmin
                    )
                }
            }
            .addOnFailureListener {
                shared.update {
                    it.copy(adminState = AdminState.NotAdmin)
                }
            }
    }

    override fun onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }
}