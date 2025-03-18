package com.example.finalchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // No user signed in, launch LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Close MainActivity
            return
        }

        setContentView(R.layout.activity_main)

        database = Firebase.database.reference
        messageList = ArrayList()
        MessageAdapter(messageList, currentUser.uid).also { messageAdapter = it } // Pass current user ID
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        val chatRecyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        loadMessages()

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString()
            Log.d("SendButton", "Message captured: $messageText")
            sendMessage()
        }
    }

    private fun loadMessages() {
        database.child("messages").orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    message?.let { messageList.add(it) }
                }
                messageAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "loadMessages:onCancelled", error.toException())
            }
        })
    }

    private fun sendMessage() {
        Log.d("SendMessage", "sendMessage() called")
        val currentUser = auth.currentUser
        val messageText = messageEditText.text.toString()
        Log.d("SendMessage", "Current user: $currentUser")
        Log.d("SendMessage", "messageEditText is empty: ${messageText.isEmpty()}")

        if (currentUser != null && messageText.isNotEmpty()) {
            val message = Message(currentUser.uid, messageText, System.currentTimeMillis())
            database.child("messages").push().setValue(message)
            messageEditText.text.clear()
        }
    }
}