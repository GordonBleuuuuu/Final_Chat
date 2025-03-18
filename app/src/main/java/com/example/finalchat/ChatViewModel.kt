package com.example.finalchat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val database = Firebase.database.reference
    val messages = mutableStateListOf<Message>()
    val messageInput = mutableStateOf("")

    init {
        authenticateAndLoadMessages() // Combined authentication and loading
    }

    private fun authenticateAndLoadMessages() {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    android.util.Log.d("Auth", "signInAnonymously:success")
                    loadMessages() // Load messages only after successful authentication.
                } else {
                    // If sign in fails, display a message to the user.
                    android.util.Log.w("Auth", "signInAnonymously:failure", task.exception)
                }
            }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            database.child("messages").orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        message?.let { messages.add(it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("DatabaseError", "Failed to load messages: ${error.message}")
                }
            })
        }
    }

    fun sendMessage() {
        val currentUser = auth.currentUser
        if (currentUser != null && messageInput.value.isNotEmpty()) {
            val message = Message(currentUser.uid, messageInput.value, System.currentTimeMillis())
            database.child("messages").push().setValue(message)
            messageInput.value = "" // Clear input
        }
    }
}