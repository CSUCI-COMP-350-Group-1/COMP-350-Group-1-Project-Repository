package com.example.firebase_sprint0

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class User : AppCompatActivity() {
    // Declaration of global variables.
    var link: String? = null
    var code: String? = null
    var reply: String? = null

    // Default function call on creation of an activity.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged_in) // Setting the content view
    }

    // Method for deleting user data.
    fun delete(view: View?) {
        // Create various messages to display in the app.
        val failed_toast = Toast.makeText(this@User, "Request failed", Toast.LENGTH_SHORT)
        val delete_toast = Toast.makeText(this@User, "Account deleted", Toast.LENGTH_SHORT)
        // Create a worker thread for sending HTTP requests.
        val thread = Thread(object : Runnable {
            override fun run() {
                link =
                    "http://ip-address-temp:8080/users/all" // The private IP address of the machine is used
                try {
                    val url = URL(link) // new url object is created
                    val conn =
                        url.openConnection() as HttpURLConnection // HTTP connection object is created
                    conn.setRequestMethod("DELETE") // DELETE method
                    conn.setDoOutput(true)
                    conn.setDoInput(true)
                    conn.connect()
                    val `is` =
                        conn.getInputStream() // Input stream object for HTTP connection is created
                    val sb = StringBuffer() // String buffer object is created
                    // Fetch and append the incoming bytes until no more comes over the input stream.
                    try {
                        var chr: Int
                        while ((`is`.read().also { chr = it }) != -1) {
                            sb.append(chr.toChar())
                        }
                        reply = sb.toString()
                    } finally {
                        `is`.close() // Closing the input stream
                    }
                    code = conn.getResponseCode().toString() // Get the HTTP status code
                    conn.disconnect() // Disconnecting
                    // For unreachable network or other network related failures.
                    if (code != "200") {
                        failed_toast.show()
                    } else {
                        delete_toast.show()
                        val main = Intent(this@User, MainActivity::class.java)
                        main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) // Destroy all previous activities and clear the activity stack
                        startActivity(main)
                    }
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        })
        thread.start()
    }
}