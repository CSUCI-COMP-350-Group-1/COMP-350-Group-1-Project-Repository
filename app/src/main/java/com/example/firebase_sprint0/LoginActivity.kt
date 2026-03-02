package com.example.firebase_sprint0

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

// Login-related imports
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException


class LoginActivity : AppCompatActivity() {
    // Declaration of global variables.
    var name: EditText? = null
    var pass: EditText? = null
    var email: EditText? = null
    var name1: String? = null
    var pass1: String? = null
    var email1: String? = null
    var link: String? = null
    var reply: String? = null
    var code: String? = null
    var eye: ImageView? = null
    var state: Boolean = false

    // Default function call on creation of an activity.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing) // Setting the content view
        // Instantiate the View elements
        name = findViewById(R.id.user1)
        pass = findViewById(R.id.pass1)
        email = findViewById(R.id.email1)
        eye = findViewById(R.id.toggle_view1)
        name1 = ""
        pass1 = ""
        email1 = ""
    }

    // Method to start registration activity.
    fun register(view: View?) {
        val register = Intent(this, Register::class.java)
        startActivity(register)
    }

    // Method for login.
    fun login(view: View?) {
        // Get the inputs from the user.
        name1 = name!!.getText().toString()
        pass1 = pass!!.getText().toString()
        email1 = email!!.getText().toString()

        if (name1!!.isEmpty() || pass1!!.isEmpty() || email1!!.isEmpty()) {
            Toast.makeText(this@LoginActivity, "Fields cannot be blank", Toast.LENGTH_SHORT)
                .show() // Check whether the fields are not blank
        } else {
            // Create various messages to display in the app.
            val failed_toast =
                Toast.makeText(this@LoginActivity, "Request failed", Toast.LENGTH_SHORT)
            val incorrect_toast =
                Toast.makeText(this@LoginActivity, "Credentials are incorrect", Toast.LENGTH_SHORT)
            val logged_toast =
                Toast.makeText(this@LoginActivity, "Logged in", Toast.LENGTH_SHORT)
            // Create a worker thread for sending HTTP requests.
            val thread = Thread(object : Runnable {
                override fun run() {
                    // skip all this stuff until the HTTP type login query can be replaced with a Firebase-focused one.
                    // TEMPORARY!!!!!!
                    runOnUiThread {
                        logged_toast.show()
                        val user = Intent(this@LoginActivity, User::class.java)
                        user.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) // Destroy previous activities and clear Activity stack.
                        startActivity(user)
                    }
                    // END TEMPORARY
                    link =
                        "http://ip-address-temp:8080/users/login" // The private IP address of the machine is used
                    try {
                        val url = URL(link) // new url object is created
                        val conn =
                            url.openConnection() as HttpURLConnection // HTTP connection object is created
                        conn.setRequestMethod("POST") // POST method
                        conn.setRequestProperty(
                            "Content-Type",
                            "application/json; utf-8"
                        ) // JSON format is specified
                        conn.setRequestProperty("Accept", "application/json")
                        conn.setDoOutput(true)
                        conn.setDoInput(true)
                        val input = JSONObject() // New JSON object is created
                        // Give data to the JSON object
                        input.put("username", name1)
                        input.put("password", pass1)
                        input.put("email", email1)
                        val os =
                            DataOutputStream(conn.getOutputStream()) // Output stream object for HTTP connection is created
                        os.writeBytes(input.toString()) // JSON object is serialized and sent over the HTTP connection to the listening server
                        os.flush() // Flushing the output buffers
                        os.close() // Closing the output stream
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
                            runOnUiThread { failed_toast.show() }
                        } else {
                            // Generate an error message when the database has no matching data.
                            if (reply == "\"FAILURE\"") {
                                runOnUiThread { incorrect_toast.show() }
                            } else {
                                runOnUiThread {
                                    logged_toast.show()
                                    val user = Intent(this@LoginActivity, User::class.java)
                                    user.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) // Destroy previous activities and clear Activity stack.
                                    startActivity(user)
                                }
                            }
                        }
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            })
            thread.start()
        }
    }

    // For toggling visibility of password.
    fun toggle(v: View?) {
        if (!state) {
            pass!!.setTransformationMethod(HideReturnsTransformationMethod.getInstance())
            pass!!.setSelection(pass!!.getText().length)
            eye!!.setImageResource(R.drawable.show)
        } else {
            pass!!.setTransformationMethod(PasswordTransformationMethod.getInstance())
            pass!!.setSelection(pass!!.getText().length)
            eye!!.setImageResource(R.drawable.hide)
        }
        state = !state
    }
}
